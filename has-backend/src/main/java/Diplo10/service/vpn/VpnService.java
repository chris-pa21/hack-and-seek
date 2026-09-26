package Diplo10.service.vpn;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.exception.VpnUnavailableException;
import Diplo10.repository.PlayerRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class VpnService {

    private static final String WG_CONTAINER = "has-wireguard-server-1";
    private static final String WG_INTERFACE = "wg0";

    /** WireGuard public keys: 32 bytes, base64-encoded. */
    private static final Pattern WG_KEY = Pattern.compile("[A-Za-z0-9+/]{42}[AEIMQUYcgkosw048]=");

    private final DockerClient docker;
    private final VpnManagerClient vpnManagerClient;
    private final PlayerRepository playerRepository;

    @Value("${vpn.server.endpoint}")
    private String serverEndpoint;

    public VpnService(DockerClient docker, VpnManagerClient vpnManagerClient, PlayerRepository playerRepository) {
        this.docker = docker;
        this.vpnManagerClient = vpnManagerClient;
        this.playerRepository = playerRepository;
    }

    /**
     * Attaches the WireGuard container to a game's Docker network so it can
     * route traffic to/from challenge containers.
     */
    public void includeNetwork(String networkId) {
        try {
            docker.connectToNetworkCmd()
                    .withContainerId(WG_CONTAINER)
                    .withNetworkId(networkId)
                    .exec();
            System.out.println("Attached WireGuard to network: " + networkId);
        } catch (Exception e) {
            if (e.getMessage() != null &&
                    (e.getMessage().contains("already exists in network") ||
                     e.getMessage().contains("already connected"))) {
                System.out.println("WireGuard already connected to network: " + networkId);
            } else {
                System.err.println("Error attaching WireGuard to network " + networkId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Detaches the WireGuard container from a game's Docker network on game end.
     */
    public void excludeNetwork(String networkId) {
        try {
            docker.disconnectFromNetworkCmd()
                    .withContainerId(WG_CONTAINER)
                    .withNetworkId(networkId)
                    .exec();
            System.out.println("Detached WireGuard from network: " + networkId);
        } catch (Exception e) {
            System.err.println("Error detaching WireGuard from network " + networkId + ": " + e.getMessage());
        }
    }

    /**
     * Ensures the player has a VPN IP, a WireGuard public key, and a peer entry
     * on the server. Idempotent: safe to call multiple times.
     *
     * Key generation strategy:
     * - If the player already has a public key (e.g. submitted by the client via
     *   {@code POST /api/user/vpn-public-key}), that key is respected and no new
     *   keypair is generated.
     * - If no public key exists, a full keypair is generated server-side via
     *   {@code wg genkey} and both keys are persisted so a complete config file
     *   can later be downloaded via {@code GET /api/games/{id}/vpn-config}.
     */
    public void ensureVpnConfig(Player player) {
        System.out.println("ensureVpnConfig for player: " + player.getPlayer());

        boolean needsSave = false;

        // 1. Assign a unique VPN IP
        if (player.getVpnIp() == null) {
            int nextSuffix = playerRepository.findAll().stream()
                    .map(Player::getVpnIp)
                    .filter(ip -> ip != null && ip.startsWith("10.8.19."))
                    .mapToInt(ip -> {
                        try { return Integer.parseInt(ip.substring(ip.lastIndexOf('.') + 1)); }
                        catch (NumberFormatException e) { return 0; }
                    })
                    .max()
                    .orElse(1) + 1;
            player.setVpnIp("10.8.19." + nextSuffix);
            System.out.println("Assigned VPN IP: " + player.getVpnIp());
            needsSave = true;
        }

        if (player.getVpnPublicKey() == null) {
            String output = execInContainer(
                    "sh", "-c",
                    "pk=$(wg genkey); printf '%s\\n%s\\n' \"$pk\" \"$(printf '%s' \"$pk\" | wg pubkey)\""
            );
            if (output == null || !output.contains("\n")) {
                System.err.println("Failed to generate WireGuard keypair for " + player.getPlayer());
                return;
            }
            String[] lines = output.trim().split("\n");
            if (lines.length < 2) {
                System.err.println("Unexpected keypair output for " + player.getPlayer() + ": " + output);
                return;
            }
            player.setVpnPrivateKey(lines[0].trim());
            player.setVpnPublicKey(lines[1].trim());
            System.out.println("Generated server-side keypair for " + player.getPlayer()
                    + " — pubkey: " + player.getVpnPublicKey());
            needsSave = true;
        }

        if (needsSave) {
            playerRepository.save(player);
        }

        addPeerToWireguard(player);
    }

    /**
     * Adds (or refreshes) the player's WireGuard peer entry on the server.
     */
    private void addPeerToWireguard(Player player) {
        execInContainer("wg", "set", WG_INTERFACE,
                "peer", player.getVpnPublicKey(),
                "allowed-ips", player.getVpnIp() + "/32");
        // wg set does NOT add a kernel route — add it explicitly so replies
        // are routed back through the tunnel instead of the default gateway.
        execInContainer("ip", "route", "replace",
                player.getVpnIp() + "/32", "dev", WG_INTERFACE);
        System.out.println("Registered WireGuard peer for: " + player.getPlayer()
                + " (" + player.getVpnIp() + ")");
    }

    /**
     * Removes the player's WireGuard peer from the server.
     * Call this when permanently removing a player's VPN access.
     */
    public void removePeer(Player player) {
        if (player.getVpnPublicKey() == null) return;
        execInContainer("wg", "set", WG_INTERFACE, "peer", player.getVpnPublicKey(), "remove");
        if (player.getVpnIp() != null) {
            execInContainer("ip", "route", "del", player.getVpnIp() + "/32", "dev", WG_INTERFACE);
        }
        System.out.println("Removed WireGuard peer for: " + player.getPlayer());
    }

    /**
     * Generates a ready-to-use WireGuard client config for the player.
     * The AllowedIPs covers the VPN subnet and the Docker bridge range so that
     * traffic to challenge containers is routed through the tunnel.
     * Actual access is enforced by iptables rules on the WireGuard host.
     *
     * @throws VpnUnavailableException if the WireGuard server is not running
     */
    public String generateConfig(Player player) {
        String privateKey  = player.getVpnPrivateKey() != null ? player.getVpnPrivateKey() : "<PRIVATE_KEY_MISSING>";
        String serverPub   = serverPublicKey();
        String endpoint    = serverEndpoint;

        return "[Interface]\n" +
               "PrivateKey = " + privateKey + "\n" +
               "Address = " + player.getVpnIp() + "/32\n\n" +
               "[Peer]\n" +
               "PublicKey = " + serverPub + "\n" +
               "Endpoint = " + endpoint + "\n" +
               "AllowedIPs = 10.8.19.0/24, 10.200.0.0/16\n" +
               "PersistentKeepalive = 25\n";
    }

    /**
     * Adds per-player iptables rules so each player can reach the game subnet.
     * Called after the game network and containers are ready.
     */
    public void authorizeGameAccess(Game game, String networkId) {
        String subnet = getSubnet(networkId);
        if (subnet == null) return;

        for (Team team : game.getTeam_list()) {
            for (Player player : team.getTeam_members()) {
                if (player.getVpnIp() != null) {
                    vpnManagerClient.addPlayerToGame(player.getVpnIp(), subnet);
                    System.out.println("Authorized " + player.getPlayer()
                            + " (" + player.getVpnIp() + ") → " + subnet);
                }
            }
        }
    }

    /**
     * Removes per-player iptables rules for the game subnet on game end.
     */
    public void revokeGameAccess(Game game, String networkId) {
        String subnet = getSubnet(networkId);
        if (subnet == null) return;

        for (Team team : game.getTeam_list()) {
            for (Player player : team.getTeam_members()) {
                if (player.getVpnIp() != null) {
                    vpnManagerClient.removePlayerFromGame(player.getVpnIp(), subnet);
                    System.out.println("Revoked " + player.getPlayer()
                            + " (" + player.getVpnIp() + ") from " + subnet);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * The server's public key, read from the running interface. The WireGuard
     * container creates its keypair on first start, so there is nothing to
     * configure; reading it on every use means a regenerated key is picked up
     * at once.
     */
    private String serverPublicKey() {
        String out = execInContainer("wg", "show", WG_INTERFACE, "public-key");
        String key = out == null ? "" : out.trim();
        if (!WG_KEY.matcher(key).matches()) {
            throw new VpnUnavailableException("WireGuard server is not running, try again shortly");
        }
        return key;
    }

    private String getSubnet(String networkId) {
        try {
            Network network = docker.inspectNetworkCmd().withNetworkId(networkId).exec();
            List<Network.Ipam.Config> configs = network.getIpam().getConfig();
            if (configs != null && !configs.isEmpty()) {
                return configs.get(0).getSubnet();
            }
        } catch (Exception e) {
            System.err.println("Error inspecting network " + networkId + ": " + e.getMessage());
        }
        return null;
    }

    private String execInContainer(String... command) {
        try {
            String execId = docker.execCreateCmd(WG_CONTAINER)
                    .withCmd(command)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec().getId();

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            docker.execStartCmd(execId)
                    .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.Frame>() {
                        @Override
                        public void onNext(com.github.dockerjava.api.model.Frame frame) {
                            // Only capture stdout — stderr mixed in corrupts key output
                            if (frame.getStreamType() == com.github.dockerjava.api.model.StreamType.STDOUT) {
                                try { out.write(frame.getPayload()); }
                                catch (java.io.IOException ignored) {}
                            }
                        }
                    })
                    .awaitCompletion();

            return out.toString();
        } catch (Exception e) {
            System.err.println("execInContainer failed (" + String.join(" ", command) + "): " + e.getMessage());
            return null;
        }
    }
}

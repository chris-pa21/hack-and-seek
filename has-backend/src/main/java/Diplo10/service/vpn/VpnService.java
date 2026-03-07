package Diplo10.service.vpn;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.PlayerRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VpnService {
    private final DockerClient docker;
    private final VpnManagerClient vpnManagerClient;
    private final PlayerRepository playerRepository;
    private final AtomicInteger nextIpSuffix = new AtomicInteger(2); // Start from 10.8.19.2

    @Value("${vpn.server.public-key:SERVER_PUBKEY_HERE}")
    private String serverPublicKey;

    @Value("${vpn.server.endpoint:SERVER_IP:51820}")
    private String serverEndpoint;

    public VpnService(DockerClient docker, VpnManagerClient vpnManagerClient, PlayerRepository playerRepository) {
        this.docker = docker;
        this.vpnManagerClient = vpnManagerClient;
        this.playerRepository = playerRepository;
    }

    public void includeNetwork(String networkId) {
        final String containerId = "has-wireguard-server-1";
        try {
            // 1. Attach to network
            try {
                docker.connectToNetworkCmd()
                    .withContainerId(containerId)
                    .withNetworkId(networkId)
                    .exec();
                System.out.println("Successfully attached WireGuard to network: " + networkId);
            } catch (Exception e) {
                if (e.getMessage().contains("already exists in network") || e.getMessage().contains("already connected")) {
                    System.out.println("WireGuard already connected to network: " + networkId);
                } else {
                    throw e;
                }
            }

            // 2. Get subnet from network inspection
            String subnet = getSubnet(networkId);
            if (subnet != null) {
                // We no longer add a global subnet rule here. 
                // Rules are added per-player in authorizeGameAccess.
                System.out.println("WireGuard now has access to subnet: " + subnet);
            }

        } catch (Exception e) {
            System.err.println("Error in VpnService.includeNetwork: " + e.getMessage());
        }
    }

    public void excludeNetwork(String networkId) {
        final String containerId = "has-wireguard-server-1";
        try {
            // 1. Get subnet before detaching (to identify which rule to delete)
            String subnet = getSubnet(networkId);
            if (subnet != null) {
                vpnManagerClient.deleteSubnet(subnet);
                System.out.println("Removed firewall rule for subnet: " + subnet);
            }

            // 2. Detach from network
            docker.disconnectFromNetworkCmd()
                .withContainerId(containerId)
                .withNetworkId(networkId)
                .exec();

            System.out.println("Successfully detached WireGuard from network: " + networkId);
        } catch (Exception e) {
            System.err.println("Error disconnecting from network: " + e.getMessage());
        }
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

    public void addPeer() {

    }

    public void ensureVpnConfig(Player player) {
        System.out.println("Starting ensureVpnConfig for player: " + player.getPlayer());
        if (player.getVpnIp() != null && player.getVpnPublicKey() != null) {
            System.out.println("Player already has VPN config. IP: " + player.getVpnIp() + ", PubKey: " + player.getVpnPublicKey());
            return;
        }

        final String containerId = "has-wireguard-server-1";
        
        // 1. Assign Static IP if missing
        if (player.getVpnIp() == null) {
            long count = playerRepository.count() + 2; 
            player.setVpnIp("10.8.19." + count);
            System.out.println("Assigned VPN IP: " + player.getVpnIp());
        }

        // 2. Set Server Public Key if missing
        if (player.getVpnServerPublicKey() == null) {
            player.setVpnServerPublicKey(serverPublicKey);
            System.out.println("Assigned Server Public Key: " + serverPublicKey);
        }

        // 3. Generate Player Public Key if missing
        if (player.getVpnPublicKey() == null) {
            System.out.println("Generating new player public key...");
            String privateKey = execInContainer(containerId, "wg", "genkey").trim();
            String publicKey = execInContainer(containerId, "sh", "-c", "echo " + privateKey + " | wg pubkey").trim();
            player.setVpnPublicKey(publicKey);
            System.out.println("Generated Public Key: " + publicKey);
        } else {
            System.out.println("Using existing player public key: " + player.getVpnPublicKey());
        }

        playerRepository.save(player);
        System.out.println("Saved player VPN metadata to database.");
        addPeerToWireguard(player);
    }

    private void addPeerToWireguard(Player player) {
        final String containerId = "has-wireguard-server-1";
        // wg set wg0 peer <publicKey> allowed-ips <vpnIp>/32
        execInContainer(containerId, "wg", "set", "wg0", "peer", player.getVpnPublicKey(), "allowed-ips", player.getVpnIp() + "/32");
        System.out.println("Registered WireGuard peer for: " + player.getPlayer());
    }

    public String generateConfig(Player player) {
        // This is a dummy for now since we don't store private keys.
        // In a real app, the player provides their own public key.
        String serverPub = player.getVpnServerPublicKey() != null ? player.getVpnServerPublicKey() : serverPublicKey;
        
        return "[Interface]\n" +
               "PrivateKey = <YOUR_PRIVATE_KEY>\n" +
               "Address = " + player.getVpnIp() + "/24\n" +
               "DNS = 1.1.1.1\n\n" +
               "[Peer]\n" +
               "PublicKey = " + serverPub + "\n" +
               "Endpoint = " + serverEndpoint + "\n" +
               "AllowedIPs = 10.8.19.0/24, 172.0.0.0/8\n" +
               "PersistentKeepalive = 25\n";
    }

    public void removePeer() {

    }

    public void authorizeGameAccess(Game game, String networkId) {
        String subnet = getSubnet(networkId);
        if (subnet == null) return;

        for (Team team : game.getTeam_list()) {
            for (Player player : team.getTeam_members()) {
                if (player.getVpnIp() != null) {
                    vpnManagerClient.addPlayerToGame(player.getVpnIp(), subnet);
                    System.out.println("Authorized player " + player.getPlayer() + " (" + player.getVpnIp() + ") for subnet " + subnet);
                }
            }
        }
    }

    public void revokeGameAccess(Game game, String networkId) {
        String subnet = getSubnet(networkId);
        if (subnet == null) return;

        for (Team team : game.getTeam_list()) {
            for (Player player : team.getTeam_members()) {
                if (player.getVpnIp() != null) {
                    vpnManagerClient.removePlayerFromGame(player.getVpnIp(), subnet);
                    System.out.println("Revoked player " + player.getPlayer() + " (" + player.getVpnIp() + ") from subnet " + subnet);
                }
            }
        }
    }

    public String getVpnConfig(int peerNumber) {
        final String containerId = "has-wireguard-server-1";
        final String configPath = "/config/peer" + peerNumber + "/peer" + peerNumber + ".conf";

        try {
            String catResult = execInContainer(containerId, "cat", configPath);
            return catResult;
        } catch (Exception e) {
            System.err.println("Error fetching VPN config: " + e.getMessage());
            return null;
        }
    }

    public String getServerEndpoint() {
        return serverEndpoint;
    }

    private String execInContainer(String containerId, String... command) {
        try {
            String execId = docker.execCreateCmd(containerId)
                    .withCmd(command)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec().getId();

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            docker.execStartCmd(execId).exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.Frame>() {
                @Override
                public void onNext(com.github.dockerjava.api.model.Frame item) {
                    try {
                        outputStream.write(item.getPayload());
                    } catch (java.io.IOException ignored) {}
                }
            }).awaitCompletion();
            
            return outputStream.toString();
        } catch (Exception e) {
            return null;
        }
    }
}

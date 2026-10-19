package Diplo10.service.orchestration;

import Diplo10.entity.Game;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NetworkService {
    private final DockerClient dockerClient;

    public NetworkService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    /**
     * Creates the game's network, or returns it if it already exists. The name
     * comes from the game id rather than the game name: names are chosen freely
     * and need not be unique, and two running games sharing a network would let
     * each game's players reach the other's containers.
     */
    public String createIsolatedNetwork(Game game) {
        String networkName = networkName(game);

        Network existing = findExact(networkName);
        if (existing != null) {
            return existing.getId();
        }

        String subnet = allocateSubnet();
        String networkId = dockerClient.createNetworkCmd()
            .withName(networkName)
            .withCheckDuplicate(true)
            .withInternal(false)
            .withIpam(new Network.Ipam()
                .withDriver("default")
                .withConfig(new Network.Ipam.Config().withSubnet(subnet)))
            .exec().getId();
        System.out.println("Created game network " + networkName + " with subnet " + subnet);
        return networkId;
    }

    public String getIsolatedNetworkId(Game game) {
        Network net = findExact(networkName(game));
        return net != null ? net.getId() : null;
    }

    static String networkName(Game game) {
        return "net_game" + game.getId();
    }

    private Network findExact(String networkName) {
        // Docker's name filter is a substring match, so filter again for the exact name
        return dockerClient.listNetworksCmd().withNameFilter(networkName).exec()
            .stream()
            .filter(n -> networkName.equals(n.getName()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Picks the next free /24 in 10.200.x.0.
     * Using 10.200.x.x avoids the 172.x.x.x range that Docker Desktop adds to
     * the Windows routing table, which would otherwise intercept VPN traffic
     * before it reaches the WireGuard tunnel.
     */
    private String allocateSubnet() {
        List<Network> all = dockerClient.listNetworksCmd().exec();
        Pattern p = Pattern.compile("10\\.200\\.(\\d+)\\.0/24");
        int maxThird = 0;
        for (Network net : all) {
            if (net.getIpam() == null || net.getIpam().getConfig() == null) continue;
            for (Network.Ipam.Config cfg : net.getIpam().getConfig()) {
                if (cfg.getSubnet() == null) continue;
                Matcher m = p.matcher(cfg.getSubnet());
                if (m.matches()) {
                    maxThird = Math.max(maxThird, Integer.parseInt(m.group(1)));
                }
            }
        }
        return "10.200." + (maxThird + 1) + ".0/24";
    }

    public void deleteIsolatedNetwork(Game game) {
        String networkName = networkName(game);
        Network net = findExact(networkName);
        if (net != null) {
            try {
                dockerClient.removeNetworkCmd(net.getId()).exec();
            } catch (Exception e) {
                System.err.println("Fehler beim Löschen des Netzwerks " + networkName + ": " + e.getMessage());
            }
        }
    }
}
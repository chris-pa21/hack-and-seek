package Diplo10.service.orchestration;

import Diplo10.service.vpn.VpnService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import org.springframework.beans.factory.annotation.Autowired;
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

    public String createIsolatedNetwork(String gameName) {
        String networkName = "net_" + gameName.replaceAll("[^a-zA-Z0-9]", "_");

        // Docker name filter is a substring match — find exact match only
        Network existing = findExact(networkName);

        // If old network uses 172.x.x.x, delete it and recreate with 10.200.x.x
        if (existing != null && hasLegacySubnet(existing)) {
            try {
                dockerClient.removeNetworkCmd(existing.getId()).exec();
                System.out.println("Removed legacy 172.x.x.x network: " + networkName);
            } catch (Exception e) {
                System.err.println("Could not remove legacy network " + networkName + ": " + e.getMessage());
            }
            existing = null;
        }

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

    public String getIsolatedNetworkId(String gameName) {
        String networkName = "net_" + gameName.replaceAll("[^a-zA-Z0-9]", "_");
        Network net = findExact(networkName);
        return net != null ? net.getId() : null;
    }

    private Network findExact(String networkName) {
        return dockerClient.listNetworksCmd().withNameFilter(networkName).exec()
            .stream()
            .filter(n -> networkName.equals(n.getName()))
            .findFirst()
            .orElse(null);
    }

    private boolean hasLegacySubnet(Network network) {
        if (network.getIpam() == null || network.getIpam().getConfig() == null) return false;
        return network.getIpam().getConfig().stream()
            .anyMatch(c -> c.getSubnet() != null && c.getSubnet().startsWith("172."));
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

    public void deleteIsolatedNetwork(String gameName) {
        String networkName = "net_" + gameName.replaceAll("[^a-zA-Z0-9]", "_");
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
package Diplo10.service.orchestration;

import Diplo10.service.vpn.VpnService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NetworkService {
    private final DockerClient dockerClient;

    public NetworkService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String createIsolatedNetwork(String gameName) {
        String networkName = "net_" + gameName.replaceAll("[^a-zA-Z0-9]", "_");
        
        // Check if network already exists
        List<Network> networks = dockerClient.listNetworksCmd().withNameFilter(networkName).exec();
        String networkId;
        if (!networks.isEmpty()) {
            networkId = networks.get(0).getId();
        } else {
            networkId = dockerClient.createNetworkCmd()
                .withName(networkName)
                .withCheckDuplicate(true)
                .withInternal(false)
                .exec().getId();
        }

        return networkId;
    }

    public String getIsolatedNetworkId(String gameName) {
        String networkName = "net_" + gameName.replaceAll("[^a-zA-Z0-9]", "_");
        List<Network> networks = dockerClient.listNetworksCmd().withNameFilter(networkName).exec();
        if (!networks.isEmpty()) {
            return networks.get(0).getId();
        }
        return null;
    }

    public void deleteIsolatedNetwork(String gameName) {
        String networkName = "net_" + gameName.replaceAll("[^a-zA-Z0-9]", "_");

        List<Network> networks = dockerClient.listNetworksCmd().withNameFilter(networkName).exec();

        for(Network net : networks) {
            try {
                dockerClient.removeNetworkCmd(net.getId()).exec();
            } catch (Exception e) {
                System.err.println("Fehler beim Löschen des Netzwerks " + networkName + ": " + e.getMessage());
            }
        }
    }
}
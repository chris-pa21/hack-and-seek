package Diplo10.service.orchestration;

import Diplo10.service.VpnService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NetworkService {
    @Autowired
    VpnService vpn;
    private final DockerClient dockerClient;

    public NetworkService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String createIsolatedNetwork(String teamName) {
        String networkName = "net_" + teamName;
        vpn.includeNetwork(networkName);

        // Prüfen, ob Netzwerk bereits existiert (Reproduzierbarkeit)
        List<Network> networks = dockerClient.listNetworksCmd().withNameFilter(networkName).exec();
        if (!networks.isEmpty()) return networks.get(0).getId();

        return dockerClient.createNetworkCmd()
                .withName(networkName)
                .withCheckDuplicate(true)
                .withInternal(false) // Erlaubt Internet für Updates, falls nötig
                .exec().getId();
    }

    // TODO write another function deleteIsolatedNetwork
    public String deleteIsolatedNetwork(String teamName) {
        String networkName = "net_" + teamName;
        vpn.excludeNetwork(networkName);

        return null;
    }
}
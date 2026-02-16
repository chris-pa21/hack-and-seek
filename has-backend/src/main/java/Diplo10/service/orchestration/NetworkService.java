package Diplo10.service.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NetworkService {
    private final DockerClient dockerClient;

    public NetworkService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String createIsolatedNetwork(String teamName) {
        String networkName = "net_" + teamName;

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
}
package Diplo10.service.orchestration;

import Diplo10.entity.ContainerEntity;
import Diplo10.repository.ContainerRepository;
import com.github.dockerjava.api.DockerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrchestrationService {
    private final ContainerService containerService;
    private final NetworkService networkService;
    private final ContainerRepository repository;
    private DockerClient dockerclient;

    public OrchestrationService(ContainerService containerService, NetworkService networkService, ContainerRepository repository, DockerClient dockerclient) {
        this.containerService = containerService;
        this.networkService = networkService;
        this.repository = repository;
        this.dockerclient = dockerclient;

    }

    @Transactional
    public ContainerEntity setupGameForTeam(String teamName, String challengeImage) {
        // 1. Isoliertes Netzwerk erstellen
        String netId = networkService.createIsolatedNetwork(teamName);
        String result = containerService.spawnChallengeContainer(challengeImage, teamName, netId);
        String[] parts = result.split(":");
        String containerId = parts[0];
        Integer port = Integer.parseInt(parts[1]);

        // 2. Container im Netzwerk starten

        // 3. Im Repository speichern für Reproduzierbarkeit/Monitoring
        ContainerEntity entity = ContainerEntity.builder()
                .containerId(containerId)
                .teamName(teamName)
                .networkId(netId)
                .image(challengeImage)
                .publicPort(port)
                .build();

        return repository.save(entity);
    }
    @Transactional
    public void terminateGame(String teamName) {
        List<ContainerEntity> containers = repository.findByTeamName(teamName);

        for (ContainerEntity entity : containers) {
            try {
                dockerclient.removeContainerCmd(entity.getContainerId())
                        .withForce(true)
                        .exec();

                repository.delete(entity);

                dockerclient.removeNetworkCmd(entity.getNetworkId()).exec();
            } catch (Exception e) {
                System.err.println("Fehler beim Entfernen von Container " + entity.getContainerId() + ": " + e.getMessage());
            }
        }
        networkService.deleteIsolatedNetwork(teamName);
    }
}
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
    public void setupGameForTeam(String teamName, String challengeImage) {
        // 1. Isoliertes Netzwerk erstellen
        String netId = networkService.createIsolatedNetwork(teamName);

        // 2. Container im Netzwerk starten
        String containerId = containerService.spawnChallengeContainer(challengeImage, teamName, netId);

        // 3. Im Repository speichern für Reproduzierbarkeit/Monitoring
        ContainerEntity entity = ContainerEntity.builder()
                .containerId(containerId)
                .teamName(teamName)
                .networkId(netId)
                .image(challengeImage) // Wichtig: Das Image mit speichern!
                .build();
        repository.save(entity);
    }
    @Transactional
    public void terminateGame(String teamName) {
        // 1. Alle Container des Teams aus PostgreSQL suchen
        List<ContainerEntity> containers = repository.findByTeamName(teamName);

        for (ContainerEntity entity : containers) {
            try {
                // 2. Docker Container stoppen und entfernen
                dockerclient.removeContainerCmd(entity.getContainerId())
                        .withForce(true)
                        .exec();

                // 3. Aus Datenbank löschen (Reproduzierbarkeit: Weg ist weg)
                repository.delete(entity);

                // 4. Netzwerk entfernen
                dockerclient.removeNetworkCmd(entity.getNetworkId()).exec();
            } catch (Exception e) {
                // Log Fehler, falls Container bereits manuell gelöscht wurde
            }
        }
    }
}
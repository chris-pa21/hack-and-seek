package Diplo10.service.orchestration;

import Diplo10.entity.ContainerEntity;
import Diplo10.repository.ContainerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrchestrationService {
    private final ContainerService containerService;
    private final NetworkService networkService;
    private final ContainerRepository repository;

    public OrchestrationService(ContainerService containerService, NetworkService networkService, ContainerRepository repository) {
        this.containerService = containerService;
        this.networkService = networkService;
        this.repository = repository;
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
}
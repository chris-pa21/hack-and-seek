package Diplo10.service.orchestration;

import Diplo10.entity.ContainerEntity;
import Diplo10.entity.Game;
import Diplo10.entity.Team;
import Diplo10.repository.ContainerRepository;
import com.github.dockerjava.api.DockerClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrchestrationService {
    private final ContainerService containerService;
    private final NetworkService networkService;
    private final ContainerRepository repository;
    private final DockerClient dockerclient;

    public OrchestrationService(ContainerService containerService, NetworkService networkService, ContainerRepository repository, DockerClient dockerclient) {
        this.containerService = containerService;
        this.networkService = networkService;
        this.repository = repository;
        this.dockerclient = dockerclient;
    }

    @Transactional
    public ContainerEntity setupTeamContainer(Game game, Team team, String networkId) {
        String teamName = team.getName();
        String challengeImage = team.getChallenge();

        // Spawn container in the game's shared network
        SpawnResult result = containerService.spawnChallengeContainer(challengeImage, teamName, networkId);
        String containerId = result.containerId();
        String ipAddress = result.ipAddress();
        String flag = result.flag();
        int points = result.points();
        String rootPassword = result.rootPassword();
        String flagPath = result.flagPath();

        // Save to DB with proper relationships
        ContainerEntity entity = ContainerEntity.builder()
                .containerId(containerId)
                .team(team)
                .game(game)
                .networkId(networkId)
                .image(challengeImage)
                .ipAddress(ipAddress)
                .flag(flag)
                .points(points)
                .rootPassword(rootPassword)
                .flagPath(flagPath)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(entity);
    }

    @Transactional
    public void terminateGame(Game game) {
        List<ContainerEntity> containers = repository.findByGame(game);

        for (ContainerEntity entity : containers) {
            try {
                dockerclient.removeContainerCmd(entity.getContainerId())
                        .withForce(true)
                        .exec();
            } catch (Exception e) {
                System.err.println("Error removing container " + entity.getContainerId() + ": " + e.getMessage());
            }
        }
        repository.deleteAll(containers);

        // Remove the shared game network
        networkService.deleteIsolatedNetwork(game);
    }
}

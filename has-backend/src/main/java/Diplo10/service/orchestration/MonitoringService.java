package Diplo10.service.orchestration;

import Diplo10.entity.ContainerEntity;
import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.SlaEvent;
import Diplo10.entity.Team;
import Diplo10.repository.ContainerRepository;
import Diplo10.repository.GameRepository;
import Diplo10.repository.PlayerRepository;
import Diplo10.repository.SlaEventRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.GameService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonitoringService {
    private final DockerClient dockerClient;
    private final GameRepository gameRepository;
    private final ContainerRepository containerRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final SlaEventRepository slaEventRepository;
    private final GameService gameService;

    private static final int PENALTY_POINTS = 50;

    public MonitoringService(DockerClient dockerClient, GameRepository gameRepository,
                             ContainerRepository containerRepository, TeamRepository teamRepository,
                             PlayerRepository playerRepository, SlaEventRepository slaEventRepository,
                             GameService gameService) {
        this.dockerClient = dockerClient;
        this.gameRepository = gameRepository;
        this.containerRepository = containerRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.slaEventRepository = slaEventRepository;
        this.gameService = gameService;
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void monitorContainers() {
        List<Game> activeGames = gameRepository.findAll().stream()
                .filter(g -> g.getStarted_at() != null && g.getFinished_at() == null)
                .toList();

        for (Game game : activeGames) {
            List<ContainerEntity> containers = containerRepository.findByGameAndDisqualifiedFalse(game);

            for (ContainerEntity container : containers) {
                runHealthChecks(game, container);
            }
        }
    }

    private void runHealthChecks(Game game, ContainerEntity container) {
        int gameId = Math.toIntExact(game.getId());
        String teamName = container.getTeam().getName();
        String containerId = container.getContainerId();

        // Check 1: Flag integrity
        if (!checkFlagIntegrity(gameId, teamName, container)) {
            disqualify(game, container, "FLAG_INTEGRITY", "Flag was deleted or modified");
            return;
        }

        // Check 2: Service availability
        if (!checkServiceAvailability(gameId, teamName, container)) {
            disqualify(game, container, "SERVICE_AVAILABILITY", "Web service is not responding");
            return;
        }

        // Check 3: Functionality (healthcheck.sh)
        if (!checkFunctionality(gameId, teamName, container)) {
            disqualify(game, container, "FUNCTIONALITY", "Healthcheck script failed");
            return;
        }

        // All checks passed
        logSlaEvent(gameId, teamName, containerId, "ALL", true, "All checks passed");
    }

    private boolean checkFlagIntegrity(int gameId, String teamName, ContainerEntity container) {
        try {
            String flagPath = container.getFlagPath() != null ? container.getFlagPath() : "/home/flag.txt";
            String output = execInContainer(container.getContainerId(), "timeout", "5", "cat", flagPath);

            if (output == null || !output.trim().equals(container.getFlag())) {
                logSlaEvent(gameId, teamName, container.getContainerId(), "FLAG_INTEGRITY", false, "Flag mismatch or missing");
                return false;
            }

            logSlaEvent(gameId, teamName, container.getContainerId(), "FLAG_INTEGRITY", true, "OK");
            return true;
        } catch (Exception e) {
            logSlaEvent(gameId, teamName, container.getContainerId(), "FLAG_INTEGRITY", false, "Error: " + e.getMessage());
            return false;
        }
    }

    private boolean checkServiceAvailability(int gameId, String teamName, ContainerEntity container) {
        try {
            String output = execInContainer(container.getContainerId(),
                    "timeout", "5", "sh", "-c", "curl -s -o /dev/null -w '%{http_code}' http://localhost:80/");

            if (output == null || output.trim().isEmpty()) {
                logSlaEvent(gameId, teamName, container.getContainerId(), "SERVICE_AVAILABILITY", false, "No response");
                return false;
            }

            int statusCode = Integer.parseInt(output.trim().replace("'", ""));
            if (statusCode < 200 || statusCode >= 400) {
                logSlaEvent(gameId, teamName, container.getContainerId(), "SERVICE_AVAILABILITY", false, "HTTP " + statusCode);
                return false;
            }

            logSlaEvent(gameId, teamName, container.getContainerId(), "SERVICE_AVAILABILITY", true, "HTTP " + statusCode);
            return true;
        } catch (Exception e) {
            logSlaEvent(gameId, teamName, container.getContainerId(), "SERVICE_AVAILABILITY", false, "Error: " + e.getMessage());
            return false;
        }
    }

    private boolean checkFunctionality(int gameId, String teamName, ContainerEntity container) {
        try {
            execInContainer(container.getContainerId(), "timeout", "5", "/healthcheck.sh");

            logSlaEvent(gameId, teamName, container.getContainerId(), "FUNCTIONALITY", true, "OK");
            return true;
        } catch (Exception e) {
            logSlaEvent(gameId, teamName, container.getContainerId(), "FUNCTIONALITY", false, "Error: " + e.getMessage());
            return false;
        }
    }

    private void disqualify(Game game, ContainerEntity container, String checkType, String reason) {
        container.setDisqualified(true);
        containerRepository.save(container);

        Team team = container.getTeam();
        team.setScore(team.getScore() - PENALTY_POINTS);
        team.setDefeated(true);
        team.setDisqualified(true);
        teamRepository.save(team);

        for (Player member : team.getTeam_members()) {
            member.setPoints((member.getPoints() != null ? member.getPoints() : 0) - PENALTY_POINTS);
            playerRepository.save(member);
        }

        // Stop and remove the Docker container (but keep the DB record for endGame to clean up)
        try {
            dockerClient.stopContainerCmd(container.getContainerId()).exec();
            dockerClient.removeContainerCmd(container.getContainerId()).exec();
        } catch (Exception e) {
            System.err.println("Failed to remove disqualified container " + container.getContainerId() + ": " + e.getMessage());
        }

        // Check if only one team remains → game over
        List<Team> remainingTeams = game.getTeam_list().stream()
                .filter(t -> !t.isDefeated())
                .toList();

        if (remainingTeams.size() == 1) {
            gameService.endGame(game, remainingTeams.get(0));
        }

        System.out.println("DISQUALIFIED team " + team.getName()
                + " - " + checkType + ": " + reason);
    }

    private String execInContainer(String containerId, String... cmd) throws Exception {
        String execId = dockerClient.execCreateCmd(containerId)
                .withCmd(cmd)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec().getId();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        dockerClient.execStartCmd(execId).exec(new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame frame) {
                try {
                    outputStream.write(frame.getPayload());
                } catch (Exception e) {
                    // ignore
                }
            }
        }).awaitCompletion();

        Long exitCode = dockerClient.inspectExecCmd(execId).exec().getExitCodeLong();
        if (exitCode != null && exitCode != 0) {
            throw new RuntimeException("Exit code " + exitCode);
        }

        return outputStream.toString().trim();
    }

    private void logSlaEvent(int gameId, String teamName, String containerId, String checkType, boolean passed, String details) {
        SlaEvent event = SlaEvent.builder()
                .gameId(gameId)
                .teamName(teamName)
                .containerId(containerId)
                .checkType(checkType)
                .passed(passed)
                .details(details)
                .checkedAt(LocalDateTime.now())
                .build();
        slaEventRepository.save(event);
    }
}

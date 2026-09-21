package Diplo10.service;

import Diplo10.exception.NotFoundException;
import Diplo10.entity.ContainerEntity;
import Diplo10.entity.FlagSubmission;
import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.ContainerRepository;
import Diplo10.repository.FlagSubmissionRepository;
import Diplo10.repository.GameRepository;
import Diplo10.repository.PlayerRepository;
import Diplo10.repository.TeamRepository;
import com.github.dockerjava.api.DockerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FlagService {

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final ContainerRepository containerRepository;
    private final FlagSubmissionRepository flagSubmissionRepository;
    private final PlayerRepository playerRepository;
    private final DockerClient dockerClient;
    private final GameService gameService;

    @Transactional
    public Map<String, Object> submitFlag(Long gameId, String username, String flagString) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));

        if (game.getFinished_at() != null) {
            return Map.of("success", false, "message", "This game is already finished");
        }

        // Find the submitter's team in this game
        Team attackingTeam = null;
        for (Team team : game.getTeam_list()) {
            boolean isMember = team.getTeam_members().stream()
                    .anyMatch(p -> p.getPlayer().equals(username));
            if (isMember) {
                attackingTeam = team;
                break;
            }
        }

        if (attackingTeam == null) {
            return Map.of("success", false, "message", "You are not part of any team in this game");
        }

        if (attackingTeam.isDefeated()) {
            return Map.of("success", false, "message", "Your team has been defeated and cannot submit flags");
        }

        // Search all containers in this game for the matching flag
        List<ContainerEntity> containers = containerRepository.findByGame(game);
        ContainerEntity targetContainer = null;
        for (ContainerEntity c : containers) {
            if (flagString.equals(c.getFlag())) {
                targetContainer = c;
                break;
            }
        }

        if (targetContainer == null) {
            return Map.of("success", false, "message", "Invalid flag");
        }

        Team targetTeam = targetContainer.getTeam();

        // Can't submit own team's flag
        if (targetTeam.getName().equals(attackingTeam.getName())) {
            return Map.of("success", false, "message", "You cannot submit your own flag");
        }

        // Check if this flag was already captured
        if (flagSubmissionRepository.existsByGameIdAndDefeatedTeam(
                Math.toIntExact(gameId), targetTeam.getName())) {
            return Map.of("success", false, "message", "This flag has already been captured");
        }

        int points = targetContainer.getPoints() != null ? targetContainer.getPoints() : 100;

        // Record the submission
        FlagSubmission submission = FlagSubmission.builder()
                .gameId(Math.toIntExact(gameId))
                .attackingTeam(attackingTeam.getName())
                .defeatedTeam(targetTeam.getName())
                .flag(flagString)
                .pointsAwarded(points)
                .submittedAt(LocalDateTime.now())
                .build();
        flagSubmissionRepository.save(submission);

        // Award points to attacking team
        attackingTeam.setScore(attackingTeam.getScore() + points);
        teamRepository.save(attackingTeam);

        // Award points to each player in the attacking team
        for (Player member : attackingTeam.getTeam_members()) {
            member.setPoints((member.getPoints() != null ? member.getPoints() : 0) + points);
            playerRepository.save(member);
        }

        // Defeat the target team
        targetTeam.setDefeated(true);
        teamRepository.save(targetTeam);

        // Stop and remove the defeated team's container
        try {
            dockerClient.stopContainerCmd(targetContainer.getContainerId()).exec();
            dockerClient.removeContainerCmd(targetContainer.getContainerId()).exec();
            containerRepository.delete(targetContainer);
        } catch (Exception e) {
            System.err.println("Failed to stop/remove container " + targetContainer.getContainerId() + ": " + e.getMessage());
        }

        // Check if only one team remains → game over
        List<Team> remainingTeams = game.getTeam_list().stream()
                .filter(t -> !t.isDefeated())
                .toList();

        boolean gameOver = remainingTeams.size() == 1;
        String winnerName = null;

        if (gameOver) {
            Team winner = remainingTeams.get(0);
            winnerName = winner.getName();
            gameService.endGame(game, winner);
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("message", gameOver
                ? "Flag captured! +" + points + " points. Game over!"
                : "Flag captured! +" + points + " points");
        result.put("points", points);
        result.put("defeatedTeam", targetTeam.getName());
        result.put("gameOver", gameOver);
        result.put("winnerTeam", winnerName);
        return result;
    }

    public List<FlagSubmission> getDefeatLog(Long gameId) {
        return flagSubmissionRepository.findByGameIdOrderBySubmittedAtDesc(Math.toIntExact(gameId));
    }
}

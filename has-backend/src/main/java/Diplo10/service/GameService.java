package Diplo10.service;

import Diplo10.exception.NotFoundException;
import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.GameRepository;
import Diplo10.repository.PlayerRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.orchestration.NetworkService;
import Diplo10.service.orchestration.OrchestrationService;
import Diplo10.service.vpn.VpnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final OrchestrationService orchestrationService;
    private final NetworkService networkService;
    private final VpnService vpnService;

    public void addTeam(Team team, Long gameId) {
        Game game = this.gameRepository.findById(gameId).get();
        game.addTeam(team);
        gameRepository.save(game);
    }

    public void addPlayer(Long gameId, String teamId, Player player) {
        Game game = gameRepository.findById(gameId).get();
        Team team = teamRepository.findByGameIdAndName(Math.toIntExact(gameId), teamId).get();
        team.addPlayer(player);
        teamRepository.save(team);
    }

    public void deleteTeam(String teamId, Long gameId) {
        Team team = this.teamRepository.findByName(teamId).get();
        Game game = this.gameRepository.findById(gameId).get();
        game.deleteTeam(team);
        gameRepository.save(game);
    }

    /**
     * Starts a game, all or nothing: a game is only fair if every team has a
     * container, so any failure tears down what was already created and leaves
     * the game in its lobby state rather than running and unplayable.
     */
    @Transactional
    public Game startGame(Game game) {
        // Create one shared network for the entire game
        String networkId = networkService.createIsolatedNetwork(game.getName());

        // Start a container for each team
        try {
            for (Team team : game.getTeam_list()) {
                orchestrationService.setupTeamContainer(game, team, networkId);
                // Ensure all team members have a VPN config assigned
                for (Player p : team.getTeam_members()) {
                    vpnService.ensureVpnConfig(p);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to start game " + game.getName() + ": " + e.getMessage());
            orchestrationService.terminateGame(game);
            throw new IllegalStateException(
                    "Could not start the game: " + e.getMessage() + ". No teams were started.");
        }

        game.setStarted_at(LocalDateTime.now());
        game = gameRepository.save(game);

        // Attach WireGuard to the network
        vpnService.includeNetwork(networkId);
        // Add per-player firewall rules
        vpnService.authorizeGameAccess(game, networkId);

        return game;
    }

    public void deletePlayer(String teamId, Long gameId, Player player) {
        Team team = this.teamRepository.findByGameIdAndName(Math.toIntExact(gameId), teamId).get();
        team.deletePlayer(player);
        teamRepository.save(team);
    }

    public void endGame(Game game, Team winner) {
        game.setFinished_at(LocalDateTime.now());
        game.setWinnerTeam(winner);
        gameRepository.save(game);

        // Stop and remove all remaining containers + network
        orchestrationService.terminateGame(game);

        // Revoke access and detach from network
        String networkId = networkService.getIsolatedNetworkId(game.getName());
        if (networkId != null) {
            vpnService.revokeGameAccess(game, networkId);
            vpnService.excludeNetwork(networkId);
        }
    }

    public boolean dismissGame(Long gameId, String username) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));

        // Find the player
        Player player = playerRepository.findByPlayer(username)
                .orElseThrow(() -> new NotFoundException("Player not found"));

        // Remove player from their team in this game
        for (Team team : game.getTeam_list()) {
            if (team.getTeam_members().removeIf(m -> m.getPlayer().equals(username))) {
                teamRepository.save(team);
                break;
            }
        }

        // Hand the game over if the creator is the one leaving: start and stop
        // are creator-only, so a creator-less game could never be run again
        if (game.getCreated_by() != null && game.getCreated_by().getPlayer().equals(username)) {
            game.setCreated_by(remainingPlayer(game, username));
            gameRepository.save(game);
        }

        // Check if any players remain across all teams
        boolean anyPlayersLeft = game.getTeam_list().stream()
                .anyMatch(t -> !t.getTeam_members().isEmpty());

        if (!anyPlayersLeft) {
            // Nobody is left to play it. Tear down the running containers and
            // network first, or they would keep running with nothing
            // referencing them.
            if (game.getStarted_at() != null && game.getFinished_at() == null) {
                orchestrationService.terminateGame(game);
            }
            gameRepository.delete(game);
            return true; // game deleted
        }

        return false; // game still exists
    }

    /**
     * Any player still in the game other than the one leaving, or null when
     * nobody remains.
     */
    private Player remainingPlayer(Game game, String leaving) {
        return game.getTeam_list().stream()
                .flatMap(team -> team.getTeam_members().stream())
                .filter(member -> !member.getPlayer().equals(leaving))
                .findFirst()
                .orElse(null);
    }
}

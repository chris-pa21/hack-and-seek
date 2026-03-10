package Diplo10.service;

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

    @Transactional
    public Game startGame(Game game) {
        game.setStarted_at(LocalDateTime.now());
        game = gameRepository.save(game);

        // Create one shared network for the entire game
        String networkId = networkService.createIsolatedNetwork(game.getName());

        // Start a container for each team
        for (Team team : game.getTeam_list()) {
            try {
                orchestrationService.setupTeamContainer(game, team, networkId);
                // Ensure all team members have a VPN config assigned
                for (Player p : team.getTeam_members()) {
                    vpnService.ensureVpnConfig(p);
                }
            } catch (Exception e) {
                System.err.println("Failed to start container for team " + team.getName() + ": " + e.getMessage());
            }
        }

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
                .orElseThrow(() -> new NullPointerException("Game not found"));

        // Find the player
        Player player = playerRepository.findByPlayer(username)
                .orElseThrow(() -> new NullPointerException("Player not found"));

        // Remove player from their team in this game
        for (Team team : game.getTeam_list()) {
            if (team.getTeam_members().removeIf(m -> m.getPlayer().equals(username))) {
                teamRepository.save(team);
                break;
            }
        }

        // Also remove creator reference if this user is the creator
        if (game.getCreated_by() != null && game.getCreated_by().getPlayer().equals(username)) {
            game.setCreated_by(null);
            gameRepository.save(game);
        }

        // Check if any players remain across all teams
        boolean anyPlayersLeft = game.getTeam_list().stream()
                .anyMatch(t -> !t.getTeam_members().isEmpty());

        if (!anyPlayersLeft) {
            // No players left — delete the game and its teams
            gameRepository.delete(game);
            return true; // game deleted
        }

        return false; // game still exists
    }
}

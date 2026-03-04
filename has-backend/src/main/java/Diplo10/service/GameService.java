package Diplo10.service;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.GameRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.orchestration.NetworkService;
import Diplo10.service.orchestration.OrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final OrchestrationService orchestrationService;
    private final NetworkService networkService;

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

    public Game startGame(Game game) {
        game.setStarted_at(LocalDateTime.now());
        game = gameRepository.save(game);

        // Create one shared network for the entire game
        String networkId = networkService.createIsolatedNetwork(game.getName());

        // Start a container for each team
        for (Team team : game.getTeam_list()) {
            try {
                orchestrationService.setupTeamContainer(game, team, networkId);
            } catch (Exception e) {
                System.err.println("Failed to start container for team " + team.getName() + ": " + e.getMessage());
            }
        }

        return game;
    }

    public void deletePlayer(String teamId, Long gameId, Player player) {
        Team team = this.teamRepository.findByGameIdAndName(Math.toIntExact(gameId), teamId).get();
        team.deletePlayer(player);
        teamRepository.save(team);
    }
}

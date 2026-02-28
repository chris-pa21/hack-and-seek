package Diplo10.service;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.GameRepository;
import Diplo10.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;

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

    public void deletePlayer(String teamId, Long gameId, Player player) {
        Team team = this.teamRepository.findByGameIdAndName(Math.toIntExact(gameId), teamId).get();
        team.deletePlayer(player);
        teamRepository.save(team);
    }
}

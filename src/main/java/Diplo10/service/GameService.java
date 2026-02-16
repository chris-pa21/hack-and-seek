package Diplo10.service;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.GameRepository;
import Diplo10.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GameService {
    private GameRepository gameRepository;
    private TeamRepository teamRepository;

    public void addTeam(Team team, Integer game_id){
        Game game = this.gameRepository.findById(game_id).get();
        game.addTeam(team);
    }
    public void addPlayer(int gameId, String teamId, Player player){
        Game game = gameRepository.findById(gameId).get();
        Team team = teamRepository.findByGame_idAndName(teamId, gameId).get();
        team.addPlayer(player);
    }

}

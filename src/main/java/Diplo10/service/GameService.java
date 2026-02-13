package Diplo10.service;

import Diplo10.entity.Game;
import Diplo10.entity.Team;
import Diplo10.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GameService {
    private GameRepository gameRepository;

    public void addTeam(Team team, Integer game_id){
        Game game = this.gameRepository.findById(game_id).get();
        game.addTeam(team);
    }

}

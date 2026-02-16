package Diplo10.controller;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.GameRepository;
import Diplo10.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private GameRepository gameRepository;
    private GameService gameService;

    @PostMapping("/create")
    public ResponseEntity<Void> createGame(@RequestBody Game game){
        this.gameRepository.save(game);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/team")
    public ResponseEntity<Void> createTeam(@PathVariable int id, @RequestParam String name){
        Team team = new Team(name);
        this.gameService.addTeam(team, id);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{gameId}/{teamId}/addUser")
    public ResponseEntity<Void> addPlayer(@PathVariable int gameId, @PathVariable String teamId, @RequestBody Player player){
        this.gameService.addPlayer(gameId, teamId, player);
        return ResponseEntity.ok().build();
    }







}

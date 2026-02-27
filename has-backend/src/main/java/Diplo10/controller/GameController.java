package Diplo10.controller;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.GameRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private GameRepository gameRepository;
    private GameService gameService;
    private TeamRepository teamRepository;

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

    @GetMapping("/")
    public ResponseEntity<List> showGames(){
        List games = this.gameRepository.findAll();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/{gameId}/teams")
    public ResponseEntity<List> showTeams(@PathVariable int gameId){
        List teams = this.teamRepository.findByGameId(gameId);
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{gameId}/teams/{teamId}")
    public ResponseEntity<Team> showTeams(@PathVariable int gameId, @PathVariable String teamId){
        Team teams = this.teamRepository.findByGameIdAndName(gameId, teamId).get();
        return ResponseEntity.ok(teams);
    }

    @PatchMapping("/{gameId}")
    public ResponseEntity<Void> editGame(@PathVariable int gameId, @RequestParam String name){
        Game game = this.gameRepository.findById(gameId).get();
        game.setName(name);
        return ResponseEntity.ok().build();
    }


    //====================================Image??=======================================
    /*@PatchMapping("/{gameId}/teams/{teamId}")
    public ResponseEntity<Void> editTeamInGame(@PathVariable int gameId, @PathVariable String teamId, @RequestParam String name){
        Game game = this.gameRepository.findById(gameId).get();

        game.setName(name);
        return ResponseEntity.ok().build();
    }*/


    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable int gameId){
        this.gameRepository.deleteById(gameId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{gameId}/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable int gameId, @PathVariable String teamId){
        this.gameService.deleteTeam(teamId, gameId);
        return ResponseEntity.ok().build();
    }

    /*@DeleteMapping("/{gameId}/teams/{teamId}")
    public ResponseEntity<Void> deletePlayerInTeam(@PathVariable int gameId, @PathVariable String teamId, @RequestParam Player player){
        this.gameService.deletePlayer(teamId, gameId, player);
        return ResponseEntity.ok().build();
    }*/
}

package Diplo10.controller;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.GameRepository;
import Diplo10.repository.PlayerRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    /**
     * Full game creation: accepts a name + list of teams (each with players & challenge).
     */
    @PostMapping("/create")
    public ResponseEntity<Game> createGame(@RequestBody CreateGameRequest req) {
        // 1. Save the bare game first to get an ID
        Game game = new Game();
        game.setName(req.name());
        game.setCreated_at(new Date());
        game.setTeam_list(new ArrayList<>());
        game = gameRepository.save(game);

        // 2. Build and attach each team
        for (CreateGameRequest.TeamRequest teamReq : req.teams()) {
            Team team = new Team();
            team.setName(teamReq.name() + "_game" + game.getId()); // make name unique per game
            team.setGameId(Math.toIntExact(game.getId()));
            team.setChallenge(teamReq.challenge());
            team.setTeam_members(new ArrayList<>());

            // Resolve players by username
            for (String username : teamReq.players()) {
                playerRepository.findByPlayer(username).ifPresent(team::addPlayer);
            }

            teamRepository.save(team);
            game.addTeam(team);
        }

        game = gameRepository.save(game);
        return ResponseEntity.status(201).body(game);
    }

    @PostMapping("/{id}/team")
    public ResponseEntity<Void> createTeam(@PathVariable Long id, @RequestParam String name) {
        Team team = new Team(name);
        this.gameService.addTeam(team, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/{teamId}/addUser")
    public ResponseEntity<Void> addPlayer(@PathVariable Long gameId, @PathVariable String teamId, @RequestBody Player player) {
        this.gameService.addPlayer(gameId, teamId, player);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<Game>> showGames() {
        List<Game> games = this.gameRepository.findAll();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/{gameId}/teams")
    public ResponseEntity<List<Team>> showTeams(@PathVariable Long gameId) {
        List<Team> teams = this.teamRepository.findByGameId(Math.toIntExact(gameId));
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{gameId}/teams/{teamId}")
    public ResponseEntity<Team> showTeam(@PathVariable Long gameId, @PathVariable String teamId) {
        Team team = this.teamRepository.findByGameIdAndName(Math.toIntExact(gameId), teamId).get();
        return ResponseEntity.ok(team);
    }

    @PatchMapping("/{gameId}")
    public ResponseEntity<Void> editGame(@PathVariable Long gameId, @RequestParam String name) {
        Game game = this.gameRepository.findById(gameId).get();
        game.setName(name);
        gameRepository.save(game);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long gameId) {
        this.gameRepository.deleteById(gameId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{gameId}/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long gameId, @PathVariable String teamId) {
        this.gameService.deleteTeam(teamId, gameId);
        return ResponseEntity.ok().build();
    }
}

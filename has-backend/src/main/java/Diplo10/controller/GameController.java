package Diplo10.controller;

import Diplo10.exception.NotFoundException;
import Diplo10.dto.ChallengeDTO;
import Diplo10.dto.DefeatLogEntry;
import Diplo10.entity.ContainerEntity;
import Diplo10.entity.FlagSubmission;
import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.ContainerRepository;
import Diplo10.repository.GameRepository;
import Diplo10.repository.PlayerRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.FlagService;
import Diplo10.service.GameService;
import Diplo10.service.orchestration.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final ImageService imageService;
    private final FlagService flagService;
    private final ContainerRepository containerRepository;

    @GetMapping("/challenges")
    public ResponseEntity<List<ChallengeDTO>> getChallenges() {
        return ResponseEntity.ok(imageService.listChallengeImages());
    }

    /**
     * Full game creation: accepts a name + list of teams (each with players & challenge).
     */
    @PostMapping("/create")
    public ResponseEntity<Game> createGame(@RequestBody CreateGameRequest req, Authentication authentication) {
        // 1. Save the bare game first to get an ID
        Game game = new Game();
        game.setName(req.name());
        game.setCreated_at(new Date());
        game.setTeam_list(new ArrayList<>());

        // Set the creator to the authenticated user
        String username = authentication.getName();
        playerRepository.findByPlayer(username).ifPresent(game::setCreated_by);

        game = gameRepository.save(game);

        // 2. Build and attach each team
        for (CreateGameRequest.TeamRequest teamReq : req.teams()) {
            Team team = new Team();
            team.setName(teamReq.name() + "_game" + game.getId()); // make name unique per game
            team.setGameId(Math.toIntExact(game.getId()));
            team.setChallenge(teamReq.challenge());
            team.setTeam_members(new ArrayList<>());

            // Resolve players by username
            for (String playerName : teamReq.players()) {
                playerRepository.findByPlayer(playerName).ifPresent(team::addPlayer);
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
    public ResponseEntity<List<Game>> showGames(Authentication authentication) {
        String username = authentication.getName();
        List<Game> games = this.gameRepository.findGamesByPlayer(username);
        return ResponseEntity.ok(games);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<Game> getGame(@PathVariable Long gameId) {
        Game game = this.gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameId}/start")
    public ResponseEntity<Game> startGame(@PathVariable Long gameId, Authentication authentication) {
        Game game = this.gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));
        String username = authentication.getName();
        if (game.getCreated_by() == null || !game.getCreated_by().getPlayer().equals(username)) {
            return ResponseEntity.status(403).build();
        }
        if (game.getStarted_at() != null) {
            return ResponseEntity.badRequest().build();
        }
        if (game.getTeam_list() == null || game.getTeam_list().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        game = gameService.startGame(game);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameId}/stop")
    public ResponseEntity<Game> stopGame(@PathVariable Long gameId, Authentication authentication) {
        Game game = this.gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));
        String username = authentication.getName();
        if (game.getCreated_by() == null || !game.getCreated_by().getPlayer().equals(username)) {
            return ResponseEntity.status(403).build();
        }
        if (game.getStarted_at() == null || game.getFinished_at() != null) {
            return ResponseEntity.badRequest().build();
        }

        // Winner = non-defeated team with highest score, null if draw
        List<Team> alive = game.getTeam_list().stream()
                .filter(t -> !t.isDefeated())
                .toList();
        int maxScore = alive.stream().mapToInt(Team::getScore).max().orElse(0);
        List<Team> topTeams = alive.stream()
                .filter(t -> t.getScore() == maxScore)
                .toList();
        Team winner = topTeams.size() == 1 ? topTeams.get(0) : null;

        gameService.endGame(game, winner);
        return ResponseEntity.ok(game);
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

    @PostMapping("/{gameId}/submit-flag")
    public ResponseEntity<Map<String, Object>> submitFlag(
            @PathVariable Long gameId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String flag = body.get("flag");
        if (flag == null || flag.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Flag is required"));
        }
        String username = authentication.getName();
        Map<String, Object> result = flagService.submitFlag(gameId, username, flag.trim());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{gameId}/defeat-log")
    public ResponseEntity<List<DefeatLogEntry>> getDefeatLog(@PathVariable Long gameId,
                                                             Authentication authentication) {
        Game game = this.gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));

        if (teamOf(game, authentication.getName()) == null) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(flagService.getDefeatLog(gameId));
    }

    @PostMapping("/{gameId}/dismiss")
    public ResponseEntity<Map<String, Object>> dismissGame(@PathVariable Long gameId, Authentication authentication) {
        String username = authentication.getName();
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));

        if (game.getFinished_at() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Game is not finished yet"));
        }

        boolean deleted = gameService.dismissGame(gameId, username);
        return ResponseEntity.ok(Map.of("success", true, "gameDeleted", deleted));
    }

    @GetMapping("/{gameId}/credentials")
    public ResponseEntity<Map<String, String>> getCredentials(@PathVariable Long gameId, Authentication authentication) {
        String username = authentication.getName();
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));

        // Find the team the authenticated user belongs to
        Team userTeam = teamOf(game, username);

        if (userTeam == null) {
            return ResponseEntity.status(403).build();
        }

        ContainerEntity container = containerRepository.findByTeam(userTeam).orElse(null);
        if (container == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "rootPassword", container.getRootPassword(),
                "ipAddress", container.getIpAddress()
        ));
    }

    /**
     * The team the given player belongs to in this game, or null if they are
     * not taking part in it.
     */
    private Team teamOf(Game game, String username) {
        if (game.getTeam_list() == null) {
            return null;
        }
        return game.getTeam_list().stream()
                .filter(t -> t.getTeam_members() != null && t.getTeam_members().stream()
                        .anyMatch(m -> m.getPlayer().equals(username)))
                .findFirst()
                .orElse(null);
    }
}

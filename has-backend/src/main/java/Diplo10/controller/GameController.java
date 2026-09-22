package Diplo10.controller;

import Diplo10.exception.NotFoundException;
import Diplo10.dto.ChallengeDTO;
import Diplo10.dto.DefeatLogEntry;
import Diplo10.dto.GameView;
import Diplo10.entity.ContainerEntity;
import Diplo10.entity.Game;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every endpoint needs a logged-in user (SecurityConfig). On top of that, each
 * game endpoint checks who the caller is in that game: the creator alone may
 * start and stop it, and only the creator and the players taking part may see
 * or act on it. Anyone else gets 403.
 */
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
    public ResponseEntity<GameView> createGame(@RequestBody CreateGameRequest req, Authentication authentication) {
        validate(req);

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
        return ResponseEntity.status(201).body(view(game));
    }

    @GetMapping
    public ResponseEntity<List<GameView>> showGames(Authentication authentication) {
        String username = authentication.getName();
        List<GameView> games = this.gameRepository.findGamesByPlayer(username).stream()
                .map(this::view)
                .toList();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameView> getGame(@PathVariable Long gameId, Authentication authentication) {
        Game game = findGame(gameId);
        if (!canView(game, authentication.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(view(game));
    }

    @PostMapping("/{gameId}/start")
    public ResponseEntity<GameView> startGame(@PathVariable Long gameId, Authentication authentication) {
        Game game = findGame(gameId);
        if (!isCreator(game, authentication.getName())) {
            return ResponseEntity.status(403).build();
        }
        if (game.getStarted_at() != null) {
            return ResponseEntity.badRequest().build();
        }
        if (game.getTeam_list() == null || game.getTeam_list().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        game = gameService.startGame(game);
        return ResponseEntity.ok(view(game));
    }

    @PostMapping("/{gameId}/stop")
    public ResponseEntity<GameView> stopGame(@PathVariable Long gameId, Authentication authentication) {
        Game game = findGame(gameId);
        if (!isCreator(game, authentication.getName())) {
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
        return ResponseEntity.ok(view(game));
    }

    @PostMapping("/{gameId}/submit-flag")
    public ResponseEntity<Map<String, Object>> submitFlag(
            @PathVariable Long gameId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String username = authentication.getName();
        if (!canView(findGame(gameId), username)) {
            return ResponseEntity.status(403).build();
        }
        String flag = body.get("flag");
        if (flag == null || flag.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Flag is required"));
        }
        Map<String, Object> result = flagService.submitFlag(gameId, username, flag.trim());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{gameId}/defeat-log")
    public ResponseEntity<List<DefeatLogEntry>> getDefeatLog(@PathVariable Long gameId,
                                                             Authentication authentication) {
        Game game = findGame(gameId);

        if (teamOf(game, authentication.getName()) == null) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(flagService.getDefeatLog(gameId));
    }

    @PostMapping("/{gameId}/dismiss")
    public ResponseEntity<Map<String, Object>> dismissGame(@PathVariable Long gameId, Authentication authentication) {
        String username = authentication.getName();
        Game game = findGame(gameId);

        if (!canView(game, username)) {
            return ResponseEntity.status(403).build();
        }

        if (game.getFinished_at() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Game is not finished yet"));
        }

        boolean deleted = gameService.dismissGame(gameId, username);
        return ResponseEntity.ok(Map.of("success", true, "gameDeleted", deleted));
    }

    @GetMapping("/{gameId}/credentials")
    public ResponseEntity<Map<String, String>> getCredentials(@PathVariable Long gameId, Authentication authentication) {
        String username = authentication.getName();
        Game game = findGame(gameId);

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
     * Rejects a creation request before anything is saved. Challenges must be
     * one of the challenge images the platform offers: the name is handed to
     * Docker as an image to run, so anything else would let a creator start
     * any image present on the host.
     */
    private void validate(CreateGameRequest req) {
        if (req.teams() == null) {
            throw new IllegalArgumentException("A game needs a list of teams");
        }
        Set<String> challenges = imageService.listChallengeImages().stream()
                .map(ChallengeDTO::name)
                .collect(Collectors.toSet());
        for (CreateGameRequest.TeamRequest team : req.teams()) {
            if (team.players() == null) {
                throw new IllegalArgumentException("Team " + team.name() + " needs a list of players");
            }
            if (!challenges.contains(team.challenge())) {
                throw new IllegalArgumentException("Unknown challenge: " + team.challenge());
            }
        }
    }

    private Game findGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));
    }

    /**
     * The game as sent to clients, with each team's container address filled
     * in from the containers that currently exist for it.
     */
    private GameView view(Game game) {
        Map<String, String> ipByTeam = new HashMap<>();
        for (ContainerEntity container : containerRepository.findByGame(game)) {
            if (container.getTeam() != null) {
                ipByTeam.put(container.getTeam().getName(), container.getIpAddress());
            }
        }
        return GameView.from(game, ipByTeam);
    }

    private boolean isCreator(Game game, String username) {
        return game.getCreated_by() != null && game.getCreated_by().getPlayer().equals(username);
    }

    /**
     * Only the creator and the players taking part may see a game: it lists
     * who plays, the scores and the addresses of the challenge containers.
     */
    private boolean canView(Game game, String username) {
        return isCreator(game, username) || teamOf(game, username) != null;
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

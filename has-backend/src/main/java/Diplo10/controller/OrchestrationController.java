package Diplo10.controller;

import Diplo10.entity.ContainerEntity;
import Diplo10.entity.Game;
import Diplo10.entity.Team;
import Diplo10.repository.ContainerRepository;
import Diplo10.repository.GameRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.orchestration.OrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orchestration")
public class OrchestrationController {

    private final OrchestrationService orchestrationService;
    private final ContainerRepository containerRepository;
    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;

    public OrchestrationController(OrchestrationService orchestrationService, ContainerRepository containerRepository, TeamRepository teamRepository, GameRepository gameRepository) {
        this.orchestrationService = orchestrationService;
        this.containerRepository = containerRepository;
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
    }

    @GetMapping("/session/all")
    public ResponseEntity<List<ContainerEntity>> listAllActiveGames() {
        return ResponseEntity.ok(containerRepository.findAll());
    }

    @GetMapping("/session/game/{gameId}")
    public ResponseEntity<List<ContainerEntity>> getContainersByGame(@PathVariable Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NullPointerException("Game not found"));
        return ResponseEntity.ok(containerRepository.findByGame(game));
    }

    @DeleteMapping("/stop/{gameId}")
    public ResponseEntity<String> stopGame(@PathVariable Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NullPointerException("Game not found"));
        orchestrationService.terminateGame(game);
        return ResponseEntity.ok("Game '" + game.getName() + "' terminated. All resources cleaned up.");
    }
}

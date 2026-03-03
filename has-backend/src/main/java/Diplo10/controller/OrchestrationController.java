package Diplo10.controller;

import Diplo10.entity.ContainerEntity;
import Diplo10.repository.ContainerRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.orchestration.OrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orchestration")
public class OrchestrationController {

    private final OrchestrationService orchestrationService;
    private final ContainerRepository containerRepository;
    private final TeamRepository teamRepository;

    public OrchestrationController(OrchestrationService orchestrationService, ContainerRepository containerRepository, TeamRepository teamRepository) {
        this.orchestrationService = orchestrationService;
        this.containerRepository = containerRepository;
        this.teamRepository = teamRepository;
    }
    @GetMapping("/session/all")
    public ResponseEntity<List<ContainerEntity>> listAllActiveGames() {
        return ResponseEntity.ok(containerRepository.findAll());
    }

    @GetMapping("/session/my-team/{teamName}")
    public ResponseEntity<List<ContainerEntity>> getGamesByTeam(@PathVariable String teamName) {
        List<ContainerEntity> teamContainers = containerRepository.findByTeamName(teamName);
        return ResponseEntity.ok(teamContainers);
    }

    @PostMapping("/session/create")
    public ResponseEntity<String> createFullSession(@RequestBody Map<String, Object> payload) {
        String gameName = (String) payload.get("gameName");

        List<Map<String, Object>> teams = (List<Map<String, Object>>) payload.get("teams");

        for (Map<String, Object> teamData : teams) {
            String teamName = (String) teamData.get("name");
            String machineImage = (String) teamData.get("machine");
            String teamPlayers = (String) teamData.get("players");
            orchestrationService.setupGameForTeam(teamName, machineImage, teamPlayers);
        }
        return ResponseEntity.ok("CTF Session '" + gameName + "' mit " + teams.size() + " Teams erfolgreich erstellt.");
    }

    @PostMapping("/deploy")
    public ResponseEntity<?> startChallenge(@RequestParam String teamName, @RequestParam String image, @RequestParam String teamPlayers) {
        try {
            ContainerEntity entity = orchestrationService.setupGameForTeam(teamName, image, teamPlayers);
            return ResponseEntity.ok(entity);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Fehler: " + e.getMessage());
        }
    }

    @DeleteMapping("stop/{teamName}")
    public ResponseEntity<String> stopGame(@PathVariable String teamName) {
        orchestrationService.terminateGame(teamName);
        return ResponseEntity.ok("Spiel für Team " + teamName + " wurde beendet und alle Ressourcen (Container & Netzwerk) wurden gelöscht.");
    }
}
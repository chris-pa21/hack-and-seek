package Diplo10.controller;

import Diplo10.entity.ContainerEntity;
import Diplo10.service.orchestration.OrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orchestration")
public class OrchestrationController {

    private final OrchestrationService orchestrationService;

    public OrchestrationController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/deploy")
    public ResponseEntity<?> deployChallenge(@RequestParam String teamName, @RequestParam String image) {
        try {
            ContainerEntity entity = orchestrationService.setupGameForTeam(teamName, image);
            return ResponseEntity.ok("Deployment für Team " + teamName + " erfolgreich gestartet.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Fehler beim Deployment: " + e.getMessage());
        }
    }
    @DeleteMapping("stop/{teamName}")
    public ResponseEntity<String> stopGame(@PathVariable String teamName) {
        orchestrationService.terminateGame(teamName);
        return ResponseEntity.ok("Spiel für Team " + teamName + " wurde beendet und alle Ressourcen (Container & Netzwerk) wurden gelöscht.");
    }
}
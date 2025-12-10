package Diplo10.controller;

import Diplo10.Service.FriendService;
import Diplo10.Service.ScoreboardService;
import Diplo10.entity.PlayerDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/scoreboard")
public class ScoreboardController {

    private ScoreboardService scoreboardService;

    public ScoreboardController(ScoreboardService scoreboardService) {
        this.scoreboardService = scoreboardService;
    }

    @GetMapping("/get")
    public ResponseEntity<?> get(@RequestParam String name){
        try {
            List saved = this.scoreboardService.get(name);



            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

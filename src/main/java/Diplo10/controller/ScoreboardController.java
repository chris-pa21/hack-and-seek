package Diplo10.controller;

import Diplo10.entity.Player;
import Diplo10.service.ScoreboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/scoreboard")
public class ScoreboardController {

    private final ScoreboardService scoreboardService;

    public ScoreboardController(ScoreboardService scoreboardService) {
        this.scoreboardService = scoreboardService;
    }


    public record ScoreboardResponse(String username, Integer points) {}

    @GetMapping()
    public ResponseEntity<List<ScoreboardResponse>> getScoreboard(@RequestParam String username) {
        List<Player> players = scoreboardService.getScoreboard(username);
        List<ScoreboardResponse> scoreboard = new ArrayList<>();
        for (Player player : players) {
            ScoreboardResponse reponse = new ScoreboardResponse(player.getPlayer(), player.getPoints());
            scoreboard.add(reponse);
        }
        return new ResponseEntity<>(scoreboard, HttpStatus.OK);
    }
}

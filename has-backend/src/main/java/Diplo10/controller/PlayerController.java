package Diplo10.controller;

import Diplo10.entity.Player;
import Diplo10.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class PlayerController {

    private PlayerService playerService;

    @Autowired
    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }


    public record GetPlayerResponse(String username, Integer points) {}

    @GetMapping
    public ResponseEntity<GetPlayerResponse> findByUsername(@RequestParam String username) {
        Player player = playerService.findByPlayer(username);
        GetPlayerResponse response = new GetPlayerResponse(username, player.getPoints());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}


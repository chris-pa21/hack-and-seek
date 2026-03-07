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
    private Diplo10.service.vpn.VpnService vpnService;

    @Autowired
    public PlayerController(PlayerService playerService, Diplo10.service.vpn.VpnService vpnService) {
        this.playerService = playerService;
        this.vpnService = vpnService;
    }
    public record GetPlayerResponse(
            String username, 
            Integer points, 
            String vpnIp, 
            String vpnPublicKey, 
            String vpnServerPublicKey,
            String vpnServerEndpoint
    ) {}

    @GetMapping
    public ResponseEntity<GetPlayerResponse> findByUsername(@RequestParam String username) {
        Player player = playerService.findByPlayer(username);
        GetPlayerResponse response = new GetPlayerResponse(
                username, 
                player.getPoints(),
                player.getVpnIp(),
                player.getVpnPublicKey(),
                player.getVpnServerPublicKey(),
                vpnService.getServerEndpoint()
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @GetMapping("/points")
    public ResponseEntity<Integer> getPoints(@RequestParam String username) {
        Player player = playerService.findByPlayer(username);
        return new ResponseEntity<>(player.getPoints(), HttpStatus.OK);
    }

    @org.springframework.web.bind.annotation.PostMapping("/vpn-public-key")
    public ResponseEntity<Void> updateVpnPublicKey(@RequestParam String username, @RequestParam String publicKey) {
        playerService.updateVpnPublicKey(username, publicKey);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}


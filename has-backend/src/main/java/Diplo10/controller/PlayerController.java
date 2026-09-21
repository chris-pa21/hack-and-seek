package Diplo10.controller;

import Diplo10.entity.Player;
import Diplo10.service.PlayerService;
import Diplo10.service.vpn.VpnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class PlayerController {

    private PlayerService playerService;
    private VpnService vpnService;

    @Autowired
    public PlayerController(PlayerService playerService, VpnService vpnService) {
        this.playerService = playerService;
        this.vpnService = vpnService;
    }

    /**
     * Public profile of any player. Deliberately contains no VPN data:
     * key material is only ever served to the session it belongs to.
     */
    public record GetPlayerResponse(
            String username,
            Integer points
    ) {}

    /**
     * Own profile. vpnIp is safe to show to its owner; the private key is not
     * included here either - it never leaves the server (see getMyVpnConfig).
     */
    public record MeResponse(
            String username,
            Integer points,
            String vpnIp
    ) {}

    @GetMapping
    public ResponseEntity<GetPlayerResponse> findByUsername(@RequestParam String username) {
        Player player = playerService.findByPlayer(username);
        return new ResponseEntity<>(new GetPlayerResponse(username, player.getPoints()), HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe(Authentication authentication) {
        Player player = playerService.findByPlayer(authentication.getName());
        ensureVpnConfig(player);
        return new ResponseEntity<>(
                new MeResponse(player.getPlayer(), player.getPoints(), player.getVpnIp()),
                HttpStatus.OK);
    }

    /**
     * Builds the WireGuard config for the *authenticated* player only. The
     * username is taken from the session, never from a request parameter, so
     * one player can never obtain another player's tunnel credentials.
     */
    @GetMapping("/me/vpn-config")
    public ResponseEntity<String> getMyVpnConfig(Authentication authentication) {
        Player player = playerService.findByPlayer(authentication.getName());
        ensureVpnConfig(player);

        if (player.getVpnPrivateKey() == null || player.getVpnIp() == null) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + player.getPlayer() + "-wg.conf\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(vpnService.generateConfig(player));
    }

    @GetMapping("/points")
    public ResponseEntity<Integer> getPoints(@RequestParam String username) {
        Player player = playerService.findByPlayer(username);
        return new ResponseEntity<>(player.getPoints(), HttpStatus.OK);
    }

    private void ensureVpnConfig(Player player) {
        if (player.getVpnPrivateKey() != null && player.getVpnPublicKey() != null) return;
        try {
            vpnService.ensureVpnConfig(player);
        } catch (Exception e) {
            System.err.println("VPN key generation failed for " + player.getPlayer() + ": " + e.getMessage());
        }
    }
}

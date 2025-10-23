package Diplo10.api;

import Diplo10.entity.Player;
import Diplo10.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        // minimal validation
        if (req.player == null || req.player.isBlank() ||
                req.password == null || req.password.isBlank()) {
            return ResponseEntity.badRequest().body("player and password required");
        }

        if (playerRepository.findByPlayer(req.player).isPresent()) {
            return ResponseEntity.status(409).body("player already exists");
        }
        if (req.email != null && !req.email.isBlank()
                && playerRepository.findByEmail(req.email).isPresent()) {
            return ResponseEntity.status(409).body("email already exists");
        }

        Player saved = playerRepository.save(
                new Player(
                        req.player,
                        passwordEncoder.encode(req.password),
                        req.email
                )
        );

        return ResponseEntity.status(201).body("ok:" + saved.getPlayer());
    }

    public record RegisterRequest(String player, String password, String email) {}
}

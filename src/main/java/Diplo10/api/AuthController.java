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
        if (req.username == null || req.username.isBlank() ||
                req.password == null || req.password.isBlank()) {
            return ResponseEntity.badRequest().body("player and password required");
        }

        if(!req.password.equals(req.confirmPassword)) {
            return ResponseEntity.badRequest().body("passwords do not match");
        }

        if (playerRepository.findByPlayer(req.username).isPresent()) {
            return ResponseEntity.status(409).body("player already exists");
        }
        if (req.email != null && !req.email.isBlank()
                && playerRepository.findByEmail(req.email).isPresent()) {
            return ResponseEntity.status(409).body("email already exists");
        }

        Player saved = playerRepository.save(
                new Player(
                        req.username,
                        passwordEncoder.encode(req.password),
                        req.email
                )
        );

        return ResponseEntity.status(201).body("ok:" + saved.getPlayer());
    }

    public record RegisterRequest(String username, String email, String password, String confirmPassword) {}
}

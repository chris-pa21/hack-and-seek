package Diplo10.api;

import Diplo10.entity.Player;
import Diplo10.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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



    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Wenn du Spring Security's UserDetails verwendest
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;

            // Erstelle ein DTO für die Response
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", userDetails.getUsername());
            userInfo.put("authorities", userDetails.getAuthorities());

            return ResponseEntity.ok(userInfo);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    public record RegisterRequest(String username, String email, String password, String confirmPassword) {}
}

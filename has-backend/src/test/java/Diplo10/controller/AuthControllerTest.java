package Diplo10.controller;

import Diplo10.entity.Player;
import Diplo10.repository.PlayerRepository;
import Diplo10.service.vpn.VpnService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import Diplo10.config.SecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)   // so the test also proves /api/auth/** is public
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PlayerRepository playerRepository;
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private VpnService vpnService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithAnonymousUser
    @DisplayName("a valid registration creates the player with a hashed password")
    void registersANewPlayer() throws Exception {
        when(playerRepository.findByPlayer("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        register("newuser", "password123", "password123")
                .andExpect(status().isCreated())
                .andExpect(content().string("ok:newuser"));

        // the raw password is never what gets stored
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @WithAnonymousUser
    @DisplayName("a new player gets a VPN config straight away")
    void registrationProvisionsVpnConfig() throws Exception {
        when(playerRepository.findByPlayer("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("HASHED");
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        register("newuser", "password123", "password123")
                .andExpect(status().isCreated());

        verify(vpnService).ensureVpnConfig(any(Player.class));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("registration still succeeds when VPN provisioning fails")
    void vpnFailureDoesNotBlockRegistration() throws Exception {
        when(playerRepository.findByPlayer("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("HASHED");
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("wireguard container down"))
                .when(vpnService).ensureVpnConfig(any(Player.class));

        // the account is usable even if the tunnel is not ready yet
        register("newuser", "password123", "password123")
                .andExpect(status().isCreated());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("an existing username is rejected with 409")
    void duplicateUsernameIsConflict() throws Exception {
        when(playerRepository.findByPlayer("taken")).thenReturn(Optional.of(new Player("taken", "x")));

        register("taken", "password123", "password123")
                .andExpect(status().isConflict());

        verify(playerRepository, never()).save(any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("a password shorter than 8 characters is rejected")
    void shortPasswordIsRejected() throws Exception {
        register("newuser", "short", "short")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("password must be at least 8 characters"));

        verify(playerRepository, never()).save(any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("mismatched passwords are rejected")
    void mismatchedPasswordsAreRejected() throws Exception {
        register("newuser", "password123", "different123")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("passwords do not match"));

        verify(playerRepository, never()).save(any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("a blank username or password is rejected")
    void blankCredentialsAreRejected() throws Exception {
        register("", "password123", "password123").andExpect(status().isBadRequest());
        register("newuser", "", "").andExpect(status().isBadRequest());

        verify(playerRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "alice", authorities = "USER")
    @DisplayName("/me reports the authenticated user")
    void meReturnsTheSessionUser() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.authorities").isArray());
    }

    private org.springframework.test.web.servlet.ResultActions register(
            String username, String password, String confirmPassword) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new AuthController.RegisterRequest(username, password, confirmPassword))));
    }
}

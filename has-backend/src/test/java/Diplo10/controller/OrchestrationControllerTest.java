package Diplo10.controller;

import Diplo10.entity.ContainerEntity;
import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.ContainerRepository;
import Diplo10.repository.GameRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.orchestration.OrchestrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Containers must refer to their team by name: the Team entity would carry
 * every member's Player record, with password hash and VPN keys.
 */
@WebMvcTest(OrchestrationController.class)
class OrchestrationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrchestrationService orchestrationService;
    @MockitoBean private ContainerRepository containerRepository;
    @MockitoBean private TeamRepository teamRepository;
    @MockitoBean private GameRepository gameRepository;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a game's containers carry their team's name but no member data")
    void containersCarryNoSecrets() throws Exception {
        Player alice = new Player("alice", "HASH_alice");
        alice.setVpnPrivateKey("PRIVATE_KEY_alice");
        Team alpha = new Team("alpha", 1);
        alpha.addPlayer(alice);

        Game game = new Game();
        game.setId(1L);
        game.setCreated_by(alice);
        game.setTeam_list(new ArrayList<>(List.of(alpha)));

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(containerRepository.findByGame(game)).thenReturn(List.of(
                ContainerEntity.builder()
                        .containerId("c1")
                        .team(alpha)
                        .game(game)
                        .ipAddress("10.200.1.2")
                        .flag("HAS{secret}")
                        .rootPassword("ROOTPW")
                        .build()));

        mockMvc.perform(get("/api/orchestration/session/game/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamName").value("alpha"))
                .andExpect(jsonPath("$[0].gameId").value(1))
                .andExpect(jsonPath("$[0].ipAddress").value("10.200.1.2"))
                .andExpect(content().string(not(containsString("HASH_"))))
                .andExpect(content().string(not(containsString("PRIVATE_KEY_"))))
                .andExpect(content().string(not(containsString("HAS{secret}"))))
                .andExpect(content().string(not(containsString("ROOTPW"))));
    }
}

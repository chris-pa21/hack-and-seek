package Diplo10.controller;

import Diplo10.entity.Player;
import Diplo10.service.ScoreboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The scoreboard is always the session user's own, whatever username a
 * request names.
 */
@WebMvcTest(ScoreboardController.class)
class ScoreboardControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ScoreboardService scoreboardService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("the scoreboard is built for the session user")
    void scoreboardIsTheSessionUsers() throws Exception {
        Player alice = new Player("alice", "secret");
        alice.setPoints(120);
        when(scoreboardService.getScoreboard("alice")).thenReturn(List.of(alice));

        mockMvc.perform(get("/api/scoreboard").param("username", "mallory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].points").value(120));

        verify(scoreboardService, never()).getScoreboard("mallory");
    }
}

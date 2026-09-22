package Diplo10.controller;

import Diplo10.config.SecurityConfig;
import Diplo10.entity.ContainerEntity;
import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.ContainerRepository;
import Diplo10.repository.GameRepository;
import Diplo10.repository.PlayerRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.FlagService;
import Diplo10.service.GameService;
import Diplo10.service.orchestration.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization rules around the game lifecycle: only the creator may start or
 * stop a game, and credentials are scoped to the caller's own team.
 */
@WebMvcTest(GameController.class)
@Import(SecurityConfig.class)
class GameControllerTest {

    private static final long GAME_ID = 1L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GameRepository gameRepository;
    @MockitoBean private GameService gameService;
    @MockitoBean private TeamRepository teamRepository;
    @MockitoBean private PlayerRepository playerRepository;
    @MockitoBean private ImageService imageService;
    @MockitoBean private FlagService flagService;
    @MockitoBean private ContainerRepository containerRepository;
    @MockitoBean private UserDetailsService userDetailsService;

    private Game game;
    private Team alpha;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        alice = new Player("alice", "secret");
        bob = new Player("bob", "secret");

        alpha = new Team("alpha");
        alpha.addPlayer(alice);
        Team bravo = new Team("bravo");
        bravo.addPlayer(bob);

        game = new Game();
        game.setId(GAME_ID);
        game.setName("test-game");
        game.setCreated_by(alice);
        game.setTeam_list(new ArrayList<>(List.of(alpha, bravo)));
    }

    // ── starting ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("the creator can start their game")
    void creatorCanStart() throws Exception {
        stubGame();
        when(gameService.startGame(game)).thenReturn(game);

        mockMvc.perform(post("/api/games/{id}/start", GAME_ID).with(csrf()))
                .andExpect(status().isOk());

        verify(gameService).startGame(game);
    }

    @Test
    @WithMockUser(username = "bob")
    @DisplayName("a player who is not the creator cannot start the game")
    void nonCreatorCannotStart() throws Exception {
        stubGame();

        mockMvc.perform(post("/api/games/{id}/start", GAME_ID).with(csrf()))
                .andExpect(status().isForbidden());

        verify(gameService, never()).startGame(any());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a game cannot be started twice")
    void startingTwiceIsRejected() throws Exception {
        game.setStarted_at(LocalDateTime.now());
        stubGame();

        mockMvc.perform(post("/api/games/{id}/start", GAME_ID).with(csrf()))
                .andExpect(status().isBadRequest());

        verify(gameService, never()).startGame(any());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a game with no teams cannot be started")
    void startingWithoutTeamsIsRejected() throws Exception {
        game.setTeam_list(new ArrayList<>());
        stubGame();

        mockMvc.perform(post("/api/games/{id}/start", GAME_ID).with(csrf()))
                .andExpect(status().isBadRequest());

        verify(gameService, never()).startGame(any());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("starting a game that does not exist is a 404")
    void startingUnknownGameIsNotFound() throws Exception {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/games/{id}/start", GAME_ID).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("an anonymous caller cannot start a game")
    void anonymousCannotStart() throws Exception {
        mockMvc.perform(post("/api/games/{id}/start", GAME_ID).with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(gameService, never()).startGame(any());
    }

    // ── stopping ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "bob")
    @DisplayName("a player who is not the creator cannot stop the game")
    void nonCreatorCannotStop() throws Exception {
        game.setStarted_at(LocalDateTime.now());
        stubGame();

        mockMvc.perform(post("/api/games/{id}/stop", GAME_ID).with(csrf()))
                .andExpect(status().isForbidden());

        verify(gameService, never()).endGame(any(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a game that never started cannot be stopped")
    void stoppingAnUnstartedGameIsRejected() throws Exception {
        stubGame();

        mockMvc.perform(post("/api/games/{id}/stop", GAME_ID).with(csrf()))
                .andExpect(status().isBadRequest());

        verify(gameService, never()).endGame(any(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("stopping names the highest-scoring surviving team as winner")
    void stopPicksTheLeader() throws Exception {
        game.setStarted_at(LocalDateTime.now());
        alpha.setScore(300);
        game.getTeam_list().get(1).setScore(100);
        stubGame();

        mockMvc.perform(post("/api/games/{id}/stop", GAME_ID).with(csrf()))
                .andExpect(status().isOk());

        verify(gameService).endGame(game, alpha);
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("stopping a tied game records no winner")
    void stopWithATieHasNoWinner() throws Exception {
        game.setStarted_at(LocalDateTime.now());
        alpha.setScore(100);
        game.getTeam_list().get(1).setScore(100);
        stubGame();

        mockMvc.perform(post("/api/games/{id}/stop", GAME_ID).with(csrf()))
                .andExpect(status().isOk());

        verify(gameService).endGame(game, null);
    }

    // ── credentials ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a player gets the credentials for their own team's container")
    void ownTeamCredentialsAreReturned() throws Exception {
        stubGame();
        when(containerRepository.findByTeam(alpha)).thenReturn(Optional.of(
                ContainerEntity.builder()
                        .containerId("c1")
                        .team(alpha)
                        .rootPassword("s3cret42")
                        .ipAddress("172.20.0.2")
                        .build()));

        mockMvc.perform(get("/api/games/{id}/credentials", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootPassword").value("s3cret42"))
                .andExpect(jsonPath("$.ipAddress").value("172.20.0.2"));
    }

    @Test
    @WithMockUser(username = "stranger")
    @DisplayName("someone who is not in the game gets no credentials")
    void outsiderGetsNoCredentials() throws Exception {
        stubGame();

        mockMvc.perform(get("/api/games/{id}/credentials", GAME_ID))
                .andExpect(status().isForbidden());

        verify(containerRepository, never()).findByTeam(any());
    }

    // ── adding a team ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a team added to a game is bound to that game's id")
    void addedTeamKnowsItsGame() throws Exception {
        mockMvc.perform(post("/api/games/{id}/team", GAME_ID).with(csrf()).param("name", "charlie"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Team> team = org.mockito.ArgumentCaptor.forClass(Team.class);
        verify(gameService).addTeam(team.capture(), org.mockito.ArgumentMatchers.eq(GAME_ID));

        // with gameId left at 0, findByGameIdAndName could never find this team
        assertThat(team.getValue().getName()).isEqualTo("charlie");
        assertThat(team.getValue().getGameId()).isEqualTo((int) GAME_ID);
    }

    // ── defeat log ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a participant can read the defeat log, without the flag values")
    void participantReadsDefeatLogWithoutFlags() throws Exception {
        stubGame();
        when(flagService.getDefeatLog(GAME_ID)).thenReturn(List.of(
                new Diplo10.dto.DefeatLogEntry(1L, "alpha", "bravo", 100,
                        LocalDateTime.of(2026, 9, 22, 14, 3))));

        mockMvc.perform(get("/api/games/{id}/defeat-log", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attackingTeam").value("alpha"))
                .andExpect(jsonPath("$[0].defeatedTeam").value("bravo"))
                .andExpect(jsonPath("$[0].pointsAwarded").value(100))
                // the captured flag is the defender's secret and is never sent
                .andExpect(jsonPath("$[0].flag").doesNotExist());
    }

    @Test
    @WithMockUser(username = "stranger")
    @DisplayName("someone not in the game cannot read its defeat log")
    void outsiderCannotReadDefeatLog() throws Exception {
        stubGame();

        mockMvc.perform(get("/api/games/{id}/defeat-log", GAME_ID))
                .andExpect(status().isForbidden());

        verify(flagService, never()).getDefeatLog(any());
    }

    // ── flag submission ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("an empty flag is rejected before reaching the scoring logic")
    void emptyFlagIsRejected() throws Exception {
        mockMvc.perform(post("/api/games/{id}/submit-flag", GAME_ID)
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"flag\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(flagService, never()).submitFlag(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("the submitted flag is trimmed and attributed to the session user")
    void flagIsTrimmedAndAttributedToTheCaller() throws Exception {
        when(flagService.submitFlag(GAME_ID, "alice", "HAS{abc}"))
                .thenReturn(java.util.Map.of("success", true));

        mockMvc.perform(post("/api/games/{id}/submit-flag", GAME_ID)
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"flag\":\"  HAS{abc}  \"}"))
                .andExpect(status().isOk());

        // the username comes from the session, never from the request body
        verify(flagService).submitFlag(GAME_ID, "alice", "HAS{abc}");
    }

    private void stubGame() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor csrf() {
        return org.springframework.security.test.web.servlet.request
                .SecurityMockMvcRequestPostProcessors.csrf();
    }
}

package Diplo10.service;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.exception.NotFoundException;
import Diplo10.repository.GameRepository;
import Diplo10.repository.PlayerRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.orchestration.NetworkService;
import Diplo10.service.orchestration.OrchestrationService;
import Diplo10.service.vpn.VpnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    private static final long GAME_ID = 1L;
    private static final String NETWORK_ID = "net-abc123";

    @Mock private GameRepository gameRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private OrchestrationService orchestrationService;
    @Mock private NetworkService networkService;
    @Mock private VpnService vpnService;

    @InjectMocks private GameService gameService;

    private Game game;
    private Team alpha;
    private Team bravo;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        alice = new Player("alice", "secret");
        bob = new Player("bob", "secret");

        alpha = new Team("alpha");
        alpha.addPlayer(alice);
        bravo = new Team("bravo");
        bravo.addPlayer(bob);

        game = new Game();
        game.setId(GAME_ID);
        game.setName("test-game");
        game.setCreated_by(alice);
        game.setTeam_list(new ArrayList<>(List.of(alpha, bravo)));
    }

    // ── starting ────────────────────────────────────────────────────────

    @Test
    @DisplayName("starting a game creates the network, a container per team and VPN access")
    void startGameSetsEverythingUp() {
        when(networkService.createIsolatedNetwork(game)).thenReturn(NETWORK_ID);
        when(gameRepository.save(game)).thenReturn(game);

        Game started = gameService.startGame(game);

        assertThat(started.getStarted_at()).isNotNull();
        verify(orchestrationService).setupTeamContainer(game, alpha, NETWORK_ID);
        verify(orchestrationService).setupTeamContainer(game, bravo, NETWORK_ID);
        verify(vpnService).ensureVpnConfig(alice);
        verify(vpnService).ensureVpnConfig(bob);
        verify(vpnService).includeNetwork(NETWORK_ID);
        verify(vpnService).authorizeGameAccess(game, NETWORK_ID);
    }

    @Test
    @DisplayName("if any team's container fails the whole start is rolled back")
    void startGameIsAllOrNothing() {
        when(networkService.createIsolatedNetwork(game)).thenReturn(NETWORK_ID);
        when(orchestrationService.setupTeamContainer(game, alpha, NETWORK_ID))
                .thenThrow(new RuntimeException("docker daemon unreachable"));

        assertThatThrownBy(() -> gameService.startGame(game))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not start the game");

        // the game stays in the lobby instead of "running" with no containers
        assertThat(game.getStarted_at()).isNull();
        verify(orchestrationService).terminateGame(game);
        verify(gameRepository, never()).save(any());
        verify(vpnService, never()).includeNetwork(anyString());
        verify(vpnService, never()).authorizeGameAccess(any(), anyString());
    }

    @Test
    @DisplayName("a later team failing also rolls back the teams that already started")
    void startGameRollsBackPartialProgress() {
        when(networkService.createIsolatedNetwork(game)).thenReturn(NETWORK_ID);
        when(orchestrationService.setupTeamContainer(game, bravo, NETWORK_ID))
                .thenThrow(new RuntimeException("no such image"));

        assertThatThrownBy(() -> gameService.startGame(game))
                .isInstanceOf(IllegalStateException.class);

        verify(orchestrationService).setupTeamContainer(game, alpha, NETWORK_ID);
        verify(orchestrationService).terminateGame(game);
        assertThat(game.getStarted_at()).isNull();
    }

    // ── ending ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("ending a game records the winner and tears the infrastructure down")
    void endGameTearsDown() {
        when(networkService.getIsolatedNetworkId(game)).thenReturn(NETWORK_ID);

        gameService.endGame(game, alpha);

        assertThat(game.getFinished_at()).isNotNull();
        assertThat(game.getWinnerTeam()).isEqualTo(alpha);
        verify(gameRepository).save(game);
        verify(orchestrationService).terminateGame(game);
        verify(vpnService).revokeGameAccess(game, NETWORK_ID);
        verify(vpnService).excludeNetwork(NETWORK_ID);
    }

    @Test
    @DisplayName("a game that ends in a draw is recorded with no winner")
    void endGameAcceptsADraw() {
        when(networkService.getIsolatedNetworkId(game)).thenReturn(null);

        gameService.endGame(game, null);

        assertThat(game.getFinished_at()).isNotNull();
        assertThat(game.getWinnerTeam()).isNull();
        verify(orchestrationService).terminateGame(game);
        // no network to detach from, so the VPN is left alone
        verify(vpnService, never()).excludeNetwork(anyString());
    }

    // ── dismissing ──────────────────────────────────────────────────────

    @Test
    @DisplayName("dismissing removes the player from their team and keeps the game")
    void dismissRemovesThePlayer() {
        stubLookup("bob", bob);

        boolean deleted = gameService.dismissGame(GAME_ID, "bob");

        assertThat(deleted).isFalse();
        assertThat(bravo.getTeam_members()).isEmpty();
        assertThat(alpha.getTeam_members()).containsExactly(alice);
        verify(teamRepository).save(bravo);
    }

    @Test
    @DisplayName("when the creator leaves, the game is handed to a remaining player")
    void creatorLeavingHandsOverTheGame() {
        stubLookup("alice", alice);

        boolean deleted = gameService.dismissGame(GAME_ID, "alice");

        assertThat(deleted).isFalse();
        // bob can still start and stop it; a null creator would lock it forever
        assertThat(game.getCreated_by()).isEqualTo(bob);
        verify(gameRepository).save(game);
    }

    @Test
    @DisplayName("the last player leaving deletes the game")
    void lastPlayerLeavingDeletesTheGame() {
        alpha.getTeam_members().clear();
        stubLookup("bob", bob);

        boolean deleted = gameService.dismissGame(GAME_ID, "bob");

        assertThat(deleted).isTrue();
        verify(gameRepository).delete(game);
    }

    @Test
    @DisplayName("deleting a running game tears its containers down first")
    void lastPlayerLeavingARunningGameTearsDown() {
        alpha.getTeam_members().clear();
        game.setStarted_at(LocalDateTime.now());
        stubLookup("bob", bob);

        boolean deleted = gameService.dismissGame(GAME_ID, "bob");

        assertThat(deleted).isTrue();
        // otherwise the containers would outlive the game that owned them
        verify(orchestrationService).terminateGame(game);
        verify(gameRepository).delete(game);
    }

    @Test
    @DisplayName("a finished game is deleted without tearing anything down twice")
    void lastPlayerLeavingAFinishedGameDoesNotTearDownAgain() {
        alpha.getTeam_members().clear();
        game.setStarted_at(LocalDateTime.now());
        game.setFinished_at(LocalDateTime.now());
        stubLookup("bob", bob);

        gameService.dismissGame(GAME_ID, "bob");

        verify(orchestrationService, never()).terminateGame(any());
        verify(gameRepository).delete(game);
    }

    @Test
    @DisplayName("dismissing an unknown game is a 404")
    void dismissUnknownGameIsNotFound() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.dismissGame(GAME_ID, "alice"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    @DisplayName("dismissing as an unknown player is a 404")
    void dismissUnknownPlayerIsNotFound() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(playerRepository.findByPlayer("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.dismissGame(GAME_ID, "ghost"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Player not found");
    }

    private void stubLookup(String username, Player player) {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(playerRepository.findByPlayer(username)).thenReturn(Optional.of(player));
    }
}

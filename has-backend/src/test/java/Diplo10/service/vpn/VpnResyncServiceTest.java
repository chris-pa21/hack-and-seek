package Diplo10.service.vpn;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.GameRepository;
import Diplo10.service.orchestration.NetworkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VpnResyncServiceTest {

    private static final String NETWORK_ID = "net-running";

    @Mock private GameRepository gameRepository;
    @Mock private NetworkService networkService;
    @Mock private VpnService vpnService;
    @Mock private VpnManagerClient vpnManagerClient;

    @InjectMocks private VpnResyncService resync;

    private Game running;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        alice = new Player("alice", "secret");
        bob = new Player("bob", "secret");
        running = game("running", LocalDateTime.now(), null, alice, bob);
    }

    @Test
    @DisplayName("the first check restores every running game, and only running ones")
    void firstCheckRestoresRunningGames() {
        Player carol = new Player("carol", "secret");
        Game lobby = game("lobby", null, null, carol);
        Game finished = game("finished", LocalDateTime.now().minusHours(1), LocalDateTime.now(), carol);
        when(vpnManagerClient.instanceId()).thenReturn("instance-1");
        when(gameRepository.findAll()).thenReturn(List.of(lobby, running, finished));
        when(networkService.getIsolatedNetworkId("running")).thenReturn(NETWORK_ID);

        resync.resyncIfReset();

        verify(vpnService).includeNetwork(NETWORK_ID);
        verify(vpnService).ensureVpnConfig(alice);
        verify(vpnService).ensureVpnConfig(bob);
        verify(vpnService).authorizeGameAccess(running, NETWORK_ID);
        verify(vpnService, never()).ensureVpnConfig(carol);
        verify(networkService, never()).getIsolatedNetworkId("lobby");
        verify(networkService, never()).getIsolatedNetworkId("finished");
    }

    @Test
    @DisplayName("nothing happens while the manager keeps its instance")
    void sameInstanceIsNotRestoredTwice() {
        when(vpnManagerClient.instanceId()).thenReturn("instance-1");
        when(gameRepository.findAll()).thenReturn(List.of(running));
        when(networkService.getIsolatedNetworkId("running")).thenReturn(NETWORK_ID);

        resync.resyncIfReset();
        resync.resyncIfReset();

        verify(vpnService, times(1)).authorizeGameAccess(running, NETWORK_ID);
    }

    @Test
    @DisplayName("a new instance means a restart, so everything is restored again")
    void newInstanceIsRestored() {
        when(vpnManagerClient.instanceId()).thenReturn("instance-1", "instance-2");
        when(gameRepository.findAll()).thenReturn(List.of(running));
        when(networkService.getIsolatedNetworkId("running")).thenReturn(NETWORK_ID);

        resync.resyncIfReset();
        resync.resyncIfReset();

        verify(vpnService, times(2)).ensureVpnConfig(alice);
        verify(vpnService, times(2)).authorizeGameAccess(running, NETWORK_ID);
    }

    @Test
    @DisplayName("while WireGuard is down nothing is touched; it is restored once it answers")
    void unreachableManagerIsWaitedFor() {
        when(vpnManagerClient.instanceId())
                .thenThrow(new ResourceAccessException("Connection refused"))
                .thenReturn("instance-1");
        when(gameRepository.findAll()).thenReturn(List.of(running));
        when(networkService.getIsolatedNetworkId("running")).thenReturn(NETWORK_ID);

        resync.resyncIfReset();
        verifyNoInteractions(gameRepository, vpnService);

        resync.resyncIfReset();
        verify(vpnService).authorizeGameAccess(running, NETWORK_ID);
    }

    @Test
    @DisplayName("a failed restore is retried on the next check")
    void failedRestoreIsRetried() {
        when(vpnManagerClient.instanceId()).thenReturn("instance-1");
        when(gameRepository.findAll()).thenReturn(List.of(running));
        when(networkService.getIsolatedNetworkId("running")).thenReturn(NETWORK_ID);
        doThrow(new ResourceAccessException("Connection reset"))
                .doNothing()
                .when(vpnService).authorizeGameAccess(running, NETWORK_ID);

        resync.resyncIfReset(); // fails, instance not marked as restored
        resync.resyncIfReset(); // same instance, tried again
        resync.resyncIfReset(); // succeeded before, nothing to do

        verify(vpnService, times(2)).authorizeGameAccess(running, NETWORK_ID);
    }

    @Test
    @DisplayName("a running game whose network is gone is skipped")
    void gameWithoutNetworkIsSkipped() {
        when(vpnManagerClient.instanceId()).thenReturn("instance-1");
        when(gameRepository.findAll()).thenReturn(List.of(running));
        when(networkService.getIsolatedNetworkId("running")).thenReturn(null);

        resync.resyncIfReset();

        verify(vpnService, never()).includeNetwork(anyString());
        verify(vpnService, never()).ensureVpnConfig(any());
        verify(vpnService, never()).authorizeGameAccess(any(), anyString());
    }

    private static Game game(String name, LocalDateTime startedAt, LocalDateTime finishedAt, Player... members) {
        Team team = new Team(name + "-team");
        for (Player member : members) {
            team.addPlayer(member);
        }
        Game game = new Game();
        game.setName(name);
        game.setStarted_at(startedAt);
        game.setFinished_at(finishedAt);
        game.setTeam_list(new ArrayList<>(List.of(team)));
        return game;
    }
}

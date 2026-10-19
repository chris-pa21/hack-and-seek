package Diplo10.service.vpn;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.GameRepository;
import Diplo10.service.orchestration.NetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Restores the VPN state of running games after the WireGuard container
 * restarted. Peers, routes and firewall rules live only in the running
 * container; a restart wipes them and would cut every player off until the
 * game ends. The database holds everything needed to rebuild them, so this
 * only has to notice the restart: the VPN manager reports a new instance id
 * on every start.
 *
 * Every operation used here is idempotent, so restoring state that was not
 * lost (the first check after the backend starts) does no harm.
 */
@Service
@RequiredArgsConstructor
public class VpnResyncService {

    private final GameRepository gameRepository;
    private final NetworkService networkService;
    private final VpnService vpnService;
    private final VpnManagerClient vpnManagerClient;

    /** Manager run whose state has been restored; null until the first resync. */
    private String restoredInstance;

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void resyncIfReset() {
        String instance;
        try {
            instance = vpnManagerClient.instanceId();
        } catch (Exception e) {
            return; // WireGuard is down; its next instance is restored once it is back
        }
        if (instance == null || instance.equals(restoredInstance)) {
            return;
        }

        try {
            for (Game game : runningGames()) {
                restore(game);
            }
            restoredInstance = instance;
        } catch (Exception e) {
            System.err.println("VPN resync failed, retrying: " + e.getMessage());
        }
    }

    private List<Game> runningGames() {
        return gameRepository.findAll().stream()
                .filter(g -> g.getStarted_at() != null && g.getFinished_at() == null)
                .toList();
    }

    private void restore(Game game) {
        String networkId = networkService.getIsolatedNetworkId(game);
        if (networkId == null) {
            return;
        }
        // a restart keeps the attachment, but a recreated container loses it
        vpnService.includeNetwork(networkId);
        for (Team team : game.getTeam_list()) {
            for (Player member : team.getTeam_members()) {
                vpnService.ensureVpnConfig(member); // peer and route
            }
        }
        vpnService.authorizeGameAccess(game, networkId);
        System.out.println("Restored VPN access for game " + game.getName());
    }
}

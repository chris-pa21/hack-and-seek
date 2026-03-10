package Diplo10.service;

import Diplo10.entity.Player;
import Diplo10.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private PlayerRepository playerRepository;
    private Diplo10.service.vpn.VpnService vpnService;

    @Autowired
    public PlayerService(PlayerRepository playerRepository, Diplo10.service.vpn.VpnService vpnService) {
        this.playerRepository = playerRepository;
        this.vpnService = vpnService;
    }



    @Transactional
    public void updateVpnPublicKey(String playerName, String publicKey) {
        Player player = findByPlayer(playerName);
        player.setVpnPublicKey(publicKey);
        vpnService.ensureVpnConfig(player);
    }

    @Transactional
    public Player findByPlayer(String playerName) {
        return playerRepository.findByPlayer(playerName).orElseThrow(() -> new NullPointerException("Player with username " + playerName + " not found"));
    }
}

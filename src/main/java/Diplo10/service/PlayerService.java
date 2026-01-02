package Diplo10.service;

import Diplo10.entity.Player;
import Diplo10.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private PlayerRepository playerRepository;

    @Autowired
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }



    @Transactional
    public Player findByPlayer(String playerName) {
        return playerRepository.findByPlayer(playerName).orElseThrow(() -> new NullPointerException("Player with username " + playerName + " not found"));
    }
}

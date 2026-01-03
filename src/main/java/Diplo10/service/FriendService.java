package Diplo10.service;

import Diplo10.entity.Player;
import Diplo10.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class FriendService {
    private final PlayerRepository playerRepository;

    public FriendService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public void addFriend(Player player, Player req) {
        Optional<Player> optionalPlayer = playerRepository.findByPlayer(player.getPlayer());
        Optional<Player> optionalReq = playerRepository.findByPlayer(req.getPlayer());

        if (optionalPlayer.isEmpty() || optionalReq.isEmpty()) {
            throw new RuntimeException("One or both players not found");
        }

        Player managedPlayer = optionalPlayer.get();
        Player managedReq = optionalReq.get();

        managedPlayer.getFriends().add(managedReq);
        managedReq.getFriends().add(managedPlayer);

        playerRepository.save(managedPlayer);
        playerRepository.save(managedReq);
    }

    @Transactional
    public Set<Player> getFriends(String name) {
        return playerRepository.findByPlayer(name)
                .map(Player::getFriends)
                .orElseGet(HashSet::new);
    }



}

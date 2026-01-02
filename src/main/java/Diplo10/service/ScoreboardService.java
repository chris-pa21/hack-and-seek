package Diplo10.service;

import Diplo10.entity.Player;
import Diplo10.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScoreboardService {
    private final FriendService friendService;
    private final PlayerRepository playerRepository;

    public ScoreboardService(FriendService friendService, PlayerRepository playerRepository) {
        this.friendService = friendService;
        this.playerRepository = playerRepository;
    }

    public List<Player> get(String name) {
        Set<Player> saved = this.friendService.getFriends(name);

        Player currentPlayer = playerRepository.findByPlayer(name)
                .orElseThrow(() -> new RuntimeException("Player not found: " + name));

        saved.add(currentPlayer);

        List<Player> list = new ArrayList<>(saved);
        list.sort(Comparator.comparingInt(p -> Optional.ofNullable(p.getPoints()).orElse(0)));
        Collections.reverse(list);

        return list;
    }
}

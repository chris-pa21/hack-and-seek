package Diplo10.service;

import Diplo10.entity.Player;
import Diplo10.repository.PlayerRepository;
import org.springframework.data.domain.PageRequest;
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

    public List<Player> getScoreboard(String name) {
        Player user = playerRepository.findByPlayer(name)
                .orElseThrow(() -> new RuntimeException("Player not found: " + name));

        List<Player> topFriends = playerRepository
                .findTopFriendsByPoints(name, PageRequest.of(0, 10));

        int userPoints = Optional.ofNullable(user.getPoints()).orElse(0);

        // Case 1: fewer than 10 friends
        if (topFriends.size() < 10) {
            topFriends.add(user);
        } else {
            int lastFriendPoints = Optional.ofNullable(
                    topFriends.get(topFriends.size() - 1).getPoints()
            ).orElse(0);

            // If user does not beat the 10th friend, just return the friends
            if (userPoints <= lastFriendPoints) {
                return topFriends;
            }

            // User beats at least the 10th friend, include them
            topFriends.add(user);
        }

        // Sort and keep at most 10
        topFriends.sort(Comparator.comparingInt(
                (Player p) -> Optional.ofNullable(p.getPoints()).orElse(0)
        ).reversed());

        return topFriends.size() > 10 ? topFriends.subList(0, 10) : topFriends;
    }
}

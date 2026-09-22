package Diplo10.service;

import Diplo10.entity.Player;
import Diplo10.exception.NotFoundException;
import Diplo10.repository.PlayerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ScoreboardService {

    private static final int BOARD_SIZE = 10;

    private final PlayerRepository playerRepository;

    public ScoreboardService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    /**
     * The player's top friends by points, with the player themselves always on
     * the board. If they rank below the last friend, they take the final slot
     * rather than dropping off their own scoreboard.
     */
    public List<Player> getScoreboard(String name) {
        Player user = playerRepository.findByPlayer(name)
                .orElseThrow(() -> new NotFoundException("Player not found: " + name));

        List<Player> board = new ArrayList<>(
                playerRepository.findTopFriendsByPoints(name, PageRequest.of(0, BOARD_SIZE)));
        board.add(user);
        board.sort(Comparator.comparingInt(ScoreboardService::pointsOf).reversed());

        if (board.size() <= BOARD_SIZE) {
            return board;
        }

        List<Player> top = new ArrayList<>(board.subList(0, BOARD_SIZE));
        boolean userOnBoard = top.stream().anyMatch(p -> p.getPlayer().equals(name));
        if (!userOnBoard) {
            top.set(BOARD_SIZE - 1, user);
        }
        return top;
    }

    private static int pointsOf(Player player) {
        return Optional.ofNullable(player.getPoints()).orElse(0);
    }
}

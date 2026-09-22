package Diplo10.service;

import Diplo10.entity.Player;
import Diplo10.exception.NotFoundException;
import Diplo10.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreboardServiceTest {

    @Mock private PlayerRepository playerRepository;

    @InjectMocks private ScoreboardService scoreboardService;

    @Test
    @DisplayName("with fewer than ten friends the board lists them all plus the user")
    void shortBoardIncludesEveryone() {
        Player user = player("alice", 120);
        stubUser(user);
        stubFriends(List.of(player("bob", 300), player("carol", 50)));

        List<Player> board = scoreboardService.getScoreboard("alice");

        assertThat(board).hasSize(3);
        assertThat(names(board)).containsExactly("bob", "alice", "carol");
    }

    @Test
    @DisplayName("the board is ordered by points, highest first")
    void boardIsSortedByPointsDescending() {
        stubUser(player("alice", 10));
        stubFriends(List.of(player("bob", 500), player("carol", 250), player("dave", 900)));

        List<Player> board = scoreboardService.getScoreboard("alice");

        assertThat(names(board)).containsExactly("dave", "bob", "carol", "alice");
    }

    @Test
    @DisplayName("with ten friends the board is capped at ten entries")
    void boardIsCappedAtTen() {
        stubUser(player("alice", 10_000));
        stubFriends(friends(10, 100));

        List<Player> board = scoreboardService.getScoreboard("alice");

        assertThat(board).hasSize(10);
        assertThat(board.get(0).getPlayer()).isEqualTo("alice");
    }

    @Test
    @DisplayName("the user stays on their own board even when ranked below every friend")
    void userAlwaysAppearsOnTheirOwnBoard() {
        stubUser(player("alice", 0));
        stubFriends(friends(10, 100));

        List<Player> board = scoreboardService.getScoreboard("alice");

        assertThat(board).hasSize(10);
        assertThat(names(board)).contains("alice");
        // they take the final slot rather than dropping off
        assertThat(board.get(9).getPlayer()).isEqualTo("alice");
    }

    @Test
    @DisplayName("a tie with the last friend does not push the user off the board")
    void tieKeepsTheUserOnTheBoard() {
        stubUser(player("alice", 100));
        stubFriends(friends(10, 100));

        List<Player> board = scoreboardService.getScoreboard("alice");

        assertThat(board).hasSize(10);
        assertThat(names(board)).contains("alice");
    }

    @Test
    @DisplayName("players with no points yet are treated as zero, not an error")
    void nullPointsAreTreatedAsZero() {
        Player user = player("alice", 50);
        Player newcomer = new Player("newcomer", "secret");   // points still null
        stubUser(user);
        stubFriends(List.of(newcomer));

        List<Player> board = scoreboardService.getScoreboard("alice");

        assertThat(names(board)).containsExactly("alice", "newcomer");
    }

    @Test
    @DisplayName("a scoreboard for an unknown player is a 404")
    void unknownPlayerIsNotFound() {
        when(playerRepository.findByPlayer("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scoreboardService.getScoreboard("ghost"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ghost");
    }

    // ── fixtures ────────────────────────────────────────────────────────

    private void stubUser(Player user) {
        when(playerRepository.findByPlayer(user.getPlayer())).thenReturn(Optional.of(user));
    }

    private void stubFriends(List<Player> friends) {
        when(playerRepository.findTopFriendsByPoints(any(), any(Pageable.class)))
                .thenReturn(new ArrayList<>(friends));
    }

    private static List<Player> friends(int count, int points) {
        List<Player> friends = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            friends.add(player("friend" + i, points));
        }
        return friends;
    }

    private static Player player(String name, int points) {
        Player player = new Player(name, "secret");
        player.setPoints(points);
        return player;
    }

    private static List<String> names(List<Player> players) {
        return players.stream().map(Player::getPlayer).toList();
    }
}

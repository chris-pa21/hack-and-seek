package Diplo10.repository;

import Diplo10.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findTopFriendsByPoints is hand-written JPQL, so it is exercised against a
 * real database rather than a mock.
 */
@DataJpaTest
class PlayerRepositoryTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private PlayerRepository playerRepository;

    @Test
    @DisplayName("returns the player's friends ordered by points, highest first")
    void ordersFriendsByPointsDescending() {
        Player bob = persist("bob", 300);
        Player carol = persist("carol", 900);
        Player dave = persist("dave", 50);
        persistWithFriends("alice", 100, bob, carol, dave);

        List<Player> friends = playerRepository.findTopFriendsByPoints("alice", PageRequest.of(0, 10));

        assertThat(names(friends)).containsExactly("carol", "bob", "dave");
    }

    @Test
    @DisplayName("a friend with no points yet sorts as zero instead of being dropped")
    void nullPointsCountAsZero() {
        Player newcomer = persist("newcomer", null);
        Player bob = persist("bob", 10);
        persistWithFriends("alice", 100, newcomer, bob);

        List<Player> friends = playerRepository.findTopFriendsByPoints("alice", PageRequest.of(0, 10));

        // coalesce keeps them in the result, ranked last
        assertThat(names(friends)).containsExactly("bob", "newcomer");
    }

    @Test
    @DisplayName("the page size caps how many friends come back")
    void respectsThePageSize() {
        Player[] friends = new Player[15];
        for (int i = 0; i < 15; i++) {
            friends[i] = persist("friend" + i, i * 10);
        }
        persistWithFriends("alice", 0, friends);

        List<Player> top = playerRepository.findTopFriendsByPoints("alice", PageRequest.of(0, 10));

        assertThat(top).hasSize(10);
        // the ten highest, not an arbitrary ten
        assertThat(top.get(0).getPlayer()).isEqualTo("friend14");
        assertThat(top.get(9).getPlayer()).isEqualTo("friend5");
    }

    @Test
    @DisplayName("the player themselves is never part of their friend list")
    void doesNotIncludeThePlayer() {
        Player bob = persist("bob", 500);
        persistWithFriends("alice", 9999, bob);

        List<Player> friends = playerRepository.findTopFriendsByPoints("alice", PageRequest.of(0, 10));

        assertThat(names(friends)).containsExactly("bob");
    }

    @Test
    @DisplayName("a player with no friends gets an empty list, not an error")
    void noFriendsIsEmpty() {
        persist("loner", 100);

        List<Player> friends = playerRepository.findTopFriendsByPoints("loner", PageRequest.of(0, 10));

        assertThat(friends).isEmpty();
    }

    @Test
    @DisplayName("an unknown player gets an empty list")
    void unknownPlayerIsEmpty() {
        List<Player> friends = playerRepository.findTopFriendsByPoints("ghost", PageRequest.of(0, 10));

        assertThat(friends).isEmpty();
    }

    @Test
    @DisplayName("the query follows the owning side, so friendships must be stored both ways")
    void friendshipDirectionMatters() {
        Player bob = persist("bob", 100);
        persistWithFriends("alice", 50, bob);   // only alice -> bob is stored
        entityManager.flush();
        entityManager.clear();

        // this is why FriendService links both sides when a request is accepted
        assertThat(playerRepository.findTopFriendsByPoints("alice", PageRequest.of(0, 10)))
                .hasSize(1);
        assertThat(playerRepository.findTopFriendsByPoints("bob", PageRequest.of(0, 10)))
                .isEmpty();
    }

    // ── fixtures ────────────────────────────────────────────────────────

    private Player persist(String name, Integer points) {
        Player player = new Player(name, "secret");
        player.setPoints(points);
        return entityManager.persist(player);
    }

    private Player persistWithFriends(String name, Integer points, Player... friends) {
        Player player = new Player(name, "secret");
        player.setPoints(points);
        player.getFriends().addAll(List.of(friends));
        Player saved = entityManager.persist(player);
        entityManager.flush();
        return saved;
    }

    private static List<String> names(List<Player> players) {
        return players.stream().map(Player::getPlayer).toList();
    }
}

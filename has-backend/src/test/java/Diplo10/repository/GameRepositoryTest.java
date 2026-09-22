package Diplo10.repository;

import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findGamesByPlayer is hand-written JPQL joining two many-to-many tables, so it
 * is exercised against a real database rather than a mock.
 */
@DataJpaTest
class GameRepositoryTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private GameRepository gameRepository;

    @Test
    @DisplayName("finds games the player is a team member of")
    void findsGamesTheyPlayIn() {
        Player alice = persistPlayer("alice");
        Player creator = persistPlayer("creator");
        persistGame("alices-game", creator, persistTeam("alpha", alice));

        List<Game> games = gameRepository.findGamesByPlayer("alice");

        assertThat(names(games)).containsExactly("alices-game");
    }

    @Test
    @DisplayName("finds games the player created even when they are in no team")
    void findsGamesTheyCreated() {
        Player alice = persistPlayer("alice");
        Player bob = persistPlayer("bob");
        persistGame("organised-by-alice", alice, persistTeam("alpha", bob));

        List<Game> games = gameRepository.findGamesByPlayer("alice");

        assertThat(names(games)).containsExactly("organised-by-alice");
    }

    @Test
    @DisplayName("does not return games the player has nothing to do with")
    void ignoresUnrelatedGames() {
        Player alice = persistPlayer("alice");
        Player bob = persistPlayer("bob");
        Player carol = persistPlayer("carol");
        persistGame("someone-elses-game", bob, persistTeam("bravo", carol));
        persistGame("alices-game", alice, persistTeam("alpha", alice));

        List<Game> games = gameRepository.findGamesByPlayer("alice");

        assertThat(names(games)).containsExactly("alices-game");
    }

    @Test
    @DisplayName("a creator who also plays sees the game once, not twice")
    void creatorWhoPlaysGetsNoDuplicate() {
        Player alice = persistPlayer("alice");
        persistGame("alices-game", alice, persistTeam("alpha", alice));

        List<Game> games = gameRepository.findGamesByPlayer("alice");

        // this is what the DISTINCT in the query is for
        assertThat(games).hasSize(1);
    }

    @Test
    @DisplayName("a game with no teams is still found by its creator")
    void emptyGameIsFoundByCreator() {
        Player alice = persistPlayer("alice");
        persistGame("empty-lobby", alice);

        List<Game> games = gameRepository.findGamesByPlayer("alice");

        // the LEFT JOINs matter here: an inner join would hide the game
        assertThat(names(games)).containsExactly("empty-lobby");
    }

    @Test
    @DisplayName("a player in no games gets an empty list")
    void noGamesIsEmpty() {
        persistPlayer("loner");

        assertThat(gameRepository.findGamesByPlayer("loner")).isEmpty();
    }

    // ── fixtures ────────────────────────────────────────────────────────

    private Player persistPlayer(String name) {
        return entityManager.persist(new Player(name, "secret"));
    }

    private Team persistTeam(String name, Player... members) {
        Team team = new Team(name);
        team.getTeam_members().addAll(List.of(members));
        return entityManager.persist(team);
    }

    private Game persistGame(String name, Player creator, Team... teams) {
        Game game = new Game();
        game.setName(name);
        game.setCreated_by(creator);
        game.setTeam_list(new ArrayList<>(List.of(teams)));
        Game saved = entityManager.persist(game);
        entityManager.flush();
        return saved;
    }

    private static List<String> names(List<Game> games) {
        return games.stream().map(Game::getName).toList();
    }
}

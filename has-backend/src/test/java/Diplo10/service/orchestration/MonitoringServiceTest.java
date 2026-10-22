package Diplo10.service.orchestration;

import Diplo10.entity.ContainerEntity;
import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.repository.ContainerRepository;
import Diplo10.repository.GameRepository;
import Diplo10.repository.PlayerRepository;
import Diplo10.repository.SlaEventRepository;
import Diplo10.repository.TeamRepository;
import Diplo10.service.GameService;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the periodic checks in MonitoringService.monitorContainers.
 */
@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock private DockerClient dockerClient;
    @Mock private GameRepository gameRepository;
    @Mock private ContainerRepository containerRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private SlaEventRepository slaEventRepository;
    @Mock private GameService gameService;

    @InjectMocks private MonitoringService monitoringService;

    private Game game;
    private Team alpha;
    private Team bravo;

    @BeforeEach
    void setUp() {
        alpha = team("alpha", player("alice"));
        bravo = team("bravo", player("bob"));

        game = new Game();
        game.setId(1L);
        game.setName("test-game");
        game.setStarted_at(LocalDateTime.now());
        game.setTeam_list(new ArrayList<>(List.of(alpha, bravo)));
    }

    @Test
    @DisplayName("a disqualification that ends the game stops the checks, so the winner is not disqualified too")
    void winnerIsNotCheckedAfterTheGameEnds() {
        when(gameRepository.findAll()).thenReturn(List.of(game));
        when(containerRepository.findByGameAndDisqualifiedFalse(game))
                .thenReturn(List.of(container(alpha), container(bravo)));
        // alpha's container no longer answers, so its flag check fails
        when(dockerClient.execCreateCmd("container-alpha"))
                .thenThrow(new RuntimeException("container is not running"));
        // like the real endGame, which also removes the remaining containers
        doAnswer(invocation -> {
            game.setFinished_at(LocalDateTime.now());
            return null;
        }).when(gameService).endGame(game, bravo);

        monitoringService.monitorContainers();

        verify(gameService).endGame(game, bravo);
        verify(dockerClient, never()).execCreateCmd("container-bravo");
        assertThat(alpha.isDisqualified()).isTrue();
        assertThat(bravo.isDefeated()).isFalse();
        assertThat(bravo.isDisqualified()).isFalse();
        assertThat(bravo.getScore()).isZero();
    }

    // ── fixtures ────────────────────────────────────────────────────────

    private static Player player(String name) {
        Player player = new Player(name, "secret");
        player.setPoints(0);
        return player;
    }

    private static Team team(String name, Player member) {
        Team team = new Team(name);
        team.addPlayer(member);
        return team;
    }

    private static ContainerEntity container(Team team) {
        return ContainerEntity.builder()
                .containerId("container-" + team.getName())
                .team(team)
                .flag("HAS{" + team.getName() + "}")
                .build();
    }
}

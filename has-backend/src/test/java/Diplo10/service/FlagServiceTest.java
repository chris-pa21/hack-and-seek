package Diplo10.service;

import Diplo10.entity.ContainerEntity;
import Diplo10.entity.FlagSubmission;
import Diplo10.entity.Game;
import Diplo10.entity.Player;
import Diplo10.entity.Team;
import Diplo10.exception.NotFoundException;
import Diplo10.repository.ContainerRepository;
import Diplo10.repository.FlagSubmissionRepository;
import Diplo10.repository.GameRepository;
import Diplo10.repository.PlayerRepository;
import Diplo10.repository.TeamRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the attack-defense scoring rules in FlagService.submitFlag.
 */
@ExtendWith(MockitoExtension.class)
class FlagServiceTest {

    private static final long GAME_ID = 1L;
    private static final String ALPHA_FLAG = "HAS{alpha_flag}";
    private static final String BRAVO_FLAG = "HAS{bravo_flag}";
    private static final String CHARLIE_FLAG = "HAS{charlie_flag}";

    @Mock private GameRepository gameRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private ContainerRepository containerRepository;
    @Mock private FlagSubmissionRepository flagSubmissionRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private DockerClient dockerClient;
    @Mock private GameService gameService;

    @InjectMocks private FlagService flagService;

    private Game game;
    private Team alpha;
    private Team bravo;
    private Team charlie;
    private Player alice;
    private ContainerEntity bravoContainer;

    @BeforeEach
    void setUp() {
        alice = player("alice", 50);
        alpha = team("alpha", alice);
        bravo = team("bravo", player("bob", 0));
        charlie = team("charlie", player("carol", 0));

        game = new Game();
        game.setId(GAME_ID);
        game.setName("test-game");
        game.setTeam_list(new ArrayList<>(List.of(alpha, bravo, charlie)));

        bravoContainer = container(bravo, BRAVO_FLAG, 100);
    }

    // ── happy path ──────────────────────────────────────────────────────

    @Test
    @DisplayName("a valid flag from an opposing team scores and defeats that team")
    void validFlagCapturesTheTargetTeam() {
        stubGameAndContainers();
        stubDocker();

        Map<String, Object> result = flagService.submitFlag(GAME_ID, "alice", BRAVO_FLAG);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("points")).isEqualTo(100);
        assertThat(result.get("defeatedTeam")).isEqualTo("bravo");
        assertThat(result.get("gameOver")).isEqualTo(false);

        assertThat(bravo.isDefeated()).isTrue();
        assertThat(alpha.getScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("every member of the attacking team receives the full point value")
    void allAttackingMembersGetTheFullPoints() {
        Player dave = player("dave", 10);
        alpha.addPlayer(dave);
        stubGameAndContainers();
        stubDocker();

        flagService.submitFlag(GAME_ID, "alice", BRAVO_FLAG);

        assertThat(alice.getPoints()).isEqualTo(150);
        assertThat(dave.getPoints()).isEqualTo(110);
        // the team itself is credited once, not once per member
        assertThat(alpha.getScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("the capture is recorded as a FlagSubmission")
    void captureIsRecorded() {
        stubGameAndContainers();
        stubDocker();

        flagService.submitFlag(GAME_ID, "alice", BRAVO_FLAG);

        ArgumentCaptor<FlagSubmission> captor = ArgumentCaptor.forClass(FlagSubmission.class);
        verify(flagSubmissionRepository).save(captor.capture());

        FlagSubmission saved = captor.getValue();
        assertThat(saved.getAttackingTeam()).isEqualTo("alpha");
        assertThat(saved.getDefeatedTeam()).isEqualTo("bravo");
        assertThat(saved.getFlag()).isEqualTo(BRAVO_FLAG);
        assertThat(saved.getPointsAwarded()).isEqualTo(100);
    }

    @Test
    @DisplayName("the defeated team's container is stopped, removed and forgotten")
    void defeatedContainerIsTornDown() {
        stubGameAndContainers();
        stubDocker();

        flagService.submitFlag(GAME_ID, "alice", BRAVO_FLAG);

        verify(dockerClient).stopContainerCmd("container-bravo");
        verify(dockerClient).removeContainerCmd("container-bravo");
        verify(containerRepository).delete(bravoContainer);
    }

    @Test
    @DisplayName("defeating the last opponent ends the game and names the winner")
    void lastTeamStandingEndsTheGame() {
        // a two-team game: capturing bravo's flag leaves only alpha
        game.setTeam_list(new ArrayList<>(List.of(alpha, bravo)));
        stubGameAndContainers();
        stubDocker();

        Map<String, Object> result = flagService.submitFlag(GAME_ID, "alice", BRAVO_FLAG);

        assertThat(result.get("gameOver")).isEqualTo(true);
        assertThat(result.get("winnerTeam")).isEqualTo("alpha");
        verify(gameService).endGame(game, alpha);
    }

    // ── rejections ──────────────────────────────────────────────────────

    @Test
    @DisplayName("you cannot score with your own team's flag")
    void ownFlagIsRejected() {
        stubGameAndContainers();

        Map<String, Object> result = flagService.submitFlag(GAME_ID, "alice", ALPHA_FLAG);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("You cannot submit your own flag");
        assertThat(alpha.isDefeated()).isFalse();
        verify(flagSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("an unknown flag string is rejected")
    void unknownFlagIsRejected() {
        stubGameAndContainers();

        Map<String, Object> result = flagService.submitFlag(GAME_ID, "alice", "HAS{not_a_real_flag}");

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("Invalid flag");
        verify(flagSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("a team's flag can only be captured once per game")
    void alreadyCapturedFlagIsRejected() {
        stubGameAndContainers();
        when(flagSubmissionRepository.existsByGameIdAndDefeatedTeam(anyInt(), anyString()))
                .thenReturn(true);

        Map<String, Object> result = flagService.submitFlag(GAME_ID, "alice", BRAVO_FLAG);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("This flag has already been captured");
        verify(flagSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("a player who is in no team in this game cannot submit")
    void nonParticipantIsRejected() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        Map<String, Object> result = flagService.submitFlag(GAME_ID, "stranger", BRAVO_FLAG);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("You are not part of any team in this game");
    }

    @Test
    @DisplayName("a defeated team can no longer submit flags")
    void defeatedTeamIsRejected() {
        alpha.setDefeated(true);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        Map<String, Object> result = flagService.submitFlag(GAME_ID, "alice", BRAVO_FLAG);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("Your team has been defeated and cannot submit flags");
    }

    @Test
    @DisplayName("no submissions are accepted once the game has finished")
    void finishedGameIsRejected() {
        game.setFinished_at(LocalDateTime.now());
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        Map<String, Object> result = flagService.submitFlag(GAME_ID, "alice", BRAVO_FLAG);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("This game is already finished");
    }

    @Test
    @DisplayName("submitting to a game that does not exist is a 404")
    void unknownGameThrowsNotFound() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flagService.submitFlag(GAME_ID, "alice", BRAVO_FLAG))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Game not found");
    }

    // ── fixtures ────────────────────────────────────────────────────────

    private void stubGameAndContainers() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(containerRepository.findByGame(game)).thenReturn(List.of(
                container(alpha, ALPHA_FLAG, 100),
                bravoContainer,
                container(charlie, CHARLIE_FLAG, 100)));
    }

    private void stubDocker() {
        StopContainerCmd stopCmd = org.mockito.Mockito.mock(StopContainerCmd.class);
        RemoveContainerCmd removeCmd = org.mockito.Mockito.mock(RemoveContainerCmd.class);
        when(dockerClient.stopContainerCmd(anyString())).thenReturn(stopCmd);
        when(dockerClient.removeContainerCmd(anyString())).thenReturn(removeCmd);
    }

    private static Player player(String name, int points) {
        Player player = new Player(name, "secret");
        player.setPoints(points);
        return player;
    }

    private static Team team(String name, Player member) {
        Team team = new Team(name);
        team.addPlayer(member);
        return team;
    }

    private static ContainerEntity container(Team team, String flag, int points) {
        return ContainerEntity.builder()
                .containerId("container-" + team.getName())
                .team(team)
                .flag(flag)
                .points(points)
                .build();
    }
}

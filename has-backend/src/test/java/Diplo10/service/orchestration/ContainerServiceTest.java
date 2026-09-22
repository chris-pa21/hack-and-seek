package Diplo10.service.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.NetworkSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerServiceTest {

    // ── pure helpers ────────────────────────────────────────────────────

    @Nested
    @DisplayName("flag_path validation")
    class FlagPathValidation {

        @Test
        @DisplayName("the paths used by the real challenge images are accepted unchanged")
        void realImagePathsAreAccepted() {
            assertThat(ContainerService.safeFlagPath("/home/jdoe/flag.txt"))
                    .isEqualTo("/home/jdoe/flag.txt");
            assertThat(ContainerService.safeFlagPath("/home/www-data/flag.txt"))
                    .isEqualTo("/home/www-data/flag.txt");
            assertThat(ContainerService.safeFlagPath("/home/admin/.secret/flag.txt"))
                    .isEqualTo("/home/admin/.secret/flag.txt");
        }

        @Test
        @DisplayName("a missing label falls back to the default path")
        void missingLabelFallsBack() {
            assertThat(ContainerService.safeFlagPath(null))
                    .isEqualTo(ContainerService.DEFAULT_FLAG_PATH);
        }

        @Test
        @DisplayName("a label that would break out of the echo command is refused")
        void shellInjectionIsRefused() {
            // each of these would otherwise run inside the challenge container
            assertThat(ContainerService.safeFlagPath("/tmp/f.txt; rm -rf /"))
                    .isEqualTo(ContainerService.DEFAULT_FLAG_PATH);
            assertThat(ContainerService.safeFlagPath("/tmp/f.txt' ; id ; echo '"))
                    .isEqualTo(ContainerService.DEFAULT_FLAG_PATH);
            assertThat(ContainerService.safeFlagPath("/tmp/$(whoami)"))
                    .isEqualTo(ContainerService.DEFAULT_FLAG_PATH);
            assertThat(ContainerService.safeFlagPath("/tmp/`id`"))
                    .isEqualTo(ContainerService.DEFAULT_FLAG_PATH);
        }

        @Test
        @DisplayName("relative paths, traversal and directories are refused")
        void malformedPathsAreRefused() {
            assertThat(ContainerService.safeFlagPath("home/flag.txt"))
                    .isEqualTo(ContainerService.DEFAULT_FLAG_PATH);
            assertThat(ContainerService.safeFlagPath("/home/../etc/passwd"))
                    .isEqualTo(ContainerService.DEFAULT_FLAG_PATH);
            assertThat(ContainerService.safeFlagPath("/home/"))
                    .isEqualTo(ContainerService.DEFAULT_FLAG_PATH);
        }
    }

    @Nested
    @DisplayName("container naming")
    class ContainerNaming {

        @Test
        @DisplayName("team names are folded into the character set Docker accepts")
        void unsafeCharactersAreReplaced() {
            assertThat(ContainerService.safeContainerName("Team Alpha!")).isEqualTo("Team_Alpha_");
            assertThat(ContainerService.safeContainerName("ünïcode")).isEqualTo("_n_code");
            assertThat(ContainerService.safeContainerName("../evil")).isEqualTo(".._evil");
        }

        @Test
        @DisplayName("names that are already valid are left alone")
        void validNamesSurvive() {
            assertThat(ContainerService.safeContainerName("team-1_a.b")).isEqualTo("team-1_a.b");
        }
    }

    // ── spawning ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("spawnChallengeContainer")
    class Spawning {

        private static final String NETWORK_ID = "net-abc";
        private static final String CONTAINER_ID = "container-xyz";

        private DockerClient dockerClient;
        private ContainerService containerService;
        private CreateContainerCmd createCmd;

        @BeforeEach
        void setUp() {
            dockerClient = mock(DockerClient.class);
            containerService = new ContainerService(dockerClient);
            createCmd = mock(CreateContainerCmd.class, RETURNS_SELF);
        }

        @Test
        @DisplayName("reads points and flag path from the image labels")
        void readsImageLabels() {
            stubDocker(Map.of("flag_path", "/home/jdoe/flag.txt", "points", "250"));

            SpawnResult result = containerService.spawnChallengeContainer("leaky-blog", "alpha", NETWORK_ID);

            assertThat(result.points()).isEqualTo(250);
            assertThat(result.flagPath()).isEqualTo("/home/jdoe/flag.txt");
            assertThat(result.containerId()).isEqualTo(CONTAINER_ID);
        }

        @Test
        @DisplayName("an unreadable points label falls back to 100 rather than failing the spawn")
        void malformedPointsFallBack() {
            stubDocker(Map.of("points", "a lot"));

            SpawnResult result = containerService.spawnChallengeContainer("img", "alpha", NETWORK_ID);

            assertThat(result.points()).isEqualTo(100);
            assertThat(result.flagPath()).isEqualTo(ContainerService.DEFAULT_FLAG_PATH);
        }

        @Test
        @DisplayName("each container gets a fresh random flag in the HAS{...} format")
        void generatesARandomFlag() {
            stubDocker(Map.of());

            SpawnResult first = containerService.spawnChallengeContainer("img", "alpha", NETWORK_ID);
            SpawnResult second = containerService.spawnChallengeContainer("img", "bravo", NETWORK_ID);

            assertThat(first.flag()).matches("HAS\\{[0-9a-f]{32}\\}");
            assertThat(second.flag()).isNotEqualTo(first.flag());
            assertThat(first.rootPassword()).hasSize(8);
            assertThat(second.rootPassword()).isNotEqualTo(first.rootPassword());
        }

        @Test
        @DisplayName("the container's IP is taken from the game network it was attached to")
        void picksTheIpOnTheGameNetwork() {
            stubDocker(Map.of(), Map.of(
                    "bridge", network("other-net", "172.17.0.5"),
                    "game", network(NETWORK_ID, "172.20.0.3")));

            SpawnResult result = containerService.spawnChallengeContainer("img", "alpha", NETWORK_ID);

            assertThat(result.ipAddress()).isEqualTo("172.20.0.3");
        }

        @Test
        @DisplayName("resource limits and ownership labels are applied")
        void appliesLimitsAndLabels() {
            stubDocker(Map.of());

            containerService.spawnChallengeContainer("img", "Team Alpha!", NETWORK_ID);

            org.mockito.ArgumentCaptor<HostConfig> hostConfig =
                    org.mockito.ArgumentCaptor.forClass(HostConfig.class);
            org.mockito.verification.VerificationMode once = org.mockito.Mockito.times(1);
            org.mockito.Mockito.verify(createCmd, once).withHostConfig(hostConfig.capture());
            org.mockito.Mockito.verify(createCmd, once)
                    .withLabels(Map.of("owner", "Team Alpha!", "type", "challenge"));

            assertThat(hostConfig.getValue().getMemory()).isEqualTo(128 * 1024 * 1024L);
            assertThat(hostConfig.getValue().getCpuQuota()).isEqualTo(50000L);
            assertThat(hostConfig.getValue().getNetworkMode()).isEqualTo(NETWORK_ID);
        }

        // ── fixtures ────────────────────────────────────────────────────

        private void stubDocker(Map<String, String> labels) {
            stubDocker(labels, Map.of("game", network(NETWORK_ID, "172.20.0.2")));
        }

        private void stubDocker(Map<String, String> labels, Map<String, ContainerNetwork> networks) {
            ContainerConfig config = mock(ContainerConfig.class);
            when(config.getLabels()).thenReturn(labels);
            InspectImageResponse imageResponse = mock(InspectImageResponse.class);
            when(imageResponse.getConfig()).thenReturn(config);
            InspectImageCmd inspectImageCmd = mock(InspectImageCmd.class);
            when(inspectImageCmd.exec()).thenReturn(imageResponse);
            when(dockerClient.inspectImageCmd(anyString())).thenReturn(inspectImageCmd);

            CreateContainerResponse createResponse = new CreateContainerResponse();
            createResponse.setId(CONTAINER_ID);
            when(createCmd.exec()).thenReturn(createResponse);
            when(dockerClient.createContainerCmd(anyString())).thenReturn(createCmd);

            StartContainerCmd startCmd = mock(StartContainerCmd.class);
            when(dockerClient.startContainerCmd(anyString())).thenReturn(startCmd);

            NetworkSettings networkSettings = mock(NetworkSettings.class);
            when(networkSettings.getNetworks()).thenReturn(networks);
            InspectContainerResponse inspectResponse = mock(InspectContainerResponse.class);
            when(inspectResponse.getNetworkSettings()).thenReturn(networkSettings);
            InspectContainerCmd inspectContainerCmd = mock(InspectContainerCmd.class);
            when(inspectContainerCmd.exec()).thenReturn(inspectResponse);
            when(dockerClient.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);

            ExecCreateCmdResponse execResponse = mock(ExecCreateCmdResponse.class);
            when(execResponse.getId()).thenReturn("exec-1");
            ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class, RETURNS_SELF);
            when(execCreateCmd.exec()).thenReturn(execResponse);
            when(dockerClient.execCreateCmd(anyString())).thenReturn(execCreateCmd);

            ExecStartCmd execStartCmd = mock(ExecStartCmd.class, RETURNS_SELF);
            when(dockerClient.execStartCmd(anyString())).thenReturn(execStartCmd);
        }

        private ContainerNetwork network(String id, String ip) {
            ContainerNetwork network = mock(ContainerNetwork.class);
            when(network.getNetworkID()).thenReturn(id);
            when(network.getIpAddress()).thenReturn(ip);
            return network;
        }
    }
}

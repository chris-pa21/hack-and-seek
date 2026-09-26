package Diplo10.service.vpn;

import Diplo10.entity.Player;
import Diplo10.exception.VpnUnavailableException;
import Diplo10.repository.PlayerRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The server's public key is read from the running WireGuard interface, not
 * configured. These tests stand in for the WireGuard container: every command
 * run in it prints what the test chose.
 */
class VpnServiceTest {

    // example key from the wg(8) man page
    private static final String SERVER_KEY = "yAnz5TF+lXXJte14tji3zlMNq+hd2rYUIgJBgB3fBmk=";

    private DockerClient docker;
    private PlayerRepository playerRepository;
    private ExecCreateCmd execCreateCmd;
    private VpnService vpnService;

    private Player alice;

    @BeforeEach
    void setUp() {
        docker = mock(DockerClient.class);
        playerRepository = mock(PlayerRepository.class);
        vpnService = new VpnService(docker, mock(VpnManagerClient.class), playerRepository);
        ReflectionTestUtils.setField(vpnService, "serverEndpoint", "vpn.test:51820");

        alice = new Player("alice", "secret");
        alice.setVpnIp("10.8.19.2");
        alice.setVpnPrivateKey("ALICE_PRIVATE_KEY");
        alice.setVpnPublicKey("ALICE_PUBLIC_KEY");
    }

    @Test
    @DisplayName("the config carries the key the running interface reports")
    void configUsesTheLiveServerKey() {
        wireguardPrints(SERVER_KEY + "\n");

        String config = vpnService.generateConfig(alice);

        assertThat(config)
                .contains("PrivateKey = ALICE_PRIVATE_KEY")
                .contains("Address = 10.8.19.2/32")
                .contains("PublicKey = " + SERVER_KEY + "\n")
                .contains("Endpoint = vpn.test:51820");
        verify(execCreateCmd).withCmd("wg", "show", "wg0", "public-key");
    }

    @Test
    @DisplayName("no config is built while the WireGuard server is down")
    void noConfigWithoutWireguard() {
        wireguardPrints("");

        assertThatThrownBy(() -> vpnService.generateConfig(alice))
                .isInstanceOf(VpnUnavailableException.class);
    }

    @Test
    @DisplayName("output that is not a key never ends up in a config")
    void garbageIsNotAKey() {
        wireguardPrints("Unable to access interface: No such device\n");

        assertThatThrownBy(() -> vpnService.generateConfig(alice))
                .isInstanceOf(VpnUnavailableException.class);
    }

    @Test
    @DisplayName("registering a player's peer does not need the server key")
    void ensureVpnConfigLeavesTheServerKeyAlone() {
        wireguardPrints("");

        vpnService.ensureVpnConfig(alice);

        verify(execCreateCmd, never()).withCmd("wg", "show", "wg0", "public-key");
        // alice already has an IP and keys, so there is nothing to persist
        verify(playerRepository, never()).save(any());
    }

    /** Makes every command run in the WireGuard container print the given stdout. */
    private void wireguardPrints(String stdout) {
        ExecCreateCmdResponse created = mock(ExecCreateCmdResponse.class);
        when(created.getId()).thenReturn("exec-1");
        execCreateCmd = mock(ExecCreateCmd.class, RETURNS_SELF);
        when(execCreateCmd.exec()).thenReturn(created);
        when(docker.execCreateCmd(anyString())).thenReturn(execCreateCmd);

        ExecStartCmd execStartCmd = mock(ExecStartCmd.class, RETURNS_SELF);
        when(docker.execStartCmd("exec-1")).thenReturn(execStartCmd);
        when(execStartCmd.exec(any())).thenAnswer(inv -> {
            ResultCallback.Adapter<Frame> callback = inv.getArgument(0);
            callback.onNext(new Frame(StreamType.STDOUT, stdout.getBytes(StandardCharsets.UTF_8)));
            callback.onComplete();
            return callback;
        });
    }
}

package Diplo10.service.orchestration;

import Diplo10.entity.Game;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.command.ListNetworksCmd;
import com.github.dockerjava.api.model.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NetworkServiceTest {

    private DockerClient docker;
    private ListNetworksCmd listCmd;
    private CreateNetworkCmd createCmd;
    private NetworkService networkService;

    @BeforeEach
    void setUp() {
        docker = mock(DockerClient.class);
        listCmd = mock(ListNetworksCmd.class, RETURNS_SELF);
        createCmd = mock(CreateNetworkCmd.class, RETURNS_SELF);
        when(docker.listNetworksCmd()).thenReturn(listCmd);
        when(docker.createNetworkCmd()).thenReturn(createCmd);
        when(listCmd.exec()).thenReturn(List.of());
        CreateNetworkResponse created = mock(CreateNetworkResponse.class);
        when(created.getId()).thenReturn("new-network");
        when(createCmd.exec()).thenReturn(created);
        networkService = new NetworkService(docker);
    }

    @Test
    @DisplayName("two games with the same name get separate networks")
    void sameNameGamesGetSeparateNetworks() {
        networkService.createIsolatedNetwork(game(1L, "test"));
        networkService.createIsolatedNetwork(game(2L, "test"));

        verify(createCmd).withName("net_game1");
        verify(createCmd).withName("net_game2");
    }

    @Test
    @DisplayName("the network of the same game is reused instead of created again")
    void sameGameReusesItsNetwork() {
        Network existing = network("existing", "net_game7");
        when(listCmd.exec()).thenReturn(List.of(existing));

        String networkId = networkService.createIsolatedNetwork(game(7L, "test"));

        assertThat(networkId).isEqualTo("existing");
        verify(docker, never()).createNetworkCmd();
    }

    @Test
    @DisplayName("a network whose name only starts with the game's is not reused")
    void prefixMatchIsNotReused() {
        // Docker's name filter would return net_game10 when asked for net_game1
        Network other = network("other", "net_game10");
        when(listCmd.exec()).thenReturn(List.of(other));

        String networkId = networkService.createIsolatedNetwork(game(1L, "test"));

        assertThat(networkId).isEqualTo("new-network");
        verify(createCmd).withName("net_game1");
    }

    private static Game game(Long id, String name) {
        Game game = new Game();
        game.setId(id);
        game.setName(name);
        return game;
    }

    private static Network network(String id, String name) {
        Network network = mock(Network.class);
        when(network.getId()).thenReturn(id);
        when(network.getName()).thenReturn(name);
        return network;
    }
}

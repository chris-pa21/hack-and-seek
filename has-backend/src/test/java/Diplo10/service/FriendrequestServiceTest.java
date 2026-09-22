package Diplo10.service;

import Diplo10.entity.Friendrequest;
import Diplo10.entity.Player;
import Diplo10.exception.NotFoundException;
import Diplo10.repository.FriendrequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendrequestServiceTest {

    @Mock private FriendrequestRepository friendrequestRepository;
    @Mock private PlayerService playerService;
    @Mock private FriendService friendService;

    @InjectMocks private FriendrequestService friendrequestService;

    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        alice = new Player("alice", "secret");
        bob = new Player("bob", "secret");
    }

    @Test
    @DisplayName("a first request between two strangers is stored as pending")
    void createsPendingRequest() {
        stubPlayers();
        when(friendrequestRepository.existsByRequesterAndReceiver("alice", "bob")).thenReturn(false);
        when(friendrequestRepository.getFriendrequestByRequesterAndReceiver("bob", "alice"))
                .thenReturn(Optional.empty());
        when(friendrequestRepository.save(any(Friendrequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Friendrequest> result = friendrequestService.sendRequest("alice", "bob");

        assertThat(result).isPresent();
        assertThat(result.get().getRequester()).isEqualTo("alice");
        assertThat(result.get().getReceiver()).isEqualTo("bob");
        verify(friendService, never()).befriend(any(), any());
    }

    @Test
    @DisplayName("requesting someone who already requested you makes you friends immediately")
    void crossedRequestsBecomeAFriendship() {
        stubPlayers();
        Friendrequest incoming = new Friendrequest();
        incoming.setRequester("bob");
        incoming.setReceiver("alice");

        when(friendrequestRepository.existsByRequesterAndReceiver("alice", "bob")).thenReturn(false);
        when(friendrequestRepository.getFriendrequestByRequesterAndReceiver("bob", "alice"))
                .thenReturn(Optional.of(incoming));

        Optional<Friendrequest> result = friendrequestService.sendRequest("alice", "bob");

        // nothing stays pending: the mutual consent is resolved on the spot
        assertThat(result).isEmpty();
        verify(friendService).befriend(alice, bob);
        verify(friendrequestRepository).delete(incoming);
        verify(friendrequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("you cannot send a friend request to yourself")
    void selfRequestIsRejected() {
        assertThatThrownBy(() -> friendrequestService.sendRequest("alice", "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yourself");

        verify(friendrequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("a null requester or receiver is rejected")
    void nullArgumentsAreRejected() {
        assertThatThrownBy(() -> friendrequestService.sendRequest(null, "bob"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> friendrequestService.sendRequest("alice", null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(friendrequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("sending the same request twice is rejected")
    void duplicateRequestIsRejected() {
        stubPlayers();
        when(friendrequestRepository.existsByRequesterAndReceiver("alice", "bob")).thenReturn(true);

        assertThatThrownBy(() -> friendrequestService.sendRequest("alice", "bob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already sent");

        verify(friendrequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("you cannot request someone who is already your friend")
    void requestToExistingFriendIsRejected() {
        alice.getFriends().add(bob);
        stubPlayers();

        assertThatThrownBy(() -> friendrequestService.sendRequest("alice", "bob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already friends");

        verify(friendrequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("a request naming an unknown player is a 404")
    void unknownPlayerIsNotFound() {
        when(playerService.findByPlayer("alice")).thenReturn(alice);
        when(playerService.findByPlayer("ghost"))
                .thenThrow(new NotFoundException("Player with username ghost not found"));

        assertThatThrownBy(() -> friendrequestService.sendRequest("alice", "ghost"))
                .isInstanceOf(NotFoundException.class);

        verify(friendrequestRepository, never()).save(any());
    }

    private void stubPlayers() {
        when(playerService.findByPlayer("alice")).thenReturn(alice);
        when(playerService.findByPlayer("bob")).thenReturn(bob);
    }
}

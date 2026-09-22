package Diplo10.service;

import Diplo10.entity.Friendrequest;
import Diplo10.entity.Player;
import Diplo10.exception.NotFoundException;
import Diplo10.repository.FriendrequestRepository;
import Diplo10.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock private PlayerRepository playerRepository;
    @Mock private FriendrequestRepository friendrequestRepository;

    @InjectMocks private FriendService friendService;

    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        alice = new Player("alice", "secret");
        bob = new Player("bob", "secret");
    }

    @Test
    @DisplayName("accepting a request makes both players friends and clears the request")
    void acceptCreatesFriendshipBothWays() {
        Friendrequest request = request("bob", "alice");
        when(friendrequestRepository.getFriendrequestByRequesterAndReceiver("bob", "alice"))
                .thenReturn(Optional.of(request));
        stubPlayers();

        friendService.acceptFriendRequest("bob", "alice");

        assertThat(alice.getFriends()).contains(bob);
        assertThat(bob.getFriends()).contains(alice);
        verify(friendrequestRepository).delete(request);
    }

    @Test
    @DisplayName("refusing a request deletes it without creating a friendship")
    void refuseDeletesWithoutFriendship() {
        Friendrequest request = request("bob", "alice");
        when(friendrequestRepository.getFriendrequestByRequesterAndReceiver("bob", "alice"))
                .thenReturn(Optional.of(request));

        friendService.refuseFriendRequest("bob", "alice");

        assertThat(alice.getFriends()).isEmpty();
        assertThat(bob.getFriends()).isEmpty();
        verify(friendrequestRepository).delete(request);
        verify(playerRepository, never()).save(any());
    }

    @Test
    @DisplayName("accepting a request that does not exist is a 404")
    void acceptingMissingRequestIsNotFound() {
        when(friendrequestRepository.getFriendrequestByRequesterAndReceiver("ghost", "alice"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.acceptFriendRequest("ghost", "alice"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No pending friend request");
    }

    @Test
    @DisplayName("refusing a request that does not exist is a 404")
    void refusingMissingRequestIsNotFound() {
        when(friendrequestRepository.getFriendrequestByRequesterAndReceiver("ghost", "alice"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.refuseFriendRequest("ghost", "alice"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deleting a friend removes the link on both sides")
    void deleteRemovesBothSides() {
        alice.getFriends().add(bob);
        bob.getFriends().add(alice);
        stubPlayers();

        friendService.deleteFriend("alice", "bob");

        assertThat(alice.getFriends()).isEmpty();
        assertThat(bob.getFriends()).isEmpty();
    }

    @Test
    @DisplayName("deleting a friendship with an unknown player is a 404")
    void deletingUnknownPlayerIsNotFound() {
        when(playerRepository.findByPlayer("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.deleteFriend("ghost", "bob"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    @DisplayName("the friend list of an unknown player is empty rather than an error")
    void friendsOfUnknownPlayerIsEmpty() {
        when(playerRepository.findByPlayer("ghost")).thenReturn(Optional.empty());

        Set<Player> friends = friendService.getFriends("ghost");

        assertThat(friends).isEmpty();
    }

    private void stubPlayers() {
        when(playerRepository.findByPlayer("alice")).thenReturn(Optional.of(alice));
        when(playerRepository.findByPlayer("bob")).thenReturn(Optional.of(bob));
    }

    private static Friendrequest request(String requester, String receiver) {
        Friendrequest request = new Friendrequest();
        request.setRequester(requester);
        request.setReceiver(receiver);
        return request;
    }
}

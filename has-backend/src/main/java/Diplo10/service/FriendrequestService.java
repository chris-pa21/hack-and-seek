package Diplo10.service;

import Diplo10.entity.Friendrequest;
import Diplo10.entity.Player;
import Diplo10.repository.FriendrequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FriendrequestService {
    private final FriendrequestRepository friendrequestRepository;
    private final PlayerService playerService;
    private final FriendService friendService;

    public FriendrequestService(FriendrequestRepository friendrequestRepository,
                                PlayerService playerService,
                                FriendService friendService) {
        this.friendrequestRepository = friendrequestRepository;
        this.playerService = playerService;
        this.friendService = friendService;
    }

    /**
     * Sends a friend request.
     *
     * @return the created request, or an empty Optional when the receiver had
     *         already requested the sender: that is mutual consent, so the
     *         friendship is created immediately and no request is left pending.
     */
    @Transactional
    public Optional<Friendrequest> sendRequest(String requester, String receiver) {
        if (requester == null || receiver == null) {
            throw new IllegalArgumentException("Requester and receiver must be provided");
        }

        if (requester.equals(receiver)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself");
        }

        // Validate that both players exist (throws 404 via GlobalExceptionHandler if not)
        Player requesterPlayer = playerService.findByPlayer(requester);
        Player receiverPlayer = playerService.findByPlayer(receiver);

        if (requesterPlayer.getFriends().stream()
                .anyMatch(friend -> friend.getPlayer().equals(receiver))) {
            throw new IllegalStateException("You are already friends");
        }

        // Prevent duplicate requests in the same direction
        if (friendrequestRepository.existsByRequesterAndReceiver(requester, receiver)) {
            throw new IllegalStateException("Friend request already sent");
        }

        // The other player already asked: treat this as accepting their request
        Optional<Friendrequest> incoming =
                friendrequestRepository.getFriendrequestByRequesterAndReceiver(receiver, requester);
        if (incoming.isPresent()) {
            friendService.befriend(requesterPlayer, receiverPlayer);
            friendrequestRepository.delete(incoming.get());
            return Optional.empty();
        }

        Friendrequest friendrequest = new Friendrequest();
        friendrequest.setRequester(requester);
        friendrequest.setReceiver(receiver);
        return Optional.of(this.friendrequestRepository.save(friendrequest));
    }

    @Transactional
    public List<Friendrequest> getAllFriendRequests(String username) {
        return friendrequestRepository.findByReceiver(username);
    }
}

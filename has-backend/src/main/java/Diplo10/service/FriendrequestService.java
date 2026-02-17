package Diplo10.service;

import Diplo10.entity.Friendrequest;
import Diplo10.repository.FriendrequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendrequestService {
    private final FriendrequestRepository friendrequestRepository;
    private final PlayerService playerService;

    public FriendrequestService(FriendrequestRepository friendrequestRepository, PlayerService playerService) {
        this.friendrequestRepository = friendrequestRepository;
        this.playerService = playerService;
    }

    @Transactional
    public Friendrequest sendRequest(String requester, String receiver) {
        if (requester == null || receiver == null) {
            throw new IllegalArgumentException("Requester and receiver must be provided");
        }

        if (requester.equals(receiver)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself");
        }

        // Validate that both players exist (throws 404 via GlobalExceptionHandler if not)
        playerService.findByPlayer(requester);
        playerService.findByPlayer(receiver);

        // Prevent duplicate requests in the same direction
        if (friendrequestRepository.existsByRequesterAndReceiver(requester, receiver)) {
            throw new IllegalStateException("Friend request already sent");
        }

        Friendrequest friendrequest = new Friendrequest();
        friendrequest.setRequester(requester);
        friendrequest.setReceiver(receiver);
        return this.friendrequestRepository.save(friendrequest);
    }


    @Transactional
    public List<Friendrequest> getAllFriendRequests(String username) {
        return friendrequestRepository.findByReceiver(username);
    }


}

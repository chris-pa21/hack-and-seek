package Diplo10.controller;

import Diplo10.dto.PlayerSummary;
import Diplo10.entity.Friendrequest;
import Diplo10.entity.Player;
import Diplo10.service.FriendService;
import Diplo10.service.FriendrequestService;
import Diplo10.service.PlayerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Friend requests and friendships of the logged-in player. The acting player
 * is always taken from the session: no request names "who I am", so nobody
 * can send, accept, refuse or delete in someone else's name, or read another
 * player's friends and requests.
 */
@RestController
@RequestMapping("/api/friend")
public class FriendController {

    private final FriendService friendService;
    private final FriendrequestService friendrequestService;
    private final PlayerService playerService;

    public FriendController(FriendService friendService, FriendrequestService friendrequestService, PlayerService playerService) {
        this.playerService = playerService;
        this.friendService = friendService;
        this.friendrequestService = friendrequestService;
    }

    public record CreateFriendRequest(String receiver) {}

    /**
     * status is REQUEST_SENT (201, request pending) or FRIENDSHIP_CREATED
     * (200, the receiver had already asked, so both are now friends).
     */
    public record CreateFriendResponse(String status, Friendrequest request) {}

    @PostMapping
    public ResponseEntity<CreateFriendResponse> createFriendRequest(@RequestBody CreateFriendRequest request,
                                                                    Authentication authentication) {
        return friendrequestService.sendRequest(authentication.getName(), request.receiver())
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new CreateFriendResponse("REQUEST_SENT", saved)))
                .orElseGet(() -> ResponseEntity.ok(
                        new CreateFriendResponse("FRIENDSHIP_CREATED", null)));
    }


    public record GetFriendRequestsResponse(String username, Integer points) {}

    @GetMapping("/requests")
    public ResponseEntity<List<GetFriendRequestsResponse>> getFriendRequests(Authentication authentication) {
        List<GetFriendRequestsResponse> response = new ArrayList<>();

        List<Friendrequest> requests = friendrequestService.getAllFriendRequests(authentication.getName());
        for (Friendrequest req : requests) {
            Player requester = playerService.findByPlayer(req.getRequester());
            response.add(new GetFriendRequestsResponse(req.getRequester(), requester.getPoints()));
        }

        return ResponseEntity.ok(response);
    }


    public record AcceptFriendRequestRequest(String requester) {}

    @PostMapping("/acceptRequest")
    public ResponseEntity<Void> acceptFriendRequest(@RequestBody AcceptFriendRequestRequest request,
                                                    Authentication authentication) {
        friendService.acceptFriendRequest(request.requester(), authentication.getName());
        return ResponseEntity.ok().build();
    }


    public record RefuseFriendRequestRequest(String requester) {}

    @PostMapping("/refuseRequest")
    public ResponseEntity<Void> refuseFriendRequest(@RequestBody RefuseFriendRequestRequest request,
                                                    Authentication authentication) {
        friendService.refuseFriendRequest(request.requester(), authentication.getName());
        return ResponseEntity.ok().build();
    }


    @GetMapping("/friends")
    public ResponseEntity<List<PlayerSummary>> getFriends(Authentication authentication) {
        List<PlayerSummary> friends = friendService.getFriends(authentication.getName()).stream()
                .map(PlayerSummary::from)
                .toList();
        return ResponseEntity.ok(friends);
    }


    @DeleteMapping
    public ResponseEntity<Void> deleteFriend(@RequestParam String friend, Authentication authentication) {
        friendService.deleteFriend(authentication.getName(), friend);
        return ResponseEntity.ok().build();
    }
}

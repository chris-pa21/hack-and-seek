package Diplo10.controller;

import Diplo10.entity.Friendrequest;
import Diplo10.entity.Player;
import Diplo10.service.FriendService;
import Diplo10.service.FriendrequestService;
import Diplo10.service.PlayerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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

    public record CreateFriendRequest(String requester, String receiver) {}

    /**
     * status is REQUEST_SENT (201, request pending) or FRIENDSHIP_CREATED
     * (200, the receiver had already asked, so both are now friends).
     */
    public record CreateFriendResponse(String status, Friendrequest request) {}

    @PostMapping
    public ResponseEntity<CreateFriendResponse> createFriendRequest(@RequestBody CreateFriendRequest request) {
        return friendrequestService.sendRequest(request.requester(), request.receiver())
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new CreateFriendResponse("REQUEST_SENT", saved)))
                .orElseGet(() -> ResponseEntity.ok(
                        new CreateFriendResponse("FRIENDSHIP_CREATED", null)));
    }


    public record GetFriendRequestsResponse(String username, Integer points) {}

    @GetMapping("/requests")
    public ResponseEntity<List<GetFriendRequestsResponse>> getFriendRequests(@RequestParam String username) {
        List<GetFriendRequestsResponse> response = new ArrayList<>();

        List<Friendrequest> requests = friendrequestService.getAllFriendRequests(username);
        for (Friendrequest req : requests) {
            Player requester = playerService.findByPlayer(req.getRequester());
            response.add(new GetFriendRequestsResponse(req.getRequester(), requester.getPoints()));
        }

        return ResponseEntity.ok(response);
    }


    public record AcceptFriendRequestRequest(String requester, String accepter) {}

    @PostMapping("/acceptRequest")
    public ResponseEntity<Void> acceptFriendRequest(@RequestBody AcceptFriendRequestRequest request) {
        friendService.acceptFriendRequest(request.requester, request.accepter);
        return ResponseEntity.ok().build();
    }


    public record RefuseFriendRequestRequest(String requester, String refuser) {}

    @PostMapping("/refuseRequest")
    public ResponseEntity<Void> refuseFriendRequest(@RequestBody RefuseFriendRequestRequest request) {
        friendService.refuseFriendRequest(request.requester, request.refuser);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/friends")
    public ResponseEntity<List<Player>> getFriends(@RequestParam String username) {
        List<Player> friends = new ArrayList<>(friendService.getFriends(username));
        return ResponseEntity.ok(friends);
    }


    @DeleteMapping
    public ResponseEntity<Void> deleteFriendRequest(@RequestParam String username, @RequestParam String friend){
        friendService.deleteFriend(username, friend);
        return ResponseEntity.ok().build();
    }
}

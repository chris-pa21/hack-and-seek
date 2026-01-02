package Diplo10.controller;

import Diplo10.service.FriendService;
import Diplo10.service.FriendrequestService;
import Diplo10.entity.Friendrequest;
import Diplo10.entity.Player;
import Diplo10.repository.FriendrequestRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/friend")
public class FriendController {

    private final FriendService friendService;
    private final FriendrequestService friendrequestService;
    private final FriendrequestRepository friendrequestRepository;

    public FriendController(FriendService friendService, FriendrequestService friendrequestService, FriendrequestRepository friendrequestRepository) {
        this.friendService = friendService;
        this.friendrequestService = friendrequestService;
        this.friendrequestRepository = friendrequestRepository;
    }

    @GetMapping("/get")
    public ResponseEntity<?> get(@RequestParam String name){
        try {
            Set<?> saved = this.friendService.getFriends(name);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/sendRequests")
    public ResponseEntity<?> sendRequests(@RequestBody Map<String, Player> request, @RequestParam String message) {
        try {
            Player player = request.get("player");
            Player req = request.get("req");

            Friendrequest friendrequest = this.friendrequestService.sendRequest(player,req,message);
            return ResponseEntity.ok(friendrequest);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/acceptRequest")
    public ResponseEntity<?> acceptRequest(@RequestParam int id) {
        try {
            Friendrequest friendrequest = friendrequestRepository.findById(id);
            friendService.addFriend(friendrequest.getRequester(), friendrequest.getReciever());
            return ResponseEntity.status(200).body("ok:");

        }catch(RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

}

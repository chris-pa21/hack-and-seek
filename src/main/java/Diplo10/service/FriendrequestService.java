package Diplo10.service;
import Diplo10.entity.Friendrequest;
import Diplo10.entity.Player;
import Diplo10.repository.FriendrequestRepository;
import org.springframework.stereotype.Service;

@Service
public class FriendrequestService {
    private final FriendrequestRepository friendrequestRepository;
    private final FriendService friendService;

    public FriendrequestService(FriendrequestRepository friendrequestRepository, FriendService friendService) {
        this.friendrequestRepository = friendrequestRepository;
        this.friendService = friendService;
    }

    public Friendrequest sendRequest(Player player1, Player player2, String message){
        Friendrequest friendrequest = new Friendrequest();
        friendrequest.setRequester(player1);
        friendrequest.setReciever(player2);
        friendrequest.setMessage(message);

        return this.friendrequestRepository.save(friendrequest);
    }

    public void acceptRequest(int id){
        Friendrequest friendrequest = this.friendrequestRepository.findById(id);
        Player requester = friendrequest.getRequester();
        Player reciever = friendrequest.getReciever();

        friendService.addFriend(requester,reciever);
    }
}
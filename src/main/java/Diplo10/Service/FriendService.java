package Diplo10.Service;

import Diplo10.entity.Friends;
import Diplo10.entity.Player;
import Diplo10.entity.PlayerDTO;
import Diplo10.repository.FriendRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;

import java.util.HashSet;
import java.util.Set;
@Service
public class FriendService {
    private final FriendRepository friendRepository;
    @Autowired
    private ModelMapper modelMapper;

    public FriendService(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }

    public Friends addFriend(Player player, Player req) {
        // Prüfen, ob es schon einen Eintrag für diesen Spieler gibt
        Friends friendList = this.friendRepository.findByUsername(player.getPlayer());

        if (friendList == null) {
            // Neuer Freundes-Eintrag für diesen Spieler
            Friends friend = new Friends();
            friend.setUsername(player.getPlayer());
            friend.addFriend(req);
            this.friendRepository.save(friend);
            //return friend;
        }
        else {
            friendList.addFriend(req);
            this.friendRepository.save(friendList);
        }

        //return friendList;

        friendList = this.friendRepository.findByUsername(req.getPlayer());

        if (friendList == null) {
            // Neuer Freundes-Eintrag für diesen Spieler
            Friends friend = new Friends();
            friend.setUsername(req.getPlayer());
            friend.addFriend(player);
            this.friendRepository.save(friend);
            //return friend;
        }
        else {
            friendList.addFriend(req);
            this.friendRepository.save(friendList);
        }


        return friendList;
    }


    public void newFriendlist(String name){
        Friends f = new Friends();
        f.setUsername(name);
        this.friendRepository.save(f);
    }

    public Set<PlayerDTO> getFriends(String name) {
        Set<PlayerDTO> t = new HashSet<>();
        Friends f = this.friendRepository.findByUsername(name);

        if (f == null) {
            newFriendlist(name);
            f = this.friendRepository.findByUsername(name);
        }
        for(Player player: f.getFriends()){

            t.add(this.modelMapper.map(player, PlayerDTO.class));
            //t.add(new PlayerDTO(player.getPlayer()));
        }
        return t;
    }
}
package Diplo10.service;

import Diplo10.entity.Friendrequest;
import Diplo10.entity.Player;
import Diplo10.repository.FriendrequestRepository;
import Diplo10.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class FriendService {
    private final PlayerRepository playerRepository;
    private final FriendrequestRepository friendrequestRepository;

    public FriendService(PlayerRepository playerRepository, FriendrequestRepository friendrequestRepository) {
        this.playerRepository = playerRepository;
        this.friendrequestRepository = friendrequestRepository;
    }

    @Transactional
    public void acceptFriendRequest(String requester, String accepter) {
        Optional<Friendrequest> request = friendrequestRepository.getFriendrequestByRequesterAndReceiver(requester, accepter);
        Player requesterPlayer = playerRepository.findByPlayer(requester).get();
        Player accepterPlayer = playerRepository.findByPlayer(accepter).get();

        requesterPlayer.getFriends().add(accepterPlayer);
        accepterPlayer.getFriends().add(requesterPlayer);

        playerRepository.save(requesterPlayer);
        playerRepository.save(accepterPlayer);

        friendrequestRepository.delete(request.get());
    }

    @Transactional
    public Set<Player> getFriends(String name) {
        return playerRepository.findByPlayer(name)
                .map(Player::getFriends)
                .orElseGet(HashSet::new);
    }



}

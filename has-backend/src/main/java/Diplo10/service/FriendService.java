package Diplo10.service;

import Diplo10.entity.Friendrequest;
import Diplo10.entity.Player;
import Diplo10.exception.NotFoundException;
import Diplo10.repository.FriendrequestRepository;
import Diplo10.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
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
        Friendrequest request = findRequest(requester, accepter);
        Player requesterPlayer = findPlayer(requester);
        Player accepterPlayer = findPlayer(accepter);

        befriend(requesterPlayer, accepterPlayer);

        friendrequestRepository.delete(request);
    }

    @Transactional
    public void refuseFriendRequest(String requester, String refuser) {
        friendrequestRepository.delete(findRequest(requester, refuser));
    }

    @Transactional
    public Set<Player> getFriends(String name) {
        return playerRepository.findByPlayer(name)
                .map(Player::getFriends)
                .orElseGet(HashSet::new);
    }

    @Transactional
    public void deleteFriend(String player, String friend) {
        Player playerEntity = findPlayer(player);
        Player friendEntity = findPlayer(friend);

        playerEntity.getFriends().remove(friendEntity);
        friendEntity.getFriends().remove(playerEntity);

        playerRepository.save(playerEntity);
        playerRepository.save(friendEntity);
    }

    /**
     * Links two players as friends in both directions.
     */
    @Transactional
    public void befriend(Player one, Player other) {
        one.getFriends().add(other);
        other.getFriends().add(one);

        playerRepository.save(one);
        playerRepository.save(other);
    }

    private Player findPlayer(String username) {
        return playerRepository.findByPlayer(username)
                .orElseThrow(() -> new NotFoundException("Player with username " + username + " not found"));
    }

    private Friendrequest findRequest(String requester, String receiver) {
        return friendrequestRepository.getFriendrequestByRequesterAndReceiver(requester, receiver)
                .orElseThrow(() -> new NotFoundException(
                        "No pending friend request from " + requester + " to " + receiver));
    }
}

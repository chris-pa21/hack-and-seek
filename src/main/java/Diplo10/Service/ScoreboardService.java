package Diplo10.Service;

import Diplo10.entity.PlayerDTO;
import Diplo10.repository.PlayerRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScoreboardService {
    private final FriendService friendService;
    @Autowired
    private ModelMapper modelMapper;
    private final PlayerRepository playerRepository;

    public ScoreboardService(FriendService friendService, PlayerRepository playerRepository) {
        this.friendService = friendService;
        this.playerRepository = playerRepository;
    }

    public List<PlayerDTO> get (String name){
        Set saved = this.friendService.getFriends(name);
        saved.add(modelMapper.map(this.playerRepository.findByPlayer(name), PlayerDTO.class));
        ArrayList<PlayerDTO> list = new ArrayList<>(saved);
        Collections.sort(list);
        Collections.reverse(list);

        return list;
    }
}

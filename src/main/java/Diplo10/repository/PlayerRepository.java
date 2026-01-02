package Diplo10.repository;

import Diplo10.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String>{
    Optional<Player> findByPlayer(String username);

    Optional<Player> findByEmail(String email);
}


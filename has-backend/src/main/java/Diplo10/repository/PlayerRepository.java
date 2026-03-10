package Diplo10.repository;

import Diplo10.entity.Player;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String>{
    Optional<Player> findByPlayer(String username);

    @Query("""
       select f
       from Player p
       join p.friends f
       where p.player = :playerName
       order by coalesce(f.points, 0) desc
       """)
    List<Player> findTopFriendsByPoints(@Param("playerName") String playerName, Pageable pageable);
}


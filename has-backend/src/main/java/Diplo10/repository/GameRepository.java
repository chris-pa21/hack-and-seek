package Diplo10.repository;

import Diplo10.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findById(Long id);

    @Query("SELECT DISTINCT g FROM Game g LEFT JOIN g.team_list t LEFT JOIN t.team_members m " +
           "WHERE m.player = :username OR g.created_by.player = :username")
    List<Game> findGamesByPlayer(@Param("username") String username);
}

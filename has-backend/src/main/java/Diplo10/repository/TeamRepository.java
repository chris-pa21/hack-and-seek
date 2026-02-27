package Diplo10.repository;

import Diplo10.entity.Player;
import Diplo10.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, String> {
    Optional<Team> findByName(String teamName);
    List<Team> findByGameId(int gameId);
    Optional<Team> findByGameIdAndName(int gameId, String teamName);
}

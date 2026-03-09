package Diplo10.repository;

import Diplo10.entity.ContainerEntity;
import Diplo10.entity.Game;
import Diplo10.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContainerRepository extends JpaRepository<ContainerEntity, Long> {
    Optional<ContainerEntity> findByTeam(Team team);
    List<ContainerEntity> findByGame(Game game);
    Optional<ContainerEntity> findByContainerId(String containerId);
    void deleteByContainerId(String containerId);
    List<ContainerEntity> findByGameAndDisqualifiedFalse(Game game);
}

package Diplo10.repository;

import Diplo10.entity.ContainerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContainerRepository extends JpaRepository<ContainerEntity, String> {
    List<ContainerEntity> findByTeamName(String teamName);
    void deleteByContainerId(String containerId);
}

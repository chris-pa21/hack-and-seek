package Diplo10.repository;

import Diplo10.entity.FlagSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlagSubmissionRepository extends JpaRepository<FlagSubmission, Long> {
    boolean existsByGameIdAndDefeatedTeam(int gameId, String defeatedTeam);
    List<FlagSubmission> findByGameIdOrderBySubmittedAtDesc(int gameId);
}

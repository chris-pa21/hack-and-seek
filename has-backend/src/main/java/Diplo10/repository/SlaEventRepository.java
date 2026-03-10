package Diplo10.repository;

import Diplo10.entity.SlaEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlaEventRepository extends JpaRepository<SlaEvent, Long> {
    List<SlaEvent> findByGameIdOrderByCheckedAtDesc(int gameId);
}

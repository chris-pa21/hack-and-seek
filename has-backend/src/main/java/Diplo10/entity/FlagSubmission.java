package Diplo10.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlagSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private int gameId;
    private String attackingTeam;
    private String defeatedTeam;
    private String flag;
    private int pointsAwarded;
    private LocalDateTime submittedAt;
}

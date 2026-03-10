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
public class SlaEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private int gameId;
    private String teamName;
    private String containerId;
    private String checkType;
    private boolean passed;
    private String details;
    private LocalDateTime checkedAt;
}

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
public class ContainerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String containerId;

    @OneToOne
    @JoinColumn(name = "team_name")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    private String networkId;
    private String image;
    private LocalDateTime createdAt;
    private String ipAddress;
    private String flag;
}

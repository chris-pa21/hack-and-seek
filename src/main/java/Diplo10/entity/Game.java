package Diplo10.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String Name;
    @ManyToOne
    @JoinColumn(name = "created_by_player")
    private Player created_by;

    private Date created_at;

    private LocalDateTime started_at;

    private int points;

    private String description;

    @ManyToMany
    private List<Team> team_list;

    public void addTeam(Team team){
        team_list.add(team);
    }

    public Team findTeam(int teamId){

    }
}

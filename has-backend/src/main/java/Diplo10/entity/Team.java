package Diplo10.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Team {

    @Id
    private String name;

    private int gameId;

    private String challenge;

    private int score;

    private boolean defeated;

    private boolean disqualified;

    @ManyToMany
    private List<Player> team_members;

    public Team(String name, int gameId) {
        this.name = name;
        this.gameId = gameId;
        this.team_members = new ArrayList<>();
    }

    public Team(String name) {
        this(name, 0);
    }

    public void addPlayer(Player player){
        this.team_members.add(player);
    }

    public void deletePlayer(Player player){
        this.team_members.remove(player);
    }

}

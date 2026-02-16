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

    private int game_id;

    @ManyToMany
    private List<Player> team_members;

    public Team(String name) {
        this.name = name;
        this.game_id = game_id;
        this.team_members = new ArrayList<>();
    }

    public void addPlayer(Player player){
        this.team_members.add(player);
    }

    public void deletePlayer(Player player){
        this.team_members.remove(player);
    }

}

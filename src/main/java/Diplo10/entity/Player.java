package Diplo10.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Player {
    @Id
    private String player;

    private String password;

    private String email;

    private Integer points;

    private String vpn_key;

    @ManyToMany
    @JoinTable(
            name = "player_friends",
            joinColumns = @JoinColumn(name = "player_username"),
            inverseJoinColumns = @JoinColumn(name = "friend_username")
    )
    @ToString.Exclude
    private Set<Player> friends = new HashSet<>();

    public Player(String name, String password, String email) {
        this.player = name;
        this.password = password;
        this.email = email;
        this.points = 0;
    }
}

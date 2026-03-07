package Diplo10.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Player {
    @Id
    @EqualsAndHashCode.Include
    private String player;

    private String password;

    private String email;

    private Integer points;

    private String vpnIp;

    private String vpnPublicKey;
    
    private String vpnServerPublicKey;

    @ManyToMany
    @JoinTable(
            name = "player_friends",
            joinColumns = @JoinColumn(name = "player_username"),
            inverseJoinColumns = @JoinColumn(name = "friend_username")
    )
    @ToString.Exclude
    @JsonIgnore
    private Set<Player> friends = new HashSet<>();

    public Player(String name, String password, String email) {
        this.player = name;
        this.password = password;
        this.email = email;
        this.points = 0;
    }
}

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

    // Controllers answer with DTOs (see Diplo10.dto). These annotations are a
    // second line of defence: should a Player ever be serialized by accident,
    // its credentials and VPN data still stay out of the JSON.
    @JsonIgnore
    private String password;

    private Integer points;

    @JsonIgnore
    private String vpnIp;

    @JsonIgnore
    private String vpnPrivateKey;

    @JsonIgnore
    private String vpnPublicKey;

    @JsonIgnore
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

    public Player(String name, String password) {
        this.player = name;
        this.password = password;
        this.points = 0;
    }
}

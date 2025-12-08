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
public class Friends {
    @Id
    @Column(unique = true, nullable = false)
    private String username;

    // Eine Liste aller Freundschaftsbeziehungen, an denen der User beteiligt ist
    // @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    @OneToMany
    @ToString.Exclude
    private Set<Player> friends = new HashSet<>();

    public void addFriend(Player player){
        this.friends.add(player);
    }

}
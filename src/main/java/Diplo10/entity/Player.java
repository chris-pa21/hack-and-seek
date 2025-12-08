package Diplo10.entity;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

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


    public Player(String name, String password, String email){
        this.player = name;
        this.password = password;
        this.email = email;
        this.points = 0;

    }
    @PostConstruct
    public void createFriendlist(){
        //
        // newFriendlist();
        Friends f = new Friends();
        f.setUsername(player);
    }
}
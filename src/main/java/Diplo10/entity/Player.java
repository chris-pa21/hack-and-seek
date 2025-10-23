package Diplo10.entity;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String player;

    private String password;

    private String email;

    private int points;

    private String vpn_key;


    public Player(String name, String password, String email){
        this.id = (int) (Math.random() * 1000);
        this.player = name;
        this.password = password;
        this.email = email;
        this.points = 0;

    }


}
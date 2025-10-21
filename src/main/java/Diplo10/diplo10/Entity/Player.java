package Diplo10.diplo10.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
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

    public Player(){
    }


    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }


    public String getPlayer() {
        return player;
    }

    public void setPlayer(String user) {
        this.player = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
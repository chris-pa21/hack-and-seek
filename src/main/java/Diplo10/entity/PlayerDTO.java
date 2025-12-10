package Diplo10.entity;
import lombok.*;


@Getter
@Setter
public class PlayerDTO implements Comparable<PlayerDTO>{
    private String player;
    private int points;

    public PlayerDTO() {
        super();
    }

    public PlayerDTO(String player) {
        player = player;
    }

    @Override
    public int compareTo(PlayerDTO o) {
        return this.getPoints()-o.getPoints();
    }
}



package Diplo10.entity;
import lombok.*;


@Getter
@Setter
public class PlayerDTO {
    private String player;

    public PlayerDTO() {
        super();
    }

    public PlayerDTO(String player) {
        player = player;
    }
}



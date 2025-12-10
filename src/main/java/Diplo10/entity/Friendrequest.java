package Diplo10.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Friendrequest {
    @Id
    @GeneratedValue
    private int id;
    @ManyToOne(optional = false)
    private Player requester;
    @ManyToOne(optional = false)
    private Player reciever;
    private String message;


}


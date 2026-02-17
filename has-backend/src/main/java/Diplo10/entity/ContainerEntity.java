package Diplo10.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerEntity {

    @Id
    private String containerId; // Die ID von Docker
    private String teamName;
    private String networkId;
    private String image;
    private LocalDateTime createdAt;





}
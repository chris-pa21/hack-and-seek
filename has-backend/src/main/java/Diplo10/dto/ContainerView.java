package Diplo10.dto;

import Diplo10.entity.ContainerEntity;

import java.time.LocalDateTime;

/**
 * A challenge container as returned by the API. Refers to its team and game by
 * name and id only: the entities would drag every member's Player record, with
 * password hash and VPN keys, into the response.
 */
public record ContainerView(
        String containerId,
        Long gameId,
        String teamName,
        String image,
        String ipAddress,
        LocalDateTime createdAt,
        boolean disqualified
) {

    public static ContainerView from(ContainerEntity container) {
        return new ContainerView(
                container.getContainerId(),
                container.getGame() != null ? container.getGame().getId() : null,
                container.getTeam() != null ? container.getTeam().getName() : null,
                container.getImage(),
                container.getIpAddress(),
                container.getCreatedAt(),
                container.isDisqualified());
    }
}

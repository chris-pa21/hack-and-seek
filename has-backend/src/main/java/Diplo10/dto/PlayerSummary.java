package Diplo10.dto;

import Diplo10.entity.Player;

/**
 * A player as other players see them. Controllers return this instead of the
 * Player entity, which also carries the password hash and VPN key material.
 */
public record PlayerSummary(String username, Integer points) {

    public static PlayerSummary from(Player player) {
        return new PlayerSummary(player.getPlayer(), player.getPoints());
    }
}

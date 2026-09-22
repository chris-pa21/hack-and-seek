package Diplo10.dto;

import Diplo10.entity.Team;

import java.util.List;

/**
 * A team as shown in a game. ipAddress is the address of the team's challenge
 * container, or null while the game has not started or once the container is
 * gone.
 */
public record TeamView(
        String name,
        String challenge,
        int score,
        boolean defeated,
        boolean disqualified,
        String ipAddress,
        List<PlayerSummary> members
) {

    public static TeamView from(Team team, String ipAddress) {
        List<PlayerSummary> members = team.getTeam_members() == null
                ? List.of()
                : team.getTeam_members().stream().map(PlayerSummary::from).toList();

        return new TeamView(
                team.getName(),
                team.getChallenge(),
                team.getScore(),
                team.isDefeated(),
                team.isDisqualified(),
                ipAddress,
                members);
    }
}

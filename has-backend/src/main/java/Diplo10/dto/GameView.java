package Diplo10.dto;

import Diplo10.entity.Game;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A game as returned by the API. Players are reduced to PlayerSummary, so no
 * password hash or VPN key can reach a client through a game.
 */
public record GameView(
        Long id,
        String name,
        String createdBy,
        Date createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String winnerTeam,
        List<TeamView> teams
) {

    /**
     * @param ipByTeam container address per team name; teams without an entry
     *                 get a null ipAddress
     */
    public static GameView from(Game game, Map<String, String> ipByTeam) {
        List<TeamView> teams = game.getTeam_list() == null
                ? List.of()
                : game.getTeam_list().stream()
                        .map(team -> TeamView.from(team, ipByTeam.get(team.getName())))
                        .toList();

        return new GameView(
                game.getId(),
                game.getName(),
                game.getCreated_by() != null ? game.getCreated_by().getPlayer() : null,
                game.getCreated_at(),
                game.getStarted_at(),
                game.getFinished_at(),
                game.getWinnerTeam() != null ? game.getWinnerTeam().getName() : null,
                teams);
    }
}

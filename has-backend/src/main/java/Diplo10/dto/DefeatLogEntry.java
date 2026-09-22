package Diplo10.dto;

import java.time.LocalDateTime;

/**
 * A capture as shown in a game's defeat log. Deliberately omits the captured
 * flag itself: no client displays it, and it is the secret the defending team
 * was hiding.
 */
public record DefeatLogEntry(
        Long id,
        String attackingTeam,
        String defeatedTeam,
        int pointsAwarded,
        LocalDateTime submittedAt
) {}

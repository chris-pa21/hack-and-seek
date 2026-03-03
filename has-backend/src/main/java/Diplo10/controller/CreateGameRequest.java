package Diplo10.controller;

import java.util.List;

public record CreateGameRequest(
        String name,
        List<TeamRequest> teams
) {
    public record TeamRequest(
            String name,
            String challenge,
            List<String> players  // list of player usernames
    ) {}
}

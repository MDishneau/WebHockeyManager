package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.GameDate;
import com.hockeymanager.backend.model.ScheduledGame;
import com.hockeymanager.backend.model.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScheduleGenerator {

    private static final int GAMES_PER_TEAM = 82;

    /**
     * Generates an 82-game schedule for each team.
     *
     * Strategy:
     *  - Each team plays every other team at least twice (home + away).
     *  - Remaining games filled by cycling through opponents evenly.
     *  - Games spread across dates starting from seasonStart, ~4 games per week.
     */
    public List<ScheduledGame> generate(List<Team> teams, GameDate seasonStart) {
        List<ScheduledGame> schedule = new ArrayList<>();
        int n = teams.size();

        // Build a pool of matchups (home, away pairs)
        List<int[]> matchups = new ArrayList<>();

        // Every team plays every other team home + away = base 2 games
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) matchups.add(new int[]{i, j});
            }
        }

        // Each team needs 82 games total.
        // Base round (n-1)*2 games per team. Fill remainder by repeating matchups.
        int baseGamesPerTeam = (n - 1) * 2;
        int extraPerTeam     = GAMES_PER_TEAM - baseGamesPerTeam;

        // Track how many extra games each team still needs
        int[] extraNeeded = new int[n];
        for (int i = 0; i < n; i++) extraNeeded[i] = extraPerTeam;

        // Add extra matchups until all teams reach 82
        List<int[]> extraMatchups = new ArrayList<>();
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < n && progress == false; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) continue;
                    if (extraNeeded[i] > 0 && extraNeeded[j] > 0) {
                        extraMatchups.add(new int[]{i, j});
                        extraNeeded[i]--;
                        extraNeeded[j]--;
                        progress = true;
                        break;
                    }
                }
            }
        }

        matchups.addAll(extraMatchups);
        Collections.shuffle(matchups);

        // Assign dates — spread ~4 games across every 2 days
        GameDate current = seasonStart;
        int gamesOnDate = 0;
        int maxPerDay = Math.max(1, n / 2); // at most n/2 games per day

        for (int[] m : matchups) {
            schedule.add(new ScheduledGame(current, teams.get(m[0]), teams.get(m[1])));
            gamesOnDate++;
            if (gamesOnDate >= maxPerDay) {
                current = current.plusDays(2);
                gamesOnDate = 0;
            }
        }

        // Sort by date
        schedule.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return schedule;
    }
}
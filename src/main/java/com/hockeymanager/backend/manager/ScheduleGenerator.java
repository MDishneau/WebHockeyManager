package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.GameDate;
import com.hockeymanager.backend.model.ScheduledGame;
import com.hockeymanager.backend.model.Team;

import java.util.*;

public class ScheduleGenerator {

    private static final int GAMES_PER_TEAM = 82;

    /**
     * Generates an 82-game schedule for each team.
     *
     * Strategy:
     *  - Each team plays every other team at least twice (home + away).
     *  - Remaining games filled by cycling through opponents evenly.
     *  - Games spread across dates starting from seasonStart, ~4 games per week.
     *  - A team may appear on a given date at most once.
     */
    public List<ScheduledGame> generate(List<Team> teams, GameDate seasonStart) {
        List<ScheduledGame> schedule = new ArrayList<>();
        int n = teams.size();

        // ── Build matchup pool ────────────────────────────────────────

        // Every team plays every other team home + away
        List<int[]> matchups = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) matchups.add(new int[]{i, j});
            }
        }

        // Fill each team to 82 games with extra matchups
        int baseGamesPerTeam = (n - 1) * 2;
        int extraPerTeam     = GAMES_PER_TEAM - baseGamesPerTeam;
        int[] extraNeeded    = new int[n];
        Arrays.fill(extraNeeded, extraPerTeam);

        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) continue;
                    if (extraNeeded[i] > 0 && extraNeeded[j] > 0) {
                        matchups.add(new int[]{i, j});
                        extraNeeded[i]--;
                        extraNeeded[j]--;
                        progress = true;
                        break;
                    }
                }
                if (progress) break;
            }
        }

        Collections.shuffle(matchups);

        // ── Assign dates with per-team one-game-per-day enforcement ──

        // Track the last date each team has been scheduled on
        GameDate[] lastGameDate = new GameDate[n];

        // Track how many games are already placed on each date (league-wide cap)
        Map<String, Integer> gamesPerDate = new HashMap<>();
        int maxPerDay = Math.max(1, n / 2);

        GameDate cursor = seasonStart;

        for (int[] m : matchups) {
            int home = m[0];
            int away = m[1];

            // Find the earliest date >= cursor where:
            //   1. Neither team already has a game
            //   2. The date hasn't hit the league-wide daily cap
            GameDate candidate = cursor;
            while (true) {
                String key = candidate.toString();
                int gamesOnDay = gamesPerDate.getOrDefault(key, 0);

                boolean homeConflict = lastGameDate[home] != null
                        && lastGameDate[home].equals(candidate);
                boolean awayConflict = lastGameDate[away] != null
                        && lastGameDate[away].equals(candidate);
                boolean dayFull = gamesOnDay >= maxPerDay;

                if (!homeConflict && !awayConflict && !dayFull) break;
                candidate = candidate.nextDay();
            }

            String key = candidate.toString();
            schedule.add(new ScheduledGame(candidate, teams.get(home), teams.get(away)));
            lastGameDate[home] = candidate;
            lastGameDate[away] = candidate;
            gamesPerDate.merge(key, 1, Integer::sum);

            // Only advance the cursor when the current date is reasonably full
            if (gamesPerDate.getOrDefault(cursor.toString(), 0) >= maxPerDay) {
                cursor = cursor.nextDay();
            }
        }

        schedule.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return schedule;
    }
}
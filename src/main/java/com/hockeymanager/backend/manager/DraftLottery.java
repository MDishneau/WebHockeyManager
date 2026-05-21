package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.Team;

import java.util.*;

/**
 * NHL-style draft lottery.
 * Non-playoff teams are weighted by reverse standings (worst = most chances).
 * Top 3 picks are lotteried; remaining non-playoff teams fill out picks 4-16
 * in reverse standings order. Playoff teams pick 17-32 in reverse standings.
 */
public class DraftLottery {

    private final Random random;

    public DraftLottery(Random random) {
        this.random = random;
    }

    /**
     * @param allTeamsByPoints teams sorted worst-to-best by points
     * @param playoffTeams     the 16 teams that made playoffs
     * @return full 32-team draft order, index 0 = 1st overall pick
     */
    public List<Team> runLottery(List<Team> allTeamsByPoints, Set<Team> playoffTeams) {
        // Split into lottery teams (non-playoff) and playoff teams
        List<Team> lotteryTeams = new ArrayList<>();
        List<Team> postseasonTeams = new ArrayList<>();

        for (Team t : allTeamsByPoints) {
            if (playoffTeams.contains(t)) postseasonTeams.add(t);
            else lotteryTeams.add(t); // already worst-to-best order
        }

        // Build weighted pool — worst team gets most entries
        // Team with worst record gets 16 balls, next gets 15, etc.
        List<Team> pool = new ArrayList<>();
        for (int i = 0; i < lotteryTeams.size(); i++) {
            int balls = lotteryTeams.size() - i; // worst gets most
            for (int b = 0; b < balls; b++) {
                pool.add(lotteryTeams.get(i));
            }
        }

        // Draw top 3 picks from lottery pool (no replacement of team, only ball)
        List<Team> draftOrder = new ArrayList<>();
        Set<Team> alreadyDrawn = new HashSet<>();

        for (int pick = 0; pick < Math.min(3, lotteryTeams.size()); pick++) {
            Team drawn = null;
            while (drawn == null || alreadyDrawn.contains(drawn)) {
                drawn = pool.get(random.nextInt(pool.size()));
            }
            draftOrder.add(drawn);
            alreadyDrawn.add(drawn);
        }

        // Remaining lottery teams in reverse standings order (worst first)
        for (Team t : lotteryTeams) {
            if (!alreadyDrawn.contains(t)) draftOrder.add(t);
        }

        // Playoff teams pick last, worst playoff team first
        Collections.reverse(postseasonTeams); // now worst playoff team first
        draftOrder.addAll(postseasonTeams);

        return draftOrder;
    }
}
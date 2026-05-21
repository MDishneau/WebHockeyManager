package com.hockeymanager.backend.engine;

import com.hockeymanager.backend.model.Player;
import com.hockeymanager.backend.model.Position;
import com.hockeymanager.backend.model.Team;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GameSimulator {

    private static final int BASE_GOALS_PER_GAME = 3; // per team baseline
    private final Random random;

    public GameSimulator() {
        this.random = new Random();
    }

    public GameSimulator(long seed) {
        this.random = new Random(seed); // deterministic for tests
    }

    public GameResult simulate(Team home, Team away) {
        int homeScore = simulateTeamScore(home, away);
        int awayScore = simulateTeamScore(away, home);

        boolean overtime = false;

        // Tied after regulation — go to OT, one random goal decides it
        if (homeScore == awayScore) {
            overtime = true;
            if (random.nextBoolean()) {
                homeScore++;
            } else {
                awayScore++;
            }
        }

        // Award stats to players
        awardGoals(home, homeScore);
        awardGoals(away, awayScore);
        updateGoalieStats(home, away, homeScore, awayScore, overtime);
        updateSkaterGamePlayed(home);
        updateSkaterGamePlayed(away);

        // Update team records
        Team winner = homeScore > awayScore ? home : away;
        Team loser  = homeScore > awayScore ? away : home;

        winner.addWin();
        if (overtime) {
            loser.addOtLoss();
        } else {
            loser.addLoss();
        }

        return new GameResult(home, away, homeScore, awayScore, overtime);
    }

    // Score is influenced by offensive strength vs opponent goaltending
    private int simulateTeamScore(Team attacking, Team defending) {
        double offence  = attacking.getOffensiveStrength();  // 1-100
        double goalie   = defending.getGoaltendingStrength(); // 1-100

        // Ratio of offence vs goalie — 1.0 means evenly matched
        double ratio = offence / goalie;

        // Expected goals roughly 1–6, centered near BASE_GOALS_PER_GAME
        double expected = BASE_GOALS_PER_GAME * ratio;
        expected = Math.max(0.5, Math.min(expected, 8.0));

        // Poisson-like approximation using repeated random trials
        int goals = 0;
        int trials = 20;
        double prob = expected / trials;
        for (int i = 0; i < trials; i++) {
            if (random.nextDouble() < prob) goals++;
        }
        return goals;
    }

    private void awardGoals(Team team, int goalsToAward) {
        List<Player> skaters = team.getRoster().stream()
                .filter(p -> p.getPosition() != Position.GOALIE)
                .collect(Collectors.toList());

        if (skaters.isEmpty()) return;

        for (int i = 0; i < goalsToAward; i++) {
            Player scorer = weightedPickByShotDanger(skaters);
            scorer.addGoal();

            if (skaters.size() > 1) {
                Player assister = weightedPickByOverall(skaters);
                int attempts = 0;
                while (assister == scorer && attempts < 5) {
                    assister = weightedPickByOverall(skaters);
                    attempts++;
                }
                if (assister != scorer) assister.addAssist();
            }
        }
    }

    private Player weightedPickByShotDanger(List<Player> players) {
        int total = players.stream().mapToInt(Player::getEffectiveShotDanger).sum();
        int roll  = random.nextInt(Math.max(total, 1));
        int cum   = 0;
        for (Player p : players) {
            cum += p.getEffectiveShotDanger();
            if (roll < cum) return p;
        }
        return players.get(players.size() - 1);
    }

    private Player weightedPickByOverall(List<Player> players) {
        int total = players.stream().mapToInt(Player::getEffectiveOverall).sum();
        int roll  = random.nextInt(Math.max(total, 1));
        int cum   = 0;
        for (Player p : players) {
            cum += p.getEffectiveOverall();
            if (roll < cum) return p;
        }
        return players.get(players.size() - 1);
    }

    private void updateGoalieStats(Team home, Team away,
                                   int homeScore, int awayScore, boolean overtime) {
        Player homeGoalie = home.getGoalie();
        Player awayGoalie = away.getGoalie();

        if (homeScore > awayScore) {
            if (homeGoalie != null) homeGoalie.addWin();
            if (awayGoalie  != null) awayGoalie.addLoss();
        } else {
            if (awayGoalie  != null) awayGoalie.addWin();
            if (homeGoalie  != null) {
                if (overtime) homeGoalie.addWin(); // OT loss still a win for goalie?
                    // In real NHL the goalie gets an OT loss too — keep it simple for now
                else homeGoalie.addLoss();
            }
        }
    }

    private void updateSkaterGamePlayed(Team team) {
        team.getRoster().stream()
                .filter(p -> p.getPosition() != Position.GOALIE)
                .forEach(Player::addGamePlayed);
    }
}
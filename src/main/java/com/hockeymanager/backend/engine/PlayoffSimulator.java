package com.hockeymanager.backend.engine;

import com.hockeymanager.backend.model.*;

import java.util.Random;

public class PlayoffSimulator {

    private final Random random;

    public PlayoffSimulator() {
        this.random = new Random();
    }

    public PlayoffSimulator(long seed) {
        this.random = new Random(seed);
    }

    // Sim a single playoff game — clutch/mental weighted higher than regular season
    public GameResult simulateGame(Team home, Team away) {
        double homeStrength = getPlayoffStrength(home);
        double awayStrength = getPlayoffStrength(away);

        int homeScore = simulateScore(homeStrength, getPlayoffGoaltending(away));
        int awayScore = simulateScore(awayStrength, getPlayoffGoaltending(home));

        boolean overtime = false;
        if (homeScore == awayScore) {
            overtime = true;
            // Sudden death OT — weighted by team strength
            if (random.nextDouble() < homeStrength / (homeStrength + awayStrength)) {
                homeScore++;
            } else {
                awayScore++;
            }
        }

        // Update goalie stats
        Player homeGoalie = home.getGoalie();
        Player awayGoalie = away.getGoalie();
        if (homeScore > awayScore) {
            if (homeGoalie != null) homeGoalie.addWin();
            if (awayGoalie  != null) awayGoalie.addLoss();
        } else {
            if (awayGoalie  != null) awayGoalie.addWin();
            if (homeGoalie  != null) homeGoalie.addLoss();
        }

        // Update skater games played
        home.getRoster().stream()
                .filter(p -> p.getPosition() != Position.GOALIE)
                .forEach(Player::addGamePlayed);
        away.getRoster().stream()
                .filter(p -> p.getPosition() != Position.GOALIE)
                .forEach(Player::addGamePlayed);

        // Update team records
        if (homeScore > awayScore) { home.addWin(); away.addLoss(); }
        else                       { away.addWin(); home.addLoss(); }

        return new GameResult(home, away, homeScore, awayScore, overtime);
    }

    public void simulateSeries(PlayoffSeries series) {
        while (!series.isOver()) {
            Team home = series.getNextHomeTeam();
            Team away = series.getNextAwayTeam();
            GameResult result = simulateGame(home, away);
            series.addGame(result);
        }
    }

    // Playoff strength weights clutch and mental more heavily
    private double getPlayoffStrength(Team team) {
        return team.getRoster().stream()
                .filter(p -> p.getPosition() != Position.GOALIE)
                .mapToDouble(p -> {
                    double base    = p.getEffectiveOverall();
                    double clutch  = p.getAttributes().getMental().getClutch();
                    double compete = p.getAttributes().getMental().getCompeteLevel();
                    return base * 0.70 + clutch * 0.15 + compete * 0.15;
                })
                .average()
                .orElse(50.0);
    }

    private double getPlayoffGoaltending(Team team) {
        return team.getRoster().stream()
                .filter(p -> p.getPosition() == Position.GOALIE)
                .mapToDouble(p -> {
                    double stopping = p.getEffectiveGoaltending();
                    double composure = p.getAttributes().getMental().getComposure();
                    return stopping * 0.80 + composure * 0.20;
                })
                .average()
                .orElse(50.0);
    }

    private int simulateScore(double offence, double goaltending) {
        double ratio   = offence / Math.max(goaltending, 1);
        double expected = 2.5 * ratio; // playoff games lower scoring
        expected = Math.max(0.5, Math.min(6.0, expected));

        int goals  = 0;
        int trials = 20;
        double prob = expected / trials;
        for (int i = 0; i < trials; i++) {
            if (random.nextDouble() < prob) goals++;
        }
        return goals;
    }
}

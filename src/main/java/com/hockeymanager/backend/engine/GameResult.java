package com.hockeymanager.backend.engine;

import com.hockeymanager.backend.model.Team;

public class GameResult {

    private final Team homeTeam;
    private final Team awayTeam;
    private final int homeScore;
    private final int awayScore;
    private final boolean overtime;

    public GameResult(Team homeTeam, Team awayTeam, int homeScore, int awayScore, boolean overtime) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.overtime = overtime;
    }

    public Team getWinner() {
        return homeScore > awayScore ? homeTeam : awayTeam;
    }

    public Team getLoser() {
        return homeScore > awayScore ? awayTeam : homeTeam;
    }

    public boolean isOvertime()     { return overtime; }
    public Team getHomeTeam()       { return homeTeam; }
    public Team getAwayTeam()       { return awayTeam; }
    public int getHomeScore()       { return homeScore; }
    public int getAwayScore()       { return awayScore; }

    @Override
    public String toString() {
        String otLabel = overtime ? " (OT)" : "";
        return String.format("%s %d - %d %s%s",
                homeTeam.getFullName(), homeScore,
                awayScore, awayTeam.getFullName(), otLabel);
    }
}
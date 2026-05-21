package com.hockeymanager.backend.model;

import com.hockeymanager.backend.engine.GameResult;

public class ScheduledGame {

    private final GameDate date;
    private final Team homeTeam;
    private final Team awayTeam;
    private GameResult result;

    public ScheduledGame(GameDate date, Team homeTeam, Team awayTeam) {
        this.date     = date;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    public boolean isPlayed() {
        return result != null;
    }

    public void setResult(GameResult result) {
        this.result = result;
    }

    public GameDate getDate()     { return date; }
    public Team getHomeTeam()     { return homeTeam; }
    public Team getAwayTeam()     { return awayTeam; }
    public GameResult getResult() { return result; }

    @Override
    public String toString() {
        if (isPlayed()) {
            return date + "  " + result;
        }
        return date + "  " + homeTeam.getFullName() + " vs " + awayTeam.getFullName();
    }
}
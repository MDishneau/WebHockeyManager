package com.hockeymanager.backend.model;

import com.hockeymanager.backend.engine.GameResult;
import java.util.ArrayList;
import java.util.List;

public class PlayoffSeries {

    // 2-2-1-1-1 home/away pattern (true = higher seed is home)
    private static final boolean[] HOME_PATTERN = {true, true, false, false, true, false, true};

    private final Team higherSeed;
    private final Team lowerSeed;
    private final int round;
    private final List<GameResult> games;
    private int higherSeedWins;
    private int lowerSeedWins;

    public PlayoffSeries(Team higherSeed, Team lowerSeed, int round) {
        this.higherSeed = higherSeed;
        this.lowerSeed  = lowerSeed;
        this.round      = round;
        this.games      = new ArrayList<>();
    }

    public void addGame(GameResult result) {
        games.add(result);
        if (result.getWinner() == higherSeed) higherSeedWins++;
        else lowerSeedWins++;
    }

    public boolean isOver() {
        return higherSeedWins == 4 || lowerSeedWins == 4;
    }

    public Team getLeader() {
        return higherSeedWins >= lowerSeedWins ? higherSeed : lowerSeed;
    }

    public Team getWinner() {
        if (!isOver()) return null;
        return higherSeedWins == 4 ? higherSeed : lowerSeed;
    }

    public Team getLoser() {
        if (!isOver()) return null;
        return higherSeedWins == 4 ? lowerSeed : higherSeed;
    }

    // Home team for the next game based on 2-2-1-1-1 pattern
    public Team getNextHomeTeam() {
        int gameIndex = games.size();
        if (gameIndex >= HOME_PATTERN.length) return higherSeed;
        return HOME_PATTERN[gameIndex] ? higherSeed : lowerSeed;
    }

    public Team getNextAwayTeam() {
        return getNextHomeTeam() == higherSeed ? lowerSeed : higherSeed;
    }

    public int getGameCount()       { return games.size(); }
    public int getHigherSeedWins()  { return higherSeedWins; }
    public int getLowerSeedWins()   { return lowerSeedWins; }
    public Team getHigherSeed()     { return higherSeed; }
    public Team getLowerSeed()      { return lowerSeed; }
    public List<GameResult> getGames() { return games; }
    public int getRound()           { return round; }

    public String getRoundName() {
        return switch (round) {
            case 1 -> "First Round";
            case 2 -> "Second Round";
            case 3 -> "Conference Finals";
            case 4 -> "Stanley Cup Final";
            default -> "Round " + round;
        };
    }

    @Override
    public String toString() {
        if (isOver()) {
            return String.format("%s def. %s %d-%d",
                    getWinner().getFullName(), getLoser().getFullName(),
                    Math.max(higherSeedWins, lowerSeedWins),
                    Math.min(higherSeedWins, lowerSeedWins));
        }
        return String.format("%s %d - %d %s",
                higherSeed.getFullName(), higherSeedWins,
                lowerSeedWins, lowerSeed.getFullName());
    }
}
package com.hockeymanager.backend.model;

import java.util.ArrayList;
import java.util.List;

public class Team {

    private final String city;
    private final String name;
    private final List<Player> roster;
    private final MarketSize marketSize;

    // Season record
    private int wins;
    private int losses;
    private int otLosses;

    public Team(String city, String name, MarketSize marketSize) {
        this.city       = city;
        this.name       = name;
        this.marketSize = marketSize;
        this.roster     = new ArrayList<>();
    }

    // Keep the old constructor for tests so nothing breaks
    public Team(String city, String name) {
        this(city, name, MarketSize.MEDIUM);
    }

    public void addPlayer(Player player) {
        roster.add(player);
    }

    public MarketSize getMarketSize() { return marketSize; }

    // Points in standings (2 for win, 1 for OT loss, 0 for loss)
    public int getPoints() {
        return (wins * 2) + otLosses;
    }

    public double getOffensiveStrength() {
        return roster.stream()
                .filter(p -> p.getPosition() != Position.GOALIE)
                .mapToInt(Player::getEffectiveOverall)
                .average()
                .orElse(50.0);
    }

    public double getGoaltendingStrength() {
        return roster.stream()
                .filter(p -> p.getPosition() == Position.GOALIE)
                .mapToInt(Player::getEffectiveGoaltending)
                .average()
                .orElse(50.0);
    }

    public Player getGoalie() {
        return roster.stream()
                .filter(p -> p.getPosition() == Position.GOALIE)
                .findFirst()
                .orElse(null);
    }

    public void resetRecord() {
        wins     = 0;
        losses   = 0;
        otLosses = 0;
    }

    // Record mutators
    public void addWin()      { wins++; }
    public void addLoss()     { losses++; }
    public void addOtLoss()   { otLosses++; }

    // Getters
    public String getCity()         { return city; }
    public String getName()         { return name; }
    public String getFullName()     { return city + " " + name; }
    public List<Player> getRoster() { return roster; }
    public int getWins()            { return wins; }
    public int getLosses()          { return losses; }
    public int getOtLosses()        { return otLosses; }
    public int getGamesPlayed()     { return wins + losses + otLosses; }

    @Override
    public String toString() {
        return String.format("%-25s W:%-3d L:%-3d OTL:%-3d PTS:%d",
                getFullName(), wins, losses, otLosses, getPoints());
    }
}
package com.hockeymanager.backend.model;

import com.hockeymanager.backend.model.attributes.PlayerAttributes;
import com.hockeymanager.backend.model.personality.PlayerPersonality;

public class Player {

    // Identity
    private final String name;
    private final Position position;
    private int age;

    // Attributes & personality
    private final PlayerAttributes  attributes;
    private final PlayerPersonality personality;
    private GrowthArchetype growthArchetype;
    private Contract contract;
    private int      serviceYears;
    private boolean  released;
    // Cached overall (recalculate if attributes ever change)
    private int overall;

    // Season stats — skaters
    private int goals;
    private int assists;
    private int gamesPlayed;
    private int plusMinus;
    private int hits;
    private int blockedShots;
    private int penaltyMinutes;

    // Season stats — goalies
    private int wins;
    private int losses;
    private int otLosses;
    private int shotsAgainst;
    private int goalsAgainst;

    public Player(String name, Position position, int age,
                  PlayerAttributes attributes, PlayerPersonality personality) {
        this.name        = name;
        this.position    = position;
        this.age         = age;
        this.attributes  = attributes;
        this.personality = personality;
        this.overall     = PlayerOverallCalculator.calculate(position, attributes);
    }

    // --- Effective rating: overall adjusted by morale ---
    public int getEffectiveOverall() {
        return (int) Math.round(overall * personality.getMoraleMultiplier());
    }

    // Convenience passthrough attributes used by GameSimulator
    public int getEffectiveShotDanger() {
        return (int) Math.round(
                attributes.getShooting().getShotDanger() * personality.getMoraleMultiplier());
    }

    public int getEffectiveGoaltending() {
        if (!attributes.isGoalie()) return 0;
        return (int) Math.round(
                attributes.getGoalie().getStoppingPower() * personality.getMoraleMultiplier());
    }

    public int getEffectiveDefense() {
        return (int) Math.round(
                attributes.getDefense().getAverage() * personality.getMoraleMultiplier());
    }

    public int getEffectiveSkating() {
        return (int) Math.round(
                attributes.getSkating().getAverage() * personality.getMoraleMultiplier());
    }

    // --- Stat mutators ---
    public void addGoal()            { goals++;           gamesPlayed++; }
    public void addAssist()          { assists++; }
    public void addGamePlayed()      { gamesPlayed++; }
    public void addHit()             { hits++; }
    public void addBlockedShot()     { blockedShots++; }
    public void addPenaltyMinutes(int pim) { penaltyMinutes += pim; }
    public void addPlusMinus(int v)  { plusMinus += v; }

    public void addWin()    { wins++;    gamesPlayed++; }
    public void addLoss()   { losses++;  gamesPlayed++; }
    public void addOtLoss() { otLosses++; gamesPlayed++; }
    public void addShotAgainst()       { shotsAgainst++; }
    public void addGoalAgainst()       { goalsAgainst++; shotsAgainst++; }

    // --- Getters ---
    public String            getName()        { return name; }
    public Position          getPosition()    { return position; }
    public int               getAge()         { return age; }
    public PlayerAttributes  getAttributes()  { return attributes; }
    public PlayerPersonality getPersonality() { return personality; }
    public int               getOverall()     { return overall; }

    public int getGoals()          { return goals; }
    public int getAssists()        { return assists; }
    public int getPoints()         { return goals + assists; }
    public int getGamesPlayed()    { return gamesPlayed; }
    public int getPlusMinus()      { return plusMinus; }
    public int getHits()           { return hits; }
    public int getBlockedShots()   { return blockedShots; }
    public int getPenaltyMinutes() { return penaltyMinutes; }

    public int getWins()           { return wins; }
    public int getLosses()         { return losses; }
    public int getOtLosses()       { return otLosses; }
    public int getShotsAgainst()   { return shotsAgainst; }
    public int getGoalsAgainst()   { return goalsAgainst; }
    public double getSavePercentage() {
        if (shotsAgainst == 0) return 0.000;
        return (shotsAgainst - goalsAgainst) / (double) shotsAgainst;
    }

    public double getGAA() {
        if (gamesPlayed == 0) return 0.00;
        return (goalsAgainst / (double) gamesPlayed);
    }

    public void incrementAge() {
        age++;
    }


    public Contract getContract()                    { return contract; }
    public void     setContract(Contract contract)   { this.contract = contract; }
    public int      getServiceYears()                { return serviceYears; }
    public boolean  isReleased()                     { return released; }
    public void     setReleased(boolean released)    { this.released = released; }

    public void advanceContract() {
        if (contract != null) contract.advanceYear();
    }

// Add to constructor body — leave as null by default

    // Add methods:
    public GrowthArchetype getGrowthArchetype()              { return growthArchetype; }
    public void setGrowthArchetype(GrowthArchetype archetype){ this.growthArchetype = archetype; }
    public void setOverall(int overall)                      { this.overall = Math.max(1, Math.min(99, overall)); }

    public void resetSeasonStats() {
        goals         = 0;
        assists       = 0;
        gamesPlayed   = 0;
        plusMinus     = 0;
        hits          = 0;
        blockedShots  = 0;
        penaltyMinutes = 0;
        wins          = 0;
        losses        = 0;
        otLosses      = 0;
        shotsAgainst  = 0;
        goalsAgainst  = 0;
    }

    @Override
    public String toString() {
        if (position == Position.GOALIE) {
            return String.format("%-22s %-14s OVR:%-3d  W:%-3d L:%-3d GAA:%.2f SV%%:%.3f",
                    name, position, overall, wins, losses, getGAA(), getSavePercentage());
        }
        return String.format("%-22s %-14s OVR:%-3d  G:%-3d A:%-3d PTS:%-3d +/-:%-3d",
                name, position, overall, goals, assists, getPoints(), plusMinus);
    }
}
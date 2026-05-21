package com.hockeymanager.backend.model.attributes;

public class MentalAttributes {
    private final int composure;
    private final int competeLevel;
    private final int leadership;
    private final int hockeyIQ;
    private final int clutch;

    public MentalAttributes(int composure, int competeLevel, int leadership, int hockeyIQ, int clutch) {
        this.composure    = composure;
        this.competeLevel = competeLevel;
        this.leadership   = leadership;
        this.hockeyIQ     = hockeyIQ;
        this.clutch       = clutch;
    }

    public int getComposure()    { return composure; }
    public int getCompeteLevel() { return competeLevel; }
    public int getLeadership()   { return leadership; }
    public int getHockeyIQ()     { return hockeyIQ; }
    public int getClutch()       { return clutch; }

    public int getAverage() {
        return (composure + competeLevel + leadership + hockeyIQ + clutch) / 5;
    }
}
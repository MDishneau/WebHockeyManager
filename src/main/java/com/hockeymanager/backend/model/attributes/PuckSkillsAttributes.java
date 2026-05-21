package com.hockeymanager.backend.model.attributes;

public class PuckSkillsAttributes {
    private final int handling;
    private final int deking;
    private final int boardBattles;
    private final int puckProtection;

    public PuckSkillsAttributes(int handling, int deking, int boardBattles, int puckProtection) {
        this.handling       = handling;
        this.deking         = deking;
        this.boardBattles   = boardBattles;
        this.puckProtection = puckProtection;
    }

    public int getHandling()       { return handling; }
    public int getDeking()         { return deking; }
    public int getBoardBattles()   { return boardBattles; }
    public int getPuckProtection() { return puckProtection; }

    public int getAverage() {
        return (handling + deking + boardBattles + puckProtection) / 4;
    }
}

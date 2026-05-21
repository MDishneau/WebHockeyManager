package com.hockeymanager.backend.model.attributes;

public class DefenseAttributes {
    private final int positioning;
    private final int stickChecking;
    private final int bodyChecking;
    private final int shotBlocking;

    public DefenseAttributes(int positioning, int stickChecking, int bodyChecking, int shotBlocking) {
        this.positioning   = positioning;
        this.stickChecking = stickChecking;
        this.bodyChecking  = bodyChecking;
        this.shotBlocking  = shotBlocking;
    }

    public int getPositioning()   { return positioning; }
    public int getStickChecking() { return stickChecking; }
    public int getBodyChecking()  { return bodyChecking; }
    public int getShotBlocking()  { return shotBlocking; }

    public int getAverage() {
        return (positioning + stickChecking + bodyChecking + shotBlocking) / 4;
    }
}

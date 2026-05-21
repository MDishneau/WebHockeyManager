package com.hockeymanager.backend.model.attributes;

public class ShootingAttributes {
    private final int power;
    private final int accuracy;
    private final int oneTimer;
    private final int wristShot;
    private final int slapShot;

    public ShootingAttributes(int power, int accuracy, int oneTimer, int wristShot, int slapShot) {
        this.power     = power;
        this.accuracy  = accuracy;
        this.oneTimer  = oneTimer;
        this.wristShot = wristShot;
        this.slapShot  = slapShot;
    }

    public int getPower()     { return power; }
    public int getAccuracy()  { return accuracy; }
    public int getOneTimer()  { return oneTimer; }
    public int getWristShot() { return wristShot; }
    public int getSlapShot()  { return slapShot; }

    public int getAverage() {
        return (power + accuracy + oneTimer + wristShot + slapShot) / 5;
    }

    // Composite: how dangerous is this player's shot overall
    public int getShotDanger() {
        return (int) (accuracy * 0.35 + power * 0.25 + wristShot * 0.20
                + oneTimer * 0.10 + slapShot * 0.10);
    }
}
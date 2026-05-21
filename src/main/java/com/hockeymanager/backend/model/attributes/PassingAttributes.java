package com.hockeymanager.backend.model.attributes;

public class PassingAttributes {
    private final int accuracy;
    private final int vision;
    private final int saucerPass;
    private final int oneTouch;

    public PassingAttributes(int accuracy, int vision, int saucerPass, int oneTouch) {
        this.accuracy   = accuracy;
        this.vision     = vision;
        this.saucerPass = saucerPass;
        this.oneTouch   = oneTouch;
    }

    public int getAccuracy()   { return accuracy; }
    public int getVision()     { return vision; }
    public int getSaucerPass() { return saucerPass; }
    public int getOneTouch()   { return oneTouch; }

    public int getAverage() {
        return (accuracy + vision + saucerPass + oneTouch) / 4;
    }
}
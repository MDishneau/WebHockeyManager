package com.hockeymanager.backend.model.attributes;

public class SkatingAttributes {
    private final int speed;
    private final int acceleration;
    private final int agility;
    private final int stamina;

    public SkatingAttributes(int speed, int acceleration, int agility, int stamina) {
        this.speed        = speed;
        this.acceleration = acceleration;
        this.agility      = agility;
        this.stamina      = stamina;
    }

    public int getSpeed()        { return speed; }
    public int getAcceleration() { return acceleration; }
    public int getAgility()      { return agility; }
    public int getStamina()      { return stamina; }

    public int getAverage() {
        return (speed + acceleration + agility + stamina) / 4;
    }
}
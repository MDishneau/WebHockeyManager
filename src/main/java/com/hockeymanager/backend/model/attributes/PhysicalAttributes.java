package com.hockeymanager.backend.model.attributes;

public class PhysicalAttributes {
    private final int strength;
    private final int aggression;
    private final int balance;
    private final int fighting;

    public PhysicalAttributes(int strength, int aggression, int balance, int fighting) {
        this.strength   = strength;
        this.aggression = aggression;
        this.balance    = balance;
        this.fighting   = fighting;
    }

    public int getStrength()   { return strength; }
    public int getAggression() { return aggression; }
    public int getBalance()    { return balance; }
    public int getFighting()   { return fighting; }

    public int getAverage() {
        return (strength + aggression + balance + fighting) / 4;
    }
}

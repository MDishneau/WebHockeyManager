package com.hockeymanager.backend.model;

public enum ScoutingRegion {
    NORTH_AMERICA  ("North America"),
    EUROPE         ("Europe"),
    RUSSIA         ("Russia"),
    SCANDINAVIA    ("Scandinavia"),
    INTERNATIONAL  ("International");

    private final String displayName;

    ScoutingRegion(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
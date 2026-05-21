package com.hockeymanager.backend.model;

/**
 * Defines a player's development ceiling and trajectory.
 * Skater archetypes and goalie archetypes are separate.
 *
 * peakOverall: the overall rating this player can reach at their best
 * developmentSpeed: how fast they climb (1.0 = normal, 1.3 = fast, 0.7 = slow)
 */
public enum GrowthArchetype {

    // --- Skater archetypes ---
    GENERATIONAL  ("Generational",   96, 1.4, false),
    FRANCHISE     ("Franchise",      90, 1.3, false),
    ELITE         ("Elite",          85, 1.2, false),
    TOP_SIX       ("Top-6",          80, 1.1, false),
    TOP_NINE      ("Top-9",          75, 1.0, false),
    BOTTOM_SIX    ("Bottom-6",       70, 0.9, false),
    DEPTH_FORWARD ("Depth Forward",  65, 0.8, false),

    // --- Defense archetypes ---
    TOP_PAIR      ("Top Pair",       84, 1.2, false),
    TOP_FOUR      ("Top-4",          79, 1.1, false),
    DEPTH_DEFENSE ("Depth Defense",  68, 0.9, false),

    // --- Goalie archetypes ---
    STARTER       ("Starter",        85, 1.2, true),
    BACKUP        ("Backup",         74, 1.0, true),
    DEPTH_GOALIE  ("Depth Goalie",   65, 0.8, true);

    private final String displayName;
    private final int    peakOverall;
    private final double developmentSpeed;
    private final boolean isGoalie;

    GrowthArchetype(String displayName, int peakOverall,
                    double developmentSpeed, boolean isGoalie) {
        this.displayName      = displayName;
        this.peakOverall      = peakOverall;
        this.developmentSpeed = developmentSpeed;
        this.isGoalie         = isGoalie;
    }

    public String getDisplayName()     { return displayName; }
    public int    getPeakOverall()     { return peakOverall; }
    public double getDevelopmentSpeed(){ return developmentSpeed; }
    public boolean isGoalie()          { return isGoalie; }

    /** Letter grade shown to user (hidden until scouted) */
    public String getPotentialGrade() {
        return switch (this) {
            case GENERATIONAL  -> "A+";
            case FRANCHISE     -> "A";
            case ELITE         -> "A-";
            case TOP_PAIR,
                 TOP_SIX       -> "B+";
            case TOP_FOUR,
                 TOP_NINE,
                 STARTER       -> "B";
            case BOTTOM_SIX,
                 DEPTH_DEFENSE,
                 BACKUP        -> "C+";
            case DEPTH_FORWARD,
                 DEPTH_GOALIE  -> "C";
        };
    }
}
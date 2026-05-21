package com.hockeymanager.backend.model;

import com.hockeymanager.backend.model.attributes.PlayerAttributes;
import com.hockeymanager.backend.model.personality.PlayerPersonality;

/**
 * A draft-eligible prospect. Wraps a Player with additional
 * draft-specific fields. Archetype and potential are hidden
 * until enough scouting points have been invested.
 */
public class Prospect {

    private static final int POINTS_TO_REVEAL_POTENTIAL  = 30;
    private static final int POINTS_TO_REVEAL_ARCHETYPE  = 60;

    private final Player         player;
    private final GrowthArchetype growthArchetype;
    private final ScoutingRegion  region;
    private int                   scoutingPoints; // 0-100 per team (we track user's team)
    private int                   draftRank;      // set after class is generated

    public Prospect(Player player, GrowthArchetype growthArchetype, ScoutingRegion region) {
        this.player          = player;
        this.growthArchetype = growthArchetype;
        this.region          = region;
        this.scoutingPoints  = 0;
    }

    // --- Scouting reveal logic ---

    public void addScoutingPoints(int points) {
        scoutingPoints = Math.min(100, scoutingPoints + points);
    }

    public boolean isPotentialRevealed()  { return scoutingPoints >= POINTS_TO_REVEAL_POTENTIAL; }
    public boolean isArchetypeRevealed()  { return scoutingPoints >= POINTS_TO_REVEAL_ARCHETYPE; }

    public String getPotentialGrade() {
        return isPotentialRevealed() ? growthArchetype.getPotentialGrade() : "?";
    }

    public String getArchetypeDisplay() {
        return isArchetypeRevealed() ? growthArchetype.getDisplayName() : "Unknown";
    }

    // --- Passthrough helpers ---
    public String   getName()           { return player.getName(); }
    public Position getPosition()       { return player.getPosition(); }
    public int      getAge()            { return player.getAge(); }
    public int      getOverall()        { return player.getOverall(); }
    public Player   getPlayer()         { return player; }

    public GrowthArchetype getGrowthArchetype() { return growthArchetype; }
    public ScoutingRegion  getRegion()           { return region; }
    public int             getScoutingPoints()   { return scoutingPoints; }
    public int             getDraftRank()        { return draftRank; }
    public void            setDraftRank(int r)   { draftRank = r; }

    @Override
    public String toString() {
        return String.format("%-22s %-14s Age:%-3d OVR:%-3d  POT:%-3s  Archetype:%-18s Region:%s",
                getName(), getPosition(), getAge(), getOverall(),
                getPotentialGrade(), getArchetypeDisplay(), region.getDisplayName());
    }
}
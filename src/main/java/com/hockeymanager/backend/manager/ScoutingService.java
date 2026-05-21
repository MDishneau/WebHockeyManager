package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.*;

import java.util.*;

/**
 * Manages scouting budget allocation and prospect reveal logic.
 *
 * Each team gets BUDGET_PER_SEASON scouting points.
 * Points are allocated to regions. At end of regular season,
 * prospects from those regions gain scouting points proportional
 * to the investment, revealing potential and archetype progressively.
 */
public class ScoutingService {

    public static final int BUDGET_PER_SEASON = 1000;

    // region -> points allocated by user's team
    private final Map<ScoutingRegion, Integer> regionAllocation;
    private int remainingBudget;

    public ScoutingService() {
        this.regionAllocation = new EnumMap<>(ScoutingRegion.class);
        for (ScoutingRegion r : ScoutingRegion.values()) {
            regionAllocation.put(r, 0);
        }
        this.remainingBudget = BUDGET_PER_SEASON;
    }

    public boolean allocate(ScoutingRegion region, int points) {
        if (points > remainingBudget) return false;
        regionAllocation.merge(region, points, Integer::sum);
        remainingBudget -= points;
        return true;
    }

    public void resetBudget() {
        remainingBudget = BUDGET_PER_SEASON;
        regionAllocation.replaceAll((r, v) -> 0);
    }

    /**
     * Apply scouting to all prospects in draft class.
     * Points per prospect = region allocation / number of prospects in that region.
     * CPU teams get baseline scouting (50% of max) automatically.
     */
    public void applyScoutingToClass(List<Prospect> draftClass) {
        // Count prospects per region
        Map<ScoutingRegion, List<Prospect>> byRegion = new EnumMap<>(ScoutingRegion.class);
        for (Prospect p : draftClass) {
            byRegion.computeIfAbsent(p.getRegion(), k -> new ArrayList<>()).add(p);
        }

        for (ScoutingRegion region : ScoutingRegion.values()) {
            List<Prospect> regional = byRegion.getOrDefault(region, List.of());
            if (regional.isEmpty()) continue;

            int allocated = regionAllocation.getOrDefault(region, 0);
            if (allocated == 0) continue;

            // Distribute points evenly across prospects in region
            int pointsEach = Math.max(1, allocated / regional.size());
            for (Prospect p : regional) {
                p.addScoutingPoints(pointsEach);
            }
        }
    }

    public int getRemainingBudget()                         { return remainingBudget; }
    public Map<ScoutingRegion, Integer> getAllocation()     { return Collections.unmodifiableMap(regionAllocation); }
    public int getAllocationFor(ScoutingRegion r)           { return regionAllocation.getOrDefault(r, 0); }
}

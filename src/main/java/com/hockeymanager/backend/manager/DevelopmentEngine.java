package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.*;


import java.util.Random;

/**
 * End-of-season player development.
 *
 * Players under 26 develop toward their archetype ceiling.
 * Players 27-30 plateau with small random swings.
 * Players 31+ decline gradually.
 *
 * Only applies to players who have a GrowthArchetype assigned.
 * Drafted prospects get their archetype set when added to rosters.
 */
public class DevelopmentEngine {

    private final Random random;

    public DevelopmentEngine(Random random) {
        this.random = random;
    }

    public void processLeagueDevelopment(League league) {
        for (Team team : league.getTeams()) {
            for (Player player : team.getRoster()) {
                if (player.getGrowthArchetype() != null) {
                    develop(player);
                }
            }
        }
    }

    private void develop(Player player) {
        int age     = player.getAge();
        int current = player.getOverall();
        int peak    = player.getGrowthArchetype().getPeakOverall();
        double speed = player.getGrowthArchetype().getDevelopmentSpeed();

        int delta;

        if (age <= 22) {
            // Fast growth phase
            int gap = peak - current;
            delta = (int) Math.round(gap * 0.18 * speed) + random.nextInt(3);
        } else if (age <= 25) {
            // Normal growth
            int gap = peak - current;
            delta = (int) Math.round(gap * 0.10 * speed) + random.nextInt(2);
        } else if (age <= 29) {
            // Plateau — small random swing
            delta = random.nextInt(3) - 1; // -1, 0, or 1
        } else if (age <= 32) {
            // Early decline
            delta = -(random.nextInt(3)); // 0 to -2
        } else {
            // Steep decline
            delta = -(1 + random.nextInt(3)); // -1 to -3
        }

        // Never exceed peak, never go below 40
        int newOverall = Math.max(40, Math.min(peak, current + delta));
        player.setOverall(newOverall);
    }
}
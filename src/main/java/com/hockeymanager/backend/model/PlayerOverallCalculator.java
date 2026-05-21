package com.hockeymanager.backend.model;

import com.hockeymanager.backend.model.attributes.*;

/**
 * Derives a single 1-100 overall rating from full attributes,
 * weighted by position. Mirrors how EA NHL weights matter differently
 * per role — a enforcer's fighting matters more than a sniper's.
 */
public class PlayerOverallCalculator {

    public static int calculate(Position position, PlayerAttributes a) {
        return switch (position) {
            case CENTER       -> calcCenter(a);
            case LEFT_WING,
                 RIGHT_WING   -> calcWing(a);
            case LEFT_DEFENSE,
                 RIGHT_DEFENSE -> calcDefenseman(a);
            case GOALIE       -> calcGoalie(a);
        };
    }

    private static int calcCenter(PlayerAttributes a) {
        return weighted(
                w(a.getSkating().getAverage(),       0.18),
                w(a.getShooting().getShotDanger(),   0.17),
                w(a.getPassing().getAverage(),        0.18),
                w(a.getPuckSkills().getAverage(),     0.15),
                w(a.getDefense().getAverage(),        0.12),
                w(a.getMental().getAverage(),         0.12),
                w(a.getPhysical().getAverage(),       0.08)
        );
    }

    private static int calcWing(PlayerAttributes a) {
        return weighted(
                w(a.getSkating().getAverage(),       0.20),
                w(a.getShooting().getShotDanger(),   0.25),
                w(a.getPuckSkills().getAverage(),    0.18),
                w(a.getPassing().getAverage(),       0.12),
                w(a.getMental().getAverage(),        0.10),
                w(a.getDefense().getAverage(),       0.08),
                w(a.getPhysical().getAverage(),      0.07)
        );
    }

    private static int calcDefenseman(PlayerAttributes a) {
        return weighted(
                w(a.getDefense().getAverage(),       0.25),
                w(a.getSkating().getAverage(),       0.18),
                w(a.getPhysical().getAverage(),      0.15),
                w(a.getPassing().getAverage(),       0.15),
                w(a.getMental().getAverage(),        0.12),
                w(a.getPuckSkills().getAverage(),    0.08),
                w(a.getShooting().getShotDanger(),   0.07)
        );
    }

    private static int calcGoalie(PlayerAttributes a) {
        if (!a.isGoalie()) return 50;
        return weighted(
                w(a.getGoalie().getStoppingPower(),  0.50),
                w(a.getGoalie().getAverage(),        0.30),
                w(a.getMental().getAverage(),        0.15),
                w(a.getSkating().getAgility(),       0.05)
        );
    }

    @SafeVarargs
    private static int weighted(double... terms) {
        double sum = 0;
        for (double t : terms) sum += t;
        return (int) Math.round(Math.max(1, Math.min(99, sum)));
    }

    private static double w(int value, double weight) {
        return value * weight;
    }
}
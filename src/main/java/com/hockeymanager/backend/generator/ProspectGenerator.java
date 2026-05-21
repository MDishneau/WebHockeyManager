package com.hockeymanager.backend.generator;

import com.hockeymanager.backend.model.*;
import com.hockeymanager.backend.model.attributes.*;
import com.hockeymanager.backend.model.personality.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ProspectGenerator {

    private static final int PROSPECTS_PER_DRAFT = 224; // 7 rounds x 32 teams

    private final Random          random;
    private final PlayerGenerator playerGenerator;

    public ProspectGenerator(Random random) {
        this.random          = random;
        this.playerGenerator = new PlayerGenerator(random);
    }

    public List<Prospect> generateDraftClass() {
        List<Prospect> prospects = new ArrayList<>();

        // Position distribution mirroring a real draft class
        int[] positionCounts = {
                55,  // CENTER
                40,  // LEFT_WING
                40,  // RIGHT_WING
                30,  // LEFT_DEFENSE
                30,  // RIGHT_DEFENSE
                29   // GOALIE
        };
        // Total = 224
        Position[] positions = Position.values();

        for (int pi = 0; pi < positions.length; pi++) {
            Position pos = positions[pi];
            for (int i = 0; i < positionCounts[pi]; i++) {
                GrowthArchetype archetype = rollArchetype(pos);
                ScoutingRegion  region    = rollRegion();
                int             age       = 18 + random.nextInt(3); // 18-20

                Player player = generateProspect(pos, age, archetype);
                prospects.add(new Prospect(player, archetype, region));
            }
        }

        // Sort by overall descending, assign draft ranks
        prospects.sort(Comparator.comparingInt(Prospect::getOverall).reversed());
        for (int i = 0; i < prospects.size(); i++) {
            prospects.get(i).setDraftRank(i + 1);
        }

        return prospects;
    }

    /**
     * Prospects are generated weaker than NHL players — their archetype
     * ceiling is where they'll end up after development.
     * Raw ratings are roughly 50-70% of their eventual peak.
     */
    private Player generateProspect(Position pos, int age, GrowthArchetype archetype) {
        int peak = archetype.getPeakOverall();
        // Raw starting overall roughly 55-75% of peak depending on age
        double rawFactor = 0.55 + (age - 18) * 0.05 + random.nextDouble() * 0.10;
        int targetRaw = (int) (peak * rawFactor);

        // Clamp to realistic prospect range
        targetRaw = Math.max(40, Math.min(74, targetRaw));

        // Generate with tighter attribute bands centered on targetRaw
        int lo = Math.max(35, targetRaw - 10);
        int hi = Math.min(80, targetRaw + 10);

        PlayerAttributes attrs = generateProspectAttributes(pos, lo, hi);
        PlayerPersonality personality = generateProspectPersonality(age);

        return new Player(
                new NameGenerator(random).generateName(),
                pos, age, attrs, personality
        );
    }

    private PlayerAttributes generateProspectAttributes(Position pos, int lo, int hi) {
        return switch (pos) {
            case CENTER -> new PlayerAttributes(
                    new SkatingAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi)),
                    new ShootingAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi)),
                    new PuckSkillsAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi)),
                    new PassingAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi)),
                    new DefenseAttributes(r(lo-5,hi-5), r(lo-5,hi-5), r(lo-5,hi-5), r(lo-5,hi-5)),
                    new PhysicalAttributes(r(lo-5,hi-5), r(lo-5,hi-5), r(lo,hi), r(lo-5,hi-5)),
                    new MentalAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi))
            );
            case LEFT_WING, RIGHT_WING -> new PlayerAttributes(
                    new SkatingAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi)),
                    new ShootingAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi)),
                    new PuckSkillsAttributes(r(lo,hi), r(lo,hi), r(lo-5,hi-5), r(lo,hi)),
                    new PassingAttributes(r(lo-5,hi-5), r(lo,hi), r(lo-5,hi-5), r(lo-5,hi-5)),
                    new DefenseAttributes(r(lo-8,hi-8), r(lo-5,hi-5), r(lo-5,hi-5), r(lo-5,hi-5)),
                    new PhysicalAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo-5,hi-5)),
                    new MentalAttributes(r(lo,hi), r(lo,hi), r(lo-5,hi-5), r(lo,hi), r(lo,hi))
            );
            case LEFT_DEFENSE, RIGHT_DEFENSE -> new PlayerAttributes(
                    new SkatingAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi)),
                    new ShootingAttributes(r(lo-5,hi-5), r(lo-5,hi-5), r(lo-5,hi-5), r(lo-5,hi-5), r(lo-5,hi-5)),
                    new PuckSkillsAttributes(r(lo-5,hi-5), r(lo-5,hi-5), r(lo,hi), r(lo,hi)),
                    new PassingAttributes(r(lo,hi), r(lo,hi), r(lo-5,hi-5), r(lo-5,hi-5)),
                    new DefenseAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi)),
                    new PhysicalAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo-5,hi-5)),
                    new MentalAttributes(r(lo,hi), r(lo,hi), r(lo-5,hi-5), r(lo,hi), r(lo,hi))
            );
            case GOALIE -> new PlayerAttributes(
                    new SkatingAttributes(r(lo-5,hi-5), r(lo-5,hi-5), r(lo,hi), r(lo-5,hi-5)),
                    new ShootingAttributes(35, 35, 35, 35, 35),
                    new PuckSkillsAttributes(r(lo-5,hi-5), r(lo-10,hi-10), r(lo-5,hi-5), r(lo-5,hi-5)),
                    new PassingAttributes(r(lo-5,hi-5), r(lo-5,hi-5), r(lo-10,hi-10), r(lo-10,hi-10)),
                    new DefenseAttributes(r(lo-5,hi-5), r(lo-8,hi-8), r(lo-10,hi-10), r(lo-10,hi-10)),
                    new PhysicalAttributes(r(lo-5,hi-5), r(lo-8,hi-8), r(lo,hi), r(lo-15,hi-15)),
                    new MentalAttributes(r(lo,hi), r(lo,hi), r(lo-5,hi-5), r(lo,hi), r(lo,hi)),
                    new GoalieAttributes(r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi), r(lo,hi))
            );
        };
    }

    private PlayerPersonality generateProspectPersonality(int age) {
        // Young players skew toward YOUNG_GUN and HOT_HEAD
        PersonalityArchetype archetype;
        int roll = random.nextInt(100);
        if (age == 18) {
            archetype = roll < 55 ? PersonalityArchetype.YOUNG_GUN
                    : roll < 80 ? PersonalityArchetype.HOT_HEAD
                    : PersonalityArchetype.QUIET_PRO;
        } else if (age == 19) {
            archetype = roll < 45 ? PersonalityArchetype.YOUNG_GUN
                    : roll < 70 ? PersonalityArchetype.HOT_HEAD
                    : roll < 88 ? PersonalityArchetype.QUIET_PRO
                    : PersonalityArchetype.LEADER;
        } else {
            archetype = roll < 35 ? PersonalityArchetype.YOUNG_GUN
                    : roll < 58 ? PersonalityArchetype.QUIET_PRO
                    : roll < 78 ? PersonalityArchetype.HOT_HEAD
                    : PersonalityArchetype.LEADER;
        }

        ContractPriorities priorities = new ContractPriorities(
                r(40, 70), r(50, 80), r(60, 90), r(30, 70), r(40, 75)
        );
        return new PlayerPersonality(archetype, priorities, r(65, 85));
    }

    /**
     * Archetype distribution weighted so early picks lean elite,
     * late picks lean depth — but hidden gems can appear anywhere.
     */
    private GrowthArchetype rollArchetype(Position pos) {
        boolean isGoalie  = pos == Position.GOALIE;
        boolean isDefense = pos == Position.LEFT_DEFENSE || pos == Position.RIGHT_DEFENSE;
        int roll = random.nextInt(1000);

        if (isGoalie) {
            if (roll < 5)   return GrowthArchetype.STARTER;       // rare top goalie
            if (roll < 250) return GrowthArchetype.STARTER;
            if (roll < 700) return GrowthArchetype.BACKUP;
            return GrowthArchetype.DEPTH_GOALIE;
        }

        if (isDefense) {
            if (roll < 3)   return GrowthArchetype.TOP_PAIR;      // hidden gem
            if (roll < 150) return GrowthArchetype.TOP_PAIR;
            if (roll < 500) return GrowthArchetype.TOP_FOUR;
            return GrowthArchetype.DEPTH_DEFENSE;
        }

        // Forwards
        if (roll < 2)   return GrowthArchetype.GENERATIONAL;      // ultra rare hidden gem
        if (roll < 30)  return GrowthArchetype.FRANCHISE;
        if (roll < 100) return GrowthArchetype.ELITE;
        if (roll < 280) return GrowthArchetype.TOP_SIX;
        if (roll < 520) return GrowthArchetype.TOP_NINE;
        if (roll < 780) return GrowthArchetype.BOTTOM_SIX;
        return GrowthArchetype.DEPTH_FORWARD;
    }

    private ScoutingRegion rollRegion() {
        int roll = random.nextInt(100);
        if (roll < 45) return ScoutingRegion.NORTH_AMERICA;
        if (roll < 65) return ScoutingRegion.EUROPE;
        if (roll < 75) return ScoutingRegion.SCANDINAVIA;
        if (roll < 87) return ScoutingRegion.RUSSIA;
        return ScoutingRegion.INTERNATIONAL;
    }

    private int r(int lo, int hi) {
        lo = Math.max(1, lo);
        hi = Math.max(lo, Math.min(99, hi));
        return lo + random.nextInt(hi - lo + 1);
    }
}
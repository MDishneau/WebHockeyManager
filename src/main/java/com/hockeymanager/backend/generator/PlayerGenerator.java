package com.hockeymanager.backend.generator;

import com.hockeymanager.backend.model.Position;
import com.hockeymanager.backend.model.Player;
import com.hockeymanager.backend.model.attributes.*;
import com.hockeymanager.backend.model.personality.*;

import java.util.Random;

public class PlayerGenerator {

    private final Random random;
    private final NameGenerator nameGenerator;

    public PlayerGenerator(Random random) {
        this.random        = random;
        this.nameGenerator = new NameGenerator(random);
    }

    public Player generate(Position position) {
        String name = nameGenerator.generateName();
        int age     = generateAge();
        PlayerAttributes attrs = generateAttributes(position);
        PlayerPersonality personality = generatePersonality(position, age);
        return new Player(name, position, age, attrs, personality);
    }

    // --- Age: bell curve weighted toward 22-32 ---
    private int generateAge() {
        // Average two rolls to create a bell curve, then shift into 18-38 range
        int roll1 = random.nextInt(21); // 0-20
        int roll2 = random.nextInt(21); // 0-20
        return 18 + (roll1 + roll2) / 2;
    }

    // --- Attributes by position ---
    private PlayerAttributes generateAttributes(Position position) {
        return switch (position) {
            case CENTER       -> generateCenterAttributes();
            case LEFT_WING    -> generateWingAttributes(false);
            case RIGHT_WING   -> generateWingAttributes(true);
            case LEFT_DEFENSE -> generateDefenseAttributes();
            case RIGHT_DEFENSE -> generateDefenseAttributes();
            case GOALIE       -> generateGoalieAttributes();
        };
    }

    private PlayerAttributes generateCenterAttributes() {
        return new PlayerAttributes(
                skating(75, 90),
                shooting(65, 85),
                puckSkills(72, 90),
                passing(75, 92),
                defense(60, 80),
                physical(60, 80),
                mental(70, 90)
        );
    }

    private PlayerAttributes generateWingAttributes(boolean isPower) {
        // Power wingers (RW archetype) skew more physical/shooting
        // Skill wingers (LW archetype) skew more skating/puck
        int shootLow  = isPower ? 72 : 65;
        int shootHigh = isPower ? 92 : 85;
        int physLow   = isPower ? 68 : 55;
        int physHigh  = isPower ? 85 : 75;
        return new PlayerAttributes(
                skating(72, 90),
                shooting(shootLow, shootHigh),
                puckSkills(68, 88),
                passing(65, 85),
                defense(50, 72),
                physical(physLow, physHigh),
                mental(65, 88)
        );
    }

    private PlayerAttributes generateDefenseAttributes() {
        // Randomly skew either offensive (QB) or defensive (stay-at-home)
        boolean offensive = random.nextBoolean();
        int passLow   = offensive ? 72 : 58;
        int passHigh  = offensive ? 90 : 74;
        int shootLow  = offensive ? 65 : 50;
        int shootHigh = offensive ? 82 : 68;
        int defLow    = offensive ? 68 : 78;
        int defHigh   = offensive ? 82 : 92;
        int physLow   = offensive ? 62 : 72;
        int physHigh  = offensive ? 78 : 90;
        return new PlayerAttributes(
                skating(70, 88),
                shooting(shootLow, shootHigh),
                puckSkills(60, 80),
                passing(passLow, passHigh),
                defense(defLow, defHigh),
                physical(physLow, physHigh),
                mental(65, 85)
        );
    }

    private PlayerAttributes generateGoalieAttributes() {
        return new PlayerAttributes(
                skating(62, 78),
                shooting(35, 50),   // irrelevant
                puckSkills(45, 62),
                passing(50, 68),
                defense(55, 70),
                physical(55, 72),
                mental(68, 90),
                goalie(68, 92)
        );
    }

    // --- Personality generation ---
    private PlayerPersonality generatePersonality(Position position, int age) {
        PersonalityArchetype archetype = generateArchetype(position, age);
        ContractPriorities priorities  = generatePriorities(archetype);
        int morale = rand(65, 85); // start reasonably happy
        return new PlayerPersonality(archetype, priorities, morale);
    }

    private PersonalityArchetype generateArchetype(Position position, int age) {
        // Weight archetypes by position and age
        boolean isOld      = age >= 33;
        boolean isYoung    = age <= 22;
        boolean isPhysical = position == Position.LEFT_WING || position == Position.RIGHT_WING;

        int roll = random.nextInt(100);

        if (isOld)   return roll < 40 ? PersonalityArchetype.VETERAN_PRESENCE
                : roll < 65 ? PersonalityArchetype.QUIET_PRO
                : roll < 80 ? PersonalityArchetype.LEADER
                : PersonalityArchetype.MERCENARY;

        if (isYoung) return roll < 50 ? PersonalityArchetype.YOUNG_GUN
                : roll < 75 ? PersonalityArchetype.QUIET_PRO
                : roll < 88 ? PersonalityArchetype.HOT_HEAD
                : PersonalityArchetype.LOCKER_ROOM_GUY;

        if (isPhysical) return roll < 25 ? PersonalityArchetype.HOT_HEAD
                : roll < 50 ? PersonalityArchetype.QUIET_PRO
                : roll < 68 ? PersonalityArchetype.LOCKER_ROOM_GUY
                : roll < 82 ? PersonalityArchetype.LEADER
                : PersonalityArchetype.MERCENARY;

        // Default mix for centers/defense
        return roll < 20 ? PersonalityArchetype.LEADER
                : roll < 40 ? PersonalityArchetype.QUIET_PRO
                : roll < 55 ? PersonalityArchetype.LOCKER_ROOM_GUY
                : roll < 68 ? PersonalityArchetype.YOUNG_GUN
                : roll < 80 ? PersonalityArchetype.HOT_HEAD
                : roll < 90 ? PersonalityArchetype.VETERAN_PRESENCE
                : PersonalityArchetype.MERCENARY;
    }

    private ContractPriorities generatePriorities(PersonalityArchetype archetype) {
        return switch (archetype) {
            case MERCENARY         -> new ContractPriorities(
                    rand(80, 99), rand(20, 45), rand(60, 80),
                    rand(20, 50), rand(10, 30));
            case LEADER            -> new ContractPriorities(
                    rand(40, 65), rand(80, 99), rand(65, 85),
                    rand(40, 70), rand(60, 85));
            case LOCKER_ROOM_GUY   -> new ContractPriorities(
                    rand(45, 68), rand(65, 85), rand(55, 75),
                    rand(50, 80), rand(65, 88));
            case HOT_HEAD          -> new ContractPriorities(
                    rand(60, 85), rand(50, 75), rand(70, 90),
                    rand(30, 60), rand(30, 55));
            case YOUNG_GUN         -> new ContractPriorities(
                    rand(55, 80), rand(60, 85), rand(70, 92),
                    rand(40, 70), rand(45, 70));
            case VETERAN_PRESENCE  -> new ContractPriorities(
                    rand(35, 60), rand(70, 90), rand(50, 72),
                    rand(55, 85), rand(70, 92));
            case QUIET_PRO         -> new ContractPriorities(
                    rand(50, 75), rand(55, 78), rand(55, 78),
                    rand(40, 70), rand(55, 78));
        };
    }

    // --- Attribute builder helpers ---

    private SkatingAttributes skating(int low, int high) {
        return new SkatingAttributes(
                rand(low, high), rand(low, high),
                rand(low, high), rand(low, high));
    }

    private ShootingAttributes shooting(int low, int high) {
        return new ShootingAttributes(
                rand(low, high), rand(low, high), rand(low, high),
                rand(low, high), rand(low, high));
    }

    private PuckSkillsAttributes puckSkills(int low, int high) {
        return new PuckSkillsAttributes(
                rand(low, high), rand(low, high),
                rand(low, high), rand(low, high));
    }

    private PassingAttributes passing(int low, int high) {
        return new PassingAttributes(
                rand(low, high), rand(low, high),
                rand(low, high), rand(low, high));
    }

    private DefenseAttributes defense(int low, int high) {
        return new DefenseAttributes(
                rand(low, high), rand(low, high),
                rand(low, high), rand(low, high));
    }

    private PhysicalAttributes physical(int low, int high) {
        return new PhysicalAttributes(
                rand(low, high), rand(low, high),
                rand(low, high), rand(low, high));
    }

    private MentalAttributes mental(int low, int high) {
        return new MentalAttributes(
                rand(low, high), rand(low, high), rand(low, high),
                rand(low, high), rand(low, high));
    }

    private GoalieAttributes goalie(int low, int high) {
        return new GoalieAttributes(
                rand(low, high), rand(low, high), rand(low, high),
                rand(low, high), rand(low, high), rand(low, high),
                rand(low, high));
    }

    private int rand(int low, int high) {
        return low + random.nextInt(high - low + 1);
    }
}
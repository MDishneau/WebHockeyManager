package com.hockeymanager.backend.model;

import com.hockeymanager.backend.model.attributes.*;
import com.hockeymanager.backend.model.personality.*;

/**
 * Convenience factory so we're not constructing giant attribute
 * trees inline in Main. Will later be used by the player generator.
 */
public class PlayerFactory {

    public static Player createSkater(String name, Position position, int age,
                                      // Skating
                                      int speed, int accel, int agility, int stamina,
                                      // Shooting
                                      int sPower, int sAccuracy, int oneTimer, int wrist, int slap,
                                      // Puck
                                      int handling, int deking, int boards, int protection,
                                      // Passing
                                      int pAccuracy, int vision, int saucer, int oneTouch,
                                      // Defense
                                      int dPositioning, int stickCheck, int bodyCheck, int shotBlock,
                                      // Physical
                                      int strength, int aggression, int balance, int fighting,
                                      // Mental
                                      int composure, int compete, int leadership, int iq, int clutch,
                                      // Personality
                                      PersonalityArchetype archetype,
                                      int moneyPriority, int winningPriority, int rolePriority,
                                      int hometownPriority, int loyaltyPriority,
                                      int initialMorale) {

        PlayerAttributes attrs = new PlayerAttributes(
                new SkatingAttributes(speed, accel, agility, stamina),
                new ShootingAttributes(sPower, sAccuracy, oneTimer, wrist, slap),
                new PuckSkillsAttributes(handling, deking, boards, protection),
                new PassingAttributes(pAccuracy, vision, saucer, oneTouch),
                new DefenseAttributes(dPositioning, stickCheck, bodyCheck, shotBlock),
                new PhysicalAttributes(strength, aggression, balance, fighting),
                new MentalAttributes(composure, compete, leadership, iq, clutch)
        );

        PlayerPersonality personality = new PlayerPersonality(
                archetype,
                new ContractPriorities(moneyPriority, winningPriority, rolePriority,
                        hometownPriority, loyaltyPriority),
                initialMorale
        );

        return new Player(name, position, age, attrs, personality);
    }

    public static Player createGoalie(String name, int age,
                                      // Skating (goalies still skate)
                                      int speed, int accel, int agility, int stamina,
                                      // Goalie-specific
                                      int reflexes, int positioning, int glove, int blocker,
                                      int reboundControl, int pokeCheck, int fiveHole,
                                      // Mental
                                      int composure, int compete, int leadership, int iq, int clutch,
                                      // Personality
                                      PersonalityArchetype archetype,
                                      int moneyPriority, int winningPriority, int rolePriority,
                                      int hometownPriority, int loyaltyPriority,
                                      int initialMorale) {

        PlayerAttributes attrs = new PlayerAttributes(
                new SkatingAttributes(speed, accel, agility, stamina),
                new ShootingAttributes(40, 40, 40, 40, 40), // irrelevant for goalies
                new PuckSkillsAttributes(50, 40, 50, 50),
                new PassingAttributes(55, 55, 45, 45),
                new DefenseAttributes(60, 50, 40, 40),
                new PhysicalAttributes(60, 50, 70, 30),
                new MentalAttributes(composure, compete, leadership, iq, clutch),
                new GoalieAttributes(reflexes, positioning, glove, blocker,
                        reboundControl, pokeCheck, fiveHole)
        );

        PlayerPersonality personality = new PlayerPersonality(
                archetype,
                new ContractPriorities(moneyPriority, winningPriority, rolePriority,
                        hometownPriority, loyaltyPriority),
                initialMorale
        );

        return new Player(name, Position.GOALIE, age, attrs, personality);
    }
}
package com.hockeymanager.backend.model.personality;

/**
 * Combines archetype + contract priorities + live morale state.
 *
 * Morale (0-100) affects performance in the sim:
 *   90-100 = hot streak bonus
 *   60-89  = normal
 *   40-59  = slight penalty
 *   0-39   = significant penalty (player is unhappy)
 *
 * Happiness drivers tracked as deltas — will be used by future
 * events (contract signed, traded, benched, won cup, etc.)
 */
public class PlayerPersonality {

    private final PersonalityArchetype archetype;
    private final ContractPriorities   contractPriorities;
    private int morale; // 0-100, mutable

    public PlayerPersonality(PersonalityArchetype archetype,
                             ContractPriorities contractPriorities,
                             int initialMorale) {
        this.archetype          = archetype;
        this.contractPriorities = contractPriorities;
        this.morale             = clamp(initialMorale);
    }

    // --- Morale helpers ---

    public void adjustMorale(int delta) {
        morale = clamp(morale + delta);
    }

    public MoraleLevel getMoraleLevel() {
        if (morale >= 90) return MoraleLevel.ECSTATIC;
        if (morale >= 70) return MoraleLevel.HAPPY;
        if (morale >= 50) return MoraleLevel.NEUTRAL;
        if (morale >= 30) return MoraleLevel.UNHAPPY;
        return MoraleLevel.MISERABLE;
    }

    /**
     * Morale multiplier applied to effective ratings in sim.
     * Range: 0.85 (miserable) to 1.08 (ecstatic)
     */
    public double getMoraleMultiplier() {
        return switch (getMoraleLevel()) {
            case ECSTATIC  -> 1.08;
            case HAPPY     -> 1.02;
            case NEUTRAL   -> 1.00;
            case UNHAPPY   -> 0.93;
            case MISERABLE -> 0.85;
        };
    }

    private int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    public PersonalityArchetype getArchetype()               { return archetype; }
    public ContractPriorities   getContractPriorities()      { return contractPriorities; }
    public int                  getMorale()                  { return morale; }

    @Override
    public String toString() {
        return archetype + " | Morale: " + morale + " (" + getMoraleLevel() + ")";
    }
}
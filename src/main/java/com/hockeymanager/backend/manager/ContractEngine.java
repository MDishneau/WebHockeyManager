package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.*;
import com.hockeymanager.backend.model.personality.ContractPriorities;

import java.util.Random;

/**
 * Calculates market value for players and determines
 * whether they accept or reject contract offers.
 */
public class ContractEngine {

    private final Random random;

    public ContractEngine(Random random) {
        this.random = random;
    }

    // ── Market Value ──────────────────────────────────────────

    /**
     * Base market value in dollars based on OVR, age, archetype.
     * Scales from MIN_SALARY to MAX_SALARY.
     */
    public long calculateMarketValue(Player player) {
        int ovr  = player.getOverall();
        int age  = player.getAge();
        GrowthArchetype arch = player.getGrowthArchetype();

        // Base: scale OVR 50-99 → MIN to MAX salary
        double ovr_factor = Math.max(0, (ovr - 50)) / 49.0; // 0.0 to 1.0
        long base = Contract.MIN_SALARY +
                (long)(ovr_factor * (Contract.MAX_SALARY - Contract.MIN_SALARY));

        // Age adjustment: peak 26-30, discount young/old
        double age_mult;
        if      (age <= 22) age_mult = 0.70;
        else if (age <= 25) age_mult = 0.88;
        else if (age <= 30) age_mult = 1.00;
        else if (age <= 33) age_mult = 0.90;
        else                age_mult = 0.75;

        // Archetype premium
        double arch_mult = 1.0;
        if (arch != null) {
            arch_mult = switch (arch) {
                case GENERATIONAL -> 1.20;
                case FRANCHISE    -> 1.10;
                case ELITE        -> 1.05;
                case STARTER      -> 1.05;
                default           -> 1.00;
            };
        }

        long value = (long)(base * age_mult * arch_mult);
        return clamp(value, Contract.MIN_SALARY, Contract.MAX_SALARY);
    }

    /**
     * Player's actual asking price = market value adjusted by contract priorities.
     * Money-driven players ask more; loyalty-driven players ask less from current team.
     */
    public long calculateAskingPrice(Player player, boolean isCurrentTeam) {
        long market = calculateMarketValue(player);
        ContractPriorities pri = player.getPersonality().getContractPriorities();

        // Money priority: 0-100 → multiplier 0.90 to 1.15
        double moneyMult = 0.90 + (pri.getMoney() / 100.0) * 0.25;

        // Loyalty discount if re-signing with current team
        double loyaltyDiscount = 1.0;
        if (isCurrentTeam) {
            loyaltyDiscount = 1.0 - (pri.getLoyalty() / 100.0) * 0.15;
        }

        long asking = (long)(market * moneyMult * loyaltyDiscount);
        return clamp(asking, Contract.MIN_SALARY, Contract.MAX_SALARY);
    }

    /**
     * Determine preferred contract length based on age and priorities.
     * Younger players want shorter deals to hit UFA sooner.
     * Older players want security (longer deals).
     */
    public int preferredLength(Player player, boolean sameTeam) {
        int age    = player.getAge();
        int maxYrs = sameTeam ? Contract.MAX_YEARS_SAME : Contract.MAX_YEARS_NEW;
        ContractPriorities pri = player.getPersonality().getContractPriorities();

        int base;
        if      (age <= 23) base = 2 + random.nextInt(2); // 2-3 yrs (hit UFA young)
        else if (age <= 27) base = 3 + random.nextInt(2); // 3-4 yrs
        else if (age <= 31) base = 4 + random.nextInt(3); // 4-6 yrs (security)
        else                base = 1 + random.nextInt(3); // 1-3 yrs (take what they can)

        // Loyalty-driven players want longer deals with current team
        if (sameTeam && pri.getLoyalty() > 70) base = Math.min(base + 1, maxYrs);

        return Math.min(base, maxYrs);
    }

    /**
     * Determine contract type based on age and service years.
     */
    public ContractType determineType(Player player) {
        int age     = player.getAge();
        int service = player.getContract() != null
                ? player.getContract().getServiceYears() : 0;

        if (age <= 21 && service == 0) return ContractType.ENTRY_LEVEL;
        if (age < 27 && service < 7)   return ContractType.RFA;
        if (calculateMarketValue(player) >= Contract.MAX_SALARY * 0.95)
            return ContractType.MAX;
        return ContractType.UFA;
    }

    // ── Offer Evaluation ─────────────────────────────────────

    /**
     * Returns true if the player accepts the offer.
     * Factors: salary vs asking, winning priority, role priority, loyalty.
     */
    public boolean evaluateOffer(Player player, long offeredSalary,
                                 boolean isCurrentTeam, double teamWinPct,
                                 boolean isTopRole) {
        ContractPriorities pri = player.getPersonality().getContractPriorities();
        long asking = calculateAskingPrice(player, isCurrentTeam);

        // Salary score: how close is the offer to their ask?
        double salaryScore = Math.min(1.0, (double) offeredSalary / asking);

        // Winning score (0-1)
        double winScore = teamWinPct; // pass in as 0.0-1.0

        // Role score
        double roleScore = isTopRole ? 1.0 : 0.5;

        // Loyalty bonus
        double loyaltyBonus = isCurrentTeam ? (pri.getLoyalty() / 100.0) * 0.2 : 0.0;

        // Weighted acceptance score
        double score = salaryScore     * (pri.getMoney()   / 100.0) * 0.40
                + winScore        * (pri.getWinning() / 100.0) * 0.30
                + roleScore       * (pri.getRole()    / 100.0) * 0.20
                + loyaltyBonus;

        // Accept if score > threshold (with some randomness)
        double threshold = 0.55 + random.nextDouble() * 0.15;
        return score >= threshold;
    }

    // ── ELC Factory ──────────────────────────────────────────

    public Contract createELC(int years) {
        years = Math.min(3, Math.max(1, years));
        return new Contract(Contract.ELC_MAX_BASE, years,
                ContractType.ENTRY_LEVEL, true);
    }

    public Contract createContract(Player player, long salary,
                                   int years, boolean sameTeam) {
        ContractType type = determineType(player);
        int maxYrs = sameTeam ? Contract.MAX_YEARS_SAME : Contract.MAX_YEARS_NEW;
        years = Math.min(years, maxYrs);
        return new Contract(salary, years, type, false);
    }

    private long clamp(long v, long lo, long hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

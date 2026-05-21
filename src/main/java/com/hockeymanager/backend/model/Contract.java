package com.hockeymanager.backend.model;

/**
 * Represents a player contract.
 *
 * 2026-27 NHL rules:
 *   Cap: $104,000,000
 *   Floor: $76,900,000
 *   Max salary: $20,800,000 (20% of cap)
 *   ELC base: up to $1,000,000 + bonuses
 *   Min salary: $850,000
 *
 * Re-sign same team: max 7 years
 * Sign with new team: max 6 years
 */
public class Contract {

    public static final long CAP_CEILING     = 104_000_000L;
    public static final long CAP_FLOOR       = 76_900_000L;
    public static final long MAX_SALARY      = 20_800_000L;
    public static final long ELC_MAX_BASE    = 1_000_000L;
    public static final long MIN_SALARY      = 850_000L;
    public static final int  MAX_YEARS_SAME  = 7;
    public static final int  MAX_YEARS_NEW   = 6;

    private final long         annualSalary; // cap hit per year
    private final int          years;
    private final ContractType type;
    private       int          yearsRemaining;
    private       int          serviceYears;  // NHL service years accumulated
    private final boolean      hasPerformanceBonus; // ELC only

    public Contract(long annualSalary, int years, ContractType type, boolean hasPerformanceBonus) {
        this.annualSalary        = clampSalary(annualSalary, type);
        this.years               = years;
        this.yearsRemaining      = years;
        this.type                = type;
        this.hasPerformanceBonus = hasPerformanceBonus;
        this.serviceYears        = 0;
    }

    private long clampSalary(long salary, ContractType type) {
        if (type == ContractType.ENTRY_LEVEL) return Math.min(salary, ELC_MAX_BASE);
        return Math.max(MIN_SALARY, Math.min(salary, MAX_SALARY));
    }

    /** Call at end of each season to tick down contract */
    public void advanceYear() {
        if (yearsRemaining > 0) yearsRemaining--;
        serviceYears++;
    }

    public boolean isExpired()      { return yearsRemaining <= 0; }
    public long getCapHit()         { return annualSalary; }
    public long getAnnualSalary()   { return annualSalary; }
    public int getYears()           { return years; }
    public int getYearsRemaining()  { return yearsRemaining; }
    public int getServiceYears()    { return serviceYears; }
    public ContractType getType()   { return type; }
    public boolean hasBonus()       { return hasPerformanceBonus; }

    public String getSalaryDisplay() {
        return String.format("$%,.0fM", annualSalary / 1_000_000.0);
    }

    @Override
    public String toString() {
        return String.format("%s  %s x%d yr  (%d remaining)",
                type.getDisplayName(), getSalaryDisplay(), years, yearsRemaining);
    }
}

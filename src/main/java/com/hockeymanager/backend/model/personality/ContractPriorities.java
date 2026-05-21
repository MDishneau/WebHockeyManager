package com.hockeymanager.backend.model.personality;

/**
 * How much a player weighs each factor when deciding on a contract.
 * All values 0-100. Higher = cares more about that factor.
 *
 * Examples:
 *   A mercenary: money=95, winning=20, loyalty=10
 *   A hometown kid: hometown=90, loyalty=80, money=40
 *   A winner: winning=90, role=70, money=40
 */
public class ContractPriorities {

    private final int money;     // wants max dollars
    private final int winning;   // wants to be on a contender
    private final int role;      // wants top-line / top-pair / starter role
    private final int hometown;  // wants to stay in/go to a specific city
    private final int loyalty;   // values staying with current team

    public ContractPriorities(int money, int winning, int role, int hometown, int loyalty) {
        this.money    = clamp(money);
        this.winning  = clamp(winning);
        this.role     = clamp(role);
        this.hometown = clamp(hometown);
        this.loyalty  = clamp(loyalty);
    }

    private int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    public int getMoney()    { return money; }
    public int getWinning()  { return winning; }
    public int getRole()     { return role; }
    public int getHometown() { return hometown; }
    public int getLoyalty()  { return loyalty; }

    @Override
    public String toString() {
        return String.format("Money:%d Winning:%d Role:%d Hometown:%d Loyalty:%d",
                money, winning, role, hometown, loyalty);
    }
}
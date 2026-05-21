package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects all players entering free agency at season end.
 * - Players with expired contracts
 * - Players explicitly released
 * Each entry tracks which team they came from and their FA status.
 */
public class FreeAgentPool {

    public record FreeAgent(Player player, Team previousTeam, ContractType status) {}

    private final List<FreeAgent> pool = new ArrayList<>();
    private final ContractEngine  contractEngine;

    public FreeAgentPool(ContractEngine contractEngine) {
        this.contractEngine = contractEngine;
    }

    public void collectFromLeague(League league) {
        for (Team team : league.getTeams()) {
            List<Player> toRemove = new ArrayList<>();
            for (Player p : team.getRoster()) {
                if (shouldEnterFA(p)) {
                    ContractType status = contractEngine.determineType(p);
                    pool.add(new FreeAgent(p, team, status));
                    toRemove.add(p);
                }
            }
            team.getRoster().removeAll(toRemove);
        }
    }

    public void addReleasedPlayer(Player player, Team team) {
        ContractType status = contractEngine.determineType(player);
        pool.add(new FreeAgent(player, team, status));
        team.getRoster().remove(player);
    }

    private boolean shouldEnterFA(Player p) {
        if (p.isReleased()) return true;
        Contract c = p.getContract();
        if (c == null) return true; // no contract = FA
        return c.isExpired();
    }

    public List<FreeAgent> getPool()         { return pool; }
    public List<FreeAgent> getUFAs()         {
        return pool.stream()
                .filter(fa -> fa.status() == ContractType.UFA
                        || fa.status() == ContractType.MAX)
                .toList();
    }
    public List<FreeAgent> getRFAs()         {
        return pool.stream()
                .filter(fa -> fa.status() == ContractType.RFA)
                .toList();
    }
    public void remove(FreeAgent fa)         { pool.remove(fa); }
    public boolean isEmpty()                 { return pool.isEmpty(); }
}

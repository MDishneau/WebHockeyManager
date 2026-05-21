package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.Contract;
import com.hockeymanager.backend.model.Player;
import com.hockeymanager.backend.model.Team;

/**
 * Tracks cap usage per team, enforces ceiling and warns on floor.
 */
public class SalaryCapManager {

    public long getTeamCapHit(Team team) {
        return team.getRoster().stream()
                .filter(p -> p.getContract() != null)
                .mapToLong(p -> p.getContract().getCapHit())
                .sum();
    }

    public long getCapSpace(Team team) {
        return Contract.CAP_CEILING - getTeamCapHit(team);
    }

    public boolean canSign(Team team, long salary) {
        return getCapSpace(team) >= salary;
    }

    public boolean isUnderFloor(Team team) {
        return getTeamCapHit(team) < Contract.CAP_FLOOR;
    }

    public boolean isOverCap(Team team) {
        return getTeamCapHit(team) > Contract.CAP_CEILING;
    }

    public String getCapSummary(Team team) {
        long hit   = getTeamCapHit(team);
        long space = getCapSpace(team);
        String status = isOverCap(team) ? " ⚠ OVER CAP"
                : isUnderFloor(team) ? " ⚠ UNDER FLOOR"
                : "";
        return String.format("Cap Hit: $%.2fM / $%.2fM  Space: $%.2fM%s",
                hit / 1_000_000.0,
                Contract.CAP_CEILING / 1_000_000.0,
                space / 1_000_000.0,
                status);
    }
}

package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs the 7-round draft.
 * Round 1: picks in draft order (1→32)
 * Round 2: reverse (32→1) — snake format
 * Alternates each round like real NHL.
 *
 * CPU teams auto-pick best available by overall + positional need.
 * User picks manually when it's their turn.
 */
public class DraftEngine {

    private static final int ROUNDS = 7;

    private final List<Team>     draftOrder;    // pick 1 = index 0
    private final List<Prospect> availableProspects;
    private final Team           userTeam;
    private final List<Prospect> draftResults;  // in pick order

    public DraftEngine(List<Team> draftOrder, List<Prospect> draftClass, Team userTeam) {
        this.draftOrder          = draftOrder;
        this.availableProspects  = new ArrayList<>(draftClass);
        this.userTeam            = userTeam;
        this.draftResults        = new ArrayList<>();
    }

    public List<Prospect> getAvailableProspects() { return availableProspects; }
    public List<Prospect> getDraftResults()        { return draftResults; }

    /** Returns the team picking at this slot */
    public Team getTeamForPick(int round, int pickInRound) {
        // Odd rounds: 0→31, even rounds: 31→0 (snake)
        boolean forward = (round % 2 == 1);
        int index = forward ? pickInRound : (draftOrder.size() - 1 - pickInRound);
        return draftOrder.get(index);
    }

    public int totalPicks() {
        return ROUNDS * draftOrder.size();
    }

    /** CPU auto-pick: best available considering positional need */
    public Prospect cpuPick(Team team) {
        Map<Position, Long> rosterCounts = team.getRoster().stream()
                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));

        // Score each prospect: overall + positional need bonus
        return availableProspects.stream()
                .max(Comparator.comparingDouble(p -> scoreForTeam(p, rosterCounts)))
                .orElse(availableProspects.get(0));
    }

    private double scoreForTeam(Prospect p, Map<Position, Long> rosterCounts) {
        double base = p.getOverall();
        long count  = rosterCounts.getOrDefault(p.getPosition(), 0L);
        // Bonus for positions where team is thin
        double needBonus = count < 3 ? 5.0 : count < 5 ? 2.0 : 0.0;
        return base + needBonus;
    }

    public void draftPlayer(Prospect prospect, Team team) {
        prospect.getPlayer().setGrowthArchetype(prospect.getGrowthArchetype());
        team.addPlayer(prospect.getPlayer());
        availableProspects.remove(prospect);
        draftResults.add(prospect);
    }
}
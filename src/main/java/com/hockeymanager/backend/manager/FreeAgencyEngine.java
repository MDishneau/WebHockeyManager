package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.*;

import java.util.*;

/**
 * Manages the free agency process.
 *
 * Flow:
 * 1. User submits offers (stored as pending)
 * 2. CPU teams also make offers (simulated simultaneously)
 * 3. User resolves — each player picks best offer they'd accept
 */
public class FreeAgencyEngine {

    public record Offer(Team team, Player player, long salary, int years) {}

    private final ContractEngine     contractEngine;
    private final SalaryCapManager   capManager;
    private final Random             random;
    private final List<Offer>        pendingOffers = new ArrayList<>();

    public FreeAgencyEngine(ContractEngine contractEngine,
                            SalaryCapManager capManager, Random random) {
        this.contractEngine = contractEngine;
        this.capManager     = capManager;
        this.random         = random;
    }

    // ── User offer submission ─────────────────────────────────

    public boolean submitOffer(Team team, Player player, long salary, int years) {
        if (!capManager.canSign(team, salary)) return false;
        // Remove any existing offer from this team to this player
        pendingOffers.removeIf(o -> o.team() == team && o.player() == player);
        pendingOffers.add(new Offer(team, player, salary, years));
        return true;
    }

    public void withdrawOffer(Team team, Player player) {
        pendingOffers.removeIf(o -> o.team() == team && o.player() == player);
    }

    public List<Offer> getPendingOffersFor(Team team) {
        return pendingOffers.stream().filter(o -> o.team() == team).toList();
    }

    // ── CPU offer generation ─────────────────────────────────

    public void generateCpuOffers(FreeAgentPool pool, League league, Team userTeam) {
        for (Team team : league.getTeams()) {
            if (team == userTeam) continue;
            // Each CPU team tries to fill roster gaps
            int forwardCount  = countPosition(team, List.of(
                    Position.CENTER, Position.LEFT_WING, Position.RIGHT_WING));
            int defenseCount  = countPosition(team, List.of(
                    Position.LEFT_DEFENSE, Position.RIGHT_DEFENSE));
            int goalieCount   = countPosition(team, List.of(Position.GOALIE));

            for (FreeAgentPool.FreeAgent fa : pool.getPool()) {
                if (capManager.getCapSpace(team) < Contract.MIN_SALARY) break;
                if (hasOfferFor(team, fa.player())) continue;

                boolean needed = switch (fa.player().getPosition()) {
                    case CENTER, LEFT_WING, RIGHT_WING     -> forwardCount < 12;
                    case LEFT_DEFENSE, RIGHT_DEFENSE       -> defenseCount < 6;
                    case GOALIE                            -> goalieCount  < 2;
                };
                if (!needed) continue;

                long market  = contractEngine.calculateMarketValue(fa.player());
                long offered = (long)(market * (0.90 + random.nextDouble() * 0.20));
                offered      = Math.min(offered, capManager.getCapSpace(team));
                int years    = contractEngine.preferredLength(fa.player(), false);

                if (capManager.canSign(team, offered)) {
                    pendingOffers.add(new Offer(team, fa.player(), offered, years));
                }
            }
        }
    }

    // ── Resolution ───────────────────────────────────────────

    /**
     * Resolves all pending offers. Each FA player evaluates all offers
     * they received, picks the best acceptable one, signs it.
     * Returns list of signed deals for display.
     */
    public List<String> resolveAllOffers(FreeAgentPool pool, Team userTeam) {
        List<String> results = new ArrayList<>();

        // Group offers by player
        Map<Player, List<Offer>> byPlayer = new LinkedHashMap<>();
        for (Offer o : pendingOffers) {
            byPlayer.computeIfAbsent(o.player(), k -> new ArrayList<>()).add(o);
        }

        for (Map.Entry<Player, List<Offer>> entry : byPlayer.entrySet()) {
            Player player = entry.getKey();
            List<Offer> offers = entry.getValue();

            // Find the FA entry
            FreeAgentPool.FreeAgent fa = pool.getPool().stream()
                    .filter(f -> f.player() == player)
                    .findFirst().orElse(null);
            if (fa == null) continue;

            // Player evaluates each offer
            Offer accepted = null;
            double bestScore = -1;

            for (Offer o : offers) {
                if (!capManager.canSign(o.team(), o.salary())) continue;
                boolean isCurrentTeam = o.team() == fa.previousTeam();
                double winPct = getTeamWinPct(o.team());
                boolean topRole = isTopRole(player, o.team());

                boolean accepts = contractEngine.evaluateOffer(
                        player, o.salary(), isCurrentTeam, winPct, topRole);

                if (accepts) {
                    // Score this offer from player's perspective
                    long asking = contractEngine.calculateAskingPrice(player, isCurrentTeam);
                    double score = (double) o.salary() / asking;
                    if (score > bestScore) {
                        bestScore = score;
                        accepted  = o;
                    }
                }
            }

            if (accepted != null) {
                // Sign the player
                Contract contract = contractEngine.createContract(
                        player, accepted.salary(), accepted.years(),
                        accepted.team() == fa.previousTeam());
                player.setContract(contract);
                player.setReleased(false);
                accepted.team().addPlayer(player);
                pool.remove(fa);

                String tag = accepted.team() == userTeam ? " ◄ YOUR TEAM" : "";
                results.add(String.format("%s signs with %s — %s x%d yr%s",
                        player.getName(), accepted.team().getFullName(),
                        contract.getSalaryDisplay(), accepted.years(), tag));
            } else {
                results.add(player.getName() + " remains unsigned.");
            }
        }

        pendingOffers.clear();
        return results;
    }

    // ── Helpers ──────────────────────────────────────────────

    private boolean hasOfferFor(Team team, Player player) {
        return pendingOffers.stream()
                .anyMatch(o -> o.team() == team && o.player() == player);
    }

    private int countPosition(Team team, List<Position> positions) {
        return (int) team.getRoster().stream()
                .filter(p -> positions.contains(p.getPosition()))
                .count();
    }

    private double getTeamWinPct(Team team) {
        int gp = team.getGamesPlayed();
        return gp == 0 ? 0.5 : (double) team.getWins() / gp;
    }

    private boolean isTopRole(Player player, Team team) {
        return team.getRoster().stream()
                .filter(p -> p.getPosition() == player.getPosition())
                .filter(p -> p.getOverall() > player.getOverall())
                .count() < 2;
    }
}

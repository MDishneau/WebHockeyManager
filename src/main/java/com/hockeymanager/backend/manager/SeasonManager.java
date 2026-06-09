package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.engine.GameResult;
import com.hockeymanager.backend.engine.GameSimulator;
import com.hockeymanager.backend.model.*;
import java.util.Optional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SeasonManager {

    private final League league;
    private final GameSimulator simulator;
    private final List<ScheduledGame> schedule;
    private GameDate currentDate;

    public SeasonManager(League league, GameSimulator simulator, List<ScheduledGame> schedule, GameDate seasonStart) {
        this.league      = league;
        this.simulator   = simulator;
        this.schedule    = schedule;
        this.currentDate = seasonStart;
    }

    // --- Advance methods ---

    /**
     * Advances to the user team's next unplayed game date, sims all games
     * on that date (whole league), and returns all results from that day.
     * This keeps the schedule consistent while giving the user a game to watch.
     */
    public List<GameResult> simUpToUserGame(Team userTeam) {
        List<GameResult> results = new ArrayList<>();

        // Find the date of the user's next unplayed game
        Optional<GameDate> userNextDate = schedule.stream()
                .filter(g -> !g.isPlayed()
                        && (g.getHomeTeam() == userTeam || g.getAwayTeam() == userTeam))
                .map(ScheduledGame::getDate)
                .findFirst();

        if (userNextDate.isEmpty()) {
            return results; // no more user games
        }

        GameDate targetDate = userNextDate.get();

        // Sim every unplayed game up to and including that date
        for (ScheduledGame game : schedule) {
            if (!game.isPlayed() && !game.getDate().isAfter(targetDate)) {
                results.add(playGame(game));
            }
        }

        currentDate = targetDate.nextDay();
        return results;
    }

    public List<GameResult> simOneGame() {
        List<GameResult> results = new ArrayList<>();
        for (ScheduledGame game : schedule) {
            if (!game.isPlayed() && !game.getDate().isAfter(currentDate)) {
                results.add(playGame(game));
                return results; // only one
            }
        }
        // No game today — advance to next game date
        advanceToNextGameDate();
        return results;
    }

    public List<GameResult> simDay() {
        List<GameResult> results = new ArrayList<>();
        for (ScheduledGame game : schedule) {
            if (!game.isPlayed() && game.getDate().equals(currentDate)) {
                results.add(playGame(game));
            }
        }
        currentDate = currentDate.nextDay();
        return results;
    }

    public List<GameResult> simWeek() {
        List<GameResult> results = new ArrayList<>();
        GameDate weekEnd = currentDate.plusDays(7);
        for (ScheduledGame game : schedule) {
            if (!game.isPlayed() && !game.getDate().isAfter(weekEnd)) {
                results.add(playGame(game));
            }
        }
        currentDate = weekEnd.nextDay();
        return results;
    }

    public List<GameResult> simSeason() {
        List<GameResult> results = new ArrayList<>();
        for (ScheduledGame game : schedule) {
            if (!game.isPlayed()) {
                results.add(playGame(game));
            }
        }
        currentDate = schedule.get(schedule.size() - 1).getDate();
        return results;
    }

    private GameResult playGame(ScheduledGame game) {
        GameResult result = simulator.simulate(game.getHomeTeam(), game.getAwayTeam());
        game.setResult(result);
        return result;
    }

    private void advanceToNextGameDate() {
        for (ScheduledGame game : schedule) {
            if (!game.isPlayed()) {
                currentDate = game.getDate();
                return;
            }
        }
    }

    // --- Status helpers ---

    public boolean isSeasonOver() {
        return schedule.stream().allMatch(ScheduledGame::isPlayed);
    }

    public int gamesPlayed() {
        return (int) schedule.stream().filter(ScheduledGame::isPlayed).count();
    }

    public int totalGames() {
        return schedule.size();
    }

    public GameDate getCurrentDate() {
        return currentDate;
    }

    public List<ScheduledGame> getUpcomingGames(int n) {
        return schedule.stream()
                .filter(g -> !g.isPlayed())
                .limit(n)
                .collect(Collectors.toList());
    }

    public List<ScheduledGame> getRecentResults(int n) {
        List<ScheduledGame> played = schedule.stream()
                .filter(ScheduledGame::isPlayed)
                .collect(Collectors.toList());
        int from = Math.max(0, played.size() - n);
        return played.subList(from, played.size());
    }

    public List<ScheduledGame> getSchedule() {
        return schedule;
    }

    public void printTopScorers(int n) {
        System.out.println("\n=== TOP " + n + " SCORERS ===");
        System.out.printf("%-20s %-25s %4s %4s %4s %4s%n", "Player", "Team", "G", "A", "PTS", "GP");
        System.out.println("-".repeat(65));

        league.getTeams().stream()
                .flatMap(t -> t.getRoster().stream()
                        .filter(p -> p.getPosition() != Position.GOALIE)
                        .map(p -> new Object[]{p, t}))
                .sorted((a, b) -> ((Player) b[0]).getPoints() - ((Player) a[0]).getPoints())
                .limit(n)
                .forEach(pair -> {
                    Player p = (Player) pair[0];
                    Team   t = (Team)   pair[1];
                    System.out.printf("%-20s %-25s %4d %4d %4d %4d%n",
                            p.getName(), t.getFullName(),
                            p.getGoals(), p.getAssists(), p.getPoints(), p.getGamesPlayed());
                });
    }

    public League getLeague() { return league; }
}
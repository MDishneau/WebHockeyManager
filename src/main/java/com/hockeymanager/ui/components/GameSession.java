package com.hockeymanager.ui.components;

import com.hockeymanager.backend.engine.GameResult;
import com.hockeymanager.backend.engine.GameSimulator;
import com.hockeymanager.backend.engine.PlayoffSimulator;
import com.hockeymanager.backend.generator.LeagueGenerator;
import com.hockeymanager.backend.generator.NameGenerator;
import com.hockeymanager.backend.generator.ProspectGenerator;
import com.hockeymanager.backend.generator.TeamGenerator;
import com.hockeymanager.backend.manager.*;
import com.hockeymanager.backend.model.*;
import com.hockeymanager.ui.layouts.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;


import java.util.*;
import java.util.stream.Collectors;



/**
 * Central game state holder for one UI session.
 * Replaces GameContext + CareerLoop from the console game.
 * All views inject this bean to read/write game state.
 */
@Component
@UIScope
public class GameSession  {
    private Runnable onChange;
    private final NameGenerator nameGenerator;
    private final TeamGenerator teamGenerator;
    // ── Core state ──────────────────────────────────────────────────
    private League        league;
    private Team          userTeam;
    private SeasonManager seasonManager;
    private NewsService   newsService;
    private SeasonPhase   phase = SeasonPhase.REGULAR_SEASON;
    private int           year  = 2024;

    // ── Playoff state ───────────────────────────────────────────────
    private PlayoffBracket    playoffBracket;
    private List<Team>        lastPlayoffTeams = new ArrayList<>();
    private final PlayoffSimulator playoffSimulator = new PlayoffSimulator();

    // ── Offseason state ─────────────────────────────────────────────
    private ScoutingService   scoutingService;
    private List<Prospect>    draftClass;
    private DraftEngine       draftEngine;
    private FreeAgentPool     freeAgentPool;
    private FreeAgencyEngine  freeAgencyEngine;
    private SalaryCapManager  salaryCapManager;
    private ContractEngine    contractEngine;
    private boolean           scoutingDone = false;
    private boolean           draftDone    = false;
    private boolean           freeAgencyDone = false;

    private boolean initialized = false;

    public GameSession(NameGenerator nameGenerator, TeamGenerator teamGenerator) {
        this.nameGenerator = nameGenerator;
        this.teamGenerator = teamGenerator;
    }

    public void addGamePhaseListener(Runnable onChange) {
        this.onChange = onChange;
    }

    // ── Initialization ───────────────────────────────────────────────

    public void initNewGame(Team selectedTeam) {
        Random random = new Random();
        this.league      = new LeagueGenerator(teamGenerator).generate("Hockey Manager League");

        // Replace one random team with the selected team data (it was already generated)
        // Actually selectedTeam comes from the league, so we just store reference
        this.userTeam    = selectedTeam;
        this.newsService = new NewsService();
        this.contractEngine = new ContractEngine(random);
        this.salaryCapManager = new SalaryCapManager();

        startNewSeason();
        this.initialized = true;
    }

    public void initWithLeague(League league, Team selectedTeam) {
        Random random = new Random();
        this.league       = league;
        this.userTeam     = selectedTeam;
        this.newsService  = new NewsService();
        this.contractEngine = new ContractEngine(random);
        this.salaryCapManager = new SalaryCapManager();
        startNewSeason();
        this.initialized  = true;
    }

    private void startNewSeason() {
        GameDate seasonStart = new GameDate(year, 10, 4);
        List<ScheduledGame> schedule = new ScheduleGenerator()
                .generate(league.getTeams(), seasonStart);
        this.seasonManager = new SeasonManager(
                league, new GameSimulator(), schedule, seasonStart);
        this.phase = SeasonPhase.REGULAR_SEASON;
        if(this.onChange != null) {
            this.onChange.run();
        }
    }

    // ── Simulation ───────────────────────────────────────────────────

    public List<GameResult> simNextGame() {
        // Advance to the date of the user's next unplayed game, sim everything on that date
        List<GameResult> results = seasonManager.simUpToUserGame(userTeam);
        postSimNews(results);
        checkSeasonEnd();
        return results;
    }

    public List<GameResult> simDay() {
        List<GameResult> results = seasonManager.simDay();
        postSimNews(results);
        checkSeasonEnd();
        return results;
    }

    public List<GameResult> simWeek() {
        List<GameResult> results = seasonManager.simWeek();
        postSimNews(results);
        checkSeasonEnd();
        return results;
    }

    public List<GameResult> simSeason() {
        List<GameResult> results = seasonManager.simSeason();
        postSimNews(results);
        checkSeasonEnd();
        return results;
    }

    private void postSimNews(List<GameResult> results) {
        for (GameResult r : results) {
            String headline = formatResult(r);
            boolean mine = isMyTeamGame(r);
            newsService.addHeadline(
                    seasonManager.getCurrentDate(),
                    headline,
                    mine ? NewsEvent.Category.GAME_RESULT : NewsEvent.Category.LEAGUE_NEWS,
                    mine ? userTeam : null);
        }
    }

    private void checkSeasonEnd() {
        if (seasonManager.isSeasonOver() && phase == SeasonPhase.REGULAR_SEASON) {
            beginPlayoffs();
        }
    }

    // ── Playoffs ─────────────────────────────────────────────────────

    public void beginPlayoffs() {
        phase = SeasonPhase.PLAYOFFS;
        List<Team> standings = league.getStandings();
        lastPlayoffTeams = new ArrayList<>(standings.subList(0, Math.min(16, standings.size())));
        playoffBracket = new PlayoffBracket(lastPlayoffTeams);
        if(this.onChange != null) {
            this.onChange.run();
        }
    }

    /**
     * Sims one game in the user's active series (if not over), then one game
     * in every other active series to keep the round moving in lockstep.
     * Returns results for display: user's game first, then others.
     */
    public List<GameResult> simPlayoffGame() {
        if (playoffBracket == null || playoffBracket.isOver()) return List.of();

        List<GameResult> results = new ArrayList<>();
        List<PlayoffSeries> activeSeries = playoffBracket.getCurrentRoundSeries().stream()
                .filter(s -> !s.isOver())
                .collect(Collectors.toList());

        // Find user's series in this round (null if eliminated or not in round)
        PlayoffSeries userSeries = activeSeries.stream()
                .filter(s -> s.getHigherSeed() == userTeam || s.getLowerSeed() == userTeam)
                .findFirst().orElse(null);

        // Sim user's series first so the result is front and center
        if (userSeries != null) {
            results.add(simSeriesGame(userSeries));
        }

        // Sim one game in each other active series
        for (PlayoffSeries series : activeSeries) {
            if (series == userSeries) continue;
            if (!series.isOver()) {
                results.add(simSeriesGame(series));
            }
        }

        if (playoffBracket.isCurrentRoundOver()) {
            playoffBracket.advanceRound();
        }
        if (playoffBracket.isOver()) {
            Team champ = playoffBracket.getChampion();
            newsService.addHeadline(seasonManager.getCurrentDate(),
                    "🏆 " + champ.getFullName() + " win the Stanley Cup!",
                    NewsEvent.Category.LEAGUE_NEWS, null);
            phase = SeasonPhase.OFFSEASON;
            if (onChange != null) onChange.run();
        }
        return results;
    }

    private GameResult simSeriesGame(PlayoffSeries series) {
        Team home = series.getNextHomeTeam();
        Team away = series.getNextAwayTeam();
        GameResult result = playoffSimulator.simulateGame(home, away);
        series.addGame(result);
        boolean mine = home == userTeam || away == userTeam;
        String headline = String.format("%s %d-%d %s (Game %d — %s)",
                home.getFullName(), result.getHomeScore(),
                result.getAwayScore(), away.getFullName(),
                series.getGameCount(), series.getRoundName());
        newsService.addHeadline(seasonManager.getCurrentDate(), headline,
                mine ? NewsEvent.Category.GAME_RESULT : NewsEvent.Category.LEAGUE_NEWS,
                mine ? userTeam : null);
        if (series.isOver() && series.getWinner() != null) {
            newsService.addHeadline(seasonManager.getCurrentDate(),
                    series.getWinner().getFullName() + " win the series " +
                            series.getHigherSeedWins() + "-" + series.getLowerSeedWins(),
                    NewsEvent.Category.LEAGUE_NEWS, null);
        }
        return result;
    }

    public void simPlayoffRound() {
        if (playoffBracket == null) return;
        // Sim all remaining games in each active series until the round is done
        while (!playoffBracket.isCurrentRoundOver() && !playoffBracket.isOver()) {
            simPlayoffGame();
        }
        if (playoffBracket.isCurrentRoundOver() && !playoffBracket.isOver()) {
            playoffBracket.advanceRound();
        }
    }

    public void simAllPlayoffs() {
        while (playoffBracket != null && !playoffBracket.isOver()) {
            simPlayoffRound();
        }
        phase = SeasonPhase.OFFSEASON;
    }

    // ── Offseason setup ───────────────────────────────────────────────

    public void beginOffseason() {
        phase = SeasonPhase.OFFSEASON;
        Random random = new Random();
        if(this.onChange != null) {
            this.onChange.run();
        }

        // Process retirements / age
        new OffseasonManager(league).processOffseason();
        new DevelopmentEngine(random).processLeagueDevelopment(league);

        // Scouting
        scoutingService = new ScoutingService();
        draftClass = new ProspectGenerator(nameGenerator).generateDraftClass();
        scoutingDone   = false;
        draftDone      = false;
        freeAgencyDone = false;

        // Free agency
        ContractEngine ce = new ContractEngine(random);
        freeAgentPool  = new FreeAgentPool(ce);
        freeAgentPool.collectFromLeague(league);
        freeAgencyEngine = new FreeAgencyEngine(ce, salaryCapManager, random);
        contractEngine   = ce;
    }

    public void finalizeScouting() {
        scoutingService.applyScoutingToClass(draftClass);
        scoutingDone = true;
    }

    public void beginDraft() {
        List<Team> allTeams = new ArrayList<>(league.getStandings());
        Collections.reverse(allTeams);
        Set<Team> playoffSet = new HashSet<>(lastPlayoffTeams);
        List<Team> draftOrder = new DraftLottery(new Random()).runLottery(allTeams, playoffSet);
        draftEngine = new DraftEngine(draftOrder, draftClass, userTeam);
    }

    public void completeDraft() {
        draftDone = true;
    }

    public void completeFreeAgency() {
        freeAgencyDone = true;
    }

    public void beginNextSeason() {
        year++;
        startNewSeason();
        // Reset all offseason flags
        scoutingDone   = false;
        draftDone      = false;
        freeAgencyDone = false;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    public int userGamesPlayed() {
        return (int) seasonManager.getSchedule().stream()
                .filter(g -> g.isPlayed()
                        && (g.getHomeTeam() == userTeam || g.getAwayTeam() == userTeam))
                .count();
    }

    public int userTotalGames() {
        return (int) seasonManager.getSchedule().stream()
                .filter(g -> g.getHomeTeam() == userTeam || g.getAwayTeam() == userTeam)
                .count();
    }

    public boolean isMyTeamGame(GameResult r) {
        return r.getHomeTeam() == userTeam || r.getAwayTeam() == userTeam;
    }

    public String formatResult(GameResult r) {
        String base = r.getHomeTeam().getFullName() + " " + r.getHomeScore()
                + " - " + r.getAwayScore() + " " + r.getAwayTeam().getFullName()
                + (r.isOvertime() ? " (OT)" : "");
        if (isMyTeamGame(r)) {
            boolean won = r.getWinner() == userTeam;
            return (won ? "✓ " : "✗ ") + base;
        }
        return base;
    }

    public boolean isUserEliminated() {
        if (playoffBracket == null) return true;
        if (!lastPlayoffTeams.contains(userTeam)) return true;
        for (List<PlayoffSeries> round : playoffBracket.getRounds()) {
            for (PlayoffSeries s : round) {
                if (s.isOver() && s.getLoser() == userTeam) return true;
            }
        }
        return false;
    }

    public String getNextGameDisplay() {
        return seasonManager.getSchedule().stream()
                .filter(g -> !g.isPlayed()
                        && (g.getHomeTeam() == userTeam || g.getAwayTeam() == userTeam))
                .findFirst()
                .map(g -> {
                    boolean home = g.getHomeTeam() == userTeam;
                    String opp = home ? g.getAwayTeam().getFullName()
                            : g.getHomeTeam().getFullName();
                    return g.getDate() + "  " + (home ? "vs " : "@ ") + opp;
                })
                .orElse("No upcoming games");
    }

    // ── Getters ────────────────────────────────────────────────────────

    public boolean isInitialized()          { return initialized; }
    public League getLeague()               { return league; }
    public Team getUserTeam()               { return userTeam; }
    public SeasonManager getSeasonManager() { return seasonManager; }
    public NewsService getNewsService()     { return newsService; }
    public SeasonPhase getPhase()           { return phase; }
    public int getYear()                    { return year; }
    public PlayoffBracket getPlayoffBracket()  { return playoffBracket; }
    public List<Team> getLastPlayoffTeams() { return lastPlayoffTeams; }
    public ScoutingService getScoutingService() { return scoutingService; }
    public List<Prospect> getDraftClass()   { return draftClass; }
    public DraftEngine getDraftEngine()     { return draftEngine; }
    public FreeAgentPool getFreeAgentPool() { return freeAgentPool; }
    public FreeAgencyEngine getFreeAgencyEngine() { return freeAgencyEngine; }
    public SalaryCapManager getSalaryCapManager() { return salaryCapManager; }
    public ContractEngine getContractEngine()     { return contractEngine; }
    public boolean isScoutingDone()         { return scoutingDone; }
    public boolean isDraftDone()            { return draftDone; }
    public boolean isFreeAgencyDone()       { return freeAgencyDone; }
}
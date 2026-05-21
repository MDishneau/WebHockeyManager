package com.hockeymanager.backend.model;

import java.util.ArrayList;
import java.util.List;

public class PlayoffBracket {

    private final List<List<PlayoffSeries>> rounds; // rounds.get(0) = R1, etc.
    private int currentRound; // 0-indexed
    private Team champion;

    public PlayoffBracket(List<Team> seededTeams) {
        this.rounds       = new ArrayList<>();
        this.currentRound = 0;
        buildFirstRound(seededTeams);
    }

    // Seeds 1v16, 2v15 ... 8v9
    private void buildFirstRound(List<Team> seeds) {
        List<PlayoffSeries> r1 = new ArrayList<>();
        int n = seeds.size(); // 16
        for (int i = 0; i < n / 2; i++) {
            r1.add(new PlayoffSeries(seeds.get(i), seeds.get(n - 1 - i), 1));
        }
        rounds.add(r1);
    }

    public List<PlayoffSeries> getCurrentRoundSeries() {
        if (currentRound >= rounds.size()) return List.of();
        return rounds.get(currentRound);
    }

    public boolean isCurrentRoundOver() {
        return getCurrentRoundSeries().stream().allMatch(PlayoffSeries::isOver);
    }

    // Advance to next round using winners of current round
    public void advanceRound() {
        if (!isCurrentRoundOver()) return;

        List<PlayoffSeries> current = getCurrentRoundSeries();
        List<Team> winners = new ArrayList<>();
        for (PlayoffSeries s : current) winners.add(s.getWinner());

        currentRound++;

        if (winners.size() == 1) {
            // Championship decided
            champion = winners.get(0);
            return;
        }

        int nextRoundNum = currentRound + 1;
        List<PlayoffSeries> nextRound = new ArrayList<>();
        for (int i = 0; i < winners.size() / 2; i++) {
            nextRound.add(new PlayoffSeries(
                    winners.get(i), winners.get(winners.size() - 1 - i), nextRoundNum));
        }
        rounds.add(nextRound);
    }

    public boolean isOver() {
        return champion != null;
    }

    public Team getChampion()                      { return champion; }
    public List<List<PlayoffSeries>> getRounds()   { return rounds; }
    public int getCurrentRoundIndex()              { return currentRound; }

    public String getCurrentRoundName() {
        if (isOver()) return "Complete";
        List<PlayoffSeries> series = getCurrentRoundSeries();
        if (series.isEmpty()) return "Unknown";
        return series.get(0).getRoundName();
    }
}
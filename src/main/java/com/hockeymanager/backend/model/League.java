package com.hockeymanager.backend.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class League {

    private final String name;
    private final List<Team> teams;

    public League(String name) {
        this.name = name;
        this.teams = new ArrayList<>();
    }

    public void addTeam(Team team) {
        teams.add(team);
    }

    public List<Team> getTeams() {
        return teams;
    }

    public List<Team> getStandings() {
        List<Team> sorted = new ArrayList<>(teams);
        sorted.sort(Comparator.comparingInt(Team::getPoints).reversed());
        return sorted;
    }

    public void printStandings() {
        System.out.println("\n=== " + name + " STANDINGS ===");
        System.out.printf("%-25s %4s %4s %4s %4s%n", "Team", "W", "L", "OTL", "PTS");
        System.out.println("-".repeat(45));
        for (Team team : getStandings()) {
            System.out.printf("%-25s %4d %4d %4d %4d%n",
                    team.getFullName(),
                    team.getWins(),
                    team.getLosses(),
                    team.getOtLosses(),
                    team.getPoints());
        }
    }

    public String getName() {
        return name;
    }
}
package com.hockeymanager.backend.manager;

import com.hockeymanager.backend.model.*;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class OffseasonManager {

    private final League         league;
    private final ContractEngine contractEngine;

    public OffseasonManager(League league) {
        this.league        = league;
        this.contractEngine = new ContractEngine(new Random());
    }

    public void processOffseason() {
        for (Team team : league.getTeams()) {
            // Retire players 40+
            List<Player> retirees = team.getRoster().stream()
                    .filter(p -> p.getAge() >= 40)
                    .collect(Collectors.toList());
            retirees.forEach(r -> {
                team.getRoster().remove(r);
                System.out.println("  " + r.getName() + " has retired.");
            });

            for (Player p : team.getRoster()) {
                p.incrementAge();
                p.resetSeasonStats();
                p.advanceContract();

                // Assign ELC to any player without a contract (generated players)
                if (p.getContract() == null) {
                    assignStarterContract(p, team);
                }
            }

            team.resetRecord();
        }
    }

    private void assignStarterContract(Player p, Team team) {
        ContractType type = contractEngine.determineType(p);
        long salary;
        int  years;

        if (type == ContractType.ENTRY_LEVEL) {
            p.setContract(contractEngine.createELC(3));
        } else {
            salary = contractEngine.calculateMarketValue(p);
            years  = contractEngine.preferredLength(p, true);
            p.setContract(contractEngine.createContract(p, salary, years, true));
        }
    }
}
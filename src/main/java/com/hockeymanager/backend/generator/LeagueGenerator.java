package com.hockeymanager.backend.generator;

import com.hockeymanager.backend.model.*;

import java.util.List;
import java.util.Random;

public class LeagueGenerator {

    // All 32 NHL franchises with market size
    private static final Object[][] NHL_TEAMS = {
            // Atlantic
            {"Boston",        "Bruins",        MarketSize.LARGE},
            {"Buffalo",       "Sabres",        MarketSize.SMALL},
            {"Detroit",       "Red Wings",     MarketSize.LARGE},
            {"Florida",       "Panthers",      MarketSize.MEDIUM},
            {"Montreal",      "Canadiens",     MarketSize.LARGE},
            {"Ottawa",        "Senators",      MarketSize.SMALL},
            {"Tampa Bay",     "Lightning",     MarketSize.MEDIUM},
            {"Toronto",       "Maple Leafs",   MarketSize.LARGE},
            // Metropolitan
            {"Carolina",      "Hurricanes",    MarketSize.MEDIUM},
            {"Columbus",      "Blue Jackets",  MarketSize.SMALL},
            {"New Jersey",    "Devils",        MarketSize.LARGE},
            {"New York",      "Islanders",     MarketSize.LARGE},
            {"New York",      "Rangers",       MarketSize.LARGE},
            {"Philadelphia",  "Flyers",        MarketSize.LARGE},
            {"Pittsburgh",    "Penguins",      MarketSize.LARGE},
            {"Washington",    "Capitals",      MarketSize.LARGE},
            // Central
            {"Arizona",       "Coyotes",       MarketSize.SMALL},
            {"Chicago",       "Blackhawks",    MarketSize.LARGE},
            {"Colorado",      "Avalanche",     MarketSize.MEDIUM},
            {"Dallas",        "Stars",         MarketSize.MEDIUM},
            {"Minnesota",     "Wild",          MarketSize.MEDIUM},
            {"Nashville",     "Predators",     MarketSize.SMALL},
            {"St. Louis",     "Blues",         MarketSize.MEDIUM},
            {"Winnipeg",      "Jets",          MarketSize.SMALL},
            // Pacific
            {"Anaheim",       "Ducks",         MarketSize.MEDIUM},
            {"Calgary",       "Flames",        MarketSize.MEDIUM},
            {"Edmonton",      "Oilers",        MarketSize.MEDIUM},
            {"Los Angeles",   "Kings",         MarketSize.LARGE},
            {"San Jose",      "Sharks",        MarketSize.MEDIUM},
            {"Seattle",       "Kraken",        MarketSize.MEDIUM},
            {"Vancouver",     "Canucks",       MarketSize.MEDIUM},
            {"Vegas",         "Golden Knights",MarketSize.MEDIUM},
    };

    private final TeamGenerator teamGenerator;

    public LeagueGenerator(Random random) {
        this.teamGenerator = new TeamGenerator(random);
    }

    public League generate(String leagueName) {
        League league = new League(leagueName);

        for (Object[] teamData : NHL_TEAMS) {
            String city       = (String)     teamData[0];
            String name       = (String)     teamData[1];
            MarketSize market = (MarketSize) teamData[2];

            Team team = new Team(city, name, market);
            teamGenerator.populateRoster(team);
            league.addTeam(team);
        }

        return league;
    }
}
package com.hockeymanager.ui.views;

import com.hockeymanager.backend.generator.LeagueGenerator;
import com.hockeymanager.backend.manager.SeasonManager;
import com.hockeymanager.backend.model.League;
import com.hockeymanager.ui.components.GameSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("")
public class DashboardView extends VerticalLayout {
    private LeagueGenerator leagueGenerator;
    private GameSession gameSession;
    public DashboardView(LeagueGenerator leagueGenerator, GameSession gameSession) {
        this.leagueGenerator = leagueGenerator;
        this.gameSession = gameSession;

        TextField leagueName = new TextField("Enter league name:");
        Button submitButton = new Button("Submit");


        submitButton.addClickListener(e -> {
            gameSession.league = leagueGenerator.generate(leagueName.getValue());
            UI.getCurrent().navigate("teamselection");
        });

        add(leagueName, submitButton);
    }
}

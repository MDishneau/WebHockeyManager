package com.hockeymanager.ui.views;

import com.hockeymanager.backend.generator.LeagueGenerator;
import com.hockeymanager.backend.generator.TeamGenerator;
import com.hockeymanager.backend.model.League;
import com.hockeymanager.backend.model.MarketSize;
import com.hockeymanager.backend.model.Team;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.layouts.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;
import java.util.Random;

@Route(value = "", layout = MainLayout.class)
@PageTitle("New Game — Hockey Manager")
@AnonymousAllowed
public class TeamSelectionView extends VerticalLayout {

    private final GameSession session;
    private final TeamGenerator teamGenerator;
    private League previewLeague;

    public TeamSelectionView(GameSession session, TeamGenerator teamGenerator) {
        this.session = session;

        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        // Generate a preview league just for team selection
        previewLeague = new LeagueGenerator(teamGenerator).generate("Hockey Manager League");

        add(buildHeader());
        add(buildTeamGrid());
        this.teamGenerator = teamGenerator;
    }

    private Div buildHeader() {
        Div header = new Div();
        header.addClassNames(LumoUtility.TextAlignment.CENTER, LumoUtility.Margin.Bottom.LARGE);

        H1 title = new H1(" Hockey Manager");
        title.addClassNames(LumoUtility.FontSize.XXXLARGE);

        H2 subtitle = new H2("Select Your Team");
        subtitle.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.TextColor.SECONDARY);

        Paragraph hint = new Paragraph("Click a team card to choose your franchise");
        hint.addClassNames(LumoUtility.TextColor.SECONDARY);

        header.add(title, subtitle, hint);
        return header;
    }

    private FlexLayout buildTeamGrid() {
        FlexLayout grid = new FlexLayout();
        grid.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        grid.setJustifyContentMode(JustifyContentMode.CENTER);
        grid.getStyle().set("gap", "12px");
        grid.setMaxWidth("1200px");

        List<Team> teams = previewLeague.getTeams();
        for (Team team : teams) {
            grid.add(buildTeamCard(team));
        }

        return grid;
    }

    private Div buildTeamCard(Team team) {
        Div card = new Div();
        card.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.BASE,
                LumoUtility.BoxSizing.BORDER
        );
        card.getStyle()
                .set("width", "220px")
                .set("cursor", "pointer")
                .set("transition", "all 0.15s ease")
                .set("border-color", "var(--lumo-contrast-20pct)");

        card.getElement().addEventListener("mouseenter", e ->
                card.getStyle().set("border-color", "var(--lumo-primary-color)")
                        .set("background", "var(--lumo-primary-color-10pct)")
        );
        card.getElement().addEventListener("mouseleave", e ->
                card.getStyle().set("border-color", "var(--lumo-contrast-20pct)")
                        .set("background", "var(--lumo-base-color)")
        );

        Span city = new Span(team.getCity());
        city.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        H3 name = new H3(team.getName());
        name.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Span market = new Span(marketBadgeText(team.getMarketSize()));
        market.addClassNames(LumoUtility.FontSize.XSMALL);
        market.getStyle()
                .set("padding", "2px 8px")
                .set("border-radius", "12px")
                .set("background", marketBadgeColor(team.getMarketSize()))
                .set("color", "white");

        Button selectBtn = new Button("Select Team");
        selectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        selectBtn.setWidthFull();
        selectBtn.addClickListener(e -> confirmSelection(team));

        VerticalLayout content = new VerticalLayout(city, name, market, selectBtn);
        content.setSpacing(false);
        content.setPadding(false);
        content.getStyle().set("gap", "6px");

        card.add(content);
        card.addClickListener(e -> confirmSelection(team));
        return card;
    }

    private void confirmSelection(Team team) {
        session.initWithLeague(previewLeague, team);
        Notification.show("You selected: " + team.getFullName() + " — Good luck, GM!",
                        3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate(DashboardView.class);
    }

    private String marketBadgeText(MarketSize size) {
        return switch (size) {
            case LARGE  -> "Large Market";
            case MEDIUM -> "Mid Market";
            case SMALL  -> "Small Market";
        };
    }

    private String marketBadgeColor(MarketSize size) {
        return switch (size) {
            case LARGE  -> "#2e7d32";
            case MEDIUM -> "#f5c62b";
            case SMALL  -> "#f02c2c";
        };
    }
}
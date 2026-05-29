package com.hockeymanager.ui.views;

import com.hockeymanager.backend.engine.GameResult;
import com.hockeymanager.backend.manager.SeasonManager;
import com.hockeymanager.backend.model.*;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.layouts.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard — Hockey Manager")
@AnonymousAllowed
public class DashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final GameSession session;

    // Dynamic stat sections we refresh after sims
    private Div statsCard;
    private Div resultsLog;

    public DashboardView(GameSession session) {
        this.session = session;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!session.isInitialized()) {
            event.rerouteTo(TeamSelectionView.class);
            return;
        }
        if (session.getPhase() == SeasonPhase.PLAYOFFS) {
            event.rerouteTo(PlayoffView.class);
            return;
        }
        if (session.getPhase() == SeasonPhase.OFFSEASON) {
            event.rerouteTo(OffseasonView.class);
            return;
        }
        buildUI();
    }

    private void buildUI() {
        removeAll();
        Team team = session.getUserTeam();
        SeasonManager sm = session.getSeasonManager();

        // ── Header ──────────────────────────────────────────────────
        H2 heading = new H2("GM Dashboard — " + sm.getCurrentDate());
        heading.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        // ── Top cards row ────────────────────────────────────────────
        statsCard = buildStatsCard(team, sm);

        HorizontalLayout topRow = new HorizontalLayout(statsCard);
        topRow.setWidthFull();
        topRow.setSpacing(true);

        // ── Sim controls ─────────────────────────────────────────────
        Div simSection = buildSimSection();

        // ── Results log ───────────────────────────────────────────────
        resultsLog = new Div();
        resultsLog.setWidthFull();
        resultsLog.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        add(heading, topRow, simSection, resultsLog);
    }

    private Div buildStatsCard(Team team, SeasonManager sm) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM, LumoUtility.Background.BASE);
        card.setWidthFull();

        H3 teamName = new H3(team.getFullName());
        teamName.addClassNames(LumoUtility.Margin.Bottom.XSMALL);

        Span record = new Span("Record: " + team.getWins() + "-" + team.getLosses()
                + "-" + team.getOtLosses() + "   PTS: " + team.getPoints());
        record.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        Span nextGame = new Span("Next game: " + session.getNextGameDisplay());
        nextGame.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        Span progress = new Span("Season: " + sm.gamesPlayed() + " / " + sm.totalGames() + " games played");
        progress.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        card.add(teamName, record, new Div(nextGame), new Div(progress));
        return card;
    }

    private Div buildSimSection() {
        Div section = new Div();
        section.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM, LumoUtility.Background.BASE);
        section.setWidthFull();

        H3 simTitle = new H3("Simulate");
        simTitle.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        Button simNext = new Button("▶ Sim Next Game");
        simNext.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        simNext.addClickListener(e -> handleSimNext());

        Button simDay = new Button(" Sim Today");
        simDay.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        simDay.addClickListener(e -> handleSimDay());

        Button simWeek = new Button(" Sim Next Week");
        simWeek.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        simWeek.addClickListener(e -> handleSimWeek());

        Button simSeason = new Button(" Sim Entire Season");
        simSeason.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        simSeason.addClickListener(e -> confirmSimSeason());

        FlexLayout buttons = new FlexLayout(simNext, simDay, simWeek, simSeason);
        buttons.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        // Apply uniform spacing using Lumo theme variables
        buttons.getStyle().set("gap", "var(--lumo-space-m)");

        section.add(simTitle, buttons);
        return section;
    }

    // ── Sim handlers ──────────────────────────────────────────────────

    private void handleSimNext() {
        List<GameResult> results = session.simNextGame();
        showResults(results, "Next Game Result");
        refreshStats();
        checkPhaseTransition();
    }

    private void handleSimDay() {
        List<GameResult> results = session.simDay();
        showResults(results, "Today's Results");
        refreshStats();
        checkPhaseTransition();
    }

    private void handleSimWeek() {
        List<GameResult> results = session.simWeek();
        showResults(results, "This Week's Results");
        refreshStats();
        checkPhaseTransition();
    }

    private void confirmSimSeason() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Simulate Full Season?");
        Paragraph msg = new Paragraph("Sim all remaining games to end of season. Are you sure?");
        Button confirm = new Button("Sim Season", e -> {
            dialog.close();
            List<GameResult> results = session.simSeason();
            showResults(results, "Season Results (" + results.size() + " games)");
            refreshStats();
            checkPhaseTransition();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, confirm);
        dialog.add(msg);
        dialog.open();
    }

    private void checkPhaseTransition() {
        if (session.getPhase() == SeasonPhase.PLAYOFFS) {
            Notification n = Notification.show(
                    " Regular season over! Playoffs begin!", 4000,
                    Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate(PlayoffView.class);
        }
    }



    private void showResults(List<GameResult> results, String title) {
        resultsLog.removeAll();

        if (results.isEmpty()) {
            resultsLog.add(new Span("No games played — advanced to next game day."));
            return;
        }

        H3 header = new H3(title);
        header.addClassNames(LumoUtility.Margin.Bottom.SMALL);
        resultsLog.add(header);

        for (GameResult r : results) {
            Div row = new Div();
            boolean mine = session.isMyTeamGame(r);
            String text = session.formatResult(r);

            Span label = new Span((mine ? "► " : "   ") + text);
            if (mine) {
                boolean won = r.getWinner() == session.getUserTeam();
                label.getStyle().set("color", won ? "var(--lumo-success-color)"
                        : "var(--lumo-error-color)");
                label.addClassNames(LumoUtility.FontWeight.BOLD);
            }
            row.add(label);
            resultsLog.add(row);
        }
    }

    private void refreshStats() {
        // Rebuild the stats card in-place
        Team team = session.getUserTeam();
        SeasonManager sm = session.getSeasonManager();
        Div newCard = buildStatsCard(team, sm);
        newCard.getStyle().set("width", statsCard.getStyle().get("width"));
        statsCard.getParent().ifPresent(parent -> {
            int idx = -1;
            var children = parent.getChildren().toList();
            for (int i = 0; i < children.size(); i++) {
                if (children.get(i) == statsCard) { idx = i; break; }
            }
        });
        // Simplest refresh: just update content
        statsCard.removeAll();
        H3 teamName = new H3(team.getFullName());
        teamName.addClassNames(LumoUtility.Margin.Bottom.XSMALL);
        Span record = new Span("Record: " + team.getWins() + "-" + team.getLosses()
                + "-" + team.getOtLosses() + "   PTS: " + team.getPoints());
        record.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        Span nextGame = new Span("Next game: " + session.getNextGameDisplay());
        nextGame.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        Span progress = new Span("Season: " + sm.gamesPlayed() + " / " + sm.totalGames() + " games played");
        progress.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        statsCard.add(teamName, record, new Div(nextGame), new Div(progress));
    }
}
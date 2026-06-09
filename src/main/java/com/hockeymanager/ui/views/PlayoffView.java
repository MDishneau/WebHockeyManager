package com.hockeymanager.ui.views;

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
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;
import java.util.stream.Collectors;

@Route(value = "playoffs", layout = MainLayout.class)
@PageTitle("Playoffs — Hockey Manager")
@AnonymousAllowed
public class PlayoffView extends VerticalLayout implements BeforeEnterObserver {

    private final GameSession session;
    private Div resultsLog;

    public PlayoffView(GameSession session) {
        this.session = session;
        setSizeFull();
        setPadding(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!session.isInitialized()) {
            event.rerouteTo(TeamSelectionView.class);
            return;
        }
        // Trigger playoff start if we just came from regular season end
        if (session.getPhase() == SeasonPhase.REGULAR_SEASON
                && session.getSeasonManager().isSeasonOver()) {
            session.beginPlayoffs();
        }
        if (session.getPhase() == SeasonPhase.OFFSEASON) {
            event.rerouteTo(OffseasonView.class);
            return;
        }
        buildUI();
    }

    private void buildUI() {
        removeAll();

        PlayoffBracket bracket = session.getPlayoffBracket();

        H2 title = new H2("🏒 Playoffs — " + session.getYear() + "-" + (session.getYear() + 1));
        add(title);

        if (bracket == null) {
            add(new Span("Playoffs have not started yet."));
            return;
        }

        if (bracket.isOver()) {
            Team champ = bracket.getChampion();
            Div champBanner = new Div();
            champBanner.addClassNames(LumoUtility.Padding.LARGE, LumoUtility.BorderRadius.MEDIUM,
                    LumoUtility.TextAlignment.CENTER);
            champBanner.getStyle().set("background", "var(--lumo-warning-color-10pct)");
            H2 champMsg = new H2("🏆 " + champ.getFullName() + " win the Stanley Cup!");
            champMsg.getStyle().set("color", "var(--lumo-warning-text-color)");
            Button goOffseason = new Button("Continue to Offseason →");
            goOffseason.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            goOffseason.addClickListener(e -> {
                session.beginOffseason();
                UI.getCurrent().navigate(OffseasonView.class);
            });
            champBanner.add(champMsg, goOffseason);
            add(champBanner);
            add(buildBracketDisplay(bracket));
            return;
        }

        // ── Sim controls ──────────────────────────────────────────────
        boolean eliminated = session.isUserEliminated();
        Div simSection = buildSimSection(eliminated, bracket);
        add(simSection);

        // ── Results log ───────────────────────────────────────────────
        resultsLog = new Div();
        resultsLog.setWidthFull();
        add(resultsLog);

        // ── Bracket display ───────────────────────────────────────────
        add(buildBracketDisplay(bracket));
    }

    private Div buildSimSection(boolean eliminated, PlayoffBracket bracket) {
        Div section = new Div();
        section.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM, LumoUtility.Background.BASE);
        section.setWidthFull();

        H3 roundTitle = new H3("Current Round: " + bracket.getCurrentRoundName());
        roundTitle.addClassNames(LumoUtility.Margin.Bottom.SMALL);
        section.add(roundTitle);

        if (eliminated) {
            Span msg = new Span("Your team has been eliminated.");
            msg.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontWeight.BOLD);
            section.add(msg);
        }

        Button simGame = new Button("▶ Sim Next Game");
        simGame.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        simGame.addClickListener(e -> {
            List<com.hockeymanager.backend.engine.GameResult> results = session.simPlayoffGame();
            refreshView();
            showPlayoffResults(results);
        });

        Button simRound = new Button("⏭ Sim Rest of Round");
        simRound.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        simRound.addClickListener(e -> {
            session.simPlayoffRound();
            refreshView();
        });

        Button simAll = new Button("⏩ Sim All Playoffs");
        simAll.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        simAll.addClickListener(e -> confirmSimAll());

        HorizontalLayout buttons = new HorizontalLayout(simGame, simRound, simAll);
        buttons.setSpacing(true);
        section.add(buttons);

        return section;
    }

    private Div buildBracketDisplay(PlayoffBracket bracket) {
        Div bracketDiv = new Div();
        bracketDiv.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        bracketDiv.setWidthFull();

        for (List<PlayoffSeries> round : bracket.getRounds()) {
            if (round.isEmpty()) continue;

            H3 roundName = new H3(round.get(0).getRoundName().toUpperCase());
            roundName.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Margin.Bottom.SMALL);
            bracketDiv.add(roundName);

            for (PlayoffSeries series : round) {
                bracketDiv.add(buildSeriesRow(series));
            }
        }

        return bracketDiv;
    }

    private Div buildSeriesRow(PlayoffSeries series) {
        Div row = new Div();
        row.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.SMALL, LumoUtility.Margin.Bottom.XSMALL);
        row.setWidthFull();
        row.getStyle().set("max-width", "600px");

        Team myTeam = session.getUserTeam();
        boolean isMySereis = series.getHigherSeed() == myTeam || series.getLowerSeed() == myTeam;
        if (isMySereis) {
            row.getStyle().set("border-color", "var(--lumo-primary-color)")
                    .set("background", "var(--lumo-primary-color-10pct)");
        }

        HorizontalLayout content = new HorizontalLayout();
        content.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        content.setWidthFull();

        Span higher = new Span(series.getHigherSeed().getFullName());
        higher.getStyle().set("flex", "1");
        if (series.isOver() && series.getWinner() == series.getHigherSeed()) {
            higher.addClassNames(LumoUtility.FontWeight.BOLD);
        } else if (series.isOver()) {
            higher.addClassNames(LumoUtility.TextColor.SECONDARY);
        }

        Span score = new Span(series.getHigherSeedWins() + " - " + series.getLowerSeedWins());
        score.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.LARGE);
        score.getStyle().set("min-width", "60px").set("text-align", "center");

        Span lower = new Span(series.getLowerSeed().getFullName());
        lower.getStyle().set("flex", "1").set("text-align", "right");
        if (series.isOver() && series.getWinner() == series.getLowerSeed()) {
            lower.addClassNames(LumoUtility.FontWeight.BOLD);
        } else if (series.isOver()) {
            lower.addClassNames(LumoUtility.TextColor.SECONDARY);
        }

        if (series.isOver()) {
            Span winner = new Span(" ✓");
            winner.getStyle().set("color", "var(--lumo-success-color)");
            if (series.getWinner() == series.getHigherSeed()) higher.add(winner);
            else lower.add(winner);
        }

        content.add(higher, score, lower);
        row.add(content);

        if (series.isOver()) {
            Span result = new Span(series.getWinner().getFullName() + " win "
                    + series.getHigherSeedWins() + "-" + series.getLowerSeedWins());
            result.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
            row.add(result);
        }

        return row;
    }

    private void confirmSimAll() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Simulate All Playoffs?");
        Paragraph msg = new Paragraph("Simulate all remaining playoff games at once?");
        Button confirm = new Button("Sim All", e -> {
            dialog.close();
            session.simAllPlayoffs();
            refreshView();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, confirm);
        dialog.add(msg);
        dialog.open();
    }

    private void showPlayoffResults(List<com.hockeymanager.backend.engine.GameResult> results) {
        if (resultsLog == null || results.isEmpty()) return;
        resultsLog.removeAll();

        List<com.hockeymanager.backend.engine.GameResult> myGames = results.stream()
                .filter(session::isMyTeamGame).collect(Collectors.toList());
        List<com.hockeymanager.backend.engine.GameResult> otherGames = results.stream()
                .filter(r -> !session.isMyTeamGame(r)).collect(Collectors.toList());

        for (com.hockeymanager.backend.engine.GameResult r : myGames) {
            Div row = new Div();
            row.addClassNames(LumoUtility.Margin.Bottom.XSMALL);
            boolean won = r.getWinner() == session.getUserTeam();
            Span label = new Span("► " + session.formatResult(r));
            label.getStyle().set("color", won ? "var(--lumo-success-color)" : "var(--lumo-error-color)");
            label.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
            row.add(label);
            resultsLog.add(row);
        }

        if (!otherGames.isEmpty()) {
            Details leagueResults = new Details();
            leagueResults.setSummaryText("Other series results (" + otherGames.size() + ")");
            leagueResults.setOpened(false);
            VerticalLayout inner = new VerticalLayout();
            inner.setPadding(false);
            inner.setSpacing(false);
            for (com.hockeymanager.backend.engine.GameResult r : otherGames) {
                Span s = new Span(session.formatResult(r));
                s.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
                inner.add(s);
            }
            leagueResults.add(inner);
            resultsLog.add(leagueResults);
        }
    }

    private void refreshView() {
        if (session.getPhase() == SeasonPhase.OFFSEASON) {
            Team champ = session.getPlayoffBracket().getChampion();
            if (champ != null) {
                Notification n = Notification.show(
                        "🏆 " + champ.getFullName() + " win the Stanley Cup!",
                        5000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        }
        buildUI();
    }
}
package com.hockeymanager.ui.views;

import com.hockeymanager.backend.engine.GameResult;
import com.hockeymanager.backend.manager.SeasonManager;
import com.hockeymanager.backend.model.*;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.layouts.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;
import java.util.stream.Collectors;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard — Hockey Manager")
@AnonymousAllowed
public class DashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final GameSession session;

    // Sections rebuilt after each sim
    private Div heroSection;
    private Div lowerSection;
    private Div resultsArea;

    public DashboardView(GameSession session) {
        this.session = session;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
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
        getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("min-height", "100vh");

        add(buildTeamBanner());

        heroSection = buildHeroSection();
        add(heroSection);

        resultsArea = new Div();
        resultsArea.setWidthFull();
        resultsArea.getStyle().set("padding", "0 var(--lumo-space-m)");
        add(resultsArea);

        lowerSection = buildLowerSection();
        add(lowerSection);
    }

    // ── Team banner ───────────────────────────────────────────────────

    private Div buildTeamBanner() {
        Div banner = new Div();
        banner.setWidthFull();
        banner.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-bottom", "2px solid var(--lumo-primary-color)")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-m)");

        Team team = session.getUserTeam();
        SeasonManager sm = session.getSeasonManager();

        // Left: team name + season label
        Div left = new Div();
        H2 teamName = new H2(team.getFullName());
        teamName.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("color", "var(--lumo-primary-text-color)");
        Span season = new Span(session.getYear() + "–" + (session.getYear() + 1) + " Regular Season");
        season.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        left.add(teamName, new Div(season));

        // Right: stat pills
        Div right = new Div();
        right.getStyle().set("display", "flex").set("gap", "var(--lumo-space-l)").set("align-items", "center");

        right.add(statPill("RECORD",
                team.getWins() + "–" + team.getLosses() + "–" + team.getOtLosses(), false));
        right.add(statPill("PTS", String.valueOf(team.getPoints()), false));
        right.add(statPill("GAMES", session.userGamesPlayed() + " / " + session.userTotalGames(), false));

        int rank = getRank();
        boolean inPlayoffs = rank <= 16;
        right.add(statPill("RANK", "#" + rank + (inPlayoffs ? " 🏒" : ""), !inPlayoffs));

        long capSpace = session.getSalaryCapManager().getCapSpace(team);
        boolean overCap = session.getSalaryCapManager().isOverCap(team);
        right.add(statPill("CAP SPACE",
                String.format("$%.1fM", capSpace / 1_000_000.0), overCap));

        banner.add(left, right);
        return banner;
    }

    private Div statPill(String label, String value, boolean warning) {
        Div pill = new Div();
        pill.getStyle()
                .set("text-align", "center")
                .set("min-width", "70px");

        Span val = new Span(value);
        val.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "700")
                .set("color", warning ? "var(--lumo-error-color)" : "var(--lumo-primary-text-color)");

        Span lbl = new Span(label);
        lbl.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("letter-spacing", "0.08em");

        pill.add(val, lbl);
        return pill;
    }

    // ── Hero section: next game + sim controls ────────────────────────

    private Div buildHeroSection() {
        Div hero = new Div();
        hero.setWidthFull();
        hero.getStyle()
                .set("padding", "var(--lumo-space-l)")
                .set("display", "flex")
                .set("gap", "var(--lumo-space-l)")
                .set("flex-wrap", "wrap");

        hero.add(buildNextGameCard());
        hero.add(buildSimCard());

        return hero;
    }

    private Div buildNextGameCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "var(--lumo-space-l)")
                .set("flex", "1")
                .set("min-width", "280px");

        Span eyebrow = new Span("NEXT GAME");
        eyebrow.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("letter-spacing", "0.1em")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "700");

        Team myTeam = session.getUserTeam();
        List<ScheduledGame> schedule = session.getSeasonManager().getSchedule();

        ScheduledGame nextGame = schedule.stream()
                .filter(g -> !g.isPlayed()
                        && (g.getHomeTeam() == myTeam || g.getAwayTeam() == myTeam))
                .findFirst().orElse(null);

        if (nextGame == null) {
            card.add(eyebrow, new H3("Season complete"));
            return card;
        }

        boolean isHome = nextGame.getHomeTeam() == myTeam;
        Team opponent = isHome ? nextGame.getAwayTeam() : nextGame.getHomeTeam();

        Span dateLabel = new Span(nextGame.getDate().toString()
                + "  ·  " + (isHome ? "HOME" : "AWAY"));
        dateLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "var(--lumo-space-xs)")
                .set("display", "block");

        // Matchup display
        Div matchup = new Div();
        matchup.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("margin", "var(--lumo-space-m) 0")
                .set("gap", "var(--lumo-space-m)");

        Div myTeamBlock = teamBlock(myTeam, true);
        Div vsBlock = new Div();
        Span vs = new Span("VS");
        vs.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "700");
        vsBlock.add(vs);

        Div oppBlock = teamBlock(opponent, false);
        matchup.add(myTeamBlock, vsBlock, oppBlock);

        // Opponent record
        Span oppRecord = new Span("Opp: " + opponent.getWins() + "–"
                + opponent.getLosses() + "–" + opponent.getOtLosses()
                + "  PTS: " + opponent.getPoints());
        oppRecord.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        card.add(eyebrow, dateLabel, matchup, oppRecord);
        return card;
    }

    private Div teamBlock(Team team, boolean isUser) {
        Div block = new Div();
        block.getStyle().set("text-align", isUser ? "left" : "right");

        Span city = new Span(team.getCity());
        city.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span name = new Span(team.getName());
        name.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "700")
                .set("color", isUser ? "var(--lumo-primary-color)" : "var(--lumo-primary-text-color)");

        block.add(city, name);
        return block;
    }

    private Div buildSimCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "var(--lumo-space-l)")
                .set("flex", "1")
                .set("min-width", "280px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");

        Span eyebrow = new Span("SIMULATE");
        eyebrow.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("letter-spacing", "0.1em")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "700");
        card.add(eyebrow);

        Button simNext = new Button("▶  Sim Next Game");
        simNext.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        simNext.setWidthFull();
        simNext.addClickListener(e -> handleSim(() -> session.simNextGame(), "Next Game"));

        Button simWeek = new Button("⏭  Sim Next Week");
        simWeek.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        simWeek.setWidthFull();
        simWeek.addClickListener(e -> handleSim(() -> session.simWeek(), "This Week"));

        Button simMonth = new Button("⏩  Sim 2 Weeks");
        simMonth.setWidthFull();
        simMonth.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        simMonth.addClickListener(e -> {
            List<GameResult> r1 = session.simWeek();
            List<GameResult> r2 = session.simWeek();
            r1.addAll(r2);
            refreshDashboard();
            showResults(r1, "2 Weeks");
            checkPhaseTransition();
        });

        Button simSeason = new Button("⚡  Sim Rest of Season");
        simSeason.setWidthFull();
        simSeason.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        simSeason.addClickListener(e -> confirmSimSeason());

        card.add(simNext, simWeek, simMonth, simSeason);
        return card;
    }

    // ── Lower section: results | standings | news ─────────────────────

    private Div buildLowerSection() {
        Div lower = new Div();
        lower.setWidthFull();
        lower.getStyle()
                .set("display", "flex")
                .set("gap", "var(--lumo-space-l)")
                .set("padding", "0 var(--lumo-space-l) var(--lumo-space-l)")
                .set("flex-wrap", "wrap")
                .set("align-items", "flex-start");

        lower.add(buildRecentResultsCard());
        lower.add(buildStandingsCard());
        lower.add(buildNewsCard());

        return lower;
    }

    private Div buildRecentResultsCard() {
        Div card = sectionCard("RECENT RESULTS", "flex: 1; min-width: 220px; max-width: 340px;");

        Team myTeam = session.getUserTeam();
        List<ScheduledGame> played = session.getSeasonManager().getSchedule().stream()
                .filter(g -> g.isPlayed()
                        && (g.getHomeTeam() == myTeam || g.getAwayTeam() == myTeam))
                .collect(Collectors.toList());

        if (played.isEmpty()) {
            Span empty = new Span("No games played yet.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");
            card.add(empty);
            return card;
        }

        List<ScheduledGame> last8 = played.subList(Math.max(0, played.size() - 8), played.size());
        // Most recent first
        for (int i = last8.size() - 1; i >= 0; i--) {
            card.add(buildResultRow(last8.get(i), myTeam));
        }

        return card;
    }

    private Div buildResultRow(ScheduledGame game, Team myTeam) {
        GameResult r = game.getResult();
        boolean isHome = game.getHomeTeam() == myTeam;
        int myScore  = isHome ? r.getHomeScore() : r.getAwayScore();
        int oppScore = isHome ? r.getAwayScore() : r.getHomeScore();
        Team opp = isHome ? game.getAwayTeam() : game.getHomeTeam();

        boolean won = myScore > oppScore;
        String resultLabel = won ? "W" : (r.isOvertime() ? "OTL" : "L");

        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("padding", "var(--lumo-space-xs) 0")
                .set("border-bottom", "1px solid var(--lumo-contrast-5pct)");

        // Result badge
        Span badge = new Span(resultLabel);
        badge.getStyle()
                .set("font-weight", "700")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("min-width", "28px")
                .set("color", won ? "var(--lumo-success-color)" : "var(--lumo-error-color)");

        Span oppName = new Span((isHome ? "vs " : "@ ") + opp.getName());
        oppName.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("flex", "1")
                .set("padding", "0 var(--lumo-space-s)");

        Span score = new Span(myScore + "–" + oppScore + (r.isOvertime() ? " OT" : ""));
        score.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)");

        row.add(badge, oppName, score);
        return row;
    }

    private Div buildStandingsCard() {
        Div card = sectionCard("STANDINGS", "flex: 1; min-width: 260px; max-width: 380px;");

        List<Team> standings = session.getLeague().getStandings();
        Team myTeam = session.getUserTeam();
        int myRank = getRank();

        // Show teams around the user: top 3 + teams around user (±2) + cutline context
        int start = Math.max(0, myRank - 3); // 2 above user
        int end   = Math.min(standings.size(), myRank + 2); // 2 below user
        // Always include top 3
        start = Math.min(start, 3);

        // Header row
        Div header = new Div();
        header.getStyle()
                .set("display", "flex")
                .set("padding", "0 0 var(--lumo-space-xs) 0")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("margin-bottom", "var(--lumo-space-xs)");
        Stream_addStandingsHeader(header);
        card.add(header);

        for (int i = 0; i < standings.size(); i++) {
            Team t = standings.get(i);
            boolean inRange = i < 3 || (i >= start && i < end);
            if (!inRange) {
                // Show ellipsis when skipping
                if (i == 3 && start > 3) {
                    card.add(ellipsisRow());
                }
                continue;
            }
            card.add(buildStandingsRow(t, i + 1, myTeam, i == 15));
        }

        // If user is near bottom, add ellipsis after top 3
        Span viewAll = new Span("View full standings →");
        viewAll.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-primary-color)")
                .set("cursor", "pointer")
                .set("margin-top", "var(--lumo-space-s)")
                .set("display", "block");
        viewAll.addClickListener(e -> UI.getCurrent().navigate(StandingsView.class));
        card.add(viewAll);

        return card;
    }

    private void Stream_addStandingsHeader(Div header) {
        String[] labels = {"#", "TEAM", "GP", "PTS"};
        String[] widths = {"24px", "1", "36px", "36px"};
        for (int i = 0; i < labels.length; i++) {
            Span s = new Span(labels[i]);
            s.getStyle()
                    .set("font-size", "var(--lumo-font-size-xxs)")
                    .set("color", "var(--lumo-tertiary-text-color)")
                    .set("letter-spacing", "0.06em");
            if (i == 1) s.getStyle().set("flex", widths[i]);
            else s.getStyle().set("width", widths[i]).set("text-align", "right");
            header.add(s);
        }
    }

    private Div buildStandingsRow(Team team, int rank, Team myTeam, boolean isPlayoffCutline) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("padding", "3px 0")
                .set("gap", "var(--lumo-space-s)");

        if (isPlayoffCutline) {
            row.getStyle().set("border-top", "1px dashed var(--lumo-contrast-30pct)");
        }

        boolean isUser = team == myTeam;
        if (isUser) {
            row.getStyle()
                    .set("background", "var(--lumo-primary-color-10pct)")
                    .set("border-radius", "var(--lumo-border-radius-s)")
                    .set("padding", "3px var(--lumo-space-xs)");
        }

        Span rankSpan = new Span(String.valueOf(rank));
        rankSpan.getStyle()
                .set("width", "24px")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span nameSpan = new Span(team.getName());
        nameSpan.getStyle()
                .set("flex", "1")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", isUser ? "700" : "400")
                .set("color", isUser ? "var(--lumo-primary-color)" : "var(--lumo-primary-text-color)")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");

        Span gp = new Span(String.valueOf(team.getGamesPlayed()));
        gp.getStyle().set("width", "36px").set("text-align", "right")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span pts = new Span(String.valueOf(team.getPoints()));
        pts.getStyle().set("width", "36px").set("text-align", "right")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700");

        row.add(rankSpan, nameSpan, gp, pts);
        return row;
    }

    private Div ellipsisRow() {
        Div row = new Div();
        Span dots = new Span("· · ·");
        dots.getStyle()
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("padding", "2px 0");
        row.add(dots);
        return row;
    }

    private Div buildNewsCard() {
        Div card = sectionCard("TEAM NEWS", "flex: 1; min-width: 220px; max-width: 340px;");

        List<NewsEvent> news = session.getNewsService()
                .getRecentForTeam(session.getUserTeam(), 8);

        if (news.isEmpty()) {
            Span empty = new Span("No news yet.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");
            card.add(empty);
            return card;
        }

        for (NewsEvent event : news) {
            Div row = new Div();
            row.getStyle()
                    .set("padding", "var(--lumo-space-xs) 0")
                    .set("border-bottom", "1px solid var(--lumo-contrast-5pct)");

            Span date = new Span(event.getDate().toString() + " · ");
            date.getStyle()
                    .set("font-size", "var(--lumo-font-size-xxs)")
                    .set("color", "var(--lumo-tertiary-text-color)");

            Span headline = new Span(event.getHeadline());
            headline.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)");

            row.add(date, headline);
            card.add(row);
        }

        Span viewAll = new Span("View all news →");
        viewAll.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-primary-color)")
                .set("cursor", "pointer")
                .set("margin-top", "var(--lumo-space-s)")
                .set("display", "block");
        viewAll.addClickListener(e -> UI.getCurrent().navigate(NewsView.class));
        card.add(viewAll);

        return card;
    }

    // ── Shared card builder ───────────────────────────────────────────

    private Div sectionCard(String title, String flexStyle) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "var(--lumo-space-m)");

        for (String pair : flexStyle.split(";")) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) card.getStyle().set(kv[0].trim(), kv[1].trim());
        }

        Span eyebrow = new Span(title);
        eyebrow.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("letter-spacing", "0.1em")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "700")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-s)");
        card.add(eyebrow);
        return card;
    }

    // ── Sim handlers ──────────────────────────────────────────────────

    @FunctionalInterface
    interface SimAction { List<GameResult> run(); }

    private void handleSim(SimAction action, String label) {
        List<GameResult> results = action.run();
        refreshDashboard();
        showResults(results, label);
        checkPhaseTransition();
    }

    private void confirmSimSeason() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Sim Rest of Season?");
        dialog.add(new Paragraph("Simulate all remaining games to end of season. Cannot be undone."));
        Button confirm = new Button("Sim Season", e -> {
            dialog.close();
            handleSim(() -> session.simSeason(), "Season");
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void checkPhaseTransition() {
        if (session.getPhase() == SeasonPhase.PLAYOFFS) {
            Notification n = Notification.show(
                    "Regular season over — playoffs begin!", 4000,
                    Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate(PlayoffView.class);
        }
    }

    private void showResults(List<GameResult> results, String label) {
        resultsArea.removeAll();
        if (results.isEmpty()) return;

        List<GameResult> myGames = results.stream()
                .filter(session::isMyTeamGame).collect(Collectors.toList());
        List<GameResult> otherGames = results.stream()
                .filter(r -> !session.isMyTeamGame(r)).collect(Collectors.toList());

        Div resultsCard = new Div();
        resultsCard.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        Span eyebrow = new Span("RESULT");
        eyebrow.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("letter-spacing", "0.1em")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "700")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-s)");
        resultsCard.add(eyebrow);

        for (GameResult r : myGames) {
            boolean won = r.getWinner() == session.getUserTeam();
            Span line = new Span((won ? "✓ " : "✗ ") + session.formatResult(r));
            line.getStyle()
                    .set("display", "block")
                    .set("font-size", "var(--lumo-font-size-m)")
                    .set("font-weight", "700")
                    .set("color", won ? "var(--lumo-success-color)" : "var(--lumo-error-color)");
            resultsCard.add(line);
        }

        if (!otherGames.isEmpty()) {
            Details others = new Details();
            others.setSummaryText("Other results (" + otherGames.size() + ")");
            others.setOpened(false);
            others.getStyle().set("margin-top", "var(--lumo-space-s)");
            VerticalLayout inner = new VerticalLayout();
            inner.setPadding(false);
            inner.setSpacing(false);
            for (GameResult r : otherGames) {
                Span s = new Span(session.formatResult(r));
                s.getStyle()
                        .set("font-size", "var(--lumo-font-size-s)")
                        .set("color", "var(--lumo-secondary-text-color)");
                inner.add(s);
            }
            others.add(inner);
            resultsCard.add(others);
        }

        resultsArea.add(resultsCard);
    }

    // ── Refresh ───────────────────────────────────────────────────────

    private void refreshDashboard() {
        List<com.vaadin.flow.component.Component> toRemove = getChildren().collect(Collectors.toList());
        toRemove.forEach(this::remove);
        add(buildTeamBanner());
        heroSection = buildHeroSection();
        add(heroSection);
        add(resultsArea);
        lowerSection = buildLowerSection();
        add(lowerSection);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private int getRank() {
        List<Team> standings = session.getLeague().getStandings();
        Team myTeam = session.getUserTeam();
        for (int i = 0; i < standings.size(); i++) {
            if (standings.get(i) == myTeam) return i + 1;
        }
        return standings.size();
    }
}
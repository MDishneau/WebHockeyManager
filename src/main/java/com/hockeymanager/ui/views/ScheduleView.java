package com.hockeymanager.ui.views;

import com.hockeymanager.backend.model.*;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.layouts.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;
import java.util.stream.Collectors;

@Route(value = "schedule", layout = MainLayout.class)
@PageTitle("My Schedule — Hockey Manager")
@AnonymousAllowed
public class ScheduleView extends VerticalLayout implements BeforeEnterObserver {

    private final GameSession session;

    public ScheduleView(GameSession session) {
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
        buildUI();
    }

    private void buildUI() {
        removeAll();
        Team myTeam = session.getUserTeam();

        H2 title = new H2(myTeam.getFullName() + " — Schedule");
        add(title);

        List<ScheduledGame> myGames = session.getSeasonManager().getSchedule().stream()
                .filter(g -> g.getHomeTeam() == myTeam || g.getAwayTeam() == myTeam)
                .collect(Collectors.toList());

        List<ScheduledGame> recent = myGames.stream()
                .filter(ScheduledGame::isPlayed).collect(Collectors.toList());
        List<ScheduledGame> upcoming = myGames.stream()
                .filter(g -> !g.isPlayed()).collect(Collectors.toList());

        // ── Recent Results ──────────────────────────────────────────
        H3 recentTitle = new H3("Recent Results");
        add(recentTitle);

        if (recent.isEmpty()) {
            add(new Span("No games played yet."));
        } else {
            List<ScheduledGame> last5 = recent.subList(Math.max(0, recent.size() - 5), recent.size());
            add(buildScheduleGrid(last5, myTeam, true));
        }

        // ── Upcoming Games ──────────────────────────────────────────
        H3 upcomingTitle = new H3("Upcoming Games");
        upcomingTitle.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        add(upcomingTitle);

        if (upcoming.isEmpty()) {
            add(new Span("No upcoming games — season may be over."));
        } else {
            add(buildScheduleGrid(upcoming.subList(0, Math.min(15, upcoming.size())), myTeam, false));
        }
    }

    private Grid<ScheduledGame> buildScheduleGrid(List<ScheduledGame> games, Team myTeam, boolean played) {
        Grid<ScheduledGame> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.setItems(games);

        grid.addColumn(g -> g.getDate().toString()).setHeader("Date").setWidth("110px").setFlexGrow(0);
        grid.addColumn(g -> {
            boolean isHome = g.getHomeTeam() == myTeam;
            return isHome ? "vs" : " @";
        }).setHeader("H/A").setWidth("55px").setFlexGrow(0);
        grid.addColumn(g -> {
            boolean isHome = g.getHomeTeam() == myTeam;
            return isHome ? g.getAwayTeam().getFullName() : g.getHomeTeam().getFullName();
        }).setHeader("Opponent").setFlexGrow(2);

        if (played) {
            grid.addColumn(g -> {
                if (!g.isPlayed()) return "";
                var r = g.getResult();
                boolean isHome = g.getHomeTeam() == myTeam;
                int myScore  = isHome ? r.getHomeScore() : r.getAwayScore();
                int oppScore = isHome ? r.getAwayScore() : r.getHomeScore();
                return myScore > oppScore ? "W" : (r.isOvertime() ? "OTL" : "L");
            }).setHeader("Result").setWidth("70px").setFlexGrow(0);

            grid.addColumn(g -> {
                if (!g.isPlayed()) return "";
                var r = g.getResult();
                boolean isHome = g.getHomeTeam() == myTeam;
                int myScore  = isHome ? r.getHomeScore() : r.getAwayScore();
                int oppScore = isHome ? r.getAwayScore() : r.getHomeScore();
                return myScore + " - " + oppScore + (r.isOvertime() ? " OT" : "");
            }).setHeader("Score").setWidth("90px").setFlexGrow(0);

            // Color rows by W/L
            grid.setPartNameGenerator(g -> {
                if (!g.isPlayed()) return null;
                var r = g.getResult();
                boolean isHome = g.getHomeTeam() == myTeam;
                int myScore  = isHome ? r.getHomeScore() : r.getAwayScore();
                int oppScore = isHome ? r.getAwayScore() : r.getHomeScore();
                return myScore > oppScore ? "win" : "loss";
            });
        }

        return grid;
    }
}
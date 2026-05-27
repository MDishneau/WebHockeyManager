package com.hockeymanager.ui.views;

import com.hockeymanager.backend.model.Team;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.layouts.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Route(value = "standings", layout = MainLayout.class)
@PageTitle("Standings — Hockey Manager")
@AnonymousAllowed
public class StandingsView extends VerticalLayout implements BeforeEnterObserver {

    private final GameSession session;

    public StandingsView(GameSession session) {
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

        H2 title = new H2("🏆 League Standings");
        add(title);

        List<Team> standings = session.getLeague().getStandings();
        Team myTeam = session.getUserTeam();

        // Numbered rank alongside team
        AtomicInteger rank = new AtomicInteger(1);

        Grid<Team> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.setMaxWidth("900px");

        // Rank column — computed from sorted index
        grid.addColumn(t -> rank.getAndIncrement()).setHeader("#")
                .setWidth("55px").setFlexGrow(0);

        grid.addComponentColumn(t -> {
            Span name = new Span(t.getFullName());
            if (t == myTeam) {
                name.getStyle().set("color", "var(--lumo-primary-color)");
                name.getStyle().set("font-weight", "bold");
            }
            return name;
        }).setHeader("Team").setFlexGrow(3);

        grid.addColumn(Team::getGamesPlayed).setHeader("GP").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Team::getWins).setHeader("W").setWidth("55px").setFlexGrow(0);
        grid.addColumn(Team::getLosses).setHeader("L").setWidth("55px").setFlexGrow(0);
        grid.addColumn(Team::getOtLosses).setHeader("OTL").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Team::getPoints).setHeader("PTS").setWidth("60px").setFlexGrow(0);

        // Highlight user's team row
        grid.setPartNameGenerator(t -> t == myTeam ? "my-team" : null);

        grid.setItems(standings);
        add(grid);

        // Playoff cutoff line hint
        if (standings.size() >= 16) {
            Span cutoff = new Span("─── Playoff cut line (top 16) ───");
            cutoff.getStyle().set("color", "var(--lumo-tertiary-text-color)")
                    .set("font-size", "var(--lumo-font-size-xs)");
            // Insert after the 16th grid row — simplest approach: just add a label below
            add(cutoff);
        }
    }
}
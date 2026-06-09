package com.hockeymanager.ui.layouts;

import com.hockeymanager.backend.model.SeasonPhase;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.views.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    private final GameSession session;

    // Persistent drawer components — refreshed in place by updateNav()
    private final VerticalLayout drawerLayout = new VerticalLayout();
    private final Div            teamHeader   = new Div();
    private final SideNav        nav          = new SideNav();

    public MainLayout(GameSession session) {
        this.session = session;
        setPrimarySection(Section.DRAWER);

        drawerLayout.setSizeFull();
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);

        Scroller scroller = new Scroller(nav);
        scroller.setWidthFull();

        drawerLayout.add(teamHeader, scroller);
        drawerLayout.expand(scroller);

        addToDrawer(drawerLayout);
        addToNavbar(true, createNavbarContent());

        // Initial population
        refreshTeamHeader();
        refreshNav();

        this.session.addGamePhaseListener(() ->
                getUI().ifPresent(ui -> ui.access(this::updateNav)));
    }

    public void updateNav() {
        refreshTeamHeader();
        refreshNav();
    }

    private void refreshTeamHeader() {
        teamHeader.removeAll();
        if (!session.isInitialized()) return;

        teamHeader.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Background.CONTRAST_5);

        Span teamName = new Span(session.getUserTeam().getFullName());
        teamName.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);

        Span record = new Span(
                session.getUserTeam().getWins() + "-"
                        + session.getUserTeam().getLosses() + "-"
                        + session.getUserTeam().getOtLosses()
                        + "  PTS: " + session.getUserTeam().getPoints());
        record.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

        VerticalLayout inner = new VerticalLayout(teamName, record);
        inner.setPadding(false);
        inner.setSpacing(false);
        teamHeader.add(inner);
    }

    private void refreshNav() {
        nav.removeAll();
        nav.setWidthFull();

        SeasonPhase phase = session.isInitialized() ? session.getPhase() : null;

        if (phase == SeasonPhase.REGULAR_SEASON) {
            nav.addItem(new SideNavItem(" Dashboard",   DashboardView.class));
            nav.addItem(new SideNavItem(" My Roster",   RosterView.class));
            nav.addItem(new SideNavItem(" My Schedule", ScheduleView.class));
            nav.addItem(new SideNavItem(" Standings",   StandingsView.class));
            nav.addItem(new SideNavItem(" News",        NewsView.class));
        } else if (phase == SeasonPhase.PLAYOFFS) {
            nav.addItem(new SideNavItem(" Playoffs",    PlayoffView.class));
            nav.addItem(new SideNavItem(" Standings",   StandingsView.class));
            nav.addItem(new SideNavItem(" News",        NewsView.class));
        } else if (phase == SeasonPhase.OFFSEASON) {
            nav.addItem(new SideNavItem(" Offseason",   OffseasonView.class));
            nav.addItem(new SideNavItem(" Scouting",    ScoutingView.class));
            nav.addItem(new SideNavItem(" Draft",       DraftView.class));
            nav.addItem(new SideNavItem(" Free Agency", FreeAgencyView.class));
            nav.addItem(new SideNavItem(" News",        NewsView.class));
        } else {
            nav.addItem(new SideNavItem("🏠 New Game", TeamSelectionView.class));
        }
    }

    private Component createNavbarContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Toggle menu");

        H2 title = new H2(" Hockey Manager");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        HorizontalLayout header = new HorizontalLayout(toggle, title);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        return header;
    }
}
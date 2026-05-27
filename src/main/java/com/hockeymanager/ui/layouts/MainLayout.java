package com.hockeymanager.ui.layouts;

import com.hockeymanager.backend.model.SeasonPhase;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.views.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    private final GameSession session;
    public final SideNav nav = new SideNav();

    public MainLayout(GameSession session) {
        this.session = session;
        setPrimarySection(Section.DRAWER);
        addToDrawer(createDrawerContent());
        addToNavbar(true, createNavbarContent());
        this.session.addGamePhaseListener(() -> {
            getUI().ifPresent(ui -> {
                ui.access(this::updateNav);
            });
        });
    }

    public void updateNav() {
        addToDrawer(createDrawerContent());
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



    private Component createDrawerContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);

        // Team name header
        if (session.isInitialized()) {
            Div teamHeader = new Div();
            teamHeader.addClassNames(LumoUtility.Padding.MEDIUM,
                    LumoUtility.Background.CONTRAST_5);
            Span teamName = new Span(session.getUserTeam().getFullName());
            teamName.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
            Span record = new Span(session.getUserTeam().getWins() + "-"
                    + session.getUserTeam().getLosses() + "-"
                    + session.getUserTeam().getOtLosses()
                    + "  PTS: " + session.getUserTeam().getPoints());
            record.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
            teamHeader.add(new VerticalLayout(teamName, record));
            layout.add(teamHeader);
        }


        nav.setWidthFull();

        SeasonPhase phase = session.isInitialized() ? session.getPhase() : null;
        nav.removeAll();

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
            // Pre-game: only team selection
            nav.addItem(new SideNavItem("🏠 New Game", TeamSelectionView.class));
        }

        Scroller scroller = new Scroller(nav);
        scroller.setWidthFull();
        layout.add(scroller);
        layout.expand(scroller);

        return layout;
    }
}
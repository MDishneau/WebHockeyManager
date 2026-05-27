package com.hockeymanager.ui.views;

import com.hockeymanager.backend.manager.DraftEngine;
import com.hockeymanager.backend.model.*;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.layouts.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "draft", layout = MainLayout.class)
@PageTitle("Draft — Hockey Manager")
@AnonymousAllowed
public class DraftView extends VerticalLayout implements BeforeEnterObserver {

    private static final int ROUNDS = 7;
    private static final int TEAMS  = 32;

    private final GameSession session;

    // Draft state
    private int currentRound    = 1;
    private int pickInRound     = 0;
    private int totalPickNumber = 0;
    private boolean draftOver   = false;

    // UI pieces we refresh
    private Div statusBar;
    private Div logArea;
    private VerticalLayout bigBoardSection;
    private Div myPicksSection;

    public DraftView(GameSession session) {
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
        if (session.getDraftEngine() == null) {
            event.rerouteTo(OffseasonView.class);
            return;
        }
        buildUI();
    }

    private void buildUI() {
        removeAll();

        H2 title = new H2("📋 NHL Entry Draft — " + session.getYear());
        add(title);

        // Status bar
        statusBar = new Div();
        statusBar.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.SMALL, LumoUtility.Background.BASE,
                LumoUtility.Margin.Bottom.SMALL);
        statusBar.setWidthFull();
        add(statusBar);

        // Log area (CPU picks log)
        logArea = new Div();
        logArea.setWidthFull();
        logArea.getStyle().set("max-height", "180px").set("overflow-y", "auto");
        logArea.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.SMALL, LumoUtility.Background.CONTRAST_5,
                LumoUtility.Margin.Bottom.SMALL);
        add(logArea);

        // Big board
        bigBoardSection = new VerticalLayout();
        bigBoardSection.setPadding(false);
        bigBoardSection.setSpacing(false);
        bigBoardSection.setWidthFull();
        add(bigBoardSection);

        // My picks
        myPicksSection = new Div();
        myPicksSection.setWidthFull();
        add(myPicksSection);

        updateStatusBar();
        renderBigBoard();
        advanceDraft(); // start processing CPU picks immediately
    }

    /** Runs CPU picks until it's the user's turn or draft is over. */
    private void advanceDraft() {
        DraftEngine engine = session.getDraftEngine();

        while (currentRound <= ROUNDS) {
            if (engine.getAvailableProspects().isEmpty()) {
                draftOver = true;
                break;
            }

            Team pickingTeam = engine.getTeamForPick(currentRound, pickInRound);
            totalPickNumber++;

            if (pickingTeam == session.getUserTeam()) {
                // User's turn — stop and show pick dialog
                updateStatusBar();
                renderBigBoard();
                showUserPickDialog(currentRound, totalPickNumber);
                return;
            } else {
                // CPU auto-pick
                Prospect pick = engine.cpuPick(pickingTeam);
                engine.draftPlayer(pick, pickingTeam);
                appendLog(String.format("Pick %3d: %-25s selects %-22s OVR:%-3d POT:%s",
                        totalPickNumber, pickingTeam.getFullName(),
                        pick.getName(), pick.getOverall(), pick.getPotentialGrade()));
            }

            pickInRound++;
            if (pickInRound >= TEAMS) {
                pickInRound = 0;
                currentRound++;
            }
        }

        // Draft complete
        draftOver = true;
        updateStatusBar();
        renderBigBoard();
        renderMyPicks();
        Button finish = new Button("✓ Draft Complete — Return to Offseason");
        finish.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        finish.addClickListener(e -> {
            session.completeDraft();
            UI.getCurrent().navigate(OffseasonView.class);
        });
        add(finish);
    }

    private void showUserPickDialog(int round, int pickNum) {
        DraftEngine engine = session.getDraftEngine();
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("750px");
        dialog.setHeaderTitle("► YOUR PICK — Round " + round + ", Pick #" + pickNum);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);

        Paragraph hint = new Paragraph("Click a prospect row to select them for your team.");
        hint.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        content.add(hint);

        List<Prospect> available = engine.getAvailableProspects().stream()
                .sorted(Comparator.comparingInt(Prospect::getOverall).reversed())
                .collect(Collectors.toList());

        Grid<Prospect> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setHeight("400px");
        grid.setItems(available);

        grid.addColumn(Prospect::getDraftRank).setHeader("Rank").setWidth("70px").setFlexGrow(0);
        grid.addColumn(Prospect::getName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(p -> p.getPosition().toString()).setHeader("Pos").setWidth("100px").setFlexGrow(0);
        grid.addColumn(Prospect::getAge).setHeader("Age").setWidth("55px").setFlexGrow(0);
        grid.addColumn(Prospect::getOverall).setHeader("OVR").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Prospect::getPotentialGrade).setHeader("POT").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Prospect::getArchetypeDisplay).setHeader("Archetype").setFlexGrow(1);
        grid.addColumn(p -> p.getRegion().getDisplayName()).setHeader("Region").setFlexGrow(1);

        grid.addComponentColumn(prospect -> {
            Button btn = new Button("Draft");
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            btn.addClickListener(e -> {
                engine.draftPlayer(prospect, session.getUserTeam());
                appendLog(String.format("✓ YOU drafted: %-22s %-14s OVR:%-3d POT:%s",
                        prospect.getName(), prospect.getPosition(),
                        prospect.getOverall(), prospect.getPotentialGrade()));
                dialog.close();
                Notification.show("You drafted " + prospect.getName() + "!",
                                3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Advance to next pick
                pickInRound++;
                if (pickInRound >= TEAMS) {
                    pickInRound = 0;
                    currentRound++;
                }
                advanceDraft();
            });
            return btn;
        }).setHeader("Pick").setWidth("80px").setFlexGrow(0);

        content.add(grid);
        dialog.add(content);
        dialog.open();
    }

    private void updateStatusBar() {
        statusBar.removeAll();
        if (draftOver) {
            Span done = new Span("✓ Draft Complete");
            done.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SUCCESS);
            statusBar.add(done);
        } else {
            Span status = new Span("Round " + currentRound + " of " + ROUNDS
                    + "  |  Pick " + (totalPickNumber + 1)
                    + "  |  Prospects remaining: "
                    + session.getDraftEngine().getAvailableProspects().size());
            status.addClassNames(LumoUtility.FontWeight.BOLD);
            statusBar.add(status);
        }
    }

    private void renderBigBoard() {
        bigBoardSection.removeAll();
        if (draftOver) return;

        H3 boardTitle = new H3("Big Board — Available Prospects");
        boardTitle.addClassNames(LumoUtility.Margin.Bottom.XSMALL);
        bigBoardSection.add(boardTitle);

        DraftEngine engine = session.getDraftEngine();
        List<Prospect> available = engine.getAvailableProspects().stream()
                .sorted(Comparator.comparingInt(Prospect::getOverall).reversed())
                .limit(50)
                .collect(Collectors.toList());

        Grid<Prospect> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.setItems(available);

        grid.addColumn(Prospect::getDraftRank).setHeader("Rank").setWidth("70px").setFlexGrow(0);
        grid.addColumn(Prospect::getName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(p -> p.getPosition().toString()).setHeader("Pos").setWidth("100px").setFlexGrow(0);
        grid.addColumn(Prospect::getAge).setHeader("Age").setWidth("55px").setFlexGrow(0);
        grid.addColumn(Prospect::getOverall).setHeader("OVR").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Prospect::getPotentialGrade).setHeader("POT").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Prospect::getArchetypeDisplay).setHeader("Archetype").setFlexGrow(1);
        grid.addColumn(p -> p.getRegion().getDisplayName()).setHeader("Region").setFlexGrow(1);

        bigBoardSection.add(grid);
    }

    private void renderMyPicks() {
        myPicksSection.removeAll();
        DraftEngine engine = session.getDraftEngine();
        Team userTeam = session.getUserTeam();

        List<Prospect> myPicks = engine.getDraftResults().stream()
                .filter(p -> userTeam.getRoster().contains(p.getPlayer()))
                .collect(Collectors.toList());

        if (myPicks.isEmpty()) return;

        H3 myPicksTitle = new H3("Your Draft Picks");
        myPicksSection.add(myPicksTitle);

        Grid<Prospect> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.setItems(myPicks);

        grid.addColumn(Prospect::getName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(p -> p.getPosition().toString()).setHeader("Position").setFlexGrow(1);
        grid.addColumn(Prospect::getOverall).setHeader("OVR").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Prospect::getPotentialGrade).setHeader("POT").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Prospect::getArchetypeDisplay).setHeader("Archetype").setFlexGrow(1);

        myPicksSection.add(grid);
    }

    private void appendLog(String text) {
        Div line = new Div();
        Span content = new Span(text);
        content.addClassNames(LumoUtility.FontSize.XSMALL);
        if (text.startsWith("✓")) {
            content.getStyle().set("color", "var(--lumo-success-color)");
            content.addClassNames(LumoUtility.FontWeight.BOLD);
        }
        line.add(content);
        logArea.add(line);
        // Auto-scroll to bottom
        logArea.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }
}
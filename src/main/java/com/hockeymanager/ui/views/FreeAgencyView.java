package com.hockeymanager.ui.views;

import com.hockeymanager.backend.manager.ContractEngine;
import com.hockeymanager.backend.manager.FreeAgentPool;
import com.hockeymanager.backend.manager.FreeAgencyEngine;
import com.hockeymanager.backend.manager.SalaryCapManager;
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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "freeagency", layout = MainLayout.class)
@PageTitle("Free Agency — Hockey Manager")
@AnonymousAllowed
public class FreeAgencyView extends VerticalLayout implements BeforeEnterObserver {

    private final GameSession session;

    // UI sections we refresh
    private Div capBar;
    private Div pendingOffersSection;
    private Div resultsSection;
    private Grid<FreeAgentPool.FreeAgent> faGrid;

    // Filter state
    private String positionFilter = "All";

    public FreeAgencyView(GameSession session) {
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
        if (session.getFreeAgentPool() == null) {
            event.rerouteTo(OffseasonView.class);
            return;
        }
        buildUI();
    }

    private void buildUI() {
        removeAll();

        H2 title = new H2("🖊 Free Agency — " + session.getYear());
        Paragraph desc = new Paragraph(
                "Browse available free agents, submit contract offers, then resolve signing day.");
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        add(title, desc);

        // ── Cap bar ───────────────────────────────────────────────────
        capBar = buildCapBar();
        add(capBar);

        // ── Filter row ────────────────────────────────────────────────
        add(buildFilterRow());

        // ── Free agent grid ───────────────────────────────────────────
        faGrid = buildFaGrid();
        add(faGrid);

        // ── Pending offers section ────────────────────────────────────
        pendingOffersSection = new Div();
        pendingOffersSection.setWidthFull();
        add(pendingOffersSection);
        refreshPendingOffers();

        // ── Resolve + results ─────────────────────────────────────────
        resultsSection = new Div();
        resultsSection.setWidthFull();
        add(resultsSection);

        add(buildResolveSection());
    }

    // ── Cap bar ───────────────────────────────────────────────────────

    private Div buildCapBar() {
        Div bar = new Div();
        bar.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.SMALL, LumoUtility.Background.BASE,
                LumoUtility.Margin.Bottom.SMALL);
        bar.setWidthFull();
        refreshCapBar(bar);
        return bar;
    }

    private void refreshCapBar(Div bar) {
        bar.removeAll();
        SalaryCapManager cap = session.getSalaryCapManager();
        Team team = session.getUserTeam();

        String summary = cap.getCapSummary(team);
        Span capSpan = new Span(team.getFullName() + "  |  " + summary);
        capSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);

        if (cap.isOverCap(team)) {
            capSpan.getStyle().set("color", "var(--lumo-error-color)");
        } else if (cap.isUnderFloor(team)) {
            capSpan.getStyle().set("color", "var(--lumo-warning-text-color)");
        }
        bar.add(capSpan);
    }

    // ── Filter row ────────────────────────────────────────────────────

    private HorizontalLayout buildFilterRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        row.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        Span label = new Span("Filter:");
        label.addClassNames(LumoUtility.FontSize.SMALL);

        Select<String> posSelect = new Select<>();
        posSelect.setItems("All", "Forwards", "Defense", "Goalies");
        posSelect.setValue("All");
        posSelect.setWidth("130px");
        posSelect.addValueChangeListener(e -> {
            positionFilter = e.getValue();
            refreshFaGrid();
        });

        row.add(label, posSelect);
        return row;
    }

    // ── Free agent grid ───────────────────────────────────────────────

    private Grid<FreeAgentPool.FreeAgent> buildFaGrid() {
        Grid<FreeAgentPool.FreeAgent> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(false);
        grid.setHeight("380px");
        grid.setWidthFull();

        grid.addColumn(fa -> fa.player().getPosition().toString())
                .setHeader("Pos").setWidth("80px").setFlexGrow(0).setSortable(true);
        grid.addColumn(fa -> fa.player().getName())
                .setHeader("Name").setFlexGrow(2).setSortable(true);
        grid.addColumn(fa -> fa.player().getAge())
                .setHeader("Age").setWidth("60px").setFlexGrow(0).setSortable(true);
        grid.addColumn(fa -> fa.player().getOverall())
                .setHeader("OVR").setWidth("60px").setFlexGrow(0).setSortable(true);
        grid.addColumn(fa -> fa.status().getDisplayName())
                .setHeader("Status").setWidth("80px").setFlexGrow(0);
        grid.addColumn(fa -> fa.previousTeam().getFullName())
                .setHeader("Prev Team").setFlexGrow(2);
        grid.addColumn(fa -> {
            long mv = session.getContractEngine().calculateMarketValue(fa.player());
            return String.format("$%.1fM", mv / 1_000_000.0);
        }).setHeader("Market Val").setWidth("100px").setFlexGrow(0);

        // Stats column — goals/assists for skaters, wins for goalies
        grid.addColumn(fa -> {
            Player p = fa.player();
            if (p.getPosition() == Position.GOALIE) {
                return p.getWins() + "W  " + String.format("%.2f", p.getGAA()) + " GAA";
            }
            return p.getGoals() + "G  " + p.getAssists() + "A  " + p.getPoints() + "PTS";
        }).setHeader("Last Season").setFlexGrow(2);

        // Make offer button column
        grid.addComponentColumn(fa -> {
            Button btn = new Button("Make Offer");
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            btn.addClickListener(e -> showOfferDialog(fa));
            return btn;
        }).setHeader("").setWidth("120px").setFlexGrow(0);

        grid.setItems(getFilteredAgents());
        return grid;
    }

    private void refreshFaGrid() {
        faGrid.setItems(getFilteredAgents());
    }

    private List<FreeAgentPool.FreeAgent> getFilteredAgents() {
        List<FreeAgentPool.FreeAgent> pool = session.getFreeAgentPool().getPool();
        return pool.stream()
                .filter(fa -> switch (positionFilter) {
                    case "Forwards" -> fa.player().getPosition() == Position.CENTER
                            || fa.player().getPosition() == Position.LEFT_WING
                            || fa.player().getPosition() == Position.RIGHT_WING;
                    case "Defense" -> fa.player().getPosition() == Position.LEFT_DEFENSE
                            || fa.player().getPosition() == Position.RIGHT_DEFENSE;
                    case "Goalies" -> fa.player().getPosition() == Position.GOALIE;
                    default -> true;
                })
                .sorted(Comparator.comparingInt(fa -> -fa.player().getOverall()))
                .collect(Collectors.toList());
    }

    // ── Offer dialog ──────────────────────────────────────────────────

    private void showOfferDialog(FreeAgentPool.FreeAgent fa) {
        Player player = fa.player();
        ContractEngine ce = session.getContractEngine();
        SalaryCapManager cap = session.getSalaryCapManager();
        Team userTeam = session.getUserTeam();

        long marketVal = ce.calculateMarketValue(player);
        long capSpace = cap.getCapSpace(userTeam);
        int suggestedYears = ce.preferredLength(player, fa.previousTeam() == userTeam);

        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setHeaderTitle("Make Offer — " + player.getName());

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);

        // Player summary
        Div summary = new Div();
        summary.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.SMALL, LumoUtility.Margin.Bottom.SMALL);
        summary.add(new Span(player.getPosition() + "  |  Age: " + player.getAge()
                + "  |  OVR: " + player.getOverall()
                + "  |  Status: " + fa.status().getDisplayName()));

        Div statsDiv = new Div();
        if (player.getPosition() == Position.GOALIE) {
            statsDiv.add(new Span("Last season: " + player.getWins() + "W  "
                    + String.format("%.2f", player.getGAA()) + " GAA  "
                    + String.format("%.3f", player.getSavePercentage()) + " SV%"));
        } else {
            statsDiv.add(new Span("Last season: " + player.getGoals() + "G  "
                    + player.getAssists() + "A  " + player.getPoints() + "PTS"));
        }
        statsDiv.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        summary.add(statsDiv);
        content.add(summary);

        // Market value info
        Span mvLabel = new Span(String.format(
                "Market value: $%.1fM/yr   |   Your cap space: $%.1fM",
                marketVal / 1_000_000.0, capSpace / 1_000_000.0));
        mvLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        content.add(mvLabel);

        Hr hr = new Hr();
        hr.getStyle().set("margin", "var(--lumo-space-s) 0");
        content.add(hr);

        // Salary field (in $M)
        NumberField salaryField = new NumberField("Annual salary ($ millions)");
        salaryField.setMin(Contract.MIN_SALARY / 1_000_000.0);
        salaryField.setMax(Math.min(Contract.MAX_SALARY, capSpace) / 1_000_000.0);
        salaryField.setStep(0.1);
        salaryField.setValue(Math.min(marketVal, capSpace) / 1_000_000.0);
        salaryField.setWidthFull();
        content.add(salaryField);

        // Years field
        IntegerField yearsField = new IntegerField("Contract length (years)");
        yearsField.setMin(1);
        yearsField.setMax(Contract.MAX_YEARS_NEW);
        yearsField.setValue(Math.min(suggestedYears, Contract.MAX_YEARS_NEW));
        yearsField.setStepButtonsVisible(true);
        yearsField.setWidthFull();
        content.add(yearsField);

        // Warning if over cap space
        Span capWarning = new Span();
        capWarning.addClassNames(LumoUtility.FontSize.XSMALL);
        capWarning.getStyle().set("color", "var(--lumo-error-color)");
        content.add(capWarning);

        salaryField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                long offered = (long)(e.getValue() * 1_000_000);
                if (offered > capSpace) {
                    capWarning.setText("⚠ Exceeds your cap space of $"
                            + String.format("%.1f", capSpace / 1_000_000.0) + "M");
                } else {
                    capWarning.setText("");
                }
            }
        });

        dialog.add(content);

        // Footer buttons
        Button submitBtn = new Button("Submit Offer", e -> {
            Double salaryMil = salaryField.getValue();
            Integer years = yearsField.getValue();
            if (salaryMil == null || years == null) return;
            long salary = (long)(salaryMil * 1_000_000);

            boolean ok = session.getFreeAgencyEngine()
                    .submitOffer(userTeam, player, salary, years);
            dialog.close();
            if (ok) {
                Notification.show(
                                "Offer submitted to " + player.getName() + " — $"
                                        + String.format("%.1f", salaryMil) + "M x" + years + " yr",
                                3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show("Could not submit offer — check cap space.",
                                3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            refreshCapBar(capBar);
            refreshPendingOffers();
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelBtn, submitBtn);
        dialog.open();
    }

    // ── Pending offers section ─────────────────────────────────────────

    private void refreshPendingOffers() {
        pendingOffersSection.removeAll();

        List<FreeAgencyEngine.Offer> offers = session.getFreeAgencyEngine()
                .getPendingOffersFor(session.getUserTeam());

        if (offers.isEmpty()) return;

        H3 offerTitle = new H3("Your Pending Offers (" + offers.size() + ")");
        offerTitle.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Margin.Bottom.XSMALL);
        pendingOffersSection.add(offerTitle);

        Grid<FreeAgencyEngine.Offer> offerGrid = new Grid<>();
        offerGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        offerGrid.setAllRowsVisible(true);
        offerGrid.setWidthFull();
        offerGrid.setItems(offers);

        offerGrid.addColumn(o -> o.player().getName()).setHeader("Player").setFlexGrow(2);
        offerGrid.addColumn(o -> o.player().getPosition().toString())
                .setHeader("Pos").setWidth("80px").setFlexGrow(0);
        offerGrid.addColumn(o -> o.player().getOverall())
                .setHeader("OVR").setWidth("60px").setFlexGrow(0);
        offerGrid.addColumn(o -> String.format("$%.1fM/yr", o.salary() / 1_000_000.0))
                .setHeader("Salary").setWidth("100px").setFlexGrow(0);
        offerGrid.addColumn(o -> o.years() + " yr").setHeader("Term").setWidth("70px").setFlexGrow(0);

        offerGrid.addComponentColumn(offer -> {
            Button withdraw = new Button("Withdraw");
            withdraw.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY);
            withdraw.addClickListener(e -> {
                session.getFreeAgencyEngine()
                        .withdrawOffer(session.getUserTeam(), offer.player());
                Notification.show("Offer withdrawn from " + offer.player().getName(),
                        2000, Notification.Position.BOTTOM_START);
                refreshPendingOffers();
                refreshCapBar(capBar);
            });
            return withdraw;
        }).setHeader("").setWidth("110px").setFlexGrow(0);

        pendingOffersSection.add(offerGrid);
    }

    // ── Resolve section ────────────────────────────────────────────────

    private Div buildResolveSection() {
        Div section = new Div();
        section.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM, LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Background.BASE);
        section.setWidthFull();

        H3 resolveTitle = new H3("Signing Day");
        resolveTitle.addClassNames(LumoUtility.Margin.Bottom.XSMALL);

        Paragraph resolveDesc = new Paragraph(
                "When you're ready, run signing day. CPU teams will also make offers and "
                        + "each player will choose the best deal they're willing to accept.");
        resolveDesc.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Button resolveBtn = new Button("🖊 Run Signing Day");
        resolveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        resolveBtn.addClickListener(e -> confirmResolve(resolveBtn));

        Button backBtn = new Button("← Back to Offseason");
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backBtn.addClickListener(e -> UI.getCurrent().navigate(OffseasonView.class));

        HorizontalLayout actions = new HorizontalLayout(resolveBtn, backBtn);
        actions.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        actions.setSpacing(true);

        section.add(resolveTitle, resolveDesc, actions);
        return section;
    }

    private void confirmResolve(Button resolveBtn) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Run Signing Day?");
        Paragraph msg = new Paragraph(
                "CPU teams will submit offers to all free agents, then every player picks "
                        + "the best deal they'll accept. This cannot be undone.");

        Button confirm = new Button("Run Signing Day", e -> {
            dialog.close();
            runSigningDay();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, confirm);
        dialog.add(msg);
        dialog.open();
    }

    private void runSigningDay() {
        FreeAgencyEngine engine = session.getFreeAgencyEngine();
        FreeAgentPool pool = session.getFreeAgentPool();

        // CPU teams make their offers first
        engine.generateCpuOffers(pool, session.getLeague(), session.getUserTeam());

        // Resolve all offers
        List<String> results = engine.resolveAllOffers(pool, session.getUserTeam());

        // Mark free agency done
        session.completeFreeAgency();

        // Show results
        showSigningResults(results);

        // Refresh the grid — signed players are off the board
        refreshFaGrid();
        refreshCapBar(capBar);
        refreshPendingOffers();
    }

    private void showSigningResults(List<String> results) {
        resultsSection.removeAll();

        H3 resultsTitle = new H3("Signing Day Results");
        resultsTitle.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Margin.Bottom.XSMALL);
        resultsSection.add(resultsTitle);

        // Separate user signings from rest
        List<String> mySignings = results.stream()
                .filter(r -> r.contains("YOUR TEAM")).collect(Collectors.toList());
        List<String> otherResults = results.stream()
                .filter(r -> !r.contains("YOUR TEAM")).collect(Collectors.toList());

        if (!mySignings.isEmpty()) {
            H4 myTitle = new H4("Your Signings");
            myTitle.addClassNames(LumoUtility.Margin.Bottom.XSMALL, LumoUtility.Margin.Top.SMALL);
            resultsSection.add(myTitle);
            for (String line : mySignings) {
                Div row = new Div();
                Span span = new Span("✓ " + line.replace(" ◄ YOUR TEAM", ""));
                span.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD);
                span.getStyle().set("color", "var(--lumo-success-color)");
                row.add(span);
                resultsSection.add(row);
            }
        }

        // League transactions — collapsible log
        Div leagueLog = new Div();
        leagueLog.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.SMALL, LumoUtility.Background.CONTRAST_5,
                LumoUtility.Margin.Top.SMALL);
        leagueLog.getStyle().set("max-height", "200px").set("overflow-y", "auto");

        H4 leagueTitle = new H4("League Transactions (" + otherResults.size() + ")");
        leagueTitle.addClassNames(LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.XSMALL);
        leagueLog.add(leagueTitle);

        for (String line : otherResults) {
            Div row = new Div();
            Span span = new Span(line);
            span.addClassNames(LumoUtility.FontSize.XSMALL);
            if (line.contains("remains unsigned")) {
                span.addClassNames(LumoUtility.TextColor.SECONDARY);
            }
            row.add(span);
            leagueLog.add(row);
        }
        resultsSection.add(leagueLog);

        // Return to offseason button
        Button doneBtn = new Button("✓ Return to Offseason");
        doneBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        doneBtn.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        doneBtn.addClickListener(e -> UI.getCurrent().navigate(OffseasonView.class));
        resultsSection.add(doneBtn);
    }
}
package com.hockeymanager.ui.views;

import com.hockeymanager.backend.manager.ScoutingService;
import com.hockeymanager.backend.model.ScoutingRegion;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.layouts.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route(value = "scouting", layout = MainLayout.class)
@PageTitle("Scouting — Hockey Manager")
@AnonymousAllowed
public class ScoutingView extends VerticalLayout implements BeforeEnterObserver {

    private final GameSession session;
    private Span budgetLabel;

    public ScoutingView(GameSession session) {
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
        if (session.getScoutingService() == null) {
            event.rerouteTo(OffseasonView.class);
            return;
        }
        buildUI();
    }

    private void buildUI() {
        removeAll();
        ScoutingService scouting = session.getScoutingService();

        H2 title = new H2("🔭 Scouting Budget");
        Paragraph desc = new Paragraph("Allocate scouting points across regions. "
                + "More points in a region reveals more prospect details before the draft.");
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        budgetLabel = new Span("Remaining: " + scouting.getRemainingBudget()
                + " / " + ScoutingService.BUDGET_PER_SEASON + " points");
        budgetLabel.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        add(title, desc, budgetLabel);

        // ── Region allocation cards ──────────────────────────────────
        for (ScoutingRegion region : ScoutingRegion.values()) {
            add(buildRegionCard(region, scouting));
        }

        // ── Action buttons ────────────────────────────────────────────
        Button finish = new Button("✓ Finalize Scouting");
        finish.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        finish.addClickListener(e -> {
            session.finalizeScouting();
            Notification.show("Scouting complete! Prospects updated.",
                            3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate(OffseasonView.class);
        });

        Button back = new Button("← Back to Offseason");
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.addClickListener(e -> UI.getCurrent().navigate(OffseasonView.class));

        HorizontalLayout actions = new HorizontalLayout(finish, back);
        actions.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        add(actions);
    }

    private Div buildRegionCard(ScoutingRegion region, ScoutingService scouting) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM, LumoUtility.Margin.Bottom.SMALL,
                LumoUtility.Background.BASE);
        card.setWidthFull();
        card.getStyle().set("max-width", "600px");

        HorizontalLayout row = new HorizontalLayout();
        row.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        row.setWidthFull();

        Span regionName = new Span(region.getDisplayName());
        regionName.addClassNames(LumoUtility.FontWeight.BOLD);
        regionName.getStyle().set("width", "150px").set("flex-shrink", "0");

        Span currentPts = new Span("Current: " + scouting.getAllocationFor(region) + " pts");
        currentPts.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        currentPts.getStyle().set("width", "110px").set("flex-shrink", "0");

        IntegerField field = new IntegerField();
        field.setMin(0);
        field.setMax(scouting.getRemainingBudget() + scouting.getAllocationFor(region));
        field.setValue(0);
        field.setWidth("100px");
        field.setStepButtonsVisible(true);

        Button allocBtn = new Button("Allocate");
        allocBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        allocBtn.addClickListener(e -> {
            Integer pts = field.getValue();
            if (pts == null || pts <= 0) return;
            boolean ok = scouting.allocate(region, pts);
            if (ok) {
                budgetLabel.setText("Remaining: " + scouting.getRemainingBudget()
                        + " / " + ScoutingService.BUDGET_PER_SEASON + " points");
                currentPts.setText("Current: " + scouting.getAllocationFor(region) + " pts");
                field.setMax(scouting.getRemainingBudget() + scouting.getAllocationFor(region));
                field.setValue(0);
                Notification.show("Allocated " + pts + " pts to " + region.getDisplayName(),
                        2000, Notification.Position.BOTTOM_START);
            } else {
                Notification.show("Not enough budget remaining.", 2000,
                                Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        row.add(regionName, currentPts, field, allocBtn);
        card.add(row);
        return card;
    }
}
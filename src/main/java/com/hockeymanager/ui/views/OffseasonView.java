package com.hockeymanager.ui.views;

import com.hockeymanager.backend.model.SeasonPhase;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route(value = "offseason", layout = MainLayout.class)
@PageTitle("Offseason — Hockey Manager")
@AnonymousAllowed
public class OffseasonView extends VerticalLayout implements BeforeEnterObserver {

    private final GameSession session;

    public OffseasonView(GameSession session) {
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
        // Auto-begin offseason if playoffs just ended
        if (session.getPhase() == SeasonPhase.PLAYOFFS
                && session.getPlayoffBracket() != null
                && session.getPlayoffBracket().isOver()) {
            session.beginOffseason();
        }
        buildUI();
    }

    private void buildUI() {
        removeAll();

        H2 title = new H2(" Offseason — " + session.getYear());
        Paragraph subtitle = new Paragraph("Complete offseason activities before starting the next season.");
        subtitle.addClassNames(LumoUtility.TextColor.SECONDARY);
        add(title, subtitle);

        // ── Activity cards ─────────────────────────────────────────────
        add(buildActivityCard(
                " Scouting",
                "Allocate your scouting budget across regions to reveal prospect details before the draft.",
                session.isScoutingDone(),
                "Go to Scouting",
                () -> UI.getCurrent().navigate(ScoutingView.class)
        ));

        add(buildActivityCard(
                " Draft",
                "Run the NHL Entry Draft. Pick prospects for your team across 7 rounds.",
                session.isDraftDone(),
                "Go to Draft",
                () -> {
                    if (!session.isScoutingDone()) {
                        Notification.show("Complete scouting first to reveal prospect details.",
                                        3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_WARNING);
                        return;
                    }
                    session.beginDraft();
                    UI.getCurrent().navigate(DraftView.class);
                }
        ));

        add(buildActivityCard(
                " Free Agency",
                "Browse UFAs and RFAs, submit contract offers, and sign players to your team.",
                session.isFreeAgencyDone(),
                "Go to Free Agency",
                () -> UI.getCurrent().navigate(FreeAgencyView.class)
        ));

        // ── Begin next season button ───────────────────────────────────
        Div nextSeasonSection = new Div();
        nextSeasonSection.addClassNames(LumoUtility.Margin.Top.LARGE,
                LumoUtility.Padding.MEDIUM, LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM);

        H3 nextTitle = new H3("Begin " + (session.getYear() + 1) + "-" + (session.getYear() + 2) + " Season");
        Button beginBtn = new Button("▶ Start Next Season");
        beginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        beginBtn.addClickListener(e -> {
            session.beginNextSeason();
            Notification.show(session.getYear() + "-" + (session.getYear() + 1) + " season has begun!",
                            3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate(DashboardView.class);
        });

        Paragraph hint = new Paragraph("You can begin the next season at any time. "
                + "Uncompleted activities will be skipped.");
        hint.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        nextSeasonSection.add(nextTitle, hint, beginBtn);
        add(nextSeasonSection);
    }

    private Div buildActivityCard(String cardTitle, String description,
                                  boolean done, String buttonLabel, Runnable action) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM, LumoUtility.Margin.Bottom.MEDIUM,
                LumoUtility.Background.BASE);
        card.setWidthFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();

        H3 titleSpan = new H3(cardTitle);
        titleSpan.addClassNames(LumoUtility.Margin.NONE);

        Span badge = done
                ? createBadge("✓ Done", "var(--lumo-success-color)")
                : createBadge("Pending", "var(--lumo-tertiary-text-color)");

        header.add(titleSpan, badge);
        header.expand(titleSpan);

        Paragraph desc = new Paragraph(description);
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        Button btn = new Button(buttonLabel);
        btn.addThemeVariants(done ? ButtonVariant.LUMO_CONTRAST : ButtonVariant.LUMO_PRIMARY);
        if (done) btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btn.addClickListener(e -> action.run());

        card.add(header, desc, btn);
        return card;
    }

    private Span createBadge(String text, String color) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("padding", "2px 10px")
                .set("border-radius", "12px")
                .set("background", color)
                .set("color", "white")
                .set("font-size", "var(--lumo-font-size-xs)");
        return badge;
    }
}
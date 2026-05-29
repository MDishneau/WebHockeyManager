package com.hockeymanager.ui.views;

import com.hockeymanager.backend.model.NewsEvent;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.layouts.MainLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

@Route(value = "news", layout = MainLayout.class)
@PageTitle("News — Hockey Manager")
@AnonymousAllowed
public class NewsView extends VerticalLayout implements BeforeEnterObserver {

    private final GameSession session;

    public NewsView(GameSession session) {
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

        H2 title = new H2("📰 News Feed");
        add(title);

        List<NewsEvent> news = session.getNewsService()
                .getRecentForTeam(session.getUserTeam(), 50);

        if (news.isEmpty()) {
            Span empty = new Span("No news yet — simulate some games first.");
            empty.addClassNames(LumoUtility.TextColor.SECONDARY);
            add(empty);
            return;
        }

        NewsEvent.Category lastCategory = null;
        for (NewsEvent event : news) {
            if (event.getCategory() != lastCategory) {
                H3 catHeader = new H3(categoryLabel(event.getCategory()));
                catHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM,
                        LumoUtility.Margin.Bottom.XSMALL,
                        LumoUtility.TextColor.SECONDARY);
                add(catHeader);
                lastCategory = event.getCategory();
            }

            Div row = new Div();
            row.addClassNames(LumoUtility.Padding.Vertical.XSMALL);

            Span date = new Span("[" + event.getDate() + "] ");
            date.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            boolean isMyTeam = event.getRelatedTeam() == session.getUserTeam();
            Span headline = new Span(event.getHeadline());
            headline.addClassNames(LumoUtility.FontSize.SMALL);
            if (isMyTeam) {
                headline.addClassNames(LumoUtility.FontWeight.BOLD);
            }

            row.add(date, headline);
            add(row);

            // Subtle divider
            Hr hr = new Hr();
            hr.getStyle().set("margin", "2px 0").set("opacity", "0.3");
            add(hr);
        }
    }

    private String categoryLabel(NewsEvent.Category cat) {
        return switch (cat) {
            case GAME_RESULT -> "🏒 Game Results";
            case LEAGUE_NEWS -> "📢 League News";
        };
    }
}
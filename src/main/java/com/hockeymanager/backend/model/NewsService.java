package com.hockeymanager.backend.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NewsService {

    private final List<NewsEvent> events = new ArrayList<>();

    public void addEvent(NewsEvent event) {
        events.add(event);
    }

    public void addHeadline(GameDate date, String headline,
                            NewsEvent.Category category, Team relatedTeam) {
        events.add(new NewsEvent(date, headline, category, relatedTeam));
    }

    // All news, most recent first
    public List<NewsEvent> getAll() {
        List<NewsEvent> sorted = new ArrayList<>(events);
        sorted.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return sorted;
    }

    // Only news involving a specific team, most recent first
    public List<NewsEvent> getForTeam(Team team) {
        return getAll().stream()
                .filter(e -> e.getRelatedTeam() == team
                        || e.getCategory() == NewsEvent.Category.LEAGUE_NEWS)
                .collect(Collectors.toList());
    }

    public List<NewsEvent> getRecent(int n) {
        List<NewsEvent> all = getAll();
        return all.subList(0, Math.min(n, all.size()));
    }

    public List<NewsEvent> getRecentForTeam(Team team, int n) {
        List<NewsEvent> all = getForTeam(team);
        return all.subList(0, Math.min(n, all.size()));
    }
}
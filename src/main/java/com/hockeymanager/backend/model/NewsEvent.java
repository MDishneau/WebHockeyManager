package com.hockeymanager.backend.model;

public class NewsEvent {

    public enum Category {
        GAME_RESULT, LEAGUE_NEWS
    }

    private final GameDate date;
    private final String headline;
    private final Category category;
    private final Team relatedTeam; // nullable

    public NewsEvent(GameDate date, String headline, Category category, Team relatedTeam) {
        this.date        = date;
        this.headline    = headline;
        this.category    = category;
        this.relatedTeam = relatedTeam;
    }

    public GameDate getDate()      { return date; }
    public String getHeadline()    { return headline; }
    public Category getCategory()  { return category; }
    public Team getRelatedTeam()   { return relatedTeam; }

    @Override
    public String toString() {
        return "[" + date + "] " + headline;
    }
}
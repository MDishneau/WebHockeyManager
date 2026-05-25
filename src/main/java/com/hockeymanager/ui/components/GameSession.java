package com.hockeymanager.ui.components;

import com.hockeymanager.backend.manager.SeasonManager;
import com.hockeymanager.backend.model.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

@Component
@UIScope
public class GameSession {
    public League league;
    private SeasonManager seasonManager;
    private Team userTeam;
    // getters/setters
}


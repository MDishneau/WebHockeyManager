package com.hockeymanager.backend.generator;

import com.hockeymanager.backend.model.*;
import org.springframework.stereotype.Service;

import java.util.Random;
@Service
public class TeamGenerator {

    /*
     * Full 23-man NHL roster breakdown:
     *   4 lines × 3 forwards = 12 forwards
     *   3 pairs × 2 defense  =  6 defensemen
     *   2 goalies             =  2 goalies
     *   3 extras (healthy scratches / call-ups) = mix of F/D
     *   Total = 23
     *
     * Position distribution for 12 forwards:
     *   4 Centers, 4 Left Wings, 4 Right Wings
     * Position distribution for 6 defense:
     *   3 Left Defense, 3 Right Defense
     * Extras: 2 forwards (1 C, 1 W), 1 defenseman
     */

    private static final int CENTERS     = 5;  // 4 roster + 1 extra
    private static final int LEFT_WINGS  = 4;
    private static final int RIGHT_WINGS = 4;
    private static final int LEFT_DEF    = 4;  // 3 roster + 1 extra
    private static final int RIGHT_DEF   = 3;
    private static final int GOALIES     = 2;
    // Total = 5+4+4+4+3+2 = 22... one extra wing to hit 23
    private static final int EXTRA_WINGS = 1;

    private final PlayerGenerator playerGenerator;

    public TeamGenerator(PlayerGenerator playerGenerator) {
        this.playerGenerator = playerGenerator;
    }



    public void populateRoster(Team team) {
        // Forwards
        for (int i = 0; i < CENTERS;     i++) team.addPlayer(playerGenerator.generate(Position.CENTER));
        for (int i = 0; i < LEFT_WINGS;  i++) team.addPlayer(playerGenerator.generate(Position.LEFT_WING));
        for (int i = 0; i < RIGHT_WINGS + EXTRA_WINGS; i++) team.addPlayer(playerGenerator.generate(Position.RIGHT_WING));

        // Defense
        for (int i = 0; i < LEFT_DEF;  i++) team.addPlayer(playerGenerator.generate(Position.LEFT_DEFENSE));
        for (int i = 0; i < RIGHT_DEF; i++) team.addPlayer(playerGenerator.generate(Position.RIGHT_DEFENSE));

        // Goalies
        for (int i = 0; i < GOALIES; i++) team.addPlayer(playerGenerator.generate(Position.GOALIE));
    }
}

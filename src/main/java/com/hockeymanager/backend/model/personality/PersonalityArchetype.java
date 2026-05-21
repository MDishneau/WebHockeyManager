package com.hockeymanager.backend.model.personality;

/**
 * Broad personality type. Affects locker room chemistry, media events,
 * contract negotiations, and future coaching/morale systems.
 */
public enum PersonalityArchetype {

    LEADER,           // boosts teammates' morale, takes pay cuts for winners
    QUIET_PRO,        // consistent, low drama, doesn't rock the boat
    LOCKER_ROOM_GUY,  // high chemistry impact, beloved by teammates
    HOT_HEAD,         // high compete but risks penalties, morale swings
    MERCENARY,        // follows the money, low loyalty, high production focus
    YOUNG_GUN,        // high upside, inconsistent, responds well to mentors
    VETERAN_PRESENCE  // declining physically but stabilizes young rosters
}
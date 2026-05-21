package com.hockeymanager.backend.model.attributes;

public class GoalieAttributes {
    private final int reflexes;
    private final int positioning;
    private final int glove;
    private final int blocker;
    private final int reboundControl;
    private final int pokeCheck;
    private final int fiveHole;

    public GoalieAttributes(int reflexes, int positioning, int glove, int blocker,
                            int reboundControl, int pokeCheck, int fiveHole) {
        this.reflexes      = reflexes;
        this.positioning   = positioning;
        this.glove         = glove;
        this.blocker       = blocker;
        this.reboundControl = reboundControl;
        this.pokeCheck     = pokeCheck;
        this.fiveHole      = fiveHole;
    }

    public int getReflexes()       { return reflexes; }
    public int getPositioning()    { return positioning; }
    public int getGlove()          { return glove; }
    public int getBlocker()        { return blocker; }
    public int getReboundControl() { return reboundControl; }
    public int getPokeCheck()      { return pokeCheck; }
    public int getFiveHole()       { return fiveHole; }

    public int getAverage() {
        return (reflexes + positioning + glove + blocker + reboundControl + pokeCheck + fiveHole) / 7;
    }

    // Composite: overall goalie stopping power
    public int getStoppingPower() {
        return (int) (reflexes * 0.25 + positioning * 0.25 + glove * 0.15
                + blocker * 0.15 + fiveHole * 0.10 + reboundControl * 0.05 + pokeCheck * 0.05);
    }
}
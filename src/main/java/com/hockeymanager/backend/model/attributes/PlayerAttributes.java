package com.hockeymanager.backend.model.attributes;

public class PlayerAttributes {
    private final SkatingAttributes  skating;
    private final ShootingAttributes shooting;
    private final PuckSkillsAttributes puckSkills;
    private final PassingAttributes  passing;
    private final DefenseAttributes  defense;
    private final PhysicalAttributes physical;
    private final MentalAttributes   mental;
    private final GoalieAttributes   goalie; // null for skaters

    // Constructor for skaters
    public PlayerAttributes(SkatingAttributes skating, ShootingAttributes shooting,
                            PuckSkillsAttributes puckSkills, PassingAttributes passing,
                            DefenseAttributes defense, PhysicalAttributes physical,
                            MentalAttributes mental) {
        this(skating, shooting, puckSkills, passing, defense, physical, mental, null);
    }

    // Constructor for goalies
    public PlayerAttributes(SkatingAttributes skating, ShootingAttributes shooting,
                            PuckSkillsAttributes puckSkills, PassingAttributes passing,
                            DefenseAttributes defense, PhysicalAttributes physical,
                            MentalAttributes mental, GoalieAttributes goalie) {
        this.skating    = skating;
        this.shooting   = shooting;
        this.puckSkills = puckSkills;
        this.passing    = passing;
        this.defense    = defense;
        this.physical   = physical;
        this.mental     = mental;
        this.goalie     = goalie;
    }

    public SkatingAttributes   getSkating()    { return skating; }
    public ShootingAttributes  getShooting()   { return shooting; }
    public PuckSkillsAttributes getPuckSkills(){ return puckSkills; }
    public PassingAttributes   getPassing()    { return passing; }
    public DefenseAttributes   getDefense()    { return defense; }
    public PhysicalAttributes  getPhysical()   { return physical; }
    public MentalAttributes    getMental()     { return mental; }
    public GoalieAttributes    getGoalie()     { return goalie; }
    public boolean isGoalie()                  { return goalie != null; }
}
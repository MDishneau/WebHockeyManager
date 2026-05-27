package com.hockeymanager.ui.views;

import com.hockeymanager.backend.model.*;
import com.hockeymanager.backend.model.attributes.*;
import com.hockeymanager.ui.components.GameSession;
import com.hockeymanager.ui.layouts.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "roster", layout = MainLayout.class)
@PageTitle("My Roster — Hockey Manager")
@AnonymousAllowed
public class RosterView extends VerticalLayout implements BeforeEnterObserver {

    private final GameSession session;

    public RosterView(GameSession session) {
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
        Team team = session.getUserTeam();

        H2 title = new H2(team.getFullName() + " — Roster");
        Span record = new Span("Record: " + team.getWins() + "-" + team.getLosses()
                + "-" + team.getOtLosses() + "  |  Points: " + team.getPoints());
        record.addClassNames(LumoUtility.TextColor.SECONDARY);

        add(title, record);
        add(buildPositionGroup(" FORWARDS", team,
                List.of(Position.CENTER, Position.LEFT_WING, Position.RIGHT_WING)));
        add(buildPositionGroup(" DEFENSEMEN", team,
                List.of(Position.LEFT_DEFENSE, Position.RIGHT_DEFENSE)));
        add(buildPositionGroup(" GOALIES", team,
                List.of(Position.GOALIE)));
    }

    private VerticalLayout buildPositionGroup(String label, Team team, List<Position> positions) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(false);
        section.setPadding(false);
        section.setWidthFull();

        H3 groupTitle = new H3(label);
        groupTitle.addClassNames(LumoUtility.Margin.Bottom.XSMALL);
        section.add(groupTitle);

        List<Player> group = team.getRoster().stream()
                .filter(p -> positions.contains(p.getPosition()))
                .sorted(Comparator.comparingInt(Player::getOverall).reversed())
                .collect(Collectors.toList());

        boolean isGoalie = positions.contains(Position.GOALIE);
        Grid<Player> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.setItems(group);

        grid.addColumn(Player::getName).setHeader("Name").setFlexGrow(2).setSortable(true);
        grid.addColumn(p -> p.getPosition().toString()).setHeader("Position").setFlexGrow(1);
        grid.addColumn(Player::getAge).setHeader("Age").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Player::getOverall).setHeader("OVR").setWidth("60px").setFlexGrow(0).setSortable(true);

        if (isGoalie) {
            grid.addColumn(Player::getWins).setHeader("W").setWidth("55px").setFlexGrow(0);
            grid.addColumn(Player::getLosses).setHeader("L").setWidth("55px").setFlexGrow(0);
            grid.addColumn(p -> String.format("%.3f", p.getSavePercentage())).setHeader("SV%").setFlexGrow(0);
            grid.addColumn(p -> String.format("%.2f", p.getGAA())).setHeader("GAA").setFlexGrow(0);
        } else {
            grid.addColumn(Player::getGoals).setHeader("G").setWidth("55px").setFlexGrow(0);
            grid.addColumn(Player::getAssists).setHeader("A").setWidth("55px").setFlexGrow(0);
            grid.addColumn(Player::getPoints).setHeader("PTS").setWidth("60px").setFlexGrow(0).setSortable(true);
            grid.addColumn(Player::getPlusMinus).setHeader("+/-").setWidth("60px").setFlexGrow(0);
        }

        grid.addComponentColumn(p -> {
            Button btn = new Button("View");
            btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            btn.addClickListener(e -> showPlayerDetail(p));
            return btn;
        }).setHeader("Detail").setWidth("80px").setFlexGrow(0);

        // Row click also opens detail
        grid.addItemClickListener(e -> showPlayerDetail(e.getItem()));

        section.add(grid);
        return section;
    }

    private void showPlayerDetail(Player p) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        dialog.setHeaderTitle(p.getName() + "  |  " + p.getPosition()
                + "  |  Age: " + p.getAge() + "  |  OVR: " + p.getOverall());

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);

        // Personality row
        Span personality = new Span("Personality: " + p.getPersonality().getArchetype()
                + "  |  Morale: " + p.getPersonality().getMorale()
                + " (" + p.getPersonality().getMoraleLevel() + ")");
        personality.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        content.add(personality);

        // Contract
        if (p.getContract() != null) {
            Span contract = new Span("Contract: " + p.getContract());
            contract.addClassNames(LumoUtility.FontSize.SMALL);
            content.add(contract);
        }

        Hr hr = new Hr();
        content.add(hr);

        // Attributes
        PlayerAttributes a = p.getAttributes();
        if (p.getPosition() == Position.GOALIE) {
            GoalieAttributes g = a.getGoalie();
            content.add(buildAttrBlock(" Goalie",
                    new String[]{"Reflexes", "Positioning", "Glove", "Blocker", "Rebound Ctrl", "Poke Check", "5-Hole"},
                    new int[]{g.getReflexes(), g.getPositioning(), g.getGlove(), g.getBlocker(),
                            g.getReboundControl(), g.getPokeCheck(), g.getFiveHole()}));
        } else {
            content.add(buildAttrBlock(" Skating",
                    new String[]{"Speed", "Acceleration", "Agility", "Stamina"},
                    new int[]{a.getSkating().getSpeed(), a.getSkating().getAcceleration(),
                            a.getSkating().getAgility(), a.getSkating().getStamina()}));
            content.add(buildAttrBlock(" Shooting",
                    new String[]{"Power", "Accuracy", "One-Timer", "Wrist Shot", "Slap Shot"},
                    new int[]{a.getShooting().getPower(), a.getShooting().getAccuracy(),
                            a.getShooting().getOneTimer(), a.getShooting().getWristShot(),
                            a.getShooting().getSlapShot()}));
            content.add(buildAttrBlock(" Puck Skills",
                    new String[]{"Handling", "Deking", "Board Battles", "Puck Protection"},
                    new int[]{a.getPuckSkills().getHandling(), a.getPuckSkills().getDeking(),
                            a.getPuckSkills().getBoardBattles(), a.getPuckSkills().getPuckProtection()}));
            content.add(buildAttrBlock(" Passing",
                    new String[]{"Accuracy", "Vision", "Saucer Pass", "One-Touch"},
                    new int[]{a.getPassing().getAccuracy(), a.getPassing().getVision(),
                            a.getPassing().getSaucerPass(), a.getPassing().getOneTouch()}));
            content.add(buildAttrBlock(" Defense",
                    new String[]{"Positioning", "Stick Check", "Body Check", "Shot Block"},
                    new int[]{a.getDefense().getPositioning(), a.getDefense().getStickChecking(),
                            a.getDefense().getBodyChecking(), a.getDefense().getShotBlocking()}));
            content.add(buildAttrBlock(" Physical",
                    new String[]{"Strength", "Aggression", "Balance", "Fighting"},
                    new int[]{a.getPhysical().getStrength(), a.getPhysical().getAggression(),
                            a.getPhysical().getBalance(), a.getPhysical().getFighting()}));
        }
        content.add(buildAttrBlock(" Mental",
                new String[]{"Composure", "Compete", "Leadership", "Hockey IQ", "Clutch"},
                new int[]{a.getMental().getComposure(), a.getMental().getCompeteLevel(),
                        a.getMental().getLeadership(), a.getMental().getHockeyIQ(),
                        a.getMental().getClutch()}));

        Span priorities = new Span("Contract Priorities: " + p.getPersonality().getContractPriorities());
        priorities.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
        content.add(priorities);

        dialog.add(content);
        Button close = new Button("Close", e -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(close);
        dialog.open();
    }

    private Div buildAttrBlock(String label, String[] names, int[] values) {
        Div block = new Div();
        block.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        H4 title = new H4(label);
        title.addClassNames(LumoUtility.Margin.Bottom.XSMALL, LumoUtility.Margin.Top.SMALL);
        block.add(title);

        for (int i = 0; i < names.length; i++) {
            HorizontalLayout row = new HorizontalLayout();
            row.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            row.setWidthFull();

            Span name = new Span(names[i]);
            name.getStyle().set("width", "140px").set("flex-shrink", "0");
            name.addClassNames(LumoUtility.FontSize.SMALL);

            // Rating bar
            int val = values[i];
            Div barOuter = new Div();
            barOuter.getStyle()
                    .set("flex", "1")
                    .set("height", "8px")
                    .set("background", "var(--lumo-contrast-10pct)")
                    .set("border-radius", "4px")
                    .set("overflow", "hidden");
            Div barFill = new Div();
            barFill.getStyle()
                    .set("width", val + "%")
                    .set("height", "100%")
                    .set("background", ratingColor(val))
                    .set("border-radius", "4px");
            barOuter.add(barFill);

            Span valSpan = new Span(String.valueOf(val));
            valSpan.getStyle().set("width", "30px").set("text-align", "right").set("flex-shrink", "0");
            valSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD);

            row.add(name, barOuter, valSpan);
            block.add(row);
        }
        return block;
    }

    private String ratingColor(int val) {
        if (val >= 80) return "var(--lumo-success-color)";
        if (val >= 65) return "#f59e0b";
        return "var(--lumo-error-color)";
    }
}
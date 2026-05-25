package com.hockeymanager.backend.generator;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class NameGenerator {

    private static final List<String> FIRST_NAMES = List.of(
            "Alex", "Anders", "Anton", "Axel", "Blake", "Bo", "Brady", "Brayden",
            "Brett", "Cam", "Carter", "Cole", "Colin", "Connor", "Damon", "Daniel",
            "Derek", "Dmitri", "Drew", "Dylan", "Elias", "Ethan", "Evan", "Filip",
            "finn", "Gage", "Garrett", "Hunter", "Ivan", "Jack", "Jacob", "Jake",
            "James", "Jared", "Jason", "Jayden", "Jesse", "Joel", "John", "Jonas",
            "Jonathan", "Jordan", "Josh", "Julian", "Justin", "Karl", "Kasperi",
            "Kevin", "Kyle", "Liam", "Logan", "Lucas", "Luke", "Marcus", "Mark",
            "Mason", "Matt", "Max", "Mikael", "Mike", "Milan", "Nathan", "Nick",
            "Nikita", "Noah", "Nolan", "Oliver", "Owen", "Patrick", "Pavel", "Peter",
            "Phillip", "Rasmus", "Riley", "Robin", "Roman", "Ryan", "Sam", "Scott",
            "Sean", "Sebastian", "Shane", "Simon", "Stefan", "Taylor", "Thomas",
            "Tim", "Todd", "Travis", "Trevor", "Tyler", "Victor", "Wade", "Will",
            "William", "Wyatt", "Yannick", "Zach"
    );

    private static final List<String> LAST_NAMES = List.of(
            "Anderson", "Berg", "Brown", "Campbell", "Carter", "Christensen",
            "Clark", "Cole", "Collins", "Cooper", "Davidson", "Davis", "Dubois",
            "Ekman", "Ellis", "Evans", "Forsberg", "Foster", "Gagne", "Garcia",
            "Grant", "Green", "Hall", "Hansen", "Harris", "Hart", "Hedman",
            "Hill", "Hoffman", "Hughes", "Jackson", "Jacobsen", "Jensen", "Johnson",
            "Jones", "Karlsson", "Kelly", "King", "Koivu", "Larsen", "Larsson",
            "Lee", "Lewis", "Lindgren", "Lindqvist", "Lundqvist", "Martin", "Martinez",
            "Miller", "Mitchell", "Moore", "Morgan", "Morris", "Murphy", "Murray",
            "Nelson", "Nielsen", "Nilsson", "Nylander", "Olofsson", "Olsen",
            "Olsson", "Parker", "Paterson", "Persson", "Peters", "Peterson",
            "Phillips", "Price", "Reid", "Richards", "Richardson", "Robinson",
            "Rogers", "Ross", "Scott", "Shaw", "Simpson", "Sjogren", "Skinner",
            "Smith", "Sorensen", "Stein", "Stewart", "Sullivan", "Svensson",
            "Taylor", "Thomas", "Thompson", "Thomson", "Turner", "Walker",
            "Ward", "Watson", "White", "Williams", "Wilson", "Wood", "Wright",
            "Young", "Zimmermann"
    );

    private final Random random = new Random();

    public NameGenerator() {

    }

    public String generateName() {
        String first = FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()));
        String last  = LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
        // Capitalize first letter properly in case of lowercase entry
        first = Character.toUpperCase(first.charAt(0)) + first.substring(1);
        return first + " " + last;
    }
}
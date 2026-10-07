package Application.Actor;

// IMPORTS
import java.util.List;
import java.util.ArrayList;

/**
 * A test class for creating a static list of weapons.
 * This class appears to be for testing purposes and is not currently used
 * in the main game logic.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
* @author Rosaline Liu
 */
public class Weapons {
    // PROPERTIES
    private static List<Weapon> testWeapons = new ArrayList<>();
    
    /**
     * Constructs a Weapons object and adds a single test weapon to the static list.
     */
    public Weapons() {
        testWeapons.add(new Weapon(ClassType.SOLDIER, "test", 0, "", 1));

}
}

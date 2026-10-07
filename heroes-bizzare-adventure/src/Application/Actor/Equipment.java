package Application.Actor;

// IMPORTS
import java.util.HashMap;

/**
 * Represents a piece of equipment that can be worn by an actor.
 * This is a placeholder for a potential armor/gear system.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Equipment extends PortableObject {
    // PROPERTIES
    Slots slot;
    HashMap<EffectType, Integer> statsModifier;

    /**
     * Constructs a new piece of Equipment.
     * @param slot The equipment slot this item occupies (e.g., HEAD, CHEST).
     * @param statsMap A map of status effects and their magnitudes provided by this item.
     */
    public Equipment(Slots slot, HashMap<EffectType, Integer> statsMap) {
        this.slot = slot;
        this.statsModifier = statsMap;
    }

    /**
     * Gets the slot this equipment belongs to.
     * @return The equipment slot.
     */
    public Slots getSlot() {
        return slot;
    }

    /**
     * Gets the map of stat modifiers provided by this equipment.
     * @return A HashMap where the key is the EffectType and the value is the modifier amount.
     */
    public HashMap<EffectType, Integer>  getModifiers() {
        return statsModifier;
    }

}
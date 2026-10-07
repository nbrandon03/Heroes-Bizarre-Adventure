package Application.Actor;

// IMPORTS

/**
 * Defines the types of status effects that can be applied to an actor.
 * This is part of a potential status effect system that is not fully implemented.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public enum EffectType {
    /** Increases health. */
    HEAL,
    /** Provides a temporary damage shield. */
    SHIELD,
    /** Increases mana or a similar resource. */
    MANA,
    /** Increases movement speed. */
    SPEED
}
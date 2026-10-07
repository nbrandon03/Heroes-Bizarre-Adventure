package Application.Actor;

// IMPORTS

/**
 * Represents a temporary effect applied to an actor (buff, debuff, status condition).
 * Effects are sorted by expiration time (earliest first).
 * This is part of a potential status effect system that is not fully implemented.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class EffectData implements Comparable<EffectData> {
    // PROPERTIES
    EffectType type;
    int magnitude;
    double endTime;
    boolean isStackable;

    /**
     * Constructs a new effect.
     * @param type The effect type (e.g., HEAL, SPEED).
     * @param magnitude The strength or intensity of the effect.
     * @param endTime The timestamp (in milliseconds) when this effect expires.
     * @param stackable True if multiple instances of this effect can stack.
     */
    public EffectData(EffectType type, int magnitude, double endTime, boolean stackable) {
        this.type = type;
        this.magnitude = magnitude;
        this.endTime = endTime;
        this.isStackable = stackable;
    }

    /**
     * @return The type of effect.
     */
    public EffectType getType() {
        return type;
    }

    /**
     * @return The expiration time in milliseconds.
     */
    public double getEndTime() {
        return this.endTime;
    }

    /**
     * @return The magnitude of the effect.
     */
    public int getMagnitude() {
        return magnitude;
    }

    /** @return true if stackable */
    public boolean isStackable() {
        return isStackable;
    }

    /**
     * Compares effects by expiration time (earliest first).
     * @param o The other EffectData to compare against.
     * @return A negative integer, zero, or a positive integer as this effect expires before, at the same time as, or after the specified effect.
     */
    @Override
    public int compareTo(EffectData o) {
        return Double.compare(this.endTime, o.getEndTime());
    }
}
package Application.Actor;

// IMPORTS

/**
 * Represents a weapon that can be equipped by the player.
 * A weapon is a type of PortableObject that provides a damage bonus.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Weapon extends PortableObject{
    // WEAPON PROPERTIES
    private ClassType weaponClass;
    private String name;
    private int damageBonus;
    private String iconPath; // Path to the inventory icon for this weapon.
    private int shopLevel;

    /**
     * Constructs a new Weapon.
     * @param weaponClass The class type that can use this weapon.
     * @param name The display name of the weapon.
     * @param damageBonus The amount of extra damage this weapon provides.
     * @param iconPath The path to the weapon's icon image.
     * @param shopLevel The tier or level of the weapon.
     */
    public Weapon(ClassType weaponClass, String name, int damageBonus, String iconPath, int shopLevel) {
        this.weaponClass = weaponClass;
        this.name = name;
        this.damageBonus = damageBonus;
        this.iconPath = iconPath;
        this.shopLevel = shopLevel;
    }

    /**
     * @return The class type that can use this weapon.
     */
    public ClassType getClassType() {
        return weaponClass;
    }

    /**
     * @return The display name of the weapon.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The damage bonus provided by this weapon.
     */
    public int getDamage() {
        return damageBonus;
    }

    /**
     * @return The file path for the weapon's inventory icon.
     */
    public String getIconPath() {
        return iconPath;
    }

    /**
     * @return The shop tier or level of the weapon.
     */
    public int shopLevel() {
        return shopLevel;
    }
    
}

package Application;

// IMPORTS
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.json.JSONObject;
import Application.Actor.ClassType;
// Import Mobs for attack interactions
import Application.Actor.PortableObject;
import Application.Actor.Weapon;

import java.util.ArrayList;
import java.util.List;
import Application.Mobs;

/**
 * Represents the player character in the game.
 * This class manages the player's stats, animations, inventory, and interactions
 * with the game world, such as attacking mobs and taking damage.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Player {
    // CONSTANTS
    private static final int MAX_HEALTH = 100;
    private static final int MAX_STAMINA = 100;
    private static final int REGEN_AMOUNT = 10; // per second

    // CORE STATS
    private int health;
    private int stamina;
    private int agility;
    private int baseAttack;
    private int attack; // This is the final, calculated attack stat.
    private int defense;
    private int score;
    private int numCoins;
    private int dayCounter;
    private boolean isDead;
    private boolean isDeadAnimationPlayed;
    private ClassType classType;

    // ANIMATION & UI
    private AnimationState currentState;
    private ImageView playerImageView;
    private Timeline playerAnimation;
    private int currentFrameIndex;
    private Timeline regenTimeline;

    // INVENTORY & EQUIPMENT
    private List<PortableObject> inventory;
    private Weapon equippedWeapon;

    /**
     * Defines the possible animation states for the player.
     */
    public enum AnimationState {
        IDLE,
        WALK,
        ATTACK,
        HURT,
        DEATH
    }

    // ANIMATION FRAMES
    private String[] walkFrames = {
        "Assets/PlayerClasses/Walk/0.png",
        "Assets/PlayerClasses/Walk/1.png",
        "Assets/PlayerClasses/Walk/2.png",
        "Assets/PlayerClasses/Walk/3.png",
        "Assets/PlayerClasses/Walk/4.png",
        "Assets/PlayerClasses/Walk/5.png",
        "Assets/PlayerClasses/Walk/6.png",
        "Assets/PlayerClasses/Walk/7.png"
    };

    private String[] deathFrames = {
        "Assets/PlayerClasses/Death/0.png",
        "Assets/PlayerClasses/Death/1.png",
        "Assets/PlayerClasses/Death/2.png",
        "Assets/PlayerClasses/Death/3.png",
        "Assets/PlayerClasses/Death/4.png"
    };

    private String[] hurtFrames = {
        "Assets/PlayerClasses/Hurt/0.png",
        "Assets/PlayerClasses/Hurt/1.png",
        "Assets/PlayerClasses/Hurt/2.png",
        "Assets/PlayerClasses/Hurt/3.png"
    };

    private String[] archerATKFrames = {
        "Assets/PlayerClasses/ArcherATK/0.png",
        "Assets/PlayerClasses/ArcherATK/1.png",
        "Assets/PlayerClasses/ArcherATK/2.png",
        "Assets/PlayerClasses/ArcherATK/3.png",
        "Assets/PlayerClasses/ArcherATK/4.png",
        "Assets/PlayerClasses/ArcherATK/5.png",
        "Assets/PlayerClasses/ArcherATK/6.png",
        "Assets/PlayerClasses/ArcherATK/7.png"
    };

    private String[] soldierATKFrames = {
        "Assets/PlayerClasses/SoldierATK/0.png",
        "Assets/PlayerClasses/SoldierATK/1.png",
        "Assets/PlayerClasses/SoldierATK/2.png",
        "Assets/PlayerClasses/SoldierATK/3.png",
        "Assets/PlayerClasses/SoldierATK/4.png",
        "Assets/PlayerClasses/SoldierATK/5.png"
    };

    /**
     * The core animation method that plays a sequence of frames.
     * @param frames An array of paths to the image frames.
     * @param millisPerFrame The duration each frame is displayed.
     * @param loop Whether the animation should loop.
     * @param state The animation state this represents.
     */
    private void startAnimation(String[] frames, int millisPerFrame, boolean loop, AnimationState state) {
        if (playerAnimation == null) {
            playerAnimation = new Timeline();
        }
        if (currentState == state && loop) {
            return;
        }
        playerAnimation.stop();
        playerAnimation.getKeyFrames().clear();
        currentState = state;
        currentFrameIndex = 0;

        KeyFrame kf = new KeyFrame(Duration.millis(millisPerFrame), e -> {
            if (currentFrameIndex < frames.length) {
                playerImageView.setImage(loadImage(frames[currentFrameIndex]));
                currentFrameIndex++;
            } else if (loop) {
                currentFrameIndex = 0;
                playerImageView.setImage(loadImage(frames[currentFrameIndex]));
                currentFrameIndex++;
            }
        });

        playerAnimation.getKeyFrames().add(kf);
        playerAnimation.setCycleCount(loop ? Timeline.INDEFINITE : frames.length + 1);
        playerAnimation.setOnFinished(event -> {
            if (!loop && frames.length > 0) {
                playerImageView.setImage(loadImage(frames[frames.length - 1]));
            }
        });
        playerAnimation.play();
    }

    /**
     * Starts the player's walking animation.
     */
    public void startWalk() {
        startAnimation(walkFrames, 120, true, AnimationState.WALK);
    }

    /**
     * Stops any current animation and shows the player's idle frame.
     */
    public void showIdle() {
        if (playerAnimation != null) {
            playerAnimation.stop();
        }
        playerImageView.setImage(loadImage(walkFrames[0]));
        currentState = AnimationState.IDLE;
    }

    /**
     * Starts the player's attack animation based on their class.
     */
    public void startAttack() {
        String[] frames = classType == ClassType.ARCHER ? archerATKFrames : soldierATKFrames;
        startAnimation(frames, 140, false, AnimationState.ATTACK);
    }

    /**
     * Perform an attack against a mob: plays the attack animation and applies damage.
     * Damage application is immediate; adjust to delay if frame-based timing is desired.
     * @param target the mob to attack
     */
    public void attackMob(Mobs target) {
        if (target == null) return;
        if (isDead || target.isDead()) return;
        // Trigger player attack animation
        startAttack();
        // Apply damage using player's attack stat
        target.takeDamage(attack);
        // Optional stamina cost
        spendStamina(5);
    }

    /**
     * Plays the hurt animation.
     */
    public void startHurt() {
        startAnimation(hurtFrames, 160, false, AnimationState.HURT);
    }

    /**
     * Plays the death animation.
     */
    public void startDeath() {
        if (isDeadAnimationPlayed) return;
        isDeadAnimationPlayed = true;
        startAnimation(deathFrames, 200, false, AnimationState.DEATH);
    }

    /**
     * Constructs a new Player with default stats and initializes animations.
     */
    public Player() {
        this.health = MAX_HEALTH;
        this.stamina = MAX_STAMINA;
        this.agility = 10; // Agility affects movement speed.
        this.baseAttack = 10; // Default base attack.
        this.attack = this.baseAttack; // Initial attack is the base attack.
        this.defense = 10;
        this.score = 0; 
        this.numCoins = 0;
        this.dayCounter = 0;
        this.isDead = false;
        this.isDeadAnimationPlayed = false;
        this.classType = ClassType.SOLDIER;
        this.currentState = AnimationState.IDLE;
        this.inventory = new ArrayList<>();
        this.equippedWeapon = null;

        this.playerImageView = new ImageView();
        this.playerImageView.setFitWidth(200);
        this.playerImageView.setFitHeight(200);
        this.playerImageView.setPreserveRatio(true);
        this.playerImageView.setSmooth(true);
        this.playerImageView.setTranslateY(80);

        this.playerAnimation = new Timeline();
        startRegeneration();
    }

    /**
     * Loads an image from the project's assets.
     * @param relativePath The path to the image relative to the assets folder.
     */
    private Image loadImage(String relativePath) {
        String cp = "/" + relativePath;
        try {
            return new Image(getClass().getResourceAsStream(cp), 200, 200, true, true);
        } catch (Exception ex) {
            return new Image("file:" + "src/" + relativePath, 200, 200, true, true);
        }
    }

    /**
     * @return The ImageView node representing the player.
     */
    public ImageView getNode() {
        return playerImageView;
    }

    // GETTERS
    public int getHealth() { return health; }
    public int getStamina() { return stamina; }
    public int getAgility() { return agility; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getScore() { return score; }
    public int getNumCoins() { return numCoins; }
    public int getDayCounter() { return dayCounter; }
    public boolean isDead() { return isDead; }
    public ClassType getClassType() { return classType; }
    public AnimationState getCurrentState() { return currentState; }
    public int getMaxHealth() { return MAX_HEALTH; }
    public int getMaxStamina() { return MAX_STAMINA; }
    public List<PortableObject> getInventory() { return inventory; }
    public Weapon getEquippedWeapon() { return equippedWeapon; }

    // SETTERS
    public void setHealth(int h) {
        health = Math.max(0, Math.min(h, MAX_HEALTH));
        if (health == 0) {
            isDead = true;
            startDeath();
        }
    }

    public void setStamina(int s) {
        stamina = Math.max(0, Math.min(s, MAX_STAMINA));
    }

    public void setAgility(int a) { agility = Math.max(0, a); }
    public void setAttack(int a) {
        // This now sets the base attack, and recalculates the final attack stat.
        baseAttack = Math.max(0, a);
        recalculateStats();
    }

    public void setDefense(int d) { defense = Math.max(0, d); }
    public void setScore(int s) { score = Math.max(0, s); }
    public void setNumCoins(int c) { numCoins = Math.max(0, c); }
    public void setDayCounter(int dc) { dayCounter = Math.max(0, dc); }
    public void setClassType(ClassType ct) { if (ct != null) classType = ct; }
    public void setInventory(List<PortableObject> inventory) { this.inventory = inventory != null ? inventory : new ArrayList<>(); }

    /**
     * Reduces the player's health after calculating damage mitigation from defense.
     */
    public void takeDamage(int raw) {
        if (isDead) return;
        int mitigated = Math.max(0, raw - defense);
        if (mitigated > 0) {
            // Play getting hurt SFX when actual damage is taken
            Application.AudioManager.getInstance().playSfx("GETTING_HURT");
        }
        setHealth(health - mitigated);
        if (!isDead) startHurt();
    }

    /**
     * Increases the player's health.
     */
    public void heal(int amount) {
        if (amount > 0 && !isDead) setHealth(health + amount);
    }

    /**
     * Reduces the player's stamina.
     */
    public void spendStamina(int amount) {
        if (amount > 0) setStamina(stamina - amount);
    }

    /**
     * Equips a weapon from the inventory. If another weapon is already equipped,
     * it is unequipped first. The player's attack stat is then updated.
     *
     * @param weapon The weapon to equip. Must be in the player's inventory.
     */
    public void equipWeapon(Weapon weapon) {
        // Do nothing if the weapon is null or not in inventory.
        if (weapon == null || !inventory.contains(weapon)) {
            return;
        }

        // If a weapon is already equipped, unequip it first.
        if (this.equippedWeapon != null) {
            unequipWeapon();
        }

        // Set the new weapon and update stats.
        this.equippedWeapon = weapon;
        recalculateStats();
    }

    /**
     * Unequips the currently equipped weapon, removing its bonus from the player's
     * attack stat.
     */
    public void unequipWeapon() {
        if (this.equippedWeapon == null) {
            return; // Nothing to unequip.
        }

        this.equippedWeapon = null;
        recalculateStats();
    }

    /**
     * Recalculates the player's final stats (like attack) based on their
     * base stats and any bonuses from equipped items.
     */
    private void recalculateStats() {
        // Start with the base attack.
        int finalAttack = this.baseAttack;

        // Add bonus from the equipped weapon, if any.
        if (this.equippedWeapon != null) {
            finalAttack += this.equippedWeapon.getDamage();
        }

        // Add other bonuses from armor, effects, etc. here in the future.

        // Set the final calculated attack stat.
        this.attack = finalAttack;
    }

    /**
     * Adds an item to the player's inventory.
     */
    public void addToInventory(PortableObject item) {
        if (item != null) inventory.add(item);
    }

    /**
     * Removes an item from the player's inventory.
     */
    public void removeFromInventory(PortableObject item) {
        if (item != null) inventory.remove(item);
    }

    /**
     * Checks if the player's inventory contains at least one weapon.
     * @return true if a weapon is found, false otherwise.
     */
    public boolean hasWeaponInInventory() {
        for (PortableObject item : inventory) {
            if (item instanceof Weapon) {
                return true;
            }
        }
        return false;
    }

    /**
     * Increases the player's stamina.
     */
    public void restoreStamina(int amount) {
        if (amount > 0) setStamina(stamina + amount);
    }

    /**
     * Increases the player's coin count.
     */
    public void addCoins(int amount) {
        if (amount > 0) setNumCoins(numCoins + amount);
    }

    /**
     * Increases the player's score.
     */
    public void addScore(int amount) {
        if (amount > 0) setScore(score + amount);
    }

    /**
     * Increments the day counter.
     */
    public void nextDay() {
        setDayCounter(dayCounter + 1);
    }

    /**
     * Converts the player's state to a JSON object for saving.
     * @return A JSONObject representing the player.
     */
    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("health", health);
        o.put("stamina", stamina);
        o.put("agility", agility);
        o.put("attack", attack);
        o.put("defense", defense);
        o.put("score", score);
        o.put("numCoins", numCoins);
        o.put("dayCounter", dayCounter);
        o.put("isDead", isDead);
        o.put("classType", classType.name());
        return o;
    }

    /**
     * Creates the starting weapon based on the player's class, adds it to inventory,
     * to the inventory. This is called from the constructor.
     */
    public void initializeStartingWeapon() {
        Weapon startingWeapon;
        if (this.classType == ClassType.ARCHER) {
            // Create the starting Archer weapon.
            startingWeapon = new Weapon(ClassType.ARCHER, "Recurve Bow", 5, "Assets/Weapons/Archer/104.png", 1);
        } else {
            // Default to the Soldier/Warrior weapon.
            startingWeapon = new Weapon(ClassType.SOLDIER, "Iron Sword", 8, "Assets/Weapons/Warrior/1.png", 1);
        }
        // Add the newly created weapon to the player's inventory list.
        addToInventory(startingWeapon);
        // Also equip the starting weapon by default.
        equipWeapon(startingWeapon);
    }

    /**
     * Updates the player's state from a JSON object.
     * @param o The JSONObject to load from.
     */
    public void fromJson(JSONObject o) {
        if (o == null) return;
        setHealth(o.optInt("health", health));
        setStamina(o.optInt("stamina", stamina));
        setAgility(o.optInt("agility", agility));
        setAttack(o.optInt("attack", attack));
        setDefense(o.optInt("defense", defense));
        setScore(o.optInt("score", score));
        setNumCoins(o.optInt("numCoins", numCoins));
        setDayCounter(o.optInt("dayCounter", dayCounter));

        boolean dead = o.optBoolean("isDead", isDead);
        if (dead && !isDead) {
            isDead = true;
            startDeath();
        }

        try {
            ClassType ct = ClassType.valueOf(o.optString("classType", classType.name()));
            setClassType(ct);
        } catch (Exception ignored) {}
    }

    /**
     * Resets the flag that prevents the death animation from playing multiple times.
     */
    public void resetDeathAnimationFlag() {
        isDeadAnimationPlayed = false;
    }

    /**
     * Starts the passive health and stamina regeneration timeline.
     */
    private void startRegeneration() {
        regenTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!isDead && health < MAX_HEALTH) {
                setHealth(Math.min(health + REGEN_AMOUNT, MAX_HEALTH));
            }
            if (stamina < MAX_STAMINA) {
                setStamina(Math.min(stamina + REGEN_AMOUNT, MAX_STAMINA));
            }
        }));
        regenTimeline.setCycleCount(Timeline.INDEFINITE);
        regenTimeline.play();
    }

    /**
     * Stops the passive regeneration timeline.
     */
    public void stopRegeneration() {
        if (regenTimeline != null) {
            regenTimeline.stop();
        }
    }
}

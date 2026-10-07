package Application.Engine;

// IMPORTS
import Application.Mobs;

/**
 * Simple global game engine that tracks score, coins, time,
 * and location progress. Implemented as a single instance so every
 * part of the game uses the same shared engine.
 *
 * @author Rosaline Liu
 */
public class GameEngine {

    private static GameEngine instance;
    private long startTime;
    private int totalCoins;
    private int totalScore;
    private int totalSecsSpent;
    private String currentLocation;

    /**
     * Private constructor (Singleton).
     * Initializes counters and timers.
     */
    private GameEngine() {
        this.startTime = System.currentTimeMillis();
        this.totalSecsSpent = 0;
        this.totalCoins = 0;
        this.totalScore = 0;
        this.currentLocation = null;
    }

    /**
     * @return the single shared GameEngine instance.
     */
    public static GameEngine getInstance() { 
        if (instance == null) {
            instance = new GameEngine(); 
        }
        return instance; 
    }

    /**
     * Awards coins AND score when a mob is killed.
     * This is the main method to call when a mob dies.
     * 
     * @param mob the mob that was killed
     */
    public void onMobKilled(Mobs mob) {
        if (mob == null || !mob.isDead()) return;
        
        // Award coins
        int coins = mob.getCoinAmount();
        totalCoins += coins;
        
        // Award score
        int score = mob.getScoreAmount();
        totalScore += score;
        
        System.out.println("[GameEngine] Mob killed! +" + coins + " coins, +" + score + " score. Total score: " + totalScore);
    }

    /**
     * Adds coins (negative values subtract).
     * Total coins never goes below 0.
     * 
     * @param amount coins to add (positive) or spend (negative)
     */
    public void addCoins(int amount) {
        totalCoins = Math.max(0, totalCoins + amount);
    }

    /**
     * Adds raw score to the total.
     * 
     * @param amount score points to add
     */
    public void addScore(int amount) {
        totalScore += amount;
    }

    /**
     * Starts tracking a new location.
     * Resets the location timer.
     * 
     * @param locationName name of the location
     */
    public void startLocation(String locationName) {
        currentLocation = locationName;
        startTime = System.currentTimeMillis();
    }

    /**
     * Fully resets the game: score, coins, and timer.
     */
    public void resetGame() {
        startTime = System.currentTimeMillis();
        totalCoins = 0;
        totalScore = 0;
        totalSecsSpent = 0;
        currentLocation = null;
    }

    // --------------------------
    // Getters
    // --------------------------

    /** @return total coins collected */
    public int getTotalCoins() {
        return totalCoins;
    }

    /** @return total score across all activities */
    public int getTotalScore() {
        return totalScore;
    }

    /** @return seconds elapsed in the current location */
    public long getGameElapsed() {
        return getElapseTimeSecs();
    }

    /** @return formatted MM:SS of total time spent */
    public String getTotalTimeSpent() {
        return getFormattedTime(totalSecsSpent);
    }

    /** @return formatted MM:SS of current location time */
    public String getFormattedTime() {
        long secs = getGameElapsed();  
        long mins = secs / 60;          
        long s = secs % 60;             
        return String.format("%02d:%02d", mins, s); 
    }

    /**
     * Formats any raw seconds value into MM:SS.
     * 
     * @param totalSecs seconds to format
     * @return formatted time string (MM:SS)
     */
    public String getFormattedTime(int totalSecs) {
        long secs = totalSecs;
        long mins = secs / 60;          
        long s = secs % 60;             
        return String.format("%02d:%02d", mins, s); 
    }

    /** @return the name of the current location, or null */
    public String getCurrentLocation() {
        return currentLocation;
    }

    // --------------------------
    // Private Helpers
    // --------------------------

    /**
     * @return seconds passed since the current location started
     */
    private long getElapseTimeSecs() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }    
}
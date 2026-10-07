package Application.Engine;


// IMPORTS
import Application.AudioManager;
import Application.Actor.ClassType;
import Application.Actor.Equipment;
import Application.Actor.PortableObject;
import Application.Actor.Weapon;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified game state and settings persistence.
 * Holds audio volumes, player/class and simple progress fields.
 * Uses org.json for straightforward JSON save/load.
 *
 * @author Brandon Nguyen
 */
public class GameState {
    // Audio settings
    private float masterVolume = 1.0f;
    private float musicVolume = 0.8f;
    private float sfxVolume = 0.9f;

    // Gameplay state (minimal snapshot)
    private ClassType classType = ClassType.SOLDIER; // default now matches NewGame initial selection
    // Player stats snapshot
    private int health = 100;
    private int stamina = 100;
    private int agility = 10;
    /**
     * This is the BASE attack (without weapon bonus).
     * The final attack is recalculated on Player side when re-equipping.
     */
    private int attack = 10;
    private int defense = 10;
    private boolean isDead = false;
    private List<PortableObject> inventory = new ArrayList<>();
    private List<Equipment> equipment = new ArrayList<>();
    private int lives = 3;
    private int score = 0;
    private int coins = 0;
    private int time = 0; // seconds
    private String location = "Start"; // scene/location name
    // Locations where primary mob/boss has been defeated
    private java.util.Set<String> clearedLocations = new java.util.HashSet<>();

    // Index of the equipped weapon in inventory (-1 = none)
    private int equippedWeaponIndex = -1;

    // Accessors
    public float getMasterVolume() { return masterVolume; }
    public float getMusicVolume() { return musicVolume; }
    public float getSfxVolume() { return sfxVolume; }
    public void setMasterVolume(float v) { masterVolume = clamp01(v); }
    public void setMusicVolume(float v) { musicVolume = clamp01(v); }
    public void setSfxVolume(float v) { sfxVolume = clamp01(v); }

    public ClassType getClassType() { return classType; }
    public void setClassType(ClassType ct) { if (ct != null) classType = ct; }

    public int getHealth() { return health; }
    public int getStamina() { return stamina; }
    public int getAgility() { return agility; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public boolean getIsDead() { return isDead; }

    public List<PortableObject> getInventory() { return inventory; }
    public List<Equipment> getEquipment() { return equipment; }
    public int getLives() { return lives; }
    public int getScore() { return score; }
    public int getCoins() { return coins; }
    public int getTime() { return time; }
    public String getLocation() { return location; }
    public boolean isLocationCleared(String loc) { return loc != null && clearedLocations.contains(loc); }
    public void markLocationCleared(String loc) { if (loc != null && !loc.isEmpty()) clearedLocations.add(loc); }
    public java.util.Set<String> getClearedLocations() { return java.util.Collections.unmodifiableSet(clearedLocations); }

    public int getEquippedWeaponIndex() { return equippedWeaponIndex; }
    public void setEquippedWeaponIndex(int idx) { this.equippedWeaponIndex = idx; }

    public void setInventory(List<PortableObject> inv) { inventory = (inv != null) ? inv : new ArrayList<>(); }
    public void setEquipment(List<Equipment> eq) { equipment = (eq != null) ? eq : new ArrayList<>(); }
    public void setLives(int v) { lives = Math.max(0, v); }
    public void setScore(int v) { score = Math.max(0, v); }
    public void setCoins(int v) { coins = Math.max(0, v); }
    public void setTime(int v) { time = Math.max(0, v); }
    public void setLocation(String v) { if (v != null && !v.isEmpty()) location = v; }
    public void setHealth(int v) { health = Math.max(0, Math.min(v, 100)); }
    public void setStamina(int v) { stamina = Math.max(0, Math.min(v, 100)); }
    public void setAgility(int v) { agility = Math.max(0, v); }
    /** This is stored as base attack, not including weapon bonus. */
    public void setAttack(int v) { attack = Math.max(0, v); }
    public void setDefense(int v) { defense = Math.max(0, v); }
    public void setIsDead(boolean v) { isDead = v; }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    // Apply to AudioManager
    public void applyAudioTo(AudioManager am) {
        am.setMasterVolume(masterVolume);
        am.setMusicVolume(musicVolume);
        am.setSfxVolume(sfxVolume);
    }

    // Update from AudioManager
    public void captureAudioFrom(AudioManager am) {
        setMasterVolume(am.getMasterVolume());
        setMusicVolume(am.getMusicVolume());
        setSfxVolume(am.getSfxVolume());
    }

    // --- JSON persistence ---
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("masterVolume", masterVolume);
        obj.put("musicVolume", musicVolume);
        obj.put("sfxVolume", sfxVolume);
        obj.put("classType", classType.toString());
        obj.put("health", health);
        obj.put("stamina", stamina);
        obj.put("agility", agility);
        obj.put("attack", attack);     // base attack
        obj.put("defense", defense);
        obj.put("isDead", isDead);
        obj.put("lives", lives);
        obj.put("score", score);
        obj.put("coins", coins);
        obj.put("time", time);
        obj.put("location", location);
        obj.put("equippedWeaponIndex", equippedWeaponIndex);

        // Persist cleared locations
        JSONArray cleared = new JSONArray();
        for (String s : clearedLocations) cleared.put(s);
        obj.put("clearedLocations", cleared);

        // Inventory as structured JSON (currently only Weapon is supported)
        JSONArray inv = new JSONArray();
        for (PortableObject p : inventory) {
            if (p instanceof Weapon) {
                Weapon w = (Weapon) p;
                JSONObject wJson = new JSONObject();
                wJson.put("type", "WEAPON");
                wJson.put("classType", w.getClassType().name());
                wJson.put("name", w.getName());
                wJson.put("damage", w.getDamage());
                wJson.put("iconPath", w.getIconPath());
                inv.put(wJson);
            } else {
                // Fallback for unknown types – store just a debug string
                inv.put(p.toString());
            }
        }
        obj.put("inventory", inv);

        // Equipment still stored as strings (placeholder)
        JSONArray eq = new JSONArray();
        for (Equipment e : equipment) {
            eq.put(e.toString());
        }
        obj.put("equipment", eq);
        return obj;
    }

    public static GameState fromJson(JSONObject obj) {
        GameState gs = new GameState();
        if (obj == null) return gs;
        gs.masterVolume = (float)obj.optDouble("masterVolume", gs.masterVolume);
        gs.musicVolume = (float)obj.optDouble("musicVolume", gs.musicVolume);
        gs.sfxVolume = (float)obj.optDouble("sfxVolume", gs.sfxVolume);
        try { gs.classType = ClassType.valueOf(obj.optString("classType", gs.classType.toString())); } catch (Exception ignored) {}
        gs.health = obj.optInt("health", gs.health);
        gs.stamina = obj.optInt("stamina", gs.stamina);
        gs.agility = obj.optInt("agility", gs.agility);
        gs.attack = obj.optInt("attack", gs.attack); // base attack
        gs.defense = obj.optInt("defense", gs.defense);
        gs.isDead = obj.optBoolean("isDead", gs.isDead);
        gs.lives = obj.optInt("lives", gs.lives);
        gs.score = obj.optInt("score", gs.score);
        gs.coins = obj.optInt("coins", gs.coins);
        gs.time = obj.optInt("time", gs.time);
        gs.location = obj.optString("location", gs.location);
        gs.equippedWeaponIndex = obj.optInt("equippedWeaponIndex", -1);

        // Load cleared locations
        gs.clearedLocations = new java.util.HashSet<>();
        JSONArray cl = obj.optJSONArray("clearedLocations");
        if (cl != null) {
            for (int i = 0; i < cl.length(); i++) {
                String loc = cl.optString(i, null);
                if (loc != null && !loc.isEmpty()) gs.clearedLocations.add(loc);
            }
        }

        // Inventory: reconstruct Weapon objects
        gs.inventory = new ArrayList<>();
        JSONArray inv = obj.optJSONArray("inventory");
        if (inv != null) {
            for (int i = 0; i < inv.length(); i++) {
                Object elem = inv.get(i);
                if (elem instanceof JSONObject) {
                    JSONObject it = (JSONObject) elem;
                    String type = it.optString("type", "");
                    if ("WEAPON".equals(type)) {
                        ClassType ct;
                        try {
                            ct = ClassType.valueOf(it.optString("classType", ClassType.SOLDIER.name()));
                        } catch (Exception ex) {
                            ct = ClassType.SOLDIER;
                        }
                        String name = it.optString("name", "Unknown");
                        int dmg = it.optInt("damage", 0);
                        String icon = it.optString("iconPath", "");
                        int level = it.optInt("level", 1);
                        gs.inventory.add(new Weapon(ct, name, dmg, icon, level));
                    } else {
                        // Unknown type – ignore for now
                    }
                } else if (elem instanceof String) {
                    // Old saves; you could attempt to parse this if you want
                }
            }
        }

        // Equipment (still placeholder)
        gs.equipment = new ArrayList<>();
        JSONArray eq = obj.optJSONArray("equipment");
        if (eq != null) {
            for (int i = 0; i < eq.length(); i++) {
                // TODO: map string to Equipment subtype
            }
        }
        return gs;
    }

    // --- Player integration helpers ---
    public void capturePlayer(Application.Player p) {
        if (p == null) return;

        setClassType(p.getClassType());
        setHealth(p.getHealth());
        setStamina(p.getStamina());
        setAgility(p.getAgility());
        setDefense(p.getDefense());
        setScore(p.getScore());
        setCoins(p.getNumCoins());
        setIsDead(p.isDead());

        // We want to store BASE attack (no weapon bonus).
        int effectiveAttack = p.getAttack();
        Weapon equipped = p.getEquippedWeapon();
        int base = effectiveAttack;
        if (equipped != null) {
            base = Math.max(0, effectiveAttack - equipped.getDamage());
        }
        setAttack(base);

        // Copy inventory
        setInventory(new ArrayList<>(p.getInventory()));

        // Track which slot is currently equipped
        int idx = -1;
        if (equipped != null) {
            idx = p.getInventory().indexOf(equipped);
        }
        setEquippedWeaponIndex(idx);
    }

    public void applyToPlayer(Application.Player p) {
        if (p == null) return;

        p.setClassType(getClassType());
        p.setHealth(getHealth());
        p.setStamina(getStamina());
        p.setAgility(getAgility());
        // This is base attack; Player will recompute final attack when equipping
        p.setAttack(getAttack());
        p.setDefense(getDefense());
        p.setScore(getScore());
        p.setNumCoins(getCoins());

        // Restore inventory
        p.setInventory(new ArrayList<>(getInventory()));

        // Restore equipped weapon if valid index
        if (equippedWeaponIndex >= 0 && equippedWeaponIndex < p.getInventory().size()) {
            PortableObject po = p.getInventory().get(equippedWeaponIndex);
            if (po instanceof Weapon) {
                p.equipWeapon((Weapon) po);
            }
        }

        if (getIsDead()) {
            p.startDeath();
        } else {
            p.showIdle();
        }

        // If the player has no items after loading state, it's a new game.
        // Give them their starting weapon and ensure it's equipped.
        if (p.getInventory().isEmpty() && p.getEquippedWeapon() == null) {
            p.initializeStartingWeapon();
        }
    }

    public void save(Path path) {
        try {
            Files.writeString(path, toJson().toString(2), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GameState load(Path path) {
        if (!Files.exists(path)) return new GameState();
        try {
            String txt = Files.readString(path, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(txt);
            return fromJson(obj);
        } catch (IOException e) {
            e.printStackTrace();
            return new GameState();
        }
    }

    public static Path defaultPath() {
        // Persist to settings.json at project parent if present, else local
        Path parent = Path.of("..", "settings.json").normalize();
        if (Files.exists(parent)) return parent;
        return Path.of("settings.json");
    }

    // Convenience helpers
    public void save() { save(defaultPath()); }
    public static GameState load() { return load(defaultPath()); }

    // --- Global singleton convenience (optional use) ---
    private static GameState global;
    public static GameState getGlobal() {
        if (global == null) {
            global = load();
        }
        return global;
    }
    public static void setGlobal(GameState gs) { global = gs; }
    public static void saveGlobal() { if (global != null) global.save(); }
}

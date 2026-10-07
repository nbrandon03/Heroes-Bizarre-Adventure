package Application;

// IMPORTS
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Parent;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

/**
 * Mobs class mirrors the Player structure while supporting multiple mob types
 * and their animation states sourced from the Assets/Mobs folder.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Mobs {
	// DEBUG
	private static final boolean DEBUG = false;

	// CORE STATS
	private static final int MAX_HEALTH = 1000;
	private int health;
	private int attack;
	private int defense;
	private boolean isDead;
	private int coin;
	private int score;

	// TYPES & STATES
	/**
	 * Defines the different types of mobs available in the game.
	 */
	public enum MobType {
		DEMON_BOSS("Boss", true),
		CAVE("Cave", false),
		FOREST("Forest", false),
		GRASSLANDS("Grasslands", false),
		MOUNTAIN("Mountain", false),
		VILLAGE("Village", false);

		private final String assetDir;
		private final boolean detailed;
		MobType(String assetDir, boolean detailed) {
			this.assetDir = assetDir;
			this.detailed = detailed;
		}
		public String getAssetDir() { return assetDir; }
		public boolean isDetailed() { return detailed; }
	}

	/**
	 * Defines the possible animation states for a mob.
	 */
	public enum AnimationState {
		IDLE,
		WALK,
		ATTACK,
		HURT,
		DEATH
	}

	private MobType mobType;
	private AnimationState currentState;
	private boolean deathAnimationPlayed;

	// JAVAFX & ANIMATION
	private ImageView imageView;
	private Timeline animation;
	private int currentFrameIndex;
	private TranslateTransition shakeTransition;

	// A cache for loaded animation frame paths to improve performance.
	private final Map<String, String[]> frameCache = new HashMap<>();

	/**
	 * Constructs a new Mob of a specific type.
	 */
	public Mobs(MobType type) {
		this.mobType = type == null ? MobType.DEMON_BOSS : type;
		this.health = getTypeHealth();
		this.attack = computeBaseAttackForType();
		this.defense = computeBaseDefenseForType();
		this.coin = computeCoinAmountForType();
		this.score = computeScoreAmountForType();
		this.isDead = false;
		this.currentState = AnimationState.IDLE;
		this.deathAnimationPlayed = false;

		this.imageView = new ImageView();
		this.imageView.setFitWidth(200);
		this.imageView.setFitHeight(200);
		this.imageView.setPreserveRatio(true);
		this.imageView.setSmooth(true);
		// Do not set placeholder here; wait until we know frames are missing

		this.animation = new Timeline();
		// Show initial idle frame
		showIdle();
	}

	/**
	 * @return The maximum health for the current mob type.
	 */
	private int getTypeHealth() {
		switch (mobType) {
			case CAVE: return 250;
			case FOREST: return 150;
			case GRASSLANDS: return 100;
			case MOUNTAIN: return 350;
			case VILLAGE: return 500;
			case DEMON_BOSS: default: return 600;
		}
	}

	/**
	 * @return The base attack damage for the current mob type.
	 */
	private int computeBaseAttackForType() {
		switch (mobType) {
			case GRASSLANDS: return 8;
			case FOREST: return 10;
			case CAVE: return 12;		
			case MOUNTAIN: return 14;
			case VILLAGE: return 18;
			case DEMON_BOSS: default: return 22;
		}
	}

	/**
	 * @return The base defense value for the current mob type.
	 */
	private int computeBaseDefenseForType() {
		switch (mobType) {
			case GRASSLANDS: return 2;
			case FOREST: return 3;
			case CAVE: return 4;		
			case MOUNTAIN: return 5;
			case VILLAGE: return 6;
			case DEMON_BOSS: default: return 8;
		}
	}

	/**
	 * @return The amount of coins this mob drops upon defeat.
	 */
	private int computeCoinAmountForType() {
		switch (mobType) {
			case GRASSLANDS: return 20;
			case FOREST: return 30;
			case CAVE: return 40;		
			case MOUNTAIN: return 50;
			case VILLAGE: return 60;
			case DEMON_BOSS: default: return 80;
		}
	}

	/**
	 * @return The amount of score this mob drops upon defeat.
	 */
	private int computeScoreAmountForType() {
		switch (mobType) {
			case GRASSLANDS: return 50;
			case FOREST: return 100;
			case CAVE: return 200;		
			case MOUNTAIN: return 400;
			case VILLAGE: return 800;
			case DEMON_BOSS: default: return 1600;
		}
	}
	

	/**
	 * Gets the specific asset subdirectory for a given animation state (for detailed mobs like the boss).
	 */
	private String getDetailedDir(AnimationState state) {
		switch (state) {
			case IDLE: return "01_demon_idle";
			case WALK: return "02_demon_walk";
			case ATTACK: return "03_demon_cleave";
			case HURT: return "04_demon_take_hit";
			case DEATH: return "05_demon_death";
			default: return "01_demon_idle";
		}
	}

	/**
	 * Loads all frame paths from a given directory, caching the result.
	 */
	private String[] loadFrames(String relativeDir) {
		if (frameCache.containsKey(relativeDir)) {
			return frameCache.get(relativeDir);
		}

		List<String> frames = new ArrayList<>();
		// Fallback to file system 'src/' prefix for development
		File dir = new File("src/" + relativeDir);
		if (DEBUG) System.out.println("[Mobs] Scanning filesystem dir: " + dir.getAbsolutePath());
		if (dir.exists() && dir.isDirectory()) {
			File[] pngs = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
			if (pngs != null) {
				Arrays.sort(pngs, Comparator.comparingInt(f -> extractNumericPrefix(f.getName())));
				for (File f : pngs) {
					frames.add(relativeDir + "/" + f.getName());
				}
			}
		}

		// If file system scan found nothing, attempt classpath numeric scan (0..199)
		if (frames.isEmpty()) {
			for (int i = 0; i < 200; i++) {
				String rel = relativeDir + "/" + i + ".png";
				try {
					java.io.InputStream is = getClass().getResourceAsStream("/" + rel);
					if (is != null) {
						frames.add(rel);
					}
				} catch (Exception ignored) {}
			}
			if (DEBUG) System.out.println("[Mobs] Classpath probe found " + frames.size() + " frames for dir: " + relativeDir);
		}

		// If no frames found, attempt sequential fallback (0.png..9.png)
		if (frames.isEmpty()) {
			for (int i = 0; i < 10; i++) {
				File f = new File("src/" + relativeDir + "/" + i + ".png");
				if (f.exists()) {
					frames.add(relativeDir + "/" + i + ".png");
				}
			}
		}

		if (DEBUG) System.out.println("[Mobs] Loaded " + frames.size() + " frames from " + relativeDir);
		String[] arr = frames.toArray(new String[0]);
		frameCache.put(relativeDir, arr);
		return arr;
	}

	/**
	 * Helper to extract a numeric prefix from a filename for sorting (e.g., "0.png", "demon_01.png").
	 */
	private int extractNumericPrefix(String name) {
		try {
			StringBuilder digits = new StringBuilder();
			for (char c : name.toCharArray()) {
				if (Character.isDigit(c)) digits.append(c); else if (digits.length() > 0) break;
			}
			return digits.length() == 0 ? Integer.MAX_VALUE : Integer.parseInt(digits.toString());
		} catch (Exception e) {
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Loads an image from a relative asset path, with fallbacks.
	 */
	private Image loadImage(String relativePath) {
		String cp = "/" + relativePath;
		try {
			java.io.InputStream is = getClass().getResourceAsStream(cp);
			if (is != null) {
				Image img = new Image(is, 200, 200, true, true);
				if (!img.isError()) return img;
			}
		} catch (Exception ex) {
			// fall through to file path
		}
		// Try filesystem path (src-based)
		Image fileImg = new Image("file:" + "src/" + relativePath, 200, 200, true, true);
		if (!fileImg.isError()) return fileImg;

		// Case-insensitive fallback: try .PNG when .png fails
		if (relativePath.toLowerCase().endsWith(".png")) {
			String alt = relativePath.substring(0, relativePath.length() - 4) + ".PNG";
			try {
				// classpath alt
				java.io.InputStream is2 = getClass().getResourceAsStream("/" + alt);
				if (is2 != null) {
					Image img2 = new Image(is2, 200, 200, true, true);
					if (!img2.isError()) return img2;
				}
			} catch (Exception ignored) {}
			// filesystem alt
			Image fileImg2 = new Image("file:" + "src/" + alt, 200, 200, true, true);
			if (!fileImg2.isError()) return fileImg2;
		}

		if (DEBUG) System.out.println("[Mobs] Failed to load image: " + relativePath);
		return null;
	}

	/**
	 * Creates a simple red box as a placeholder image when assets are missing.
	 */
	private Image createPlaceholderImage() {
		int w = 200, h = 200;
		WritableImage wi = new WritableImage(w, h);
		Canvas canvas = new Canvas(w, h);
		GraphicsContext g = canvas.getGraphicsContext2D();
		g.setFill(Color.DARKRED);
		g.fillRect(0, 0, w, h);
		g.setFill(Color.WHITE);
		g.fillText("Mob", 80, 100);
		canvas.snapshot(null, wi);
		return wi;
	}

	/**
	 * The core animation method that plays a sequence of frames.
	 */
	private void startAnimation(String[] frames, int millisPerFrame, boolean loop, AnimationState state) {
		if (frames == null || frames.length == 0) {
			// Nothing to animate
			return;
		}
		if (animation == null) {
			animation = new Timeline();
		}
		if (currentState == state && loop) {
			return; // Already looping this state
		}
		animation.stop();
		animation.getKeyFrames().clear();
		currentState = state;
		currentFrameIndex = 0;

		// Set the first frame immediately so the mob is visible before the first tick
		Image first = loadImage(frames[0]);
		if (first != null) {
			imageView.setImage(first);
		} else {
			if (DEBUG) System.out.println("[Mobs] First frame failed to load: " + frames[0]);
			imageView.setImage(createPlaceholderImage());
		}

		KeyFrame kf = new KeyFrame(Duration.millis(millisPerFrame), e -> {
			if (currentFrameIndex < frames.length) {
				Image img = loadImage(frames[currentFrameIndex]);
				imageView.setImage(img != null ? img : createPlaceholderImage());
				currentFrameIndex++;
			} else if (loop) {
				currentFrameIndex = 0;
				Image img = loadImage(frames[currentFrameIndex]);
				imageView.setImage(img != null ? img : createPlaceholderImage());
				currentFrameIndex++;
			}
		});

		animation.getKeyFrames().add(kf);
		animation.setCycleCount(loop ? Timeline.INDEFINITE : frames.length + 1);
		animation.setOnFinished(ev -> {
			if (!loop && frames.length > 0) {
				Image img = loadImage(frames[frames.length - 1]);
				imageView.setImage(img != null ? img : createPlaceholderImage());
			}
		});
		animation.play();
	}

	/**
	 * Starts the mob's walking animation (if available).
	 */
	public void startWalk() {
		// Regular mobs (non-detailed) should not animate walk; keep idle frame
		if (!mobType.isDetailed()) {
			showIdle();
			return;
		}
		startAnimation(resolveFrames(AnimationState.WALK), 120, true, AnimationState.WALK);
	}

	/**
	 * Stops any current animation and shows the mob's idle frame.
	 */
	public void showIdle() {
		if (animation != null) animation.stop();
		String[] idle = resolveFrames(AnimationState.IDLE);
		// For Grasslands, freeze on explicit frame 0.png to guarantee visibility
		if (!mobType.isDetailed() && mobType == MobType.GRASSLANDS) {
			String frame0 = "Assets/Mobs/" + mobType.getAssetDir() + "/0.png";
			Image img0 = loadImage(frame0);
			if (img0 != null) {
				if (DEBUG) System.out.println("[Mobs] Grasslands idle pinned to 0.png");
				imageView.setImage(img0);
			} else {
				if (DEBUG) System.out.println("[Mobs] Failed to load Grasslands 0.png, using placeholder");
				imageView.setImage(createPlaceholderImage());
			}
		} else if (!mobType.isDetailed()) {
			// Other simple mobs: show first available frame statically
			if (idle.length > 0) {
				Image img = loadImage(idle[0]);
				imageView.setImage(img != null ? img : createPlaceholderImage());
			} else {
				imageView.setImage(createPlaceholderImage());
			}
		} else {
			// Detailed mobs (e.g., boss) animate using their specific directories
			if (idle.length > 0) {
				startAnimation(idle, 200, true, AnimationState.IDLE);
			}
		}
		currentState = AnimationState.IDLE;
	}

	/**
	 * Starts the mob's attack animation.
	 */
	public void startAttack() {
		// For Grasslands simple mob, attack frames are 9-13.png
		if (!mobType.isDetailed() && mobType == MobType.GRASSLANDS) {
			String[] atk = resolveFramesRange("Assets/Mobs/" + mobType.getAssetDir(), 9, 13);
			startAnimation(atk, 140, false, AnimationState.ATTACK);
			return;
		}
		// For other mobs, use generic resolution
		startAnimation(resolveFrames(AnimationState.ATTACK), 140, false, AnimationState.ATTACK);
	}

	/**
	 * Plays the hurt animation or a shake effect.
	 */
	public void startHurt() {
		// Regular mobs shake only; boss animates hurt without shake.
		if (mobType.isDetailed()) {
			startAnimation(resolveFrames(AnimationState.HURT), 160, false, AnimationState.HURT);
			return;
		}
		shake(240, 8);
	}

	/**
	 * Plays the death animation and despawns the mob.
	 */
	public void startDeath() {
		if (deathAnimationPlayed) return;
		deathAnimationPlayed = true;
		// Play mob death SFX once when death starts
		try {
			Application.AudioManager.getInstance().playSfx("MOB_DEATH");
		} catch (Exception ignored) {}
		// Only play death animation if this mob type has detailed frames (boss)
		if (!mobType.isDetailed()) {
			despawn();
			return;
		}
		String[] frames = resolveFrames(AnimationState.DEATH);
		if (frames.length == 0) { // no frames found, remove immediately
			despawn();
			return;
		}
		startAnimation(frames, 200, false, AnimationState.DEATH);
		if (animation != null) {
			animation.setOnFinished(ev -> {
				// Ensure final frame persists briefly, then remove
				imageView.setImage(loadImage(frames[frames.length - 1]));
				despawn();
			});
		}
	}

	/**
	 * Resolves the correct frame paths for a given animation state.
	 */
	private String[] resolveFrames(AnimationState state) {
		String base = "Assets/Mobs/" + mobType.getAssetDir();
		if (mobType.isDetailed()) {
			String dir = base + "/" + getDetailedDir(state);
			String[] frames = loadFrames(dir);
			if (frames.length > 0) return frames;
		}
		// Fallback: use root directory images (static or idle substitute)
		String[] rootFrames = loadFrames(base);
		return rootFrames.length > 0 ? rootFrames : new String[0];
	}

	/**
	 * Helper to build a list of frame paths for a simple numeric sequence (e.g., 9.png, 10.png, ...).
	 */
	private String[] resolveFramesRange(String baseDir, int startIdx, int endIdx) {
		List<String> frames = new ArrayList<>();
		for (int i = startIdx; i <= endIdx; i++) {
			String rel = baseDir + "/" + i + ".png";
			// Trust naming; loadImage handles classpath or src fallback
			frames.add(rel);
		}
		if (DEBUG) System.out.println("[Mobs] Range (no-check) built: " + frames.size() + " frames in " + baseDir + " from " + startIdx + " to " + endIdx);
		return frames.toArray(new String[0]);
	}

	/**
	 * Reduces the mob's health after calculating damage mitigation from defense.
	 */
	public void takeDamage(int raw) {
		if (isDead) return;
		int mitigated = Math.max(0, raw - defense);
		// Ensure a minimum of 1 damage when a positive hit lands but defense fully mitigates
		if (raw > 0 && mitigated == 0) {
			mitigated = 1;
		}
		setHealth(health - mitigated);
		if (!isDead) startHurt();
	}

	/**
	 * Increases the mob's health.
	 */
	public void heal(int amount) {
		if (amount > 0 && !isDead) setHealth(health + amount);
	}

	/**
	 * Initiates an attack against the player.
	 */
	public void attackPlayer(Player player) {
		if (player == null || isDead) return;
		startAttack();
		player.takeDamage(attack);
	}

	/**
	 * Removes the mob's ImageView from its parent container, effectively
	 * despawning it from the scene.
	 */
	private void despawn() {
		Parent parent = imageView.getParent();
		if (parent instanceof Pane) {
			((Pane) parent).getChildren().remove(imageView);
		} else if (parent instanceof Group) {
			((Group) parent).getChildren().remove(imageView);
		}
	}

	/**
	 * A simple visual effect to shake the mob's node, used for hurt feedback.
	 */
	private void shake(int totalDurationMs, double amplitude) {
		if (imageView == null) return;
		if (shakeTransition != null) {
			shakeTransition.stop();
		}
		double baseX = imageView.getTranslateX();
		int cycles = Math.max(2, totalDurationMs / 80); // ~40ms forward + 40ms reverse per cycle
		shakeTransition = new TranslateTransition(Duration.millis(40), imageView);
		shakeTransition.setFromX(baseX - amplitude);
		shakeTransition.setToX(baseX + amplitude);
		shakeTransition.setAutoReverse(true);
		shakeTransition.setCycleCount(cycles);
		shakeTransition.setOnFinished(e -> imageView.setTranslateX(baseX));
		shakeTransition.play();
	}

	// GETTERS
	public int getHealth() { return health; }
	public int getAttack() { return attack; }
	public int getDefense() { return defense; }
	public boolean isDead() { return isDead; }
	public MobType getMobType() { return mobType; }
	public AnimationState getCurrentState() { return currentState; }
	public int getMaxHealth() { return MAX_HEALTH; }
	public ImageView getNode() { return imageView; }
	public int getCoinAmount() {return coin; }
	public int getScoreAmount() {return score; }

	// SETTERS
	public void setHealth(int h) {
		health = Math.max(0, Math.min(h, MAX_HEALTH));
		if (health == 0) {
			isDead = true;
			startDeath();
		}
	}
	public void setAttack(int a) { attack = Math.max(0, a); }
	public void setDefense(int d) { defense = Math.max(0, d); }
	public void setMobType(MobType type) {
		if (type != null && type != mobType) {
			mobType = type;
			deathAnimationPlayed = false;
			attack = computeBaseAttackForType();
			defense = computeBaseDefenseForType();
			setHealth(getTypeHealth());
			showIdle();
		}
	}
	public void resetDeathAnimationFlag() { deathAnimationPlayed = false; }

	/**
	 * Converts the mob's state to a JSON object for saving.
	 */
	public JSONObject toJson() {
		JSONObject o = new JSONObject();
		o.put("mobType", mobType.name());
		o.put("health", health);
		o.put("attack", attack);
		o.put("defense", defense);
		o.put("isDead", isDead);
		o.put("state", currentState.name());
		return o;
	}

	public void fromJson(JSONObject o) {
		if (o == null) return;
		try {
			MobType mt = MobType.valueOf(o.optString("mobType", mobType.name()));
			setMobType(mt);
		} catch (Exception ignored) {}
		setHealth(o.optInt("health", health));
		setAttack(o.optInt("attack", attack));
		setDefense(o.optInt("defense", defense));
		boolean dead = o.optBoolean("isDead", isDead);
		if (dead && !isDead) {
			isDead = true;
			startDeath();
		}
		try {
			AnimationState st = AnimationState.valueOf(o.optString("state", currentState.name()));
			// Attempt to reflect saved state (only if not dead)
			if (!isDead) {
				switch (st) {
					case WALK: startWalk(); break;
					case ATTACK: startAttack(); break;
					case HURT: startHurt(); break;
					case DEATH: startDeath(); break;
					default: showIdle(); break;
				}
			}
		} catch (Exception ignored) {}
	}
}

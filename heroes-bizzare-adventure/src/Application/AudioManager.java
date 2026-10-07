package Application;

// IMPORTS
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.HashMap;

/**
 * AudioManager centralizes background music and sound effects playback
 * for the game. It loads WAV assets from the `src/Assets/Sounds` folder,
 * manages a single looping music track with optional cross-fade, and
 * preloads short SFX clips for low-latency playback.
 * 
 * Supported audio format: WAV files only.
 * 
 * @author Brandon Nguyen
 * 
 */
public class AudioManager {

	/**
	 * Available background music tracks for major game contexts.
	 */
	public enum MusicTrack {
		MAIN_MENU,
		GAMEPLAY,
		BOSS,
		GAMEOVER
	}

	private static AudioManager instance;

	// AUDIO FILES
	private final Map<MusicTrack, File> musicFiles = new EnumMap<>(MusicTrack.class);
	/** SFX file mapping (WAV only). */
	private final Map<String, File> sfxFiles = new HashMap<>();
	/** Preloaded SFX clips for low-latency playback. */
	private final Map<String, Clip> sfxClips = new HashMap<>();

	// STATE
	private Clip currentMusic;
	private MusicTrack activeMusicTrack;

	/** Global volume multiplier in range [0..1]. */
	private float masterVolume = 1.0f;
	/** Music volume base in range [0..1]. */
	private float musicVolume = 0.8f;
	/** SFX volume base in range [0..1]. */
	private float sfxVolume = 0.9f;

	/**
	 * Initializes mappings and preloads SFX clips.
	 */
	private AudioManager() {
		// Music files
		musicFiles.put(MusicTrack.MAIN_MENU, new File("src/Assets/Sounds/MainMenu.wav"));
		musicFiles.put(MusicTrack.GAMEPLAY, new File("src/Assets/Sounds/BackgroundMusicMain.wav"));
		musicFiles.put(MusicTrack.BOSS, new File("src/Assets/Sounds/BackgroundMusicBoss.wav"));

		// Sound effects mapping (WAV only)
		sfxFiles.put("CLICK", new File("src/Assets/Sounds/Click.wav"));
		sfxFiles.put("COIN_BUY", new File("src/Assets/Sounds/CoinBuy.wav"));
		sfxFiles.put("MOB_DEATH", new File("src/Assets/Sounds/MobDeath.wav"));
		sfxFiles.put("GameOver", new File("src/Assets/Sounds/GameOver.wav"));
		sfxFiles.put("HIT", new File("src/Assets/Sounds/Hit.wav"));
		sfxFiles.put("GETTING_HURT", new File("src/Assets/Sounds/GettingHurt.wav"));
		

		preloadSfx();
	}

	/**
	 * Returns the singleton instance.
	 *
	 * @return the {@code AudioManager} instance
	 */
	public static AudioManager getInstance() {
		if (instance == null) instance = new AudioManager();
		return instance;
	}

	/**
	 * Switches background music to a new track if it differs from the current
	 * active track. Uses a cross-fade for a smooth transition.
	 *
	 * @param music the target {@link MusicTrack}; ignored if {@code null} or same as active
	 */
	public void switchMusic(MusicTrack music) {
		if (music != null && music != activeMusicTrack) {
			playMusic(music, true);
		}
	}

	/**
	 * Plays the given music track, starting it in a continuous loop.
	 *
	 * @param track the {@link MusicTrack} to play
	 * @param crossFade whether to cross-fade from the current track (if any)
	 */
	public void playMusic(MusicTrack track, boolean crossFade) {
		activeMusicTrack = track;
		replaceLoopingClip(track, musicFiles, crossFade);
	}

	/**
	 * Plays a preloaded sound effect by key. If the key is not loaded or
	 * unavailable, logs an error and returns.
	 *
	 * @param key SFX identifier (e.g., "CLICK", "HIT")
	 */
	public void playSfx(String key) {
		Clip clip = sfxClips.get(key);
		if (clip == null) {
			System.err.println("SFX clip not loaded: " + key);
			return;
		}
		try {
			// Reset to start
			clip.stop();
			clip.setFramePosition(0);
			setClipVolume(clip, masterVolume * sfxVolume);
			clip.start();
		} catch (Exception e) {
			System.err.println("Failed to play preloaded SFX " + key + ": " + e.getMessage());
		}
	}

	/**
	 * Stops and releases the current background music clip, clearing the active
	 * track.
	 */
	public void stopMusic() {
		stopClip(currentMusic);
		currentMusic = null;
		activeMusicTrack = null;
	}

	/**
	 * Sets the music volume in range [0..1] and applies it to the current
	 * music clip.
	 *
	 * @param volume linear volume in [0..1]
	 */
	public void setMusicVolume(float volume) {
		musicVolume = clamp01(volume);
		applyMusicVolume();
	}

	/**
	 * Sets the SFX volume in range [0..1].
	 *
	 * @param volume linear volume in [0..1]
	 */
	public void setSfxVolume(float volume) {
		sfxVolume = clamp01(volume);
	}

	/**
	 * Sets the master volume in range [0..1] and applies it to the current
	 * music clip.
	 *
	 * @param volume linear volume in [0..1]
	 */
	public void setMasterVolume(float volume) {
		masterVolume = clamp01(volume);
		applyMusicVolume();
	}

	/**
	 * Applies the current master and music volume levels to the active music clip.
	 */
	private void applyMusicVolume() {
		if (currentMusic != null) setClipVolume(currentMusic, masterVolume * musicVolume);
	}

	// --- Internal helpers ---
	private void replaceLoopingClip(MusicTrack key, Map<MusicTrack, File> fileMap, boolean crossFade) {
		File file = fileMap.get(key);
		if (file == null) return;
		try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
			Clip newClip = AudioSystem.getClip();
			newClip.open(ais);
			setClipVolume(newClip, masterVolume * musicVolume);
			newClip.loop(Clip.LOOP_CONTINUOUSLY);
			newClip.start();
			if (crossFade && currentMusic != null) {
				crossFade(currentMusic, newClip, 800);
			} else {
				stopClip(currentMusic);
			}
			currentMusic = newClip;
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	private void crossFade(Clip oldClip, Clip newClip, int durationMs) {
		new Thread(() -> {
			try {
				FloatControl oldGain = (FloatControl) oldClip.getControl(FloatControl.Type.MASTER_GAIN);
				FloatControl newGain = (FloatControl) newClip.getControl(FloatControl.Type.MASTER_GAIN);
				float oldStart = oldGain.getValue();
				float newTarget = newGain.getValue();
				newGain.setValue(Math.max(newGain.getMinimum(), -50f));
				int steps = 30;
				for (int i=1; i<=steps; i++) {
					float ratio = i/(float)steps;
					oldGain.setValue(oldStart + (-50f - oldStart)*ratio);
					newGain.setValue(-50f + (newTarget + 50f)*ratio);
					Thread.sleep(durationMs/steps);
				}
				stopClip(oldClip);
				newGain.setValue(newTarget);
			} catch (Exception ignored) {}
		}).start();
	}

	/**
	 * Safely stops and closes a Clip.
	 */
	private void stopClip(Clip clip) {
		if (clip != null) {
			try { clip.stop(); clip.close(); } catch (Exception ignored) {}
		}
	}

	/**
	 * Sets the volume of a Clip using a linear scale [0.0, 1.0].
	 */
	private void setClipVolume(Clip clip, float linear) {
		try {
			FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			float dB = (float)(Math.log10(Math.max(linear, 0.0001))*20.0);
			gain.setValue(Math.max(gain.getMinimum(), Math.min(dB, gain.getMaximum())));
		} catch (IllegalArgumentException ignored) {}
	}

	/**
	 * Preloads all sound effects into memory for low-latency playback.
	 */
	private void preloadSfx() {
		for (Map.Entry<String, File> e : sfxFiles.entrySet()) {
			File f = e.getValue();
			if (!f.exists()) {
				System.err.println("SFX file missing: " + f.getPath());
				continue;
			}
			try (AudioInputStream aisOriginal = AudioSystem.getAudioInputStream(f)) {
				AudioFormat baseFormat = aisOriginal.getFormat();
				AudioFormat decoded = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					baseFormat.getSampleRate(),
					16,
					baseFormat.getChannels(),
					baseFormat.getChannels() * 2,
					baseFormat.getSampleRate(),
					false
				);
				AudioInputStream ais = AudioSystem.getAudioInputStream(decoded, aisOriginal);
				Clip clip = AudioSystem.getClip();
				clip.open(ais);
				sfxClips.put(e.getKey(), clip);
				System.out.println("Loaded SFX " + e.getKey() + " format=" + decoded.getSampleRate() + "Hz channels=" + decoded.getChannels());
			} catch (Exception ex) {
				System.err.println("Failed to load SFX " + e.getKey() + ": " + ex.getMessage());
			}
		}
	}

	/**
	 * Clamps a float value to the range [0.0, 1.0].
	 */
	private float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

	/**
	 * Returns the currently active music track, or {@code null} if none.
	 */
	public MusicTrack getActiveMusicTrack() { return activeMusicTrack; }
	/** Returns the music volume in [0..1]. */
	public float getMusicVolume() { return musicVolume; }
	/** Returns the SFX volume in [0..1]. */
	public float getSfxVolume() { return sfxVolume; }
	/** Returns the master volume in [0..1]. */
	public float getMasterVolume() { return masterVolume; }
}

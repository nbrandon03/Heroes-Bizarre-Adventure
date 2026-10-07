
   package Application.GameScreens;

// IMPORTS
import Application.AudioManager;
import Application.AudioManager.MusicTrack;
import Application.Engine.GameState;
import Application.Player;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The in-game pause menu, accessible by pressing ESC during gameplay.
 * It provides options to save, adjust settings, or return to the main menu.
 *
 * @author Brandon Nguyen
 */
public class PauseMenu extends BorderPane {
	private final Runnable onBack;
	private final Runnable openAudio;
	private final Runnable openModes;
	private final Player player; // Player object to capture stats when saving.
	private final List<MenuRow> rows = new ArrayList<>();
	private int selectedIndex = 0;

	public PauseMenu(Runnable onBack) {
		this(onBack, null, null, null);
	}

	public PauseMenu(Runnable onBack, Runnable openAudio) {
		this(onBack, openAudio, null, null);
	}

	public PauseMenu(Runnable onBack, Runnable openAudio, Runnable openModes) {
		this(onBack, openAudio, openModes, null);
	}

	public PauseMenu(Runnable onBack, Runnable openAudio, Runnable openModes, Player player) {
		this.onBack = onBack;
		this.openAudio = openAudio;
		this.openModes = openModes;
		this.player = player;
		build();
	}

	/**
	 * Builds the UI components for the pause menu.
	 */
	private void build() {
		setPadding(new Insets(10));
		setBackground(buildBackground());

		// AUDIO
		AudioManager.getInstance().switchMusic(MusicTrack.GAMEPLAY);

		// TITLE
		Label title = new Label("OPTIONS");
		title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 42px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 12, 0.4, 0, 3);");
		VBox titleBox = new VBox(title);
		titleBox.setAlignment(Pos.CENTER);
		titleBox.setPadding(new Insets(40, 0, 25, 0)); // more breathing room below title

		// MENU ROWS
		VBox listBox = new VBox();
		listBox.setAlignment(Pos.CENTER);
		listBox.setSpacing(24); // larger gap between rows
		listBox.setPadding(new Insets(10, 0, 50, 0)); // bottom space so Back isn't tight

		rows.add(createRow("Audio", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			// Navigate to the Audio settings screen.
			javafx.scene.Scene sc = getScene();
			PauseMenu pm = this; // capture current pause menu instance for back navigation
			if (sc != null) {
				sc.setRoot(new Audio(() -> sc.setRoot(pm)));
			} else if (openAudio != null) {
				openAudio.run();
			} else {
				System.out.println("Audio settings");
			}
		}));

		// Save the current game state.
		rows.add(createRow("Save", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			GameState gs = GameState.load();
			gs.captureAudioFrom(AudioManager.getInstance());
			if (player != null) {
				gs.capturePlayer(player);
			}
			gs.save();
			System.out.println("Game saved to settings.json");
		}));

		rows.add(createRow("Modes", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			// If scene is available, open ModesMenu and ensure back returns here
			javafx.scene.Scene sc = getScene();
			PauseMenu pm = this;
			if (sc != null) {
				sc.setRoot(new ModesMenu(() -> sc.setRoot(pm)));
			} else if (openModes != null) {
				openModes.run();
			} else {
				System.out.println("Modes settings");
			}
		}));

		// Return to the main menu and auto-save the game.
		rows.add(createRow("Main Menu", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			GameState gs = GameState.load();
			gs.captureAudioFrom(AudioManager.getInstance());
			if (player != null) {
				gs.capturePlayer(player);
			}
			gs.save();
			javafx.scene.Scene sc = getScene();
			if (sc != null) {
				// Rebuild MainMenu with correct navigation callbacks.
				final Runnable startCb = () -> { if (onBack != null) onBack.run(); };
				final Runnable[] openSettingsHolder = new Runnable[1];
				final Runnable[] openNewHolder = new Runnable[1];
				openSettingsHolder[0] = () -> sc.setRoot(new Settings(() -> sc.setRoot(new MainMenu(openSettingsHolder[0], startCb, openNewHolder[0]))));
				openNewHolder[0] = () -> sc.setRoot(new NewGame(
						// onBack from NewGame -> MainMenu
						() -> sc.setRoot(new MainMenu(openSettingsHolder[0], startCb, openNewHolder[0])),
						// onOpenMap -> WorldMap
						() -> {
							    java.util.Map<String, Runnable> nav = new java.util.HashMap<>();

							    java.util.function.BiFunction<javafx.scene.Scene, Application.Player, Runnable> backToMapFactory = (sceneRef, playerRef) -> () -> sceneRef.setRoot(new WorldMap(
								    () -> sceneRef.setRoot(new Start(() -> sceneRef.setRoot(new MainMenu(openSettingsHolder[0], startCb, openNewHolder[0])), () -> {})),
								    nav
							    ));

							    java.util.function.Function<javafx.scene.Scene, Application.Player> loadPlayer = (sceneRef) -> {
								Application.Player p = new Application.Player();
								Application.Engine.GameState.load().applyToPlayer(p);
								return p;
							    };

							    nav.put("Grasslands", () -> {
								Application.Player p = loadPlayer.apply(sc);
								sc.setRoot(new Grasslands(
									backToMapFactory.apply(sc, p),
									() -> {},
									p
								));
							    });

							    nav.put("Forest", () -> {
								Application.Player p = loadPlayer.apply(sc);
								sc.setRoot(new Forest(
									backToMapFactory.apply(sc, p),
									() -> {},
									p
								));
							    });

							    nav.put("Cave", () -> {
								Application.Player p = loadPlayer.apply(sc);
								sc.setRoot(new Cave(
									backToMapFactory.apply(sc, p),
									() -> {},
									p
								));
							    });

							    nav.put("Mountain", () -> {
								Application.Player p = loadPlayer.apply(sc);
								sc.setRoot(new Mountain(
									backToMapFactory.apply(sc, p),
									() -> {},
									p
								));
							    });

							    nav.put("Village", () -> {
								Application.Player p = loadPlayer.apply(sc);
								sc.setRoot(new Village(
									backToMapFactory.apply(sc, p),
									() -> {},
									p
								));
							    });

							    nav.put("Dungeon", () -> {
								Application.Player p = loadPlayer.apply(sc);
								sc.setRoot(new Dungeon(
									backToMapFactory.apply(sc, p),
									() -> {},
									p
								));
							    });

							    nav.put("Boss", () -> {
								Application.Player p = loadPlayer.apply(sc);
								sc.setRoot(new Boss(
									backToMapFactory.apply(sc, p),
									() -> {},
									p
								));
							    });

							    nav.put("Shop", () -> {
								Application.Player p = loadPlayer.apply(sc);
								sc.setRoot(new Shop(
									backToMapFactory.apply(sc, p),
									p
								));
							    });
							sc.setRoot(new WorldMap(
									// Back from map -> Hall of Heroes (Start)
									() -> sc.setRoot(new Start(() -> sc.setRoot(new MainMenu(openSettingsHolder[0], startCb, openNewHolder[0])), () -> {})),
									nav
							));
						}
				));
				sc.setRoot(new MainMenu(openSettingsHolder[0], startCb, openNewHolder[0]));
			} else if (onBack != null) {
				onBack.run();
			}
		}));
		rows.add(createRow("Back", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			if (onBack != null) onBack.run();
		}));

		for (MenuRow r : rows) listBox.getChildren().add(r.container);

		VBox center = new VBox(10, titleBox, listBox);
		center.setAlignment(Pos.TOP_CENTER);
		center.setPadding(new Insets(0, 0, 0, 0)); // titleBox already provides top padding
		center.setFillWidth(true);
		setCenter(center);


		// KEYBOARD NAVIGATION
		updateSelection(0);
		setFocusTraversable(true);
		Platform.runLater(this::requestFocus);
		addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.UP) {
				moveSelection(-1);
				AudioManager.getInstance().playSfx("CLICK");
				e.consume();
			} else if (e.getCode() == KeyCode.DOWN) {
				moveSelection(1);
				AudioManager.getInstance().playSfx("CLICK");
				e.consume();
			} else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
				rows.get(selectedIndex).button.fire();
				e.consume();
			}
		});
	}

	/**
	 * Creates a standard menu row with a single button.
	 */
	private MenuRow createRow(String text, Runnable action) {
		Label left = new Label("❮");
		Label right = new Label("❯");
		styleArrow(left); styleArrow(right);
		left.setVisible(false); right.setVisible(false);

		Button btn = new Button(text);
		btn.setCursor(Cursor.HAND);
		btn.setOnAction(e -> {
			AudioManager.getInstance().playSfx("CLICK");
			action.run();
		});
		btn.setFocusTraversable(false);
		btn.setStyle(minimalButtonStyle(false));

		DropShadow glow = new DropShadow(18, Color.web("#ffffff80"));
		btn.setOnMouseEntered(e -> { selectRow(indexOf(btn)); btn.setEffect(glow); });
		btn.setOnMouseExited(e -> btn.setEffect(null));

		HBox container = new HBox(20, left, btn, right);
		container.setAlignment(Pos.CENTER);
		MenuRow row = new MenuRow(container, left, btn, right);
		container.setOnMouseEntered(e -> selectRow(rows.indexOf(row)));
		return row;
	}

	/**
	 * Styles the selection indicator arrows.
	 */
	private void styleArrow(Label arrow) {
		arrow.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
		arrow.setOpacity(0.9);
	}

	/**
	 * Defines the CSS style for a menu button.
	 */
	private String minimalButtonStyle(boolean selected) {
		String color = selected ? "#ffffff" : "#e8e8e8";
		return String.join("",
				"-fx-background-color: transparent;",
				"-fx-text-fill: ", color, ";",
				"-fx-font-size: 20px;",
				"-fx-font-weight: bold;",
				"-fx-padding: 4 12 4 12;",
				"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 8, 0.2, 0, 2);"
		);
	}

	/**
	 * Moves the selection up or down.
	 */
	private void moveSelection(int delta) {
		int next = (selectedIndex + delta + rows.size()) % rows.size();
		updateSelection(next);
	}

	/**
	 * Sets the currently selected row by its index.
	 */
	private void selectRow(int index) { if (index >= 0 && index < rows.size()) updateSelection(index); }

	private void updateSelection(int index) {
		selectedIndex = index;
		for (int i = 0; i < rows.size(); i++) {
			MenuRow r = rows.get(i);
			boolean sel = i == selectedIndex;
			r.left.setVisible(sel); r.right.setVisible(sel);
			r.button.setStyle(minimalButtonStyle(sel));
		}
	}

	/**
	 * Finds the index of the row containing the given button.
	 */
	private int indexOf(Button btn) { for (int i=0;i<rows.size();i++) if (rows.get(i).button==btn) return i; return -1; }

	/**
	 * Builds the background for the menu.
	 */
	private Background buildBackground() {
		Image img = loadMenuImage();
		BackgroundSize size = new BackgroundSize(100,100,true,true,false,true);
		return new Background(new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, size));
	}

	/**
	 * Loads the background image for the menu.
	 */
	private Image loadMenuImage() {
		URL url = getClass().getResource("/Assets/menu.png");
		if (url != null) return new Image(url.toExternalForm());
		return new Image(new File("src/Assets/menu.png").toURI().toString());
	}

	/**
	 * A helper class to hold the components of a single menu row.
	 */
	private static class MenuRow {
		final HBox container; final Label left; final Button button; final Label right;
		MenuRow(HBox c, Label l, Button b, Label r){ container=c; left=l; button=b; right=r; }
	}
}

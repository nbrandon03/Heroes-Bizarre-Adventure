package Application.GameScreens;

// IMPORTS
import Application.AudioManager;
import Application.Engine.GameState;
import Application.Actor.ClassType;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * New Game scene allowing the player to pick a class (Soldier/Archer).
 * Uses project Assets for simple frame preview and minimal UI consistent
 * with existing screens.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class NewGame extends BorderPane {
	// CLASS SELECTION
	public enum PlayerClass { SOLDIER, ARCHER }

	// NAVIGATION
	private final Runnable onBack;

	// MENU STATE
	private final List<MenuRow> rows = new ArrayList<>();
	private int selectedIndex = 0;

	// UI & ANIMATION
	private final ImageView preview = new ImageView();
	private PlayerClass current = PlayerClass.SOLDIER;
	private Label classNameLabel = new Label();
	private Label classDescLabel = new Label();
	private Timeline attackAnimation;

	public NewGame(Runnable onBack) {
		this(onBack, onBack); // fallback: map navigates using the same callback path
	}

	public NewGame(Runnable onBack, Runnable onOpenMap) {
		this.onBack = onBack;
		setPadding(new Insets(10));
		setBackground(buildBackground());

		// TITLE
		Label title = new Label("NEW GAME");
		title.setStyle("-fx-font-size: 42px; -fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 12, 0.4, 0, 3);");
		VBox titleBox = new VBox(title);
		titleBox.setAlignment(Pos.CENTER);
		titleBox.setPadding(new Insets(30, 0, 10, 0));

		// CLASS PREVIEW
		preview.setPreserveRatio(true);
		// Disable smoothing to avoid blurriness on pixel art
		preview.setSmooth(false);
		// Slightly smaller size to keep sprites crisp
		preview.setFitWidth(160);
		preview.setFitHeight(160);
		StackPane previewPane = new StackPane(preview);
		previewPane.setPadding(new Insets(10));
		previewPane.setMinSize(190, 190);
		previewPane.setPrefSize(190, 190);
		previewPane.setMaxSize(190, 190);

		// CLASS INFO
		classNameLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");
		classDescLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: white;");
		classDescLabel.setWrapText(true);
		classDescLabel.setPrefWidth(340);
		classDescLabel.setMaxWidth(340);
		VBox infoPanel = new VBox(8, classNameLabel, classDescLabel);
		infoPanel.setAlignment(Pos.CENTER_LEFT);
		infoPanel.setStyle("-fx-padding: 10; -fx-background-color: rgba(0,0,0,0.25); -fx-background-radius: 8;");
		// Fix info panel size so background box is consistent across classes
		infoPanel.setMinSize(360, 140);
		infoPanel.setPrefSize(360, 140);
		infoPanel.setMaxSize(360, 140);

		// NAVIGATION ARROWS
		Button prevArrow = new Button("❮");
		Button nextArrow = new Button("❯");
		prevArrow.setCursor(Cursor.HAND);
		nextArrow.setCursor(Cursor.HAND);
		String navStyle = "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-size: 18px; -fx-background-radius: 20; -fx-min-width: 40px; -fx-min-height: 36px;";
		prevArrow.setStyle(navStyle);
		nextArrow.setStyle(navStyle);
		prevArrow.setOnAction(e -> { cycleClass(-1); AudioManager.getInstance().playSfx("CLICK"); });
		nextArrow.setOnAction(e -> { cycleClass(1); AudioManager.getInstance().playSfx("CLICK"); });
		HBox navRow = new HBox(12, prevArrow, nextArrow);
		navRow.setAlignment(Pos.CENTER);

		// LAYOUT
		VBox previewColumn = new VBox(10, previewPane, navRow);
		previewColumn.setAlignment(Pos.CENTER);

		HBox topRow = new HBox(24, previewColumn, infoPanel);
		topRow.setAlignment(Pos.CENTER);
		// Fix overall height to avoid layout reflow during animation
		topRow.setMinHeight(300);
		topRow.setPrefHeight(300);

		updatePreview(); // initialize soldier by default with description

		// BUTTONS
		VBox buttonColumn = new VBox(12);
		buttonColumn.setAlignment(Pos.CENTER);
		rows.add(createMenuRow("Start", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			// Create a fresh game state while preserving audio settings.
			GameState existing = GameState.load();
			GameState gs = new GameState();
			// Preserve audio
			gs.setMasterVolume(existing.getMasterVolume());
			gs.setMusicVolume(existing.getMusicVolume());
			gs.setSfxVolume(existing.getSfxVolume());
			// Set gameplay defaults
			gs.setClassType(mapToClassType(current));
			gs.setHealth(100);
			gs.setStamina(100);
			gs.setAgility(10);
			gs.setAttack(10);
			gs.setDefense(10);
			gs.setScore(0);
			gs.setCoins(0);
			gs.setIsDead(false);
			gs.setLocation("Start");
			gs.save();
			// Navigate to the starting location.
			if (getScene() != null) {
				getScene().setRoot(new Start(onOpenMap, () -> {}));
			} else if (onBack != null) {
				onBack.run();
			}
		}));
		rows.add(createMenuRow("Back", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			if (onBack != null) {
				onBack.run();
			}
		}));
		for (MenuRow r : rows) buttonColumn.getChildren().add(r.container);

		VBox center = new VBox(28, titleBox, topRow, buttonColumn);
		center.setAlignment(Pos.TOP_CENTER);
		setCenter(center);

		// KEYBOARD NAVIGATION
		updateSelection(0);
		setFocusTraversable(true);
		Platform.runLater(this::requestFocus);
		addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.UP) { moveSelection(-1); AudioManager.getInstance().playSfx("CLICK"); e.consume(); }
			else if (e.getCode() == KeyCode.DOWN) { moveSelection(1); AudioManager.getInstance().playSfx("CLICK"); e.consume(); }
			else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) { rows.get(selectedIndex).button.fire(); e.consume(); }
			else if (e.getCode() == KeyCode.LEFT) { cycleClass(-1); e.consume(); }
			else if (e.getCode() == KeyCode.RIGHT) { cycleClass(1); e.consume(); }
		});
	}

	/**
	 * Cycles between the available player classes.
	 */
	private void cycleClass(int delta) {
		if (current == PlayerClass.SOLDIER && delta > 0) {
			current = PlayerClass.ARCHER;
		} else if (current == PlayerClass.ARCHER && delta > 0) {
			current = PlayerClass.SOLDIER;
		} else if (current == PlayerClass.SOLDIER && delta < 0) {
			current = PlayerClass.ARCHER;
		} else if (current == PlayerClass.ARCHER && delta < 0) {
			current = PlayerClass.SOLDIER;
		}
		updatePreview();
	}

	/**
	 * Updates the preview animation and description text for the currently selected class.
	 */
	private void updatePreview() {
		String path;
		if (current == PlayerClass.SOLDIER) {
			path = resolveImagePath(
				"/Assets/PlayerClasses/SoldierATK/0.png",
				"src/Assets/PlayerClasses/SoldierATK/0.png"
			);
			classNameLabel.setText("Soldier");
			classDescLabel.setText("Soldier: sturdy melee fighter with balanced stats.\nSpecial: strong close-range attack.");
		} else {
			path = resolveImagePath(
				"/Assets/PlayerClasses/ArcherATK/0.png",
				"src/Assets/PlayerClasses/ArcherATK/0.png"
			);
			classNameLabel.setText("Archer");
			classDescLabel.setText("Archer: agile ranged combatant focused on precision.\nSpecial: powerful attack from distance.");
		}
		preview.setImage(new Image(path));

		// Build the attack animation from the class's asset frames.
		String base = (current == PlayerClass.SOLDIER)
			? resolveBase("/Assets/PlayerClasses/SoldierATK/", "src/Assets/PlayerClasses/SoldierATK/")
			: resolveBase("/Assets/PlayerClasses/ArcherATK/", "src/Assets/PlayerClasses/ArcherATK/");
		String[] frames = (current == PlayerClass.SOLDIER)
			? new String[] { "0.png", "1.png", "2.png", "3.png", "4.png", "5.png" }
			: new String[] { "0.png", "1.png", "2.png", "3.png", "4.png", "5.png", "6.png", "7.png" };

		if (attackAnimation != null) attackAnimation.stop();
		attackAnimation = new Timeline();
		int delay = 120; // ms per frame
		for (int i = 0; i < frames.length; i++) {
			final String f = base + frames[i];
			attackAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(i * delay), e -> {
				preview.setImage(new Image(f));
			}));
		}
		attackAnimation.setCycleCount(Timeline.INDEFINITE);
		attackAnimation.play();
	}

	/**
	 * Helper method to resolve the base path for animation frames.
	 */
	private String resolveBase(String classpathDir, String fileDir) {
		URL url = getClass().getResource(classpathDir);
		if (url != null) return url.toExternalForm();
		return new File(fileDir).toURI().toString();
	}

	/**
	 * Helper method to resolve a single image path.
	 */
	private String resolveImagePath(String classpathResource, String filePath) {
		URL url = getClass().getResource(classpathResource);
		if (url != null) return url.toExternalForm();
		return new File(filePath).toURI().toString();
	}

	private Background buildBackground() {
		Image img = loadMenuImage();
		BackgroundSize size = new BackgroundSize(100,100,true,true,false,true);
		return new Background(new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, size));
	}

	private Image loadMenuImage() {
		URL url = getClass().getResource("/Assets/menu.png");
		if (url != null) return new Image(url.toExternalForm());
		return new Image(new File("src/Assets/menu.png").toURI().toString());
	}

	/**
	 * Creates a standard menu row with a single button.
	 */
	private MenuRow createMenuRow(String text, Runnable action) {
		Label left = new Label("❮");
		Label right = new Label("❯");
		styleArrow(left); styleArrow(right);
		left.setVisible(false); right.setVisible(false);

		Button btn = new Button(text);
		btn.setCursor(Cursor.HAND);
		btn.setOnAction(e -> action.run());
		btn.setFocusTraversable(false);
		btn.setStyle(minimalButtonStyle(false));

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
				"-fx-padding: 6 12 6 12;",
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
	private void selectRow(int index) {
		if (index >= 0 && index < rows.size()) updateSelection(index);
	}

	private void updateSelection(int index) {
		selectedIndex = index;
		for (int i = 0; i < rows.size(); i++) {
			MenuRow r = rows.get(i);
			boolean sel = i == selectedIndex;
			r.left.setVisible(sel);
			r.right.setVisible(sel);
			r.button.setStyle(minimalButtonStyle(sel));
		}
	}

	/**
	 * Maps the local PlayerClass enum to the global Actor.ClassType enum.
	 */
	private ClassType mapToClassType(PlayerClass pc) {
		if (pc == PlayerClass.ARCHER) return ClassType.ARCHER;
		return ClassType.SOLDIER;
	}

	/**
	 * A helper class to hold the components of a single menu row.
	 */
	private static class MenuRow {
		final HBox container; final Label left; final Button button; final Label right;
		MenuRow(HBox container, Label left, Button button, Label right) { this.container = container; this.left = left; this.button = button; this.right = right; }
	}
}

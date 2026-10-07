package Application.GameScreens;

// IMPORTS
import javafx.application.Platform;
import Application.AudioManager;
import Application.AudioManager.MusicTrack;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The main menu of the game.
 * It serves as the entry point, providing options to start, begin a new game,
 * access settings, view the tutorial, or quit.
 *
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 */
public class MainMenu extends BorderPane {
	// MENU STATE
	private final List<MenuRow> rows = new ArrayList<>();
	private int selectedIndex = 0;
    // NAVIGATION
    private final Runnable openSettings;
    private final Runnable startGame;
	private final Runnable openNewGame;

	/**
	 * Default constructor with no-op navigation actions.
	 */
	public MainMenu() {
		this(() -> {}, () -> {}, () -> {}); // no-op default
	}

	public MainMenu(Runnable openSettings) {
        this(openSettings, () -> {}, () -> {});
    }

	/**
	 * Constructs the MainMenu with specified navigation actions.
	 * @param openSettings Action to run when "Settings" is clicked.
	 * @param startGame Action to run when "Start Game" is clicked.
	 * @param openNewGame Action to run when "New Game" is clicked.
	 */
	public MainMenu(Runnable openSettings, Runnable startGame, Runnable openNewGame) {
        this.openSettings = openSettings;
        this.startGame = startGame;
        this.openNewGame = openNewGame;
		setPadding(new Insets(10));

		// Background image (menu.png) covering the entire view
		// BACKGROUND
		setBackground(buildBackground());

		// AUDIO
		AudioManager.getInstance().switchMusic(MusicTrack.MAIN_MENU);


		// LOGO
		ImageView titleImage = new ImageView(loadTitleImage());
		titleImage.setPreserveRatio(true);
		titleImage.setSmooth(true);
		titleImage.setCache(true);
		titleImage.setFitWidth(680);
		StackPane logoBox = new StackPane(titleImage);
		logoBox.setPadding(new Insets(40, 20, 10, 20));

		// MENU BUTTONS
		VBox buttonColumn = new VBox(10);
		buttonColumn.setAlignment(Pos.CENTER);
		buttonColumn.setPadding(new Insets(10, 0, 0, 0));

		rows.add(createMenuRow("Start Game", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			if (startGame != null) {
				startGame.run();
			} else if (getScene() != null) {
				javafx.scene.Scene sc = getScene();
				sc.setRoot(new Start(() -> sc.setRoot(new MainMenu(this.openSettings, this.startGame, this.openNewGame))));
			} else {
				System.out.println("Start Game clicked (no handler)");
			}
		}));
		rows.add(createMenuRow("New Game", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			if (openNewGame != null) openNewGame.run();
			else if (getScene() != null) {
				getScene().setRoot(new NewGame(() -> getScene().setRoot(new MainMenu(this.openSettings, this.startGame, this.openNewGame))));
			}
		}));
		rows.add(createMenuRow("Settings", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			// Prefer direct scene swap to ensure Settings opens from MainMenu
			if (getScene() != null) {
				javafx.scene.Scene sc = getScene();
				sc.setRoot(new Settings(() -> sc.setRoot(new MainMenu(this.openSettings, this.startGame, this.openNewGame))));
			} else if (openSettings != null) {
				openSettings.run();
			} else {
				System.out.println("Settings clicked (no handler)");
			}
		}));
		
		rows.add(createMenuRow("Tutorial", () -> {
		AudioManager.getInstance().playSfx("CLICK");
		// Prefer direct scene swap to ensure Tutorial opens from MainMenu
		if (getScene() != null) {
			javafx.scene.Scene sc = getScene();
			sc.setRoot(new TutorialScreen(() -> 
				sc.setRoot(new MainMenu(this.openSettings, this.startGame, this.openNewGame))
			));
		} else {
			System.out.println("Tutorial clicked (no handler)");
		}
		}));

		rows.add(createMenuRow("Quit Game", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			quit();
		}));

		for (MenuRow r : rows) {
			buttonColumn.getChildren().add(r.container);
		}

		// LAYOUT
		VBox centerBox = new VBox(28, logoBox, buttonColumn);
		centerBox.setAlignment(Pos.CENTER);
		centerBox.setPadding(new Insets(30, 0, 0, 0));

		setCenter(centerBox);

		// FOOTER
		Label authorsLabel = new Label("Brandon Nguyen, Nikolas Kiroff, Rosaline Liu, Uday Prashant");
		authorsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.35); -fx-padding: 4 8 4 8; -fx-background-radius: 6;");
		HBox bottomBar = new HBox(authorsLabel);
		bottomBar.setAlignment(Pos.BOTTOM_LEFT);
		bottomBar.setPadding(new Insets(5, 10, 10, 10));
		setBottom(bottomBar);

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
	private MenuRow createMenuRow(String text, Runnable action) {
		Label left = new Label("❮");
		Label right = new Label("❯");
		styleArrow(left);
		styleArrow(right);
		left.setVisible(false);
		right.setVisible(false);

		Button btn = new Button(text);
		btn.setCursor(Cursor.HAND);
		btn.setOnAction(e -> action.run());
		btn.setFocusTraversable(false);
		btn.setStyle(minimalButtonStyle(false));

		DropShadow glow = new DropShadow(18, Color.web("#ffffff80"));
		btn.setOnMouseEntered(e -> {
			selectRow(indexOfIfContains(btn));
			btn.setEffect(glow);
		});
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
		if (index >= 0 && index < rows.size()) {
			updateSelection(index);
		}
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
	 * Saves audio settings and closes the application window.
	 */
	private void quit() {
		// Persist current audio volumes using unified GameState before closing
		Application.Engine.GameState gs = Application.Engine.GameState.load();
		Application.AudioManager am = Application.AudioManager.getInstance();
		gs.captureAudioFrom(am);
		gs.save();

		if (getScene() != null && getScene().getWindow() != null) {
			getScene().getWindow().hide();
		}
	}

	/**
	 * Builds the background for the menu.
	 */
	private Background buildBackground() {
		Image img = loadMenuImage();
		BackgroundSize size = new BackgroundSize(100, 100, true, true, false, true); // cover
		BackgroundImage bgImg = new BackgroundImage(
				img,
				BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER,
				size
		);
		return new Background(bgImg);
	}

	/**
	 * Loads the background image for the menu.
	 */
	private Image loadMenuImage() {
		// Try classpath resource first: /Assets/menu.png
		URL url = getClass().getResource("/Assets/menu.png");
		if (url != null) {
			return new Image(url.toExternalForm());
		}
		// Fallback to dev file path
		File f = new File("src/Assets/menu.png");
		return new Image(f.toURI().toString());
	}

	/**
	 * Loads the title logo image for the menu.
	 */
	private Image loadTitleImage() {
		// Try classpath resource first: /Assets/title.png
		URL url = getClass().getResource("/Assets/title.png");
		if (url != null) {
			return new Image(url.toExternalForm());
		}
		// Fallback to dev file path
		File f = new File("src/Assets/title.png");
		return new Image(f.toURI().toString());
	}

	/**
	 * A helper class to hold the components of a single menu row.
	 */
	private static class MenuRow {
		final HBox container;
		final Label left;
		final Button button;
		final Label right;
		MenuRow(HBox container, Label left, Button button, Label right) {
			this.container = container;
			this.left = left;
			this.button = button;
			this.right = right;
		}
	}

	/**
	 * Finds the index of the row containing the given button.
	 */
	private int indexOfIfContains(Button btn) {
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i).button == btn) return i;
		}
		return -1;
	}
}

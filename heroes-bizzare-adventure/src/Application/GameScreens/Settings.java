package Application.GameScreens;

// IMPORTS
import Application.AudioManager;
import Application.AudioManager.MusicTrack;

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
 * The settings menu, providing navigation to Audio and Game Mode options.
 * It maintains a consistent style with the MainMenu.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Settings extends BorderPane {
	// NAVIGATION & STATE
	private final Runnable onBack;
	private final Runnable openAudio;
	private final Runnable openModes;
	private final List<MenuRow> rows = new ArrayList<>();
	private int selectedIndex = 0;

	public Settings(Runnable onBack) {
		this(onBack, null, null);
	}

	public Settings(Runnable onBack, Runnable openAudio) {
		this(onBack, openAudio, null);
	}

	public Settings(Runnable onBack, Runnable openAudio, Runnable openModes) {
		this.onBack = onBack;
		this.openAudio = openAudio;
		this.openModes = openModes;
		build();
	}

	/**
	 * Builds the UI components for the settings menu.
	 */
	private void build() {
		setPadding(new Insets(10));
		setBackground(buildBackground());

		// AUDIO
		AudioManager.getInstance().switchMusic(MusicTrack.MAIN_MENU);

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
			// Navigate directly to Audio scene; capture the current Scene to avoid null later
			javafx.scene.Scene sc = getScene();
			if (sc != null) {
				sc.setRoot(new Audio(() -> sc.setRoot(new Settings(this.onBack, this.openAudio, this.openModes))));
			} else if (openAudio != null) {
				openAudio.run();
			} else {
				System.out.println("Audio settings");
			}
		}));
		rows.add(createRow("Modes", () -> {
			AudioManager.getInstance().playSfx("CLICK");
			// If scene is available, open ModesMenu and ensure back returns here
			javafx.scene.Scene sc = getScene();
			if (sc != null) {
				sc.setRoot(new ModesMenu(() -> sc.setRoot(new Settings(this.onBack, this.openAudio, this.openModes))));
			} else if (openModes != null) {
				openModes.run();
			} else {
				System.out.println("Modes settings");
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

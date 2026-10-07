package Application.GameScreens;


// IMPORTS
import Application.AudioManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the UI for adjusting music and sound effect volumes.
 * This screen features sliders for volume control and a 'Back' button,
 * with support for keyboard navigation.
 */
public class Audio extends BorderPane {

	/** Callback for the Back button action. */
	private final Runnable onBack;
	/** The game's single audio controller. */
	private final AudioManager audioManager = AudioManager.getInstance();

	/** List of buttons that can be selected with the keyboard. */
	private final List<Button> selectableButtons = new ArrayList<>();
	/** Index of the currently selected button for keyboard navigation. */
	private int selectedIndex = 0;

	/**
	 * Constructs the Audio settings screen.
	 *
	 * @param onBack The action to perform when the user navigates back from this screen.
	 */
	public Audio(Runnable onBack) {
		this.onBack = onBack;
		build();
	}

	/**
	 * Builds UI for the Audio screen. Including title,
	 * volume sliders, and the back button, and sets up keyboard event handlers.
	 */
	private void build() {
		// Basic Setup
		setPadding(new Insets(10));
		setBackground(buildBackground());

		// Title
		Label title = new Label("AUDIO");
		title.setFont(Font.font("Verdana", 42));
		title.setTextFill(Color.WHITE);
		title.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 12, 0.4, 0, 3);");

		VBox titleBox = new VBox(title);
		titleBox.setAlignment(Pos.CENTER);
		titleBox.setPadding(new Insets(30, 0, 10, 0));

		// Music Volume Slider
		Label musicLabel = new Label("Music Volume");
		musicLabel.setTextFill(Color.WHITE);
		musicLabel.setStyle("-fx-font-size:16px; -fx-font-weight: bold;");
		Slider musicSlider = new Slider(0, 100, audioManager.getMusicVolume() * 100.0);
		musicSlider.setMajorTickUnit(25);
		musicSlider.setMinorTickCount(4);
		musicSlider.setShowTickMarks(true);
		musicSlider.setShowTickLabels(true);
		// Update AudioManager in real-time as the slider is moved.
		musicSlider.valueProperty().addListener((obs, oldV, newV) -> {
			float val = (float)(newV.doubleValue() / 100.0);
			audioManager.setMusicVolume(val);
		});
		HBox musicRow = new HBox(16, musicLabel, musicSlider);
		musicRow.setAlignment(Pos.CENTER);

		// Sound Effects Volume Slider
		Label sfxLabel = new Label("SFX Volume");
		sfxLabel.setTextFill(Color.WHITE);
		sfxLabel.setStyle("-fx-font-size:16px; -fx-font-weight: bold;");
		Slider sfxSlider = new Slider(0, 100, audioManager.getSfxVolume() * 100.0);
		sfxSlider.setMajorTickUnit(25);
		sfxSlider.setMinorTickCount(4);
		sfxSlider.setShowTickMarks(true);
		sfxSlider.setShowTickLabels(true);
		// Update AudioManager in real-time.
		sfxSlider.valueProperty().addListener((obs, oldV, newV) -> {
			float val = (float)(newV.doubleValue() / 100.0);
			audioManager.setSfxVolume(val);
		});
		HBox sfxRow = new HBox(16, sfxLabel, sfxSlider);
		sfxRow.setAlignment(Pos.CENTER);

		// Back Button
		HBox backRow = createBackRow();

		// Layout
		VBox list = new VBox(24, musicRow, sfxRow, backRow);
		list.setAlignment(Pos.CENTER);
		list.setPadding(new Insets(10, 0, 0, 0));

		VBox center = new VBox(30, titleBox, list);
		center.setAlignment(Pos.TOP_CENTER);
		setCenter(center);

		// Keyboard Navigation
		updateSelection(0);
		setFocusTraversable(true);
		Platform.runLater(this::requestFocus);
		addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
			switch (e.getCode()) {
				case UP, DOWN -> { // Only one selectable row; still play SFX
					AudioManager.getInstance().playSfx("CLICK");
					e.consume();
				}
				case ENTER, SPACE -> {
					selectableButtons.get(selectedIndex).fire();
					e.consume();
				}
				default -> { /* ignore other keys */ }
			}
		});
	}

	/**
	 * Creates the Back button row with selection arrows.
	 *
	 * @return An HBox containing the styled 'Back' button and its navigation arrows.
	 */
	private HBox createBackRow() {
		Label left = new Label("❮");
		Label right = new Label("❯");
		styleArrow(left); styleArrow(right);
		left.setVisible(false); right.setVisible(false);

		Button backBtn = new Button("Back");
		backBtn.setCursor(Cursor.HAND);
		backBtn.setFocusTraversable(false);
		backBtn.setStyle(minimalButtonStyle(false));
		backBtn.setOnAction(e -> {
			AudioManager.getInstance().playSfx("CLICK");
			// Use the provided callback to return to Settings
			if (onBack != null) onBack.run();
		});

		backBtn.setOnMouseEntered(e -> {
			selectedIndex = 0; // only one
			updateSelection(0);
		});
		backBtn.setOnMouseExited(e -> backBtn.setEffect(null));

		HBox row = new HBox(20, left, backBtn, right);
		row.setAlignment(Pos.CENTER);
		selectableButtons.add(backBtn);
		row.setOnMouseEntered(e -> { selectedIndex = 0; updateSelection(0); });
		return row;
	}

	/**
	 * Highlights the selected button for keyboard navigation.
	 * In this screen, it always selects the Back button.
	 *
	 * @param index The index of the button to mark as selected (always 0 here).
	 */
	private void updateSelection(int index) {
		if (selectableButtons.isEmpty()) return;
		// Only one button; apply selected style and arrows visibility
		Button b = selectableButtons.get(0);
		b.setStyle(minimalButtonStyle(true));
		// Arrows controlled via parent children
		HBox row = (HBox) b.getParent();
		if (row.getChildren().size() == 3) { // left, button, right
			row.getChildren().get(0).setVisible(true);
			row.getChildren().get(2).setVisible(true);
		}
	}

	/**
	 * Styles the arrow labels used for selection.
	 *
	 * @param arrow The Label to be styled.
	 */
	private void styleArrow(Label arrow) {
		arrow.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
		arrow.setOpacity(0.9);
	}

	/**
	 * Generates the CSS style for a menu button.
	 *
	 * @param selected true if the button is currently selected, false otherwise.
	 * @return A CSS style string for the button.
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
	 * Builds the background for the screen.
	 *
	 * @return A Background object to be applied to the pane.
	 */
	private Background buildBackground() {
		Image img = loadMenuImage();
		BackgroundSize size = new BackgroundSize(100,100,true,true,false,true);
		return new Background(new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, size));
	}

	/**
	 * Loads the background image from the classpath, with a fallback to the
	 * file system for development.
	 *
	 * @return The loaded Image object for the background.
	 */
	private Image loadMenuImage() {
		URL url = getClass().getResource("/Assets/menu.png");
		if (url != null) return new Image(url.toExternalForm());
		return new Image(new File("src/Assets/menu.png").toURI().toString());
	}
}

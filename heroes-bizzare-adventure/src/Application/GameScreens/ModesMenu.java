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
 * A menu screen for selecting game modes, such as difficulty.
 * This screen is navigated to from the PauseMenu.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class ModesMenu extends BorderPane {
    // NAVIGATION
    private final Runnable onBack;

    // AUDIO
    private final AudioManager audioManager = AudioManager.getInstance();

    // MENU STATE
    private final List<MenuRow> rows = new ArrayList<>();
    private int selectedIndex = 0;
    // Index for the selected difficulty (0=Easy, 1=Medium, 2=Hard).
    private int difficultyIndex = 0;

    /**
     * Constructs the ModesMenu.
     * @param onBack The action to run when the "Back" button is clicked.
     */
    public ModesMenu(Runnable onBack) {
        this.onBack = onBack;
        build();
    }

    /**
     * Builds the UI components for the modes menu.
     */
    private void build() {
        setPadding(new Insets(10));
        setBackground(buildBackground());

        // TITLE
        Label title = new Label("MODES");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 42px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 12, 0.4, 0, 3);");
        VBox titleBox = new VBox(title);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(30, 0, 10, 0));

        VBox listBox = new VBox(14);
        listBox.setAlignment(Pos.CENTER);

        // MENU ROWS
        rows.add(createDifficultyRow(
                new String[]{"Easy", "Medium", "Hard"},
                new Runnable[]{
                        () -> { AudioManager.getInstance().playSfx("CLICK"); System.out.println("Difficulty: Easy selected"); },
                        () -> { AudioManager.getInstance().playSfx("CLICK"); System.out.println("Difficulty: Medium selected"); },
                        () -> { AudioManager.getInstance().playSfx("CLICK"); System.out.println("Difficulty: Hard selected"); }
                }
        ));

        rows.add(createRow("Endless", () -> {
            AudioManager.getInstance().playSfx("CLICK");
            // locked: do not open
            System.out.println("Endless mode is locked");
        }, true));

        rows.add(createRow("Back", () -> {
            AudioManager.getInstance().playSfx("CLICK");
            if (onBack != null) onBack.run();
        }));

        for (MenuRow r : rows) listBox.getChildren().add(r.container);

        VBox center = new VBox(26, titleBox, listBox);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(20, 0, 0, 0));
        setCenter(center);

        updateSelection(0);
        // KEYBOARD NAVIGATION
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
            } else if (e.getCode() == KeyCode.LEFT) { // Move difficulty selection left.
                MenuRow r = rows.get(selectedIndex);
                if (r.buttons.size() > 1) {
                    difficultyIndex = (difficultyIndex - 1 + r.buttons.size()) % r.buttons.size();
                    updateSelection(selectedIndex);
                    AudioManager.getInstance().playSfx("CLICK");
                    e.consume();
                }
            } else if (e.getCode() == KeyCode.RIGHT) { // Move difficulty selection right.
                MenuRow r = rows.get(selectedIndex);
                if (r.buttons.size() > 1) {
                    difficultyIndex = (difficultyIndex + 1) % r.buttons.size();
                    updateSelection(selectedIndex);
                    AudioManager.getInstance().playSfx("CLICK");
                    e.consume();
                }
            } else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) { // Activate selected button.
                MenuRow sel = rows.get(selectedIndex);
                if (sel.buttons.size() == 1) sel.buttons.get(0).fire();
                else sel.buttons.get(difficultyIndex).fire();
                e.consume();
            }
        });
    }

    /**
     * Creates a standard menu row with a single button.
     */
    private MenuRow createRow(String text, Runnable action) {
        return createRow(text, action, false);
    }

    /**
     * Creates a standard menu row with a single button, with an option to disable it.
     * @param disabled If true, the button will be disabled.
     */
    private MenuRow createRow(String text, Runnable action, boolean disabled) {
        Label left = new Label("❮");
        Label right = new Label("❯");
        styleArrow(left); styleArrow(right);
        left.setVisible(false); right.setVisible(false);

        Button btn = new Button(text);
        btn.setCursor(Cursor.HAND);
        btn.setDisable(disabled);
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
        List<Button> btnList = new ArrayList<>(); btnList.add(btn);
        MenuRow row = new MenuRow(container, left, btnList, right);
        container.setOnMouseEntered(e -> selectRow(rows.indexOf(row)));
        return row;
    }

    /**
     * Creates a special menu row for selecting difficulty, containing multiple buttons.
     */
    private MenuRow createDifficultyRow(String[] labels, Runnable[] actions) {
        Label left = new Label("❮");
        Label right = new Label("❯");
        styleArrow(left); styleArrow(right);
        left.setVisible(false); right.setVisible(false);

        Button b1 = new Button(labels[0]);
        Button b2 = new Button(labels[1]);
        Button b3 = new Button(labels[2]);
        Button[] buttons = new Button[]{b1, b2, b3};
        for (int i = 0; i < buttons.length; i++) {
            Button b = buttons[i];
            final int idx = i;
            b.setCursor(Cursor.HAND);
            b.setFocusTraversable(false);
            b.setStyle(minimalButtonStyle(false));
            b.setOnAction(e -> actions[idx].run());
            DropShadow glow = new DropShadow(18, Color.web("#ffffff80"));
            b.setOnMouseEntered(e -> b.setEffect(glow));
            b.setOnMouseExited(e -> b.setEffect(null));
        }

        HBox difficultyBox = new HBox(24, b1, b2, b3);
        difficultyBox.setAlignment(Pos.CENTER);
        HBox container = new HBox(18, left, difficultyBox, right);
        container.setAlignment(Pos.CENTER);
        List<Button> btnList = new ArrayList<>(); btnList.add(b1); btnList.add(b2); btnList.add(b3);
        MenuRow row = new MenuRow(container, left, btnList, right);
        container.setOnMouseEntered(e -> {
            int idx = rows.indexOf(row);
            if (idx >= 0) selectRow(idx);
        });
        // per-button mouseenter should select the difficulty row and highlight the hovered difficulty
        for (int i = 0; i < buttons.length; i++) {
            final int idx = i;
            buttons[idx].setOnMouseEntered(e -> { difficultyIndex = idx; selectRow(rows.indexOf(row)); buttons[idx].setEffect(new DropShadow(18, Color.web("#ffffff80")));});
        }
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
            if (r.buttons.size() == 1) {
                r.buttons.get(0).setStyle(minimalButtonStyle(sel));
            } else {
                for (int j = 0; j < r.buttons.size(); j++) {
                    r.buttons.get(j).setStyle(minimalButtonStyle(sel && j == difficultyIndex));
                }
            }
        }
    }

    /**
     * Finds the index of the row containing the given button.
     */
    private int indexOf(Button btn) {
        for (int i = 0; i < rows.size(); i++) {
            MenuRow r = rows.get(i);
            for (Button b : r.buttons) if (b == btn) return i;
        }
        return -1;
    }

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
    private static class MenuRow { final HBox container; final Label left; final List<Button> buttons; final Label right; MenuRow(HBox c, Label l, List<Button> buttons, Label r){ container=c; left=l; this.buttons = buttons; right=r; }}
}

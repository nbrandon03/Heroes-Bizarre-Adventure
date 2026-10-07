package Application.GameScreens;

// IMPORTS
import Application.Player;
import Application.Engine.GameState;
import Application.Engine.GameEngine;
import Application.Mobs;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.File;

/**
 * An abstract base class for all game location screens.
 * It handles common functionality like background layers, player management,
 * pause menu integration, and location lifecycle events.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public abstract class LocationScreen extends StackPane {

    // PLAYER
    protected Player player;
    /** A callback to execute when returning to the world map. */
    protected final Runnable onBackToMap;
    /** A callback to execute when navigating to the next location. */
    protected final Runnable onNavigateNext;
    // GAME ENGINE
    protected GameEngine gameEngine;

    // LAYERS
    protected final StackPane backgroundLayer = new StackPane();
    protected final StackPane contentLayer = new StackPane();
    protected final StackPane overlayLayer = new StackPane();

    /**
     * Gets the path to the background image for this location.
     * @return A string representing the file path.
     */
    protected abstract String getBackgroundImagePath();
    /**
     * Gets the display name of the location.
     * @return The location name.
     */
    protected abstract String getLocationName();
    /**
     * Gets the name of the next location in the sequence.
     * @return The next location's name.
     */
    protected abstract String getNextLocationName();

    /**
     * Constructs a new LocationScreen.
     * @param onBackToMap The action to run when returning to the world map.
     * @param onNavigateNext The action to run when navigating to the next location.
     * @param player The player object.
     */
    public LocationScreen(Runnable onBackToMap, Runnable onNavigateNext, Player player) {
        this.player = player;
        this.onBackToMap = onBackToMap;
        this.onNavigateNext = onNavigateNext;
        this.gameEngine = GameEngine.getInstance();
        gameEngine.startLocation(getLocationName());

        // BACKGROUND IMAGE
        Image bgImage = loadBackgroundImage(getBackgroundImagePath());
ImageView bgView = new ImageView(bgImage);

// Fill the whole screen: no letterboxing
        bgView.setPreserveRatio(false);  // <-- changed from true to false
        bgView.setSmooth(true);

        // Bind to this LocationScreen's size so it always fills the view
        bgView.fitWidthProperty().bind(widthProperty());
        bgView.fitHeightProperty().bind(heightProperty());

        backgroundLayer.getChildren().add(bgView);
        backgroundLayer.setPickOnBounds(false);


        // LAYERING
        getChildren().setAll(backgroundLayer, contentLayer, overlayLayer);

        // Ensure overlay can receive clicks and sits above content
        overlayLayer.setMouseTransparent(false);
        overlayLayer.setPickOnBounds(true);
        contentLayer.setPickOnBounds(false);

        // LOCATION LABEL
        Label locationLabel = new Label(getLocationName());
        locationLabel.setFont(new Font("Arial", 24));
        locationLabel.setTextFill(Color.WHITE);
        locationLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 5, 0.5, 0, 1);");
        StackPane.setAlignment(locationLabel, Pos.TOP_CENTER);
        locationLabel.setPadding(new Insets(20));
        overlayLayer.getChildren().add(locationLabel);

        // OVERLAY
        overlayLayer.setMouseTransparent(false);
        overlayLayer.toFront();

        // PLAYER STATE
        if (this.player != null) {
            try {
                GameState gs = GameState.load();
                gs.applyToPlayer(this.player);
            } catch (Exception ignored) {}
        }

        // KEYBOARD CONTROLS
        setFocusTraversable(true);
        javafx.application.Platform.runLater(this::requestFocus);
        addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                javafx.scene.Scene sc = getScene();
                if (sc != null) {
                    try {
                        GameState gs = GameState.load();
                        if (player != null) gs.capturePlayer(player);
                        gs.setLocation(getLocationName());
                        gs.save();
                    } catch (Exception ignore) {}
                    sc.setRoot(new PauseMenu(() -> {
                        sc.setRoot(this);
                        javafx.application.Platform.runLater(this::requestFocus);
                        // Reinforce overlay z-order after returning
                        overlayLayer.setMouseTransparent(false);
                        overlayLayer.toFront();
                    }, null, null, player));
                }
                e.consume();
            }
        });
    }

    /**
     * Called when a mob in the location is killed. Updates game score and coins.
     * @param mob The mob that was killed.
     */
    protected void onMobKilled(Mobs mob) {
    if (mob != null && mob.isDead()) {
        gameEngine.onMobKilled(mob);
    }
    }

    /**
     * Loads a background image.
     * It tries to load from the classpath first and falls back to a file path if needed.
     * @param path The path to the image resource.
     */
    private Image loadBackgroundImage(String path) {
        try {
            // Try as classpath resource
            if (path != null) {
                var url = getClass().getResource(path.startsWith("/") ? path : "/" + path);
                if (url != null) {
                    return new Image(url.toExternalForm());
                }
            }
        } catch (Exception ignored) {}

        // Fallback: treat as file system path
        return new Image(new File(path).toURI().toString());
    }

    /**
     * Provides access to the content layer for subclasses.
     * @return The content layer pane.
     */
    protected Pane getContentLayer() { return contentLayer; }
}

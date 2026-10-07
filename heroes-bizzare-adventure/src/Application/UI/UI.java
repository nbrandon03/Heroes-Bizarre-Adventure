package Application.UI;

// IMPORTS
import Application.Actor.PortableObject;
import Application.Actor.Weapon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.net.URL;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import Application.Player;
import Application.Engine.GameState;
import Application.Engine.GameEngine;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

/**
 * Represents the main Heads-Up Display (HUD) for the game.
 * This class manages the display of player stats (health, stamina),
 * game info (location, score, time), and provides access to the map,
 * dialogue, and inventory.
 *
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 * @author Uday Prashant
 */
public class UI extends BorderPane {
    // UI STATE
    private final Label locationLabel = new Label("Location: Unknown");
    private String currentLocationName = "Unknown";

    // GAME STATS
    private final Label coinsLabel = new Label("Coins: 0");
    private final Label scoreLabel = new Label("Score: 0");
    private final Label timeLabel = new Label("Time: 00:00");
    private Timeline gameStatsTimeline = null;

    // PLAYER STATS BARS
    private final Region healthFill = new Region();
    private final Region staminaFill = new Region();
    private final StackPane healthBarContainer = new StackPane();
    private final StackPane staminaBarContainer = new StackPane();

    // PLAYER BINDING & EQUIPMENT
    private final ImageView weaponImage = new ImageView();
    private Player boundPlayer = null;
    private Timeline bindTimeline = null;

    // ACTION BUTTONS
    private final Button mapButton = new Button("Map");
    private final Button talkButton = new Button("Talk");
    
    
    // INVENTORY
    private StackPane inventoryOverlay = null;
    private ImageView inventoryImageView = new ImageView();
    private FlowPane inventoryGrid;
    private VBox itemDetailPane;

    // CONSTANTS
    private static final double BAR_WIDTH = 160;
    private static final double BAR_HEIGHT = 12;
    private static final int INVENTORY_SLOTS = 9;

    /**
     * Constructs the main UI component.
     */
    public UI() {
        setPickOnBounds(false);
        build();
        // Auto-attach inventory overlay when this UI is added to a parent pane.
        parentProperty().addListener((obs, oldParent, newParent) -> {
            try {
                if (newParent instanceof StackPane) attachOverlayTo((StackPane) newParent);
            } catch (Exception ignored) {}
        });
        setVisible(true);
    }

    /**
     * Builds the visual components of the UI.
     */
    private void build() {
        setPadding(new Insets(10));

        // TOP BAR
        locationLabel.setFont(new Font("Arial", 24));
        locationLabel.setTextFill(Color.WHITE);
        locationLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 5, 0.5, 0, 1);");

        coinsLabel.setTextFill(Color.GOLD);
        coinsLabel.setFont(Font.font("Verdana", 16));
        coinsLabel.setStyle("-fx-font-weight: bold");

        scoreLabel.setTextFill(Color.RED);
        scoreLabel.setFont(Font.font("Verdana", 16));
        scoreLabel.setStyle("-fx-font-weight: bold");

        timeLabel.setTextFill(Color.WHITE);
        timeLabel.setFont(Font.font("Verdana", 16));
        timeLabel.setStyle("-fx-font-weight: bold");

        mapButton.setFocusTraversable(false);
        mapButton.setStyle(
            "-fx-background-color: linear-gradient(#264e36,#12311b);" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 5 10 5 10;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // HEALTH BAR
        Region healthBg = new Region();
        healthBg.setStyle(
            "-fx-background-color: rgba(0,0,0,0.45);" +
            "-fx-border-color: rgba(255,255,255,0.08);" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;"
        );
        healthBg.setPrefSize(BAR_WIDTH, BAR_HEIGHT);

        healthFill.setStyle(
            "-fx-background-color: linear-gradient(#ff5555,#aa2222);" +
            "-fx-background-radius: 3;"
        );
        healthFill.setPrefHeight(BAR_HEIGHT);
        healthFill.setMinWidth(0);
        healthFill.setMaxWidth(BAR_WIDTH);
        healthFill.setPrefWidth(BAR_WIDTH);
        StackPane.setAlignment(healthFill, Pos.CENTER_LEFT);

        healthBarContainer.getChildren().addAll(healthBg, healthFill);

        // STAMINA BAR
          Region stamBg = new Region();
        stamBg.setStyle(
            "-fx-background-color: rgba(0,0,0,0.45);" +
            "-fx-border-color: rgba(255,255,255,0.08);" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;"
        );
        stamBg.setPrefSize(BAR_WIDTH, BAR_HEIGHT);

        staminaFill.setStyle(
            "-fx-background-color: linear-gradient(#77dd77,#1a8a1a);" +
            "-fx-background-radius: 3;"
        );
        staminaFill.setPrefHeight(BAR_HEIGHT);
        staminaFill.setMinWidth(0);
        staminaFill.setMaxWidth(BAR_WIDTH);
        staminaFill.setPrefWidth(BAR_WIDTH);
        StackPane.setAlignment(staminaFill, Pos.CENTER_LEFT);

        staminaBarContainer.getChildren().addAll(stamBg, staminaFill);

        VBox bars = new VBox(6, healthBarContainer, staminaBarContainer, coinsLabel, scoreLabel, timeLabel);
        bars.setAlignment(Pos.CENTER_LEFT);

        HBox leftBox = new HBox(12, bars, locationLabel);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        HBox topBar = new HBox(6, leftBox, spacer, mapButton);
        topBar.setAlignment(Pos.TOP_LEFT);
        topBar.setPadding(new Insets(6));
        setTop(topBar);

        setHealthPercent(1.0);
        setStaminaPercent(1.0);

        // RIGHT SIDE BUTTONS
        talkButton.setFocusTraversable(false);

        Region topSpacer = new Region();
        Region bottomSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        VBox right = new VBox(topSpacer, talkButton, bottomSpacer);
        right.setPadding(new Insets(0, 10, 0, 10));
        right.setAlignment(Pos.CENTER_RIGHT);
        setRight(right);

        setPrefHeight(120);
        setPrefWidth(380);

        // BOTTOM BAR (WEAPON & INVENTORY)
        StackPane equippedWeaponSlot = new StackPane();
        equippedWeaponSlot.setPrefSize(64, 64); // A good size for the HUD.
        equippedWeaponSlot.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 5; -fx-border-color: #ffdf85; -fx-border-radius: 5; -fx-border-width: 2;");

        weaponImage.setFitWidth(56);
        weaponImage.setFitHeight(56);
        equippedWeaponSlot.getChildren().add(weaponImage);

        Button inventory = new Button("Inventory");
        inventory.setFocusTraversable(false);
        inventory.setStyle(
            "-fx-background-color: linear-gradient(#6b3e1b,#3c2a13);" +
            "-fx-text-fill: #ffdf85;" +
            "-fx-border-color: transparent;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 8 4 8;" +
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;"
        );

            inventory.setOnAction(ev -> toggleInventoryOverlay());
            HBox bottomContents = new HBox(12, equippedWeaponSlot, inventory);
        bottomContents.setAlignment(Pos.CENTER);

        StackPane bottomCenter = new StackPane(bottomContents);
        bottomCenter.setPadding(new Insets(10));
        BorderPane.setAlignment(bottomCenter, Pos.BOTTOM_CENTER);
        setBottom(bottomCenter);
    }

    /**
     * Sets the action to be performed when the "Talk" button is clicked.
     * @param action The action to run.
     */
    public void setTalkButtonAction(Runnable action) {
        if (action == null) {
            talkButton.setOnAction(null);
            return;
        }
        talkButton.setOnAction(e -> {
            // Persist a snapshot of game state before triggering the action.
            try {
                GameState gs = GameState.load();
                if (boundPlayer != null) gs.capturePlayer(boundPlayer);
                gs.setLocation(currentLocationName != null ? currentLocationName : "Unknown");
                gs.save();
            } catch (Exception ignored) {}
            action.run();
        });
    }

    /**
     * Sets the displayed location name.
     * @param location The name of the current location.
     */
    public void setLocation(String location) {
        locationLabel.setText(location);
        currentLocationName = location;
    }

    /**
     * Sets the action to be performed when the "Map" button is clicked.
     * @param action The action to run.
     */
    public void setMapButtonAction(Runnable action) {
        mapButton.setOnAction(e -> {
            try {
                GameState gs = GameState.load();
                // Capture player state if bound, so stats/inventory persist.
                if (boundPlayer != null) {
                    // Keep Player coins in sync with GameEngine before persisting
                    GameEngine engine = GameEngine.getInstance();
                    boundPlayer.setNumCoins(engine.getTotalCoins());
                    gs.capturePlayer(boundPlayer);
                }
                // Persist current location for correct resume
                gs.setLocation(currentLocationName != null ? currentLocationName : "Unknown");
                gs.save();
            } catch (Exception ignored) { }
            action.run();
        });
    }

    /**
     * Sets the fill percentage of the health bar.
     * @param percent A value between 0.0 and 1.0.
     */
    public void setHealthPercent(double percent) {
        percent = Math.max(0.0, Math.min(1.0, percent));
        double width = BAR_WIDTH * percent;
        healthFill.setPrefWidth(width);
        healthFill.setMaxWidth(width);
    }

    /**
     * Sets the fill percentage of the stamina bar.
     * @param percent A value between 0.0 and 1.0.
     */
    public void setStaminaPercent(double percent) {
        percent = Math.max(0.0, Math.min(1.0, percent));
        double width = BAR_WIDTH * percent;
        staminaFill.setPrefWidth(width);
        staminaFill.setMaxWidth(width);
    }

    /**
     * Binds the UI to a Player object, allowing it to automatically update stats.
     * @param player The player to bind to.
     */
    public void bindToPlayer(Player player) {
        if (player == null) return;
        boundPlayer = player;

        if (bindTimeline != null) {
            bindTimeline.stop();
        }

        bindTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            double hp = boundPlayer.getHealth()  / (double) boundPlayer.getMaxHealth();
            double st = boundPlayer.getStamina() / (double) boundPlayer.getMaxStamina();

            Platform.runLater(() -> {
                setHealthPercent(hp);
                setStaminaPercent(st);

                // Update the equipped weapon icon.
                Weapon equipped = boundPlayer.getEquippedWeapon();
                if (equipped != null) {
                    // If a weapon is equipped, load its icon.
                    Image icon = loadWeaponImage(equipped.getIconPath(), equipped.getIconPath());
                    weaponImage.setImage(icon);
                } else {
                    // If no weapon is equipped, clear the image.
                    weaponImage.setImage(null);
                }
            });
        }));
        bindTimeline.setCycleCount(Timeline.INDEFINITE);
        bindTimeline.play();
    }
    
    /**
     * Unbinds the UI from the current player and stops automatic updates.
     */
    public void unbindPlayer() {
        if (bindTimeline != null) bindTimeline.stop();
        bindTimeline = null;
        boundPlayer = null;
        stopGameStatsUpdates();
    }

    /**
     * Starts a timeline to periodically update game stats like coins, score, and time.
     */
    public void startGameStatsUpdates() {
    if (gameStatsTimeline != null) {
        gameStatsTimeline.stop();
    }

    gameStatsTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> {
        GameEngine engine = GameEngine.getInstance();
        
        Platform.runLater(() -> {
            coinsLabel.setText("Coins: " + engine.getTotalCoins());
            scoreLabel.setText("Score: " + engine.getTotalScore());
            timeLabel.setText("Time: " + engine.getFormattedTime());
        });
    }));
    
    gameStatsTimeline.setCycleCount(Timeline.INDEFINITE);
    gameStatsTimeline.play();
    }

    /**
     * Stops the timeline that updates game stats.
     */
    public void stopGameStatsUpdates() {
    if (gameStatsTimeline != null) {
        gameStatsTimeline.stop();
        gameStatsTimeline = null;
    }
    }

    /**
     * Loads an image, trying the classpath first and falling back to a file path.
     * @param cpPath The classpath resource path.
     * @param devPath The development file system path.
     * @return The loaded Image object, or null if not found.
     */
    private Image loadWeaponImage(String cpPath, String devPath) {
        // Normalize the path to always start with a forward slash for classpath loading.
        String classpathPath = cpPath;
        if (!classpathPath.startsWith("/")) {
            classpathPath = "/" + classpathPath;
        }

        try {
            // Attempt to load from the classpath first.
            URL url = getClass().getResource(classpathPath);
            if (url != null) return new Image(url.toExternalForm());
            // Fallback to the development file path if classpath loading fails.
            File f = new File(devPath);
            return new Image(f.toURI().toString());
        } catch (Exception e) {
            return null;
        }
    }

    // INVENTORY OVERLAY
    /**
     * Creates the inventory overlay panel if it doesn't already exist.
     */
    private void ensureInventoryOverlay() {
        if (inventoryOverlay != null) return;
        inventoryOverlay = new StackPane();
        inventoryOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        inventoryOverlay.setVisible(false);
        inventoryOverlay.setPickOnBounds(true);

        // Load the book image that serves as the inventory background.
        Image book = loadWeaponImage("/Assets/Inventory/book.png", "src/Assets/Inventory/book.png");
        if (book == null) book = loadWeaponImage("/Assets/Inventory/0.png", "src/Assets/Inventory/0.png");
        if (book != null) {
            inventoryImageView.setImage(book);
            inventoryImageView.setPreserveRatio(true);
            inventoryImageView.setFitWidth(680); // A fixed larger size for the book.
        }

        // This FlowPane contains the inventory slots in a grid layout.
        inventoryGrid = new FlowPane();
        inventoryGrid.setPadding(new Insets(50, 20, 20, 35));
        inventoryGrid.setAlignment(Pos.CENTER);
        inventoryGrid.setHgap(8); // Reduced gap for smaller boxes.
        inventoryGrid.setVgap(8);
        inventoryGrid.setPrefWrapLength(200); // Approx 3 items wide (56*3 + 8*2 = 184).
        inventoryGrid.setPrefSize(280, 380); // Reverted to a smaller width for 3 columns.

        // This VBox shows details of a selected item.
        itemDetailPane = new VBox(10);
        itemDetailPane.setPadding(new Insets(40, 40, 20, 20));
        itemDetailPane.setAlignment(Pos.TOP_CENTER);
        itemDetailPane.setPrefSize(280, 380); // Restored original width.

        // Layout for the two pages of the book.
        HBox bookPages = new HBox(40, inventoryGrid, itemDetailPane);
        bookPages.setAlignment(Pos.CENTER);
        bookPages.setMaxWidth(640);

        Button exit = new Button("Close");
        exit.setOnAction(e -> setInventoryOverlayVisible(false));
        exit.setFocusTraversable(false);

        // A VBox to stack the book pages and the close button.
        VBox inventoryContent = new VBox(10, bookPages, exit);
        inventoryContent.setAlignment(Pos.CENTER);

        inventoryOverlay.getChildren().addAll(inventoryImageView, inventoryContent);
        StackPane.setAlignment(inventoryContent, Pos.CENTER);

        // Clicking the background area closes the inventory.
        inventoryOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == inventoryOverlay) setInventoryOverlayVisible(false);
        });
    }

    /**
     * Populates the inventory grid UI with items from the player's inventory.
     * This should be called whenever the inventory is opened.
     */
    private void populateInventory() {
        if (inventoryGrid == null || boundPlayer == null) {
            return;
        }

        // Clear any previous items from the grid and detail pane.
        inventoryGrid.getChildren().clear();
        itemDetailPane.getChildren().clear();

        List<PortableObject> playerItems = boundPlayer.getInventory();

        // Create a full grid of slots, filling them with items or leaving them empty.
        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            if (i < playerItems.size()) {
                // This slot has an item.
                PortableObject item = playerItems.get(i);
                if (item instanceof Weapon) {
                    inventoryGrid.getChildren().add(createItemSlot((Weapon) item));
                } else {
                    // Placeholder for other item types like potions in the future.
                    inventoryGrid.getChildren().add(createEmptySlot());
                }
            } else {
                // This is an empty slot.
                inventoryGrid.getChildren().add(createEmptySlot());
            }
        }
    }

    /**
     * Creates a visual placeholder for an empty inventory slot.
     */
    private StackPane createEmptySlot() {
        StackPane emptySlot = new StackPane();
        emptySlot.setPrefSize(56, 56);
        emptySlot.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 5; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 5;");
        return emptySlot;
    }

    /**
     * Creates a single visual slot for an item in the inventory grid.
     * @param item The item to represent.
     * @return A StackPane that acts as a clickable inventory slot.
     */
    private StackPane createItemSlot(PortableObject item) {
        StackPane slot = new StackPane();
        slot.setPrefSize(56, 56);
        slot.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 5; -fx-border-color: #ffdf85; -fx-border-radius: 5; -fx-border-width: 1;");

        // Handle different item types. Currently only Weapon is supported.
        if (item instanceof Weapon) {
            Weapon weapon = (Weapon) item;
            // Load the item's icon.
            Image iconImage = loadWeaponImage(weapon.getIconPath(), weapon.getIconPath());
            if (iconImage != null) {
                ImageView iconView = new ImageView(iconImage);
                iconView.setFitWidth(48); // Icon is slightly smaller than the slot for a nice border.
                iconView.setFitHeight(48);
                slot.getChildren().add(iconView);
            }

            // Add a tooltip to show the item name on hover.
            Tooltip.install(slot, new Tooltip(weapon.getName()));

            // When the slot is clicked, show the item's details.
            slot.setOnMouseClicked(e -> showItemDetails(weapon));
        } else {
            // Future items (like potions) can be handled here.
            // For now, just show a placeholder label if the type is unknown.
            Label placeholder = new Label("?");
            placeholder.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");
            slot.getChildren().add(placeholder);
        }

        return slot;
    }

    /**
     * Displays the details of a selected weapon on the right page of the inventory book,
     * including an "Equip" or "Unequip" button.
     * @param weapon The weapon whose details are to be shown.
     */
    private void showItemDetails(Weapon weapon) {
        itemDetailPane.getChildren().clear(); // Clear previous details.

        Label nameLabel = new Label(weapon.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label statsLabel = new Label("Damage: +" + weapon.getDamage());
        statsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #444;");

        // Determine if the button should be "Equip" or "Unequip".
        boolean isEquipped = (boundPlayer != null && weapon.equals(boundPlayer.getEquippedWeapon()));
        Button actionButton = new Button(isEquipped ? "Unequip" : "Equip");

        actionButton.setOnAction(e -> {
            if (isEquipped) {
                boundPlayer.unequipWeapon();
            } else {
                boundPlayer.equipWeapon(weapon);
            }

            // Close inventory to return focus to gameplay and allow attacks.
            setInventoryOverlayVisible(false);
            try {
                // Nudge focus back to the scene root to ensure key events reach the LocationScreen.
                if (getScene() != null && getScene().getRoot() instanceof javafx.scene.layout.Pane) {
                    getScene().getRoot().requestFocus();
                }
            } catch (Exception ignored) {}
        });

        itemDetailPane.getChildren().addAll(nameLabel, statsLabel, actionButton);
    }

    /**
     * Attaches the inventory overlay to a parent StackPane.
     * @param parent The parent pane to attach to.
     */
    public void attachOverlayTo(StackPane parent) {
        if (parent == null) return;
        ensureInventoryOverlay();
        // If overlay is attached to another parent, remove it first.
        if (inventoryOverlay.getParent() instanceof javafx.scene.layout.Pane) {
            javafx.scene.layout.Pane old = (javafx.scene.layout.Pane) inventoryOverlay.getParent();
            old.getChildren().remove(inventoryOverlay);
        }
        if (!parent.getChildren().contains(inventoryOverlay)) {
            parent.getChildren().add(inventoryOverlay);
        }
        StackPane.setAlignment(inventoryOverlay, Pos.CENTER);
        inventoryOverlay.prefWidthProperty().bind(parent.widthProperty());
        inventoryOverlay.prefHeightProperty().bind(parent.heightProperty());
        inventoryOverlay.toFront();
    }

    /**
     * Sets the visibility of the inventory overlay.
     * @param visible True to show the overlay, false to hide it.
     */
    private void setInventoryOverlayVisible(boolean visible) {
        ensureInventoryOverlay();
        inventoryOverlay.setVisible(visible);
        if (visible) {
            // When the inventory is made visible, populate it with the player's current items.
            populateInventory();
            inventoryOverlay.toFront();
        }
    }

    /**
     * Toggles the visibility of the inventory overlay.
     */
    private void toggleInventoryOverlay() {
        ensureInventoryOverlay();
        boolean isVisible = inventoryOverlay.isVisible();
        setInventoryOverlayVisible(!isVisible);

        // If we are opening the inventory, refresh its contents.
        if (!isVisible) {
            populateInventory();
        }
    }
}

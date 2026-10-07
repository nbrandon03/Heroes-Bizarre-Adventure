package Application.GameScreens;

// IMPORTS
import Application.Player;
import Application.AudioManager;
import Application.AudioManager.MusicTrack;
import Application.UI.UI;
import Application.Actor.PortableObject;
import Application.Actor.Weapon;
import Application.Actor.ClassType;
import Application.Engine.GameEngine;
import Application.Dialogue.DialogueManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shop location: supports buying weapons and selling the player's weapons.
 * Potions removed – weapons only.
 *
 * @author Nikolas Kiroff
 */
public class Shop extends LocationScreen {
    // SHOP OVERLAY
    private StackPane shopOverlay;

    // SHOP UI
    private Label shopCoinsLabel;
    private Label shopMessageLabel;
    private FlowPane weaponsFlow; // weapons for sale
    private FlowPane sellFlow;    // weapons to sell

    // SHOPKEEPER NPC
    private ImageView shopkView;
    private Timeline shopkAnim;
    private List<Image> shopkFrames;
    private int shopkIndex = 0;

    // DIALOGUE
    private DialogueManager dialogueManager;
    private Label dialogueLabel;
    private boolean dialogueFinished = false;

    // SHOP INVENTORY
    private final List<ShopWeapon> weaponItems = new ArrayList<>();

    /**
     * A simple descriptor for weapons sold in the shop.
     */
    private static class ShopWeapon {
        String displayName;
        String iconPathCp;        // classpath path, e.g. "/Assets/Weapons/Loot Pool/0.png"
        String iconPathRelative;  // relative path used by Weapon, e.g. "Assets/Weapons/Loot Pool/0.png"
        int price;
        int damage;
    }

    /**
     * Constructs the Shop screen.
     * @param onBackToMap Action to run when returning to the world map.
     * @param player The player object.
     */
    public Shop(Runnable onBackToMap, Player player) {
        super(onBackToMap, null, player); // no "next" location
        initShopWeapons();
        buildShopContent();
        AudioManager.getInstance().switchMusic(MusicTrack.GAMEPLAY);
    }

    @Override
    protected String getBackgroundImagePath() {
        return "/Assets/Locations/Shop.png";
    }

    @Override
    protected String getLocationName() {
        return "Shop";
    }

    @Override
    protected String getNextLocationName() { return null; }

    /**
     * Builds the base content for the shop screen, including the NPC and dialogue triggers.
     */
    private void buildShopContent() {
        // UI
        UI ui = new UI();
        ui.setLocation(getLocationName());
        ui.setMapButtonAction(onBackToMap);
        ui.setMouseTransparent(false);
        StackPane.setAlignment(ui, Pos.TOP_LEFT);
        overlayLayer.getChildren().add(ui);
        ui.toFront();
        if (player != null) ui.bindToPlayer(player);

        // keep coins/score/time updating in HUD
        ui.startGameStatsUpdates();

        // DIALOGUE
        dialogueManager = new DialogueManager("src/Assets/Dialogue/shop1.txt");

        dialogueLabel = new Label();
        dialogueLabel.setWrapText(true);
        dialogueLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-background-color: rgba(0,0,0,0.6);" +
                "-fx-padding: 8px;" +
                "-fx-background-radius: 8px;"
        );
        StackPane.setAlignment(dialogueLabel, Pos.CENTER);
        dialogueLabel.setTranslateY(-55);
        dialogueLabel.setVisible(false); // hidden until first Talk
        overlayLayer.getChildren().add(dialogueLabel);
        dialogueLabel.toFront();

        // Talk button behavior:
        //  - While dialogue not finished: advance dialogue
        //  - When dialogue ends: hide label + open shop
        //  - After that: Talk just toggles shop open/close
        ui.setTalkButtonAction(() -> {
            if (!dialogueFinished) {
                String line = dialogueManager.getNextLine();  // should return null when done
                if (line != null && !line.isEmpty()) {
                    dialogueLabel.setVisible(true);
                    dialogueLabel.setText(line);
                } else {
                    // no more lines -> mark finished & hide label
                    dialogueFinished = true;
                    dialogueLabel.setVisible(false);
                    dialogueLabel.setText("");
                    // open shop on this same click
                    toggleShopOverlay();
                }
            } else {
                // dialogue already done -> Talk toggles shop
                toggleShopOverlay();
            }
        });

        // PLAYER
        if (player != null) {
            StackPane.setAlignment(player.getNode(), Pos.BOTTOM_CENTER);
            player.getNode().setTranslateY(-40);
            getContentLayer().getChildren().add(player.getNode());
        }

        // NPC
        shopkFrames = loadFrames("src/Assets/NPC/Shop");
        if (!shopkFrames.isEmpty()) {
            shopkView = new ImageView(shopkFrames.get(0));
            shopkView.setPreserveRatio(true);
            shopkView.setSmooth(true);
            shopkView.fitWidthProperty().bind(widthProperty().multiply(0.10));

            StackPane.setAlignment(shopkView, Pos.BOTTOM_LEFT);
            shopkView.setTranslateX(550);
            shopkView.setTranslateY(-90);

            getContentLayer().getChildren().add(shopkView);

            shopkAnim = new Timeline(new KeyFrame(Duration.millis(350),
                    e -> advanceFrame(shopkView, shopkFrames, shopkIndex++)));
            shopkAnim.setCycleCount(Timeline.INDEFINITE);
            shopkAnim.play();
        }

        // FINAL UI ADJUSTMENTS
        javafx.application.Platform.runLater(() -> {
            overlayLayer.setMouseTransparent(false);
            overlayLayer.toFront();
            ui.toFront();
            if (dialogueLabel != null) dialogueLabel.toFront();
        });
    }

    /**
     * Initializes the list of weapons available for purchase based on the player's class.
     */
    private void initShopWeapons() {
        weaponItems.clear();

        // Decide which folder to use based on the player's class
        String folderRel;     // relative path, e.g. "Assets/Weapons/Archer/"
        String baseName;      // label prefix if we run out of fancy names
        boolean skipFirstPngForStarter = false;

        // Fancy name pools
        String[] archerNames = {
                "Goblin Slayer",
                "Eagle Eye",
                "Dragonpiercer"
        };

        String[] soldierNames = {
                "Iron Longsword",
                "Knight's Edge",
                "Demon Splitter",
                "Crusader's Oath",
                "Dragonrender"
        };

        String[] namePool;
        int maxShopItems;

        if (player != null && player.getClassType() == ClassType.ARCHER) {
            folderRel = "Assets/Weapons/Archer/";
            baseName  = "Bow ";
            namePool  = archerNames;
            // FIRST PNG = starter bow → skip it in shop
            skipFirstPngForStarter = true;
            maxShopItems = 3;   // Archer: only 3 bows for sale
        } else {
            // Soldier / default uses the loot pool
            folderRel = "Assets/Weapons/Warrior/";
            baseName  = "Weapon ";
            namePool  = soldierNames;
            maxShopItems = 5;   // Soldier: show up to 5 swords
        }

        // Dev-time folder to scan actual PNG files
        String devFolder = "src/" + folderRel;
        File dir = new File(devFolder);
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));

        if (files == null || files.length == 0) {
            return; // no assets found, nothing to sell
        }

        // Keep a stable order (0.png,1.png,...)
        Arrays.sort(files);

        int startIndex = 0;
        if (skipFirstPngForStarter && files.length > 0) {
            // 0.png is the starter bow → not sold in shop
            startIndex = 1;
        }

        int shopIndex = 0;
        for (int i = startIndex; i < files.length && shopIndex < maxShopItems; i++) {
            File f = files[i];
            String fileName = f.getName(); // e.g. "0.png"

            ShopWeapon item = new ShopWeapon();

            // Nice name if available, otherwise a generic fallback
            if (namePool != null && shopIndex < namePool.length) {
                item.displayName = namePool[shopIndex];
            } else {
                item.displayName = baseName + (shopIndex + 1);
            }

            item.iconPathRelative = folderRel + fileName; // e.g. "Assets/Weapons/Archer/1.png"
            item.iconPathCp       = "/" + item.iconPathRelative; // "/Assets/Weapons/Archer/1.png"
            item.price            = 40 + shopIndex * 20;   // 40, 60, 80, ...
            item.damage           = 10 + shopIndex * 5;    // 10, 15, 20, ...

            weaponItems.add(item);
            shopIndex++;
        }
    }

    /**
     * Creates the main shop panel overlay if it doesn't exist.
     */
    private void ensureShopOverlay() {
        if (shopOverlay != null) return;

        shopOverlay = new StackPane();
        shopOverlay.setPickOnBounds(false);
        shopOverlay.setPadding(new Insets(20));
        shopOverlay.setStyle("-fx-background-color: transparent;");

        VBox panel = new VBox(16);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPadding(new Insets(16));
        panel.setMaxWidth(460);
        panel.setStyle(
                "-fx-background-color: rgba(15,15,22,0.96);" +
                "-fx-background-radius: 16;" +
                "-fx-border-radius: 16;" +
                "-fx-border-color: #ffdf85;" +
                "-fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 20, 0.35, 0, 4);"
        );

        // TITLE & COINS
        Label title = new Label("Weapon Shop");
        title.setStyle(
                "-fx-font-size: 24px;" +
                "-fx-text-fill: #ffdf85;" +
                "-fx-font-weight: bold;"
        );

        shopCoinsLabel = new Label();
        shopCoinsLabel.setStyle("-fx-text-fill: #ffdf85; -fx-font-size: 16px;");

        HBox titleRow = new HBox(10, title, shopCoinsLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // STATUS MESSAGE
        shopMessageLabel = new Label("");
        shopMessageLabel.setStyle("-fx-text-fill: #ffaaaa; -fx-font-size: 12px;");

        // BUY SECTION
        Label weaponsHeader = new Label("Weapons (Buy)");
        weaponsHeader.setStyle(
                "-fx-text-fill: #f4f4f4;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;"
        );

        weaponsFlow = new FlowPane();
        weaponsFlow.setHgap(10);
        weaponsFlow.setVgap(10);
        weaponsFlow.setPadding(new Insets(4, 0, 4, 0));
        weaponsFlow.setPrefWrapLength(400);
        weaponsFlow.setAlignment(Pos.TOP_LEFT);

        // SELL SECTION
        Label sellHeader = new Label("Your Weapons (Sell)");
        sellHeader.setStyle(
                "-fx-text-fill: #f4f4f4;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;"
        );

        sellFlow = new FlowPane();
        sellFlow.setHgap(10);
        sellFlow.setVgap(10);
        sellFlow.setPadding(new Insets(4, 0, 4, 0));
        sellFlow.setPrefWrapLength(400);
        sellFlow.setAlignment(Pos.TOP_LEFT);

        // CLOSE BUTTON
        Button closeButton = new Button("Close Shop");
        closeButton.setOnAction(e -> toggleShopOverlay());
        closeButton.setStyle(
                "-fx-background-color: #ffdf85;" +
                "-fx-text-fill: #2b2220;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 16 6 16;" +
                "-fx-background-radius: 12;"
        );

        HBox closeRow = new HBox(closeButton);
        closeRow.setAlignment(Pos.CENTER_RIGHT);
        closeRow.setPadding(new Insets(8, 0, 0, 0));

        panel.getChildren().addAll(
                titleRow,
                shopMessageLabel,
                createSeparator(),
                weaponsHeader,
                weaponsFlow,
                createSeparator(),
                sellHeader,
                sellFlow,
                createSeparator(),
                closeRow
        );

        shopOverlay.getChildren().add(panel);
        StackPane.setAlignment(panel, Pos.CENTER_LEFT);
        panel.setTranslateX(40);

        // First fill
        refreshShopUI();
    }

    /**
     * Creates a horizontal line separator for the UI.
     */
    private HBox createSeparator() {
        HBox sep = new HBox();
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.15);");
        return sep;
    }

    /**
     * Toggles the visibility of the shop overlay panel.
     */
    private void toggleShopOverlay() {
        ensureShopOverlay();
        if (overlayLayer.getChildren().contains(shopOverlay)) {
            // === CLOSING SHOP: persist player + inventory ===
            overlayLayer.getChildren().remove(shopOverlay);

            try {
                Application.Engine.GameState gs = Application.Engine.GameState.load();
                if (player != null) {
                    // keep GameEngine coins and Player numCoins in sync (defensive)
                    GameEngine engine = GameEngine.getInstance();
                    player.setNumCoins(engine.getTotalCoins());
                    gs.capturePlayer(player);
                }
                gs.save();
            } catch (Exception ignored) { }
        } else {
            // === OPENING SHOP: refresh and show panel ===
            refreshShopUI();
            overlayLayer.getChildren().add(shopOverlay);
            overlayLayer.toFront();
            shopOverlay.toFront();
        }
    }

    /**
     * Refreshes all UI elements within the shop panel to reflect the current game state.
     */
    private void refreshShopUI() {
        if (shopOverlay == null) return;

        GameEngine engine = GameEngine.getInstance();
        int coins = engine.getTotalCoins();
        shopCoinsLabel.setText("Coins: " + coins);

        shopMessageLabel.setText("");
        weaponsFlow.getChildren().clear();
        sellFlow.getChildren().clear();

        // Weapons for sale
        for (ShopWeapon item : weaponItems) {
            weaponsFlow.getChildren().add(buildBuyCard(item));
        }

        // Weapons owned by player (to sell)
        if (player != null) {
            List<PortableObject> inv = player.getInventory();
            if (inv != null) {
                for (PortableObject obj : inv) {
                    if (obj instanceof Weapon) {
                        sellFlow.getChildren().add(buildSellCard((Weapon) obj));
                    }
                }
            }
        }
    }

    /**
     * Builds a UI card for a weapon available for purchase.
     */
    private VBox buildBuyCard(ShopWeapon item) {
        ImageView icon = new ImageView();
        icon.setFitWidth(32);
        icon.setFitHeight(32);
        try {
            String path = getClass().getResource(item.iconPathCp).toExternalForm();
            icon.setImage(new Image(path));
        } catch (Exception ignored) {}

        String dispName = (item.displayName == null || item.displayName.trim().isEmpty())
                ? "Weapon"
                : item.displayName;

        Label nameLabel = new Label(dispName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");

        Label priceLabel = new Label(item.price + " coins");
        priceLabel.setStyle("-fx-text-fill: #ffdf85; -fx-font-size: 11px;");

        Button buyButton = new Button("Buy");
        buyButton.setOnAction(e -> tryBuy(item));

        VBox box = new VBox(4, icon, nameLabel, priceLabel, buyButton);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(4));
        box.setStyle(
                "-fx-background-color: rgba(0,0,0,0.4);" +
                "-fx-background-radius: 8;"
        );
        return box;
    }

    /**
     * Builds a UI card for a weapon in the player's inventory that can be sold.
     */
    private VBox buildSellCard(Weapon weapon) {
        ImageView icon = new ImageView();
        icon.setFitWidth(32);
        icon.setFitHeight(32);
        try {
            String cp = "/" + weapon.getIconPath(); // weapon stores "Assets/..." so add leading "/"
            String path = getClass().getResource(cp).toExternalForm();
            icon.setImage(new Image(path));
        } catch (Exception ignored) {}

        String dispName = weapon.getName();
        if (dispName == null || dispName.trim().isEmpty()) {
            // Generic fallback name based on class
            if (player != null && player.getClassType() == ClassType.ARCHER) {
                dispName = "Bow";
            } else {
                dispName = "Weapon";
            }
        }

        Label nameLabel = new Label(dispName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");

        int sellPrice = getSellPriceForWeapon(weapon);
        Label priceLabel = new Label("Sell for " + sellPrice);
        priceLabel.setStyle("-fx-text-fill: #98ff98; -fx-font-size: 11px;");

        Button sellButton = new Button("Sell");
        sellButton.setOnAction(e -> trySell(weapon, sellPrice));

        VBox box = new VBox(4, icon, nameLabel, priceLabel, sellButton);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(4));
        box.setStyle(
                "-fx-background-color: rgba(0,0,0,0.4);" +
                "-fx-background-radius: 8;"
        );
        return box;
    }

    /**
     * Handles the logic for purchasing a weapon from the shop.
     */
    private void tryBuy(ShopWeapon item) {
        if (player == null) {
            shopMessageLabel.setText("No player bound.");
            return;
        }

        GameEngine engine = GameEngine.getInstance();
        int coins = engine.getTotalCoins();

        if (coins < item.price) {
            shopMessageLabel.setText("Not enough coins!");
            return;
        }

        // Spend coins
        engine.addCoins(-item.price);
        player.setNumCoins(engine.getTotalCoins()); // keep Player.numCoins in sync

        // Persist coins and player state immediately
        try {
            Application.Engine.GameState gs = Application.Engine.GameState.getGlobal();
            if (gs != null) {
                gs.capturePlayer(player);
            }
            Application.Engine.GameState.saveGlobal();
        } catch (Exception ignored) {}

        String dispName = (item.displayName == null || item.displayName.trim().isEmpty())
                ? "Weapon"
                : item.displayName;

        // Create a Weapon using your constructor:
        // Weapon(ClassType, String name, int damage, String iconPath, int tier)
        Weapon newWeapon = new Weapon(
                player.getClassType(),
                dispName,
                item.damage,
                item.iconPathRelative,   // e.g. "Assets/Weapons/Archer/1.png"
                1                         // tier
        );
        player.addToInventory(newWeapon);

        shopMessageLabel.setText("You bought " + dispName + "!");
        refreshShopUI();
    }

    /**
     * Handles the logic for selling a player's weapon to the shop.
     */
    private void trySell(Weapon weapon, int sellPrice) {
        if (player == null) {
            shopMessageLabel.setText("No player bound.");
            return;
        }

        GameEngine engine = GameEngine.getInstance();

        // Remove from inventory
        player.removeFromInventory(weapon);

        // If equipped, unequip
        if (weapon.equals(player.getEquippedWeapon())) {
            player.unequipWeapon();
        }

        // Give coins
        engine.addCoins(sellPrice);
        player.setNumCoins(engine.getTotalCoins());

        // Persist coins and player state immediately
        try {
            Application.Engine.GameState gs = Application.Engine.GameState.getGlobal();
            if (gs != null) {
                gs.capturePlayer(player);
            }
            Application.Engine.GameState.saveGlobal();
        } catch (Exception ignored) {}

        String dispName = weapon.getName();
        if (dispName == null || dispName.trim().isEmpty()) {
            if (player != null && player.getClassType() == ClassType.ARCHER) {
                dispName = "Bow";
            } else {
                dispName = "Weapon";
            }
        }

        shopMessageLabel.setText("You sold " + dispName + " for " + sellPrice + " coins.");
        refreshShopUI();
    }

    /**
     * Calculates the sell price for a given weapon.
     * Rule: 5 coins per damage point, with a minimum of 5 coins.
     */
    private int getSellPriceForWeapon(Weapon weapon) {
        int base = weapon.getDamage() * 5;
        return Math.max(5, base);
    }

    /**
     * Loads all .png image frames from a given directory.
     */
    private List<Image> loadFrames(String directoryPath) {
        List<Image> frames = new ArrayList<>();
        File[] files = new File(directoryPath).listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files != null) {
            for (File file : files) {
                try {
                    frames.add(new Image(file.toURI().toString()));
                } catch (Exception ignored) {}
            }
        }
        return frames;
    }

    /**
     * Advances the animation frame for a given ImageView.
     */
    private void advanceFrame(ImageView view, List<Image> frames, int index) {
        if (frames == null || frames.isEmpty() || view == null) return;
        int frameIndex = index % frames.size();
        view.setImage(frames.get(frameIndex));
    }
}

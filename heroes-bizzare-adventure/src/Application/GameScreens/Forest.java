package Application.GameScreens;

// IMPORTS
import Application.Player;
import Application.UI.UI;
import Application.Dialogue.DialogueManager;  
import Application.Mobs;
import Application.AudioManager;
import Application.Engine.GameState;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The Forest location screen. This is a hostile area where the player
 * can fight a mob and interact with an NPC.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Forest extends LocationScreen {

    // KID NPC
    private ImageView kidView;
    private Timeline kidAnim;
    private List<Image> kidFrames;
    private int kidIndex = 0;

    // DIALOGUE
    private DialogueManager dialogueManager;
    private Label dialogueLabel;

    // PLAYER MOVEMENT
    private Timeline moveTimeline;
    private Double targetTX; // desired translateX
    private Double targetTY; // desired translateY relative to bottom center anchor

    // MOB
    private Mobs forestMob;
    private Timeline proximityWatcher;

    // DEATH SCREEN OVERLAY
    private DeathScreen deathScreen;

    /**
     * Constructs the Forest screen.
     * @param onBackToMap Action to run when returning to the world map.
     * @param onNavigateNext Action to run after this location is cleared.
     * @param player The player object.
     */
    public Forest(Runnable onBackToMap, Runnable onNavigateNext, Player player) {
        super(onBackToMap, onNavigateNext, player);
        buildForestContent();
    }

    // Provides the background image path for this location.
    @Override
    protected String getBackgroundImagePath() {
        return "src/Assets/Locations/Forest.png";
    }

    // Provides the name of this location.
    @Override
    protected String getLocationName() {
        return "Forest";
    }

    // Provides the name of the next location.
    @Override
    protected String getNextLocationName() { 
        return "Dungeon"; 
    }

    /**
     * Builds all the content for the forest screen, including UI, player,
     * the mob, NPC, and event handlers for gameplay.
     */
    private void buildForestContent() {
        // UI
        UI ui = new UI();
        ui.setLocation(getLocationName());
        ui.setMapButtonAction(onBackToMap);
        ui.setMouseTransparent(false);
        StackPane.setAlignment(ui, Pos.TOP_LEFT);
        ui.setMaxWidth(Double.MAX_VALUE);
        ui.setMaxHeight(Double.MAX_VALUE);
        overlayLayer.getChildren().add(ui);
        overlayLayer.setMouseTransparent(false);
        overlayLayer.toFront();
        ui.toFront();
        if (player != null) {
            ui.bindToPlayer(player);
            ui.startGameStatsUpdates();
        }

        // PLAYER
        if (player != null) {
            StackPane.setAlignment(player.getNode(), Pos.BOTTOM_CENTER);
            player.getNode().setTranslateY(-40);
            getContentLayer().getChildren().add(player.getNode());
        }

        // NPC
        kidFrames = loadFrames("src/Assets/NPC/Forest kid");
        if (!kidFrames.isEmpty()) {
            kidView = new ImageView(kidFrames.get(0));
            kidView.setPreserveRatio(true);
            kidView.setSmooth(true);
            kidView.fitWidthProperty().bind(widthProperty().multiply(0.10)); // ~10% of screen width

            StackPane.setAlignment(kidView, Pos.BOTTOM_LEFT);
            kidView.setTranslateX(150);
            kidView.setTranslateY(-100);

            getContentLayer().getChildren().add(kidView);

            kidAnim = new Timeline(new KeyFrame(Duration.millis(350),
                    e -> advanceFrame(kidView, kidFrames, kidIndex++)));
            kidAnim.setCycleCount(Timeline.INDEFINITE);
            kidAnim.play();
        }

        // DIALOGUE
        dialogueManager = new DialogueManager("src/Assets/Dialogue/forest.txt");

        // DIALOGUE LABEL
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

        getChildren().add(dialogueLabel);
        dialogueLabel.toFront();

        // HIDE BY DEFAULT
        dialogueLabel.setVisible(false);
        dialogueLabel.setManaged(false);

        // TALK BUTTON ACTION
        ui.setTalkButtonAction(() -> {
            if (dialogueManager == null) return;

            String line = dialogueManager.getNextLine();
            if (line != null) {
                line = line.trim();
            }

            if (line != null && !line.isEmpty()) {
                dialogueLabel.setText(line);
                dialogueLabel.setVisible(true);
                dialogueLabel.setManaged(true);
                dialogueLabel.toFront();
            } else {
                // HIDE BOX WHEN DIALOGUE ENDS
                dialogueLabel.setText("");
                dialogueLabel.setVisible(false);
                dialogueLabel.setManaged(false);
            }
        });

        // MOB
        GameState gs = GameState.getGlobal();
        boolean forestCleared = gs.isLocationCleared(getLocationName());
        if (!forestCleared) {
            try {
                forestMob = new Mobs(Mobs.MobType.FOREST);
            } catch (Exception ignored) {
                forestMob = new Mobs(Mobs.MobType.GRASSLANDS);
            }
            forestMob.setAttack(35);
            forestMob.getNode().setPreserveRatio(true);
            forestMob.getNode().setSmooth(true);
            forestMob.getNode().fitWidthProperty().bind(widthProperty().multiply(0.30));
            StackPane.setAlignment(forestMob.getNode(), Pos.BOTTOM_RIGHT);
            forestMob.getNode().setTranslateX(-120);
            forestMob.getNode().setTranslateY(-110);
            getContentLayer().getChildren().add(forestMob.getNode());
        } else {
            forestMob = null;
        }

        // MOB ATTACK LOGIC
        final int proximityThreshold = 110;
        final int attackCooldownMs = 1200;
        proximityWatcher = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            if (player == null || player.isDead()) return;
            if (forestMob == null) return;
            if (forestMob.isDead()) {
                if (!forestCleared) {
                    gs.markLocationCleared(getLocationName());
                    onMobKilled(forestMob);
                    gs.save();
                }
                return;
            }
            javafx.geometry.Bounds pb = player.getNode().getBoundsInParent();
            javafx.geometry.Bounds mb = forestMob.getNode().getBoundsInParent();
            double px = pb.getMinX() + pb.getWidth()/2.0;
            double mx = mb.getMinX() + mb.getWidth()/2.0;
            double dist = Math.abs(px - mx);

            Long lastAttack = (Long) forestMob.getNode().getProperties().getOrDefault("forest.lastAttack", 0L);
            long now = System.currentTimeMillis();
            boolean cooledDown = (now - lastAttack) >= attackCooldownMs;
            if (dist <= proximityThreshold && cooledDown) {
                forestMob.getNode().getProperties().put("forest.lastAttack", now);
                forestMob.attackPlayer(player);
                if (player.isDead()) {
                    handlePlayerDeath();
                }
            }
        }));
        proximityWatcher.setCycleCount(Timeline.INDEFINITE);
        proximityWatcher.play();

        // PLAYER MOVEMENT
        setFocusTraversable(true);
        javafx.application.Platform.runLater(this::requestFocus);
        addEventHandler(MouseEvent.MOUSE_CLICKED, me -> {
            if (player == null || player.isDead()) return;
            if (me.getButton() == MouseButton.PRIMARY) {
                double clickX = me.getX();
                double clickY = me.getY();
                double centerX = getWidth() / 2.0;
                double bottomY = getHeight();
                double desiredTX = clickX - centerX;
                double desiredTY = clickY - bottomY;
                // Clamp vertical to player floor or mob foot
                double footClamp = -20;
                if (forestMob != null) footClamp = forestMob.getNode().getTranslateY();
                if (desiredTY < footClamp) desiredTY = footClamp;
                startMoveTowards(desiredTX, desiredTY);
                me.consume();
            }
        });

        // PLAYER ATTACK
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (player == null) return;
            if (e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                int cost = 20;
                if (player.getStamina() >= cost && !player.isDead()) {
                    player.spendStamina(cost);
                    player.startAttack();
                    int frames = (player.getClassType() == Application.Actor.ClassType.ARCHER ? 8 : 6);
                    PauseTransition pt = new PauseTransition(Duration.millis(frames * 140));
                    pt.setOnFinished(ev -> {
                        if (!player.isDead()) {
                            if (forestMob != null && !forestMob.isDead()) {
                                javafx.geometry.Bounds pb2 = player.getNode().getBoundsInParent();
                                javafx.geometry.Bounds mb2 = forestMob.getNode().getBoundsInParent();
                                double px2 = pb2.getMinX() + pb2.getWidth()/2.0;
                                double mx2 = mb2.getMinX() + mb2.getWidth()/2.0;
                                double distX = Math.abs(px2 - mx2);
                                final int playerAttackRange = (player.getClassType() == Application.Actor.ClassType.ARCHER ? 150 : 110);
                                if (distX <= playerAttackRange) {
                                    player.attackMob(forestMob);
                                    if (forestMob.isDead()) {
                                        if (proximityWatcher != null) { try { proximityWatcher.stop(); } catch (Exception ignored2) {} proximityWatcher = null; }
                                        try { forestMob.startDeath(); } catch (Exception ignored2) {}
                                        GameState.getGlobal().markLocationCleared(getLocationName());
                                        onMobKilled(forestMob);
                                        GameState.saveGlobal();
                                    } else {
                                        try { forestMob.startHurt(); } catch (Exception ignored2) {}
                                        AudioManager.getInstance().playSfx("HIT");
                                    }
                                }
                            }
                            player.showIdle();
                        }
                    });
                    pt.play();
                }
                e.consume();
            }
        });

        // FINAL UI ADJUSTMENTS
        javafx.application.Platform.runLater(() -> {
            overlayLayer.setMouseTransparent(false);
            overlayLayer.toFront();
            if (overlayLayer.getChildren().contains(ui)) {
                ui.setMouseTransparent(false);
                ui.toFront();
            }
            if (dialogueLabel != null) dialogueLabel.toFront();
        });

        ui.toFront();
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

    /**
     * Starts moving the player towards a target coordinate.
     * @param tx The target X coordinate.
     * @param ty The target Y coordinate.
     */
    private void startMoveTowards(double tx, double ty) {
        this.targetTX = tx;
        this.targetTY = ty;
        if (moveTimeline != null) moveTimeline.stop();
        if (player != null) player.startWalk();
        moveTimeline = new Timeline(new KeyFrame(Duration.millis(16), e -> stepMove())); // ~60 FPS
        moveTimeline.setCycleCount(Timeline.INDEFINITE);
        moveTimeline.play();
    }

    /**
     * A single step in the player's movement, called by the movement timeline.
     */
    private void stepMove() {
        if (player == null || targetTX == null || targetTY == null) { stopMove(); return; }
        double speed = Math.max(1.0, player.getAgility() * 0.5);
        double cx = player.getNode().getTranslateX();
        double cy = player.getNode().getTranslateY();
        double dx = targetTX - cx;
        // Clamp vertical to floor
        double footClamp = -20.0;
        try {
            for (javafx.scene.Node n : getContentLayer().getChildren()) {
                if (n == null) continue;
                double ty = n.getTranslateY();
                if (ty < footClamp) footClamp = ty;
            }
        } catch (Exception ignored) {}
        if (targetTY < footClamp) targetTY = footClamp;
        double dy = targetTY - cy;
        double dist = Math.hypot(dx, dy);
        if (dist <= speed) {
            player.getNode().setTranslateX(targetTX);
            player.getNode().setTranslateY(targetTY);
            stopMove();
            return;
        }
        double nx = cx + (dx / dist) * speed;
        double ny = cy + (dy / dist) * speed;
        if (ny < footClamp) ny = footClamp;
        player.getNode().setTranslateX(nx);
        player.getNode().setTranslateY(ny);
    }

    /**
     * Stops the player's movement and sets their animation back to idle.
     */
    private void stopMove() {
        if (moveTimeline != null) moveTimeline.stop();
        moveTimeline = null;
        targetTX = null;
        targetTY = null;
        if (player != null && !player.isDead()) player.showIdle();
    }

    /**
     * Handles the player's death by stopping game interactions and showing the death screen.
     */
    private void handlePlayerDeath() {
        stopMove();
        if (proximityWatcher != null) {
            proximityWatcher.stop();
            proximityWatcher = null;
        }
        if (kidAnim != null) kidAnim.stop();

        // Disable only the gameplay layer, not the whole screen
        getContentLayer().setDisable(true);

        if (deathScreen == null) {
            deathScreen = new DeathScreen(() -> {
                if (getScene() != null) {
                    javafx.scene.Scene sc = getScene();
                    MainMenu menu = new MainMenu(
                        () -> sc.setRoot(new Settings(() -> sc.setRoot(new MainMenu()))),
                        () -> sc.setRoot(new Start(() -> sc.setRoot(new MainMenu()))),
                        () -> sc.setRoot(new NewGame(
                            () -> sc.setRoot(new MainMenu()),
                            () -> {
                                java.util.Map<String, Runnable> nav = new java.util.HashMap<>();

                                java.util.function.BiFunction<javafx.scene.Scene, Application.Player, Runnable> backToMapFactory = (sceneRef, playerRef) -> () -> sceneRef.setRoot(new WorldMap(
                                        () -> sceneRef.setRoot(new Start(() -> sc.setRoot(new MainMenu()), () -> {})),
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
                                        () -> sc.setRoot(new WorldMap(
                                            () -> sc.setRoot(new Start(() -> sc.setRoot(new MainMenu()), () -> {})),
                                            nav
                                        )),
                                        p
                                    ));
                                });

                                sc.setRoot(new WorldMap(
                                        () -> sc.setRoot(new Start(() -> sc.setRoot(new MainMenu()), () -> {})),
                                        nav
                                ));
                            }
                        ))
                    );
                    sc.setRoot(menu);
                } else {
                    AudioManager.getInstance().switchMusic(Application.AudioManager.MusicTrack.MAIN_MENU);
                }
            });
            StackPane.setAlignment(deathScreen, Pos.CENTER);
        }

        if (!overlayLayer.getChildren().contains(deathScreen)) {
            overlayLayer.getChildren().add(deathScreen);
        }
        overlayLayer.toFront();
    }
}

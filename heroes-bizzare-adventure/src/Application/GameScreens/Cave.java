package Application.GameScreens;

// IMPORTS
import Application.Player;
import Application.AudioManager;
import Application.AudioManager.MusicTrack;
import Application.Mobs;
import Application.UI.UI;
import Application.Engine.GameState;
import Application.Dialogue.DialogueManager;    
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;               
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyEvent;
import javafx.animation.PauseTransition;
import javafx.scene.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The Cave location screen. This is a hostile area where the player
 * can fight a mob and interact with an NPC.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Cave extends LocationScreen {

    // Fields for handling player point-and-click movement.
    private Timeline moveTimeline;
    private Double targetTX; // desired translateX
    private Double targetTY; // desired translateY relative to bottom center anchor

    // Fields for the Old Man NPC.
    private ImageView oldmView;
    private Timeline oldmAnim;
    private List<Image> oldmFrames;
    private int oldmIndex = 0;

    // Fields for managing and displaying dialogue.
    private DialogueManager dialogueManager;
    private Label dialogueLabel;

    // Fields for the mob and its attack logic.
    private Mobs caveMob;
    private Timeline proximityWatcher;

    // The overlay screen shown when the player dies.
    private DeathScreen deathScreen;

    /**
     * Constructs the Cave screen.
     * @param onBackToMap Action to run when returning to the world map.
     * @param onNavigateNext Action to run after this location is cleared.
     * @param player The player object.
     */
    public Cave(Runnable onBackToMap, Runnable onNavigateNext, Player player) {
        super(onBackToMap, onNavigateNext, player);
        buildCaveContent();
        AudioManager.getInstance().switchMusic(MusicTrack.GAMEPLAY);
    }

    // Provides the background image path for this location.
    @Override
    protected String getBackgroundImagePath() { return "src/Assets/Locations/Cave.png"; }

    // Provides the name of this location.
    @Override
    protected String getLocationName() { return "Cave"; }

    // Provides the name of the next location.
    @Override
    protected String getNextLocationName() { return "Mountain"; }

    /**
     * Builds all the content for the cave screen, including UI, player,
     * the mob, NPC, and event handlers for gameplay.
     */
    private void buildCaveContent() {
        // UI
        UI ui = new UI();
        ui.setLocation(getLocationName());
        ui.setMapButtonAction(onBackToMap);
        ui.setMouseTransparent(false);
        StackPane.setAlignment(ui, Pos.TOP_LEFT);
        ui.setMaxWidth(Double.MAX_VALUE);
        ui.setMaxHeight(Double.MAX_VALUE);
        overlayLayer.getChildren().add(ui);
        // Make sure the UI is on top and can be clicked.
        overlayLayer.setMouseTransparent(false);
        overlayLayer.toFront();

        // MOB
        GameState gs = GameState.getGlobal();
        boolean caveCleared = gs.isLocationCleared(getLocationName());
        if (!caveCleared) {
            caveMob = new Mobs(Mobs.MobType.CAVE);
            caveMob.setAttack(35);
            caveMob.getNode().setPreserveRatio(true);
            caveMob.getNode().setSmooth(true);
            caveMob.getNode().fitWidthProperty().bind(widthProperty().multiply(0.32));
            StackPane.setAlignment(caveMob.getNode(), Pos.BOTTOM_RIGHT);
            caveMob.getNode().setTranslateX(-140);
            caveMob.getNode().setTranslateY(-135);
            getContentLayer().getChildren().add(caveMob.getNode());
        } else {
            caveMob = null;
        }
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


        // MOB ATTACK LOGIC
        final int proximityThreshold = 110;
        final int attackCooldownMs = 1200;
        proximityWatcher = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            if (player == null || player.isDead()) return;
            if (caveMob == null) return;
            if (caveMob.isDead()) {
                // If the mob is dead, save the cleared state and stop checking.
                if (!gs.isLocationCleared(getLocationName())) {
                    gs.markLocationCleared(getLocationName());
                    onMobKilled(caveMob);
                    gs.save();
                }
                return;
            }

            // Check the distance between the player and the mob.
            javafx.geometry.Bounds pb = player.getNode().getBoundsInParent();
            javafx.geometry.Bounds mb = caveMob.getNode().getBoundsInParent();
            double px = pb.getMinX() + pb.getWidth()/2.0;
            double mx = mb.getMinX() + mb.getWidth()/2.0;
            double dist = Math.abs(px - mx);

            Long lastAttack = (Long) caveMob.getNode().getProperties().getOrDefault("cave.lastAttack", 0L);
            long now = System.currentTimeMillis();
            boolean cooledDown = (now - lastAttack) >= attackCooldownMs;

            // If the player is close enough and the cooldown is over, the mob attacks.
            if (dist <= proximityThreshold && cooledDown) {
                caveMob.getNode().getProperties().put("cave.lastAttack", now);
                caveMob.attackPlayer(player);
                if (player.isDead()) {
                    handlePlayerDeath();
                }
            }
        }));
        proximityWatcher.setCycleCount(Timeline.INDEFINITE);
        proximityWatcher.play();

        // PLAYER MOVEMENT
        addEventHandler(MouseEvent.MOUSE_CLICKED, me -> {
            if (player == null || player.isDead()) return;
            if (me.getButton() == MouseButton.PRIMARY) {
                double clickX = me.getX();
                double clickY = me.getY();

                double centerX = getWidth() / 2.0;
                double desiredTX = clickX - centerX;

                double bottomY = getHeight();
                double currentAbsY = bottomY + player.getNode().getTranslateY();
                double desiredTY = player.getNode().getTranslateY() + (clickY - currentAbsY);
                if (desiredTY < -40) {
                    desiredTY = -40;
                }

                startMoveTowards(desiredTX, desiredTY);
                me.consume();
            }
        });

        // PLAYER ATTACK
        setFocusTraversable(true);
        javafx.application.Platform.runLater(this::requestFocus);
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
                        if (!player.isDead()) { // After the attack animation finishes, check if the mob was hit.
                            if (caveMob != null && !caveMob.isDead()) {
                                javafx.geometry.Bounds pb2 = player.getNode().getBoundsInParent();
                                javafx.geometry.Bounds mb2 = caveMob.getNode().getBoundsInParent();
                                double px2 = pb2.getMinX() + pb2.getWidth()/2.0;
                                double mx2 = mb2.getMinX() + mb2.getWidth()/2.0;
                                double distX = Math.abs(px2 - mx2);
                                final int playerAttackRange = (player.getClassType() == Application.Actor.ClassType.ARCHER ? 150 : 110);

                                // If the mob is in range, deal damage.
                                if (distX <= playerAttackRange) {
                                    player.attackMob(caveMob);
                                    if (caveMob.isDead()) {
                                        // If the mob is defeated, stop its attack logic and save progress.
                                        if (proximityWatcher != null) { try { proximityWatcher.stop(); } catch (Exception ignored) {} proximityWatcher = null; }
                                        try { caveMob.startDeath(); } catch (Exception ignored) {}
                                        GameState.getGlobal().markLocationCleared("Cave");
                                        onMobKilled(caveMob);
                                        GameState.saveGlobal();
                                    } else {
                                        // If the mob is just hurt, play a sound and a visual shake effect.
                                        try { caveMob.startHurt(); } catch (Exception ignored) {}
                                        shakeNode(caveMob.getNode(), 8, 140);
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

        // NPC
        oldmFrames = loadFrames("src/Assets/NPC/Cave Oldman");
        if (!oldmFrames.isEmpty()) {
            oldmView = new ImageView(oldmFrames.get(0));
            oldmView.setPreserveRatio(true);
            oldmView.setSmooth(true);
            oldmView.fitWidthProperty().bind(widthProperty().multiply(0.10)); // ~10% of screen width

            // Position the NPC on the screen.
            StackPane.setAlignment(oldmView, Pos.BOTTOM_LEFT);
            oldmView.setTranslateX(150);  // Move 150px from the left edge
            oldmView.setTranslateY(-100); // Raised up from bottom

            getContentLayer().getChildren().add(oldmView);

            // Start the NPC's idle animation.
            oldmAnim = new Timeline(new KeyFrame(Duration.millis(350),
                    e -> advanceFrame(oldmView, oldmFrames, oldmIndex++)));
            oldmAnim.setCycleCount(Timeline.INDEFINITE);
            oldmAnim.play();
        }

        // DIALOGUE
        dialogueManager = new DialogueManager("src/Assets/Dialogue/cave.txt");

        // The label that displays dialogue text.
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

        // Hide the dialogue box by default.
        dialogueLabel.setVisible(false);
        dialogueLabel.setManaged(false);

        // When the Talk button is pressed, show the next line of dialogue.
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
                // Hide the box when there are no more lines.
                dialogueLabel.setText("");
                dialogueLabel.setVisible(false);
                dialogueLabel.setManaged(false);
            }
        });

        // Make sure the UI is on top and can be clicked.
        javafx.application.Platform.runLater(() -> {
            overlayLayer.setMouseTransparent(false);
            overlayLayer.toFront();
            if (overlayLayer.getChildren().contains(ui)) {
                ui.setMouseTransparent(false);
                ui.toFront();
            }
            if (dialogueLabel != null) {
                dialogueLabel.toFront();
            }
        });
    }

    /**
     * Starts moving the player towards a target coordinate.
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
        if (targetTY < -40) targetTY = -40.0;
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
        if (ny < -40) ny = -40.0;
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
     * Handles the player's death by stopping game interactions and showing the death screen.
     */
    private void handlePlayerDeath() {
        stopMove();
        if (proximityWatcher != null) {
            proximityWatcher.stop();
            proximityWatcher = null;
        }

        if (oldmAnim != null) {
            oldmAnim.stop();
        }

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
                    AudioManager.getInstance().switchMusic(MusicTrack.MAIN_MENU);
                }
            });
            StackPane.setAlignment(deathScreen, Pos.CENTER);
        }

        if (!overlayLayer.getChildren().contains(deathScreen)) {
            overlayLayer.getChildren().add(deathScreen);
        }
        overlayLayer.toFront();
    }

    /**
     * A simple visual effect to shake a node, used for hurt feedback.
     */
    private void shakeNode(Node node, double amplitude, double durationMs) {
        if (node == null) return;
        double originalX = node.getTranslateX();
        Timeline shake = new Timeline(
                new KeyFrame(Duration.millis(0),    e -> node.setTranslateX(originalX - amplitude)),
                new KeyFrame(Duration.millis(durationMs * 0.33), e -> node.setTranslateX(originalX + amplitude)),
                new KeyFrame(Duration.millis(durationMs * 0.66), e -> node.setTranslateX(originalX - amplitude)),
                new KeyFrame(Duration.millis(durationMs),        e -> node.setTranslateX(originalX))
        );
        shake.setCycleCount(1);
        shake.play();
    }
}

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The Grasslands location screen. This is a hostile area where the player
 * can fight a mob and interact with an NPC.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Grasslands extends LocationScreen {

    // FARMER NPC
    private ImageView farmerView;
    private Timeline farmerAnim;
    private List<Image> farmerFrames;
    private int farmerIndex = 0;

    // PLAYER MOVEMENT
    private Timeline moveTimeline;
    private Double targetTX; // desired translateX
    private Double targetTY; // desired translateY relative to bottom center anchor

    // MOB
    private Timeline proximityWatcher;

    // Track if mob already cleared (persisted)
    private boolean grassMobCleared;

    // DIALOGUE
    private DialogueManager dialogueManager;
    private Label dialogueLabel;

    // DEATH SCREEN OVERLAY
    private DeathScreen DeathScreen;

    /**
     * Constructs the Grasslands screen.
     * @param onBackToMap Action to run when returning to the world map.
     * @param onNavigateNext Action to run after this location is cleared.
     * @param player The player object.
     */
    public Grasslands(Runnable onBackToMap, Runnable onNavigateNext, Player player) {
        super(onBackToMap, onNavigateNext, player);
        buildGrasslandsContent();
        AudioManager.getInstance().switchMusic(MusicTrack.GAMEPLAY);
    }

    // Provides the background image path for this location.
    @Override
    protected String getBackgroundImagePath() {
        return "src/Assets/Locations/Grasslands.png";
    }

    // Provides the name of this location.
    @Override
    protected String getLocationName() {
        return "Grasslands";
    }

    @Override
    protected String getNextLocationName() { 
        return "Forest";
    }

    /**
     * Builds all the content for the grasslands screen, including UI, player,
     * the mob, NPC, and event handlers for gameplay.
     */
    private void buildGrasslandsContent() {
        // UI
        UI ui = new UI();
        ui.setLocation(getLocationName());
        ui.setMapButtonAction(onBackToMap);
        ui.setMouseTransparent(false);
        StackPane.setAlignment(ui, Pos.TOP_LEFT);
        ui.setMaxWidth(Double.MAX_VALUE);
        ui.setMaxHeight(Double.MAX_VALUE);
        overlayLayer.getChildren().add(ui);
        // Ensure overlay stays interactable and on top
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

        // FARMER NPC
        farmerFrames = loadFrames("src/Assets/NPC/Grassland Farmer");
        if (!farmerFrames.isEmpty()) {
            farmerView = new ImageView(farmerFrames.get(0));
            farmerView.setPreserveRatio(true);
            farmerView.setSmooth(true);
            farmerView.fitWidthProperty().bind(widthProperty().multiply(0.10)); // ~10% of screen width

            // FARMER POSITION
            StackPane.setAlignment(farmerView, Pos.BOTTOM_LEFT);
            farmerView.setTranslateX(150);  // Move 150px from the left edge
            farmerView.setTranslateY(-100); // Raised up from bottom

            getContentLayer().getChildren().add(farmerView);

            // Start idle animation
            farmerAnim = new Timeline(new KeyFrame(Duration.millis(350),
                    e -> advanceFrame(farmerView, farmerFrames, farmerIndex++)));
            farmerAnim.setCycleCount(Timeline.INDEFINITE);
            farmerAnim.play();
        }

        // DIALOGUE
        dialogueManager = new DialogueManager("src/Assets/Dialogue/grassland.txt");

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
        grassMobCleared = gs.isLocationCleared(getLocationName());
        final Mobs grassMob;
        if (!grassMobCleared) {
            grassMob = new Mobs(Mobs.MobType.GRASSLANDS);
            grassMob.setAttack(35); // Increased mob damage
            StackPane.setAlignment(grassMob.getNode(), Pos.BOTTOM_RIGHT);
            grassMob.getNode().setTranslateX(-120);
            grassMob.getNode().setTranslateY(-135);
            getContentLayer().getChildren().add(grassMob.getNode());
        } else {
            grassMob = null;
        }

        // MOB ATTACK LOGIC
        final int proximityThreshold = 110; // widen a bit for consistent feel
        final int attackCooldownMs = 1200;  // minimum time between mob attacks

        if (grassMob != null) {
            proximityWatcher = new Timeline(new KeyFrame(Duration.millis(100), e -> {
                if (player == null || player.isDead()) return;
                if (grassMob.isDead()) {
                    if (!grassMobCleared) {
                        grassMobCleared = true;
                        gs.markLocationCleared(getLocationName());
                        onMobKilled(grassMob);
                        gs.save();
                    }
                    return;
                }
                // Use visual centers and X-only distance for robustness
                javafx.geometry.Bounds pb = player.getNode().getBoundsInParent();
                javafx.geometry.Bounds mb = grassMob.getNode().getBoundsInParent();
                double px = pb.getMinX() + pb.getWidth()/2.0;
                double mx = mb.getMinX() + mb.getWidth()/2.0;
                double dist = Math.abs(px - mx);

                Long lastAttack = (Long) grassMob.getNode().getProperties()
                        .getOrDefault("grasslands.lastAttack", 0L);
                long now = System.currentTimeMillis();
                boolean cooledDown = (now - lastAttack) >= attackCooldownMs;

                if (dist <= proximityThreshold && cooledDown) {
                    grassMob.getNode().getProperties().put("grasslands.lastAttack", now);
                    grassMob.attackPlayer(player);
                    if (player.isDead()) {
                        handlePlayerDeath();
                    }
                }
            }));
            proximityWatcher.setCycleCount(Timeline.INDEFINITE);
            proximityWatcher.play();
        }

        // PLAYER MOVEMENT
        setFocusTraversable(true);
        javafx.application.Platform.runLater(this::requestFocus);
        addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, me -> {
            if (player == null || player.isDead()) return;
            if (me.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                double clickX = me.getX();
                double clickY = me.getY();
                double centerX = getWidth() / 2.0;
                double bottomY = getHeight();
                double desiredTX = clickX - centerX;
                double desiredTY = clickY - bottomY;
                // Clamp to the mob's foot level (use mob translateY when present)
                double footClamp = -40;
                if (grassMob != null) footClamp = grassMob.getNode().getTranslateY();
                if (desiredTY < footClamp) {
                    desiredTY = footClamp;
                }

                startMoveTowards(desiredTX, desiredTY);
                me.consume();
            }
        });

        // PLAYER ATTACK
        addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (player == null) return;
            if (e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                int cost = 20;
                if (player.getStamina() >= cost && !player.isDead()) {
                    player.spendStamina(cost);
                    player.startAttack();

                    final Mobs localMob = grassMob;
                    // Class-specific attack ranges: Archer longer, Soldier shorter
                    final int playerAttackRange =
                            (player.getClassType() == Application.Actor.ClassType.ARCHER ? 150 : 110);

                    int frames = (player.getClassType() == Application.Actor.ClassType.ARCHER ? 8 : 6);
                    javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(
                            Duration.millis(frames * 140));
                    pt.setOnFinished(ev -> {
                        if (localMob != null && !player.isDead() && !localMob.isDead()) {
                            // Use bounds centers and X-only distance like Boss
                            javafx.geometry.Bounds pb2 = player.getNode().getBoundsInParent();
                            javafx.geometry.Bounds mb2 = localMob.getNode().getBoundsInParent();
                            double px2 = pb2.getMinX() + pb2.getWidth()/2.0;
                            double mx2 = mb2.getMinX() + mb2.getWidth()/2.0;
                            double distX = Math.abs(px2 - mx2);
                            if (distX <= playerAttackRange) {
                                player.attackMob(localMob);
                                // Show hurt if still alive; if dead, let mob's death flow handle persistence
                                if (localMob.isDead() && !grassMobCleared) {
                                    grassMobCleared = true;
                                    gs.markLocationCleared(getLocationName());
                                    onMobKilled(grassMob);
                                    gs.save();
                                } else {
                                    try { localMob.startHurt(); } catch (Exception ignored) {}
                                        // play hit sound on successful non-lethal hit
                                        AudioManager.getInstance().playSfx("HIT");
                                }
                            }
                        }
                        if (!player.isDead()) player.showIdle();
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
            if (dialogueLabel != null) {
                dialogueLabel.toFront();
            }
        });
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
        if (player == null || targetTX == null || targetTY == null) {
            stopMove();
            return;
        }
        double speed = Math.max(1.0, player.getAgility() * 0.5);
        double cx = player.getNode().getTranslateX();
        double cy = player.getNode().getTranslateY();
        double dx = targetTX - cx;
        // Ensure vertical target stays at mob foot level or below
        double footClamp = -40.0;
        try {
            // Heuristic: inspect content layer nodes for lowest translateY as "floor"
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
        // Stop movement and proximity watcher
        stopMove();
        if (proximityWatcher != null) {
            proximityWatcher.stop();
            proximityWatcher = null;
        }
        // Stop NPC animations
        if (farmerAnim != null) {
            farmerAnim.stop();
        }

        // Disable only the gameplay layer, not the whole screen,
        // so the DeathScreen buttons stay clickable.
        getContentLayer().setDisable(true);

        if (DeathScreen == null) {
            DeathScreen = new DeathScreen(() -> {
                // Navigate to Main Menu with working button handlers
                if (getScene() != null) {
                    javafx.scene.Scene sc = getScene();
                    MainMenu menu = new MainMenu(
                        () -> sc.setRoot(new Settings(() -> sc.setRoot(new MainMenu()))),
                        () -> sc.setRoot(new Start(() -> sc.setRoot(new MainMenu()))),
                        () -> sc.setRoot(new NewGame(
                            () -> sc.setRoot(new MainMenu()),
                            () -> {
                                java.util.Map<String, Runnable> nav = new java.util.HashMap<>();

                                java.util.function.BiFunction<javafx.scene.Scene, Application.Player, Runnable> backToMapFactory =
                                        (sceneRef, playerRef) -> () -> sceneRef.setRoot(new WorldMap(
                                                () -> sceneRef.setRoot(new Start(() -> sc.setRoot(new MainMenu()), () -> {})),
                                                nav
                                        ));

                                java.util.function.Function<javafx.scene.Scene, Application.Player> loadPlayer =
                                        (sceneRef) -> {
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
            StackPane.setAlignment(DeathScreen, Pos.CENTER);
        }

        if (!overlayLayer.getChildren().contains(DeathScreen)) {
            overlayLayer.getChildren().add(DeathScreen);
        }
        overlayLayer.toFront();
    }
}

package Application.GameScreens;

// IMPORTS
import Application.Player;
import Application.Mobs;
import Application.UI.UI;
import Application.Dialogue.DialogueManager;
import Application.AudioManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Represents the final Boss battle screen.
 * This location features the main boss mob, special music, and triggers
 * the victory screen upon completion.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Boss extends LocationScreen {

    // PLAYER MOVEMENT
    private Timeline moveTimeline;
    private Double targetTX; // desired translateX
    private Double targetTY; // desired translateY relative to bottom center anchor

    // MOB
    private Timeline proximityWatcher;

    // DEATH SCREEN OVERLAY
    private DeathScreen deathScreen;

    // DIALOGUE
    private DialogueManager dialogueManager;
    private Label dialogueLabel;

    /**
     * Constructs the Boss screen.
     * @param onBackToMap Action to run when returning to the world map.
     * @param onNavigateNext Action to run after this location (not used here).
     * @param player The player object.
     */
    public Boss(Runnable onBackToMap, Runnable onNavigateNext, Player player) {
        super(onBackToMap, onNavigateNext, player);
        buildBossContent();
    }

    // Provides the background image path for this location.
    @Override
    protected String getBackgroundImagePath() {
        return "src/Assets/Locations/Boss.png";
    }

    // Provides the name of this location.
    @Override
    protected String getLocationName() { return "Boss"; }

    // Provides the name of the next location.
    @Override
    protected String getNextLocationName() { return "World Map"; }

    /**
     * Builds all the content for the boss screen, including UI, player,
     * the mob, and event handlers for gameplay.
     */
    private void buildBossContent() {
        // MUSIC
        AudioManager.getInstance().stopMusic();
        AudioManager.getInstance().playMusic(AudioManager.MusicTrack.BOSS, true);

        // UI
        UI ui = new UI();
        ui.setLocation(getLocationName());
        ui.setMapButtonAction(onBackToMap);
        ui.setMouseTransparent(false);
        StackPane.setAlignment(ui, Pos.TOP_LEFT);
        ui.setMaxWidth(Double.MAX_VALUE);
        ui.setMaxHeight(Double.MAX_VALUE);
        overlayLayer.getChildren().add(ui);
        // Make sure UI is on top and clickable.
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

        // MOB
        final Mobs bossMob = new Mobs(Mobs.MobType.DEMON_BOSS);
        bossMob.setAttack(60); // Boss hits harder
        bossMob.getNode().setPreserveRatio(true);
        bossMob.getNode().setSmooth(true);
        // Make boss size responsive to screen width.
        bossMob.getNode().fitWidthProperty().bind(widthProperty().multiply(0.80));
        StackPane.setAlignment(bossMob.getNode(), Pos.BOTTOM_RIGHT);
        bossMob.getNode().setTranslateX(-140); // offset from right
        bossMob.getNode().setTranslateY(-120);  // adjust for larger size
        getContentLayer().getChildren().add(bossMob.getNode());

        // DIALOGUE
        dialogueManager = new DialogueManager("src/Assets/Dialogue/boss.txt");

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

        // HIDE DIALOGUE BY DEFAULT
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

        // MOB ATTACK LOGIC
        final int proximityThreshold = 140; // pixels, larger boss needs wider radius
        final int attackCooldownMs = 5000; // 5 seconds cooldown to avoid barrage
        proximityWatcher = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            if (player == null || player.isDead()) return;
            if (bossMob.isDead()) return;

            // Check the distance between the player and the boss.
            javafx.geometry.Bounds pb = player.getNode().getBoundsInParent();
            javafx.geometry.Bounds mb = bossMob.getNode().getBoundsInParent();
            double px = pb.getMinX() + pb.getWidth()/2.0;
            double mx = mb.getMinX() + mb.getWidth()/2.0;
            double dist = Math.abs(px - mx);

            Long lastAttack = (Long) bossMob.getNode().getProperties()
                    .getOrDefault("boss.lastAttack", 0L);
            long now = System.currentTimeMillis();
            boolean cooledDown = (now - lastAttack) >= attackCooldownMs;

            // If the player is close enough and the cooldown is over, the boss attacks.
            if (dist <= proximityThreshold && cooledDown) {
                bossMob.getNode().getProperties().put("boss.lastAttack", now);
                bossMob.attackPlayer(player);
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
                double desiredTX = player.getNode().getTranslateX() + (clickX - (centerX + player.getNode().getTranslateX()));

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
        addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (player == null) return;
            if (e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                int cost = 20;
                if (player.getStamina() >= cost && !player.isDead()) {
                    player.spendStamina(cost);
                    player.startAttack();

                    // Different classes have different attack ranges.
                    final int playerAttackRange =
                            (player.getClassType() == Application.Actor.ClassType.ARCHER ? 150 : 110);
                    int frames = (player.getClassType() == Application.Actor.ClassType.ARCHER ? 8 : 6);
                    javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(
                            javafx.util.Duration.millis(frames * 140));
                    // After the attack animation finishes, check if the boss was hit.
                    pt.setOnFinished(ev -> {
                        if (bossMob != null && !player.isDead() && !bossMob.isDead()) {
                            javafx.geometry.Bounds pb2 = player.getNode().getBoundsInParent();
                            javafx.geometry.Bounds mb2 = bossMob.getNode().getBoundsInParent();
                            double px2 = pb2.getMinX() + pb2.getWidth()/2.0;
                            double mx2 = mb2.getMinX() + mb2.getWidth()/2.0;
                            double distX = Math.abs(px2 - mx2);
                            // If the boss is in range, deal damage.
                            if (distX <= playerAttackRange) {
                                player.attackMob(bossMob);
                                if (bossMob.isDead()) {
                                    try { bossMob.startDeath(); } catch (Exception ex) { /* ignore */ }
                                    handleBossDefeat(bossMob);
                                } else {
                                    try { bossMob.startHurt(); } catch (Exception ex) { /* ignore */ }
                                    // play hit sound on successful non-lethal hit
                                    Application.AudioManager.getInstance().playSfx("HIT");
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
    }

    /**
     * Starts moving the player towards a target coordinate.
     */
    private void startMoveTowards(double tx, double ty) {
        this.targetTX = tx;
        this.targetTY = ty;
        if (moveTimeline != null) moveTimeline.stop();
        player.startWalk();
        moveTimeline = new Timeline(new KeyFrame(Duration.millis(16), e -> stepMove())); // ~60 FPS
        moveTimeline.setCycleCount(Timeline.INDEFINITE);
        moveTimeline.play();
    }

    /**
     * A single step in the player's movement, called by the moveTimeline.
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
     * Handles the player's death by stopping game interactions and showing the death screen.
     */
    private void handlePlayerDeath() {
        stopMove();
        if (proximityWatcher != null) {
            proximityWatcher.stop();
            proximityWatcher = null;
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
                    Application.AudioManager.getInstance().switchMusic(Application.AudioManager.MusicTrack.MAIN_MENU);
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
     * Handles the boss's defeat by playing its death animation and showing the victory screen.
     */
    private void handleBossDefeat(Mobs bossMob) {
        if (proximityWatcher != null) {
            proximityWatcher.stop();
            proximityWatcher = null;
        }

        try { bossMob.startDeath(); } catch (Exception ignored) {}

        markBossClearedInSettings();

        Application.Engine.GameState.getGlobal().markLocationCleared(getLocationName());
        onMobKilled(bossMob);
        Application.Engine.GameState.saveGlobal();

        // Wait for the boss death animation to finish before showing the victory screen.
        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(3500));
        pt.setOnFinished(ev -> {
            VictoryScreen victory = new VictoryScreen(() -> {
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

                                sc.setRoot(new WorldMap(
                                        () -> sc.setRoot(new Start(() -> sc.setRoot(new MainMenu()), () -> {})),
                                        nav
                                ));
                            }
                        ))
                    );
                    sc.setRoot(menu);
                } else {
                    Application.AudioManager.getInstance().switchMusic(Application.AudioManager.MusicTrack.MAIN_MENU);
                }
            });
            StackPane.setAlignment(victory, Pos.CENTER);
            overlayLayer.getChildren().add(victory);
            overlayLayer.toFront();
        });
        pt.play();
    }

    /**
     * A temporary method to mark the boss as cleared in the settings file.
     */
    private void markBossClearedInSettings() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("settings.json");
            if (!java.nio.file.Files.exists(path)) return;
            String content = java.nio.file.Files.readString(path);
            String updated = content;
            if (updated.contains("\"Boss\": false")) {
                updated = updated.replace("\"Boss\": false", "\"Boss\": true");
            } else if (updated.contains("\"Boss\":true") || updated.contains("\"Boss\": true")) {
                // already true
            } else if (updated.contains("\"locationsCleared\"")) {
                updated = updated.replaceFirst("(\\\"locationsCleared\\\"\\s*:\\s*\\{)", "$1\\n  \"Boss\": true,");
            }
            if (!updated.equals(content)) {
                java.nio.file.Files.writeString(path, updated);
            }
        } catch (Exception ignored) {}
    }
}

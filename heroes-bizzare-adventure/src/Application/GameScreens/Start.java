package Application.GameScreens;

// IMPORTS
import Application.UI.UI;
import Application.AudioManager;
import Application.AudioManager.MusicTrack;
import Application.Engine.GameState;
import Application.Player;
import Application.Actor.ClassType;
import Application.Dialogue.DialogueManager;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.scene.image.ImageView;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The starting screen of the game, the Hall of Heroes.
 * This is the central hub where the player begins and can interact with the Queen.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Start extends StackPane {
    // QUEEN NPC
    private ImageView queenView;
    private Timeline queenAnim;
    private List<Image> queenFrames;
    private int queenIndex = 0;

    // PLAYER
    private Player player; // player avatar based on chosen class

    // DIALOGUE
    private DialogueManager dialogueManager;
    private Label dialogueLabel;

    // PLAYER MOVEMENT
    private Timeline moveTimeline;
    private Double targetTX; // desired translateX
    private Double targetTY; // desired translateY relative to bottom center anchor

    /**
     * Constructs the Start screen.
     * @param onOpenMap Action to run when the map is opened.
     * @param onNavigateNext Action to run to navigate to the next location (not used here).
     */
    public Start(Runnable onOpenMap, Runnable onNavigateNext) {
        setBackground(buildCastleBackground());
        // MUSIC
        AudioManager.getInstance().switchMusic(MusicTrack.GAMEPLAY);

        // UI
        UI ui = new UI();
        ui.setLocation("Hall of Heroes");
        ui.setMapButtonAction(onOpenMap);
        ui.setMouseTransparent(false);
        StackPane.setAlignment(ui, Pos.TOP_LEFT);
        ui.setMaxWidth(Double.MAX_VALUE);
        ui.setMaxHeight(Double.MAX_VALUE);

        // NPC
        queenFrames = loadQueenFrames("src/Assets/NPC/Queen");

        // PLAYER
        try {
            GameState gs = GameState.load();
            player = new Player();
            gs.applyToPlayer(player);
            StackPane.setAlignment(player.getNode(), Pos.BOTTOM_CENTER);
            player.getNode().setTranslateY(-40);
        } catch (Exception ex) {
            System.out.println("[Start] Failed to init player from GameState: " + ex.getMessage());
        }

        if (!queenFrames.isEmpty()) {
            queenView = new ImageView(queenFrames.get(0));
            queenView.setPreserveRatio(true);
            queenView.setSmooth(true);
            queenView.setCache(true);
            // Size queen to be responsive to window width (~12% of width)
            queenView.fitWidthProperty().bind(widthProperty().multiply(0.12));
            // Position at throne area
            StackPane.setAlignment(queenView, Pos.CENTER);
            queenView.translateYProperty().bind(heightProperty().multiply(-0.28));
            queenView.setTranslateX(7);
            getChildren().add(queenView);

            if (player != null) {
                getChildren().add(player.getNode());
            }

            getChildren().add(ui);
            ui.toFront();
            if (player != null) ui.bindToPlayer(player);

            // NPC ANIMATION
            queenAnim = new Timeline(new KeyFrame(Duration.millis(300), e -> advanceQueenFrame()));
            queenAnim.setCycleCount(Timeline.INDEFINITE);
            queenAnim.play();
        } else {
            if (player != null) {
                getChildren().add(player.getNode());
            }
            getChildren().add(ui);
            ui.toFront();
            if (player != null) ui.bindToPlayer(player);
            System.out.println("[Start] UI added without queenView.");
        }

        // DIALOGUE
        dialogueManager = new DialogueManager("src/Assets/Dialogue/castle.txt");

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

        // KEYBOARD CONTROLS
        setFocusTraversable(true);
        javafx.application.Platform.runLater(this::requestFocus);
        addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            javafx.scene.Scene sc = getScene();
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                if (sc != null) {
                    try {
                        GameState gs = GameState.load();
                        gs.capturePlayer(player);
                        gs.setLocation("Start");
                        gs.save();
                    } catch (Exception ignore) {}
                    if (ui != null) ui.unbindPlayer();
                    sc.setRoot(new PauseMenu(() -> {
                        sc.setRoot(this);
                        if (ui != null) ui.bindToPlayer(player);
                        javafx.application.Platform.runLater(this::requestFocus);
                    }, null, null, player));
                }
                e.consume();
            } else if (player != null) {
                if (e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                    int cost = 20;
                    if (player.getStamina() >= cost && !player.isDead()) {
                        player.spendStamina(cost);
                        player.startAttack();
                        javafx.animation.PauseTransition pt =
                                new javafx.animation.PauseTransition(
                                        Duration.millis((player.getClassType() == ClassType.ARCHER ? 8 : 6) * 140 + 120));
                        pt.setOnFinished(ev -> {
                            if (!player.isDead()) player.showIdle();
                        });
                        pt.play();
                    }
                    e.consume();
                }
            }
        });

        // PLAYER MOVEMENT
        addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, me -> {
            if (player == null || player.isDead()) return;
            if (me.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                double clickX = me.getX();
                double clickY = me.getY();

                double centerX = getWidth() / 2.0;
                double desiredTX = player.getNode().getTranslateX()
                        + (clickX - (centerX + player.getNode().getTranslateX()));

                double bottomY = getHeight();
                double currentAbsY = bottomY + player.getNode().getTranslateY();
                double desiredTY = player.getNode().getTranslateY() + (clickY - currentAbsY);

                // Clamp: do not allow player to move above queen's feet
                if (queenView != null) {
                    double queenCenterY = getHeight() / 2.0 + queenView.getTranslateY();
                    double queenHeight = queenView.getBoundsInParent().getHeight();
                    double buffer = Math.max(45, queenHeight * 0.08);
                    double queenFeetY = queenCenterY + (queenHeight / 2.0) + buffer;
                    if (bottomY + desiredTY < queenFeetY) {
                        desiredTY = queenFeetY - bottomY;
                    }
                }

                startMoveTowards(desiredTX, desiredTY);
                me.consume();
            }
        });
    }

    /**
     * Default constructor.
     */
    public Start() {
        this(() -> {}, () -> {});
    }

    /**
     * Constructor with a back navigation action.
     * @param ignoredBack The action to run on back navigation.
     */
    public Start(Runnable ignoredBack) {
        this(() -> {}, () -> {});
    }

    private Background buildCastleBackground() {
        Image img = loadImage("/Assets/Castle.png", "src/Assets/Locations/Castle.png");
        BackgroundSize size = new BackgroundSize(10, 10, true, true, false, true); // cover
        BackgroundImage bgImg = new BackgroundImage(
                img,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                size
        );
        return new Background(bgImg);
    }

    /**
     * Advances the Queen's animation to the next frame.
     */
    private void advanceQueenFrame() {
        if (queenFrames == null || queenFrames.isEmpty() || queenView == null) return;
        queenIndex = (queenIndex + 1) % queenFrames.size();
        queenView.setImage(queenFrames.get(queenIndex));
    }

    /**
     * Loads all .png image frames for the Queen from a given directory.
     * @param baseDevPath The path to the directory containing the frames.
     * @return A list of Image objects.
     */
    private List<Image> loadQueenFrames(String baseDevPath) {
        List<File> files = new ArrayList<>();
        collectPngsRecursively(new File(baseDevPath), files);
        files.sort(new NaturalFileNameComparator());

        List<Image> frames = new ArrayList<>();
        for (File f : files) {
            try {
                frames.add(new Image(f.toURI().toString()));
            } catch (Exception ignored) { }
        }
        return frames;
    }

    /**
     * Recursively collects all .png files from a directory and its subdirectories.
     * @param dir The directory to search.
     * @param out The list to add found files to.
     */
    private void collectPngsRecursively(File dir, List<File> out) {
        if (dir == null || !dir.exists()) return;
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                collectPngsRecursively(f, out);
            } else if (f.getName().toLowerCase().endsWith(".png")) {
                out.add(f);
            }
        }
    }

    /**
     * A comparator for sorting file names in natural order (e.g., 1, 2, 10 instead of 1, 10, 2).
     */
    private static class NaturalFileNameComparator implements Comparator<File> {
        private final Pattern chunk = Pattern.compile("(\\d+)|(\\D+)");
        @Override public int compare(File a, File b) {
            String s1 = a.getName();
            String s2 = b.getName();
            Matcher m1 = chunk.matcher(s1);
            Matcher m2 = chunk.matcher(s2);
            while (m1.find() && m2.find()) {
                String c1 = m1.group();
                String c2 = m2.group();
                int result;
                if (isNumber(c1) && isNumber(c2)) {
                    result = Integer.compare(Integer.parseInt(c1), Integer.parseInt(c2));
                } else {
                    result = c1.compareToIgnoreCase(c2);
                }
                if (result != 0) return result;
            }
            return s1.length() - s2.length();
        }
        private boolean isNumber(String s) { return Character.isDigit(s.charAt(0)); }
    }

    /**
     * Loads an image from the classpath, falling back to a development file path.
     * @param classpath The classpath resource path.
     * @param devPath The development file system path.
     * @return The loaded Image object.
     */
    private Image loadImage(String classpath, String devPath) {
        Image viaCp = tryClasspath(classpath);
        if (viaCp != null) return viaCp;
        File f = new File(devPath);
        return new Image(f.toURI().toString());
    }

    /**
     * Tries to load an image from the classpath.
     * @param cpPath The classpath resource path.
     * @return The loaded Image object, or null if not found.
     */
    private Image tryClasspath(String cpPath) {
        URL url = getClass().getResource(cpPath);
        if (url != null) {
            return new Image(url.toExternalForm());
        }
        return null;
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
}

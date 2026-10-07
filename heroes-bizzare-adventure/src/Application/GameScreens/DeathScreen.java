package Application.GameScreens;

// IMPORTS
import java.io.File;
import java.net.URL;

import Application.AudioManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * A screen that appears when the player's health reaches zero.
 * It displays a "YOU DIED" message, plays a death animation, and provides
 * an option to return to the main menu.
 *
 * @author Rosaline Liu
 * @author Brandon Nguyen
 */
public class DeathScreen extends BorderPane {
    // ANIMATION
    private Timeline deathAnimation;
    private final ImageView preview = new ImageView();

    /**
     * Helper method to resolve the base path for animation frames, trying classpath first.
     * @param classpathDir The directory path within the classpath.
     * @param fileDir The fallback file system directory path.
     * @return The resolved base path as a string.
     */
    private String resolveBase(String classpathDir, String fileDir) {
        URL url = getClass().getResource(classpathDir);
        if (url != null) return url.toExternalForm();      // e.g. "file:/.../Death/"
        return new File(fileDir).toURI().toString();       // fallback "file:/.../src/Assets/PlayerClasses/Death/"
    }

    /**
     * Constructs the DeathScreen.
     * @param onBack The action to run when the "Return to Menu" button is clicked.
     */
    public DeathScreen(Runnable onBack) {
        // LAYOUT
        setPadding(new Insets(10));
        setBackground(new Background(
                new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)
        ));

        // AUDIO
        AudioManager.getInstance().stopMusic();
        AudioManager.getInstance().playSfx("GameOver");

        // TITLE
        Label title = new Label("YOU DIED...");
        title.setStyle(
                "-fx-text-fill: #a41010ff;" +
                "-fx-font-size: 90px;" +
                "-fx-font-weight: bold;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 12, 0.4, 0, 3);"
        );
        VBox titleBox = new VBox(title);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(80, 0, 10, 0));
        setTop(titleBox);   // <-- actually add the title to the BorderPane


        // CENTER CONTENT
        preview.setPreserveRatio(true);
        preview.setFitWidth(256);  // tweak to match sprite size

        // BUTTON
        Button menuBtn = new Button("Return to Menu");
        menuBtn.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 12;" +
                "-fx-background-color: #444444;"
        );

        menuBtn.setOnAction(e -> {
            AudioManager.getInstance().playSfx("CLICK");
            if (onBack != null) onBack.run();
        });

        VBox centerBox = new VBox(30, preview, menuBtn);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(20));
        setCenter(centerBox);   // <-- put animation & button in the center


        // ANIMATION SETUP
        String base = resolveBase("/Assets/PlayerClasses/Death/", "src/Assets/PlayerClasses/Death/");
        String[] frames = new String[] { "0.png", "1.png", "2.png", "3.png" };

        deathAnimation = new Timeline();

        for (int i = 0; i < frames.length; i++) {
            final String frameUrl = base + frames[i];
            deathAnimation.getKeyFrames().add(
                    new KeyFrame(Duration.millis(i * 120), e -> {
                        preview.setImage(new Image(frameUrl));
                    })
            );
        }

        deathAnimation.setCycleCount(1);
        deathAnimation.play();
    }
}

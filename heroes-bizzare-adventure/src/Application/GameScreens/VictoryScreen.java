package Application.GameScreens;

// IMPORTS
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import Application.AudioManager;
import Application.Engine.GameEngine;

/**
 * A screen displayed when the player successfully defeats the final boss.
 * It shows the final score, coins, and completion time.
 *
 * @author Rosaline Liu
 */
public class VictoryScreen extends BorderPane {
    // GAME STATS
    private final GameEngine instance;
    private final int finalScore;
    private final int finalCoin;
    private final String finalTimeSpent;

    /**
     * Constructs the VictoryScreen.
     * @param onBack The action to run when the "Return to Menu" button is clicked.
     */
    public VictoryScreen(Runnable onBack) {
        // INITIALIZE STATS
        this.instance = GameEngine.getInstance();
        this.finalScore = instance.getTotalScore();
        this.finalCoin = instance.getTotalCoins();
        this.finalTimeSpent = instance.getTotalTimeSpent();

        // BACKGROUND
        Image bgImage = new Image(getClass().getResource("/Assets/Locations/Castle.png").toExternalForm());
        BackgroundSize size = new BackgroundSize(100, 100, true, true, false, true);
        BackgroundImage bg = new BackgroundImage(
            bgImage,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER,
            size
        );
        setBackground(new Background(bg));

        // TITLE
        Label title = new Label("YOU WON!");
        title.setStyle(
            "-fx-text-fill: #0a4c2dff;" +
            "-fx-font-size: 90px;" +
            "-fx-font-weight: bold;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 12, 0.4, 0, 3);"
        );
        VBox titleBox = new VBox(title);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(80, 0, 10, 0));
        setTop(titleBox);

        // STATS DISPLAY
        Label score = new Label("Score: " + finalScore);
        score.setStyle("-fx-text-fill: green; -fx-font-size: 50px; -fx-font-weight: bold;");
        Label coins = new Label("Coins: " + finalCoin);
        coins.setStyle("-fx-text-fill: gold; -fx-font-size: 50px; -fx-font-weight: bold;");
        // Label time = new Label("Completion Time: " + finalTimeSpent);
        // time.setStyle("-fx-text-fill: white; -fx-font-size: 50px; -fx-font-weight: bold;");

        VBox statsBox = new VBox(20, score, coins);
        statsBox.setAlignment(Pos.CENTER);

        // RETURN BUTTON
        Button menuBtn = new Button("Return to Menu");
        menuBtn.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 12;" +
            "-fx-background-color: #e0d5d5ff;"
        );
        menuBtn.setOnAction(e -> {
            AudioManager.getInstance().playSfx("CLICK");
            if (onBack != null) onBack.run();
        });

        VBox centerBox = new VBox(40, statsBox, menuBtn);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(40, 0, 60, 0));
        setCenter(centerBox);
    }
}

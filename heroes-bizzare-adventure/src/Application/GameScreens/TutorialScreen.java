package Application.GameScreens;

// IMPORTS
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.util.List;

import Application.AudioManager;

/**
 * A screen that displays a series of tutorial images.
 * The user can navigate through the images using next and previous buttons.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class TutorialScreen extends BorderPane {
    private int currentIndex = 0;
    private final List<String> tutorialImages;
    private final ImageView imageView = new ImageView();

    /**
     * Constructs the TutorialScreen.
     * @param onBack The action to run when the tutorial is finished or exited.
     */
    public TutorialScreen(Runnable onBack) {
        // TUTORIAL IMAGES
        tutorialImages = List.of(
            "/Assets/TutorialScreens/GameScreen/GameScreen1.jpg",
            "/Assets/TutorialScreens/GameScreen/GameScreen2.jpg",
            "/Assets/TutorialScreens/GameScreen/GameScreen3.jpg",
            "/Assets/TutorialScreens/MapTutorial/TutorialScreen1.jpg",
            "/Assets/TutorialScreens/MapTutorial/TutorialScreen2.jpg",
            "/Assets/TutorialScreens/MapTutorial/TutorialScreen3.jpg",
            "/Assets/TutorialScreens/MapTutorial/TutorialScreen4.jpg",
            "/Assets/TutorialScreens/ShopTut/ShopScreen1.jpg",
            "/Assets/TutorialScreens/ShopTut/ShopScreen2.jpg"
        );
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(680);
        showImage(0);

        // LAYOUT
        setBackground(new Background(
        new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)
        ));
 
        // NEXT BUTTON
        Button nextBtn = new Button("▶");
        nextBtn.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 12;" +
            "-fx-background-color: #da0f0fff;"
        );

        nextBtn.setOnAction(e -> {
            AudioManager.getInstance().playSfx("CLICK");
            currentIndex++;
            if (currentIndex < tutorialImages.size()) {
                showImage(currentIndex);
            } else {
                if (onBack != null) onBack.run(); // end tutorial
            }
        });

        // PREVIOUS BUTTON
        Button prevBtn = new Button("◀");
        prevBtn.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 12;" +
            "-fx-background-color: #1dc549ff;"
        );

        prevBtn.setOnAction(e -> {
            AudioManager.getInstance().playSfx("CLICK");
            currentIndex--;
            if (currentIndex >= 0) {
                showImage(currentIndex);
            } else {
                if (onBack != null) onBack.run();
            }
        });

        // ARRANGE COMPONENTS
        StackPane imagePane = new StackPane(imageView);
        imagePane.setAlignment(Pos.CENTER);

        VBox leftBox = new VBox(prevBtn);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        VBox rightBox = new VBox(nextBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane layout = new BorderPane();
        layout.setCenter(imagePane);
        layout.setLeft(leftBox);
        layout.setRight(rightBox);

        setCenter(layout);
    }

    /**
     * Displays the tutorial image at the given index.
     */
    private void showImage(int index) {
        Image img = new Image(getClass().getResource(tutorialImages.get(index)).toExternalForm());
        imageView.setImage(img);
    }
       
}

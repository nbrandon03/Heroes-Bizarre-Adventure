package Application.GameScreens;

// IMPORTS
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.File;
import java.util.Map;

/**
 * The world map screen, allowing the player to select and travel to different locations.
 * It displays a map image with buttons overlaid at specific coordinates for each destination.
 *
 * @author Uday Prashant
 */
public class WorldMap extends StackPane {

    // TRAVEL BAR STATE
    private VBox travelBar;
    private Label travelLabel;
    private String selectedLocationName;
    private Runnable selectedTravelAction;

    /**
     * Constructs the WorldMap screen.
     * @param onBack The action to run when returning to the "Hall of Heroes".
     * @param locationNav A map of location names to the actions that navigate to them.
     */
    public WorldMap(Runnable onBack, Map<String, Runnable> locationNav) {
        // BACKGROUND
        Image mapImage = new Image(new File("src/Assets/map.png").toURI().toString());
        setBackground(new Background(new BackgroundImage(mapImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(100, 100, true, true, false, true))));

        // TITLE
        Label title = new Label("World Map");
        title.setFont(new Font("Arial", 32));
        title.setStyle("-fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 5, 0.5, 0, 1);");
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        title.setPadding(new Insets(20));

        // This pane will hold the location buttons, allowing us to position them freely
        // LOCATION BUTTONS
        Pane buttonPane = new Pane();

        if (locationNav.containsKey("Grasslands")) {
            Button grasslandsButton = createLocationButton("Grasslands", locationNav.get("Grasslands"));
            grasslandsButton.setLayoutX(440);            
            grasslandsButton.setLayoutY(535);  
            buttonPane.getChildren().add(grasslandsButton);
        }

        if (locationNav.containsKey("Forest")) {
            Button forestButton = createLocationButton("Forest", locationNav.get("Forest"));
            forestButton.setLayoutX(295);
            forestButton.setLayoutY(325);
            buttonPane.getChildren().add(forestButton);
        }

        if (locationNav.containsKey("Cave")) {
            Button caveButton = createLocationButton("Cave", locationNav.get("Cave"));
            caveButton.setLayoutX(145);
            caveButton.setLayoutY(240);
            buttonPane.getChildren().add(caveButton);
        }

        if (locationNav.containsKey("Mountain")) {
            Button mountainButton = createLocationButton("Mountain", locationNav.get("Mountain"));
            mountainButton.setLayoutX(135);
            mountainButton.setLayoutY(135);
            buttonPane.getChildren().add(mountainButton);
        }

        if (locationNav.containsKey("Village")) {
            Button villageButton = createLocationButton("Village", locationNav.get("Village"));
            villageButton.setLayoutX(415);
            villageButton.setLayoutY(105);
            buttonPane.getChildren().add(villageButton);
        }
        if (locationNav.containsKey("Boss")) {
            Button bossButton = createLocationButton("Boss", locationNav.get("Boss"));
            bossButton.setLayoutX(615);
            bossButton.setLayoutY(125);
            buttonPane.getChildren().add(bossButton);
        }

        if (locationNav.containsKey("Shop")) {
            Button shopButton = createLocationButton("Shop", locationNav.get("Shop"));
            shopButton.setLayoutX(135);            
            shopButton.setLayoutY(410);  
            buttonPane.getChildren().add(shopButton);
        }

        // Hall of Heroes Button
        Button hallOfHeroesButton = createLocationButton("Hall of Heroes", onBack);
        hallOfHeroesButton.setLayoutX(645);
        hallOfHeroesButton.setLayoutY(380);
        buttonPane.getChildren().add(hallOfHeroesButton);

        // TRAVEL BAR
        createTravelBar();

        getChildren().addAll(buttonPane, title, travelBar);
    }

    /**
     * Creates a styled button for a map location.
     * @param name The display name of the location.
     * @param action The action to run when the button is clicked.
     * @return A configured Button.
     */
    private Button createLocationButton(String name, Runnable action) {
        Button button = new Button(name);
        button.setOnAction(e -> {
            this.selectedLocationName = name;
            this.selectedTravelAction = action;
            updateTravelBar(true);
        });
        button.setStyle("-fx-background-color: rgba(40, 20, 0, 0.7); -fx-text-fill: white; -fx-border-color: #e8c67a; -fx-border-width: 2; -fx-background-radius: 5; -fx-border-radius: 5;");
        return button;
    }

    /**
     * Creates the UI for the bottom travel bar. It starts hidden.
     */
    private void createTravelBar() {
        travelLabel = new Label("Fast travelling to...");
        travelLabel.setFont(new Font("Arial", 16));
        travelLabel.setTextFill(Color.WHITE);

        Button goButton = new Button("Go");
        goButton.setStyle("-fx-background-color: #264e36; -fx-text-fill: white; -fx-font-weight: bold;");
        goButton.setOnAction(e -> {
            if (selectedTravelAction != null) {
                selectedTravelAction.run();
            }
        });

        HBox buttonBox = new HBox(15, goButton);
        buttonBox.setAlignment(Pos.CENTER);

        travelBar = new VBox(10, travelLabel, buttonBox);
        travelBar.setAlignment(Pos.CENTER);
        travelBar.setPadding(new Insets(15));
        travelBar.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); -fx-background-radius: 10 10 0 0;");
        travelBar.setPickOnBounds(false); // Allow mouse events to pass through the transparent parts of the VBox.
        travelBar.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        travelBar.setVisible(false); // Start hidden
        StackPane.setAlignment(travelBar, Pos.BOTTOM_CENTER);
    }

    /**
     * Updates the text and visibility of the travel bar.
     * @param show True to show the bar, false to hide it.
     */
    private void updateTravelBar(boolean show) {
        if (show) {
            travelLabel.setText("Fast travel to " + selectedLocationName + "?");
        }
        travelBar.setVisible(show);
    }
}

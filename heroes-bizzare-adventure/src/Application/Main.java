package Application;

// IMPORTS
import Application.GameScreens.MainMenu;
import Application.GameScreens.Shop;
import Application.GameScreens.Audio;
import Application.GameScreens.ModesMenu;
import Application.GameScreens.Start;
import Application.GameScreens.Settings;
import Application.GameScreens.WorldMap;
import Application.GameScreens.Cave;
import Application.GameScreens.Dungeon;
import Application.GameScreens.Grasslands;
import Application.GameScreens.Forest;
import Application.GameScreens.NewGame;
import Application.GameScreens.Mountain;
import Application.GameScreens.Village;
import Application.GameScreens.Boss;
import Application.Engine.GameState;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

import Application.Player;

/**
 * The main entry point for the JavaFX application.
 * This class is responsible for initializing the primary stage, setting up the scene,
 * and managing the navigation between different game screens.
 *
 * @author Uday Prashant
 * @author Brandon Nguyen
 * @author Nikolas Kiroff
 * @author Rosaline Liu
 */
public class Main extends Application {
    // The primary scene for the application.
    private Scene scene; // stored so lambdas don't reference before init
    @Override
    public void start(Stage primaryStage) {
        // Load the persisted game state and apply audio settings.
        GameState gs = GameState.load();
        AudioManager am = AudioManager.getInstance();
        gs.applyAudioTo(am);

        // Create main menu first, passing callback that uses instance method (scene will be assigned afterward)
        MainMenu mainMenu = new MainMenu(this::openSettings, this::startGame, this::openNewGame);
        scene = new Scene(mainMenu, 800, 600);
        primaryStage.setTitle("Heroes Bizarre Adventure - Main Menu");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Navigates to the settings screen.
     */
    private void openSettings() {
        // Build settings view; back returns to Main Menu. Audio opens Audio screen with back to Settings.
        Settings settingsView = new Settings(
            () -> scene.setRoot(new MainMenu(this::openSettings, this::startGame, this::openNewGame)),
            () -> scene.setRoot(new Audio(() -> openSettings())),
            () -> scene.setRoot(new ModesMenu(() -> openSettings()))
        );
        scene.setRoot(settingsView);
    }

    /**
     * Navigates to the starting location of the game (the "Hall of Heroes").
     */
    private void startGame() {
        // The "Start" screen is our Castle. We give it the instructions
        // for what to do when its "World Map" and "Next" buttons are clicked.
        scene.setRoot(new Start(this::openWorldMap, this::openGrasslands));
    }

    /**
     * Navigates to the new game/class selection screen.
     */
    private void openNewGame() {
        // Pass openWorldMap so Start's Map button works after New Game
        scene.setRoot(new NewGame(
            () -> scene.setRoot(new MainMenu(this::openSettings, this::startGame, this::openNewGame)),
            this::openWorldMap
        ));
    }

    /**
     * The main method, which launches the JavaFX application.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Navigates to the world map screen.
     */
    private void openWorldMap() {
        // The World Map needs to know how to open every location.
        // 1. Create a map of "location name" -> "action to open that location"
        Map<String, Runnable> locationNav = new HashMap<>();
        // --- Add future locations here as they are created ---
        locationNav.put("Grasslands", this::openGrasslands);
        locationNav.put("Forest", this::openForest);
        locationNav.put("Dungeon", this::openDungeon); // Now properly implemented
        locationNav.put("Cave", this::openCave);
        locationNav.put("Shop", this::openShop);
        locationNav.put("Mountain", this::openMountain);
        locationNav.put("Village", this::openVillage);
        locationNav.put("Boss", this::openBoss);

        // 2. Create the WorldMap, giving it the navigation map.
        //    The "Back" button on the map will go to the castle (startGame).
        WorldMap worldMap = new WorldMap(this::startGame, locationNav);
        scene.setRoot(worldMap);
    }

    /**
     * Navigates to the Grasslands location.
     */
    private void openGrasslands() {
        // This now creates and shows the new Grasslands screen.
        scene.setRoot(new Grasslands(this::openWorldMap, this::openForest, new Player()));
    }

    /**
     * Navigates to the Forest location.
     */
    private void openForest() {
        // This now creates and shows the new Forest screen.
        scene.setRoot(new Forest(this::openWorldMap, this::openDungeon, new Player()));
    }

    /**
     * Navigates to the Dungeon location.
     */
    private void openDungeon() {
        scene.setRoot(new Dungeon(this::openWorldMap, this::openCave, new Player()));
    }

    /**
     * Navigates to the Cave location.
     */
    private void openCave() {
        scene.setRoot(new Cave(this::openWorldMap, this::openMountain, new Player()));
    }

    /**
     * Navigates to the Mountain location.
     */
    private void openMountain() {
        scene.setRoot(new Mountain(this::openWorldMap, this::openVillage, new Player()));
    }

    /**
     * Navigates to the Village location.
     */
    private void openVillage() {
        // The "next" location from Village is Boss, which is not yet implemented.
        scene.setRoot(new Village(this::openWorldMap, () -> System.out.println("Boss not implemented"), new Player()));
    }

    /**
     * Navigates to the Boss location.
     */
    private void openBoss() {
        // The "next" location from Village is Boss, which is not yet implemented.
        scene.setRoot(new Boss(this::openWorldMap, () -> System.out.println("There is no location after this one"), new Player()));
    }

    /**
     * Navigates to the Shop location.
     */
    private void openShop() {
        scene.setRoot(new Shop(this::openWorldMap, new Player()));
    }
}

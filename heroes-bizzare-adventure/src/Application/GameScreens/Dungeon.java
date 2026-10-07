package Application.GameScreens;

import Application.Player;

/**
 * The Dungeon location screen. This is a hostile area between the
 * Forest and the Cave.
 */
public class Dungeon extends LocationScreen {

    public Dungeon(Runnable onBackToMap, Runnable onNavigateNext, Player player) {
        super(onBackToMap, onNavigateNext, player);
        buildDungeonContent();
    }

    @Override
    protected String getBackgroundImagePath() { return "src/Assets/Locations/Dungeon.png"; }

    @Override
    protected String getLocationName() { return "Dungeon"; }

    @Override
    protected String getNextLocationName() { return "Cave"; }

    private void buildDungeonContent() { /* TODO: Add dungeon-specific enemies or NPCs */ }
}
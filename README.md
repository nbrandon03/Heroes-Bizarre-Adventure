# Heroes Bizarre Adventure

A JavaFX desktop game developed in Java as a group software development project. The game features an interactive graphical interface, character selection, combat systems, equipment and weapons, enemies, multiple game screens, and game progression.

## Overview

Heroes Bizarre Adventure is a 2D desktop game built using **JavaFX** and **Java 17**. The project was developed as a collaborative software engineering project, with a focus on object-oriented programming, graphical user interface development, game systems, and application structure.

The game allows players to start a new game, select a character class, explore different locations, interact with enemies, manage equipment, and progress through gameplay.

## Features

* JavaFX graphical user interface
* Main menu and game navigation
* New game and character class selection
* Player character system
* Enemy and mob system
* Combat mechanics
* Weapons and equipment
* Inventory and item systems
* Health and stamina systems
* Multiple game screens and locations
* Shop system
* Pause menu
* Audio and music system
* Character and enemy animations
* Game state management
* Save/settings data using JSON
* Custom game artwork, sprites, audio, and other assets

## Technologies

* **Java 17**
* **JavaFX**
* **Maven**
* **JSON**
* Object-Oriented Programming

## Project Structure

```text
heroes-bizarre-adventure/
├── src/
│   ├── Application/
│   │   ├── Actor/
│   │   ├── Engine/
│   │   └── GameScreens/
│   └── Assets/
├── pom.xml
├── settings.json
└── README.md
```

### Main Components

* **Actor** — Player equipment, weapons, effects, and related game objects
* **Engine** — Game engine and game state management
* **GameScreens** — Main menu, gameplay locations, shop, pause menu, and other screens
* **Assets** — Game sprites, images, audio, and other resources

## Running the Project

### Requirements

* Java 17 or later
* Maven

### Run

From the project directory:

```bash
mvn javafx:run
```

## Development

The project uses Maven to manage dependencies and run the JavaFX application.

The application is organized into separate packages for game actors, engine functionality, and graphical game screens to help keep the project modular and maintainable.

## My Contributions

As a member of the development team, I contributed to the development of the game's graphical interface and gameplay presentation, including JavaFX UI components, game screens, HUD elements, and visual assets.

My work included:

* Developing JavaFX user interface components
* Working on game screen layouts and navigation
* Implementing and adjusting HUD elements
* Working with health, stamina, and weapon displays
* Integrating graphical assets into the game interface
* Working with JavaFX `ImageView` components and animations
* Contributing to the overall visual presentation and usability of the game

## Project Goals

The project provided practical experience with:

* Object-oriented software development
* Java and JavaFX
* GUI development
* Event-driven programming
* Game architecture
* Asset management
* Collaborative software development
* Maven project management
* Designing and organizing a larger Java application

## Screenshots

Screenshots of the game and its interface can be added here to demonstrate the main menu, gameplay, HUD, character selection, and other game systems.

## Credits

Developed as a collaborative university software development project.

This repository is maintained as a portfolio project to demonstrate Java, JavaFX, GUI development, and software engineering experience.

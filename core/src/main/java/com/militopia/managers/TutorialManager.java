package com.militopia.managers;

import com.militopia.utils.GameLogger;

public class TutorialManager {

    private static TutorialManager instance;
    private int currentStep = 0;
    private boolean active = false;

    public enum Step {
        WELCOME("Welcome to Militopia! Let's learn the basics."),
        CAMERA("Use WASD or drag the mouse to pan, and the scroll wheel to zoom."),
        SELECT_UNIT("Select your Scout unit by clicking on it."),
        MOVE_UNIT("Move the Scout to the highlighted tile."),
        SELECT_BASE("Select your Base to see city options."),
        SUMMON_UNIT("Summon a Recruit from the Base menu."),
        CAPTURE_TOWN("Select your Recruit and move it to the neutral Town to capture it."),
        BUILD_STRUCTURE("Select a tile within your territory and build a Windmill."),
        CUT_TREE("Move a unit onto a Tree tile and click 'Cut Tree' in the Info Panel."),
        HUNT_ANIMAL("Move a unit next to an Animal and click 'Hunt' in the Slide Menu."),
        ATTACK_ENEMY("Move your unit near the enemy and click on the enemy to attack."),
        SCAVENGE_RUINS("Move a unit onto a Ruins tile and click 'Scavenge'."),
        CHECK_STATS("Click on the enemy unit or base to see their stats."),
        DISBAND_UNIT("Select a unit and click 'Disband' in the Info Panel."),
        DEMOLISH_STRUCT("Select a structure and click 'Demolish' in the Info Panel."),
        END_TURN("Click 'End Turn' to finish your turn."),
        SAVE_EXIT("Open Settings and click 'Save & Exit' to complete the tutorial."),
        COMPLETED("Tutorial Completed! You're ready to play.");

        public final String instruction;
        Step(String instruction) {
            this.instruction = instruction;
        }
    }

    private TutorialManager() {}

    public static TutorialManager getInstance() {
        if (instance == null) {
            instance = new TutorialManager();
        }
        return instance;
    }

    public void startTutorial() {
        active = true;
        currentStep = 0;
        GameLogger.log(GameLogger.UI, "Tutorial Started");
    }

    public void endTutorial() {
        active = false;
        GameLogger.log(GameLogger.UI, "Tutorial Ended");
    }

    public boolean isActive() {
        return active;
    }

    public Step getCurrentStep() {
        return Step.values()[currentStep];
    }

    public void nextStep() {
        if (currentStep < Step.values().length - 1) {
            currentStep++;
            GameLogger.log(GameLogger.UI, "Tutorial Advanced to: " + getCurrentStep().name());
        }
    }

    public boolean isActionAllowed(String action) {
        if (!active) return true;
        
        // Basic logic: only allow actions relevant to the current step
        // This can be expanded as needed
        switch (getCurrentStep()) {
            case SELECT_UNIT: return action.equals("SELECT_UNIT");
            case MOVE_UNIT: return action.equals("MOVE_UNIT");
            case SELECT_BASE: return action.equals("SELECT_BASE");
            case SUMMON_UNIT: return action.equals("SUMMON_UNIT");
            // ... add more guards ...
            default: return true;
        }
    }
}

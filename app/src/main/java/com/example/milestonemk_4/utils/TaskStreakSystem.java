package com.example.milestonemk_4.utils;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.milestonemk_4.R;

/**
 * Utility class to manage the streak/progress system in projects
 * The frog mascot evolves as more tasks are completed
 */
public class TaskStreakSystem {

    // Constants for streak levels
    public static final int STAGE_ONE = 1;   // 0-49% completion
    public static final int STAGE_TWO = 2;   // 50-74% completion
    public static final int STAGE_THREE = 3; // 75-99% completion
    public static final int STAGE_FOUR = 4;  // 100% completion

    // Keep track of the last stage to detect evolution
    private static int lastRecordedStage = STAGE_ONE;

    /**
     * Updates the frog image based on task completion percentage
     *
     * @param frogImage ImageView to update
     * @param totalTasks Total number of tasks in the project
     * @param completedTasks Number of completed tasks
     * @return The current streak stage (1-4)
     */
    public static int updateFrogStage(ImageView frogImage, int totalTasks, int completedTasks) {
        if (frogImage == null || totalTasks <= 0) {
            return STAGE_ONE;
        }

        // Calculate completion percentage
        float completionPercentage = (float) completedTasks / totalTasks * 100;
        int currentStage;

        // Determine the current stage based on completion percentage
        if (completionPercentage >= 100) {
            currentStage = STAGE_FOUR;
            frogImage.setImageResource(R.drawable.frog_stage_4);
        } else if (completionPercentage >= 75) {
            currentStage = STAGE_THREE;
            frogImage.setImageResource(R.drawable.frog_stage_3);
        } else if (completionPercentage >= 50) {
            currentStage = STAGE_TWO;
            frogImage.setImageResource(R.drawable.frog_stage_2);
        } else {
            currentStage = STAGE_ONE;
            frogImage.setImageResource(R.drawable.frog_stage_1);
        }

        return currentStage;
    }

    /**
     * Updates the frog image and checks for evolution
     *
     * @param context Activity context
     * @param frogImage ImageView to update
     * @param totalTasks Total number of tasks in the project
     * @param completedTasks Number of completed tasks
     * @return The current streak stage (1-4)
     */
    public static int updateFrogStageWithEvolution(Context context, ImageView frogImage,
                                                   int totalTasks, int completedTasks) {
        int currentStage = updateFrogStage(frogImage, totalTasks, completedTasks);

        // Check for evolution (stage increase)
        if (context != null && currentStage > lastRecordedStage) {
            // Only show evolution dialog when actually evolving to a higher stage
            showEvolutionDialog(context, currentStage, completedTasks, totalTasks);
        }

        // Update last recorded stage
        lastRecordedStage = currentStage;

        return currentStage;
    }

    /**
     * Updates the frog image and displays completion percentage text
     *
     * @param frogImage ImageView to update
     * @param progressText TextView to show percentage (can be null)
     * @param totalTasks Total number of tasks in the project
     * @param completedTasks Number of completed tasks
     * @return The current streak stage (1-4)
     */
    public static int updateFrogStageWithText(ImageView frogImage, TextView progressText,
                                              int totalTasks, int completedTasks) {
        int stage = updateFrogStage(frogImage, totalTasks, completedTasks);

        // Update progress text if provided
        if (progressText != null && totalTasks > 0) {
            int completionPercentage = (int) ((float) completedTasks / totalTasks * 100);
            progressText.setText(completionPercentage + "%");
        }

        return stage;
    }

    /**
     * Gets a motivational message based on the current completion stage
     *
     * @param currentStage The current frog evolution stage (1-4)
     * @return A motivational message
     */
    public static String getStreakMessage(int currentStage) {
        switch (currentStage) {
            case STAGE_FOUR:
                return "Amazing! You've completed all tasks!";
            case STAGE_THREE:
                return "Almost there! Keep going!";
            case STAGE_TWO:
                return "Halfway there! Making great progress!";
            case STAGE_ONE:
            default:
                return "Let's start completing some tasks!";
        }
    }

    /**
     * Gets an evolution message based on the new stage
     *
     * @param newStage The stage the frog just evolved to
     * @return An evolution message
     */
    private static String getEvolutionMessage(int newStage) {
        switch (newStage) {
            case STAGE_FOUR:
                return "Congratulations! Your frog is fully grown! All tasks completed!";
            case STAGE_THREE:
                return "Your frog is growing fast! Keep up the good work!";
            case STAGE_TWO:
                return "Your frog has started growing! Halfway there!";
            default:
                return "Your frog is just a tadpole. Complete tasks to help it grow!";
        }
    }

    /**
     * Shows a dialog when the frog evolves to a new stage
     *
     * @param context The activity context
     * @param newStage The stage the frog just evolved to
     * @param completedTasks Number of completed tasks
     * @param totalTasks Total number of tasks
     */
    private static void showEvolutionDialog(Context context, int newStage, int completedTasks, int totalTasks) {
        if (context == null || !(context instanceof Activity)) {
            return;
        }

        Activity activity = (Activity) context;

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.frog_evolution_dialog, null);
        builder.setView(dialogView);

        // Get dialog elements
        ImageView evolutionFrogImage = dialogView.findViewById(R.id.evolution_frog_image);
        TextView evolutionTitle = dialogView.findViewById(R.id.evolution_title);
        TextView evolutionMessage = dialogView.findViewById(R.id.evolution_message);
        TextView evolutionPercentage = dialogView.findViewById(R.id.evolution_percentage);
        Button continueButton = dialogView.findViewById(R.id.evolution_continue_button);

        // Set frog image based on new stage
        int frogDrawableId;
        switch (newStage) {
            case STAGE_FOUR:
                frogDrawableId = R.drawable.frog_stage_4;
                break;
            case STAGE_THREE:
                frogDrawableId = R.drawable.frog_stage_3;
                break;
            case STAGE_TWO:
                frogDrawableId = R.drawable.frog_stage_2;
                break;
            default:
                frogDrawableId = R.drawable.frog_stage_1;
        }
        evolutionFrogImage.setImageResource(frogDrawableId);

        // Set completion percentage text
        int percentage = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;
        evolutionPercentage.setText(percentage + "%");

        // Set evolution message
        evolutionMessage.setText(getEvolutionMessage(newStage));

        // Create and show dialog
        AlertDialog dialog = builder.create();

        // Set up continue button to dismiss dialog
        continueButton.setOnClickListener(v -> dialog.dismiss());

        // Show dialog
        dialog.show();
    }
}
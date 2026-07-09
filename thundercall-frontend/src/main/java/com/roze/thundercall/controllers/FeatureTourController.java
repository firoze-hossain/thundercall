package com.roze.thundercall.controllers;

import com.roze.thundercall.models.OnboardingStep;
import com.roze.thundercall.services.WorkspaceService;
import com.roze.thundercall.utils.AlertUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class FeatureTourController implements Initializable {
    @FXML
    private StackPane tourContainer;
    @FXML
    private Label titleLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button skipButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label stepIndicator;
    private List<OnboardingStep> steps;
    private int currentStepIndex = 0;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadTourSteps();
        showStep(0);
    }

    private void loadTourSteps() {
        steps = WorkspaceService.getOnboardingSteps();
    }

    @FXML
    private void handleNext() {
        if (currentStepIndex < steps.size() - 1) {  // ← Fix: steps.size() - 1
            markStepComplete(steps.get(currentStepIndex).getId());
            showStep(currentStepIndex + 1);
        } else {
            completeTour();
        }
    }

    @FXML
    private void handlePrevious() {
        if (currentStepIndex > 0) {
            showStep(currentStepIndex - 1);
        }
    }

    @FXML
    private void handleSkip() {
        completeTour();
    }

    private void showStep(int index) {
        currentStepIndex = index;
        OnboardingStep step = steps.get(index);
        titleLabel.setText(step.getTitle());
        descriptionLabel.setText(step.getDescription());
        updateNavigation();
        updateProgress();
    }

    private void updateNavigation() {
        previousButton.setDisable(currentStepIndex == 0);
        nextButton.setText(currentStepIndex == steps.size() - 1 ? "Finish" : "Next");
    }

    private void updateProgress() {
        double progress = (double) (currentStepIndex + 1) / steps.size();
        progressBar.setProgress(progress);
        stepIndicator.setText((currentStepIndex + 1) + "/" + steps.size());
    }

    private void markStepComplete(String stepId) {
        WorkspaceService.markTutorialComplete(stepId);
    }

    private void completeTour() {
        markStepComplete("completed");
        if (mainController != null) {
            mainController.removeFeatureTour();
        }
        AlertUtils.showSuccess("Feature tour completed! You're ready to start using ThunderCall.");
    }

}

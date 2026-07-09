package com.roze.thundercall.dialogs;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.util.Optional;

public class WorkspaceSetupDialog {
    public Optional<String> showAndAwait() {
        Dialog<Pair<String, Boolean>> dialog = new Dialog<>();
        dialog.setTitle("Workspace Setup");
        dialog.setHeaderText("Configure your Workspace");
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));
        TextField workspaceNameField = new TextField();
        workspaceNameField.setPromptText("My Workspace");
        CheckBox sampleDataCheckbox = new CheckBox("Create sample requests");
        sampleDataCheckbox.setSelected(true);
        gridPane.add(new Label("Workspace Name: "), 0, 0);
        gridPane.add(workspaceNameField, 1, 0);
        gridPane.add(sampleDataCheckbox, 0, 1, 2, 1);

        dialog.getDialogPane().setContent(gridPane);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                String workspaceName = workspaceNameField.getText().trim();
                if (workspaceName.isEmpty()) {
                    workspaceName = "My Workspace";
                }
                return new Pair<>(workspaceName, sampleDataCheckbox.isSelected());
            }
            return null;
        });
        Optional<Pair<String, Boolean>> result = dialog.showAndWait();
        return result.map(Pair::getKey);
    }
}

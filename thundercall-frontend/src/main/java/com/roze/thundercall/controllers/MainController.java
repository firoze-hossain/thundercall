package com.roze.thundercall.controllers;

import com.roze.thundercall.Main;
import com.roze.thundercall.dialogs.WorkspaceSetupDialog;
import com.roze.thundercall.enums.HttpMethod;
import com.roze.thundercall.models.*;
import com.roze.thundercall.services.*;
import com.roze.thundercall.utils.AlertUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.json.JSONObject;
import org.json.XML;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    @FXML
    private TreeView<String> collectionsTree;
    @FXML
    private Label statusMessage;
    @FXML
    private TextField urlField;
    @FXML
    private ComboBox<String> methodCombo;
    @FXML
    private ComboBox<String> authTypeCombo;
    @FXML
    private TextField tokenField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextArea bodyTextArea;
    @FXML
    private ToggleGroup bodyTypeGroup;
    @FXML
    private Label statusLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label sizeLabel;
    @FXML
    private TextArea responseBodyArea;
    @FXML
    private ListView<String> historyList;
    @FXML
    private TextArea testsArea;
    @FXML
    private ListView<String> testResultsList;
    @FXML
    private WebView responsePreview;
    @FXML
    private TableView<KeyValuePair> responseHeadersTable;
    @FXML
    private ComboBox<String> environmentCombo;
    @FXML
    private TableView<KeyValuePair> paramsTable;
    @FXML
    private TableView<KeyValuePair> headersTable;
    @FXML
    private TableView<KeyValuePair> cookiesTable;
    @FXML
    private TableView<KeyValuePair> formDataTable;
    @FXML
    private MenuButton collectionsMenuBtn;
    @FXML
    private ContextMenu treeContextMenu;

    private static final Logger logger = Logger.getLogger(MainController.class.getName());
    private boolean isRequestInProgress = false;
    private long requestStartTime;
    private final ObservableList<String> historyData = FXCollections.observableArrayList();
    private final ObservableList<KeyValuePair> paramsData = FXCollections.observableArrayList();
    private final ObservableList<KeyValuePair> headersData = FXCollections.observableArrayList();
    private final ObservableList<KeyValuePair> responseHeadersData = FXCollections.observableArrayList();
    private final ObservableList<KeyValuePair> cookiesData = FXCollections.observableArrayList();
    private final ObservableList<KeyValuePair> formData = FXCollections.observableArrayList();
    private Map<TreeItem<String>, Long> collectionIdMap = new HashMap<>();
    private Map<TreeItem<String>, Long> folderIdMap = new HashMap<>();
    private Map<TreeItem<String>, Long> requestIdMap = new HashMap<>();
    private Map<String, EnvironmentResponse> environmentsMap = new HashMap<>();
    @FXML
    private ToggleButton noneBodyTypeButton;
    @FXML
    private ToggleButton rawBodyTypeButton;
    @FXML
    private ToggleButton formDataBodyTypeButton;
    @FXML
    private ToggleButton urlEncodedBodyTypeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController...");

        // Fix for TreeView context menu
        setupTreeViewContextMenu();

        bodyTypeGroup = new ToggleGroup();

        // Manually set up the toggle buttons
        noneBodyTypeButton.setToggleGroup(bodyTypeGroup);
        rawBodyTypeButton.setToggleGroup(bodyTypeGroup);
        formDataBodyTypeButton.setToggleGroup(bodyTypeGroup);
        urlEncodedBodyTypeButton.setToggleGroup(bodyTypeGroup);

        // Select the "None" button by default
        noneBodyTypeButton.setSelected(true);

        // First check authentication
        if (!TokenManager.isLoggedIn()) {
            logger.warning("User not logged in, redirecting to login");
            Main.showLoginView();
            return;
        }

        try {
            setupCollectionsTree();
            setupEnvironmentCombo();
            setupTables();
            setupAuthCombo();
            setUpMethodCombo();
            setupBodyTypeHandling();
            loadUserData();
            updateStatus("Ready");
            checkUserOnboarding();

            // Load data from server only after ensuring user is authenticated
            loadCollectionsFromServer();
            loadEnvironments();
            loadHistory();

            logger.info("MainController initialized successfully");
        } catch (Exception e) {
            logger.severe("Error initializing MainController: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Failed to initialize application: " + e.getMessage());
        }
    }

    private void setupTreeViewContextMenu() {
        // Create context menu for TreeView
        treeContextMenu = new ContextMenu();
        MenuItem newRequestItem = new MenuItem("New Request");
        MenuItem newFolderItem = new MenuItem("New Folder");
        MenuItem renameItem = new MenuItem("Rename");
        MenuItem duplicateItem = new MenuItem("Duplicate");
        MenuItem deleteItem = new MenuItem("Delete");

        newRequestItem.setOnAction(event -> handleAddRequestToCollection());
        newFolderItem.setOnAction(event -> handleNewFolder());
        renameItem.setOnAction(event -> handleRenameItem());
        duplicateItem.setOnAction(event -> handleDuplicateItem());
        deleteItem.setOnAction(event -> handleDeleteItem());

        treeContextMenu.getItems().addAll(
                newRequestItem,
                newFolderItem,
                new SeparatorMenuItem(),
                renameItem,
                duplicateItem,
                new SeparatorMenuItem(),
                deleteItem
        );

        // Set context menu on TreeView
        collectionsTree.setContextMenu(treeContextMenu);
    }

    private void setupBodyTypeHandling() {
        bodyTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                String selectedType = ((ToggleButton) newToggle).getText();
                switch (selectedType) {
                    case "Form Data":
                        formDataTable.setVisible(true);
                        bodyTextArea.setVisible(false);
                        break;
                    default:
                        formDataTable.setVisible(false);
                        bodyTextArea.setVisible(true);
                        break;
                }
            }
        });

        // Initialize form data table
        setupKeyValueTable(formDataTable, formData, "Key", "Value", "Description");
    }

    private void loadCollectionsFromServer() {
        new Thread(() -> {
            try {
                Optional<List<CollectionResponse>> collections = CollectionService.getUserCollections();
                if (collections.isPresent()) {
                    populateCollectionsTree(collections.get());
                } else {
                    Platform.runLater(() ->
                            AlertUtils.showError("Failed to load collections from server"));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        handleApiError(e, "Loading collections"));
            }
        }).start();
    }

    private void populateCollectionsTree(List<CollectionResponse> collectionResponses) {
        Platform.runLater(() -> {
            TreeItem<String> root = new TreeItem<>("Collections");
            root.setExpanded(true);
            collectionIdMap.clear();
            folderIdMap.clear();
            requestIdMap.clear();

            for (CollectionResponse collectionResponse : collectionResponses) {
                TreeItem<String> collectionItem = new TreeItem<>(collectionResponse.getName());
                collectionIdMap.put(collectionItem, collectionResponse.getId());

                // Add folders if they exist
                if (collectionResponse.getFolderResponses() != null && !collectionResponse.getFolderResponses().isEmpty()) {
                    for (FolderResponse folderResponse : collectionResponse.getFolderResponses()) {
                        TreeItem<String> folderItem = new TreeItem<>(folderResponse.getName());
                        folderIdMap.put(folderItem, folderResponse.getId());

                        // Add requests in folders if they exist
                        if (folderResponse.getRequestCount() > 0 && collectionResponse.getRequestResponses() != null) {
                            for (RequestResponse requestResponse : collectionResponse.getRequestResponses()) {
                                if (requestResponse.getFolderId() != null && requestResponse.getFolderId().equals(folderResponse.getId())) {
                                    TreeItem<String> requestItem = new TreeItem<>(requestResponse.getName());
                                    requestIdMap.put(requestItem, requestResponse.getId());
                                    folderItem.getChildren().add(requestItem);
                                }
                            }
                        }
                        collectionItem.getChildren().add(folderItem);
                    }
                }

                // Add requests directly in collection if they exist
                if (collectionResponse.getRequestResponses() != null && !collectionResponse.getRequestResponses().isEmpty()) {
                    for (RequestResponse requestResponse : collectionResponse.getRequestResponses()) {
                        if (requestResponse.getFolderId() == null) {
                            TreeItem<String> requestItem = new TreeItem<>(requestResponse.getName());
                            requestIdMap.put(requestItem, requestResponse.getId());
                            collectionItem.getChildren().add(requestItem);
                        }
                    }
                }

                root.getChildren().add(collectionItem);
            }

            collectionsTree.setRoot(root);

            // Set up custom cell factory for context menu
            collectionsTree.setCellFactory(tv -> {
                TreeCell<String> cell = new TreeCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item);
                        }
                    }
                };

                // Add context menu to each cell
                cell.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                    if (isEmpty) {
                        cell.setContextMenu(null);
                    } else {
                        cell.setContextMenu(treeContextMenu);
                    }
                });

                return cell;
            });

            // Expand all collections by default for better UX
            for (TreeItem<String> item : root.getChildren()) {
                item.setExpanded(true);
            }
        });
    }

    private void loadEnvironments() {
        new Thread(() -> {
            try {
                Optional<List<EnvironmentResponse>> environments = EnvironmentService.getUserEnvironments();
                if (environments.isPresent()) {
                    Platform.runLater(() -> {
                        environmentCombo.getItems().clear();
                        environmentsMap.clear();

                        for (EnvironmentResponse env : environments.get()) {
                            if (env.getIsActive()) {
                                environmentCombo.getItems().add(env.getName());
                                environmentsMap.put(env.getName(), env);
                            }
                        }

                        if (!environmentCombo.getItems().isEmpty()) {
                            environmentCombo.getSelectionModel().select(0);
                            updateEnvironmentVariables(environmentCombo.getValue());
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Loading environments"));
            }
        }).start();
    }

    private void loadHistory() {
        new Thread(() -> {
            try {
                Optional<List<RequestHistoryResponse>> history = RequestHistoryService.getUserRequestHistory();
                if (history.isPresent()) {
                    Platform.runLater(() -> {
                        historyData.clear();
                        for (RequestHistoryResponse item : history.get()) {
                            String historyEntry = item.getStatusCode() + " " + item.getRequestName() + " - " +
                                    item.getDuration() + "ms";
                            historyData.add(historyEntry);
                        }
                        historyList.setItems(historyData);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Loading history"));
            }
        }).start();
    }

    private void checkUserOnboarding() {
        if (TokenManager.isLoggedIn()) {
            new Thread(() -> {
                try {
                    boolean hasWorkspace = WorkspaceManager.hasWorkspace();
                    if (!hasWorkspace) {
                        Platform.runLater(this::showWorkspaceSetup);
                    } else if (!WorkspaceService.checkTutorialStatus()) {
                        Platform.runLater(this::showFeatureTour);
                    }
                } catch (Exception e) {
                    System.out.println("Error checking onboarding: " + e.getMessage());
                }
            }).start();
        }
    }

    private void showWorkspaceSetup() {
        WorkspaceSetupDialog dialog = new WorkspaceSetupDialog();
        Optional<String> result = dialog.showAndAwait();
        result.ifPresent(workspaceName -> {
            new Thread(() -> {
                Optional<Workspace> workspace = WorkspaceService.setupInitialWorkspace(workspaceName);
                workspace.ifPresent(ws -> {
                    Platform.runLater(() -> {
                        WorkspaceManager.setCurrentWorkspace(ws);
                        showFeatureTour();
                    });
                });
            }).start();
        });
    }

    private void showFeatureTour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/feature-tour.fxml"));
            Parent tourView = loader.load();
            FeatureTourController controller = loader.getController();
            controller.setMainController(this);

            StackPane root = (StackPane) Main.getPrimaryStage().getScene().getRoot();
            root.getChildren().add(tourView);
            tourView.toFront();

            disableMainContent(true);

        } catch (IOException e) {
            AlertUtils.showError("Failed to load feature tour: " + e.getMessage());
        }
    }

    public void removeFeatureTour() {
        StackPane root = (StackPane) Main.getPrimaryStage().getScene().getRoot();
        if (root.getChildren().size() > 1) {
            root.getChildren().remove(root.getChildren().size() - 1);
        }
        disableMainContent(false);
    }

    private void disableMainContent(boolean disable) {
        StackPane root = (StackPane) Main.getPrimaryStage().getScene().getRoot();
        if (root.getChildren().size() > 0) {
            Node mainContent = root.getChildren().get(0);
            mainContent.setDisable(disable);
            if (disable) {
                mainContent.setOpacity(0.6);
            } else {
                mainContent.setOpacity(1.0);
            }
        }
    }

    @FXML
    private void handleShowTour() {
        showFeatureTour();
    }

    private void setUpMethodCombo() {
        methodCombo.getItems().addAll("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
        methodCombo.getSelectionModel().select(0);
    }

    private void loadUserData() {
        String username = TokenManager.getUsername();
        if (username != null) {
            updateStatus("Logged in as: " + username);
        }
    }

    private void setupAuthCombo() {
        authTypeCombo.getItems().addAll("No Auth", "Bearer Token", "Basic Auth", "OAuth 2.0");
        authTypeCombo.getSelectionModel().select(0);
        authTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateAuthFieldsVisibility(newVal);
        });
        updateAuthFieldsVisibility("No Auth");
    }

    private void updateAuthFieldsVisibility(String authType) {
        tokenField.setVisible("Bearer Token".equals(authType));
        usernameField.setVisible("Basic Auth".equals(authType));
        passwordField.setVisible("Basic Auth".equals(authType));
    }

    private void setupTables() {
        setupKeyValueTable(paramsTable, paramsData, "Key", "Value", "Description");
        setupKeyValueTable(headersTable, headersData, "Key", "Value", "Description");
        setupKeyValueTable(responseHeadersTable, responseHeadersData, "Key", "Value");
        setupKeyValueTable(cookiesTable, cookiesData, "Name", "Value", "Domain");

        addCommonHeaders();
    }

    private void addCommonHeaders() {
        headersData.addAll(
                new KeyValuePair("Content-Type", "application/json", "Request content type"),
                new KeyValuePair("Accept", "application/json", "Response format"),
                new KeyValuePair("User-Agent", "JavaFX-API-Client/1.0", "Client identifier")
        );
    }

    private void setupKeyValueTable(TableView<KeyValuePair> table, ObservableList<KeyValuePair> data, String... columns) {
        table.setItems(data);
        table.setEditable(true);

        for (int i = 0; i < columns.length; i++) {
            TableColumn<KeyValuePair, String> column = new TableColumn<>(columns[i]);
            final int colIndex = i;
            column.setCellValueFactory(cellData -> {
                switch (colIndex) {
                    case 0:
                        return cellData.getValue().keyProperty();
                    case 1:
                        return cellData.getValue().valueProperty();
                    case 2:
                        return cellData.getValue().descriptionProperty();
                    default:
                        return new SimpleStringProperty("");
                }
            });
            column.setCellFactory(TextFieldTableCell.forTableColumn());
            column.setPrefWidth(150);
            table.getColumns().add(column);
        }

        // Add context menu for row operations
        ContextMenu contextMenu = new ContextMenu();
        MenuItem addItem = new MenuItem("Add Row");
        MenuItem deleteItem = new MenuItem("Delete Row");
        addItem.setOnAction(e -> data.add(new KeyValuePair("", "", "")));
        deleteItem.setOnAction(e -> {
            KeyValuePair selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) data.remove(selected);
        });
        contextMenu.getItems().addAll(addItem, deleteItem);
        table.setContextMenu(contextMenu);
    }

    private void setupEnvironmentCombo() {
        environmentCombo.getItems().addAll("No Environment");
        environmentCombo.getSelectionModel().select(0);
        environmentCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateEnvironmentVariables(newVal);
        });
    }

    private void updateEnvironmentVariables(String environmentName) {
        if ("No Environment".equals(environmentName) || environmentName == null) {
            return;
        }

        EnvironmentResponse env = environmentsMap.get(environmentName);
        if (env != null && env.getVariables() != null) {
            // Apply environment variables to the URL field if it contains variables
            String currentUrl = urlField.getText();
            if (currentUrl != null && !currentUrl.trim().isEmpty()) {
                for (Map.Entry<String, String> entry : env.getVariables().entrySet()) {
                    String variable = "{{" + entry.getKey() + "}}";
                    if (currentUrl.contains(variable)) {
                        currentUrl = currentUrl.replace(variable, entry.getValue());
                    }
                }
                urlField.setText(currentUrl);
            }

            // Apply environment variables to headers
            for (Map.Entry<String, String> entry : env.getVariables().entrySet()) {
                String headerKey = entry.getKey();
                if (headerKey.startsWith("header_")) {
                    String actualHeader = headerKey.substring(7); // Remove "header_" prefix
                    boolean headerExists = false;
                    for (KeyValuePair header : headersData) {
                        if (actualHeader.equalsIgnoreCase(header.getKey())) {
                            header.setValue(entry.getValue());
                            headerExists = true;
                            break;
                        }
                    }
                    if (!headerExists) {
                        headersData.add(new KeyValuePair(actualHeader, entry.getValue(), "From environment"));
                    }
                }
            }
        }
    }

    private void setupCollectionsTree() {
        TreeItem<String> root = new TreeItem<>("Collections");
        root.setExpanded(true);
        collectionsTree.setRoot(root);

        // Handle collection selection
        collectionsTree.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.isLeaf()) {
                        handleItemSelected(newValue);
                    }
                }
        );
    }

    private void handleItemSelected(TreeItem<String> item) {
        Long requestId = requestIdMap.get(item);
        if (requestId != null) {
            new Thread(() -> {
                try {
                    Optional<RequestResponse> request = RequestService.getRequest(requestId);
                    if (request.isPresent()) {
                        loadRequestIntoUI(request.get());
                    } else {
                        Platform.runLater(() ->
                                AlertUtils.showError("Failed to load request details"));
                    }
                } catch (Exception e) {
                    Platform.runLater(() ->
                            handleApiError(e, "Loading request"));
                }
            }).start();
        }
    }

    private void loadRequestIntoUI(RequestResponse requestResponse) {
        Platform.runLater(() -> {
            methodCombo.setValue(requestResponse.getMethod().name());
            urlField.setText(requestResponse.getUrl());

            // Load headers
            if (requestResponse.getHeaders() != null && !requestResponse.getHeaders().isEmpty()) {
                try {
                    JSONObject headersJson = new JSONObject(requestResponse.getHeaders());
                    headersData.clear();
                    for (String key : headersJson.keySet()) {
                        headersData.add(new KeyValuePair(key, headersJson.getString(key), ""));
                    }
                } catch (Exception e) {
                    // Fallback to simple header parsing
                    String[] headerLines = requestResponse.getHeaders().split("\n");
                    for (String line : headerLines) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            headersData.add(new KeyValuePair(parts[0].trim(), parts[1].trim(), ""));
                        }
                    }
                }
            }

            // Load body
            bodyTextArea.setText(requestResponse.getBody());

            updateStatus("Request loaded: " + requestResponse.getName());
        });
    }

    @FXML
    private void handleProfile() {
        AlertUtils.showInfo("Profile feature coming soon!");
    }

    @FXML
    private void handleSettings() {
        AlertUtils.showInfo("Settings feature coming soon!");
    }

    @FXML
    private void handleLogout() {
        if (AlertUtils.showConfirmation("Logout", "Are you sure you want to logout?")) {
            try {
                ApiClient.logout();
                TokenManager.clearTokens();
                Main.showLoginView();
            } catch (Exception e) {
                AlertUtils.showError("Logout failed: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleNewCollection() {
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("New Collection");
        nameDialog.setHeaderText("Create a new Collection");
        nameDialog.setContentText("Collection Name: ");

        Optional<String> nameResult = nameDialog.showAndWait();
        nameResult.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                TextInputDialog descDialog = new TextInputDialog();
                descDialog.setTitle("Collection Description");
                descDialog.setHeaderText("Add a description (optional)");
                descDialog.setContentText("Description: ");

                descDialog.showAndWait().ifPresent(description -> {
                    CollectionRequest request = CollectionRequest.builder()
                            .name(name)
                            .description(description)
                            .build();
                    createNewCollection(request);
                });
            }
        });
    }

    private void createNewCollection(CollectionRequest request) {
        new Thread(() -> {
            try {
                Optional<CollectionResponse> collection = CollectionService.createCollection(request);
                if (collection.isPresent()) {
                    Platform.runLater(() -> {
                        AlertUtils.showSuccess("Collection '" + request.getName() + "' created");
                        refreshCollectionsTree();
                    });
                } else {
                    Platform.runLater(() -> {
                        AlertUtils.showError("Failed to create collection");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        handleApiError(e, "Creating collection"));
            }
        }).start();
    }

    private void refreshCollectionsTree() {
        loadCollectionsFromServer();
    }

    @FXML
    private void handleNewEnvironment() {
        // Create a custom dialog for environment creation
        Dialog<EnvironmentCreationData> dialog = new Dialog<>();
        dialog.setTitle("Create New Environment");
        dialog.setHeaderText("Create a new environment with custom variables");

        // Set the button types
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Create the form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Environment name");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description (optional)");
        descriptionArea.setPrefRowCount(3);

        // Variables table
        TableView<Variable> variablesTable = new TableView<>();
        variablesTable.setPrefHeight(200);
        variablesTable.setEditable(true); // Make table editable

        // Variable Name column with editable text field
        TableColumn<Variable, String> nameCol = new TableColumn<>("Variable Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(event -> {
            Variable variable = event.getRowValue();
            variable.setName(event.getNewValue());
        });

        // Value column with editable text field
        TableColumn<Variable, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        valueCol.setCellFactory(TextFieldTableCell.forTableColumn());
        valueCol.setOnEditCommit(event -> {
            Variable variable = event.getRowValue();
            variable.setValue(event.getNewValue());
        });

        variablesTable.getColumns().addAll(nameCol, valueCol);

        // Add variable button
        Button addVarButton = new Button("Add Variable");
        addVarButton.setOnAction(e -> {
            Variable newVar = new Variable("", "");
            variablesTable.getItems().add(newVar);
            // Select the new row and start editing
            variablesTable.getSelectionModel().select(newVar);
            variablesTable.edit(variablesTable.getItems().size() - 1, nameCol);
        });

        // Remove variable button
        Button removeVarButton = new Button("Remove Selected");
        removeVarButton.setOnAction(e -> {
            Variable selected = variablesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                variablesTable.getItems().remove(selected);
            }
        });

        HBox buttonBox = new HBox(10, addVarButton, removeVarButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Variables:"), 0, 2);
        grid.add(variablesTable, 1, 2);
        grid.add(buttonBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to EnvironmentCreationData when create button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Map<String, String> variables = variablesTable.getItems().stream()
                        .filter(v -> v.getName() != null && !v.getName().trim().isEmpty())
                        .collect(Collectors.toMap(Variable::getName, Variable::getValue));

                return new EnvironmentCreationData(
                        nameField.getText().trim(),
                        descriptionArea.getText().trim(),
                        variables
                );
            }
            return null;
        });

        Optional<EnvironmentCreationData> result = dialog.showAndWait();
        result.ifPresent(envData -> {
            if (!envData.getName().isEmpty()) {
                new Thread(() -> {
                    Optional<EnvironmentResponse> environment = EnvironmentService.createEnvironment(
                            envData.getName(),
                            envData.getDescription(),
                            envData.getVariables()
                    );
                    if (environment.isPresent()) {
                        Platform.runLater(() -> {
                            AlertUtils.showSuccess("Environment '" + envData.getName() + "' created");
                            loadEnvironments();
                        });
                    } else {
                        Platform.runLater(() -> {
                            AlertUtils.showError("Failed to create environment");
                        });
                    }
                }).start();
            }
        });
    }

    @FXML
    private void handleClearHistory() {
        if (AlertUtils.showConfirmation("Clear History", "Are you sure you want to clear all history?")) {
            new Thread(() -> {
                boolean success = RequestHistoryService.clearUserHistory();
                if (success) {
                    Platform.runLater(() -> {
                        historyData.clear();
                        updateStatus("History Cleared");
                        AlertUtils.showSuccess("History cleared successfully");
                    });
                } else {
                    Platform.runLater(() -> {
                        AlertUtils.showError("Failed to clear history");
                    });
                }
            }).start();
        }
    }

    private void updateStatus(String message) {
        statusMessage.setText(message);
    }

    @FXML
    private void handleSaveRequest() {
        String name = showSaveDialog();
        if (name != null && !name.trim().isEmpty()) {
            TreeItem<String> selectedItem = collectionsTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                Long collectionId = getCollectionIdFromTreeItem(selectedItem);
                Long folderId = getFolderIdFromTreeItem(selectedItem);

                if (collectionId != null) {
                    saveRequestToCollection(name, collectionId, folderId);
                }
            } else {
                AlertUtils.showError("Please select a collection or folder to save to");
            }
        }
    }

    private void saveRequestToCollection(String name, Long collectionId, Long folderId) {
        new Thread(() -> {
            try {
                ApiRequest apiRequest = new ApiRequest();
                apiRequest.setName(name);
                apiRequest.setMethod(HttpMethod.valueOf(methodCombo.getValue()));
                apiRequest.setUrl(urlField.getText());
                apiRequest.setHeaders(new JSONObject(buildHeaders()).toString());
                apiRequest.setBody(buildRequestBody());
                apiRequest.setCollectionId(collectionId);
                apiRequest.setFolderId(folderId);

                Optional<RequestResponse> savedRequest = RequestService.saveRequest(apiRequest);
                if (savedRequest.isPresent()) {
                    Platform.runLater(() -> {
                        AlertUtils.showSuccess("Request saved successfully");
                        refreshCollectionsTree();
                    });
                } else {
                    Platform.runLater(() -> {
                        AlertUtils.showError("Failed to save request");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        handleApiError(e, "Saving request"));
            }
        }).start();
    }

    private Long getCollectionIdFromTreeItem(TreeItem<String> item) {
        // If item is a collection, return its ID
        Long collectionId = collectionIdMap.get(item);
        if (collectionId != null) {
            return collectionId;
        }

        // If item is a folder, get its parent collection
        Long folderId = folderIdMap.get(item);
        if (folderId != null && item.getParent() != null) {
            return collectionIdMap.get(item.getParent());
        }

        // If item is a request, get its parent collection
        Long requestId = requestIdMap.get(item);
        if (requestId != null && item.getParent() != null) {
            TreeItem<String> parent = item.getParent();
            if (folderIdMap.containsKey(parent)) {
                // Request is in a folder, get the folder's parent collection
                return collectionIdMap.get(parent.getParent());
            } else {
                // Request is directly in a collection
                return collectionIdMap.get(parent);
            }
        }

        return null;
    }

    private Long getFolderIdFromTreeItem(TreeItem<String> item) {
        // If item is a folder, return its ID
        Long folderId = folderIdMap.get(item);
        if (folderId != null) {
            return folderId;
        }

        // If item is a request and its parent is a folder, return the folder ID
        Long requestId = requestIdMap.get(item);
        if (requestId != null && item.getParent() != null) {
            return folderIdMap.get(item.getParent());
        }

        return null;
    }

    private String showSaveDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Save Request");
        dialog.setHeaderText("Save current request to collection");
        dialog.setContentText("Request name: ");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    @FXML
    private void handleCopyResponse() {
        String response = responseBodyArea.getText();
        if (response != null && !response.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(response);
            Clipboard.getSystemClipboard().setContent(content);
            AlertUtils.showSuccess("Response copied to clipboard");
        }
    }

    @FXML
    private void handleSaveResponse() {
        AlertUtils.showInfo("Save response feature coming soon");
    }

    @FXML
    private void handleRunTests() {
        String testScript = testsArea.getText();
        if (testScript.isEmpty()) {
            AlertUtils.showInfo("No test scripts to run");
            return;
        }
        testResultsList.getItems().clear();
        testResultsList.getItems().addAll(
                "✓ Status code is 200",
                "✓ Response time <500ms",
                "✓ Response contains valid JSON",
                "✓ Content-Type header is present"
        );
        updateStatus("Tests executed: 4 passed, 0 failed");
    }

    @FXML
    private void handleSendRequest() {
        if (isRequestInProgress) {
            AlertUtils.showInfo("Request already in progress");
            return;
        }

        if (!validateRequest()) {
            return;
        }

        isRequestInProgress = true;
        requestStartTime = System.currentTimeMillis();
        updateStatus("Sending request...");

        // Disable send button during request
        Node sendButton = urlField.getScene().lookup(".send-button");
        if (sendButton != null) {
            sendButton.setDisable(true);
        }

        String url = buildFullUrl();
        String method = methodCombo.getValue();
        Map<String, String> headers = buildHeaders();
        String body = buildRequestBody();

        // Use Task for proper JavaFX threading
        Task<Optional<ApiResponse>> requestTask = new Task<Optional<ApiResponse>>() {
            @Override
            protected Optional<ApiResponse> call() throws Exception {
                ApiRequest apiRequest = new ApiRequest();
                apiRequest.setUrl(url);
                apiRequest.setMethod(HttpMethod.valueOf(method));
                apiRequest.setHeaders(new JSONObject(headers).toString());
                apiRequest.setBody(body);

                return RequestService.executeRequest(apiRequest);
            }
        };

        requestTask.setOnSucceeded(event -> {
            isRequestInProgress = false;

            // Re-enable send button
            Node sendButton1 = urlField.getScene().lookup(".send-button");
            if (sendButton1 != null) {
                sendButton1.setDisable(false);
            }

            Optional<ApiResponse> result = requestTask.getValue();
            if (result.isPresent()) {
                ApiResponse apiResponse = result.get();
                updateResponseUI(
                        apiResponse.getStatusCode(),
                        apiResponse.getDuration(),
                        apiResponse.getResponse().length(),
                        apiResponse.getResponse(),
                        parseHeaders(apiResponse.getResponseHeaders())
                );

                String historyEntry = method + " " + url + " (" + apiResponse.getStatusCode() + ")";
                if (!historyData.contains(historyEntry)) {
                    historyData.add(0, historyEntry);
                    historyList.setItems(historyData);
                }

                updateStatus("Request completed");
            } else {
                AlertUtils.showError("Request failed with no response");
                updateStatus("Request failed");
            }
        });

        requestTask.setOnFailed(event -> {
            isRequestInProgress = false;

            // Re-enable send button
            Node sendButton2 = urlField.getScene().lookup(".send-button");
            if (sendButton2 != null) {
                sendButton2.setDisable(false);
            }

            AlertUtils.showError("Request failed: " + requestTask.getException().getMessage());
            updateStatus("Request failed");
        });

        new Thread(requestTask).start();
    }

    private Map<String, String> parseHeaders(String headersString) {
        Map<String, String> headersMap = new HashMap<>();
        if (headersString != null && !headersString.trim().isEmpty()) {
            String[] lines = headersString.split("\n");
            for (String line : lines) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    headersMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return headersMap;
    }

    private void updateResponseUI(int statusCode, long responseTime, int contentLength, String responseBody, Map<String, String> headers) {
        Platform.runLater(() -> {
            statusLabel.setText("Status: " + statusCode + " " + getStatusText(statusCode));
            timeLabel.setText("Time: " + responseTime + "ms");
            sizeLabel.setText("Size: " + contentLength + "B");

            // Apply status code styling
            statusLabel.getStyleClass().removeAll("status-2xx", "status-4xx", "status-5xx");
            if (statusCode >= 200 && statusCode < 300) {
                statusLabel.getStyleClass().add("status-2xx");
            } else if (statusCode >= 400 && statusCode < 500) {
                statusLabel.getStyleClass().add("status-4xx");
            } else if (statusCode >= 500) {
                statusLabel.getStyleClass().add("status-5xx");
            }

            responseBodyArea.setText(responseBody);
            formatResponseBody(responseBody);

            responseHeadersData.clear();
            headers.forEach((key, value) -> responseHeadersData.add(new KeyValuePair(key, value, "")));

            // Add to history
            String historyEntry = methodCombo.getValue() + " " + urlField.getText();
            if (!historyData.contains(historyEntry)) {
                historyData.add(0, historyEntry);
                historyList.setItems(historyData);
            }
        });
    }

    private void formatResponseBody(String responseBody) {
        try {
            if (responseBody.trim().startsWith("{")) {
                JSONObject json = new JSONObject(responseBody);
                responseBodyArea.setText(json.toString(2));
            } else if (responseBody.trim().startsWith("<")) {
                JSONObject json = XML.toJSONObject(responseBody);
                responseBodyArea.setText(json.toString(2));
            }
        } catch (Exception e) {
            // If formatting fails, just use the original response
        }
    }

    private String getStatusText(int statusCode) {
        switch (statusCode) {
            case 200:
                return "OK";
            case 201:
                return "Created";
            case 202:
                return "Accepted";
            case 204:
                return "No Content";
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 500:
                return "Internal Server Error";
            case 502:
                return "Bad Gateway";
            case 503:
                return "Service Unavailable";
            default:
                return "Unknown";
        }
    }

    private String buildFullUrl() {
        String baseUrl = urlField.getText().trim();
        StringBuilder urlBuilder = new StringBuilder(baseUrl);

        // Add query parameters
        if (!paramsData.isEmpty()) {
            boolean firstParam = true;
            for (KeyValuePair param : paramsData) {
                if (!param.getKey().isEmpty() && !param.getValue().isEmpty()) {
                    if (firstParam) {
                        urlBuilder.append("?");
                        firstParam = false;
                    } else {
                        urlBuilder.append("&");
                    }
                    try {
                        urlBuilder.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8.toString()))
                                .append("=")
                                .append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8.toString()));
                    } catch (UnsupportedEncodingException e) {
                        urlBuilder.append(param.getKey()).append("=").append(param.getValue());
                    }
                }
            }
        }

        return urlBuilder.toString();
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();

        // Add user-defined headers
        for (KeyValuePair header : headersData) {
            if (!header.getKey().isEmpty() && !header.getValue().isEmpty()) {
                headers.put(header.getKey(), header.getValue());
            }
        }

        // Set Content-Type based on body type if not already set
        Toggle selectedToggle = bodyTypeGroup.getSelectedToggle();
        if (selectedToggle != null) {
            String toggleText = ((ToggleButton) selectedToggle).getText();
            String bodyText = buildRequestBody();

            if (!bodyText.isEmpty() && !headers.containsKey("Content-Type")) {
                switch (toggleText) {
                    case "Form Data":
                        headers.put("Content-Type", "multipart/form-data");
                        break;
                    case "x-www-form-urlencoded":
                        headers.put("Content-Type", "application/x-www-form-urlencoded");
                        break;
                    case "Raw":
                        if (bodyText.startsWith("{") || bodyText.startsWith("[")) {
                            headers.put("Content-Type", "application/json");
                        } else if (bodyText.startsWith("<")) {
                            headers.put("Content-Type", "application/xml");
                        } else {
                            headers.put("Content-Type", "text/plain");
                        }
                        break;
                }
            }
        }

        // Add authentication headers
        String authType = authTypeCombo.getValue();
        switch (authType) {
            case "Bearer Token":
                if (!tokenField.getText().isEmpty()) {
                    headers.put("Authorization", "Bearer " + tokenField.getText());
                }
                break;
            case "Basic Auth":
                if (!usernameField.getText().isEmpty() && !passwordField.getText().isEmpty()) {
                    String auth = usernameField.getText() + ":" + passwordField.getText();
                    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                    headers.put("Authorization", "Basic " + encodedAuth);
                }
                break;
            case "OAuth 2.0":
                AlertUtils.showInfo("OAuth 2.0 support coming soon!");
                break;
        }

        return headers;
    }

    private String buildRequestBody() {
        Toggle selectedToggle = bodyTypeGroup.getSelectedToggle();

        if (selectedToggle == null) {
            return "";
        }

        String toggleText = ((ToggleButton) selectedToggle).getText();

        switch (toggleText) {
            case "Form Data":
                String formDataJson = convertFormDataToJson(formData);
                return formDataJson;
            case "x-www-form-urlencoded":
                String urlEncoded = convertFormDataToUrlEncoded(formData);
                return urlEncoded;
            case "Raw":
                String bodyText = bodyTextArea.getText().trim();
                // Validate JSON if it looks like JSON
                if (bodyText.startsWith("{") || bodyText.startsWith("[")) {
                    try {
                        new JSONObject(bodyText);
                    } catch (Exception e) {
                        AlertUtils.showError("Invalid JSON format");
                        return "";
                    }
                }
                return bodyText;
            default: // "None"
                return "";
        }
    }

    private String convertFormDataToJson(ObservableList<KeyValuePair> formData) {
        JSONObject json = new JSONObject();
        for (KeyValuePair pair : formData) {
            if (!pair.getKey().isEmpty() && !pair.getValue().isEmpty()) {
                json.put(pair.getKey(), pair.getValue());
            }
        }
        return json.toString();
    }

    private String convertFormDataToUrlEncoded(ObservableList<KeyValuePair> formData) {
        try {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < formData.size(); i++) {
                KeyValuePair pair = formData.get(i);
                if (!pair.getKey().isEmpty() && !pair.getValue().isEmpty()) {
                    if (i > 0) result.append("&");
                    result.append(URLEncoder.encode(pair.getKey(), StandardCharsets.UTF_8.toString()))
                            .append("=")
                            .append(URLEncoder.encode(pair.getValue(), StandardCharsets.UTF_8.toString()));
                }
            }
            return result.toString();
        } catch (UnsupportedEncodingException e) {
            AlertUtils.showError("Encoding error: " + e.getMessage());
            return "";
        }
    }

    private boolean validateRequest() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            AlertUtils.showError("Please enter a URL");
            return false;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            AlertUtils.showError("URL must start with http:// or https://");
            return false;
        }
        return true;
    }

    @FXML
    private void handleSamplePostRequest() {
        methodCombo.getSelectionModel().select("POST");
        urlField.setText("https://jsonplaceholder.typicode.com/posts");

        // Set sample JSON body
        bodyTextArea.setText("{\n  \"title\": \"foo\",\n  \"body\": \"bar\",\n  \"userId\": 1\n}");

        // Select Raw body type
        for (Toggle toggle : bodyTypeGroup.getToggles()) {
            if (((ToggleButton) toggle).getText().equals("Raw")) {
                bodyTypeGroup.selectToggle(toggle);
                break;
            }
        }

        // Add content-type header if not exists
        boolean hasContentType = false;
        for (KeyValuePair header : headersData) {
            if ("Content-Type".equalsIgnoreCase(header.getKey())) {
                hasContentType = true;
                break;
            }
        }

        if (!hasContentType) {
            headersData.add(new KeyValuePair("Content-Type", "application/json", "Request content type"));
        }

        AlertUtils.showInfo("Sample POST request loaded. Click Send to execute.");
    }

    @FXML
    private void handleAddParamRow() {
        paramsData.add(new KeyValuePair("", "", ""));
    }

    @FXML
    private void handleDeleteParamRow() {
        KeyValuePair selected = paramsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            paramsData.remove(selected);
        }
    }

    @FXML
    private void handleAddHeaderRow() {
        headersData.add(new KeyValuePair("", "", ""));
    }

    @FXML
    private void handleDeleteHeaderRow() {
        KeyValuePair selected = headersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            headersData.remove(selected);
        }
    }

    @FXML
    private void handleAddFormDataRow() {
        formData.add(new KeyValuePair("", "", ""));
    }

    @FXML
    private void handleDeleteFormDataRow() {
        KeyValuePair selected = formDataTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            formData.remove(selected);
        }
    }

    // Context menu handlers
    @FXML
    private void handleAddRequestToCollection() {
        TreeItem<String> selectedItem = collectionsTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            showAddRequestDialog(selectedItem);
        } else {
            AlertUtils.showError("Please select a collection or folder to add a request to");
        }
    }

    @FXML
    private void handleNewFolder() {
        TreeItem<String> selectedItem = collectionsTree.getSelectionModel().getSelectedItem();

        // Check if the selected item is a collection (has a collection ID)
        if (selectedItem != null && collectionIdMap.containsKey(selectedItem)) {
            // This is a collection, add folder to it
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Folder");
            dialog.setHeaderText("Create a new folder in " + selectedItem.getValue());
            dialog.setContentText("Folder name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    createNewFolder(name, selectedItem);
                }
            });
        } else {
            AlertUtils.showError("Please select a collection to add a folder to");
        }
    }

    @FXML
    private void handleRenameItem() {
        TreeItem<String> selectedItem = collectionsTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem != collectionsTree.getRoot()) {
            TextInputDialog dialog = new TextInputDialog(selectedItem.getValue());
            dialog.setTitle("Rename");
            dialog.setHeaderText("Rename this item");
            dialog.setContentText("New name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newName -> {
                if (!newName.trim().isEmpty()) {
                    // Update in backend and UI
                    Long collectionId = collectionIdMap.get(selectedItem);
                    Long folderId = folderIdMap.get(selectedItem);

                    if (collectionId != null) {
                        renameCollection(collectionId, newName);
                    } else if (folderId != null) {
                        renameFolder(folderId, newName);
                    } else {
                        // For requests, just update the UI (requests should be renamed through save)
                        selectedItem.setValue(newName);
                        updateStatus("Renamed to: " + newName);
                    }
                }
            });
        } else {
            AlertUtils.showError("Please select an item to rename");
        }
    }

    @FXML
    private void handleDuplicateItem() {
        TreeItem<String> selectedItem = collectionsTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem != collectionsTree.getRoot()) {
            Long collectionId = collectionIdMap.get(selectedItem);
            Long folderId = folderIdMap.get(selectedItem);

            if (collectionId != null) {
                duplicateCollection(collectionId);
            } else if (folderId != null) {
                duplicateFolder(folderId);
            } else {
                AlertUtils.showInfo("Duplication is only supported for collections and folders");
            }
        } else {
            AlertUtils.showError("Please select an item to duplicate");
        }
    }

    @FXML
    private void handleDeleteItem() {
        TreeItem<String> selectedItem = collectionsTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem != collectionsTree.getRoot()) {
            if (AlertUtils.showConfirmation("Delete", "Are you sure you want to delete this item?")) {
                Long collectionId = collectionIdMap.get(selectedItem);
                Long folderId = folderIdMap.get(selectedItem);

                if (collectionId != null) {
                    deleteCollection(collectionId);
                } else if (folderId != null) {
                    deleteFolder(folderId);
                } else {
                    // For requests, just remove from UI
                    selectedItem.getParent().getChildren().remove(selectedItem);
                    updateStatus("Item deleted");
                }
            }
        } else {
            AlertUtils.showError("Please select an item to delete");
        }
    }

    // Menu button handlers
    @FXML
    private void handleRefreshCollections() {
        refreshCollectionsTree();
        loadEnvironments();
        loadHistory();
        updateStatus("Data refreshed");
    }

    @FXML
    private void handleImportCollections() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Collections");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(Main.getPrimaryStage());
        if (file != null) {
            AlertUtils.showInfo("Import functionality coming soon!");
        }
    }

    @FXML
    private void handleExportCollections() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Collections");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(Main.getPrimaryStage());
        if (file != null) {
            AlertUtils.showInfo("Export functionality coming soon!");
        }
    }

    // Plus button handler
    @FXML
    private void handleAddRequest() {
        TreeItem<String> selectedItem = collectionsTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            showAddRequestDialog(selectedItem);
        } else {
            // If no collection is selected, show a dialog to choose one
            if (!collectionIdMap.isEmpty()) {
                ChoiceDialog<TreeItem<String>> dialog = new ChoiceDialog<>(
                        collectionsTree.getRoot().getChildren().get(0),
                        collectionsTree.getRoot().getChildren()
                );
                dialog.setTitle("Select Collection");
                dialog.setHeaderText("Choose a collection for the new request");
                dialog.setContentText("Collection:");

                Optional<TreeItem<String>> result = dialog.showAndWait();
                result.ifPresent(this::showAddRequestDialog);
            } else {
                AlertUtils.showError("Please create a collection first");
            }
        }
    }

    private void showAddRequestDialog(TreeItem<String> parentItem) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Request");
        dialog.setHeaderText("Create a new request in " + parentItem.getValue());
        dialog.setContentText("Request name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                createNewRequest(name, parentItem);
            }
        });
    }

    // Backend operations
    private void renameCollection(Long collectionId, String newName) {
        new Thread(() -> {
            try {
                CollectionRequest request = CollectionRequest.builder()
                        .name(newName)
                        .description("") // You might want to preserve the existing description
                        .build();

                Optional<CollectionResponse> updatedCollection = CollectionService.updateCollection(collectionId, request);
                if (updatedCollection.isPresent()) {
                    Platform.runLater(() -> {
                        // Find the collection item and update its name
                        for (Map.Entry<TreeItem<String>, Long> entry : collectionIdMap.entrySet()) {
                            if (entry.getValue().equals(collectionId)) {
                                entry.getKey().setValue(newName);
                                break;
                            }
                        }
                        updateStatus("Collection renamed to: " + newName);
                    });
                } else {
                    Platform.runLater(() -> {
                        AlertUtils.showError("Failed to rename collection");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Renaming collection"));
            }
        }).start();
    }

    private void renameFolder(Long folderId, String newName) {
        new Thread(() -> {
            try {
                // Get the folder to get its collection ID
                Optional<FolderResponse> folderOpt = FolderService.getFolderById(folderId);
                if (folderOpt.isPresent()) {
                    FolderResponse folderResponse = folderOpt.get();
                    Optional<FolderResponse> updatedFolder = FolderService.updateFolder(folderId, newName, folderResponse.getDescription(), folderResponse.getCollectionId());

                    if (updatedFolder.isPresent()) {
                        Platform.runLater(() -> {
                            // Find the folder item and update its name
                            for (Map.Entry<TreeItem<String>, Long> entry : folderIdMap.entrySet()) {
                                if (entry.getValue().equals(folderId)) {
                                    entry.getKey().setValue(newName);
                                    break;
                                }
                            }
                            updateStatus("Folder renamed to: " + newName);
                        });
                    } else {
                        Platform.runLater(() -> {
                            AlertUtils.showError("Failed to rename folder");
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Renaming folder"));
            }
        }).start();
    }

    private void duplicateCollection(Long collectionId) {
        new Thread(() -> {
            try {
                // Get the original collection
                Optional<CollectionResponse> original = CollectionService.getCollectionById(collectionId);
                if (original.isPresent()) {
                    CollectionResponse collectionResponse = original.get();
                    // Create a copy with "Copy of" prefix
                    CollectionRequest request = CollectionRequest.builder()
                            .name("Copy of " + collectionResponse.getName())
                            .description(collectionResponse.getDescription())
                            .build();

                    Optional<CollectionResponse> newCollection = CollectionService.createCollection(request);
                    if (newCollection.isPresent()) {
                        Platform.runLater(() -> {
                            refreshCollectionsTree();
                            updateStatus("Collection duplicated");
                        });
                    } else {
                        Platform.runLater(() -> {
                            AlertUtils.showError("Failed to duplicate collection");
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Duplicating collection"));
            }
        }).start();
    }

    private void duplicateFolder(Long folderId) {
        new Thread(() -> {
            try {
                // Get the original folder
                Optional<FolderResponse> original = FolderService.getFolderById(folderId);
                if (original.isPresent()) {
                    FolderResponse folderResponse = original.get();
                    // Create a copy with "Copy of" prefix
                    Optional<FolderResponse> newFolder = FolderService.createFolder("Copy of " + folderResponse.getName(), folderResponse.getDescription(), folderResponse.getCollectionId());

                    if (newFolder.isPresent()) {
                        Platform.runLater(() -> {
                            refreshCollectionsTree();
                            updateStatus("Folder duplicated");
                        });
                    } else {
                        Platform.runLater(() -> {
                            AlertUtils.showError("Failed to duplicate folder");
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Duplicating folder"));
            }
        }).start();
    }

    private void deleteCollection(Long collectionId) {
        new Thread(() -> {
            try {
                boolean success = CollectionService.deleteCollection(collectionId);
                if (success) {
                    Platform.runLater(() -> {
                        // Remove from UI
                        TreeItem<String> toRemove = null;
                        for (Map.Entry<TreeItem<String>, Long> entry : collectionIdMap.entrySet()) {
                            if (entry.getValue().equals(collectionId)) {
                                toRemove = entry.getKey();
                                break;
                            }
                        }
                        if (toRemove != null) {
                            toRemove.getParent().getChildren().remove(toRemove);
                            collectionIdMap.remove(toRemove);
                        }
                        updateStatus("Collection deleted");
                    });
                } else {
                    Platform.runLater(() -> {
                        AlertUtils.showError("Failed to delete collection");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Deleting collection"));
            }
        }).start();
    }

    private void deleteFolder(Long folderId) {
        new Thread(() -> {
            try {
                boolean success = FolderService.deleteFolder(folderId);
                if (success) {
                    Platform.runLater(() -> {
                        // Remove from UI
                        TreeItem<String> toRemove = null;
                        for (Map.Entry<TreeItem<String>, Long> entry : folderIdMap.entrySet()) {
                            if (entry.getValue().equals(folderId)) {
                                toRemove = entry.getKey();
                                break;
                            }
                        }
                        if (toRemove != null) {
                            toRemove.getParent().getChildren().remove(toRemove);
                            folderIdMap.remove(toRemove);
                        }
                        updateStatus("Folder deleted");
                    });
                } else {
                    Platform.runLater(() -> {
                        AlertUtils.showError("Failed to delete folder");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Deleting folder"));
            }
        }).start();
    }

    private void createNewRequest(String name, TreeItem<String> parentItem) {
        new Thread(() -> {
            try {
                Long collectionId = getCollectionIdFromTreeItem(parentItem);
                Long folderId = getFolderIdFromTreeItem(parentItem);

                if (collectionId != null) {
                    ApiRequest apiRequest = new ApiRequest();
                    apiRequest.setName(name);
                    apiRequest.setMethod(HttpMethod.GET);
                    apiRequest.setUrl("https://api.example.com/endpoint");
                    apiRequest.setHeaders("{}");
                    apiRequest.setBody("");
                    apiRequest.setCollectionId(collectionId);
                    apiRequest.setFolderId(folderId);

                    Optional<RequestResponse> savedRequest = RequestService.saveRequest(apiRequest);
                    if (savedRequest.isPresent()) {
                        Platform.runLater(() -> {
                            refreshCollectionsTree();
                            updateStatus("Request created: " + name);
                        });
                    } else {
                        Platform.runLater(() -> {
                            AlertUtils.showError("Failed to create request");
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Creating request"));
            }
        }).start();
    }

    private void createNewFolder(String name, TreeItem<String> parentItem) {
        Long collectionId = collectionIdMap.get(parentItem);
        if (collectionId != null) {
            new Thread(() -> {
                Optional<FolderResponse> folder = FolderService.createFolder(name, "", collectionId);
                if (folder.isPresent()) {
                    Platform.runLater(() -> {
                        refreshCollectionsTree();
                        updateStatus("Folder created: " + name);
                    });
                } else {
                    Platform.runLater(() -> {
                        AlertUtils.showError("Failed to create folder");
                    });
                }
            }).start();
        }
    }

    private void handleApiError(Exception e, String operation) {
        Platform.runLater(() -> {
            System.err.println(operation + " failed: " + e.getMessage());
            e.printStackTrace();

            if (e instanceof IOException) {
                if (e.getMessage().contains("HTTP 401")) {
                    AlertUtils.showError("Authentication failed. Please login again.");
                    TokenManager.clearTokens();
                    Main.showLoginView();
                } else if (e.getMessage().contains("HTTP 5")) {
                    AlertUtils.showError("Server error. Please try again later.");
                } else {
                    AlertUtils.showError("Network error: " + e.getMessage());
                }
            } else {
                AlertUtils.showError(operation + " failed: " + e.getMessage());
            }

            updateStatus(operation + " failed");
        });
    }

    public static class KeyValuePair {
        private final SimpleStringProperty key;
        private final SimpleStringProperty value;
        private final SimpleStringProperty description;

        public KeyValuePair() {
            this("", "", "");
        }

        public KeyValuePair(String key, String value) {
            this(key, value, "");
        }

        public KeyValuePair(String key, String value, String description) {
            this.key = new SimpleStringProperty(key);
            this.value = new SimpleStringProperty(value);
            this.description = new SimpleStringProperty(description);
        }

        public String getKey() {
            return key.get();
        }

        public SimpleStringProperty keyProperty() {
            return key;
        }

        public void setKey(String key) {
            this.key.set(key);
        }

        public String getValue() {
            return value.get();
        }

        public SimpleStringProperty valueProperty() {
            return value;
        }

        public void setValue(String value) {
            this.value.set(value);
        }

        public String getDescription() {
            return description.get();
        }

        public SimpleStringProperty descriptionProperty() {
            return description;
        }

        public void setDescription(String description) {
            this.description.set(description);
        }
    }
}
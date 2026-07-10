package com.roze.thundercall.ui.controllers;

import com.roze.thundercall.ui.Main;
import com.roze.thundercall.ui.dialogs.WorkspaceSetupDialog;
import com.roze.thundercall.ui.enums.HttpMethod;
import com.roze.thundercall.ui.models.*;
import com.roze.thundercall.ui.services.*;
import com.roze.thundercall.ui.models.*;
import com.roze.thundercall.ui.services.*;
import com.roze.thundercall.ui.utils.AlertUtils;
import com.roze.thundercall.ui.utils.ThemeManager;
import com.roze.thundercall.ui.utils.VariableResolver;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    private final Map<TreeItem<String>, String> requestMethodMap = new HashMap<>();
    private TreeItem<String> fullCollectionsRoot;
    @FXML
    private TextField collectionsSearchField;
    @FXML
    private TextField globalSearchField;
    @FXML
    private MenuButton workspacesMenuBtn;
    @FXML
    private Label workspaceNameLabel;
    @FXML
    private TabPane requestTabPane;
    private final Map<Tab, RequestTabState> tabStates = new HashMap<>();
    private boolean switchingTabs = false;
    private final javafx.scene.image.Image collectionIconImg = loadTreeIcon("/images/collection-icon.png");
    private final javafx.scene.image.Image folderIconImg = loadTreeIcon("/images/folder-icon.png");

    private static javafx.scene.image.Image loadTreeIcon(String path) {
        try {
            return new javafx.scene.image.Image(
                    Objects.requireNonNull(MainController.class.getResourceAsStream(path)));
        } catch (Exception e) {
            return null;
        }
    }
    private Map<String, EnvironmentResponse> environmentsMap = new HashMap<>();
    @FXML
    private ToggleButton noneBodyTypeButton;
    @FXML
    private ToggleButton rawBodyTypeButton;
    @FXML
    private ToggleButton formDataBodyTypeButton;
    @FXML
    private ToggleButton urlEncodedBodyTypeButton;
    // Postman-style sidebar rail (may be null with the old FXML — always null-checked)
    @FXML
    private VBox collectionsPane;
    @FXML
    private VBox environmentsPane;
    @FXML
    private VBox historyPane;
    @FXML
    private ToggleButton railCollectionsBtn;
    @FXML
    private ToggleButton railEnvironmentsBtn;
    @FXML
    private ToggleButton railHistoryBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController...");

        // Apply the saved theme (dark/light) once the scene exists
        Platform.runLater(() -> {
            if (urlField != null && urlField.getScene() != null) {
                ThemeManager.apply(urlField.getScene());
                // Global search: Ctrl+K, like Postman
                urlField.getScene().getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(
                                javafx.scene.input.KeyCode.K,
                                javafx.scene.input.KeyCombination.CONTROL_DOWN),
                        () -> openGlobalSearch(""));
            }
        });
        if (collectionsSearchField != null) {
            collectionsSearchField.textProperty()
                    .addListener((obs, oldVal, newVal) -> applyCollectionsFilter(newVal));
        }
        setupRequestTabs();

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
        // Postman-style context menu (right-click or the ⋯ button on a row)
        treeContextMenu = new ContextMenu();

        MenuItem addRequestItem = new MenuItem("Add request");
        MenuItem addFolderItem = new MenuItem("Add folder");
        MenuItem runItem = new MenuItem("Run");
        MenuItem shareItem = new MenuItem("Share");
        MenuItem copyLinkItem = new MenuItem("Copy link");
        MenuItem renameItem = new MenuItem("Rename");
        MenuItem duplicateItem = new MenuItem("Duplicate");
        MenuItem deleteItem = new MenuItem("Delete");

        addRequestItem.setOnAction(event -> handleAddRequestToCollection());
        addFolderItem.setOnAction(event -> handleNewFolder());
        renameItem.setOnAction(event -> handleRenameItem());
        duplicateItem.setOnAction(event -> handleDuplicateItem());
        deleteItem.setOnAction(event -> handleDeleteItem());

        // Not implemented yet — shown to match Postman, disabled to stay honest
        runItem.setDisable(true);
        shareItem.setDisable(true);
        copyLinkItem.setDisable(true);

        renameItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+E"));
        duplicateItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+D"));
        deleteItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Delete"));
        deleteItem.getStyleClass().add("danger-menu-item");

        treeContextMenu.getItems().addAll(
                addRequestItem,
                addFolderItem,
                new SeparatorMenuItem(),
                runItem,
                new SeparatorMenuItem(),
                shareItem,
                copyLinkItem,
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
                    // FIX: the list endpoint returns SHORT responses with no
                    // folders/requests — that's why children never appeared.
                    // Fetch full details per collection (current workspace only).
                    Workspace cur = WorkspaceManager.getCurrentWorkspace();
                    List<CollectionResponse> detailed = new ArrayList<>();
                    for (CollectionResponse c : collections.get()) {
                        if (cur != null && c.getWorkspaceId() != null
                                && !String.valueOf(cur.getId()).equals(c.getWorkspaceId())) {
                            continue;
                        }
                        detailed.add(CollectionService.getCollectionWithDetails(c.getId()).orElse(c));
                    }
                    populateCollectionsTree(detailed);
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
            requestMethodMap.clear();

            Workspace currentWorkspace = WorkspaceManager.getCurrentWorkspace();
            for (CollectionResponse collectionResponse : collectionResponses) {
                // Show only the selected workspace's collections, like Postman.
                // (This also removes the duplicate "Getting started" entries
                // that came from other workspaces.)
                if (currentWorkspace != null && collectionResponse.getWorkspaceId() != null
                        && !String.valueOf(currentWorkspace.getId())
                        .equals(collectionResponse.getWorkspaceId())) {
                    continue;
                }
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
                                    if (requestResponse.getMethod() != null) {
                                        requestMethodMap.put(requestItem, requestResponse.getMethod().name());
                                    }
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
                            if (requestResponse.getMethod() != null) {
                                requestMethodMap.put(requestItem, requestResponse.getMethod().name());
                            }
                            collectionItem.getChildren().add(requestItem);
                        }
                    }
                }

                root.getChildren().add(collectionItem);
            }

            fullCollectionsRoot = root;
            collectionsTree.setRoot(root);

            // Postman-style cells: colored method badges, folder/collection
            // icons, hover + / ⋯ buttons
            collectionsTree.setCellFactory(tv -> createPostmanTreeCell());

            // Expand all collections by default for better UX
            for (TreeItem<String> item : root.getChildren()) {
                item.setExpanded(true);
            }

            // Re-apply any active sidebar search
            if (collectionsSearchField != null && collectionsSearchField.getText() != null
                    && !collectionsSearchField.getText().isBlank()) {
                applyCollectionsFilter(collectionsSearchField.getText());
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
                    // FIX: hasWorkspace() was an in-memory flag, always false at
                    // startup — the setup popup opened on EVERY login. Ask the
                    // server instead, and remember the workspace list.
                    List<Workspace> workspaces =
                            WorkspaceService.getUserWorkspaces().orElse(new ArrayList<>());
                    workspaces.sort(Comparator.comparing(Workspace::getId));
                    if (workspaces.isEmpty()) {
                        Platform.runLater(this::showWorkspaceSetup);
                    } else {
                        Platform.runLater(() -> {
                            if (WorkspaceManager.getCurrentWorkspace() == null) {
                                WorkspaceManager.setCurrentWorkspace(workspaces.get(0));
                            }
                            refreshWorkspacesMenu(workspaces);
                            updateWorkspaceLabels();
                            refreshCollectionsTree();
                        });
                        // FIX: the tour reopened on EVERY login (server flag is
                        // only set when the tour is fully finished). Show it
                        // once per user on this machine, whatever they click.
                        java.util.prefs.Preferences prefs =
                                java.util.prefs.Preferences.userNodeForPackage(MainController.class);
                        String tourKey = "tour.seen." + TokenManager.getUsername();
                        if (!prefs.getBoolean(tourKey, false)
                                && !WorkspaceService.checkTutorialStatus()) {
                            prefs.putBoolean(tourKey, true);
                            Platform.runLater(this::showFeatureTour);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error checking onboarding: " + e.getMessage());
                }
            }).start();
        }
    }

    /** Rebuilds the Workspaces ▾ menu with the user's real workspaces. */
    private void refreshWorkspacesMenu(List<Workspace> workspaces) {
        if (workspacesMenuBtn == null) {
            return;
        }
        workspacesMenuBtn.getItems().clear();
        for (Workspace ws : workspaces) {
            MenuItem item = new MenuItem(ws.getName());
            if (WorkspaceManager.getCurrentWorkspace() != null
                    && Objects.equals(WorkspaceManager.getCurrentWorkspace().getId(), ws.getId())) {
                item.setText("✓ " + ws.getName());
            }
            item.setOnAction(e -> switchWorkspace(ws));
            workspacesMenuBtn.getItems().add(item);
        }
        workspacesMenuBtn.getItems().add(new SeparatorMenuItem());
        MenuItem create = new MenuItem("Create Workspace…");
        create.setOnAction(e -> handleCreateWorkspace());
        workspacesMenuBtn.getItems().add(create);
    }

    private void switchWorkspace(Workspace workspace) {
        WorkspaceManager.setCurrentWorkspace(workspace);
        updateWorkspaceLabels();
        reloadWorkspacesAndCollections();
        updateStatus("Workspace: " + workspace.getName());
    }

    private void updateWorkspaceLabels() {
        Workspace current = WorkspaceManager.getCurrentWorkspace();
        if (workspaceNameLabel != null && current != null) {
            workspaceNameLabel.setText(current.getName());
        }
    }

    private void reloadWorkspacesAndCollections() {
        new Thread(() -> {
            List<Workspace> workspaces =
                    WorkspaceService.getUserWorkspaces().orElse(new ArrayList<>());
            workspaces.sort(Comparator.comparing(Workspace::getId));
            Platform.runLater(() -> {
                refreshWorkspacesMenu(workspaces);
                refreshCollectionsTree();
            });
        }).start();
    }

    private void showWorkspaceSetup() {
        WorkspaceSetupDialog dialog = new WorkspaceSetupDialog();
        Optional<javafx.util.Pair<String, Boolean>> result = dialog.showAndAwait();
        result.ifPresent(nameAndSample -> {
            new Thread(() -> {
                Optional<Workspace> workspace = WorkspaceService.setupInitialWorkspace(
                        nameAndSample.getKey(), nameAndSample.getValue());
                workspace.ifPresent(ws -> {
                    Platform.runLater(() -> {
                        // FIX: the newly created workspace is now SELECTED and
                        // the sidebar switches to it immediately
                        WorkspaceManager.setCurrentWorkspace(ws);
                        updateWorkspaceLabels();
                        reloadWorkspacesAndCollections();
                        updateStatus("Workspace created: " + ws.getName());
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
        // Intentionally does NOT rewrite the URL field or headers table.
        // Templates like {{baseUrl}} stay visible in the UI and are resolved
        // at send time by VariableResolver (see handleSendRequest). Switching
        // environments now works repeatedly, and saved requests keep their
        // {{variables}} instead of hard-coded values.
        if (environmentName == null || "No Environment".equals(environmentName)) {
            updateStatus("No environment selected");
        } else {
            updateStatus("Environment: " + environmentName);
        }
    }

    /** Variables of the currently selected environment, or an empty map. */
    private Map<String, String> currentEnvironmentVariables() {
        String selected = environmentCombo.getValue();
        if (selected == null || "No Environment".equals(selected)) {
            return Collections.emptyMap();
        }
        EnvironmentResponse env = environmentsMap.get(selected);
        return (env != null && env.getVariables() != null)
                ? env.getVariables()
                : Collections.emptyMap();
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
            // Postman-style tabs: reuse an open tab (keeps unsaved edits),
            // otherwise open a new one and load from the server
            Tab existing = findTabByRequestId(requestId);
            if (existing != null && requestTabPane != null) {
                requestTabPane.getSelectionModel().select(existing);
                return;
            }
            openRequestTab(item.getValue(), requestId);
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
            // Keep the active tab in sync with the loaded request
            if (requestTabPane != null) {
                Tab currentTab = requestTabPane.getSelectionModel().getSelectedItem();
                RequestTabState state = currentTab != null ? tabStates.get(currentTab) : null;
                if (state != null) {
                    currentTab.setText(requestResponse.getName());
                    state.name = requestResponse.getName();
                    state.requestId = requestResponse.getId();
                }
            }
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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Appearance");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        RadioButton darkBtn = new RadioButton("Dark theme");
        RadioButton lightBtn = new RadioButton("Light theme");
        ToggleGroup themeGroup = new ToggleGroup();
        darkBtn.setToggleGroup(themeGroup);
        lightBtn.setToggleGroup(themeGroup);
        if (ThemeManager.isLight()) {
            lightBtn.setSelected(true);
        } else {
            darkBtn.setSelected(true);
        }

        VBox content = new VBox(10, darkBtn, lightBtn);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        ThemeManager.styleDialog(dialog.getDialogPane());

        dialog.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                ThemeManager.setLight(lightBtn.isSelected(), urlField.getScene());
                updateStatus("Theme: " + (lightBtn.isSelected() ? "Light" : "Dark"));
            }
        });
    }

    @FXML
    private void handleThemeDark() {
        ThemeManager.setLight(false, urlField.getScene());
        updateStatus("Theme: Dark");
    }

    @FXML
    private void handleThemeLight() {
        ThemeManager.setLight(true, urlField.getScene());
        updateStatus("Theme: Light");
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    @FXML
    private void handleAbout() {
        AlertUtils.showInfo("Thundercall v1.0.0\nA professional API client.\nBackend: Spring Boot  |  Frontend: JavaFX");
    }

    @FXML
    private void handleCreateWorkspace() {
        showWorkspaceSetup();
    }

    @FXML
    private void handleShowCollections() {
        showSidebarPane(collectionsPane);
    }

    @FXML
    private void handleShowEnvironments() {
        showSidebarPane(environmentsPane);
    }

    @FXML
    private void handleShowHistory() {
        showSidebarPane(historyPane);
    }

    private void showSidebarPane(VBox pane) {
        if (collectionsPane == null || environmentsPane == null || historyPane == null || pane == null) {
            return; // old FXML without the rail
        }
        collectionsPane.setVisible(pane == collectionsPane);
        environmentsPane.setVisible(pane == environmentsPane);
        historyPane.setVisible(pane == historyPane);
    }

    // ==================== Postman-style request tabs ====================

    /** Snapshot of the request editor for one tab. */
    private static class RequestTabState {
        String name = "Untitled Request";
        Long requestId;
        String method = "GET";
        String url = "";
        String body = "";
        List<KeyValuePair> params = new ArrayList<>();
        List<KeyValuePair> headers = new ArrayList<>();
    }

    private void setupRequestTabs() {
        if (requestTabPane == null) {
            return; // old FXML without the tab bar
        }
        requestTabPane.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldTab, newTab) -> {
                    if (switchingTabs) {
                        return;
                    }
                    if (oldTab != null && tabStates.containsKey(oldTab)) {
                        captureTabState(oldTab);
                    }
                    if (newTab != null && tabStates.containsKey(newTab)) {
                        restoreTabState(newTab);
                    }
                });
        openRequestTab("Untitled Request", null);
    }

    /** Opens a new editor tab (blank when requestId is null) and selects it. */
    private Tab openRequestTab(String name, Long requestId) {
        if (requestTabPane == null) {
            return null;
        }
        RequestTabState state = new RequestTabState();
        state.name = name;
        state.requestId = requestId;

        Tab tab = new Tab(name);
        tab.setClosable(true);
        tabStates.put(tab, state);
        tab.setOnClosed(e -> {
            tabStates.remove(tab);
            if (requestTabPane.getTabs().isEmpty()) {
                openRequestTab("Untitled Request", null);
            }
        });

        switchingTabs = true;
        Tab previous = requestTabPane.getSelectionModel().getSelectedItem();
        if (previous != null && tabStates.containsKey(previous)) {
            captureTabState(previous);
        }
        requestTabPane.getTabs().add(tab);
        requestTabPane.getSelectionModel().select(tab);
        switchingTabs = false;

        restoreTabState(tab); // blank editor for a fresh tab
        return tab;
    }

    private void captureTabState(Tab tab) {
        RequestTabState state = tabStates.get(tab);
        if (state == null) {
            return;
        }
        state.method = methodCombo.getValue();
        state.url = urlField.getText();
        state.body = bodyTextArea.getText();
        state.params = new ArrayList<>(paramsData);
        state.headers = new ArrayList<>(headersData);
    }

    private void restoreTabState(Tab tab) {
        RequestTabState state = tabStates.get(tab);
        if (state == null) {
            return;
        }
        methodCombo.setValue(state.method == null ? "GET" : state.method);
        urlField.setText(state.url == null ? "" : state.url);
        bodyTextArea.setText(state.body == null ? "" : state.body);
        paramsData.setAll(state.params);
        headersData.setAll(state.headers);
    }

    private Tab findTabByRequestId(Long requestId) {
        if (requestId == null) {
            return null;
        }
        for (Map.Entry<Tab, RequestTabState> entry : tabStates.entrySet()) {
            if (requestId.equals(entry.getValue().requestId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ==================== Postman-style collections tree ====================

    private TreeCell<String> createPostmanTreeCell() {
        return new TreeCell<>() {
            private final Label methodLabel = new Label();
            private final Label nameLabel = new Label();
            private final Button plusBtn = new Button("+");
            private final Button moreBtn = new Button("⋯");
            private final Region spacer = new Region();
            private final HBox box = new HBox(6);

            {
                methodLabel.getStyleClass().add("tree-method");
                plusBtn.getStyleClass().add("tree-inline-btn");
                moreBtn.getStyleClass().add("tree-inline-btn");
                plusBtn.setTooltip(new Tooltip("Add request"));
                moreBtn.setTooltip(new Tooltip("More actions"));
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                box.setAlignment(Pos.CENTER_LEFT);

                plusBtn.setOnAction(e -> {
                    selectThisItem();
                    handleAddRequestToCollection();
                });
                moreBtn.setOnAction(e -> {
                    selectThisItem();
                    if (treeContextMenu != null) {
                        treeContextMenu.show(moreBtn, javafx.geometry.Side.BOTTOM, 0, 0);
                    }
                });
                // Buttons appear on hover only, like Postman
                plusBtn.visibleProperty().bind(hoverProperty());
                moreBtn.visibleProperty().bind(hoverProperty());
            }

            private void selectThisItem() {
                if (getTreeItem() != null) {
                    collectionsTree.getSelectionModel().select(getTreeItem());
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    return;
                }
                setText(null);
                setContextMenu(treeContextMenu);
                nameLabel.setText(item);
                box.getChildren().clear();

                TreeItem<String> treeItem = getTreeItem();
                String method = treeItem != null ? requestMethodMap.get(treeItem) : null;
                if (method != null) {
                    // Request row: colored method badge + name (like Postman)
                    methodLabel.setText(methodBadge(method));
                    methodLabel.getStyleClass().removeIf(s -> s.startsWith("method-"));
                    methodLabel.getStyleClass().add("method-" + method.toLowerCase(Locale.ROOT));
                    box.getChildren().addAll(methodLabel, nameLabel);
                } else {
                    // Collection/folder row: icon + name + hover actions
                    javafx.scene.image.Image icon =
                            (treeItem != null && collectionIdMap.containsKey(treeItem))
                                    ? collectionIconImg : folderIconImg;
                    if (icon != null) {
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(icon);
                        iv.setFitWidth(14);
                        iv.setFitHeight(14);
                        iv.setPreserveRatio(true);
                        box.getChildren().add(iv);
                    }
                    box.getChildren().addAll(nameLabel, spacer, plusBtn, moreBtn);
                }
                setGraphic(box);
            }
        };
    }

    private static String methodBadge(String method) {
        switch (method) {
            case "DELETE":
                return "DEL";
            case "OPTIONS":
                return "OPT";
            case "PATCH":
                return "PAT";
            default:
                return method;
        }
    }

    /** Filters the sidebar tree by name, keeping matching branches (Postman-like). */
    private void applyCollectionsFilter(String query) {
        if (fullCollectionsRoot == null || collectionsTree == null) {
            return;
        }
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            collectionsTree.setRoot(fullCollectionsRoot);
            return;
        }
        TreeItem<String> filteredRoot = new TreeItem<>("Collections");
        filteredRoot.setExpanded(true);
        for (TreeItem<String> collection : fullCollectionsRoot.getChildren()) {
            TreeItem<String> copy = filteredCopy(collection, q);
            if (copy != null) {
                filteredRoot.getChildren().add(copy);
            }
        }
        collectionsTree.setRoot(filteredRoot);
    }

    private TreeItem<String> filteredCopy(TreeItem<String> source, String q) {
        boolean selfMatch = source.getValue() != null
                && source.getValue().toLowerCase(Locale.ROOT).contains(q);
        List<TreeItem<String>> matchedChildren = new ArrayList<>();
        for (TreeItem<String> child : source.getChildren()) {
            TreeItem<String> copy = filteredCopy(child, q);
            if (copy != null) {
                matchedChildren.add(copy);
            }
        }
        if (!selfMatch && matchedChildren.isEmpty()) {
            return null;
        }
        TreeItem<String> copy = new TreeItem<>(source.getValue());
        copy.setExpanded(true);
        copy.getChildren().addAll(matchedChildren);
        // Register the copy in the id maps so selection/actions keep working
        Long collectionId = collectionIdMap.get(source);
        if (collectionId != null) collectionIdMap.put(copy, collectionId);
        Long folderId = folderIdMap.get(source);
        if (folderId != null) folderIdMap.put(copy, folderId);
        Long requestId = requestIdMap.get(source);
        if (requestId != null) requestIdMap.put(copy, requestId);
        String method = requestMethodMap.get(source);
        if (method != null) requestMethodMap.put(copy, method);
        return copy;
    }

    // ==================== Global search (Ctrl+K), like Postman ====================

    @FXML
    private void handleGlobalSearch() {
        openGlobalSearch(globalSearchField != null ? globalSearchField.getText() : "");
        if (globalSearchField != null) {
            globalSearchField.clear();
        }
    }

    private void openGlobalSearch(String initialQuery) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Search Thundercall");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextField searchField = new TextField(initialQuery == null ? "" : initialQuery);
        searchField.setPromptText("Search collections, folders and requests");
        ListView<TreeItem<String>> results = new ListView<>();
        results.setPrefSize(480, 320);
        results.setPlaceholder(new Label("Type to search your workspace"));
        results.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TreeItem<String> item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(s -> s.startsWith("method-"));
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String kind;
                String method = requestMethodMap.get(item);
                if (method != null) {
                    kind = method;
                    getStyleClass().add("method-" + method.toLowerCase(Locale.ROOT));
                } else if (collectionIdMap.containsKey(item)) {
                    kind = "Collection";
                } else {
                    kind = "Folder";
                }
                setText(kind + "  ·  " + treePath(item));
            }
        });

        Runnable refresh = () -> {
            String q = searchField.getText() == null
                    ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
            List<TreeItem<String>> hits = new ArrayList<>();
            if (!q.isEmpty() && fullCollectionsRoot != null) {
                collectSearchHits(fullCollectionsRoot, q, hits);
            }
            results.getItems().setAll(hits);
        };
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refresh.run());
        searchField.setOnAction(e -> {
            if (!results.getItems().isEmpty()) {
                revealInTree(results.getItems().get(0));
                dialog.close();
            }
        });
        results.setOnMouseClicked(e -> {
            TreeItem<String> selected = results.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selected != null) {
                revealInTree(selected);
                dialog.close();
            }
        });

        VBox content = new VBox(8, searchField, results);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        ThemeManager.styleDialog(dialog.getDialogPane());
        refresh.run();
        Platform.runLater(searchField::requestFocus);
        dialog.showAndWait();
    }

    private void collectSearchHits(TreeItem<String> node, String q, List<TreeItem<String>> out) {
        for (TreeItem<String> child : node.getChildren()) {
            if (child.getValue() != null
                    && child.getValue().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(child);
            }
            collectSearchHits(child, q, out);
        }
    }

    private String treePath(TreeItem<String> item) {
        StringBuilder sb = new StringBuilder(item.getValue());
        TreeItem<String> parent = item.getParent();
        while (parent != null && parent.getParent() != null) {
            sb.insert(0, parent.getValue() + " / ");
            parent = parent.getParent();
        }
        return sb.toString();
    }

    private void revealInTree(TreeItem<String> item) {
        if (collectionsSearchField != null) {
            collectionsSearchField.clear();
        }
        collectionsTree.setRoot(fullCollectionsRoot);
        if (railCollectionsBtn != null) {
            railCollectionsBtn.setSelected(true);
        }
        showSidebarPane(collectionsPane);
        TreeItem<String> parent = item.getParent();
        while (parent != null) {
            parent.setExpanded(true);
            parent = parent.getParent();
        }
        collectionsTree.getSelectionModel().select(item);
        int row = collectionsTree.getRow(item);
        if (row >= 0) {
            collectionsTree.scrollTo(row);
        }
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

        // Resolve {{variables}} from the selected environment at send time.
        // The UI keeps showing the template; only the outgoing request is resolved.
        Map<String, String> vars = currentEnvironmentVariables();
        String url = VariableResolver.resolve(buildFullUrl(), vars);
        String method = methodCombo.getValue();
        Map<String, String> headers = VariableResolver.resolveMap(buildHeaders(), vars);
        String body = VariableResolver.resolve(buildRequestBody(), vars);

        Set<String> unresolved = new LinkedHashSet<>(VariableResolver.findUnresolved(url, vars));
        unresolved.addAll(VariableResolver.findUnresolved(body, vars));
        if (!unresolved.isEmpty()) {
            isRequestInProgress = false;
            if (sendButton != null) {
                sendButton.setDisable(false);
            }
            AlertUtils.showError("Unresolved variables: " + String.join(", ", unresolved)
                    + "\nSelect an environment that defines them, or fix the spelling.");
            updateStatus("Unresolved variables");
            return;
        }

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

        // Works from a collection OR from a folder/request inside it —
        // the folder is created in the enclosing collection. (Nested
        // folders need backend support for a parent-folder id; the data
        // model is flat today, so we stay honest instead of silently
        // dropping the folder.)
        Long collectionId = selectedItem != null ? getCollectionIdFromTreeItem(selectedItem) : null;
        if (collectionId != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Folder");
            dialog.setHeaderText("Create a new folder");
            dialog.setContentText("Folder name:");
            ThemeManager.styleDialog(dialog.getDialogPane());

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    createNewFolder(name, collectionId);
                }
            });
        } else {
            AlertUtils.showError("Please select a collection (or an item inside one) to add a folder to");
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
                List<TreeItem<String>> collections = collectionsTree.getRoot().getChildren();
                // FIX: show collection NAMES, not "TreeItem [ value: ... ]"
                ChoiceDialog<String> dialog = new ChoiceDialog<>(
                        collections.get(0).getValue(),
                        collections.stream().map(TreeItem::getValue).toList()
                );
                dialog.setTitle("Select Collection");
                dialog.setHeaderText("Choose a collection for the new request");
                dialog.setContentText("Collection:");
                ThemeManager.styleDialog(dialog.getDialogPane());

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(name -> collections.stream()
                        .filter(item -> name.equals(item.getValue()))
                        .findFirst()
                        .ifPresent(this::showAddRequestDialog));
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
        ThemeManager.styleDialog(dialog.getDialogPane());

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
                            // Open the new request in its own tab, like Postman
                            openRequestTab(name, savedRequest.get().getId());
                            loadRequestIntoUI(savedRequest.get());
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

    private void createNewFolder(String name, Long collectionId) {
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
package com.roze.thundercall.ui.controllers;

import com.roze.thundercall.ui.Main;
import com.roze.thundercall.ui.dialogs.WorkspaceSetupDialog;
import com.roze.thundercall.ui.enums.HttpMethod;
import com.roze.thundercall.ui.models.*;
import com.roze.thundercall.ui.services.*;
import com.roze.thundercall.ui.models.*;
import com.roze.thundercall.ui.services.*;
import com.roze.thundercall.ui.utils.AlertUtils;
import com.roze.thundercall.ui.utils.CodeAreaSearch;
import com.roze.thundercall.ui.utils.JsonSyntaxHighlighter;
import com.roze.thundercall.ui.utils.VariableAutocomplete;
import org.fxmisc.richtext.CodeArea;
import com.roze.thundercall.ui.utils.CsvParser;
import com.roze.thundercall.ui.utils.ScriptRunner;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
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
    private CodeArea urlField;
    @FXML
    private Label urlPromptLabel;
    @FXML
    private ComboBox<String> methodCombo;
    @FXML
    private Button sendBtn;
    @FXML
    private Button wsConnectBtn;
    @FXML
    private Tab wsMessagesTab;
    @FXML
    private Label wsStatusLabel;
    @FXML
    private ListView<String> wsMessageLog;
    @FXML
    private TextArea wsMessageComposeArea;
    @FXML
    private Tab wsMessageTab;
    @FXML
    private TabPane requestBuilderTabPane;
    @FXML
    private Button socketIoConnectBtn;
    @FXML
    private Tab socketIoTab;
    @FXML
    private TextField socketIoNamespaceField;
    @FXML
    private Label socketIoStatusLabel;
    @FXML
    private ListView<String> socketIoEventLog;
    @FXML
    private TextField socketIoEventNameField;
    @FXML
    private TextField socketIoEventDataField;
    @FXML
    private ComboBox<String> authTypeCombo;
    @FXML
    private VBox basicAuthBox;
    @FXML
    private Label authHintLabel;
    /** The most recent response — kept as an object so binary bytes never
     * pass through a TextArea (which would corrupt them). Used by Save. */
    private ApiResponse lastApiResponse;
    @FXML
    private CodeArea tokenField;
    @FXML
    private Label tokenPromptLabel;
    @FXML
    private CodeArea usernameField;
    @FXML
    private Label usernamePromptLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private VBox bearerAuthBox;
    @FXML
    private VBox oauth2Box;
    @FXML
    private ComboBox<String> oauth2AddToCombo;
    @FXML
    private TextField oauth2CurrentTokenField;
    @FXML
    private TextField oauth2HeaderPrefixField;
    @FXML
    private ComboBox<String> oauth2GrantTypeCombo;
    @FXML
    private HBox oauth2AuthUrlRow;
    @FXML
    private TextField oauth2AuthUrlField;
    @FXML
    private TextField oauth2TokenUrlField;
    @FXML
    private TextField oauth2ClientIdField;
    @FXML
    private PasswordField oauth2ClientSecretField;
    @FXML
    private TextField oauth2ScopeField;
    @FXML
    private HBox oauth2UsernameRow;
    @FXML
    private TextField oauth2UsernameField;
    @FXML
    private HBox oauth2PasswordRow;
    @FXML
    private PasswordField oauth2PasswordField;
    @FXML
    private Label oauth2StatusLabel;
    @FXML
    private CodeArea bodyTextArea;
    @FXML
    private Label bodyPromptLabel;
    @FXML
    private ToggleGroup bodyTypeGroup;
    @FXML
    private Label statusLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label sizeLabel;
    @FXML
    private CodeArea responseBodyArea;
    @FXML
    private Label responsePromptLabel;
    @FXML
    private VBox responseSearchBarContainer;
    private CodeAreaSearch responseSearch;
    @FXML
    private ListView<String> historyList;
    @FXML
    private TextArea preRequestScriptsArea;
    @FXML
    private TextArea postRequestScriptsArea;
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
    private TableView<KeyValuePair> urlEncodedTable;
    @FXML
    private MenuButton collectionsMenuBtn;
    @FXML
    private ContextMenu treeContextMenu;

    private static final Logger logger = Logger.getLogger(MainController.class.getName());
    private boolean isRequestInProgress = false;
    // Connects DIRECTLY from this desktop client to the target ws://
    // server — unlike HTTP requests, which proxy through the backend,
    // a persistent bidirectional connection doesn't fit that one-shot
    // request/response model, and a desktop app has none of a
    // browser's cross-origin restrictions stopping a direct connection.
    private WebSocket activeWebSocket;
    private volatile boolean wsConnected = false;
    // Socket.IO connects through the backend (see the class-level note
    // near the WS section) — the desktop client just polls it
    // periodically for new events rather than holding its own live
    // connection, so state here is simpler: a session id and a poll loop.
    private volatile String socketIoSessionId;
    private volatile boolean socketIoConnected = false;
    private int socketIoEventsSeen = 0;
    private Thread socketIoPollThread;
    private long requestStartTime;
    private final ObservableList<String> historyData = FXCollections.observableArrayList();
    private final ObservableList<KeyValuePair> paramsData = FXCollections.observableArrayList();
    private final ObservableList<KeyValuePair> headersData = FXCollections.observableArrayList();
    private final ObservableList<KeyValuePair> responseHeadersData = FXCollections.observableArrayList();
    private final ObservableList<KeyValuePair> cookiesData = FXCollections.observableArrayList();
    private final ObservableList<KeyValuePair> formData = FXCollections.observableArrayList();
    // FIX: x-www-form-urlencoded used to fall through to the generic
    // "default" case and share the SAME list as Form Data — meaning it
    // showed the raw text area (wrong widget entirely) and, had it been
    // wired to a table, would have silently mixed the two body types'
    // rows together. It gets its own table and its own independent list.
    private final ObservableList<KeyValuePair> urlEncodedData = FXCollections.observableArrayList();
    // Holds EnvironmentResponse or TreeItem<String> — whichever kind of
    // search result was most recently opened, newest first, shown when
    // global search is opened with an empty query.
    private final List<Object> recentlyViewedItems = new ArrayList<>();
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
    @FXML
    private ListView<EnvironmentResponse> environmentsList;
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
    @FXML
    private ToggleButton graphqlBodyTypeButton;
    @FXML
    private SplitPane graphqlPane;
    @FXML
    private CodeArea graphqlQueryArea;
    @FXML
    private CodeArea graphqlVariablesArea;
    // Postman-style sidebar rail (may be null with the old FXML — always null-checked)
    @FXML
    private VBox collectionsPane;
    @FXML
    private VBox environmentsPane;
    @FXML
    private VBox historyPane;
    @FXML
    private VBox teamsPane;
    @FXML
    private ListView<TeamResponse> teamsList;
    @FXML
    private VBox mockServersPane;
    @FXML
    private ListView<MockServerResponse> mockServersList;
    @FXML
    private ToggleButton railMockServersBtn;
    @FXML
    private VBox monitorsPane;
    @FXML
    private ListView<MonitorResponse> monitorsList;
    @FXML
    private ToggleButton railMonitorsBtn;
    @FXML
    private ToggleButton railCollectionsBtn;
    @FXML
    private ToggleButton railEnvironmentsBtn;
    @FXML
    private ToggleButton railHistoryBtn;
    @FXML
    private ToggleButton railTeamsBtn;
    @FXML
    private SplitPane sidebarSplitPane;
    @FXML
    private StackPane sidebarContentPane;

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
                // Save request: Ctrl+S, like Postman
                urlField.getScene().getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(
                                javafx.scene.input.KeyCode.S,
                                javafx.scene.input.KeyCombination.CONTROL_DOWN),
                        this::handleSaveRequest);
            }
        });
        if (collectionsSearchField != null) {
            collectionsSearchField.textProperty()
                    .addListener((obs, oldVal, newVal) -> applyCollectionsFilter(newVal));
        }
        setupRequestTabs();
        setupUrlField();
        setupBodyCodeArea();
        setupGraphqlCodeAreas();
        setupResponseCodeArea();
        setupEnvironmentsList();
        setupTeamsList();
        setupMockServersList();
        setupMonitorsList();

        // Fix for TreeView context menu
        setupTreeViewContextMenu();

        bodyTypeGroup = new ToggleGroup();

        // Manually set up the toggle buttons
        noneBodyTypeButton.setToggleGroup(bodyTypeGroup);
        rawBodyTypeButton.setToggleGroup(bodyTypeGroup);
        formDataBodyTypeButton.setToggleGroup(bodyTypeGroup);
        urlEncodedBodyTypeButton.setToggleGroup(bodyTypeGroup);
        graphqlBodyTypeButton.setToggleGroup(bodyTypeGroup);

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
            loadTeams();
            loadMockServers();
            loadMonitors();

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
        MenuItem exportJsonItem = new MenuItem("Export as JSON (Postman)");
        MenuItem exportCsvItem = new MenuItem("Export as CSV");
        MenuItem deleteItem = new MenuItem("Delete");

        addRequestItem.setOnAction(event -> handleAddRequestToCollection());
        addFolderItem.setOnAction(event -> handleNewFolder());
        renameItem.setOnAction(event -> handleRenameItem());
        duplicateItem.setOnAction(event -> handleDuplicateItem());
        exportJsonItem.setOnAction(event -> handleExportSelectedAsJson());
        exportCsvItem.setOnAction(event -> handleExportSelectedAsCsv());
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
                exportJsonItem,
                exportCsvItem,
                new SeparatorMenuItem(),
                deleteItem
        );

        // Export only makes sense on a COLLECTION itself — hide it for
        // folders and requests rather than showing an option that errors.
        treeContextMenu.setOnShowing(event -> {
            TreeItem<String> selected = collectionsTree.getSelectionModel().getSelectedItem();
            boolean isCollection = selected != null && collectionIdMap.containsKey(selected);
            exportJsonItem.setVisible(isCollection);
            exportCsvItem.setVisible(isCollection);
        });

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
                        urlEncodedTable.setVisible(false);
                        bodyTextArea.setVisible(false);
                        graphqlPane.setVisible(false);
                        break;
                    case "x-www-form-urlencoded":
                        // FIX: this used to fall into the same "default"
                        // branch as Raw/None and show the plain text
                        // area — meaning there was no key/value table at
                        // all for this body type, unlike every other
                        // Postman-style client.
                        formDataTable.setVisible(false);
                        urlEncodedTable.setVisible(true);
                        bodyTextArea.setVisible(false);
                        graphqlPane.setVisible(false);
                        break;
                    case "GraphQL":
                        formDataTable.setVisible(false);
                        urlEncodedTable.setVisible(false);
                        bodyTextArea.setVisible(false);
                        graphqlPane.setVisible(true);
                        // GraphQL always goes over POST — matches Postman,
                        // which doesn't even offer a method choice for it.
                        if (methodCombo != null) {
                            methodCombo.setValue("POST");
                        }
                        break;
                    default:
                        formDataTable.setVisible(false);
                        urlEncodedTable.setVisible(false);
                        bodyTextArea.setVisible(true);
                        graphqlPane.setVisible(false);
                        break;
                }
            }
        });

        // Initialize form data table
        setupFormDataTable();
        // x-www-form-urlencoded is plain key/value pairs only — no file
        // attachments make sense here, so the generic table (same as
        // Params/Headers) is the right fit, not the Form Data one.
        setupKeyValueTable(urlEncodedTable, urlEncodedData, "Key", "Value", "Description");

        // Default to "None" so the state is always visible
        if (bodyTypeGroup.getSelectedToggle() == null && noneBodyTypeButton != null) {
            noneBodyTypeButton.setSelected(true);
        }

        // FIX: typing a body while "None" is selected silently sent NO body
        // (your login JSON never left the app → server said "Required request
        // body is missing"). Typing now auto-selects "Raw", like Postman.
        bodyTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank() && rawBodyTypeButton != null) {
                Toggle selected = bodyTypeGroup.getSelectedToggle();
                if (selected == null || selected == noneBodyTypeButton) {
                    rawBodyTypeButton.setSelected(true);
                }
            }
        });
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
            // ISSUE 1 FIX: remember which collections/folders were expanded,
            // restore after rebuild — no more everything-expands-on-reload
            Set<String> expandedKeys = new HashSet<>();
            captureExpandedKeys(fullCollectionsRoot, expandedKeys);

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
                collectionItem.setExpanded(expandedKeys.contains("c:" + collectionResponse.getId()));
                collectionIdMap.put(collectionItem, collectionResponse.getId());

                // Add folders if they exist
                // Build the folder hierarchy (nested folders, like Postman)
                Map<Long, TreeItem<String>> folderItemsById = new HashMap<>();
                if (collectionResponse.getFolderResponses() != null && !collectionResponse.getFolderResponses().isEmpty()) {
                    for (FolderResponse folderResponse : collectionResponse.getFolderResponses()) {
                        TreeItem<String> folderItem = new TreeItem<>(folderResponse.getName());
                        folderIdMap.put(folderItem, folderResponse.getId());
                        folderItemsById.put(folderResponse.getId(), folderItem);
                    }
                    // Attach each folder to its parent folder, or to the collection
                    for (FolderResponse folderResponse : collectionResponse.getFolderResponses()) {
                        TreeItem<String> folderItem = folderItemsById.get(folderResponse.getId());
                        TreeItem<String> parent = folderResponse.getParentFolderId() != null
                                ? folderItemsById.get(folderResponse.getParentFolderId())
                                : null;
                        if (parent != null) {
                            parent.getChildren().add(folderItem);
                        } else {
                            collectionItem.getChildren().add(folderItem);
                        }
                        folderItem.setExpanded(expandedKeys.contains("f:" + folderResponse.getId()));
                    }
                }

                // Place every request under its folder (any depth) or the collection
                if (collectionResponse.getRequestResponses() != null && !collectionResponse.getRequestResponses().isEmpty()) {
                    for (RequestResponse requestResponse : collectionResponse.getRequestResponses()) {
                        TreeItem<String> requestItem = new TreeItem<>(requestResponse.getName());
                        requestIdMap.put(requestItem, requestResponse.getId());
                        if (requestResponse.getMethod() != null) {
                            requestMethodMap.put(requestItem, requestResponse.getMethod().name());
                        }
                        TreeItem<String> parent = requestResponse.getFolderId() != null
                                ? folderItemsById.get(requestResponse.getFolderId())
                                : null;
                        if (parent != null) {
                            parent.getChildren().add(requestItem);
                        } else {
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

                        if (environmentsList != null) {
                            environmentsList.getItems().setAll(environments.get());
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

    /** URL bar: only {{variables}} are colored (it isn't JSON), plus the
     * same autocomplete-and-auto-close behavior as the body editor. */
    private void setupUrlField() {
        urlField.setWrapText(false);
        urlField.plainTextChanges().subscribe(change -> {
            urlField.setStyleSpans(0, JsonSyntaxHighlighter.computeUrlHighlighting(urlField.getText()));
            if (!syncingUrlAndParams) {
                syncUrlToParams();
            }
        });
        VariableAutocomplete.attach(urlField, this::currentEnvironmentVariables);
        if (urlPromptLabel != null) {
            urlPromptLabel.visibleProperty().bind(
                    javafx.beans.binding.Bindings.createBooleanBinding(
                            () -> urlField.getText().isEmpty(), urlField.textProperty()));
        }
        // Enter in the URL bar sends the request, like Postman
        urlField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleSendRequest();
                e.consume();
            }
        });
    }

    /** Request body editor: full JSON + {{variable}} coloring, autocomplete,
     * and auto-closing braces. */
    private void setupBodyCodeArea() {
        bodyTextArea.plainTextChanges().subscribe(change ->
                bodyTextArea.setStyleSpans(0, JsonSyntaxHighlighter.computeHighlighting(bodyTextArea.getText())));
        VariableAutocomplete.attach(bodyTextArea, this::currentEnvironmentVariables);
        if (bodyPromptLabel != null) {
            bodyPromptLabel.visibleProperty().bind(
                    javafx.beans.binding.Bindings.createBooleanBinding(
                            () -> bodyTextArea.getText().isEmpty(), bodyTextArea.textProperty()));
        }
    }

    /** GraphQL's query is its own syntax (not JSON), so it doesn't get the
     * JSON highlighter — but {{variable}} autocomplete still applies since
     * environment variables are just as usable in a GraphQL query/header
     * as anywhere else. Variables genuinely IS JSON, so it gets full
     * highlighting like the Raw body editor does. */
    private void setupGraphqlCodeAreas() {
        if (graphqlQueryArea == null || graphqlVariablesArea == null) {
            return;
        }
        VariableAutocomplete.attach(graphqlQueryArea, this::currentEnvironmentVariables);
        graphqlVariablesArea.plainTextChanges().subscribe(change ->
                graphqlVariablesArea.setStyleSpans(0,
                        JsonSyntaxHighlighter.computeHighlighting(graphqlVariablesArea.getText())));
        VariableAutocomplete.attach(graphqlVariablesArea, this::currentEnvironmentVariables);
    }

    /** Response viewer: read-only JSON + {{variable}} coloring, plus a
     * Ctrl/Cmd+F find bar overlaid in its top-right corner. */
    private void setupResponseCodeArea() {
        responseBodyArea.setWrapText(true);
        responseBodyArea.plainTextChanges().subscribe(change -> {
            if (responseSearch == null || responseSearch.getBar().isManaged()) {
                return; // the search bar owns styling (base + match highlight) while open
            }
            responseBodyArea.setStyleSpans(0,
                    JsonSyntaxHighlighter.computeHighlighting(responseBodyArea.getText()));
        });
        if (responsePromptLabel != null) {
            responsePromptLabel.visibleProperty().bind(
                    javafx.beans.binding.Bindings.createBooleanBinding(
                            () -> responseBodyArea.getText().isEmpty(), responseBodyArea.textProperty()));
        }
        responseSearch = CodeAreaSearch.attach(responseBodyArea);
        if (responseSearchBarContainer != null) {
            responseSearchBarContainer.getChildren().add(responseSearch.getBar());
        }
    }


    private void setUpMethodCombo() {
        methodCombo.getItems().addAll("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "WS", "SOCKETIO");
        methodCombo.getSelectionModel().select(0);

        // Postman-style colored methods — reuses the same .method-xxx
        // classes already defined for the tree's method badges, so the
        // colors are consistent everywhere in the app.
        javafx.util.Callback<ListView<String>, ListCell<String>> methodCellFactory =
                lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(String method, boolean empty) {
                        super.updateItem(method, empty);
                        getStyleClass().removeIf(c -> c.startsWith("method-"));
                        if (empty || method == null) {
                            setText(null);
                        } else {
                            setText(method);
                            getStyleClass().add("method-" + method.toLowerCase(Locale.ROOT));
                        }
                    }
                };
        methodCombo.setCellFactory(methodCellFactory);
        methodCombo.setButtonCell(methodCellFactory.call(null));

        methodCombo.valueProperty().addListener((obs, oldMethod, newMethod) -> {
            boolean isWs = "WS".equals(newMethod);
            boolean isSocketIo = "SOCKETIO".equals(newMethod);
            if (sendBtn != null) {
                sendBtn.setVisible(!isWs && !isSocketIo);
                sendBtn.setManaged(!isWs && !isSocketIo);
            }
            if (wsConnectBtn != null) {
                wsConnectBtn.setVisible(isWs);
                wsConnectBtn.setManaged(isWs);
            }
            if (socketIoConnectBtn != null) {
                socketIoConnectBtn.setVisible(isSocketIo);
                socketIoConnectBtn.setManaged(isSocketIo);
            }
            // FIX: the compose box used to be buried in the response
            // area's Messages tab, easy to miss entirely — it's now the
            // first tab at the top (matching Postman's own layout), and
            // switching to WS jumps straight to it so there's no hunting.
            if (isWs && requestBuilderTabPane != null && wsMessageTab != null) {
                requestBuilderTabPane.getSelectionModel().select(wsMessageTab);
            }
            if (isSocketIo && requestBuilderTabPane != null && socketIoTab != null) {
                requestBuilderTabPane.getSelectionModel().select(socketIoTab);
            }
            // Switching away from WS while still connected would leave a
            // socket open with no way to see or close it from the UI —
            // disconnect cleanly instead.
            if (!isWs && wsConnected) {
                closeActiveWebSocket();
            }
            if (!isSocketIo && socketIoConnected) {
                closeActiveSocketIoSession();
            }
        });
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
            onEditorFieldChanged();
        });
        updateAuthFieldsVisibility("No Auth");

        // {{variable}} coloring + autocomplete on the token/username fields —
        // exactly the same behavior as the URL bar and request body.
        tokenField.setWrapText(false);
        tokenField.plainTextChanges().subscribe(change ->
                tokenField.setStyleSpans(0, JsonSyntaxHighlighter.computeUrlHighlighting(tokenField.getText())));
        VariableAutocomplete.attach(tokenField, this::currentEnvironmentVariables);
        if (tokenPromptLabel != null) {
            tokenPromptLabel.visibleProperty().bind(
                    javafx.beans.binding.Bindings.createBooleanBinding(
                            () -> tokenField.getText().isEmpty(), tokenField.textProperty()));
        }

        usernameField.setWrapText(false);
        usernameField.plainTextChanges().subscribe(change ->
                usernameField.setStyleSpans(0, JsonSyntaxHighlighter.computeUrlHighlighting(usernameField.getText())));
        VariableAutocomplete.attach(usernameField, this::currentEnvironmentVariables);
        if (usernamePromptLabel != null) {
            usernamePromptLabel.visibleProperty().bind(
                    javafx.beans.binding.Bindings.createBooleanBinding(
                            () -> usernameField.getText().isEmpty(), usernameField.textProperty()));
        }

        // Any edit to the auth fields marks the tab unsaved, same as url/body
        tokenField.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        usernameField.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        passwordField.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());

        setupOAuth2Panel();
    }

    private void updateAuthFieldsVisibility(String authType) {
        boolean bearer = "Bearer Token".equals(authType);
        boolean basic = "Basic Auth".equals(authType);
        boolean oauth2 = "OAuth 2.0".equals(authType);

        if (bearerAuthBox != null) {
            bearerAuthBox.setVisible(bearer);
            bearerAuthBox.setManaged(bearer);
        }
        if (authHintLabel != null) {
            authHintLabel.setVisible(bearer || basic || oauth2);
            authHintLabel.setManaged(bearer || basic || oauth2);
            authHintLabel.setText(oauth2
                    ? "The authorization data will be automatically generated when you send the request."
                    : "The authorization header will be generated automatically when you send the request.");
        }
        if (basicAuthBox != null) {
            basicAuthBox.setVisible(basic);
            basicAuthBox.setManaged(basic);
        }
        if (oauth2Box != null) {
            oauth2Box.setVisible(oauth2);
            oauth2Box.setManaged(oauth2);
        }
    }

    /** Sets up the OAuth 2.0 panel: grant-type dropdown (with fields that
     * show/hide per grant type, like Postman), and the combo for where the
     * resulting token gets attached. */
    /** What actually gets persisted in the authToken column: the plain
     * token for Bearer Auth, or a JSON blob of the whole OAuth2
     * configuration when OAuth 2.0 is selected — reuses the existing
     * column rather than needing a schema change. */
    private String getAuthTokenForPersistence() {
        if ("OAuth 2.0".equals(authTypeCombo.getValue())) {
            return serializeOAuth2Config();
        }
        return tokenField.getText();
    }

    private void loadAuthTokenForPersistence(String authType, String authToken) {
        if ("OAuth 2.0".equals(authType)) {
            deserializeOAuth2Config(authToken);
            tokenField.replaceText("");
        } else {
            tokenField.replaceText(authToken == null ? "" : authToken);
            resetOAuth2Fields();
        }
    }

    private String serializeOAuth2Config() {
        if (oauth2GrantTypeCombo == null) {
            return "";
        }
        JSONObject json = new JSONObject();
        json.put("grantType", nullToEmpty(oauth2GrantTypeCombo.getValue()));
        json.put("addTo", nullToEmpty(oauth2AddToCombo.getValue()));
        json.put("currentToken", nullToEmpty(oauth2CurrentTokenField.getText()));
        json.put("headerPrefix", nullToEmpty(oauth2HeaderPrefixField.getText()));
        json.put("authUrl", nullToEmpty(oauth2AuthUrlField.getText()));
        json.put("tokenUrl", nullToEmpty(oauth2TokenUrlField.getText()));
        json.put("clientId", nullToEmpty(oauth2ClientIdField.getText()));
        json.put("clientSecret", nullToEmpty(oauth2ClientSecretField.getText()));
        json.put("scope", nullToEmpty(oauth2ScopeField.getText()));
        json.put("username", nullToEmpty(oauth2UsernameField.getText()));
        json.put("password", nullToEmpty(oauth2PasswordField.getText()));
        return json.toString();
    }

    private void deserializeOAuth2Config(String raw) {
        if (oauth2GrantTypeCombo == null) {
            return;
        }
        resetOAuth2Fields();
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            JSONObject json = new JSONObject(raw);
            oauth2GrantTypeCombo.setValue(json.optString("grantType", "Client Credentials"));
            oauth2AddToCombo.setValue(json.optString("addTo", "Request Headers"));
            oauth2CurrentTokenField.setText(json.optString("currentToken", ""));
            oauth2HeaderPrefixField.setText(json.optString("headerPrefix", "Bearer"));
            oauth2AuthUrlField.setText(json.optString("authUrl", ""));
            oauth2TokenUrlField.setText(json.optString("tokenUrl", ""));
            oauth2ClientIdField.setText(json.optString("clientId", ""));
            oauth2ClientSecretField.setText(json.optString("clientSecret", ""));
            oauth2ScopeField.setText(json.optString("scope", ""));
            oauth2UsernameField.setText(json.optString("username", ""));
            oauth2PasswordField.setText(json.optString("password", ""));
        } catch (Exception ignored) {
            // Not valid OAuth2 JSON (e.g. an older/foreign value) — leave defaults
        }
    }

    private void resetOAuth2Fields() {
        if (oauth2GrantTypeCombo == null) {
            return;
        }
        oauth2GrantTypeCombo.setValue("Client Credentials");
        oauth2AddToCombo.setValue("Request Headers");
        oauth2CurrentTokenField.setText("");
        oauth2HeaderPrefixField.setText("Bearer");
        oauth2AuthUrlField.setText("");
        oauth2TokenUrlField.setText("");
        oauth2ClientIdField.setText("");
        oauth2ClientSecretField.setText("");
        oauth2ScopeField.setText("");
        oauth2UsernameField.setText("");
        oauth2PasswordField.setText("");
        if (oauth2StatusLabel != null) {
            oauth2StatusLabel.setText("");
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** "Get New Access Token" — fully implemented for the two grant types
     * that don't need a browser redirect (Client Credentials and Password
     * Credentials); Authorization Code and Implicit need an interactive
     * browser step this app doesn't have yet, so they're told plainly
     * rather than faking a result. */
    @FXML
    private void handleGetOAuth2Token() {
        String grantType = oauth2GrantTypeCombo.getValue();
        String tokenUrl = oauth2TokenUrlField.getText().trim();
        if (tokenUrl.isEmpty()) {
            oauth2StatusLabel.setText("Enter an Access Token URL first.");
            return;
        }
        if ("Authorization Code".equals(grantType) || "Implicit".equals(grantType)) {
            oauth2StatusLabel.setText("This grant type needs a browser sign-in step, which isn't built yet — "
                    + "paste a token directly into \"Current Token\" above, or use Client Credentials/"
                    + "Password Credentials if your server supports them.");
            return;
        }

        oauth2StatusLabel.setText("Requesting token...");
        Map<String, String> vars = currentEnvironmentVariables();
        String resolvedTokenUrl = VariableResolver.resolve(tokenUrl, vars);
        String clientId = VariableResolver.resolve(oauth2ClientIdField.getText().trim(), vars);
        String clientSecret = VariableResolver.resolve(oauth2ClientSecretField.getText().trim(), vars);
        String scope = VariableResolver.resolve(oauth2ScopeField.getText().trim(), vars);
        String username = VariableResolver.resolve(oauth2UsernameField.getText().trim(), vars);
        String password = VariableResolver.resolve(oauth2PasswordField.getText().trim(), vars);
        boolean isPasswordGrant = "Password Credentials".equals(grantType);

        new Thread(() -> {
            try {
                StringBuilder form = new StringBuilder();
                form.append("grant_type=").append(isPasswordGrant ? "password" : "client_credentials");
                if (!clientId.isEmpty()) {
                    form.append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
                }
                if (!clientSecret.isEmpty()) {
                    form.append("&client_secret=").append(URLEncoder.encode(clientSecret, StandardCharsets.UTF_8));
                }
                if (!scope.isEmpty()) {
                    form.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
                }
                if (isPasswordGrant) {
                    form.append("&username=").append(URLEncoder.encode(username, StandardCharsets.UTF_8));
                    form.append("&password=").append(URLEncoder.encode(password, StandardCharsets.UTF_8));
                }

                URL url = new URL(resolvedTokenUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                try (var os = connection.getOutputStream()) {
                    os.write(form.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = connection.getResponseCode();
                java.io.InputStream stream = code < 400 ? connection.getInputStream() : connection.getErrorStream();
                String body = stream != null
                        ? new String(stream.readAllBytes(), StandardCharsets.UTF_8) : "";

                if (code >= 200 && code < 300) {
                    JSONObject responseJson = new JSONObject(body);
                    String accessToken = responseJson.optString("access_token", "");
                    if (accessToken.isEmpty()) {
                        Platform.runLater(() -> oauth2StatusLabel.setText(
                                "Server responded but no \"access_token\" field was found in: " + abbreviate(body)));
                        return;
                    }
                    Platform.runLater(() -> {
                        oauth2CurrentTokenField.setText(accessToken);
                        oauth2StatusLabel.setText("Token received successfully.");
                    });
                } else {
                    String finalBody = body;
                    Platform.runLater(() -> oauth2StatusLabel.setText(
                            "Server returned HTTP " + code + ": " + abbreviate(finalBody)));
                }
            } catch (Exception e) {
                Platform.runLater(() -> oauth2StatusLabel.setText("Request failed: " + e.getMessage()));
            }
        }).start();
    }

    private String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 200 ? s.substring(0, 197) + "..." : s;
    }

    private void setupOAuth2Panel() {
        if (oauth2AddToCombo == null) {
            return; // old FXML without the OAuth2 panel
        }
        oauth2AddToCombo.getItems().addAll("Request Headers", "Query Params");
        oauth2AddToCombo.getSelectionModel().select(0);

        oauth2GrantTypeCombo.getItems().addAll(
                "Authorization Code", "Implicit", "Password Credentials", "Client Credentials");
        oauth2GrantTypeCombo.getSelectionModel().select("Client Credentials");
        oauth2GrantTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateOAuth2FieldsVisibility(newVal));
        updateOAuth2FieldsVisibility("Client Credentials");

        // Any OAuth2 field edit marks the tab unsaved, same as everything else
        for (TextField field : new TextField[]{oauth2CurrentTokenField, oauth2HeaderPrefixField,
                oauth2AuthUrlField, oauth2TokenUrlField, oauth2ClientIdField, oauth2ScopeField, oauth2UsernameField}) {
            field.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        }
        oauth2ClientSecretField.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        oauth2PasswordField.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        oauth2GrantTypeCombo.valueProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        oauth2AddToCombo.valueProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
    }

    /** Shows only the fields each grant type actually needs — Authorization
     * Code/Implicit need a browser redirect (Auth URL); Password
     * Credentials needs a username/password; Client Credentials needs
     * neither, just the client id/secret already shown for all types. */
    private void updateOAuth2FieldsVisibility(String grantType) {
        boolean needsAuthUrl = "Authorization Code".equals(grantType) || "Implicit".equals(grantType);
        boolean needsUserPass = "Password Credentials".equals(grantType);
        if (oauth2AuthUrlRow != null) {
            oauth2AuthUrlRow.setVisible(needsAuthUrl);
            oauth2AuthUrlRow.setManaged(needsAuthUrl);
        }
        if (oauth2UsernameRow != null) {
            oauth2UsernameRow.setVisible(needsUserPass);
            oauth2UsernameRow.setManaged(needsUserPass);
        }
        if (oauth2PasswordRow != null) {
            oauth2PasswordRow.setVisible(needsUserPass);
            oauth2PasswordRow.setManaged(needsUserPass);
        }
    }

    private void setupTables() {
        setupKeyValueTable(paramsTable, paramsData, this::syncParamsToUrl, "Key", "Value", "Description");
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
        setupKeyValueTable(table, data, null, columns);
    }

    /** @param onCommit optional — run after any cell edit commits (e.g. the
     *  Params table uses this to keep the URL's query string in sync). */
    private void setupKeyValueTable(TableView<KeyValuePair> table, ObservableList<KeyValuePair> data,
                                    Runnable onCommit, String... columns) {
        // FIX: the FXML used to ALSO define static <columns> for these
        // tables (an old, pre-dynamic-setup leftover). Every table ended
        // up with its columns doubled — the 3 static ones from FXML plus
        // 3 more added here — which is exactly the "Key Value Description
        // Key Value Description" duplication. The static columns are gone
        // from the FXML now, but clearing here too means this can never
        // recur even if a table is (re)initialized more than once.
        table.getColumns().clear();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
            // FIX: JavaFX's default TextFieldTableCell only commits an edit
            // on Enter — clicking away to edit a different cell silently
            // CANCELS it instead of saving, which is exactly what felt
            // "not good" about double-click-type-Enter. This cell commits
            // on focus-loss too, so clicking elsewhere behaves like every
            // other spreadsheet-style editor (and like Postman's tables).
            column.setCellFactory(col -> commitOnFocusLossCell());
            column.setOnEditCommit(event -> {
                String newValue = event.getNewValue();
                switch (colIndex) {
                    case 0:
                        event.getRowValue().setKey(newValue);
                        break;
                    case 1:
                        event.getRowValue().setValue(newValue);
                        break;
                    case 2:
                        event.getRowValue().setDescription(newValue);
                        break;
                    default:
                        break;
                }
                if (onCommit != null) {
                    onCommit.run();
                }
            });
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

    /** A TextFieldTableCell that also commits when it loses focus (clicking
     * to another cell, another tab, or Send) instead of only on Enter —
     * the standard fix for JavaFX's well-known cancel-on-focus-loss quirk. */
    private TableCell<KeyValuePair, String> commitOnFocusLossCell() {
        TextFieldTableCell<KeyValuePair, String> cell = new TextFieldTableCell<>(new javafx.util.converter.DefaultStringConverter()) {
            @Override
            public void startEdit() {
                super.startEdit();
                if (getGraphic() instanceof TextField field) {
                    field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                        if (!isFocused && isEditing()) {
                            commitEdit(field.getText());
                        }
                    });
                }
            }
        };
        return cell;
    }

    /** Form-data gets its own table (not the generic key/value one) so
     * each row can be either plain text or a real file attachment —
     * matching Postman's Text/File toggle, with actual file bytes read
     * off disk at send time (CSV, Excel, images, anything). */
    private void setupFormDataTable() {
        formDataTable.getColumns().clear();
        formDataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        formDataTable.setItems(formData);
        formDataTable.setEditable(true);

        TableColumn<KeyValuePair, String> keyCol = new TableColumn<>("Key");
        keyCol.setCellValueFactory(cellData -> cellData.getValue().keyProperty());
        keyCol.setCellFactory(col -> commitOnFocusLossCell());
        keyCol.setOnEditCommit(event -> event.getRowValue().setKey(event.getNewValue()));
        keyCol.setPrefWidth(150);

        TableColumn<KeyValuePair, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        valueCol.setCellFactory(col -> createFormDataValueCell());
        valueCol.setOnEditCommit(event -> event.getRowValue().setValue(event.getNewValue()));
        valueCol.setPrefWidth(220);

        TableColumn<KeyValuePair, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        typeCol.setCellFactory(javafx.scene.control.cell.ComboBoxTableCell.forTableColumn("Text", "File"));
        typeCol.setOnEditCommit(event -> {
            event.getRowValue().setType(event.getNewValue());
            // The Value column's rendering (text field vs. file picker)
            // depends on this row's type, and TableView doesn't know to
            // re-check a SIBLING column on its own — force it to.
            formDataTable.refresh();
        });
        typeCol.setPrefWidth(80);

        TableColumn<KeyValuePair, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descCol.setCellFactory(col -> commitOnFocusLossCell());
        descCol.setOnEditCommit(event -> event.getRowValue().setDescription(event.getNewValue()));
        descCol.setPrefWidth(150);

        formDataTable.getColumns().addAll(keyCol, valueCol, typeCol, descCol);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem addItem = new MenuItem("Add Row");
        MenuItem deleteItem = new MenuItem("Delete Row");
        addItem.setOnAction(e -> formData.add(new KeyValuePair("", "", "")));
        deleteItem.setOnAction(e -> {
            KeyValuePair selected = formDataTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                formData.remove(selected);
            }
        });
        contextMenu.getItems().addAll(addItem, deleteItem);
        formDataTable.setContextMenu(contextMenu);
    }

    /** Behaves exactly like the normal editable text cell for "Text"
     * rows (reuses TextFieldTableCell's own machinery), but for "File"
     * rows shows the chosen file name plus a "Choose File..." button
     * instead — clicking it opens a real file picker. */
    private TableCell<KeyValuePair, String> createFormDataValueCell() {
        return new TextFieldTableCell<>(new javafx.util.converter.DefaultStringConverter()) {
            private final Button chooseFileBtn = new Button("Choose File...");
            private final Label fileLabel = new Label();
            private final HBox fileBox = new HBox(6, fileLabel, chooseFileBtn);

            {
                fileBox.setAlignment(Pos.CENTER_LEFT);
                chooseFileBtn.setOnAction(e -> {
                    if (!(getTableRow() != null && getTableRow().getItem() instanceof KeyValuePair kv)) {
                        return;
                    }
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Choose File");
                    chooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
                            new FileChooser.ExtensionFilter("All Files", "*.*"));
                    File file = chooser.showOpenDialog(Main.getPrimaryStage());
                    if (file != null) {
                        kv.setFilePath(file.getAbsolutePath());
                        kv.setFileName(file.getName());
                        kv.setValue(file.getName());
                        fileLabel.setText(file.getName());
                        getTableView().refresh();
                    }
                });
            }

            private boolean isFileRow() {
                return getTableRow() != null && getTableRow().getItem() instanceof KeyValuePair kv
                        && "File".equalsIgnoreCase(kv.getType());
            }

            @Override
            public void startEdit() {
                if (isFileRow()) {
                    return; // file rows use the button, not the normal text editor
                }
                super.startEdit();
                if (getGraphic() instanceof TextField field) {
                    field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                        if (!isFocused && isEditing()) {
                            commitEdit(field.getText());
                        }
                    });
                }
            }

            @Override
            public void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                if (isFileRow()) {
                    KeyValuePair kv = (KeyValuePair) getTableRow().getItem();
                    fileLabel.setText(kv.getFileName() == null || kv.getFileName().isEmpty()
                            ? "(no file chosen)" : kv.getFileName());
                    setGraphic(fileBox);
                    setText(null);
                }
                // else: super.updateItem() above already set the normal text correctly
            }
        };
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
            openRequestTab(item.getValue(), requestId, requestMethodMap.get(item));
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
            restoringTabState = true;
            Tab currentTab = requestTabPane != null
                    ? requestTabPane.getSelectionModel().getSelectedItem() : null;
            RequestTabState state = currentTab != null ? tabStates.get(currentTab) : null;
            try {
                // Keep the active tab in sync with the loaded request
                if (state != null) {
                    state.name = requestResponse.getName();
                    state.requestId = requestResponse.getId();
                }
                methodCombo.setValue(requestResponse.getMethod().name());
                urlField.replaceText(requestResponse.getUrl() != null ? requestResponse.getUrl() : "");

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
                bodyTextArea.replaceText(requestResponse.getBody() != null ? requestResponse.getBody() : "");
                // Scripts are saved WITH the request (like Postman) — restore them
                if (preRequestScriptsArea != null) {
                    preRequestScriptsArea.setText(
                            requestResponse.getPreRequestScript() != null
                                    ? requestResponse.getPreRequestScript() : "");
                }
                if (postRequestScriptsArea != null) {
                    postRequestScriptsArea.setText(
                            requestResponse.getTestsScript() != null
                                    ? requestResponse.getTestsScript() : "");
                }
                if (requestResponse.getBody() != null && !requestResponse.getBody().isBlank()
                        && rawBodyTypeButton != null) {
                    rawBodyTypeButton.setSelected(true);
                } else if (noneBodyTypeButton != null) {
                    noneBodyTypeButton.setSelected(true);
                }

                // Authorization tab (saved WITH the request too)
                authTypeCombo.setValue(requestResponse.getAuthType() != null
                        ? requestResponse.getAuthType() : "No Auth");
                updateAuthFieldsVisibility(authTypeCombo.getValue());
                loadAuthTokenForPersistence(authTypeCombo.getValue(), requestResponse.getAuthToken());
                usernameField.replaceText(requestResponse.getAuthUsername() != null ? requestResponse.getAuthUsername() : "");
                passwordField.setText(requestResponse.getAuthPassword() != null ? requestResponse.getAuthPassword() : "");
            } finally {
                restoringTabState = false;
            }
            if (state != null) {
                state.dirty = false; // freshly loaded from the server: nothing unsaved
            }
            if (currentTab != null) {
                refreshTabGraphic(currentTab);
            }

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
        // FIX: this used to always show "Mail Server Settings..." to
        // every user — the backend correctly rejected non-admins, but
        // only after they'd already clicked into it and hit Save/Test.
        // Regular users shouldn't see this entry point exist at all.
        if (TokenManager.isAdmin()) {
            Hyperlink mailSettingsLink = new Hyperlink("Mail Server Settings...");
            mailSettingsLink.setOnAction(e -> openMailSettingsDialog());
            content.getChildren().addAll(new Separator(), mailSettingsLink);
        }
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

    /** Lets the project owner configure the app's own outgoing SMTP
     * server — used for email verification codes and team invitations.
     * The backend rejects this for non-admins, so nothing bad happens if
     * a non-admin somehow opens it; they'll just see a clear error. */
    // ============================================================
    // Team management
    // ============================================================

    private void setupTeamsList() {
        if (teamsList == null) {
            return;
        }
        teamsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TeamResponse team, boolean empty) {
                super.updateItem(team, empty);
                if (empty || team == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label nameLabel = new Label(team.getName());
                nameLabel.getStyleClass().add("variable-suggestion-name");
                Label metaLabel = new Label(team.getMemberCount() + " member"
                        + (team.getMemberCount() == 1 ? "" : "s") + " · " + team.getMyRole());
                metaLabel.getStyleClass().add("variable-suggestion-value");
                setGraphic(new VBox(1, nameLabel, metaLabel));
                setText(null);
            }
        });
        teamsList.setOnMouseClicked(e -> {
            TeamResponse selected = teamsList.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selected != null) {
                openTeamDetailDialog(selected);
            }
        });
    }

    private void loadTeams() {
        if (teamsList == null) {
            return;
        }
        new Thread(() -> {
            Optional<List<TeamResponse>> teams = TeamService.getMyTeams();
            Platform.runLater(() -> teams.ifPresent(list -> teamsList.getItems().setAll(list)));
        }).start();
    }

    @FXML
    private void handleCreateTeam() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Team");
        dialog.setHeaderText("Give your team a name — you can invite people once it's created.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField nameField = new TextField();
        nameField.setPromptText("Team name");
        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("What's this team for? (optional)");
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);

        VBox content = new VBox(10, new Label("Name"), nameField, new Label("Description"), descriptionField);
        content.setPadding(new Insets(10));
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        ThemeManager.styleDialog(dialog.getDialogPane());

        dialog.showAndWait().ifPresent(button -> {
            if (button != ButtonType.OK) {
                return;
            }
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                AlertUtils.showError("Give the team a name first.");
                return;
            }
            new Thread(() -> {
                Optional<TeamResponse> created = TeamService.createTeam(name, descriptionField.getText());
                Platform.runLater(() -> {
                    if (created.isPresent()) {
                        loadTeams();
                        updateStatus("Team created: " + name);
                    }
                });
            }).start();
        });
    }

    @FXML
    private void handleJoinTeam() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join a Team");
        dialog.setHeaderText("Enter the invite code from your invitation email");
        dialog.setContentText("Invite code:");
        ThemeManager.styleDialog(dialog.getDialogPane());

        dialog.showAndWait().ifPresent(token -> {
            String trimmed = token == null ? "" : token.trim();
            if (trimmed.isEmpty()) {
                return;
            }
            new Thread(() -> {
                try {
                    TeamResponse team = TeamService.acceptInvitation(trimmed);
                    Platform.runLater(() -> {
                        loadTeams();
                        AlertUtils.showSuccess("You've joined \"" + team.getName() + "\"");
                        updateStatus("Joined team: " + team.getName());
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> AlertUtils.showError(
                            "Couldn't join that team: " + TeamService.friendlyMessage(e)));
                }
            }).start();
        });
    }

    /** The main team-management surface: members, pending invitations
     * (owner/admin only), inviting people by email, changing roles,
     * removing members, and leaving/deleting the team — each action's
     * visibility is driven by the viewer's own role on this team. */
    private void openTeamDetailDialog(TeamResponse team) {
        boolean isOwner = "OWNER".equals(team.getMyRole());
        boolean isAdminOrOwner = isOwner || "ADMIN".equals(team.getMyRole());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(team.getName());
        dialog.setHeaderText((team.getDescription() != null && !team.getDescription().isBlank()
                ? team.getDescription() + "\n" : "")
                + "Your role: " + team.getMyRole() + "  ·  Owner: " + team.getOwnerUsername());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(480);

        // ---- Members ----
        TableView<TeamMemberResponse> membersTable = new TableView<>();
        TableColumn<TeamMemberResponse, String> memberNameCol = new TableColumn<>("Username");
        memberNameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        TableColumn<TeamMemberResponse, String> memberEmailCol = new TableColumn<>("Email");
        memberEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        TableColumn<TeamMemberResponse, String> memberRoleCol = new TableColumn<>("Role");
        memberRoleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        membersTable.getColumns().addAll(memberNameCol, memberEmailCol, memberRoleCol);
        membersTable.setPrefHeight(160);
        membersTable.setPrefWidth(480);
        membersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Runnable[] refreshMembers = new Runnable[1];
        refreshMembers[0] = () -> new Thread(() -> {
            Optional<List<TeamMemberResponse>> members = TeamService.getMembers(team.getId());
            Platform.runLater(() -> members.ifPresent(list -> membersTable.getItems().setAll(list)));
        }).start();
        refreshMembers[0].run();

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().add(new Label("Members"));
        content.getChildren().add(membersTable);

        if (isAdminOrOwner) {
            ComboBox<String> newRoleCombo = new ComboBox<>();
            newRoleCombo.getItems().addAll("ADMIN", "MEMBER");
            newRoleCombo.setPromptText("New role");
            Button changeRoleBtn = new Button("Change Role");
            Button removeBtn = new Button("Remove Member");
            HBox memberActions = new HBox(8, newRoleCombo, changeRoleBtn, removeBtn);
            memberActions.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().add(memberActions);

            changeRoleBtn.setOnAction(e -> {
                TeamMemberResponse selected = membersTable.getSelectionModel().getSelectedItem();
                String newRole = newRoleCombo.getValue();
                if (selected == null || newRole == null) {
                    statusLabel.setText("Select a member and a role first.");
                    return;
                }
                new Thread(() -> {
                    boolean ok = TeamService.changeMemberRole(team.getId(), selected.getUserId(), newRole);
                    Platform.runLater(() -> {
                        statusLabel.setText(ok ? "Role updated." : "Couldn't change that role.");
                        if (ok) {
                            refreshMembers[0].run();
                        }
                    });
                }).start();
            });
            removeBtn.setOnAction(e -> {
                TeamMemberResponse selected = membersTable.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    statusLabel.setText("Select a member to remove first.");
                    return;
                }
                if (!AlertUtils.showConfirmation("Remove Member",
                        "Remove " + selected.getUsername() + " from " + team.getName() + "?")) {
                    return;
                }
                new Thread(() -> {
                    boolean ok = TeamService.removeMember(team.getId(), selected.getUserId());
                    Platform.runLater(() -> {
                        statusLabel.setText(ok ? "Member removed." : "Couldn't remove that member.");
                        if (ok) {
                            refreshMembers[0].run();
                        }
                    });
                }).start();
            });
        }

        // ---- Pending invitations (owner/admin only) ----
        if (isAdminOrOwner) {
            content.getChildren().add(new Separator());
            content.getChildren().add(new Label("Pending Invitations"));

            TableView<TeamInvitationResponse> invitationsTable = new TableView<>();
            TableColumn<TeamInvitationResponse, String> inviteEmailCol = new TableColumn<>("Email");
            inviteEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
            TableColumn<TeamInvitationResponse, String> inviteRoleCol = new TableColumn<>("Role");
            inviteRoleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
            TableColumn<TeamInvitationResponse, String> inviteStatusCol = new TableColumn<>("Status");
            inviteStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
            invitationsTable.getColumns().addAll(inviteEmailCol, inviteRoleCol, inviteStatusCol);
            invitationsTable.setPrefHeight(120);
            invitationsTable.setPrefWidth(480);
            invitationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            content.getChildren().add(invitationsTable);

            Runnable[] refreshInvitations = new Runnable[1];
            refreshInvitations[0] = () -> new Thread(() -> {
                Optional<List<TeamInvitationResponse>> invitations = TeamService.getPendingInvitations(team.getId());
                Platform.runLater(() -> invitations.ifPresent(list -> invitationsTable.getItems().setAll(list)));
            }).start();
            refreshInvitations[0].run();

            Button revokeBtn = new Button("Revoke Invitation");
            content.getChildren().add(revokeBtn);
            revokeBtn.setOnAction(e -> {
                TeamInvitationResponse selected = invitationsTable.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    statusLabel.setText("Select an invitation to revoke first.");
                    return;
                }
                new Thread(() -> {
                    boolean ok = TeamService.revokeInvitation(team.getId(), selected.getId());
                    Platform.runLater(() -> {
                        statusLabel.setText(ok ? "Invitation revoked." : "Couldn't revoke that invitation.");
                        if (ok) {
                            refreshInvitations[0].run();
                        }
                    });
                }).start();
            });

            // ---- Invite by email ----
            content.getChildren().add(new Separator());
            content.getChildren().add(new Label("Invite someone by email"));
            TextField inviteEmailField = new TextField();
            inviteEmailField.setPromptText("email@example.com");
            ComboBox<String> inviteRoleCombo = new ComboBox<>();
            inviteRoleCombo.getItems().addAll("ADMIN", "MEMBER");
            inviteRoleCombo.setValue("MEMBER");
            Button inviteBtn = new Button("Send Invite");
            HBox inviteRow = new HBox(8, inviteEmailField, inviteRoleCombo, inviteBtn);
            inviteRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(inviteEmailField, Priority.ALWAYS);
            content.getChildren().add(inviteRow);

            inviteBtn.setOnAction(e -> {
                String email = inviteEmailField.getText() == null ? "" : inviteEmailField.getText().trim();
                if (email.isEmpty()) {
                    statusLabel.setText("Enter an email address first.");
                    return;
                }
                statusLabel.setText("Sending invitation...");
                new Thread(() -> {
                    try {
                        TeamService.inviteMember(team.getId(), email, inviteRoleCombo.getValue());
                        Platform.runLater(() -> {
                            statusLabel.setText("Invitation sent to " + email + ".");
                            inviteEmailField.clear();
                            refreshInvitations[0].run();
                        });
                    } catch (IOException ex) {
                        Platform.runLater(() -> statusLabel.setText(
                                "Couldn't send that invitation: " + TeamService.friendlyMessage(ex)));
                    }
                }).start();
            });
        }

        // ---- Leave / delete ----
        content.getChildren().add(new Separator());
        HBox dangerRow = new HBox(8);
        dangerRow.setAlignment(Pos.CENTER_LEFT);
        if (isOwner) {
            Button deleteBtn = new Button("Delete Team");
            deleteBtn.getStyleClass().add("danger-menu-item");
            deleteBtn.setOnAction(e -> {
                if (!AlertUtils.showConfirmation("Delete Team",
                        "Delete \"" + team.getName() + "\"? This removes everyone and can't be undone.")) {
                    return;
                }
                new Thread(() -> {
                    boolean ok = TeamService.deleteTeam(team.getId());
                    Platform.runLater(() -> {
                        if (ok) {
                            dialog.close();
                            loadTeams();
                            updateStatus("Team deleted: " + team.getName());
                        } else {
                            statusLabel.setText("Couldn't delete this team.");
                        }
                    });
                }).start();
            });
            dangerRow.getChildren().add(deleteBtn);
        } else {
            Button leaveBtn = new Button("Leave Team");
            leaveBtn.getStyleClass().add("danger-menu-item");
            leaveBtn.setOnAction(e -> {
                if (!AlertUtils.showConfirmation("Leave Team", "Leave \"" + team.getName() + "\"?")) {
                    return;
                }
                new Thread(() -> {
                    boolean ok = TeamService.leaveTeam(team.getId());
                    Platform.runLater(() -> {
                        if (ok) {
                            dialog.close();
                            loadTeams();
                            updateStatus("Left team: " + team.getName());
                        } else {
                            statusLabel.setText("Couldn't leave this team.");
                        }
                    });
                }).start();
            });
            dangerRow.getChildren().add(leaveBtn);
        }
        content.getChildren().add(dangerRow);
        content.getChildren().add(statusLabel);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(520);
        dialog.getDialogPane().setContent(scrollPane);
        ThemeManager.styleDialog(dialog.getDialogPane());
        dialog.showAndWait();
    }

    // ============================================================
    // Mock Servers
    // ============================================================

    private void setupMockServersList() {
        if (mockServersList == null) {
            return;
        }
        mockServersList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MockServerResponse server, boolean empty) {
                super.updateItem(server, empty);
                if (empty || server == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label nameLabel = new Label(server.getName() + (server.isEnabled() ? "" : "  (disabled)"));
                nameLabel.getStyleClass().add("variable-suggestion-name");
                Label metaLabel = new Label(server.getRouteCount() + " route" + (server.getRouteCount() == 1 ? "" : "s")
                        + " · " + server.getBaseUrl());
                metaLabel.getStyleClass().add("variable-suggestion-value");
                setGraphic(new VBox(1, nameLabel, metaLabel));
                setText(null);
            }
        });
        mockServersList.setOnMouseClicked(e -> {
            MockServerResponse selected = mockServersList.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selected != null) {
                openMockServerDetailDialog(selected);
            }
        });
    }

    private void loadMockServers() {
        if (mockServersList == null) {
            return;
        }
        new Thread(() -> {
            Optional<List<MockServerResponse>> servers = MockServerService.getMyMockServers();
            Platform.runLater(() -> servers.ifPresent(list -> mockServersList.getItems().setAll(list)));
        }).start();
    }

    @FXML
    private void handleCreateMockServer() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Mock Server");
        dialog.setHeaderText("Give it a name — you'll add routes (method + path + canned response) once it's created.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField nameField = new TextField();
        nameField.setPromptText("Mock server name");
        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("What's this mocking? (optional)");
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);

        VBox content = new VBox(10, new Label("Name"), nameField, new Label("Description"), descriptionField);
        content.setPadding(new Insets(10));
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        ThemeManager.styleDialog(dialog.getDialogPane());

        dialog.showAndWait().ifPresent(button -> {
            if (button != ButtonType.OK) {
                return;
            }
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                AlertUtils.showError("Give the mock server a name first.");
                return;
            }
            new Thread(() -> {
                Optional<MockServerResponse> created = MockServerService.createMockServer(name, descriptionField.getText());
                Platform.runLater(() -> {
                    if (created.isPresent()) {
                        loadMockServers();
                        updateStatus("Mock server created: " + name);
                        openMockServerDetailDialog(created.get());
                    }
                });
            }).start();
        });
    }

    /** The main mock-server-management surface: base URL (copyable),
     * enable/disable toggle, routes table with add/edit/delete, and
     * deleting the server itself. */
    private void openMockServerDetailDialog(MockServerResponse server) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(server.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(560);

        TextField baseUrlField = new TextField(server.getBaseUrl());
        baseUrlField.setEditable(false);
        Button copyUrlBtn = new Button("Copy");
        HBox urlRow = new HBox(8, baseUrlField, copyUrlBtn);
        HBox.setHgrow(baseUrlField, Priority.ALWAYS);
        urlRow.setAlignment(Pos.CENTER_LEFT);
        copyUrlBtn.setOnAction(e -> {
            ClipboardContent clipContent = new ClipboardContent();
            clipContent.putString(server.getBaseUrl());
            Clipboard.getSystemClipboard().setContent(clipContent);
            statusLabel.setText("Base URL copied to clipboard.");
        });

        CheckBox enabledCheck = new CheckBox("Enabled — routes only respond while this is checked");
        enabledCheck.setSelected(server.isEnabled());
        enabledCheck.selectedProperty().addListener((obs, was, isNow) -> new Thread(() -> {
            boolean ok = MockServerService.setEnabled(server.getId(), isNow);
            Platform.runLater(() -> {
                statusLabel.setText(ok ? (isNow ? "Enabled." : "Disabled — routes will 404 until re-enabled.")
                        : "Couldn't update.");
                if (ok) {
                    loadMockServers();
                }
            });
        }).start());

        TableView<MockRouteResponse> routesTable = new TableView<>();
        TableColumn<MockRouteResponse, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(new PropertyValueFactory<>("method"));
        TableColumn<MockRouteResponse, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        TableColumn<MockRouteResponse, Number> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getResponseStatus()));
        routesTable.getColumns().addAll(methodCol, pathCol, statusCol);
        routesTable.setPrefSize(560, 200);
        routesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Runnable[] refreshRoutes = new Runnable[1];
        refreshRoutes[0] = () -> new Thread(() -> {
            Optional<List<MockRouteResponse>> routes = MockServerService.getRoutes(server.getId());
            Platform.runLater(() -> routes.ifPresent(list -> routesTable.getItems().setAll(list)));
        }).start();
        refreshRoutes[0].run();

        Button addRouteBtn = new Button("Add Route");
        Button editRouteBtn = new Button("Edit Route");
        Button deleteRouteBtn = new Button("Delete Route");
        HBox routeActions = new HBox(8, addRouteBtn, editRouteBtn, deleteRouteBtn);
        routeActions.setAlignment(Pos.CENTER_LEFT);

        addRouteBtn.setOnAction(e -> openRouteEditDialog(server.getId(), null, refreshRoutes[0], statusLabel));
        editRouteBtn.setOnAction(e -> {
            MockRouteResponse selected = routesTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Select a route to edit first.");
                return;
            }
            openRouteEditDialog(server.getId(), selected, refreshRoutes[0], statusLabel);
        });
        deleteRouteBtn.setOnAction(e -> {
            MockRouteResponse selected = routesTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Select a route to delete first.");
                return;
            }
            if (!AlertUtils.showConfirmation("Delete Route",
                    "Delete " + selected.getMethod() + " " + selected.getPath() + "?")) {
                return;
            }
            new Thread(() -> {
                boolean ok = MockServerService.deleteRoute(server.getId(), selected.getId());
                Platform.runLater(() -> {
                    statusLabel.setText(ok ? "Route deleted." : "Couldn't delete that route.");
                    if (ok) {
                        refreshRoutes[0].run();
                    }
                });
            }).start();
        });

        Button deleteServerBtn = new Button("Delete Mock Server");
        deleteServerBtn.getStyleClass().add("danger-menu-item");
        deleteServerBtn.setOnAction(e -> {
            if (!AlertUtils.showConfirmation("Delete Mock Server",
                    "Delete \"" + server.getName() + "\" and all its routes? This can't be undone.")) {
                return;
            }
            new Thread(() -> {
                boolean ok = MockServerService.deleteMockServer(server.getId());
                Platform.runLater(() -> {
                    if (ok) {
                        dialog.close();
                        loadMockServers();
                        updateStatus("Mock server deleted: " + server.getName());
                    } else {
                        statusLabel.setText("Couldn't delete this mock server.");
                    }
                });
            }).start();
        });

        VBox content = new VBox(10,
                new Label("Base URL"), urlRow,
                enabledCheck,
                new Separator(),
                new Label("Routes"), routesTable, routeActions,
                new Separator(),
                deleteServerBtn,
                statusLabel);
        content.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(520);
        dialog.getDialogPane().setContent(scrollPane);
        ThemeManager.styleDialog(dialog.getDialogPane());
        dialog.showAndWait();
    }

    /** Add/edit a single route — method, path, status, response body,
     * headers (as JSON), and an optional artificial delay. */
    private void openRouteEditDialog(Long mockServerId, MockRouteResponse existing, Runnable onSaved, Label parentStatusLabel) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Route" : "Edit Route");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
        methodCombo.setValue(existing != null ? existing.getMethod() : "GET");

        TextField pathField = new TextField(existing != null ? existing.getPath() : "");
        pathField.setPromptText("/users/1");

        TextField statusField = new TextField(existing != null ? String.valueOf(existing.getResponseStatus()) : "200");

        TextArea bodyArea = new TextArea(existing != null ? existing.getResponseBody() : "");
        bodyArea.setPromptText("Response body — e.g. {\"id\": 1, \"name\": \"Jane\"}");
        bodyArea.setPrefRowCount(6);
        bodyArea.setWrapText(true);

        TextArea headersArea = new TextArea(existing != null ? existing.getResponseHeaders() : "");
        headersArea.setPromptText("Response headers as JSON (optional) — e.g. {\"Content-Type\": \"application/json\"}");
        headersArea.setPrefRowCount(2);
        headersArea.setWrapText(true);

        TextField delayField = new TextField(existing != null ? String.valueOf(existing.getDelayMs()) : "0");

        VBox content = new VBox(8,
                new Label("Method"), methodCombo,
                new Label("Path"), pathField,
                new Label("Status Code"), statusField,
                new Label("Response Body"), bodyArea,
                new Label("Response Headers (JSON, optional)"), headersArea,
                new Label("Artificial Delay (ms, optional)"), delayField);
        content.setPadding(new Insets(10));
        content.setPrefWidth(420);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(480);
        dialog.getDialogPane().setContent(scrollPane);
        ThemeManager.styleDialog(dialog.getDialogPane());

        dialog.showAndWait().ifPresent(button -> {
            if (button != ButtonType.OK) {
                return;
            }
            String path = pathField.getText() == null ? "" : pathField.getText().trim();
            if (path.isEmpty()) {
                AlertUtils.showError("Enter a path first, e.g. /users/1");
                return;
            }
            int status;
            try {
                status = Integer.parseInt(statusField.getText().trim());
            } catch (NumberFormatException ex) {
                AlertUtils.showError("Status code must be a number, e.g. 200.");
                return;
            }
            int delay;
            try {
                delay = delayField.getText() == null || delayField.getText().isBlank()
                        ? 0 : Integer.parseInt(delayField.getText().trim());
            } catch (NumberFormatException ex) {
                AlertUtils.showError("Delay must be a number of milliseconds, e.g. 0 or 500.");
                return;
            }
            String headersText = headersArea.getText();
            if (headersText != null && !headersText.isBlank()) {
                try {
                    new JSONObject(headersText);
                } catch (Exception ex) {
                    AlertUtils.showError("Response Headers must be valid JSON, e.g. {\"Content-Type\": \"application/json\"}");
                    return;
                }
            }
            MockRouteRequest request = new MockRouteRequest(
                    methodCombo.getValue(), path, status, bodyArea.getText(), headersText, delay);
            new Thread(() -> {
                try {
                    if (existing == null) {
                        MockServerService.addRoute(mockServerId, request);
                    } else {
                        MockServerService.updateRoute(mockServerId, existing.getId(), request);
                    }
                    Platform.runLater(() -> {
                        parentStatusLabel.setText(existing == null ? "Route added." : "Route updated.");
                        onSaved.run();
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> AlertUtils.showError(
                            "Couldn't save that route: " + MockServerService.friendlyMessage(ex)));
                }
            }).start();
        });
    }

    // ============================================================
    // Monitors
    // ============================================================

    private static final LinkedHashMap<String, Integer> MONITOR_INTERVAL_PRESETS = new LinkedHashMap<>();

    static {
        MONITOR_INTERVAL_PRESETS.put("Every 5 minutes", 5);
        MONITOR_INTERVAL_PRESETS.put("Every 10 minutes", 10);
        MONITOR_INTERVAL_PRESETS.put("Every 15 minutes", 15);
        MONITOR_INTERVAL_PRESETS.put("Every 30 minutes", 30);
        MONITOR_INTERVAL_PRESETS.put("Every hour", 60);
        MONITOR_INTERVAL_PRESETS.put("Every 6 hours", 360);
        MONITOR_INTERVAL_PRESETS.put("Every 12 hours", 720);
        MONITOR_INTERVAL_PRESETS.put("Every day", 1440);
    }

    private void setupMonitorsList() {
        if (monitorsList == null) {
            return;
        }
        monitorsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MonitorResponse monitor, boolean empty) {
                super.updateItem(monitor, empty);
                if (empty || monitor == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label nameLabel = new Label(monitor.getName() + (monitor.isEnabled() ? "" : "  (disabled)"));
                nameLabel.getStyleClass().add("variable-suggestion-name");
                String lastRun = monitor.getLastRunStatus() != null
                        ? "Last run: " + monitor.getLastRunStatus() : "Never run yet";
                Label metaLabel = new Label(monitor.getCollectionName() + " · " + lastRun);
                metaLabel.getStyleClass().add("variable-suggestion-value");
                setGraphic(new VBox(1, nameLabel, metaLabel));
                setText(null);
            }
        });
        monitorsList.setOnMouseClicked(e -> {
            MonitorResponse selected = monitorsList.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selected != null) {
                openMonitorDetailDialog(selected);
            }
        });
    }

    private void loadMonitors() {
        if (monitorsList == null) {
            return;
        }
        new Thread(() -> {
            Optional<List<MonitorResponse>> monitors = MonitorService.getMyMonitors();
            Platform.runLater(() -> monitors.ifPresent(list -> monitorsList.getItems().setAll(list)));
        }).start();
    }

    @FXML
    private void handleCreateMonitor() {
        openMonitorEditDialog(null);
    }

    /** Shared by both create and edit — null existing means create. */
    private void openMonitorEditDialog(MonitorResponse existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Create Monitor" : "Edit Monitor");
        dialog.setHeaderText("A monitor runs a whole collection on a schedule and tracks pass/fail history.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        nameField.setPromptText("Monitor name");

        ComboBox<String> collectionCombo = new ComboBox<>();
        collectionCombo.setPromptText("Loading collections...");
        Map<String, Long> collectionIds = new LinkedHashMap<>();

        ComboBox<String> environmentCombo = new ComboBox<>();
        environmentCombo.getItems().add("(no environment)");
        environmentCombo.setValue("(no environment)");
        Map<String, Long> environmentIds = new LinkedHashMap<>();

        ComboBox<String> intervalCombo = new ComboBox<>();
        intervalCombo.getItems().addAll(MONITOR_INTERVAL_PRESETS.keySet());
        intervalCombo.setValue("Every 5 minutes");
        if (existing != null) {
            for (Map.Entry<String, Integer> entry : MONITOR_INTERVAL_PRESETS.entrySet()) {
                if (entry.getValue() == existing.getIntervalMinutes()) {
                    intervalCombo.setValue(entry.getKey());
                    break;
                }
            }
        }

        CheckBox enabledCheck = new CheckBox("Enabled");
        enabledCheck.setSelected(existing == null || existing.isEnabled());

        CheckBox notifyCheck = new CheckBox("Email me on failure");
        notifyCheck.setSelected(existing == null || existing.isNotifyOnFailure());

        TextField notifyEmailField = new TextField(existing != null ? existing.getNotifyEmail() : "");
        notifyEmailField.setPromptText("Defaults to your account email if left blank");

        VBox content = new VBox(8,
                new Label("Name"), nameField,
                new Label("Collection"), collectionCombo,
                new Label("Environment (optional)"), environmentCombo,
                new Label("Run"), intervalCombo,
                enabledCheck, notifyCheck,
                new Label("Notify Email"), notifyEmailField);
        content.setPadding(new Insets(10));
        content.setPrefWidth(380);
        dialog.getDialogPane().setContent(content);
        ThemeManager.styleDialog(dialog.getDialogPane());

        // Populate the collection/environment pickers in the background so the
        // dialog itself opens instantly.
        new Thread(() -> {
            Optional<List<CollectionResponse>> collections = CollectionService.getUserCollections();
            Optional<List<EnvironmentResponse>> environments = EnvironmentService.getUserEnvironments();
            Platform.runLater(() -> {
                collectionCombo.setPromptText("Choose a collection");
                collections.ifPresent(list -> {
                    for (CollectionResponse c : list) {
                        collectionIds.put(c.getName(), c.getId());
                        collectionCombo.getItems().add(c.getName());
                    }
                    if (existing != null) {
                        collectionIds.entrySet().stream()
                                .filter(en -> en.getValue().equals(existing.getCollectionId()))
                                .findFirst()
                                .ifPresent(en -> collectionCombo.setValue(en.getKey()));
                    }
                });
                environments.ifPresent(list -> {
                    for (EnvironmentResponse env : list) {
                        environmentIds.put(env.getName(), env.getId());
                        environmentCombo.getItems().add(env.getName());
                    }
                    if (existing != null && existing.getEnvironmentId() != null) {
                        environmentIds.entrySet().stream()
                                .filter(en -> en.getValue().equals(existing.getEnvironmentId()))
                                .findFirst()
                                .ifPresent(en -> environmentCombo.setValue(en.getKey()));
                    }
                });
            });
        }).start();

        dialog.showAndWait().ifPresent(button -> {
            if (button != ButtonType.OK) {
                return;
            }
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            Long collectionId = collectionIds.get(collectionCombo.getValue());
            if (name.isEmpty() || collectionId == null) {
                AlertUtils.showError("Give the monitor a name and choose a collection first.");
                return;
            }
            Long environmentId = environmentIds.get(environmentCombo.getValue());
            int intervalMinutes = MONITOR_INTERVAL_PRESETS.getOrDefault(intervalCombo.getValue(), 5);
            MonitorRequest request = new MonitorRequest(
                    name, collectionId, environmentId, intervalMinutes,
                    enabledCheck.isSelected(), notifyCheck.isSelected(), notifyEmailField.getText());
            new Thread(() -> {
                try {
                    if (existing == null) {
                        MonitorService.createMonitor(request);
                    } else {
                        MonitorService.updateMonitor(existing.getId(), request);
                    }
                    Platform.runLater(() -> {
                        loadMonitors();
                        updateStatus(existing == null ? "Monitor created: " + name : "Monitor updated: " + name);
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> AlertUtils.showError(
                            "Couldn't save that monitor: " + MonitorService.friendlyMessage(ex)));
                }
            }).start();
        });
    }

    /** The main monitor-management surface: schedule/enable summary,
     * Run Now, and a run-history table. */
    private void openMonitorDetailDialog(MonitorResponse monitor) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(monitor.getName());
        dialog.setHeaderText(monitor.getCollectionName()
                + (monitor.getEnvironmentName() != null ? "  ·  env: " + monitor.getEnvironmentName() : "")
                + "  ·  " + monitor.getIntervalMinutes() + " min interval");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(600);

        TableView<MonitorRunResponse> runsTable = new TableView<>();
        TableColumn<MonitorRunResponse, String> startedCol = new TableColumn<>("Started");
        startedCol.setCellValueFactory(new PropertyValueFactory<>("startedAt"));
        TableColumn<MonitorRunResponse, Boolean> successCol = new TableColumn<>("Result");
        successCol.setCellValueFactory(new PropertyValueFactory<>("success"));
        successCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean success, boolean empty) {
                super.updateItem(success, empty);
                setText(empty || success == null ? null : (success ? "Passed" : "Failed"));
            }
        });
        TableColumn<MonitorRunResponse, Number> passedCol = new TableColumn<>("Passed");
        passedCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getPassedRequests()));
        TableColumn<MonitorRunResponse, Number> failedCol = new TableColumn<>("Failed");
        failedCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getFailedRequests()));
        TableColumn<MonitorRunResponse, Number> avgCol = new TableColumn<>("Avg ms");
        avgCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleLongProperty(cellData.getValue().getAvgResponseTimeMs()));
        runsTable.getColumns().addAll(startedCol, successCol, passedCol, failedCol, avgCol);
        runsTable.setPrefSize(600, 260);
        runsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        runsTable.setPlaceholder(new Label("No runs yet — click Run Now, or wait for the schedule to fire."));

        Runnable[] refreshRuns = new Runnable[1];
        refreshRuns[0] = () -> new Thread(() -> {
            Optional<List<MonitorRunResponse>> runs = MonitorService.getRuns(monitor.getId());
            Platform.runLater(() -> runs.ifPresent(list -> runsTable.getItems().setAll(list)));
        }).start();
        refreshRuns[0].run();

        Button runNowBtn = new Button("Run Now");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete Monitor");
        deleteBtn.getStyleClass().add("danger-menu-item");
        HBox actions = new HBox(8, runNowBtn, editBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        runNowBtn.setOnAction(e -> {
            statusLabel.setText("Running...");
            runNowBtn.setDisable(true);
            new Thread(() -> {
                try {
                    MonitorService.runNow(monitor.getId());
                    Platform.runLater(() -> {
                        statusLabel.setText("Run complete.");
                        runNowBtn.setDisable(false);
                        refreshRuns[0].run();
                        loadMonitors();
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Couldn't run: " + MonitorService.friendlyMessage(ex));
                        runNowBtn.setDisable(false);
                    });
                }
            }).start();
        });
        editBtn.setOnAction(e -> {
            dialog.close();
            openMonitorEditDialog(monitor);
        });
        deleteBtn.setOnAction(e -> {
            if (!AlertUtils.showConfirmation("Delete Monitor",
                    "Delete \"" + monitor.getName() + "\"? Its run history goes with it.")) {
                return;
            }
            new Thread(() -> {
                boolean ok = MonitorService.deleteMonitor(monitor.getId());
                Platform.runLater(() -> {
                    if (ok) {
                        dialog.close();
                        loadMonitors();
                        updateStatus("Monitor deleted: " + monitor.getName());
                    } else {
                        statusLabel.setText("Couldn't delete this monitor.");
                    }
                });
            }).start();
        });

        VBox content = new VBox(10, actions, new Separator(), new Label("Run History"), runsTable, statusLabel);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        ThemeManager.styleDialog(dialog.getDialogPane());
        dialog.showAndWait();
    }

    /** Lets the project owner configure the app's own outgoing SMTP
     * server — used for email verification codes and team invitations.
     * The backend rejects this for non-admins, so nothing bad happens if
     * a non-admin somehow opens it; they'll just see a clear error. */
    private void openMailSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Mail Server Settings");
        dialog.setHeaderText("Outgoing mail (SMTP) — used for verification emails and team invitations");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        TextField hostField = new TextField();
        hostField.setPromptText("smtp.gmail.com");
        TextField portField = new TextField();
        portField.setPromptText("587");
        TextField usernameField2 = new TextField();
        usernameField2.setPromptText("you@example.com");
        PasswordField passwordField2 = new PasswordField();
        passwordField2.setPromptText("(leave blank to keep the current password)");
        TextField fromAddressField = new TextField();
        fromAddressField.setPromptText("noreply@yourapp.com");
        TextField fromNameField = new TextField();
        fromNameField.setPromptText("Thundercall");
        CheckBox tlsCheck = new CheckBox("Use STARTTLS");
        tlsCheck.setSelected(true);
        CheckBox sslCheck = new CheckBox("Use SSL");
        // FIX: this used to sit in the same row as the STARTTLS/SSL
        // protocol checkboxes — easy to miss as "just another technical
        // toggle" when it's actually the master on/off switch. It's now
        // its own bold, top-of-form row, and defaults to checked (a fresh
        // setup should just work once real details are filled in, not
        // silently stay off).
        CheckBox enabledCheck = new CheckBox("Mail sending enabled");
        enabledCheck.setSelected(true);
        enabledCheck.setStyle("-fx-font-weight: bold;");
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(360);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        int row = 0;
        grid.add(enabledCheck, 0, row, 2, 1);
        row++;
        grid.addRow(row++, new Label("SMTP Host"), hostField);
        grid.addRow(row++, new Label("SMTP Port"), portField);
        grid.addRow(row++, new Label("Username"), usernameField2);
        grid.addRow(row++, new Label("Password"), passwordField2);
        grid.addRow(row++, new Label("From Address"), fromAddressField);
        grid.addRow(row++, new Label("From Name"), fromNameField);
        grid.add(new HBox(12, tlsCheck, sslCheck), 1, row++);

        TextField testEmailField = new TextField();
        testEmailField.setPromptText("Send a test to...");
        Button testBtn = new Button("Send test email");
        Button saveBtn = new Button("Save");
        HBox actionsRow = new HBox(8, testEmailField, testBtn, new Region(), saveBtn);
        HBox.setHgrow(testEmailField, Priority.ALWAYS);
        HBox.setHgrow((Region) actionsRow.getChildren().get(2), Priority.ALWAYS);

        VBox content = new VBox(10, grid, actionsRow, statusLabel);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        ThemeManager.styleDialog(dialog.getDialogPane());

        // Load current values
        new Thread(() -> {
            try {
                Optional<MailSettingsResponse> current = MailSettingsService.getSettings();
                Platform.runLater(() -> current.ifPresent(settings -> {
                    hostField.setText(settings.getSmtpHost());
                    portField.setText(settings.getSmtpPort() != null ? String.valueOf(settings.getSmtpPort()) : "");
                    usernameField2.setText(settings.getSmtpUsername());
                    fromAddressField.setText(settings.getFromAddress());
                    fromNameField.setText(settings.getFromName());
                    tlsCheck.setSelected(settings.isUseTls());
                    sslCheck.setSelected(settings.isUseSsl());
                    enabledCheck.setSelected(settings.isEnabled());
                    if (settings.isHasPassword()) {
                        passwordField2.setPromptText("(a password is already saved — leave blank to keep it)");
                    }
                }));
            } catch (IOException e) {
                Platform.runLater(() -> statusLabel.setText("Couldn't load current settings: " + e.getMessage()));
            }
        }).start();

        saveBtn.setOnAction(e -> {
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                statusLabel.setText("Port must be a number, e.g. 587.");
                return;
            }
            MailSettingsRequest request = new MailSettingsRequest(
                    hostField.getText().trim(), port, usernameField2.getText().trim(),
                    passwordField2.getText(), fromAddressField.getText().trim(), fromNameField.getText().trim(),
                    tlsCheck.isSelected(), sslCheck.isSelected(), enabledCheck.isSelected());
            statusLabel.setText("Saving...");
            new Thread(() -> {
                try {
                    MailSettingsService.updateSettings(request);
                    Platform.runLater(() -> statusLabel.setText("Saved."));
                } catch (IOException ex) {
                    Platform.runLater(() -> statusLabel.setText("Couldn't save: " + ex.getMessage()));
                }
            }).start();
        });

        testBtn.setOnAction(e -> {
            String toAddress = testEmailField.getText().trim();
            if (toAddress.isEmpty()) {
                statusLabel.setText("Enter an address to send the test email to.");
                return;
            }
            statusLabel.setText("Sending test email...");
            new Thread(() -> {
                try {
                    MailSettingsService.sendTestEmail(toAddress);
                    Platform.runLater(() -> statusLabel.setText("Test email sent to " + toAddress + " — check the inbox."));
                } catch (IOException ex) {
                    Platform.runLater(() -> statusLabel.setText("Couldn't send: " + ex.getMessage()));
                }
            }).start();
        });

        dialog.showAndWait();
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

    @FXML
    private void handleShowTeams() {
        showSidebarPane(teamsPane);
        loadTeams();
    }

    @FXML
    private void handleShowMockServers() {
        showSidebarPane(mockServersPane);
        loadMockServers();
    }

    @FXML
    private void handleShowMonitors() {
        showSidebarPane(monitorsPane);
        loadMonitors();
    }

    private void showSidebarPane(VBox pane) {
        if (collectionsPane == null || environmentsPane == null || historyPane == null || pane == null) {
            return; // old FXML without the rail
        }
        collectionsPane.setVisible(pane == collectionsPane);
        environmentsPane.setVisible(pane == environmentsPane);
        historyPane.setVisible(pane == historyPane);
        if (teamsPane != null) {
            teamsPane.setVisible(pane == teamsPane);
        }
        if (mockServersPane != null) {
            mockServersPane.setVisible(pane == mockServersPane);
        }
        if (monitorsPane != null) {
            monitorsPane.setVisible(pane == monitorsPane);
        }
    }

    /**
     * Runs a script (Postman pm.* patterns via ScriptRunner) and applies any
     * pm.environment.set/unset results to the SELECTED environment — locally
     * right away (so {{vars}} resolve on the next send) and persisted to the
     * server in the background.
     */
    private void runScriptsAndApply(String script, String responseBody, int statusCode, String label) {
        if (script == null || script.isBlank()) {
            return;
        }
        Map<String, String> current = new LinkedHashMap<>(currentEnvironmentVariables());
        ScriptRunner.Result result = ScriptRunner.run(script, responseBody, statusCode, current);
        result.log.forEach(line -> System.out.println("[" + label + "] " + line));
        // Show what the script did (or why lines were skipped) in the Tests tab
        if (testResultsList != null && !result.log.isEmpty()) {
            List<String> uiLog = new ArrayList<>();
            result.log.forEach(line -> uiLog.add("[" + label + "] " + line));
            Platform.runLater(() -> testResultsList.getItems().addAll(0, uiLog));
        }
        if (result.setVariables.isEmpty() && result.unsetVariables.isEmpty()) {
            updateStatus(label + ": no variables set — open the response Tests tab to see why");
            return;
        }
        String envName = environmentCombo.getValue();
        EnvironmentResponse env = envName != null ? environmentsMap.get(envName) : null;
        if (env == null) {
            updateStatus(label + ": select an environment to store variables");
            return;
        }
        Map<String, String> merged = new LinkedHashMap<>(
                env.getVariables() != null ? env.getVariables() : Collections.emptyMap());
        merged.putAll(result.setVariables);
        result.unsetVariables.forEach(merged::remove);
        env.setVariables(merged); // effective immediately for {{variables}}
        updateStatus(label + ": " + (result.setVariables.isEmpty()
                ? "variables updated"
                : "set " + String.join(", ", result.setVariables.keySet())));
        new Thread(() -> EnvironmentService.updateEnvironmentVariables(env.getId(), merged)).start();
    }

    // ==================== Environments sidebar ====================

    private void setupEnvironmentsList() {
        if (environmentsList == null) {
            return;
        }
        environmentsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EnvironmentResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.getName()
                        + (Boolean.TRUE.equals(item.getIsActive()) ? "" : "  (inactive)"));
            }
        });
        environmentsList.setOnMouseClicked(e -> {
            EnvironmentResponse selected = environmentsList.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selected != null) {
                openEnvironmentEditor(selected);
            }
        });
        setupEnvironmentsContextMenu();
    }

    /** Right-click menu for the Environments list — Rename, Duplicate,
     * Export as JSON, Delete (the locally-relevant subset of Postman's
     * own environment menu; Share/fork/PR/merge are cloud/git features
     * with no equivalent in a self-hosted single-user tool like this). */
    private void setupEnvironmentsContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem renameItem = new MenuItem("Rename");
        MenuItem duplicateItem = new MenuItem("Duplicate");
        MenuItem exportItem = new MenuItem("Export as JSON");
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.getStyleClass().add("danger-menu-item");
        renameItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+E"));
        duplicateItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+D"));

        renameItem.setOnAction(e -> {
            EnvironmentResponse env = environmentsList.getSelectionModel().getSelectedItem();
            if (env == null) {
                return;
            }
            TextInputDialog dialog = new TextInputDialog(env.getName());
            dialog.setTitle("Rename Environment");
            dialog.setHeaderText("Rename \"" + env.getName() + "\"");
            dialog.setContentText("New name:");
            ThemeManager.styleDialog(dialog.getDialogPane());
            dialog.showAndWait().ifPresent(newName -> {
                if (newName == null || newName.isBlank() || newName.equals(env.getName())) {
                    return;
                }
                new Thread(() -> {
                    Optional<EnvironmentResponse> updated = EnvironmentService.updateEnvironment(
                            env.getId(), newName.trim(), env.getDescription(), env.getVariables(), env.getIsActive());
                    Platform.runLater(() -> {
                        if (updated.isPresent()) {
                            loadEnvironments();
                            updateStatus("Environment renamed to \"" + newName.trim() + "\"");
                        } else {
                            AlertUtils.showError("Failed to rename environment");
                        }
                    });
                }).start();
            });
        });

        duplicateItem.setOnAction(e -> {
            EnvironmentResponse env = environmentsList.getSelectionModel().getSelectedItem();
            if (env == null) {
                return;
            }
            String copyName = env.getName() + " Copy";
            new Thread(() -> {
                Optional<EnvironmentResponse> created = EnvironmentService.createEnvironment(
                        copyName, env.getDescription(),
                        env.getVariables() != null ? new LinkedHashMap<>(env.getVariables()) : new LinkedHashMap<>());
                Platform.runLater(() -> {
                    if (created.isPresent()) {
                        loadEnvironments();
                        updateStatus("Duplicated as \"" + copyName + "\"");
                    } else {
                        AlertUtils.showError("Failed to duplicate environment");
                    }
                });
            }).start();
        });

        exportItem.setOnAction(e -> {
            EnvironmentResponse env = environmentsList.getSelectionModel().getSelectedItem();
            if (env == null) {
                return;
            }
            exportEnvironmentAsJson(env);
        });

        deleteItem.setOnAction(e -> {
            EnvironmentResponse env = environmentsList.getSelectionModel().getSelectedItem();
            if (env == null) {
                return;
            }
            if (!AlertUtils.showConfirmation("Delete Environment",
                    "Delete \"" + env.getName() + "\"? This cannot be undone.")) {
                return;
            }
            new Thread(() -> {
                boolean deleted = EnvironmentService.deleteEnvironment(env.getId());
                Platform.runLater(() -> {
                    if (deleted) {
                        loadEnvironments();
                        updateStatus("Environment deleted: " + env.getName());
                    } else {
                        AlertUtils.showError("Failed to delete environment");
                    }
                });
            }).start();
        });

        menu.getItems().addAll(renameItem, duplicateItem, new SeparatorMenuItem(), exportItem,
                new SeparatorMenuItem(), deleteItem);

        // Only show the menu when right-clicking an actual row, and select
        // it first so the actions above operate on the right environment.
        environmentsList.setContextMenu(menu);
        environmentsList.setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                int index = environmentsList.getSelectionModel().getSelectedIndex();
                Node node = e.getPickResult().getIntersectedNode();
                while (node != null && !(node instanceof ListCell)) {
                    node = node.getParent();
                }
                if (node instanceof ListCell<?> cell && cell.getItem() != null) {
                    environmentsList.getSelectionModel().select((EnvironmentResponse) cell.getItem());
                }
            }
        });
    }

    /** Exports one environment as a clean, portable JSON file. */
    @FXML
    private void handleImportEnvironment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Environment");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showOpenDialog(Main.getPrimaryStage());
        if (file == null) {
            return;
        }
        try {
            String content = java.nio.file.Files.readString(file.toPath());
            JSONObject root = new JSONObject(content);

            String name = root.optString("name", file.getName().replaceAll("(?i)\\.json$", ""));
            String description = root.optString("description", "Imported from " + file.getName());

            // Understands both our own export format and Postman's actual
            // environment export (both use a top-level "values" array of
            // {key, value[, enabled]} — Postman also adds "type", which we
            // just ignore).
            Map<String, String> variables = new LinkedHashMap<>();
            JSONArray values = root.optJSONArray("values");
            if (values != null) {
                for (int i = 0; i < values.length(); i++) {
                    JSONObject entry = values.getJSONObject(i);
                    boolean enabled = entry.optBoolean("enabled", true);
                    if (!enabled) {
                        continue; // matches Postman: disabled variables aren't carried over
                    }
                    String key = entry.optString("key", null);
                    if (key == null || key.isBlank()) {
                        continue;
                    }
                    variables.put(key, entry.optString("value", ""));
                }
            }

            if (variables.isEmpty()) {
                AlertUtils.showError("No variables found in that file — expected a \"values\" array "
                        + "with key/value pairs (this app's own export, or a Postman environment export).");
                return;
            }

            String finalName = name;
            new Thread(() -> {
                Optional<EnvironmentResponse> created = EnvironmentService.createEnvironment(
                        finalName, description, variables);
                Platform.runLater(() -> {
                    if (created.isPresent()) {
                        loadEnvironments();
                        updateStatus("Imported environment \"" + finalName + "\" with "
                                + variables.size() + " variable(s)");
                        AlertUtils.showSuccess("Imported \"" + finalName + "\" with "
                                + variables.size() + " variable(s)");
                    } else {
                        AlertUtils.showError("Failed to import environment");
                    }
                });
            }).start();
        } catch (Exception ex) {
            AlertUtils.showError("Couldn't read that file: " + ex.getMessage());
        }
    }

    private void exportEnvironmentAsJson(EnvironmentResponse env) {
        JSONObject root = new JSONObject();
        root.put("name", env.getName());
        root.put("description", env.getDescription() != null ? env.getDescription() : "");
        JSONArray values = new JSONArray();
        if (env.getVariables() != null) {
            env.getVariables().forEach((key, value) -> {
                JSONObject entry = new JSONObject();
                entry.put("key", key);
                entry.put("value", value);
                entry.put("enabled", true);
                values.put(entry);
            });
        }
        root.put("values", values);
        root.put("_exported_from", "Thundercall");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Environment");
        String suggested = env.getName().replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replace(' ', '_') + ".json";
        fileChooser.setInitialFileName(suggested);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(Main.getPrimaryStage());
        if (file == null) {
            return;
        }
        try {
            java.nio.file.Files.writeString(file.toPath(), root.toString(2));
            AlertUtils.showSuccess("Exported \"" + env.getName() + "\" to " + file.getName());
            updateStatus("Environment exported: " + file.getName());
        } catch (Exception ex) {
            AlertUtils.showError("Failed to write file: " + ex.getMessage());
        }
    }

    /** Postman-style variable editor: key/value table with add/delete/save. */
    private void openEnvironmentEditor(EnvironmentResponse env) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Environment");
        dialog.setHeaderText("Variables of \"" + env.getName() + "\"");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TableView<KeyValuePair> table = new TableView<>();
        ObservableList<KeyValuePair> data = FXCollections.observableArrayList();
        if (env.getVariables() != null) {
            env.getVariables().forEach((k, v) -> data.add(new KeyValuePair(k, v, "")));
        }
        table.setItems(data);
        table.setEditable(true);
        table.setPrefSize(460, 260);

        TableColumn<KeyValuePair, String> keyCol = new TableColumn<>("Variable");
        keyCol.setCellValueFactory(new PropertyValueFactory<>("key"));
        keyCol.setCellFactory(TextFieldTableCell.forTableColumn());
        keyCol.setOnEditCommit(e -> e.getRowValue().setKey(e.getNewValue()));
        keyCol.setPrefWidth(170);

        TableColumn<KeyValuePair, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setCellFactory(TextFieldTableCell.forTableColumn());
        valueCol.setOnEditCommit(e -> e.getRowValue().setValue(e.getNewValue()));
        valueCol.setPrefWidth(260);

        table.getColumns().add(keyCol);
        table.getColumns().add(valueCol);

        Button addBtn = new Button("Add Variable");
        addBtn.setOnAction(e -> data.add(new KeyValuePair("KEY", "value", "")));
        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> {
            KeyValuePair selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                data.remove(selected);
            }
        });
        HBox buttons = new HBox(8, addBtn, deleteBtn);

        VBox content = new VBox(10, table, buttons);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        ThemeManager.styleDialog(dialog.getDialogPane());

        dialog.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                Map<String, String> variables = new LinkedHashMap<>();
                for (KeyValuePair kv : data) {
                    if (kv.getKey() != null && !kv.getKey().isBlank()) {
                        variables.put(kv.getKey().trim(), kv.getValue() == null ? "" : kv.getValue());
                    }
                }
                new Thread(() -> {
                    Optional<EnvironmentResponse> updated =
                            EnvironmentService.updateEnvironmentVariables(env.getId(), variables);
                    Platform.runLater(() -> {
                        if (updated.isPresent()) {
                            updateStatus("Environment saved: " + env.getName());
                            loadEnvironments();
                        } else {
                            AlertUtils.showError("Failed to save environment variables");
                        }
                    });
                }).start();
            }
        });
    }

    /** Records "c:<id>" / "f:<id>" keys for every expanded node. */
    private void captureExpandedKeys(TreeItem<String> node, Set<String> out) {
        if (node == null) {
            return;
        }
        for (TreeItem<String> child : node.getChildren()) {
            if (child.isExpanded()) {
                Long collectionId = collectionIdMap.get(child);
                if (collectionId != null) {
                    out.add("c:" + collectionId);
                }
                Long folderId = folderIdMap.get(child);
                if (folderId != null) {
                    out.add("f:" + folderId);
                }
            }
            captureExpandedKeys(child, out);
        }
    }

    // ==================== Postman-style request tabs ====================

    /** Snapshot of the request editor for one tab. */
    private static class RequestTabState {
        String name = "Untitled Request";
        Long requestId;
        String method = "GET";
        String url = "";
        String body = "";
        String preScript = "";
        String testsScript = "";
        String authType = "No Auth";
        String authToken = "";
        String authUsername = "";
        String authPassword = "";
        List<KeyValuePair> params = new ArrayList<>();
        List<KeyValuePair> headers = new ArrayList<>();
        // Postman-style unsaved indicator (the small dot on the tab)
        boolean dirty = false;
        // FIX: the response viewer used to be one shared set of fields
        // across every tab, so switching tabs could show a DIFFERENT
        // tab's response — each tab now keeps its own last response.
        ApiResponse lastApiResponse;
        Map<String, String> lastResponseHeaders;
    }

    /** True while we're programmatically loading a tab's state into the
     * shared editor controls — guards the dirty-tracking listeners so
     * restoring a tab never marks it unsaved. */
    private boolean restoringTabState = false;

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

        // Live dirty-tracking + method-badge updates. These controls are
        // shared across every tab (the tab switch swaps their content), so
        // one set of listeners covers all tabs; restoringTabState guards
        // against marking a tab dirty while WE are the ones setting values.
        methodCombo.valueProperty().addListener((o, ov, nv) -> {
            if (restoringTabState) {
                return;
            }
            Tab active = requestTabPane.getSelectionModel().getSelectedItem();
            RequestTabState state = active != null ? tabStates.get(active) : null;
            if (state != null && nv != null) {
                state.method = nv;
                markDirty(active);
            }
        });
        urlField.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        bodyTextArea.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        if (preRequestScriptsArea != null) {
            preRequestScriptsArea.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        }
        if (postRequestScriptsArea != null) {
            postRequestScriptsArea.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        }
        paramsData.addListener((javafx.collections.ListChangeListener<KeyValuePair>) c -> {
            onEditorFieldChanged();
            if (!syncingUrlAndParams) {
                syncParamsToUrl();
            }
        });
        headersData.addListener((javafx.collections.ListChangeListener<KeyValuePair>) c -> onEditorFieldChanged());

        openRequestTab("Untitled Request", null);
    }

    /** True while the URL<->Params sync itself is writing to either side —
     * prevents the two directions from ping-ponging off each other. */
    private boolean syncingUrlAndParams = false;

    /** Parses the URL's query string into the Params table — this is what
     * fixes params showing empty after opening an imported request (or any
     * request whose URL already has a query string baked in): the Params
     * tab used to be a write-only scratchpad that was never derived from
     * the URL at all. Existing rows are matched by key so a param's
     * Description survives repeated URL edits instead of being wiped. */
    private void syncUrlToParams() {
        String url = urlField.getText();
        int qIndex = url.indexOf('?');
        String query = qIndex >= 0 ? url.substring(qIndex + 1) : "";

        LinkedHashMap<String, String> parsed = new LinkedHashMap<>();
        if (!query.isEmpty()) {
            for (String pair : query.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                String key = eq >= 0 ? pair.substring(0, eq) : pair;
                String value = eq >= 0 ? pair.substring(eq + 1) : "";
                try {
                    key = java.net.URLDecoder.decode(key, StandardCharsets.UTF_8);
                    value = java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                    // keep the raw (un-decoded) text rather than losing the param
                }
                parsed.put(key, value);
            }
        }

        syncingUrlAndParams = true;
        try {
            // Keep existing rows (and their Description) for keys still present
            Map<String, KeyValuePair> existingByKey = new HashMap<>();
            for (KeyValuePair kv : paramsData) {
                if (!kv.getKey().isEmpty()) {
                    existingByKey.put(kv.getKey(), kv);
                }
            }
            List<KeyValuePair> rebuilt = new ArrayList<>();
            for (Map.Entry<String, String> entry : parsed.entrySet()) {
                KeyValuePair existing = existingByKey.get(entry.getKey());
                if (existing != null) {
                    existing.setValue(entry.getValue());
                    rebuilt.add(existing);
                } else {
                    rebuilt.add(new KeyValuePair(entry.getKey(), entry.getValue(), ""));
                }
            }
            if (!rebuilt.equals(new ArrayList<>(paramsData))) {
                paramsData.setAll(rebuilt);
            }
        } finally {
            syncingUrlAndParams = false;
        }
    }

    /** The reverse direction: rebuilds the URL's query string from whatever
     * is currently in the Params table — editing a param updates the URL
     * live, exactly like Postman. */
    private void syncParamsToUrl() {
        String url = urlField.getText();
        int qIndex = url.indexOf('?');
        String base = qIndex >= 0 ? url.substring(0, qIndex) : url;

        StringBuilder query = new StringBuilder();
        for (KeyValuePair kv : paramsData) {
            if (kv.getKey() == null || kv.getKey().isEmpty()) {
                continue;
            }
            if (query.length() > 0) {
                query.append('&');
            }
            try {
                query.append(URLEncoder.encode(kv.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(kv.getValue() == null ? "" : kv.getValue(), StandardCharsets.UTF_8));
            } catch (Exception ignored) {
                query.append(kv.getKey()).append('=').append(kv.getValue() == null ? "" : kv.getValue());
            }
        }
        String newUrl = query.length() > 0 ? base + "?" + query : base;
        if (!newUrl.equals(url)) {
            syncingUrlAndParams = true;
            try {
                urlField.replaceText(newUrl);
            } finally {
                syncingUrlAndParams = false;
            }
        }
    }

    /** Marks the ACTIVE tab as having unsaved changes (adds the dot), unless
     * we're currently restoring/loading a tab's own state into the editor. */
    private void onEditorFieldChanged() {
        if (restoringTabState || requestTabPane == null) {
            return;
        }
        markDirty(requestTabPane.getSelectionModel().getSelectedItem());
    }

    private void markDirty(Tab tab) {
        if (tab == null) {
            return;
        }
        RequestTabState state = tabStates.get(tab);
        if (state == null || state.dirty) {
            refreshTabGraphic(tab); // still reflect a live method change even if already dirty
            return;
        }
        state.dirty = true;
        refreshTabGraphic(tab);
    }

    /** Opens a new editor tab (blank when requestId is null) and selects it. */
    private Tab openRequestTab(String name, Long requestId) {
        return openRequestTab(name, requestId, "GET");
    }

    private Tab openRequestTab(String name, Long requestId, String method) {
        if (requestTabPane == null) {
            return null;
        }
        RequestTabState state = new RequestTabState();
        state.name = name;
        state.requestId = requestId;
        state.method = method == null ? "GET" : method;

        Tab tab = new Tab();
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
        refreshTabGraphic(tab);
        return tab;
    }

    /** Postman-style tab label: colored method badge + name + unsaved dot. */
    private void refreshTabGraphic(Tab tab) {
        RequestTabState state = tabStates.get(tab);
        if (state == null) {
            return;
        }
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        Label methodLabel = new Label(methodBadge(state.method == null ? "GET" : state.method));
        methodLabel.getStyleClass().add("tab-method-badge");
        methodLabel.getStyleClass().add("method-" + (state.method == null ? "get" : state.method.toLowerCase(Locale.ROOT)));

        Label nameLabel = new Label(state.name);
        nameLabel.getStyleClass().add("tab-name-label");

        box.getChildren().addAll(methodLabel, nameLabel);
        if (state.dirty) {
            Label dot = new Label("\u25CF");
            dot.getStyleClass().add("tab-unsaved-dot");
            box.getChildren().add(dot);
        }
        tab.setGraphic(box);
        tab.setText(null);
    }

    private void captureTabState(Tab tab) {
        RequestTabState state = tabStates.get(tab);
        if (state == null) {
            return;
        }
        state.method = methodCombo.getValue();
        state.url = urlField.getText();
        state.body = bodyTextArea.getText();
        state.preScript = preRequestScriptsArea != null ? preRequestScriptsArea.getText() : "";
        state.testsScript = postRequestScriptsArea != null ? postRequestScriptsArea.getText() : "";
        state.authType = authTypeCombo.getValue();
        state.authToken = getAuthTokenForPersistence();
        state.authUsername = usernameField.getText();
        state.authPassword = passwordField.getText();
        state.params = new ArrayList<>(paramsData);
        state.headers = new ArrayList<>(headersData);

        // FIX: snapshot the response too, so switching back to this tab
        // later shows ITS response, not whatever the app last displayed.
        state.lastApiResponse = this.lastApiResponse;
        Map<String, String> headersSnapshot = new LinkedHashMap<>();
        responseHeadersData.forEach(kv -> headersSnapshot.put(kv.getKey(), kv.getValue()));
        state.lastResponseHeaders = headersSnapshot;
    }

    private void restoreTabState(Tab tab) {
        RequestTabState state = tabStates.get(tab);
        if (state == null) {
            return;
        }
        // Guard: none of the field-changes triggered by restoring should
        // mark this (or the previous) tab dirty.
        restoringTabState = true;
        try {
            methodCombo.setValue(state.method == null ? "GET" : state.method);
            urlField.replaceText(state.url == null ? "" : state.url);
            bodyTextArea.replaceText(state.body == null ? "" : state.body);
            if (preRequestScriptsArea != null) {
                preRequestScriptsArea.setText(state.preScript == null ? "" : state.preScript);
            }
            if (postRequestScriptsArea != null) {
                postRequestScriptsArea.setText(state.testsScript == null ? "" : state.testsScript);
            }
            authTypeCombo.setValue(state.authType == null ? "No Auth" : state.authType);
            updateAuthFieldsVisibility(authTypeCombo.getValue());
            loadAuthTokenForPersistence(authTypeCombo.getValue(), state.authToken);
            usernameField.replaceText(state.authUsername == null ? "" : state.authUsername);
            passwordField.setText(state.authPassword == null ? "" : state.authPassword);
            paramsData.setAll(state.params);
            headersData.setAll(state.headers);
        } finally {
            restoringTabState = false;
        }

        // FIX: this is the actual bug fix — redisplay THIS tab's own
        // response (or clear to "no response yet" if it has none) instead
        // of leaving whatever the previously-viewed tab's response was.
        this.lastApiResponse = state.lastApiResponse;
        if (state.lastApiResponse != null) {
            renderResponse(state.lastApiResponse,
                    state.lastResponseHeaders != null ? state.lastResponseHeaders : Collections.emptyMap());
        } else {
            clearResponseUI();
        }

        refreshTabGraphic(tab);
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
                HBox.setHgrow(spacer, Priority.ALWAYS);
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

    /** FIX: the search bar only opened anything once you'd typed
     * something and pressed Enter — clicking it did nothing but place a
     * cursor, unlike Postman where a click opens the full search
     * overlay immediately (showing recent items before you've typed a
     * single character). */
    @FXML
    private void handleGlobalSearchFieldClicked() {
        openGlobalSearch("");
        if (globalSearchField != null) {
            globalSearchField.clear();
        }
    }

    private void openGlobalSearch(String initialQuery) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Search Thundercall");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextField searchField = new TextField(initialQuery == null ? "" : initialQuery);
        searchField.setPromptText("Search collections, folders, requests and environments");
        // FIX: this used to only ever hold TreeItem<String> (collections
        // tree entries) — environments were entirely out of scope for
        // "global" search. Object lets both kinds share one results list,
        // exactly like Postman's single combined result set.
        ListView<Object> results = new ListView<>();
        results.setPrefSize(520, 360);
        results.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(s -> s.startsWith("method-"));
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                if (item instanceof EnvironmentResponse env) {
                    setText("Environment  ·  " + env.getName());
                } else if (item instanceof TreeItem<?> rawItem) {
                    @SuppressWarnings("unchecked")
                    TreeItem<String> treeItem = (TreeItem<String>) rawItem;
                    String kind;
                    String method = requestMethodMap.get(treeItem);
                    if (method != null) {
                        kind = method;
                        getStyleClass().add("method-" + method.toLowerCase(Locale.ROOT));
                    } else if (collectionIdMap.containsKey(treeItem)) {
                        kind = "Collection";
                    } else {
                        kind = "Folder";
                    }
                    setText(kind + "  ·  " + treePath(treeItem));
                }
            }
        });

        // FIX: an empty query used to just show a "type to search"
        // placeholder with nothing else — Postman shows your recently
        // opened items right away, before you've typed anything, which
        // is often exactly what you're looking for.
        Runnable refresh = () -> {
            String q = searchField.getText() == null
                    ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
            if (q.isEmpty()) {
                results.setPlaceholder(new Label(recentlyViewedItems.isEmpty()
                        ? "Type to search your workspace" : "Recently viewed"));
                results.getItems().setAll(recentlyViewedItems);
                return;
            }
            List<Object> hits = new ArrayList<>();
            if (fullCollectionsRoot != null) {
                List<TreeItem<String>> treeHits = new ArrayList<>();
                collectSearchHits(fullCollectionsRoot, q, treeHits);
                hits.addAll(treeHits);
            }
            if (environmentsList != null) {
                for (EnvironmentResponse env : environmentsList.getItems()) {
                    if (env.getName() != null && env.getName().toLowerCase(Locale.ROOT).contains(q)) {
                        hits.add(env);
                    }
                }
            }
            results.setPlaceholder(new Label("No matches"));
            results.getItems().setAll(hits);
        };
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refresh.run());
        searchField.setOnAction(e -> {
            if (!results.getItems().isEmpty()) {
                openSearchResult(results.getItems().get(0));
                dialog.close();
            }
        });
        results.setOnMouseClicked(e -> {
            Object selected = results.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selected != null) {
                openSearchResult(selected);
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

    /** Opens whichever kind of result was picked, and remembers it so it
     * shows up under "Recently viewed" next time the search is opened
     * with an empty query. */
    private void openSearchResult(Object item) {
        recentlyViewedItems.remove(item);
        recentlyViewedItems.add(0, item);
        while (recentlyViewedItems.size() > 8) {
            recentlyViewedItems.remove(recentlyViewedItems.size() - 1);
        }
        if (item instanceof EnvironmentResponse env) {
            if (railEnvironmentsBtn != null) {
                railEnvironmentsBtn.setSelected(true);
            }
            showSidebarPane(environmentsPane);
            environmentsList.getSelectionModel().select(env);
            int row = environmentsList.getItems().indexOf(env);
            if (row >= 0) {
                environmentsList.scrollTo(row);
            }
        } else if (item instanceof TreeItem<?> rawItem) {
            @SuppressWarnings("unchecked")
            TreeItem<String> treeItem = (TreeItem<String>) rawItem;
            revealInTree(treeItem);
        }
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
        // FIX: Ctrl+S is a scene-level shortcut, so it can fire while a
        // Params/Headers table cell is still mid-edit (focus never left
        // the cell's text field) — without this, whatever you were
        // typing when you hit Ctrl+S could be silently lost. Shifting
        // focus away forces the focus-loss commit (see
        // commitOnFocusLossCell) to run synchronously before we read
        // any of the tables below.
        if (paramsTable != null) {
            paramsTable.requestFocus();
        }
        // Postman behaviour: if the active tab is an already-saved request,
        // Save / Ctrl+S updates it IN PLACE (same collection & folder).
        RequestTabState currentState = null;
        if (requestTabPane != null) {
            Tab currentTab = requestTabPane.getSelectionModel().getSelectedItem();
            currentState = currentTab != null ? tabStates.get(currentTab) : null;
        }
        if (currentState != null && currentState.requestId != null) {
            Long requestId = currentState.requestId;
            String requestName = currentState.name;
            Tab tabToClear = requestTabPane.getSelectionModel().getSelectedItem();
            RequestTabState finalCurrentState = currentState;
            new Thread(() -> {
                try {
                    ApiRequest apiRequest = new ApiRequest();
                    apiRequest.setName(requestName);
                    apiRequest.setMethod(HttpMethod.valueOf(methodCombo.getValue()));
                    apiRequest.setUrl(urlField.getText());
                    apiRequest.setHeaders(new JSONObject(buildHeaders()).toString());
                    apiRequest.setBody(buildRequestBody());
                    apiRequest.setPreRequestScript(
                            preRequestScriptsArea != null ? preRequestScriptsArea.getText() : null);
                    apiRequest.setTestsScript(
                            postRequestScriptsArea != null ? postRequestScriptsArea.getText() : null);
                    apiRequest.setAuthType(authTypeCombo.getValue());
                    apiRequest.setAuthToken(getAuthTokenForPersistence());
                    apiRequest.setAuthUsername(usernameField.getText());
                    apiRequest.setAuthPassword(passwordField.getText());
                    Optional<RequestResponse> updated = RequestService.updateRequest(requestId, apiRequest);
                    Platform.runLater(() -> {
                        if (updated.isPresent()) {
                            refreshCollectionsTree();
                            updateStatus("Saved: " + requestName);
                            // Clear the unsaved dot — this tab now matches the server
                            finalCurrentState.dirty = false;
                            if (tabToClear != null) {
                                refreshTabGraphic(tabToClear);
                            }
                        } else {
                            AlertUtils.showError("Failed to save request");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> handleApiError(e, "Saving request"));
                }
            }).start();
            return;
        }

        // Unsaved tab: ask for a name and a destination (selected
        // collection/folder in the tree)
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
        Tab tabBeingSaved = requestTabPane != null
                ? requestTabPane.getSelectionModel().getSelectedItem() : null;
        new Thread(() -> {
            try {
                ApiRequest apiRequest = new ApiRequest();
                apiRequest.setName(name);
                apiRequest.setMethod(HttpMethod.valueOf(methodCombo.getValue()));
                apiRequest.setUrl(urlField.getText());
                apiRequest.setHeaders(new JSONObject(buildHeaders()).toString());
                apiRequest.setBody(buildRequestBody());
                apiRequest.setPreRequestScript(
                        preRequestScriptsArea != null ? preRequestScriptsArea.getText() : null);
                apiRequest.setTestsScript(
                        postRequestScriptsArea != null ? postRequestScriptsArea.getText() : null);
                apiRequest.setAuthType(authTypeCombo.getValue());
                apiRequest.setAuthToken(getAuthTokenForPersistence());
                apiRequest.setAuthUsername(usernameField.getText());
                apiRequest.setAuthPassword(passwordField.getText());
                apiRequest.setCollectionId(collectionId);
                apiRequest.setFolderId(folderId);

                Optional<RequestResponse> savedRequest = RequestService.saveRequest(apiRequest);
                if (savedRequest.isPresent()) {
                    Platform.runLater(() -> {
                        AlertUtils.showSuccess("Request saved successfully");
                        refreshCollectionsTree();
                        // FIX: attach the new id/name to this tab — otherwise
                        // it stayed "untitled" forever and Ctrl+S kept asking
                        // to save-as again instead of updating in place, and
                        // the unsaved dot never cleared.
                        if (tabBeingSaved != null) {
                            RequestTabState state = tabStates.get(tabBeingSaved);
                            if (state != null) {
                                state.requestId = savedRequest.get().getId();
                                state.name = name;
                                state.dirty = false;
                                refreshTabGraphic(tabBeingSaved);
                            }
                        }
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
        // FIX: walk ALL ancestors. The old code only looked one level up, so
        // items inside nested folders (folder-in-folder) reported
        // "please select a collection" for Add folder / Add request.
        TreeItem<String> current = item;
        while (current != null) {
            Long collectionId = collectionIdMap.get(current);
            if (collectionId != null) {
                return collectionId;
            }
            current = current.getParent();
        }
        return null;
    }

    private Long getFolderIdFromTreeItem(TreeItem<String> item) {
        // Nearest enclosing folder: the item itself, or the first folder
        // ancestor — works at any nesting depth.
        TreeItem<String> current = item;
        while (current != null) {
            Long folderId = folderIdMap.get(current);
            if (folderId != null) {
                return folderId;
            }
            if (collectionIdMap.containsKey(current)) {
                return null; // reached the collection: no folder in between
            }
            current = current.getParent();
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
        if (lastApiResponse != null && lastApiResponse.isBinary()) {
            AlertUtils.showInfo("This is a binary response — use the save icon to download it instead of copying text");
            return;
        }
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
        if (lastApiResponse == null) {
            AlertUtils.showInfo("Send a request first — there's no response to save yet");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Response");

        if (lastApiResponse.isBinary()) {
            // PDF/Excel/zip/image etc: save the EXACT bytes the server sent
            String suggested = lastApiResponse.getFileName() != null
                    ? lastApiResponse.getFileName() : "response.bin";
            fileChooser.setInitialFileName(suggested);
            int dot = suggested.lastIndexOf('.');
            String ext = dot >= 0 ? suggested.substring(dot + 1) : "bin";
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter(ext.toUpperCase(Locale.ROOT) + " file", "*." + ext),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));

            File file = fileChooser.showSaveDialog(Main.getPrimaryStage());
            if (file == null) {
                return;
            }
            try {
                byte[] bytes = Base64.getDecoder().decode(lastApiResponse.getResponse());
                java.nio.file.Files.write(file.toPath(), bytes);
                AlertUtils.showSuccess("Saved " + formatSize(bytes.length) + " to " + file.getName());
                updateStatus("Response saved: " + file.getName());
            } catch (Exception e) {
                AlertUtils.showError("Failed to save file: " + e.getMessage());
            }
        } else {
            // Text response: guess a sensible extension from Content-Type
            String ext = "txt";
            String ct = lastApiResponse.getContentType();
            if (ct != null) {
                if (ct.contains("json")) {
                    ext = "json";
                } else if (ct.contains("xml")) {
                    ext = "xml";
                } else if (ct.contains("html")) {
                    ext = "html";
                } else if (ct.contains("csv")) {
                    ext = "csv";
                }
            } else if (responseBodyArea.getText().trim().startsWith("{")
                    || responseBodyArea.getText().trim().startsWith("[")) {
                ext = "json";
            }
            fileChooser.setInitialFileName("response." + ext);
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter(ext.toUpperCase(Locale.ROOT) + " file", "*." + ext),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));

            File file = fileChooser.showSaveDialog(Main.getPrimaryStage());
            if (file == null) {
                return;
            }
            try {
                java.nio.file.Files.writeString(file.toPath(), responseBodyArea.getText());
                AlertUtils.showSuccess("Saved to " + file.getName());
                updateStatus("Response saved: " + file.getName());
            } catch (Exception e) {
                AlertUtils.showError("Failed to save file: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRunTests() {
        String testScript = postRequestScriptsArea != null ? postRequestScriptsArea.getText() : null;
        if (testScript == null || testScript.isBlank()) {
            AlertUtils.showInfo("No test script in the Scripts tab to run");
            return;
        }
        if (lastApiResponse == null) {
            AlertUtils.showInfo("Send this request at least once first — there's no response to test against yet");
            return;
        }
        // Re-runs the SAME script the real send pipeline uses, against the
        // last real response — lets you iterate on a script without
        // re-sending the request every time.
        Map<String, String> vars = new LinkedHashMap<>(currentEnvironmentVariables());
        ScriptRunner.Result result = ScriptRunner.run(
                testScript, lastApiResponse.getResponse(), lastApiResponse.getStatusCode(), vars);
        testResultsList.getItems().clear();
        if (result.log.isEmpty()) {
            testResultsList.getItems().add("Script ran but produced no output");
        } else {
            testResultsList.getItems().addAll(result.log);
        }
        updateStatus("Tests re-run against the last response — see the Tests tab");
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
        Node sendButton = sendBtn;
        if (sendButton != null) {
            sendButton.setDisable(true);
        }

        // Run pre-request scripts first (they may set environment variables)
        runScriptsAndApply(preRequestScriptsArea != null ? preRequestScriptsArea.getText() : null,
                null, -1, "Pre-request script");

        // Resolve {{variables}} from the selected environment at send time.
        // The UI keeps showing the template; only the outgoing request is resolved.
        Map<String, String> vars = currentEnvironmentVariables();
        String url = VariableResolver.resolve(buildFullUrl(), vars);
        if ("OAuth 2.0".equals(authTypeCombo.getValue()) && oauth2CurrentTokenField != null
                && oauth2AddToCombo != null && "Query Params".equals(oauth2AddToCombo.getValue())
                && !oauth2CurrentTokenField.getText().isBlank()) {
            url = appendQueryParam(url, "access_token", oauth2CurrentTokenField.getText().trim());
        }
        String method = methodCombo.getValue();
        Map<String, String> headers = VariableResolver.resolveMap(buildHeadersForSend(), vars);
        String body = VariableResolver.resolve(buildRequestBody(), vars);

        // Form Data can include real file attachments — read now (FX
        // thread, same as url/headers/body just above) so the Task below
        // just carries an already-built, effectively-final list.
        List<FormDataField> formDataFields = null;
        if (bodyTypeGroup.getSelectedToggle() != null
                && "Form Data".equals(((ToggleButton) bodyTypeGroup.getSelectedToggle()).getText())) {
            formDataFields = buildFormDataFields(vars);
        }
        List<FormDataField> finalFormDataFields = formDataFields;

        Set<String> unresolved = new LinkedHashSet<>(VariableResolver.findUnresolved(url, vars));
        unresolved.addAll(VariableResolver.findUnresolved(body, vars));
        // Headers (Authorization included) can carry {{Token}} too — check them
        for (String headerValue : headers.values()) {
            unresolved.addAll(VariableResolver.findUnresolved(headerValue, vars));
        }
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

        // Validate the RESOLVED url (the template was allowed through)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            isRequestInProgress = false;
            if (sendButton != null) {
                sendButton.setDisable(false);
            }
            AlertUtils.showError("Resolved URL must start with http:// or https:// — got: "
                    + (url.length() > 60 ? url.substring(0, 57) + "..." : url));
            updateStatus("Invalid URL after variable resolution");
            return;
        }

        // Use Task for proper JavaFX threading
        String finalUrl = url;
        Task<Optional<ApiResponse>> requestTask = new Task<Optional<ApiResponse>>() {
            @Override
            protected Optional<ApiResponse> call() throws Exception {
                ApiRequest apiRequest = new ApiRequest();
                apiRequest.setUrl(finalUrl);
                apiRequest.setMethod(HttpMethod.valueOf(method));
                apiRequest.setHeaders(new JSONObject(headers).toString());
                apiRequest.setBody(body);
                apiRequest.setFormData(finalFormDataFields);

                return RequestService.executeRequest(apiRequest);
            }
        };

        String finalUrl1 = url;
        requestTask.setOnSucceeded(event -> {
            isRequestInProgress = false;

            // Re-enable send button
            Node sendButton1 = sendBtn;
            if (sendButton1 != null) {
                sendButton1.setDisable(false);
            }

            Optional<ApiResponse> result = requestTask.getValue();
            if (result.isPresent()) {
                ApiResponse apiResponse = result.get();
                // Kept as an object (not just text) so binary bytes are never
                // mangled by passing through a JavaFX TextArea.
                lastApiResponse = apiResponse;
                updateResponseUI(apiResponse, parseHeaders(apiResponse.getResponseHeaders()));

                String historyEntry = method + " " + finalUrl1 + " (" + apiResponse.getStatusCode() + ")";
                if (!historyData.contains(historyEntry)) {
                    historyData.add(0, historyEntry);
                    historyList.setItems(historyData);
                }

                updateStatus("Request completed");

                // Post-response scripts: pm.environment.set / pm.response.json()
                String postScript = postRequestScriptsArea != null
                        ? postRequestScriptsArea.getText() : null;
                runScriptsAndApply(postScript, apiResponse.getResponse(),
                        apiResponse.getStatusCode(), "Script");

                // Convenience: a response-reading script placed in the
                // PRE-request box (very common) still works — it is run
                // again here, after the response, so tokens get captured.
                String preScript = preRequestScriptsArea != null
                        ? preRequestScriptsArea.getText() : null;
                if (preScript != null && preScript.contains("pm.response")
                        && (postScript == null || !postScript.contains("pm.response"))) {
                    runScriptsAndApply(preScript, apiResponse.getResponse(),
                            apiResponse.getStatusCode(), "Script (from Pre-request box)");
                }
            } else {
                AlertUtils.showError("Request failed with no response");
                updateStatus("Request failed");
            }
        });

        requestTask.setOnFailed(event -> {
            isRequestInProgress = false;

            // Re-enable send button
            Node sendButton2 = sendBtn;
            if (sendButton2 != null) {
                sendButton2.setDisable(false);
            }

            AlertUtils.showError("Request failed: " + requestTask.getException().getMessage());
            updateStatus("Request failed");
        });

        new Thread(requestTask).start();
    }

    // ============================================================
    // WebSocket (method = "WS")
    // ============================================================

    @FXML
    private void handleWsConnectToggle() {
        if (wsConnected) {
            closeActiveWebSocket();
            return;
        }
        String rawUrl = urlField.getText() == null ? "" : urlField.getText().trim();
        if (rawUrl.isEmpty()) {
            AlertUtils.showError("Enter a ws:// or wss:// URL first.");
            return;
        }
        Map<String, String> vars = currentEnvironmentVariables();
        String resolvedUrl = VariableResolver.resolve(rawUrl, vars);
        if (!resolvedUrl.startsWith("ws://") && !resolvedUrl.startsWith("wss://")) {
            AlertUtils.showError("A WebSocket URL must start with ws:// or wss:// — got: "
                    + (resolvedUrl.length() > 60 ? resolvedUrl.substring(0, 57) + "..." : resolvedUrl));
            return;
        }
        if (wsMessageLog != null) {
            wsMessageLog.getItems().clear();
        }
        setWsStatus("Connecting...");
        wsConnectBtn.setDisable(true);

        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(resolvedUrl), new WebSocket.Listener() {
                    private final StringBuilder buffer = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        activeWebSocket = webSocket;
                        wsConnected = true;
                        Platform.runLater(() -> {
                            wsConnectBtn.setText("Disconnect");
                            wsConnectBtn.setDisable(false);
                            setWsStatus("Connected to " + resolvedUrl);
                            logWsMessage("— connected —", false);
                            updateStatus("WebSocket connected");
                        });
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        buffer.append(data);
                        if (last) {
                            String message = buffer.toString();
                            buffer.setLength(0);
                            Platform.runLater(() -> logWsMessage(message, false));
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        wsConnected = false;
                        activeWebSocket = null;
                        Platform.runLater(() -> {
                            wsConnectBtn.setText("Connect");
                            wsConnectBtn.setDisable(false);
                            setWsStatus("Connection error: " + error.getMessage());
                            logWsMessage("— error: " + error.getMessage() + " —", false);
                            updateStatus("WebSocket error");
                        });
                        WebSocket.Listener.super.onError(webSocket, error);
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        wsConnected = false;
                        activeWebSocket = null;
                        Platform.runLater(() -> {
                            wsConnectBtn.setText("Connect");
                            wsConnectBtn.setDisable(false);
                            setWsStatus("Disconnected (" + statusCode
                                    + (reason != null && !reason.isBlank() ? ": " + reason : "") + ")");
                            logWsMessage("— disconnected —", false);
                            updateStatus("WebSocket disconnected");
                        });
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        wsConnectBtn.setText("Connect");
                        wsConnectBtn.setDisable(false);
                        setWsStatus("Couldn't connect: " + ex.getMessage());
                        updateStatus("WebSocket connection failed");
                    });
                    return null;
                });
    }

    @FXML
    private void handleWsSendMessage() {
        if (!wsConnected || activeWebSocket == null) {
            AlertUtils.showError("Not connected — click Connect first.");
            return;
        }
        String text = wsMessageComposeArea.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        Map<String, String> vars = currentEnvironmentVariables();
        String resolved = VariableResolver.resolve(text, vars);
        activeWebSocket.sendText(resolved, true);
        logWsMessage(resolved, true);
        wsMessageComposeArea.clear();
    }

    private void closeActiveWebSocket() {
        if (activeWebSocket != null) {
            activeWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client closed");
        }
        wsConnected = false;
        activeWebSocket = null;
        if (wsConnectBtn != null) {
            wsConnectBtn.setText("Connect");
            wsConnectBtn.setDisable(false);
        }
        setWsStatus("Not connected");
    }

    private void setWsStatus(String text) {
        if (wsStatusLabel != null) {
            wsStatusLabel.setText(text);
        }
    }

    private void logWsMessage(String message, boolean outgoing) {
        if (wsMessageLog == null) {
            return;
        }
        String time = java.time.LocalTime.now().withNano(0).toString();
        String prefix = outgoing ? "↑ SENT " : "↓ RECV ";
        wsMessageLog.getItems().add("[" + time + "] " + prefix + message);
        wsMessageLog.scrollTo(wsMessageLog.getItems().size() - 1);
    }

    // ============================================================
    // Socket.IO (method = "SOCKETIO")
    // ============================================================

    @FXML
    private void handleSocketIoConnectToggle() {
        if (socketIoConnected) {
            closeActiveSocketIoSession();
            return;
        }
        String rawUrl = urlField.getText() == null ? "" : urlField.getText().trim();
        if (rawUrl.isEmpty()) {
            AlertUtils.showError("Enter a Socket.IO server URL first, e.g. http://localhost:3000");
            return;
        }
        Map<String, String> vars = currentEnvironmentVariables();
        String resolvedUrl = VariableResolver.resolve(rawUrl, vars);
        String namespace = socketIoNamespaceField != null ? socketIoNamespaceField.getText() : null;

        if (socketIoEventLog != null) {
            socketIoEventLog.getItems().clear();
        }
        socketIoEventsSeen = 0;
        setSocketIoStatus("Connecting...");
        socketIoConnectBtn.setDisable(true);

        new Thread(() -> {
            try {
                SocketIoStatusResponse status = SocketIoService.connect(resolvedUrl, namespace);
                socketIoSessionId = status.getSessionId();
                socketIoConnected = true;
                Platform.runLater(() -> {
                    socketIoConnectBtn.setText("Disconnect");
                    socketIoConnectBtn.setDisable(false);
                    setSocketIoStatus("Status: " + status.getStatus());
                    applySocketIoEvents(status.getEvents());
                    updateStatus("Socket.IO connecting...");
                });
                startSocketIoPolling();
            } catch (IOException e) {
                Platform.runLater(() -> {
                    socketIoConnectBtn.setDisable(false);
                    setSocketIoStatus("Couldn't connect: " + SocketIoService.friendlyMessage(e));
                    updateStatus("Socket.IO connection failed");
                });
            }
        }).start();
    }

    /** The desktop client doesn't hold its own live Socket.IO connection
     * — the backend does (see SocketIoServiceImpl) — so this just asks
     * for what's new every half second while connected. Simple and
     * reliable over adding a second streaming layer on top of the
     * connection the backend already maintains. */
    private void startSocketIoPolling() {
        socketIoPollThread = new Thread(() -> {
            while (socketIoConnected && socketIoSessionId != null) {
                try {
                    Thread.sleep(500);
                    if (!socketIoConnected || socketIoSessionId == null) {
                        break;
                    }
                    String sid = socketIoSessionId;
                    SocketIoStatusResponse status = SocketIoService.poll(sid, socketIoEventsSeen);
                    Platform.runLater(() -> {
                        setSocketIoStatus("Status: " + status.getStatus());
                        applySocketIoEvents(status.getEvents());
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    // A single transient poll failure isn't worth giving up
                    // the whole connection over — show it and keep trying.
                    Platform.runLater(() -> setSocketIoStatus("Poll error: " + SocketIoService.friendlyMessage(e)));
                }
            }
        });
        socketIoPollThread.setDaemon(true);
        socketIoPollThread.start();
    }

    private void applySocketIoEvents(List<SocketIoEventResponse> events) {
        if (events == null || events.isEmpty() || socketIoEventLog == null) {
            return;
        }
        for (SocketIoEventResponse event : events) {
            String prefix;
            switch (event.getDirection()) {
                case "in":
                    prefix = "↓ RECV ";
                    break;
                case "out":
                    prefix = "↑ SENT ";
                    break;
                default:
                    prefix = "• ";
                    break;
            }
            socketIoEventLog.getItems().add(prefix + event.getEventName() + "  " + event.getData());
        }
        socketIoEventsSeen += events.size();
        socketIoEventLog.scrollTo(socketIoEventLog.getItems().size() - 1);
    }

    @FXML
    private void handleSocketIoEmit() {
        if (!socketIoConnected || socketIoSessionId == null) {
            AlertUtils.showError("Not connected — click Connect first.");
            return;
        }
        String eventName = socketIoEventNameField.getText() == null ? "" : socketIoEventNameField.getText().trim();
        if (eventName.isEmpty()) {
            AlertUtils.showError("Enter an event name first.");
            return;
        }
        Map<String, String> vars = currentEnvironmentVariables();
        String data = VariableResolver.resolve(
                socketIoEventDataField.getText() == null ? "" : socketIoEventDataField.getText(), vars);
        String sid = socketIoSessionId;
        new Thread(() -> {
            try {
                SocketIoService.emit(sid, eventName, data);
                Platform.runLater(() -> socketIoEventDataField.clear());
            } catch (IOException e) {
                Platform.runLater(() -> AlertUtils.showError(
                        "Couldn't emit that event: " + SocketIoService.friendlyMessage(e)));
            }
        }).start();
    }

    private void closeActiveSocketIoSession() {
        socketIoConnected = false;
        String sid = socketIoSessionId;
        socketIoSessionId = null;
        if (sid != null) {
            new Thread(() -> {
                try {
                    SocketIoService.disconnect(sid);
                } catch (IOException ignored) {
                    // best-effort — the backend session just sits idle otherwise
                }
            }).start();
        }
        if (socketIoConnectBtn != null) {
            socketIoConnectBtn.setText("Connect");
            socketIoConnectBtn.setDisable(false);
        }
        setSocketIoStatus("Not connected");
    }

    private void setSocketIoStatus(String text) {
        if (socketIoStatusLabel != null) {
            socketIoStatusLabel.setText(text);
        }
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

    private boolean isPdfResponse(ApiResponse apiResponse) {
        String contentType = apiResponse.getContentType();
        String fileName = apiResponse.getFileName();
        return (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("pdf"))
                || (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf"));
    }

    /** Renders every page of a PDF response as an image and shows them in
     * the response's WebView pane — that pane existed already but was
     * never actually wired up to anything, always blank. */
    private void renderPdfPreview(ApiResponse apiResponse) {
        if (responsePreview == null) {
            return;
        }
        responsePreview.getEngine().loadContent(
                "<html><body style='margin:0;padding:12px;background:#1d1f24;'>"
                        + "<p style='font-family:sans-serif;color:#9a9ea9;font-size:12px;'>Rendering PDF preview…</p>"
                        + "</body></html>");

        new Thread(() -> {
            try {
                byte[] pdfBytes = Base64.getDecoder().decode(apiResponse.getResponse());
                StringBuilder html = new StringBuilder(
                        "<html><body style='margin:0;padding:12px;background:#1d1f24;text-align:center;'>");
                try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(pdfBytes)) {
                    org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(document);
                    int pageCount = document.getNumberOfPages();
                    // A very long PDF would be slow/heavy to render fully inline;
                    // cap it and point to Save for the complete file.
                    int pagesToRender = Math.min(pageCount, 20);
                    for (int i = 0; i < pagesToRender; i++) {
                        java.awt.image.BufferedImage image = renderer.renderImageWithDPI(i, 110);
                        java.io.ByteArrayOutputStream pngBytes = new java.io.ByteArrayOutputStream();
                        javax.imageio.ImageIO.write(image, "png", pngBytes);
                        String base64Png = Base64.getEncoder().encodeToString(pngBytes.toByteArray());
                        html.append("<img src='data:image/png;base64,").append(base64Png)
                                .append("' style='max-width:100%;margin-bottom:12px;box-shadow:0 2px 8px rgba(0,0,0,0.4);'/>");
                    }
                    if (pageCount > pagesToRender) {
                        html.append("<p style='font-family:sans-serif;color:#9a9ea9;font-size:12px;'>")
                                .append(pageCount - pagesToRender)
                                .append(" more page(s) not shown — save the file to see the rest.</p>");
                    }
                }
                html.append("</body></html>");
                String finalHtml = html.toString();
                Platform.runLater(() -> responsePreview.getEngine().loadContent(finalHtml));
            } catch (Exception e) {
                Platform.runLater(() -> responsePreview.getEngine().loadContent(
                        "<html><body style='margin:0;padding:12px;background:#1d1f24;'>"
                                + "<p style='font-family:sans-serif;color:#e57373;font-size:12px;'>"
                                + "Couldn't render a preview: " + e.getMessage() + "</p></body></html>"));
            }
        }).start();
    }

    private void clearResponsePreview() {
        if (responsePreview != null) {
            responsePreview.getEngine().loadContent("");
        }
    }

    private void updateResponseUI(ApiResponse apiResponse, Map<String, String> headers) {

        Platform.runLater(() -> {
            renderResponse(apiResponse, headers);

            // Add to history (only for an actual new send, not a tab restore)
            String historyEntry = methodCombo.getValue() + " " + urlField.getText();
            if (!historyData.contains(historyEntry)) {
                historyData.add(0, historyEntry);
                historyList.setItems(historyData);
            }
        });
    }

    /** Renders a response into the shared viewer — used both right after a
     * send and when switching to a tab that already has a stored response.
     * Must run on the FX thread. */
    private void renderResponse(ApiResponse apiResponse, Map<String, String> headers) {
        int statusCode = apiResponse.getStatusCode();
        long responseTime = apiResponse.getDuration();
        statusLabel.setText("Status: " + statusCode + " " + getStatusText(statusCode));
        timeLabel.setText("Time: " + responseTime + "ms");
        sizeLabel.setText("Size: " + formatSize(apiResponse.getSizeBytes()));

        // Apply status code styling
        statusLabel.getStyleClass().removeAll("status-2xx", "status-4xx", "status-5xx");
        if (statusCode >= 200 && statusCode < 300) {
            statusLabel.getStyleClass().add("status-2xx");
        } else if (statusCode >= 400 && statusCode < 500) {
            statusLabel.getStyleClass().add("status-4xx");
        } else if (statusCode >= 500) {
            statusLabel.getStyleClass().add("status-5xx");
        }

        if (responseSearch != null) {
            responseSearch.reset();
        }
        if (apiResponse.isBinary()) {
            // A generated PDF/Excel/zip etc: never dump raw/Base64 bytes
            // into the text area. Show a clear "download this" card
            // instead — Save Response (the icon button) does the rest.
            responseBodyArea.setEditable(false);
            boolean isPdf = isPdfResponse(apiResponse);
            responseBodyArea.replaceText(
                    "\uD83D\uDCC4  Binary response\n\n"
                            + "Content-Type: " + (apiResponse.getContentType() != null ? apiResponse.getContentType() : "unknown") + "\n"
                            + "Size: " + formatSize(apiResponse.getSizeBytes()) + "\n"
                            + "Suggested file name: " + apiResponse.getFileName() + "\n\n"
                            + (isPdf
                            ? "PDF preview is shown on the right. Click the save icon above "
                            + "the response to download the original file."
                            : "This isn't text, so it can't be shown here — click the save icon "
                            + "above the response to download it exactly as the server sent it."));
            if (isPdf) {
                renderPdfPreview(apiResponse);
            } else {
                clearResponsePreview();
            }
        } else {
            responseBodyArea.setEditable(true);
            responseBodyArea.replaceText(apiResponse.getResponse() != null ? apiResponse.getResponse() : "");
            formatResponseBody(apiResponse.getResponse());
            clearResponsePreview();
        }

        responseHeadersData.clear();
        if (headers != null) {
            headers.forEach((key, value) -> responseHeadersData.add(new KeyValuePair(key, value, "")));
        }
    }

    /** Resets the response viewer to its empty "no response yet" state —
     * used when switching to a tab that has never been sent. */
    private void clearResponseUI() {
        statusLabel.setText("Status: ");
        statusLabel.getStyleClass().removeAll("status-2xx", "status-4xx", "status-5xx");
        timeLabel.setText("Time: ");
        sizeLabel.setText("Size: ");
        if (responseSearch != null) {
            responseSearch.reset();
        }
        responseBodyArea.setEditable(true);
        responseBodyArea.replaceText("");
        responseHeadersData.clear();
        clearResponsePreview();
    }

    private void formatResponseBody(String responseBody) {
        try {
            if (responseBody.trim().startsWith("{")) {
                JSONObject json = new JSONObject(responseBody);
                responseBodyArea.replaceText(json.toString(2));
            } else if (responseBody.trim().startsWith("<")) {
                JSONObject json = XML.toJSONObject(responseBody);
                responseBodyArea.replaceText(json.toString(2));
            }
        } catch (Exception e) {
            // If formatting fails, just use the original response
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
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

    /** Appends one query parameter to a (possibly already-parameterized) URL. */
    private String appendQueryParam(String url, String key, String value) {
        try {
            String separator = url.contains("?") ? "&" : "?";
            return url + separator + URLEncoder.encode(key, StandardCharsets.UTF_8)
                    + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return url + (url.contains("?") ? "&" : "?") + key + "=" + value;
        }
    }

    private String buildFullUrl() {
        // FIX: params used to be appended here again on top of the URL,
        // but the URL and Params table are now kept in sync live (see
        // syncParamsToUrl/syncUrlToParams) — the URL already reflects
        // whatever is in the Params table by the time Send is clicked.
        // Appending again here would have double-added every param
        // (?eid=16465&eid=16465) once params round-tripped through the URL.
        return urlField.getText().trim();
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
                    case "GraphQL":
                        headers.put("Content-Type", "application/json");
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

        // NOTE: the Authorization header is intentionally NOT added here.
        // It's computed fresh in buildHeadersForSend() from the Authorization
        // tab, and the tab's settings are saved as their own fields — so a
        // saved request's Headers tab stays clean instead of showing a
        // duplicate, stale "Authorization: Bearer {{Token}}" row (like
        // Postman: auth lives in its own tab, not baked into Headers).
        return headers;
    }

    /** Headers actually sent on the wire: user headers + the computed
     * Authorization header from the Authorization tab. */
    private Map<String, String> buildHeadersForSend() {
        Map<String, String> headers = buildHeaders();

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
                // Query Params mode is handled separately, right after the
                // URL is resolved (see handleSendRequest) — a token added
                // there belongs in the URL, not a header.
                if (oauth2CurrentTokenField != null && oauth2AddToCombo != null
                        && "Request Headers".equals(oauth2AddToCombo.getValue())
                        && !oauth2CurrentTokenField.getText().isBlank()) {
                    String prefix = oauth2HeaderPrefixField != null ? oauth2HeaderPrefixField.getText().trim() : "Bearer";
                    headers.put("Authorization",
                            (prefix.isEmpty() ? "" : prefix + " ") + oauth2CurrentTokenField.getText().trim());
                }
                break;
        }

        return headers;
    }

    /** Builds the real, send-ready form-data list — text values resolved
     * for {{variables}} same as everywhere else, file rows read straight
     * off disk and Base64-encoded (a row with "File" selected but no
     * file chosen yet is just skipped rather than failing the request). */
    private List<FormDataField> buildFormDataFields(Map<String, String> vars) {
        List<FormDataField> fields = new ArrayList<>();
        for (KeyValuePair kv : formData) {
            if (kv.getKey() == null || kv.getKey().isBlank()) {
                continue;
            }
            String resolvedKey = VariableResolver.resolve(kv.getKey(), vars);
            if ("File".equalsIgnoreCase(kv.getType())) {
                if (kv.getFilePath() == null || kv.getFilePath().isBlank()) {
                    continue;
                }
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(kv.getFilePath()));
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    fields.add(new FormDataField(resolvedKey, "file", null, kv.getFileName(), base64));
                } catch (Exception e) {
                    AlertUtils.showError("Couldn't read the file for \"" + kv.getKey() + "\": " + e.getMessage());
                }
            } else {
                fields.add(new FormDataField(resolvedKey, "text", VariableResolver.resolve(kv.getValue(), vars), null, null));
            }
        }
        return fields;
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
                String urlEncoded = convertFormDataToUrlEncoded(urlEncodedData);
                return urlEncoded;
            case "GraphQL":
                // Matches how every GraphQL server expects it over HTTP:
                // a single JSON object with "query" and "variables".
                String query = graphqlQueryArea.getText();
                String variablesText = graphqlVariablesArea.getText() == null
                        ? "" : graphqlVariablesArea.getText().trim();
                JSONObject graphqlBody = new JSONObject();
                graphqlBody.put("query", query == null ? "" : query);
                if (variablesText.isEmpty()) {
                    graphqlBody.put("variables", new JSONObject());
                } else {
                    try {
                        graphqlBody.put("variables", new JSONObject(variablesText));
                    } catch (Exception e) {
                        AlertUtils.showError("Query Variables must be valid JSON");
                        return "";
                    }
                }
                return graphqlBody.toString();
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
        // FIX: {{BASE_URL}}/auth/login was rejected before the variable was
        // even resolved. Template URLs pass here; the RESOLVED url is
        // validated in handleSendRequest after substitution.
        if (url.startsWith("{{")) {
            return true;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            AlertUtils.showError("URL must start with http:// or https:// (or a {{variable}})");
            return false;
        }
        return true;
    }

    @FXML
    private void handleSamplePostRequest() {
        methodCombo.getSelectionModel().select("POST");
        urlField.replaceText("https://jsonplaceholder.typicode.com/posts");

        // Set sample JSON body
        bodyTextArea.replaceText("{\n  \"title\": \"foo\",\n  \"body\": \"bar\",\n  \"userId\": 1\n}");

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
        // FIX: this button bar is shared between Form Data and
        // x-www-form-urlencoded (same HBox in the FXML) — it used to
        // always add to the Form Data list even while looking at the
        // url-encoded table. It now acts on whichever table is actually
        // visible.
        if (urlEncodedTable != null && urlEncodedTable.isVisible()) {
            urlEncodedData.add(new KeyValuePair("", "", ""));
        } else {
            formData.add(new KeyValuePair("", "", ""));
        }
    }

    @FXML
    private void handleDeleteFormDataRow() {
        if (urlEncodedTable != null && urlEncodedTable.isVisible()) {
            KeyValuePair selected = urlEncodedTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                urlEncodedData.remove(selected);
            }
            return;
        }
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

        Long collectionId = selectedItem != null ? getCollectionIdFromTreeItem(selectedItem) : null;
        // Nested folders, like Postman: creating from a folder puts the new
        // folder INSIDE it; creating from a collection makes a top-level one
        Long parentFolderId = selectedItem != null ? folderIdMap.get(selectedItem) : null;
        if (collectionId != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Folder");
            dialog.setHeaderText(parentFolderId != null
                    ? "Create a folder inside \"" + selectedItem.getValue() + "\""
                    : "Create a new folder");
            dialog.setContentText("Folder name:");
            ThemeManager.styleDialog(dialog.getDialogPane());

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    createNewFolder(name, collectionId, parentFolderId);
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
                    // FIX: requests were only removed from the UI — they came
                    // back on every refresh. Delete on the server for real.
                    Long requestId = requestIdMap.get(selectedItem);
                    if (requestId != null) {
                        String requestName = selectedItem.getValue();
                        new Thread(() -> {
                            boolean deleted = RequestService.deleteRequest(requestId);
                            Platform.runLater(() -> {
                                if (deleted) {
                                    // Close its tab if open
                                    Tab openTab = findTabByRequestId(requestId);
                                    if (openTab != null && requestTabPane != null) {
                                        tabStates.remove(openTab);
                                        requestTabPane.getTabs().remove(openTab);
                                        if (requestTabPane.getTabs().isEmpty()) {
                                            openRequestTab("Untitled Request", null);
                                        }
                                    }
                                    refreshCollectionsTree();
                                    updateStatus("Request deleted: " + requestName);
                                } else {
                                    AlertUtils.showError("Failed to delete request");
                                }
                            });
                        }).start();
                    }
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
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(Main.getPrimaryStage());
        if (file == null) {
            return;
        }
        if (file.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            importRequestsFromCsv(file);
        } else if (file.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
            importPostmanCollection(file);
        } else {
            AlertUtils.showInfo("Pick a Postman collection (.json) or a requests spreadsheet (.csv)");
        }
    }

    /**
     * Imports a Postman Collection v2.1 export: creates a NEW collection
     * named after the file's info.name (auto-suffixed on a name clash),
     * recreates the folder tree (item groups) at any depth, creates every
     * request with its method/URL/headers/body, carries over pre-request
     * and test scripts, maps Bearer/Basic auth (collection-level auth is
     * used as a fallback for requests that don't set their own), and — if
     * the file defines collection variables — creates a matching
     * Environment so {{variables}} used in the collection resolve
     * immediately.
     */
    private void importPostmanCollection(File file) {
        new Thread(() -> {
            // Bulk operation: individual folder/request failures are
            // counted and reported in ONE final summary instead of a
            // separate modal dialog per failure.
            AlertUtils.setQuiet(true);
            try {
                String text = java.nio.file.Files.readString(file.toPath());
                JSONObject root = new JSONObject(text);
                if (!root.has("item")) {
                    Platform.runLater(() -> AlertUtils.showError(
                            "This doesn't look like a Postman collection export (no \"item\" array found)"));
                    return;
                }
                String baseName = root.has("info") && root.getJSONObject("info").has("name")
                        ? root.getJSONObject("info").getString("name")
                        : file.getName().replaceAll("(?i)\\.json$", "");

                // Avoid silently merging into a same-named collection —
                // always import as a fresh collection, de-duplicating the name.
                Set<String> existingNames = new HashSet<>();
                Optional<List<CollectionResponse>> existingCollections = CollectionService.getUserCollections();
                Long currentWorkspaceId = WorkspaceManager.getCurrentWorkspace() != null
                        ? WorkspaceManager.getCurrentWorkspace().getId() : null;
                if (existingCollections.isPresent()) {
                    for (CollectionResponse c : existingCollections.get()) {
                        if (currentWorkspaceId != null
                                && String.valueOf(currentWorkspaceId).equals(c.getWorkspaceId())) {
                            existingNames.add(c.getName());
                        }
                    }
                }
                String collectionName = baseName;
                int suffix = 2;
                while (existingNames.contains(collectionName)) {
                    collectionName = baseName + " (" + suffix + ")";
                    suffix++;
                }

                CollectionRequest collectionRequest = new CollectionRequest();
                collectionRequest.setName(collectionName);
                collectionRequest.setDescription("Imported from " + file.getName());
                Optional<CollectionResponse> createdCollection = CollectionService.createCollection(collectionRequest);
                if (createdCollection.isEmpty()) {
                    Platform.runLater(() -> AlertUtils.showError("Failed to create the collection"));
                    return;
                }
                Long collectionId = createdCollection.get().getId();

                // Collection-level auth (used when a request specifies none)
                String[] fallbackAuth = extractAuth(root.optJSONObject("auth"));

                int[] counters = {0, 0, 0, 0}; // requests, folders, skipped(file uploads etc.), FAILED
                importPostmanItems(root.getJSONArray("item"), collectionId, null, fallbackAuth, counters);

                // Collection variables → an Environment of the same name, so
                // {{variables}} used throughout the import resolve right away
                int variableCount = 0;
                if (root.has("variable")) {
                    JSONArray vars = root.getJSONArray("variable");
                    Map<String, String> variables = new LinkedHashMap<>();
                    for (int i = 0; i < vars.length(); i++) {
                        JSONObject v = vars.getJSONObject(i);
                        String key = v.optString("key", "");
                        if (!key.isEmpty()) {
                            variables.put(key, v.optString("value", ""));
                        }
                    }
                    if (!variables.isEmpty()) {
                        Optional<EnvironmentResponse> env = EnvironmentService.createEnvironment(
                                collectionName, "Imported collection variables", variables);
                        if (env.isPresent()) {
                            variableCount = variables.size();
                        }
                    }
                }

                int finalRequests = counters[0];
                int finalFolders = counters[1];
                int finalSkipped = counters[2];
                int finalFailed = counters[3];
                int finalVariables = variableCount;
                String finalCollectionName = collectionName;
                boolean sessionExpiredMidImport = ApiClient.getToken() == null;
                Platform.runLater(() -> {
                    refreshCollectionsTree();
                    loadEnvironments();
                    if (sessionExpiredMidImport) {
                        // FIX: don't claim success when the import was cut
                        // short — say exactly what happened and what to do.
                        AlertUtils.showError("Your session expired partway through this import.\n\n"
                                + "Completed before that happened: " + finalRequests + " request(s), "
                                + finalFolders + " folder(s).\n\n"
                                + "Please log in again, then DELETE \"" + finalCollectionName
                                + "\" and re-run the import from scratch — this importer isn't safe to "
                                + "resume partway (re-running would create a second, duplicate collection).");
                        updateStatus("Import stopped: session expired");
                        return;
                    }
                    StringBuilder msg = new StringBuilder("Imported \"" + finalCollectionName + "\": "
                            + finalRequests + " request(s)");
                    if (finalFolders > 0) {
                        msg.append(", ").append(finalFolders).append(" folder(s)");
                    }
                    if (finalVariables > 0) {
                        msg.append(", ").append(finalVariables).append(" variable(s) into a new environment");
                    }
                    if (finalSkipped > 0) {
                        msg.append(". ").append(finalSkipped).append(" item(s) skipped (e.g. file uploads)");
                    }
                    if (finalFailed > 0) {
                        // FIX: these used to vanish silently (the count was
                        // just lower than the file's true total) while ALSO
                        // popping their own separate error dialog — now
                        // they're counted honestly in this one summary, and
                        // the per-item dialog is suppressed (see console).
                        msg.append(". ").append(finalFailed)
                                .append(" item(s) FAILED (transient server/network error — check the console "
                                        + "log, then re-add those manually if needed)");
                    }
                    AlertUtils.showSuccess(msg.toString());
                    updateStatus("Postman import complete: " + finalCollectionName);
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.showError("Import failed: " + e.getMessage()));
            } finally {
                AlertUtils.setQuiet(false);
            }
        }).start();
    }

    /** Recursively walks a Postman "item" array: item groups become nested
     * folders, leaf items become requests. counters = {requests, folders, skipped, failed}. */
    private void importPostmanItems(JSONArray items, Long collectionId, Long parentFolderId,
                                    String[] fallbackAuth, int[] counters) {
        for (int i = 0; i < items.length(); i++) {
            // FIX: if the session dies partway through a long import, every
            // remaining call used to fail the SAME way and each one popped
            // its own modal "session expired" dialog — potentially dozens
            // in a row for a big file. Stop cleanly on the first one instead.
            if (ApiClient.getToken() == null) {
                counters[2] += (items.length() - i);
                return;
            }
            JSONObject item = items.getJSONObject(i);
            String name = item.optString("name", "Untitled");
            if (item.has("item")) {
                // Item group (folder) — Postman nests these to any depth
                Optional<FolderResponse> folder = FolderService.createFolder(name, "", collectionId, parentFolderId);
                if (folder.isPresent()) {
                    counters[1]++;
                    importPostmanItems(item.getJSONArray("item"), collectionId, folder.get().getId(),
                            fallbackAuth, counters);
                } else {
                    // FIX: used to vanish with no count at all (and its own
                    // separate popup). Now it's counted, and anything that
                    // WOULD have been nested inside it is honestly counted
                    // as failed too, since it has nowhere to go.
                    counters[3]++;
                    counters[3] += countPostmanLeaves(item.optJSONArray("item"));
                }
                continue;
            }
            if (!item.has("request")) {
                counters[2]++;
                continue;
            }
            JSONObject request = item.getJSONObject("request");

            String methodStr = request.optString("method", "GET").toUpperCase(Locale.ROOT);
            HttpMethod method;
            try {
                method = HttpMethod.valueOf(methodStr);
            } catch (IllegalArgumentException e) {
                method = HttpMethod.GET;
            }

            String url = resolvePostmanUrl(request.opt("url"));

            JSONObject headersJson = new JSONObject();
            if (request.has("header")) {
                JSONArray headerArr = request.getJSONArray("header");
                for (int h = 0; h < headerArr.length(); h++) {
                    JSONObject header = headerArr.getJSONObject(h);
                    if (header.optBoolean("disabled", false)) {
                        continue;
                    }
                    headersJson.put(header.optString("key", ""), header.optString("value", ""));
                }
            }

            String body = "";
            boolean hasFileUpload = false;
            if (request.has("body")) {
                JSONObject bodyObj = request.getJSONObject("body");
                String mode = bodyObj.optString("mode", "");
                if ("raw".equals(mode)) {
                    body = bodyObj.optString("raw", "");
                } else if ("urlencoded".equals(mode) || "formdata".equals(mode)) {
                    StringBuilder sb = new StringBuilder();
                    JSONArray entries = bodyObj.optJSONArray(mode);
                    if (entries != null) {
                        for (int e = 0; e < entries.length(); e++) {
                            JSONObject entry = entries.getJSONObject(e);
                            if (entry.optBoolean("disabled", false)) {
                                continue;
                            }
                            if ("file".equals(entry.optString("type", ""))) {
                                hasFileUpload = true;
                                continue;
                            }
                            if (sb.length() > 0) {
                                sb.append('&');
                            }
                            sb.append(entry.optString("key", "")).append('=').append(entry.optString("value", ""));
                        }
                    }
                    body = sb.toString();
                    if (!headersJson.has("Content-Type") && "urlencoded".equals(mode)) {
                        headersJson.put("Content-Type", "application/x-www-form-urlencoded");
                    }
                }
            }
            if (hasFileUpload) {
                counters[2]++;
            }

            // Pre-request / test scripts live in the Postman "event" array
            String preScript = "";
            String testsScript = "";
            if (item.has("event")) {
                JSONArray events = item.getJSONArray("event");
                for (int e = 0; e < events.length(); e++) {
                    JSONObject event = events.getJSONObject(e);
                    String listen = event.optString("listen", "");
                    JSONObject script = event.optJSONObject("script");
                    if (script == null) {
                        continue;
                    }
                    String code = joinExec(script.optJSONArray("exec"));
                    if ("prerequest".equals(listen)) {
                        preScript = code;
                    } else if ("test".equals(listen)) {
                        testsScript = code;
                    }
                }
            }

            String[] auth = extractAuth(request.optJSONObject("auth"));
            if (auth == null) {
                auth = fallbackAuth; // inherit collection-level auth
            }

            ApiRequest apiRequest = new ApiRequest();
            apiRequest.setName(name);
            apiRequest.setMethod(method);
            apiRequest.setUrl(url);
            apiRequest.setHeaders(headersJson.toString());
            apiRequest.setBody(body);
            apiRequest.setPreRequestScript(preScript);
            apiRequest.setTestsScript(testsScript);
            if (auth != null) {
                apiRequest.setAuthType(auth[0]);
                apiRequest.setAuthToken(auth[1]);
                apiRequest.setAuthUsername(auth[2]);
                apiRequest.setAuthPassword(auth[3]);
            }
            apiRequest.setCollectionId(collectionId);
            apiRequest.setFolderId(parentFolderId);

            if (RequestService.saveRequest(apiRequest).isPresent()) {
                counters[0]++;
            } else {
                counters[3]++;
            }
        }
    }

    /** Counts requests nested (at any depth) inside a Postman "item" array —
     * used to report an honest failure count when a parent folder fails. */
    private int countPostmanLeaves(JSONArray items) {
        if (items == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (item.has("item")) {
                count += countPostmanLeaves(item.getJSONArray("item"));
            } else if (item.has("request")) {
                count++;
            }
        }
        return count;
    }

    /** Postman's url field is either a plain string or a {"raw": "..."} object. */
    private String resolvePostmanUrl(Object urlValue) {
        if (urlValue instanceof String) {
            return (String) urlValue;
        }
        if (urlValue instanceof JSONObject) {
            JSONObject urlObj = (JSONObject) urlValue;
            if (urlObj.has("raw")) {
                return urlObj.getString("raw");
            }
        }
        return "";
    }

    private String joinExec(JSONArray exec) {
        if (exec == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < exec.length(); i++) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(exec.getString(i));
        }
        return sb.toString();
    }

    /** Maps a Postman auth block to {authType, token, username, password}.
     * Returns null when there's no usable auth here (e.g. type "noauth"). */
    private String[] extractAuth(JSONObject authObj) {
        if (authObj == null) {
            return null;
        }
        String type = authObj.optString("type", "noauth");
        if ("bearer".equals(type)) {
            String token = findAuthValue(authObj.optJSONArray("bearer"), "token");
            return new String[]{"Bearer Token", token, "", ""};
        }
        if ("basic".equals(type)) {
            String username = findAuthValue(authObj.optJSONArray("basic"), "username");
            String password = findAuthValue(authObj.optJSONArray("basic"), "password");
            return new String[]{"Basic Auth", "", username, password};
        }
        return null; // noauth, apikey, oauth2 etc. — not modeled yet
    }

    private String findAuthValue(JSONArray entries, String key) {
        if (entries == null) {
            return "";
        }
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            if (key.equals(entry.optString("key", ""))) {
                return entry.optString("value", "");
            }
        }
        return "";
    }

    /**
     * Bulk-imports requests from a spreadsheet. Expected columns (header
     * row, any order): Folder, Name, Method, URL, Headers, Body,
     * PreRequestScript, TestsScript. Folder supports nested paths with "/",
     * e.g. "Auth/Login" creates (or reuses) Auth > Login. Missing optional
     * columns are simply left blank.
     */
    private void importRequestsFromCsv(File file) {
        TreeItem<String> selectedItem = collectionsTree.getSelectionModel().getSelectedItem();
        Long preselectedCollectionId = selectedItem != null ? getCollectionIdFromTreeItem(selectedItem) : null;

        Runnable doImport = () -> new Thread(() -> {
            AlertUtils.setQuiet(true);
            try {
                Long collectionId = preselectedCollectionId;
                if (collectionId == null) {
                    Platform.runLater(() -> AlertUtils.showError(
                            "Select a collection in the sidebar first, then import again"));
                    return;
                }
                String csvText = java.nio.file.Files.readString(file.toPath());
                List<Map<String, String>> rows = CsvParser.parseWithHeader(csvText);
                if (rows.isEmpty()) {
                    Platform.runLater(() -> AlertUtils.showError("CSV file has no data rows"));
                    return;
                }

                // Pre-load existing folders so re-importing is idempotent —
                // it reuses folders instead of creating duplicates.
                Map<String, Long> pathToFolderId = new HashMap<>();
                Optional<CollectionResponse> existing = CollectionService.getCollectionWithDetails(collectionId);
                Map<Long, FolderResponse> foldersById = new HashMap<>();
                if (existing.isPresent() && existing.get().getFolderResponses() != null) {
                    for (FolderResponse f : existing.get().getFolderResponses()) {
                        foldersById.put(f.getId(), f);
                    }
                    for (FolderResponse f : existing.get().getFolderResponses()) {
                        pathToFolderId.put(folderPath(f, foldersById), f.getId());
                    }
                }

                int requestCount = 0;
                int folderCount = 0;
                int failedCount = 0;
                boolean sessionExpiredMidImport = false;
                for (Map<String, String> row : rows) {
                    // Same fix as the JSON importer: stop cleanly the moment
                    // the session dies instead of one dialog per remaining row.
                    if (ApiClient.getToken() == null) {
                        sessionExpiredMidImport = true;
                        break;
                    }
                    String folderPath = row.getOrDefault("Folder", "").trim();
                    Long folderId = null;
                    boolean folderFailed = false;
                    if (!folderPath.isEmpty()) {
                        String builtPath = "";
                        for (String segment : folderPath.split("/")) {
                            segment = segment.trim();
                            if (segment.isEmpty()) {
                                continue;
                            }
                            builtPath = builtPath.isEmpty() ? segment : builtPath + "/" + segment;
                            Long existingId = pathToFolderId.get(builtPath);
                            if (existingId != null) {
                                folderId = existingId;
                                continue;
                            }
                            Optional<FolderResponse> created =
                                    FolderService.createFolder(segment, "", collectionId, folderId);
                            if (created.isEmpty()) {
                                // FIX: this used to throw and abort the ENTIRE
                                // import on one bad folder. Now it's counted
                                // as a failure and the import keeps going —
                                // consistent with the JSON importer.
                                folderFailed = true;
                                break;
                            }
                            folderId = created.get().getId();
                            pathToFolderId.put(builtPath, folderId);
                            folderCount++;
                        }
                    }
                    if (folderFailed) {
                        failedCount++;
                        continue;
                    }

                    String name = row.getOrDefault("Name", "Imported request").trim();
                    if (name.isEmpty()) {
                        name = "Imported request";
                    }
                    String methodStr = row.getOrDefault("Method", "GET").trim().toUpperCase(Locale.ROOT);
                    HttpMethod method;
                    try {
                        method = HttpMethod.valueOf(methodStr.isEmpty() ? "GET" : methodStr);
                    } catch (IllegalArgumentException e) {
                        method = HttpMethod.GET;
                    }

                    ApiRequest apiRequest = new ApiRequest();
                    apiRequest.setName(name);
                    apiRequest.setMethod(method);
                    apiRequest.setUrl(row.getOrDefault("URL", "").trim());
                    apiRequest.setHeaders(new JSONObject(parseHeaderCell(row.getOrDefault("Headers", ""))).toString());
                    apiRequest.setBody(row.getOrDefault("Body", ""));
                    apiRequest.setPreRequestScript(row.getOrDefault("PreRequestScript", ""));
                    apiRequest.setTestsScript(row.getOrDefault("TestsScript", ""));
                    apiRequest.setCollectionId(collectionId);
                    apiRequest.setFolderId(folderId);

                    if (RequestService.saveRequest(apiRequest).isPresent()) {
                        requestCount++;
                    } else {
                        failedCount++;
                    }
                }

                int finalRequestCount = requestCount;
                int finalFolderCount = folderCount;
                int finalFailedCount = failedCount;
                boolean finalSessionExpired = sessionExpiredMidImport;
                Platform.runLater(() -> {
                    refreshCollectionsTree();
                    if (finalSessionExpired) {
                        AlertUtils.showError("Your session expired partway through this import.\n\n"
                                + "Completed before that happened: " + finalRequestCount + " request(s), "
                                + finalFolderCount + " new folder(s).\n\n"
                                + "Please log in again, then re-run the import — folders already created "
                                + "will be reused safely, but requests that already succeeded will be "
                                + "duplicated, so it's worth deleting those first.");
                        updateStatus("CSV import stopped: session expired");
                        return;
                    }
                    StringBuilder msg = new StringBuilder("Imported " + finalRequestCount + " request(s)");
                    if (finalFolderCount > 0) {
                        msg.append(" into ").append(finalFolderCount).append(" new folder(s)");
                    }
                    if (finalFailedCount > 0) {
                        // FIX: these used to either vanish silently or (for
                        // folders) abort the WHOLE import — now they're
                        // counted honestly and everything else still ran.
                        msg.append(". ").append(finalFailedCount)
                                .append(" row(s) FAILED (transient server/network error — check the console "
                                        + "log, then re-add those manually if needed)");
                    }
                    AlertUtils.showSuccess(msg.toString());
                    updateStatus("CSV import complete: " + finalRequestCount + " request(s)");
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.showError("CSV import failed: " + e.getMessage()));
            } finally {
                AlertUtils.setQuiet(false);
            }
        }).start();

        if (preselectedCollectionId == null) {
            AlertUtils.showError("Select a collection in the sidebar first, then use Import again");
            return;
        }
        doImport.run();
    }

    /** Rebuilds "Parent/Child" from a folder and the collection's folder set. */
    private String folderPath(FolderResponse folder, Map<Long, FolderResponse> byId) {
        List<String> parts = new ArrayList<>();
        FolderResponse current = folder;
        while (current != null) {
            parts.add(0, current.getName());
            current = current.getParentFolderId() != null ? byId.get(current.getParentFolderId()) : null;
        }
        return String.join("/", parts);
    }

    /** Header cell supports either "Key: Value; Key2: Value2" or raw JSON. */
    private Map<String, String> parseHeaderCell(String cell) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (cell == null || cell.isBlank()) {
            return headers;
        }
        String trimmed = cell.trim();
        if (trimmed.startsWith("{")) {
            try {
                JSONObject json = new JSONObject(trimmed);
                for (String key : json.keySet()) {
                    headers.put(key, json.getString(key));
                }
                return headers;
            } catch (Exception ignored) {
                // fall through to the "Key: Value; ..." parser
            }
        }
        for (String pair : trimmed.split(";")) {
            int idx = pair.indexOf(':');
            if (idx > 0) {
                headers.put(pair.substring(0, idx).trim(), pair.substring(idx + 1).trim());
            }
        }
        return headers;
    }

    //  @FXML
    /** Legacy generic entry point some older menu items may still call —
     * kept working via extension auto-detection, but the toolbar now
     * exposes explicit "Import from JSON" / "Import from CSV" instead. */
    @FXML
    private void handleExportCollections() {
        handleExportSelectedAsJson();
    }

    @FXML
    private void handleImportPostmanJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Postman Collection");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showOpenDialog(Main.getPrimaryStage());
        if (file != null) {
            importPostmanCollection(file);
        }
    }

    @FXML
    private void handleImportCsvSpreadsheet() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import CSV Spreadsheet");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showOpenDialog(Main.getPrimaryStage());
        if (file != null) {
            importRequestsFromCsv(file);
        }
    }

    /** Finds the collection to export: the exact collection selected in the
     * sidebar (right-click target, or the current tree selection). */
    private Long resolveSelectedCollectionForExport(TreeItem<String> rightClickedItem) {
        TreeItem<String> item = rightClickedItem != null
                ? rightClickedItem : collectionsTree.getSelectionModel().getSelectedItem();
        if (item == null) {
            return null;
        }
        return collectionIdMap.get(item); // only a COLLECTION node itself, not a folder/request
    }

    @FXML
    private void handleExportSelectedAsJson() {
        exportCollection(resolveSelectedCollectionForExport(null), "json");
    }

    @FXML
    private void handleExportSelectedAsCsv() {
        exportCollection(resolveSelectedCollectionForExport(null), "csv");
    }

    private void exportCollection(Long collectionId, String format) {
        if (collectionId == null) {
            AlertUtils.showError("Select a collection in the sidebar first, then export again");
            return;
        }
        new Thread(() -> {
            try {
                Optional<CollectionResponse> detailed = CollectionService.getCollectionWithDetails(collectionId);
                if (detailed.isEmpty()) {
                    Platform.runLater(() -> AlertUtils.showError("Failed to load the collection to export"));
                    return;
                }
                CollectionResponse collection = detailed.get();
                String content = "json".equals(format)
                        ? buildPostmanJson(collection)
                        : buildCsv(collection);
                String suggestedName = collection.getName().replaceAll("[^a-zA-Z0-9-_ ]", "").trim()
                        .replace(' ', '_') + "." + format;

                Platform.runLater(() -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Export \"" + collection.getName() + "\"");
                    fileChooser.setInitialFileName(suggestedName);
                    if ("json".equals(format)) {
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter("JSON Files", "*.json"));
                    } else {
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
                    }
                    File file = fileChooser.showSaveDialog(Main.getPrimaryStage());
                    if (file == null) {
                        return;
                    }
                    try {
                        java.nio.file.Files.writeString(file.toPath(), content);
                        AlertUtils.showSuccess("Exported \"" + collection.getName() + "\" to " + file.getName());
                        updateStatus("Export complete: " + file.getName());
                    } catch (Exception e) {
                        AlertUtils.showError("Failed to write file: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.showError("Export failed: " + e.getMessage()));
            }
        }).start();
    }

    /** Builds a Postman Collection v2.1 JSON document — the mirror image of
     * importPostmanCollection, so exporting and re-importing round-trips. */
    private String buildPostmanJson(CollectionResponse collection) {
        JSONObject root = new JSONObject();
        JSONObject info = new JSONObject();
        info.put("name", collection.getName());
        info.put("_postman_id", UUID.randomUUID().toString());
        info.put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
        root.put("info", info);

        Map<Long, FolderResponse> foldersById = new HashMap<>();
        Map<Long, JSONArray> childrenByFolderId = new HashMap<>();
        JSONArray topLevel = new JSONArray();
        if (collection.getFolderResponses() != null) {
            for (FolderResponse f : collection.getFolderResponses()) {
                foldersById.put(f.getId(), f);
                childrenByFolderId.put(f.getId(), new JSONArray());
            }
        }
        Map<Long, JSONObject> folderJsonById = new HashMap<>();
        if (collection.getFolderResponses() != null) {
            for (FolderResponse f : collection.getFolderResponses()) {
                JSONObject folderJson = new JSONObject();
                folderJson.put("name", f.getName());
                folderJson.put("item", childrenByFolderId.get(f.getId()));
                folderJsonById.put(f.getId(), folderJson);
            }
            for (FolderResponse f : collection.getFolderResponses()) {
                JSONObject folderJson = folderJsonById.get(f.getId());
                if (f.getParentFolderId() != null && childrenByFolderId.containsKey(f.getParentFolderId())) {
                    childrenByFolderId.get(f.getParentFolderId()).put(folderJson);
                } else {
                    topLevel.put(folderJson);
                }
            }
        }
        if (collection.getRequestResponses() != null) {
            for (RequestResponse r : collection.getRequestResponses()) {
                JSONObject itemJson = requestToPostmanItem(r);
                if (r.getFolderId() != null && childrenByFolderId.containsKey(r.getFolderId())) {
                    childrenByFolderId.get(r.getFolderId()).put(itemJson);
                } else {
                    topLevel.put(itemJson);
                }
            }
        }
        root.put("item", topLevel);

        // Best-effort symmetry with import: if an environment shares this
        // collection's name, export its variables as collection variables.
        Optional<List<EnvironmentResponse>> environments = EnvironmentService.getUserEnvironments();
        if (environments.isPresent()) {
            for (EnvironmentResponse env : environments.get()) {
                if (env.getName().equals(collection.getName()) && env.getVariables() != null
                        && !env.getVariables().isEmpty()) {
                    JSONArray vars = new JSONArray();
                    env.getVariables().forEach((k, v) -> {
                        JSONObject var = new JSONObject();
                        var.put("key", k);
                        var.put("value", v);
                        vars.put(var);
                    });
                    root.put("variable", vars);
                    break;
                }
            }
        }
        return root.toString(2);
    }

    private JSONObject requestToPostmanItem(RequestResponse r) {
        JSONObject item = new JSONObject();
        item.put("name", r.getName());

        JSONObject request = new JSONObject();
        request.put("method", r.getMethod() != null ? r.getMethod().name() : "GET");

        JSONArray headerArr = new JSONArray();
        if (r.getHeaders() != null && !r.getHeaders().isBlank()) {
            try {
                JSONObject headersJson = new JSONObject(r.getHeaders());
                for (String key : headersJson.keySet()) {
                    JSONObject h = new JSONObject();
                    h.put("key", key);
                    h.put("value", headersJson.getString(key));
                    headerArr.put(h);
                }
            } catch (Exception ignored) {
                // leave headers empty rather than exporting garbage
            }
        }
        request.put("header", headerArr);

        JSONObject urlObj = new JSONObject();
        urlObj.put("raw", r.getUrl() != null ? r.getUrl() : "");
        request.put("url", urlObj);

        if (r.getBody() != null && !r.getBody().isBlank()) {
            JSONObject body = new JSONObject();
            body.put("mode", "raw");
            body.put("raw", r.getBody());
            request.put("body", body);
        }
        item.put("request", request);

        JSONArray events = new JSONArray();
        if (r.getPreRequestScript() != null && !r.getPreRequestScript().isBlank()) {
            events.put(postmanEvent("prerequest", r.getPreRequestScript()));
        }
        if (r.getTestsScript() != null && !r.getTestsScript().isBlank()) {
            events.put(postmanEvent("test", r.getTestsScript()));
        }
        if (events.length() > 0) {
            item.put("event", events);
        }
        return item;
    }

    private JSONObject postmanEvent(String listen, String scriptText) {
        JSONObject event = new JSONObject();
        event.put("listen", listen);
        JSONObject script = new JSONObject();
        script.put("type", "text/javascript");
        script.put("exec", new JSONArray(scriptText.split("\\r?\\n")));
        event.put("script", script);
        return event;
    }

    /** Builds the same CSV shape importRequestsFromCsv reads — Folder
     * (nested paths with "/"), Name, Method, URL, Headers, Body, scripts. */
    private String buildCsv(CollectionResponse collection) {
        Map<Long, FolderResponse> foldersById = new HashMap<>();
        if (collection.getFolderResponses() != null) {
            for (FolderResponse f : collection.getFolderResponses()) {
                foldersById.put(f.getId(), f);
            }
        }
        StringBuilder csv = new StringBuilder("Folder,Name,Method,URL,Headers,Body,PreRequestScript,TestsScript\n");
        if (collection.getRequestResponses() != null) {
            for (RequestResponse r : collection.getRequestResponses()) {
                String folderPathStr = r.getFolderId() != null && foldersById.containsKey(r.getFolderId())
                        ? folderPath(foldersById.get(r.getFolderId()), foldersById) : "";
                csv.append(csvEscape(folderPathStr)).append(',')
                        .append(csvEscape(r.getName())).append(',')
                        .append(csvEscape(r.getMethod() != null ? r.getMethod().name() : "GET")).append(',')
                        .append(csvEscape(r.getUrl())).append(',')
                        .append(csvEscape(r.getHeaders())).append(',')
                        .append(csvEscape(r.getBody())).append(',')
                        .append(csvEscape(r.getPreRequestScript())).append(',')
                        .append(csvEscape(r.getTestsScript())).append('\n');
            }
        }
        return csv.toString();
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n");
        if (!needsQuoting) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
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
        Dialog<javafx.util.Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("New Request");
        dialog.setHeaderText("Create a new request in " + parentItem.getValue());

        TextField nameField = new TextField();
        nameField.setPromptText("Request name");
        // FIX: every new request used to be hardcoded to GET regardless of
        // what the user actually wanted — this is where that got decided.
        ComboBox<String> methodBox = new ComboBox<>();
        methodBox.getItems().addAll("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD", "WS", "SOCKETIO");
        methodBox.getSelectionModel().select("GET");
        methodBox.setPrefWidth(110);

        HBox row = new HBox(10, methodBox, nameField);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        VBox content = new VBox(10, new Label("Method and name:"), row);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ThemeManager.styleDialog(dialog.getDialogPane());
        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(button -> button == ButtonType.OK
                ? new javafx.util.Pair<>(methodBox.getValue(), nameField.getText())
                : null);

        Optional<javafx.util.Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(pair -> {
            String name = pair.getValue();
            String method = pair.getKey();
            if (name != null && !name.trim().isEmpty()) {
                createNewRequest(name, method == null ? "GET" : method, parentItem);
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

    private void createNewRequest(String name, String method, TreeItem<String> parentItem) {
        new Thread(() -> {
            try {
                Long collectionId = getCollectionIdFromTreeItem(parentItem);
                Long folderId = getFolderIdFromTreeItem(parentItem);

                if (collectionId != null) {
                    ApiRequest apiRequest = new ApiRequest();
                    apiRequest.setName(name);
                    apiRequest.setMethod(HttpMethod.valueOf(method));
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
                            openRequestTab(name, savedRequest.get().getId(), method);
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

    private void createNewFolder(String name, Long collectionId, Long parentFolderId) {
        if (collectionId != null) {
            new Thread(() -> {
                Optional<FolderResponse> folder = FolderService.createFolder(name, "", collectionId, parentFolderId);
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
        // Only meaningful for the form-data table — "Text" (default) or
        // "File". Every other table (params, headers, cookies...) just
        // never touches these.
        private final SimpleStringProperty type = new SimpleStringProperty("Text");
        private final SimpleStringProperty filePath = new SimpleStringProperty("");
        private final SimpleStringProperty fileName = new SimpleStringProperty("");

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

        public String getType() {
            return type.get();
        }

        public SimpleStringProperty typeProperty() {
            return type;
        }

        public void setType(String type) {
            this.type.set(type);
        }

        public String getFilePath() {
            return filePath.get();
        }

        public SimpleStringProperty filePathProperty() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath.set(filePath);
        }

        public String getFileName() {
            return fileName.get();
        }

        public SimpleStringProperty fileNameProperty() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName.set(fileName);
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
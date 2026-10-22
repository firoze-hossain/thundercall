package com.roze.thundercall.ui.controllers;

import com.roze.thundercall.ui.Main;
import com.roze.thundercall.ui.dialogs.WorkspaceSetupDialog;
import com.roze.thundercall.ui.enums.HttpMethod;
import com.roze.thundercall.ui.models.*;
import com.roze.thundercall.ui.services.*;
import com.roze.thundercall.ui.utils.*;
import com.roze.thundercall.ui.update.AppUpdateService;
import com.roze.thundercall.ui.update.AppUpdateDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.util.Duration;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    @FXML
    private BorderPane mainRootPane;
    @FXML
    private Button addRequestBtn;
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
    private Button saveRequestBtn;
    @FXML
    private Button samplePostBtn;
    @FXML
    private TabPane responseTabPane;
    @FXML
    private Tab responseBodyTab;
    @FXML
    private Tab wsMessagesTab;
    @FXML
    private Tab socketIoMessagesTab;
    @FXML
    private Tab responseCookiesTab;
    @FXML
    private Tab responseHeadersTab;
    @FXML
    private Tab responseTestsTab;
    @FXML
    private Label wsStatusLabel;
    @FXML
    private ListView<String> wsMessageLog;
    @FXML
    private TextArea wsMessageComposeArea;
    @FXML
    private Tab wsMessageTab;
    @FXML
    private Tab socketIoMessageTab;
    @FXML
    private Tab socketIoEventsTab;
    @FXML
    private Tab paramsTab;
    @FXML
    private Tab authTab;
    @FXML
    private Tab headersTab;
    @FXML
    private Tab bodyTab;
    @FXML
    private Tab scriptsTab;
    @FXML
    private TabPane requestBuilderTabPane;
    @FXML
    private Button socketIoConnectBtn;
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
    private TextArea socketIoMessageComposeArea;
    @FXML
    private TableView<SocketIoEventItem> socketIoEventsTable;
    private final ObservableList<SocketIoEventItem> socketIoEventsData = FXCollections.observableArrayList();
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
    private VBox noneBodyPane;
    @FXML
    private HBox bodyTableButtonBar;
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
    private JsonFoldManager responseFoldManager;
    @FXML
    private ListView<String> historyList;
    @FXML
    private TextArea preRequestScriptsArea;
    @FXML
    private TextArea postRequestScriptsArea;
    @FXML
    private ListView<String> testResultsList;
    @FXML
    private SplitPane responseBodySplitPane;
    @FXML
    private WebView responsePreview;
    @FXML
    private WebView responseVisualizationView;
    @FXML
    private MenuButton rawFormatMenu;
    @FXML
    private Button rawBeautifyBtn;
    @FXML
    private MenuButton responseFormatMenu;
    @FXML
    private ToggleButton responsePreviewToggle;
    @FXML
    private ToggleButton responseVisualizerToggle;
    @FXML
    private ToggleButton responseWrapToggle;
    @FXML
    private StackPane responseCodeAreaPane;
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
    // ---- Profile & presence ----
    @FXML
    private MenuButton userMenuBtn;
    @FXML
    private ImageView userAvatarView;
    @FXML
    private FlowPane onlineUsersBox;
    /** The logged-in user's profile as last fetched from /users/me. */
    private UserProfile currentUserProfile;
    /** User ids currently online (teammates / workspace collaborators) —
     * shared by the sidebar bubbles and the team-members dialog dots. */
    private final Set<Long> onlineUserIds = Collections.synchronizedSet(new HashSet<>());
    private javafx.animation.Timeline presenceRefreshTimeline;
    @FXML
    private Label workspaceNameLabel;
    @FXML
    private TabPane requestTabPane;
    @FXML
    private ListView<EnvironmentResponse> environmentsList;
    private final Map<Tab, RequestTabState> tabStates = new HashMap<>();
    private boolean switchingTabs = false;
    private SessionData currentSession;
    private boolean restoringSession = false;
    private boolean restoringTreeSelection = false;
    private final PauseTransition sessionSaveDebounce = new PauseTransition(Duration.millis(500));
    private final Image collectionIconImg = loadTreeIcon("/images/collection-icon.png");
    private final Image folderIconImg = loadTreeIcon("/images/folder-icon.png");

    private static Image loadTreeIcon(String path) {
        try {
            return new Image(
                    Objects.requireNonNull(MainController.class.getResourceAsStream(path)));
        } catch (Exception e) {
            return null;
        }
    }
    private Map<String, EnvironmentResponse> environmentsMap = new HashMap<>();
    // Postman-style Global scope: a reserved, hidden "Globals" environment
    // per workspace — reuses all the existing Environment CRUD/backend
    // instead of a whole separate global-variables concept. Never shown in
    // environmentCombo/environmentsList (see populateEnvironmentsUi).
    private EnvironmentResponse globalsEnvironment;
    // Collection-scoped variables, fetched lazily the first time a request
    // from that collection is opened (see ensureCollectionVariablesLoaded).
    private final Map<Long, Map<String, String>> collectionVariablesCache = new HashMap<>();
    // Postman-style inline body comments, keyed by requestId — root
    // threads only, each carrying its own replies (see CommentResponse).
    private final Map<Long, List<CommentResponse>> commentThreadsCache = new HashMap<>();
    // Paints the yellow "has a comment" highlight on top of the body
    // editor's JSON syntax highlighting, and reopens a thread on click.
    private CommentHighlighter bodyCommentHighlighter;
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
    @FXML
    private HBox graphqlToolbar;
    @FXML
    private Button graphqlAutoFetchBtn;
    @FXML
    private Button graphqlRefreshBtn;
    @FXML
    private Label graphqlWarningLabel;
    @FXML
    private Tooltip graphqlWarningTooltip;

    private boolean isGraphqlAutoFetchEnabled = true;
    private String graphqlSchemaStatusText = "No schema";
    private final List<String> introspectedTypeNames = new ArrayList<>();
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
    private VBox teamWorkspacesPane;
    @FXML
    private ListView<WorkspaceAccessResponse> teamWorkspacesList;
    @FXML
    private ToggleButton railTeamWorkspacesBtn;
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
                if (AppUpdateService.isAutoCheckEnabled()) {
                    Thread.startVirtualThread(() -> {
                        try {
                            Thread.sleep(2500);
                            var update = AppUpdateService.checkForUpdate();
                            if (update != null && update.available()) {
                                Platform.runLater(() -> AppUpdateDialog.show(urlField.getScene().getWindow(), update));
                            }
                        } catch (Exception ignored) { }
                    });
                }
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
                // New request tab: Ctrl+T, like Postman
                urlField.getScene().getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(
                                javafx.scene.input.KeyCode.T,
                                javafx.scene.input.KeyCombination.CONTROL_DOWN),
                        () -> openRequestTab("Untitled Request", null));
                // Close active tab: Ctrl+W, like Postman
                urlField.getScene().getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(
                                javafx.scene.input.KeyCode.W,
                                javafx.scene.input.KeyCombination.CONTROL_DOWN),
                        () -> closeTabSafely(activeRequestTab(), false));
                // Force-close active tab: Alt+Ctrl+W, like Postman
                urlField.getScene().getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(
                                javafx.scene.input.KeyCode.W,
                                javafx.scene.input.KeyCombination.CONTROL_DOWN,
                                javafx.scene.input.KeyCombination.ALT_DOWN),
                        () -> closeTabSafely(activeRequestTab(), true));
            }
        });
        if (collectionsSearchField != null) {
            collectionsSearchField.textProperty()
                    .addListener((obs, oldVal, newVal) -> applyCollectionsFilter(newVal));
        }
        setupRequestTabs();
        setupUrlField();
        setupTopBarTooltips();
        setupBodyCodeArea();
        setupGraphqlCodeAreas();
        setupResponseCodeArea();
        setupScriptEditorsContextMenu();
        setupEnvironmentsList();
        setupTeamsList();
        setupMockServersList();
        setupMonitorsList();
        setupTeamWorkspacesList();

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
            setupSessionPersistence();
            loadUserData();
            loadUserProfile();
            startPresenceUpdates();
            updateStatus("Ready");
            checkUserOnboarding();

            // Load data from server only after ensuring user is authenticated
            loadCollectionsFromServer();
            loadEnvironments();
            loadHistory();
            loadTeams();
            loadMockServers();
            loadMonitors();
            loadTeamWorkspaces();

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
            boolean isRequest = selected != null && requestIdMap.containsKey(selected);
            exportJsonItem.setVisible(isCollection);
            exportCsvItem.setVisible(isCollection);
            // A request can't contain a folder — Postman doesn't offer
            // this on a request either.
            addFolderItem.setVisible(!isRequest);
        });

        // Set context menu on TreeView
        collectionsTree.setContextMenu(treeContextMenu);
    }

    private void setupBodyTypeHandling() {
        bodyTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updateBodyViewsVisibility(newToggle != null ? ((ToggleButton) newToggle).getText() : "None");
            onEditorFieldChanged();
        });

        // Initialize form data and urlencoded tables
        setupFormDataTable();
        setupUrlEncodedTable();
        setupGraphqlToolbar();

        // Default to "None" so the state is always visible
        if (bodyTypeGroup.getSelectedToggle() == null && noneBodyTypeButton != null) {
            noneBodyTypeButton.setSelected(true);
        } else {
            updateBodyViewsVisibility(bodyTypeGroup.getSelectedToggle() != null
                    ? ((ToggleButton) bodyTypeGroup.getSelectedToggle()).getText() : "None");
        }

        // When user types in raw body while Raw is active, keep Raw selected
        bodyTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!restoringTabState && newVal != null && !newVal.isBlank() && rawBodyTypeButton != null && bodyTextArea.isVisible()) {
                Toggle selected = bodyTypeGroup.getSelectedToggle();
                if (selected == null || selected == noneBodyTypeButton) {
                    rawBodyTypeButton.setSelected(true);
                }
            }
        });
    }

    private void updateBodyViewsVisibility(String selectedType) {
        if (selectedType == null) {
            selectedType = "None";
        }
        boolean isNone = "None".equalsIgnoreCase(selectedType);
        boolean isRaw = "Raw".equalsIgnoreCase(selectedType);
        boolean isFormData = "Form Data".equalsIgnoreCase(selectedType);
        boolean isUrlEncoded = "x-www-form-urlencoded".equalsIgnoreCase(selectedType);
        boolean isGraphql = "GraphQL".equalsIgnoreCase(selectedType);

        if (noneBodyPane != null) {
            noneBodyPane.setVisible(isNone);
            noneBodyPane.setManaged(isNone);
        }
        if (bodyTextArea != null) {
            bodyTextArea.setVisible(isRaw);
            bodyTextArea.setManaged(isRaw);
        }
        if (rawFormatMenu != null) {
            rawFormatMenu.setVisible(isRaw);
            rawFormatMenu.setManaged(isRaw);
        }
        if (rawBeautifyBtn != null) {
            rawBeautifyBtn.setVisible(isRaw);
            rawBeautifyBtn.setManaged(isRaw);
        }
        if (formDataTable != null) {
            formDataTable.setVisible(isFormData);
            formDataTable.setManaged(isFormData);
            if (isFormData) {
                ensureFormDataPlaceholderRow();
                updateFormDataMasterCheckBox(formDataMasterCheckBox);
                updateFormDataMasterSelectBox(formDataMasterSelectBox);
            }
        }
        if (urlEncodedTable != null) {
            urlEncodedTable.setVisible(isUrlEncoded);
            urlEncodedTable.setManaged(isUrlEncoded);
            if (isUrlEncoded) {
                ensureUrlEncodedPlaceholderRow();
                updateUrlEncodedMasterCheckBox(urlEncodedMasterCheckBox);
                updateUrlEncodedMasterSelectBox(urlEncodedMasterSelectBox);
            }
        }
        if (graphqlPane != null) {
            graphqlPane.setVisible(isGraphql);
            graphqlPane.setManaged(isGraphql);
        }
        if (graphqlToolbar != null) {
            graphqlToolbar.setVisible(isGraphql);
            graphqlToolbar.setManaged(isGraphql);
        }
        if (bodyTableButtonBar != null) {
            boolean showButtons = isFormData || isUrlEncoded;
            bodyTableButtonBar.setVisible(showButtons);
            bodyTableButtonBar.setManaged(showButtons);
        }
        if (!isGraphql && graphqlWarningTooltip != null && graphqlWarningTooltip.isShowing()) {
            graphqlWarningTooltip.hide();
        }
        if (isGraphql) {
            if (methodCombo != null) {
                methodCombo.setValue("POST");
            }
            if (isGraphqlAutoFetchEnabled) {
                triggerGraphqlAutoFetch(false);
            }
        }
    }

    private void loadCollectionsFromServer() {
        // Shared workspace context: the owner-scoped list endpoint would
        // return nothing for a workspace you don't own, so this uses the
        // same read path the Team Spaces browse view already relies on.
        if (WorkspaceManager.isViewingSharedWorkspace() && WorkspaceManager.hasWorkspace()) {
            Long workspaceId = WorkspaceManager.getCurrentWorkspace().getId();
            new Thread(() -> {
                Optional<WorkspaceContentsResponse> contents = WorkspaceSharingService.getWorkspaceContents(workspaceId);
                if (contents.isPresent()) {
                    populateCollectionsTree(contents.get().getCollections() != null
                            ? contents.get().getCollections() : new ArrayList<>());
                } else {
                    Platform.runLater(() -> AlertUtils.showError("Failed to load this shared workspace's collections"));
                }
            }).start();
            return;
        }
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
            if (fullCollectionsRoot != null) {
                captureExpandedKeys(fullCollectionsRoot, expandedKeys);
            } else if (currentSession != null && currentSession.getExpandedTreeKeys() != null) {
                expandedKeys.addAll(currentSession.getExpandedTreeKeys());
            }

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

            // Restore sidebar tree selection (active tab or saved tree key)
            String targetKey = (currentSession != null) ? currentSession.getSelectedTreeKey() : null;
            if (targetKey == null) {
                Tab active = activeRequestTab();
                RequestTabState activeState = active != null ? tabStates.get(active) : null;
                if (activeState != null && activeState.requestId != null) {
                    targetKey = "r:" + activeState.requestId;
                }
            }
            if (targetKey != null) {
                restoreCollectionsTreeSelection(targetKey);
            }
        });
    }

    private void loadEnvironments() {
        // Shared workspace context: environments are opt-in only, and
        // getWorkspaceContents() already returns exactly the ones this
        // user was actually granted — same read path Team Spaces uses.
        if (WorkspaceManager.isViewingSharedWorkspace() && WorkspaceManager.hasWorkspace()) {
            Long workspaceId = WorkspaceManager.getCurrentWorkspace().getId();
            new Thread(() -> {
                Optional<WorkspaceContentsResponse> contents = WorkspaceSharingService.getWorkspaceContents(workspaceId);
                if (contents.isPresent()) {
                    populateEnvironmentsUi(contents.get().getEnvironments() != null
                            ? contents.get().getEnvironments() : new ArrayList<>());
                } else {
                    Platform.runLater(() -> AlertUtils.showError("Failed to load this shared workspace's environments"));
                }
            }).start();
            return;
        }
        new Thread(() -> {
            try {
                Optional<List<EnvironmentResponse>> environments = EnvironmentService.getUserEnvironments();
                environments.ifPresent(this::populateEnvironmentsUi);
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Loading environments"));
            }
        }).start();
    }

    private void populateEnvironmentsUi(List<EnvironmentResponse> environments) {
        Platform.runLater(() -> {
            // FIX: this used to clear the combo and add ONLY the real
            // environments, silently dropping "No Environment" (which
            // setupEnvironmentCombo() only ever added once, at startup,
            // before any real environments existed). Every reload after
            // that point left no way to opt out of using an environment
            // at all — exactly the opposite of Postman, where "No
            // Environment" is always available as a real choice.
            String previousSelection = environmentCombo.getValue();
            environmentCombo.getItems().clear();
            environmentsMap.clear();

            environmentCombo.getItems().add("No Environment");
            for (EnvironmentResponse env : environments) {
                // The reserved Globals store (Postman's Global scope) is
                // real data but never appears as a switchable environment.
                if ("Globals".equals(env.getName())) {
                    globalsEnvironment = env;
                    continue;
                }
                if (env.getIsActive()) {
                    environmentCombo.getItems().add(env.getName());
                    environmentsMap.put(env.getName(), env);
                }
            }

            if (environmentsList != null) {
                environmentsList.getItems().setAll(
                        environments.stream().filter(e -> !"Globals".equals(e.getName())).toList());
            }

            String savedEnv = (currentSession != null) ? currentSession.getSelectedEnvironment() : null;
            if (savedEnv != null && environmentCombo.getItems().contains(savedEnv)) {
                environmentCombo.setValue(savedEnv);
                updateEnvironmentVariables(savedEnv);
            } else if (previousSelection != null && environmentCombo.getItems().contains(previousSelection)) {
                environmentCombo.setValue(previousSelection);
                updateEnvironmentVariables(previousSelection);
            } else {
                environmentCombo.getSelectionModel().select(0);
                updateEnvironmentVariables(environmentCombo.getValue());
            }
        });
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
                            Workspace current = WorkspaceManager.getCurrentWorkspace();
                            boolean currentBelongsToUser = current != null
                                    && workspaces.stream().anyMatch(w -> Objects.equals(w.getId(), current.getId()));

                            if (!currentBelongsToUser) {
                                Workspace target = null;
                                if (currentSession != null && currentSession.getActiveWorkspaceId() != null) {
                                    Long savedWsId = currentSession.getActiveWorkspaceId();
                                    target = workspaces.stream()
                                            .filter(w -> savedWsId.equals(w.getId()))
                                            .findFirst()
                                            .orElse(null);
                                }
                                if (target == null && !workspaces.isEmpty()) {
                                    target = workspaces.get(0);
                                }
                                WorkspaceManager.setCurrentWorkspace(target);
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

    /** Rebuilds the Workspaces ▾ menu with the user's real workspaces,
     * plus anything shared with them — switching to one of those puts
     * the whole app (Collections, Environments, creating/editing
     * things) into that workspace's context instead of your own,
     * respecting your Editor/Viewer role there. */
    private void refreshWorkspacesMenu(List<Workspace> workspaces) {
        if (workspacesMenuBtn == null) {
            return;
        }
        workspacesMenuBtn.getItems().clear();
        for (Workspace ws : workspaces) {
            MenuItem item = new MenuItem(ws.getName());
            if (!WorkspaceManager.isViewingSharedWorkspace() && WorkspaceManager.getCurrentWorkspace() != null
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

        // Shared workspaces get their own section — loaded async since
        // this menu rebuild itself runs on the FX thread.
        new Thread(() -> {
            Optional<List<WorkspaceAccessResponse>> shared = WorkspaceSharingService.getSharedWithMe();
            Platform.runLater(() -> {
                List<WorkspaceAccessResponse> sharedList = shared.orElse(List.of());
                if (sharedList.isEmpty()) {
                    return;
                }
                workspacesMenuBtn.getItems().add(new SeparatorMenuItem());
                Menu sharedMenu = new Menu("Shared with me");
                for (WorkspaceAccessResponse access : sharedList) {
                    MenuItem item = new MenuItem(access.getWorkspaceName() + "  (" + access.getRole() + ")");
                    if (WorkspaceManager.isViewingSharedWorkspace() && WorkspaceManager.getCurrentWorkspace() != null
                            && Objects.equals(WorkspaceManager.getCurrentWorkspace().getId(), access.getWorkspaceId())) {
                        item.setText("✓ " + access.getWorkspaceName() + "  (" + access.getRole() + ")");
                    }
                    item.setOnAction(e -> switchToSharedWorkspace(access));
                    sharedMenu.getItems().add(item);
                }
                workspacesMenuBtn.getItems().add(sharedMenu);
            });
        }).start();
    }

    private void switchWorkspace(Workspace workspace) {
        WorkspaceManager.setCurrentWorkspace(workspace);
        updateWorkspaceLabels();
        reloadWorkspacesAndCollections();
        updateStatus("Workspace: " + workspace.getName());
        scheduleSessionSave();
    }

    /** Switches the whole app into a workspace someone else shared
     * with you — same Collections/Environments panels, same
     * create/save actions, just pointed at their data instead of
     * yours, and respecting whatever role you were granted. */
    private void switchToSharedWorkspace(WorkspaceAccessResponse access) {
        Workspace asWorkspace = new Workspace();
        asWorkspace.setId(access.getWorkspaceId());
        asWorkspace.setName(access.getWorkspaceName());
        WorkspaceManager.setCurrentSharedWorkspace(asWorkspace, access.getRole());
        updateWorkspaceLabels();
        if (workspaceNameLabel != null) {
            workspaceNameLabel.setText(access.getWorkspaceName() + "  (" + access.getRole()
                    + " · owner: " + access.getWorkspaceOwnerUsername() + ")");
        }
        loadCollectionsFromServer();
        loadEnvironments();
        updateStatus("Now working in \"" + access.getWorkspaceName() + "\" as " + access.getRole());
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
            HoverTooltip.convertAll(tourView);
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
        // Single-line URL bar: keep vertically locked so text/underscores never clip or shift
        urlField.showParagraphAtTop(0);
        urlField.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() != 0) {
                e.consume(); // prevent vertical scrolling inside the single-line URL field
            }
        });
        urlField.plainTextChanges().subscribe(change -> {
            urlField.setStyleSpans(0, JsonSyntaxHighlighter.computeUrlHighlighting(urlField.getText()));
            urlField.showParagraphAtTop(0);
            if (!syncingUrlAndParams && (urlField.isFocused() || restoringTabState)) {
                syncUrlToParams();
            }
        });
        VariableAutocomplete.attach(urlField, this::currentEnvironmentVariables);
        if (urlPromptLabel != null) {
            urlPromptLabel.visibleProperty().bind(
                    javafx.beans.binding.Bindings.createBooleanBinding(
                            () -> urlField.getText().isEmpty() && !urlField.isFocused(),
                            urlField.textProperty(),
                            urlField.focusedProperty()));
        }
        // Enter in the URL bar sends the request, like Postman; Up/Down prevented from vertical scrolling
        urlField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.UP || e.getCode() == javafx.scene.input.KeyCode.DOWN) {
                e.consume();
            } else if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleSendRequest();
                e.consume();
            }
        });
        urlField.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            if (!isFocused && isGraphqlActive() && isGraphqlAutoFetchEnabled) {
                triggerGraphqlAutoFetch(false);
            }
        });
    }

    /** Request body editor: full JSON + {{variable}} coloring, autocomplete,
     * and auto-closing braces. */
    private void setupBodyCodeArea() {
        // Postman-style line numbers down the left edge of the body editor.
        bodyTextArea.setParagraphGraphicFactory(LineNumberFactory.get(bodyTextArea));
        bodyCommentHighlighter = new CommentHighlighter(bodyTextArea, this::handleOpenCommentThread);
        bodyTextArea.plainTextChanges().subscribe(change -> bodyCommentHighlighter.refresh());
        VariableAutocomplete.attach(bodyTextArea, this::currentEnvironmentVariables);
        AutoPairing.attach(bodyTextArea);
        attachPrettifyShortcut(bodyTextArea);
        if (bodyPromptLabel != null) {
            bodyPromptLabel.visibleProperty().bind(
                    javafx.beans.binding.Bindings.createBooleanBinding(
                            () -> bodyTextArea.isVisible() && bodyTextArea.getText().isEmpty(),
                            bodyTextArea.visibleProperty(), bodyTextArea.textProperty()));
            bodyPromptLabel.managedProperty().bind(bodyPromptLabel.visibleProperty());
        }
        // Postman-style right-click menu: Comment, Set as variable, Cut/Copy/
        // Paste, EncodeURIComponent/DecodeURIComponent, Find.
        EditorContextMenu.attach(TextEditTarget.of(bodyTextArea), true, false, bodyMenuExtensions());
        // Ctrl+/ toggles "//" line-comments — independent of the "Comment"
        // annotation feature above, exactly like Postman.
        attachCommentToggleShortcut(bodyTextArea, TextEditTarget.of(bodyTextArea));
        if (bodyCommentHighlighter != null) {
            bodyCommentHighlighter.refresh();
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
        graphqlQueryArea.setParagraphGraphicFactory(LineNumberFactory.get(graphqlQueryArea));
        graphqlVariablesArea.setParagraphGraphicFactory(LineNumberFactory.get(graphqlVariablesArea));
        VariableAutocomplete.attach(graphqlQueryArea, this::currentEnvironmentVariables);
        AutoPairing.attach(graphqlQueryArea);
        graphqlVariablesArea.plainTextChanges().subscribe(change ->
                graphqlVariablesArea.setStyleSpans(0,
                        JsonSyntaxHighlighter.computeHighlighting(graphqlVariablesArea.getText())));
        VariableAutocomplete.attach(graphqlVariablesArea, this::currentEnvironmentVariables);
        AutoPairing.attach(graphqlVariablesArea);
        attachPrettifyShortcut(graphqlVariablesArea);

        // Set as variable / Cut/Copy/Paste/Encode/Decode/Find — the inline
        // "Comment" annotation thread feature is body-only for now (see
        // handleAddComment), so these two don't get that particular item.
        EditorContextMenu.attach(TextEditTarget.of(graphqlQueryArea), false, false, bodyMenuExtensions());
        EditorContextMenu.attach(TextEditTarget.of(graphqlVariablesArea), false, false, bodyMenuExtensions());
        attachCommentToggleShortcut(graphqlQueryArea, TextEditTarget.of(graphqlQueryArea));
        attachCommentToggleShortcut(graphqlVariablesArea, TextEditTarget.of(graphqlVariablesArea));
    }

    private void setupGraphqlToolbar() {
        if (graphqlAutoFetchBtn == null || graphqlRefreshBtn == null) {
            return;
        }

        // Context menu for Auto Fetch button
        ContextMenu autoFetchMenu = new ContextMenu();
        CheckMenuItem autoFetchItem = new CheckMenuItem("Auto Fetch");
        autoFetchItem.setSelected(isGraphqlAutoFetchEnabled);
        autoFetchItem.setOnAction(e -> {
            isGraphqlAutoFetchEnabled = autoFetchItem.isSelected();
            if (isGraphqlAutoFetchEnabled) {
                triggerGraphqlAutoFetch(false);
            }
        });

        MenuItem schemaStatusItem = new MenuItem(graphqlSchemaStatusText);
        schemaStatusItem.setDisable(true);

        autoFetchMenu.getItems().addAll(autoFetchItem, new SeparatorMenuItem(), schemaStatusItem);

        autoFetchMenu.setOnShowing(e -> {
            autoFetchItem.setSelected(isGraphqlAutoFetchEnabled);
            schemaStatusItem.setText(graphqlSchemaStatusText);
        });

        graphqlAutoFetchBtn.setOnAction(e -> {
            if (autoFetchMenu.isShowing()) {
                autoFetchMenu.hide();
            } else {
                schemaStatusItem.setText(graphqlSchemaStatusText);
                autoFetchItem.setSelected(isGraphqlAutoFetchEnabled);
                autoFetchMenu.show(graphqlAutoFetchBtn, Side.BOTTOM, 0, 4);
            }
        });

        graphqlRefreshBtn.setOnAction(e -> {
            triggerGraphqlAutoFetch(true);
        });

        if (graphqlRefreshBtn != null) {
            attachHoverTooltip(graphqlRefreshBtn, "Refresh");
        }

        if (graphqlWarningLabel != null) {
            graphqlWarningLabel.setVisible(true);
            graphqlWarningLabel.setManaged(true);
            graphqlWarningLabel.setTooltip(null); // Detach default TooltipBehavior to eliminate any flickering

            if (graphqlWarningTooltip != null) {
                graphqlWarningTooltip.setText(
                        "Could not auto fetch. Make sure Authorization, URL & selected environment are valid. Check console for more details.");
                graphqlWarningTooltip.setAutoHide(false);
            }

            PauseTransition showDelay = new PauseTransition(Duration.millis(80));
            PauseTransition hideDelay = new PauseTransition(Duration.millis(150));

            showDelay.setOnFinished(e -> {
                if (!graphqlWarningLabel.isVisible() || graphqlWarningTooltip == null) {
                    return;
                }
                Point2D p = graphqlWarningLabel.localToScreen(0, graphqlWarningLabel.getHeight());
                if (p != null) {
                    double x = Math.max(10, p.getX() - 110);
                    double y = p.getY() + 6;
                    if (!graphqlWarningTooltip.isShowing()) {
                        graphqlWarningTooltip.show(graphqlWarningLabel, x, y);
                    }
                }
            });

            hideDelay.setOnFinished(e -> {
                if (graphqlWarningTooltip != null && graphqlWarningTooltip.isShowing()) {
                    graphqlWarningTooltip.hide();
                }
            });

            graphqlWarningLabel.setOnMouseEntered(e -> {
                hideDelay.stop();
                if (graphqlWarningTooltip != null && !graphqlWarningTooltip.isShowing()) {
                    showDelay.playFromStart();
                }
            });

            graphqlWarningLabel.setOnMouseExited(e -> {
                showDelay.stop();
                hideDelay.playFromStart();
            });

            if (graphqlWarningTooltip != null) {
                graphqlWarningTooltip.setOnShowing(ev -> {
                    if (graphqlWarningTooltip.getScene() != null) {
                        ThemeManager.apply(graphqlWarningTooltip.getScene());
                        if (graphqlWarningTooltip.getScene().getRoot() != null) {
                            Node root = graphqlWarningTooltip.getScene().getRoot();
                            root.setOnMouseEntered(me -> hideDelay.stop());
                            root.setOnMouseExited(me -> hideDelay.playFromStart());
                        }
                    }
                });
            }

            // Also allow clicking the warning icon to toggle/pin the tooltip
            graphqlWarningLabel.setOnMouseClicked(e -> {
                showDelay.stop();
                hideDelay.stop();
                if (graphqlWarningTooltip != null) {
                    if (graphqlWarningTooltip.isShowing()) {
                        graphqlWarningTooltip.hide();
                    } else {
                        Point2D p = graphqlWarningLabel.localToScreen(0, graphqlWarningLabel.getHeight());
                        if (p != null) {
                            graphqlWarningTooltip.show(graphqlWarningLabel, Math.max(10, p.getX() - 110), p.getY() + 6);
                        }
                    }
                }
            });
        }
    }

    private void setupTopBarTooltips() {
        if (saveRequestBtn != null) {
            HoverTooltip.attach(saveRequestBtn, "Save request");
        }
        if (samplePostBtn != null) {
            HoverTooltip.attach(samplePostBtn, "Load sample POST request");
        }
        if (addRequestBtn != null) {
            HoverTooltip.attach(addRequestBtn, "New request");
        }
        if (mainRootPane != null) {
            HoverTooltip.convertAll(mainRootPane);
        }
        Platform.runLater(() -> {
            if (mainRootPane != null) {
                HoverTooltip.convertAll(mainRootPane);
            }
        });
    }

    /**
     * Attaches a zero-flicker, persistent hover tooltip to a control.
     */
    private void attachHoverTooltip(Control control, String text) {
        HoverTooltip.attach(control, text);
    }

    private void triggerGraphqlAutoFetch(boolean userTriggered) {
        String rawUrl = buildFullUrl();
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = urlField != null ? urlField.getText().trim() : "";
        }

        Map<String, String> vars = currentEnvironmentVariables();
        String resolvedUrl = VariableResolver.resolve(rawUrl, vars);
        Set<String> unresolved = VariableResolver.findUnresolved(resolvedUrl, vars);

        if (rawUrl.isBlank() || !unresolved.isEmpty() || !isValidHttpUrl(resolvedUrl)) {
            Platform.runLater(() -> {
                graphqlSchemaStatusText = "No schema";
                if (graphqlWarningLabel != null) {
                    graphqlWarningLabel.setVisible(true);
                    graphqlWarningLabel.setManaged(true);
                }
            });
            if (userTriggered) {
                logger.warning("GraphQL auto fetch failed: URL is empty, contains unresolved variables, or is not a valid HTTP URL: " + rawUrl);
            }
            return;
        }

        Map<String, String> resolvedHeaders = VariableResolver.resolveMap(buildHeadersForSend(), vars);

        // Spin refresh button for visual feedback
        if (graphqlRefreshBtn != null && graphqlRefreshBtn.getGraphic() != null) {
            RotateTransition rt = new RotateTransition(Duration.millis(800), graphqlRefreshBtn.getGraphic());
            rt.setByAngle(360);
            rt.setCycleCount(2);
            rt.play();
        }

        String introspectionPayload = "{\"query\":\"query IntrospectionQuery { __schema { queryType { name } mutationType { name } subscriptionType { name } types { kind name description } } }\"}";

        Thread t = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(4))
                        .build();

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(resolvedUrl))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(introspectionPayload, StandardCharsets.UTF_8));

                if (resolvedHeaders != null) {
                    for (Map.Entry<String, String> entry : resolvedHeaders.entrySet()) {
                        String k = entry.getKey();
                        if (k != null && !k.equalsIgnoreCase("Content-Type")
                                && !k.equalsIgnoreCase("Content-Length")
                                && !k.equalsIgnoreCase("Host")) {
                            try {
                                reqBuilder.header(k, entry.getValue() != null ? entry.getValue() : "");
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }

                HttpResponse<String> resp = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body() != null && resp.body().contains("__schema")) {
                    JSONObject json = new JSONObject(resp.body());
                    JSONObject data = json.optJSONObject("data");
                    JSONObject schema = data != null ? data.optJSONObject("__schema") : null;
                    JSONArray types = schema != null ? schema.optJSONArray("types") : null;
                    int count = types != null ? types.length() : 0;
                    List<String> names = new ArrayList<>();
                    if (types != null) {
                        for (int i = 0; i < types.length(); i++) {
                            JSONObject typeObj = types.optJSONObject(i);
                            if (typeObj != null && typeObj.has("name")) {
                                String name = typeObj.getString("name");
                                if (!name.startsWith("__")) {
                                    names.add(name);
                                }
                            }
                        }
                    }
                    int displayCount = names.isEmpty() ? count : names.size();
                    Platform.runLater(() -> {
                        introspectedTypeNames.clear();
                        introspectedTypeNames.addAll(names);
                        graphqlSchemaStatusText = "Schema loaded (" + displayCount + " types)";
                        if (graphqlWarningTooltip != null && graphqlWarningTooltip.isShowing()) {
                            graphqlWarningTooltip.hide();
                        }
                        if (graphqlWarningLabel != null) {
                            graphqlWarningLabel.setVisible(false);
                            graphqlWarningLabel.setManaged(false);
                        }
                    });
                    logger.info("GraphQL schema introspected successfully: " + displayCount + " types");
                } else {
                    Platform.runLater(() -> {
                        graphqlSchemaStatusText = "No schema";
                        if (graphqlWarningLabel != null) {
                            graphqlWarningLabel.setVisible(true);
                            graphqlWarningLabel.setManaged(true);
                        }
                    });
                    logger.warning("GraphQL auto fetch returned HTTP " + resp.statusCode());
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    graphqlSchemaStatusText = "No schema";
                    if (graphqlWarningLabel != null) {
                        graphqlWarningLabel.setVisible(true);
                        graphqlWarningLabel.setManaged(true);
                    }
                });
                logger.warning("GraphQL auto fetch failed: " + ex.getMessage());
            }
        }, "graphql-schema-autofetch");
        t.setDaemon(true);
        t.start();
    }

    private boolean isValidHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.trim().toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            return uri.getHost() != null && !uri.getHost().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isGraphqlActive() {
        Toggle sel = bodyTypeGroup != null ? bodyTypeGroup.getSelectedToggle() : null;
        return sel instanceof ToggleButton tb && "GraphQL".equalsIgnoreCase(tb.getText());
    }

    /** Postman-style right-click menu for the Pre-request/Tests script
     * editors: Set as variable, Save to Package Library, Cut/Copy/Paste,
     * EncodeURIComponent/DecodeURIComponent, Find — replaces the plain
     * TextArea's default OS context menu. */
    private void setupScriptEditorsContextMenu() {
        if (preRequestScriptsArea != null) {
            EditorContextMenu.attach(TextEditTarget.of(preRequestScriptsArea), false, true, scriptMenuExtensions());
            attachCommentToggleShortcut(preRequestScriptsArea, TextEditTarget.of(preRequestScriptsArea));
        }
        if (postRequestScriptsArea != null) {
            EditorContextMenu.attach(TextEditTarget.of(postRequestScriptsArea), false, true, scriptMenuExtensions());
            attachCommentToggleShortcut(postRequestScriptsArea, TextEditTarget.of(postRequestScriptsArea));
        }
    }

    /** Ctrl+/ toggles "//" line-comments in the given editor — press once
     * to comment the selected/current line(s), press again to uncomment.
     * Independent of the "Comment" annotation-thread feature. */
    private void attachCommentToggleShortcut(Node node, TextEditTarget target) {
        node.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.SLASH && e.isControlDown()) {
                EditorContextMenu.toggleLineComment(target);
                e.consume();
            }
        });
    }

    /** Ctrl+Shift+F reformats the WHOLE JSON body with 2-space indentation
     * (the same convention used everywhere else in the app — response
     * viewer, exports) regardless of any current selection, matching how
     * a "Format Document" command works in most editors rather than a
     * "format selection" one — no need to select-all first, just press
     * the shortcut. JSON itself has no concept of comments, so if the
     * body has any "//" or block comments they're stripped as part of
     * reformatting, exactly like every other JSON beautifier; the person
     * is told this via the status bar so it's never a silent surprise. */
    private void attachPrettifyShortcut(CodeArea area) {
        area.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.F && e.isControlDown() && e.isShiftDown()) {
                prettifyJsonEditor(area);
                e.consume();
            }
        });
    }

    private void prettifyJsonEditor(CodeArea area) {
        String text = area.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        String trimmed = text.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            AlertUtils.showError("Can't format — this doesn't look like JSON");
            return;
        }
        boolean hadComments = JsonCommentStripper.containsComment(trimmed);
        String forParsing = JsonCommentStripper.strip(trimmed).trim();
        try {
            String pretty = JsonPrettyPrinter.prettyPrint(forParsing);
            area.replaceText(pretty);
            area.moveTo(0);
            if (area == bodyTextArea && bodyCommentHighlighter != null) {
                bodyCommentHighlighter.refresh();
            } else {
                area.setStyleSpans(0, JsonSyntaxHighlighter.computeHighlighting(area.getText()));
            }
            updateStatus(hadComments ? "Formatted JSON (comments removed)" : "Formatted JSON");
        } catch (Exception ex) {
            AlertUtils.showError("Can't format — invalid JSON");
        }
    }

    /** "Set as variable" (+ "Comment" for the body editor) extensions
     * shared by every body-style editor. */
    private EditorContextMenu.Extensions bodyMenuExtensions() {
        return new EditorContextMenu.Extensions() {
            @Override
            public void onSetAsVariable(TextEditTarget target) {
                handleSetAsVariable(target);
            }

            @Override
            public void onComment(TextEditTarget target) {
                handleAddComment(target);
            }
        };
    }

    /** "Set as variable" + "Save to Package Library" extensions shared by
     * the pre-request/tests script editors. */
    private EditorContextMenu.Extensions scriptMenuExtensions() {
        return new EditorContextMenu.Extensions() {
            @Override
            public void onSetAsVariable(TextEditTarget target) {
                handleSetAsVariable(target);
            }

            @Override
            public void onSaveToExistingPackage(TextEditTarget target) {
                handleSaveToPackage(target, false);
            }

            @Override
            public void onSaveToNewPackage(TextEditTarget target) {
                handleSaveToPackage(target, true);
            }
        };
    }

    /** Postman's "Set as new variable" card: Name / Value (read-only
     * preview) / Scope — Environment, Collection, Global or Vault. Saving
     * always replaces the selection with a {{name}} (or {{vault:name}}
     * for Vault) reference, then persists to whichever scope was chosen. */
    private void handleSetAsVariable(TextEditTarget target) {
        String selected = target.getSelectedText();
        if (selected == null || selected.isEmpty()) {
            AlertUtils.showError("Select some text first to turn it into a variable.");
            return;
        }
        IndexRange range = target.getSelection();
        SetVariablePopup.show(target.getNode(), selected, (scope, name, value) -> {
            String reference = "Vault".equals(scope) ? "{{vault:" + name + "}}" : "{{" + name + "}}";
            target.replaceText(range.getStart(), range.getEnd(), reference);
            switch (scope) {
                case "Environment" -> saveEnvironmentVariable(name, value);
                case "Global" -> saveGlobalVariable(name, value);
                case "Vault" -> saveVaultVariable(name, value);
                case "Collection" -> {
                    Tab active = activeRequestTab();
                    RequestTabState state = active != null ? tabStates.get(active) : null;
                    saveCollectionVariable(state != null ? state.collectionId : null, name, value);
                }
                default -> { /* unreachable — SetVariablePopup only offers the four scopes above */ }
            }
        });
    }

    /** Postman's right-click "Comment" on selected body text — opens the
     * "Ask questions or provide feedback..." composer, then persists the
     * new thread and repaints the yellow highlight once the server
     * confirms it. Comments are attached to a saved request, so this
     * needs the active tab to already have one. */
    private void handleAddComment(TextEditTarget target) {
        String selected = target.getSelectedText();
        if (selected == null || selected.isEmpty()) {
            AlertUtils.showError("Select some text first to leave a comment on it.");
            return;
        }
        Tab active = activeRequestTab();
        RequestTabState state = active != null ? tabStates.get(active) : null;
        if (state == null || state.requestId == null) {
            AlertUtils.showError("Save this request first — comments are attached to a saved request.");
            return;
        }
        IndexRange range = target.getSelection();
        int start = range.getStart();
        int end = range.getEnd();
        Long requestId = state.requestId;

        CommentThreadPopup.showComposer(target.getNode(), message -> new Thread(() -> {
            Optional<CommentResponse> created = CommentService.createThread(
                    requestId, message, start, end, selected, "body");
            created.ifPresent(thread -> Platform.runLater(() -> {
                List<CommentResponse> threads = new ArrayList<>(
                        commentThreadsCache.getOrDefault(requestId, new ArrayList<>()));
                threads.add(thread);
                commentThreadsCache.put(requestId, threads);
                refreshBodyCommentHighlights();
                updateStatus("Comment added");
            }));
        }).start());
    }

    /** Reopens an existing thread (clicked via its yellow highlight in the
     * body editor) as the breadcrumb + messages + reply popup. */
    private void handleOpenCommentThread(CommentResponse thread) {
        Tab active = activeRequestTab();
        RequestTabState state = active != null ? tabStates.get(active) : null;
        Long requestId = state != null && state.requestId != null ? state.requestId : thread.getRequestId();

        String preview = thread.getSnippet() != null ? thread.getSnippet() : "";
        String flatPreview = preview.replace("\n", " ").trim();
        if (flatPreview.length() > 40) {
            flatPreview = flatPreview.substring(0, 40) + "...";
        }
        String breadcrumb = "Body > raw > " + flatPreview;
        Long currentUserId = TokenManager.getUserId();

        CommentThreadPopup.showThread(bodyTextArea, thread, breadcrumb, currentUserId,
                new CommentThreadPopup.ThreadCallback() {
                    @Override
                    public void onReply(String message) {
                        new Thread(() -> {
                            Optional<CommentResponse> updated = CommentService.addReply(thread.getId(), message);
                            updated.ifPresent(t -> Platform.runLater(() -> replaceThreadInCache(requestId, t)));
                        }).start();
                    }

                    @Override
                    public void onEdit(Long commentId, String newMessage) {
                        new Thread(() -> {
                            Optional<CommentResponse> updated = CommentService.editMessage(commentId, newMessage);
                            updated.ifPresent(t -> Platform.runLater(() -> replaceThreadInCache(requestId, t)));
                        }).start();
                    }

                    @Override
                    public void onDelete(Long commentId) {
                        boolean isRoot = commentId.equals(thread.getId());
                        boolean confirmed = AlertUtils.showConfirmation("Delete Comment",
                                isRoot ? "Delete this comment and its replies?" : "Delete this reply?");
                        if (!confirmed) {
                            return;
                        }
                        new Thread(() -> {
                            boolean ok = CommentService.deleteMessage(commentId);
                            if (!ok) {
                                return;
                            }
                            Platform.runLater(() -> {
                                if (isRoot) {
                                    removeThreadFromCache(requestId, thread.getId());
                                } else {
                                    // A reply doesn't hand back an updated parent —
                                    // simplest correct fix is to reload the thread.
                                    reloadCommentThreads(requestId);
                                }
                            });
                        }).start();
                    }

                    @Override
                    public void onToggleResolve(boolean resolve) {
                        new Thread(() -> {
                            Optional<CommentResponse> updated = CommentService.setResolved(thread.getId(), resolve);
                            updated.ifPresent(t -> Platform.runLater(() -> replaceThreadInCache(requestId, t)));
                        }).start();
                    }
                });
    }

    private void replaceThreadInCache(Long requestId, CommentResponse updated) {
        List<CommentResponse> threads = new ArrayList<>(commentThreadsCache.getOrDefault(requestId, new ArrayList<>()));
        threads.removeIf(t -> t.getId().equals(updated.getId()));
        threads.add(updated);
        commentThreadsCache.put(requestId, threads);
        refreshBodyCommentHighlights();
    }

    private void removeThreadFromCache(Long requestId, Long threadId) {
        List<CommentResponse> threads = new ArrayList<>(commentThreadsCache.getOrDefault(requestId, new ArrayList<>()));
        threads.removeIf(t -> t.getId().equals(threadId));
        commentThreadsCache.put(requestId, threads);
        refreshBodyCommentHighlights();
    }

    private void reloadCommentThreads(Long requestId) {
        if (requestId == null) {
            return;
        }
        new Thread(() -> {
            Optional<List<CommentResponse>> threads = CommentService.getThreadsForRequest(requestId);
            commentThreadsCache.put(requestId, new ArrayList<>(threads.orElse(new ArrayList<>())));
            Platform.runLater(this::refreshBodyCommentHighlights);
        }).start();
    }

    /** Loads a request's comment threads once (best-effort, cached
     * afterward) and paints the body editor's highlights for whichever of
     * them are still the active tab's — called both right after a request
     * loads and whenever the active tab switches. */
    private void ensureCommentThreadsLoaded(Long requestId) {
        if (requestId == null) {
            return;
        }
        if (commentThreadsCache.containsKey(requestId)) {
            refreshBodyCommentHighlights();
            return;
        }
        new Thread(() -> {
            Optional<List<CommentResponse>> threads = CommentService.getThreadsForRequest(requestId);
            commentThreadsCache.put(requestId, new ArrayList<>(threads.orElse(new ArrayList<>())));
            Platform.runLater(this::refreshBodyCommentHighlights);
        }).start();
    }

    /** Repaints the body editor's yellow "has a comment" highlights from
     * whatever's cached for the currently active tab's request. */
    private void refreshBodyCommentHighlights() {
        if (bodyCommentHighlighter == null) {
            return;
        }
        Tab active = activeRequestTab();
        RequestTabState state = active != null ? tabStates.get(active) : null;
        if (state == null || state.requestId == null) {
            bodyCommentHighlighter.setThreads(Collections.emptyList());
            return;
        }
        List<CommentResponse> all = commentThreadsCache.getOrDefault(state.requestId, Collections.emptyList());
        bodyCommentHighlighter.setThreads(all.stream().filter(t -> "body".equals(t.getFieldName())).toList());
    }

    /** Postman's "Save to Package Library" — opens the real two-pane New
     * Package editor or the existing-package picker (see
     * PackageLibraryDialogs), persisting the selected script either way. */
    private void handleSaveToPackage(TextEditTarget target, boolean createNew) {
        String selected = target.getSelectedText();
        if (selected == null || selected.isEmpty()) {
            AlertUtils.showError("Select the script code you want to save first.");
            return;
        }
        javafx.stage.Window owner = target.getNode().getScene() != null
                ? target.getNode().getScene().getWindow() : null;
        if (createNew) {
            PackageLibraryDialogs.showNewPackage(owner, selected,
                    pkg -> updateStatus("Saved to new package \"" + pkg.getName() + "\""));
        } else {
            List<ScriptPackage> packages = PackageLibraryService.listPackages();
            if (packages.isEmpty()) {
                AlertUtils.showError("No packages yet — use \"New Package\" to create one first.");
                return;
            }
            PackageLibraryDialogs.showExistingPackagePicker(owner, pkg -> {
                PackageLibraryService.appendToPackage(pkg.getId(), selected);
                updateStatus("Added to package \"" + pkg.getName() + "\"");
            });
        }
    }

    /** Response viewer: read-only JSON + {{variable}} coloring, plus a
     * Ctrl/Cmd+F find bar overlaid in its top-right corner. */
    private void setupResponseCodeArea() {
        if (responsePreview != null && !responsePreview.getStyleClass().contains("response-preview")) {
            responsePreview.getStyleClass().add("response-preview");
        }
        if (responseWrapToggle != null) {
            responseWrapToggle.setSelected(true);
        }
        responseBodyArea.setWrapText(true);
        // Postman-style code folding & line numbers down the left edge of the response viewer.
        responseFoldManager = JsonFoldManager.attach(responseBodyArea);
        attachPrettifyShortcut(responseBodyArea);
        responseBodyArea.plainTextChanges().subscribe(change -> {
            if (responseSearch == null || responseSearch.getBar().isManaged()) {
                return; // the search bar owns styling (base + match highlight) while open
            }
            if (responseFoldManager != null && responseFoldManager.isUpdating()) {
                return;
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

        // Postman-style right-click context menu with fold controls
        ContextMenu responseContextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("Copy Response");
        copyItem.setOnAction(e -> handleCopyResponse());
        MenuItem foldAllItem = new MenuItem("Collapse All");
        foldAllItem.setOnAction(e -> handleCollapseAllResponse());
        MenuItem unfoldAllItem = new MenuItem("Expand All");
        unfoldAllItem.setOnAction(e -> handleExpandAllResponse());
        MenuItem findItem = new MenuItem("Find...");
        findItem.setOnAction(e -> handleOpenResponseSearch());
        responseContextMenu.getItems().addAll(copyItem, new SeparatorMenuItem(), foldAllItem, unfoldAllItem, new SeparatorMenuItem(), findItem);
        responseBodyArea.setContextMenu(responseContextMenu);

        if (responseBodyArea.getText() != null && !responseBodyArea.getText().isEmpty()) {
            responseFoldManager.setFullText(responseBodyArea.getText());
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
            updateRequestBuilderTabs(newMethod);
            updateResponseTabs(newMethod);
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

        updateRequestBuilderTabs(methodCombo.getValue() != null ? methodCombo.getValue() : "GET");
        updateResponseTabs(methodCombo.getValue() != null ? methodCombo.getValue() : "GET");
    }

    private void updateRequestBuilderTabs(String method) {
        if (requestBuilderTabPane == null) {
            return;
        }
        Tab selectedBefore = requestBuilderTabPane.getSelectionModel().getSelectedItem();
        List<Tab> targetTabs;
        if ("WS".equals(method)) {
            targetTabs = Arrays.asList(wsMessageTab, paramsTab, headersTab);
        } else if ("SOCKETIO".equals(method)) {
            targetTabs = Arrays.asList(socketIoMessageTab, socketIoEventsTab, paramsTab, headersTab);
        } else {
            targetTabs = Arrays.asList(paramsTab, authTab, headersTab, bodyTab, scriptsTab);
        }

        List<Tab> validTabs = targetTabs.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (!requestBuilderTabPane.getTabs().equals(validTabs)) {
            requestBuilderTabPane.getTabs().setAll(validTabs);
            if (selectedBefore != null && validTabs.contains(selectedBefore)) {
                requestBuilderTabPane.getSelectionModel().select(selectedBefore);
            } else if (!validTabs.isEmpty()) {
                requestBuilderTabPane.getSelectionModel().select(validTabs.get(0));
            }
        }
    }

    private void updateResponseTabs(String method) {
        if (responseTabPane == null) {
            return;
        }
        Tab selectedBefore = responseTabPane.getSelectionModel().getSelectedItem();
        List<Tab> targetTabs;
        if ("WS".equals(method)) {
            targetTabs = Collections.singletonList(wsMessagesTab);
        } else if ("SOCKETIO".equals(method)) {
            targetTabs = Collections.singletonList(socketIoMessagesTab);
        } else {
            targetTabs = Arrays.asList(responseBodyTab, responseCookiesTab, responseHeadersTab, responseTestsTab);
        }

        List<Tab> validTabs = targetTabs.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (!responseTabPane.getTabs().equals(validTabs)) {
            responseTabPane.getTabs().setAll(validTabs);
            if (selectedBefore != null && validTabs.contains(selectedBefore)) {
                responseTabPane.getSelectionModel().select(selectedBefore);
            } else if (!validTabs.isEmpty()) {
                responseTabPane.getSelectionModel().select(validTabs.get(0));
            }
        }
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
                            () -> tokenField.getText().isEmpty() && !tokenField.isFocused(),
                            tokenField.textProperty(),
                            tokenField.focusedProperty()));
        }

        usernameField.setWrapText(false);
        usernameField.plainTextChanges().subscribe(change ->
                usernameField.setStyleSpans(0, JsonSyntaxHighlighter.computeUrlHighlighting(usernameField.getText())));
        VariableAutocomplete.attach(usernameField, this::currentEnvironmentVariables);
        if (usernamePromptLabel != null) {
            usernamePromptLabel.visibleProperty().bind(
                    javafx.beans.binding.Bindings.createBooleanBinding(
                            () -> usernameField.getText().isEmpty() && !usernameField.isFocused(),
                            usernameField.textProperty(),
                            usernameField.focusedProperty()));
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
        setupParamsTable();
        addCommonHeaders();
        setupHeadersTable();
        setupKeyValueTable(responseHeadersTable, responseHeadersData, "Key", "Value");
        setupKeyValueTable(cookiesTable, cookiesData, "Name", "Value", "Domain");
        setupSocketIoEventsTable();
    }

    private static final javafx.css.PseudoClass ROW_SELECTED_PSEUDO =
            javafx.css.PseudoClass.getPseudoClass("row-selected");
    private CheckBox paramsMasterCheckBox;
    private CheckBox paramsMasterSelectBox;
    private int pendingFocusParamRow = -1;

    private CheckBox headersMasterCheckBox;
    private CheckBox headersMasterSelectBox;
    private int pendingFocusHeaderRow = -1;

    private CheckBox formDataMasterCheckBox;
    private CheckBox formDataMasterSelectBox;
    private int pendingFocusFormDataRow = -1;

    private CheckBox urlEncodedMasterCheckBox;
    private CheckBox urlEncodedMasterSelectBox;
    private int pendingFocusUrlEncodedRow = -1;

    private void setupParamsTable() {
        paramsTable.getColumns().clear();
        paramsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        paramsTable.setItems(paramsData);
        paramsTable.setEditable(true);
        paramsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Row factory applies user-friendly selection color dynamically via listener
        paramsTable.setRowFactory(tv -> new TableRow<>() {
            private final javafx.beans.value.ChangeListener<Boolean> selListener = (obs, oldVal, newVal) -> {
                pseudoClassStateChanged(ROW_SELECTED_PSEUDO, Boolean.TRUE.equals(newVal));
            };

            @Override
            protected void updateItem(KeyValuePair item, boolean empty) {
                KeyValuePair oldItem = getItem();
                if (oldItem != null) {
                    oldItem.selectedProperty().removeListener(selListener);
                }
                super.updateItem(item, empty);
                if (empty || item == null) {
                    pseudoClassStateChanged(ROW_SELECTED_PSEUDO, false);
                } else {
                    pseudoClassStateChanged(ROW_SELECTED_PSEUDO, item.isSelected());
                    item.selectedProperty().addListener(selListener);
                }
            }
        });

        CheckBox masterCheckBox = new CheckBox();
        this.paramsMasterCheckBox = masterCheckBox;
        masterCheckBox.setAlignment(Pos.CENTER);
        masterCheckBox.setMaxWidth(Double.MAX_VALUE);
        masterCheckBox.setSelected(true);
        masterCheckBox.setOnAction(e -> {
            boolean selectAll = masterCheckBox.isSelected();
            for (KeyValuePair kv : paramsData) {
                if (!isRowEmpty(kv) && !kv.isPlaceholder()) {
                    kv.setEnabled(selectAll);
                }
            }
            paramsTable.refresh();
            syncParamsToUrl();
            onEditorFieldChanged();
        });

        // 1. Tick mark (enable/disable param in URL query string)
        TableColumn<KeyValuePair, Boolean> checkCol = new TableColumn<>();
        checkCol.setGraphic(masterCheckBox);
        checkCol.getStyleClass().add("param-check-col");
        checkCol.setMinWidth(38);
        checkCol.setMaxWidth(38);
        checkCol.setPrefWidth(38);
        checkCol.setResizable(false);
        checkCol.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());
        checkCol.setCellFactory(col -> new TableCell<KeyValuePair, Boolean>() {
            private final CheckBox rowCheckBox = new CheckBox();
            private final javafx.beans.InvalidationListener rowStateListener = obs -> updateState();
            private KeyValuePair boundItem = null;

            private void updateState() {
                KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item == null || (item.isPlaceholder() && isRowEmpty(item))) {
                    rowCheckBox.setVisible(false);
                } else {
                    rowCheckBox.setVisible(true);
                    rowCheckBox.setSelected(item.isEnabled());
                }
            }

            {
                rowCheckBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(rowCheckBox);
                getStyleClass().add("param-check-cell");
                rowCheckBox.setOnAction(e -> {
                    KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setEnabled(rowCheckBox.isSelected());
                        syncParamsToUrl();
                        onEditorFieldChanged();
                        updateMasterCheckBox(paramsMasterCheckBox);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean val, boolean empty) {
                if (boundItem != null) {
                    boundItem.placeholderProperty().removeListener(rowStateListener);
                    boundItem.enabledProperty().removeListener(rowStateListener);
                }
                super.updateItem(val, empty);
                boundItem = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (boundItem != null) {
                    boundItem.placeholderProperty().addListener(rowStateListener);
                    boundItem.enabledProperty().addListener(rowStateListener);
                }
                updateState();
            }
        });

        // 2. Key column
        TableColumn<KeyValuePair, String> keyCol = new TableColumn<>("Key");
        keyCol.setMinWidth(140);
        keyCol.setPrefWidth(220);
        keyCol.setCellValueFactory(cellData -> cellData.getValue().keyProperty());
        keyCol.setCellFactory(col -> createParamCell("Key", 0, KeyValuePair::getKey, KeyValuePair::setKey));

        // 3. Value column
        TableColumn<KeyValuePair, String> valueCol = new TableColumn<>("Value");
        valueCol.setMinWidth(140);
        valueCol.setPrefWidth(240);
        valueCol.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        valueCol.setCellFactory(col -> createParamCell("Value", 1, KeyValuePair::getValue, KeyValuePair::setValue));

        // 4. Description column
        TableColumn<KeyValuePair, String> descCol = new TableColumn<>("Description");
        descCol.setMinWidth(140);
        descCol.setPrefWidth(240);
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descCol.setCellFactory(col -> createParamCell("Description", 2, KeyValuePair::getDescription, KeyValuePair::setDescription));

        // 5. Select column (row select option: centered checkbox in header, aligned above row checkboxes)
        CheckBox masterSelectBox = new CheckBox();
        this.paramsMasterSelectBox = masterSelectBox;
        masterSelectBox.setAlignment(Pos.CENTER);
        masterSelectBox.setMaxWidth(Double.MAX_VALUE);
        masterSelectBox.setOnAction(e -> {
            boolean selectAll = masterSelectBox.isSelected();
            for (KeyValuePair kv : paramsData) {
                if (selectAll) {
                    if (!isRowEmpty(kv) && !kv.isPlaceholder()) {
                        kv.setSelected(true);
                    }
                } else {
                    kv.setSelected(false);
                }
            }
            paramsTable.getSelectionModel().clearSelection();
            paramsTable.refresh();
        });


        TableColumn<KeyValuePair, Boolean> selectCol = new TableColumn<>();
        selectCol.setGraphic(masterSelectBox);
        selectCol.getStyleClass().add("param-select-col");
        selectCol.setMinWidth(38);
        selectCol.setMaxWidth(38);
        selectCol.setPrefWidth(38);
        selectCol.setResizable(false);
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(col -> new TableCell<KeyValuePair, Boolean>() {
            private final CheckBox rowSelectBox = new CheckBox();
            private final javafx.beans.InvalidationListener rowStateListener = obs -> updateState();
            private KeyValuePair boundItem = null;

            private void updateState() {
                KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item == null || (item.isPlaceholder() && isRowEmpty(item))) {
                    rowSelectBox.setVisible(false);
                } else {
                    rowSelectBox.setVisible(true);
                    rowSelectBox.setSelected(item.isSelected());
                }
            }

            {
                rowSelectBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(rowSelectBox);
                getStyleClass().add("param-select-cell");
                rowSelectBox.setOnAction(e -> {
                    KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setSelected(rowSelectBox.isSelected());
                        updateMasterSelectBox(paramsMasterSelectBox);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean val, boolean empty) {
                if (boundItem != null) {
                    boundItem.placeholderProperty().removeListener(rowStateListener);
                    boundItem.selectedProperty().removeListener(rowStateListener);
                }
                super.updateItem(val, empty);
                boundItem = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (boundItem != null) {
                    boundItem.placeholderProperty().addListener(rowStateListener);
                    boundItem.selectedProperty().addListener(rowStateListener);
                }
                updateState();
            }
        });

        paramsTable.getColumns().addAll(checkCol, keyCol, valueCol, descCol, selectCol);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem addItem = new MenuItem("Add Row");
        MenuItem deleteItem = new MenuItem("Delete Row");
        addItem.setOnAction(e -> handleAddParamRow());
        deleteItem.setOnAction(e -> handleDeleteParamRow());
        contextMenu.getItems().addAll(addItem, deleteItem);
        paramsTable.setContextMenu(contextMenu);

        ensurePlaceholderRow();
        updateMasterCheckBox(paramsMasterCheckBox);
        updateMasterSelectBox(paramsMasterSelectBox);
    }

    private TableCell<KeyValuePair, String> createParamCell(String placeholder,
                                                            int colIndex,
                                                            Function<KeyValuePair, String> getter,
                                                            BiConsumer<KeyValuePair, String> setter) {
        return new TableCell<KeyValuePair, String>() {
            private final TextField textField = new TextField();
            private boolean updating = false;

            {
                textField.setPromptText(placeholder);
                textField.getStyleClass().add("param-cell-field");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(textField);
                getStyleClass().add("param-table-cell");

                setOnMouseClicked(e -> textField.requestFocus());

                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) {
                        return;
                    }
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row == null) {
                        return;
                    }
                    String safeVal = newVal != null ? newVal : "";
                    setter.accept(row, safeVal);

                    if (!isRowEmpty(row)) {
                        row.setPlaceholder(false);
                        if (!row.isEnabled()) {
                            row.setEnabled(true);
                        }
                        ensurePlaceholderRow();
                        updateMasterCheckBox(paramsMasterCheckBox);
                        updateMasterSelectBox(paramsMasterSelectBox);
                    }

                    if (!syncingUrlAndParams) {
                        syncParamsToUrl();
                    }
                    onEditorFieldChanged();
                });

                textField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                        e.consume();
                        navigateNextParamCell(colIndex);
                    }
                });
            }

            private void navigateNextParamCell(int currentCol) {
                int rowIndex = getIndex();
                if (currentCol < 2) {
                    focusParamCell(rowIndex, currentCol + 2);
                } else {
                    int nextRow = rowIndex + 1;
                    if (nextRow < paramsData.size()) {
                        focusParamCell(nextRow, 1);
                    }
                }
            }

            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                KeyValuePair row = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (empty || row == null) {
                    updating = true;
                    try {
                        textField.setText("");
                        textField.setVisible(false);
                    } finally {
                        updating = false;
                    }
                } else {
                    textField.setVisible(true);
                    String current = getter.apply(row);
                    String safeCurrent = current != null ? current : "";
                    if (!safeCurrent.equals(textField.getText())) {
                        updating = true;
                        try {
                            textField.setText(safeCurrent);
                        } finally {
                            updating = false;
                        }
                    }
                    if (!row.isEnabled()) {
                        textField.setStyle("-fx-text-fill: -text-muted; -fx-opacity: 0.6;");
                    } else {
                        textField.setStyle("");
                    }

                    // Check if this row was just added and needs Key focus
                    if (colIndex == 0 && getIndex() == pendingFocusParamRow) {
                        pendingFocusParamRow = -1;
                        Platform.runLater(() -> {
                            textField.requestFocus();
                            textField.selectAll();
                        });
                    }
                }
            }
        };
    }

    private void focusParamCell(int rowIdx, int colIdx) {
        Platform.runLater(() -> {
            paramsTable.scrollTo(rowIdx);
            if (colIdx >= 0 && colIdx < paramsTable.getColumns().size()) {
                paramsTable.getFocusModel().focus(rowIdx, paramsTable.getColumns().get(colIdx));
            }
            Platform.runLater(() -> {
                if (colIdx >= 0 && colIdx < paramsTable.getColumns().size()) {
                    var targetCol = paramsTable.getColumns().get(colIdx);
                    for (Node n : paramsTable.lookupAll(".param-table-cell")) {
                        if (n instanceof TableCell<?, ?> tc && tc.getTableColumn() == targetCol && tc.getIndex() == rowIdx) {
                            if (tc.getGraphic() instanceof TextField tf) {
                                tf.requestFocus();
                                tf.selectAll();
                                break;
                            }
                        }
                    }
                }
            });
        });
    }

    private boolean isRowEmpty(KeyValuePair kv) {
        if (kv == null) {
            return true;
        }
        String k = kv.getKey();
        String v = kv.getValue();
        String d = kv.getDescription();
        String fp = kv.getFilePath();
        return (k == null || k.trim().isEmpty())
                && (v == null || v.trim().isEmpty())
                && (d == null || d.trim().isEmpty())
                && (fp == null || fp.trim().isEmpty());
    }

    private void ensurePlaceholderRow() {
        if (paramsData.isEmpty()) {
            paramsData.add(new KeyValuePair(true, "", "", "", true));
            return;
        }
        while (paramsData.size() > 1) {
            KeyValuePair last = paramsData.get(paramsData.size() - 1);
            KeyValuePair secondLast = paramsData.get(paramsData.size() - 2);
            if (last.isPlaceholder() && isRowEmpty(last) && secondLast.isPlaceholder() && isRowEmpty(secondLast)) {
                paramsData.remove(paramsData.size() - 1);
            } else {
                break;
            }
        }
        KeyValuePair last = paramsData.get(paramsData.size() - 1);
        if (!last.isPlaceholder()) {
            paramsData.add(new KeyValuePair(true, "", "", "", true));
        }
    }

    private void updateMasterCheckBox(CheckBox masterCheckBox) {
        if (masterCheckBox == null) {
            return;
        }
        long totalActive = paramsData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder()).count();
        if (totalActive == 0) {
            masterCheckBox.setSelected(true);
            masterCheckBox.setIndeterminate(false);
            return;
        }
        long enabledCount = paramsData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder() && kv.isEnabled()).count();
        if (enabledCount == totalActive) {
            masterCheckBox.setSelected(true);
            masterCheckBox.setIndeterminate(false);
        } else if (enabledCount == 0) {
            masterCheckBox.setSelected(false);
            masterCheckBox.setIndeterminate(false);
        } else {
            masterCheckBox.setIndeterminate(true);
        }
    }

    private void updateMasterSelectBox(CheckBox masterSelectBox) {
        if (masterSelectBox == null) {
            return;
        }
        long totalActive = paramsData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder()).count();
        if (totalActive == 0) {
            masterSelectBox.setSelected(false);
            masterSelectBox.setIndeterminate(false);
            return;
        }
        long selectedCount = paramsData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder() && kv.isSelected()).count();
        if (selectedCount == totalActive) {
            masterSelectBox.setSelected(true);
            masterSelectBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            masterSelectBox.setSelected(false);
            masterSelectBox.setIndeterminate(false);
        } else {
            masterSelectBox.setIndeterminate(true);
        }
    }

    private void addCommonHeaders() {
        headersData.addAll(
                new KeyValuePair(true, "Content-Type", "application/json", "Request content type", false),
                new KeyValuePair(true, "Accept", "application/json", "Response format", false),
                new KeyValuePair(true, "User-Agent", "JavaFX-API-Client/1.0", "Client identifier", false)
        );
    }

    private void setupHeadersTable() {
        headersTable.getColumns().clear();
        headersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        headersTable.setItems(headersData);
        headersTable.setEditable(true);
        headersTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Row factory applies user-friendly selection color dynamically via listener
        headersTable.setRowFactory(tv -> new TableRow<>() {
            private final javafx.beans.value.ChangeListener<Boolean> selListener = (obs, oldVal, newVal) -> {
                pseudoClassStateChanged(ROW_SELECTED_PSEUDO, Boolean.TRUE.equals(newVal));
            };

            @Override
            protected void updateItem(KeyValuePair item, boolean empty) {
                KeyValuePair oldItem = getItem();
                if (oldItem != null) {
                    oldItem.selectedProperty().removeListener(selListener);
                }
                super.updateItem(item, empty);
                if (empty || item == null) {
                    pseudoClassStateChanged(ROW_SELECTED_PSEUDO, false);
                } else {
                    pseudoClassStateChanged(ROW_SELECTED_PSEUDO, item.isSelected());
                    item.selectedProperty().addListener(selListener);
                }
            }
        });

        CheckBox masterCheckBox = new CheckBox();
        this.headersMasterCheckBox = masterCheckBox;
        masterCheckBox.setAlignment(Pos.CENTER);
        masterCheckBox.setMaxWidth(Double.MAX_VALUE);
        masterCheckBox.setSelected(true);
        masterCheckBox.setOnAction(e -> {
            boolean selectAll = masterCheckBox.isSelected();
            for (KeyValuePair kv : headersData) {
                if (!isRowEmpty(kv) && !kv.isPlaceholder()) {
                    kv.setEnabled(selectAll);
                }
            }
            headersTable.refresh();
            onEditorFieldChanged();
        });

        // 1. Tick mark (enable/disable header in request)
        TableColumn<KeyValuePair, Boolean> checkCol = new TableColumn<>();
        checkCol.setGraphic(masterCheckBox);
        checkCol.getStyleClass().add("param-check-col");
        checkCol.setMinWidth(38);
        checkCol.setMaxWidth(38);
        checkCol.setPrefWidth(38);
        checkCol.setResizable(false);
        checkCol.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());
        checkCol.setCellFactory(col -> new TableCell<KeyValuePair, Boolean>() {
            private final CheckBox rowCheckBox = new CheckBox();
            private final javafx.beans.InvalidationListener rowStateListener = obs -> updateState();
            private KeyValuePair boundItem = null;

            private void updateState() {
                KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item == null || (item.isPlaceholder() && isRowEmpty(item))) {
                    rowCheckBox.setVisible(false);
                } else {
                    rowCheckBox.setVisible(true);
                    rowCheckBox.setSelected(item.isEnabled());
                }
            }

            {
                rowCheckBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(rowCheckBox);
                getStyleClass().add("param-check-cell");
                rowCheckBox.setOnAction(e -> {
                    KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setEnabled(rowCheckBox.isSelected());
                        onEditorFieldChanged();
                        updateHeadersMasterCheckBox(headersMasterCheckBox);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean val, boolean empty) {
                if (boundItem != null) {
                    boundItem.placeholderProperty().removeListener(rowStateListener);
                    boundItem.enabledProperty().removeListener(rowStateListener);
                }
                super.updateItem(val, empty);
                boundItem = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (boundItem != null) {
                    boundItem.placeholderProperty().addListener(rowStateListener);
                    boundItem.enabledProperty().addListener(rowStateListener);
                }
                updateState();
            }
        });

        // 2. Key column (with HeaderAutocomplete attached)
        TableColumn<KeyValuePair, String> keyCol = new TableColumn<>("Key");
        keyCol.setMinWidth(140);
        keyCol.setPrefWidth(220);
        keyCol.setCellValueFactory(cellData -> cellData.getValue().keyProperty());
        keyCol.setCellFactory(col -> createHeaderCell("Key", 0, KeyValuePair::getKey, KeyValuePair::setKey));

        // 3. Value column
        TableColumn<KeyValuePair, String> valueCol = new TableColumn<>("Value");
        valueCol.setMinWidth(140);
        valueCol.setPrefWidth(240);
        valueCol.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        valueCol.setCellFactory(col -> createHeaderCell("Value", 1, KeyValuePair::getValue, KeyValuePair::setValue));

        // 4. Description column
        TableColumn<KeyValuePair, String> descCol = new TableColumn<>("Description");
        descCol.setMinWidth(140);
        descCol.setPrefWidth(240);
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descCol.setCellFactory(col -> createHeaderCell("Description", 2, KeyValuePair::getDescription, KeyValuePair::setDescription));

        // 5. Select column (row select checkbox for delete)
        CheckBox masterSelectBox = new CheckBox();
        this.headersMasterSelectBox = masterSelectBox;
        masterSelectBox.setAlignment(Pos.CENTER);
        masterSelectBox.setMaxWidth(Double.MAX_VALUE);
        masterSelectBox.setOnAction(e -> {
            boolean selectAll = masterSelectBox.isSelected();
            for (KeyValuePair kv : headersData) {
                if (selectAll) {
                    if (!isRowEmpty(kv) && !kv.isPlaceholder()) {
                        kv.setSelected(true);
                    }
                } else {
                    kv.setSelected(false);
                }
            }
            headersTable.getSelectionModel().clearSelection();
            headersTable.refresh();
        });

        TableColumn<KeyValuePair, Boolean> selectCol = new TableColumn<>();
        selectCol.setGraphic(masterSelectBox);
        selectCol.getStyleClass().add("param-select-col");
        selectCol.setMinWidth(38);
        selectCol.setMaxWidth(38);
        selectCol.setPrefWidth(38);
        selectCol.setResizable(false);
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(col -> new TableCell<KeyValuePair, Boolean>() {
            private final CheckBox rowSelectBox = new CheckBox();
            private final javafx.beans.InvalidationListener rowStateListener = obs -> updateState();
            private KeyValuePair boundItem = null;

            private void updateState() {
                KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item == null || (item.isPlaceholder() && isRowEmpty(item))) {
                    rowSelectBox.setVisible(false);
                } else {
                    rowSelectBox.setVisible(true);
                    rowSelectBox.setSelected(item.isSelected());
                }
            }

            {
                rowSelectBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(rowSelectBox);
                getStyleClass().add("param-select-cell");
                rowSelectBox.setOnAction(e -> {
                    KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setSelected(rowSelectBox.isSelected());
                        updateHeadersMasterSelectBox(headersMasterSelectBox);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean val, boolean empty) {
                if (boundItem != null) {
                    boundItem.placeholderProperty().removeListener(rowStateListener);
                    boundItem.selectedProperty().removeListener(rowStateListener);
                }
                super.updateItem(val, empty);
                boundItem = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (boundItem != null) {
                    boundItem.placeholderProperty().addListener(rowStateListener);
                    boundItem.selectedProperty().addListener(rowStateListener);
                }
                updateState();
            }
        });

        headersTable.getColumns().addAll(checkCol, keyCol, valueCol, descCol, selectCol);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem addItem = new MenuItem("Add Row");
        MenuItem deleteItem = new MenuItem("Delete Row");
        addItem.setOnAction(e -> handleAddHeaderRow());
        deleteItem.setOnAction(e -> handleDeleteHeaderRow());
        contextMenu.getItems().addAll(addItem, deleteItem);
        headersTable.setContextMenu(contextMenu);

        ensureHeadersPlaceholderRow();
        updateHeadersMasterCheckBox(headersMasterCheckBox);
        updateHeadersMasterSelectBox(headersMasterSelectBox);
    }

    private TableCell<KeyValuePair, String> createHeaderCell(String placeholder,
                                                             int colIndex,
                                                             Function<KeyValuePair, String> getter,
                                                             BiConsumer<KeyValuePair, String> setter) {
        return new TableCell<KeyValuePair, String>() {
            private final TextField textField = new TextField();
            private boolean updating = false;

            {
                textField.setPromptText(placeholder);
                textField.getStyleClass().add("param-cell-field");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(textField);
                getStyleClass().add("param-table-cell");

                setOnMouseClicked(e -> textField.requestFocus());

                if (colIndex == 0) {
                    HeaderAutocomplete.attach(textField, selectedHeader -> {
                        KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                        if (row != null) {
                            row.setKey(selectedHeader);
                            row.setPlaceholder(false);
                            if (!row.isEnabled()) {
                                row.setEnabled(true);
                            }
                            ensureHeadersPlaceholderRow();
                            updateHeadersMasterCheckBox(headersMasterCheckBox);
                            updateHeadersMasterSelectBox(headersMasterSelectBox);
                            onEditorFieldChanged();
                            focusHeaderCell(getIndex(), 2);
                        }
                    });
                }

                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) {
                        return;
                    }
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row == null) {
                        return;
                    }
                    String safeVal = newVal != null ? newVal : "";
                    setter.accept(row, safeVal);

                    if (!isRowEmpty(row)) {
                        row.setPlaceholder(false);
                        if (!row.isEnabled()) {
                            row.setEnabled(true);
                        }
                        ensureHeadersPlaceholderRow();
                        updateHeadersMasterCheckBox(headersMasterCheckBox);
                        updateHeadersMasterSelectBox(headersMasterSelectBox);
                    }

                    onEditorFieldChanged();
                });

                textField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                    if (e.isConsumed()) {
                        return;
                    }
                    if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                        e.consume();
                        navigateNextHeaderCell(colIndex);
                    }
                });
            }

            private void navigateNextHeaderCell(int currentCol) {
                int rowIndex = getIndex();
                if (currentCol < 2) {
                    focusHeaderCell(rowIndex, currentCol + 2);
                } else {
                    int nextRow = rowIndex + 1;
                    if (nextRow < headersData.size()) {
                        focusHeaderCell(nextRow, 1);
                    }
                }
            }

            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                KeyValuePair row = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (empty || row == null) {
                    updating = true;
                    try {
                        textField.setText("");
                        textField.setVisible(false);
                    } finally {
                        updating = false;
                    }
                } else {
                    textField.setVisible(true);
                    String current = getter.apply(row);
                    String safeCurrent = current != null ? current : "";
                    if (!safeCurrent.equals(textField.getText())) {
                        updating = true;
                        try {
                            textField.setText(safeCurrent);
                        } finally {
                            updating = false;
                        }
                    }
                    if (!row.isEnabled()) {
                        textField.setStyle("-fx-text-fill: -text-muted; -fx-opacity: 0.6;");
                    } else {
                        textField.setStyle("");
                    }

                    // Check if this row was just added and needs Key focus
                    if (colIndex == 0 && getIndex() == pendingFocusHeaderRow) {
                        pendingFocusHeaderRow = -1;
                        Platform.runLater(() -> {
                            textField.requestFocus();
                            textField.selectAll();
                        });
                    }
                }
            }
        };
    }

    private void focusHeaderCell(int rowIdx, int colIdx) {
        Platform.runLater(() -> {
            headersTable.scrollTo(rowIdx);
            if (colIdx >= 0 && colIdx < headersTable.getColumns().size()) {
                headersTable.getFocusModel().focus(rowIdx, headersTable.getColumns().get(colIdx));
            }
            Platform.runLater(() -> {
                if (colIdx >= 0 && colIdx < headersTable.getColumns().size()) {
                    var targetCol = headersTable.getColumns().get(colIdx);
                    for (Node n : headersTable.lookupAll(".param-table-cell")) {
                        if (n instanceof TableCell<?, ?> tc && tc.getTableColumn() == targetCol && tc.getIndex() == rowIdx) {
                            if (tc.getGraphic() instanceof TextField tf) {
                                tf.requestFocus();
                                tf.selectAll();
                                break;
                            }
                        }
                    }
                }
            });
        });
    }

    private void ensureHeadersPlaceholderRow() {
        if (headersData.isEmpty()) {
            headersData.add(new KeyValuePair(true, "", "", "", true));
            return;
        }
        while (headersData.size() > 1) {
            KeyValuePair last = headersData.get(headersData.size() - 1);
            KeyValuePair secondLast = headersData.get(headersData.size() - 2);
            if (last.isPlaceholder() && isRowEmpty(last) && secondLast.isPlaceholder() && isRowEmpty(secondLast)) {
                headersData.remove(headersData.size() - 1);
            } else {
                break;
            }
        }
        KeyValuePair last = headersData.get(headersData.size() - 1);
        if (!last.isPlaceholder()) {
            headersData.add(new KeyValuePair(true, "", "", "", true));
        }
    }

    private void updateHeadersMasterCheckBox(CheckBox masterCheckBox) {
        if (masterCheckBox == null) {
            return;
        }
        long totalActive = headersData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder()).count();
        if (totalActive == 0) {
            masterCheckBox.setSelected(true);
            masterCheckBox.setIndeterminate(false);
            return;
        }
        long enabledCount = headersData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder() && kv.isEnabled()).count();
        if (enabledCount == totalActive) {
            masterCheckBox.setSelected(true);
            masterCheckBox.setIndeterminate(false);
        } else if (enabledCount == 0) {
            masterCheckBox.setSelected(false);
            masterCheckBox.setIndeterminate(false);
        } else {
            masterCheckBox.setIndeterminate(true);
        }
    }

    private void updateHeadersMasterSelectBox(CheckBox masterSelectBox) {
        if (masterSelectBox == null) {
            return;
        }
        long totalActive = headersData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder()).count();
        if (totalActive == 0) {
            masterSelectBox.setSelected(false);
            masterSelectBox.setIndeterminate(false);
            return;
        }
        long selectedCount = headersData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder() && kv.isSelected()).count();
        if (selectedCount == totalActive) {
            masterSelectBox.setSelected(true);
            masterSelectBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            masterSelectBox.setSelected(false);
            masterSelectBox.setIndeterminate(false);
        } else {
            masterSelectBox.setIndeterminate(true);
        }
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
     * matching Postman's Text/File toggle, with single-click editing,
     * left tickmark, right row selection, and auto-growing placeholder row. */
    private void setupFormDataTable() {
        formDataTable.getColumns().clear();
        formDataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        formDataTable.setItems(formData);
        formDataTable.setEditable(true);
        formDataTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Row factory applies soft pink selection color dynamically via listener
        formDataTable.setRowFactory(tv -> new TableRow<>() {
            private final javafx.beans.value.ChangeListener<Boolean> selListener = (obs, oldVal, newVal) -> {
                pseudoClassStateChanged(ROW_SELECTED_PSEUDO, Boolean.TRUE.equals(newVal));
            };

            @Override
            protected void updateItem(KeyValuePair item, boolean empty) {
                KeyValuePair oldItem = getItem();
                if (oldItem != null) {
                    oldItem.selectedProperty().removeListener(selListener);
                }
                super.updateItem(item, empty);
                if (empty || item == null) {
                    pseudoClassStateChanged(ROW_SELECTED_PSEUDO, false);
                } else {
                    pseudoClassStateChanged(ROW_SELECTED_PSEUDO, item.isSelected());
                    item.selectedProperty().addListener(selListener);
                }
            }
        });

        // 1. Tick mark (enable/disable row in request)
        CheckBox masterCheckBox = new CheckBox();
        this.formDataMasterCheckBox = masterCheckBox;
        masterCheckBox.setAlignment(Pos.CENTER);
        masterCheckBox.setMaxWidth(Double.MAX_VALUE);
        masterCheckBox.setSelected(true);
        masterCheckBox.setOnAction(e -> {
            boolean selectAll = masterCheckBox.isSelected();
            for (KeyValuePair kv : formData) {
                if (!isRowEmpty(kv) && !kv.isPlaceholder()) {
                    kv.setEnabled(selectAll);
                }
            }
            formDataTable.refresh();
            onEditorFieldChanged();
        });

        TableColumn<KeyValuePair, Boolean> checkCol = new TableColumn<>();
        checkCol.setGraphic(masterCheckBox);
        checkCol.getStyleClass().add("param-check-col");
        checkCol.setMinWidth(38);
        checkCol.setMaxWidth(38);
        checkCol.setPrefWidth(38);
        checkCol.setResizable(false);
        checkCol.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());
        checkCol.setCellFactory(col -> new TableCell<KeyValuePair, Boolean>() {
            private final CheckBox rowCheckBox = new CheckBox();
            private final javafx.beans.InvalidationListener rowStateListener = obs -> updateState();
            private KeyValuePair boundItem = null;

            private void updateState() {
                KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item == null || (item.isPlaceholder() && isRowEmpty(item))) {
                    rowCheckBox.setVisible(false);
                } else {
                    rowCheckBox.setVisible(true);
                    rowCheckBox.setSelected(item.isEnabled());
                }
            }

            {
                rowCheckBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(rowCheckBox);
                getStyleClass().add("param-check-cell");
                rowCheckBox.setOnAction(e -> {
                    KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setEnabled(rowCheckBox.isSelected());
                        onEditorFieldChanged();
                        updateFormDataMasterCheckBox(formDataMasterCheckBox);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean val, boolean empty) {
                if (boundItem != null) {
                    boundItem.placeholderProperty().removeListener(rowStateListener);
                    boundItem.enabledProperty().removeListener(rowStateListener);
                }
                super.updateItem(val, empty);
                boundItem = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (boundItem != null) {
                    boundItem.placeholderProperty().addListener(rowStateListener);
                    boundItem.enabledProperty().addListener(rowStateListener);
                }
                updateState();
            }
        });

        // 2. Key column with inline Postman-style Type dropdown (Text ▾ / File ▾)
        TableColumn<KeyValuePair, String> keyCol = new TableColumn<>("Key");
        keyCol.setMinWidth(160);
        keyCol.setPrefWidth(220);
        keyCol.setCellValueFactory(cellData -> cellData.getValue().keyProperty());
        keyCol.setCellFactory(col -> createFormDataKeyCell());

        // 3. Value column (single-click text field or file picker button depending on type)
        TableColumn<KeyValuePair, String> valueCol = new TableColumn<>("Value");
        valueCol.setMinWidth(180);
        valueCol.setPrefWidth(260);
        valueCol.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        valueCol.setCellFactory(col -> createFormDataValueCell());

        // 4. Description column
        TableColumn<KeyValuePair, String> descCol = new TableColumn<>("Description");
        descCol.setMinWidth(140);
        descCol.setPrefWidth(240);
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descCol.setCellFactory(col -> createFormDataDescCell());

        // 5. Select column
        CheckBox masterSelectBox = new CheckBox();
        this.formDataMasterSelectBox = masterSelectBox;
        masterSelectBox.setAlignment(Pos.CENTER);
        masterSelectBox.setMaxWidth(Double.MAX_VALUE);
        masterSelectBox.setOnAction(e -> {
            boolean selectAll = masterSelectBox.isSelected();
            for (KeyValuePair kv : formData) {
                if (selectAll) {
                    if (!isRowEmpty(kv) && !kv.isPlaceholder()) {
                        kv.setSelected(true);
                    }
                } else {
                    kv.setSelected(false);
                }
            }
            formDataTable.getSelectionModel().clearSelection();
            formDataTable.refresh();
        });

        TableColumn<KeyValuePair, Boolean> selectCol = new TableColumn<>();
        selectCol.setGraphic(masterSelectBox);
        selectCol.getStyleClass().add("param-select-col");
        selectCol.setMinWidth(38);
        selectCol.setMaxWidth(38);
        selectCol.setPrefWidth(38);
        selectCol.setResizable(false);
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(col -> new TableCell<KeyValuePair, Boolean>() {
            private final CheckBox rowSelectBox = new CheckBox();
            private final javafx.beans.InvalidationListener rowStateListener = obs -> updateState();
            private KeyValuePair boundItem = null;

            private void updateState() {
                KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item == null || (item.isPlaceholder() && isRowEmpty(item))) {
                    rowSelectBox.setVisible(false);
                } else {
                    rowSelectBox.setVisible(true);
                    rowSelectBox.setSelected(item.isSelected());
                }
            }

            {
                rowSelectBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(rowSelectBox);
                getStyleClass().add("param-select-cell");
                rowSelectBox.setOnAction(e -> {
                    KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setSelected(rowSelectBox.isSelected());
                        updateFormDataMasterSelectBox(formDataMasterSelectBox);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean val, boolean empty) {
                if (boundItem != null) {
                    boundItem.placeholderProperty().removeListener(rowStateListener);
                    boundItem.selectedProperty().removeListener(rowStateListener);
                }
                super.updateItem(val, empty);
                boundItem = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (boundItem != null) {
                    boundItem.placeholderProperty().addListener(rowStateListener);
                    boundItem.selectedProperty().addListener(rowStateListener);
                }
                updateState();
            }
        });

        formDataTable.getColumns().addAll(checkCol, keyCol, valueCol, descCol, selectCol);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem addItem = new MenuItem("Add Row");
        MenuItem deleteItem = new MenuItem("Delete Row");
        addItem.setOnAction(e -> handleAddFormDataRow());
        deleteItem.setOnAction(e -> handleDeleteFormDataRow());
        contextMenu.getItems().addAll(addItem, deleteItem);
        formDataTable.setContextMenu(contextMenu);

        ensureFormDataPlaceholderRow();
        updateFormDataMasterCheckBox(formDataMasterCheckBox);
        updateFormDataMasterSelectBox(formDataMasterSelectBox);
    }

    private TableCell<KeyValuePair, String> createFormDataKeyCell() {
        return new TableCell<KeyValuePair, String>() {
            private final TextField textField = new TextField();
            private final Button typeBtn = new Button("Text \u25BE");
            private final HBox cellBox = new HBox(4, textField, typeBtn);
            private boolean updating = false;

            {
                textField.setPromptText("Key");
                textField.getStyleClass().add("param-cell-field");
                HBox.setHgrow(textField, Priority.ALWAYS);

                typeBtn.getStyleClass().add("form-data-type-btn");
                typeBtn.setFocusTraversable(false);

                cellBox.setAlignment(Pos.CENTER_LEFT);
                cellBox.setPadding(new Insets(0, 4, 0, 0));
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(cellBox);
                getStyleClass().add("param-table-cell");

                setOnMouseClicked(e -> {
                    if (e.getTarget() != typeBtn) {
                        textField.requestFocus();
                    }
                });

                typeBtn.setOnAction(e -> {
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row == null) {
                        return;
                    }
                    ContextMenu typeMenu = new ContextMenu();
                    MenuItem textItem = new MenuItem("Text");
                    MenuItem fileItem = new MenuItem("File");
                    textItem.setOnAction(ev -> {
                        row.setType("Text");
                        typeBtn.setText("Text \u25BE");
                        if (row.isPlaceholder()) {
                            row.setPlaceholder(false);
                            if (!row.isEnabled()) row.setEnabled(true);
                            ensureFormDataPlaceholderRow();
                            updateFormDataMasterCheckBox(formDataMasterCheckBox);
                            updateFormDataMasterSelectBox(formDataMasterSelectBox);
                        }
                        formDataTable.refresh();
                        onEditorFieldChanged();
                    });
                    fileItem.setOnAction(ev -> {
                        row.setType("File");
                        typeBtn.setText("File \u25BE");
                        if (row.isPlaceholder()) {
                            row.setPlaceholder(false);
                            if (!row.isEnabled()) row.setEnabled(true);
                            ensureFormDataPlaceholderRow();
                            updateFormDataMasterCheckBox(formDataMasterCheckBox);
                            updateFormDataMasterSelectBox(formDataMasterSelectBox);
                        }
                        formDataTable.refresh();
                        onEditorFieldChanged();
                    });
                    typeMenu.getItems().addAll(textItem, fileItem);
                    typeMenu.show(typeBtn, Side.BOTTOM, 0, 2);
                });

                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) {
                        return;
                    }
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row == null) {
                        return;
                    }
                    row.setKey(newVal != null ? newVal : "");
                    if (!isRowEmpty(row)) {
                        row.setPlaceholder(false);
                        if (!row.isEnabled()) {
                            row.setEnabled(true);
                        }
                        ensureFormDataPlaceholderRow();
                        updateFormDataMasterCheckBox(formDataMasterCheckBox);
                        updateFormDataMasterSelectBox(formDataMasterSelectBox);
                    }
                    onEditorFieldChanged();
                });

                textField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        e.consume();
                        focusFormDataCell(getIndex(), 2);
                    }
                });
            }

            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                KeyValuePair row = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (empty || row == null) {
                    updating = true;
                    try {
                        textField.setText("");
                        cellBox.setVisible(false);
                    } finally {
                        updating = false;
                    }
                } else {
                    cellBox.setVisible(true);
                    String current = row.getKey();
                    String safeCurrent = current != null ? current : "";
                    if (!safeCurrent.equals(textField.getText())) {
                        updating = true;
                        try {
                            textField.setText(safeCurrent);
                        } finally {
                            updating = false;
                        }
                    }
                    boolean isFile = "File".equalsIgnoreCase(row.getType());
                    typeBtn.setText(isFile ? "File \u25BE" : "Text \u25BE");
                    if (!row.isEnabled()) {
                        textField.setStyle("-fx-text-fill: -text-muted; -fx-opacity: 0.6;");
                        typeBtn.setOpacity(0.5);
                    } else {
                        textField.setStyle("");
                        typeBtn.setOpacity(1.0);
                    }

                    if (getIndex() == pendingFocusFormDataRow) {
                        pendingFocusFormDataRow = -1;
                        Platform.runLater(() -> {
                            textField.requestFocus();
                            textField.selectAll();
                        });
                    }
                }
            }
        };
    }

    private TableCell<KeyValuePair, String> createFormDataValueCell() {
        return new TableCell<KeyValuePair, String>() {
            private final TextField textField = new TextField();
            private final HBox fileBox = new HBox(6);
            private final Label fileNameLabel = new Label();
            private final Button chooseFileBtn = new Button("Select Files");
            private final Button clearFileBtn = new Button("\u00D7");
            private final StackPane container = new StackPane();
            private boolean updating = false;

            {
                textField.setPromptText("Value");
                textField.getStyleClass().add("param-cell-field");

                fileBox.setAlignment(Pos.CENTER_LEFT);
                fileBox.setPadding(new Insets(2, 8, 2, 8));

                fileNameLabel.getStyleClass().add("form-data-file-label");
                HBox.setHgrow(fileNameLabel, Priority.ALWAYS);

                chooseFileBtn.getStyleClass().add("form-data-file-btn");
                clearFileBtn.getStyleClass().add("form-data-file-clear-btn");
                HoverTooltip.attach(clearFileBtn, "Clear selected file");

                fileBox.getChildren().addAll(fileNameLabel, chooseFileBtn, clearFileBtn);

                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(textField, fileBox);

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(container);
                getStyleClass().add("param-table-cell");

                setOnMouseClicked(e -> {
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row != null && !"File".equalsIgnoreCase(row.getType())) {
                        textField.requestFocus();
                    }
                });

                chooseFileBtn.setOnAction(e -> {
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row == null) {
                        return;
                    }
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Select File");
                    chooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("All Files", "*.*"),
                            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                            new FileChooser.ExtensionFilter("PDF Documents", "*.pdf"),
                            new FileChooser.ExtensionFilter("Text / CSV / JSON", "*.txt", "*.csv", "*.json"),
                            new FileChooser.ExtensionFilter("Excel Spreadsheets", "*.xlsx", "*.xls")
                    );
                    File file = chooser.showOpenDialog(Main.getPrimaryStage());
                    if (file != null) {
                        row.setFilePath(file.getAbsolutePath());
                        row.setFileName(file.getName());
                        row.setValue(file.getName());
                        if (row.isPlaceholder()) {
                            row.setPlaceholder(false);
                            if (!row.isEnabled()) row.setEnabled(true);
                            ensureFormDataPlaceholderRow();
                            updateFormDataMasterCheckBox(formDataMasterCheckBox);
                            updateFormDataMasterSelectBox(formDataMasterSelectBox);
                        }
                        formDataTable.refresh();
                        onEditorFieldChanged();
                    }
                });

                clearFileBtn.setOnAction(e -> {
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row == null) {
                        return;
                    }
                    row.setFilePath("");
                    row.setFileName("");
                    row.setValue("");
                    formDataTable.refresh();
                    onEditorFieldChanged();
                });

                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) {
                        return;
                    }
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row == null || "File".equalsIgnoreCase(row.getType())) {
                        return;
                    }
                    row.setValue(newVal != null ? newVal : "");
                    if (!isRowEmpty(row)) {
                        row.setPlaceholder(false);
                        if (!row.isEnabled()) {
                            row.setEnabled(true);
                        }
                        ensureFormDataPlaceholderRow();
                        updateFormDataMasterCheckBox(formDataMasterCheckBox);
                        updateFormDataMasterSelectBox(formDataMasterSelectBox);
                    }
                    onEditorFieldChanged();
                });

                textField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        e.consume();
                        focusFormDataCell(getIndex(), 3);
                    }
                });
            }

            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                KeyValuePair row = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (empty || row == null) {
                    updating = true;
                    try {
                        textField.setText("");
                        container.setVisible(false);
                    } finally {
                        updating = false;
                    }
                } else {
                    container.setVisible(true);
                    boolean isFile = "File".equalsIgnoreCase(row.getType());
                    if (isFile) {
                        textField.setVisible(false);
                        fileBox.setVisible(true);
                        boolean hasFile = row.getFilePath() != null && !row.getFilePath().isBlank();
                        if (hasFile) {
                            fileNameLabel.setText(row.getFileName() != null ? row.getFileName() : new File(row.getFilePath()).getName());
                            fileNameLabel.setVisible(true);
                            fileNameLabel.setManaged(true);
                            chooseFileBtn.setText("Change");
                            clearFileBtn.setVisible(true);
                            clearFileBtn.setManaged(true);
                        } else {
                            fileNameLabel.setText("");
                            fileNameLabel.setVisible(false);
                            fileNameLabel.setManaged(false);
                            chooseFileBtn.setText("Select Files");
                            clearFileBtn.setVisible(false);
                            clearFileBtn.setManaged(false);
                        }
                    } else {
                        fileBox.setVisible(false);
                        textField.setVisible(true);
                        String current = row.getValue();
                        String safeCurrent = current != null ? current : "";
                        if (!safeCurrent.equals(textField.getText())) {
                            updating = true;
                            try {
                                textField.setText(safeCurrent);
                            } finally {
                                updating = false;
                            }
                        }
                    }

                    if (!row.isEnabled()) {
                        textField.setStyle("-fx-text-fill: -text-muted; -fx-opacity: 0.6;");
                        fileBox.setOpacity(0.6);
                    } else {
                        textField.setStyle("");
                        fileBox.setOpacity(1.0);
                    }
                }
            }
        };
    }

    private TableCell<KeyValuePair, String> createFormDataDescCell() {
        return new TableCell<KeyValuePair, String>() {
            private final TextField textField = new TextField();
            private boolean updating = false;

            {
                textField.setPromptText("Description");
                textField.getStyleClass().add("param-cell-field");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(textField);
                getStyleClass().add("param-table-cell");

                setOnMouseClicked(e -> textField.requestFocus());

                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) {
                        return;
                    }
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row == null) {
                        return;
                    }
                    row.setDescription(newVal != null ? newVal : "");
                    if (!isRowEmpty(row)) {
                        row.setPlaceholder(false);
                        if (!row.isEnabled()) {
                            row.setEnabled(true);
                        }
                        ensureFormDataPlaceholderRow();
                        updateFormDataMasterCheckBox(formDataMasterCheckBox);
                        updateFormDataMasterSelectBox(formDataMasterSelectBox);
                    }
                    onEditorFieldChanged();
                });

                textField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        e.consume();
                        int nextRow = getIndex() + 1;
                        if (nextRow < formData.size()) {
                            focusFormDataCell(nextRow, 1);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                KeyValuePair row = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (empty || row == null) {
                    updating = true;
                    try {
                        textField.setText("");
                        textField.setVisible(false);
                    } finally {
                        updating = false;
                    }
                } else {
                    textField.setVisible(true);
                    String current = row.getDescription();
                    String safeCurrent = current != null ? current : "";
                    if (!safeCurrent.equals(textField.getText())) {
                        updating = true;
                        try {
                            textField.setText(safeCurrent);
                        } finally {
                            updating = false;
                        }
                    }
                    if (!row.isEnabled()) {
                        textField.setStyle("-fx-text-fill: -text-muted; -fx-opacity: 0.6;");
                    } else {
                        textField.setStyle("");
                    }
                }
            }
        };
    }

    private void focusFormDataCell(int rowIdx, int colIdx) {
        Platform.runLater(() -> {
            formDataTable.scrollTo(rowIdx);
            if (colIdx >= 0 && colIdx < formDataTable.getColumns().size()) {
                formDataTable.getFocusModel().focus(rowIdx, formDataTable.getColumns().get(colIdx));
            }
            Platform.runLater(() -> {
                if (colIdx >= 0 && colIdx < formDataTable.getColumns().size()) {
                    var targetCol = formDataTable.getColumns().get(colIdx);
                    for (Node n : formDataTable.lookupAll(".param-table-cell")) {
                        if (n instanceof TableCell<?, ?> tc && tc.getTableColumn() == targetCol && tc.getIndex() == rowIdx) {
                            if (tc.getGraphic() instanceof TextField tf) {
                                tf.requestFocus();
                                tf.selectAll();
                                break;
                            } else if (tc.getGraphic() instanceof Pane pane) {
                                for (Node child : pane.getChildren()) {
                                    if (child instanceof TextField tf && tf.isVisible()) {
                                        tf.requestFocus();
                                        tf.selectAll();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            });
        });
    }

    private void ensureFormDataPlaceholderRow() {
        if (formData.isEmpty()) {
            KeyValuePair ph = new KeyValuePair(true, "", "", "", true);
            ph.setType("Text");
            formData.add(ph);
            return;
        }
        while (formData.size() > 1) {
            KeyValuePair last = formData.get(formData.size() - 1);
            KeyValuePair secondLast = formData.get(formData.size() - 2);
            if (last.isPlaceholder() && isRowEmpty(last) && secondLast.isPlaceholder() && isRowEmpty(secondLast)) {
                formData.remove(formData.size() - 1);
            } else {
                break;
            }
        }
        KeyValuePair last = formData.get(formData.size() - 1);
        if (!last.isPlaceholder()) {
            KeyValuePair ph = new KeyValuePair(true, "", "", "", true);
            ph.setType("Text");
            formData.add(ph);
        }
    }

    private void updateFormDataMasterCheckBox(CheckBox masterCheckBox) {
        if (masterCheckBox == null) return;
        long totalActive = formData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder()).count();
        if (totalActive == 0) {
            masterCheckBox.setSelected(true);
            masterCheckBox.setIndeterminate(false);
            return;
        }
        long enabledCount = formData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder() && kv.isEnabled()).count();
        if (enabledCount == totalActive) {
            masterCheckBox.setSelected(true);
            masterCheckBox.setIndeterminate(false);
        } else if (enabledCount == 0) {
            masterCheckBox.setSelected(false);
            masterCheckBox.setIndeterminate(false);
        } else {
            masterCheckBox.setIndeterminate(true);
        }
    }

    private void updateFormDataMasterSelectBox(CheckBox masterSelectBox) {
        if (masterSelectBox == null) return;
        long totalActive = formData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder()).count();
        if (totalActive == 0) {
            masterSelectBox.setSelected(false);
            masterSelectBox.setIndeterminate(false);
            return;
        }
        long selectedCount = formData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder() && kv.isSelected()).count();
        if (selectedCount == totalActive) {
            masterSelectBox.setSelected(true);
            masterSelectBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            masterSelectBox.setSelected(false);
            masterSelectBox.setIndeterminate(false);
        } else {
            masterSelectBox.setIndeterminate(true);
        }
    }

    private void setupUrlEncodedTable() {
        urlEncodedTable.getColumns().clear();
        urlEncodedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        urlEncodedTable.setItems(urlEncodedData);
        urlEncodedTable.setEditable(true);
        urlEncodedTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        urlEncodedTable.setRowFactory(tv -> new TableRow<>() {
            private final javafx.beans.value.ChangeListener<Boolean> selListener = (obs, oldVal, newVal) -> {
                pseudoClassStateChanged(ROW_SELECTED_PSEUDO, Boolean.TRUE.equals(newVal));
            };

            @Override
            protected void updateItem(KeyValuePair item, boolean empty) {
                KeyValuePair oldItem = getItem();
                if (oldItem != null) {
                    oldItem.selectedProperty().removeListener(selListener);
                }
                super.updateItem(item, empty);
                if (empty || item == null) {
                    pseudoClassStateChanged(ROW_SELECTED_PSEUDO, false);
                } else {
                    pseudoClassStateChanged(ROW_SELECTED_PSEUDO, item.isSelected());
                    item.selectedProperty().addListener(selListener);
                }
            }
        });

        CheckBox masterCheckBox = new CheckBox();
        this.urlEncodedMasterCheckBox = masterCheckBox;
        masterCheckBox.setAlignment(Pos.CENTER);
        masterCheckBox.setMaxWidth(Double.MAX_VALUE);
        masterCheckBox.setSelected(true);
        masterCheckBox.setOnAction(e -> {
            boolean selectAll = masterCheckBox.isSelected();
            for (KeyValuePair kv : urlEncodedData) {
                if (!isRowEmpty(kv) && !kv.isPlaceholder()) {
                    kv.setEnabled(selectAll);
                }
            }
            urlEncodedTable.refresh();
            onEditorFieldChanged();
        });

        TableColumn<KeyValuePair, Boolean> checkCol = new TableColumn<>();
        checkCol.setGraphic(masterCheckBox);
        checkCol.getStyleClass().add("param-check-col");
        checkCol.setMinWidth(38);
        checkCol.setMaxWidth(38);
        checkCol.setPrefWidth(38);
        checkCol.setResizable(false);
        checkCol.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());
        checkCol.setCellFactory(col -> new TableCell<KeyValuePair, Boolean>() {
            private final CheckBox rowCheckBox = new CheckBox();
            private final javafx.beans.InvalidationListener rowStateListener = obs -> updateState();
            private KeyValuePair boundItem = null;

            private void updateState() {
                KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item == null || (item.isPlaceholder() && isRowEmpty(item))) {
                    rowCheckBox.setVisible(false);
                } else {
                    rowCheckBox.setVisible(true);
                    rowCheckBox.setSelected(item.isEnabled());
                }
            }

            {
                rowCheckBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(rowCheckBox);
                getStyleClass().add("param-check-cell");
                rowCheckBox.setOnAction(e -> {
                    KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setEnabled(rowCheckBox.isSelected());
                        onEditorFieldChanged();
                        updateUrlEncodedMasterCheckBox(urlEncodedMasterCheckBox);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean val, boolean empty) {
                if (boundItem != null) {
                    boundItem.placeholderProperty().removeListener(rowStateListener);
                    boundItem.enabledProperty().removeListener(rowStateListener);
                }
                super.updateItem(val, empty);
                boundItem = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (boundItem != null) {
                    boundItem.placeholderProperty().addListener(rowStateListener);
                    boundItem.enabledProperty().addListener(rowStateListener);
                }
                updateState();
            }
        });

        TableColumn<KeyValuePair, String> keyCol = new TableColumn<>("Key");
        keyCol.setMinWidth(140);
        keyCol.setPrefWidth(220);
        keyCol.setCellValueFactory(cellData -> cellData.getValue().keyProperty());
        keyCol.setCellFactory(col -> createUrlEncodedCell("Key", 0, KeyValuePair::getKey, KeyValuePair::setKey));

        TableColumn<KeyValuePair, String> valueCol = new TableColumn<>("Value");
        valueCol.setMinWidth(140);
        valueCol.setPrefWidth(240);
        valueCol.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        valueCol.setCellFactory(col -> createUrlEncodedCell("Value", 1, KeyValuePair::getValue, KeyValuePair::setValue));

        TableColumn<KeyValuePair, String> descCol = new TableColumn<>("Description");
        descCol.setMinWidth(140);
        descCol.setPrefWidth(240);
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descCol.setCellFactory(col -> createUrlEncodedCell("Description", 2, KeyValuePair::getDescription, KeyValuePair::setDescription));

        CheckBox masterSelectBox = new CheckBox();
        this.urlEncodedMasterSelectBox = masterSelectBox;
        masterSelectBox.setAlignment(Pos.CENTER);
        masterSelectBox.setMaxWidth(Double.MAX_VALUE);
        masterSelectBox.setOnAction(e -> {
            boolean selectAll = masterSelectBox.isSelected();
            for (KeyValuePair kv : urlEncodedData) {
                if (selectAll) {
                    if (!isRowEmpty(kv) && !kv.isPlaceholder()) {
                        kv.setSelected(true);
                    }
                } else {
                    kv.setSelected(false);
                }
            }
            urlEncodedTable.getSelectionModel().clearSelection();
            urlEncodedTable.refresh();
        });

        TableColumn<KeyValuePair, Boolean> selectCol = new TableColumn<>();
        selectCol.setGraphic(masterSelectBox);
        selectCol.getStyleClass().add("param-select-col");
        selectCol.setMinWidth(38);
        selectCol.setMaxWidth(38);
        selectCol.setPrefWidth(38);
        selectCol.setResizable(false);
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(col -> new TableCell<KeyValuePair, Boolean>() {
            private final CheckBox rowSelectBox = new CheckBox();
            private final javafx.beans.InvalidationListener rowStateListener = obs -> updateState();
            private KeyValuePair boundItem = null;

            private void updateState() {
                KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item == null || (item.isPlaceholder() && isRowEmpty(item))) {
                    rowSelectBox.setVisible(false);
                } else {
                    rowSelectBox.setVisible(true);
                    rowSelectBox.setSelected(item.isSelected());
                }
            }

            {
                rowSelectBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(rowSelectBox);
                getStyleClass().add("param-select-cell");
                rowSelectBox.setOnAction(e -> {
                    KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setSelected(rowSelectBox.isSelected());
                        updateUrlEncodedMasterSelectBox(urlEncodedMasterSelectBox);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean val, boolean empty) {
                if (boundItem != null) {
                    boundItem.placeholderProperty().removeListener(rowStateListener);
                    boundItem.selectedProperty().removeListener(rowStateListener);
                }
                super.updateItem(val, empty);
                boundItem = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (boundItem != null) {
                    boundItem.placeholderProperty().addListener(rowStateListener);
                    boundItem.selectedProperty().addListener(rowStateListener);
                }
                updateState();
            }
        });

        urlEncodedTable.getColumns().addAll(checkCol, keyCol, valueCol, descCol, selectCol);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem addItem = new MenuItem("Add Row");
        MenuItem deleteItem = new MenuItem("Delete Row");
        addItem.setOnAction(e -> handleAddFormDataRow());
        deleteItem.setOnAction(e -> handleDeleteFormDataRow());
        contextMenu.getItems().addAll(addItem, deleteItem);
        urlEncodedTable.setContextMenu(contextMenu);

        ensureUrlEncodedPlaceholderRow();
        updateUrlEncodedMasterCheckBox(urlEncodedMasterCheckBox);
        updateUrlEncodedMasterSelectBox(urlEncodedMasterSelectBox);
    }

    private TableCell<KeyValuePair, String> createUrlEncodedCell(String placeholder,
                                                                 int colIndex,
                                                                 Function<KeyValuePair, String> getter,
                                                                 BiConsumer<KeyValuePair, String> setter) {
        return new TableCell<KeyValuePair, String>() {
            private final TextField textField = new TextField();
            private boolean updating = false;

            {
                textField.setPromptText(placeholder);
                textField.getStyleClass().add("param-cell-field");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(textField);
                getStyleClass().add("param-table-cell");

                setOnMouseClicked(e -> textField.requestFocus());

                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) {
                        return;
                    }
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row == null) {
                        return;
                    }
                    setter.accept(row, newVal != null ? newVal : "");

                    if (!isRowEmpty(row)) {
                        row.setPlaceholder(false);
                        if (!row.isEnabled()) {
                            row.setEnabled(true);
                        }
                        ensureUrlEncodedPlaceholderRow();
                        updateUrlEncodedMasterCheckBox(urlEncodedMasterCheckBox);
                        updateUrlEncodedMasterSelectBox(urlEncodedMasterSelectBox);
                    }
                    onEditorFieldChanged();
                });

                textField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        e.consume();
                        if (colIndex < 2) {
                            focusUrlEncodedCell(getIndex(), colIndex + 2);
                        } else {
                            int nextRow = getIndex() + 1;
                            if (nextRow < urlEncodedData.size()) {
                                focusUrlEncodedCell(nextRow, 1);
                            }
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                KeyValuePair row = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (empty || row == null) {
                    updating = true;
                    try {
                        textField.setText("");
                        textField.setVisible(false);
                    } finally {
                        updating = false;
                    }
                } else {
                    textField.setVisible(true);
                    String current = getter.apply(row);
                    String safeCurrent = current != null ? current : "";
                    if (!safeCurrent.equals(textField.getText())) {
                        updating = true;
                        try {
                            textField.setText(safeCurrent);
                        } finally {
                            updating = false;
                        }
                    }
                    if (!row.isEnabled()) {
                        textField.setStyle("-fx-text-fill: -text-muted; -fx-opacity: 0.6;");
                    } else {
                        textField.setStyle("");
                    }

                    if (colIndex == 0 && getIndex() == pendingFocusUrlEncodedRow) {
                        pendingFocusUrlEncodedRow = -1;
                        Platform.runLater(() -> {
                            textField.requestFocus();
                            textField.selectAll();
                        });
                    }
                }
            }
        };
    }

    private void focusUrlEncodedCell(int rowIdx, int colIdx) {
        Platform.runLater(() -> {
            urlEncodedTable.scrollTo(rowIdx);
            if (colIdx >= 0 && colIdx < urlEncodedTable.getColumns().size()) {
                urlEncodedTable.getFocusModel().focus(rowIdx, urlEncodedTable.getColumns().get(colIdx));
            }
            Platform.runLater(() -> {
                if (colIdx >= 0 && colIdx < urlEncodedTable.getColumns().size()) {
                    var targetCol = urlEncodedTable.getColumns().get(colIdx);
                    for (Node n : urlEncodedTable.lookupAll(".param-table-cell")) {
                        if (n instanceof TableCell<?, ?> tc && tc.getTableColumn() == targetCol && tc.getIndex() == rowIdx) {
                            if (tc.getGraphic() instanceof TextField tf) {
                                tf.requestFocus();
                                tf.selectAll();
                                break;
                            }
                        }
                    }
                }
            });
        });
    }

    private void ensureUrlEncodedPlaceholderRow() {
        if (urlEncodedData.isEmpty()) {
            urlEncodedData.add(new KeyValuePair(true, "", "", "", true));
            return;
        }
        while (urlEncodedData.size() > 1) {
            KeyValuePair last = urlEncodedData.get(urlEncodedData.size() - 1);
            KeyValuePair secondLast = urlEncodedData.get(urlEncodedData.size() - 2);
            if (last.isPlaceholder() && isRowEmpty(last) && secondLast.isPlaceholder() && isRowEmpty(secondLast)) {
                urlEncodedData.remove(urlEncodedData.size() - 1);
            } else {
                break;
            }
        }
        KeyValuePair last = urlEncodedData.get(urlEncodedData.size() - 1);
        if (!last.isPlaceholder()) {
            urlEncodedData.add(new KeyValuePair(true, "", "", "", true));
        }
    }

    private void updateUrlEncodedMasterCheckBox(CheckBox masterCheckBox) {
        if (masterCheckBox == null) return;
        long totalActive = urlEncodedData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder()).count();
        if (totalActive == 0) {
            masterCheckBox.setSelected(true);
            masterCheckBox.setIndeterminate(false);
            return;
        }
        long enabledCount = urlEncodedData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder() && kv.isEnabled()).count();
        if (enabledCount == totalActive) {
            masterCheckBox.setSelected(true);
            masterCheckBox.setIndeterminate(false);
        } else if (enabledCount == 0) {
            masterCheckBox.setSelected(false);
            masterCheckBox.setIndeterminate(false);
        } else {
            masterCheckBox.setIndeterminate(true);
        }
    }

    private void updateUrlEncodedMasterSelectBox(CheckBox masterSelectBox) {
        if (masterSelectBox == null) return;
        long totalActive = urlEncodedData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder()).count();
        if (totalActive == 0) {
            masterSelectBox.setSelected(false);
            masterSelectBox.setIndeterminate(false);
            return;
        }
        long selectedCount = urlEncodedData.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder() && kv.isSelected()).count();
        if (selectedCount == totalActive) {
            masterSelectBox.setSelected(true);
            masterSelectBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            masterSelectBox.setSelected(false);
            masterSelectBox.setIndeterminate(false);
        } else {
            masterSelectBox.setIndeterminate(true);
        }
    }

    private void setupSocketIoEventsTable() {
        if (socketIoEventsTable == null) {
            return;
        }
        socketIoEventsTable.getColumns().clear();
        socketIoEventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        socketIoEventsTable.setItems(socketIoEventsData);
        socketIoEventsTable.setEditable(true);

        TableColumn<SocketIoEventItem, String> eventCol = new TableColumn<>("EVENTS");
        eventCol.setCellValueFactory(cellData -> cellData.getValue().eventNameProperty());
        eventCol.setCellFactory(col -> commitOnFocusLossEventCell("Add event"));
        eventCol.setOnEditCommit(event -> {
            event.getRowValue().setEventName(event.getNewValue());
            onEditorFieldChanged();
        });
        eventCol.setPrefWidth(220);

        TableColumn<SocketIoEventItem, Boolean> listenCol = new TableColumn<>("LISTEN");
        listenCol.setCellValueFactory(cellData -> cellData.getValue().listenProperty());
        listenCol.setCellFactory(col -> createListenToggleCell());
        listenCol.setPrefWidth(100);
        listenCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<SocketIoEventItem, String> descCol = new TableColumn<>("DESCRIPTION");
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descCol.setCellFactory(col -> commitOnFocusLossEventCell("Add description"));
        descCol.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            onEditorFieldChanged();
        });
        descCol.setPrefWidth(260);

        socketIoEventsTable.getColumns().addAll(eventCol, listenCol, descCol);

        if (socketIoEventsData.isEmpty()) {
            socketIoEventsData.add(new SocketIoEventItem("", true, ""));
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem addItem = new MenuItem("Add Event");
        MenuItem deleteItem = new MenuItem("Delete Event");
        addItem.setOnAction(e -> handleAddSocketIoEventRow());
        deleteItem.setOnAction(e -> handleDeleteSocketIoEventRow());
        contextMenu.getItems().addAll(addItem, deleteItem);
        socketIoEventsTable.setContextMenu(contextMenu);
    }

    private TableCell<SocketIoEventItem, String> commitOnFocusLossEventCell(String promptText) {
        return new TextFieldTableCell<SocketIoEventItem, String>(new javafx.util.converter.DefaultStringConverter()) {
            @Override
            public void startEdit() {
                super.startEdit();
                if (getGraphic() instanceof TextField field) {
                    field.setPromptText(promptText);
                    field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                        if (!isFocused && isEditing()) {
                            commitEdit(field.getText());
                        }
                    });
                }
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (!isEditing()) {
                    if (item == null || item.trim().isEmpty()) {
                        setText(promptText);
                        setStyle("-fx-text-fill: -text-muted; -fx-font-style: italic;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: -text-primary; -fx-font-style: normal;");
                    }
                }
            }
        };
    }

    private TableCell<SocketIoEventItem, Boolean> createListenToggleCell() {
        return new TableCell<>() {
            private final ToggleButton toggle = new ToggleButton();
            private final Region thumb = new Region();
            {
                thumb.getStyleClass().add("toggle-thumb");
                thumb.setMouseTransparent(true);
                toggle.getStyleClass().add("event-toggle-switch");
                toggle.setGraphic(thumb);
                toggle.setOnAction(e -> {
                    SocketIoEventItem item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setListen(toggle.isSelected());
                        updateToggleAppearance(toggle.isSelected());
                        onEditorFieldChanged();
                    }
                });
                setAlignment(Pos.CENTER);
            }

            private void updateToggleAppearance(boolean selected) {
                if (selected) {
                    toggle.setStyle("-fx-background-color: #007acc; -fx-alignment: center-right;");
                } else {
                    toggle.setStyle("-fx-background-color: #3e4451; -fx-alignment: center-left;");
                }
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    boolean val = item != null && item;
                    toggle.setSelected(val);
                    updateToggleAppearance(val);
                    setGraphic(toggle);
                    setText(null);
                }
            }
        };
    }

    @FXML
    private void handleAddSocketIoEventRow() {
        SocketIoEventItem newItem = new SocketIoEventItem("", true, "");
        socketIoEventsData.add(newItem);
        if (socketIoEventsTable != null) {
            int newIdx = socketIoEventsData.size() - 1;
            socketIoEventsTable.getSelectionModel().select(newIdx);
            socketIoEventsTable.scrollTo(newIdx);
            socketIoEventsTable.edit(newIdx, socketIoEventsTable.getColumns().get(0));
        }
        onEditorFieldChanged();
    }

    @FXML
    private void handleDeleteSocketIoEventRow() {
        if (socketIoEventsTable != null) {
            SocketIoEventItem selected = socketIoEventsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                socketIoEventsData.remove(selected);
                onEditorFieldChanged();
            }
        }
    }

    @FXML
    private void handleClearWsMessages() {
        if (wsMessageLog != null) {
            wsMessageLog.getItems().clear();
        }
    }

    @FXML
    private void handleClearSocketIoMessages() {
        if (socketIoEventLog != null) {
            socketIoEventLog.getItems().clear();
        }
        socketIoEventsSeen = 0;
    }

    private void setupEnvironmentCombo() {
        environmentCombo.getItems().addAll("No Environment");
        environmentCombo.getSelectionModel().select(0);
        environmentCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateEnvironmentVariables(newVal);
            if (isGraphqlActive() && isGraphqlAutoFetchEnabled) {
                triggerGraphqlAutoFetch(false);
            }
            if (!restoringSession) {
                scheduleSessionSave();
            }
        });
    }

    /** Every variable in scope for the currently active tab, merged with
     * Postman's real precedence (later entries win): Vault < Collection <
     * Global < Environment. Used for both {{var}} autocomplete and actual
     * send-time resolution (see VariableResolver), so saving a variable to
     * any scope from "Set as variable" takes effect everywhere immediately. */
    private Map<String, String> currentEnvironmentVariables() {
        Map<String, String> merged = new LinkedHashMap<>(VaultService.asPrefixedVariables());

        Tab active = activeRequestTab();
        RequestTabState state = active != null ? tabStates.get(active) : null;
        if (state != null && state.collectionId != null) {
            Map<String, String> collectionVars = collectionVariablesCache.get(state.collectionId);
            if (collectionVars != null) {
                merged.putAll(collectionVars);
            }
        }

        if (globalsEnvironment != null && globalsEnvironment.getVariables() != null) {
            merged.putAll(globalsEnvironment.getVariables());
        }

        String selected = environmentCombo.getValue();
        if (selected != null && !"No Environment".equals(selected)) {
            EnvironmentResponse env = environmentsMap.get(selected);
            if (env != null && env.getVariables() != null) {
                merged.putAll(env.getVariables());
            }
        }
        return merged;
    }

    /** Fetches this collection's variables once (best-effort, cached
     * afterward) so Collection-scoped variables are ready by the time the
     * person sends a request or opens "Set as variable" again. */
    private void ensureCollectionVariablesLoaded(Long collectionId) {
        if (collectionId == null || collectionVariablesCache.containsKey(collectionId)) {
            return;
        }
        new Thread(() -> {
            Optional<CollectionResponse> collection = CollectionService.getCollectionById(collectionId);
            Map<String, String> vars = collection.map(CollectionResponse::getVariables).orElse(null);
            collectionVariablesCache.put(collectionId, vars != null ? vars : new LinkedHashMap<>());
        }).start();
    }

    /** Postman's "Set as variable" > Environment scope. */
    private void saveEnvironmentVariable(String name, String value) {
        String envName = environmentCombo != null ? environmentCombo.getValue() : null;
        if (envName == null || "No Environment".equals(envName)) {
            AlertUtils.showError("Select (or create) an environment first, then try again.");
            return;
        }
        EnvironmentResponse env = environmentsMap.get(envName);
        if (env == null) {
            AlertUtils.showError("Couldn't find the active environment.");
            return;
        }
        Map<String, String> updated = new LinkedHashMap<>(
                env.getVariables() != null ? env.getVariables() : Collections.emptyMap());
        updated.put(name, value);
        env.setVariables(updated);
        new Thread(() -> {
            EnvironmentService.updateEnvironmentVariables(env.getId(), updated);
            Platform.runLater(() -> updateStatus("Variable \"" + name + "\" saved to " + envName));
        }).start();
    }

    /** Postman's "Set as variable" > Global scope — writes to the
     * reserved, hidden "Globals" environment, creating it on first use. */
    private void saveGlobalVariable(String name, String value) {
        new Thread(() -> {
            EnvironmentResponse target = globalsEnvironment;
            if (target == null) {
                target = EnvironmentService.getUserEnvironments().orElse(Collections.emptyList()).stream()
                        .filter(e -> "Globals".equals(e.getName()))
                        .findFirst()
                        .orElse(null);
            }
            Optional<EnvironmentResponse> saved;
            if (target == null) {
                saved = EnvironmentService.createEnvironment("Globals",
                        "Global variables — not shown in the environment switcher",
                        new LinkedHashMap<>(Map.of(name, value)));
            } else {
                Map<String, String> updated = new LinkedHashMap<>(
                        target.getVariables() != null ? target.getVariables() : Collections.emptyMap());
                updated.put(name, value);
                saved = EnvironmentService.updateEnvironmentVariables(target.getId(), updated);
            }
            if (saved.isPresent()) {
                globalsEnvironment = saved.get();
                Platform.runLater(() -> updateStatus("Global variable \"" + name + "\" saved"));
            } else {
                Platform.runLater(() -> AlertUtils.showError("Failed to save global variable \"" + name + "\""));
            }
        }).start();
    }

    /** Postman's "Set as variable" > Collection scope. */
    private void saveCollectionVariable(Long collectionId, String name, String value) {
        if (collectionId == null) {
            AlertUtils.showError("Save this request into a collection first, then try again.");
            return;
        }
        new Thread(() -> {
            Map<String, String> current = new LinkedHashMap<>();
            Optional<CollectionResponse> latest = CollectionService.getCollectionById(collectionId);
            latest.map(CollectionResponse::getVariables).ifPresent(current::putAll);
            current.put(name, value);
            Optional<CollectionResponse> saved = CollectionService.updateCollectionVariables(collectionId, current);
            if (saved.isPresent()) {
                collectionVariablesCache.put(collectionId,
                        saved.get().getVariables() != null ? saved.get().getVariables() : current);
                Platform.runLater(() -> updateStatus("Collection variable \"" + name + "\" saved"));
            } else {
                Platform.runLater(() -> AlertUtils.showError("Failed to save collection variable \"" + name + "\""));
            }
        }).start();
    }

    /** Postman's "Set as variable" > Vault scope — device-local only. */
    private void saveVaultVariable(String name, String value) {
        VaultService.setSecret(name, value);
        updateStatus("Vault secret \"" + name + "\" saved (this device only, never synced)");
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

    private void setupCollectionsTree() {
        TreeItem<String> root = new TreeItem<>("Collections");
        root.setExpanded(true);
        collectionsTree.setRoot(root);

        // Handle collection selection
        collectionsTree.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (restoringTreeSelection) {
                        return;
                    }
                    if (newValue != null && newValue.isLeaf()) {
                        handleItemSelected(newValue);
                    }
                    if (!restoringSession) {
                        scheduleSessionSave();
                    }
                }
        );

        collectionsTree.addEventHandler(TreeItem.branchExpandedEvent(), e -> {
            if (!restoringSession) {
                scheduleSessionSave();
            }
        });
        collectionsTree.addEventHandler(TreeItem.branchCollapsedEvent(), e -> {
            if (!restoringSession) {
                scheduleSessionSave();
            }
        });
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
                if (requestResponse.getMethod() != null) {
                    String m = requestResponse.getMethod().name();
                    if (state != null) {
                        state.method = m;
                    }
                    methodCombo.setValue(m);
                    updateRequestBuilderTabs(m);
                    updateResponseTabs(m);
                }
                String reqUrl = requestResponse.getUrl();
                if (reqUrl == null || reqUrl.isEmpty()
                        || "https://api.example.com/endpoint".equalsIgnoreCase(reqUrl.trim())
                        || "Enter URL or paste text".equalsIgnoreCase(reqUrl.trim())) {
                    urlField.replaceText("");
                } else {
                    urlField.replaceText(reqUrl);
                }

                // Load headers
                headersData.clear();
                if (requestResponse.getHeaders() != null && !requestResponse.getHeaders().isEmpty()) {
                    try {
                        JSONObject headersJson = new JSONObject(requestResponse.getHeaders());
                        for (String key : headersJson.keySet()) {
                            headersData.add(new KeyValuePair(true, key, headersJson.getString(key), "", false));
                        }
                    } catch (Exception e) {
                        // Fallback to simple header parsing
                        String[] headerLines = requestResponse.getHeaders().split("\n");
                        for (String line : headerLines) {
                            String[] parts = line.split(":", 2);
                            if (parts.length == 2) {
                                headersData.add(new KeyValuePair(true, parts[0].trim(), parts[1].trim(), "", false));
                            }
                        }
                    }
                }
                ensureHeadersPlaceholderRow();
                updateHeadersMasterCheckBox(headersMasterCheckBox);
                updateHeadersMasterSelectBox(headersMasterSelectBox);
                ensureFormDataPlaceholderRow();
                ensureUrlEncodedPlaceholderRow();

                // Load body
                bodyTextArea.replaceText(requestResponse.getBody() != null ? requestResponse.getBody() : "");
                if (bodyCommentHighlighter != null) {
                    bodyCommentHighlighter.refresh();
                }
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
                boolean isGraphqlBody = false;
                if (requestResponse.getBody() != null && !requestResponse.getBody().isBlank()) {
                    String rawBody = requestResponse.getBody().trim();
                    if (rawBody.startsWith("{") && rawBody.contains("\"query\"")) {
                        try {
                            JSONObject obj = new JSONObject(rawBody);
                            if (obj.has("query")) {
                                isGraphqlBody = true;
                                String q = obj.optString("query", "");
                                if (graphqlQueryArea != null) {
                                    graphqlQueryArea.replaceText(q);
                                }
                                if (graphqlVariablesArea != null) {
                                    Object vars = obj.opt("variables");
                                    if (vars instanceof JSONObject jo) {
                                        graphqlVariablesArea.replaceText(jo.toString(2));
                                    } else if (vars != null) {
                                        graphqlVariablesArea.replaceText(vars.toString());
                                    } else {
                                        graphqlVariablesArea.replaceText("");
                                    }
                                }
                                if (graphqlBodyTypeButton != null) {
                                    graphqlBodyTypeButton.setSelected(true);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (!isGraphqlBody) {
                    if (requestResponse.getBody() != null && !requestResponse.getBody().isBlank()
                            && rawBodyTypeButton != null) {
                        rawBodyTypeButton.setSelected(true);
                    } else if (noneBodyTypeButton != null) {
                        noneBodyTypeButton.setSelected(true);
                    }
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
                state.collectionId = requestResponse.getCollectionId();
                ensureCollectionVariablesLoaded(state.collectionId);
                ensureCommentThreadsLoaded(state.requestId);
            }
            if (currentTab != null) {
                refreshTabGraphic(currentTab);
            }
            scheduleSessionSave();

            updateStatus("Request loaded: " + requestResponse.getName());
        });
    }

    // ============================================================
    // User profile (header menu + Profile dialog)
    // ============================================================

    /** Puts the real person in the header immediately (username from the
     * stored login), then upgrades to their full name + avatar once
     * /users/me answers. */
    private void loadUserProfile() {
        if (userMenuBtn != null) {
            String username = TokenManager.getUsername();
            if (username != null && !username.isBlank()) {
                userMenuBtn.setText(username);
            }
            updateUserMenuSessionItem();
            userMenuBtn.setOnShowing(ev -> updateUserMenuSessionItem());
        }
        new Thread(() -> UserService.getMe().ifPresent(profile ->
                Platform.runLater(() -> applyProfileToHeader(profile)))).start();
    }

    private void updateUserMenuSessionItem() {
        if (userMenuBtn == null) {
            return;
        }
        MenuItem sessionItem = null;
        for (MenuItem item : userMenuBtn.getItems()) {
            if ("sessionInfoItem".equals(item.getId())) {
                sessionItem = item;
                break;
            }
        }
        String remaining = TokenManager.getSessionTimeRemainingHumanReadable();
        if (sessionItem == null) {
            sessionItem = new MenuItem("Session: " + remaining);
            sessionItem.setId("sessionInfoItem");
            sessionItem.setDisable(true);
            userMenuBtn.getItems().add(0, sessionItem);
            userMenuBtn.getItems().add(1, new SeparatorMenuItem());
        } else {
            sessionItem.setText("Session: " + remaining);
        }
    }

    private void applyProfileToHeader(UserProfile profile) {
        currentUserProfile = profile;
        if (userMenuBtn != null) {
            userMenuBtn.setText(profile.displayName());
        }
        if (userAvatarView != null) {
            Image avatar = decodeAvatarImage(profile.getAvatarBase64());
            if (avatar != null) {
                userAvatarView.setImage(avatar);
                userAvatarView.setPreserveRatio(false);
                userAvatarView.setFitWidth(20);
                userAvatarView.setFitHeight(20);
                userAvatarView.setClip(new javafx.scene.shape.Circle(10, 10, 10));
            } else {
                // Back to the stock icon (e.g. after "Remove image")
                userAvatarView.setClip(null);
                userAvatarView.setPreserveRatio(true);
                try {
                    userAvatarView.setImage(new Image(Objects.requireNonNull(
                            getClass().getResourceAsStream("/images/user-icon.png"))));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Image decodeAvatarImage(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            Image image = new Image(new java.io.ByteArrayInputStream(bytes));
            return image.isError() ? null : image;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @FXML
    private void handleProfile() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("My Profile");
        dialog.setHeaderText("Your profile");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        // Editable state for this dialog session. avatarBase64[0] == null
        // means "unchanged"; removeAvatar[0] wins over everything.
        final String[] newAvatarBase64 = {null};
        final boolean[] removeAvatar = {false};

        ImageView avatarPreview = new ImageView();
        avatarPreview.setFitWidth(72);
        avatarPreview.setFitHeight(72);
        avatarPreview.setPreserveRatio(false);
        Runnable refreshPreview = () -> {
            Image img = null;
            if (!removeAvatar[0]) {
                img = decodeAvatarImage(newAvatarBase64[0] != null
                        ? newAvatarBase64[0]
                        : (currentUserProfile != null ? currentUserProfile.getAvatarBase64() : null));
            }
            if (img != null) {
                avatarPreview.setImage(img);
                avatarPreview.setClip(new javafx.scene.shape.Circle(36, 36, 36));
            } else {
                avatarPreview.setClip(null);
                try {
                    avatarPreview.setImage(new Image(Objects.requireNonNull(
                            getClass().getResourceAsStream("/images/user-icon.png"))));
                } catch (Exception ignored) {
                }
            }
        };
        refreshPreview.run();

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(360);

        Button chooseImageBtn = new Button("Choose Image…");
        chooseImageBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose a profile image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File file = chooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file == null) {
                return;
            }
            try {
                newAvatarBase64[0] = encodeAvatarFile(file);
                removeAvatar[0] = false;
                statusLabel.setText("");
                refreshPreview.run();
            } catch (IOException ex) {
                statusLabel.setText("Couldn't use that image: " + ex.getMessage());
            }
        });

        Button removeImageBtn = new Button("Remove");
        removeImageBtn.setOnAction(e -> {
            removeAvatar[0] = true;
            newAvatarBase64[0] = null;
            refreshPreview.run();
        });

        TextField fullNameField = new TextField(
                currentUserProfile != null && currentUserProfile.getFullName() != null
                        ? currentUserProfile.getFullName() : "");
        fullNameField.setPromptText("Your full name");
        fullNameField.setPrefWidth(240);

        String username = currentUserProfile != null
                ? currentUserProfile.getUsername() : TokenManager.getUsername();
        String email = currentUserProfile != null ? currentUserProfile.getEmail() : null;

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Full name"), 0, 0);
        grid.add(fullNameField, 1, 0);
        grid.add(new Label("Username"), 0, 1);
        grid.add(new Label(username != null ? username : "—"), 1, 1);
        grid.add(new Label("Email"), 0, 2);
        grid.add(new Label(email != null ? email : "—"), 1, 2);

        HBox avatarButtons = new HBox(8, chooseImageBtn, removeImageBtn);
        avatarButtons.setAlignment(Pos.CENTER_LEFT);
        VBox avatarBox = new VBox(8, avatarPreview, avatarButtons);
        avatarBox.setAlignment(Pos.TOP_CENTER);

        HBox mainRow = new HBox(20, avatarBox, grid);
        VBox content = new VBox(12, mainRow, statusLabel);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        initAndStyleDialog(dialog);

        // Save asynchronously and only close on success — consuming the
        // event keeps the dialog open so an error is actually visible.
        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            saveBtn.setDisable(true);
            statusLabel.setText("Saving…");
            UpdateProfileRequest request = new UpdateProfileRequest(
                    fullNameField.getText(), newAvatarBase64[0], removeAvatar[0]);
            new Thread(() -> {
                try {
                    Optional<UserProfile> updated = UserService.updateMe(request);
                    Platform.runLater(() -> {
                        updated.ifPresent(this::applyProfileToHeader);
                        updateStatus("Profile updated");
                        dialog.close();
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Couldn't save: " + ex.getMessage());
                        saveBtn.setDisable(false);
                    });
                }
            }).start();
        });

        dialog.showAndWait();
    }

    /** Reads an image file, scales it down to at most 256px on its longest
     * side, and returns it base64-encoded as PNG — keeps the payload tiny
     * no matter what the person picked (a raw photo would otherwise be
     * megabytes of base64). */
    private String encodeAvatarFile(File file) throws IOException {
        java.awt.image.BufferedImage source = javax.imageio.ImageIO.read(file);
        if (source == null) {
            throw new IOException("that file doesn't look like an image");
        }
        int max = 256;
        double scale = Math.min(1.0, (double) max / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        java.awt.image.BufferedImage scaled =
                new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(scaled, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    // ============================================================
    // Presence (online teammates / collaborators)
    // ============================================================

    /** Starts the heartbeat (marks THIS user online) and refreshes the
     * "Online now" bubbles every 30 seconds. */
    private void startPresenceUpdates() {
        PresenceService.startHeartbeat();
        refreshOnlineUsers();
        if (presenceRefreshTimeline != null) {
            presenceRefreshTimeline.stop();
        }
        presenceRefreshTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(30), e -> refreshOnlineUsers()));
        presenceRefreshTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        presenceRefreshTimeline.play();
    }

    private void stopPresenceUpdates() {
        PresenceService.stopHeartbeat();
        if (presenceRefreshTimeline != null) {
            presenceRefreshTimeline.stop();
            presenceRefreshTimeline = null;
        }
    }

    private void refreshOnlineUsers() {
        new Thread(() -> {
            List<OnlineUser> online = PresenceService.getOnlineUsers();
            Platform.runLater(() -> {
                onlineUserIds.clear();
                online.forEach(u -> onlineUserIds.add(u.getUserId()));
                renderOnlineUsers(online);
            });
        }).start();
    }

    private void renderOnlineUsers(List<OnlineUser> online) {
        if (onlineUsersBox == null) {
            return;
        }
        onlineUsersBox.getChildren().clear();
        if (online.isEmpty()) {
            Label none = new Label("No one else is online");
            none.getStyleClass().add("online-empty-label");
            onlineUsersBox.getChildren().add(none);
            return;
        }
        for (OnlineUser user : online) {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4);
            dot.getStyleClass().add("online-dot");
            Label name = new Label(user.displayName());
            name.getStyleClass().add("online-user-name");
            HBox chip = new HBox(5, dot, name);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("online-user-chip");
            HoverTooltip.attach(chip, user.getUsername() + " is online now");
            onlineUsersBox.getChildren().add(chip);
        }
    }

    @FXML
    private void handleSettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        Window owner = (urlField != null && urlField.getScene() != null)
                ? urlField.getScene().getWindow() : null;
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Settings");
        dialog.setResizable(false);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("settings-dialog-pane");
        dialogPane.getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL, ButtonType.OK);

        // Sidebar Navigation
        VBox navigation = new VBox(4);
        navigation.setPrefWidth(210);
        navigation.setMinWidth(210);
        navigation.setMaxWidth(210);
        navigation.getStyleClass().add("settings-nav-sidebar");

        Label navHeader = new Label("PREFERENCES");
        navHeader.getStyleClass().add("settings-nav-header");
        navigation.getChildren().add(navHeader);

        Button appearanceNav = new Button("🎨  Appearance");
        appearanceNav.getStyleClass().add("settings-nav-item");
        appearanceNav.setMaxWidth(Double.MAX_VALUE);

        Button updatesNav = new Button("🚀  Updates");
        updatesNav.getStyleClass().add("settings-nav-item");
        updatesNav.setMaxWidth(Double.MAX_VALUE);

        navigation.getChildren().addAll(appearanceNav, updatesNav);

        Button mailNav = null;
        if (TokenManager.isAdmin()) {
            mailNav = new Button();
            mailNav.getStyleClass().add("settings-nav-item");
            mailNav.setMaxWidth(Double.MAX_VALUE);

            HBox mailNavBox = new HBox(8);
            mailNavBox.setAlignment(Pos.CENTER_LEFT);
            Label mailTitle = new Label("✉  Mail Server");
            Region mailSpacer = new Region();
            HBox.setHgrow(mailSpacer, Priority.ALWAYS);
            Label adminBadge = new Label("ADMIN");
            adminBadge.getStyleClass().add("settings-admin-badge");
            mailNavBox.getChildren().addAll(mailTitle, mailSpacer, adminBadge);
            mailNav.setGraphic(mailNavBox);
            mailNav.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            navigation.getChildren().add(mailNav);
        }

        // Right-side page container inside ScrollPane
        VBox pageContainer = new VBox(16);
        pageContainer.getStyleClass().add("settings-page-container");
        pageContainer.setFillWidth(true);
        pageContainer.setPrefWidth(570);
        pageContainer.setMinWidth(550);

        ScrollPane scrollPane = new ScrollPane(pageContainer);
        scrollPane.getStyleClass().add("settings-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefWidth(590);
        scrollPane.setMinWidth(570);

        // ============================================================
        // 1. Appearance Page
        // ============================================================
        final boolean[] selectedLight = new boolean[]{ThemeManager.isLight()};

        Label appTitle = new Label("Appearance");
        appTitle.getStyleClass().add("settings-page-title");
        Label appSub = new Label("Customize the interface theme and visual presentation of Thundercall.");
        appSub.getStyleClass().add("settings-page-subtitle");
        VBox appHeader = new VBox(4, appTitle, appSub);

        VBox themeCard = new VBox(12);
        themeCard.getStyleClass().add("settings-card");
        Label themeCardTitle = new Label("Interface Theme");
        themeCardTitle.getStyleClass().add("settings-card-title");
        Label themeCardDesc = new Label("Select your preferred appearance. Thundercall adapts contrast and palettes instantly.");
        themeCardDesc.getStyleClass().add("settings-card-desc");

        HBox themeCardsBox = new HBox(16);

        // Dark Theme Card
        VBox darkCard = new VBox(10);
        darkCard.getStyleClass().add("settings-theme-card");
        HBox.setHgrow(darkCard, Priority.ALWAYS);
        StackPane darkMockup = createThemeMockup(false);
        HBox darkTitleRow = new HBox(8);
        darkTitleRow.setAlignment(Pos.CENTER_LEFT);
        Label darkTitle = new Label("🌙  Dark Theme");
        darkTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");
        Region darkSp = new Region();
        HBox.setHgrow(darkSp, Priority.ALWAYS);
        Label darkBadge = new Label("✓ Active");
        darkBadge.getStyleClass().add("settings-status-badge-ok");
        darkTitleRow.getChildren().addAll(darkTitle, darkSp, darkBadge);
        Label darkDesc = new Label("Refined slate and charcoal canvas engineered for focus, code readability, and low eye fatigue.");
        darkDesc.getStyleClass().add("settings-card-desc");
        darkDesc.setWrapText(true);
        darkCard.getChildren().addAll(darkMockup, darkTitleRow, darkDesc);

        // Light Theme Card
        VBox lightCard = new VBox(10);
        lightCard.getStyleClass().add("settings-theme-card");
        HBox.setHgrow(lightCard, Priority.ALWAYS);
        StackPane lightMockup = createThemeMockup(true);
        HBox lightTitleRow = new HBox(8);
        lightTitleRow.setAlignment(Pos.CENTER_LEFT);
        Label lightTitle = new Label("☀️  Light Theme");
        lightTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");
        Region lightSp = new Region();
        HBox.setHgrow(lightSp, Priority.ALWAYS);
        Label lightBadge = new Label("✓ Active");
        lightBadge.getStyleClass().add("settings-status-badge-ok");
        lightTitleRow.getChildren().addAll(lightTitle, lightSp, lightBadge);
        Label lightDesc = new Label("Clean, bright surfaces offering crisp contrast optimized for daylight and high-ambient lighting.");
        lightDesc.getStyleClass().add("settings-card-desc");
        lightDesc.setWrapText(true);
        lightCard.getChildren().addAll(lightMockup, lightTitleRow, lightDesc);

        themeCardsBox.getChildren().addAll(darkCard, lightCard);

        Runnable refreshThemeSelection = () -> {
            if (selectedLight[0]) {
                if (!lightCard.getStyleClass().contains("selected")) lightCard.getStyleClass().add("selected");
                darkCard.getStyleClass().remove("selected");
                lightBadge.setVisible(true);
                darkBadge.setVisible(false);
            } else {
                if (!darkCard.getStyleClass().contains("selected")) darkCard.getStyleClass().add("selected");
                lightCard.getStyleClass().remove("selected");
                darkBadge.setVisible(true);
                lightBadge.setVisible(false);
            }
        };

        darkCard.setOnMouseClicked(e -> {
            selectedLight[0] = false;
            refreshThemeSelection.run();
        });
        lightCard.setOnMouseClicked(e -> {
            selectedLight[0] = true;
            refreshThemeSelection.run();
        });
        refreshThemeSelection.run();

        themeCard.getChildren().addAll(themeCardTitle, themeCardDesc, themeCardsBox);

        // Brand Info Card
        VBox brandCard = new VBox(10);
        brandCard.getStyleClass().add("settings-card");
        Label brandTitle = new Label("Design System & Typography");
        brandTitle.getStyleClass().add("settings-card-title");
        Label brandDesc = new Label("Thundercall combines Postman-grade workflow ergonomics with modern reactive design tokens.");
        brandDesc.getStyleClass().add("settings-card-desc");

        HBox tokensRow = new HBox(12);
        tokensRow.setAlignment(Pos.CENTER_LEFT);
        Label accentPill = new Label("● Thundercall Orange (#ff6c37)");
        accentPill.setStyle("-fx-background-color: rgba(255, 108, 55, 0.15); -fx-text-fill: #ff6c37; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10 4 10; -fx-background-radius: 6;");
        Label fontPill = new Label("JetBrains Mono Code Rendering");
        fontPill.getStyleClass().add("settings-status-badge-muted");
        Label uiFontPill = new Label("Inter / System UI");
        uiFontPill.getStyleClass().add("settings-status-badge-muted");
        tokensRow.getChildren().addAll(accentPill, fontPill, uiFontPill);

        brandCard.getChildren().addAll(brandTitle, brandDesc, tokensRow);

        List<Node> appearancePageNodes = List.of(appHeader, themeCard, brandCard);

        // ============================================================
        // 2. Updates Page
        // ============================================================
        Label updTitle = new Label("Updates");
        updTitle.getStyleClass().add("settings-page-title");
        Label updSub = new Label("Manage application updates, release channels, and distribution endpoints.");
        updSub.getStyleClass().add("settings-page-subtitle");
        VBox updHeader = new VBox(4, updTitle, updSub);

        // Current Version Card
        VBox versionCard = new VBox(12);
        versionCard.getStyleClass().add("settings-card");

        HBox versionRow = new HBox(12);
        versionRow.setAlignment(Pos.CENTER_LEFT);

        Label rocketIcon = new Label("🚀");
        rocketIcon.setStyle("-fx-font-size: 24px; -fx-background-color: -accent-soft; -fx-background-radius: 8; -fx-padding: 6 10 6 10;");

        VBox versionInfoBox = new VBox(4);
        HBox versionBadges = new HBox(8);
        versionBadges.setAlignment(Pos.CENTER_LEFT);
        Label appVer = new Label("Thundercall v" + AppUpdateService.CURRENT_VERSION);
        appVer.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        Label okBadge = new Label("Up to date");
        okBadge.getStyleClass().add("settings-status-badge-ok");
        Label channelBadge = new Label(AppUpdateService.getChannel().toUpperCase() + " CHANNEL");
        channelBadge.getStyleClass().add("settings-status-badge-muted");
        versionBadges.getChildren().addAll(appVer, okBadge, channelBadge);

        Label integrityLabel = new Label("Packages are cryptographically verified with SHA-256 before installation.");
        integrityLabel.getStyleClass().add("settings-card-desc");
        versionInfoBox.getChildren().addAll(versionBadges, integrityLabel);

        Region vSpacer = new Region();
        HBox.setHgrow(vSpacer, Priority.ALWAYS);

        Button checkBtn = new Button("Check for Updates...");
        checkBtn.getStyleClass().add("custom-button");
        checkBtn.setOnAction(e -> {
            Window w = dialog.getDialogPane().getScene() != null
                    ? dialog.getDialogPane().getScene().getWindow() : null;
            if (w != null) {
                AppUpdateDialog.check(w, true);
            }
        });

        versionRow.getChildren().addAll(rocketIcon, versionInfoBox, vSpacer, checkBtn);
        versionCard.getChildren().add(versionRow);

        // Auto Update Card
        VBox autoUpdateCard = new VBox(8);
        autoUpdateCard.getStyleClass().add("settings-card");
        CheckBox autoUpdate = new CheckBox("Automatically check for application updates on startup");
        autoUpdate.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        autoUpdate.setSelected(AppUpdateService.isAutoCheckEnabled());
        Label autoUpdateDesc = new Label("Recommended. Thundercall silently checks for bug fixes and security patches on launch.");
        autoUpdateDesc.getStyleClass().add("settings-card-desc");
        autoUpdateCard.getChildren().addAll(autoUpdate, autoUpdateDesc);

        // Channels & Endpoints Card
        VBox endpointsCard = new VBox(12);
        endpointsCard.getStyleClass().add("settings-card");
        Label endpointsTitle = new Label("Distribution Channel & Update Mirrors");
        endpointsTitle.getStyleClass().add("settings-card-title");
        Label endpointsDesc = new Label("Thundercall supports dual-source update distribution with automatic fallback.");
        endpointsDesc.getStyleClass().add("settings-card-desc");

        ComboBox<String> channel = new ComboBox<>();
        channel.getItems().addAll("Stable", "Beta", "Nightly");
        channel.setValue(AppUpdateService.getChannel());
        channel.setMaxWidth(Double.MAX_VALUE);
        channel.setOnAction(e -> {
            if (channel.getValue() != null) {
                channelBadge.setText(channel.getValue().toUpperCase() + " CHANNEL");
            }
        });

        TextField endpoint = new TextField(AppUpdateService.getEndpoint());
        endpoint.setPromptText(AppUpdateService.DEFAULT_ENDPOINT);

        TextField githubRepo = new TextField(AppUpdateService.getGitHubRepo());
        githubRepo.setPromptText(AppUpdateService.DEFAULT_GITHUB_REPO);

        Label channelLbl = new Label("Release Channel");
        channelLbl.getStyleClass().add("settings-input-label");
        Label endpLbl = new Label("RozeHub Update Endpoint (Priority 1)");
        endpLbl.getStyleClass().add("settings-input-label");
        Label gitLbl = new Label("GitHub Repository Mirror (Priority 2 Fallback)");
        gitLbl.getStyleClass().add("settings-input-label");

        HBox mirrorNotice = new HBox(8);
        mirrorNotice.setAlignment(Pos.CENTER_LEFT);
        Label mirrorIcon = new Label("ℹ");
        mirrorIcon.setStyle("-fx-font-weight: bold; -fx-text-fill: -info;");
        Label mirrorText = new Label("Updates are checked first against RozeHub website API. If unreachable, Thundercall falls back automatically to GitHub Releases.");
        mirrorText.getStyleClass().add("settings-card-desc");
        mirrorText.setWrapText(true);
        mirrorNotice.getChildren().addAll(mirrorIcon, mirrorText);

        endpointsCard.getChildren().addAll(endpointsTitle, endpointsDesc,
                channelLbl, channel,
                endpLbl, endpoint,
                gitLbl, githubRepo,
                mirrorNotice);

        List<Node> updatesPageNodes = List.of(updHeader, versionCard, autoUpdateCard, endpointsCard);

        // ============================================================
        // 3. Mail (SMTP) Page (Inside Settings!)
        // ============================================================
        List<Node> mailPageNodes = new ArrayList<>();
        final Runnable[] saveMailSettingsRef = new Runnable[1];

        final Button finalMailNav = mailNav;

        if (TokenManager.isAdmin()) {
            Label mailPageTitle = new Label("Mail Server");
            mailPageTitle.getStyleClass().add("settings-page-title");
            Label mailPageSub = new Label("Configure outgoing SMTP server for team invitations and email OTP verification codes.");
            mailPageSub.getStyleClass().add("settings-page-subtitle");
            VBox mailHeader = new VBox(4, mailPageTitle, mailPageSub);

            // Master switch card
            VBox masterCard = new VBox(8);
            masterCard.getStyleClass().add("settings-card");
            HBox masterRow = new HBox(12);
            masterRow.setAlignment(Pos.CENTER_LEFT);

            CheckBox enabledCheck = new CheckBox("Outgoing Mail Service Enabled");
            enabledCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            enabledCheck.setSelected(true);

            Region mSpacer = new Region();
            HBox.setHgrow(mSpacer, Priority.ALWAYS);

            Label enabledPill = new Label("ACTIVE");
            enabledPill.getStyleClass().add("settings-status-badge-ok");

            masterRow.getChildren().addAll(enabledCheck, mSpacer, enabledPill);

            Label masterDesc = new Label("Master switch for all outbound emails. When disabled, workspace invitations and password verification emails will be queued or skipped.");
            masterDesc.getStyleClass().add("settings-card-desc");
            masterDesc.setWrapText(true);
            masterCard.getChildren().addAll(masterRow, masterDesc);

            enabledCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    enabledPill.setText("ACTIVE");
                    enabledPill.getStyleClass().setAll("settings-status-badge-ok");
                } else {
                    enabledPill.setText("DISABLED");
                    enabledPill.getStyleClass().setAll("settings-status-badge-muted");
                }
            });

            // Quick Presets
            HBox presetsBox = new HBox(8);
            presetsBox.setAlignment(Pos.CENTER_LEFT);
            Label presetsTitle = new Label("Quick Presets:");
            presetsTitle.getStyleClass().add("settings-input-label");

            Button gmailPreset = new Button("Gmail");
            gmailPreset.getStyleClass().add("custom-button");

            Button outlookPreset = new Button("Outlook / Office 365");
            outlookPreset.getStyleClass().add("custom-button");

            Button sendgridPreset = new Button("SendGrid");
            sendgridPreset.getStyleClass().add("custom-button");

            presetsBox.getChildren().addAll(presetsTitle, gmailPreset, outlookPreset, sendgridPreset);

            // Server Config Card
            VBox smtpCard = new VBox(12);
            smtpCard.getStyleClass().add("settings-card");
            Label smtpCardTitle = new Label("SMTP Credentials & Encryption");
            smtpCardTitle.getStyleClass().add("settings-card-title");

            TextField hostField = new TextField();
            hostField.setPromptText("smtp.gmail.com");
            TextField portField = new TextField();
            portField.setPromptText("587");
            portField.setPrefWidth(90);
            portField.setMinWidth(90);

            HBox hostPortRow = new HBox(12);
            VBox hostBox = new VBox(4);
            HBox.setHgrow(hostBox, Priority.ALWAYS);
            Label hostLbl = new Label("SMTP Host");
            hostLbl.getStyleClass().add("settings-input-label");
            hostBox.getChildren().addAll(hostLbl, hostField);

            VBox portBox = new VBox(4);
            Label portLbl = new Label("Port");
            portLbl.getStyleClass().add("settings-input-label");
            portBox.getChildren().addAll(portLbl, portField);
            hostPortRow.getChildren().addAll(hostBox, portBox);

            TextField usernameField = new TextField();
            usernameField.setPromptText("you@domain.com");

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("(leave blank to keep current password)");
            TextField passwordVisibleField = new TextField();
            passwordVisibleField.setPromptText("(leave blank to keep current password)");
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

            Button eyeToggle = new Button("👁");
            eyeToggle.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0 8 0 8; -fx-font-size: 13px;");
            StackPane.setAlignment(eyeToggle, Pos.CENTER_RIGHT);
            StackPane.setMargin(eyeToggle, new Insets(0, 4, 0, 0));

            eyeToggle.setOnAction(e -> {
                boolean show = !passwordVisibleField.isVisible();
                passwordVisibleField.setVisible(show);
                passwordVisibleField.setManaged(show);
                passwordField.setVisible(!show);
                passwordField.setManaged(!show);
                eyeToggle.setStyle(show
                        ? "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0 8 0 8; -fx-font-size: 13px; -fx-text-fill: -accent;"
                        : "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0 8 0 8; -fx-font-size: 13px;");
            });

            StackPane passContainer = new StackPane(passwordField, passwordVisibleField, eyeToggle);

            HBox userPassRow = new HBox(12);
            VBox userBox = new VBox(4);
            HBox.setHgrow(userBox, Priority.ALWAYS);
            Label userLbl = new Label("Username / Email");
            userLbl.getStyleClass().add("settings-input-label");
            userBox.getChildren().addAll(userLbl, usernameField);

            VBox passBox = new VBox(4);
            HBox.setHgrow(passBox, Priority.ALWAYS);
            Label passLbl = new Label("SMTP Password");
            passLbl.getStyleClass().add("settings-input-label");
            passBox.getChildren().addAll(passLbl, passContainer);
            userPassRow.getChildren().addAll(userBox, passBox);

            TextField fromAddressField = new TextField();
            fromAddressField.setPromptText("noreply@thundercall.io");
            TextField fromNameField = new TextField();
            fromNameField.setPromptText("Thundercall");

            HBox fromRow = new HBox(12);
            VBox fromAddrBox = new VBox(4);
            HBox.setHgrow(fromAddrBox, Priority.ALWAYS);
            Label fromAddrLbl = new Label("Sender Email (From)");
            fromAddrLbl.getStyleClass().add("settings-input-label");
            fromAddrBox.getChildren().addAll(fromAddrLbl, fromAddressField);

            VBox fromNameBox = new VBox(4);
            HBox.setHgrow(fromNameBox, Priority.ALWAYS);
            Label fromNameLbl = new Label("Sender Display Name");
            fromNameLbl.getStyleClass().add("settings-input-label");
            fromNameBox.getChildren().addAll(fromNameLbl, fromNameField);
            fromRow.getChildren().addAll(fromAddrBox, fromNameBox);

            CheckBox tlsCheck = new CheckBox("Use STARTTLS (port 587)");
            tlsCheck.setSelected(true);
            CheckBox sslCheck = new CheckBox("Use SSL/TLS (port 465)");
            HBox securityRow = new HBox(20, tlsCheck, sslCheck);

            // Preset handlers
            gmailPreset.setOnAction(e -> {
                hostField.setText("smtp.gmail.com");
                portField.setText("587");
                tlsCheck.setSelected(true);
                sslCheck.setSelected(false);
            });
            outlookPreset.setOnAction(e -> {
                hostField.setText("smtp.office365.com");
                portField.setText("587");
                tlsCheck.setSelected(true);
                sslCheck.setSelected(false);
            });
            sendgridPreset.setOnAction(e -> {
                hostField.setText("smtp.sendgrid.net");
                portField.setText("587");
                tlsCheck.setSelected(true);
                sslCheck.setSelected(false);
            });

            smtpCard.getChildren().addAll(smtpCardTitle, presetsBox, hostPortRow, userPassRow, fromRow, securityRow);

            // Diagnostic & Live Test Card
            VBox testCard = new VBox(10);
            testCard.getStyleClass().add("settings-card");
            Label testCardTitle = new Label("Diagnostic SMTP Test");
            testCardTitle.getStyleClass().add("settings-card-title");
            Label testCardDesc = new Label("Dispatch a real-time verification test to confirm server connectivity and authentication.");
            testCardDesc.getStyleClass().add("settings-card-desc");

            TextField testEmailField = new TextField();
            testEmailField.setPromptText("Enter recipient email address...");
            HBox.setHgrow(testEmailField, Priority.ALWAYS);

            Button sendTestBtn = new Button("Send Test Email");
            sendTestBtn.getStyleClass().add("custom-button");

            Button saveMailBtn = new Button("Save Configuration");
            saveMailBtn.getStyleClass().add("primary-button");

            HBox testRow = new HBox(10, testEmailField, sendTestBtn, saveMailBtn);
            testRow.setAlignment(Pos.CENTER_LEFT);

            Label mailStatusLabel = new Label();
            mailStatusLabel.setWrapText(true);
            mailStatusLabel.setMaxWidth(Double.MAX_VALUE);

            testCard.getChildren().addAll(testCardTitle, testCardDesc, testRow, mailStatusLabel);

            mailPageNodes.addAll(List.of(mailHeader, masterCard, smtpCard, testCard));

            // Load existing mail settings
            new Thread(() -> {
                try {
                    Optional<MailSettingsResponse> current = MailSettingsService.getSettings();
                    Platform.runLater(() -> current.ifPresent(settings -> {
                        if (settings.getSmtpHost() != null) hostField.setText(settings.getSmtpHost());
                        if (settings.getSmtpPort() != null) portField.setText(String.valueOf(settings.getSmtpPort()));
                        if (settings.getSmtpUsername() != null) usernameField.setText(settings.getSmtpUsername());
                        if (settings.getFromAddress() != null) fromAddressField.setText(settings.getFromAddress());
                        if (settings.getFromName() != null) fromNameField.setText(settings.getFromName());
                        tlsCheck.setSelected(settings.isUseTls());
                        sslCheck.setSelected(settings.isUseSsl());
                        enabledCheck.setSelected(settings.isEnabled());
                        if (settings.isHasPassword()) {
                            passwordField.setPromptText("(password is saved on server — leave blank to keep)");
                            passwordVisibleField.setPromptText("(password is saved on server — leave blank to keep)");
                        }
                    }));
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        mailStatusLabel.setText("Couldn't fetch current SMTP settings: " + ex.getMessage());
                        mailStatusLabel.setStyle("-fx-text-fill: -warning;");
                    });
                }
            }).start();

            // Save logic
            saveMailSettingsRef[0] = () -> {
                String host = hostField.getText().trim();
                String portStr = portField.getText().trim();
                if (host.isEmpty() && portStr.isEmpty()) {
                    return;
                }
                int port = 587;
                if (!portStr.isEmpty()) {
                    try {
                        port = Integer.parseInt(portStr);
                    } catch (NumberFormatException nfe) {
                        mailStatusLabel.setText("Port must be a valid integer number (e.g. 587 or 465).");
                        mailStatusLabel.setStyle("-fx-text-fill: -danger;");
                        return;
                    }
                }
                MailSettingsRequest request = new MailSettingsRequest(
                        host, port, usernameField.getText().trim(),
                        passwordField.getText(),
                        fromAddressField.getText().trim(),
                        fromNameField.getText().trim(),
                        tlsCheck.isSelected(), sslCheck.isSelected(), enabledCheck.isSelected());

                mailStatusLabel.setText("Saving mail configuration...");
                mailStatusLabel.setStyle("-fx-text-fill: -text-secondary;");

                new Thread(() -> {
                    try {
                        MailSettingsService.updateSettings(request);
                        Platform.runLater(() -> {
                            mailStatusLabel.setText("✓ Mail server settings saved successfully.");
                            mailStatusLabel.setStyle("-fx-text-fill: -success; -fx-font-weight: bold;");
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            mailStatusLabel.setText("Failed to save mail settings: " + ex.getMessage());
                            mailStatusLabel.setStyle("-fx-text-fill: -danger;");
                        });
                    }
                }).start();
            };

            saveMailBtn.setOnAction(e -> {
                if (saveMailSettingsRef[0] != null) saveMailSettingsRef[0].run();
            });

            // Test email logic
            sendTestBtn.setOnAction(e -> {
                String to = testEmailField.getText().trim();
                if (to.isEmpty()) {
                    mailStatusLabel.setText("Please enter an email address to send the diagnostic test.");
                    mailStatusLabel.setStyle("-fx-text-fill: -danger;");
                    return;
                }
                sendTestBtn.setDisable(true);
                mailStatusLabel.setText("Connecting to SMTP server and dispatching test email...");
                mailStatusLabel.setStyle("-fx-text-fill: -text-secondary;");

                new Thread(() -> {
                    try {
                        MailSettingsService.sendTestEmail(to);
                        Platform.runLater(() -> {
                            sendTestBtn.setDisable(false);
                            mailStatusLabel.setText("✓ Diagnostic test email sent to " + to + " — check your inbox.");
                            mailStatusLabel.setStyle("-fx-text-fill: -success; -fx-font-weight: bold;");
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            sendTestBtn.setDisable(false);
                            mailStatusLabel.setText("Test delivery failed: " + ex.getMessage());
                            mailStatusLabel.setStyle("-fx-text-fill: -danger;");
                        });
                    }
                }).start();
            });
        }

        // ============================================================
        // Navigation Switching (In-Page Swap!)
        // ============================================================
        appearanceNav.setOnAction(e -> {
            pageContainer.getChildren().setAll(appearancePageNodes);
            selectSettingsNav(appearanceNav, appearanceNav, updatesNav, finalMailNav);
        });

        updatesNav.setOnAction(e -> {
            pageContainer.getChildren().setAll(updatesPageNodes);
            selectSettingsNav(updatesNav, appearanceNav, updatesNav, finalMailNav);
        });

        if (mailNav != null) {
            mailNav.setOnAction(e -> {
                pageContainer.getChildren().setAll(mailPageNodes);
                selectSettingsNav(finalMailNav, appearanceNav, updatesNav, finalMailNav);
            });
        }

        // Populate appearance by default before root layout
        pageContainer.getChildren().setAll(appearancePageNodes);
        selectSettingsNav(appearanceNav, appearanceNav, updatesNav, finalMailNav);

        BorderPane root = new BorderPane();
        root.setLeft(navigation);
        root.setCenter(scrollPane);
        root.setPrefSize(800, 540);
        root.setMinSize(780, 520);

        dialogPane.setContent(root);
        dialogPane.setPrefSize(800, 580);
        dialogPane.setMinSize(780, 540);
        ThemeManager.styleDialog(dialogPane);

        // Enforce stage dimensions and center on screen
        dialog.setOnShown(e -> {
            Window window = dialogPane.getScene() != null ? dialogPane.getScene().getWindow() : null;
            if (window instanceof Stage stage) {
                stage.setMinWidth(780);
                stage.setMinHeight(540);
                stage.setWidth(800);
                stage.setHeight(580);
                stage.centerOnScreen();
            }
        });

        // Dialog buttons
        Button applyButton = (Button) dialogPane.lookupButton(ButtonType.APPLY);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (applyButton != null) applyButton.setText("Apply");
        if (cancelButton != null) cancelButton.setText("Cancel");
        if (okButton != null) okButton.setText("OK");

        Runnable applySettings = () -> {
            if (urlField != null && urlField.getScene() != null) {
                ThemeManager.setLight(selectedLight[0], urlField.getScene());
            }
            AppUpdateService.setAutoCheckEnabled(autoUpdate.isSelected());
            AppUpdateService.setChannel(channel.getValue());
            AppUpdateService.setEndpoint(endpoint.getText());
            AppUpdateService.setGitHubRepo(githubRepo.getText());
            if (saveMailSettingsRef[0] != null) {
                saveMailSettingsRef[0].run();
            }
            updateStatus("Settings saved");
        };

        if (applyButton != null) {
            applyButton.addEventFilter(ActionEvent.ACTION, e -> {
                applySettings.run();
                e.consume(); // keep dialog open on Apply
            });
        }
        if (okButton != null) {
            okButton.addEventFilter(ActionEvent.ACTION, e -> {
                applySettings.run();
            });
        }

        dialog.showAndWait();
    }

    private void selectSettingsNav(Button active, Button appearance, Button updates, Button mail) {
        if (appearance != null) appearance.getStyleClass().remove("active");
        if (updates != null) updates.getStyleClass().remove("active");
        if (mail != null) mail.getStyleClass().remove("active");
        if (active != null && !active.getStyleClass().contains("active")) {
            active.getStyleClass().add("active");
        }
    }

    private StackPane createThemeMockup(boolean isLight) {
        VBox root = new VBox(0);
        root.setPrefHeight(90);
        root.setMinHeight(90);
        root.setMaxHeight(90);
        root.setStyle(isLight
                ? "-fx-background-color: #f8f9fa; -fx-background-radius: 6; -fx-border-color: #dcdcdc; -fx-border-radius: 6;"
                : "-fx-background-color: #1e1e1e; -fx-background-radius: 6; -fx-border-color: #333333; -fx-border-radius: 6;");

        // Top window bar
        HBox topBar = new HBox(5);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(5, 8, 5, 8));
        topBar.setStyle(isLight
                ? "-fx-background-color: #edf0f2; -fx-background-radius: 6 6 0 0;"
                : "-fx-background-color: #141414; -fx-background-radius: 6 6 0 0;");

        Region dot1 = new Region();
        dot1.setMinSize(7, 7);
        dot1.setMaxSize(7, 7);
        dot1.setStyle("-fx-background-color: #ff5f56; -fx-background-radius: 50%;");

        Region dot2 = new Region();
        dot2.setMinSize(7, 7);
        dot2.setMaxSize(7, 7);
        dot2.setStyle("-fx-background-color: #ffbd2e; -fx-background-radius: 50%;");

        Region dot3 = new Region();
        dot3.setMinSize(7, 7);
        dot3.setMaxSize(7, 7);
        dot3.setStyle("-fx-background-color: #27c93f; -fx-background-radius: 50%;");

        Region topBarTitle = new Region();
        topBarTitle.setPrefSize(40, 5);
        topBarTitle.setStyle(isLight
                ? "-fx-background-color: #cbd5e1; -fx-background-radius: 3;"
                : "-fx-background-color: #383838; -fx-background-radius: 3;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(dot1, dot2, dot3, spacer, topBarTitle);

        // Body area: sidebar + editor
        HBox body = new HBox(0);
        VBox.setVgrow(body, Priority.ALWAYS);

        // Mini sidebar
        VBox miniSidebar = new VBox(4);
        miniSidebar.setPrefWidth(45);
        miniSidebar.setMinWidth(45);
        miniSidebar.setPadding(new Insets(6, 4, 6, 4));
        miniSidebar.setStyle(isLight
                ? "-fx-background-color: #f1f3f5; -fx-background-radius: 0 0 0 6; -fx-border-color: transparent #e2e8f0 transparent transparent;"
                : "-fx-background-color: #161616; -fx-background-radius: 0 0 0 6; -fx-border-color: transparent #2a2a2a transparent transparent;");

        Region sideActive = new Region();
        sideActive.setPrefSize(35, 6);
        sideActive.setStyle("-fx-background-color: #ff6c37; -fx-background-radius: 2;");

        Region side2 = new Region();
        side2.setPrefSize(28, 5);
        side2.setStyle(isLight ? "-fx-background-color: #cbd5e1; -fx-background-radius: 2;" : "-fx-background-color: #383838; -fx-background-radius: 2;");

        Region side3 = new Region();
        side3.setPrefSize(32, 5);
        side3.setStyle(isLight ? "-fx-background-color: #cbd5e1; -fx-background-radius: 2;" : "-fx-background-color: #383838; -fx-background-radius: 2;");

        miniSidebar.getChildren().addAll(sideActive, side2, side3);

        // Mini editor
        VBox miniEditor = new VBox(6);
        miniEditor.setPadding(new Insets(6, 8, 6, 8));
        VBox.setVgrow(miniEditor, Priority.ALWAYS);
        HBox.setHgrow(miniEditor, Priority.ALWAYS);
        miniEditor.setStyle(isLight
                ? "-fx-background-color: #ffffff; -fx-background-radius: 0 0 6 0;"
                : "-fx-background-color: #1e1e1e; -fx-background-radius: 0 0 6 0;");

        // Mini URL bar
        HBox miniUrlBar = new HBox(4);
        miniUrlBar.setAlignment(Pos.CENTER_LEFT);
        miniUrlBar.setPadding(new Insets(3, 4, 3, 4));
        miniUrlBar.setStyle(isLight
                ? "-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 3; -fx-background-radius: 3;"
                : "-fx-background-color: #262626; -fx-border-color: #333333; -fx-border-radius: 3; -fx-background-radius: 3;");

        Label miniMethod = new Label("GET");
        miniMethod.setStyle(isLight
                ? "-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: #16a34a;"
                : "-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");

        Region miniUrlLine = new Region();
        miniUrlLine.setPrefSize(70, 4);
        miniUrlLine.setStyle(isLight
                ? "-fx-background-color: #94a3b8; -fx-background-radius: 2;"
                : "-fx-background-color: #525252; -fx-background-radius: 2;");

        miniUrlBar.getChildren().addAll(miniMethod, miniUrlLine);

        // Mini code lines
        VBox miniLines = new VBox(3);
        Region line1 = new Region();
        line1.setPrefSize(95, 4);
        line1.setStyle(isLight ? "-fx-background-color: #cbd5e1; -fx-background-radius: 2;" : "-fx-background-color: #404040; -fx-background-radius: 2;");

        Region line2 = new Region();
        line2.setPrefSize(65, 4);
        line2.setStyle(isLight ? "-fx-background-color: #e2e8f0; -fx-background-radius: 2;" : "-fx-background-color: #2e2e2e; -fx-background-radius: 2;");

        miniLines.getChildren().addAll(line1, line2);
        miniEditor.getChildren().addAll(miniUrlBar, miniLines);

        body.getChildren().addAll(miniSidebar, miniEditor);
        root.getChildren().addAll(topBar, body);

        StackPane wrapper = new StackPane(root);
        wrapper.setPadding(new Insets(2));
        return wrapper;
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
        initAndStyleDialog(dialog);

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
        initAndStyleDialog(dialog);

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
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("team-dialog-pane");

        // --- Header with Team Icon, Name, Subtitle, My Role Badge & Danger Button ---
        StackPane headerBadge = new StackPane();
        headerBadge.getStyleClass().add("share-header-badge");
        headerBadge.setMinWidth(38);
        headerBadge.setMaxWidth(38);
        headerBadge.setMinHeight(38);
        headerBadge.setMaxHeight(38);

        javafx.scene.shape.SVGPath teamIcon = new javafx.scene.shape.SVGPath();
        teamIcon.setContent("M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z");
        teamIcon.setStyle("-fx-fill: -accent;");
        teamIcon.setScaleX(1.1);
        teamIcon.setScaleY(1.1);
        headerBadge.getChildren().add(teamIcon);

        Label titleLabel = new Label(team.getName());
        titleLabel.getStyleClass().add("share-title");

        String subtitleText = team.getDescription() != null && !team.getDescription().isBlank()
                ? team.getDescription().trim()
                : "Created by " + team.getOwnerUsername();
        Label subtitleLabel = new Label(subtitleText);
        subtitleLabel.getStyleClass().add("share-subtitle");
        subtitleLabel.setMinWidth(Region.USE_PREF_SIZE);

        VBox titleBox = new VBox(2, titleLabel, subtitleLabel);
        titleBox.setMinWidth(Region.USE_PREF_SIZE);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerSpacer.setMinWidth(12);

        // My role badge on header
        Label myRoleBadge = new Label(team.getMyRole());
        myRoleBadge.getStyleClass().addAll("role-badge",
                "OWNER".equals(team.getMyRole()) ? "role-badge-owner" :
                "ADMIN".equals(team.getMyRole()) ? "role-badge-admin" : "role-badge-member");

        HBox headerRight = new HBox(8, myRoleBadge);
        headerRight.setAlignment(Pos.CENTER_RIGHT);

        if (isOwner) {
            Button deleteBtn = new Button("Delete Team");
            deleteBtn.getStyleClass().add("team-danger-btn");
            deleteBtn.setOnAction(e -> {
                if (!AlertUtils.showConfirmation("Delete Team",
                        "Delete \"" + team.getName() + "\"? This removes all members and cannot be undone.")) {
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
                            AlertUtils.showError("Couldn't delete this team.");
                        }
                    });
                }).start();
            });
            headerRight.getChildren().add(deleteBtn);
        } else {
            Button leaveBtn = new Button("Leave Team");
            leaveBtn.getStyleClass().add("team-danger-btn");
            leaveBtn.setOnAction(e -> {
                if (!AlertUtils.showConfirmation("Leave Team", "Are you sure you want to leave \"" + team.getName() + "\"?")) {
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
                            AlertUtils.showError("Couldn't leave this team.");
                        }
                    });
                }).start();
            });
            headerRight.getChildren().add(leaveBtn);
        }

        HBox headerRow = new HBox(12, headerBadge, titleBox, headerSpacer, headerRight);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // --- Interactive Status Alert Banner ---
        HBox statusBanner = new HBox(8);
        statusBanner.getStyleClass().add("share-status-banner");
        statusBanner.setAlignment(Pos.CENTER_LEFT);
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);

        Label statusIcon = new Label();
        statusIcon.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label statusText = new Label();
        statusText.setWrapText(true);
        statusText.setMaxWidth(560);
        HBox.setHgrow(statusText, Priority.ALWAYS);

        Button dismissBannerBtn = new Button("✕");
        dismissBannerBtn.getStyleClass().add("share-text-btn");
        dismissBannerBtn.setOnAction(e -> {
            statusBanner.setVisible(false);
            statusBanner.setManaged(false);
        });
        statusBanner.getChildren().addAll(statusIcon, statusText, dismissBannerBtn);

        BiConsumer<String, Boolean> showStatus = (msg, isError) -> {
            statusBanner.getStyleClass().removeAll("share-status-success", "share-status-error");
            if (isError) {
                statusBanner.getStyleClass().add("share-status-error");
                statusIcon.setText("⚠");
            } else {
                statusBanner.getStyleClass().add("share-status-success");
                statusIcon.setText("✓");
            }
            statusText.setText(msg);
            statusBanner.setVisible(true);
            statusBanner.setManaged(true);
        };

        // --- Quick Invite Card (Owner / Admin only) ---
        VBox inviteCard = null;
        TextField inviteEmailField = new TextField();
        ComboBox<String> inviteRoleCombo = new ComboBox<>();
        Button inviteBtn = new Button("Send Invite");

        if (isAdminOrOwner) {
            inviteCard = new VBox(10);
            inviteCard.getStyleClass().add("share-card");

            Label inviteCardTitle = new Label("INVITE TEAM MEMBER");
            inviteCardTitle.getStyleClass().add("share-card-header");

            inviteEmailField.setPromptText("Enter colleague's email address...");
            HBox.setHgrow(inviteEmailField, Priority.ALWAYS);

            inviteRoleCombo.getItems().addAll("MEMBER", "ADMIN");
            inviteRoleCombo.setValue("MEMBER");
            inviteRoleCombo.getStyleClass().add("share-role-combo");

            inviteBtn.getStyleClass().addAll("button", "button-primary");
            inviteBtn.setStyle("-fx-font-weight: bold; -fx-padding: 6 16 6 16;");

            HBox inviteRow = new HBox(8, inviteEmailField, inviteRoleCombo, inviteBtn);
            inviteRow.setAlignment(Pos.CENTER_LEFT);
            inviteCard.getChildren().addAll(inviteCardTitle, inviteRow);
        }

        // --- Tabs for Members and Pending Invitations ---
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("share-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab membersTab = new Tab("Members (0)");
        VBox membersContainer = new VBox(8);
        membersContainer.setPadding(new Insets(10, 2, 10, 2));
        ScrollPane membersScroll = new ScrollPane(membersContainer);
        membersScroll.getStyleClass().add("share-scroll-area");
        membersScroll.setFitToWidth(true);
        membersTab.setContent(membersScroll);

        Tab pendingTab = new Tab("Pending Invitations (0)");
        VBox pendingContainer = new VBox(8);
        pendingContainer.setPadding(new Insets(10, 2, 10, 2));
        ScrollPane pendingScroll = new ScrollPane(pendingContainer);
        pendingScroll.getStyleClass().add("share-scroll-area");
        pendingScroll.setFitToWidth(true);
        pendingTab.setContent(pendingScroll);

        tabPane.getTabs().add(membersTab);
        if (isAdminOrOwner) {
            tabPane.getTabs().add(pendingTab);
        }

        Runnable[] refreshInvitations = new Runnable[1];
        Runnable[] refreshMembers = new Runnable[1];

        refreshMembers[0] = () -> new Thread(() -> {
            List<OnlineUser> online = PresenceService.getOnlineUsers();
            Optional<List<TeamMemberResponse>> membersOpt = TeamService.getMembers(team.getId());
            Platform.runLater(() -> {
                onlineUserIds.clear();
                online.forEach(u -> onlineUserIds.add(u.getUserId()));
                renderOnlineUsers(online);

                membersContainer.getChildren().clear();
                List<TeamMemberResponse> members = membersOpt.orElse(List.of());
                membersTab.setText("Members (" + members.size() + ")");

                if (members.isEmpty()) {
                    VBox emptyBox = new VBox(8);
                    emptyBox.getStyleClass().add("share-empty-box");
                    javafx.scene.shape.SVGPath emptyUsersSvg = new javafx.scene.shape.SVGPath();
                    emptyUsersSvg.setContent("M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z");
                    emptyUsersSvg.setStyle("-fx-fill: -text-muted; -fx-opacity: 0.6;");
                    emptyUsersSvg.setScaleX(1.3);
                    emptyUsersSvg.setScaleY(1.3);
                    Label emptyTitle = new Label("No team members found");
                    emptyTitle.getStyleClass().add("share-empty-title");
                    emptyBox.getChildren().addAll(emptyUsersSvg, emptyTitle);
                    membersContainer.getChildren().add(emptyBox);
                } else {
                    for (TeamMemberResponse member : members) {
                        HBox row = new HBox(12);
                        row.getStyleClass().add("share-row");

                        StackPane avatar = createAvatar(member.getUsername());

                        // Online / offline dot
                        boolean isUserOnline = member.getUserId() != null && onlineUserIds.contains(member.getUserId());
                        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4);
                        dot.getStyleClass().add(isUserOnline ? "online-dot" : "offline-dot");
                        HoverTooltip.attach(dot, member.getUsername() + (isUserOnline ? " is online" : " is offline"));

                        Label nameLabel = new Label(member.getUsername());
                        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");

                        HBox nameRow = new HBox(6, dot, nameLabel);
                        nameRow.setAlignment(Pos.CENTER_LEFT);

                        Label emailLabel = new Label(member.getEmail() != null ? member.getEmail() : "No email");
                        emailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

                        VBox userDetails = new VBox(2, nameRow, emailLabel);

                        Region rowSpacer = new Region();
                        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                        boolean memberIsOwner = "OWNER".equals(member.getRole());

                        if (memberIsOwner) {
                            Label ownerBadge = new Label("OWNER");
                            ownerBadge.getStyleClass().addAll("role-badge", "role-badge-owner");
                            row.getChildren().addAll(avatar, userDetails, rowSpacer, ownerBadge);
                        } else if (isAdminOrOwner) {
                            ComboBox<String> roleBox = new ComboBox<>();
                            roleBox.getItems().addAll("ADMIN", "MEMBER");
                            roleBox.setValue(member.getRole());
                            roleBox.getStyleClass().add("share-role-combo");

                            roleBox.valueProperty().addListener((obs, was, isNow) -> {
                                if (isNow != null && !isNow.equals(was)) {
                                    new Thread(() -> {
                                        boolean ok = TeamService.changeMemberRole(team.getId(), member.getUserId(), isNow);
                                        Platform.runLater(() -> {
                                            if (ok) {
                                                showStatus.accept("Updated " + member.getUsername() + "'s role to " + isNow + ".", false);
                                            } else {
                                                showStatus.accept("Failed to update role for " + member.getUsername() + ".", true);
                                                roleBox.setValue(was);
                                            }
                                        });
                                    }).start();
                                }
                            });

                            Button removeMemberBtn = new Button("Remove");
                            removeMemberBtn.getStyleClass().add("share-remove-btn");
                            removeMemberBtn.setOnAction(e -> {
                                if (!AlertUtils.showConfirmation("Remove Member",
                                        "Remove " + member.getUsername() + " from " + team.getName() + "?")) {
                                    return;
                                }
                                removeMemberBtn.setDisable(true);
                                new Thread(() -> {
                                    boolean ok = TeamService.removeMember(team.getId(), member.getUserId());
                                    Platform.runLater(() -> {
                                        if (ok) {
                                            showStatus.accept("Removed " + member.getUsername() + " from the team.", false);
                                            membersContainer.getChildren().remove(row);
                                            membersTab.setText("Members (" + membersContainer.getChildren().size() + ")");
                                        } else {
                                            removeMemberBtn.setDisable(false);
                                            showStatus.accept("Failed to remove " + member.getUsername() + ".", true);
                                        }
                                    });
                                }).start();
                            });

                            row.getChildren().addAll(avatar, userDetails, rowSpacer, roleBox, removeMemberBtn);
                        } else {
                            Label roleBadge = new Label(member.getRole());
                            roleBadge.getStyleClass().addAll("role-badge",
                                    "ADMIN".equals(member.getRole()) ? "role-badge-admin" : "role-badge-member");
                            row.getChildren().addAll(avatar, userDetails, rowSpacer, roleBadge);
                        }

                        membersContainer.getChildren().add(row);
                    }
                }
            });
        }).start();

        if (isAdminOrOwner) {
            refreshInvitations[0] = () -> new Thread(() -> {
                Optional<List<TeamInvitationResponse>> invitationsOpt = TeamService.getPendingInvitations(team.getId());
                Platform.runLater(() -> {
                    pendingContainer.getChildren().clear();
                    List<TeamInvitationResponse> invitations = invitationsOpt.orElse(List.of());
                    pendingTab.setText("Pending Invitations (" + invitations.size() + ")");

                    if (invitations.isEmpty()) {
                        VBox emptyBox = new VBox(8);
                        emptyBox.getStyleClass().add("share-empty-box");
                        javafx.scene.shape.SVGPath emptyMailSvg = new javafx.scene.shape.SVGPath();
                        emptyMailSvg.setContent("M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z");
                        emptyMailSvg.setStyle("-fx-fill: -text-muted; -fx-opacity: 0.6;");
                        emptyMailSvg.setScaleX(1.3);
                        emptyMailSvg.setScaleY(1.3);
                        Label emptyTitle = new Label("No pending invitations");
                        emptyTitle.getStyleClass().add("share-empty-title");
                        Label emptyDesc = new Label("Invitations waiting to be accepted will appear here.");
                        emptyDesc.getStyleClass().add("share-empty-desc");
                        emptyBox.getChildren().addAll(emptyMailSvg, emptyTitle, emptyDesc);
                        pendingContainer.getChildren().add(emptyBox);
                    } else {
                        for (TeamInvitationResponse invite : invitations) {
                            HBox row = new HBox(12);
                            row.getStyleClass().add("share-row");

                            StackPane mailBadge = new StackPane();
                            mailBadge.getStyleClass().add("share-avatar");
                            mailBadge.setStyle("-fx-background-color: #f59e0b;");
                            javafx.scene.shape.SVGPath mailSvg = new javafx.scene.shape.SVGPath();
                            mailSvg.setContent("M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z");
                            mailSvg.setStyle("-fx-fill: white;");
                            mailSvg.setScaleX(0.75);
                            mailSvg.setScaleY(0.75);
                            mailBadge.getChildren().add(mailSvg);

                            Label emailLabel = new Label(invite.getEmail());
                            emailLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");
                            Label pendingSubtext = new Label("Invited as " + invite.getRole() + "  •  " + invite.getStatus());
                            pendingSubtext.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
                            VBox inviteDetails = new VBox(2, emailLabel, pendingSubtext);

                            Region rowSpacer = new Region();
                            HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                            Button revokeInviteBtn = new Button("Revoke Invitation");
                            revokeInviteBtn.getStyleClass().add("share-remove-btn");
                            revokeInviteBtn.setOnAction(e -> {
                                revokeInviteBtn.setDisable(true);
                                new Thread(() -> {
                                    boolean ok = TeamService.revokeInvitation(team.getId(), invite.getId());
                                    Platform.runLater(() -> {
                                        if (ok) {
                                            showStatus.accept("Revoked invitation to " + invite.getEmail() + ".", false);
                                            pendingContainer.getChildren().remove(row);
                                            pendingTab.setText("Pending Invitations (" + pendingContainer.getChildren().size() + ")");
                                            if (pendingContainer.getChildren().isEmpty()) {
                                                refreshInvitations[0].run();
                                            }
                                        } else {
                                            revokeInviteBtn.setDisable(false);
                                            showStatus.accept("Failed to revoke invitation.", true);
                                        }
                                    });
                                }).start();
                            });

                            row.getChildren().addAll(mailBadge, inviteDetails, rowSpacer, revokeInviteBtn);
                            pendingContainer.getChildren().add(row);
                        }
                    }
                });
            }).start();

            inviteBtn.setOnAction(e -> {
                String email = inviteEmailField.getText() == null ? "" : inviteEmailField.getText().trim();
                if (email.isEmpty()) {
                    showStatus.accept("Please enter an email address first.", true);
                    return;
                }
                showStatus.accept("Sending invitation...", false);
                inviteBtn.setDisable(true);
                new Thread(() -> {
                    try {
                        TeamService.inviteMember(team.getId(), email, inviteRoleCombo.getValue());
                        Platform.runLater(() -> {
                            showStatus.accept("Invitation successfully sent to " + email + ".", false);
                            inviteBtn.setDisable(false);
                            inviteEmailField.clear();
                            refreshInvitations[0].run();
                        });
                    } catch (IOException ex) {
                        Platform.runLater(() -> {
                            inviteBtn.setDisable(false);
                            showStatus.accept("Couldn't send invitation: " + TeamService.friendlyMessage(ex), true);
                        });
                    }
                }).start();
            });
        }

        VBox mainContent = new VBox(12);
        mainContent.setPadding(new Insets(16));
        mainContent.getChildren().addAll(headerRow, statusBanner);
        if (inviteCard != null) {
            mainContent.getChildren().add(inviteCard);
        }
        mainContent.getChildren().add(tabPane);

        dialog.getDialogPane().setContent(mainContent);
        initAndStyleDialog(dialog);

        refreshMembers[0].run();
        if (isAdminOrOwner && refreshInvitations[0] != null) {
            refreshInvitations[0].run();
        }

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
        initAndStyleDialog(dialog);

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

        Label baseUrlSectionLabel = new Label("Base URL");
        baseUrlSectionLabel.getStyleClass().add("dialog-section-title");
        Label routesSectionLabel = new Label("Routes");
        routesSectionLabel.getStyleClass().add("dialog-section-title");

        VBox content = new VBox(10,
                baseUrlSectionLabel, urlRow,
                enabledCheck,
                new Separator(),
                routesSectionLabel, routesTable, routeActions,
                new Separator(),
                deleteServerBtn,
                statusLabel);
        content.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(520);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        dialog.getDialogPane().setContent(scrollPane);
        initAndStyleDialog(dialog);
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
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        dialog.getDialogPane().setContent(scrollPane);
        initAndStyleDialog(dialog);

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
        initAndStyleDialog(dialog);

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

        Label runHistoryLabel = new Label("Run History");
        runHistoryLabel.getStyleClass().add("dialog-section-title");
        VBox content = new VBox(10, actions, new Separator(), runHistoryLabel, runsTable, statusLabel);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        initAndStyleDialog(dialog);
        dialog.showAndWait();
    }

    // ============================================================
    // Team Workspaces (shared workspace access)
    // ============================================================

    private void setupTeamWorkspacesList() {
        if (teamWorkspacesList == null) {
            return;
        }
        teamWorkspacesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(WorkspaceAccessResponse access, boolean empty) {
                super.updateItem(access, empty);
                if (empty || access == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                HBox card = new HBox(10);
                card.getStyleClass().add("team-workspace-card");
                card.setAlignment(Pos.CENTER_LEFT);

                StackPane avatar = createAvatar(access.getWorkspaceName());
                avatar.setMinSize(28, 28);
                avatar.setMaxSize(28, 28);

                Label nameLabel = new Label(access.getWorkspaceName());
                nameLabel.getStyleClass().add("team-workspace-card-title");

                String meta = "by " + access.getWorkspaceOwnerUsername();
                if (access.getTeamName() != null && !access.getTeamName().isBlank()) {
                    meta += " · " + access.getTeamName();
                }
                Label metaLabel = new Label(meta);
                metaLabel.getStyleClass().add("team-workspace-card-meta");

                VBox textBox = new VBox(2, nameLabel, metaLabel);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                Label roleBadge = new Label(access.getRole());
                roleBadge.getStyleClass().addAll("role-badge",
                        "EDITOR".equals(access.getRole()) ? "role-badge-owner" : "role-badge-member");

                card.getChildren().addAll(avatar, textBox, roleBadge);
                setGraphic(card);
                setText(null);
            }
        });
        teamWorkspacesList.setOnMouseClicked(e -> {
            WorkspaceAccessResponse selected = teamWorkspacesList.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selected != null) {
                openSharedWorkspaceBrowseDialog(selected);
            }
        });
    }

    private void loadTeamWorkspaces() {
        if (teamWorkspacesList == null) {
            return;
        }
        new Thread(() -> {
            Optional<List<WorkspaceAccessResponse>> shared = WorkspaceSharingService.getSharedWithMe();
            Platform.runLater(() -> shared.ifPresent(list -> teamWorkspacesList.getItems().setAll(list)));
        }).start();
    }

    /** Share your current workspace with a team you own/admin, setting
     * each member's access individually — this doubles as the "manage
     * access" screen since sharing and managing who has access are the
     * same underlying action. */
    @FXML
    private void handleShareWorkspace() {
        if (!WorkspaceManager.hasWorkspace()) {
            AlertUtils.showError("No workspace to share yet.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Share Workspace");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("share-dialog-pane");

        // --- Header with Badge and Workspace Switcher ---
        StackPane headerBadge = new StackPane();
        headerBadge.getStyleClass().add("share-header-badge");
        headerBadge.setMinWidth(38);
        headerBadge.setMaxWidth(38);
        headerBadge.setMinHeight(38);
        headerBadge.setMaxHeight(38);

        javafx.scene.shape.SVGPath usersIcon = new javafx.scene.shape.SVGPath();
        usersIcon.setContent("M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z");
        usersIcon.setStyle("-fx-fill: -accent;");
        usersIcon.setScaleX(1.1);
        usersIcon.setScaleY(1.1);
        headerBadge.getChildren().add(usersIcon);

        Label titleLabel = new Label("Share Workspace");
        titleLabel.getStyleClass().add("share-title");
        Label subtitleLabel = new Label("Invite teammates and manage access permissions");
        subtitleLabel.getStyleClass().add("share-subtitle");
        subtitleLabel.setMinWidth(Region.USE_PREF_SIZE);
        VBox titleBox = new VBox(2, titleLabel, subtitleLabel);
        titleBox.setMinWidth(Region.USE_PREF_SIZE);

        javafx.scene.shape.SVGPath folderIcon = new javafx.scene.shape.SVGPath();
        folderIcon.setContent("M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z");
        folderIcon.setStyle("-fx-fill: -text-muted;");
        folderIcon.setScaleX(0.85);
        folderIcon.setScaleY(0.85);

        Label wsTextLabel = new Label("Workspace:");
        wsTextLabel.getStyleClass().add("share-ws-label");
        wsTextLabel.setMinWidth(Region.USE_PREF_SIZE);

        ComboBox<String> workspaceCombo = new ComboBox<>();
        workspaceCombo.getStyleClass().add("share-ws-combo");
        workspaceCombo.setPromptText("Loading workspaces...");
        workspaceCombo.setPrefWidth(180);
        workspaceCombo.setMinWidth(150);
        Map<String, Long> workspaceIds = new LinkedHashMap<>();

        HBox wsSelectorBox = new HBox(8, folderIcon, wsTextLabel, workspaceCombo);
        wsSelectorBox.setAlignment(Pos.CENTER_LEFT);
        wsSelectorBox.getStyleClass().add("share-ws-chip");
        wsSelectorBox.setMinWidth(Region.USE_PREF_SIZE);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerSpacer.setMinWidth(16);

        HBox headerRow = new HBox(12, headerBadge, titleBox, headerSpacer, wsSelectorBox);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // --- Interactive Status Alert Banner ---
        HBox statusBanner = new HBox(8);
        statusBanner.getStyleClass().add("share-status-banner");
        statusBanner.setAlignment(Pos.CENTER_LEFT);
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);

        Label statusIcon = new Label();
        statusIcon.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label statusText = new Label();
        statusText.setWrapText(true);
        statusText.setMaxWidth(560);
        HBox.setHgrow(statusText, Priority.ALWAYS);

        Button dismissBannerBtn = new Button("✕");
        dismissBannerBtn.getStyleClass().add("share-text-btn");
        dismissBannerBtn.setOnAction(e -> {
            statusBanner.setVisible(false);
            statusBanner.setManaged(false);
        });
        statusBanner.getChildren().addAll(statusIcon, statusText, dismissBannerBtn);

        // Helper to show status message smoothly
        BiConsumer<String, Boolean> showStatus = (msg, isError) -> {
            statusBanner.getStyleClass().removeAll("share-status-success", "share-status-error");
            if (isError) {
                statusBanner.getStyleClass().add("share-status-error");
                statusIcon.setText("⚠");
            } else {
                statusBanner.getStyleClass().add("share-status-success");
                statusIcon.setText("✓");
            }
            statusText.setText(msg);
            statusBanner.setVisible(true);
            statusBanner.setManaged(true);
        };

        // --- Quick Invite Card ---
        VBox inviteCard = new VBox(10);
        inviteCard.getStyleClass().add("share-card");

        Label inviteCardTitle = new Label("INVITE COLLABORATOR");
        inviteCardTitle.getStyleClass().add("share-card-header");

        Region inviteCardSpacer = new Region();
        HBox.setHgrow(inviteCardSpacer, Priority.ALWAYS);

        Button toggleTeamBtn = new Button("Bulk invite team ▾");
        toggleTeamBtn.getStyleClass().add("share-text-btn");

        HBox inviteCardHeader = new HBox(8, inviteCardTitle, inviteCardSpacer, toggleTeamBtn);
        inviteCardHeader.setAlignment(Pos.CENTER_LEFT);

        TextField emailField = new TextField();
        emailField.setPromptText("Enter colleague's email address...");
        HBox.setHgrow(emailField, Priority.ALWAYS);

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("EDITOR", "VIEWER");
        roleCombo.setValue("EDITOR");
        roleCombo.getStyleClass().add("share-role-combo");

        List<EnvironmentResponse> workspaceEnvironments = new ArrayList<>();
        Set<Long> selectedEnvironmentIds = new HashSet<>();
        Button chooseEnvBtn = new Button("Environments: All");
        chooseEnvBtn.getStyleClass().add("share-env-btn");
        chooseEnvBtn.setOnAction(e -> {
            if (workspaceEnvironments.isEmpty()) {
                showStatus.accept("This workspace currently has no environments defined.", true);
                return;
            }
            openEnvironmentPickerPopup(workspaceEnvironments, selectedEnvironmentIds, () -> {
                if (selectedEnvironmentIds.isEmpty()) {
                    chooseEnvBtn.setText("Environments: All");
                    chooseEnvBtn.getStyleClass().remove("share-env-btn-active");
                } else {
                    chooseEnvBtn.setText("Environments: " + selectedEnvironmentIds.size() + " selected");
                    if (!chooseEnvBtn.getStyleClass().contains("share-env-btn-active")) {
                        chooseEnvBtn.getStyleClass().add("share-env-btn-active");
                    }
                }
            });
        });

        Button sendInviteBtn = new Button("Send Invite");
        sendInviteBtn.getStyleClass().addAll("button", "button-primary");
        sendInviteBtn.setStyle("-fx-font-weight: bold; -fx-padding: 6 16 6 16;");

        HBox inviteRow = new HBox(8, emailField, roleCombo, chooseEnvBtn, sendInviteBtn);
        inviteRow.setAlignment(Pos.CENTER_LEFT);

        // --- Collapsible Team Bulk Invite Box ---
        VBox teamBox = new VBox(8);
        teamBox.setStyle("-fx-padding: 10 0 0 0; -fx-border-color: -border-subtle transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        teamBox.setVisible(false);
        teamBox.setManaged(false);

        Label teamPromptLabel = new Label("Select a team you own/admin to invite all of its members at once:");
        teamPromptLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        ComboBox<String> bulkTeamCombo = new ComboBox<>();
        bulkTeamCombo.setPromptText("Select a team...");
        bulkTeamCombo.setPrefWidth(200);
        Map<String, Long> bulkTeamIds = new LinkedHashMap<>();

        ComboBox<String> bulkRoleCombo = new ComboBox<>();
        bulkRoleCombo.getItems().addAll("EDITOR", "VIEWER");
        bulkRoleCombo.setValue("EDITOR");
        bulkRoleCombo.getStyleClass().add("share-role-combo");

        Button inviteTeamBtn = new Button("Invite All Team Members");
        inviteTeamBtn.getStyleClass().addAll("button", "button-primary");

        HBox bulkRow = new HBox(8, bulkTeamCombo, bulkRoleCombo, inviteTeamBtn);
        bulkRow.setAlignment(Pos.CENTER_LEFT);
        teamBox.getChildren().addAll(teamPromptLabel, bulkRow);

        toggleTeamBtn.setOnAction(e -> {
            boolean showing = !teamBox.isVisible();
            teamBox.setVisible(showing);
            teamBox.setManaged(showing);
            toggleTeamBtn.setText(showing ? "Hide team invite ▴" : "Bulk invite team ▾");
        });

        inviteCard.getChildren().addAll(inviteCardHeader, inviteRow, teamBox);

        // --- Tabs for Active Collaborators and Pending Invitations ---
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("share-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab collaboratorsTab = new Tab("Collaborators (0)");
        VBox collaboratorsContainer = new VBox(8);
        collaboratorsContainer.setPadding(new Insets(10, 2, 10, 2));
        ScrollPane collabScroll = new ScrollPane(collaboratorsContainer);
        collabScroll.getStyleClass().add("share-scroll-area");
        collabScroll.setFitToWidth(true);
        collaboratorsTab.setContent(collabScroll);

        Tab pendingTab = new Tab("Pending (0)");
        VBox pendingContainer = new VBox(8);
        pendingContainer.setPadding(new Insets(10, 2, 10, 2));
        ScrollPane pendingScroll = new ScrollPane(pendingContainer);
        pendingScroll.getStyleClass().add("share-scroll-area");
        pendingScroll.setFitToWidth(true);
        pendingTab.setContent(pendingScroll);

        tabPane.getTabs().addAll(collaboratorsTab, pendingTab);

        // Load owned/admin teams in background
        new Thread(() -> {
            Optional<List<TeamResponse>> teams = TeamService.getMyTeams();
            Platform.runLater(() -> teams.ifPresent(list -> list.stream()
                    .filter(t -> "OWNER".equals(t.getMyRole()) || "ADMIN".equals(t.getMyRole()))
                    .forEach(t -> {
                        bulkTeamIds.put(t.getName(), t.getId());
                        bulkTeamCombo.getItems().add(t.getName());
                    })));
        }).start();

        // Main Dialog Content Box
        VBox mainContent = new VBox(12, headerRow, statusBanner, inviteCard, tabPane);
        mainContent.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(mainContent);
        initAndStyleDialog(dialog);

        Runnable[] refreshLists = new Runnable[1];
        refreshLists[0] = () -> {
            Long workspaceId = workspaceIds.get(workspaceCombo.getValue());
            if (workspaceId == null) {
                return;
            }
            new Thread(() -> {
                Optional<List<WorkspaceInvitationResponse>> pendingOpt =
                        WorkspaceInvitationService.getPendingInvitations(workspaceId);
                Optional<List<WorkspaceAccessResponse>> accessOpt =
                        WorkspaceSharingService.getWorkspaceAccessList(workspaceId);
                Optional<WorkspaceContentsResponse> contentsOpt =
                        WorkspaceSharingService.getWorkspaceContents(workspaceId);

                Platform.runLater(() -> {
                    workspaceEnvironments.clear();
                    contentsOpt.ifPresent(wc -> {
                        if (wc.getEnvironments() != null) {
                            workspaceEnvironments.addAll(wc.getEnvironments());
                        }
                    });

                    // Build Collaborators UI
                    collaboratorsContainer.getChildren().clear();
                    List<WorkspaceAccessResponse> accessList = accessOpt.orElse(List.of());
                    collaboratorsTab.setText("Collaborators (" + accessList.size() + ")");

                    if (accessList.isEmpty()) {
                        VBox emptyBox = new VBox(8);
                        emptyBox.getStyleClass().add("share-empty-box");
                        javafx.scene.shape.SVGPath emptyUsersSvg = new javafx.scene.shape.SVGPath();
                        emptyUsersSvg.setContent("M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z");
                        emptyUsersSvg.setStyle("-fx-fill: -text-muted; -fx-opacity: 0.6;");
                        emptyUsersSvg.setScaleX(1.3);
                        emptyUsersSvg.setScaleY(1.3);
                        Label emptyTitle = new Label("No active collaborators yet");
                        emptyTitle.getStyleClass().add("share-empty-title");
                        Label emptyDesc = new Label("Send an invitation above to share this workspace with your team.");
                        emptyDesc.getStyleClass().add("share-empty-desc");
                        emptyBox.getChildren().addAll(emptyUsersSvg, emptyTitle, emptyDesc);
                        collaboratorsContainer.getChildren().add(emptyBox);
                    } else {
                        for (WorkspaceAccessResponse access : accessList) {
                            HBox row = new HBox(12);
                            row.getStyleClass().add("share-row");

                            StackPane avatar = createAvatar(access.getUsername());

                            Label nameLabel = new Label(access.getUsername());
                            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");
                            Label emailOrRole = new Label("Collaborator");
                            emailOrRole.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
                            VBox userDetails = new VBox(2, nameLabel, emailOrRole);

                            Region rowSpacer = new Region();
                            HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                            ComboBox<String> memberRoleCombo = new ComboBox<>();
                            memberRoleCombo.getItems().addAll("EDITOR", "VIEWER");
                            memberRoleCombo.setValue(access.getRole());
                            memberRoleCombo.getStyleClass().add("share-role-combo");
                            memberRoleCombo.valueProperty().addListener((obs, was, isNow) -> {
                                if (isNow != null && !isNow.equals(was)) {
                                    new Thread(() -> {
                                        boolean ok = WorkspaceSharingService.updateAccessRole(
                                                access.getWorkspaceId(), access.getUserId(), isNow);
                                        Platform.runLater(() -> {
                                            if (ok) {
                                                showStatus.accept("Updated " + access.getUsername() + "'s role to " + isNow + ".", false);
                                            } else {
                                                showStatus.accept("Failed to update role for " + access.getUsername() + ".", true);
                                                memberRoleCombo.setValue(was);
                                            }
                                        });
                                    }).start();
                                }
                            });

                            Button memberEnvBtn = new Button();
                            memberEnvBtn.getStyleClass().add("share-env-btn");
                            int envCount = access.getAllowedEnvironmentIds() != null ? access.getAllowedEnvironmentIds().size() : 0;
                            if (envCount > 0) {
                                memberEnvBtn.setText(envCount + " envs");
                                memberEnvBtn.getStyleClass().add("share-env-btn-active");
                            } else {
                                memberEnvBtn.setText("All envs");
                            }
                            memberEnvBtn.setOnAction(e -> {
                                new Thread(() -> {
                                    Optional<WorkspaceContentsResponse> contents =
                                            WorkspaceSharingService.getWorkspaceContents(access.getWorkspaceId());
                                    Platform.runLater(() -> {
                                        List<EnvironmentResponse> available = contents.map(WorkspaceContentsResponse::getEnvironments)
                                                .orElse(List.of());
                                        if (available.isEmpty()) {
                                            showStatus.accept("This workspace has no environments defined.", true);
                                            return;
                                        }
                                        Set<Long> selected = access.getAllowedEnvironmentIds() != null
                                                ? new HashSet<>(access.getAllowedEnvironmentIds()) : new HashSet<>();
                                        openEnvironmentPickerPopup(available, selected, () -> {
                                            int newCount = selected.size();
                                            if (newCount > 0) {
                                                memberEnvBtn.setText(newCount + " envs");
                                                if (!memberEnvBtn.getStyleClass().contains("share-env-btn-active")) {
                                                    memberEnvBtn.getStyleClass().add("share-env-btn-active");
                                                }
                                            } else {
                                                memberEnvBtn.setText("All envs");
                                                memberEnvBtn.getStyleClass().remove("share-env-btn-active");
                                            }
                                            new Thread(() -> WorkspaceSharingService.updateAccessEnvironments(
                                                    access.getWorkspaceId(), access.getUserId(), new ArrayList<>(selected))).start();
                                        });
                                    });
                                }).start();
                            });

                            Button removeBtn = new Button("Remove");
                            removeBtn.getStyleClass().add("share-remove-btn");
                            removeBtn.setOnAction(e -> {
                                removeBtn.setDisable(true);
                                new Thread(() -> {
                                    boolean ok = WorkspaceSharingService.revokeAccess(access.getWorkspaceId(), access.getUserId());
                                    Platform.runLater(() -> {
                                        if (ok) {
                                            showStatus.accept("Removed access for " + access.getUsername() + ".", false);
                                            collaboratorsContainer.getChildren().remove(row);
                                            int remaining = collaboratorsContainer.getChildren().size();
                                            collaboratorsTab.setText("Collaborators (" + remaining + ")");
                                            if (remaining == 0) {
                                                refreshLists[0].run();
                                            }
                                        } else {
                                            removeBtn.setDisable(false);
                                            showStatus.accept("Failed to remove access for " + access.getUsername() + ".", true);
                                        }
                                    });
                                }).start();
                            });

                            row.getChildren().addAll(avatar, userDetails, rowSpacer, memberEnvBtn, memberRoleCombo, removeBtn);
                            collaboratorsContainer.getChildren().add(row);
                        }
                    }

                    // Build Pending UI
                    pendingContainer.getChildren().clear();
                    List<WorkspaceInvitationResponse> pendingList = pendingOpt.orElse(List.of());
                    pendingTab.setText("Pending (" + pendingList.size() + ")");

                    if (pendingList.isEmpty()) {
                        VBox emptyBox = new VBox(8);
                        emptyBox.getStyleClass().add("share-empty-box");
                        javafx.scene.shape.SVGPath emptyMailSvg = new javafx.scene.shape.SVGPath();
                        emptyMailSvg.setContent("M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z");
                        emptyMailSvg.setStyle("-fx-fill: -text-muted; -fx-opacity: 0.6;");
                        emptyMailSvg.setScaleX(1.3);
                        emptyMailSvg.setScaleY(1.3);
                        Label emptyTitle = new Label("No pending invitations");
                        emptyTitle.getStyleClass().add("share-empty-title");
                        Label emptyDesc = new Label("Invitations waiting to be accepted will appear here.");
                        emptyDesc.getStyleClass().add("share-empty-desc");
                        emptyBox.getChildren().addAll(emptyMailSvg, emptyTitle, emptyDesc);
                        pendingContainer.getChildren().add(emptyBox);
                    } else {
                        for (WorkspaceInvitationResponse invite : pendingList) {
                            HBox row = new HBox(12);
                            row.getStyleClass().add("share-row");

                            StackPane mailBadge = new StackPane();
                            mailBadge.getStyleClass().add("share-avatar");
                            mailBadge.setStyle("-fx-background-color: #f59e0b;");
                            javafx.scene.shape.SVGPath mailSvg = new javafx.scene.shape.SVGPath();
                            mailSvg.setContent("M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z");
                            mailSvg.setStyle("-fx-fill: white;");
                            mailSvg.setScaleX(0.75);
                            mailSvg.setScaleY(0.75);
                            mailBadge.getChildren().add(mailSvg);

                            Label emailLabel = new Label(invite.getEmail());
                            emailLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");
                            Label pendingLabel = new Label("Awaiting acceptance  •  " + invite.getRole());
                            pendingLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
                            VBox inviteDetails = new VBox(2, emailLabel, pendingLabel);

                            Region rowSpacer = new Region();
                            HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                            Button cancelInviteBtn = new Button("Cancel Invitation");
                            cancelInviteBtn.getStyleClass().add("share-remove-btn");
                            cancelInviteBtn.setOnAction(e -> {
                                cancelInviteBtn.setDisable(true);
                                new Thread(() -> {
                                    boolean ok = WorkspaceInvitationService.revokeInvitation(workspaceId, invite.getId());
                                    Platform.runLater(() -> {
                                        if (ok) {
                                            showStatus.accept("Cancelled invitation to " + invite.getEmail() + ".", false);
                                            pendingContainer.getChildren().remove(row);
                                            int remaining = pendingContainer.getChildren().size();
                                            pendingTab.setText("Pending (" + remaining + ")");
                                            if (remaining == 0) {
                                                refreshLists[0].run();
                                            }
                                        } else {
                                            cancelInviteBtn.setDisable(false);
                                            showStatus.accept("Failed to cancel invitation.", true);
                                        }
                                    });
                                }).start();
                            });

                            row.getChildren().addAll(mailBadge, inviteDetails, rowSpacer, cancelInviteBtn);
                            pendingContainer.getChildren().add(row);
                        }
                    }
                });
            }).start();
        };

        workspaceCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                statusBanner.setVisible(false);
                statusBanner.setManaged(false);
                refreshLists[0].run();
            }
        });

        new Thread(() -> {
            Optional<List<Workspace>> workspaces = WorkspaceService.getUserWorkspaces();
            Platform.runLater(() -> {
                workspaces.ifPresent(list -> list.forEach(w -> {
                    workspaceIds.put(w.getName(), w.getId());
                    workspaceCombo.getItems().add(w.getName());
                }));
                if (WorkspaceManager.hasWorkspace()) {
                    String currentName = WorkspaceManager.getCurrentWorkspace().getName();
                    if (workspaceCombo.getItems().contains(currentName)) {
                        workspaceCombo.setValue(currentName);
                    } else if (!workspaceCombo.getItems().isEmpty()) {
                        workspaceCombo.setValue(workspaceCombo.getItems().get(0));
                    }
                } else if (!workspaceCombo.getItems().isEmpty()) {
                    workspaceCombo.setValue(workspaceCombo.getItems().get(0));
                }
            });
        }).start();

        sendInviteBtn.setOnAction(e -> {
            Long workspaceId = workspaceIds.get(workspaceCombo.getValue());
            if (workspaceId == null) {
                showStatus.accept("Please choose a workspace first.", true);
                return;
            }
            String email = emailField.getText() == null ? "" : emailField.getText().trim();
            if (email.isEmpty()) {
                showStatus.accept("Please enter an email address.", true);
                return;
            }
            showStatus.accept("Sending invitation...", false);
            sendInviteBtn.setDisable(true);
            InviteToWorkspaceRequest request = new InviteToWorkspaceRequest(
                    email, roleCombo.getValue(), new ArrayList<>(selectedEnvironmentIds));
            new Thread(() -> {
                try {
                    WorkspaceInvitationService.inviteToWorkspace(workspaceId, request);
                    Platform.runLater(() -> {
                        showStatus.accept("Invitation successfully sent to " + email + ".", false);
                        sendInviteBtn.setDisable(false);
                        emailField.clear();
                        selectedEnvironmentIds.clear();
                        chooseEnvBtn.setText("Environments: All");
                        chooseEnvBtn.getStyleClass().remove("share-env-btn-active");
                        refreshLists[0].run();
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        showStatus.accept("Couldn't send invite: " + WorkspaceInvitationService.friendlyMessage(ex), true);
                        sendInviteBtn.setDisable(false);
                    });
                }
            }).start();
        });

        inviteTeamBtn.setOnAction(e -> {
            Long workspaceId = workspaceIds.get(workspaceCombo.getValue());
            Long teamId = bulkTeamIds.get(bulkTeamCombo.getValue());
            if (workspaceId == null) {
                showStatus.accept("Please select a workspace first.", true);
                return;
            }
            if (teamId == null) {
                showStatus.accept("Please select a team first.", true);
                return;
            }
            showStatus.accept("Inviting team members...", false);
            inviteTeamBtn.setDisable(true);
            InviteTeamRequest request = new InviteTeamRequest(teamId, bulkRoleCombo.getValue(), List.of());
            new Thread(() -> {
                try {
                    List<WorkspaceInvitationResponse> sent = WorkspaceInvitationService.inviteTeamMembers(workspaceId, request);
                    Platform.runLater(() -> {
                        inviteTeamBtn.setDisable(false);
                        showStatus.accept(sent.isEmpty()
                                ? "Everyone on that team already has access or a pending invitation."
                                : "Successfully invited " + sent.size() + " team member(s).", false);
                        refreshLists[0].run();
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        inviteTeamBtn.setDisable(false);
                        showStatus.accept("Couldn't invite team: " + WorkspaceInvitationService.friendlyMessage(ex), true);
                    });
                }
            }).start();
        });

        dialog.showAndWait();
    }

    /** The other half of Share — enter a code you received by email to
     * get access to someone else's workspace. Mirrors "Join Team"'s
     * existing code-entry pattern for consistency. */
    @FXML
    private void handleAcceptWorkspaceInvite() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Enter Invite Code");
        dialog.setHeaderText("Paste the invite code from the email you received.");
        dialog.setContentText("Code:");
        initAndStyleDialog(dialog);

        dialog.showAndWait().ifPresent(code -> {
            String trimmed = code == null ? "" : code.trim();
            if (trimmed.isEmpty()) {
                return;
            }
            new Thread(() -> {
                try {
                    WorkspaceAccessResponse access = WorkspaceInvitationService.acceptInvitation(trimmed);
                    Platform.runLater(() -> {
                        updateStatus("You now have " + access.getRole() + " access to \"" + access.getWorkspaceName() + "\".");
                        loadTeamWorkspaces();
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> AlertUtils.showError(
                            "Couldn't accept that invite: " + WorkspaceInvitationService.friendlyMessage(ex)));
                }
            }).start();
        });
    }


    /** Modern popup used by the Share dialog's Environments column — a
     * clean checklist of the workspace's environments with quick select all/clear actions,
     * mutating the passed-in selected set directly and calling onChange so the
     * button label updates immediately without full refresh. */
    private void openEnvironmentPickerPopup(List<EnvironmentResponse> available, Set<Long> selected, Runnable onChange) {
        Dialog<Void> popup = new Dialog<>();
        popup.setTitle("Environment Access");
        popup.setHeaderText(null);
        popup.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        popup.getDialogPane().getStyleClass().add("share-dialog-pane");
        popup.getDialogPane().setStyle("-fx-pref-width: 440px; -fx-pref-height: 380px; -fx-min-width: 400px; -fx-min-height: 320px;");

        Label title = new Label("Select Environments");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        Label desc = new Label("Choose which environments are visible to this collaborator. If none are selected, all environments remain accessible.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        Button selectAllBtn = new Button("Select All");
        selectAllBtn.getStyleClass().add("share-text-btn");

        Button clearAllBtn = new Button("Clear All");
        clearAllBtn.getStyleClass().add("share-text-btn");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox quickActions = new HBox(8, selectAllBtn, clearAllBtn);
        quickActions.setAlignment(Pos.CENTER_RIGHT);
        HBox topRow = new HBox(title, spacer, quickActions);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox checklist = new VBox(8);
        checklist.setPadding(new Insets(10));
        List<CheckBox> checkBoxes = new ArrayList<>();

        for (EnvironmentResponse env : available) {
            CheckBox cb = new CheckBox(env.getName());
            cb.setSelected(selected.contains(env.getId()));
            cb.setStyle("-fx-font-size: 12px; -fx-cursor: hand;");
            cb.selectedProperty().addListener((obs, was, isNow) -> {
                if (isNow) {
                    selected.add(env.getId());
                } else {
                    selected.remove(env.getId());
                }
                onChange.run();
            });
            checkBoxes.add(cb);
            checklist.getChildren().add(cb);
        }

        selectAllBtn.setOnAction(e -> {
            for (CheckBox cb : checkBoxes) {
                cb.setSelected(true);
            }
        });

        clearAllBtn.setOnAction(e -> {
            for (CheckBox cb : checkBoxes) {
                cb.setSelected(false);
            }
        });

        ScrollPane scroll = new ScrollPane(checklist);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("share-scroll-area");
        scroll.setStyle("-fx-background-color: -bg-panel; -fx-background: -bg-panel; -fx-border-color: -border-subtle; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(10, topRow, desc, scroll);
        content.setPadding(new Insets(16));
        popup.getDialogPane().setContent(content);
        initAndStyleDialog(popup);
        popup.showAndWait();
    }

    /** Creates a circular initials avatar badge with deterministic background color. */
    private StackPane createAvatar(String name) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("share-avatar");
        String initials = "?";
        if (name != null && !name.trim().isEmpty()) {
            String trimmed = name.trim();
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else if (!trimmed.isEmpty()) {
                initials = ("" + trimmed.charAt(0)).toUpperCase();
            }
        }
        Label text = new Label(initials);
        text.getStyleClass().add("share-avatar-text");
        String[] colors = {"#4F46E5", "#0891B2", "#059669", "#D97706", "#DC2626", "#7C3AED", "#DB2777"};
        int hash = Math.abs((name != null ? name : "").hashCode());
        String color = colors[hash % colors.length];
        avatar.setStyle("-fx-background-color: " + color + ";");
        avatar.getChildren().add(text);
        return avatar;
    }

    /** Read-only browse of a workspace shared with you — collections,
     * folders, requests, and environments. Editing/sending requests
     * in-place here is a natural next step; this establishes the
     * access-controlled viewing experience first. */
    private void openSharedWorkspaceBrowseDialog(WorkspaceAccessResponse access) {
        boolean canSend = "EDITOR".equals(access.getRole());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(access.getWorkspaceName());
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("shared-workspace-dialog");

        // --- Header ---
        StackPane headerBadge = new StackPane();
        headerBadge.getStyleClass().add("share-header-badge");
        headerBadge.setMinSize(38, 38);
        headerBadge.setMaxSize(38, 38);

        javafx.scene.shape.SVGPath wsIcon = new javafx.scene.shape.SVGPath();
        wsIcon.setContent("M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z");
        wsIcon.setStyle("-fx-fill: -accent;");
        wsIcon.setScaleX(1.1);
        wsIcon.setScaleY(1.1);
        headerBadge.getChildren().add(wsIcon);

        Label titleLabel = new Label(access.getWorkspaceName());
        titleLabel.getStyleClass().add("share-title");

        String ownerName = access.getWorkspaceOwnerUsername() != null ? access.getWorkspaceOwnerUsername() : "owner";
        String subtitleText = "Shared workspace owned by " + ownerName;
        if (access.getTeamName() != null && !access.getTeamName().isBlank()) {
            subtitleText += " • via " + access.getTeamName();
        }
        Label subtitleLabel = new Label(subtitleText);
        subtitleLabel.getStyleClass().add("share-subtitle");
        subtitleLabel.setMinWidth(Region.USE_PREF_SIZE);

        VBox titleBox = new VBox(2, titleLabel, subtitleLabel);
        titleBox.setMinWidth(Region.USE_PREF_SIZE);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerSpacer.setMinWidth(12);

        Label roleBadge = new Label(access.getRole());
        roleBadge.getStyleClass().addAll("role-badge",
                canSend ? "role-badge-owner" : "role-badge-member");

        Label hintBadge = new Label(canSend ? "Double-click request to send" : "Browse only");
        hintBadge.getStyleClass().add("shared-workspace-hint");
        hintBadge.setStyle("-fx-padding: 3 8 3 8; -fx-background-color: -bg-elevated; -fx-background-radius: 10px;");

        HBox headerRight = new HBox(8, hintBadge, roleBadge);
        headerRight.setAlignment(Pos.CENTER_RIGHT);

        HBox headerRow = new HBox(12, headerBadge, titleBox, headerSpacer, headerRight);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // --- Tabs: Collections & Requests, and Environments ---
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("share-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab collectionsTab = new Tab("Collections & Requests");
        Tab environmentsTab = new Tab("Environments (0)");

        // Collections Content
        VBox collectionsBox = new VBox(10);
        collectionsBox.setPadding(new Insets(10, 0, 0, 0));

        TextField filterField = new TextField();
        filterField.setPromptText("Filter collections and requests...");
        filterField.getStyleClass().add("search-field");
        filterField.setStyle("-fx-pref-height: 32px; -fx-font-size: 12px;");
        HBox.setHgrow(filterField, Priority.ALWAYS);

        Button expandAllBtn = new Button("Expand All");
        expandAllBtn.getStyleClass().add("share-text-btn");

        Button collapseAllBtn = new Button("Collapse All");
        collapseAllBtn.getStyleClass().add("share-text-btn");

        HBox filterBar = new HBox(8, filterField, expandAllBtn, collapseAllBtn);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Map<TreeItem<String>, RequestResponse> requestByNode = new HashMap<>();
        Set<TreeItem<String>> folderNodes = new HashSet<>();
        List<EnvironmentResponse> loadedEnvironments = new ArrayList<>();

        TreeItem<String> root = new TreeItem<>(access.getWorkspaceName());
        root.setExpanded(true);
        TreeView<String> tree = new TreeView<>(root);
        tree.setShowRoot(false);
        tree.getStyleClass().add("shared-workspace-tree");
        VBox.setVgrow(tree, Priority.ALWAYS);

        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                TreeItem<String> treeItem = getTreeItem();
                RequestResponse req = requestByNode.get(treeItem);
                if (req != null) {
                    Label methodLabel = new Label(String.valueOf(req.getMethod()));
                    methodLabel.getStyleClass().addAll("method-chip",
                            "method-chip-" + req.getMethod().toString().toLowerCase(Locale.ROOT));
                    methodLabel.setMinWidth(48);

                    Label nameLabel = new Label(item);
                    nameLabel.setStyle("-fx-text-fill: -text-primary; -fx-font-size: 12px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    HBox row = new HBox(8, methodLabel, nameLabel, spacer);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("shared-workspace-request-row");

                    if (canSend) {
                        Label runHint = new Label("Send ↗");
                        runHint.getStyleClass().add("share-text-btn");
                        runHint.setOnMouseClicked(ev -> {
                            ev.consume();
                            openSendSharedRequestDialog(access.getWorkspaceId(), req, loadedEnvironments);
                        });
                        row.getChildren().add(runHint);
                    }

                    setGraphic(row);
                    setText(null);
                } else if (folderNodes.contains(treeItem)) {
                    ImageView icon = new ImageView(new Image(
                            getClass().getResourceAsStream("/images/folder-icon.png")));
                    icon.setFitHeight(14);
                    icon.setFitWidth(14);
                    icon.setPreserveRatio(true);
                    setGraphic(icon);
                    setText(item);
                    getStyleClass().removeAll("shared-workspace-collection-cell");
                } else {
                    ImageView icon = new ImageView(new Image(
                            getClass().getResourceAsStream("/images/collection-icon.png")));
                    icon.setFitHeight(15);
                    icon.setFitWidth(15);
                    icon.setPreserveRatio(true);
                    setGraphic(icon);
                    setText(item);
                    if (!getStyleClass().contains("shared-workspace-collection-cell")) {
                        getStyleClass().add("shared-workspace-collection-cell");
                    }
                }
            }
        });

        expandAllBtn.setOnAction(e -> setTreeExpanded(root, true));
        collapseAllBtn.setOnAction(e -> setTreeExpanded(root, false));

        filterField.textProperty().addListener((obs, oldVal, query) -> {
            String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
            if (q.isEmpty()) {
                setTreeExpanded(root, true);
            } else {
                expandMatchingNodes(root, q);
            }
        });

        collectionsBox.getChildren().addAll(filterBar, tree);
        collectionsTab.setContent(collectionsBox);

        // Environments Content
        VBox environmentsContainer = new VBox(8);
        environmentsContainer.setPadding(new Insets(12, 4, 12, 4));
        ScrollPane envScroll = new ScrollPane(environmentsContainer);
        envScroll.getStyleClass().add("share-scroll-area");
        envScroll.setFitToWidth(true);
        environmentsTab.setContent(envScroll);

        tabPane.getTabs().addAll(collectionsTab, environmentsTab);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(24, 24);
        Label statusLabel = new Label("Loading shared workspace contents…");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        HBox loadingRow = new HBox(8, spinner, statusLabel);
        loadingRow.setAlignment(Pos.CENTER_LEFT);
        loadingRow.setPadding(new Insets(4, 0, 4, 0));

        VBox content = new VBox(12, headerRow, loadingRow, tabPane);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        initAndStyleDialog(dialog);

        Runnable renderEnvironments = () -> {
            environmentsContainer.getChildren().clear();
            environmentsTab.setText("Environments (" + loadedEnvironments.size() + ")");
            if (loadedEnvironments.isEmpty()) {
                VBox emptyBox = new VBox(8);
                emptyBox.getStyleClass().add("share-empty-box");
                javafx.scene.shape.SVGPath emptyGlobeSvg = new javafx.scene.shape.SVGPath();
                emptyGlobeSvg.setContent("M12 2a10 10 0 100 20 10 10 0 000-20zm0 18c-4.41 0-8-3.59-8-8 0-.55.06-1.09.17-1.61L9 15.24V17a2 2 0 002 2v.93c.33.05.66.07 1 .07zm6.93-4.16A7.96 7.96 0 0019 12c0-1.42-.37-2.75-1.02-3.91l-4.91 4.91a2 2 0 01-1.41.59h-1.6v2h2a2 2 0 002-2v-.59l5.93-2.16z");
                emptyGlobeSvg.setStyle("-fx-fill: -text-muted; -fx-opacity: 0.6;");
                emptyGlobeSvg.setScaleX(1.3);
                emptyGlobeSvg.setScaleY(1.3);
                Label emptyTitle = new Label("No environments shared in this workspace");
                emptyTitle.getStyleClass().add("share-empty-title");
                Label emptyDesc = new Label("Environments granted to you by the workspace owner will appear here.");
                emptyDesc.getStyleClass().add("share-empty-desc");
                emptyBox.getChildren().addAll(emptyGlobeSvg, emptyTitle, emptyDesc);
                environmentsContainer.getChildren().add(emptyBox);
            } else {
                for (EnvironmentResponse env : loadedEnvironments) {
                    HBox row = new HBox(12);
                    row.getStyleClass().add("share-row");
                    row.setAlignment(Pos.CENTER_LEFT);

                    StackPane envIconBadge = new StackPane();
                    envIconBadge.getStyleClass().add("share-avatar");
                    envIconBadge.setStyle("-fx-background-color: #0891B2;");
                    javafx.scene.shape.SVGPath envSvg = new javafx.scene.shape.SVGPath();
                    envSvg.setContent("M12 2a10 10 0 100 20 10 10 0 000-20zm0 18c-4.41 0-8-3.59-8-8 0-.55.06-1.09.17-1.61L9 15.24V17a2 2 0 002 2v.93c.33.05.66.07 1 .07zm6.93-4.16A7.96 7.96 0 0019 12c0-1.42-.37-2.75-1.02-3.91l-4.91 4.91a2 2 0 01-1.41.59h-1.6v2h2a2 2 0 002-2v-.59l5.93-2.16z");
                    envSvg.setStyle("-fx-fill: white;");
                    envSvg.setScaleX(0.75);
                    envSvg.setScaleY(0.75);
                    envIconBadge.getChildren().add(envSvg);

                    int varCount = env.getVariables() != null ? env.getVariables().size() : 0;
                    Label nameLabel = new Label(env.getName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text-primary;");

                    Label metaLabel = new Label(varCount + (varCount == 1 ? " variable" : " variables")
                            + " • " + (canSend ? "Double-click to view or edit" : "Double-click to view"));
                    metaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
                    VBox details = new VBox(2, nameLabel, metaLabel);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button actionBtn = new Button(canSend ? "Edit Variables" : "View Variables");
                    actionBtn.getStyleClass().add("share-env-btn");
                    actionBtn.setOnAction(ev -> openSharedEnvironmentDialog(env, canSend));

                    row.setOnMouseClicked(ev -> {
                        if (ev.getClickCount() == 2) {
                            openSharedEnvironmentDialog(env, canSend);
                        }
                    });

                    row.getChildren().addAll(envIconBadge, details, spacer, actionBtn);
                    environmentsContainer.getChildren().add(row);
                }
            }
        };

        if (canSend) {
            tree.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    TreeItem<String> selected = tree.getSelectionModel().getSelectedItem();
                    RequestResponse req = selected != null ? requestByNode.get(selected) : null;
                    if (req != null) {
                        openSendSharedRequestDialog(access.getWorkspaceId(), req, loadedEnvironments);
                    }
                }
            });
        }

        new Thread(() -> {
            Optional<WorkspaceContentsResponse> contents = WorkspaceSharingService.getWorkspaceContents(access.getWorkspaceId());
            Platform.runLater(() -> {
                loadingRow.setVisible(false);
                loadingRow.setManaged(false);
                contents.ifPresent(wc -> {
                    if (wc.getCollections() != null) {
                        for (CollectionResponse collection : wc.getCollections()) {
                            TreeItem<String> collectionNode = new TreeItem<>(collection.getName());

                            List<FolderResponse> folders = collection.getFolderResponses() != null
                                    ? collection.getFolderResponses() : List.of();
                            List<RequestResponse> requests = collection.getRequestResponses() != null
                                    ? collection.getRequestResponses() : List.of();

                            Map<Long, TreeItem<String>> folderNodeById = new HashMap<>();
                            for (FolderResponse folder : folders) {
                                TreeItem<String> folderNode = new TreeItem<>(folder.getName());
                                folderNodes.add(folderNode);
                                folderNodeById.put(folder.getId(), folderNode);
                            }
                            for (FolderResponse folder : folders) {
                                TreeItem<String> folderNode = folderNodeById.get(folder.getId());
                                TreeItem<String> parentNode = folder.getParentFolderId() != null
                                        ? folderNodeById.get(folder.getParentFolderId()) : null;
                                (parentNode != null ? parentNode : collectionNode).getChildren().add(folderNode);
                            }
                            for (RequestResponse request : requests) {
                                TreeItem<String> requestNode = new TreeItem<>(request.getName());
                                requestByNode.put(requestNode, request);
                                TreeItem<String> parentNode = request.getFolderId() != null
                                        ? folderNodeById.get(request.getFolderId()) : null;
                                (parentNode != null ? parentNode : collectionNode).getChildren().add(requestNode);
                            }
                            root.getChildren().add(collectionNode);
                        }
                    }
                    if (wc.getEnvironments() != null) {
                        loadedEnvironments.addAll(wc.getEnvironments());
                    }
                    renderEnvironments.run();
                });
                if (contents.isEmpty()) {
                    statusLabel.setText("Couldn't load this workspace — you may no longer have access.");
                    loadingRow.setVisible(true);
                    loadingRow.setManaged(true);
                    spinner.setVisible(false);
                }
            });
        }).start();

        dialog.showAndWait();
    }

    private void setTreeExpanded(TreeItem<String> item, boolean expanded) {
        if (item == null) return;
        item.setExpanded(expanded);
        for (TreeItem<String> child : item.getChildren()) {
            setTreeExpanded(child, expanded);
        }
    }

    private boolean expandMatchingNodes(TreeItem<String> item, String query) {
        if (item == null) return false;
        boolean childMatched = false;
        for (TreeItem<String> child : item.getChildren()) {
            if (expandMatchingNodes(child, query)) {
                childMatched = true;
            }
        }
        boolean matched = (item.getValue() != null && item.getValue().toLowerCase(Locale.ROOT).contains(query)) || childMatched;
        item.setExpanded(matched);
        return matched;
    }

    /** Shows one shared workspace's environment variables — Editors can
     * change and save them, Viewers get a clean read-only view. */
    private void openSharedEnvironmentDialog(EnvironmentResponse env, boolean canEdit) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(env.getName());
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("shared-env-dialog");

        // Header
        StackPane headerBadge = new StackPane();
        headerBadge.getStyleClass().add("share-header-badge");
        headerBadge.setMinSize(36, 36);
        headerBadge.setMaxSize(36, 36);

        javafx.scene.shape.SVGPath globeSvg = new javafx.scene.shape.SVGPath();
        globeSvg.setContent("M12 2a10 10 0 100 20 10 10 0 000-20zm0 18c-4.41 0-8-3.59-8-8 0-.55.06-1.09.17-1.61L9 15.24V17a2 2 0 002 2v.93c.33.05.66.07 1 .07zm6.93-4.16A7.96 7.96 0 0019 12c0-1.42-.37-2.75-1.02-3.91l-4.91 4.91a2 2 0 01-1.41.59h-1.6v2h2a2 2 0 002-2v-.59l5.93-2.16z");
        globeSvg.setStyle("-fx-fill: -accent;");
        globeSvg.setScaleX(1.0);
        globeSvg.setScaleY(1.0);
        headerBadge.getChildren().add(globeSvg);

        Label titleLabel = new Label(env.getName());
        titleLabel.getStyleClass().add("share-title");

        Label subtitleLabel = new Label(canEdit
                ? "Edit environment variables • Changes sync directly back to the shared workspace"
                : "Viewer access — variables are displayed read-only");
        subtitleLabel.getStyleClass().add("share-subtitle");
        subtitleLabel.setMinWidth(Region.USE_PREF_SIZE);

        VBox titleBox = new VBox(2, titleLabel, subtitleLabel);
        HBox headerRow = new HBox(12, headerBadge, titleBox);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Status Banner
        HBox statusBanner = new HBox(8);
        statusBanner.getStyleClass().add("share-status-banner");
        statusBanner.setAlignment(Pos.CENTER_LEFT);
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);

        Label statusIcon = new Label();
        statusIcon.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label statusText = new Label();
        statusText.setWrapText(true);
        HBox.setHgrow(statusText, Priority.ALWAYS);

        Button dismissBannerBtn = new Button("✕");
        dismissBannerBtn.getStyleClass().add("share-text-btn");
        dismissBannerBtn.setOnAction(e -> {
            statusBanner.setVisible(false);
            statusBanner.setManaged(false);
        });
        statusBanner.getChildren().addAll(statusIcon, statusText, dismissBannerBtn);

        BiConsumer<String, Boolean> showStatus = (msg, isError) -> {
            statusBanner.getStyleClass().removeAll("share-status-success", "share-status-error");
            if (isError) {
                statusBanner.getStyleClass().add("share-status-error");
                statusIcon.setText("⚠");
            } else {
                statusBanner.getStyleClass().add("share-status-success");
                statusIcon.setText("✓");
            }
            statusText.setText(msg);
            statusBanner.setVisible(true);
            statusBanner.setManaged(true);
        };

        // Table
        TableView<KeyValuePair> varsTable = new TableView<>();
        TableColumn<KeyValuePair, String> keyCol = new TableColumn<>("Variable");
        keyCol.setCellValueFactory(new PropertyValueFactory<>("key"));
        keyCol.setPrefWidth(220);

        TableColumn<KeyValuePair, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setPrefWidth(360);

        if (canEdit) {
            keyCol.setCellFactory(TextFieldTableCell.forTableColumn());
            keyCol.setOnEditCommit(e -> e.getRowValue().setKey(e.getNewValue()));
            valueCol.setCellFactory(TextFieldTableCell.forTableColumn());
            valueCol.setOnEditCommit(e -> e.getRowValue().setValue(e.getNewValue()));
            varsTable.setEditable(true);
        }

        varsTable.getColumns().addAll(keyCol, valueCol);
        varsTable.getStyleClass().add("shared-workspace-tree");
        varsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        varsTable.setPlaceholder(new Label("No variables in this environment."));
        VBox.setVgrow(varsTable, Priority.ALWAYS);

        if (env.getVariables() != null) {
            env.getVariables().forEach((k, v) -> varsTable.getItems().add(new KeyValuePair(k, v)));
        }

        // Actions
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        if (canEdit) {
            Button addRowBtn = new Button("+ Add Variable");
            addRowBtn.getStyleClass().add("share-env-btn");
            addRowBtn.setOnAction(e -> {
                KeyValuePair newPair = new KeyValuePair("", "");
                varsTable.getItems().add(newPair);
                varsTable.getSelectionModel().select(newPair);
            });

            Button deleteRowBtn = new Button("Delete Selected");
            deleteRowBtn.getStyleClass().add("share-remove-btn");
            deleteRowBtn.setOnAction(e -> {
                KeyValuePair selected = varsTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    varsTable.getItems().remove(selected);
                }
            });

            Region actionSpacer = new Region();
            HBox.setHgrow(actionSpacer, Priority.ALWAYS);

            Button saveBtn = new Button("Save Changes");
            saveBtn.getStyleClass().addAll("button", "button-primary");
            saveBtn.setStyle("-fx-font-weight: bold; -fx-padding: 6 16 6 16;");
            saveBtn.setOnAction(e -> {
                Map<String, String> updated = new LinkedHashMap<>();
                for (KeyValuePair pair : varsTable.getItems()) {
                    if (pair.getKey() != null && !pair.getKey().isBlank()) {
                        updated.put(pair.getKey().trim(), pair.getValue() == null ? "" : pair.getValue());
                    }
                }
                showStatus.accept("Saving changes to server...", false);
                saveBtn.setDisable(true);
                new Thread(() -> {
                    Optional<EnvironmentResponse> saved = EnvironmentService.updateEnvironmentVariables(env.getId(), updated);
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        if (saved.isPresent()) {
                            env.setVariables(updated);
                            showStatus.accept("Environment variables saved successfully.", false);
                        } else {
                            showStatus.accept("Failed to save variables — please check your connection and access.", true);
                        }
                    });
                }).start();
            });

            actionsRow.getChildren().addAll(addRowBtn, deleteRowBtn, actionSpacer, saveBtn);
        }

        VBox content = new VBox(12, headerRow, statusBanner, varsTable, actionsRow);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        initAndStyleDialog(dialog);
        dialog.showAndWait();
    }

    /** Lets an Editor send a request that lives in someone else's shared workspace. */
    private void openSendSharedRequestDialog(Long workspaceId, RequestResponse request, List<EnvironmentResponse> environments) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(request.getName());
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("shared-request-dialog");

        // Header
        Label methodChip = new Label(String.valueOf(request.getMethod()));
        methodChip.getStyleClass().addAll("method-chip",
                "method-chip-" + request.getMethod().toString().toLowerCase(Locale.ROOT));
        methodChip.setStyle("-fx-font-size: 12px; -fx-padding: 4 10 4 10; -fx-font-weight: bold;");

        Label titleLabel = new Label(request.getName());
        titleLabel.getStyleClass().add("share-title");

        Label subtitleLabel = new Label("Execute request against shared workspace environment");
        subtitleLabel.getStyleClass().add("share-subtitle");

        VBox titleBox = new VBox(2, titleLabel, subtitleLabel);
        HBox headerRow = new HBox(10, methodChip, titleBox);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Interactive URL & Environment & Send Row
        TextField urlField = new TextField(request.getUrl());
        urlField.getStyleClass().add("shared-workspace-url-field");
        urlField.setStyle("-fx-font-family: 'Consolas', 'JetBrains Mono', monospace; -fx-pref-height: 34px; -fx-font-size: 12px;");
        HBox.setHgrow(urlField, Priority.ALWAYS);

        ComboBox<String> envCombo = new ComboBox<>();
        envCombo.setStyle("-fx-pref-height: 34px; -fx-font-size: 11px;");
        Map<String, EnvironmentResponse> envByName = new LinkedHashMap<>();
        envCombo.getItems().add("No Environment");
        for (EnvironmentResponse env : environments) {
            envByName.put(env.getName(), env);
            envCombo.getItems().add(env.getName());
        }
        envCombo.getSelectionModel().select(environments.isEmpty() ? "No Environment" : environments.get(0).getName());

        Button sendBtn = new Button("➤  Send");
        sendBtn.getStyleClass().addAll("button", "button-primary");
        sendBtn.setStyle("-fx-font-weight: bold; -fx-pref-height: 34px; -fx-padding: 6 18 6 18;");

        HBox requestBar = new HBox(8, urlField, envCombo, sendBtn);
        requestBar.setAlignment(Pos.CENTER_LEFT);

        // Request Tabs: Headers & Body
        TabPane reqTabPane = new TabPane();
        reqTabPane.getStyleClass().add("share-tab-pane");
        reqTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        reqTabPane.setPrefHeight(160);

        Tab headersTab = new Tab("Headers");
        TextArea headersArea = new TextArea(request.getHeaders());
        headersArea.setPromptText("{\"Content-Type\": \"application/json\"}");
        headersArea.setStyle("-fx-font-family: 'Consolas', 'JetBrains Mono', monospace; -fx-font-size: 11px;");
        headersArea.setWrapText(true);
        headersTab.setContent(headersArea);

        Tab bodyTab = new Tab("Body");
        TextArea bodyArea = new TextArea(request.getBody());
        bodyArea.setPromptText("Request payload body (JSON, text, or raw)");
        bodyArea.setStyle("-fx-font-family: 'Consolas', 'JetBrains Mono', monospace; -fx-font-size: 11px;");
        bodyArea.setWrapText(true);
        bodyTab.setContent(bodyArea);

        reqTabPane.getTabs().addAll(headersTab, bodyTab);

        // Response Status Bar
        HBox responseStatusBar = new HBox(12);
        responseStatusBar.setAlignment(Pos.CENTER_LEFT);
        responseStatusBar.setStyle("-fx-padding: 4 2 4 2;");

        Label responseSectionTitle = new Label("RESPONSE");
        responseSectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -text-secondary;");

        Label statusChip = new Label("Ready");
        statusChip.setStyle("-fx-padding: 3 8 3 8; -fx-background-radius: 4px; -fx-background-color: -bg-elevated; -fx-text-fill: -text-muted; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);

        Button copyResponseBtn = new Button("Copy");
        copyResponseBtn.getStyleClass().add("share-text-btn");

        Button saveFileBtn = new Button("Save File");
        saveFileBtn.getStyleClass().addAll("button", "button-primary");
        saveFileBtn.setVisible(false);
        saveFileBtn.setManaged(false);

        responseStatusBar.getChildren().addAll(responseSectionTitle, statusChip, statusSpacer, copyResponseBtn, saveFileBtn);

        // Response Body Area
        TextArea responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.getStyleClass().add("shared-workspace-response-area");
        responseArea.setPromptText("Response will show here after sending.");
        responseArea.setStyle("-fx-font-family: 'Consolas', 'JetBrains Mono', monospace; -fx-font-size: 11px;");
        responseArea.setWrapText(true);
        VBox.setVgrow(responseArea, Priority.ALWAYS);

        copyResponseBtn.setOnAction(e -> {
            String text = responseArea.getText();
            if (text != null && !text.isBlank()) {
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(text);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                copyResponseBtn.setText("Copied! ✓");
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> copyResponseBtn.setText("Copy"));
                }).start();
            }
        });

        // Script variables pill
        Label scriptResultLabel = new Label();
        scriptResultLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -accent; -fx-font-weight: bold;");
        scriptResultLabel.setVisible(false);
        scriptResultLabel.setManaged(false);

        VBox content = new VBox(10,
                headerRow,
                requestBar,
                reqTabPane,
                responseStatusBar,
                responseArea,
                scriptResultLabel);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        initAndStyleDialog(dialog);

        sendBtn.setOnAction(e -> {
            statusChip.setText("Sending…");
            statusChip.setStyle("-fx-padding: 3 8 3 8; -fx-background-radius: 4px; -fx-background-color: -bg-elevated; -fx-text-fill: -text-muted; -fx-font-size: 11px; -fx-font-weight: bold;");
            sendBtn.setDisable(true);
            Map<String, String> vars = Map.of();
            if (!environments.isEmpty() && envCombo.getValue() != null) {
                EnvironmentResponse selectedEnv = envByName.get(envCombo.getValue());
                if (selectedEnv != null && selectedEnv.getVariables() != null) {
                    vars = selectedEnv.getVariables();
                }
            }
            final Map<String, String> resolveVars = vars;
            String resolvedHeadersJson = VariableResolver.resolve(headersArea.getText(), resolveVars);
            try {
                String authType = request.getAuthType();
                String authHeaderValue = null;
                if ("Bearer Token".equals(authType) && request.getAuthToken() != null && !request.getAuthToken().isBlank()) {
                    authHeaderValue = "Bearer " + VariableResolver.resolve(request.getAuthToken(), resolveVars);
                } else if ("Basic Auth".equals(authType)
                        && request.getAuthUsername() != null && !request.getAuthUsername().isBlank()) {
                    String user = VariableResolver.resolve(request.getAuthUsername(), resolveVars);
                    String pass = VariableResolver.resolve(
                            request.getAuthPassword() != null ? request.getAuthPassword() : "", resolveVars);
                    authHeaderValue = "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
                }
                if (authHeaderValue != null) {
                    JSONObject headersObj = (resolvedHeadersJson == null || resolvedHeadersJson.isBlank())
                            ? new JSONObject() : new JSONObject(resolvedHeadersJson);
                    if (!headersObj.has("Authorization")) {
                        headersObj.put("Authorization", authHeaderValue);
                    }
                    resolvedHeadersJson = headersObj.toString();
                }
            } catch (Exception ignored) {
            }
            ApiRequest overrides = ApiRequest.builder()
                    .url(VariableResolver.resolve(urlField.getText(), resolveVars))
                    .headers(resolvedHeadersJson)
                    .body(VariableResolver.resolve(bodyArea.getText(), resolveVars))
                    .build();
            new Thread(() -> {
                ApiResponse response = ClientHttpExecutor.execute(
                        request.getMethod().name(), overrides.getUrl(), overrides.getHeaders(), overrides.getBody());
                Platform.runLater(() -> {
                    sendBtn.setDisable(false);
                    statusChip.setText("Status: " + response.getStatusCode()
                            + " " + getStatusText(response.getStatusCode())
                            + "  •  " + response.getDuration() + " ms"
                            + "  •  " + formatSize(response.getSizeBytes()));
                    if (response.isSuccess()) {
                        statusChip.setStyle("-fx-padding: 3 8 3 8; -fx-background-radius: 4px; -fx-background-color: -success-soft; -fx-text-fill: -success; -fx-font-size: 11px; -fx-font-weight: bold;");
                    } else {
                        statusChip.setStyle("-fx-padding: 3 8 3 8; -fx-background-radius: 4px; -fx-background-color: -danger-soft; -fx-text-fill: -danger; -fx-font-size: 11px; -fx-font-weight: bold;");
                    }
                    if (response.isBinary()) {
                        responseArea.setText("📄  Binary response ("
                                + (response.getContentType() != null ? response.getContentType() : "unknown")
                                + ")\nSize: " + response.getSizeBytes() + " bytes\n"
                                + "Suggested file name: " + response.getFileName() + "\n\n"
                                + "Click Save File above to download it.");
                        saveFileBtn.setVisible(true);
                        saveFileBtn.setManaged(true);
                        saveFileBtn.setOnAction(saveEvent -> {
                            FileChooser fileChooser = new FileChooser();
                            fileChooser.setTitle("Save Response");
                            String suggested = response.getFileName() != null ? response.getFileName() : "response.bin";
                            fileChooser.setInitialFileName(suggested);
                            File file = fileChooser.showSaveDialog(dialog.getDialogPane().getScene().getWindow());
                            if (file == null) {
                                return;
                            }
                            try {
                                byte[] bytes = Base64.getDecoder().decode(response.getResponse());
                                java.nio.file.Files.write(file.toPath(), bytes);
                                statusChip.setText("Saved " + bytes.length + " bytes to " + file.getName());
                            } catch (Exception saveEx) {
                                statusChip.setText("Save failed: " + saveEx.getMessage());
                            }
                        });
                    } else {
                        saveFileBtn.setVisible(false);
                        saveFileBtn.setManaged(false);
                        responseArea.setText(response.getResponse());
                    }

                    String script = request.getTestsScript();
                    if (script != null && !script.isBlank() && !environments.isEmpty() && envCombo.getValue() != null) {
                        EnvironmentResponse selectedEnv = envByName.get(envCombo.getValue());
                        if (selectedEnv != null) {
                            Map<String, String> current = new LinkedHashMap<>(
                                    selectedEnv.getVariables() != null ? selectedEnv.getVariables() : Map.of());
                            ScriptRunner.Result result = ScriptRunner.run(
                                    script, response.getResponse(), response.getStatusCode(), current);
                            if (!result.setVariables.isEmpty() || !result.unsetVariables.isEmpty()) {
                                Map<String, String> merged = new LinkedHashMap<>(current);
                                merged.putAll(result.setVariables);
                                result.unsetVariables.forEach(merged::remove);
                                selectedEnv.setVariables(merged);
                                scriptResultLabel.setText("✓ Test script updated environment: "
                                        + String.join(", ", result.setVariables.keySet()));
                                scriptResultLabel.setVisible(true);
                                scriptResultLabel.setManaged(true);
                                new Thread(() -> EnvironmentService.updateEnvironmentVariables(
                                        selectedEnv.getId(), merged)).start();
                            }
                        }
                    }
                });
            }).start();
        });

        dialog.showAndWait();
    }

    /** Lets the project owner configure the app's own outgoing SMTP
     * server — used for email verification codes and team invitations.
     * The backend rejects this for non-admins, so nothing bad happens if
     * a non-admin somehow opens it; they'll just see a clear error. */
    private void openMailSettingsDialog(Window owner) {
        handleSettings();
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
    private void handleCheckForUpdates() {
        AppUpdateDialog.check(urlField.getScene().getWindow(), true);
    }

    @FXML
    private void handleAbout() {
        AlertUtils.showInfo("Thundercall v" + AppUpdateService.CURRENT_VERSION + "\nA professional API client.\nBackend: Spring Boot  |  Frontend: JavaFX");
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

    @FXML
    private void handleShowTeamWorkspaces() {
        showSidebarPane(teamWorkspacesPane);
        loadTeamWorkspaces();
    }

    @FXML
    private void handleRefreshTeamWorkspaces() {
        loadTeamWorkspaces();
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
        if (teamWorkspacesPane != null) {
            teamWorkspacesPane.setVisible(pane == teamWorkspacesPane);
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
        MenuItem editItem = new MenuItem("Edit Environment");
        MenuItem renameItem = new MenuItem("Rename");
        MenuItem duplicateItem = new MenuItem("Duplicate");
        MenuItem exportItem = new MenuItem("Export as JSON");
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.getStyleClass().add("danger-menu-item");
        renameItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+E"));
        duplicateItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+D"));

        editItem.setOnAction(e -> {
            EnvironmentResponse env = environmentsList.getSelectionModel().getSelectedItem();
            if (env != null) {
                openEnvironmentEditor(env);
            }
        });

        renameItem.setOnAction(e -> {
            EnvironmentResponse env = environmentsList.getSelectionModel().getSelectedItem();
            if (env == null) {
                return;
            }
            TextInputDialog dialog = new TextInputDialog(env.getName());
            dialog.setTitle("Rename Environment");
            dialog.setHeaderText("Rename \"" + env.getName() + "\"");
            dialog.setContentText("New name:");
            initAndStyleDialog(dialog);
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

        menu.getItems().addAll(editItem, new SeparatorMenuItem(), renameItem, duplicateItem, new SeparatorMenuItem(), exportItem,
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

    // ==================== Environment Variables Table & Dialogs ====================

    private TableView<KeyValuePair> createVariablesTable(
            ObservableList<KeyValuePair> data,
            String nameColumnTitle,
            String valueColumnTitle,
            CheckBox masterCheckBox,
            CheckBox masterSelectBox,
            int[] pendingFocus) {

        TableView<KeyValuePair> table = new TableView<>();
        table.getStyleClass().add("key-value-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(data);
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setPrefHeight(200);
        table.setMinHeight(160);

        table.setRowFactory(tv -> new TableRow<>() {
            private final javafx.beans.value.ChangeListener<Boolean> selListener = (obs, oldVal, newVal) -> {
                pseudoClassStateChanged(ROW_SELECTED_PSEUDO, Boolean.TRUE.equals(newVal));
            };

            @Override
            protected void updateItem(KeyValuePair item, boolean empty) {
                KeyValuePair oldItem = getItem();
                if (oldItem != null) {
                    oldItem.selectedProperty().removeListener(selListener);
                }
                super.updateItem(item, empty);
                if (empty || item == null) {
                    pseudoClassStateChanged(ROW_SELECTED_PSEUDO, false);
                } else {
                    pseudoClassStateChanged(ROW_SELECTED_PSEUDO, item.isSelected());
                    item.selectedProperty().addListener(selListener);
                }
            }
        });

        // 1. Tick mark master checkbox
        masterCheckBox.setAlignment(Pos.CENTER);
        masterCheckBox.setMaxWidth(Double.MAX_VALUE);
        masterCheckBox.setSelected(true);
        masterCheckBox.setOnAction(e -> {
            boolean selectAll = masterCheckBox.isSelected();
            for (KeyValuePair kv : data) {
                if (!isRowEmpty(kv) && !kv.isPlaceholder()) {
                    kv.setEnabled(selectAll);
                }
            }
            table.refresh();
        });

        // 1. Tick mark column (38px)
        TableColumn<KeyValuePair, Boolean> checkCol = new TableColumn<>();
        checkCol.setGraphic(masterCheckBox);
        checkCol.getStyleClass().add("param-check-col");
        checkCol.setMinWidth(38);
        checkCol.setMaxWidth(38);
        checkCol.setPrefWidth(38);
        checkCol.setResizable(false);
        checkCol.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());
        checkCol.setCellFactory(col -> new TableCell<KeyValuePair, Boolean>() {
            private final CheckBox rowCheckBox = new CheckBox();
            private final javafx.beans.InvalidationListener rowStateListener = obs -> updateState();
            private KeyValuePair boundItem = null;

            private void updateState() {
                KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item == null || (item.isPlaceholder() && isRowEmpty(item))) {
                    rowCheckBox.setVisible(false);
                } else {
                    rowCheckBox.setVisible(true);
                    rowCheckBox.setSelected(item.isEnabled());
                }
            }

            {
                rowCheckBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(rowCheckBox);
                getStyleClass().add("param-check-cell");
                rowCheckBox.setOnAction(e -> {
                    KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setEnabled(rowCheckBox.isSelected());
                        updateVariableMasterCheckBox(data, masterCheckBox);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean val, boolean empty) {
                if (boundItem != null) {
                    boundItem.placeholderProperty().removeListener(rowStateListener);
                    boundItem.enabledProperty().removeListener(rowStateListener);
                }
                super.updateItem(val, empty);
                boundItem = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (boundItem != null) {
                    boundItem.placeholderProperty().addListener(rowStateListener);
                    boundItem.enabledProperty().addListener(rowStateListener);
                }
                updateState();
            }
        });

        // 2. Variable Name column
        TableColumn<KeyValuePair, String> varCol = new TableColumn<>(nameColumnTitle != null ? nameColumnTitle : "Variable Name");
        varCol.setMinWidth(140);
        varCol.setPrefWidth(200);
        varCol.setCellValueFactory(cellData -> cellData.getValue().keyProperty());
        varCol.setCellFactory(col -> createVariableCell(table, data, "Variable Name", 0,
                KeyValuePair::getKey, KeyValuePair::setKey, masterCheckBox, masterSelectBox, pendingFocus));

        // 3. Value column
        TableColumn<KeyValuePair, String> valCol = new TableColumn<>(valueColumnTitle != null ? valueColumnTitle : "Value");
        valCol.setMinWidth(140);
        valCol.setPrefWidth(240);
        valCol.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        valCol.setCellFactory(col -> createVariableCell(table, data, "Value", 1,
                KeyValuePair::getValue, KeyValuePair::setValue, masterCheckBox, masterSelectBox, pendingFocus));

        // 4. Select master checkbox
        masterSelectBox.setAlignment(Pos.CENTER);
        masterSelectBox.setMaxWidth(Double.MAX_VALUE);
        masterSelectBox.setOnAction(e -> {
            boolean selectAll = masterSelectBox.isSelected();
            for (KeyValuePair kv : data) {
                if (selectAll) {
                    if (!isRowEmpty(kv) && !kv.isPlaceholder()) {
                        kv.setSelected(true);
                    }
                } else {
                    kv.setSelected(false);
                }
            }
            table.getSelectionModel().clearSelection();
            table.refresh();
        });

        // 4. Select column (38px)
        TableColumn<KeyValuePair, Boolean> selectCol = new TableColumn<>();
        selectCol.setGraphic(masterSelectBox);
        selectCol.getStyleClass().add("param-select-col");
        selectCol.setMinWidth(38);
        selectCol.setMaxWidth(38);
        selectCol.setPrefWidth(38);
        selectCol.setResizable(false);
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(col -> new TableCell<KeyValuePair, Boolean>() {
            private final CheckBox rowSelectBox = new CheckBox();
            private final javafx.beans.InvalidationListener rowStateListener = obs -> updateState();
            private KeyValuePair boundItem = null;

            private void updateState() {
                KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item == null || (item.isPlaceholder() && isRowEmpty(item))) {
                    rowSelectBox.setVisible(false);
                } else {
                    rowSelectBox.setVisible(true);
                    rowSelectBox.setSelected(item.isSelected());
                }
            }

            {
                rowSelectBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(rowSelectBox);
                getStyleClass().add("param-select-cell");
                rowSelectBox.setOnAction(e -> {
                    KeyValuePair item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setSelected(rowSelectBox.isSelected());
                        updateVariableMasterSelectBox(data, masterSelectBox);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean val, boolean empty) {
                if (boundItem != null) {
                    boundItem.placeholderProperty().removeListener(rowStateListener);
                    boundItem.selectedProperty().removeListener(rowStateListener);
                }
                super.updateItem(val, empty);
                boundItem = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (boundItem != null) {
                    boundItem.placeholderProperty().addListener(rowStateListener);
                    boundItem.selectedProperty().addListener(rowStateListener);
                }
                updateState();
            }
        });

        table.getColumns().addAll(checkCol, varCol, valCol, selectCol);

        // Context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem addItem = new MenuItem("Add Variable");
        MenuItem deleteItem = new MenuItem("Delete Variable");
        addItem.setOnAction(e -> addVariableRow(table, data, pendingFocus));
        deleteItem.setOnAction(e -> removeSelectedVariables(table, data, masterCheckBox, masterSelectBox));
        contextMenu.getItems().addAll(addItem, deleteItem);
        table.setContextMenu(contextMenu);

        ensureVariablePlaceholderRow(data);
        updateVariableMasterCheckBox(data, masterCheckBox);
        updateVariableMasterSelectBox(data, masterSelectBox);

        return table;
    }

    private TableCell<KeyValuePair, String> createVariableCell(
            TableView<KeyValuePair> table,
            ObservableList<KeyValuePair> data,
            String placeholder,
            int colIndex,
            Function<KeyValuePair, String> getter,
            BiConsumer<KeyValuePair, String> setter,
            CheckBox masterCheckBox,
            CheckBox masterSelectBox,
            int[] pendingFocus) {

        return new TableCell<KeyValuePair, String>() {
            private final TextField textField = new TextField();
            private boolean updating = false;

            {
                textField.setPromptText(placeholder);
                textField.getStyleClass().add("param-cell-field");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(textField);
                getStyleClass().add("param-table-cell");

                setOnMouseClicked(e -> textField.requestFocus());

                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) {
                        return;
                    }
                    KeyValuePair row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row == null) {
                        return;
                    }
                    String safeVal = newVal != null ? newVal : "";
                    setter.accept(row, safeVal);

                    if (!isRowEmpty(row)) {
                        row.setPlaceholder(false);
                        if (!row.isEnabled()) {
                            row.setEnabled(true);
                        }
                        ensureVariablePlaceholderRow(data);
                        updateVariableMasterCheckBox(data, masterCheckBox);
                        updateVariableMasterSelectBox(data, masterSelectBox);
                    }
                });

                textField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                    if (e.isConsumed()) {
                        return;
                    }
                    if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                        e.consume();
                        int rowIndex = getIndex();
                        if (colIndex == 0) {
                            focusVariableCell(table, rowIndex, 2);
                        } else {
                            int nextRow = rowIndex + 1;
                            if (nextRow < data.size()) {
                                focusVariableCell(table, nextRow, 1);
                            }
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                KeyValuePair row = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (empty || row == null) {
                    updating = true;
                    try {
                        textField.setText("");
                        textField.setVisible(false);
                    } finally {
                        updating = false;
                    }
                } else {
                    textField.setVisible(true);
                    String current = getter.apply(row);
                    String safeCurrent = current != null ? current : "";
                    if (!safeCurrent.equals(textField.getText())) {
                        updating = true;
                        try {
                            textField.setText(safeCurrent);
                        } finally {
                            updating = false;
                        }
                    }
                    if (!row.isEnabled()) {
                        textField.setStyle("-fx-text-fill: -text-muted; -fx-opacity: 0.6;");
                    } else {
                        textField.setStyle("");
                    }

                    if (colIndex == 0 && pendingFocus[0] == getIndex()) {
                        pendingFocus[0] = -1;
                        Platform.runLater(() -> {
                            textField.requestFocus();
                            textField.selectAll();
                        });
                    }
                }
            }
        };
    }

    private void focusVariableCell(TableView<KeyValuePair> table, int rowIdx, int colIdx) {
        Platform.runLater(() -> {
            table.scrollTo(rowIdx);
            if (colIdx >= 0 && colIdx < table.getColumns().size()) {
                table.getFocusModel().focus(rowIdx, table.getColumns().get(colIdx));
            }
            Platform.runLater(() -> {
                if (colIdx >= 0 && colIdx < table.getColumns().size()) {
                    var targetCol = table.getColumns().get(colIdx);
                    for (Node n : table.lookupAll(".param-table-cell")) {
                        if (n instanceof TableCell<?, ?> tc && tc.getTableColumn() == targetCol && tc.getIndex() == rowIdx) {
                            if (tc.getGraphic() instanceof TextField tf) {
                                tf.requestFocus();
                                tf.selectAll();
                                break;
                            }
                        }
                    }
                }
            });
        });
    }

    private void addVariableRow(TableView<KeyValuePair> table, ObservableList<KeyValuePair> data, int[] pendingFocus) {
        KeyValuePair newRow = new KeyValuePair(true, "", "", "", false);
        newRow.setSelected(false);

        int targetIndex;
        if (!data.isEmpty() && data.get(data.size() - 1).isPlaceholder()) {
            targetIndex = data.size() - 1;
            data.add(targetIndex, newRow);
        } else {
            data.add(newRow);
            targetIndex = data.size() - 1;
        }

        ensureVariablePlaceholderRow(data);
        pendingFocus[0] = targetIndex;
        table.getSelectionModel().clearSelection();
        focusVariableCell(table, targetIndex, 1);
    }

    private void removeSelectedVariables(TableView<KeyValuePair> table, ObservableList<KeyValuePair> data,
                                         CheckBox masterCheckBox, CheckBox masterSelectBox) {
        List<KeyValuePair> toDelete = data.stream()
                .filter(kv -> kv.isSelected() && !isRowEmpty(kv) && !kv.isPlaceholder())
                .collect(Collectors.toList());

        if (toDelete.isEmpty()) {
            KeyValuePair active = table.getSelectionModel().getSelectedItem();
            if (active != null && !isRowEmpty(active) && !active.isPlaceholder()) {
                toDelete.add(active);
            }
        }

        if (!toDelete.isEmpty()) {
            data.removeAll(toDelete);
            ensureVariablePlaceholderRow(data);
            updateVariableMasterCheckBox(data, masterCheckBox);
            updateVariableMasterSelectBox(data, masterSelectBox);
            table.refresh();
        }
    }

    private void ensureVariablePlaceholderRow(ObservableList<KeyValuePair> data) {
        if (data.isEmpty()) {
            data.add(new KeyValuePair(true, "", "", "", true));
            return;
        }
        while (data.size() > 1) {
            KeyValuePair last = data.get(data.size() - 1);
            KeyValuePair secondLast = data.get(data.size() - 2);
            if (last.isPlaceholder() && isRowEmpty(last) && secondLast.isPlaceholder() && isRowEmpty(secondLast)) {
                data.remove(data.size() - 1);
            } else {
                break;
            }
        }
        KeyValuePair last = data.get(data.size() - 1);
        if (!last.isPlaceholder()) {
            data.add(new KeyValuePair(true, "", "", "", true));
        }
    }

    private void updateVariableMasterCheckBox(ObservableList<KeyValuePair> data, CheckBox masterCheckBox) {
        if (masterCheckBox == null) return;
        long totalActive = data.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder()).count();
        if (totalActive == 0) {
            masterCheckBox.setSelected(true);
            masterCheckBox.setIndeterminate(false);
            return;
        }
        long enabledCount = data.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder() && kv.isEnabled()).count();
        if (enabledCount == totalActive) {
            masterCheckBox.setSelected(true);
            masterCheckBox.setIndeterminate(false);
        } else if (enabledCount == 0) {
            masterCheckBox.setSelected(false);
            masterCheckBox.setIndeterminate(false);
        } else {
            masterCheckBox.setIndeterminate(true);
        }
    }

    private void updateVariableMasterSelectBox(ObservableList<KeyValuePair> data, CheckBox masterSelectBox) {
        if (masterSelectBox == null) return;
        long totalActive = data.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder()).count();
        if (totalActive == 0) {
            masterSelectBox.setSelected(false);
            masterSelectBox.setIndeterminate(false);
            return;
        }
        long selectedCount = data.stream().filter(kv -> !isRowEmpty(kv) && !kv.isPlaceholder() && kv.isSelected()).count();
        if (selectedCount == totalActive) {
            masterSelectBox.setSelected(true);
            masterSelectBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            masterSelectBox.setSelected(false);
            masterSelectBox.setIndeterminate(false);
        } else {
            masterSelectBox.setIndeterminate(true);
        }
    }

    private HBox createVariablesButtonBar(TableView<KeyValuePair> table, ObservableList<KeyValuePair> data,
                                          CheckBox masterCheckBox, CheckBox masterSelectBox, int[] pendingFocus) {
        Button addVarButton = new Button("+ Add Variable");
        addVarButton.getStyleClass().add("add-row-button");
        addVarButton.setOnAction(e -> addVariableRow(table, data, pendingFocus));

        Button removeVarButton = new Button("Remove Selected");
        removeVarButton.getStyleClass().add("delete-row-button");
        removeVarButton.setOnAction(e -> removeSelectedVariables(table, data, masterCheckBox, masterSelectBox));

        HBox buttonBox = new HBox(8, addVarButton, removeVarButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(4, 0, 0, 0));
        return buttonBox;
    }

    /** Postman-style variable editor: key/value table with add/delete/save. */
    private void openEnvironmentEditor(EnvironmentResponse env) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Environment");

        initAndStyleDialog(dialog);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Header
        VBox headerBox = new VBox(3);
        Label titleLabel = new Label("Edit Environment");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        Label subtitleLabel = new Label("Variables of \"" + env.getName() + "\"");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Name field
        Label nameLabel = new Label("Environment Name *");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-primary;");
        TextField nameField = new TextField(env.getName());
        nameField.setPromptText("Environment name");
        nameField.setStyle("-fx-background-color: -bg-elevated; -fx-border-color: -border-color; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 7 10; -fx-text-fill: -text-primary;");

        VBox nameBox = new VBox(5, nameLabel, nameField);

        // Description area
        Label descriptionLabel = new Label("Description (optional)");
        descriptionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-primary;");
        TextArea descriptionArea = new TextArea(env.getDescription() != null ? env.getDescription() : "");
        descriptionArea.setPromptText("What is this environment for?");
        descriptionArea.setPrefRowCount(2);
        descriptionArea.setWrapText(true);
        descriptionArea.setStyle("-fx-background-color: -bg-elevated; -fx-border-color: -border-subtle; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12px; -fx-text-fill: -text-primary;");

        VBox descBox = new VBox(5, descriptionLabel, descriptionArea);

        // Variables
        Label variablesLabel = new Label("Variables");
        variablesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-primary;");

        ObservableList<KeyValuePair> data = FXCollections.observableArrayList();
        if (env.getVariables() != null) {
            env.getVariables().forEach((k, v) -> data.add(new KeyValuePair(true, k, v != null ? v : "", "", false)));
        }

        CheckBox masterCheckBox = new CheckBox();
        CheckBox masterSelectBox = new CheckBox();
        int[] pendingFocus = new int[]{-1};

        TableView<KeyValuePair> table = createVariablesTable(
                data, "Variable", "Value", masterCheckBox, masterSelectBox, pendingFocus);

        HBox buttonBox = createVariablesButtonBar(table, data, masterCheckBox, masterSelectBox, pendingFocus);

        VBox varBox = new VBox(6, variablesLabel, table, buttonBox);
        VBox.setVgrow(varBox, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox formContent = new VBox(12, headerBox, nameBox, descBox, varBox);
        formContent.setPadding(new Insets(16, 20, 16, 20));
        formContent.setPrefWidth(580);
        formContent.setMinWidth(560);

        ScrollPane scrollPane = new ScrollPane(formContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(620);
        scrollPane.setPrefViewportHeight(460);
        scrollPane.setMinWidth(580);
        scrollPane.setMinHeight(380);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefSize(640, 560);
        dialog.getDialogPane().setMinSize(600, 480);
        dialog.setResizable(true);

        dialog.setOnShown(e -> {
            Window window = dialog.getDialogPane().getScene().getWindow();
            if (window instanceof Stage stage) {
                stage.setMinWidth(600);
                stage.setMinHeight(480);
                stage.setWidth(640);
                stage.setHeight(560);
                stage.centerOnScreen();
            }
            Platform.runLater(nameField::requestFocus);
        });

        Node saveBtn = dialog.getDialogPane().lookupButton(saveButtonType);
        if (saveBtn instanceof Button btn) {
            btn.getStyleClass().add("primary-button");
            btn.setDisable(nameField.getText() == null || nameField.getText().trim().isEmpty());
            nameField.textProperty().addListener((obs, oldV, newV) -> {
                btn.setDisable(newV == null || newV.trim().isEmpty());
            });
        }
        Node cancelBtn = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn instanceof Button btn) {
            btn.getStyleClass().add("secondary-button");
        }

        dialog.showAndWait().ifPresent(button -> {
            if (button == saveButtonType) {
                Map<String, String> variables = new LinkedHashMap<>();
                for (KeyValuePair kv : data) {
                    if (kv.isEnabled() && !kv.isPlaceholder() && kv.getKey() != null && !kv.getKey().isBlank()) {
                        variables.put(kv.getKey().trim(), kv.getValue() == null ? "" : kv.getValue());
                    }
                }

                String updatedName = nameField.getText() != null && !nameField.getText().isBlank()
                        ? nameField.getText().trim() : env.getName();
                String updatedDesc = descriptionArea.getText() != null
                        ? descriptionArea.getText().trim() : "";

                new Thread(() -> {
                    Optional<EnvironmentResponse> updated =
                            EnvironmentService.updateEnvironment(env.getId(), updatedName, updatedDesc, variables, env.getIsActive());
                    Platform.runLater(() -> {
                        if (updated.isPresent()) {
                            updateStatus("Environment saved: " + updatedName);
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
        // Which collection this (saved) request lives in — powers the
        // Collection scope in "Set as variable" and its precedence in
        // currentEnvironmentVariables(). Null for an unsaved/new request.
        Long collectionId;
        String method = "GET";
        String url = "";
        String body = "";
        String bodyType = "None";
        String graphqlQuery = "";
        String graphqlVariables = "";
        String preScript = "";
        String testsScript = "";
        String authType = "No Auth";
        String authToken = "";
        String authUsername = "";
        String authPassword = "";
        List<KeyValuePair> params = new ArrayList<>();
        List<KeyValuePair> headers = new ArrayList<>();
        List<KeyValuePair> formData = new ArrayList<>();
        List<KeyValuePair> urlEncoded = new ArrayList<>();
        String socketIoNamespace = "";
        String socketIoEventName = "";
        String socketIoMessageBody = "";
        List<SocketIoEventItem> socketIoEvents = new ArrayList<>();
        // Postman-style unsaved indicator (the small dot on the tab)
        boolean dirty = false;
        // FIX: the response viewer used to be one shared set of fields
        // across every tab, so switching tabs could show a DIFFERENT
        // tab's response — each tab now keeps its own last response.
        ApiResponse lastApiResponse;
        Map<String, String> lastResponseHeaders;

        /** Postman's "Duplicate Tab": an independent, unsaved copy of this
         * tab's editor contents — never linked back to the same saved
         * request, so editing/saving the copy can't clobber the original. */
        RequestTabState copyForDuplicate() {
            RequestTabState copy = new RequestTabState();
            copy.name = this.name;
            copy.requestId = null;
            copy.collectionId = this.collectionId;
            copy.method = this.method;
            copy.url = this.url;
            copy.body = this.body;
            copy.bodyType = this.bodyType;
            copy.graphqlQuery = this.graphqlQuery;
            copy.graphqlVariables = this.graphqlVariables;
            copy.preScript = this.preScript;
            copy.testsScript = this.testsScript;
            copy.authType = this.authType;
            copy.authToken = this.authToken;
            copy.authUsername = this.authUsername;
            copy.authPassword = this.authPassword;
            copy.params = new ArrayList<>();
            for (KeyValuePair kv : this.params) {
                copy.params.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription()));
            }
            copy.headers = new ArrayList<>();
            for (KeyValuePair kv : this.headers) {
                copy.headers.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), kv.isPlaceholder()));
            }
            copy.formData = new ArrayList<>();
            for (KeyValuePair kv : this.formData) {
                KeyValuePair item = new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), kv.isPlaceholder());
                item.setType(kv.getType());
                item.setFilePath(kv.getFilePath());
                item.setFileName(kv.getFileName());
                copy.formData.add(item);
            }
            copy.urlEncoded = new ArrayList<>();
            for (KeyValuePair kv : this.urlEncoded) {
                copy.urlEncoded.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), kv.isPlaceholder()));
            }
            copy.socketIoNamespace = this.socketIoNamespace;
            copy.socketIoEventName = this.socketIoEventName;
            copy.socketIoMessageBody = this.socketIoMessageBody;
            copy.socketIoEvents = new ArrayList<>();
            for (SocketIoEventItem item : this.socketIoEvents) {
                copy.socketIoEvents.add(new SocketIoEventItem(item.getEventName(), item.isListen(), item.getDescription()));
            }
            copy.dirty = true; // an unsaved copy always starts dirty
            return copy;
        }
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
                        syncCollectionsTreeSelectionToActiveTab(newTab);
                    }
                    scheduleSessionSave();
                });

        requestTabPane.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) c -> {
            if (!restoringSession) {
                scheduleSessionSave();
            }
        });

        TabDragReorderHelper.install(requestTabPane, isMoving -> switchingTabs = isMoving);

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
                scheduleSessionSave();
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
        if (socketIoNamespaceField != null) {
            socketIoNamespaceField.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        }
        if (socketIoEventNameField != null) {
            socketIoEventNameField.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        }
        if (socketIoMessageComposeArea != null) {
            socketIoMessageComposeArea.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        }
        socketIoEventsData.addListener((javafx.collections.ListChangeListener<SocketIoEventItem>) c -> onEditorFieldChanged());
        paramsData.addListener((javafx.collections.ListChangeListener<KeyValuePair>) c -> {
            onEditorFieldChanged();
            if (!syncingUrlAndParams && !restoringTabState) {
                syncParamsToUrl();
            }
        });
        headersData.addListener((javafx.collections.ListChangeListener<KeyValuePair>) c -> onEditorFieldChanged());
        formData.addListener((javafx.collections.ListChangeListener<KeyValuePair>) c -> onEditorFieldChanged());
        urlEncodedData.addListener((javafx.collections.ListChangeListener<KeyValuePair>) c -> onEditorFieldChanged());
        if (graphqlQueryArea != null) {
            graphqlQueryArea.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        }
        if (graphqlVariablesArea != null) {
            graphqlVariablesArea.textProperty().addListener((o, ov, nv) -> onEditorFieldChanged());
        }
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
            // Keep existing rows (and their Description / Enabled state)
            Map<String, KeyValuePair> existingByKey = new LinkedHashMap<>();
            List<KeyValuePair> disabledRows = new ArrayList<>();
            for (KeyValuePair kv : paramsData) {
                if (kv.getKey() != null && !kv.getKey().isEmpty()) {
                    if (kv.isEnabled()) {
                        existingByKey.put(kv.getKey(), kv);
                    } else {
                        disabledRows.add(kv);
                    }
                }
            }
            List<KeyValuePair> rebuilt = new ArrayList<>();
            for (Map.Entry<String, String> entry : parsed.entrySet()) {
                KeyValuePair existing = existingByKey.get(entry.getKey());
                if (existing != null) {
                    existing.setValue(entry.getValue());
                    rebuilt.add(existing);
                } else {
                    rebuilt.add(new KeyValuePair(true, entry.getKey(), entry.getValue(), ""));
                }
            }
            // Preserve disabled rows in the table
            for (KeyValuePair disabled : disabledRows) {
                if (!parsed.containsKey(disabled.getKey())) {
                    rebuilt.add(disabled);
                }
            }
            paramsData.setAll(rebuilt);
            ensurePlaceholderRow();
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
            if (!kv.isEnabled()) {
                continue;
            }
            if (kv.getKey() == null || kv.getKey().trim().isEmpty()) {
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
                Platform.runLater(() -> syncingUrlAndParams = false);
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
        scheduleSessionSave();
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

        Tab tab = createManagedTab(state);

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
        scheduleSessionSave();
        return tab;
    }

    /** The tab currently showing in the editor, or null if there isn't one
     * (e.g. an old FXML without the tab bar). */
    private Tab activeRequestTab() {
        return requestTabPane != null ? requestTabPane.getSelectionModel().getSelectedItem() : null;
    }

    /** Builds a new Tab wired up with its state, close cleanup, and the
     * Postman-style right-click menu — shared by openRequestTab() and
     * duplicateTab() so both kinds of tab behave identically. */
    private Tab createManagedTab(RequestTabState state) {
        Tab tab = new Tab();
        tab.setClosable(true);
        tabStates.put(tab, state);
        tab.setOnClosed(e -> {
            tabStates.remove(tab);
            if (requestTabPane.getTabs().isEmpty()) {
                openRequestTab("Untitled Request", null);
            }
            scheduleSessionSave();
        });
        setupTabContextMenu(tab);
        return tab;
    }

    /** Postman's tab-bar right-click menu: New Request, Duplicate Tab,
     * Close Tab / Force Close Tab, Close Other Tabs, Close All Tabs,
     * Force Close All Tabs. */
    private void setupTabContextMenu(Tab tab) {
        ContextMenu menu = new ContextMenu();

        MenuItem newRequestItem = new MenuItem("New Request");
        newRequestItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+T"));
        newRequestItem.setOnAction(e -> openRequestTab("Untitled Request", null));

        MenuItem duplicateItem = new MenuItem("Duplicate Tab");
        duplicateItem.setOnAction(e -> duplicateTab(tab));

        MenuItem closeItem = new MenuItem("Close Tab");
        closeItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+W"));
        closeItem.setOnAction(e -> closeTabSafely(tab, false));

        MenuItem forceCloseItem = new MenuItem("Force Close Tab");
        forceCloseItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Alt+Ctrl+W"));
        forceCloseItem.setOnAction(e -> closeTabSafely(tab, true));

        MenuItem closeOthersItem = new MenuItem("Close Other Tabs");
        closeOthersItem.setOnAction(e -> closeTabsMatching(t -> t != tab, false));

        MenuItem closeAllItem = new MenuItem("Close All Tabs");
        closeAllItem.setOnAction(e -> closeTabsMatching(t -> true, false));

        MenuItem forceCloseAllItem = new MenuItem("Force Close All Tabs");
        forceCloseAllItem.setOnAction(e -> closeTabsMatching(t -> true, true));

        menu.getItems().addAll(
                newRequestItem, duplicateItem,
                new SeparatorMenuItem(),
                closeItem, forceCloseItem, closeOthersItem, closeAllItem, forceCloseAllItem);
        tab.setContextMenu(menu);
    }

    /** Postman's "Duplicate Tab" — opens an independent, unsaved copy of
     * the right-clicked tab's editor contents right next to it. */
    private void duplicateTab(Tab source) {
        if (source == null || requestTabPane == null) {
            return;
        }
        if (source == requestTabPane.getSelectionModel().getSelectedItem()) {
            captureTabState(source); // sync latest, unsaved edits before copying
        }
        RequestTabState original = tabStates.get(source);
        if (original == null) {
            return;
        }
        RequestTabState copy = original.copyForDuplicate();
        Tab newTab = createManagedTab(copy);

        switchingTabs = true;
        int index = requestTabPane.getTabs().indexOf(source);
        requestTabPane.getTabs().add(index + 1, newTab);
        requestTabPane.getSelectionModel().select(newTab);
        switchingTabs = false;

        restoreTabState(newTab);
        refreshTabGraphic(newTab);
        scheduleSessionSave();
    }

    /** Closes a single tab. Unless {@code force} is true, a tab with
     * unsaved changes asks for confirmation first. Always keeps at least
     * one tab open, matching Postman. */
    private void closeTabSafely(Tab tab, boolean force) {
        if (tab == null || requestTabPane == null) {
            return;
        }
        RequestTabState state = tabStates.get(tab);
        if (!force && state != null && state.dirty) {
            boolean confirmed = AlertUtils.showConfirmation("Unsaved Changes",
                    "\"" + state.name + "\" has unsaved changes. Close this tab anyway?");
            if (!confirmed) {
                return;
            }
        }
        tabStates.remove(tab);
        requestTabPane.getTabs().remove(tab);
        if (requestTabPane.getTabs().isEmpty()) {
            openRequestTab("Untitled Request", null);
        }
        scheduleSessionSave();
    }

    /** Closes every tab matched by {@code which} — backs "Close Other
     * Tabs", "Close All Tabs" and their Force variants. Unless
     * {@code force} is true, asks once (for the whole batch) if any
     * matched tab has unsaved changes. */
    private void closeTabsMatching(java.util.function.Predicate<Tab> which, boolean force) {
        if (requestTabPane == null) {
            return;
        }
        List<Tab> targets = new ArrayList<>();
        for (Tab t : requestTabPane.getTabs()) {
            if (which.test(t)) {
                targets.add(t);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        if (!force) {
            long dirtyCount = targets.stream()
                    .map(tabStates::get)
                    .filter(s -> s != null && s.dirty)
                    .count();
            if (dirtyCount > 0) {
                boolean confirmed = AlertUtils.showConfirmation("Unsaved Changes",
                        dirtyCount + (dirtyCount == 1 ? " tab has" : " tabs have")
                                + " unsaved changes. Close anyway?");
                if (!confirmed) {
                    return;
                }
            }
        }
        for (Tab t : targets) {
            tabStates.remove(t);
        }
        requestTabPane.getTabs().removeAll(targets);
        if (requestTabPane.getTabs().isEmpty()) {
            openRequestTab("Untitled Request", null);
        }
        scheduleSessionSave();
    }

    /** Postman-style tab label: colored method badge + name + unsaved dot. */
    private void refreshTabGraphic(Tab tab) {
        RequestTabState state = tabStates.get(tab);
        if (state == null) {
            return;
        }
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        String curMethod = state.method == null ? "GET" : state.method;
        Label methodLabel = new Label(methodBadge(curMethod));
        methodLabel.getStyleClass().addAll("tab-method-badge", "method-chip", "method-chip-" + curMethod.toLowerCase(Locale.ROOT));
        methodLabel.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(state.name);
        nameLabel.getStyleClass().add("tab-name-label");

        methodLabel.setMouseTransparent(true);
        nameLabel.setMouseTransparent(true);

        box.getChildren().addAll(methodLabel, nameLabel);
        if (state.dirty) {
            Label dot = new Label("\u25CF");
            dot.getStyleClass().add("tab-unsaved-dot");
            dot.setMouseTransparent(true);
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
        state.graphqlQuery = graphqlQueryArea != null ? graphqlQueryArea.getText() : "";
        state.graphqlVariables = graphqlVariablesArea != null ? graphqlVariablesArea.getText() : "";
        Toggle selToggle = bodyTypeGroup != null ? bodyTypeGroup.getSelectedToggle() : null;
        state.bodyType = (selToggle instanceof ToggleButton btn) ? btn.getText() : "None";
        state.preScript = preRequestScriptsArea != null ? preRequestScriptsArea.getText() : "";
        state.testsScript = postRequestScriptsArea != null ? postRequestScriptsArea.getText() : "";
        state.authType = authTypeCombo.getValue();
        state.authToken = getAuthTokenForPersistence();
        state.authUsername = usernameField.getText();
        state.authPassword = passwordField.getText();
        state.params = new ArrayList<>();
        for (KeyValuePair kv : paramsData) {
            state.params.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription()));
        }
        state.headers = new ArrayList<>();
        for (KeyValuePair kv : headersData) {
            state.headers.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), kv.isPlaceholder()));
        }
        state.formData = new ArrayList<>();
        for (KeyValuePair kv : formData) {
            KeyValuePair item = new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), kv.isPlaceholder());
            item.setType(kv.getType());
            item.setFilePath(kv.getFilePath());
            item.setFileName(kv.getFileName());
            state.formData.add(item);
        }
        state.urlEncoded = new ArrayList<>();
        for (KeyValuePair kv : urlEncodedData) {
            state.urlEncoded.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), kv.isPlaceholder()));
        }
        state.socketIoNamespace = socketIoNamespaceField != null ? socketIoNamespaceField.getText() : "";
        state.socketIoEventName = socketIoEventNameField != null ? socketIoEventNameField.getText() : "";
        state.socketIoMessageBody = socketIoMessageComposeArea != null ? socketIoMessageComposeArea.getText() : "";
        state.socketIoEvents = new ArrayList<>();
        for (SocketIoEventItem item : socketIoEventsData) {
            state.socketIoEvents.add(new SocketIoEventItem(item.getEventName(), item.isListen(), item.getDescription()));
        }

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
            String stateUrl = state.url;
            if (stateUrl == null || stateUrl.isEmpty()
                    || "https://api.example.com/endpoint".equalsIgnoreCase(stateUrl.trim())
                    || "Enter URL or paste text".equalsIgnoreCase(stateUrl.trim())) {
                urlField.replaceText("");
            } else {
                urlField.replaceText(stateUrl);
            }
            bodyTextArea.replaceText(state.body == null ? "" : state.body);
            if (bodyCommentHighlighter != null) {
                bodyCommentHighlighter.refresh();
            }
            if (graphqlQueryArea != null) {
                graphqlQueryArea.replaceText(state.graphqlQuery != null ? state.graphqlQuery : "");
            }
            if (graphqlVariablesArea != null) {
                graphqlVariablesArea.replaceText(state.graphqlVariables != null ? state.graphqlVariables : "");
            }
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
            paramsData.clear();
            if (state.params != null && !state.params.isEmpty()) {
                for (KeyValuePair kv : state.params) {
                    paramsData.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription()));
                }
            }
            ensurePlaceholderRow();
            headersData.clear();
            if (state.headers != null && !state.headers.isEmpty()) {
                for (KeyValuePair kv : state.headers) {
                    headersData.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), kv.isPlaceholder()));
                }
            }
            ensureHeadersPlaceholderRow();
            updateHeadersMasterCheckBox(headersMasterCheckBox);
            updateHeadersMasterSelectBox(headersMasterSelectBox);

            formData.clear();
            if (state.formData != null && !state.formData.isEmpty()) {
                for (KeyValuePair kv : state.formData) {
                    KeyValuePair item = new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), kv.isPlaceholder());
                    item.setType(kv.getType());
                    item.setFilePath(kv.getFilePath());
                    item.setFileName(kv.getFileName());
                    formData.add(item);
                }
            }
            ensureFormDataPlaceholderRow();
            updateFormDataMasterCheckBox(formDataMasterCheckBox);
            updateFormDataMasterSelectBox(formDataMasterSelectBox);

            urlEncodedData.clear();
            if (state.urlEncoded != null && !state.urlEncoded.isEmpty()) {
                for (KeyValuePair kv : state.urlEncoded) {
                    urlEncodedData.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), kv.isPlaceholder()));
                }
            }
            ensureUrlEncodedPlaceholderRow();
            updateUrlEncodedMasterCheckBox(urlEncodedMasterCheckBox);
            updateUrlEncodedMasterSelectBox(urlEncodedMasterSelectBox);

            if (socketIoNamespaceField != null) {
                socketIoNamespaceField.setText(state.socketIoNamespace != null ? state.socketIoNamespace : "");
            }
            if (socketIoEventNameField != null) {
                socketIoEventNameField.setText(state.socketIoEventName != null ? state.socketIoEventName : "");
            }
            if (socketIoMessageComposeArea != null) {
                socketIoMessageComposeArea.setText(state.socketIoMessageBody != null ? state.socketIoMessageBody : "");
            }
            socketIoEventsData.clear();
            if (state.socketIoEvents != null && !state.socketIoEvents.isEmpty()) {
                for (SocketIoEventItem item : state.socketIoEvents) {
                    socketIoEventsData.add(new SocketIoEventItem(item.getEventName(), item.isListen(), item.getDescription()));
                }
            } else {
                socketIoEventsData.add(new SocketIoEventItem("", true, ""));
            }
            String bType = state.bodyType;
            if (bType == null || bType.isBlank()) {
                bType = (state.body != null && !state.body.isBlank()) ? "Raw" : "None";
            }
            if (bodyTypeGroup != null) {
                for (Toggle toggle : bodyTypeGroup.getToggles()) {
                    if (toggle instanceof ToggleButton tb && tb.getText().equalsIgnoreCase(bType)) {
                        tb.setSelected(true);
                        break;
                    }
                }
            }
            updateBodyViewsVisibility(bType);

            updateRequestBuilderTabs(state.method);
            updateResponseTabs(state.method);
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
        ensureCollectionVariablesLoaded(state.collectionId);
        ensureCommentThreadsLoaded(state.requestId);
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

    // ==================== Postman-style session persistence ====================

    private void setupSessionPersistence() {
        sessionSaveDebounce.setOnFinished(e -> saveCurrentSession());
        currentSession = SessionStateManager.loadSession(TokenManager.getUserId());
        restoreSessionTabs();

        Platform.runLater(() -> {
            Stage stage = Main.getPrimaryStage();
            if (stage != null) {
                javafx.event.EventHandler<javafx.stage.WindowEvent> prev = stage.getOnCloseRequest();
                stage.setOnCloseRequest(e -> {
                    saveCurrentSession();
                    if (prev != null) {
                        prev.handle(e);
                    }
                });
            }
        });
    }

    private void restoreSessionTabs() {
        if (requestTabPane == null) {
            return;
        }
        restoringSession = true;
        try {
            if (currentSession == null) {
                currentSession = SessionStateManager.loadSession(TokenManager.getUserId());
            }
            List<TabStateDto> tabDtos = (currentSession != null) ? currentSession.getTabs() : null;

            if (tabDtos != null && !tabDtos.isEmpty()) {
                requestTabPane.getTabs().clear();
                tabStates.clear();

                for (TabStateDto dto : tabDtos) {
                    RequestTabState state = fromTabDto(dto);
                    Tab tab = createManagedTab(state);
                    refreshTabGraphic(tab);
                    requestTabPane.getTabs().add(tab);
                }

                int targetIndex = currentSession.getActiveTabIndex();
                if (targetIndex < 0 || targetIndex >= requestTabPane.getTabs().size()) {
                    targetIndex = 0;
                }
                Tab activeTab = requestTabPane.getTabs().get(targetIndex);
                requestTabPane.getSelectionModel().select(activeTab);
                restoreTabState(activeTab);
                refreshTabGraphic(activeTab);
            } else {
                if (requestTabPane.getTabs().isEmpty()) {
                    openRequestTab("Untitled Request", null);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to restore session tabs: " + e.getMessage());
            if (requestTabPane.getTabs().isEmpty()) {
                openRequestTab("Untitled Request", null);
            }
        } finally {
            restoringSession = false;
        }
    }

    private void saveCurrentSession() {
        if (restoringSession || requestTabPane == null) {
            return;
        }
        try {
            Tab active = activeRequestTab();
            if (active != null && tabStates.containsKey(active)) {
                captureTabState(active);
            }
            SessionData session = new SessionData();

            // 1. Tabs
            List<TabStateDto> tabDtos = new ArrayList<>();
            for (Tab tab : requestTabPane.getTabs()) {
                RequestTabState state = tabStates.get(tab);
                if (state != null) {
                    tabDtos.add(toTabDto(state));
                }
            }
            session.setTabs(tabDtos);
            int activeIdx = requestTabPane.getSelectionModel().getSelectedIndex();
            session.setActiveTabIndex(Math.max(0, activeIdx));

            // 2. Environment
            if (environmentCombo != null && environmentCombo.getValue() != null) {
                session.setSelectedEnvironment(environmentCombo.getValue());
            }

            // 3. Collections tree expanded keys
            Set<String> expandedKeys = new HashSet<>();
            captureExpandedKeys(fullCollectionsRoot != null ? fullCollectionsRoot : collectionsTree.getRoot(), expandedKeys);
            session.setExpandedTreeKeys(expandedKeys);

            // 4. Collections tree selected item
            if (collectionsTree != null && collectionsTree.getSelectionModel().getSelectedItem() != null) {
                TreeItem<String> sel = collectionsTree.getSelectionModel().getSelectedItem();
                Long reqId = requestIdMap.get(sel);
                if (reqId != null) {
                    session.setSelectedTreeKey("r:" + reqId);
                } else {
                    Long folderId = folderIdMap.get(sel);
                    if (folderId != null) {
                        session.setSelectedTreeKey("f:" + folderId);
                    } else {
                        Long colId = collectionIdMap.get(sel);
                        if (colId != null) {
                            session.setSelectedTreeKey("c:" + colId);
                        }
                    }
                }
            }

            // 5. Active Workspace
            if (WorkspaceManager.getCurrentWorkspace() != null) {
                session.setActiveWorkspaceId(WorkspaceManager.getCurrentWorkspace().getId());
            }

            this.currentSession = session;
            SessionStateManager.saveSession(TokenManager.getUserId(), session);
        } catch (Exception e) {
            logger.warning("Error saving current session: " + e.getMessage());
        }
    }

    private void scheduleSessionSave() {
        if (restoringSession) {
            return;
        }
        Platform.runLater(() -> {
            sessionSaveDebounce.stop();
            sessionSaveDebounce.playFromStart();
        });
    }

    private TabStateDto toTabDto(RequestTabState state) {
        TabStateDto dto = new TabStateDto();
        dto.setName(state.name);
        dto.setRequestId(state.requestId);
        dto.setCollectionId(state.collectionId);
        dto.setMethod(state.method);
        dto.setUrl(state.url);
        dto.setBody(state.body);
        dto.setBodyType(state.bodyType);
        dto.setGraphqlQuery(state.graphqlQuery);
        dto.setGraphqlVariables(state.graphqlVariables);
        dto.setPreScript(state.preScript);
        dto.setTestsScript(state.testsScript);
        dto.setAuthType(state.authType);
        dto.setAuthToken(state.authToken);
        dto.setAuthUsername(state.authUsername);
        dto.setAuthPassword(state.authPassword);
        dto.setDirty(state.dirty);

        if (state.params != null) {
            for (KeyValuePair kv : state.params) {
                if (!kv.isPlaceholder()) {
                    dto.getParams().add(new KeyValuePairDto(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), false, "Text", "", ""));
                }
            }
        }
        if (state.headers != null) {
            for (KeyValuePair kv : state.headers) {
                if (!kv.isPlaceholder()) {
                    dto.getHeaders().add(new KeyValuePairDto(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), false, "Text", "", ""));
                }
            }
        }
        if (state.formData != null) {
            for (KeyValuePair kv : state.formData) {
                if (!kv.isPlaceholder()) {
                    dto.getFormData().add(new KeyValuePairDto(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), false, kv.getType(), kv.getFilePath(), kv.getFileName()));
                }
            }
        }
        if (state.urlEncoded != null) {
            for (KeyValuePair kv : state.urlEncoded) {
                if (!kv.isPlaceholder()) {
                    dto.getUrlEncoded().add(new KeyValuePairDto(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), false, "Text", "", ""));
                }
            }
        }
        dto.setSocketIoNamespace(state.socketIoNamespace);
        dto.setSocketIoEventName(state.socketIoEventName);
        dto.setSocketIoMessageBody(state.socketIoMessageBody);
        if (state.socketIoEvents != null) {
            for (SocketIoEventItem item : state.socketIoEvents) {
                dto.getSocketIoEvents().add(new SocketIoEventDto(item.getEventName(), item.isListen(), item.getDescription()));
            }
        }
        return dto;
    }

    private RequestTabState fromTabDto(TabStateDto dto) {
        RequestTabState state = new RequestTabState();
        state.name = dto.getName() != null ? dto.getName() : "Untitled Request";
        state.requestId = dto.getRequestId();
        state.collectionId = dto.getCollectionId();
        state.method = dto.getMethod() != null ? dto.getMethod() : "GET";
        state.url = dto.getUrl() != null ? dto.getUrl() : "";
        state.body = dto.getBody() != null ? dto.getBody() : "";
        state.bodyType = dto.getBodyType() != null ? dto.getBodyType() : "None";
        state.graphqlQuery = dto.getGraphqlQuery() != null ? dto.getGraphqlQuery() : "";
        state.graphqlVariables = dto.getGraphqlVariables() != null ? dto.getGraphqlVariables() : "";
        state.preScript = dto.getPreScript() != null ? dto.getPreScript() : "";
        state.testsScript = dto.getTestsScript() != null ? dto.getTestsScript() : "";
        state.authType = dto.getAuthType() != null ? dto.getAuthType() : "No Auth";
        state.authToken = dto.getAuthToken() != null ? dto.getAuthToken() : "";
        state.authUsername = dto.getAuthUsername() != null ? dto.getAuthUsername() : "";
        state.authPassword = dto.getAuthPassword() != null ? dto.getAuthPassword() : "";
        state.dirty = dto.isDirty();

        state.params = new ArrayList<>();
        if (dto.getParams() != null) {
            for (KeyValuePairDto kv : dto.getParams()) {
                state.params.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription()));
            }
        }
        state.headers = new ArrayList<>();
        if (dto.getHeaders() != null) {
            for (KeyValuePairDto kv : dto.getHeaders()) {
                state.headers.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), false));
            }
        }
        state.formData = new ArrayList<>();
        if (dto.getFormData() != null) {
            for (KeyValuePairDto kv : dto.getFormData()) {
                KeyValuePair item = new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), false);
                item.setType(kv.getType() != null ? kv.getType() : "Text");
                item.setFilePath(kv.getFilePath() != null ? kv.getFilePath() : "");
                item.setFileName(kv.getFileName() != null ? kv.getFileName() : "");
                state.formData.add(item);
            }
        }
        state.urlEncoded = new ArrayList<>();
        if (dto.getUrlEncoded() != null) {
            for (KeyValuePairDto kv : dto.getUrlEncoded()) {
                state.urlEncoded.add(new KeyValuePair(kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription(), false));
            }
        }
        state.socketIoNamespace = dto.getSocketIoNamespace() != null ? dto.getSocketIoNamespace() : "";
        state.socketIoEventName = dto.getSocketIoEventName() != null ? dto.getSocketIoEventName() : "";
        state.socketIoMessageBody = dto.getSocketIoMessageBody() != null ? dto.getSocketIoMessageBody() : "";
        state.socketIoEvents = new ArrayList<>();
        if (dto.getSocketIoEvents() != null) {
            for (SocketIoEventDto item : dto.getSocketIoEvents()) {
                state.socketIoEvents.add(new SocketIoEventItem(item.getEventName(), item.isListen(), item.getDescription()));
            }
        }
        return state;
    }

    private TreeItem<String> findTreeItemByKey(String key) {
        if (key == null) {
            return null;
        }
        if (key.startsWith("r:")) {
            try {
                Long reqId = Long.parseLong(key.substring(2));
                for (Map.Entry<TreeItem<String>, Long> entry : requestIdMap.entrySet()) {
                    if (reqId.equals(entry.getValue())) {
                        return entry.getKey();
                    }
                }
            } catch (NumberFormatException ignored) {}
        } else if (key.startsWith("f:")) {
            try {
                Long folderId = Long.parseLong(key.substring(2));
                for (Map.Entry<TreeItem<String>, Long> entry : folderIdMap.entrySet()) {
                    if (folderId.equals(entry.getValue())) {
                        return entry.getKey();
                    }
                }
            } catch (NumberFormatException ignored) {}
        } else if (key.startsWith("c:")) {
            try {
                Long colId = Long.parseLong(key.substring(2));
                for (Map.Entry<TreeItem<String>, Long> entry : collectionIdMap.entrySet()) {
                    if (colId.equals(entry.getValue())) {
                        return entry.getKey();
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private void restoreCollectionsTreeSelection(String key) {
        if (key == null || collectionsTree == null) {
            return;
        }
        TreeItem<String> item = findTreeItemByKey(key);
        if (item != null) {
            TreeItem<String> p = item.getParent();
            while (p != null) {
                p.setExpanded(true);
                p = p.getParent();
            }
            restoringTreeSelection = true;
            try {
                collectionsTree.getSelectionModel().select(item);
                int row = collectionsTree.getRow(item);
                if (row >= 0) {
                    collectionsTree.scrollTo(row);
                }
            } finally {
                restoringTreeSelection = false;
            }
        }
    }

    private void syncCollectionsTreeSelectionToActiveTab(Tab tab) {
        if (tab == null || collectionsTree == null) {
            return;
        }
        RequestTabState state = tabStates.get(tab);
        if (state != null && state.requestId != null) {
            restoreCollectionsTreeSelection("r:" + state.requestId);
        }
    }

    // ==================== Postman-style collections tree ====================

    private TreeCell<String> createPostmanTreeCell() {
        return new TreeCell<>() {
            private final Label methodLabel = new Label();
            private final Label nameLabel = new Label();
            private final Button plusBtn = new Button("+");
            private final Button moreBtn = new Button("⋯");
            private final Region spacer = new Region();
            private final HBox box = new HBox(8);

            {
                methodLabel.getStyleClass().addAll("method-chip", "tree-method-chip");
                methodLabel.setMinWidth(46);
                methodLabel.setMaxWidth(Region.USE_PREF_SIZE);
                methodLabel.setAlignment(Pos.CENTER);
                plusBtn.getStyleClass().add("tree-inline-btn");
                moreBtn.getStyleClass().add("tree-inline-btn");
                HoverTooltip.attach(plusBtn, "Add request");
                HoverTooltip.attach(moreBtn, "More actions");
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
                    // Request row: colored interactive method pill badge + name (like Postman)
                    methodLabel.setText(methodBadge(method));
                    methodLabel.getStyleClass().removeIf(s -> s.startsWith("method-") || s.startsWith("method-chip-"));
                    methodLabel.getStyleClass().addAll("method-chip", "method-chip-" + method.toLowerCase(Locale.ROOT));
                    box.getChildren().addAll(methodLabel, nameLabel, spacer, moreBtn);
                } else {
                    // Collection/folder row: icon + name + hover actions
                    Image icon =
                            (treeItem != null && collectionIdMap.containsKey(treeItem))
                                    ? collectionIconImg : folderIconImg;
                    if (icon != null) {
                        ImageView iv = new ImageView(icon);
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
                setGraphic(null);
                setText(null);
                getStyleClass().removeIf(s -> s.startsWith("method-") || s.startsWith("method-chip-"));
                if (empty || item == null) {
                    return;
                }
                if (item instanceof EnvironmentResponse env) {
                    setText("Environment  ·  " + env.getName());
                } else if (item instanceof TreeItem<?> rawItem) {
                    @SuppressWarnings("unchecked")
                    TreeItem<String> treeItem = (TreeItem<String>) rawItem;
                    String method = requestMethodMap.get(treeItem);
                    if (method != null) {
                        Label methodChip = new Label(methodBadge(method));
                        methodChip.getStyleClass().addAll("method-chip", "method-chip-" + method.toLowerCase(Locale.ROOT));
                        methodChip.setMinWidth(46);
                        methodChip.setAlignment(Pos.CENTER);
                        Label pathLabel = new Label(treePath(treeItem));
                        pathLabel.setStyle("-fx-text-fill: -text-primary; -fx-font-size: 12px;");
                        HBox row = new HBox(8, methodChip, pathLabel);
                        row.setAlignment(Pos.CENTER_LEFT);
                        setGraphic(row);
                    } else if (collectionIdMap.containsKey(treeItem)) {
                        setText("Collection  ·  " + treePath(treeItem));
                    } else {
                        setText("Folder  ·  " + treePath(treeItem));
                    }
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
        initAndStyleDialog(dialog);
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
                // 1. Cancel debounce & save this user's session cleanly before destroying state
                if (sessionSaveDebounce != null) {
                    sessionSaveDebounce.stop();
                }
                saveCurrentSession();

                // 2. Stop reporting this client as online & stop session background monitor
                stopPresenceUpdates();
                TokenManager.stopSessionMonitor();

                // 3. Clear all workspace context and active session data
                WorkspaceManager.clear();
                currentSession = null;

                // 4. Clear in-memory tabs, caches, and tree maps
                if (requestTabPane != null) {
                    requestTabPane.getTabs().clear();
                }
                tabStates.clear();
                requestIdMap.clear();
                collectionIdMap.clear();
                folderIdMap.clear();
                commentThreadsCache.clear();
                collectionVariablesCache.clear();

                // 5. Clear server session and local authentication tokens
                ApiClient.logout();
                TokenManager.clearTokens();

                // 6. Transition to login view
                Main.showLoginView();
            } catch (Exception e) {
                AlertUtils.showError("Logout failed: " + e.getMessage());
            }
        }
    }

    /**
     * Centers a dialog over the main application window and applies the active theme stylesheet.
     * Prevents dialogs from opening unanchored at (0, 0) or overflowing the display edges.
     */
    private void initAndStyleDialog(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }
        Window owner = Main.getPrimaryStage();
        if (owner == null && collectionsTree != null && collectionsTree.getScene() != null) {
            owner = collectionsTree.getScene().getWindow();
        }
        if (owner != null) {
            dialog.initOwner(owner);
        }
        ThemeManager.styleDialog(dialog.getDialogPane());
    }

    @FXML
    private void handleNewCollection() {
        if (!WorkspaceManager.canEditCurrentWorkspace()) {
            AlertUtils.showError("You have Viewer access to this workspace — Editor access is needed to create things in it.");
            return;
        }

        Dialog<CollectionRequest> dialog = new Dialog<>();
        dialog.setTitle("New Collection");
        dialog.setHeaderText("Create a new collection to organize your requests");

        initAndStyleDialog(dialog);

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        Label nameLabel = new Label("Collection Name *");
        nameLabel.setStyle("-fx-font-weight: bold;");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Users API, Auth Service, Payment Gateway");

        Label descLabel = new Label("Description (optional)");
        descLabel.setStyle("-fx-font-weight: bold;");
        TextArea descArea = new TextArea();
        descArea.setPromptText("What requests are grouped in this collection?");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        VBox content = new VBox(10, nameLabel, nameField, descLabel, descArea);
        content.setPadding(new Insets(14));
        content.setPrefWidth(440);
        content.setMinWidth(400);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(460, 280);
        dialog.getDialogPane().setMinSize(420, 240);
        dialog.setResizable(true);

        Node createBtn = dialog.getDialogPane().lookupButton(createButtonType);
        if (createBtn != null) {
            createBtn.setDisable(true);
            nameField.textProperty().addListener((obs, oldV, newV) -> {
                createBtn.setDisable(newV == null || newV.trim().isEmpty());
            });
        }

        dialog.setOnShown(e -> {
            Window window = dialog.getDialogPane().getScene().getWindow();
            if (window instanceof Stage stage) {
                stage.setMinWidth(420);
                stage.setMinHeight(240);
                stage.setWidth(460);
                stage.setHeight(280);
                stage.centerOnScreen();
            }
            Platform.runLater(nameField::requestFocus);
        });

        dialog.setResultConverter(button -> {
            if (button == createButtonType) {
                String name = nameField.getText() == null ? "" : nameField.getText().trim();
                String desc = descArea.getText() == null ? "" : descArea.getText().trim();
                return CollectionRequest.builder()
                        .name(name)
                        .description(desc)
                        .workspaceId(WorkspaceManager.hasWorkspace() ? WorkspaceManager.getCurrentWorkspace().getId() : null)
                        .build();
            }
            return null;
        });

        Optional<CollectionRequest> result = dialog.showAndWait();
        result.ifPresent(this::createNewCollection);
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
        if (!WorkspaceManager.canEditCurrentWorkspace()) {
            AlertUtils.showError("You have Viewer access to this workspace — Editor access is needed to create things in it.");
            return;
        }

        // Create a custom dialog for environment creation
        Dialog<EnvironmentCreationData> dialog = new Dialog<>();
        dialog.setTitle("Create New Environment");

        // Set owner to center the dialog over the application window and apply theme
        initAndStyleDialog(dialog);

        // Set the button types
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Header
        VBox headerBox = new VBox(3);
        Label titleLabel = new Label("Create New Environment");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        Label subtitleLabel = new Label("Create a new environment with custom variables");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Name field
        Label nameLabel = new Label("Environment Name *");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-primary;");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Development, Staging, Production");
        nameField.setStyle("-fx-background-color: -bg-elevated; -fx-border-color: -border-color; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 7 10; -fx-text-fill: -text-primary;");

        VBox nameBox = new VBox(5, nameLabel, nameField);

        // Description area
        Label descriptionLabel = new Label("Description (optional)");
        descriptionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-primary;");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("What is this environment for?");
        descriptionArea.setPrefRowCount(2);
        descriptionArea.setWrapText(true);
        descriptionArea.setStyle("-fx-background-color: -bg-elevated; -fx-border-color: -border-subtle; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12px; -fx-text-fill: -text-primary;");

        VBox descBox = new VBox(5, descriptionLabel, descriptionArea);

        // Variables table
        Label variablesLabel = new Label("Variables");
        variablesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-primary;");

        ObservableList<KeyValuePair> variablesData = FXCollections.observableArrayList();
        CheckBox masterCheckBox = new CheckBox();
        CheckBox masterSelectBox = new CheckBox();
        int[] pendingFocus = new int[]{-1};

        TableView<KeyValuePair> variablesTable = createVariablesTable(
                variablesData, "Variable Name", "Initial Value", masterCheckBox, masterSelectBox, pendingFocus);

        HBox buttonBox = createVariablesButtonBar(
                variablesTable, variablesData, masterCheckBox, masterSelectBox, pendingFocus);

        VBox varBox = new VBox(6, variablesLabel, variablesTable, buttonBox);
        VBox.setVgrow(varBox, Priority.ALWAYS);
        VBox.setVgrow(variablesTable, Priority.ALWAYS);

        VBox formContent = new VBox(12, headerBox, nameBox, descBox, varBox);
        formContent.setPadding(new Insets(16, 20, 16, 20));
        formContent.setPrefWidth(580);
        formContent.setMinWidth(560);

        // Wrap in ScrollPane so it gracefully fits any screen height without overflowing offscreen
        ScrollPane scrollPane = new ScrollPane(formContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(620);
        scrollPane.setPrefViewportHeight(460);
        scrollPane.setMinWidth(580);
        scrollPane.setMinHeight(380);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefSize(640, 560);
        dialog.getDialogPane().setMinSize(600, 480);
        dialog.setResizable(true);

        dialog.setOnShown(e -> {
            Window window = dialog.getDialogPane().getScene().getWindow();
            if (window instanceof Stage stage) {
                stage.setMinWidth(600);
                stage.setMinHeight(480);
                stage.setWidth(640);
                stage.setHeight(560);
                stage.centerOnScreen();
            }
            Platform.runLater(nameField::requestFocus);
        });

        // Validate Create button - only enable if environment name is provided
        Node createBtn = dialog.getDialogPane().lookupButton(createButtonType);
        if (createBtn instanceof Button btn) {
            btn.getStyleClass().add("primary-button");
            btn.setDisable(true);
            nameField.textProperty().addListener((obs, oldV, newV) -> {
                btn.setDisable(newV == null || newV.trim().isEmpty());
            });
        }
        Node cancelBtn = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn instanceof Button btn) {
            btn.getStyleClass().add("secondary-button");
        }

        // Convert the result to EnvironmentCreationData when create button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Map<String, String> variables = new LinkedHashMap<>();
                for (KeyValuePair kv : variablesData) {
                    if (kv.isEnabled() && !kv.isPlaceholder() && kv.getKey() != null && !kv.getKey().trim().isEmpty()) {
                        variables.put(kv.getKey().trim(), kv.getValue() != null ? kv.getValue() : "");
                    }
                }

                return new EnvironmentCreationData(
                        nameField.getText().trim(),
                        descriptionArea.getText() != null ? descriptionArea.getText().trim() : "",
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
        if (!WorkspaceManager.canEditCurrentWorkspace()) {
            AlertUtils.showError("You have Viewer access to this workspace — Editor access is needed to save changes in it.");
            return;
        }
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
                            scheduleSessionSave();
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
                                scheduleSessionSave();
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
        initAndStyleDialog(dialog);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    @FXML
    private void handleCopyResponse() {
        if (lastApiResponse != null && lastApiResponse.isBinary()) {
            AlertUtils.showInfo("This is a binary response — use the save icon to download it instead of copying text");
            return;
        }
        String response = (responseFoldManager != null && responseFoldManager.getFullText() != null && !responseFoldManager.getFullText().isEmpty())
                ? responseFoldManager.getFullText()
                : responseBodyArea.getText();
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
            String fullText = (responseFoldManager != null && responseFoldManager.getFullText() != null && !responseFoldManager.getFullText().isEmpty())
                    ? responseFoldManager.getFullText()
                    : responseBodyArea.getText();
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
            } else if (fullText.trim().startsWith("{")
                    || fullText.trim().startsWith("[")) {
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
                java.nio.file.Files.writeString(file.toPath(), fullText);
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

        // FIX: WS/SOCKETIO are markers for "use the dedicated Connect
        // button," not real HTTP methods — the backend used to guard
        // against sending these as an actual HTTP call, but now that
        // execution happens client-side (see below), that guard needs
        // to live here instead. Without it, e.g. pressing Enter in the
        // URL field while WS is selected would try to build an HTTP
        // request with method "WS", which java.net.http rejects outright
        // with an unhandled exception rather than a clear message.
        String selectedMethod = methodCombo.getValue();
        if ("WS".equals(selectedMethod) || "SOCKETIO".equals(selectedMethod)) {
            AlertUtils.showInfo("Use the Connect button for " + selectedMethod + " — it isn't a regular HTTP send.");
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

                // FIX: this used to call RequestService.executeRequest(),
                // which runs the actual HTTP call on the BACKEND server.
                // That's fine when the backend happens to run on the same
                // machine as you, but breaks the moment it's hosted
                // anywhere else — "localhost" or anything on your own
                // private network resolves on the SERVER, not on your
                // machine, so it can never reach the thing you're
                // actually trying to test. Executing directly from here
                // (matching how Postman's own desktop app behaves, and
                // exactly what the Team Spaces quick-send dialog already
                // does) fixes that regardless of where the backend lives.
                ApiResponse apiResponse = (finalFormDataFields != null && !finalFormDataFields.isEmpty())
                        ? ClientHttpExecutor.executeMultipart(method, finalUrl, apiRequest.getHeaders(), finalFormDataFields)
                        : ClientHttpExecutor.execute(method, finalUrl, apiRequest.getHeaders(), body);

                // History is still server-side bookkeeping — the call
                // already happened above; this just logs what happened,
                // same as executeRequest() always did internally.
                RequestService.recordHistory(apiRequest, apiResponse);

                return Optional.of(apiResponse);
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

    private boolean shouldListenToEvent(String eventName) {
        if (socketIoEventsData == null || socketIoEventsData.isEmpty()) {
            return true;
        }
        for (SocketIoEventItem item : socketIoEventsData) {
            if (item.getEventName() != null && !item.getEventName().trim().isEmpty()) {
                if (item.getEventName().trim().equalsIgnoreCase(eventName != null ? eventName.trim() : "")) {
                    return item.isListen();
                }
            }
        }
        return true;
    }

    private void applySocketIoEvents(List<SocketIoEventResponse> events) {
        if (events == null || events.isEmpty() || socketIoEventLog == null) {
            return;
        }
        for (SocketIoEventResponse event : events) {
            if ("in".equalsIgnoreCase(event.getDirection()) && !shouldListenToEvent(event.getEventName())) {
                continue;
            }
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
        String rawData = "";
        if (socketIoMessageComposeArea != null && !socketIoMessageComposeArea.getText().isEmpty()) {
            rawData = socketIoMessageComposeArea.getText();
        } else if (socketIoEventDataField != null && socketIoEventDataField.getText() != null) {
            rawData = socketIoEventDataField.getText();
        }
        String data = VariableResolver.resolve(rawData, vars);
        String sid = socketIoSessionId;
        new Thread(() -> {
            try {
                SocketIoService.emit(sid, eventName, data);
                Platform.runLater(() -> {
                    if (socketIoEventDataField != null) {
                        socketIoEventDataField.clear();
                    }
                });
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
        if (responseCodeAreaPane != null) {
            responseCodeAreaPane.setVisible(false);
            responseCodeAreaPane.setManaged(false);
        }
        if (responseVisualizationView != null) {
            responseVisualizationView.setVisible(false);
            responseVisualizationView.setManaged(false);
        }
        responsePreview.setVisible(true);
        responsePreview.setManaged(true);
        if (responsePreviewToggle != null) {
            responsePreviewToggle.setSelected(true);
        }
        if (responseVisualizerToggle != null) {
            responseVisualizerToggle.setSelected(false);
        }
        String bg = ThemeManager.isLight() ? "#ffffff" : "#1d1f24";
        String textColor = ThemeManager.isLight() ? "#6b6b6b" : "#9a9ea9";
        responsePreview.getEngine().loadContent(
                "<html><body style='margin:0;padding:12px;background:" + bg + ";'>"
                        + "<p style='font-family:sans-serif;color:" + textColor + ";font-size:12px;'>Rendering PDF preview…</p>"
                        + "</body></html>");

        new Thread(() -> {
            try {
                byte[] pdfBytes = Base64.getDecoder().decode(apiResponse.getResponse());
                StringBuilder html = new StringBuilder(
                        "<html><body style='margin:0;padding:12px;background:" + bg + ";text-align:center;'>");
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
                        html.append("<p style='font-family:sans-serif;color:" + textColor + ";font-size:12px;'>")
                                .append(pageCount - pagesToRender)
                                .append(" more page(s) not shown — save the file to see the rest.</p>");
                    }
                }
                html.append("</body></html>");
                String finalHtml = html.toString();
                Platform.runLater(() -> responsePreview.getEngine().loadContent(finalHtml));
            } catch (Exception e) {
                Platform.runLater(() -> responsePreview.getEngine().loadContent(
                        "<html><body style='margin:0;padding:12px;background:" + bg + ";'>"
                                + "<p style='font-family:sans-serif;color:#e57373;font-size:12px;'>"
                                + "Couldn't render a preview: " + e.getMessage() + "</p></body></html>"));
            }
        }).start();
    }

    private void clearResponsePreview() {
        if (responsePreview != null) {
            String bg = ThemeManager.isLight() ? "#ffffff" : "#1d1f24";
            responsePreview.getEngine().loadContent(
                    "<html><body style='background:" + bg + ";'></body></html>");
            responsePreview.setVisible(false);
            responsePreview.setManaged(false);
        }
        if (responseVisualizationView != null) {
            responseVisualizationView.setVisible(false);
            responseVisualizationView.setManaged(false);
        }
        if (responsePreviewToggle != null) {
            responsePreviewToggle.setSelected(false);
        }
        if (responseVisualizerToggle != null) {
            responseVisualizerToggle.setSelected(false);
        }
        if (responseCodeAreaPane != null) {
            responseCodeAreaPane.setVisible(true);
            responseCodeAreaPane.setManaged(true);
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
            String binMsg = "\uD83D\uDCC4  Binary response\n\n"
                    + "Content-Type: " + (apiResponse.getContentType() != null ? apiResponse.getContentType() : "unknown") + "\n"
                    + "Size: " + formatSize(apiResponse.getSizeBytes()) + "\n"
                    + "Suggested file name: " + apiResponse.getFileName() + "\n\n"
                    + (isPdf
                    ? "PDF preview is shown on the right. Click the save icon above "
                    + "the response to download the original file."
                    : "This isn't text, so it can't be shown here — click the save icon "
                    + "above the response to download it exactly as the server sent it.");
            if (responseFoldManager != null) {
                responseFoldManager.setFullText(binMsg);
            } else {
                responseBodyArea.replaceText(binMsg);
            }
            if (isPdf) {
                renderPdfPreview(apiResponse);
            } else {
                clearResponsePreview();
            }
        } else {
            responseBodyArea.setEditable(true);
            formatResponseBody(apiResponse.getResponse());
            clearResponsePreview();
            if (responseFormatMenu != null) {
                String ctype = apiResponse.getContentType() != null ? apiResponse.getContentType().toLowerCase(Locale.ROOT) : "";
                String body = apiResponse.getResponse() != null ? apiResponse.getResponse().trim() : "";
                if (ctype.contains("json") || body.startsWith("{") || body.startsWith("[")) {
                    responseFormatMenu.setText("{ } JSON");
                } else if (ctype.contains("xml") || body.startsWith("<xml") || body.startsWith("<?xml")) {
                    responseFormatMenu.setText("XML");
                } else if (ctype.contains("html") || body.toLowerCase(Locale.ROOT).startsWith("<!doctype html") || body.toLowerCase(Locale.ROOT).startsWith("<html")) {
                    responseFormatMenu.setText("HTML");
                } else {
                    responseFormatMenu.setText("Text");
                }
            }
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
        if (responseFoldManager != null) {
            responseFoldManager.setFullText("");
        } else {
            responseBodyArea.replaceText("");
        }
        responseHeadersData.clear();
        clearResponsePreview();
    }

    private void formatResponseBody(String responseBody) {
        try {
            if (responseBody == null || responseBody.isBlank()) {
                if (responseFoldManager != null) {
                    responseFoldManager.setFullText("");
                } else {
                    responseBodyArea.replaceText("");
                }
                return;
            }
            String trimmed = responseBody.trim();
            String formattedText = null;
            if (trimmed.startsWith("{")) {
                try {
                    JSONObject json = new JSONObject(trimmed);
                    formattedText = json.toString(2);
                } catch (Exception ignored) {
                }
            } else if (trimmed.startsWith("[")) {
                try {
                    JSONArray json = new JSONArray(trimmed);
                    formattedText = json.toString(2);
                } catch (Exception ignored) {
                }
            } else if (trimmed.startsWith("<")) {
                try {
                    JSONObject json = XML.toJSONObject(trimmed);
                } catch (Exception ignored) {
                }
            }

            String finalText = (formattedText != null) ? formattedText : responseBody;
            if (responseFoldManager != null) {
                responseFoldManager.setFullText(finalText);
            } else {
                responseBodyArea.replaceText(finalText);
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    responseBodyArea.setStyleSpans(0,
                            JsonSyntaxHighlighter.computeHighlighting(responseBodyArea.getText()));
                }
            }
        } catch (Exception e) {
            if (responseFoldManager != null) {
                responseFoldManager.setFullText(responseBody != null ? responseBody : "");
            } else {
                responseBodyArea.replaceText(responseBody != null ? responseBody : "");
            }
        }
    }

    /* =========================================================================
     * Request Raw Body Format & Beautify Handlers
     * ========================================================================= */

    private void setOrUpdateHeader(String key, String value) {
        boolean found = false;
        for (KeyValuePair kv : headersData) {
            if (key.equalsIgnoreCase(kv.getKey())) {
                kv.setValue(value);
                kv.setEnabled(true);
                found = true;
                break;
            }
        }
        if (!found) {
            int insertIdx = !headersData.isEmpty() && headersData.get(headersData.size() - 1).isPlaceholder()
                    ? headersData.size() - 1 : headersData.size();
            headersData.add(insertIdx, new KeyValuePair(true, key, value, "", false));
            ensureHeadersPlaceholderRow();
            updateHeadersMasterCheckBox(headersMasterCheckBox);
            updateHeadersMasterSelectBox(headersMasterSelectBox);
        }
        if (headersTable != null) {
            headersTable.refresh();
        }
    }

    @FXML
    private void handleSelectRawFormatJson() {
        if (rawFormatMenu != null) rawFormatMenu.setText("JSON");
        setOrUpdateHeader("Content-Type", "application/json");
    }

    @FXML
    private void handleSelectRawFormatXml() {
        if (rawFormatMenu != null) rawFormatMenu.setText("XML");
        setOrUpdateHeader("Content-Type", "application/xml");
    }

    @FXML
    private void handleSelectRawFormatHtml() {
        if (rawFormatMenu != null) rawFormatMenu.setText("HTML");
        setOrUpdateHeader("Content-Type", "text/html");
    }

    @FXML
    private void handleSelectRawFormatText() {
        if (rawFormatMenu != null) rawFormatMenu.setText("Text");
        setOrUpdateHeader("Content-Type", "text/plain");
    }

    @FXML
    private void handleSelectRawFormatJs() {
        if (rawFormatMenu != null) rawFormatMenu.setText("JavaScript");
        setOrUpdateHeader("Content-Type", "application/javascript");
    }

    @FXML
    private void handleBeautifyBody() {
        if (bodyTextArea != null) {
            prettifyJsonEditor(bodyTextArea);
        }
    }

    /* =========================================================================
     * Response Toolbar Handlers: { } JSON ▾, Preview, Visualization, Wrap, Search
     * ========================================================================= */

    private void showResponseCodeView(String formatName) {
        if (formatName != null && responseFormatMenu != null) {
            responseFormatMenu.setText(formatName);
        }
        if (responsePreviewToggle != null) responsePreviewToggle.setSelected(false);
        if (responseVisualizerToggle != null) responseVisualizerToggle.setSelected(false);
        if (responseCodeAreaPane != null) {
            responseCodeAreaPane.setVisible(true);
            responseCodeAreaPane.setManaged(true);
        }
        if (responsePreview != null) {
            responsePreview.setVisible(false);
            responsePreview.setManaged(false);
        }
        if (responseVisualizationView != null) {
            responseVisualizationView.setVisible(false);
            responseVisualizationView.setManaged(false);
        }
    }

    @FXML
    private void handleSelectResponseJson() {
        showResponseCodeView("{ } JSON");
        if (lastApiResponse != null && !lastApiResponse.isBinary() && lastApiResponse.getResponse() != null) {
            formatResponseBody(lastApiResponse.getResponse());
        }
    }

    @FXML
    private void handleSelectResponseXml() {
        showResponseCodeView("XML");
    }

    @FXML
    private void handleSelectResponseHtml() {
        showResponseCodeView("HTML");
    }

    @FXML
    private void handleSelectResponseText() {
        showResponseCodeView("Text");
    }

    @FXML
    private void handleSelectResponseRaw() {
        showResponseCodeView("Raw");
        if (lastApiResponse != null && !lastApiResponse.isBinary() && lastApiResponse.getResponse() != null) {
            if (responseFoldManager != null) {
                responseFoldManager.setFullText(lastApiResponse.getResponse());
            } else {
                responseBodyArea.replaceText(lastApiResponse.getResponse());
            }
        }
    }

    @FXML
    private void handleToggleResponsePreview() {
        if (responsePreviewToggle != null && responsePreviewToggle.isSelected()) {
            if (responseVisualizerToggle != null) responseVisualizerToggle.setSelected(false);
            if (responseCodeAreaPane != null) {
                responseCodeAreaPane.setVisible(false);
                responseCodeAreaPane.setManaged(false);
            }
            if (responseVisualizationView != null) {
                responseVisualizationView.setVisible(false);
                responseVisualizationView.setManaged(false);
            }
            if (responsePreview != null) {
                responsePreview.setVisible(true);
                responsePreview.setManaged(true);
                renderPreviewView();
            }
        } else {
            showResponseCodeView(null);
        }
    }

    @FXML
    private void handleToggleResponseVisualization() {
        if (responseVisualizerToggle != null && responseVisualizerToggle.isSelected()) {
            if (responsePreviewToggle != null) responsePreviewToggle.setSelected(false);
            if (responseCodeAreaPane != null) {
                responseCodeAreaPane.setVisible(false);
                responseCodeAreaPane.setManaged(false);
            }
            if (responsePreview != null) {
                responsePreview.setVisible(false);
                responsePreview.setManaged(false);
            }
            if (responseVisualizationView != null) {
                responseVisualizationView.setVisible(true);
                responseVisualizationView.setManaged(true);
                renderVisualizationView();
            }
        } else {
            showResponseCodeView(null);
        }
    }

    @FXML
    private void handleCollapseAllResponse() {
        if (responseFoldManager != null) {
            responseFoldManager.foldAll();
        }
    }

    @FXML
    private void handleExpandAllResponse() {
        if (responseFoldManager != null) {
            responseFoldManager.unfoldAll();
        }
    }

    @FXML
    private void handleToggleResponseWrap() {
        if (responseBodyArea != null && responseWrapToggle != null) {
            responseBodyArea.setWrapText(responseWrapToggle.isSelected());
        }
    }

    @FXML
    private void handleOpenResponseSearch() {
        if (responseSearch != null) {
            if (responseSearch.getBar().isManaged()) {
                responseSearch.close();
            } else {
                showResponseCodeView(null);
                responseSearch.open();
            }
        }
    }

    private void renderPreviewView() {
        if (responsePreview == null) return;
        if (lastApiResponse != null && isPdfResponse(lastApiResponse)) {
            renderPdfPreview(lastApiResponse);
            return;
        }
        String text = lastApiResponse != null && lastApiResponse.getResponse() != null
                ? lastApiResponse.getResponse() : responseBodyArea.getText();
        if (text == null) text = "";
        String trimmed = text.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("<!doctype html") || lower.startsWith("<html")) {
            responsePreview.getEngine().loadContent(text);
        } else {
            boolean isLight = ThemeManager.isLight();
            String bg = isLight ? "#ffffff" : "#1d1f24";
            String cardBg = isLight ? "#f6f8fa" : "#161b22";
            String border = isLight ? "#d0d7de" : "#30363d";
            String textColor = isLight ? "#24292f" : "#c9d1d9";
            String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>"
                    + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: " + bg + "; color: " + textColor + "; padding: 16px; margin: 0; }"
                    + "pre { background: " + cardBg + "; border: 1px solid " + border + "; border-radius: 6px; padding: 12px; font-family: 'Consolas', 'JetBrains Mono', monospace; font-size: 12px; line-height: 1.5; overflow: auto; white-space: pre-wrap; word-break: break-word; }"
                    + "</style></head><body><pre>" + escapeHtml(text) + "</pre></body></html>";
            responsePreview.getEngine().loadContent(html);
        }
    }

    private void renderVisualizationView() {
        if (responseVisualizationView == null) return;
        String raw = lastApiResponse != null && lastApiResponse.getResponse() != null
                ? lastApiResponse.getResponse() : responseBodyArea.getText();
        boolean isLight = ThemeManager.isLight();
        String html = generateVisualizationHtml(raw, isLight);
        responseVisualizationView.getEngine().loadContent(html);
    }

    private String generateVisualizationHtml(String jsonString, boolean isLight) {
        String bg = isLight ? "#ffffff" : "#1d1f24";
        String cardBg = isLight ? "#f6f8fa" : "#161b22";
        String border = isLight ? "#d0d7de" : "#30363d";
        String text = isLight ? "#24292f" : "#e6edf3";
        String textMuted = isLight ? "#57606a" : "#8b949e";
        String hoverBg = isLight ? "#f0f4f9" : "#262c36";
        String zebraBg = isLight ? "#fafbfc" : "#191c20";
        String headerBg = isLight ? "#f6f8fa" : "#161b22";
        String accent = "#0969da";

        if (jsonString == null || jsonString.isBlank()) {
            return "<!DOCTYPE html><html><body style='font-family:sans-serif;padding:24px;text-align:center;background:" + bg + ";color:" + textMuted + ";'>"
                    + "<h3>No response body to visualize</h3><p>Send a request returning JSON to see an interactive visualization.</p></body></html>";
        }

        String trimmed = jsonString.trim();
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        boolean isTable = false;

        try {
            if (trimmed.startsWith("[")) {
                JSONArray arr = new JSONArray(trimmed);
                if (arr.length() > 0 && arr.get(0) instanceof JSONObject) {
                    LinkedHashSet<String> keySet = new LinkedHashSet<>();
                    for (int i = 0; i < arr.length(); i++) {
                        Object item = arr.get(i);
                        if (item instanceof JSONObject obj) {
                            for (String k : obj.keySet()) {
                                keySet.add(k);
                            }
                        }
                    }
                    headers.addAll(keySet);
                    for (int i = 0; i < arr.length(); i++) {
                        Object item = arr.get(i);
                        List<String> row = new ArrayList<>();
                        if (item instanceof JSONObject obj) {
                            for (String h : headers) {
                                Object val = obj.opt(h);
                                row.add(formatCellValue(val));
                            }
                        } else {
                            row.add(formatCellValue(item));
                        }
                        rows.add(row);
                    }
                    isTable = true;
                } else {
                    headers.add("#");
                    headers.add("Value");
                    for (int i = 0; i < arr.length(); i++) {
                        List<String> row = new ArrayList<>();
                        row.add(String.valueOf(i + 1));
                        row.add(formatCellValue(arr.get(i)));
                        rows.add(row);
                    }
                    isTable = true;
                }
            } else if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                headers.add("Key");
                headers.add("Type");
                headers.add("Value");
                for (String k : obj.keySet()) {
                    Object val = obj.get(k);
                    List<String> row = new ArrayList<>();
                    row.add("<strong>" + escapeHtml(k) + "</strong>");
                    row.add("<span style='color:" + textMuted + ";font-size:11px;'>" + getTypeName(val) + "</span>");
                    row.add(formatCellValue(val));
                    rows.add(row);
                }
                isTable = true;
            }
        } catch (Exception e) {
            isTable = false;
        }

        if (!isTable) {
            return "<!DOCTYPE html><html><body style='font-family:-apple-system,sans-serif;padding:24px;background:" + bg + ";color:" + text + ";'>"
                    + "<div style='background:" + cardBg + ";border:1px solid " + border + ";border-radius:6px;padding:16px;'>"
                    + "<h4 style='margin-top:0;'>Raw Response Preview</h4>"
                    + "<pre style='font-family:monospace;font-size:12px;overflow:auto;white-space:pre-wrap;'>" + escapeHtml(trimmed) + "</pre>"
                    + "</div></body></html>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'>");
        html.append("<style>");
        html.append("* { box-sizing: border-box; }");
        html.append("body { margin: 0; padding: 12px 16px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 13px; background-color: ").append(bg).append("; color: ").append(text).append("; }");
        html.append(".visualizer-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; gap: 12px; }");
        html.append(".search-box { display: flex; align-items: center; gap: 8px; flex: 1; max-width: 320px; }");
        html.append(".search-box input { width: 100%; padding: 5px 10px; border: 1px solid ").append(border).append("; border-radius: 4px; background: ").append(cardBg).append("; color: ").append(text).append("; font-size: 12px; outline: none; }");
        html.append(".search-box input:focus { border-color: ").append(accent).append("; }");
        html.append(".counter { font-size: 12px; color: ").append(textMuted).append("; }");
        html.append(".table-wrap { overflow: auto; border: 1px solid ").append(border).append("; border-radius: 6px; max-height: calc(100vh - 70px); }");
        html.append("table { width: 100%; border-collapse: collapse; text-align: left; font-size: 12px; }");
        html.append("th { background: ").append(headerBg).append("; color: ").append(text).append("; font-weight: 600; padding: 8px 12px; border-bottom: 2px solid ").append(border).append("; position: sticky; top: 0; z-index: 2; cursor: pointer; user-select: none; white-space: nowrap; }");
        html.append("th:hover { background: ").append(hoverBg).append("; }");
        html.append("th .sort-icon { margin-left: 6px; font-size: 10px; opacity: 0.6; }");
        html.append("td { padding: 7px 12px; border-bottom: 1px solid ").append(border).append("; white-space: nowrap; max-width: 450px; overflow: hidden; text-overflow: ellipsis; vertical-align: top; }");
        html.append("tr:nth-child(even) td { background-color: ").append(zebraBg).append("; }");
        html.append("tr:hover td { background-color: ").append(hoverBg).append(" !important; }");
        html.append(".val-num { color: ").append(isLight ? "#098658" : "#7ee787").append("; font-family: monospace; }");
        html.append(".val-str { color: ").append(isLight ? "#0366d6" : "#79c0ff").append("; }");
        html.append(".val-bool { color: ").append(isLight ? "#005cc5" : "#79c0ff").append("; font-weight: bold; }");
        html.append(".val-null { color: ").append(isLight ? "#6e7781" : "#8b949e").append("; font-style: italic; }");
        html.append(".val-json { font-family: monospace; font-size: 11px; opacity: 0.85; }");
        html.append("</style></head><body>");

        html.append("<div class='visualizer-topbar'>");
        html.append("<div class='search-box'>");
        html.append("<input type='text' id='filterInput' placeholder='Filter records...' onkeyup='applyFilter()' />");
        html.append("</div>");
        html.append("<div class='counter' id='recordCount'>Showing ").append(rows.size()).append(" records</div>");
        html.append("</div>");

        html.append("<div class='table-wrap'>");
        html.append("<table id='visTable'><thead><tr>");
        for (int c = 0; c < headers.size(); c++) {
            html.append("<th onclick='sortTable(").append(c).append(")'>")
                    .append(escapeHtml(headers.get(c)))
                    .append("<span class='sort-icon' id='sort-icon-").append(c).append("'>↕</span></th>");
        }
        html.append("</tr></thead><tbody>");

        for (List<String> r : rows) {
            html.append("<tr>");
            for (String cell : r) {
                html.append("<td>").append(cell).append("</td>");
            }
            html.append("</tr>");
        }

        html.append("</tbody></table></div>");

        html.append("<script>");
        html.append("function applyFilter() {");
        html.append("  var input = document.getElementById('filterInput').value.toLowerCase();");
        html.append("  var rows = document.querySelectorAll('#visTable tbody tr');");
        html.append("  var count = 0;");
        html.append("  rows.forEach(function(row) {");
        html.append("    var text = row.innerText.toLowerCase();");
        html.append("    if (text.indexOf(input) > -1) { row.style.display = ''; count++; }");
        html.append("    else { row.style.display = 'none'; }");
        html.append("  });");
        html.append("  document.getElementById('recordCount').innerText = 'Showing ' + count + ' of ").append(rows.size()).append(" records';");
        html.append("}");
        html.append("var sortDirections = {};");
        html.append("function sortTable(colIndex) {");
        html.append("  var table = document.getElementById('visTable');");
        html.append("  var tbody = table.querySelector('tbody');");
        html.append("  var rows = Array.from(tbody.querySelectorAll('tr'));");
        html.append("  var dir = sortDirections[colIndex] === 'asc' ? 'desc' : 'asc';");
        html.append("  sortDirections[colIndex] = dir;");
        html.append("  rows.sort(function(a, b) {");
        html.append("    var aVal = a.cells[colIndex] ? a.cells[colIndex].innerText.trim() : '';");
        html.append("    var bVal = b.cells[colIndex] ? b.cells[colIndex].innerText.trim() : '';");
        html.append("    var aNum = parseFloat(aVal), bNum = parseFloat(bVal);");
        html.append("    if (!isNaN(aNum) && !isNaN(bNum)) { return dir === 'asc' ? aNum - bNum : bNum - aNum; }");
        html.append("    return dir === 'asc' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);");
        html.append("  });");
        html.append("  rows.forEach(function(r) { tbody.appendChild(r); });");
        html.append("  for (var i = 0; i < ").append(headers.size()).append("; i++) {");
        html.append("    var icon = document.getElementById('sort-icon-' + i);");
        html.append("    if (icon) icon.innerText = (i === colIndex ? (dir === 'asc' ? '▲' : '▼') : '↕');");
        html.append("  }");
        html.append("}");
        html.append("</script>");

        html.append("</body></html>");
        return html.toString();
    }

    private String formatCellValue(Object val) {
        if (val == null || val == JSONObject.NULL) {
            return "<span class='val-null'>null</span>";
        } else if (val instanceof Boolean) {
            return "<span class='val-bool'>" + val + "</span>";
        } else if (val instanceof Number) {
            return "<span class='val-num'>" + val + "</span>";
        } else if (val instanceof JSONObject || val instanceof JSONArray) {
            return "<span class='val-json'>" + escapeHtml(val.toString()) + "</span>";
        } else {
            return "<span class='val-str'>" + escapeHtml(String.valueOf(val)) + "</span>";
        }
    }

    private String getTypeName(Object val) {
        if (val == null || val == JSONObject.NULL) return "null";
        if (val instanceof Boolean) return "boolean";
        if (val instanceof Number) return "number";
        if (val instanceof JSONObject) return "object";
        if (val instanceof JSONArray) return "array";
        return "string";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
            if (header.isEnabled() && !header.isPlaceholder()
                    && header.getKey() != null && !header.getKey().trim().isEmpty()
                    && header.getValue() != null && !header.getValue().trim().isEmpty()) {
                headers.put(header.getKey().trim(), header.getValue().trim());
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
            if (kv.isPlaceholder() || !kv.isEnabled()) {
                continue;
            }
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
                // Validate JSON if it looks like JSON. Comments are stripped
                // first — Postman-style "//" notes and commented-out fields
                // (e.g. disabling "fatherName"/"motherName" while filling a
                // form) are valid things to leave in the editor and must
                // never trip this check. The comments themselves are NOT
                // removed from what actually gets sent — bodyText below is
                // still the original raw text, exactly like Postman sends
                // the editor's raw contents verbatim.
                if (bodyText.startsWith("{") || bodyText.startsWith("[")) {
                    String forValidation = JsonCommentStripper.strip(bodyText).trim();
                    try {
                        if (forValidation.startsWith("[")) {
                            new JSONArray(forValidation);
                        } else {
                            new JSONObject(forValidation);
                        }
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
            if (pair.isPlaceholder() || !pair.isEnabled()) {
                continue;
            }
            if (!pair.getKey().isEmpty() && !pair.getValue().isEmpty()) {
                json.put(pair.getKey(), pair.getValue());
            }
        }
        return json.toString();
    }

    private String convertFormDataToUrlEncoded(ObservableList<KeyValuePair> formData) {
        try {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (int i = 0; i < formData.size(); i++) {
                KeyValuePair pair = formData.get(i);
                if (pair.isPlaceholder() || !pair.isEnabled()) {
                    continue;
                }
                if (!pair.getKey().isEmpty() && !pair.getValue().isEmpty()) {
                    if (!first) result.append("&");
                    first = false;
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
            int insertIdx = !headersData.isEmpty() && headersData.get(headersData.size() - 1).isPlaceholder()
                    ? headersData.size() - 1 : headersData.size();
            headersData.add(insertIdx, new KeyValuePair(true, "Content-Type", "application/json", "Request content type", false));
            ensureHeadersPlaceholderRow();
            updateHeadersMasterCheckBox(headersMasterCheckBox);
            updateHeadersMasterSelectBox(headersMasterSelectBox);
        }

        AlertUtils.showInfo("Sample POST request loaded. Click Send to execute.");
    }

    @FXML
    private void handleAddParamRow() {
        KeyValuePair newRow = new KeyValuePair(true, "", "", "", false);
        newRow.setSelected(false);

        int targetIndex;
        if (!paramsData.isEmpty() && paramsData.get(paramsData.size() - 1).isPlaceholder()) {
            targetIndex = paramsData.size() - 1;
            paramsData.add(targetIndex, newRow);
        } else {
            paramsData.add(newRow);
            targetIndex = paramsData.size() - 1;
        }

        ensurePlaceholderRow();

        pendingFocusParamRow = targetIndex;
        paramsTable.getSelectionModel().clearSelection();
        focusParamCell(targetIndex, 1);
        onEditorFieldChanged();
    }

    @FXML
    private void handleDeleteParamRow() {
        // Collect all rows marked by Select checkbox
        List<KeyValuePair> toDelete = paramsData.stream()
                .filter(kv -> kv.isSelected() && !isRowEmpty(kv) && !kv.isPlaceholder())
                .collect(Collectors.toList());

        // If no explicit selection, fall back to the currently focused row
        if (toDelete.isEmpty()) {
            KeyValuePair active = paramsTable.getSelectionModel().getSelectedItem();
            if (active != null && !isRowEmpty(active) && !active.isPlaceholder()) {
                toDelete.add(active);
            }
        }

        if (!toDelete.isEmpty()) {
            paramsData.removeAll(toDelete);
            ensurePlaceholderRow();
            syncParamsToUrl();
            onEditorFieldChanged();
            updateMasterCheckBox(paramsMasterCheckBox);
            updateMasterSelectBox(paramsMasterSelectBox);
            paramsTable.refresh();
        }
    }

    @FXML
    private void handleAddHeaderRow() {
        KeyValuePair newRow = new KeyValuePair(true, "", "", "", false);
        newRow.setSelected(false);

        int targetIndex;
        if (!headersData.isEmpty() && headersData.get(headersData.size() - 1).isPlaceholder()) {
            targetIndex = headersData.size() - 1;
            headersData.add(targetIndex, newRow);
        } else {
            headersData.add(newRow);
            targetIndex = headersData.size() - 1;
        }

        ensureHeadersPlaceholderRow();

        pendingFocusHeaderRow = targetIndex;
        headersTable.getSelectionModel().clearSelection();
        focusHeaderCell(targetIndex, 1);
        onEditorFieldChanged();
    }

    @FXML
    private void handleDeleteHeaderRow() {
        // Collect all rows marked by Select checkbox
        List<KeyValuePair> toDelete = headersData.stream()
                .filter(kv -> kv.isSelected() && !isRowEmpty(kv) && !kv.isPlaceholder())
                .collect(Collectors.toList());

        // If no explicit selection, fall back to the currently focused row
        if (toDelete.isEmpty()) {
            KeyValuePair active = headersTable.getSelectionModel().getSelectedItem();
            if (active != null && !isRowEmpty(active) && !active.isPlaceholder()) {
                toDelete.add(active);
            }
        }

        if (!toDelete.isEmpty()) {
            headersData.removeAll(toDelete);
            ensureHeadersPlaceholderRow();
            onEditorFieldChanged();
            updateHeadersMasterCheckBox(headersMasterCheckBox);
            updateHeadersMasterSelectBox(headersMasterSelectBox);
            headersTable.refresh();
        }
    }

    @FXML
    private void handleAddFormDataRow() {
        if (urlEncodedTable != null && urlEncodedTable.isVisible()) {
            KeyValuePair newRow = new KeyValuePair(true, "", "", "", false);
            newRow.setSelected(false);
            int targetIndex;
            if (!urlEncodedData.isEmpty() && urlEncodedData.get(urlEncodedData.size() - 1).isPlaceholder()) {
                targetIndex = urlEncodedData.size() - 1;
                urlEncodedData.add(targetIndex, newRow);
            } else {
                urlEncodedData.add(newRow);
                targetIndex = urlEncodedData.size() - 1;
            }
            ensureUrlEncodedPlaceholderRow();
            pendingFocusUrlEncodedRow = targetIndex;
            urlEncodedTable.getSelectionModel().clearSelection();
            focusUrlEncodedCell(targetIndex, 1);
            onEditorFieldChanged();
            return;
        }

        KeyValuePair newRow = new KeyValuePair(true, "", "", "", false);
        newRow.setSelected(false);
        int targetIndex;
        if (!formData.isEmpty() && formData.get(formData.size() - 1).isPlaceholder()) {
            targetIndex = formData.size() - 1;
            formData.add(targetIndex, newRow);
        } else {
            formData.add(newRow);
            targetIndex = formData.size() - 1;
        }
        ensureFormDataPlaceholderRow();
        pendingFocusFormDataRow = targetIndex;
        formDataTable.getSelectionModel().clearSelection();
        focusFormDataCell(targetIndex, 1);
        onEditorFieldChanged();
    }

    @FXML
    private void handleDeleteFormDataRow() {
        if (urlEncodedTable != null && urlEncodedTable.isVisible()) {
            List<KeyValuePair> toDelete = urlEncodedData.stream()
                    .filter(kv -> kv.isSelected() && !isRowEmpty(kv) && !kv.isPlaceholder())
                    .collect(Collectors.toList());
            if (toDelete.isEmpty()) {
                KeyValuePair active = urlEncodedTable.getSelectionModel().getSelectedItem();
                if (active != null && !isRowEmpty(active) && !active.isPlaceholder()) {
                    toDelete.add(active);
                }
            }
            if (!toDelete.isEmpty()) {
                urlEncodedData.removeAll(toDelete);
                ensureUrlEncodedPlaceholderRow();
                onEditorFieldChanged();
                updateUrlEncodedMasterCheckBox(urlEncodedMasterCheckBox);
                updateUrlEncodedMasterSelectBox(urlEncodedMasterSelectBox);
                urlEncodedTable.refresh();
            }
            return;
        }

        List<KeyValuePair> toDelete = formData.stream()
                .filter(kv -> kv.isSelected() && !isRowEmpty(kv) && !kv.isPlaceholder())
                .collect(Collectors.toList());
        if (toDelete.isEmpty()) {
            KeyValuePair active = formDataTable.getSelectionModel().getSelectedItem();
            if (active != null && !isRowEmpty(active) && !active.isPlaceholder()) {
                toDelete.add(active);
            }
        }
        if (!toDelete.isEmpty()) {
            formData.removeAll(toDelete);
            ensureFormDataPlaceholderRow();
            onEditorFieldChanged();
            updateFormDataMasterCheckBox(formDataMasterCheckBox);
            updateFormDataMasterSelectBox(formDataMasterSelectBox);
            formDataTable.refresh();
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
        if (!WorkspaceManager.canEditCurrentWorkspace()) {
            AlertUtils.showError("You have Viewer access to this workspace — Editor access is needed to create things in it.");
            return;
        }
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
            initAndStyleDialog(dialog);

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
            initAndStyleDialog(dialog);

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
            Long requestId = requestIdMap.get(selectedItem);

            if (collectionId != null) {
                duplicateCollection(collectionId);
            } else if (folderId != null) {
                duplicateFolder(folderId);
            } else if (requestId != null) {
                duplicateRequest(requestId);
            } else {
                AlertUtils.showError("Please select a collection, folder, or request to duplicate");
            }
        } else {
            AlertUtils.showError("Please select an item to duplicate");
        }
    }

    @FXML
    private void handleDeleteItem() {
        if (!WorkspaceManager.canEditCurrentWorkspace()) {
            AlertUtils.showError("You have Viewer access to this workspace — Editor access is needed to delete things in it.");
            return;
        }
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
                initAndStyleDialog(dialog);

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
        initAndStyleDialog(dialog);
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

    /** Postman's collection Duplicate: an exact, independent copy of the
     * WHOLE tree — every folder, every nested subfolder, every request in
     * each, all landing under a freshly-created "{name} Copy" collection.
     * Only the collection itself gets renamed; everything inside keeps
     * its original name unchanged, matching Postman. */
    private void duplicateCollection(Long collectionId) {
        new Thread(() -> {
            try {
                Optional<CollectionResponse> originalOpt = CollectionService.getCollectionById(collectionId);
                if (originalOpt.isEmpty()) {
                    Platform.runLater(() -> AlertUtils.showError("Failed to duplicate collection"));
                    return;
                }
                CollectionResponse original = originalOpt.get();

                List<String> siblingNames = CollectionService.getUserCollections()
                        .orElse(new ArrayList<>())
                        .stream().map(CollectionResponse::getName).toList();
                String newName = generateCopyName(original.getName(), siblingNames);

                CollectionRequest createReq = CollectionRequest.builder()
                        .name(newName)
                        .description(original.getDescription())
                        .build();
                Optional<CollectionResponse> createdOpt = CollectionService.createCollection(createReq);
                if (createdOpt.isEmpty()) {
                    Platform.runLater(() -> AlertUtils.showError("Failed to duplicate collection"));
                    return;
                }
                Long newCollectionId = createdOpt.get().getId();

                deepCopySubtree(collectionId, null, newCollectionId, null);

                Platform.runLater(() -> {
                    refreshCollectionsTree();
                    updateStatus("Collection duplicated as \"" + newName + "\"");
                });
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Duplicating collection"));
            }
        }).start();
    }

    /** Postman's folder Duplicate: an exact copy of this folder AND
     * everything inside it (nested subfolders, requests), placed as a
     * sibling in the same parent. Only the folder itself gets renamed. */
    private void duplicateFolder(Long folderId) {
        new Thread(() -> {
            try {
                Optional<FolderResponse> originalOpt = FolderService.getFolderById(folderId);
                if (originalOpt.isEmpty()) {
                    Platform.runLater(() -> AlertUtils.showError("Failed to duplicate folder"));
                    return;
                }
                FolderResponse original = originalOpt.get();
                Long collectionId = original.getCollectionId();
                Long parentFolderId = original.getParentFolderId();

                List<String> siblingNames = FolderService.getCollectionFolders(collectionId)
                        .orElse(new ArrayList<>())
                        .stream()
                        .filter(f -> Objects.equals(f.getParentFolderId(), parentFolderId))
                        .map(FolderResponse::getName)
                        .toList();
                String newName = generateCopyName(original.getName(), siblingNames);

                Optional<FolderResponse> createdOpt = FolderService.createFolder(
                        newName, original.getDescription(), collectionId, parentFolderId);
                if (createdOpt.isEmpty()) {
                    Platform.runLater(() -> AlertUtils.showError("Failed to duplicate folder"));
                    return;
                }
                Long newFolderId = createdOpt.get().getId();

                deepCopySubtree(collectionId, folderId, collectionId, newFolderId);

                Platform.runLater(() -> {
                    refreshCollectionsTree();
                    updateStatus("Folder duplicated as \"" + newName + "\"");
                });
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Duplicating folder"));
            }
        }).start();
    }

    /** Postman's request Duplicate: an exact copy of this one request
     * (every field — method, URL, headers, body, scripts, auth) landing
     * right next to the original, in the same folder/collection. */
    private void duplicateRequest(Long requestId) {
        new Thread(() -> {
            try {
                Optional<RequestResponse> originalOpt = RequestService.getRequest(requestId);
                if (originalOpt.isEmpty()) {
                    Platform.runLater(() -> AlertUtils.showError("Failed to duplicate request"));
                    return;
                }
                RequestResponse original = originalOpt.get();
                Long collectionId = original.getCollectionId();
                Long folderId = original.getFolderId();

                List<String> siblingNames = CollectionService.getCollectionWithDetails(collectionId)
                        .map(CollectionResponse::getRequestResponses)
                        .orElse(new ArrayList<>())
                        .stream()
                        .filter(r -> Objects.equals(r.getFolderId(), folderId))
                        .map(RequestResponse::getName)
                        .toList();
                String newName = generateCopyName(original.getName(), siblingNames);

                ApiRequest copyRequest = ApiRequest.builder()
                        .name(newName)
                        .description(original.getDescription())
                        .method(original.getMethod())
                        .url(original.getUrl())
                        .headers(original.getHeaders())
                        .body(original.getBody())
                        .collectionId(collectionId)
                        .folderId(folderId)
                        .preRequestScript(original.getPreRequestScript())
                        .testsScript(original.getTestsScript())
                        .authType(original.getAuthType())
                        .authToken(original.getAuthToken())
                        .authUsername(original.getAuthUsername())
                        .authPassword(original.getAuthPassword())
                        .build();
                Optional<RequestResponse> createdOpt = RequestService.saveRequest(copyRequest);

                Platform.runLater(() -> {
                    if (createdOpt.isPresent()) {
                        refreshCollectionsTree();
                        updateStatus("Request duplicated as \"" + newName + "\"");
                    } else {
                        AlertUtils.showError("Failed to duplicate request");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> handleApiError(e, "Duplicating request"));
            }
        }).start();
    }

    /** Deep-copies every folder/request at or below {@code sourceRootFolderId}
     * (or, when null, every top-level folder/request in the whole
     * collection) from {@code sourceCollectionId} into
     * {@code targetCollectionId}, landing under {@code targetRootFolderId}
     * (null = the collection's root). Names are copied exactly — only the
     * item that was actually right-clicked gets the "Copy" rename;
     * everything nested under it keeps its original name, matching
     * Postman. Runs entirely on the calling (background) thread. */
    private void deepCopySubtree(Long sourceCollectionId, Long sourceRootFolderId,
                                  Long targetCollectionId, Long targetRootFolderId) {
        List<FolderResponse> allFolders = FolderService.getCollectionFolders(sourceCollectionId)
                .orElse(new ArrayList<>());
        List<RequestResponse> allRequests = CollectionService.getCollectionWithDetails(sourceCollectionId)
                .map(CollectionResponse::getRequestResponses)
                .orElse(new ArrayList<>());

        // old folder id -> new folder id. Seed it with the already-created
        // copy of the root folder itself, when duplicating a folder.
        Map<Long, Long> folderIdMapping = new HashMap<>();
        if (sourceRootFolderId != null) {
            folderIdMapping.put(sourceRootFolderId, targetRootFolderId);
        }

        // Recreate folders in parent-before-child order (breadth-first by
        // however many passes the tree's depth needs) so every child's
        // parent has already been mapped by the time we get to it.
        List<FolderResponse> remaining = new ArrayList<>(allFolders);
        boolean progress = true;
        while (progress && !remaining.isEmpty()) {
            progress = false;
            List<FolderResponse> ready = new ArrayList<>();
            for (FolderResponse f : remaining) {
                boolean isTopLevelOfWholeCollection = sourceRootFolderId == null && f.getParentFolderId() == null;
                boolean parentAlreadyMapped = f.getParentFolderId() != null && folderIdMapping.containsKey(f.getParentFolderId());
                if (isTopLevelOfWholeCollection || parentAlreadyMapped) {
                    ready.add(f);
                }
            }
            for (FolderResponse f : ready) {
                Long newParentId = f.getParentFolderId() != null ? folderIdMapping.get(f.getParentFolderId()) : targetRootFolderId;
                Optional<FolderResponse> created = FolderService.createFolder(
                        f.getName(), f.getDescription(), targetCollectionId, newParentId);
                created.ifPresent(nf -> folderIdMapping.put(f.getId(), nf.getId()));
                progress = true;
            }
            remaining.removeAll(ready);
        }

        for (RequestResponse reqSummary : allRequests) {
            Long originalFolderId = reqSummary.getFolderId();
            Long newFolderId;
            if (sourceRootFolderId == null) {
                // Whole-collection copy: every request comes along, either
                // into its mapped folder or straight into the new root.
                if (originalFolderId == null) {
                    newFolderId = null;
                } else {
                    newFolderId = folderIdMapping.get(originalFolderId);
                    if (newFolderId == null) {
                        continue; // shouldn't happen for a well-formed tree; skip rather than misplace it
                    }
                }
            } else {
                // Single-folder copy: only requests inside it (or a
                // descendant) come along.
                if (originalFolderId == null || !folderIdMapping.containsKey(originalFolderId)) {
                    continue;
                }
                newFolderId = folderIdMapping.get(originalFolderId);
            }

            final Long resolvedFolderId = newFolderId;
            RequestService.getRequest(reqSummary.getId()).ifPresent(full -> {
                ApiRequest copyRequest = ApiRequest.builder()
                        .name(full.getName())
                        .description(full.getDescription())
                        .method(full.getMethod())
                        .url(full.getUrl())
                        .headers(full.getHeaders())
                        .body(full.getBody())
                        .collectionId(targetCollectionId)
                        .folderId(resolvedFolderId)
                        .preRequestScript(full.getPreRequestScript())
                        .testsScript(full.getTestsScript())
                        .authType(full.getAuthType())
                        .authToken(full.getAuthToken())
                        .authUsername(full.getAuthUsername())
                        .authPassword(full.getAuthPassword())
                        .build();
                RequestService.saveRequest(copyRequest);
            });
        }
    }

    private static final java.util.regex.Pattern COPY_NAME_PATTERN =
            java.util.regex.Pattern.compile("^(.+) Copy(?: (\\d+(?:\\.\\d+)?))?$");

    /** Postman's duplicate-naming scheme: "X" -> "X Copy" -> "X Copy 2" ->
     * "X Copy 3" for repeated duplicates of the SAME original — but
     * duplicating a copy branches into a sub-version instead of
     * continuing that top-level count: "X Copy 3" -> "X Copy 3.1" -> "X
     * Copy 3.2", and a bare "X Copy" (implicitly version 1) -> "X Copy
     * 1.1". Either way the result is guaranteed unique among
     * {@code siblingNames}. */
    private String generateCopyName(String originalName, List<String> siblingNames) {
        Set<String> taken = new HashSet<>(siblingNames);
        java.util.regex.Matcher matcher = COPY_NAME_PATTERN.matcher(originalName);

        if (!matcher.matches()) {
            String base = originalName + " Copy";
            if (!taken.contains(base)) {
                return base;
            }
            int n = 2;
            while (taken.contains(originalName + " Copy " + n)) {
                n++;
            }
            return originalName + " Copy " + n;
        }

        String base = matcher.group(1);
        String version = matcher.group(2);
        String majorPart;
        int minorPart;
        if (version == null) {
            majorPart = "1";
            minorPart = 1;
        } else if (version.contains(".")) {
            String[] parts = version.split("\\.", 2);
            majorPart = parts[0];
            minorPart = Integer.parseInt(parts[1]) + 1;
        } else {
            majorPart = version;
            minorPart = 1;
        }
        String candidate = base + " Copy " + majorPart + "." + minorPart;
        while (taken.contains(candidate)) {
            minorPart++;
            candidate = base + " Copy " + majorPart + "." + minorPart;
        }
        return candidate;
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
                    apiRequest.setUrl("");
                    apiRequest.setHeaders("{}");
                    apiRequest.setBody("");
                    apiRequest.setCollectionId(collectionId);
                    apiRequest.setFolderId(folderId);

                    Optional<RequestResponse> savedRequest = RequestService.saveRequest(apiRequest);
                    if (savedRequest.isEmpty()) {
                        // Fallback in case remote backend requires non-blank URL
                        apiRequest.setUrl("https://api.example.com/endpoint");
                        savedRequest = RequestService.saveRequest(apiRequest);
                    }
                    if (savedRequest.isPresent()) {
                        final RequestResponse createdRequest = savedRequest.get();
                        Platform.runLater(() -> {
                            refreshCollectionsTree();
                            updateStatus("Request created: " + name);
                            // Open the new request in its own tab, like Postman
                            openRequestTab(name, createdRequest.getId(), method);
                            loadRequestIntoUI(createdRequest);
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
        private final SimpleBooleanProperty enabled;
        private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
        private final SimpleBooleanProperty placeholder = new SimpleBooleanProperty(false);
        // Only meaningful for the form-data table — "Text" (default) or
        // "File". Every other table (params, headers, cookies...) just
        // never touches these.
        private final SimpleStringProperty type = new SimpleStringProperty("Text");
        private final SimpleStringProperty filePath = new SimpleStringProperty("");
        private final SimpleStringProperty fileName = new SimpleStringProperty("");

        public KeyValuePair() {
            this(true, "", "", "");
        }

        public KeyValuePair(String key, String value) {
            this(true, key, value, "");
        }

        public KeyValuePair(String key, String value, String description) {
            this(true, key, value, description);
        }

        public KeyValuePair(boolean enabled, String key, String value, String description) {
            this(enabled, key, value, description, false);
        }

        public KeyValuePair(boolean enabled, String key, String value, String description, boolean placeholder) {
            this.enabled = new SimpleBooleanProperty(enabled);
            this.key = new SimpleStringProperty(key != null ? key : "");
            this.value = new SimpleStringProperty(value != null ? value : "");
            this.description = new SimpleStringProperty(description != null ? description : "");
            this.placeholder.set(placeholder);
        }

        public boolean isPlaceholder() {
            return placeholder.get();
        }

        public SimpleBooleanProperty placeholderProperty() {
            return placeholder;
        }

        public void setPlaceholder(boolean placeholder) {
            this.placeholder.set(placeholder);
        }

        public boolean isEnabled() {
            return enabled.get();
        }

        public SimpleBooleanProperty enabledProperty() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled.set(enabled);
        }

        public boolean isSelected() {
            return selected.get();
        }

        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
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

    public static class SocketIoEventItem {
        private final SimpleStringProperty eventName = new SimpleStringProperty("");
        private final SimpleBooleanProperty listen = new SimpleBooleanProperty(true);
        private final SimpleStringProperty description = new SimpleStringProperty("");

        public SocketIoEventItem() {
            this("", true, "");
        }

        public SocketIoEventItem(String eventName, boolean listen, String description) {
            this.eventName.set(eventName != null ? eventName : "");
            this.listen.set(listen);
            this.description.set(description != null ? description : "");
        }

        public String getEventName() {
            return eventName.get();
        }

        public void setEventName(String value) {
            eventName.set(value != null ? value : "");
        }

        public SimpleStringProperty eventNameProperty() {
            return eventName;
        }

        public boolean isListen() {
            return listen.get();
        }

        public void setListen(boolean value) {
            listen.set(value);
        }

        public SimpleBooleanProperty listenProperty() {
            return listen;
        }

        public String getDescription() {
            return description.get();
        }

        public void setDescription(String value) {
            description.set(value != null ? value : "");
        }

        public SimpleStringProperty descriptionProperty() {
            return description;
        }
    }
}
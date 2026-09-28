package com.banking.ui;

import com.banking.model.Transaction;
import com.banking.pattern.creational.DatabaseManager;
import com.banking.service.AuthService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Khung giao diện chính (BorderPane):
 * - Bên trái: Thanh điều hướng Sidebar Navy (#0A2342) cố định với logo VB, menu & shortcut hints
 * - Ở giữa: StackPane chứa Breadcrumb, ScrollPane nội dung và Toast container nổi
 * - Hỗ trợ: Dark/Light Mode toggle, Keyboard Shortcuts (Ctrl+T, Ctrl+D, Ctrl+Z, v.v.)
 */
public class MainLayout extends BorderPane {

    private final UIContext ctx = UIContext.getInstance();
    private final ScrollPane centerScrollPane = new ScrollPane();
    private final VBox navButtonBox = new VBox(6);
    private final Map<String, Button> navButtons = new HashMap<>();

    // Breadcrumb controls
    private final HBox breadcrumbBar = new HBox(8);
    private final Label lblBreadcrumbCurrent = new Label("Tổng quan (Dashboard)");

    // Toast Container
    private final VBox toastContainer = new VBox(10);

    // Theme toggle
    private boolean isDarkMode = false;
    private final Button btnThemeToggle = new Button("🌙  Chế độ tối (Ctrl+L)");

    private DashboardView dashboardView;
    private AccountView accountView;
    private DepositWithdrawView depositWithdrawView;
    private TransferView transferView;
    private HistoryView historyView;
    private ProxyDemoView proxyDemoView;
    private UserView userView;
    private ProfileView profileView;
    private final AuthService.User currentUser;

    private final Label lblSidebarDbInfo = new Label();
    private final Label lblSidebarUndoInfo = new Label();

    public MainLayout(AuthService auth, AuthService.User user) {
        this.currentUser = user;

        // Initialize views
        dashboardView = new DashboardView(this::navigateTo, user.role());
        accountView = new AccountView();
        depositWithdrawView = new DepositWithdrawView();
        transferView = new TransferView();
        historyView = new HistoryView(user.role() != AuthService.Role.VIEWER);
        profileView = new ProfileView(auth, user);
        if (user.role() == AuthService.Role.ADMIN) {
            proxyDemoView = new ProxyDemoView();
            userView = new UserView(auth);
        }

        // Setup Breadcrumb
        buildBreadcrumb();

        // Setup center scroll pane
        centerScrollPane.getStyleClass().add("main-scroll-pane");
        centerScrollPane.setFitToWidth(true);
        centerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(centerScrollPane, Priority.ALWAYS);

        // Center Box: Breadcrumb on top + Content
        VBox centerBox = new VBox();
        centerBox.getChildren().addAll(breadcrumbBar, centerScrollPane);

        // Floating Toast container
        toastContainer.setAlignment(Pos.TOP_RIGHT);
        toastContainer.setPadding(new Insets(16, 24, 16, 24));
        toastContainer.setPickOnBounds(false); // Cho phép sự kiện chuột xuyên qua vùng trống
        toastContainer.setMaxWidth(400);
        StackPane.setAlignment(toastContainer, Pos.TOP_RIGHT);
        ToastNotification.setGlobalContainer(toastContainer);

        // StackPane combining content and floating toasts
        StackPane centerContainer = new StackPane();
        centerContainer.getChildren().addAll(centerBox, toastContainer);
        setCenter(centerContainer);

        // Setup sidebar
        setLeft(buildSidebar());

        // Default screen
        navigateTo("dashboard");

        // Sync sidebar footer with live data
        ctx.addDataChangeListener(this::updateSidebarFooter);
        updateSidebarFooter();

        // Register Global Keyboard Shortcuts when attached to Scene
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                registerKeyboardShortcuts(newScene);
            }
        });
    }

    private void buildBreadcrumb() {
        breadcrumbBar.getStyleClass().add("breadcrumb-bar");

        Button btnHome = new Button("Trang chủ");
        btnHome.getStyleClass().add("breadcrumb-link");
        btnHome.setOnAction(e -> navigateTo("dashboard"));

        Label sep = new Label("›");
        sep.getStyleClass().add("breadcrumb-separator");

        lblBreadcrumbCurrent.getStyleClass().add("breadcrumb-current");

        breadcrumbBar.getChildren().addAll(btnHome, sep, lblBreadcrumbCurrent);
    }

    private void updateBreadcrumb(String viewKey) {
        String title = switch (viewKey) {
            case "accounts" -> "Quản lý & Mở tài khoản (Builder)";
            case "deposit"  -> "Nạp & Rút tiền mặt (State)";
            case "transfer" -> "Chuyển khoản liên ngân hàng (Facade & Command)";
            case "history"  -> "Lịch sử giao dịch & Hoàn tác (Command Undo)";
            case "proxy"    -> "Phân quyền truy cập an ninh (Proxy RBAC)";
            case "users"    -> "Quản trị người dùng & Phân quyền";
            case "profile"  -> "Thông tin phiên đăng nhập";
            default         -> "Tổng quan (Dashboard)";
        };
        lblBreadcrumbCurrent.setText(title);
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        // Brand box
        HBox brand = new HBox(12);
        brand.getStyleClass().add("brand-box");

        Label logo = new Label("VB");
        logo.getStyleClass().add("brand-logo");

        VBox titleBox = new VBox(2);
        Label title = new Label("VietBank");
        title.getStyleClass().add("brand-title");
        Label subtitle = new Label("9 Design Patterns Demo");
        subtitle.getStyleClass().add("brand-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        brand.getChildren().addAll(logo, titleBox);

        // Nav buttons with shortcut hints
        Label navTitle = new Label("DANH MỤC QUẢN LÝ");
        navTitle.getStyleClass().add("nav-section-title");

        addNavButton("dashboard", "📊  Tổng quan", "Ctrl+B");
        if (currentUser.role() == AuthService.Role.ADMIN) {
            addNavButton("accounts", "👤  Mở tài khoản", "Ctrl+A");
        }
        if (currentUser.role() != AuthService.Role.VIEWER) {
            addNavButton("deposit", "💵  Nạp & Rút tiền", "Ctrl+D");
            addNavButton("transfer", "🔁  Chuyển khoản", "Ctrl+T");
        }
        addNavButton("history", "📜  Lịch sử & Undo", "Ctrl+H");
        if (currentUser.role() == AuthService.Role.ADMIN) {
            addNavButton("proxy", "🛡️  Phân quyền (Proxy)", null);
            addNavButton("users", "🔐  Người dùng", null);
        }
        addNavButton("profile", "⚙  Tài khoản đăng nhập", null);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Sidebar Footer (Theme toggle + Singleton + Undo info)
        VBox footer = new VBox(6);
        footer.getStyleClass().add("sidebar-footer");

        btnThemeToggle.setMaxWidth(Double.MAX_VALUE);
        btnThemeToggle.getStyleClass().add("btn-secondary");
        btnThemeToggle.setOnAction(e -> toggleTheme());

        lblSidebarDbInfo.getStyleClass().add("sidebar-status-text");
        lblSidebarDbInfo.setText("● DB Singleton @" + Integer.toHexString(DatabaseManager.getInstance().hashCode()));

        lblSidebarUndoInfo.getStyleClass().add("sidebar-status-text");
        lblSidebarUndoInfo.setText("● Lệnh Undo: 0");

        Label patternCount = new Label("★ 9 Patterns Active");
        patternCount.setStyle("-fx-text-fill: #C9A84C; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label signedIn = new Label("● " + currentUser.username() + " (" + currentUser.role() + ")");
        signedIn.getStyleClass().add("sidebar-status-text");

        Button signOut = new Button("Đăng xuất");
        signOut.getStyleClass().add("btn-secondary");
        signOut.setMaxWidth(Double.MAX_VALUE);
        signOut.setOnAction(event -> getScene().getWindow().hide());

        footer.getChildren().addAll(signedIn, btnThemeToggle, signOut, patternCount, lblSidebarDbInfo, lblSidebarUndoInfo);
        footer.setPadding(new Insets(12));

        sidebar.getChildren().addAll(brand, navTitle, navButtonBox, spacer, footer);
        return sidebar;
    }

    private void addNavButton(String key, String text, String shortcutHint) {
        Button btn = new Button();
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblText = new Label(text);
        lblText.setStyle("-fx-text-fill: inherit;");
        HBox.setHgrow(lblText, Priority.ALWAYS);

        row.getChildren().add(lblText);

        if (shortcutHint != null) {
            Label lblHint = new Label(shortcutHint);
            lblHint.setStyle("-fx-text-fill: #607289; -fx-font-size: 10px; -fx-font-family: monospace;");
            row.getChildren().add(lblHint);
        }

        btn.setGraphic(row);
        btn.setOnAction(e -> navigateTo(key));
        navButtons.put(key, btn);
        navButtonBox.getChildren().add(btn);
    }

    public void navigateTo(String viewKey) {
        if (viewKey != null && viewKey.contains(":")) {
            String[] parts = viewKey.split(":", 2);
            navigateTo(parts[0], parts[1]);
            return;
        }
        navigateTo(viewKey, null);
    }

    public void navigateTo(String viewKey, String targetAccountNo) {
        if (!navButtons.containsKey(viewKey)) viewKey = "dashboard";
        // Reset active style
        for (Button btn : navButtons.values()) {
            btn.getStyleClass().remove("nav-button-active");
        }

        Button activeBtn = navButtons.get(viewKey);
        if (activeBtn != null && !activeBtn.getStyleClass().contains("nav-button-active")) {
            activeBtn.getStyleClass().add("nav-button-active");
        }

        if (targetAccountNo != null) {
            if ("deposit".equals(viewKey)) {
                depositWithdrawView.selectDepositAccount(targetAccountNo);
            } else if ("withdraw".equals(viewKey)) {
                depositWithdrawView.selectWithdrawAccount(targetAccountNo);
                viewKey = "deposit";
            } else if ("transfer".equals(viewKey)) {
                transferView.selectFromAccount(targetAccountNo);
            }
        }

        Node targetView = switch (viewKey) {
            case "accounts" -> accountView;
            case "deposit"  -> depositWithdrawView;
            case "transfer" -> transferView;
            case "history"  -> historyView;
            case "proxy"    -> proxyDemoView;
            case "users"    -> userView;
            case "profile"  -> profileView;
            default         -> dashboardView;
        };

        updateBreadcrumb(viewKey);
        centerScrollPane.setContent(targetView);
        centerScrollPane.setVvalue(0); // Scroll to top
    }

    public void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (getScene() != null && getScene().getRoot() != null) {
            if (isDarkMode) {
                getScene().getRoot().getStyleClass().add("theme-dark");
                btnThemeToggle.setText("☀️  Chế độ sáng (Ctrl+L)");
                ToastNotification.showInfo("Đã chuyển sang Giao diện Tối (Dark Mode)");
            } else {
                getScene().getRoot().getStyleClass().remove("theme-dark");
                btnThemeToggle.setText("🌙  Chế độ tối (Ctrl+L)");
                ToastNotification.showInfo("Đã chuyển sang Giao diện Sáng (Light Mode)");
            }
        }
    }

    private void registerKeyboardShortcuts(Scene scene) {
        // Ctrl + B: Dashboard
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN),
                () -> navigateTo("dashboard")
        );
        // Ctrl + T: Chuyển khoản
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN),
                () -> navigateTo("transfer")
        );
        // Ctrl + D: Nạp & Rút tiền
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
                () -> navigateTo("deposit")
        );
        // Ctrl + H: Lịch sử & Undo
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN),
                () -> navigateTo("history")
        );
        // Ctrl + A: Mở tài khoản (nếu admin)
        if (currentUser.role() == AuthService.Role.ADMIN) {
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN),
                    () -> navigateTo("accounts")
            );
        }
        // Ctrl + L: Đổi Dark / Light mode
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN),
                this::toggleTheme
        );
        // Ctrl + Z: Hoàn tác nhanh giao dịch gần nhất (Command Undo)
        if (currentUser.role() != AuthService.Role.VIEWER) {
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
                    () -> {
                        try {
                            Transaction undone = ctx.getFacade().undoLastTransfer();
                            ctx.notifyDataChanged();
                            ToastNotification.showSuccess("Đã hoàn tác giao dịch " + undone.getRelatedTransactionId());
                        } catch (Exception ex) {
                            ToastNotification.showWarning(UiUtils.humanizeError(ex));
                        }
                    }
            );
        }
    }

    private void updateSidebarFooter() {
        lblSidebarDbInfo.setText("● DB Singleton @" + Integer.toHexString(DatabaseManager.getInstance().hashCode()));
        lblSidebarUndoInfo.setText("● Lệnh Undo: " + ctx.getTxHistory().size());
    }
}

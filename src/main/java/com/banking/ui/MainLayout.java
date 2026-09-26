package com.banking.ui;

import com.banking.pattern.creational.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Khung giao diện chính (BorderPane):
 * - Bên trái: Thanh điều hướng Sidebar Navy (#0A2342) với logo VB & menu
 * - Ở giữa: Vùng hiển thị nội dung các màn hình tương ứng
 */
public class MainLayout extends BorderPane {

    private final UIContext ctx = UIContext.getInstance();
    private final ScrollPane centerScrollPane = new ScrollPane();
    private final VBox navButtonBox = new VBox(6);
    private final Map<String, Button> navButtons = new HashMap<>();

    private DashboardView dashboardView;
    private AccountView accountView;
    private DepositWithdrawView depositWithdrawView;
    private TransferView transferView;
    private HistoryView historyView;
    private ProxyDemoView proxyDemoView;

    private final Label lblSidebarDbInfo = new Label();
    private final Label lblSidebarUndoInfo = new Label();

    public MainLayout() {
        // Initialize views
        dashboardView = new DashboardView(this::navigateTo);
        accountView = new AccountView();
        depositWithdrawView = new DepositWithdrawView();
        transferView = new TransferView();
        historyView = new HistoryView();
        proxyDemoView = new ProxyDemoView();

        // Setup center scroll pane
        centerScrollPane.getStyleClass().add("main-scroll-pane");
        centerScrollPane.setFitToWidth(true);
        centerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(centerScrollPane);

        // Setup sidebar
        setLeft(buildSidebar());

        // Default screen
        navigateTo("dashboard");

        // Sync sidebar footer with live data
        ctx.addDataChangeListener(this::updateSidebarFooter);
        updateSidebarFooter();
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

        // Nav buttons
        Label navTitle = new Label("DANH MỤC QUẢN LÝ");
        navTitle.getStyleClass().add("nav-section-title");

        addNavButton("dashboard", "📊  Tổng quan (Dashboard)");
        addNavButton("accounts", "👤  Mở tài khoản (Builder)");
        addNavButton("deposit", "💵  Nạp & Rút tiền (State)");
        addNavButton("transfer", "🔁  Chuyển khoản (Facade)");
        addNavButton("history", "📜  Lịch sử & Hoàn tác (Command)");
        addNavButton("proxy", "🛡️  Phân quyền (Proxy)");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Sidebar Footer (Singleton + Undo info)
        VBox footer = new VBox(6);
        footer.getStyleClass().add("sidebar-footer");

        lblSidebarDbInfo.getStyleClass().add("sidebar-status-text");
        lblSidebarDbInfo.setText("● DB Singleton @" + Integer.toHexString(DatabaseManager.getInstance().hashCode()));

        lblSidebarUndoInfo.getStyleClass().add("sidebar-status-text");
        lblSidebarUndoInfo.setText("● Lệnh Undo: 0");

        Label patternCount = new Label("★ 9 Patterns Active");
        patternCount.setStyle("-fx-text-fill: #C9A84C; -fx-font-size: 11px; -fx-font-weight: bold;");

        footer.getChildren().addAll(patternCount, lblSidebarDbInfo, lblSidebarUndoInfo);
        footer.setPadding(new Insets(12));

        sidebar.getChildren().addAll(brand, navTitle, navButtonBox, spacer, footer);
        return sidebar;
    }

    private void addNavButton(String key, String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> navigateTo(key));
        navButtons.put(key, btn);
        navButtonBox.getChildren().add(btn);
    }

    public void navigateTo(String viewKey) {
        // Reset active style
        for (Button btn : navButtons.values()) {
            btn.getStyleClass().remove("nav-button-active");
        }

        Button activeBtn = navButtons.get(viewKey);
        if (activeBtn != null && !activeBtn.getStyleClass().contains("nav-button-active")) {
            activeBtn.getStyleClass().add("nav-button-active");
        }

        Node targetView = switch (viewKey) {
            case "accounts" -> accountView;
            case "deposit"  -> depositWithdrawView;
            case "transfer" -> transferView;
            case "history"  -> historyView;
            case "proxy"    -> proxyDemoView;
            default         -> dashboardView;
        };

        centerScrollPane.setContent(targetView);
        centerScrollPane.setVvalue(0); // Scroll to top
    }

    private void updateSidebarFooter() {
        lblSidebarDbInfo.setText("● DB Singleton @" + Integer.toHexString(DatabaseManager.getInstance().hashCode()));
        lblSidebarUndoInfo.setText("● Lệnh Undo: " + ctx.getTxHistory().size());
    }
}

package com.banking.ui;

import com.banking.service.AuthService;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** One navigation and session boundary for the FXML screens and existing admin tools. */
public final class NovaBankShell extends BorderPane {
    private final AuthService auth;
    private final AuthService.User user;
    private final Map<String, Parent> pages = new HashMap<>();
    private final StackPane content = new StackPane();
    private final VBox toasts = new VBox(8);

    public NovaBankShell(AuthService auth, AuthService.User user) {
        this.auth = auth;
        this.user = user;
        getStylesheets().add(getClass().getResource("/com/banking/ui/novabank.css").toExternalForm());
        setLeft(buildSidebar());
        content.getChildren().add(toasts);
        StackPane.setAlignment(toasts, Pos.BOTTOM_RIGHT);
        toasts.setAlignment(Pos.BOTTOM_RIGHT);
        toasts.setPickOnBounds(false);
        toasts.setPadding(new Insets(12));
        ToastNotification.setGlobalContainer(toasts);
        setCenter(content);
        show("dashboard");
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(24);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250.0);
        sidebar.setPadding(new Insets(28, 16, 24, 20));

        // Brand Logo
        HBox brandBox = new HBox(12);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.getStyleClass().add("brand-container");

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("brand-icon-box");
        Label iconText = new Label("N");
        iconText.getStyleClass().add("brand-icon-text");
        iconBox.getChildren().add(iconText);

        Label brandTitle = new Label("NovaBank");
        brandTitle.getStyleClass().add("brand-title");

        brandBox.getChildren().addAll(iconBox, brandTitle);
        sidebar.getChildren().add(brandBox);

        // Nav Items Container
        VBox navMenu = new VBox(6);
        VBox.setVgrow(navMenu, Priority.ALWAYS);
        sidebar.getChildren().add(navMenu);

        // Standard Menu
        addButton(navMenu, "Overview", "dashboard", "M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z");
        if (user.role() != AuthService.Role.VIEWER) {
            addButton(navMenu, "Transfer & Pay", "transfer", "M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4");
            addButton(navMenu, "Transfer templates", "templates", "M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4");
            addButton(navMenu, "Cash", "cash", "M12 6v6m0 0v6m0-6h6m-6 0H6");
        }
        addButton(navMenu, "Transactions", "history", "M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z");
        addButton(navMenu, "Accounts", "accounts", "M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z");

        // Admin Menu
        if (user.role() == AuthService.Role.ADMIN) {
            Label adminLabel = new Label("ADMIN TOOLS");
            adminLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 10 0 0 10;");
            navMenu.getChildren().add(adminLabel);
            addButton(navMenu, "Users", "users", "M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z");
            addButton(navMenu, "Proxy Demo", "proxy", "M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z");
            addButton(navMenu, "Open Account", "open", "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z");
        }

        // Bottom section (Profile + Logout)
        VBox bottomMenu = new VBox(8);
        Button profileBtn = createBottomBtn(user.username(), "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z");
        profileBtn.setOnAction(e -> show("profile"));

        Button logoutBtn = createBottomBtn("Sign out", "M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1");
        logoutBtn.setOnAction(e -> getScene().getWindow().hide());

        bottomMenu.getChildren().addAll(profileBtn, logoutBtn);
        sidebar.getChildren().add(bottomMenu);

        return sidebar;
    }

    private final Map<String, Button> navButtons = new HashMap<>();

    private Button createBottomBtn(String label, String svgData) {
        Button button = new Button(label);
        javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
        path.setContent(svgData);
        path.setStyle("-fx-stroke: #94A3B8; -fx-stroke-width: 2px; -fx-stroke-line-cap: round; -fx-stroke-line-join: round; -fx-fill: transparent;");
        button.setGraphic(path);
        button.setGraphicTextGap(12);
        button.getStyleClass().add("nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.BASELINE_LEFT);
        return button;
    }

    private void addButton(VBox bar, String label, String key, String svgData) {
        Button button = new Button(label);
        javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
        path.setContent(svgData);
        path.setStyle("-fx-stroke: #94A3B8; -fx-stroke-width: 2px; -fx-stroke-line-cap: round; -fx-stroke-line-join: round; -fx-fill: transparent;");
        button.setGraphic(path);
        button.setGraphicTextGap(12);
        button.getStyleClass().add("nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.BASELINE_LEFT);
        button.setOnAction(event -> show(key));
        navButtons.put(key, button);
        bar.getChildren().add(button);
    }

    public void show(String key) {
        if (!allowed(key)) return;

        navButtons.values().forEach(btn -> btn.getStyleClass().remove("nav-item-active"));
        if (navButtons.containsKey(key)) {
            navButtons.get(key).getStyleClass().add("nav-item-active");
        }

        Parent page = pages.computeIfAbsent(key, this::createPage);
        content.getChildren().setAll(page, toasts);
        UIContext.getInstance().notifyDataChanged();
    }

    private boolean allowed(String key) {
        if (user.role() == AuthService.Role.VIEWER && ("transfer".equals(key) || "templates".equals(key) || "cash".equals(key))) return false;
        if (user.role() != AuthService.Role.ADMIN && ("open".equals(key) || "users".equals(key) || "proxy".equals(key))) return false;
        return true;
    }

    private Parent createPage(String key) {
        return switch (key) {
            case "dashboard", "accounts", "transfer", "history" -> loadFxml(key);
            case "cash" -> scroll(new DepositWithdrawView());
            case "templates" -> scroll(new TransferView());
            case "open" -> scroll(new AccountView());
            case "users" -> scroll(new UserView(auth));
            case "profile" -> scroll(new ProfileView(auth, user));
            case "proxy" -> scroll(new ProxyDemoView());
            default -> throw new IllegalArgumentException("Màn hình không tồn tại: " + key);
        };
    }

    private ScrollPane scroll(Parent page) {
        ScrollPane pane = new ScrollPane(page);
        pane.setFitToWidth(true);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return pane;
    }

    private Parent loadFxml(String key) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/banking/ui/novabank_" + key + ".fxml"));
        try {
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof NovaBankNavigable navigable) navigable.setNavigator(this::show);
            if (controller instanceof NovaBankDashboardController dashboard) dashboard.setUser(user);
            if (controller instanceof NovaBankTransferController transfer) transfer.setUser(user);
            if (controller instanceof NovaBankAccountsController accounts) accounts.setRole(user.role());
            if (controller instanceof NovaBankHistoryController history) history.setRole(user.role());
            if (user.role() == AuthService.Role.VIEWER) {
                for (Node node : root.lookupAll(".nav-item")) {
                    if (node instanceof Button button && button.getText().contains("Transfer")) {
                        node.setVisible(false);
                        node.setManaged(false);
                    }
                }
                for (Node node : root.lookupAll(".quick-action-btn")) {
                    Node title = node.lookup(".action-title");
                    if (title instanceof Label label && (label.getText().equals("Transfer") || label.getText().equals("Top up"))) {
                        node.setVisible(false);
                        node.setManaged(false);
                    }
                }
            }
            return root;
        } catch (IOException e) {
            throw new IllegalStateException("Không tải được màn hình " + key, e);
        }
    }
}

package com.banking.ui;

import com.banking.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.util.Arrays;

/**
 * Màn hình Quản lý người dùng & Phân quyền truy cập (User Management & Access Control).
 * Thiết kế chuẩn theo Figma Brief & UI Specification:
 * - Breadcrumb & thông tin phiên đăng nhập ở header.
 * - Cột trái: Form tạo người dùng với ComboBox chọn vai trò, nút CTA chuẩn nhận diện thương hiệu,
 *             và khung phản hồi kết quả nội tuyến (Inline Result Box) thay cho dialog hộp thoại.
 * - Cột phải: Card hướng dẫn "Hiểu quyền trước khi cấp" giải thích chi tiết 3 vai trò (ADMIN, STAFF, VIEWER),
 *             callout box lưu ý về cơ chế xác thực AuthService, và ghi chú phạm vi hệ thống.
 */
final class UserView extends VBox {

    private final AuthService auth;
    private final AuthService.User currentUser;

    // Form inputs
    private final TextField txtUsername = new TextField();
    private final PasswordField txtPassword = new PasswordField();
    private final ComboBox<AuthService.Role> cbRole = new ComboBox<>();

    // Inline Result Box components
    private final HBox inlineResultBox = new HBox(12);
    private final StackPane inlineIconContainer = new StackPane();
    private final SVGPath inlineIconSvg = new SVGPath();
    private final Label lblResultTitle = new Label();
    private final Label lblResultDetail = new Label();

    UserView(AuthService auth, AuthService.User currentUser) {
        this.auth = auth;
        this.currentUser = currentUser;

        setSpacing(20);
        getStyleClass().add("content-pane");
        setStyle("-fx-background-color: #F8FAFC; -fx-padding: 24 32;");

        buildHeader();

        HBox mainGrid = new HBox(24);
        mainGrid.setAlignment(Pos.TOP_LEFT);

        VBox leftCard = buildLeftCard();
        VBox rightCard = buildRightCard();

        HBox.setHgrow(leftCard, Priority.ALWAYS);
        HBox.setHgrow(rightCard, Priority.ALWAYS);
        leftCard.setMaxWidth(Double.MAX_VALUE);
        rightCard.setMaxWidth(Double.MAX_VALUE);

        mainGrid.getChildren().addAll(leftCard, rightCard);
        getChildren().add(mainGrid);
    }

    UserView(AuthService auth) {
        this(auth, null);
    }

    private void buildHeader() {
        VBox headerBox = new VBox(6);

        // Top line: Breadcrumb & Session info
        HBox topMetaRow = new HBox();
        topMetaRow.setAlignment(Pos.CENTER_LEFT);

        Label lblBreadcrumb = new Label("NovaBank / Quản trị");
        lblBreadcrumb.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-weight: 500;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String sessionRole = (currentUser != null && currentUser.role() != null)
                ? currentUser.role().name()
                : "ADMIN";
        Label lblSession = new Label("Phiên đăng nhập: " + sessionRole);
        lblSession.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155; -fx-font-weight: 700;");

        topMetaRow.getChildren().addAll(lblBreadcrumb, spacer, lblSession);

        // Main Title
        Label lblTitle = new Label("Người dùng & quyền truy cập");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        // Subtitle
        Label lblSubtitle = new Label("Tạo tài khoản đăng nhập và chọn vai trò phù hợp");
        lblSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        headerBox.getChildren().addAll(topMetaRow, lblTitle, lblSubtitle);
        getChildren().add(headerBox);
    }

    private VBox buildLeftCard() {
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; -fx-padding: 24px; " +
                "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.03), 8, 0, 0, 2);");

        Label cardTitle = new Label("Tạo người dùng");
        cardTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        VBox form = new VBox(14);

        // 1. Tên đăng nhập
        VBox grpUsername = new VBox(4);
        Label lblUname = new Label("Tên đăng nhập");
        lblUname.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1E293B;");
        txtUsername.setPromptText("Nhập thông tin");
        txtUsername.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; -fx-padding: 9 12; -fx-font-size: 13px; -fx-text-fill: #0F172A;");
        Label lblUnameHint = new Label("3-32 chữ/số/gạch dưới");
        lblUnameHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        grpUsername.getChildren().addAll(lblUname, txtUsername, lblUnameHint);

        // 2. Mật khẩu
        VBox grpPassword = new VBox(4);
        Label lblPwd = new Label("Mật khẩu");
        lblPwd.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1E293B;");

        StackPane pwdWrapper = new StackPane();
        pwdWrapper.setAlignment(Pos.CENTER_RIGHT);
        txtPassword.setPromptText("Nhập thông tin");
        txtPassword.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; -fx-padding: 9 36 9 12; -fx-font-size: 13px; -fx-text-fill: #0F172A;");

        SVGPath lockIcon = new SVGPath();
        lockIcon.setContent("M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z");
        lockIcon.setStyle("-fx-fill: #94A3B8; -fx-scale-x: 0.65; -fx-scale-y: 0.65;");
        StackPane.setMargin(lockIcon, new Insets(0, 10, 0, 0));
        pwdWrapper.getChildren().addAll(txtPassword, lockIcon);

        Label lblPwdHint = new Label("Tối thiểu 8 ký tự");
        lblPwdHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        grpPassword.getChildren().addAll(lblPwd, pwdWrapper, lblPwdHint);

        // 3. Vai trò
        VBox grpRole = new VBox(4);
        Label lblRole = new Label("Vai trò");
        lblRole.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1E293B;");

        cbRole.getItems().setAll(AuthService.Role.STAFF, AuthService.Role.ADMIN, AuthService.Role.VIEWER);
        cbRole.setValue(AuthService.Role.STAFF);
        cbRole.setMaxWidth(Double.MAX_VALUE);
        cbRole.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; -fx-font-size: 13px; -fx-cursor: hand;");
        cbRole.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(AuthService.Role item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name());
                    setStyle("-fx-font-size: 13px; -fx-padding: 6 10; -fx-text-fill: #0F172A;");
                }
            }
        });
        cbRole.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AuthService.Role item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name());
                    setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #0F172A;");
                }
            }
        });
        grpRole.getChildren().addAll(lblRole, cbRole);

        // Keyboard navigation
        txtUsername.setOnAction(e -> txtPassword.requestFocus());
        txtPassword.setOnAction(e -> handleCreateUser());

        // 4. Nút Tạo người dùng
        Button btnCreate = new Button("+ Tạo người dùng");
        btnCreate.setStyle("-fx-background-color: #00C476; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; " +
                "-fx-font-size: 13px; -fx-padding: 9 18; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnCreate.setOnMouseEntered(e -> btnCreate.setStyle("-fx-background-color: #00B069; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 9 18; -fx-background-radius: 6px; -fx-cursor: hand;"));
        btnCreate.setOnMouseExited(e -> btnCreate.setStyle("-fx-background-color: #00C476; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 9 18; -fx-background-radius: 6px; -fx-cursor: hand;"));
        btnCreate.setOnAction(e -> handleCreateUser());

        // 5. Khung kết quả nội tuyến
        buildInlineResultBox();

        form.getChildren().addAll(grpUsername, grpPassword, grpRole, btnCreate, inlineResultBox);
        card.getChildren().addAll(cardTitle, form);
        return card;
    }

    private void buildInlineResultBox() {
        inlineResultBox.setAlignment(Pos.CENTER_LEFT);
        inlineIconContainer.setAlignment(Pos.CENTER);
        inlineIconContainer.getChildren().add(inlineIconSvg);

        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        lblResultTitle.setWrapText(true);
        lblResultDetail.setWrapText(true);
        textBox.getChildren().addAll(lblResultTitle, lblResultDetail);

        inlineResultBox.getChildren().addAll(inlineIconContainer, textBox);
        showEmptyResult();
    }

    private void showEmptyResult() {
        inlineResultBox.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; -fx-padding: 14 16;");

        // User outline icon
        inlineIconSvg.setContent("M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z");
        inlineIconSvg.setStyle("-fx-fill: transparent; -fx-stroke: #64748B; -fx-stroke-width: 1.5; -fx-scale-x: 0.85; -fx-scale-y: 0.85;");

        lblResultTitle.setText("Chưa tạo người dùng");
        lblResultTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");

        lblResultDetail.setText("Kết quả gồm tên đăng nhập và vai trò sẽ xuất hiện tại đây. Không hiển thị mật khẩu.");
        lblResultDetail.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B; -fx-line-spacing: 1.5;");
    }

    private void showSuccessResult(String username, AuthService.Role role) {
        inlineResultBox.setStyle("-fx-background-color: #F0FDF4; -fx-border-color: #BBF7D0; -fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; -fx-padding: 14 16;");

        // Check circle icon
        inlineIconSvg.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z");
        inlineIconSvg.setStyle("-fx-fill: #16A34A; -fx-stroke: transparent; -fx-scale-x: 0.9; -fx-scale-y: 0.9;");

        lblResultTitle.setText("Đã tạo người dùng: " + username);
        lblResultTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #15803D;");

        lblResultDetail.setText("Vai trò: " + role.name() + " · Tài khoản đã sẵn sàng đăng nhập và hoạt động.");
        lblResultDetail.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #166534; -fx-line-spacing: 1.5;");
    }

    private void showErrorResult(String errorMessage) {
        inlineResultBox.setStyle("-fx-background-color: #FEF2F2; -fx-border-color: #FECACA; -fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; -fx-padding: 14 16;");

        // Alert icon
        inlineIconSvg.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z");
        inlineIconSvg.setStyle("-fx-fill: #DC2626; -fx-stroke: transparent; -fx-scale-x: 0.9; -fx-scale-y: 0.9;");

        lblResultTitle.setText("Không thể tạo người dùng");
        lblResultTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #B91C1C;");

        lblResultDetail.setText(errorMessage);
        lblResultDetail.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #991B1B; -fx-line-spacing: 1.5;");
    }

    private VBox buildRightCard() {
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; -fx-padding: 24px; " +
                "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.03), 8, 0, 0, 2);");

        Label cardTitle = new Label("Hiểu quyền trước khi cấp");
        cardTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        // Table List
        VBox tableList = new VBox(0);

        // Header Row
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 8 12; -fx-background-radius: 4px;");

        Label colHeaderRole = new Label("Vai trò");
        colHeaderRole.setPrefWidth(90);
        colHeaderRole.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #475569;");

        Label colHeaderScope = new Label("Phạm vi quyền truy cập");
        colHeaderScope.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #475569;");

        headerRow.getChildren().addAll(colHeaderRole, colHeaderScope);
        tableList.getChildren().add(headerRow);

        // Rows
        tableList.getChildren().add(buildRoleRow("ADMIN", "Quản trị", "Quản lý người dùng và các công cụ quản trị."));
        tableList.getChildren().add(createSeparator());
        tableList.getChildren().add(buildRoleRow("STAFF", "Giao dịch", "Thực hiện nghiệp vụ giao dịch theo quyền của dịch vụ."));
        tableList.getChildren().add(createSeparator());
        tableList.getChildren().add(buildRoleRow("VIEWER", "Chỉ xem", "Xem thông tin; không thực hiện thao tác ghi."));

        // Callout Notice Box
        HBox calloutBox = new HBox(10);
        calloutBox.setAlignment(Pos.CENTER_LEFT);
        calloutBox.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #DBEAFE; -fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; -fx-padding: 12 14;");

        SVGPath infoIcon = new SVGPath();
        infoIcon.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z");
        infoIcon.setStyle("-fx-fill: #2563EB; -fx-scale-x: 0.85; -fx-scale-y: 0.85;");

        Label lblCallout = new Label("ADMIN quản trị · STAFF giao dịch · VIEWER chỉ xem.");
        lblCallout.setStyle("-fx-font-size: 12px; -fx-text-fill: #1D4ED8; -fx-line-spacing: 2;");
        lblCallout.setWrapText(true);
        HBox.setHgrow(lblCallout, Priority.ALWAYS);

        calloutBox.getChildren().addAll(infoIcon, lblCallout);

        card.getChildren().addAll(cardTitle, tableList, calloutBox);
        return card;
    }

    private HBox buildRoleRow(String roleName, String scopeTitle, String scopeDesc) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 12 12;");

        Label lblRole = new Label(roleName);
        lblRole.setPrefWidth(90);
        lblRole.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #059669;");

        VBox scopeBox = new VBox(2);
        HBox.setHgrow(scopeBox, Priority.ALWAYS);

        Label lblTitle = new Label(scopeTitle);
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        Label lblDesc = new Label(scopeDesc);
        lblDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
        lblDesc.setWrapText(true);

        scopeBox.getChildren().addAll(lblTitle, lblDesc);
        row.getChildren().addAll(lblRole, scopeBox);
        return row;
    }

    private Region createSeparator() {
        Region sep = new Region();
        sep.setStyle("-fx-background-color: #F1F5F9; -fx-pref-height: 1px; -fx-min-height: 1px;");
        return sep;
    }

    private void handleCreateUser() {
        String uname = txtUsername.getText().trim();
        String pwd = txtPassword.getText();

        if (uname.isEmpty()) {
            showErrorResult("Vui lòng nhập tên đăng nhập.");
            txtUsername.requestFocus();
            return;
        }

        if (!uname.matches("[A-Za-z0-9_]{3,32}")) {
            showErrorResult("Tên đăng nhập cần 3–32 ký tự chữ, số hoặc dấu gạch dưới.");
            txtUsername.requestFocus();
            return;
        }

        if (pwd == null || pwd.length() < 8) {
            showErrorResult("Mật khẩu phải có tối thiểu 8 ký tự.");
            txtPassword.requestFocus();
            return;
        }

        AuthService.Role selectedRole = cbRole.getValue();
        if (selectedRole == null) {
            showErrorResult("Vui lòng chọn vai trò.");
            return;
        }

        char[] secret = pwd.toCharArray();
        try {
            auth.createUser(uname, secret, selectedRole);
            showSuccessResult(uname, selectedRole);
            txtUsername.clear();
            txtPassword.clear();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showErrorResult(exception.getMessage());
        } finally {
            Arrays.fill(secret, '\0');
        }
    }
}

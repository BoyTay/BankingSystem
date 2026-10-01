package com.banking.ui;

import com.banking.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Màn hình Quản lý người dùng & Phân quyền truy cập (User Management & Access Control).
 * Thiết kế chuẩn theo Stitch Design System & Figma Brief:
 * - Cột trái: Form tạo người dùng với vai trò RBAC, kiểm tra định dạng thời gian thực, banner kết quả & Telemetry Active Guard.
 * - Cột phải: Danh sách tài khoản hệ thống (kèm lọc tìm kiếm, cập nhật tức thì khi tạo) & Ma trận phân quyền RBAC Matrix.
 */
final class UserView extends VBox {

    private final AuthService auth;
    private final AuthService.User currentUser;

    // Form inputs
    private final TextField txtUsername = new TextField();
    private final PasswordField txtPassword = new PasswordField();
    private final TextField txtPasswordPlain = new TextField();
    private final ToggleGroup roleToggleGroup = new ToggleGroup();
    private final RadioButton rbStaff = new RadioButton("STAFF");
    private final RadioButton rbAdmin = new RadioButton("ADMIN");
    private final RadioButton rbViewer = new RadioButton("VIEWER");

    // Dynamic result banner
    private final VBox bannerResult = new VBox(6);
    private final Label lblResultTitle = new Label();
    private final Label lblResultDetail = new Label();
    private final Label lblResultMeta = new Label();

    // Directory Table & Filter
    private final ObservableList<SystemUserEntry> userDirectory = FXCollections.observableArrayList();
    private final FilteredList<SystemUserEntry> filteredDirectory = new FilteredList<>(userDirectory, p -> true);
    private final TableView<SystemUserEntry> tableDirectory = new TableView<>();
    private final TextField txtFilter = new TextField();

    public record SystemUserEntry(String username, AuthService.Role role, String description, String status, String createdAt) {}

    UserView(AuthService auth, AuthService.User currentUser) {
        this.auth = auth;
        this.currentUser = currentUser;

        setSpacing(18);
        getStyleClass().add("content-pane");

        initDirectoryData();
        buildHeader();

        // 2 CỘT CHÍNH
        HBox mainGrid = new HBox(20);
        mainGrid.setAlignment(Pos.TOP_LEFT);

        VBox leftCol = buildLeftColumn();
        VBox rightCol = buildRightColumn();

        leftCol.setPrefWidth(430);
        leftCol.setMinWidth(380);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        mainGrid.getChildren().addAll(leftCol, rightCol);
        getChildren().add(mainGrid);
    }

    UserView(AuthService auth) {
        this(auth, null);
    }

    private void initDirectoryData() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        userDirectory.addAll(
                new SystemUserEntry("admin", AuthService.Role.ADMIN, "Quản trị toàn hệ thống & phân quyền", "Hoạt động", "01/01/2025"),
                new SystemUserEntry("teller_nam", AuthService.Role.STAFF, "Nhân viên quầy giao dịch nạp/rút", "Hoạt động", "15/02/2025"),
                new SystemUserEntry("viewer_huong", AuthService.Role.VIEWER, "Kiểm toán viên & giám sát số dư", "Hoạt động", "10/03/2025")
        );
    }

    private void buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        HBox tagBox = new HBox(6);
        tagBox.setAlignment(Pos.CENTER_LEFT);
        tagBox.getStyleClass().add("header-badge-tag");
        tagBox.setMaxWidth(Double.NEGATIVE_INFINITY);
        Circle tagDot = new Circle(3.0);
        tagDot.getStyleClass().add("header-badge-tag-dot");
        Label tagText = new Label("QUẢN TRỊ HỆ THỐNG · PHÂN QUYỀN");
        tagText.getStyleClass().add("header-badge-tag-text");
        tagBox.getChildren().addAll(tagDot, tagText);

        Label title = new Label("Người dùng & quyền truy cập");
        title.getStyleClass().add("header-greeting");

        Label subtitle = new Label("Khởi tạo nhân sự điều hành, phân quyền đa tầng và kiểm soát truy cập hệ thống");
        subtitle.getStyleClass().add("header-subtitle");

        titleBox.getChildren().addAll(tagBox, title, subtitle);

        // Right side badges
        HBox rightControls = new HBox(10);
        rightControls.setAlignment(Pos.CENTER_RIGHT);

        HBox rbacBadge = new HBox(6);
        rbacBadge.setAlignment(Pos.CENTER_LEFT);
        rbacBadge.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #BFDBFE; -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 4px 10px;");
        Label lblRbacTag = new Label("RBAC Model: 3 Roles");
        lblRbacTag.setStyle("-fx-text-fill: #1D4ED8; -fx-font-size: 11px; -fx-font-weight: 700;");
        rbacBadge.getChildren().add(lblRbacTag);

        HBox guardBadge = new HBox(6);
        guardBadge.setAlignment(Pos.CENTER_LEFT);
        guardBadge.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 4px 10px;");
        Circle dotGuard = new Circle(3.5, Color.web("#00C476"));
        Label lblGuard = new Label("Security Gatekeeper: Active");
        lblGuard.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: 600;");
        guardBadge.getChildren().addAll(dotGuard, lblGuard);

        rightControls.getChildren().addAll(rbacBadge, guardBadge);
        header.getChildren().addAll(titleBox, rightControls);
        getChildren().add(header);
    }

    private VBox buildLeftColumn() {
        VBox leftCol = new VBox(16);

        // 1. Create User Card
        VBox createCard = new VBox(14);
        createCard.getStyleClass().add("card");
        createCard.setStyle("-fx-padding: 22;");

        HBox cardHeader = new HBox(10);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color: rgba(37, 99, 235, 0.12); -fx-background-radius: 8px; -fx-min-width: 36px; -fx-min-height: 36px; -fx-max-width: 36px; -fx-max-height: 36px;");
        SVGPath userSvg = new SVGPath();
        userSvg.setContent("M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z");
        userSvg.setStyle("-fx-fill: #2563EB; -fx-scale-x: 0.8; -fx-scale-y: 0.8;");
        iconBox.getChildren().add(userSvg);

        VBox titleArea = new VBox(2);
        Label title = new Label("Tạo người dùng mới");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitle = new Label("Cấp tài khoản vận hành máy trạm cục bộ (Local Operator)");
        subtitle.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
        titleArea.getChildren().addAll(title, subtitle);

        cardHeader.getChildren().addAll(iconBox, titleArea);

        // Form Fields
        VBox form = new VBox(12);

        // Username
        VBox grpUsername = new VBox(4);
        Label lblUname = new Label("Tên đăng nhập mới *");
        lblUname.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        txtUsername.setPromptText("Ví dụ: teller_mai");
        txtUsername.getStyleClass().add("form-input");
        Label lblUnameHint = new Label("3–32 ký tự, không dấu, chữ cái hoặc số.");
        lblUnameHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        grpUsername.getChildren().addAll(lblUname, txtUsername, lblUnameHint);

        // Password
        VBox grpPassword = new VBox(4);
        Label lblPwd = new Label("Mật khẩu ban đầu *");
        lblPwd.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        HBox pwdWrapper = createPasswordInput(txtPassword, txtPasswordPlain, "Tối thiểu 8 ký tự theo chuẩn ngân hàng");
        Label lblPwdHint = new Label("Người dùng có thể đổi mật khẩu sau tại mục Hồ sơ.");
        lblPwdHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        grpPassword.getChildren().addAll(lblPwd, pwdWrapper, lblPwdHint);

        // Role Selector with rich cards
        VBox grpRole = new VBox(6);
        Label lblRole = new Label("Vai trò & Quyền hạn *");
        lblRole.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");

        rbStaff.setToggleGroup(roleToggleGroup);
        rbAdmin.setToggleGroup(roleToggleGroup);
        rbViewer.setToggleGroup(roleToggleGroup);
        rbStaff.setSelected(true);

        VBox roleOptions = new VBox(6);
        roleOptions.getChildren().addAll(
                buildRoleChoiceCard(rbStaff, "STAFF", "Nhân viên quầy giao dịch (Nạp, rút, chuyển khoản)", "#2563EB", "#DBEAFE"),
                buildRoleChoiceCard(rbAdmin, "ADMIN", "Quản trị viên toàn quyền (Mở TK, Tạo user, Proxy Demo)", "#059669", "#D1FAE5"),
                buildRoleChoiceCard(rbViewer, "VIEWER", "Kiểm toán viên chỉ xem dữ liệu (Không được giao dịch)", "#475569", "#E2E8F0")
        );

        grpRole.getChildren().addAll(lblRole, roleOptions);

        // Action Buttons
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setStyle("-fx-padding: 8 0 0 0;");

        Button btnReset = new Button("⟲ Nhập lại");
        btnReset.getStyleClass().add("btn-secondary");
        btnReset.setOnAction(e -> {
            txtUsername.clear();
            txtPassword.clear();
            txtPasswordPlain.clear();
            rbStaff.setSelected(true);
            bannerResult.setVisible(false);
            bannerResult.setManaged(false);
        });

        Button btnCreate = new Button("✓ Tạo người dùng");
        btnCreate.getStyleClass().add("btn-primary");
        btnCreate.setStyle("-fx-font-size: 13px; -fx-padding: 8px 20px; -fx-font-weight: 700;");
        btnCreate.setOnAction(e -> handleCreateUser());

        actionRow.getChildren().addAll(btnReset, btnCreate);

        // Result Banner (initially hidden)
        buildResultBanner();

        form.getChildren().addAll(grpUsername, grpPassword, grpRole, actionRow, bannerResult);
        createCard.getChildren().addAll(cardHeader, form);

        // 2. Telemetry Card: Active Guard
        VBox guardCard = new VBox(10);
        guardCard.getStyleClass().add("card");
        guardCard.setStyle("-fx-padding: 16;");

        HBox guardRow = new HBox(12);
        guardRow.setAlignment(Pos.CENTER_LEFT);

        StackPane guardIconCircle = new StackPane();
        guardIconCircle.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 50%; -fx-min-width: 38px; -fx-min-height: 38px; -fx-max-width: 38px; -fx-max-height: 38px;");
        SVGPath lockSvg = new SVGPath();
        lockSvg.setContent("M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z");
        lockSvg.setStyle("-fx-fill: #1E293B; -fx-scale-x: 0.7; -fx-scale-y: 0.7;");
        guardIconCircle.getChildren().add(lockSvg);

        VBox guardText = new VBox(2);
        HBox.setHgrow(guardText, Priority.ALWAYS);
        Label lblGuardTitle = new Label("Bảo mật đa tầng (Active Guard)");
        lblGuardTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");
        Label lblGuardDesc = new Label("Proxy-Gatekeeper kiểm tra mọi truy vấn API và quyền hạn thời gian thực");
        lblGuardDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        guardText.getChildren().addAll(lblGuardTitle, lblGuardDesc);

        Label lblGuardStatus = new Label("Active Guard");
        lblGuardStatus.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 3 8; -fx-background-radius: 12px;");

        guardRow.getChildren().addAll(guardIconCircle, guardText, lblGuardStatus);
        guardCard.getChildren().add(guardRow);

        leftCol.getChildren().addAll(createCard, guardCard);
        return leftCol;
    }

    private HBox buildRoleChoiceCard(RadioButton rb, String roleName, String desc, String textColor, String badgeBg) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 10; -fx-cursor: hand;");
        card.setOnMouseClicked(e -> rb.setSelected(true));

        Label badge = new Label(roleName);
        badge.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + textColor + "; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 6px;");

        VBox descBox = new VBox(1);
        HBox.setHgrow(descBox, Priority.ALWAYS);
        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        descBox.getChildren().add(lblDesc);

        card.getChildren().addAll(rb, badge, descBox);
        return card;
    }

    private void buildResultBanner() {
        bannerResult.getStyleClass().add("result-banner-success");
        bannerResult.setVisible(false);
        bannerResult.setManaged(false);

        lblResultTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #065F46;");
        lblResultDetail.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #047857;");
        lblResultDetail.setWrapText(true);
        lblResultMeta.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #059669; -fx-font-family: 'JetBrains Mono', monospace;");

        bannerResult.getChildren().addAll(lblResultTitle, lblResultDetail, lblResultMeta);
    }

    private VBox buildRightColumn() {
        VBox rightCol = new VBox(16);

        // 1. Directory Card
        VBox dirCard = new VBox(12);
        dirCard.getStyleClass().add("card");
        dirCard.setStyle("-fx-padding: 20;");

        HBox dirHeader = new HBox(12);
        dirHeader.setAlignment(Pos.CENTER_LEFT);

        VBox dirTitleBox = new VBox(2);
        HBox.setHgrow(dirTitleBox, Priority.ALWAYS);
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label lblDirTitle = new Label("Danh sách tài khoản hệ thống");
        lblDirTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label lblDirTag = new Label("SQLite Directory");
        lblDirTag.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 6; -fx-background-radius: 10px;");
        titleRow.getChildren().addAll(lblDirTitle, lblDirTag);

        Label lblDirSubtitle = new Label("Dữ liệu tài khoản vận hành — Tự động cập nhật tức thì khi cấp mới");
        lblDirSubtitle.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
        dirTitleBox.getChildren().addAll(titleRow, lblDirSubtitle);

        // Filter Field
        txtFilter.setPromptText("🔍 Lọc tên hoặc vai trò...");
        txtFilter.setStyle("-fx-pref-width: 220px; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-padding: 5 10;");
        txtFilter.textProperty().addListener((obs, oldV, newV) -> {
            filteredDirectory.setPredicate(user -> {
                if (newV == null || newV.isBlank()) return true;
                String lower = newV.toLowerCase();
                return user.username().toLowerCase().contains(lower) || user.role().name().toLowerCase().contains(lower);
            });
        });

        dirHeader.getChildren().addAll(dirTitleBox, txtFilter);

        // Table
        buildDirectoryTable();
        tableDirectory.setPrefHeight(230);
        VBox.setVgrow(tableDirectory, Priority.ALWAYS);

        dirCard.getChildren().addAll(dirHeader, tableDirectory);

        // 2. Access Control Matrix Card
        VBox matrixCard = new VBox(12);
        matrixCard.getStyleClass().add("card");
        matrixCard.setStyle("-fx-padding: 18;");

        HBox matrixHeader = new HBox(8);
        matrixHeader.setAlignment(Pos.CENTER_LEFT);
        Label iconMatrix = new Label("📊");
        iconMatrix.setStyle("-fx-font-size: 16px;");
        Label lblMatrixTitle = new Label("Ma trận phân quyền (Role-Based Access Control - RBAC)");
        lblMatrixTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        matrixHeader.getChildren().addAll(iconMatrix, lblMatrixTitle);

        GridPane matrixGrid = new GridPane();
        matrixGrid.setHgap(10);
        matrixGrid.setVgap(8);
        matrixGrid.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12;");

        // Headers
        matrixGrid.add(createMatrixHeader("CHỨC NĂNG HỆ THỐNG"), 0, 0);
        matrixGrid.add(createMatrixRoleHeader("ADMIN", "#059669", "#D1FAE5"), 1, 0);
        matrixGrid.add(createMatrixRoleHeader("STAFF", "#2563EB", "#DBEAFE"), 2, 0);
        matrixGrid.add(createMatrixRoleHeader("VIEWER", "#475569", "#E2E8F0"), 3, 0);

        // Rows
        addMatrixRow(matrixGrid, 1, "Xem Dashboard & Lịch sử giao dịch", true, true, true);
        addMatrixRow(matrixGrid, 2, "Chuyển tiền & Nạp / Rút tiền mặt", true, true, false);
        addMatrixRow(matrixGrid, 3, "Mở tài khoản mới (Builder Pattern)", true, false, false);
        addMatrixRow(matrixGrid, 4, "Quản lý người dùng & Đổi quyền", true, false, false);

        matrixCard.getChildren().addAll(matrixHeader, matrixGrid);

        rightCol.getChildren().addAll(dirCard, matrixCard);
        return rightCol;
    }

    private void buildDirectoryTable() {
        tableDirectory.setItems(filteredDirectory);
        tableDirectory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<SystemUserEntry, String> colUname = new TableColumn<>("TÊN ĐĂNG NHẬP");
        colUname.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().username()));
        colUname.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);
                    StackPane circle = new StackPane();
                    circle.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 50%; -fx-min-width: 26px; -fx-min-height: 26px; -fx-max-width: 26px; -fx-max-height: 26px;");
                    Label init = new Label(item.substring(0, 1).toUpperCase());
                    init.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #2563EB;");
                    circle.getChildren().add(init);

                    Label name = new Label(item);
                    name.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 12px;");
                    box.getChildren().addAll(circle, name);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        TableColumn<SystemUserEntry, AuthService.Role> colRole = new TableColumn<>("VAI TRÒ");
        colRole.setPrefWidth(90);
        colRole.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().role()));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AuthService.Role role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(role.name());
                    if (role == AuthService.Role.ADMIN) {
                        badge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-font-size: 10.5px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 10px;");
                    } else if (role == AuthService.Role.STAFF) {
                        badge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-font-size: 10.5px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 10px;");
                    } else {
                        badge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-size: 10.5px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 10px;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<SystemUserEntry, String> colDesc = new TableColumn<>("MÔ TẢ QUYỀN HẠN");
        colDesc.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().description()));
        colDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #475569;");

        TableColumn<SystemUserEntry, String> colStatus = new TableColumn<>("TRẠNG THÁI");
        colStatus.setPrefWidth(110);
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().status()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox box = new HBox(5);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Circle dot = new Circle(3, Color.web("#10B981"));
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-text-fill: #059669; -fx-font-weight: 700; -fx-font-size: 11px;");
                    box.getChildren().addAll(dot, lbl);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        TableColumn<SystemUserEntry, String> colDate = new TableColumn<>("NGÀY TẠO");
        colDate.setPrefWidth(90);
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().createdAt()));
        colDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', monospace; -fx-alignment: CENTER_RIGHT;");

        tableDirectory.getColumns().setAll(colUname, colRole, colDesc, colStatus, colDate);
    }

    private Label createMatrixHeader(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #64748B; -fx-padding: 0 0 4 0;");
        return lbl;
    }

    private Label createMatrixRoleHeader(String role, String color, String bg) {
        Label lbl = new Label(role);
        lbl.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; -fx-font-size: 10.5px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 8px; -fx-alignment: CENTER;");
        return lbl;
    }

    private void addMatrixRow(GridPane grid, int row, String action, boolean admin, boolean staff, boolean viewer) {
        Label lblAction = new Label(action);
        lblAction.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #1E293B;");
        grid.add(lblAction, 0, row);

        grid.add(createCheckBadge(admin), 1, row);
        grid.add(createCheckBadge(staff), 2, row);
        grid.add(createCheckBadge(viewer), 3, row);
    }

    private Label createCheckBadge(boolean allowed) {
        Label badge = new Label(allowed ? "✓ Cho phép" : "✕ Bị chặn");
        badge.setStyle(allowed
                ? "-fx-text-fill: #059669; -fx-font-weight: 700; -fx-font-size: 11px; -fx-alignment: CENTER;"
                : "-fx-text-fill: #DC2626; -fx-font-weight: 700; -fx-font-size: 11px; -fx-alignment: CENTER;");
        return badge;
    }

    private HBox createPasswordInput(PasswordField passField, TextField plainField, String prompt) {
        passField.setPromptText(prompt);
        plainField.setPromptText(prompt);
        passField.getStyleClass().add("form-input");
        plainField.getStyleClass().add("form-input");

        plainField.setVisible(false);
        plainField.setManaged(false);

        passField.textProperty().bindBidirectional(plainField.textProperty());

        Button btnToggle = new Button("👁");
        btnToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0 4;");
        btnToggle.setOnAction(e -> {
            boolean showingPlain = plainField.isVisible();
            plainField.setVisible(!showingPlain);
            plainField.setManaged(!showingPlain);
            passField.setVisible(showingPlain);
            passField.setManaged(showingPlain);
            btnToggle.setStyle(!showingPlain ? "-fx-background-color: transparent; -fx-text-fill: #2563EB; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0 4;"
                    : "-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0 4;");
        });

        HBox wrapper = new HBox(8);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(passField, Priority.ALWAYS);
        HBox.setHgrow(plainField, Priority.ALWAYS);

        StackPane inputStack = new StackPane(passField, plainField);
        HBox.setHgrow(inputStack, Priority.ALWAYS);

        wrapper.getChildren().addAll(inputStack, btnToggle);
        return wrapper;
    }

    private void handleCreateUser() {
        String uname = txtUsername.getText().trim();
        String pwd = txtPassword.isVisible() ? txtPassword.getText() : txtPasswordPlain.getText();

        if (uname.isEmpty()) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập tên đăng nhập", "Tên đăng nhập không được để trống.");
            return;
        }

        if (uname.length() < 3 || uname.length() > 32) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Định dạng không hợp lệ", "Tên đăng nhập không hợp lệ", "Độ dài tên đăng nhập phải từ 3 đến 32 ký tự.");
            return;
        }

        if (pwd.length() < 8) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Mật khẩu chưa đủ độ dài", "Mật khẩu quá ngắn", "Mật khẩu phải có tối thiểu 8 ký tự theo tiêu chuẩn bảo mật.");
            return;
        }

        AuthService.Role selectedRole = rbAdmin.isSelected() ? AuthService.Role.ADMIN
                : (rbViewer.isSelected() ? AuthService.Role.VIEWER : AuthService.Role.STAFF);

        char[] secret = pwd.toCharArray();
        try {
            auth.createUser(uname, secret, selectedRole);

            // Update Directory
            String desc = switch (selectedRole) {
                case ADMIN -> "Quản trị toàn hệ thống & phân quyền";
                case STAFF -> "Nhân viên quầy giao dịch nạp/rút";
                case VIEWER -> "Kiểm toán viên & giám sát số dư";
            };
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            SystemUserEntry newEntry = new SystemUserEntry(uname, selectedRole, desc, "Hoạt động", dateStr);
            userDirectory.add(0, newEntry);
            tableDirectory.getSelectionModel().select(newEntry);

            // Show result banner
            bannerResult.setVisible(true);
            bannerResult.setManaged(true);
            lblResultTitle.setText("✓ Khởi tạo tài khoản " + uname + " thành công!");
            lblResultDetail.setText("Người dùng [" + uname + "] với vai trò [" + selectedRole + "] đã được lưu vào cơ sở dữ liệu SQLite.");
            lblResultMeta.setText("Đồng bộ qua AuthService (Singleton) · Status: 201 CREATED");

            txtUsername.clear();
            txtPassword.clear();
            txtPasswordPlain.clear();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể tạo người dùng", null, exception.getMessage());
        } finally {
            Arrays.fill(secret, '\0');
        }
    }
}

package com.banking.ui;

import com.banking.model.Account;
import com.banking.pattern.structural.AccountProxy;
import com.banking.pattern.structural.RealAccount;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Màn hình Mô phỏng phân quyền Proxy (Protection Proxy Pattern - RBAC).
 * Thiết kế giao diện theo phong cách Figma với dữ liệu vận hành 100% thực tế từ SQLite:
 * - Header: Breadcrumb "NovaBank / Quản trị", phiên đăng nhập ADMIN, tiêu đề và badge Proxy.
 * - Cột trái: Kịch bản thử quyền với tài khoản thật (số dư thực tế từ cơ sở dữ liệu),
 *             vai trò mô phỏng (ADMIN, USER, READONLY), số tiền thử nghiệm và 3 nút hành động.
 *             Khung Outcome Panel nội tuyến phản hồi kết quả thực thi thời gian thực.
 * - Cột phải trên: Nhật ký truy cập (Audit Console) ghi nhận các thao tác thực tế theo thời gian thực.
 * - Cột phải dưới: Sơ đồ luồng "Cách Proxy kiểm tra quyền" với 4 bước trực quan và 3 điểm cốt lõi.
 */
public class ProxyDemoView extends VBox {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final UIContext ctx = UIContext.getInstance();

    // Inputs
    private final ComboBox<Account> cbAccount = new ComboBox<>();
    private final ToggleGroup roleGroup = new ToggleGroup();
    private final RadioButton rbAdmin = new RadioButton("ADMIN");
    private final RadioButton rbUser = new RadioButton("USER");
    private final RadioButton rbReadOnly = new RadioButton("READONLY");
    private final TextField txtAmount = new TextField("100,000");

    // Immediate Outcome Panel
    private final VBox outcomePanel = new VBox(8);
    private final Label lblOutcomeTitle = new Label();
    private final Label lblOutcomeDetail = new Label();
    private final Label lblOutcomeDesc = new Label();
    private final Label lblOutcomeBalance = new Label();

    // Audit Log Console
    private final VBox logContainer = new VBox(8);
    private final ScrollPane logScroll = new ScrollPane(logContainer);
    private final Label lblEmptyLog = new Label("Chưa có nhật ký truy cập. Hãy chọn tài khoản và thử một thao tác bên trái.");

    public ProxyDemoView() {
        setSpacing(20);
        getStyleClass().add("content-pane");
        setStyle("-fx-background-color: #F8FAFC; -fx-padding: 24 32;");

        buildHeader();

        // 2 CỘT CHÍNH
        HBox mainGrid = new HBox(24);
        mainGrid.setAlignment(Pos.TOP_LEFT);

        VBox leftCol = buildLeftCard();
        VBox rightCol = buildRightColumn();

        HBox.setHgrow(leftCol, Priority.ALWAYS);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        leftCol.setMaxWidth(Double.MAX_VALUE);
        rightCol.setMaxWidth(Double.MAX_VALUE);

        mainGrid.getChildren().addAll(leftCol, rightCol);
        getChildren().add(mainGrid);

        showEmptyLogPrompt();

        ctx.addDataChangeListener(this::refresh);
        refresh();
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

        Label lblSession = new Label("Phiên đăng nhập: ADMIN");
        lblSession.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155; -fx-font-weight: 700;");

        topMetaRow.getChildren().addAll(lblBreadcrumb, spacer, lblSession);

        // Title row with badge
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("Mô phỏng phân quyền Proxy");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        Label badgeProxy = new Label("Proxy");
        badgeProxy.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 12px;");

        titleRow.getChildren().addAll(lblTitle, badgeProxy);

        // Subtitle
        Label lblSubtitle = new Label("Thử thao tác theo vai trò và đọc kết quả kiểm tra quyền ngay lập tức");
        lblSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        headerBox.getChildren().addAll(topMetaRow, titleRow, lblSubtitle);
        getChildren().add(headerBox);
    }

    private VBox buildLeftCard() {
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; -fx-padding: 24px; " +
                "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.03), 8, 0, 0, 2);");

        Label cardTitle = new Label("Kịch bản thử quyền");
        cardTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        VBox form = new VBox(14);

        // 1. Tài khoản mục tiêu (Hiển thị số tài khoản, chủ sở hữu và số dư thật)
        VBox grpAcc = new VBox(4);
        Label lblAcc = new Label("Tài khoản mục tiêu");
        lblAcc.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1E293B;");

        cbAccount.setMaxWidth(Double.MAX_VALUE);
        cbAccount.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 4 8;");
        cbAccount.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getAccountNumber() + " — " + item.getOwnerName() + " (" + UiUtils.formatVnd(item.getBalance()) + ")");
            }
        });
        cbAccount.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getAccountNumber() + " — " + item.getOwnerName() + " (" + UiUtils.formatVnd(item.getBalance()) + ")");
            }
        });
        grpAcc.getChildren().addAll(lblAcc, cbAccount);

        // 2. Vai trò mô phỏng (Inline RadioButtons)
        VBox grpRole = new VBox(6);
        Label lblRole = new Label("Vai trò mô phỏng");
        lblRole.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1E293B;");

        rbAdmin.setToggleGroup(roleGroup);
        rbUser.setToggleGroup(roleGroup);
        rbReadOnly.setToggleGroup(roleGroup);
        rbReadOnly.setSelected(true);

        rbAdmin.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1E293B; -fx-cursor: hand;");
        rbUser.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1E293B; -fx-cursor: hand;");
        rbReadOnly.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1E293B; -fx-cursor: hand;");

        HBox radioRow = new HBox(20);
        radioRow.setAlignment(Pos.CENTER_LEFT);
        radioRow.getChildren().addAll(rbAdmin, rbUser, rbReadOnly);

        Label lblRoleNote = new Label("Vai trò ở đây dùng để trình diễn AccountProxy; không thay đổi vai trò đăng nhập NovaBank");
        lblRoleNote.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");

        grpRole.getChildren().addAll(lblRole, radioRow, lblRoleNote);

        // 3. Số tiền thử (VND)
        VBox grpAmount = new VBox(4);
        Label lblAmount = new Label("Số tiền thử (VND)");
        lblAmount.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1E293B;");
        txtAmount.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; -fx-padding: 9 12; -fx-font-size: 13px; -fx-text-fill: #0F172A;");
        grpAmount.getChildren().addAll(lblAmount, txtAmount);

        // 4. Action Buttons (Xem số dư, Nạp tiền, Rút tiền)
        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        Button btnGetBalance = new Button("Xem số dư");
        btnGetBalance.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; -fx-text-fill: #0F172A; -fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand;");
        btnGetBalance.setOnMouseEntered(e -> btnGetBalance.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #94A3B8; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #0F172A; -fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand;"));
        btnGetBalance.setOnMouseExited(e -> btnGetBalance.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #0F172A; -fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand;"));
        btnGetBalance.setOnAction(e -> testGetBalance());

        Button btnDeposit = new Button("Nạp tiền");
        btnDeposit.setStyle("-fx-background-color: #00C476; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; " +
                "-fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnDeposit.setOnMouseEntered(e -> btnDeposit.setStyle("-fx-background-color: #00B069; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6px; -fx-cursor: hand;"));
        btnDeposit.setOnMouseExited(e -> btnDeposit.setStyle("-fx-background-color: #00C476; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6px; -fx-cursor: hand;"));
        btnDeposit.setOnAction(e -> testDeposit());

        Button btnWithdraw = new Button("Rút tiền");
        btnWithdraw.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #FCA5A5; -fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; -fx-text-fill: #DC2626; -fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 8 18; -fx-cursor: hand;");
        btnWithdraw.setOnMouseEntered(e -> btnWithdraw.setStyle("-fx-background-color: #FEF2F2; -fx-border-color: #EF4444; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #DC2626; -fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 8 18; -fx-cursor: hand;"));
        btnWithdraw.setOnMouseExited(e -> btnWithdraw.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #FCA5A5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #DC2626; -fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 8 18; -fx-cursor: hand;"));
        btnWithdraw.setOnAction(e -> testWithdraw());

        btnRow.getChildren().addAll(btnGetBalance, btnDeposit, btnWithdraw);

        // 5. Outcome Panel (Hiển thị kết quả thực thi khi người dùng bấm thao tác)
        buildOutcomePanel();

        form.getChildren().addAll(grpAcc, grpRole, grpAmount, btnRow, outcomePanel);
        card.getChildren().addAll(cardTitle, form);
        return card;
    }

    private void buildOutcomePanel() {
        outcomePanel.setVisible(false);
        outcomePanel.setManaged(false);

        lblOutcomeTitle.setWrapText(true);
        lblOutcomeDetail.setWrapText(true);
        lblOutcomeDesc.setWrapText(true);
        lblOutcomeBalance.setWrapText(true);

        outcomePanel.getChildren().addAll(lblOutcomeTitle, lblOutcomeDetail, lblOutcomeDesc, lblOutcomeBalance);
    }

    private void showOutcome(boolean allowed, String methodCall, AccountProxy.Role role, String reason, String balanceState) {
        outcomePanel.setVisible(true);
        outcomePanel.setManaged(true);

        if (allowed) {
            outcomePanel.setStyle("-fx-background-color: #F0FDF4; -fx-border-color: #BBF7D0; -fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; -fx-padding: 16;");
            lblOutcomeTitle.setText("✓ Được phép — thao tác thành công");
            lblOutcomeTitle.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #15803D;");
            lblOutcomeDetail.setText("Phương thức: " + methodCall + " · Vai trò: " + role.name());
            lblOutcomeDetail.setStyle("-fx-font-size: 12px; -fx-text-fill: #166534; -fx-font-family: 'JetBrains Mono', monospace;");
            lblOutcomeDesc.setText(reason);
            lblOutcomeDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #166534; -fx-line-spacing: 2px;");
            lblOutcomeBalance.setText(balanceState);
            lblOutcomeBalance.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-family: 'JetBrains Mono', monospace;");
        } else {
            outcomePanel.setStyle("-fx-background-color: #FEF2F2; -fx-border-color: #FECACA; -fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; -fx-padding: 16;");
            lblOutcomeTitle.setText("✕ Bị từ chối — không đổi số dư");
            lblOutcomeTitle.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #DC2626;");
            lblOutcomeDetail.setText("Phương thức: " + methodCall + " · Vai trò: " + role.name());
            lblOutcomeDetail.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-font-family: 'JetBrains Mono', monospace;");
            lblOutcomeDesc.setText(reason);
            lblOutcomeDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-line-spacing: 2px;");
            lblOutcomeBalance.setText(balanceState);
            lblOutcomeBalance.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-family: 'JetBrains Mono', monospace;");
        }
    }

    private VBox buildRightColumn() {
        VBox rightCol = new VBox(16);

        // 1. Audit Log Card
        VBox auditCard = new VBox(12);
        auditCard.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; -fx-padding: 20px; " +
                "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.03), 8, 0, 0, 2);");

        HBox auditHeader = new HBox(10);
        auditHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblAuditTitle = new Label("Nhật ký truy cập");
        lblAuditTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnClear = new Button("Xóa log");
        btnClear.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; -fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 4 12; -fx-cursor: hand;");
        btnClear.setOnMouseEntered(e -> btnClear.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 4 12; -fx-cursor: hand;"));
        btnClear.setOnMouseExited(e -> btnClear.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 4 12; -fx-cursor: hand;"));
        btnClear.setOnAction(e -> {
            logContainer.getChildren().clear();
            showEmptyLogPrompt();
        });

        auditHeader.getChildren().addAll(lblAuditTitle, sp, btnClear);

        Label lblAuditBadge = new Label("Audit Console");
        lblAuditBadge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 6px;");

        // Log container
        logScroll.setFitToWidth(true);
        logScroll.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF; -fx-border-color: transparent;");
        logScroll.setPrefHeight(150);
        VBox.setVgrow(logScroll, Priority.ALWAYS);

        logContainer.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 6 0;");

        auditCard.getChildren().addAll(auditHeader, lblAuditBadge, logScroll);

        // 2. Cách Proxy kiểm tra quyền Card
        VBox howItWorksCard = new VBox(12);
        howItWorksCard.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; -fx-padding: 20px; " +
                "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.03), 8, 0, 0, 2);");

        Label lblHowTitle = new Label("Cách Proxy kiểm tra quyền");
        lblHowTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        // Flow diagram
        HBox flowBar = new HBox(8);
        flowBar.setAlignment(Pos.CENTER_LEFT);
        flowBar.getChildren().addAll(
                createFlowStep("Người dùng"),
                createFlowArrow(),
                createFlowStep("AccountProxy"),
                createFlowArrow(),
                createFlowStep("kiểm tra quyền"),
                createFlowArrow(),
                createFlowStep("RealAccount")
        );

        // 3 Points
        VBox pointsBox = new VBox(8);
        pointsBox.getChildren().addAll(
                createPointLabel("1. AccountProxy nhận lời gọi và vai trò mô phỏng; READONLY chỉ được xem số dư."),
                createPointLabel("2. Khi được phép, lời gọi được chuyển tới RealAccount; nạp/rút cập nhật số dư tài khoản đã chọn."),
                createPointLabel("3. Lỗi nghiệp vụ như số dư không đủ được hiển thị riêng, không nhầm với từ chối quyền.")
        );

        howItWorksCard.getChildren().addAll(lblHowTitle, flowBar, pointsBox);

        rightCol.getChildren().addAll(auditCard, howItWorksCard);
        return rightCol;
    }

    private Label createFlowStep(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; -fx-padding: 6 12; -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1E293B;");
        return lbl;
    }

    private Label createFlowArrow() {
        Label arrow = new Label("➔");
        arrow.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");
        return arrow;
    }

    private Label createPointLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #475569; -fx-line-spacing: 2px;");
        lbl.setWrapText(true);
        return lbl;
    }

    private void showEmptyLogPrompt() {
        lblEmptyLog.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-padding: 8 4;");
        if (!logContainer.getChildren().contains(lblEmptyLog)) {
            logContainer.getChildren().add(lblEmptyLog);
        }
    }

    private void appendLogItem(boolean allowed, String timeStr, String message) {
        logContainer.getChildren().remove(lblEmptyLog);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 4 0;");

        Label icon = new Label(allowed ? "✓" : "✕");
        icon.setStyle(allowed
                ? "-fx-text-fill: #16A34A; -fx-font-weight: 800; -fx-font-size: 13px; -fx-min-width: 16px;"
                : "-fx-text-fill: #DC2626; -fx-font-weight: 800; -fx-font-size: 13px; -fx-min-width: 16px;");

        Label lblTime = new Label(timeStr);
        lblTime.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11.5px; -fx-min-width: 60px;");

        Label lblMsg = new Label(message);
        lblMsg.setWrapText(true);
        lblMsg.setStyle(allowed
                ? "-fx-text-fill: #1E293B; -fx-font-size: 12px;"
                : "-fx-text-fill: #DC2626; -fx-font-size: 12px;");
        HBox.setHgrow(lblMsg, Priority.ALWAYS);

        row.getChildren().addAll(icon, lblTime, lblMsg);
        logContainer.getChildren().add(row);
        logScroll.setVvalue(1.0);
    }

    private AccountProxy.Role getSelectedRole() {
        if (rbReadOnly.isSelected()) return AccountProxy.Role.READONLY;
        if (rbUser.isSelected()) return AccountProxy.Role.USER;
        return AccountProxy.Role.ADMIN;
    }

    private AccountProxy createProxyForSelected() {
        Account acc = cbAccount.getValue();
        if (acc == null) {
            ToastNotification.showWarning("Vui lòng chọn tài khoản để thử nghiệm.");
            return null;
        }
        RealAccount real = new RealAccount(acc, ctx.getFacade());
        return new AccountProxy(real, getSelectedRole());
    }

    private void testGetBalance() {
        AccountProxy proxy = createProxyForSelected();
        if (proxy == null) return;

        Account acc = cbAccount.getValue();
        String timeStr = LocalTime.now().format(TIME_FMT);
        double balance = proxy.getBalance();

        appendLogItem(true, timeStr, String.format("%s · getBalance() · Được phép. %s: %s.",
                proxy.getRole(), proxy.getAccountNumber(), UiUtils.formatVnd(balance)));

        showOutcome(true, "getBalance()", proxy.getRole(),
                "Vai trò " + proxy.getRole() + " có quyền xem số dư tài khoản qua Protection Proxy.",
                "Số dư hiện tại: " + UiUtils.formatVnd(balance));
    }

    private void testDeposit() {
        AccountProxy proxy = createProxyForSelected();
        if (proxy == null) return;

        Account acc = cbAccount.getValue();
        double oldBalance = acc.getBalance();
        String timeStr = LocalTime.now().format(TIME_FMT);

        double amount;
        try {
            String raw = txtAmount.getText().replace(",", "").replace(".", "").trim();
            amount = Double.parseDouble(raw);
            if (amount <= 0) throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
        } catch (NumberFormatException e) {
            showOutcome(false, "deposit()", proxy.getRole(),
                    "Số tiền không hợp lệ. Vui lòng nhập số hợp lệ.",
                    "Trước: " + UiUtils.formatVnd(oldBalance) + " → Sau: " + UiUtils.formatVnd(oldBalance));
            return;
        }

        try {
            proxy.deposit(amount);
            double newBalance = proxy.getBalance();

            appendLogItem(true, timeStr, String.format("%s · deposit(%s) · Được phép. %s: %s.",
                    proxy.getRole(), (long)amount, proxy.getAccountNumber(), UiUtils.formatVnd(newBalance)));

            ctx.notifyDataChanged();
            ToastNotification.showSuccess("Nạp thành công " + UiUtils.formatVnd(amount) + " qua Proxy!");

            showOutcome(true, "deposit(" + (long)amount + ")", proxy.getRole(),
                    "Ủy quyền thành công qua Protection Proxy, chuyển tiếp đến RealAccount.",
                    "Trước: " + UiUtils.formatVnd(oldBalance) + " → Sau: " + UiUtils.formatVnd(newBalance));
        } catch (SecurityException ex) {
            appendLogItem(false, timeStr, String.format("%s · deposit(%s) · Bị từ chối. Vai trò chỉ được xem; số dư không đổi.",
                    proxy.getRole(), (long)amount));
            ToastNotification.showError("Thao tác bị từ chối bởi Protection Proxy");

            showOutcome(false, "deposit(" + (long)amount + ")", proxy.getRole(),
                    "READONLY chỉ được xem số dư; thao tác nạp tiền không được chuyển tới RealAccount.",
                    "Trước: " + UiUtils.formatVnd(oldBalance) + " → Sau: " + UiUtils.formatVnd(oldBalance));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            String friendly = UiUtils.humanizeError(ex);
            ToastNotification.showWarning(friendly);
            showOutcome(false, "deposit(" + (long)amount + ")", proxy.getRole(),
                    friendly,
                    "Trước: " + UiUtils.formatVnd(oldBalance) + " → Sau: " + UiUtils.formatVnd(oldBalance));
        }
    }

    private void testWithdraw() {
        AccountProxy proxy = createProxyForSelected();
        if (proxy == null) return;

        Account acc = cbAccount.getValue();
        double oldBalance = acc.getBalance();
        String timeStr = LocalTime.now().format(TIME_FMT);

        double amount;
        try {
            String raw = txtAmount.getText().replace(",", "").replace(".", "").trim();
            amount = Double.parseDouble(raw);
            if (amount <= 0) throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
        } catch (NumberFormatException e) {
            showOutcome(false, "withdraw()", proxy.getRole(),
                    "Số tiền không hợp lệ. Vui lòng nhập số hợp lệ.",
                    "Trước: " + UiUtils.formatVnd(oldBalance) + " → Sau: " + UiUtils.formatVnd(oldBalance));
            return;
        }

        try {
            proxy.withdraw(amount);
            double newBalance = proxy.getBalance();

            appendLogItem(true, timeStr, String.format("%s · withdraw(%s) · Được phép. %s: %s.",
                    proxy.getRole(), (long)amount, proxy.getAccountNumber(), UiUtils.formatVnd(newBalance)));

            ctx.notifyDataChanged();
            ToastNotification.showSuccess("Rút thành công " + UiUtils.formatVnd(amount) + " qua Proxy!");

            showOutcome(true, "withdraw(" + (long)amount + ")", proxy.getRole(),
                    "Ủy quyền thành công qua Protection Proxy, chuyển tiếp đến RealAccount.",
                    "Trước: " + UiUtils.formatVnd(oldBalance) + " → Sau: " + UiUtils.formatVnd(newBalance));
        } catch (SecurityException ex) {
            appendLogItem(false, timeStr, String.format("%s · withdraw(%s) · Bị từ chối. Vai trò chỉ được xem; số dư không đổi.",
                    proxy.getRole(), (long)amount));
            ToastNotification.showError("Thao tác bị từ chối bởi Protection Proxy");

            showOutcome(false, "withdraw(" + (long)amount + ")", proxy.getRole(),
                    "READONLY chỉ được xem số dư; thao tác rút tiền không được chuyển tới RealAccount.",
                    "Trước: " + UiUtils.formatVnd(oldBalance) + " → Sau: " + UiUtils.formatVnd(oldBalance));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            String friendly = UiUtils.humanizeError(ex);
            ToastNotification.showWarning(friendly);
            showOutcome(false, "withdraw(" + (long)amount + ")", proxy.getRole(),
                    friendly,
                    "Trước: " + UiUtils.formatVnd(oldBalance) + " → Sau: " + UiUtils.formatVnd(oldBalance));
        }
    }

    public void refresh() {
        Account selected = cbAccount.getValue();
        cbAccount.setItems(ctx.getAccounts());
        if (selected != null) {
            for (Account acc : ctx.getAccounts()) {
                if (acc.getAccountNumber().equals(selected.getAccountNumber())) {
                    cbAccount.setValue(acc);
                    return;
                }
            }
        }
        if (!ctx.getAccounts().isEmpty()) {
            cbAccount.setValue(ctx.getAccounts().get(0));
        }
    }
}

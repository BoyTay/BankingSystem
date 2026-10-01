package com.banking.ui;

import com.banking.model.Account;
import com.banking.pattern.structural.AccountProxy;
import com.banking.pattern.structural.RealAccount;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Màn hình Mô phỏng phân quyền truy cập (Protection Proxy Pattern - RBAC).
 * Thiết kế chuẩn theo Stitch Design System & Figma Brief:
 * - Cột trái: Cấu hình phiên truy cập Proxy, các phương thức AccountProxy & Outcome Panel trực quan hóa kết quả (Được phép / Bị từ chối)
 * - Cột phải: Nhật ký can thiệp của Proxy (Audit Console phong cách Dark Terminal)
 * - Khối dưới: Sơ đồ kiến trúc luồng dữ liệu (Client ➔ AccountProxy ➔ RBAC ➔ RealAccount)
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
    private final TextField txtAmount = new TextField("100000");

    // Immediate Outcome Panel
    private final VBox outcomePanel = new VBox(8);
    private final Label lblOutcomeTitle = new Label();
    private final Label lblOutcomeCode = new Label();
    private final Label lblOutcomeMethod = new Label();
    private final Label lblOutcomeRole = new Label();
    private final Label lblOutcomeReason = new Label();
    private final Label lblOutcomeBalanceState = new Label();

    // Audit Log Console
    private final TextFlow logFlow = new TextFlow();
    private final ScrollPane logScroll = new ScrollPane(logFlow);

    public ProxyDemoView() {
        setSpacing(18);
        getStyleClass().add("content-pane");

        buildHeader();

        // TOP SECTION: 2 Cột (Trái: Scenario + Outcome / Phải: Audit Console)
        HBox topSection = new HBox(20);
        topSection.setAlignment(Pos.TOP_LEFT);

        VBox controlCol = buildControlSection();
        VBox consoleCol = buildAuditConsole();

        controlCol.setPrefWidth(600);
        controlCol.setMinWidth(480);
        HBox.setHgrow(controlCol, Priority.ALWAYS);

        consoleCol.setPrefWidth(460);
        consoleCol.setMinWidth(380);

        topSection.getChildren().addAll(controlCol, consoleCol);

        // BOTTOM SECTION: Sơ đồ luồng kiến trúc (How It Works)
        VBox architectureCard = buildArchitectureFlowCard();

        getChildren().addAll(topSection, architectureCard);

        ctx.addDataChangeListener(this::refresh);
        refresh();
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
        Label tagText = new Label("KIẾN TRÚC MẪU CẤU TRÚC · PROTECTION PROXY");
        tagText.getStyleClass().add("header-badge-tag-text");
        tagBox.getChildren().addAll(tagDot, tagText);

        Label title = new Label("Mô phỏng phân quyền Proxy");
        title.getStyleClass().add("header-greeting");

        Label subtitle = new Label("Mô phỏng cơ chế kiểm soát truy cập bảo mật (Protection Proxy & RBAC) can thiệp trước khi gọi đối tượng thực");
        subtitle.getStyleClass().add("header-subtitle");

        titleBox.getChildren().addAll(tagBox, title, subtitle);

        // Right Pattern Badges
        HBox patternBadges = new HBox(8);
        patternBadges.setAlignment(Pos.CENTER_RIGHT);
        patternBadges.getChildren().addAll(
                UiUtils.createPatternBadge("Proxy (AccountProxy)", "structural"),
                UiUtils.createPatternBadge("Protection Proxy (RBAC)", "structural")
        );

        header.getChildren().addAll(titleBox, patternBadges);
        getChildren().add(header);
    }

    private VBox buildControlSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20;");

        // Section Title
        HBox cardTitleBox = new HBox(10);
        cardTitleBox.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color: rgba(37, 99, 235, 0.12); -fx-background-radius: 8px; -fx-min-width: 36px; -fx-min-height: 36px; -fx-max-width: 36px; -fx-max-height: 36px;");
        SVGPath shieldSvg = new SVGPath();
        shieldSvg.setContent("M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z");
        shieldSvg.setStyle("-fx-fill: #2563EB; -fx-scale-x: 0.8; -fx-scale-y: 0.8;");
        iconBox.getChildren().add(shieldSvg);

        VBox titleArea = new VBox(2);
        Label title = new Label("Cấu hình phiên truy cập Proxy");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitle = new Label("Chọn tài khoản, gán vai trò mô phỏng và kích hoạt phương thức bảo vệ");
        subtitle.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
        titleArea.getChildren().addAll(title, subtitle);

        cardTitleBox.getChildren().addAll(iconBox, titleArea);

        // Inputs Form
        VBox form = new VBox(12);

        // 1. Account Selector
        VBox grpAcc = new VBox(4);
        Label lblAcc = new Label("Tài khoản mục tiêu (Target RealAccount) *");
        lblAcc.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");

        cbAccount.setMaxWidth(Double.MAX_VALUE);
        cbAccount.getStyleClass().add("account-selector-combo");
        cbAccount.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getAccountNumber() + " - " + item.getOwnerName() + " (" + UiUtils.formatVnd(item.getBalance()) + ")");
            }
        });
        cbAccount.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getAccountNumber() + " - " + item.getOwnerName() + " (" + UiUtils.formatVnd(item.getBalance()) + ")");
            }
        });
        grpAcc.getChildren().addAll(lblAcc, cbAccount);

        // 2. Role Selector with cards
        VBox grpRole = new VBox(6);
        Label lblRole = new Label("Vai trò mô phỏng (Simulated Role) *");
        lblRole.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");

        rbAdmin.setToggleGroup(roleGroup);
        rbUser.setToggleGroup(roleGroup);
        rbReadOnly.setToggleGroup(roleGroup);
        rbAdmin.setSelected(true);

        VBox roleChoices = new VBox(6);
        roleChoices.getChildren().addAll(
                buildRoleChoice(rbAdmin, "ADMIN", "Quản trị viên — Toàn quyền truy cập và chỉnh sửa số dư", "#065F46", "#D1FAE5"),
                buildRoleChoice(rbUser, "USER", "Khách hàng — Có quyền đọc số dư và nạp / rút tiền mặt", "#1D4ED8", "#DBEAFE"),
                buildRoleChoice(rbReadOnly, "READONLY", "Kiểm toán viên — CHỈ ĐỌC số dư (Bị chặn thao tác nạp/rút)", "#991B1B", "#FEE2E2")
        );

        Label lblRoleNote = new Label("💡 Lưu ý: Vai trò ở đây dùng để trình diễn AccountProxy.checkPermission(); không thay đổi phiên đăng nhập thực tế.");
        lblRoleNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-line-spacing: 2px;");

        grpRole.getChildren().addAll(lblRole, roleChoices, lblRoleNote);

        // 3. Amount Input
        VBox grpAmount = new VBox(4);
        Label lblAmount = new Label("Số tiền thử nghiệm giao dịch (VND) *");
        lblAmount.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        txtAmount.getStyleClass().add("form-input");
        grpAmount.getChildren().addAll(lblAmount, txtAmount);

        // 4. Method Trigger Buttons
        VBox grpActions = new VBox(8);
        Label lblActionTitle = new Label("Gọi phương thức qua AccountProxy:");
        lblActionTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");

        HBox btnRow = new HBox(10);
        Button btnGetBalance = new Button("👁 getBalance()");
        btnGetBalance.getStyleClass().add("btn-secondary");
        btnGetBalance.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 8 16;");
        HBox.setHgrow(btnGetBalance, Priority.ALWAYS);
        btnGetBalance.setOnAction(e -> testGetBalance());

        Button btnDeposit = new Button("➕ deposit()");
        btnDeposit.getStyleClass().add("btn-primary");
        btnDeposit.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 8 16;");
        HBox.setHgrow(btnDeposit, Priority.ALWAYS);
        btnDeposit.setOnAction(e -> testDeposit());

        Button btnWithdraw = new Button("➖ withdraw()");
        btnWithdraw.getStyleClass().add("btn-secondary");
        btnWithdraw.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #DC2626; -fx-border-color: #FECACA; -fx-padding: 8 16;");
        HBox.setHgrow(btnWithdraw, Priority.ALWAYS);
        btnWithdraw.setOnAction(e -> testWithdraw());

        btnRow.getChildren().addAll(btnGetBalance, btnDeposit, btnWithdraw);

        Label lblDepositNotice = new Label("● Nạp / rút qua Proxy sẽ thay đổi số dư thực tế của tài khoản trong phiên mô phỏng.");
        lblDepositNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        grpActions.getChildren().addAll(lblActionTitle, btnRow, lblDepositNotice);

        // 5. Outcome Panel (Feedback card)
        buildOutcomePanel();

        form.getChildren().addAll(grpAcc, grpRole, grpAmount, grpActions, outcomePanel);
        card.getChildren().addAll(cardTitleBox, form);
        return card;
    }

    private HBox buildRoleChoice(RadioButton rb, String name, String desc, String color, String bg) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 10; -fx-cursor: hand;");
        card.setOnMouseClicked(e -> rb.setSelected(true));

        Label badge = new Label(name);
        badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 6px;");

        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");

        card.getChildren().addAll(rb, badge, lblDesc);
        return card;
    }

    private void buildOutcomePanel() {
        outcomePanel.setVisible(false);
        outcomePanel.setManaged(false);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        lblOutcomeTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        lblOutcomeCode.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 2 6; -fx-background-radius: 6px;");
        topRow.getChildren().addAll(lblOutcomeTitle, sp, lblOutcomeCode);

        HBox metaRow = new HBox(16);
        metaRow.setStyle("-fx-background-color: rgba(255, 255, 255, 0.7); -fx-padding: 8; -fx-background-radius: 6px;");
        VBox col1 = new VBox(2, new Label("Phương thức:"), lblOutcomeMethod);
        col1.getChildren().get(0).setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
        lblOutcomeMethod.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-font-family: 'JetBrains Mono', monospace;");

        VBox col2 = new VBox(2, new Label("Vai trò thực thi:"), lblOutcomeRole);
        col2.getChildren().get(0).setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
        lblOutcomeRole.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700;");

        HBox.setHgrow(col1, Priority.ALWAYS);
        HBox.setHgrow(col2, Priority.ALWAYS);
        metaRow.getChildren().addAll(col1, col2);

        lblOutcomeReason.setStyle("-fx-font-size: 11.5px; -fx-line-spacing: 2px;");
        lblOutcomeReason.setWrapText(true);

        lblOutcomeBalanceState.setStyle("-fx-font-size: 11px; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 700;");

        outcomePanel.getChildren().addAll(topRow, metaRow, lblOutcomeReason, lblOutcomeBalanceState);
    }

    private void showOutcome(boolean allowed, String method, AccountProxy.Role role, String reason, String balanceState) {
        outcomePanel.setVisible(true);
        outcomePanel.setManaged(true);
        outcomePanel.getStyleClass().clear();

        if (allowed) {
            outcomePanel.getStyleClass().add("outcome-card-allowed");
            lblOutcomeTitle.setText("✓ ĐƯỢC PHÉP (ALLOWED) — Proxy Ủy Quyền Thành Công");
            lblOutcomeTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #065F46;");
            lblOutcomeCode.setText("200 OK");
            lblOutcomeCode.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 2 6; -fx-background-radius: 6px;");
            lblOutcomeMethod.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #065F46; -fx-font-family: 'JetBrains Mono', monospace;");
            lblOutcomeRole.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #059669;");
            lblOutcomeReason.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #047857; -fx-line-spacing: 2px;");
            lblOutcomeBalanceState.setStyle("-fx-font-size: 11px; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 700; -fx-text-fill: #065F46;");
        } else {
            outcomePanel.getStyleClass().add("outcome-card-denied");
            lblOutcomeTitle.setText("⛔ BỊ TỪ CHỐI (DENIED) — SecurityException");
            lblOutcomeTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #991B1B;");
            lblOutcomeCode.setText("403 Forbidden");
            lblOutcomeCode.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 2 6; -fx-background-radius: 6px;");
            lblOutcomeMethod.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #DC2626; -fx-font-family: 'JetBrains Mono', monospace;");
            lblOutcomeRole.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");
            lblOutcomeReason.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #991B1B; -fx-line-spacing: 2px;");
            lblOutcomeBalanceState.setStyle("-fx-font-size: 11px; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 700; -fx-text-fill: #7F1D1D;");
        }

        lblOutcomeMethod.setText(method);
        lblOutcomeRole.setText(role.name());
        lblOutcomeReason.setText(reason);
        lblOutcomeBalanceState.setText("Trạng thái số dư: " + balanceState);
    }

    private VBox buildAuditConsole() {
        VBox card = new VBox(10);
        card.getStyleClass().add("terminal-surface");

        HBox titleBar = new HBox(8);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getStyleClass().add("terminal-title-bar");

        HBox dots = new HBox(5);
        Circle dotRed = new Circle(4.5);
        dotRed.getStyleClass().add("terminal-dot-red");
        Circle dotYellow = new Circle(4.5);
        dotYellow.getStyleClass().add("terminal-dot-yellow");
        Circle dotGreen = new Circle(4.5);
        dotGreen.getStyleClass().add("terminal-dot-green");
        dots.getChildren().addAll(dotRed, dotYellow, dotGreen);

        Label lblFile = new Label("PROXY_AUDIT_STREAM");
        lblFile.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnClear = new Button("Xóa log");
        btnClear.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #CBD5E1; -fx-font-size: 10.5px; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-cursor: hand;");
        btnClear.setOnAction(e -> logFlow.getChildren().clear());

        titleBar.getChildren().addAll(dots, lblFile, sp, btnClear);

        Label lblSub = new Label("Nhật ký can thiệp thời gian thực của AccountProxy:");
        lblSub.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        // Log Stream Container
        logScroll.setFitToWidth(true);
        logScroll.setStyle("-fx-background: #0F172A; -fx-background-color: #0F172A; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        logScroll.setPrefHeight(320);
        VBox.setVgrow(logScroll, Priority.ALWAYS);

        logFlow.setStyle("-fx-background-color: #0F172A; -fx-padding: 10;");

        appendLog("Hệ thống kiểm soát truy cập Proxy đã kích hoạt. Sẵn sàng điều phối...", "#64748B");

        card.getChildren().addAll(titleBar, lblSub, logScroll);
        return card;
    }

    private void appendLog(String message, String colorHex) {
        String time = LocalTime.now().format(TIME_FMT);
        Text tTime = new Text("[" + time + "] ");
        tTime.setStyle("-fx-fill: #64748B; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px;");

        Text tMsg = new Text(message + "\n");
        tMsg.setStyle("-fx-fill: " + colorHex + "; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px;");

        logFlow.getChildren().addAll(tTime, tMsg);
        logScroll.setVvalue(1.0);
    }

    private VBox buildArchitectureFlowCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20;");

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label iconArch = new Label("⚙️");
        iconArch.setStyle("-fx-font-size: 16px;");
        Label lblTitle = new Label("Cơ chế hoạt động của Protection Proxy Pattern");
        lblTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        titleRow.getChildren().addAll(iconArch, lblTitle);

        // Architecture Flow Diagram
        HBox flowBar = new HBox(12);
        flowBar.setAlignment(Pos.CENTER);
        flowBar.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 14 20;");

        flowBar.getChildren().addAll(
                buildFlowStep("1. Client / Actor", "Người dùng / Controller", "#1E293B"),
                buildFlowArrow(),
                buildFlowStep("2. AccountProxy", "Cổng kiểm soát an ninh", "#2563EB"),
                buildFlowArrow(),
                buildFlowStep("3. RBAC Guard", "Kiểm tra quyền READ/WRITE", "#D97706"),
                buildFlowArrow(),
                buildFlowStep("4. RealAccount", "Thực thể tài khoản thật", "#059669")
        );

        // 3 Key Takeaways
        HBox notesRow = new HBox(16);
        notesRow.getChildren().addAll(
                buildNoteBox("Ủy quyền & Đóng gói", "Mọi cuộc gọi nạp/rút đều phải ủy quyền qua Proxy trước khi chạm tới RealAccount."),
                buildNoteBox("Chặn truy cập tức thì", "Vai trò READONLY bị ngắt ngay lập tức khi gọi hàm ghi, ném SecurityException và bảo toàn số dư."),
                buildNoteBox("Minh bạch giao diện", "AccountProxy triển khai cùng interface với RealAccount, tầng gọi không cần thay đổi logic nghiệp vụ.")
        );

        card.getChildren().addAll(titleRow, flowBar, notesRow);
        return card;
    }

    private VBox buildFlowStep(String title, String sub, String colorHex) {
        VBox step = new VBox(2);
        step.getStyleClass().add("flow-step-box");
        step.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 12;");
        HBox.setHgrow(step, Priority.ALWAYS);

        Label lbl1 = new Label(title);
        lbl1.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: " + colorHex + ";");

        Label lbl2 = new Label(sub);
        lbl2.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #64748B;");

        step.getChildren().addAll(lbl1, lbl2);
        return step;
    }

    private Label buildFlowArrow() {
        Label arrow = new Label("➔");
        arrow.setStyle("-fx-font-size: 16px; -fx-text-fill: #94A3B8;");
        return arrow;
    }

    private VBox buildNoteBox(String title, String desc) {
        VBox box = new VBox(3);
        box.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 12;");
        HBox.setHgrow(box, Priority.ALWAYS);

        Label lblTitle = new Label("• " + title);
        lblTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-line-spacing: 2px;");
        lblDesc.setWrapText(true);

        box.getChildren().addAll(lblTitle, lblDesc);
        return box;
    }

    private AccountProxy.Role getSelectedRole() {
        if (rbReadOnly.isSelected()) return AccountProxy.Role.READONLY;
        if (rbUser.isSelected()) return AccountProxy.Role.USER;
        return AccountProxy.Role.ADMIN;
    }

    private AccountProxy createProxyForSelected() {
        Account acc = cbAccount.getValue();
        if (acc == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Chưa chọn tài khoản", "Vui lòng chọn tài khoản để thử nghiệm.");
            return null;
        }
        RealAccount real = new RealAccount(acc, ctx.getFacade());
        return new AccountProxy(real, getSelectedRole());
    }

    private void testGetBalance() {
        AccountProxy proxy = createProxyForSelected();
        if (proxy == null) return;

        double balance = proxy.getBalance();
        appendLog(String.format("✔ [Proxy.getBalance] Cho phép vai trò %s xem số dư tài khoản %s: %s",
                proxy.getRole(), proxy.getAccountNumber(), UiUtils.formatVnd(balance)), "#40E18F");

        showOutcome(true, "getBalance()", proxy.getRole(),
                "Vai trò " + proxy.getRole() + " có quyền xem số dư tài khoản qua Proxy.",
                UiUtils.formatVnd(balance));
    }

    private void testDeposit() {
        AccountProxy proxy = createProxyForSelected();
        if (proxy == null) return;

        Account acc = cbAccount.getValue();
        double oldBalance = acc.getBalance();

        try {
            double amount = Double.parseDouble(txtAmount.getText().trim());
            proxy.deposit(amount);
            double newBalance = proxy.getBalance();

            appendLog(String.format("✔ [Proxy.deposit] Cho phép vai trò %s nạp +%s vào tài khoản %s. Số dư mới: %s",
                    proxy.getRole(), UiUtils.formatVnd(amount), proxy.getAccountNumber(), UiUtils.formatVnd(newBalance)), "#40E18F");

            ctx.notifyDataChanged();
            ToastNotification.showSuccess("Nạp thành công " + UiUtils.formatVnd(amount) + " qua Proxy!");

            showOutcome(true, "deposit(" + UiUtils.formatVnd(amount) + ")", proxy.getRole(),
                    "Ủy quyền thành công qua Protection Proxy, chuyển tiếp đến RealAccount.",
                    UiUtils.formatVnd(newBalance));
        } catch (SecurityException ex) {
            String friendly = UiUtils.humanizeError(ex);
            appendLog(String.format("⛔ [Proxy INTERCEPTED] TỪ CHỐI TRUY CẬP: %s", ex.getMessage()), "#EF4444");
            ToastNotification.showError(friendly);

            showOutcome(false, "deposit(" + txtAmount.getText() + " VND)", proxy.getRole(),
                    "Vai trò READONLY chỉ được phép gọi getBalance(). Không có quyền ghi (WRITE_PERMISSION) hoặc thay đổi số dư thực tế.",
                    "Không đổi (Giữ nguyên: " + UiUtils.formatVnd(oldBalance) + ")");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            String friendly = UiUtils.humanizeError(ex);
            ToastNotification.showWarning(friendly);
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", null, friendly);
        }
    }

    private void testWithdraw() {
        AccountProxy proxy = createProxyForSelected();
        if (proxy == null) return;

        Account acc = cbAccount.getValue();
        double oldBalance = acc.getBalance();

        try {
            double amount = Double.parseDouble(txtAmount.getText().trim());
            proxy.withdraw(amount);
            double newBalance = proxy.getBalance();

            appendLog(String.format("✔ [Proxy.withdraw] Cho phép vai trò %s rút -%s từ tài khoản %s. Số dư còn: %s",
                    proxy.getRole(), UiUtils.formatVnd(amount), proxy.getAccountNumber(), UiUtils.formatVnd(newBalance)), "#40E18F");

            ctx.notifyDataChanged();
            ToastNotification.showSuccess("Rút thành công " + UiUtils.formatVnd(amount) + " qua Proxy!");

            showOutcome(true, "withdraw(" + UiUtils.formatVnd(amount) + ")", proxy.getRole(),
                    "Ủy quyền thành công qua Protection Proxy, chuyển tiếp đến RealAccount.",
                    UiUtils.formatVnd(newBalance));
        } catch (SecurityException ex) {
            String friendly = UiUtils.humanizeError(ex);
            appendLog(String.format("⛔ [Proxy INTERCEPTED] TỪ CHỐI TRUY CẬP: %s", ex.getMessage()), "#EF4444");
            ToastNotification.showError(friendly);

            showOutcome(false, "withdraw(" + txtAmount.getText() + " VND)", proxy.getRole(),
                    "Vai trò READONLY chỉ được phép gọi getBalance(). Không có quyền ghi (WRITE_PERMISSION) hoặc thay đổi số dư thực tế.",
                    "Không đổi (Giữ nguyên: " + UiUtils.formatVnd(oldBalance) + ")");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            String friendly = UiUtils.humanizeError(ex);
            ToastNotification.showWarning(friendly);
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", null, friendly);
        }
    }

    public void refresh() {
        Account selected = cbAccount.getValue();
        cbAccount.setItems(ctx.getAccounts());
        if (selected != null && ctx.getAccounts().contains(selected)) {
            cbAccount.setValue(selected);
        } else if (!ctx.getAccounts().isEmpty()) {
            cbAccount.setValue(ctx.getAccounts().get(0));
        }
    }
}

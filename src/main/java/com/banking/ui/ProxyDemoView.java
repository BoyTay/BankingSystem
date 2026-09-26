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
 * Màn hình Demo Proxy Pattern (Role-Based Access Control - RBAC).
 * Minh họa:
 * - Proxy đóng vai trò cổng an ninh kiểm soát mọi truy cập tới RealAccount.
 * - Vai trò READONLY chỉ được xem số dư, các thao tác nạp/rút sẽ bị Proxy chặn và bắn SecurityException.
 * - Vai trò USER và ADMIN được cấp quyền đầy đủ.
 */
public class ProxyDemoView extends VBox {

    private final UIContext ctx = UIContext.getInstance();

    private final ComboBox<Account> cbAccount = new ComboBox<>();
    private final ToggleGroup roleGroup = new ToggleGroup();
    private final RadioButton rbAdmin = new RadioButton("ADMIN (Quản trị viên - Toàn quyền)");
    private final RadioButton rbUser = new RadioButton("USER (Khách hàng - Đọc & Ghi)");
    private final RadioButton rbReadOnly = new RadioButton("READONLY (Kiểm toán viên - Chỉ đọc)");
    private final TextField txtAmount = new TextField("100000");
    private final TextArea txtAuditLog = new TextArea();

    public ProxyDemoView() {
        setSpacing(24);
        getStyleClass().add("content-pane");

        buildHeader();

        HBox mainSection = new HBox(20);
        VBox controlCard = buildControlCard();
        VBox logCard = buildAuditLogCard();
        HBox.setHgrow(controlCard, Priority.ALWAYS);
        HBox.setHgrow(logCard, Priority.ALWAYS);
        mainSection.getChildren().addAll(controlCard, logCard);

        VBox explainerCard = buildPatternExplainerCard();

        getChildren().addAll(mainSection, explainerCard);

        ctx.addDataChangeListener(this::refresh);
        refresh();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Phân Quyền Truy Cập (Proxy Pattern)");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Mô phỏng cơ chế kiểm soát truy cập bảo mật (Protection Proxy) theo vai trò");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        HBox patternBadges = new HBox(8);
        patternBadges.setAlignment(Pos.CENTER_RIGHT);
        patternBadges.getChildren().addAll(
                UiUtils.createPatternBadge("Proxy (AccountProxy)", "structural"),
                UiUtils.createPatternBadge("Protection Proxy (RBAC)", "structural")
        );

        HBox header = new HBox();
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, patternBadges);
        header.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(header);
    }

    private VBox buildControlCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Cấu hình phiên truy cập Proxy");
        cardTitle.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);

        cbAccount.setMaxWidth(Double.MAX_VALUE);
        cbAccount.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getAccountNumber() + " - " + item.getOwnerName());
            }
        });
        cbAccount.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getAccountNumber() + " - " + item.getOwnerName());
            }
        });

        // Setup radio buttons
        rbAdmin.setToggleGroup(roleGroup);
        rbUser.setToggleGroup(roleGroup);
        rbReadOnly.setToggleGroup(roleGroup);
        rbAdmin.setSelected(true);

        VBox roleBox = new VBox(8);
        roleBox.getChildren().addAll(rbAdmin, rbUser, rbReadOnly);

        grid.add(new Label("Tài khoản mục tiêu:"), 0, 0);
        grid.add(cbAccount, 1, 0);

        grid.add(new Label("Vai trò (Role):"), 0, 1);
        grid.add(roleBox, 1, 1);

        grid.add(new Label("Số tiền thử nghiệm:"), 0, 2);
        grid.add(txtAmount, 1, 2);

        ColumnConstraints c1 = new ColumnConstraints(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        // Action buttons
        VBox actions = new VBox(10);
        Label lblAction = new Label("Thử nghiệm các phương thức qua Proxy:");
        lblAction.getStyleClass().add("form-label");

        HBox btnRow = new HBox(10);
        Button btnGetBalance = new Button("👁 getBalance()");
        btnGetBalance.getStyleClass().add("btn-secondary");
        btnGetBalance.setOnAction(e -> testGetBalance());

        Button btnDeposit = new Button("📥 deposit()");
        btnDeposit.getStyleClass().add("btn-primary");
        btnDeposit.setOnAction(e -> testDeposit());

        Button btnWithdraw = new Button("📤 withdraw()");
        btnWithdraw.getStyleClass().add("btn-navy");
        btnWithdraw.setOnAction(e -> testWithdraw());

        btnRow.getChildren().addAll(btnGetBalance, btnDeposit, btnWithdraw);
        actions.getChildren().addAll(lblAction, btnRow);

        card.getChildren().addAll(cardTitle, grid, new Separator(), actions);
        return card;
    }

    private VBox buildAuditLogCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        HBox header = new HBox(8);
        Label title = new Label("Nhật ký can thiệp của Proxy (Audit Console)");
        title.getStyleClass().add("card-title");
        Button btnClear = new Button("Xóa log");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setOnAction(e -> txtAuditLog.clear());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, btnClear);
        header.setAlignment(Pos.CENTER_LEFT);

        txtAuditLog.setEditable(false);
        txtAuditLog.setPrefRowCount(14);
        txtAuditLog.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px; -fx-control-inner-background: #0F172A; -fx-text-fill: #A7F3D0;");
        appendLog("Hệ thống kiểm soát truy cập Proxy đã kích hoạt. Sẵn sàng nhận yêu cầu...");

        card.getChildren().addAll(header, txtAuditLog);
        return card;
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
        RealAccount real = new RealAccount(acc);
        return new AccountProxy(real, getSelectedRole());
    }

    private void testGetBalance() {
        AccountProxy proxy = createProxyForSelected();
        if (proxy == null) return;

        double balance = proxy.getBalance();
        appendLog(String.format("✔ [Proxy.getBalance] Cho phép vai trò %s truy cập số dư: %s",
                proxy.getRole(), UiUtils.formatVnd(balance)));
    }

    private void testDeposit() {
        AccountProxy proxy = createProxyForSelected();
        if (proxy == null) return;

        try {
            double amount = Double.parseDouble(txtAmount.getText().trim());
            proxy.deposit(amount);
            appendLog(String.format("✔ [Proxy.deposit] Cho phép vai trò %s nạp +%s vào tài khoản %s. Số dư mới: %s",
                    proxy.getRole(), UiUtils.formatVnd(amount), proxy.getAccountNumber(), UiUtils.formatVnd(proxy.getBalance())));
            ctx.notifyDataChanged();
        } catch (SecurityException ex) {
            appendLog(String.format("⛔ [Proxy INTERCEPTED] TỪ CHỐI TRUY CẬP: %s", ex.getMessage()));
            UiUtils.showAlert(Alert.AlertType.ERROR, "Proxy Security Block",
                    "⛔ Bị chặn bởi AccountProxy (SecurityException)",
                    "Vai trò READONLY chỉ có quyền xem số dư, không được phép nạp tiền vào hệ thống!");
        } catch (NumberFormatException ex) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Số tiền không hợp lệ", "Vui lòng nhập định dạng số hợp lệ.");
        }
    }

    private void testWithdraw() {
        AccountProxy proxy = createProxyForSelected();
        if (proxy == null) return;

        try {
            double amount = Double.parseDouble(txtAmount.getText().trim());
            proxy.withdraw(amount);
            appendLog(String.format("✔ [Proxy.withdraw] Cho phép vai trò %s rút -%s từ tài khoản %s. Số dư còn: %s",
                    proxy.getRole(), UiUtils.formatVnd(amount), proxy.getAccountNumber(), UiUtils.formatVnd(proxy.getBalance())));
            ctx.notifyDataChanged();
        } catch (SecurityException ex) {
            appendLog(String.format("⛔ [Proxy INTERCEPTED] TỪ CHỐI TRUY CẬP: %s", ex.getMessage()));
            UiUtils.showAlert(Alert.AlertType.ERROR, "Proxy Security Block",
                    "⛔ Bị chặn bởi AccountProxy (SecurityException)",
                    "Vai trò READONLY không có quyền rút tiền!\nAccountProxy đã chủ động chặn cuộc gọi trước khi tới RealAccount.");
        } catch (NumberFormatException ex) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Số tiền không hợp lệ", "Vui lòng nhập định dạng số hợp lệ.");
        }
    }

    private void appendLog(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        txtAuditLog.appendText("[" + time + "] " + message + "\n");
    }

    private VBox buildPatternExplainerCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Nguyên lý hoạt động của Protection Proxy Pattern");
        cardTitle.getStyleClass().add("card-title");

        Label desc = new Label(
                "• AccountProxy và RealAccount đều cùng hiện thực interface chung BankAccount.\n"
                        + "• Khách hàng giao tiếp trực tiếp với AccountProxy thay vì đối tượng thật RealAccount.\n"
                        + "• AccountProxy đóng vai trò như một bức tường lửa kiểm soát quyền hạn (RBAC). Khi vai trò là READONLY, Proxy lập tức ném ra SecurityException và ngăn không cho phương thức deposit() hoặc withdraw() của RealAccount được thực thi."
        );
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-line-spacing: 4px;");

        card.getChildren().addAll(cardTitle, desc);
        return card;
    }

    public void refresh() {
        Account current = cbAccount.getValue();
        cbAccount.setItems(ctx.getAccounts());
        if (current != null) {
            ctx.getAccounts().stream().filter(a -> a.getAccountNumber().equals(current.getAccountNumber())).findFirst().ifPresent(cbAccount::setValue);
        } else if (!ctx.getAccounts().isEmpty()) {
            cbAccount.setValue(ctx.getAccounts().get(0));
        }
    }
}

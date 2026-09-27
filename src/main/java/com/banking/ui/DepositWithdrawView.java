package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.model.enums.AccountStatus;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Màn hình Nạp & Rút tiền.
 * Minh họa Patterns:
 * - State Pattern: Khi tài khoản ở LockedState, lệnh rút tiền sẽ bị từ chối.
 * - Strategy Pattern: Tính phí rút tiền theo chiến lược riêng của từng tài khoản.
 * - Observer Pattern: Kích hoạt thông báo đa kênh (SMS, Email, UI) sau giao dịch.
 * - Facade Pattern: Gọi qua BankingFacade để xử lý trọn gói.
 */
public class DepositWithdrawView extends VBox {

    private final UIContext ctx = UIContext.getInstance();

    // Controls Nạp tiền
    private final ComboBox<Account> cbDepositAccount = new ComboBox<>();
    private final TextField txtDepositAmount = new TextField();
    private final Label lblDepositBalance = new Label();

    // Controls Rút tiền
    private final ComboBox<Account> cbWithdrawAccount = new ComboBox<>();
    private final TextField txtWithdrawAmount = new TextField();
    private final Label lblWithdrawBalance = new Label();
    private final Label lblWithdrawFeePreview = new Label();

    public DepositWithdrawView() {
        setSpacing(24);
        getStyleClass().add("content-pane");

        buildHeader();

        HBox mainCards = new HBox(20);
        VBox depositCard = buildDepositCard();
        VBox withdrawCard = buildWithdrawCard();
        HBox.setHgrow(depositCard, Priority.ALWAYS);
        HBox.setHgrow(withdrawCard, Priority.ALWAYS);
        mainCards.getChildren().addAll(depositCard, withdrawCard);

        VBox stateInfoCard = buildStateObserverExplainerCard();

        getChildren().addAll(mainCards, stateInfoCard);

        ctx.addDataChangeListener(this::refresh);
        refresh();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Nạp & Rút Tiền Mặt");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Giao dịch tức thì — Kiểm tra trạng thái tài khoản với State Pattern & Thông báo qua Observer");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        HBox patternBadges = new HBox(8);
        patternBadges.setAlignment(Pos.CENTER_RIGHT);
        patternBadges.getChildren().addAll(
                UiUtils.createPatternBadge("State (Active vs Locked)", "behavioral"),
                UiUtils.createPatternBadge("Strategy (Phí rút tiền)", "behavioral"),
                UiUtils.createPatternBadge("Observer (SMS & Email)", "behavioral")
        );

        HBox header = new HBox();
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, patternBadges);
        header.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(header);
    }

    private VBox buildDepositCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("📥 Nạp tiền vào tài khoản (Deposit)");
        cardTitle.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);

        setupAccountComboBox(cbDepositAccount, lblDepositBalance);
        txtDepositAmount.setPromptText("Ví dụ: 1000000");

        grid.add(new Label("Chọn tài khoản:"), 0, 0);
        grid.add(cbDepositAccount, 1, 0);

        grid.add(new Label("Số dư hiện tại:"), 0, 1);
        grid.add(lblDepositBalance, 1, 1);

        grid.add(new Label("Số tiền cần nạp:"), 0, 2);
        grid.add(txtDepositAmount, 1, 2);

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        Button btnDeposit = new Button("✔ Xác nhận Nạp tiền (Facade.deposit)");
        btnDeposit.getStyleClass().add("btn-primary");
        btnDeposit.setMaxWidth(Double.MAX_VALUE);
        btnDeposit.setOnAction(e -> handleDeposit());

        card.getChildren().addAll(cardTitle, grid, btnDeposit);
        return card;
    }

    private VBox buildWithdrawCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("📤 Rút tiền mặt (Withdraw)");
        cardTitle.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);

        setupAccountComboBox(cbWithdrawAccount, lblWithdrawBalance);
        txtWithdrawAmount.setPromptText("Ví dụ: 500000");
        txtWithdrawAmount.textProperty().addListener((obs, oldVal, newVal) -> updateWithdrawFee());
        cbWithdrawAccount.valueProperty().addListener((obs, oldVal, newVal) -> updateWithdrawFee());

        lblWithdrawFeePreview.setStyle("-fx-font-size: 11px; -fx-text-fill: #B45309; -fx-font-weight: bold;");
        lblWithdrawFeePreview.setText("Phí ước tính: 0 VND");

        grid.add(new Label("Chọn tài khoản:"), 0, 0);
        grid.add(cbWithdrawAccount, 1, 0);

        grid.add(new Label("Số dư hiện tại:"), 0, 1);
        grid.add(lblWithdrawBalance, 1, 1);

        grid.add(new Label("Số tiền cần rút:"), 0, 2);
        grid.add(txtWithdrawAmount, 1, 2);

        grid.add(new Label("Phí giao dịch:"), 0, 3);
        grid.add(lblWithdrawFeePreview, 1, 3);

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        Button btnWithdraw = new Button("⚠ Xác nhận Rút tiền (State Check)");
        btnWithdraw.getStyleClass().add("btn-navy");
        btnWithdraw.setMaxWidth(Double.MAX_VALUE);
        btnWithdraw.setOnAction(e -> handleWithdraw());

        card.getChildren().addAll(cardTitle, grid, btnWithdraw);
        return card;
    }

    private void setupAccountComboBox(ComboBox<Account> cb, Label lblBalance) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getAccountNumber() + " - " + item.getOwnerName() + " (" + item.getStatus() + ")");
                }
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getAccountNumber() + " - " + item.getOwnerName() + " (" + item.getStatus() + ")");
                }
            }
        });
        cb.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblBalance.setText(UiUtils.formatVnd(newVal.getBalance()) + " [" + newVal.getStatus() + "]");
                if (newVal.getStatus() == AccountStatus.LOCKED) {
                    lblBalance.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                } else {
                    lblBalance.setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold;");
                }
            } else {
                lblBalance.setText("-");
            }
        });
    }

    private void updateWithdrawFee() {
        Account acc = cbWithdrawAccount.getValue();
        if (acc == null) {
            lblWithdrawFeePreview.setText("Phí ước tính: 0 VND");
            return;
        }
        try {
            double amount = Money.positive(Double.parseDouble(txtWithdrawAmount.getText().trim()));
            double fee = acc.getFeeStrategy().calculateFee(amount);
            lblWithdrawFeePreview.setText(String.format("Phí ước tính: %s (Chiến lược: %s)",
                    UiUtils.formatVnd(fee), acc.getFeeStrategy().getName()));
        } catch (IllegalArgumentException e) {
            lblWithdrawFeePreview.setText("Phí ước tính: 0 VND");
        }
    }

    private void handleDeposit() {
        Account acc = cbDepositAccount.getValue();
        if (acc == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Chưa chọn tài khoản", "Vui lòng chọn tài khoản để nạp tiền.");
            return;
        }

        try {
            double amount = Double.parseDouble(txtDepositAmount.getText().trim());
            if (amount <= 0) {
                UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Số tiền không hợp lệ", "Số tiền nạp phải lớn hơn 0.");
                return;
            }

            ctx.getFacade().deposit(acc.getAccountNumber(), amount);
            ctx.notifyDataChanged();

            UiUtils.showAlert(Alert.AlertType.INFORMATION, "Nạp tiền thành công", "Giao dịch hoàn tất",
                    String.format("Đã nạp thành công %s vào tài khoản %s.\nSố dư mới: %s",
                            UiUtils.formatVnd(amount), acc.getAccountNumber(), UiUtils.formatVnd(acc.getBalance())));

            txtDepositAmount.clear();
        } catch (IllegalArgumentException | IllegalStateException e) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Không thể nạp tiền", e.getMessage());
        }
    }

    private void handleWithdraw() {
        Account acc = cbWithdrawAccount.getValue();
        if (acc == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Chưa chọn tài khoản", "Vui lòng chọn tài khoản để rút tiền.");
            return;
        }

        // Kiểm tra State Pattern: Nếu trạng thái là LOCKED
        if (acc.getStatus() == AccountStatus.LOCKED) {
            UiUtils.showAlert(Alert.AlertType.ERROR, "Lỗi State Pattern", "Tài khoản đang bị KHÓA (LockedState)",
                    "Hành vi của State Pattern: Đối tượng đang ở trạng thái LockedState, mọi thao tác rút tiền đều bị chặn!\n"
                            + "Vui lòng mở khóa tài khoản tại tab 'Mở & Quản lý TK' trước khi thực hiện.");
            return;
        }

        try {
            double amount = Double.parseDouble(txtWithdrawAmount.getText().trim());
            if (amount <= 0) {
                UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Số tiền không hợp lệ", "Số tiền rút phải lớn hơn 0.");
                return;
            }

            double fee = acc.getFeeStrategy().calculateFee(amount);
            double total = amount + fee;
            if (acc.getBalance() < total) {
                UiUtils.showAlert(Alert.AlertType.ERROR, "Lỗi số dư", "Số dư không đủ",
                        String.format("Số dư hiện tại: %s\nTổng tiền cần (gồm phí %s): %s",
                                UiUtils.formatVnd(acc.getBalance()), UiUtils.formatVnd(fee), UiUtils.formatVnd(total)));
                return;
            }

            ctx.getFacade().withdraw(acc.getAccountNumber(), amount);
            ctx.notifyDataChanged();

            UiUtils.showAlert(Alert.AlertType.INFORMATION, "Rút tiền thành công", "Giao dịch hoàn tất",
                    String.format("Đã rút thành công: %s\nPhí giao dịch: %s\nSố dư còn lại: %s",
                            UiUtils.formatVnd(amount), UiUtils.formatVnd(fee), UiUtils.formatVnd(acc.getBalance())));

            txtWithdrawAmount.clear();
        } catch (IllegalArgumentException | IllegalStateException e) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Không thể rút tiền", e.getMessage());
        }
    }

    private VBox buildStateObserverExplainerCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Giải thích luồng Pattern (State, Strategy, Observer, Facade)");
        cardTitle.getStyleClass().add("card-title");

        Label desc = new Label(
                "1. Facade Pattern: Ứng dụng chỉ cần gọi facade.deposit() hoặc facade.withdraw() thay vì phải tự debit, credit, log và notify.\n"
                        + "2. State Pattern: Account ủy quyền deposit/withdraw cho AccountState (ActiveState vs LockedState). Nếu bị khóa, state sẽ từ chối giao dịch.\n"
                        + "3. Strategy Pattern: Chiến lược phí tự động áp dụng (Standard = 0.1%, Savings = bậc thang, Premium = miễn phí).\n"
                        + "4. Observer Pattern: Sau khi hoàn thành, hệ thống tự động bắn tín hiệu tới SmsNotifier, EmailNotifier và giao diện JavaFX."
        );
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-line-spacing: 4px;");

        card.getChildren().addAll(cardTitle, desc);
        return card;
    }

    public void refresh() {
        Account selectedDeposit = cbDepositAccount.getValue();
        Account selectedWithdraw = cbWithdrawAccount.getValue();

        cbDepositAccount.setItems(ctx.getAccounts());
        cbWithdrawAccount.setItems(ctx.getAccounts());

        if (selectedDeposit != null) {
            ctx.getAccounts().stream()
                    .filter(a -> a.getAccountNumber().equals(selectedDeposit.getAccountNumber()))
                    .findFirst()
                    .ifPresent(cbDepositAccount::setValue);
        } else if (!ctx.getAccounts().isEmpty()) {
            cbDepositAccount.setValue(ctx.getAccounts().get(0));
        }

        if (selectedWithdraw != null) {
            ctx.getAccounts().stream()
                    .filter(a -> a.getAccountNumber().equals(selectedWithdraw.getAccountNumber()))
                    .findFirst()
                    .ifPresent(cbWithdrawAccount::setValue);
        } else if (!ctx.getAccounts().isEmpty()) {
            cbWithdrawAccount.setValue(ctx.getAccounts().get(0));
        }

        updateWithdrawFee();
    }
}

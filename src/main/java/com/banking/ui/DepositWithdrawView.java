package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.model.enums.AccountStatus;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.regex.Pattern;

/**
 * Màn hình Nạp & Rút tiền.
 * Minh họa Patterns:
 * - State Pattern: Khi tài khoản ở LockedState, lệnh rút tiền sẽ bị từ chối ngay lập tức.
 * - Strategy Pattern: Tính phí rút tiền theo chiến lược riêng của từng tài khoản.
 * - Observer Pattern: Kích hoạt thông báo đa kênh (SMS, Email, UI) sau giao dịch.
 * - Facade Pattern: Gọi qua BankingFacade để xử lý trọn gói.
 *
 * Tính năng nâng cao:
 * - TextFormatter chỉ nhận số nguyên.
 * - Real-time validation cảnh báo khi vượt số dư hoặc tài khoản bị khóa.
 * - Phản hồi Toast Notification và thông báo lỗi thân thiện (Humanized Error).
 */
public class DepositWithdrawView extends VBox {

    private final UIContext ctx = UIContext.getInstance();

    // Controls Nạp tiền
    private final ComboBox<Account> cbDepositAccount = new ComboBox<>();
    private final TextField txtDepositAmount = new TextField();
    private final Label lblDepositBalance = new Label();
    private final Label lblDepositValidation = new Label();
    private final Button btnDeposit = new Button("✔ Xác nhận Nạp tiền (Facade.deposit)");

    // Controls Rút tiền
    private final ComboBox<Account> cbWithdrawAccount = new ComboBox<>();
    private final TextField txtWithdrawAmount = new TextField();
    private final Label lblWithdrawBalance = new Label();
    private final Label lblWithdrawFeePreview = new Label();
    private final Label lblWithdrawValidation = new Label();
    private final Button btnWithdraw = new Button("⚠ Xác nhận Rút tiền (State Check)");

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
        Label subtitle = new Label("Giao dịch tức thì — Kiểm tra trạng thái tài khoản với State Pattern & Validation thời gian thực");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        HBox patternBadges = new HBox(8);
        patternBadges.setAlignment(Pos.CENTER_RIGHT);
        patternBadges.getChildren().addAll(
                UiUtils.createPatternBadge("State (ActiveState / LockedState)", "behavioral"),
                UiUtils.createPatternBadge("Strategy (Fee Calculation)", "behavioral"),
                UiUtils.createPatternBadge("Facade (BankingFacade)", "structural")
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

        // TextFormatter chỉ nhận số
        Pattern digitPattern = Pattern.compile("\\d*");
        txtDepositAmount.setTextFormatter(new TextFormatter<>(change ->
                digitPattern.matcher(change.getControlNewText()).matches() ? change : null
        ));
        txtDepositAmount.setPromptText("Nhập số tiền nạp (VND)");

        lblDepositValidation.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        txtDepositAmount.textProperty().addListener((obs, oldVal, newVal) -> validateDeposit());
        cbDepositAccount.valueProperty().addListener((obs, oldVal, newVal) -> validateDeposit());

        VBox amountBox = new VBox(4);
        amountBox.getChildren().addAll(txtDepositAmount, lblDepositValidation);

        grid.add(new Label("Chọn tài khoản:"), 0, 0);
        grid.add(cbDepositAccount, 1, 0);

        grid.add(new Label("Số dư hiện tại:"), 0, 1);
        grid.add(lblDepositBalance, 1, 1);

        grid.add(new Label("Số tiền cần nạp:"), 0, 2);
        grid.add(amountBox, 1, 2);

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

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

        // TextFormatter chỉ nhận số
        Pattern digitPattern = Pattern.compile("\\d*");
        txtWithdrawAmount.setTextFormatter(new TextFormatter<>(change ->
                digitPattern.matcher(change.getControlNewText()).matches() ? change : null
        ));
        txtWithdrawAmount.setPromptText("Nhập số tiền rút (VND)");

        lblWithdrawFeePreview.setStyle("-fx-font-size: 11px; -fx-text-fill: #B45309; -fx-font-weight: bold;");
        lblWithdrawFeePreview.setText("Phí ước tính: 0 VND");
        lblWithdrawValidation.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        txtWithdrawAmount.textProperty().addListener((obs, oldVal, newVal) -> validateWithdraw());
        cbWithdrawAccount.valueProperty().addListener((obs, oldVal, newVal) -> validateWithdraw());

        VBox amountBox = new VBox(4);
        amountBox.getChildren().addAll(txtWithdrawAmount, lblWithdrawValidation);

        grid.add(new Label("Chọn tài khoản:"), 0, 0);
        grid.add(cbWithdrawAccount, 1, 0);

        grid.add(new Label("Số dư hiện tại:"), 0, 1);
        grid.add(lblWithdrawBalance, 1, 1);

        grid.add(new Label("Số tiền cần rút:"), 0, 2);
        grid.add(amountBox, 1, 2);

        grid.add(new Label("Phí giao dịch:"), 0, 3);
        grid.add(lblWithdrawFeePreview, 1, 3);

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

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

    private void validateDeposit() {
        Account acc = cbDepositAccount.getValue();
        if (acc == null) {
            lblDepositValidation.setText("Vui lòng chọn tài khoản.");
            lblDepositValidation.setStyle("-fx-text-fill: #64748B;");
            txtDepositAmount.setStyle("");
            btnDeposit.setDisable(true);
            return;
        }

        String amtStr = txtDepositAmount.getText().trim();
        if (amtStr.isEmpty()) {
            lblDepositValidation.setText("Vui lòng nhập số tiền nạp.");
            lblDepositValidation.setStyle("-fx-text-fill: #64748B;");
            txtDepositAmount.setStyle("");
            btnDeposit.setDisable(true);
            return;
        }

        try {
            double amt = Double.parseDouble(amtStr);
            if (amt <= 0) {
                lblDepositValidation.setText("⚠️ Số tiền phải lớn hơn 0 VND.");
                lblDepositValidation.setStyle("-fx-text-fill: #DC2626;");
                txtDepositAmount.setStyle("-fx-border-color: #EF4444; -fx-border-radius: 8px;");
                btnDeposit.setDisable(true);
            } else {
                double newBal = acc.getBalance() + amt;
                lblDepositValidation.setText("✔ Hợp lệ. Số dư sau nạp: " + UiUtils.formatVnd(newBal));
                lblDepositValidation.setStyle("-fx-text-fill: #15803D;");
                txtDepositAmount.setStyle("-fx-border-color: #10B981; -fx-border-radius: 8px;");
                btnDeposit.setDisable(false);
            }
        } catch (NumberFormatException e) {
            lblDepositValidation.setText("⚠️ Số tiền không hợp lệ.");
            lblDepositValidation.setStyle("-fx-text-fill: #DC2626;");
            txtDepositAmount.setStyle("-fx-border-color: #EF4444; -fx-border-radius: 8px;");
            btnDeposit.setDisable(true);
        }
    }

    private void validateWithdraw() {
        Account acc = cbWithdrawAccount.getValue();
        if (acc == null) {
            lblWithdrawValidation.setText("Vui lòng chọn tài khoản.");
            lblWithdrawValidation.setStyle("-fx-text-fill: #64748B;");
            txtWithdrawAmount.setStyle("");
            btnWithdraw.setDisable(true);
            return;
        }

        // Kiểm tra State Pattern
        if (acc.getStatus() == AccountStatus.LOCKED) {
            lblWithdrawValidation.setText("⚠️ Tài khoản đang bị KHÓA (LockedState). Thao tác rút tiền bị chặn!");
            lblWithdrawValidation.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
            txtWithdrawAmount.setStyle("-fx-border-color: #EF4444; -fx-border-radius: 8px;");
            btnWithdraw.setDisable(true);
            return;
        }

        String amtStr = txtWithdrawAmount.getText().trim();
        if (amtStr.isEmpty()) {
            lblWithdrawFeePreview.setText("Phí ước tính: 0 VND");
            lblWithdrawValidation.setText("Vui lòng nhập số tiền rút.");
            lblWithdrawValidation.setStyle("-fx-text-fill: #64748B;");
            txtWithdrawAmount.setStyle("");
            btnWithdraw.setDisable(true);
            return;
        }

        try {
            double amt = Double.parseDouble(amtStr);
            if (amt <= 0) {
                lblWithdrawValidation.setText("⚠️ Số tiền phải lớn hơn 0 VND.");
                lblWithdrawValidation.setStyle("-fx-text-fill: #DC2626;");
                txtWithdrawAmount.setStyle("-fx-border-color: #EF4444; -fx-border-radius: 8px;");
                btnWithdraw.setDisable(true);
                return;
            }

            double fee = Money.nonNegative(acc.getFeeStrategy().calculateFee(amt));
            lblWithdrawFeePreview.setText(String.format("Phí: %s (%s)",
                    UiUtils.formatVnd(fee), acc.getFeeStrategy().getName()));

            double total = amt + fee;
            if (acc.getBalance() < total) {
                lblWithdrawValidation.setText(String.format(
                        "⚠️ Số dư không đủ! Cần: %s (gồm phí), Khả dụng: %s",
                        UiUtils.formatVnd(total), UiUtils.formatVnd(acc.getBalance())
                ));
                lblWithdrawValidation.setStyle("-fx-text-fill: #DC2626;");
                txtWithdrawAmount.setStyle("-fx-border-color: #EF4444; -fx-border-radius: 8px;");
                btnWithdraw.setDisable(true);
            } else {
                double remaining = acc.getBalance() - total;
                lblWithdrawValidation.setText("✔ Hợp lệ. Số dư dự kiến còn lại: " + UiUtils.formatVnd(remaining));
                lblWithdrawValidation.setStyle("-fx-text-fill: #15803D;");
                txtWithdrawAmount.setStyle("-fx-border-color: #10B981; -fx-border-radius: 8px;");
                btnWithdraw.setDisable(false);
            }
        } catch (NumberFormatException e) {
            lblWithdrawValidation.setText("⚠️ Số tiền không hợp lệ.");
            lblWithdrawValidation.setStyle("-fx-text-fill: #DC2626;");
            txtWithdrawAmount.setStyle("-fx-border-color: #EF4444; -fx-border-radius: 8px;");
            btnWithdraw.setDisable(true);
        }
    }

    private void handleDeposit() {
        Account acc = cbDepositAccount.getValue();
        if (acc == null) {
            ToastNotification.showWarning("Vui lòng chọn tài khoản để nạp tiền.");
            return;
        }

        try {
            double amount = Money.positive(Double.parseDouble(txtDepositAmount.getText().trim()));
            ctx.getFacade().deposit(acc.getAccountNumber(), amount);
            ctx.notifyDataChanged();

            ToastNotification.showSuccess(String.format("Đã nạp thành công %s vào tài khoản %s!",
                    UiUtils.formatVnd(amount), acc.getAccountNumber()));

            txtDepositAmount.clear();
            validateDeposit();
        } catch (Exception e) {
            String error = UiUtils.humanizeError(e);
            ToastNotification.showError(error);
            UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể nạp tiền", null, error);
        }
    }

    private void handleWithdraw() {
        Account acc = cbWithdrawAccount.getValue();
        if (acc == null) {
            ToastNotification.showWarning("Vui lòng chọn tài khoản để rút tiền.");
            return;
        }

        // Kiểm tra State Pattern: Nếu trạng thái là LOCKED
        if (acc.getStatus() == AccountStatus.LOCKED) {
            String stateMsg = "Hành vi State Pattern: Tài khoản đang ở trạng thái LockedState, mọi thao tác rút tiền đều bị chặn!";
            ToastNotification.showError("Tài khoản đang bị KHÓA (LockedState)!");
            UiUtils.showAlert(Alert.AlertType.ERROR, "Lỗi State Pattern", "Tài khoản đang bị KHÓA", stateMsg);
            return;
        }

        try {
            double amount = Money.positive(Double.parseDouble(txtWithdrawAmount.getText().trim()));
            double fee = Money.nonNegative(acc.getFeeStrategy().calculateFee(amount));
            double total = Money.add(amount, fee);

            if (acc.getBalance() < total) {
                ToastNotification.showError("Số dư không đủ để rút tiền và thanh toán phí!");
                return;
            }

            ctx.getFacade().withdraw(acc.getAccountNumber(), amount);
            ctx.notifyDataChanged();

            ToastNotification.showSuccess(String.format("Đã rút thành công: %s (Phí: %s)",
                    UiUtils.formatVnd(amount), UiUtils.formatVnd(fee)));

            txtWithdrawAmount.clear();
            validateWithdraw();
        } catch (Exception e) {
            String error = UiUtils.humanizeError(e);
            ToastNotification.showError(error);
            UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể rút tiền", null, error);
        }
    }

    private VBox buildStateObserverExplainerCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Giải thích luồng Pattern (State, Strategy, Observer, Facade)");
        cardTitle.getStyleClass().add("card-title");

        Label desc = new Label(
                "• State Pattern: Quản lý trạng thái đóng/mở rút tiền tự động. Tài khoản ở LockedState sẽ từ chối rút tiền ngay tại State.withdraw().\n"
                        + "• Strategy Pattern: Tính toán phí rút tiền linh hoạt theo loại tài khoản (Standard: 0.1%, Premium: 0%, Savings: bậc thang).\n"
                        + "• Facade Pattern: BankingFacade đóng gói trọn gói thao tác rút tiền, trừ số dư, ghi log Transaction và gọi Observer phát thông báo.\n"
                        + "• Observer Pattern: Cập nhật biến động số dư tới SMS, Email và luồng dữ liệu thời gian thực trên Dashboard."
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

        validateDeposit();
        validateWithdraw();
    }

    public void selectDepositAccount(String accNo) {
        if (accNo == null) return;
        ctx.getAccounts().stream()
                .filter(a -> a.getAccountNumber().equalsIgnoreCase(accNo))
                .findFirst()
                .ifPresent(cbDepositAccount::setValue);
    }

    public void selectWithdrawAccount(String accNo) {
        if (accNo == null) return;
        ctx.getAccounts().stream()
                .filter(a -> a.getAccountNumber().equalsIgnoreCase(accNo))
                .findFirst()
                .ifPresent(cbWithdrawAccount::setValue);
    }
}

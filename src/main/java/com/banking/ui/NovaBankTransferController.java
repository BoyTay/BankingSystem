package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.pattern.creational.TransferTemplate;
import com.banking.pattern.creational.TransferTemplate;
import com.banking.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.function.Consumer;

/**
 * Controller cho giao diện NovaBank Transfer & Payment Wizard (Figma Step-by-Step).
 * Tích hợp 4 Design Patterns:
 * - Strategy Pattern: Tính toán phí giao dịch động theo FeeStrategy của tài khoản trích tiền.
 * - State Pattern: Kiểm tra trạng thái ActiveState / LockedState của tài khoản nguồn.
 * - Facade Pattern: Gọi BankingFacade.transfer() để xử lý trọn gói.
 * - Command Pattern: Đóng gói thành TransferCommand và lưu vào TransactionHistory để hỗ trợ Undo.
 */
public class NovaBankTransferController implements Initializable, NovaBankNavigable {

    @FXML private ComboBox<Account> cbFromAccount;
    @FXML private ComboBox<Account> cbToAccount;
    @FXML private Button btnChangeRecipient;
    @FXML private Label lblRecipientAvatar;
    @FXML private Label lblRecipientName;
    @FXML private Label lblRecipientDetails;

    @FXML private TextField txtSendAmount;
    @FXML private Label lblAvailableBalance;
    @FXML private TextField txtReceiveAmount;
    @FXML private Label lblRateLockedTimer;
    @FXML private TextField txtReference;
    @FXML private CheckBox chkSaveRecipient;
    @FXML private HBox errorBanner;
    @FXML private Label lblValidationError;
    @FXML private Button btnReviewTransfer;

    // Stepper Nodes
    @FXML private javafx.scene.layout.Region stepLine2;
    @FXML private javafx.scene.layout.StackPane stepIcon3;
    @FXML private Label stepNum3;
    @FXML private Label stepTitle3;

    // Summary Labels
    @FXML private Label lblSummarySendAmount;
    @FXML private Label lblSummaryFee;
    @FXML private Label lblSummaryTotal;

    // Header Labels
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileRole;
    @FXML private Label lblProfileAvatar;

    public void setUser(AuthService.User user) {
        String name = user.username();
        String display = name.substring(0, 1).toUpperCase() + name.substring(1);
        if (lblProfileName != null) lblProfileName.setText(display);
        if (lblProfileRole != null) {
            String roleStr = user.role().name().toLowerCase();
            roleStr = roleStr.substring(0, 1).toUpperCase() + roleStr.substring(1);
            lblProfileRole.setText(roleStr);
        }
        if (lblProfileAvatar != null && !name.isEmpty()) {
            lblProfileAvatar.setText(name.substring(0, 1).toUpperCase());
        }
    }

    private final UIContext ctx = UIContext.getInstance();
    private Consumer<String> navigator = key -> { };

    @Override
    public void setNavigator(Consumer<String> navigator) { this.navigator = navigator; }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupAccountCombos();
        setupAmountValidation();

        loadInitialData();

        // ── OBSERVER PATTERN HOOK ──────────────────────────────────────────
        ctx.addDataChangeListener(() -> Platform.runLater(this::refreshData));
    }

    private void setupAccountCombos() {
        cbFromAccount.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getOwnerName() + " (" + item.getAccountNumber() + ") • "
                            + UiUtils.formatVnd(item.getBalance()) + " [" + item.getStatus() + "]");
                }
            }
        });
        cbFromAccount.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Select source account");
                } else {
                    setText(String.format("💳 %s (•••• %s) • %s",
                            item.getOwnerName(),
                            item.getAccountNumber().length() > 4 ? item.getAccountNumber().substring(item.getAccountNumber().length() - 4) : item.getAccountNumber(),
                            UiUtils.formatVnd(item.getBalance())));
                }
            }
        });

        cbToAccount.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s (%s)", item.getAccountNumber(), item.getOwnerName(), item.getType()));
                }
            }
        });
        cbToAccount.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s (%s)", item.getAccountNumber(), item.getOwnerName(), item.getType()));
                }
            }
        });

        cbFromAccount.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblAvailableBalance.setText("Số dư khả dụng: " + UiUtils.formatVnd(newVal.getBalance()));
            } else {
                lblAvailableBalance.setText("Số dư khả dụng: 0 VND");
            }
            updateCalculations();
        });

        cbToAccount.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblRecipientName.setText(newVal.getOwnerName());
                String last4 = newVal.getAccountNumber().length() > 4
                        ? newVal.getAccountNumber().substring(newVal.getAccountNumber().length() - 4)
                        : newVal.getAccountNumber();
                lblRecipientDetails.setText(String.format("NovaBank • VND • •••• %s", last4));

                // Sinh avatar initials
                String initials = getInitials(newVal.getOwnerName());
                lblRecipientAvatar.setText(initials);
            } else {
                lblRecipientName.setText("Chọn tài khoản nhận");
                lblRecipientDetails.setText("");
                lblRecipientAvatar.setText("?");
            }
            updateCalculations();
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "NB";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private void setupAmountValidation() {
        Pattern pattern = Pattern.compile("\\d*");
        txtSendAmount.setTextFormatter(new TextFormatter<>(change ->
                pattern.matcher(change.getControlNewText()).matches() ? change : null
        ));

        txtSendAmount.textProperty().addListener((obs, oldVal, newVal) -> updateCalculations());
    }

    private void loadInitialData() {
        List<Account> accounts = ctx.getAccounts();
        if (accounts != null && !accounts.isEmpty()) {
            cbFromAccount.getItems().setAll(accounts);
            cbToAccount.getItems().setAll(accounts);

            cbFromAccount.setValue(accounts.get(0));
            if (accounts.size() > 1) {
                cbToAccount.setValue(accounts.get(1));
            } else {
                cbToAccount.setValue(null);
            }
        } else {
            lblRecipientName.setText("Chọn tài khoản nhận");
            lblRecipientDetails.setText("");
            lblRecipientAvatar.setText("?");
        }
        updateCalculations();
    }

    private void refreshData() {
        TransferTemplate pending = ctx.consumePendingTransferTemplate();
        if (pending != null) {
            applyTemplate(pending);
            return;
        }
        Account currentFrom = cbFromAccount.getValue();
        Account currentTo = cbToAccount.getValue();

        List<Account> accounts = ctx.getAccounts();
        cbFromAccount.getItems().setAll(accounts);
        cbToAccount.getItems().setAll(accounts);

        if (currentFrom != null) {
            accounts.stream().filter(a -> a.getAccountNumber().equals(currentFrom.getAccountNumber())).findFirst().ifPresent(cbFromAccount::setValue);
        }
        if (currentTo != null) {
            accounts.stream().filter(a -> a.getAccountNumber().equals(currentTo.getAccountNumber())).findFirst().ifPresent(cbToAccount::setValue);
        }
        updateCalculations();
    }

    /**
     * Tính toán thời gian thực: quy đổi tiền tệ, tính phí theo Strategy và cập nhật Summary Card
     */
    private void updateCalculations() {
        hideError();
        Account from = cbFromAccount.getValue();
        Account to = cbToAccount.getValue();

        String sendText = txtSendAmount.getText() != null ? txtSendAmount.getText().trim() : "";
        if (sendText.isEmpty() || from == null) {
            txtReceiveAmount.setText("0 VND");
            lblSummarySendAmount.setText("0 VND");
            lblSummaryFee.setText("0 VND");
            lblSummaryTotal.setText("0 VND");
            btnReviewTransfer.setDisable(true);
            return;
        }

        double amount;
        try {
            amount = Money.positive(Double.parseDouble(sendText));
        } catch (IllegalArgumentException ex) {
            showError(UiUtils.humanizeError(ex));
            btnReviewTransfer.setDisable(true);
            return;
        }

        txtReceiveAmount.setText(UiUtils.formatVnd(amount));

        // ── STRATEGY PATTERN: TÍNH PHÍ GIAO DỊCH ─────────────────────
        double fee;
        double totalToPay;
        try {
            fee = Money.nonNegative(from.getFeeStrategy().calculateFee(amount));
            totalToPay = Money.add(amount, fee);
        } catch (IllegalArgumentException ex) {
            showError(UiUtils.humanizeError(ex));
            btnReviewTransfer.setDisable(true);
            return;
        }

        lblSummarySendAmount.setText(UiUtils.formatVnd(amount));
        lblSummaryFee.setText(UiUtils.formatVnd(fee));
        lblSummaryTotal.setText(UiUtils.formatVnd(totalToPay));

        // ── VALIDATION KIỂM TRA ĐIỀU KIỆN ─────────────────────────────
        if (from.getStatus() != AccountStatus.ACTIVE) {
            showError("Tài khoản nguồn chưa ở trạng thái hoạt động.");
            btnReviewTransfer.setDisable(true);
            return;
        }

        if (to == null || from.getAccountNumber().equals(to.getAccountNumber())) {
            showError("Source and destination accounts cannot be identical.");
            btnReviewTransfer.setDisable(true);
            return;
        }

        if (to.getStatus() == AccountStatus.CLOSED || to.getStatus() == AccountStatus.SUSPENDED) {
            showError("Tài khoản nhận không thể nhận tiền.");
            btnReviewTransfer.setDisable(true);
            return;
        }

        if (from.getBalance() < totalToPay) {
            showError("Số dư không đủ. Cần " + UiUtils.formatVnd(totalToPay)
                    + ", hiện có " + UiUtils.formatVnd(from.getBalance()));
            btnReviewTransfer.setDisable(true);
            return;
        }

        btnReviewTransfer.setDisable(false);
    }

    @FXML
    private void handleChangeRecipient() {
        cbToAccount.show();
    }

    @FXML
    private void handleReviewTransfer() {
        Account from = cbFromAccount.getValue();
        Account to = cbToAccount.getValue();

        if (from == null || to == null) {
            showError("Please select both source and destination accounts.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(txtSendAmount.getText().trim());
        } catch (Exception ex) {
            showError("Invalid amount.");
            return;
        }

        String ref = txtReference.getText() != null && !txtReference.getText().trim().isEmpty()
                ? txtReference.getText().trim()
                : "Chuyển khoản";

        // Simulate Step 3 visually!
        stepLine2.getStyleClass().setAll("step-line-completed");
        stepIcon3.getStyleClass().setAll("step-icon-active");
        stepNum3.getStyleClass().setAll("step-num-active");
        stepTitle3.getStyleClass().setAll("step-title-active");

        // Show custom premium confirmation dialog
        javafx.stage.Stage ownerStage = (javafx.stage.Stage) btnReviewTransfer.getScene().getWindow();
        String displayAmount = UiUtils.formatVnd(amount) + " VND";
        String displayFrom = from.getOwnerName() + " (•••• " + from.getAccountNumber().substring(from.getAccountNumber().length() - 4) + ")";
        String displayFee = "Miễn phí";

        boolean userConfirmed = ConfirmTransferDialog.show(
                ownerStage, displayAmount, to.getOwnerName(), displayFrom, displayFee, ref);

        if (userConfirmed) {
            // ── FACADE & COMMAND PATTERN: THỰC THI CHUYỂN TIỀN AN TOÀN ──
            try {
                Transaction tx = ctx.getFacade().transfer(
                        from.getAccountNumber(), to.getAccountNumber(), amount, ref);

                ctx.logCustomEvent("Command Pattern", "Executed transfer " + tx.getId());
                ctx.notifyDataChanged();

                ToastNotification.showSuccess("Đã chuyển " + UiUtils.formatVnd(amount) + " đến " + to.getOwnerName());

                txtSendAmount.clear();
                updateCalculations();
            } catch (Exception ex) {
                String friendly = UiUtils.humanizeError(ex);
                showError(friendly);
                ToastNotification.showError(friendly);
            }
        }

        // Reset Step 3 back to pending after the dialog closes
        stepLine2.getStyleClass().setAll("step-line-pending");
        stepIcon3.getStyleClass().setAll("step-icon-pending");
        stepNum3.getStyleClass().setAll("step-num-pending");
        stepTitle3.getStyleClass().setAll("step-title-pending");
    }

    @FXML
    private void handleNavOverview() {
        navigator.accept("dashboard");
    }

    @FXML
    private void handleNavTransactions() {
        navigator.accept("history");
    }

    @FXML
    private void handleNavAccounts() {
        navigator.accept("accounts");
    }

    private void showError(String msg) {
        lblValidationError.setText(msg);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
    }

    private void hideError() {
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }

    public void applyTemplate(TransferTemplate template) {
        if (template == null) return;
        List<Account> accounts = ctx.getAccounts();
        cbFromAccount.getItems().setAll(accounts);
        cbToAccount.getItems().setAll(accounts);

        accounts.stream().filter(a -> a.getAccountNumber().equals(template.getFromAccountNumber()))
                .findFirst().ifPresent(cbFromAccount::setValue);

        accounts.stream().filter(a -> a.getAccountNumber().equals(template.getToAccountNumber()))
                .findFirst().ifPresent(cbToAccount::setValue);

        txtSendAmount.setText(String.valueOf((long) template.getAmount()));
        if (txtReference != null) {
            txtReference.setText(template.getDescription());
        }
        updateCalculations();
    }
}
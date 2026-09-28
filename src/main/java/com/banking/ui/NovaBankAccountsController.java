package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import com.banking.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller cho giao diện NovaBank Accounts & Statements (Figma Details & Cards).
 * Tích hợp Design Patterns:
 * - State Pattern: Cho phép chuyển đổi trạng thái ActiveState / LockedState ngay tức thì từ thẻ an ninh.
 * - Builder Pattern: Trích xuất các tham số cấu hình tài khoản hoàn chỉnh từ Account.
 * - Observer Pattern: Tự động cập nhật số dư và trạng thái trên toàn bộ các thẻ card.
 */
public class NovaBankAccountsController implements Initializable, NovaBankNavigable {

    // Top Account Cards
    @FXML private VBox cardStandard;
    @FXML private ComboBox<Account> cbAllAccounts;
    @FXML private Label lblCard1Balance;
    @FXML private Label lblCard1Selected;

    @FXML private VBox cardSavings;
    @FXML private Label lblCard2Balance;

    @FXML private VBox cardPremium;
    @FXML private Label lblCard3Status;
    @FXML private Label lblCard3Balance;

    // Detail Panel
    @FXML private Label lblDetailAccountTitle;
    @FXML private Label lblDetailAccountDesc;
    @FXML private Label lblDetailStatusBadge;
    @FXML private Label lblDetailHolder;
    @FXML private Label lblDetailNumber;

    // Mini Stats
    @FXML private Label lblStatCurrentBalance;
    @FXML private Label lblStatAvailableSpend;

    // Security & State Pattern Card
    @FXML private Label lblSecurityLockTitle;
    @FXML private Label lblSecurityLockDesc;
    @FXML private Button btnToggleLock;

    private final UIContext ctx = UIContext.getInstance();
    private Account selectedAccount;

    private Account standardAccount;
    private Account savingsAccount;
    private Account premiumAccount;
    private AuthService.Role role = AuthService.Role.VIEWER;
    private Consumer<String> navigator = key -> { };

    @Override
    public void setNavigator(Consumer<String> navigator) { this.navigator = navigator; }

    public void setRole(AuthService.Role role) {
        this.role = role;
        btnToggleLock.setVisible(role == AuthService.Role.ADMIN);
        btnToggleLock.setManaged(role == AuthService.Role.ADMIN);
        updateDetailsPanel();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbAllAccounts.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                setText(empty || account == null ? "Chọn tài khoản" : account.getAccountNumber() + " — " + account.getOwnerName());
            }
        });
        cbAllAccounts.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                setText(empty || account == null ? null : account.getAccountNumber() + " — " + account.getOwnerName());
            }
        });
        cbAllAccounts.valueProperty().addListener((obs, old, current) -> selectAccount(current));
        bindAccountsFromDatabase();

        // ── OBSERVER PATTERN HOOK ──────────────────────────────────────────
        ctx.addDataChangeListener(() -> Platform.runLater(this::bindAccountsFromDatabase));
    }

    private void bindAccountsFromDatabase() {
        List<Account> accounts = ctx.getAccounts();
        standardAccount = null;
        savingsAccount = null;
        premiumAccount = null;
        if (accounts == null || accounts.isEmpty()) {
            cbAllAccounts.getItems().clear();
            selectedAccount = null;
            lblDetailHolder.setText("Chưa có tài khoản");
            lblDetailNumber.setText("—");
            lblStatCurrentBalance.setText("0 VND");
            lblStatAvailableSpend.setText("0 VND");
            lblCard1Balance.setText("0 VND");
            lblCard2Balance.setText("0 VND");
            lblCard3Balance.setText("0 VND");
            btnToggleLock.setDisable(true);
            return;
        }

        for (Account a : accounts) {
            if (a.getType() == AccountType.STANDARD && standardAccount == null) {
                standardAccount = a;
            } else if (a.getType() == AccountType.SAVINGS && savingsAccount == null) {
                savingsAccount = a;
            } else if (a.getType() == AccountType.PREMIUM && premiumAccount == null) {
                premiumAccount = a;
            }
        }

        String selectedNumber = selectedAccount == null ? null : selectedAccount.getAccountNumber();
        cbAllAccounts.getItems().setAll(accounts);
        Account next = accounts.stream().filter(a -> a.getAccountNumber().equals(selectedNumber)).findFirst().orElse(accounts.get(0));
        cbAllAccounts.setValue(next);
        selectAccount(next);
        refreshDisplay();
    }

    private void selectAccount(Account account) {
        this.selectedAccount = account;
        if (account == null) return;
        if (cbAllAccounts.getValue() != account) cbAllAccounts.setValue(account);

        // Cập nhật Highlight Card
        cardStandard.getStyleClass().remove("account-card-selected");
        cardSavings.getStyleClass().remove("account-card-selected");
        cardPremium.getStyleClass().remove("account-card-selected");
        lblCard1Selected.setVisible(false);

        lblDetailAccountTitle.setText("Tài khoản " + account.getType());
        lblDetailAccountDesc.setText(account.getAccountNumber());
        if (account == standardAccount) {
            cardStandard.getStyleClass().add("account-card-selected");
            lblCard1Selected.setVisible(true);
        } else if (account == savingsAccount) {
            cardSavings.getStyleClass().add("account-card-selected");
        } else if (account == premiumAccount) {
            cardPremium.getStyleClass().add("account-card-selected");
        }

        updateDetailsPanel();
    }

    private void updateDetailsPanel() {
        if (selectedAccount == null) return;

        lblDetailHolder.setText(selectedAccount.getOwnerName());
        lblDetailNumber.setText(selectedAccount.getAccountNumber());

        double bal = selectedAccount.getBalance();
        lblStatCurrentBalance.setText(UiUtils.formatVnd(bal));
        lblStatAvailableSpend.setText(UiUtils.formatVnd(selectedAccount.getStatus() == AccountStatus.ACTIVE ? bal : 0));
        btnToggleLock.setDisable(selectedAccount.getStatus() != AccountStatus.ACTIVE
                && selectedAccount.getStatus() != AccountStatus.LOCKED);

        // ── STATE PATTERN: CẬP NHẬT TRẠNG THÁI VÀ THẺ AN NINH ───────────────
        boolean isLocked = selectedAccount.getStatus() == AccountStatus.LOCKED;
        if (isLocked) {
            lblDetailStatusBadge.setText("● Locked");
            lblDetailStatusBadge.getStyleClass().setAll("status-badge-locked");

            lblSecurityLockTitle.setText("Account is currently LOCKED");
            lblSecurityLockDesc.setText("State Pattern: Outgoing payments are restricted. Click below to unlock.");
            btnToggleLock.setText("🔓  Unlock account (ActiveState)");
            btnToggleLock.setStyle("-fx-text-fill: #15803D; -fx-font-weight: 800;");
        } else {
            lblDetailStatusBadge.setText("● " + selectedAccount.getStatus());
            lblDetailStatusBadge.getStyleClass().setAll("status-badge-active");

            lblSecurityLockTitle.setText("Need to lock this account?");
            lblSecurityLockDesc.setText("Restrict outgoing payments instantly via State Pattern.");
            btnToggleLock.setText("🔒  Lock this account (LockedState)");
            btnToggleLock.setStyle("-fx-text-fill: #081225; -fx-font-weight: 800;");
        }
        if (role != AuthService.Role.ADMIN) {
            lblSecurityLockDesc.setText("Chỉ quản trị viên được khóa hoặc mở khóa tài khoản.");
        }
    }

    private void refreshDisplay() {
        if (standardAccount != null) {
            lblCard1Balance.setText(UiUtils.formatVnd(standardAccount.getBalance()));
        } else {
            lblCard1Balance.setText("Chưa có");
        }
        if (savingsAccount != null) {
            lblCard2Balance.setText(UiUtils.formatVnd(savingsAccount.getBalance()));
        } else {
            lblCard2Balance.setText("Chưa có");
        }
        if (premiumAccount != null) {
            lblCard3Balance.setText(UiUtils.formatVnd(premiumAccount.getBalance()));
            boolean isLocked = premiumAccount.getStatus() == AccountStatus.LOCKED;
            lblCard3Status.setText(isLocked ? "● Locked" : "● Active");
            lblCard3Status.getStyleClass().setAll(isLocked ? "status-badge-locked" : "status-badge-active");
        } else {
            lblCard3Balance.setText("Chưa có");
            lblCard3Status.setText("—");
        }
        updateDetailsPanel();
    }

    @FXML
    public void selectStandardAccount() {
        if (standardAccount != null) selectAccount(standardAccount);
    }

    @FXML
    public void selectSavingsAccount() {
        if (savingsAccount != null) selectAccount(savingsAccount);
    }

    @FXML
    public void selectPremiumAccount() {
        if (premiumAccount != null) selectAccount(premiumAccount);
    }

    @FXML
    private void handleToggleAccountState() {
        // ── STATE PATTERN: CHUYỂN ĐỔI GIỮA ACTIVESTATE VÀ LOCKEDSTATE ──────
        if (selectedAccount == null || role != AuthService.Role.ADMIN) return;

        try {
            if (selectedAccount.getStatus() == AccountStatus.ACTIVE) {
                ctx.getAccountService().lockAccount(selectedAccount.getAccountNumber());
                ToastNotification.showWarning("Đã khóa tài khoản " + selectedAccount.getAccountNumber());
            } else {
                ctx.getAccountService().unlockAccount(selectedAccount.getAccountNumber());
                ToastNotification.showSuccess("Đã mở khóa tài khoản " + selectedAccount.getAccountNumber());
            }
            ctx.notifyDataChanged();
        } catch (RuntimeException e) {
            ToastNotification.showError(UiUtils.humanizeError(e));
        }
    }

    @FXML
    private void copyAccountNumber() {
        copyToClipboard(lblDetailNumber.getText(), "Account number");
    }

    private void copyToClipboard(String text, String fieldName) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        ToastNotification.showInfo("Copied " + fieldName + " to clipboard!");
    }

    @FXML
    private void handleDownloadStatement() {
        FileChooser chooser = new FileChooser();
        if (selectedAccount == null) return;
        chooser.setTitle("Xuất lịch sử tài khoản");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName("NovaBank_" + selectedAccount.getAccountNumber() + ".csv");

        File file = chooser.showSaveDialog(btnToggleLock.getScene().getWindow());
        if (file != null) {
            try (var fw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                fw.write("ID,ThoiGian,TuTaiKhoan,DenTaiKhoan,SoTienVND,PhiVND,NoiDung\n");
                for (Transaction tx : ctx.getTransactions()) {
                    if (!selectedAccount.getAccountNumber().equals(tx.getFromAccountNumber())
                            && !selectedAccount.getAccountNumber().equals(tx.getToAccountNumber())) continue;
                    fw.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%.0f,%.0f,\"%s\"%n",
                            tx.getId(), tx.getTimestamp(), tx.getFromAccountNumber(), tx.getToAccountNumber(),
                            tx.getAmount(), tx.getFee(), tx.getDescription().replace("\"", "\"\"")));
                }
                ToastNotification.showSuccess("Đã xuất lịch sử tài khoản.");
            } catch (IOException e) {
                ToastNotification.showError("Failed to save statement: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSignOut() {
        btnToggleLock.getScene().getWindow().hide();
    }

    @FXML
    private void handleNavOverview() {
        navigator.accept("dashboard");
    }

    @FXML
    private void handleNavTransfer() {
        navigator.accept("transfer");
    }

    @FXML
    private void handleNavTransactions() {
        navigator.accept("history");
    }
}

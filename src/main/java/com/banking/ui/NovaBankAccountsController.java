package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import com.banking.service.AuthService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller cho giao diện Quản lý Tài khoản & Hạn mức (NovaBank Accounts & Limits).
 * Tích hợp Design Patterns:
 * - State Pattern: Chuyển đổi trạng thái ActiveState / LockedState ngay tức thì từ thẻ an ninh.
 * - Strategy Pattern: Trích xuất và hiển thị biểu phí tính tự động (StandardFee, TieredFee, PremiumFee).
 * - Observer Pattern: Tự động đồng bộ số dư và lịch sử giao dịch khi dữ liệu hệ thống thay đổi.
 */
public class NovaBankAccountsController implements Initializable, NovaBankNavigable {

    // Top Account Cards
    @FXML private VBox cardStandard;
    @FXML private ComboBox<Account> cbAllAccounts;
    @FXML private Label lblCard1Balance;
    @FXML private Label lblCard1Masked;
    @FXML private Label lblCard1Selected;

    @FXML private VBox cardSavings;
    @FXML private Label lblCard2Balance;
    @FXML private Label lblCard2Masked;

    @FXML private VBox cardPremium;
    @FXML private Label lblCard3Status;
    @FXML private Label lblCard3Balance;
    @FXML private Label lblCard3Masked;

    // Detail Panel
    @FXML private Label lblDetailAccountTitle;
    @FXML private Label lblDetailAccountDesc;
    @FXML private Label lblDetailStatusBadge;
    @FXML private Label lblDetailHolder;
    @FXML private Label lblDetailNumber;
    @FXML private Label lblDetailFeeStrategy;
    @FXML private Label lblDetailDailyLimit;

    // Mini Stats
    @FXML private Label lblStatCurrentBalance;
    @FXML private Label lblStatAvailableSpend;

    // Security & State Pattern Card
    @FXML private Label lblSecurityLockTitle;
    @FXML private Label lblSecurityLockDesc;
    @FXML private Button btnToggleLock;

    // Recent Transactions Table for selected account
    @FXML private Label lblTxCountForAccount;
    @FXML private TableView<Transaction> tableRecentAccountTx;
    @FXML private TableColumn<Transaction, Transaction> colRecentDate;
    @FXML private TableColumn<Transaction, Transaction> colRecentType;
    @FXML private TableColumn<Transaction, Transaction> colRecentDesc;
    @FXML private TableColumn<Transaction, Transaction> colRecentAmount;
    @FXML private TableColumn<Transaction, String> colRecentStatus;

    // User Profile in Header
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileRole;
    @FXML private Label lblProfileAvatar;

    private final UIContext ctx = UIContext.getInstance();
    private Account selectedAccount;

    private Account standardAccount;
    private Account savingsAccount;
    private Account premiumAccount;
    private AuthService.Role role = AuthService.Role.VIEWER;
    private Consumer<String> navigator = key -> { };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    @Override
    public void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator;
    }

    public void setRole(AuthService.Role role) {
        this.role = role;
        btnToggleLock.setVisible(role == AuthService.Role.ADMIN);
        btnToggleLock.setManaged(role == AuthService.Role.ADMIN);
        updateDetailsPanel();
    }

    public void setUser(AuthService.User user) {
        if (user == null) return;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupAccountComboBox();
        setupRecentTableColumns();
        bindAccountsFromDatabase();

        // ── OBSERVER PATTERN HOOK ──────────────────────────────────────────
        ctx.addDataChangeListener(() -> Platform.runLater(this::bindAccountsFromDatabase));
    }

    private void setupAccountComboBox() {
        cbAllAccounts.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                setText(empty || account == null ? "Chọn tài khoản..." : account.getAccountNumber() + " — " + account.getOwnerName() + " (" + account.getType() + ")");
            }
        });
        cbAllAccounts.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                setText(empty || account == null ? null : account.getAccountNumber() + " — " + account.getOwnerName() + " (" + account.getType() + ")");
            }
        });
        cbAllAccounts.valueProperty().addListener((obs, old, current) -> selectAccount(current));
    }

    private void setupRecentTableColumns() {
        if (tableRecentAccountTx == null) return;

        colRecentDate.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colRecentDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Label l1 = new Label(item.getTimestamp().format(DATE_FMT));
                    l1.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 11.5px;");
                    Label l2 = new Label(item.getTimestamp().format(TIME_FMT));
                    l2.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px;");
                    box.getChildren().addAll(l1, l2);
                    setGraphic(box);
                }
            }
        });

        colRecentType.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colRecentType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String type = typeOf(item);
                    Label lbl = new Label(type);
                    if ("Nạp tiền".equals(type)) {
                        lbl.setStyle("-fx-font-weight: 700; -fx-font-size: 11.5px; -fx-text-fill: #059669;");
                    } else if ("Rút tiền".equals(type)) {
                        lbl.setStyle("-fx-font-weight: 700; -fx-font-size: 11.5px; -fx-text-fill: #DC2626;");
                    } else if ("Hoàn tác".equals(type)) {
                        lbl.setStyle("-fx-font-weight: 700; -fx-font-size: 11.5px; -fx-text-fill: #7C3AED;");
                    } else {
                        lbl.setStyle("-fx-font-weight: 700; -fx-font-size: 11.5px; -fx-text-fill: #2563EB;");
                    }
                    setGraphic(lbl);
                }
            }
        });

        colRecentDesc.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colRecentDesc.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_LEFT);
                    String desc = item.getDescription() != null && !item.getDescription().isEmpty()
                            ? item.getDescription()
                            : "Giao dịch hệ thống";
                    Label l1 = new Label(desc);
                    l1.setStyle("-fx-font-weight: 600; -fx-text-fill: #0F172A; -fx-font-size: 11.5px;");
                    String route = "Từ " + item.getFromAccountNumber() + " ➔ Đến " + item.getToAccountNumber();
                    Label l2 = new Label(route);
                    l2.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px;");
                    box.getChildren().addAll(l1, l2);
                    setGraphic(box);
                }
            }
        });

        colRecentAmount.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colRecentAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String accNum = selectedAccount != null ? selectedAccount.getAccountNumber() : "";
                    boolean isIncoming = accNum.equals(item.getToAccountNumber());
                    String prefix = isIncoming ? "+" : "-";
                    Label lbl = new Label(prefix + UiUtils.formatVnd(item.getAmount()));
                    if (isIncoming) {
                        lbl.setStyle("-fx-font-weight: 800; -fx-text-fill: #059669; -fx-font-size: 12px;");
                    } else {
                        lbl.setStyle("-fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-size: 12px;");
                    }
                    StackPane p = new StackPane(lbl);
                    p.setAlignment(Pos.CENTER_RIGHT);
                    setGraphic(p);
                }
            }
        });

        colRecentStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRelatedTransactionId() != null ? "Hoàn tác" : "Thành công"));
        colRecentStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label();
                    if ("Hoàn tác".equals(item)) {
                        badge.setText("↶ Hoàn tác");
                        badge.getStyleClass().add("badge-status-undone");
                    } else {
                        badge.setText("✓ Thành công");
                        badge.getStyleClass().add("badge-status-success");
                    }
                    StackPane p = new StackPane(badge);
                    p.setAlignment(Pos.CENTER);
                    setGraphic(p);
                }
            }
        });
    }

    private String typeOf(Transaction tx) {
        if (tx.getRelatedTransactionId() != null) return "Hoàn tác";
        if ("SYSTEM".equals(tx.getFromAccountNumber())) return "Nạp tiền";
        if ("CASH".equals(tx.getToAccountNumber())) return "Rút tiền";
        return "Chuyển khoản";
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
        if (lblCard1Selected != null) lblCard1Selected.setVisible(false);

        String typeDisplay = switch (account.getType()) {
            case STANDARD -> "Tiêu chuẩn (STANDARD)";
            case SAVINGS -> "Tiết kiệm (SAVINGS)";
            case PREMIUM -> "Đặc quyền (PREMIUM)";
        };
        lblDetailAccountTitle.setText("Chi tiết Tài khoản " + typeDisplay);
        lblDetailAccountDesc.setText("Số hiệu tài khoản: " + account.getAccountNumber());

        if (account == standardAccount) {
            cardStandard.getStyleClass().add("account-card-selected");
            if (lblCard1Selected != null) lblCard1Selected.setVisible(true);
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

        // Strategy Pattern Fee info
        if (lblDetailFeeStrategy != null && selectedAccount.getFeeStrategy() != null) {
            lblDetailFeeStrategy.setText(selectedAccount.getFeeStrategy().getName() + " (" + getFeeStrategyDescription(selectedAccount) + ")");
        }
        if (lblDetailDailyLimit != null) {
            lblDetailDailyLimit.setText(getDailyLimitDescription(selectedAccount));
        }

        double bal = selectedAccount.getBalance();
        lblStatCurrentBalance.setText(UiUtils.formatVnd(bal));
        lblStatAvailableSpend.setText(UiUtils.formatVnd(selectedAccount.getStatus() == AccountStatus.ACTIVE ? bal : 0));
        btnToggleLock.setDisable(selectedAccount.getStatus() != AccountStatus.ACTIVE
                && selectedAccount.getStatus() != AccountStatus.LOCKED);

        // ── STATE PATTERN: CẬP NHẬT TRẠNG THÁI VÀ THẺ AN NINH ───────────────
        boolean isLocked = selectedAccount.getStatus() == AccountStatus.LOCKED;
        if (isLocked) {
            lblDetailStatusBadge.setText("● Đã tạm khóa");
            lblDetailStatusBadge.getStyleClass().setAll("status-badge-locked");

            lblSecurityLockTitle.setText("Tài khoản đang bị TẠM KHÓA");
            lblSecurityLockDesc.setText("State Pattern (LockedState): Đang chặn toàn bộ giao dịch chuyển & rút tiền. Nhấp bên dưới để mở lại.");
            btnToggleLock.setText("🔓  Mở khóa tài khoản (ActiveState)");
            btnToggleLock.setStyle("-fx-text-fill: #15803D; -fx-font-weight: 800;");
        } else {
            lblDetailStatusBadge.setText("● Đang hoạt động");
            lblDetailStatusBadge.getStyleClass().setAll("status-badge-active");

            lblSecurityLockTitle.setText("An Toàn & Trạng Thái Thẻ");
            lblSecurityLockDesc.setText("Bảo vệ bởi State Pattern. Bạn có thể tạm khóa tài khoản để chặn mọi giao dịch khi có sự cố.");
            btnToggleLock.setText("🔒  Tạm khóa tài khoản (LockedState)");
            btnToggleLock.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: 800;");
        }
        if (role != AuthService.Role.ADMIN) {
            lblSecurityLockDesc.setText("Chỉ tài khoản Quản trị viên (Admin) mới có quyền khóa hoặc mở khóa tài khoản.");
        }

        updateRecentTransactions();
    }

    private void updateRecentTransactions() {
        if (selectedAccount == null || tableRecentAccountTx == null) return;
        String accNum = selectedAccount.getAccountNumber();
        List<Transaction> list = ctx.getTransactions().stream()
                .filter(t -> accNum.equals(t.getFromAccountNumber()) || accNum.equals(t.getToAccountNumber()))
                .limit(5)
                .toList();

        tableRecentAccountTx.setItems(FXCollections.observableArrayList(list));
        if (lblTxCountForAccount != null) {
            lblTxCountForAccount.setText(list.size() + " giao dịch gần nhất");
        }
    }

    private String getFeeStrategyDescription(Account a) {
        return switch (a.getType()) {
            case STANDARD -> "Phí cố định 0.1% mỗi giao dịch";
            case SAVINGS -> "Theo số tiền giao dịch: ≤1 triệu 0,1%; ≤10 triệu 0,05%; >10 triệu 0,02%";
            case PREMIUM -> "Miễn phí 100% mọi giao dịch";
        };
    }

    private String getDailyLimitDescription(Account a) {
        return switch (a.getType()) {
            case STANDARD -> "50,000,000 VND / ngày";
            case SAVINGS -> "100,000,000 VND / ngày";
            case PREMIUM -> "Không giới hạn hạn mức";
        };
    }

    private void refreshDisplay() {
        if (standardAccount != null) {
            lblCard1Balance.setText(UiUtils.formatVnd(standardAccount.getBalance()));
            if (lblCard1Masked != null) lblCard1Masked.setText("•••• " + maskAccount(standardAccount.getAccountNumber()));
        } else {
            lblCard1Balance.setText("Chưa có");
            if (lblCard1Masked != null) lblCard1Masked.setText("•••• ----");
        }

        if (savingsAccount != null) {
            lblCard2Balance.setText(UiUtils.formatVnd(savingsAccount.getBalance()));
            if (lblCard2Masked != null) lblCard2Masked.setText("•••• " + maskAccount(savingsAccount.getAccountNumber()));
        } else {
            lblCard2Balance.setText("Chưa có");
            if (lblCard2Masked != null) lblCard2Masked.setText("•••• ----");
        }

        if (premiumAccount != null) {
            lblCard3Balance.setText(UiUtils.formatVnd(premiumAccount.getBalance()));
            boolean isLocked = premiumAccount.getStatus() == AccountStatus.LOCKED;
            lblCard3Status.setText(isLocked ? "● Đã khóa" : "● Hoạt động");
            lblCard3Status.getStyleClass().setAll(isLocked ? "status-badge-locked" : "status-badge-active");
            if (lblCard3Masked != null) lblCard3Masked.setText("•••• " + maskAccount(premiumAccount.getAccountNumber()));
        } else {
            lblCard3Balance.setText("Chưa có");
            lblCard3Status.setText("—");
            if (lblCard3Masked != null) lblCard3Masked.setText("•••• ----");
        }
        updateDetailsPanel();
    }

    private String maskAccount(String acc) {
        if (acc == null || acc.length() <= 4) return acc != null ? acc : "0000";
        return acc.substring(acc.length() - 4);
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
                ToastNotification.showWarning("Đã chuyển tài khoản " + selectedAccount.getAccountNumber() + " sang LockedState (Tạm khóa).");
            } else {
                ctx.getAccountService().unlockAccount(selectedAccount.getAccountNumber());
                ToastNotification.showSuccess("Đã mở khóa tài khoản " + selectedAccount.getAccountNumber() + " sang ActiveState (Hoạt động).");
            }
            ctx.notifyDataChanged();
        } catch (RuntimeException e) {
            ToastNotification.showError(UiUtils.humanizeError(e));
        }
    }

    @FXML
    private void copyAccountNumber() {
        copyToClipboard(lblDetailNumber.getText(), "số tài khoản");
    }

    private void copyToClipboard(String text, String fieldName) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        ToastNotification.showInfo("Đã sao chép " + fieldName + " vào bộ nhớ tạm!");
    }

    @FXML
    private void handleDownloadStatement() {
        FileChooser chooser = new FileChooser();
        if (selectedAccount == null) return;
        chooser.setTitle("Xuất sao kê tài khoản");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tập tin CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName("NovaBank_SaoKe_" + selectedAccount.getAccountNumber() + ".csv");

        File file = chooser.showSaveDialog(btnToggleLock.getScene().getWindow());
        if (file != null) {
            try (var fw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                fw.write("MaGD,ThoiGian,TuTaiKhoan,DenTaiKhoan,SoTienVND,PhiVND,NoiDung\n");
                for (Transaction tx : ctx.getTransactions()) {
                    if (!selectedAccount.getAccountNumber().equals(tx.getFromAccountNumber())
                            && !selectedAccount.getAccountNumber().equals(tx.getToAccountNumber())) continue;
                    fw.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%.0f,%.0f,\"%s\"%n",
                            tx.getId(), tx.getTimestamp(), tx.getFromAccountNumber(), tx.getToAccountNumber(),
                            tx.getAmount(), tx.getFee(), tx.getDescription().replace("\"", "\"\"")));
                }
                ToastNotification.showSuccess("Đã xuất sao kê tài khoản thành công!");
            } catch (IOException e) {
                ToastNotification.showError("Không thể lưu file sao kê: " + e.getMessage());
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

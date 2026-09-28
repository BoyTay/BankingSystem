package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.service.AuthService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller cho giao diện NovaBank Transaction History (Figma Table & KPIs).
 * Tích hợp Design Patterns:
 * - Command Pattern: TransactionHistory quản lý stack các Command, cho phép hoàn tác (Undo) trực tiếp qua BankingFacade.
 * - Observer Pattern: Tự động cập nhật bảng lịch sử và 3 thẻ KPI ngay khi có giao dịch mới.
 */
public class NovaBankHistoryController implements Initializable, NovaBankNavigable {

    // 3 KPI Cards
    @FXML private Label lblMoneyIn;
    @FXML private Label lblMoneyOut;
    @FXML private Label lblPendingAmount;
    @FXML private Label lblPendingCount;

    // Search and Filters
    @FXML private TextField txtSearch;
    @FXML private Label lblTransactionCount;
    @FXML private ComboBox<String> cbDateFilter;
    @FXML private ComboBox<String> cbTypeFilter;
    @FXML private ComboBox<String> cbAmountFilter;
    @FXML private ComboBox<String> cbAccountFilter;

    // Table & Columns
    @FXML private TableView<Transaction> tableTransactions;
    @FXML private TableColumn<Transaction, Transaction> colDate;
    @FXML private TableColumn<Transaction, Transaction> colTypeAccount;
    @FXML private TableColumn<Transaction, Transaction> colDescription;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, Transaction> colAmount;

    @FXML private Button btnUndo;
    @FXML private Button btnExport;

    private final UIContext ctx = UIContext.getInstance();
    private FilteredList<Transaction> filteredTransactions;
    private final ObservableList<Transaction> masterTransactions = FXCollections.observableArrayList();
    private AuthService.Role role = AuthService.Role.VIEWER;
    private Consumer<String> navigator = key -> { };

    @Override
    public void setNavigator(Consumer<String> navigator) { this.navigator = navigator; }

    public void setRole(AuthService.Role role) {
        this.role = role;
        btnUndo.setVisible(role != AuthService.Role.VIEWER);
        btnUndo.setManaged(role != AuthService.Role.VIEWER);
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilterCombos();
        setupTableColumns();
        loadTransactionData();

        // ── OBSERVER PATTERN HOOK ──────────────────────────────────────────
        ctx.addDataChangeListener(() -> Platform.runLater(this::loadTransactionData));
    }

    private void setupFilterCombos() {
        cbDateFilter.setItems(FXCollections.observableArrayList("Date: All time", "Today", "Last 7 days", "Last 30 days"));
        cbDateFilter.setValue("Date: All time");

        cbTypeFilter.setItems(FXCollections.observableArrayList("Type: All types", "Transfers", "Deposits", "Withdrawals"));
        cbTypeFilter.setValue("Type: All types");

        cbAmountFilter.setItems(FXCollections.observableArrayList("Amount: Any amount", "< 1 triệu VND", "1–10 triệu VND", "> 10 triệu VND"));
        cbAmountFilter.setValue("Amount: Any amount");

        cbAccountFilter.setItems(FXCollections.observableArrayList("Account: All accounts"));
        cbAccountFilter.setValue("Account: All accounts");

        // Gắn listener lọc
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        cbDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        cbTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        cbAmountFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        cbAccountFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupTableColumns() {
        // Cột 1: DATE (2 dòng: Ngày & Giờ)
        colDate.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_LEFT);
                    String dateStr = item.getTimestamp().format(DATE_FMT);
                    String timeStr = item.getTimestamp().format(TIME_FMT);
                    Label l1 = new Label(dateStr);
                    l1.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 12px;");
                    Label l2 = new Label(timeStr);
                    l2.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
                    box.getChildren().addAll(l1, l2);
                    setGraphic(box);
                }
            }
        });

        // Cột 2: TYPE & ACCOUNT (2 dòng: Phân loại & Số tài khoản)
        colTypeAccount.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colTypeAccount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_LEFT);
                    String type = typeOf(item);
                    String acc = item.getFromAccountNumber() + " → " + item.getToAccountNumber();

                    Label l1 = new Label(type);
                    l1.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 12px;");
                    Label l2 = new Label(acc);
                    l2.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
                    box.getChildren().addAll(l1, l2);
                    setGraphic(box);
                }
            }
        });

        // Cột 3: DESCRIPTION (2 dòng: Nội dung & Danh mục)
        colDescription.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colDescription.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Label l1 = new Label(item.getDescription());
                    l1.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 12px;");
                    Label l2 = new Label(item.getRelatedTransactionId() != null ? "Reversal / Undo" : "General");
                    l2.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
                    box.getChildren().addAll(l1, l2);
                    setGraphic(box);
                }
            }
        });

        // Cột 4: STATUS (Badge màu)
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty("Success"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label("● Success");
                    badge.getStyleClass().add("badge-status-success");
                    setGraphic(badge);
                }
            }
        });

        // Cột 5: AMOUNT (+ / - màu sắc phân biệt)
        colAmount.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    boolean isIncome = "Nạp tiền".equals(typeOf(item));
                    Label lbl = new Label((isIncome ? "+" : "") + UiUtils.formatVnd(item.getAmount()));
                    lbl.setStyle(isIncome
                            ? "-fx-font-weight: 800; -fx-text-fill: #00A862; -fx-font-size: 13px;"
                            : "-fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-size: 13px;");
                    setGraphic(lbl);
                }
            }
        });

        filteredTransactions = new FilteredList<>(masterTransactions, p -> true);
        tableTransactions.setItems(filteredTransactions);
    }

    private void loadTransactionData() {
        List<Transaction> list = ctx.getTransactions();
        masterTransactions.setAll(list);

        // Cập nhật tài khoản trong Combobox lọc
        String currentAccFilter = cbAccountFilter.getValue();
        cbAccountFilter.getItems().clear();
        cbAccountFilter.getItems().add("Account: All accounts");
        for (Account a : ctx.getAccounts()) {
            cbAccountFilter.getItems().add(a.getAccountNumber() + " (" + a.getOwnerName() + ")");
        }
        if (currentAccFilter != null && cbAccountFilter.getItems().contains(currentAccFilter)) {
            cbAccountFilter.setValue(currentAccFilter);
        } else {
            cbAccountFilter.setValue("Account: All accounts");
        }

        // Chỉ tính số liệu có trong sổ giao dịch.
        double moneyIn = 0;
        double moneyOut = 0;
        for (Transaction tx : list) {
            if ("SYSTEM".equals(tx.getFromAccountNumber())) {
                moneyIn += tx.getAmount();
            } else if ("CASH".equals(tx.getToAccountNumber())) {
                moneyOut += tx.getAmount() + tx.getFee();
            }
        }
        lblMoneyIn.setText(UiUtils.formatVnd(moneyIn));
        lblMoneyOut.setText(UiUtils.formatVnd(moneyOut));
        lblPendingAmount.setText("0 VND");
        lblPendingCount.setText("Không có giao dịch chờ");

        // Cập nhật trạng thái nút Undo (Command Pattern)
        int undoSize = ctx.getTxHistory().size();
        btnUndo.setDisable(undoSize == 0 || role == AuthService.Role.VIEWER);
        btnUndo.setText("↶ Undo (" + undoSize + " ready)");

        applyFilters();
    }

    private void applyFilters() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        String accFilter = cbAccountFilter.getValue();
        String dateFilter = cbDateFilter.getValue();
        String typeFilter = cbTypeFilter.getValue();
        String amountFilter = cbAmountFilter.getValue();

        filteredTransactions.setPredicate(tx -> {
            boolean matchesSearch = true;
            if (!keyword.isEmpty()) {
                matchesSearch = (tx.getDescription() != null && tx.getDescription().toLowerCase().contains(keyword))
                        || (tx.getFromAccountNumber() != null && tx.getFromAccountNumber().toLowerCase().contains(keyword))
                        || (tx.getToAccountNumber() != null && tx.getToAccountNumber().toLowerCase().contains(keyword))
                        || String.valueOf(tx.getAmount()).contains(keyword);
            }

            boolean matchesAccount = true;
            if (accFilter != null && !accFilter.equals("Account: All accounts")) {
                String selectedAcc = accFilter.split(" ")[0];
                matchesAccount = selectedAcc.equals(tx.getFromAccountNumber()) || selectedAcc.equals(tx.getToAccountNumber());
            }

            LocalDate date = tx.getTimestamp().toLocalDate();
            boolean matchesDate = switch (dateFilter == null ? "" : dateFilter) {
                case "Today" -> date.equals(LocalDate.now());
                case "Last 7 days" -> !date.isBefore(LocalDate.now().minusDays(6));
                case "Last 30 days" -> !date.isBefore(LocalDate.now().minusDays(29));
                default -> true;
            };
            boolean matchesType = switch (typeFilter == null ? "" : typeFilter) {
                case "Transfers" -> "Chuyển khoản".equals(typeOf(tx)) || "Hoàn tác".equals(typeOf(tx));
                case "Deposits" -> "Nạp tiền".equals(typeOf(tx));
                case "Withdrawals" -> "Rút tiền".equals(typeOf(tx));
                default -> true;
            };
            boolean matchesAmount = switch (amountFilter == null ? "" : amountFilter) {
                case "< 1 triệu VND" -> tx.getAmount() < 1_000_000;
                case "1–10 triệu VND" -> tx.getAmount() >= 1_000_000 && tx.getAmount() <= 10_000_000;
                case "> 10 triệu VND" -> tx.getAmount() > 10_000_000;
                default -> true;
            };
            return matchesSearch && matchesAccount && matchesDate && matchesType && matchesAmount;
        });

        int total = filteredTransactions.size();
        lblTransactionCount.setText(total + " transactions");
    }

    @FXML
    private void handleClearFilters() {
        txtSearch.clear();
        cbDateFilter.setValue("Date: All time");
        cbTypeFilter.setValue("Type: All types");
        cbAmountFilter.setValue("Amount: Any amount");
        cbAccountFilter.setValue("Account: All accounts");
        applyFilters();
    }

    @FXML
    private void handleUndo() {
        if (role == AuthService.Role.VIEWER) return;
        // ── COMMAND PATTERN: HOÀN TÁC GIAO DỊCH GẦN NHẤT ─────────────
        if (ctx.getTxHistory().isEmpty()) {
            ToastNotification.showWarning("Không còn giao dịch chuyển khoản nào để hoàn tác.");
            return;
        }

        try {
            Transaction undone = ctx.getFacade().undoLastTransfer();
            ctx.logCustomEvent("Command Pattern", "Undone transaction: " + undone.getId());
            ctx.notifyDataChanged();

            ToastNotification.showSuccess("Hoàn tác thành công giao dịch " + undone.getRelatedTransactionId() + "!");
        } catch (Exception ex) {
            String friendly = UiUtils.humanizeError(ex);
            ToastNotification.showError(friendly);
        }
    }

    @FXML
    private void handleExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Transactions to CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        chooser.setInitialFileName("NovaBank_Transactions_" + System.currentTimeMillis() + ".csv");

        File file = chooser.showSaveDialog(btnExport.getScene().getWindow());
        if (file != null) {
            try (var writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                writer.write("TransactionID,Timestamp,FromAccount,ToAccount,Amount,Fee,Description\n");
                for (Transaction tx : filteredTransactions) {
                    writer.write(String.format(Locale.US, "\"%s\",\"%s\",\"%s\",\"%s\",%.0f,%.0f,\"%s\"%n",
                            tx.getId(), tx.getTimestamp(),
                            tx.getFromAccountNumber() != null ? tx.getFromAccountNumber() : "-",
                            tx.getToAccountNumber() != null ? tx.getToAccountNumber() : "-",
                            tx.getAmount(), tx.getFee(),
                            tx.getDescription() != null ? tx.getDescription().replace("\"", "\"\"") : ""));
                }
                ToastNotification.showSuccess("Exported " + filteredTransactions.size() + " transactions to CSV!");
            } catch (IOException e) {
                ToastNotification.showError("Failed to export CSV: " + e.getMessage());
            }
        }
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
    private void handleNavAccounts() {
        navigator.accept("accounts");
    }

    private static String typeOf(Transaction tx) {
        if (tx.getRelatedTransactionId() != null) return "Hoàn tác";
        if ("SYSTEM".equals(tx.getFromAccountNumber())) return "Nạp tiền";
        if ("CASH".equals(tx.getToAccountNumber())) return "Rút tiền";
        return "Chuyển khoản";
    }
}

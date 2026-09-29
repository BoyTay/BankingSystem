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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller cho giao diện NovaBank Transaction History (2-Column Split View & Live Receipt Inspector).
 * Tích hợp Design Patterns:
 * - Command Pattern: TransactionHistory quản lý stack các Command, cho phép hoàn tác (Undo) trực tiếp qua BankingFacade.
 * - Observer Pattern: Tự động cập nhật bảng lịch sử, tỷ trọng thu chi và các thẻ KPI ngay khi có giao dịch mới.
 */
public class NovaBankHistoryController implements Initializable, NovaBankNavigable {

    // 3 High-Depth KPI Cards
    @FXML private Label lblMoneyIn;
    @FXML private Label lblMoneyOut;
    @FXML private Label lblPendingAmount;
    @FXML private Label lblPendingCount;

    // Search and Quick Filter Chips
    @FXML private TextField txtSearch;
    @FXML private Label lblTransactionCount;
    @FXML private Button btnChipAll;
    @FXML private Button btnChipIn;
    @FXML private Button btnChipOut;
    @FXML private Button btnChipTransfer;
    @FXML private Button btnChipUndo;

    // Advanced Filter Combos
    @FXML private ComboBox<String> cbDateFilter;
    @FXML private ComboBox<String> cbTypeFilter;
    @FXML private ComboBox<String> cbAmountFilter;
    @FXML private ComboBox<String> cbAccountFilter;

    // Table & Columns (Left Column)
    @FXML private TableView<Transaction> tableTransactions;
    @FXML private TableColumn<Transaction, Transaction> colDate;
    @FXML private TableColumn<Transaction, Transaction> colTypeAccount;
    @FXML private TableColumn<Transaction, Transaction> colDescription;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, Transaction> colAmount;

    // Pagination Controls
    @FXML private Label lblPaginationInfo;
    @FXML private Button btnPagePrev;
    @FXML private Button btnPageNext;
    @FXML private HBox boxPageNumbers;

    // Right Column: Widget 1 - Cashflow Ratio
    @FXML private Region barInflowRatio;
    @FXML private Label lblInflowPercent;
    @FXML private Label lblOutflowPercent;
    @FXML private Label lblNetCashflow;

    // Right Column: Widget 2 - Live Receipt Inspector
    @FXML private Label lblLiveStatusBadge;
    @FXML private Label lblLiveAmount;
    @FXML private Label lblLiveTxId;
    @FXML private Label lblLiveTime;
    @FXML private Label lblLiveType;
    @FXML private Label lblLiveFrom;
    @FXML private Label lblLiveTo;
    @FXML private Label lblLiveFee;
    @FXML private Label lblLiveDesc;
    @FXML private HBox rowLiveRelated;
    @FXML private Label lblLiveRelatedId;
    @FXML private Button btnLiveUndo;

    // Header Controls
    @FXML private Button btnUndo;
    @FXML private Button btnExport;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileRole;
    @FXML private Label lblProfileAvatar;

    private final UIContext ctx = UIContext.getInstance();
    private FilteredList<Transaction> filteredTransactions;
    private final ObservableList<Transaction> masterTransactions = FXCollections.observableArrayList();
    private final ObservableList<Transaction> pagedTransactions = FXCollections.observableArrayList();

    private AuthService.Role role = AuthService.Role.VIEWER;
    private Consumer<String> navigator = key -> { };

    private String activeQuickFilter = "ALL";
    private Transaction currentSelectedTx;

    private static final int ITEMS_PER_PAGE = 8;
    private int currentPage = 1;
    private int totalPages = 1;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
    private static final DateTimeFormatter FULL_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.US);

    @Override
    public void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator;
    }

    public void setRole(AuthService.Role role) {
        this.role = role;
        btnUndo.setVisible(role != AuthService.Role.VIEWER);
        btnUndo.setManaged(role != AuthService.Role.VIEWER);
        btnLiveUndo.setVisible(role != AuthService.Role.VIEWER);
        btnLiveUndo.setManaged(role != AuthService.Role.VIEWER);
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
        setupFilterCombos();
        setupTableColumns();
        setupRowSelectionHandler();
        loadTransactionData();

        // ── OBSERVER PATTERN HOOK ──────────────────────────────────────────
        ctx.addDataChangeListener(() -> Platform.runLater(this::loadTransactionData));
    }

    private void setupFilterCombos() {
        cbDateFilter.setItems(FXCollections.observableArrayList("Thời gian: Tất cả", "Hôm nay", "7 ngày qua", "30 ngày qua"));
        cbDateFilter.setValue("Thời gian: Tất cả");

        cbTypeFilter.setItems(FXCollections.observableArrayList("Phân loại: Tất cả", "Chuyển khoản", "Nạp tiền", "Rút tiền"));
        cbTypeFilter.setValue("Phân loại: Tất cả");

        cbAmountFilter.setItems(FXCollections.observableArrayList("Số tiền: Bất kỳ", "< 1 triệu VND", "1–10 triệu VND", "> 10 triệu VND"));
        cbAmountFilter.setValue("Số tiền: Bất kỳ");

        cbAccountFilter.setItems(FXCollections.observableArrayList("Tài khoản: Tất cả"));
        cbAccountFilter.setValue("Tài khoản: Tất cả");

        // Gắn listener lọc tự động
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            applyFilters();
        });
        cbDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            applyFilters();
        });
        cbTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            applyFilters();
        });
        cbAmountFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            applyFilters();
        });
        cbAccountFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            applyFilters();
        });
    }

    private void setupTableColumns() {
        // Cột 1: THỜI GIAN (Ngày & Giờ rõ nét)
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
                    Label l1 = new Label(item.getTimestamp().format(DATE_FMT));
                    l1.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 12px;");
                    Label l2 = new Label(item.getTimestamp().format(TIME_FMT));
                    l2.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10.5px;");
                    box.getChildren().addAll(l1, l2);
                    setGraphic(box);
                }
            }
        });

        // Cột 2: PHÂN LOẠI & TÀI KHOẢN (Circular Icon Avatar + Flow Badge)
        colTypeAccount.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colTypeAccount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox root = new HBox(10);
                    root.setAlignment(Pos.CENTER_LEFT);

                    StackPane avatar = new StackPane();
                    avatar.getStyleClass().add("tx-avatar-base");
                    SVGPath svg = new SVGPath();
                    svg.setFill(Color.TRANSPARENT);
                    svg.setStrokeWidth(2.0);

                    String type = typeOf(item);
                    String flowText;
                    if ("Nạp tiền".equals(type)) {
                        avatar.getStyleClass().add("tx-avatar-deposit");
                        svg.setContent("M19 14l-7 7m0 0l-7-7m7 7V3");
                        svg.setStroke(Color.web("#059669"));
                        flowText = "Hệ thống ➔ " + item.getToAccountNumber();
                    } else if ("Rút tiền".equals(type)) {
                        avatar.getStyleClass().add("tx-avatar-withdraw");
                        svg.setContent("M5 10l7-7m0 0l7 7m-7-7v18");
                        svg.setStroke(Color.web("#DC2626"));
                        flowText = item.getFromAccountNumber() + " ➔ Tiền mặt";
                    } else if ("Hoàn tác".equals(type)) {
                        avatar.getStyleClass().add("tx-avatar-undo");
                        svg.setContent("M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6");
                        svg.setStroke(Color.web("#7C3AED"));
                        flowText = "Hoàn tác: " + item.getFromAccountNumber() + " ➔ " + item.getToAccountNumber();
                    } else {
                        avatar.getStyleClass().add("tx-avatar-transfer");
                        svg.setContent("M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4");
                        svg.setStroke(Color.web("#2563EB"));
                        flowText = item.getFromAccountNumber() + " ➔ " + item.getToAccountNumber();
                    }
                    avatar.getChildren().add(svg);

                    VBox info = new VBox(2);
                    info.setAlignment(Pos.CENTER_LEFT);
                    Label lblType = new Label(type);
                    lblType.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 12px;");

                    Label lblFlow = new Label(flowText);
                    lblFlow.getStyleClass().add("tx-flow-badge");

                    info.getChildren().addAll(lblType, lblFlow);
                    root.getChildren().addAll(avatar, info);
                    setGraphic(root);
                }
            }
        });

        // Cột 3: NỘI DUNG GIAO DỊCH (Mô tả & TxID Hash)
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
                    String desc = item.getDescription() != null && !item.getDescription().isEmpty()
                            ? item.getDescription()
                            : "Giao dịch hệ thống";
                    Label l1 = new Label(desc);
                    l1.setStyle("-fx-font-weight: 600; -fx-text-fill: #0F172A; -fx-font-size: 12px;");

                    HBox sub = new HBox(6);
                    sub.setAlignment(Pos.CENTER_LEFT);
                    String hash = item.getId().length() > 9 ? item.getId().substring(0, 9) + "..." : item.getId();
                    Label lblHash = new Label("#" + hash);
                    lblHash.getStyleClass().add("tx-hash-badge");

                    Label l2 = new Label(item.getRelatedTransactionId() != null ? "Lệnh Hoàn tác" : "Thông thường");
                    l2.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10.5px;");

                    sub.getChildren().addAll(lblHash, l2);
                    box.getChildren().addAll(l1, sub);
                    setGraphic(box);
                }
            }
        });

        // Cột 4: TRẠNG THÁI (Pill Badge)
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRelatedTransactionId() != null ? "Hoàn tác" : "Thành công"));
        colStatus.setCellFactory(col -> new TableCell<>() {
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
                    StackPane container = new StackPane(badge);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                }
            }
        });

        // Cột 5: SỐ TIỀN (+ / - Màu sắc Fintech)
        colAmount.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_RIGHT);

                    boolean isIncome = "Nạp tiền".equals(typeOf(item));
                    String prefix = isIncome ? "+" : "-";
                    Label lbl = new Label(prefix + UiUtils.formatVnd(item.getAmount()));
                    if (isIncome) {
                        lbl.setStyle("-fx-font-weight: 800; -fx-text-fill: #059669; -fx-font-size: 13px;");
                    } else {
                        lbl.setStyle("-fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-size: 13px;");
                    }

                    Label lblFee = new Label(item.getFee() > 0 ? "Phí: " + UiUtils.formatVnd(item.getFee()) : "Miễn phí");
                    lblFee.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");

                    box.getChildren().addAll(lbl, lblFee);
                    setGraphic(box);
                }
            }
        });

        filteredTransactions = new FilteredList<>(masterTransactions, p -> true);
        tableTransactions.setItems(pagedTransactions);
    }

    private void setupRowSelectionHandler() {
        tableTransactions.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateLiveReceipt(newVal);
            }
        });

        tableTransactions.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    updateLiveReceipt(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadTransactionData() {
        List<Transaction> list = ctx.getTransactions();
        masterTransactions.setAll(list);

        // Cập nhật danh sách tài khoản trong ComboBox lọc
        String currentAccFilter = cbAccountFilter.getValue();
        cbAccountFilter.getItems().clear();
        cbAccountFilter.getItems().add("Tài khoản: Tất cả");
        for (Account a : ctx.getAccounts()) {
            cbAccountFilter.getItems().add(a.getAccountNumber() + " (" + a.getOwnerName() + ")");
        }
        if (currentAccFilter != null && cbAccountFilter.getItems().contains(currentAccFilter)) {
            cbAccountFilter.setValue(currentAccFilter);
        } else {
            cbAccountFilter.setValue("Tài khoản: Tất cả");
        }

        // Tính toán tổng số liệu cho 3 thẻ KPI & Cashflow Widget
        double moneyIn = 0;
        double moneyOut = 0;
        for (Transaction tx : list) {
            if ("SYSTEM".equals(tx.getFromAccountNumber())) {
                moneyIn += tx.getAmount();
            } else if ("CASH".equals(tx.getToAccountNumber()) || "Chuyển khoản".equals(typeOf(tx))) {
                moneyOut += tx.getAmount() + tx.getFee();
            }
        }
        lblMoneyIn.setText(UiUtils.formatVnd(moneyIn));
        lblMoneyOut.setText(UiUtils.formatVnd(moneyOut));
        lblPendingAmount.setText("0 VND");
        lblPendingCount.setText("● 0 giao dịch chờ");

        updateCashflowRatio(moneyIn, moneyOut);

        // Cập nhật trạng thái nút Undo (Command Pattern)
        int undoSize = ctx.getTxHistory().size();
        btnUndo.setDisable(undoSize == 0 || role == AuthService.Role.VIEWER);
        btnUndo.setText(undoSize > 0 ? "↶ Hoàn tác (" + undoSize + " lệnh)" : "↶ Hoàn tác");

        applyFilters();
    }

    private void updateCashflowRatio(double moneyIn, double moneyOut) {
        double total = moneyIn + moneyOut;
        double inPercent = 100.0;
        double outPercent = 0.0;
        if (total > 0) {
            inPercent = (moneyIn / total) * 100.0;
            outPercent = (moneyOut / total) * 100.0;
        }
        lblInflowPercent.setText(String.format(Locale.US, "● Vào: %.1f%%", inPercent));
        lblOutflowPercent.setText(String.format(Locale.US, "● Ra: %.1f%%", outPercent));

        double trackWidth = 320.0;
        double barWidth = (inPercent / 100.0) * trackWidth;
        barInflowRatio.setPrefWidth(Math.max(12.0, Math.min(trackWidth, barWidth)));

        double net = moneyIn - moneyOut;
        String sign = net >= 0 ? "+" : "";
        lblNetCashflow.setText(sign + UiUtils.formatVnd(net));
        if (net >= 0) {
            lblNetCashflow.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #059669;");
        } else {
            lblNetCashflow.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #DC2626;");
        }
    }

    private void applyFilters() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        String accFilter = cbAccountFilter.getValue();
        String dateFilter = cbDateFilter.getValue();
        String typeFilter = cbTypeFilter.getValue();
        String amountFilter = cbAmountFilter.getValue();

        filteredTransactions.setPredicate(tx -> {
            // 1. Quick Filter Chip
            boolean matchesChip = switch (activeQuickFilter) {
                case "IN" -> "Nạp tiền".equals(typeOf(tx));
                case "OUT" -> "Rút tiền".equals(typeOf(tx)) || "Chuyển khoản".equals(typeOf(tx));
                case "TRANSFER" -> "Chuyển khoản".equals(typeOf(tx));
                case "UNDO" -> tx.getRelatedTransactionId() != null;
                default -> true;
            };
            if (!matchesChip) return false;

            // 2. Tìm kiếm tự do theo từ khóa
            boolean matchesSearch = true;
            if (!keyword.isEmpty()) {
                matchesSearch = (tx.getId() != null && tx.getId().toLowerCase().contains(keyword))
                        || (tx.getDescription() != null && tx.getDescription().toLowerCase().contains(keyword))
                        || (tx.getFromAccountNumber() != null && tx.getFromAccountNumber().toLowerCase().contains(keyword))
                        || (tx.getToAccountNumber() != null && tx.getToAccountNumber().toLowerCase().contains(keyword))
                        || String.valueOf(tx.getAmount()).contains(keyword);
            }
            if (!matchesSearch) return false;

            // 3. Lọc theo tài khoản
            if (accFilter != null && !accFilter.equals("Tài khoản: Tất cả") && !accFilter.equals("Account: All accounts")) {
                String selectedAcc = accFilter.split(" ")[0];
                if (!selectedAcc.equals(tx.getFromAccountNumber()) && !selectedAcc.equals(tx.getToAccountNumber())) {
                    return false;
                }
            }

            // 4. Lọc theo ngày
            LocalDate date = tx.getTimestamp().toLocalDate();
            boolean matchesDate = switch (dateFilter == null ? "" : dateFilter) {
                case "Hôm nay", "Today" -> date.equals(LocalDate.now());
                case "7 ngày qua", "Last 7 days" -> !date.isBefore(LocalDate.now().minusDays(6));
                case "30 ngày qua", "Last 30 days" -> !date.isBefore(LocalDate.now().minusDays(29));
                default -> true;
            };
            if (!matchesDate) return false;

            // 5. Lọc theo phân loại
            boolean matchesType = switch (typeFilter == null ? "" : typeFilter) {
                case "Chuyển khoản", "Transfers" -> "Chuyển khoản".equals(typeOf(tx)) || "Hoàn tác".equals(typeOf(tx));
                case "Nạp tiền", "Deposits" -> "Nạp tiền".equals(typeOf(tx));
                case "Rút tiền", "Withdrawals" -> "Rút tiền".equals(typeOf(tx));
                default -> true;
            };
            if (!matchesType) return false;

            // 6. Lọc theo số tiền
            return switch (amountFilter == null ? "" : amountFilter) {
                case "< 1 triệu VND" -> tx.getAmount() < 1_000_000;
                case "1–10 triệu VND" -> tx.getAmount() >= 1_000_000 && tx.getAmount() <= 10_000_000;
                case "> 10 triệu VND" -> tx.getAmount() > 10_000_000;
                default -> true;
            };
        });

        int total = filteredTransactions.size();
        lblTransactionCount.setText(total + " giao dịch");

        // Cập nhật phân trang
        totalPages = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;
        updatePage();
    }

    private void updatePage() {
        int total = filteredTransactions.size();
        if (total == 0) {
            pagedTransactions.clear();
            lblPaginationInfo.setText("Không có giao dịch phù hợp");
            clearLiveReceipt();
        } else {
            int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
            int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, total);
            pagedTransactions.setAll(filteredTransactions.subList(fromIndex, toIndex));
            lblPaginationInfo.setText(String.format("Hiển thị %d - %d trên %d giao dịch", fromIndex + 1, toIndex, total));

            // Tự động chọn dòng đầu tiên hoặc duy trì dòng đang chọn
            if (!pagedTransactions.isEmpty()) {
                int selIdx = pagedTransactions.indexOf(currentSelectedTx);
                if (selIdx >= 0) {
                    tableTransactions.getSelectionModel().select(selIdx);
                    updateLiveReceipt(currentSelectedTx);
                } else {
                    tableTransactions.getSelectionModel().select(0);
                    updateLiveReceipt(pagedTransactions.get(0));
                }
            }
        }

        // Cập nhật trạng thái nút Prev / Next
        btnPagePrev.setDisable(currentPage <= 1);
        btnPageNext.setDisable(currentPage >= totalPages);

        // Tạo danh sách nút số trang
        boxPageNumbers.getChildren().clear();
        for (int i = 1; i <= totalPages; i++) {
            final int pageIdx = i;
            Button btnPage = new Button(String.valueOf(i));
            btnPage.getStyleClass().add("btn-page-number");
            if (i == currentPage) {
                btnPage.getStyleClass().add("btn-page-number-active");
            }
            btnPage.setOnAction(e -> {
                currentPage = pageIdx;
                updatePage();
            });
            boxPageNumbers.getChildren().add(btnPage);
        }
    }

    @FXML
    private void handlePagePrev() {
        if (currentPage > 1) {
            currentPage--;
            updatePage();
        }
    }

    @FXML
    private void handlePageNext() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePage();
        }
    }

    // ── Quick Filter Chips ───────────────────────────────────────────────────
    @FXML
    private void handleChipFilterAll() {
        activeQuickFilter = "ALL";
        currentPage = 1;
        updateChipStyles(btnChipAll);
        applyFilters();
    }

    @FXML
    private void handleChipFilterIn() {
        activeQuickFilter = "IN";
        currentPage = 1;
        updateChipStyles(btnChipIn);
        applyFilters();
    }

    @FXML
    private void handleChipFilterOut() {
        activeQuickFilter = "OUT";
        currentPage = 1;
        updateChipStyles(btnChipOut);
        applyFilters();
    }

    @FXML
    private void handleChipFilterTransfer() {
        activeQuickFilter = "TRANSFER";
        currentPage = 1;
        updateChipStyles(btnChipTransfer);
        applyFilters();
    }

    @FXML
    private void handleChipFilterUndo() {
        activeQuickFilter = "UNDO";
        currentPage = 1;
        updateChipStyles(btnChipUndo);
        applyFilters();
    }

    private void updateChipStyles(Button activeBtn) {
        Button[] chips = {btnChipAll, btnChipIn, btnChipOut, btnChipTransfer, btnChipUndo};
        for (Button b : chips) {
            if (b != null) b.getStyleClass().remove("quick-filter-chip-active");
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("quick-filter-chip-active")) {
            activeBtn.getStyleClass().add("quick-filter-chip-active");
        }
    }

    @FXML
    private void handleClearFilters() {
        txtSearch.clear();
        cbDateFilter.setValue("Thời gian: Tất cả");
        cbTypeFilter.setValue("Phân loại: Tất cả");
        cbAmountFilter.setValue("Số tiền: Bất kỳ");
        cbAccountFilter.setValue("Tài khoản: Tất cả");
        activeQuickFilter = "ALL";
        currentPage = 1;
        updateChipStyles(btnChipAll);
        applyFilters();
    }

    // ── Right Column: Live Receipt Inspector ─────────────────────────────────
    private void updateLiveReceipt(Transaction tx) {
        if (tx == null) {
            clearLiveReceipt();
            return;
        }
        this.currentSelectedTx = tx;

        boolean isIncome = "Nạp tiền".equals(typeOf(tx));
        String prefix = isIncome ? "+" : "-";
        lblLiveAmount.setText(prefix + UiUtils.formatVnd(tx.getAmount()));
        if (isIncome) {
            lblLiveAmount.setStyle("-fx-font-size: 21px; -fx-font-weight: 800; -fx-text-fill: #059669;");
        } else {
            lblLiveAmount.setStyle("-fx-font-size: 21px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        }

        if (tx.getRelatedTransactionId() != null) {
            lblLiveStatusBadge.setText("● Đã hoàn tác");
            lblLiveStatusBadge.getStyleClass().setAll("badge-status-undone");
            rowLiveRelated.setVisible(true);
            rowLiveRelated.setManaged(true);
            lblLiveRelatedId.setText(tx.getRelatedTransactionId());
        } else {
            lblLiveStatusBadge.setText("● Thành công");
            lblLiveStatusBadge.getStyleClass().setAll("badge-status-success");
            rowLiveRelated.setVisible(false);
            rowLiveRelated.setManaged(false);
        }

        lblLiveTxId.setText(tx.getId());
        lblLiveTime.setText(tx.getTimestamp().format(FULL_TIME_FMT));
        lblLiveType.setText(typeOf(tx));
        lblLiveFrom.setText(tx.getFromAccountNumber() != null ? tx.getFromAccountNumber() : "Hệ thống");
        lblLiveTo.setText(tx.getToAccountNumber() != null ? tx.getToAccountNumber() : "Tiền mặt");
        lblLiveFee.setText(tx.getFee() > 0 ? UiUtils.formatVnd(tx.getFee()) : "0 VND (Miễn phí)");
        lblLiveDesc.setText(tx.getDescription() != null && !tx.getDescription().isEmpty()
                ? tx.getDescription()
                : "Giao dịch hệ thống");

        // Kiểm tra điều kiện hoàn tác của nút Live Undo
        boolean canUndo = role != AuthService.Role.VIEWER && !ctx.getTxHistory().isEmpty();
        btnLiveUndo.setDisable(!canUndo);
    }

    private void clearLiveReceipt() {
        this.currentSelectedTx = null;
        lblLiveAmount.setText("0 VND");
        lblLiveStatusBadge.setText("● Chờ chọn");
        lblLiveStatusBadge.getStyleClass().setAll("badge-status-pending");
        lblLiveTxId.setText("--");
        lblLiveTime.setText("--");
        lblLiveType.setText("--");
        lblLiveFrom.setText("--");
        lblLiveTo.setText("--");
        lblLiveFee.setText("0 VND");
        lblLiveDesc.setText("Chọn một giao dịch trên bảng để xem chi tiết biên lai.");
        rowLiveRelated.setVisible(false);
        rowLiveRelated.setManaged(false);
        btnLiveUndo.setDisable(true);
    }

    @FXML
    private void handleCopyTxId() {
        if (currentSelectedTx == null) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(currentSelectedTx.getId());
        clipboard.setContent(content);
        ToastNotification.showSuccess("Đã sao chép mã giao dịch: " + currentSelectedTx.getId());
    }

    @FXML
    private void handleUndoSelected() {
        handleUndo();
    }

    // ── Command Pattern: Hoàn tác giao dịch gần nhất ──────────────────────────
    @FXML
    private void handleUndo() {
        if (role == AuthService.Role.VIEWER) return;

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

    // ── Xuất file CSV ────────────────────────────────────────────────────────
    @FXML
    private void handleExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Xuất sao kê giao dịch (CSV)");
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
                ToastNotification.showSuccess("Đã xuất thành công " + filteredTransactions.size() + " bản ghi sao kê CSV!");
            } catch (IOException e) {
                ToastNotification.showError("Không thể xuất file CSV: " + e.getMessage());
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

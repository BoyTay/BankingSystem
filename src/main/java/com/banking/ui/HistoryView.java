package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Transaction;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Màn hình Lịch sử Giao Dịch & Hoàn Tác.
 * Minh họa Patterns:
 * - Command Pattern: TransactionHistory quản lý stack các Command, cho phép hoàn tác (undo) giao dịch chuyển khoản gần nhất.
 */
public class HistoryView extends VBox {

    private final UIContext ctx = UIContext.getInstance();
    private final TableView<Transaction> historyTable = new TableView<>();
    private final Label lblUndoCount = new Label();
    private final Button btnUndo = new Button("↶ Hoàn Tác Giao Dịch Gần Nhất (Undo Command)");
    private final ComboBox<String> cbAccountFilter = new ComboBox<>();
    private FilteredList<Transaction> filteredTransactions;
    private final boolean canUndo;

    public HistoryView(boolean canUndo) {
        this.canUndo = canUndo;
        setSpacing(24);
        getStyleClass().add("content-pane");

        buildHeader();

        VBox mainCard = buildTableCard();
        VBox commandExplainer = buildCommandPatternExplainerCard();

        getChildren().addAll(mainCard, commandExplainer);

        ctx.addDataChangeListener(this::refresh);
        refresh();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Lịch Sử Giao Dịch & Hoàn Tác");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Theo dõi biến động số dư và sử dụng Command Pattern để hoàn tác (Undo) giao dịch");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        HBox patternBadges = new HBox(8);
        patternBadges.setAlignment(Pos.CENTER_RIGHT);
        patternBadges.getChildren().addAll(
                UiUtils.createPatternBadge("Command (TransactionHistory)", "behavioral"),
                UiUtils.createPatternBadge("Command (TransferCommand.undo())", "behavioral")
        );

        HBox header = new HBox();
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, patternBadges);
        header.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(header);
    }

    private VBox buildTableCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        // Action Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label lblFilter = new Label("Lọc theo tài khoản:");
        lblFilter.getStyleClass().add("form-label");

        cbAccountFilter.getItems().add("Tất cả tài khoản");
        cbAccountFilter.setValue("Tất cả tài khoản");
        cbAccountFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblUndoCount.setStyle("-fx-font-size: 12px; -fx-text-fill: #0369A1; -fx-font-weight: bold; -fx-background-color: #E0F2FE; -fx-padding: 6 10; -fx-background-radius: 6;");

        btnUndo.getStyleClass().add("btn-danger");
        btnUndo.setOnAction(e -> handleUndo());

        toolbar.getChildren().addAll(lblFilter, cbAccountFilter, spacer, lblUndoCount, btnUndo);

        // Configure Table
        TableColumn<Transaction, String> colId = new TableColumn<>("Mã GD");
        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getId()));
        colId.setPrefWidth(90);

        TableColumn<Transaction, String> colDate = new TableColumn<>("Thời Gian");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTimestamp().toString().replace("T", " ").substring(0, 19)));
        colDate.setPrefWidth(150);

        TableColumn<Transaction, String> colFrom = new TableColumn<>("Từ Tài Khoản");
        colFrom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFromAccountNumber()));
        colFrom.setPrefWidth(120);

        TableColumn<Transaction, String> colTo = new TableColumn<>("Đến Tài Khoản");
        colTo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getToAccountNumber()));
        colTo.setPrefWidth(120);

        TableColumn<Transaction, String> colAmount = new TableColumn<>("Số Tiền");
        colAmount.setCellValueFactory(d -> new SimpleStringProperty(UiUtils.formatVnd(d.getValue().getAmount())));
        colAmount.setPrefWidth(140);

        TableColumn<Transaction, String> colFee = new TableColumn<>("Phí");
        colFee.setCellValueFactory(d -> new SimpleStringProperty(UiUtils.formatVnd(d.getValue().getFee())));
        colFee.setPrefWidth(110);

        TableColumn<Transaction, String> colDesc = new TableColumn<>("Nội Dung & Ghi Chú");
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colDesc.setPrefWidth(260);

        historyTable.getColumns().setAll(colId, colDate, colFrom, colTo, colAmount, colFee, colDesc);
        historyTable.setPrefHeight(320);
        historyTable.setPlaceholder(new Label("Không có giao dịch nào phù hợp với bộ lọc."));

        filteredTransactions = new FilteredList<>(ctx.getTransactions(), p -> true);
        historyTable.setItems(filteredTransactions);

        card.getChildren().addAll(toolbar, historyTable);
        return card;
    }

    private void applyFilter() {
        String selected = cbAccountFilter.getValue();
        if (selected == null || "Tất cả tài khoản".equals(selected)) {
            filteredTransactions.setPredicate(tx -> true);
        } else {
            filteredTransactions.setPredicate(tx ->
                    selected.equals(tx.getFromAccountNumber()) || selected.equals(tx.getToAccountNumber()));
        }
    }

    private void handleUndo() {
        if (!canUndo) return;
        if (ctx.getTxHistory().isEmpty()) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể hoàn tác", "Undo Stack rỗng",
                    "Hiện không còn giao dịch chuyển khoản nào trong lịch sử để hoàn tác.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận hoàn tác (Command Pattern)");
        confirm.setHeaderText("Bạn có chắc chắn muốn hoàn tác lệnh gần nhất?");
        confirm.setContentText("Lệnh chỉ hoàn tác nếu tài khoản nhận còn đủ tiền. Lịch sử sẽ ghi thêm giao dịch hoàn tác.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                Transaction reversal = ctx.getFacade().undoLastTransfer();
                ctx.logCustomEvent("Command Pattern", "Đã thực hiện Command.undo() — hoàn trả số dư tài khoản.");
                ctx.notifyDataChanged();
                UiUtils.showAlert(Alert.AlertType.INFORMATION, "Hoàn tác thành công", "Đã đảo ngược giao dịch",
                        "Đã tạo giao dịch hoàn tác " + reversal.getId() + " cho " + reversal.getRelatedTransactionId());
            } catch (IllegalStateException ex) {
                UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể hoàn tác", "Giao dịch chưa hoàn tác", ex.getMessage());
            }
        }
    }

    private VBox buildCommandPatternExplainerCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label title = new Label("Cơ chế hoạt động của Command Pattern trong hệ thống ngân hàng");
        title.getStyleClass().add("card-title");

        Label desc = new Label(
                "• Giao diện Command: Định nghĩa 2 phương thức trừu tượng execute() và undo().\n"
                        + "• Đối tượng TransferCommand: Lưu vết (fromAccount, toAccount, amount, fee). Khi gọi undo(), nó tự động trừ tiền bên nhận và hoàn tiền + hoàn phí cho bên gửi.\n"
                        + "• TransactionHistory (Invoker): Quản lý Deque<Command> theo cơ chế LIFO. Chỉ lưu lệnh thành công; undo chỉ xóa lệnh khi bên nhận còn đủ số dư."
        );
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-line-spacing: 4px;");

        card.getChildren().addAll(title, desc);
        return card;
    }

    public void refresh() {
        int undoCount = ctx.getTxHistory().size();
        lblUndoCount.setText("Lệnh chờ Undo: " + undoCount);
        btnUndo.setDisable(!canUndo || undoCount == 0);

        String currentFilter = cbAccountFilter.getValue();
        cbAccountFilter.getItems().clear();
        cbAccountFilter.getItems().add("Tất cả tài khoản");
        for (Account acc : ctx.getAccounts()) {
            cbAccountFilter.getItems().add(acc.getAccountNumber());
        }

        if (currentFilter != null && cbAccountFilter.getItems().contains(currentFilter)) {
            cbAccountFilter.setValue(currentFilter);
        } else {
            cbAccountFilter.setValue("Tất cả tài khoản");
        }

        applyFilter();
    }
}

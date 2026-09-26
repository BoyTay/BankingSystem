package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.pattern.creational.DatabaseManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

/**
 * Màn hình Dashboard — Tổng quan tài khoản và giao dịch gần đây.
 * Minh họa Patterns:
 * - Singleton (DatabaseManager instance info)
 * - Observer (Live updates and event feed)
 */
public class DashboardView extends VBox {

    private final UIContext ctx = UIContext.getInstance();
    private final Consumer<String> navigationHandler;
    private final HBox accountCardsContainer = new HBox(16);
    private final TableView<Transaction> txTable = new TableView<>();
    private final ListView<String> observerFeed = new ListView<>();

    public DashboardView(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
        setSpacing(24);
        getStyleClass().add("content-pane");

        buildHeader();
        buildAccountCardsSection();
        buildRecentTransactionsSection();
        buildObserverFeedSection();

        // Đăng ký cập nhật khi có thay đổi dữ liệu
        ctx.addDataChangeListener(this::refresh);
        refresh();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");

        String currentOwner = ctx.getAccounts().isEmpty() ? "Quý khách" : ctx.getAccounts().get(0).getOwnerName();
        Label subtitle = new Label("Xin chào, " + currentOwner + "!");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        HBox patternBadges = new HBox(8);
        patternBadges.setAlignment(Pos.CENTER_RIGHT);
        patternBadges.getChildren().addAll(
                UiUtils.createPatternBadge("Singleton (DatabaseManager @" + Integer.toHexString(DatabaseManager.getInstance().hashCode()) + ")", "creational"),
                UiUtils.createPatternBadge("Observer (Live Account Stream)", "behavioral")
        );

        HBox header = new HBox();
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, patternBadges);
        header.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(header);
    }

    private void buildAccountCardsSection() {
        VBox section = new VBox(12);

        Label secTitle = new Label("Danh sách tài khoản (Account Balances)");
        secTitle.getStyleClass().add("card-title");

        accountCardsContainer.setAlignment(Pos.CENTER_LEFT);
        accountCardsContainer.setPadding(new Insets(4, 4, 12, 4));

        ScrollPane scrollPane = new ScrollPane(accountCardsContainer);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Quick Actions panel
        HBox quickActions = new HBox(12);
        quickActions.setAlignment(Pos.CENTER_LEFT);

        Button btnTransfer = new Button("⇄ Chuyển khoản");
        btnTransfer.getStyleClass().addAll("btn-primary");
        btnTransfer.setOnAction(e -> navigationHandler.accept("transfer"));

        Button btnDeposit = new Button("📥 Nạp tiền");
        btnDeposit.getStyleClass().addAll("btn-secondary");
        btnDeposit.setOnAction(e -> navigationHandler.accept("deposit"));

        Button btnWithdraw = new Button("📤 Rút tiền");
        btnWithdraw.getStyleClass().addAll("btn-secondary");
        btnWithdraw.setOnAction(e -> navigationHandler.accept("deposit"));

        Button btnAddAccount = new Button("➕ Mở tài khoản");
        btnAddAccount.getStyleClass().addAll("btn-secondary");
        btnAddAccount.setOnAction(e -> navigationHandler.accept("accounts"));

        quickActions.getChildren().addAll(btnTransfer, btnDeposit, btnWithdraw, btnAddAccount);

        section.getChildren().addAll(secTitle, scrollPane, quickActions);
        getChildren().add(section);
    }

    private void buildRecentTransactionsSection() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        HBox cardHeader = new HBox();
        Label cardTitle = new Label("Giao dịch gần đây (Recent Transactions)");
        cardTitle.getStyleClass().add("card-title");

        Button btnViewAll = new Button("Xem tất cả / Hoàn tác →");
        btnViewAll.getStyleClass().add("btn-secondary");
        btnViewAll.setOnAction(e -> navigationHandler.accept("history"));

        HBox.setHgrow(cardTitle, Priority.ALWAYS);
        cardHeader.getChildren().addAll(cardTitle, btnViewAll);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        // Configure table columns
        TableColumn<Transaction, String> colId = new TableColumn<>("Mã GD");
        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getId()));
        colId.setPrefWidth(90);

        TableColumn<Transaction, String> colFrom = new TableColumn<>("TK Nguồn");
        colFrom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFromAccountNumber()));
        colFrom.setPrefWidth(100);

        TableColumn<Transaction, String> colTo = new TableColumn<>("TK Đích");
        colTo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getToAccountNumber()));
        colTo.setPrefWidth(100);

        TableColumn<Transaction, String> colDesc = new TableColumn<>("Mô tả");
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colDesc.setPrefWidth(240);

        TableColumn<Transaction, String> colAmount = new TableColumn<>("Số tiền");
        colAmount.setCellValueFactory(d -> new SimpleStringProperty(UiUtils.formatVnd(d.getValue().getAmount())));
        colAmount.setPrefWidth(140);

        TableColumn<Transaction, String> colFee = new TableColumn<>("Phí GD");
        colFee.setCellValueFactory(d -> new SimpleStringProperty(UiUtils.formatVnd(d.getValue().getFee())));
        colFee.setPrefWidth(110);

        TableColumn<Transaction, String> colDate = new TableColumn<>("Thời gian");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTimestamp().toString().replace("T", " ").substring(0, 19)));
        colDate.setPrefWidth(150);

        txTable.getColumns().setAll(colId, colFrom, colTo, colDesc, colAmount, colFee, colDate);
        txTable.setPrefHeight(220);
        txTable.setPlaceholder(new Label("Chưa có giao dịch nào được ghi nhận."));

        card.getChildren().addAll(cardHeader, txTable);
        getChildren().add(card);
    }

    private void buildObserverFeedSection() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        HBox cardHeader = new HBox(8);
        Label title = new Label("Kênh thông báo Observer thời gian thực (SMS, Email & UI Feed)");
        title.getStyleClass().add("card-title");
        Label badge = UiUtils.createPatternBadge("Observer Pattern", "behavioral");

        HBox.setHgrow(title, Priority.ALWAYS);
        cardHeader.getChildren().addAll(title, badge);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        observerFeed.setItems(ctx.getNotificationLogs());
        observerFeed.setPrefHeight(120);
        observerFeed.setPlaceholder(new Label("Chưa có thông báo nào từ các Observer."));

        Label note = new Label("💡 Mỗi khi số dư thay đổi, Account sẽ tự động gọi notifyObservers() để gửi tin nhắn đến SMS, Email và cập nhật trực tiếp lên bảng điều khiển.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        card.getChildren().addAll(cardHeader, observerFeed, note);
        getChildren().add(card);
    }

    public void refresh() {
        // Refresh account cards
        accountCardsContainer.getChildren().clear();
        for (Account acc : ctx.getAccounts()) {
            VBox card = new VBox(8);
            card.getStyleClass().add("account-card");
            if (acc.getType() == com.banking.model.enums.AccountType.PREMIUM) {
                card.getStyleClass().add("account-card-premium");
            }
            if (acc.getStatus() == AccountStatus.LOCKED) {
                card.getStyleClass().add("account-card-locked");
            }

            HBox topRow = new HBox(8);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label logo = new Label("VB");
            logo.setStyle("-fx-background-color: #C9A84C; -fx-text-fill: #0A2342; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 6; -fx-background-radius: 4px;");

            Label typeBadge = UiUtils.createTypeBadge(acc.getType());
            Label statusBadge = UiUtils.createStatusBadge(acc.getStatus());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            topRow.getChildren().addAll(logo, typeBadge, spacer, statusBadge);

            Label lblTypeTitle = new Label("Tài khoản " + acc.getType().name());
            lblTypeTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

            Label lblAccNo = new Label("Số TK: " + acc.getAccountNumber());
            lblAccNo.getStyleClass().add("account-number");

            Label lblOwner = new Label("Chủ TK: " + acc.getOwnerName());
            lblOwner.getStyleClass().add("account-owner");

            Label lblBalanceLabel = new Label("Số dư khả dụng:");
            lblBalanceLabel.getStyleClass().add("account-balance-label");

            Label lblBalance = new Label(UiUtils.formatVnd(acc.getBalance()));
            lblBalance.getStyleClass().add("account-balance");

            Label lblFeeDesc = new Label("Chiến lược: " + acc.getFeeStrategy().getName());
            lblFeeDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

            card.getChildren().addAll(topRow, lblTypeTitle, lblAccNo, lblOwner, new Separator(), lblBalanceLabel, lblBalance, lblFeeDesc);
            accountCardsContainer.getChildren().add(card);
        }

        // Refresh transaction table
        txTable.setItems(ctx.getTransactions());
    }
}

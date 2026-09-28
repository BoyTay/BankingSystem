package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import com.banking.pattern.behavioral.ActiveState;
import com.banking.pattern.behavioral.LockedState;
import com.banking.pattern.creational.DatabaseManager;
import com.banking.service.AuthService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Màn hình Dashboard — Tổng quan tài khoản, phân tích biểu đồ và giao dịch gần đây.
 * Minh họa Patterns:
 * - Singleton (DatabaseManager instance info)
 * - Observer (Live updates and event feed, dynamic charts reactivity)
 * - State (ActiveState / LockedState trực tiếp trên thẻ tài khoản)
 * - Facade (BankingFacade cho các thao tác nhanh)
 */
public class DashboardView extends VBox {

    private final UIContext ctx = UIContext.getInstance();
    private final Consumer<String> navigationHandler;
    private final AuthService.Role role;

    // Header & KPIs
    private final Label lblTotalAssets = new Label("0 VND");
    private final Label lblActiveAccounts = new Label("0 / 0 Hoạt động");
    private final Label lblTotalTransactions = new Label("0 giao dịch");

    // Account Cards Container
    private final HBox accountCardsContainer = new HBox(16);

    // Charts
    private final PieChart balancePieChart = new PieChart();
    private final CategoryAxis lineXAxis = new CategoryAxis();
    private final NumberAxis lineYAxis = new NumberAxis();
    private final LineChart<String, Number> balanceLineChart = new LineChart<>(lineXAxis, lineYAxis);
    private final ComboBox<String> cbAccountFilter = new ComboBox<>();
    private final ComboBox<String> cbTimeFilter = new ComboBox<>();
    private final Label lblChartObserverBadge = new Label("● Observer Live Sync");

    // Table & Feeds
    private final TableView<Transaction> txTable = new TableView<>();
    private final ListView<String> observerFeed = new ListView<>();

    public DashboardView(Consumer<String> navigationHandler, AuthService.Role role) {
        this.navigationHandler = navigationHandler;
        this.role = role;
        setSpacing(24);
        getStyleClass().add("content-pane");

        buildHeader();
        buildKpiSummarySection();
        buildAccountCardsSection();
        buildAnalyticsChartsSection();
        buildRecentTransactionsSection();
        buildObserverFeedSection();

        // Đăng ký cập nhật khi có thay đổi dữ liệu (Observer Pattern)
        ctx.addDataChangeListener(this::refresh);
        refresh();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Dashboard Tổng Quan");
        title.getStyleClass().add("page-title");

        String currentOwner = ctx.getAccounts().isEmpty() ? "Quý khách" : ctx.getAccounts().get(0).getOwnerName();
        Label subtitle = new Label("Xin chào, " + currentOwner + "! Chào mừng đến với hệ thống ngân hàng số VietBank.");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        HBox patternBadges = new HBox(8);
        patternBadges.setAlignment(Pos.CENTER_RIGHT);
        patternBadges.getChildren().addAll(
                UiUtils.createPatternBadge("Singleton (DB @" + Integer.toHexString(DatabaseManager.getInstance().hashCode()) + ")", "creational"),
                UiUtils.createPatternBadge("Observer (Live Account Stream)", "behavioral")
        );

        HBox header = new HBox();
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, patternBadges);
        header.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(header);
    }

    private void buildKpiSummarySection() {
        HBox kpiContainer = new HBox(16);
        kpiContainer.setAlignment(Pos.CENTER_LEFT);

        // Card 1: Tổng tài sản
        VBox cardAssets = createKpiCard("💰", "TỔNG SỐ DƯ TÀI SẢN", lblTotalAssets, "Tất cả tài khoản thanh toán & tiết kiệm");
        // Card 2: Tài khoản hoạt động
        VBox cardAccounts = createKpiCard("🛡️", "TRẠNG THÁI TÀI KHOẢN", lblActiveAccounts, "Kiểm soát qua State Pattern");
        // Card 3: Lịch sử & Undo
        VBox cardTransactions = createKpiCard("⚡", "HOẠT ĐỘNG GIAO DỊCH", lblTotalTransactions, "Lệnh Command & Undo sẵn sàng");

        HBox.setHgrow(cardAssets, Priority.ALWAYS);
        HBox.setHgrow(cardAccounts, Priority.ALWAYS);
        HBox.setHgrow(cardTransactions, Priority.ALWAYS);

        kpiContainer.getChildren().addAll(cardAssets, cardAccounts, cardTransactions);
        getChildren().add(kpiContainer);
    }

    private VBox createKpiCard(String icon, String title, Label valueLabel, String subtext) {
        VBox card = new VBox(6);
        card.getStyleClass().add("kpi-card");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label lblIcon = new Label(icon);
        lblIcon.setStyle("-fx-font-size: 18px;");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("kpi-title");
        topRow.getChildren().addAll(lblIcon, lblTitle);

        valueLabel.getStyleClass().add("kpi-value");

        Label lblSub = new Label(subtext);
        lblSub.getStyleClass().add("kpi-subtext");

        card.getChildren().addAll(topRow, valueLabel, lblSub);
        return card;
    }

    private void buildAccountCardsSection() {
        VBox section = new VBox(12);

        HBox secHeader = new HBox();
        secHeader.setAlignment(Pos.CENTER_LEFT);

        Label secTitle = new Label("Danh sách tài khoản (Account Cards)");
        secTitle.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnOpenAcc = new Button("➕ Mở thêm tài khoản");
        btnOpenAcc.getStyleClass().add("btn-secondary");
        btnOpenAcc.setOnAction(e -> navigationHandler.accept("accounts"));

        secHeader.getChildren().addAll(secTitle, spacer);
        if (role == AuthService.Role.ADMIN) secHeader.getChildren().add(btnOpenAcc);

        accountCardsContainer.setAlignment(Pos.CENTER_LEFT);
        accountCardsContainer.setPadding(new Insets(4, 4, 12, 4));

        ScrollPane scrollPane = new ScrollPane(accountCardsContainer);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        section.getChildren().addAll(secHeader, scrollPane);
        getChildren().add(section);
    }

    private void buildAnalyticsChartsSection() {
        HBox chartsRow = new HBox(20);
        chartsRow.setAlignment(Pos.TOP_LEFT);

        // ── Card 1: PieChart Tỷ Trọng Số Dư ───────────────────
        VBox pieCard = new VBox(12);
        pieCard.getStyleClass().add("card");
        HBox.setHgrow(pieCard, Priority.ALWAYS);
        pieCard.setPrefWidth(380);

        HBox pieHeader = new HBox(8);
        pieHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblPieTitle = new Label("Cơ cấu số dư theo loại tài khoản");
        lblPieTitle.getStyleClass().add("card-title");
        Region pieSpacer = new Region();
        HBox.setHgrow(pieSpacer, Priority.ALWAYS);
        Label pieBadge = UiUtils.createPatternBadge("Observer", "behavioral");
        pieHeader.getChildren().addAll(lblPieTitle, pieSpacer, pieBadge);

        balancePieChart.setAnimated(true);
        balancePieChart.setLegendSide(Side.BOTTOM);
        balancePieChart.setPrefHeight(270);

        Label pieNote = new Label("Biểu đồ phân bổ tỷ lệ phần trăm số dư giữa các loại gói Standard, Savings và Premium.");
        pieNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        pieCard.getChildren().addAll(pieHeader, balancePieChart, pieNote);

        // ── Card 2: LineChart Biến Động Số Dư Theo Thời Gian ──
        VBox lineCard = new VBox(12);
        lineCard.getStyleClass().add("card");
        HBox.setHgrow(lineCard, Priority.ALWAYS);
        lineCard.setPrefWidth(540);

        HBox lineHeader = new HBox(10);
        lineHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblLineTitle = new Label("Biến động số dư theo thời gian");
        lblLineTitle.getStyleClass().add("card-title");

        Region lineSpacer = new Region();
        HBox.setHgrow(lineSpacer, Priority.ALWAYS);

        // Observer sync indicator badge
        lblChartObserverBadge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12px;");

        // Filter controls
        cbAccountFilter.setPrefWidth(140);
        cbAccountFilter.getStyleClass().add("combo-box");
        cbAccountFilter.setOnAction(e -> refreshLineChart());

        cbTimeFilter.getItems().setAll("Tất cả mốc GD", "7 ngày gần nhất", "30 ngày gần nhất");
        cbTimeFilter.setValue("Tất cả mốc GD");
        cbTimeFilter.getStyleClass().add("combo-box");
        cbTimeFilter.setOnAction(e -> refreshLineChart());

        lineHeader.getChildren().addAll(lblLineTitle, lineSpacer, lblChartObserverBadge, cbAccountFilter, cbTimeFilter);

        // Configure LineChart axes
        lineXAxis.setLabel("Mốc thời gian");
        lineXAxis.setAnimated(false);
        lineYAxis.setLabel("Số dư (VND)");
        lineYAxis.setAnimated(true);
        lineYAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return UiUtils.formatVndCompact(object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });

        balanceLineChart.setAnimated(true);
        balanceLineChart.setLegendVisible(false);
        balanceLineChart.setPrefHeight(270);
        balanceLineChart.setCreateSymbols(true);

        Label lineNote = new Label("💡 Minh họa Observer Pattern: Đồ thị tự động cập nhật ngay lập tức khi phát sinh giao dịch Nạp, Rút, Chuyển hoặc Undo.");
        lineNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        lineCard.getChildren().addAll(lineHeader, balanceLineChart, lineNote);

        chartsRow.getChildren().addAll(pieCard, lineCard);
        getChildren().add(chartsRow);
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

        Label note = new Label("💡 Mỗi khi số dư thay đổi, Account sẽ tự động gọi notifyObservers() để gửi tin nhắn đến SMS, Email và cập nhật trực tiếp lên bảng điều khiển và đồ thị.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        card.getChildren().addAll(cardHeader, observerFeed, note);
        getChildren().add(card);
    }

    public void refresh() {
        refreshKpis();
        refreshAccountCards();
        refreshPieChart();
        refreshLineChart();

        // Refresh transaction table
        txTable.setItems(ctx.getTransactions());
    }

    private void refreshKpis() {
        double totalBalance = ctx.getAccounts().stream().mapToDouble(Account::getBalance).sum();
        lblTotalAssets.setText(UiUtils.formatVnd(totalBalance));

        long totalCount = ctx.getAccounts().size();
        long activeCount = ctx.getAccounts().stream().filter(a -> a.getStatus() == AccountStatus.ACTIVE).count();
        lblActiveAccounts.setText(activeCount + " / " + totalCount + " Hoạt động");

        int txCount = ctx.getTransactions().size();
        int undoCount = ctx.getTxHistory().size();
        lblTotalTransactions.setText(txCount + " giao dịch (" + undoCount + " Undo)");
    }

    private void refreshAccountCards() {
        accountCardsContainer.getChildren().clear();

        // Cập nhật danh sách tài khoản cho bộ lọc của LineChart
        String currentSelectedAcc = cbAccountFilter.getValue();
        List<String> accOptions = new ArrayList<>();
        accOptions.add("Tất cả tài khoản");
        for (Account a : ctx.getAccounts()) {
            accOptions.add(a.getAccountNumber());
        }
        cbAccountFilter.getItems().setAll(accOptions);
        if (currentSelectedAcc != null && accOptions.contains(currentSelectedAcc)) {
            cbAccountFilter.setValue(currentSelectedAcc);
        } else {
            cbAccountFilter.setValue("Tất cả tài khoản");
        }

        // Tạo từng thẻ tài khoản tương tác
        for (Account acc : ctx.getAccounts()) {
            VBox card = new VBox(8);
            card.getStyleClass().add("account-card");
            if (acc.getType() == AccountType.PREMIUM) {
                card.getStyleClass().add("account-card-premium");
            }
            if (acc.getStatus() == AccountStatus.LOCKED) {
                card.getStyleClass().add("account-card-locked");
            }

            // Top Row
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

            // ── Quick Action Toolbar trực tiếp trên thẻ ───────────
            HBox actionToolbar = new HBox(6);
            actionToolbar.setAlignment(Pos.CENTER_LEFT);
            actionToolbar.setPadding(new Insets(6, 0, 0, 0));

            Button btnQuickDeposit = new Button("📥 Nạp");
            btnQuickDeposit.getStyleClass().add("btn-card-action-primary");
            btnQuickDeposit.setTooltip(new Tooltip("Nạp tiền nhanh vào TK " + acc.getAccountNumber()));
            btnQuickDeposit.setOnAction(e -> showQuickDepositDialog(acc));

            Button btnQuickWithdraw = new Button("📤 Rút");
            btnQuickWithdraw.getStyleClass().add("btn-card-action");
            btnQuickWithdraw.setTooltip(new Tooltip("Rút tiền từ TK " + acc.getAccountNumber()));
            btnQuickWithdraw.setOnAction(e -> showQuickWithdrawDialog(acc));

            Button btnQuickTransfer = new Button("⇄ Chuyển");
            btnQuickTransfer.getStyleClass().add("btn-card-action");
            btnQuickTransfer.setTooltip(new Tooltip("Chuyển tiền từ tài khoản này"));
            btnQuickTransfer.setOnAction(e -> navigationHandler.accept("transfer:" + acc.getAccountNumber()));

            Button btnToggleLock = new Button(acc.getStatus() == AccountStatus.ACTIVE ? "🔒 Khóa" : "🔓 Mở");
            btnToggleLock.getStyleClass().add(acc.getStatus() == AccountStatus.ACTIVE ? "btn-card-action-danger" : "btn-card-action-success");
            btnToggleLock.setTooltip(new Tooltip("Thay đổi trạng thái tài khoản (State Pattern)"));
            btnToggleLock.setOnAction(e -> toggleAccountLock(acc));

            if (role != AuthService.Role.VIEWER) {
                actionToolbar.getChildren().addAll(btnQuickDeposit, btnQuickWithdraw, btnQuickTransfer);
            }
            if (role == AuthService.Role.ADMIN) actionToolbar.getChildren().add(btnToggleLock);

            card.getChildren().addAll(topRow, lblTypeTitle, lblAccNo, lblOwner,
                    new Separator(), lblBalanceLabel, lblBalance, lblFeeDesc);
            if (!actionToolbar.getChildren().isEmpty()) {
                card.getChildren().addAll(new Separator(), actionToolbar);
            }

            accountCardsContainer.getChildren().add(card);
        }
    }

    private void refreshPieChart() {
        Map<AccountType, Double> balanceByType = ctx.getAccounts().stream()
                .collect(Collectors.groupingBy(Account::getType, Collectors.summingDouble(Account::getBalance)));

        double total = balanceByType.values().stream().mapToDouble(Double::doubleValue).sum();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (total > 0) {
            for (AccountType type : AccountType.values()) {
                double bal = balanceByType.getOrDefault(type, 0.0);
                if (bal > 0) {
                    double pct = (bal / total) * 100.0;
                    String label = String.format("%s (%.1f%%)", type.name(), pct);
                    PieChart.Data data = new PieChart.Data(label, bal);
                    pieData.add(data);
                }
            }
        } else {
            pieData.add(new PieChart.Data("Chưa có số dư (0%)", 1));
        }

        balancePieChart.setData(pieData);

        // Gắn Tooltip & Màu sắc tương ứng sau khi vẽ
        Platform.runLater(() -> {
            for (PieChart.Data d : balancePieChart.getData()) {
                if (d.getNode() != null) {
                    Tooltip.install(d.getNode(), new Tooltip(d.getName() + "\nSố dư: " + UiUtils.formatVnd(d.getPieValue())));
                    if (d.getName().startsWith("PREMIUM")) {
                        d.getNode().setStyle("-fx-pie-color: #C9A84C;");
                    } else if (d.getName().startsWith("SAVINGS")) {
                        d.getNode().setStyle("-fx-pie-color: #10B981;");
                    } else if (d.getName().startsWith("STANDARD")) {
                        d.getNode().setStyle("-fx-pie-color: #0A2342;");
                    }
                }
            }
        });
    }

    private void refreshLineChart() {
        String selectedAcc = cbAccountFilter.getValue();
        boolean isAll = selectedAcc == null || "Tất cả tài khoản".equals(selectedAcc);
        String targetAcc = isAll ? null : selectedAcc;

        String timeFilter = cbTimeFilter.getValue();
        if (timeFilter == null) timeFilter = "Tất cả mốc GD";

        List<Transaction> transactions = new ArrayList<>(ctx.getTransactions());
        transactions.sort(Comparator.comparing(Transaction::getTimestamp));

        // Lọc theo thời gian nếu cần
        LocalDateTime now = LocalDateTime.now();
        if ("7 ngày gần nhất".equals(timeFilter)) {
            LocalDateTime cutoff = now.minusDays(7);
            transactions.removeIf(tx -> tx.getTimestamp().isBefore(cutoff));
        } else if ("30 ngày gần nhất".equals(timeFilter)) {
            LocalDateTime cutoff = now.minusDays(30);
            transactions.removeIf(tx -> tx.getTimestamp().isBefore(cutoff));
        }

        // Tính toán chuỗi biến động số dư theo thuật toán hồi quy ngược từ số dư hiện tại
        Map<String, Double> runningBal = new HashMap<>();
        for (Account a : ctx.getAccounts()) {
            runningBal.put(a.getAccountNumber(), a.getBalance());
        }

        int n = transactions.size();
        double[] balancePoints = new double[n + 1];

        // Điểm cuối cùng chính là số dư hiện tại
        balancePoints[n] = targetAcc == null
                ? runningBal.values().stream().mapToDouble(Double::doubleValue).sum()
                : runningBal.getOrDefault(targetAcc, 0.0);

        // Hồi quy ngược qua từng giao dịch để tính số dư tại từng mốc
        for (int i = n - 1; i >= 0; i--) {
            Transaction tx = transactions.get(i);
            String from = tx.getFromAccountNumber();
            String to = tx.getToAccountNumber();
            double amount = tx.getAmount();
            double fee = tx.getFee();

            // Hoàn lại tiền cho from
            if (runningBal.containsKey(from)) {
                runningBal.put(from, runningBal.get(from) + amount + fee);
            }
            // Trừ lại tiền của to
            if (runningBal.containsKey(to)) {
                runningBal.put(to, runningBal.get(to) - amount);
            }

            balancePoints[i] = targetAcc == null
                    ? runningBal.values().stream().mapToDouble(Double::doubleValue).sum()
                    : runningBal.getOrDefault(targetAcc, 0.0);
        }

        // Xây dựng Series cho LineChart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(targetAcc == null ? "Tổng số dư" : "Số dư TK " + targetAcc);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Điểm khởi đầu
        String initialTime = n > 0 ? transactions.get(0).getTimestamp().minusMinutes(5).format(timeFmt) : "Khởi tạo";
        series.getData().add(new XYChart.Data<>(initialTime + " (Gốc)", balancePoints[0]));

        // Các điểm giao dịch tiếp theo
        for (int i = 0; i < n; i++) {
            Transaction tx = transactions.get(i);
            String timeLabel = tx.getTimestamp().format(timeFmt) + " (#" + (i + 1) + ")";
            series.getData().add(new XYChart.Data<>(timeLabel, balancePoints[i + 1]));
        }

        balanceLineChart.getData().setAll(series);

        // Gắn Tooltip hiển thị số tiền trên từng nốt dữ liệu
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> entry : series.getData()) {
                if (entry.getNode() != null) {
                    Tooltip.install(entry.getNode(), new Tooltip(
                            entry.getXValue() + "\nSố dư: " + UiUtils.formatVnd(entry.getYValue().doubleValue())
                    ));
                }
            }
        });
    }

    // ── Quick Action Dialogs ─────────────────────────────────

    private void showQuickDepositDialog(Account acc) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Nạp tiền nhanh — VietBank");
        dialog.setHeaderText("Nạp tiền trực tiếp vào TK: " + acc.getAccountNumber() + " (" + acc.getOwnerName() + ")");

        ButtonType btnConfirmType = new ButtonType("Xác nhận nạp", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmType, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setPadding(new Insets(10));

        Label lblCurrent = new Label("Số dư hiện tại: " + UiUtils.formatVnd(acc.getBalance()));
        lblCurrent.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TextField txtAmount = new TextField();
        txtAmount.setPromptText("Nhập số tiền nạp (VND)");
        java.util.regex.Pattern digitPattern = java.util.regex.Pattern.compile("\\d*");
        txtAmount.setTextFormatter(new TextFormatter<>(change ->
                digitPattern.matcher(change.getControlNewText()).matches() ? change : null
        ));

        HBox presetBox = new HBox(8);
        presetBox.setAlignment(Pos.CENTER_LEFT);
        double[] presets = { 500_000, 1_000_000, 2_000_000, 5_000_000, 10_000_000 };
        for (double p : presets) {
            String label = "+" + (p >= 1_000_000 ? ((long) (p / 1_000_000)) + "M" : ((long) (p / 1_000)) + "K");
            Button btnP = new Button(label);
            btnP.getStyleClass().add("btn-secondary");
            btnP.setOnAction(e -> txtAmount.setText(String.valueOf((long) p)));
            presetBox.getChildren().add(btnP);
        }

        content.getChildren().addAll(lblCurrent, new Label("Số tiền cần nạp:"), txtAmount, presetBox);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getStylesheets().add(UiUtils.class.getResource("/com/banking/ui/app.css").toExternalForm());
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnConfirmType);
        okBtn.getStyleClass().add("btn-primary");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnConfirmType) {
                try {
                    return Double.parseDouble(txtAmount.getText().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(amount -> {
            try {
                if (amount <= 0) {
                    ToastNotification.showWarning("Số tiền nạp phải lớn hơn 0 VND.");
                    return;
                }
                ctx.getFacade().deposit(acc.getAccountNumber(), amount);
                ctx.notifyDataChanged();
                ToastNotification.showSuccess(String.format("Đã nạp thành công %s vào TK %s!",
                        UiUtils.formatVnd(amount), acc.getAccountNumber()));
            } catch (Exception ex) {
                String friendly = UiUtils.humanizeError(ex);
                ToastNotification.showError(friendly);
                UiUtils.showAlert(Alert.AlertType.ERROR, "Lỗi nạp tiền", null, friendly);
            }
        });
    }

    private void showQuickWithdrawDialog(Account acc) {
        if (acc.getStatus() == AccountStatus.LOCKED) {
            String msg = "Theo quy tắc của State Pattern: Tài khoản ở LockedState không được phép rút tiền!\nVui lòng mở khóa tài khoản trước khi thực hiện.";
            ToastNotification.showError("Tài khoản đang bị KHÓA (LockedState)!");
            UiUtils.showAlert(Alert.AlertType.ERROR, "Lỗi State Pattern", "Tài khoản đang bị KHÓA", msg);
            return;
        }

        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Rút tiền nhanh — VietBank");
        dialog.setHeaderText("Rút tiền mặt từ TK: " + acc.getAccountNumber() + " (" + acc.getOwnerName() + ")");

        ButtonType btnConfirmType = new ButtonType("Xác nhận rút", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmType, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setPadding(new Insets(10));

        Label lblCurrent = new Label("Số dư khả dụng: " + UiUtils.formatVnd(acc.getBalance()));
        lblCurrent.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TextField txtAmount = new TextField();
        txtAmount.setPromptText("Nhập số tiền rút (VND)");
        java.util.regex.Pattern digitPattern = java.util.regex.Pattern.compile("\\d*");
        txtAmount.setTextFormatter(new TextFormatter<>(change ->
                digitPattern.matcher(change.getControlNewText()).matches() ? change : null
        ));

        Label lblFee = new Label("Phí ước tính: 0 VND (Chiến lược: " + acc.getFeeStrategy().getName() + ")");
        lblFee.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        txtAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double amt = Double.parseDouble(newVal.trim());
                double fee = acc.getFeeStrategy().calculateFee(amt);
                lblFee.setText("Phí ước tính: " + UiUtils.formatVnd(fee) + " (Chiến lược: " + acc.getFeeStrategy().getName() + ")");
            } catch (Exception e) {
                lblFee.setText("Phí ước tính: 0 VND (Chiến lược: " + acc.getFeeStrategy().getName() + ")");
            }
        });

        HBox presetBox = new HBox(8);
        presetBox.setAlignment(Pos.CENTER_LEFT);
        double[] presets = { 200_000, 500_000, 1_000_000, 2_000_000, 5_000_000 };
        for (double p : presets) {
            String label = (p >= 1_000_000 ? ((long) (p / 1_000_000)) + "M" : ((long) (p / 1_000)) + "K");
            Button btnP = new Button(label);
            btnP.getStyleClass().add("btn-secondary");
            btnP.setOnAction(e -> txtAmount.setText(String.valueOf((long) p)));
            presetBox.getChildren().add(btnP);
        }

        content.getChildren().addAll(lblCurrent, new Label("Số tiền cần rút:"), txtAmount, lblFee, presetBox);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getStylesheets().add(UiUtils.class.getResource("/com/banking/ui/app.css").toExternalForm());
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnConfirmType);
        okBtn.getStyleClass().add("btn-primary");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnConfirmType) {
                try {
                    return Double.parseDouble(txtAmount.getText().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(amount -> {
            try {
                if (amount <= 0) {
                    ToastNotification.showWarning("Số tiền rút phải lớn hơn 0 VND.");
                    return;
                }
                ctx.getFacade().withdraw(acc.getAccountNumber(), amount);
                ctx.notifyDataChanged();
                ToastNotification.showSuccess(String.format("Đã rút thành công %s từ TK %s!",
                        UiUtils.formatVnd(amount), acc.getAccountNumber()));
            } catch (Exception ex) {
                String friendly = UiUtils.humanizeError(ex);
                ToastNotification.showError(friendly);
                UiUtils.showAlert(Alert.AlertType.ERROR, "Lỗi rút tiền", null, friendly);
            }
        });
    }

    private void toggleAccountLock(Account acc) {
        if (acc.getStatus() == AccountStatus.ACTIVE) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nhận khóa tài khoản");
            confirm.setHeaderText("Khóa tài khoản " + acc.getAccountNumber() + " (" + acc.getOwnerName() + ")");
            confirm.setContentText("Hành vi State Pattern: Tài khoản sẽ chuyển sang LockedState và chặn mọi giao dịch rút tiền. Bạn có chắc chắn muốn khóa?");
            confirm.getDialogPane().getStylesheets().add(UiUtils.class.getResource("/com/banking/ui/app.css").toExternalForm());
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                try {
                    ctx.getAccountService().lockAccount(acc.getAccountNumber());
                    ctx.logCustomEvent("State Pattern", "Tài khoản " + acc.getAccountNumber() + " chuyển sang LockedState.");
                    ctx.notifyDataChanged();
                    ToastNotification.showWarning("Đã khóa tài khoản " + acc.getAccountNumber() + " (LockedState)");
                } catch (Exception ex) {
                    String friendly = UiUtils.humanizeError(ex);
                    ToastNotification.showError(friendly);
                    UiUtils.showAlert(Alert.AlertType.ERROR, "Lỗi", null, friendly);
                }
            }
        } else {
            try {
                ctx.getAccountService().unlockAccount(acc.getAccountNumber());
                ctx.logCustomEvent("State Pattern", "Tài khoản " + acc.getAccountNumber() + " chuyển sang ActiveState.");
                ctx.notifyDataChanged();
                ToastNotification.showSuccess("Đã mở khóa tài khoản " + acc.getAccountNumber() + " (ActiveState)");
            } catch (Exception ex) {
                String friendly = UiUtils.humanizeError(ex);
                ToastNotification.showError(friendly);
                UiUtils.showAlert(Alert.AlertType.ERROR, "Lỗi", null, friendly);
            }
        }
    }
}

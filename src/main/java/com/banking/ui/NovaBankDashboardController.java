package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountType;
import com.banking.service.AuthService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller cho giao diện NovaBank Dashboard (Overview).
 * Tích hợp trực tiếp với UIContext và 9 Design Patterns:
 * - Observer Pattern: Tự động lắng nghe thay đổi số dư và cập nhật LineChart, Donut Chart.
 * - Strategy Pattern: Hiển thị phân loại số dư theo loại tài khoản (STANDARD, SAVINGS, PREMIUM).
 * - Facade & Command: Hỗ trợ các nút thao tác nhanh (Quick Actions).
 */
public class NovaBankDashboardController implements Initializable, NovaBankNavigable {

    @FXML private Label lblGreeting;
    @FXML private Label lblTotalBalance;
    @FXML private Button btnToggleBalance;
    @FXML private SVGPath svgBalanceEye;
    @FXML private Label lblTrendBadge;
    @FXML private HBox boxTrendBadge;

    private static final String EYE_SHOW = "M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z";
    private static final String EYE_HIDE = "M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21";

    // Profile Header
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileRole;
    @FXML private Label lblProfileAvatar;

    // Charts
    @FXML private AreaChart<String, Number> lineChartBalance;
    @FXML private CategoryAxis axisMonths;
    @FXML private NumberAxis axisAmount;
    @FXML private ComboBox<String> cbTimeRange;

    @FXML private PieChart pieChartDistribution;
    @FXML private Label lblDonutTotal;
    @FXML private Label lblStandardAmount;
    @FXML private Label lblStandardPct;
    @FXML private Label lblSavingsAmount;
    @FXML private Label lblSavingsPct;
    @FXML private Label lblPremiumAmount;
    @FXML private Label lblPremiumPct;

    @FXML private HBox boxRecentTransactions;
    @FXML private ListView<String> listObserverEvents;

    private final UIContext ctx = UIContext.getInstance();
    private boolean isBalanceHidden = false;
    private double currentTotalBalance;
    private Consumer<String> navigator = key -> { };

    @Override
    public void setNavigator(Consumer<String> navigator) { this.navigator = navigator; }

    public void setUser(AuthService.User user) {
        String name = user.username();
        String display = name.substring(0, 1).toUpperCase() + name.substring(1);
        lblGreeting.setText("Welcome back, " + display);

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
        listObserverEvents.setItems(ctx.getNotificationLogs());
        listObserverEvents.setPlaceholder(new Label("Chưa có thông báo tài khoản."));
        ctx.getNotificationLogs().addListener((ListChangeListener<String>) change ->
                listObserverEvents.scrollTo(0));
        if (btnToggleBalance != null) {
            btnToggleBalance.setText("");
        }
        setupTimeFilter();
        setupAxisFormatter();
        loadDashboardData();

        // ── OBSERVER PATTERN HOOK ──────────────────────────────────────────
        // Lắng nghe sự kiện phát sinh giao dịch mới và làm mới giao diện
        ctx.addDataChangeListener(() -> Platform.runLater(this::loadDashboardData));
    }

    private void setupAxisFormatter() {
        axisAmount.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                double val = object.doubleValue();
                return UiUtils.formatVndCompact(val);
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void setupTimeFilter() {
        cbTimeRange.setItems(FXCollections.observableArrayList("30 days", "3 months", "6 months", "1 year"));
        cbTimeRange.setValue("6 months");
        cbTimeRange.valueProperty().addListener((obs, oldVal, newVal) -> loadBalanceTrendData());
    }

    /**
     * Tải và đồng bộ toàn bộ dữ liệu Dashboard từ DatabaseManager & UIContext
     */
    public void loadDashboardData() {
        double standardTotal = 0;
        double savingsTotal = 0;
        double premiumTotal = 0;

        List<Account> accounts = ctx.getAccounts();
        if (accounts != null && !accounts.isEmpty()) {
            for (Account acc : accounts) {
                if (acc.getType() == AccountType.STANDARD) {
                    standardTotal += acc.getBalance();
                } else if (acc.getType() == AccountType.SAVINGS) {
                    savingsTotal += acc.getBalance();
                } else if (acc.getType() == AccountType.PREMIUM) {
                    premiumTotal += acc.getBalance();
                }
            }
        }
        currentTotalBalance = standardTotal + savingsTotal + premiumTotal;

        // Cập nhật thẻ tổng số dư
        updateBalanceDisplay();

        // Cập nhật Donut Chart tỷ trọng tài khoản
        updateAccountDistribution(standardTotal, savingsTotal, premiumTotal);

        // Cập nhật biểu đồ biến động LineChart
        loadBalanceTrendData();

        // Cập nhật thẻ giao dịch gần nhất
        loadRecentTransactions();
    }

    private void updateBalanceDisplay() {
        if (isBalanceHidden) {
            lblTotalBalance.setText("••••••••");
            if (lblTrendBadge != null) lblTrendBadge.setText("••••");
        } else {
            lblTotalBalance.setText(UiUtils.formatVnd(currentTotalBalance));
            if (lblTrendBadge != null) updateBalanceTrend();
        }
    }

    private void updateBalanceTrend() {
        double lastMonthBalance = currentTotalBalance;
        YearMonth currentMonth = YearMonth.from(LocalDate.now());

        List<Transaction> transactions = ctx.getTransactions();
        if (transactions != null) {
            for (Transaction tx : transactions) {
                if (!YearMonth.from(tx.getTimestamp()).isBefore(currentMonth)) {
                    lastMonthBalance -= balanceChange(tx);
                }
            }
        }

        double diff = currentTotalBalance - lastMonthBalance;
        if (lastMonthBalance == 0) {
            lblTrendBadge.setText(diff > 0 ? "↗ 100%" : (diff < 0 ? "↘ 100%" : "→ 0%"));
            setTrendBadgeStyle(diff);
        } else {
            double pct = (diff / Math.abs(lastMonthBalance)) * 100.0;
            lblTrendBadge.setText(String.format("%s %.1f%%", diff >= 0 ? "↗" : "↘", Math.abs(pct)));
            setTrendBadgeStyle(diff);
        }
    }

    private void setTrendBadgeStyle(double diff) {
        if (diff >= 0) {
            boxTrendBadge.setStyle("-fx-background-color: rgba(0, 208, 132, 0.15); -fx-background-radius: 4px; -fx-padding: 2px 6px;");
            lblTrendBadge.setStyle("-fx-text-fill: #00D084; -fx-font-weight: 700;");
        } else {
            boxTrendBadge.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-background-radius: 4px; -fx-padding: 2px 6px;");
            lblTrendBadge.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: 700;");
        }
    }

    private void updateAccountDistribution(double standard, double savings, double premium) {
        double total = standard + savings + premium;
        double pStd = total == 0 ? 0 : (standard / total) * 100.0;
        double pSav = total == 0 ? 0 : (savings / total) * 100.0;
        double pPre = total == 0 ? 0 : (premium / total) * 100.0;

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Standard", standard),
                new PieChart.Data("Savings", savings),
                new PieChart.Data("Premium", premium)
        );
        pieChartDistribution.setData(pieData);

        lblDonutTotal.setText(UiUtils.formatVndCompact(total).replace(" VND", ""));
        lblStandardAmount.setText(UiUtils.formatVndCompact(standard));
        lblStandardPct.setText(String.format("%.0f%%", pStd));

        lblSavingsAmount.setText(UiUtils.formatVndCompact(savings));
        lblSavingsPct.setText(String.format("%.0f%%", pSav));

        lblPremiumAmount.setText(UiUtils.formatVndCompact(premium));
        lblPremiumPct.setText(String.format("%.0f%%", pPre));
    }

    private void loadBalanceTrendData() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Balance Trend");

        int months = switch (cbTimeRange.getValue()) {
            case "30 days" -> 1;
            case "3 months" -> 3;
            case "1 year" -> 12;
            default -> 6;
        };
        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        List<Transaction> ledger = ctx.getTransactions();
        for (int offset = months - 1; offset >= 0; offset--) {
            YearMonth month = currentMonth.minusMonths(offset);
            double balance = currentTotalBalance;
            for (Transaction tx : ledger) {
                if (YearMonth.from(tx.getTimestamp()).isAfter(month)) {
                    balance -= balanceChange(tx);
                }
            }
            series.getData().add(new XYChart.Data<>(month.format(DateTimeFormatter.ofPattern("MM/yyyy")), Math.max(0, balance)));
        }

        lineChartBalance.getData().setAll(series);
    }

    private double balanceChange(Transaction tx) {
        if ("SYSTEM".equals(tx.getFromAccountNumber())) return tx.getAmount();
        if ("CASH".equals(tx.getToAccountNumber())) return -tx.getAmount() - tx.getFee();
        if (tx.getRelatedTransactionId() != null) {
            String description = tx.getDescription();
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("hoàn phí (\\d+)").matcher(description);
            return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0;
        }
        return -tx.getFee();
    }

    private void loadRecentTransactions() {
        List<Transaction> transactions = ctx.getTransactions();
        boxRecentTransactions.getChildren().clear();
        if (transactions == null || transactions.isEmpty()) {
            boxRecentTransactions.getChildren().add(new Label("Chưa có giao dịch."));
            return;
        }
        int count = Math.min(3, transactions.size());
        for (int i = 0; i < count; i++) {
            Transaction tx = transactions.get(transactions.size() - 1 - i);
            boxRecentTransactions.getChildren().add(createTxCard(tx));
        }
    }

    private HBox createTxCard(Transaction tx) {
        HBox card = new HBox(12);
        card.getStyleClass().add("tx-card");
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);

        boolean deposit = "SYSTEM".equals(tx.getFromAccountNumber());
        boolean withdraw = "CASH".equals(tx.getToAccountNumber());

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("tx-icon-box");
        if (deposit) {
            iconBox.setStyle("-fx-background-color: #E6FBF2; -fx-border-color: #E6FBF2;");
        } else {
            iconBox.setStyle("-fx-background-color: #F1F5F9; -fx-border-color: #F1F5F9;");
        }

        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        if (deposit) {
            // Arrow down (Income)
            icon.setContent("M 12 5 L 12 19 M 12 19 L 5 12 M 12 19 L 19 12");
            icon.setStyle("-fx-stroke: #00C476; -fx-stroke-width: 2px; -fx-fill: transparent; -fx-stroke-line-cap: round; -fx-stroke-line-join: round;");
        } else {
            // Arrow up right (Expense/Transfer)
            icon.setContent("M 5 19 L 19 5 M 19 5 L 9 5 M 19 5 L 19 15");
            icon.setStyle("-fx-stroke: #64748B; -fx-stroke-width: 2px; -fx-fill: transparent; -fx-stroke-line-cap: round; -fx-stroke-line-join: round;");
        }
        iconBox.getChildren().add(icon);

        VBox metaBox = new VBox(2);
        HBox.setHgrow(metaBox, Priority.ALWAYS);
        Label title = new Label(tx.getDescription() != null ? tx.getDescription() : "Chuyển tiền");
        title.getStyleClass().add("tx-title");

        String time = tx.getTimestamp() != null ? tx.getTimestamp().toString().replace("T", " ").substring(0, 16) : "Vừa xong";
        Label meta = new Label(tx.getFromAccountNumber() + " → " + tx.getToAccountNumber() + " • " + time);
        meta.getStyleClass().add("tx-meta");
        metaBox.getChildren().addAll(title, meta);

        Label amount = new Label((deposit ? "+" : withdraw ? "−" : "") + UiUtils.formatVnd(tx.getAmount()));
        amount.getStyleClass().add(deposit ? "tx-amount-pos" : "tx-amount-neg");

        card.getChildren().addAll(iconBox, metaBox, amount);
        return card;
    }

    @FXML
    private void toggleBalanceVisibility() {
        isBalanceHidden = !isBalanceHidden;
        if (btnToggleBalance != null) {
            btnToggleBalance.setText("");
        }
        if (svgBalanceEye != null) {
            svgBalanceEye.setContent(isBalanceHidden ? EYE_HIDE : EYE_SHOW);
        } else if (btnToggleBalance != null && btnToggleBalance.getGraphic() instanceof SVGPath path) {
            path.setContent(isBalanceHidden ? EYE_HIDE : EYE_SHOW);
        }
        updateBalanceDisplay();
    }

    @FXML
    private void handleQuickTransfer() {
        navigator.accept("transfer");
    }

    @FXML
    private void handleQuickTopUp() {
        navigator.accept("cash");
    }

    @FXML
    private void handleQuickPayBill() {
        navigator.accept("history");
    }

    @FXML
    private void handleNavTransfer() {
        handleQuickTransfer();
    }

    @FXML
    private void handleNavTransactions() {
        navigator.accept("history");
    }

    @FXML
    private void handleNavAccounts() {
        navigator.accept("accounts");
    }

    @FXML
    private void handleViewAllTransactions() {
        handleNavTransactions();
    }
}

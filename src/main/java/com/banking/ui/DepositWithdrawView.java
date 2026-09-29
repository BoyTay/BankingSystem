package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Màn hình Nạp & Rút tiền mặt (Cash View).
 * Minh họa Patterns:
 * - State Pattern: Khi tài khoản ở LockedState, lệnh rút tiền sẽ bị từ chối ngay lập tức tại cấp State.
 * - Strategy Pattern: Tính phí rút tiền linh hoạt theo chiến lược riêng của từng tài khoản.
 * - Observer Pattern: Kích hoạt thông báo đa kênh (SMS, Email, UI) sau giao dịch.
 * - Facade Pattern: Gọi qua BankingFacade để xử lý trọn gói.
 */
public class DepositWithdrawView extends VBox {

    private final UIContext ctx = UIContext.getInstance();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    // Controls Nạp tiền
    private final ComboBox<Account> cbDepositAccount = new ComboBox<>();
    private final TextField txtDepositAmount = new TextField();
    private final Label lblDepositBalance = new Label("-");
    private final Label lblDepositStatusBadge = new Label("● ACTIVE");
    private final Label lblDepositEstimatedBal = new Label("0 VND");
    private final Label lblDepositValidation = new Label();
    private final Button btnDeposit = new Button("Xác Nhận Nạp Tiền");

    // Controls Rút tiền
    private final ComboBox<Account> cbWithdrawAccount = new ComboBox<>();
    private final TextField txtWithdrawAmount = new TextField();
    private final Label lblWithdrawBalance = new Label("-");
    private final Label lblWithdrawStatusBadge = new Label("● ACTIVE");
    private final Label lblWithdrawFeePreview = new Label("0 VND");
    private final Label lblWithdrawTotalDebit = new Label("0 VND");
    private final Label lblWithdrawRemainingBal = new Label("0 VND");
    private final Label lblWithdrawValidation = new Label();
    private final Button btnWithdraw = new Button("Xác Nhận Rút Tiền");

    // Bottom container for recent cash transactions
    private final VBox boxRecentCashTx = new VBox(8);

    private final AuthService.User user;

    public DepositWithdrawView(AuthService.User user) {
        this.user = user;
        setSpacing(24);
        getStyleClass().add("content-pane");

        buildHeader();

        // 2 CỘT CHÍNH: Nạp tiền (Trái) & Rút tiền (Phải)
        HBox mainCards = new HBox(24);
        VBox depositCard = buildDepositCard();
        VBox withdrawCard = buildWithdrawCard();
        HBox.setHgrow(depositCard, Priority.ALWAYS);
        HBox.setHgrow(withdrawCard, Priority.ALWAYS);
        mainCards.getChildren().addAll(depositCard, withdrawCard);

        // 2 CỘT DƯỚI: Lịch sử GD tiền mặt gần nhất (Trái) & Cơ chế Design Patterns (Phải)
        HBox bottomSection = new HBox(24);
        VBox txHistoryCard = buildRecentCashCard();
        VBox patternCard = buildPatternArchitectureCard();
        HBox.setHgrow(txHistoryCard, Priority.ALWAYS);
        HBox.setHgrow(patternCard, Priority.ALWAYS);
        bottomSection.getChildren().addAll(txHistoryCard, patternCard);

        getChildren().addAll(mainCards, bottomSection);

        ctx.addDataChangeListener(this::refresh);
        refresh();
    }

    public DepositWithdrawView() {
        this(null);
    }

    private void buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        VBox titleBox = new VBox(5);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Eyebrow Tag (Hug content with USE_PREF_SIZE)
        HBox tagBox = new HBox(6);
        tagBox.setAlignment(Pos.CENTER_LEFT);
        tagBox.getStyleClass().add("header-badge-tag");
        tagBox.setMaxWidth(Double.NEGATIVE_INFINITY);
        Circle tagDot = new Circle(3.0);
        tagDot.getStyleClass().add("header-badge-tag-dot");
        Label tagText = new Label("QUẦY TIỀN MẶT");
        tagText.getStyleClass().add("header-badge-tag-text");
        tagBox.getChildren().addAll(tagDot, tagText);

        Label title = new Label("Nạp & Rút Tiền Mặt");
        title.getStyleClass().add("header-greeting");

        Label subtitle = new Label("Giao dịch tiền mặt tức thì — Tích hợp State Pattern (khóa/mở) và Strategy Pattern (tính phí tự động).");
        subtitle.getStyleClass().add("header-subtitle");

        titleBox.getChildren().addAll(tagBox, title, subtitle);

        // Right Controls: Search Box + Notification Bell with Red Dot + User Profile Pill
        HBox rightControls = new HBox(12);
        rightControls.setAlignment(Pos.CENTER_RIGHT);

        // Search Box with shortcut cap
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("search-box");
        searchBox.setPrefWidth(290);
        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z");
        searchIcon.getStyleClass().add("search-icon");
        TextField searchInput = new TextField();
        searchInput.setPromptText("Tìm kiếm giao dịch...");
        searchInput.getStyleClass().add("search-input");
        HBox.setHgrow(searchInput, Priority.ALWAYS);
        Label shortcutBadge = new Label("Ctrl K");
        shortcutBadge.getStyleClass().add("search-shortcut-badge");
        shortcutBadge.setMinWidth(Region.USE_PREF_SIZE);
        searchBox.getChildren().addAll(searchIcon, searchInput, shortcutBadge);

        // Notification Bell with Red Dot
        StackPane bellBtn = new StackPane();
        bellBtn.getStyleClass().add("icon-btn-round");
        SVGPath bellIcon = new SVGPath();
        bellIcon.setContent("M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9");
        bellIcon.getStyleClass().add("header-bell-icon");
        Circle redDot = new Circle(3.5);
        redDot.getStyleClass().add("notification-dot-badge");
        StackPane.setAlignment(redDot, Pos.TOP_RIGHT);
        StackPane.setMargin(redDot, new Insets(8, 8, 0, 0));
        bellBtn.getChildren().addAll(bellIcon, redDot);

        // User Profile Pill
        HBox profilePill = new HBox(9);
        profilePill.setAlignment(Pos.CENTER_LEFT);
        profilePill.getStyleClass().add("user-profile-pill");

        String uname = (user != null) ? user.username() : "Admin";
        String urole = (user != null) ? user.role().name() : "Admin";
        String initial = !uname.isEmpty() ? uname.substring(0, 1).toUpperCase() : "A";
        String displayUname = uname.substring(0, 1).toUpperCase() + (uname.length() > 1 ? uname.substring(1).toLowerCase() : "");
        String displayRole = urole.substring(0, 1).toUpperCase() + (urole.length() > 1 ? urole.substring(1).toLowerCase() : "");

        StackPane avatarCircle = new StackPane();
        avatarCircle.getStyleClass().add("avatar-circle");
        Label avatarLabel = new Label(initial);
        avatarLabel.getStyleClass().add("avatar-text");
        avatarCircle.getChildren().add(avatarLabel);

        VBox nameBox = new VBox(1);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(displayUname);
        nameLabel.getStyleClass().add("user-name");
        Label roleLabel = new Label(displayRole);
        roleLabel.getStyleClass().add("user-tier");
        nameBox.getChildren().addAll(nameLabel, roleLabel);

        Label chevron = new Label("▾");
        chevron.getStyleClass().add("user-chevron");

        profilePill.getChildren().addAll(avatarCircle, nameBox, chevron);

        rightControls.getChildren().addAll(searchBox, bellBtn, profilePill);
        header.getChildren().addAll(titleBox, rightControls);

        getChildren().add(header);
    }

    private VBox buildDepositCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        // Card Header (Icon Circle + Title + Subtitle)
        HBox cardHeader = new HBox(12);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane();
        iconCircle.getStyleClass().add("icon-circle-blue");
        SVGPath depositSvg = new SVGPath();
        depositSvg.setContent("M12 4v12m0 0l-4-4m4 4l4-4M4 17v2a2 2 0 002 2h12a2 2 0 002-2v-2");
        depositSvg.setStyle("-fx-fill: transparent; -fx-stroke: #2563EB; -fx-stroke-width: 2; -fx-stroke-line-cap: round; -fx-stroke-line-join: round;");
        iconCircle.getChildren().add(depositSvg);

        VBox titleBox = new VBox(2);
        Label cardTitle = new Label("Nạp Tiền Vào Tài Khoản");
        cardTitle.getStyleClass().add("card-title");
        Label cardSub = new Label("Cộng số dư trực tiếp qua cổng thanh toán Facade");
        cardSub.getStyleClass().add("card-subtitle");
        titleBox.getChildren().addAll(cardTitle, cardSub);
        cardHeader.getChildren().addAll(iconCircle, titleBox);

        // Field 1: Chọn tài khoản
        VBox accBox = new VBox(6);
        Label lblAcc = new Label("Tài khoản thụ hưởng");
        lblAcc.getStyleClass().add("field-label");

        setupAccountComboBox(cbDepositAccount, lblDepositBalance, lblDepositStatusBadge);
        cbDepositAccount.getStyleClass().add("custom-combo-box");

        // Account Balance Pill
        HBox balPill = new HBox(10);
        balPill.setAlignment(Pos.CENTER_LEFT);
        balPill.getStyleClass().add("cash-account-pill");

        VBox balBox = new VBox(2);
        Label lblBalText = new Label("Số dư khả dụng hiện tại:");
        lblBalText.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        lblDepositBalance.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        balBox.getChildren().addAll(lblBalText, lblDepositBalance);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        balPill.getChildren().addAll(balBox, spacer, lblDepositStatusBadge);

        accBox.getChildren().addAll(lblAcc, cbDepositAccount, balPill);

        // Field 2: Số tiền cần nạp
        VBox amountBox = new VBox(6);
        Label lblAmt = new Label("Số tiền cần nạp (VND)");
        lblAmt.getStyleClass().add("field-label");

        HBox inputContainer = new HBox(8);
        inputContainer.setAlignment(Pos.CENTER_LEFT);
        inputContainer.getStyleClass().add("amount-input-box");

        Pattern digitPattern = Pattern.compile("\\d*");
        txtDepositAmount.setTextFormatter(new TextFormatter<>(change ->
                digitPattern.matcher(change.getControlNewText()).matches() ? change : null
        ));
        txtDepositAmount.setPromptText("0");
        txtDepositAmount.getStyleClass().add("amount-field-large");
        HBox.setHgrow(txtDepositAmount, Priority.ALWAYS);

        Label currencyLabel = new Label("VND");
        currencyLabel.getStyleClass().add("currency-badge-dark");
        inputContainer.getChildren().addAll(txtDepositAmount, currencyLabel);

        // Quick Amount Chips
        HBox quickChips = new HBox(6);
        quickChips.setAlignment(Pos.CENTER_LEFT);
        quickChips.getChildren().addAll(
                createChip("+100K", () -> addAmount(txtDepositAmount, 100_000)),
                createChip("+500K", () -> addAmount(txtDepositAmount, 500_000)),
                createChip("+1M", () -> addAmount(txtDepositAmount, 1_000_000)),
                createChip("+2M", () -> addAmount(txtDepositAmount, 2_000_000)),
                createChip("+5M", () -> addAmount(txtDepositAmount, 5_000_000))
        );

        amountBox.getChildren().addAll(lblAmt, inputContainer, quickChips);

        // Preview & Summary Box
        VBox summaryBox = new VBox(6);
        summaryBox.getStyleClass().add("cash-summary-box");

        HBox rowFee = createSummaryRow("Phí nạp tiền:", "0 VND (Miễn phí)", false);
        HBox rowNewBal = createSummaryRow("Số dư sau nạp dự kiến:", lblDepositEstimatedBal, true);

        lblDepositValidation.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700;");
        summaryBox.getChildren().addAll(rowFee, rowNewBal, lblDepositValidation);

        txtDepositAmount.textProperty().addListener((obs, oldVal, newVal) -> validateDeposit());
        cbDepositAccount.valueProperty().addListener((obs, oldVal, newVal) -> validateDeposit());

        // Action Button
        btnDeposit.getStyleClass().add("btn-save-template-primary");
        btnDeposit.setMaxWidth(Double.MAX_VALUE);
        btnDeposit.setOnAction(e -> handleDeposit());

        card.getChildren().addAll(cardHeader, accBox, amountBox, summaryBox, btnDeposit);
        return card;
    }

    private VBox buildWithdrawCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        // Card Header (Icon Circle + Title + Subtitle)
        HBox cardHeader = new HBox(12);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane();
        iconCircle.getStyleClass().add("icon-circle-blue");
        SVGPath withdrawSvg = new SVGPath();
        withdrawSvg.setContent("M12 20V8m0 0l-4 4m4-4l4 4M4 7V5a2 2 0 012-2h12a2 2 0 012 2v2");
        withdrawSvg.setStyle("-fx-fill: transparent; -fx-stroke: #2563EB; -fx-stroke-width: 2; -fx-stroke-line-cap: round; -fx-stroke-line-join: round;");
        iconCircle.getChildren().add(withdrawSvg);

        VBox titleBox = new VBox(2);
        Label cardTitle = new Label("Rút Tiền Mặt");
        cardTitle.getStyleClass().add("card-title");
        Label cardSub = new Label("Kiểm tra trạng thái State Pattern và tính phí Strategy");
        cardSub.getStyleClass().add("card-subtitle");
        titleBox.getChildren().addAll(cardTitle, cardSub);
        cardHeader.getChildren().addAll(iconCircle, titleBox);

        // Field 1: Chọn tài khoản
        VBox accBox = new VBox(6);
        Label lblAcc = new Label("Tài khoản trích tiền");
        lblAcc.getStyleClass().add("field-label");

        setupAccountComboBox(cbWithdrawAccount, lblWithdrawBalance, lblWithdrawStatusBadge);
        cbWithdrawAccount.getStyleClass().add("custom-combo-box");

        // Account Balance Pill
        HBox balPill = new HBox(10);
        balPill.setAlignment(Pos.CENTER_LEFT);
        balPill.getStyleClass().add("cash-account-pill");

        VBox balBox = new VBox(2);
        Label lblBalText = new Label("Số dư khả dụng hiện tại:");
        lblBalText.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        lblWithdrawBalance.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        balBox.getChildren().addAll(lblBalText, lblWithdrawBalance);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        balPill.getChildren().addAll(balBox, spacer, lblWithdrawStatusBadge);

        accBox.getChildren().addAll(lblAcc, cbWithdrawAccount, balPill);

        // Field 2: Số tiền cần rút
        VBox amountBox = new VBox(6);
        Label lblAmt = new Label("Số tiền cần rút (VND)");
        lblAmt.getStyleClass().add("field-label");

        HBox inputContainer = new HBox(8);
        inputContainer.setAlignment(Pos.CENTER_LEFT);
        inputContainer.getStyleClass().add("amount-input-box");

        Pattern digitPattern = Pattern.compile("\\d*");
        txtWithdrawAmount.setTextFormatter(new TextFormatter<>(change ->
                digitPattern.matcher(change.getControlNewText()).matches() ? change : null
        ));
        txtWithdrawAmount.setPromptText("0");
        txtWithdrawAmount.getStyleClass().add("amount-field-large");
        HBox.setHgrow(txtWithdrawAmount, Priority.ALWAYS);

        Label currencyLabel = new Label("VND");
        currencyLabel.getStyleClass().add("currency-badge-dark");
        inputContainer.getChildren().addAll(txtWithdrawAmount, currencyLabel);

        // Quick Amount Chips
        HBox quickChips = new HBox(6);
        quickChips.setAlignment(Pos.CENTER_LEFT);
        quickChips.getChildren().addAll(
                createChip("+100K", () -> addAmount(txtWithdrawAmount, 100_000)),
                createChip("+500K", () -> addAmount(txtWithdrawAmount, 500_000)),
                createChip("+1M", () -> addAmount(txtWithdrawAmount, 1_000_000)),
                createChip("+2M", () -> addAmount(txtWithdrawAmount, 2_000_000)),
                createChip("Tối đa", this::setWithdrawMax)
        );

        amountBox.getChildren().addAll(lblAmt, inputContainer, quickChips);

        // Preview & Summary Box
        VBox summaryBox = new VBox(6);
        summaryBox.getStyleClass().add("cash-summary-box");

        HBox rowFee = createSummaryRow("Phí rút tiền (Strategy):", lblWithdrawFeePreview, false);
        HBox rowTotal = createSummaryRow("Tổng tiền trích nợ:", lblWithdrawTotalDebit, false);
        HBox rowRem = createSummaryRow("Số dư còn lại dự kiến:", lblWithdrawRemainingBal, true);

        lblWithdrawValidation.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700;");
        summaryBox.getChildren().addAll(rowFee, rowTotal, rowRem, lblWithdrawValidation);

        txtWithdrawAmount.textProperty().addListener((obs, oldVal, newVal) -> validateWithdraw());
        cbWithdrawAccount.valueProperty().addListener((obs, oldVal, newVal) -> validateWithdraw());

        // Action Button
        btnWithdraw.getStyleClass().add("btn-save-template-primary");
        btnWithdraw.setMaxWidth(Double.MAX_VALUE);
        btnWithdraw.setOnAction(e -> handleWithdraw());

        card.getChildren().addAll(cardHeader, accBox, amountBox, summaryBox, btnWithdraw);
        return card;
    }

    private VBox buildRecentCashCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("icon-circle-blue");
        SVGPath histSvg = new SVGPath();
        histSvg.setContent("M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z");
        histSvg.setStyle("-fx-fill: transparent; -fx-stroke: #2563EB; -fx-stroke-width: 1.8; -fx-stroke-line-cap: round;");
        iconBox.getChildren().add(histSvg);

        VBox textCol = new VBox(2);
        Label cardTitle = new Label("Giao Dịch Tiền Mặt Gần Đây");
        cardTitle.getStyleClass().add("card-title");
        Label cardSub = new Label("Lịch sử nạp và rút tiền mặt được lưu trong sổ cái");
        cardSub.getStyleClass().add("card-subtitle");
        textCol.getChildren().addAll(cardTitle, cardSub);
        titleBox.getChildren().addAll(iconBox, textCol);

        card.getChildren().addAll(titleBox, boxRecentCashTx);
        return card;
    }

    private VBox buildPatternArchitectureCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("icon-circle-blue");
        SVGPath archSvg = new SVGPath();
        archSvg.setContent("M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z");
        archSvg.setStyle("-fx-fill: transparent; -fx-stroke: #2563EB; -fx-stroke-width: 1.8; -fx-stroke-line-cap: round;");
        iconBox.getChildren().add(archSvg);

        VBox textCol = new VBox(2);
        Label cardTitle = new Label("Cơ Chế Phối Hợp Design Patterns");
        cardTitle.getStyleClass().add("card-title");
        Label cardSub = new Label("Đảm bảo bảo mật tài khoản và chuẩn hóa giao dịch nghiệp vụ");
        cardSub.getStyleClass().add("card-subtitle");
        textCol.getChildren().addAll(cardTitle, cardSub);
        titleBox.getChildren().addAll(iconBox, textCol);

        VBox rulesBox = new VBox(8);

        rulesBox.getChildren().addAll(
                createRuleItem("01. State Pattern", "ActiveState cho phép rút tiền bình thường. Khi ở LockedState, lệnh rút tiền bị từ chối tức thì ngay tại hàm State.withdraw()."),
                createRuleItem("02. Strategy Pattern", "FeeStrategy tính phí linh hoạt (Standard: 0.1%, Premium: 0đ miễn phí, Savings: biểu phí bậc thang theo số tiền)."),
                createRuleItem("03. Facade & Observer", "BankingFacade gom debit, credit, logging trọn gói; Observer lập tức phát thông báo SMS/Email và cập nhật Dashboard.")
        );

        card.getChildren().addAll(titleBox, rulesBox);
        return card;
    }

    private VBox createRuleItem(String step, String desc) {
        VBox box = new VBox(3);
        box.getStyleClass().add("rule-item-box");

        Label lblStep = new Label(step);
        lblStep.setStyle("-fx-font-weight: 800; -fx-text-fill: #1D4ED8; -fx-font-size: 12px;");

        Label lblDesc = new Label(desc);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #475569; -fx-font-size: 11.5px; -fx-line-spacing: 2px;");

        box.getChildren().addAll(lblStep, lblDesc);
        return box;
    }

    private Button createChip(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("quick-chip-btn");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void addAmount(TextField field, long delta) {
        long current = 0;
        try {
            if (!field.getText().trim().isEmpty()) {
                current = Long.parseLong(field.getText().trim());
            }
        } catch (NumberFormatException ignored) { }
        field.setText(String.valueOf(current + delta));
    }

    private void setWithdrawMax() {
        Account acc = cbWithdrawAccount.getValue();
        if (acc != null) {
            long max = (long) Math.max(0, acc.getBalance());
            txtWithdrawAmount.setText(String.valueOf(max));
        }
    }

    private HBox createSummaryRow(String label, String value, boolean highlight) {
        Label lblVal = new Label(value);
        return createSummaryRow(label, lblVal, highlight);
    }

    private HBox createSummaryRow(String label, Label valueLabel, boolean highlight) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (highlight) {
            valueLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        } else {
            valueLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1D4ED8;");
        }

        row.getChildren().addAll(lbl, spacer, valueLabel);
        return row;
    }

    private void setupAccountComboBox(ComboBox<Account> cb, Label lblBalance, Label lblBadge) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getAccountNumber() + " - " + item.getOwnerName() + " (" + item.getType() + " · " + item.getStatus() + ")");
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
                    setText(item.getAccountNumber() + " - " + item.getOwnerName() + " (" + item.getType() + ")");
                }
            }
        });

        cb.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblBalance.setText(UiUtils.formatVnd(newVal.getBalance()));
                if (newVal.getStatus() == AccountStatus.LOCKED) {
                    lblBadge.setText("⛔ ĐÃ BỊ KHÓA");
                    lblBadge.getStyleClass().setAll("badge-status-locked");
                } else {
                    lblBadge.setText("● ĐANG HOẠT ĐỘNG");
                    lblBadge.getStyleClass().setAll("badge-status-active");
                }
            } else {
                lblBalance.setText("-");
                lblBadge.setText("● CHƯA CHỌN");
                lblBadge.getStyleClass().setAll("badge-status-active");
            }
        });
    }

    private void validateDeposit() {
        Account acc = cbDepositAccount.getValue();
        if (acc == null) {
            lblDepositValidation.setText("Vui lòng chọn tài khoản nhận tiền.");
            lblDepositValidation.setStyle("-fx-text-fill: #64748B;");
            lblDepositEstimatedBal.setText("0 VND");
            btnDeposit.setDisable(true);
            return;
        }

        String amtStr = txtDepositAmount.getText().trim();
        if (amtStr.isEmpty()) {
            lblDepositValidation.setText("Vui lòng nhập số tiền cần nạp.");
            lblDepositValidation.setStyle("-fx-text-fill: #64748B;");
            lblDepositEstimatedBal.setText(UiUtils.formatVnd(acc.getBalance()));
            btnDeposit.setDisable(true);
            return;
        }

        try {
            double amt = Double.parseDouble(amtStr);
            if (amt <= 0) {
                lblDepositValidation.setText("⚠️ Số tiền phải lớn hơn 0 VND.");
                lblDepositValidation.setStyle("-fx-text-fill: #DC2626;");
                lblDepositEstimatedBal.setText(UiUtils.formatVnd(acc.getBalance()));
                btnDeposit.setDisable(true);
            } else {
                double newBal = acc.getBalance() + amt;
                lblDepositEstimatedBal.setText(UiUtils.formatVnd(newBal));
                lblDepositValidation.setText("✔ Số tiền hợp lệ. Sẵn sàng nạp.");
                lblDepositValidation.setStyle("-fx-text-fill: #059669;");
                btnDeposit.setDisable(false);
            }
        } catch (NumberFormatException e) {
            lblDepositValidation.setText("⚠️ Số tiền không đúng định dạng.");
            lblDepositValidation.setStyle("-fx-text-fill: #DC2626;");
            btnDeposit.setDisable(true);
        }
    }

    private void validateWithdraw() {
        Account acc = cbWithdrawAccount.getValue();
        if (acc == null) {
            lblWithdrawValidation.setText("Vui lòng chọn tài khoản trích tiền.");
            lblWithdrawValidation.setStyle("-fx-text-fill: #64748B;");
            btnWithdraw.setDisable(true);
            return;
        }

        // Kiểm tra State Pattern: Bị khóa
        if (acc.getStatus() == AccountStatus.LOCKED) {
            lblWithdrawValidation.setText("⚠️ Tài khoản đang bị KHÓA (LockedState). Thao tác rút tiền bị chặn!");
            lblWithdrawValidation.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: 800;");
            lblWithdrawFeePreview.setText("0 VND");
            lblWithdrawTotalDebit.setText("0 VND");
            lblWithdrawRemainingBal.setText(UiUtils.formatVnd(acc.getBalance()));
            btnWithdraw.setDisable(true);
            return;
        }

        String amtStr = txtWithdrawAmount.getText().trim();
        if (amtStr.isEmpty()) {
            lblWithdrawFeePreview.setText("0 VND");
            lblWithdrawTotalDebit.setText("0 VND");
            lblWithdrawRemainingBal.setText(UiUtils.formatVnd(acc.getBalance()));
            lblWithdrawValidation.setText("Vui lòng nhập số tiền cần rút.");
            lblWithdrawValidation.setStyle("-fx-text-fill: #64748B;");
            btnWithdraw.setDisable(true);
            return;
        }

        try {
            double amt = Double.parseDouble(amtStr);
            if (amt <= 0) {
                lblWithdrawValidation.setText("⚠️ Số tiền phải lớn hơn 0 VND.");
                lblWithdrawValidation.setStyle("-fx-text-fill: #DC2626;");
                btnWithdraw.setDisable(true);
                return;
            }

            double fee = Money.nonNegative(acc.getFeeStrategy().calculateFee(amt));
            lblWithdrawFeePreview.setText(String.format("%s (%s)",
                    UiUtils.formatVnd(fee), acc.getFeeStrategy().getName()));

            double total = amt + fee;
            lblWithdrawTotalDebit.setText(UiUtils.formatVnd(total));

            if (acc.getBalance() < total) {
                lblWithdrawValidation.setText(String.format("⚠️ Số dư không đủ! Cần: %s (gồm phí), hiện có: %s",
                        UiUtils.formatVnd(total), UiUtils.formatVnd(acc.getBalance())));
                lblWithdrawValidation.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: 700;");
                lblWithdrawRemainingBal.setText("Không khả dụng");
                btnWithdraw.setDisable(true);
            } else {
                double remaining = acc.getBalance() - total;
                lblWithdrawRemainingBal.setText(UiUtils.formatVnd(remaining));
                lblWithdrawValidation.setText("✔ Hợp lệ. Đủ điều kiện thực hiện lệnh rút.");
                lblWithdrawValidation.setStyle("-fx-text-fill: #059669;");
                btnWithdraw.setDisable(false);
            }
        } catch (NumberFormatException e) {
            lblWithdrawValidation.setText("⚠️ Định dạng số tiền không hợp lệ.");
            lblWithdrawValidation.setStyle("-fx-text-fill: #DC2626;");
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
            renderRecentCashTx();
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
            renderRecentCashTx();
        } catch (Exception e) {
            String error = UiUtils.humanizeError(e);
            ToastNotification.showError(error);
            UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể rút tiền", null, error);
        }
    }

    private void renderRecentCashTx() {
        boxRecentCashTx.getChildren().clear();

        List<Transaction> cashTx = ctx.getTransactions().stream()
                .filter(t -> "CASH".equalsIgnoreCase(t.getFromAccountNumber())
                        || "CASH".equalsIgnoreCase(t.getToAccountNumber())
                        || (t.getDescription() != null && (t.getDescription().toLowerCase().contains("nạp")
                        || t.getDescription().toLowerCase().contains("rút")
                        || t.getDescription().toLowerCase().contains("deposit")
                        || t.getDescription().toLowerCase().contains("withdraw"))))
                .limit(4)
                .toList();

        if (cashTx.isEmpty()) {
            VBox emptyBox = new VBox(6);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 24; -fx-background-color: #F8FAFC; -fx-background-radius: 10px; -fx-border-color: #E2E8F0; -fx-border-radius: 10px;");
            Label lblEmpty = new Label("Chưa có giao dịch nạp / rút tiền mặt nào.");
            lblEmpty.setStyle("-fx-text-fill: #64748B; -fx-font-weight: 700; -fx-font-size: 12px;");
            emptyBox.getChildren().add(lblEmpty);
            boxRecentCashTx.getChildren().add(emptyBox);
            return;
        }

        for (Transaction tx : cashTx) {
            HBox item = new HBox(12);
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("cash-tx-item");

            boolean isDeposit = "CASH".equalsIgnoreCase(tx.getFromAccountNumber())
                    || (tx.getDescription() != null && tx.getDescription().toLowerCase().contains("nạp"));

            StackPane iconCircle = new StackPane();
            iconCircle.setStyle(isDeposit
                    ? "-fx-background-color: rgba(16, 185, 129, 0.12); -fx-background-radius: 50%; -fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;"
                    : "-fx-background-color: rgba(37, 99, 235, 0.12); -fx-background-radius: 50%; -fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;");

            SVGPath iconSvg = new SVGPath();
            iconSvg.setContent(isDeposit ? "M12 4v16m8-8H4" : "M20 12H4");
            iconSvg.setStyle("-fx-fill: transparent; -fx-stroke: " + (isDeposit ? "#059669" : "#2563EB") + "; -fx-stroke-width: 2.2; -fx-stroke-line-cap: round;");
            iconCircle.getChildren().add(iconSvg);

            VBox infoCol = new VBox(2);
            HBox.setHgrow(infoCol, Priority.ALWAYS);

            Label lblDesc = new Label(tx.getDescription() != null ? tx.getDescription() : (isDeposit ? "Nạp tiền" : "Rút tiền"));
            lblDesc.setStyle("-fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-size: 12.5px;");

            String timeStr = tx.getTimestamp() != null ? tx.getTimestamp().format(TIME_FMT) : "Vừa xong";
            String accStr = isDeposit ? ("Đích: " + tx.getToAccountNumber()) : ("Nguồn: " + tx.getFromAccountNumber());
            Label lblMeta = new Label(accStr + " · " + timeStr);
            lblMeta.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

            infoCol.getChildren().addAll(lblDesc, lblMeta);

            Label lblAmt = new Label((isDeposit ? "+ " : "- ") + UiUtils.formatVnd(tx.getAmount()));
            lblAmt.setStyle(isDeposit
                    ? "-fx-font-weight: 800; -fx-text-fill: #059669; -fx-font-size: 13.5px;"
                    : "-fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-size: 13.5px;");

            item.getChildren().addAll(iconCircle, infoCol, lblAmt);
            boxRecentCashTx.getChildren().add(item);
        }
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
        renderRecentCashTx();
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

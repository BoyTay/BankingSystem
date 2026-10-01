package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Màn hình Mở & Quản lý tài khoản (Account Creation & State Management).
 * Thiết kế chuẩn theo Stitch Design System & Figma Brief:
 * - 60% Form khởi tạo với Builder Pattern & Strategy Pattern
 * - 40% Visual Code Inspector trực quan hóa method chaining trong thời gian thực
 * - Bảng quản trị trạng thái tài khoản (State Pattern) với chức năng tìm kiếm & chuyển đổi Active/Locked tức thì
 */
public class AccountView extends VBox {

    private final UIContext ctx = UIContext.getInstance();

    // Form inputs
    private final TextField txtOwnerName = new TextField();
    private final ToggleGroup typeGroup = new ToggleGroup();
    private final RadioButton rbStandard = new RadioButton("STANDARD");
    private final RadioButton rbSavings = new RadioButton("SAVINGS");
    private final RadioButton rbPremium = new RadioButton("PREMIUM");
    private final TextField txtInitialDeposit = new TextField("1000000");
    private final Label lblBalancePreview = new Label("1,000,000 VND");

    // Dynamic Strategy Info Box
    private final Label lblStrategyBadge = new Label("StandardFeeStrategy");
    private final Label lblStrategyDesc = new Label();

    // Dynamic Result Banner
    private final VBox bannerResult = new VBox(6);
    private final Label lblResultTitle = new Label();
    private final Label lblResultDetail = new Label();
    private final Label lblResultMeta = new Label();

    // Visual Code Inspector Elements
    private final TextFlow codeFlow = new TextFlow();

    // Table & Filter
    private final TableView<Account> accountTable = new TableView<>();
    private final TextField txtTableSearch = new TextField();
    private FilteredList<Account> filteredAccounts;

    public AccountView() {
        setSpacing(18);
        getStyleClass().add("content-pane");

        buildHeader();

        // TOP SECTION: 60% Form + 40% Visual Inspector
        HBox topSection = new HBox(20);
        topSection.setAlignment(Pos.TOP_LEFT);

        VBox formCard = buildOpenAccountForm();
        VBox inspectorCard = buildCodeInspectorCard();

        formCard.setPrefWidth(600);
        formCard.setMinWidth(480);
        HBox.setHgrow(formCard, Priority.ALWAYS);

        inspectorCard.setPrefWidth(460);
        inspectorCard.setMinWidth(380);

        topSection.getChildren().addAll(formCard, inspectorCard);

        // BOTTOM SECTION: Table of accounts with State management
        VBox tableCard = buildAccountTableSection();
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        getChildren().addAll(topSection, tableCard);

        ctx.addDataChangeListener(this::refresh);
        refresh();
    }

    private void buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        HBox tagBox = new HBox(6);
        tagBox.setAlignment(Pos.CENTER_LEFT);
        tagBox.getStyleClass().add("header-badge-tag");
        tagBox.setMaxWidth(Double.NEGATIVE_INFINITY);
        Circle tagDot = new Circle(3.0);
        tagDot.getStyleClass().add("header-badge-tag-dot");
        Label tagText = new Label("THIẾT KẾ MẪU TẠO LẬP · BUILDER & STRATEGY");
        tagText.getStyleClass().add("header-badge-tag-text");
        tagBox.getChildren().addAll(tagDot, tagText);

        Label title = new Label("Mở & quản lý tài khoản");
        title.getStyleClass().add("header-greeting");

        Label subtitle = new Label("Khởi tạo tài khoản với Builder Pattern & áp dụng chiến lược phí linh hoạt (Strategy Pattern)");
        subtitle.getStyleClass().add("header-subtitle");

        titleBox.getChildren().addAll(tagBox, title, subtitle);

        // Right Pattern Badges
        HBox patternBadges = new HBox(8);
        patternBadges.setAlignment(Pos.CENTER_RIGHT);
        patternBadges.getChildren().addAll(
                UiUtils.createPatternBadge("Builder (Account.Builder)", "creational"),
                UiUtils.createPatternBadge("Strategy (FeeStrategy)", "behavioral"),
                UiUtils.createPatternBadge("State (Active / Locked)", "behavioral")
        );

        header.getChildren().addAll(titleBox, patternBadges);
        getChildren().add(header);
    }

    private VBox buildOpenAccountForm() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20;");

        // Section Title
        HBox cardTitleBox = new HBox(10);
        cardTitleBox.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color: rgba(0, 196, 118, 0.15); -fx-background-radius: 8px; -fx-min-width: 36px; -fx-min-height: 36px; -fx-max-width: 36px; -fx-max-height: 36px;");
        SVGPath builderIcon = new SVGPath();
        builderIcon.setContent("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z");
        builderIcon.setStyle("-fx-fill: #008751; -fx-scale-x: 0.9; -fx-scale-y: 0.9;");
        iconBox.getChildren().add(builderIcon);

        VBox titleArea = new VBox(2);
        Label title = new Label("Mở tài khoản mới (Builder Form)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitle = new Label("Thiết lập tham số và tự động gắn kết FeeStrategy tương ứng");
        subtitle.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
        titleArea.getChildren().addAll(title, subtitle);

        cardTitleBox.getChildren().addAll(iconBox, titleArea);

        // Inputs
        VBox form = new VBox(12);

        // 1. Owner Name
        VBox grpOwner = new VBox(4);
        Label lblOwner = new Label("Tên chủ sở hữu tài khoản *");
        lblOwner.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        txtOwnerName.setPromptText("Ví dụ: LÊ HOÀNG LONG");
        txtOwnerName.getStyleClass().add("form-input");
        txtOwnerName.textProperty().addListener((obs, oldV, newV) -> updateCodePreview());
        grpOwner.getChildren().addAll(lblOwner, txtOwnerName);

        // 2. Account Type Selector
        VBox grpType = new VBox(6);
        Label lblType = new Label("Gói tài khoản & Hạng dịch vụ *");
        lblType.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");

        rbStandard.setToggleGroup(typeGroup);
        rbSavings.setToggleGroup(typeGroup);
        rbPremium.setToggleGroup(typeGroup);
        rbStandard.setSelected(true);

        typeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            updateStrategyInfo();
            updateCodePreview();
        });

        HBox typeOptions = new HBox(8);
        typeOptions.getChildren().addAll(
                buildTypeChoice(rbStandard, "STANDARD", "Cơ bản", "#1D4ED8", "#DBEAFE"),
                buildTypeChoice(rbSavings, "SAVINGS", "Tiết kiệm", "#B45309", "#FEF3C7"),
                buildTypeChoice(rbPremium, "PREMIUM", "VIP", "#065F46", "#D1FAE5")
        );
        grpType.getChildren().addAll(lblType, typeOptions);

        // 3. Strategy Dynamic Info Box
        VBox strategyBox = new VBox(6);
        strategyBox.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 12;");

        HBox strategyHeader = new HBox(8);
        strategyHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblStrategyTitle = new Label("Chiến lược phí được gán tự động (Strategy Pattern):");
        lblStrategyTitle.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        Region spStrategy = new Region();
        HBox.setHgrow(spStrategy, Priority.ALWAYS);
        lblStrategyBadge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 6px; -fx-font-family: 'JetBrains Mono', monospace;");
        strategyHeader.getChildren().addAll(lblStrategyTitle, spStrategy, lblStrategyBadge);

        lblStrategyDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #1E293B; -fx-line-spacing: 2px;");
        lblStrategyDesc.setWrapText(true);
        strategyBox.getChildren().addAll(strategyHeader, lblStrategyDesc);
        updateStrategyInfo();

        // 4. Initial Deposit
        VBox grpDeposit = new VBox(4);
        HBox depositLabelRow = new HBox();
        Label lblDeposit = new Label("Số tiền nạp ban đầu (VND) *");
        lblDeposit.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        Region spDeposit = new Region();
        HBox.setHgrow(spDeposit, Priority.ALWAYS);
        lblBalancePreview.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #059669; -fx-font-family: 'JetBrains Mono', monospace;");
        depositLabelRow.getChildren().addAll(lblDeposit, spDeposit, lblBalancePreview);

        txtInitialDeposit.getStyleClass().add("form-input");
        txtInitialDeposit.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) {
                txtInitialDeposit.setText(newV.replaceAll("[^\\d]", ""));
                return;
            }
            try {
                double val = txtInitialDeposit.getText().isEmpty() ? 0 : Double.parseDouble(txtInitialDeposit.getText());
                lblBalancePreview.setText(UiUtils.formatVnd(val));
            } catch (Exception ignored) {
                lblBalancePreview.setText("0 VND");
            }
            updateCodePreview();
        });
        grpDeposit.getChildren().addAll(depositLabelRow, txtInitialDeposit);

        // Action Button
        Button btnCreate = new Button("✓ Khởi tạo tài khoản với Builder Pattern");
        btnCreate.getStyleClass().add("btn-primary");
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        btnCreate.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-padding: 10 16;");
        btnCreate.setOnAction(e -> handleOpenAccount());

        // Result Banner
        buildResultBanner();

        form.getChildren().addAll(grpOwner, grpType, strategyBox, grpDeposit, btnCreate, bannerResult);
        card.getChildren().addAll(cardTitleBox, form);
        return card;
    }

    private HBox buildTypeChoice(RadioButton rb, String typeName, String sub, String color, String bg) {
        HBox card = new HBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 10; -fx-cursor: hand;");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setOnMouseClicked(e -> rb.setSelected(true));

        Label badge = new Label(typeName);
        badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 6px;");

        Label lblSub = new Label("(" + sub + ")");
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        card.getChildren().addAll(rb, badge, lblSub);
        return card;
    }

    private void updateStrategyInfo() {
        AccountType type = getSelectedAccountType();
        switch (type) {
            case STANDARD -> {
                lblStrategyBadge.setText("StandardFeeStrategy");
                lblStrategyBadge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 6px; -fx-font-family: 'JetBrains Mono', monospace;");
                lblStrategyDesc.setText("Thu phí cố định 0.1% trên mỗi giao dịch chuyển tiền (tối thiểu 1,000 VND).");
            }
            case SAVINGS -> {
                lblStrategyBadge.setText("TieredFeeStrategy");
                lblStrategyBadge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 6px; -fx-font-family: 'JetBrains Mono', monospace;");
                lblStrategyDesc.setText("Phí bậc thang theo số tiền giao dịch: ≤ 1M: 0.1% | ≤ 10M: 0.05% | > 10M: 0.02%. Tối ưu cho người gửi tiết kiệm.");
            }
            case PREMIUM -> {
                lblStrategyBadge.setText("PremiumFeeStrategy");
                lblStrategyBadge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 6px; -fx-font-family: 'JetBrains Mono', monospace;");
                lblStrategyDesc.setText("Đặc quyền VIP: Miễn phí 100% mọi giao dịch chuyển tiền và dịch vụ ngân hàng trực tuyến.");
            }
        }
    }

    private AccountType getSelectedAccountType() {
        if (rbPremium.isSelected()) return AccountType.PREMIUM;
        if (rbSavings.isSelected()) return AccountType.SAVINGS;
        return AccountType.STANDARD;
    }

    private void buildResultBanner() {
        bannerResult.getStyleClass().add("result-banner-success");
        bannerResult.setVisible(false);
        bannerResult.setManaged(false);

        lblResultTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #065F46;");
        lblResultDetail.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #047857;");
        lblResultDetail.setWrapText(true);
        lblResultMeta.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #059669; -fx-font-family: 'JetBrains Mono', monospace;");

        bannerResult.getChildren().addAll(lblResultTitle, lblResultDetail, lblResultMeta);
    }

    private VBox buildCodeInspectorCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("terminal-surface");

        // Title Bar with macOS dots
        HBox titleBar = new HBox(8);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getStyleClass().add("terminal-title-bar");

        HBox dots = new HBox(5);
        Circle dotRed = new Circle(4.5);
        dotRed.getStyleClass().add("terminal-dot-red");
        Circle dotYellow = new Circle(4.5);
        dotYellow.getStyleClass().add("terminal-dot-yellow");
        Circle dotGreen = new Circle(4.5);
        dotGreen.getStyleClass().add("terminal-dot-green");
        dots.getChildren().addAll(dotRed, dotYellow, dotGreen);

        Label lblFile = new Label("AccountFactoryInspector.java");
        lblFile.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox liveTag = new HBox(4);
        liveTag.setAlignment(Pos.CENTER_RIGHT);
        Circle liveDot = new Circle(3, Color.web("#40E18F"));
        Label lblLive = new Label("LIVE REFLECTION");
        lblLive.setStyle("-fx-text-fill: #40E18F; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 9.5px; -fx-font-weight: 800;");
        liveTag.getChildren().addAll(liveDot, lblLive);

        titleBar.getChildren().addAll(dots, lblFile, sp, liveTag);

        Label lblHint = new Label("Minh họa lời gọi Builder & Strategy qua Form — Giá trị thực được cấp khi tạo");
        lblHint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        // Code Area
        ScrollPane codeScroll = new ScrollPane(codeFlow);
        codeScroll.setFitToWidth(true);
        codeScroll.setStyle("-fx-background: #0F172A; -fx-background-color: #0F172A; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        codeScroll.setPrefHeight(230);
        VBox.setVgrow(codeScroll, Priority.ALWAYS);

        codeFlow.setStyle("-fx-background-color: #0F172A; -fx-padding: 10;");

        // Telemetry Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label lblHeap = new Label("💾 Heap Allocation: OK");
        lblHeap.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 10px;");
        Region spF = new Region();
        HBox.setHgrow(spF, Priority.ALWAYS);
        Label lblHash = new Label("Hashcode: 0x7F4A2C");
        lblHash.setStyle("-fx-text-fill: #40E18F; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 10px; -fx-font-weight: 700;");
        footer.getChildren().addAll(lblHeap, spF, lblHash);

        card.getChildren().addAll(titleBar, lblHint, codeScroll, footer);
        updateCodePreview();
        return card;
    }

    private void updateCodePreview() {
        codeFlow.getChildren().clear();

        String name = txtOwnerName.getText().trim().isEmpty() ? "NGUYỄN VĂN A" : txtOwnerName.getText().trim();
        AccountType type = getSelectedAccountType();
        String stratClass = switch (type) {
            case PREMIUM -> "PremiumFeeStrategy()";
            case SAVINGS -> "TieredFeeStrategy()";
            case STANDARD -> "StandardFeeStrategy()";
        };
        String depositVal = txtInitialDeposit.getText().isEmpty() ? "0.0" : txtInitialDeposit.getText() + ".0";

        addCodeText("// 1. Áp dụng Strategy Pattern dựa trên gói tài khoản\n", "#64748B");
        addCodeText("FeeStrategy ", "#B7C6EB");
        addCodeText("strategy = ", "#E2E8F0");
        addCodeText("new ", "#F59E0B");
        addCodeText(stratClass + ";\n\n", "#40E18F");

        addCodeText("// 2. Khởi tạo đối tượng qua Builder lồng (Method Chaining)\n", "#64748B");
        addCodeText("Account ", "#B7C6EB");
        addCodeText("account = ", "#E2E8F0");
        addCodeText("new ", "#F59E0B");
        addCodeText("Account.Builder", "#38BDF8");
        addCodeText("(\"ACC####\", \"", "#E2E8F0");
        addCodeText(name, "#63FEA9");
        addCodeText("\")\n", "#E2E8F0");

        addCodeText("    .type(", "#E2E8F0");
        addCodeText("AccountType." + type.name(), "#CBDAFF");
        addCodeText(")\n", "#E2E8F0");

        addCodeText("    .balance(", "#E2E8F0");
        addCodeText(depositVal, "#40E18F");
        addCodeText(")\n", "#E2E8F0");

        addCodeText("    .status(", "#E2E8F0");
        addCodeText("AccountStatus.ACTIVE", "#63FEA9");
        addCodeText(")\n", "#E2E8F0");

        addCodeText("    .feeStrategy(", "#E2E8F0");
        addCodeText("strategy", "#B7C6EB");
        addCodeText(")\n", "#E2E8F0");

        addCodeText("    .state(", "#E2E8F0");
        addCodeText("new ActiveState()", "#CBDAFF");
        addCodeText(")\n", "#E2E8F0");

        addCodeText("    .", "#E2E8F0");
        addCodeText("build", "#F59E0B");
        addCodeText("();\n\n", "#E2E8F0");

        addCodeText("// 3. Ghi nhận vào SQLite qua Ledger Service\n", "#64748B");
        addCodeText("accountService.save(account);", "#38BDF8");
    }

    private void addCodeText(String str, String colorHex) {
        Text t = new Text(str);
        t.setStyle("-fx-fill: " + colorHex + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
        codeFlow.getChildren().add(t);
    }

    private VBox buildAccountTableSection() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20;");

        HBox tableHeader = new HBox(12);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        VBox titleArea = new VBox(2);
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("Danh sách tài khoản & Quản lý trạng thái");
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label lblStateTag = new Label("State Pattern");
        lblStateTag.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 6; -fx-background-radius: 10px;");
        titleRow.getChildren().addAll(lblTitle, lblStateTag);

        Label lblSub = new Label("Mô hình hóa các chuyển đổi trạng thái (ActiveState ↔ LockedState) bảo vệ tính toàn vẹn giao dịch");
        lblSub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
        titleArea.getChildren().addAll(titleRow, lblSub);

        // Search Field
        txtTableSearch.setPromptText("🔍 Tìm theo mã hoặc tên...");
        txtTableSearch.setStyle("-fx-pref-width: 220px; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-padding: 5 10;");

        tableHeader.getChildren().addAll(titleArea, txtTableSearch);

        // Table Columns
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Account, String> colAccNo = new TableColumn<>("SỐ TÀI KHOẢN");
        colAccNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAccountNumber()));
        colAccNo.setPrefWidth(125);
        colAccNo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-size: 12px;");
                }
            }
        });

        TableColumn<Account, String> colOwner = new TableColumn<>("CHỦ TÀI KHOẢN");
        colOwner.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOwnerName()));
        colOwner.setPrefWidth(160);
        colOwner.setStyle("-fx-font-weight: 700; -fx-text-fill: #1E293B;");

        TableColumn<Account, String> colType = new TableColumn<>("LOẠI GÓI");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().name()));
        colType.setPrefWidth(100);
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    if ("PREMIUM".equals(item)) {
                        badge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-font-size: 10.5px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 8px;");
                    } else if ("SAVINGS".equals(item)) {
                        badge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-font-size: 10.5px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 8px;");
                    } else {
                        badge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-font-size: 10.5px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 8px;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Account, String> colBalance = new TableColumn<>("SỐ DƯ KHẢ DỤNG");
        colBalance.setCellValueFactory(d -> new SimpleStringProperty(UiUtils.formatVnd(d.getValue().getBalance())));
        colBalance.setPrefWidth(140);
        colBalance.setStyle("-fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-family: 'JetBrains Mono', monospace;");

        TableColumn<Account, String> colStrategy = new TableColumn<>("CHIẾN LƯỢC PHÍ");
        colStrategy.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFeeStrategy().getName()));
        colStrategy.setPrefWidth(150);
        colStrategy.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #475569;");

        TableColumn<Account, String> colStatus = new TableColumn<>("TRẠNG THÁI");
        colStatus.setPrefWidth(130);
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStatus() == AccountStatus.ACTIVE ? "Đang hoạt động" : "Đã khóa"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    boolean isActive = "Đang hoạt động".equals(item);
                    HBox box = new HBox(5);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Circle dot = new Circle(3, isActive ? Color.web("#10B981") : Color.web("#EF4444"));
                    Label lbl = new Label(isActive ? "● Hoạt động" : "🔒 Đã khóa");
                    lbl.setStyle(isActive ? "-fx-text-fill: #059669; -fx-font-weight: 700; -fx-font-size: 11px;"
                            : "-fx-text-fill: #DC2626; -fx-font-weight: 700; -fx-font-size: 11px;");
                    box.getChildren().addAll(dot, lbl);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        TableColumn<Account, Void> colAction = new TableColumn<>("THAO TÁC STATE");
        colAction.setPrefWidth(140);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnToggle = new Button();

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Account acc = getTableRow().getItem();
                if (acc.getStatus() == AccountStatus.ACTIVE) {
                    btnToggle.setText("🔒 Khóa TK");
                    btnToggle.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-cursor: hand;");
                    btnToggle.setOnAction(e -> {
                        try {
                            ctx.getAccountService().lockAccount(acc.getAccountNumber());
                            ctx.logCustomEvent("State Pattern", "Tài khoản " + acc.getAccountNumber() + " chuyển sang LockedState.");
                            ctx.notifyDataChanged();
                            ToastNotification.showWarning("Đã khóa tài khoản " + acc.getAccountNumber() + " (LockedState)");
                        } catch (Exception ex) {
                            ToastNotification.showError(UiUtils.humanizeError(ex));
                        }
                    });
                } else {
                    btnToggle.setText("🔓 Mở khóa");
                    btnToggle.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-cursor: hand;");
                    btnToggle.setOnAction(e -> {
                        try {
                            ctx.getAccountService().unlockAccount(acc.getAccountNumber());
                            ctx.logCustomEvent("State Pattern", "Tài khoản " + acc.getAccountNumber() + " mở khóa (ActiveState).");
                            ctx.notifyDataChanged();
                            ToastNotification.showSuccess("Đã mở khóa tài khoản " + acc.getAccountNumber() + " (ActiveState)");
                        } catch (Exception ex) {
                            ToastNotification.showError(UiUtils.humanizeError(ex));
                        }
                    });
                }
                setGraphic(btnToggle);
            }
        });

        accountTable.getColumns().setAll(colAccNo, colOwner, colType, colBalance, colStrategy, colStatus, colAction);
        accountTable.setPrefHeight(240);

        // Bind filter
        filteredAccounts = new FilteredList<>(ctx.getAccounts(), p -> true);
        txtTableSearch.textProperty().addListener((obs, oldV, newV) -> {
            filteredAccounts.setPredicate(acc -> {
                if (newV == null || newV.isBlank()) return true;
                String lower = newV.toLowerCase();
                return acc.getAccountNumber().toLowerCase().contains(lower) || acc.getOwnerName().toLowerCase().contains(lower);
            });
        });
        accountTable.setItems(filteredAccounts);

        card.getChildren().addAll(tableHeader, accountTable);
        return card;
    }

    private void handleOpenAccount() {
        String owner = txtOwnerName.getText().trim();
        if (owner.isEmpty()) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Chưa nhập tên", "Vui lòng nhập họ tên chủ tài khoản.");
            return;
        }

        double initialDeposit = 0;
        try {
            initialDeposit = Money.nonNegative(Double.parseDouble(txtInitialDeposit.getText().trim()));
        } catch (IllegalArgumentException e) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Số tiền không hợp lệ", "Vui lòng nhập một số dương hợp lệ.");
            return;
        }

        AccountType type = getSelectedAccountType();
        Account created;
        try {
            created = ctx.getAccountService().openAccount(owner, type);
            ctx.attachUiObserver(created);
            if (initialDeposit > 0) {
                ctx.getFacade().deposit(created.getAccountNumber(), initialDeposit);
            }
            ctx.notifyDataChanged();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            ctx.notifyDataChanged();
            UiUtils.showAlert(Alert.AlertType.ERROR, "Không thể mở tài khoản", null, exception.getMessage());
            return;
        }

        // Show result banner
        bannerResult.setVisible(true);
        bannerResult.setManaged(true);
        lblResultTitle.setText("✓ Khởi tạo thành công tài khoản " + created.getAccountNumber() + "!");
        lblResultDetail.setText("Chủ tài khoản: " + created.getOwnerName() + " | Gói: " + created.getType() + " | Số dư: " + UiUtils.formatVnd(created.getBalance()));
        lblResultMeta.setText("Chiến lược phí: " + created.getFeeStrategy().getName() + " · State: ActiveState");

        ToastNotification.showSuccess("Đã mở tài khoản " + created.getAccountNumber() + " với Builder Pattern!");

        // Select and scroll to new row in table
        accountTable.getSelectionModel().select(created);
        accountTable.scrollTo(created);

        txtOwnerName.clear();
        txtInitialDeposit.setText("1000000");
    }

    public void refresh() {
        accountTable.refresh();
        updateCodePreview();
    }
}

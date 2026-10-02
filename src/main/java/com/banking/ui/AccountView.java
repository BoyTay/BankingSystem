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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Account opening and status management, with optional pattern details for teaching. */
public class AccountView extends VBox {

    private final UIContext ctx = UIContext.getInstance();

    // Form inputs
    private final TextField txtOwnerName = new TextField();
    private final ToggleGroup typeGroup = new ToggleGroup();
    private final RadioButton rbStandard = new RadioButton();
    private final RadioButton rbSavings = new RadioButton();
    private final RadioButton rbPremium = new RadioButton();

    private HBox cardStandardTile;
    private HBox cardSavingsTile;
    private HBox cardPremiumTile;

    private final TextField txtInitialDeposit = new TextField("0");
    private final Label lblBalancePreview = new Label("0 VND");

    // Dynamic Strategy Info Box + Smart Tier Calculator
    private final Label lblStrategyBadge = new Label("StandardFeeStrategy");
    private final Label lblStrategyDesc = new Label();
    // Action button
    private final Button btnCreate = new Button("Mở tài khoản");
    private final Button btnTechnicalDetails = new Button("Xem cách hệ thống hoạt động");
    private VBox inspectorCard;

    // Dynamic Result Banner
    private final VBox bannerResult = new VBox(6);
    private final Label lblResultTitle = new Label();
    private final Label lblResultDetail = new Label();
    private final Label lblResultMeta = new Label();
    private final Button btnCreateAnother = new Button("Mở tài khoản khác");

    // Pattern Activity Log UI Elements (Zero Truncation)
    private VBox step1Box;
    private VBox step2Box;
    private VBox step3Box;
    private VBox step4Box;
    private VBox step5Box;

    private final Label lblStep1StrategyValue = new Label("StandardFeeStrategy (STANDARD)");
    private final Label lblStep1StrategyDesc = new Label("Phí chuyển tiền: 0,1% số tiền giao dịch");
    private final Label lblStep2TypeValue = new Label("AccountType.STANDARD");
    private final Label lblStep2OwnerValue = new Label("\"NGUYỄN VĂN A\"");
    private final Label lblStep3BuiltValue = new Label("Chưa tạo");
    private final Label lblStep4StoredValue = new Label("Chưa lưu");
    private final Label lblStep5DepositValue = new Label("Không nạp ban đầu");
    private final Label[] stepIcons = {new Label("○"), new Label("○"), new Label("○"), new Label("○"), new Label("○")};

    // State Transition log inside Activity Log
    private final VBox boxStateTransition = new VBox(3);
    private final Label lblStateTransitionText = new Label("Chưa có chuyển đổi trạng thái mới");

    // Segmented Inspector Views (Timeline vs Code Inspector)
    private VBox paneTimeline;
    private ScrollPane paneCodeScroll;
    private Button btnTabTimeline;
    private Button btnTabCode;
    private final TextFlow codeFlow = new TextFlow();

    // Table & Filter
    private final TableView<Account> accountTable = new TableView<>();
    private final TextField txtTableSearch = new TextField();
    private FilteredList<Account> filteredAccounts;
    private Account lastCreatedAccount = null;

    // Quick Stats Bar Elements
    private final Label lblStatTotalAccounts = new Label("0");
    private final Label lblStatActiveAccounts = new Label("0");
    private final Label lblStatLockedAccounts = new Label("0");
    private final Label lblStatTotalDeposits = new Label("0 VND");

    public AccountView() {
        setSpacing(16);
        getStyleClass().add("content-pane");

        buildHeader();

        // Keep the banking task first; technical details are available on demand.
        VBox topSection = new VBox(16);
        topSection.setAlignment(Pos.TOP_LEFT);

        VBox formCard = buildOpenAccountForm();
        inspectorCard = buildPatternActivityInspectorCard();
        inspectorCard.setId("technicalDetailsPanel");
        formCard.setMaxWidth(760);
        inspectorCard.setMaxWidth(760);
        inspectorCard.setVisible(false);
        inspectorCard.setManaged(false);

        topSection.getChildren().addAll(formCard, inspectorCard);

        // BOTTOM SECTION: Quick Stats + Table of accounts with State management
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

        Label title = new Label("Mở & quản lý tài khoản");
        title.getStyleClass().add("header-greeting");

        Label subtitle = new Label("Chọn gói, nhập thông tin và xem tài khoản sau khi mở.");
        subtitle.getStyleClass().add("header-subtitle");

        titleBox.getChildren().addAll(title, subtitle);

        btnTechnicalDetails.getStyleClass().add("btn-pattern-info");
        btnTechnicalDetails.setStyle("-fx-font-size: 13px; -fx-padding: 8px 12px;");
        btnTechnicalDetails.setId("technicalDetailsToggle");
        btnTechnicalDetails.setOnAction(e -> {
            boolean expanded = !inspectorCard.isVisible();
            inspectorCard.setVisible(expanded);
            inspectorCard.setManaged(expanded);
            btnTechnicalDetails.setText(expanded ? "Ẩn cách hệ thống hoạt động" : "Xem cách hệ thống hoạt động");
        });

        header.getChildren().addAll(titleBox, btnTechnicalDetails);
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
        Label title = new Label("Mở tài khoản mới");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitle = new Label("Chọn gói phù hợp và nhập thông tin chủ tài khoản.");
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
        txtOwnerName.setId("openAccountOwner");
        txtOwnerName.getStyleClass().add("form-input");
        txtOwnerName.textProperty().addListener((obs, oldV, newV) -> {
            resetPreviewStatus();
            updateActivityLog();
            updateCodePreview();
        });
        grpOwner.getChildren().addAll(lblOwner, txtOwnerName);

        // 2. Account Type Selector (Card Tiles - Khắc phục hoàn toàn lỗi tràn chữ)
        VBox grpType = new VBox(6);
        Label lblType = new Label("Gói tài khoản *");
        lblType.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");

        rbStandard.setToggleGroup(typeGroup);
        rbSavings.setToggleGroup(typeGroup);
        rbPremium.setToggleGroup(typeGroup);

        cardStandardTile = buildTypeChoiceCard(rbStandard, "STANDARD", "Cơ bản", "Phí chuyển 0,1%", "#1D4ED8", "#DBEAFE");
        cardSavingsTile = buildTypeChoiceCard(rbSavings, "SAVINGS", "Tiết kiệm", "Phí theo mức chuyển", "#B45309", "#FEF3C7");
        cardPremiumTile = buildTypeChoiceCard(rbPremium, "PREMIUM", "Ưu tiên", "Miễn phí chuyển", "#065F46", "#D1FAE5");

        rbStandard.setSelected(true);
        updateCardTileStyles();

        typeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            resetPreviewStatus();
            updateCardTileStyles();
            updateStrategyInfo();
            updateActivityLog();
            updateCodePreview();
        });

        HBox typeOptions = new HBox(10);
        typeOptions.getChildren().addAll(cardStandardTile, cardSavingsTile, cardPremiumTile);
        grpType.getChildren().addAll(lblType, typeOptions);

        // 3. Strategy Dynamic Info Box + Smart Tier Calculator
        VBox strategyBox = new VBox(6);
        strategyBox.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 12;");

        HBox strategyHeader = new HBox(8);
        strategyHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblStrategyTitle = new Label("Phí chuyển tiền của gói này");
        lblStrategyTitle.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        Region spStrategy = new Region();
        HBox.setHgrow(spStrategy, Priority.ALWAYS);
        lblStrategyBadge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 6px; -fx-font-family: 'JetBrains Mono', monospace;");
        strategyHeader.getChildren().addAll(lblStrategyTitle, spStrategy, lblStrategyBadge);

        lblStrategyDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #1E293B; -fx-line-spacing: 2px;");
        lblStrategyDesc.setWrapText(true);

        strategyBox.getChildren().addAll(strategyHeader, lblStrategyDesc);

        // 4. Initial Deposit + Quick Fill Chips
        VBox grpDeposit = new VBox(6);
        HBox depositLabelRow = new HBox();
        Label lblDeposit = new Label("Nạp tiền ban đầu (VND, không bắt buộc)");
        lblDeposit.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        Region spDeposit = new Region();
        HBox.setHgrow(spDeposit, Priority.ALWAYS);
        lblBalancePreview.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #059669; -fx-font-family: 'JetBrains Mono', monospace;");
        depositLabelRow.getChildren().addAll(lblDeposit, spDeposit, lblBalancePreview);

        txtInitialDeposit.getStyleClass().add("form-input");
        txtInitialDeposit.setId("initialDeposit");
        txtInitialDeposit.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) {
                txtInitialDeposit.setText(newV.replaceAll("[^\\d]", ""));
                return;
            }
            resetPreviewStatus();
            try {
                double val = txtInitialDeposit.getText().isEmpty() ? 0 : Double.parseDouble(txtInitialDeposit.getText());
                lblBalancePreview.setText(UiUtils.formatVnd(val));
            } catch (Exception ignored) {
                lblBalancePreview.setText("0 VND");
            }
            updateActivityLog();
            updateCodePreview();
        });

        // Quick Deposit Amount Chips
        HBox quickChipsRow = new HBox(6);
        quickChipsRow.setAlignment(Pos.CENTER_LEFT);
        Label lblQuick = new Label("Chọn nhanh:");
        lblQuick.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: 600;");

        quickChipsRow.getChildren().addAll(
                lblQuick,
                createDepositChip("0", 0),
                createDepositChip("500 nghìn", 500_000),
                createDepositChip("1 triệu", 1_000_000),
                createDepositChip("5 triệu", 5_000_000)
        );

        grpDeposit.getChildren().addAll(depositLabelRow, txtInitialDeposit, quickChipsRow);

        // Action Button
        btnCreate.getStyleClass().add("btn-primary");
        btnCreate.setId("openAccountSubmit");
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        btnCreate.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-padding: 11 16; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> handleOpenAccount());

        // Result Banner
        buildResultBanner();

        form.getChildren().addAll(grpOwner, grpType, strategyBox, grpDeposit, btnCreate, bannerResult);
        card.getChildren().addAll(cardTitleBox, form);

        updateStrategyInfo();
        return card;
    }

    private Button createDepositChip(String text, double amount) {
        Button btn = new Button(text);
        btn.getStyleClass().add("quick-chip");
        btn.setOnAction(e -> txtInitialDeposit.setText(String.format("%.0f", amount)));
        return btn;
    }

    private HBox buildTypeChoiceCard(RadioButton rb, String typeName, String sub, String feeHint, String color, String bg) {
        HBox card = new HBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("type-choice-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        rb.setText("");

        VBox contentBox = new VBox(3);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(typeName);
        badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 6px;");

        Label lblSub = new Label("(" + sub + ")");
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: 600;");
        topRow.getChildren().addAll(badge, lblSub);

        Label lblFee = new Label(feeHint);
        lblFee.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #475569;");

        contentBox.getChildren().addAll(topRow, lblFee);

        card.getChildren().addAll(rb, contentBox);
        card.setOnMouseClicked(e -> {
            rb.setSelected(true);
            updateCardTileStyles();
        });

        return card;
    }

    private void updateCardTileStyles() {
        if (cardStandardTile == null || cardSavingsTile == null || cardPremiumTile == null) return;

        cardStandardTile.getStyleClass().remove("type-choice-card-selected-standard");
        cardSavingsTile.getStyleClass().remove("type-choice-card-selected-savings");
        cardPremiumTile.getStyleClass().remove("type-choice-card-selected-premium");

        if (rbStandard.isSelected()) {
            cardStandardTile.getStyleClass().add("type-choice-card-selected-standard");
        } else if (rbSavings.isSelected()) {
            cardSavingsTile.getStyleClass().add("type-choice-card-selected-savings");
        } else if (rbPremium.isSelected()) {
            cardPremiumTile.getStyleClass().add("type-choice-card-selected-premium");
        }
    }

    private void updateStrategyInfo() {
        AccountType type = getSelectedAccountType();
        switch (type) {
            case STANDARD -> {
                lblStrategyBadge.setText("StandardFeeStrategy");
                lblStrategyBadge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 6px; -fx-font-family: 'JetBrains Mono', monospace;");
                lblStrategyDesc.setText("Chuyển tiền: 0,1% số tiền giao dịch. Tiền nạp ban đầu không dùng để tính phí chuyển tiền.");
            }
            case SAVINGS -> {
                lblStrategyBadge.setText("TieredFeeStrategy");
                lblStrategyBadge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 6px; -fx-font-family: 'JetBrains Mono', monospace;");
                lblStrategyDesc.setText("Chuyển tiền: đến 1 triệu 0,1%; trên 1 đến 10 triệu 0,05%; trên 10 triệu 0,02% (theo số tiền chuyển).");
            }
            case PREMIUM -> {
                lblStrategyBadge.setText("PremiumFeeStrategy");
                lblStrategyBadge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 8; -fx-background-radius: 6px; -fx-font-family: 'JetBrains Mono', monospace;");
                lblStrategyDesc.setText("Chuyển tiền: 0 VND phí giao dịch.");
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
        bannerResult.setId("accountOpenResult");
        bannerResult.setVisible(false);
        bannerResult.setManaged(false);

        lblResultTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #065F46;");
        lblResultDetail.setStyle("-fx-font-size: 14px; -fx-text-fill: #047857;");
        lblResultDetail.setWrapText(true);
        lblResultMeta.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669;");
        btnCreateAnother.getStyleClass().add("btn-secondary");
        btnCreateAnother.setOnAction(e -> {
            txtOwnerName.clear();
            txtInitialDeposit.setText("0");
            rbStandard.setSelected(true);
            lastCreatedAccount = null;
            accountTable.refresh();
            bannerResult.setVisible(false);
            bannerResult.setManaged(false);
            btnCreate.setDisable(false);
            txtOwnerName.requestFocus();
        });

        bannerResult.getChildren().addAll(lblResultTitle, lblResultDetail, lblResultMeta, btnCreateAnother);
    }

    /**
     * Bảng điều khiển Pattern Activity Log với Zero Truncation Layout
     */
    private VBox buildPatternActivityInspectorCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("activity-log-surface");
        lblStep4StoredValue.setId("accountStoredStatus");

        // Optional technical view for an examiner or curious user.
        HBox titleBar = new HBox(8);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getStyleClass().add("terminal-title-bar");

        Label lblTitle = new Label("Cách hệ thống mở tài khoản");
        lblTitle.setStyle("-fx-text-fill: #F1F5F9; -fx-font-size: 14px; -fx-font-weight: 700;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Segmented Switch Buttons: [⚡ Timeline Pipeline] | [💻 Code Inspector]
        HBox tabBox = new HBox(4);
        tabBox.setAlignment(Pos.CENTER_RIGHT);

        btnTabTimeline = new Button("Các bước xử lý");
        btnTabTimeline.getStyleClass().addAll("activity-tab-btn", "activity-tab-btn-active");

        btnTabCode = new Button("Mã minh họa");
        btnTabCode.getStyleClass().add("activity-tab-btn");

        btnTabTimeline.setOnAction(e -> switchInspectorTab(true));
        btnTabCode.setOnAction(e -> switchInspectorTab(false));

        tabBox.getChildren().addAll(btnTabTimeline, btnTabCode);
        titleBar.getChildren().addAll(lblTitle, sp, tabBox);

        // Container switching between Timeline and Code Inspector
        StackPane inspectorContainer = new StackPane();
        VBox.setVgrow(inspectorContainer, Priority.ALWAYS);

        // VIEW 1: Timeline Pipeline View (Mặc định)
        paneTimeline = buildTimelinePipelineView();

        // VIEW 2: Code Inspector View
        paneCodeScroll = buildCodeView();
        paneCodeScroll.setVisible(false);
        paneCodeScroll.setManaged(false);

        inspectorContainer.getChildren().addAll(paneTimeline, paneCodeScroll);

        Label note = new Label("Các giá trị bên dưới là bản xem trước; chỉ được đánh dấu hoàn thành sau khi tài khoản được lưu.");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 12px;");
        Button btnExplain = new Button("Giải thích Builder, Strategy và State");
        btnExplain.getStyleClass().add("activity-tab-btn");
        btnExplain.setOnAction(e -> showPatternExplanationDialog());

        card.getChildren().addAll(titleBar, note, inspectorContainer, btnExplain);

        updateActivityLog();
        updateCodePreview();
        resetPreviewStatus();
        return card;
    }

    private void switchInspectorTab(boolean showTimeline) {
        if (showTimeline) {
            btnTabTimeline.getStyleClass().add("activity-tab-btn-active");
            btnTabCode.getStyleClass().remove("activity-tab-btn-active");
            paneTimeline.setVisible(true);
            paneTimeline.setManaged(true);
            paneCodeScroll.setVisible(false);
            paneCodeScroll.setManaged(false);
        } else {
            btnTabCode.getStyleClass().add("activity-tab-btn-active");
            btnTabTimeline.getStyleClass().remove("activity-tab-btn-active");
            paneCodeScroll.setVisible(true);
            paneCodeScroll.setManaged(true);
            paneTimeline.setVisible(false);
            paneTimeline.setManaged(false);
        }
    }

    /**
     * Xây dựng Timeline Pipeline với Zero-Truncation Layout
     */
    private VBox buildTimelinePipelineView() {
        VBox box = new VBox(8);
        box.getStyleClass().add("timeline-card-box");
        box.setPrefHeight(270);

        Label lblHint = new Label("Dữ liệu dự kiến trước khi tạo; kết quả được xác nhận sau khi lưu.");
        lblHint.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-weight: 600;");

        // STEP 1: Strategy Pattern Assigned
        step1Box = new VBox(2);
        step1Box.getStyleClass().add("timeline-step-row");
        HBox step1Header = new HBox(6);
        step1Header.setAlignment(Pos.CENTER_LEFT);
        Label chk1 = stepIcons[0];
        chk1.getStyleClass().add("timeline-check-icon");
        Label lbl1 = new Label("Chọn cách tính phí");
        lbl1.getStyleClass().add("timeline-step-name");
        lbl1.setMinWidth(125);
        Label arr1 = new Label("➔");
        arr1.getStyleClass().add("timeline-arrow");
        lblStep1StrategyValue.getStyleClass().add("timeline-step-val");
        lblStep1StrategyValue.setStyle("-fx-text-fill: #38BDF8;");
        lblStep1StrategyValue.setWrapText(true);
        HBox.setHgrow(lblStep1StrategyValue, Priority.ALWAYS);
        step1Header.getChildren().addAll(chk1, lbl1, arr1, lblStep1StrategyValue);

        lblStep1StrategyDesc.getStyleClass().add("timeline-sub-hint");
        step1Box.getChildren().addAll(step1Header, lblStep1StrategyDesc);

        // STEP 2: Builder.type() & Builder.owner()
        step2Box = new VBox(2);
        step2Box.getStyleClass().add("timeline-step-row");
        HBox step2Header = new HBox(6);
        step2Header.setAlignment(Pos.CENTER_LEFT);
        Label chk2 = stepIcons[1];
        chk2.getStyleClass().add("timeline-check-icon");
        Label lbl2 = new Label("Account.Builder");
        lbl2.getStyleClass().add("timeline-step-name");
        lbl2.setMinWidth(125);
        Label arr2 = new Label("➔");
        arr2.getStyleClass().add("timeline-arrow");
        lblStep2TypeValue.getStyleClass().add("timeline-step-val");
        lblStep2TypeValue.setStyle("-fx-text-fill: #A78BFA;");
        HBox.setHgrow(lblStep2TypeValue, Priority.ALWAYS);
        step2Header.getChildren().addAll(chk2, lbl2, arr2, lblStep2TypeValue);

        HBox step2Sub = new HBox(4);
        step2Sub.setAlignment(Pos.CENTER_LEFT);
        step2Sub.setPadding(new Insets(1, 0, 0, 22));
        Label lblOwnerTitle = new Label("Chủ tài khoản ➔ ");
        lblOwnerTitle.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #64748B;");
        lblStep2OwnerValue.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #40E18F;");
        step2Sub.getChildren().addAll(lblOwnerTitle, lblStep2OwnerValue);
        step2Box.getChildren().addAll(step2Header, step2Sub);

        // STEP 3: build the account with a zero balance.
        step3Box = new VBox(2);
        step3Box.getStyleClass().add("timeline-step-row");
        HBox step3Header = new HBox(6);
        step3Header.setAlignment(Pos.CENTER_LEFT);
        Label chk3 = stepIcons[2];
        chk3.getStyleClass().add("timeline-check-icon");
        Label lbl3 = new Label("Builder.build()");
        lbl3.getStyleClass().add("timeline-step-name");
        lbl3.setMinWidth(125);
        Label arr3 = new Label("➔");
        arr3.getStyleClass().add("timeline-arrow");
        lblStep3BuiltValue.getStyleClass().add("timeline-step-val");
        HBox.setHgrow(lblStep3BuiltValue, Priority.ALWAYS);
        step3Header.getChildren().addAll(chk3, lbl3, arr3, lblStep3BuiltValue);
        step3Box.getChildren().add(step3Header);

        // STEP 4: persist the new account.
        step4Box = new VBox(2);
        step4Box.getStyleClass().add("timeline-step-row");
        HBox step4Header = new HBox(6);
        step4Header.setAlignment(Pos.CENTER_LEFT);
        Label chk4 = stepIcons[3];
        chk4.getStyleClass().add("timeline-check-icon");
        Label lbl4 = new Label("Lưu tài khoản");
        lbl4.getStyleClass().add("timeline-step-name");
        lbl4.setMinWidth(125);
        Label arr4 = new Label("➔");
        arr4.getStyleClass().add("timeline-arrow");
        lblStep4StoredValue.getStyleClass().add("timeline-step-val");
        lblStep4StoredValue.setWrapText(true);
        HBox.setHgrow(lblStep4StoredValue, Priority.ALWAYS);
        step4Header.getChildren().addAll(chk4, lbl4, arr4, lblStep4StoredValue);
        step4Box.getChildren().add(step4Header);

        // STEP 5: deposit is a separate, optional facade operation.
        step5Box = new VBox(2);
        step5Box.getStyleClass().add("timeline-step-row");
        HBox step5Header = new HBox(6);
        step5Header.setAlignment(Pos.CENTER_LEFT);
        Label chk5 = stepIcons[4];
        chk5.getStyleClass().add("timeline-check-icon");
        Label lbl5 = new Label("Nạp ban đầu");
        lbl5.getStyleClass().add("timeline-step-name");
        lbl5.setMinWidth(125);
        Label arr5 = new Label("➔");
        arr5.getStyleClass().add("timeline-arrow");
        lblStep5DepositValue.getStyleClass().add("timeline-step-val");
        lblStep5DepositValue.setWrapText(true);
        HBox.setHgrow(lblStep5DepositValue, Priority.ALWAYS);
        step5Header.getChildren().addAll(chk5, lbl5, arr5, lblStep5DepositValue);
        step5Box.getChildren().add(step5Header);

        // Dynamic State Transition Event Box
        boxStateTransition.getStyleClass().add("timeline-state-box");
        HBox stateHead = new HBox(6);
        stateHead.setAlignment(Pos.CENTER_LEFT);
        Label iconState = new Label("🔄");
        iconState.setStyle("-fx-font-size: 11px;");
        Label titleState = new Label("State Pattern Event Log:");
        titleState.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #F59E0B;");
        stateHead.getChildren().addAll(iconState, titleState);

        lblStateTransitionText.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #E2E8F0;");
        lblStateTransitionText.setWrapText(true);
        boxStateTransition.getChildren().addAll(stateHead, lblStateTransitionText);

        box.getChildren().addAll(lblHint, step1Box, step2Box, step3Box, step4Box, step5Box, boxStateTransition);
        return box;
    }

    private ScrollPane buildCodeView() {
        ScrollPane codeScroll = new ScrollPane(codeFlow);
        codeScroll.setFitToWidth(true);
        codeScroll.setStyle("-fx-background: #0F172A; -fx-background-color: #0F172A; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        codeScroll.setPrefHeight(270);

        codeFlow.setStyle("-fx-background-color: #0F172A; -fx-padding: 10;");
        return codeScroll;
    }

    private void updateActivityLog() {
        AccountType type = getSelectedAccountType();
        String owner = txtOwnerName.getText().trim().isEmpty() ? "Chưa nhập" : txtOwnerName.getText().trim();
        String depositVal = lblBalancePreview.getText();

        switch (type) {
            case STANDARD -> {
                lblStep1StrategyValue.setText("StandardFeeStrategy (STANDARD)");
                lblStep1StrategyDesc.setText("Phí chuyển tiền: 0,1% số tiền giao dịch");
                lblStep2TypeValue.setText("AccountType.STANDARD");
            }
            case SAVINGS -> {
                lblStep1StrategyValue.setText("TieredFeeStrategy (SAVINGS)");
                lblStep1StrategyDesc.setText("Phí bậc thang theo GD: ≤1M: 0.1% | ≤10M: 0.05% | >10M: 0.02%");
                lblStep2TypeValue.setText("AccountType.SAVINGS");
            }
            case PREMIUM -> {
                lblStep1StrategyValue.setText("PremiumFeeStrategy (PREMIUM)");
                lblStep1StrategyDesc.setText("Phí chuyển tiền: 0 VND");
                lblStep2TypeValue.setText("AccountType.PREMIUM");
            }
        }

        lblStep2OwnerValue.setText("\"" + owner.toUpperCase() + "\"");
        if (!"✓".equals(stepIcons[3].getText())) {
            lblStep5DepositValue.setText("0 VND".equals(depositVal) ? "Không nạp ban đầu" : "Dự kiến " + depositVal);
        }
    }

    private void resetPreviewStatus() {
        if (bannerResult.isVisible()) {
            bannerResult.setVisible(false);
            bannerResult.setManaged(false);
            btnCreate.setDisable(false);
        }
        for (Label icon : stepIcons) {
            icon.setText("○");
            icon.setStyle("-fx-text-fill: #94A3B8;");
        }
        lblStep3BuiltValue.setText("Chưa tạo");
        lblStep3BuiltValue.setStyle("-fx-text-fill: #94A3B8;");
        lblStep4StoredValue.setText("Chưa lưu");
        lblStep4StoredValue.setStyle("-fx-text-fill: #94A3B8;");
    }

    private void markStoredStatus(Account account, double requestedDeposit, String depositError) {
        for (Label icon : stepIcons) {
            icon.setText("✓");
            icon.setStyle("-fx-text-fill: #10B981;");
        }
        if (requestedDeposit == 0) {
            stepIcons[4].setText("–");
            stepIcons[4].setStyle("-fx-text-fill: #94A3B8;");
            lblStep5DepositValue.setText("Không nạp ban đầu");
        } else if (depositError != null) {
            stepIcons[4].setText("×");
            stepIcons[4].setStyle("-fx-text-fill: #F87171;");
            lblStep5DepositValue.setText("Chưa nạp được tiền");
        } else {
            lblStep5DepositValue.setText("Đã nạp " + UiUtils.formatVnd(requestedDeposit));
        }
        lblStep3BuiltValue.setText(account.getAccountNumber() + " đã tạo");
        lblStep3BuiltValue.setStyle("-fx-text-fill: #10B981;");
        lblStep4StoredValue.setText("Đã lưu vào SQLite");
        lblStep4StoredValue.setStyle("-fx-text-fill: #10B981;");
    }

    private void updateCodePreview() {
        codeFlow.getChildren().clear();

        String name = txtOwnerName.getText().trim().isEmpty() ? "<Tên chủ tài khoản>" : txtOwnerName.getText().trim().toUpperCase();
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

        addCodeText("// 2. Trong AccountService.openAccount(): tạo tài khoản số dư 0\n", "#64748B");
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
        addCodeText("0", "#40E18F");
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

        addCodeText("// 3. Lưu tài khoản, sau đó nạp tiền nếu có\n", "#64748B");
        addCodeText("db.saveAccount(account);\n", "#38BDF8");
        if (!"0.0".equals(depositVal)) {
            addCodeText("facade.deposit(account.getAccountNumber(), " + depositVal + ");", "#38BDF8");
        }
    }

    private void addCodeText(String str, String colorHex) {
        Text t = new Text(str);
        t.setStyle("-fx-fill: " + colorHex + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
        codeFlow.getChildren().add(t);
    }

    private VBox buildAccountTableSection() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 18;");

        HBox tableHeader = new HBox(12);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        VBox titleArea = new VBox(2);
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("Danh sách tài khoản");
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        titleRow.getChildren().add(lblTitle);

        Label lblSub = new Label("Tìm tài khoản, xem số dư và khóa hoặc mở khóa khi cần.");
        lblSub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
        titleArea.getChildren().addAll(titleRow, lblSub);

        // Search Field
        txtTableSearch.setPromptText("🔍 Tìm theo mã hoặc tên...");
        txtTableSearch.setStyle("-fx-pref-width: 220px; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-padding: 5 10;");

        tableHeader.getChildren().addAll(titleArea, txtTableSearch);

        // Quick Stats Bar
        HBox statsBar = buildQuickStatsBar();

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

        // Fixed Double-Dot in Status Column
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
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Circle dot = new Circle(3.5, isActive ? Color.web("#10B981") : Color.web("#EF4444"));
                    Label lbl = new Label(isActive ? "Hoạt động" : "Đã khóa");
                    lbl.setStyle(isActive ? "-fx-text-fill: #059669; -fx-font-weight: 700; -fx-font-size: 11.5px;"
                            : "-fx-text-fill: #DC2626; -fx-font-weight: 700; -fx-font-size: 11.5px;");
                    box.getChildren().addAll(dot, lbl);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // Polished State Action Buttons
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
                    btnToggle.setText("Khóa tài khoản");
                    btnToggle.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-cursor: hand; -fx-border-color: #FDE68A; -fx-border-radius: 6px;");
                    btnToggle.setOnAction(e -> {
                        try {
                            ctx.getAccountService().lockAccount(acc.getAccountNumber());
                            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                            String eventMsg = "[" + time + "] " + acc.getAccountNumber() + " [ActiveState] ➔ [LockedState] (Khóa giao dịch)";
                            lblStateTransitionText.setText(eventMsg);

                            ctx.logCustomEvent("State Pattern", "Tài khoản " + acc.getAccountNumber() + " chuyển sang LockedState.");
                            ctx.notifyDataChanged();
                            ToastNotification.showWarning("Đã khóa tài khoản " + acc.getAccountNumber() + " (LockedState)");
                        } catch (Exception ex) {
                            ToastNotification.showError(UiUtils.humanizeError(ex));
                        }
                    });
                } else {
                    btnToggle.setText("Mở khóa");
                    btnToggle.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-cursor: hand; -fx-border-color: #A7F3D0; -fx-border-radius: 6px;");
                    btnToggle.setOnAction(e -> {
                        try {
                            ctx.getAccountService().unlockAccount(acc.getAccountNumber());
                            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                            String eventMsg = "[" + time + "] " + acc.getAccountNumber() + " [LockedState] ➔ [ActiveState] (Khôi phục hoạt động)";
                            lblStateTransitionText.setText(eventMsg);

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
        accountTable.setPrefHeight(190);
        accountTable.setMinHeight(160);

        // Flash highlight on newly created row
        accountTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && item.equals(lastCreatedAccount)) {
                    getStyleClass().add("table-row-newly-created");
                } else {
                    getStyleClass().remove("table-row-newly-created");
                }
            }
        });

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

        card.getChildren().addAll(tableHeader, statsBar, accountTable);
        return card;
    }

    private HBox buildQuickStatsBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("table-quick-stats");

        bar.getChildren().addAll(
                createStatItem("TỔNG TÀI KHOẢN", lblStatTotalAccounts, "#1E293B"),
                createStatSeparator(),
                createStatItem("ĐANG HOẠT ĐỘNG", lblStatActiveAccounts, "#059669"),
                createStatSeparator(),
                createStatItem("TẠM KHÓA", lblStatLockedAccounts, "#DC2626"),
                createStatSeparator(),
                createStatItem("TỔNG SỐ DƯ", lblStatTotalDeposits, "#2563EB")
        );
        return bar;
    }

    private VBox createStatItem(String label, Label valueLabel, String colorHex) {
        VBox item = new VBox(2);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-pill-label");
        valueLabel.getStyleClass().add("stat-pill-value");
        valueLabel.setStyle("-fx-text-fill: " + colorHex + ";");
        item.getChildren().addAll(lbl, valueLabel);
        return item;
    }

    private Region createStatSeparator() {
        Region r = new Region();
        r.setStyle("-fx-background-color: #E2E8F0; -fx-min-width: 1px; -fx-pref-width: 1px; -fx-max-width: 1px; -fx-min-height: 24px;");
        return r;
    }

    private void updateQuickStats() {
        int total = ctx.getAccounts().size();
        long active = ctx.getAccounts().stream().filter(a -> a.getStatus() == AccountStatus.ACTIVE).count();
        long locked = total - active;
        double sumBalance = ctx.getAccounts().stream().mapToDouble(Account::getBalance).sum();

        lblStatTotalAccounts.setText(total + " tài khoản");
        lblStatActiveAccounts.setText(active + " tài khoản");
        lblStatLockedAccounts.setText(locked + " tài khoản");
        lblStatTotalDeposits.setText(UiUtils.formatVnd(sumBalance));
    }

    /**
     * Khởi tạo tài khoản với Step-by-Step Builder Pipeline Animation
     */
    private void handleOpenAccount() {
        String owner = txtOwnerName.getText().trim();
        if (owner.isEmpty()) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Chưa nhập tên", "Vui lòng nhập họ tên chủ tài khoản.");
            return;
        }

        double initialDeposit = 0;
        try {
            String requestedDeposit = txtInitialDeposit.getText().trim();
            initialDeposit = requestedDeposit.isEmpty() ? 0 : Money.nonNegative(Double.parseDouble(requestedDeposit));
        } catch (IllegalArgumentException e) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Số tiền không hợp lệ", "Vui lòng nhập một số dương hợp lệ.");
            return;
        }

        AccountType type = getSelectedAccountType();
        Account created;
        try {
            created = ctx.getAccountService().openAccount(owner, type);
            ctx.attachUiObserver(created);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            UiUtils.showAlert(Alert.AlertType.ERROR, "Không thể mở tài khoản", null, exception.getMessage());
            return;
        }

        String depositError = null;
        if (initialDeposit > 0) {
            try {
                ctx.getFacade().deposit(created.getAccountNumber(), initialDeposit);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                depositError = UiUtils.humanizeError(exception);
            }
        }
        ctx.notifyDataChanged();

        lastCreatedAccount = created;
        btnCreate.setDisable(true);
        markStoredStatus(created, initialDeposit, depositError);
        lblStateTransitionText.setText("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                + "] " + created.getAccountNumber() + " bắt đầu ở trạng thái hoạt động.");

        bannerResult.setVisible(true);
        bannerResult.setManaged(true);
        lblResultTitle.setText(depositError == null ? "Đã mở tài khoản " + created.getAccountNumber()
                : "Đã mở tài khoản; chưa nạp được tiền");
        lblResultDetail.setText("Chủ tài khoản: " + created.getOwnerName() + " · Gói: " + created.getType()
                + " · Số dư: " + UiUtils.formatVnd(created.getBalance()));
        lblResultMeta.setText(depositError == null
                ? "Trạng thái: Đang hoạt động · Phí chuyển tiền: " + created.getFeeStrategy().getName()
                : "Tài khoản " + created.getAccountNumber() + " đã được lưu. Nạp tiền chưa thành công: "
                        + depositError + ". Có thể nạp lại ở mục Tiền mặt.");

        if (depositError == null) ToastNotification.showSuccess("Đã mở tài khoản " + created.getAccountNumber());
        else ToastNotification.showWarning("Tài khoản đã mở, nhưng chưa nạp tiền: " + depositError);
        accountTable.getSelectionModel().select(created);
        accountTable.scrollTo(created);
        accountTable.refresh();
        updateQuickStats();
    }

    /**
     * Modal Dialog giải thích ý nghĩa kiến trúc của 3 Design Pattern
     */
    private void showPatternExplanationDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ý Nghĩa Kiến Trúc 3 Design Pattern trong Mở Tài Khoản");
        dialog.setHeaderText("Kiến Trúc Phần Mềm: Builder · Strategy · State Pattern");

        VBox content = new VBox(12);
        content.setPrefWidth(520);
        content.setStyle("-fx-padding: 14;");

        content.getChildren().addAll(
                createPatternExplainBlock("🏗️ 1. Builder Pattern (Account.Builder)",
                        "• Mục đích: Tách rời quá trình khởi tạo đối tượng phức tạp khỏi biểu diễn thực tế.\n" +
                                "• Lợi ích: Tránh lỗi Telescoping Constructor (quá nhiều tham số trong hàm khởi tạo). Cho phép gọi tuần tự type(), balance(), status(), feeStrategy(), state() trước khi build()."),

                createPatternExplainBlock("🎯 2. Strategy Pattern (FeeStrategy)",
                        "• Mục đích: Định nghĩa và đóng gói các thuật toán tính phí chuyển tiền linh hoạt.\n" +
                                "• Lợi ích: Tuân thủ Open-Closed Principle (OCP). Dễ dàng thêm gói cước mới (Standard, Tiered, Premium) mà không phải sửa đổi mã nguồn lớp Account."),

                createPatternExplainBlock("🛡️ 3. State Pattern (ActiveState ↔ LockedState)",
                        "• Mục đích: Đóng gói các hành vi thay đổi theo trạng thái của tài khoản.\n" +
                                "• Lợi ích: LockedState chặn rút và chuyển tiền từ tài khoản nguồn, nhưng vẫn cho phép nhận tiền; ActiveState cho phép giao dịch bình thường.")
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private VBox createPatternExplainBlock(String title, String desc) {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 12;");
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #475569; -fx-line-spacing: 2px;");
        lblDesc.setWrapText(true);
        box.getChildren().addAll(lblTitle, lblDesc);
        return box;
    }

    public void refresh() {
        accountTable.refresh();
        updateQuickStats();
        updateActivityLog();
        updateCodePreview();
    }
}

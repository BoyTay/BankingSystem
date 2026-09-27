package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Màn hình Quản lý & Mở tài khoản.
 * Minh họa Patterns:
 * - Builder Pattern (Khởi tạo Account qua Account.Builder)
 * - Strategy Pattern (Lựa chọn FeeStrategy theo loại tài khoản)
 * - State Pattern (Khóa / Mở khóa tài khoản trực tiếp trên bảng)
 */
public class AccountView extends VBox {

    private final UIContext ctx = UIContext.getInstance();
    private final TextField txtOwnerName = new TextField();
    private final ComboBox<AccountType> cbAccountType = new ComboBox<>();
    private final TextField txtInitialDeposit = new TextField("0");
    private final Label lblStrategyDescription = new Label();
    private final TextArea txtCodePreview = new TextArea();
    private final TableView<Account> accountTable = new TableView<>();

    public AccountView() {
        setSpacing(24);
        getStyleClass().add("content-pane");

        buildHeader();

        // 2 columns layout: Form Mở tài khoản bên trái + Visual Builder Code Inspector bên phải
        HBox topSection = new HBox(20);
        VBox formCard = buildOpenAccountForm();
        VBox inspectorCard = buildCodeInspectorCard();
        HBox.setHgrow(formCard, Priority.ALWAYS);
        HBox.setHgrow(inspectorCard, Priority.ALWAYS);
        topSection.getChildren().addAll(formCard, inspectorCard);

        VBox tableCard = buildAccountTableSection();

        getChildren().addAll(topSection, tableCard);

        ctx.addDataChangeListener(this::refresh);
        refresh();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Mở & Quản Lý Tài Khoản");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Khởi tạo tài khoản với Builder Pattern & áp dụng chiến lược phí linh hoạt");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        HBox patternBadges = new HBox(8);
        patternBadges.setAlignment(Pos.CENTER_RIGHT);
        patternBadges.getChildren().addAll(
                UiUtils.createPatternBadge("Builder (Account.Builder)", "creational"),
                UiUtils.createPatternBadge("Strategy (FeeStrategy)", "behavioral"),
                UiUtils.createPatternBadge("State (Active / Locked)", "behavioral")
        );

        HBox header = new HBox();
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, patternBadges);
        header.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(header);
    }

    private VBox buildOpenAccountForm() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Mở tài khoản mới (Builder Form)");
        cardTitle.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);

        txtOwnerName.setPromptText("Ví dụ: NGUYỄN VĂN A");
        txtOwnerName.textProperty().addListener((obs, oldVal, newVal) -> updateCodePreview());

        cbAccountType.getItems().addAll(AccountType.STANDARD, AccountType.SAVINGS, AccountType.PREMIUM);
        cbAccountType.setValue(AccountType.STANDARD);
        cbAccountType.setMaxWidth(Double.MAX_VALUE);
        cbAccountType.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateStrategyText(newVal);
            updateCodePreview();
        });

        txtInitialDeposit.setPromptText("Số tiền nạp ban đầu (VND)");
        txtInitialDeposit.textProperty().addListener((obs, oldVal, newVal) -> updateCodePreview());

        lblStrategyDescription.setWrapText(true);
        lblStrategyDescription.setStyle("-fx-font-size: 11px; -fx-text-fill: #0369A1; -fx-background-color: #E0F2FE; -fx-padding: 8px 10px; -fx-background-radius: 6px;");
        updateStrategyText(AccountType.STANDARD);

        grid.add(new Label("Tên chủ tài khoản:"), 0, 0);
        grid.add(txtOwnerName, 1, 0);

        grid.add(new Label("Gói tài khoản:"), 0, 1);
        grid.add(cbAccountType, 1, 1);

        grid.add(new Label("Chiến lược phí:"), 0, 2);
        grid.add(lblStrategyDescription, 1, 2);

        grid.add(new Label("Nạp tiền ban đầu:"), 0, 3);
        grid.add(txtInitialDeposit, 1, 3);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(130);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        Button btnSubmit = new Button("✔ Khởi tạo với Builder Pattern");
        btnSubmit.getStyleClass().add("btn-primary");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setOnAction(e -> handleOpenAccount());

        card.getChildren().addAll(cardTitle, grid, btnSubmit);
        return card;
    }

    private VBox buildCodeInspectorCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Visual Code Inspector (Builder Pattern)");
        cardTitle.getStyleClass().add("card-title");

        Label info = new Label("Mã nguồn thực thi phía sau giao diện theo Builder & Strategy:");
        info.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        txtCodePreview.setEditable(false);
        txtCodePreview.setPrefRowCount(10);
        txtCodePreview.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px; -fx-control-inner-background: #0F172A; -fx-text-fill: #38BDF8;");

        updateCodePreview();

        card.getChildren().addAll(cardTitle, info, txtCodePreview);
        return card;
    }

    private void updateStrategyText(AccountType type) {
        if (type == null) return;
        switch (type) {
            case STANDARD -> lblStrategyDescription.setText("StandardFeeStrategy: Thu phí cố định 0.1% mỗi giao dịch.");
            case SAVINGS -> lblStrategyDescription.setText("TieredFeeStrategy: Miễn phí nếu < 1M, 0.05% nếu < 10M, 0.1% nếu ≥ 10M.");
            case PREMIUM -> lblStrategyDescription.setText("PremiumFeeStrategy: Miễn 100% mọi phí dịch vụ và chuyển khoản.");
        }
    }

    private void updateCodePreview() {
        String name = txtOwnerName.getText().trim().isEmpty() ? "NGUYỄN VĂN A" : txtOwnerName.getText().trim();
        AccountType type = cbAccountType.getValue() == null ? AccountType.STANDARD : cbAccountType.getValue();
        String strategyClass = switch (type) {
            case PREMIUM -> "PremiumFeeStrategy()";
            case SAVINGS -> "TieredFeeStrategy()";
            case STANDARD -> "StandardFeeStrategy()";
        };

        String code = "// 1. Áp dụng Strategy Pattern\n"
                + "FeeStrategy strategy = new " + strategyClass + ";\n\n"
                + "// 2. Áp dụng Builder Pattern để tạo đối tượng Account\n"
                + "Account account = new Account.Builder(\"ACC####\", \"" + name + "\")\n"
                + "    .type(AccountType." + type.name() + ")\n"
                + "    .balance(" + txtInitialDeposit.getText() + ")\n"
                + "    .status(AccountStatus.ACTIVE)\n"
                + "    .feeStrategy(strategy)\n"
                + "    .state(new ActiveState())\n"
                + "    .build();\n\n"
                + "// 3. Đăng ký Observer thông báo\n"
                + "account.addObserver(new SmsNotifier());\n"
                + "account.addObserver(new EmailNotifier());";

        txtCodePreview.setText(code);
    }

    private void handleOpenAccount() {
        String owner = txtOwnerName.getText().trim();
        if (owner.isEmpty()) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Chưa nhập tên", "Vui lòng nhập tên chủ tài khoản.");
            return;
        }

        double initialDeposit = 0;
        try {
            initialDeposit = Money.nonNegative(Double.parseDouble(txtInitialDeposit.getText().trim()));
        } catch (IllegalArgumentException e) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Số tiền không hợp lệ", "Vui lòng nhập một số hợp lệ.");
            return;
        }

        Account created = ctx.getAccountService().openAccount(owner, cbAccountType.getValue());
        ctx.attachUiObserver(created);

        if (initialDeposit > 0) {
            ctx.getFacade().deposit(created.getAccountNumber(), initialDeposit);
        }

        ctx.notifyDataChanged();

        UiUtils.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Mở tài khoản thành công!",
                "Tài khoản số: " + created.getAccountNumber() + "\nChủ tài khoản: " + created.getOwnerName()
                        + "\nLoại: " + created.getType() + "\nSố dư: " + UiUtils.formatVnd(created.getBalance()));

        txtOwnerName.clear();
        txtInitialDeposit.setText("0");
    }

    private VBox buildAccountTableSection() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Danh sách tài khoản & Quản lý trạng thái (State Pattern)");
        cardTitle.getStyleClass().add("card-title");

        TableColumn<Account, String> colAccNo = new TableColumn<>("Số Tài Khoản");
        colAccNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAccountNumber()));
        colAccNo.setPrefWidth(120);

        TableColumn<Account, String> colOwner = new TableColumn<>("Chủ Tài Khoản");
        colOwner.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOwnerName()));
        colOwner.setPrefWidth(170);

        TableColumn<Account, String> colType = new TableColumn<>("Loại Gói");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().name()));
        colType.setPrefWidth(110);

        TableColumn<Account, String> colBalance = new TableColumn<>("Số Dư");
        colBalance.setCellValueFactory(d -> new SimpleStringProperty(UiUtils.formatVnd(d.getValue().getBalance())));
        colBalance.setPrefWidth(150);

        TableColumn<Account, String> colStrategy = new TableColumn<>("Chiến Lược Phí");
        colStrategy.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFeeStrategy().getName()));
        colStrategy.setPrefWidth(150);

        TableColumn<Account, String> colStatus = new TableColumn<>("Trạng Thái");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStatus() == AccountStatus.ACTIVE ? "Đang hoạt động" : "Đã khóa"));
        colStatus.setPrefWidth(120);

        // Actions column with Lock / Unlock buttons
        TableColumn<Account, Void> colAction = new TableColumn<>("Thao tác State");
        colAction.setPrefWidth(160);
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
                    btnToggle.setText("🔒 Khóa (Lock)");
                    btnToggle.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-background-radius: 6px;");
                    btnToggle.setOnAction(e -> {
                        ctx.getAccountService().lockAccount(acc.getAccountNumber());
                        ctx.logCustomEvent("State Pattern", "Tài khoản " + acc.getAccountNumber() + " chuyển sang LockedState.");
                        ctx.notifyDataChanged();
                    });
                } else {
                    btnToggle.setText("🔓 Mở khóa");
                    btnToggle.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-background-radius: 6px;");
                    btnToggle.setOnAction(e -> {
                        ctx.getAccountService().unlockAccount(acc.getAccountNumber());
                        ctx.logCustomEvent("State Pattern", "Tài khoản " + acc.getAccountNumber() + " chuyển sang ActiveState.");
                        ctx.notifyDataChanged();
                    });
                }
                setGraphic(btnToggle);
            }
        });

        accountTable.getColumns().setAll(colAccNo, colOwner, colType, colBalance, colStrategy, colStatus, colAction);
        accountTable.setPrefHeight(240);

        card.getChildren().addAll(cardTitle, accountTable);
        return card;
    }

    public void refresh() {
        accountTable.setItems(ctx.getAccounts());
    }
}

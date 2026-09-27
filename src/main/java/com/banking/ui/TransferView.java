package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.model.Transaction;
import com.banking.pattern.creational.TransferTemplate;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Màn hình Chuyển khoản ngân hàng.
 * Minh họa Patterns:
 * - Facade Pattern: Đơn giản hóa quy trình chuyển khoản (debit, credit, log, notify).
 * - Command Pattern: Chuyển khoản được đóng gói thành đối tượng TransferCommand, lưu vào TransactionHistory để hỗ trợ Undo.
 * - Prototype Pattern: Tạo mẫu chuyển khoản (TransferTemplate) và nhân bản (clone()) nhanh chóng.
 * - Strategy Pattern: Phí được tính tự động từ FeeStrategy của tài khoản gửi.
 */
public class TransferView extends VBox {

    private final UIContext ctx = UIContext.getInstance();

    // Form controls
    private final ComboBox<Account> cbFromAccount = new ComboBox<>();
    private final ComboBox<Account> cbToAccount = new ComboBox<>();
    private final TextField txtAmount = new TextField();
    private final TextField txtDescription = new TextField("Chuyển tiền qua Banking");
    private final Label lblFromBalance = new Label();
    private final Label lblFeeCalculation = new Label();

    // Prototype controls
    private TransferTemplate savedTemplate;
    private final Label lblTemplateStatus = new Label("Chưa có mẫu nào được lưu.");
    private final TextArea txtTemplateInspector = new TextArea();

    public TransferView() {
        setSpacing(24);
        getStyleClass().add("content-pane");

        buildHeader();

        HBox mainSection = new HBox(20);
        VBox transferCard = buildTransferFormCard();
        VBox prototypeCard = buildPrototypeCard();
        HBox.setHgrow(transferCard, Priority.ALWAYS);
        HBox.setHgrow(prototypeCard, Priority.ALWAYS);
        mainSection.getChildren().addAll(transferCard, prototypeCard);

        VBox explainerCard = buildPatternExplainerCard();

        getChildren().addAll(mainSection, explainerCard);

        ctx.addDataChangeListener(this::refresh);
        refresh();
    }

    private void buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("Chuyển Khoản Ngân Hàng");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Giao dịch chuyển tiền liên tài khoản hỗ trợ Undo (Command) & Mẫu giao dịch (Prototype)");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        HBox patternBadges = new HBox(8);
        patternBadges.setAlignment(Pos.CENTER_RIGHT);
        patternBadges.getChildren().addAll(
                UiUtils.createPatternBadge("Facade (BankingFacade)", "structural"),
                UiUtils.createPatternBadge("Command (TransferCommand)", "behavioral"),
                UiUtils.createPatternBadge("Prototype (TransferTemplate.clone())", "creational")
        );

        HBox header = new HBox();
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, patternBadges);
        header.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(header);
    }

    private VBox buildTransferFormCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Thông tin chuyển khoản (Transfer Form)");
        cardTitle.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);

        setupAccountCombo(cbFromAccount, true);
        setupAccountCombo(cbToAccount, false);

        txtAmount.setPromptText("Nhập số tiền (VND)");
        txtAmount.textProperty().addListener((obs, oldVal, newVal) -> updateFeePreview());
        cbFromAccount.valueProperty().addListener((obs, oldVal, newVal) -> updateFeePreview());

        lblFeeCalculation.setStyle("-fx-font-size: 11px; -fx-text-fill: #0369A1; -fx-font-weight: bold;");
        lblFeeCalculation.setText("Phí giao dịch: 0 VND");

        grid.add(new Label("Tài khoản nguồn:"), 0, 0);
        grid.add(cbFromAccount, 1, 0);

        grid.add(new Label("Số dư nguồn:"), 0, 1);
        grid.add(lblFromBalance, 1, 1);

        grid.add(new Label("Tài khoản thụ hưởng:"), 0, 2);
        grid.add(cbToAccount, 1, 2);

        grid.add(new Label("Số tiền chuyển:"), 0, 3);
        grid.add(txtAmount, 1, 3);

        grid.add(new Label("Phí giao dịch:"), 0, 4);
        grid.add(lblFeeCalculation, 1, 4);

        grid.add(new Label("Nội dung:"), 0, 5);
        grid.add(txtDescription, 1, 5);

        ColumnConstraints c1 = new ColumnConstraints(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        HBox buttonBox = new HBox(12);
        Button btnTransfer = new Button("🚀 Chuyển Khoản Ngay (Command + Facade)");
        btnTransfer.getStyleClass().add("btn-primary");
        btnTransfer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnTransfer, Priority.ALWAYS);
        btnTransfer.setOnAction(e -> handleTransfer());

        Button btnSaveTemplate = new Button("💾 Lưu Mẫu (Prototype)");
        btnSaveTemplate.getStyleClass().add("btn-secondary");
        btnSaveTemplate.setOnAction(e -> handleSaveTemplate());

        buttonBox.getChildren().addAll(btnTransfer, btnSaveTemplate);

        card.getChildren().addAll(cardTitle, grid, buttonBox);
        return card;
    }

    private VBox buildPrototypeCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        HBox cardHeader = new HBox(8);
        Label cardTitle = new Label("Prototype Pattern — Quản lý Mẫu Giao Dịch");
        cardTitle.getStyleClass().add("card-title");
        Label badge = UiUtils.createPatternBadge("Prototype", "creational");
        HBox.setHgrow(cardTitle, Priority.ALWAYS);
        cardHeader.getChildren().addAll(cardTitle, badge);

        lblTemplateStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        txtTemplateInspector.setEditable(false);
        txtTemplateInspector.setPrefRowCount(8);
        txtTemplateInspector.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px; -fx-control-inner-background: #0F172A; -fx-text-fill: #FDE047;");
        txtTemplateInspector.setText("// Prototype Demo\n// Bấm 'Lưu Mẫu' từ form bên trái để tạo mẫu gốc (Original).\n// Sau đó bấm 'Nhân bản (clone())' để sinh bản sao độc lập.");

        HBox actionBox = new HBox(10);
        Button btnClone = new Button("📑 Nhân bản mẫu (clone())");
        btnClone.getStyleClass().add("btn-navy");
        btnClone.setOnAction(e -> handleCloneTemplate());

        Button btnApply = new Button("⚡ Áp dụng vào Form");
        btnApply.getStyleClass().add("btn-secondary");
        btnApply.setOnAction(e -> handleApplyTemplate());

        actionBox.getChildren().addAll(btnClone, btnApply);

        card.getChildren().addAll(cardHeader, lblTemplateStatus, txtTemplateInspector, actionBox);
        return card;
    }

    private void setupAccountCombo(ComboBox<Account> cb, boolean isSource) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setCellFactory(param -> new ListCell<>() {
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

        if (isSource) {
            cb.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    lblFromBalance.setText(UiUtils.formatVnd(newVal.getBalance()) + " (" + newVal.getStatus() + ")");
                } else {
                    lblFromBalance.setText("-");
                }
            });
        }
    }

    private void updateFeePreview() {
        Account from = cbFromAccount.getValue();
        if (from == null) {
            lblFeeCalculation.setText("Phí giao dịch: 0 VND");
            return;
        }
        try {
            double amount = Double.parseDouble(txtAmount.getText().trim());
            double fee = Money.nonNegative(from.getFeeStrategy().calculateFee(Money.positive(amount)));
            lblFeeCalculation.setText(String.format("Phí: %s (%s)",
                    UiUtils.formatVnd(fee), from.getFeeStrategy().getName()));
        } catch (IllegalArgumentException e) {
            lblFeeCalculation.setText("Phí giao dịch: 0 VND");
        }
    }

    private void handleTransfer() {
        Account from = cbFromAccount.getValue();
        Account to = cbToAccount.getValue();

        if (from == null || to == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Thiếu thông tin", "Vui lòng chọn cả tài khoản nguồn và tài khoản đích.");
            return;
        }

        try {
            double amount = Double.parseDouble(txtAmount.getText().trim());
            String desc = txtDescription.getText().trim().isEmpty() ? "Chuyển tiền qua Banking" : txtDescription.getText().trim();
            Transaction transaction = ctx.getFacade().transfer(
                    from.getAccountNumber(), to.getAccountNumber(), amount, desc);

            ctx.logCustomEvent("Command Pattern", "Đã thực thi " + transaction.getId());
            ctx.notifyDataChanged();

            UiUtils.showAlert(Alert.AlertType.INFORMATION, "Chuyển khoản thành công", "Giao dịch đã thực hiện qua Command Pattern!",
                    String.format("Chuyển: %s\nĐến tài khoản: %s (%s)\nPhí: %s\nSố dư mới của bạn: %s\n\nCó thể hoàn tác tại tab 'Lịch sử GD' nếu tài khoản nhận còn đủ số dư.",
                            UiUtils.formatVnd(transaction.getAmount()), to.getAccountNumber(), to.getOwnerName(),
                            UiUtils.formatVnd(transaction.getFee()), UiUtils.formatVnd(from.getBalance())));

            txtAmount.clear();
        } catch (IllegalArgumentException | IllegalStateException e) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể chuyển khoản", "Giao dịch chưa thực hiện", e.getMessage());
        }
    }

    private void handleSaveTemplate() {
        Account from = cbFromAccount.getValue();
        Account to = cbToAccount.getValue();
        if (from == null || to == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Chưa đủ thông tin", "Vui lòng chọn tài khoản nguồn và đích trước khi lưu mẫu.");
            return;
        }

        double amount;
        try {
            amount = Money.positive(Double.parseDouble(txtAmount.getText().trim()));
        } catch (IllegalArgumentException e) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể lưu mẫu", "Số tiền không hợp lệ", e.getMessage());
            return;
        }

        String desc = txtDescription.getText().trim();
        savedTemplate = new TransferTemplate(from.getAccountNumber(), to.getAccountNumber(), amount, desc);

        lblTemplateStatus.setText("✔ Đã lưu mẫu gốc: " + savedTemplate.getFromAccountNumber() + " -> " + savedTemplate.getToAccountNumber());
        txtTemplateInspector.setText("// Mẫu gốc (Original Prototype):\n"
                + "TransferTemplate original = new TransferTemplate(\n"
                + "    \"" + savedTemplate.getFromAccountNumber() + "\",\n"
                + "    \"" + savedTemplate.getToAccountNumber() + "\",\n"
                + "    " + savedTemplate.getAmount() + ",\n"
                + "    \"" + savedTemplate.getDescription() + "\"\n"
                + ");\n"
                + "// HashCode: @" + Integer.toHexString(savedTemplate.hashCode()) + "\n"
                + "// Đã sẵn sàng để nhân bản bằng clone()!");

        UiUtils.showAlert(Alert.AlertType.INFORMATION, "Prototype Pattern", "Đã lưu mẫu gốc thành công",
                "Mẫu giao dịch đã được tạo. Bạn có thể bấm 'Nhân bản mẫu (clone())' để tạo bản sao độc lập theo Prototype Pattern.");
    }

    private void handleCloneTemplate() {
        if (savedTemplate == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Chưa có mẫu", "Chưa có mẫu gốc", "Vui lòng tạo và lưu một mẫu giao dịch trước.");
            return;
        }

        // Gọi clone() của Prototype Pattern
        TransferTemplate cloned = savedTemplate.clone();
        cloned.setDescription("Bản sao định kỳ — " + savedTemplate.getDescription());
        try {
            cloned.setAmount(Money.positive(savedTemplate.getAmount() * 1.05)); // Tăng 5% cho bản sao
        } catch (IllegalArgumentException e) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể nhân bản", "Số tiền vượt giới hạn", e.getMessage());
            return;
        }

        txtTemplateInspector.setText("// KẾT QUẢ NHÂN BẢN BẰNG PROTOTYPE PATTERN (clone()):\n\n"
                + "TransferTemplate cloned = original.clone();\n"
                + "cloned.setDescription(\"" + cloned.getDescription() + "\");\n"
                + "cloned.setAmount(" + cloned.getAmount() + ");\n\n"
                + "// Kiểm tra tính chất Prototype:\n"
                + "original == cloned       => " + (savedTemplate == cloned) + " (2 đối tượng độc lập trong bộ nhớ)\n"
                + "original.hashCode()       => @" + Integer.toHexString(savedTemplate.hashCode()) + "\n"
                + "cloned.hashCode()         => @" + Integer.toHexString(cloned.hashCode()) + "\n"
                + "original.getAmount()      => " + savedTemplate.getAmount() + "\n"
                + "cloned.getAmount()        => " + cloned.getAmount() + " (Đã chỉnh sửa không ảnh hưởng mẫu gốc)");

        lblTemplateStatus.setText("✔ Đã clone thành công bản sao mới: @" + Integer.toHexString(cloned.hashCode()));
    }

    private void handleApplyTemplate() {
        if (savedTemplate == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Chưa có mẫu", "Chưa có mẫu", "Vui lòng lưu hoặc nhân bản mẫu trước.");
            return;
        }

        ctx.getAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(savedTemplate.getFromAccountNumber()))
                .findFirst()
                .ifPresent(cbFromAccount::setValue);

        ctx.getAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(savedTemplate.getToAccountNumber()))
                .findFirst()
                .ifPresent(cbToAccount::setValue);

        txtAmount.setText(String.valueOf((long) savedTemplate.getAmount()));
        txtDescription.setText(savedTemplate.getDescription());
    }

    private VBox buildPatternExplainerCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Quy trình phối hợp Design Patterns trong màn hình Chuyển Khoản");
        cardTitle.getStyleClass().add("card-title");

        Label desc = new Label(
                "• Facade Pattern: Cung cấp API đơn giản cho UI, che giấu sự phức tạp của việc debit, credit, kiểm tra số dư và thông báo.\n"
                        + "• Command Pattern: Mỗi giao dịch chuyển tiền là một đối tượng TransferCommand độc lập với 2 phương thức: execute() và undo(). Khi bấm Chuyển, lệnh được đẩy vào TransactionHistory (Deque stack).\n"
                        + "• Prototype Pattern: Cho phép người dùng lưu lại mẫu chuyển khoản và gọi clone() để tạo các giao dịch định kỳ tương tự mà không cần cấu hình lại từ đầu."
        );
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-line-spacing: 4px;");

        card.getChildren().addAll(cardTitle, desc);
        return card;
    }

    public void refresh() {
        Account from = cbFromAccount.getValue();
        Account to = cbToAccount.getValue();

        cbFromAccount.setItems(ctx.getAccounts());
        cbToAccount.setItems(ctx.getAccounts());

        if (from != null) {
            ctx.getAccounts().stream().filter(a -> a.getAccountNumber().equals(from.getAccountNumber())).findFirst().ifPresent(cbFromAccount::setValue);
        } else if (!ctx.getAccounts().isEmpty()) {
            cbFromAccount.setValue(ctx.getAccounts().get(0));
        }

        if (to != null) {
            ctx.getAccounts().stream().filter(a -> a.getAccountNumber().equals(to.getAccountNumber())).findFirst().ifPresent(cbToAccount::setValue);
        } else if (ctx.getAccounts().size() > 1) {
            cbToAccount.setValue(ctx.getAccounts().get(1));
        }

        updateFeePreview();
    }
}

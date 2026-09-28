package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.pattern.creational.TransferTemplate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Màn hình Chuyển khoản ngân hàng.
 * Minh họa Patterns:
 * - Facade Pattern: Đơn giản hóa quy trình chuyển khoản (debit, credit, log, notify).
 * - Command Pattern: Chuyển khoản được đóng gói thành đối tượng TransferCommand, lưu vào TransactionHistory để hỗ trợ Undo.
 * - Prototype Pattern: Tạo mẫu chuyển khoản (TransferTemplate) và nhân bản (clone()) nhanh chóng.
 * - Strategy Pattern: Phí được tính tự động từ FeeStrategy của tài khoản gửi.
 *
 * Tính năng nâng cao:
 * - Validation thời gian thực & TextFormatter chỉ nhận số nguyên.
 * - Autocomplete tìm kiếm tài khoản thụ hưởng theo tên hoặc số tài khoản.
 * - Dialog xác nhận tóm tắt giao dịch trước khi gửi lệnh.
 * - Toast notification phản hồi kết quả mượt mà.
 */
public class TransferView extends VBox {

    private final UIContext ctx = UIContext.getInstance();

    // Form controls
    private final ComboBox<Account> cbFromAccount = new ComboBox<>();
    private final ComboBox<Account> cbToAccount = new ComboBox<>();
    private final TextField txtSearchToAccount = new TextField();
    private final ContextMenu autocompleteMenu = new ContextMenu();
    private final HBox recipientPreviewCard = new HBox(12);

    private final TextField txtAmount = new TextField();
    private final Label lblAmountValidation = new Label();
    private final TextField txtDescription = new TextField("Chuyển tiền qua Banking");
    private final Label lblFromBalance = new Label();
    private final Label lblFeeCalculation = new Label();
    private final Button btnTransfer = new Button("🚀 Chuyển Khoản Ngay (Command + Facade)");

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
        Label subtitle = new Label("Giao dịch chuyển tiền liên tài khoản hỗ trợ Undo (Command), Mẫu giao dịch (Prototype) & Validation thời gian thực");
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
        grid.setVgap(12);

        setupAccountCombo(cbFromAccount, true);
        setupAccountCombo(cbToAccount, false);

        // ── 1. TextFormatter chỉ nhận chữ số ──────────────────
        Pattern digitPattern = Pattern.compile("\\d*");
        txtAmount.setTextFormatter(new TextFormatter<>(change ->
                digitPattern.matcher(change.getControlNewText()).matches() ? change : null
        ));
        txtAmount.setPromptText("Nhập số tiền chuyển (VND)");

        // ── 2. Real-time validation listener ───────────────────
        txtAmount.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        cbFromAccount.valueProperty().addListener((obs, oldVal, newVal) -> validateForm());
        cbToAccount.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRecipientPreview();
            validateForm();
        });

        lblAmountValidation.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        lblFeeCalculation.setStyle("-fx-font-size: 11px; -fx-text-fill: #0369A1; -fx-font-weight: bold;");
        lblFeeCalculation.setText("Phí giao dịch: 0 VND");

        // ── 3. Autocomplete / Search ô người nhận ─────────────
        txtSearchToAccount.setPromptText("🔍 Gõ tìm nhanh số TK hoặc tên người nhận...");
        txtSearchToAccount.textProperty().addListener((obs, oldVal, newVal) -> handleSearchToAccount(newVal));

        VBox toAccountBox = new VBox(6);
        toAccountBox.getChildren().addAll(txtSearchToAccount, cbToAccount, recipientPreviewCard);

        VBox amountBox = new VBox(4);
        amountBox.getChildren().addAll(txtAmount, lblAmountValidation);

        grid.add(new Label("Tài khoản nguồn:"), 0, 0);
        grid.add(cbFromAccount, 1, 0);

        grid.add(new Label("Số dư nguồn:"), 0, 1);
        grid.add(lblFromBalance, 1, 1);

        grid.add(new Label("Tài khoản thụ hưởng:"), 0, 2);
        grid.add(toAccountBox, 1, 2);

        grid.add(new Label("Số tiền chuyển:"), 0, 3);
        grid.add(amountBox, 1, 3);

        grid.add(new Label("Phí giao dịch:"), 0, 4);
        grid.add(lblFeeCalculation, 1, 4);

        grid.add(new Label("Nội dung:"), 0, 5);
        grid.add(txtDescription, 1, 5);

        ColumnConstraints c1 = new ColumnConstraints(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        HBox buttonBox = new HBox(12);
        btnTransfer.getStyleClass().add("btn-primary");
        btnTransfer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnTransfer, Priority.ALWAYS);
        btnTransfer.setOnAction(e -> handleTransferConfirmation());

        Button btnSaveTemplate = new Button("💾 Lưu Mẫu (Prototype)");
        btnSaveTemplate.getStyleClass().add("btn-secondary");
        btnSaveTemplate.setOnAction(e -> handleSaveTemplate());

        buttonBox.getChildren().addAll(btnTransfer, btnSaveTemplate);

        card.getChildren().addAll(cardTitle, grid, buttonBox);
        return card;
    }

    private void handleSearchToAccount(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            autocompleteMenu.hide();
            return;
        }

        String search = keyword.trim().toLowerCase();
        Account from = cbFromAccount.getValue();

        List<Account> matches = ctx.getAccounts().stream()
                .filter(a -> from == null || !a.getAccountNumber().equals(from.getAccountNumber()))
                .filter(a -> a.getAccountNumber().toLowerCase().contains(search)
                        || a.getOwnerName().toLowerCase().contains(search))
                .toList();

        if (matches.isEmpty()) {
            autocompleteMenu.hide();
            return;
        }

        autocompleteMenu.getItems().clear();
        for (Account a : matches) {
            String text = String.format("%s - %s (%s)", a.getAccountNumber(), a.getOwnerName(), a.getType());
            MenuItem item = new MenuItem(text);
            item.setOnAction(e -> {
                cbToAccount.setValue(a);
                txtSearchToAccount.clear();
                autocompleteMenu.hide();
            });
            autocompleteMenu.getItems().add(item);
        }

        if (!autocompleteMenu.isShowing()) {
            autocompleteMenu.show(txtSearchToAccount, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void updateRecipientPreview() {
        recipientPreviewCard.getChildren().clear();
        Account to = cbToAccount.getValue();
        if (to == null) return;

        recipientPreviewCard.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 8 12; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px;");
        recipientPreviewCard.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label("👤 " + to.getOwnerName());
        lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 12px;");

        Label typeBadge = UiUtils.createTypeBadge(to.getType());
        Label statusBadge = UiUtils.createStatusBadge(to.getStatus());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        recipientPreviewCard.getChildren().addAll(lblName, spacer, typeBadge, statusBadge);
    }

    private void validateForm() {
        Account from = cbFromAccount.getValue();
        Account to = cbToAccount.getValue();

        if (from == null) {
            lblAmountValidation.setText("Vui lòng chọn tài khoản nguồn.");
            lblAmountValidation.setStyle("-fx-text-fill: #64748B;");
            txtAmount.setStyle("");
            btnTransfer.setDisable(true);
            return;
        }

        // Kiểm tra nếu tài khoản trùng nhau
        if (to != null && from.getAccountNumber().equals(to.getAccountNumber())) {
            lblAmountValidation.setText("⚠️ Tài khoản nguồn và đích không được trùng nhau!");
            lblAmountValidation.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
            txtAmount.setStyle("-fx-border-color: #EF4444; -fx-border-radius: 8px;");
            btnTransfer.setDisable(true);
            return;
        }

        String amtStr = txtAmount.getText().trim();
        if (amtStr.isEmpty()) {
            lblFeeCalculation.setText("Phí giao dịch: 0 VND");
            lblAmountValidation.setText("Vui lòng nhập số tiền muốn chuyển.");
            lblAmountValidation.setStyle("-fx-text-fill: #64748B;");
            txtAmount.setStyle("");
            btnTransfer.setDisable(true);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (NumberFormatException e) {
            lblAmountValidation.setText("⚠️ Định dạng số tiền không hợp lệ.");
            lblAmountValidation.setStyle("-fx-text-fill: #DC2626;");
            txtAmount.setStyle("-fx-border-color: #EF4444; -fx-border-radius: 8px;");
            btnTransfer.setDisable(true);
            return;
        }

        if (amount <= 0) {
            lblAmountValidation.setText("⚠️ Số tiền phải lớn hơn 0 VND.");
            lblAmountValidation.setStyle("-fx-text-fill: #DC2626;");
            txtAmount.setStyle("-fx-border-color: #EF4444; -fx-border-radius: 8px;");
            btnTransfer.setDisable(true);
            return;
        }

        // Tính phí
        double fee = Money.nonNegative(from.getFeeStrategy().calculateFee(amount));
        lblFeeCalculation.setText(String.format("Phí: %s (%s)",
                UiUtils.formatVnd(fee), from.getFeeStrategy().getName()));

        double totalDebit = amount + fee;

        if (from.getBalance() < totalDebit) {
            lblAmountValidation.setText(String.format(
                    "⚠️ Số dư không đủ! Cần: %s (gồm phí), Khả dụng: %s",
                    UiUtils.formatVnd(totalDebit), UiUtils.formatVnd(from.getBalance())
            ));
            lblAmountValidation.setStyle("-fx-text-fill: #DC2626;");
            txtAmount.setStyle("-fx-border-color: #EF4444; -fx-border-radius: 8px; -fx-focus-color: #EF4444;");
            btnTransfer.setDisable(true);
        } else {
            double remaining = from.getBalance() - totalDebit;
            lblAmountValidation.setText(String.format(
                    "✔ Hợp lệ. Số dư dự kiến còn lại: %s",
                    UiUtils.formatVnd(remaining)
            ));
            lblAmountValidation.setStyle("-fx-text-fill: #15803D;");
            txtAmount.setStyle("-fx-border-color: #10B981; -fx-border-radius: 8px; -fx-focus-color: #10B981;");
            btnTransfer.setDisable(to == null);
        }
    }

    private void handleTransferConfirmation() {
        Account from = cbFromAccount.getValue();
        Account to = cbToAccount.getValue();

        if (from == null || to == null) {
            ToastNotification.showWarning("Vui lòng chọn cả tài khoản nguồn và đích.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(txtAmount.getText().trim());
        } catch (NumberFormatException e) {
            ToastNotification.showError("Số tiền nhập vào không hợp lệ.");
            return;
        }

        double fee = Money.nonNegative(from.getFeeStrategy().calculateFee(amount));
        double totalDebit = amount + fee;
        String desc = txtDescription.getText().trim().isEmpty() ? "Chuyển tiền qua Banking" : txtDescription.getText().trim();

        // ── Modal Xác Nhận Giao Dịch ─────────────────────────
        Dialog<Boolean> confirmModal = new Dialog<>();
        confirmModal.setTitle("Xác nhận chuyển khoản");
        confirmModal.setHeaderText("XÁC NHẬN LỆNH CHUYỂN KHOẢN LIÊN TÀI KHOẢN");

        ButtonType btnConfirm = new ButtonType("✔ Xác nhận chuyển", ButtonBar.ButtonData.OK_DONE);
        confirmModal.getDialogPane().getButtonTypes().addAll(btnConfirm, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(12));

        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(12);
        summaryGrid.setVgap(8);

        summaryGrid.add(new Label("Tài khoản trích tiền:"), 0, 0);
        summaryGrid.add(new Label(from.getAccountNumber() + " (" + from.getOwnerName() + ")"), 1, 0);

        summaryGrid.add(new Label("Tài khoản thụ hưởng:"), 0, 1);
        summaryGrid.add(new Label(to.getAccountNumber() + " (" + to.getOwnerName() + ")"), 1, 1);

        summaryGrid.add(new Label("Số tiền chuyển:"), 0, 2);
        Label lblAmt = new Label(UiUtils.formatVnd(amount));
        lblAmt.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");
        summaryGrid.add(lblAmt, 1, 2);

        summaryGrid.add(new Label("Phí giao dịch:"), 0, 3);
        summaryGrid.add(new Label(UiUtils.formatVnd(fee) + " (" + from.getFeeStrategy().getName() + ")"), 1, 3);

        summaryGrid.add(new Label("Tổng tiền trích nợ:"), 0, 4);
        Label lblTotal = new Label(UiUtils.formatVnd(totalDebit));
        lblTotal.setStyle("-fx-font-weight: bold; -fx-text-fill: #DC2626; -fx-font-size: 14px;");
        summaryGrid.add(lblTotal, 1, 4);

        summaryGrid.add(new Label("Nội dung chuyển:"), 0, 5);
        summaryGrid.add(new Label(desc), 1, 5);

        Label note = new Label("💡 Giao dịch này được ghi vào TransactionHistory (Command Pattern) và hỗ trợ hoàn tác Undo.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        content.getChildren().addAll(summaryGrid, new Separator(), note);
        confirmModal.getDialogPane().setContent(content);

        confirmModal.getDialogPane().getStylesheets().add(UiUtils.class.getResource("/com/banking/ui/app.css").toExternalForm());
        Button okButton = (Button) confirmModal.getDialogPane().lookupButton(btnConfirm);
        okButton.getStyleClass().add("btn-primary");

        confirmModal.setResultConverter(b -> b == btnConfirm);

        confirmModal.showAndWait().ifPresent(confirmed -> {
            if (confirmed) {
                executeTransfer(from, to, amount, desc);
            }
        });
    }

    private void executeTransfer(Account from, Account to, double amount, String desc) {
        try {
            Transaction transaction = ctx.getFacade().transfer(
                    from.getAccountNumber(), to.getAccountNumber(), amount, desc);

            ctx.logCustomEvent("Command Pattern", "Đã thực thi " + transaction.getId());
            ctx.notifyDataChanged();

            ToastNotification.showSuccess(String.format("Chuyển thành công %s đến TK %s!",
                    UiUtils.formatVnd(amount), to.getAccountNumber()));

            txtAmount.clear();
            validateForm();
        } catch (Exception e) {
            String friendlyError = UiUtils.humanizeError(e);
            ToastNotification.showError(friendlyError);
            UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể chuyển khoản", null, friendlyError);
        }
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

    private void handleSaveTemplate() {
        Account from = cbFromAccount.getValue();
        Account to = cbToAccount.getValue();
        if (from == null || to == null) {
            ToastNotification.showWarning("Vui lòng chọn tài khoản nguồn và đích trước khi lưu mẫu.");
            return;
        }

        double amount;
        try {
            amount = Money.positive(Double.parseDouble(txtAmount.getText().trim()));
        } catch (IllegalArgumentException e) {
            ToastNotification.showError("Số tiền không hợp lệ để lưu mẫu.");
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

        ToastNotification.showSuccess("Đã lưu mẫu gốc thành công (Prototype Pattern)!");
    }

    private void handleCloneTemplate() {
        if (savedTemplate == null) {
            ToastNotification.showWarning("Chưa có mẫu gốc! Vui lòng lưu một mẫu trước.");
            return;
        }

        TransferTemplate cloned = savedTemplate.clone();
        cloned.setDescription("Bản sao định kỳ — " + savedTemplate.getDescription());
        try {
            cloned.setAmount(Money.positive(savedTemplate.getAmount() * 1.05));
        } catch (IllegalArgumentException e) {
            cloned.setAmount(savedTemplate.getAmount());
        }

        lblTemplateStatus.setText("✔ Đã nhân bản (clone()): Bản sao có số tiền +5% và nội dung mới.");
        txtTemplateInspector.setText("// Bản sao độc lập được sinh ra từ clone():\n"
                + "TransferTemplate cloned = original.clone();\n"
                + "cloned.setAmount(" + cloned.getAmount() + "); // Tăng 5%\n"
                + "// HashCode Cloned: @" + Integer.toHexString(cloned.hashCode()) + " != Original: @" + Integer.toHexString(savedTemplate.hashCode()) + "\n"
                + "// Thay đổi trên bản sao KHÔNG ảnh hưởng mẫu gốc!");

        savedTemplate = cloned;
        ToastNotification.showInfo("Nhân bản thành công qua clone()!");
    }

    private void handleApplyTemplate() {
        if (savedTemplate == null) {
            ToastNotification.showWarning("Chưa có mẫu nào được lưu.");
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
        ToastNotification.showSuccess("Đã áp dụng mẫu giao dịch vào Form!");
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

        updateRecipientPreview();
        validateForm();
    }

    public void selectFromAccount(String accNo) {
        if (accNo == null) return;
        ctx.getAccounts().stream()
                .filter(a -> a.getAccountNumber().equalsIgnoreCase(accNo))
                .findFirst()
                .ifPresent(cbFromAccount::setValue);
    }
}

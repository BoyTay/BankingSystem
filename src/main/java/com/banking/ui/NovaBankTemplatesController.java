package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.pattern.creational.TransferTemplate;
import com.banking.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Controller chuyên biệt cho Quản lý Danh bạ Mẫu & Lệnh Định Kỳ (Template Hub).
 * Minh họa Patterns:
 * - Prototype Pattern: Quản lý thư viện mẫu (TransferTemplate) và nhân bản độc lập qua clone().
 * - Strategy Pattern: Ước tính biểu phí giao dịch cho mẫu theo loại tài khoản.
 * - Navigation: Điều hướng mượt mà sang Transfer & Pay khi người dùng chọn "⚡ Chuyển ngay".
 */
public class NovaBankTemplatesController implements Initializable, NovaBankNavigable {

    private final UIContext ctx = UIContext.getInstance();
    private Consumer<String> navigator = key -> { };

    // Form fields (Tạo Mẫu Mới)
    @FXML private TextField txtTemplateDescription;
    @FXML private ComboBox<Account> cbFromAccount;
    @FXML private Label lblFromBalance;
    @FXML private TextField txtSearchToAccount;
    @FXML private ComboBox<Account> cbToAccount;
    @FXML private HBox boxRecipientPreview;
    @FXML private StackPane paneRecipientAvatar;
    @FXML private Label lblRecipientAvatarText;
    @FXML private Label lblRecipientName;
    @FXML private Label lblRecipientAccountType;
    @FXML private TextField txtAmount;
    @FXML private Label lblFeeCalculation;
    @FXML private Label lblAmountValidation;
    @FXML private Button btnSaveTemplate;

    // Header & Gallery fields
    @FXML private TextField txtSearchTemplates;
    @FXML private Label lblTemplateCountBadge;
    @FXML private VBox boxTemplatesList;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileRole;
    @FXML private Label lblProfileAvatar;

    // Architecture Pipeline collapsible
    @FXML private VBox boxPipelineContent;
    @FXML private Button btnTogglePipeline;

    private final List<TransferTemplate> templates = new ArrayList<>();
    private final ContextMenu autocompleteMenu = new ContextMenu();
    private String templateSearchFilter = "";

    public void setUser(AuthService.User user) {
        if (user == null) return;
        String name = user.username();
        String display = name.substring(0, 1).toUpperCase() + name.substring(1);
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
    public void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupAccountCombos();
        setupSearchAutocomplete();
        setupAmountValidation();
        setupTemplateSearch();
        seedDefaultTemplatesIfEmpty();
        renderTemplatesList();

        ctx.addDataChangeListener(() -> Platform.runLater(this::refreshData));
    }

    private void refreshData() {
        populateAccounts();
        validateForm();
    }

    private void setupTemplateSearch() {
        if (txtSearchTemplates != null) {
            txtSearchTemplates.textProperty().addListener((obs, oldVal, newVal) -> {
                templateSearchFilter = newVal != null ? newVal.trim().toLowerCase() : "";
                renderTemplatesList();
            });
        }
    }

    private void setupAccountCombos() {
        populateAccounts();

        cbFromAccount.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getAccountNumber() + " - " + item.getOwnerName() + " (" + item.getType() + ")");
            }
        });
        cbFromAccount.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getAccountNumber() + " - " + item.getOwnerName() + " (" + item.getType() + ")");
            }
        });

        cbToAccount.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getAccountNumber() + " - " + item.getOwnerName() + " (" + item.getType() + ")");
            }
        });
        cbToAccount.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getAccountNumber() + " - " + item.getOwnerName() + " (" + item.getType() + ")");
            }
        });

        cbFromAccount.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblFromBalance.setText("Số dư khả dụng: " + UiUtils.formatVnd(newVal.getBalance()) + " (" + newVal.getStatus() + ")");
            } else {
                lblFromBalance.setText("Số dư khả dụng: 0 VND");
            }
            validateForm();
        });

        cbToAccount.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRecipientPreview(newVal);
            validateForm();
        });
    }

    private void populateAccounts() {
        List<Account> accounts = ctx.getAccounts();
        Account prevFrom = cbFromAccount.getValue();
        Account prevTo = cbToAccount.getValue();

        cbFromAccount.getItems().setAll(accounts);
        cbToAccount.getItems().setAll(accounts);

        if (prevFrom != null) {
            accounts.stream().filter(a -> a.getAccountNumber().equals(prevFrom.getAccountNumber()))
                    .findFirst().ifPresent(cbFromAccount::setValue);
        } else if (!accounts.isEmpty()) {
            cbFromAccount.setValue(accounts.get(0));
        }

        if (prevTo != null) {
            accounts.stream().filter(a -> a.getAccountNumber().equals(prevTo.getAccountNumber()))
                    .findFirst().ifPresent(cbToAccount::setValue);
        } else if (accounts.size() > 1) {
            cbToAccount.setValue(accounts.get(1));
        }
    }

    private void updateRecipientPreview(Account recipient) {
        if (recipient == null) {
            boxRecipientPreview.setVisible(false);
            boxRecipientPreview.setManaged(false);
            return;
        }

        boxRecipientPreview.setVisible(true);
        boxRecipientPreview.setManaged(true);
        lblRecipientName.setText(recipient.getOwnerName() + " (" + recipient.getAccountNumber() + ")");
        lblRecipientAccountType.setText(recipient.getType() + " · " + (recipient.getStatus().name()));

        String name = recipient.getOwnerName().trim();
        String initials = "NB";
        if (!name.isEmpty()) {
            String[] parts = name.split("\\s+");
            initials = parts.length > 1
                    ? (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase()
                    : name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
        lblRecipientAvatarText.setText(initials);
    }

    private void setupSearchAutocomplete() {
        txtSearchToAccount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                autocompleteMenu.hide();
                return;
            }

            String search = newVal.trim().toLowerCase();
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
                MenuItem item = new MenuItem(a.getAccountNumber() + " - " + a.getOwnerName() + " (" + a.getType() + ")");
                item.setOnAction(e -> {
                    cbToAccount.setValue(a);
                    txtSearchToAccount.clear();
                    autocompleteMenu.hide();
                });
                autocompleteMenu.getItems().add(item);
            }

            if (!autocompleteMenu.isShowing() && txtSearchToAccount.getScene() != null) {
                autocompleteMenu.show(txtSearchToAccount, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });
    }

    private void setupAmountValidation() {
        Pattern digitPattern = Pattern.compile("\\d*");
        txtAmount.setTextFormatter(new TextFormatter<>(change ->
                digitPattern.matcher(change.getControlNewText()).matches() ? change : null
        ));

        txtAmount.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    }

    private void validateForm() {
        Account from = cbFromAccount.getValue();
        Account to = cbToAccount.getValue();

        if (from == null) {
            lblAmountValidation.setText("Vui lòng chọn tài khoản nguồn.");
            lblAmountValidation.setStyle("-fx-text-fill: #64748B;");
            btnSaveTemplate.setDisable(true);
            return;
        }

        if (to != null && from.getAccountNumber().equals(to.getAccountNumber())) {
            lblAmountValidation.setText("⚠️ Nguồn và đích không được trùng!");
            lblAmountValidation.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
            btnSaveTemplate.setDisable(true);
            return;
        }

        String amtStr = txtAmount.getText().trim();
        if (amtStr.isEmpty()) {
            if (lblFeeCalculation != null) lblFeeCalculation.setText("Phí ước tính: 0 VND");
            lblAmountValidation.setText("Vui lòng nhập số tiền mẫu.");
            lblAmountValidation.setStyle("-fx-text-fill: #64748B;");
            btnSaveTemplate.setDisable(true);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (NumberFormatException e) {
            lblAmountValidation.setText("⚠️ Định dạng không hợp lệ.");
            lblAmountValidation.setStyle("-fx-text-fill: #DC2626;");
            btnSaveTemplate.setDisable(true);
            return;
        }

        if (amount <= 0) {
            lblAmountValidation.setText("⚠️ Số tiền phải lớn hơn 0.");
            lblAmountValidation.setStyle("-fx-text-fill: #DC2626;");
            btnSaveTemplate.setDisable(true);
            return;
        }

        double fee = Money.nonNegative(from.getFeeStrategy().calculateFee(amount));
        if (lblFeeCalculation != null) {
            lblFeeCalculation.setText(String.format("Phí ước tính: %s (%s)",
                    UiUtils.formatVnd(fee), from.getFeeStrategy().getName()));
        }

        lblAmountValidation.setText("✔ Mẫu hợp lệ");
        lblAmountValidation.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        btnSaveTemplate.setDisable(to == null);
    }

    // Quick Amount Chips
    @FXML private void handleQuickAmount100k() { addAmount(100_000); }
    @FXML private void handleQuickAmount500k() { addAmount(500_000); }
    @FXML private void handleQuickAmount2m()   { addAmount(2_000_000); }
    @FXML private void handleQuickAmount5m()   { addAmount(5_000_000); }

    @FXML private void handleQuickAmountAll() {
        Account from = cbFromAccount.getValue();
        if (from != null) {
            long max = (long) Math.max(0, from.getBalance());
            txtAmount.setText(String.valueOf(max));
        }
    }

    private void addAmount(long delta) {
        long current = 0;
        try {
            if (!txtAmount.getText().trim().isEmpty()) {
                current = Long.parseLong(txtAmount.getText().trim());
            }
        } catch (NumberFormatException ignored) { }
        txtAmount.setText(String.valueOf(current + delta));
    }

    // ── PROTOTYPE PATTERN LOGIC ──
    private void seedDefaultTemplatesIfEmpty() {
        if (!templates.isEmpty()) return;
        List<Account> accounts = ctx.getAccounts();
        if (accounts.size() >= 2) {
            String acc1 = accounts.get(0).getAccountNumber();
            String acc2 = accounts.get(1).getAccountNumber();
            templates.add(new TransferTemplate(acc1, acc2, 3_500_000, "Tiền thuê căn hộ tháng"));
            templates.add(new TransferTemplate(acc1, acc2, 1_200_000, "Học phí và khóa học trực tuyến"));
            templates.add(new TransferTemplate(acc1, acc2, 500_000, "Gửi tiền sinh hoạt gia đình"));
        }
    }

    @FXML
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
            ToastNotification.showError("Số tiền phải lớn hơn 0 VND để lưu mẫu.");
            return;
        }

        String desc = (txtTemplateDescription != null) ? txtTemplateDescription.getText().trim() : "";
        if (desc.isEmpty()) {
            desc = "Chuyển cho " + to.getOwnerName();
        }

        TransferTemplate template = new TransferTemplate(from.getAccountNumber(), to.getAccountNumber(), amount, desc);
        templates.add(0, template);
        renderTemplatesList();

        if (txtTemplateDescription != null) {
            txtTemplateDescription.clear();
        }

        ToastNotification.showSuccess("Đã lưu mẫu giao dịch thành công!");
    }

    @FXML
    private void handleFocusNewTemplate() {
        if (txtTemplateDescription != null) {
            txtTemplateDescription.clear();
            txtTemplateDescription.requestFocus();
        } else {
            txtAmount.clear();
            txtAmount.requestFocus();
        }
    }

    private void handleCloneTemplate(TransferTemplate original) {
        // Cốt lõi của Prototype Pattern: clone()
        TransferTemplate cloned = original.clone();

        // Đánh dấu bản sao rõ ràng để người dùng nhận biết
        cloned.setDescription(original.getDescription() + " (Bản sao)");

        templates.add(0, cloned);
        renderTemplatesList();

        ToastNotification.showSuccess("Đã nhân bản mẫu '" + original.getDescription() + "' thành công!");
    }

    private void handleTransferWithTemplate(TransferTemplate t) {
        ctx.setPendingTransferTemplate(t);
        ToastNotification.showSuccess("Đang chuyển tiếp sang màn hình Chuyển Khoản với mẫu '" + t.getDescription() + "'...");
        navigator.accept("transfer");
    }

    private void handleDeleteTemplate(TransferTemplate t) {
        templates.remove(t);
        renderTemplatesList();
        ToastNotification.showInfo("Đã xóa mẫu khỏi danh sách.");
    }

    @FXML
    private void handleTogglePipeline() {
        if (boxPipelineContent != null) {
            boolean isVisible = boxPipelineContent.isVisible();
            boxPipelineContent.setVisible(!isVisible);
            boxPipelineContent.setManaged(!isVisible);
            if (btnTogglePipeline != null) {
                btnTogglePipeline.setText(!isVisible ? "Thu gọn ▲" : "Mở rộng ▼");
            }
        }
    }

    private void renderTemplatesList() {
        List<TransferTemplate> filtered = templates.stream()
                .filter(t -> {
                    if (templateSearchFilter == null || templateSearchFilter.isEmpty()) return true;
                    return (t.getDescription() != null && t.getDescription().toLowerCase().contains(templateSearchFilter))
                            || t.getFromAccountNumber().toLowerCase().contains(templateSearchFilter)
                            || t.getToAccountNumber().toLowerCase().contains(templateSearchFilter);
                })
                .toList();

        lblTemplateCountBadge.setText(filtered.size() + " mẫu");
        boxTemplatesList.getChildren().clear();

        if (filtered.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 36; -fx-background-color: #F8FAFC; -fx-background-radius: 12px; -fx-border-color: #E2E8F0; -fx-border-radius: 12px;");

            SVGPath emptyIcon = new SVGPath();
            emptyIcon.setContent("M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z");
            emptyIcon.setStyle("-fx-fill: transparent; -fx-stroke: #94A3B8; -fx-stroke-width: 1.6;");
            emptyIcon.setScaleX(1.2);
            emptyIcon.setScaleY(1.2);

            Label lblEmpty = new Label(templateSearchFilter.isEmpty() ? "Chưa có mẫu nào được lưu." : "Không tìm thấy mẫu phù hợp.");
            lblEmpty.setStyle("-fx-text-fill: #475569; -fx-font-weight: 700; -fx-font-size: 13px;");
            Label lblHint = new Label(templateSearchFilter.isEmpty() ? "Điền thông tin ở biểu mẫu bên trái rồi bấm 'Lưu Mẫu Giao Dịch'." : "Thử tìm kiếm với từ khóa khác.");
            lblHint.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
            emptyBox.getChildren().addAll(emptyIcon, lblEmpty, lblHint);
            boxTemplatesList.getChildren().add(emptyBox);
            return;
        }

        for (TransferTemplate t : filtered) {
            VBox itemCard = new VBox(12);
            itemCard.getStyleClass().add("template-item-card");

            // --- Hàng trên: Icon danh mục + Tên mẫu & Lộ trình + Nút Xóa đồng bộ ---
            HBox topRow = new HBox(12);
            topRow.setAlignment(Pos.CENTER_LEFT);

            // Icon danh mục mẫu với màu sắc tinh tế
            StackPane iconBox = new StackPane();
            iconBox.getStyleClass().add("template-icon-circle");
            String catColor = getCategoryColor(t.getDescription());
            String catBg = getCategoryBg(t.getDescription());
            iconBox.setStyle("-fx-background-color: " + catBg + "; -fx-border-color: transparent;");

            SVGPath svg = new SVGPath();
            svg.setContent(getCategorySvg(t.getDescription()));
            svg.setStyle("-fx-fill: transparent; -fx-stroke: " + catColor + "; -fx-stroke-width: 1.8; -fx-stroke-line-cap: round; -fx-stroke-line-join: round;");
            iconBox.getChildren().add(svg);

            // Tên gợi nhớ & Lộ trình chuyển khoản
            VBox infoBox = new VBox(3);
            HBox.setHgrow(infoBox, Priority.ALWAYS);
            Label lblDesc = new Label(t.getDescription());
            lblDesc.setStyle("-fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-size: 14px;");

            String toOwner = ctx.getAccounts().stream()
                    .filter(a -> a.getAccountNumber().equals(t.getToAccountNumber()))
                    .map(Account::getOwnerName)
                    .findFirst().orElse("");

            String routeText = "Từ TK " + t.getFromAccountNumber() + "  →  Đến TK " + t.getToAccountNumber()
                    + (toOwner.isEmpty() ? "" : " (" + toOwner + ")");
            Label lblRoute = new Label(routeText);
            lblRoute.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

            infoBox.getChildren().addAll(lblDesc, lblRoute);

            // Nút Xóa (Đồng bộ thiết kế icon thùng rác, kích thước 32x32px, hover đỏ)
            Button btnDelete = new Button();
            btnDelete.getStyleClass().add("btn-template-delete");
            btnDelete.setTooltip(new Tooltip("Xóa mẫu giao dịch này"));

            SVGPath trashSvg = new SVGPath();
            trashSvg.setContent("M3 6h18 M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2 M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6 M10 11v6 M14 11v6");
            trashSvg.setStyle("-fx-fill: transparent; -fx-stroke: #94A3B8; -fx-stroke-width: 1.6;");
            trashSvg.setScaleX(0.7);
            trashSvg.setScaleY(0.7);
            btnDelete.setGraphic(trashSvg);

            btnDelete.hoverProperty().addListener((obs, oldVal, isHover) -> {
                if (isHover) {
                    trashSvg.setStyle("-fx-fill: transparent; -fx-stroke: #EF4444; -fx-stroke-width: 1.8;");
                } else {
                    trashSvg.setStyle("-fx-fill: transparent; -fx-stroke: #94A3B8; -fx-stroke-width: 1.6;");
                }
            });
            btnDelete.setOnAction(e -> handleDeleteTemplate(t));

            topRow.getChildren().addAll(iconBox, infoBox, btnDelete);

            // --- Đường phân cách mảnh tinh tế ---
            Region divider = new Region();
            divider.setStyle("-fx-background-color: #F1F5F9; -fx-pref-height: 1px; -fx-max-height: 1px;");

            // --- Hàng dưới: Số tiền nổi bật + Cụm 2 nút hành động (Sao chép & Chuyển ngay) ---
            HBox bottomRow = new HBox(10);
            bottomRow.setAlignment(Pos.CENTER_LEFT);

            Label lblAmount = new Label(UiUtils.formatVnd(t.getAmount()));
            lblAmount.setStyle("-fx-font-weight: 800; -fx-text-fill: #1D4ED8; -fx-font-size: 15px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Nút Sao chép (Prototype clone)
            Button btnClone = new Button("Sao chép");
            btnClone.getStyleClass().add("btn-template-clone");
            btnClone.setTooltip(new Tooltip("Nhân bản mẫu này (Prototype Pattern)"));

            SVGPath copySvg = new SVGPath();
            copySvg.setContent("M8 7v8a2 2 0 002 2h6M8 7V5a2 2 0 012-2h4.586a1 1 0 01.707.293l4.414 4.414a1 1 0 01.293.707V15a2 2 0 01-2 2h-2M8 7H6a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2v-2");
            copySvg.setStyle("-fx-fill: transparent; -fx-stroke: #475569; -fx-stroke-width: 1.5;");
            copySvg.setScaleX(0.7);
            copySvg.setScaleY(0.7);
            btnClone.setGraphic(copySvg);

            btnClone.hoverProperty().addListener((obs, oldVal, isHover) -> {
                if (isHover) {
                    copySvg.setStyle("-fx-fill: transparent; -fx-stroke: #7C3AED; -fx-stroke-width: 1.6;");
                } else {
                    copySvg.setStyle("-fx-fill: transparent; -fx-stroke: #475569; -fx-stroke-width: 1.5;");
                }
            });
            btnClone.setOnAction(e -> handleCloneTemplate(t));

            // Nút Chuyển ngay
            Button btnQuickTransfer = new Button("⚡ Chuyển ngay");
            btnQuickTransfer.getStyleClass().add("btn-template-apply");
            btnQuickTransfer.setTooltip(new Tooltip("Chuyển tiền với mẫu này"));
            btnQuickTransfer.setOnAction(e -> handleTransferWithTemplate(t));

            bottomRow.getChildren().addAll(lblAmount, spacer, btnClone, btnQuickTransfer);

            itemCard.getChildren().addAll(topRow, divider, bottomRow);
            boxTemplatesList.getChildren().add(itemCard);
        }
    }

    private String getCategorySvg(String desc) {
        String lower = desc != null ? desc.toLowerCase() : "";
        if (lower.contains("nhà") || lower.contains("thuê") || lower.contains("phòng") || lower.contains("căn hộ")) {
            return "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6";
        } else if (lower.contains("học") || lower.contains("trường") || lower.contains("khóa") || lower.contains("sách")) {
            return "M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14zm-4 6v-7.5l4-2.222";
        } else if (lower.contains("sinh hoạt") || lower.contains("ăn") || lower.contains("uống") || lower.contains("gia đình") || lower.contains("bố") || lower.contains("mẹ")) {
            return "M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z";
        } else {
            return "M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z";
        }
    }

    private String getCategoryColor(String desc) {
        String lower = desc != null ? desc.toLowerCase() : "";
        if (lower.contains("nhà") || lower.contains("thuê") || lower.contains("phòng") || lower.contains("căn hộ")) {
            return "#2563EB"; // Blue
        } else if (lower.contains("học") || lower.contains("trường") || lower.contains("khóa") || lower.contains("sách")) {
            return "#7C3AED"; // Purple
        } else if (lower.contains("sinh hoạt") || lower.contains("ăn") || lower.contains("uống") || lower.contains("gia đình") || lower.contains("bố") || lower.contains("mẹ")) {
            return "#059669"; // Emerald
        } else {
            return "#0284C7"; // Sky Blue
        }
    }

    private String getCategoryBg(String desc) {
        String lower = desc != null ? desc.toLowerCase() : "";
        if (lower.contains("nhà") || lower.contains("thuê") || lower.contains("phòng") || lower.contains("căn hộ")) {
            return "rgba(37, 99, 235, 0.1)";
        } else if (lower.contains("học") || lower.contains("trường") || lower.contains("khóa") || lower.contains("sách")) {
            return "rgba(124, 58, 237, 0.1)";
        } else if (lower.contains("sinh hoạt") || lower.contains("ăn") || lower.contains("uống") || lower.contains("gia đình") || lower.contains("bố") || lower.contains("mẹ")) {
            return "rgba(5, 150, 105, 0.1)";
        } else {
            return "rgba(2, 132, 199, 0.1)";
        }
    }
}

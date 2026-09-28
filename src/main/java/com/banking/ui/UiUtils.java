package com.banking.ui;

import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class UiUtils {

    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY); // Uses dot for thousands
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    public static String formatVnd(double amount) {
        return CURRENCY_FORMAT.format(amount) + " VND";
    }

    public static String formatVndCompact(double amount) {
        if (amount >= 1_000_000_000) {
            return String.format(Locale.US, "%.1fB VND", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            return String.format(Locale.US, "%.1fM VND", amount / 1_000_000);
        } else if (amount >= 1_000) {
            return String.format(Locale.US, "%.0fK VND", amount / 1_000);
        } else {
            return formatVnd(amount);
        }
    }

    public static Label createPatternBadge(String patternName, String group) {
        Label label = new Label("Pattern: " + patternName);
        label.getStyleClass().add("badge-pattern");
        switch (group.toLowerCase()) {
            case "creational" -> label.getStyleClass().add("badge-creational");
            case "structural" -> label.getStyleClass().add("badge-structural");
            case "behavioral" -> label.getStyleClass().add("badge-behavioral");
            default -> label.getStyleClass().add("badge-pattern");
        }
        return label;
    }

    public static Label createStatusBadge(AccountStatus status) {
        Label label = new Label(status == AccountStatus.ACTIVE ? "● Đang hoạt động" : "🔒 Đã khóa");
        label.getStyleClass().add(status == AccountStatus.ACTIVE ? "badge-status-active" : "badge-status-locked");
        return label;
    }

    public static Label createTypeBadge(AccountType type) {
        Label label = new Label(type.name());
        switch (type) {
            case PREMIUM -> label.getStyleClass().add("badge-type-premium");
            case SAVINGS -> label.getStyleClass().add("badge-type-savings");
            default -> label.getStyleClass().add("badge-type-standard");
        }
        return label;
    }

    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(
                UiUtils.class.getResource("/com/banking/ui/app.css").toExternalForm()
        );
        alert.showAndWait();
    }

    public static String humanizeError(Throwable t) {
        if (t == null) return "Đã xảy ra lỗi không xác định. Vui lòng thử lại.";

        while ((t instanceof java.lang.reflect.InvocationTargetException || t.getClass().equals(RuntimeException.class))
                && t.getCause() != null) {
            t = t.getCause();
        }

        String msg = t.getMessage() != null ? t.getMessage() : "";

        if (t instanceof SecurityException) {
            String roleHint = msg.contains("READONLY") ? " (vai trò READONLY chỉ có quyền xem)" : " (vai trò bị giới hạn)";
            return "Quyền truy cập bị từ chối: Bạn không có quyền thực hiện thao tác này" + roleHint + ". Vui lòng liên hệ Quản trị viên để nâng cấp quyền.";
        }
        if (msg.contains("Số dư không đủ") || msg.contains("không đủ")) {
            return "Số dư tài khoản không đủ: Tài khoản nguồn không đủ tiền để chuyển và trả phí giao dịch. Vui lòng kiểm tra lại số dư.";
        }
        if (msg.contains("LockedState") || msg.contains("KHÓA") || msg.contains("bị khóa")) {
            return "Tài khoản đang tạm khóa: Tài khoản này đang được bảo vệ ở trạng thái khóa (LockedState). Vui lòng mở khóa tài khoản trước khi thực hiện.";
        }
        if (msg.contains("Hoàn tác") || msg.contains("hoàn tác")) {
            return "Không thể hoàn tác giao dịch: " + msg;
        }
        if (t instanceof IllegalArgumentException) {
            if (msg.contains("greater than zero") || msg.contains("lớn hơn 0") || msg.contains("positive")) {
                return "Số tiền không hợp lệ: Vui lòng nhập số tiền lớn hơn 0 VND.";
            }
            return "Thông tin không hợp lệ: " + msg;
        }
        if (t instanceof IllegalStateException) {
            return "Thao tác không thể hoàn tất: " + msg;
        }
        return msg.isBlank() ? "Đã xảy ra lỗi trong quá trình xử lý giao dịch. Vui lòng thử lại." : msg;
    }

    public static void runAsyncWithLoading(javafx.scene.layout.StackPane rootPane, String message, Runnable backgroundTask, Runnable onSuccess, java.util.function.Consumer<Throwable> onError) {
        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.45);");
        overlay.setAlignment(Pos.CENTER);

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(340);
        box.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 24 32; -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 16, 0, 0, 4);");

        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(44, 44);

        Label lbl = new Label(message != null ? message : "Đang xử lý giao dịch an toàn...");
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 13px;");

        box.getChildren().addAll(spinner, lbl);
        overlay.getChildren().add(box);

        if (rootPane != null) {
            rootPane.getChildren().add(overlay);
        }

        new Thread(() -> {
            try {
                backgroundTask.run();
                javafx.application.Platform.runLater(() -> {
                    if (rootPane != null) rootPane.getChildren().remove(overlay);
                    if (onSuccess != null) onSuccess.run();
                });
            } catch (Throwable t) {
                javafx.application.Platform.runLater(() -> {
                    if (rootPane != null) rootPane.getChildren().remove(overlay);
                    if (onError != null) onError.accept(t);
                });
            }
        }).start();
    }
}

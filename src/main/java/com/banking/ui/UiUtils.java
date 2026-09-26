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
}

package com.banking.ui;

import com.banking.service.AuthService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Arrays;

final class LoginDialog {
    private LoginDialog() { }

    static AuthService.User show(AuthService auth) {
        boolean setup = auth.needsSetup();
        Dialog<AuthService.User> dialog = new Dialog<>();
        dialog.setTitle(setup ? "Thiết lập quản trị viên" : "Đăng nhập VietBank");
        dialog.setHeaderText(setup ? "Tạo tài khoản quản trị đầu tiên" : "Đăng nhập để sử dụng ứng dụng");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField username = new TextField();
        username.setPromptText("Tên đăng nhập");
        PasswordField password = new PasswordField();
        password.setPromptText("Mật khẩu");
        Label error = new Label();
        error.setStyle("-fx-text-fill: #DC2626;");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.setPadding(new Insets(12));
        form.addRow(0, new Label("Tài khoản:"), username);
        form.addRow(1, new Label("Mật khẩu:"), password);
        form.add(error, 0, 2, 2, 1);
        dialog.getDialogPane().setContent(form);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        final AuthService.User[] authenticated = new AuthService.User[1];
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            char[] secret = password.getText().toCharArray();
            try {
                if (setup) {
                    auth.createFirstAdmin(username.getText().trim(), secret);
                }
                authenticated[0] = auth.authenticate(username.getText(), secret);
                if (authenticated[0] == null) {
                    error.setText("Sai tài khoản hoặc mật khẩu.");
                    event.consume();
                }
            } catch (IllegalArgumentException | IllegalStateException exception) {
                error.setText(exception.getMessage());
                event.consume();
            } finally {
                Arrays.fill(secret, '\0');
            }
        });
        dialog.setResultConverter(button -> button == ButtonType.OK ? authenticated[0] : null);
        return dialog.showAndWait().orElse(null);
    }
}

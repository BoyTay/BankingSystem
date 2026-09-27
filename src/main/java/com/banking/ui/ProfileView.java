package com.banking.ui;

import com.banking.service.AuthService;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

import java.util.Arrays;

final class ProfileView extends VBox {
    ProfileView(AuthService auth, AuthService.User user) {
        setSpacing(16);
        getStyleClass().add("content-pane");
        Label title = new Label("Tài khoản đăng nhập");
        title.getStyleClass().add("page-title");
        Label identity = new Label(user.username() + " — " + user.role());
        PasswordField oldPassword = new PasswordField();
        oldPassword.setPromptText("Mật khẩu hiện tại");
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("Mật khẩu mới, tối thiểu 8 ký tự");
        Button change = new Button("Đổi mật khẩu");
        change.getStyleClass().add("btn-primary");
        change.setOnAction(event -> {
            char[] oldSecret = oldPassword.getText().toCharArray();
            char[] newSecret = newPassword.getText().toCharArray();
            try {
                auth.changePassword(user.username(), oldSecret, newSecret);
                UiUtils.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đổi mật khẩu", null);
                oldPassword.clear();
                newPassword.clear();
            } catch (IllegalArgumentException | IllegalStateException exception) {
                UiUtils.showAlert(Alert.AlertType.WARNING, "Không đổi được mật khẩu", null, exception.getMessage());
            } finally {
                Arrays.fill(oldSecret, '\0');
                Arrays.fill(newSecret, '\0');
            }
        });
        VBox card = new VBox(12, identity, oldPassword, newPassword, change);
        card.getStyleClass().add("card");
        card.setMaxWidth(460);
        getChildren().addAll(title, card);
    }
}

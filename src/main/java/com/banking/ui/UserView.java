package com.banking.ui;

import com.banking.service.AuthService;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Arrays;

/** Admin-only local operator management. */
final class UserView extends VBox {
    UserView(AuthService auth) {
        setSpacing(16);
        getStyleClass().add("content-pane");
        Label title = new Label("Quản lý người dùng");
        title.getStyleClass().add("page-title");
        Label help = new Label("ADMIN quản lý tài khoản; STAFF thực hiện giao dịch; VIEWER chỉ xem dữ liệu.");
        help.getStyleClass().add("page-subtitle");
        TextField username = new TextField();
        username.setPromptText("Tên đăng nhập mới");
        PasswordField password = new PasswordField();
        password.setPromptText("Mật khẩu tối thiểu 8 ký tự");
        ComboBox<AuthService.Role> role = new ComboBox<>();
        role.getItems().addAll(AuthService.Role.STAFF, AuthService.Role.VIEWER, AuthService.Role.ADMIN);
        role.setValue(AuthService.Role.STAFF);
        Button create = new Button("Tạo người dùng");
        create.getStyleClass().add("btn-primary");
        create.setOnAction(event -> {
            char[] secret = password.getText().toCharArray();
            try {
                auth.createUser(username.getText().trim(), secret, role.getValue());
                UiUtils.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tạo người dùng", username.getText().trim());
                username.clear();
                password.clear();
            } catch (IllegalArgumentException | IllegalStateException exception) {
                UiUtils.showAlert(Alert.AlertType.WARNING, "Không thể tạo người dùng", null, exception.getMessage());
            } finally {
                Arrays.fill(secret, '\0');
            }
        });
        VBox card = new VBox(12, new Label("Tài khoản đăng nhập"), username, password, role, create);
        card.getStyleClass().add("card");
        card.setMaxWidth(460);
        getChildren().addAll(title, help, card);
    }
}

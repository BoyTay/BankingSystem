package com.banking.ui;

import com.banking.service.AuthService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Điểm khởi chạy ứng dụng đồ họa JavaFX (VietBank).
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        AuthService auth = new AuthService();
        AuthService.User user = LoginDialog.show(auth);
        if (user == null) {
            primaryStage.close();
            return;
        }
        MainLayout layout = new MainLayout(auth, user);
        Scene scene = new Scene(layout, 1200, 760);

        // Load global CSS design system
        String cssPath = getClass().getResource("/com/banking/ui/app.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        primaryStage.setTitle("VietBank — Hệ Thống Ngân Hàng Mô Phỏng 9 Design Patterns");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(680);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

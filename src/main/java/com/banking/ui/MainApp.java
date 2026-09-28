package com.banking.ui;

import com.banking.service.AuthService;
import com.banking.persistence.SingleInstanceGuard;
import com.banking.persistence.SqliteStore;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Điểm khởi chạy ứng dụng đồ họa JavaFX (VietBank).
 */
public class MainApp extends Application {
    private SingleInstanceGuard instanceGuard;

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            instanceGuard = SingleInstanceGuard.acquire(SqliteStore.defaultPath());
        } catch (IllegalStateException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            primaryStage.close();
            return;
        }
        AuthService auth = new AuthService();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/banking/ui/novabank_login.fxml"));
        Parent login = loader.load();
        NovaBankLoginController controller = loader.getController();
        controller.setAuthService(auth);
        Scene scene = new Scene(login, 1100, 720);
        controller.setOnLoginSuccess(user -> {
            scene.setRoot(new NovaBankShell(auth, user));
            primaryStage.setMinWidth(1050);
            primaryStage.setMinHeight(720);
            primaryStage.setWidth(1440);
            primaryStage.setHeight(900);
        });

        primaryStage.setTitle("NovaBank — Banking System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (instanceGuard != null) instanceGuard.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

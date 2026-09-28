package com.banking;

import com.banking.service.AuthService;
import com.banking.ui.MainApp;
import com.banking.ui.NovaBankShell;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Exercises FXMLLoader injection, action bindings and the actual navigation shell. */
class NovaBankFxmlTest {
    @Test
    void allNovaBankScreensLoadAndNavigate() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> Platform.runLater(() -> {
            try {
                Platform.setImplicitExit(false);
                MainApp app = new MainApp();
                Stage stage = new Stage();
                app.start(stage);
                assertTrue(stage.isShowing());
                stage.close();
                app.stop();
                NovaBankShell shell = new NovaBankShell(new AuthService(),
                        new AuthService.User("admin", AuthService.Role.ADMIN));
                shell.show("accounts");
                shell.show("transfer");
                shell.show("history");
                shell.show("dashboard");
                NovaBankShell viewer = new NovaBankShell(new AuthService(),
                        new AuthService.User("viewer", AuthService.Role.VIEWER));
                Parent page = (Parent) ((StackPane) viewer.getCenter()).getChildren().get(0);
                viewer.show("transfer");
                assertSame(page, ((StackPane) viewer.getCenter()).getChildren().get(0));
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                done.countDown();
            }
        }));
        assertTrue(done.await(20, TimeUnit.SECONDS), "JavaFX loading timed out");
        Platform.exit();
        if (failure.get() != null) throw new AssertionError("FXML screen failed to load", failure.get());
    }
}

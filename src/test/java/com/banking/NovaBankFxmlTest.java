package com.banking;

import com.banking.model.Account;
import com.banking.model.enums.AccountType;
import com.banking.service.AuthService;
import com.banking.ui.MainApp;
import com.banking.ui.NovaBankShell;
import com.banking.ui.UIContext;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
                shell.show("templates");
                shell.show("open");
                shell.show("cash");
                shell.show("proxy");
                shell.show("dashboard");
                Node status = shell.lookup("#databaseStatus");
                assertNotNull(status);
                assertTrue(((Label) status).getText().contains("Singleton @"));
                Parent dashboard = (Parent) ((StackPane) shell.getCenter()).getChildren().get(0);
                ScrollPane dashboardScroll = (ScrollPane) ((BorderPane) dashboard).getCenter();
                Node eventPanel = dashboardScroll.getContent().lookup("#listObserverEvents");
                assertTrue(eventPanel instanceof ListView<?>);
                NovaBankShell viewer = new NovaBankShell(new AuthService(),
                        new AuthService.User("viewer", AuthService.Role.VIEWER));
                Parent page = (Parent) ((StackPane) viewer.getCenter()).getChildren().get(0);
                viewer.show("transfer");
                assertSame(page, ((StackPane) viewer.getCenter()).getChildren().get(0));

                UIContext ctx = UIContext.getInstance();
                Account account = ctx.getAccountService().openAccount("Observer UI Demo", AccountType.STANDARD);
                ctx.attachUiObserver(account);
                ctx.attachUiObserver(account);
                ctx.notifyDataChanged();
                ctx.getFacade().deposit(account.getAccountNumber(), 123_457);
                Platform.runLater(() -> {
                    try {
                        long matching = ctx.getNotificationLogs().stream()
                                .filter(log -> log.contains("TK " + account.getAccountNumber()
                                        + ": Nạp tiền +123457"))
                                .count();
                        assertEquals(1, matching);
                        assertSame(ctx.getNotificationLogs(), ((ListView<?>) eventPanel).getItems());
                    } catch (Throwable error) {
                        failure.set(error);
                    } finally {
                        done.countDown();
                    }
                });
            } catch (Throwable error) {
                failure.set(error);
                done.countDown();
            }
        }));
        assertTrue(done.await(20, TimeUnit.SECONDS), "JavaFX loading timed out");
        Platform.exit();
        if (failure.get() != null) throw new AssertionError("FXML screen failed to load", failure.get());
    }
}

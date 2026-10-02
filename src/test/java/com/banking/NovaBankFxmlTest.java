package com.banking;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import com.banking.pattern.behavioral.PremiumFeeStrategy;
import com.banking.pattern.behavioral.StandardFeeStrategy;
import com.banking.pattern.creational.DatabaseManager;
import com.banking.persistence.SqliteStore;
import com.banking.service.AuthService;
import com.banking.ui.MainApp;
import com.banking.ui.NovaBankShell;
import com.banking.ui.DepositWithdrawView;
import com.banking.ui.ProxyDemoView;
import com.banking.ui.UIContext;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                Stage shellStage = new Stage();
                shellStage.setScene(new Scene(shell, 1440, 900));
                shellStage.show();
                shell.show("accounts");
                shell.show("transfer");
                shell.show("dashboard");
                shell.show("templates");
                shell.show("open");
                Parent openPage = (Parent) ((ScrollPane) ((StackPane) shell.getCenter()).getChildren().get(0)).getContent();
                Node technicalPanel = openPage.lookup("#technicalDetailsPanel");
                Button technicalToggle = (Button) openPage.lookup("#technicalDetailsToggle");
                assertNotNull(technicalPanel);
                assertNotNull(technicalToggle);
                assertFalse(technicalPanel.isManaged());
                assertEquals("Chưa lưu", ((Label) openPage.lookup("#accountStoredStatus")).getText());
                technicalToggle.fire();
                assertTrue(technicalPanel.isManaged());
                ((TextField) openPage.lookup("#openAccountOwner")).setText("UI Workflow Demo");
                ((TextField) openPage.lookup("#initialDeposit")).setText("200000");
                assertEquals("Chưa lưu", ((Label) openPage.lookup("#accountStoredStatus")).getText());
                ((Button) openPage.lookup("#openAccountSubmit")).fire();
                assertEquals("Đã lưu vào SQLite", ((Label) openPage.lookup("#accountStoredStatus")).getText());
                assertTrue(openPage.lookup("#accountOpenResult").isManaged());
                UIContext ctx = UIContext.getInstance();
                Account source = ctx.getAccounts().stream()
                        .filter(a -> "UI Workflow Demo".equals(a.getOwnerName()))
                        .max(Comparator.comparing(Account::getAccountNumber)).orElseThrow();
                assertEquals(200_000, source.getBalance());
                System.out.println("PATTERN_UI_PASS: Builder | account=" + source.getAccountNumber()
                        + " | initialBalance=" + source.getBalance());
                Account target = ctx.getAccountService().openAccount("UI Flow Recipient", AccountType.STANDARD);
                ctx.attachUiObserver(target);
                ctx.notifyDataChanged();
                FlowEvidence flow = exerciseNinePatternUiFlow(shell, source, target);
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
                        assertTrue(ctx.getTransactions().stream().anyMatch(tx -> flow.reference().equals(tx.getDescription())));
                        assertEquals(1, ctx.getNotificationLogs().stream()
                                .filter(log -> log.contains("TK " + source.getAccountNumber() + ": Nạp tiền +1000"))
                                .count());
                        System.out.println("PATTERN_UI_PASS: Observer | one UI event after navigation");
                        shell.show("history");
                        Platform.runLater(() -> {
                            try {
                                Parent history = shownPage(shell);
                                Button undo = (Button) history.lookup("#btnUndo");
                                assertFalse(undo.isDisabled());
                                assertTrue(undo.getText().contains("Hoàn tác"));
                                undo.fire();
                                assertEquals(flow.sourceBefore(), source.getBalance());
                                assertEquals(flow.targetBefore(), target.getBalance());
                                assertEquals(flow.undoBefore(), ctx.getTxHistory().size());
                                Transaction reversal = ctx.getTransactions().get(ctx.getTransactions().size() - 1);
                                assertEquals(flow.transactionId(), reversal.getRelatedTransactionId());
                                System.out.println("PATTERN_UI_PASS: Command | reversal=" + reversal.getId());
                                System.out.println("PATTERN_UI_FLOW: 9/9 automated JavaFX interactions passed");
                            } catch (Throwable error) {
                                failure.set(error);
                            } finally {
                                shellStage.close();
                                done.countDown();
                            }
                        });
                    } catch (Throwable error) {
                        failure.set(error);
                        shellStage.close();
                        done.countDown();
                    }
                });
            } catch (Throwable error) {
                failure.set(error);
                done.countDown();
            }
        }));
        assertTrue(done.await(60, TimeUnit.SECONDS), "JavaFX loading timed out");
        Platform.exit();
        if (failure.get() != null) throw new AssertionError("FXML screen failed to load", failure.get());
    }

    private record FlowEvidence(String reference, String transactionId,
                                double sourceBefore, double targetBefore, int undoBefore) { }

    @SuppressWarnings("unchecked")
    private static FlowEvidence exerciseNinePatternUiFlow(NovaBankShell shell, Account source, Account target)
            throws Exception {
        UIContext ctx = UIContext.getInstance();
        String singletonBadge = ((Label) shell.lookup("#databaseStatus")).getText();
        assertTrue(singletonBadge.contains("Singleton @"));
        assertTrue(DatabaseManager.getInstance() == DatabaseManager.getInstance());
        System.out.println("PATTERN_UI_PASS: Singleton | " + singletonBadge);

        // Strategy: change the plan through the Accounts form and verify SQLite plus transfer preview.
        shell.show("accounts");
        Parent accounts = shownPage(shell);
        ((ComboBox<Account>) accounts.lookup("#cbAllAccounts")).setValue(source);
        ComboBox<AccountType> plan = (ComboBox<AccountType>) accounts.lookup("#cbNewPlan");
        plan.setValue(AccountType.PREMIUM);
        plan.fireEvent(new ActionEvent());
        assertEquals(source.getAccountNumber(), ((Label) accounts.lookup("#lblDetailNumber")).getText());
        assertEquals(AccountType.PREMIUM, plan.getValue());
        assertTrue(((Label) accounts.lookup("#lblPlanFeePreview")).getText().endsWith("→ 0 VND."));
        assertFalse(((Button) accounts.lookup("#btnApplyPlan")).isDisabled());
        fireAndConfirm("Đổi gói tài khoản", null,
                () -> ((Button) accounts.lookup("#btnApplyPlan")).fire());
        assertEquals(AccountType.PREMIUM, source.getType());
        assertTrue(source.getFeeStrategy() instanceof PremiumFeeStrategy);
        Account restored = new SqliteStore(SqliteStore.defaultPath()).loadAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(source.getAccountNumber())).findFirst().orElseThrow();
        assertEquals(AccountType.PREMIUM, restored.getType());

        shell.show("transfer");
        Parent transfer = shownPage(shell);
        ((ComboBox<Account>) transfer.lookup("#cbFromAccount")).setValue(source);
        ((ComboBox<Account>) transfer.lookup("#cbToAccount")).setValue(target);
        ((TextField) transfer.lookup("#txtSendAmount")).setText("10000");
        assertEquals("0 VND", ((Label) transfer.lookup("#lblSummaryFee")).getText());

        shell.show("accounts");
        plan.setValue(AccountType.STANDARD);
        plan.fireEvent(new ActionEvent());
        fireAndConfirm("Đổi gói tài khoản", null,
                () -> ((Button) accounts.lookup("#btnApplyPlan")).fire());
        assertEquals(AccountType.STANDARD, source.getType());
        assertTrue(source.getFeeStrategy() instanceof StandardFeeStrategy);
        System.out.println("PATTERN_UI_PASS: Strategy | account=" + source.getAccountNumber()
                + " | PREMIUM=0 VND | STANDARD=10 VND on 10000 VND");

        // State: the Accounts action changes the account; Cash visibly disables withdrawal.
        Button toggleLock = (Button) accounts.lookup("#btnToggleLock");
        toggleLock.fire();
        assertEquals(AccountStatus.LOCKED, source.getStatus());
        shell.show("cash");
        DepositWithdrawView cash = (DepositWithdrawView) shownPage(shell);
        field(cash, "cbWithdrawAccount", ComboBox.class).setValue(source);
        field(cash, "txtWithdrawAmount", TextField.class).setText("1000");
        assertTrue(field(cash, "btnWithdraw", Button.class).isDisabled());
        assertTrue(field(cash, "lblWithdrawValidation", Label.class).getText().contains("KHÓA"));
        shell.show("accounts");
        toggleLock.fire();
        assertEquals(AccountStatus.ACTIVE, source.getStatus());
        System.out.println("PATTERN_UI_PASS: State | ACTIVE -> LOCKED -> ACTIVE");

        // Proxy: READONLY may inspect but cannot change the balance.
        shell.show("proxy");
        ProxyDemoView proxy = (ProxyDemoView) shownPage(shell);
        field(proxy, "cbAccount", ComboBox.class).setValue(source);
        field(proxy, "rbReadOnly", RadioButton.class).setSelected(true);
        field(proxy, "txtAmount", TextField.class).setText("1000");
        double beforeDenied = source.getBalance();
        findButton(proxy, "Xem số dư").fire();
        assertTrue(field(proxy, "lblOutcomeTitle", Label.class).getText().contains("Được phép"));
        findButton(proxy, "Nạp tiền").fire();
        assertTrue(field(proxy, "lblOutcomeTitle", Label.class).getText().contains("Bị từ chối"));
        findButton(proxy, "Rút tiền").fire();
        assertEquals(beforeDenied, source.getBalance());
        assertTrue(field(proxy, "logContainer", VBox.class).getChildren().size() >= 3);
        System.out.println("PATTERN_UI_PASS: Proxy | READONLY write denied | balance unchanged");

        // Prototype: save and clone through the actual FXML controls.
        shell.show("templates");
        Parent templates = shownPage(shell);
        ((ComboBox<Account>) templates.lookup("#cbFromAccount")).setValue(source);
        ((ComboBox<Account>) templates.lookup("#cbToAccount")).setValue(target);
        ((TextField) templates.lookup("#txtAmount")).setText("10000");
        ((TextField) templates.lookup("#txtTemplateDescription")).setText("UI Prototype Flow");
        ((Button) templates.lookup("#btnSaveTemplate")).fire();
        VBox templateList = (VBox) templates.lookup("#boxTemplatesList");
        int beforeClone = templateList.getChildren().size();
        findButton(templateList.getChildren().get(0), "Sao chép").fire();
        assertEquals(beforeClone + 1, templateList.getChildren().size());
        assertTrue(labels(templateList.getChildren().get(0)).stream()
                .anyMatch(label -> label.getText().contains("(Bản sao)")));
        System.out.println("PATTERN_UI_PASS: Prototype | templates=" + beforeClone + " -> "
                + templateList.getChildren().size());

        // Observer: repeated navigation must not multiply one deposit event.
        shell.show("dashboard");
        shell.show("accounts");
        shell.show("cash");
        cash = (DepositWithdrawView) shownPage(shell);
        field(cash, "cbDepositAccount", ComboBox.class).setValue(source);
        field(cash, "txtDepositAmount", TextField.class).setText("1000");
        assertFalse(field(cash, "btnDeposit", Button.class).isDisabled());
        double beforeDeposit = source.getBalance();
        field(cash, "btnDeposit", Button.class).fire();
        assertEquals(beforeDeposit + 1_000, source.getBalance());

        // Facade + Command: use the transfer UI, inspect its confirmation, then Undo in Transactions.
        shell.show("transfer");
        transfer = shownPage(shell);
        ((ComboBox<Account>) transfer.lookup("#cbFromAccount")).setValue(source);
        ((ComboBox<Account>) transfer.lookup("#cbToAccount")).setValue(target);
        ((TextField) transfer.lookup("#txtSendAmount")).clear();
        ((TextField) transfer.lookup("#txtSendAmount")).setText("10000");
        assertEquals("10 VND", ((Label) transfer.lookup("#lblSummaryFee")).getText());
        double sourceBefore = source.getBalance();
        double targetBefore = target.getBalance();
        int undoBefore = ctx.getTxHistory().size();
        String reference = "UI Facade Flow " + source.getAccountNumber();
        ((TextField) transfer.lookup("#txtReference")).setText(reference);
        Parent transferPage = transfer;
        fireAndConfirm("Xác nhận chuyển khoản", "10 VND",
                () -> ((Button) transferPage.lookup("#btnReviewTransfer")).fire());
        assertEquals(sourceBefore - 10_010, source.getBalance());
        assertEquals(targetBefore + 10_000, target.getBalance());
        assertEquals(undoBefore + 1, ctx.getTxHistory().size());
        Transaction transaction = ctx.getTransactions().stream()
                .filter(tx -> reference.equals(tx.getDescription())).findFirst().orElseThrow();
        assertEquals(10, transaction.getFee());
        System.out.println("PATTERN_UI_PASS: Facade | tx=" + transaction.getId()
                + " | amount=10000 | fee=" + transaction.getFee());
        assertEquals(singletonBadge, ((Label) shell.lookup("#databaseStatus")).getText());
        return new FlowEvidence(reference, transaction.getId(), sourceBefore, targetBefore, undoBefore);
    }

    private static Parent shownPage(NovaBankShell shell) {
        shell.applyCss();
        shell.layout();
        Parent page = (Parent) ((StackPane) shell.getCenter()).getChildren().get(0);
        if (page instanceof ScrollPane scroll) return (Parent) scroll.getContent();
        if (page instanceof BorderPane border && border.getCenter() instanceof ScrollPane scroll) {
            return (Parent) scroll.getContent();
        }
        return page;
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static Button findButton(Node root, String text) {
        if (root instanceof Button button && text.equals(button.getText())) return button;
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button found = findButtonOrNull(child, text);
                if (found != null) return found;
            }
        }
        throw new AssertionError("Button not found: " + text);
    }

    private static Button findButtonOrNull(Node root, String text) {
        if (root instanceof Button button && text.equals(button.getText())) return button;
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button found = findButtonOrNull(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static List<Label> labels(Node root) {
        java.util.ArrayList<Label> result = new java.util.ArrayList<>();
        collectLabels(root, result);
        return result;
    }

    private static void collectLabels(Node root, List<Label> result) {
        if (root instanceof Label label) result.add(label);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectLabels(child, result);
        }
    }

    private static void fireAndConfirm(String title, String expectedText, Runnable trigger) {
        AtomicReference<Throwable> dialogFailure = new AtomicReference<>();
        Platform.runLater(() -> {
            Stage modal = Window.getWindows().stream().filter(Window::isShowing)
                    .filter(window -> window instanceof Stage stage && title.equals(stage.getTitle()))
                    .map(window -> (Stage) window).findFirst().orElse(null);
            if (modal == null) {
                dialogFailure.set(new AssertionError("Confirmation dialog not shown: " + title));
                return;
            }
            try {
                if (expectedText != null) assertTrue(labels(modal.getScene().getRoot()).stream()
                        .anyMatch(label -> expectedText.equals(label.getText())), "Dialog fee mismatch");
                if (modal.getScene().getRoot() instanceof DialogPane pane) {
                    ((Button) pane.lookupButton(ButtonType.OK)).fire();
                } else {
                    findButton(modal.getScene().getRoot(), "Xác nhận chuyển khoản →").fire();
                }
            } catch (Throwable error) {
                dialogFailure.set(error);
                modal.close();
            }
        });
        trigger.run();
        if (dialogFailure.get() != null) throw new AssertionError("Modal interaction failed", dialogFailure.get());
    }
}

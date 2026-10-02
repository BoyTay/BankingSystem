package com.banking;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import com.banking.pattern.behavioral.LockedState;
import com.banking.pattern.behavioral.TransferCommand;
import com.banking.pattern.behavioral.TransactionHistory;
import com.banking.pattern.behavioral.StandardFeeStrategy;
import com.banking.pattern.behavioral.PremiumFeeStrategy;
import com.banking.pattern.behavioral.TieredFeeStrategy;
import com.banking.pattern.creational.DatabaseManager;
import com.banking.pattern.creational.TransferTemplate;
import com.banking.persistence.SqliteStore;
import com.banking.pattern.structural.AccountProxy;
import com.banking.pattern.structural.BankingFacade;
import com.banking.pattern.structural.RealAccount;
import com.banking.service.AccountService;
import com.banking.service.NotificationService;
import com.banking.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BankingFlowTest {
    @BeforeAll
    static void useIsolatedDatabase() throws IOException {
        System.setProperty("banking.data.file", Files.createTempDirectory("banking-flow-")
                .resolve("banking.db").toString());
    }
    private AccountService accounts;
    private TransactionService transactions;
    private TransactionHistory commands;
    private BankingFacade facade;
    private Account source;
    private Account target;

    @BeforeEach
    void setUp() {
        accounts = new AccountService();
        transactions = new TransactionService();
        commands = new TransactionHistory();
        facade = new BankingFacade(accounts, transactions, new NotificationService(), commands);
        source = accounts.openAccount("Nguồn", AccountType.STANDARD);
        target = accounts.openAccount("Đích", AccountType.PREMIUM);
        facade.deposit(source.getAccountNumber(), 10_000);
    }

    @Test
    void transferAndUndoKeepBalancesAndHistoryConsistent() {
        List<String> events = new ArrayList<>();
        source.addObserver((account, message) -> events.add(message));

        Transaction original = facade.transfer(source.getAccountNumber(), target.getAccountNumber(),
                3_000, "Tiền học phí");
        assertEquals(6_997, source.getBalance());
        assertEquals(3_000, target.getBalance());
        assertEquals(3, original.getFee());
        assertEquals(1, commands.size());

        Transaction reversal = facade.undoLastTransfer();
        assertEquals(original.getId(), reversal.getRelatedTransactionId());
        assertEquals(10_000, source.getBalance());
        assertEquals(0, target.getBalance());
        assertEquals(0, commands.size());
        assertEquals(2, events.size()); // chuyển và hoàn tác
        assertTrue(transactions.getHistory(source.getAccountNumber()).contains(reversal));
    }

    @Test
    void rejectedTransferDoesNotEnterUndoHistoryOrTransactionLog() {
        int before = transactions.getAllTransactions().size();
        assertThrows(IllegalStateException.class, () -> facade.transfer(
                source.getAccountNumber(), target.getAccountNumber(), 100_000, "Quá số dư"));
        assertEquals(0, commands.size());
        assertEquals(before, transactions.getAllTransactions().size());
        assertEquals(10_000, source.getBalance());
    }

    @Test
    void brokenObserverDoesNotTurnCompletedTransferIntoFailure() {
        source.addObserver((account, message) -> { throw new IllegalStateException("SMS lỗi"); });
        Transaction transaction = facade.transfer(source.getAccountNumber(), target.getAccountNumber(),
                1_000, "Thông báo lỗi");
        assertNotNull(transaction.getId());
        assertEquals(8_999, source.getBalance());
        assertEquals(1_000, target.getBalance());
        assertEquals(1, commands.size());
    }

    @Test
    void lockedSourceCannotTransferOrWithdraw() {
        accounts.lockAccount(source.getAccountNumber());
        assertEquals(AccountStatus.LOCKED, source.getStatus());
        assertInstanceOf(LockedState.class, source.getState());
        assertThrows(IllegalStateException.class, () -> facade.transfer(
                source.getAccountNumber(), target.getAccountNumber(), 1_000, "Bị khóa"));
        assertThrows(IllegalStateException.class, () -> facade.withdraw(source.getAccountNumber(), 1_000));
        assertEquals(10_000, source.getBalance());
        assertEquals(0, commands.size());
    }

    @Test
    void undoIsRejectedWhenRecipientHasSpentTheMoney() {
        facade.transfer(source.getAccountNumber(), target.getAccountNumber(), 3_000, "Chuyển");
        facade.withdraw(target.getAccountNumber(), 2_000);
        int before = transactions.getAllTransactions().size();
        assertThrows(IllegalStateException.class, facade::undoLastTransfer);
        assertEquals(1, commands.size());
        assertEquals(before, transactions.getAllTransactions().size());
        assertEquals(1_000, target.getBalance());
    }

    @Test
    void moneyValidationRejectsNonFiniteAndZeroAfterRounding() {
        assertThrows(IllegalArgumentException.class, () -> facade.deposit(source.getAccountNumber(), Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> facade.deposit(source.getAccountNumber(), Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> facade.transfer(
                source.getAccountNumber(), target.getAccountNumber(), 0.4, "Quá nhỏ"));
        assertEquals(10_000, source.getBalance());
        assertEquals(0, target.getBalance());
        assertEquals(0, Money.nonNegative(0.4));
    }

    @Test
    void proxyAndPrototypeKeepTheirPatternBehavior() {
        AccountProxy readonly = new AccountProxy(new RealAccount(source), AccountProxy.Role.READONLY);
        assertEquals(10_000, readonly.getBalance());
        assertThrows(SecurityException.class, () -> readonly.withdraw(1_000));
        assertEquals(10_000, source.getBalance());

        TransferTemplate original = new TransferTemplate(source.getAccountNumber(),
                target.getAccountNumber(), 1_000, "Định kỳ");
        TransferTemplate copy = original.clone();
        copy.setAmount(2_000);
        assertNotSame(original, copy);
        assertEquals(1_000, original.getAmount());
    }

    @Test
    void accountNumbersRemainUniqueAcrossServiceInstances() {
        Account createdElsewhere = new AccountService().openAccount("Khác", AccountType.SAVINGS);
        assertNotEquals(source.getAccountNumber(), createdElsewhere.getAccountNumber());
        assertSame(source, accounts.findAccount(source.getAccountNumber()));
    }

    @Test
    void failedCommandIsNeverPushedToUndoStack() {
        TransferCommand command = new TransferCommand(source, target, 100_000, 100);
        assertFalse(commands.executeCommand(command));
        assertTrue(commands.isEmpty());
        assertFalse(commands.undoLast());
    }

    @Test
    void failedLedgerWriteRestoresBalancesAndUndoStack() {
        TransactionService failing = new TransactionService() {
            @Override
            public Transaction logTransaction(String from, String to, double amount,
                                              double fee, String description, String related) {
                throw new IllegalStateException("Disk full");
            }
        };
        BankingFacade broken = new BankingFacade(accounts, failing, new NotificationService(), commands);
        assertThrows(IllegalStateException.class, () -> broken.transfer(
                source.getAccountNumber(), target.getAccountNumber(), 1_000, "Fail"));
        assertEquals(10_000, source.getBalance());
        assertEquals(0, target.getBalance());
        assertTrue(commands.isEmpty());
    }

    @Test
    void undoHistoryCanBeRebuiltFromSavedLedger() {
        Transaction original = facade.transfer(source.getAccountNumber(), target.getAccountNumber(),
                2_000, "Before restart");
        TransactionHistory restored = new TransactionHistory();
        restored.restoreFromLedger(List.of(original), accounts);
        assertEquals(1, restored.size());
        BankingFacade restarted = new BankingFacade(accounts, transactions, new NotificationService(), restored);
        Transaction reversal = restarted.undoLastTransfer();
        assertEquals(original.getId(), reversal.getRelatedTransactionId());
        TransactionHistory afterUndo = new TransactionHistory();
        afterUndo.restoreFromLedger(List.of(original, reversal), accounts);
        assertTrue(afterUndo.isEmpty());
    }

    @Test
    void strategyFeesAndSingletonAreObservable() {
        assertEquals(1_000, new StandardFeeStrategy().calculateFee(1_000_000));
        assertEquals(0, new PremiumFeeStrategy().calculateFee(1_000_000));
        assertEquals(1_000, new TieredFeeStrategy().calculateFee(1_000_000));
        assertEquals(500, new TieredFeeStrategy().calculateFee(1_000_001), 0.001);
        assertSame(DatabaseManager.getInstance(), DatabaseManager.getInstance());
    }

    @Test
    void changingExistingAccountPlanSwapsStrategyAndPersistsIt() {
        String number = source.getAccountNumber();
        assertEquals(5_000, accounts.estimateFee(AccountType.STANDARD, 5_000_000));

        Account changed = accounts.changeAccountType(number, AccountType.PREMIUM);
        assertSame(source, changed);
        assertEquals(AccountType.PREMIUM, source.getType());
        assertInstanceOf(PremiumFeeStrategy.class, source.getFeeStrategy());
        assertEquals(0, facade.transfer(number, target.getAccountNumber(), 1_000, "Sau đổi gói").getFee());

        Path dbFile = Path.of(System.getProperty("banking.data.file"));
        Account reloaded = new SqliteStore(dbFile).loadAccounts().stream()
                .filter(account -> account.getAccountNumber().equals(number)).findFirst().orElseThrow();
        assertEquals(AccountType.PREMIUM, reloaded.getType());
        assertInstanceOf(PremiumFeeStrategy.class, reloaded.getFeeStrategy());

        accounts.changeAccountType(number, AccountType.SAVINGS);
        assertInstanceOf(TieredFeeStrategy.class, source.getFeeStrategy());
        assertEquals(2_500, accounts.estimateFee(AccountType.SAVINGS, 5_000_000));
        assertEquals(1, facade.transfer(number, target.getAccountNumber(), 1_000, "Gói tiết kiệm").getFee());
    }

    @Test
    void uiUtilsFormatVndCompactWorks() {
        assertEquals("1.5M VND", com.banking.ui.UiUtils.formatVndCompact(1_500_000));
        assertEquals("2.0B VND", com.banking.ui.UiUtils.formatVndCompact(2_000_000_000));
        assertEquals("500K VND", com.banking.ui.UiUtils.formatVndCompact(500_000));
        assertTrue(com.banking.ui.UiUtils.formatVnd(10_000).contains("10,000"));
    }

    @Test
    void humanizeErrorTranslatesExceptionsIntoFriendlyMessages() {
        String secErr = com.banking.ui.UiUtils.humanizeError(new SecurityException("READONLY role denied"));
        assertTrue(secErr.contains("Quyền truy cập bị từ chối") && secErr.contains("READONLY"));

        String balErr = com.banking.ui.UiUtils.humanizeError(new IllegalStateException("Số dư không đủ để chuyển tiền và trả phí."));
        assertTrue(balErr.contains("Số dư tài khoản không đủ"));

        String lockErr = com.banking.ui.UiUtils.humanizeError(new IllegalStateException("Tài khoản đang bị KHÓA"));
        assertTrue(lockErr.contains("Tài khoản đang tạm khóa"));

        String amtErr = com.banking.ui.UiUtils.humanizeError(new IllegalArgumentException("positive amount required"));
        assertTrue(amtErr.contains("Số tiền không hợp lệ"));
    }

    @Test
    void toastNotificationTypesExist() {
        assertEquals(4, com.banking.ui.ToastNotification.Type.values().length);
    }

    @Test
    void novaBankDashboardResourcesExist() {
        assertNotNull(getClass().getResource("/com/banking/ui/novabank_dashboard.fxml"), "Dashboard FXML layout file should exist in classpath");
        assertNotNull(getClass().getResource("/com/banking/ui/novabank_login.fxml"), "Login FXML layout file should exist in classpath");
        assertNotNull(getClass().getResource("/com/banking/ui/novabank_transfer.fxml"), "Transfer FXML layout file should exist in classpath");
        assertNotNull(getClass().getResource("/com/banking/ui/novabank_history.fxml"), "History FXML layout file should exist in classpath");
        assertNotNull(getClass().getResource("/com/banking/ui/novabank_accounts.fxml"), "Accounts FXML layout file should exist in classpath");
        assertNotNull(getClass().getResource("/com/banking/ui/novabank.css"), "CSS stylesheet should exist in classpath");
    }
}

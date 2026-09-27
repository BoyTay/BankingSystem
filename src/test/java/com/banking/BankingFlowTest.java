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
import com.banking.pattern.structural.AccountProxy;
import com.banking.pattern.structural.BankingFacade;
import com.banking.pattern.structural.RealAccount;
import com.banking.service.AccountService;
import com.banking.service.NotificationService;
import com.banking.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankingFlowTest {
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
    void strategyFeesAndSingletonAreObservable() {
        assertEquals(1_000, new StandardFeeStrategy().calculateFee(1_000_000));
        assertEquals(0, new PremiumFeeStrategy().calculateFee(1_000_000));
        assertEquals(1_000, new TieredFeeStrategy().calculateFee(1_000_000));
        assertEquals(500, new TieredFeeStrategy().calculateFee(1_000_001), 0.001);
        assertSame(DatabaseManager.getInstance(), DatabaseManager.getInstance());
    }
}

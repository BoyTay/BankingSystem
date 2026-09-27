// Pattern: Facade — Một API nghiệp vụ cho nạp, rút, chuyển và hoàn tác
package com.banking.pattern.structural;

import com.banking.model.Account;
import com.banking.model.Money;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.pattern.behavioral.TransferCommand;
import com.banking.pattern.behavioral.TransactionHistory;
import com.banking.service.AccountService;
import com.banking.service.NotificationService;
import com.banking.service.TransactionService;

/** Điều phối State, Strategy, Command, lịch sử giao dịch và Observer. */
public class BankingFacade {
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final TransactionHistory commandHistory;

    public BankingFacade(AccountService accountService,
                         TransactionService transactionService,
                         NotificationService notificationService,
                         TransactionHistory commandHistory) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
        this.commandHistory = commandHistory;
    }

    public Transaction transfer(String fromAccNo, String toAccNo, double amount, String description) {
        Account from = requireAccount(fromAccNo);
        Account to = requireAccount(toAccNo);
        if (from == to) {
            throw new IllegalArgumentException("Tài khoản nguồn và đích phải khác nhau.");
        }
        requireOutgoingAccount(from);
        if (to.getStatus() == AccountStatus.CLOSED || to.getStatus() == AccountStatus.SUSPENDED) {
            throw new IllegalStateException("Tài khoản nhận không thể nhận tiền.");
        }

        double transferAmount = Money.positive(amount);
        double fee = Money.nonNegative(from.getFeeStrategy().calculateFee(transferAmount));
        double total = Money.add(transferAmount, fee);
        if (from.getBalance() < total) {
            throw new IllegalStateException("Số dư không đủ để chuyển tiền và trả phí.");
        }

        TransferCommand command = new TransferCommand(from, to, transferAmount, fee);
        double oldSourceBalance = from.getBalance();
        double oldTargetBalance = to.getBalance();
        if (!commandHistory.executeCommand(command)) {
            throw new IllegalStateException("Không thể thực hiện chuyển khoản.");
        }
        String detail = description == null || description.isBlank() ? "Chuyển khoản" : description.trim();
        Transaction transaction;
        try {
            transaction = transactionService.logTransaction(fromAccNo, toAccNo,
                    transferAmount, fee, detail);
        } catch (RuntimeException failure) {
            from.setBalance(oldSourceBalance);
            to.setBalance(oldTargetBalance);
            commandHistory.discardLast();
            throw failure;
        }
        command.setTransactionId(transaction.getId());
        notificationService.notifyAccountEvent(from,
                String.format("Chuyển -%.0f đến %s (phí %.0f)", transferAmount, toAccNo, fee));
        notificationService.notifyAccountEvent(to,
                String.format("Nhận +%.0f từ %s", transferAmount, fromAccNo));
        return transaction;
    }

    public Transaction undoLastTransfer() {
        if (!(commandHistory.peekLast() instanceof TransferCommand command)) {
            throw new IllegalStateException("Không có chuyển khoản nào để hoàn tác.");
        }
        double oldSourceBalance = command.getFromAccount().getBalance();
        double oldTargetBalance = command.getToAccount().getBalance();
        if (!commandHistory.undoLast()) {
            throw new IllegalStateException("Tài khoản nhận không đủ số dư để hoàn tác.");
        }
        String originalId = command.getTransactionId();
        Transaction reversal;
        try {
            reversal = transactionService.logTransaction(
                    command.getToAccount().getAccountNumber(),
                    command.getFromAccount().getAccountNumber(),
                    command.getAmount(), 0,
                    String.format("Hoàn tác %s; hoàn phí %.0f VND", originalId, command.getFee()),
                    originalId);
        } catch (RuntimeException failure) {
            command.restoreAfterFailedUndo(oldSourceBalance, oldTargetBalance);
            commandHistory.restoreRecorded(command);
            throw failure;
        }
        notificationService.notifyAccountEvent(command.getFromAccount(),
                "Hoàn tác chuyển khoản " + originalId);
        notificationService.notifyAccountEvent(command.getToAccount(),
                "Hoàn tác chuyển khoản " + originalId);
        return reversal;
    }

    public Transaction deposit(String accountNumber, double amount) {
        Account account = requireAccount(accountNumber);
        if (account.getStatus() == AccountStatus.CLOSED || account.getStatus() == AccountStatus.SUSPENDED) {
            throw new IllegalStateException("Tài khoản không thể nhận tiền.");
        }
        double depositAmount = Money.positive(amount);
        double oldBalance = account.getBalance();
        account.deposit(depositAmount);
        Transaction transaction;
        try {
            transaction = transactionService.logTransaction("SYSTEM", accountNumber,
                    depositAmount, 0, "Nạp tiền");
        } catch (RuntimeException failure) {
            account.setBalance(oldBalance);
            throw failure;
        }
        notificationService.notifyAccountEvent(account,
                String.format("Nạp tiền +%.0f. Số dư: %.0f", depositAmount, account.getBalance()));
        return transaction;
    }

    public Transaction withdraw(String accountNumber, double amount) {
        Account account = requireAccount(accountNumber);
        requireOutgoingAccount(account);
        double withdrawalAmount = Money.positive(amount);
        double fee = Money.nonNegative(account.getFeeStrategy().calculateFee(withdrawalAmount));
        double total = Money.add(withdrawalAmount, fee);
        if (account.getBalance() < total) {
            throw new IllegalStateException("Số dư không đủ để rút tiền và trả phí.");
        }
        double oldBalance = account.getBalance();
        account.withdraw(total);
        Transaction transaction;
        try {
            transaction = transactionService.logTransaction(accountNumber, "CASH",
                    withdrawalAmount, fee, "Rút tiền");
        } catch (RuntimeException failure) {
            account.setBalance(oldBalance);
            throw failure;
        }
        notificationService.notifyAccountEvent(account,
                String.format("Rút tiền -%.0f (phí %.0f). Số dư: %.0f",
                        withdrawalAmount, fee, account.getBalance()));
        return transaction;
    }

    private Account requireAccount(String accountNumber) {
        Account account = accountService.findAccount(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản " + accountNumber);
        }
        return account;
    }

    private void requireOutgoingAccount(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Tài khoản nguồn không ở trạng thái hoạt động.");
        }
    }

    public AccountService getAccountService() { return accountService; }
    public TransactionService getTransactionService() { return transactionService; }
    public NotificationService getNotificationService() { return notificationService; }
    public TransactionHistory getCommandHistory() { return commandHistory; }
}

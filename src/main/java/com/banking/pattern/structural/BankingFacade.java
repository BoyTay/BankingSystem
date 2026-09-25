// Pattern: Facade — Đơn giản hóa các thao tác ngân hàng phức tạp thành một giao diện duy nhất
package com.banking.pattern.structural;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.service.AccountService;
import com.banking.service.NotificationService;
import com.banking.service.TransactionService;

/**
 * Facade bọc AccountService + TransactionService + NotificationService.
 * Một lần gọi transfer() = debit + credit + log + notify.
 */
public class BankingFacade {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    public BankingFacade(AccountService accountService,
                         TransactionService transactionService,
                         NotificationService notificationService) {
        this.accountService       = accountService;
        this.transactionService   = transactionService;
        this.notificationService  = notificationService;
    }

    /**
     * Chuyển khoản — thực hiện đầy đủ: debit + credit + log + notify.
     */
    public boolean transfer(String fromAccNo, String toAccNo, double amount) {
        System.out.println("\n  ══ [Facade] Bắt đầu chuyển khoản ══");

        // 1. Tìm tài khoản
        Account from = accountService.findAccount(fromAccNo);
        Account to   = accountService.findAccount(toAccNo);
        if (from == null || to == null) {
            System.out.println("  [Facade] Không tìm thấy tài khoản.");
            return false;
        }

        // 2. Tính phí
        double fee = from.getFeeStrategy().calculateFee(amount);
        System.out.printf("  [Facade] Phí giao dịch (%s): %.2f%n",
                from.getFeeStrategy().getName(), fee);

        // 3. Debit
        double totalDebit = amount + fee;
        if (from.getBalance() < totalDebit) {
            System.out.printf("  [Facade] Số dư không đủ (cần %.2f, có %.2f).%n",
                    totalDebit, from.getBalance());
            return false;
        }
        from.setBalance(from.getBalance() - totalDebit);
        System.out.printf("  [Facade] Debit %s: -%.2f (gồm phí). Số dư: %.2f%n",
                fromAccNo, totalDebit, from.getBalance());

        // 4. Credit
        to.setBalance(to.getBalance() + amount);
        System.out.printf("  [Facade] Credit %s: +%.2f. Số dư: %.2f%n",
                toAccNo, amount, to.getBalance());

        // 5. Log transaction
        Transaction tx = transactionService.logTransaction(fromAccNo, toAccNo, amount, fee,
                "Chuyển khoản qua Facade");
        System.out.println("  [Facade] Ghi log giao dịch: " + tx.getId());

        // 6. Notify observers
        notificationService.notifyAccountEvent(from,
                String.format("Chuyển khoản -%.2f đến %s", amount, toAccNo));
        notificationService.notifyAccountEvent(to,
                String.format("Nhận tiền +%.2f từ %s", amount, fromAccNo));

        System.out.println("  ══ [Facade] Chuyển khoản hoàn tất ══\n");
        return true;
    }

    /**
     * Nạp tiền — debit-free, chỉ cộng tiền + log + notify.
     */
    public void deposit(String accountNumber, double amount) {
        Account acc = accountService.findAccount(accountNumber);
        if (acc == null) {
            System.out.println("  [Facade] Không tìm thấy tài khoản " + accountNumber);
            return;
        }
        acc.deposit(amount);
        transactionService.logTransaction("SYSTEM", accountNumber, amount, 0, "Nạp tiền");
        notificationService.notifyAccountEvent(acc,
                String.format("Nạp tiền +%.2f. Số dư: %.2f", amount, acc.getBalance()));
    }

    /**
     * Rút tiền — tính phí + debit + log + notify.
     */
    public void withdraw(String accountNumber, double amount) {
        Account acc = accountService.findAccount(accountNumber);
        if (acc == null) {
            System.out.println("  [Facade] Không tìm thấy tài khoản " + accountNumber);
            return;
        }
        double fee = acc.getFeeStrategy().calculateFee(amount);
        System.out.printf("  [Facade] Phí rút tiền (%s): %.2f%n", acc.getFeeStrategy().getName(), fee);

        double total = amount + fee;
        if (acc.getBalance() < total) {
            System.out.println("  [Facade] Số dư không đủ.");
            return;
        }
        acc.withdraw(amount);
        if (fee > 0) {
            acc.setBalance(acc.getBalance() - fee);
        }
        transactionService.logTransaction(accountNumber, "CASH", amount, fee, "Rút tiền");
        notificationService.notifyAccountEvent(acc,
                String.format("Rút tiền -%.2f (phí %.2f). Số dư: %.2f", amount, fee, acc.getBalance()));
    }

    // ── Convenience accessors ───────────────────────────────

    public AccountService getAccountService() {
        return accountService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}

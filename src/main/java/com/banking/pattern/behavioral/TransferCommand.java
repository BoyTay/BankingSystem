// Pattern: Command (TransferCommand) — Lệnh chuyển khoản có thể undo/redo
package com.banking.pattern.behavioral;

import com.banking.model.Account;

/**
 * Command cụ thể — chuyển khoản giữa hai tài khoản.
 * Hỗ trợ undo bằng cách hoàn tiền ngược lại.
 */
public class TransferCommand implements Command {

    private final Account fromAccount;
    private final Account toAccount;
    private final double amount;
    private final double fee;
    private boolean executed = false;

    public TransferCommand(Account fromAccount, Account toAccount,
                           double amount, double fee) {
        this.fromAccount = fromAccount;
        this.toAccount   = toAccount;
        this.amount      = amount;
        this.fee         = fee;
    }

    @Override
    public void execute() {
        double totalDebit = amount + fee;
        if (fromAccount.getBalance() < totalDebit) {
            System.out.printf("  [TransferCommand] Số dư TK %s không đủ (cần %.2f, có %.2f).%n",
                    fromAccount.getAccountNumber(), totalDebit, fromAccount.getBalance());
            return;
        }
        fromAccount.setBalance(fromAccount.getBalance() - totalDebit);
        toAccount.setBalance(toAccount.getBalance() + amount);
        executed = true;

        System.out.printf("  [TransferCommand] Chuyển %.2f (phí %.2f) từ %s → %s thành công.%n",
                amount, fee, fromAccount.getAccountNumber(), toAccount.getAccountNumber());
    }

    @Override
    public void undo() {
        if (!executed) {
            System.out.println("  [TransferCommand] Không có giao dịch nào để hoàn tác.");
            return;
        }
        // Hoàn tiền
        toAccount.setBalance(toAccount.getBalance() - amount);
        fromAccount.setBalance(fromAccount.getBalance() + amount + fee);
        executed = false;

        System.out.printf("  [TransferCommand] ↩ Hoàn tác chuyển khoản %.2f từ %s → %s.%n",
                amount, fromAccount.getAccountNumber(), toAccount.getAccountNumber());
    }

    @Override
    public String describe() {
        return String.format("Transfer %.2f from %s to %s (fee: %.2f)",
                amount, fromAccount.getAccountNumber(), toAccount.getAccountNumber(), fee);
    }
}

// Pattern: Command (TransferCommand) — Lệnh chuyển khoản có thể hoàn tác
package com.banking.pattern.behavioral;

import com.banking.model.Account;
import com.banking.model.Money;

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
    private String transactionId;

    public TransferCommand(Account fromAccount, Account toAccount,
                           double amount, double fee) {
        this.fromAccount = fromAccount;
        this.toAccount   = toAccount;
        this.amount      = Money.positive(amount);
        this.fee         = Money.nonNegative(fee);
    }

    @Override
    public boolean execute() {
        if (executed || !Double.isFinite(amount) || amount <= 0
                || !Double.isFinite(fee) || fee < 0
                || fromAccount == toAccount
                || fromAccount.getStatus() != com.banking.model.enums.AccountStatus.ACTIVE) {
            return false;
        }
        double totalDebit = Money.add(amount, fee);
        if (!Double.isFinite(totalDebit) || fromAccount.getBalance() < totalDebit) {
            System.out.printf("  [TransferCommand] Số dư TK %s không đủ (cần %.2f, có %.2f).%n",
                    fromAccount.getAccountNumber(), totalDebit, fromAccount.getBalance());
            return false;
        }
        // Tính trước để không bắt đầu giao dịch nếu tài khoản nhận vượt giới hạn tiền.
        Money.add(toAccount.getBalance(), amount);
        fromAccount.withdraw(totalDebit);
        toAccount.deposit(amount);
        executed = true;

        System.out.printf("  [TransferCommand] Chuyển %.2f (phí %.2f) từ %s → %s thành công.%n",
                amount, fee, fromAccount.getAccountNumber(), toAccount.getAccountNumber());
        return true;
    }

    @Override
    public boolean undo() {
        if (!executed) {
            System.out.println("  [TransferCommand] Không có giao dịch nào để hoàn tác.");
            return false;
        }
        if (toAccount.getBalance() < amount) {
            System.out.println("  [TransferCommand] Tài khoản nhận không đủ tiền để hoàn tác.");
            return false;
        }
        // Hoàn tiền
        double newTargetBalance = Money.subtract(toAccount.getBalance(), amount);
        double newSourceBalance = Money.add(fromAccount.getBalance(), Money.add(amount, fee));
        toAccount.setBalance(newTargetBalance);
        fromAccount.setBalance(newSourceBalance);
        executed = false;

        System.out.printf("  [TransferCommand] ↩ Hoàn tác chuyển khoản %.2f từ %s → %s.%n",
                amount, fromAccount.getAccountNumber(), toAccount.getAccountNumber());
        return true;
    }

    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public static TransferCommand fromRecorded(Account from, Account to, double amount,
                                                double fee, String transactionId) {
        TransferCommand command = new TransferCommand(from, to, amount, fee);
        command.executed = true;
        command.transactionId = transactionId;
        return command;
    }

    public void restoreAfterFailedUndo(double sourceBalance, double targetBalance) {
        fromAccount.setBalance(sourceBalance);
        toAccount.setBalance(targetBalance);
        executed = true;
    }
    public String getTransactionId() { return transactionId; }
    public Account getFromAccount() { return fromAccount; }
    public Account getToAccount() { return toAccount; }
    public double getAmount() { return amount; }
    public double getFee() { return fee; }

    @Override
    public String describe() {
        return String.format("Transfer %.2f from %s to %s (fee: %.2f)",
                amount, fromAccount.getAccountNumber(), toAccount.getAccountNumber(), fee);
    }
}

// Pattern: State (LockedState) — Trạng thái bị khóa, chặn rút tiền
package com.banking.pattern.behavioral;

import com.banking.model.Account;

/**
 * Trạng thái LOCKED — cho phép nạp tiền nhưng từ chối rút tiền.
 */
public class LockedState implements AccountState {

    private Account account;

    @Override
    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public void deposit(double amount) {
        if (amount <= 0) {
            System.out.println("  [LockedState] Số tiền nạp phải lớn hơn 0.");
            return;
        }
        account.setBalance(account.getBalance() + amount);
        System.out.printf("  [LockedState] Nạp thành công %.2f (tài khoản bị khóa nhưng vẫn nhận tiền). Số dư: %.2f%n",
                amount, account.getBalance());
    }

    @Override
    public void withdraw(double amount) {
        System.out.println("  ❌ Tài khoản bị khóa — không thể rút tiền.");
    }
}

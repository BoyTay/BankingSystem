// Pattern: State (ActiveState) — Trạng thái hoạt động bình thường, cho phép mọi thao tác
package com.banking.pattern.behavioral;

import com.banking.model.Account;
import com.banking.model.Money;

/**
 * Trạng thái ACTIVE — cho phép deposit và withdraw bình thường.
 */
public class ActiveState implements AccountState {

    private Account account;

    @Override
    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public void deposit(double amount) {
        if (amount <= 0) {
            System.out.println("  [ActiveState] Số tiền nạp phải lớn hơn 0.");
            return;
        }
        account.setBalance(Money.add(account.getBalance(), amount));
        System.out.printf("  [ActiveState] Nạp thành công %.2f. Số dư mới: %.2f%n",
                amount, account.getBalance());
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            System.out.println("  [ActiveState] Số tiền rút phải lớn hơn 0.");
            return;
        }
        if (account.getBalance() < amount) {
            System.out.println("  [ActiveState] Số dư không đủ để thực hiện giao dịch.");
            return;
        }
        account.setBalance(Money.subtract(account.getBalance(), amount));
        System.out.printf("  [ActiveState] Rút thành công %.2f. Số dư mới: %.2f%n",
                amount, account.getBalance());
    }
}

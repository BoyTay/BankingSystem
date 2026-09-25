// Pattern: Proxy (RealAccount) — Triển khai thực sự của BankAccount
package com.banking.pattern.structural;

import com.banking.model.Account;

/**
 * RealSubject — ủy quyền thao tác thực tế cho Account model.
 */
public class RealAccount implements BankAccount {

    private final Account account;

    public RealAccount(Account account) {
        this.account = account;
    }

    @Override
    public void deposit(double amount) {
        account.deposit(amount);
        account.notifyObservers(String.format("Nạp tiền: +%.2f. Số dư: %.2f", amount, account.getBalance()));
    }

    @Override
    public void withdraw(double amount) {
        account.withdraw(amount);
    }

    @Override
    public double getBalance() {
        return account.getBalance();
    }

    @Override
    public String getAccountNumber() {
        return account.getAccountNumber();
    }

    public Account getAccount() {
        return account;
    }
}

// Pattern: Proxy — Interface tài khoản ngân hàng cho Proxy pattern
package com.banking.pattern.structural;

/**
 * Interface chung cho RealAccount và AccountProxy.
 */
public interface BankAccount {

    void deposit(double amount);

    void withdraw(double amount);

    double getBalance();

    String getAccountNumber();
}

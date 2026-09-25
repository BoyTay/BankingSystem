// Pattern: Builder — Xây dựng đối tượng Account với nhiều thuộc tính tùy chọn
package com.banking.model;

import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import com.banking.pattern.behavioral.AccountObserver;
import com.banking.pattern.behavioral.AccountState;
import com.banking.pattern.behavioral.ActiveState;
import com.banking.pattern.behavioral.FeeStrategy;
import com.banking.pattern.behavioral.StandardFeeStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Tài khoản ngân hàng — sử dụng Builder Pattern để khởi tạo.
 */
public class Account {

    private final String accountNumber;
    private final String ownerName;
    private double balance;
    private AccountType type;
    private AccountStatus status;
    private FeeStrategy feeStrategy;
    private AccountState state;
    private final List<AccountObserver> observers = new ArrayList<>();

    // ── Private constructor — chỉ Builder mới gọi được ─────

    private Account(Builder builder) {
        this.accountNumber = builder.accountNumber;
        this.ownerName     = builder.ownerName;
        this.balance       = builder.balance;
        this.type          = builder.type;
        this.status        = builder.status;
        this.feeStrategy   = builder.feeStrategy;
        this.state         = builder.state;
        // Gắn state vào account hiện tại
        this.state.setAccount(this);
    }

    // ── Getters & Setters ───────────────────────────────────

    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName()     { return ownerName; }
    public double getBalance()       { return balance; }
    public AccountType getType()     { return type; }
    public AccountStatus getStatus() { return status; }
    public FeeStrategy getFeeStrategy() { return feeStrategy; }
    public AccountState getState()   { return state; }

    public void setBalance(double balance)           { this.balance = balance; }
    public void setStatus(AccountStatus status)      { this.status = status; }
    public void setFeeStrategy(FeeStrategy strategy) { this.feeStrategy = strategy; }

    public void setState(AccountState state) {
        this.state = state;
        this.state.setAccount(this);
    }

    // ── Observer management ─────────────────────────────────

    public void addObserver(AccountObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AccountObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String message) {
        for (AccountObserver o : observers) {
            o.update(accountNumber, message);
        }
    }

    // ── Deposit / Withdraw delegated to State ───────────────

    public void deposit(double amount) {
        state.deposit(amount);
    }

    public void withdraw(double amount) {
        state.withdraw(amount);
    }

    @Override
    public String toString() {
        return String.format("Account{number='%s', owner='%s', balance=%.2f, type=%s, status=%s}",
                accountNumber, ownerName, balance, type, status);
    }

    // ═══════════════════════════════════════════════════════
    //  Builder (Creational Pattern)
    // ═══════════════════════════════════════════════════════

    public static class Builder {
        private final String accountNumber;
        private final String ownerName;
        private double balance = 0.0;
        private AccountType type = AccountType.STANDARD;
        private AccountStatus status = AccountStatus.ACTIVE;
        private FeeStrategy feeStrategy = new StandardFeeStrategy();
        private AccountState state = new ActiveState();

        public Builder(String accountNumber, String ownerName) {
            this.accountNumber = accountNumber;
            this.ownerName = ownerName;
        }

        public Builder balance(double balance) {
            this.balance = balance;
            return this;
        }

        public Builder type(AccountType type) {
            this.type = type;
            return this;
        }

        public Builder status(AccountStatus status) {
            this.status = status;
            return this;
        }

        public Builder feeStrategy(FeeStrategy feeStrategy) {
            this.feeStrategy = feeStrategy;
            return this;
        }

        public Builder state(AccountState state) {
            this.state = state;
            return this;
        }

        public Account build() {
            return new Account(this);
        }
    }
}

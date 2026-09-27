// Pattern: Builder — Xây dựng đối tượng Account với nhiều thuộc tính tùy chọn
package com.banking.model;

import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import com.banking.pattern.behavioral.AccountObserver;
import com.banking.pattern.behavioral.AccountState;
import com.banking.pattern.behavioral.ActiveState;
import com.banking.pattern.behavioral.LockedState;
import com.banking.pattern.behavioral.FeeStrategy;
import com.banking.pattern.behavioral.StandardFeeStrategy;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Tài khoản ngân hàng — sử dụng Builder Pattern để khởi tạo.
 */
public class Account {

    private final String accountNumber;
    private final String ownerName;
    private BigDecimal balance;
    private AccountType type;
    private AccountStatus status;
    private FeeStrategy feeStrategy;
    private AccountState state;
    private final List<AccountObserver> observers = new ArrayList<>();

    // ── Private constructor — chỉ Builder mới gọi được ─────

    private Account(Builder builder) {
        this.accountNumber = builder.accountNumber;
        this.ownerName     = builder.ownerName;
        this.balance       = Money.of(Money.nonNegative(builder.balance));
        this.type          = builder.type;
        this.status        = builder.status;
        if ((status == AccountStatus.ACTIVE && !(builder.state instanceof ActiveState))
                || (status == AccountStatus.LOCKED && !(builder.state instanceof LockedState))
                || (status != AccountStatus.ACTIVE && status != AccountStatus.LOCKED)) {
            throw new IllegalArgumentException("Trạng thái tài khoản và State không khớp hoặc chưa được hỗ trợ.");
        }
        this.feeStrategy   = builder.feeStrategy;
        this.state         = builder.state;
        // Gắn state vào account hiện tại
        this.state.setAccount(this);
    }

    // ── Getters & Setters ───────────────────────────────────

    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName()     { return ownerName; }
    public double getBalance()       { return balance.doubleValue(); }
    public AccountType getType()     { return type; }
    public AccountStatus getStatus() { return status; }
    public FeeStrategy getFeeStrategy() { return feeStrategy; }
    public AccountState getState()   { return state; }

    public void setBalance(double balance)           { this.balance = Money.of(Money.nonNegative(balance)); }
    public void setFeeStrategy(FeeStrategy strategy) { this.feeStrategy = strategy; }

    public void setState(AccountState state) {
        if (state instanceof ActiveState) {
            this.status = AccountStatus.ACTIVE;
        } else if (state instanceof LockedState) {
            this.status = AccountStatus.LOCKED;
        } else {
            throw new IllegalArgumentException("State chưa được hỗ trợ.");
        }
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
            try {
                o.update(accountNumber, message);
            } catch (RuntimeException e) {
                System.err.println("[Observer] Không gửi được thông báo cho " + accountNumber
                        + ": " + e.getMessage());
            }
        }
    }

    // ── Deposit / Withdraw delegated to State ───────────────

    public void deposit(double amount) {
        state.deposit(Money.positive(amount));
    }

    public void withdraw(double amount) {
        state.withdraw(Money.positive(amount));
    }

    @Override
    public String toString() {
        return String.format("Account{number='%s', owner='%s', balance=%.2f, type=%s, status=%s}",
                accountNumber, ownerName, getBalance(), type, status);
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
            if (accountNumber == null || accountNumber.isBlank()
                    || ownerName == null || ownerName.isBlank()) {
                throw new IllegalArgumentException("Số tài khoản và tên chủ tài khoản không được trống.");
            }
            return new Account(this);
        }
    }
}

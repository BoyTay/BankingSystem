package com.banking.service;

import com.banking.model.Account;
import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import com.banking.pattern.behavioral.*;
import com.banking.pattern.creational.DatabaseManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service quản lý tài khoản — tạo, tìm, khóa/mở khóa.
 */
public class AccountService {

    private final DatabaseManager db = DatabaseManager.getInstance();
    private static final AtomicInteger accountCounter = new AtomicInteger(1000);

    public AccountService() {
        db.getAllAccounts().stream().map(Account::getAccountNumber)
                .filter(number -> number.matches("ACC[0-9]+"))
                .mapToInt(number -> Integer.parseInt(number.substring(3)))
                .max().ifPresent(max -> accountCounter.accumulateAndGet(max, Math::max));
    }

    /**
     * Tạo tài khoản mới bằng Builder pattern.
     */
    public Account openAccount(String ownerName, AccountType type) {
        if (ownerName == null || ownerName.isBlank() || type == null) {
            throw new IllegalArgumentException("Tên chủ tài khoản và loại tài khoản không được trống.");
        }
        String accNo = "ACC" + accountCounter.incrementAndGet();

        // Chọn FeeStrategy theo loại tài khoản
        FeeStrategy feeStrategy = switch (type) {
            case PREMIUM  -> new PremiumFeeStrategy();
            case SAVINGS  -> new TieredFeeStrategy();
            case STANDARD -> new StandardFeeStrategy();
        };

        // Sử dụng Builder pattern
        Account account = new Account.Builder(accNo, ownerName)
                .type(type)
                .balance(0)
                .status(AccountStatus.ACTIVE)
                .feeStrategy(feeStrategy)
                .state(new ActiveState())
                .build();

        // Gắn Observer mặc định
        account.addObserver(new SmsNotifier());
        account.addObserver(new EmailNotifier());

        db.saveAccount(account);
        System.out.printf("  ✅ Tạo tài khoản thành công: %s (%s - %s) | Fee: %s%n",
                accNo, ownerName, type, feeStrategy.getName());
        return account;
    }

    public Account findAccount(String accountNumber) {
        return db.findAccount(accountNumber);
    }

    public List<Account> getAllAccounts() {
        return db.getAllAccounts();
    }

    /**
     * Khóa tài khoản — chuyển sang LockedState.
     */
    public void lockAccount(String accountNumber) {
        Account acc = db.findAccount(accountNumber);
        if (acc == null) {
            System.out.println("  Không tìm thấy tài khoản " + accountNumber);
            return;
        }
        AccountStatus previous = acc.getStatus();
        acc.setState(new LockedState());
        try {
            db.saveAccount(acc);
        } catch (RuntimeException failure) {
            acc.setState(previous == AccountStatus.LOCKED ? new LockedState() : new ActiveState());
            throw failure;
        }
        System.out.printf("  🔒 Tài khoản %s đã bị KHÓA.%n", accountNumber);
        acc.notifyObservers("Tài khoản đã được khóa (LockedState).");
    }

    /**
     * Mở khóa tài khoản — chuyển về ActiveState.
     */
    public void unlockAccount(String accountNumber) {
        Account acc = db.findAccount(accountNumber);
        if (acc == null) {
            System.out.println("  Không tìm thấy tài khoản " + accountNumber);
            return;
        }
        AccountStatus previous = acc.getStatus();
        acc.setState(new ActiveState());
        try {
            db.saveAccount(acc);
        } catch (RuntimeException failure) {
            acc.setState(previous == AccountStatus.LOCKED ? new LockedState() : new ActiveState());
            throw failure;
        }
        System.out.printf("  🔓 Tài khoản %s đã được MỞ KHÓA.%n", accountNumber);
        acc.notifyObservers("Tài khoản đã được mở khóa (ActiveState).");
    }
}

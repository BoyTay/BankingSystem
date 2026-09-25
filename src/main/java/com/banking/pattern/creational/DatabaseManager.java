// Pattern: Singleton — Đảm bảo chỉ có một instance quản lý lưu trữ dữ liệu
package com.banking.pattern.creational;

import com.banking.model.Account;
import com.banking.model.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton quản lý lưu trữ dữ liệu (in-memory fallback).
 * Chỉ tồn tại duy nhất một instance trong toàn bộ ứng dụng.
 */
public class DatabaseManager {

    // ── Singleton instance (eager) ──────────────────────────
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    // ── In-memory storage ───────────────────────────────────
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();

    // ── Private constructor — ngăn tạo instance bên ngoài ──
    private DatabaseManager() {
        System.out.println("[DatabaseManager] Singleton instance created (in-memory storage).");
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    // ── Account CRUD ────────────────────────────────────────

    public void saveAccount(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public Account findAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public boolean accountExists(String accountNumber) {
        return accounts.containsKey(accountNumber);
    }

    // ── Transaction CRUD ────────────────────────────────────

    public void saveTransaction(Transaction tx) {
        transactions.add(tx);
    }

    public List<Transaction> getTransactionsForAccount(String accountNumber) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction tx : transactions) {
            if (accountNumber.equals(tx.getFromAccountNumber())
                    || accountNumber.equals(tx.getToAccountNumber())) {
                result.add(tx);
            }
        }
        return result;
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }
}

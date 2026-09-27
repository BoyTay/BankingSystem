// Pattern: Singleton — Đảm bảo chỉ có một instance quản lý lưu trữ dữ liệu
package com.banking.pattern.creational;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.persistence.SqliteStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton quản lý dữ liệu trong RAM cho bản mô phỏng.
 * Chỉ tồn tại duy nhất một instance trong toàn bộ ứng dụng.
 */
public class DatabaseManager {

    // ── Singleton instance (eager) ──────────────────────────
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    // ── In-memory storage ───────────────────────────────────
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final SqliteStore store;

    // ── Private constructor — ngăn tạo instance bên ngoài ──
    private DatabaseManager() {
        store = new SqliteStore(SqliteStore.defaultPath());
        for (Account account : store.loadAccounts()) accounts.put(account.getAccountNumber(), account);
        transactions.addAll(store.loadTransactions());
        System.out.println("[DatabaseManager] SQLite đã sẵn sàng; khôi phục " + accounts.size() + " tài khoản.");
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    // ── Account CRUD ────────────────────────────────────────

    public void saveAccount(Account account) {
        store.saveAccount(account, !accounts.containsKey(account.getAccountNumber()));
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
        store.saveTransaction(new ArrayList<>(accounts.values()), tx);
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

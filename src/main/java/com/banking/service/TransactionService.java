package com.banking.service;

import com.banking.model.Transaction;
import com.banking.pattern.creational.DatabaseManager;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý giao dịch — ghi log và truy vấn lịch sử.
 */
public class TransactionService {

    private final DatabaseManager db = DatabaseManager.getInstance();

    /**
     * Ghi log một giao dịch.
     */
    public Transaction logTransaction(String fromAccNo, String toAccNo,
                                       double amount, double fee, String description) {
        return logTransaction(fromAccNo, toAccNo, amount, fee, description, null);
    }

    public Transaction logTransaction(String fromAccNo, String toAccNo,
                                       double amount, double fee, String description,
                                       String relatedTransactionId) {
        String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction tx = new Transaction(txId, fromAccNo, toAccNo, amount, fee, description,
                relatedTransactionId);
        db.saveTransaction(tx);
        return tx;
    }

    /**
     * Lấy lịch sử giao dịch của một tài khoản.
     */
    public List<Transaction> getHistory(String accountNumber) {
        return db.getTransactionsForAccount(accountNumber);
    }

    /**
     * Lấy toàn bộ giao dịch.
     */
    public List<Transaction> getAllTransactions() {
        return db.getAllTransactions();
    }
}

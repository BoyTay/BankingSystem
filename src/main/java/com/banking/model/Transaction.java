package com.banking.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Đại diện một giao dịch trong hệ thống.
 */
public class Transaction {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final String id;
    private final String fromAccountNumber;
    private final String toAccountNumber;
    private final double amount;
    private final double fee;
    private final String description;
    private final LocalDateTime timestamp;

    public Transaction(String id, String fromAccountNumber, String toAccountNumber,
                       double amount, double fee, String description) {
        this.id = id;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.fee = fee;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    // ── Getters ──────────────────────────────────────────────

    public String getId()                { return id; }
    public String getFromAccountNumber() { return fromAccountNumber; }
    public String getToAccountNumber()   { return toAccountNumber; }
    public double getAmount()            { return amount; }
    public double getFee()               { return fee; }
    public String getDescription()       { return description; }
    public LocalDateTime getTimestamp()   { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s | %.2f (fee: %.2f) | %s",
                timestamp.format(FMT), fromAccountNumber, toAccountNumber,
                amount, fee, description);
    }
}

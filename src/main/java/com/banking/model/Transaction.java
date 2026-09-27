package com.banking.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

/**
 * Đại diện một giao dịch trong hệ thống.
 */
public class Transaction {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final String id;
    private final String fromAccountNumber;
    private final String toAccountNumber;
    private final BigDecimal amount;
    private final BigDecimal fee;
    private final String description;
    private final String relatedTransactionId;
    private final LocalDateTime timestamp;

    public Transaction(String id, String fromAccountNumber, String toAccountNumber,
                       double amount, double fee, String description) {
        this(id, fromAccountNumber, toAccountNumber, amount, fee, description, null);
    }

    public Transaction(String id, String fromAccountNumber, String toAccountNumber,
                       double amount, double fee, String description, String relatedTransactionId) {
        this.id = id;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = Money.of(Money.nonNegative(amount));
        this.fee = Money.of(Money.nonNegative(fee));
        this.description = description;
        this.relatedTransactionId = relatedTransactionId;
        this.timestamp = LocalDateTime.now();
    }

    // ── Getters ──────────────────────────────────────────────

    public String getId()                { return id; }
    public String getFromAccountNumber() { return fromAccountNumber; }
    public String getToAccountNumber()   { return toAccountNumber; }
    public double getAmount()            { return amount.doubleValue(); }
    public double getFee()               { return fee.doubleValue(); }
    public String getDescription()       { return description; }
    public String getRelatedTransactionId() { return relatedTransactionId; }
    public LocalDateTime getTimestamp()   { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s | %.2f (fee: %.2f) | %s",
                timestamp.format(FMT), fromAccountNumber, toAccountNumber,
                getAmount(), getFee(), description);
    }
}

// Pattern: Prototype — Nhân bản mẫu giao dịch chuyển khoản để tái sử dụng
package com.banking.pattern.creational;

/**
 * Prototype cho phép clone một mẫu giao dịch chuyển khoản
 * thay vì tạo mới từ đầu mỗi lần.
 */
public class TransferTemplate implements Cloneable {

    private String fromAccountNumber;
    private String toAccountNumber;
    private double amount;
    private String description;

    public TransferTemplate(String fromAccountNumber, String toAccountNumber,
                            double amount, String description) {
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.description = description;
    }

    // ── Prototype: clone ────────────────────────────────────

    @Override
    public TransferTemplate clone() {
        try {
            return (TransferTemplate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone failed", e);
        }
    }

    // ── Getters & Setters ───────────────────────────────────

    public String getFromAccountNumber() { return fromAccountNumber; }
    public String getToAccountNumber()   { return toAccountNumber; }
    public double getAmount()            { return amount; }
    public String getDescription()       { return description; }

    public void setFromAccountNumber(String from) { this.fromAccountNumber = from; }
    public void setToAccountNumber(String to)     { this.toAccountNumber = to; }
    public void setAmount(double amount)           { this.amount = amount; }
    public void setDescription(String desc)        { this.description = desc; }

    @Override
    public String toString() {
        return String.format("TransferTemplate{from='%s', to='%s', amount=%.2f, desc='%s'}",
                fromAccountNumber, toAccountNumber, amount, description);
    }
}

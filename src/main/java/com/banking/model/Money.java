package com.banking.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Quy tắc tiền tệ thống nhất cho bản mô phỏng (VND, làm tròn đến đồng). */
public final class Money {
    // Giữ số nguyên VND trong miền biểu diễn chính xác của double tại ranh giới UI cũ.
    private static final BigDecimal MAX = new BigDecimal("9000000000000000");
    private Money() { }

    public static BigDecimal of(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Số tiền phải là số hữu hạn.");
        }
        return checked(BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP));
    }

    public static double positive(double value) {
        BigDecimal amount = of(value);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0 VND.");
        }
        return amount.doubleValue();
    }

    public static double nonNegative(double value) {
        BigDecimal amount = of(value);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Số tiền không được âm.");
        }
        return amount.doubleValue();
    }

    public static double add(double left, double right) {
        return checked(of(left).add(of(right))).doubleValue();
    }

    public static double subtract(double left, double right) {
        return checked(of(left).subtract(of(right))).doubleValue();
    }

    private static BigDecimal checked(BigDecimal amount) {
        if (amount.abs().compareTo(MAX) > 0) {
            throw new IllegalArgumentException("Số tiền vượt giới hạn bản mô phỏng.");
        }
        return amount;
    }
}

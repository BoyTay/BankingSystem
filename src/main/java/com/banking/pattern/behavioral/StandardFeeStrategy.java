// Pattern: Strategy (StandardFeeStrategy) — Phí 0.1% cho tài khoản thường
package com.banking.pattern.behavioral;

/**
 * Phí 0.1% áp dụng cho tài khoản STANDARD.
 */
public class StandardFeeStrategy implements FeeStrategy {

    private static final double RATE = 0.001; // 0.1%

    @Override
    public double calculateFee(double amount) {
        return amount * RATE;
    }

    @Override
    public String getName() {
        return "StandardFee (0.1%)";
    }
}

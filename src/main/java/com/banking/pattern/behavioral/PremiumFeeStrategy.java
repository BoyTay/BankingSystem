// Pattern: Strategy (PremiumFeeStrategy) — Miễn phí cho tài khoản PREMIUM
package com.banking.pattern.behavioral;

/**
 * Phí 0% — miễn phí giao dịch cho tài khoản PREMIUM.
 */
public class PremiumFeeStrategy implements FeeStrategy {

    @Override
    public double calculateFee(double amount) {
        return 0.0;
    }

    @Override
    public String getName() {
        return "PremiumFee (0%)";
    }
}

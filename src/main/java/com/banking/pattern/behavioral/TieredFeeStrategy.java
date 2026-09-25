// Pattern: Strategy (TieredFeeStrategy) — Phí theo bậc dựa trên số tiền giao dịch
package com.banking.pattern.behavioral;

/**
 * Phí giao dịch theo bậc:
 *   - Dưới 1,000,000  → 0.1%
 *   - 1,000,000 – 10,000,000 → 0.05%
 *   - Trên 10,000,000 → 0.02%
 */
public class TieredFeeStrategy implements FeeStrategy {

    @Override
    public double calculateFee(double amount) {
        if (amount <= 1_000_000) {
            return amount * 0.001;
        } else if (amount <= 10_000_000) {
            return amount * 0.0005;
        } else {
            return amount * 0.0002;
        }
    }

    @Override
    public String getName() {
        return "TieredFee (0.1%/0.05%/0.02%)";
    }
}

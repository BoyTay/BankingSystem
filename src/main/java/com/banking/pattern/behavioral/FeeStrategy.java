// Pattern: Strategy — Interface chiến lược tính phí giao dịch
package com.banking.pattern.behavioral;

/**
 * Strategy interface — định nghĩa cách tính phí giao dịch.
 * Mỗi loại tài khoản có thể sử dụng strategy khác nhau.
 */
public interface FeeStrategy {

    /**
     * Tính phí dựa trên số tiền giao dịch.
     * @param amount số tiền giao dịch
     * @return phí
     */
    double calculateFee(double amount);

    /**
     * Tên chiến lược (để hiển thị).
     */
    String getName();
}

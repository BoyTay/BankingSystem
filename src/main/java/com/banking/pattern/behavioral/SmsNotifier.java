// Pattern: Observer (SmsNotifier) — In thông báo dạng SMS khi có sự kiện tài khoản
package com.banking.pattern.behavioral;

/**
 * Observer cụ thể — gửi thông báo qua SMS (giả lập bằng console print).
 */
public class SmsNotifier implements AccountObserver {

    @Override
    public void update(String accountNumber, String message) {
        System.out.printf("  📱 [SMS → %s] %s%n", accountNumber, message);
    }
}

// Pattern: Observer (EmailNotifier) — In thông báo dạng Email khi có sự kiện tài khoản
package com.banking.pattern.behavioral;

/**
 * Observer cụ thể — gửi thông báo qua Email (giả lập bằng console print).
 */
public class EmailNotifier implements AccountObserver {

    @Override
    public void update(String accountNumber, String message) {
        System.out.printf("  📧 [Email → %s] %s%n", accountNumber, message);
    }
}

// Pattern: Observer — Interface quan sát sự kiện tài khoản
package com.banking.pattern.behavioral;

/**
 * Observer interface — nhận thông báo khi có sự kiện trên tài khoản.
 */
public interface AccountObserver {

    /**
     * Được gọi khi có sự kiện trên tài khoản.
     * @param accountNumber số tài khoản phát sinh sự kiện
     * @param message nội dung thông báo
     */
    void update(String accountNumber, String message);
}

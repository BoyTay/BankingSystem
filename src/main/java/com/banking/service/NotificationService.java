package com.banking.service;

import com.banking.model.Account;

/**
 * Service thông báo — kích hoạt Observer pattern trên tài khoản.
 */
public class NotificationService {

    /**
     * Gửi thông báo đến tất cả observer của tài khoản.
     */
    public void notifyAccountEvent(Account account, String message) {
        account.notifyObservers(message);
    }
}

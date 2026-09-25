// Pattern: State — Interface trạng thái tài khoản, quyết định hành vi deposit/withdraw
package com.banking.pattern.behavioral;

import com.banking.model.Account;

/**
 * State interface — mỗi trạng thái tài khoản định nghĩa
 * hành vi riêng cho deposit() và withdraw().
 */
public interface AccountState {

    void deposit(double amount);

    void withdraw(double amount);

    /**
     * Gắn tham chiếu đến Account sở hữu state này.
     */
    void setAccount(Account account);
}

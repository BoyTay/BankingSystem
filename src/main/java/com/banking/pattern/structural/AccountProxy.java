// Pattern: Proxy — Kiểm soát truy cập dựa trên vai trò (role-based access control)
package com.banking.pattern.structural;

/**
 * Proxy bọc RealAccount, kiểm tra quyền trước khi cho phép thao tác.
 * Vai trò READONLY chỉ được xem số dư, không được deposit/withdraw.
 */
public class AccountProxy implements BankAccount {

    /**
     * Vai trò người dùng.
     */
    public enum Role {
        ADMIN,
        USER,
        READONLY
    }

    private final RealAccount realAccount;
    private final Role role;

    public AccountProxy(RealAccount realAccount, Role role) {
        this.realAccount = realAccount;
        this.role = role;
    }

    @Override
    public void deposit(double amount) {
        if (role == Role.READONLY) {
            System.out.printf("  ⛔ [Proxy] Truy cập bị từ chối — vai trò %s không được phép nạp tiền.%n", role);
            throw new SecurityException("AccessDeniedException: Role " + role + " cannot deposit.");
        }
        System.out.printf("  [Proxy] Vai trò %s — cho phép nạp tiền.%n", role);
        realAccount.deposit(amount);
    }

    @Override
    public void withdraw(double amount) {
        if (role == Role.READONLY) {
            System.out.printf("  ⛔ [Proxy] Truy cập bị từ chối — vai trò %s không được phép rút tiền.%n", role);
            throw new SecurityException("AccessDeniedException: Role " + role + " cannot withdraw.");
        }
        System.out.printf("  [Proxy] Vai trò %s — cho phép rút tiền.%n", role);
        realAccount.withdraw(amount);
    }

    @Override
    public double getBalance() {
        System.out.printf("  [Proxy] Vai trò %s — cho phép xem số dư.%n", role);
        return realAccount.getBalance();
    }

    @Override
    public String getAccountNumber() {
        return realAccount.getAccountNumber();
    }

    public Role getRole() {
        return role;
    }
}

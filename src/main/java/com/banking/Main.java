package com.banking;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountType;
import com.banking.pattern.behavioral.*;
import com.banking.pattern.creational.DatabaseManager;
import com.banking.pattern.creational.TransferTemplate;
import com.banking.pattern.structural.AccountProxy;
import com.banking.pattern.structural.BankingFacade;
import com.banking.pattern.structural.RealAccount;
import com.banking.service.AccountService;
import com.banking.service.AuthService;
import com.banking.service.NotificationService;
import com.banking.service.TransactionService;
import com.banking.persistence.SingleInstanceGuard;
import com.banking.persistence.SqliteStore;

import java.util.List;
import java.util.Scanner;
import java.util.Arrays;

/**
 * Hệ thống Ngân hàng / Ví điện tử — Console Menu.
 * Minh họa 9 Design Patterns: Builder, Singleton, Prototype,
 * Proxy, Facade, Command, Observer, State, Strategy.
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    // ── Services ────────────────────────────────────────────
    private static final AccountService accountService = new AccountService();
    private static final TransactionService transactionService = new TransactionService();
    private static final NotificationService notificationService = new NotificationService();

    // ── Command History ─────────────────────────────────────
    private static final TransactionHistory txHistory = new TransactionHistory();
    private static final BankingFacade facade = new BankingFacade(
            accountService, transactionService, notificationService, txHistory);

    // ═══════════════════════════════════════════════════════
    //  MAIN
    // ═══════════════════════════════════════════════════════

    public static void main(String[] args) {
        try (SingleInstanceGuard ignored = SingleInstanceGuard.acquire(SqliteStore.defaultPath())) {
            runMenu();
        }
    }

    private static void runMenu() {
        AuthService.User operator = login();
        if (operator == null) return;
        txHistory.restoreFromLedger(transactionService.getAllTransactions(), accountService);
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   🏦  HỆ THỐNG NGÂN HÀNG — DESIGN PATTERNS DEMO    ║");
        System.out.println("║   9 Patterns: Builder, Singleton, Prototype,        ║");
        System.out.println("║   Proxy, Facade, Command, Observer, State, Strategy ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // Singleton demo — accessed once, same instance throughout
        System.out.println("\n⚙ Khởi tạo DatabaseManager (Singleton)...");
        DatabaseManager db = DatabaseManager.getInstance();
        System.out.println("  → Instance: " + db.hashCode());

        boolean running = true;
        while (running) {
            printMenu(operator);
            String choice = scanner.nextLine().trim();

            if (!allowed(operator.role(), choice)) {
                System.out.println("  Vai trò hiện tại không được phép thực hiện thao tác này.");
                continue;
            }

            try {
                switch (choice) {
                    case "1"  -> openAccount();
                    case "2"  -> deposit();
                    case "3"  -> withdraw();
                    case "4"  -> transfer();
                    case "5"  -> transferWithPrototype();
                    case "6"  -> lockUnlockAccount();
                    case "7"  -> viewHistory();
                    case "8"  -> undoLastTransaction();
                    case "9"  -> proxyDemo();
                    case "10" -> listAccounts();
                    case "11" -> createOperator();
                    case "12" -> changePassword(operator.username());
                    case "0"  -> {
                        running = false;
                        System.out.println("\n👋 Cảm ơn đã sử dụng hệ thống. Tạm biệt!");
                    }
                    default -> System.out.println("  ❌ Lựa chọn không hợp lệ. Vui lòng thử lại.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("  ❌ " + e.getMessage());
            }
        }
        scanner.close();
    }

    private static AuthService.User login() {
        AuthService auth = new AuthService();
        if (auth.needsSetup()) {
            System.out.println("Thiết lập quản trị viên đầu tiên.");
            System.out.print("Tên đăng nhập: ");
            String name = scanner.nextLine().trim();
            char[] secret = readPassword();
            try {
                auth.createFirstAdmin(name, secret);
                return auth.authenticate(name, secret);
            } finally {
                Arrays.fill(secret, '\0');
            }
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            System.out.print("Tên đăng nhập: ");
            String name = scanner.nextLine().trim();
            char[] secret = readPassword();
            try {
                AuthService.User user = auth.authenticate(name, secret);
                if (user != null) return user;
            } finally {
                Arrays.fill(secret, '\0');
            }
            System.out.println("Sai tài khoản hoặc mật khẩu.");
        }
        return null;
    }

    private static char[] readPassword() {
        if (System.console() != null) return System.console().readPassword("Mật khẩu: ");
        System.out.print("Mật khẩu: ");
        return scanner.nextLine().toCharArray();
    }

    private static boolean allowed(AuthService.Role role, String choice) {
        if ("0".equals(choice)) return true;
        if (role == AuthService.Role.ADMIN) return true;
        if (role == AuthService.Role.VIEWER) {
            return "7".equals(choice) || "10".equals(choice) || "12".equals(choice);
        }
        return !"1".equals(choice) && !"6".equals(choice)
                && !"9".equals(choice) && !"11".equals(choice);
    }

    // ── Menu ────────────────────────────────────────────────

    private static void printMenu(AuthService.User user) {
        System.out.println("\n── MENU: " + user.username() + " (" + user.role() + ") ──");
        if (user.role() == AuthService.Role.ADMIN) System.out.println("  1. Mở tài khoản (Builder)");
        if (user.role() != AuthService.Role.VIEWER) {
            System.out.println("  2. Nạp tiền (Observer)");
            System.out.println("  3. Rút tiền (State)");
            System.out.println("  4. Chuyển khoản (Facade + Command)");
            System.out.println("  5. Chuyển khoản mẫu (Prototype)");
        }
        if (user.role() == AuthService.Role.ADMIN) System.out.println("  6. Khóa/Mở khóa tài khoản");
        System.out.println("  7. Xem lịch sử giao dịch");
        if (user.role() != AuthService.Role.VIEWER) System.out.println("  8. Hoàn tác chuyển khoản cuối");
        if (user.role() == AuthService.Role.ADMIN) System.out.println("  9. Demo Proxy");
        System.out.println(" 10. Danh sách tài khoản");
        if (user.role() == AuthService.Role.ADMIN) System.out.println(" 11. Tạo người dùng");
        System.out.println(" 12. Đổi mật khẩu");
        System.out.println("  0. Thoát");
        System.out.print("Chọn: ");
    }

    private static void createOperator() {
        System.out.print("Tên đăng nhập mới: ");
        String username = scanner.nextLine().trim();
        System.out.print("Vai trò (ADMIN/STAFF/VIEWER): ");
        AuthService.Role role = AuthService.Role.valueOf(scanner.nextLine().trim().toUpperCase());
        char[] secret = readPassword();
        try {
            new AuthService().createUser(username, secret, role);
            System.out.println("Đã tạo người dùng " + username + ".");
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    private static void changePassword(String username) {
        System.out.println("Mật khẩu hiện tại:");
        char[] oldSecret = readPassword();
        System.out.println("Mật khẩu mới:");
        char[] newSecret = readPassword();
        try {
            new AuthService().changePassword(username, oldSecret, newSecret);
            System.out.println("Đã đổi mật khẩu.");
        } finally {
            Arrays.fill(oldSecret, '\0');
            Arrays.fill(newSecret, '\0');
        }
    }

    // ═══════════════════════════════════════════════════════
    //  1. MỞ TÀI KHOẢN — Builder Pattern
    // ═══════════════════════════════════════════════════════

    private static void openAccount() {
        System.out.println("\n── Mở tài khoản (Builder Pattern) ──");
        System.out.print("  Tên chủ tài khoản: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("  ❌ Tên không được để trống.");
            return;
        }

        System.out.println("  Loại tài khoản:");
        System.out.println("    1. STANDARD  (phí 0.1%)");
        System.out.println("    2. SAVINGS   (phí theo bậc)");
        System.out.println("    3. PREMIUM   (miễn phí)");
        System.out.print("  Chọn (1-3): ");
        String typeChoice = scanner.nextLine().trim();

        AccountType type = switch (typeChoice) {
            case "2"  -> AccountType.SAVINGS;
            case "3"  -> AccountType.PREMIUM;
            default   -> AccountType.STANDARD;
        };

        Account account = accountService.openAccount(name, type);
        System.out.println("  → Builder tạo: " + account);
    }

    // ═══════════════════════════════════════════════════════
    //  2. NẠP TIỀN — Observer Pattern (SMS + Email notifiers)
    // ═══════════════════════════════════════════════════════

    private static void deposit() {
        System.out.println("\n── Nạp tiền (Observer Pattern) ──");
        String accNo = askAccountNumber();
        if (accNo == null) return;

        double amount = askAmount("Số tiền nạp");
        if (amount <= 0) return;

        facade.deposit(accNo, amount);
    }

    // ═══════════════════════════════════════════════════════
    //  3. RÚT TIỀN — State Pattern (Active vs Locked)
    // ═══════════════════════════════════════════════════════

    private static void withdraw() {
        System.out.println("\n── Rút tiền (State Pattern) ──");
        String accNo = askAccountNumber();
        if (accNo == null) return;

        double amount = askAmount("Số tiền rút");
        if (amount <= 0) return;

        facade.withdraw(accNo, amount);
    }

    // ═══════════════════════════════════════════════════════
    //  4. CHUYỂN KHOẢN — Facade + Command Pattern
    // ═══════════════════════════════════════════════════════

    private static void transfer() {
        System.out.println("\n── Chuyển khoản (Facade + Command Pattern) ──");
        System.out.print("  Số TK nguồn: ");
        String from = scanner.nextLine().trim();
        System.out.print("  Số TK đích : ");
        String to = scanner.nextLine().trim();

        Account fromAcc = accountService.findAccount(from);
        Account toAcc   = accountService.findAccount(to);
        if (fromAcc == null || toAcc == null) {
            System.out.println("  ❌ Không tìm thấy một trong hai tài khoản.");
            return;
        }

        double amount = askAmount("Số tiền chuyển");
        if (amount <= 0) return;

        Transaction tx = facade.transfer(from, to, amount, "Chuyển khoản từ console");
        System.out.printf("  Thành công: %s | phí %.0f VND%n", tx.getId(), tx.getFee());
    }

    // ═══════════════════════════════════════════════════════
    //  5. CHUYỂN KHOẢN MẪU — Prototype Pattern
    // ═══════════════════════════════════════════════════════

    private static void transferWithPrototype() {
        System.out.println("\n── Chuyển khoản mẫu (Prototype Pattern) ──");
        System.out.print("  Số TK nguồn (mẫu): ");
        String from = scanner.nextLine().trim();
        System.out.print("  Số TK đích  (mẫu): ");
        String to = scanner.nextLine().trim();

        double amount = askAmount("Số tiền mẫu");
        if (amount <= 0) return;

        // Tạo template gốc
        TransferTemplate original = new TransferTemplate(from, to, amount, "Mẫu chuyển khoản");
        System.out.println("  Original: " + original);

        // Clone template và tùy chỉnh
        TransferTemplate cloned = original.clone();
        cloned.setDescription("Bản sao — chuyển khoản hàng tháng");
        cloned.setAmount(amount * 1.1); // Tăng 10% cho bản sao
        System.out.println("  Cloned  : " + cloned);
        System.out.println("  → Original == Cloned? " + (original == cloned));
        System.out.println("  → Prototype pattern: clone() tạo bản sao độc lập.");

        // Thực hiện chuyển khoản từ bản clone
        Account fromAcc = accountService.findAccount(cloned.getFromAccountNumber());
        Account toAcc   = accountService.findAccount(cloned.getToAccountNumber());
        if (fromAcc != null && toAcc != null) {
            Transaction tx = facade.transfer(cloned.getFromAccountNumber(),
                    cloned.getToAccountNumber(), cloned.getAmount(), cloned.getDescription());
            System.out.println("  Giao dịch từ bản sao: " + tx.getId());
        } else {
            System.out.println("  ⚠ Không tìm thấy tài khoản — bỏ qua thực thi.");
        }
    }

    // ═══════════════════════════════════════════════════════
    //  6. KHÓA / MỞ KHÓA — State Pattern
    // ═══════════════════════════════════════════════════════

    private static void lockUnlockAccount() {
        System.out.println("\n── Khóa / Mở khóa tài khoản (State Pattern) ──");
        String accNo = askAccountNumber();
        if (accNo == null) return;

        Account acc = accountService.findAccount(accNo);
        System.out.printf("  Trạng thái hiện tại: %s%n", acc.getStatus());
        System.out.println("  1. Khóa tài khoản");
        System.out.println("  2. Mở khóa tài khoản");
        System.out.print("  Chọn (1-2): ");
        String choice = scanner.nextLine().trim();

        if ("1".equals(choice)) {
            accountService.lockAccount(accNo);
        } else if ("2".equals(choice)) {
            accountService.unlockAccount(accNo);
        } else {
            System.out.println("  ❌ Lựa chọn không hợp lệ.");
        }
    }

    // ═══════════════════════════════════════════════════════
    //  7. XEM LỊCH SỬ GIAO DỊCH
    // ═══════════════════════════════════════════════════════

    private static void viewHistory() {
        System.out.println("\n── Lịch sử giao dịch ──");
        String accNo = askAccountNumber();
        if (accNo == null) return;

        List<Transaction> txs = transactionService.getHistory(accNo);
        if (txs.isEmpty()) {
            System.out.println("  (Chưa có giao dịch)");
            return;
        }
        System.out.println("  ┌─────────────────────────────────────────────────┐");
        for (Transaction tx : txs) {
            System.out.println("  │ " + tx);
        }
        System.out.println("  └─────────────────────────────────────────────────┘");
    }

    // ═══════════════════════════════════════════════════════
    //  8. HOÀN TÁC — Command Pattern (undo)
    // ═══════════════════════════════════════════════════════

    private static void undoLastTransaction() {
        System.out.println("\n── Hoàn tác giao dịch cuối (Command Pattern) ──");
        System.out.printf("  Số lệnh có thể hoàn tác: %d%n", txHistory.size());
        Transaction reversal = facade.undoLastTransfer();
        System.out.printf("  Đã hoàn tác %s bằng giao dịch %s%n",
                reversal.getRelatedTransactionId(), reversal.getId());
    }

    // ═══════════════════════════════════════════════════════
    //  9. DEMO PROXY — Proxy Pattern (role-based access)
    // ═══════════════════════════════════════════════════════

    private static void proxyDemo() {
        System.out.println("\n── Demo Proxy Pattern ──");
        String accNo = askAccountNumber();
        if (accNo == null) return;

        Account acc = accountService.findAccount(accNo);
        RealAccount real = new RealAccount(acc, facade);

        // Demo ADMIN role
        System.out.println("\n  ── Test vai trò ADMIN ──");
        AccountProxy adminProxy = new AccountProxy(real, AccountProxy.Role.ADMIN);
        System.out.printf("  Balance (ADMIN): %.2f%n", adminProxy.getBalance());
        adminProxy.deposit(100);

        // Demo USER role
        System.out.println("\n  ── Test vai trò USER ──");
        AccountProxy userProxy = new AccountProxy(real, AccountProxy.Role.USER);
        System.out.printf("  Balance (USER): %.2f%n", userProxy.getBalance());

        // Demo READONLY role — sẽ throw SecurityException
        System.out.println("\n  ── Test vai trò READONLY ──");
        AccountProxy readonlyProxy = new AccountProxy(real, AccountProxy.Role.READONLY);
        System.out.printf("  Balance (READONLY): %.2f%n", readonlyProxy.getBalance());
        try {
            readonlyProxy.withdraw(50);
        } catch (SecurityException e) {
            System.out.println("  ✔ Bắt được exception: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  10. DANH SÁCH TÀI KHOẢN
    // ═══════════════════════════════════════════════════════

    private static void listAccounts() {
        System.out.println("\n── Danh sách tài khoản ──");
        List<Account> accounts = accountService.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("  (Chưa có tài khoản nào)");
            return;
        }
        System.out.println("  ┌──────────────────────────────────────────────────────────────┐");
        for (Account a : accounts) {
            System.out.printf("  │ %-10s | %-15s | %12.2f | %-8s | %-6s | %s%n",
                    a.getAccountNumber(), a.getOwnerName(), a.getBalance(),
                    a.getType(), a.getStatus(), a.getFeeStrategy().getName());
        }
        System.out.println("  └──────────────────────────────────────────────────────────────┘");
    }

    // ── Helpers ─────────────────────────────────────────────

    private static String askAccountNumber() {
        System.out.print("  Số tài khoản: ");
        String accNo = scanner.nextLine().trim();
        if (accountService.findAccount(accNo) == null) {
            System.out.println("  ❌ Không tìm thấy tài khoản " + accNo);
            return null;
        }
        return accNo;
    }

    private static double askAmount(String label) {
        System.out.print("  " + label + ": ");
        try {
            double amount = Double.parseDouble(scanner.nextLine().trim());
            if (amount <= 0) {
                System.out.println("  ❌ Số tiền phải lớn hơn 0.");
            }
            return amount;
        } catch (NumberFormatException e) {
            System.out.println("  ❌ Số tiền không hợp lệ.");
            return -1;
        }
    }
}

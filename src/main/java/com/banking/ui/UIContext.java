package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountType;
import com.banking.pattern.behavioral.AccountObserver;
import com.banking.pattern.behavioral.TransactionHistory;
import com.banking.pattern.structural.BankingFacade;
import com.banking.service.AccountService;
import com.banking.service.NotificationService;
import com.banking.service.TransactionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý trạng thái và kết nối UI với các Service / Pattern.
 * Đảm bảo tính nhất quán (Single Source of Truth) cho JavaFX.
 */
public class UIContext {

    private static UIContext instance;

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final BankingFacade facade;
    private final TransactionHistory txHistory;

    // Observable data cho UI components
    private final ObservableList<Account> accounts = FXCollections.observableArrayList();
    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    private final ObservableList<String> notificationLogs = FXCollections.observableArrayList();

    private final List<Runnable> dataChangeListeners = new ArrayList<>();

    private UIContext() {
        this.accountService = new AccountService();
        this.transactionService = new TransactionService();
        this.notificationService = new NotificationService();
        this.txHistory = new TransactionHistory();
        this.facade = new BankingFacade(accountService, transactionService, notificationService, txHistory);

        seedInitialData();
        refresh();
    }

    public static synchronized UIContext getInstance() {
        if (instance == null) {
            instance = new UIContext();
        }
        return instance;
    }

    /**
     * Khởi tạo dữ liệu mẫu nếu chưa có tài khoản nào.
     */
    private void seedInitialData() {
        if (accountService.getAllAccounts().isEmpty()) {
            Account acc1 = accountService.openAccount("NGUYỄN VĂN A", AccountType.STANDARD);
            Account acc2 = accountService.openAccount("TRẦN THỊ B", AccountType.PREMIUM);
            Account acc3 = accountService.openAccount("LÊ VĂN C", AccountType.SAVINGS);

            // Gắn UI Observer để lắng nghe thông báo thời gian thực
            attachUiObserver(acc1);
            attachUiObserver(acc2);
            attachUiObserver(acc3);

            // Nạp tiền ban đầu
            facade.deposit(acc1.getAccountNumber(), 50_000_000);
            facade.deposit(acc2.getAccountNumber(), 12_000_000);
            facade.deposit(acc3.getAccountNumber(), 20_000_000);

            // Chuyển khoản mẫu
            facade.transfer(acc1.getAccountNumber(), acc2.getAccountNumber(), 2_000_000,
                    "Chuyển khoản mẫu");
        }
    }

    public void attachUiObserver(Account account) {
        account.addObserver(new AccountObserver() {
            @Override
            public void update(String accountNumber, String message) {
                String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String log = String.format("[%s] TK %s: %s", timestamp, accountNumber, message);
                Platform.runLater(() -> {
                    notificationLogs.add(0, log);
                    if (notificationLogs.size() > 50) {
                        notificationLogs.remove(notificationLogs.size() - 1);
                    }
                });
            }
        });
    }

    public void logCustomEvent(String prefix, String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String log = String.format("[%s] [%s] %s", timestamp, prefix, message);
        Platform.runLater(() -> {
            notificationLogs.add(0, log);
            if (notificationLogs.size() > 50) {
                notificationLogs.remove(notificationLogs.size() - 1);
            }
        });
    }

    public void addDataChangeListener(Runnable listener) {
        dataChangeListeners.add(listener);
    }

    public void notifyDataChanged() {
        refresh();
        for (Runnable listener : dataChangeListeners) {
            listener.run();
        }
    }

    public void refresh() {
        accounts.setAll(accountService.getAllAccounts());
        transactions.setAll(transactionService.getAllTransactions());
    }

    // ── Getters ─────────────────────────────────────────────
    public AccountService getAccountService() { return accountService; }
    public TransactionService getTransactionService() { return transactionService; }
    public NotificationService getNotificationService() { return notificationService; }
    public BankingFacade getFacade() { return facade; }
    public TransactionHistory getTxHistory() { return txHistory; }
    public ObservableList<Account> getAccounts() { return accounts; }
    public ObservableList<Transaction> getTransactions() { return transactions; }
    public ObservableList<String> getNotificationLogs() { return notificationLogs; }
}

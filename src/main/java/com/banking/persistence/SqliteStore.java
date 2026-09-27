package com.banking.persistence;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import com.banking.pattern.behavioral.ActiveState;
import com.banking.pattern.behavioral.EmailNotifier;
import com.banking.pattern.behavioral.LockedState;
import com.banking.pattern.behavioral.PremiumFeeStrategy;
import com.banking.pattern.behavioral.SmsNotifier;
import com.banking.pattern.behavioral.StandardFeeStrategy;
import com.banking.pattern.behavioral.TieredFeeStrategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Local durable storage. One SQLite commit contains balances and the corresponding ledger entry. */
public final class SqliteStore {
    private final String url;

    public SqliteStore(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được thư mục dữ liệu: " + file, e);
        }
        url = "jdbc:sqlite:" + file.toAbsolutePath();
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS accounts (" +
                    "number TEXT PRIMARY KEY, owner TEXT NOT NULL, type TEXT NOT NULL, " +
                    "status TEXT NOT NULL, balance INTEGER NOT NULL CHECK(balance >= 0))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id TEXT PRIMARY KEY, source TEXT NOT NULL, destination TEXT NOT NULL, " +
                    "amount INTEGER NOT NULL, fee INTEGER NOT NULL, description TEXT NOT NULL, " +
                    "related_id TEXT, created_at TEXT NOT NULL)");
        } catch (SQLException e) {
            throw new IllegalStateException("Không mở được cơ sở dữ liệu: " + file, e);
        }
    }

    public static Path defaultPath() {
        String configured = System.getProperty("banking.data.file");
        return configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), ".vietbank", "banking.db")
                : Path.of(configured);
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout=5000");
        }
        return connection;
    }

    public List<Account> loadAccounts() {
        List<Account> result = new ArrayList<>();
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT number, owner, type, status, balance FROM accounts ORDER BY number")) {
            while (rows.next()) {
                AccountType type = AccountType.valueOf(rows.getString("type"));
                AccountStatus status = AccountStatus.valueOf(rows.getString("status"));
                Account account = new Account.Builder(rows.getString("number"), rows.getString("owner"))
                        .type(type).status(status).balance(rows.getLong("balance"))
                        .state(status == AccountStatus.LOCKED ? new LockedState() : new ActiveState())
                        .feeStrategy(switch (type) {
                            case STANDARD -> new StandardFeeStrategy();
                            case SAVINGS -> new TieredFeeStrategy();
                            case PREMIUM -> new PremiumFeeStrategy();
                        }).build();
                account.addObserver(new SmsNotifier());
                account.addObserver(new EmailNotifier());
                result.add(account);
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được tài khoản đã lưu.", e);
        }
    }

    public List<Transaction> loadTransactions() {
        List<Transaction> result = new ArrayList<>();
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM transactions ORDER BY created_at, rowid")) {
            while (rows.next()) {
                result.add(new Transaction(rows.getString("id"), rows.getString("source"),
                        rows.getString("destination"), rows.getLong("amount"), rows.getLong("fee"),
                        rows.getString("description"), rows.getString("related_id"),
                        LocalDateTime.parse(rows.getString("created_at"))));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được lịch sử giao dịch.", e);
        }
    }

    public void saveAccount(Account account, boolean isNew) {
        try (Connection connection = connect()) {
            writeAccount(connection, account, isNew);
        } catch (SQLException e) {
            throw new IllegalStateException("Không lưu được tài khoản.", e);
        }
    }

    public void saveTransaction(List<Account> accounts, Transaction transaction) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                for (Account account : accounts) writeAccount(connection, account, false);
                try (PreparedStatement query = connection.prepareStatement(
                        "INSERT INTO transactions (id, source, destination, amount, fee, description, related_id, created_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    query.setString(1, transaction.getId());
                    query.setString(2, transaction.getFromAccountNumber());
                    query.setString(3, transaction.getToAccountNumber());
                    query.setLong(4, (long) transaction.getAmount());
                    query.setLong(5, (long) transaction.getFee());
                    query.setString(6, transaction.getDescription());
                    query.setString(7, transaction.getRelatedTransactionId());
                    query.setString(8, transaction.getTimestamp().toString());
                    query.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không lưu được giao dịch; dữ liệu trên đĩa chưa thay đổi.", e);
        }
    }

    private void writeAccount(Connection connection, Account account, boolean isNew) throws SQLException {
        String sql = isNew
                ? "INSERT INTO accounts (number, owner, type, status, balance) VALUES (?, ?, ?, ?, ?)"
                : "UPDATE accounts SET owner=?, type=?, status=?, balance=? WHERE number=?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            if (isNew) query.setString(1, account.getAccountNumber());
            query.setString(isNew ? 2 : 1, account.getOwnerName());
            query.setString(isNew ? 3 : 2, account.getType().name());
            query.setString(isNew ? 4 : 3, account.getStatus().name());
            query.setLong(isNew ? 5 : 4, (long) account.getBalance());
            if (!isNew) query.setString(5, account.getAccountNumber());
            if (query.executeUpdate() != 1) throw new SQLException("Tài khoản không còn tồn tại: " + account.getAccountNumber());
        }
    }
}

package com.banking.service;

import com.banking.persistence.SqliteStore;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Path;
import java.util.Arrays;

/** Local operator accounts for the desktop application. */
public final class AuthService {
    public enum Role { ADMIN, STAFF, VIEWER }
    public record User(String username, Role role) { }

    private static final int ITERATIONS = 210_000;
    private final String url;

    public AuthService() {
        this(SqliteStore.defaultPath());
    }

    public AuthService(Path file) {
        new SqliteStore(file);
        url = "jdbc:sqlite:" + file.toAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, role TEXT NOT NULL, salt BLOB NOT NULL, hash BLOB NOT NULL)");
        } catch (SQLException e) {
            throw new IllegalStateException("Không mở được dữ liệu người dùng.", e);
        }
    }

    public boolean needsSetup() {
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            return rows.next() && rows.getInt(1) == 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không kiểm tra được người dùng.", e);
        }
    }

    public void createFirstAdmin(String username, char[] password) {
        if (!needsSetup()) throw new IllegalStateException("Quản trị viên đã tồn tại.");
        createUser(username, password, Role.ADMIN);
    }

    public void createUser(String username, char[] password, Role role) {
        String normalized = validateUsername(username);
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự.");
        }
        if (role == null) throw new IllegalArgumentException("Chưa chọn vai trò.");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(password, salt);
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users(username, role, salt, hash) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, normalized);
            statement.setString(2, role.name());
            statement.setBytes(3, salt);
            statement.setBytes(4, hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Không tạo được người dùng; tên có thể đã tồn tại.", e);
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    public User authenticate(String username, char[] password) {
        if (username == null || password == null) return null;
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT role, salt, hash FROM users WHERE username = ?")) {
            statement.setString(1, username.trim());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return null;
                byte[] actual = hash(password, rows.getBytes("salt"));
                boolean matches = MessageDigest.isEqual(actual, rows.getBytes("hash"));
                Arrays.fill(actual, (byte) 0);
                return matches ? new User(username.trim(), Role.valueOf(rows.getString("role"))) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không xác thực được người dùng.", e);
        }
    }

    public void changePassword(String username, char[] oldPassword, char[] newPassword) {
        if (authenticate(username, oldPassword) == null) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
        if (newPassword == null || newPassword.length < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự.");
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(newPassword, salt);
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET salt=?, hash=? WHERE username=?")) {
            statement.setBytes(1, salt);
            statement.setBytes(2, hash);
            statement.setString(3, username);
            if (statement.executeUpdate() != 1) throw new IllegalStateException("Không tìm thấy người dùng.");
        } catch (SQLException e) {
            throw new IllegalStateException("Không đổi được mật khẩu.", e);
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    private String validateUsername(String username) {
        if (username == null || !username.matches("[A-Za-z0-9_]{3,32}")) {
            throw new IllegalArgumentException("Tên đăng nhập cần 3–32 ký tự chữ, số hoặc dấu gạch dưới.");
        }
        return username;
    }

    private byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được mã băm mật khẩu.", e);
        } finally {
            spec.clearPassword();
        }
    }
}

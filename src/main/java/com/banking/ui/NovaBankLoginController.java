package com.banking.ui;

import com.banking.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Controller cho giao diện NovaBank Login (Figma Split-Screen).
 * Tích hợp bảo mật:
 * - PBKDF2WithHmacSHA256 Password Hashing thông qua AuthService.
 * - Toggle Show/Hide password với Text & Password Field synchronization.
 * - Hỗ trợ thiết lập tài khoản Admin đầu tiên (First-time setup).
 * - Callback onSuccess chuyển giao phiên đăng nhập tới MainLayout / Dashboard.
 */
public class NovaBankLoginController implements Initializable {

    @FXML private HBox errorBanner;
    @FXML private Label lblError;
    @FXML private Label lblLoginSubtitle;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordPlain;
    @FXML private Button btnToggleShowPass;
    @FXML private SVGPath svgEyeIcon;
    @FXML private Button btnSignIn;
    @FXML private CheckBox chkRememberMe;

    private static final String EYE_SHOW = "M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z";
    private static final String EYE_HIDE = "M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21";

    private static final String PREF_KEY_USERNAME = "novabank.remember.username";
    private static final String PREF_KEY_REMEMBER = "novabank.remember.enabled";
    private final Preferences prefs = Preferences.userNodeForPackage(NovaBankLoginController.class);

    private AuthService authService;
    private Consumer<AuthService.User> onLoginSuccess;
    private boolean isPasswordShown = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mặc định khởi tạo AuthService
        this.authService = new AuthService();
        updateSetupMode();

        // Đồng bộ 2 chiều giữa PasswordField và PlainTextField
        txtPassword.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isPasswordShown) {
                txtPasswordPlain.setText(newVal);
            }
        });
        txtPasswordPlain.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isPasswordShown) {
                txtPassword.setText(newVal);
            }
        });

        // Hỗ trợ nhấn Enter để đăng nhập ngay
        txtUsername.setOnAction(e -> handleSignIn());
        txtPassword.setOnAction(e -> handleSignIn());
        txtPasswordPlain.setOnAction(e -> handleSignIn());

        // ── REMEMBER ME: Load saved credentials ──
        loadRememberedUser();
    }

    private void loadRememberedUser() {
        boolean remembered = prefs.getBoolean(PREF_KEY_REMEMBER, false);
        if (remembered) {
            String savedUsername = prefs.get(PREF_KEY_USERNAME, "");
            if (!savedUsername.isEmpty()) {
                txtUsername.setText(savedUsername);
                if (chkRememberMe != null) chkRememberMe.setSelected(true);
                txtPassword.requestFocus();
            }
        }
    }

    private void saveRememberedUser(String username) {
        if (chkRememberMe != null && chkRememberMe.isSelected()) {
            prefs.put(PREF_KEY_USERNAME, username);
            prefs.putBoolean(PREF_KEY_REMEMBER, true);
        } else {
            prefs.remove(PREF_KEY_USERNAME);
            prefs.putBoolean(PREF_KEY_REMEMBER, false);
        }
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
        updateSetupMode();
    }

    private void updateSetupMode() {
        boolean setup = authService.needsSetup();
        btnSignIn.setText(setup ? "Tạo quản trị viên đầu tiên →" : "Đăng nhập →");
        if (lblLoginSubtitle != null) {
            if (setup) {
                lblLoginSubtitle.setText("Lần đầu: tạo tài khoản ADMIN, mật khẩu ít nhất 8 ký tự.");
                lblLoginSubtitle.setVisible(true);
                lblLoginSubtitle.setManaged(true);
            } else {
                lblLoginSubtitle.setText("");
                lblLoginSubtitle.setVisible(false);
                lblLoginSubtitle.setManaged(false);
            }
        }
    }

    public void setOnLoginSuccess(Consumer<AuthService.User> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    @FXML
    private void handleTogglePasswordVisibility() {
        isPasswordShown = !isPasswordShown;
        if (isPasswordShown) {
            txtPasswordPlain.setText(txtPassword.getText());
            txtPasswordPlain.setVisible(true);
            txtPasswordPlain.setManaged(true);

            txtPassword.setVisible(false);
            txtPassword.setManaged(false);

            svgEyeIcon.setContent(EYE_HIDE);
            txtPasswordPlain.requestFocus();
            txtPasswordPlain.positionCaret(txtPasswordPlain.getText().length());
        } else {
            txtPassword.setText(txtPasswordPlain.getText());
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);

            txtPasswordPlain.setVisible(false);
            txtPasswordPlain.setManaged(false);

            svgEyeIcon.setContent(EYE_SHOW);
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText().length());
        }
    }

    @FXML
    private void handleSignIn() {
        hideError();
        String username = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        String passwordStr = isPasswordShown ? txtPasswordPlain.getText() : txtPassword.getText();

        if (username.isEmpty()) {
            showError("Please enter your username.");
            txtUsername.requestFocus();
            return;
        }

        if (passwordStr == null || passwordStr.isEmpty()) {
            showError("Please enter your password.");
            txtPassword.requestFocus();
            return;
        }

        char[] secret = passwordStr.toCharArray();
        try {
            if (authService != null && authService.needsSetup()) {
                // First-time admin creation
                authService.createFirstAdmin(username, secret);
            }

            AuthService.User user = authService != null ? authService.authenticate(username, secret) : null;
            if (user == null) {
                showError("Invalid username or password. Please try again.");
                ToastNotification.showError("Đăng nhập thất bại: Sai tài khoản hoặc mật khẩu.");
                return;
            }

            // Đăng nhập thành công
            saveRememberedUser(username);
            ToastNotification.showSuccess("Welcome back, " + user.username() + "!");
            if (onLoginSuccess != null) {
                onLoginSuccess.accept(user);
            }

        } catch (IllegalArgumentException | IllegalStateException ex) {
            String friendlyMsg = UiUtils.humanizeError(ex);
            showError(friendlyMsg);
            ToastNotification.showWarning(friendlyMsg);
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    @FXML
    private void handleForgotPassword() {
        ToastNotification.showInfo("Vui lòng liên hệ Quản trị viên để đặt lại thông tin tài khoản.");
    }

    private void showError(String message) {
        lblError.setText(message);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
    }

    private void hideError() {
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }
}

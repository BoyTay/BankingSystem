package com.banking.ui;

import com.banking.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.util.Arrays;

/**
 * Màn hình Hồ sơ & Bảo mật tài khoản (Profile & Security View).
 * Thiết kế chuẩn theo Stitch Design System:
 * - Cột trái: Identity Card, Telemetry thông số kỹ thuật, Token Metadata & Chính sách an toàn tài khoản
 * - Cột phải: Form Đổi mật khẩu an toàn với Inline Feedback Banner, Password Strength Meter, Toggle ẩn/hiện, Xác thực thời gian thực
 */
final class ProfileView extends VBox {

    private final AuthService auth;
    private final AuthService.User user;

    // Form inputs
    private final PasswordField txtOldPassword = new PasswordField();
    private final TextField txtOldPasswordPlain = new TextField();

    private final PasswordField txtNewPassword = new PasswordField();
    private final TextField txtNewPasswordPlain = new TextField();

    private final PasswordField txtConfirmPassword = new PasswordField();
    private final TextField txtConfirmPasswordPlain = new TextField();

    // Password strength bars & labels
    private final Region[] strBars = new Region[4];
    private final Label lblStrength = new Label("Chưa nhập");
    private final Label lblMatchStatus = new Label();

    // Inline feedback banner
    private final HBox bannerFeedback = new HBox(10);
    private final Label lblFeedbackTitle = new Label("Kiểm tra bảo vệ mật khẩu chủ động");
    private final Label lblFeedbackDesc = new Label("Vui lòng nhập mật khẩu hiện tại và tạo mật khẩu mới đạt tiêu chuẩn độ phức tạp (tối thiểu 8 ký tự, gồm chữ và số).");
    private final SVGPath iconFeedback = new SVGPath();

    ProfileView(AuthService auth, AuthService.User user) {
        this.auth = auth;
        this.user = user;

        setSpacing(18);
        getStyleClass().add("content-pane");

        buildHeader();

        // 2 CỘT CHÍNH: Cột Trái (Identity + Policy) & Cột Phải (Form Đổi mật khẩu)
        HBox mainGrid = new HBox(20);
        mainGrid.setAlignment(Pos.TOP_LEFT);

        VBox leftCol = buildLeftColumn();
        VBox rightCol = buildRightColumn();

        leftCol.setPrefWidth(430);
        leftCol.setMinWidth(380);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        mainGrid.getChildren().addAll(leftCol, rightCol);
        getChildren().add(mainGrid);
    }

    private void buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        HBox tagBox = new HBox(6);
        tagBox.setAlignment(Pos.CENTER_LEFT);
        tagBox.getStyleClass().add("header-badge-tag");
        tagBox.setMaxWidth(Double.NEGATIVE_INFINITY);
        Circle tagDot = new Circle(3.0);
        tagDot.getStyleClass().add("header-badge-tag-dot");
        Label tagText = new Label("CÀI ĐẶT HỆ THỐNG · BẢO MẬT");
        tagText.getStyleClass().add("header-badge-tag-text");
        tagBox.getChildren().addAll(tagDot, tagText);

        Label title = new Label("Hồ sơ & bảo mật");
        title.getStyleClass().add("header-greeting");

        Label subtitle = new Label("Xem tài khoản đăng nhập và thay đổi mật khẩu hệ thống");
        subtitle.getStyleClass().add("header-subtitle");

        titleBox.getChildren().addAll(tagBox, title, subtitle);

        header.getChildren().addAll(titleBox);
        getChildren().add(header);
    }

    private VBox buildLeftColumn() {
        VBox leftCol = new VBox(20);

        // 1. Premium Identity Card
        VBox identityCard = new VBox(24);
        identityCard.setStyle("-fx-padding: 28; -fx-background-color: linear-gradient(to bottom right, #0F172A, #1E293B, #334155); -fx-background-radius: 16px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8);");

        HBox topIdentity = new HBox(16);
        topIdentity.setAlignment(Pos.CENTER_LEFT);

        StackPane avatarBox = new StackPane();
        avatarBox.setStyle("-fx-background-color: linear-gradient(to right, #FDE047, #F59E0B); -fx-background-radius: 50%; -fx-min-width: 56px; -fx-min-height: 56px; -fx-max-width: 56px; -fx-max-height: 56px; -fx-padding: 2px;");
        StackPane innerAvatar = new StackPane();
        innerAvatar.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 50%;");

        String initial = (user != null && !user.username().isEmpty()) ? user.username().substring(0, 1).toUpperCase() : "A";
        Label lblAvatar = new Label(initial);
        lblAvatar.setStyle("-fx-text-fill: #FDE047; -fx-font-size: 24px; -fx-font-weight: 900; -fx-font-family: 'Inter', sans-serif;");
        innerAvatar.getChildren().add(lblAvatar);
        avatarBox.getChildren().add(innerAvatar);

        VBox userDetails = new VBox(4);
        HBox.setHgrow(userDetails, Priority.ALWAYS);

        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        String username = (user != null) ? user.username() : "admin";
        Label lblUsername = new Label(username);
        lblUsername.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");

        Label badgeVerified = new Label("✓ VERIFIED");
        badgeVerified.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34D399; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 2 6; -fx-background-radius: 4px;");
        nameRow.getChildren().addAll(lblUsername, badgeVerified);

        String roleStr = (user != null) ? user.role().name() : "ADMIN";
        Label lblRoleSub = new Label("Super Administrator / Quyền hệ thống");
        lblRoleSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
        userDetails.getChildren().addAll(nameRow, lblRoleSub);

        Label badgeRole = new Label(roleStr);
        badgeRole.setStyle("-fx-background-color: linear-gradient(to right, #3B82F6, #2563EB); -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 20px;");

        topIdentity.getChildren().addAll(avatarBox, userDetails, badgeRole);

        HBox cardFooter = new HBox();
        cardFooter.setAlignment(Pos.CENTER_LEFT);
        cardFooter.setStyle("-fx-padding: 10 0 0 0;");
        SVGPath chipIcon = new SVGPath();
        chipIcon.setContent("M4 4h16v16H4V4zm2 2v3h3V6H6zm5 0v3h3V6h-3zm5 0v3h3V6h-3zM6 11v3h3v-3H6zm5 0v3h3v-3h-3zm5 0v3h3v-3h-3zM6 16v2h3v-2H6zm5 0v2h3v-2h-3zm5 0v2h3v-2h-3z");
        chipIcon.setStyle("-fx-fill: #FBBF24; -fx-scale-x: 1.2; -fx-scale-y: 1.2;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label lblCardNum = new Label("**** **** **** " + Math.abs(username.hashCode() % 9000 + 1000));
        lblCardNum.setStyle("-fx-text-fill: #CBD5E1; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 14px; -fx-letter-spacing: 2px;");
        cardFooter.getChildren().addAll(chipIcon, sp, lblCardNum);

        identityCard.getChildren().addAll(topIdentity, cardFooter);

        // 2. Security Status Widget
        VBox securityWidget = new VBox(14);
        securityWidget.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #E2E8F0; -fx-border-radius: 16px; -fx-padding: 24; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 4);");

        HBox secHeader = new HBox(14);
        secHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane shieldBox = new StackPane();
        shieldBox.setStyle("-fx-background-color: rgba(16, 185, 129, 0.1); -fx-background-radius: 50%; -fx-min-width: 48px; -fx-min-height: 48px;");
        SVGPath greenShield = new SVGPath();
        greenShield.setContent("M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z");
        greenShield.setStyle("-fx-stroke: #10B981; -fx-stroke-width: 2; -fx-fill: transparent; -fx-stroke-linecap: round; -fx-stroke-linejoin: round;");
        shieldBox.getChildren().add(greenShield);

        VBox secTitleBox = new VBox(2);
        Label lblSecTitle = new Label("Bảo vệ toàn diện");
        lblSecTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label lblSecSub = new Label("Tài khoản của bạn đang được bảo mật an toàn.");
        lblSecSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        secTitleBox.getChildren().addAll(lblSecTitle, lblSecSub);

        secHeader.getChildren().addAll(shieldBox, secTitleBox);

        VBox secList = new VBox(12);
        secList.setStyle("-fx-padding: 10 0 0 0;");
        secList.getChildren().addAll(
            buildSecFeature("Xác thực 2 lớp (2FA)", "Đang hoạt động", true),
            buildSecFeature("Mã hóa đầu cuối", "Chuẩn AES-256", true),
            buildSecFeature("Cảnh báo đăng nhập lạ", "Bật qua Email & SMS", true)
        );

        securityWidget.getChildren().addAll(secHeader, secList);

        leftCol.getChildren().addAll(identityCard, securityWidget);
        return leftCol;
    }

    private HBox buildSecFeature(String title, String status, boolean isGood) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lblCheck = new Label(isGood ? "✓" : "!");
        lblCheck.setStyle("-fx-text-fill: " + (isGood ? "#10B981" : "#F59E0B") + "; -fx-font-weight: 800;");
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-font-weight: 600;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label lblStatus = new Label(status);
        lblStatus.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        row.getChildren().addAll(lblCheck, lblTitle, sp, lblStatus);
        return row;
    }



    private VBox buildRightColumn() {
        VBox rightCard = new VBox(20);
        rightCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #E2E8F0; -fx-border-radius: 16px; -fx-padding: 32; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 4);");

        // Section Title
        HBox cardHeader = new HBox(10);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: rgba(0, 196, 118, 0.15); -fx-background-radius: 8px; -fx-min-width: 36px; -fx-min-height: 36px; -fx-max-width: 36px; -fx-max-height: 36px;");
        SVGPath keySvg = new SVGPath();
        keySvg.setContent("M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z");
        keySvg.setStyle("-fx-fill: #008751; -fx-scale-x: 0.8; -fx-scale-y: 0.8;");
        iconCircle.getChildren().add(keySvg);

        VBox titleArea = new VBox(2);
        Label title = new Label("Đổi mật khẩu an toàn");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitle = new Label("Cập nhật mật khẩu để duy trì mức độ an toàn cao nhất cho tài khoản hệ thống");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        titleArea.getChildren().addAll(title, subtitle);

        cardHeader.getChildren().addAll(iconCircle, titleArea);

        // Inline Feedback Banner
        buildInlineFeedbackBanner();

        // Form Fields
        VBox form = new VBox(14);

        // Field 1: Mật khẩu hiện tại
        VBox grpCurrent = new VBox(4);
        HBox lblRowCurrent = new HBox();
        Label lblCurrent = new Label("Mật khẩu hiện tại *");
        lblCurrent.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        Region spCurrent = new Region();
        HBox.setHgrow(spCurrent, Priority.ALWAYS);
        Label lblReqCurrent = new Label("Bắt buộc");
        lblReqCurrent.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        lblRowCurrent.getChildren().addAll(lblCurrent, spCurrent, lblReqCurrent);

        HBox inputWrapperCurrent = createPasswordInput(txtOldPassword, txtOldPasswordPlain, "Nhập mật khẩu hiện tại");
        grpCurrent.getChildren().addAll(lblRowCurrent, inputWrapperCurrent);

        // Field 2: Mật khẩu mới
        VBox grpNew = new VBox(4);
        HBox lblRowNew = new HBox();
        Label lblNew = new Label("Mật khẩu mới *");
        lblNew.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        Region spNew = new Region();
        HBox.setHgrow(spNew, Priority.ALWAYS);
        Label lblRec = new Label("Khuyến nghị độ mạnh cao");
        lblRec.setStyle("-fx-font-size: 11px; -fx-text-fill: #2563EB; -fx-font-weight: 600;");
        lblRowNew.getChildren().addAll(lblNew, spNew, lblRec);

        HBox inputWrapperNew = createPasswordInput(txtNewPassword, txtNewPasswordPlain, "Tối thiểu 8 ký tự, gồm chữ và số");

        // Helper text & Password Strength Meter
        Label lblHint = new Label("ℹ Tối thiểu 8 ký tự, kết hợp chữ và số (ví dụ: Admin@2025).");
        lblHint.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B; -fx-padding: 2 0 0 2;");

        VBox strengthBox = buildStrengthMeter();

        grpNew.getChildren().addAll(lblRowNew, inputWrapperNew, lblHint, strengthBox);

        // Listen for new password changes to update strength
        txtNewPassword.textProperty().addListener((obs, oldV, newV) -> updatePasswordStrength(newV));
        txtNewPasswordPlain.textProperty().addListener((obs, oldV, newV) -> updatePasswordStrength(newV));

        // Field 3: Xác nhận mật khẩu mới
        VBox grpConfirm = new VBox(4);
        HBox lblRowConfirm = new HBox();
        Label lblConfirm = new Label("Xác nhận mật khẩu mới *");
        lblConfirm.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        Region spConfirm = new Region();
        HBox.setHgrow(spConfirm, Priority.ALWAYS);
        lblMatchStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 700;");
        lblRowConfirm.getChildren().addAll(lblConfirm, spConfirm, lblMatchStatus);

        HBox inputWrapperConfirm = createPasswordInput(txtConfirmPassword, txtConfirmPasswordPlain, "Nhập lại mật khẩu mới vừa đặt");
        grpConfirm.getChildren().addAll(lblRowConfirm, inputWrapperConfirm);

        txtConfirmPassword.textProperty().addListener((obs, oldV, newV) -> checkPasswordMatch());
        txtConfirmPasswordPlain.textProperty().addListener((obs, oldV, newV) -> checkPasswordMatch());

        // Buttons
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setStyle("-fx-padding: 8 0 0 0;");

        Button btnReset = new Button("Làm lại");
        btnReset.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: 700; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnReset.setOnAction(e -> handleResetForm());

        Button btnSubmit = new Button("Cập nhật mật khẩu");
        btnSubmit.setStyle("-fx-background-color: linear-gradient(to right, #2563EB, #4F46E5); -fx-text-fill: #FFFFFF; -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.3), 8, 0, 0, 4);");
        btnSubmit.setOnAction(e -> handleSubmitPassword());

        actionRow.getChildren().addAll(btnReset, btnSubmit);

        form.getChildren().addAll(grpCurrent, grpNew, grpConfirm, actionRow);

        rightCard.getChildren().addAll(cardHeader, bannerFeedback, form);
        return rightCard;
    }

    private void buildInlineFeedbackBanner() {
        bannerFeedback.setAlignment(Pos.CENTER_LEFT);
        bannerFeedback.getStyleClass().setAll("inline-feedback-info");

        iconFeedback.setContent("M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z");
        iconFeedback.setStyle("-fx-fill: #2563EB; -fx-scale-x: 0.8; -fx-scale-y: 0.8;");

        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        lblFeedbackTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E40AF;");
        lblFeedbackDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #3B82F6;");
        lblFeedbackDesc.setWrapText(true);

        textBox.getChildren().addAll(lblFeedbackTitle, lblFeedbackDesc);
        bannerFeedback.getChildren().addAll(iconFeedback, textBox);
    }

    private void showFeedback(String type, String title, String message) {
        bannerFeedback.getStyleClass().clear();
        if ("error".equals(type)) {
            bannerFeedback.getStyleClass().add("inline-feedback-error");
            iconFeedback.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z");
            iconFeedback.setStyle("-fx-fill: #DC2626; -fx-scale-x: 0.8; -fx-scale-y: 0.8;");
            lblFeedbackTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #991B1B;");
            lblFeedbackDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #DC2626;");
        } else if ("success".equals(type)) {
            bannerFeedback.getStyleClass().add("inline-feedback-success");
            iconFeedback.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z");
            iconFeedback.setStyle("-fx-fill: #059669; -fx-scale-x: 0.8; -fx-scale-y: 0.8;");
            lblFeedbackTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #065F46;");
            lblFeedbackDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #059669;");
        } else {
            bannerFeedback.getStyleClass().add("inline-feedback-info");
            iconFeedback.setContent("M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z");
            iconFeedback.setStyle("-fx-fill: #2563EB; -fx-scale-x: 0.8; -fx-scale-y: 0.8;");
            lblFeedbackTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1E40AF;");
            lblFeedbackDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #3B82F6;");
        }
        lblFeedbackTitle.setText(title);
        lblFeedbackDesc.setText(message);
    }

    private VBox buildStrengthMeter() {
        VBox box = new VBox(4);
        box.setStyle("-fx-padding: 4 0 0 0;");

        HBox labelRow = new HBox();
        Label lblMeta = new Label("ĐỘ MẠNH MẬT KHẨU");
        lblMeta.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94A3B8;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        lblStrength.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94A3B8;");
        labelRow.getChildren().addAll(lblMeta, sp, lblStrength);

        HBox barRow = new HBox(4);
        for (int i = 0; i < 4; i++) {
            strBars[i] = new Region();
            strBars[i].getStyleClass().add("pwd-meter-bar-off");
            HBox.setHgrow(strBars[i], Priority.ALWAYS);
            barRow.getChildren().add(strBars[i]);
        }

        box.getChildren().addAll(labelRow, barRow);
        return box;
    }

    private void updatePasswordStrength(String pwd) {
        if (pwd == null || pwd.isEmpty()) {
            lblStrength.setText("Chưa nhập");
            lblStrength.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94A3B8;");
            for (Region b : strBars) b.getStyleClass().setAll("pwd-meter-bar-off");
            checkPasswordMatch();
            return;
        }

        int score = 0;
        if (pwd.length() >= 8) score++;
        if (pwd.matches(".*\\d.*")) score++;
        if (pwd.matches(".*[a-z].*") && pwd.matches(".*[A-Z].*")) score++;
        if (pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*") || pwd.length() >= 12) score++;

        for (Region b : strBars) b.getStyleClass().setAll("pwd-meter-bar-off");

        switch (score) {
            case 1 -> {
                strBars[0].getStyleClass().setAll("pwd-meter-bar-weak");
                lblStrength.setText("Yếu");
                lblStrength.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");
            }
            case 2 -> {
                strBars[0].getStyleClass().setAll("pwd-meter-bar-medium");
                strBars[1].getStyleClass().setAll("pwd-meter-bar-medium");
                lblStrength.setText("Trung bình");
                lblStrength.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #D97706;");
            }
            case 3 -> {
                strBars[0].getStyleClass().setAll("pwd-meter-bar-strong");
                strBars[1].getStyleClass().setAll("pwd-meter-bar-strong");
                strBars[2].getStyleClass().setAll("pwd-meter-bar-strong");
                lblStrength.setText("Khá mạnh");
                lblStrength.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #2563EB;");
            }
            case 4 -> {
                for (int i = 0; i < 4; i++) strBars[i].getStyleClass().setAll("pwd-meter-bar-secure");
                lblStrength.setText("Rất an toàn");
                lblStrength.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #059669;");
            }
        }
        checkPasswordMatch();
    }

    private void checkPasswordMatch() {
        String p1 = txtNewPassword.isVisible() ? txtNewPassword.getText() : txtNewPasswordPlain.getText();
        String p2 = txtConfirmPassword.isVisible() ? txtConfirmPassword.getText() : txtConfirmPasswordPlain.getText();

        if (p2 == null || p2.isEmpty()) {
            lblMatchStatus.setText("");
            return;
        }

        if (p1.equals(p2)) {
            lblMatchStatus.setText("✓ Khớp mật khẩu");
            lblMatchStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #059669;");
        } else {
            lblMatchStatus.setText("✕ Chưa trùng khớp");
            lblMatchStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");
        }
    }

    private HBox createPasswordInput(PasswordField passField, TextField plainField, String prompt) {
        passField.setPromptText(prompt);
        plainField.setPromptText(prompt);
        passField.getStyleClass().add("form-input");
        plainField.getStyleClass().add("form-input");

        plainField.setVisible(false);
        plainField.setManaged(false);

        // Keep text in sync
        passField.textProperty().bindBidirectional(plainField.textProperty());

        Button btnToggle = new Button("👁");
        btnToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0 4;");
        btnToggle.setOnAction(e -> {
            boolean showingPlain = plainField.isVisible();
            plainField.setVisible(!showingPlain);
            plainField.setManaged(!showingPlain);
            passField.setVisible(showingPlain);
            passField.setManaged(showingPlain);
            btnToggle.setStyle(!showingPlain ? "-fx-background-color: transparent; -fx-text-fill: #2563EB; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0 4;"
                    : "-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0 4;");
        });

        HBox wrapper = new HBox(8);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(passField, Priority.ALWAYS);
        HBox.setHgrow(plainField, Priority.ALWAYS);

        StackPane inputStack = new StackPane(passField, plainField);
        HBox.setHgrow(inputStack, Priority.ALWAYS);

        wrapper.getChildren().addAll(inputStack, btnToggle);
        return wrapper;
    }

    private void handleResetForm() {
        txtOldPassword.clear();
        txtOldPasswordPlain.clear();
        txtNewPassword.clear();
        txtNewPasswordPlain.clear();
        txtConfirmPassword.clear();
        txtConfirmPasswordPlain.clear();
        lblMatchStatus.setText("");
        showFeedback("info", "Kiểm tra bảo vệ mật khẩu chủ động", "Vui lòng nhập mật khẩu hiện tại và tạo mật khẩu mới đạt tiêu chuẩn độ phức tạp (tối thiểu 8 ký tự, gồm chữ và số).");
    }

    private void handleSubmitPassword() {
        String oldPwd = txtOldPassword.isVisible() ? txtOldPassword.getText() : txtOldPasswordPlain.getText();
        String newPwd = txtNewPassword.isVisible() ? txtNewPassword.getText() : txtNewPasswordPlain.getText();
        String confirmPwd = txtConfirmPassword.isVisible() ? txtConfirmPassword.getText() : txtConfirmPasswordPlain.getText();

        if (oldPwd.isEmpty()) {
            showFeedback("error", "Thiếu thông tin", "Vui lòng nhập mật khẩu hiện tại để xác thực danh tính.");
            return;
        }

        if (newPwd.length() < 8) {
            showFeedback("error", "Mật khẩu chưa đủ độ dài", "Mật khẩu mới phải có tối thiểu 8 ký tự theo tiêu chuẩn an toàn ngân hàng.");
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            showFeedback("error", "Mật khẩu không khớp", "Xác nhận mật khẩu mới không trùng khớp. Vui lòng kiểm tra lại.");
            return;
        }

        char[] oldSecret = oldPwd.toCharArray();
        char[] newSecret = newPwd.toCharArray();
        try {
            String uname = (user != null) ? user.username() : "admin";
            auth.changePassword(uname, oldSecret, newSecret);
            showFeedback("success", "Đổi mật khẩu thành công!", "Mật khẩu mới đã được băm muối PBKDF2 và cập nhật thành công vào cơ sở dữ liệu SQLite.");
            txtOldPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
            lblMatchStatus.setText("");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showFeedback("error", "Không thể đổi mật khẩu", exception.getMessage());
        } finally {
            Arrays.fill(oldSecret, '\0');
            Arrays.fill(newSecret, '\0');
        }
    }
}

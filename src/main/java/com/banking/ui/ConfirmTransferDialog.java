package com.banking.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Custom premium-styled confirmation dialog for transfer operations.
 * Replaces the default JavaFX Alert with a fully themed NovaBank dialog.
 */
public class ConfirmTransferDialog {

    private boolean confirmed = false;

    /**
     * Shows a premium confirmation dialog and returns true if user confirmed.
     */
    public static boolean show(Stage owner, String amount, String recipientName,
                                String fromAccount, String fee, String reference) {
        ConfirmTransferDialog dialog = new ConfirmTransferDialog();
        dialog.display(owner, amount, recipientName, fromAccount, fee, reference);
        return dialog.confirmed;
    }

    private void display(Stage owner, String amount, String recipientName,
                         String fromAccount, String fee, String reference) {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("Xác nhận chuyển khoản");

        // ── ROOT CONTAINER ──
        VBox root = new VBox(0);
        root.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0.1, 0, 10);"
        );
        root.setPrefWidth(420);
        root.setMaxWidth(420);

        // ── HEADER: Gradient banner ──
        StackPane header = new StackPane();
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, #2563EB, #4F46E5);" +
            "-fx-background-radius: 16 16 0 0;" +
            "-fx-padding: 28 24 24 24;"
        );

        VBox headerContent = new VBox(6);
        headerContent.setAlignment(Pos.CENTER);

        // Shield icon
        StackPane iconCircle = new StackPane();
        iconCircle.setStyle(
            "-fx-background-color: rgba(255,255,255,0.2);" +
            "-fx-background-radius: 50%;" +
            "-fx-min-width: 56; -fx-min-height: 56;" +
            "-fx-max-width: 56; -fx-max-height: 56;"
        );
        SVGPath shieldIcon = new SVGPath();
        shieldIcon.setContent("M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z");
        shieldIcon.setStyle("-fx-fill: transparent; -fx-stroke: white; -fx-stroke-width: 2;");
        iconCircle.getChildren().add(shieldIcon);

        Label headerTitle = new Label("Xác nhận giao dịch");
        headerTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 800;");

        Label headerSub = new Label("Vui lòng kiểm tra thông tin trước khi xác nhận");
        headerSub.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13px;");

        headerContent.getChildren().addAll(iconCircle, headerTitle, headerSub);
        header.getChildren().add(headerContent);

        // ── BODY: Transaction details ──
        VBox body = new VBox(12);
        body.setPadding(new Insets(24, 28, 16, 28));

        // Amount highlight
        VBox amountBox = new VBox(4);
        amountBox.setAlignment(Pos.CENTER);
        amountBox.setStyle(
            "-fx-background-color: #F0F7FF;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 18 16 18 16;"
        );
        Label amountLabel = new Label("Số tiền chuyển");
        amountLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-weight: 500;");
        Label amountValue = new Label(amount);
        amountValue.setStyle("-fx-text-fill: #1E293B; -fx-font-size: 28px; -fx-font-weight: 800;");
        amountBox.getChildren().addAll(amountLabel, amountValue);

        // Detail rows
        VBox detailsBox = new VBox(0);
        detailsBox.setStyle(
            "-fx-background-color: #F8FAFC;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 4 0 4 0;"
        );

        detailsBox.getChildren().addAll(
            createDetailRow("Người nhận", recipientName),
            createSeparator(),
            createDetailRow("Từ tài khoản", fromAccount),
            createSeparator(),
            createDetailRow("Phí giao dịch", fee),
            createSeparator(),
            createDetailRow("Nội dung", reference)
        );

        // OTP notice
        HBox otpNotice = new HBox(8);
        otpNotice.setAlignment(Pos.CENTER_LEFT);
        otpNotice.setStyle(
            "-fx-background-color: #ECFDF5;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 14 10 14;"
        );
        SVGPath lockIcon = new SVGPath();
        lockIcon.setContent("M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z");
        lockIcon.setStyle("-fx-fill: #10B981; -fx-scale-x: 0.7; -fx-scale-y: 0.7;");
        Label otpText = new Label("Giao dịch được bảo mật bởi hệ thống NovaBank.");
        otpText.setStyle("-fx-text-fill: #065F46; -fx-font-size: 12px; -fx-font-weight: 600;");
        otpNotice.getChildren().addAll(lockIcon, otpText);

        body.getChildren().addAll(amountBox, detailsBox, otpNotice);

        // ── FOOTER: Action buttons ──
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 28, 24, 28));

        Button btnCancel = new Button("Huỷ bỏ");
        btnCancel.setStyle(
            "-fx-background-color: #F1F5F9;" +
            "-fx-text-fill: #475569;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 600;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 12 28 12 28;" +
            "-fx-cursor: hand;"
        );
        btnCancel.setOnMouseEntered(e -> btnCancel.setStyle(
            "-fx-background-color: #E2E8F0;" +
            "-fx-text-fill: #334155;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 600;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 12 28 12 28;" +
            "-fx-cursor: hand;"
        ));
        btnCancel.setOnMouseExited(e -> btnCancel.setStyle(
            "-fx-background-color: #F1F5F9;" +
            "-fx-text-fill: #475569;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 600;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 12 28 12 28;" +
            "-fx-cursor: hand;"
        ));

        Button btnConfirm = new Button("Xác nhận chuyển khoản →");
        btnConfirm.setStyle(
            "-fx-background-color: linear-gradient(to right, #2563EB, #4F46E5);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 700;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 12 28 12 28;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(37, 99, 235, 0.4), 8, 0.1, 0, 3);"
        );
        btnConfirm.setOnMouseEntered(e -> btnConfirm.setStyle(
            "-fx-background-color: linear-gradient(to right, #3B82F6, #6366F1);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 700;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 12 28 12 28;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(37, 99, 235, 0.6), 12, 0.2, 0, 5);"
        ));
        btnConfirm.setOnMouseExited(e -> btnConfirm.setStyle(
            "-fx-background-color: linear-gradient(to right, #2563EB, #4F46E5);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 700;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 12 28 12 28;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(37, 99, 235, 0.4), 8, 0.1, 0, 3);"
        ));

        btnCancel.setOnAction(e -> stage.close());
        btnConfirm.setOnAction(e -> {
            confirmed = true;
            stage.close();
        });

        footer.getChildren().addAll(btnCancel, btnConfirm);

        // ── ASSEMBLE ──
        root.getChildren().addAll(header, body, footer);

        // Wrap in a transparent overlay pane for the rounded corners
        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setPadding(new Insets(10));

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
        stage.setResizable(false);
        stage.showAndWait();
    }

    private static HBox createDetailRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));

        Label lblKey = new Label(label);
        lblKey.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px; -fx-font-weight: 500;");
        lblKey.setMinWidth(110);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: #1E293B; -fx-font-size: 13px; -fx-font-weight: 700;");
        lblValue.setWrapText(true);

        row.getChildren().addAll(lblKey, spacer, lblValue);
        return row;
    }

    private static Separator createSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #E2E8F0;");
        return sep;
    }
}

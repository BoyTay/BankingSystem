package com.banking.ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * Hệ thống hiển thị thông báo Toast trượt mượt mà từ góc màn hình.
 * Hỗ trợ 4 loại trạng thái: SUCCESS, ERROR, WARNING, INFO.
 */
public class ToastNotification {

    public enum Type {
        SUCCESS, ERROR, WARNING, INFO
    }

    private static Pane globalContainer;

    public static void setGlobalContainer(Pane container) {
        globalContainer = container;
    }

    public static void showSuccess(String message) {
        show(globalContainer, message, Type.SUCCESS);
    }

    public static void showError(String message) {
        show(globalContainer, message, Type.ERROR);
    }

    public static void showWarning(String message) {
        show(globalContainer, message, Type.WARNING);
    }

    public static void showInfo(String message) {
        show(globalContainer, message, Type.INFO);
    }

    public static void show(Pane container, String message, Type type) {
        if (container == null) container = globalContainer;
        if (container == null) {
            System.out.println("[Toast fallback] " + type + ": " + message);
            return;
        }

        final Pane targetContainer = container;
        Platform.runLater(() -> {
            HBox toast = new HBox(10);
            toast.setAlignment(Pos.CENTER_LEFT);
            toast.getStyleClass().add("toast-box");
            toast.setMaxWidth(380);
            toast.setMinWidth(280);

            // Icon & Type Style
            Label iconLbl = new Label();
            iconLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

            switch (type) {
                case SUCCESS -> {
                    toast.getStyleClass().add("toast-success");
                    iconLbl.setText("✔");
                    iconLbl.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
                case ERROR -> {
                    toast.getStyleClass().add("toast-error");
                    iconLbl.setText("✖");
                    iconLbl.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
                case WARNING -> {
                    toast.getStyleClass().add("toast-warning");
                    iconLbl.setText("⚠");
                    iconLbl.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
                case INFO -> {
                    toast.getStyleClass().add("toast-info");
                    iconLbl.setText("ℹ");
                    iconLbl.setStyle("-fx-text-fill: #3B82F6; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
            }

            Label msgLbl = new Label(message);
            msgLbl.setWrapText(true);
            msgLbl.getStyleClass().add("toast-message");
            HBox.setHgrow(msgLbl, Priority.ALWAYS);

            Button btnClose = new Button("×");
            btnClose.getStyleClass().add("toast-close-btn");

            toast.getChildren().addAll(iconLbl, msgLbl, btnClose);

            targetContainer.getChildren().add(toast);

            // Animation vào: Slide in + Fade in
            toast.setTranslateX(160);
            toast.setOpacity(0);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(260), toast);
            slideIn.setToX(0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(260), toast);
            fadeIn.setToValue(1.0);

            ParallelTransition enterTransition = new ParallelTransition(slideIn, fadeIn);

            // Animation ra: Fade out + Slide out
            PauseTransition stay = new PauseTransition(Duration.millis(3500));

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(260), toast);
            slideOut.setToX(160);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(260), toast);
            fadeOut.setToValue(0);

            ParallelTransition exitTransition = new ParallelTransition(slideOut, fadeOut);
            exitTransition.setOnFinished(e -> targetContainer.getChildren().remove(toast));

            SequentialTransition fullSeq = new SequentialTransition(enterTransition, stay, exitTransition);

            btnClose.setOnAction(e -> {
                fullSeq.stop();
                targetContainer.getChildren().remove(toast);
            });

            fullSeq.play();
        });
    }
}

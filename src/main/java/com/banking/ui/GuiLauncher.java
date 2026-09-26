package com.banking.ui;

/**
 * Lớp khởi chạy trung gian không kế thừa Application.
 * Giúp tương thích hoàn hảo khi chạy JavaFX trên classpath trong môi trường Java 17+.
 */
public class GuiLauncher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}

// Pattern: Command (TransactionHistory) — Lưu lịch sử lệnh để hỗ trợ undo
package com.banking.pattern.behavioral;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Lưu trữ stack các Command đã thực thi.
 * Hỗ trợ undoLast() — hoàn tác lệnh gần nhất.
 */
public class TransactionHistory {

    private final Deque<Command> history = new ArrayDeque<>();

    /**
     * Thực thi command và đưa vào stack lịch sử.
     */
    public void executeCommand(Command command) {
        command.execute();
        history.push(command);
    }

    /**
     * Hoàn tác command gần nhất.
     */
    public boolean undoLast() {
        if (history.isEmpty()) {
            System.out.println("  [TransactionHistory] Không còn giao dịch nào để hoàn tác.");
            return false;
        }
        Command last = history.pop();
        System.out.println("  [TransactionHistory] Hoàn tác: " + last.describe());
        last.undo();
        return true;
    }

    /**
     * Số lượng command trong lịch sử.
     */
    public int size() {
        return history.size();
    }

    /**
     * Kiểm tra lịch sử có rỗng không.
     */
    public boolean isEmpty() {
        return history.isEmpty();
    }
}

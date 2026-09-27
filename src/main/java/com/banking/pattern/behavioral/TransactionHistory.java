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
    public boolean executeCommand(Command command) {
        if (!command.execute()) {
            return false;
        }
        history.push(command);
        return true;
    }

    /**
     * Hoàn tác command gần nhất.
     */
    public boolean undoLast() {
        if (history.isEmpty()) {
            System.out.println("  [TransactionHistory] Không còn giao dịch nào để hoàn tác.");
            return false;
        }
        Command last = history.peek();
        if (!last.undo()) {
            return false;
        }
        history.pop();
        System.out.println("  [TransactionHistory] Hoàn tác: " + last.describe());
        return true;
    }

    public Command peekLast() {
        return history.peek();
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

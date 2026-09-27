// Pattern: Command — Interface cho lệnh có thể thực thi và hoàn tác
package com.banking.pattern.behavioral;

/**
 * Command interface — mỗi command có thể execute() và undo().
 */
public interface Command {

    /**
     * Thực thi lệnh.
     */
    boolean execute();

    /**
     * Hoàn tác lệnh.
     */
    boolean undo();

    /**
     * Mô tả lệnh (để hiển thị trong lịch sử).
     */
    String describe();
}

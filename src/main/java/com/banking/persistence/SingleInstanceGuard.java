package com.banking.persistence;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Prevents two application processes from using the same SQLite file at once. */
public final class SingleInstanceGuard implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private SingleInstanceGuard(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    public static SingleInstanceGuard acquire(Path databaseFile) {
        Path file = databaseFile.toAbsolutePath().normalize();
        Path lockFile = file.resolveSibling(file.getFileName() + ".lock");
        try {
            Files.createDirectories(lockFile.getParent());
            FileChannel channel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            try {
                FileLock lock = channel.tryLock();
                if (lock == null) {
                    channel.close();
                    throw new IllegalStateException("Dữ liệu đang được một phiên VietBank khác sử dụng: " + file);
                }
                return new SingleInstanceGuard(channel, lock);
            } catch (OverlappingFileLockException e) {
                channel.close();
                throw new IllegalStateException("Dữ liệu đang được một phiên VietBank khác sử dụng: " + file, e);
            } catch (IOException | RuntimeException e) {
                channel.close();
                throw e;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không khóa được tệp dữ liệu: " + file, e);
        }
    }

    @Override
    public void close() {
        try {
            lock.release();
            channel.close();
        } catch (IOException e) {
            throw new IllegalStateException("Không giải phóng được khóa dữ liệu.", e);
        }
    }
}

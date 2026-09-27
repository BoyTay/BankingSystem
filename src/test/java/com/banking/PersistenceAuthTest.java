package com.banking;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.enums.AccountStatus;
import com.banking.model.enums.AccountType;
import com.banking.pattern.behavioral.LockedState;
import com.banking.persistence.SqliteStore;
import com.banking.service.AuthService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceAuthTest {
    @Test
    void restartRestoresAccountsLedgerAndUsers() throws Exception {
        Path file = Files.createTempDirectory("vietbank-store-").resolve("banking.db");
        SqliteStore first = new SqliteStore(file);
        Account source = new Account.Builder("ACC5001", "Nguyen A")
                .type(AccountType.STANDARD).balance(10_000).build();
        Account target = new Account.Builder("ACC5002", "Tran B")
                .type(AccountType.PREMIUM).status(AccountStatus.LOCKED)
                .state(new LockedState()).balance(2_000).build();
        first.saveAccount(source, true);
        first.saveAccount(target, true);
        source.setBalance(8_999);
        target.setBalance(3_000);
        Transaction original = new Transaction("TX-TEST1", "ACC5001", "ACC5002", 1_000, 1, "Test");
        first.saveTransaction(java.util.List.of(source, target), original);

        AuthService auth = new AuthService(file);
        assertTrue(auth.needsSetup());
        auth.createFirstAdmin("admin", "password123".toCharArray());
        auth.createUser("staff", "password456".toCharArray(), AuthService.Role.STAFF);

        SqliteStore reopened = new SqliteStore(file);
        assertEquals(2, reopened.loadAccounts().size());
        assertEquals(8_999, reopened.loadAccounts().get(0).getBalance());
        assertEquals(AccountStatus.LOCKED, reopened.loadAccounts().get(1).getStatus());
        assertEquals(original.getId(), reopened.loadTransactions().get(0).getId());
        assertEquals(AuthService.Role.STAFF,
                new AuthService(file).authenticate("staff", "password456".toCharArray()).role());
        assertNull(auth.authenticate("staff", "wrong-password".toCharArray()));
        auth.changePassword("staff", "password456".toCharArray(), "newPassword789".toCharArray());
        assertNull(auth.authenticate("staff", "password456".toCharArray()));
        assertNotNull(auth.authenticate("staff", "newPassword789".toCharArray()));
        assertFalse(auth.needsSetup());
    }
}

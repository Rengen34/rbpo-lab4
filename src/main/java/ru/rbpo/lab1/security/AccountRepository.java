package ru.rbpo.lab1.security;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccountRepository {
    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Account> findByUsername(String username) {
        return jdbc.query("""
                SELECT id, username, password_hash, role, status, procurement_user_id, created_at
                FROM accounts WHERE username = ?
                """, (rs, row) -> new Account(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getLong("procurement_user_id"),
                rs.getTimestamp("created_at").toInstant()), username).stream().findFirst();
    }

    public Account create(
            String username,
            String passwordHash,
            String role,
            String status,
            long procurementUserId,
            Instant createdAt) {
        Long nextId = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM accounts", Long.class);
        long id = nextId == null ? 1 : nextId;
        jdbc.update("""
                INSERT INTO accounts
                    (id, username, password_hash, role, status, procurement_user_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, username, passwordHash, role, status, procurementUserId, Timestamp.from(createdAt));
        return new Account(id, username, passwordHash, role, status, procurementUserId, createdAt);
    }

    public List<AccountView> list() {
        return jdbc.query("""
                SELECT id, username, role, status, procurement_user_id, created_at
                FROM accounts ORDER BY id
                """, (rs, row) -> new AccountView(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getLong("procurement_user_id"),
                rs.getTimestamp("created_at").toInstant()));
    }

    public AccountView changeStatus(String username, String status) {
        int changed = jdbc.update("UPDATE accounts SET status = ? WHERE username = ?", status, username);
        if (changed == 0) {
            throw new IllegalArgumentException("Учётная запись не найдена");
        }
        Account account = findByUsername(username).orElseThrow();
        return account.view();
    }

    public record Account(
            long id,
            String username,
            String passwordHash,
            String role,
            String status,
            long procurementUserId,
            Instant createdAt) {
        AccountView view() {
            return new AccountView(id, username, role, status, procurementUserId, createdAt);
        }
    }

    public record AccountView(
            long id,
            String username,
            String role,
            String status,
            long procurementUserId,
            Instant createdAt) {
    }
}

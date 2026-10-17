package ru.rbpo.lab1.procurement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DatabasePermissionsTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void applicationUsesLimitedDatabaseUser() {
        assertThat(jdbc.queryForObject("SELECT CURRENT_USER", String.class)).isEqualTo("TEST_APP");
        assertThatThrownBy(() -> jdbc.execute("CREATE TABLE forbidden_probe(id INT)"))
                .isInstanceOf(DataAccessException.class);
    }
}

package ru.rbpo.lab1.procurement;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(DatabaseConfiguration.DatabaseProperties.class)
public class DatabaseConfiguration {
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,62}");
    private static final String[] TABLES = {
        "users", "purchase_requests", "suppliers", "commercial_offers", "purchase_orders", "accounts"
    };

    @Bean
    @Primary
    DataSource dataSource(DatabaseProperties properties, ResourceLoader resources) throws SQLException {
        validate(properties);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("rbpo-database");
        dataSource.setJdbcUrl(properties.url());
        dataSource.setUsername(properties.appUser());
        dataSource.setPassword(properties.appPassword());
        dataSource.setMaximumPoolSize(5);

        try (Connection connection = DriverManager.getConnection(
                properties.url(), properties.ownerUser(), properties.ownerPassword())) {
            ScriptUtils.executeSqlScript(connection, resources.getResource("classpath:schema.sql"));
            prepareRuntimeUser(connection, properties.appUser(), properties.appPassword());
            if (properties.seedData()) {
                ScriptUtils.executeSqlScript(connection, resources.getResource("classpath:data.sql"));
            }
            try (Connection ignored = dataSource.getConnection()) {
            }
        } catch (SQLException | RuntimeException error) {
            dataSource.close();
            throw error;
        }
        return dataSource;
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    private static void prepareRuntimeUser(Connection connection, String user, String password)
            throws SQLException {
        String passwordLiteral = quoteLiteral(password);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE USER IF NOT EXISTS " + user + " PASSWORD " + passwordLiteral);
            statement.execute("ALTER USER " + user + " SET PASSWORD " + passwordLiteral);
            for (String table : TABLES) {
                statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON " + table + " TO " + user);
            }
        }
    }

    private static void validate(DatabaseProperties properties) {
        if (properties.url() == null || !properties.url().startsWith("jdbc:h2:")) {
            throw new IllegalStateException("RBPO_DB_URL должен быть адресом H2");
        }
        requireIdentifier(properties.ownerUser(), "RBPO_DB_OWNER");
        requireIdentifier(properties.appUser(), "RBPO_DB_USER");
        requireSecret(properties.ownerPassword(), "RBPO_DB_OWNER_PASSWORD");
        requireSecret(properties.appPassword(), "RBPO_DB_PASSWORD");
        if (properties.ownerUser().equalsIgnoreCase(properties.appUser())) {
            throw new IllegalStateException("Владелец БД и пользователь приложения должны различаться");
        }
    }

    private static void requireIdentifier(String value, String name) {
        if (value == null || !SQL_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalStateException(name + " содержит недопустимое имя пользователя");
        }
    }

    private static void requireSecret(String value, String name) {
        if (value == null || value.length() < 8) {
            throw new IllegalStateException(name + " должен содержать не менее 8 символов");
        }
    }

    private static String quoteLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    @ConfigurationProperties("rbpo.database")
    public record DatabaseProperties(
            String url,
            String ownerUser,
            String ownerPassword,
            String appUser,
            String appPassword,
            boolean seedData) {
    }
}

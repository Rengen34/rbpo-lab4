package ru.rbpo.lab1.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(ManagerInitializer.ManagerSettings.class)
public class ManagerInitializer implements ApplicationRunner {
    private final AccountService accounts;
    private final ManagerSettings settings;

    public ManagerInitializer(AccountService accounts, ManagerSettings settings) {
        this.accounts = accounts;
        this.settings = settings;
    }

    @Override
    public void run(ApplicationArguments args) {
        accounts.createManagerIfMissing(
                settings.managerUsername(),
                settings.managerPassword(),
                settings.managerDisplayName());
    }

    @ConfigurationProperties("rbpo.security")
    public record ManagerSettings(
            String managerUsername,
            String managerPassword,
            String managerDisplayName) {
    }
}

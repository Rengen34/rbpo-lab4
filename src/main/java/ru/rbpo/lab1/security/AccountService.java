package ru.rbpo.lab1.security;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rbpo.lab1.procurement.ProcurementException;
import ru.rbpo.lab1.procurement.ProcurementModels.User;
import ru.rbpo.lab1.procurement.ProcurementModels.UserInput;
import ru.rbpo.lab1.procurement.ProcurementService;
import ru.rbpo.lab1.security.AccountRepository.Account;
import ru.rbpo.lab1.security.AccountRepository.AccountView;

@Service
public class AccountService {
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{4,40}");
    private static final Pattern UPPER = Pattern.compile(".*[A-ZА-Я].*");
    private static final Pattern LOWER = Pattern.compile(".*[a-zа-я].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final List<String> STATUSES = List.of("ACTIVE", "LOCKED", "DISABLED");

    private final AccountRepository accounts;
    private final ProcurementService procurement;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountService(
            AccountRepository accounts,
            ProcurementService procurement,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.accounts = accounts;
        this.procurement = procurement;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public AccountView register(RegistrationRequest request) {
        requireRequest(request);
        String username = normalizeUsername(request.username());
        validatePassword(request.password());
        String displayName = requiredText(request.displayName(), "displayName");
        if (accounts.findByUsername(username).isPresent()) {
            throw ProcurementException.conflict("Логин уже занят");
        }
        User user = procurement.createUser(new UserInput(displayName));
        return accounts.create(
                username,
                passwordEncoder.encode(request.password()),
                "USER",
                "ACTIVE",
                user.id(),
                clock.instant()).view();
    }

    @Transactional
    public void createManagerIfMissing(String usernameValue, String password, String displayNameValue) {
        String username = normalizeUsername(usernameValue);
        validatePassword(password);
        if (accounts.findByUsername(username).isPresent()) {
            return;
        }
        User user = procurement.createUser(new UserInput(requiredText(displayNameValue, "displayName")));
        accounts.create(username, passwordEncoder.encode(password), "MANAGER", "ACTIVE", user.id(), clock.instant());
    }

    public List<AccountView> list() {
        return accounts.list();
    }

    public AccountView changeStatus(String usernameValue, StatusRequest request) {
        requireRequest(request);
        String username = normalizeUsername(usernameValue);
        String status = requiredText(request.status(), "status").toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw ProcurementException.invalid("Статус должен быть ACTIVE, LOCKED или DISABLED");
        }
        try {
            return accounts.changeStatus(username, status);
        } catch (IllegalArgumentException error) {
            throw ProcurementException.missing(error.getMessage());
        }
    }

    private static String normalizeUsername(String value) {
        String username = requiredText(value, "username").toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(username).matches()) {
            throw ProcurementException.invalid("Логин: 4–40 символов, латинские буквы, цифры, точка, дефис или подчёркивание");
        }
        return username;
    }

    private static void validatePassword(String value) {
        if (value == null || value.length() < 10 || !UPPER.matcher(value).matches()
                || !LOWER.matcher(value).matches() || !DIGIT.matcher(value).matches()) {
            throw ProcurementException.invalid("Пароль: не менее 10 символов, верхний и нижний регистр, цифра");
        }
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ProcurementException.invalid("Поле " + field + " обязательно");
        }
        return value.trim();
    }

    private static void requireRequest(Object value) {
        if (value == null) {
            throw ProcurementException.invalid("Тело запроса обязательно");
        }
    }

    public record RegistrationRequest(String username, String password, String displayName) {
    }

    public record StatusRequest(String status) {
    }
}

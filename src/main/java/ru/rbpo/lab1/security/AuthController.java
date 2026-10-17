package ru.rbpo.lab1.security;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rbpo.lab1.security.AccountRepository.AccountView;
import ru.rbpo.lab1.security.AccountService.RegistrationRequest;
import ru.rbpo.lab1.security.AccountService.StatusRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of(
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName(),
                "token", token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<AccountView> register(@RequestBody RegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.register(request));
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasRole('MANAGER')")
    public List<AccountView> list() {
        return accounts.list();
    }

    @PatchMapping("/accounts/{username}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public AccountView changeStatus(
            @PathVariable String username,
            @RequestBody StatusRequest request) {
        return accounts.changeStatus(username, request);
    }
}

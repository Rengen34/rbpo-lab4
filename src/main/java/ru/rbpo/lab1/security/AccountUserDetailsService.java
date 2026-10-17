package ru.rbpo.lab1.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final AccountRepository accounts;

    public AccountUserDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return accounts.findByUsername(username.toLowerCase())
                .map(account -> new AccountPrincipal(
                        account.id(),
                        account.procurementUserId(),
                        account.username(),
                        account.passwordHash(),
                        account.role(),
                        account.status()))
                .orElseThrow(() -> new UsernameNotFoundException("Неверный логин или пароль"));
    }
}

package ru.rbpo.lab1.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");

        return http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers(
                                "/api/auth/accounts/**",
                                "/api/users/**",
                                "/api/suppliers/**",
                                "/api/commercial-offers/**",
                                "/api/purchase-orders/**").hasRole("MANAGER")
                        .requestMatchers("/api/purchase-requests/**", "/api/business/**").authenticated()
                        .anyRequest().permitAll())
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, error) ->
                        writeError(response, 401, "AUTHENTICATION_REQUIRED", "Требуется вход")))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, error) ->
                        writeError(response, 403, "ACCESS_DENIED", "Недостаточно прав или отсутствует CSRF-токен")))
                .build();
    }

    private static void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}

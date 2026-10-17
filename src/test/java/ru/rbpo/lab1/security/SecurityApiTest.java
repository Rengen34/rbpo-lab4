package ru.rbpo.lab1.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityApiTest {
    private static final String MANAGER_PASSWORD = "ManagerPass123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void distinguishesAuthenticationAndCsrfErrors() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").isNotEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no_csrf\",\"password\":\"StudentPass123\",\"displayName\":\"Без токена\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void registrationHashesPasswordAndCannotGrantManagerRole() throws Exception {
        JsonNode registered = register("student_one", "StudentPass123", "Студент Один");
        assertThat(registered.path("role").asText()).isEqualTo("USER");
        assertThat(registered.has("passwordHash")).isFalse();
        var saved = accounts.findByUsername("student_one").orElseThrow();
        assertThat(saved.passwordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("StudentPass123", saved.passwordHash())).isTrue();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student_one\",\"password\":\"StudentPass123\",\"displayName\":\"Дубль\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"weak_user\",\"password\":\"weak\",\"displayName\":\"Слабый пароль\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"role_user\",\"password\":\"StudentPass123\",\"displayName\":\"Роль\",\"role\":\"MANAGER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userManagesOwnRequestButManagerControlsSuppliers() throws Exception {
        String username = "student_two";
        String password = "StudentPass123";
        JsonNode registered = register(username, password, "Студент Два");
        long userId = registered.path("procurementUserId").asLong();

        mockMvc.perform(get("/api/suppliers").with(httpBasic(username, password)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/suppliers")
                        .with(httpBasic(username, password)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Запрещённый\",\"category\":\"Техника\",\"admitted\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/suppliers")
                        .with(httpBasic("manager", MANAGER_PASSWORD)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Разрешённый\",\"category\":\"Техника\",\"admitted\":true}"))
                .andExpect(status().isCreated());

        long requestId = json(mockMvc.perform(post("/api/purchase-requests")
                        .with(httpBasic(username, password)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":%d,\"description\":\"Своя заявка\",\"maxAmount\":50000,\"status\":\"DRAFT\"}".formatted(userId)))
                .andExpect(status().isCreated()).andReturn()).path("id").asLong();
        mockMvc.perform(post("/api/business/purchase-requests/{id}/publish", requestId)
                        .with(httpBasic(username, password)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(post("/api/purchase-requests")
                        .with(httpBasic(username, password)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":%d,\"description\":\"Чужая заявка\",\"maxAmount\":50000,\"status\":\"DRAFT\"}".formatted(managerUserId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void lockedAccountCanNoLongerAuthenticate() throws Exception {
        String username = "student_lock";
        String password = "StudentPass123";
        register(username, password, "Блокируемый пользователь");
        mockMvc.perform(get("/api/users").with(httpBasic(username, password)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/auth/accounts/{username}/status", username)
                        .with(httpBasic("manager", MANAGER_PASSWORD)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));
        mockMvc.perform(get("/api/users").with(httpBasic(username, password)))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode register(String username, String password, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\",\"displayName\":\"%s\"}"
                                .formatted(username, password, displayName)))
                .andExpect(status().isCreated()).andReturn();
        return json(result);
    }

    private long managerUserId() throws Exception {
        JsonNode body = json(mockMvc.perform(get("/api/auth/accounts")
                        .with(httpBasic("manager", MANAGER_PASSWORD)))
                .andExpect(status().isOk()).andReturn());
        return StreamSupport.stream(body.spliterator(), false)
                .filter(item -> "manager".equals(item.path("username").asText()))
                .findFirst().orElseThrow().path("procurementUserId").asLong();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}

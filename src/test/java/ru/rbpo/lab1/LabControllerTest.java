package ru.rbpo.lab1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class LabControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.application.name}")
    private String applicationName;

    @Test
    void messageReturnsJsonText() throws Exception {
        mockMvc.perform(get("/api/message"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Проект работает"));
    }

    @Test
    void numbersReturnsJsonArray() throws Exception {
        mockMvc.perform(get("/api/numbers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numbers.length()").value(3))
                .andExpect(jsonPath("$.numbers[0]").value(1))
                .andExpect(jsonPath("$.numbers[1]").value(2))
                .andExpect(jsonPath("$.numbers[2]").value(3));
    }

    @Test
    void unknownPathReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void applicationNameMatchesTicket() {
        assertThat(applicationName).isEqualTo("1БКС24106");
    }
}

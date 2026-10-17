package ru.rbpo.lab1.procurement;

import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "manager", roles = "MANAGER")
class BusinessOperationsApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void completesOrderAndProtectsActiveSupplier() throws Exception {
        Scenario scenario = createScenario(true, 90_000, 100_000);

        mockMvc.perform(post("/api/business/purchase-requests/{id}/publish", scenario.requestId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        long orderId = json(mockMvc.perform(post(
                        "/api/business/purchase-requests/{requestId}/select-offer/{offerId}",
                        scenario.requestId(), scenario.offerId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fulfillmentStatus").value("PENDING"))
                .andReturn()).path("id").asLong();

        mockMvc.perform(post("/api/business/suppliers/{id}/suspend", scenario.supplierId()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/business/purchase-orders/{id}/complete", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fulfillmentStatus").value("COMPLETED"));

        mockMvc.perform(get("/api/purchase-requests/{id}", scenario.requestId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/business/purchase-orders/{id}/complete", orderId))
                .andExpect(status().isConflict());
    }

    @Test
    void cancellationReopensRequestAndAllowsSupplierSuspension() throws Exception {
        Scenario scenario = createScenario(true, 80_000, 100_000);
        mockMvc.perform(post("/api/business/purchase-requests/{id}/publish", scenario.requestId()))
                .andExpect(status().isOk());
        long orderId = json(mockMvc.perform(post(
                        "/api/business/purchase-requests/{requestId}/select-offer/{offerId}",
                        scenario.requestId(), scenario.offerId()))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asLong();

        mockMvc.perform(post("/api/business/purchase-orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fulfillmentStatus").value("CANCELLED"));
        mockMvc.perform(get("/api/purchase-requests/{id}", scenario.requestId()))
                .andExpect(jsonPath("$.status").value("OPEN"));
        mockMvc.perform(post("/api/business/suppliers/{id}/suspend", scenario.supplierId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admitted").value(false));
    }

    @Test
    void rejectsOfferOutsideBusinessRules() throws Exception {
        Scenario expensive = createScenario(true, 120_000, 100_000);
        mockMvc.perform(post("/api/business/purchase-requests/{id}/publish", expensive.requestId()))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/business/purchase-requests/{requestId}/select-offer/{offerId}",
                        expensive.requestId(), expensive.offerId()))
                .andExpect(status().isConflict());

        Scenario blocked = createScenario(false, 90_000, 100_000);
        mockMvc.perform(post("/api/business/purchase-requests/{id}/publish", blocked.requestId()))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/business/purchase-requests/{requestId}/select-offer/{offerId}",
                        blocked.requestId(), blocked.offerId()))
                .andExpect(status().isConflict());

        assertThat(listSize("purchase-orders")).isGreaterThanOrEqualTo(0);
    }

    private Scenario createScenario(boolean admitted, int price, int maxAmount) throws Exception {
        long suffix = System.nanoTime();
        long userId = create("users", "{\"name\":\"Автор %d\"}".formatted(suffix));
        long requestId = create("purchase-requests", """
                {"authorId":%d,"description":"Закупка %d","maxAmount":%d,"status":"DRAFT"}
                """.formatted(userId, suffix, maxAmount));
        long supplierId = create("suppliers", """
                {"name":"Поставщик %d","category":"Техника","admitted":%s}
                """.formatted(suffix, admitted));
        long offerId = create("commercial-offers", """
                {"purchaseRequestId":%d,"supplierId":%d,"price":%d,"deliveryDays":5,"validityDays":20}
                """.formatted(requestId, supplierId, price));
        return new Scenario(requestId, supplierId, offerId);
    }

    private long create(String resource, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/" + resource)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("id").asLong();
    }

    private int listSize(String resource) throws Exception {
        return json(mockMvc.perform(get("/api/" + resource))
                .andExpect(status().isOk())
                .andReturn()).size();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private record Scenario(long requestId, long supplierId, long offerId) {
    }
}

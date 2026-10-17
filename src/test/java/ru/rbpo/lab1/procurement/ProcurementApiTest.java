package ru.rbpo.lab1.procurement;

import org.springframework.security.test.context.support.WithMockUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "manager", roles = "MANAGER")
class ProcurementApiTest {
    private static final String FIXED_TIME = "2026-09-19T12:34:56Z";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Test
    void completeResourceLifecycle() throws Exception {
        for (String resource : List.of("users", "purchase-requests", "suppliers", "commercial-offers", "purchase-orders")) {
            assertThat(list(resource).isArray()).isTrue();
        }
        JsonNode user = create("users", "{\"name\":\"Анна\"}");
        long userId = user.path("id").asLong();
        JsonNode request = create("purchase-requests", "{\"authorId\":%d,\"description\":\"Ноутбук\",\"maxAmount\":45000.50,\"status\":\"Новая\"}".formatted(userId));
        long requestId = request.path("id").asLong();
        JsonNode supplier = create("suppliers", "{\"name\":\"Поставка\",\"category\":\"Техника\",\"admitted\":true}");
        long supplierId = supplier.path("id").asLong();
        JsonNode offer = create("commercial-offers", "{\"purchaseRequestId\":%d,\"supplierId\":%d,\"price\":42000.25,\"deliveryDays\":5,\"validityDays\":14}".formatted(requestId, supplierId));
        long offerId = offer.path("id").asLong();
        JsonNode order = create("purchase-orders", "{\"purchaseRequestId\":%d,\"selectedOfferId\":%d,\"fulfillmentStatus\":\"Ожидает\"}".formatted(requestId, offerId));
        long orderId = order.path("id").asLong();
        assertThat(read("users", userId).path("name").asText()).isEqualTo("Анна");
        assertThat(read("purchase-requests", requestId).path("authorId").asLong()).isEqualTo(userId);
        assertThat(read("suppliers", supplierId).path("admitted").asBoolean()).isTrue();
        assertThat(read("commercial-offers", offerId).path("purchaseRequestId").asLong()).isEqualTo(requestId);
        assertThat(read("purchase-orders", orderId).path("selectedOfferId").asLong()).isEqualTo(offerId);
        assertThat(list("users").findValuesAsText("id")).contains(Long.toString(userId));
        assertThat(list("purchase-requests").findValuesAsText("id")).contains(Long.toString(requestId));
        assertThat(list("suppliers").findValuesAsText("id")).contains(Long.toString(supplierId));
        assertThat(list("commercial-offers").findValuesAsText("id")).contains(Long.toString(offerId));
        assertThat(list("purchase-orders").findValuesAsText("id")).contains(Long.toString(orderId));
        assertThat(patchRecord("users", userId, "{\"name\":\"Анна Иванова\"}").path("name").asText()).isEqualTo("Анна Иванова");
        JsonNode changedRequest = patchRecord("purchase-requests", requestId, "{\"status\":\"Согласована\"}");
        assertThat(changedRequest.path("status").asText()).isEqualTo("Согласована");
        assertThat(changedRequest.path("authorId").asLong()).isEqualTo(userId);
        assertThat(changedRequest.path("description").asText()).isEqualTo("Ноутбук");
        assertThat(changedRequest.path("maxAmount").decimalValue()).isEqualByComparingTo("45000.50");
        JsonNode changedSupplier = patchRecord("suppliers", supplierId, "{\"category\":\"Оргтехника\"}");
        assertThat(changedSupplier.path("name").asText()).isEqualTo("Поставка");
        assertThat(changedSupplier.path("admitted").asBoolean()).isTrue();
        JsonNode changedOffer = patchRecord("commercial-offers", offerId, "{\"deliveryDays\":7}");
        assertThat(changedOffer.path("purchaseRequestId").asLong()).isEqualTo(requestId);
        assertThat(changedOffer.path("supplierId").asLong()).isEqualTo(supplierId);
        assertThat(changedOffer.path("price").decimalValue()).isEqualByComparingTo("42000.25");
        assertThat(changedOffer.path("validityDays").asInt()).isEqualTo(14);
        JsonNode changedOrder = patchRecord("purchase-orders", orderId, "{\"fulfillmentStatus\":\"Доставлен\"}");
        assertThat(changedOrder.path("purchaseRequestId").asLong()).isEqualTo(requestId);
        assertThat(changedOrder.path("selectedOfferId").asLong()).isEqualTo(offerId);
        assertThat(read("purchase-orders", orderId).path("fulfillmentStatus").asText()).isEqualTo("Доставлен");
        assertDeleted("purchase-orders", orderId);
        assertDeleted("commercial-offers", offerId);
        assertDeleted("purchase-requests", requestId);
        assertDeleted("suppliers", supplierId);
        assertDeleted("users", userId);
    }
    @Test
    void rejectsInvalidAndServerOwnedFields() throws Exception {
        expectInvalid(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"));
        expectInvalid(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Анна\",\"unknown\":1}"));
        expectInvalid(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Поставка\",\"category\":\"Техника\"}"));
        for (String field : List.of("\"id\":1", "\"createdAt\":\"2000-01-01T00:00:00Z\"", "\"updatedAt\":\"2000-01-01T00:00:00Z\"", "\"currentTime\":\"2000-01-01T00:00:00Z\"")) {
            expectInvalid(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Анна\",%s}".formatted(field)));
        }
        long userId = create("users", "{\"name\":\"Анна\"}").path("id").asLong();
        expectInvalid(patch("/api/users/{id}", userId).contentType(MediaType.APPLICATION_JSON).content("{\"name\":null}"));
        expectInvalid(patch("/api/users/{id}", userId).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Другое имя\",\"createdAt\":\"2000-01-01T00:00:00Z\"}"));
        expectInvalid(patch("/api/users/{id}", userId).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Другое имя\",\"unknown\":1}"));
        expectInvalid(patch("/api/users/{id}", userId).contentType(MediaType.APPLICATION_JSON).content("{}"));
        assertThat(read("users", userId).path("name").asText()).isEqualTo("Анна");
    }
    @Test
    void missingRecordsAndReferencesReturnNotFound() throws Exception {
        expectNotFound(get("/api/users/999999999"));
        expectNotFound(patch("/api/users/999999999").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Анна\"}"));
        expectNotFound(delete("/api/users/999999999"));
        expectNotFound(post("/api/purchase-requests").contentType(MediaType.APPLICATION_JSON).content("{\"authorId\":999999999,\"description\":\"Ноутбук\",\"maxAmount\":100,\"status\":\"Новая\"}"));
    }
    @Test
    void failedPatchPreservesRecord() throws Exception {
        long userId = create("users", "{\"name\":\"Борис\"}").path("id").asLong();
        long requestId = create("purchase-requests", "{\"authorId\":%d,\"description\":\"Принтер\",\"maxAmount\":12000,\"status\":\"Новая\"}".formatted(userId)).path("id").asLong();
        expectInvalid(patch("/api/purchase-requests/{id}", requestId).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"Согласована\",\"maxAmount\":-1}"));
        assertThat(read("purchase-requests", requestId).path("status").asText()).isEqualTo("Новая");
        expectNotFound(patch("/api/purchase-requests/{id}", requestId).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"Согласована\",\"authorId\":999999999}"));
        assertThat(read("purchase-requests", requestId).path("status").asText()).isEqualTo("Новая");
        assertThat(read("purchase-requests", requestId).path("authorId").asLong()).isEqualTo(userId);
    }
    @Test
    void incompatibleOffersAndLinkedDeletesPreserveGraph() throws Exception {
        long userId = create("users", "{\"name\":\"Вера\"}").path("id").asLong();
        long firstRequestId = create("purchase-requests", "{\"authorId\":%d,\"description\":\"Стол\",\"maxAmount\":8000,\"status\":\"Новая\"}".formatted(userId)).path("id").asLong();
        long secondRequestId = create("purchase-requests", "{\"authorId\":%d,\"description\":\"Шкаф\",\"maxAmount\":10000,\"status\":\"Новая\"}".formatted(userId)).path("id").asLong();
        long supplierId = create("suppliers", "{\"name\":\"Мебель\",\"category\":\"Офис\",\"admitted\":false}").path("id").asLong();
        long offerId = create("commercial-offers", "{\"purchaseRequestId\":%d,\"supplierId\":%d,\"price\":7000,\"deliveryDays\":3,\"validityDays\":10}".formatted(firstRequestId, supplierId)).path("id").asLong();
        expectConflict(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON).content("{\"purchaseRequestId\":%d,\"selectedOfferId\":%d,\"fulfillmentStatus\":\"Ожидает\"}".formatted(secondRequestId, offerId)));
        long orderId = create("purchase-orders", "{\"purchaseRequestId\":%d,\"selectedOfferId\":%d,\"fulfillmentStatus\":\"Ожидает\"}".formatted(firstRequestId, offerId)).path("id").asLong();
        expectConflict(patch("/api/commercial-offers/{id}", offerId).contentType(MediaType.APPLICATION_JSON).content("{\"purchaseRequestId\":%d}".formatted(secondRequestId)));
        expectConflict(patch("/api/purchase-orders/{id}", orderId).contentType(MediaType.APPLICATION_JSON).content("{\"purchaseRequestId\":%d}".formatted(secondRequestId)));
        assertThat(read("commercial-offers", offerId).path("purchaseRequestId").asLong()).isEqualTo(firstRequestId);
        assertThat(read("purchase-orders", orderId).path("purchaseRequestId").asLong()).isEqualTo(firstRequestId);
        expectConflict(delete("/api/users/{id}", userId));
        expectConflict(delete("/api/purchase-requests/{id}", firstRequestId));
        expectConflict(delete("/api/suppliers/{id}", supplierId));
        expectConflict(delete("/api/commercial-offers/{id}", offerId));
        assertThat(read("purchase-orders", orderId).path("selectedOfferId").asLong()).isEqualTo(offerId);
        assertThat(read("suppliers", supplierId).path("name").asText()).isEqualTo("Мебель");
    }
    private JsonNode create(String resource, String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/" + resource).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").value(FIXED_TIME))
                .andExpect(jsonPath("$.updatedAt").value(FIXED_TIME))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(body.path("id").asLong()).isPositive();
        return body;
    }
    private JsonNode patchRecord(String resource, long id, String json) throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/{resource}/{id}", resource, id).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.createdAt").value(FIXED_TIME))
                .andExpect(jsonPath("$.updatedAt").value(FIXED_TIME))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
    private JsonNode read(String resource, long id) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/{resource}/{id}", resource, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
    private JsonNode list(String resource) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/" + resource))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
    private void expectInvalid(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
    private void expectNotFound(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
    private void expectConflict(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
    private void assertDeleted(String resource, long id) throws Exception {
        mockMvc.perform(delete("/api/{resource}/{id}", resource, id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        expectNotFound(get("/api/{resource}/{id}", resource, id));
    }
    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse(FIXED_TIME), ZoneOffset.UTC);
        }
    }
}

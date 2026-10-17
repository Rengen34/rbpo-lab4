package ru.rbpo.lab1.procurement;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
public final class ProcurementModels {
    private ProcurementModels() {
    }
    public record User(
            long id,
            String name,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
    public record PurchaseRequest(
            long id,
            long authorId,
            String description,
            BigDecimal maxAmount,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
    public record Supplier(
            long id,
            String name,
            String category,
            boolean admitted,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
    public record CommercialOffer(
            long id,
            long purchaseRequestId,
            long supplierId,
            BigDecimal price,
            int deliveryDays,
            int validityDays,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
    public record PurchaseOrder(
            long id,
            long purchaseRequestId,
            long selectedOfferId,
            String fulfillmentStatus,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
    public record UserInput(
            @JsonSetter(nulls = Nulls.FAIL) String name
    ) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static UserInput fromJson(JsonNode json) {
            InputJson.requireObject(json, "name");
            return new UserInput(InputJson.text(json, "name"));
        }
    }
    public record PurchaseRequestInput(
            @JsonSetter(nulls = Nulls.FAIL) Long authorId,
            @JsonSetter(nulls = Nulls.FAIL) String description,
            @JsonSetter(nulls = Nulls.FAIL) BigDecimal maxAmount,
            @JsonSetter(nulls = Nulls.FAIL) String status
    ) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static PurchaseRequestInput fromJson(JsonNode json) {
            InputJson.requireObject(json, "authorId", "description", "maxAmount", "status");
            return new PurchaseRequestInput(
                    InputJson.longNumber(json, "authorId"),
                    InputJson.text(json, "description"),
                    InputJson.decimal(json, "maxAmount"),
                    InputJson.text(json, "status")
            );
        }
    }
    public record SupplierInput(
            @JsonSetter(nulls = Nulls.FAIL) String name,
            @JsonSetter(nulls = Nulls.FAIL) String category,
            @JsonSetter(nulls = Nulls.FAIL) Boolean admitted
    ) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static SupplierInput fromJson(JsonNode json) {
            InputJson.requireObject(json, "name", "category", "admitted");
            return new SupplierInput(
                    InputJson.text(json, "name"),
                    InputJson.text(json, "category"),
                    InputJson.bool(json, "admitted")
            );
        }
    }
    public record CommercialOfferInput(
            @JsonSetter(nulls = Nulls.FAIL) Long purchaseRequestId,
            @JsonSetter(nulls = Nulls.FAIL) Long supplierId,
            @JsonSetter(nulls = Nulls.FAIL) BigDecimal price,
            @JsonSetter(nulls = Nulls.FAIL) Integer deliveryDays,
            @JsonSetter(nulls = Nulls.FAIL) Integer validityDays
    ) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CommercialOfferInput fromJson(JsonNode json) {
            InputJson.requireObject(json, "purchaseRequestId", "supplierId", "price", "deliveryDays", "validityDays");
            return new CommercialOfferInput(
                    InputJson.longNumber(json, "purchaseRequestId"),
                    InputJson.longNumber(json, "supplierId"),
                    InputJson.decimal(json, "price"),
                    InputJson.integer(json, "deliveryDays"),
                    InputJson.integer(json, "validityDays")
            );
        }
    }
    public record PurchaseOrderInput(
            @JsonSetter(nulls = Nulls.FAIL) Long purchaseRequestId,
            @JsonSetter(nulls = Nulls.FAIL) Long selectedOfferId,
            @JsonSetter(nulls = Nulls.FAIL) String fulfillmentStatus
    ) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static PurchaseOrderInput fromJson(JsonNode json) {
            InputJson.requireObject(json, "purchaseRequestId", "selectedOfferId", "fulfillmentStatus");
            return new PurchaseOrderInput(
                    InputJson.longNumber(json, "purchaseRequestId"),
                    InputJson.longNumber(json, "selectedOfferId"),
                    InputJson.text(json, "fulfillmentStatus")
            );
        }
    }
    private static final class InputJson {
        private InputJson() {
        }
        private static void requireObject(JsonNode json, String... allowedFields) {
            if (json == null || !json.isObject()) {
                throw new IllegalArgumentException("Ожидается объект JSON");
            }
            Set<String> allowed = Set.of(allowedFields);
            Iterator<String> names = json.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (!allowed.contains(name)) {
                    throw new IllegalArgumentException("Неизвестное поле: " + name);
                }
            }
        }
        private static String text(JsonNode json, String name) {
            return read(json, name, JsonNode::isTextual, JsonNode::textValue);
        }
        private static Long longNumber(JsonNode json, String name) {
            return read(json, name, value -> value.isIntegralNumber() && value.canConvertToLong(), JsonNode::longValue);
        }
        private static BigDecimal decimal(JsonNode json, String name) {
            return read(json, name, JsonNode::isNumber, JsonNode::decimalValue);
        }
        private static Integer integer(JsonNode json, String name) {
            return read(json, name, value -> value.isIntegralNumber() && value.canConvertToInt(), JsonNode::intValue);
        }
        private static Boolean bool(JsonNode json, String name) {
            return read(json, name, JsonNode::isBoolean, JsonNode::booleanValue);
        }
        private static <T> T read(JsonNode json, String name, Predicate<JsonNode> valid, Function<JsonNode, T> convert) {
            JsonNode value = json.get(name);
            if (value == null) {
                return null;
            }
            if (value.isNull() || !valid.test(value)) {
                throw new IllegalArgumentException("Некорректное поле: " + name);
            }
            return convert.apply(value);
        }
    }
}

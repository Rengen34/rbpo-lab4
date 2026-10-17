package ru.rbpo.lab1.procurement;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rbpo.lab1.procurement.ProcurementModels.User;
import ru.rbpo.lab1.procurement.ProcurementModels.UserInput;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseRequest;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseRequestInput;
import ru.rbpo.lab1.procurement.ProcurementModels.Supplier;
import ru.rbpo.lab1.procurement.ProcurementModels.SupplierInput;
import ru.rbpo.lab1.procurement.ProcurementModels.CommercialOffer;
import ru.rbpo.lab1.procurement.ProcurementModels.CommercialOfferInput;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseOrder;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseOrderInput;
@Service
public class ProcurementService {
    private final JdbcProcurementRepository repository;
    private final Clock clock;
    public ProcurementService(JdbcProcurementRepository repository, Clock clock) {
        this.repository = java.util.Objects.requireNonNull(repository);
        this.clock = java.util.Objects.requireNonNull(clock);
    }
    public synchronized List<User> listUsers() {
        return repository.users.list();
    }
    public synchronized User getUser(long id) {
        return require(repository.users.find(id), "Пользователь", id);
    }
    public synchronized User createUser(UserInput input) {
        requireBody(input);
        String name = requiredText(input.name(), "name");
        long id = repository.users.nextId();
        Instant now = clock.instant();
        User created = new User(id, name, now, now);
        repository.users.save(id, created);
        return created;
    }
    public synchronized User updateUser(long id, UserInput input) {
        User current = getUser(id);
        requireBody(input);
        requireChange(input.name());
        String name = requiredText(input.name(), "name");
        User updated = new User(id, name, current.createdAt(), clock.instant());
        repository.users.save(id, updated);
        return updated;
    }
    public synchronized void deleteUser(long id) {
        getUser(id);
        if (repository.purchaseRequests.anyMatch(item -> item.authorId() == id)) {
            throw ProcurementException.conflict("Пользователь связан с заявкой");
        }
        repository.users.delete(id);
    }
    public synchronized List<PurchaseRequest> listPurchaseRequests() {
        return repository.purchaseRequests.list();
    }
    public synchronized PurchaseRequest getPurchaseRequest(long id) {
        return require(repository.purchaseRequests.find(id), "Заявка", id);
    }
    public synchronized PurchaseRequest createPurchaseRequest(PurchaseRequestInput input) {
        requireBody(input);
        long authorId = requiredId(input.authorId(), "authorId");
        getUser(authorId);
        String description = requiredText(input.description(), "description");
        BigDecimal maxAmount = positiveAmount(input.maxAmount(), "maxAmount");
        String status = requiredText(input.status(), "status");
        long id = repository.purchaseRequests.nextId();
        Instant now = clock.instant();
        PurchaseRequest created = new PurchaseRequest(id, authorId, description, maxAmount, status, now, now);
        repository.purchaseRequests.save(id, created);
        return created;
    }
    public synchronized PurchaseRequest updatePurchaseRequest(long id, PurchaseRequestInput input) {
        PurchaseRequest current = getPurchaseRequest(id);
        requireBody(input);
        requireChange(input.authorId(), input.description(), input.maxAmount(), input.status());
        long authorId = input.authorId() == null ? current.authorId() : requiredId(input.authorId(), "authorId");
        getUser(authorId);
        String description = input.description() == null ? current.description() : requiredText(input.description(), "description");
        BigDecimal maxAmount = input.maxAmount() == null ? current.maxAmount() : positiveAmount(input.maxAmount(), "maxAmount");
        String status = input.status() == null ? current.status() : requiredText(input.status(), "status");
        PurchaseRequest updated = new PurchaseRequest(id, authorId, description, maxAmount, status, current.createdAt(), clock.instant());
        repository.purchaseRequests.save(id, updated);
        return updated;
    }
    public synchronized void deletePurchaseRequest(long id) {
        getPurchaseRequest(id);
        if (repository.commercialOffers.anyMatch(item -> item.purchaseRequestId() == id)) {
            throw ProcurementException.conflict("Заявка связана с предложением");
        }
        if (repository.purchaseOrders.anyMatch(item -> item.purchaseRequestId() == id)) {
            throw ProcurementException.conflict("Заявка связана с заказом");
        }
        repository.purchaseRequests.delete(id);
    }
    public synchronized List<Supplier> listSuppliers() {
        return repository.suppliers.list();
    }
    public synchronized Supplier getSupplier(long id) {
        return require(repository.suppliers.find(id), "Поставщик", id);
    }
    public synchronized Supplier createSupplier(SupplierInput input) {
        requireBody(input);
        String name = requiredText(input.name(), "name");
        String category = requiredText(input.category(), "category");
        boolean admitted = requiredFlag(input.admitted(), "admitted");
        long id = repository.suppliers.nextId();
        Instant now = clock.instant();
        Supplier created = new Supplier(id, name, category, admitted, now, now);
        repository.suppliers.save(id, created);
        return created;
    }
    public synchronized Supplier updateSupplier(long id, SupplierInput input) {
        Supplier current = getSupplier(id);
        requireBody(input);
        requireChange(input.name(), input.category(), input.admitted());
        String name = input.name() == null ? current.name() : requiredText(input.name(), "name");
        String category = input.category() == null ? current.category() : requiredText(input.category(), "category");
        boolean admitted = input.admitted() == null ? current.admitted() : input.admitted();
        Supplier updated = new Supplier(id, name, category, admitted, current.createdAt(), clock.instant());
        repository.suppliers.save(id, updated);
        return updated;
    }
    public synchronized void deleteSupplier(long id) {
        getSupplier(id);
        if (repository.commercialOffers.anyMatch(item -> item.supplierId() == id)) {
            throw ProcurementException.conflict("Поставщик связан с предложением");
        }
        repository.suppliers.delete(id);
    }
    public synchronized List<CommercialOffer> listCommercialOffers() {
        return repository.commercialOffers.list();
    }
    public synchronized CommercialOffer getCommercialOffer(long id) {
        return require(repository.commercialOffers.find(id), "Предложение", id);
    }
    public synchronized CommercialOffer createCommercialOffer(CommercialOfferInput input) {
        requireBody(input);
        long requestId = requiredId(input.purchaseRequestId(), "purchaseRequestId");
        getPurchaseRequest(requestId);
        long supplierId = requiredId(input.supplierId(), "supplierId");
        getSupplier(supplierId);
        BigDecimal price = positiveAmount(input.price(), "price");
        int deliveryDays = positiveDays(input.deliveryDays(), "deliveryDays");
        int validityDays = positiveDays(input.validityDays(), "validityDays");
        long id = repository.commercialOffers.nextId();
        Instant now = clock.instant();
        CommercialOffer created = new CommercialOffer(id, requestId, supplierId, price, deliveryDays, validityDays, now, now);
        repository.commercialOffers.save(id, created);
        return created;
    }
    public synchronized CommercialOffer updateCommercialOffer(long id, CommercialOfferInput input) {
        CommercialOffer current = getCommercialOffer(id);
        requireBody(input);
        requireChange(input.purchaseRequestId(), input.supplierId(), input.price(), input.deliveryDays(), input.validityDays());
        long requestId = input.purchaseRequestId() == null ? current.purchaseRequestId() : requiredId(input.purchaseRequestId(), "purchaseRequestId");
        getPurchaseRequest(requestId);
        long supplierId = input.supplierId() == null ? current.supplierId() : requiredId(input.supplierId(), "supplierId");
        getSupplier(supplierId);
        if (repository.purchaseOrders.anyMatch(order -> order.selectedOfferId() == id && order.purchaseRequestId() != requestId)) {
            throw ProcurementException.conflict("Предложение уже выбрано заказом для другой заявки");
        }
        BigDecimal price = input.price() == null ? current.price() : positiveAmount(input.price(), "price");
        int deliveryDays = input.deliveryDays() == null ? current.deliveryDays() : positiveDays(input.deliveryDays(), "deliveryDays");
        int validityDays = input.validityDays() == null ? current.validityDays() : positiveDays(input.validityDays(), "validityDays");
        CommercialOffer updated = new CommercialOffer(id, requestId, supplierId, price, deliveryDays, validityDays, current.createdAt(), clock.instant());
        repository.commercialOffers.save(id, updated);
        return updated;
    }
    public synchronized void deleteCommercialOffer(long id) {
        getCommercialOffer(id);
        if (repository.purchaseOrders.anyMatch(item -> item.selectedOfferId() == id)) {
            throw ProcurementException.conflict("Предложение связано с заказом");
        }
        repository.commercialOffers.delete(id);
    }
    public synchronized List<PurchaseOrder> listPurchaseOrders() {
        return repository.purchaseOrders.list();
    }
    public synchronized PurchaseOrder getPurchaseOrder(long id) {
        return require(repository.purchaseOrders.find(id), "Заказ", id);
    }
    public synchronized PurchaseOrder createPurchaseOrder(PurchaseOrderInput input) {
        requireBody(input);
        long requestId = requiredId(input.purchaseRequestId(), "purchaseRequestId");
        getPurchaseRequest(requestId);
        long offerId = requiredId(input.selectedOfferId(), "selectedOfferId");
        CommercialOffer offer = getCommercialOffer(offerId);
        ensureMatchingOffer(requestId, offer);
        String status = requiredText(input.fulfillmentStatus(), "fulfillmentStatus");
        long id = repository.purchaseOrders.nextId();
        Instant now = clock.instant();
        PurchaseOrder created = new PurchaseOrder(id, requestId, offerId, status, now, now);
        repository.purchaseOrders.save(id, created);
        return created;
    }
    public synchronized PurchaseOrder updatePurchaseOrder(long id, PurchaseOrderInput input) {
        PurchaseOrder current = getPurchaseOrder(id);
        requireBody(input);
        requireChange(input.purchaseRequestId(), input.selectedOfferId(), input.fulfillmentStatus());
        long requestId = input.purchaseRequestId() == null ? current.purchaseRequestId() : requiredId(input.purchaseRequestId(), "purchaseRequestId");
        getPurchaseRequest(requestId);
        long offerId = input.selectedOfferId() == null ? current.selectedOfferId() : requiredId(input.selectedOfferId(), "selectedOfferId");
        CommercialOffer offer = getCommercialOffer(offerId);
        ensureMatchingOffer(requestId, offer);
        String status = input.fulfillmentStatus() == null ? current.fulfillmentStatus() : requiredText(input.fulfillmentStatus(), "fulfillmentStatus");
        PurchaseOrder updated = new PurchaseOrder(id, requestId, offerId, status, current.createdAt(), clock.instant());
        repository.purchaseOrders.save(id, updated);
        return updated;
    }
    public synchronized void deletePurchaseOrder(long id) {
        getPurchaseOrder(id);
        repository.purchaseOrders.delete(id);
    }
    @Transactional
    public synchronized PurchaseRequest publishPurchaseRequest(long id) {
        PurchaseRequest current = getPurchaseRequest(id);
        requireStatus(current.status(), "DRAFT", "Опубликовать можно только черновик заявки");
        PurchaseRequest updated = new PurchaseRequest(
                current.id(), current.authorId(), current.description(), current.maxAmount(), "OPEN",
                current.createdAt(), clock.instant());
        repository.purchaseRequests.save(id, updated);
        return updated;
    }

    @Transactional
    public synchronized PurchaseOrder selectOffer(long requestId, long offerId) {
        PurchaseRequest request = getPurchaseRequest(requestId);
        requireStatus(request.status(), "OPEN", "Выбрать предложение можно только для открытой заявки");
        CommercialOffer offer = getCommercialOffer(offerId);
        ensureMatchingOffer(requestId, offer);
        Supplier supplier = getSupplier(offer.supplierId());
        if (!supplier.admitted()) {
            throw ProcurementException.conflict("Поставщик не допущен к закупкам");
        }
        if (offer.price().compareTo(request.maxAmount()) > 0) {
            throw ProcurementException.conflict("Цена предложения превышает предел заявки");
        }
        if (repository.purchaseOrders.anyMatch(order ->
                order.purchaseRequestId() == requestId && "PENDING".equals(order.fulfillmentStatus()))) {
            throw ProcurementException.conflict("Для заявки уже есть активный заказ");
        }

        Instant now = clock.instant();
        long orderId = repository.purchaseOrders.nextId();
        PurchaseOrder order = new PurchaseOrder(orderId, requestId, offerId, "PENDING", now, now);
        PurchaseRequest updatedRequest = new PurchaseRequest(
                request.id(), request.authorId(), request.description(), request.maxAmount(), "ORDERED",
                request.createdAt(), now);
        repository.purchaseOrders.save(orderId, order);
        repository.purchaseRequests.save(requestId, updatedRequest);
        return order;
    }

    @Transactional
    public synchronized PurchaseOrder completeOrder(long id) {
        PurchaseOrder order = getPurchaseOrder(id);
        requireStatus(order.fulfillmentStatus(), "PENDING", "Завершить можно только активный заказ");
        PurchaseRequest request = getPurchaseRequest(order.purchaseRequestId());
        requireStatus(request.status(), "ORDERED", "Заявка не находится в исполнении");
        Instant now = clock.instant();
        PurchaseOrder completed = new PurchaseOrder(
                order.id(), order.purchaseRequestId(), order.selectedOfferId(), "COMPLETED",
                order.createdAt(), now);
        PurchaseRequest completedRequest = new PurchaseRequest(
                request.id(), request.authorId(), request.description(), request.maxAmount(), "COMPLETED",
                request.createdAt(), now);
        repository.purchaseOrders.save(id, completed);
        repository.purchaseRequests.save(request.id(), completedRequest);
        return completed;
    }

    @Transactional
    public synchronized PurchaseOrder cancelOrder(long id) {
        PurchaseOrder order = getPurchaseOrder(id);
        requireStatus(order.fulfillmentStatus(), "PENDING", "Отменить можно только активный заказ");
        PurchaseRequest request = getPurchaseRequest(order.purchaseRequestId());
        requireStatus(request.status(), "ORDERED", "Заявка не находится в исполнении");
        Instant now = clock.instant();
        PurchaseOrder cancelled = new PurchaseOrder(
                order.id(), order.purchaseRequestId(), order.selectedOfferId(), "CANCELLED",
                order.createdAt(), now);
        PurchaseRequest reopenedRequest = new PurchaseRequest(
                request.id(), request.authorId(), request.description(), request.maxAmount(), "OPEN",
                request.createdAt(), now);
        repository.purchaseOrders.save(id, cancelled);
        repository.purchaseRequests.save(request.id(), reopenedRequest);
        return cancelled;
    }

    @Transactional
    public synchronized Supplier suspendSupplier(long id) {
        Supplier supplier = getSupplier(id);
        if (!supplier.admitted()) {
            throw ProcurementException.conflict("Поставщик уже приостановлен");
        }
        boolean hasActiveOrder = repository.purchaseOrders.list().stream()
                .filter(order -> "PENDING".equals(order.fulfillmentStatus()))
                .map(order -> getCommercialOffer(order.selectedOfferId()))
                .anyMatch(offer -> offer.supplierId() == id);
        if (hasActiveOrder) {
            throw ProcurementException.conflict("Нельзя приостановить поставщика с активным заказом");
        }
        Supplier suspended = new Supplier(
                supplier.id(), supplier.name(), supplier.category(), false,
                supplier.createdAt(), clock.instant());
        repository.suppliers.save(id, suspended);
        return suspended;
    }

    private static void requireStatus(String actual, String expected, String message) {
        if (!expected.equals(actual)) {
            throw ProcurementException.conflict(message);
        }
    }
    private static <T> T require(Optional<T> item, String type, long id) {
        return item.orElseThrow(() -> ProcurementException.missing(type + " с id=" + id + " не найден"));
    }
    private static void requireBody(Object body) {
        if (body == null) {
            throw ProcurementException.invalid("Тело запроса обязательно");
        }
    }
    private static void requireChange(Object... fields) {
        for (Object field : fields) {
            if (field != null) {
                return;
            }
        }
        throw ProcurementException.invalid("Укажите хотя бы одно поле для изменения");
    }
    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ProcurementException.invalid("Поле " + field + " должно быть непустым");
        }
        return value.trim();
    }
    private static long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw ProcurementException.invalid("Поле " + field + " должно быть положительным id");
        }
        return value;
    }
    private static BigDecimal positiveAmount(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw ProcurementException.invalid("Поле " + field + " должно быть положительным");
        }
        return value;
    }
    private static int positiveDays(Integer value, String field) {
        if (value == null || value <= 0) {
            throw ProcurementException.invalid("Поле " + field + " должно быть положительным числом дней");
        }
        return value;
    }
    private static boolean requiredFlag(Boolean value, String field) {
        if (value == null) {
            throw ProcurementException.invalid("Поле " + field + " обязательно");
        }
        return value;
    }
    private static void ensureMatchingOffer(long requestId, CommercialOffer offer) {
        if (offer.purchaseRequestId() != requestId) {
            throw ProcurementException.conflict("Предложение относится к другой заявке");
        }
    }
}

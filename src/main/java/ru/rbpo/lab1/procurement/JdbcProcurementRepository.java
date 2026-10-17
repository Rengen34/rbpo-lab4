package ru.rbpo.lab1.procurement;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.rbpo.lab1.procurement.ProcurementModels.CommercialOffer;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseOrder;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseRequest;
import ru.rbpo.lab1.procurement.ProcurementModels.Supplier;
import ru.rbpo.lab1.procurement.ProcurementModels.User;

@Component
public class JdbcProcurementRepository {
    final UserStore users;
    final PurchaseRequestStore purchaseRequests;
    final SupplierStore suppliers;
    final CommercialOfferStore commercialOffers;
    final PurchaseOrderStore purchaseOrders;

    public JdbcProcurementRepository(DataSource dataSource) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        users = new UserStore(jdbc);
        purchaseRequests = new PurchaseRequestStore(jdbc);
        suppliers = new SupplierStore(jdbc);
        commercialOffers = new CommercialOfferStore(jdbc);
        purchaseOrders = new PurchaseOrderStore(jdbc);
    }

    abstract static class Store<T> {
        private final JdbcTemplate jdbc;
        private final String table;
        private final RowMapper<T> mapper;

        Store(JdbcTemplate jdbc, String table, RowMapper<T> mapper) {
            this.jdbc = jdbc;
            this.table = table;
            this.mapper = mapper;
        }

        long nextId() {
            Long value = jdbc.queryForObject(
                    "SELECT COALESCE(MAX(id), 0) + 1 FROM " + table, Long.class);
            return value == null ? 1 : value;
        }

        List<T> list() {
            return jdbc.query("SELECT * FROM " + table + " ORDER BY id", mapper);
        }

        Optional<T> find(long id) {
            return jdbc.query("SELECT * FROM " + table + " WHERE id = ?", mapper, id)
                    .stream()
                    .findFirst();
        }

        void delete(long id) {
            jdbc.update("DELETE FROM " + table + " WHERE id = ?", id);
        }

        boolean anyMatch(Predicate<T> predicate) {
            return list().stream().anyMatch(predicate);
        }

        JdbcTemplate jdbc() {
            return jdbc;
        }

        abstract void save(long id, T value);
    }

    static final class UserStore extends Store<User> {
        UserStore(JdbcTemplate jdbc) {
            super(jdbc, "users", (rs, row) -> new User(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()));
        }

        @Override
        void save(long id, User value) {
            jdbc().update("""
                    MERGE INTO users (id, name, created_at, updated_at) KEY(id)
                    VALUES (?, ?, ?, ?)
                    """, id, value.name(), Timestamp.from(value.createdAt()), Timestamp.from(value.updatedAt()));
        }
    }

    static final class PurchaseRequestStore extends Store<PurchaseRequest> {
        PurchaseRequestStore(JdbcTemplate jdbc) {
            super(jdbc, "purchase_requests", (rs, row) -> new PurchaseRequest(
                    rs.getLong("id"),
                    rs.getLong("author_id"),
                    rs.getString("description"),
                    rs.getBigDecimal("max_amount"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()));
        }

        @Override
        void save(long id, PurchaseRequest value) {
            jdbc().update("""
                    MERGE INTO purchase_requests
                        (id, author_id, description, max_amount, status, created_at, updated_at)
                    KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, id, value.authorId(), value.description(), value.maxAmount(), value.status(),
                    Timestamp.from(value.createdAt()), Timestamp.from(value.updatedAt()));
        }
    }

    static final class SupplierStore extends Store<Supplier> {
        SupplierStore(JdbcTemplate jdbc) {
            super(jdbc, "suppliers", (rs, row) -> new Supplier(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getBoolean("admitted"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()));
        }

        @Override
        void save(long id, Supplier value) {
            jdbc().update("""
                    MERGE INTO suppliers (id, name, category, admitted, created_at, updated_at) KEY(id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, value.name(), value.category(), value.admitted(),
                    Timestamp.from(value.createdAt()), Timestamp.from(value.updatedAt()));
        }
    }

    static final class CommercialOfferStore extends Store<CommercialOffer> {
        CommercialOfferStore(JdbcTemplate jdbc) {
            super(jdbc, "commercial_offers", (rs, row) -> new CommercialOffer(
                    rs.getLong("id"),
                    rs.getLong("purchase_request_id"),
                    rs.getLong("supplier_id"),
                    rs.getBigDecimal("price"),
                    rs.getInt("delivery_days"),
                    rs.getInt("validity_days"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()));
        }

        @Override
        void save(long id, CommercialOffer value) {
            jdbc().update("""
                    MERGE INTO commercial_offers
                        (id, purchase_request_id, supplier_id, price, delivery_days, validity_days, created_at, updated_at)
                    KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, value.purchaseRequestId(), value.supplierId(), value.price(), value.deliveryDays(),
                    value.validityDays(), Timestamp.from(value.createdAt()), Timestamp.from(value.updatedAt()));
        }
    }

    static final class PurchaseOrderStore extends Store<PurchaseOrder> {
        PurchaseOrderStore(JdbcTemplate jdbc) {
            super(jdbc, "purchase_orders", (rs, row) -> new PurchaseOrder(
                    rs.getLong("id"),
                    rs.getLong("purchase_request_id"),
                    rs.getLong("selected_offer_id"),
                    rs.getString("fulfillment_status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()));
        }

        @Override
        void save(long id, PurchaseOrder value) {
            jdbc().update("""
                    MERGE INTO purchase_orders
                        (id, purchase_request_id, selected_offer_id, fulfillment_status, created_at, updated_at)
                    KEY(id) VALUES (?, ?, ?, ?, ?, ?)
                    """, id, value.purchaseRequestId(), value.selectedOfferId(), value.fulfillmentStatus(),
                    Timestamp.from(value.createdAt()), Timestamp.from(value.updatedAt()));
        }
    }
}

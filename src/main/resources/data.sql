MERGE INTO users (id, name, created_at, updated_at) KEY(id)
VALUES (1, 'Анна Смирнова', TIMESTAMP '2026-10-01 06:00:00', TIMESTAMP '2026-10-01 06:00:00');

MERGE INTO purchase_requests (id, author_id, description, max_amount, status, created_at, updated_at) KEY(id)
VALUES (1, 1, 'Ноутбуки для отдела', 100000.00, 'DRAFT', TIMESTAMP '2026-10-01 06:05:00', TIMESTAMP '2026-10-01 06:05:00');

MERGE INTO suppliers (id, name, category, admitted, created_at, updated_at) KEY(id)
VALUES (1, 'Техника-Сервис', 'Компьютерная техника', TRUE, TIMESTAMP '2026-10-01 06:10:00', TIMESTAMP '2026-10-01 06:10:00');

MERGE INTO commercial_offers (id, purchase_request_id, supplier_id, price, delivery_days, validity_days, created_at, updated_at) KEY(id)
VALUES (1, 1, 1, 95000.00, 7, 30, TIMESTAMP '2026-10-01 06:15:00', TIMESTAMP '2026-10-01 06:15:00');

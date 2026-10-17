CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS purchase_requests (
    id BIGINT PRIMARY KEY,
    author_id BIGINT NOT NULL,
    description VARCHAR(1000) NOT NULL,
    max_amount DECIMAL(19, 2) NOT NULL CHECK (max_amount > 0),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_request_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(120) NOT NULL,
    admitted BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS commercial_offers (
    id BIGINT PRIMARY KEY,
    purchase_request_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    price DECIMAL(19, 2) NOT NULL CHECK (price > 0),
    delivery_days INTEGER NOT NULL CHECK (delivery_days > 0),
    validity_days INTEGER NOT NULL CHECK (validity_days > 0),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_offer_request FOREIGN KEY (purchase_request_id) REFERENCES purchase_requests(id),
    CONSTRAINT fk_offer_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGINT PRIMARY KEY,
    purchase_request_id BIGINT NOT NULL,
    selected_offer_id BIGINT NOT NULL,
    fulfillment_status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_request FOREIGN KEY (purchase_request_id) REFERENCES purchase_requests(id),
    CONSTRAINT fk_order_offer FOREIGN KEY (selected_offer_id) REFERENCES commercial_offers(id)
);

CREATE INDEX IF NOT EXISTS idx_request_author ON purchase_requests(author_id);
CREATE INDEX IF NOT EXISTS idx_offer_request ON commercial_offers(purchase_request_id);
CREATE INDEX IF NOT EXISTS idx_offer_supplier ON commercial_offers(supplier_id);
CREATE INDEX IF NOT EXISTS idx_order_request ON purchase_orders(purchase_request_id);
CREATE INDEX IF NOT EXISTS idx_order_offer ON purchase_orders(selected_offer_id);

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'MANAGER')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    procurement_user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_account_user FOREIGN KEY (procurement_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_account_username ON accounts(username);

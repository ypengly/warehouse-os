-- =====================================================================
-- WarehouseOS core schema
-- =====================================================================

-- ------------------------- identity ---------------------------------
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(32) NOT NULL UNIQUE
);

CREATE TABLE warehouses (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(32)  NOT NULL UNIQUE,
    name         VARCHAR(160) NOT NULL,
    address_line VARCHAR(255),
    city         VARCHAR(120),
    country      VARCHAR(120),
    postal_code  VARCHAR(32),
    contact_name  VARCHAR(160),
    contact_email VARCHAR(160),
    contact_phone VARCHAR(48),
    manager_id   BIGINT,
    status       VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    capacity_units INTEGER,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(160) NOT NULL,
    phone         VARCHAR(48),
    status        VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    warehouse_id  BIGINT REFERENCES warehouses (id) ON DELETE SET NULL,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_users_warehouse ON users (warehouse_id);

ALTER TABLE warehouses
    ADD CONSTRAINT fk_warehouse_manager
    FOREIGN KEY (manager_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 hex of the opaque token
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- ------------------------- catalogue --------------------------------
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    parent_id   BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE suppliers (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(32)  NOT NULL UNIQUE,
    company_name  VARCHAR(180) NOT NULL,
    contact_person VARCHAR(160),
    email         VARCHAR(160),
    phone         VARCHAR(48),
    address_line  VARCHAR(255),
    city          VARCHAR(120),
    country       VARCHAR(120),
    status        VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id             BIGSERIAL PRIMARY KEY,
    sku            VARCHAR(64)  NOT NULL UNIQUE,
    barcode        VARCHAR(64)  UNIQUE,
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    category_id    BIGINT REFERENCES categories (id) ON DELETE RESTRICT,
    brand          VARCHAR(120),
    unit_of_measure VARCHAR(16) NOT NULL DEFAULT 'PIECE',
    cost_price     NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (cost_price >= 0),
    selling_price  NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (selling_price >= 0),
    min_stock_level INTEGER NOT NULL DEFAULT 0 CHECK (min_stock_level >= 0),
    max_stock_level INTEGER CHECK (max_stock_level IS NULL OR max_stock_level >= 0),
    reorder_level   INTEGER NOT NULL DEFAULT 0 CHECK (reorder_level >= 0),
    weight_grams    INTEGER CHECK (weight_grams IS NULL OR weight_grams >= 0),
    length_mm       INTEGER,
    width_mm        INTEGER,
    height_mm       INTEGER,
    image_url       VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_products_stock_levels
        CHECK (max_stock_level IS NULL OR max_stock_level >= min_stock_level)
);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_active   ON products (active);
CREATE INDEX idx_products_name     ON products (lower(name));

CREATE TABLE product_suppliers (
    product_id       BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    supplier_id      BIGINT NOT NULL REFERENCES suppliers (id) ON DELETE CASCADE,
    supplier_sku     VARCHAR(64),
    lead_time_days   INTEGER CHECK (lead_time_days IS NULL OR lead_time_days >= 0),
    last_cost        NUMERIC(14,2),
    preferred        BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (product_id, supplier_id)
);

-- ------------------------- locations --------------------------------
CREATE TABLE warehouse_locations (
    id            BIGSERIAL PRIMARY KEY,
    warehouse_id  BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE CASCADE,
    code          VARCHAR(48) NOT NULL,   -- e.g. A-01-01
    zone          VARCHAR(16),
    aisle         VARCHAR(16),
    rack          VARCHAR(16),
    shelf         VARCHAR(16),
    bin           VARCHAR(16),
    capacity_units INTEGER CHECK (capacity_units IS NULL OR capacity_units >= 0),
    status        VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_location_code_per_warehouse UNIQUE (warehouse_id, code)
);
CREATE INDEX idx_locations_warehouse ON warehouse_locations (warehouse_id);

-- ------------------------- inventory --------------------------------
CREATE TABLE inventory (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    warehouse_id      BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE RESTRICT,
    location_id       BIGINT REFERENCES warehouse_locations (id) ON DELETE RESTRICT,
    quantity_on_hand  INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    damaged_quantity  INTEGER NOT NULL DEFAULT 0,
    reorder_level     INTEGER,
    last_movement_at  TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_inv_on_hand_non_negative  CHECK (quantity_on_hand  >= 0),
    CONSTRAINT chk_inv_reserved_non_negative CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inv_damaged_non_negative  CHECK (damaged_quantity  >= 0),
    -- the core invariant: available = on_hand - reserved must never go below zero
    CONSTRAINT chk_inv_available_non_negative CHECK (reserved_quantity <= quantity_on_hand)
);
-- one stock record per product/warehouse/location; NULL location handled separately
-- because Postgres treats NULLs as distinct in UNIQUE constraints.
CREATE UNIQUE INDEX uq_inventory_with_location
    ON inventory (product_id, warehouse_id, location_id) WHERE location_id IS NOT NULL;
CREATE UNIQUE INDEX uq_inventory_without_location
    ON inventory (product_id, warehouse_id) WHERE location_id IS NULL;
CREATE INDEX idx_inventory_warehouse ON inventory (warehouse_id);
CREATE INDEX idx_inventory_product   ON inventory (product_id);

CREATE TABLE stock_movements (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    warehouse_id      BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE RESTRICT,
    location_id       BIGINT REFERENCES warehouse_locations (id) ON DELETE RESTRICT,
    movement_type     VARCHAR(24) NOT NULL,
    quantity          INTEGER NOT NULL,          -- signed delta applied to on-hand
    previous_quantity INTEGER NOT NULL,
    new_quantity      INTEGER NOT NULL,
    reference_type    VARCHAR(32),               -- RECEIPT / TRANSFER / ADJUSTMENT / PO ...
    reference_number  VARCHAR(64),
    performed_by      BIGINT REFERENCES users (id) ON DELETE SET NULL,
    notes             VARCHAR(500),
    occurred_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_movements_product_time   ON stock_movements (product_id, occurred_at DESC);
CREATE INDEX idx_movements_warehouse_time ON stock_movements (warehouse_id, occurred_at DESC);
CREATE INDEX idx_movements_reference      ON stock_movements (reference_type, reference_number);

-- Stock movement history is an append-only ledger. Enforced at the DB level so
-- that no code path -- application, migration or manual psql -- can rewrite it.
CREATE OR REPLACE FUNCTION forbid_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Table % is append-only and cannot be updated or deleted', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_stock_movements_immutable
    BEFORE UPDATE OR DELETE ON stock_movements
    FOR EACH ROW EXECUTE FUNCTION forbid_mutation();

CREATE TABLE inventory_reservations (
    id            BIGSERIAL PRIMARY KEY,
    inventory_id  BIGINT NOT NULL REFERENCES inventory (id) ON DELETE RESTRICT,
    quantity      INTEGER NOT NULL CHECK (quantity > 0),
    status        VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/RELEASED/CONSUMED/EXPIRED
    reference_type VARCHAR(32),
    reference_number VARCHAR(64),
    created_by    BIGINT REFERENCES users (id) ON DELETE SET NULL,
    expires_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    released_at   TIMESTAMPTZ,
    version       BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_reservations_inventory ON inventory_reservations (inventory_id, status);

-- ------------------------- inbound ----------------------------------
CREATE TABLE purchase_orders (
    id            BIGSERIAL PRIMARY KEY,
    po_number     VARCHAR(48) NOT NULL UNIQUE,
    supplier_id   BIGINT NOT NULL REFERENCES suppliers (id) ON DELETE RESTRICT,
    warehouse_id  BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE RESTRICT,
    status        VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    total_amount  NUMERIC(16,2) NOT NULL DEFAULT 0,
    expected_date DATE,
    created_by    BIGINT REFERENCES users (id) ON DELETE SET NULL,
    approved_by   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE purchase_order_items (
    id                BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders (id) ON DELETE CASCADE,
    product_id        BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity_ordered  INTEGER NOT NULL CHECK (quantity_ordered > 0),
    quantity_received INTEGER NOT NULL DEFAULT 0 CHECK (quantity_received >= 0),
    unit_cost         NUMERIC(14,2) NOT NULL CHECK (unit_cost >= 0),
    CONSTRAINT uq_po_item UNIQUE (purchase_order_id, product_id),
    CONSTRAINT chk_po_item_not_over_received CHECK (quantity_received <= quantity_ordered)
);

CREATE TABLE goods_receipts (
    id                BIGSERIAL PRIMARY KEY,
    receipt_number    VARCHAR(48) NOT NULL UNIQUE,
    warehouse_id      BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE RESTRICT,
    supplier_id       BIGINT REFERENCES suppliers (id) ON DELETE SET NULL,
    purchase_order_id BIGINT REFERENCES purchase_orders (id) ON DELETE SET NULL,
    status            VARCHAR(24) NOT NULL DEFAULT 'DRAFT',  -- DRAFT/CONFIRMED/CANCELLED
    received_by       BIGINT REFERENCES users (id) ON DELETE SET NULL,
    confirmed_at      TIMESTAMPTZ,
    notes             VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE goods_receipt_items (
    id                BIGSERIAL PRIMARY KEY,
    goods_receipt_id  BIGINT NOT NULL REFERENCES goods_receipts (id) ON DELETE CASCADE,
    product_id        BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    location_id       BIGINT REFERENCES warehouse_locations (id) ON DELETE RESTRICT,
    quantity_received INTEGER NOT NULL CHECK (quantity_received > 0),
    quantity_damaged  INTEGER NOT NULL DEFAULT 0 CHECK (quantity_damaged >= 0),
    unit_cost         NUMERIC(14,2)
);

-- ------------------------- transfers --------------------------------
CREATE TABLE stock_transfers (
    id                       BIGSERIAL PRIMARY KEY,
    transfer_number          VARCHAR(48) NOT NULL UNIQUE,
    source_warehouse_id      BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE RESTRICT,
    destination_warehouse_id BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE RESTRICT,
    status        VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    requested_by  BIGINT REFERENCES users (id) ON DELETE SET NULL,
    approved_by   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    shipped_at    TIMESTAMPTZ,
    received_at   TIMESTAMPTZ,
    notes         VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_transfer_distinct_warehouses
        CHECK (source_warehouse_id <> destination_warehouse_id)
);

CREATE TABLE stock_transfer_items (
    id                  BIGSERIAL PRIMARY KEY,
    stock_transfer_id   BIGINT NOT NULL REFERENCES stock_transfers (id) ON DELETE CASCADE,
    product_id          BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity_requested  INTEGER NOT NULL CHECK (quantity_requested > 0),
    quantity_shipped    INTEGER NOT NULL DEFAULT 0 CHECK (quantity_shipped >= 0),
    quantity_received   INTEGER NOT NULL DEFAULT 0 CHECK (quantity_received >= 0),
    source_location_id      BIGINT REFERENCES warehouse_locations (id) ON DELETE SET NULL,
    destination_location_id BIGINT REFERENCES warehouse_locations (id) ON DELETE SET NULL,
    reservation_id      BIGINT REFERENCES inventory_reservations (id) ON DELETE SET NULL,
    CONSTRAINT uq_transfer_item UNIQUE (stock_transfer_id, product_id)
);

-- ------------------------- counting & picking -----------------------
CREATE TABLE inventory_counts (
    id           BIGSERIAL PRIMARY KEY,
    count_number VARCHAR(48) NOT NULL UNIQUE,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE RESTRICT,
    location_id  BIGINT REFERENCES warehouse_locations (id) ON DELETE SET NULL,
    status       VARCHAR(24) NOT NULL DEFAULT 'DRAFT', -- DRAFT/COUNTING/PENDING_APPROVAL/APPROVED/CANCELLED
    created_by   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    approved_by  BIGINT REFERENCES users (id) ON DELETE SET NULL,
    approved_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE inventory_count_items (
    id                  BIGSERIAL PRIMARY KEY,
    inventory_count_id  BIGINT NOT NULL REFERENCES inventory_counts (id) ON DELETE CASCADE,
    product_id          BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    location_id         BIGINT REFERENCES warehouse_locations (id) ON DELETE SET NULL,
    system_quantity     INTEGER NOT NULL,
    physical_quantity   INTEGER,
    counted_by          BIGINT REFERENCES users (id) ON DELETE SET NULL,
    counted_at          TIMESTAMPTZ,
    notes               VARCHAR(500)
);

CREATE TABLE picking_tasks (
    id            BIGSERIAL PRIMARY KEY,
    task_number   VARCHAR(48) NOT NULL UNIQUE,
    warehouse_id  BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE RESTRICT,
    assigned_to   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    status        VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    reference_type   VARCHAR(32),
    reference_number VARCHAR(64),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at  TIMESTAMPTZ,
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE picking_task_items (
    id               BIGSERIAL PRIMARY KEY,
    picking_task_id  BIGINT NOT NULL REFERENCES picking_tasks (id) ON DELETE CASCADE,
    product_id       BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    location_id      BIGINT REFERENCES warehouse_locations (id) ON DELETE SET NULL,
    quantity_requested INTEGER NOT NULL CHECK (quantity_requested > 0),
    quantity_picked    INTEGER NOT NULL DEFAULT 0 CHECK (quantity_picked >= 0),
    status           VARCHAR(24) NOT NULL DEFAULT 'PENDING'
);

-- ------------------------- cross-cutting ----------------------------
CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(48) NOT NULL,
    title      VARCHAR(200) NOT NULL,
    message    VARCHAR(1000),
    severity   VARCHAR(16) NOT NULL DEFAULT 'INFO',
    link       VARCHAR(255),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    username    VARCHAR(64),
    action      VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64),
    entity_id   VARCHAR(64),
    description VARCHAR(1000),
    ip_address  VARCHAR(64),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_time   ON audit_logs (occurred_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);

CREATE TRIGGER trg_audit_logs_immutable
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION forbid_mutation();

INSERT INTO roles (name) VALUES ('ADMIN'), ('MANAGER'), ('STAFF');

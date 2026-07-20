CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE categories
(
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(80) NOT NULL,
    color   VARCHAR(7)  NOT NULL,
    user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_category_user_name UNIQUE (user_id, name)
);
CREATE TABLE expenses
(
    id           BIGSERIAL PRIMARY KEY,
    amount       NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    description  VARCHAR(255)   NOT NULL,
    expense_date DATE           NOT NULL,
    receipt_key  VARCHAR(500),
    user_id      BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id  BIGINT         NOT NULL REFERENCES categories (id),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_expenses_user_date ON expenses (user_id, expense_date DESC);

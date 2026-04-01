-- VIP字段
ALTER TABLE profiles ADD COLUMN vip_tier VARCHAR(20) NOT NULL DEFAULT 'none';
ALTER TABLE profiles ADD COLUMN vip_expires_at TIMESTAMPTZ;

-- VIP订单表
CREATE TABLE vip_orders (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL REFERENCES profiles(id),
    tier              VARCHAR(20)  NOT NULL,
    days              INT          NOT NULL,
    amount            INT          NOT NULL,
    platform          VARCHAR(20),
    external_order_id VARCHAR(200),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_vip_orders_user ON vip_orders(user_id);

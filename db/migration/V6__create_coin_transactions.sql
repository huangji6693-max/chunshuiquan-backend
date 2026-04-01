-- 金币交易流水表
CREATE TABLE coin_transactions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES profiles(id),
    amount        INT          NOT NULL,
    balance_after INT          NOT NULL,
    type          VARCHAR(30)  NOT NULL,
    note          VARCHAR(200),
    order_id      VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_coin_tx_user ON coin_transactions(user_id);

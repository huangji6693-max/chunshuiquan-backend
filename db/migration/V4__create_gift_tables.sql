-- 虚拟礼物表
CREATE TABLE gifts (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    icon        VARCHAR(50)  NOT NULL,
    coins       INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 礼物赠送记录表
CREATE TABLE gift_records (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id   UUID         NOT NULL REFERENCES profiles(id),
    receiver_id UUID         NOT NULL REFERENCES profiles(id),
    gift_id     BIGINT       NOT NULL REFERENCES gifts(id),
    match_id    UUID         NOT NULL REFERENCES matches(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_gift_records_sender   ON gift_records(sender_id);
CREATE INDEX idx_gift_records_receiver ON gift_records(receiver_id);

-- 预置礼物数据
INSERT INTO gifts (name, icon, coins) VALUES
    ('玫瑰',   '🌹', 10),
    ('巧克力', '🍫', 20),
    ('钻戒',   '💍', 100),
    ('跑车',   '🏎️', 500),
    ('城堡',   '🏰', 1000);

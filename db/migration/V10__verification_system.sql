-- 实名认证
CREATE TABLE verifications (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES profiles(id),
    real_name     VARCHAR(50)  NOT NULL,
    id_photo_url  TEXT         NOT NULL,
    selfie_url    TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'pending',
    reject_reason VARCHAR(200),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_at   TIMESTAMPTZ
);

CREATE INDEX idx_verification_user   ON verifications(user_id);
CREATE INDEX idx_verification_status ON verifications(status);

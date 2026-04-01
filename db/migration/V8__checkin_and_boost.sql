-- 每日签到表
CREATE TABLE daily_checkins (
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES profiles(id),
    check_date DATE        NOT NULL,
    reward     INT         NOT NULL,
    streak_day INT         NOT NULL,
    UNIQUE(user_id, check_date)
);

CREATE INDEX idx_checkin_user_date ON daily_checkins(user_id, check_date);

-- 曝光加速字段
ALTER TABLE profiles ADD COLUMN boost_until TIMESTAMPTZ;

-- 春水圈交友App 初始建表脚本
-- 从 JPA Entity 推断，适用于 PostgreSQL

-- 用户资料表
CREATE TABLE IF NOT EXISTS profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    birth_date      DATE NOT NULL,
    gender          VARCHAR(50),
    looking_for     VARCHAR(50) DEFAULT 'everyone',
    bio             TEXT DEFAULT '',
    job_title       VARCHAR(255) DEFAULT '',
    avatar_urls     TEXT[],                          -- 头像URL数组
    photo_statuses  TEXT[],                          -- 照片审核状态数组（pending/approved/rejected）
    tags            TEXT[],                          -- 兴趣标签数组
    is_active       BOOLEAN DEFAULT TRUE,            -- 账号是否激活
    last_active     TIMESTAMPTZ,                     -- 最后活跃时间
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fcm_token       VARCHAR(512)                     -- Firebase 推送 token
);

-- 匹配表（双方互相喜欢后创建）
CREATE TABLE IF NOT EXISTS matches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id        UUID NOT NULL REFERENCES profiles(id),
    user2_id        UUID NOT NULL REFERENCES profiles(id),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user1_id, user2_id)
);

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id        UUID NOT NULL REFERENCES matches(id),
    sender_id       UUID NOT NULL REFERENCES profiles(id),
    content         TEXT NOT NULL,
    msg_type        VARCHAR(20) DEFAULT 'text',      -- 消息类型：text / image
    is_read         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 滑动记录表
CREATE TABLE IF NOT EXISTS swipes (
    id              BIGSERIAL PRIMARY KEY,
    swiper_id       UUID NOT NULL REFERENCES profiles(id),
    swiped_id       UUID NOT NULL REFERENCES profiles(id),
    direction       VARCHAR(20) NOT NULL,            -- left / right / super_like
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (swiper_id, swiped_id)
);

-- 屏蔽用户表
CREATE TABLE IF NOT EXISTS blocked_users (
    id              BIGSERIAL PRIMARY KEY,
    blocker_id      UUID NOT NULL REFERENCES profiles(id),
    blocked_id      UUID NOT NULL REFERENCES profiles(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (blocker_id, blocked_id)
);

-- 举报表
CREATE TABLE IF NOT EXISTS reports (
    id              BIGSERIAL PRIMARY KEY,
    reporter_id     UUID NOT NULL REFERENCES profiles(id),
    reported_id     UUID NOT NULL REFERENCES profiles(id),
    reason          VARCHAR(50) NOT NULL,            -- INAPPROPRIATE_PHOTO/SPAM/FAKE_PROFILE/HARASSMENT/UNDERAGE
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_profiles_email ON profiles(email);
CREATE INDEX IF NOT EXISTS idx_profiles_is_active ON profiles(is_active);
CREATE INDEX IF NOT EXISTS idx_matches_user1 ON matches(user1_id);
CREATE INDEX IF NOT EXISTS idx_matches_user2 ON matches(user2_id);
CREATE INDEX IF NOT EXISTS idx_messages_match_id ON messages(match_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(match_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_swipes_swiper ON swipes(swiper_id);
CREATE INDEX IF NOT EXISTS idx_swipes_swiped ON swipes(swiped_id);
CREATE INDEX IF NOT EXISTS idx_blocked_blocker ON blocked_users(blocker_id);
CREATE INDEX IF NOT EXISTS idx_blocked_blocked ON blocked_users(blocked_id);

-- 动态/朋友圈
CREATE TABLE moments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id     UUID         NOT NULL REFERENCES profiles(id),
    content       TEXT,
    image_urls    TEXT[],
    location      VARCHAR(100),
    visibility    VARCHAR(20)  NOT NULL DEFAULT 'public',
    like_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_moments_author   ON moments(author_id);
CREATE INDEX idx_moments_timeline ON moments(created_at DESC) WHERE is_deleted = FALSE;

-- 动态点赞
CREATE TABLE moment_likes (
    id         BIGSERIAL PRIMARY KEY,
    moment_id  UUID NOT NULL REFERENCES moments(id),
    user_id    UUID NOT NULL REFERENCES profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(moment_id, user_id)
);

-- 动态评论
CREATE TABLE moment_comments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id   UUID NOT NULL REFERENCES moments(id),
    author_id   UUID NOT NULL REFERENCES profiles(id),
    reply_to_id UUID,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_moment_comments ON moment_comments(moment_id);

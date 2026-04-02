-- 性能索引优化 2026-04-02
-- 解决 N+1 查询和慢查询问题

-- 消息查询：按match_id + 发送者 + 已读状态（未读计数）
CREATE INDEX IF NOT EXISTS idx_messages_match_sender_read
    ON messages(match_id, sender_id, is_read);

-- 消息查询：按match_id + 创建时间倒序（最新消息）
CREATE INDEX IF NOT EXISTS idx_messages_match_created
    ON messages(match_id, created_at DESC);

-- 动态点赞：用户是否点赞过某条动态
CREATE INDEX IF NOT EXISTS idx_moment_likes_user_moment
    ON moment_likes(user_id, moment_id);

-- 谁喜欢了我：按被滑动者 + 方向查询
CREATE INDEX IF NOT EXISTS idx_swipes_swiped_direction
    ON swipes(swiped_id, direction);

-- 匹配去重：检查是否已匹配
CREATE INDEX IF NOT EXISTS idx_swipes_swiper_swiped
    ON swipes(swiper_id, swiped_id);

-- 推荐排序：Boost优先 > VIP > 活跃度
CREATE INDEX IF NOT EXISTS idx_profiles_boost_until
    ON profiles(boost_until DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_profiles_last_active
    ON profiles(last_active DESC NULLS LAST);

-- 签到去重：每个用户每天只签一次
CREATE UNIQUE INDEX IF NOT EXISTS idx_checkin_user_date
    ON daily_check_ins(user_id, check_date);

-- 举报列表：按状态+时间排序
CREATE INDEX IF NOT EXISTS idx_reports_status_created
    ON reports(status, created_at ASC);

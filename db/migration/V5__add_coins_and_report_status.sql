-- 用户金币余额
ALTER TABLE profiles ADD COLUMN coins INT NOT NULL DEFAULT 100;

-- 举报处理状态：pending / resolved
ALTER TABLE reports ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'pending';
-- 处理动作：warn / ban / dismiss
ALTER TABLE reports ADD COLUMN resolution VARCHAR(20);

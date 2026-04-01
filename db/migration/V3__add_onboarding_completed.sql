-- 新增 onboarding 完成标记字段
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN DEFAULT FALSE;

-- 已有用户视为已完成 onboarding
UPDATE profiles SET onboarding_completed = TRUE WHERE onboarding_completed IS NULL;

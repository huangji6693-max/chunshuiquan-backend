# 春水圈进度记录（2026-04-01）

## 已完成
- FCM 推送集成
- MatchController 修复（UserDetails→String）
- router.dart initialLocation → /auth/login
- CORS 收紧（不再用通配 *）
- pubspec.yaml 重复依赖清理
- MatchController 全面换 DTO（MatchItemDto/MessageResponseDto）
- myId 空值 guard（chat_screen.dart）
- Matches 页进入自动刷新（ref.invalidate）
- Redis 接入 — JWT token黑名单（登出吊销 + refresh轮换吊销）
- 前端logout调用后端 /api/auth/logout API
- 多照片管理UI（ProfileScreen照片网格 + 删除 + 添加）
- Super Like按钮视觉升级（蓝色渐变脉冲动画 + 向上滑overlay动画）

## 待做（优先级顺序）
1. 确认 Railway 已配 CLOUDINARY_URL + CLARIFAI_API_KEY + REDIS_HOST/PORT/PASSWORD
2. 照片拖拽重排（需后端新增PUT /api/users/avatar/reorder API）
3. iOS 打包 + TestFlight
4. WebSocket实时消息（替代5秒轮询）
5. 视频通话（Agora已支持RTC）

## 技术栈基准（主人确认）
前端 Flutter（Web+App统一）/ 后端 Java SpringBoot SpringCloud / Redis / Kafka / TiDB / AWS-S3 / Nacos / Cron / 时间 Asia/Shanghai

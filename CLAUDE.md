# 春水圈进度记录（2026-04-01）

## 已完成
- FCM 推送集成
- MatchController 修复 + 全面换DTO
- CORS 收紧 + router initialLocation
- Redis 接入 — JWT token黑名单（登出吊销 + refresh轮换吊销）
- 前端logout调用后端 /api/auth/logout API
- 多照片管理UI（ProfileScreen照片网格 + 删除 + 添加）
- Super Like按钮视觉升级（蓝色渐变脉冲动画 + overlay动画）
- **WebSocket 实时消息**（STOMP协议 + JWT认证 + 实时推送 + 已读回执）
- **用户资料扩展**（身高/学历/星座/城市/吸烟/饮酒/经纬度）
- **在线状态**（Redis TTL 60s + heartbeat + WebSocket连接监听）
- **前端UI全面升级**（登录波浪动画/发现页多照片切换/匹配页渐变光环/聊天粉红气泡/资料页轮播）
- **前端WebSocket STOMP对接** + 心跳服务 + 在线状态显示
- **图片消息**（聊天中发图片，Cloudinary上传）

## 待做（优先级顺序）
1. 后端数据库迁移脚本（新增Profile字段的ALTER TABLE）
2. 照片拖拽重排（需后端PUT /api/users/avatar/reorder）
3. 匹配页lastMessage预览（后端MatchItemDto加lastMessage）
4. iOS 打包 + TestFlight
5. 视频通话（Agora已支持RTC）
6. 距离排序（基于经纬度的feed推荐）
7. 筛选功能（按年龄/距离/性别筛选推荐）

## 技术栈基准（主人确认）
前端 Flutter（Web+App统一）/ 后端 Java SpringBoot SpringCloud / Redis / Kafka / TiDB / AWS-S3 / Nacos / Cron / 时间 Asia/Shanghai

# 麻将计分助手

这是按《麻将计分小程序完整项目文档.md》落地的 MVP + V1 可扩展工程，包含：

- `mahjong-score-server`：Spring Boot 3 + MyBatis-Plus + MySQL + JWT + WebSocket 后端。
- `mahjong-score-miniprogram`：uni-app + Vue 3 + Pinia 微信小程序前端。

## 核心模型

- 座位分绑定在东、南、西、北座位上，新玩家加入空座位后继承座位当前分。
- 个人战绩只统计用户实际参与的轮次，不继承上一位玩家的历史分。
- 每轮提交的是“我输给谁多少分”的支出记录，系统用原始 `score_payment` 流水结算和重算。
- 历史局修改、删除、撤销后会重算房间后续座位分、个人战绩、每日统计和排行榜冗余字段。
- WebSocket 只做实时通知，最终状态以前端拉取房间快照为准。

## 后端启动

1. 创建 MySQL 8 数据库：

```sql
CREATE DATABASE mahjong_score DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行建表脚本：

```text
mahjong-score-server/src/main/resources/db/schema.sql
```

3. 修改配置：

```text
mahjong-score-server/src/main/resources/application.yml
```

生产环境需要配置 `app.jwt.secret`、数据库账号密码、微信小程序 `app-id/app-secret`。

4. 启动：

```bash
cd mahjong-score-server
mvn spring-boot:run
```

本地未配置微信 `app-id/app-secret` 时，后端会使用 dev 分支模拟 openid 和手机号绑定，便于联调。

## 前端启动

1. 安装依赖：

```bash
cd mahjong-score-miniprogram
npm install
```

2. 设置接口地址，可创建 `.env.local`：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

3. 运行微信小程序：

```bash
npm run dev:mp-weixin
```

然后用微信开发者工具打开生成的 `dist/dev/mp-weixin`。

## 主要接口

- 登录：`POST /api/auth/wechat-login`
- 绑定手机号：`POST /api/auth/bind-phone`
- 创建房间：`POST /api/rooms`
- 房间快照：`GET /api/rooms/{roomId}/snapshot`
- 提交本轮：`POST /api/rounds/{roundId}/submit`
- 房主代提交：`POST /api/rounds/{roundId}/owner-submit`
- 强制不输不赢：`POST /api/rounds/{roundId}/force-neutral`
- 当前房间排行：`GET /api/rooms/{roomId}/rank`
- 首页排行：`GET /api/rankings/home`
- 个人战绩：`GET /api/stats/me?range=ALL`

WebSocket 地址：

```text
/ws/room?token={jwt}&roomId={roomId}
```

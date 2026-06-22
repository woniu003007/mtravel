# mtravel

旅游地接业务数字化管理平台。

## 目录

- `backend/`：Spring Boot 后端服务。
- `frontend-preview/apps/web-antd/`：前端管理后台。
- `db/`：数据库建表 SQL 和表结构说明。
- `文档/`：需求、调研、原型和汇报材料。
- `scripts/`：文档和原型生成脚本。

## 本地启动

后端推荐使用根目录脚本启动：

```bash
./start-backend.sh
```

前端：

```bash
./scripts/dev-frontend-restart.sh
```

首次启动前，先复制 `.env.example` 为 `.env.local` 并填写本机数据库、Redis 密码。
详细说明见 `文档/本地启动说明.md`。

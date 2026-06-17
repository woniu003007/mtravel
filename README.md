# mtravel

旅游地接业务数字化管理平台。

## 目录

- `backend/`：Spring Boot 后端服务。
- `frontend-preview/apps/web-antd/`：前端管理后台。
- `db/`：数据库建表 SQL 和表结构说明。
- `文档/`：需求、调研、原型和汇报材料。
- `scripts/`：文档和原型生成脚本。

## 本地启动

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend-preview
pnpm -F @vben/web-antd run dev -- --host 0.0.0.0 --port 5666
```

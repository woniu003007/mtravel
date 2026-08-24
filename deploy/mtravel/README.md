# MTravel 生产部署说明

本项目部署在共享服务器上，当前采用 `systemd + 宝塔 Nginx` 方式，不复用、不重启现有 `yanxue-*`、Dify 等容器。

## 线上地址

- 域名：`http://mtravel.apexplore.cn/`
- 后端健康检查：`http://mtravel.apexplore.cn/actuator/health`

## 服务器目录

- 后端 jar：`/opt/mtravel/backend/app.jar`
- 前端 dist：`/opt/mtravel/frontend/dist`
- 后端环境变量：`/opt/mtravel/config/mtravel.env`
- 后端日志：`/opt/mtravel/logs/api.log`、`/opt/mtravel/logs/api-error.log`
- 备份目录：`/opt/mtravel/backups`

`mtravel.env` 包含数据库、Redis、JWT 等敏感配置，只保存在服务器，禁止提交到 Git。

共享 PostgreSQL 环境下建议在 `mtravel.env` 中显式限制连接池，避免 Spring Boot 默认每个实例常驻 10 个连接：

```bash
DB_POOL_MAX_SIZE=5
DB_POOL_MIN_IDLE=1
DB_POOL_IDLE_TIMEOUT_MS=300000
DB_POOL_KEEPALIVE_TIME_MS=300000
DB_POOL_MAX_LIFETIME_MS=1800000
DB_POOL_CONNECTION_TIMEOUT_MS=30000
DB_POOL_VALIDATION_TIMEOUT_MS=5000
DB_APPLICATION_NAME=mtravel-production
```

该配置使单个实例常驻 1 个空闲连接，并发时最多扩展到 5 个；额外连接空闲 5 分钟后回收。修改后需要重启 `mtravel-api.service` 才会生效。

## 运行方式

- 后端服务：`mtravel-api.service`
- 后端监听：`127.0.0.1:3002`
- 前端静态文件：由宝塔 Nginx 直接托管
- 域名配置：`/www/server/panel/vhost/nginx/mtravel.apexplore.cn.conf`
- 数据库：PostgreSQL 容器 `postgres-yanxue` 内的独立数据库 `mtravel`
- Redis：`redis-yanxue`，当前使用 database `1`

## 常用命令

```bash
systemctl status mtravel-api
systemctl restart mtravel-api
tail -f /opt/mtravel/logs/api.log
tail -f /opt/mtravel/logs/api-error.log
```

Nginx 配置检查和重载：

```bash
/www/server/nginx/sbin/nginx -t
/www/server/nginx/sbin/nginx -s reload
```

## 部署验证

```bash
curl -s http://127.0.0.1:3002/actuator/health
curl -s -H 'Host: mtravel.apexplore.cn' http://127.0.0.1/actuator/health
curl -s http://mtravel.apexplore.cn/actuator/health
curl -s -X POST http://mtravel.apexplore.cn/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"<后台账号>","password":"<后台密码>"}'
```

## 注意事项

- 不要占用 `8080`，该端口已有 Dify 服务。
- 不要修改 `apexplore.cn`、`dify.apexplore.cn` 等既有站点配置。
- 不要把 MTravel 表建到 `yanxue` 数据库，当前项目使用独立数据库 `mtravel`。
- 更新前端生产包时，确认 `frontend-preview/apps/web-antd/.env.production` 中 `VITE_GLOB_API_URL=/api`。

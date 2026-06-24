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

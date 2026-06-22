# 车型报价与微信群询价 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在产品团队安排的用车信息里接入路书公里、车型报价规则测算和微信群车队询价记录。

**Architecture:** 车队报价规则作为计调侧基础规则维护；产品团队安排保存报价测算快照和询价记录子表，避免后续规则变化影响历史产品模板。前端在产品团队安排页用已有路书数据同步公里数，并允许记录多个车队报价、选定报价后回填价格信息。

**Tech Stack:** Spring Boot 3.5、Java 21、MyBatis-Plus、PostgreSQL、Vue/Vben Admin、Ant Design Vue、Vitest。

---

### Task 1: 后端 TDD 与数据库结构

**Files:**
- Create: `backend/src/test/java/com/mtravel/platform/dispatch/vehiclequote/service/VehicleQuoteRuleServiceTest.java`
- Create: `backend/src/test/java/com/mtravel/platform/dispatch/vehiclequote/controller/VehicleQuoteRuleControllerMappingTest.java`
- Modify: `db/011_sales_product_schema.sql`
- Modify: `db/数据库设计-销售产品模板说明.md`
- Modify: `db/README.md`

- [ ] 写失败测试：报价规则按基础公里、超出公里、浮动比例、最低价计算。
- [ ] 写失败测试：车型报价规则接口路径固定为 `/dispatch/vehicle-quote-rules`。
- [ ] 本地 SQL 增加 `vehicle_quote_rules`、`sales_product_vehicle_quote_snapshots`、`sales_product_vehicle_inquiries`。
- [ ] 本地表说明文档补充三张表字段、边界和快照原则。

### Task 2: 后端报价规则接口

**Files:**
- Create package: `backend/src/main/java/com/mtravel/platform/dispatch/vehiclequote/`
- Create DTO/entity/enums/mapper/service/controller for quote rules and calculation.

- [ ] 实现 `VehicleQuoteRuleService.calculate()`，返回规则快照和计算金额。
- [ ] 实现分页、启用列表、新增、修改、删除、测算接口。
- [ ] 状态和计价字段做基础校验；删除使用软删除。

### Task 3: 产品团队安排保存快照与询价记录

**Files:**
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/service/SalesProductService.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/dto/SalesProductArrangementItemRequest.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/dto/SalesProductArrangementItemResponse.java`
- Create: sales product vehicle snapshot/inquiry DTO/entity/mapper.
- Modify: `backend/src/test/java/com/mtravel/platform/sales/product/service/SalesProductServiceTest.java`

- [ ] 扩展请求/响应字段：`vehicleQuoteSnapshot`、`vehicleInquiryRecords`。
- [ ] 保存安排项后保存快照与询价子表。
- [ ] 详情读取时按安排项组装快照和询价记录。
- [ ] 产品更新/删除时软删除对应快照和询价记录。

### Task 4: 前端 API 与报价规则页面

**Files:**
- Create: `frontend-preview/apps/web-antd/src/api/dispatch/vehicle-quote.ts`
- Create: `frontend-preview/apps/web-antd/src/views/dispatch/vehicle-quote/index.vue`
- Modify: `frontend-preview/apps/web-antd/src/router/routes/modules/dispatch.ts`
- Modify: `backend/src/main/java/com/mtravel/platform/menu/MenuController.java`

- [ ] 增加车型报价规则页面：查询、规则列表、新增/修改、启停状态。
- [ ] 增加测算区域：车型、公里数、地区，调用后端测算接口。
- [ ] 菜单挂到 `计调操作 / 用车报价测算`。

### Task 5: 产品团队安排用车弹窗联动

**Files:**
- Modify: `frontend-preview/apps/web-antd/src/api/sales/product.ts`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/product-form-utils.ts`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/product-form-utils.test.ts`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/form.vue`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/team-arrangement.vue`

- [ ] 路书抽屉支持在同一抽屉切换第几天。
- [ ] 用车弹窗增加“路书公里”“报价测算”“车队询价记录”。
- [ ] 同步选中日期范围内的路书公里和预计车程。
- [ ] 测算结果可应用到价格信息。
- [ ] 询价记录支持多车队报价、选定报价、回填价格信息。

### Task 6: 验证与进度台账

**Files:**
- Modify: `文档/开发进度与模块记录.md`

- [ ] 运行后端目标测试和 `mvn -q test`。
- [ ] 运行前端 Vitest 和 typecheck。
- [ ] 如数据库环境变量可用，应用 DDL 并运行 schema consistency check。
- [ ] 更新开发进度台账，记录接口、数据库表、页面、验证结果和待确认问题。

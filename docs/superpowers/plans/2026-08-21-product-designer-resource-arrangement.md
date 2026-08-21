# 产品设计工作台资源编排重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将产品设计工作台重构为“产品级全程用车 + 每日住宿、用餐、日程资源、地接服务”四区编排，并让地图、供应商成本、报价和 Word 使用同一份可追溯数据。

**Architecture:** 每日资源继续使用 `sales_product_day_resources`，通过明确的 `arrangement_role` 分类和快照字段表达酒店、餐次、日程、地接；全程用车新增设计草稿专用快照表，避免与只服务正式产品的团队安排数据耦合。后端负责早餐来源、供应商有效性、成本和报价的唯一计算；前端只负责选择资源、展示结果和轻量确认。

**Tech Stack:** Spring Boot / Java 21 / MyBatis-Plus / PostgreSQL / Vue 3 / TypeScript / Ant Design Vue / Vben Admin / AMap 2.x / Vitest / JUnit 5。

**Spec:** `docs/superpowers/specs/2026-08-21-product-designer-resource-arrangement-design.md`

## Global Constraints

- 本次只改产品设计工作台；不修改采购资源主档录入页、交通编排和正式团队安排流程。
- 全程用车使用产品设计专用表/API，不复用只接受 `product_scope='template'` 的正式团队安排保存服务。
- 所有成本、供应商有效性、成人报价成本在后端计算；前端不得用 `reduce` 作为正式金额结果。
- 资源加入时冻结采购关系、供应商和价格快照；资源主档后续变化不得反向改写历史产品。
- 餐厅无上下文时只能弹“早餐 / 中餐 / 晚餐”小选择器；不得猜测中餐。
- 每日酒店可多家；地接可多次；中餐和晚餐本期各只允许一家餐厅。
- Dn 早餐只引用 Dn-1 标记含早的酒店；外部早餐和酒店含早互斥；D1 不承接行前夜酒店早餐。
- 用车不出现在每日地图、资源地图候选或日程资源中；成本只计入产品级一次。
- 数据库变更执行前必须再次确认目标为当前 `mtravel` 数据库；不在本计划执行阶段自动写入远程数据库。
- 不执行 Git 操作、提交或推送，除非用户另行明确授权。
- 后端改动上线时必须重新编译、重启 Java 服务并检查 `/actuator/health`；前端热更新不能代替后端重启。

---

## File map and responsibilities

| 文件 | 改动职责 |
| --- | --- |
| `db/052_sales_product_designer_multi_hotel_accommodation.sql` | 修复已执行旧版索引，把住宿移出餐次唯一约束 |
| `db/053_sales_product_designer_resource_arrangement_v2.sql` | 日资源角色/快照升级、地接重复规则、全程用车表、索引和注释 |
| `db/037_sales_product_designer_schema.sql` | 新装环境的完整基础结构与最终约束 |
| `db/README.md`、`db/数据库设计-销售产品设计工作台说明.md`、`db/数据库设计-销售产品模板说明.md` | 迁移顺序、表结构、价格快照和业务口径 |
| `backend/.../designer/enums/ProductDesignerArrangementRole.java` | 日资源角色和值域校验的单一来源 |
| `backend/.../designer/entity/SalesProductDayResourceEntity.java` | 采购关系/报价模式快照字段 |
| `backend/.../designer/entity/SalesProductDesignerVehicleArrangementEntity.java` | 产品级全程用车快照实体 |
| `backend/.../designer/mapper/SalesProductDesignerVehicleArrangementMapper.java` | 全程用车持久化 |
| `backend/.../designer/dto/*.java` | 早餐计划、供应商切换、全程用车、详情/成本拆分契约 |
| `backend/.../designer/service/SalesProductDesignerService.java` | 角色、早餐、供应商、全程用车、成本、详情和草稿清理事务 |
| `backend/.../designer/service/SalesProductDesignerDocumentService.java` | 多酒店、早餐、用车的 Word 文字输出 |
| `backend/.../designer/controller/SalesProductDesignerController.java` | 新增窄粒度保存/删除/排序接口 |
| `backend/.../purchase/relation/mapper/PurchaseRelationMapper.java` | 供应商自身启用状态与可计算报价过滤 |
| `backend/.../sales/product/service/SalesProductService.java` | 正式产品删除时同步软删设计工作台全程用车快照 |
| `frontend-preview/.../api/sales/product-designer.ts` | 前端类型和新接口封装 |
| `frontend-preview/.../views/sales/product/designer.vue` | 页面编排、资源选择命令、数据加载和原有 Word 功能协调 |
| `frontend-preview/.../components/ProductDesignerDayArrangementPanel.vue` | 每天四区的紧凑 UI 和已安排资源操作 |
| `frontend-preview/.../components/ProductDesignerVehicleArrangementPanel.vue` | 产品级全程用车区 |
| `frontend-preview/.../components/ProductResourceSupplierPicker.vue` | 紧凑的有效供应商 Popover |
| `frontend-preview/.../components/ProductResourceMapWorkspace.vue` | 全屏地图的候选列表和统一点击行为 |
| `frontend-preview/.../components/product-resource-map-marker.ts`、`.css` | 紧凑/全屏共用的热气球标记 HTML、状态和样式 |
| `frontend-preview/.../views/sales/product/product-designer-*.test.ts` | 前端交互契约和组件接线回归测试 |
| `backend/.../SalesProductDesignerServiceTest.java`、`SalesProductDesignerDocumentServiceTest.java`、`SalesProductServiceTest.java` | 后端规则、文档、清理和成本回归测试 |

---

### Task 1: Lock the business rules with regression tests before schema or UI work

**Files:**
- Modify: `backend/src/test/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerServiceTest.java`
- Modify: `backend/src/test/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerDocumentServiceTest.java`
- Modify: `backend/src/test/java/com/mtravel/platform/sales/product/service/SalesProductServiceTest.java`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/product-designer-itinerary.test.ts`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/product-designer-flow.test.ts`
- Create: `frontend-preview/apps/web-antd/src/views/sales/product/product-designer-arrangement-utils.test.ts`

**Interfaces:**
- Consumes: current `ProductDesignerDayResourceSaveRequest`, current day-resource save/reorder API.
- Produces: executable acceptance names that later tasks must turn green: `ground_service`, `BreakfastPlan`, `supplierRelationId`, `vehicleArrangements`, and role-scoped ordering.

- [ ] **Step 1: Add failing service tests for the exact day-resource matrix**

Add tests with these names and assertions:

```java
@Test void saveDayResourceShouldAllowTwoAccommodationResourcesOnTheSameDay() {}
@Test void saveDayResourceShouldAllowRepeatedGroundServiceResourceOnTheSameDay() {}
@Test void saveDayResourceShouldRejectVehicleAsADayResource() {}
@Test void saveDayResourceShouldRejectExternalBreakfastWhenPreviousNightHasBreakfastHotel() {}
@Test void saveDayResourceShouldReplacePreviousNightBreakfastWhenReplaceBreakfastSourceIsTrue() {}
@Test void reorderDayResourcesShouldOnlyReorderTheRequestedArrangementRole() {}
@Test void detailShouldExposeHotelBreakfastSourcesAndSeparateCostBreakdown() {}
```

Use Mockito captors to prove `ground_service` inserts twice, that `itinerary` order is untouched by a ground-service reorder, and that no frontend-only computation is needed for `BreakfastPlan`.

- [ ] **Step 2: Add failing supplier and vehicle tests**

Add tests that use two relations for one supplier and one disabled/non-priced relation:

```java
@Test void saveDayResourceShouldSnapshotExactSupplierRelationAndPriceMode() {}
@Test void changeDayResourceSupplierShouldRejectDisabledOrUnpricedRelation() {}
@Test void productCostShouldIncludeDayResourcesAndDesignerVehicleArrangements() {}
@Test void deleteDraftShouldSoftDeleteDesignerVehicleArrangements() {}
@Test void deleteFormalProductShouldSoftDeleteDesignerVehicleArrangements() {}
```

Assert the response returns `supplierRelationId`, `priceMode`, `dayResourceCostAmount`, `vehicleCostAmount`, and a total equal to their sum.

- [ ] **Step 3: Add Word-output regressions**

Add tests for this exact text behavior:

```java
@Test void wordShouldJoinMultipleHotelsInAccommodationColumn() {}
@Test void wordShouldListAllPreviousNightBreakfastHotelsWhenNoExternalBreakfastExists() {}
@Test void wordShouldPreferExternalBreakfastRestaurantOverHotelBreakfast() {}
@Test void wordShouldNotMixGroundServiceIntoCustomerItineraryRoute() {}
@Test void wordShouldAddVehicleSummaryWithoutInternalCost() {}
```

Use D1 hotel A/B, D2 restaurant breakfast, a ground service and a vehicle fixture so each branch has a stable expected paragraph/table-cell value.

- [ ] **Step 4: Add frontend RED tests around the final interaction contract**

Create a small pure helper test for resource target resolution:

```ts
expect(resolveArrangementTarget('hotel')).toEqual({ role: 'accommodation' });
expect(resolveArrangementTarget('ground_agent')).toEqual({ role: 'ground_service', allowRepeat: true });
expect(resolveArrangementTarget('restaurant')).toEqual({ requiresMealSelection: true });
expect(resolveArrangementTarget('vehicle')).toEqual({ unsupportedInDayMap: true });
```

Update the two existing source-level tests to assert the four section headings, no `加入 D` button in `ProductResourceMapWorkspace.vue`, `ProductResourceSupplierPicker`, `mealPlan`, `vehicleArrangements`, and shared `product-resource-map-marker` use.

- [ ] **Step 5: Run the RED tests and record the intended failures**

Run:

```bash
cd backend
mvn -q -Dtest=SalesProductDesignerServiceTest,SalesProductDesignerDocumentServiceTest,SalesProductServiceTest test

cd ../frontend-preview
pnpm exec vitest run --dom \
  apps/web-antd/src/views/sales/product/product-designer-itinerary.test.ts \
  apps/web-antd/src/views/sales/product/product-designer-flow.test.ts \
  apps/web-antd/src/views/sales/product/product-designer-arrangement-utils.test.ts
```

Expected: new tests fail or do not compile because the new role/DTO/API/component contracts do not yet exist; unrelated existing tests continue to pass.

---

### Task 2: Repair multi-hotel production drift and define the V2 database schema

**Files:**
- Modify: `db/052_sales_product_designer_multi_hotel_accommodation.sql` only if its predicate differs from the required final predicate below
- Create: `db/053_sales_product_designer_resource_arrangement_v2.sql`
- Modify: `db/037_sales_product_designer_schema.sql`
- Modify: `db/README.md`
- Modify: `db/数据库设计-销售产品设计工作台说明.md`
- Modify: `db/数据库设计-销售产品模板说明.md`

**Interfaces:**
- Consumes: existing `sales_product_day_resources`, `sales_products`, `purchase_resources`, `purchase_relations`, `suppliers`.
- Produces: database support for `ground_service`, relation/price snapshots, role-scoped uniqueness, and `sales_product_designer_vehicle_arrangements`.

- [ ] **Step 1: Preserve and verify the 052 hotfix**

Keep `052` limited to the live drift it was created for:

```sql
DROP INDEX IF EXISTS uk_sales_product_day_resources_day_meal_role_active;
CREATE UNIQUE INDEX uk_sales_product_day_resources_day_meal_role_active
  ON sales_product_day_resources (tenant_id, product_id, day_no, arrangement_role)
  WHERE is_deleted = false
    AND arrangement_role IN ('breakfast', 'lunch', 'dinner');
```

Do not rerun `051`: live `mtravel` has an earlier 051 result whose predicate includes `accommodation`, while the current source text was later normalized. Record this source/live drift in `db/README.md`.

- [ ] **Step 2: Write the 053 migration with defensive role conversion**

In one transaction, add the new day-resource snapshots and move ground agents:

```sql
ALTER TABLE sales_product_day_resources
  ADD COLUMN IF NOT EXISTS supplier_relation_id_snapshot bigint,
  ADD COLUMN IF NOT EXISTS price_mode_snapshot varchar(20);

UPDATE sales_product_day_resources
SET arrangement_role = 'ground_service'
WHERE is_deleted = false
  AND resource_type_snapshot = 'ground_agent'
  AND arrangement_role = 'itinerary';
```

Drop/recreate `chk_sales_product_day_resources_arrangement_role` so hotel, restaurant, ground agent, scenic/shopping/other, and legacy vehicle/traffic have the exact matrix documented in the spec. The new service will reject new vehicle/traffic day rows; the schema keeps legacy rows readable rather than deleting historic product data.

- [ ] **Step 3: Make duplicate and order constraints match the business rules**

Replace the current all-role duplicate index with:

```sql
DROP INDEX IF EXISTS uk_sales_product_day_resources_day_resource_role_active;
CREATE UNIQUE INDEX uk_sales_product_day_resources_day_resource_role_active
  ON sales_product_day_resources (tenant_id, product_id, day_no, resource_id, arrangement_role)
  WHERE is_deleted = false AND arrangement_role <> 'ground_service';

CREATE INDEX IF NOT EXISTS idx_sales_product_day_resources_product_day_role_sort
  ON sales_product_day_resources (tenant_id, is_deleted, product_id, day_no, arrangement_role, sort_order, id);
```

Keep the 052 meal-role uniqueness index; do not add a database cross-day breakfast constraint because cross-day mutation and replacement must be handled transactionally by the service.

- [ ] **Step 4: Create the product-design full-journey vehicle table**

Create `sales_product_designer_vehicle_arrangements` with: tenant/product IDs, optional `resource_id`, resource/supplier/relation/price-mode snapshots, `vehicle_type_snapshot`, `start_day_no`, `end_day_no`, quantity/unit-price/cost snapshots, `sort_order`, `remark`, standard soft-delete/audit columns, product and resource/supplier FKs, non-negative money/quantity checks, day-range check, `updated_at` trigger, and indexes:

```sql
CREATE INDEX idx_sales_product_designer_vehicle_product
  ON sales_product_designer_vehicle_arrangements
  (tenant_id, is_deleted, product_id, sort_order, id);
```

The table must allow 0-N vehicle rows per product. It must not have `day_no`, map coordinates or a unique constraint that collapses multiple vehicle segments.

- [ ] **Step 5: Synchronize the new-install schema and documents**

Copy the final V2 constraints, columns, indexes, triggers and `COMMENT ON` statements into `037_sales_product_designer_schema.sql`. Update both database design documents with the role matrix, breakfast invariant, supplier relation snapshot, vehicle table and migration order `037 → 051 → 052 (for drifted installs) → 053`.

- [ ] **Step 6: Add migration verification SQL, but do not execute remotely yet**

Document these exact queries in `db/README.md`:

```sql
SELECT indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname = 'uk_sales_product_day_resources_day_meal_role_active';

SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'sales_product_day_resources'::regclass
  AND conname LIKE 'chk_sales_product_day_resources%';

SELECT column_name
FROM information_schema.columns
WHERE table_name = 'sales_product_designer_vehicle_arrangements'
ORDER BY ordinal_position;
```

Expected: the meal index predicate lists only `breakfast/lunch/dinner`; V2 role and snapshot columns exist; no active duplicate is found before creating the non-ground unique index.

---

### Task 3: Implement role, breakfast and role-scoped order behavior in the backend

**Files:**
- Create: `backend/src/main/java/com/mtravel/platform/sales/product/designer/enums/ProductDesignerArrangementRole.java`
- Create: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerBreakfastPlanResponse.java`
- Create: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerBreakfastHotelResponse.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerDayPlanResponse.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerDayResourceSaveRequest.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerDayResourceReorderRequest.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerService.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/controller/SalesProductDesignerController.java`
- Test: `backend/src/test/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerServiceTest.java`

**Interfaces:**
- Consumes: V2 database role constraint from Task 2.
- Produces:

```java
record ProductDesignerBreakfastPlanResponse(
    String source, // hotel, restaurant, none
    List<ProductDesignerBreakfastHotelResponse> hotelSources,
    ProductDesignerDayResourceResponse restaurant
) {}

record ProductDesignerDayResourceReorderRequest(
    Long productId, Integer dayNo, String arrangementRole, List<Long> dayResourceIds
) {}
```

- [ ] **Step 1: Centralize legal role values**

Create an enum with `ACCOMMODATION`, `BREAKFAST`, `LUNCH`, `DINNER`, `ITINERARY`, `GROUND_SERVICE`, and `LEGACY_UNASSIGNED`. Give it `value()`, `isMeal()`, `isIndependentlySortable()`, and `fromValue(String)` methods. Replace the service’s scattered `ARRANGEMENT_*` strings with enum values where business validation occurs.

- [ ] **Step 2: Make save routing strict for new resources**

In `normalizeArrangementRole`, enforce:

```java
hotel        -> accommodation
restaurant   -> breakfast | lunch | dinner
ground_agent -> ground_service
scenic/shopping/other -> itinerary
vehicle/traffic -> throw new BizException("用车和交通不能安排到某一天，请使用产品级安排")
```

`unassigned` stays readable for historic rows but cannot be passed by a new request. `ground_service` must bypass same-resource duplicate rejection; all other roles retain same-day duplicate protection.

- [ ] **Step 3: Make `sort_order` independent per visual section**

Change `nextSortOrder` to receive a role and query only that role. Change `reorderDayResources` to require `arrangementRole`, reject IDs outside the requested role, and update only that role’s IDs. Allow reordering for `accommodation`, `itinerary`, and `ground_service`; reject meal roles because their position is fixed.

- [ ] **Step 4: Add transactional breakfast conflict resolution**

Add `Boolean replaceBreakfastSource` to `ProductDesignerDayResourceSaveRequest`. In `saveDayResource`:

```java
if (savingExternalBreakfast) {
    assertOrClearPreviousNightBreakfastHotels(productId, dayNo, replaceBreakfastSource, operator);
}
if (enablingHotelBreakfast) {
    assertOrClearNextDayBreakfastRestaurant(productId, dayNo + 1, replaceBreakfastSource, operator);
}
```

When the flag is false, reject with a clear conflict message. When it is true, soft-delete the conflicting breakfast restaurant or clear the relevant hotel breakfast flags in the same `@Transactional` method. Never issue two client-side requests to enact the switch.

- [ ] **Step 5: Return an authoritative meal plan**

In `dayPlan`, resolve breakfast from Dn-1 hotels and the Dn breakfast restaurant. Return `source='restaurant'` when a restaurant exists, otherwise `hotel` only when one or more previous-night hotels are marked, otherwise `none`. Include the actual hotel resource IDs/names so Vue does not reconstruct the rule.

Keep legacy `breakfastIncluded/lunchIncluded/dinnerIncluded` response fields temporarily for compatibility, but have the designer page stop reading them.

- [ ] **Step 6: Run the targeted service tests to green**

Run:

```bash
cd backend
mvn -q -Dtest=SalesProductDesignerServiceTest test
```

Expected: all old tests and all Task 1 day-resource/breakfast/order tests pass.

---

### Task 4: Harden supplier candidates and implement a safe single-resource supplier switch

**Files:**
- Modify: `backend/src/main/java/com/mtravel/platform/purchase/relation/mapper/PurchaseRelationMapper.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/entity/SalesProductDayResourceEntity.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerDayResourceSaveRequest.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerDayResourceResponse.java`
- Create: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerDayResourceSupplierSaveRequest.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerService.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/controller/SalesProductDesignerController.java`
- Test: `backend/src/test/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerServiceTest.java`

**Interfaces:**
- Consumes: active supplier/resource relations and price rows from `PurchaseRelationMapper`.
- Produces:

```java
record ProductDesignerDayResourceSupplierSaveRequest(
    Long productId, Long dayResourceId, Long supplierRelationId
) {}

POST /sales/product/designer/day-resource/supplier
```

- [ ] **Step 1: Filter candidate supplier relations correctly**

Update the joined candidate SQL to require all of: resource relation active/not-deleted, supplier active/not-deleted, and a calculable price. A unified-price relation needs a valid unified amount; a classified-price relation needs at least one active price row. Return `relationId`, `supplierId`, `supplierName`, `isDefault`, `priceMode`, and `referenceUnitPrice`.

- [ ] **Step 2: Add exact relation and price snapshots to daily resources**

Map `supplier_relation_id_snapshot` and `price_mode_snapshot` in `SalesProductDayResourceEntity`. Add the same fields to `ProductDesignerDayResourceResponse`. Prefer `supplierRelationId` when it is supplied; keep `supplierId` as old-client compatibility input only.

- [ ] **Step 3: Refactor supplier resolution into one reusable method**

Implement a single `resolveSupplierQuote(tenantId, resource, supplierRelationId, supplierId)` path used by initial add and supplier switch. It must:

1. Select the default valid relation when no supplier input exists.
2. Return a zero-price `pending` result only when no valid relation exists and no explicit relation was requested.
3. Reject an explicitly requested missing, disabled, unrelated or unpriced relation.
4. Return the exact relation, quote mode and calculated unit price.

- [ ] **Step 4: Implement the narrow supplier-switch endpoint**

`changeDayResourceSupplier` loads the existing day resource under tenant/product scope, calls the shared resolver, updates relation/supplier/unit-price/cost snapshots, and delegates compatible selected optional-item re-pricing to `SalesProductDesignerOptionalItemService`. It returns the refreshed `ProductDesignerDayResourceResponse`; it must not overwrite introduction/image/remark fields.

- [ ] **Step 5: Make downstream quote data fresh**

After any add, delete, supplier switch or vehicle save/delete, recompute the current draft adult quote’s cost using the same backend `productCost`. Preserve the externally entered sale price and recompute its derived markup; never trust a front-end quote total.

- [ ] **Step 6: Run supplier and quote tests**

Run:

```bash
cd backend
mvn -q -Dtest=SalesProductDesignerServiceTest test
```

Expected: default valid relation is snapshotted, invalid supplier candidates are excluded/rejected, no-candidate resources remain arrangable as pending, and cost/quote totals refresh server-side.

---

### Task 5: Add product-design full-journey vehicle persistence and cost integration

**Files:**
- Create: `backend/src/main/java/com/mtravel/platform/sales/product/designer/entity/SalesProductDesignerVehicleArrangementEntity.java`
- Create: `backend/src/main/java/com/mtravel/platform/sales/product/designer/mapper/SalesProductDesignerVehicleArrangementMapper.java`
- Create: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerVehicleArrangementSaveRequest.java`
- Create: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerVehicleArrangementDeleteRequest.java`
- Create: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerVehicleArrangementResponse.java`
- Create: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerVehicleArrangementReorderRequest.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerDetailResponse.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerService.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/controller/SalesProductDesignerController.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/service/SalesProductService.java`
- Test: `backend/src/test/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerServiceTest.java`
- Test: `backend/src/test/java/com/mtravel/platform/sales/product/service/SalesProductServiceTest.java`

**Interfaces:**
- Consumes: V2 vehicle table, `PurchaseResourceEntity` where `resource_type='vehicle'`, shared supplier quote resolver.
- Produces:

```java
record ProductDesignerVehicleArrangementSaveRequest(
    Long id, Long productId, Long resourceId, Long supplierRelationId,
    Integer startDayNo, Integer endDayNo, BigDecimal quantity,
    String vehicleType, Integer sortOrder, String remark
) {}

record ProductDesignerDetailResponse(
    /* existing fields */, BigDecimal dayResourceCostAmount,
    BigDecimal vehicleCostAmount,
    List<ProductDesignerVehicleArrangementResponse> vehicleArrangements
) {}
```

- [ ] **Step 1: Map and validate the vehicle entity**

Implement all V2 table fields with `TenantSoftDeleteEntity`. Validate `startDayNo/endDayNo` against the product travel days when supplied, `endDayNo >= startDayNo`, positive sort position, and non-negative quantity/cost fields.

- [ ] **Step 2: Add narrow vehicle APIs**

Add these controller routes with `@OperationLog` and normal tenant/authentication handling:

```text
GET  /sales/product/designer/vehicle-resources
POST /sales/product/designer/vehicle-arrangement/save
POST /sales/product/designer/vehicle-arrangement/delete
POST /sales/product/designer/vehicle-arrangement/reorder
```

`vehicle-resources` is paginated, filters only active `vehicle` resources, and never returns map fields for the day-map workflow.

- [ ] **Step 3: Snapshot a vehicle’s supplier and cost using the shared resolver**

Vehicle save copies resource name, resource ID, relation ID, supplier ID/name, price mode, quantity, unit price and cost into the vehicle arrangement. A missing valid supplier is represented as pending/zero cost rather than blocking product design; an explicitly chosen invalid relation is rejected.

- [ ] **Step 4: Include vehicles in all product cost paths**

Refactor `productCost` into a calculation with named components:

```java
BigDecimal dayResourceCost = sumActiveDayResourceCosts(tenantId, productId);
BigDecimal vehicleCost = sumActiveDesignerVehicleCosts(tenantId, productId);
BigDecimal total = dayResourceCost.add(vehicleCost);
```

Use this calculation for `detail`, adult quote save/reprice and all mutation side effects. Do not duplicate vehicle cost into a `DayPlan`.

- [ ] **Step 5: Clean up vehicle rows on draft/formal-product deletion**

Extend product-design draft deletion to soft-delete its vehicles. Add a targeted soft-delete branch to `SalesProductService` for final product deletion so orphaned product-design vehicle rows cannot remain after the product is deleted.

- [ ] **Step 6: Run vehicle and product deletion tests**

Run:

```bash
cd backend
mvn -q -Dtest=SalesProductDesignerServiceTest,SalesProductServiceTest test
```

Expected: a product can hold multiple vehicle rows, none appear in day data, total cost includes each exactly once, and both deletion paths clean them up.

---

### Task 6: Align product detail, adult quote and Word generation with the final model

**Files:**
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/dto/ProductDesignerDetailResponse.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerService.java`
- Modify: `backend/src/main/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerDocumentService.java`
- Modify: `backend/src/test/java/com/mtravel/platform/sales/product/designer/service/SalesProductDesignerDocumentServiceTest.java`

**Interfaces:**
- Consumes: day role rows, `BreakfastPlan`, product-design vehicle arrangements and computed cost breakdown.
- Produces: one server-side view of day text and cost used by workbench detail, adult quote and Word.

- [ ] **Step 1: Batch-load resources by role and avoid N+1 lookups**

Use one active day-resource query, one active vehicle query, then group in memory by day/role. Do not request resource/supplier records inside per-day loops. Keep `dayResourceCostAmount`, `vehicleCostAmount`, and `totalCostAmount` money-scaled on the server.

- [ ] **Step 2: Rework Word day rows**

Replace the single-hotel helper with a list-based implementation:

```java
String accommodationText = accommodationResources(day).stream()
    .map(SalesProductDayResourceEntity::getResourceNameSnapshot)
    .collect(joining("、"));
```

For breakfast, first use the Dn breakfast restaurant; otherwise join all Dn-1 hotels with `hotel_breakfast_included=true`; otherwise output the template’s empty/“未安排” text. Use only `itinerary` role rows to build the customer route and never insert `ground_service` rows.

- [ ] **Step 3: Add non-price vehicle wording to the product Word**

Build an optional text such as `全程用车：39座旅游大巴（D1–D4）` from product vehicle snapshots. Put it in the existing product overview/remarks-compatible section and leave it absent when no vehicle is configured. Do not expose supplier or internal cost in customer-facing Word output.

- [ ] **Step 4: Ensure adult quote regenerates from the full total**

Use the shared cost calculation in adult quote save/generation. Tests must prove that changing one vehicle price changes adult cost, and that no front-end-provided total affects the result.

- [ ] **Step 5: Run document and quote tests**

Run:

```bash
cd backend
mvn -q -Dtest=SalesProductDesignerServiceTest,SalesProductDesignerDocumentServiceTest test
```

Expected: multi-hotel/meal/vehicle text and full cost calculation pass all old and new examples.

---

### Task 7: Create typed frontend APIs and focused reusable arrangement components

**Files:**
- Modify: `frontend-preview/apps/web-antd/src/api/sales/product-designer.ts`
- Create: `frontend-preview/apps/web-antd/src/views/sales/product/components/product-designer-arrangement-utils.ts`
- Create: `frontend-preview/apps/web-antd/src/views/sales/product/components/ProductDesignerDayArrangementPanel.vue`
- Create: `frontend-preview/apps/web-antd/src/views/sales/product/components/ProductDesignerVehicleArrangementPanel.vue`
- Create: `frontend-preview/apps/web-antd/src/views/sales/product/components/ProductResourceSupplierPicker.vue`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/designer.vue`
- Test: `frontend-preview/apps/web-antd/src/views/sales/product/product-designer-arrangement-utils.test.ts`
- Test: `frontend-preview/apps/web-antd/src/views/sales/product/product-designer-itinerary.test.ts`

**Interfaces:**
- Consumes: backend detail with `mealPlan`, cost breakdown and vehicle arrays; narrow save/reorder/supplier/vehicle APIs.
- Produces: typed events from child panels, for example:

```ts
type ArrangementTarget =
  | { role: 'accommodation' | 'itinerary' | 'ground_service'; allowRepeat?: boolean }
  | { requiresMealSelection: true }
  | { unsupportedInDayMap: true };

emit('select-meal-resource', role: 'breakfast' | 'lunch' | 'dinner')
emit('remove-day-resource', row: SalesProductDesignerApi.DayResource)
emit('change-supplier', row: SalesProductDesignerApi.DayResource, relationId: number)
```

- [ ] **Step 1: Add TypeScript types and request wrappers**

Extend `ArrangementRole` with `ground_service`; add `BreakfastPlan`, `BreakfastHotel`, `CostBreakdown`, `VehicleArrangement`, `DayResourceSupplierSaveRequest`, and vehicle save/delete/reorder request types. Add wrappers for the exact routes in Tasks 4–5. Change day-resource reorder to require `arrangementRole`.

- [ ] **Step 2: Implement pure resource-target resolution**

Create `product-designer-arrangement-utils.ts` and export `resolveArrangementTarget`. It contains no HTTP or DOM work and returns the exact matrix from the spec. Its tests must cover hotel, restaurant with/without meal context, scenic, shopping, other, ground agent, vehicle and traffic.

- [ ] **Step 3: Build the compact supplier picker**

`ProductResourceSupplierPicker.vue` accepts one arranged row and cached resource supplier candidates. Use Ant Design Vue `Popover + List`, not a large Modal. Each candidate row shows `供应商名 · ¥参考单价`, identifies the default one, disables the control for `not_required`, and displays a compact empty state `暂无有效报价，请后续补充`.

The component emits only a `supplier-relation-change` event. It does not calculate money, mutate the row locally, or request global suppliers.

- [ ] **Step 4: Build the four-section day panel**

`ProductDesignerDayArrangementPanel.vue` renders visible empty states and only these action targets:

```text
当天住宿：酒店行 + 含次日早餐 Checkbox + 更换供应商 + 删除
当天用餐：早餐 / 中餐 / 晚餐固定槽位 + 选择/更换/清除
当日行程：景区、购物、其它的可排序行
地接服务：可重复、可排序行
```

Use `size="small"`, link-style row actions, ellipsis/tooltip for long names, and the standard modal/button visual language. A resource row only emits intent upward; `designer.vue` owns requests and refresh.

- [ ] **Step 5: Build the product-level vehicle panel**

`ProductDesignerVehicleArrangementPanel.vue` sits outside day switching. It lists vehicle type, Dn range, supplier/pending state, cost, edit/delete actions, and `新增全程用车`. It emits `select-vehicle`, `edit-vehicle`, `delete-vehicle`, and `reorder-vehicles`; it does not render a map.

- [ ] **Step 6: Wire the page orchestration without duplicating business rules**

In `designer.vue`, replace legacy `activeAccommodations`/meal and group rendering markup with the child panel. Keep `loadDetail`, error handling and existing Word material functions in the page. On a successful mutation: refresh detail, scroll to the target section, set the new row ID as transient highlight, and clear meal context when the day changes.

- [ ] **Step 7: Run frontend unit tests for this layer**

Run:

```bash
cd frontend-preview
pnpm exec vitest run --dom \
  apps/web-antd/src/views/sales/product/product-designer-arrangement-utils.test.ts \
  apps/web-antd/src/views/sales/product/product-designer-itinerary.test.ts
```

Expected: all resource types resolve to the intended section and the page source/component contract contains the four fixed areas and compact supplier control.

---

### Task 8: Unify compact and fullscreen map markers, lists and add behavior

**Files:**
- Create: `frontend-preview/apps/web-antd/src/views/sales/product/components/product-resource-map-marker.ts`
- Create: `frontend-preview/apps/web-antd/src/views/sales/product/components/product-resource-map-marker.css`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/components/ProductResourceMapWorkspace.vue`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/designer.vue`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/product-designer-flow.test.ts`

**Interfaces:**
- Consumes: `MapResource`, current selected resource ID, current-day arranged state, `activateMapResource` callback.
- Produces:

```ts
buildProductResourceMarkerHtml(resource, {
  selected: boolean,
  arranged: boolean,
}): string
```

- [ ] **Step 1: Create a safe shared hot-air-balloon marker builder**

Escape resource names before inserting them into the AMap marker HTML. Return one DOM structure with three state classes: `is-selected`, `is-arranged`, `is-pending`. Put all marker label/color/arrow CSS in the new global CSS module so scoped Vue styles cannot diverge.

- [ ] **Step 2: Replace both current marker implementations**

Replace compact-map `designer-map-label` construction and fullscreen `resource-map-label` construction with `content: buildProductResourceMarkerHtml(...)`. Use the same offset/z-index rule, map click callback and title in both components. Preserve clustering and the normal-marker fallback.

- [ ] **Step 3: Remove the duplicate full-screen add button**

Remove `add-resource`, `addingResourceIds`, `resourceActionLabel`, and the visible `加入 Dn` button from `ProductResourceMapWorkspace.vue`. List-row and marker click both emit one `activate-resource` event. For a non-repeatable resource already arranged on the current day, parent selection focuses/highlights the existing row; for ground service it creates another row.

- [ ] **Step 4: Make list density and state explicit**

Set desktop full-screen list width to 280–300px and rows to 48–52px. Keep name, type/city, price or `未定位`, and a small `已安排` state; do not reintroduce an action button. Preserve no-coordinate rows in the list and map failure fallback.

- [ ] **Step 5: Preserve lifecycle safety**

On filter/resource/selection changes, clear prior overlays before rerender. On close/unmount, clear cluster instances, markers and resize timers, then destroy the AMap instance. Keep the map plugin fallback non-blocking. Test close/open manually twice to confirm no duplicate markers or duplicate API requests.

- [ ] **Step 6: Run focused frontend tests**

Run:

```bash
cd frontend-preview
pnpm exec vitest run --dom \
  apps/web-antd/src/views/sales/product/product-designer-flow.test.ts \
  apps/web-antd/src/views/sales/product/product-designer-itinerary.test.ts
```

Expected: shared marker helper, no duplicated add button, correct resource routing, and full-screen workspace contract are present.

---

### Task 9: Integrate selection flows, compact confirmations and supplier/cost feedback

**Files:**
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/designer.vue`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/components/ProductDesignerDayArrangementPanel.vue`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/components/ProductDesignerVehicleArrangementPanel.vue`
- Modify: `frontend-preview/apps/web-antd/src/views/sales/product/components/ProductResourceSupplierPicker.vue`
- Test: `frontend-preview/apps/web-antd/src/views/sales/product/product-designer-itinerary.test.ts`

**Interfaces:**
- Consumes: Task 7 typed events and Task 8 map activation.
- Produces: one-click additions except for explicit meal/breakfast conflict decisions.

- [ ] **Step 1: Route all map/list activations through one command function**

Refactor `activateMapResource` to call `resolveArrangementTarget`. It must set selected marker state first, then either save directly, set the meal target, open the compact meal picker, or show the product-level vehicle hint. It must never decide the target based on the previous generic `itinerary` fallback.

- [ ] **Step 2: Use a tiny meal picker only for unknown meal context**

Keep the existing small `Modal`/`Radio.Group` pattern, title it `选择安排餐次`, and expose only breakfast, lunch and dinner. When a day panel slot initiated selection, skip the modal and write directly to the requested role. Clear the context after save, cancel and day change.

- [ ] **Step 3: Make breakfast conflicts intentional**

When the backend returns a breakfast conflict, show one confirmation with exact consequence:

```text
将改为外部早餐，并取消前一晚酒店的含次日早餐。是否继续？
```

or

```text
将标记酒店含次日早餐，并清除 Dn 已选外部早餐餐厅。是否继续？
```

On confirmation, retry the same save with `replaceBreakfastSource: true`; on cancel, make no mutation.

- [ ] **Step 4: Show supplier and pending-price feedback consistently**

After resource add, show one compact success message naming the destination section. The added row immediately shows default supplier/cost or `待询价 · ¥0.00`. Supplier picker selection calls the narrow endpoint, reloads detail, and shows the refreshed backend total; no component recomputes `totalCostAmount` locally.

- [ ] **Step 5: Integrate vehicle creation/editing in a standard modal**

Use a standard Ant Design Vue Modal for the short vehicle form: resource, valid supplier relation, vehicle type, start/end day, quantity and remark. Existing long-form team arrangement vehicle UI is not embedded because it belongs to a different product scope. Saving refreshes product cost and the dedicated vehicle panel only.

- [ ] **Step 6: Run the day-flow tests and typecheck**

Run:

```bash
cd frontend-preview
pnpm exec vitest run --dom \
  apps/web-antd/src/views/sales/product/product-designer-itinerary.test.ts \
  apps/web-antd/src/views/sales/product/product-designer-flow.test.ts \
  apps/web-antd/src/views/sales/product/product-designer-arrangement-utils.test.ts
pnpm -F @vben/web-antd run typecheck
```

Expected: no type errors; all map/list actions, meal context, supplier switch and vehicle area compile against the final API types.

---

### Task 10: Apply migrations under authorization, restart safely, and perform browser acceptance

**Files:**
- Modify after successful implementation: `文档/开发进度与模块记录.md`
- No Git files or commits.

**Interfaces:**
- Consumes: tested code from Tasks 1–9 and user confirmation of target database.
- Produces: live local/approved environment with schema, backend, frontend and browser evidence aligned.

- [ ] **Step 1: Obtain fresh database execution authorization and validate the target**

Before any write, confirm with the user that the target remains the current `mtravel` database. Read `.env.local` only to verify host/database target without printing credentials. Run read-only checks for active duplicates and current index predicate before applying a migration.

- [ ] **Step 2: Back up and apply 052 then 053 in order**

Create a timestamped PostgreSQL backup using the approved target, then execute:

```bash
psql "$MT_DB_URL" -v ON_ERROR_STOP=1 -f db/052_sales_product_designer_multi_hotel_accommodation.sql
psql "$MT_DB_URL" -v ON_ERROR_STOP=1 -f db/053_sales_product_designer_resource_arrangement_v2.sql
```

Do not use a broad or inferred database URL. Stop immediately on a migration error; report the exact non-sensitive constraint/index issue rather than changing data ad hoc.

- [ ] **Step 3: Validate the live schema after migration**

Run the Task 2 verification SQL plus:

```sql
SELECT arrangement_role, count(*)
FROM sales_product_day_resources
WHERE is_deleted = false
GROUP BY arrangement_role
ORDER BY arrangement_role;
```

Expected: `uk_sales_product_day_resources_day_meal_role_active` has only three meal roles; V2 table/columns exist; any migrated `ground_agent` rows use `ground_service`.

- [ ] **Step 4: Run final backend tests and restart the actual service**

Run:

```bash
cd backend
mvn -q -Dtest=SalesProductDesignerServiceTest,SalesProductDesignerDocumentServiceTest,SalesProductServiceTest test
cd ..
./start-backend.sh
curl -s http://127.0.0.1:8080/actuator/health
```

Expected: targeted tests pass and health reports `UP`. Do not claim the new backend behavior is live before the restart/health check succeeds.

- [ ] **Step 5: Run final frontend verification**

Run the three focused Vitest files once, then:

```bash
cd frontend-preview
pnpm -F @vben/web-antd run typecheck
```

Expected: all tests and typecheck pass. Record any pre-existing unrelated failure separately instead of modifying unrelated modules.

- [ ] **Step 6: Perform browser acceptance in desktop and narrow widths**

Use a product-design draft with at least two hotels, a hotel with D+1 breakfast, lunch/dinner restaurants, scenic/shopping/other rows, two same-day ground services and two vehicle rows. Verify in this order:

1. Add two hotels to one day; close/reopen and refresh.
2. Toggle previous-night hotel breakfast; create and resolve an external-breakfast conflict.
3. Add each map-supported resource type and verify its target section/highlight.
4. Change a supplier and verify the backend cost total refreshes after page reload.
5. Add/edit/delete/reorder full-journey vehicles; ensure no vehicle appears in the map/day areas.
6. Open/close full-screen map twice; verify hot-air-balloon marker/name/state match compact map, no duplicate markers, and unlocated list rows still add.
7. Generate Word and inspect multi-hotel, breakfast, route, ground-service omission and vehicle summary text.
8. Repeat at a narrow viewport to verify map/list stacking and filter wrapping without overlap.

- [ ] **Step 7: Update the development record with evidence**

Update `文档/开发进度与模块记录.md` with: frontend files, backend endpoints, executed migration IDs, no changes to formal team-arrangement tables, targeted test results, health result, browser acceptance, and remaining P2 items (multi-restaurant, richer ground-service fields, pre-trip overnight breakfast).

---

## Plan self-review

| Spec requirement | Implemented by |
| --- | --- |
| 多酒店与线上索引修复 | Tasks 1–3, 10 |
| 前一晚酒店早餐和外部早餐互斥 | Tasks 1, 3, 6, 9, 10 |
| 地接可重复、独立排序 | Tasks 2–3, 7, 9 |
| 默认供应商、有效报价与成本快照 | Tasks 2, 4, 7, 9 |
| 产品级全程用车和成本一次计入 | Tasks 2, 5–7, 9–10 |
| 地图/全屏列表统一点击和热气球样式 | Tasks 7–9 |
| 报价和 Word 使用同一后端模型 | Tasks 4–6, 10 |
| 不影响采购主档、交通和正式团队安排 | Global Constraints, Tasks 5 and 10 |

No execution step in this plan performs a remote database write without a fresh confirmation. No task requires a Git commit.

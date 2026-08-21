-- 旅游接待管理系统：产品设计酒店与三餐资源化编排
-- PostgreSQL
-- 执行前必须确认目标环境、目标数据库，并先核验历史数据不会违反新增约束。

BEGIN;

ALTER TABLE sales_product_day_resources
  ADD COLUMN IF NOT EXISTS arrangement_role varchar(20) NOT NULL DEFAULT 'itinerary',
  ADD COLUMN IF NOT EXISTS hotel_breakfast_included boolean NOT NULL DEFAULT false;

-- 将历史酒店、餐厅和普通行程资源迁移到明确的业务归属，保留历史餐厅为未分配状态。
UPDATE sales_product_day_resources
SET arrangement_role = CASE
  WHEN resource_type_snapshot = 'hotel' THEN 'accommodation'
  WHEN resource_type_snapshot = 'restaurant' THEN 'unassigned'
  ELSE 'itinerary'
END
WHERE arrangement_role = 'itinerary';

ALTER TABLE sales_product_day_resources
  DROP CONSTRAINT IF EXISTS chk_sales_product_day_resources_arrangement_role,
  DROP CONSTRAINT IF EXISTS chk_sales_product_day_resources_hotel_breakfast;

ALTER TABLE sales_product_day_resources
  ADD CONSTRAINT chk_sales_product_day_resources_arrangement_role CHECK (
    (resource_type_snapshot = 'hotel' AND arrangement_role = 'accommodation')
    OR (resource_type_snapshot = 'restaurant' AND arrangement_role IN ('unassigned', 'breakfast', 'lunch', 'dinner'))
    OR (resource_type_snapshot NOT IN ('hotel', 'restaurant') AND arrangement_role = 'itinerary')
  ),
  ADD CONSTRAINT chk_sales_product_day_resources_hotel_breakfast CHECK (
    arrangement_role = 'accommodation' OR hotel_breakfast_included = false
  );

DROP INDEX IF EXISTS uk_sales_product_day_resources_day_resource_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_day_resources_day_resource_role_active
  ON sales_product_day_resources (tenant_id, product_id, day_no, resource_id, arrangement_role)
  WHERE is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_day_resources_day_meal_role_active
  ON sales_product_day_resources (tenant_id, product_id, day_no, arrangement_role)
  WHERE is_deleted = false AND arrangement_role IN ('breakfast', 'lunch', 'dinner');

COMMENT ON COLUMN sales_product_day_resources.arrangement_role IS
  '资源在当天的编排归属。酒店固定为accommodation；餐厅可为breakfast、lunch、dinner或unassigned；其他资源为itinerary。';
COMMENT ON COLUMN sales_product_day_resources.hotel_breakfast_included IS
  '当晚住宿酒店是否包含次日早餐，仅住宿酒店可设置。';
COMMENT ON INDEX uk_sales_product_day_resources_day_resource_role_active IS
  '同一产品同一天同一资源在同一编排归属下不能重复加入的唯一索引。';
COMMENT ON INDEX uk_sales_product_day_resources_day_meal_role_active IS
  '同一产品同一天每个餐次仅允许一个餐厅的唯一索引；当天住宿允许安排多个酒店。';

COMMIT;

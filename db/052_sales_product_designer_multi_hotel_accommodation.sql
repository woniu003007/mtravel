-- 旅游接待管理系统：产品设计当天多酒店住宿（历史误迁移，禁止新执行）
-- PostgreSQL
-- 当前业务规则已确定为“同一天只能安排一家酒店”。
-- 若历史上已经执行过本脚本，改用 055_sales_product_designer_single_hotel_accommodation.sql 恢复唯一约束。

BEGIN;

DROP INDEX IF EXISTS uk_sales_product_day_resources_day_meal_role_active;
CREATE UNIQUE INDEX uk_sales_product_day_resources_day_meal_role_active
  ON sales_product_day_resources (tenant_id, product_id, day_no, arrangement_role)
  WHERE is_deleted = false AND arrangement_role IN ('breakfast', 'lunch', 'dinner');

COMMENT ON INDEX uk_sales_product_day_resources_day_meal_role_active IS
  '同一产品同一天每个餐次仅允许一个餐厅的唯一索引；当天住宿允许安排多个酒店。';

COMMIT;

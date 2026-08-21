-- 旅游接待管理系统：产品设计当天多酒店住宿
-- PostgreSQL
-- 051 已执行过的数据库需执行本脚本，移除“当天仅一个住宿”的错误限制。

BEGIN;

DROP INDEX IF EXISTS uk_sales_product_day_resources_day_meal_role_active;
CREATE UNIQUE INDEX uk_sales_product_day_resources_day_meal_role_active
  ON sales_product_day_resources (tenant_id, product_id, day_no, arrangement_role)
  WHERE is_deleted = false AND arrangement_role IN ('breakfast', 'lunch', 'dinner');

COMMENT ON INDEX uk_sales_product_day_resources_day_meal_role_active IS
  '同一产品同一天每个餐次仅允许一个餐厅的唯一索引；当天住宿允许安排多个酒店。';

COMMIT;

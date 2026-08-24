-- 旅游接待管理系统：产品设计当天单酒店住宿规则
-- PostgreSQL
-- 仅供曾执行 052 的目标库恢复当前规则：同一天只能安排一家酒店，每餐次只能安排一家餐厅。
-- 执行前先检查是否已有同产品、同日的多条有效住宿；存在时需由业务人员先确认保留哪一家。

BEGIN;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM sales_product_day_resources
    WHERE is_deleted = false
      AND arrangement_role = 'accommodation'
    GROUP BY tenant_id, product_id, day_no
    HAVING COUNT(*) > 1
  ) THEN
    RAISE EXCEPTION '存在同一天多家有效酒店，请先完成业务数据清理后再执行 055';
  END IF;
END $$;

DROP INDEX IF EXISTS uk_sales_product_day_resources_day_meal_role_active;
CREATE UNIQUE INDEX uk_sales_product_day_resources_day_meal_role_active
  ON sales_product_day_resources (tenant_id, product_id, day_no, arrangement_role)
  WHERE is_deleted = false AND arrangement_role IN ('accommodation', 'breakfast', 'lunch', 'dinner');

COMMENT ON INDEX uk_sales_product_day_resources_day_meal_role_active IS
  '同一产品同一天仅允许一家酒店，且每个餐次仅允许一个餐厅的唯一索引。';

COMMIT;

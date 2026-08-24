-- 旅游接待管理系统：产品设计无需采购报价快照修正
-- PostgreSQL
-- 依赖 053 已完成：允许每日资源和产品级全程用车明确保存无需采购状态。
-- 执行前必须确认目标环境和数据库，并按上线流程完成备份与历史数据预检。

BEGIN;

ALTER TABLE sales_product_day_resources
  DROP CONSTRAINT IF EXISTS chk_sales_product_day_resources_price_mode,
  ADD CONSTRAINT chk_sales_product_day_resources_price_mode CHECK (
    price_mode_snapshot IS NULL
    OR price_mode_snapshot IN ('unified', 'classified', 'pending', 'not_required')
  );

ALTER TABLE sales_product_designer_vehicle_arrangements
  DROP CONSTRAINT IF EXISTS chk_sales_product_designer_vehicle_price_mode,
  ADD CONSTRAINT chk_sales_product_designer_vehicle_price_mode CHECK (
    price_mode_snapshot IN ('unified', 'classified', 'pending', 'not_required')
  );

COMMENT ON COLUMN sales_product_day_resources.price_mode_snapshot IS
  '保存时采购关系报价模式快照。unified统一报价，classified分类报价，pending表示待询价，not_required表示无需采购。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.price_mode_snapshot IS
  '采购关系报价模式快照。unified统一报价，classified分类报价，pending表示待询价，not_required表示无需采购。';

COMMIT;

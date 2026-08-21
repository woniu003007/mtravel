-- 旅游接待管理系统：产品设计草稿生命周期
-- PostgreSQL

BEGIN;

ALTER TABLE sales_products
  ADD COLUMN IF NOT EXISTS product_scope varchar(30) NOT NULL DEFAULT 'template';

ALTER TABLE sales_products
  DROP CONSTRAINT IF EXISTS chk_sales_products_scope;

ALTER TABLE sales_products
  ADD CONSTRAINT chk_sales_products_scope
  CHECK (product_scope IN ('template', 'team_snapshot', 'design_draft'));

DROP INDEX IF EXISTS uk_sales_products_tenant_name_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_products_tenant_name_active
  ON sales_products (tenant_id, product_name)
  WHERE is_deleted = false AND product_scope IN ('template', 'design_draft');

DROP INDEX IF EXISTS uk_sales_products_tenant_draft_name_active;

CREATE INDEX IF NOT EXISTS idx_sales_products_tenant_scope_updated
  ON sales_products (tenant_id, is_deleted, product_scope, updated_at DESC, id DESC);

COMMENT ON COLUMN sales_products.product_scope IS
  '产品记录用途。design_draft表示尚未进入产品管理的设计草稿；template表示已完成设计的正式产品模板；team_snapshot表示团队专属产品快照。';

COMMENT ON TABLE sales_products IS
  '销售产品主表。用于维护正式线路产品模板、产品设计草稿和团队专属快照。';

COMMENT ON INDEX idx_sales_products_tenant_scope_updated IS
  '支持按租户和产品用途分页查询设计草稿或正式产品。';

COMMENT ON INDEX uk_sales_products_tenant_name_active IS
  '产品名称唯一索引，仅约束同一租户下未删除的template和design_draft产品，排除团队专属快照。';

COMMIT;

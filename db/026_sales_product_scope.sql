ALTER TABLE sales_products
  ADD COLUMN IF NOT EXISTS product_scope varchar(30) NOT NULL DEFAULT 'template';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_sales_products_scope'
  ) THEN
    ALTER TABLE sales_products
      ADD CONSTRAINT chk_sales_products_scope CHECK (product_scope IN ('template', 'team_snapshot'));
  END IF;
END $$;

UPDATE sales_products p
SET product_scope = 'team_snapshot'
WHERE p.is_deleted = false
  AND EXISTS (
    SELECT 1
    FROM sales_teams t
    WHERE t.tenant_id = p.tenant_id
      AND t.product_id = p.id
      AND t.is_deleted = false
  );

DROP INDEX IF EXISTS uk_sales_products_tenant_name_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_products_tenant_name_active
  ON sales_products (tenant_id, product_name)
  WHERE is_deleted = false AND product_scope = 'template';

DROP INDEX IF EXISTS idx_sales_products_tenant_deleted_status;
CREATE INDEX IF NOT EXISTS idx_sales_products_tenant_deleted_status
  ON sales_products (tenant_id, is_deleted, product_scope, status);

COMMENT ON COLUMN sales_products.product_scope IS '产品记录用途。template 表示正式产品模板；team_snapshot 表示直接建团产生的团队专属产品快照，不进入产品管理列表。';

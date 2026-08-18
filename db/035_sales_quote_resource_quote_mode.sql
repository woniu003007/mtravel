-- 普通资源报价规则：限定可用报价方式
-- PostgreSQL

BEGIN;

ALTER TABLE sales_quote_resource_rules
  ADD COLUMN IF NOT EXISTS quote_mode varchar(20) NOT NULL DEFAULT 'both';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_sales_quote_resource_rule_mode'
      AND conrelid = 'sales_quote_resource_rules'::regclass
  ) THEN
    ALTER TABLE sales_quote_resource_rules
      ADD CONSTRAINT chk_sales_quote_resource_rule_mode
      CHECK (quote_mode IN ('rate', 'fixed', 'both'));
  END IF;
END $$;

COMMENT ON TABLE sales_quote_resource_rules IS '普通资源销售报价规则表。按资源类型和客户等级维护采购成本基础上的建议上浮和最低上浮规则，并限定可用报价方式。';
COMMENT ON COLUMN sales_quote_resource_rules.quote_mode IS '报价方式。rate按比例报价，fixed按固定加价报价，both两种方式均可报价。';

COMMIT;

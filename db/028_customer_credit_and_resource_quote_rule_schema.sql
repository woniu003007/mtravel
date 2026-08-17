-- 旅游接待管理系统：客户授信规则与普通资源报价规则
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS customer_credit_rules (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  customer_level_id bigint NOT NULL,
  credit_limit numeric(14,2) NOT NULL DEFAULT 0,
  account_period_days integer NOT NULL DEFAULT 0,
  allow_over_limit boolean NOT NULL DEFAULT false,
  approver_employee_ids varchar(1000) NOT NULL DEFAULT '',
  cc_employee_ids varchar(1000) NOT NULL DEFAULT '',
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_customer_credit_rules_amount CHECK (credit_limit >= 0),
  CONSTRAINT chk_customer_credit_rules_account_period CHECK (account_period_days >= 0),
  CONSTRAINT chk_customer_credit_rules_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT fk_customer_credit_rules_level FOREIGN KEY (tenant_id, customer_level_id)
    REFERENCES customer_categories (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS resource_quote_rules (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_type varchar(40) NOT NULL,
  customer_level_id bigint,
  suggested_markup_rate numeric(8,2) NOT NULL DEFAULT 0,
  minimum_markup_rate numeric(8,2) NOT NULL DEFAULT 0,
  suggested_fixed_markup numeric(14,2) NOT NULL DEFAULT 0,
  minimum_fixed_markup numeric(14,2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_resource_quote_rules_resource_type CHECK (
    resource_type IN ('hotel', 'scenic', 'vehicle', 'restaurant', 'guide', 'ground_agent', 'ticket', 'shopping', 'other')
  ),
  CONSTRAINT chk_resource_quote_rules_rates CHECK (
    suggested_markup_rate >= 0
    AND minimum_markup_rate >= 0
    AND minimum_markup_rate <= suggested_markup_rate
  ),
  CONSTRAINT chk_resource_quote_rules_amounts CHECK (
    suggested_fixed_markup >= 0
    AND minimum_fixed_markup >= 0
    AND minimum_fixed_markup <= suggested_fixed_markup
  ),
  CONSTRAINT chk_resource_quote_rules_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT fk_resource_quote_rules_level FOREIGN KEY (tenant_id, customer_level_id)
  REFERENCES customer_categories (tenant_id, id)
);

-- 保证脚本重复执行时，已存在的表也会收紧为相同的跨字段业务约束。
ALTER TABLE resource_quote_rules
  DROP CONSTRAINT IF EXISTS chk_resource_quote_rules_rates,
  DROP CONSTRAINT IF EXISTS chk_resource_quote_rules_amounts;

ALTER TABLE resource_quote_rules
  ADD CONSTRAINT chk_resource_quote_rules_rates CHECK (
    suggested_markup_rate >= 0
    AND minimum_markup_rate >= 0
    AND minimum_markup_rate <= suggested_markup_rate
  ),
  ADD CONSTRAINT chk_resource_quote_rules_amounts CHECK (
    suggested_fixed_markup >= 0
    AND minimum_fixed_markup >= 0
    AND minimum_fixed_markup <= suggested_fixed_markup
  );

DROP TRIGGER IF EXISTS trg_customer_credit_rules_updated_at ON customer_credit_rules;
CREATE TRIGGER trg_customer_credit_rules_updated_at
BEFORE UPDATE ON customer_credit_rules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_resource_quote_rules_updated_at ON resource_quote_rules;
CREATE TRIGGER trg_resource_quote_rules_updated_at
BEFORE UPDATE ON resource_quote_rules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_customer_credit_rules_tenant_deleted_limit
  ON customer_credit_rules (tenant_id, is_deleted, credit_limit, id);

CREATE INDEX IF NOT EXISTS idx_customer_credit_rules_tenant_deleted_status
  ON customer_credit_rules (tenant_id, is_deleted, status, credit_limit, id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_credit_rules_tenant_level_active
  ON customer_credit_rules (tenant_id, customer_level_id)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_resource_quote_rules_tenant_deleted_type_level
  ON resource_quote_rules (tenant_id, is_deleted, resource_type, customer_level_id, id);

CREATE INDEX IF NOT EXISTS idx_resource_quote_rules_tenant_deleted_status_type
  ON resource_quote_rules (tenant_id, is_deleted, status, resource_type, id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_resource_quote_rules_tenant_type_level_active
  ON resource_quote_rules (tenant_id, resource_type, customer_level_id)
  WHERE is_deleted = false AND customer_level_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_resource_quote_rules_tenant_type_default_active
  ON resource_quote_rules (tenant_id, resource_type)
  WHERE is_deleted = false AND customer_level_id IS NULL;

COMMENT ON TABLE customer_credit_rules IS '客户授信规则表。按客户等级维护默认授信额度、账期、超额处理与审批通知配置。';
COMMENT ON COLUMN customer_credit_rules.id IS '客户授信规则主键ID，系统内部使用。';
COMMENT ON COLUMN customer_credit_rules.tenant_id IS '租户ID，标识规则属于哪一家地接公司。';
COMMENT ON COLUMN customer_credit_rules.customer_level_id IS '客户等级ID，关联客户分类字典。';
COMMENT ON COLUMN customer_credit_rules.credit_limit IS '该客户等级的默认授信额度，单位为元。';
COMMENT ON COLUMN customer_credit_rules.account_period_days IS '该客户等级允许的账期天数。';
COMMENT ON COLUMN customer_credit_rules.allow_over_limit IS '是否允许订单金额超过可用授信额度。';
COMMENT ON COLUMN customer_credit_rules.approver_employee_ids IS '按审批顺序保存的企业员工ID列表，使用英文逗号分隔。';
COMMENT ON COLUMN customer_credit_rules.cc_employee_ids IS '接收审批通知的企业员工ID列表，使用英文逗号分隔。';
COMMENT ON COLUMN customer_credit_rules.status IS '规则状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN customer_credit_rules.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN customer_credit_rules.remark IS '规则备注，用于记录适用口径或内部说明。';
COMMENT ON COLUMN customer_credit_rules.created_at IS '创建时间。';
COMMENT ON COLUMN customer_credit_rules.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN customer_credit_rules.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN customer_credit_rules.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN customer_credit_rules.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE resource_quote_rules IS '普通资源报价规则表。按资源类型和可选客户等级维护建议及最低报价上浮口径。';
COMMENT ON COLUMN resource_quote_rules.id IS '普通资源报价规则主键ID，系统内部使用。';
COMMENT ON COLUMN resource_quote_rules.tenant_id IS '租户ID，标识规则属于哪一家地接公司。';
COMMENT ON COLUMN resource_quote_rules.resource_type IS '资源类型，例如酒店、景区、车辆、餐厅或地接。';
COMMENT ON COLUMN resource_quote_rules.customer_level_id IS '客户等级ID。为空时表示不区分客户等级的默认规则。';
COMMENT ON COLUMN resource_quote_rules.suggested_markup_rate IS '建议比例上浮，按百分数保存，例如20表示20%。';
COMMENT ON COLUMN resource_quote_rules.minimum_markup_rate IS '最低比例上浮，按百分数保存，例如15表示15%。';
COMMENT ON COLUMN resource_quote_rules.suggested_fixed_markup IS '建议固定加价，单位为元。';
COMMENT ON COLUMN resource_quote_rules.minimum_fixed_markup IS '最低固定加价，单位为元。';
COMMENT ON COLUMN resource_quote_rules.status IS '规则状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN resource_quote_rules.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN resource_quote_rules.remark IS '规则备注，用于记录适用口径或内部说明。';
COMMENT ON COLUMN resource_quote_rules.created_at IS '创建时间。';
COMMENT ON COLUMN resource_quote_rules.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN resource_quote_rules.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN resource_quote_rules.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN resource_quote_rules.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMIT;

-- 旅游接待管理系统：导游备用金申请与发放表
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 仅用于让本脚本可在独立临时库中校验。正式库中这些表已由前置脚本创建。
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS sales_teams (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_no varchar(80) NOT NULL,
  team_type varchar(20),
  business_type varchar(120),
  departure_date date,
  department_id bigint,
  department_name varchar(160),
  operator_employee_id bigint,
  operator_employee_name varchar(100),
  is_deleted boolean NOT NULL DEFAULT false,
  CONSTRAINT uk_sales_teams_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS system_configs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  config_key varchar(100) NOT NULL,
  config_value varchar(500) NOT NULL,
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_system_configs_tenant_key UNIQUE (tenant_id, config_key)
);

CREATE TABLE IF NOT EXISTS finance_guide_imprests (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  request_no varchar(64) NOT NULL,
  team_id bigint NOT NULL,
  team_no varchar(80) NOT NULL,
  team_type varchar(20),
  business_type varchar(120),
  departure_date date,
  department_id bigint,
  department_name varchar(160),
  operator_employee_id bigint,
  operator_employee_name varchar(100),
  guide_id bigint NOT NULL,
  guide_name varchar(100) NOT NULL,
  guide_mobile varchar(40),
  guest_count integer NOT NULL DEFAULT 0,
  company_markup_rate numeric(8,2) NOT NULL DEFAULT 70,
  cash_cost_amount numeric(14,2) NOT NULL DEFAULT 0,
  optional_deduction_amount numeric(14,2) NOT NULL DEFAULT 0,
  calculated_amount numeric(14,2) NOT NULL DEFAULT 0,
  suggested_imprest_amount numeric(14,2) NOT NULL DEFAULT 0,
  guide_turn_in_amount numeric(14,2) NOT NULL DEFAULT 0,
  requested_amount numeric(14,2) NOT NULL DEFAULT 0,
  approved_amount numeric(14,2) NOT NULL DEFAULT 0,
  paid_amount numeric(14,2) NOT NULL DEFAULT 0,
  balance_amount numeric(14,2) NOT NULL DEFAULT 0,
  status varchar(30) NOT NULL DEFAULT 'pending_manager',
  applicant varchar(80),
  applied_at timestamptz,
  approved_by varchar(80),
  approved_at timestamptz,
  rejected_by varchar(80),
  rejected_at timestamptz,
  approval_remark text,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_finance_guide_imprests_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT uk_finance_guide_imprests_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_finance_guide_imprests_status CHECK (
    status IN ('draft', 'pending_manager', 'manager_approved', 'manager_rejected', 'paid', 'settled', 'cancelled')
  ),
  CONSTRAINT chk_finance_guide_imprests_amount CHECK (
    guest_count >= 0
    AND company_markup_rate >= 0
    AND cash_cost_amount >= 0
    AND optional_deduction_amount >= 0
    AND suggested_imprest_amount >= 0
    AND guide_turn_in_amount >= 0
    AND requested_amount >= 0
    AND approved_amount >= 0
    AND paid_amount >= 0
    AND balance_amount >= 0
  )
);

ALTER TABLE finance_guide_imprests
  ADD COLUMN IF NOT EXISTS cancelled_by varchar(80),
  ADD COLUMN IF NOT EXISTS cancelled_at timestamptz,
  ADD COLUMN IF NOT EXISTS cancel_reason text;

DROP TRIGGER IF EXISTS trg_finance_guide_imprests_updated_at ON finance_guide_imprests;
CREATE TRIGGER trg_finance_guide_imprests_updated_at
BEFORE UPDATE ON finance_guide_imprests
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_finance_guide_imprests_tenant_request_no_active
  ON finance_guide_imprests (tenant_id, request_no)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_finance_guide_imprests_tenant_status_time
  ON finance_guide_imprests (tenant_id, is_deleted, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_finance_guide_imprests_tenant_team_guide
  ON finance_guide_imprests (tenant_id, is_deleted, team_id, guide_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_finance_guide_imprests_tenant_departure
  ON finance_guide_imprests (tenant_id, is_deleted, departure_date, status);

CREATE INDEX IF NOT EXISTS idx_finance_guide_imprests_tenant_operator
  ON finance_guide_imprests (tenant_id, is_deleted, operator_employee_id, created_at DESC);

CREATE TABLE IF NOT EXISTS finance_guide_imprest_calc_lines (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  imprest_id bigint NOT NULL,
  team_id bigint NOT NULL,
  line_type varchar(30) NOT NULL,
  source_arrangement_id bigint,
  source_price_line_id bigint,
  arrangement_type varchar(30),
  item_name varchar(160) NOT NULL,
  sale_price numeric(14,2) NOT NULL DEFAULT 0,
  cost_price numeric(14,2) NOT NULL DEFAULT 0,
  guide_commission_amount numeric(14,2) NOT NULL DEFAULT 0,
  guide_commission_rate numeric(8,2) NOT NULL DEFAULT 0,
  guide_commission_calc_type varchar(20),
  company_markup_rate numeric(8,2) NOT NULL DEFAULT 70,
  guest_count integer NOT NULL DEFAULT 0,
  amount numeric(14,2) NOT NULL DEFAULT 0,
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_finance_guide_imprest_calc_imprest
    FOREIGN KEY (tenant_id, imprest_id) REFERENCES finance_guide_imprests (tenant_id, id),
  CONSTRAINT fk_finance_guide_imprest_calc_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT chk_finance_guide_imprest_calc_line_type CHECK (line_type IN ('cash_cost', 'optional_deduction')),
  CONSTRAINT chk_finance_guide_imprest_calc_commission_type CHECK (
    guide_commission_calc_type IS NULL OR guide_commission_calc_type IN ('fixed', 'percent')
  ),
  CONSTRAINT chk_finance_guide_imprest_calc_amount CHECK (
    sale_price >= 0
    AND cost_price >= 0
    AND guide_commission_amount >= 0
    AND guide_commission_rate >= 0
    AND company_markup_rate >= 0
    AND guest_count >= 0
    AND amount >= 0
    AND sort_order >= 1
  )
);

DROP TRIGGER IF EXISTS trg_finance_guide_imprest_calc_lines_updated_at ON finance_guide_imprest_calc_lines;
CREATE TRIGGER trg_finance_guide_imprest_calc_lines_updated_at
BEFORE UPDATE ON finance_guide_imprest_calc_lines
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_finance_guide_imprest_calc_tenant_imprest
  ON finance_guide_imprest_calc_lines (tenant_id, is_deleted, imprest_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_finance_guide_imprest_calc_tenant_team
  ON finance_guide_imprest_calc_lines (tenant_id, is_deleted, team_id, line_type);

CREATE TABLE IF NOT EXISTS finance_guide_imprest_payments (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  imprest_id bigint NOT NULL,
  team_id bigint NOT NULL,
  payment_no varchar(64) NOT NULL,
  payment_date date NOT NULL,
  payment_method varchar(40),
  payment_account_name varchar(120),
  amount numeric(14,2) NOT NULL DEFAULT 0,
  payer varchar(80),
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_finance_guide_imprest_payments_imprest
    FOREIGN KEY (tenant_id, imprest_id) REFERENCES finance_guide_imprests (tenant_id, id),
  CONSTRAINT fk_finance_guide_imprest_payments_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT chk_finance_guide_imprest_payments_status CHECK (status IN ('active', 'cancelled')),
  CONSTRAINT chk_finance_guide_imprest_payments_amount CHECK (amount > 0)
);

DROP TRIGGER IF EXISTS trg_finance_guide_imprest_payments_updated_at ON finance_guide_imprest_payments;
CREATE TRIGGER trg_finance_guide_imprest_payments_updated_at
BEFORE UPDATE ON finance_guide_imprest_payments
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_finance_guide_imprest_payments_tenant_payment_no_active
  ON finance_guide_imprest_payments (tenant_id, payment_no)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_finance_guide_imprest_payments_tenant_imprest
  ON finance_guide_imprest_payments (tenant_id, is_deleted, imprest_id, payment_date);

CREATE INDEX IF NOT EXISTS idx_finance_guide_imprest_payments_tenant_team_date
  ON finance_guide_imprest_payments (tenant_id, is_deleted, team_id, payment_date);

COMMENT ON TABLE finance_guide_imprests IS '导游备用金申请表。保存计调提交的备用金申请、公式计算快照、总经理审批状态和财务付款汇总。';
COMMENT ON COLUMN finance_guide_imprests.id IS '导游备用金申请主键 ID。';
COMMENT ON COLUMN finance_guide_imprests.tenant_id IS '租户 ID，用于隔离不同地接公司的备用金数据。';
COMMENT ON COLUMN finance_guide_imprests.request_no IS '备用金申请编号，同一租户下未删除申请唯一。';
COMMENT ON COLUMN finance_guide_imprests.team_id IS '关联团队 ID。';
COMMENT ON COLUMN finance_guide_imprests.team_no IS '团号快照。';
COMMENT ON COLUMN finance_guide_imprests.team_type IS '团队类型快照。';
COMMENT ON COLUMN finance_guide_imprests.business_type IS '业务类型快照。';
COMMENT ON COLUMN finance_guide_imprests.departure_date IS '发团日期快照。';
COMMENT ON COLUMN finance_guide_imprests.department_id IS '部门 ID 快照。';
COMMENT ON COLUMN finance_guide_imprests.department_name IS '部门名称快照。';
COMMENT ON COLUMN finance_guide_imprests.operator_employee_id IS '操作计调员工 ID 快照。';
COMMENT ON COLUMN finance_guide_imprests.operator_employee_name IS '操作计调姓名快照。';
COMMENT ON COLUMN finance_guide_imprests.guide_id IS '导游档案 ID。';
COMMENT ON COLUMN finance_guide_imprests.guide_name IS '导游姓名快照。';
COMMENT ON COLUMN finance_guide_imprests.guide_mobile IS '导游手机号快照。';
COMMENT ON COLUMN finance_guide_imprests.guest_count IS '团队实收人数快照。';
COMMENT ON COLUMN finance_guide_imprests.company_markup_rate IS '本次公司加点率快照，按百分数保存；默认取系统配置，也允许计调按申请覆盖。';
COMMENT ON COLUMN finance_guide_imprests.cash_cost_amount IS '计算时现付总成本。';
COMMENT ON COLUMN finance_guide_imprests.optional_deduction_amount IS '计算时自费加点抵扣金额。';
COMMENT ON COLUMN finance_guide_imprests.calculated_amount IS '原始公式计算结果，可为负数。';
COMMENT ON COLUMN finance_guide_imprests.suggested_imprest_amount IS '建议发放备用金金额，计算结果为负数时为 0。';
COMMENT ON COLUMN finance_guide_imprests.guide_turn_in_amount IS '计算结果为负数时导游应上交金额。';
COMMENT ON COLUMN finance_guide_imprests.requested_amount IS '计调本次手工填写的申请发放金额。';
COMMENT ON COLUMN finance_guide_imprests.approved_amount IS '总经理同意后的审批金额。';
COMMENT ON COLUMN finance_guide_imprests.paid_amount IS '已付款金额汇总。';
COMMENT ON COLUMN finance_guide_imprests.balance_amount IS '剩余未付款金额。';
COMMENT ON COLUMN finance_guide_imprests.status IS '申请状态。draft草稿，pending_manager待总经理审批，manager_approved总经理已同意，manager_rejected总经理已拒绝，paid已付款，settled已结算，cancelled已取消。';
COMMENT ON COLUMN finance_guide_imprests.applicant IS '申请人账号或姓名。';
COMMENT ON COLUMN finance_guide_imprests.applied_at IS '申请提交时间。';
COMMENT ON COLUMN finance_guide_imprests.approved_by IS '审批同意人账号或姓名。';
COMMENT ON COLUMN finance_guide_imprests.approved_at IS '审批同意时间。';
COMMENT ON COLUMN finance_guide_imprests.rejected_by IS '审批拒绝人账号或姓名。';
COMMENT ON COLUMN finance_guide_imprests.rejected_at IS '审批拒绝时间。';
COMMENT ON COLUMN finance_guide_imprests.approval_remark IS '审批意见。';
COMMENT ON COLUMN finance_guide_imprests.cancelled_by IS '作废人账号或姓名。';
COMMENT ON COLUMN finance_guide_imprests.cancelled_at IS '作废时间。';
COMMENT ON COLUMN finance_guide_imprests.cancel_reason IS '作废原因。';
COMMENT ON COLUMN finance_guide_imprests.created_by IS '创建人账号或姓名。';
COMMENT ON COLUMN finance_guide_imprests.remark IS '申请备注。';
COMMENT ON COLUMN finance_guide_imprests.created_at IS '创建时间。';
COMMENT ON COLUMN finance_guide_imprests.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN finance_guide_imprests.is_deleted IS '是否已软删除。false正常，true已删除。';
COMMENT ON COLUMN finance_guide_imprests.deleted_at IS '软删除时间。';
COMMENT ON COLUMN finance_guide_imprests.deleted_by IS '执行软删除的操作人。';
COMMENT ON INDEX uk_finance_guide_imprests_tenant_request_no_active IS '导游备用金申请编号唯一索引，仅约束未删除记录。';
COMMENT ON INDEX idx_finance_guide_imprests_tenant_status_time IS '按租户、状态和创建时间查询备用金申请。';
COMMENT ON INDEX idx_finance_guide_imprests_tenant_team_guide IS '按团队和导游查询备用金申请。';
COMMENT ON INDEX idx_finance_guide_imprests_tenant_departure IS '按发团日期和状态查询备用金申请。';
COMMENT ON INDEX idx_finance_guide_imprests_tenant_operator IS '按操作计调查询备用金申请。';

COMMENT ON TABLE finance_guide_imprest_calc_lines IS '导游备用金计算明细表。保存每次申请时现付成本和自费加点抵扣的计算依据快照。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.id IS '导游备用金计算明细主键 ID。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.tenant_id IS '租户 ID，用于隔离不同地接公司的计算明细。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.imprest_id IS '关联导游备用金申请 ID。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.team_id IS '关联团队 ID。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.line_type IS '明细类型。cash_cost现付成本，optional_deduction自费加点抵扣。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.source_arrangement_id IS '来源团队安排成本 ID。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.source_price_line_id IS '来源团队安排价格明细 ID。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.arrangement_type IS '团队安排类型。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.item_name IS '项目名称。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.sale_price IS '自费售价快照。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.cost_price IS '自费成本快照。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.guide_commission_amount IS '导游提成金额快照。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.guide_commission_rate IS '导游提成比例快照。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.guide_commission_calc_type IS '导游提成计算方式。fixed固定金额，percent毛利百分比。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.company_markup_rate IS '本次公司加点率快照。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.guest_count IS '团队实收人数快照。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.amount IS '本行计算金额。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.sort_order IS '排序号。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.created_by IS '创建人账号或姓名。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.remark IS '备注。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.created_at IS '创建时间。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.is_deleted IS '是否已软删除。false正常，true已删除。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.deleted_at IS '软删除时间。';
COMMENT ON COLUMN finance_guide_imprest_calc_lines.deleted_by IS '执行软删除的操作人。';
COMMENT ON INDEX idx_finance_guide_imprest_calc_tenant_imprest IS '按备用金申请查询计算明细。';
COMMENT ON INDEX idx_finance_guide_imprest_calc_tenant_team IS '按团队和明细类型查询计算明细。';

COMMENT ON TABLE finance_guide_imprest_payments IS '导游备用金付款记录表。保存一张备用金申请下的多次付款记录。';
COMMENT ON COLUMN finance_guide_imprest_payments.id IS '导游备用金付款记录主键 ID。';
COMMENT ON COLUMN finance_guide_imprest_payments.tenant_id IS '租户 ID，用于隔离不同地接公司的付款记录。';
COMMENT ON COLUMN finance_guide_imprest_payments.imprest_id IS '关联导游备用金申请 ID。';
COMMENT ON COLUMN finance_guide_imprest_payments.team_id IS '关联团队 ID。';
COMMENT ON COLUMN finance_guide_imprest_payments.payment_no IS '付款编号，同一租户下未删除记录唯一。';
COMMENT ON COLUMN finance_guide_imprest_payments.payment_date IS '付款日期。';
COMMENT ON COLUMN finance_guide_imprest_payments.payment_method IS '付款方式。';
COMMENT ON COLUMN finance_guide_imprest_payments.payment_account_name IS '付款账户名称。';
COMMENT ON COLUMN finance_guide_imprest_payments.amount IS '本次付款金额。';
COMMENT ON COLUMN finance_guide_imprest_payments.payer IS '付款经办人。';
COMMENT ON COLUMN finance_guide_imprest_payments.status IS '付款记录状态。active生效，cancelled已取消。';
COMMENT ON COLUMN finance_guide_imprest_payments.created_by IS '创建人账号或姓名。';
COMMENT ON COLUMN finance_guide_imprest_payments.remark IS '付款备注。';
COMMENT ON COLUMN finance_guide_imprest_payments.created_at IS '创建时间。';
COMMENT ON COLUMN finance_guide_imprest_payments.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN finance_guide_imprest_payments.is_deleted IS '是否已软删除。false正常，true已删除。';
COMMENT ON COLUMN finance_guide_imprest_payments.deleted_at IS '软删除时间。';
COMMENT ON COLUMN finance_guide_imprest_payments.deleted_by IS '执行软删除的操作人。';
COMMENT ON INDEX uk_finance_guide_imprest_payments_tenant_payment_no_active IS '导游备用金付款编号唯一索引，仅约束未删除记录。';
COMMENT ON INDEX idx_finance_guide_imprest_payments_tenant_imprest IS '按备用金申请和付款日期查询付款记录。';
COMMENT ON INDEX idx_finance_guide_imprest_payments_tenant_team_date IS '按团队和付款日期查询付款记录。';

INSERT INTO system_configs (tenant_id, config_key, config_value, remark)
SELECT id, 'guide_imprest_company_markup_rate', '70', '导游备用金计算使用的默认公司加点率，按百分数保存；每次申请可覆盖'
FROM tenants
ON CONFLICT (tenant_id, config_key) DO NOTHING;

COMMIT;

-- 旅游接待管理系统：购物业绩反馈与公司补佣结算表
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 仅用于让本脚本可在独立临时库中校验。正式库中 tenants、sales_teams 已由前置脚本创建。
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
  is_deleted boolean NOT NULL DEFAULT false,
  CONSTRAINT uk_sales_teams_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS finance_shopping_commission_rules (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  rule_name varchar(120) NOT NULL,
  threshold_per_capita_amount numeric(14,2) NOT NULL DEFAULT 5000,
  base_commission_rate numeric(8,2) NOT NULL DEFAULT 8,
  target_commission_rate numeric(8,2) NOT NULL DEFAULT 10,
  ladder_calc_mode varchar(30) NOT NULL DEFAULT 'full_amount_diff',
  effective_start_date date,
  effective_end_date date,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_finance_shopping_rules_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_finance_shopping_rules_mode CHECK (ladder_calc_mode IN ('full_amount_diff')),
  CONSTRAINT chk_finance_shopping_rules_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT chk_finance_shopping_rules_amount CHECK (
    threshold_per_capita_amount >= 0
    AND base_commission_rate >= 0
    AND target_commission_rate >= 0
  )
);

DROP TRIGGER IF EXISTS trg_finance_shopping_commission_rules_updated_at ON finance_shopping_commission_rules;
CREATE TRIGGER trg_finance_shopping_commission_rules_updated_at
BEFORE UPDATE ON finance_shopping_commission_rules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_finance_shopping_rules_tenant_status
  ON finance_shopping_commission_rules (tenant_id, is_deleted, status, id DESC);

CREATE TABLE IF NOT EXISTS finance_shopping_team_rule_overrides (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_id bigint NOT NULL,
  team_no varchar(80) NOT NULL,
  threshold_per_capita_amount numeric(14,2) NOT NULL DEFAULT 5000,
  base_commission_rate numeric(8,2) NOT NULL DEFAULT 8,
  target_commission_rate numeric(8,2) NOT NULL DEFAULT 10,
  ladder_calc_mode varchar(30) NOT NULL DEFAULT 'full_amount_diff',
  override_reason text,
  overridden_by varchar(80),
  overridden_at timestamptz,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_finance_shopping_override_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT uk_finance_shopping_override_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_finance_shopping_override_mode CHECK (ladder_calc_mode IN ('full_amount_diff')),
  CONSTRAINT chk_finance_shopping_override_status CHECK (status IN ('active', 'superseded', 'cancelled')),
  CONSTRAINT chk_finance_shopping_override_amount CHECK (
    threshold_per_capita_amount >= 0
    AND base_commission_rate >= 0
    AND target_commission_rate >= 0
  )
);

DROP TRIGGER IF EXISTS trg_finance_shopping_team_rule_overrides_updated_at ON finance_shopping_team_rule_overrides;
CREATE TRIGGER trg_finance_shopping_team_rule_overrides_updated_at
BEFORE UPDATE ON finance_shopping_team_rule_overrides
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_finance_shopping_override_tenant_team_active
  ON finance_shopping_team_rule_overrides (tenant_id, team_id)
  WHERE is_deleted = false AND status = 'active';

CREATE INDEX IF NOT EXISTS idx_finance_shopping_override_tenant_team
  ON finance_shopping_team_rule_overrides (tenant_id, is_deleted, team_id, status, id DESC);

CREATE TABLE IF NOT EXISTS finance_shopping_feedback_lines (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_id bigint NOT NULL,
  team_no varchar(80) NOT NULL,
  supplier_id bigint,
  shop_name varchar(200) NOT NULL,
  guide_id bigint,
  guide_name varchar(100),
  business_date date,
  people_count integer NOT NULL DEFAULT 0,
  consumption_amount numeric(14,2) NOT NULL DEFAULT 0,
  company_rebate_amount numeric(14,2) NOT NULL DEFAULT 0,
  guide_commission_amount numeric(14,2) NOT NULL DEFAULT 0,
  head_fee_amount numeric(14,2) NOT NULL DEFAULT 0,
  feedback_source varchar(20) NOT NULL DEFAULT 'manual',
  import_batch_id bigint,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  rebate_calc_mode varchar(20) NOT NULL DEFAULT 'total',
  CONSTRAINT fk_finance_shopping_feedback_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT uk_finance_shopping_feedback_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_finance_shopping_feedback_rebate_mode CHECK (rebate_calc_mode IN ('total', 'category')),
  CONSTRAINT chk_finance_shopping_feedback_source CHECK (feedback_source IN ('manual', 'excel', 'api')),
  CONSTRAINT chk_finance_shopping_feedback_status CHECK (status IN ('active', 'cancelled')),
  CONSTRAINT chk_finance_shopping_feedback_amount CHECK (
    people_count >= 0
    AND consumption_amount >= 0
    AND company_rebate_amount >= 0
    AND guide_commission_amount >= 0
    AND head_fee_amount >= 0
  )
);

DROP TRIGGER IF EXISTS trg_finance_shopping_feedback_lines_updated_at ON finance_shopping_feedback_lines;
CREATE TRIGGER trg_finance_shopping_feedback_lines_updated_at
BEFORE UPDATE ON finance_shopping_feedback_lines
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_finance_shopping_feedback_tenant_team
  ON finance_shopping_feedback_lines (tenant_id, is_deleted, team_id, status, business_date, id);

CREATE INDEX IF NOT EXISTS idx_finance_shopping_feedback_tenant_supplier_date
  ON finance_shopping_feedback_lines (tenant_id, is_deleted, supplier_id, business_date);

CREATE INDEX IF NOT EXISTS idx_finance_shopping_feedback_tenant_guide_date
  ON finance_shopping_feedback_lines (tenant_id, is_deleted, guide_id, business_date);

CREATE TABLE IF NOT EXISTS finance_shopping_feedback_detail_lines (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  feedback_line_id bigint NOT NULL,
  team_id bigint NOT NULL,
  category_name varchar(100) NOT NULL,
  people_count integer NOT NULL DEFAULT 0,
  head_fee_amount numeric(14,2) NOT NULL DEFAULT 0,
  consumption_amount numeric(14,2) NOT NULL DEFAULT 0,
  company_rebate_rate numeric(8,4) NOT NULL DEFAULT 0,
  company_rebate_amount numeric(14,2) NOT NULL DEFAULT 0,
  guide_commission_rate numeric(8,4) NOT NULL DEFAULT 0,
  guide_commission_amount numeric(14,2) NOT NULL DEFAULT 0,
  cash_amount numeric(14,2) NOT NULL DEFAULT 0,
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_finance_shopping_feedback_detail_parent
    FOREIGN KEY (tenant_id, feedback_line_id) REFERENCES finance_shopping_feedback_lines (tenant_id, id),
  CONSTRAINT fk_finance_shopping_feedback_detail_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT uk_finance_shopping_feedback_detail_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_finance_shopping_feedback_detail_amount CHECK (
    people_count >= 0
    AND head_fee_amount >= 0
    AND consumption_amount >= 0
    AND company_rebate_rate >= 0
    AND company_rebate_amount >= 0
    AND guide_commission_rate >= 0
    AND guide_commission_amount >= 0
    AND cash_amount >= 0
    AND sort_order >= 1
  )
);

DROP TRIGGER IF EXISTS trg_finance_shopping_feedback_detail_lines_updated_at ON finance_shopping_feedback_detail_lines;
CREATE TRIGGER trg_finance_shopping_feedback_detail_lines_updated_at
BEFORE UPDATE ON finance_shopping_feedback_detail_lines
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_finance_shopping_feedback_detail_tenant_feedback
  ON finance_shopping_feedback_detail_lines (tenant_id, is_deleted, feedback_line_id, sort_order, id);

CREATE INDEX IF NOT EXISTS idx_finance_shopping_feedback_detail_tenant_team_category
  ON finance_shopping_feedback_detail_lines (tenant_id, is_deleted, team_id, category_name);

CREATE TABLE IF NOT EXISTS finance_shopping_settlements (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_id bigint NOT NULL,
  team_no varchar(80) NOT NULL,
  team_type varchar(20),
  business_type varchar(120),
  departure_date date,
  rule_source varchar(30) NOT NULL,
  guest_count integer NOT NULL DEFAULT 0,
  threshold_per_capita_amount numeric(14,2) NOT NULL DEFAULT 5000,
  base_commission_rate numeric(8,2) NOT NULL DEFAULT 8,
  target_commission_rate numeric(8,2) NOT NULL DEFAULT 10,
  ladder_calc_mode varchar(30) NOT NULL DEFAULT 'full_amount_diff',
  total_consumption_amount numeric(14,2) NOT NULL DEFAULT 0,
  per_capita_consumption_amount numeric(14,2) NOT NULL DEFAULT 0,
  threshold_reached boolean NOT NULL DEFAULT false,
  base_guide_commission_amount numeric(14,2) NOT NULL DEFAULT 0,
  ladder_extra_commission_amount numeric(14,2) NOT NULL DEFAULT 0,
  guide_commission_total_amount numeric(14,2) NOT NULL DEFAULT 0,
  company_rebate_amount numeric(14,2) NOT NULL DEFAULT 0,
  head_fee_amount numeric(14,2) NOT NULL DEFAULT 0,
  internal_company_profit_amount numeric(14,2) NOT NULL DEFAULT 0,
  external_company_profit_amount numeric(14,2) NOT NULL DEFAULT 0,
  calculated_by varchar(80),
  calculated_at timestamptz,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  manual_guide_bonus_amount numeric(14,2) NOT NULL DEFAULT 0,
  manual_guide_bonus_remark text,
  CONSTRAINT fk_finance_shopping_settlements_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT uk_finance_shopping_settlements_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_finance_shopping_settlements_rule_source CHECK (
    rule_source IN ('system_default', 'default_rule', 'team_override')
  ),
  CONSTRAINT chk_finance_shopping_settlements_mode CHECK (ladder_calc_mode IN ('full_amount_diff')),
  CONSTRAINT chk_finance_shopping_settlements_status CHECK (status IN ('active', 'superseded', 'cancelled')),
  CONSTRAINT chk_finance_shopping_settlements_amount CHECK (
    guest_count >= 0
    AND threshold_per_capita_amount >= 0
    AND base_commission_rate >= 0
    AND target_commission_rate >= 0
    AND total_consumption_amount >= 0
    AND per_capita_consumption_amount >= 0
    AND base_guide_commission_amount >= 0
    AND ladder_extra_commission_amount >= 0
    AND guide_commission_total_amount >= 0
    AND manual_guide_bonus_amount >= 0
    AND company_rebate_amount >= 0
    AND head_fee_amount >= 0
  )
);

DROP TRIGGER IF EXISTS trg_finance_shopping_settlements_updated_at ON finance_shopping_settlements;
CREATE TRIGGER trg_finance_shopping_settlements_updated_at
BEFORE UPDATE ON finance_shopping_settlements
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_finance_shopping_settlements_tenant_team_active
  ON finance_shopping_settlements (tenant_id, team_id)
  WHERE is_deleted = false AND status = 'active';

CREATE INDEX IF NOT EXISTS idx_finance_shopping_settlements_tenant_departure
  ON finance_shopping_settlements (tenant_id, is_deleted, departure_date, status);

CREATE INDEX IF NOT EXISTS idx_finance_shopping_settlements_tenant_team
  ON finance_shopping_settlements (tenant_id, is_deleted, team_id, status, calculated_at DESC);

CREATE TABLE IF NOT EXISTS finance_shopping_settlement_lines (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  settlement_id bigint NOT NULL,
  team_id bigint NOT NULL,
  feedback_line_id bigint,
  supplier_id bigint,
  shop_name varchar(200) NOT NULL,
  business_date date,
  people_count integer NOT NULL DEFAULT 0,
  consumption_amount numeric(14,2) NOT NULL DEFAULT 0,
  company_rebate_amount numeric(14,2) NOT NULL DEFAULT 0,
  guide_commission_amount numeric(14,2) NOT NULL DEFAULT 0,
  head_fee_amount numeric(14,2) NOT NULL DEFAULT 0,
  line_company_profit_amount numeric(14,2) NOT NULL DEFAULT 0,
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_finance_shopping_settlement_lines_settlement
    FOREIGN KEY (tenant_id, settlement_id) REFERENCES finance_shopping_settlements (tenant_id, id),
  CONSTRAINT fk_finance_shopping_settlement_lines_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT uk_finance_shopping_settlement_lines_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_finance_shopping_settlement_lines_amount CHECK (
    people_count >= 0
    AND consumption_amount >= 0
    AND company_rebate_amount >= 0
    AND guide_commission_amount >= 0
    AND head_fee_amount >= 0
    AND sort_order >= 1
  )
);

DROP TRIGGER IF EXISTS trg_finance_shopping_settlement_lines_updated_at ON finance_shopping_settlement_lines;
CREATE TRIGGER trg_finance_shopping_settlement_lines_updated_at
BEFORE UPDATE ON finance_shopping_settlement_lines
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_finance_shopping_settlement_lines_tenant_settlement
  ON finance_shopping_settlement_lines (tenant_id, is_deleted, settlement_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_finance_shopping_settlement_lines_tenant_supplier_date
  ON finance_shopping_settlement_lines (tenant_id, is_deleted, supplier_id, business_date);

COMMENT ON TABLE finance_shopping_commission_rules IS '购物参考阶梯规则默认表。维护公司层面的综合人均消费门槛、基础比例和达标比例，用于补佣参考测算。';
COMMENT ON COLUMN finance_shopping_commission_rules.rule_name IS '规则名称。';
COMMENT ON COLUMN finance_shopping_commission_rules.threshold_per_capita_amount IS '人均消费门槛金额。';
COMMENT ON COLUMN finance_shopping_commission_rules.base_commission_rate IS '基础导游佣金比例，按百分数保存。';
COMMENT ON COLUMN finance_shopping_commission_rules.target_commission_rate IS '达标后目标导游佣金比例，按百分数保存。';
COMMENT ON COLUMN finance_shopping_commission_rules.ladder_calc_mode IS '阶梯补差方式。full_amount_diff表示达标后按全团消费全额补差。';
COMMENT ON COLUMN finance_shopping_commission_rules.effective_start_date IS '生效开始日期。';
COMMENT ON COLUMN finance_shopping_commission_rules.effective_end_date IS '生效结束日期。';
COMMENT ON COLUMN finance_shopping_commission_rules.status IS '规则状态。active启用，disabled停用。';

COMMENT ON TABLE finance_shopping_team_rule_overrides IS '团队购物参考阶梯规则覆盖表。保存计调按团队调整门槛和比例的快照。';
COMMENT ON COLUMN finance_shopping_team_rule_overrides.team_id IS '团队ID。';
COMMENT ON COLUMN finance_shopping_team_rule_overrides.team_no IS '团号快照。';
COMMENT ON COLUMN finance_shopping_team_rule_overrides.threshold_per_capita_amount IS '团队覆盖后的人均消费门槛金额。';
COMMENT ON COLUMN finance_shopping_team_rule_overrides.base_commission_rate IS '团队覆盖后的基础导游佣金比例。';
COMMENT ON COLUMN finance_shopping_team_rule_overrides.target_commission_rate IS '团队覆盖后的达标目标佣金比例。';
COMMENT ON COLUMN finance_shopping_team_rule_overrides.ladder_calc_mode IS '团队覆盖后的阶梯补差方式。';
COMMENT ON COLUMN finance_shopping_team_rule_overrides.override_reason IS '团队规则覆盖原因。';
COMMENT ON COLUMN finance_shopping_team_rule_overrides.overridden_by IS '规则覆盖操作人。';
COMMENT ON COLUMN finance_shopping_team_rule_overrides.overridden_at IS '规则覆盖时间。';
COMMENT ON COLUMN finance_shopping_team_rule_overrides.status IS '覆盖状态。active生效，superseded被新规则替代，cancelled作废。';

COMMENT ON TABLE finance_shopping_feedback_lines IS '购物店业绩反馈汇总表。保存单个购物店反馈的汇总金额，正式计算以消费详情汇总后的结果为准。';
COMMENT ON COLUMN finance_shopping_feedback_lines.team_id IS '团队ID。';
COMMENT ON COLUMN finance_shopping_feedback_lines.team_no IS '团号快照。';
COMMENT ON COLUMN finance_shopping_feedback_lines.supplier_id IS '购物店供应商ID。';
COMMENT ON COLUMN finance_shopping_feedback_lines.shop_name IS '购物店名称。';
COMMENT ON COLUMN finance_shopping_feedback_lines.guide_id IS '导游ID。';
COMMENT ON COLUMN finance_shopping_feedback_lines.guide_name IS '导游姓名快照。';
COMMENT ON COLUMN finance_shopping_feedback_lines.business_date IS '消费或反馈业务日期。';
COMMENT ON COLUMN finance_shopping_feedback_lines.people_count IS '进店人数。';
COMMENT ON COLUMN finance_shopping_feedback_lines.consumption_amount IS '消费总额。';
COMMENT ON COLUMN finance_shopping_feedback_lines.company_rebate_amount IS '公司返佣金额。';
COMMENT ON COLUMN finance_shopping_feedback_lines.guide_commission_amount IS '导游从购物店现场取得或应得的佣金金额，仅用于业务核对。';
COMMENT ON COLUMN finance_shopping_feedback_lines.head_fee_amount IS '人头费金额。';
COMMENT ON COLUMN finance_shopping_feedback_lines.rebate_calc_mode IS '返佣计算模式。total总额返佣，category按品类返佣。';
COMMENT ON COLUMN finance_shopping_feedback_lines.feedback_source IS '反馈来源。manual人工，excel导入，api接口。';
COMMENT ON COLUMN finance_shopping_feedback_lines.import_batch_id IS 'Excel导入批次ID。';
COMMENT ON COLUMN finance_shopping_feedback_lines.status IS '反馈状态。active生效，cancelled作废。';

COMMENT ON TABLE finance_shopping_feedback_detail_lines IS '购物反馈消费详情表。保存总额返佣的综合明细或按品类返佣的多行消费详情。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.feedback_line_id IS '购物反馈汇总记录ID。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.team_id IS '团队ID。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.category_name IS '购物品类。总额返佣模式固定为综合。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.people_count IS '当前品类进店人数，用于核对，不作为团队人均消费分母。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.head_fee_amount IS '人头费金额。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.consumption_amount IS '消费金额。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.company_rebate_rate IS '公司返佣比例，按百分数保存。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.company_rebate_amount IS '公司返佣金额。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.guide_commission_rate IS '导游现场提成比例，按百分数保存。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.guide_commission_amount IS '导游现场提成金额，仅用于业务核对。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.cash_amount IS '购物店现场现结金额，仅用于核对。';
COMMENT ON COLUMN finance_shopping_feedback_detail_lines.sort_order IS '排序号。';

COMMENT ON TABLE finance_shopping_settlements IS '购物业绩结算快照表。保存团队购物消费汇总、参考补佣测算、公司补佣和内外账公司利润。';
COMMENT ON COLUMN finance_shopping_settlements.team_id IS '团队ID。';
COMMENT ON COLUMN finance_shopping_settlements.team_no IS '团号快照。';
COMMENT ON COLUMN finance_shopping_settlements.team_type IS '团队类型快照。';
COMMENT ON COLUMN finance_shopping_settlements.business_type IS '业务类型快照。';
COMMENT ON COLUMN finance_shopping_settlements.departure_date IS '发团日期快照。';
COMMENT ON COLUMN finance_shopping_settlements.rule_source IS '规则来源。system_default系统默认，default_rule公司默认规则，team_override团队覆盖。';
COMMENT ON COLUMN finance_shopping_settlements.guest_count IS '团队实收人数。';
COMMENT ON COLUMN finance_shopping_settlements.threshold_per_capita_amount IS '计算时的人均消费门槛金额。';
COMMENT ON COLUMN finance_shopping_settlements.base_commission_rate IS '计算时的基础导游佣金比例。';
COMMENT ON COLUMN finance_shopping_settlements.target_commission_rate IS '计算时的达标目标佣金比例。';
COMMENT ON COLUMN finance_shopping_settlements.ladder_calc_mode IS '计算时的阶梯补差方式。';
COMMENT ON COLUMN finance_shopping_settlements.total_consumption_amount IS '全团购物消费总额。';
COMMENT ON COLUMN finance_shopping_settlements.per_capita_consumption_amount IS '按团队实收人数计算的人均购物消费。';
COMMENT ON COLUMN finance_shopping_settlements.threshold_reached IS '是否达到参考阶梯门槛。';
COMMENT ON COLUMN finance_shopping_settlements.base_guide_commission_amount IS '按基础比例测算的参考导游佣金金额。';
COMMENT ON COLUMN finance_shopping_settlements.ladder_extra_commission_amount IS '达标后的参考阶梯补差佣金，不自动计入正式成本。';
COMMENT ON COLUMN finance_shopping_settlements.guide_commission_total_amount IS '导游现场佣金加参考补差的展示合计，不自动计入公司成本。';
COMMENT ON COLUMN finance_shopping_settlements.manual_guide_bonus_amount IS '计调确认由公司补给导游的正式补佣金额，参与内部购物利润扣减。';
COMMENT ON COLUMN finance_shopping_settlements.manual_guide_bonus_remark IS '公司补佣说明。';
COMMENT ON COLUMN finance_shopping_settlements.company_rebate_amount IS '公司返佣合计。';
COMMENT ON COLUMN finance_shopping_settlements.head_fee_amount IS '人头费合计。';
COMMENT ON COLUMN finance_shopping_settlements.internal_company_profit_amount IS '内账公司购物利润，可用于真实毛利。';
COMMENT ON COLUMN finance_shopping_settlements.external_company_profit_amount IS '外账公司购物利润。无发票购物佣金默认不进入外账。';
COMMENT ON COLUMN finance_shopping_settlements.calculated_by IS '计算人。';
COMMENT ON COLUMN finance_shopping_settlements.calculated_at IS '计算时间。';
COMMENT ON COLUMN finance_shopping_settlements.status IS '结算状态。active当前有效，superseded被新计算替代，cancelled作废。';

COMMENT ON TABLE finance_shopping_settlement_lines IS '购物佣金结算明细表。按购物店反馈行保存结算快照明细。';
COMMENT ON COLUMN finance_shopping_settlement_lines.settlement_id IS '购物佣金结算快照ID。';
COMMENT ON COLUMN finance_shopping_settlement_lines.team_id IS '团队ID。';
COMMENT ON COLUMN finance_shopping_settlement_lines.feedback_line_id IS '购物反馈明细ID。';
COMMENT ON COLUMN finance_shopping_settlement_lines.supplier_id IS '购物店供应商ID。';
COMMENT ON COLUMN finance_shopping_settlement_lines.shop_name IS '购物店名称。';
COMMENT ON COLUMN finance_shopping_settlement_lines.business_date IS '消费日期。';
COMMENT ON COLUMN finance_shopping_settlement_lines.people_count IS '进店人数。';
COMMENT ON COLUMN finance_shopping_settlement_lines.consumption_amount IS '消费总额。';
COMMENT ON COLUMN finance_shopping_settlement_lines.company_rebate_amount IS '公司返佣金额。';
COMMENT ON COLUMN finance_shopping_settlement_lines.guide_commission_amount IS '导游从购物店现场取得或应得的佣金金额，仅用于业务核对。';
COMMENT ON COLUMN finance_shopping_settlement_lines.head_fee_amount IS '人头费金额。';
COMMENT ON COLUMN finance_shopping_settlement_lines.line_company_profit_amount IS '明细行公司购物利润，按人头费加公司返佣计算。';
COMMENT ON COLUMN finance_shopping_settlement_lines.sort_order IS '排序号。';

COMMIT;

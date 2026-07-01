-- 旅游接待管理系统：正式团队安排成本表
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 仅用于让本脚本可在独立临时库中校验。正式库中 tenants、sales_teams、sales_orders 已由前置脚本创建。
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS sales_teams (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_no varchar(80) NOT NULL,
  team_type varchar(20),
  is_deleted boolean NOT NULL DEFAULT false,
  CONSTRAINT uk_sales_teams_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS sales_orders (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_id bigint NOT NULL,
  order_no varchar(80) NOT NULL,
  customer_id bigint,
  customer_name varchar(200),
  guest_count integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'pending',
  is_deleted boolean NOT NULL DEFAULT false,
  CONSTRAINT uk_sales_orders_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS dispatch_team_arrangements (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_id bigint NOT NULL,
  team_no varchar(80) NOT NULL,
  team_type varchar(20),
  business_type varchar(120),
  department_id bigint,
  department_name varchar(160),
  operator_employee_id bigint,
  operator_employee_name varchar(100),
  arrangement_type varchar(30) NOT NULL,
  item_name varchar(160) NOT NULL,
  arrangement_content text,
  allocation_mode varchar(40) NOT NULL DEFAULT 'group_order_average',
  split_mode varchar(30),
  split_batch_no varchar(40),
  schedule_start_day varchar(40),
  schedule_end_day varchar(40),
  business_date date,
  departure_place varchar(120),
  arrival_place varchar(120),
  days_count integer NOT NULL DEFAULT 0,
  resource_name varchar(200),
  supplier_id bigint,
  supplier_name varchar(200),
  traffic_type varchar(40),
  vehicle_type varchar(40),
  driver_name varchar(100),
  vehicle_plate varchar(40),
  responsible_employee_id bigint,
  responsible_employee_name varchar(100),
  total_amount numeric(14,2) NOT NULL DEFAULT 0,
  cash_amount numeric(14,2) NOT NULL DEFAULT 0,
  credit_amount numeric(14,2) NOT NULL DEFAULT 0,
  prepaid_amount numeric(14,2) NOT NULL DEFAULT 0,
  sale_amount numeric(14,2) NOT NULL DEFAULT 0,
  cost_amount numeric(14,2) NOT NULL DEFAULT 0,
  guide_commission_amount numeric(14,2) NOT NULL DEFAULT 0,
  company_rebate_amount numeric(14,2) NOT NULL DEFAULT 0,
  head_fee_amount numeric(14,2) NOT NULL DEFAULT 0,
  consumption_amount numeric(14,2) NOT NULL DEFAULT 0,
  people_count numeric(12,2) NOT NULL DEFAULT 0,
  no_guide_report boolean NOT NULL DEFAULT false,
  guide_involved boolean NOT NULL DEFAULT true,
  cost_stage varchar(30) NOT NULL DEFAULT 'arrangement',
  guide_report_status varchar(20) NOT NULL DEFAULT 'pending',
  operator_audit_status varchar(20) NOT NULL DEFAULT 'pending',
  finance_audit_status varchar(20) NOT NULL DEFAULT 'pending',
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  settlement_type varchar(20) NOT NULL DEFAULT 'credit',
  meal_type varchar(40),
  fund_included varchar(40),
  confirmed boolean NOT NULL DEFAULT false,
  confirmation_no varchar(100),
  guide_id bigint,
  guide_name varchar(100),
  CONSTRAINT fk_dispatch_team_arrangements_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT uk_dispatch_team_arrangements_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_dispatch_team_arrangements_type CHECK (
    arrangement_type IN ('traffic', 'hotel', 'vehicle', 'scenic', 'meal', 'other', 'optional', 'shopping', 'ground_agent', 'extra_fee')
  ),
  CONSTRAINT chk_dispatch_team_arrangements_allocation CHECK (
    allocation_mode IN ('group_order_average', 'multi_order_average')
  ),
  CONSTRAINT chk_dispatch_team_arrangements_split CHECK (
    split_mode IS NULL OR split_mode IN ('by_order', 'by_people')
  ),
  CONSTRAINT chk_dispatch_team_arrangements_settlement CHECK (
    settlement_type IN ('cash', 'credit')
  ),
  CONSTRAINT chk_dispatch_team_arrangements_amount CHECK (
    days_count >= 0
    AND total_amount >= 0
    AND cash_amount >= 0
    AND credit_amount >= 0
    AND prepaid_amount >= 0
    AND sale_amount >= 0
    AND cost_amount >= 0
    AND guide_commission_amount >= 0
    AND company_rebate_amount >= 0
    AND head_fee_amount >= 0
    AND consumption_amount >= 0
    AND people_count >= 0
  ),
  CONSTRAINT chk_dispatch_team_arrangements_stage CHECK (
    cost_stage IN ('arrangement', 'guide', 'operator', 'finance')
  ),
  CONSTRAINT chk_dispatch_team_arrangements_report_status CHECK (
    guide_report_status IN ('pending', 'synced', 'submitted', 'approved', 'rejected', 'cancelled')
  ),
  CONSTRAINT chk_dispatch_team_arrangements_operator_status CHECK (
    operator_audit_status IN ('pending', 'synced', 'approved', 'rejected', 'cancelled')
  ),
  CONSTRAINT chk_dispatch_team_arrangements_finance_status CHECK (
    finance_audit_status IN ('pending', 'approved', 'rejected', 'cancelled')
  ),
  CONSTRAINT chk_dispatch_team_arrangements_status CHECK (status IN ('active', 'cancelled'))
);

DROP TRIGGER IF EXISTS trg_dispatch_team_arrangements_updated_at ON dispatch_team_arrangements;
CREATE TRIGGER trg_dispatch_team_arrangements_updated_at
BEFORE UPDATE ON dispatch_team_arrangements
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangements_tenant_team_type
  ON dispatch_team_arrangements (tenant_id, is_deleted, team_id, arrangement_type, business_date);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangements_tenant_supplier_date
  ON dispatch_team_arrangements (tenant_id, is_deleted, supplier_id, business_date);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangements_tenant_operator_date
  ON dispatch_team_arrangements (tenant_id, is_deleted, operator_employee_id, business_date);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangements_tenant_audit
  ON dispatch_team_arrangements (tenant_id, is_deleted, cost_stage, guide_report_status, operator_audit_status, finance_audit_status);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangements_tenant_split
  ON dispatch_team_arrangements (tenant_id, is_deleted, split_batch_no)
  WHERE split_batch_no IS NOT NULL;

CREATE TABLE IF NOT EXISTS dispatch_team_arrangement_price_lines (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  arrangement_id bigint NOT NULL,
  team_id bigint NOT NULL,
  project_id bigint,
  project_name varchar(120),
  unit_price numeric(14,2) NOT NULL DEFAULT 0,
  quantity numeric(12,2) NOT NULL DEFAULT 0,
  amount numeric(14,2) NOT NULL DEFAULT 0,
  sale_price numeric(14,2) NOT NULL DEFAULT 0,
  cost_price numeric(14,2) NOT NULL DEFAULT 0,
  cash_amount numeric(14,2) NOT NULL DEFAULT 0,
  credit_amount numeric(14,2) NOT NULL DEFAULT 0,
  guide_commission_amount numeric(14,2) NOT NULL DEFAULT 0,
  guide_commission_rate numeric(8,4) NOT NULL DEFAULT 0,
  company_rebate_amount numeric(14,2) NOT NULL DEFAULT 0,
  company_rebate_rate numeric(8,4) NOT NULL DEFAULT 0,
  head_fee_amount numeric(14,2) NOT NULL DEFAULT 0,
  consumption_amount numeric(14,2) NOT NULL DEFAULT 0,
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_dispatch_team_arrangement_price_arrangement
    FOREIGN KEY (tenant_id, arrangement_id) REFERENCES dispatch_team_arrangements (tenant_id, id),
  CONSTRAINT fk_dispatch_team_arrangement_price_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT chk_dispatch_team_arrangement_price_amount CHECK (
    unit_price >= 0
    AND quantity >= 0
    AND amount >= 0
    AND sale_price >= 0
    AND cost_price >= 0
    AND cash_amount >= 0
    AND credit_amount >= 0
    AND guide_commission_amount >= 0
    AND guide_commission_rate >= 0
    AND company_rebate_amount >= 0
    AND company_rebate_rate >= 0
    AND head_fee_amount >= 0
    AND consumption_amount >= 0
    AND sort_order >= 1
  )
);

DROP TRIGGER IF EXISTS trg_dispatch_team_arrangement_price_lines_updated_at ON dispatch_team_arrangement_price_lines;
CREATE TRIGGER trg_dispatch_team_arrangement_price_lines_updated_at
BEFORE UPDATE ON dispatch_team_arrangement_price_lines
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangement_price_tenant_arrangement
  ON dispatch_team_arrangement_price_lines (tenant_id, is_deleted, arrangement_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangement_price_tenant_team
  ON dispatch_team_arrangement_price_lines (tenant_id, is_deleted, team_id);

CREATE TABLE IF NOT EXISTS dispatch_team_arrangement_order_allocations (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  arrangement_id bigint NOT NULL,
  team_id bigint NOT NULL,
  team_no varchar(80) NOT NULL,
  allocation_scope varchar(20) NOT NULL DEFAULT 'team',
  order_id bigint,
  order_no varchar(80),
  customer_id bigint,
  customer_name varchar(200),
  guest_count integer NOT NULL DEFAULT 0,
  allocation_mode varchar(40) NOT NULL DEFAULT 'group_order_average',
  split_mode varchar(30),
  split_batch_no varchar(40),
  original_amount numeric(14,2) NOT NULL DEFAULT 0,
  allocation_amount numeric(14,2) NOT NULL DEFAULT 0,
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_dispatch_team_arrangement_alloc_arrangement
    FOREIGN KEY (tenant_id, arrangement_id) REFERENCES dispatch_team_arrangements (tenant_id, id),
  CONSTRAINT fk_dispatch_team_arrangement_alloc_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT fk_dispatch_team_arrangement_alloc_order
    FOREIGN KEY (tenant_id, order_id) REFERENCES sales_orders (tenant_id, id),
  CONSTRAINT chk_dispatch_team_arrangement_alloc_scope CHECK (allocation_scope IN ('team', 'order')),
  CONSTRAINT chk_dispatch_team_arrangement_alloc_mode CHECK (
    allocation_mode IN ('group_order_average', 'multi_order_average')
  ),
  CONSTRAINT chk_dispatch_team_arrangement_alloc_split CHECK (
    split_mode IS NULL OR split_mode IN ('by_order', 'by_people')
  ),
  CONSTRAINT chk_dispatch_team_arrangement_alloc_amount CHECK (
    guest_count >= 0 AND original_amount >= 0 AND allocation_amount >= 0 AND sort_order >= 1
  )
);

DROP TRIGGER IF EXISTS trg_dispatch_team_arrangement_order_allocations_updated_at ON dispatch_team_arrangement_order_allocations;
CREATE TRIGGER trg_dispatch_team_arrangement_order_allocations_updated_at
BEFORE UPDATE ON dispatch_team_arrangement_order_allocations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangement_alloc_tenant_arrangement
  ON dispatch_team_arrangement_order_allocations (tenant_id, is_deleted, arrangement_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangement_alloc_tenant_team
  ON dispatch_team_arrangement_order_allocations (tenant_id, is_deleted, team_id);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangement_alloc_tenant_order
  ON dispatch_team_arrangement_order_allocations (tenant_id, is_deleted, order_id);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangement_alloc_tenant_split
  ON dispatch_team_arrangement_order_allocations (tenant_id, is_deleted, split_batch_no)
  WHERE split_batch_no IS NOT NULL;

CREATE TABLE IF NOT EXISTS dispatch_team_arrangement_flow_records (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  arrangement_id bigint NOT NULL,
  team_id bigint NOT NULL,
  team_no varchar(80) NOT NULL,
  flow_type varchar(30) NOT NULL,
  sync_source varchar(40) NOT NULL,
  flow_status varchar(20) NOT NULL DEFAULT 'synced',
  flow_amount numeric(14,2) NOT NULL DEFAULT 0,
  registered_by varchar(80),
  registered_at timestamptz NOT NULL DEFAULT now(),
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_dispatch_team_arrangement_flow_arrangement
    FOREIGN KEY (tenant_id, arrangement_id) REFERENCES dispatch_team_arrangements (tenant_id, id),
  CONSTRAINT fk_dispatch_team_arrangement_flow_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT chk_dispatch_team_arrangement_flow_type CHECK (flow_type IN ('guide_report', 'operator_audit', 'finance_audit')),
  CONSTRAINT chk_dispatch_team_arrangement_flow_source CHECK (sync_source IN ('no_guide_report', 'manual', 'guide_submit', 'audit')),
  CONSTRAINT chk_dispatch_team_arrangement_flow_status CHECK (
    flow_status IN ('pending', 'synced', 'submitted', 'approved', 'rejected', 'cancelled')
  ),
  CONSTRAINT chk_dispatch_team_arrangement_flow_amount CHECK (flow_amount >= 0)
);

DROP TRIGGER IF EXISTS trg_dispatch_team_arrangement_flow_records_updated_at ON dispatch_team_arrangement_flow_records;
CREATE TRIGGER trg_dispatch_team_arrangement_flow_records_updated_at
BEFORE UPDATE ON dispatch_team_arrangement_flow_records
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangement_flow_tenant_arrangement
  ON dispatch_team_arrangement_flow_records (tenant_id, is_deleted, arrangement_id, flow_type);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangement_flow_tenant_team
  ON dispatch_team_arrangement_flow_records (tenant_id, is_deleted, team_id, flow_type, flow_status);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_arrangement_flow_tenant_status
  ON dispatch_team_arrangement_flow_records (tenant_id, is_deleted, flow_type, flow_status, registered_at DESC);

COMMENT ON TABLE dispatch_team_arrangements IS '正式团队安排成本主表。保存团队实际执行阶段的大交通、住宿、用车、景区、用餐等资源安排和成本。';
COMMENT ON COLUMN dispatch_team_arrangements.id IS '团队安排成本主键 ID。';
COMMENT ON COLUMN dispatch_team_arrangements.tenant_id IS '租户 ID，用于隔离不同地接公司的团队安排成本数据。';
COMMENT ON COLUMN dispatch_team_arrangements.team_id IS '所属团队 ID。';
COMMENT ON COLUMN dispatch_team_arrangements.team_no IS '团队编号快照，用于成本、应付和统计展示。';
COMMENT ON COLUMN dispatch_team_arrangements.team_type IS '团队类型快照。';
COMMENT ON COLUMN dispatch_team_arrangements.business_type IS '团队业务类型快照。';
COMMENT ON COLUMN dispatch_team_arrangements.department_id IS '团队归属部门 ID 快照。';
COMMENT ON COLUMN dispatch_team_arrangements.department_name IS '团队归属部门名称快照。';
COMMENT ON COLUMN dispatch_team_arrangements.operator_employee_id IS '操作计调员工 ID 快照。';
COMMENT ON COLUMN dispatch_team_arrangements.operator_employee_name IS '操作计调姓名快照。';
COMMENT ON COLUMN dispatch_team_arrangements.arrangement_type IS '安排资源类型。traffic 大交通，hotel 住宿，vehicle 用车，scenic 景区，meal 用餐，other 其它，optional 自费，shopping 购物，ground_agent 地接，extra_fee 附加。';
COMMENT ON COLUMN dispatch_team_arrangements.item_name IS '安排名称或费用摘要。';
COMMENT ON COLUMN dispatch_team_arrangements.arrangement_content IS '安排内容摘要。';
COMMENT ON COLUMN dispatch_team_arrangements.allocation_mode IS '成本归属模式。group_order_average 全团或单订单归属，multi_order_average 多订单均摊录入来源。';
COMMENT ON COLUMN dispatch_team_arrangements.split_mode IS '多订单均摊方式。by_order 按订单均摊，by_people 按人数均摊。';
COMMENT ON COLUMN dispatch_team_arrangements.split_batch_no IS '多订单均摊拆分批次号，用于追溯同一次录入拆出的多条成本。';
COMMENT ON COLUMN dispatch_team_arrangements.schedule_start_day IS '开始或使用日期文本，例如第1天。';
COMMENT ON COLUMN dispatch_team_arrangements.schedule_end_day IS '结束日期文本。';
COMMENT ON COLUMN dispatch_team_arrangements.business_date IS '业务发生日期，用于财务和统计按日期查询。';
COMMENT ON COLUMN dispatch_team_arrangements.departure_place IS '出发地。';
COMMENT ON COLUMN dispatch_team_arrangements.arrival_place IS '抵达地。';
COMMENT ON COLUMN dispatch_team_arrangements.days_count IS '天数、晚数或使用天数。';
COMMENT ON COLUMN dispatch_team_arrangements.resource_name IS '资源名称，例如酒店、景区、餐厅、购物店。';
COMMENT ON COLUMN dispatch_team_arrangements.supplier_id IS '供应商 ID。';
COMMENT ON COLUMN dispatch_team_arrangements.supplier_name IS '供应商名称快照。';
COMMENT ON COLUMN dispatch_team_arrangements.traffic_type IS '交通类型，例如飞机、高铁、火车、邮轮。';
COMMENT ON COLUMN dispatch_team_arrangements.vehicle_type IS '车型。';
COMMENT ON COLUMN dispatch_team_arrangements.driver_name IS '司机信息。';
COMMENT ON COLUMN dispatch_team_arrangements.vehicle_plate IS '车牌号。';
COMMENT ON COLUMN dispatch_team_arrangements.responsible_employee_id IS '责任员工 ID。';
COMMENT ON COLUMN dispatch_team_arrangements.responsible_employee_name IS '责任员工名称快照。';
COMMENT ON COLUMN dispatch_team_arrangements.settlement_type IS '默认结算类型。cash 现结，credit 挂账，用于从产品模板或团队安排表单回显用户选择。';
COMMENT ON COLUMN dispatch_team_arrangements.meal_type IS '餐型、用餐时间或酒店早餐类型。';
COMMENT ON COLUMN dispatch_team_arrangements.fund_included IS '基金或附加项目是否包含的文本快照。';
COMMENT ON COLUMN dispatch_team_arrangements.confirmed IS '资源安排是否已确认，例如酒店订房确认。';
COMMENT ON COLUMN dispatch_team_arrangements.confirmation_no IS '资源确认号或供应商确认编号。';
COMMENT ON COLUMN dispatch_team_arrangements.guide_id IS '关联导游 ID。';
COMMENT ON COLUMN dispatch_team_arrangements.guide_name IS '关联导游姓名快照。';
COMMENT ON COLUMN dispatch_team_arrangements.total_amount IS '安排合计金额。';
COMMENT ON COLUMN dispatch_team_arrangements.cash_amount IS '现结金额。现结表示现场付款，需要进入导游报账或付款来源说明。';
COMMENT ON COLUMN dispatch_team_arrangements.credit_amount IS '挂账金额。挂账金额进入供应商应付口径。';
COMMENT ON COLUMN dispatch_team_arrangements.prepaid_amount IS '预付款金额。';
COMMENT ON COLUMN dispatch_team_arrangements.sale_amount IS '收入金额，自费和购物等混合业务使用。';
COMMENT ON COLUMN dispatch_team_arrangements.cost_amount IS '成本金额。';
COMMENT ON COLUMN dispatch_team_arrangements.guide_commission_amount IS '导游提成金额。';
COMMENT ON COLUMN dispatch_team_arrangements.company_rebate_amount IS '公司返佣金额。';
COMMENT ON COLUMN dispatch_team_arrangements.head_fee_amount IS '人头费金额。';
COMMENT ON COLUMN dispatch_team_arrangements.consumption_amount IS '消费金额。';
COMMENT ON COLUMN dispatch_team_arrangements.people_count IS '人数。';
COMMENT ON COLUMN dispatch_team_arrangements.no_guide_report IS '是否无需导游报账。true 表示保存时自动同步导游报账和计调审核流水。';
COMMENT ON COLUMN dispatch_team_arrangements.guide_involved IS '导游是否参与报账。';
COMMENT ON COLUMN dispatch_team_arrangements.cost_stage IS '成本阶段。arrangement 排团，guide 导游报账，operator 计调确认，finance 财务审核。';
COMMENT ON COLUMN dispatch_team_arrangements.guide_report_status IS '导游报账状态。';
COMMENT ON COLUMN dispatch_team_arrangements.operator_audit_status IS '计调审核状态。';
COMMENT ON COLUMN dispatch_team_arrangements.finance_audit_status IS '财务审核状态。';
COMMENT ON COLUMN dispatch_team_arrangements.status IS '安排状态。active 生效，cancelled 已取消。';
COMMENT ON COLUMN dispatch_team_arrangements.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN dispatch_team_arrangements.remark IS '备注。';
COMMENT ON COLUMN dispatch_team_arrangements.created_at IS '创建时间。';
COMMENT ON COLUMN dispatch_team_arrangements.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN dispatch_team_arrangements.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN dispatch_team_arrangements.deleted_at IS '软删除时间。';
COMMENT ON COLUMN dispatch_team_arrangements.deleted_by IS '软删除操作人。';

COMMENT ON TABLE dispatch_team_arrangement_price_lines IS '正式团队安排价格明细表。保存一条安排下的费用项目、单价、数量、小计、现结、挂账和提成返佣字段。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.id IS '团队安排价格明细主键 ID。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.tenant_id IS '租户 ID，用于隔离不同地接公司的团队安排价格明细。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.arrangement_id IS '所属团队安排成本 ID。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.team_id IS '所属团队 ID，用于团队成本汇总和索引查询。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.project_id IS '费用项目 ID。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.project_name IS '费用项目名称快照，例如成人机票、标间、门票。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.unit_price IS '单价。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.quantity IS '数量。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.amount IS '小计金额，通常为单价乘数量。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.sale_price IS '销售价，自费和购物等混合收入成本场景使用。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.cost_price IS '成本价，自费和购物等混合收入成本场景使用。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.cash_amount IS '本价格明细现结金额。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.credit_amount IS '本价格明细挂账金额。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.guide_commission_amount IS '导游提成金额。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.guide_commission_rate IS '导游提成比例。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.company_rebate_amount IS '公司返佣金额。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.company_rebate_rate IS '公司返佣比例。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.head_fee_amount IS '人头费金额。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.consumption_amount IS '消费金额。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.sort_order IS '明细排序号。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.remark IS '备注。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.created_at IS '创建时间。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.deleted_at IS '软删除时间。';
COMMENT ON COLUMN dispatch_team_arrangement_price_lines.deleted_by IS '软删除操作人。';

COMMENT ON TABLE dispatch_team_arrangement_order_allocations IS '正式团队安排订单归属表。保存团队公共成本、单订单成本和多订单均摊拆分结果。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.id IS '团队安排订单归属主键 ID。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.tenant_id IS '租户 ID，用于隔离不同地接公司的团队安排归属数据。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.arrangement_id IS '所属团队安排成本 ID。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.team_id IS '所属团队 ID。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.team_no IS '团队编号快照，用于成本归属和财务展示。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.allocation_scope IS '归属范围。team 表示团队公共成本，order 表示归属到具体订单。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.order_id IS '归属订单 ID。团队公共成本为空。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.order_no IS '归属订单编号快照。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.customer_id IS '归属订单客户单位 ID 快照。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.customer_name IS '归属订单客户单位名称快照。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.guest_count IS '归属订单人数快照，用于按人数均摊追溯。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.allocation_mode IS '成本归属模式。group_order_average 全团或单订单归属，multi_order_average 多订单均摊拆分来源。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.split_mode IS '多订单均摊方式。by_order 按订单均摊，by_people 按人数均摊。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.split_batch_no IS '多订单均摊拆分批次号，用于追溯同一次录入拆出的成本。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.original_amount IS '原始录入总金额。多订单均摊时为拆分前总金额。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.allocation_amount IS '本归属行承担金额。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.sort_order IS '归属行排序号。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.remark IS '备注。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.created_at IS '创建时间。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.deleted_at IS '软删除时间。';
COMMENT ON COLUMN dispatch_team_arrangement_order_allocations.deleted_by IS '软删除操作人。';

COMMENT ON TABLE dispatch_team_arrangement_flow_records IS '正式团队安排流程流水表。保存导游报账、计调审核等下游流程同步记录。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.id IS '团队安排流程流水主键 ID。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.tenant_id IS '租户 ID，用于隔离不同地接公司的流程流水。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.arrangement_id IS '所属团队安排成本 ID。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.team_id IS '所属团队 ID。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.team_no IS '团队编号快照。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.flow_type IS '流程类型。guide_report 导游报账，operator_audit 计调审核，finance_audit 财务审核。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.sync_source IS '流水来源。no_guide_report 无需导游报账自动同步，manual 手工登记，guide_submit 导游提交，audit 审核产生。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.flow_status IS '流程状态。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.flow_amount IS '流程涉及金额。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.registered_by IS '登记人或同步操作人。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.registered_at IS '登记或同步时间。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.remark IS '备注。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.created_at IS '创建时间。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.deleted_at IS '软删除时间。';
COMMENT ON COLUMN dispatch_team_arrangement_flow_records.deleted_by IS '软删除操作人。';

COMMIT;

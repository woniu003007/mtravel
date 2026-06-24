-- 旅游接待管理系统：销售团期与团队价格表
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 仅用于让本脚本可在独立临时库中校验。正式库中 tenants 和 sales_products 已由前置脚本创建。
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS sales_products (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  CONSTRAINT uk_sales_products_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS sales_teams (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  team_no varchar(80) NOT NULL,
  team_type varchar(20) NOT NULL DEFAULT 'sanpin',
  departure_date date NOT NULL,
  operator_employee_id bigint,
  operator_employee_name varchar(100),
  status varchar(20) NOT NULL DEFAULT 'normal',
  total_seats integer NOT NULL DEFAULT 0,
  used_seats integer NOT NULL DEFAULT 0,
  remaining_seats integer NOT NULL DEFAULT 0,
  single_room_difference numeric(12,2) NOT NULL DEFAULT 0,
  close_days_before integer NOT NULL DEFAULT 0,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  business_type varchar(120),
  department_id bigint,
  department_name varchar(160),
  escort_employee_id bigint,
  escort_employee_name varchar(100),
  CONSTRAINT fk_sales_teams_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT uk_sales_teams_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_sales_teams_type CHECK (team_type IN ('sanpin', 'zhengtuan', 'santuan', 'single')),
  CONSTRAINT chk_sales_teams_status CHECK (status IN ('normal', 'stopped', 'cancelled')),
  CONSTRAINT chk_sales_teams_total_seats CHECK (total_seats >= 0),
  CONSTRAINT chk_sales_teams_used_seats CHECK (used_seats >= 0),
  CONSTRAINT chk_sales_teams_remaining_seats CHECK (remaining_seats >= 0),
  CONSTRAINT chk_sales_teams_single_room CHECK (single_room_difference >= 0),
  CONSTRAINT chk_sales_teams_close_days CHECK (close_days_before >= 0)
);

DROP TRIGGER IF EXISTS trg_sales_teams_updated_at ON sales_teams;
CREATE TRIGGER trg_sales_teams_updated_at
BEFORE UPDATE ON sales_teams
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_teams_tenant_team_no_active
  ON sales_teams (tenant_id, team_no)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_sales_teams_tenant_deleted_product
  ON sales_teams (tenant_id, is_deleted, product_id, departure_date);

CREATE INDEX IF NOT EXISTS idx_sales_teams_tenant_deleted_status
  ON sales_teams (tenant_id, is_deleted, status, departure_date);

CREATE INDEX IF NOT EXISTS idx_sales_teams_tenant_deleted_operator
  ON sales_teams (tenant_id, is_deleted, operator_employee_id, departure_date);

CREATE INDEX IF NOT EXISTS idx_sales_teams_tenant_deleted_department
  ON sales_teams (tenant_id, is_deleted, department_id, departure_date);

CREATE INDEX IF NOT EXISTS idx_sales_teams_tenant_deleted_business
  ON sales_teams (tenant_id, is_deleted, business_type, departure_date);

CREATE TABLE IF NOT EXISTS sales_team_prices (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_id bigint NOT NULL,
  product_id bigint NOT NULL,
  customer_category_id bigint,
  customer_category_name varchar(120) NOT NULL DEFAULT '默认',
  adult_price numeric(12,2) NOT NULL DEFAULT 0,
  child_price numeric(12,2) NOT NULL DEFAULT 0,
  child_no_bed_price numeric(12,2) NOT NULL DEFAULT 0,
  senior_price numeric(12,2) NOT NULL DEFAULT 0,
  extra_fee numeric(12,2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_team_prices_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT fk_sales_team_prices_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT chk_sales_team_prices_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT chk_sales_team_prices_adult CHECK (adult_price >= 0),
  CONSTRAINT chk_sales_team_prices_child CHECK (child_price >= 0),
  CONSTRAINT chk_sales_team_prices_child_no_bed CHECK (child_no_bed_price >= 0),
  CONSTRAINT chk_sales_team_prices_senior CHECK (senior_price >= 0),
  CONSTRAINT chk_sales_team_prices_extra CHECK (extra_fee >= 0)
);

DROP TRIGGER IF EXISTS trg_sales_team_prices_updated_at ON sales_team_prices;
CREATE TRIGGER trg_sales_team_prices_updated_at
BEFORE UPDATE ON sales_team_prices
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_team_prices_category_id_active
  ON sales_team_prices (tenant_id, team_id, customer_category_id)
  WHERE is_deleted = false AND customer_category_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_team_prices_category_name_active
  ON sales_team_prices (tenant_id, team_id, customer_category_name)
  WHERE is_deleted = false AND customer_category_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_sales_team_prices_tenant_deleted_team
  ON sales_team_prices (tenant_id, is_deleted, team_id);

CREATE INDEX IF NOT EXISTS idx_sales_team_prices_tenant_deleted_product
  ON sales_team_prices (tenant_id, is_deleted, product_id);

CREATE TABLE IF NOT EXISTS sales_team_status_logs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_id bigint NOT NULL,
  from_status varchar(20),
  to_status varchar(20) NOT NULL,
  action_type varchar(40) NOT NULL,
  operator varchar(80),
  action_time timestamptz NOT NULL DEFAULT now(),
  remark text,
  CONSTRAINT fk_sales_team_status_logs_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT chk_sales_team_status_logs_from CHECK (from_status IS NULL OR from_status IN ('normal', 'stopped', 'cancelled')),
  CONSTRAINT chk_sales_team_status_logs_to CHECK (to_status IN ('normal', 'stopped', 'cancelled')),
  CONSTRAINT chk_sales_team_status_logs_action CHECK (
    action_type IN ('create', 'stop', 'start', 'cancel', 'recover', 'delete')
  )
);

CREATE INDEX IF NOT EXISTS idx_sales_team_status_logs_tenant_team
  ON sales_team_status_logs (tenant_id, team_id, action_time DESC);

CREATE TABLE IF NOT EXISTS sales_team_no_logs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  departure_date date NOT NULL,
  team_no varchar(80) NOT NULL,
  suffix_code varchar(10) NOT NULL,
  operator varchar(80),
  created_at timestamptz NOT NULL DEFAULT now(),
  remark text,
  CONSTRAINT fk_sales_team_no_logs_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id)
);

CREATE INDEX IF NOT EXISTS idx_sales_team_no_logs_tenant_product_date
  ON sales_team_no_logs (tenant_id, product_id, departure_date, created_at DESC);

COMMENT ON TABLE sales_teams IS '销售团队团期主表，保存由产品模板生成的正式团队记录。';
COMMENT ON COLUMN sales_teams.id IS '销售团队主键 ID。';
COMMENT ON COLUMN sales_teams.tenant_id IS '租户 ID，用于隔离不同地接公司的团队数据。';
COMMENT ON COLUMN sales_teams.product_id IS '所属销售产品模板 ID。';
COMMENT ON COLUMN sales_teams.team_no IS '团队编号，按产品、团队类型和发团日期生成。';
COMMENT ON COLUMN sales_teams.team_type IS '团队类型：散拼、整团、散团或单项。团期管理默认生成散拼团队。';
COMMENT ON COLUMN sales_teams.business_type IS '团队业务类型快照。默认从产品带入，正式团队可单独调整。';
COMMENT ON COLUMN sales_teams.departure_date IS '发团日期。';
COMMENT ON COLUMN sales_teams.department_id IS '团队归属部门 ID。';
COMMENT ON COLUMN sales_teams.department_name IS '团队归属部门名称快照。';
COMMENT ON COLUMN sales_teams.operator_employee_id IS '操作计调员工 ID。';
COMMENT ON COLUMN sales_teams.operator_employee_name IS '操作计调员工姓名快照。';
COMMENT ON COLUMN sales_teams.escort_employee_id IS '全陪员工 ID。';
COMMENT ON COLUMN sales_teams.escort_employee_name IS '全陪员工姓名快照。';
COMMENT ON COLUMN sales_teams.status IS '团队状态：正常、停收或取消。';
COMMENT ON COLUMN sales_teams.total_seats IS '总位数。';
COMMENT ON COLUMN sales_teams.used_seats IS '已收客或已占用位数。';
COMMENT ON COLUMN sales_teams.remaining_seats IS '剩余位数。';
COMMENT ON COLUMN sales_teams.single_room_difference IS '单人房差价格。';
COMMENT ON COLUMN sales_teams.close_days_before IS '出团前截止收客天数。';
COMMENT ON COLUMN sales_teams.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_teams.remark IS '团队备注。';
COMMENT ON COLUMN sales_teams.created_at IS '创建时间。';
COMMENT ON COLUMN sales_teams.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_teams.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN sales_teams.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_teams.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_team_prices IS '销售团队客户类型价格表，保存同一团队下不同客户类型的成人、儿童、老人和附加费价格。';
COMMENT ON COLUMN sales_team_prices.id IS '团队价格主键 ID。';
COMMENT ON COLUMN sales_team_prices.tenant_id IS '租户 ID，用于隔离不同地接公司的团队价格。';
COMMENT ON COLUMN sales_team_prices.team_id IS '所属销售团队 ID。';
COMMENT ON COLUMN sales_team_prices.product_id IS '所属销售产品模板 ID，便于按产品快速查询价格行。';
COMMENT ON COLUMN sales_team_prices.customer_category_id IS '客户分类 ID。为空时表示默认客户类型。';
COMMENT ON COLUMN sales_team_prices.customer_category_name IS '客户类型名称快照。';
COMMENT ON COLUMN sales_team_prices.adult_price IS '成人价格。';
COMMENT ON COLUMN sales_team_prices.child_price IS '儿童价格。';
COMMENT ON COLUMN sales_team_prices.child_no_bed_price IS '儿童不占床价格。';
COMMENT ON COLUMN sales_team_prices.senior_price IS '老人价格。';
COMMENT ON COLUMN sales_team_prices.extra_fee IS '附加费用。';
COMMENT ON COLUMN sales_team_prices.status IS '价格状态：启用或停用。';
COMMENT ON COLUMN sales_team_prices.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_team_prices.remark IS '价格备注。';
COMMENT ON COLUMN sales_team_prices.created_at IS '创建时间。';
COMMENT ON COLUMN sales_team_prices.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_team_prices.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN sales_team_prices.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_team_prices.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_team_status_logs IS '销售团队状态日志，记录团期创建、停收、恢复、取消和删除动作。';
COMMENT ON COLUMN sales_team_status_logs.id IS '状态日志主键 ID。';
COMMENT ON COLUMN sales_team_status_logs.tenant_id IS '租户 ID。';
COMMENT ON COLUMN sales_team_status_logs.team_id IS '所属销售团队 ID。';
COMMENT ON COLUMN sales_team_status_logs.from_status IS '变更前状态。';
COMMENT ON COLUMN sales_team_status_logs.to_status IS '变更后状态。';
COMMENT ON COLUMN sales_team_status_logs.action_type IS '状态动作类型。';
COMMENT ON COLUMN sales_team_status_logs.operator IS '操作人账号或名称。';
COMMENT ON COLUMN sales_team_status_logs.action_time IS '操作时间。';
COMMENT ON COLUMN sales_team_status_logs.remark IS '状态变更说明。';

COMMENT ON TABLE sales_team_no_logs IS '销售团队编号生成日志，记录每次生成团号时使用的日期和后缀。';
COMMENT ON COLUMN sales_team_no_logs.id IS '团号日志主键 ID。';
COMMENT ON COLUMN sales_team_no_logs.tenant_id IS '租户 ID。';
COMMENT ON COLUMN sales_team_no_logs.product_id IS '所属销售产品模板 ID。';
COMMENT ON COLUMN sales_team_no_logs.departure_date IS '发团日期。';
COMMENT ON COLUMN sales_team_no_logs.team_no IS '生成的团队编号。';
COMMENT ON COLUMN sales_team_no_logs.suffix_code IS '生成团号时使用的后缀。';
COMMENT ON COLUMN sales_team_no_logs.operator IS '操作人账号或名称。';
COMMENT ON COLUMN sales_team_no_logs.created_at IS '生成时间。';
COMMENT ON COLUMN sales_team_no_logs.remark IS '团号生成说明。';

COMMIT;

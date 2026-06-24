-- 旅游接待管理系统：销售收客订单主链路表
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 仅用于让本脚本可在独立临时库中校验。正式库中 tenants、customers、sales_teams 已由前置脚本创建。
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS customers (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  customer_name varchar(200) NOT NULL,
  CONSTRAINT uk_customers_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS sales_teams (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_no varchar(80) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'normal',
  total_seats integer NOT NULL DEFAULT 0,
  used_seats integer NOT NULL DEFAULT 0,
  remaining_seats integer NOT NULL DEFAULT 0,
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
  contact_name varchar(80),
  contact_phone varchar(40),
  customer_team_no varchar(120),
  source_province varchar(80),
  source_city varchar(80),
  source_district varchar(80),
  travel_description text,
  pickup_info text,
  dropoff_info text,
  pickup_remark text,
  guide_name varchar(80),
  guide_phone varchar(40),
  guide_remark text,
  hotel_info text,
  adult_count integer NOT NULL DEFAULT 0,
  child_count integer NOT NULL DEFAULT 0,
  child_no_bed_count integer NOT NULL DEFAULT 0,
  senior_count integer NOT NULL DEFAULT 0,
  escort_count integer NOT NULL DEFAULT 0,
  guest_count integer NOT NULL DEFAULT 0,
  receivable_amount numeric(14,2) NOT NULL DEFAULT 0,
  received_amount numeric(14,2) NOT NULL DEFAULT 0,
  balance_amount numeric(14,2) NOT NULL DEFAULT 0,
  fee_remark text,
  confirm_remark text,
  order_remark text,
  status varchar(20) NOT NULL DEFAULT 'pending',
  booked_by varchar(80),
  booked_at timestamptz NOT NULL DEFAULT now(),
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_orders_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT fk_sales_orders_customer
    FOREIGN KEY (tenant_id, customer_id) REFERENCES customers (tenant_id, id),
  CONSTRAINT uk_sales_orders_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_sales_orders_status CHECK (status IN ('pending', 'confirmed', 'cancelled')),
  CONSTRAINT chk_sales_orders_counts CHECK (
    adult_count >= 0 AND child_count >= 0 AND child_no_bed_count >= 0
    AND senior_count >= 0 AND escort_count >= 0 AND guest_count >= 0
  ),
  CONSTRAINT chk_sales_orders_amounts CHECK (
    receivable_amount >= 0 AND received_amount >= 0
  )
);

DROP TRIGGER IF EXISTS trg_sales_orders_updated_at ON sales_orders;
CREATE TRIGGER trg_sales_orders_updated_at
BEFORE UPDATE ON sales_orders
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_orders_tenant_order_no_active
  ON sales_orders (tenant_id, order_no)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_sales_orders_tenant_deleted_team
  ON sales_orders (tenant_id, is_deleted, team_id, status);

CREATE INDEX IF NOT EXISTS idx_sales_orders_tenant_deleted_customer
  ON sales_orders (tenant_id, is_deleted, customer_id, booked_at DESC);

CREATE INDEX IF NOT EXISTS idx_sales_orders_tenant_deleted_booked
  ON sales_orders (tenant_id, is_deleted, booked_at DESC);

CREATE TABLE IF NOT EXISTS sales_order_price_lines (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  order_id bigint NOT NULL,
  team_id bigint NOT NULL,
  line_type varchar(40) NOT NULL,
  item_name varchar(120) NOT NULL,
  unit_price numeric(14,2) NOT NULL DEFAULT 0,
  quantity numeric(12,2) NOT NULL DEFAULT 0,
  subtotal_amount numeric(14,2) NOT NULL DEFAULT 0,
  sort_order integer NOT NULL DEFAULT 0,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_order_price_lines_order
    FOREIGN KEY (tenant_id, order_id) REFERENCES sales_orders (tenant_id, id),
  CONSTRAINT fk_sales_order_price_lines_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT chk_sales_order_price_lines_type CHECK (
    line_type IN ('adult', 'child', 'child_no_bed', 'senior', 'escort', 'single_room',
                  'flight', 'train', 'vehicle', 'guide_service', 'meal', 'room',
                  'ticket', 'optional', 'misc', 'extra')
  ),
  CONSTRAINT chk_sales_order_price_lines_amount CHECK (
    unit_price >= 0 AND quantity >= 0 AND subtotal_amount >= 0
  )
);

DROP TRIGGER IF EXISTS trg_sales_order_price_lines_updated_at ON sales_order_price_lines;
CREATE TRIGGER trg_sales_order_price_lines_updated_at
BEFORE UPDATE ON sales_order_price_lines
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_order_price_lines_tenant_order
  ON sales_order_price_lines (tenant_id, is_deleted, order_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_sales_order_price_lines_tenant_team
  ON sales_order_price_lines (tenant_id, is_deleted, team_id);

CREATE TABLE IF NOT EXISTS sales_order_guests (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  order_id bigint NOT NULL,
  team_id bigint NOT NULL,
  index_no integer NOT NULL DEFAULT 0,
  guest_name varchar(80) NOT NULL,
  english_name varchar(120),
  certificate_no varchar(80),
  passport_no varchar(80),
  gender varchar(20),
  birth_date date,
  age integer,
  phone varchar(40),
  guest_type varchar(30) NOT NULL DEFAULT 'adult',
  room_group varchar(120),
  leader_flag boolean NOT NULL DEFAULT false,
  id_card_valid boolean,
  id_card_warning varchar(300),
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  room_remark varchar(200),
  CONSTRAINT fk_sales_order_guests_order
    FOREIGN KEY (tenant_id, order_id) REFERENCES sales_orders (tenant_id, id),
  CONSTRAINT fk_sales_order_guests_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT chk_sales_order_guests_type CHECK (guest_type IN ('adult', 'child', 'child_no_bed', 'senior', 'escort')),
  CONSTRAINT chk_sales_order_guests_age CHECK (age IS NULL OR age >= 0)
);

DROP TRIGGER IF EXISTS trg_sales_order_guests_updated_at ON sales_order_guests;
CREATE TRIGGER trg_sales_order_guests_updated_at
BEFORE UPDATE ON sales_order_guests
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_order_guests_tenant_order
  ON sales_order_guests (tenant_id, is_deleted, order_id, index_no);

CREATE INDEX IF NOT EXISTS idx_sales_order_guests_tenant_team_name
  ON sales_order_guests (tenant_id, is_deleted, team_id, guest_name);

CREATE INDEX IF NOT EXISTS idx_sales_order_guests_tenant_certificate
  ON sales_order_guests (tenant_id, is_deleted, certificate_no);

CREATE TABLE IF NOT EXISTS sales_order_fee_changes (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  order_id bigint NOT NULL,
  team_id bigint NOT NULL,
  change_type varchar(20) NOT NULL DEFAULT 'increase',
  fee_description text NOT NULL,
  amount numeric(14,2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'pending',
  registered_by varchar(80),
  registered_at timestamptz NOT NULL DEFAULT now(),
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_order_fee_changes_order
    FOREIGN KEY (tenant_id, order_id) REFERENCES sales_orders (tenant_id, id),
  CONSTRAINT fk_sales_order_fee_changes_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT chk_sales_order_fee_changes_type CHECK (change_type IN ('increase', 'decrease')),
  CONSTRAINT chk_sales_order_fee_changes_status CHECK (status IN ('pending', 'approved', 'rejected')),
  CONSTRAINT chk_sales_order_fee_changes_amount CHECK (amount >= 0)
);

DROP TRIGGER IF EXISTS trg_sales_order_fee_changes_updated_at ON sales_order_fee_changes;
CREATE TRIGGER trg_sales_order_fee_changes_updated_at
BEFORE UPDATE ON sales_order_fee_changes
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_order_fee_changes_tenant_order
  ON sales_order_fee_changes (tenant_id, is_deleted, order_id, registered_at DESC);

CREATE INDEX IF NOT EXISTS idx_sales_order_fee_changes_tenant_team
  ON sales_order_fee_changes (tenant_id, is_deleted, team_id, status);

COMMENT ON TABLE sales_orders IS '销售收客订单主表，保存团队下客户报名、接送、行程、导游、酒店、人数、应收和订单状态。';
COMMENT ON COLUMN sales_orders.id IS '收客订单主键 ID。';
COMMENT ON COLUMN sales_orders.tenant_id IS '租户 ID，用于隔离不同地接公司的订单数据。';
COMMENT ON COLUMN sales_orders.team_id IS '所属销售团队 ID。';
COMMENT ON COLUMN sales_orders.order_no IS '订单编号，同一租户内未删除订单唯一。';
COMMENT ON COLUMN sales_orders.customer_id IS '客户单位 ID。';
COMMENT ON COLUMN sales_orders.customer_name IS '客户单位名称快照。';
COMMENT ON COLUMN sales_orders.contact_name IS '客户联系人姓名。';
COMMENT ON COLUMN sales_orders.contact_phone IS '客户联系人电话。';
COMMENT ON COLUMN sales_orders.customer_team_no IS '客户方团队编号或客户团号。';
COMMENT ON COLUMN sales_orders.source_province IS '客源地省份。';
COMMENT ON COLUMN sales_orders.source_city IS '客源地城市。';
COMMENT ON COLUMN sales_orders.source_district IS '客源地区县。';
COMMENT ON COLUMN sales_orders.travel_description IS '行程说明，包含来程、返程、参团时间等资料。';
COMMENT ON COLUMN sales_orders.pickup_info IS '接站或接机信息。';
COMMENT ON COLUMN sales_orders.dropoff_info IS '送站或送机信息。';
COMMENT ON COLUMN sales_orders.pickup_remark IS '接送备注。';
COMMENT ON COLUMN sales_orders.guide_name IS '导游姓名快照。';
COMMENT ON COLUMN sales_orders.guide_phone IS '导游联系电话快照。';
COMMENT ON COLUMN sales_orders.guide_remark IS '导游相关备注。';
COMMENT ON COLUMN sales_orders.hotel_info IS '订单酒店信息或住宿要求。';
COMMENT ON COLUMN sales_orders.adult_count IS '成人数量。';
COMMENT ON COLUMN sales_orders.child_count IS '儿童占床数量。';
COMMENT ON COLUMN sales_orders.child_no_bed_count IS '儿童不占床数量。';
COMMENT ON COLUMN sales_orders.senior_count IS '老人数量。';
COMMENT ON COLUMN sales_orders.escort_count IS '全陪数量。';
COMMENT ON COLUMN sales_orders.guest_count IS '订单总人数，保存时按游客名单和人数字段汇总。';
COMMENT ON COLUMN sales_orders.receivable_amount IS '订单应收金额，按价格明细合计。';
COMMENT ON COLUMN sales_orders.received_amount IS '订单已收金额，后续由收款模块回写。';
COMMENT ON COLUMN sales_orders.balance_amount IS '订单余额，应收减已收。';
COMMENT ON COLUMN sales_orders.fee_remark IS '费用说明。';
COMMENT ON COLUMN sales_orders.confirm_remark IS '确认说明。';
COMMENT ON COLUMN sales_orders.order_remark IS '订单备注。';
COMMENT ON COLUMN sales_orders.status IS '订单状态：pending未处理，confirmed已确认，cancelled已取消。';
COMMENT ON COLUMN sales_orders.booked_by IS '预订人或下单人名称。';
COMMENT ON COLUMN sales_orders.booked_at IS '预订或下单时间。';
COMMENT ON COLUMN sales_orders.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_orders.remark IS '订单内部备注。';
COMMENT ON COLUMN sales_orders.created_at IS '创建时间。';
COMMENT ON COLUMN sales_orders.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_orders.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN sales_orders.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_orders.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_order_price_lines IS '销售收客订单价格明细表，保存成人、儿童、交通、酒店、门票等订单收入分项。';
COMMENT ON COLUMN sales_order_price_lines.id IS '订单价格明细主键 ID。';
COMMENT ON COLUMN sales_order_price_lines.tenant_id IS '租户 ID，用于隔离不同地接公司的价格明细。';
COMMENT ON COLUMN sales_order_price_lines.order_id IS '所属订单 ID。';
COMMENT ON COLUMN sales_order_price_lines.team_id IS '所属销售团队 ID。';
COMMENT ON COLUMN sales_order_price_lines.line_type IS '价格分项类型，例如成人、儿童、单房差、机票、门票、自费等。';
COMMENT ON COLUMN sales_order_price_lines.item_name IS '价格项目名称。';
COMMENT ON COLUMN sales_order_price_lines.unit_price IS '单价。';
COMMENT ON COLUMN sales_order_price_lines.quantity IS '数量。';
COMMENT ON COLUMN sales_order_price_lines.subtotal_amount IS '小计金额。';
COMMENT ON COLUMN sales_order_price_lines.sort_order IS '显示顺序。';
COMMENT ON COLUMN sales_order_price_lines.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_order_price_lines.remark IS '价格明细备注。';
COMMENT ON COLUMN sales_order_price_lines.created_at IS '创建时间。';
COMMENT ON COLUMN sales_order_price_lines.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_order_price_lines.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN sales_order_price_lines.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_order_price_lines.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_order_guests IS '销售收客订单游客名单表，保存订单下游客证件、客户类型、分房、领队和校验结果。';
COMMENT ON COLUMN sales_order_guests.id IS '游客名单主键 ID。';
COMMENT ON COLUMN sales_order_guests.tenant_id IS '租户 ID，用于隔离不同地接公司的游客名单。';
COMMENT ON COLUMN sales_order_guests.order_id IS '所属订单 ID。';
COMMENT ON COLUMN sales_order_guests.team_id IS '所属销售团队 ID。';
COMMENT ON COLUMN sales_order_guests.index_no IS '名单序号。';
COMMENT ON COLUMN sales_order_guests.guest_name IS '游客姓名。';
COMMENT ON COLUMN sales_order_guests.english_name IS '游客英文名。';
COMMENT ON COLUMN sales_order_guests.certificate_no IS '身份证号或主要证件号。';
COMMENT ON COLUMN sales_order_guests.passport_no IS '护照号。';
COMMENT ON COLUMN sales_order_guests.gender IS '性别。';
COMMENT ON COLUMN sales_order_guests.birth_date IS '出生日期。';
COMMENT ON COLUMN sales_order_guests.age IS '年龄。';
COMMENT ON COLUMN sales_order_guests.phone IS '联系电话。';
COMMENT ON COLUMN sales_order_guests.guest_type IS '游客类型：成人、儿童占床、儿童不占床、老人或全陪。';
COMMENT ON COLUMN sales_order_guests.room_group IS '房间组号，用于标记同住一间房的游客。';
COMMENT ON COLUMN sales_order_guests.room_remark IS '分房备注，保存房型、同住要求和特殊住宿说明。';
COMMENT ON COLUMN sales_order_guests.leader_flag IS '是否领队。';
COMMENT ON COLUMN sales_order_guests.id_card_valid IS '身份证程序校验是否通过。';
COMMENT ON COLUMN sales_order_guests.id_card_warning IS '身份证校验提醒。';
COMMENT ON COLUMN sales_order_guests.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_order_guests.remark IS '游客备注。';
COMMENT ON COLUMN sales_order_guests.created_at IS '创建时间。';
COMMENT ON COLUMN sales_order_guests.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_order_guests.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN sales_order_guests.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_order_guests.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_order_fee_changes IS '销售订单费用变更记录表，保存加收、退减等应收调整记录。';
COMMENT ON COLUMN sales_order_fee_changes.id IS '费用变更主键 ID。';
COMMENT ON COLUMN sales_order_fee_changes.tenant_id IS '租户 ID，用于隔离不同地接公司的费用变更。';
COMMENT ON COLUMN sales_order_fee_changes.order_id IS '所属订单 ID。';
COMMENT ON COLUMN sales_order_fee_changes.team_id IS '所属销售团队 ID。';
COMMENT ON COLUMN sales_order_fee_changes.change_type IS '变更类型：increase加收，decrease退减。';
COMMENT ON COLUMN sales_order_fee_changes.fee_description IS '费用变更说明。';
COMMENT ON COLUMN sales_order_fee_changes.amount IS '变更金额。';
COMMENT ON COLUMN sales_order_fee_changes.status IS '变更状态：pending待处理，approved已通过，rejected已拒绝。';
COMMENT ON COLUMN sales_order_fee_changes.registered_by IS '登记人名称。';
COMMENT ON COLUMN sales_order_fee_changes.registered_at IS '登记时间。';
COMMENT ON COLUMN sales_order_fee_changes.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_order_fee_changes.remark IS '费用变更备注。';
COMMENT ON COLUMN sales_order_fee_changes.created_at IS '创建时间。';
COMMENT ON COLUMN sales_order_fee_changes.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_order_fee_changes.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN sales_order_fee_changes.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_order_fee_changes.deleted_by IS '删除人账号或名称。';

COMMIT;

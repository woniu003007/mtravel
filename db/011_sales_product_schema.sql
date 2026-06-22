-- 旅游接待管理系统：销售产品模板表
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 仅用于让本脚本可在独立临时库中校验。正式库中 tenants 已由客户管理基础脚本创建。
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS sales_products (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_name varchar(200) NOT NULL,
  business_type varchar(120),
  domestic_international varchar(20) NOT NULL DEFAULT 'domestic',
  province varchar(80),
  city varchar(80),
  district varchar(80),
  trip_type varchar(20) NOT NULL DEFAULT 'irregular',
  reception_standard varchar(120),
  product_theme varchar(120),
  travel_days integer NOT NULL DEFAULT 1,
  close_days_before integer NOT NULL DEFAULT 0,
  single_room_difference numeric(12,2) NOT NULL DEFAULT 0,
  planned_capacity integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_sales_products_domestic CHECK (domestic_international IN ('domestic', 'international')),
  CONSTRAINT chk_sales_products_trip_type CHECK (trip_type IN ('daily', 'weekly', 'irregular')),
  CONSTRAINT chk_sales_products_days CHECK (travel_days >= 1),
  CONSTRAINT chk_sales_products_close_days CHECK (close_days_before >= 0),
  CONSTRAINT chk_sales_products_single_room_difference CHECK (single_room_difference >= 0),
  CONSTRAINT chk_sales_products_planned_capacity CHECK (planned_capacity >= 0),
  CONSTRAINT chk_sales_products_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_sales_products_tenant_id_id UNIQUE (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_sales_products_updated_at ON sales_products;
CREATE TRIGGER trg_sales_products_updated_at
BEFORE UPDATE ON sales_products
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_products_tenant_deleted_status
  ON sales_products (tenant_id, is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_sales_products_tenant_deleted_city
  ON sales_products (tenant_id, is_deleted, province, city, district);

CREATE INDEX IF NOT EXISTS idx_sales_products_tenant_deleted_type
  ON sales_products (tenant_id, is_deleted, business_type, reception_standard, product_theme);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_products_tenant_name_active
  ON sales_products (tenant_id, product_name)
  WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS sales_product_itinerary_days (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  day_no integer NOT NULL,
  day_title varchar(200),
  itinerary_content text,
  accommodation_note varchar(300),
  related_hotel varchar(200),
  seasonal_surcharge numeric(12,2) NOT NULL DEFAULT 0,
  breakfast_included boolean NOT NULL DEFAULT false,
  lunch_included boolean NOT NULL DEFAULT false,
  dinner_included boolean NOT NULL DEFAULT false,
  roadbook_place varchar(300),
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  roadbook_summary varchar(500),
  roadbook_total_distance_meters integer NOT NULL DEFAULT 0,
  roadbook_total_duration_seconds integer NOT NULL DEFAULT 0,
  CONSTRAINT fk_sales_product_itinerary_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT chk_sales_product_itinerary_day CHECK (day_no >= 1),
  CONSTRAINT chk_sales_product_itinerary_surcharge CHECK (seasonal_surcharge >= 0),
  CONSTRAINT chk_sales_product_itinerary_distance CHECK (roadbook_total_distance_meters >= 0),
  CONSTRAINT chk_sales_product_itinerary_duration CHECK (roadbook_total_duration_seconds >= 0)
);

DROP TRIGGER IF EXISTS trg_sales_product_itinerary_days_updated_at ON sales_product_itinerary_days;
CREATE TRIGGER trg_sales_product_itinerary_days_updated_at
BEFORE UPDATE ON sales_product_itinerary_days
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_itinerary_product
  ON sales_product_itinerary_days (tenant_id, is_deleted, product_id, day_no);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_itinerary_day_active
  ON sales_product_itinerary_days (tenant_id, product_id, day_no)
  WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS sales_product_roadbook_points (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  day_no integer NOT NULL,
  point_order integer NOT NULL,
  place_name varchar(200) NOT NULL,
  address varchar(300),
  longitude varchar(40),
  latitude varchar(40),
  point_type varchar(30) NOT NULL DEFAULT 'waypoint',
  stay_minutes integer NOT NULL DEFAULT 0,
  distance_to_next_meters integer NOT NULL DEFAULT 0,
  duration_to_next_seconds integer NOT NULL DEFAULT 0,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_product_roadbook_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT chk_sales_product_roadbook_day CHECK (day_no >= 1),
  CONSTRAINT chk_sales_product_roadbook_order CHECK (point_order >= 1),
  CONSTRAINT chk_sales_product_roadbook_stay CHECK (stay_minutes >= 0),
  CONSTRAINT chk_sales_product_roadbook_distance CHECK (distance_to_next_meters >= 0),
  CONSTRAINT chk_sales_product_roadbook_duration CHECK (duration_to_next_seconds >= 0),
  CONSTRAINT chk_sales_product_roadbook_type CHECK (
    point_type IN ('departure', 'waypoint', 'scenic', 'meal', 'shopping', 'hotel', 'arrival')
  )
);

DROP TRIGGER IF EXISTS trg_sales_product_roadbook_points_updated_at ON sales_product_roadbook_points;
CREATE TRIGGER trg_sales_product_roadbook_points_updated_at
BEFORE UPDATE ON sales_product_roadbook_points
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_roadbook_product_day
  ON sales_product_roadbook_points (tenant_id, is_deleted, product_id, day_no, point_order);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_roadbook_point_order_active
  ON sales_product_roadbook_points (tenant_id, product_id, day_no, point_order)
  WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS sales_product_descriptions (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  booking_notice text,
  product_description text,
  fee_included text,
  fee_excluded text,
  child_policy text,
  shopping_arrangement text,
  optional_items text,
  gift_items text,
  attention_items text,
  warm_reminder text,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_product_descriptions_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_sales_product_descriptions_updated_at ON sales_product_descriptions;
CREATE TRIGGER trg_sales_product_descriptions_updated_at
BEFORE UPDATE ON sales_product_descriptions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_descriptions_product
  ON sales_product_descriptions (tenant_id, is_deleted, product_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_descriptions_product_active
  ON sales_product_descriptions (tenant_id, product_id)
  WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS sales_product_arrangement_items (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  arrangement_type varchar(30) NOT NULL,
  item_name varchar(160) NOT NULL,
  arrangement_content text,
  quantity numeric(12,2) NOT NULL DEFAULT 0,
  unit_price numeric(12,2) NOT NULL DEFAULT 0,
  unit_name varchar(40),
  settlement_type varchar(20) NOT NULL DEFAULT 'credit',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  allocation_mode varchar(40),
  schedule_start_day varchar(40),
  schedule_end_day varchar(40),
  departure_place varchar(120),
  arrival_place varchar(120),
  days_count integer NOT NULL DEFAULT 0,
  resource_name varchar(200),
  supplier_id bigint,
  supplier_name varchar(200),
  driver_name varchar(100),
  vehicle_plate varchar(40),
  traffic_type varchar(40),
  vehicle_type varchar(40),
  meal_type varchar(40),
  fund_included varchar(40),
  confirmed boolean NOT NULL DEFAULT false,
  confirmation_no varchar(100),
  guide_id bigint,
  guide_name varchar(100),
  responsible_employee_id bigint,
  responsible_employee_name varchar(100),
  order_scope varchar(120) NOT NULL DEFAULT '=不关联订单=',
  total_amount numeric(12,2) NOT NULL DEFAULT 0,
  cash_amount numeric(12,2) NOT NULL DEFAULT 0,
  credit_amount numeric(12,2) NOT NULL DEFAULT 0,
  prepaid_amount numeric(12,2) NOT NULL DEFAULT 0,
  sale_amount numeric(12,2) NOT NULL DEFAULT 0,
  cost_amount numeric(12,2) NOT NULL DEFAULT 0,
  guide_commission_amount numeric(12,2) NOT NULL DEFAULT 0,
  company_rebate_amount numeric(12,2) NOT NULL DEFAULT 0,
  head_fee_amount numeric(12,2) NOT NULL DEFAULT 0,
  consumption_amount numeric(12,2) NOT NULL DEFAULT 0,
  people_count numeric(12,2) NOT NULL DEFAULT 0,
  no_guide_report boolean NOT NULL DEFAULT false,
  CONSTRAINT fk_sales_product_arrangement_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT chk_sales_product_arrangement_type CHECK (
    arrangement_type IN ('traffic', 'hotel', 'vehicle', 'scenic', 'meal', 'other', 'optional', 'shopping', 'ground_agent', 'extra_fee')
  ),
  CONSTRAINT chk_sales_product_arrangement_quantity CHECK (quantity >= 0),
  CONSTRAINT chk_sales_product_arrangement_unit_price CHECK (unit_price >= 0),
  CONSTRAINT chk_sales_product_arrangement_settlement CHECK (settlement_type IN ('cash', 'credit')),
  CONSTRAINT chk_sales_product_arrangement_allocation CHECK (
    allocation_mode IS NULL OR allocation_mode IN ('group_order_average', 'multi_order_average')
  ),
  CONSTRAINT chk_sales_product_arrangement_days CHECK (days_count >= 0),
  CONSTRAINT chk_sales_product_arrangement_amounts CHECK (
    total_amount >= 0
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
  CONSTRAINT uk_sales_product_arrangement_tenant_id_id UNIQUE (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_sales_product_arrangement_items_updated_at ON sales_product_arrangement_items;
CREATE TRIGGER trg_sales_product_arrangement_items_updated_at
BEFORE UPDATE ON sales_product_arrangement_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_arrangement_product
  ON sales_product_arrangement_items (tenant_id, is_deleted, product_id, arrangement_type);

ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS allocation_mode varchar(40);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS schedule_start_day varchar(40);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS schedule_end_day varchar(40);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS departure_place varchar(120);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS arrival_place varchar(120);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS days_count integer NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS resource_name varchar(200);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS supplier_id bigint;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS supplier_name varchar(200);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS driver_name varchar(100);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS vehicle_plate varchar(40);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS traffic_type varchar(40);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS vehicle_type varchar(40);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS meal_type varchar(40);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS fund_included varchar(40);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS confirmed boolean NOT NULL DEFAULT false;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS confirmation_no varchar(100);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS guide_id bigint;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS guide_name varchar(100);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS responsible_employee_id bigint;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS responsible_employee_name varchar(100);
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS order_scope varchar(120) NOT NULL DEFAULT '=不关联订单=';
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS total_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS cash_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS credit_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS prepaid_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS sale_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS cost_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS guide_commission_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS company_rebate_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS head_fee_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS consumption_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS people_count numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales_product_arrangement_items ADD COLUMN IF NOT EXISTS no_guide_report boolean NOT NULL DEFAULT false;
ALTER TABLE sales_product_arrangement_items DROP CONSTRAINT IF EXISTS chk_sales_product_arrangement_allocation;
ALTER TABLE sales_product_arrangement_items ADD CONSTRAINT chk_sales_product_arrangement_allocation CHECK (
  allocation_mode IS NULL OR allocation_mode IN ('group_order_average', 'multi_order_average')
);
ALTER TABLE sales_product_arrangement_items DROP CONSTRAINT IF EXISTS chk_sales_product_arrangement_days;
ALTER TABLE sales_product_arrangement_items ADD CONSTRAINT chk_sales_product_arrangement_days CHECK (days_count >= 0);
ALTER TABLE sales_product_arrangement_items DROP CONSTRAINT IF EXISTS chk_sales_product_arrangement_amounts;
ALTER TABLE sales_product_arrangement_items ADD CONSTRAINT chk_sales_product_arrangement_amounts CHECK (
  total_amount >= 0
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
);
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conrelid = 'sales_product_arrangement_items'::regclass
      AND conname = 'uk_sales_product_arrangement_tenant_id_id'
  ) THEN
    ALTER TABLE sales_product_arrangement_items
      ADD CONSTRAINT uk_sales_product_arrangement_tenant_id_id UNIQUE (tenant_id, id);
  END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS sales_product_arrangement_price_lines (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  arrangement_item_id bigint NOT NULL,
  project_id bigint,
  project_name varchar(120),
  unit_price numeric(12,2) NOT NULL DEFAULT 0,
  quantity numeric(12,2) NOT NULL DEFAULT 0,
  amount numeric(12,2) NOT NULL DEFAULT 0,
  sale_price numeric(12,2) NOT NULL DEFAULT 0,
  cost_price numeric(12,2) NOT NULL DEFAULT 0,
  cash_amount numeric(12,2) NOT NULL DEFAULT 0,
  credit_amount numeric(12,2) NOT NULL DEFAULT 0,
  guide_commission_amount numeric(12,2) NOT NULL DEFAULT 0,
  guide_commission_rate numeric(8,4) NOT NULL DEFAULT 0,
  company_rebate_amount numeric(12,2) NOT NULL DEFAULT 0,
  head_fee_amount numeric(12,2) NOT NULL DEFAULT 0,
  consumption_amount numeric(12,2) NOT NULL DEFAULT 0,
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_product_arrangement_price_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT fk_sales_product_arrangement_price_item
    FOREIGN KEY (tenant_id, arrangement_item_id) REFERENCES sales_product_arrangement_items (tenant_id, id),
  CONSTRAINT chk_sales_product_arrangement_price_amounts CHECK (
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
    AND head_fee_amount >= 0
    AND consumption_amount >= 0
  ),
  CONSTRAINT chk_sales_product_arrangement_price_sort CHECK (sort_order >= 1)
);

DROP TRIGGER IF EXISTS trg_sales_product_arrangement_price_lines_updated_at ON sales_product_arrangement_price_lines;
CREATE TRIGGER trg_sales_product_arrangement_price_lines_updated_at
BEFORE UPDATE ON sales_product_arrangement_price_lines
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_arrangement_price_item
  ON sales_product_arrangement_price_lines (tenant_id, is_deleted, product_id, arrangement_item_id, sort_order);

CREATE TABLE IF NOT EXISTS vehicle_quote_rules (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  vehicle_type varchar(40) NOT NULL,
  province varchar(80),
  city varchar(80),
  district varchar(80),
  base_price numeric(12,2) NOT NULL DEFAULT 0,
  base_kilometers numeric(10,2) NOT NULL DEFAULT 0,
  extra_kilometer_price numeric(12,2) NOT NULL DEFAULT 0,
  minimum_price numeric(12,2) NOT NULL DEFAULT 0,
  float_rate numeric(8,4) NOT NULL DEFAULT 1,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_vehicle_quote_rules_amounts CHECK (
    base_price >= 0
    AND base_kilometers >= 0
    AND extra_kilometer_price >= 0
    AND minimum_price >= 0
    AND float_rate > 0
  ),
  CONSTRAINT chk_vehicle_quote_rules_status CHECK (status IN ('active', 'disabled'))
);

DROP TRIGGER IF EXISTS trg_vehicle_quote_rules_updated_at ON vehicle_quote_rules;
CREATE TRIGGER trg_vehicle_quote_rules_updated_at
BEFORE UPDATE ON vehicle_quote_rules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_vehicle_quote_rules_scope_active
  ON vehicle_quote_rules (tenant_id, vehicle_type)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_vehicle_quote_rules_query
  ON vehicle_quote_rules (tenant_id, is_deleted, status, vehicle_type, province, city, district);

CREATE TABLE IF NOT EXISTS sales_product_vehicle_quote_snapshots (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  arrangement_item_id bigint NOT NULL,
  schedule_start_day varchar(40),
  schedule_end_day varchar(40),
  start_day_no integer,
  end_day_no integer,
  synced_distance_meters integer NOT NULL DEFAULT 0,
  synced_duration_seconds integer NOT NULL DEFAULT 0,
  route_summary varchar(1000),
  quote_rule_id bigint,
  rule_vehicle_type varchar(40),
  rule_province varchar(80),
  rule_city varchar(80),
  rule_district varchar(80),
  rule_base_price numeric(12,2) NOT NULL DEFAULT 0,
  rule_base_kilometers numeric(10,2) NOT NULL DEFAULT 0,
  rule_extra_kilometer_price numeric(12,2) NOT NULL DEFAULT 0,
  rule_minimum_price numeric(12,2) NOT NULL DEFAULT 0,
  rule_float_rate numeric(8,4) NOT NULL DEFAULT 1,
  calculated_amount numeric(12,2) NOT NULL DEFAULT 0,
  confirmed_amount numeric(12,2) NOT NULL DEFAULT 0,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_product_vehicle_quote_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT fk_sales_product_vehicle_quote_item
    FOREIGN KEY (tenant_id, arrangement_item_id) REFERENCES sales_product_arrangement_items (tenant_id, id),
  CONSTRAINT chk_sales_product_vehicle_quote_days CHECK (
    (start_day_no IS NULL OR start_day_no >= 1)
    AND (end_day_no IS NULL OR end_day_no >= 1)
  ),
  CONSTRAINT chk_sales_product_vehicle_quote_distance CHECK (
    synced_distance_meters >= 0
    AND synced_duration_seconds >= 0
  ),
  CONSTRAINT chk_sales_product_vehicle_quote_amounts CHECK (
    rule_base_price >= 0
    AND rule_base_kilometers >= 0
    AND rule_extra_kilometer_price >= 0
    AND rule_minimum_price >= 0
    AND rule_float_rate > 0
    AND calculated_amount >= 0
    AND confirmed_amount >= 0
  )
);

DROP TRIGGER IF EXISTS trg_sales_product_vehicle_quote_snapshots_updated_at ON sales_product_vehicle_quote_snapshots;
CREATE TRIGGER trg_sales_product_vehicle_quote_snapshots_updated_at
BEFORE UPDATE ON sales_product_vehicle_quote_snapshots
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_vehicle_quote_item_active
  ON sales_product_vehicle_quote_snapshots (tenant_id, arrangement_item_id)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_sales_product_vehicle_quote_product
  ON sales_product_vehicle_quote_snapshots (tenant_id, is_deleted, product_id, arrangement_item_id);

CREATE TABLE IF NOT EXISTS sales_product_vehicle_inquiries (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  arrangement_item_id bigint NOT NULL,
  sort_order integer NOT NULL DEFAULT 1,
  inquiry_method varchar(30) NOT NULL DEFAULT 'wechat_group',
  inquiry_person varchar(100),
  inquiry_time timestamptz,
  group_name varchar(160),
  supplier_id bigint,
  supplier_name varchar(200),
  quoted_amount numeric(12,2) NOT NULL DEFAULT 0,
  includes_toll boolean NOT NULL DEFAULT false,
  includes_parking boolean NOT NULL DEFAULT false,
  includes_driver_meal boolean NOT NULL DEFAULT false,
  includes_driver_lodging boolean NOT NULL DEFAULT false,
  available_vehicle_count integer NOT NULL DEFAULT 0,
  reply_person varchar(100),
  reply_time timestamptz,
  attachment_id bigint,
  attachment_url varchar(500),
  selected boolean NOT NULL DEFAULT false,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_product_vehicle_inquiry_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT fk_sales_product_vehicle_inquiry_item
    FOREIGN KEY (tenant_id, arrangement_item_id) REFERENCES sales_product_arrangement_items (tenant_id, id),
  CONSTRAINT chk_sales_product_vehicle_inquiry_method CHECK (
    inquiry_method IN ('wechat_group', 'enterprise_wechat', 'phone', 'other')
  ),
  CONSTRAINT chk_sales_product_vehicle_inquiry_amount CHECK (
    sort_order >= 1
    AND quoted_amount >= 0
    AND available_vehicle_count >= 0
  )
);

DROP TRIGGER IF EXISTS trg_sales_product_vehicle_inquiries_updated_at ON sales_product_vehicle_inquiries;
CREATE TRIGGER trg_sales_product_vehicle_inquiries_updated_at
BEFORE UPDATE ON sales_product_vehicle_inquiries
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_vehicle_inquiry_item
  ON sales_product_vehicle_inquiries (tenant_id, is_deleted, product_id, arrangement_item_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_sales_product_vehicle_inquiry_selected
  ON sales_product_vehicle_inquiries (tenant_id, is_deleted, product_id, selected);

CREATE TABLE IF NOT EXISTS vehicle_usage_histories (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  history_type varchar(30) NOT NULL,
  content varchar(160) NOT NULL,
  normalized_content varchar(160) NOT NULL,
  usage_count integer NOT NULL DEFAULT 1,
  last_used_at timestamptz NOT NULL DEFAULT now(),
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_vehicle_usage_histories_type CHECK (history_type IN ('driver_info', 'vehicle_plate')),
  CONSTRAINT chk_vehicle_usage_histories_usage_count CHECK (usage_count >= 0)
);

DROP TRIGGER IF EXISTS trg_vehicle_usage_histories_updated_at ON vehicle_usage_histories;
CREATE TRIGGER trg_vehicle_usage_histories_updated_at
BEFORE UPDATE ON vehicle_usage_histories
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_vehicle_usage_histories_content_active
  ON vehicle_usage_histories (tenant_id, history_type, normalized_content)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_vehicle_usage_histories_suggest
  ON vehicle_usage_histories (tenant_id, is_deleted, history_type, usage_count DESC, last_used_at DESC);

COMMENT ON TABLE sales_products IS '销售产品模板主表。用于维护线路产品的基础资料，后续团期和团队从产品模板生成。';
COMMENT ON COLUMN sales_products.id IS '销售产品主键ID，系统内部使用。';
COMMENT ON COLUMN sales_products.tenant_id IS '租户ID，标识该产品属于哪一家地接公司。';
COMMENT ON COLUMN sales_products.product_name IS '产品名称，也就是线路名称。';
COMMENT ON COLUMN sales_products.business_type IS '业务类型，例如疗休养、定制团、红色培训。';
COMMENT ON COLUMN sales_products.domestic_international IS '国内国际标记。domestic表示国内，international表示国际。';
COMMENT ON COLUMN sales_products.province IS '接团省份。';
COMMENT ON COLUMN sales_products.city IS '接团城市。';
COMMENT ON COLUMN sales_products.district IS '接团区县。';
COMMENT ON COLUMN sales_products.trip_type IS '出团类型。daily每天发，weekly每周发，irregular不定期。';
COMMENT ON COLUMN sales_products.reception_standard IS '接待标准，例如商务快捷、携程四钻、携程五钻。';
COMMENT ON COLUMN sales_products.product_theme IS '产品主题，例如观光、亲子游、夕阳红。';
COMMENT ON COLUMN sales_products.travel_days IS '旅游天数。';
COMMENT ON COLUMN sales_products.close_days_before IS '截止收客天数，表示出团前多少天停止收客。';
COMMENT ON COLUMN sales_products.single_room_difference IS '单人房差金额。';
COMMENT ON COLUMN sales_products.planned_capacity IS '预控人数，用于后续生成团期时参考。';
COMMENT ON COLUMN sales_products.status IS '产品状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN sales_products.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_products.remark IS '备注，用于记录产品管理说明。';
COMMENT ON COLUMN sales_products.created_at IS '创建时间。';
COMMENT ON COLUMN sales_products.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_products.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN sales_products.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN sales_products.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE sales_product_itinerary_days IS '销售产品每日行程表。用于按天维护产品行程、住宿、用餐和路书地点。';
COMMENT ON COLUMN sales_product_itinerary_days.id IS '每日行程主键ID。';
COMMENT ON COLUMN sales_product_itinerary_days.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_itinerary_days.product_id IS '所属销售产品ID。';
COMMENT ON COLUMN sales_product_itinerary_days.day_no IS '行程第几天，从1开始。';
COMMENT ON COLUMN sales_product_itinerary_days.day_title IS '当日行程标题。';
COMMENT ON COLUMN sales_product_itinerary_days.itinerary_content IS '当日行程内容。';
COMMENT ON COLUMN sales_product_itinerary_days.accommodation_note IS '住宿说明。';
COMMENT ON COLUMN sales_product_itinerary_days.related_hotel IS '关联酒店名称或说明。';
COMMENT ON COLUMN sales_product_itinerary_days.seasonal_surcharge IS '旺季附加费。';
COMMENT ON COLUMN sales_product_itinerary_days.breakfast_included IS '是否含早餐。';
COMMENT ON COLUMN sales_product_itinerary_days.lunch_included IS '是否含中餐。';
COMMENT ON COLUMN sales_product_itinerary_days.dinner_included IS '是否含晚餐。';
COMMENT ON COLUMN sales_product_itinerary_days.roadbook_place IS '路书地点或关键途经地点。';
COMMENT ON COLUMN sales_product_itinerary_days.roadbook_summary IS '当天路书路线摘要，例如出发地到景区再到酒店。';
COMMENT ON COLUMN sales_product_itinerary_days.roadbook_total_distance_meters IS '当天路书总距离，单位米。';
COMMENT ON COLUMN sales_product_itinerary_days.roadbook_total_duration_seconds IS '当天路书预计总车程，单位秒。';
COMMENT ON COLUMN sales_product_itinerary_days.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_itinerary_days.remark IS '备注。';
COMMENT ON COLUMN sales_product_itinerary_days.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_itinerary_days.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_product_itinerary_days.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_itinerary_days.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_itinerary_days.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_product_roadbook_points IS '销售产品每日路书地点表。用于按天保存地图选点、路线顺序和到下一站距离时长。';
COMMENT ON COLUMN sales_product_roadbook_points.id IS '路书地点主键ID。';
COMMENT ON COLUMN sales_product_roadbook_points.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_roadbook_points.product_id IS '所属销售产品ID。';
COMMENT ON COLUMN sales_product_roadbook_points.day_no IS '行程第几天，从1开始。';
COMMENT ON COLUMN sales_product_roadbook_points.point_order IS '当天地点顺序，从1开始。';
COMMENT ON COLUMN sales_product_roadbook_points.place_name IS '地点名称。';
COMMENT ON COLUMN sales_product_roadbook_points.address IS '地点详细地址。';
COMMENT ON COLUMN sales_product_roadbook_points.longitude IS '地点经度。';
COMMENT ON COLUMN sales_product_roadbook_points.latitude IS '地点纬度。';
COMMENT ON COLUMN sales_product_roadbook_points.point_type IS '地点类型。departure出发，waypoint途经，scenic景区，meal用餐，shopping购物，hotel酒店，arrival结束。';
COMMENT ON COLUMN sales_product_roadbook_points.stay_minutes IS '计划停留时长，单位分钟。';
COMMENT ON COLUMN sales_product_roadbook_points.distance_to_next_meters IS '到下一站距离，单位米。';
COMMENT ON COLUMN sales_product_roadbook_points.duration_to_next_seconds IS '到下一站预计车程，单位秒。';
COMMENT ON COLUMN sales_product_roadbook_points.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_roadbook_points.remark IS '备注。';
COMMENT ON COLUMN sales_product_roadbook_points.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_roadbook_points.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_product_roadbook_points.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_roadbook_points.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_roadbook_points.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_product_descriptions IS '销售产品说明表。用于维护收客须知、费用包含不含、儿童安排等可复用说明。';
COMMENT ON COLUMN sales_product_descriptions.id IS '产品说明主键ID。';
COMMENT ON COLUMN sales_product_descriptions.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_descriptions.product_id IS '所属销售产品ID。';
COMMENT ON COLUMN sales_product_descriptions.booking_notice IS '收客须知。';
COMMENT ON COLUMN sales_product_descriptions.product_description IS '产品说明正文。';
COMMENT ON COLUMN sales_product_descriptions.fee_included IS '费用包含说明。';
COMMENT ON COLUMN sales_product_descriptions.fee_excluded IS '费用不含说明。';
COMMENT ON COLUMN sales_product_descriptions.child_policy IS '儿童安排说明。';
COMMENT ON COLUMN sales_product_descriptions.shopping_arrangement IS '购物安排说明。';
COMMENT ON COLUMN sales_product_descriptions.optional_items IS '自费项目说明。';
COMMENT ON COLUMN sales_product_descriptions.gift_items IS '赠送项目说明。';
COMMENT ON COLUMN sales_product_descriptions.attention_items IS '注意事项。';
COMMENT ON COLUMN sales_product_descriptions.warm_reminder IS '温馨提醒。';
COMMENT ON COLUMN sales_product_descriptions.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_descriptions.remark IS '备注。';
COMMENT ON COLUMN sales_product_descriptions.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_descriptions.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_product_descriptions.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_descriptions.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_descriptions.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_product_arrangement_items IS '销售产品团队安排参数表。用于维护产品模板下的大交通、住宿、用车、景区、用餐等默认安排和费用参考。';
COMMENT ON COLUMN sales_product_arrangement_items.id IS '团队安排参数主键ID。';
COMMENT ON COLUMN sales_product_arrangement_items.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_arrangement_items.product_id IS '所属销售产品ID。';
COMMENT ON COLUMN sales_product_arrangement_items.arrangement_type IS '安排类型。traffic大交通，hotel住宿，vehicle用车，scenic景区，meal用餐，other其它，optional自费，shopping购物，ground_agent地接，extra_fee附加费用。';
COMMENT ON COLUMN sales_product_arrangement_items.item_name IS '安排项目名称。';
COMMENT ON COLUMN sales_product_arrangement_items.arrangement_content IS '安排内容或默认说明。';
COMMENT ON COLUMN sales_product_arrangement_items.quantity IS '默认数量。';
COMMENT ON COLUMN sales_product_arrangement_items.unit_price IS '默认单价或费用参考。';
COMMENT ON COLUMN sales_product_arrangement_items.unit_name IS '计量单位。';
COMMENT ON COLUMN sales_product_arrangement_items.settlement_type IS '结算类型。cash表示现结，credit表示挂账。';
COMMENT ON COLUMN sales_product_arrangement_items.allocation_mode IS '费用归属模式。group_order_average表示全团或订单均摊，multi_order_average表示多订单均摊成本。';
COMMENT ON COLUMN sales_product_arrangement_items.schedule_start_day IS '使用开始日期或行程第几天。';
COMMENT ON COLUMN sales_product_arrangement_items.schedule_end_day IS '使用结束日期或退房日期。';
COMMENT ON COLUMN sales_product_arrangement_items.departure_place IS '出发地，主要用于大交通安排。';
COMMENT ON COLUMN sales_product_arrangement_items.arrival_place IS '目的地，主要用于大交通安排。';
COMMENT ON COLUMN sales_product_arrangement_items.days_count IS '天数、晚数或使用天数。';
COMMENT ON COLUMN sales_product_arrangement_items.resource_name IS '资源名称，例如酒店、景区、餐厅或购物店。';
COMMENT ON COLUMN sales_product_arrangement_items.supplier_id IS '供应商ID。';
COMMENT ON COLUMN sales_product_arrangement_items.supplier_name IS '供应商名称快照，便于历史产品模板回显。';
COMMENT ON COLUMN sales_product_arrangement_items.driver_name IS '司机姓名或联系方式，主要用于用车安排。';
COMMENT ON COLUMN sales_product_arrangement_items.vehicle_plate IS '车牌号，主要用于用车安排。';
COMMENT ON COLUMN sales_product_arrangement_items.traffic_type IS '交通类型，例如飞机、高铁、火车。';
COMMENT ON COLUMN sales_product_arrangement_items.vehicle_type IS '车型，例如7座、39座、54座。';
COMMENT ON COLUMN sales_product_arrangement_items.meal_type IS '用餐时间或餐型，例如早餐、中餐、晚餐。';
COMMENT ON COLUMN sales_product_arrangement_items.fund_included IS '酒店基金是否包含。';
COMMENT ON COLUMN sales_product_arrangement_items.confirmed IS '是否已确认。';
COMMENT ON COLUMN sales_product_arrangement_items.confirmation_no IS '确认号。';
COMMENT ON COLUMN sales_product_arrangement_items.guide_id IS '导游ID。';
COMMENT ON COLUMN sales_product_arrangement_items.guide_name IS '导游姓名快照。';
COMMENT ON COLUMN sales_product_arrangement_items.responsible_employee_id IS '责任员工ID，例如房调、车调或计调。';
COMMENT ON COLUMN sales_product_arrangement_items.responsible_employee_name IS '责任员工名称快照。';
COMMENT ON COLUMN sales_product_arrangement_items.order_scope IS '订单归属说明。产品模板阶段默认不关联正式订单。';
COMMENT ON COLUMN sales_product_arrangement_items.total_amount IS '合计成本或总金额。';
COMMENT ON COLUMN sales_product_arrangement_items.cash_amount IS '现结金额。';
COMMENT ON COLUMN sales_product_arrangement_items.credit_amount IS '挂账金额。';
COMMENT ON COLUMN sales_product_arrangement_items.prepaid_amount IS '预付款金额。';
COMMENT ON COLUMN sales_product_arrangement_items.sale_amount IS '收入合计，主要用于自费项目。';
COMMENT ON COLUMN sales_product_arrangement_items.cost_amount IS '成本合计。';
COMMENT ON COLUMN sales_product_arrangement_items.guide_commission_amount IS '导游提成金额。';
COMMENT ON COLUMN sales_product_arrangement_items.company_rebate_amount IS '公司返佣金额。';
COMMENT ON COLUMN sales_product_arrangement_items.head_fee_amount IS '人头费金额。';
COMMENT ON COLUMN sales_product_arrangement_items.consumption_amount IS '消费金额。';
COMMENT ON COLUMN sales_product_arrangement_items.people_count IS '人数。';
COMMENT ON COLUMN sales_product_arrangement_items.no_guide_report IS '是否无需导游报账，同步计调审核数据。';
COMMENT ON COLUMN sales_product_arrangement_items.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_arrangement_items.remark IS '备注。';
COMMENT ON COLUMN sales_product_arrangement_items.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_arrangement_items.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_product_arrangement_items.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_arrangement_items.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_arrangement_items.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_product_arrangement_price_lines IS '销售产品团队安排价格明细表。用于保存团队安排项目下多行价格信息。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.id IS '价格明细主键ID。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.product_id IS '所属销售产品ID。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.arrangement_item_id IS '所属团队安排项目ID。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.project_id IS '费用项目ID。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.project_name IS '费用项目名称。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.unit_price IS '单价。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.quantity IS '数量。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.amount IS '小计金额。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.sale_price IS '自费项目销售单价。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.cost_price IS '自费项目成本单价。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.cash_amount IS '现结金额。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.credit_amount IS '挂账金额。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.guide_commission_amount IS '导游提成金额。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.guide_commission_rate IS '导游提成比例。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.company_rebate_amount IS '公司返佣金额。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.head_fee_amount IS '人头费金额。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.consumption_amount IS '消费金额。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.sort_order IS '排序号，数字越小越靠前。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.remark IS '备注。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_arrangement_price_lines.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE vehicle_quote_rules IS '座位数报价规则表。用于按车辆座位数和路书公里测算用车参考价。';
COMMENT ON COLUMN vehicle_quote_rules.id IS '座位数报价规则主键ID。';
COMMENT ON COLUMN vehicle_quote_rules.tenant_id IS '租户ID。';
COMMENT ON COLUMN vehicle_quote_rules.vehicle_type IS '车辆座位数，例如7座、39座。';
COMMENT ON COLUMN vehicle_quote_rules.province IS '预留省份字段，当前座位数报价规则暂不按地区区分。';
COMMENT ON COLUMN vehicle_quote_rules.city IS '预留城市字段，当前座位数报价规则暂不按地区区分。';
COMMENT ON COLUMN vehicle_quote_rules.district IS '预留区县字段，当前座位数报价规则暂不按地区区分。';
COMMENT ON COLUMN vehicle_quote_rules.base_price IS '基础价，覆盖基础公里以内的参考费用。';
COMMENT ON COLUMN vehicle_quote_rules.base_kilometers IS '基础公里数。';
COMMENT ON COLUMN vehicle_quote_rules.extra_kilometer_price IS '超出基础公里后的每公里参考价。';
COMMENT ON COLUMN vehicle_quote_rules.minimum_price IS '最低参考价。';
COMMENT ON COLUMN vehicle_quote_rules.float_rate IS '浮动系数，例如1.10表示上浮10%。';
COMMENT ON COLUMN vehicle_quote_rules.status IS '规则状态。active启用，disabled停用。';
COMMENT ON COLUMN vehicle_quote_rules.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN vehicle_quote_rules.remark IS '备注。';
COMMENT ON COLUMN vehicle_quote_rules.created_at IS '创建时间。';
COMMENT ON COLUMN vehicle_quote_rules.updated_at IS '更新时间。';
COMMENT ON COLUMN vehicle_quote_rules.is_deleted IS '是否已删除。';
COMMENT ON COLUMN vehicle_quote_rules.deleted_at IS '删除时间。';
COMMENT ON COLUMN vehicle_quote_rules.deleted_by IS '删除人账号或名称。';
COMMENT ON INDEX uk_vehicle_quote_rules_scope_active IS '座位数报价规则唯一索引，当前约束同一租户同座位数未删除规则。';
COMMENT ON INDEX idx_vehicle_quote_rules_query IS '座位数报价规则查询索引，用于按座位数和状态筛选。';

COMMENT ON TABLE sales_product_vehicle_quote_snapshots IS '销售产品用车报价测算快照表。用于保存产品团队安排用车时的路书公里、命中规则和测算金额。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.id IS '用车报价测算快照主键ID。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.product_id IS '所属销售产品ID。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.arrangement_item_id IS '所属团队安排项目ID。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.schedule_start_day IS '用车开始日期或行程天文本。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.schedule_end_day IS '用车结束日期或行程天文本。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.start_day_no IS '同步路书的开始天数。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.end_day_no IS '同步路书的结束天数。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.synced_distance_meters IS '同步的路书总距离，单位米。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.synced_duration_seconds IS '同步的预计车程，单位秒。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.route_summary IS '同步的路书摘要。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.quote_rule_id IS '测算时命中的座位数报价规则ID。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.rule_vehicle_type IS '测算时规则中的座位数快照。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.rule_province IS '测算时规则中的预留省份快照，当前通常为空。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.rule_city IS '测算时规则中的预留城市快照，当前通常为空。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.rule_district IS '测算时规则中的预留区县快照，当前通常为空。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.rule_base_price IS '测算时规则基础价快照。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.rule_base_kilometers IS '测算时规则基础公里数快照。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.rule_extra_kilometer_price IS '测算时规则超公里单价快照。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.rule_minimum_price IS '测算时规则最低价快照。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.rule_float_rate IS '测算时规则浮动系数快照。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.calculated_amount IS '系统测算参考价。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.confirmed_amount IS '用户确认采用的参考金额。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.remark IS '备注。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_vehicle_quote_snapshots.deleted_by IS '删除人账号或名称。';
COMMENT ON INDEX uk_sales_product_vehicle_quote_item_active IS '用车报价快照唯一索引，仅约束同一安排项一条未删除快照。';
COMMENT ON INDEX idx_sales_product_vehicle_quote_product IS '用车报价快照查询索引，用于产品详情按安排项回显。';

COMMENT ON TABLE sales_product_vehicle_inquiries IS '销售产品用车询价记录表。用于记录业务人员通过微信群、电话等方式向车队询价后的多家报价。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.id IS '用车询价记录主键ID。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.product_id IS '所属销售产品ID。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.arrangement_item_id IS '所属团队安排项目ID。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.sort_order IS '排序号，数字越小越靠前。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.inquiry_method IS '询价方式。wechat_group微信群，enterprise_wechat企业微信，phone电话，other其它。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.inquiry_person IS '询价人。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.inquiry_time IS '询价时间。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.group_name IS '微信群或沟通群名称。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.supplier_id IS '报价供应商ID。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.supplier_name IS '报价供应商名称快照。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.quoted_amount IS '供应商报价金额。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.includes_toll IS '报价是否包含过路费。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.includes_parking IS '报价是否包含停车费。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.includes_driver_meal IS '报价是否包含司机餐费。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.includes_driver_lodging IS '报价是否包含司机住宿。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.available_vehicle_count IS '供应商回复可用车辆数量。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.reply_person IS '供应商回复人。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.reply_time IS '供应商回复时间。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.attachment_id IS '询价截图或附件ID。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.attachment_url IS '询价截图或附件访问地址。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.selected IS '是否选定该报价。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.remark IS '备注。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_vehicle_inquiries.deleted_by IS '删除人账号或名称。';
COMMENT ON INDEX idx_sales_product_vehicle_inquiry_item IS '用车询价记录查询索引，用于产品详情按安排项回显报价列表。';
COMMENT ON INDEX idx_sales_product_vehicle_inquiry_selected IS '用车询价选定报价查询索引。';

COMMENT ON INDEX uk_sales_products_tenant_name_active IS '产品名称唯一索引，仅约束同一租户下未删除产品。';
COMMENT ON INDEX uk_sales_product_itinerary_day_active IS '每日行程唯一索引，仅约束同一产品下未删除行程天数。';
COMMENT ON INDEX uk_sales_product_descriptions_product_active IS '产品说明唯一索引，仅约束同一产品下未删除说明。';
COMMENT ON INDEX uk_sales_product_roadbook_point_order_active IS '路书地点顺序唯一索引，仅约束同一产品同一天未删除地点顺序。';
COMMENT ON INDEX idx_sales_product_arrangement_price_item IS '团队安排价格明细查询索引，用于按产品和安排项回显价格信息。';

COMMENT ON TABLE vehicle_usage_histories IS '用车历史候选表。用于沉淀用车安排中手动输入的司机信息和车牌号。';
COMMENT ON COLUMN vehicle_usage_histories.id IS '用车历史候选主键ID。';
COMMENT ON COLUMN vehicle_usage_histories.tenant_id IS '租户ID。';
COMMENT ON COLUMN vehicle_usage_histories.history_type IS '历史候选类型。driver_info表示司机信息，vehicle_plate表示车牌号。';
COMMENT ON COLUMN vehicle_usage_histories.content IS '页面展示的候选内容。';
COMMENT ON COLUMN vehicle_usage_histories.normalized_content IS '归一化后的候选内容，用于去重。';
COMMENT ON COLUMN vehicle_usage_histories.usage_count IS '使用次数，用于下拉排序。';
COMMENT ON COLUMN vehicle_usage_histories.last_used_at IS '最近使用时间，用于同次数候选排序。';
COMMENT ON COLUMN vehicle_usage_histories.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN vehicle_usage_histories.remark IS '备注。';
COMMENT ON COLUMN vehicle_usage_histories.created_at IS '创建时间。';
COMMENT ON COLUMN vehicle_usage_histories.updated_at IS '更新时间。';
COMMENT ON COLUMN vehicle_usage_histories.is_deleted IS '是否已删除。';
COMMENT ON COLUMN vehicle_usage_histories.deleted_at IS '删除时间。';
COMMENT ON COLUMN vehicle_usage_histories.deleted_by IS '删除人账号或名称。';
COMMENT ON INDEX uk_vehicle_usage_histories_content_active IS '用车历史候选唯一索引，仅约束同一租户同类型未删除候选。';
COMMENT ON INDEX idx_vehicle_usage_histories_suggest IS '用车历史候选推荐索引，用于按类型、使用次数和最近使用时间查询。';

COMMIT;

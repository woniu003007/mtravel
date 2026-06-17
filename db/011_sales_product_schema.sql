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
  CONSTRAINT fk_sales_product_arrangement_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT chk_sales_product_arrangement_type CHECK (
    arrangement_type IN ('traffic', 'hotel', 'vehicle', 'scenic', 'meal', 'other', 'optional', 'shopping', 'ground_agent', 'extra_fee')
  ),
  CONSTRAINT chk_sales_product_arrangement_quantity CHECK (quantity >= 0),
  CONSTRAINT chk_sales_product_arrangement_unit_price CHECK (unit_price >= 0),
  CONSTRAINT chk_sales_product_arrangement_settlement CHECK (settlement_type IN ('cash', 'credit'))
);

DROP TRIGGER IF EXISTS trg_sales_product_arrangement_items_updated_at ON sales_product_arrangement_items;
CREATE TRIGGER trg_sales_product_arrangement_items_updated_at
BEFORE UPDATE ON sales_product_arrangement_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_arrangement_product
  ON sales_product_arrangement_items (tenant_id, is_deleted, product_id, arrangement_type);

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
COMMENT ON COLUMN sales_product_arrangement_items.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_arrangement_items.remark IS '备注。';
COMMENT ON COLUMN sales_product_arrangement_items.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_arrangement_items.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_product_arrangement_items.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_arrangement_items.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_arrangement_items.deleted_by IS '删除人账号或名称。';

COMMENT ON INDEX uk_sales_products_tenant_name_active IS '产品名称唯一索引，仅约束同一租户下未删除产品。';
COMMENT ON INDEX uk_sales_product_itinerary_day_active IS '每日行程唯一索引，仅约束同一产品下未删除行程天数。';
COMMENT ON INDEX uk_sales_product_descriptions_product_active IS '产品说明唯一索引，仅约束同一产品下未删除说明。';
COMMENT ON INDEX uk_sales_product_roadbook_point_order_active IS '路书地点顺序唯一索引，仅约束同一产品同一天未删除地点顺序。';

COMMIT;

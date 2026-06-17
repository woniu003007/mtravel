-- 旅游接待管理系统：计调房源与房态库存
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 仅用于让本脚本可在独立临时库中校验。正式库中 tenants 已由前置脚本创建。
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS controlled_room_resources (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  hotel_name varchar(200) NOT NULL,
  city varchar(80),
  area varchar(120),
  address varchar(300),
  star_standard varchar(80),
  room_type varchar(120),
  purchase_price numeric(14,2) NOT NULL DEFAULT 0,
  agreement_price numeric(14,2) NOT NULL DEFAULT 0,
  price_unit varchar(40) NOT NULL DEFAULT '间夜',
  valid_from date,
  valid_to date,
  contact_name varchar(80),
  contact_phone varchar(40),
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  province varchar(80),
  district varchar(120),
  source_name varchar(200),
  CONSTRAINT chk_controlled_room_resources_price CHECK (purchase_price >= 0 AND agreement_price >= 0),
  CONSTRAINT chk_controlled_room_resources_date CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_to >= valid_from),
  CONSTRAINT chk_controlled_room_resources_status CHECK (status IN ('active', 'disabled', 'expired')),
  CONSTRAINT uk_controlled_room_resources_tenant_id_id UNIQUE (tenant_id, id)
);

ALTER TABLE controlled_room_resources ALTER COLUMN room_type DROP NOT NULL;

CREATE TABLE IF NOT EXISTS controlled_room_types (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_id bigint NOT NULL,
  room_type varchar(120) NOT NULL,
  bed_type varchar(80),
  capacity integer NOT NULL DEFAULT 0,
  purchase_price numeric(14,2) NOT NULL DEFAULT 0,
  agreement_price numeric(14,2) NOT NULL DEFAULT 0,
  price_unit varchar(40) NOT NULL DEFAULT '间夜',
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_controlled_room_types_capacity CHECK (capacity >= 0),
  CONSTRAINT chk_controlled_room_types_price CHECK (purchase_price >= 0 AND agreement_price >= 0),
  CONSTRAINT chk_controlled_room_types_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_controlled_room_types_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_controlled_room_types_resource FOREIGN KEY (tenant_id, resource_id)
    REFERENCES controlled_room_resources (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS controlled_room_units (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_id bigint NOT NULL,
  building_name varchar(80),
  floor_no varchar(40),
  room_no varchar(80) NOT NULL,
  room_type varchar(120),
  bed_type varchar(80),
  capacity integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_controlled_room_units_capacity CHECK (capacity >= 0),
  CONSTRAINT chk_controlled_room_units_status CHECK (status IN ('active', 'disabled', 'maintenance')),
  CONSTRAINT uk_controlled_room_units_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_controlled_room_units_resource FOREIGN KEY (tenant_id, resource_id)
    REFERENCES controlled_room_resources (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS controlled_room_lock_records (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_id bigint,
  room_id bigint,
  check_in_date date NOT NULL,
  check_out_date date NOT NULL,
  team_no varchar(80),
  team_name varchar(200),
  required_standard varchar(80),
  status varchar(20) NOT NULL DEFAULT 'locked',
  released_at timestamptz,
  released_by varchar(64),
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  source_type varchar(30),
  source_id bigint,
  room_type_id bigint,
  room_type varchar(120),
  quantity integer NOT NULL DEFAULT 1,
  CONSTRAINT chk_controlled_room_lock_records_date CHECK (check_out_date > check_in_date),
  CONSTRAINT chk_controlled_room_lock_records_quantity CHECK (quantity > 0),
  CONSTRAINT chk_controlled_room_lock_records_source CHECK (source_type IS NULL OR source_type IN ('self_owned', 'purchased_resource')),
  CONSTRAINT chk_controlled_room_lock_records_status CHECK (status IN ('locked', 'occupied', 'released')),
  CONSTRAINT uk_controlled_room_lock_records_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_controlled_room_lock_records_resource FOREIGN KEY (tenant_id, resource_id)
    REFERENCES controlled_room_resources (tenant_id, id),
  CONSTRAINT fk_controlled_room_lock_records_room FOREIGN KEY (tenant_id, room_id)
    REFERENCES controlled_room_units (tenant_id, id),
  CONSTRAINT fk_controlled_room_lock_records_room_type FOREIGN KEY (tenant_id, room_type_id)
    REFERENCES controlled_room_types (tenant_id, id)
);

ALTER TABLE controlled_room_lock_records ADD COLUMN IF NOT EXISTS source_type varchar(30);
ALTER TABLE controlled_room_lock_records ADD COLUMN IF NOT EXISTS source_id bigint;
ALTER TABLE controlled_room_lock_records ADD COLUMN IF NOT EXISTS room_type_id bigint;
ALTER TABLE controlled_room_lock_records ADD COLUMN IF NOT EXISTS room_type varchar(120);
ALTER TABLE controlled_room_lock_records ADD COLUMN IF NOT EXISTS quantity integer NOT NULL DEFAULT 1;
ALTER TABLE controlled_room_lock_records ALTER COLUMN resource_id DROP NOT NULL;
ALTER TABLE controlled_room_lock_records ALTER COLUMN room_id DROP NOT NULL;
ALTER TABLE controlled_room_lock_records DROP CONSTRAINT IF EXISTS chk_controlled_room_lock_records_quantity;
ALTER TABLE controlled_room_lock_records ADD CONSTRAINT chk_controlled_room_lock_records_quantity CHECK (quantity > 0);
ALTER TABLE controlled_room_lock_records DROP CONSTRAINT IF EXISTS chk_controlled_room_lock_records_source;
ALTER TABLE controlled_room_lock_records ADD CONSTRAINT chk_controlled_room_lock_records_source CHECK (source_type IS NULL OR source_type IN ('self_owned', 'purchased_resource'));
ALTER TABLE controlled_room_lock_records DROP CONSTRAINT IF EXISTS fk_controlled_room_lock_records_room_type;
ALTER TABLE controlled_room_lock_records ADD CONSTRAINT fk_controlled_room_lock_records_room_type FOREIGN KEY (tenant_id, room_type_id)
  REFERENCES controlled_room_types (tenant_id, id);

CREATE TABLE IF NOT EXISTS controlled_room_day_statuses (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_id bigint NOT NULL,
  room_id bigint NOT NULL,
  stay_date date NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'available',
  lock_record_id bigint,
  team_no varchar(80),
  team_name varchar(200),
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_controlled_room_day_statuses_status CHECK (status IN ('available', 'locked', 'occupied', 'maintenance', 'reserved')),
  CONSTRAINT uk_controlled_room_day_statuses_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_controlled_room_day_statuses_resource FOREIGN KEY (tenant_id, resource_id)
    REFERENCES controlled_room_resources (tenant_id, id),
  CONSTRAINT fk_controlled_room_day_statuses_room FOREIGN KEY (tenant_id, room_id)
    REFERENCES controlled_room_units (tenant_id, id),
  CONSTRAINT fk_controlled_room_day_statuses_lock FOREIGN KEY (tenant_id, lock_record_id)
    REFERENCES controlled_room_lock_records (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS room_inventories (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  source_type varchar(30) NOT NULL,
  source_id bigint NOT NULL,
  room_type_id bigint,
  hotel_name varchar(200) NOT NULL,
  supplier_name varchar(200),
  room_type varchar(120) NOT NULL,
  stay_date date NOT NULL,
  total_quantity integer NOT NULL DEFAULT 0,
  locked_quantity integer NOT NULL DEFAULT 0,
  occupied_quantity integer NOT NULL DEFAULT 0,
  remaining_quantity integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_room_inventories_source_type CHECK (source_type IN ('self_owned', 'purchased_resource')),
  CONSTRAINT chk_room_inventories_quantity CHECK (total_quantity >= 0 AND locked_quantity >= 0 AND occupied_quantity >= 0 AND remaining_quantity >= 0),
  CONSTRAINT chk_room_inventories_total CHECK (locked_quantity + occupied_quantity + remaining_quantity <= total_quantity),
  CONSTRAINT chk_room_inventories_status CHECK (status IN ('active', 'stopped')),
  CONSTRAINT uk_room_inventories_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_room_inventories_room_type FOREIGN KEY (tenant_id, room_type_id)
    REFERENCES controlled_room_types (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_controlled_room_resources_updated_at ON controlled_room_resources;
CREATE TRIGGER trg_controlled_room_resources_updated_at
BEFORE UPDATE ON controlled_room_resources
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_controlled_room_types_updated_at ON controlled_room_types;
CREATE TRIGGER trg_controlled_room_types_updated_at
BEFORE UPDATE ON controlled_room_types
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_controlled_room_units_updated_at ON controlled_room_units;
CREATE TRIGGER trg_controlled_room_units_updated_at
BEFORE UPDATE ON controlled_room_units
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_controlled_room_lock_records_updated_at ON controlled_room_lock_records;
CREATE TRIGGER trg_controlled_room_lock_records_updated_at
BEFORE UPDATE ON controlled_room_lock_records
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_controlled_room_day_statuses_updated_at ON controlled_room_day_statuses;
CREATE TRIGGER trg_controlled_room_day_statuses_updated_at
BEFORE UPDATE ON controlled_room_day_statuses
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_room_inventories_updated_at ON room_inventories;
CREATE TRIGGER trg_room_inventories_updated_at
BEFORE UPDATE ON room_inventories
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP INDEX IF EXISTS uk_controlled_room_resources_tenant_hotel_room_period_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_controlled_room_resources_tenant_hotel_period_active
  ON controlled_room_resources (tenant_id, hotel_name, COALESCE(valid_from, DATE '0001-01-01'), COALESCE(valid_to, DATE '9999-12-31'))
  WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_controlled_room_resources_tenant_deleted_city
  ON controlled_room_resources (tenant_id, is_deleted, province, city, district);
DROP INDEX IF EXISTS idx_controlled_room_resources_tenant_deleted_standard;
CREATE INDEX IF NOT EXISTS idx_controlled_room_resources_tenant_deleted_standard
  ON controlled_room_resources (tenant_id, is_deleted, star_standard);
CREATE INDEX IF NOT EXISTS idx_controlled_room_resources_tenant_deleted_status
  ON controlled_room_resources (tenant_id, is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_controlled_room_types_tenant_deleted_resource
  ON controlled_room_types (tenant_id, is_deleted, resource_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_controlled_room_types_tenant_resource_type_active
  ON controlled_room_types (tenant_id, resource_id, room_type)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_controlled_room_units_tenant_deleted_resource
  ON controlled_room_units (tenant_id, is_deleted, resource_id);
CREATE INDEX IF NOT EXISTS idx_controlled_room_units_tenant_deleted_status
  ON controlled_room_units (tenant_id, is_deleted, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_controlled_room_units_tenant_resource_room_active
  ON controlled_room_units (tenant_id, resource_id, room_no)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_controlled_room_day_statuses_tenant_deleted_date
  ON controlled_room_day_statuses (tenant_id, is_deleted, stay_date, status);
CREATE INDEX IF NOT EXISTS idx_controlled_room_day_statuses_tenant_deleted_resource_date
  ON controlled_room_day_statuses (tenant_id, is_deleted, resource_id, stay_date);
CREATE INDEX IF NOT EXISTS idx_controlled_room_day_statuses_tenant_deleted_lock
  ON controlled_room_day_statuses (tenant_id, is_deleted, lock_record_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_controlled_room_day_statuses_tenant_room_date_active
  ON controlled_room_day_statuses (tenant_id, room_id, stay_date)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_controlled_room_lock_records_tenant_deleted_resource
  ON controlled_room_lock_records (tenant_id, is_deleted, resource_id, status);
CREATE INDEX IF NOT EXISTS idx_controlled_room_lock_records_tenant_deleted_source
  ON controlled_room_lock_records (tenant_id, is_deleted, source_type, source_id, room_type, status);
CREATE INDEX IF NOT EXISTS idx_controlled_room_lock_records_tenant_deleted_team
  ON controlled_room_lock_records (tenant_id, is_deleted, team_no);

CREATE INDEX IF NOT EXISTS idx_room_inventories_tenant_deleted_date
  ON room_inventories (tenant_id, is_deleted, stay_date, status);
CREATE INDEX IF NOT EXISTS idx_room_inventories_tenant_deleted_source
  ON room_inventories (tenant_id, is_deleted, source_type, source_id, room_type, stay_date);
CREATE UNIQUE INDEX IF NOT EXISTS uk_room_inventories_tenant_source_type_date_active
  ON room_inventories (tenant_id, source_type, source_id, COALESCE(room_type_id, 0), room_type, stay_date)
  WHERE is_deleted = false;

COMMENT ON TABLE controlled_room_resources IS '自营酒店房源档案表。用于维护企业已买断、包房或长期协议控制的酒店、所在地、住宿标准和有效期。';
COMMENT ON COLUMN controlled_room_resources.id IS '自营酒店房源主键ID，系统内部使用。';
COMMENT ON COLUMN controlled_room_resources.tenant_id IS '租户ID，标识该房源属于哪一家地接公司。';
COMMENT ON COLUMN controlled_room_resources.hotel_name IS '酒店名称。';
COMMENT ON COLUMN controlled_room_resources.province IS '酒店所在省份。';
COMMENT ON COLUMN controlled_room_resources.city IS '酒店所在城市。';
COMMENT ON COLUMN controlled_room_resources.district IS '酒店所在区县。';
COMMENT ON COLUMN controlled_room_resources.area IS '酒店所在区域或商圈。';
COMMENT ON COLUMN controlled_room_resources.address IS '酒店详细地址。';
COMMENT ON COLUMN controlled_room_resources.star_standard IS '星级、钻级或内部住宿标准，后续排房按该字段匹配团队住宿标准。';
COMMENT ON COLUMN controlled_room_resources.room_type IS '历史兼容房型字段。新业务房型维护在自营房型表。';
COMMENT ON COLUMN controlled_room_resources.source_name IS '房源来源或酒店方名称，普通文本，不关联采购供应商。';
COMMENT ON COLUMN controlled_room_resources.purchase_price IS '历史兼容采购价字段。新业务采购价维护在自营房型表。';
COMMENT ON COLUMN controlled_room_resources.agreement_price IS '历史兼容协议价字段。新业务协议价维护在自营房型表。';
COMMENT ON COLUMN controlled_room_resources.price_unit IS '历史兼容价格单位字段。新业务价格单位维护在自营房型表。';
COMMENT ON COLUMN controlled_room_resources.valid_from IS '房源使用权有效期开始日期。';
COMMENT ON COLUMN controlled_room_resources.valid_to IS '房源使用权有效期结束日期。';
COMMENT ON COLUMN controlled_room_resources.contact_name IS '房源联系人姓名。';
COMMENT ON COLUMN controlled_room_resources.contact_phone IS '房源联系人电话。';
COMMENT ON COLUMN controlled_room_resources.status IS '自营酒店房源状态。active启用，disabled停用，expired到期。';
COMMENT ON COLUMN controlled_room_resources.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN controlled_room_resources.remark IS '备注，用于记录房源控制说明。';
COMMENT ON COLUMN controlled_room_resources.created_at IS '创建时间。';
COMMENT ON COLUMN controlled_room_resources.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN controlled_room_resources.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN controlled_room_resources.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN controlled_room_resources.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE controlled_room_types IS '自营房型表。用于维护自营酒店下的标间、大床房、三人间等房型、床型、可住人数和价格。';
COMMENT ON COLUMN controlled_room_types.id IS '自营房型主键ID，系统内部使用。';
COMMENT ON COLUMN controlled_room_types.tenant_id IS '租户ID，标识该房型属于哪一家地接公司。';
COMMENT ON COLUMN controlled_room_types.resource_id IS '所属自营酒店房源ID。';
COMMENT ON COLUMN controlled_room_types.room_type IS '房型名称。';
COMMENT ON COLUMN controlled_room_types.bed_type IS '床型。';
COMMENT ON COLUMN controlled_room_types.capacity IS '可住人数。';
COMMENT ON COLUMN controlled_room_types.purchase_price IS '采购价，单位元。';
COMMENT ON COLUMN controlled_room_types.agreement_price IS '协议价，单位元。';
COMMENT ON COLUMN controlled_room_types.price_unit IS '价格单位，默认间夜。';
COMMENT ON COLUMN controlled_room_types.status IS '房型状态。active启用，disabled停用。';
COMMENT ON COLUMN controlled_room_types.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN controlled_room_types.remark IS '房型备注。';
COMMENT ON COLUMN controlled_room_types.created_at IS '创建时间。';
COMMENT ON COLUMN controlled_room_types.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN controlled_room_types.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN controlled_room_types.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN controlled_room_types.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE controlled_room_units IS '自营房号明细表。用于维护自营房源下的具体房号；资源采购房源不维护房号。';
COMMENT ON COLUMN controlled_room_units.id IS '自营房号主键ID，系统内部使用。';
COMMENT ON COLUMN controlled_room_units.tenant_id IS '租户ID，标识该房号属于哪一家地接公司。';
COMMENT ON COLUMN controlled_room_units.resource_id IS '所属自营酒店房源ID。';
COMMENT ON COLUMN controlled_room_units.building_name IS '楼栋名称或编号。';
COMMENT ON COLUMN controlled_room_units.floor_no IS '楼层。';
COMMENT ON COLUMN controlled_room_units.room_no IS '房号。';
COMMENT ON COLUMN controlled_room_units.room_type IS '房型名称快照。';
COMMENT ON COLUMN controlled_room_units.bed_type IS '床型。';
COMMENT ON COLUMN controlled_room_units.capacity IS '可住人数。';
COMMENT ON COLUMN controlled_room_units.status IS '房号状态。active启用，disabled停用，maintenance维修。';
COMMENT ON COLUMN controlled_room_units.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN controlled_room_units.remark IS '房号备注。';
COMMENT ON COLUMN controlled_room_units.created_at IS '创建时间。';
COMMENT ON COLUMN controlled_room_units.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN controlled_room_units.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN controlled_room_units.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN controlled_room_units.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE controlled_room_lock_records IS '房态锁房流水表。用于记录自营房源或资源采购房源按房型数量锁房、释放和转占用过程。';
COMMENT ON COLUMN controlled_room_lock_records.id IS '锁房流水主键ID，系统内部使用。';
COMMENT ON COLUMN controlled_room_lock_records.tenant_id IS '租户ID，标识该锁房流水属于哪一家地接公司。';
COMMENT ON COLUMN controlled_room_lock_records.resource_id IS '自营酒店房源ID。资源采购房源或纯数量锁房可为空。';
COMMENT ON COLUMN controlled_room_lock_records.source_type IS '房源来源类型。self_owned自营房源，purchased_resource资源采购房源。';
COMMENT ON COLUMN controlled_room_lock_records.source_id IS '房源来源ID。自营房源为自营酒店房源ID，资源采购房源为采购关系ID。';
COMMENT ON COLUMN controlled_room_lock_records.room_type_id IS '自营房型ID。资源采购房源或历史房号锁房记录可为空。';
COMMENT ON COLUMN controlled_room_lock_records.room_type IS '房型名称快照。';
COMMENT ON COLUMN controlled_room_lock_records.quantity IS '锁房数量。按房型数量锁房时使用。';
COMMENT ON COLUMN controlled_room_lock_records.room_id IS '具体房号ID。按房型数量锁房时为空，仅后续精细排房号时使用。';
COMMENT ON COLUMN controlled_room_lock_records.check_in_date IS '入住日期，包含当天。';
COMMENT ON COLUMN controlled_room_lock_records.check_out_date IS '退房日期，不包含当天。';
COMMENT ON COLUMN controlled_room_lock_records.team_no IS '团队编号快照。';
COMMENT ON COLUMN controlled_room_lock_records.team_name IS '团队名称快照。';
COMMENT ON COLUMN controlled_room_lock_records.required_standard IS '团队住宿标准，用于和房源星钻标准做差异提醒。';
COMMENT ON COLUMN controlled_room_lock_records.status IS '锁房状态。locked已锁定，occupied已转占用，released已释放。';
COMMENT ON COLUMN controlled_room_lock_records.released_at IS '释放时间。未释放时为空。';
COMMENT ON COLUMN controlled_room_lock_records.released_by IS '释放人账号或名称。';
COMMENT ON COLUMN controlled_room_lock_records.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN controlled_room_lock_records.remark IS '锁房备注。';
COMMENT ON COLUMN controlled_room_lock_records.created_at IS '创建时间。';
COMMENT ON COLUMN controlled_room_lock_records.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN controlled_room_lock_records.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN controlled_room_lock_records.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN controlled_room_lock_records.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE controlled_room_day_statuses IS '自营房号每日状态表。用于兼容按具体房号维护可用、锁定、占用、维修和保留状态。';
COMMENT ON COLUMN controlled_room_day_statuses.id IS '每日房号状态主键ID，系统内部使用。';
COMMENT ON COLUMN controlled_room_day_statuses.tenant_id IS '租户ID，标识该房号状态属于哪一家地接公司。';
COMMENT ON COLUMN controlled_room_day_statuses.resource_id IS '自营酒店房源ID。';
COMMENT ON COLUMN controlled_room_day_statuses.room_id IS '具体房号ID。';
COMMENT ON COLUMN controlled_room_day_statuses.stay_date IS '住宿日期。';
COMMENT ON COLUMN controlled_room_day_statuses.status IS '房号状态。available可用，locked已锁定，occupied已占用，maintenance维修，reserved保留。';
COMMENT ON COLUMN controlled_room_day_statuses.lock_record_id IS '当前锁房流水ID。';
COMMENT ON COLUMN controlled_room_day_statuses.team_no IS '当前锁定或占用团队编号快照。';
COMMENT ON COLUMN controlled_room_day_statuses.team_name IS '当前锁定或占用团队名称快照。';
COMMENT ON COLUMN controlled_room_day_statuses.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN controlled_room_day_statuses.remark IS '每日房号状态备注。';
COMMENT ON COLUMN controlled_room_day_statuses.created_at IS '创建时间。';
COMMENT ON COLUMN controlled_room_day_statuses.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN controlled_room_day_statuses.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN controlled_room_day_statuses.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN controlled_room_day_statuses.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE room_inventories IS '房态聚合库存表。用于按酒店来源、房型和住宿日期维护总量、已锁、已占和余量。';
COMMENT ON COLUMN room_inventories.id IS '房态库存主键ID，系统内部使用。';
COMMENT ON COLUMN room_inventories.tenant_id IS '租户ID，标识该库存属于哪一家地接公司。';
COMMENT ON COLUMN room_inventories.source_type IS '房源来源类型。self_owned自营房源，purchased_resource资源采购房源。';
COMMENT ON COLUMN room_inventories.source_id IS '房源来源ID。自营房源为自营酒店房源ID，资源采购房源为采购关系ID。';
COMMENT ON COLUMN room_inventories.room_type_id IS '自营房型ID。资源采购房源为空。';
COMMENT ON COLUMN room_inventories.hotel_name IS '酒店名称快照。';
COMMENT ON COLUMN room_inventories.supplier_name IS '供应商名称快照，自营房源可为空。';
COMMENT ON COLUMN room_inventories.room_type IS '房型名称快照。';
COMMENT ON COLUMN room_inventories.stay_date IS '住宿日期。';
COMMENT ON COLUMN room_inventories.total_quantity IS '总库存间数。';
COMMENT ON COLUMN room_inventories.locked_quantity IS '已锁定间数。';
COMMENT ON COLUMN room_inventories.occupied_quantity IS '已占用间数。';
COMMENT ON COLUMN room_inventories.remaining_quantity IS '剩余可用间数。';
COMMENT ON COLUMN room_inventories.status IS '库存状态。active有效，stopped停售。';
COMMENT ON COLUMN room_inventories.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN room_inventories.remark IS '库存备注。';
COMMENT ON COLUMN room_inventories.created_at IS '创建时间。';
COMMENT ON COLUMN room_inventories.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN room_inventories.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN room_inventories.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN room_inventories.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_controlled_room_resources_tenant_hotel_period_active IS '自营酒店房源唯一索引，仅约束未删除记录。';
COMMENT ON INDEX uk_controlled_room_types_tenant_resource_type_active IS '自营房型唯一索引，仅约束同一自营酒店下未删除房型。';
COMMENT ON INDEX uk_controlled_room_units_tenant_resource_room_active IS '自营房号唯一索引，仅约束同一自营酒店下未删除房号。';
COMMENT ON INDEX uk_controlled_room_day_statuses_tenant_room_date_active IS '自营房号每日状态唯一索引，仅约束同一房号同一日期未删除记录。';
COMMENT ON INDEX uk_room_inventories_tenant_source_type_date_active IS '房态聚合库存唯一索引，仅约束同一来源、房型和日期未删除记录。';

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM tenants WHERE id = 1) THEN
    INSERT INTO controlled_room_resources (
      tenant_id, hotel_name, province, city, district, area, address, star_standard,
      source_name, purchase_price, agreement_price, price_unit, valid_from, valid_to,
      contact_name, contact_phone, status, created_by, remark
    )
    SELECT 1, '苏州中心自营房源', '江苏省', '苏州市', '工业园区', NULL, '苏州工业园区星港街', '四钻',
           '苏州中心酒店', 0, 0, '间夜', DATE '2026-01-01', DATE '2026-12-31',
           '周经理', '0512-66660001', 'active', 'admin', '年度自营房源'
    WHERE NOT EXISTS (
      SELECT 1 FROM controlled_room_resources
      WHERE tenant_id = 1 AND hotel_name = '苏州中心自营房源' AND is_deleted = false
    );

    INSERT INTO controlled_room_types (
      tenant_id, resource_id, room_type, bed_type, capacity, purchase_price, agreement_price, price_unit, status, created_by, remark
    )
    SELECT 1, r.id, types.room_type, types.bed_type, types.capacity, types.purchase_price, types.agreement_price, '间夜', 'active', 'admin', types.remark
    FROM controlled_room_resources r
    CROSS JOIN (VALUES
      ('标准间', '双床', 2, 420::numeric, 520::numeric, '自营标准间'),
      ('大床房', '大床', 2, 460::numeric, 560::numeric, '自营大床房'),
      ('三人间', '三床', 3, 620::numeric, 760::numeric, '自营三人间')
    ) AS types(room_type, bed_type, capacity, purchase_price, agreement_price, remark)
    WHERE r.tenant_id = 1
      AND r.hotel_name = '苏州中心自营房源'
      AND r.is_deleted = false
      AND NOT EXISTS (
        SELECT 1 FROM controlled_room_types t
        WHERE t.tenant_id = 1 AND t.resource_id = r.id AND t.room_type = types.room_type AND t.is_deleted = false
      );

    INSERT INTO controlled_room_units (
      tenant_id, resource_id, building_name, floor_no, room_no, room_type, bed_type, capacity, status, created_by
    )
    SELECT 1, r.id, 'A座', '8F', rooms.room_no, rooms.room_type, rooms.bed_type, rooms.capacity, 'active', 'admin'
    FROM controlled_room_resources r
    CROSS JOIN (VALUES
      ('801', '标准间', '双床', 2),
      ('802', '标准间', '双床', 2),
      ('803', '大床房', '大床', 2),
      ('805', '三人间', '三床', 3)
    ) AS rooms(room_no, room_type, bed_type, capacity)
    WHERE r.tenant_id = 1
      AND r.hotel_name = '苏州中心自营房源'
      AND r.is_deleted = false
      AND NOT EXISTS (
        SELECT 1 FROM controlled_room_units u
        WHERE u.tenant_id = 1 AND u.resource_id = r.id AND u.room_no = rooms.room_no AND u.is_deleted = false
      );
  END IF;
END $$;

COMMIT;

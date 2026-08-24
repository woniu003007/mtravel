-- 旅游接待管理系统：销售产品地图式设计工作台
-- PostgreSQL
-- 本脚本只定义本地结构；执行迁移前必须确认目标环境和数据库，并做线上/本地结构一致性检查。

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS sales_product_day_resources (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  day_no integer NOT NULL,
  resource_id bigint NOT NULL,
  resource_name_snapshot varchar(200) NOT NULL,
  resource_type_snapshot varchar(30) NOT NULL,
  arrangement_role varchar(20) NOT NULL DEFAULT 'itinerary',
  hotel_breakfast_included boolean NOT NULL DEFAULT false,
  province_snapshot varchar(80),
  city_snapshot varchar(80),
  district_snapshot varchar(80),
  address_snapshot varchar(300),
  longitude_snapshot numeric(10,7),
  latitude_snapshot numeric(9,7),
  procurement_mode_snapshot varchar(20) NOT NULL DEFAULT 'required',
  sort_order integer NOT NULL DEFAULT 1,
  stay_minutes integer NOT NULL DEFAULT 0,
  include_in_word boolean NOT NULL DEFAULT true,
  supplier_relation_id_snapshot bigint,
  supplier_id bigint,
  supplier_name_snapshot varchar(200),
  price_mode_snapshot varchar(20),
  unit_price_snapshot numeric(12,2) NOT NULL DEFAULT 0,
  quantity_snapshot numeric(12,2) NOT NULL DEFAULT 1,
  cost_amount_snapshot numeric(12,2) NOT NULL DEFAULT 0,
  selected_introduction_id bigint,
  introduction_index_version integer,
  introduction_title_snapshot varchar(160),
  introduction_content_snapshot text,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_sales_product_day_resources_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_sales_product_day_resources_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resources_resource
    FOREIGN KEY (tenant_id, resource_id) REFERENCES purchase_resources(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resources_supplier
    FOREIGN KEY (tenant_id, supplier_id) REFERENCES suppliers(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resources_supplier_relation
    FOREIGN KEY (tenant_id, supplier_relation_id_snapshot) REFERENCES purchase_relations(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resources_intro
    FOREIGN KEY (tenant_id, selected_introduction_id) REFERENCES purchase_resource_introductions(tenant_id, id),
  CONSTRAINT chk_sales_product_day_resources_day CHECK (day_no >= 1),
  CONSTRAINT chk_sales_product_day_resources_sort CHECK (sort_order >= 1),
  CONSTRAINT chk_sales_product_day_resources_stay CHECK (stay_minutes >= 0),
  CONSTRAINT chk_sales_product_day_resources_procurement CHECK (procurement_mode_snapshot IN ('required', 'not_required')),
  CONSTRAINT chk_sales_product_day_resources_type CHECK (
    resource_type_snapshot IN ('scenic', 'hotel', 'restaurant', 'shopping', 'vehicle', 'traffic', 'ground_agent', 'other')
  ),
  CONSTRAINT chk_sales_product_day_resources_arrangement_role CHECK (
    (resource_type_snapshot = 'hotel' AND arrangement_role = 'accommodation')
    OR (resource_type_snapshot = 'restaurant' AND arrangement_role IN ('unassigned', 'breakfast', 'lunch', 'dinner'))
    OR (resource_type_snapshot = 'ground_agent' AND arrangement_role = 'ground_service')
    OR (resource_type_snapshot IN ('scenic', 'shopping', 'other') AND arrangement_role = 'itinerary')
    OR (resource_type_snapshot IN ('vehicle', 'traffic') AND arrangement_role = 'itinerary')
  ),
  CONSTRAINT chk_sales_product_day_resources_hotel_breakfast CHECK (
    arrangement_role = 'accommodation' OR hotel_breakfast_included = false
  ),
  CONSTRAINT chk_sales_product_day_resources_lnglat_pair CHECK (
    (longitude_snapshot IS NULL AND latitude_snapshot IS NULL)
    OR (longitude_snapshot IS NOT NULL AND latitude_snapshot IS NOT NULL)
  ),
  CONSTRAINT chk_sales_product_day_resources_lng CHECK (longitude_snapshot IS NULL OR longitude_snapshot BETWEEN -180 AND 180),
  CONSTRAINT chk_sales_product_day_resources_lat CHECK (latitude_snapshot IS NULL OR latitude_snapshot BETWEEN -90 AND 90),
  CONSTRAINT chk_sales_product_day_resources_money CHECK (
    unit_price_snapshot >= 0 AND quantity_snapshot >= 0 AND cost_amount_snapshot >= 0
  ),
  CONSTRAINT chk_sales_product_day_resources_free_supplier CHECK (
    procurement_mode_snapshot <> 'not_required' OR supplier_id IS NULL
  ),
  CONSTRAINT chk_sales_product_day_resources_price_mode CHECK (
    price_mode_snapshot IS NULL OR price_mode_snapshot IN ('unified', 'classified', 'pending', 'not_required')
  )
);

DROP TRIGGER IF EXISTS trg_sales_product_day_resources_updated_at ON sales_product_day_resources;
CREATE TRIGGER trg_sales_product_day_resources_updated_at
BEFORE UPDATE ON sales_product_day_resources
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_day_resources_product_day
  ON sales_product_day_resources (tenant_id, is_deleted, product_id, day_no, sort_order, id);
CREATE INDEX IF NOT EXISTS idx_sales_product_day_resources_resource
  ON sales_product_day_resources (tenant_id, is_deleted, resource_id);
CREATE INDEX IF NOT EXISTS idx_sales_product_day_resources_supplier
  ON sales_product_day_resources (tenant_id, is_deleted, supplier_id)
  WHERE supplier_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sales_product_day_resources_supplier_relation
  ON sales_product_day_resources (tenant_id, is_deleted, supplier_relation_id_snapshot)
  WHERE supplier_relation_id_snapshot IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_day_resources_day_resource_role_active
  ON sales_product_day_resources (tenant_id, product_id, day_no, resource_id, arrangement_role)
  WHERE is_deleted = false AND arrangement_role <> 'ground_service';
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_day_resources_day_meal_role_active
  ON sales_product_day_resources (tenant_id, product_id, day_no, arrangement_role)
  WHERE is_deleted = false AND arrangement_role IN ('accommodation', 'breakfast', 'lunch', 'dinner');
CREATE INDEX IF NOT EXISTS idx_sales_product_day_resources_product_day_role_sort
  ON sales_product_day_resources (tenant_id, is_deleted, product_id, day_no, arrangement_role, sort_order, id);

CREATE TABLE IF NOT EXISTS sales_product_designer_vehicle_arrangements (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  resource_id bigint,
  resource_name_snapshot varchar(200),
  supplier_relation_id_snapshot bigint,
  supplier_id bigint,
  supplier_name_snapshot varchar(200),
  price_mode_snapshot varchar(20) NOT NULL DEFAULT 'pending',
  vehicle_type_snapshot varchar(120) NOT NULL,
  start_day_no integer,
  end_day_no integer,
  quantity_snapshot numeric(12,2) NOT NULL DEFAULT 1,
  unit_price_snapshot numeric(12,2) NOT NULL DEFAULT 0,
  cost_amount_snapshot numeric(12,2) NOT NULL DEFAULT 0,
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_sales_product_designer_vehicle_arrangements_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_sales_product_designer_vehicle_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT fk_sales_product_designer_vehicle_resource
    FOREIGN KEY (tenant_id, resource_id) REFERENCES purchase_resources (tenant_id, id),
  CONSTRAINT fk_sales_product_designer_vehicle_supplier_relation
    FOREIGN KEY (tenant_id, supplier_relation_id_snapshot) REFERENCES purchase_relations (tenant_id, id),
  CONSTRAINT fk_sales_product_designer_vehicle_supplier
    FOREIGN KEY (tenant_id, supplier_id) REFERENCES suppliers (tenant_id, id),
  CONSTRAINT chk_sales_product_designer_vehicle_price_mode CHECK (
    price_mode_snapshot IN ('unified', 'classified', 'pending', 'not_required')
  ),
  CONSTRAINT chk_sales_product_designer_vehicle_day_range CHECK (
    (start_day_no IS NULL AND end_day_no IS NULL)
    OR (start_day_no IS NOT NULL AND end_day_no IS NOT NULL AND start_day_no >= 1 AND end_day_no >= start_day_no)
  ),
  CONSTRAINT chk_sales_product_designer_vehicle_quantity_money CHECK (
    quantity_snapshot >= 0 AND unit_price_snapshot >= 0 AND cost_amount_snapshot >= 0
  ),
  CONSTRAINT chk_sales_product_designer_vehicle_sort CHECK (sort_order >= 1)
);

DROP TRIGGER IF EXISTS trg_sales_product_designer_vehicle_arrangements_updated_at
  ON sales_product_designer_vehicle_arrangements;
CREATE TRIGGER trg_sales_product_designer_vehicle_arrangements_updated_at
BEFORE UPDATE ON sales_product_designer_vehicle_arrangements
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_designer_vehicle_product
  ON sales_product_designer_vehicle_arrangements (tenant_id, is_deleted, product_id, sort_order, id);
CREATE INDEX IF NOT EXISTS idx_sales_product_designer_vehicle_resource
  ON sales_product_designer_vehicle_arrangements (tenant_id, is_deleted, resource_id)
  WHERE resource_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sales_product_designer_vehicle_supplier_relation
  ON sales_product_designer_vehicle_arrangements (tenant_id, is_deleted, supplier_relation_id_snapshot)
  WHERE supplier_relation_id_snapshot IS NOT NULL;

CREATE TABLE IF NOT EXISTS sales_product_day_resource_images (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  day_resource_id bigint NOT NULL,
  resource_image_id bigint NOT NULL,
  attachment_id bigint NOT NULL,
  original_filename_snapshot varchar(255) NOT NULL,
  caption_snapshot varchar(300),
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_sales_product_day_resource_images_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_images_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_images_day_resource
    FOREIGN KEY (tenant_id, day_resource_id) REFERENCES sales_product_day_resources(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_images_image
    FOREIGN KEY (tenant_id, resource_image_id) REFERENCES purchase_resource_images(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_images_attachment
    FOREIGN KEY (tenant_id, attachment_id) REFERENCES common_attachments(tenant_id, id),
  CONSTRAINT chk_sales_product_day_resource_images_sort CHECK (sort_order >= 1)
);

DROP TRIGGER IF EXISTS trg_sales_product_day_resource_images_updated_at ON sales_product_day_resource_images;
CREATE TRIGGER trg_sales_product_day_resource_images_updated_at
BEFORE UPDATE ON sales_product_day_resource_images
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_day_resource_images_day_resource
  ON sales_product_day_resource_images (tenant_id, is_deleted, day_resource_id, sort_order, id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_day_resource_images_active
  ON sales_product_day_resource_images (tenant_id, day_resource_id, resource_image_id)
  WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS sales_product_adult_quotes (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  planned_adult_count integer NOT NULL,
  adult_cost_amount numeric(12,2) NOT NULL DEFAULT 0,
  markup_amount numeric(12,2) NOT NULL DEFAULT 0,
  adult_sale_amount numeric(12,2) NOT NULL DEFAULT 0,
  valid_until date,
  quote_remark text,
  status varchar(20) NOT NULL DEFAULT 'draft',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_sales_product_adult_quotes_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_sales_product_adult_quotes_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products(tenant_id, id),
  CONSTRAINT chk_sales_product_adult_quotes_count CHECK (planned_adult_count >= 1),
  CONSTRAINT chk_sales_product_adult_quotes_money CHECK (
    adult_cost_amount >= 0 AND markup_amount >= 0 AND adult_sale_amount >= 0
  ),
  CONSTRAINT chk_sales_product_adult_quotes_status CHECK (status IN ('draft', 'confirmed'))
);

DROP TRIGGER IF EXISTS trg_sales_product_adult_quotes_updated_at ON sales_product_adult_quotes;
CREATE TRIGGER trg_sales_product_adult_quotes_updated_at
BEFORE UPDATE ON sales_product_adult_quotes
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_adult_quotes_product
  ON sales_product_adult_quotes (tenant_id, is_deleted, product_id, status, id DESC);

CREATE TABLE IF NOT EXISTS sales_product_document_versions (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  document_type varchar(30) NOT NULL,
  version_no integer NOT NULL,
  source_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
  attachment_id bigint,
  file_name_snapshot varchar(255),
  generate_status varchar(20) NOT NULL DEFAULT 'pending',
  generated_by varchar(80),
  generated_at timestamptz,
  error_message varchar(500),
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_sales_product_document_versions_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_sales_product_document_versions_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products(tenant_id, id),
  CONSTRAINT fk_sales_product_document_versions_attachment
    FOREIGN KEY (tenant_id, attachment_id) REFERENCES common_attachments(tenant_id, id),
  CONSTRAINT chk_sales_product_document_versions_type CHECK (document_type IN ('product_word', 'adult_quote')),
  CONSTRAINT chk_sales_product_document_versions_no CHECK (version_no >= 1),
  CONSTRAINT chk_sales_product_document_versions_snapshot CHECK (jsonb_typeof(source_snapshot) = 'object'),
  CONSTRAINT chk_sales_product_document_versions_status CHECK (generate_status IN ('pending', 'success', 'failed'))
);

DROP TRIGGER IF EXISTS trg_sales_product_document_versions_updated_at ON sales_product_document_versions;
CREATE TRIGGER trg_sales_product_document_versions_updated_at
BEFORE UPDATE ON sales_product_document_versions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_document_versions_product
  ON sales_product_document_versions (tenant_id, is_deleted, product_id, document_type, version_no DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_document_versions_product_type_version_active
  ON sales_product_document_versions (tenant_id, product_id, document_type, version_no)
  WHERE is_deleted = false;

COMMENT ON TABLE sales_product_day_resources IS '销售产品每日资源编排表，用于保存产品设计工作台中某一天选用的采购资源及其成本、供应商和介绍快照。';
COMMENT ON COLUMN sales_product_day_resources.id IS '每日资源编排主键ID。';
COMMENT ON COLUMN sales_product_day_resources.tenant_id IS '租户ID，用于隔离不同地接公司的产品设计数据。';
COMMENT ON COLUMN sales_product_day_resources.product_id IS '销售产品模板ID。';
COMMENT ON COLUMN sales_product_day_resources.day_no IS '产品行程第几天，从1开始。';
COMMENT ON COLUMN sales_product_day_resources.resource_id IS '采购资源主档ID。';
COMMENT ON COLUMN sales_product_day_resources.resource_name_snapshot IS '资源名称快照，用于产品行程和生成文件历史展示。';
COMMENT ON COLUMN sales_product_day_resources.resource_type_snapshot IS '资源类型快照。';
COMMENT ON COLUMN sales_product_day_resources.arrangement_role IS '资源在当天的编排归属。酒店为accommodation；餐厅为breakfast、lunch、dinner或历史unassigned；地接为ground_service；景区、购物及其它为itinerary；历史用车和交通仅兼容保留为itinerary。';
COMMENT ON COLUMN sales_product_day_resources.hotel_breakfast_included IS '当晚住宿酒店是否包含次日早餐，仅住宿酒店可设置。';
COMMENT ON COLUMN sales_product_day_resources.province_snapshot IS '资源所在省份快照。';
COMMENT ON COLUMN sales_product_day_resources.city_snapshot IS '资源所在城市快照。';
COMMENT ON COLUMN sales_product_day_resources.district_snapshot IS '资源所在区县快照。';
COMMENT ON COLUMN sales_product_day_resources.address_snapshot IS '资源详细地址快照。';
COMMENT ON COLUMN sales_product_day_resources.longitude_snapshot IS '高德GCJ-02经度快照。';
COMMENT ON COLUMN sales_product_day_resources.latitude_snapshot IS '高德GCJ-02纬度快照。';
COMMENT ON COLUMN sales_product_day_resources.procurement_mode_snapshot IS '资源采购属性快照。required表示需要采购，not_required表示无需采购。';
COMMENT ON COLUMN sales_product_day_resources.sort_order IS '当天资源排序号。';
COMMENT ON COLUMN sales_product_day_resources.stay_minutes IS '计划停留时长，单位分钟。';
COMMENT ON COLUMN sales_product_day_resources.include_in_word IS '是否纳入产品Word行程输出。';
COMMENT ON COLUMN sales_product_day_resources.supplier_relation_id_snapshot IS '保存时选中的采购关系ID快照，用于区分同一供应商的不同资源报价关系。';
COMMENT ON COLUMN sales_product_day_resources.supplier_id IS '当时选择的供应商ID。无需采购资源为空。';
COMMENT ON COLUMN sales_product_day_resources.supplier_name_snapshot IS '供应商名称快照，只用于内部成本和回显，不输出到对外产品文件。';
COMMENT ON COLUMN sales_product_day_resources.price_mode_snapshot IS '保存时采购关系报价模式快照。unified统一报价，classified分类报价，pending表示待询价，not_required表示无需采购。';
COMMENT ON COLUMN sales_product_day_resources.unit_price_snapshot IS '后端计算的单位成本快照。';
COMMENT ON COLUMN sales_product_day_resources.quantity_snapshot IS '成本数量快照。';
COMMENT ON COLUMN sales_product_day_resources.cost_amount_snapshot IS '后端计算的成本小计快照。';
COMMENT ON COLUMN sales_product_day_resources.selected_introduction_id IS '选中的已发布资源介绍ID。';
COMMENT ON COLUMN sales_product_day_resources.introduction_index_version IS '资源介绍向量索引版本快照，用于追踪介绍版本。';
COMMENT ON COLUMN sales_product_day_resources.introduction_title_snapshot IS '资源介绍标题快照。';
COMMENT ON COLUMN sales_product_day_resources.introduction_content_snapshot IS '资源介绍正文快照，用于生成产品Word。';
COMMENT ON COLUMN sales_product_day_resources.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_day_resources.remark IS '产品设计资源备注。';
COMMENT ON COLUMN sales_product_day_resources.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_day_resources.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_product_day_resources.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN sales_product_day_resources.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_day_resources.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_product_designer_vehicle_arrangements IS '销售产品设计全程用车编排表，保存产品级用车资源、供应商采购关系和成本快照，不归属单个行程日。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.id IS '全程用车编排主键ID。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.tenant_id IS '租户ID，用于隔离不同地接公司的产品设计数据。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.product_id IS '销售产品设计草稿或产品模板ID。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.resource_id IS '车辆采购资源主档ID，允许为空以兼容待确认的用车安排。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.resource_name_snapshot IS '车辆资源名称快照。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.supplier_relation_id_snapshot IS '保存时选中的车辆采购关系ID快照。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.supplier_id IS '保存时选中的供应商ID快照。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.supplier_name_snapshot IS '供应商名称快照，仅用于内部成本回显。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.price_mode_snapshot IS '采购关系报价模式快照。unified统一报价，classified分类报价，pending表示待询价，not_required表示无需采购。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.vehicle_type_snapshot IS '车辆车型或座位数快照。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.start_day_no IS '用车开始行程日次，未划分日段时为空。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.end_day_no IS '用车结束行程日次，未划分日段时为空。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.quantity_snapshot IS '保存时用车数量快照。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.unit_price_snapshot IS '后端计算的单位成本快照。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.cost_amount_snapshot IS '后端计算的全程用车成本小计快照。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.sort_order IS '产品级全程用车编排排序号。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.remark IS '全程用车内部备注。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_designer_vehicle_arrangements.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_product_day_resource_images IS '销售产品每日资源配图表，用于保存生成产品Word时选用的资源图片快照。';
COMMENT ON COLUMN sales_product_day_resource_images.id IS '资源配图主键ID。';
COMMENT ON COLUMN sales_product_day_resource_images.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_day_resource_images.product_id IS '销售产品模板ID。';
COMMENT ON COLUMN sales_product_day_resource_images.day_resource_id IS '每日资源编排ID。';
COMMENT ON COLUMN sales_product_day_resource_images.resource_image_id IS '采购资源图片素材ID。';
COMMENT ON COLUMN sales_product_day_resource_images.attachment_id IS '公共附件ID。';
COMMENT ON COLUMN sales_product_day_resource_images.original_filename_snapshot IS '图片原始文件名快照。';
COMMENT ON COLUMN sales_product_day_resource_images.caption_snapshot IS '图片图注或说明快照。';
COMMENT ON COLUMN sales_product_day_resource_images.sort_order IS '图片输出排序号。';
COMMENT ON COLUMN sales_product_day_resource_images.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_day_resource_images.remark IS '备注。';
COMMENT ON COLUMN sales_product_day_resource_images.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_day_resource_images.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_product_day_resource_images.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_day_resource_images.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_day_resource_images.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_product_adult_quotes IS '销售产品成人报价草稿表，用于保存按成人口径生成报价单前的后端成本和对外价。';
COMMENT ON COLUMN sales_product_adult_quotes.id IS '成人报价主键ID。';
COMMENT ON COLUMN sales_product_adult_quotes.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_adult_quotes.product_id IS '销售产品模板ID。';
COMMENT ON COLUMN sales_product_adult_quotes.planned_adult_count IS '计划成人数量，用于将产品总成本折算为成人成本。';
COMMENT ON COLUMN sales_product_adult_quotes.adult_cost_amount IS '后端按当前资源快照计算的成人成本金额。';
COMMENT ON COLUMN sales_product_adult_quotes.markup_amount IS '业务人员填写的单成人加价金额。';
COMMENT ON COLUMN sales_product_adult_quotes.adult_sale_amount IS '成人对外报价金额。';
COMMENT ON COLUMN sales_product_adult_quotes.valid_until IS '报价有效截止日期。';
COMMENT ON COLUMN sales_product_adult_quotes.quote_remark IS '报价备注。';
COMMENT ON COLUMN sales_product_adult_quotes.status IS '报价状态。draft草稿，confirmed已确认。';
COMMENT ON COLUMN sales_product_adult_quotes.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_adult_quotes.remark IS '内部备注。';
COMMENT ON COLUMN sales_product_adult_quotes.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_adult_quotes.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_product_adult_quotes.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_adult_quotes.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_adult_quotes.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE sales_product_document_versions IS '销售产品生成文件版本表，用于保存产品Word和成人报价单的不可变生成记录。';
COMMENT ON COLUMN sales_product_document_versions.id IS '生成文件版本主键ID。';
COMMENT ON COLUMN sales_product_document_versions.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_document_versions.product_id IS '销售产品模板ID。';
COMMENT ON COLUMN sales_product_document_versions.document_type IS '文档类型。product_word表示产品介绍Word，adult_quote表示成人报价单。';
COMMENT ON COLUMN sales_product_document_versions.version_no IS '同产品同文档类型下的版本号，从1开始。';
COMMENT ON COLUMN sales_product_document_versions.source_snapshot IS '生成时使用的产品、行程、资源介绍、图片和报价来源快照。';
COMMENT ON COLUMN sales_product_document_versions.attachment_id IS '生成文件的公共附件ID。';
COMMENT ON COLUMN sales_product_document_versions.file_name_snapshot IS '生成文件名快照。';
COMMENT ON COLUMN sales_product_document_versions.generate_status IS '生成状态。pending处理中，success成功，failed失败。';
COMMENT ON COLUMN sales_product_document_versions.generated_by IS '生成操作人账号或名称。';
COMMENT ON COLUMN sales_product_document_versions.generated_at IS '生成完成时间。';
COMMENT ON COLUMN sales_product_document_versions.error_message IS '生成失败原因。';
COMMENT ON COLUMN sales_product_document_versions.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_document_versions.remark IS '备注。';
COMMENT ON COLUMN sales_product_document_versions.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_document_versions.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_product_document_versions.is_deleted IS '是否已删除。';
COMMENT ON COLUMN sales_product_document_versions.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_document_versions.deleted_by IS '删除人账号或名称。';

COMMENT ON INDEX idx_sales_product_day_resources_product_day IS '产品设计工作台按产品和天数查询资源编排的索引。';
COMMENT ON INDEX idx_sales_product_day_resources_resource IS '产品设计工作台按资源反查引用产品的索引。';
COMMENT ON INDEX idx_sales_product_day_resources_supplier IS '产品设计工作台按供应商查询成本来源的索引。';
COMMENT ON INDEX idx_sales_product_day_resources_supplier_relation IS '产品设计工作台按采购关系追踪每日资源成本快照的索引。';
COMMENT ON INDEX uk_sales_product_day_resources_day_resource_role_active IS '同一产品同一天同一资源在非地接编排归属下不可重复；地接服务允许同日重复安排。';
COMMENT ON INDEX uk_sales_product_day_resources_day_meal_role_active IS '同一产品同一天仅允许一家酒店，且每个餐次仅允许一个餐厅的唯一索引。';
COMMENT ON INDEX idx_sales_product_day_resources_product_day_role_sort IS '产品设计工作台按产品、日次、编排区块和排序读取每日资源的索引。';
COMMENT ON INDEX idx_sales_product_designer_vehicle_product IS '产品设计全程用车按产品和排序查询的索引。';
COMMENT ON INDEX idx_sales_product_designer_vehicle_resource IS '按车辆资源追踪产品设计用车引用的索引。';
COMMENT ON INDEX idx_sales_product_designer_vehicle_supplier_relation IS '按采购关系追踪产品设计用车成本快照的索引。';

CREATE TABLE IF NOT EXISTS sales_product_day_resource_introductions (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  day_resource_id bigint NOT NULL,
  resource_introduction_id bigint NOT NULL,
  introduction_index_version integer,
  title_snapshot varchar(160),
  content_snapshot text,
  notice_snapshot text,
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_sales_product_day_resource_introductions_tenant_id_id
    UNIQUE (tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_introductions_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products (tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_introductions_day_resource
    FOREIGN KEY (tenant_id, day_resource_id) REFERENCES sales_product_day_resources (tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_introductions_source
    FOREIGN KEY (tenant_id, resource_introduction_id)
    REFERENCES purchase_resource_introductions (tenant_id, id),
  CONSTRAINT chk_sales_product_day_resource_introductions_sort CHECK (sort_order >= 1),
  CONSTRAINT chk_sales_product_day_resource_introductions_index_version CHECK (
    introduction_index_version IS NULL OR introduction_index_version >= 1
  )
);

DROP TRIGGER IF EXISTS trg_sales_product_day_resource_introductions_updated_at
  ON sales_product_day_resource_introductions;
CREATE TRIGGER trg_sales_product_day_resource_introductions_updated_at
BEFORE UPDATE ON sales_product_day_resource_introductions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_product_day_resource_introductions_day_resource
  ON sales_product_day_resource_introductions
  (tenant_id, is_deleted, product_id, day_resource_id, sort_order, id);
CREATE INDEX IF NOT EXISTS idx_sales_product_day_resource_introductions_source
  ON sales_product_day_resource_introductions (tenant_id, is_deleted, resource_introduction_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_day_resource_introductions_active
  ON sales_product_day_resource_introductions (tenant_id, day_resource_id, resource_introduction_id)
  WHERE is_deleted = false;

COMMENT ON TABLE sales_product_day_resource_introductions IS
  '销售产品每日资源介绍素材快照表，用于组合多个资源介绍并按顺序生成产品Word。';
COMMENT ON COLUMN sales_product_day_resource_introductions.id IS '每日资源介绍快照主键ID。';
COMMENT ON COLUMN sales_product_day_resource_introductions.tenant_id IS '租户ID，用于隔离不同地接公司的产品设计数据。';
COMMENT ON COLUMN sales_product_day_resource_introductions.product_id IS '销售产品设计草稿或产品模板ID。';
COMMENT ON COLUMN sales_product_day_resource_introductions.day_resource_id IS '产品某天资源编排ID。';
COMMENT ON COLUMN sales_product_day_resource_introductions.resource_introduction_id IS '资源主档介绍素材ID，仅用于追踪来源，正文以快照为准。';
COMMENT ON COLUMN sales_product_day_resource_introductions.introduction_index_version IS '保存时资源介绍的向量索引版本快照。';
COMMENT ON COLUMN sales_product_day_resource_introductions.title_snapshot IS '介绍素材标题快照。';
COMMENT ON COLUMN sales_product_day_resource_introductions.content_snapshot IS '介绍素材正文快照，用于生成产品Word。';
COMMENT ON COLUMN sales_product_day_resource_introductions.notice_snapshot IS '介绍素材注意事项快照，生成产品Word时按红色提示输出。';
COMMENT ON COLUMN sales_product_day_resource_introductions.sort_order IS '同一产品日资源内介绍素材输出顺序，从1开始。';
COMMENT ON COLUMN sales_product_day_resource_introductions.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN sales_product_day_resource_introductions.remark IS '介绍素材快照备注。';
COMMENT ON COLUMN sales_product_day_resource_introductions.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_day_resource_introductions.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_product_day_resource_introductions.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN sales_product_day_resource_introductions.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_day_resource_introductions.deleted_by IS '删除人账号或名称。';
COMMENT ON INDEX idx_sales_product_day_resource_introductions_day_resource IS '每日资源介绍快照按产品、资源和顺序查询的索引。';
COMMENT ON INDEX idx_sales_product_day_resource_introductions_source IS '按资源主档介绍素材追踪产品引用的索引。';
COMMENT ON INDEX uk_sales_product_day_resource_introductions_active IS '同一产品日资源不能重复选择同一介绍素材的唯一索引。';
COMMENT ON INDEX idx_sales_product_day_resource_images_day_resource IS '产品设计配图按每日资源和排序查询的索引。';
COMMENT ON INDEX uk_sales_product_day_resource_images_active IS '同一每日资源不能重复选择同一张图片的唯一索引。';
COMMENT ON INDEX idx_sales_product_adult_quotes_product IS '产品成人报价按产品和状态查询的索引。';
COMMENT ON INDEX idx_sales_product_document_versions_product IS '产品生成文件按产品、类型和版本查询的索引。';
COMMENT ON INDEX uk_sales_product_document_versions_product_type_version_active IS '同一产品同一文档类型的版本号唯一索引。';

COMMIT;

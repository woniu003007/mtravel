-- 旅游接待管理系统：产品设计工作台资源编排 V2
-- PostgreSQL
-- 执行前必须确认目标环境和数据库；本迁移保留“同一天只能安排一家酒店”的住宿唯一规则。

BEGIN;

ALTER TABLE sales_product_day_resources
  ADD COLUMN IF NOT EXISTS supplier_relation_id_snapshot bigint,
  ADD COLUMN IF NOT EXISTS price_mode_snapshot varchar(20);

-- 先解除旧角色约束，才能把历史地接资源从普通行程迁移为独立地接服务。
ALTER TABLE sales_product_day_resources
  DROP CONSTRAINT IF EXISTS chk_sales_product_day_resources_arrangement_role,
  DROP CONSTRAINT IF EXISTS chk_sales_product_day_resources_price_mode,
  DROP CONSTRAINT IF EXISTS fk_sales_product_day_resources_supplier_relation;

UPDATE sales_product_day_resources
SET arrangement_role = 'ground_service'
WHERE is_deleted = false
  AND resource_type_snapshot = 'ground_agent'
  AND arrangement_role = 'itinerary';

ALTER TABLE sales_product_day_resources
  ADD CONSTRAINT fk_sales_product_day_resources_supplier_relation
    FOREIGN KEY (tenant_id, supplier_relation_id_snapshot)
    REFERENCES purchase_relations (tenant_id, id),
  ADD CONSTRAINT chk_sales_product_day_resources_arrangement_role CHECK (
    (resource_type_snapshot = 'hotel' AND arrangement_role = 'accommodation')
    OR (resource_type_snapshot = 'restaurant' AND arrangement_role IN ('unassigned', 'breakfast', 'lunch', 'dinner'))
    OR (resource_type_snapshot = 'ground_agent' AND arrangement_role = 'ground_service')
    OR (resource_type_snapshot IN ('scenic', 'shopping', 'other') AND arrangement_role = 'itinerary')
    -- vehicle/traffic are retained only so historical day-resource records remain readable.
    OR (resource_type_snapshot IN ('vehicle', 'traffic') AND arrangement_role = 'itinerary')
  ),
  ADD CONSTRAINT chk_sales_product_day_resources_price_mode CHECK (
    price_mode_snapshot IS NULL OR price_mode_snapshot IN ('unified', 'classified', 'pending', 'not_required')
  );

DROP INDEX IF EXISTS uk_sales_product_day_resources_day_resource_role_active;
CREATE UNIQUE INDEX uk_sales_product_day_resources_day_resource_role_active
  ON sales_product_day_resources (tenant_id, product_id, day_no, resource_id, arrangement_role)
  WHERE is_deleted = false AND arrangement_role <> 'ground_service';

CREATE INDEX IF NOT EXISTS idx_sales_product_day_resources_product_day_role_sort
  ON sales_product_day_resources (tenant_id, is_deleted, product_id, day_no, arrangement_role, sort_order, id);
CREATE INDEX IF NOT EXISTS idx_sales_product_day_resources_supplier_relation
  ON sales_product_day_resources (tenant_id, is_deleted, supplier_relation_id_snapshot)
  WHERE supplier_relation_id_snapshot IS NOT NULL;

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

COMMENT ON COLUMN sales_product_day_resources.arrangement_role IS
  '资源在当天的编排归属。酒店为accommodation；餐厅为breakfast、lunch、dinner或历史unassigned；地接为ground_service；景区、购物及其它为itinerary；历史用车和交通仅兼容保留为itinerary。';
COMMENT ON COLUMN sales_product_day_resources.supplier_relation_id_snapshot IS
  '保存时选中的采购关系ID快照，用于区分同一供应商的不同资源报价关系。';
COMMENT ON COLUMN sales_product_day_resources.price_mode_snapshot IS
  '保存时采购关系报价模式快照。unified统一报价，classified分类报价，pending表示待询价，not_required表示无需采购。';
COMMENT ON INDEX uk_sales_product_day_resources_day_resource_role_active IS
  '同一产品同一天同一资源在非地接编排归属下不可重复；地接服务允许同日重复安排。';
COMMENT ON INDEX idx_sales_product_day_resources_product_day_role_sort IS
  '产品设计工作台按产品、日次、编排区块和排序读取每日资源的索引。';
COMMENT ON INDEX idx_sales_product_day_resources_supplier_relation IS
  '产品设计工作台按采购关系追踪每日资源成本快照的索引。';

COMMENT ON TABLE sales_product_designer_vehicle_arrangements IS
  '销售产品设计全程用车编排表，保存产品级用车资源、供应商采购关系和成本快照，不归属单个行程日。';
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
COMMENT ON INDEX idx_sales_product_designer_vehicle_product IS '产品设计全程用车按产品和排序查询的索引。';
COMMENT ON INDEX idx_sales_product_designer_vehicle_resource IS '按车辆资源追踪产品设计用车引用的索引。';
COMMENT ON INDEX idx_sales_product_designer_vehicle_supplier_relation IS '按采购关系追踪产品设计用车成本快照的索引。';

COMMIT;

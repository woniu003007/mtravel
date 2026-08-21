-- 资源级自费项目主档及产品设计自费项目快照。
-- 供应商报价保存成本和建议对外价；产品设计保存最终对外价，Word 只读取最终对外价。
-- 本文件仅维护本地结构，远程执行须在目标数据库确认后进行。

BEGIN;

CREATE TABLE IF NOT EXISTS purchase_resource_optional_items (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_id bigint NOT NULL,
  project_name varchar(200) NOT NULL,
  item_type varchar(30) NOT NULL DEFAULT 'recommended_self_pay',
  price_unit varchar(30) NOT NULL DEFAULT 'yuan_per_person',
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_purchase_resource_optional_items_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_purchase_resource_optional_items_resource
    FOREIGN KEY (tenant_id, resource_id) REFERENCES purchase_resources(tenant_id, id),
  CONSTRAINT chk_purchase_resource_optional_items_name CHECK (btrim(project_name) <> ''),
  CONSTRAINT chk_purchase_resource_optional_items_type
    CHECK (item_type IN ('scenic_transport', 'recommended_self_pay')),
  CONSTRAINT chk_purchase_resource_optional_items_unit CHECK (price_unit = 'yuan_per_person'),
  CONSTRAINT chk_purchase_resource_optional_items_status CHECK (status IN ('active', 'disabled'))
);

DROP TRIGGER IF EXISTS trg_purchase_resource_optional_items_updated_at ON purchase_resource_optional_items;
CREATE TRIGGER trg_purchase_resource_optional_items_updated_at
BEFORE UPDATE ON purchase_resource_optional_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_purchase_resource_optional_items_resource
  ON purchase_resource_optional_items (tenant_id, is_deleted, resource_id, status, id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_resource_optional_items_active_name
  ON purchase_resource_optional_items (tenant_id, resource_id, project_name)
  WHERE is_deleted = false;

ALTER TABLE purchase_relation_optional_items
  ADD COLUMN IF NOT EXISTS resource_optional_item_id bigint,
  ADD COLUMN IF NOT EXISTS suggested_sale_price numeric(14,2);

-- 将历史供应商自费报价的名称提升为资源级主档，并回填关系，避免迁移后丢失既有报价。
INSERT INTO purchase_resource_optional_items (
  tenant_id, resource_id, project_name, item_type, price_unit, status, created_by, is_deleted
)
SELECT DISTINCT r.tenant_id, r.resource_id, i.project_name, 'recommended_self_pay', 'yuan_per_person', 'active', i.created_by, false
FROM purchase_relation_optional_items i
JOIN purchase_relations r ON r.tenant_id = i.tenant_id AND r.id = i.relation_id AND r.is_deleted = false
JOIN purchase_resources pr ON pr.tenant_id = r.tenant_id AND pr.id = r.resource_id AND pr.is_deleted = false
WHERE i.is_deleted = false AND pr.resource_type = 'scenic'
ON CONFLICT DO NOTHING;
UPDATE purchase_relation_optional_items i
SET resource_optional_item_id = m.id
FROM purchase_relations r, purchase_resource_optional_items m
WHERE i.tenant_id = r.tenant_id
  AND i.relation_id = r.id
  AND m.tenant_id = r.tenant_id
  AND m.resource_id = r.resource_id
  AND m.project_name = i.project_name
  AND m.is_deleted = false
  AND i.is_deleted = false
  AND i.resource_optional_item_id IS NULL;

ALTER TABLE purchase_relation_optional_items
  DROP CONSTRAINT IF EXISTS fk_purchase_relation_optional_items_resource_optional_item;
ALTER TABLE purchase_relation_optional_items
  ADD CONSTRAINT fk_purchase_relation_optional_items_resource_optional_item
  FOREIGN KEY (tenant_id, resource_optional_item_id)
  REFERENCES purchase_resource_optional_items(tenant_id, id);
ALTER TABLE purchase_relation_optional_items
  DROP CONSTRAINT IF EXISTS chk_purchase_relation_optional_items_suggested_sale;
ALTER TABLE purchase_relation_optional_items
  ADD CONSTRAINT chk_purchase_relation_optional_items_suggested_sale
  CHECK (suggested_sale_price IS NULL OR suggested_sale_price >= 0);
CREATE INDEX IF NOT EXISTS idx_purchase_relation_optional_items_master
  ON purchase_relation_optional_items (tenant_id, is_deleted, resource_optional_item_id, status)
  WHERE resource_optional_item_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_relation_optional_items_active_master
  ON purchase_relation_optional_items (tenant_id, relation_id, resource_optional_item_id)
  WHERE is_deleted = false AND resource_optional_item_id IS NOT NULL;

ALTER TABLE purchase_resource_introductions
  ADD COLUMN IF NOT EXISTS resource_optional_item_id bigint;
ALTER TABLE purchase_resource_introductions
  DROP CONSTRAINT IF EXISTS fk_purchase_resource_introductions_optional_item;
ALTER TABLE purchase_resource_introductions
  ADD CONSTRAINT fk_purchase_resource_introductions_optional_item
  FOREIGN KEY (tenant_id, resource_optional_item_id)
  REFERENCES purchase_resource_optional_items(tenant_id, id);
ALTER TABLE purchase_resource_introductions
  DROP CONSTRAINT IF EXISTS chk_purchase_resource_introductions_optional_item_link;
ALTER TABLE purchase_resource_introductions
  ADD CONSTRAINT chk_purchase_resource_introductions_optional_item_link
  CHECK ((is_optional_item = false AND resource_optional_item_id IS NULL)
      OR (is_optional_item = true AND resource_optional_item_id IS NOT NULL));
CREATE INDEX IF NOT EXISTS idx_purchase_resource_introductions_optional_item
  ON purchase_resource_introductions (tenant_id, is_deleted, resource_optional_item_id, status)
  WHERE resource_optional_item_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS sales_product_day_resource_optional_items (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  product_id bigint NOT NULL,
  day_resource_id bigint NOT NULL,
  resource_optional_item_id bigint NOT NULL,
  supplier_optional_item_id bigint,
  item_type_snapshot varchar(30) NOT NULL,
  project_name_snapshot varchar(200) NOT NULL,
  price_unit_snapshot varchar(30) NOT NULL DEFAULT 'yuan_per_person',
  supplier_cost_price_snapshot numeric(14,2),
  suggested_sale_price_snapshot numeric(14,2),
  final_sale_price numeric(14,2) NOT NULL,
  selected_introduction_id bigint,
  introduction_title_snapshot varchar(160),
  introduction_content_snapshot text,
  introduction_notice_snapshot text,
  introduction_warm_tip_snapshot text,
  introduction_visit_duration_snapshot varchar(100),
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_sales_product_day_resource_optional_items_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_optional_items_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES sales_products(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_optional_items_day_resource
    FOREIGN KEY (tenant_id, day_resource_id) REFERENCES sales_product_day_resources(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_optional_items_master
    FOREIGN KEY (tenant_id, resource_optional_item_id) REFERENCES purchase_resource_optional_items(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_optional_items_supplier_item
    FOREIGN KEY (tenant_id, supplier_optional_item_id) REFERENCES purchase_relation_optional_items(tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_optional_items_intro
    FOREIGN KEY (tenant_id, selected_introduction_id) REFERENCES purchase_resource_introductions(tenant_id, id),
  CONSTRAINT chk_sales_product_day_resource_optional_items_type
    CHECK (item_type_snapshot IN ('scenic_transport', 'recommended_self_pay')),
  CONSTRAINT chk_sales_product_day_resource_optional_items_unit CHECK (price_unit_snapshot = 'yuan_per_person'),
  CONSTRAINT chk_sales_product_day_resource_optional_items_amounts CHECK (
    supplier_cost_price_snapshot IS NULL OR supplier_cost_price_snapshot >= 0
  ),
  CONSTRAINT chk_sales_product_day_resource_optional_items_suggested CHECK (
    suggested_sale_price_snapshot IS NULL OR suggested_sale_price_snapshot >= 0
  ),
  CONSTRAINT chk_sales_product_day_resource_optional_items_final CHECK (final_sale_price > 0),
  CONSTRAINT chk_sales_product_day_resource_optional_items_sort CHECK (sort_order >= 1)
);

DROP TRIGGER IF EXISTS trg_sales_product_day_resource_optional_items_updated_at ON sales_product_day_resource_optional_items;
CREATE TRIGGER trg_sales_product_day_resource_optional_items_updated_at
BEFORE UPDATE ON sales_product_day_resource_optional_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE INDEX IF NOT EXISTS idx_sales_product_day_resource_optional_items_day_resource
  ON sales_product_day_resource_optional_items (tenant_id, is_deleted, product_id, day_resource_id, sort_order, id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_day_resource_optional_items_active
  ON sales_product_day_resource_optional_items (tenant_id, day_resource_id, resource_optional_item_id)
  WHERE is_deleted = false;

COMMENT ON TABLE purchase_resource_optional_items IS '采购资源自费项目主档，维护景区下可选的小交通或推荐自费项目，不保存供应商成本。';
COMMENT ON COLUMN purchase_resource_optional_items.resource_id IS '所属景区资源主档ID。';
COMMENT ON COLUMN purchase_resource_optional_items.project_name IS '自费项目名称。';
COMMENT ON COLUMN purchase_resource_optional_items.id IS '资源自费项目主键ID。';
COMMENT ON COLUMN purchase_resource_optional_items.tenant_id IS '租户ID。';
COMMENT ON COLUMN purchase_resource_optional_items.item_type IS '项目类型，scenic_transport为景区小交通，recommended_self_pay为推荐自费。';
COMMENT ON COLUMN purchase_resource_optional_items.price_unit IS '固定计价单位，yuan_per_person表示元/人。';
COMMENT ON COLUMN purchase_resource_optional_items.status IS '项目状态，active启用，disabled停用。';
COMMENT ON COLUMN purchase_resource_optional_items.created_by IS '创建人。';
COMMENT ON COLUMN purchase_resource_optional_items.remark IS '内部备注。';
COMMENT ON COLUMN purchase_resource_optional_items.created_at IS '创建时间。';
COMMENT ON COLUMN purchase_resource_optional_items.updated_at IS '更新时间。';
COMMENT ON COLUMN purchase_resource_optional_items.is_deleted IS '是否软删除。';
COMMENT ON COLUMN purchase_resource_optional_items.deleted_at IS '删除时间。';
COMMENT ON COLUMN purchase_resource_optional_items.deleted_by IS '删除人。';
COMMENT ON COLUMN purchase_relation_optional_items.resource_optional_item_id IS '关联的资源级自费项目主档ID，历史未关联记录允许为空。';
COMMENT ON COLUMN purchase_relation_optional_items.suggested_sale_price IS '供应商建议的游客对外自费价，仅作为产品设计默认值，不直接输出Word。';
COMMENT ON COLUMN purchase_resource_introductions.resource_optional_item_id IS '自费介绍素材关联的资源级自费项目主档ID，常规介绍为空。';
COMMENT ON TABLE sales_product_day_resource_optional_items IS '产品每日资源自费项目快照表，保存最终游客对外价和介绍快照，产品Word不暴露供应商成本。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.final_sale_price IS '本产品最终向游客展示的自费报价，固定按元/人。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.supplier_cost_price_snapshot IS '供应商成本快照，仅内部使用，不得输出到产品Word。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.id IS '产品自费项目快照主键ID。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.tenant_id IS '租户ID。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.product_id IS '产品设计草稿ID。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.day_resource_id IS '所属每日资源编排ID。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.resource_optional_item_id IS '资源级自费项目主档ID。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.supplier_optional_item_id IS '选用的供应商自费报价ID。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.item_type_snapshot IS '项目类型快照。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.project_name_snapshot IS '项目名称快照。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.price_unit_snapshot IS '固定元/人计价单位快照。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.suggested_sale_price_snapshot IS '供应商建议对外价快照。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.selected_introduction_id IS '选用介绍素材ID。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.introduction_title_snapshot IS '介绍标题快照。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.introduction_content_snapshot IS '介绍正文快照。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.introduction_notice_snapshot IS '注意事项快照。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.introduction_warm_tip_snapshot IS '温馨提示快照。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.introduction_visit_duration_snapshot IS '游览时间快照。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.sort_order IS '输出排序。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.created_by IS '创建人。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.remark IS '内部备注。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.created_at IS '创建时间。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.is_deleted IS '是否软删除。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.deleted_at IS '删除时间。';
COMMENT ON COLUMN sales_product_day_resource_optional_items.deleted_by IS '删除人。';
COMMENT ON INDEX idx_purchase_resource_optional_items_resource IS '按景区查询未删除自费项目的索引。';
COMMENT ON INDEX uk_purchase_resource_optional_items_active_name IS '同一景区未删除自费项目名称唯一索引。';
COMMENT ON INDEX idx_purchase_relation_optional_items_master IS '按资源自费项目查询供应商报价的索引。';
COMMENT ON INDEX uk_purchase_relation_optional_items_active_master IS '同一供应商关系下同一资源自费项目唯一索引。';
COMMENT ON INDEX idx_purchase_resource_introductions_optional_item IS '按自费项目查询关联介绍素材的索引。';
COMMENT ON INDEX idx_sales_product_day_resource_optional_items_day_resource IS '按产品每日资源查询自费项目快照的索引。';
COMMENT ON INDEX uk_sales_product_day_resource_optional_items_active IS '同一产品每日资源不能重复选择同一自费项目。';

COMMIT;

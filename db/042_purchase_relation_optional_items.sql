-- 采购关系自费项目报价：供应商关系下可维护多个自费门票/体验项目。
-- 自费项目只服务景区资源，供应商成本价统一按元/人保存。
-- 执行迁移前必须确认目标数据库已获批准；本脚本仅作为本地结构迁移，支持重复执行。

BEGIN;

CREATE TABLE IF NOT EXISTS purchase_relation_optional_items (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  relation_id bigint NOT NULL,
  project_name varchar(200) NOT NULL,
  cost_price numeric(14,2) NOT NULL,
  price_unit varchar(30) NOT NULL DEFAULT 'yuan_per_person',
  price_description varchar(500),
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_purchase_relation_optional_items_tenant_id_id
    UNIQUE (tenant_id, id),
  CONSTRAINT fk_purchase_relation_optional_items_relation
    FOREIGN KEY (tenant_id, relation_id)
    REFERENCES purchase_relations (tenant_id, id),
  CONSTRAINT chk_purchase_relation_optional_items_name
    CHECK (btrim(project_name) <> ''),
  CONSTRAINT chk_purchase_relation_optional_items_cost
    CHECK (cost_price >= 0),
  CONSTRAINT chk_purchase_relation_optional_items_unit
    CHECK (price_unit = 'yuan_per_person'),
  CONSTRAINT chk_purchase_relation_optional_items_status
    CHECK (status IN ('active', 'disabled'))
);

DROP TRIGGER IF EXISTS trg_purchase_relation_optional_items_updated_at
  ON purchase_relation_optional_items;
CREATE TRIGGER trg_purchase_relation_optional_items_updated_at
BEFORE UPDATE ON purchase_relation_optional_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_purchase_relation_optional_items_tenant_deleted_relation
  ON purchase_relation_optional_items (tenant_id, is_deleted, relation_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_relation_optional_items_active_name
  ON purchase_relation_optional_items (tenant_id, relation_id, project_name)
  WHERE is_deleted = false;

COMMENT ON TABLE purchase_relation_optional_items IS
  '采购关系自费项目报价表，保存供应商提供的自费门票或体验项目成本价，统一按元/人。';
COMMENT ON COLUMN purchase_relation_optional_items.id IS
  '自费项目报价主键ID。';
COMMENT ON COLUMN purchase_relation_optional_items.tenant_id IS
  '租户ID，用于隔离不同地接公司的采购关系自费项目。';
COMMENT ON COLUMN purchase_relation_optional_items.relation_id IS
  '采购关系ID，关联资源与供应商绑定关系。';
COMMENT ON COLUMN purchase_relation_optional_items.project_name IS
  '自费项目名称，例如苏州游船或电瓶车。';
COMMENT ON COLUMN purchase_relation_optional_items.cost_price IS
  '供应商成本价，统一按元/人保存。';
COMMENT ON COLUMN purchase_relation_optional_items.price_unit IS
  '计价单位代码，当前固定为 yuan_per_person（元/人）。';
COMMENT ON COLUMN purchase_relation_optional_items.price_description IS
  '自费项目价格说明，例如使用条件、时段和包含内容。';
COMMENT ON COLUMN purchase_relation_optional_items.status IS
  '自费项目状态，active表示启用，disabled表示停用。';
COMMENT ON COLUMN purchase_relation_optional_items.created_by IS
  '创建人账号或名称。';
COMMENT ON COLUMN purchase_relation_optional_items.remark IS
  '内部备注。';
COMMENT ON COLUMN purchase_relation_optional_items.created_at IS
  '创建时间。';
COMMENT ON COLUMN purchase_relation_optional_items.updated_at IS
  '更新时间，由触发器自动维护。';
COMMENT ON COLUMN purchase_relation_optional_items.is_deleted IS
  '是否已删除，false表示正常，true表示软删除。';
COMMENT ON COLUMN purchase_relation_optional_items.deleted_at IS
  '删除时间，未删除时为空。';
COMMENT ON COLUMN purchase_relation_optional_items.deleted_by IS
  '删除人账号或名称，未删除时为空。';

COMMENT ON INDEX idx_purchase_relation_optional_items_tenant_deleted_relation IS
  '按租户、软删除状态和采购关系查询自费项目报价的索引。';
COMMENT ON INDEX uk_purchase_relation_optional_items_active_name IS
  '同一采购关系下未删除自费项目名称不能重复。';

COMMIT;

-- 销售产品设计工作台：每日资源多介绍素材快照。
-- 一个产品日资源可以组合多个资源介绍版本，并按 sort_order 串联生成产品 Word。
-- 执行迁移前必须确认目标数据库已获批准；本脚本仅作为本地结构迁移，支持重复执行。

BEGIN;

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
    FOREIGN KEY (tenant_id, product_id)
    REFERENCES sales_products (tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_introductions_day_resource
    FOREIGN KEY (tenant_id, day_resource_id)
    REFERENCES sales_product_day_resources (tenant_id, id),
  CONSTRAINT fk_sales_product_day_resource_introductions_source
    FOREIGN KEY (tenant_id, resource_introduction_id)
    REFERENCES purchase_resource_introductions (tenant_id, id),
  CONSTRAINT chk_sales_product_day_resource_introductions_sort
    CHECK (sort_order >= 1),
  CONSTRAINT chk_sales_product_day_resource_introductions_index_version
    CHECK (introduction_index_version IS NULL OR introduction_index_version >= 1)
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
  ON sales_product_day_resource_introductions
  (tenant_id, is_deleted, resource_introduction_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_product_day_resource_introductions_active
  ON sales_product_day_resource_introductions
  (tenant_id, day_resource_id, resource_introduction_id)
  WHERE is_deleted = false;

COMMENT ON TABLE sales_product_day_resource_introductions IS
  '销售产品每日资源介绍素材快照表，用于组合多个资源介绍并按顺序生成产品Word。';
COMMENT ON COLUMN sales_product_day_resource_introductions.id IS
  '每日资源介绍快照主键ID。';
COMMENT ON COLUMN sales_product_day_resource_introductions.tenant_id IS
  '租户ID，用于隔离不同地接公司的产品设计数据。';
COMMENT ON COLUMN sales_product_day_resource_introductions.product_id IS
  '销售产品设计草稿或产品模板ID。';
COMMENT ON COLUMN sales_product_day_resource_introductions.day_resource_id IS
  '产品某天资源编排ID。';
COMMENT ON COLUMN sales_product_day_resource_introductions.resource_introduction_id IS
  '资源主档介绍素材ID，仅用于追踪来源，正文以快照为准。';
COMMENT ON COLUMN sales_product_day_resource_introductions.introduction_index_version IS
  '保存时资源介绍的向量索引版本快照。';
COMMENT ON COLUMN sales_product_day_resource_introductions.title_snapshot IS
  '介绍素材标题快照。';
COMMENT ON COLUMN sales_product_day_resource_introductions.content_snapshot IS
  '介绍素材正文快照，用于生成产品Word。';
COMMENT ON COLUMN sales_product_day_resource_introductions.notice_snapshot IS
  '介绍素材注意事项快照，生成产品Word时按红色提示输出。';
COMMENT ON COLUMN sales_product_day_resource_introductions.sort_order IS
  '同一产品日资源内介绍素材输出顺序，从1开始。';
COMMENT ON COLUMN sales_product_day_resource_introductions.created_by IS
  '创建人账号或名称。';
COMMENT ON COLUMN sales_product_day_resource_introductions.remark IS
  '介绍素材快照备注。';
COMMENT ON COLUMN sales_product_day_resource_introductions.created_at IS
  '创建时间。';
COMMENT ON COLUMN sales_product_day_resource_introductions.updated_at IS
  '更新时间，由触发器自动维护。';
COMMENT ON COLUMN sales_product_day_resource_introductions.is_deleted IS
  '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN sales_product_day_resource_introductions.deleted_at IS
  '删除时间。';
COMMENT ON COLUMN sales_product_day_resource_introductions.deleted_by IS
  '删除人账号或名称。';

COMMENT ON INDEX idx_sales_product_day_resource_introductions_day_resource IS
  '每日资源介绍快照按产品、资源和顺序查询的索引。';
COMMENT ON INDEX idx_sales_product_day_resource_introductions_source IS
  '按资源主档介绍素材追踪产品引用的索引。';
COMMENT ON INDEX uk_sales_product_day_resource_introductions_active IS
  '同一产品日资源不能重复选择同一介绍素材的唯一索引。';

COMMIT;

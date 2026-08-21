-- 采购资源介绍素材和图片素材。
-- 本脚本只定义本地结构；执行迁移前必须确认目标是已批准的测试或生产数据库。

BEGIN;

CREATE TABLE IF NOT EXISTS purchase_resource_introductions (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_id bigint NOT NULL,
  sort_order integer NOT NULL DEFAULT 1,
  title varchar(160) NOT NULL,
  tags jsonb NOT NULL DEFAULT '[]'::jsonb,
  content text NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'draft',
  index_status varchar(20) NOT NULL DEFAULT 'pending',
  index_version integer NOT NULL DEFAULT 1,
  error_message varchar(500),
  published_at timestamptz,
  created_by varchar(64),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_purchase_resource_introductions_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_purchase_resource_introductions_resource FOREIGN KEY (tenant_id, resource_id)
    REFERENCES purchase_resources (tenant_id, id),
  CONSTRAINT chk_purchase_resource_introductions_tags CHECK (jsonb_typeof(tags) = 'array'),
  CONSTRAINT chk_purchase_resource_introductions_status CHECK (
    status IN ('draft', 'published', 'disabled')
  ),
  CONSTRAINT chk_purchase_resource_introductions_index_status CHECK (
    index_status IN ('pending', 'indexed', 'failed', 'deleted')
  ),
  CONSTRAINT chk_purchase_resource_introductions_index_version CHECK (index_version >= 1),
  CONSTRAINT chk_purchase_resource_introductions_sort_order CHECK (sort_order >= 1)
);

CREATE TABLE IF NOT EXISTS purchase_resource_introduction_chunks (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  introduction_id bigint NOT NULL,
  resource_id bigint NOT NULL,
  chunk_no integer NOT NULL,
  chunk_text text NOT NULL,
  token_count integer NOT NULL,
  embedding_model varchar(120),
  embedding vector(1024),
  index_version integer NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_purchase_resource_introduction_chunks_introduction FOREIGN KEY (tenant_id, introduction_id)
    REFERENCES purchase_resource_introductions (tenant_id, id),
  CONSTRAINT fk_purchase_resource_introduction_chunks_resource FOREIGN KEY (tenant_id, resource_id)
    REFERENCES purchase_resources (tenant_id, id),
  CONSTRAINT chk_purchase_resource_introduction_chunks_chunk_no CHECK (chunk_no >= 1),
  CONSTRAINT chk_purchase_resource_introduction_chunks_token_count CHECK (token_count >= 0),
  CONSTRAINT chk_purchase_resource_introduction_chunks_index_version CHECK (index_version >= 1)
);

CREATE TABLE IF NOT EXISTS purchase_resource_images (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_id bigint NOT NULL,
  attachment_id bigint NOT NULL,
  original_filename varchar(255) NOT NULL,
  file_ext varchar(20) NOT NULL,
  file_size bigint NOT NULL,
  tags jsonb NOT NULL DEFAULT '[]'::jsonb,
  is_cover boolean NOT NULL DEFAULT false,
  sort_order integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(64),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_purchase_resource_images_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_purchase_resource_images_resource FOREIGN KEY (tenant_id, resource_id)
    REFERENCES purchase_resources (tenant_id, id),
  CONSTRAINT fk_purchase_resource_images_attachment FOREIGN KEY (tenant_id, attachment_id)
    REFERENCES common_attachments (tenant_id, id),
  CONSTRAINT chk_purchase_resource_images_tags CHECK (jsonb_typeof(tags) = 'array'),
  CONSTRAINT chk_purchase_resource_images_file_size CHECK (file_size >= 0),
  CONSTRAINT chk_purchase_resource_images_sort_order CHECK (sort_order >= 0),
  CONSTRAINT chk_purchase_resource_images_status CHECK (status IN ('active', 'disabled'))
);

DROP TRIGGER IF EXISTS trg_purchase_resource_introductions_updated_at ON purchase_resource_introductions;
CREATE TRIGGER trg_purchase_resource_introductions_updated_at
BEFORE UPDATE ON purchase_resource_introductions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_purchase_resource_images_updated_at ON purchase_resource_images;
CREATE TRIGGER trg_purchase_resource_images_updated_at
BEFORE UPDATE ON purchase_resource_images
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_purchase_resource_introductions_tenant_resource
  ON purchase_resource_introductions (tenant_id, is_deleted, resource_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_purchase_resource_introductions_tenant_resource_sort
  ON purchase_resource_introductions (tenant_id, is_deleted, resource_id, sort_order, id)
  WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_purchase_resource_introductions_tenant_index
  ON purchase_resource_introductions (tenant_id, is_deleted, status, index_status)
  WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_purchase_resource_introduction_chunks_tenant_introduction
  ON purchase_resource_introduction_chunks (tenant_id, introduction_id, index_version);
CREATE INDEX IF NOT EXISTS idx_purchase_resource_introduction_chunks_tenant_resource
  ON purchase_resource_introduction_chunks (tenant_id, resource_id, introduction_id);
CREATE INDEX IF NOT EXISTS idx_purchase_resource_images_tenant_resource
  ON purchase_resource_images (tenant_id, is_deleted, resource_id, sort_order, id)
  WHERE is_deleted = false AND status = 'active';
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_resource_images_tenant_resource_cover_active
  ON purchase_resource_images (tenant_id, resource_id)
  WHERE is_deleted = false AND status = 'active' AND is_cover = true;

COMMENT ON TABLE purchase_resource_introductions IS '采购资源介绍素材表，用于维护可复用的多版本资源介绍正文及资源内维护排序。';
COMMENT ON COLUMN purchase_resource_introductions.id IS '介绍素材主键ID。';
COMMENT ON COLUMN purchase_resource_introductions.tenant_id IS '租户ID，用于隔离不同地接公司的介绍素材。';
COMMENT ON COLUMN purchase_resource_introductions.resource_id IS '采购资源主档ID。';
COMMENT ON COLUMN purchase_resource_introductions.sort_order IS '介绍素材在当前资源内的维护排序，从1开始，数值越小越靠前。';
COMMENT ON COLUMN purchase_resource_introductions.title IS '介绍素材名称。';
COMMENT ON COLUMN purchase_resource_introductions.tags IS '介绍适用标签JSON数组，例如通用、秋冬、亲子或研学。';
COMMENT ON COLUMN purchase_resource_introductions.content IS '介绍正文，用于产品行程和产品手册生成。';
COMMENT ON COLUMN purchase_resource_introductions.status IS '介绍发布状态。draft表示草稿，published表示可用于产品生成，disabled表示停用。';
COMMENT ON COLUMN purchase_resource_introductions.index_status IS '介绍正文向量索引状态。pending待向量化，indexed已入库，failed处理失败，deleted已删除。';
COMMENT ON COLUMN purchase_resource_introductions.index_version IS '介绍正文向量索引版本，保存、发布或删除时递增以阻止过期异步任务写入。';
COMMENT ON COLUMN purchase_resource_introductions.error_message IS '最近一次向量化失败原因。';
COMMENT ON COLUMN purchase_resource_introductions.published_at IS '介绍最近一次发布时间。';
COMMENT ON COLUMN purchase_resource_introductions.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN purchase_resource_introductions.remark IS '介绍维护备注。';
COMMENT ON COLUMN purchase_resource_introductions.created_at IS '创建时间。';
COMMENT ON COLUMN purchase_resource_introductions.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN purchase_resource_introductions.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN purchase_resource_introductions.deleted_at IS '删除时间。';
COMMENT ON COLUMN purchase_resource_introductions.deleted_by IS '删除人账号或名称。';

COMMENT ON TABLE purchase_resource_introduction_chunks IS '采购资源介绍素材切片表，用于保存介绍正文的检索文本及pgvector向量。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.id IS '介绍素材切片主键ID。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.tenant_id IS '租户ID。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.introduction_id IS '介绍素材ID。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.resource_id IS '采购资源主档ID，便于按资源过滤检索。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.chunk_no IS '切片序号，从1开始。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.chunk_text IS '带正文来源标签的介绍切片文本。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.token_count IS '估算token数量。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.embedding_model IS '生成向量使用的模型名称。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.embedding IS '介绍正文切片向量，固定1024维。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.index_version IS '介绍素材索引版本。';
COMMENT ON COLUMN purchase_resource_introduction_chunks.created_at IS '创建时间。';

COMMENT ON TABLE purchase_resource_images IS '采购资源图片素材表，用于维护景区、酒店和其它资源的可复用图片。';
COMMENT ON COLUMN purchase_resource_images.id IS '图片素材主键ID。';
COMMENT ON COLUMN purchase_resource_images.tenant_id IS '租户ID。';
COMMENT ON COLUMN purchase_resource_images.resource_id IS '采购资源主档ID。';
COMMENT ON COLUMN purchase_resource_images.attachment_id IS '公共附件ID，关联原始图片文件。';
COMMENT ON COLUMN purchase_resource_images.original_filename IS '原始图片文件名快照。';
COMMENT ON COLUMN purchase_resource_images.file_ext IS '图片文件扩展名。';
COMMENT ON COLUMN purchase_resource_images.file_size IS '图片文件大小，单位字节。';
COMMENT ON COLUMN purchase_resource_images.tags IS '图片适用标签JSON数组。';
COMMENT ON COLUMN purchase_resource_images.is_cover IS '是否为当前资源封面图。每个有效资源最多一张封面图。';
COMMENT ON COLUMN purchase_resource_images.sort_order IS '图片展示排序值，数值越小越靠前。';
COMMENT ON COLUMN purchase_resource_images.status IS '图片状态。active表示可用，disabled表示停用。';
COMMENT ON COLUMN purchase_resource_images.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN purchase_resource_images.remark IS '图片维护备注。';
COMMENT ON COLUMN purchase_resource_images.created_at IS '创建时间。';
COMMENT ON COLUMN purchase_resource_images.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN purchase_resource_images.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN purchase_resource_images.deleted_at IS '删除时间。';
COMMENT ON COLUMN purchase_resource_images.deleted_by IS '删除人账号或名称。';

COMMENT ON INDEX idx_purchase_resource_introductions_tenant_resource IS '资源介绍素材按租户、资源和状态查询的索引。';
COMMENT ON INDEX idx_purchase_resource_introductions_tenant_resource_sort IS '资源内未删除介绍素材按维护排序读取的索引。';
COMMENT ON INDEX idx_purchase_resource_introductions_tenant_index IS '已维护介绍素材按发布和向量状态查询的索引。';
COMMENT ON INDEX idx_purchase_resource_introduction_chunks_tenant_introduction IS '介绍素材切片按租户、素材和索引版本删除或重建的索引。';
COMMENT ON INDEX idx_purchase_resource_introduction_chunks_tenant_resource IS '介绍素材切片按租户和资源查询的索引。';
COMMENT ON INDEX idx_purchase_resource_images_tenant_resource IS '有效图片素材按租户、资源和排序查询的索引。';
COMMENT ON INDEX uk_purchase_resource_images_tenant_resource_cover_active IS '每个资源至多一张有效封面图片的唯一索引。';

COMMIT;

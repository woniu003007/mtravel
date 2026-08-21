-- 介绍素材与资源图片素材的多对多关联。
-- 图片文件仍归当前资源图片素材库；本表只保存某一介绍素材实际采用的图片及输出顺序。

BEGIN;

CREATE TABLE IF NOT EXISTS purchase_resource_introduction_images (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  introduction_id bigint NOT NULL,
  resource_image_id bigint NOT NULL,
  sort_order integer NOT NULL DEFAULT 1,
  created_by varchar(64),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  remark text,
  CONSTRAINT uk_purchase_resource_introduction_images_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_purchase_resource_introduction_images_introduction
    FOREIGN KEY (tenant_id, introduction_id)
    REFERENCES purchase_resource_introductions(tenant_id, id),
  CONSTRAINT fk_purchase_resource_introduction_images_resource_image
    FOREIGN KEY (tenant_id, resource_image_id)
    REFERENCES purchase_resource_images(tenant_id, id),
  CONSTRAINT chk_purchase_resource_introduction_images_sort CHECK (sort_order >= 1)
);

ALTER TABLE purchase_resource_introduction_images
  ADD COLUMN IF NOT EXISTS remark text;

DROP TRIGGER IF EXISTS trg_purchase_resource_introduction_images_updated_at
  ON purchase_resource_introduction_images;
CREATE TRIGGER trg_purchase_resource_introduction_images_updated_at
BEFORE UPDATE ON purchase_resource_introduction_images
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_purchase_resource_introduction_images_introduction
  ON purchase_resource_introduction_images (tenant_id, is_deleted, introduction_id, sort_order, id)
  WHERE is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_resource_introduction_images_active
  ON purchase_resource_introduction_images (tenant_id, introduction_id, resource_image_id)
  WHERE is_deleted = false;

COMMENT ON TABLE purchase_resource_introduction_images IS '采购资源介绍素材图片关联表，保存每份介绍素材选用的资源图片和输出顺序。';
COMMENT ON COLUMN purchase_resource_introduction_images.id IS '介绍素材图片关联主键ID。';
COMMENT ON COLUMN purchase_resource_introduction_images.tenant_id IS '租户ID。';
COMMENT ON COLUMN purchase_resource_introduction_images.introduction_id IS '介绍素材ID。';
COMMENT ON COLUMN purchase_resource_introduction_images.resource_image_id IS '资源图片素材ID。';
COMMENT ON COLUMN purchase_resource_introduction_images.sort_order IS '图片在当前介绍素材中的输出顺序，从1开始。';
COMMENT ON COLUMN purchase_resource_introduction_images.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN purchase_resource_introduction_images.remark IS '图片关联维护备注。';
COMMENT ON COLUMN purchase_resource_introduction_images.created_at IS '创建时间。';
COMMENT ON COLUMN purchase_resource_introduction_images.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN purchase_resource_introduction_images.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN purchase_resource_introduction_images.deleted_at IS '软删除时间。';
COMMENT ON COLUMN purchase_resource_introduction_images.deleted_by IS '软删除人账号或名称。';
COMMENT ON INDEX idx_purchase_resource_introduction_images_introduction IS '按介绍素材读取未删除关联图片及排序的索引。';
COMMENT ON INDEX uk_purchase_resource_introduction_images_active IS '同一介绍素材不能重复关联同一张未删除图片。';

COMMIT;

-- 资源介绍素材的资源内维护排序。
-- 历史素材按原列表 updated_at DESC、id DESC 回填。
-- 本文件仅维护本地迁移，远程执行前必须确认已批准的目标数据库。

BEGIN;

ALTER TABLE purchase_resource_introductions
  ADD COLUMN IF NOT EXISTS sort_order integer;

-- 已删除素材单独编号，确保当前未删除列表仍从 1 连续排序。
WITH ranked AS (
  SELECT id,
         ROW_NUMBER() OVER (
           PARTITION BY tenant_id, resource_id, is_deleted
           ORDER BY updated_at DESC, id DESC
         )::integer AS next_sort_order
  FROM purchase_resource_introductions
)
UPDATE purchase_resource_introductions introduction
SET sort_order = ranked.next_sort_order
FROM ranked
WHERE introduction.id = ranked.id
  AND introduction.sort_order IS NULL;

ALTER TABLE purchase_resource_introductions
  ALTER COLUMN sort_order SET DEFAULT 1;
ALTER TABLE purchase_resource_introductions
  ALTER COLUMN sort_order SET NOT NULL;
ALTER TABLE purchase_resource_introductions
  DROP CONSTRAINT IF EXISTS chk_purchase_resource_introductions_sort_order;
ALTER TABLE purchase_resource_introductions
  ADD CONSTRAINT chk_purchase_resource_introductions_sort_order
  CHECK (sort_order >= 1);

DROP INDEX IF EXISTS idx_purchase_resource_introductions_tenant_resource_sort;
CREATE INDEX idx_purchase_resource_introductions_tenant_resource_sort
  ON purchase_resource_introductions (tenant_id, is_deleted, resource_id, sort_order, id)
  WHERE is_deleted = false;

COMMENT ON TABLE purchase_resource_introductions IS
  '采购资源介绍素材表，用于维护可复用的多版本资源介绍正文及资源内维护排序。';
COMMENT ON COLUMN purchase_resource_introductions.sort_order IS
  '介绍素材在当前资源内的维护排序，从1开始，数值越小越靠前。';
COMMENT ON INDEX idx_purchase_resource_introductions_tenant_resource_sort IS
  '资源内未删除介绍素材按维护排序读取的索引。';

COMMIT;

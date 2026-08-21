-- 资源介绍素材的结构化扩展内容模块，以及产品设计中的不可变快照。
-- 首版用于拍照机位推荐、温馨提示等按条目编号输出的内容，不参与全文检索。

BEGIN;

ALTER TABLE purchase_resource_introductions
  ADD COLUMN IF NOT EXISTS extension_blocks jsonb NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE purchase_resource_introductions
  DROP CONSTRAINT IF EXISTS chk_purchase_resource_introductions_extension_blocks;
ALTER TABLE purchase_resource_introductions
  ADD CONSTRAINT chk_purchase_resource_introductions_extension_blocks
  CHECK (jsonb_typeof(extension_blocks) = 'array');

ALTER TABLE sales_product_day_resource_introductions
  ADD COLUMN IF NOT EXISTS extension_blocks_snapshot jsonb NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE sales_product_day_resource_introductions
  DROP CONSTRAINT IF EXISTS chk_sales_product_day_resource_introductions_extension_blocks_snapshot;
ALTER TABLE sales_product_day_resource_introductions
  ADD CONSTRAINT chk_sales_product_day_resource_introductions_extension_blocks_snapshot
  CHECK (jsonb_typeof(extension_blocks_snapshot) = 'array');

COMMENT ON COLUMN purchase_resource_introductions.extension_blocks IS
  '介绍正文后可按顺序输出的结构化内容模块JSON数组，例如拍照机位推荐和温馨提示。';
COMMENT ON COLUMN sales_product_day_resource_introductions.extension_blocks_snapshot IS
  '保存产品编排时资源介绍扩展内容模块的JSON快照，避免源素材修改影响已设计产品。';

COMMIT;

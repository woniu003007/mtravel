-- 资源介绍素材补充温馨提示，并明确注意事项与温馨提示的独立业务口径。
-- 现有 notice_content 和 notice_snapshot 保留原值，继续作为注意事项，不做数据迁移。
-- 执行迁移前必须确认目标数据库已获批准；本脚本可重复执行。

BEGIN;

ALTER TABLE purchase_resource_introductions
  ADD COLUMN IF NOT EXISTS warm_tip_content text;

ALTER TABLE sales_product_day_resources
  ADD COLUMN IF NOT EXISTS introduction_warm_tip_snapshot text;

ALTER TABLE sales_product_day_resource_introductions
  ADD COLUMN IF NOT EXISTS warm_tip_snapshot text;

COMMENT ON TABLE purchase_resource_introductions IS
  '采购资源介绍素材表，用于维护可复用的多版本资源介绍正文、温馨提示、注意事项和游览时间。';

COMMENT ON COLUMN purchase_resource_introductions.notice_content IS
  '介绍使用注意事项，按非空行逐条维护，生成产品Word时按红色提示输出。';

COMMENT ON COLUMN purchase_resource_introductions.warm_tip_content IS
  '介绍使用温馨提示，按非空行逐条维护，生成产品Word时按普通文字输出。';

COMMENT ON COLUMN purchase_resource_introductions.visit_duration IS
  '资源建议游览时间，例如约2小时。';

COMMENT ON COLUMN sales_product_day_resources.introduction_notice_snapshot IS
  '资源介绍注意事项快照，生成产品Word时按非空行逐条红色输出。';

COMMENT ON COLUMN sales_product_day_resources.introduction_warm_tip_snapshot IS
  '资源介绍温馨提示快照，生成产品Word时按非空行逐条普通文字输出。';

COMMENT ON COLUMN sales_product_day_resources.introduction_visit_duration_snapshot IS
  '资源介绍游览时间快照，用于生成产品Word。';

COMMENT ON COLUMN sales_product_day_resource_introductions.notice_snapshot IS
  '介绍素材注意事项快照，生成产品Word时按非空行逐条红色输出。';

COMMENT ON COLUMN sales_product_day_resource_introductions.warm_tip_snapshot IS
  '介绍素材温馨提示快照，生成产品Word时按非空行逐条普通文字输出。';

COMMENT ON COLUMN sales_product_day_resource_introductions.visit_duration_snapshot IS
  '介绍素材游览时间快照，用于生成产品Word。';

COMMENT ON COLUMN purchase_resource_introductions.index_status IS
  '介绍正文、温馨提示和注意事项向量索引状态。pending待向量化，indexed已入库，failed处理失败，deleted已删除。';

COMMENT ON COLUMN purchase_resource_introductions.index_version IS
  '介绍正文、温馨提示和注意事项向量索引版本，保存、发布或删除时递增以阻止过期异步任务写入。';

COMMENT ON TABLE purchase_resource_introduction_chunks IS
  '采购资源介绍素材切片表，用于保存介绍正文、温馨提示和注意事项的检索文本及pgvector向量。';

COMMENT ON COLUMN purchase_resource_introduction_chunks.chunk_text IS
  '带正文、温馨提示或注意事项来源标签的介绍切片文本。';

COMMIT;

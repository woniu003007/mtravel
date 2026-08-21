-- 资源介绍注意事项及产品设计快照。
-- 执行迁移前必须确认目标数据库已获批准；本脚本可重复执行。

BEGIN;

ALTER TABLE purchase_resource_introductions
  ADD COLUMN IF NOT EXISTS notice_content text;

ALTER TABLE sales_product_day_resources
  ADD COLUMN IF NOT EXISTS introduction_notice_snapshot text;

COMMENT ON COLUMN purchase_resource_introductions.notice_content IS
  '介绍使用注意事项，按非空行逐条维护，用于产品资料和执行提醒。';

COMMENT ON COLUMN sales_product_day_resources.introduction_notice_snapshot IS
  '资源介绍注意事项快照，用于生成产品Word时按非空行逐条红色输出。';

COMMIT;

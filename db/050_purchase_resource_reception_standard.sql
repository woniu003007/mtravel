-- 旅游接待管理系统：酒店资源星级与接待标准字典对齐
-- PostgreSQL

BEGIN;

ALTER TABLE purchase_resources
  ALTER COLUMN star_level TYPE varchar(120);

ALTER TABLE purchase_resources
  DROP CONSTRAINT IF EXISTS chk_purchase_resources_star_level;

COMMENT ON COLUMN purchase_resources.star_level IS '酒店或餐厅星级/接待标准名称，酒店由企业产品字典 reception_standard 提供；非酒店资源为空。';

COMMIT;

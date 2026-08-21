-- 资源介绍素材增加自费项目标记，复用现有介绍字段和产品输出流程。
-- 历史介绍默认保持常规介绍；本脚本可重复执行。

BEGIN;

ALTER TABLE purchase_resource_introductions
  ADD COLUMN IF NOT EXISTS is_optional_item boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN purchase_resource_introductions.is_optional_item IS
  '是否为自费项目介绍素材。true表示自费项目，false表示常规资源介绍。';

COMMIT;

-- 将已启用的座位数报价规则补齐为车队费用项目。
-- 这样用车弹窗中“座位数”和“价格信息”可以使用同一项目名称口径。

INSERT INTO resource_projects (
  tenant_id,
  resource_type,
  project_name,
  statistics_enabled,
  sort_order,
  status,
  created_by,
  remark,
  is_deleted
)
SELECT
  rules.tenant_id,
  'vehicle',
  rules.vehicle_type,
  true,
  COALESCE(NULLIF(regexp_replace(rules.vehicle_type, '\D', '', 'g'), '')::integer, 999),
  'active',
  'system',
  '由座位数报价规则自动补齐',
  false
FROM vehicle_quote_rules rules
WHERE rules.is_deleted = false
  AND rules.status = 'active'
  AND NOT EXISTS (
    SELECT 1
    FROM resource_projects projects
    WHERE projects.tenant_id = rules.tenant_id
      AND projects.is_deleted = false
      AND projects.resource_type = 'vehicle'
      AND projects.project_name = rules.vehicle_type
  );

UPDATE resource_projects projects
SET status = 'active',
    updated_at = now()
WHERE projects.is_deleted = false
  AND projects.resource_type = 'vehicle'
  AND projects.status <> 'active'
  AND EXISTS (
    SELECT 1
    FROM vehicle_quote_rules rules
    WHERE rules.tenant_id = projects.tenant_id
      AND rules.is_deleted = false
      AND rules.status = 'active'
      AND rules.vehicle_type = projects.project_name
  );

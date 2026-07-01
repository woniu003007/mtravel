-- 清理车队费用项目中早期按车型描述初始化的种子项。
-- 车辆座位数现在以 vehicle_quote_rules.vehicle_type 为准，并同步生成同名费用项目。

BEGIN;

UPDATE resource_projects projects
SET status = 'disabled',
    is_deleted = true,
    deleted_at = now(),
    deleted_by = 'system',
    updated_at = now()
WHERE projects.is_deleted = false
  AND projects.resource_type = 'vehicle'
  AND projects.project_name IN ('5座轿车', '7座商务车', '19座中巴', '33座大巴', '45座大巴', '34座', '7座', '54座')
  AND NOT EXISTS (
    SELECT 1
    FROM vehicle_quote_rules rules
    WHERE rules.tenant_id = projects.tenant_id
      AND rules.vehicle_type = projects.project_name
      AND rules.status = 'active'
      AND rules.is_deleted = false
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sales_product_arrangement_price_lines lines
    WHERE lines.tenant_id = projects.tenant_id
      AND lines.project_id = projects.id
      AND lines.is_deleted = false
  )
  AND NOT EXISTS (
    SELECT 1
    FROM dispatch_team_arrangement_price_lines lines
    WHERE lines.tenant_id = projects.tenant_id
      AND lines.project_id = projects.id
      AND lines.is_deleted = false
  )
  AND NOT EXISTS (
    SELECT 1
    FROM supplier_resource_prices prices
    WHERE prices.tenant_id = projects.tenant_id
      AND prices.resource_project_id = projects.id
      AND prices.is_deleted = false
  );

UPDATE resource_projects projects
SET sort_order = COALESCE(NULLIF(regexp_replace(projects.project_name, '\D', '', 'g'), '')::integer, projects.sort_order),
    statistics_enabled = true,
    status = 'active',
    updated_at = now()
WHERE projects.is_deleted = false
  AND projects.resource_type = 'vehicle'
  AND EXISTS (
    SELECT 1
    FROM vehicle_quote_rules rules
    WHERE rules.tenant_id = projects.tenant_id
      AND rules.vehicle_type = projects.project_name
      AND rules.status = 'active'
      AND rules.is_deleted = false
  );

COMMIT;

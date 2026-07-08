-- 产品排期生成团队时，清理历史自动带入的模板数量和金额。
--
-- 产品团队安排只作为正式团队的参考信息：可带供应商、资源、项目和参考单价，
-- 不应把产品模板中的人数、数量和金额直接计入新团队成本、现结、挂账和预算利润。

BEGIN;

WITH candidate_arrangements AS (
  SELECT a.id
  FROM dispatch_team_arrangements a
  JOIN sales_teams t
    ON t.tenant_id = a.tenant_id
   AND t.id = a.team_id
   AND t.is_deleted = false
  WHERE a.is_deleted = false
    AND a.status = 'active'
    AND a.confirmed = false
    AND a.no_guide_report = false
    AND a.guide_report_status = 'pending'
    AND a.operator_audit_status = 'pending'
    AND a.finance_audit_status = 'pending'
    AND abs(extract(epoch from (a.created_at - t.created_at))) <= 300
    AND NOT EXISTS (
      SELECT 1
      FROM dispatch_team_arrangement_flow_records f
      WHERE f.tenant_id = a.tenant_id
        AND f.arrangement_id = a.id
        AND f.is_deleted = false
    )
    AND NOT EXISTS (
      SELECT 1
      FROM dispatch_team_arrangement_order_allocations o
      WHERE o.tenant_id = a.tenant_id
        AND o.arrangement_id = a.id
        AND o.is_deleted = false
        AND (o.allocation_scope = 'order' OR o.order_id IS NOT NULL)
    )
    AND EXISTS (
      SELECT 1
      FROM dispatch_team_arrangement_price_lines pl
      WHERE pl.tenant_id = a.tenant_id
        AND pl.arrangement_id = a.id
        AND pl.is_deleted = false
        AND (
          coalesce(pl.quantity, 0) > 0
          OR coalesce(pl.amount, 0) > 0
          OR coalesce(pl.cash_amount, 0) > 0
          OR coalesce(pl.credit_amount, 0) > 0
        )
    )
),
updated_price_lines AS (
  UPDATE dispatch_team_arrangement_price_lines pl
  SET unit_price = CASE
        WHEN coalesce(pl.unit_price, 0) > 0 THEN pl.unit_price
        WHEN coalesce(pl.quantity, 0) > 0 THEN round(coalesce(pl.amount, 0) / pl.quantity, 2)
        ELSE 0
      END,
      quantity = 0,
      amount = 0,
      cash_amount = 0,
      credit_amount = 0,
      guide_commission_amount = 0,
      company_rebate_amount = 0,
      head_fee_amount = 0,
      consumption_amount = 0,
      updated_at = now()
  FROM candidate_arrangements c
  WHERE pl.arrangement_id = c.id
    AND pl.is_deleted = false
  RETURNING pl.arrangement_id
),
updated_allocations AS (
  UPDATE dispatch_team_arrangement_order_allocations o
  SET original_amount = 0,
      allocation_amount = 0,
      guest_count = 0,
      updated_at = now()
  FROM candidate_arrangements c
  WHERE o.arrangement_id = c.id
    AND o.is_deleted = false
    AND o.allocation_scope = 'team'
    AND o.order_id IS NULL
  RETURNING o.arrangement_id
)
UPDATE dispatch_team_arrangements a
SET total_amount = 0,
    cash_amount = 0,
    credit_amount = 0,
    prepaid_amount = 0,
    sale_amount = 0,
    cost_amount = 0,
    guide_commission_amount = 0,
    company_rebate_amount = 0,
    head_fee_amount = 0,
    consumption_amount = 0,
    people_count = 0,
    confirmed = false,
    updated_at = now()
FROM candidate_arrangements c
WHERE a.id = c.id
  AND a.is_deleted = false;

COMMIT;

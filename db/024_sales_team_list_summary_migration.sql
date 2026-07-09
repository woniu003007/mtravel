-- 旅游接待管理系统：团队名称与团队列表汇总表迁移
-- PostgreSQL

BEGIN;

ALTER TABLE sales_teams
  ADD COLUMN IF NOT EXISTS team_name varchar(200);

CREATE TABLE IF NOT EXISTS sales_team_list_summaries (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_id bigint NOT NULL,
  team_no varchar(80) NOT NULL,
  team_name varchar(200),
  team_type varchar(20),
  status varchar(20),
  departure_date date,
  end_date date,
  departure_place varchar(300),
  travel_days integer,
  business_type varchar(120),
  department_name varchar(160),
  operator_employee_name varchar(100),
  customer_summary text,
  salesperson_summary text,
  guide_summary text,
  order_status_summary text,
  total_seats integer NOT NULL DEFAULT 0,
  used_seats integer NOT NULL DEFAULT 0,
  remaining_seats integer NOT NULL DEFAULT 0,
  guide_plan varchar(20) NOT NULL DEFAULT 'none',
  traffic_plan varchar(20) NOT NULL DEFAULT 'none',
  hotel_plan varchar(20) NOT NULL DEFAULT 'none',
  vehicle_plan varchar(20) NOT NULL DEFAULT 'none',
  scenic_plan varchar(20) NOT NULL DEFAULT 'none',
  meal_plan varchar(20) NOT NULL DEFAULT 'none',
  other_plan varchar(20) NOT NULL DEFAULT 'none',
  optional_plan varchar(20) NOT NULL DEFAULT 'none',
  shopping_plan varchar(20) NOT NULL DEFAULT 'none',
  ground_agent_plan varchar(20) NOT NULL DEFAULT 'none',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_team_list_summaries_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT uk_sales_team_list_summaries_tenant_team UNIQUE (tenant_id, team_id)
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sales_team_list_summaries_updated_at ON sales_team_list_summaries;
CREATE TRIGGER trg_sales_team_list_summaries_updated_at
BEFORE UPDATE ON sales_team_list_summaries
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_sales_teams_tenant_deleted_team_name
  ON sales_teams (tenant_id, is_deleted, team_name);

CREATE INDEX IF NOT EXISTS idx_sales_team_summary_main
  ON sales_team_list_summaries (tenant_id, is_deleted, created_at DESC, team_no);

CREATE INDEX IF NOT EXISTS idx_sales_team_summary_departure_date
  ON sales_team_list_summaries (tenant_id, is_deleted, departure_date DESC, team_no);

CREATE INDEX IF NOT EXISTS idx_sales_team_summary_type_date
  ON sales_team_list_summaries (tenant_id, is_deleted, team_type, departure_date DESC);

CREATE INDEX IF NOT EXISTS idx_sales_team_summary_status_date
  ON sales_team_list_summaries (tenant_id, is_deleted, status, departure_date DESC);

CREATE INDEX IF NOT EXISTS idx_sales_team_summary_business_date
  ON sales_team_list_summaries (tenant_id, is_deleted, business_type, departure_date DESC);

CREATE INDEX IF NOT EXISTS idx_sales_team_summary_department_date
  ON sales_team_list_summaries (tenant_id, is_deleted, department_name, departure_date DESC);

UPDATE sales_teams t
SET team_name = CASE
  WHEN p.product_name IS NULL THEN t.team_no
  WHEN p.product_name LIKE '%' || '-' || t.team_no THEN left(p.product_name, length(p.product_name) - length(t.team_no) - 1)
  ELSE p.product_name
END
FROM sales_products p
WHERE p.tenant_id = t.tenant_id
  AND p.id = t.product_id
  AND (t.team_name IS NULL OR btrim(t.team_name) = '');

INSERT INTO sales_team_list_summaries (
  tenant_id, team_id, team_no, team_name, team_type, status,
  departure_date, end_date, departure_place, travel_days, business_type,
  department_name, operator_employee_name, total_seats, used_seats, remaining_seats,
  guide_plan, traffic_plan, hotel_plan, vehicle_plan, scenic_plan, meal_plan,
  other_plan, optional_plan, shopping_plan, ground_agent_plan,
  created_by, remark, created_at, updated_at, is_deleted, deleted_at, deleted_by
)
SELECT
  t.tenant_id,
  t.id,
  t.team_no,
  COALESCE(NULLIF(btrim(t.team_name), ''), t.team_no),
  t.team_type,
  t.status,
  t.departure_date,
  CASE WHEN p.travel_days IS NULL OR p.travel_days <= 0 THEN t.departure_date ELSE t.departure_date + (p.travel_days - 1) END,
  concat_ws('', p.province, p.city, p.district),
  COALESCE(NULLIF(p.travel_days, 0), 1),
  COALESCE(t.business_type, p.business_type),
  t.department_name,
  t.operator_employee_name,
  COALESCE(t.total_seats, 0),
  COALESCE(t.used_seats, 0),
  COALESCE(t.remaining_seats, 0),
  'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none',
  t.created_by,
  t.remark,
  t.created_at,
  t.updated_at,
  t.is_deleted,
  t.deleted_at,
  t.deleted_by
FROM sales_teams t
LEFT JOIN sales_products p ON p.tenant_id = t.tenant_id AND p.id = t.product_id
ON CONFLICT (tenant_id, team_id) DO UPDATE SET
  team_no = EXCLUDED.team_no,
  team_name = EXCLUDED.team_name,
  team_type = EXCLUDED.team_type,
  status = EXCLUDED.status,
  departure_date = EXCLUDED.departure_date,
  end_date = EXCLUDED.end_date,
  departure_place = EXCLUDED.departure_place,
  travel_days = EXCLUDED.travel_days,
  business_type = EXCLUDED.business_type,
  department_name = EXCLUDED.department_name,
  operator_employee_name = EXCLUDED.operator_employee_name,
  total_seats = EXCLUDED.total_seats,
  used_seats = EXCLUDED.used_seats,
  remaining_seats = EXCLUDED.remaining_seats,
  created_by = EXCLUDED.created_by,
  remark = EXCLUDED.remark,
  is_deleted = EXCLUDED.is_deleted,
  deleted_at = EXCLUDED.deleted_at,
  deleted_by = EXCLUDED.deleted_by;

COMMENT ON COLUMN sales_teams.team_name IS '团队展示名称。直接新增团队时保存用户填写名称，列表和拼团选择均以该字段为准。';
COMMENT ON TABLE sales_team_list_summaries IS '销售团队列表查询汇总表，供团队管理列表单表分页搜索。';

COMMIT;

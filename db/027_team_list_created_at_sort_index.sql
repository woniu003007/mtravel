DROP INDEX IF EXISTS idx_sales_team_summary_main;

CREATE INDEX IF NOT EXISTS idx_sales_team_summary_main
  ON sales_team_list_summaries (tenant_id, is_deleted, created_at DESC, team_no);

CREATE INDEX IF NOT EXISTS idx_sales_team_summary_departure_date
  ON sales_team_list_summaries (tenant_id, is_deleted, departure_date DESC, team_no);

-- 旅游接待管理系统：客户风控审批表
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 仅用于让本脚本可在独立临时库中校验。正式库中这些表已由前置脚本创建。
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS customers (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  customer_name varchar(200) NOT NULL,
  is_deleted boolean NOT NULL DEFAULT false,
  CONSTRAINT uk_customers_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS sales_teams (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_no varchar(80) NOT NULL,
  is_deleted boolean NOT NULL DEFAULT false,
  CONSTRAINT uk_sales_teams_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS sales_orders (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_id bigint,
  customer_id bigint,
  customer_name varchar(200),
  order_no varchar(80) NOT NULL,
  is_deleted boolean NOT NULL DEFAULT false,
  CONSTRAINT uk_sales_orders_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS system_configs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  config_key varchar(100) NOT NULL,
  config_value varchar(500) NOT NULL,
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_system_configs_tenant_key UNIQUE (tenant_id, config_key)
);

CREATE TABLE IF NOT EXISTS customer_risk_approval_requests (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  customer_id bigint NOT NULL,
  customer_name varchar(200),
  team_id bigint,
  order_id bigint,
  request_no varchar(64) NOT NULL,
  requested_amount numeric(14,2) NOT NULL DEFAULT 0,
  risk_types varchar(200) NOT NULL,
  risk_summary text,
  contract_expire_date date,
  credit_limit numeric(14,2) NOT NULL DEFAULT 0,
  occupied_amount numeric(14,2) NOT NULL DEFAULT 0,
  pending_approval_amount numeric(14,2) NOT NULL DEFAULT 0,
  available_amount numeric(14,2) NOT NULL DEFAULT 0,
  over_limit_amount numeric(14,2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'pending',
  applicant varchar(80),
  approved_by varchar(80),
  approved_at timestamptz,
  rejected_by varchar(80),
  rejected_at timestamptz,
  approval_remark text,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_customer_risk_approval_customer
    FOREIGN KEY (tenant_id, customer_id) REFERENCES customers (tenant_id, id),
  CONSTRAINT fk_customer_risk_approval_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT fk_customer_risk_approval_order
    FOREIGN KEY (tenant_id, order_id) REFERENCES sales_orders (tenant_id, id),
  CONSTRAINT chk_customer_risk_approval_status
    CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled')),
  CONSTRAINT chk_customer_risk_approval_requested_amount
    CHECK (requested_amount >= 0),
  CONSTRAINT chk_customer_risk_approval_snapshot_amounts
    CHECK (
      credit_limit >= 0
      AND occupied_amount >= 0
      AND pending_approval_amount >= 0
      AND over_limit_amount >= 0
    )
);

DROP TRIGGER IF EXISTS trg_customer_risk_approval_requests_updated_at ON customer_risk_approval_requests;
CREATE TRIGGER trg_customer_risk_approval_requests_updated_at
BEFORE UPDATE ON customer_risk_approval_requests
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_risk_approval_tenant_request_no_active
  ON customer_risk_approval_requests (tenant_id, request_no)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_customer_risk_approval_tenant_status_time
  ON customer_risk_approval_requests (tenant_id, is_deleted, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_customer_risk_approval_tenant_customer_status
  ON customer_risk_approval_requests (tenant_id, is_deleted, customer_id, status);

CREATE INDEX IF NOT EXISTS idx_customer_risk_approval_tenant_team_order
  ON customer_risk_approval_requests (tenant_id, is_deleted, team_id, order_id);

CREATE INDEX IF NOT EXISTS idx_customer_risk_approval_tenant_applicant_time
  ON customer_risk_approval_requests (tenant_id, is_deleted, applicant, created_at DESC);

-- 已绑定订单的已同意审批单必须与订单客户一致。不一致的历史授权保留流水但取消有效授权。
UPDATE customer_risk_approval_requests approval
SET status = 'cancelled',
    approval_remark = COALESCE(approval.approval_remark || E'\n', '') || '系统整理：审批单客户与订单当前客户不一致或订单客户为空，本记录取消有效授权。',
    updated_at = now()
FROM sales_orders orders
WHERE approval.tenant_id = orders.tenant_id
  AND approval.order_id = orders.id
  AND approval.is_deleted = false
  AND approval.status = 'approved'
  AND orders.is_deleted = false
  AND (
    orders.customer_id IS NULL
    OR orders.customer_id <> approval.customer_id
  );

-- 已绑定订单的已同意审批单必须一单一审批。重复历史保留最新一条有效，其余转为已取消。
WITH duplicate_approved AS (
  SELECT id,
         row_number() OVER (
           PARTITION BY tenant_id, order_id
           ORDER BY approved_at DESC NULLS LAST, id DESC
         ) AS rn
  FROM customer_risk_approval_requests
  WHERE is_deleted = false
    AND status = 'approved'
    AND order_id IS NOT NULL
)
UPDATE customer_risk_approval_requests target
SET status = 'cancelled',
    approval_remark = COALESCE(target.approval_remark || E'\n', '') || '系统整理：同一订单保留最新一张已同意风控审批单，本记录取消有效授权。',
    updated_at = now()
FROM duplicate_approved dup
WHERE target.id = dup.id
  AND dup.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_risk_approval_tenant_order_approved_active
  ON customer_risk_approval_requests (tenant_id, order_id)
  WHERE is_deleted = false
    AND status = 'approved'
    AND order_id IS NOT NULL;

COMMENT ON TABLE customer_risk_approval_requests IS '客户风控审批申请表。用于保存客户合同到期、授信超限时的总经理审批流水和申请时快照。';
COMMENT ON COLUMN customer_risk_approval_requests.id IS '客户风控审批申请主键 ID。';
COMMENT ON COLUMN customer_risk_approval_requests.tenant_id IS '租户 ID，用于隔离不同地接公司的审批数据。';
COMMENT ON COLUMN customer_risk_approval_requests.customer_id IS '客户单位 ID。';
COMMENT ON COLUMN customer_risk_approval_requests.customer_name IS '客户单位名称快照。';
COMMENT ON COLUMN customer_risk_approval_requests.team_id IS '关联团队 ID，可为空。';
COMMENT ON COLUMN customer_risk_approval_requests.order_id IS '关联订单 ID，新订单申请时可为空，保存后可回填。';
COMMENT ON COLUMN customer_risk_approval_requests.request_no IS '审批申请编号，同一租户下未删除申请唯一。';
COMMENT ON COLUMN customer_risk_approval_requests.requested_amount IS '本次订单预计应收金额。';
COMMENT ON COLUMN customer_risk_approval_requests.risk_types IS '风险类型编码，多个类型用英文逗号分隔，例如 contract_expired,credit_over_limit。';
COMMENT ON COLUMN customer_risk_approval_requests.risk_summary IS '风险摘要，面向审批人展示。';
COMMENT ON COLUMN customer_risk_approval_requests.contract_expire_date IS '申请时客户合同有效期止快照。';
COMMENT ON COLUMN customer_risk_approval_requests.credit_limit IS '申请时客户授信额度快照。';
COMMENT ON COLUMN customer_risk_approval_requests.occupied_amount IS '申请时客户已占用额度快照。';
COMMENT ON COLUMN customer_risk_approval_requests.pending_approval_amount IS '申请时客户审批中额度快照。';
COMMENT ON COLUMN customer_risk_approval_requests.available_amount IS '申请时客户可用额度快照，可能为负数。';
COMMENT ON COLUMN customer_risk_approval_requests.over_limit_amount IS '申请时本次订单超出可用额度的金额。';
COMMENT ON COLUMN customer_risk_approval_requests.status IS '审批状态。pending待审批，approved已同意，rejected已拒绝，cancelled已取消。';
COMMENT ON COLUMN customer_risk_approval_requests.applicant IS '申请人账号或名称。';
COMMENT ON COLUMN customer_risk_approval_requests.approved_by IS '同意审批人账号或名称。';
COMMENT ON COLUMN customer_risk_approval_requests.approved_at IS '同意审批时间。';
COMMENT ON COLUMN customer_risk_approval_requests.rejected_by IS '拒绝审批人账号或名称。';
COMMENT ON COLUMN customer_risk_approval_requests.rejected_at IS '拒绝审批时间。';
COMMENT ON COLUMN customer_risk_approval_requests.approval_remark IS '审批意见。';
COMMENT ON COLUMN customer_risk_approval_requests.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN customer_risk_approval_requests.remark IS '申请备注。';
COMMENT ON COLUMN customer_risk_approval_requests.created_at IS '创建时间。';
COMMENT ON COLUMN customer_risk_approval_requests.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN customer_risk_approval_requests.is_deleted IS '是否已软删除。false正常，true已删除。';
COMMENT ON COLUMN customer_risk_approval_requests.deleted_at IS '软删除时间。';
COMMENT ON COLUMN customer_risk_approval_requests.deleted_by IS '执行软删除的操作人。';
COMMENT ON INDEX uk_customer_risk_approval_tenant_request_no_active IS '客户风控审批申请编号唯一索引，仅约束未删除记录。';
COMMENT ON INDEX idx_customer_risk_approval_tenant_status_time IS '按租户、状态和创建时间查询审批申请。';
COMMENT ON INDEX idx_customer_risk_approval_tenant_customer_status IS '按客户和状态查询审批申请。';
COMMENT ON INDEX idx_customer_risk_approval_tenant_team_order IS '按团队和订单追溯审批申请。';
COMMENT ON INDEX idx_customer_risk_approval_tenant_applicant_time IS '按申请人和创建时间查询审批申请。';
COMMENT ON INDEX uk_customer_risk_approval_tenant_order_approved_active IS '约束同一租户同一订单只能有一张未删除且已同意的风控审批单。';

INSERT INTO system_configs (tenant_id, config_key, config_value, remark)
SELECT id, 'customer_risk_approval_enabled', 'false', '客户合同到期或授信超限时是否强制总经理审批'
FROM tenants
ON CONFLICT (tenant_id, config_key) DO NOTHING;

COMMIT;

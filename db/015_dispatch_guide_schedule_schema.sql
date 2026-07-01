-- 旅游接待管理系统：导游安排、导游排班与请假表
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

CREATE TABLE IF NOT EXISTS sales_teams (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_no varchar(80) NOT NULL,
  is_deleted boolean NOT NULL DEFAULT false,
  CONSTRAINT uk_sales_teams_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS enterprise_guides (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  guide_name varchar(80) NOT NULL,
  mobile_phone varchar(40),
  status varchar(20) NOT NULL DEFAULT 'active',
  is_deleted boolean NOT NULL DEFAULT false,
  CONSTRAINT uk_enterprise_guides_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS dispatch_team_guides (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  team_id bigint NOT NULL,
  team_no varchar(80) NOT NULL,
  guide_id bigint NOT NULL,
  guide_name varchar(80) NOT NULL,
  guide_mobile varchar(40),
  guide_fee numeric(12,2) NOT NULL DEFAULT 0,
  imprest_amount numeric(12,2) NOT NULL DEFAULT 0,
  operation_fee numeric(12,2) NOT NULL DEFAULT 0,
  start_at timestamp NOT NULL,
  end_at timestamp NOT NULL,
  fee_memo text,
  guide_memo text,
  is_tentative boolean NOT NULL DEFAULT false,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_dispatch_team_guides_team
    FOREIGN KEY (tenant_id, team_id) REFERENCES sales_teams (tenant_id, id),
  CONSTRAINT fk_dispatch_team_guides_guide
    FOREIGN KEY (tenant_id, guide_id) REFERENCES enterprise_guides (tenant_id, id),
  CONSTRAINT chk_dispatch_team_guides_status CHECK (status IN ('active', 'cancelled')),
  CONSTRAINT chk_dispatch_team_guides_time CHECK (end_at > start_at),
  CONSTRAINT chk_dispatch_team_guides_money CHECK (
    guide_fee >= 0 AND imprest_amount >= 0 AND operation_fee >= 0
  )
);

DROP TRIGGER IF EXISTS trg_dispatch_team_guides_updated_at ON dispatch_team_guides;
CREATE TRIGGER trg_dispatch_team_guides_updated_at
BEFORE UPDATE ON dispatch_team_guides
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_dispatch_team_guides_tenant_team
  ON dispatch_team_guides (tenant_id, is_deleted, team_id, status);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_guides_tenant_guide_time
  ON dispatch_team_guides (tenant_id, is_deleted, guide_id, status, start_at, end_at);

CREATE INDEX IF NOT EXISTS idx_dispatch_team_guides_tenant_time
  ON dispatch_team_guides (tenant_id, is_deleted, status, start_at, end_at);

CREATE TABLE IF NOT EXISTS dispatch_guide_leave_records (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  guide_id bigint NOT NULL,
  guide_name varchar(80) NOT NULL,
  guide_mobile varchar(40),
  source_type varchar(30) NOT NULL DEFAULT 'guide_apply',
  start_at timestamp NOT NULL,
  end_at timestamp NOT NULL,
  leave_reason varchar(300) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'pending',
  applicant varchar(80),
  applied_at timestamptz NOT NULL DEFAULT now(),
  approved_by varchar(80),
  approved_at timestamptz,
  rejected_by varchar(80),
  rejected_at timestamptz,
  approval_remark text,
  withdrawn_by varchar(80),
  withdrawn_at timestamptz,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_dispatch_guide_leave_records_guide
    FOREIGN KEY (tenant_id, guide_id) REFERENCES enterprise_guides (tenant_id, id),
  CONSTRAINT chk_dispatch_guide_leave_source CHECK (source_type IN ('guide_apply', 'dispatcher_direct')),
  CONSTRAINT chk_dispatch_guide_leave_status CHECK (status IN ('pending', 'approved', 'rejected', 'withdrawn', 'cancelled')),
  CONSTRAINT chk_dispatch_guide_leave_time CHECK (end_at > start_at)
);

DROP TRIGGER IF EXISTS trg_dispatch_guide_leave_records_updated_at ON dispatch_guide_leave_records;
CREATE TRIGGER trg_dispatch_guide_leave_records_updated_at
BEFORE UPDATE ON dispatch_guide_leave_records
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_dispatch_guide_leave_tenant_guide_time
  ON dispatch_guide_leave_records (tenant_id, is_deleted, guide_id, status, start_at, end_at);

CREATE INDEX IF NOT EXISTS idx_dispatch_guide_leave_tenant_status_time
  ON dispatch_guide_leave_records (tenant_id, is_deleted, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_dispatch_guide_leave_tenant_source_time
  ON dispatch_guide_leave_records (tenant_id, is_deleted, source_type, start_at, end_at);

COMMENT ON TABLE dispatch_team_guides IS '团队导游安排表。保存正式团队的带团导游、费用、上团下团时间、备注和待定状态，是导游排班汇总的团队占用来源。';
COMMENT ON COLUMN dispatch_team_guides.id IS '团队导游安排主键 ID。';
COMMENT ON COLUMN dispatch_team_guides.tenant_id IS '租户 ID，用于隔离不同地接公司的导游安排数据。';
COMMENT ON COLUMN dispatch_team_guides.team_id IS '所属团队 ID。';
COMMENT ON COLUMN dispatch_team_guides.team_no IS '团队编号快照，用于排班日历和统计展示。';
COMMENT ON COLUMN dispatch_team_guides.guide_id IS '带团导游档案 ID。';
COMMENT ON COLUMN dispatch_team_guides.guide_name IS '带团导游姓名快照。';
COMMENT ON COLUMN dispatch_team_guides.guide_mobile IS '带团导游手机号快照。';
COMMENT ON COLUMN dispatch_team_guides.guide_fee IS '导服费金额。';
COMMENT ON COLUMN dispatch_team_guides.imprest_amount IS '导游备用金申请或预计金额。';
COMMENT ON COLUMN dispatch_team_guides.operation_fee IS '操作费金额。';
COMMENT ON COLUMN dispatch_team_guides.start_at IS '上团时间。';
COMMENT ON COLUMN dispatch_team_guides.end_at IS '下团时间。';
COMMENT ON COLUMN dispatch_team_guides.fee_memo IS '费用说明。';
COMMENT ON COLUMN dispatch_team_guides.guide_memo IS '导游备注。';
COMMENT ON COLUMN dispatch_team_guides.is_tentative IS '是否待定。true 表示导游待定中。';
COMMENT ON COLUMN dispatch_team_guides.status IS '安排状态。active 生效，cancelled 已取消。';
COMMENT ON COLUMN dispatch_team_guides.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN dispatch_team_guides.remark IS '备注。';
COMMENT ON COLUMN dispatch_team_guides.created_at IS '创建时间。';
COMMENT ON COLUMN dispatch_team_guides.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN dispatch_team_guides.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN dispatch_team_guides.deleted_at IS '软删除时间。';
COMMENT ON COLUMN dispatch_team_guides.deleted_by IS '软删除操作人。';
COMMENT ON INDEX idx_dispatch_team_guides_tenant_team IS '按团队查询导游安排。';
COMMENT ON INDEX idx_dispatch_team_guides_tenant_guide_time IS '按导游和时间查询团队占用和冲突。';
COMMENT ON INDEX idx_dispatch_team_guides_tenant_time IS '按时间范围查询排班日历团队占用。';

COMMENT ON TABLE dispatch_guide_leave_records IS '导游请假记录表。保存导游申请请假、计调审批和计调直接设置的不可上团时间。';
COMMENT ON COLUMN dispatch_guide_leave_records.id IS '导游请假记录主键 ID。';
COMMENT ON COLUMN dispatch_guide_leave_records.tenant_id IS '租户 ID，用于隔离不同地接公司的导游请假数据。';
COMMENT ON COLUMN dispatch_guide_leave_records.guide_id IS '请假导游档案 ID。';
COMMENT ON COLUMN dispatch_guide_leave_records.guide_name IS '请假导游姓名快照。';
COMMENT ON COLUMN dispatch_guide_leave_records.guide_mobile IS '请假导游手机号快照。';
COMMENT ON COLUMN dispatch_guide_leave_records.source_type IS '来源类型。guide_apply 导游申请，dispatcher_direct 计调直接设置。';
COMMENT ON COLUMN dispatch_guide_leave_records.start_at IS '请假开始时间。';
COMMENT ON COLUMN dispatch_guide_leave_records.end_at IS '请假结束时间。';
COMMENT ON COLUMN dispatch_guide_leave_records.leave_reason IS '请假原因或不可上团原因。';
COMMENT ON COLUMN dispatch_guide_leave_records.status IS '请假状态。pending 待审批，approved 已通过，rejected 已驳回，withdrawn 已撤回，cancelled 已取消。';
COMMENT ON COLUMN dispatch_guide_leave_records.applicant IS '申请人账号或名称。导游申请时为导游账号，计调直接设置时为计调账号。';
COMMENT ON COLUMN dispatch_guide_leave_records.applied_at IS '申请时间。';
COMMENT ON COLUMN dispatch_guide_leave_records.approved_by IS '审批通过人账号或名称。';
COMMENT ON COLUMN dispatch_guide_leave_records.approved_at IS '审批通过时间。';
COMMENT ON COLUMN dispatch_guide_leave_records.rejected_by IS '驳回人账号或名称。';
COMMENT ON COLUMN dispatch_guide_leave_records.rejected_at IS '驳回时间。';
COMMENT ON COLUMN dispatch_guide_leave_records.approval_remark IS '审批意见。';
COMMENT ON COLUMN dispatch_guide_leave_records.withdrawn_by IS '撤回人账号或名称。';
COMMENT ON COLUMN dispatch_guide_leave_records.withdrawn_at IS '撤回时间。';
COMMENT ON COLUMN dispatch_guide_leave_records.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN dispatch_guide_leave_records.remark IS '备注。';
COMMENT ON COLUMN dispatch_guide_leave_records.created_at IS '创建时间。';
COMMENT ON COLUMN dispatch_guide_leave_records.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN dispatch_guide_leave_records.is_deleted IS '是否已软删除。';
COMMENT ON COLUMN dispatch_guide_leave_records.deleted_at IS '软删除时间。';
COMMENT ON COLUMN dispatch_guide_leave_records.deleted_by IS '软删除操作人。';
COMMENT ON INDEX idx_dispatch_guide_leave_tenant_guide_time IS '按导游和时间查询请假占用和冲突。';
COMMENT ON INDEX idx_dispatch_guide_leave_tenant_status_time IS '按状态和创建时间查询请假审批列表。';
COMMENT ON INDEX idx_dispatch_guide_leave_tenant_source_time IS '按来源类型和时间查询请假记录。';

COMMIT;

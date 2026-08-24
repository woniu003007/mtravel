-- 销售报价配置
-- PostgreSQL

BEGIN;

CREATE TABLE IF NOT EXISTS sales_quote_resource_rules (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL,
  resource_type varchar(30) NOT NULL,
  customer_category_id bigint,
  customer_category_name varchar(100),
  suggested_markup_rate numeric(8,4) NOT NULL DEFAULT 0,
  minimum_markup_rate numeric(8,4) NOT NULL DEFAULT 0,
  suggested_fixed_markup numeric(14,2) NOT NULL DEFAULT 0,
  minimum_fixed_markup numeric(14,2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  quote_mode varchar(20) NOT NULL DEFAULT 'both',
  CONSTRAINT fk_sales_quote_resource_rule_category
    FOREIGN KEY (tenant_id, customer_category_id) REFERENCES customer_categories (tenant_id, id),
  CONSTRAINT chk_sales_quote_resource_rule_type
    CHECK (resource_type IN ('scenic', 'hotel', 'restaurant', 'vehicle', 'transport', 'other', 'misc')),
  CONSTRAINT chk_sales_quote_resource_rule_mode
    CHECK (quote_mode IN ('rate', 'fixed', 'both')),
  CONSTRAINT chk_sales_quote_resource_rule_status
    CHECK (status IN ('active', 'disabled')),
  CONSTRAINT chk_sales_quote_resource_rule_amounts
    CHECK (
      suggested_markup_rate >= 0
      AND minimum_markup_rate >= 0
      AND suggested_fixed_markup >= 0
      AND minimum_fixed_markup >= 0
    )
);

CREATE TABLE IF NOT EXISTS sales_quote_guide_levels (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL,
  level_name varchar(80) NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT uk_sales_quote_guide_levels_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT chk_sales_quote_guide_level_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT chk_sales_quote_guide_level_sort_order CHECK (sort_order >= 0)
);

ALTER TABLE enterprise_guides
  ADD COLUMN IF NOT EXISTS guide_level_id bigint,
  ADD COLUMN IF NOT EXISTS guide_level_name varchar(80);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'fk_enterprise_guides_guide_level'
      AND conrelid = 'enterprise_guides'::regclass
  ) THEN
    ALTER TABLE enterprise_guides
      ADD CONSTRAINT fk_enterprise_guides_guide_level
      FOREIGN KEY (tenant_id, guide_level_id) REFERENCES sales_quote_guide_levels (tenant_id, id);
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS sales_quote_guide_rules (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL,
  guide_level_id bigint NOT NULL,
  guide_level_name varchar(80) NOT NULL,
  language varchar(80) NOT NULL DEFAULT '普通话',
  base_daily_fee numeric(14,2) NOT NULL DEFAULT 0,
  foreign_language_daily_markup numeric(14,2) NOT NULL DEFAULT 0,
  overtime_hourly_fee numeric(14,2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_quote_guide_rule_level
    FOREIGN KEY (tenant_id, guide_level_id) REFERENCES sales_quote_guide_levels (tenant_id, id),
  CONSTRAINT chk_sales_quote_guide_rule_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT chk_sales_quote_guide_rule_amounts
    CHECK (
      base_daily_fee >= 0
      AND foreign_language_daily_markup >= 0
      AND overtime_hourly_fee >= 0
    )
);

CREATE TABLE IF NOT EXISTS sales_quote_ground_agent_rules (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL,
  min_people integer NOT NULL,
  max_people integer NOT NULL,
  group_package_price numeric(14,2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_sales_quote_ground_agent_people
    CHECK (min_people >= 1 AND max_people >= min_people),
  CONSTRAINT chk_sales_quote_ground_agent_price CHECK (group_package_price >= 0),
  CONSTRAINT chk_sales_quote_ground_agent_status CHECK (status IN ('active', 'disabled'))
);

CREATE TABLE IF NOT EXISTS sales_quote_approval_members (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL,
  member_type varchar(20) NOT NULL,
  system_user_id bigint NOT NULL,
  step_order integer NOT NULL DEFAULT 0,
  created_by varchar(80),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_sales_quote_approval_member_user
    FOREIGN KEY (tenant_id, system_user_id) REFERENCES system_users (tenant_id, id),
  CONSTRAINT chk_sales_quote_approval_member_type CHECK (member_type IN ('approver', 'cc')),
  CONSTRAINT chk_sales_quote_approval_member_step_order CHECK (step_order >= 0)
);

CREATE TABLE IF NOT EXISTS sales_quote_approval_configs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL,
  approval_mode varchar(30) NOT NULL DEFAULT 'specified_person',
  created_by varchar(80),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_sales_quote_approval_config_mode
    CHECK (approval_mode IN ('department_manager', 'specified_person'))
);

DROP TRIGGER IF EXISTS trg_sales_quote_resource_rules_updated_at ON sales_quote_resource_rules;
CREATE TRIGGER trg_sales_quote_resource_rules_updated_at
BEFORE UPDATE ON sales_quote_resource_rules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_sales_quote_guide_levels_updated_at ON sales_quote_guide_levels;
CREATE TRIGGER trg_sales_quote_guide_levels_updated_at
BEFORE UPDATE ON sales_quote_guide_levels
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_sales_quote_guide_rules_updated_at ON sales_quote_guide_rules;
CREATE TRIGGER trg_sales_quote_guide_rules_updated_at
BEFORE UPDATE ON sales_quote_guide_rules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_sales_quote_ground_agent_rules_updated_at ON sales_quote_ground_agent_rules;
CREATE TRIGGER trg_sales_quote_ground_agent_rules_updated_at
BEFORE UPDATE ON sales_quote_ground_agent_rules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_sales_quote_approval_members_updated_at ON sales_quote_approval_members;
CREATE TRIGGER trg_sales_quote_approval_members_updated_at
BEFORE UPDATE ON sales_quote_approval_members
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_sales_quote_approval_configs_updated_at ON sales_quote_approval_configs;
CREATE TRIGGER trg_sales_quote_approval_configs_updated_at
BEFORE UPDATE ON sales_quote_approval_configs
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_quote_resource_rule_active
  ON sales_quote_resource_rules (tenant_id, resource_type, COALESCE(customer_category_id, 0))
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_sales_quote_resource_rule_page
  ON sales_quote_resource_rules (tenant_id, is_deleted, resource_type, status, id DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_quote_guide_level_name_active
  ON sales_quote_guide_levels (tenant_id, level_name)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_sales_quote_guide_level_active
  ON sales_quote_guide_levels (tenant_id, is_deleted, status, sort_order, id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_quote_guide_rule_active
  ON sales_quote_guide_rules (tenant_id, guide_level_id, language)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_sales_quote_guide_rule_page
  ON sales_quote_guide_rules (tenant_id, is_deleted, guide_level_id, status, id DESC);

CREATE INDEX IF NOT EXISTS idx_enterprise_guides_guide_level
  ON enterprise_guides (tenant_id, is_deleted, guide_level_id, status);

CREATE INDEX IF NOT EXISTS idx_sales_quote_ground_agent_rule_page
  ON sales_quote_ground_agent_rules (tenant_id, is_deleted, status, min_people, max_people);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_quote_approval_member_active
  ON sales_quote_approval_members (tenant_id, member_type, system_user_id)
  WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_quote_approval_approver_step_active
  ON sales_quote_approval_members (tenant_id, step_order)
  WHERE is_deleted = false AND member_type = 'approver';

CREATE INDEX IF NOT EXISTS idx_sales_quote_approval_member_user
  ON sales_quote_approval_members (tenant_id, is_deleted, system_user_id, member_type);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_quote_approval_config_active
  ON sales_quote_approval_configs (tenant_id)
  WHERE is_deleted = false;

INSERT INTO sales_quote_guide_levels (
  tenant_id,
  level_name,
  sort_order,
  status,
  created_by,
  remark,
  is_deleted
)
SELECT tenant.id, seed.level_name, seed.sort_order, 'active', 'system', '系统初始化导游等级', false
FROM tenants tenant
CROSS JOIN (
  VALUES ('普通', 10), ('资深', 20), ('金牌', 30)
) AS seed(level_name, sort_order)
WHERE tenant.is_deleted = false
  AND NOT EXISTS (
    SELECT 1
    FROM sales_quote_guide_levels existing
    WHERE existing.tenant_id = tenant.id
      AND existing.is_deleted = false
      AND existing.level_name = seed.level_name
  );

COMMENT ON TABLE sales_quote_resource_rules IS '普通资源销售报价规则表。按资源类型和客户等级维护采购成本基础上的建议上浮和最低上浮规则，并限定可用报价方式。';
COMMENT ON COLUMN sales_quote_resource_rules.id IS '报价规则主键 ID。';
COMMENT ON COLUMN sales_quote_resource_rules.tenant_id IS '租户 ID。';
COMMENT ON COLUMN sales_quote_resource_rules.resource_type IS '资源类型。scenic景区，hotel酒店，restaurant餐饮，vehicle用车，transport大交通，other其它资源，misc杂费。';
COMMENT ON COLUMN sales_quote_resource_rules.customer_category_id IS '客户等级或分类 ID。为空表示不区分客户等级的默认规则。';
COMMENT ON COLUMN sales_quote_resource_rules.customer_category_name IS '客户等级或分类名称快照，用于列表快速展示和规则历史识别。';
COMMENT ON COLUMN sales_quote_resource_rules.quote_mode IS '报价方式。rate按比例报价，fixed按固定加价报价，both两种方式均可报价。';
COMMENT ON COLUMN sales_quote_resource_rules.suggested_markup_rate IS '建议比例上浮，0.1000 表示在采购成本上上浮 10%。';
COMMENT ON COLUMN sales_quote_resource_rules.minimum_markup_rate IS '最低比例上浮，低于该比例计算的销售价需要触发报价审批。';
COMMENT ON COLUMN sales_quote_resource_rules.suggested_fixed_markup IS '建议固定加价金额，报价人选择固定加价方式时使用。';
COMMENT ON COLUMN sales_quote_resource_rules.minimum_fixed_markup IS '最低固定加价金额，低于该金额计算的销售价需要触发报价审批。';
COMMENT ON COLUMN sales_quote_resource_rules.status IS '规则状态。active启用，disabled停用。';
COMMENT ON COLUMN sales_quote_resource_rules.created_by IS '创建人账号。';
COMMENT ON COLUMN sales_quote_resource_rules.remark IS '规则备注。';
COMMENT ON COLUMN sales_quote_resource_rules.created_at IS '创建时间。';
COMMENT ON COLUMN sales_quote_resource_rules.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_quote_resource_rules.is_deleted IS '是否软删除。';
COMMENT ON COLUMN sales_quote_resource_rules.deleted_at IS '软删除时间。';
COMMENT ON COLUMN sales_quote_resource_rules.deleted_by IS '软删除操作人。';

COMMENT ON TABLE sales_quote_guide_levels IS '导游等级配置表。用于导游档案单选等级和导游报价规则匹配。';
COMMENT ON COLUMN sales_quote_guide_levels.id IS '导游等级主键 ID。';
COMMENT ON COLUMN sales_quote_guide_levels.tenant_id IS '租户 ID。';
COMMENT ON COLUMN sales_quote_guide_levels.level_name IS '导游等级名称。';
COMMENT ON COLUMN sales_quote_guide_levels.sort_order IS '排序号，数字越小越靠前。';
COMMENT ON COLUMN sales_quote_guide_levels.status IS '等级状态。active启用，disabled停用。';
COMMENT ON COLUMN sales_quote_guide_levels.created_by IS '创建人账号。';
COMMENT ON COLUMN sales_quote_guide_levels.remark IS '等级备注。';
COMMENT ON COLUMN sales_quote_guide_levels.created_at IS '创建时间。';
COMMENT ON COLUMN sales_quote_guide_levels.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_quote_guide_levels.is_deleted IS '是否软删除。';
COMMENT ON COLUMN sales_quote_guide_levels.deleted_at IS '软删除时间。';
COMMENT ON COLUMN sales_quote_guide_levels.deleted_by IS '软删除操作人。';

COMMENT ON COLUMN enterprise_guides.guide_level_id IS '导游等级 ID，关联导游等级配置。为空表示暂未分级。';
COMMENT ON COLUMN enterprise_guides.guide_level_name IS '导游等级名称快照，用于导游列表快速展示。';

COMMENT ON TABLE sales_quote_guide_rules IS '导游销售报价规则表。按导游等级和服务语种维护基础导服费、外语加价和超时费。';
COMMENT ON COLUMN sales_quote_guide_rules.id IS '导游报价规则主键 ID。';
COMMENT ON COLUMN sales_quote_guide_rules.tenant_id IS '租户 ID。';
COMMENT ON COLUMN sales_quote_guide_rules.guide_level_id IS '导游等级 ID。';
COMMENT ON COLUMN sales_quote_guide_rules.guide_level_name IS '导游等级名称快照。';
COMMENT ON COLUMN sales_quote_guide_rules.language IS '服务语种。普通话表示不额外计算外语服务加价。';
COMMENT ON COLUMN sales_quote_guide_rules.base_daily_fee IS '基础导服费，按天计费。';
COMMENT ON COLUMN sales_quote_guide_rules.foreign_language_daily_markup IS '外语服务加价，作为基础导服费之外的按天加价。';
COMMENT ON COLUMN sales_quote_guide_rules.overtime_hourly_fee IS '超时费，按小时计费。';
COMMENT ON COLUMN sales_quote_guide_rules.status IS '规则状态。active启用，disabled停用。';
COMMENT ON COLUMN sales_quote_guide_rules.created_by IS '创建人账号。';
COMMENT ON COLUMN sales_quote_guide_rules.remark IS '规则备注。';
COMMENT ON COLUMN sales_quote_guide_rules.created_at IS '创建时间。';
COMMENT ON COLUMN sales_quote_guide_rules.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_quote_guide_rules.is_deleted IS '是否软删除。';
COMMENT ON COLUMN sales_quote_guide_rules.deleted_at IS '软删除时间。';
COMMENT ON COLUMN sales_quote_guide_rules.deleted_by IS '软删除操作人。';

COMMENT ON TABLE sales_quote_ground_agent_rules IS '地接销售报价规则表。按人数区间维护整团打包价。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.id IS '地接报价规则主键 ID。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.tenant_id IS '租户 ID。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.min_people IS '适用最小人数，包含边界。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.max_people IS '适用最大人数，包含边界。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.group_package_price IS '整团打包价，不是人均价。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.status IS '规则状态。active启用，disabled停用。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.created_by IS '创建人账号。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.remark IS '规则备注。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.created_at IS '创建时间。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.is_deleted IS '是否软删除。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.deleted_at IS '软删除时间。';
COMMENT ON COLUMN sales_quote_ground_agent_rules.deleted_by IS '软删除操作人。';

COMMENT ON TABLE sales_quote_approval_members IS '销售报价低价审批人员配置表。所有报价类型共用一套审批人和抄送人配置。';
COMMENT ON COLUMN sales_quote_approval_members.id IS '审批人员配置主键 ID。';
COMMENT ON COLUMN sales_quote_approval_members.tenant_id IS '租户 ID。';
COMMENT ON COLUMN sales_quote_approval_members.member_type IS '人员类型。approver审批人，cc抄送人。';
COMMENT ON COLUMN sales_quote_approval_members.system_user_id IS '指定系统用户 ID。';
COMMENT ON COLUMN sales_quote_approval_members.step_order IS '审批顺序。审批人从1开始，抄送人为0。';
COMMENT ON COLUMN sales_quote_approval_members.created_by IS '创建人账号。';
COMMENT ON COLUMN sales_quote_approval_members.created_at IS '创建时间。';
COMMENT ON COLUMN sales_quote_approval_members.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_quote_approval_members.is_deleted IS '是否软删除。';
COMMENT ON COLUMN sales_quote_approval_members.deleted_at IS '软删除时间。';
COMMENT ON COLUMN sales_quote_approval_members.deleted_by IS '软删除操作人。';

COMMENT ON TABLE sales_quote_approval_configs IS '销售报价低价审批模式配置表。维护当前登录账号所属部门负责人或指定人员审批模式。';
COMMENT ON COLUMN sales_quote_approval_configs.id IS '审批模式配置主键 ID。';
COMMENT ON COLUMN sales_quote_approval_configs.tenant_id IS '租户 ID。';
COMMENT ON COLUMN sales_quote_approval_configs.approval_mode IS '审批模式。department_manager按当前登录账号所属部门负责人审批，specified_person按指定人员审批。';
COMMENT ON COLUMN sales_quote_approval_configs.created_by IS '创建或首次保存人账号。';
COMMENT ON COLUMN sales_quote_approval_configs.created_at IS '创建时间。';
COMMENT ON COLUMN sales_quote_approval_configs.updated_at IS '更新时间。';
COMMENT ON COLUMN sales_quote_approval_configs.is_deleted IS '是否软删除。';
COMMENT ON COLUMN sales_quote_approval_configs.deleted_at IS '软删除时间。';
COMMENT ON COLUMN sales_quote_approval_configs.deleted_by IS '软删除操作人。';

COMMIT;

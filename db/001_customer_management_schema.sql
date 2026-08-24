-- 旅游接待管理系统：客户管理模块基础表
-- PostgreSQL

BEGIN;

CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY,
  tenant_code varchar(64) NOT NULL UNIQUE,
  tenant_name varchar(200) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'active',
  contact_name varchar(80),
  contact_phone varchar(40),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_tenants_status CHECK (status IN ('active', 'disabled'))
);

CREATE TABLE IF NOT EXISTS customer_categories (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  category_name varchar(100) NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  default_credit_limit numeric(14,2) NOT NULL DEFAULT 0,
  credit_term_days integer NOT NULL DEFAULT 0,
  allow_over_limit boolean NOT NULL DEFAULT false,
  CONSTRAINT chk_customer_categories_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT chk_customer_categories_default_credit_limit CHECK (default_credit_limit >= 0),
  CONSTRAINT chk_customer_categories_credit_term_days CHECK (credit_term_days BETWEEN 0 AND 3650),
  CONSTRAINT uk_customer_categories_tenant_id_id UNIQUE (tenant_id, id)
);

-- 仅用于让本脚本可在独立临时库中校验。正式库中企业资料表由企业资料基础脚本创建。
CREATE TABLE IF NOT EXISTS enterprise_departments (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  department_name varchar(160) NOT NULL,
  CONSTRAINT uk_enterprise_departments_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS enterprise_employees (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  employee_name varchar(80) NOT NULL,
  CONSTRAINT uk_enterprise_employees_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS customers (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  customer_code varchar(64),
  customer_name varchar(200) NOT NULL,
  category_id bigint,
  province varchar(80),
  city varchar(80),
  district varchar(80),
  department_name varchar(100),
  dispatcher_name varchar(80),
  contact_name varchar(80),
  contact_phone varchar(40),
  registrar_name varchar(80),
  contract_expire_date date,
  status varchar(20) NOT NULL DEFAULT 'active',
  remark text,
  created_by varchar(80),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  credit_limit numeric(14,2) NOT NULL DEFAULT 0,
  department_id bigint,
  dispatcher_employee_id bigint,
  settlement_method varchar(40) NOT NULL DEFAULT 'unlimited',
  bill_start_date date,
  bill_day integer,
  CONSTRAINT chk_customers_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT chk_customers_credit_limit CHECK (credit_limit >= 0),
  CONSTRAINT chk_customers_settlement_method CHECK (
    settlement_method IN (
      'unlimited',
      'cash',
      'monthly_1',
      'monthly_2',
      'monthly_3',
      'monthly_4',
      'monthly_5',
      'monthly_6',
      'monthly_7',
      'monthly_8',
      'monthly_9',
      'monthly_10',
      'monthly_11',
      'monthly_12'
    )
  ),
  CONSTRAINT chk_customers_bill_day CHECK (bill_day IS NULL OR (bill_day >= 1 AND bill_day <= 31)),
  CONSTRAINT uk_customers_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_customers_category FOREIGN KEY (tenant_id, category_id)
    REFERENCES customer_categories (tenant_id, id),
  CONSTRAINT fk_customers_department FOREIGN KEY (tenant_id, department_id)
    REFERENCES enterprise_departments (tenant_id, id),
  CONSTRAINT fk_customers_dispatcher_employee FOREIGN KEY (tenant_id, dispatcher_employee_id)
    REFERENCES enterprise_employees (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS customer_contracts (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  customer_id bigint,
  contract_no varchar(80) NOT NULL,
  contract_type smallint NOT NULL DEFAULT 0,
  customer_name varchar(200),
  start_date date,
  end_date date,
  settlement_method varchar(100),
  contract_file_url text,
  status varchar(20) NOT NULL DEFAULT 'active',
  print_status varchar(20),
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  legal_subject varchar(200),
  invoice_subject varchar(200),
  settlement_subject varchar(200),
  template_name varchar(120),
  reminder_days integer NOT NULL DEFAULT 30,
  attachment_id bigint,
  party_a_name varchar(200),
  party_a_phone varchar(40),
  party_a_fax varchar(40),
  party_a_address varchar(300),
  party_a_contact varchar(80),
  party_b_name varchar(200),
  party_b_phone varchar(40),
  party_b_fax varchar(40),
  party_b_address varchar(300),
  party_b_contact varchar(80),
  agreement_content text,
  other_content text,
  CONSTRAINT chk_customer_contracts_status CHECK (status IN ('active', 'disabled', 'terminated')),
  CONSTRAINT chk_customer_contracts_reminder_days CHECK (reminder_days >= 0),
  CONSTRAINT fk_customer_contracts_customer FOREIGN KEY (tenant_id, customer_id)
    REFERENCES customers (tenant_id, id)
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_tenants_updated_at ON tenants;
CREATE TRIGGER trg_tenants_updated_at
BEFORE UPDATE ON tenants
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_customer_categories_updated_at ON customer_categories;
CREATE TRIGGER trg_customer_categories_updated_at
BEFORE UPDATE ON customer_categories
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_customers_updated_at ON customers;
CREATE TRIGGER trg_customers_updated_at
BEFORE UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_customer_contracts_updated_at ON customer_contracts;
CREATE TRIGGER trg_customer_contracts_updated_at
BEFORE UPDATE ON customer_contracts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_tenants_deleted
  ON tenants (is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_customer_categories_tenant_deleted_status
  ON customer_categories (tenant_id, is_deleted, status, sort_order);

CREATE INDEX IF NOT EXISTS idx_customers_tenant_deleted_status
  ON customers (tenant_id, is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_customers_tenant_deleted_name
  ON customers (tenant_id, is_deleted, customer_name);

CREATE INDEX IF NOT EXISTS idx_customers_tenant_deleted_category
  ON customers (tenant_id, is_deleted, category_id);

CREATE INDEX IF NOT EXISTS idx_customers_tenant_deleted_department
  ON customers (tenant_id, is_deleted, department_id);

CREATE INDEX IF NOT EXISTS idx_customers_tenant_deleted_dispatcher
  ON customers (tenant_id, is_deleted, dispatcher_employee_id);

CREATE INDEX IF NOT EXISTS idx_customers_tenant_deleted_settlement
  ON customers (tenant_id, is_deleted, settlement_method);

CREATE INDEX IF NOT EXISTS idx_customers_tenant_deleted_contract_expire
  ON customers (tenant_id, is_deleted, contract_expire_date);

CREATE INDEX IF NOT EXISTS idx_customer_contracts_tenant_deleted_customer
  ON customer_contracts (tenant_id, is_deleted, customer_id);

CREATE INDEX IF NOT EXISTS idx_customer_contracts_tenant_deleted_status
  ON customer_contracts (tenant_id, is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_customer_contracts_tenant_deleted_end_date
  ON customer_contracts (tenant_id, is_deleted, end_date);

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_categories_tenant_name_active
  ON customer_categories (tenant_id, category_name)
  WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customers_tenant_code_active
  ON customers (tenant_id, customer_code)
  WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_contracts_tenant_contract_no_active
  ON customer_contracts (tenant_id, contract_no)
  WHERE is_deleted = false;

COMMENT ON TABLE tenants IS '租户表。一条记录代表一家使用平台的旅游地接公司，用于区分不同公司的账号、权限和业务数据归属。';
COMMENT ON COLUMN tenants.id IS '租户主键ID，系统内部使用。';
COMMENT ON COLUMN tenants.tenant_code IS '租户编码，平台内唯一，用于识别一家地接公司。';
COMMENT ON COLUMN tenants.tenant_name IS '租户名称，即地接公司名称。';
COMMENT ON COLUMN tenants.status IS '租户状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN tenants.contact_name IS '联系人姓名，通常为该公司的管理员或负责人。';
COMMENT ON COLUMN tenants.contact_phone IS '联系人手机号或联系电话。';
COMMENT ON COLUMN tenants.remark IS '备注，用于记录开通说明、合作信息或内部说明。';
COMMENT ON COLUMN tenants.created_at IS '创建时间。';
COMMENT ON COLUMN tenants.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN tenants.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN tenants.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN tenants.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE customer_categories IS '客户分类表。用于维护客户分类字典，例如A类客户、B类客户、旅行社、单位客户等，供客户主档、订单和统计模块使用。';
COMMENT ON COLUMN customer_categories.id IS '客户分类主键ID，系统内部使用。';
COMMENT ON COLUMN customer_categories.tenant_id IS '租户ID，标识该客户分类属于哪一家地接公司。';
COMMENT ON COLUMN customer_categories.category_name IS '客户分类名称，例如A类客户、B类客户、组团旅行社、单位客户等。';
COMMENT ON COLUMN customer_categories.default_credit_limit IS '默认授信额度。新增或切换客户分类时可带入客户主档，客户保存后可单独调整。';
COMMENT ON COLUMN customer_categories.credit_term_days IS '默认账期天数，取值0到3650。0表示不提供账期。';
COMMENT ON COLUMN customer_categories.allow_over_limit IS '是否允许客户授信超额后发起审批。false表示超额不可申请，true表示按本等级审批流程处理。';
COMMENT ON COLUMN customer_categories.sort_order IS '排序号，数字越小越靠前。';
COMMENT ON COLUMN customer_categories.status IS '分类状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN customer_categories.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN customer_categories.remark IS '备注，用于说明分类规则或管理口径。';
COMMENT ON COLUMN customer_categories.created_at IS '创建时间。';
COMMENT ON COLUMN customer_categories.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN customer_categories.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN customer_categories.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN customer_categories.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE customers IS '客户单位表。用于维护下游客户、组团社、批发商、单位客户等客户主档，是订单、团队、应收账款、合同提醒和额度提醒的基础数据。';
COMMENT ON COLUMN customers.id IS '客户单位主键ID，系统内部使用。';
COMMENT ON COLUMN customers.tenant_id IS '租户ID，标识该客户属于哪一家地接公司。';
COMMENT ON COLUMN customers.customer_code IS '客户编码或业务代码，同一租户内唯一。';
COMMENT ON COLUMN customers.customer_name IS '客户单位名称。';
COMMENT ON COLUMN customers.category_id IS '客户分类ID，关联客户分类表。';
COMMENT ON COLUMN customers.credit_limit IS '客户实际授信额度。默认可来自客户分类，但保存后按客户独立维护。';
COMMENT ON COLUMN customers.province IS '客户所在地省份。';
COMMENT ON COLUMN customers.city IS '客户所在地城市。';
COMMENT ON COLUMN customers.district IS '客户所在地区县。';
COMMENT ON COLUMN customers.department_id IS '归属部门ID，用于关联企业部门资料。为空表示全公司可见或暂未分配。';
COMMENT ON COLUMN customers.department_name IS '归属部门名称冗余字段，用于列表展示和历史数据兼容。';
COMMENT ON COLUMN customers.dispatcher_employee_id IS '默认操作计调员工ID，用于关联企业员工资料。为空表示未分配。';
COMMENT ON COLUMN customers.dispatcher_name IS '默认操作计调姓名冗余字段，用于列表展示和历史数据兼容。';
COMMENT ON COLUMN customers.settlement_method IS '客户结款方式。unlimited表示不限，cash表示现结，monthly_1到monthly_12表示按1到12个月账期结款。';
COMMENT ON COLUMN customers.bill_start_date IS '客户账单起始日期，用于计算应收账期和结款提醒。';
COMMENT ON COLUMN customers.bill_day IS '客户约定结款日，取值1到31；为空表示未约定固定结款日。';
COMMENT ON COLUMN customers.contact_name IS '客户负责人或联系人姓名。';
COMMENT ON COLUMN customers.contact_phone IS '客户负责人或联系人电话。';
COMMENT ON COLUMN customers.registrar_name IS '登记人名称。';
COMMENT ON COLUMN customers.contract_expire_date IS '客户合同有效期止，用于下单或排团时进行到期提醒。';
COMMENT ON COLUMN customers.status IS '客户主档状态。active表示启用，disabled表示停用。合同未签、即将到期、已过期不写入本字段，应由合同数据计算。';
COMMENT ON COLUMN customers.remark IS '客户备注，用于记录合作说明、特殊结算要求或内部备注。';
COMMENT ON COLUMN customers.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN customers.created_at IS '创建时间。';
COMMENT ON COLUMN customers.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN customers.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN customers.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN customers.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE customer_contracts IS '客户合同表。用于维护客户销售合同台账，记录合同编号、客户单位、合同期限、结款方式、合同文件和合同状态，并支撑合同到期提醒。';
COMMENT ON COLUMN customer_contracts.id IS '客户合同主键ID，系统内部使用。';
COMMENT ON COLUMN customer_contracts.tenant_id IS '租户ID，标识该客户合同属于哪一家地接公司。';
COMMENT ON COLUMN customer_contracts.customer_id IS '客户单位ID，关联客户单位表。历史合同无法匹配客户时可为空，新建合同应绑定客户。';
COMMENT ON COLUMN customer_contracts.contract_no IS '合同编号，同一租户内唯一。';
COMMENT ON COLUMN customer_contracts.contract_type IS '合同类型。0表示客户合同，其他类型可用于后续扩展。';
COMMENT ON COLUMN customer_contracts.customer_name IS '合同对应公司名称或客户名称冗余字段，用于保留合同原始抬头。';
COMMENT ON COLUMN customer_contracts.start_date IS '合同开始日期。';
COMMENT ON COLUMN customer_contracts.end_date IS '合同结束日期或有效期止，用于合同到期提醒。';
COMMENT ON COLUMN customer_contracts.settlement_method IS '结款方式，如月结、现结等。';
COMMENT ON COLUMN customer_contracts.contract_file_url IS '合同文件地址，用于保存合同扫描件、电子文件或外部文件链接。';
COMMENT ON COLUMN customer_contracts.status IS '合同管理状态。active表示正常使用，disabled表示停用，terminated表示提前终止。合同自然到期应由end_date计算。';
COMMENT ON COLUMN customer_contracts.print_status IS '打印状态或打印标记。';
COMMENT ON COLUMN customer_contracts.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN customer_contracts.remark IS '合同备注，用于记录补充条款、特殊结算说明或内部备注。';
COMMENT ON COLUMN customer_contracts.created_at IS '创建时间。';
COMMENT ON COLUMN customer_contracts.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN customer_contracts.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN customer_contracts.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN customer_contracts.deleted_by IS '删除人账号或名称。未删除时为空。';
COMMENT ON COLUMN customer_contracts.legal_subject IS '客户合同主体，用于记录合同签署的法律主体。';
COMMENT ON COLUMN customer_contracts.invoice_subject IS '开票主体，用于记录发票抬头或开票责任主体。';
COMMENT ON COLUMN customer_contracts.settlement_subject IS '结算主体，用于记录应收结算归属主体。';
COMMENT ON COLUMN customer_contracts.template_name IS '合同模板名称。';
COMMENT ON COLUMN customer_contracts.reminder_days IS '到期提醒提前天数。';
COMMENT ON COLUMN customer_contracts.attachment_id IS '合同附件ID，关联公共附件表。';
COMMENT ON COLUMN customer_contracts.party_a_name IS '甲方名称快照，通常为本企业名称，可从企业公司信息带入后手工调整。';
COMMENT ON COLUMN customer_contracts.party_a_phone IS '甲方联系电话快照。';
COMMENT ON COLUMN customer_contracts.party_a_fax IS '甲方传真号码快照。';
COMMENT ON COLUMN customer_contracts.party_a_address IS '甲方办公地址快照。';
COMMENT ON COLUMN customer_contracts.party_a_contact IS '甲方负责人或联系人快照。';
COMMENT ON COLUMN customer_contracts.party_b_name IS '乙方名称快照，通常为客户单位名称，可从客户主档带入后手工调整。';
COMMENT ON COLUMN customer_contracts.party_b_phone IS '乙方联系电话快照。';
COMMENT ON COLUMN customer_contracts.party_b_fax IS '乙方传真号码快照。';
COMMENT ON COLUMN customer_contracts.party_b_address IS '乙方地址快照。';
COMMENT ON COLUMN customer_contracts.party_b_contact IS '乙方负责人或联系人快照。';
COMMENT ON COLUMN customer_contracts.agreement_content IS '合同约定内容，用于记录双方核心合作约定。';
COMMENT ON COLUMN customer_contracts.other_content IS '合同其它内容，用于记录补充条款或未尽事项。';

COMMENT ON INDEX uk_customer_categories_tenant_name_active IS '客户分类名称唯一索引，仅约束未删除记录。';
COMMENT ON INDEX uk_customers_tenant_code_active IS '客户编码唯一索引，仅约束未删除记录。';
COMMENT ON INDEX uk_customer_contracts_tenant_contract_no_active IS '客户合同编号唯一索引，仅约束未删除记录。';

INSERT INTO tenants (tenant_code, tenant_name, status, remark)
VALUES ('default', '默认地接公司', 'active', '系统初始化默认租户')
ON CONFLICT (tenant_code) DO NOTHING;

COMMIT;

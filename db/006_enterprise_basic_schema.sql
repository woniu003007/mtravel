-- 旅游接待管理系统：企业资料基础表
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 仅用于让本脚本可在独立临时库中校验。正式库中 tenants 已由客户管理基础脚本创建。
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS enterprise_bank_accounts (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  bank_name varchar(200) NOT NULL,
  account_name varchar(200) NOT NULL,
  account_no varchar(200) NOT NULL,
  print_enabled boolean NOT NULL DEFAULT false,
  other_info text,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_enterprise_bank_accounts_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_enterprise_bank_accounts_tenant_id_id UNIQUE (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_enterprise_bank_accounts_updated_at ON enterprise_bank_accounts;
CREATE TRIGGER trg_enterprise_bank_accounts_updated_at
BEFORE UPDATE ON enterprise_bank_accounts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_enterprise_bank_accounts_tenant_deleted_status
  ON enterprise_bank_accounts (tenant_id, is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_enterprise_bank_accounts_tenant_deleted_print
  ON enterprise_bank_accounts (tenant_id, is_deleted, print_enabled);

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_bank_accounts_tenant_account_no_active
  ON enterprise_bank_accounts (tenant_id, account_no)
  WHERE is_deleted = false;

COMMENT ON TABLE enterprise_bank_accounts IS '企业银行账号表。用于维护企业收款、付款、确认件打印和现金账授权可使用的账户资料。';
COMMENT ON COLUMN enterprise_bank_accounts.id IS '企业银行账号主键ID，系统内部使用。';
COMMENT ON COLUMN enterprise_bank_accounts.tenant_id IS '租户ID，标识该账号属于哪一家地接公司。';
COMMENT ON COLUMN enterprise_bank_accounts.bank_name IS '开户行或账户类型，例如银行名称、支付宝、微信、客户现付、房券。';
COMMENT ON COLUMN enterprise_bank_accounts.account_name IS '户名或收款主体名称。';
COMMENT ON COLUMN enterprise_bank_accounts.account_no IS '银行账号、支付账号或内部结算方式标识。';
COMMENT ON COLUMN enterprise_bank_accounts.print_enabled IS '是否在打印单据或确认件中展示。true表示展示，false表示不展示。';
COMMENT ON COLUMN enterprise_bank_accounts.other_info IS '其它账户说明，例如银行地址、SWIFT、联行号或境外汇款资料。';
COMMENT ON COLUMN enterprise_bank_accounts.status IS '账号状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN enterprise_bank_accounts.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN enterprise_bank_accounts.remark IS '备注，用于记录账户管理说明。';
COMMENT ON COLUMN enterprise_bank_accounts.created_at IS '创建时间。';
COMMENT ON COLUMN enterprise_bank_accounts.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN enterprise_bank_accounts.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN enterprise_bank_accounts.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN enterprise_bank_accounts.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_enterprise_bank_accounts_tenant_account_no_active IS '企业银行账号唯一索引，仅约束同一租户下未删除记录。';

CREATE TABLE IF NOT EXISTS enterprise_company_infos (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  company_name varchar(200) NOT NULL,
  province varchar(80),
  city varchar(80),
  district varchar(80),
  contact_name varchar(80),
  contact_phone varchar(40),
  fax_number varchar(40),
  office_address varchar(300),
  alipay_enterprise_name varchar(200),
  alipay_account varchar(160),
  alipay_nickname varchar(120),
  sign_status varchar(20) NOT NULL DEFAULT 'unsigned',
  sign_link text,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_enterprise_company_infos_sign_status CHECK (sign_status IN ('unsigned', 'signed')),
  CONSTRAINT chk_enterprise_company_infos_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_enterprise_company_infos_tenant_id_id UNIQUE (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_enterprise_company_infos_updated_at ON enterprise_company_infos;
CREATE TRIGGER trg_enterprise_company_infos_updated_at
BEFORE UPDATE ON enterprise_company_infos
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_enterprise_company_infos_tenant_deleted_status
  ON enterprise_company_infos (tenant_id, is_deleted, status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_company_infos_tenant_active
  ON enterprise_company_infos (tenant_id)
  WHERE is_deleted = false;

COMMENT ON TABLE enterprise_company_infos IS '企业公司信息表。用于维护本企业合同甲方、确认件抬头、签约账号和基础联系信息。';
COMMENT ON COLUMN enterprise_company_infos.id IS '企业公司信息主键ID，系统内部使用。';
COMMENT ON COLUMN enterprise_company_infos.tenant_id IS '租户ID，标识该公司信息属于哪一家地接公司。';
COMMENT ON COLUMN enterprise_company_infos.company_name IS '公司名称，用于合同甲方、确认件抬头和企业资料展示。';
COMMENT ON COLUMN enterprise_company_infos.province IS '公司所在省份。';
COMMENT ON COLUMN enterprise_company_infos.city IS '公司所在城市。';
COMMENT ON COLUMN enterprise_company_infos.district IS '公司所在区县。';
COMMENT ON COLUMN enterprise_company_infos.contact_name IS '公司负责人或联系人姓名。';
COMMENT ON COLUMN enterprise_company_infos.contact_phone IS '公司联系电话。';
COMMENT ON COLUMN enterprise_company_infos.fax_number IS '公司传真号码。';
COMMENT ON COLUMN enterprise_company_infos.office_address IS '公司办公地址。';
COMMENT ON COLUMN enterprise_company_infos.alipay_enterprise_name IS '支付宝企业名称。';
COMMENT ON COLUMN enterprise_company_infos.alipay_account IS '支付宝登录账号或签约账号。';
COMMENT ON COLUMN enterprise_company_infos.alipay_nickname IS '支付宝企业简称。';
COMMENT ON COLUMN enterprise_company_infos.sign_status IS '企业签约状态。unsigned表示未签约，signed表示已签约。';
COMMENT ON COLUMN enterprise_company_infos.sign_link IS '签约链接或外部签约地址。';
COMMENT ON COLUMN enterprise_company_infos.status IS '公司信息状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN enterprise_company_infos.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN enterprise_company_infos.remark IS '备注，用于记录公司信息维护说明。';
COMMENT ON COLUMN enterprise_company_infos.created_at IS '创建时间。';
COMMENT ON COLUMN enterprise_company_infos.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN enterprise_company_infos.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN enterprise_company_infos.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN enterprise_company_infos.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_enterprise_company_infos_tenant_active IS '企业公司信息唯一索引，仅允许同一租户保留一条未删除公司信息。';

CREATE TABLE IF NOT EXISTS enterprise_departments (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  parent_id bigint,
  department_code varchar(80),
  department_name varchar(160) NOT NULL,
  manager_name varchar(80),
  contact_phone varchar(40),
  sort_order integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_enterprise_departments_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_enterprise_departments_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_enterprise_departments_parent
    FOREIGN KEY (tenant_id, parent_id) REFERENCES enterprise_departments (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_enterprise_departments_updated_at ON enterprise_departments;
CREATE TRIGGER trg_enterprise_departments_updated_at
BEFORE UPDATE ON enterprise_departments
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_enterprise_departments_tenant_deleted_status
  ON enterprise_departments (tenant_id, is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_enterprise_departments_tenant_deleted_parent
  ON enterprise_departments (tenant_id, is_deleted, parent_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_enterprise_departments_tenant_deleted_sort
  ON enterprise_departments (tenant_id, is_deleted, sort_order, id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_departments_tenant_name_active
  ON enterprise_departments (tenant_id, department_name)
  WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_departments_tenant_code_active
  ON enterprise_departments (tenant_id, department_code)
  WHERE is_deleted = false AND department_code IS NOT NULL;

COMMENT ON TABLE enterprise_departments IS '企业部门表。用于维护企业组织架构中的部门资料，供员工、角色、计调归属和业务统计使用。';
COMMENT ON COLUMN enterprise_departments.id IS '企业部门主键ID，系统内部使用。';
COMMENT ON COLUMN enterprise_departments.tenant_id IS '租户ID，标识该部门属于哪一家地接公司。';
COMMENT ON COLUMN enterprise_departments.parent_id IS '上级部门ID。为空表示一级部门。';
COMMENT ON COLUMN enterprise_departments.department_code IS '部门编码，用于内部识别和外部数据导入匹配。';
COMMENT ON COLUMN enterprise_departments.department_name IS '部门名称，例如销售部、计调部、财务部。';
COMMENT ON COLUMN enterprise_departments.manager_name IS '部门负责人姓名。';
COMMENT ON COLUMN enterprise_departments.contact_phone IS '部门联系电话或负责人联系电话。';
COMMENT ON COLUMN enterprise_departments.sort_order IS '排序值。数字越小越靠前。';
COMMENT ON COLUMN enterprise_departments.status IS '部门状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN enterprise_departments.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN enterprise_departments.remark IS '备注，用于记录部门管理说明。';
COMMENT ON COLUMN enterprise_departments.created_at IS '创建时间。';
COMMENT ON COLUMN enterprise_departments.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN enterprise_departments.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN enterprise_departments.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN enterprise_departments.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_enterprise_departments_tenant_name_active IS '企业部门名称唯一索引，仅约束同一租户下未删除记录。';
COMMENT ON INDEX uk_enterprise_departments_tenant_code_active IS '企业部门编码唯一索引，仅约束同一租户下未删除且编码不为空的记录。';

CREATE TABLE IF NOT EXISTS enterprise_roles (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  role_code varchar(80) NOT NULL,
  role_name varchar(160) NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  system_builtin boolean NOT NULL DEFAULT false,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_enterprise_roles_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_enterprise_roles_tenant_id_id UNIQUE (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_enterprise_roles_updated_at ON enterprise_roles;
CREATE TRIGGER trg_enterprise_roles_updated_at
BEFORE UPDATE ON enterprise_roles
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_enterprise_roles_tenant_deleted_status
  ON enterprise_roles (tenant_id, is_deleted, status, sort_order);

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_roles_tenant_code_active
  ON enterprise_roles (tenant_id, role_code)
  WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_roles_tenant_name_active
  ON enterprise_roles (tenant_id, role_name)
  WHERE is_deleted = false;

COMMENT ON TABLE enterprise_roles IS '企业角色表。用于维护后台用户角色，支撑员工账号归属、菜单权限入口和后续按钮权限控制。';
COMMENT ON COLUMN enterprise_roles.id IS '企业角色主键ID，系统内部使用。';
COMMENT ON COLUMN enterprise_roles.tenant_id IS '租户ID，标识该角色属于哪一家地接公司。';
COMMENT ON COLUMN enterprise_roles.role_code IS '角色编码，用于登录令牌、权限判断和外部数据导入匹配。';
COMMENT ON COLUMN enterprise_roles.role_name IS '角色名称，例如管理员、销售、计调、财务、总经理。';
COMMENT ON COLUMN enterprise_roles.sort_order IS '排序值。数字越小越靠前。';
COMMENT ON COLUMN enterprise_roles.system_builtin IS '是否系统内置角色。true表示初始化角色，业务界面默认不建议删除。';
COMMENT ON COLUMN enterprise_roles.status IS '角色状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN enterprise_roles.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN enterprise_roles.remark IS '备注，用于记录角色职责或管理说明。';
COMMENT ON COLUMN enterprise_roles.created_at IS '创建时间。';
COMMENT ON COLUMN enterprise_roles.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN enterprise_roles.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN enterprise_roles.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN enterprise_roles.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_enterprise_roles_tenant_code_active IS '企业角色编码唯一索引，仅约束同一租户下未删除记录。';
COMMENT ON INDEX uk_enterprise_roles_tenant_name_active IS '企业角色名称唯一索引，仅约束同一租户下未删除记录。';

CREATE TABLE IF NOT EXISTS enterprise_role_permissions (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  role_id bigint NOT NULL,
  module_code varchar(80) NOT NULL,
  module_name varchar(120) NOT NULL,
  permission_code varchar(120) NOT NULL,
  permission_name varchar(160) NOT NULL,
  permission_type varchar(20) NOT NULL DEFAULT 'menu',
  sort_order integer NOT NULL DEFAULT 0,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_enterprise_role_permissions_type CHECK (permission_type IN ('menu', 'button', 'data')),
  CONSTRAINT uk_enterprise_role_permissions_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_enterprise_role_permissions_role
    FOREIGN KEY (tenant_id, role_id) REFERENCES enterprise_roles (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_enterprise_role_permissions_updated_at ON enterprise_role_permissions;
CREATE TRIGGER trg_enterprise_role_permissions_updated_at
BEFORE UPDATE ON enterprise_role_permissions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_enterprise_role_permissions_tenant_deleted_role
  ON enterprise_role_permissions (tenant_id, is_deleted, role_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_enterprise_role_permissions_tenant_deleted_module
  ON enterprise_role_permissions (tenant_id, is_deleted, module_code, permission_type);

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_role_permissions_tenant_role_code_active
  ON enterprise_role_permissions (tenant_id, role_id, permission_code)
  WHERE is_deleted = false;

COMMENT ON TABLE enterprise_role_permissions IS '企业角色权限表。用于保存角色已分配的菜单、按钮或数据权限编码，首版作为权限管理入口。';
COMMENT ON COLUMN enterprise_role_permissions.id IS '角色权限主键ID，系统内部使用。';
COMMENT ON COLUMN enterprise_role_permissions.tenant_id IS '租户ID，标识该权限属于哪一家地接公司。';
COMMENT ON COLUMN enterprise_role_permissions.role_id IS '角色ID，关联企业角色表。';
COMMENT ON COLUMN enterprise_role_permissions.module_code IS '模块编码，用于按业务模块分组展示权限。';
COMMENT ON COLUMN enterprise_role_permissions.module_name IS '模块名称，例如客户管理、销售管理、计调操作、财务管理。';
COMMENT ON COLUMN enterprise_role_permissions.permission_code IS '权限编码，用于菜单、按钮或数据范围判断。';
COMMENT ON COLUMN enterprise_role_permissions.permission_name IS '权限名称，用于权限管理界面展示。';
COMMENT ON COLUMN enterprise_role_permissions.permission_type IS '权限类型。menu表示菜单，button表示按钮，data表示数据范围。';
COMMENT ON COLUMN enterprise_role_permissions.sort_order IS '排序值。数字越小越靠前。';
COMMENT ON COLUMN enterprise_role_permissions.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN enterprise_role_permissions.remark IS '备注，用于记录权限配置说明。';
COMMENT ON COLUMN enterprise_role_permissions.created_at IS '创建时间。';
COMMENT ON COLUMN enterprise_role_permissions.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN enterprise_role_permissions.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN enterprise_role_permissions.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN enterprise_role_permissions.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_enterprise_role_permissions_tenant_role_code_active IS '企业角色权限唯一索引，仅约束同一租户下同一角色未删除权限编码。';

CREATE TABLE IF NOT EXISTS enterprise_employees (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  system_user_id bigint,
  employee_code varchar(80),
  employee_name varchar(80) NOT NULL,
  username varchar(80) NOT NULL,
  department_id bigint,
  role_id bigint,
  gender varchar(20) NOT NULL DEFAULT 'unknown',
  telephone varchar(40),
  mobile_phone varchar(40),
  email varchar(120),
  info_scope varchar(30) NOT NULL DEFAULT 'personal',
  profit_scope varchar(30) NOT NULL DEFAULT 'personal',
  reception_scope varchar(30) NOT NULL DEFAULT 'personal',
  customer_scope varchar(30) NOT NULL DEFAULT 'personal',
  sort_order integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_enterprise_employees_gender CHECK (gender IN ('male', 'female', 'unknown')),
  CONSTRAINT chk_enterprise_employees_info_scope CHECK (info_scope IN ('company', 'department', 'personal')),
  CONSTRAINT chk_enterprise_employees_profit_scope CHECK (profit_scope IN ('company', 'department', 'personal')),
  CONSTRAINT chk_enterprise_employees_reception_scope CHECK (reception_scope IN ('company', 'department', 'personal')),
  CONSTRAINT chk_enterprise_employees_customer_scope CHECK (customer_scope IN ('company', 'department', 'personal')),
  CONSTRAINT chk_enterprise_employees_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_enterprise_employees_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_enterprise_employees_department
    FOREIGN KEY (tenant_id, department_id) REFERENCES enterprise_departments (tenant_id, id),
  CONSTRAINT fk_enterprise_employees_role
    FOREIGN KEY (tenant_id, role_id) REFERENCES enterprise_roles (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_enterprise_employees_updated_at ON enterprise_employees;
CREATE TRIGGER trg_enterprise_employees_updated_at
BEFORE UPDATE ON enterprise_employees
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_enterprise_employees_tenant_deleted_status
  ON enterprise_employees (tenant_id, is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_enterprise_employees_tenant_deleted_department
  ON enterprise_employees (tenant_id, is_deleted, department_id, status);

CREATE INDEX IF NOT EXISTS idx_enterprise_employees_tenant_deleted_role
  ON enterprise_employees (tenant_id, is_deleted, role_id, status);

CREATE INDEX IF NOT EXISTS idx_enterprise_employees_tenant_deleted_sort
  ON enterprise_employees (tenant_id, is_deleted, sort_order, id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_employees_tenant_code_active
  ON enterprise_employees (tenant_id, employee_code)
  WHERE is_deleted = false AND employee_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_employees_tenant_username_active
  ON enterprise_employees (tenant_id, username)
  WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_employees_tenant_system_user_active
  ON enterprise_employees (tenant_id, system_user_id)
  WHERE is_deleted = false AND system_user_id IS NOT NULL;

COMMENT ON TABLE enterprise_employees IS '企业员工表。用于维护员工基础资料、所属部门、角色归属、登录账号和数据查看范围。';
COMMENT ON COLUMN enterprise_employees.id IS '企业员工主键ID，系统内部使用。';
COMMENT ON COLUMN enterprise_employees.tenant_id IS '租户ID，标识该员工属于哪一家地接公司。';
COMMENT ON COLUMN enterprise_employees.system_user_id IS '系统用户ID，用于关联后台登录账号。';
COMMENT ON COLUMN enterprise_employees.employee_code IS '员工业务编码，用于业务归属、外部导入或内部识别。';
COMMENT ON COLUMN enterprise_employees.employee_name IS '员工姓名，用于业务页面、单据和操作留痕展示。';
COMMENT ON COLUMN enterprise_employees.username IS '登录账号冗余值，用于员工列表展示和账号查重。';
COMMENT ON COLUMN enterprise_employees.department_id IS '所属部门ID。为空表示暂未分配部门。';
COMMENT ON COLUMN enterprise_employees.role_id IS '角色ID。为空表示暂未分配角色。';
COMMENT ON COLUMN enterprise_employees.gender IS '性别。male表示男，female表示女，unknown表示未填写。';
COMMENT ON COLUMN enterprise_employees.telephone IS '固定电话或办公室电话。';
COMMENT ON COLUMN enterprise_employees.mobile_phone IS '手机号码。';
COMMENT ON COLUMN enterprise_employees.email IS '邮箱地址。';
COMMENT ON COLUMN enterprise_employees.info_scope IS '信息查看范围。company表示全公司，department表示部门范围，personal表示个人信息。';
COMMENT ON COLUMN enterprise_employees.profit_scope IS '利润查看范围。company表示全公司，department表示部门范围，personal表示个人信息。';
COMMENT ON COLUMN enterprise_employees.reception_scope IS '收客查看范围。company表示全公司，department表示部门范围，personal表示个人信息。';
COMMENT ON COLUMN enterprise_employees.customer_scope IS '客户查看范围。company表示全公司，department表示部门范围，personal表示个人信息。';
COMMENT ON COLUMN enterprise_employees.sort_order IS '排序值。数字越小越靠前。';
COMMENT ON COLUMN enterprise_employees.status IS '员工状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN enterprise_employees.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN enterprise_employees.remark IS '备注，用于记录员工管理说明。';
COMMENT ON COLUMN enterprise_employees.created_at IS '创建时间。';
COMMENT ON COLUMN enterprise_employees.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN enterprise_employees.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN enterprise_employees.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN enterprise_employees.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_enterprise_employees_tenant_code_active IS '企业员工业务编码唯一索引，仅约束同一租户下未删除且编码不为空的记录。';
COMMENT ON INDEX uk_enterprise_employees_tenant_username_active IS '企业员工登录账号唯一索引，仅约束同一租户下未删除记录。';
COMMENT ON INDEX uk_enterprise_employees_tenant_system_user_active IS '企业员工关联登录账号唯一索引，仅约束同一租户下未删除且已关联登录账号的记录。';

CREATE TABLE IF NOT EXISTS enterprise_guides (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  guide_code varchar(80),
  guide_name varchar(80) NOT NULL,
  username varchar(80),
  gender varchar(20) NOT NULL DEFAULT 'unknown',
  certificate_no varchar(120),
  id_card_no varchar(120),
  telephone varchar(40),
  mobile_phone varchar(40),
  settlement_account varchar(200),
  alipay_account varchar(200),
  enterprise_code_account varchar(120),
  enterprise_code_status varchar(20) NOT NULL DEFAULT 'not_joined',
  status varchar(20) NOT NULL DEFAULT 'active',
  rating numeric(3,2) NOT NULL DEFAULT 0,
  total_tours integer NOT NULL DEFAULT 0,
  sort_order integer NOT NULL DEFAULT 0,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  guide_manager_employee_id bigint,
  guide_manager_name varchar(80),
  removed_column_slot_20260617_1 varchar(1),
  fax varchar(40),
  bank_name varchar(120),
  bank_account_no varchar(120),
  alipay_name varchar(80),
  enterprise_code_invited_at timestamptz,
  age integer,
  native_place varchar(120),
  working_years integer,
  languages varchar(200),
  personal_intro text,
  removed_column_slot_20260617_2 varchar(1),
  certificate_file_url varchar(500),
  photo_url varchar(500),
  CONSTRAINT fk_enterprise_guides_manager
    FOREIGN KEY (tenant_id, guide_manager_employee_id)
    REFERENCES enterprise_employees (tenant_id, id),
  CONSTRAINT chk_enterprise_guides_gender CHECK (gender IN ('male', 'female', 'unknown')),
  CONSTRAINT chk_enterprise_guides_enterprise_code_status CHECK (enterprise_code_status IN ('not_joined', 'invite_link', 'signed_success', 'bound', 'unbound', 'disabled')),
  CONSTRAINT chk_enterprise_guides_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT chk_enterprise_guides_age CHECK (age IS NULL OR age >= 0),
  CONSTRAINT chk_enterprise_guides_working_years CHECK (working_years IS NULL OR working_years >= 0),
  CONSTRAINT chk_enterprise_guides_rating CHECK (rating >= 0 AND rating <= 5),
  CONSTRAINT chk_enterprise_guides_total_tours CHECK (total_tours >= 0),
  CONSTRAINT uk_enterprise_guides_tenant_id_id UNIQUE (tenant_id, id)
);

ALTER TABLE enterprise_guides DROP COLUMN IF EXISTS removed_column_slot_20260617_1;
ALTER TABLE enterprise_guides DROP COLUMN IF EXISTS removed_column_slot_20260617_2;

DROP TRIGGER IF EXISTS trg_enterprise_guides_updated_at ON enterprise_guides;
CREATE TRIGGER trg_enterprise_guides_updated_at
BEFORE UPDATE ON enterprise_guides
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_enterprise_guides_tenant_deleted_status
  ON enterprise_guides (tenant_id, is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_enterprise_guides_tenant_deleted_manager
  ON enterprise_guides (tenant_id, is_deleted, guide_manager_employee_id, status);

CREATE INDEX IF NOT EXISTS idx_enterprise_guides_tenant_deleted_enterprise_code
  ON enterprise_guides (tenant_id, is_deleted, enterprise_code_status);

CREATE INDEX IF NOT EXISTS idx_enterprise_guides_tenant_deleted_sort
  ON enterprise_guides (tenant_id, is_deleted, sort_order, id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_guides_tenant_code_active
  ON enterprise_guides (tenant_id, guide_code)
  WHERE is_deleted = false AND guide_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_guides_tenant_username_active
  ON enterprise_guides (tenant_id, username)
  WHERE is_deleted = false AND username IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_guides_tenant_certificate_active
  ON enterprise_guides (tenant_id, certificate_no)
  WHERE is_deleted = false AND certificate_no IS NOT NULL;

COMMENT ON TABLE enterprise_guides IS '企业导游档案表。用于维护导游基础资料、联系方式、证件、结算账号、企业码状态和后续排班结算引用信息。';
COMMENT ON COLUMN enterprise_guides.id IS '导游档案主键ID，系统内部使用。';
COMMENT ON COLUMN enterprise_guides.tenant_id IS '租户ID，标识该导游属于哪一家地接公司。';
COMMENT ON COLUMN enterprise_guides.guide_code IS '导游编码，用于内部识别、导入匹配和排班引用。';
COMMENT ON COLUMN enterprise_guides.guide_name IS '导游姓名，用于排团、结算和统计展示。';
COMMENT ON COLUMN enterprise_guides.username IS '导游端登录用户名或外部系统用户名。';
COMMENT ON COLUMN enterprise_guides.guide_manager_employee_id IS '所属导管员工ID，用于导游绩效归属、分组管理和导管统计。';
COMMENT ON COLUMN enterprise_guides.guide_manager_name IS '所属导管员工姓名冗余，用于列表展示和历史归属留痕。';
COMMENT ON COLUMN enterprise_guides.gender IS '性别。male表示男，female表示女，unknown表示未填写。';
COMMENT ON COLUMN enterprise_guides.certificate_no IS '导游证件号或导游证编号。';
COMMENT ON COLUMN enterprise_guides.id_card_no IS '身份证号或身份识别号码。';
COMMENT ON COLUMN enterprise_guides.telephone IS '固定电话。';
COMMENT ON COLUMN enterprise_guides.fax IS '传真号码。';
COMMENT ON COLUMN enterprise_guides.mobile_phone IS '手机号码。';
COMMENT ON COLUMN enterprise_guides.settlement_account IS '历史结算账号字段，保留用于兼容既有数据。';
COMMENT ON COLUMN enterprise_guides.bank_name IS '银行名称，用于导游结算付款资料。';
COMMENT ON COLUMN enterprise_guides.bank_account_no IS '银行账号，用于导游结算付款资料。';
COMMENT ON COLUMN enterprise_guides.alipay_name IS '支付宝姓名，用于备用金发放、核销退补或导游结算付款。';
COMMENT ON COLUMN enterprise_guides.alipay_account IS '支付宝账号，用于备用金发放、核销退补或导游结算付款。';
COMMENT ON COLUMN enterprise_guides.enterprise_code_account IS '企业码账号或企业码绑定标识。';
COMMENT ON COLUMN enterprise_guides.enterprise_code_status IS '企业码状态。not_joined表示未加入企业码，invite_link表示已获取签约链接，signed_success表示已签约成功；bound、unbound、disabled为兼容状态。';
COMMENT ON COLUMN enterprise_guides.enterprise_code_invited_at IS '最近一次发送企业码签约邀请的时间。';
COMMENT ON COLUMN enterprise_guides.status IS '导游状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN enterprise_guides.age IS '年龄，用于导游展示资料和团队匹配参考。';
COMMENT ON COLUMN enterprise_guides.native_place IS '籍贯，用于导游展示资料。';
COMMENT ON COLUMN enterprise_guides.working_years IS '从业年数，用于导游展示资料和经验判断。';
COMMENT ON COLUMN enterprise_guides.languages IS '语言能力，例如普通话、英语或其它语种。';
COMMENT ON COLUMN enterprise_guides.personal_intro IS '个人介绍，用于导游展示资料。';
COMMENT ON COLUMN enterprise_guides.certificate_file_url IS '导游证书附件地址。';
COMMENT ON COLUMN enterprise_guides.photo_url IS '个人照片地址，用于导游展示资料。';
COMMENT ON COLUMN enterprise_guides.rating IS '导游评分，取值范围0到5，用于后续排班参考和导游统计。';
COMMENT ON COLUMN enterprise_guides.total_tours IS '累计带团次数，用于导游统计和排班参考。';
COMMENT ON COLUMN enterprise_guides.sort_order IS '排序值。数字越小越靠前。';
COMMENT ON COLUMN enterprise_guides.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN enterprise_guides.remark IS '备注，用于记录导游档案说明。';
COMMENT ON COLUMN enterprise_guides.created_at IS '创建时间。';
COMMENT ON COLUMN enterprise_guides.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN enterprise_guides.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN enterprise_guides.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN enterprise_guides.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_enterprise_guides_tenant_code_active IS '导游编码唯一索引，仅约束同一租户下未删除且编码不为空的记录。';
COMMENT ON INDEX uk_enterprise_guides_tenant_username_active IS '导游用户名唯一索引，仅约束同一租户下未删除且用户名不为空的记录。';
COMMENT ON INDEX uk_enterprise_guides_tenant_certificate_active IS '导游证件号唯一索引，仅约束同一租户下未删除且证件号不为空的记录。';

CREATE TABLE IF NOT EXISTS enterprise_guide_tags (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  tag_name varchar(80) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'active',
  sort_order integer NOT NULL DEFAULT 0,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_enterprise_guide_tags_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_enterprise_guide_tags_tenant_id_id UNIQUE (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_enterprise_guide_tags_updated_at ON enterprise_guide_tags;
CREATE TRIGGER trg_enterprise_guide_tags_updated_at
BEFORE UPDATE ON enterprise_guide_tags
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_enterprise_guide_tags_tenant_deleted_status
  ON enterprise_guide_tags (tenant_id, is_deleted, status, sort_order, id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_guide_tags_tenant_name_active
  ON enterprise_guide_tags (tenant_id, tag_name)
  WHERE is_deleted = false;

COMMENT ON TABLE enterprise_guide_tags IS '导游标签表。用于维护导游能力、线路经验、服务类型和内部分类标签。';
COMMENT ON COLUMN enterprise_guide_tags.id IS '导游标签主键ID，系统内部使用。';
COMMENT ON COLUMN enterprise_guide_tags.tenant_id IS '租户ID，标识该标签属于哪一家地接公司。';
COMMENT ON COLUMN enterprise_guide_tags.tag_name IS '标签名称，例如金牌导游、研学、亲子、英语。';
COMMENT ON COLUMN enterprise_guide_tags.status IS '标签状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN enterprise_guide_tags.sort_order IS '排序值。数字越小越靠前。';
COMMENT ON COLUMN enterprise_guide_tags.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN enterprise_guide_tags.remark IS '备注，用于记录标签使用说明。';
COMMENT ON COLUMN enterprise_guide_tags.created_at IS '创建时间。';
COMMENT ON COLUMN enterprise_guide_tags.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN enterprise_guide_tags.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN enterprise_guide_tags.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN enterprise_guide_tags.deleted_by IS '删除人账号或名称。未删除时为空。';
COMMENT ON INDEX uk_enterprise_guide_tags_tenant_name_active IS '导游标签名称唯一索引，仅约束同一租户下未删除记录。';

CREATE TABLE IF NOT EXISTS enterprise_guide_tag_relations (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  guide_id bigint NOT NULL,
  tag_id bigint NOT NULL,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT fk_enterprise_guide_tag_relations_guide
    FOREIGN KEY (tenant_id, guide_id)
    REFERENCES enterprise_guides (tenant_id, id),
  CONSTRAINT fk_enterprise_guide_tag_relations_tag
    FOREIGN KEY (tenant_id, tag_id)
    REFERENCES enterprise_guide_tags (tenant_id, id),
  CONSTRAINT uk_enterprise_guide_tag_relations_tenant_id_id UNIQUE (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_enterprise_guide_tag_relations_updated_at ON enterprise_guide_tag_relations;
CREATE TRIGGER trg_enterprise_guide_tag_relations_updated_at
BEFORE UPDATE ON enterprise_guide_tag_relations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_enterprise_guide_tag_relations_tenant_deleted_guide
  ON enterprise_guide_tag_relations (tenant_id, is_deleted, guide_id);

CREATE INDEX IF NOT EXISTS idx_enterprise_guide_tag_relations_tenant_deleted_tag
  ON enterprise_guide_tag_relations (tenant_id, is_deleted, tag_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_enterprise_guide_tag_relations_active
  ON enterprise_guide_tag_relations (tenant_id, guide_id, tag_id)
  WHERE is_deleted = false;

COMMENT ON TABLE enterprise_guide_tag_relations IS '导游标签关系表。用于维护导游档案与导游标签的多对多绑定关系。';
COMMENT ON COLUMN enterprise_guide_tag_relations.id IS '导游标签关系主键ID，系统内部使用。';
COMMENT ON COLUMN enterprise_guide_tag_relations.tenant_id IS '租户ID，标识该关系属于哪一家地接公司。';
COMMENT ON COLUMN enterprise_guide_tag_relations.guide_id IS '导游档案ID。';
COMMENT ON COLUMN enterprise_guide_tag_relations.tag_id IS '导游标签ID。';
COMMENT ON COLUMN enterprise_guide_tag_relations.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN enterprise_guide_tag_relations.remark IS '备注，用于记录标签绑定说明。';
COMMENT ON COLUMN enterprise_guide_tag_relations.created_at IS '创建时间。';
COMMENT ON COLUMN enterprise_guide_tag_relations.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN enterprise_guide_tag_relations.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN enterprise_guide_tag_relations.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN enterprise_guide_tag_relations.deleted_by IS '删除人账号或名称。未删除时为空。';
COMMENT ON INDEX uk_enterprise_guide_tag_relations_active IS '导游与标签关系唯一索引，仅约束同一租户下未删除的关系。';

INSERT INTO enterprise_roles (
  tenant_id,
  role_code,
  role_name,
  sort_order,
  system_builtin,
  status,
  created_by,
  remark
)
SELECT seed.tenant_id, seed.role_code, seed.role_name, seed.sort_order, true, 'active', 'system', seed.remark
FROM (
  SELECT id AS tenant_id, 'admin' AS role_code, '管理员' AS role_name, 10 AS sort_order, '系统管理、基础资料维护和权限配置角色' AS remark FROM tenants
  UNION ALL
  SELECT id, 'sales', '销售', 20, '销售收客、订单和客户资料维护角色' FROM tenants
  UNION ALL
  SELECT id, 'dispatch', '计调', 30, '团队安排、履约跟进和计调审核角色' FROM tenants
  UNION ALL
  SELECT id, 'finance', '财务', 40, '收付款、审核和结算管理角色' FROM tenants
  UNION ALL
  SELECT id, 'boss', '总经理', 50, '经营统计、全局看数和管理决策角色' FROM tenants
) seed
ON CONFLICT (tenant_id, role_code) WHERE is_deleted = false DO NOTHING;

DO $$
BEGIN
  IF to_regclass('public.system_users') IS NOT NULL THEN
    UPDATE system_users user_account
    SET role_id = role.id
    FROM enterprise_roles role
    WHERE user_account.tenant_id = role.tenant_id
      AND user_account.role_code = role.role_code
      AND user_account.role_id IS NULL
      AND user_account.is_deleted = false
      AND role.is_deleted = false;
  END IF;
END
$$;

COMMIT;

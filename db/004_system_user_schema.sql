-- 旅游接待管理系统：系统用户表
-- PostgreSQL

BEGIN;

-- 仅用于让本脚本可在独立临时库中校验。正式库中 tenants 已由客户管理基础脚本创建。
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY,
  tenant_code varchar(80) UNIQUE
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS system_users (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  username varchar(80) NOT NULL,
  password_hash varchar(200) NOT NULL,
  real_name varchar(80) NOT NULL,
  mobile_phone varchar(40),
  email varchar(120),
  role_code varchar(40) NOT NULL DEFAULT 'admin',
  is_tenant_admin boolean NOT NULL DEFAULT false,
  status varchar(20) NOT NULL DEFAULT 'active',
  last_login_at timestamptz,
  remark text,
  created_by varchar(80),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  role_id bigint,
  employee_id bigint,
  CONSTRAINT chk_system_users_status CHECK (status IN ('active', 'disabled', 'locked')),
  CONSTRAINT uk_system_users_tenant_id_id UNIQUE (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_system_users_updated_at ON system_users;
CREATE TRIGGER trg_system_users_updated_at
BEFORE UPDATE ON system_users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_system_users_tenant_deleted_status
  ON system_users (tenant_id, is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_system_users_tenant_deleted_role
  ON system_users (tenant_id, is_deleted, role_code);

CREATE INDEX IF NOT EXISTS idx_system_users_tenant_deleted_employee
  ON system_users (tenant_id, is_deleted, employee_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_system_users_tenant_username_active
  ON system_users (tenant_id, username)
  WHERE is_deleted = false;

COMMENT ON TABLE system_users IS '系统用户表。用于保存每个租户的后台登录账号、密码哈希、基础角色和账号状态，是登录认证、操作留痕和后续权限控制的基础。';
COMMENT ON COLUMN system_users.id IS '系统用户主键ID，系统内部使用。';
COMMENT ON COLUMN system_users.tenant_id IS '租户ID，标识该用户属于哪一家地接公司。';
COMMENT ON COLUMN system_users.username IS '登录账号，同一租户内未删除账号必须唯一。';
COMMENT ON COLUMN system_users.password_hash IS '登录密码哈希，使用安全哈希算法保存，不能保存明文密码。';
COMMENT ON COLUMN system_users.real_name IS '用户姓名，用于页面显示、操作日志和业务单据留痕。';
COMMENT ON COLUMN system_users.mobile_phone IS '用户手机号，可用于联系、登录验证或后续消息通知。';
COMMENT ON COLUMN system_users.email IS '用户邮箱，可用于通知或后续找回密码。';
COMMENT ON COLUMN system_users.role_id IS '企业角色ID，用于关联账号当前归属的业务角色。';
COMMENT ON COLUMN system_users.employee_id IS '企业员工ID，用于关联账号对应的员工资料。';
COMMENT ON COLUMN system_users.role_code IS '角色编码。用于登录令牌和粗粒度权限识别，具体角色资料由企业角色表维护。';
COMMENT ON COLUMN system_users.is_tenant_admin IS '是否租户管理员。true表示该账号可维护本租户基础设置。';
COMMENT ON COLUMN system_users.status IS '账号状态。active表示启用，disabled表示停用，locked表示锁定。';
COMMENT ON COLUMN system_users.last_login_at IS '最后登录时间。';
COMMENT ON COLUMN system_users.remark IS '备注，用于记录账号管理说明。';
COMMENT ON COLUMN system_users.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN system_users.created_at IS '创建时间。';
COMMENT ON COLUMN system_users.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN system_users.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN system_users.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN system_users.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_system_users_tenant_username_active IS '用户登录账号唯一索引，仅约束未删除记录。';

INSERT INTO system_users (
  tenant_id,
  username,
  password_hash,
  real_name,
  role_code,
  is_tenant_admin,
  status,
  created_by,
  remark
)
SELECT
  id,
  'admin',
  '$2b$10$0O6/.IdsVPRJbZEMEIO6b.HIRCgDKpcfT20086sbs4oeJ1Tz5EJpq',
  '系统管理员',
  'admin',
  true,
  'active',
  'system',
  '系统初始化默认管理员账号'
FROM tenants
WHERE tenant_code = 'default'
ON CONFLICT (tenant_id, username) WHERE is_deleted = false DO NOTHING;

COMMIT;

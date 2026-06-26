-- 旅游接待管理系统：系统配置表
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 仅用于让本脚本可在独立临时库中校验。正式库中 tenants 已由客户管理脚本创建。
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY,
  tenant_code varchar(50)
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

DROP TRIGGER IF EXISTS trg_system_configs_updated_at ON system_configs;
CREATE TRIGGER trg_system_configs_updated_at
BEFORE UPDATE ON system_configs
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_system_configs_tenant_key
  ON system_configs (tenant_id, config_key);

COMMENT ON TABLE system_configs IS '系统配置表。用于保存每个租户可独立设置的登录安全、业务风控、AI辅助录入和地图服务等运行参数。';
COMMENT ON COLUMN system_configs.id IS '系统配置主键ID，系统内部使用。';
COMMENT ON COLUMN system_configs.tenant_id IS '租户ID，标识该配置属于哪一家地接公司。';
COMMENT ON COLUMN system_configs.config_key IS '配置键，使用稳定英文编码标识具体配置项。';
COMMENT ON COLUMN system_configs.config_value IS '配置值，按配置键约定保存数字、文本或布尔值。';
COMMENT ON COLUMN system_configs.remark IS '配置说明，用于解释该配置项的业务含义和单位。';
COMMENT ON COLUMN system_configs.created_at IS '创建时间。';
COMMENT ON COLUMN system_configs.updated_at IS '更新时间，由触发器自动维护。';

COMMENT ON INDEX uk_system_configs_tenant_key IS '租户配置唯一约束，同一租户下同一个配置键只能有一条记录。';
COMMENT ON INDEX idx_system_configs_tenant_key IS '按租户和配置键查询系统配置。';

INSERT INTO system_configs (tenant_id, config_key, config_value, remark)
SELECT id, 'login_idle_timeout_minutes', '120', '浏览器无操作自动退出时间，单位分钟'
FROM tenants
WHERE tenant_code = 'default'
ON CONFLICT (tenant_id, config_key) DO NOTHING;

INSERT INTO system_configs (tenant_id, config_key, config_value, remark)
SELECT id, 'customer_risk_approval_enabled', 'false', '客户合同到期或授信超限时是否强制总经理审批'
FROM tenants
WHERE tenant_code = 'default'
ON CONFLICT (tenant_id, config_key) DO NOTHING;

COMMIT;

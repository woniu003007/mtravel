-- 旅游接待管理系统：系统操作日志表
-- PostgreSQL

BEGIN;

CREATE TABLE IF NOT EXISTS system_operation_logs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  operator_id bigint,
  operator_name varchar(80),
  module_name varchar(80) NOT NULL,
  operation_type varchar(40) NOT NULL,
  request_path varchar(300) NOT NULL,
  request_method varchar(10) NOT NULL,
  request_params text,
  ip_address varchar(80),
  user_agent varchar(500),
  success boolean NOT NULL DEFAULT true,
  duration_ms bigint NOT NULL DEFAULT 0,
  error_message varchar(500),
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT chk_system_operation_logs_request_method CHECK (request_method IN ('GET', 'POST')),
  CONSTRAINT chk_system_operation_logs_operation_type CHECK (
    operation_type IN ('查询', '新增', '修改', '删除', '启用', '停用', '审核', '导入', '导出', '登录', '退出', '其他')
  )
);

CREATE INDEX IF NOT EXISTS idx_system_operation_logs_tenant_created
  ON system_operation_logs (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_system_operation_logs_tenant_module_created
  ON system_operation_logs (tenant_id, module_name, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_system_operation_logs_tenant_operator_created
  ON system_operation_logs (tenant_id, operator_name, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_system_operation_logs_tenant_success_created
  ON system_operation_logs (tenant_id, success, created_at DESC);

COMMENT ON TABLE system_operation_logs IS '系统操作日志表。用于记录用户访问接口、维护资料、审核、导入导出和异常访问等操作，支撑审计、问题追踪和安全排查。';
COMMENT ON COLUMN system_operation_logs.id IS '操作日志主键ID，系统内部使用。';
COMMENT ON COLUMN system_operation_logs.tenant_id IS '租户ID，标识该操作日志属于哪一家地接公司。';
COMMENT ON COLUMN system_operation_logs.operator_id IS '操作人用户ID。未登录或无法识别时为空。';
COMMENT ON COLUMN system_operation_logs.operator_name IS '操作人账号或名称。未登录时可记录为anonymous或system。';
COMMENT ON COLUMN system_operation_logs.module_name IS '模块名称，例如客户管理、销售管理、财务管理、系统设置。';
COMMENT ON COLUMN system_operation_logs.operation_type IS '操作类型，例如查询、新增、修改、删除、启用、停用、审核、导入、导出、登录、退出。';
COMMENT ON COLUMN system_operation_logs.request_path IS '请求路径，不包含域名。';
COMMENT ON COLUMN system_operation_logs.request_method IS '请求方法。系统统一使用GET和POST。';
COMMENT ON COLUMN system_operation_logs.request_params IS '请求参数摘要。密码、Token等敏感字段必须脱敏。';
COMMENT ON COLUMN system_operation_logs.ip_address IS '客户端IP地址。';
COMMENT ON COLUMN system_operation_logs.user_agent IS '浏览器或客户端User-Agent摘要。';
COMMENT ON COLUMN system_operation_logs.success IS '操作是否成功。true表示成功，false表示失败。';
COMMENT ON COLUMN system_operation_logs.duration_ms IS '接口处理耗时，单位毫秒。';
COMMENT ON COLUMN system_operation_logs.error_message IS '失败原因摘要。成功时为空。';
COMMENT ON COLUMN system_operation_logs.created_at IS '操作发生时间。';

COMMIT;

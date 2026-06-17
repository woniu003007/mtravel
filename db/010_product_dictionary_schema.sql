-- 旅游接待管理系统：企业资料产品字典表
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

CREATE TABLE IF NOT EXISTS product_dictionaries (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  dict_type varchar(40) NOT NULL,
  dict_name varchar(120) NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_product_dictionaries_type CHECK (dict_type IN ('business_type', 'reception_standard', 'product_theme')),
  CONSTRAINT chk_product_dictionaries_sort CHECK (sort_order >= 0),
  CONSTRAINT chk_product_dictionaries_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_product_dictionaries_tenant_id_id UNIQUE (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_product_dictionaries_updated_at ON product_dictionaries;
CREATE TRIGGER trg_product_dictionaries_updated_at
BEFORE UPDATE ON product_dictionaries
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_product_dictionaries_tenant_deleted_type
  ON product_dictionaries (tenant_id, is_deleted, dict_type, sort_order);

CREATE INDEX IF NOT EXISTS idx_product_dictionaries_tenant_deleted_status
  ON product_dictionaries (tenant_id, is_deleted, status);

CREATE INDEX IF NOT EXISTS idx_product_dictionaries_tenant_deleted_name
  ON product_dictionaries (tenant_id, is_deleted, dict_name);

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_dictionaries_tenant_type_name_active
  ON product_dictionaries (tenant_id, dict_type, dict_name)
  WHERE is_deleted = false;

COMMENT ON TABLE product_dictionaries IS '产品字典表。用于维护销售产品模板可选择的业务类型、接待标准和产品主题。';
COMMENT ON COLUMN product_dictionaries.id IS '产品字典主键ID，系统内部使用。';
COMMENT ON COLUMN product_dictionaries.tenant_id IS '租户ID，标识该产品字典属于哪一家地接公司。';
COMMENT ON COLUMN product_dictionaries.dict_type IS '字典类型。business_type表示业务类型，reception_standard表示接待标准，product_theme表示产品主题。';
COMMENT ON COLUMN product_dictionaries.dict_name IS '字典名称，用于产品模板页面展示和保存。';
COMMENT ON COLUMN product_dictionaries.sort_order IS '排序值。数字越小越靠前。';
COMMENT ON COLUMN product_dictionaries.status IS '产品字典状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN product_dictionaries.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN product_dictionaries.remark IS '备注，用于记录产品字典管理说明。';
COMMENT ON COLUMN product_dictionaries.created_at IS '创建时间。';
COMMENT ON COLUMN product_dictionaries.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN product_dictionaries.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN product_dictionaries.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN product_dictionaries.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_product_dictionaries_tenant_type_name_active IS '产品字典名称唯一索引，仅约束同一租户、同一字典类型下未删除记录。';

INSERT INTO product_dictionaries (
  tenant_id,
  dict_type,
  dict_name,
  sort_order,
  status,
  created_by,
  remark
)
SELECT seed.tenant_id, seed.dict_type, seed.dict_name, seed.sort_order, 'active', 'system', seed.remark
FROM (
  SELECT id AS tenant_id, 'business_type' AS dict_type, '疗休养' AS dict_name, 10 AS sort_order, '初始化默认业务类型' AS remark FROM tenants
  UNION ALL SELECT id, 'business_type', '定制团', 20, '初始化默认业务类型' FROM tenants
  UNION ALL SELECT id, 'business_type', '红色培训', 30, '初始化默认业务类型' FROM tenants
  UNION ALL SELECT id, 'business_type', '门票冲量', 40, '初始化默认业务类型' FROM tenants
  UNION ALL SELECT id, 'business_type', '特惠游', 50, '初始化默认业务类型' FROM tenants
  UNION ALL SELECT id, 'business_type', '漂流', 60, '初始化默认业务类型' FROM tenants
  UNION ALL SELECT id, 'business_type', '落地散', 70, '初始化默认业务类型' FROM tenants
  UNION ALL SELECT id, 'business_type', '地接团', 80, '初始化默认业务类型' FROM tenants
  UNION ALL SELECT id, 'business_type', '周边游', 90, '初始化默认业务类型' FROM tenants
  UNION ALL SELECT id, 'business_type', '办公费用', 100, '初始化默认业务类型' FROM tenants
  UNION ALL SELECT id, 'business_type', '杂项经费', 110, '初始化默认业务类型' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '商务/快捷', 10, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '携程两钻', 20, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '准三星', 30, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '挂牌三星', 40, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '携程三钻', 50, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '挂牌四星', 60, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '准四星', 70, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '当地四星', 80, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '国际四星', 90, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '携程四钻', 100, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '准五星', 110, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '挂牌五星', 120, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '当地五星', 130, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '国际五星', 140, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '携程五钻', 150, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '超五星级', 160, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'reception_standard', '其它等级', 170, '初始化默认接待标准' FROM tenants
  UNION ALL SELECT id, 'product_theme', '观光', 10, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '美食', 20, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '夕阳红', 30, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '亲子游', 40, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '摄影', 50, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '滑雪', 60, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '夏令营', 70, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '春节', 80, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '五一', 90, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '中秋', 100, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '国庆', 110, '初始化默认产品主题' FROM tenants
  UNION ALL SELECT id, 'product_theme', '元旦', 120, '初始化默认产品主题' FROM tenants
) seed
ON CONFLICT (tenant_id, dict_type, dict_name) WHERE is_deleted = false DO NOTHING;

COMMIT;

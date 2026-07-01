-- 旅游接待管理系统：客户扩展、采购管理与公共附件表
-- PostgreSQL

BEGIN;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS common_attachments (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  business_module varchar(80) NOT NULL,
  business_type varchar(80) NOT NULL,
  business_id bigint,
  original_filename varchar(255) NOT NULL,
  stored_filename varchar(255) NOT NULL,
  storage_path text NOT NULL,
  file_url text NOT NULL,
  content_type varchar(120),
  file_size bigint NOT NULL DEFAULT 0,
  file_ext varchar(30),
  status varchar(20) NOT NULL DEFAULT 'active',
  uploaded_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  created_by varchar(80),
  CONSTRAINT chk_common_attachments_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT chk_common_attachments_file_size CHECK (file_size >= 0),
  CONSTRAINT uk_common_attachments_tenant_id_id UNIQUE (tenant_id, id)
);

ALTER TABLE common_attachments
  DROP CONSTRAINT IF EXISTS uk_common_attachments_tenant_id_id;

ALTER TABLE common_attachments
  ADD CONSTRAINT uk_common_attachments_tenant_id_id UNIQUE (tenant_id, id);

CREATE TABLE IF NOT EXISTS customer_credit_accounts (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  customer_id bigint NOT NULL,
  credit_limit numeric(14,2) NOT NULL DEFAULT 0,
  occupied_amount numeric(14,2) NOT NULL DEFAULT 0,
  pending_approval_amount numeric(14,2) NOT NULL DEFAULT 0,
  available_amount numeric(14,2) GENERATED ALWAYS AS (credit_limit - occupied_amount - pending_approval_amount) STORED,
  warning_threshold numeric(14,2) NOT NULL DEFAULT 0,
  over_limit_action varchar(20) NOT NULL DEFAULT 'remind',
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_customer_credit_accounts_amounts CHECK (
    credit_limit >= 0 AND occupied_amount >= 0 AND pending_approval_amount >= 0 AND warning_threshold >= 0
  ),
  CONSTRAINT chk_customer_credit_accounts_over_limit_action CHECK (over_limit_action IN ('none', 'remind', 'approval')),
  CONSTRAINT chk_customer_credit_accounts_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT fk_customer_credit_accounts_customer FOREIGN KEY (tenant_id, customer_id)
    REFERENCES customers (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS customer_product_authorizations (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  customer_id bigint NOT NULL,
  product_code varchar(80),
  product_name varchar(200) NOT NULL,
  authorized_start_date date,
  authorized_end_date date,
  authorization_status varchar(20) NOT NULL DEFAULT 'active',
  sale_scope text,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_customer_product_authorizations_status CHECK (authorization_status IN ('active', 'suspended', 'expired')),
  CONSTRAINT fk_customer_product_authorizations_customer FOREIGN KEY (tenant_id, customer_id)
    REFERENCES customers (tenant_id, id)
);

ALTER TABLE customer_contracts
  ADD COLUMN IF NOT EXISTS legal_subject varchar(200),
  ADD COLUMN IF NOT EXISTS invoice_subject varchar(200),
  ADD COLUMN IF NOT EXISTS settlement_subject varchar(200),
  ADD COLUMN IF NOT EXISTS template_name varchar(120),
  ADD COLUMN IF NOT EXISTS reminder_days integer NOT NULL DEFAULT 30,
  ADD COLUMN IF NOT EXISTS attachment_id bigint,
  ADD COLUMN IF NOT EXISTS party_a_name varchar(200),
  ADD COLUMN IF NOT EXISTS party_a_phone varchar(40),
  ADD COLUMN IF NOT EXISTS party_a_fax varchar(40),
  ADD COLUMN IF NOT EXISTS party_a_address varchar(300),
  ADD COLUMN IF NOT EXISTS party_a_contact varchar(80),
  ADD COLUMN IF NOT EXISTS party_b_name varchar(200),
  ADD COLUMN IF NOT EXISTS party_b_phone varchar(40),
  ADD COLUMN IF NOT EXISTS party_b_fax varchar(40),
  ADD COLUMN IF NOT EXISTS party_b_address varchar(300),
  ADD COLUMN IF NOT EXISTS party_b_contact varchar(80),
  ADD COLUMN IF NOT EXISTS agreement_content text,
  ADD COLUMN IF NOT EXISTS other_content text;

ALTER TABLE customer_contracts
  DROP CONSTRAINT IF EXISTS chk_customer_contracts_reminder_days;

ALTER TABLE customer_contracts
  ADD CONSTRAINT chk_customer_contracts_reminder_days CHECK (reminder_days >= 0);

CREATE TABLE IF NOT EXISTS suppliers (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  supplier_code varchar(80),
  supplier_name varchar(200) NOT NULL,
  supplier_category varchar(40) NOT NULL,
  province varchar(80),
  city varchar(80),
  district varchar(80),
  settlement_method varchar(100),
  contact_name varchar(80),
  contact_phone varchar(40),
  agreement_name varchar(160),
  rating integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  buyer_id bigint,
  fax_number varchar(40),
  office_address varchar(240),
  CONSTRAINT chk_suppliers_category CHECK (
    supplier_category IN ('hotel', 'scenic', 'vehicle', 'restaurant', 'traffic', 'ground_agent', 'shopping', 'other', 'common')
  ),
  CONSTRAINT chk_suppliers_rating CHECK (rating >= 0 AND rating <= 5),
  CONSTRAINT chk_suppliers_status CHECK (status IN ('active', 'disabled', 'blacklisted')),
  CONSTRAINT uk_suppliers_tenant_id_id UNIQUE (tenant_id, id)
);

ALTER TABLE suppliers
  ADD COLUMN IF NOT EXISTS buyer_id bigint,
  ADD COLUMN IF NOT EXISTS fax_number varchar(40),
  ADD COLUMN IF NOT EXISTS office_address varchar(240);

ALTER TABLE suppliers
  DROP CONSTRAINT IF EXISTS fk_suppliers_buyer;

ALTER TABLE suppliers
  ADD CONSTRAINT fk_suppliers_buyer FOREIGN KEY (tenant_id, buyer_id)
    REFERENCES customers (tenant_id, id);

CREATE TABLE IF NOT EXISTS purchase_resources (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_type varchar(40) NOT NULL,
  resource_name varchar(200) NOT NULL,
  province varchar(80),
  city varchar(80),
  district varchar(80),
  phone varchar(40),
  fax varchar(40),
  address varchar(300),
  warm_tip text,
  introduction text,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_purchase_resources_type CHECK (resource_type IN ('scenic', 'hotel', 'restaurant', 'shopping')),
  CONSTRAINT chk_purchase_resources_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_purchase_resources_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS resource_projects (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_type varchar(40) NOT NULL,
  project_name varchar(120) NOT NULL,
  statistics_enabled boolean NOT NULL DEFAULT true,
  sort_order integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_resource_projects_type CHECK (
    resource_type IN ('hotel', 'scenic', 'vehicle', 'restaurant', 'traffic', 'ground_agent', 'guide', 'finance_fee', 'current_refund', 'extra_fee', 'shopping', 'ticket', 'misc', 'other')
  ),
  CONSTRAINT chk_resource_projects_sort CHECK (sort_order >= 0),
  CONSTRAINT chk_resource_projects_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_resource_projects_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS purchase_relations (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  resource_type varchar(40) NOT NULL,
  resource_id bigint,
  resource_name varchar(200) NOT NULL,
  supplier_id bigint NOT NULL,
  purchase_price numeric(14,2) NOT NULL DEFAULT 0,
  price_unit varchar(40),
  settlement_method varchar(100),
  valid_from date,
  valid_to date,
  priority_level integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_purchase_relations_resource_type CHECK (
    resource_type IN ('hotel', 'scenic', 'vehicle', 'restaurant', 'guide', 'ground_agent', 'ticket', 'shopping', 'other')
  ),
  CONSTRAINT chk_purchase_relations_price CHECK (purchase_price >= 0),
  CONSTRAINT chk_purchase_relations_status CHECK (status IN ('active', 'disabled', 'expired')),
  CONSTRAINT uk_purchase_relations_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_purchase_relations_supplier FOREIGN KEY (tenant_id, supplier_id)
    REFERENCES suppliers (tenant_id, id)
);

ALTER TABLE purchase_relations
  ADD COLUMN IF NOT EXISTS group_quantity integer NOT NULL DEFAULT 0;

ALTER TABLE purchase_relations
  DROP CONSTRAINT IF EXISTS chk_purchase_relations_group_quantity;

ALTER TABLE purchase_relations
  ADD CONSTRAINT chk_purchase_relations_group_quantity CHECK (group_quantity >= 0);

ALTER TABLE purchase_relations
  DROP CONSTRAINT IF EXISTS uk_purchase_relations_tenant_id_id;

ALTER TABLE purchase_relations
  ADD CONSTRAINT uk_purchase_relations_tenant_id_id UNIQUE (tenant_id, id);

CREATE TABLE IF NOT EXISTS supplier_resource_prices (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  relation_id bigint NOT NULL,
  resource_project_id bigint NOT NULL,
  project_name varchar(120) NOT NULL,
  market_price numeric(14,2) NOT NULL DEFAULT 0,
  peer_price numeric(14,2) NOT NULL DEFAULT 0,
  team_price numeric(14,2) NOT NULL DEFAULT 0,
  price_description text,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_supplier_resource_prices_amount CHECK (
    market_price >= 0 AND peer_price >= 0 AND team_price >= 0
  ),
  CONSTRAINT chk_supplier_resource_prices_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT fk_supplier_resource_prices_relation FOREIGN KEY (tenant_id, relation_id)
    REFERENCES purchase_relations (tenant_id, id),
  CONSTRAINT fk_supplier_resource_prices_project FOREIGN KEY (tenant_id, resource_project_id)
    REFERENCES resource_projects (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS purchase_relation_ticket_templates (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  relation_id bigint NOT NULL,
  template_name varchar(200) NOT NULL,
  attachment_id bigint NOT NULL,
  template_file_url text,
  original_filename varchar(255),
  sheet_name varchar(120),
  header_row integer NOT NULL DEFAULT 1,
  data_start_row integer NOT NULL DEFAULT 2,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_purchase_relation_ticket_templates_rows CHECK (
    header_row >= 1 AND data_start_row > header_row
  ),
  CONSTRAINT chk_purchase_relation_ticket_templates_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_purchase_relation_ticket_templates_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_purchase_relation_ticket_templates_relation FOREIGN KEY (tenant_id, relation_id)
    REFERENCES purchase_relations (tenant_id, id),
  CONSTRAINT fk_purchase_relation_ticket_templates_attachment FOREIGN KEY (tenant_id, attachment_id)
    REFERENCES common_attachments (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS purchase_relation_ticket_template_fields (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  template_id bigint NOT NULL,
  template_header varchar(200) NOT NULL,
  column_index integer NOT NULL,
  system_field varchar(50),
  required boolean NOT NULL DEFAULT false,
  sort_order integer NOT NULL DEFAULT 0,
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_purchase_relation_ticket_template_fields_column CHECK (column_index >= 1),
  CONSTRAINT chk_purchase_relation_ticket_template_fields_sort CHECK (sort_order >= 0),
  CONSTRAINT chk_purchase_relation_ticket_template_fields_system_field CHECK (
    system_field IS NULL OR system_field IN ('tourist_name', 'certificate_type', 'certificate_no', 'mobile', 'gender', 'birthday', 'remark')
  ),
  CONSTRAINT fk_purchase_relation_ticket_template_fields_template FOREIGN KEY (tenant_id, template_id)
    REFERENCES purchase_relation_ticket_templates (tenant_id, id)
);

ALTER TABLE purchase_relation_ticket_template_fields
  ADD COLUMN IF NOT EXISTS fill_mode varchar(30) NOT NULL DEFAULT 'tourist_field',
  ADD COLUMN IF NOT EXISTS fixed_value varchar(300);

ALTER TABLE purchase_relation_ticket_template_fields
  ALTER COLUMN system_field DROP NOT NULL;

ALTER TABLE purchase_relation_ticket_template_fields
  DROP CONSTRAINT IF EXISTS chk_purchase_relation_ticket_template_fields_system_field,
  DROP CONSTRAINT IF EXISTS chk_purchase_relation_ticket_template_fields_fill_mode;

ALTER TABLE purchase_relation_ticket_template_fields
  ADD CONSTRAINT chk_purchase_relation_ticket_template_fields_system_field CHECK (
    system_field IS NULL OR system_field IN ('tourist_name', 'certificate_type', 'certificate_no', 'mobile', 'gender', 'birthday', 'remark')
  ),
  ADD CONSTRAINT chk_purchase_relation_ticket_template_fields_fill_mode CHECK (
    fill_mode IN ('tourist_field', 'sequence', 'constant', 'keep_original')
  );

CREATE TABLE IF NOT EXISTS supplier_contracts (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  supplier_id bigint,
  contract_no varchar(80) NOT NULL,
  contract_name varchar(200) NOT NULL,
  contract_category varchar(40) NOT NULL,
  start_date date,
  end_date date,
  settlement_terms text,
  purchase_price_summary text,
  attachment_id bigint,
  contract_file_url text,
  reminder_days integer NOT NULL DEFAULT 30,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_supplier_contracts_category CHECK (
    contract_category IN (
      'hotel',
      'scenic',
      'vehicle',
      'restaurant',
      'guide',
      'ground_agent',
      'ticket',
      'traffic',
      'finance_fee',
      'current_refund',
      'extra_fee',
      'shopping',
      'other'
    )
  ),
  CONSTRAINT chk_supplier_contracts_reminder_days CHECK (reminder_days >= 0),
  CONSTRAINT chk_supplier_contracts_status CHECK (status IN ('active', 'disabled', 'terminated')),
  CONSTRAINT fk_supplier_contracts_supplier FOREIGN KEY (tenant_id, supplier_id)
    REFERENCES suppliers (tenant_id, id)
);

ALTER TABLE supplier_contracts
  DROP CONSTRAINT IF EXISTS chk_supplier_contracts_category;

ALTER TABLE supplier_contracts
  ADD COLUMN IF NOT EXISTS party_a_name varchar(200),
  ADD COLUMN IF NOT EXISTS party_a_phone varchar(40),
  ADD COLUMN IF NOT EXISTS party_a_fax varchar(40),
  ADD COLUMN IF NOT EXISTS party_a_address varchar(300),
  ADD COLUMN IF NOT EXISTS party_a_contact varchar(80),
  ADD COLUMN IF NOT EXISTS party_b_name varchar(200),
  ADD COLUMN IF NOT EXISTS party_b_phone varchar(40),
  ADD COLUMN IF NOT EXISTS party_b_fax varchar(40),
  ADD COLUMN IF NOT EXISTS party_b_address varchar(300),
  ADD COLUMN IF NOT EXISTS party_b_contact varchar(80),
  ADD COLUMN IF NOT EXISTS agreement_content text,
  ADD COLUMN IF NOT EXISTS other_content text;

ALTER TABLE supplier_contracts
  ADD CONSTRAINT chk_supplier_contracts_category CHECK (
    contract_category IN (
      'hotel',
      'scenic',
      'vehicle',
      'restaurant',
      'guide',
      'ground_agent',
      'ticket',
      'traffic',
      'finance_fee',
      'current_refund',
      'extra_fee',
      'shopping',
      'other'
    )
  );

CREATE TABLE IF NOT EXISTS hotel_resources (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  hotel_name varchar(200) NOT NULL,
  city varchar(80),
  area varchar(120),
  address varchar(300),
  star_standard varchar(80),
  room_type varchar(120) NOT NULL,
  supplier_id bigint,
  purchase_price numeric(14,2) NOT NULL DEFAULT 0,
  agreement_price numeric(14,2) NOT NULL DEFAULT 0,
  price_unit varchar(40) NOT NULL DEFAULT '间夜',
  valid_from date,
  valid_to date,
  contact_name varchar(80),
  contact_phone varchar(40),
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_hotel_resources_price CHECK (purchase_price >= 0 AND agreement_price >= 0),
  CONSTRAINT chk_hotel_resources_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_hotel_resources_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_hotel_resources_supplier FOREIGN KEY (tenant_id, supplier_id)
    REFERENCES suppliers (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS scenic_resources (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  scenic_name varchar(200) NOT NULL,
  city varchar(80),
  area varchar(120),
  address varchar(300),
  ticket_type varchar(120) NOT NULL,
  supplier_id bigint,
  purchase_price numeric(14,2) NOT NULL DEFAULT 0,
  agreement_price numeric(14,2) NOT NULL DEFAULT 0,
  price_unit varchar(40) NOT NULL DEFAULT '人',
  valid_from date,
  valid_to date,
  free_ticket_rule text,
  half_ticket_rule text,
  contact_name varchar(80),
  contact_phone varchar(40),
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_scenic_resources_price CHECK (purchase_price >= 0 AND agreement_price >= 0),
  CONSTRAINT chk_scenic_resources_status CHECK (status IN ('active', 'disabled')),
  CONSTRAINT uk_scenic_resources_tenant_id_id UNIQUE (tenant_id, id),
  CONSTRAINT fk_scenic_resources_supplier FOREIGN KEY (tenant_id, supplier_id)
    REFERENCES suppliers (tenant_id, id)
);

CREATE TABLE IF NOT EXISTS ground_agents (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  ground_agent_name varchar(200) NOT NULL,
  city varchar(80),
  contact_name varchar(80),
  contact_phone varchar(40),
  task_name varchar(200),
  itinerary_requirement text,
  total_budget numeric(14,2) NOT NULL DEFAULT 0,
  confirmation_attachment_id bigint,
  confirmation_file_url text,
  status varchar(20) NOT NULL DEFAULT 'active',
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_ground_agents_budget CHECK (total_budget >= 0),
  CONSTRAINT chk_ground_agents_status CHECK (status IN ('active', 'disabled', 'completed'))
);

DROP TRIGGER IF EXISTS trg_common_attachments_updated_at ON common_attachments;
CREATE TRIGGER trg_common_attachments_updated_at
BEFORE UPDATE ON common_attachments
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_customer_credit_accounts_updated_at ON customer_credit_accounts;
CREATE TRIGGER trg_customer_credit_accounts_updated_at
BEFORE UPDATE ON customer_credit_accounts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_customer_product_authorizations_updated_at ON customer_product_authorizations;
CREATE TRIGGER trg_customer_product_authorizations_updated_at
BEFORE UPDATE ON customer_product_authorizations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_suppliers_updated_at ON suppliers;
CREATE TRIGGER trg_suppliers_updated_at
BEFORE UPDATE ON suppliers
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_purchase_resources_updated_at ON purchase_resources;
CREATE TRIGGER trg_purchase_resources_updated_at
BEFORE UPDATE ON purchase_resources
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_resource_projects_updated_at ON resource_projects;
CREATE TRIGGER trg_resource_projects_updated_at
BEFORE UPDATE ON resource_projects
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_purchase_relations_updated_at ON purchase_relations;
CREATE TRIGGER trg_purchase_relations_updated_at
BEFORE UPDATE ON purchase_relations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_supplier_resource_prices_updated_at ON supplier_resource_prices;
CREATE TRIGGER trg_supplier_resource_prices_updated_at
BEFORE UPDATE ON supplier_resource_prices
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_purchase_relation_ticket_templates_updated_at ON purchase_relation_ticket_templates;
CREATE TRIGGER trg_purchase_relation_ticket_templates_updated_at
BEFORE UPDATE ON purchase_relation_ticket_templates
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_purchase_relation_ticket_template_fields_updated_at ON purchase_relation_ticket_template_fields;
CREATE TRIGGER trg_purchase_relation_ticket_template_fields_updated_at
BEFORE UPDATE ON purchase_relation_ticket_template_fields
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_supplier_contracts_updated_at ON supplier_contracts;
CREATE TRIGGER trg_supplier_contracts_updated_at
BEFORE UPDATE ON supplier_contracts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_hotel_resources_updated_at ON hotel_resources;
CREATE TRIGGER trg_hotel_resources_updated_at
BEFORE UPDATE ON hotel_resources
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_scenic_resources_updated_at ON scenic_resources;
CREATE TRIGGER trg_scenic_resources_updated_at
BEFORE UPDATE ON scenic_resources
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_ground_agents_updated_at ON ground_agents;
CREATE TRIGGER trg_ground_agents_updated_at
BEFORE UPDATE ON ground_agents
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_common_attachments_tenant_deleted_business
  ON common_attachments (tenant_id, is_deleted, business_module, business_type, business_id);
CREATE INDEX IF NOT EXISTS idx_common_attachments_tenant_deleted_created
  ON common_attachments (tenant_id, is_deleted, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_customer_credit_accounts_tenant_deleted_customer
  ON customer_credit_accounts (tenant_id, is_deleted, customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_credit_accounts_tenant_deleted_status
  ON customer_credit_accounts (tenant_id, is_deleted, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_credit_accounts_tenant_customer_active
  ON customer_credit_accounts (tenant_id, customer_id)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_customer_product_auth_tenant_deleted_customer
  ON customer_product_authorizations (tenant_id, is_deleted, customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_product_auth_tenant_deleted_status
  ON customer_product_authorizations (tenant_id, is_deleted, authorization_status);
CREATE INDEX IF NOT EXISTS idx_customer_product_auth_tenant_deleted_product
  ON customer_product_authorizations (tenant_id, is_deleted, product_name);
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_product_auth_tenant_customer_product_active
  ON customer_product_authorizations (tenant_id, customer_id, product_name)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_suppliers_tenant_deleted_category
  ON suppliers (tenant_id, is_deleted, supplier_category);
CREATE INDEX IF NOT EXISTS idx_suppliers_tenant_deleted_status
  ON suppliers (tenant_id, is_deleted, status);
CREATE INDEX IF NOT EXISTS idx_suppliers_tenant_deleted_name
  ON suppliers (tenant_id, is_deleted, supplier_name);
CREATE INDEX IF NOT EXISTS idx_suppliers_tenant_deleted_buyer
  ON suppliers (tenant_id, is_deleted, buyer_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_suppliers_tenant_code_active
  ON suppliers (tenant_id, supplier_code)
  WHERE is_deleted = false AND supplier_code IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_suppliers_tenant_name_active
  ON suppliers (tenant_id, supplier_name)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_purchase_resources_tenant_deleted_type
  ON purchase_resources (tenant_id, is_deleted, resource_type);
CREATE INDEX IF NOT EXISTS idx_purchase_resources_tenant_deleted_city
  ON purchase_resources (tenant_id, is_deleted, province, city, district);
CREATE INDEX IF NOT EXISTS idx_purchase_resources_tenant_deleted_name
  ON purchase_resources (tenant_id, is_deleted, resource_name);
CREATE INDEX IF NOT EXISTS idx_purchase_resources_tenant_deleted_status
  ON purchase_resources (tenant_id, is_deleted, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_resources_tenant_type_name_city_active
  ON purchase_resources (tenant_id, resource_type, resource_name, province, city, district)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_resource_projects_tenant_deleted_type
  ON resource_projects (tenant_id, is_deleted, resource_type);
CREATE INDEX IF NOT EXISTS idx_resource_projects_tenant_deleted_status
  ON resource_projects (tenant_id, is_deleted, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_resource_projects_tenant_type_name_active
  ON resource_projects (tenant_id, resource_type, project_name)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_purchase_relations_tenant_deleted_resource
  ON purchase_relations (tenant_id, is_deleted, resource_type, resource_name);
CREATE INDEX IF NOT EXISTS idx_purchase_relations_tenant_deleted_supplier
  ON purchase_relations (tenant_id, is_deleted, supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_relations_tenant_deleted_status
  ON purchase_relations (tenant_id, is_deleted, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_relations_tenant_resource_supplier_group_active
  ON purchase_relations (tenant_id, resource_id, supplier_id, group_quantity)
  WHERE is_deleted = false AND resource_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_supplier_resource_prices_tenant_deleted_relation
  ON supplier_resource_prices (tenant_id, is_deleted, relation_id);
CREATE INDEX IF NOT EXISTS idx_supplier_resource_prices_tenant_deleted_project
  ON supplier_resource_prices (tenant_id, is_deleted, resource_project_id);
CREATE INDEX IF NOT EXISTS idx_supplier_resource_prices_tenant_deleted_status
  ON supplier_resource_prices (tenant_id, is_deleted, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_resource_prices_tenant_relation_project_active
  ON supplier_resource_prices (tenant_id, relation_id, resource_project_id)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_purchase_relation_ticket_templates_tenant_deleted_relation
  ON purchase_relation_ticket_templates (tenant_id, is_deleted, relation_id);
CREATE INDEX IF NOT EXISTS idx_purchase_relation_ticket_templates_tenant_deleted_status
  ON purchase_relation_ticket_templates (tenant_id, is_deleted, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_relation_ticket_templates_tenant_relation_active
  ON purchase_relation_ticket_templates (tenant_id, relation_id)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_purchase_relation_ticket_template_fields_tenant_deleted_template
  ON purchase_relation_ticket_template_fields (tenant_id, is_deleted, template_id);
CREATE INDEX IF NOT EXISTS idx_purchase_relation_ticket_template_fields_tenant_deleted_system
  ON purchase_relation_ticket_template_fields (tenant_id, is_deleted, system_field);
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_relation_ticket_template_fields_tenant_template_column_active
  ON purchase_relation_ticket_template_fields (tenant_id, template_id, column_index)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_supplier_contracts_tenant_deleted_supplier
  ON supplier_contracts (tenant_id, is_deleted, supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_contracts_tenant_deleted_end_date
  ON supplier_contracts (tenant_id, is_deleted, end_date);
CREATE INDEX IF NOT EXISTS idx_supplier_contracts_tenant_deleted_status
  ON supplier_contracts (tenant_id, is_deleted, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_contracts_tenant_contract_no_active
  ON supplier_contracts (tenant_id, contract_no)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_hotel_resources_tenant_deleted_name
  ON hotel_resources (tenant_id, is_deleted, hotel_name);
CREATE INDEX IF NOT EXISTS idx_hotel_resources_tenant_deleted_city
  ON hotel_resources (tenant_id, is_deleted, city, area);
CREATE INDEX IF NOT EXISTS idx_hotel_resources_tenant_deleted_supplier
  ON hotel_resources (tenant_id, is_deleted, supplier_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_hotel_resources_tenant_name_room_active
  ON hotel_resources (tenant_id, hotel_name, room_type)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_scenic_resources_tenant_deleted_name
  ON scenic_resources (tenant_id, is_deleted, scenic_name);
CREATE INDEX IF NOT EXISTS idx_scenic_resources_tenant_deleted_city
  ON scenic_resources (tenant_id, is_deleted, city, area);
CREATE INDEX IF NOT EXISTS idx_scenic_resources_tenant_deleted_supplier
  ON scenic_resources (tenant_id, is_deleted, supplier_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_scenic_resources_tenant_name_ticket_active
  ON scenic_resources (tenant_id, scenic_name, ticket_type)
  WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_ground_agents_tenant_deleted_name
  ON ground_agents (tenant_id, is_deleted, ground_agent_name);
CREATE INDEX IF NOT EXISTS idx_ground_agents_tenant_deleted_city
  ON ground_agents (tenant_id, is_deleted, city);
CREATE INDEX IF NOT EXISTS idx_ground_agents_tenant_deleted_status
  ON ground_agents (tenant_id, is_deleted, status);

COMMENT ON TABLE common_attachments IS '公共附件表。用于保存合同文件、确认单、票据、截图等业务附件的存储位置和归属信息。';
COMMENT ON COLUMN common_attachments.id IS '附件主键ID，系统内部使用。';
COMMENT ON COLUMN common_attachments.tenant_id IS '租户ID，标识附件属于哪一家地接公司。';
COMMENT ON COLUMN common_attachments.business_module IS '业务模块名称，例如客户管理、采购管理、财务管理。';
COMMENT ON COLUMN common_attachments.business_type IS '业务类型，例如客户合同、采购合同、地接确认单。';
COMMENT ON COLUMN common_attachments.business_id IS '业务记录ID。文件先上传后绑定时可为空。';
COMMENT ON COLUMN common_attachments.original_filename IS '用户上传时的原始文件名。';
COMMENT ON COLUMN common_attachments.stored_filename IS '系统保存到磁盘时使用的文件名。';
COMMENT ON COLUMN common_attachments.storage_path IS '服务器本地存储路径。';
COMMENT ON COLUMN common_attachments.file_url IS '前端访问或下载附件时使用的相对地址。';
COMMENT ON COLUMN common_attachments.content_type IS '文件MIME类型。';
COMMENT ON COLUMN common_attachments.file_size IS '文件大小，单位字节。';
COMMENT ON COLUMN common_attachments.file_ext IS '文件扩展名。';
COMMENT ON COLUMN common_attachments.status IS '附件状态。active表示可用，disabled表示停用。';
COMMENT ON COLUMN common_attachments.uploaded_by IS '上传人账号或名称。';
COMMENT ON COLUMN common_attachments.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN common_attachments.remark IS '附件备注。';
COMMENT ON COLUMN common_attachments.created_at IS '创建时间。';
COMMENT ON COLUMN common_attachments.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN common_attachments.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN common_attachments.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN common_attachments.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE customer_credit_accounts IS '客户授信账户表。用于维护客户授信额度、已占用额度、审批中额度和可用额度，支撑排团或下单时的额度提醒。';
COMMENT ON COLUMN customer_credit_accounts.id IS '客户授信账户主键ID，系统内部使用。';
COMMENT ON COLUMN customer_credit_accounts.tenant_id IS '租户ID，标识该授信账户属于哪一家地接公司。';
COMMENT ON COLUMN customer_credit_accounts.customer_id IS '客户单位ID，关联客户单位表。';
COMMENT ON COLUMN customer_credit_accounts.credit_limit IS '客户授信额度，单位元。';
COMMENT ON COLUMN customer_credit_accounts.occupied_amount IS '已占用额度，首版支持手工维护，后续由订单确认和费用变更自动占用。';
COMMENT ON COLUMN customer_credit_accounts.pending_approval_amount IS '审批中额度，用于记录超限审批或待确认占用金额。';
COMMENT ON COLUMN customer_credit_accounts.available_amount IS '可用额度，由授信额度减已占用额度和审批中额度自动计算。';
COMMENT ON COLUMN customer_credit_accounts.warning_threshold IS '预警阈值，可用于低于指定可用额度时提醒。';
COMMENT ON COLUMN customer_credit_accounts.over_limit_action IS '超限处理方式。none表示不处理，remind表示提醒，approval表示转审批。';
COMMENT ON COLUMN customer_credit_accounts.status IS '授信账户状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN customer_credit_accounts.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN customer_credit_accounts.remark IS '授信备注，用于记录额度口径或审批说明。';
COMMENT ON COLUMN customer_credit_accounts.created_at IS '创建时间。';
COMMENT ON COLUMN customer_credit_accounts.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN customer_credit_accounts.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN customer_credit_accounts.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN customer_credit_accounts.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE customer_product_authorizations IS '客户产品授权表。用于维护客户可销售或可下单的产品范围、授权期限和授权状态。';
COMMENT ON COLUMN customer_product_authorizations.id IS '产品授权主键ID，系统内部使用。';
COMMENT ON COLUMN customer_product_authorizations.tenant_id IS '租户ID，标识该授权属于哪一家地接公司。';
COMMENT ON COLUMN customer_product_authorizations.customer_id IS '客户单位ID，关联客户单位表。';
COMMENT ON COLUMN customer_product_authorizations.product_code IS '产品编码，允许为空。';
COMMENT ON COLUMN customer_product_authorizations.product_name IS '授权产品名称。';
COMMENT ON COLUMN customer_product_authorizations.authorized_start_date IS '授权开始日期。';
COMMENT ON COLUMN customer_product_authorizations.authorized_end_date IS '授权结束日期。';
COMMENT ON COLUMN customer_product_authorizations.authorization_status IS '授权状态。active表示有效，suspended表示暂停，expired表示已到期。';
COMMENT ON COLUMN customer_product_authorizations.sale_scope IS '可售范围或授权说明。';
COMMENT ON COLUMN customer_product_authorizations.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN customer_product_authorizations.remark IS '授权备注。';
COMMENT ON COLUMN customer_product_authorizations.created_at IS '创建时间。';
COMMENT ON COLUMN customer_product_authorizations.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN customer_product_authorizations.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN customer_product_authorizations.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN customer_product_authorizations.deleted_by IS '删除人账号或名称。未删除时为空。';

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

COMMENT ON TABLE suppliers IS '供应商表。用于维护酒店、景区、车队、餐厅、地接社等采购供应商档案。';
COMMENT ON COLUMN suppliers.id IS '供应商主键ID，系统内部使用。';
COMMENT ON COLUMN suppliers.tenant_id IS '租户ID，标识该供应商属于哪一家地接公司。';
COMMENT ON COLUMN suppliers.supplier_code IS '供应商编码，同一租户内未删除记录可唯一。';
COMMENT ON COLUMN suppliers.supplier_name IS '供应商名称。';
COMMENT ON COLUMN suppliers.supplier_category IS '供应商分类。hotel酒店，scenic景区，vehicle车队，restaurant餐厅，traffic大交通，ground_agent地接，shopping购物，other其它，common通用。';
COMMENT ON COLUMN suppliers.buyer_id IS '关联采购商客户单位ID。用于供应商与客户单位之间的应收应付冲抵关系；为空表示不关联。';
COMMENT ON COLUMN suppliers.province IS '供应商所在地省份。';
COMMENT ON COLUMN suppliers.city IS '供应商所在地城市。';
COMMENT ON COLUMN suppliers.district IS '供应商所在地区县。';
COMMENT ON COLUMN suppliers.settlement_method IS '结算方式，例如现结、月结、周结。';
COMMENT ON COLUMN suppliers.contact_name IS '联系人姓名。';
COMMENT ON COLUMN suppliers.contact_phone IS '联系人电话。';
COMMENT ON COLUMN suppliers.fax_number IS '供应商传真号码。';
COMMENT ON COLUMN suppliers.office_address IS '供应商办公地址。';
COMMENT ON COLUMN suppliers.agreement_name IS '协议或合作文件名称。';
COMMENT ON COLUMN suppliers.rating IS '供应商评价等级，0到5分。';
COMMENT ON COLUMN suppliers.status IS '供应商状态。active表示合作中，disabled表示停用，blacklisted表示黑名单。';
COMMENT ON COLUMN suppliers.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN suppliers.remark IS '供应商备注。';
COMMENT ON COLUMN suppliers.created_at IS '创建时间。';
COMMENT ON COLUMN suppliers.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN suppliers.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN suppliers.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN suppliers.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE purchase_resources IS '采购资源总览表。用于维护景区、酒店、餐厅和购物等采购资源主档，支撑资源查询、供应商绑定和资源图片管理。';
COMMENT ON COLUMN purchase_resources.id IS '采购资源主键ID，系统内部使用。';
COMMENT ON COLUMN purchase_resources.tenant_id IS '租户ID，标识该资源属于哪一家地接公司。';
COMMENT ON COLUMN purchase_resources.resource_type IS '资源类型。scenic景区，hotel酒店，restaurant餐厅，shopping购物。';
COMMENT ON COLUMN purchase_resources.resource_name IS '资源名称。';
COMMENT ON COLUMN purchase_resources.province IS '资源所在省份。';
COMMENT ON COLUMN purchase_resources.city IS '资源所在城市。';
COMMENT ON COLUMN purchase_resources.district IS '资源所在区县。';
COMMENT ON COLUMN purchase_resources.phone IS '资源联系电话。';
COMMENT ON COLUMN purchase_resources.fax IS '资源传真号码。';
COMMENT ON COLUMN purchase_resources.address IS '资源详细地址。';
COMMENT ON COLUMN purchase_resources.warm_tip IS '资源温馨提示，用于记录接待、预约或注意事项。';
COMMENT ON COLUMN purchase_resources.introduction IS '资源简介。';
COMMENT ON COLUMN purchase_resources.status IS '资源状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN purchase_resources.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN purchase_resources.remark IS '资源备注。';
COMMENT ON COLUMN purchase_resources.created_at IS '创建时间。';
COMMENT ON COLUMN purchase_resources.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN purchase_resources.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN purchase_resources.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN purchase_resources.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE resource_projects IS '资源费用项目表。用于维护不同资源类型下可选择的费用或价格项目，支撑采购价格、计调成本和财务统计口径。';
COMMENT ON COLUMN resource_projects.id IS '资源费用项目主键ID，系统内部使用。';
COMMENT ON COLUMN resource_projects.tenant_id IS '租户ID，标识该费用项目属于哪一家地接公司。';
COMMENT ON COLUMN resource_projects.resource_type IS '资源类型。hotel酒店，scenic景区，vehicle车队，restaurant餐厅，traffic大交通，ground_agent地接，guide导游，finance_fee财务费用，current_refund现收现退，extra_fee附加费用，shopping购物，ticket票务，misc杂费，other其他。';
COMMENT ON COLUMN resource_projects.project_name IS '项目名称，例如成人、儿童、标间、标准餐。';
COMMENT ON COLUMN resource_projects.statistics_enabled IS '是否纳入统计。true表示参与业务统计，false表示仅用于录入选择。';
COMMENT ON COLUMN resource_projects.sort_order IS '排序号，数字越小越靠前。';
COMMENT ON COLUMN resource_projects.status IS '费用项目状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN resource_projects.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN resource_projects.remark IS '费用项目备注。';
COMMENT ON COLUMN resource_projects.created_at IS '创建时间。';
COMMENT ON COLUMN resource_projects.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN resource_projects.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN resource_projects.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN resource_projects.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE purchase_relations IS '采购关系表。用于维护资源与供应商之间的绑定关系和成团数量，不直接承载具体价格明细。';
COMMENT ON COLUMN purchase_relations.id IS '采购关系主键ID，系统内部使用。';
COMMENT ON COLUMN purchase_relations.tenant_id IS '租户ID，标识该采购关系属于哪一家地接公司。';
COMMENT ON COLUMN purchase_relations.resource_type IS '资源类型。hotel酒店，scenic景区，vehicle车辆，restaurant餐厅，guide导游，ground_agent地接，ticket票务，shopping购物，other其他。';
COMMENT ON COLUMN purchase_relations.resource_id IS '资源ID，关联采购资源主档。';
COMMENT ON COLUMN purchase_relations.resource_name IS '资源名称。';
COMMENT ON COLUMN purchase_relations.supplier_id IS '供应商ID，关联供应商表。';
COMMENT ON COLUMN purchase_relations.group_quantity IS '成团数量。0表示散团同价，大于0表示达到该数量后适用对应关系价格。';
COMMENT ON COLUMN purchase_relations.purchase_price IS '历史兼容采购价格字段，当前价格明细由 supplier_resource_prices 维护。';
COMMENT ON COLUMN purchase_relations.price_unit IS '历史兼容价格单位字段，当前价格明细由 supplier_resource_prices 维护。';
COMMENT ON COLUMN purchase_relations.settlement_method IS '历史兼容结算方式字段，当前结算条款优先从合同或价格说明维护。';
COMMENT ON COLUMN purchase_relations.valid_from IS '历史兼容价格有效期开始日期，当前价格明细不使用该字段。';
COMMENT ON COLUMN purchase_relations.valid_to IS '历史兼容价格有效期结束日期，当前价格明细不使用该字段。';
COMMENT ON COLUMN purchase_relations.priority_level IS '历史兼容优先级字段，当前页面不作为主操作字段。';
COMMENT ON COLUMN purchase_relations.status IS '采购关系状态。active表示有效，disabled表示停用，expired表示过期。';
COMMENT ON COLUMN purchase_relations.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN purchase_relations.remark IS '采购关系备注。';
COMMENT ON COLUMN purchase_relations.created_at IS '创建时间。';
COMMENT ON COLUMN purchase_relations.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN purchase_relations.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN purchase_relations.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN purchase_relations.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE supplier_resource_prices IS '供应商资源价格表。用于维护某个采购关系下不同费用项目的门市价、同行价、团队价和价格说明。';
COMMENT ON COLUMN supplier_resource_prices.id IS '供应商资源价格主键ID，系统内部使用。';
COMMENT ON COLUMN supplier_resource_prices.tenant_id IS '租户ID，标识该价格属于哪一家地接公司。';
COMMENT ON COLUMN supplier_resource_prices.relation_id IS '采购关系ID，标识该价格属于哪个资源和供应商绑定关系。';
COMMENT ON COLUMN supplier_resource_prices.resource_project_id IS '资源费用项目ID，标识成人、儿童、标间等项目类型。';
COMMENT ON COLUMN supplier_resource_prices.project_name IS '项目名称快照，用于价格记录展示和历史追溯。';
COMMENT ON COLUMN supplier_resource_prices.market_price IS '门市价，单位元。';
COMMENT ON COLUMN supplier_resource_prices.peer_price IS '同行价，单位元。';
COMMENT ON COLUMN supplier_resource_prices.team_price IS '团队价，单位元。';
COMMENT ON COLUMN supplier_resource_prices.price_description IS '价格说明，例如适用条件、淡旺季说明或特殊限制。';
COMMENT ON COLUMN supplier_resource_prices.status IS '价格状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN supplier_resource_prices.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN supplier_resource_prices.remark IS '价格备注。';
COMMENT ON COLUMN supplier_resource_prices.created_at IS '创建时间。';
COMMENT ON COLUMN supplier_resource_prices.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN supplier_resource_prices.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN supplier_resource_prices.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN supplier_resource_prices.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE purchase_relation_ticket_templates IS '采购关系游客名单模板表。用于维护某个景区资源和供应商渠道对应的游客名单Excel模板。';
COMMENT ON COLUMN purchase_relation_ticket_templates.id IS '游客名单模板主键ID，系统内部使用。';
COMMENT ON COLUMN purchase_relation_ticket_templates.tenant_id IS '租户ID，标识该模板属于哪一家地接公司。';
COMMENT ON COLUMN purchase_relation_ticket_templates.relation_id IS '采购关系ID，标识该模板属于哪个资源和供应商绑定关系。';
COMMENT ON COLUMN purchase_relation_ticket_templates.template_name IS '模板名称，便于业务人员识别不同票务渠道模板。';
COMMENT ON COLUMN purchase_relation_ticket_templates.attachment_id IS '模板附件ID，关联公共附件表。';
COMMENT ON COLUMN purchase_relation_ticket_templates.template_file_url IS '模板文件访问地址快照。';
COMMENT ON COLUMN purchase_relation_ticket_templates.original_filename IS '用户上传时的模板文件名快照。';
COMMENT ON COLUMN purchase_relation_ticket_templates.sheet_name IS 'Excel工作表名称。';
COMMENT ON COLUMN purchase_relation_ticket_templates.header_row IS '表头行号，按Excel行号从1开始。';
COMMENT ON COLUMN purchase_relation_ticket_templates.data_start_row IS '游客数据开始行号，按Excel行号从1开始，必须大于表头行。';
COMMENT ON COLUMN purchase_relation_ticket_templates.status IS '模板状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN purchase_relation_ticket_templates.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN purchase_relation_ticket_templates.remark IS '模板备注。';
COMMENT ON COLUMN purchase_relation_ticket_templates.created_at IS '创建时间。';
COMMENT ON COLUMN purchase_relation_ticket_templates.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN purchase_relation_ticket_templates.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN purchase_relation_ticket_templates.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN purchase_relation_ticket_templates.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE purchase_relation_ticket_template_fields IS '采购关系游客名单模板字段映射表。用于维护Excel模板列与系统游客字段的对应关系。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.id IS '字段映射主键ID，系统内部使用。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.tenant_id IS '租户ID，标识该字段映射属于哪一家地接公司。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.template_id IS '游客名单模板ID。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.template_header IS 'Excel模板表头名称。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.column_index IS 'Excel列序号，从1开始。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.system_field IS '系统游客字段编码。游客字段填充时使用，例如tourist_name、certificate_no。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.fill_mode IS '填充方式。tourist_field表示游客字段，sequence表示自动序号，constant表示固定值，keep_original表示保留模板原内容。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.fixed_value IS '固定值填充内容，仅填充方式为constant时使用。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.required IS '是否必填。true表示生成或校验游客名单时该字段不能为空。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.sort_order IS '排序号，数字越小越靠前。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.remark IS '字段映射备注。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.created_at IS '创建时间。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN purchase_relation_ticket_template_fields.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE supplier_contracts IS '供应商合同表。用于维护采购合同台账，记录合同期限、采购价格说明、结算条款、合同附件和到期提醒。';
COMMENT ON COLUMN supplier_contracts.id IS '供应商合同主键ID，系统内部使用。';
COMMENT ON COLUMN supplier_contracts.tenant_id IS '租户ID，标识该供应商合同属于哪一家地接公司。';
COMMENT ON COLUMN supplier_contracts.supplier_id IS '供应商ID，关联供应商表。';
COMMENT ON COLUMN supplier_contracts.contract_no IS '合同编号，同一租户内未删除记录必须唯一。';
COMMENT ON COLUMN supplier_contracts.contract_name IS '合同名称。';
COMMENT ON COLUMN supplier_contracts.contract_category IS '合同分类。hotel酒店，scenic景区，vehicle车辆，restaurant餐厅，guide导游，ground_agent地接，ticket票务，traffic大交通，finance_fee财务费用，current_refund现收现退，extra_fee附加费用，shopping购物，other其他。';
COMMENT ON COLUMN supplier_contracts.start_date IS '合同开始日期。';
COMMENT ON COLUMN supplier_contracts.end_date IS '合同结束日期或有效期止。';
COMMENT ON COLUMN supplier_contracts.settlement_terms IS '结算条款。';
COMMENT ON COLUMN supplier_contracts.purchase_price_summary IS '采购价格说明。';
COMMENT ON COLUMN supplier_contracts.party_a_name IS '甲方名称快照，通常为本企业名称，可从企业公司信息带入后手工调整。';
COMMENT ON COLUMN supplier_contracts.party_a_phone IS '甲方联系电话快照。';
COMMENT ON COLUMN supplier_contracts.party_a_fax IS '甲方传真号码快照。';
COMMENT ON COLUMN supplier_contracts.party_a_address IS '甲方办公地址快照。';
COMMENT ON COLUMN supplier_contracts.party_a_contact IS '甲方负责人或联系人快照。';
COMMENT ON COLUMN supplier_contracts.party_b_name IS '乙方名称快照，通常为供应商名称，可从供应商档案带入后手工调整。';
COMMENT ON COLUMN supplier_contracts.party_b_phone IS '乙方联系电话快照。';
COMMENT ON COLUMN supplier_contracts.party_b_fax IS '乙方传真号码快照。';
COMMENT ON COLUMN supplier_contracts.party_b_address IS '乙方地址快照。';
COMMENT ON COLUMN supplier_contracts.party_b_contact IS '乙方负责人或联系人快照。';
COMMENT ON COLUMN supplier_contracts.agreement_content IS '合同约定内容，用于记录双方核心采购合作约定。';
COMMENT ON COLUMN supplier_contracts.other_content IS '合同其它内容，用于记录补充条款或未尽事项。';
COMMENT ON COLUMN supplier_contracts.attachment_id IS '合同附件ID，关联公共附件表。';
COMMENT ON COLUMN supplier_contracts.contract_file_url IS '合同文件地址。';
COMMENT ON COLUMN supplier_contracts.reminder_days IS '到期提醒提前天数。';
COMMENT ON COLUMN supplier_contracts.status IS '合同管理状态。active表示正常使用，disabled表示停用，terminated表示提前终止。';
COMMENT ON COLUMN supplier_contracts.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN supplier_contracts.remark IS '合同备注。';
COMMENT ON COLUMN supplier_contracts.created_at IS '创建时间。';
COMMENT ON COLUMN supplier_contracts.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN supplier_contracts.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN supplier_contracts.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN supplier_contracts.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE hotel_resources IS '酒店资源表。用于维护酒店档案、房型、城市区域、星钻标准、供应商和采购价格信息。';
COMMENT ON COLUMN hotel_resources.id IS '酒店资源主键ID，系统内部使用。';
COMMENT ON COLUMN hotel_resources.tenant_id IS '租户ID，标识该酒店资源属于哪一家地接公司。';
COMMENT ON COLUMN hotel_resources.hotel_name IS '酒店名称。';
COMMENT ON COLUMN hotel_resources.city IS '酒店所在城市。';
COMMENT ON COLUMN hotel_resources.area IS '酒店所在区域或商圈。';
COMMENT ON COLUMN hotel_resources.address IS '酒店详细地址。';
COMMENT ON COLUMN hotel_resources.star_standard IS '星级、钻级或内部标准。';
COMMENT ON COLUMN hotel_resources.room_type IS '房型名称。';
COMMENT ON COLUMN hotel_resources.supplier_id IS '供应商ID，关联供应商表。';
COMMENT ON COLUMN hotel_resources.purchase_price IS '采购价，单位元。';
COMMENT ON COLUMN hotel_resources.agreement_price IS '协议价，单位元。';
COMMENT ON COLUMN hotel_resources.price_unit IS '价格单位，默认间夜。';
COMMENT ON COLUMN hotel_resources.valid_from IS '价格有效期开始日期。';
COMMENT ON COLUMN hotel_resources.valid_to IS '价格有效期结束日期。';
COMMENT ON COLUMN hotel_resources.contact_name IS '酒店联系人姓名。';
COMMENT ON COLUMN hotel_resources.contact_phone IS '酒店联系人电话。';
COMMENT ON COLUMN hotel_resources.status IS '酒店资源状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN hotel_resources.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN hotel_resources.remark IS '酒店资源备注。';
COMMENT ON COLUMN hotel_resources.created_at IS '创建时间。';
COMMENT ON COLUMN hotel_resources.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN hotel_resources.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN hotel_resources.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN hotel_resources.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE scenic_resources IS '景区资源表。用于维护景区档案、票种、供应商、采购价、协议价、价格有效期和免票半票规则。';
COMMENT ON COLUMN scenic_resources.id IS '景区资源主键ID，系统内部使用。';
COMMENT ON COLUMN scenic_resources.tenant_id IS '租户ID，标识该景区资源属于哪一家地接公司。';
COMMENT ON COLUMN scenic_resources.scenic_name IS '景区名称。';
COMMENT ON COLUMN scenic_resources.city IS '景区所在城市。';
COMMENT ON COLUMN scenic_resources.area IS '景区所在区域。';
COMMENT ON COLUMN scenic_resources.address IS '景区详细地址。';
COMMENT ON COLUMN scenic_resources.ticket_type IS '票种名称。';
COMMENT ON COLUMN scenic_resources.supplier_id IS '供应商ID，关联供应商表。';
COMMENT ON COLUMN scenic_resources.purchase_price IS '采购价，单位元。';
COMMENT ON COLUMN scenic_resources.agreement_price IS '协议价，单位元。';
COMMENT ON COLUMN scenic_resources.price_unit IS '价格单位，默认人。';
COMMENT ON COLUMN scenic_resources.valid_from IS '价格有效期开始日期。';
COMMENT ON COLUMN scenic_resources.valid_to IS '价格有效期结束日期。';
COMMENT ON COLUMN scenic_resources.free_ticket_rule IS '免票规则。';
COMMENT ON COLUMN scenic_resources.half_ticket_rule IS '半票规则。';
COMMENT ON COLUMN scenic_resources.contact_name IS '景区联系人姓名。';
COMMENT ON COLUMN scenic_resources.contact_phone IS '景区联系人电话。';
COMMENT ON COLUMN scenic_resources.status IS '景区资源状态。active表示启用，disabled表示停用。';
COMMENT ON COLUMN scenic_resources.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN scenic_resources.remark IS '景区资源备注。';
COMMENT ON COLUMN scenic_resources.created_at IS '创建时间。';
COMMENT ON COLUMN scenic_resources.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN scenic_resources.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN scenic_resources.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN scenic_resources.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON TABLE ground_agents IS '地接外委表。用于维护地接社档案、外委任务、行程要求、总预算和确认单文件。';
COMMENT ON COLUMN ground_agents.id IS '地接外委主键ID，系统内部使用。';
COMMENT ON COLUMN ground_agents.tenant_id IS '租户ID，标识该地接外委记录属于哪一家地接公司。';
COMMENT ON COLUMN ground_agents.ground_agent_name IS '地接社名称。';
COMMENT ON COLUMN ground_agents.city IS '地接社所在城市或外委目的地。';
COMMENT ON COLUMN ground_agents.contact_name IS '联系人姓名。';
COMMENT ON COLUMN ground_agents.contact_phone IS '联系人电话。';
COMMENT ON COLUMN ground_agents.task_name IS '外委任务名称。';
COMMENT ON COLUMN ground_agents.itinerary_requirement IS '行程要求或服务要求。';
COMMENT ON COLUMN ground_agents.total_budget IS '外委总预算，单位元。';
COMMENT ON COLUMN ground_agents.confirmation_attachment_id IS '确认单附件ID，关联公共附件表。';
COMMENT ON COLUMN ground_agents.confirmation_file_url IS '确认单文件地址。';
COMMENT ON COLUMN ground_agents.status IS '地接外委状态。active表示进行中，disabled表示停用，completed表示已完成。';
COMMENT ON COLUMN ground_agents.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN ground_agents.remark IS '地接外委备注。';
COMMENT ON COLUMN ground_agents.created_at IS '创建时间。';
COMMENT ON COLUMN ground_agents.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN ground_agents.is_deleted IS '是否已删除。false表示正常，true表示已软删除。';
COMMENT ON COLUMN ground_agents.deleted_at IS '删除时间。未删除时为空。';
COMMENT ON COLUMN ground_agents.deleted_by IS '删除人账号或名称。未删除时为空。';

COMMENT ON INDEX uk_customer_credit_accounts_tenant_customer_active IS '客户授信账户唯一索引，仅约束未删除记录。';
COMMENT ON INDEX uk_customer_product_auth_tenant_customer_product_active IS '客户产品授权唯一索引，仅约束未删除记录。';
COMMENT ON INDEX uk_suppliers_tenant_code_active IS '供应商编码唯一索引，仅约束未删除且编码非空记录。';
COMMENT ON INDEX uk_suppliers_tenant_name_active IS '供应商名称唯一索引，仅约束未删除记录。';
COMMENT ON INDEX uk_purchase_resources_tenant_type_name_city_active IS '采购资源名称唯一索引，仅约束同类型、同省市区县下的未删除记录。';
COMMENT ON INDEX uk_resource_projects_tenant_type_name_active IS '资源费用项目唯一索引，仅约束同资源类型下未删除项目名称。';
COMMENT ON INDEX uk_purchase_relations_tenant_resource_supplier_group_active IS '采购关系唯一索引，仅约束同资源、同供应商、同成团数量下的未删除记录。';
COMMENT ON INDEX uk_supplier_resource_prices_tenant_relation_project_active IS '供应商资源价格唯一索引，仅约束同采购关系、同费用项目下的未删除价格记录。';
COMMENT ON INDEX uk_purchase_relation_ticket_templates_tenant_relation_active IS '采购关系游客名单模板唯一索引，仅约束同采购关系下的未删除模板配置。';
COMMENT ON INDEX uk_purchase_relation_ticket_template_fields_tenant_template_column_active IS '游客名单模板字段列唯一索引，仅约束同模板下的未删除列映射。';
COMMENT ON INDEX uk_supplier_contracts_tenant_contract_no_active IS '供应商合同编号唯一索引，仅约束未删除记录。';
COMMENT ON INDEX uk_hotel_resources_tenant_name_room_active IS '酒店名称和房型唯一索引，仅约束未删除记录。';
COMMENT ON INDEX uk_scenic_resources_tenant_name_ticket_active IS '景区名称和票种唯一索引，仅约束未删除记录。';

INSERT INTO resource_projects (
  tenant_id,
  resource_type,
  project_name,
  statistics_enabled,
  sort_order,
  status,
  created_by,
  remark
)
SELECT tenant.id, seed.resource_type, seed.project_name, seed.statistics_enabled, seed.sort_order, 'active', 'system', seed.remark
FROM tenants tenant
CROSS JOIN (
  VALUES
    ('scenic', '成人', true, 10, '景区成人票价项目'),
    ('scenic', '儿童', true, 20, '景区儿童票价项目'),
    ('scenic', '免票', true, 30, '景区免票项目'),
    ('scenic', '60周岁', true, 40, '景区老人优惠项目'),
    ('scenic', '套票', true, 50, '景区套票项目'),
    ('scenic', '套票2', true, 60, '景区第二套票项目'),
    ('scenic', '其它', true, 999, '景区其它费用项目'),
    ('hotel', '标间', true, 10, '酒店标准间项目'),
    ('hotel', '大床房', true, 20, '酒店大床房项目'),
    ('hotel', '豪华大床房', true, 25, '酒店豪华大床房项目'),
    ('hotel', '三人间', true, 30, '酒店三人间项目'),
    ('hotel', '加床', true, 40, '酒店加床项目'),
    ('hotel', '房券', true, 50, '酒店房券项目'),
    ('hotel', '4人火炕', true, 55, '酒店四人火炕项目'),
    ('hotel', '准三快捷自助', true, 56, '酒店准三快捷自助项目'),
    ('hotel', '双床/大床(不指定)', true, 57, '酒店双床或大床不指定项目'),
    ('hotel', '单房差', true, 60, '酒店单人入住产生的房差项目'),
    ('hotel', '早餐', true, 70, '酒店早餐费用项目'),
    ('hotel', '其它', true, 999, '酒店其它费用项目'),
    ('restaurant', '标准餐', true, 10, '餐厅标准餐项目'),
    ('restaurant', '豪华餐', true, 20, '餐厅豪华餐项目'),
    ('restaurant', '餐券', true, 30, '餐厅餐券项目'),
    ('restaurant', '围桌餐', true, 40, '餐厅按桌结算的围桌餐项目'),
    ('restaurant', '自助餐', true, 50, '餐厅按人结算的自助餐项目'),
    ('restaurant', '其它', true, 999, '餐厅其它费用项目'),
    ('vehicle', '车费', true, 57, '车辆基础用车费用项目'),
    ('vehicle', '司机补贴', true, 58, '司机补贴费用项目'),
    ('vehicle', '司机餐补', true, 60, '司机带团期间餐费补贴项目'),
    ('vehicle', '停车费', true, 65, '车辆停车费项目'),
    ('vehicle', '过路停车费', true, 70, '车辆过路费和停车费项目'),
    ('vehicle', '其它', true, 999, '车辆其它费用项目'),
    ('traffic', '飞机', true, 5, '飞机大交通费用项目'),
    ('traffic', '高铁', true, 8, '高铁大交通费用项目'),
    ('traffic', '高铁票', true, 10, '高铁票费用项目'),
    ('traffic', '火车票', true, 15, '火车票费用项目'),
    ('traffic', '普通火车票', true, 20, '普通列车票费用项目'),
    ('traffic', '机票', true, 30, '国内或国际机票费用项目'),
    ('traffic', '船票', true, 40, '客运船票费用项目'),
    ('traffic', '订票服务费', true, 50, '大交通订票服务费用项目'),
    ('traffic', '其它', true, 999, '大交通其它费用项目'),
    ('other', '礼品', true, 5, '团队礼品费用项目'),
    ('other', '特产', true, 6, '团队特产费用项目'),
    ('other', '预收款', true, 7, '团队预收款项目'),
    ('other', '保险', true, 8, '团队保险费用项目'),
    ('other', '购物返佣', true, 9, '购物返佣费用项目'),
    ('other', '购物人头', true, 10, '购物人头费用项目'),
    ('other', '旅游意外险', true, 10, '团队旅游意外保险费用项目'),
    ('other', '停车费', true, 11, '停车费用项目'),
    ('other', '房租', true, 12, '房租费用项目'),
    ('other', '水电', true, 13, '水电费用项目'),
    ('other', '会务服务费', true, 20, '会议或活动组织服务费用项目'),
    ('other', '综合服务费', true, 30, '无法归入固定资源分类的综合服务费用项目'),
    ('other', '其它', true, 999, '其它费用项目'),
    ('misc', '矿泉水', true, 10, '团队用矿泉水费用项目'),
    ('misc', '旅游帽', true, 20, '团队旅游帽费用项目'),
    ('misc', '胸牌', true, 30, '团队胸牌制作费用项目'),
    ('misc', '行李牌', true, 40, '团队行李牌制作费用项目'),
    ('misc', '横幅', true, 50, '团队横幅制作费用项目'),
    ('ground_agent', '地接综合费', true, 10, '外地地接社综合接待费用项目'),
    ('ground_agent', '成人', true, 11, '地接成人费用项目'),
    ('ground_agent', '儿童', true, 12, '地接儿童费用项目'),
    ('ground_agent', '车费', true, 13, '地接车费项目'),
    ('ground_agent', '综费', true, 14, '地接综合费用项目'),
    ('ground_agent', '接送费', true, 15, '地接接送费用项目'),
    ('ground_agent', '代收团款', true, 16, '地接代收团款项目'),
    ('ground_agent', '定金对公', true, 17, '地接对公定金项目'),
    ('ground_agent', '团费', true, 18, '地接团费项目'),
    ('ground_agent', '成本', true, 19, '地接成本项目'),
    ('ground_agent', '地接服务费', true, 20, '地接社操作和服务费用项目'),
    ('ground_agent', '当地交通费', true, 30, '地接社安排的当地交通费用项目'),
    ('ground_agent', '当地住宿费', true, 40, '地接社安排的当地住宿费用项目'),
    ('ground_agent', '当地餐费', true, 50, '地接社安排的当地用餐费用项目'),
    ('ground_agent', '其它', true, 999, '地接其它费用项目'),
    ('extra_fee', '保险', true, 5, '附加保险费用项目'),
    ('extra_fee', '矿泉水', true, 6, '附加矿泉水费用项目'),
    ('extra_fee', '返佣', true, 7, '附加返佣项目'),
    ('extra_fee', '鲜花', true, 8, '附加鲜花费用项目'),
    ('extra_fee', '操作费', true, 9, '附加操作费用项目'),
    ('extra_fee', '签约团款', true, 10, '附加签约团款项目'),
    ('extra_fee', '礼品包帽', true, 11, '附加礼品包帽费用项目'),
    ('extra_fee', '加景点', true, 20, '临时增加景点产生的附加费用项目'),
    ('extra_fee', '升级住宿', true, 30, '临时升级酒店或房型产生的附加费用项目'),
    ('extra_fee', '加餐', true, 40, '临时增加或升级用餐产生的附加费用项目'),
    ('extra_fee', '延住', true, 50, '团队延长住宿产生的附加费用项目'),
    ('extra_fee', '超公里费', true, 60, '车辆超出约定公里数产生的附加费用项目'),
    ('extra_fee', '超时费', true, 70, '车辆或服务超出约定时间产生的附加费用项目'),
    ('extra_fee', '其它', true, 999, '附加其它费用项目'),
    ('shopping', '乳胶', true, 10, '购物乳胶项目'),
    ('shopping', '茶叶', true, 20, '购物茶叶项目'),
    ('shopping', '翡翠', true, 30, '购物翡翠项目'),
    ('shopping', '厨具', true, 40, '购物厨具项目'),
    ('shopping', '黄金饰品', true, 50, '购物黄金饰品项目'),
    ('shopping', '茶多酚', true, 55, '购物茶多酚项目'),
    ('shopping', '土特产', true, 56, '购物土特产项目'),
    ('shopping', '丝绸', true, 60, '购物丝绸项目'),
    ('shopping', '珍珠', true, 70, '购物珍珠项目'),
    ('shopping', '唐卡', true, 80, '购物唐卡项目'),
    ('shopping', '藏药', true, 90, '购物藏药项目'),
    ('shopping', '其它', true, 999, '购物其它项目')
) AS seed(resource_type, project_name, statistics_enabled, sort_order, remark)
WHERE NOT EXISTS (
  SELECT 1
  FROM resource_projects existing
  WHERE existing.tenant_id = tenant.id
    AND existing.resource_type = seed.resource_type
    AND existing.project_name = seed.project_name
    AND existing.is_deleted = false
);

COMMIT;

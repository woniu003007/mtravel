BEGIN;

CREATE TABLE IF NOT EXISTS contracts (
  id BIGSERIAL PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES tenants(id),
  contract_type varchar(40) NOT NULL,
  customer_id bigint,
  supplier_id bigint,
  contract_no varchar(80) NOT NULL,
  contract_name varchar(200) NOT NULL,
  counterparty_name varchar(200),
  start_date date,
  end_date date,
  settlement_terms text,
  purchase_price_summary text,
  legal_subject varchar(200),
  invoice_subject varchar(200),
  settlement_subject varchar(200),
  template_name varchar(120),
  reminder_days integer NOT NULL DEFAULT 30,
  attachment_id bigint,
  contract_file_url text,
  print_status varchar(40),
  status varchar(20) NOT NULL DEFAULT 'active',
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
  created_by varchar(80),
  remark text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  is_deleted boolean NOT NULL DEFAULT false,
  deleted_at timestamptz,
  deleted_by varchar(64),
  CONSTRAINT chk_contracts_type CHECK (
    contract_type IN (
      'customer', 'scenic', 'hotel', 'restaurant', 'vehicle', 'traffic', 'other',
      'ground_agent', 'guide', 'finance_fee', 'current_refund', 'extra_fee', 'shopping'
    )
  ),
  CONSTRAINT chk_contracts_status CHECK (status IN ('active', 'disabled', 'terminated')),
  CONSTRAINT chk_contracts_reminder_days CHECK (reminder_days >= 0),
  CONSTRAINT chk_contracts_single_party CHECK (customer_id IS NULL OR supplier_id IS NULL),
  CONSTRAINT fk_contracts_customer FOREIGN KEY (tenant_id, customer_id)
    REFERENCES customers (tenant_id, id),
  CONSTRAINT fk_contracts_supplier FOREIGN KEY (tenant_id, supplier_id)
    REFERENCES suppliers (tenant_id, id)
);

DROP TRIGGER IF EXISTS trg_contracts_updated_at ON contracts;
CREATE TRIGGER trg_contracts_updated_at
BEFORE UPDATE ON contracts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uk_contracts_tenant_contract_no_active
  ON contracts (tenant_id, contract_no)
  WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_contracts_tenant_deleted_type
  ON contracts (tenant_id, is_deleted, contract_type);
CREATE INDEX IF NOT EXISTS idx_contracts_tenant_deleted_customer
  ON contracts (tenant_id, is_deleted, customer_id);
CREATE INDEX IF NOT EXISTS idx_contracts_tenant_deleted_supplier
  ON contracts (tenant_id, is_deleted, supplier_id);
CREATE INDEX IF NOT EXISTS idx_contracts_tenant_deleted_end_date
  ON contracts (tenant_id, is_deleted, end_date);
CREATE INDEX IF NOT EXISTS idx_contracts_tenant_deleted_status
  ON contracts (tenant_id, is_deleted, status);

COMMENT ON TABLE contracts IS '统一合同台账。用于保存分销商合同以及景区、酒店、餐厅、车队、大交通、地接等采购合同。';
COMMENT ON COLUMN contracts.id IS '合同主键ID，由数据库自增序列生成。';
COMMENT ON COLUMN contracts.tenant_id IS '租户ID，标识合同所属的地接公司。';
COMMENT ON COLUMN contracts.contract_type IS '合同类型。customer分销商，scenic景区，hotel酒店，restaurant餐厅，vehicle车队，traffic大交通，other其它，ground_agent地接，guide导游，finance_fee财务费用，current_refund现收现退，extra_fee附加费用，shopping购物。';
COMMENT ON COLUMN contracts.customer_id IS '客户单位ID，仅分销商合同使用。';
COMMENT ON COLUMN contracts.supplier_id IS '供应商ID，采购类合同使用；无法绑定供应商的特殊合同可为空。';
COMMENT ON COLUMN contracts.contract_no IS '合同编号，同一租户内未删除记录必须唯一。';
COMMENT ON COLUMN contracts.contract_name IS '合同名称。';
COMMENT ON COLUMN contracts.counterparty_name IS '合同相对方名称快照，用于保留签署时的客户或供应商名称。';
COMMENT ON COLUMN contracts.start_date IS '合同开始日期。';
COMMENT ON COLUMN contracts.end_date IS '合同结束日期或有效期止。';
COMMENT ON COLUMN contracts.settlement_terms IS '结算方式或结算条款。';
COMMENT ON COLUMN contracts.purchase_price_summary IS '采购价格摘要，仅采购类合同使用。';
COMMENT ON COLUMN contracts.legal_subject IS '合同法律主体。';
COMMENT ON COLUMN contracts.invoice_subject IS '开票主体。';
COMMENT ON COLUMN contracts.settlement_subject IS '结算主体。';
COMMENT ON COLUMN contracts.template_name IS '合同模板名称。';
COMMENT ON COLUMN contracts.reminder_days IS '合同到期提前提醒天数。';
COMMENT ON COLUMN contracts.attachment_id IS '当前主合同附件ID，关联公共附件表。';
COMMENT ON COLUMN contracts.contract_file_url IS '合同文件访问地址。';
COMMENT ON COLUMN contracts.print_status IS '合同打印状态或打印标记。';
COMMENT ON COLUMN contracts.status IS '合同状态。active正常，disabled停用，terminated提前终止。';
COMMENT ON COLUMN contracts.party_a_name IS '甲方名称快照。';
COMMENT ON COLUMN contracts.party_a_phone IS '甲方联系电话快照。';
COMMENT ON COLUMN contracts.party_a_fax IS '甲方传真快照。';
COMMENT ON COLUMN contracts.party_a_address IS '甲方地址快照。';
COMMENT ON COLUMN contracts.party_a_contact IS '甲方联系人快照。';
COMMENT ON COLUMN contracts.party_b_name IS '乙方名称快照。';
COMMENT ON COLUMN contracts.party_b_phone IS '乙方联系电话快照。';
COMMENT ON COLUMN contracts.party_b_fax IS '乙方传真快照。';
COMMENT ON COLUMN contracts.party_b_address IS '乙方地址快照。';
COMMENT ON COLUMN contracts.party_b_contact IS '乙方联系人快照。';
COMMENT ON COLUMN contracts.agreement_content IS '合同主要约定内容。';
COMMENT ON COLUMN contracts.other_content IS '合同其它补充内容。';
COMMENT ON COLUMN contracts.created_by IS '创建人账号或名称。';
COMMENT ON COLUMN contracts.remark IS '合同内部备注。';
COMMENT ON COLUMN contracts.created_at IS '创建时间。';
COMMENT ON COLUMN contracts.updated_at IS '更新时间，由触发器自动维护。';
COMMENT ON COLUMN contracts.is_deleted IS '是否已软删除。false正常，true已删除。';
COMMENT ON COLUMN contracts.deleted_at IS '软删除时间。';
COMMENT ON COLUMN contracts.deleted_by IS '执行软删除的操作人。';
COMMENT ON INDEX uk_contracts_tenant_contract_no_active IS '合同编号唯一索引，仅约束未删除记录。';

DO $$
DECLARE
  source_record record;
  target_id bigint;
BEGIN
  IF to_regclass('public.customer_contracts') IS NOT NULL THEN
    FOR source_record IN SELECT * FROM customer_contracts ORDER BY id LOOP
      SELECT id INTO target_id
      FROM contracts
      WHERE tenant_id = source_record.tenant_id
        AND contract_no = source_record.contract_no
        AND is_deleted = source_record.is_deleted
      LIMIT 1;

      IF target_id IS NULL THEN
        INSERT INTO contracts (
          tenant_id, contract_type, customer_id, contract_no, contract_name, counterparty_name,
          start_date, end_date, settlement_terms, legal_subject, invoice_subject, settlement_subject,
          template_name, reminder_days, attachment_id, contract_file_url, print_status, status,
          party_a_name, party_a_phone, party_a_fax, party_a_address, party_a_contact,
          party_b_name, party_b_phone, party_b_fax, party_b_address, party_b_contact,
          agreement_content, other_content, created_by, remark, created_at, updated_at,
          is_deleted, deleted_at, deleted_by
        ) VALUES (
          source_record.tenant_id, 'customer', source_record.customer_id, source_record.contract_no,
          COALESCE(source_record.template_name, '分销商合同'),
          COALESCE(source_record.customer_name, source_record.party_b_name),
          source_record.start_date, source_record.end_date, source_record.settlement_method,
          source_record.legal_subject, source_record.invoice_subject, source_record.settlement_subject,
          source_record.template_name, source_record.reminder_days, source_record.attachment_id,
          source_record.contract_file_url, source_record.print_status, source_record.status,
          source_record.party_a_name, source_record.party_a_phone, source_record.party_a_fax,
          source_record.party_a_address, source_record.party_a_contact, source_record.party_b_name,
          source_record.party_b_phone, source_record.party_b_fax, source_record.party_b_address,
          source_record.party_b_contact, source_record.agreement_content, source_record.other_content,
          source_record.created_by, source_record.remark, source_record.created_at,
          source_record.updated_at, source_record.is_deleted, source_record.deleted_at,
          source_record.deleted_by
        ) RETURNING id INTO target_id;
      END IF;

      UPDATE common_attachments
      SET business_module = '合同管理',
          business_type = '合同',
          business_id = target_id
      WHERE tenant_id = source_record.tenant_id
        AND business_id = source_record.id
        AND business_type IN ('客户销售合同', '客户合同');
    END LOOP;
  END IF;

  IF to_regclass('public.supplier_contracts') IS NOT NULL THEN
    FOR source_record IN SELECT * FROM supplier_contracts ORDER BY id LOOP
      SELECT id INTO target_id
      FROM contracts
      WHERE tenant_id = source_record.tenant_id
        AND contract_no = source_record.contract_no
        AND is_deleted = source_record.is_deleted
      LIMIT 1;

      IF target_id IS NULL THEN
        INSERT INTO contracts (
          tenant_id, contract_type, supplier_id, contract_no, contract_name, counterparty_name,
          start_date, end_date, settlement_terms, purchase_price_summary, reminder_days,
          attachment_id, contract_file_url, status, party_a_name, party_a_phone, party_a_fax,
          party_a_address, party_a_contact, party_b_name, party_b_phone, party_b_fax,
          party_b_address, party_b_contact, agreement_content, other_content, created_by,
          remark, created_at, updated_at, is_deleted, deleted_at, deleted_by
        ) VALUES (
          source_record.tenant_id,
          CASE WHEN source_record.contract_category = 'ticket' THEN 'scenic' ELSE source_record.contract_category END,
          source_record.supplier_id, source_record.contract_no, source_record.contract_name,
          source_record.party_b_name, source_record.start_date, source_record.end_date,
          source_record.settlement_terms, source_record.purchase_price_summary,
          source_record.reminder_days, source_record.attachment_id, source_record.contract_file_url,
          source_record.status, source_record.party_a_name, source_record.party_a_phone,
          source_record.party_a_fax, source_record.party_a_address, source_record.party_a_contact,
          source_record.party_b_name, source_record.party_b_phone, source_record.party_b_fax,
          source_record.party_b_address, source_record.party_b_contact,
          source_record.agreement_content, source_record.other_content, source_record.created_by,
          source_record.remark, source_record.created_at, source_record.updated_at,
          source_record.is_deleted, source_record.deleted_at, source_record.deleted_by
        ) RETURNING id INTO target_id;
      END IF;

      UPDATE common_attachments
      SET business_module = '合同管理',
          business_type = '合同',
          business_id = target_id
      WHERE tenant_id = source_record.tenant_id
        AND business_id = source_record.id
        AND business_type IN ('采购合同', '供应商合同');
    END LOOP;
  END IF;
END;
$$;

COMMIT;

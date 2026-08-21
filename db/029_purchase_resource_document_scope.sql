-- 采购资源资料从仅景区扩展为全部统一资源主档
-- PostgreSQL

BEGIN;

ALTER TABLE knowledge_documents
  DROP CONSTRAINT IF EXISTS chk_knowledge_documents_source_type;

ALTER TABLE knowledge_document_chunks
  DROP CONSTRAINT IF EXISTS chk_knowledge_document_chunks_source_type;

-- 只替换来源标识，不改文件、抽取文本或已生成的向量。
UPDATE knowledge_documents
SET source_type = 'purchase_resource'
WHERE source_type = 'purchase_resource_scenic';

UPDATE knowledge_document_chunks
SET source_type = 'purchase_resource'
WHERE source_type = 'purchase_resource_scenic';

UPDATE common_attachments attachment
SET business_type = '资源资料'
WHERE attachment.business_module = '采购管理'
  AND attachment.business_type = '景区资料'
  AND EXISTS (
    SELECT 1
    FROM knowledge_documents document
    WHERE document.tenant_id = attachment.tenant_id
      AND document.attachment_id = attachment.id
      AND document.source_type = 'purchase_resource'
  );

ALTER TABLE knowledge_documents
  ADD CONSTRAINT chk_knowledge_documents_source_type CHECK (source_type IN ('purchase_resource'));

ALTER TABLE knowledge_document_chunks
  ADD CONSTRAINT chk_knowledge_document_chunks_source_type CHECK (source_type IN ('purchase_resource'));

COMMENT ON COLUMN knowledge_documents.source_type IS '业务来源类型。purchase_resource表示采购资源资料。';
COMMENT ON COLUMN knowledge_document_chunks.source_type IS '业务来源类型。purchase_resource表示采购资源资料。';

COMMIT;

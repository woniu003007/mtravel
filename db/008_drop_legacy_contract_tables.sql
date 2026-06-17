BEGIN;

-- 本脚本仅在 007 迁移完成并核对合同与附件数量后执行。
DROP TABLE IF EXISTS customer_contracts;
DROP TABLE IF EXISTS supplier_contracts;

COMMIT;

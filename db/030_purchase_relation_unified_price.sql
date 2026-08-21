-- 将供应商统一报价从费用项目明细收拢到采购关系。
-- 分类报价仍保留在 supplier_resource_prices。

BEGIN;

ALTER TABLE purchase_relations
  ADD COLUMN IF NOT EXISTS unified_price numeric(14,2),
  ADD COLUMN IF NOT EXISTS price_remark text;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM purchase_relations r
    WHERE r.is_deleted = false
      AND r.price_mode = 'unified'
      AND NOT EXISTS (
        SELECT 1
        FROM supplier_resource_prices p
        WHERE p.tenant_id = r.tenant_id
          AND p.relation_id = r.id
          AND p.is_deleted = false
      )
      AND r.unified_price IS NULL
  ) THEN
    RAISE EXCEPTION '存在无法迁移的统一报价关系：无报价明细';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM purchase_relations r
    JOIN supplier_resource_prices p
      ON p.tenant_id = r.tenant_id
     AND p.relation_id = r.id
     AND p.is_deleted = false
    WHERE r.is_deleted = false
      AND r.price_mode = 'unified'
    GROUP BY r.tenant_id, r.id
    HAVING COUNT(DISTINCT p.team_price) <> 1
  ) THEN
    RAISE EXCEPTION '存在无法迁移的统一报价关系：明细金额不一致';
  END IF;
END
$$;

UPDATE purchase_relations r
SET unified_price = source.team_price,
    price_remark = source.price_remark
FROM (
  SELECT p.tenant_id,
         p.relation_id,
         MIN(p.team_price) AS team_price,
         CASE WHEN COUNT(DISTINCT NULLIF(BTRIM(p.price_description), '')) = 1
              THEN MAX(NULLIF(BTRIM(p.price_description), ''))
              ELSE NULL
         END AS price_remark
  FROM supplier_resource_prices p
  WHERE p.is_deleted = false
  GROUP BY p.tenant_id, p.relation_id
  HAVING COUNT(DISTINCT p.team_price) = 1
) source
WHERE r.tenant_id = source.tenant_id
  AND r.id = source.relation_id
  AND r.is_deleted = false
  AND r.price_mode = 'unified'
  AND r.unified_price IS NULL;

UPDATE supplier_resource_prices p
SET is_deleted = true,
    deleted_at = COALESCE(p.deleted_at, now()),
    deleted_by = COALESCE(p.deleted_by, 'unified-price-migration')
FROM purchase_relations r
WHERE r.tenant_id = p.tenant_id
  AND r.id = p.relation_id
  AND r.is_deleted = false
  AND r.price_mode = 'unified'
  AND r.unified_price IS NOT NULL
  AND p.is_deleted = false;

ALTER TABLE purchase_relations
  DROP CONSTRAINT IF EXISTS chk_purchase_relations_unified_price;

ALTER TABLE purchase_relations
  ADD CONSTRAINT chk_purchase_relations_unified_price CHECK (
    is_deleted = true
    OR (price_mode = 'unified' AND unified_price IS NOT NULL AND unified_price >= 0)
    OR (price_mode = 'classified' AND unified_price IS NULL)
  );

COMMENT ON TABLE purchase_relations IS '采购关系表。用于维护资源与供应商之间的绑定关系、成团数量和关系级统一报价。';
COMMENT ON COLUMN purchase_relations.price_mode IS '报价模式。unified表示统一报价，classified表示按资源费用项目分别报价。';
COMMENT ON COLUMN purchase_relations.purchase_price IS '历史兼容采购价格字段，当前统一报价使用 unified_price，分类报价由 supplier_resource_prices 维护。';
COMMENT ON COLUMN purchase_relations.price_unit IS '历史兼容价格单位字段，当前计价单位以资源主档和业务场景为准。';
COMMENT ON COLUMN purchase_relations.unified_price IS '统一报价金额，仅统一报价模式使用，单位元。';
COMMENT ON COLUMN purchase_relations.price_remark IS '统一报价的适用条件、有效范围或补充说明。';

COMMIT;

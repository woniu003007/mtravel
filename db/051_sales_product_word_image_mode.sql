-- 产品 Word 图片展示方式：按天保存“跟随景区 / 当天末尾 / 不展示”。
BEGIN;

ALTER TABLE sales_product_itinerary_days
  ADD COLUMN IF NOT EXISTS word_image_mode varchar(30) NOT NULL DEFAULT 'follow_resource';

UPDATE sales_product_itinerary_days
SET word_image_mode = 'follow_resource'
WHERE word_image_mode IS NULL OR word_image_mode NOT IN ('follow_resource', 'day_end', 'hidden');

ALTER TABLE sales_product_itinerary_days
  DROP CONSTRAINT IF EXISTS chk_sales_product_itinerary_word_image_mode;

ALTER TABLE sales_product_itinerary_days
  ADD CONSTRAINT chk_sales_product_itinerary_word_image_mode
  CHECK (word_image_mode IN ('follow_resource', 'day_end', 'hidden'));

COMMENT ON COLUMN sales_product_itinerary_days.word_image_mode IS '产品 Word 图片展示方式：跟随景区、当天末尾或不展示。';

COMMIT;

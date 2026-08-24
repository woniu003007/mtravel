-- 产品设计每日主行程城市：服务日卡、普通地图、全屏地图和当天编排区的同一城市上下文。
-- 不复用 related_hotel，避免酒店说明与当天地图默认范围混淆。
BEGIN;

ALTER TABLE sales_product_itinerary_days
  ADD COLUMN IF NOT EXISTS destination_province varchar(80),
  ADD COLUMN IF NOT EXISTS destination_city varchar(80),
  ADD COLUMN IF NOT EXISTS destination_district varchar(80);

COMMENT ON COLUMN sales_product_itinerary_days.destination_province IS '当天主行程目的地省份。';
COMMENT ON COLUMN sales_product_itinerary_days.destination_city IS '当天主行程目的地城市，驱动地图默认筛选范围。';
COMMENT ON COLUMN sales_product_itinerary_days.destination_district IS '当天主行程目的地区县，可选。';

COMMIT;

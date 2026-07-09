ALTER TABLE sales_order_charge_lines
  ADD COLUMN IF NOT EXISTS occupy_seat boolean NOT NULL DEFAULT true;

UPDATE sales_order_charge_lines
SET occupy_seat = CASE
  WHEN line_kind = 'base_price' AND line_type IN ('adult', 'child', 'senior', 'escort') THEN true
  ELSE false
END
WHERE line_kind = 'base_price';

COMMENT ON COLUMN sales_order_charge_lines.occupy_seat IS '是否占用团队人数名额，仅原始价格行使用；单房差、附加费等费用项通常不占位。';

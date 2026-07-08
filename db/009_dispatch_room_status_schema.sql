-- 旅游接待管理系统：计调房源与房态库存退役清理
--
-- 甲方已确认当前业务没有自营房源，也不需要独立房态库存/锁房模块。
-- 本脚本保留编号 009，作为历史模块退役的可重复执行清理脚本，避免后续环境重新建出相关表。
-- 酒店基础资料继续使用采购管理的 hotel_resources 和采购关系相关表。

BEGIN;

DROP TABLE IF EXISTS controlled_room_day_statuses CASCADE;
DROP TABLE IF EXISTS controlled_room_lock_records CASCADE;
DROP TABLE IF EXISTS room_inventories CASCADE;
DROP TABLE IF EXISTS controlled_room_units CASCADE;
DROP TABLE IF EXISTS controlled_room_types CASCADE;
DROP TABLE IF EXISTS controlled_room_resources CASCADE;

COMMIT;

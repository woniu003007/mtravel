# 数据库脚本目录

本目录用于保存旅游接待管理系统的数据库结构脚本，避免表结构只存在远程数据库中。

## 文件说明

| 文件 | 作用 |
| --- | --- |
| `001_customer_management_schema.sql` | 客户管理模块早期基础表结构：租户、客户分类、客户单位及历史客户合同迁移源表。 |
| `002_system_operation_log_schema.sql` | 系统操作日志表结构：记录接口访问、业务操作、失败原因和审计信息。 |
| `003_system_config_schema.sql` | 系统配置表结构：保存租户级登录安全等运行参数。 |
| `004_system_user_schema.sql` | 系统用户表结构：保存租户后台账号、密码哈希、基础角色和账号状态。 |
| `005_customer_purchase_expansion_schema.sql` | 客户扩展、采购管理、公共附件与知识文档表结构，包含历史供应商合同迁移源表。 |
| `006_enterprise_basic_schema.sql` | 企业资料基础表结构：企业银行账号、企业部门、企业角色、角色权限、企业员工等企业基础资料。 |
| `007_unified_contract_schema.sql` | 建立统一合同表 `contracts`，迁移客户合同、供应商合同及公共附件归属。 |
| `008_drop_legacy_contract_tables.sql` | 在迁移核对完成后删除旧 `customer_contracts`、`supplier_contracts` 表。 |
| `009_dispatch_room_status_schema.sql` | 计调房源与房态库存退役清理脚本，删除历史自控房源、房态库存和锁房流水表。 |
| `010_product_dictionary_schema.sql` | 企业资料产品字典表结构，维护产品模板可选择的业务类型、接待标准和产品主题。 |
| `011_sales_product_schema.sql` | 销售产品模板表结构，维护产品主档、产品用途隔离、每日行程、地图路书、产品说明、团队安排参数、座位数报价规则、用车报价快照、用车询价记录和用车历史候选。 |
| `012_sales_team_schema.sql` | 销售团期与团队价格表结构，维护由产品生成的团队、客户类型价格、状态日志和团号生成日志。 |
| `013_sales_booking_order_schema.sql` | 销售收客订单主链路表结构，维护团队下订单、统一应收明细、游客名单、费用变更兼容记录和拼团/转团流转日志。 |
| `014_customer_risk_approval_schema.sql` | 客户风控审批表结构，维护客户合同到期、授信超限时的客户授信审批流水和申请时快照。 |
| `015_dispatch_guide_schedule_schema.sql` | 导游安排、导游排班与请假表结构，维护团队导游安排和导游不可上团时间。 |
| `016_dispatch_team_arrangement_schema.sql` | 正式团队安排成本表结构，维护大交通、住宿、用车、景区、用餐、其它、自费、购物、地接、附加等团队执行成本、价格明细、订单归属、分类专用字段、分类流程状态和流程同步流水。 |
| `017_vehicle_quote_rule_expense_item_sync.sql` | 数据补丁：将已启用座位数报价规则同步补齐为车队费用项目，保证用车座位数和价格信息项目口径一致。 |
| `018_vehicle_expense_legacy_seed_cleanup.sql` | 数据补丁：软删除早期描述型车队费用项目种子，并按启用座位数报价规则修正同名费用项目排序。 |
| `019_product_team_arrangement_reference_cleanup.sql` | 数据补丁：清理产品排期生成团队时历史自动带入的模板人数、数量和金额，保留资源和参考单价。 |
| `020_finance_guide_imprest_schema.sql` | 导游备用金申请与发放表结构，维护备用金计算快照、总经理审批、分次付款记录和付款余额。 |
| `021_finance_shopping_commission_schema.sql` | 购物业绩反馈与公司补佣结算表结构，维护购物规则、团队规则覆盖、购物店反馈、公司补佣和内外账购物利润快照。 |
| `026_sales_product_scope.sql` | 数据迁移：为销售产品主表增加产品用途字段，区分正式产品模板、团队专属快照和产品设计草稿，并将产品名称唯一约束收敛到正式模板和设计草稿。 |
| `028_customer_credit_and_resource_quote_rule_schema.sql` | 客户授信规则与普通资源报价规则表结构，维护客户等级授信口径及资源报价上浮规则。 |
| `028_agent_customer_service_api_schema.sql` | 客服 Agent 专用接口支撑表，维护服务令牌、客户服务能力、结构化政策、询价任务、转人工待办和幂等约束。 |
| `029_purchase_resource_document_scope.sql` | 数据补丁：将采购资源资料从仅景区扩展为八类统一资源，并迁移历史来源标识。 |
| `030_purchase_relation_unified_price.sql` | 表结构与数据迁移：将统一报价收拢到采购关系，并软删除历史自动展开的费用项目明细。 |
| `031_customer_level_credit_approval.sql` | 表结构与数据迁移：增加客户等级账期、超额审批人/抄送人配置及审批步骤快照。 |
| `032_sales_quote_config_schema.sql` | 销售报价配置表结构：维护普通资源报价规则、导游等级、导游报价、地接人数区间整团价和统一低价审批人员配置。 |
| `033_sales_team_document_import_schema.sql` | 团队 Word 智能代录表结构：维护导入任务草稿、正式写入幂等记录和采购资源别名。 |
| `034_purchase_resource_materials_schema.sql` | 采购资源资料库表结构：维护可发布的介绍素材、介绍正文向量切片和可复用图片素材。 |
| `035_sales_quote_resource_quote_mode.sql` | 销售报价配置数据迁移：为普通资源报价规则增加可用报价方式。 |
| `037_sales_product_designer_schema.sql` | 销售产品地图式设计工作台表结构：维护每日资源快照、配图快照、成人报价草稿和生成文件版本。 |
| `038_sales_product_design_draft_scope.sql` | 表结构迁移：补齐 `sales_products.product_scope='design_draft'` 约束、查询索引，并将产品名称唯一约束限定到正式模板和设计草稿，排除团队专属快照。 |
| `039_resource_introduction_notice.sql` | 表结构迁移：为资源介绍增加注意事项，并为产品每日资源增加注意事项快照。 |
| `040_sales_product_day_resource_introductions.sql` | 产品设计迁移：为一个产品日资源增加多个介绍素材快照、排序和软删除约束，生成 Word 时按素材顺序串联。 |
| `041_sales_quote_department_manager_approval.sql` | 销售报价审批迁移：关联部门负责人企业员工账号，并增加直属领导/指定人员审批模式。 |
| `042_purchase_relation_optional_items.sql` | 采购关系自费项目报价：维护供应商提供的景区自费门票/游览项目成本，固定按元/人计价。 |
| `043_purchase_resource_introduction_visit_duration.sql` | 表结构迁移：为资源介绍素材增加建议游览时间。 |
| `044_purchase_resource_introduction_warm_tip.sql` | 表结构迁移：为资源介绍及产品设计快照增加温馨提示，并明确注意事项按红色输出。 |
| `045_purchase_resource_introduction_optional_item_flag.sql` | 表结构迁移：为资源介绍素材增加自费项目标记，复用现有介绍内容和产品输出流程。 |
| `046_resource_optional_item_product_word.sql` | 资源级自费项目主档、供应商成本及建议对外价、介绍素材关联和产品最终对外价快照。 |
| `047_purchase_resource_introduction_images.sql` | 资源介绍素材图片关联表结构，维护素材选用资源图片及输出顺序。 |
| `048_purchase_resource_introduction_sort_order.sql` | 表结构迁移：为资源介绍素材增加资源内维护排序，支持资料页拖拽排序。 |
| `049_purchase_resource_introduction_extension_blocks.sql` | 表结构迁移：为资源介绍素材增加可排序扩展内容模块，并为产品设计保存对应快照。 |
| `050_purchase_resource_reception_standard.sql` | 表结构迁移：将酒店资源星级改为复用企业产品字典 `reception_standard`。 |
| `051_sales_product_designer_accommodation_and_meals.sql` | 产品设计迁移：将酒店、早餐、中餐、晚餐改为明确的资源编排归属，同一天仅一间酒店，并保证每餐次唯一餐厅。 |
| `052_sales_product_designer_multi_hotel_accommodation.sql` | 历史误迁移记录：曾移除每天仅一个酒店的限制，当前禁止新执行；已执行的库使用 055 恢复。 |
| `053_sales_product_designer_resource_arrangement_v2.sql` | 产品设计资源编排 V2：增加地接独立编排、采购关系和报价模式快照，以及产品级全程用车表、约束和查询索引。 |
| `054_sales_product_designer_not_required_price_mode.sql` | 产品设计资源编排修正：允许每日资源和全程用车的报价快照明确记录 `not_required`，避免将无需采购误标为待询价。 |
| `055_sales_product_designer_single_hotel_accommodation.sql` | 产品设计规则修正：为已执行 052 的库恢复同一天仅一家酒店的唯一约束，并在多酒店数据存在时拒绝执行。 |
| `056_sales_product_designer_day_destination.sql` | 产品设计每日主行程城市：保存每日省、市、区县，驱动日卡与地图默认城市范围，不与酒店说明混用。 |
| `数据库设计-客户管理四表说明.md` | 客户管理四表的表作用、字段含义、软删除策略和唯一约束说明。 |
| `数据库设计-系统操作日志说明.md` | 系统操作日志表的表作用、字段含义、脱敏规则和索引说明。 |
| `数据库设计-系统配置说明.md` | 系统配置表的表作用、字段含义、配置项和索引说明。 |
| `数据库设计-系统用户说明.md` | 系统用户表的表作用、字段含义、登录规则和基础角色说明。 |
| `数据库设计-客户采购扩展表说明.md` | 客户扩展、采购管理、公共附件与知识文档表的表作用、字段含义、软删除策略和唯一约束说明，包含采购资源总览、费用项目和供应商资源价格。 |
| `数据库设计-企业资料基础表说明.md` | 企业资料基础表的表作用、字段含义、软删除策略和唯一约束说明。 |
| `数据库设计-统一合同表说明.md` | 统一合同台账的字段、类型、关联主体、附件和迁移规则说明。 |
| `数据库设计-计调自控房源与房态库存说明.md` | 计调房源与房态库存模块退役说明，记录替代业务口径。 |
| `数据库设计-产品字典说明.md` | 产品字典的字段、类型、默认选项、软删除策略和后续产品模板联动说明。 |
| `数据库设计-销售产品模板说明.md` | 销售产品模板、产品设计草稿、每日行程、地图路书、产品说明、团队安排参数、座位数报价规则、用车报价快照、用车询价记录和用车历史候选的字段、约束、索引和模块边界说明。 |
| `数据库设计-销售团队团期说明.md` | 销售团队、团期价格、团队状态日志和团号生成日志的字段、状态规则和模块边界说明。 |
| `数据库设计-销售收客订单说明.md` | 销售收客订单、统一应收明细、游客名单、费用变更兼容记录、拼团/转团流转日志的字段、状态规则、团队人数联动和索引说明。 |
| `数据库设计-客户风控审批说明.md` | 客户风控审批申请的字段、状态规则、订单保存拦截和索引说明。 |
| `数据库设计-导游排班与请假说明.md` | 团队导游安排、导游请假、排班占用和冲突规则说明。 |
| `数据库设计-正式团队安排成本说明.md` | 正式团队安排成本、价格明细、订单归属、多订单均摊、导游报账同步和财务统计支撑说明。 |
| `数据库设计-导游备用金说明.md` | 导游备用金申请、计算明细和付款记录的字段、状态、公式和财务边界说明。 |
| `数据库设计-购物业绩与阶梯佣金说明.md` | 购物业绩反馈、参考阶梯测算、公司补佣、内外账利润和导游结算支撑说明。 |
| `数据库设计-客户授信与普通资源报价规则说明.md` | 客户授信规则和普通资源报价规则的字段、唯一约束、默认规则与比例口径说明。 |
| `数据库设计-Agent客服接口说明.md` | Agent 客服接口专用表、稳定产品授权、数据安全、状态、幂等和兼容性说明。 |
| `数据库设计-销售报价配置说明.md` | 销售报价配置、导游等级、导游报价、地接区间价和统一报价审批配置的字段、约束和业务边界说明。 |
| `数据库设计-团队文档智能代录说明.md` | 团队文档智能代录任务、幂等写入、资源别名及金额边界说明。 |
| `数据库设计-资源资料库说明.md` | 采购资源介绍素材、图片素材、向量切片和删除边界说明。 |
| `数据库设计-销售产品设计工作台说明.md` | 产品地图设计工作台的设计草稿生命周期、资源快照、成人报价和对外文档版本边界说明。 |

## 当前约定

- 数据库使用 PostgreSQL。
- 所有业务表默认带 `tenant_id`，支持多租户数据隔离。
- 客户、合同等基础资料默认软删除，不做业务物理删除；旧合同表仅在结构迁移时物理删除。
- 常规查询必须过滤 `tenant_id` 和 `is_deleted = false`；销售产品查询还必须按 `product_scope` 区分正式模板、设计草稿和团队快照。
- 物理删除只用于测试数据清理或归档后的后台维护。
- 后端接口统一使用 `GET` 和 `POST`，操作日志通过路径或业务标记识别真实操作类型。
- 登录无操作自动退出时间默认 `120` 分钟，可按租户在 `system_configs` 中配置。
- 客户合同到期或授信超限是否启用客户授信审批由 `system_configs.customer_risk_approval_enabled` 控制，默认关闭时只提醒不阻断；开启后按客户等级指定审批人顺序处理。
- 后台登录账号保存在 `system_users`，密码只保存哈希，不保存明文；员工资料保存在 `enterprise_employees`，通过账号 ID 关联登录账号。
- 导游备用金公司规定加点率由 `system_configs.guide_imprest_company_markup_rate` 控制，默认 70，按百分数保存。

## 产品设计资源编排 V2 升级说明

- 当前规则为同一天只安排一家酒店；早期 `051_sales_product_designer_accommodation_and_meals.sql` 的索引谓词包含 `accommodation`，与此规则一致，已执行该脚本的库不得执行 052。
- `052` 是已废止的历史多酒店迁移。若目标库曾执行 052，必须先预检同日多酒店数据，再执行 `055_sales_product_designer_single_hotel_accommodation.sql` 恢复酒店唯一约束。`053` 在保留历史 `vehicle`/`traffic` 每日资源可读的同时，将有效历史地接普通行程转换为 `ground_service`，新增用车写入产品级 `sales_product_designer_vehicle_arrangements`。
- `054` 将报价快照可选值扩展为 `not_required`，用于明确无需采购且零成本的资源，不将其计为待询价。
- 新安装结构由 `037_sales_product_designer_schema.sql` 提供最终基础定义；常规历史升级按 `051 → 053 → 054` 执行。仅已执行 052 的库补执行 `055`。执行远程迁移前须再次确认目标为当前 `mtravel` 数据库，并完成历史数据预检和备份。

远程迁移完成后，使用以下只读 SQL 核验：

```sql
SELECT indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname = 'uk_sales_product_day_resources_day_meal_role_active';

SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'sales_product_day_resources'::regclass
  AND conname LIKE 'chk_sales_product_day_resources%';

SELECT column_name
FROM information_schema.columns
WHERE table_name = 'sales_product_designer_vehicle_arrangements'
ORDER BY ordinal_position;
```

预期酒店餐次索引列出 `accommodation`、`breakfast`、`lunch`、`dinner`；日资源角色与采购快照列已存在；全程用车表已建立。执行 055 前仍应按目标库实际数据复核活跃住宿重复记录。

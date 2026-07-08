export type PrototypePhase = 'P0' | 'P1' | 'P2';

export interface PrototypeSpec {
  key: string;
  module: string;
  title: string;
  routePath: string;
  icon: string;
  phase: PrototypePhase;
  oldMenu: string;
  buildMode: string;
  objective: string;
  featureText: string;
  problem: string;
  delivery: string;
  confirmPoints: string[];
  metrics?: Array<{ label: string; value: string; trend: string }>;
  scenarioRows?: PrototypeRecord[];
}

export interface PrototypeRecord {
  amount?: string;
  customer?: string;
  date?: string;
  id: string;
  owner?: string;
  progress?: number;
  risk?: string;
  stage?: string;
  status: string;
  team?: string;
  title: string;
}

export interface PrototypePageConfig extends PrototypeSpec {
  actions: string[];
  columns: Array<{ dataIndex?: string; key: string; title: string; width?: number }>;
  filters: Array<{ label: string; options?: string[]; placeholder: string; type: 'input' | 'select' }>;
  rows: PrototypeRecord[];
  steps: string[];
}

export interface PrototypeModule {
  icon: string;
  key: string;
  order: number;
  path: string;
  title: string;
}

export const prototypeModules: PrototypeModule[] = [
  { icon: 'lucide:layout-dashboard', key: 'dashboard', order: -1, path: '/dashboard', title: '工作台' },
  { icon: 'lucide:contact', key: 'customer', order: 1, path: '/customer', title: '客户管理' },
  { icon: 'lucide:shopping-bag', key: 'purchase', order: 2, path: '/purchase', title: '采购管理' },
  { icon: 'lucide:shopping-cart', key: 'sales', order: 3, path: '/sales', title: '销售管理' },
  { icon: 'lucide:clipboard-check', key: 'dispatch', order: 4, path: '/dispatch', title: '计调操作' },
  { icon: 'lucide:building-2', key: 'enterprise', order: 7, path: '/enterprise', title: '企业资料' },
];

const p0Rows: Record<string, PrototypeRecord[]> = {
  'customer-credit': [
    { amount: '授信 80万 / 占用 68万', customer: '杭州远行国旅', date: '2026-05-14', id: 'C03-001', owner: '财务部 周敏', progress: 85, risk: '超限前预警', stage: '订单确认', status: '待审批', team: 'HZ20260518-003', title: '客户授信与实时应收' },
    { amount: '授信 45万 / 占用 21万', customer: '上海春秋门店', date: '2026-05-15', id: 'C03-002', owner: '销售部 李娜', progress: 47, risk: '正常', stage: '费用变更', status: '可下单', team: 'SH20260519-006', title: '费用变更占用额度' },
    { amount: '授信 30万 / 占用 33万', customer: '南京研学中心', date: '2026-05-16', id: 'C03-003', owner: '总经理 王总', progress: 110, risk: '已超限', stage: '超限审批', status: '拦截中', team: 'NJ20260520-002', title: '超授信审批' },
  ],
  'sales-order': [
    { amount: '应收 86,400', customer: '杭州远行国旅', date: '2026-05-18', id: 'S04-001', owner: '销售 张伟', progress: 80, risk: '确认件待上传', stage: '订单确认', status: '待确认', team: 'HZ20260518-003', title: '西湖宋城二日游散拼订单' },
    { amount: '应收 132,000', customer: '上海春秋门店', date: '2026-05-19', id: 'S04-002', owner: '销售 李娜', progress: 100, risk: '正常', stage: '已生成应收', status: '已确认', team: 'SH20260519-006', title: '千岛湖黄山三日游整团订单' },
    { amount: '变更 +4,800', customer: '南京研学中心', date: '2026-05-20', id: 'S04-003', owner: '销售 王芳', progress: 60, risk: '授信临界', stage: '费用变更', status: '审批中', team: 'NJ20260520-002', title: '加订景区门票费用变更' },
  ],
  'sales-team': [
    { amount: '收入 18.6万 / 成本 13.2万', customer: '杭州远行国旅', date: '2026-05-18', id: 'S03-001', owner: '计调 刘洋', progress: 76, risk: '酒店待确认', stage: '排团中', status: '执行准备', team: 'HZ20260518-003', title: '西湖宋城二日游' },
    { amount: '收入 31.4万 / 成本 23.7万', customer: '上海春秋门店', date: '2026-05-19', id: 'S03-002', owner: '计调 陈晨', progress: 92, risk: '正常', stage: '待财务审核', status: '已排团', team: 'SH20260519-006', title: '千岛湖黄山三日游' },
    { amount: '收入 9.8万 / 成本 7.4万', customer: '南京研学中心', date: '2026-05-20', id: 'S03-003', owner: '计调 刘洋', progress: 54, risk: '车价超预算', stage: '车调询价', status: '需处理', team: 'NJ20260520-002', title: '研学定制二日游' },
  ],
  'dispatch-team-arrange': [
    { amount: '预算成本 132,000', customer: '杭州远行国旅', date: '2026-05-18', id: 'D01-001', owner: '计调 刘洋', progress: 82, risk: '住宿未完成', stage: '资源安排', status: '待补齐', team: 'HZ20260518-003', title: '导/交/住/车/景/餐安排总控' },
    { amount: '预算成本 237,000', customer: '上海春秋门店', date: '2026-05-19', id: 'D01-002', owner: '计调 陈晨', progress: 96, risk: '正常', stage: '提交审核', status: '可审核', team: 'SH20260519-006', title: '团队资源安排核对' },
    { amount: '预算成本 74,000', customer: '南京研学中心', date: '2026-05-20', id: 'D01-003', owner: '计调 刘洋', progress: 62, risk: '车辆冲突', stage: '异常处理', status: '需协调', team: 'NJ20260520-002', title: '车辆与房态异常处理' },
  ],
  'finance-team-audit': [
    { amount: '毛利 54,000', customer: '杭州远行国旅', date: '2026-05-18', id: 'F01-001', owner: '财务 周敏', progress: 88, risk: '成本超预算 6%', stage: '财务审核', status: '待复核', team: 'HZ20260518-003', title: '团队收支与成本审核' },
    { amount: '毛利 77,000', customer: '上海春秋门店', date: '2026-05-19', id: 'F01-002', owner: '财务 周敏', progress: 100, risk: '正常', stage: '生成账款', status: '已通过', team: 'SH20260519-006', title: '应收应付生成' },
    { amount: '毛利 24,000', customer: '南京研学中心', date: '2026-05-20', id: 'F01-003', owner: '财务 赵杰', progress: 70, risk: '备用金未核销', stage: '退回计调', status: '待补凭证', team: 'NJ20260520-002', title: '导游报账凭证核验' },
  ],
  'finance-receivable': [
    { amount: '86,400 / 已收 30,000', customer: '杭州远行国旅', date: '2026-05-18', id: 'F04-001', owner: '财务 周敏', progress: 35, risk: '账龄 9 天', stage: '实时应收', status: '部分收款', team: 'HZ20260518-003', title: '订单确认生成应收快照' },
    { amount: '132,000 / 已收 132,000', customer: '上海春秋门店', date: '2026-05-19', id: 'F04-002', owner: '财务 周敏', progress: 100, risk: '正常', stage: '收款完成', status: '已结清', team: 'SH20260519-006', title: '客户回款释放授信额度' },
    { amount: '48,800 / 已收 0', customer: '南京研学中心', date: '2026-05-20', id: 'F04-003', owner: '销售 王芳', progress: 0, risk: '授信占用', stage: '待收款', status: '待催收', team: 'NJ20260520-002', title: '费用变更应收追踪' },
  ],
  'statistics-reception': [
    { amount: '有效人数 286', customer: '散拼渠道', date: '2026-05', id: 'BI02-001', owner: '运营部', progress: 100, risk: '口径已统一', stage: '月度统计', status: '已生成', team: '全渠道', title: '收客统计与有效人数口径' },
    { amount: '成人 214 / 儿童 72', customer: '批发商渠道', date: '2026-05', id: 'BI02-002', owner: '销售部', progress: 100, risk: '房差不计人数', stage: '渠道统计', status: '已生成', team: '全部团队', title: '渠道收客结构' },
    { amount: '同比 +18%', customer: '研学渠道', date: '2026-05', id: 'BI02-003', owner: '管理层', progress: 82, risk: '名单待补齐', stage: '趋势分析', status: '待校验', team: '研学团队', title: '有效人数趋势' },
  ],
};

function spec(spec: PrototypeSpec): PrototypeSpec {
  return spec;
}

export const prototypeSpecs: PrototypeSpec[] = [
  spec({ key: 'customer-unit', module: 'customer', title: '客户单位', routePath: '/customer/unit', icon: 'lucide:building', phase: 'P0', oldMenu: '客户单位', buildMode: '保留增强', objective: '升级客户主档，串联订单、合同、授信和应收。', featureText: '客户列表、客户详情、联系人、附件、客户状态、客户负责人', problem: '客户资料是订单、合同、应收的源头，原系统只做基础档案。', delivery: '保留客户单位功能，增加与订单、合同、应收、授信的联动。', confirmPoints: ['客户主档字段是否满足销售建档', '客户状态是否影响下单', '客户负责人是否参与数据权限'] }),
  spec({ key: 'customer-category', module: 'customer', title: '客户分类', routePath: '/customer/category', icon: 'lucide:tags', phase: 'P0', oldMenu: '客户分类', buildMode: '保留增强', objective: '建立客户分类、等级和授信默认值。', featureText: '客户分类、A/B客户等级、默认授信额度、分类统计', problem: '客户分类不能支撑 A/B 授信和风控。', delivery: '分类继续保留，客户等级单独用于授信控制。', confirmPoints: ['A/B 客户等级是否足够', '默认授信额度是否按分类带入', '分类统计是否纳入管理看板'] }),
  spec({ key: 'customer-credit', module: 'customer', title: '客户授信与实时应收', routePath: '/customer/credit', icon: 'lucide:shield-alert', phase: 'P0', oldMenu: '客户单位/应收账款', buildMode: '新增改造', objective: '把客户授信、订单确认和费用变更联动起来。', featureText: '授信额度、已占用额度、可用额度、超限预警、超限审批', problem: '财务无法实时监控累计应收，客户并发多个团时有资金风险。', delivery: 'A 类/B 类额度可配置；订单确认和费用变更实时占用额度。', confirmPoints: ['订单确认是否立即占用授信', '超限时拦截还是转审批', '收款后是否自动释放额度'], scenarioRows: p0Rows['customer-credit'] }),
  spec({ key: 'customer-subject', module: 'customer', title: '正式主体确认', routePath: '/customer/subject', icon: 'lucide:badge-check', phase: 'P0', oldMenu: '客户单位/合同管理', buildMode: '新增改造', objective: '区分客户下单简称和合同/发票正式主体。', featureText: '原始下单名称、业务别名、正式法律主体、开票主体、结算主体、主体确认、证明附件', problem: '客户发单常用简称，但合同、发票、应收必须归到正式法律主体。', delivery: '别名只能作为业务线索；合同、发票、授信、应收只能使用已确认正式主体。', confirmPoints: ['正式主体确认责任人', '开票主体和结算主体是否可不同', '未确认主体是否允许下单'] }),
  spec({ key: 'customer-contract', module: 'customer', title: '客户销售合同管理', routePath: '/customer/contract', icon: 'lucide:file-signature', phase: 'P0', oldMenu: '客户销售合同管理', buildMode: '保留增强', objective: '保留下游客户销售合作合同台账并参与下单校验。', featureText: '客户合同台账、合同有效期、结款方式、合同文件、客户合同主体、合同模板、到期提醒、下单合同校验', problem: '业务录单/下单时必须识别客户合同是否有效。', delivery: '合同主体必须来自客户正式主体；合同到期或未签有效合同时提醒、拦截或转审批。', confirmPoints: ['合同到期提前提醒天数', '无有效合同时是否强制审批', '合同结款方式是否影响应收账期'] }),
  spec({ key: 'customer-product-auth', module: 'customer', title: '客户可售产品授权', routePath: '/customer/product-auth', icon: 'lucide:key-round', phase: 'P1', oldMenu: '产品授权', buildMode: '保留增强', objective: '按客户控制可下单产品范围。', featureText: '客户授权产品、授权期限、授权状态、可下单产品范围', problem: '产品授权现有意义不够清晰，需和订单权限联动。', delivery: '客户只能在授权产品范围内下单或录单。', confirmPoints: ['授权按产品还是线路类型', '授权到期后订单如何处理', '是否支持临时授权'] }),
  spec({ key: 'purchase-resource', module: 'purchase', title: '资源总览', routePath: '/purchase/resource', icon: 'lucide:database', phase: 'P1', oldMenu: '资源总览/酒店资源/景区资源', buildMode: '保留增强', objective: '沉淀酒店、景区、餐厅、车队、购物、地接等资源基础库。', featureText: '资源档案、城市区域、标准等级、供应商、采购价、协议价、价格有效期', problem: '业务下单和计调排团前需要稳定的可选资源和价格基础资料。', delivery: '采购管理负责资源和供应商价格基础资料，不直接承担每日库存维护。', confirmPoints: ['资源分类是否覆盖现有业务', '采购价是否允许多供应商多有效期', '资源状态是否影响计调选择'] }),
  spec({ key: 'purchase-supplier', module: 'purchase', title: '供应商管理', routePath: '/purchase/supplier', icon: 'lucide:truck', phase: 'P1', oldMenu: '供应商管理', buildMode: '保留增强', objective: '升级供应商档案，支撑资源推荐和结算。', featureText: '供应商分类、结算方式、联系人、协议、评价、状态', problem: '供应商只做台账，不能支撑后续资源推荐和结算。', delivery: '供应商要能关联资源、合同、应付和评价。', confirmPoints: ['供应商分类维度', '结算方式字段', '评价是否影响推荐排序'] }),
  spec({ key: 'purchase-relation', module: 'purchase', title: '采购关系管理', routePath: '/purchase/relation', icon: 'lucide:link', phase: 'P1', oldMenu: '采购关系管理', buildMode: '保留增强', objective: '维护资源与供应商之间的价格和优先级关系。', featureText: '资源绑定供应商、采购价、价格有效期、价格历史、优先级', problem: '酒店/景区等资源和不同供应商关系不清。', delivery: '采购关系要支撑计调选择和成本测算。', confirmPoints: ['一个资源是否允许多个供应商', '价格历史保留周期', '计调选择是否默认优先供应商'] }),
  spec({ key: 'purchase-contract', module: 'purchase', title: '供应商采购合同管理', routePath: '/purchase/contract', icon: 'lucide:file-check', phase: 'P1', oldMenu: '供应商采购合同管理', buildMode: '保留增强', objective: '保留上游采购合作合同，并参与采购价、应付和付款审核。', featureText: '供应商合同台账、合同期限、采购价格、结算条款、合同附件、合同文件、到期提醒', problem: '供应商合同是资源采购价、应付结算和付款依据。', delivery: '合同条款参与采购价格、资源调用、应付结算和付款审核。', confirmPoints: ['合同到期提醒规则', '合同价和临时报价优先级', '付款审核是否必须关联合同'] }),
  spec({ key: 'purchase-vehicle', module: 'purchase', title: '车辆资源与排班', routePath: '/purchase/vehicle', icon: 'lucide:car', phase: 'P1', oldMenu: '车辆管理', buildMode: '保留增强', objective: '把车辆档案升级为车辆资源日历和排班表。', featureText: '车辆档案、车队供应商、车型、座位数、司机、车辆日历、占用/维修/停用状态、基础报价', problem: '计调用车时需要看到每辆车在哪些日期已被哪些团队占用，避免重复派车和车辆冲突。', delivery: '计调安排用车后自动占用车辆日期，取消或变更团队后释放/调整车辆排班。', confirmPoints: ['车辆是否按具体车牌排班', '维修/停用状态维护人', '基础价格是否进入车调询价'] }),
  spec({ key: 'purchase-ground-agent', module: 'purchase', title: '地接外委管理', routePath: '/purchase/ground-agent', icon: 'lucide:handshake', phase: 'P1', oldMenu: '供应商管理/采购关系', buildMode: '新增', objective: '补齐黄山、千岛湖等外委地接闭环。', featureText: '地接社档案、外委任务、行程要求、总预算、确认单上传', problem: '异地外委地接缺少任务、确认单和财务审核闭环。', delivery: '地接反馈总成本确认单，财务按确认单审核。', confirmPoints: ['外委任务由谁发起', '确认单格式是否统一', '外委成本是否直接生成应付'] }),
  spec({ key: 'sales-product', module: 'sales', title: '产品资料与模板', routePath: '/sales/product', icon: 'lucide:package', phase: 'P0', oldMenu: '产品管理', buildMode: '保留增强', objective: '保留产品管理，增强行程、住宿、餐标和报价模板。', featureText: '产品资料、行程模板、住宿标准、餐标、用车标准、报价模板', problem: '创建团队重复填写行程、住宿、餐标。', delivery: '保留产品管理，增强模板能力。', confirmPoints: ['产品模板字段是否满足常用线路', '模板是否支持复制生成团队', '报价模板是否按客户类型区分'] }),
  spec({ key: 'sales-schedule', module: 'sales', title: '团期管理', routePath: '/sales/schedule', icon: 'lucide:calendar-plus', phase: 'P0', oldMenu: '产品管理/团期管理', buildMode: '保留增强', objective: '明确产品-团期-团队主线。', featureText: '团期计划、发团日期、价格、预控人数、停售、团期预警', problem: '产品创建后团期与团队衔接不够清楚。', delivery: '保留产品-团期-团队主线。', confirmPoints: ['团期是否可批量生成', '预控人数是否影响下单', '停售后是否允许内部录单'] }),
  spec({ key: 'sales-team', module: 'sales', title: '团队管理', routePath: '/sales/team', icon: 'lucide:users-round', phase: 'P0', oldMenu: '团队管理', buildMode: '保留增强', objective: '把团队作为销售、计调、财务贯穿的核心执行对象。', featureText: '散拼、整团、散团、单项、团队状态、复制团队、取消团队', problem: '团队是核心对象，但现有页面偏操作堆叠。', delivery: '团队继续作为系统核心执行对象。', confirmPoints: ['团队状态机是否符合现场流程', '取消团队是否释放资源和授信', '复制团队是否复制价格和资源'], scenarioRows: p0Rows['sales-team'] }),
  spec({ key: 'sales-order', module: 'sales', title: '订单管理', routePath: '/sales/order', icon: 'lucide:clipboard-list', phase: 'P0', oldMenu: '订单管理', buildMode: '保留增强', objective: '订单确认后联动团队人数、应收和客户授信。', featureText: '散拼订单、整团订单、子订单、订单状态、订单附件、订单确认件', problem: '订单与团队、应收、人数、确认件联动不足。', delivery: '订单确认后自动生成应收并占用授信。', confirmPoints: ['订单确认是否必须上传确认件', '子订单是否独立生成应收', '订单取消是否释放授信'], scenarioRows: p0Rows['sales-order'] }),
  spec({ key: 'sales-group-booking', module: 'sales', title: '散拼团队预订', routePath: '/sales/group-booking', icon: 'lucide:calendar-clock', phase: 'P0', oldMenu: '团队管理/散拼预订', buildMode: '保留增强', objective: '支撑散拼团队预订、占位和转订单。', featureText: '散拼团队、预订人数、占位、转订单、余位预警', problem: '散拼预订和正式订单之间需要稳定衔接。', delivery: '散拼预订进入订单确认后生成正式订单和应收。', confirmPoints: ['预订保留时长', '占位是否占库存', '预订转订单字段'] }),
  spec({ key: 'sales-combine-order', module: 'sales', title: '拼团订单', routePath: '/sales/combine-order', icon: 'lucide:merge', phase: 'P0', oldMenu: '拼团订单', buildMode: '保留增强', objective: '保留多订单拼入同一团队执行能力。', featureText: '未拼团、已拼团、按团展示、拼团操作、转团、共车关系', problem: '多个批发商订单需要拼到同一团队执行。', delivery: '拼团订单不能砍，需增强成本分摊能力。', confirmPoints: ['转团是否保留操作日志', '共车关系是否影响成本分摊', '拼团后客户应收是否独立'] }),
  spec({ key: 'sales-shared-car-cost', module: 'sales', title: '共车成本分摊', routePath: '/sales/shared-car-cost', icon: 'lucide:split', phase: 'P1', oldMenu: '拼团订单/团队管理', buildMode: '新增', objective: '处理多个订单共用一辆车的成本分摊。', featureText: '共车组、分摊方案、人数占比、手动调整、分摊日志', problem: '多个订单共用一辆车时成本需要合理拆分。', delivery: '默认按有效人数占比分摊，特殊情况可手动调整并留痕。', confirmPoints: ['默认分摊口径', '手动调整审批', '分摊日志查看范围'] }),
  spec({ key: 'sales-expense-change', module: 'sales', title: '订单费用变更', routePath: '/sales/expense-change', icon: 'lucide:diff', phase: 'P0', oldMenu: '订单费用变更', buildMode: '保留增强', objective: '费用变更同步影响应收、授信和利润统计。', featureText: '费用说明、加收、退减、补房差、增加景点、变更审核、应收刷新', problem: '费用变更只做台账，和应收/利润联动不够。', delivery: '费用变更必须同步影响订单应收、客户授信和利润统计。', confirmPoints: ['哪些费用变更需要审批', '退减是否释放授信', '变更是否更新利润快照'] }),
  spec({ key: 'sales-e-contract', module: 'sales', title: '电子合同', routePath: '/sales/e-contract', icon: 'lucide:file-pen', phase: 'P1', oldMenu: '电子合同', buildMode: '保留增强', objective: '保留电子合同台账并接入正式主体和合同模板。', featureText: '电子合同台账、合同编号、团队、产品、游客名单、PDF查看、签署状态', problem: '电子合同现有功能不能丢，需与主体确认、模板打通。', delivery: '电子合同只能使用已确认正式客户主体。', confirmPoints: ['签署状态来源', '合同模板调用规则', '游客名单变更是否重签'] }),
  spec({ key: 'sales-ticket-booking', module: 'sales', title: '票务系统下单', routePath: '/sales/ticket-booking', icon: 'lucide:ticket-check', phase: 'P1', oldMenu: '团队管理/景区门票预订', buildMode: '新增改造', objective: '从团队景区节点生成票务模板或接口下单。', featureText: '团队选择景区、使用日期、票种、游客名单、Excel 游客信息模板、票务系统接口下单、预约状态', problem: '门票现在由业务在团队页面安排，需要打通不同景区/票务系统。', delivery: '先做不同票务系统 Excel 模板生成，有接口条件的再直接调用票务系统下单。', confirmPoints: ['优先对接哪些景区', 'Excel 模板格式是否固定', '预约失败如何回退'] }),
  spec({ key: 'sales-tourist', module: 'sales', title: '游客信息中心', routePath: '/sales/tourist', icon: 'lucide:user-check', phase: 'P1', oldMenu: '客人信息/游客信息批量导入', buildMode: '保留增强', objective: '把游客名单、导入、OCR、查重统一在游客信息中心。', featureText: '游客资料、证件信息、年龄识别、有效人数标记、特殊票种、Excel导入、图片/OCR识别、文本粘贴解析、导入预览、字段校验、名单查重', problem: '游客名单来源分散，人工录入效率低且容易录错漏录。', delivery: '支持 Excel 导入、图片/OCR 识别、文本粘贴解析，确认后写入订单/团队游客名单。', confirmPoints: ['游客必填字段', '有效人数规则', '重复参团提醒规则'] }),
  spec({ key: 'sales-name-check', module: 'sales', title: '名单查重', routePath: '/sales/name-check', icon: 'lucide:search-check', phase: 'P1', oldMenu: '名单查重', buildMode: '保留增强', objective: '在游客导入和订单确认前发现重复名单。', featureText: '证件查重、姓名手机号查重、历史参团提醒、异常名单标记', problem: '游客重复、证件错误会影响门票预约、电子合同和统计。', delivery: '名单查重并入游客信息中心，同时保留独立查询入口。', confirmPoints: ['查重字段优先级', '重复是否拦截', '历史参团显示范围'] }),
  spec({ key: 'sales-ai-service', module: 'sales', title: '企微AI客服与工单', routePath: '/sales/ai-service', icon: 'lucide:bot', phase: 'P2', oldMenu: '企业微信客户群/AI客服', buildMode: '新增', objective: '把企业微信群需求识别为工单草稿和常见问题回复。', featureText: '企业微信群消息接入、客户需求识别、自动生成需求工单、常见问题回复、人工接管、遗漏提醒', problem: '客户需求散落在企业微信群，人工容易漏看漏记。', delivery: 'AI 生成内容需人工确认后进入正式订单/报价流程，避免误下单。', confirmPoints: ['企微接入范围', 'AI 回复是否必须人工确认', '工单如何转订单'] }),
  spec({ key: 'sales-smart-quote', module: 'sales', title: '智能行程与报价', routePath: '/sales/smart-quote', icon: 'lucide:sparkles', phase: 'P2', oldMenu: '产品管理/智能报价', buildMode: '新增', objective: '按客户需求自动生成行程和报价方案。', featureText: '客户需求输入、线路资料库匹配、模块化行程调用、报价规则、报价模板配置、方案生成、标准化文件导出、人工微调', problem: '人工翻历史线路和手工报价效率低且报价口径不统一。', delivery: '业务人员确认后发送客户。', confirmPoints: ['报价模板口径', '人工微调留痕', '输出文件格式'] }),
  spec({ key: 'sales-knowledge-base', module: 'sales', title: 'AI知识库与线路资料库', routePath: '/sales/knowledge-base', icon: 'lucide:library', phase: 'P2', oldMenu: '历史对话/线路资料库', buildMode: '新增', objective: '为 AI 客服和智能报价提供审核后的结构化知识来源。', featureText: '历史企业微信对话整理、常见问答、产品报价知识、发团时间、线路行程、酒店/景区/用车资料、标签分类、知识库更新审核', problem: '十几年线路资料如果不整理，AI 无法稳定复用。', delivery: '先导入历史线路资料和常见问题，由业务审核后发布。', confirmPoints: ['知识库审核责任人', '过期资料下线规则', 'AI 是否可引用未审核资料'] }),
  spec({ key: 'dispatch-team-arrange', module: 'dispatch', title: '团队安排总控台', routePath: '/dispatch/team-arrange', icon: 'lucide:layout-list', phase: 'P0', oldMenu: '团队安排', buildMode: '保留增强', objective: '集中查看每个团导游、住宿、用车、门票、餐饮、地接等资源安排进度。', featureText: '团队资源进度看板、导/交/住/车/景/餐/自/购/地状态、资源安排入口、预算成本汇总、异常提醒', problem: '计调安排分散到多个重复入口，成本和异常难统一。', delivery: '升级为执行总控台；资源安排产生预算成本，后续进入审核和结算。', confirmPoints: ['资源安排分项是否完整', '预算成本是否实时汇总', '异常提醒责任人'], scenarioRows: p0Rows['dispatch-team-arrange'] }),
  spec({ key: 'dispatch-team-audit', module: 'dispatch', title: '计调团队审核', routePath: '/dispatch/team-audit', icon: 'lucide:check-circle', phase: 'P0', oldMenu: '团队审核', buildMode: '保留增强', objective: '统一审核团队安排、资源成本和异常退回。', featureText: '团队安排核对、资源成本核对、异常退回、审核意见', problem: '计调审核需要统一流转和留痕。', delivery: '纳入统一审核流程。', confirmPoints: ['审核节点和责任人', '退回后是否保留意见', '审核通过后是否进入财务审核'] }),
  spec({ key: 'dispatch-guide-schedule', module: 'dispatch', title: '导游排班汇总', routePath: '/dispatch/guide-schedule', icon: 'lucide:calendar-days', phase: 'P1', oldMenu: '导游排班汇总', buildMode: '保留增强', objective: '把导游日历、团队任务、冲突提醒和报账状态联动。', featureText: '导游日历、团队任务、冲突提醒、报账状态', problem: '导游安排和报账状态需要联动。', delivery: '导游排班与团队安排、导游报账打通。', confirmPoints: ['导游冲突判断规则', '排班日历查看粒度', '报账状态是否展示在排班中'] }),
  spec({ key: 'dispatch-transfer-info', module: 'dispatch', title: '接送信息', routePath: '/dispatch/transfer-info', icon: 'lucide:bus', phase: 'P1', oldMenu: '接送信息', buildMode: '保留增强', objective: '统一管理团队接送站点、时间、车次和通知状态。', featureText: '接送站、车次航班、接送时间、导游司机、通知状态', problem: '接送信息分散记录，容易漏通知。', delivery: '接送信息和团队安排、车辆、导游任务联动。', confirmPoints: ['接送站基础资料', '通知对象', '变更提醒规则'] }),
  spec({ key: 'dispatch-guide-expense', module: 'dispatch', title: '导游报账流程', routePath: '/dispatch/guide-expense', icon: 'lucide:receipt-text', phase: 'P0', oldMenu: '团队安排/导游报账', buildMode: '新增改造', objective: '围绕排团-导游报账-计调审核-财务审核四状态升级。', featureText: '导游上传支款凭证、发票、报账明细、计调初审、财务复审', problem: '导游报账和凭证核对依赖人工。', delivery: '导游报账进入计调初审和财务复审闭环。', confirmPoints: ['导游上传端形态', '凭证必传规则', '计调初审和财务复审差异'] }),
  spec({ key: 'dispatch-car-inquiry', module: 'dispatch', title: '车调询价与派车', routePath: '/dispatch/car-inquiry', icon: 'lucide:route', phase: 'P0', oldMenu: '团队管理/用车/车调询价', buildMode: '新增改造', objective: '建立统一询价、比价、确认派车和成本回写流程。', featureText: '按团队行程发起用车询价、行程公里数、车型需求、用车日期、基础价格带入、车队报价、多家比价、选定车队/车辆/司机、派车确认、成本回写、询价记录', problem: '车价靠人工问车队和经验判断，缺少统一询价、比价、留痕。', delivery: '把确认车价写入团队成本、应付和财务审核。', confirmPoints: ['询价供应商范围', '车队报价有效期', '确认派车后是否锁定成本'] }),
  spec({ key: 'finance-team-audit', module: 'finance', title: '财务团队审核', routePath: '/finance/team-audit', icon: 'lucide:badge-check', phase: 'P0', oldMenu: '团队审核', buildMode: '保留增强', objective: '审核团队应收、应付、成本、利润、报账和异常项。', featureText: '团队应收、应付、成本、利润、报账、异常项审核', problem: '财务只在后期审账，前置风控不足。', delivery: '保留财务团队审核，并提前可见风险。', confirmPoints: ['财务审核前置到哪个节点', '哪些异常可退回计调', '审核通过后生成哪些账款'], scenarioRows: p0Rows['finance-team-audit'] }),
  spec({ key: 'finance-prepay', module: 'finance', title: '团队预付款', routePath: '/finance/prepay', icon: 'lucide:banknote', phase: 'P0', oldMenu: '团队预付款', buildMode: '保留增强', objective: '与导游备用金一起纳入预算申请和审批闭环。', featureText: '按团队预算申请预付款、计调提交、财务审批、付款凭证、核销退补', problem: '团队预付款和备用金拆开会重复，缺少闭环。', delivery: '纳入备用金闭环管理，更新团队成本和财务结算状态。', confirmPoints: ['预付款和备用金是否合并审批', '付款凭证格式', '核销差额处理'] }),
  spec({ key: 'finance-bank-cash', module: 'finance', title: '银行现金账', routePath: '/finance/bank-cash', icon: 'lucide:landmark', phase: 'P1', oldMenu: '银行现金账', buildMode: '保留增强', objective: '保留账户流水、收支记录和账户余额管理。', featureText: '账户流水、收支记录、账户余额、流水关联单据', problem: '资金流水需要继续保留。', delivery: '与收款、付款、冲抵联动。', confirmPoints: ['是否导入银行流水', '流水和单据匹配规则', '账户余额是否自动计算'] }),
  spec({ key: 'finance-receivable', module: 'finance', title: '实时应收管理', routePath: '/finance/receivable', icon: 'lucide:arrow-down-to-line', phase: 'P0', oldMenu: '应收账款', buildMode: '重构', objective: '订单确认即形成应收快照，收款后释放客户额度。', featureText: '客户应收、订单应收、团队应收、费用变更、授信占用', problem: '团队结束后 15 天才生成账单，应收不可见。', delivery: '订单确认即形成应收快照，收款后释放额度。', confirmPoints: ['应收生成时点', '账龄口径', '收款释放额度规则'], scenarioRows: p0Rows['finance-receivable'] }),
  spec({ key: 'finance-receivable-detail', module: 'finance', title: '应收账款明细', routePath: '/finance/receivable-detail', icon: 'lucide:list', phase: 'P0', oldMenu: '应收账款明细', buildMode: '保留增强', objective: '让每一笔应收都能追到订单/团队/费用变更。', featureText: '订单来源、费用变更来源、团队来源、收款抵扣、余额', problem: '应收款缺少业务来源追溯。', delivery: '每一笔应收都能追到订单/团队/费用变更。', confirmPoints: ['明细来源字段', '收款抵扣顺序', '余额是否实时计算'] }),
  spec({ key: 'finance-payable', module: 'finance', title: '应付管理', routePath: '/finance/payable', icon: 'lucide:arrow-up-from-line', phase: 'P1', oldMenu: '应付账款', buildMode: '保留增强', objective: '按供应商、资源、地接、导游归集应付成本。', featureText: '供应商应付、资源应付、地接应付、导游应付、未付提醒', problem: '应付成本需要按资源和供应商归集。', delivery: '应付来自资源安排和外委确认单。', confirmPoints: ['应付生成来源', '未付提醒规则', '供应商对账字段'] }),
  spec({ key: 'finance-payable-detail', module: 'finance', title: '应付账款明细', routePath: '/finance/payable-detail', icon: 'lucide:list-ordered', phase: 'P1', oldMenu: '应付账款明细', buildMode: '保留增强', objective: '解释每一笔成本来源。', featureText: '供应商、资源类型、团队、成本来源、付款状态', problem: '应付明细要能解释成本来源。', delivery: '关联资源安排、采购合同、付款记录。', confirmPoints: ['成本来源字段', '付款状态口径', '采购合同关联规则'] }),
  spec({ key: 'finance-cost-preview', module: 'finance', title: '预算成本与实际成本', routePath: '/finance/cost-preview', icon: 'lucide:eye', phase: 'P1', oldMenu: '成本明细预览', buildMode: '保留增强', objective: '过程化控制预算成本和实际成本偏差。', featureText: '预算成本、实际成本、差异金额、差异原因、异常标记', problem: '成本只在后期才看到，无法过程控制。', delivery: '计调安排形成预算成本，报账后形成实际成本。', confirmPoints: ['成本超预算阈值', '差异原因必填规则', '预算成本锁定时点'] }),
  spec({ key: 'finance-guide-advance', module: 'finance', title: '导游备用金闭环', routePath: '/finance/guide-advance', icon: 'lucide:hand-coins', phase: 'P0', oldMenu: '导游备用金/团队预付款', buildMode: '保留增强', objective: '建立预算申请-审批发放-支付宝付款-导游报账-计调初审-财务复审-核销退补闭环。', featureText: '预算申请、审批发放、支付宝付款、导游报账、计调初审、财务复审、核销、差额退补、偏差分析', problem: '备用金申请、发放、使用、凭证、报账和核销没有形成闭环。', delivery: '默认按支付宝方式发放备用金，核销后自动更新团队成本和财务结算状态。', confirmPoints: ['支付宝账号维护方式', '备用金审批节点', '差额退补流程'] }),
  spec({ key: 'finance-guide-settlement', module: 'finance', title: '导游结算', routePath: '/finance/guide-settlement', icon: 'lucide:calculator', phase: 'P0', oldMenu: '导游结算', buildMode: '保留增强', objective: '导游结算和报账、凭证、发票联动。', featureText: '导游收入、导游上交、现收现退、发票、结算确认', problem: '导游结算需要和报账、凭证、发票联动。', delivery: '财务复审后进入导游结算。', confirmPoints: ['导游收入来源', '现收现退字段', '结算确认责任人'] }),
  spec({ key: 'finance-shop-rebate', module: 'finance', title: '购物返佣', routePath: '/finance/shop-rebate', icon: 'lucide:percent', phase: 'P2', oldMenu: '购物返佣', buildMode: '保留增强', objective: '后续作为财务购物返佣统计和收款核对入口。', featureText: '购物点、返佣金额、导游、团队、结算状态', problem: '购物实际反馈先在单团审核里录入，财务后续需要批量统计和收款核对。', delivery: '本次不作为购物业绩录入口，后续统计页读取购物反馈和结算快照。', confirmPoints: ['返佣归属团队', '导游关联方式', '是否进入利润统计'] }),
  spec({ key: 'finance-income-record', module: 'finance', title: '收款记录', routePath: '/finance/income-record', icon: 'lucide:plus-circle', phase: 'P1', oldMenu: '收款记录', buildMode: '保留增强', objective: '收款登记和确认件金额匹配。', featureText: '收款登记、确认件上传、金额匹配、差异提醒、人工确认', problem: '应收录入和对方确认件分离，系统无法自动匹配金额。', delivery: '先做上传和金额校验，OCR 后置。', confirmPoints: ['确认件必传规则', '金额差异处理', '收款抵扣顺序'] }),
  spec({ key: 'finance-payment-record', module: 'finance', title: '付款记录', routePath: '/finance/payment-record', icon: 'lucide:minus-circle', phase: 'P1', oldMenu: '付款记录', buildMode: '保留增强', objective: '付款记录关联应付来源、供应商、导游和地接。', featureText: '付款申请、付款登记、供应商/导游/地接关联、付款状态', problem: '付款需要和应付、发票、供应商结算联动。', delivery: '保留付款记录并关联应付来源。', confirmPoints: ['付款申请是否先审批', '付款对象类型', '付款后是否更新应付余额'] }),
  spec({ key: 'finance-invoice', module: 'finance', title: '发票记录', routePath: '/finance/invoice', icon: 'lucide:file-text', phase: 'P1', oldMenu: '发票记录', buildMode: '保留增强', objective: '销项/进项发票和应收应付、报账金额校验。', featureText: '销项发票、进项发票、发票金额校验、发票附件', problem: '发票和应收应付、报账之间缺少校验。', delivery: '发票金额与应收/应付/报账金额校验。', confirmPoints: ['发票类型字段', '金额校验规则', '附件格式'] }),
  spec({ key: 'finance-payment-invoice', module: 'finance', title: '付款发票明细', routePath: '/finance/payment-invoice', icon: 'lucide:file-stack', phase: 'P1', oldMenu: '付款发票明细', buildMode: '保留增强', objective: '追踪供应商付款发票和票据状态。', featureText: '付款发票、供应商、团队、资源费用、票据状态', problem: '供应商付款发票需要继续追踪。', delivery: '保留并纳入票据核验。', confirmPoints: ['票据状态枚举', '发票和付款绑定规则', '缺票提醒'] }),
  spec({ key: 'finance-payment-progress', module: 'finance', title: '财务收支进度', routePath: '/finance/payment-progress', icon: 'lucide:bar-chart-3', phase: 'P1', oldMenu: '财务收支进度', buildMode: '保留增强', objective: '让老板和财务实时看到团队资金进度。', featureText: '团队收支进度、应收回款、应付付款、结算完成度', problem: '老板和财务需要看团队资金进度。', delivery: '保留并联动实时应收应付。', confirmPoints: ['进度口径', '管理层看板字段', '异常团队提醒'] }),
  spec({ key: 'finance-offset', module: 'finance', title: '应收应付冲抵', routePath: '/finance/offset', icon: 'lucide:arrow-left-right', phase: 'P2', oldMenu: '应收应付冲抵', buildMode: '保留增强', objective: '保留客户/供应商往来冲抵能力。', featureText: '客户/供应商往来冲抵、冲抵审批、冲抵记录', problem: '往来冲抵需要继续保留并管控。', delivery: '保留原功能，增加审批和留痕。', confirmPoints: ['冲抵审批规则', '往来对象匹配规则', '冲抵后账款展示'] }),
  spec({ key: 'statistics-team-progress', module: 'statistics', title: '团队进度统计', routePath: '/statistics/team-progress', icon: 'lucide:activity', phase: 'P1', oldMenu: '团队进度统计', buildMode: '保留增强', objective: '按统一状态机统计排团、报账、计调审核和财务审核进度。', featureText: '排团、导游报账、计调审核、财务审核进度', problem: '团队进度需要统一状态口径。', delivery: '按统一状态机统计。', confirmPoints: ['团队状态口径', '异常状态展示', '管理层筛选维度'] }),
  spec({ key: 'statistics-reception', module: 'statistics', title: '收客统计', routePath: '/statistics/reception', icon: 'lucide:user-plus', phase: 'P0', oldMenu: '收客统计', buildMode: '保留增强', objective: '统一收客人数和有效人数口径。', featureText: '收客人数、有效人数、成人儿童、渠道、客户', problem: '有效人数统计失真影响绩效。', delivery: '只把成人/儿童计入有效人数，房差和附加项不计人头。', confirmPoints: ['有效人数口径', '渠道统计口径', '绩效是否引用该口径'], scenarioRows: p0Rows['statistics-reception'] }),
  spec({ key: 'statistics-profit', module: 'statistics', title: '利润统计', routePath: '/statistics/profit', icon: 'lucide:trending-up', phase: 'P1', oldMenu: '利润统计', buildMode: '保留增强', objective: '统一订单收入、费用变更、资源成本、分摊成本、返佣和利润口径。', featureText: '订单收入、费用变更、资源成本、分摊成本、返佣、利润', problem: '利润统计需联动费用变更和成本分摊。', delivery: '利润口径统一配置。', confirmPoints: ['利润口径配置', '返佣是否计入利润', '成本分摊引用来源'] }),
  spec({ key: 'statistics-resource-purchase', module: 'statistics', title: '资源采购统计', routePath: '/statistics/resource-purchase', icon: 'lucide:package-search', phase: 'P1', oldMenu: '资源采购统计-排团', buildMode: '保留增强', objective: '按团队、资源、供应商统计采购使用。', featureText: '按团队、资源、供应商统计采购使用', problem: '资源采购数据需要归集。', delivery: '保留原维度并统一资源口径。', confirmPoints: ['资源统计维度', '供应商统计维度', '是否用于采购谈判'] }),
  spec({ key: 'statistics-account', module: 'statistics', title: '账款统计', routePath: '/statistics/account', icon: 'lucide:book-open', phase: 'P1', oldMenu: '账款统计', buildMode: '保留增强', objective: '统计客户应收、供应商应付、账龄、授信占用和欠款风险。', featureText: '客户应收、供应商应付、账龄、授信占用、欠款风险', problem: '账款统计不能只靠事后报表。', delivery: '联动实时应收和授信。', confirmPoints: ['账龄分组', '授信占用展示', '欠款风险标记'] }),
  spec({ key: 'statistics-guide-stat', module: 'statistics', title: '导游统计', routePath: '/statistics/guide-stat', icon: 'lucide:compass', phase: 'P1', oldMenu: '导游统计', buildMode: '保留增强', objective: '统计导游排班、报账、结算、异常和评价。', featureText: '排班、报账、结算、异常、评价', problem: '导游是独立结算主体。', delivery: '联动导游管理和导游结算。', confirmPoints: ['导游绩效口径', '异常统计字段', '评价是否纳入统计'] }),
  spec({ key: 'statistics-income-summary', module: 'statistics', title: '收款汇总统计', routePath: '/statistics/income-summary', icon: 'lucide:arrow-down-circle', phase: 'P1', oldMenu: '收款汇总统计', buildMode: '保留增强', objective: '按客户、团队、订单、业务员、日期统计收款。', featureText: '按客户、团队、订单、业务员、日期统计收款', problem: '回款数据需要持续统计。', delivery: '保留并联动收款确认件。', confirmPoints: ['收款统计维度', '业务员归属规则', '确认件是否影响统计'] }),
  spec({ key: 'statistics-payment-summary', module: 'statistics', title: '付款汇总统计', routePath: '/statistics/payment-summary', icon: 'lucide:arrow-up-circle', phase: 'P1', oldMenu: '付款汇总统计', buildMode: '保留增强', objective: '按供应商、资源、团队、日期统计付款。', featureText: '按供应商、资源、团队、日期统计付款', problem: '付款数据需要持续统计。', delivery: '保留并联动应付明细。', confirmPoints: ['付款统计维度', '资源成本归属', '供应商对账导出'] }),
  spec({ key: 'statistics-report-center', module: 'statistics', title: '报表中心', routePath: '/statistics/report-center', icon: 'lucide:file-bar-chart', phase: 'P1', oldMenu: '报表中心', buildMode: '保留增强', objective: '集中管理经营分析和财务报表。', featureText: '经营汇总、团队报表、财务报表、导出和权限控制', problem: '管理层需要统一入口查看汇总报表。', delivery: '保留报表中心并接入统一指标口径。', confirmPoints: ['首批报表清单', '导出格式', '报表权限'] }),
  spec({ key: 'enterprise-bank-account', module: 'enterprise', title: '银行账号', routePath: '/enterprise/bank-account', icon: 'lucide:landmark', phase: 'P1', oldMenu: '银行账号', buildMode: '保留增强', objective: '统一管理企业收付款账号。', featureText: '收款账号、付款账号、账户状态、使用范围', problem: '企业收付款账号需要统一管理。', delivery: '与收款、付款、发票联动。', confirmPoints: ['账号用途', '停用账号处理', '默认收付款账号'] }),
  spec({ key: 'enterprise-department', module: 'enterprise', title: '部门管理', routePath: '/enterprise/department', icon: 'lucide:network', phase: 'P1', oldMenu: '部门管理', buildMode: '保留增强', objective: '提供权限和数据范围组织基础。', featureText: '部门、负责人、数据范围、业务归属', problem: '权限和数据范围需要组织基础。', delivery: '与角色、员工、数据权限联动。', confirmPoints: ['部门层级', '负责人权限', '数据范围规则'] }),
  spec({ key: 'enterprise-role', module: 'enterprise', title: '角色权限', routePath: '/enterprise/role', icon: 'lucide:shield', phase: 'P0', oldMenu: '角色管理', buildMode: '重构', objective: '角色不能只控菜单，要控按钮、数据范围和审批权限。', featureText: '角色模板、菜单权限、按钮权限、数据范围权限、审批权限', problem: '现有角色易设错，跨部门权限混乱。', delivery: '不能只控菜单，要控按钮和数据范围。', confirmPoints: ['角色模板清单', '按钮权限范围', '数据范围口径'] }),
  spec({ key: 'enterprise-employee', module: 'enterprise', title: '员工管理', routePath: '/enterprise/employee', icon: 'lucide:users', phase: 'P0', oldMenu: '员工管理', buildMode: '保留增强', objective: '新增员工必须按岗位模板授权。', featureText: '员工档案、岗位、角色、部门、状态、权限校验', problem: '新增员工时角色易设错。', delivery: '新增员工必须按岗位模板授权。', confirmPoints: ['岗位模板', '离职员工处理', '权限校验节点'] }),
  spec({ key: 'enterprise-guide', module: 'enterprise', title: '导游管理', routePath: '/enterprise/guide', icon: 'lucide:map-pin', phase: 'P1', oldMenu: '导游管理', buildMode: '保留增强', objective: '导游资料、排班、结算和评价的基础资料。', featureText: '导游资料、联系方式、证件、排班、结算、评价', problem: '导游安排和结算需要基础资料。', delivery: '联动导游排班和导游结算。', confirmPoints: ['导游证件字段', '结算账号', '评价是否影响排班'] }),
  spec({ key: 'enterprise-contract-template', module: 'enterprise', title: '合同模板管理', routePath: '/enterprise/contract-template', icon: 'lucide:file-text', phase: 'P1', oldMenu: '合同模板管理', buildMode: '保留增强', objective: '明确客户合同、采购合同、旅游合同和电子合同模板调用位置。', featureText: '客户合同模板、采购合同模板、旅游合同模板、打印/导出模板', problem: '合同模板不能丢，需明确在哪里调用。', delivery: '合同模板与客户合同、采购合同、电子合同打通。', confirmPoints: ['模板类型', '变量字段', '模板更新审核'] }),
  spec({ key: 'enterprise-expense-item', module: 'enterprise', title: '费用项目', routePath: '/enterprise/expense-item', icon: 'lucide:receipt', phase: 'P1', oldMenu: '费用项目', buildMode: '保留增强', objective: '统一订单、计调、财务使用的费用项目。', featureText: '收入费用项、成本费用项、费用类型、启停状态、财务科目映射', problem: '费用项目不统一会影响报价、报账和利润统计。', delivery: '费用项目作为订单费用、成本明细和统计口径基础。', confirmPoints: ['费用项目分类', '是否映射财务科目', '停用项目处理'] }),
  spec({ key: 'enterprise-station', module: 'enterprise', title: '接送站管理', routePath: '/enterprise/station', icon: 'lucide:train', phase: 'P1', oldMenu: '接送站', buildMode: '保留增强', objective: '统一接送站点基础资料。', featureText: '站点名称、城市、集合点、默认提前时间、负责人', problem: '接送信息需要稳定站点基础资料。', delivery: '接送站与接送信息、团队安排联动。', confirmPoints: ['站点字段', '默认时间规则', '停用站点处理'] }),
  spec({ key: 'system-params', module: 'system', title: '业务参数中心', routePath: '/system/params', icon: 'lucide:sliders-horizontal', phase: 'P0', oldMenu: '系统参数配置', buildMode: '重构', objective: '把授信、审批、人数、车价、票种、财务口径配置化。', featureText: '授信额度、审批规则、人数口径、车价参数、票种规则、财务口径', problem: '很多规则散落在人工经验中。', delivery: '规则尽量配置化，不写死在代码里。', confirmPoints: ['首批参数清单', '参数修改权限', '参数生效时间'] }),
  spec({ key: 'system-e-contract-config', module: 'system', title: '电子合同配置', routePath: '/system/e-contract-config', icon: 'lucide:file-cog', phase: 'P1', oldMenu: '电子合同配置', buildMode: '保留增强', objective: '配置电子合同模板、签署规则和主体校验。', featureText: '电子合同模板、签署方式、游客名单、正式主体、签署状态同步', problem: '电子合同需与主体确认、游客名单、模板打通。', delivery: '电子合同配置服务于销售电子合同。', confirmPoints: ['签署平台', '模板变量', '状态同步规则'] }),
  spec({ key: 'system-approval-alert', module: 'system', title: '审批与预警配置', routePath: '/system/approval-alert', icon: 'lucide:bell-ring', phase: 'P0', oldMenu: '审批与预警配置', buildMode: '新增', objective: '配置超授信、备用金、报价、成本超预算、库存不足、人数异常等审批预警。', featureText: '超授信审批、备用金审批、报价审批、成本超预算、库存不足、人数异常', problem: '审批分散或缺失，风险靠人工发现。', delivery: '作为系统设置下的业务规则配置。', confirmPoints: ['审批流节点', '预警阈值', '通知对象'] }),
  spec({ key: 'system-message-notify', module: 'system', title: '消息通知配置', routePath: '/system/message-notify', icon: 'lucide:message-square', phase: 'P1', oldMenu: '消息通知配置', buildMode: '新增增强', objective: '配置站内消息与企业微信通知模板。', featureText: '审批提醒、预警提醒、任务提醒、企业微信通知模板', problem: '风险和任务靠人工沟通。', delivery: '先站内消息，再接企业微信。', confirmPoints: ['通知类型', '企微模板', '免打扰规则'] }),
  spec({ key: 'system-operation-log', module: 'system', title: '系统操作日志', routePath: '/system/operation-log', icon: 'lucide:scroll', phase: 'P0', oldMenu: '系统操作日志', buildMode: '保留增强', objective: '关键业务动作必须可追踪。', featureText: '登录、增删改、审批、金额变更、状态变更、转团拼团日志', problem: '资金、权限、订单变更都需要审计留痕。', delivery: '关键业务动作必须可追踪。', confirmPoints: ['日志保留时间', '金额变更字段', '日志查看权限'] }),
  spec({ key: 'system-team-log', module: 'system', title: '团号日志', routePath: '/system/team-log', icon: 'lucide:scroll-text', phase: 'P1', oldMenu: '团号日志', buildMode: '保留增强', objective: '把团号生成、团队变更和订单挂团纳入统一审计。', featureText: '团号生成、团队变更、订单挂团、转团、操作人、操作时间、变更前后内容', problem: '团队和订单变更属于系统审计内容，不应放在销售业务菜单里。', delivery: '从销售管理调整到系统设置，统一归入日志/审计类功能。', confirmPoints: ['团号生成规则', '转团日志字段', '销售是否可查看'] }),
  spec({ key: 'system-header-footer', module: 'system', title: '页眉页脚印章', routePath: '/system/header-footer', icon: 'lucide:stamp', phase: 'P1', oldMenu: '页眉页脚印章', buildMode: '保留增强', objective: '维护合同、报价、确认单等打印模板页眉页脚和印章。', featureText: '页眉、页脚、印章图片、模板调用范围、启停状态', problem: '打印输出需要统一企业形象。', delivery: '保留并服务于合同和确认单导出。', confirmPoints: ['印章权限', '模板调用范围', '图片规格'] }),
  spec({ key: 'system-style', module: 'system', title: '系统风格设置', routePath: '/system/style', icon: 'lucide:palette', phase: 'P1', oldMenu: '系统风格设置', buildMode: '保留增强', objective: '维护企业系统主题、菜单风格和打印风格。', featureText: '系统主题、菜单风格、登录页、打印样式', problem: '演示和长期使用需要统一企业识别。', delivery: '保留风格设置，默认采用专业后台风格。', confirmPoints: ['是否需要企业 Logo', '主题色', '登录页展示内容'] }),
  spec({ key: 'system-shortcut-menu', module: 'system', title: '快捷菜单设置', routePath: '/system/shortcut-menu', icon: 'lucide:zap', phase: 'P1', oldMenu: '快捷菜单设置', buildMode: '保留增强', objective: '为不同岗位配置常用快捷入口。', featureText: '岗位快捷入口、个人快捷入口、排序、启停', problem: '业务人员需要快速进入常用功能。', delivery: '保留快捷菜单设置，并支持角色默认配置。', confirmPoints: ['岗位默认快捷入口', '个人是否可改', '首页展示位置'] }),
];

export const phaseLabels: Record<PrototypePhase, string> = {
  P0: '一期主线',
  P1: '一期增强 / 二期候选',
  P2: '后续智能化规划',
};

export const phaseColors: Record<PrototypePhase, string> = {
  P0: 'red',
  P1: 'blue',
  P2: 'purple',
};

export const statusColors: Record<string, string> = {
  可下单: 'green',
  可审核: 'green',
  已生成: 'green',
  已确认: 'green',
  已结清: 'green',
  已通过: 'green',
  待审批: 'orange',
  待补齐: 'orange',
  待补凭证: 'orange',
  待催收: 'orange',
  待复核: 'orange',
  待确认: 'orange',
  部分收款: 'gold',
  审批中: 'blue',
  执行准备: 'blue',
  已排团: 'blue',
  需处理: 'red',
  需协调: 'red',
  拦截中: 'red',
};

export function getSpecsByModule(module: string) {
  return prototypeSpecs.filter((item) => item.module === module);
}

export function getSpecByKey(pageKey: string) {
  return prototypeSpecs.find((item) => item.key === pageKey) ?? prototypeSpecs[0]!;
}

function defaultRows(item: PrototypeSpec): PrototypeRecord[] {
  const baseNo = item.key.toUpperCase().replaceAll('-', '-').slice(0, 12);
  const isFinance = item.module === 'finance';
  const isStat = item.module === 'statistics';
  const isSystem = item.module === 'system' || item.module === 'enterprise';
  return [
    {
      amount: isFinance ? '¥86,400' : isStat ? '286 人 / ¥54,000' : isSystem ? '12 条规则' : '¥132,000',
      customer: isSystem ? '测试地接社' : '杭州远行国旅',
      date: '2026-05-18',
      id: `${baseNo}-001`,
      owner: isSystem ? '系统管理员' : item.module === 'dispatch' ? '计调 刘洋' : item.module === 'sales' ? '销售 张伟' : '负责人 周敏',
      progress: item.phase === 'P0' ? 86 : item.phase === 'P1' ? 68 : 42,
      risk: item.phase === 'P0' ? '关键确认项' : '待二次确认',
      stage: phaseLabels[item.phase],
      status: item.phase === 'P0' ? '待确认' : item.phase === 'P1' ? '已生成' : '规划中',
      team: item.module === 'customer' ? '客户主档' : item.module === 'purchase' ? '采购资源' : 'HZ20260518-003',
      title: item.objective,
    },
    {
      amount: isFinance ? '¥132,000' : isStat ? '同比 +18%' : isSystem ? '6 个模板' : '¥237,000',
      customer: isSystem ? '管理后台' : '上海春秋门店',
      date: '2026-05-19',
      id: `${baseNo}-002`,
      owner: isSystem ? '产品经理' : item.module === 'dispatch' ? '计调 陈晨' : item.module === 'sales' ? '销售 李娜' : '负责人 赵杰',
      progress: item.phase === 'P0' ? 74 : item.phase === 'P1' ? 55 : 35,
      risk: '字段待确认',
      stage: '样例数据',
      status: item.phase === 'P2' ? '规划中' : '待审批',
      team: item.module === 'purchase' ? '供应商/资源' : 'SH20260519-006',
      title: item.featureText,
    },
    {
      amount: isFinance ? '¥48,800' : isStat ? '异常 3 项' : isSystem ? '待配置' : '¥74,000',
      customer: isSystem ? '业务规则' : '南京研学中心',
      date: '2026-05-20',
      id: `${baseNo}-003`,
      owner: '管理层确认',
      progress: item.phase === 'P0' ? 60 : item.phase === 'P1' ? 44 : 25,
      risk: item.problem,
      stage: '确认点',
      status: item.phase === 'P0' ? '需处理' : '待确认',
      team: 'NJ20260520-002',
      title: item.delivery,
    },
  ];
}

export function getPrototypePage(pageKey: string): PrototypePageConfig {
  const item = getSpecByKey(pageKey);
  return {
    ...item,
    actions: item.phase === 'P0'
      ? ['新建/录入', '提交审核', '查看业务链路', '导出确认清单']
      : item.phase === 'P1'
        ? ['查看详情', '维护基础资料', '导出样例', '加入一期增强']
        : ['查看规划', '发起试点', '维护知识来源', '记录甲方意见'],
    columns: [
      { dataIndex: 'id', key: 'id', title: '编号', width: 140 },
      { dataIndex: 'title', key: 'title', title: '业务对象' },
      { dataIndex: 'team', key: 'team', title: '关联团队/对象', width: 150 },
      { dataIndex: 'customer', key: 'customer', title: '客户/主体', width: 150 },
      { dataIndex: 'amount', key: 'amount', title: '金额/指标', width: 150 },
      { dataIndex: 'stage', key: 'stage', title: '当前阶段', width: 140 },
      { dataIndex: 'progress', key: 'progress', title: '进度', width: 150 },
      { dataIndex: 'status', key: 'status', title: '状态', width: 110 },
      { key: 'action', title: '操作', width: 150 },
    ],
    filters: [
      { label: '关键词', placeholder: `搜索${item.title}/团队/客户`, type: 'input' },
      { label: '阶段', options: ['一期主线', '一期增强 / 二期候选', '后续智能化规划'], placeholder: phaseLabels[item.phase], type: 'select' },
      { label: '状态', options: ['待确认', '待审批', '已生成', '需处理'], placeholder: '全部状态', type: 'select' },
      { label: '负责人', placeholder: '销售/计调/财务/管理员', type: 'input' },
    ],
    metrics: item.metrics ?? [
      { label: '建设阶段', trend: item.buildMode, value: phaseLabels[item.phase] },
      { label: '确认事项', trend: '需甲方确认', value: `${item.confirmPoints.length} 项` },
      { label: '业务覆盖', trend: item.oldMenu, value: item.module === 'system' ? '规则配置' : '全链路联动' },
      { label: '交付方式', trend: '静态原型', value: '可演示' },
    ],
    rows: item.scenarioRows ?? defaultRows(item),
    steps: item.phase === 'P0'
      ? ['业务录入', '规则校验', '提交审核', '财务/管理确认', '生成统计']
      : item.phase === 'P1'
        ? ['基础资料维护', '业务引用', '异常提醒', '报表联动']
        : ['知识整理', '试点配置', '人工确认', '逐步上线'],
  };
}

export const coreDemoLinks = [
  { path: '/workspace', title: '业务工作台' },
  { path: '/customer/credit', title: '客户授信' },
  { path: '/sales/product', title: '产品管理' },
  { path: '/purchase/resource', title: '资源总览' },
  { path: '/enterprise/guide', title: '导游管理' },
];

export const workbenchAlerts = [
  { count: 7, label: '客户授信预警', path: '/customer/credit', status: 'P0' },
  { count: 5, label: '产品资料待完善', path: '/sales/product', status: 'P0' },
  { count: 4, label: '采购关系待维护', path: '/purchase/relation', status: 'P1' },
  { count: 6, label: '导游资料待补齐', path: '/enterprise/guide', status: 'P1' },
  { count: 12, label: '合同到期提醒', path: '/customer/contract', status: 'P1' },
];

export const deliveryScope = [
  { module: '客户管理', p0: 5, p1: 1, p2: 0, focus: '客户主体、授信、合同校验' },
  { module: '采购管理', p0: 0, p1: 4, p2: 0, focus: '资源、供应商、采购关系、合同' },
  { module: '销售管理', p0: 1, p1: 0, p2: 0, focus: '产品资料、行程、团队安排模板' },
  { module: '计调操作', p0: 0, p1: 1, p2: 0, focus: '用车报价、导游排班、导游请假' },
  { module: '企业资料', p0: 3, p1: 5, p2: 0, focus: '公司、部门、角色、员工、导游、费用字典' },
];

import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

import {
  buildSalesProductPayload,
  calculateArrangementTotal,
  createArrangementOverviewSummary,
  createDefaultArrangementItem,
  createDefaultProductForm,
  syncItineraryDaysWithTravelDays,
} from './product-form-utils';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('sales product form helpers', () => {
  it('keeps the old-system team arrangement tab as a link to the standalone page', () => {
    // 老系统产品编辑页保留“团队安排”tab；新系统点击该 tab 跳独立页，不再把安排大表内嵌在产品表单里。
    const formSource = readAppFile('src/views/sales/product/form.vue');
    const listSource = readAppFile('src/views/sales/product/index.vue');
    const routeSource = readAppFile('src/router/routes/modules/sales.ts');
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(formSource).toContain("{ key: 'arrangement', label: '团队安排' }");
    expect(formSource).toContain("if (key === 'arrangement')");
    expect(formSource).toContain("router.push(`/sales/product/team-arrangement/${productId.value}`)");
    expect(formSource).toContain('请先保存产品后再维护团队安排');
    expect(formSource).not.toContain("activeEditTab === 'arrangement'");
    expect(formSource).not.toContain('产品安排模板');
    expect(listSource).toContain("openTeamArrangementPage(record)");
    expect(listSource).not.toContain("openEditPage(record, 'arrangement')");
    expect(routeSource).toContain("path: '/sales/product/team-arrangement/:id'");
    expect(routeSource).toContain("title: '产品团队安排'");
    expect(arrangementPageSource).toContain('产品团队安排');
    expect(arrangementPageSource).not.toContain('这里只维护产品生成团队时的默认安排参数');
    expect(arrangementPageSource).not.toContain('不处理正式订单、单据、导游报账、计调审核和真实团队成本');
    expect(formSource).not.toContain('团队安排总览模板');
    expect(formSource).not.toContain('团队安排总览 Group Arrange');
    expect(formSource).not.toContain('收客 -> 排团 -> 发团 -> 结算 -> 完成');
    expect(formSource).not.toContain('订单信息');
    expect(formSource).not.toContain('订房单');
    expect(formSource).not.toContain('订车单');
    expect(formSource).not.toContain('预订单');
    expect(formSource).not.toContain('确认单');
  });

  it('keeps team arrangement header and team facts compact', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).not.toContain('description="维护产品生成团队时要带入的默认团队安排参数');
    expect(arrangementPageSource).not.toContain('form-subtitle');
    expect(arrangementPageSource).not.toContain('arrangement-alert');
    expect(arrangementPageSource).not.toContain('team-metric-card');
    expect(arrangementPageSource).not.toContain('form-primary-actions');
    expect(arrangementPageSource).toContain('team-metric-strip');
    expect(arrangementPageSource).toContain('team-metric-item');
    expect(arrangementPageSource).toContain('compact-action');
    expect(arrangementPageSource).toContain('arrangement-footer-actions');
  });

  it('uses old-system style overview and icon shortcuts instead of inline type select', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).not.toContain('arrangement-tool-bar');
    expect(arrangementPageSource).not.toContain('票据库');
    expect(arrangementPageSource).not.toContain('订单文件');
    expect(arrangementPageSource).not.toContain('备用金请款单');
    expect(arrangementPageSource).not.toContain('打印付款单');
    expect(arrangementPageSource).not.toContain('打印行程单');
    expect(arrangementPageSource).not.toContain('打印团队名单');
    expect(arrangementPageSource).not.toContain('打印计划单');
    expect(arrangementPageSource).not.toContain('打印结算单');
    expect(arrangementPageSource).not.toContain('打印毛利表');
    expect(arrangementPageSource).toContain('workflow-rail');
    expect(arrangementPageSource).toContain('产品模板');
    expect(arrangementPageSource).toContain('安排配置');
    expect(arrangementPageSource).toContain('生成团队');
    expect(arrangementPageSource).toContain('团队执行');
    expect(arrangementPageSource).toContain('财务结算');
    expect(arrangementPageSource).not.toContain("{ label: '收客', state: 'done' }");
    expect(arrangementPageSource).not.toContain("{ label: '排团', state: 'template' }");
    expect(arrangementPageSource).toContain('arrangement-command-bar');
    expect(arrangementPageSource).toContain('业务类型');
    expect(arrangementPageSource).toContain('部门');
    expect(arrangementPageSource).toContain('操作计调');
    expect(arrangementPageSource).toContain('导游');
    expect(arrangementPageSource).toContain('领队');
    expect(arrangementPageSource).toContain('全陪');
    expect(arrangementPageSource).toContain('接待标准');
    expect(arrangementPageSource).toContain('总里程数');
    expect(arrangementPageSource).toContain('应收/已收/余额');
    expect(arrangementPageSource).toContain('订单已确认/未处理/已取消');
    expect(arrangementPageSource).toContain('已付');
    expect(arrangementPageSource).toContain('旅游天数');
    expect(arrangementPageSource).toContain('预算利润');
    expect(arrangementPageSource).not.toContain("label: '散团'");
    expect(arrangementPageSource).not.toContain("label: '预控人数'");
    expect(arrangementPageSource).not.toContain("label: '实收人数'");
    expect(arrangementPageSource).not.toContain("label: '团号'");
    expect(arrangementPageSource).not.toContain("label: '出团日期'");
    expect(arrangementPageSource).toContain('团队管理');
    expect(arrangementPageSource).toContain('查看行程');
    expect(arrangementPageSource).toContain('team-note-row');
    expect(arrangementPageSource).toContain('内部备注');
    expect(arrangementPageSource).toContain('人均坑位');
    expect(arrangementPageSource).toContain('自费加点率');
    expect(arrangementPageSource).toContain('人均购物');
    expect(arrangementPageSource).toContain('正式团队安排模块接入后可用');
    expect(arrangementPageSource).toContain('arrangement-overview-table');
    expect(arrangementPageSource).toContain('导服');
    expect(arrangementPageSource).toContain('操作费');
    expect(arrangementPageSource).toContain('备用金');
    expect(arrangementPageSource).toContain('arrangement-icon-grid');
    expect(arrangementPageSource).toContain('arrangement-section-actions');
    expect(arrangementPageSource).toContain('无需');
    expect(arrangementPageSource).toContain('完成');
    expect(arrangementPageSource).toContain('arrangement-document-button');
    expect(arrangementPageSource).toContain('arrangement-add-button');
    expect(arrangementPageSource).toContain('订房单');
    expect(arrangementPageSource).toContain('订车单');
    expect(arrangementPageSource).toContain('预订单');
    expect(arrangementPageSource).toContain('确认单');
    expect(arrangementPageSource).toContain('供应商');
    expect(arrangementPageSource).toContain('备注');
    expect(arrangementPageSource).toContain('价格信息');
    expect(arrangementPageSource).toContain('成本合计');
    expect(arrangementPageSource).toContain('lucide:plane');
    expect(arrangementPageSource).toContain('lucide:building-2');
    expect(arrangementPageSource).toContain('lucide:car');
    expect(arrangementPageSource).not.toContain('<Table.Column title="类型"');
  });

  it('renders team arrangement options as old-system overview sections', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('arrangement-overview-tabs');
    expect(arrangementPageSource).toContain('arrangement-overview-sections');
    expect(arrangementPageSource).toContain('arrangement-section-card');
    expect(arrangementPageSource).toContain('arrangement-section-table');
    expect(arrangementPageSource).toContain("label: '总览'");
    expect(arrangementPageSource).toContain("anchor: 'part1'");
    expect(arrangementPageSource).toContain("anchor: 'part10'");
    expect(arrangementPageSource).toContain("documentAction: '订房单'");
    expect(arrangementPageSource).toContain("documentAction: '订车单'");
    expect(arrangementPageSource).toContain("documentAction: '预订单'");
    expect(arrangementPageSource).toContain("documentAction: '确认单'");
    expect(arrangementPageSource).toContain("'日期', '类型', '出发地', '目的地', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'");
    expect(arrangementPageSource).toContain("'入住', '退房', '几晚', '酒店名称', '早餐', '基金', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'");
    expect(arrangementPageSource).toContain("'开始', '结束', '车型', '天数', '司机', '车牌', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'");
    expect(arrangementPageSource).toContain("'日期', '景区名称', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'");
    expect(arrangementPageSource).toContain("'日期', '时间', '餐厅名称', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'");
    expect(arrangementPageSource).toContain("'日期', '景区/项目名称', '供应商', '备注', '人数', '销售价', '成本价', '收入合计', '成本合计', '导游提成', '现结', '挂账', '操作'");
    expect(arrangementPageSource).toContain("'日期', '购物店', '供应商', '备注', '品类', '进店人数', '人头费', '消费总额', '导游提成', '操作'");
    expect(arrangementPageSource).not.toContain('activeArrangementType === item.value');
    expect(arrangementPageSource).not.toContain('activeArrangementItems');
    expect(arrangementPageSource).not.toContain('<Table');
  });

  it('uses a compact operations-console layout for team arrangement', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('arrangement-command-bar');
    expect(arrangementPageSource).toContain('workflow-rail');
    expect(arrangementPageSource).toContain('team-note-row');
    expect(arrangementPageSource).toContain('compact-category-strip');
    expect(arrangementPageSource).not.toContain('arrangement-process-panel');
    expect(arrangementPageSource).not.toContain('arrangement-internal-note');
  });

  it('opens an old-system aligned traffic arrangement modal from the traffic plus button', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('openArrangementEditor(section.value)');
    expect(arrangementPageSource).toContain('saveArrangementEditor');
    expect(arrangementPageSource).toContain('persistArrangementChanges');
    expect(arrangementPageSource).toContain("editingArrangementIndex.value >= 0 ? '安排信息已修改' : '安排信息已保存'");
    expect(arrangementPageSource).toContain('traffic-arrangement-modal');
    expect(arrangementPageSource).toContain('activeEditorTitle');
    expect(arrangementPageSource).toContain('全团/订单均摊');
    expect(arrangementPageSource).toContain('多订单均摊成本');
    expect(arrangementPageSource).toContain('showMultiOrderAveragePriceNotice');
    expect(arrangementPageSource).toContain('多订单均摊成本时，价格信息组成只能统一写成一条记录，点击 ⊕ 失效');
    expect(arrangementPageSource).toContain('addArrangementPriceLine');
    expect(arrangementPageSource).toContain('removeArrangementPriceLine');
    expect(arrangementPageSource).toContain('arrangementForm.priceLines');
    expect(arrangementPageSource).toContain(':disabled="showMultiOrderAveragePriceNotice"');
    expect(arrangementPageSource).toContain(':class="{ disabled: showMultiOrderAveragePriceNotice }"');
    expect(arrangementPageSource).toContain('交通类型');
    expect(arrangementPageSource).toContain('日期行程');
    expect(arrangementPageSource).toContain('供应商');
    expect(arrangementPageSource).toContain('价格信息');
    expect(arrangementPageSource).toContain('结算方式');
    expect(arrangementPageSource).toContain('订单信息');
    expect(arrangementPageSource).toContain('备注信息');
    expect(arrangementPageSource).toContain('无需导游报账，同步更新导游报账和计调审核数据');
  });

  it('allows editing saved arrangement rows from the section table', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('editingArrangementIndex');
    expect(arrangementPageSource).toContain('hydrateArrangementFormFromItem');
    expect(arrangementPageSource).toContain('openArrangementEditor(section.value, item)');
    expect(arrangementPageSource).toContain('保存修改');
    expect(arrangementPageSource).toContain('新增安排');
    expect(arrangementPageSource).toContain("editingArrangementIndex.value >= 0 ? '安排信息已修改' : '安排信息已保存'");
    expect(arrangementPageSource).toContain('nextItems[editingArrangementIndex.value] = arrangementItem');
    expect(arrangementPageSource).not.toContain("if (column === '操作') return '--';");
  });

  it('keeps arrangement remark as manual text instead of auto-generated system fields', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('manualRemarkText');
    expect(arrangementPageSource).toContain('remark: manualRemarkText(arrangementForm.remark)');
    expect(arrangementPageSource).toContain('remark: manualRemarkText(item.remark)');
    expect(arrangementPageSource).not.toContain('`费用归属：${allocationLabel(arrangementForm.allocationMode)}`');
    expect(arrangementPageSource).not.toContain('`订单信息：${arrangementForm.orderScope || \'=不关联订单=\'}`');
    expect(arrangementPageSource).not.toContain("arrangementForm.noGuideReport ? '无需导游报账，同步更新导游报账和计调审核数据' : ''");
  });

  it('uses old-system per-field quick editors for team profile fields', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('teamProfile');
    expect(arrangementPageSource).toContain('TEAM_PROFILE_MARKER');
    expect(arrangementPageSource).toContain('openTeamProfileEditor');
    expect(arrangementPageSource).toContain('saveTeamProfileEditor');
    expect(arrangementPageSource).toContain('quickProfileEditorOpen');
    expect(arrangementPageSource).toContain('quickProfileEditorType');
    expect(arrangementPageSource).toContain('activeQuickProfileEditor');
    expect(arrangementPageSource).toContain("editorType: 'business_type'");
    expect(arrangementPageSource).toContain("editorType: 'department'");
    expect(arrangementPageSource).toContain("editorType: 'operator'");
    expect(arrangementPageSource).toContain("editorType: 'escort'");
    expect(arrangementPageSource).toContain("openTeamProfileEditor('internal_note')");
    expect(arrangementPageSource).toContain('修改业务类型');
    expect(arrangementPageSource).toContain('选择业务类型');
    expect(arrangementPageSource).toContain('修改业务部门');
    expect(arrangementPageSource).toContain('所属部门');
    expect(arrangementPageSource).toContain('修改操作计调');
    expect(arrangementPageSource).toContain('操作计调');
    expect(arrangementPageSource).toContain('团队全陪信息');
    expect(arrangementPageSource).toContain('内部备注');
    expect(arrangementPageSource).toContain('提交保存');
    expect(arrangementPageSource).toContain('保存信息');
    expect(arrangementPageSource).not.toContain('修改团队快捷信息');
    expect(arrangementPageSource).not.toContain('teamProfileModalOpen');
    expect(arrangementPageSource).toContain("label: '部门'");
    expect(arrangementPageSource).toContain("label: '操作计调'");
    expect(arrangementPageSource).toContain("label: '全陪'");
    expect(arrangementPageSource).toContain('teamProfile.departmentName ||');
    expect(arrangementPageSource).toContain('teamProfile.operatorName ||');
    expect(arrangementPageSource).toContain('teamProfile.escortName ||');
    expect(arrangementPageSource).toContain('teamProfile.totalDistanceText ||');
    expect(arrangementPageSource).toContain('teamProfile.internalNote ||');
    expect(arrangementPageSource).toContain('getEnterpriseDepartmentAll');
    expect(arrangementPageSource).toContain('getProductDictionaryAll');
    expect(arrangementPageSource).toContain('@click="item.editorType && openTeamProfileEditor(item.editorType)"');
    expect(arrangementPageSource).not.toContain("label: '部门', value: '总部'");
    expect(arrangementPageSource).not.toContain("label: '操作计调', value: '团队阶段指定'");
    expect(arrangementPageSource).not.toContain("label: '全陪', value: '--'");
    expect(arrangementPageSource).not.toContain('<Button type="link" size="small" @click="showStaticFeatureTip">');
  });

  it('keeps the hotel editor field order aligned with the old system', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const hotelLayoutSource = arrangementPageSource.slice(arrangementPageSource.indexOf('hotel-old-system-layout'));

    expect(arrangementPageSource).toContain("activeEditorType === 'hotel'");
    expect(arrangementPageSource).toContain('hotel-old-system-layout');
    expect(hotelLayoutSource.indexOf('酒店名称')).toBeLessThan(hotelLayoutSource.indexOf('早餐基金'));
    expect(hotelLayoutSource.indexOf('早餐基金')).toBeLessThan(hotelLayoutSource.indexOf('入住退房'));
    expect(hotelLayoutSource.indexOf('入住退房')).toBeLessThan(hotelLayoutSource.indexOf('<span>供应商</span>'));
    expect(hotelLayoutSource.indexOf('<span>供应商</span>')).toBeLessThan(hotelLayoutSource.indexOf('<span>价格信息</span>'));
    expect(hotelLayoutSource.indexOf('<span>价格信息</span>')).toBeLessThan(hotelLayoutSource.indexOf('<span>结算方式</span>'));
    expect(hotelLayoutSource.indexOf('<span>结算方式</span>')).toBeLessThan(hotelLayoutSource.indexOf('责任房调'));
  });

  it('keeps the vehicle editor field order aligned with the old system', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const vehicleLayoutStart = arrangementPageSource.indexOf('vehicle-old-system-layout');
    const vehicleLayoutSource = arrangementPageSource.slice(
      vehicleLayoutStart,
      arrangementPageSource.indexOf('<template v-else>', vehicleLayoutStart),
    );

    expect(arrangementPageSource).toContain("activeEditorType === 'vehicle'");
    expect(arrangementPageSource).toContain('vehicle-old-system-layout');
    expect(vehicleLayoutSource.indexOf('<span>座位数</span>')).toBeLessThan(vehicleLayoutSource.indexOf('<span>用车时间</span>'));
    expect(vehicleLayoutSource.indexOf('<span>用车时间</span>')).toBeLessThan(vehicleLayoutSource.indexOf('<span>司机车号</span>'));
    expect(vehicleLayoutSource.indexOf('<span>司机车号</span>')).toBeLessThan(vehicleLayoutSource.indexOf('<span>供应商</span>'));
    expect(vehicleLayoutSource.indexOf('<span>供应商</span>')).toBeLessThan(vehicleLayoutSource.indexOf('<span>价格信息</span>'));
    expect(vehicleLayoutSource.indexOf('<span>价格信息</span>')).toBeLessThan(vehicleLayoutSource.indexOf('<span>结算方式</span>'));
    expect(vehicleLayoutSource.indexOf('<span>结算方式</span>')).toBeLessThan(vehicleLayoutSource.indexOf('责任车调'));
    expect(vehicleLayoutSource.indexOf('责任车调')).toBeLessThan(vehicleLayoutSource.indexOf('订单信息'));
    expect(vehicleLayoutSource.indexOf('订单信息')).toBeLessThan(vehicleLayoutSource.indexOf('备注信息'));
    expect(vehicleLayoutSource).toContain('司机信息');
    expect(vehicleLayoutSource).toContain('车牌号');
    expect(vehicleLayoutSource).toContain('placeholder="手动输入司机姓名/电话"');
    expect(vehicleLayoutSource).toContain('placeholder="手动输入车牌号"');
    expect(vehicleLayoutSource).toContain(':options="driverHistoryOptions"');
    expect(vehicleLayoutSource).toContain(':options="vehiclePlateHistoryOptions"');
    expect(vehicleLayoutSource).not.toContain(':options="driverInfoOptions"');
    expect(vehicleLayoutSource).not.toContain(':options="vehiclePlateOptions"');
    expect(arrangementPageSource).toContain('getVehicleUsageHistorySuggestions');
    expect(arrangementPageSource).toContain('recordVehicleUsageHistory');
    expect(arrangementPageSource).toContain("recordVehicleHistoryUsage('driver_info'");
    expect(arrangementPageSource).toContain("recordVehicleHistoryUsage('vehicle_plate'");
  });

  it('uses seat count wording and no region fields for vehicle quote calculation', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const quotePageSource = readAppFile('src/views/dispatch/vehicle-quote/index.vue');

    expect(quotePageSource).toContain('座位数');
    expect(quotePageSource).toContain('座位数 / 状态');
    expect(quotePageSource).not.toContain('适用地区');
    expect(quotePageSource).not.toContain('formatRegion');
    expect(quotePageSource).not.toContain('ruleRegionPath');
    expect(quotePageSource).not.toContain('calcRegionPath');
    expect(quotePageSource).not.toContain('Cascader');
    expect(arrangementPageSource).toContain('<span>座位数</span>');
    expect(arrangementPageSource).toContain('Form.Item label="座位数"');
    expect(arrangementPageSource).toContain('Form.Item label="规则座位数"');
    expect(arrangementPageSource).not.toContain('province: formState.province');
    expect(arrangementPageSource).not.toContain('city: formState.city');
    expect(arrangementPageSource).not.toContain('district: formState.district');
    expect(quotePageSource).not.toContain('vehicleTypeOptions');
    expect(arrangementPageSource).not.toContain('vehicleTypeOptions');
    expect(quotePageSource).toContain('v-model:value="querySeatCount"');
    expect(quotePageSource).toContain('v-model:value="calcVehicleType"');
    expect(quotePageSource).toContain('v-model:value="formSeatCount"');
    expect(quotePageSource).toContain('addon-after="座"');
    expect(quotePageSource).toContain('function vehicleTypeFromSeatCount');
    expect(quotePageSource).toContain('function seatCountFromVehicleType');
    expect(arrangementPageSource).toContain('function seatCountFromVehicleType');
    expect(quotePageSource).toContain('placeholder="请输入座位数"');
    expect(arrangementPageSource).toContain('placeholder="请选择座位数规则"');
    expect(arrangementPageSource).toContain('v-model:value="arrangementForm.vehicleType"');
    expect(arrangementPageSource).toContain(':options="vehicleQuoteRuleOptions"');
    expect(arrangementPageSource).not.toContain('v-model:value="arrangementVehicleSeatCount"');
    expect(arrangementPageSource).not.toContain('addon-after="座"');
    expect(arrangementPageSource).not.toContain('function vehicleTypeFromSeatCount');
    expect(arrangementPageSource).not.toContain('placeholder="请输入座位数"');
    expect(quotePageSource).not.toContain('placeholder="手动输入，如 7座、19座、39座"');
    expect(arrangementPageSource).not.toContain('placeholder="手动输入，如 7座、19座、39座"');
  });

  it('uses configured vehicle quote rules for quick and map quote seat selection', () => {
    const quotePageSource = readAppFile('src/views/dispatch/vehicle-quote/index.vue');

    expect(quotePageSource).toContain('getVehicleQuoteRuleAll');
    expect(quotePageSource).toContain('quoteRuleOptions');
    expect(quotePageSource).toContain('loadQuoteRuleOptions');
    expect(quotePageSource).toContain('v-model:value="calcVehicleType"');
    expect(quotePageSource).toContain('v-model:value="mapQuoteVehicleType"');
    expect(quotePageSource).toContain(':options="quoteRuleOptions"');
    expect(quotePageSource).toContain('placeholder="请选择座位数规则"');
    expect(quotePageSource).not.toContain('v-model:value="calcSeatCount"');
    expect(quotePageSource).not.toContain('v-model:value="mapQuoteSeatCount"');
    expect(quotePageSource).toContain("await Promise.all([loadRules(), loadQuoteRuleOptions()])");
  });

  it('uses configured vehicle quote rules for team arrangement vehicle seat selection', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('getVehicleQuoteRuleAll');
    expect(arrangementPageSource).toContain('vehicleQuoteRuleOptions');
    expect(arrangementPageSource).toContain('loadVehicleQuoteRuleOptions');
    expect(arrangementPageSource).toContain('v-model:value="arrangementForm.vehicleType"');
    expect(arrangementPageSource).toContain(':options="vehicleQuoteRuleOptions"');
    expect(arrangementPageSource).toContain('placeholder="请选择座位数规则"');
    expect(arrangementPageSource).not.toContain('v-model:value="arrangementVehicleSeatCount"');
    expect(arrangementPageSource).not.toContain('请先填写座位数');
    expect(arrangementPageSource).toContain('请先选择座位数规则');
  });

  it('opens product roadbook map editor from vehicle arrangement distance panel', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const formPageSource = readAppFile('src/views/sales/product/form.vue');

    expect(arrangementPageSource).toContain('openProductRoadbookEditor');
    expect(arrangementPageSource).toContain('编辑路书地图');
    expect(arrangementPageSource).toContain("tab: 'itinerary'");
    expect(arrangementPageSource).toContain('roadbookDay');
    expect(formPageSource).toContain('requestedRoadbookDayIndex');
    expect(formPageSource).toContain('openRequestedRoadbookDrawer');
    expect(formPageSource).toContain('route.query.roadbookDay');
  });

  it('does not show real vehicle inquiry records inside product template arrangement', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).not.toContain('车队询价记录');
    expect(arrangementPageSource).not.toContain('新增询价');
    expect(arrangementPageSource).not.toContain('vehicle-inquiry-panel');
    expect(arrangementPageSource).not.toContain('addVehicleInquiryRecord');
    expect(arrangementPageSource).not.toContain('员工在微信群问车队后');
  });

  it('auto calculates vehicle day count from start and end day in team arrangement', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const vehicleLayoutStart = arrangementPageSource.indexOf('<template v-else-if="activeEditorType === \'vehicle\'">');
    const vehicleLayoutSource = arrangementPageSource.slice(
      vehicleLayoutStart,
      arrangementPageSource.indexOf('<template v-else-if="activeEditorType === \'meal\'">', vehicleLayoutStart),
    );

    expect(arrangementPageSource).toContain('syncVehicleDaysCount');
    expect(arrangementPageSource).toContain('const days = end - start + 1');
    expect(vehicleLayoutSource).toContain('@change="syncVehicleDaysCount"');
    expect(vehicleLayoutSource).toContain('v-model:value="arrangementForm.daysCount" disabled');
    expect(vehicleLayoutSource).not.toContain('<InputNumber v-model:value="arrangementForm.daysCount" :min="0" :precision="0" />');
  });

  it('auto calculates ground-agent day count from start and end day in team arrangement', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const groundAgentLayoutStart = arrangementPageSource.indexOf('<template v-else-if="activeEditorType === \'ground_agent\'">');
    const groundAgentLayoutSource = arrangementPageSource.slice(
      groundAgentLayoutStart,
      arrangementPageSource.indexOf('<template v-else>', groundAgentLayoutStart),
    );

    expect(arrangementPageSource).toContain('scheduleInclusiveDaysCount');
    expect(arrangementPageSource).toContain('syncGroundAgentDaysCount');
    expect(arrangementPageSource).toContain('const days = scheduleInclusiveDaysCount(');
    expect(groundAgentLayoutSource).toContain('@change="syncGroundAgentDaysCount"');
    expect(groundAgentLayoutSource).toContain('v-model:value="arrangementForm.daysCount" disabled');
    expect(groundAgentLayoutSource).not.toContain('<InputNumber v-model:value="arrangementForm.daysCount" :min="0" :precision="0" />');
  });

  it('uses product travel days for arrangement date options', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('const scheduleDayOptions = computed<SelectOption[]>(() =>');
    expect(arrangementPageSource).toContain('Number(formState.travelDays || 1)');
    expect(arrangementPageSource).not.toContain("const scheduleDayOptions: SelectOption[] = [");
  });

  it('auto calculates hotel night count from check-in and check-out days', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const hotelLayoutStart = arrangementPageSource.indexOf('<template v-if="activeEditorType === \'hotel\'">');
    const hotelLayoutSource = arrangementPageSource.slice(
      hotelLayoutStart,
      arrangementPageSource.indexOf('<template v-else-if="activeEditorType === \'vehicle\'">', hotelLayoutStart),
    );

    expect(arrangementPageSource).toContain('syncHotelNightsCount');
    expect(arrangementPageSource).toContain('scheduleExclusiveNightsCount');
    expect(hotelLayoutSource).toContain('@change="syncHotelNightsCount"');
    expect(hotelLayoutSource).toContain('v-model:value="arrangementForm.daysCount" disabled');
  });

  it('lets users add optional and shopping detail rows instead of only normalizing existing rows', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('addOptionalSummaryLine');
    expect(arrangementPageSource).toContain('addShoppingConsumptionLine');
    expect(arrangementPageSource).toContain('@click="addOptionalSummaryLine"');
    expect(arrangementPageSource).toContain('@click="addShoppingConsumptionLine"');
    expect(arrangementPageSource).toContain('optional-fee-summary-line');
    expect(arrangementPageSource).toContain('shopping-consumption-line');
    expect(arrangementPageSource).toContain('v-for="(line, index) in arrangementForm.priceLines"');
    expect(arrangementPageSource).toContain('v-model:value="line.salePrice"');
    expect(arrangementPageSource).toContain('v-model:value="line.costPrice"');
    expect(arrangementPageSource).toContain('v-model:value="line.consumptionAmount"');
    expect(arrangementPageSource).toContain('v-model:value="line.companyRebateRate"');
    expect(arrangementPageSource).toContain('v-model:value="line.guideCommissionRate"');
    expect(arrangementPageSource).toContain('syncOptionalLineToForm');
    expect(arrangementPageSource).toContain('syncShoppingLineToForm');
    expect(arrangementPageSource).not.toContain('class="optional-add-summary-button"\n                          title="添加费用合计"\n                          type="button"\n                          @click="normalizeArrangementPriceLines"');
    expect(arrangementPageSource).not.toContain('class="shopping-add-detail-button"\n                        title="添加消费详情"\n                        type="button"\n                        @click="normalizeArrangementPriceLines"');
  });

  it('supports deleting arrangement rows from the team arrangement table', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('deleteArrangementItem');
    expect(arrangementPageSource).toContain('confirmDeleteArrangementItem');
    expect(arrangementPageSource).toContain('@click="confirmDeleteArrangementItem(item)"');
    expect(arrangementPageSource).toContain('Modal.confirm');
    expect(arrangementPageSource).toContain("title: '确认删除这条安排？'");
    expect(arrangementPageSource).toContain('安排信息已删除');
    expect(arrangementPageSource).not.toContain('@click="deleteArrangementItem(item)"');
  });

  it('renders vehicle quote rules with visible table columns and unified list styling', () => {
    const quotePageSource = readAppFile('src/views/dispatch/vehicle-quote/index.vue');

    expect(quotePageSource).not.toContain(':columns="[]"');
    expect(quotePageSource).toContain('class="vehicle-quote-table"');
    expect(quotePageSource).toContain('<Table.Column title="座位数"');
    expect(quotePageSource).toContain('<Table.Column title="基础价"');
    expect(quotePageSource).toContain('<Table.Column title="基础公里"');
    expect(quotePageSource).toContain('<Table.Column title="操作"');
    expect(quotePageSource).toContain('.vehicle-quote-table');
  });

  it('adds a full-screen map quote drawer beside quick vehicle quote', () => {
    const quotePageSource = readAppFile('src/views/dispatch/vehicle-quote/index.vue');

    expect(quotePageSource).toContain('打开地图报价');
    expect(quotePageSource).toContain('mapQuoteDrawerOpen');
    expect(quotePageSource).toContain('class="vehicle-map-quote-drawer"');
    expect(quotePageSource).toContain('width="calc(100vw - 32px)"');
    expect(quotePageSource).toContain('vehicle-map-quote-workspace');
    expect(quotePageSource).toContain('vehicle-map-container');
    expect(quotePageSource).toContain('vehicle-map-side-panel');
    expect(quotePageSource).toContain('searchAmapTips');
    expect(quotePageSource).toContain('getAmapJsConfig');
    expect(quotePageSource).toContain('calculateRoadbookRoute');
    expect(quotePageSource).toContain('handleMapQuoteMapClick');
    expect(quotePageSource).toContain('destroyMapQuoteMap');
    expect(quotePageSource).toContain('calculateMapQuoteRoute');
    expect(quotePageSource).toContain('calculateMapQuotePrice');
    expect(quotePageSource).toContain('mapQuoteVehicleType');
    expect(quotePageSource).toContain('mapQuotePoints');
    expect(quotePageSource).toContain('地图报价');
    expect(quotePageSource).toContain('搜索地址或直接点地图');
    expect(quotePageSource).toContain('总里程');
    expect(quotePageSource).toContain('预计车程');
    expect(quotePageSource).toContain('参考报价');
    expect(quotePageSource).toContain('这是高德驾车路线距离，不是直线距离');
  });

  it('highlights map quote result metrics in a compact toolbar summary', () => {
    const quotePageSource = readAppFile('src/views/dispatch/vehicle-quote/index.vue');

    expect(quotePageSource).toContain('vehicle-map-toolbar-main');
    expect(quotePageSource).toContain('vehicle-map-route-line');
    expect(quotePageSource).toContain('vehicle-map-metric-strip');
    expect(quotePageSource).toContain('vehicle-map-metric-item');
    expect(quotePageSource).toContain('vehicle-map-metric-item price');
    expect(quotePageSource).toContain('vehicle-map-metric-label');
    expect(quotePageSource).toContain('vehicle-map-metric-value');
    expect(quotePageSource).toContain('参考报价');
    expect(quotePageSource).toContain('总里程');
    expect(quotePageSource).toContain('预计车程');
    expect(quotePageSource).not.toContain('vehicle-map-result-panel');
    expect(quotePageSource).not.toContain('vehicle-map-result-card');
    expect(quotePageSource).not.toContain('vehicle-map-meta span');
  });

  it('explains vehicle quote rule fields inside the rule modal', () => {
    const quotePageSource = readAppFile('src/views/dispatch/vehicle-quote/index.vue');

    expect(quotePageSource).toContain('只填写数字，例如 7、19、39');
    expect(quotePageSource).toContain('基础公里以内的起步参考价');
    expect(quotePageSource).toContain('超过基础公里后，每多 1 公里增加的参考费用');
    expect(quotePageSource).toContain('1.00 不浮动，1.10 表示上浮 10%');
    expect(quotePageSource).toContain('参考价 = max(基础价 + 超出公里 × 超公里单价, 最低价) × 浮动系数');
    expect(quotePageSource).toContain('当前先不按地区区分报价，只按座位数和路书公里测算');
  });

  it('uses real dropdown APIs for all team arrangement category editors', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('getSupplierAll');
    expect(arrangementPageSource).toContain('getExpenseItemAll');
    expect(arrangementPageSource).toContain('getEnterpriseEmployeeAll');
    expect(arrangementPageSource).toContain('getPurchaseResourcePage');
    expect(arrangementPageSource).toContain('getPurchaseRelationPage');
    expect(arrangementPageSource).toContain('supplierCategoryMap');
    expect(arrangementPageSource).toContain('expenseResourceTypeMap');
    expect(arrangementPageSource).toContain('loadEditorOptions(type)');
    expect(arrangementPageSource).toContain('resourceOptions');
    expect(arrangementPageSource).not.toContain('大交通供应商A');
    expect(arrangementPageSource).not.toContain('机票票务供应商');
    expect(arrangementPageSource).not.toContain('火车票服务商');
  });

  it('caches team arrangement editor options and refreshes detail after modal save', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const persistSource = arrangementPageSource.slice(
      arrangementPageSource.indexOf('async function persistArrangementChanges'),
      arrangementPageSource.indexOf('/** 保存当前分类安排到产品模板明细并立即写入后端。'),
    );
    const saveEditorSource = arrangementPageSource.slice(
      arrangementPageSource.indexOf('async function saveArrangementEditor'),
      arrangementPageSource.indexOf('function unitNameForType'),
    );
    const saveAllSource = arrangementPageSource.slice(
      arrangementPageSource.indexOf('async function saveArrangement()'),
      arrangementPageSource.indexOf('onMounted(loadDetail)'),
    );

    expect(arrangementPageSource).toContain('type EditorOptionsCacheEntry');
    expect(arrangementPageSource).toContain('function editorNeedsEmployeeOptions(type: SalesProductApi.ArrangementType)');
    expect(arrangementPageSource).toContain('const editorOptionsCache = new Map<SalesProductApi.ArrangementType, EditorOptionsCacheEntry>()');
    expect(arrangementPageSource).toContain('applyEditorOptionsCache');
    expect(arrangementPageSource).toContain('async function loadEditorOptions(type: SalesProductApi.ArrangementType, force = false)');
    expect(arrangementPageSource).toContain('const cachedOptions = editorOptionsCache.get(type)');
    expect(arrangementPageSource).toContain('editorNeedsEmployeeOptions(type) ? getEnterpriseEmployeeAll(false) : Promise.resolve([])');
    expect(arrangementPageSource).toContain('editorOptionsCache.set(type');
    expect(persistSource).toContain('reloadAfterSave?: boolean');
    expect(persistSource).toContain("saveMode?: 'arrangements' | 'full'");
    expect(persistSource).toContain('updateSalesProductArrangements(productId.value');
    expect(persistSource).toContain('arrangementItems: payload.arrangementItems');
    expect(persistSource).toContain('options.reloadAfterSave ?? true');
    expect(arrangementPageSource).toContain('updateSalesProductArrangements');
    expect(arrangementPageSource).toContain("persistArrangementChanges(`${editor.title}已保存`, { saveMode: 'full' })");
    expect(saveEditorSource).toContain('persistSingleArrangementChange');
    expect(saveEditorSource).toContain('currentArrangementId');
    expect(saveEditorSource).toContain('arrangementItem.id = savedArrangementId');
    expect(saveEditorSource).not.toContain('persistArrangementChanges(');
    expect(saveEditorSource).not.toContain('{ reloadAfterSave: false }');
    expect(saveEditorSource).toContain('previousItems');
    expect(saveEditorSource).toContain('formState.arrangementItems = previousItems');
    expect(saveAllSource).toContain("persistArrangementChanges('团队安排已保存', { reloadAfterSave: true })");
  });

  it('saves and deletes a single team arrangement item instead of rewriting the whole arrangement list', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const apiSource = readAppFile('src/api/sales/product.ts');
    const deleteSource = arrangementPageSource.slice(
      arrangementPageSource.indexOf('async function deleteArrangementItem'),
      arrangementPageSource.indexOf('function confirmDeleteArrangementItem'),
    );

    expect(apiSource).toContain("'/sales/product/arrangement/save'");
    expect(apiSource).toContain("'/sales/product/arrangement/delete'");
    expect(arrangementPageSource).toContain('saveSalesProductArrangement');
    expect(arrangementPageSource).toContain('deleteSalesProductArrangement');
    expect(arrangementPageSource).toContain('async function persistSingleArrangementChange');
    expect(arrangementPageSource).toContain('arrangementId,');
    expect(deleteSource).toContain('deleteSalesProductArrangement(productId.value, item.id)');
    expect(deleteSource).not.toContain("persistArrangementChanges('安排信息已删除'");
  });

  it('loads scenic arrangement resources from purchase relation and refreshes suppliers after selecting a scenic resource', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('getPurchaseRelationPage');
    expect(arrangementPageSource).toContain('getRelationTicketTemplateDetail');
    expect(arrangementPageSource).toContain('scenicResourceRelationOptions');
    expect(arrangementPageSource).toContain('scenicTicketTemplate');
    expect(arrangementPageSource).toContain('selectedScenicResourceRelation');
    expect(arrangementPageSource).toContain('loadResourceSupplierOptions');
    expect(arrangementPageSource).toContain('loadSelectedScenicTicketTemplate');
    expect(arrangementPageSource).toContain('applySelectedResource');
    expect(arrangementPageSource).toContain('openScenicTemplateConfigPage');
    expect(arrangementPageSource).toContain('templateRelationId');
    expect(arrangementPageSource).toContain('游客名单模板');
    expect(arrangementPageSource).toContain("resourceType: 'scenic'");
    expect(arrangementPageSource).toContain('@change="applySelectedResource"');
    expect(arrangementPageSource).not.toContain('pageSize: 500');
    expect(arrangementPageSource).not.toContain("import { getScenicResourcePage } from '#/api/purchase/scenic'");
    expect(arrangementPageSource).not.toContain("getScenicResourcePage({ page: 1, pageSize: 200, status: 'active' })");
  });

  it('opens purchase relation ticket template drawer from scenic arrangement deep link', () => {
    const relationPageSource = readAppFile('src/views/purchase/relation/index.vue');

    expect(relationPageSource).toContain('useRoute');
    expect(relationPageSource).toContain('openTemplateDrawerFromRoute');
    expect(relationPageSource).toContain('route.query.templateRelationId');
    expect(relationPageSource).toContain('query.resourceType =');
    expect(relationPageSource).toContain('openTemplateDrawer(row)');
  });

  it('loads hotel arrangement dropdown from resource overview and opens add hotel page', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const hotelLayoutSource = arrangementPageSource.slice(arrangementPageSource.indexOf('hotel-old-system-layout'));

    expect(arrangementPageSource).not.toContain("import { getHotelResourcePage } from '#/api/purchase/hotel'");
    expect(arrangementPageSource).toContain("resourceType: 'hotel'");
    expect(arrangementPageSource).toContain('openHotelCreatePage');
    expect(arrangementPageSource).toContain("path: '/purchase/resource'");
    expect(arrangementPageSource).toContain("create: '1'");
    expect(arrangementPageSource).toContain("resourceType: 'hotel'");
    expect(hotelLayoutSource).toContain('@click="openHotelCreatePage"');
    expect(hotelLayoutSource).toContain('添加酒店');
  });

  it('uses region cascaders and real traffic suppliers in the traffic arrangement editor', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const crudPageSource = readAppFile('src/views/_business/crud/CrudPage.vue');

    expect(arrangementPageSource).toContain('Cascader');
    expect(arrangementPageSource).toContain('buildRegionOptions');
    expect(arrangementPageSource).toContain('departureRegionPath');
    expect(arrangementPageSource).toContain('arrivalRegionPath');
    expect(arrangementPageSource).toContain("traffic: 'traffic'");
    expect(arrangementPageSource).toContain('getSupplierAll(supplierCategory)');
    expect(arrangementPageSource).toContain('formatTrafficRegionPath');
    expect(arrangementPageSource).toContain('openSupplierCreatePage');
    expect(arrangementPageSource).toContain('router.resolve');
    expect(arrangementPageSource).toContain("path: '/purchase/supplier'");
    expect(arrangementPageSource).toContain('category: supplierCategoryMap[activeEditorType.value]');
    expect(arrangementPageSource).toContain("create: '1'");
    expect(arrangementPageSource).toContain("window.open(routeInfo.href, '_blank', 'noopener,noreferrer')");
    expect(crudPageSource).toContain('applyRoutePreset');
    expect(crudPageSource).toContain("route.query.create === '1'");
    expect(crudPageSource).toContain('openCreateModal()');
    expect(arrangementPageSource).not.toContain('placeholder="请输入出发地"');
    expect(arrangementPageSource).not.toContain('placeholder="请输入目的地"');
    expect(arrangementPageSource).not.toContain('v-model:value="arrangementForm.departurePlace"');
    expect(arrangementPageSource).not.toContain('v-model:value="arrangementForm.arrivalPlace"');
  });

  it('uses old-system field sets for each team arrangement editor instead of a generic modal', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('arrangementEditorConfigs');
    expect(arrangementPageSource).toContain("title: '添加/修改大交通信息'");
    expect(arrangementPageSource).toContain("title: '添加/修改酒店信息'");
    expect(arrangementPageSource).toContain("title: '添加/修改用车信息'");
    expect(arrangementPageSource).toContain("title: '添加/修改景区信息'");
    expect(arrangementPageSource).toContain("title: '添加/修改用餐信息'");
    expect(arrangementPageSource).toContain("title: '添加/修改其它信息'");
    expect(arrangementPageSource).toContain("title: '添加/修改自费信息'");
    expect(arrangementPageSource).toContain("title: '添加/修改购物信息'");
    expect(arrangementPageSource).toContain("title: '添加/修改地接信息'");
    expect(arrangementPageSource).toContain("title: '添加/修改附加费用'");
    expect(arrangementPageSource).toContain('酒店名称');
    expect(arrangementPageSource).toContain('早餐基金');
    expect(arrangementPageSource).toContain('入住退房');
    expect(arrangementPageSource).toContain('责任房调');
    expect(arrangementPageSource).toContain('开始结束');
    expect(arrangementPageSource).toContain('责任车调');
    expect(arrangementPageSource).toContain('景区名称');
    expect(arrangementPageSource).toContain('餐厅名称');
    expect(arrangementPageSource).toContain('景区/项目名称');
    expect(arrangementPageSource).toContain('购物店');
    expect(arrangementPageSource).toContain('拼团日期');
    expect(arrangementPageSource).toContain('添加/修改附加费用');
    expect(arrangementPageSource).not.toContain('<span>日期行程</span>');
    expect(arrangementPageSource).not.toContain('<span>资源与供应商</span>');
  });

  it('aligns meal other optional shopping and ground-agent editors with old-system fields', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('meal-old-system-layout');
    expect(arrangementPageSource).toContain('other-old-system-layout');
    expect(arrangementPageSource).toContain('optional-old-system-layout');
    expect(arrangementPageSource).toContain('shopping-old-system-layout');
    expect(arrangementPageSource).toContain('ground-agent-old-system-layout');
    expect(arrangementPageSource).toContain("title: '添加/修改用餐信息'");
    expect(arrangementPageSource).toContain("title: '添加/修改地接信息'");
    expect(arrangementPageSource).toContain('使用日期');
    expect(arrangementPageSource).toContain('游玩日期');
    expect(arrangementPageSource).toContain('购物日期');
    expect(arrangementPageSource).toContain('费用设置');
    expect(arrangementPageSource).toContain('消费详情');
    expect(arrangementPageSource).toContain('公司返佣');
    expect(arrangementPageSource).toContain('导游提成');
    expect(arrangementPageSource).toContain('费用合计');
    expect(arrangementPageSource).toContain('人数：');
    expect(arrangementPageSource).toContain('收入：');
    expect(arrangementPageSource).toContain('提成：');
    expect(arrangementPageSource).toContain('拼团日期');
    expect(arrangementPageSource).toContain('共几天');
    expect(arrangementPageSource).toContain('standardMealProjectOptions');
    expect(arrangementPageSource).toContain('otherProjectOptions');
    expect(arrangementPageSource).toContain('groundAgentProjectOptions');
    expect(arrangementPageSource).toContain('shoppingCategoryOptions');
    expect(arrangementPageSource).toContain('礼品');
    expect(arrangementPageSource).toContain('购物返佣');
    expect(arrangementPageSource).toContain('成人');
    expect(arrangementPageSource).toContain('代收团款');
    expect(arrangementPageSource).toContain('乳胶');
    expect(arrangementPageSource).toContain('唐卡');
    expect(arrangementPageSource).toContain("resourceType: 'restaurant'");
    expect(arrangementPageSource).toContain("resourceType: 'shopping'");
    expect(arrangementPageSource).toContain("optional: 'scenic'");
    expect(arrangementPageSource).toContain("ground_agent: 'ground_agent'");
    expect(arrangementPageSource).toContain('resourceRelationOptions');
    expect(arrangementPageSource).toContain('loadResourceSupplierOptions');
    expect(arrangementPageSource).toContain("type === 'meal' || type === 'shopping'");
    expect(arrangementPageSource).toContain("loadResourceSupplierOptions(arrangementForm.resourceName)");
    expect(arrangementPageSource).toContain("|| activeEditorType.value === 'meal'");
    expect(arrangementPageSource).toContain("|| activeEditorType.value === 'shopping'");
    expect(arrangementPageSource).not.toContain("type === 'meal' || type === 'shopping' ? Promise.resolve([]) : getSupplierAll(supplierCategory)");
    expect(arrangementPageSource).not.toContain("title: '添加/修改餐厅信息'");
    expect(arrangementPageSource).not.toContain("title: '添加/修改拼团信息'");
  });

  it('aligns optional editor guide commission and fee summary with old-system fields', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('guideCommissionRate: number;');
    expect(arrangementPageSource).toContain('v-model:value="arrangementForm.guideCommissionRate"');
    expect(arrangementPageSource).toContain('元/人');
    expect(arrangementPageSource).toContain('或');
    expect(arrangementPageSource).toContain('%');
    expect(arrangementPageSource).toContain('费用合计');
    expect(arrangementPageSource).toContain('optional-fee-summary-row');
    expect(arrangementPageSource).toContain('optional-add-summary-button');
    expect(arrangementPageSource).toContain('无需导游报账，同步更新导游报账和计调审核数据');
    expect(arrangementPageSource).toContain('v-model:value="line.guideCommissionAmount"');
    expect(arrangementPageSource).toContain('guideCommissionRate: Number(line.guideCommissionRate ?? (arrangementForm.guideCommissionRate || 0))');
  });

  it('aligns shopping editor fields with old-system logic while keeping new-system visual layout', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('shopping-compact-grid');
    expect(arrangementPageSource).toContain('shopping-fee-row');
    expect(arrangementPageSource).toContain('shopping-consumption-main-row');
    expect(arrangementPageSource).toContain('shopping-consumption-extra-row');
    expect(arrangementPageSource).toContain('companyRebateRate: number;');
    expect(arrangementPageSource).toContain('v-model:value="line.companyRebateRate"');
    expect(arrangementPageSource).toContain('公司返佣');
    expect(arrangementPageSource).toContain('% =');
    expect(arrangementPageSource).toContain('导游提成');
    expect(arrangementPageSource).toContain('%销售额 =');
    expect(arrangementPageSource).toContain('shopping-add-detail-button');
    expect(arrangementPageSource).toContain('companyRebateRate: Number(line.companyRebateRate ?? (arrangementForm.companyRebateRate || 0))');
  });

  it('keeps structured team arrangement fields and price lines in the product payload', () => {
    const payload = buildSalesProductPayload(
      {
        domesticInternational: 'domestic',
        productName: '测试产品',
        status: 'active',
        travelDays: 1,
        tripType: 'irregular',
        arrangementItems: [
          {
            allocationMode: 'group_order_average',
            arrangementType: 'hotel',
            cashAmount: 20,
            creditAmount: 200,
            itemName: '苏州四钻酒店-标间',
            priceLines: [
              {
                amount: 220,
                projectId: 1,
                projectName: '标间',
                quantity: 1,
                unitPrice: 220,
              },
            ],
            quantity: 1,
            resourceName: '苏州四钻酒店',
            scheduleEndDay: '第2天',
            scheduleStartDay: '第1天',
            settlementType: 'credit',
            supplierId: 99,
            supplierName: '苏州酒店供应商',
            totalAmount: 220,
            unitPrice: 220,
          },
        ],
      },
      [],
    );

    expect(payload.arrangementItems?.[0]).toMatchObject({
      allocationMode: 'group_order_average',
      cashAmount: 20,
      creditAmount: 200,
      resourceName: '苏州四钻酒店',
      supplierId: 99,
      supplierName: '苏州酒店供应商',
      totalAmount: 220,
    });
    expect(payload.arrangementItems?.[0]?.priceLines?.[0]).toMatchObject({
      amount: 220,
      projectId: 1,
      projectName: '标间',
      quantity: 1,
      unitPrice: 220,
    });
  });

  it('keeps vehicle quote snapshots and inquiry records in the product payload', () => {
    const payload = buildSalesProductPayload(
      {
        domesticInternational: 'domestic',
        productName: '测试用车产品',
        status: 'active',
        travelDays: 3,
        tripType: 'irregular',
        arrangementItems: [
          {
            arrangementType: 'vehicle',
            cashAmount: 0,
            creditAmount: 2220.35,
            daysCount: 3,
            driverName: '王131749',
            itemName: '39座-车费',
            priceLines: [
              {
                amount: 2220.35,
                projectName: '车费',
                quantity: 1,
                unitPrice: 2220.35,
              },
            ],
            quantity: 1,
            scheduleEndDay: '第3天',
            scheduleStartDay: '第1天',
            settlementType: 'credit',
            supplierName: '浙江安心客运有限公司',
            totalAmount: 2220.35,
            unitPrice: 2220.35,
            vehicleInquiryRecords: [
              {
                availableVehicleCount: 1,
                groupName: '用车报价群',
                includesDriverMeal: true,
                includesParking: true,
                includesToll: true,
                inquiryMethod: 'wechat_group',
                inquiryPerson: '张车调',
                quotedAmount: 2220.35,
                remark: '报价可用',
                replyPerson: '王经理',
                selected: true,
                sortOrder: 1,
                supplierName: '浙江安心客运有限公司',
              },
            ],
            vehiclePlate: '浙233',
            vehicleQuoteSnapshot: {
              calculatedAmount: 2220.35,
              confirmedAmount: 2220.35,
              endDayNo: 3,
              quoteRuleId: 39,
              routeSummary: '第1天-第3天路书',
              ruleBaseKilometers: 100,
              ruleBasePrice: 1200,
              ruleExtraKilometerPrice: 6,
              ruleFloatRate: 1,
              ruleMinimumPrice: 900,
              ruleVehicleType: '39座',
              scheduleEndDay: '第3天',
              scheduleStartDay: '第1天',
              startDayNo: 1,
              syncedDistanceMeters: 249_400,
              syncedDurationSeconds: 17_340,
            },
            vehicleType: '39座',
          },
        ],
      },
      [],
    );

    expect(payload.arrangementItems?.[0]).toMatchObject({
      daysCount: 3,
      driverName: '王131749',
      supplierName: '浙江安心客运有限公司',
      vehiclePlate: '浙233',
      vehicleType: '39座',
    });
    expect(payload.arrangementItems?.[0]?.vehicleQuoteSnapshot).toMatchObject({
      calculatedAmount: 2220.35,
      confirmedAmount: 2220.35,
      endDayNo: 3,
      routeSummary: '第1天-第3天路书',
      ruleVehicleType: '39座',
      startDayNo: 1,
      syncedDistanceMeters: 249_400,
      syncedDurationSeconds: 17_340,
    });
    expect(payload.arrangementItems?.[0]?.vehicleInquiryRecords?.[0]).toMatchObject({
      includesDriverMeal: true,
      includesParking: true,
      includesToll: true,
      inquiryMethod: 'wechat_group',
      quotedAmount: 2220.35,
      selected: true,
      supplierName: '浙江安心客运有限公司',
    });
  });

  it('keeps product description fields inside the old-system description tab', () => {
    const formSource = readAppFile('src/views/sales/product/form.vue');

    expect(formSource).toContain("normalizeEditTab(String(route.query.tab || 'basic'))");
    expect(formSource).not.toContain("return key === 'description' ? 'itinerary' : key;");
    expect(formSource).toContain("{ key: 'description', label: '产品说明' }");
    expect(formSource).toContain("activeEditTab === 'description'");
    expect(formSource).toContain('product-description-matrix-layout');
    expect(formSource).toContain('product-description-side-label');
    expect(formSource).toContain('product-description-main');
    expect(formSource).toContain('产品说明');
    expect(formSource).toContain('收客须知');
    expect(formSource).toContain('费用包含');
    expect(formSource).toContain('费用不含');
    expect(formSource).toContain('儿童安排');
    expect(formSource).toContain('购物安排');
    expect(formSource).toContain('自费项目');
    expect(formSource).toContain('赠送项目');
    expect(formSource).toContain('注意事项');
    expect(formSource).toContain('温馨提醒');
  });

  it('does not expose product dictionary management inside the product edit tabs', () => {
    const formSource = readAppFile('src/views/sales/product/form.vue');

    expect(formSource).not.toContain("{ key: 'dictionary', label: '业务类型管理' }");
    expect(formSource).not.toContain("router.push('/enterprise/product-dictionary')");
    expect(formSource).not.toContain("key === 'dictionary'");
    expect(formSource).toContain("{ key: 'description', label: '产品说明' }");
    expect(formSource).toContain("{ key: 'arrangement', label: '团队安排' }");
    expect(formSource).not.toContain("{ key: 'arrangement', label: '安排模板' }");
  });

  it('uses old-system table layout for product basic information', () => {
    const formSource = readAppFile('src/views/sales/product/form.vue');

    expect(formSource).toContain('product-basic-matrix-layout');
    expect(formSource).toContain('product-basic-side-label');
    expect(formSource).toContain('product-basic-row');
    expect(formSource).toContain('线路名称');
    expect(formSource).toContain('接团城市');
    expect(formSource).toContain('截止收客');
    expect(formSource).not.toContain('<Form.Item label="产品名称"');
    expect(formSource).not.toContain('出团日期');
    expect(formSource).not.toContain('团号');
  });

  it('uses old-system itinerary matrix instead of day cards', () => {
    const formSource = readAppFile('src/views/sales/product/form.vue');

    expect(formSource).toContain('itinerary-matrix-layout');
    expect(formSource).toContain('线路行程');
    expect(formSource).toContain('导入行程');
    expect(formSource).toContain('路书信息');
    expect(formSource).not.toContain('itinerary-card');
    expect(formSource).not.toContain('template #title>第 {{ index + 1 }} 天');
  });

  it('syncs itinerary rows from travel days like the old system', () => {
    const synced = syncItineraryDaysWithTravelDays(
      [
        {
          dayNo: 1,
          dayTitle: '抵达南京',
          itineraryContent: '接站后入住酒店',
        },
      ],
      3,
    );

    expect(synced).toHaveLength(3);
    expect(synced.map((item) => item.dayNo)).toEqual([1, 2, 3]);
    expect(synced[0]).toMatchObject({
      dayTitle: '抵达南京',
      itineraryContent: '接站后入住酒店',
    });
    expect(synced[1]).toMatchObject({
      breakfastIncluded: false,
      dayNo: 2,
      dinnerIncluded: false,
      lunchIncluded: false,
    });
  });

  it('syncs itinerary rows down when travel days are reduced', () => {
    const synced = syncItineraryDaysWithTravelDays(
      [
        { dayNo: 1, dayTitle: '第一天' },
        { dayNo: 2, dayTitle: '第二天' },
        { dayNo: 3, dayTitle: '第三天' },
      ],
      2,
    );

    expect(synced).toHaveLength(2);
    expect(synced.map((item) => item.dayNo)).toEqual([1, 2]);
  });

  it('does not expose itinerary add or row delete buttons', () => {
    const formSource = readAppFile('src/views/sales/product/form.vue');

    expect(formSource).not.toContain('addItineraryDay');
    expect(formSource).not.toContain('removeItineraryDay');
    expect(formSource).not.toContain('新增一天');
    expect(formSource).not.toContain('删除多余天数');
  });

  it('uses hotel select options for itinerary related hotel', () => {
    const formSource = readAppFile('src/views/sales/product/form.vue');

    expect(formSource).toContain('getControlledRoomResourceAll');
    expect(formSource).toContain('getHotelResourcePage');
    expect(formSource).toContain('loadRelatedHotelOptions');
    expect(formSource).toContain('relatedHotelOptions');
    expect(formSource).toContain('v-model:value="day.relatedHotel"');
    expect(formSource).toContain(':options="relatedHotelOptions"');
    expect(formSource).not.toContain('placeholder="关联酒店"');
  });

  it('uses a roadbook editor instead of a plain roadbook input', () => {
    const formSource = readAppFile('src/views/sales/product/form.vue');

    expect(formSource).toContain('openRoadbookDrawer');
    expect(formSource).toContain('roadbookDrawerOpen');
    expect(formSource).toContain('编辑路书');
    expect(formSource).toContain('calculateRoadbookRoute');
    expect(formSource).toContain('searchAmapTips');
    expect(formSource).toContain('getAmapJsConfig');
    expect(formSource).toContain('handleRoadbookMapClick');
    expect(formSource).toContain('destroyRoadbookMap');
    expect(formSource).toContain('amapInstance.destroy');
    expect(formSource).toContain('center: [120.14895, 30.24490]');
    expect(formSource).not.toContain('center: [120.585315, 31.298886]');
    expect(formSource).toContain('class="roadbook-workspace-drawer"');
    expect(formSource).toContain('width="calc(100vw - 32px)"');
    expect(formSource).toContain('roadbook-workspace-main');
    expect(formSource).toContain('roadbook-map-shell');
    expect(formSource).toContain('roadbook-map-container');
    expect(formSource).toContain('roadbook-side-panel');
    expect(formSource).toContain('.roadbook-side-panel {\n  position: absolute;');
    expect(formSource).toContain('停留(分钟)');
    expect(formSource).toContain('到下一站(公里)');
    expect(formSource).toContain('车程(分钟)');
    expect(formSource).not.toContain('addon-after="秒"');
    expect(formSource).toContain('路书摘要');
    expect(formSource).not.toContain('placeholder="添加地点 / 公里"');
  });

  it('debounces roadbook place search before calling amap tips', () => {
    const formSource = readAppFile('src/views/sales/product/form.vue');

    expect(formSource).toContain('ROADBOOK_SEARCH_DEBOUNCE_MS = 700');
    expect(formSource).toContain('roadbookSearchTimer');
    expect(formSource).toContain('window.setTimeout');
    expect(formSource).toContain('window.clearTimeout(roadbookSearchTimer)');
    expect(formSource).toContain('doRoadbookSearch');
  });

  it('keeps the itinerary roadbook as a full-width detail row', () => {
    const formSource = readAppFile('src/views/sales/product/form.vue');

    expect(formSource).toContain('roadbook-detail-row');
    expect(formSource).toContain('roadbook-detail-card');
    expect(formSource).toContain('roadbook-detail-main');
    expect(formSource).toContain('roadbook-detail-stats');
    expect(formSource).toContain('roadbook-detail-actions');
    expect(formSource).toContain('roadbook-edit-button');
    expect(formSource).toContain('roadbookPointCountText(day)');
    expect(formSource).toContain('roadbookDistanceText(day)');
    expect(formSource).toContain('roadbookDurationText(day)');
    expect(formSource).not.toContain('road-col');
    expect(formSource).not.toContain('position: sticky;');
  });

  it('moves from basic information to itinerary content before saving product', () => {
    const formSource = readAppFile('src/views/sales/product/form.vue');

    expect(formSource).toContain('@change="handleTravelDaysChange"');
    expect(formSource).toContain('syncItineraryDaysWithTravelDays');
    expect(formSource).toContain("if (activeEditTab.value === 'basic')");
    expect(formSource).toContain("activeEditTab.value = 'itinerary'");
    expect(formSource).toContain('请继续维护行程内容');
  });

  it('creates a new product form with one default itinerary day', () => {
    expect(createDefaultProductForm()).toMatchObject({
      domesticInternational: 'domestic',
      productName: '',
      status: 'active',
      travelDays: 1,
      tripType: 'irregular',
    });
    expect(createDefaultProductForm().itineraryDays).toEqual([
      {
        breakfastIncluded: false,
        dayNo: 1,
        dinnerIncluded: false,
        lunchIncluded: false,
        seasonalSurcharge: 0,
      },
    ]);
    expect(createDefaultProductForm().arrangementItems).toMatchObject([
      { arrangementType: 'traffic', itemName: '' },
      { arrangementType: 'hotel', itemName: '' },
      { arrangementType: 'vehicle', itemName: '' },
      { arrangementType: 'scenic', itemName: '' },
      { arrangementType: 'meal', itemName: '' },
      { arrangementType: 'other', itemName: '' },
      { arrangementType: 'optional', itemName: '' },
      { arrangementType: 'shopping', itemName: '' },
      { arrangementType: 'ground_agent', itemName: '' },
      { arrangementType: 'extra_fee', itemName: '' },
    ]);
  });

  it('creates a default arrangement item for the product template', () => {
    expect(createDefaultArrangementItem()).toEqual({
      arrangementType: 'hotel',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitPrice: 0,
    });
  });

  it('builds a clean save payload with region fields and filtered empty arrangement rows', () => {
    const payload = buildSalesProductPayload(
      {
        arrangementItems: [
          {
            arrangementContent: '  含一晚住宿  ',
            arrangementType: 'hotel',
            itemName: '  标间  ',
            quantity: 2,
            settlementType: 'credit',
            unitName: '间',
            unitPrice: 180,
          },
          {
            arrangementType: 'meal',
            itemName: '   ',
          },
        ],
        domesticInternational: 'domestic',
        itineraryDays: [
          {
            breakfastIncluded: true,
            dayNo: 9,
            dayTitle: '  抵达杭州  ',
            itineraryContent: '  西湖游览  ',
          },
        ],
        productName: '  杭州二日游  ',
        status: 'active',
        travelDays: 2,
        tripType: 'daily',
      },
      ['浙江省', '杭州市'],
    );

    expect(payload).toMatchObject({
      city: '杭州市',
      district: undefined,
      domesticInternational: 'domestic',
      productName: '杭州二日游',
      province: '浙江省',
      travelDays: 2,
      tripType: 'daily',
    });
    expect(payload.arrangementItems).toEqual([
      {
        arrangementContent: '含一晚住宿',
        arrangementType: 'hotel',
        itemName: '标间',
        quantity: 2,
        remark: undefined,
        settlementType: 'credit',
        unitName: '间',
        unitPrice: 180,
      },
    ]);
    expect(payload.itineraryDays).toEqual([
      {
        accommodationNote: undefined,
        breakfastIncluded: true,
        dayNo: 1,
        dayTitle: '抵达杭州',
        dinnerIncluded: false,
        itineraryContent: '西湖游览',
        lunchIncluded: false,
        relatedHotel: undefined,
        roadbookPlace: undefined,
        roadbookPoints: [],
        roadbookSummary: undefined,
        roadbookTotalDistanceMeters: 0,
        roadbookTotalDurationSeconds: 0,
        seasonalSurcharge: 0,
      },
    ]);
  });

  it('calculates arrangement reference total from quantity and unit price', () => {
    expect(
      calculateArrangementTotal([
        { arrangementType: 'hotel', itemName: '房', quantity: 2, unitPrice: 150 },
        { arrangementType: 'vehicle', itemName: '车', quantity: 1, unitPrice: 800 },
      ]),
    ).toBe(1100);
  });

  it('summarizes arrangement costs by old-system cash and credit columns', () => {
    const summary = createArrangementOverviewSummary([
      { arrangementType: 'hotel', itemName: '酒店', quantity: 2, settlementType: 'cash', unitPrice: 100 },
      { arrangementType: 'hotel', itemName: '酒店挂账', quantity: 1, settlementType: 'credit', unitPrice: 80 },
      { arrangementType: 'scenic', itemName: '景区', quantity: 10, settlementType: 'credit', unitPrice: 10 },
      { arrangementType: 'optional', itemName: '自费', quantity: 3, settlementType: 'cash', unitPrice: 50 },
    ]);

    expect(summary.byType.hotel).toEqual({ cash: 200, credit: 80, total: 280 });
    expect(summary.byType.scenic).toEqual({ cash: 0, credit: 100, total: 100 });
    expect(summary.byType.optional).toEqual({ cash: 150, credit: 0, total: 150 });
    expect(summary.total).toEqual({ cash: 350, credit: 180, total: 530 });
    expect(summary.extraColumns).toEqual({
      guideService: 0,
      operationFee: 0,
      reserveFund: 0,
      selfPayIncome: 150,
    });
  });
});

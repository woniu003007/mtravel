import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

function expectTextOrder(source: string, labels: string[]) {
  let previousIndex = -1;
  for (const label of labels) {
    const currentIndex = source.indexOf(label, previousIndex + 1);
    expect(currentIndex, `missing label: ${label}`).toBeGreaterThanOrEqual(0);
    expect(currentIndex, `${label} should appear after the previous label`).toBeGreaterThan(previousIndex);
    previousIndex = currentIndex;
  }
}

describe('sales team operation layout', () => {
  it('uses product form capability when directly creating sanpin zhengtuan and santuan teams', () => {
    const source = readAppFile('src/views/sales/team/create.vue');
    const apiSource = readAppFile('src/api/sales/team.ts');

    expect(source).toContain('BusinessPillTabs');
    expect(source).toContain('Drawer');
    expect(source).toContain('isEditMode');
    expect(source).toContain('editTeamId');
    expect(source).toContain('loadEditDetail');
    expect(source).toContain('getSalesTeamEditDetail');
    expect(source).toContain('updateSalesTeam');
    expect(source).toContain("{ key: 'basic', label: '基本信息' }");
    expect(source).toContain("{ key: 'itinerary', label: '行程内容' }");
    expect(source).toContain("{ key: 'description', label: '产品说明' }");
    expect(source).toContain("{ key: 'arrangement', label: '团队安排' }");
    expect(source).not.toContain('业务类型管理');
    expect(source).toContain('添加/修改团队');
    expect(source).not.toContain('添加/修改产品团队');
    expect(source).toContain('保存团队');
    expect(source).toContain('activeEditTab');
    expect(source).toContain("activeEditTab === 'basic'");
    expect(source).toContain("activeEditTab === 'itinerary'");
    expect(source).toContain("activeEditTab === 'description'");
    expect(source).not.toContain("activeEditTab === 'arrangement'");
    expect(source).toContain('createDefaultItineraryDay');
    expect(source).toContain('syncItineraryDaysWithTravelDays');
    expect(source).toContain('formState.itineraryDays');
    expect(source).toContain('getControlledRoomResourceAll');
    expect(source).toContain('getHotelResourcePage');
    expect(source).toContain('relatedHotelOptions');
    expect(source).toContain('选择关联酒店');
    expect(source).toContain('旺季附加费');
    expect(source).toContain('路书信息');
    expect(source).toContain('编辑路书');
    expect(source).toContain('roadbookPoints');
    expect(source).toContain('searchAmapTips');
    expect(source).toContain('getAmapJsConfig');
    expect(source).toContain('calculateRoadbookRoute');
    expect(source).toContain('roadbook-workspace-drawer');
    expect(source).toContain('bookingNotice');
    expect(source).toContain('feeIncluded');
    expect(source).toContain('feeExcluded');
    expect(source).not.toContain('customerCategoryName');
    expect(source).not.toContain('adultPrice');
    expect(source).not.toContain('childPrice');
    expect(source).not.toContain('childNoBedPrice');
    expect(source).not.toContain('seniorPrice');
    expect(source).not.toContain('extraFee');
    expect(source).not.toContain('客户类型');
    expect(source).not.toContain('成人价');
    expect(source).not.toContain('儿童价');
    expect(source).not.toContain('老人价');
    expect(source).toContain('旺季附加费');
    expect(source).toContain('router.push(`/sales/team/arrangement/${result.id}`)');
    expect(source).toContain('router.push(`/sales/team/arrangement/${editTeamId.value}`)');
    expect(source).toContain("if (key === 'arrangement')");
    expect(source).not.toContain('router.push(`/sales/product/team-arrangement/${result.productId}`)');
    expect(apiSource).toContain('itineraryDays?:');
    expect(apiSource).toContain('bookingNotice?: string;');
    expect(apiSource).toContain('productDescription?: string;');
    expect(apiSource).toContain('feeIncluded?: string;');
    expect(apiSource).toContain('feeExcluded?: string;');
    expect(apiSource).toContain('getSalesTeamEditDetail');
    expect(apiSource).toContain('updateSalesTeam');
  });

  it('keeps the old-system operation order while using the new-system visual shell', () => {
    const source = readAppFile('src/views/sales/team/operation.vue');
    const apiSource = readAppFile('src/api/sales/team.ts');

    expect(source).toContain('team-operation-header');
    expect(source).toContain('top-tool-actions');
    expect(source).toContain('operation-flow-row');
    expect(source).toContain('team-profile-block');
    expect(source).toContain('team-description-stack');
    expect(source).toContain('operation-icon-actions');
    expect(source).toContain('metric-teamNo');
    expect(source).toContain('minmax(218px, 1.5fr)');
    expect(source).toContain('display: flex;');
    expect(source).toContain('stageState');
    expect(source).toContain('price-modal-table');
    expect(source).toContain('itinerary-modal-content');
    expect(source).toContain('profileEditorOpen');
    expect(source).toContain('openProfileEditor');
    expect(source).toContain('saveProfileEditor');
    expect(source).toContain('team_type');
    expect(source).toContain('business_type');
    expect(source).toContain('department');
    expect(source).toContain('operator');
    expect(source).toContain('escort');
    expect(source).toContain('getProductDictionaryAll');
    expect(source).toContain('getEnterpriseDepartmentAll');
    expect(source).toContain('getEnterpriseEmployeeAll');
    expect(source).toContain('saveSalesTeam');
    expect(source).toContain("action.code === 'teamArrangement' && team.value?.id");
    expect(source).toContain('router.push(`/sales/team/arrangement/${team.value.id}`)');
    expect(source).toContain("action.code === 'editTeam' && team.value?.id");
    expect(source).toContain('router.push(`/sales/team/edit/${team.value.id}`)');
    expect(source).not.toContain('router.push(`/sales/product/team-arrangement/${team.value.productId}`)');
    expect(source).not.toContain('router.push(`/sales/product/schedule/${team.value.productId}`)');
    expect(source).toContain('该团相关订单');
    expect(source).toContain('ORDER_TABLE_SCROLL_X');
    expect(source).toContain('order-cell-clamp');
    expect(source).toContain('order-edit-button');
    expect(source).toContain('修改');
    expect(source).toContain('isMergeSourceOrder(record)');
    expect(source).toContain('firstMergeTargetTeamId');
    expect(source).toContain('去目标团');
    expect(source).toContain('record.status');
    expect(source).toContain('record.pickupInfo');
    expect(source).toContain('record.dropoffInfo');
    expect(source).toContain('record.originalOrderInfo');
    expect(source).toContain('record.mergeOrderInfos');
    expect(source).toContain('record.orderRole');
    expect(source).toContain('record.guestCountText');
    expect(source).toContain('record.priceDetail');
    expect(source).toContain('record.feeRemark');
    expect(source).toContain('record.orderRemark');
    expect(source).toContain('Tooltip');
    expect(source).toContain('order-remark-ellipsis');
    expect(source).toContain('order-remark-tooltip');
    expect(source).toContain('原始订单信息');
    expect(source).toContain('已拼至');
    expect(source).toContain('来源订单');
    expect(source).toContain('sourceOrderInfos');
    expect(source).toContain('goRelatedTeam');
    expect(source).toContain('order-original-row');
    expect(source).toContain('order-merge-row');
    expect(source).toContain('order-multiline-cell');
    expect(source).toContain('title="拼团操作"');
    expect(source).toContain('merge-selected-orders');
    expect(source).toContain('merge-team-search-form');
    expect(source).toContain('merge-team-table');
    expect(source).toContain('mergeTeamColumns');
    expect(source).toContain('mergeTeamRowSelection');
    expect(source).toContain('mergeTeamPagination');
    expect(source).toContain('canTransferOrder');
    expect(source).toContain('transferDisabledReason');
    expect(source).toContain('validateSelectedTransferOrders');
    expect(source).toContain('已取消订单不能拼团或转团');
    expect(source).toContain('已拼出订单不能再次拼团或转团');
    expect(source).toContain('getCheckboxProps');
    expect(source).toContain('选择团期');
    expect(source).toContain('团号/团队名称');
    expect(source).toContain('客户单位');
    expect(source).toContain('出团日期始');
    expect(source).toContain('出团日期止');
    expect(source).toContain('天数');
    expect(source).toContain('预控/实收/余位');
    expect(source).toContain('已选目标团');
    expect(source).toContain('targetTeamId');
    expect(source).toContain('已选订单');
    expect(source).toContain('title="将选择的订单转到其它团队"');
    expect(source).toContain('复制当前团队，并将订单转入新团队');
    expect(source).toContain('团队日期');
    expect(source).toContain('预控人数');
    expect(source).toContain('团队名称');
    expect(source).toContain('操作备注');
    expect(source).toContain('moveForm.createNewTeam = false');
    expect(source).toContain('moveForm.allNum = Math.max(0');
    expect(source).toContain('请选择团队类型');
    expect(source).toContain('请填写团队名称');
    expect(source).toContain('预控人数填写有误');
    expect(source).not.toContain('转到已有团队');
    expect(source).not.toContain('发团日期');
    expect(source).not.toContain('总位数');
    expect(source).not.toContain('新团队备注');
    expect(source).not.toContain('title="拆分拼团"');
    expect(source).not.toContain('merge-split-card');
    expect(source).not.toContain('分配游客');
    expect(source).not.toContain('已拆金额');
    expect(source).not.toContain('新增明细');
    expect(source).not.toContain('拆分金额');
    expect(source).not.toContain('operation-tool-strip');
    expect(source).not.toContain('business-action-grid');
    expect(source).not.toContain('grid-template-columns: repeat(6, minmax(128px, 1fr))');
    expect(source).not.toContain('index <= 1');
    expect(source).not.toContain('总里程数\', value: \'0公里');
    expect(source).not.toContain('#b88207');
    expect(source).not.toContain('#087987');
    expect(apiSource).toContain('targetTeamId: number;');
    expect(apiSource).not.toContain('targetTeamIds: number[];');
    expect(apiSource).not.toContain('splits: MergeOrderSplitParams[];');

    expectTextOrder(source, [
      '团队操作',
      'top-tool-actions',
      'operation-flow-row',
      'team-profile-block',
      'team-description-stack',
      'operation-icon-actions',
      '该团相关订单',
    ]);

    expectTextOrder(source, ['产品说明', '收客须知', '内部备注']);
  });

  it('makes the arrangement cost overview easier to scan', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const productArrangementSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const arrangementLayoutSource = readAppFile('src/views/sales/team-arrangement-layout.css');

    expect(arrangementSource).toContain('costOverviewSummaryItems');
    expect(productArrangementSource).toContain('arrangementCostSummaryItems');
    expect(arrangementSource).toContain('class="cost-overview-summary"');
    expect(productArrangementSource).toContain('class="cost-overview-summary"');
    expect(arrangementSource).toContain('现结合计');
    expect(arrangementSource).toContain('挂账合计');
    expect(arrangementSource).toContain('成本合计');
    expect(arrangementSource).toContain('自费收入');
    expect(arrangementSource).toContain('导服');
    expect(arrangementSource).toContain('操作费');
    expect(arrangementSource).toContain('备用金');
    expect(arrangementSource).toContain("tone: 'primary'");
    expect(arrangementSource).toContain("tone: 'strong'");
    expect(productArrangementSource).toContain("tone: 'primary'");
    expect(productArrangementSource).toContain("tone: 'strong'");
    expect(arrangementSource).toContain("`cost-summary-card--${item.tone || 'normal'}`");
    expect(productArrangementSource).toContain("`cost-summary-card--${item.tone || 'normal'}`");
    expect(arrangementSource).toContain('costAmountClass');
    expect(productArrangementSource).toContain('costAmountClass');
    expect(arrangementLayoutSource).toContain('.cost-overview-summary');
    expect(arrangementLayoutSource).toContain('grid-template-columns: minmax(190px, 1.35fr)');
    expect(arrangementLayoutSource).toContain('.cost-summary-card--primary');
    expect(arrangementLayoutSource).toContain('.cost-summary-card--strong');
    expect(arrangementLayoutSource).toContain('.cost-summary-amount');
    expect(arrangementLayoutSource).toContain('font-size: 20px;');
    expect(arrangementLayoutSource).toContain('.cost-amount-zero');
    expect(arrangementLayoutSource).toContain('.cost-amount-nonzero');
    expect(arrangementSource).not.toContain('<th colspan="2">合计</th>');
    expect(productArrangementSource).not.toContain('<th colspan="2">合计</th>');
    expect(arrangementSource).not.toContain('class="cost-total-cell"');
    expect(productArrangementSource).not.toContain('class="cost-total-cell"');
    expect(arrangementSource).toContain("v-for=\"item in costColumns.slice(0, 10)\"");
    expect(arrangementSource).not.toContain('</template>\n                <th>现结</th>\n                <th>挂账</th>');
    expect(productArrangementSource).not.toContain('</template>\n                <th>现结</th>\n                <th>挂账</th>');
  });

  it('routes formal team arrangement by team id instead of product template id', () => {
    const routeSource = readAppFile('src/router/routes/modules/sales.ts');
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const productArrangementSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const editorSource = readAppFile('src/views/sales/components/ArrangementEditorModal.vue');
    const arrangementLayoutSource = readAppFile('src/views/sales/team-arrangement-layout.css');

    expect(routeSource).toContain("path: '/sales/team/arrangement/:id'");
    expect(routeSource).toContain("path: '/sales/team/edit/:id'");
    expect(routeSource).toContain("title: '修改团队'");
    expect(routeSource).toContain("title: '团队安排总览'");
    expect(arrangementSource).toContain("import '../team-arrangement-layout.css'");
    expect(productArrangementSource).toContain("import '../team-arrangement-layout.css'");
    expect(arrangementSource).toContain('团队安排总览');
    expect(arrangementSource).toContain('Group Arrange');
    expect(arrangementSource).toContain('team-arrangement-card');
    expect(arrangementSource).toContain('formal-team-arrangement-card');
    expect(arrangementSource).toContain('formal-arrangement-tool-strip');
    expect(arrangementSource).toContain('formal-tool-strip-title');
    expect(arrangementSource).toContain('formal-tool-strip-actions');
    expect(arrangementSource).toContain('formal-arrangement-tool-button');
    expect(arrangementSource).toContain('command-side');
    expect(arrangementSource).toContain('workflow-rail');
    expect(arrangementSource).toContain('stage-flow-item');
    expect(arrangementSource).toContain('stage-index');
    expect(arrangementSource).toContain('stage-label');
    expect(arrangementSource).toContain('arrangement-tabs-block');
    expect(productArrangementSource).toContain('arrangement-tabs-block');
    expect(arrangementSource).not.toContain('formal-arrangement-tabs-block');
    expect(arrangementSource).toContain('arrangement-icon-grid compact-category-strip');
    expect(arrangementSource).toContain('team-title-line formal-team-title-line');
    expect(arrangementSource).toContain('team-badges formal-team-badges');
    expect(arrangementSource).toContain('team-metric-strip formal-team-metric-strip');
    expect(productArrangementSource).toContain('team-title-line formal-team-title-line');
    expect(productArrangementSource).toContain('team-badges formal-team-badges');
    expect(productArrangementSource).toContain('team-metric-strip formal-team-metric-strip');
    expect(arrangementSource).toContain('calculateVehicleQuoteRule');
    expect(arrangementSource).toContain('const vehicleQuoteCalculating = ref(false);');
    expect(arrangementSource).toContain('lastVehicleQuoteResult');
    expect(arrangementSource).toContain('teamItineraryDays');
    expect(arrangementSource).toContain('vehicleDistanceText');
    expect(arrangementSource).toContain('routeDurationText');
    expect(arrangementSource).toContain('message.success(`测算参考价');
    expect(arrangementSource).toContain("message.success('已应用到价格信息')");
    expect(arrangementSource).toContain(':vehicle-quote-calculating="vehicleQuoteCalculating"');
    expect(editorSource).toContain('showProductVehicleAssistActions');
    expect(editorSource).toContain('v-if="showProductVehicleAssistActions" type="primary" ghost @click="emit(\'open-product-roadbook-editor\')"');
    expect(editorSource).toContain('v-if="showProductVehicleAssistActions" type="primary" ghost @click="emit(\'apply-vehicle-quote-to-price-info\')"');
    expect(editorSource).toContain("'sync-vehicle-price-project': [value?: unknown];");
    expect(editorSource).toContain("@change=\"(value) => emit('sync-vehicle-price-project', value)\"");
    expect(arrangementSource).toContain('function syncVehiclePriceProject');
    expect(productArrangementSource).toContain('function syncVehiclePriceProject');
    expect(arrangementSource).toContain('firstLine.projectName = selectedValue');
    expect(productArrangementSource).toContain('firstLine.projectName = selectedValue');
    expect(arrangementSource).not.toContain('正式排团用车测算后续接入');
    expect(arrangementSource).not.toContain('正式排团用车报价后续接入保存');
    expect(arrangementSource).toContain(':class="`metric-${item.key}`"');
    expect(productArrangementSource).toContain(':class="`metric-${item.key}`"');
    expect(arrangementLayoutSource).toContain('.metric-travelDays');
    expect(arrangementLayoutSource).toContain('.metric-orderStatus');
    expect(arrangementLayoutSource).toContain('.metric-paid');
    expect(arrangementLayoutSource).toContain('.metric-profit');
    expect(arrangementLayoutSource).toContain('grid-column: 1 / -1;');
    expect(arrangementLayoutSource).toContain('flex-wrap: nowrap;');
    expect(arrangementLayoutSource).toContain('width: fit-content;');
    expect(arrangementLayoutSource).toContain('max-width: 100%;');
    expect(arrangementLayoutSource).toContain('flex: 0 0 max-content;');
    expect(arrangementLayoutSource).toContain('min-width: max-content;');
    expect(arrangementLayoutSource).toContain('text-overflow: clip;');
    expect(arrangementLayoutSource).toContain('.formal-team-arrangement-card .arrangement-section-header');
    expect(arrangementLayoutSource).toContain('.formal-team-arrangement-card .arrangement-section-table th');
    expect(arrangementSource).not.toContain('formal-progress-panel');
    expect(arrangementSource).not.toContain('formal-print-more-button');
    expect(arrangementSource).not.toContain('formal-step-segment-row');
    expect(arrangementSource).not.toContain('formal-step-segment-list');
    expect(arrangementSource).not.toContain('formal-step-segment-item');
    expect(arrangementSource).not.toContain('formal-step-segment-index');
    expect(arrangementSource).not.toContain('formal-step-segment-label');
    expect(arrangementSource).not.toContain('formal-inline-progress-head');
    expect(arrangementSource).not.toContain('formal-inline-progress-rail');
    expect(arrangementSource).not.toContain('formal-inline-progress-row');
    expect(arrangementSource).not.toContain('formal-inline-progress-actions');
    expect(arrangementSource).not.toContain('formal-current-stage-pill');
    expect(arrangementSource).not.toContain('formal-next-stage-text');
    expect(arrangementSource).not.toContain('formal-execution-summary');
    expect(arrangementSource).not.toContain('primaryToolActions');
    expect(arrangementSource).not.toContain('printToolActions');
    expect(arrangementSource).not.toContain('更多打印');
    expect(arrangementSource).toContain('arrangement-command-bar');
    expect(arrangementSource).toContain('arrangement-overview-table');
    expect(arrangementSource).toContain('arrangement-overview-tabs');
    expect(arrangementSource).toContain('arrangement-section-card');
    expect(arrangementSource).toContain('formal-cost-overview-title');
    expect(productArrangementSource).toContain('formal-cost-overview-title');
    expect(arrangementSource).toContain('getSalesTeamOperationDetail');
    expect(arrangementSource).toContain('showTeamItineraryModal');
    expect(arrangementSource).toContain('@click="showItineraryModal"');
    expect(arrangementSource).not.toContain("@click=\"handleToolAction({ label: '查看行程' })\"");
    expect(arrangementSource).toContain('quickProfileEditorOpen');
    expect(arrangementSource).toContain('openTeamProfileEditor');
    expect(arrangementSource).toContain('saveTeamProfileEditor');
    expect(arrangementSource).toContain('getProductDictionaryAll');
    expect(arrangementSource).toContain('getEnterpriseDepartmentAll');
    expect(arrangementSource).toContain('getEnterpriseEmployeeAll');
    expect(arrangementSource).toContain('saveSalesTeam');
    expect(arrangementSource).toContain(':class="{ editable: badge.editorType }"');
    expect(arrangementSource).toContain("@click=\"badge.editorType && openTeamProfileEditor(badge.editorType)\"");
    expect(arrangementSource).toContain("editorType: 'business_type'");
    expect(arrangementSource).toContain("editorType: 'department'");
    expect(arrangementSource).toContain("editorType: 'operator'");
    expect(arrangementSource).toContain("editorType: 'escort'");
    expect(arrangementSource).toContain('route.params.id');
    expect(arrangementSource).toContain('getTeamArrangements');
    expect(arrangementSource).toContain('saveTeamArrangement');
    expect(arrangementSource).toContain('openArrangementEditor');
    expect(arrangementSource).toContain('arrangementModalOpen');
    expect(arrangementSource).toContain('arrangementOverviewTabs');
    expect(arrangementSource).toContain('arrangementSections');
    expect(arrangementSource).toContain('无需');
    expect(arrangementSource).toContain('完成');
    expect(arrangementSource).toContain('票据库');
    expect(arrangementSource).toContain('订单文件');
    expect(arrangementSource).toContain('备用金请款单');
    expect(arrangementSource).toContain('打印付款单');
    expect(arrangementSource).toContain('打印行程单');
    expect(arrangementSource).toContain('打印团队名单');
    expect(arrangementSource).toContain('打印计划单');
    expect(arrangementSource).toContain('打印结算单');
    expect(arrangementSource).toContain('打印毛利表');
    expect(arrangementSource).toContain('大交通');
    expect(arrangementSource).toContain('住宿');
    expect(arrangementSource).toContain('用车');
    expect(arrangementSource).toContain('景区');
    expect(arrangementSource).toContain('用餐');
    expect(arrangementSource).toContain('地接');
    expect(arrangementSource).toContain('附加');
    expect(arrangementSource).not.toContain('产品团队安排');
    expect(arrangementSource).not.toContain('产品模板');
    expect(arrangementSource).not.toContain('saveSalesProductArrangement');
    expect(arrangementSource).not.toContain('updateSalesProductArrangements');
    expect(arrangementSource).not.toContain('deleteSalesProductArrangement');
    expect(arrangementSource).not.toContain('/sales/product/team-arrangement');
    expect(arrangementSource).not.toContain('team-arrangement-shell');
    expect(arrangementSource).not.toContain('cost-table-wrap');
    expect(arrangementSource).not.toContain('arrangement-console-head');
  });

  it('reuses the product arrangement editor modal on formal team arrangement pages', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const productArrangementSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const editorSource = readAppFile('src/views/sales/components/ArrangementEditorModal.vue');

    expect(arrangementSource).toContain('ArrangementEditorModal');
    expect(productArrangementSource).toContain('ArrangementEditorModal');
    expect(arrangementSource).toContain("editorMode=\"team\"");
    expect(productArrangementSource).toContain("editorMode=\"product\"");
    expect(arrangementSource).not.toContain(':title="`添加/修改${activeEditor.label}信息`"');
    expect(arrangementSource).not.toContain('参考金额');
    expect(arrangementSource).not.toContain('`${activeEditor.label}名称`');
    expect(editorSource).toContain('hotel-old-system-layout');
    expect(editorSource).toContain('vehicle-old-system-layout');
    expect(editorSource).toContain('traffic-arrangement-modal');
    expect(arrangementSource).toContain('getTeamArrangements');
    expect(arrangementSource).toContain('saveTeamArrangement');
    expect(arrangementSource).toContain('deleteTeamArrangement');
    expect(arrangementSource).toContain('teamArrangements');
    expect(arrangementSource).not.toContain('正式排团数据表设计后接入保存');
  });

  it('loads formal team arrangement supplier resource project and order options for editor modal', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const editorSource = readAppFile('src/views/sales/components/ArrangementEditorModal.vue');

    expect(arrangementSource).toContain("import { getExpenseItemAll } from '#/api/enterprise/expense-item'");
    expect(arrangementSource).toContain("import { getEnterpriseEmployeeAll } from '#/api/enterprise/employee'");
    expect(arrangementSource).toContain("import { getPurchaseRelationPage } from '#/api/purchase/relation'");
    expect(arrangementSource).toContain("import { getPurchaseResourcePage } from '#/api/purchase/resource'");
    expect(arrangementSource).toContain("import { getSupplierAll");
    expect(arrangementSource).toContain('calculateVehicleQuote as calculateVehicleQuoteRule');
    expect(arrangementSource).toContain('getVehicleQuoteRuleAll');
    expect(arrangementSource).toContain('const orderOptions = computed<SelectOption[]>(() =>');
    expectTextOrder(arrangementSource, [
      "const customerText = item.orderInfo || item.guestName || '未命名订单';",
      "const contactText = item.orderInfo ? item.guestName : '';",
      'const bookingText = item.bookingInfo;',
      'const guestText = item.guestCountText',
      'item.orderNo || `订单${item.id}`',
      'customerText',
      'contactText',
      'bookingText',
      'guestText',
    ]);
    expect(arrangementSource).toContain(':order-options="orderOptions"');
    expect(arrangementSource).toContain('await loadEditorOptions(type)');
    expect(arrangementSource).toContain('getSupplierAll(supplierCategory)');
    expect(arrangementSource).toContain('getExpenseItemAll(expenseResourceTypeMap[type]');
    expect(arrangementSource).toContain('loadResourceOptions(type)');
    expect(arrangementSource).toContain('loadResourceSupplierOptions');
    expect(editorSource).toContain('orderOptions?: SelectOption[]');
    expect(editorSource).toContain('mergedOrderOptions');
    expect(editorSource).toContain(':options="mergedOrderOptions"');
    expect(editorSource).toContain('selectedOrderIds');
    expect(editorSource).toContain('mode="multiple"');
    expect(editorSource).toContain('按订单均摊');
    expect(editorSource).toContain('按人数均摊');
  });

  it('downloads scenic ticket guest excel from formal team arrangements only', () => {
    const apiSource = readAppFile('src/api/sales/team.ts');
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const productArrangementSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const editorSource = readAppFile('src/views/sales/components/ArrangementEditorModal.vue');

    expect(apiSource).toContain('exportScenicTicketGuests');
    expect(apiSource).toContain('/sales/team/${teamId}/arrangements/scenic-ticket-guests/export');
    expect(apiSource).toContain('requestClient.download<Blob>');
    expect(arrangementSource).toContain('getRelationTicketTemplateDetail');
    expect(arrangementSource).toContain('scenicTicketTemplate');
    expect(arrangementSource).toContain('selectedScenicResourceRelation');
    expect(arrangementSource).toContain('loadSelectedScenicTicketTemplate');
    expect(arrangementSource).toContain('downloadScenicTicketGuests');
    expect(arrangementSource).toContain('当前景区和供应商未匹配到启用采购关系，请重新选择后再下载游客名单');
    expect(arrangementSource).toContain('当前景区供应商未配置游客名单模板，请先配置模板');
    expect(arrangementSource).toContain('@download-scenic-ticket-guests="downloadScenicTicketGuests"');
    expect(editorSource).toContain('download-scenic-ticket-guests');
    expect(editorSource).toContain('showScenicTicketDownload');
    expect(editorSource).toContain('下载游客Excel');
    expect(productArrangementSource).not.toContain('@download-scenic-ticket-guests');
  });

  it('derives arrangement section status from real arrangement rows', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const layoutSource = readAppFile('src/views/sales/team-arrangement-layout.css');

    expect(arrangementSource).toContain('function sectionStatusText');
    expect(arrangementSource).toContain('function sectionStatusTone');
    expect(arrangementSource).toContain('function sectionSummary');
    expect(arrangementSource).toContain("arrangementsByType(type).length");
    expect(arrangementSource).toContain('arrangement-status-badge');
    expect(arrangementSource).toContain('arrangement-section-summary');
    expect(arrangementSource).toContain('sectionSummary(section.value)');
    expect(layoutSource).toContain('.arrangement-status-badge');
    expect(layoutSource).toContain('.arrangement-summary-chip');
  });

  it('calculates formal team budget profit using the old-system gross profit formula', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const regularCostSnippet = arrangementSource.slice(
      arrangementSource.indexOf('const regularArrangementCostTotal'),
      arrangementSource.indexOf('const teamTravelDays'),
    );
    const budgetProfitSnippet = arrangementSource.slice(
      arrangementSource.indexOf('const budgetProfit'),
      arrangementSource.indexOf('const teamTravelDays'),
    );

    expect(arrangementSource).toContain('const regularArrangementCostTotal = computed(() =>');
    expect(regularCostSnippet).toContain("!['optional', 'shopping'].includes(item.arrangementType)");
    expect(arrangementSource).toContain('item.totalAmount || item.costAmount');
    expect(arrangementSource).toContain('const optionalCompanyProfitTotal = computed(() =>');
    expect(regularCostSnippet).toContain('numericMoney(item.saleAmount)');
    expect(regularCostSnippet).toContain('numericMoney(item.costAmount)');
    expect(regularCostSnippet).toContain('numericMoney(item.guideCommissionAmount)');
    expect(arrangementSource).toContain('const shoppingCompanyProfitTotal = computed(() =>');
    expect(regularCostSnippet).toContain('numericMoney(item.headFeeAmount)');
    expect(regularCostSnippet).toContain('numericMoney(item.companyRebateAmount)');
    expect(budgetProfitSnippet).toContain('orderReceivable.value');
    expect(budgetProfitSnippet).toContain('optionalCompanyProfitTotal.value');
    expect(budgetProfitSnippet).toContain('shoppingCompanyProfitTotal.value');
    expect(budgetProfitSnippet).toContain('regularArrangementCostTotal.value');
    expect(budgetProfitSnippet).toContain('guideFeeTotal.value');
    expect(budgetProfitSnippet).not.toContain('guideOperationFeeTotal');
    expect(budgetProfitSnippet).not.toContain('guideImprestTotal');
    expect(arrangementSource).toContain("{ key: 'profit', label: '预算利润', value: formatPlainMoney(budgetProfit.value) }");
    expect(arrangementSource).not.toContain("{ key: 'profit', label: '预算利润', value: '0 元' }");
  });

  it('opens an old-system style gross profit preview from budget profit and print actions', () => {
    const routeSource = readAppFile('src/router/routes/modules/sales.ts');
    const apiSource = readAppFile('src/api/sales/team.ts');
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const previewSource = readAppFile('src/views/sales/team/gross-profit.vue');

    expect(routeSource).toContain('TeamGrossProfitPage');
    expect(routeSource).toContain("path: '/sales/team/gross-profit/:id'");
    expect(routeSource).toContain("title: '团队毛利表'");
    expect(apiSource).toContain('GrossProfitPreview');
    expect(apiSource).toContain('getSalesTeamGrossProfitPreview');
    expect(apiSource).toContain('exportSalesTeamGrossProfit');
    expect(apiSource).toContain('/sales/team/${teamId}/gross-profit/preview');
    expect(apiSource).toContain('/sales/team/${teamId}/gross-profit/export');
    expect(apiSource).toContain('requestClient.download<Blob>');
    expect(arrangementSource).toContain('openGrossProfitPreview');
    expect(arrangementSource).toContain('router.push(`/sales/team/gross-profit/${teamId.value}`)');
    expect(arrangementSource).toContain("action.label === '打印毛利表'");
    expect(arrangementSource).toContain("item.key === 'profit'");
    expect(arrangementSource).toContain('@click="handleMetricClick(item)"');
    expect(arrangementSource).toContain('team-metric-item clickable');
    expect(previewSource).toContain('团队毛利表(预算)');
    expect(previewSource).toContain('Word文件');
    expect(previewSource).toContain('Pdf文件');
    expect(previewSource).toContain('在线打印');
    expect(previewSource).toContain('getSalesTeamGrossProfitPreview');
    expect(previewSource).toContain('exportSalesTeamGrossProfit');
    expect(previewSource).toContain("exportGrossProfit('docx')");
    expect(previewSource).toContain("exportGrossProfit('pdf')");
    expect(previewSource).toContain('window.print()');
    expect(previewSource).toContain('triggerBlobDownload');
    expect(previewSource).toContain('订单收入');
    expect(previewSource).toContain('购物反佣');
    expect(previewSource).toContain('加点利润');
    expect(previewSource).toContain('成本支出');
    expect(previewSource).toContain('导服费');
    expect(previewSource).toContain('合计毛利');
    expect(previewSource).not.toContain('传真号码');
  });

  it('keeps vehicle driver and plate history suggestions on formal team arrangement pages', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const editorSource = readAppFile('src/views/sales/components/ArrangementEditorModal.vue');

    expect(editorSource).toContain(':options="driverHistoryOptions"');
    expect(editorSource).toContain(':options="vehiclePlateHistoryOptions"');
    expect(editorSource).toContain("@search=\"(value) => emit('vehicle-history-search', 'driver_info', value)\"");
    expect(editorSource).toContain("@search=\"(value) => emit('vehicle-history-search', 'vehicle_plate', value)\"");
    expect(arrangementSource).toContain('getVehicleUsageHistorySuggestions');
    expect(arrangementSource).toContain('recordVehicleUsageHistory');
    expect(arrangementSource).toContain('loadVehicleHistoryOptions(');
    expect(arrangementSource).toContain("loadVehicleHistoryOptions('driver_info')");
    expect(arrangementSource).toContain("loadVehicleHistoryOptions('vehicle_plate')");
    expect(arrangementSource).toContain('driverHistoryOptions.value = options');
    expect(arrangementSource).toContain('vehiclePlateHistoryOptions.value = options');
    expect(arrangementSource).toContain("recordVehicleHistoryUsage('driver_info', arrangementForm.driverName)");
    expect(arrangementSource).toContain("recordVehicleHistoryUsage('vehicle_plate', arrangementForm.vehiclePlate)");
  });

  it('saves every formal team arrangement category through the shared cost API', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const editorSource = readAppFile('src/views/sales/components/ArrangementEditorModal.vue');
    const editorModelSource = readAppFile('src/views/sales/components/arrangement-editor-model.ts');
    const apiSource = readAppFile('src/api/sales/team.ts');

    expect(arrangementSource).not.toContain("activeEditorType.value !== 'traffic'");
    expect(arrangementSource).not.toContain('当前先接入大交通正式保存');
    expect(arrangementSource).toContain('validateArrangementDraft');
    expect(arrangementSource).toContain('activeArrangementLabel');
    expect(arrangementSource).toContain('priceLineAmountForSubmit');
    expect(arrangementSource).toContain("activeEditorType.value === 'shopping'");
    expect(arrangementSource).toContain("activeEditorType.value === 'optional'");
    expect(arrangementSource).toContain("activeEditorType.value === 'ground_agent'");
    expect(arrangementSource).toContain('createGroundAgentPackagePriceLine');
    expect(arrangementSource).toContain('resolveGroundAgentPackageAmount');
    expect(arrangementSource).toContain(':editor-total-amount="activeEditorTotalAmount"');
    expect(arrangementSource).toContain("'开始', '结束', '天数', '供应商', '备注', '成本合计', '现结', '挂账', '操作'");
    expect(arrangementSource).not.toContain("'开始', '结束', '天数', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'");
    expect(arrangementSource).toContain('settlementType: arrangementForm.settlementType');
    expect(arrangementSource).toContain('mealType: arrangementForm.mealType');
    expect(arrangementSource).toContain('fundIncluded: arrangementForm.fundIncluded');
    expect(arrangementSource).toContain('confirmed: arrangementForm.confirmed');
    expect(arrangementSource).toContain('confirmationNo: arrangementForm.confirmationNo');
    expect(arrangementSource).toContain('guideId: arrangementForm.guideId');
    expect(arrangementSource).toContain('guideName: arrangementForm.guideName');
    expect(arrangementSource).toContain('message.success(`${activeArrangementLabel()}安排已保存`)');
    expect(arrangementSource).toContain('多订单均摊成本至少选择两个订单');
    expect(arrangementSource).toContain('多订单均摊成本时，价格信息只能保留一条记录');
    expect(arrangementSource).toContain('已选择“无需导游报账”，现结金额须为0！');
    expect(editorModelSource).toContain("extra_fee: {\n    noGuideReport: true");
    expect(editorModelSource).toContain("shopping: {\n    noGuideReport: true");
    expect(editorModelSource).toContain('showOrderInfo: true');
    expect(editorSource).toContain("showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条消费详情'");
    expect(apiSource).toContain('settlementType?: SalesProductApi.SettlementType;');
    expect(apiSource).toContain('mealType?: string;');
    expect(apiSource).toContain('fundIncluded?: string;');
    expect(apiSource).toContain('confirmationNo?: string;');
  });
});

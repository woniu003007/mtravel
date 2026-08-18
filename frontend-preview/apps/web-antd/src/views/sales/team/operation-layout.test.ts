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

describe('sales team list layout', () => {
  it('aligns the team search area with the order management search layout', () => {
    const source = readAppFile('src/views/sales/team/index.vue');

    expect(source).toContain("import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';");
    expect(source).toContain('advancedSearchOpen');
    expect(source).not.toContain('grid-class="team-search-grid"');
    expect(source).not.toContain('actions-class="team-filter-actions"');
    expect(source).not.toContain('label-width="72px"');
    expect(source).toContain('class="team-search-item" label="团号"');
    expect(source).toContain('class="business-search-item--wide" label="出团日期"');
    expect(source).toContain('<DatePicker.RangePicker');
    expect(source).toContain('<template v-if="advancedSearchOpen">');
    expect(source).toContain('class="team-search-item team-advanced-search-item" label="导游"');
    expect(source).toContain('class="team-search-item team-advanced-search-item" label="部门"');
    expect(source).toContain('class="team-search-item team-advanced-search-item" label="订单状态"');
    expect(source).toContain('class="team-search-item team-advanced-search-item" label="添加日期"');
    expect(source).toContain('class="team-table-toolbar"');
    expect(source).toContain('团队列表');
    expect(source).toContain('共 {{ pagination.total || 0 }} 条');
    expect(source).toContain('guideKeyword: clean(query.guideKeyword)');
    expect(source).toContain('departmentName: query.departmentName');
    expect(source).toContain('orderStatus: query.orderStatus');
    expect(source).toContain('addDate: dateParam(queryDates.addDate)');
    expect(source).toContain('getEnterpriseDepartmentAll');
    expect(source).toContain('loadDepartmentOptions');
    expect(source).not.toContain('disabled placeholder="导游"');
    expect(source).not.toContain('disabled allow-clear :options="orderStatusOptions"');
    expect(source).not.toContain('disabled placeholder="添加日期"');
    expect(source).not.toContain('class="team-search-actions"');
    expect(source).not.toContain('grid-template-columns: 1.25fr 1.1fr 0.8fr');
  });

  it('keeps long team names inside a fixed-width ellipsis cell with hover title', () => {
    const source = readAppFile('src/views/sales/team/index.vue');

    expect(source).toContain("className: 'team-name-column'");
    expect(source).toContain("{ className: 'team-name-column', key: 'teamName', title: '团队名称', width: 318 }");
    expect(source).toContain(`<Tooltip :title="teamRow(record).productName || '--'">`);
    expect(source).toContain('class="team-name-cell"');
    expect(source).toContain('class="team-name-link"');
    expect(source).toContain('.team-table :deep(.team-name-column)');
    expect(source).toContain('width: 318px !important;');
    expect(source).toContain('max-width: 318px !important;');
    expect(source).toContain('.team-name-cell');
    expect(source).toContain('width: 318px;');
    expect(source).toContain('.team-name-link');
    expect(source).toContain('width: 100%;');
    expect(source).toContain('.team-name-link :deep(span)');
    expect(source).toContain('max-width: 100%;');
    expect(source).toContain('overflow: hidden;');
    expect(source).toContain('text-overflow: ellipsis;');
    expect(source).toContain('white-space: nowrap;');
  });

  it('passes customer keyword and jumps arrangement icons to their section anchors', () => {
    const source = readAppFile('src/views/sales/team/index.vue');
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');

    expect(source).toContain('customerKeyword: clean(query.customerKeyword)');
    expect(source).toContain('const arrangementAnchorMap: Record<string, string>');
    expect(source).toContain("guidePlan: 'guide-arrangement'");
    expect(source).toContain("trafficPlan: 'part1'");
    expect(source).toContain("hotelPlan: 'part2'");
    expect(source).toContain("groundAgentPlan: 'part9'");
    expect(source).toContain('function openArrangementSection(record: TeamListItem, key: string)');
    expect(source).toContain('router.push(`/sales/team/arrangement/${record.id}#${anchor}`)');
    expect(source).toContain('class="arrange-jump-button"');
    expect(source).toContain('@click="openArrangementSection(teamRow(record), String(column.key))"');
    expect(arrangementSource).toContain('async function scrollToRouteHashAnchor()');
    expect(arrangementSource).toContain("const anchor = route.hash?.replace('#', '')");
    expect(arrangementSource).toContain('await scrollToRouteHashAnchor();');
  });

  it('shows product-copied arrangement prices as references before operators enter real quantities', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');

    expect(arrangementSource).toContain('参考单价');
    expect(arrangementSource).toContain('if (quantity <= 0)');
    expect(arrangementSource).toContain("return Number(record.peopleCount || 0) > 0 ? String(record.peopleCount) : '--';");
  });

  it('shows guide imprest approval records from existing imprest applications', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');

    expect(arrangementSource).toContain('getGuideImprestPage');
    expect(arrangementSource).toContain('getGuideImprestDetail');
    expect(arrangementSource).toContain('openGuideImprestRecords(record)');
    expect(arrangementSource).toContain('导游备用金审批记录');
    expect(arrangementSource).toContain('审批记录');
    expect(arrangementSource).toContain('guideImprestRecordRows');
    expect(arrangementSource).toContain('申请编号');
    expect(arrangementSource).toContain('申请金额');
    expect(arrangementSource).toContain('审批人 / 时间');
    expect(arrangementSource).toContain('已付金额');
    expect(arrangementSource).toContain('余额');
    expect(arrangementSource).toContain('查看详情');
    expect(arrangementSource).toContain('guideImprestRecordDetailOpen');
    expect(arrangementSource).toContain('title="备用金申请详情"');
    expect(arrangementSource).toContain('teamId: team.value?.id');
    expect(arrangementSource).toContain('guideId: record.guideId');
  });

  it('keeps actual shopping reconciliation out of the arrangement page', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');

    expect(arrangementSource).not.toContain('购物业绩');
    expect(arrangementSource).not.toContain('购物业绩与公司补佣');
    expect(arrangementSource).not.toContain('openShoppingCommissionModal');
  });

  it('fills arrangement header guide from arranged guide rows and leader from booking guests', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const teamDtoSource = readAppFile('../../../backend/src/main/java/com/mtravel/platform/sales/team/dto/SalesTeamOperationResponse.java');

    expect(arrangementSource).toContain('const arrangedGuideSummary = computed');
    expect(arrangementSource).toContain('teamGuides.value.map');
    expect(arrangementSource).toContain('item.guideName');
    expect(arrangementSource).toContain('item.guideMobile');
    expect(arrangementSource).toContain("{ color: 'orange', label: '导游', value: arrangedGuideSummary.value }");
    expect(arrangementSource).toContain("{ color: 'green', label: '领队', value: team.value?.leaderSummary || '--' }");
    expect(teamDtoSource).toContain('String leaderSummary');
  });

  it('moves shopping reconciliation to single-team audit and operation entries', () => {
    const operationSource = readAppFile('src/views/sales/team/operation.vue');
    const financeAuditSource = readAppFile('src/views/finance/team-audit/index.vue');
    const financeRouteSource = readAppFile('src/router/routes/modules/finance.ts');

    expect(operationSource).toContain('ShoppingReconciliationModal');
    expect(operationSource).toContain('openShoppingReconciliationModal(team.value.id)');
    expect(financeAuditSource).toContain('ShoppingReconciliationModal');
    expect(financeAuditSource).toContain('getSalesTeamPage');
    expect(financeAuditSource).toContain('openShoppingReconciliationModal');
    expect(financeAuditSource).toContain('购物核对/补佣');
    expect(financeRouteSource).toContain("path: '/finance/team-audit'");
    expect(financeRouteSource).toContain("hideInMenu: true, title: '财务团队审核'");
  });

  it('keeps customer and guide summaries clipped inside their table cells', () => {
    const source = readAppFile('src/views/sales/team/index.vue');

    expect(source).toContain("{ className: 'team-customer-column', key: 'customer', title: '客户', width: 170 }");
    expect(source).toContain("{ className: 'team-guide-summary-column', key: 'guideSummary', title: '导游信息', width: 132 }");
    expect(source).toContain('class="team-summary-cell team-customer-cell"');
    expect(source).toContain('class="team-summary-cell team-guide-cell"');
    expect(source).toContain('.team-table :deep(.team-customer-column)');
    expect(source).toContain('width: 170px !important;');
    expect(source).toContain('.team-table :deep(.team-guide-summary-column)');
    expect(source).toContain('width: 132px !important;');
    expect(source).toContain('-webkit-line-clamp: 2;');
    expect(source).toContain('-webkit-box-orient: vertical;');
    expect(source).toContain('.team-guide-cell');
    expect(source).toContain('text-overflow: ellipsis;');
    expect(source).toContain('white-space: nowrap;');
  });
});

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
    expect(source).not.toContain('getControlledRoomResourceAll');
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
    const headerSource = readAppFile('src/views/sales/components/FormalTeamPageHeader.vue');
    const apiSource = readAppFile('src/api/sales/team.ts');
    const orderSource = readAppFile('src/views/sales/order/index.vue');
    const mergeModalSource = readAppFile('src/views/sales/components/MergeOrderModal.vue');

    expect(source).toContain('FormalTeamPageHeader');
    expect(source).toContain("import '../team-arrangement-layout.css'");
    expect(source).toContain('team-arrangement-card');
    expect(source).toContain('formal-team-arrangement-card');
    expect(source).toContain('formal-operation-actions');
    expect(source).toContain('operation-action-grid');
    expect(source).toContain('.operation-action-grid {');
    expect(source).toContain('flex-wrap: nowrap;');
    expect(source).toContain('overflow-x: auto;');
    expect(source).toContain('scrollbar-width: thin;');
    expect(source).toContain('operation-action-tile');
    expect(source).toContain('formal-description-stack');
    expect(source).not.toContain('team-operation-shell');
    expect(source).not.toContain('team-operation-header');
    expect(headerSource).toContain('top-tool-actions');
    expect(source).not.toContain('operation-flow-row');
    expect(source).not.toContain('team-profile-block');
    expect(source).not.toContain('team-description-stack');
    expect(source).not.toContain('operation-icon-actions');
    expect(source).toContain('metric-teamNo');
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
    expect(source).toContain("action.code === 'shoppingReconciliation' && team.value?.id");
    expect(source).toContain('openShoppingReconciliationModal(team.value.id)');
    expect(source).toContain('ShoppingReconciliationModal');
    expect(source).toContain("action.code === 'editTeam' && team.value?.id");
    expect(source).toContain('router.push(`/sales/team/edit/${team.value.id}`)');
    expect(source).not.toContain('router.push(`/sales/product/team-arrangement/${team.value.productId}`)');
    expect(source).not.toContain('router.push(`/sales/product/schedule/${team.value.productId}`)');
    expect(source).toContain('该团相关订单');
    expect(source).toContain('ORDER_TABLE_SCROLL_X');
    expect(source).toContain('order-cell-clamp');
    expect(source).toContain('order-edit-button');
    expect(source).toContain('修改');
    expect(source).toContain('MergeOrderModal');
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
    expect(mergeModalSource).toContain('title="拼团操作"');
    expect(mergeModalSource).toContain('merge-team-search-form');
    expect(mergeModalSource).toContain('merge-team-table');
    expect(mergeModalSource).toContain('merge-team-select-cell');
    expect(mergeModalSource).toContain('merge-team-row-selected');
    expect(mergeModalSource).toContain('mergeTeamColumns');
    expect(mergeModalSource).toContain('mergeTeamPagination');
    expect(mergeModalSource).toContain("teamType: 'sanpin'");
    expect(mergeModalSource).toContain('拼团目标团队只显示散拼');
    expect(mergeModalSource).not.toContain("teamType: mergeTeamSearchForm");
    expect(source).toContain('canTransferOrder');
    expect(source).toContain('transferDisabledReason');
    expect(source).toContain('validateSelectedTransferOrders');
    expect(source).toContain('已取消订单不能拼团或转团');
    expect(source).not.toContain('已拼出订单不能再次拼团或转团');
    expect(source).toContain('getCheckboxProps');
    expect(mergeModalSource).toContain('选择团期');
    expect(mergeModalSource).toContain('团号/团队名称');
    expect(mergeModalSource).toContain('客户单位');
    expect(mergeModalSource).toContain('出团日期始');
    expect(mergeModalSource).toContain('出团日期止');
    expect(mergeModalSource).toContain('天数');
    expect(mergeModalSource).toContain('预控/实收');
    expect(mergeModalSource).not.toContain('预控/实收/余位');
    expect(mergeModalSource).toContain('已选目标团');
    expect(mergeModalSource).toContain('已选目标团 {{ selectedTargetTeamIds.length }} 个');
    expect(mergeModalSource).toContain('清空选择');
    expect(mergeModalSource).toContain('targetTeamIds');
    expect(mergeModalSource).not.toContain('merge-selected-orders');
    expect(mergeModalSource).toContain('merge-operation-table-wrap');
    expect(mergeModalSource).toContain('merge-operation-table');
    expect(mergeModalSource).toContain('merge-operation-detail-table');
    expect(mergeModalSource).toContain('merge-target-row');
    expect(mergeModalSource).toContain('merge-order-row');
    expect(mergeModalSource).toContain('类型');
    expect(mergeModalSource).toContain('团号');
    expect(mergeModalSource).toContain('团队名称');
    expect(mergeModalSource).toContain('天数');
    expect(mergeModalSource).toContain('开始');
    expect(mergeModalSource).toContain('结束');
    expect(mergeModalSource).toContain('出发地');
    expect(mergeModalSource).toContain('merge-operation-panel');
    expect(mergeModalSource).toContain('overflow-x: hidden');
    expect(mergeModalSource).toContain('flex-shrink: 0;');
    expect(mergeModalSource).toContain('拼团备注');
    expect(mergeModalSource).toContain('人数摘要');
    expect(mergeModalSource).toContain('拼团单价');
    expect(mergeModalSource).toContain('价格类型');
    expect(mergeModalSource).toContain('客户单位');
    expect(mergeModalSource).toContain('merge-item-inline-price');
    expect(mergeModalSource).toContain('merge-item-inline-label');
    expect(mergeModalSource).toContain('merge-item-inline-price-input');
    expect(mergeModalSource).toContain('元 * {{ orderPeopleCount(order) }}人');
    expect(mergeModalSource).toContain('targetSeatSummary(target)');
    expect(mergeModalSource).toContain("{ label: '车费', value: '车费' }");
    expect(mergeModalSource).toContain("{ label: '综费', value: '综费' }");
    expect(mergeModalSource).toContain("{ label: '接送费', value: '接送费' }");
    expect(mergeModalSource).toContain("{ label: '代收团款', value: '代收团款' }");
    expect(mergeModalSource).toContain("{ label: '定金对公', value: '定金对公' }");
    expect(mergeModalSource).toContain("{ label: '团费', value: '团费' }");
    expect(mergeModalSource).toContain("{ label: '成本', value: '成本' }");
    expect(mergeModalSource).toContain("{ label: '其它', value: '其它' }");
    expect(mergeModalSource).toContain('默认价格 0');
    expect(mergeModalSource).toContain('执行拼团');
    expect(mergeModalSource).toContain('Checkbox v-model:checked="mergeForm.tagFlag"');
    expect(mergeModalSource).toContain('items: mergeItems.value.map');
    expect(mergeModalSource).toContain('mergeSalesOrders');
    expect(mergeModalSource).not.toContain('merge-team-card-list');
    expect(mergeModalSource).not.toContain('merge-target-panel');
    expect(mergeModalSource).not.toContain('merge-order-editor');
    expect(orderSource).toContain('MergeOrderModal');
    expect(orderSource).not.toContain('全局订单管理拼团操作待接入');
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
    expect(apiSource).toContain('targetTeamIds: number[];');
    expect(apiSource).toContain('items: MergeOrderItemParams[];');
    expect(apiSource).toContain('mergeSalesOrders');
    expect(apiSource).not.toContain('splits: MergeOrderSplitParams[];');

    expectTextOrder(source, [
      'FormalTeamPageHeader',
      'formal-description-stack',
      'formal-operation-actions',
      '该团相关订单',
    ]);

    expectTextOrder(source, ['产品说明', '收客须知', '内部备注']);
  });

  it('matches legacy inside memo fields on the team operation page', () => {
    const source = readAppFile('src/views/sales/team/operation.vue');
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const productArrangementSource = readAppFile('src/views/sales/product/team-arrangement.vue');
    const headerSource = readAppFile('src/views/sales/components/FormalTeamPageHeader.vue');
    const arrangementLayoutSource = readAppFile('src/views/sales/team-arrangement-layout.css');
    const apiSource = readAppFile('src/api/sales/team.ts');

    expect(apiSource).toContain('perCapitaPitAmount?: number;');
    expect(apiSource).toContain('optionalMarkupRate?: number;');
    expect(apiSource).toContain('perCapitaShoppingAmount?: number;');
    expect(source).toContain('perCapitaPitAmount');
    expect(source).toContain('optionalMarkupRate');
    expect(source).toContain('perCapitaShoppingAmount');
    expect(source).toContain('人均坑位');
    expect(source).toContain('自费加点率');
    expect(source).toContain('人均购物');
    expect(source).toContain('内部备注');
    expect(source).toContain('placeholder="填写导游、控房、控车、用餐和其它要求"');
    expect(source).toContain('addon-before="¥"');
    expect(source).toContain('addon-after="%"');
    expect(source).toContain(':note="content?.internalRemark || \'无\'"');
    expect(headerSource).toContain('class="internal-note-main"');
    expect(headerSource).toContain('class="internal-note-heading"');
    expect(headerSource).toContain('class="internal-note-text"');
    expect(source).toContain(':width="profileEditorType === \'internal_note\' ? 760 : 460"');
    expect(source).toContain('class="inside-memo-textarea"');
    expect(source).toContain(':auto-size="{ minRows: 10, maxRows: 16 }"');
    expect(source).toContain('DEFAULT_INTERNAL_REMARK_TEMPLATE');
    expect(source).toContain('content.value?.internalRemark || DEFAULT_INTERNAL_REMARK_TEMPLATE');
    expect(source).toContain('>导游要求：');
    expect(source).toContain('>控房要求：');
    expect(source).toContain('>控车要求：');
    expect(source).toContain('>用餐要求：');
    expect(source).toContain('>其它要求：');
    expect(arrangementSource).toContain('DEFAULT_INTERNAL_REMARK_TEMPLATE');
    expect(arrangementSource).toContain('content.value?.internalRemark || DEFAULT_INTERNAL_REMARK_TEMPLATE');
    expect(arrangementSource).toContain(':note="content?.internalRemark || \'未填写\'"');
    expect(arrangementSource).not.toContain('internalRemarkLines');
    expect(arrangementSource).not.toContain('class="internal-note-line"');
    expect(arrangementLayoutSource).toContain('.internal-note-main');
    expect(arrangementLayoutSource).toContain('.internal-note-heading');
    expect(arrangementLayoutSource).toContain('grid-template-columns: minmax(0, 1fr) auto;');
    expect(arrangementLayoutSource).toContain('grid-column: 1 / -1;');
    expect(arrangementLayoutSource).toContain('white-space: nowrap;');
    expect(arrangementLayoutSource).toContain('.internal-note-text');
    expect(arrangementLayoutSource).toContain('white-space: pre-wrap;');
    expect(arrangementLayoutSource).not.toContain('.internal-note-line');
    expect(arrangementSource).toContain(':width="quickProfileEditorType === \'internal_note\' ? 760 : 460"');
    expect(arrangementSource).toContain('class="inside-memo-textarea"');
    expect(arrangementSource).toContain(':auto-size="{ minRows: 10, maxRows: 16 }"');
    expect(productArrangementSource).toContain('perCapitaPitAmount?: number;');
    expect(productArrangementSource).toContain('optionalMarkupRate?: number;');
    expect(productArrangementSource).toContain('perCapitaShoppingAmount?: number;');
    expect(productArrangementSource).toContain(':note="teamProfile.internalNote || \'未填写\'"');
    expect(productArrangementSource).toContain(':width="quickProfileEditorType === \'internal_note\' ? 760 : 460"');
    expect(productArrangementSource).toContain('placeholder: \'填写导游、控房、控车、用餐和其它要求\'');
    expect(productArrangementSource).toContain('DEFAULT_INTERNAL_REMARK_TEMPLATE');
    expect(productArrangementSource).toContain('teamProfile.internalNote ||= DEFAULT_INTERNAL_REMARK_TEMPLATE;');
    expect(productArrangementSource).toContain('v-if="quickProfileEditorType === \'internal_note\'"');
    expect(productArrangementSource).toContain('class="inside-memo-textarea"');
    expect(productArrangementSource).toContain(':auto-size="{ minRows: 10, maxRows: 16 }"');
    expect(productArrangementSource).toContain(':maxlength="500"');
    expect(productArrangementSource).toContain('v-model:value="teamProfile.perCapitaPitAmount"');
    expect(productArrangementSource).toContain('v-model:value="teamProfile.optionalMarkupRate"');
    expect(productArrangementSource).toContain('v-model:value="teamProfile.perCapitaShoppingAmount"');
    expect(productArrangementSource).not.toContain(':maxlength="100"');
  });

  it('uses team optional markup rate as guide imprest default anchor', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');

    expect(arrangementSource).toContain('function teamOptionalMarkupRateAnchor()');
    expect(arrangementSource).toContain('content.value?.optionalMarkupRate');
    expect(arrangementSource).toContain('return rate > 0 ? rate : undefined;');
    expect(arrangementSource).toContain('guideImprestCompanyMarkupRate.value = teamOptionalMarkupRateAnchor();');
    expect(arrangementSource).toContain('companyMarkupRate: guideImprestCompanyMarkupRate.value');
  });

  it('shows guide arrangement as a compact list with modal editing', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');

    expect(arrangementSource).toContain('guide-arrangement-table-wrap');
    expect(arrangementSource).toContain('guide-arrangement-table');
    expect(arrangementSource).toContain('guide-name-line');
    expect(arrangementSource).toContain('guide-phone-line');
    expect(arrangementSource).toContain('guide-time-range');
    expect(arrangementSource).toContain('guide-money-stack');
    expect(arrangementSource).toContain('guide-imprest-inline-actions');
    expect(arrangementSource).toContain('请先添加导游后再测算备用金');
    expect(arrangementSource).toContain(':disabled="!teamGuides.length"');
    expect(arrangementSource).toContain('openGuideImprestEntry');
    expect(arrangementSource).toContain('备用金测算');
    expect(arrangementSource).toContain('保存导游后进行备用金测算');
    expect(arrangementSource).not.toContain('v-model:value="guideDraft.imprestAmount"');
    expect(arrangementSource).not.toContain('v-model:value="guideEditDraft.imprestAmount"');
    expect(arrangementSource).toContain('guide-remark-summary');
    expect(arrangementSource).toContain('guide-row-actions');
    expect(arrangementSource).toContain('openGuideEditModal(record)');
    expect(arrangementSource).toContain('openGuidePickerForRow(record)');
    expect(arrangementSource).toContain('submitGuideEditDraft');
    expect(arrangementSource).toContain('title="修改导游安排"');
    expect(arrangementSource).toContain('添加导游');
    expect(arrangementSource).toContain('更换导游');
    expect(arrangementSource).toContain('修改');
    expect(arrangementSource).not.toContain('class="guide-plan-row guide-plan-card"');
    expect(arrangementSource).not.toContain('class="guide-add-card"');
    expect(arrangementSource).not.toContain('guide-note-textarea');
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
    expect(arrangementSource).toContain('function formatCostCashMoney');
    expect(productArrangementSource).toContain('function formatCostCashMoney');
    expect(arrangementSource).toContain('function formatCostDetailMoney');
    expect(productArrangementSource).toContain('function formatCostDetailMoney');
    expect(arrangementSource).toContain('return formatMoney(value);');
    expect(productArrangementSource).toContain('return formatMoney(value);');
    expect(arrangementSource).not.toContain("return numericMoney(value) === 0 ? '0.00' : formatMoney(value);");
    expect(productArrangementSource).not.toContain("return Number(value || 0) === 0 ? '0.00' : formatMoney(value);");
    expect(arrangementSource).not.toContain("return numericMoney(value) === 0 ? '--' : formatMoney(value);");
    expect(productArrangementSource).not.toContain("return Number(value || 0) === 0 ? '--' : formatMoney(value);");
    expect(arrangementSource).toContain("costAmountClass(item.cash, 'cost-amount-cash')");
    expect(arrangementSource).toContain("formatCostCashMoney(item.cash)");
    expect(arrangementSource).toContain("costAmountClass(item.credit, 'cost-amount-credit')");
    expect(arrangementSource).toContain("formatCostDetailMoney(item.credit)");
    expect(productArrangementSource).toContain("costAmountClass(arrangementSettlementTotal(item.value, 'cash'), 'cost-amount-cash')");
    expect(productArrangementSource).toContain("formatCostCashMoney(arrangementSettlementTotal(item.value, 'cash'))");
    expect(productArrangementSource).toContain("costAmountClass(arrangementSettlementTotal(item.value, 'credit'), 'cost-amount-credit')");
    expect(productArrangementSource).toContain("formatCostDetailMoney(arrangementSettlementTotal(item.value, 'credit'))");
    expect(arrangementSource).toContain('<th>现结</th>');
    expect(productArrangementSource).toContain('<th>现结</th>');
    expect(arrangementLayoutSource).toContain('.cost-overview-summary');
    expect(arrangementLayoutSource).toContain('grid-template-columns: minmax(190px, 1.35fr)');
    expect(arrangementLayoutSource).toContain('overflow-x: hidden;');
    expect(arrangementLayoutSource).toContain('min-width: 0;');
    expect(arrangementLayoutSource).toContain('table-layout: fixed;');
    expect(arrangementLayoutSource).not.toContain('min-width: 1500px;');
    expect(arrangementLayoutSource).toContain('.cost-summary-card--primary');
    expect(arrangementLayoutSource).toContain('.cost-summary-card--strong');
    expect(arrangementLayoutSource).toContain('.cost-summary-amount');
    expect(arrangementLayoutSource).toContain('font-size: 20px;');
    expect(arrangementLayoutSource).toContain('.cost-amount-zero');
    expect(arrangementLayoutSource).toContain('.cost-amount-nonzero');
    expect(arrangementLayoutSource).toContain('.cost-amount-cash');
    expect(arrangementLayoutSource).toContain('.cost-amount-credit');
    expect(arrangementLayoutSource).toContain('letter-spacing: 0;');
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
    const operationSource = readAppFile('src/views/sales/team/operation.vue');
    const headerSource = readAppFile('src/views/sales/components/FormalTeamPageHeader.vue');
    const editorSource = readAppFile('src/views/sales/components/ArrangementEditorModal.vue');
    const arrangementLayoutSource = readAppFile('src/views/sales/team-arrangement-layout.css');

    expect(routeSource).toContain("path: '/sales/team/arrangement/:id'");
    expect(arrangementSource).toContain('() => route.params.id,');
    expect(arrangementSource).toContain('切换到另一团时必须重新读取安排、汇总和分类状态');
    expect(routeSource).toContain("path: '/sales/team/edit/:id'");
    expect(routeSource).toContain("title: '修改团队'");
    expect(routeSource).toContain("title: '团队安排总览'");
    expect(arrangementSource).toContain("import '../team-arrangement-layout.css'");
    expect(productArrangementSource).toContain("import '../team-arrangement-layout.css'");
    expect(operationSource).toContain("import '../team-arrangement-layout.css'");
    expect(arrangementSource).toContain('FormalTeamPageHeader');
    expect(productArrangementSource).toContain('FormalTeamPageHeader');
    expect(operationSource).toContain('FormalTeamPageHeader');
    expect(headerSource).toContain("name: 'FormalTeamPageHeader'");
    expect(headerSource).toContain('formal-team-page-header');
    expect(headerSource).toContain('arrangement-command-bar');
    expect(headerSource).toContain('workflow-rail');
    expect(headerSource).toContain('formal-team-badges-line');
    expect(headerSource).toContain('formal-team-metric-strip');
    expect(headerSource).toContain('internal-note-main');
    expect(headerSource).toContain('团队安排总览');
    expect(headerSource).toContain('Group Arrange');
    expect(arrangementSource).toContain('team-arrangement-card');
    expect(arrangementSource).toContain('formal-team-arrangement-card');
    expect(headerSource).toContain('formal-arrangement-tool-strip');
    expect(headerSource).toContain('formal-tool-strip-title');
    expect(headerSource).toContain('formal-tool-strip-actions');
    expect(headerSource).toContain('formal-arrangement-tool-button');
    expect(headerSource).toContain('command-side');
    expect(headerSource).toContain('workflow-rail');
    expect(headerSource).toContain('stage-flow-item');
    expect(headerSource).toContain('stage-index');
    expect(headerSource).toContain('stage-label');
    expect(arrangementSource).toContain('arrangement-tabs-block');
    expect(productArrangementSource).toContain('arrangement-tabs-block');
    expect(arrangementSource).not.toContain('formal-arrangement-tabs-block');
    expect(arrangementSource).toContain('arrangement-icon-grid compact-category-strip');
    expect(headerSource).toContain('team-title-line formal-team-title-line');
    expect(headerSource).toContain('team-badges formal-team-badges');
    expect(headerSource).toContain('formal-team-badges-line');
    expect(headerSource).toContain('team-metric-strip formal-team-metric-strip');
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
    expect(headerSource).toContain(':class="[`metric-${metric.key}`, { clickable: metric.clickable }]"');
    expect(arrangementLayoutSource).toContain('.metric-travelDays');
    expect(arrangementLayoutSource).toContain('.metric-orderStatus');
    expect(arrangementLayoutSource).toContain('.formal-team-arrangement-card .formal-team-badges-line');
    expect(arrangementLayoutSource).toContain('flex-wrap: nowrap;');
    expect(arrangementLayoutSource).toContain('grid-template-columns: minmax(0, 1fr) 520px auto;');
    expect(arrangementLayoutSource).toContain('grid-column: 1 / -1;');
    expect(arrangementLayoutSource).toContain('grid-column: 3;');
    expect(arrangementLayoutSource).toContain('grid-row: 1;');
    expect(arrangementLayoutSource).toContain('grid-row: 2;');
    expect(arrangementLayoutSource).toContain('white-space: nowrap;');
    expect(arrangementLayoutSource).toContain('.metric-paid');
    expect(arrangementLayoutSource).toContain('.metric-profit');
    expect(arrangementLayoutSource).toContain('flex-wrap: nowrap;');
    expect(arrangementLayoutSource).toContain('width: fit-content;');
    expect(arrangementLayoutSource).toContain('max-width: 100%;');
    expect(arrangementLayoutSource).toContain('flex: 0 0 max-content;');
    expect(arrangementLayoutSource).toContain('min-width: max-content;');
    expect(arrangementLayoutSource).toContain('text-overflow: clip;');
    expect(arrangementLayoutSource).toContain('.formal-team-arrangement-card .arrangement-section-header');
    expect(arrangementLayoutSource).toContain('.formal-team-arrangement-card .arrangement-section-table th');
    expect(arrangementLayoutSource).toContain('.formal-team-arrangement-card .cost-overview-summary');
    expect(arrangementLayoutSource).toContain('grid-template-columns: minmax(170px, 1.15fr)');
    expect(arrangementLayoutSource).toContain('.formal-team-arrangement-card .cost-summary-card');
    expect(arrangementLayoutSource).toContain('padding: 8px 12px;');
    expect(arrangementLayoutSource).toContain('.formal-team-arrangement-card .arrangement-overview-tab');
    expect(arrangementLayoutSource).toContain('height: 30px;');
    expect(arrangementLayoutSource).toContain('.formal-team-arrangement-card .arrangement-icon-grid');
    expect(arrangementLayoutSource).toContain('min-height: 46px;');
    expect(arrangementLayoutSource).toContain('.formal-team-arrangement-card .arrangement-section-heading');
    expect(arrangementLayoutSource).toContain('flex-direction: row;');
    expect(arrangementLayoutSource).toContain('align-items: center;');
    expect(arrangementSource).toContain('.guide-arrangement-section .arrangement-section-header');
    expect(arrangementSource).toContain('align-items: center;');
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
    expect(arrangementSource).toContain('formalHeaderActions');
    expect(arrangementSource).toContain("key: 'itinerary'");
    expect(arrangementSource).toContain('@action-click="handleFormalHeaderAction"');
    expect(arrangementSource).toContain('function handleFormalHeaderAction(action: { key: string })');
    expect(arrangementSource).toContain("if (action.key === 'itinerary')");
    expect(arrangementSource).not.toContain("@click=\"handleToolAction({ label: '查看行程' })\"");
    expect(arrangementSource).toContain('quickProfileEditorOpen');
    expect(arrangementSource).toContain('openTeamProfileEditor');
    expect(arrangementSource).toContain('saveTeamProfileEditor');
    expect(arrangementSource).toContain('getProductDictionaryAll');
    expect(arrangementSource).toContain('getEnterpriseDepartmentAll');
    expect(arrangementSource).toContain('getEnterpriseEmployeeAll');
    expect(arrangementSource).toContain('saveSalesTeam');
    expect(headerSource).toContain(':class="{ editable: badge.editable }"');
    expect(headerSource).toContain("@click=\"badge.editable && emit('badgeClick', badge)\"");
    expect(arrangementSource).toContain('@badge-click="handleFormalHeaderBadge"');
    expect(arrangementSource).toContain('function handleFormalHeaderBadge(badge: { label: string })');
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

  it('supports Word-imported ISO schedule dates while retaining legacy relative-day arrangements', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');

    expect(arrangementSource).toContain('function parseScheduleIsoDate(value?: string)');
    expect(arrangementSource).toContain("departureDate.add(index, 'day').format('YYYY-MM-DD')");
    expect(arrangementSource).toContain('`${value}（第${dayNo}天）`');
    expect(arrangementSource).toContain('`第${dayNo}天（历史）`');
    expect(arrangementSource).toContain('[arrangementForm.scheduleStartDay, arrangementForm.scheduleEndDay]');
    expect(arrangementSource).toContain('function scheduleNightsCount(startValue?: string, endValue?: string)');
    expect(arrangementSource).toContain('function scheduleDaysCount(startValue?: string, endValue?: string)');
    expect(arrangementSource).toContain('arrangementForm.daysCount = scheduleNightsCount(');
    expect(arrangementSource).toContain('const days = scheduleDaysCount(');
    expect(arrangementSource).toContain('const start = scheduleDayNo(arrangementForm.scheduleStartDay) || 1;');
  });

  it('prefills a new scenic ticket quantity from active booking order guest counts', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');

    expect(arrangementSource).toContain("if (type === 'scenic') {\n    prefillScenicTicketQuantity();");
    expect(arrangementSource).toContain(".filter((order) => !isOrderStatus(order, ['已取消', 'cancelled']))");
    expect(arrangementSource).toContain('.reduce((sum, order) => sum + Math.max(0, Number(order.guestCount || 0)), 0);');
    expect(arrangementSource).toContain('firstLine.quantity = receivedGuestCount;');
    expect(arrangementSource).toContain('syncPrimaryPriceFields();');
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
    expect(arrangementSource).toContain('sourceOrderInfos');
    expect(arrangementSource).toContain('来源订单');
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

  it('keeps guide arrangement editing compact and avoids showing zero as the default guide', () => {
    const guideApiSource = readAppFile('src/api/dispatch/guide-schedule.ts');
    const financeApiSource = readAppFile('src/api/finance/guide-imprest.ts');
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');

    expect(guideApiSource).toContain('getGuideAvailability');
    expect(guideApiSource).toContain('/dispatch/guide-schedule/availability');
    expect(guideApiSource).toContain('approvedImprestAmount?: number;');
    expect(guideApiSource).toContain('pendingImprestAmount?: number;');
    expect(guideApiSource).toContain('paidImprestAmount?: number;');
    expect(guideApiSource).toContain('imprestBalanceAmount?: number;');
    expect(guideApiSource).toContain('imprestApprovalStatus?:');
    expect(financeApiSource).toContain('companyMarkupRate?: number;');
    expect(financeApiSource).toContain('requestedAmount?: number;');
    expect(financeApiSource).toContain('params: { companyMarkupRate?: number; guideId: number; teamId: number }');
    expect(arrangementSource).toContain(":title=\"guideModalEditing ? '更换导游' : '选择导游'\"");
    expect(arrangementSource).toContain('ok-text="提交保存"');
    expect(arrangementSource).toContain('class="guide-summary-chips"');
    expect(arrangementSource).toContain('class="guide-arrangement-table-wrap"');
    expect(arrangementSource).toContain('class="guide-arrangement-table"');
    expect(arrangementSource).toContain('class="guide-name-line"');
    expect(arrangementSource).toContain('class="guide-phone-line"');
    expect(arrangementSource).toContain('class="guide-time-range"');
    expect(arrangementSource).toContain('class="guide-money-stack"');
    expect(arrangementSource).toContain('class="guide-remark-summary"');
    expect(arrangementSource).toContain('class="guide-row-actions"');
    expect(arrangementSource).toContain('@click="openGuidePickerForRow(record)"');
    expect(arrangementSource).toContain('@click="openGuideEditModal(record)"');
    expect(arrangementSource).toContain('guideEditOpen');
    expect(arrangementSource).toContain('submitGuideEditDraft');
    expect(arrangementSource).toContain('title="修改导游安排"');
    expect(arrangementSource).not.toContain('guide-note-textarea');
    expect(arrangementSource).not.toContain('class="guide-add-card"');
    expect(arrangementSource).toContain('导游信息');
    expect(arrangementSource).toContain('更换导游');
    expect(arrangementSource).toContain('待定中');
    expect(arrangementSource).toContain('const guideModalEditingRecord = ref<TeamGuideRow>();');
    expect(arrangementSource).toContain('async function openGuidePickerForRow(record: TeamGuideRow)');
    expect(arrangementSource).not.toContain('guide-table-subtext');
    expect(arrangementSource).toContain('class="guide-modal-form"');
    expect(arrangementSource).toContain('class="guide-modal-section"');
    expect(arrangementSource).toContain('class="guide-picker-tabs"');
    expect(arrangementSource).toContain('class="guide-picker-search"');
    expect(arrangementSource).toContain('class="guide-picker-table"');
    expect(arrangementSource).toContain('导游姓名');
    expect(arrangementSource).toContain('不能出团原因');
    expect(arrangementSource).toContain("guidePickerActiveTab = ref<GuidePickerTabKey>('available')");
    expect(arrangementSource).toContain("{ key: 'all', label: '全部导游' }");
    expect(arrangementSource).not.toContain('就近安排');
    expect(arrangementSource).toContain('@click="selectGuideForDraft(item)"');
    expect(arrangementSource).toContain(':disabled="!item.available"');
    expect(arrangementSource).toContain('guideId: undefined');
    expect(arrangementSource).not.toContain('guideId: 0');
    expect(arrangementSource).not.toContain('placeholder="请选择导游"');
    expect(arrangementSource).toContain('addon-before="¥"');
    expect(arrangementSource).toContain('previewGuideImprest');
    expect(arrangementSource).toContain('submitGuideImprest');
    expect(arrangementSource).toContain('class="guide-imprest-inline-actions"');
    expect(arrangementSource).toContain('@click="openGuideImprestCalculator(record)"');
    expect(arrangementSource).toContain('v-model:open="guideImprestModalOpen"');
    expect(arrangementSource).toContain('title="计算导游备用金"');
    expect(arrangementSource).toContain('计划备用金');
    expect(arrangementSource).toContain('累计已批备用金');
    expect(arrangementSource).toContain('待审批备用金');
    expect(arrangementSource).toContain('本次公司加点率');
    expect(arrangementSource).toContain('本次申请金额');
    expect(arrangementSource).not.toContain('当前可申请');
    expect(arrangementSource).toContain('已申请/已批');
    expect(arrangementSource).toContain('建议余额参考');
    expect(arrangementSource).toContain('仅作风险提示，不限制本次申请');
    expect(arrangementSource).toContain('超过系统建议金额');
    expect(arrangementSource).toContain('guideImprestCompanyMarkupRate');
    expect(arrangementSource).toContain('guideImprestRequestedAmount');
    expect(arrangementSource).toContain('refreshGuideImprestPreview');
    expect(arrangementSource).toContain('guideImprestApprovalStatusLabel');
    expect(arrangementSource).toContain('guideImprestApprovalStatusTone(record.imprestApprovalStatus)');
    expect(arrangementSource).toContain('companyMarkupRate: guideImprestCompanyMarkupRate.value');
    expect(arrangementSource).toContain('requestedAmount: guideImprestRequestedAmount.value');
    expect(arrangementSource).toContain('现付总成本');
    expect(arrangementSource).toContain('自费抵扣');
    expect(arrangementSource).toContain("line.lineType === 'optional_deduction' ? formatPercent(line.companyMarkupRate) : '--'");
    expect(arrangementSource).toContain('function guideImprestLineFormula');
    expect(arrangementSource).toContain('function guideImprestSummaryFormula');
    expect(arrangementSource).toContain('class="guide-imprest-formula-panel"');
    expect(arrangementSource).toContain('class="guide-imprest-formula-row"');
    expect(arrangementSource).toContain('建议备用金 = 现付总成本');
    expect(arrangementSource).toContain('× ${formatPercent(line.companyMarkupRate)} × ${guestCount}人');
    expect(arrangementSource).toContain('导游应上交');
    expect(arrangementSource).toContain('使用此金额');
    expect(arrangementSource).toContain('提交备用金申请');
    expect(arrangementSource).toContain('async function openGuideImprestCalculator(record: TeamGuideRow)');
    expect(arrangementSource).toContain('async function useGuideImprestSuggestedAmount()');
    expect(arrangementSource).toContain('async function submitGuideImprestApplication()');
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

  it('loads formal team sensitive money summary from backend instead of calculating it in the page', () => {
    const apiSource = readAppFile('src/api/sales/team.ts');
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');

    expect(apiSource).toContain('TeamArrangementSummary');
    expect(apiSource).toContain('getTeamArrangementSummary');
    expect(apiSource).toContain('/sales/team/${teamId}/arrangements/summary');
    expect(arrangementSource).toContain('getTeamArrangementSummary');
    expect(arrangementSource).toContain('teamArrangementSummary');
    expect(arrangementSource).toContain('await loadArrangementSummary()');
    expect(arrangementSource).not.toContain('const regularArrangementCostTotal = computed(() =>');
    expect(arrangementSource).not.toContain('const optionalCompanyProfitTotal = computed(() =>');
    expect(arrangementSource).not.toContain('const shoppingCompanyProfitTotal = computed(() =>');
    expect(arrangementSource).not.toContain('const budgetProfit = computed(() =>');
    expect(arrangementSource).not.toContain("{ key: 'profit', label: '预算利润', value: formatPlainMoney(budgetProfit.value) }");
    expect(arrangementSource).not.toContain("{ key: 'profit', label: '预算利润', value: '0 元' }");
  });

  it('opens an old-system style gross profit preview from budget profit and print actions', () => {
    const routeSource = readAppFile('src/router/routes/modules/sales.ts');
    const apiSource = readAppFile('src/api/sales/team.ts');
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const headerSource = readAppFile('src/views/sales/components/FormalTeamPageHeader.vue');
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
    expect(arrangementSource).toContain('formalHeaderMetrics');
    expect(arrangementSource).toContain("item.key === 'profit'");
    expect(arrangementSource).toContain('@metric-click="handleMetricClick"');
    expect(headerSource).toContain('@click="metric.clickable && emit(\'metricClick\', metric)"');
    expect(headerSource).toContain(':class="[`metric-${metric.key}`, { clickable: metric.clickable }]"');
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

  it('keeps the optional cost modal aligned with the old-system field order', () => {
    const editorSource = readAppFile('src/views/sales/components/ArrangementEditorModal.vue');
    const optionalBranch = editorSource.slice(
      editorSource.indexOf(`<template v-else-if="activeEditorType === 'optional'">`),
      editorSource.indexOf(`<template v-else-if="activeEditorType === 'shopping'">`),
    );

    expect(editorSource).toContain('const showCostModeTabs = computed(() =>');
    expect(editorSource).toContain(`props.activeEditorType !== 'optional'`);
    expect(editorSource).toContain('v-if="showCostModeTabs"');
    expect(optionalBranch).not.toContain('traffic-cost-mode-tabs');
    expectTextOrder(optionalBranch, [
      '景区名称',
      '游玩日期',
      '添加景区',
      '供应商',
      '价格信息',
      '销售价',
      '成本价',
      '导游提成',
      '合计人数',
      '合计收入',
      '合计成本',
      '合计现结',
      '合计挂账',
      '合计提成',
      '费用合计',
      '人数：',
      '收入：',
      '成本：',
      '现结：',
      '挂账：',
      '提成：',
      '订单信息：',
      '备注信息：',
    ]);
    expect(optionalBranch).toContain('class="optional-summary-order-field"');
    expect(optionalBranch).toContain('class="optional-summary-remark-field"');
    expect(optionalBranch).toContain('class="optional-overview-item"');
    expect(optionalBranch).toContain('class="optional-fee-number-grid"');
    expect(optionalBranch).toContain('class="optional-fee-meta-grid"');
    expect(editorSource).not.toContain('overflow-x: auto;');
    expect(editorSource).not.toContain('min-width: 1280px;');
  });

  it('calculates optional fee summary amounts by people count and unit prices', () => {
    const teamArrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const productArrangementSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    [teamArrangementSource, productArrangementSource].forEach((source) => {
      const syncStart = source.indexOf('function syncOptionalLineToForm()');
      const syncEnd = source.indexOf('function syncShoppingLineToForm()', syncStart);
      const syncSnippet = source.slice(
        syncStart,
        syncEnd,
      );

      expect(syncSnippet).toContain('saleAmount += quantity * salePrice;');
      expect(syncSnippet).toContain('costAmount += quantity * costPrice;');
      expect(syncSnippet).toContain('guideCommissionAmount += quantity * guideCommissionUnitAmount;');
    });

    const editorSource = readAppFile('src/views/sales/components/ArrangementEditorModal.vue');
    expect(editorSource).toContain('const optionalPrimaryLine = computed<SalesProductApi.ArrangementPriceLine>');
    expect(editorSource).toContain('v-model:value="optionalPrimaryLine.salePrice"');
    expect(editorSource).toContain('v-model:value="optionalPrimaryLine.costPrice"');
    expect(editorSource).toContain('v-model:value="optionalPrimaryLine.guideCommissionAmount"');
    expect(editorSource).toContain(':value="optionalLineSaleAmount(line)"');
    expect(editorSource).toContain(':value="optionalLineCostAmount(line)"');
    expect(editorSource).toContain(':value="optionalLineGuideCommissionAmount(line)"');
  });

  it('persists formal team arrangement section done and none states', () => {
    const arrangementSource = readAppFile('src/views/sales/team/arrangement.vue');
    const editorModelSource = readAppFile('src/views/sales/components/arrangement-editor-model.ts');
    const apiSource = readAppFile('src/api/sales/team.ts');

    expect(apiSource).toContain('getTeamArrangementSectionStatuses');
    expect(apiSource).toContain('saveTeamArrangementSectionStatus');
    expect(arrangementSource).toContain('getTeamArrangementSectionStatuses');
    expect(arrangementSource).toContain('saveTeamArrangementSectionStatus');
    expect(arrangementSource).toContain('loadSectionStatuses');
    expect(arrangementSource).toContain('await saveTeamArrangementSectionStatus');
    expect(arrangementSource).not.toContain('分类状态后续接入团队流程状态表');
    expect(editorModelSource).toContain('confirmed: false');
    expect(editorModelSource).not.toContain("confirmed: type === 'hotel'");
  });
});

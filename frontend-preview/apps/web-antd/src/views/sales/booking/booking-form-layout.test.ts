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
    expect(currentIndex, `${label} should appear after previous label`).toBeGreaterThan(previousIndex);
    previousIndex = currentIndex;
  }
}

describe('sales booking order form layout', () => {
  it('keeps BookingAgent old-system sections and adds AI assisted entry', () => {
    const source = readAppFile('src/views/sales/booking/form.vue');
    const templateSource = source.slice(source.indexOf('<template>'), source.indexOf('<style scoped>'));
    const aiPlaceholderSource = source.slice(source.indexOf('const aiImportTextPlaceholder'), source.indexOf('const selectedModuleKeys'));
    const routeSource = readAppFile('src/router/routes/modules/sales.ts');
    const apiSource = readAppFile('src/api/sales/booking.ts');

    expect(source).toContain('AI辅助录入');
    expect(source).toContain('MAX_AI_IMPORT_FILE_SIZE_MB');
    expect(source).toContain('上传文件不能超过50MB');
    expect(source).toContain('SUPPORTED_AI_IMPORT_FILE_EXTENSIONS');
    expect(source).toContain('doc,docx,xls,xlsx,csv,txt,pdf,jpg,jpeg,png,webp,bmp');
    expect(source).toContain('暂不支持该文件类型，请上传 Word、Excel、PDF、图片或粘贴文本');
    expect(source).toContain('身份证校验');
    expect(source).toContain('分房');
    expect(source).toContain('领队');
    expect(source).toContain('接机标识');
    expect(source).toContain('业务员');
    expect(source).toContain('收客计调');
    expect(source).toContain('createSalesBookingFeeChange');
    expect(source).toContain('cancelSalesBookingFeeChange');
    expect(source).toContain('exportSalesBookingGuests');
    expect(source).toContain('downloadSalesBookingGuestImportTemplate');
    expect(source).toContain('importSalesBookingGuestsPreview');
    expect(source).toContain('feeChangeOpen');
    expect(source).toContain('employeeIdByName(form.salespersonEmployeeName)');
    expect(source).toContain('employeeIdByName(form.bookingOperatorEmployeeName)');
    expect(source).not.toContain('从分项报价获取价格');
    expect(source).not.toContain('转到分项报价');
    expect(source).toContain('占位');
    expect(source).toContain('收客须知');
    expect(source).toContain('saveButtonText');
    expect(source).toContain('确认订单');
    expect(source).toContain('取消订单');
    expect(source).toContain('confirmOrder');
    expect(source).toContain('cancelOrder');
    expect(source).toContain('booking-footer-status');
    expect(source).toContain('订单操作');
    expect(source).toContain('lucide:save');
    expect(source).toContain('lucide:check');
    expect(source).toContain('lucide:x');
    expect(source).toContain('DatePicker');
    expect(source).not.toContain('TimePicker');
    expect(source).not.toContain('Cascader');
    expect(source).not.toContain('buildRegionOptions');
    expect(source).not.toContain('splitRegionPath');
    expect(source).not.toContain('sourceRegionPath');
    expect(source).toContain('getSalesTeamOperationDetail');
    expect(source).toContain('saveSalesTeam');
    expect(source).toContain('operationDetail');
    expect(source).toContain('profileBadges');
    expect(source).toContain('内部备注');
    expect(source).toContain('openProfileEditor');
    expect(source).toContain('saveProfileEditor');
    expect(source).toContain('profileEditorOpen');
    expect(source).toContain('formatDistanceMeters(operationDetail.value?.routeSummary?.totalDistanceMeters)');
    expect(source).toContain('showPriceModal');
    expect(source).toContain('showItineraryModal');
    expect(source).toContain('getCustomerUnitPage');
    expect(source).toContain('customerOptions');
    expect(source).toContain('handleCustomerSelect');
    expect(source).toContain('customerOptionsLoaded');
    expect(source).toContain('loadCustomerOptions(customerKeyword.value, { force: true })');
    expect(source).toContain('checkCustomerRisk');
    expect(source).toContain('applyCustomerRiskApproval');
    expect(source).toContain('customerRiskAlert');
    expect(source).toContain('riskApprovalRequestId');
    expect(source).toContain('riskApprovalStatus');
    expect(source).toContain('customerRiskApprovalCustomerId');
    expect(source).toContain('currentCustomerRiskApprovalId');
    expect(source).toContain('riskApprovalRequestId: currentRiskApprovalId');
    expect(source).toContain('已有总经理审批通过，可提交订单');
    expect(source).toContain('已审批通过');
    expect(source).toContain('申请总经理审批');
    expect(source).toContain('客户风控审批');
    expect(source).toContain('guestGenderOptions');
    expect(source).toContain('guestTypeOptions');
    expect(source).toContain('deriveGuestTypeByTicketAge');
    expect(source).toContain('deriveGuestTypeFromAi');
    expect(source).toContain('validateChineseIdCard');
    expect(source).toContain('handleGuestCertificateChange');
    expect(source).toContain('@blur="handleGuestCertificateChange(guest)"');
    expect(source).toContain('handleGuestAgeChange');
    expect(source).toContain('@change="handleGuestAgeChange(guest)"');
    expect(source).toContain('idCardStatusText');
    expect(source).toContain('未校验');
    expect(source).toContain('feeChangeRows');
    expect(source).toContain('addGuest');
    expect(source).toContain('removeGuest');
    expect(source).toContain('recognizeBookingAiImport');
    expect(source).toContain('fillAiDraftToForm');
    expect(source).toContain("appendImportedGuests(parsedGuests, 'guest-ai')");
    expect(source).toContain('appendUniqueGuests(mappedGuests, [])');
    expect(source).toContain("fillBlank(travelInfo, 'outboundArrivalStation', result.travelInfo?.outboundStationName)");
    expect(source).toContain("fillBlank(travelInfo, 'returnDepartureStation', result.travelInfo?.returnStationName)");
    expect(source).toContain('syncPriceLinesFromTeamPrice');
    expect(source).toContain('matchedTeamPrice');
    expect(source).toContain('selectedCustomer');
    expect(source).toContain('guestTypeCounts');
    expect(source).toContain('matchPriceByCustomerCategory');
    expect(source).toContain('buildExtraFeeLine');
    expect(source).toContain("lineType: 'surcharge'");
    expect(source).toContain('按占位游客数量自动计入订单应收');
    expect(source).not.toContain('附加费用不自动计入');
    expect(source).not.toContain('buildAiFeeRemark');
    expect(source).not.toContain('buildAiCostPriceRows');
    expect(source).not.toContain("costLineTypeMap");
    expect(source).not.toContain('priceInfo?.priceLines');
    expect(source).not.toContain("fillBlank(form, 'feeRemark', feeRemarkDraft)");
    expect(templateSource).not.toContain('ai-price-lines');
    expect(source).toContain("const aiImportText = ref('')");
    expect(source).toContain('aiImportTextPlaceholder');
    expect(source).toContain('序号 姓名 年龄 出生日期 身份证号 电话 分房 备注');
    expect(source).not.toContain('报价：成人 3000元/人，儿童 1999元/人，单房差 580元');
    expect(source).toContain('示例游客A');
    expect(source).toContain('110000********1234');
    expect(source).toContain('138****0001');
    expect(source).toContain('全程家庭房');
    expect(aiPlaceholderSource).not.toMatch(/\d{17}[0-9Xx]/);
    expect(aiPlaceholderSource).not.toMatch(/1[3-9]\d{9}/);
    expect(source).not.toContain('客户：示例旅行社');
    expect(source).not.toContain('aiImportText.value = sampleText()');
    expect(source).not.toContain('function sampleText()');
    expect(source).toContain('ai-import-workbench');
    expect(source).toContain('ai-upload-zone');
    expect(source).toContain('moduleSelection');
    expect(source).toContain('selectedModuleKeys');
    expect(source).not.toContain("key: 'customer'");
    expect(source).not.toContain("selected.has('customer')");
    expect(source).not.toContain('fillAiCustomer');
    expect(source).not.toContain('customerScore');
    expect(source).toContain('moduleScores');
    expect(source).not.toContain("key: 'price'");
    expect(source).toContain('guestSummary');
    expect(source).toContain('填入所选模块');
    expect(source).toContain('只填空字段');
    expect(source).toContain('疑似漏识别');
    expect(source).toContain('识别游客数');
    expect(source).toContain('saveSalesBookingOrder');
    expect(source).toContain('old-system-booking-form');
    expect(source).toContain('booking-agent-panel');
    expect(source).toContain('booking-row-section');
    expect(source).toContain('section-side-label');
    expect(source).toContain('team-operation-shell');
    expect(source).toContain('team-operation-header');
    expect(source).toContain('operation-flow-row');
    expect(source).toContain('stage-flow-item');
    expect(source).toContain('team-profile-block');
    expect(source).toContain('team-metric-panel');
    expect(source).toContain('team-metric-strip');
    expect(source).toContain('old-system-view-actions');
    expect(source).toContain('view-action-tile');
    expect(source).toContain('查看价格');
    expect(source).toContain('查看行程');
    expect(source).toContain('price-modal-table');
    expect(source).toContain('itinerary-modal-content');
    expect(source).toContain('当前团队暂无客户类型价格');
    expect(source).toContain('当前产品暂无行程说明');
    expect(templateSource).toContain('@click="showPriceModal"');
    expect(templateSource).toContain('@click="showItineraryModal"');
    expect(templateSource).toContain('class="profile-edit-tag"');
    expect(templateSource).toContain('v-model:open="profileEditorOpen"');
    expect(templateSource).toContain('保存');
    expect(source).toContain('--operation-primary: #1677ff');
    expect(source).toContain('background: linear-gradient(180deg, #f8fbff 0%, #fff 100%)');
    expect(source).toContain('box-shadow: 0 8px 22px rgb(15 23 42 / 5%)');
    expect(source).toContain('background: #fff !important');
    expect(source).toContain('color: var(--operation-heading)');

    expect(templateSource).not.toContain('toolButtons');
    expect(templateSource).not.toContain('订单文件');
    expect(templateSource).not.toContain('打印行程单');
    expect(templateSource).not.toContain('打印团队名单');
    expect(templateSource).not.toContain('打印团队结算单');
    expect(templateSource).not.toContain('导出接送机名单');
    expect(templateSource).not.toContain('拼团操作');
    expect(templateSource).not.toContain('打印预订单');
    expect(templateSource).not.toContain('打印确认单');
    expect(templateSource).not.toContain('打印游客名单');
    expect(templateSource).not.toContain('打印结算单');
    expect(templateSource).not.toContain('出团通知书');
    expect(templateSource).not.toContain('导游评价');
    expect(templateSource).not.toContain('打印团队报价单');
    expect(templateSource).toContain('<Modal');
    expect(templateSource).not.toContain('<Drawer v-model:open="aiImportOpen"');
    expect(templateSource).not.toContain('aiImportResult.customerInfo.customerName');
    expect(templateSource).not.toContain('aiImportResult.customerInfo.contactName');
    expectTextOrder(templateSource, ['订单处理', 'AI辅助录入']);
    expectTextOrder(templateSource, ['收客', '排团', '发团', '结算', '完成']);
    expectTextOrder(templateSource, ['订单处理', '收客', "product?.productName || '收客订单'"]);

    expectTextOrder(source, [
      '预控人数',
      '实收人数',
      '剩余人数',
      '旅游天数',
      '团号',
      '出团日期',
      '总里程数',
    ]);

    expectTextOrder(templateSource, [
      '行程说明',
      '导游相关',
      '客户信息',
      '价格信息',
      '附加说明',
      '费用变更记录',
      '游客名单',
      '返回团队',
      '确认订单',
      '取消订单',
    ]);
    expect(templateSource).not.toContain('class="section-side-label">订单信息');
    expect(templateSource).not.toContain('订单编号');
    expect(templateSource).not.toContain('Form.Item label="订单状态"');
    expect(templateSource).not.toContain('已收金额');
    expect(templateSource).not.toContain('class="section-side-label">酒店信息');
    expect(templateSource).toContain('class="section-side-label">费用变更记录');
    expect(templateSource).toContain('class="section-side-label">游客名单');
    expect(templateSource).toContain('费用合计');
    expect(templateSource).toContain('v-model:value="form.salespersonEmployeeName"');
    expect(templateSource).toContain('v-model:value="form.bookingOperatorEmployeeName"');
    expect(templateSource).not.toContain('<Input :value="team?.operatorEmployeeName || \'--\'" disabled />');
    expect(templateSource).toContain('@click="openFeeChangeModal"');
    expect(templateSource).toContain('@click="exportGuests"');
    expect(templateSource).toContain('@click="downloadGuestImportTemplate"');
    expect(templateSource).toContain('beforeUploadGuestListFile');
    expect(templateSource).not.toContain('showPendingFeature(\'添加变更费用\')');
    expect(templateSource).not.toContain('showPendingFeature(\'导出名单\')');
    expect(templateSource).not.toContain('showPendingFeature(\'导入名单\')');
    expect(templateSource).toContain('v-model:open="feeChangeOpen"');
    expect(templateSource).toContain('变更方向');
    expect(templateSource).toContain('费用项目');
    expect(templateSource).toContain('v-model:value="feeChangeForm.amount"');
    expect(templateSource).toContain('@click="cancelFeeChange(record)"');
    expect(templateSource).toContain('查看电子合同');
    expect(templateSource).toContain('创建电子合同');
    expect(templateSource).toContain('导入名单');
    expect(templateSource).toContain('下载模板');
    expect(templateSource).toContain('导出名单');
    expect(templateSource).toContain('清除名单');
    expect(templateSource).toContain('出生年月');
    expect(templateSource).toContain('房间组号');
    expect(templateSource).toContain('分房备注');
    expect(templateSource).toContain('v-model:value="guest.roomRemark"');
    expect(source).toContain('class="guest-table-scroll"');
    expect(templateSource).toContain('<colgroup>');
    expect(source).toContain('width: 100%');
    expect(source).toContain('min-width: 1180px');
    expect(source).toContain('table-layout: fixed');
    expect(source).toContain('.guest-id-column');
    expect(source).toContain('width: 11%');
    expect(source).toContain('.guest-id-status-column');
    expect(source).toContain('width: 6.4%');
    expect(templateSource).toContain('show-time');
    expect(templateSource).toContain('format="YYYY/MM/DD HH:mm"');
    expect(templateSource).toContain('value-format="YYYY/MM/DD HH:mm"');
    expect(templateSource).toContain('placeholder=\"出发时间\"');
    expect(templateSource).toContain('placeholder=\"抵达时间\"');
    expect(templateSource).not.toContain('v-model:value=\"form.travelDescription\"');
    expect(templateSource).not.toContain('placeholder=\"可选择省 / 市 / 区县\"');
    expect(templateSource).toContain('class=\"guest-list-actions\"');

    expect(routeSource).toContain('/sales/team/booking/:teamId');
    expect(routeSource).toContain('/sales/team/booking/:teamId/:orderId');
    expect(apiSource).toContain('/sales/booking/save');
    expect(apiSource).toContain('/sales/booking/team/');
    expect(apiSource).toContain('/orders');
  });
});

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
    expect(source).toContain('身份证校验');
    expect(source).toContain('分房');
    expect(source).toContain('领队');
    expect(source).toContain('接机标识');
    expect(source).toContain('业务员');
    expect(source).toContain('收客计调');
    expect(source).toContain('从分项报价获取价格');
    expect(source).toContain('转到分项报价');
    expect(source).toContain('占位');
    expect(source).toContain('收客须知');
    expect(source).toContain('提交订单');
    expect(source).toContain('DatePicker');
    expect(source).not.toContain('TimePicker');
    expect(source).toContain('Cascader');
    expect(source).toContain('buildRegionOptions');
    expect(source).toContain('splitRegionPath');
    expect(source).toContain('sourceRegionPath');
    expect(source).toContain('getCustomerUnitPage');
    expect(source).toContain('customerOptions');
    expect(source).toContain('handleCustomerSelect');
    expect(source).toContain('guestGenderOptions');
    expect(source).toContain('guestTypeOptions');
    expect(source).toContain('feeChangeRows');
    expect(source).toContain('addGuest');
    expect(source).toContain('removeGuest');
    expect(source).toContain('recognizeBookingAiImport');
    expect(source).toContain('fillAiDraftToForm');
    expect(source).toContain("fillBlank(travelInfo, 'outboundArrivalStation', result.travelInfo?.outboundStationName)");
    expect(source).toContain("fillBlank(travelInfo, 'returnDepartureStation', result.travelInfo?.returnStationName)");
    expect(source).toContain('buildAiFeeRemark');
    expect(source).toContain('buildAiCostPriceRows');
    expect(source).toContain("costLineTypeMap");
    expect(source).toContain("房费");
    expect(source).toContain("车费");
    expect(source).toContain("门票");
    expect(source).toContain("餐费");
    expect(source).toContain("导服");
    expect(source).toContain('priceInfo?.priceLines');
    expect(source).toContain("fillBlank(form, 'feeRemark', feeRemarkDraft)");
    expect(templateSource).toContain('ai-price-lines');
    expect(source).toContain("const aiImportText = ref('')");
    expect(source).toContain('aiImportTextPlaceholder');
    expect(source).toContain('序号 姓名 年龄 出生日期 身份证号 电话 分房 备注');
    expect(source).toContain('示例游客A');
    expect(source).toContain('110000********1234');
    expect(source).toContain('138****0001');
    expect(source).toContain('全程家庭房');
    expect(aiPlaceholderSource).not.toMatch(/\d{17}[0-9Xx]/);
    expect(aiPlaceholderSource).not.toMatch(/1[3-9]\d{9}/);
    expect(source).not.toContain('aiImportText.value = sampleText()');
    expect(source).not.toContain('function sampleText()');
    expect(source).toContain('ai-import-workbench');
    expect(source).toContain('ai-upload-zone');
    expect(source).toContain('moduleSelection');
    expect(source).toContain('selectedModuleKeys');
    expect(source).toContain('moduleScores');
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
      '提交订单',
      '返回团队',
    ]);
    expect(templateSource).not.toContain('class="section-side-label">订单信息');
    expect(templateSource).not.toContain('订单编号');
    expect(templateSource).not.toContain('Form.Item label="订单状态"');
    expect(templateSource).not.toContain('已收金额');
    expect(templateSource).not.toContain('class="section-side-label">酒店信息');
    expect(templateSource).toContain('class="section-side-label">费用变更记录');
    expect(templateSource).toContain('class="section-side-label">游客名单');
    expect(templateSource).toContain('费用合计');
    expect(templateSource).toContain('查看电子合同');
    expect(templateSource).toContain('创建电子合同');
    expect(templateSource).toContain('导入名单');
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
    expect(source).toContain('width: 14.4%');
    expect(templateSource).toContain('show-time');
    expect(templateSource).toContain('format="YYYY/MM/DD HH:mm"');
    expect(templateSource).toContain('value-format="YYYY/MM/DD HH:mm"');
    expect(templateSource).toContain('placeholder=\"出发时间\"');
    expect(templateSource).toContain('placeholder=\"抵达时间\"');
    expect(templateSource).not.toContain('v-model:value=\"form.travelDescription\"');
    expect(templateSource).toContain('placeholder=\"可选择省 / 市 / 区县\"');
    expect(templateSource).toContain('class=\"guest-list-actions\"');

    expect(routeSource).toContain('/sales/team/booking/:teamId');
    expect(routeSource).toContain('/sales/team/booking/:teamId/:orderId');
    expect(apiSource).toContain('/sales/booking/save');
    expect(apiSource).toContain('/sales/booking/team/');
    expect(apiSource).toContain('/orders');
  });
});

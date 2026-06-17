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
    expect(arrangementPageSource).toContain('这里只维护产品生成团队时的默认安排参数');
    expect(arrangementPageSource).toContain('不处理正式订单、单据、导游报账、计调审核和真实团队成本');
    expect(formSource).not.toContain('团队安排总览模板');
    expect(formSource).not.toContain('团队安排总览 Group Arrange');
    expect(formSource).not.toContain('收客 -> 排团 -> 发团 -> 结算 -> 完成');
    expect(formSource).not.toContain('订单信息');
    expect(formSource).not.toContain('订房单');
    expect(formSource).not.toContain('订车单');
    expect(formSource).not.toContain('预订单');
    expect(formSource).not.toContain('确认单');
  });

  it('uses old-system style overview and icon shortcuts instead of inline type select', () => {
    const arrangementPageSource = readAppFile('src/views/sales/product/team-arrangement.vue');

    expect(arrangementPageSource).toContain('arrangement-overview-table');
    expect(arrangementPageSource).toContain('arrangement-icon-grid');
    expect(arrangementPageSource).toContain('lucide:plane');
    expect(arrangementPageSource).toContain('lucide:building-2');
    expect(arrangementPageSource).toContain('lucide:car');
    expect(arrangementPageSource).not.toContain('<Table.Column title="类型"');
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

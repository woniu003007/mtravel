<script lang="ts" setup>
import type { SalesTeamApi } from '#/api/sales/team';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Card, Spin } from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  exportSalesTeamGrossProfit,
  getSalesTeamGrossProfitPreview,
} from '#/api/sales/team';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const exporting = ref<'docx' | 'pdf'>();
const preview = ref<SalesTeamApi.GrossProfitPreview>();
const teamId = computed(() => Number(route.params.id || 0));
const team = computed(() => preview.value?.team);
const summary = computed(() => preview.value?.summary || {});
const pageTitle = computed(() => (team.value?.teamNo ? `团队毛利表 - ${team.value.teamNo}` : '团队毛利表'));

function money(value?: number | string) {
  const numberValue = Number(value || 0);
  return Number.isFinite(numberValue) ? numberValue.toFixed(2) : '0.00';
}

function plain(value?: number | string) {
  const numberValue = Number(value || 0);
  return Number.isFinite(numberValue) ? numberValue.toString() : '0';
}

function dateText(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '--';
}

function triggerBlobDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.style.display = 'none';
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 100);
}

function exportFilename(format: 'docx' | 'pdf') {
  const teamNo = (team.value?.teamNo || '团队').replace(/[\\/:*?"<>|\s]+/g, '_');
  return `团队毛利表${teamNo}.${format}`;
}

async function exportGrossProfit(format: 'docx' | 'pdf') {
  if (!teamId.value) return;
  exporting.value = format;
  try {
    const blob = await exportSalesTeamGrossProfit(teamId.value, format);
    triggerBlobDownload(blob, exportFilename(format));
  } finally {
    exporting.value = undefined;
  }
}

function printOnline() {
  window.print();
}

async function loadPreview() {
  if (!teamId.value) {
    router.push('/sales/team');
    return;
  }
  loading.value = true;
  try {
    preview.value = await getSalesTeamGrossProfitPreview(teamId.value);
  } finally {
    loading.value = false;
  }
}

function backToArrangement() {
  if (!teamId.value) return;
  router.push(`/sales/team/arrangement/${teamId.value}`);
}

function emptyText(rows?: unknown[]) {
  return rows?.length ? '' : '暂无明细';
}

onMounted(loadPreview);
</script>

<template>
  <Page :title="pageTitle">
    <Card class="gross-profit-shell">
      <div class="gross-profit-toolbar">
        <div class="toolbar-left">
          <Button size="small" @click="backToArrangement">
            <IconifyIcon icon="lucide:arrow-left" />
            <span>返回团队安排</span>
          </Button>
        </div>
        <div class="toolbar-actions">
          <Button :loading="exporting === 'docx'" size="small" @click="exportGrossProfit('docx')">
            <IconifyIcon icon="lucide:file-text" />
            <span>Word文件</span>
          </Button>
          <Button :loading="exporting === 'pdf'" size="small" @click="exportGrossProfit('pdf')">
            <IconifyIcon icon="lucide:file-type-2" />
            <span>Pdf文件</span>
          </Button>
          <Button type="primary" size="small" @click="printOnline">
            <IconifyIcon icon="lucide:printer" />
            <span>在线打印</span>
          </Button>
        </div>
      </div>

      <Spin :spinning="loading">
        <div v-if="preview" class="gross-profit-print-page">
          <h1>团队毛利表(预算)</h1>
          <h2>{{ team?.productName || '--' }}</h2>

          <table class="gross-profit-table meta-table">
            <tbody>
              <tr>
                <th>团号</th>
                <td>{{ team?.teamNo || '--' }}</td>
                <th>出团日期</th>
                <td>{{ dateText(team?.departureDate) }}</td>
              </tr>
              <tr>
                <th>旅游天数</th>
                <td>{{ team?.travelDays || 0 }} 天</td>
                <th>接待人数</th>
                <td>{{ team?.guestCount || 0 }}人</td>
              </tr>
              <tr>
                <th>导游</th>
                <td>{{ team?.guideSummary || '--' }}</td>
                <th>操作计调</th>
                <td>{{ team?.operatorName || '--' }}</td>
              </tr>
            </tbody>
          </table>

          <section class="gross-profit-section">
            <h3>收入</h3>
            <table class="gross-profit-table">
              <thead>
                <tr>
                  <th>客户单位</th>
                  <th>业务员</th>
                  <th>人数</th>
                  <th>应收明细</th>
                  <th>应收金额</th>
                  <th>已收金额</th>
                  <th>收客计调</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in preview.incomeRows" :key="`income-${index}`">
                  <td>{{ row.customerName || '--' }}</td>
                  <td>{{ row.salespersonName || '--' }}</td>
                  <td>{{ row.guestCount || 0 }}人</td>
                  <td class="text-left multiline">{{ row.receivableDetail || '--' }}</td>
                  <td class="amount">{{ money(row.receivableAmount) }}</td>
                  <td class="amount">{{ money(row.receivedAmount) }}</td>
                  <td>{{ row.bookingOperatorName || '--' }}</td>
                </tr>
                <tr v-if="emptyText(preview.incomeRows)">
                  <td colspan="7">{{ emptyText(preview.incomeRows) }}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section class="gross-profit-section">
            <h3>支出</h3>
            <table class="gross-profit-table">
              <thead>
                <tr>
                  <th>类别</th>
                  <th>供应商</th>
                  <th>费用说明</th>
                  <th>应付金额</th>
                  <th>现付</th>
                  <th>挂账已付</th>
                  <th>审核人</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in preview.costRows" :key="`cost-${index}`">
                  <td>{{ row.category || '--' }}</td>
                  <td>{{ row.supplierName || '--' }}</td>
                  <td class="text-left">{{ row.costDescription || '--' }}</td>
                  <td class="amount">{{ money(row.payableAmount) }}</td>
                  <td class="amount">{{ money(row.cashAmount) }}</td>
                  <td class="amount">{{ money(row.paidCreditAmount) }}</td>
                  <td>{{ row.auditorName || '--' }}</td>
                </tr>
                <tr v-if="emptyText(preview.costRows)">
                  <td colspan="7">{{ emptyText(preview.costRows) }}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section class="gross-profit-section">
            <h3>自费</h3>
            <table class="gross-profit-table">
              <thead>
                <tr>
                  <th>景区/项目</th>
                  <th>人数</th>
                  <th>销售额</th>
                  <th>成本</th>
                  <th>导游提成</th>
                  <th>公司利润</th>
                  <th>审核人</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in preview.optionalRows" :key="`optional-${index}`">
                  <td>{{ row.projectName || '--' }}</td>
                  <td>{{ plain(row.guestCount) }}人</td>
                  <td class="amount">{{ money(row.salesAmount) }}</td>
                  <td class="amount">{{ money(row.costAmount) }}</td>
                  <td class="amount">{{ money(row.guideCommissionAmount) }}</td>
                  <td class="amount">{{ money(row.companyProfit) }}</td>
                  <td>{{ row.auditorName || '--' }}</td>
                </tr>
                <tr v-if="emptyText(preview.optionalRows)">
                  <td colspan="7">{{ emptyText(preview.optionalRows) }}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section class="gross-profit-section">
            <h3>购物</h3>
            <table class="gross-profit-table">
              <thead>
                <tr>
                  <th>购物店</th>
                  <th>进店人数</th>
                  <th>人头费</th>
                  <th>销售额</th>
                  <th>公司返佣</th>
                  <th>导游返佣</th>
                  <th>公司利润</th>
                  <th>审核人</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in preview.shoppingRows" :key="`shopping-${index}`">
                  <td>{{ row.shopName || '--' }}</td>
                  <td>{{ plain(row.entryCount) }}人</td>
                  <td class="amount">{{ money(row.headFeeAmount) }}</td>
                  <td class="amount">{{ money(row.consumptionAmount) }}</td>
                  <td class="amount">{{ money(row.companyRebateAmount) }}</td>
                  <td class="amount">{{ money(row.guideCommissionAmount) }}</td>
                  <td class="amount">{{ money(row.companyProfit) }}</td>
                  <td>{{ row.auditorName || '--' }}</td>
                </tr>
                <tr v-if="emptyText(preview.shoppingRows)">
                  <td colspan="8">{{ emptyText(preview.shoppingRows) }}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section class="gross-profit-section">
            <h3>毛利</h3>
            <table class="gross-profit-table summary-table">
              <thead>
                <tr>
                  <th>订单收入</th>
                  <th>购物反佣</th>
                  <th>加点利润</th>
                  <th>成本支出</th>
                  <th>导服费</th>
                  <th>合计毛利</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td class="amount">{{ money(summary.orderIncome) }}</td>
                  <td class="amount">{{ money(summary.shoppingProfit) }}</td>
                  <td class="amount">{{ money(summary.optionalProfit) }}</td>
                  <td class="amount">{{ money(summary.regularCost) }}</td>
                  <td class="amount">{{ money(summary.guideFee) }}</td>
                  <td class="amount total-profit">{{ money(summary.grossProfit) }}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section class="gross-profit-section">
            <h3>业务员明细</h3>
            <table class="gross-profit-table">
              <thead>
                <tr>
                  <th>业务员</th>
                  <th>应收金额</th>
                  <th>已收金额</th>
                  <th>分摊毛利</th>
                  <th>毛利率</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in preview.salespersonRows" :key="`salesperson-${index}`">
                  <td>{{ row.salespersonName || '--' }}</td>
                  <td class="amount">{{ money(row.receivableAmount) }}</td>
                  <td class="amount">{{ money(row.receivedAmount) }}</td>
                  <td class="amount">{{ money(row.grossProfit) }}</td>
                  <td class="amount">{{ money(row.grossProfitRate) }}%</td>
                </tr>
                <tr v-if="emptyText(preview.salespersonRows)">
                  <td colspan="5">{{ emptyText(preview.salespersonRows) }}</td>
                </tr>
              </tbody>
            </table>
          </section>
        </div>
      </Spin>
    </Card>
  </Page>
</template>

<style scoped>
.gross-profit-shell {
  border-radius: 6px;
}

.gross-profit-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  margin-bottom: 16px;
  border-bottom: 1px solid #edf0f5;
}

.toolbar-actions,
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.gross-profit-print-page {
  width: min(100%, 1080px);
  padding: 28px 32px 36px;
  margin: 0 auto;
  color: #111827;
  background: #fff;
  border: 1px solid #d9dee8;
}

.gross-profit-print-page h1,
.gross-profit-print-page h2,
.gross-profit-section h3 {
  margin: 0;
  color: #111827;
  text-align: center;
}

.gross-profit-print-page h1 {
  font-size: 22px;
  font-weight: 700;
  line-height: 32px;
}

.gross-profit-print-page h2 {
  margin-top: 4px;
  font-size: 15px;
  font-weight: 600;
  line-height: 24px;
}

.gross-profit-section {
  margin-top: 16px;
}

.gross-profit-section h3 {
  margin-bottom: 6px;
  font-size: 15px;
  font-weight: 700;
}

.gross-profit-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 12px;
  line-height: 1.45;
}

.gross-profit-table th,
.gross-profit-table td {
  padding: 6px 7px;
  text-align: center;
  vertical-align: middle;
  word-break: break-word;
  border: 1px solid #222;
}

.gross-profit-table th {
  font-weight: 700;
  background: #f7f8fa;
}

.meta-table {
  margin-top: 14px;
}

.meta-table th {
  width: 15%;
}

.meta-table td {
  width: 35%;
}

.text-left {
  text-align: left !important;
}

.multiline {
  white-space: pre-line;
}

.amount {
  font-variant-numeric: tabular-nums;
  text-align: right !important;
}

.summary-table th,
.summary-table td {
  font-size: 13px;
}

.total-profit {
  font-weight: 700;
  color: #b42318;
}

@media print {
  :global(body) {
    background: #fff !important;
  }

  :global(.vben-basic-layout-content),
  :global(.ant-layout-content) {
    padding: 0 !important;
    margin: 0 !important;
    background: #fff !important;
  }

  .gross-profit-toolbar,
  :global(.vben-page-header),
  :global(.ant-card-head) {
    display: none !important;
  }

  .gross-profit-shell {
    border: 0 !important;
    box-shadow: none !important;
  }

  .gross-profit-print-page {
    width: 100%;
    padding: 0;
    border: 0;
  }
}
</style>

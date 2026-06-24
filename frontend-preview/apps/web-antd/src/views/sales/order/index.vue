<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Input,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  Upload,
  message,
} from 'ant-design-vue';
import type { UploadProps } from 'ant-design-vue';
import { ref } from 'vue';

import { uploadAttachment } from '#/api/common/attachment';
import {
  type BookingAiImportApi,
  recognizeBookingAiImport,
} from '#/api/sales/booking-ai-import';

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '客户单位', dataIndex: 'customer', key: 'customer' },
  { title: '联系人', dataIndex: 'contact', key: 'contact' },
  { title: '人数(成人/儿童)', dataIndex: 'count', key: 'count' },
  { title: '总金额', dataIndex: 'amount', key: 'amount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' },
];

const data = ref([
  { id: 1, orderNo: 'OD20250510001', teamNo: 'HZ20250510-001', customer: '杭州阳光旅行社', contact: '陈明', count: '8/2', amount: 2980, status: '已确认' },
  { id: 2, orderNo: 'OD20250510002', teamNo: 'HZ20250510-001', customer: '上海春秋旅行社', contact: '周丽', count: '12/3', amount: 4470, status: '已确认' },
  { id: 3, orderNo: 'OD20250512001', teamNo: 'HZ20250512-002', customer: '南京中青旅', contact: '吴刚', count: '20/5', amount: 17200, status: '待付款' },
  { id: 4, orderNo: 'OD20250515001', teamNo: 'HZ20250515-003', customer: '苏州国旅', contact: '孙燕', count: '15/3', amount: 17640, status: '待确认' },
  { id: 5, orderNo: 'OD20250516001', teamNo: 'HZ20250516-004', customer: '宁波康辉旅行社', contact: '马超', count: '25/5', amount: 16740, status: '已确认' },
  { id: 6, orderNo: 'OD20250518001', teamNo: 'HZ20250518-005', customer: '温州中旅', contact: '林芳', count: '10/2', amount: 8640, status: '已取消' },
  { id: 7, orderNo: 'OD20250520001', teamNo: 'HZ20250520-006', customer: '绍兴旅游集散中心', contact: '何伟', count: '6/0', amount: 7680, status: '待确认' },
  { id: 8, orderNo: 'OD20250522001', teamNo: 'HZ20250522-007', customer: '嘉兴南湖旅行社', contact: '黄磊', count: '4/2', amount: 15480, status: '已确认' },
]);

const aiImportOpen = ref(false);
const aiImportLoading = ref(false);
const aiImportText = ref(sampleImportText());
const aiImportResult = ref<BookingAiImportApi.RecognizeResult>();
const aiImportAttachmentId = ref<number>();
const aiImportSourceType = ref('text');

const guestColumns = [
  { dataIndex: 'indexNo', key: 'indexNo', title: '序号', width: 58 },
  { dataIndex: 'name', key: 'name', title: '姓名', width: 90 },
  { dataIndex: 'certificateNo', key: 'certificateNo', title: '证件号', width: 180 },
  { dataIndex: 'gender', key: 'gender', title: '性别', width: 64 },
  { dataIndex: 'birthDate', key: 'birthDate', title: '出生年月', width: 110 },
  { dataIndex: 'age', key: 'age', title: '年龄', width: 64 },
  { dataIndex: 'phone', key: 'phone', title: '联系电话', width: 120 },
  { dataIndex: 'roomGroup', key: 'roomGroup', title: '分房', width: 110 },
  { dataIndex: 'leader', key: 'leader', title: '领队', width: 110 },
  { dataIndex: 'warnings', key: 'warnings', title: '身份证校验 / 提醒', width: 210 },
];

function openAiImport() {
  aiImportOpen.value = true;
}

async function runAiImportRecognize() {
  if (!aiImportText.value.trim() && !aiImportAttachmentId.value) {
    message.warning('请先上传确认单或粘贴文本');
    return;
  }
  aiImportLoading.value = true;
  try {
    aiImportResult.value = await recognizeBookingAiImport({
      attachmentId: aiImportAttachmentId.value,
      sourceType: aiImportSourceType.value,
      text: aiImportText.value,
    });
    message.success('识别完成，请人工确认后再填入表单');
  } finally {
    aiImportLoading.value = false;
  }
}

const beforeUploadAiImportFile: UploadProps['beforeUpload'] = async (file) => {
  const formData = new FormData();
  formData.append('file', file as File);
  formData.append('businessModule', '销售收客');
  formData.append('businessType', 'AI辅助录入确认单');
  try {
    const attachment = await uploadAttachment(formData);
    aiImportAttachmentId.value = attachment.id;
    aiImportSourceType.value = fileExt(file.name);
    aiImportText.value = '';
    message.success('文件已上传，可以开始识别');
  } catch {
    message.error('文件上传失败');
  }
  return false;
};

function fileExt(fileName?: string) {
  const value = fileName || '';
  const index = value.lastIndexOf('.');
  return index >= 0 ? value.slice(index + 1).toLowerCase() : 'text';
}

function fillDraftToForm() {
  message.info('已填入当前表单草稿；只填入当前表单，不会自动保存订单');
}

function valueText(value?: number | string) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}

function warningColor(record: BookingAiImportApi.GuestInfo) {
  if (record.warnings?.length) return 'red';
  if (record.suspectedLeader) return 'orange';
  if (record.leader) return 'green';
  return 'default';
}

function sampleImportText() {
  return `航班时间：
2026年6月25日 大连-上海CZ6533（0910-1120）
2026年6月30日 上海-大连CZ6536（1920-2115）
导游：王导 13800000000
客户：杭州百缘 叶菊莲 13521124678
报价：成人 2999 元/人，儿童 1999 元/人，单房差 580 元
附加说明：张三、李四住一间，王五为领队。
序号 姓名 年龄 出生日期 身份证号 电话 分房 备注
1 张三 44 1982-06-21 210204198206214832 13521124678 1房 领队
2 李四 15 2010-10-28 21020420101028741X 13521124678 1房`;
}
</script>

<template>
  <Page title="订单管理" description="管理旅游订单">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加订单</Button>
        <Button @click="openAiImport">AI辅助录入</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount.toLocaleString() }}
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已确认' ? 'green' : record.status === '待付款' ? 'orange' : record.status === '已取消' ? 'red' : 'blue'">{{ record.status }}</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">查看</Button>
            <Button type="link" size="small">编辑</Button>
          </template>
        </template>
      </Table>
    </Card>
    <Drawer
      v-model:open="aiImportOpen"
      title="AI辅助录入"
      width="980"
      destroy-on-close
    >
      <div class="ai-import-layout">
        <Alert
          class="ai-import-alert"
          show-icon
          type="info"
          message="AI 只辅助整理确认单资料，识别结果需要人工确认。点击填入只填入当前表单，不会自动保存订单。"
        />
        <Card size="small" title="资料来源">
          <Input.TextArea
            v-model:value="aiImportText"
            :rows="8"
            placeholder="粘贴确认单、微信消息、游客名单或报价文本"
          />
          <div class="ai-import-actions">
            <Space>
              <Upload :show-upload-list="false" :before-upload="beforeUploadAiImportFile">
                <Button>上传确认单</Button>
              </Upload>
              <Button type="primary" :loading="aiImportLoading" @click="runAiImportRecognize">
                开始识别
              </Button>
              <Button :disabled="!aiImportResult" @click="fillDraftToForm">
                填入表单
              </Button>
            </Space>
            <Typography.Text type="secondary">
              支持 Word / Excel / 文本；PDF / 图片需配置百炼视觉/OCR后使用。
            </Typography.Text>
          </div>
          <Typography.Text v-if="aiImportAttachmentId" type="secondary">
            已上传附件 ID：{{ aiImportAttachmentId }}，类型：{{ aiImportSourceType }}
          </Typography.Text>
        </Card>

        <Card v-if="aiImportResult" size="small" title="识别结果预览">
          <Alert
            v-if="aiImportResult.warnings?.length"
            class="ai-import-alert"
            type="warning"
            show-icon
            :message="aiImportResult.warnings.join('；')"
          />
          <Tabs>
            <Tabs.TabPane key="travel" tab="行程说明">
              <Descriptions bordered size="small" :column="2">
                <Descriptions.Item label="参团时间">{{ valueText(aiImportResult.travelInfo.joinDate) }}</Descriptions.Item>
                <Descriptions.Item label="来程">{{ valueText(aiImportResult.travelInfo.outboundOriginCity) }} -> {{ valueText(aiImportResult.travelInfo.outboundArrivalCity) }}</Descriptions.Item>
                <Descriptions.Item label="来程航班/车次">{{ valueText(aiImportResult.travelInfo.outboundTrafficNo) }}</Descriptions.Item>
                <Descriptions.Item label="来程时间">{{ valueText(aiImportResult.travelInfo.outboundDepartureTime) }} / {{ valueText(aiImportResult.travelInfo.outboundArrivalTime) }}</Descriptions.Item>
                <Descriptions.Item label="返程">{{ valueText(aiImportResult.travelInfo.returnDepartureCity) }} -> {{ valueText(aiImportResult.travelInfo.returnDestinationCity) }}</Descriptions.Item>
                <Descriptions.Item label="返程航班/车次">{{ valueText(aiImportResult.travelInfo.returnTrafficNo) }}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>
            <Tabs.TabPane key="guide" tab="导游相关">
              <Descriptions bordered size="small" :column="2">
                <Descriptions.Item label="导游">{{ valueText(aiImportResult.guideInfo.guideName) }}</Descriptions.Item>
                <Descriptions.Item label="导游电话">{{ valueText(aiImportResult.guideInfo.guidePhone) }}</Descriptions.Item>
                <Descriptions.Item label="全陪">{{ valueText(aiImportResult.guideInfo.escortName) }}</Descriptions.Item>
                <Descriptions.Item label="接待要求">{{ valueText(aiImportResult.guideInfo.receptionRequirement) }}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>
            <Tabs.TabPane key="customer" tab="客户信息">
              <Descriptions bordered size="small" :column="2">
                <Descriptions.Item label="客户单位">{{ valueText(aiImportResult.customerInfo.customerName) }}</Descriptions.Item>
                <Descriptions.Item label="联系人">{{ valueText(aiImportResult.customerInfo.contactName) }}</Descriptions.Item>
                <Descriptions.Item label="联系电话">{{ valueText(aiImportResult.customerInfo.contactPhone) }}</Descriptions.Item>
                <Descriptions.Item label="备注">{{ valueText(aiImportResult.customerInfo.remark) }}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>
            <Tabs.TabPane key="price" tab="价格信息">
              <Descriptions bordered size="small" :column="2">
                <Descriptions.Item label="成人价">{{ valueText(aiImportResult.priceInfo.adultPrice) }}</Descriptions.Item>
                <Descriptions.Item label="儿童价">{{ valueText(aiImportResult.priceInfo.childPrice) }}</Descriptions.Item>
                <Descriptions.Item label="老人价">{{ valueText(aiImportResult.priceInfo.seniorPrice) }}</Descriptions.Item>
                <Descriptions.Item label="单房差">{{ valueText(aiImportResult.priceInfo.singleRoomDifference) }}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>
            <Tabs.TabPane key="additional" tab="附加说明">
              <Descriptions bordered size="small" :column="1">
                <Descriptions.Item label="附加说明">{{ valueText(aiImportResult.additionalInfo.notes) }}</Descriptions.Item>
                <Descriptions.Item label="接待标准">{{ valueText(aiImportResult.additionalInfo.receptionStandard) }}</Descriptions.Item>
                <Descriptions.Item label="分房说明">{{ valueText(aiImportResult.additionalInfo.roomingNote) }}</Descriptions.Item>
                <Descriptions.Item label="领队说明">{{ valueText(aiImportResult.additionalInfo.leaderNote) }}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>
            <Tabs.TabPane key="guests" tab="游客名单">
              <Table
                :columns="guestColumns"
                :data-source="aiImportResult.guests"
                :pagination="false"
                row-key="indexNo"
                size="small"
                :row-class-name="(record) => record.warnings?.length ? 'guest-warning-row' : ''"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'leader'">
                    <Space size="small">
                      <Tag v-if="record.leader" color="green">领队</Tag>
                      <Tag v-else-if="record.suspectedLeader" color="orange">疑似领队</Tag>
                      <span v-else>-</span>
                      <Typography.Text v-if="record.leaderSourceText" type="secondary">
                        {{ record.leaderSourceText }}
                      </Typography.Text>
                    </Space>
                  </template>
                  <template v-else-if="column.key === 'warnings'">
                    <Tag :color="warningColor(record)">
                      {{ record.warnings?.length ? record.warnings.join('；') : record.idCardValid === false ? '身份证校验失败' : '身份证校验通过' }}
                    </Tag>
                  </template>
                </template>
              </Table>
            </Tabs.TabPane>
          </Tabs>
        </Card>
      </div>
    </Drawer>
  </Page>
</template>

<style scoped>
.ai-import-layout {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ai-import-alert {
  margin-bottom: 12px;
}

.ai-import-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

:deep(.guest-warning-row) td {
  background: #fff1f0;
}
</style>

import {
  createCustomerCredit,
  deleteCustomerCredit,
  getCustomerCreditPage,
  updateCustomerCredit,
} from '#/api/customer/credit';
import {
  getCustomerUnitPage,
  type CustomerUnitApi,
} from '#/api/customer/unit';
import {
  createCustomerProductAuth,
  deleteCustomerProductAuth,
  getCustomerProductAuthPage,
  updateCustomerProductAuth,
} from '#/api/customer/product-auth';
import {
  createGroundAgent,
  deleteGroundAgent,
  getGroundAgentPage,
  updateGroundAgent,
} from '#/api/purchase/ground-agent';
import {
  createHotelResource,
  deleteHotelResource,
  getHotelResourcePage,
  updateHotelResource,
} from '#/api/purchase/hotel';
import {
  createPurchaseRelation,
  deletePurchaseRelation,
  getPurchaseRelationPage,
  updatePurchaseRelation,
} from '#/api/purchase/relation';
import {
  createScenicResource,
  deleteScenicResource,
  getScenicResourcePage,
  updateScenicResource,
} from '#/api/purchase/scenic';
import {
  createSupplier,
  deleteSupplier,
  getSupplierAll,
  getSupplierPage,
  updateSupplier,
  type SupplierApi,
} from '#/api/purchase/supplier';
import type { CrudPageConfig } from './CrudPage.vue';

const customerOptions: { label: string; value: number }[] = [];
const supplierOptions: { label: string; value: number }[] = [];

const activeStatusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const authStatusOptions = [
  { label: '有效', value: 'active' },
  { label: '暂停', value: 'suspended' },
  { label: '已到期', value: 'expired' },
];

const supplierStatusOptions = [
  { label: '有效', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const relationStatusOptions = [
  { label: '有效', value: 'active' },
  { label: '停用', value: 'disabled' },
  { label: '过期', value: 'expired' },
];

const groundAgentStatusOptions = [
  { label: '进行中', value: 'active' },
  { label: '停用', value: 'disabled' },
  { label: '已完成', value: 'completed' },
];

const overLimitActionOptions = [
  { label: '不处理', value: 'none' },
  { label: '提醒', value: 'remind' },
  { label: '转审批', value: 'approval' },
];

const supplierCategoryOptions = [
  { label: '景区', value: 'scenic' },
  { label: '酒店', value: 'hotel' },
  { label: '餐厅', value: 'restaurant' },
  { label: '车队', value: 'vehicle' },
  { label: '大交通', value: 'traffic' },
  { label: '其它', value: 'other' },
  { label: '地接', value: 'ground_agent' },
  { label: '购物', value: 'shopping' },
  { label: '通用', value: 'common' },
];

const purchaseResourceTypeOptions = [
  { label: '酒店', value: 'hotel' },
  { label: '景区', value: 'scenic' },
  { label: '车辆', value: 'vehicle' },
  { label: '餐厅', value: 'restaurant' },
  { label: '导游', value: 'guide' },
  { label: '地接', value: 'ground_agent' },
  { label: '票务', value: 'ticket' },
  { label: '购物', value: 'shopping' },
  { label: '其他', value: 'other' },
];

async function loadCustomerOptions() {
  const result = await getCustomerUnitPage({ page: 1, pageSize: 200 });
  customerOptions.splice(
    0,
    customerOptions.length,
    ...result.items.map((item: CustomerUnitApi.CustomerUnit) => ({
      label: item.customerName,
      value: item.id,
    })),
  );
}

async function loadSupplierOptions(category?: SupplierApi.Category) {
  const result = await getSupplierAll(category);
  supplierOptions.splice(
    0,
    supplierOptions.length,
    ...result.map((item) => ({ label: item.supplierName, value: item.id })),
  );
}

export const customerCreditConfig: CrudPageConfig<Record<string, any>> = {
  title: '客户授信与实时应收',
  description: '维护客户额度、已占用额度和超限提醒口径，当前阶段先做台账维护。',
  loadOptions: loadCustomerOptions,
  pageApi: getCustomerCreditPage,
  create: (data) => createCustomerCredit(data as any),
  update: (id, data) => updateCustomerCredit(id, data as any),
  delete: deleteCustomerCredit,
  queryFields: [
    { key: 'keyword', label: '客户关键字' },
    { key: 'status', label: '状态', type: 'select', options: activeStatusOptions },
  ],
  fields: [
    { key: 'customerId', label: '客户单位', type: 'select', options: customerOptions, required: true },
    { key: 'creditLimit', label: '授信额度', type: 'money' },
    { key: 'occupiedAmount', label: '已占用额度', type: 'money' },
    { key: 'pendingApprovalAmount', label: '审批中额度', type: 'money' },
    { key: 'warningThreshold', label: '预警阈值', type: 'money' },
    { key: 'overLimitAction', label: '超限处理', type: 'select', options: overLimitActionOptions },
    { key: 'status', label: '状态', type: 'select', options: activeStatusOptions },
    { key: 'remark', label: '备注', type: 'textarea' },
  ],
  columns: [
    { key: 'customerName', title: '客户单位', width: 190 },
    { key: 'creditLimit', title: '授信额度', format: 'money', width: 130 },
    { key: 'occupiedAmount', title: '已占用', format: 'money', width: 130 },
    { key: 'pendingApprovalAmount', title: '审批中', format: 'money', width: 130 },
    { key: 'availableAmount', title: '可用额度', format: 'money', width: 130 },
    { key: 'overLimitAction', title: '超限处理', format: 'status', width: 110 },
    { key: 'status', title: '状态', format: 'status', width: 100 },
    { key: 'remark', title: '备注', width: 220 },
  ],
};

export const customerProductAuthConfig: CrudPageConfig<Record<string, any>> = {
  title: '产品授权',
  description: '维护客户可下单产品范围、授权期限和授权状态。',
  loadOptions: loadCustomerOptions,
  pageApi: getCustomerProductAuthPage,
  create: (data) => createCustomerProductAuth(data as any),
  update: (id, data) => updateCustomerProductAuth(id, data as any),
  delete: deleteCustomerProductAuth,
  queryFields: [
    { key: 'keyword', label: '产品关键字' },
    { key: 'customerId', label: '客户单位', type: 'select', options: customerOptions },
    { key: 'status', label: '授权状态', type: 'select', options: authStatusOptions },
  ],
  fields: [
    { key: 'customerId', label: '客户单位', type: 'select', options: customerOptions, required: true },
    { key: 'productCode', label: '产品编码' },
    { key: 'productName', label: '产品名称', required: true },
    { key: 'authorizedStartDate', label: '授权开始', type: 'date' },
    { key: 'authorizedEndDate', label: '授权结束', type: 'date' },
    { key: 'authorizationStatus', label: '授权状态', type: 'select', options: authStatusOptions },
    { key: 'saleScope', label: '可售范围', type: 'textarea' },
    { key: 'remark', label: '备注', type: 'textarea' },
  ],
  columns: [
    { key: 'customerName', title: '客户单位', width: 180 },
    { key: 'productCode', title: '产品编码', width: 130 },
    { key: 'productName', title: '产品名称', width: 200 },
    { key: 'authorizedStartDate', secondaryKey: 'authorizedEndDate', title: '授权期限', format: 'dateRange', width: 220 },
    { key: 'authorizationStatus', title: '状态', format: 'status', width: 100 },
    { key: 'saleScope', title: '可售范围', width: 260 },
  ],
};

export const supplierConfig: CrudPageConfig<Record<string, any>> = {
  actionWidth: 230,
  title: '供应商管理',
  description: '维护景区、酒店、餐厅、车队、大交通、地接、购物等采购供应商档案。',
  loadOptions: loadCustomerOptions,
  pageApi: getSupplierPage,
  create: (data) => createSupplier(data as any),
  update: (id, data) => updateSupplier(id, data as any),
  delete: deleteSupplier,
  queryFields: [
    { key: 'keyword', label: '供应商关键字' },
    { key: 'category', label: '分类', type: 'select', options: supplierCategoryOptions },
    { key: 'status', label: '状态', type: 'select', options: supplierStatusOptions },
  ],
  fields: [
    { key: 'area', label: '所在地', type: 'region' },
    { key: 'supplierName', label: '公司名称', required: true },
    { key: 'contactName', label: '负责人', required: true },
    { key: 'supplierCategory', label: '商家分类', type: 'select', options: supplierCategoryOptions },
    { key: 'basicInfo', label: '基础信息', type: 'textarea' },
    { key: 'contactPhone', label: '联系电话' },
    { key: 'faxNumber', label: '传真号码' },
    { key: 'officeAddress', label: '办公地址' },
    { key: 'remark', label: '备注信息', type: 'textarea' },
    { key: 'status', label: '客户状态', type: 'select', options: supplierStatusOptions, required: true },
  ],
  columns: [
    { key: 'supplierName', title: '供应商名称', width: 220 },
    { key: 'supplierCategory', title: '分类', format: 'status', width: 110 },
    { key: 'area', title: '所在地', format: 'area', width: 170 },
    { key: 'contactName', title: '联系人', width: 120 },
    { key: 'basicInfo', title: '基础信息', width: 240 },
    { key: 'defaultResourceNames', title: '默认供应商', width: 220 },
    { key: 'priceSummary', title: '报价', width: 320 },
    { key: 'contactPhone', title: '电话', width: 140 },
    { key: 'status', title: '状态', format: 'status', width: 110 },
  ],
};

export const purchaseRelationConfig: CrudPageConfig<Record<string, any>> = {
  title: '采购关系管理',
  description: '维护资源和供应商之间的采购价、价格有效期、结算方式和优先级。',
  loadOptions: () => loadSupplierOptions(),
  pageApi: getPurchaseRelationPage,
  create: (data) => createPurchaseRelation(data as any),
  update: (id, data) => updatePurchaseRelation(id, data as any),
  delete: deletePurchaseRelation,
  queryFields: [
    { key: 'keyword', label: '资源名称' },
    { key: 'resourceType', label: '资源类型', type: 'select', options: purchaseResourceTypeOptions },
    { key: 'status', label: '状态', type: 'select', options: relationStatusOptions },
  ],
  fields: [
    { key: 'resourceType', label: '资源类型', type: 'select', options: purchaseResourceTypeOptions },
    { key: 'resourceName', label: '资源名称', required: true },
    { key: 'supplierId', label: '供应商', type: 'select', options: supplierOptions, required: true },
    { key: 'purchasePrice', label: '采购价', type: 'money' },
    { key: 'priceUnit', label: '价格单位' },
    { key: 'settlementMethod', label: '结算方式' },
    { key: 'validFrom', label: '有效期起', type: 'date' },
    { key: 'validTo', label: '有效期止', type: 'date' },
    { key: 'priorityLevel', label: '优先级', type: 'number' },
    { key: 'status', label: '状态', type: 'select', options: relationStatusOptions },
    { key: 'remark', label: '备注', type: 'textarea' },
  ],
  columns: [
    { key: 'resourceName', title: '资源名称', width: 220 },
    { key: 'resourceType', title: '资源类型', format: 'status', width: 110 },
    { key: 'supplierName', title: '供应商', width: 200 },
    { key: 'purchasePrice', title: '采购价', format: 'money', width: 130 },
    { key: 'priceUnit', title: '单位', width: 90 },
    { key: 'validFrom', secondaryKey: 'validTo', title: '价格有效期', format: 'dateRange', width: 220 },
    { key: 'priorityLevel', title: '优先级', width: 90 },
    { key: 'status', title: '状态', format: 'status', width: 100 },
  ],
};

export const hotelResourceConfig: CrudPageConfig<Record<string, any>> = {
  title: '酒店资源基础库',
  description: '维护酒店档案、房型、城市区域、供应商、采购价和协议价。',
  loadOptions: () => loadSupplierOptions('hotel'),
  pageApi: getHotelResourcePage,
  create: (data) => createHotelResource(data as any),
  update: (id, data) => updateHotelResource(id, data as any),
  delete: deleteHotelResource,
  queryFields: [
    { key: 'keyword', label: '酒店/房型' },
    { key: 'city', label: '城市' },
    { key: 'status', label: '状态', type: 'select', options: activeStatusOptions },
  ],
  fields: [
    { key: 'hotelName', label: '酒店名称', required: true },
    { key: 'roomType', label: '房型', required: true },
    { key: 'city', label: '城市' },
    { key: 'area', label: '区域' },
    { key: 'address', label: '地址' },
    { key: 'starStandard', label: '星钻标准' },
    { key: 'supplierId', label: '供应商', type: 'select', options: supplierOptions },
    { key: 'purchasePrice', label: '采购价', type: 'money' },
    { key: 'agreementPrice', label: '协议价', type: 'money' },
    { key: 'priceUnit', label: '价格单位' },
    { key: 'validFrom', label: '有效期起', type: 'date' },
    { key: 'validTo', label: '有效期止', type: 'date' },
    { key: 'contactName', label: '联系人' },
    { key: 'contactPhone', label: '联系电话' },
    { key: 'status', label: '状态', type: 'select', options: activeStatusOptions },
    { key: 'remark', label: '备注', type: 'textarea' },
  ],
  columns: [
    { key: 'hotelName', title: '酒店名称', width: 200 },
    { key: 'roomType', title: '房型', width: 140 },
    { key: 'city', title: '城市', width: 110 },
    { key: 'area', title: '区域', width: 120 },
    { key: 'supplierName', title: '供应商', width: 180 },
    { key: 'purchasePrice', title: '采购价', format: 'money', width: 130 },
    { key: 'agreementPrice', title: '协议价', format: 'money', width: 130 },
    { key: 'validFrom', secondaryKey: 'validTo', title: '价格有效期', format: 'dateRange', width: 220 },
    { key: 'status', title: '状态', format: 'status', width: 100 },
  ],
};

export const scenicResourceConfig: CrudPageConfig<Record<string, any>> = {
  title: '景区资源基础库',
  description: '维护景区档案、票种、供应商、采购价、协议价和免票半票规则。',
  loadOptions: () => loadSupplierOptions('scenic'),
  pageApi: getScenicResourcePage,
  create: (data) => createScenicResource(data as any),
  update: (id, data) => updateScenicResource(id, data as any),
  delete: deleteScenicResource,
  queryFields: [
    { key: 'keyword', label: '景区/票种' },
    { key: 'city', label: '城市' },
    { key: 'status', label: '状态', type: 'select', options: activeStatusOptions },
  ],
  fields: [
    { key: 'scenicName', label: '景区名称', required: true },
    { key: 'ticketType', label: '票种', required: true },
    { key: 'city', label: '城市' },
    { key: 'area', label: '区域' },
    { key: 'address', label: '地址' },
    { key: 'supplierId', label: '供应商', type: 'select', options: supplierOptions },
    { key: 'purchasePrice', label: '采购价', type: 'money' },
    { key: 'agreementPrice', label: '协议价', type: 'money' },
    { key: 'priceUnit', label: '价格单位' },
    { key: 'validFrom', label: '有效期起', type: 'date' },
    { key: 'validTo', label: '有效期止', type: 'date' },
    { key: 'freeTicketRule', label: '免票规则', type: 'textarea' },
    { key: 'halfTicketRule', label: '半票规则', type: 'textarea' },
    { key: 'contactName', label: '联系人' },
    { key: 'contactPhone', label: '联系电话' },
    { key: 'status', label: '状态', type: 'select', options: activeStatusOptions },
    { key: 'remark', label: '备注', type: 'textarea' },
  ],
  columns: [
    { key: 'scenicName', title: '景区名称', width: 200 },
    { key: 'ticketType', title: '票种', width: 140 },
    { key: 'city', title: '城市', width: 110 },
    { key: 'supplierName', title: '供应商', width: 180 },
    { key: 'purchasePrice', title: '采购价', format: 'money', width: 130 },
    { key: 'agreementPrice', title: '协议价', format: 'money', width: 130 },
    { key: 'validFrom', secondaryKey: 'validTo', title: '价格有效期', format: 'dateRange', width: 220 },
    { key: 'status', title: '状态', format: 'status', width: 100 },
  ],
};

export const groundAgentConfig: CrudPageConfig<Record<string, any>> = {
  title: '地接外委管理',
  description: '维护地接社档案、外委任务、行程要求、总预算和确认单信息。',
  pageApi: getGroundAgentPage,
  create: (data) => createGroundAgent(data as any),
  update: (id, data) => updateGroundAgent(id, data as any),
  delete: deleteGroundAgent,
  queryFields: [
    { key: 'keyword', label: '地接/任务' },
    { key: 'city', label: '城市' },
    { key: 'status', label: '状态', type: 'select', options: groundAgentStatusOptions },
  ],
  fields: [
    { key: 'groundAgentName', label: '地接社名称', required: true },
    { key: 'city', label: '城市' },
    { key: 'contactName', label: '联系人' },
    { key: 'contactPhone', label: '联系电话' },
    { key: 'taskName', label: '外委任务' },
    { key: 'totalBudget', label: '总预算', type: 'money' },
    { key: 'confirmationFileUrl', label: '确认单地址' },
    { key: 'status', label: '状态', type: 'select', options: groundAgentStatusOptions },
    { key: 'itineraryRequirement', label: '行程要求', type: 'textarea' },
    { key: 'remark', label: '备注', type: 'textarea' },
  ],
  columns: [
    { key: 'groundAgentName', title: '地接社', width: 200 },
    { key: 'city', title: '城市', width: 110 },
    { key: 'taskName', title: '外委任务', width: 210 },
    { key: 'contactName', title: '联系人', width: 120 },
    { key: 'contactPhone', title: '电话', width: 140 },
    { key: 'totalBudget', title: '总预算', format: 'money', width: 130 },
    { key: 'status', title: '状态', format: 'status', width: 100 },
  ],
};

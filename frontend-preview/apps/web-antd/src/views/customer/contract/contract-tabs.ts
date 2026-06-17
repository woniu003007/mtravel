import type { ContractApi } from '#/api/contract';

export type ContractTabKey = ContractApi.ContractType;

export interface ContractTypeTab {
  key: ContractTabKey;
  label: string;
}

export const contractTypeTabs: ContractTypeTab[] = [
  { key: 'customer', label: '分销商' },
  { key: 'scenic', label: '景区' },
  { key: 'hotel', label: '酒店' },
  { key: 'restaurant', label: '餐厅' },
  { key: 'vehicle', label: '车队' },
  { key: 'traffic', label: '大交通' },
  { key: 'other', label: '其它' },
  { key: 'ground_agent', label: '地接' },
  { key: 'guide', label: '导游' },
  { key: 'finance_fee', label: '财务费用' },
  { key: 'current_refund', label: '现收现退' },
  { key: 'extra_fee', label: '附加费用' },
  { key: 'shopping', label: '购物' },
];

export function supplierCategoryByContractTab(tabKey: ContractTabKey) {
  return tabKey === 'customer' ? undefined : tabKey;
}

export function contractTabLabel(tabKey: ContractTabKey) {
  return contractTypeTabs.find((item) => item.key === tabKey)?.label || '合同';
}

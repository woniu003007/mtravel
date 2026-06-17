import type { EnterpriseExpenseItemApi } from '#/api/enterprise/expense-item';

export type ExpenseItemTabKey = 'all' | EnterpriseExpenseItemApi.ResourceType;

export interface ExpenseItemTab {
  key: ExpenseItemTabKey;
  label: string;
}

export const expenseItemTypeOptions: Array<{
  label: string;
  value: EnterpriseExpenseItemApi.ResourceType;
}> = [
  { label: '景区', value: 'scenic' },
  { label: '酒店', value: 'hotel' },
  { label: '餐厅', value: 'restaurant' },
  { label: '车队', value: 'vehicle' },
  { label: '大交通', value: 'traffic' },
  { label: '其它', value: 'other' },
  { label: '杂费', value: 'misc' },
  { label: '地接', value: 'ground_agent' },
  { label: '附加费用', value: 'extra_fee' },
  { label: '购物', value: 'shopping' },
];

export const expenseItemTabs: ExpenseItemTab[] = [
  { key: 'all', label: '全部' },
  ...expenseItemTypeOptions.map((item) => ({
    key: item.value,
    label: item.label,
  })),
];

export function expenseItemTypeLabel(value?: string) {
  return expenseItemTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

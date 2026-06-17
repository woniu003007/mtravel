import type { EnterpriseProductDictionaryApi } from '#/api/enterprise/product-dictionary';

export interface ProductDictionaryTab {
  key: EnterpriseProductDictionaryApi.DictType;
  label: string;
  description: string;
}

export const productDictionaryTabs: ProductDictionaryTab[] = [
  {
    key: 'business_type',
    label: '业务类型',
    description: '维护产品所属业务口径，例如疗休养、定制团、地接团。',
  },
  {
    key: 'reception_standard',
    label: '接待标准',
    description: '维护产品住宿和接待标准，例如商务/快捷、携程四钻、携程五钻。',
  },
  {
    key: 'product_theme',
    label: '产品主题',
    description: '维护产品主题标签，例如观光、亲子游、夕阳红、研学。',
  },
];

export const productDictionaryTypeOptions = productDictionaryTabs.map((item) => ({
  label: item.label,
  value: item.key,
}));

export function productDictionaryTypeLabel(value?: string) {
  return productDictionaryTabs.find((item) => item.key === value)?.label || value || '-';
}

export function productDictionaryTypeDescription(value?: string) {
  return productDictionaryTabs.find((item) => item.key === value)?.description || '';
}

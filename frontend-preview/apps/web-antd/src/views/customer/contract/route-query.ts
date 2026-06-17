import type { LocationQueryRaw } from 'vue-router';

/**
 * 客户合同页路由参数工具。
 *
 * <p>客户单位列表的“合同管理”操作会跳到客户销售合同页，并通过 customerId
 * 锁定当前客户。这里集中处理路由参数，避免页面里散落字符串转换逻辑。</p>
 */
export interface CustomerContractRouteParams {
  customerId: number;
}

export interface ContractRouteQuery {
  contractType?: string;
  customerId?: number;
  supplierId?: number;
}

/**
 * 构造客户单位行内“合同管理”的目标路由。
 */
export function buildCustomerContractRoute(params: CustomerContractRouteParams) {
  return {
    path: '/customer/contract',
    query: { customerId: String(params.customerId) } satisfies LocationQueryRaw,
  };
}

/**
 * 从路由 query 中解析客户 ID。
 */
export function parseCustomerId(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value;
  if (typeof raw !== 'string' && typeof raw !== 'number') {
    return undefined;
  }
  const parsed = Number(raw);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined;
}

/**
 * 将路由 query 转换为合同分页查询初始条件。
 */
export function contractQueryFromRoute(query: Record<string, unknown>) {
  return {
    contractType: parseContractType(query.contractType || query.category),
    customerId: parseCustomerId(query.customerId),
    supplierId: parseCustomerId(query.supplierId),
  };
}

/** 只接受合同管理页已经定义的合同类型，避免路由参数制造不存在的页签。 */
function parseContractType(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value;
  const allowed = new Set([
    'customer', 'scenic', 'hotel', 'restaurant', 'vehicle', 'traffic', 'other',
    'ground_agent', 'guide', 'finance_fee', 'current_refund', 'extra_fee', 'shopping',
  ]);
  return typeof raw === 'string' && allowed.has(raw) ? raw : undefined;
}

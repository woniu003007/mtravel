/**
 * 采购关系列表字段与旧系统保持一致。
 *
 * 资源类型继续作为查询条件使用，不在列表中重复展示；成团数量属于历史价格规则，
 * 当前页面不再录入或展示。
 */
export const purchaseRelationColumns = [
  { dataIndex: 'location', key: 'location', title: '所在地', width: 210 },
  { dataIndex: 'resourceName', key: 'resourceName', title: '资源名称', width: 220 },
  { dataIndex: 'supplierName', key: 'supplierName', title: '供应商', width: 220 },
  { dataIndex: 'contactName', key: 'contactName', title: '负责人', width: 120 },
  { dataIndex: 'contactPhone', key: 'contactPhone', title: '电话', width: 150 },
  { dataIndex: 'createdBy', key: 'createdBy', title: '创建人', width: 110 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间', width: 170 },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 340 },
];

export type ProductDesignerArrangementTarget =
  | { allowRepeat?: boolean; role: 'accommodation' | 'ground_service' | 'itinerary' }
  | { requiresMealSelection: true }
  | { unsupportedInDayMap: true };

/**
 * 资源地图只决定资源落入的业务区块；餐厅缺少餐次上下文时由页面弹出小型餐次选择器。
 */
export function resolveArrangementTarget(resourceType: string): ProductDesignerArrangementTarget {
  switch (resourceType) {
    case 'hotel': {
      return { role: 'accommodation' };
    }
    case 'restaurant': {
      return { requiresMealSelection: true };
    }
    case 'ground_agent': {
      return { unsupportedInDayMap: true };
    }
    case 'traffic':
    case 'vehicle': {
      return { unsupportedInDayMap: true };
    }
    default: {
      return { role: 'itinerary' };
    }
  }
}

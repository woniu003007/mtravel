import type { SalesProductDesignerApi } from '#/api/sales/product-designer';

import './product-resource-map-marker.css';

export interface ProductResourceMarkerState {
  arranged?: boolean;
  pending?: boolean;
  selected?: boolean;
}

/**
 * 将资源名称收敛为地图和编排区共用的短展示文本，避免不同入口出现不同名称。
 */
export function formatProductResourceName(resourceName?: string) {
  const normalized = resourceName?.replace(/\s+/g, ' ').trim();
  return normalized || '未命名资源';
}

function escapeHtml(value: string) {
  return value.replace(/[&<>"']/g, (character) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  })[character] || character);
}

/**
 * 紧凑地图与全屏地图共用的高德标记 HTML。
 * 资源类型不再用杂色区分，统一采用地图蓝定位针；资源名经过转义后才插入 HTML，
 * 避免资源主档内容影响地图 DOM。
 */
export function buildProductResourceMarkerHtml(
  resource: Pick<SalesProductDesignerApi.MapResource, 'resourceName' | 'resourceType'>,
  state: ProductResourceMarkerState = {},
) {
  const stateClasses = [
    state.selected && 'is-selected',
    state.arranged && 'is-arranged',
    state.pending && 'is-pending',
  ].filter(Boolean).join(' ');
  const name = escapeHtml(formatProductResourceName(resource.resourceName));

  return `<div class="product-resource-map-marker ${stateClasses}"><span class="product-resource-map-marker__pin" aria-hidden="true"></span><span class="product-resource-map-marker__label">${name}</span></div>`;
}

/** 地图覆盖物统一使用的层级，选中资源应始终在普通资源之上。 */
export function productResourceMarkerZIndex(state: ProductResourceMarkerState = {}) {
  if (state.selected) return 120;
  if (state.arranged) return 40;
  return 10;
}

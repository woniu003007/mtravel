import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('product resource map workspace', () => {
  it('provides a full-screen resource map with the designer filters and selectable resource list', () => {
    const source = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain('资源地图 · D{{ activeDayNo }}');
    expect(source).toContain('搜索资源名称');
    expect(source).toContain('资源列表');
    expect(source).toContain('activateResource');
    expect(source).toContain("emit('activate-resource'");
    expect(source).toContain('resource-map-workspace');
  });

  it('uses clustered markers with a selected-resource fallback when the map plugin is unavailable', () => {
    const source = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain('MarkerCluster');
    expect(source).toContain('clusterData');
    expect(source).toContain('originMarker');
    expect(source).toContain('forbidenWebGL');
    expect(source).toContain('buildProductResourceMarkerHtml');
    expect(source).toContain('productResourceMarkerZIndex');
    expect(source).toContain('selectedResourceId');
    expect(source).toContain('fallbackMarkers');
    expect(source).toContain('map-cluster-fallback');
  });

  it('shows resource names directly for a small result set and clusters only dense maps', () => {
    const source = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain('const markerClusterThreshold = 30');
    expect(source).toContain('markerInstances.length <= markerClusterThreshold');
    expect(source).toContain('mapInstance.add(markerInstances)');
    expect(source.indexOf('markerInstances.length <= markerClusterThreshold'))
      .toBeLessThan(source.indexOf('await loadMarkerClusterPlugin(AMap)'));
  });

  it('keeps the full-screen resource list dense and directly selectable', () => {
    const source = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain('grid-template-columns: minmax(0, 1fr) 292px');
    expect(source).toContain('min-height: 50px');
    expect(source).toContain('resource-map-row');
    expect(source).toContain('activateResource');
  });

  it('renders clustered and ordinary points with the shared product resource marker', () => {
    const source = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain('content: buildProductResourceMarkerHtml');
    expect(source).toContain('originMarker: markerInstances[index]');
  });

  it('uses one shared marker stylesheet for the compact and full-screen maps', () => {
    const compact = readAppFile('src/views/sales/product/designer.vue');
    const fullscreen = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');
    const markerCss = readAppFile('src/views/sales/product/components/product-resource-map-marker.css');

    for (const token of [
      'padding: 3px 7px',
      'color: #334155',
      'font-size: 11px',
      'line-height: 16px',
      'background: rgb(255 255 255 / 94%)',
      'border: 1px solid #b7d7ff',
      'border-radius: 4px',
    ]) {
      expect(markerCss).toContain(token);
    }

    expect(compact).toContain('buildProductResourceMarkerHtml');
    expect(fullscreen).toContain('buildProductResourceMarkerHtml');
    expect(markerCss).toContain('.product-resource-map-marker__label');
  });

  it('keeps resource selection in the full-screen workspace instead of duplicating a compact list', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');

    expect(source).toContain('ProductResourceMapWorkspace');
    expect(source).toContain('mapFullscreenOpen');
    expect(source).toContain('全屏地图');
    expect(source).toContain('@activate-resource="activateMapResource"');
    expect(source).not.toContain('<div class="resource-list-header">');
    expect(source).not.toContain('<div class="resource-list"');
    expect(source).not.toContain('getSalesProductDesignerResourceMap');
  });

  it('preserves the current map city after arranging a resource', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');

    expect(source).toContain('async function loadDetail(options: { syncMapFilters?: boolean } = {})');
    expect(source).toContain('if (options.syncMapFilters) syncMapFiltersToActiveDay()');
    expect(source).toContain('await loadDetail({ syncMapFilters: true })');
    expect(source).toContain('await loadDetail();');
  });

  it('keeps the compact map zoom unchanged when selecting a resource', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');

    expect(source).toContain('function selectMapResource(resourceId: number) {');
    expect(source).toContain('selectedMapResourceId.value = resourceId;');
    expect(source).toContain('void renderMap(false);');
    expect(source).not.toContain('void renderMap();\n  }');
  });

  it('fits the compact map only when renderMap explicitly asks for fitView', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');

    expect(source).toContain('async function renderMap(fitView = true) {');
    expect(source).toContain('if (amapMarkers.length && fitView) {');
    expect(source).toContain('amap.setFitView(amapMarkers, false, [28, 28, 28, 28]);');
  });

  it('re-fits the full-screen map only for resource-set changes, not selection or arranged-state changes', () => {
    const source = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain('async function renderMarkers(AMap: any, fitView = true) {');
    expect(source).toContain('if (fitView) {');
    expect(source).toContain('mapInstance.setFitView(markerInstances, false, [32, 32, 32, 32]);');
    expect(source).toContain('() => props.mapResources');
    expect(source).toContain('await renderMarkers(AMap, true);');
    expect(source).toContain('() => [props.selectedResourceId, props.arrangedResourceIds]');
    expect(source).toContain('await renderMarkers(AMap, false);');
  });
});

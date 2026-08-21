import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('product resource map workspace', () => {
  it('provides a full-screen resource map with the designer filters and explicit day action', () => {
    const source = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain('资源地图 · D{{ activeDayNo }}');
    expect(source).toContain('搜索资源名称');
    expect(source).toContain('resourceActionLabel');
    expect(source).toContain('activateResource');
    expect(source).toContain("emit('add-resource'");
    expect(source).toContain('resource-map-workspace');
  });

  it('uses clustered markers with a selected-resource fallback when the map plugin is unavailable', () => {
    const source = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain('MarkerCluster');
    expect(source).toContain('clusterData');
    expect(source).toContain('originMarker');
    expect(source).toContain('forbidenWebGL');
    expect(source).toContain('resource-map-label');
    expect(source).toContain('selectedResourceId');
    expect(source).toContain('fallbackMarkers');
    expect(source).toContain('map-cluster-fallback');
  });

  it('keeps the full-screen list dense without removing the explicit add action', () => {
    const source = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain('grid-template-columns: minmax(0, 1fr) 340px');
    expect(source).toContain('min-height: 56px');
    expect(source).toContain('activateResource');
  });

  it('keeps resource names above the balloon marker after clustering resets label positioning', () => {
    const source = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain(':global(.resource-map-fullscreen-modal .amap-marker-label)');
    expect(source).toContain('top: -22px !important');
    expect(source).toContain('transform: translateX(-50%)');
  });

  it('uses the same marker label tokens as the compact designer map', () => {
    const compact = readAppFile('src/views/sales/product/designer.vue');
    const fullscreen = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    for (const token of [
      'padding: 2px 5px',
      'color: #334155',
      'font-size: 11px',
      'line-height: 16px',
      'background: rgb(255 255 255 / 94%)',
      'border: 1px solid #d9d9d9',
      'border-radius: 3px',
    ]) {
      expect(compact).toContain(token);
      expect(fullscreen).toContain(token);
    }

    expect(compact).toContain('.designer-map-label');
    expect(fullscreen).toContain('.resource-map-label');
    expect(compact).toContain('display: inline-block');
    expect(fullscreen).toContain('display: inline-block');
    expect(compact).toContain('.map-container :deep(.amap-marker-label) { padding: 0;');
    expect(fullscreen).toContain('.resource-map-fullscreen-modal .amap-marker-label) { top: -22px !important;');
  });

  it('opens the workspace from the product designer without changing the backend contract', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');

    expect(source).toContain('ProductResourceMapWorkspace');
    expect(source).toContain('mapFullscreenOpen');
    expect(source).toContain('全屏地图');
    expect(source).toContain('resource-action-label');
    expect(source).toContain('add-resource');
    expect(source).not.toContain('getSalesProductDesignerResourceMap');
  });
});

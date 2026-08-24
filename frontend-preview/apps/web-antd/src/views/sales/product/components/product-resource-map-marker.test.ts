import { describe, expect, it } from 'vitest';

import {
  buildProductResourceMarkerHtml,
  productResourceMarkerZIndex,
} from './product-resource-map-marker';

describe('product resource map marker', () => {
  it('escapes a resource name and exposes the shared visual states', () => {
    const html = buildProductResourceMarkerHtml(
      { resourceName: '<杭州 & 上海>', resourceType: 'hotel' },
      { arranged: true, pending: true, selected: true },
    );

    expect(html).toContain('product-resource-map-marker');
    expect(html).toContain('is-selected');
    expect(html).toContain('is-arranged');
    expect(html).toContain('is-pending');
    expect(html).toContain('product-resource-map-marker__pin');
    expect(html).not.toContain('is-hotel');
    expect(html).toContain('&lt;杭州 &amp; 上海&gt;');
    expect(html).not.toContain('<杭州 & 上海>');
  });

  it('keeps selected markers above arranged and ordinary markers', () => {
    expect(productResourceMarkerZIndex()).toBe(10);
    expect(productResourceMarkerZIndex({ arranged: true })).toBe(40);
    expect(productResourceMarkerZIndex({ selected: true })).toBe(120);
  });
});

import { describe, expect, it } from 'vitest';

import { buildRegionPath, splitRegionPath } from './region';

describe('customer unit region helpers', () => {
  it('splits a selected province/city/district path into saved fields', () => {
    expect(splitRegionPath(['浙江省', '杭州市', '西湖区'])).toEqual({
      city: '杭州市',
      district: '西湖区',
      province: '浙江省',
    });
  });

  it('clears saved fields when the cascader value is empty', () => {
    expect(splitRegionPath([])).toEqual({
      city: undefined,
      district: undefined,
      province: undefined,
    });
  });

  it('allows saving only province or province and city', () => {
    expect(splitRegionPath(['浙江省'])).toEqual({
      city: undefined,
      district: undefined,
      province: '浙江省',
    });
    expect(splitRegionPath(['浙江省', '杭州市'])).toEqual({
      city: '杭州市',
      district: undefined,
      province: '浙江省',
    });
  });

  it('builds a cascader path from existing saved fields', () => {
    expect(buildRegionPath('浙江省', '杭州市', '西湖区')).toEqual([
      '浙江省',
      '杭州市',
      '西湖区',
    ]);
  });

  it('ignores blank saved fields when building a cascader path', () => {
    expect(buildRegionPath(' 浙江省 ', '', undefined)).toEqual(['浙江省']);
  });
});

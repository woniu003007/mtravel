import { describe, expect, it } from 'vitest';

import {
  normalizeDateTimeForPicker,
  parseStoredTrafficInfo,
} from './traffic-info';

describe('sales booking traffic info helpers', () => {
  it('normalizes AI recognized date-time values for DatePicker', () => {
    expect(normalizeDateTimeForPicker('2026-06-25T09:10:00', '2026-06-25')).toBe('2026/06/25 09:10');
    expect(normalizeDateTimeForPicker('2026/6/25 9:10', '2026-06-25')).toBe('2026/06/25 09:10');
    expect(normalizeDateTimeForPicker('09:10', '2026-06-25')).toBe('2026/06/25 09:10');
  });

  it('parses simple saved old-system traffic fields', () => {
    const parsed = parseStoredTrafficInfo(
      '大连 / 上海 / CZ6533 / 2026/06/25 09:10 / 2026/06/25 11:20',
      '2026-06-25',
    );

    expect(parsed).toEqual({
      arrivalPlace: '上海',
      arrivalTime: '2026/06/25 11:20',
      departurePlace: '大连',
      departureTime: '2026/06/25 09:10',
      trafficNo: 'CZ6533',
    });
  });

  it('tolerates duplicated route chunks produced by previous saves', () => {
    const parsed = parseStoredTrafficInfo(
      '大连 / 上海 / 大连 / 上海 / CZ6533 / 2026/06/25 09:10 / 2026/06/25 11:20 / 2026/06/25 09:10 / 2026/06/25 11:20',
      '2026-06-25',
    );

    expect(parsed.departurePlace).toBe('大连');
    expect(parsed.arrivalPlace).toBe('上海');
    expect(parsed.trafficNo).toBe('CZ6533');
    expect(parsed.departureTime).toBe('2026/06/25 09:10');
    expect(parsed.arrivalTime).toBe('2026/06/25 11:20');
  });

  it('extracts labelled station and time text from imported confirmation snippets', () => {
    const parsed = parseStoredTrafficInfo(
      '去程：大连 - 上海，航班 CZ6533，出发 2026-06-25 09:10，抵达 2026-06-25 11:20',
      '2026-06-25',
    );

    expect(parsed.departurePlace).toBe('大连');
    expect(parsed.arrivalPlace).toBe('上海');
    expect(parsed.trafficNo).toBe('CZ6533');
    expect(parsed.departureTime).toBe('2026/06/25 09:10');
    expect(parsed.arrivalTime).toBe('2026/06/25 11:20');
  });
});

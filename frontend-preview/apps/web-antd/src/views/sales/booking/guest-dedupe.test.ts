import { describe, expect, it } from 'vitest';

import {
  appendUniqueGuests,
  type GuestDedupeDraft,
  guestDedupeKey,
} from './guest-dedupe';

describe('sales booking guest dedupe helpers', () => {
  it('uses certificate number as the primary dedupe key', () => {
    expect(guestDedupeKey({ certificateNo: ' 210204198206214832 ' })).toBe('cert:210204198206214832');
  });

  it('falls back to guest name and phone when certificate number is empty', () => {
    expect(guestDedupeKey({ guestName: '张三', phone: '13900000000' })).toBe('name-phone:张三:13900000000');
  });

  it('skips duplicate guests inside the same AI or Excel import batch', () => {
    const result = appendUniqueGuests(
      [{ guestName: '', rowKey: 'placeholder' }] as GuestDedupeDraft[],
      [
        { certificateNo: '210204198206214832', guestName: '张百全' },
        { certificateNo: '210204198206214832', guestName: '张百全' },
      ] as GuestDedupeDraft[],
    );

    expect(result.appendedCount).toBe(1);
    expect(result.skippedDuplicateCount).toBe(1);
    expect(result.rows).toHaveLength(1);
    expect(result.rows[0]?.indexNo).toBe(1);
  });

  it('skips imported guests already present on the order', () => {
    const result = appendUniqueGuests(
      [{ certificateNo: '210204198206214832', guestName: '张百全', indexNo: 1 }] as GuestDedupeDraft[],
      [{ certificateNo: '210204198206214832', guestName: '张百全' }] as GuestDedupeDraft[],
    );

    expect(result.appendedCount).toBe(0);
    expect(result.skippedDuplicateCount).toBe(1);
    expect(result.rows).toHaveLength(1);
  });

  it('collapses duplicates already present on the editable guest table during import', () => {
    const result = appendUniqueGuests(
      [
        { certificateNo: '210204198206214832', guestName: '张百全', indexNo: 1 },
        { certificateNo: '210204198206214832', guestName: '张百全', indexNo: 2 },
      ] as GuestDedupeDraft[],
      [{ certificateNo: '21020420101028741X', guestName: '张磊' }] as GuestDedupeDraft[],
    );

    expect(result.rows).toHaveLength(2);
    expect(result.rows.map((row) => row.indexNo)).toEqual([1, 2]);
    expect(result.skippedDuplicateCount).toBe(1);
  });
});

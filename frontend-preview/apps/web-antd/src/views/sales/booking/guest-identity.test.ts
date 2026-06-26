import { describe, expect, it } from 'vitest';

import {
  deriveGuestTypeByTicketAge,
  deriveGuestTypeFromAi,
  validateChineseIdCard,
} from './guest-identity';

describe('sales booking guest identity helpers', () => {
  it('derives guest type by current ticket age rule', () => {
    expect(deriveGuestTypeByTicketAge(17)).toBe('child');
    expect(deriveGuestTypeByTicketAge(18)).toBe('adult');
    expect(deriveGuestTypeByTicketAge(59)).toBe('adult');
    expect(deriveGuestTypeByTicketAge(60)).toBe('senior');
  });

  it('keeps explicit AI no-bed and escort types before age fallback', () => {
    expect(deriveGuestTypeFromAi('儿童不占床', 8)).toBe('child_no_bed');
    expect(deriveGuestTypeFromAi('全陪', 34)).toBe('escort');
  });

  it('falls back to ticket age rule when AI customer type is empty', () => {
    expect(deriveGuestTypeFromAi(undefined, 12)).toBe('child');
    expect(deriveGuestTypeFromAi('', 66)).toBe('senior');
    expect(deriveGuestTypeFromAi('', 35)).toBe('adult');
  });

  it('validates Chinese ID card and derives gender, birth date and age', () => {
    const result = validateChineseIdCard('11010519491231002X', new Date('2026-06-24T00:00:00+08:00'));

    expect(result.valid).toBe(true);
    expect(result.gender).toBe('女');
    expect(result.birthDate).toBe('1949-12-31');
    expect(result.age).toBe(76);
  });

  it('marks invalid Chinese ID cards with warning text', () => {
    const result = validateChineseIdCard('110105194912310021', new Date('2026-06-24T00:00:00+08:00'));

    expect(result.valid).toBe(false);
    expect(result.warnings).toContain('身份证校验位不正确');
  });
});

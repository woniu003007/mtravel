export type BookingGuestType = 'adult' | 'child' | 'child_no_bed' | 'escort' | 'senior';

export interface IdCardValidation {
  age?: number;
  birthDate?: string;
  gender?: string;
  valid?: boolean;
  warnings: string[];
}

const ID_CARD_WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
const ID_CARD_CHECK_CODES = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'];

export function deriveGuestTypeByTicketAge(age?: number): BookingGuestType {
  if (age === undefined || age === null || Number.isNaN(age)) return 'adult';
  if (age < 18) return 'child';
  if (age >= 60) return 'senior';
  return 'adult';
}

export function deriveGuestTypeFromAi(value?: string, age?: number): BookingGuestType {
  if (value?.includes('不占')) return 'child_no_bed';
  if (value?.includes('全陪')) return 'escort';
  if (value?.includes('儿童') || value?.includes('小孩')) return 'child';
  if (value?.includes('老人')) return 'senior';
  return deriveGuestTypeByTicketAge(age);
}

export function validateChineseIdCard(rawIdCard?: string, currentDate: Date | number = new Date()): IdCardValidation {
  const warnings: string[] = [];
  const idCard = (rawIdCard || '').trim().toUpperCase();
  if (!idCard) {
    return { valid: undefined, warnings };
  }
  if (!/^\d{17}[0-9X]$/.test(idCard)) {
    return { valid: false, warnings: ['身份证格式不正确'] };
  }

  const birthDate = parseBirthDate(idCard, warnings);
  const gender = Number(idCard.charAt(16)) % 2 === 1 ? '男' : '女';
  const age = birthDate ? calculateAge(birthDate, currentDate) : undefined;
  if (!isChecksumValid(idCard)) {
    warnings.push('身份证校验位不正确');
  }
  return {
    age,
    birthDate,
    gender,
    valid: warnings.length === 0,
    warnings,
  };
}

function calculateAge(birthDate: string, currentDate: Date | number) {
  const now = typeof currentDate === 'number'
    ? new Date(Date.UTC(currentDate, 0, 1))
    : currentDate;
  const year = Number(birthDate.slice(0, 4));
  const month = Number(birthDate.slice(5, 7));
  const day = Number(birthDate.slice(8, 10));
  let age = now.getFullYear() - year;
  const birthdayPassed = now.getMonth() + 1 > month
    || (now.getMonth() + 1 === month && now.getDate() >= day);
  if (!birthdayPassed) age -= 1;
  return Math.max(0, age);
}

function parseBirthDate(idCard: string, warnings: string[]) {
  const year = Number(idCard.slice(6, 10));
  const month = Number(idCard.slice(10, 12));
  const day = Number(idCard.slice(12, 14));
  const date = new Date(Date.UTC(year, month - 1, day));
  const validDate = date.getUTCFullYear() === year
    && date.getUTCMonth() === month - 1
    && date.getUTCDate() === day;
  if (!validDate) {
    warnings.push('身份证出生日期不合法');
    return undefined;
  }
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

function isChecksumValid(idCard: string) {
  const sum = ID_CARD_WEIGHTS.reduce((total, weight, index) => {
    return total + Number(idCard.charAt(index)) * weight;
  }, 0);
  return ID_CARD_CHECK_CODES[sum % 11] === idCard.charAt(17);
}

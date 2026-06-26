export interface ParsedStoredTrafficInfo {
  arrivalPlace: string;
  arrivalTime: string;
  departurePlace: string;
  departureTime: string;
  trafficNo: string;
}

const DATETIME_PATTERN = /(\d{4})[./\-年](\d{1,2})[./\-月](\d{1,2})日?(?:\s|T|　)*(\d{1,2})[:：](\d{2})/g;
const TIME_PATTERN = /(\d{1,2})[:：](\d{2})/;
const TRAFFIC_NO_PATTERN = /\b([A-Z]{1,3}\d{2,5}[A-Z]?|[GDCZTK]\d{1,5})\b/i;

/**
 * 将 AI 识别或旧系统保存的日期时间统一为 DatePicker 使用的 YYYY/MM/DD HH:mm。
 */
export function normalizeDateTimeForPicker(value?: string, fallbackDate?: string) {
  const rawValue = value?.trim();
  if (!rawValue) return '';

  DATETIME_PATTERN.lastIndex = 0;
  const dateTimeMatch = DATETIME_PATTERN.exec(rawValue.replace('T', ' '));
  if (dateTimeMatch) {
    return formatDateTimeParts(dateTimeMatch);
  }

  const normalized = rawValue.replace('T', ' ').replaceAll('-', '/');
  const compactMatch = normalized.match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})\s+(\d{1,2}):(\d{2})/);
  if (compactMatch) {
    return formatDateTimeParts(compactMatch);
  }

  const timeMatch = normalized.match(TIME_PATTERN);
  const fallback = normalizeDatePart(fallbackDate);
  if (!timeMatch || !fallback) return rawValue;
  const [, hour = '', minute = ''] = timeMatch;
  return `${fallback} ${hour.padStart(2, '0')}:${minute}`;
}

/**
 * 解析已保存的接送交通字符串，兼容简单五段格式、旧数据重复拼接和带标签确认单片段。
 */
export function parseStoredTrafficInfo(value?: string, fallbackDate?: string): ParsedStoredTrafficInfo {
  const rawValue = value?.trim();
  if (!rawValue) return emptyTrafficInfo();

  const slashParts = rawValue
    .split(/\s*\/\s*/)
    .map(cleanTrafficToken)
    .filter(Boolean);
  if (slashParts.length >= 3) {
    return parseSlashTrafficInfo(slashParts, fallbackDate);
  }

  return parseLabelledTrafficInfo(rawValue, fallbackDate);
}

function parseSlashTrafficInfo(parts: string[], fallbackDate?: string): ParsedStoredTrafficInfo {
  const trafficNo = firstDefined(parts.map(extractTrafficNo));
  const times = parts
    .map((part) => normalizeDateTimeForPicker(part, fallbackDate))
    .filter((part) => isDateTime(part));
  const places = parts
    .filter((part) => !extractTrafficNo(part))
    .filter((part) => !isDateTime(normalizeDateTimeForPicker(part, fallbackDate)))
    .filter((part) => !isTrafficLabel(part));
  const selectedTimes = times.length > 2 ? times.slice(-2) : times;

  return {
    arrivalPlace: places[1] || '',
    arrivalTime: selectedTimes[1] || '',
    departurePlace: places[0] || '',
    departureTime: selectedTimes[0] || '',
    trafficNo: trafficNo || '',
  };
}

function parseLabelledTrafficInfo(value: string, fallbackDate?: string): ParsedStoredTrafficInfo {
  const trafficNo = extractTrafficNo(value);
  const times = extractDateTimes(value, fallbackDate);
  const places = extractRoutePlaces(value);

  return {
    arrivalPlace: places[1] || '',
    arrivalTime: times[1] || '',
    departurePlace: places[0] || '',
    departureTime: times[0] || '',
    trafficNo: trafficNo || '',
  };
}

function extractDateTimes(value: string, fallbackDate?: string) {
  const result: string[] = [];
  DATETIME_PATTERN.lastIndex = 0;
  for (const match of value.matchAll(DATETIME_PATTERN)) {
    result.push(formatDateTimeParts(match));
  }
  if (result.length >= 2) {
    return result.slice(0, 2);
  }

  const fallback = normalizeDatePart(fallbackDate);
  if (!fallback) return result;
  const labelledTimes = [...value.matchAll(/(?:出发|起飞|开车|抵达|到达|落地)[^\d]*(\d{1,2})[:：](\d{2})/g)]
    .map((match) => `${fallback} ${String(match[1]).padStart(2, '0')}:${match[2]}`);
  return [...result, ...labelledTimes].slice(0, 2);
}

function extractRoutePlaces(value: string) {
  const cleaned = value
    .replace(/^(去程|回程|来程|返程|接机|送机)[：:\s]*/u, '')
    .replace(/航班|车次|班次|交通|出发|抵达|到达|起飞|落地/gu, ' ');
  const routeMatch = cleaned.match(/([\u4E00-\u9FA5A-Za-z]{2,20})\s*[-－—到至]\s*([\u4E00-\u9FA5A-Za-z]{2,20})/u);
  if (routeMatch) {
    return [cleanTrafficToken(routeMatch[1]), cleanTrafficToken(routeMatch[2])].filter(Boolean);
  }
  return cleaned
    .split(/[，,；;\s]+/)
    .map(cleanTrafficToken)
    .filter((part) => part.length >= 2)
    .filter((part) => !extractTrafficNo(part))
    .filter((part) => !isDateTime(normalizeDateTimeForPicker(part)))
    .slice(0, 2);
}

function extractTrafficNo(value?: string) {
  const match = value?.toUpperCase().match(TRAFFIC_NO_PATTERN);
  return match?.[1] || '';
}

function cleanTrafficToken(value?: string) {
  return (value || '')
    .trim()
    .replace(/^[：:,，\s]+|[：:,，\s]+$/g, '')
    .replace(/^(去程|回程|来程|返程|航班|车次|班次|交通|出发|抵达|到达)[：:\s]*/u, '')
    .trim();
}

function normalizeDatePart(value?: string) {
  const rawValue = value?.trim();
  if (!rawValue) return '';
  const match = rawValue.match(/(\d{4})[./\-年](\d{1,2})[./\-月](\d{1,2})/);
  if (!match) return '';
  const [, year = '', month = '', day = ''] = match;
  return `${year}/${month.padStart(2, '0')}/${day.padStart(2, '0')}`;
}

function formatDateTimeParts(match: RegExpMatchArray) {
  const [, year = '', month = '', day = '', hour = '', minute = ''] = match;
  return `${year}/${month.padStart(2, '0')}/${day.padStart(2, '0')} ${hour.padStart(2, '0')}:${minute}`;
}

function isDateTime(value?: string) {
  return /^\d{4}\/\d{2}\/\d{2}\s+\d{2}:\d{2}$/.test(value || '');
}

function isTrafficLabel(value: string) {
  return ['行程', '去程', '回程', '来程', '返程'].includes(value);
}

function firstDefined(values: string[]) {
  return values.find(Boolean);
}

function emptyTrafficInfo(): ParsedStoredTrafficInfo {
  return {
    arrivalPlace: '',
    arrivalTime: '',
    departurePlace: '',
    departureTime: '',
    trafficNo: '',
  };
}

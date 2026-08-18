import type { TeamDocumentImportApi } from '#/api/sales/team-document-import';

/** Word 代录资源在预览抽屉中的按日展示分组。 */
export interface DocumentImportResourceDayGroup {
  dayNo?: number;
  key: string;
  label: string;
  totalCount: number;
  typeGroups: Array<{
    arrangementType: TeamDocumentImportApi.ArrangementType;
    resources: TeamDocumentImportApi.ResourceDraft[];
  }>;
}

/**
 * 将资源原文时间换算为当天分钟数，供同一资源类型内排序。
 *
 * AI 正常返回 HH:mm；本地 Word 兜底可能保留“下午 2:00”，这里按 24 小时制处理。
 */
export function documentImportResourceTimeValue(resource: TeamDocumentImportApi.ResourceDraft) {
  const source = [resource.time, resource.remark, resource.sourceName].filter(Boolean).join(' ');
  const matched = /(?:^|[^\d])(?:(上午|下午|中午|晚上|早上)\s*)?([01]?\d|2[0-3])\s*(?:[:：]\s*([0-5]\d)|点\s*([0-5]\d)?\s*分?)(?!\d)/.exec(source);
  if (!matched) return undefined;
  const period = matched[1];
  let hour = Number(matched[2]);
  if ((period === '下午' || period === '晚上') && hour < 12) hour += 12;
  if (period === '中午' && hour < 11) hour += 12;
  return hour * 60 + Number(matched[3] || matched[4] || 0);
}

/**
 * 先按行程日归组，再让同一类型资源连续展示；类型组内按时刻升序。
 * 无明确时刻时保持 Word/AI 原始出现顺序，避免因为猜测而改变行程。
 */
export function buildDocumentImportResourceGroups(
  resources: TeamDocumentImportApi.ResourceDraft[] | undefined,
): DocumentImportResourceDayGroup[] {
  const groups = new Map<number | undefined, DocumentImportResourceDayGroup>();
  for (const resource of resources || []) {
    const normalizedDayNo = Number(resource.dayNo);
    const dayNo = Number.isFinite(normalizedDayNo) && normalizedDayNo > 0
      ? normalizedDayNo
      : undefined;
    let dayGroup = groups.get(dayNo);
    if (!dayGroup) {
      dayGroup = {
        dayNo,
        key: dayNo === undefined ? 'unknown-day' : `day-${dayNo}`,
        label: dayNo === undefined ? '未确定日期' : `第${dayNo}天`,
        totalCount: 0,
        typeGroups: [],
      };
      groups.set(dayNo, dayGroup);
    }

    // 首次出现的类型决定该天类型组顺序，保持确认单的业务阅读顺序。
    let typeGroup = dayGroup.typeGroups.find((item) => item.arrangementType === resource.arrangementType);
    if (!typeGroup) {
      typeGroup = { arrangementType: resource.arrangementType, resources: [] };
      dayGroup.typeGroups.push(typeGroup);
    }
    typeGroup.resources.push(resource);
    dayGroup.totalCount += 1;
  }

  for (const dayGroup of groups.values()) {
    for (const typeGroup of dayGroup.typeGroups) {
      typeGroup.resources = typeGroup.resources
        .map((resource, index) => ({ index, resource, time: documentImportResourceTimeValue(resource) }))
        .sort((left, right) => {
          if (left.time === undefined && right.time === undefined) return left.index - right.index;
          if (left.time === undefined) return 1;
          if (right.time === undefined) return -1;
          return left.time - right.time || left.index - right.index;
        })
        .map((item) => item.resource);
    }
  }

  return [...groups.values()].sort((left, right) => {
    if (left.dayNo === undefined) return 1;
    if (right.dayNo === undefined) return -1;
    return left.dayNo - right.dayNo;
  });
}

/**
 * 产品 Word 方案图片选择的纯规则。
 *
 * 当天末尾模式的图片跨景区统一排序，不能再退化成按资源分组的图片数组。
 */
export type WordPlanImageMode = 'follow_resource' | 'day_end' | 'hidden';

export interface DayEndImageSelection {
  dayResourceId: number;
  imageId: number;
}

export interface WordPlanImageSaveInput {
  dayEndImageSelections?: DayEndImageSelection[];
  imageMode?: WordPlanImageMode;
  selectedImageIdsByResource?: Record<number, number[]>;
}

export interface WordPlanImageSavePayload {
  dayEndImageSelections?: DayEndImageSelection[];
  imageMode?: WordPlanImageMode;
  selectedImageIdsByResource?: Record<number, number[]>;
}

export interface WordPlanImageValidationResult {
  code?: 'day_end_max_3' | 'day_end_min_2';
  valid: boolean;
}

function uniqueDayEndSelections(selections: DayEndImageSelection[] = []) {
  const seen = new Set<string>();
  return selections.flatMap((selection) => {
    const key = `${selection.dayResourceId}:${selection.imageId}`;
    if (seen.has(key)) return [];
    seen.add(key);
    return [{ dayResourceId: selection.dayResourceId, imageId: selection.imageId }];
  });
}

/** 根据当前图片模式构造保存负载，避免两种模式的语义混写。 */
export function buildWordPlanImageSavePayload(input: WordPlanImageSaveInput): WordPlanImageSavePayload {
  const imageMode = input.imageMode || 'follow_resource';
  if (imageMode === 'day_end') {
    return {
      dayEndImageSelections: uniqueDayEndSelections(input.dayEndImageSelections),
      imageMode,
    };
  }
  if (imageMode === 'follow_resource') {
    return {
      imageMode,
      selectedImageIdsByResource: input.selectedImageIdsByResource || {},
    };
  }
  return { imageMode };
}

/** 当天末尾图片允许不展示，展示时必须是 2 或 3 张。 */
export function validateWordPlanImageSelections(input: WordPlanImageSaveInput): WordPlanImageValidationResult {
  if (input.imageMode !== 'day_end') return { valid: true };
  const count = uniqueDayEndSelections(input.dayEndImageSelections).length;
  if (count === 1) return { code: 'day_end_min_2', valid: false };
  if (count > 3) return { code: 'day_end_max_3', valid: false };
  return { valid: true };
}

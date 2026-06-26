export interface GuestDedupeDraft {
  certificateNo?: string;
  guestName?: string;
  indexNo?: number;
  phone?: string;
  rowKey?: string;
}

export interface AppendUniqueGuestsResult<T extends GuestDedupeDraft> {
  appendedCount: number;
  rows: T[];
  skippedDuplicateCount: number;
}

/**
 * 游客去重主键：优先证件号；没有证件号时使用姓名 + 电话。
 */
export function guestDedupeKey(guest: GuestDedupeDraft) {
  const certificateNo = guest.certificateNo?.trim().toUpperCase();
  if (certificateNo) return `cert:${certificateNo}`;
  const name = guest.guestName?.trim();
  const phone = guest.phone?.trim();
  if (name && phone) return `name-phone:${name}:${phone}`;
  return '';
}

/**
 * 将 AI/Excel 导入游客追加到当前订单，并跳过已有、本批次或表格内重复游客。
 */
export function appendUniqueGuests<T extends GuestDedupeDraft>(
  existingGuests: T[],
  incomingGuests: T[],
): AppendUniqueGuestsResult<T> {
  const dedupedExistingGuests: T[] = [];
  const existingKeys = new Set<string>();
  const appendRows: T[] = [];
  let skippedDuplicateCount = 0;

  for (const existingGuest of existingGuests) {
    if (!hasGuestIdentity(existingGuest)) continue;

    const key = guestDedupeKey(existingGuest);
    if (key && existingKeys.has(key)) {
      skippedDuplicateCount += 1;
      continue;
    }

    if (key) existingKeys.add(key);
    dedupedExistingGuests.push(existingGuest);
  }

  for (const incomingGuest of incomingGuests) {
    if (!hasGuestIdentity(incomingGuest)) continue;
    const key = guestDedupeKey(incomingGuest);
    if (key && existingKeys.has(key)) {
      skippedDuplicateCount += 1;
      continue;
    }
    if (key) existingKeys.add(key);
    appendRows.push(incomingGuest);
  }

  const rows = [...dedupedExistingGuests, ...appendRows]
    .map((guest, index) => ({ ...guest, indexNo: index + 1 }) as T);
  return {
    appendedCount: appendRows.length,
    rows,
    skippedDuplicateCount,
  };
}

function hasGuestIdentity(guest: GuestDedupeDraft) {
  return Boolean(guest.guestName?.trim() || guest.certificateNo?.trim());
}

import { requestClient } from '#/api/request';

export namespace BookingAiImportApi {
  export interface RecognizeParams {
    attachmentId?: number;
    attachmentIds?: number[];
    sourceType?: string;
    text?: string;
  }

  export interface TravelInfo {
    joinDate?: string;
    outboundArrivalCity?: string;
    outboundArrivalTime?: string;
    outboundDepartureTime?: string;
    outboundOriginCity?: string;
    outboundStationName?: string;
    outboundTrafficNo?: string;
    returnArrivalTime?: string;
    returnDepartureCity?: string;
    returnDepartureTime?: string;
    returnDestinationCity?: string;
    returnStationName?: string;
    returnTrafficNo?: string;
    warnings?: string[];
  }

  export interface GuideInfo {
    escortName?: string;
    guideName?: string;
    guidePhone?: string;
    receptionRequirement?: string;
    warnings?: string[];
  }

  export interface CustomerInfo {
    contactName?: string;
    contactPhone?: string;
    customerName?: string;
    remark?: string;
    sourcePlace?: string;
    warnings?: string[];
  }

  export interface PriceInfo {
    adultPrice?: string;
    childPrice?: string;
    priceLines?: string[];
    seniorPrice?: string;
    singleRoomDifference?: string;
    totalAmount?: string;
    warnings?: string[];
  }

  export interface AdditionalInfo {
    leaderNote?: string;
    notes?: string;
    receptionStandard?: string;
    roomingNote?: string;
    warnings?: string[];
  }

  export interface GuestInfo {
    age?: number;
    birthDate?: string;
    birthplace?: string;
    certificateNo?: string;
    customerType?: string;
    englishName?: string;
    expiryDate?: string;
    gender?: string;
    groupRemark?: string;
    idCardValid?: boolean;
    indexNo?: number;
    issueDate?: string;
    issuePlace?: string;
    leader?: boolean;
    leaderSourceText?: string;
    name?: string;
    personalRemark?: string;
    phone?: string;
    roomGroup?: string;
    roomingRemark?: string;
    suspectedLeader?: boolean;
    warnings?: string[];
  }

  export interface ModuleScores {
    additionalScore?: number;
    customerScore?: number;
    guestListScore?: number;
    guideScore?: number;
    priceScore?: number;
    travelScore?: number;
  }

  export interface GuestSummary {
    guestCount?: number;
    invalidIdCardCount?: number;
    missingRequiredCount?: number;
    suspectedMissingCount?: number;
  }

  export interface RecognizeResult {
    additionalInfo: AdditionalInfo;
    confidence: number;
    customerInfo: CustomerInfo;
    evidence: string[];
    guests: GuestInfo[];
    guestSummary?: GuestSummary;
    guideInfo: GuideInfo;
    moduleScores?: ModuleScores;
    priceInfo: PriceInfo;
    sourceType: string;
    travelInfo: TravelInfo;
    warnings: string[];
  }
}

/** 识别确认单并返回 AI 辅助录入草稿。 */
export function recognizeBookingAiImport(data: BookingAiImportApi.RecognizeParams) {
  return requestClient.post<BookingAiImportApi.RecognizeResult>(
    '/sales/booking/ai-import/recognize',
    data,
  );
}

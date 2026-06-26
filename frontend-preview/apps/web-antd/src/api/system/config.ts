import { requestClient } from '#/api/request';

export namespace SystemConfigApi {
  export interface AuthConfig {
    idleTimeoutMinutes: number;
  }

  export interface BusinessRiskConfig {
    customerRiskApprovalEnabled: boolean;
  }

  export interface AiConfig {
    apiKeyMasked?: string;
    provider: 'aliyun_bailian';
    textModel?: string;
    visionModel?: string;
  }

  export interface AiConfigUpdateParams {
    apiKey?: string;
    provider: 'aliyun_bailian';
    textModel?: string;
    visionModel?: string;
  }

  export interface MapConfig {
    jsKeyMasked?: string;
    jsSecurityCodeMasked?: string;
    webServiceKeyMasked?: string;
  }

  export interface MapConfigUpdateParams {
    jsKey?: string;
    jsSecurityCode?: string;
    webServiceKey?: string;
  }
}

export function getAuthConfig() {
  return requestClient.get<SystemConfigApi.AuthConfig>('/system/config/auth');
}

export function updateAuthConfig(data: SystemConfigApi.AuthConfig) {
  return requestClient.post<SystemConfigApi.AuthConfig>(
    '/system/config/auth/update',
    data,
  );
}

export function getBusinessRiskConfig() {
  return requestClient.get<SystemConfigApi.BusinessRiskConfig>(
    '/system/config/business-risk',
  );
}

export function updateBusinessRiskConfig(
  data: SystemConfigApi.BusinessRiskConfig,
) {
  return requestClient.post<SystemConfigApi.BusinessRiskConfig>(
    '/system/config/business-risk/update',
    data,
  );
}

export function getAiConfig() {
  return requestClient.get<SystemConfigApi.AiConfig>('/system/config/ai');
}

export function updateAiConfig(data: SystemConfigApi.AiConfigUpdateParams) {
  return requestClient.post<SystemConfigApi.AiConfig>(
    '/system/config/ai/update',
    data,
  );
}

export function getMapConfig() {
  return requestClient.get<SystemConfigApi.MapConfig>('/system/config/map');
}

export function updateMapConfig(data: SystemConfigApi.MapConfigUpdateParams) {
  return requestClient.post<SystemConfigApi.MapConfig>(
    '/system/config/map/update',
    data,
  );
}

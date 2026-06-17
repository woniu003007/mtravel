import { requestClient } from '#/api/request';

export namespace ControlledRoomStatusApi {
  export type ResourceStatus = 'active' | 'disabled' | 'expired';
  export type RoomStatus = 'active' | 'disabled' | 'maintenance';
  export type InventoryStatus = 'active' | 'stopped';
  export type LockStatus = 'locked' | 'occupied' | 'released';
  export type SourceType = 'purchased_resource' | 'self_owned';

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface Resource {
    id: number;
    hotelName: string;
    province?: string;
    city?: string;
    district?: string;
    area?: string;
    address?: string;
    starStandard?: string;
    roomType?: string;
    sourceName?: string;
    purchasePrice?: number;
    agreementPrice?: number;
    priceUnit?: string;
    validFrom?: string;
    validTo?: string;
    contactName?: string;
    contactPhone?: string;
    status: ResourceStatus;
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface ResourceQuery {
    city?: string;
    district?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
    province?: string;
    roomType?: string;
    starStandard?: string;
    status?: ResourceStatus;
  }

  export interface ResourceSaveParams {
    hotelName: string;
    province?: string;
    city?: string;
    district?: string;
    area?: string;
    address?: string;
    starStandard?: string;
    roomType?: string;
    sourceName?: string;
    purchasePrice?: number;
    agreementPrice?: number;
    priceUnit?: string;
    validFrom?: string;
    validTo?: string;
    contactName?: string;
    contactPhone?: string;
    status?: ResourceStatus;
    remark?: string;
  }

  export interface RoomType {
    id: number;
    resourceId: number;
    hotelName?: string;
    roomType: string;
    bedType?: string;
    capacity: number;
    purchasePrice: number;
    agreementPrice: number;
    priceUnit?: string;
    status: 'active' | 'disabled';
    remark?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface RoomTypeQuery {
    keyword?: string;
    page?: number;
    pageSize?: number;
    resourceId?: number;
    status?: 'active' | 'disabled';
  }

  export interface RoomTypeSaveParams {
    agreementPrice?: number;
    bedType?: string;
    capacity?: number;
    priceUnit?: string;
    purchasePrice?: number;
    remark?: string;
    resourceId: number;
    roomType: string;
    status?: 'active' | 'disabled';
  }

  export interface Room {
    id: number;
    resourceId: number;
    hotelName?: string;
    starStandard?: string;
    buildingName?: string;
    floorNo?: string;
    roomNo: string;
    roomType?: string;
    bedType?: string;
    capacity: number;
    status: RoomStatus;
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface RoomQuery {
    keyword?: string;
    page?: number;
    pageSize?: number;
    resourceId?: number;
    status?: RoomStatus;
  }

  export interface RoomSaveParams {
    bedType?: string;
    buildingName?: string;
    capacity?: number;
    floorNo?: string;
    remark?: string;
    resourceId: number;
    roomNo: string;
    roomType?: string;
    status?: RoomStatus;
  }

  export interface Inventory {
    id: number;
    sourceType: SourceType;
    sourceId: number;
    roomTypeId?: number;
    hotelName: string;
    supplierName?: string;
    roomType: string;
    stayDate: string;
    totalQuantity: number;
    lockedQuantity: number;
    occupiedQuantity: number;
    remainingQuantity: number;
    status: InventoryStatus;
  }

  export interface InventoryQuery {
    endDate: string;
    roomTypeId?: number;
    sourceId?: number;
    sourceType?: SourceType;
    startDate: string;
    status?: InventoryStatus;
  }

  export interface InventoryGenerateParams {
    endDate: string;
    roomType: string;
    roomTypeId?: number;
    sourceId: number;
    sourceType: SourceType;
    startDate: string;
    status?: InventoryStatus;
    totalQuantity: number;
  }

  export interface InventoryLockParams {
    checkInDate: string;
    checkOutDate: string;
    quantity: number;
    remark?: string;
    requiredStandard?: string;
    roomType: string;
    roomTypeId?: number;
    sourceId: number;
    sourceType: SourceType;
    teamName?: string;
    teamNo?: string;
  }

  export interface InventoryOccupancy {
    lockRecordId: number;
    sourceType?: SourceType;
    sourceId?: number;
    roomType?: string;
    stayDate: string;
    quantity: number;
    teamNo?: string;
    teamName?: string;
    status: LockStatus;
    createdAt?: string;
  }

  export interface LockRecord {
    id: number;
    resourceId?: number;
    roomId?: number;
    hotelName?: string;
    roomNo?: string;
    roomType?: string;
    starStandard?: string;
    checkInDate: string;
    checkOutDate: string;
    teamNo?: string;
    teamName?: string;
    requiredStandard?: string;
    status: LockStatus;
    releasedAt?: string;
    releasedBy?: string;
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface LockQuery {
    page?: number;
    pageSize?: number;
    resourceId?: number;
    status?: LockStatus;
    teamNo?: string;
  }
}

export function getControlledRoomResourcePage(params: ControlledRoomStatusApi.ResourceQuery) {
  return requestClient.get<ControlledRoomStatusApi.PageResult<ControlledRoomStatusApi.Resource>>(
    '/dispatch/room-status/resources/page',
    { params },
  );
}

export function getControlledRoomResourceAll(includeDisabled = false) {
  return requestClient.get<ControlledRoomStatusApi.Resource[]>(
    '/dispatch/room-status/resources/all',
    { params: { includeDisabled } },
  );
}

export function createControlledRoomResource(data: ControlledRoomStatusApi.ResourceSaveParams) {
  return requestClient.post<ControlledRoomStatusApi.Resource>(
    '/dispatch/room-status/resources/create',
    data,
  );
}

export function updateControlledRoomResource(id: number, data: ControlledRoomStatusApi.ResourceSaveParams) {
  return requestClient.post<ControlledRoomStatusApi.Resource>(
    '/dispatch/room-status/resources/update',
    data,
    { params: { id } },
  );
}

export function deleteControlledRoomResource(id: number) {
  return requestClient.post<void>(
    '/dispatch/room-status/resources/delete',
    {},
    { params: { id } },
  );
}

export function getControlledRoomTypePage(params: ControlledRoomStatusApi.RoomTypeQuery) {
  return requestClient.get<ControlledRoomStatusApi.PageResult<ControlledRoomStatusApi.RoomType>>(
    '/dispatch/room-status/room-types/page',
    { params },
  );
}

export function getControlledRoomTypeAll(resourceId?: number, includeDisabled = false) {
  return requestClient.get<ControlledRoomStatusApi.RoomType[]>(
    '/dispatch/room-status/room-types/all',
    { params: { includeDisabled, resourceId } },
  );
}

export function createControlledRoomType(data: ControlledRoomStatusApi.RoomTypeSaveParams) {
  return requestClient.post<ControlledRoomStatusApi.RoomType>(
    '/dispatch/room-status/room-types/create',
    data,
  );
}

export function updateControlledRoomType(id: number, data: ControlledRoomStatusApi.RoomTypeSaveParams) {
  return requestClient.post<ControlledRoomStatusApi.RoomType>(
    '/dispatch/room-status/room-types/update',
    data,
    { params: { id } },
  );
}

export function deleteControlledRoomType(id: number) {
  return requestClient.post<void>(
    '/dispatch/room-status/room-types/delete',
    {},
    { params: { id } },
  );
}

export function getControlledRoomPage(params: ControlledRoomStatusApi.RoomQuery) {
  return requestClient.get<ControlledRoomStatusApi.PageResult<ControlledRoomStatusApi.Room>>(
    '/dispatch/room-status/rooms/page',
    { params },
  );
}

export function createControlledRoom(data: ControlledRoomStatusApi.RoomSaveParams) {
  return requestClient.post<ControlledRoomStatusApi.Room>(
    '/dispatch/room-status/rooms/create',
    data,
  );
}

export function updateControlledRoom(id: number, data: ControlledRoomStatusApi.RoomSaveParams) {
  return requestClient.post<ControlledRoomStatusApi.Room>(
    '/dispatch/room-status/rooms/update',
    data,
    { params: { id } },
  );
}

export function deleteControlledRoom(id: number) {
  return requestClient.post<void>(
    '/dispatch/room-status/rooms/delete',
    {},
    { params: { id } },
  );
}

export function generateRoomInventory(data: ControlledRoomStatusApi.InventoryGenerateParams) {
  return requestClient.post<number>('/dispatch/room-status/inventories/generate', data);
}

export function getRoomInventoryCalendar(params: ControlledRoomStatusApi.InventoryQuery) {
  return requestClient.get<ControlledRoomStatusApi.Inventory[]>(
    '/dispatch/room-status/inventories/calendar',
    { params },
  );
}

export function createRoomInventoryLock(data: ControlledRoomStatusApi.InventoryLockParams) {
  return requestClient.post<ControlledRoomStatusApi.LockRecord>(
    '/dispatch/room-status/inventories/locks/create',
    data,
  );
}

export function getRoomInventoryOccupancy(params: {
  roomType?: string;
  roomTypeId?: number;
  sourceId?: number;
  sourceType?: ControlledRoomStatusApi.SourceType;
  stayDate: string;
}) {
  return requestClient.get<ControlledRoomStatusApi.InventoryOccupancy[]>(
    '/dispatch/room-status/inventories/occupancy',
    { params },
  );
}

export function releaseControlledRoomLock(id: number) {
  return requestClient.post<void>(
    '/dispatch/room-status/locks/release',
    {},
    { params: { id } },
  );
}

export function getControlledRoomLockPage(params: ControlledRoomStatusApi.LockQuery) {
  return requestClient.get<ControlledRoomStatusApi.PageResult<ControlledRoomStatusApi.LockRecord>>(
    '/dispatch/room-status/locks/page',
    { params },
  );
}

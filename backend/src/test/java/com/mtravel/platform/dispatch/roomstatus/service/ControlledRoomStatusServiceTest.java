package com.mtravel.platform.dispatch.roomstatus.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomLockRequest;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomLockRecordResponse;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomResourceSaveRequest;
import com.mtravel.platform.dispatch.roomstatus.dto.RoomInventoryGenerateRequest;
import com.mtravel.platform.dispatch.roomstatus.dto.RoomInventoryLockRequest;
import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomDayStatusEntity;
import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomLockRecordEntity;
import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomResourceEntity;
import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomTypeEntity;
import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomUnitEntity;
import com.mtravel.platform.dispatch.roomstatus.entity.RoomInventoryEntity;
import com.mtravel.platform.dispatch.roomstatus.mapper.ControlledRoomDayStatusMapper;
import com.mtravel.platform.dispatch.roomstatus.mapper.ControlledRoomLockRecordMapper;
import com.mtravel.platform.dispatch.roomstatus.mapper.ControlledRoomResourceMapper;
import com.mtravel.platform.dispatch.roomstatus.mapper.ControlledRoomTypeMapper;
import com.mtravel.platform.dispatch.roomstatus.mapper.ControlledRoomUnitMapper;
import com.mtravel.platform.dispatch.roomstatus.mapper.RoomInventoryMapper;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControlledRoomStatusServiceTest {

    @Test
    void createResourceShouldSaveRegionAndPlainSourceName() {
        ControlledRoomResourceMapper resourceMapper = mock(ControlledRoomResourceMapper.class);
        ControlledRoomUnitMapper roomMapper = mock(ControlledRoomUnitMapper.class);
        ControlledRoomDayStatusMapper dayStatusMapper = mock(ControlledRoomDayStatusMapper.class);
        ControlledRoomLockRecordMapper lockRecordMapper = mock(ControlledRoomLockRecordMapper.class);
        ControlledRoomTypeMapper roomTypeMapper = mock(ControlledRoomTypeMapper.class);
        RoomInventoryMapper inventoryMapper = mock(RoomInventoryMapper.class);
        PurchaseRelationMapper purchaseRelationMapper = mock(PurchaseRelationMapper.class);
        ControlledRoomStatusService service = new ControlledRoomStatusService(
                resourceMapper, roomMapper, dayStatusMapper, lockRecordMapper, roomTypeMapper, inventoryMapper, purchaseRelationMapper);
        AtomicReference<ControlledRoomResourceEntity> inserted = new AtomicReference<>();

        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer((Answer<Integer>) invocation -> {
            ControlledRoomResourceEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            inserted.set(entity);
            return 1;
        }).when(resourceMapper).insert(any(ControlledRoomResourceEntity.class));
        when(resourceMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> inserted.get());

        ControlledRoomResourceSaveRequest request = new ControlledRoomResourceSaveRequest(
                "苏州中心自控房",
                "江苏省",
                "苏州市",
                "工业园区",
                null,
                "苏州工业园区星港街",
                "四钻",
                "标准间",
                "苏州中心酒店",
                BigDecimal.valueOf(420),
                BigDecimal.valueOf(520),
                "间夜",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "周经理",
                "0512-66660001",
                "active",
                "年度包房资源"
        );

        service.createResource(request, 1L, "admin");

        ControlledRoomResourceEntity entity = inserted.get();
        assertThat(entity.getProvince()).isEqualTo("江苏省");
        assertThat(entity.getCity()).isEqualTo("苏州市");
        assertThat(entity.getDistrict()).isEqualTo("工业园区");
        assertThat(entity.getSourceName()).isEqualTo("苏州中心酒店");
    }

    @Test
    void lockShouldRejectDateOutsideResourceValidPeriod() {
        ControlledRoomResourceMapper resourceMapper = mock(ControlledRoomResourceMapper.class);
        ControlledRoomUnitMapper roomMapper = mock(ControlledRoomUnitMapper.class);
        ControlledRoomDayStatusMapper dayStatusMapper = mock(ControlledRoomDayStatusMapper.class);
        ControlledRoomLockRecordMapper lockRecordMapper = mock(ControlledRoomLockRecordMapper.class);
        ControlledRoomTypeMapper roomTypeMapper = mock(ControlledRoomTypeMapper.class);
        RoomInventoryMapper inventoryMapper = mock(RoomInventoryMapper.class);
        PurchaseRelationMapper purchaseRelationMapper = mock(PurchaseRelationMapper.class);
        ControlledRoomStatusService service = new ControlledRoomStatusService(
                resourceMapper, roomMapper, dayStatusMapper, lockRecordMapper, roomTypeMapper, inventoryMapper, purchaseRelationMapper);

        ControlledRoomResourceEntity resource = resource();
        resource.setValidFrom(LocalDate.of(2026, 6, 1));
        resource.setValidTo(LocalDate.of(2026, 6, 30));
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource);
        when(roomMapper.selectOne(any(Wrapper.class))).thenReturn(room());

        ControlledRoomLockRequest request = new ControlledRoomLockRequest(
                10L,
                List.of(20L),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                "T20260701",
                "南京二日游",
                "四钻",
                "暑期团队预锁房"
        );

        assertThatThrownBy(() -> service.lockRooms(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不在房源价格有效期内");

        verify(lockRecordMapper, never()).insert(any(ControlledRoomLockRecordEntity.class));
        verify(dayStatusMapper, never()).update(any(ControlledRoomDayStatusEntity.class), any());
    }

    @Test
    void lockShouldRejectOccupiedDayStatus() {
        ControlledRoomResourceMapper resourceMapper = mock(ControlledRoomResourceMapper.class);
        ControlledRoomUnitMapper roomMapper = mock(ControlledRoomUnitMapper.class);
        ControlledRoomDayStatusMapper dayStatusMapper = mock(ControlledRoomDayStatusMapper.class);
        ControlledRoomLockRecordMapper lockRecordMapper = mock(ControlledRoomLockRecordMapper.class);
        ControlledRoomTypeMapper roomTypeMapper = mock(ControlledRoomTypeMapper.class);
        RoomInventoryMapper inventoryMapper = mock(RoomInventoryMapper.class);
        PurchaseRelationMapper purchaseRelationMapper = mock(PurchaseRelationMapper.class);
        ControlledRoomStatusService service = new ControlledRoomStatusService(
                resourceMapper, roomMapper, dayStatusMapper, lockRecordMapper, roomTypeMapper, inventoryMapper, purchaseRelationMapper);

        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource());
        when(roomMapper.selectOne(any(Wrapper.class))).thenReturn(room());
        ControlledRoomDayStatusEntity occupied = dayStatus(LocalDate.of(2026, 6, 11), "occupied");
        when(dayStatusMapper.selectOne(any(Wrapper.class))).thenReturn(occupied);

        ControlledRoomLockRequest request = lockRequest();

        assertThatThrownBy(() -> service.lockRooms(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不可锁定");

        verify(lockRecordMapper, never()).insert(any(ControlledRoomLockRecordEntity.class));
        verify(dayStatusMapper, never()).update(any(ControlledRoomDayStatusEntity.class), any());
    }

    @Test
    void lockShouldCreateRecordAndMarkDayStatusLocked() {
        ControlledRoomResourceMapper resourceMapper = mock(ControlledRoomResourceMapper.class);
        ControlledRoomUnitMapper roomMapper = mock(ControlledRoomUnitMapper.class);
        ControlledRoomDayStatusMapper dayStatusMapper = mock(ControlledRoomDayStatusMapper.class);
        ControlledRoomLockRecordMapper lockRecordMapper = mock(ControlledRoomLockRecordMapper.class);
        ControlledRoomTypeMapper roomTypeMapper = mock(ControlledRoomTypeMapper.class);
        RoomInventoryMapper inventoryMapper = mock(RoomInventoryMapper.class);
        PurchaseRelationMapper purchaseRelationMapper = mock(PurchaseRelationMapper.class);
        ControlledRoomStatusService service = new ControlledRoomStatusService(
                resourceMapper, roomMapper, dayStatusMapper, lockRecordMapper, roomTypeMapper, inventoryMapper, purchaseRelationMapper);
        ArgumentCaptor<ControlledRoomDayStatusEntity> dayStatusCaptor =
                ArgumentCaptor.forClass(ControlledRoomDayStatusEntity.class);

        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource());
        when(roomMapper.selectOne(any(Wrapper.class))).thenReturn(room());
        when(dayStatusMapper.selectOne(any(Wrapper.class))).thenReturn(dayStatus(LocalDate.of(2026, 6, 11), "available"));
        doAnswer((Answer<Integer>) invocation -> {
            Object entity = invocation.getArgument(0);
            entity.getClass().getMethod("setId", Long.class).invoke(entity, 99L);
            return 1;
        }).when(lockRecordMapper).insert(any(ControlledRoomLockRecordEntity.class));
        when(dayStatusMapper.update(any(ControlledRoomDayStatusEntity.class), any(Wrapper.class))).thenReturn(1);

        service.lockRooms(lockRequest(), 1L, "admin");

        verify(dayStatusMapper).update(dayStatusCaptor.capture(), any(Wrapper.class));
        ControlledRoomDayStatusEntity updated = dayStatusCaptor.getValue();
        assertThat(updated.getStatus()).isEqualTo("locked");
        assertThat(updated.getLockRecordId()).isEqualTo(99L);
        assertThat(updated.getTeamNo()).isEqualTo("T20260611");
        assertThat(updated.getTeamName()).isEqualTo("苏州三日游");
    }

    @Test
    void generateInventoryShouldCreateRoomTypeDailyQuantityRows() {
        ControlledRoomResourceMapper resourceMapper = mock(ControlledRoomResourceMapper.class);
        ControlledRoomUnitMapper roomMapper = mock(ControlledRoomUnitMapper.class);
        ControlledRoomDayStatusMapper dayStatusMapper = mock(ControlledRoomDayStatusMapper.class);
        ControlledRoomLockRecordMapper lockRecordMapper = mock(ControlledRoomLockRecordMapper.class);
        ControlledRoomTypeMapper roomTypeMapper = mock(ControlledRoomTypeMapper.class);
        RoomInventoryMapper inventoryMapper = mock(RoomInventoryMapper.class);
        PurchaseRelationMapper purchaseRelationMapper = mock(PurchaseRelationMapper.class);
        ControlledRoomStatusService service = new ControlledRoomStatusService(
                resourceMapper, roomMapper, dayStatusMapper, lockRecordMapper, roomTypeMapper, inventoryMapper, purchaseRelationMapper);
        ArgumentCaptor<RoomInventoryEntity> inventoryCaptor = ArgumentCaptor.forClass(RoomInventoryEntity.class);

        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource());
        when(roomTypeMapper.selectOne(any(Wrapper.class))).thenReturn(roomType());
        when(inventoryMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        int created = service.generateInventories(new RoomInventoryGenerateRequest(
                "self_owned",
                10L,
                100L,
                "标准间",
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 6, 13),
                18,
                "active"
        ), 1L, "admin");

        assertThat(created).isEqualTo(2);
        verify(inventoryMapper, org.mockito.Mockito.times(2)).insert(inventoryCaptor.capture());
        RoomInventoryEntity first = inventoryCaptor.getAllValues().get(0);
        assertThat(first.getSourceType()).isEqualTo("self_owned");
        assertThat(first.getHotelName()).isEqualTo("苏州中心酒店");
        assertThat(first.getRoomType()).isEqualTo("标准间");
        assertThat(first.getTotalQuantity()).isEqualTo(18);
        assertThat(first.getLockedQuantity()).isZero();
        assertThat(first.getRemainingQuantity()).isEqualTo(18);
    }

    @Test
    void lockInventoryShouldDecreaseRemainingAndCreateQuantityLockRecord() {
        ControlledRoomResourceMapper resourceMapper = mock(ControlledRoomResourceMapper.class);
        ControlledRoomUnitMapper roomMapper = mock(ControlledRoomUnitMapper.class);
        ControlledRoomDayStatusMapper dayStatusMapper = mock(ControlledRoomDayStatusMapper.class);
        ControlledRoomLockRecordMapper lockRecordMapper = mock(ControlledRoomLockRecordMapper.class);
        ControlledRoomTypeMapper roomTypeMapper = mock(ControlledRoomTypeMapper.class);
        RoomInventoryMapper inventoryMapper = mock(RoomInventoryMapper.class);
        PurchaseRelationMapper purchaseRelationMapper = mock(PurchaseRelationMapper.class);
        ControlledRoomStatusService service = new ControlledRoomStatusService(
                resourceMapper, roomMapper, dayStatusMapper, lockRecordMapper, roomTypeMapper, inventoryMapper, purchaseRelationMapper);
        ArgumentCaptor<ControlledRoomLockRecordEntity> lockCaptor = ArgumentCaptor.forClass(ControlledRoomLockRecordEntity.class);
        ArgumentCaptor<RoomInventoryEntity> inventoryUpdateCaptor = ArgumentCaptor.forClass(RoomInventoryEntity.class);

        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource());
        when(roomTypeMapper.selectOne(any(Wrapper.class))).thenReturn(roomType());
        when(inventoryMapper.selectOne(any(Wrapper.class))).thenReturn(inventory(LocalDate.of(2026, 6, 11), 18, 0, 0, 18));
        doAnswer((Answer<Integer>) invocation -> {
            ControlledRoomLockRecordEntity entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        }).when(lockRecordMapper).insert(any(ControlledRoomLockRecordEntity.class));
        when(inventoryMapper.update(any(RoomInventoryEntity.class), any(Wrapper.class))).thenReturn(1);

        service.lockInventory(new RoomInventoryLockRequest(
                "self_owned",
                10L,
                100L,
                "标准间",
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 6, 12),
                3,
                "T20260611",
                "苏州三日游",
                "四钻",
                "团队住宿预锁"
        ), 1L, "admin");

        verify(lockRecordMapper).insert(lockCaptor.capture());
        assertThat(lockCaptor.getValue().getQuantity()).isEqualTo(3);
        assertThat(lockCaptor.getValue().getRoomId()).isNull();
        assertThat(lockCaptor.getValue().getRoomType()).isEqualTo("标准间");
        verify(inventoryMapper).update(inventoryUpdateCaptor.capture(), any(Wrapper.class));
        RoomInventoryEntity update = inventoryUpdateCaptor.getValue();
        assertThat(update.getLockedQuantity()).isEqualTo(3);
        assertThat(update.getRemainingQuantity()).isEqualTo(15);
    }

    @Test
    void pageLocksShouldSupportQuantityLockWithoutSpecificRoom() {
        ControlledRoomResourceMapper resourceMapper = mock(ControlledRoomResourceMapper.class);
        ControlledRoomUnitMapper roomMapper = mock(ControlledRoomUnitMapper.class);
        ControlledRoomDayStatusMapper dayStatusMapper = mock(ControlledRoomDayStatusMapper.class);
        ControlledRoomLockRecordMapper lockRecordMapper = mock(ControlledRoomLockRecordMapper.class);
        ControlledRoomTypeMapper roomTypeMapper = mock(ControlledRoomTypeMapper.class);
        RoomInventoryMapper inventoryMapper = mock(RoomInventoryMapper.class);
        PurchaseRelationMapper purchaseRelationMapper = mock(PurchaseRelationMapper.class);
        ControlledRoomStatusService service = new ControlledRoomStatusService(
                resourceMapper, roomMapper, dayStatusMapper, lockRecordMapper, roomTypeMapper, inventoryMapper, purchaseRelationMapper);

        ControlledRoomLockRecordEntity lock = new ControlledRoomLockRecordEntity();
        lock.setId(88L);
        lock.setTenantId(1L);
        lock.setResourceId(10L);
        lock.setSourceType("self_owned");
        lock.setSourceId(10L);
        lock.setRoomTypeId(100L);
        lock.setRoomType("标准间");
        lock.setQuantity(3);
        lock.setRoomId(null);
        lock.setCheckInDate(LocalDate.of(2026, 6, 11));
        lock.setCheckOutDate(LocalDate.of(2026, 6, 12));
        lock.setTeamNo("T20260611");
        lock.setTeamName("苏州三日游");
        lock.setRequiredStandard("四钻");
        lock.setStatus("locked");

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ControlledRoomLockRecordEntity> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(lock));
        page.setTotal(1);
        when(lockRecordMapper.selectPage(any(), any(Wrapper.class))).thenReturn(page);
        when(resourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(resource()));

        var result = service.pageLocks(1L, null, null, null, 1, 20);

        assertThat(result.total()).isEqualTo(1);
        ControlledRoomLockRecordResponse response = result.items().get(0);
        assertThat(response.roomId()).isNull();
        assertThat(response.roomNo()).isNull();
        assertThat(response.roomType()).isEqualTo("标准间");
        assertThat(response.hotelName()).isEqualTo("苏州中心酒店");
    }

    private ControlledRoomLockRequest lockRequest() {
        return new ControlledRoomLockRequest(
                10L,
                List.of(20L),
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 6, 12),
                "T20260611",
                "苏州三日游",
                "四钻",
                "团队住宿预锁"
        );
    }

    private ControlledRoomResourceEntity resource() {
        ControlledRoomResourceEntity entity = new ControlledRoomResourceEntity();
        entity.setId(10L);
        entity.setTenantId(1L);
        entity.setHotelName("苏州中心酒店");
        entity.setRoomType("标准间");
        entity.setStarStandard("四钻");
        entity.setStatus("active");
        entity.setValidFrom(LocalDate.of(2026, 6, 1));
        entity.setValidTo(LocalDate.of(2026, 6, 30));
        return entity;
    }

    private ControlledRoomTypeEntity roomType() {
        ControlledRoomTypeEntity entity = new ControlledRoomTypeEntity();
        entity.setId(100L);
        entity.setTenantId(1L);
        entity.setResourceId(10L);
        entity.setRoomType("标准间");
        entity.setBedType("双床");
        entity.setCapacity(2);
        entity.setPurchasePrice(BigDecimal.valueOf(420));
        entity.setAgreementPrice(BigDecimal.valueOf(520));
        entity.setPriceUnit("间夜");
        entity.setStatus("active");
        return entity;
    }

    private ControlledRoomUnitEntity room() {
        ControlledRoomUnitEntity entity = new ControlledRoomUnitEntity();
        entity.setId(20L);
        entity.setTenantId(1L);
        entity.setResourceId(10L);
        entity.setRoomNo("801");
        entity.setRoomType("标准间");
        entity.setStatus("active");
        return entity;
    }

    private ControlledRoomDayStatusEntity dayStatus(LocalDate date, String status) {
        ControlledRoomDayStatusEntity entity = new ControlledRoomDayStatusEntity();
        entity.setId(30L);
        entity.setTenantId(1L);
        entity.setResourceId(10L);
        entity.setRoomId(20L);
        entity.setStayDate(date);
        entity.setStatus(status);
        return entity;
    }

    private RoomInventoryEntity inventory(LocalDate date, int total, int locked, int occupied, int remaining) {
        RoomInventoryEntity entity = new RoomInventoryEntity();
        entity.setId(40L);
        entity.setTenantId(1L);
        entity.setSourceType("self_owned");
        entity.setSourceId(10L);
        entity.setRoomTypeId(100L);
        entity.setHotelName("苏州中心酒店");
        entity.setRoomType("标准间");
        entity.setStayDate(date);
        entity.setTotalQuantity(total);
        entity.setLockedQuantity(locked);
        entity.setOccupiedQuantity(occupied);
        entity.setRemainingQuantity(remaining);
        entity.setStatus("active");
        return entity;
    }
}

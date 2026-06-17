package com.mtravel.platform.dispatch.roomstatus.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomDayStatusResponse;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomGenerateStatusRequest;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomLockRecordResponse;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomLockRequest;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomResourceResponse;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomResourceSaveRequest;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomTypeResponse;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomTypeSaveRequest;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomUnitResponse;
import com.mtravel.platform.dispatch.roomstatus.dto.ControlledRoomUnitSaveRequest;
import com.mtravel.platform.dispatch.roomstatus.dto.RoomInventoryCalendarResponse;
import com.mtravel.platform.dispatch.roomstatus.dto.RoomInventoryGenerateRequest;
import com.mtravel.platform.dispatch.roomstatus.dto.RoomInventoryLockRequest;
import com.mtravel.platform.dispatch.roomstatus.dto.RoomInventoryOccupancyResponse;
import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomDayStatusEntity;
import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomLockRecordEntity;
import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomResourceEntity;
import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomTypeEntity;
import com.mtravel.platform.dispatch.roomstatus.entity.ControlledRoomUnitEntity;
import com.mtravel.platform.dispatch.roomstatus.entity.RoomInventoryEntity;
import com.mtravel.platform.dispatch.roomstatus.enums.ControlledRoomDayStatus;
import com.mtravel.platform.dispatch.roomstatus.enums.ControlledRoomLockStatus;
import com.mtravel.platform.dispatch.roomstatus.enums.ControlledRoomResourceStatus;
import com.mtravel.platform.dispatch.roomstatus.enums.ControlledRoomUnitStatus;
import com.mtravel.platform.dispatch.roomstatus.mapper.ControlledRoomDayStatusMapper;
import com.mtravel.platform.dispatch.roomstatus.mapper.ControlledRoomLockRecordMapper;
import com.mtravel.platform.dispatch.roomstatus.mapper.ControlledRoomResourceMapper;
import com.mtravel.platform.dispatch.roomstatus.mapper.ControlledRoomTypeMapper;
import com.mtravel.platform.dispatch.roomstatus.mapper.ControlledRoomUnitMapper;
import com.mtravel.platform.dispatch.roomstatus.mapper.RoomInventoryMapper;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 自控房源与房态库存服务。
 *
 * <p>本服务只处理企业已买断、包房或长期协议控制的房源。普通外部酒店采购关系仍由采购模块管理。
 * 房态逻辑以具体房间和日期为核心，保证后续排房可以从锁定转为占用，并能释放回可用。</p>
 */
@Service
public class ControlledRoomStatusService extends BusinessCrudService<ControlledRoomResourceEntity, ControlledRoomResourceResponse> {

    private final ControlledRoomResourceMapper resourceMapper;
    private final ControlledRoomUnitMapper roomMapper;
    private final ControlledRoomDayStatusMapper dayStatusMapper;
    private final ControlledRoomLockRecordMapper lockRecordMapper;
    private final ControlledRoomTypeMapper roomTypeMapper;
    private final RoomInventoryMapper inventoryMapper;
    private final PurchaseRelationMapper purchaseRelationMapper;

    @Autowired
    public ControlledRoomStatusService(
            ControlledRoomResourceMapper resourceMapper,
            ControlledRoomUnitMapper roomMapper,
            ControlledRoomDayStatusMapper dayStatusMapper,
            ControlledRoomLockRecordMapper lockRecordMapper,
            ControlledRoomTypeMapper roomTypeMapper,
            RoomInventoryMapper inventoryMapper,
            PurchaseRelationMapper purchaseRelationMapper
    ) {
        super(resourceMapper);
        this.resourceMapper = resourceMapper;
        this.roomMapper = roomMapper;
        this.dayStatusMapper = dayStatusMapper;
        this.lockRecordMapper = lockRecordMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.inventoryMapper = inventoryMapper;
        this.purchaseRelationMapper = purchaseRelationMapper;
    }

    /**
     * 分页查询自控房源批次。
     */
    public PageResult<ControlledRoomResourceResponse> pageResources(
            Long tenantId,
            String keyword,
            String province,
            String city,
            String district,
            String starStandard,
            String roomType,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<ControlledRoomResourceEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(province), "province", province)
                .eq(StringUtils.hasText(city), "city", city)
                .eq(StringUtils.hasText(district), "district", district)
                .eq(StringUtils.hasText(starStandard), "star_standard", starStandard)
                .eq(StringUtils.hasText(status), "status", status)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("hotel_name", keyword)
                        .or()
                        .like("district", keyword)
                        .or()
                        .like("area", keyword)
                        .or()
                        .like("source_name", keyword))
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /**
     * 查询自控房源下拉列表。
     */
    public List<ControlledRoomResourceResponse> listResources(Long tenantId, boolean includeDisabled) {
        QueryWrapper<ControlledRoomResourceEntity> wrapper = baseQuery(tenantId)
                .ne(!includeDisabled, "status", ControlledRoomResourceStatus.DISABLED.getValue())
                .orderByAsc("province")
                .orderByAsc("city")
                .orderByAsc("district")
                .orderByAsc("hotel_name");
        return resourceMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    /**
     * 新增自控房源批次。
     */
    public ControlledRoomResourceResponse createResource(
            ControlledRoomResourceSaveRequest request,
            Long tenantId,
            String operator
    ) {
        validateDateRange(request.validFrom(), request.validTo());
        assertResourceDuplicate(tenantId, request, null);

        ControlledRoomResourceEntity entity = new ControlledRoomResourceEntity();
        entity.setTenantId(tenantId);
        applyResourceFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        resourceMapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改自控房源批次。
     */
    public ControlledRoomResourceResponse updateResource(
            Long id,
            ControlledRoomResourceSaveRequest request,
            Long tenantId
    ) {
        validateDateRange(request.validFrom(), request.validTo());
        assertResourceDuplicate(tenantId, request, id);

        int updated = resourceMapper.update(null, baseUpdate(tenantId)
                .eq("id", id)
                .set("hotel_name", cleanRequired(request.hotelName()))
                .set("province", clean(request.province()))
                .set("city", clean(request.city()))
                .set("district", clean(request.district()))
                .set("area", clean(request.area()))
                .set("address", clean(request.address()))
                .set("star_standard", clean(request.starStandard()))
                .set("room_type", clean(request.roomType()))
                .set("source_name", clean(request.sourceName()))
                .set("purchase_price", money(request.purchasePrice()))
                .set("agreement_price", money(request.agreementPrice()))
                .set("price_unit", StringUtils.hasText(request.priceUnit()) ? clean(request.priceUnit()) : "间夜")
                .set("valid_from", request.validFrom())
                .set("valid_to", request.validTo())
                .set("contact_name", clean(request.contactName()))
                .set("contact_phone", clean(request.contactPhone()))
                .set("status", ControlledRoomResourceStatus.fromValueOrDefault(request.status()).getValue())
                .set("remark", clean(request.remark())));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /**
     * 分页查询自控房间明细。
     */
    public PageResult<ControlledRoomUnitResponse> pageRooms(
            Long tenantId,
            Long resourceId,
            String keyword,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<ControlledRoomUnitEntity> wrapper = roomBaseQuery(tenantId)
                .eq(resourceId != null, "resource_id", resourceId)
                .eq(StringUtils.hasText(status), "status", status)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("room_no", keyword)
                        .or()
                        .like("building_name", keyword)
                        .or()
                        .like("room_type", keyword))
                .orderByAsc("resource_id")
                .orderByAsc("building_name")
                .orderByAsc("floor_no")
                .orderByAsc("room_no");
        var result = roomMapper.selectPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(page, pageSize), wrapper);
        List<ControlledRoomUnitResponse> items = toRoomResponses(tenantId, result.getRecords());
        return new PageResult<>(items, result.getTotal());
    }

    /**
     * 新增具体房间。
     */
    public ControlledRoomUnitResponse createRoom(
            ControlledRoomUnitSaveRequest request,
            Long tenantId,
            String operator
    ) {
        ControlledRoomResourceEntity resource = requireResource(tenantId, request.resourceId());
        assertRoomDuplicate(tenantId, request.resourceId(), request.roomNo(), null);

        ControlledRoomUnitEntity entity = new ControlledRoomUnitEntity();
        entity.setTenantId(tenantId);
        applyRoomFields(entity, request, resource);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        roomMapper.insert(entity);
        return roomResponse(entity, resource);
    }

    /**
     * 修改具体房间。
     */
    public ControlledRoomUnitResponse updateRoom(
            Long id,
            ControlledRoomUnitSaveRequest request,
            Long tenantId
    ) {
        ControlledRoomResourceEntity resource = requireResource(tenantId, request.resourceId());
        assertRoomDuplicate(tenantId, request.resourceId(), request.roomNo(), id);

        int updated = roomMapper.update(null, roomBaseUpdate(tenantId)
                .eq("id", id)
                .set("resource_id", request.resourceId())
                .set("building_name", clean(request.buildingName()))
                .set("floor_no", clean(request.floorNo()))
                .set("room_no", cleanRequired(request.roomNo()))
                .set("room_type", clean(request.roomType()))
                .set("bed_type", clean(request.bedType()))
                .set("capacity", number(request.capacity()))
                .set("status", StringUtils.hasText(request.status()) ? request.status() : ControlledRoomUnitStatus.ACTIVE.getValue())
                .set("remark", clean(request.remark())));
        if (updated == 0) {
            throw new BizException("房间不存在或已删除");
        }
        ControlledRoomUnitEntity entity = roomMapper.selectOne(roomBaseQuery(tenantId).eq("id", id));
        return roomResponse(entity, resource);
    }

    /**
     * 软删除具体房间。
     *
     * <p>存在已锁定或已占用房态时禁止删除，避免排房记录失去房间来源。</p>
     */
    public void deleteRoom(Long id, Long tenantId, String operator) {
        Long activeStatusCount = dayStatusMapper.selectCount(dayStatusBaseQuery(tenantId)
                .eq("room_id", id)
                .in("status",
                        ControlledRoomDayStatus.LOCKED.getValue(),
                        ControlledRoomDayStatus.OCCUPIED.getValue()));
        if (activeStatusCount != null && activeStatusCount > 0) {
            throw new BizException("房间已有锁定或占用记录，不能删除");
        }
        ControlledRoomUnitEntity entity = new ControlledRoomUnitEntity();
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(operator);
        int updated = roomMapper.update(entity, roomBaseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("房间不存在或已删除");
        }
    }

    /**
     * 分页查询自营房型。
     *
     * <p>房型承载标间、大床房、三人间等库存和价格口径。房源档案只表达酒店，
     * 不再因为不同房型重复维护酒店资料。</p>
     */
    public PageResult<ControlledRoomTypeResponse> pageRoomTypes(
            Long tenantId,
            Long resourceId,
            String keyword,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<ControlledRoomTypeEntity> wrapper = roomTypeBaseQuery(tenantId)
                .eq(resourceId != null, "resource_id", resourceId)
                .eq(StringUtils.hasText(status), "status", status)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("room_type", keyword)
                        .or()
                        .like("bed_type", keyword))
                .orderByAsc("resource_id")
                .orderByAsc("room_type");
        var result = roomTypeMapper.selectPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(page, pageSize), wrapper);
        return new PageResult<>(toRoomTypeResponses(tenantId, result.getRecords()), result.getTotal());
    }

    /**
     * 查询自营房型下拉列表。
     */
    public List<ControlledRoomTypeResponse> listRoomTypes(Long tenantId, Long resourceId, boolean includeDisabled) {
        QueryWrapper<ControlledRoomTypeEntity> wrapper = roomTypeBaseQuery(tenantId)
                .eq(resourceId != null, "resource_id", resourceId)
                .ne(!includeDisabled, "status", ControlledRoomResourceStatus.DISABLED.getValue())
                .orderByAsc("resource_id")
                .orderByAsc("room_type");
        return toRoomTypeResponses(tenantId, roomTypeMapper.selectList(wrapper));
    }

    /**
     * 新增自营房型。
     */
    public ControlledRoomTypeResponse createRoomType(
            ControlledRoomTypeSaveRequest request,
            Long tenantId,
            String operator
    ) {
        ControlledRoomResourceEntity resource = requireResource(tenantId, request.resourceId());
        assertRoomTypeDuplicate(tenantId, request.resourceId(), request.roomType(), null);

        ControlledRoomTypeEntity entity = new ControlledRoomTypeEntity();
        entity.setTenantId(tenantId);
        applyRoomTypeFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        roomTypeMapper.insert(entity);
        return ControlledRoomTypeResponse.fromEntity(entity, resource.getHotelName());
    }

    /**
     * 修改自营房型。
     */
    public ControlledRoomTypeResponse updateRoomType(
            Long id,
            ControlledRoomTypeSaveRequest request,
            Long tenantId
    ) {
        ControlledRoomResourceEntity resource = requireResource(tenantId, request.resourceId());
        assertRoomTypeDuplicate(tenantId, request.resourceId(), request.roomType(), id);
        ControlledRoomTypeEntity entity = new ControlledRoomTypeEntity();
        applyRoomTypeFields(entity, request);
        int updated = roomTypeMapper.update(entity, roomTypeBaseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("房型不存在或已删除");
        }
        ControlledRoomTypeEntity saved = roomTypeMapper.selectOne(roomTypeBaseQuery(tenantId).eq("id", id));
        return ControlledRoomTypeResponse.fromEntity(saved, resource.getHotelName());
    }

    /**
     * 软删除自营房型。
     */
    public void deleteRoomType(Long id, Long tenantId, String operator) {
        Long activeInventoryCount = inventoryMapper.selectCount(inventoryBaseQuery(tenantId)
                .eq("source_type", "self_owned")
                .eq("room_type_id", id)
                .and(nested -> nested
                        .gt("locked_quantity", 0)
                        .or()
                        .gt("occupied_quantity", 0)));
        if (activeInventoryCount != null && activeInventoryCount > 0) {
            throw new BizException("房型已有锁定或占用库存，不能删除");
        }
        ControlledRoomTypeEntity entity = new ControlledRoomTypeEntity();
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(operator);
        int updated = roomTypeMapper.update(entity, roomTypeBaseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("房型不存在或已删除");
        }
    }

    /**
     * 生成房态日历。
     *
     * <p>只补缺失日期，不覆盖已有业务状态。生成范围必须落在自控房源有效期内。</p>
     */
    @Transactional
    public int generateDayStatuses(
            ControlledRoomGenerateStatusRequest request,
            Long tenantId,
            String operator
    ) {
        ControlledRoomResourceEntity resource = requireResource(tenantId, request.resourceId());
        validateDateRange(request.startDate(), request.endDate());
        assertWithinValidPeriod(resource, request.startDate(), request.endDate().minusDays(1));

        List<ControlledRoomUnitEntity> rooms = roomMapper.selectList(roomBaseQuery(tenantId)
                .eq("resource_id", request.resourceId())
                .eq("status", ControlledRoomUnitStatus.ACTIVE.getValue())
                .orderByAsc("room_no"));
        int created = 0;
        for (ControlledRoomUnitEntity room : rooms) {
            LocalDate cursor = request.startDate();
            while (cursor.isBefore(request.endDate())) {
                ControlledRoomDayStatusEntity existing = findDayStatus(tenantId, room.getId(), cursor);
                if (existing == null) {
                    ControlledRoomDayStatusEntity entity = new ControlledRoomDayStatusEntity();
                    entity.setTenantId(tenantId);
                    entity.setResourceId(resource.getId());
                    entity.setRoomId(room.getId());
                    entity.setStayDate(cursor);
                    entity.setStatus(ControlledRoomDayStatus.AVAILABLE.getValue());
                    entity.setCreatedBy(operator);
                    entity.setIsDeleted(false);
                    dayStatusMapper.insert(entity);
                    created++;
                }
                cursor = cursor.plusDays(1);
            }
        }
        return created;
    }

    /**
     * 查询房态日历。
     */
    public List<ControlledRoomDayStatusResponse> calendar(
            Long tenantId,
            Long resourceId,
            LocalDate startDate,
            LocalDate endDate,
            String starStandard,
            String status
    ) {
        validateDateRange(startDate, endDate);
        QueryWrapper<ControlledRoomDayStatusEntity> wrapper = dayStatusBaseQuery(tenantId)
                .eq(resourceId != null, "resource_id", resourceId)
                .between("stay_date", startDate, endDate.minusDays(1))
                .eq(StringUtils.hasText(status), "status", status)
                .orderByAsc("resource_id")
                .orderByAsc("room_id")
                .orderByAsc("stay_date");
        List<ControlledRoomDayStatusEntity> statuses = dayStatusMapper.selectList(wrapper);
        return toDayStatusResponses(tenantId, statuses, starStandard);
    }

    /**
     * 按来源、房型和日期生成聚合房态库存。
     *
     * <p>新房态口径参考老系统“总量/余量”，但额外拆出已锁和已占。计调排房先锁数量，
     * 自营房源后续需要精细排房时再分配具体房号。</p>
     */
    @Transactional
    public int generateInventories(
            RoomInventoryGenerateRequest request,
            Long tenantId,
            String operator
    ) {
        validateDateRange(request.startDate(), request.endDate());
        if (request.totalQuantity() == null || request.totalQuantity() < 0) {
            throw new BizException("总量不能小于0");
        }
        SourceSnapshot source = requireSourceSnapshot(
                tenantId,
                request.sourceType(),
                request.sourceId(),
                request.roomTypeId(),
                request.roomType()
        );
        if ("self_owned".equals(source.sourceType())) {
            assertWithinValidPeriod(requireResource(tenantId, source.sourceId()), request.startDate(), request.endDate().minusDays(1));
        }
        int created = 0;
        LocalDate cursor = request.startDate();
        while (cursor.isBefore(request.endDate())) {
            RoomInventoryEntity existing = findInventory(
                    tenantId,
                    source.sourceType(),
                    source.sourceId(),
                    source.roomTypeId(),
                    source.roomType(),
                    cursor
            );
            if (existing == null) {
                RoomInventoryEntity entity = new RoomInventoryEntity();
                entity.setTenantId(tenantId);
                entity.setSourceType(source.sourceType());
                entity.setSourceId(source.sourceId());
                entity.setRoomTypeId(source.roomTypeId());
                entity.setHotelName(source.hotelName());
                entity.setSupplierName(source.supplierName());
                entity.setRoomType(source.roomType());
                entity.setStayDate(cursor);
                entity.setTotalQuantity(request.totalQuantity());
                entity.setLockedQuantity(0);
                entity.setOccupiedQuantity(0);
                entity.setRemainingQuantity(request.totalQuantity());
                entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
                entity.setCreatedBy(operator);
                entity.setIsDeleted(false);
                inventoryMapper.insert(entity);
                created++;
            }
            cursor = cursor.plusDays(1);
        }
        return created;
    }

    /**
     * 查询聚合房态库存日历。
     */
    public List<RoomInventoryCalendarResponse> inventoryCalendar(
            Long tenantId,
            String sourceType,
            Long sourceId,
            Long roomTypeId,
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {
        validateDateRange(startDate, endDate);
        QueryWrapper<RoomInventoryEntity> wrapper = inventoryBaseQuery(tenantId)
                .eq(StringUtils.hasText(sourceType), "source_type", sourceType)
                .eq(sourceId != null, "source_id", sourceId)
                .eq(roomTypeId != null, "room_type_id", roomTypeId)
                .eq(StringUtils.hasText(status), "status", status)
                .between("stay_date", startDate, endDate.minusDays(1))
                .orderByAsc("hotel_name")
                .orderByAsc("room_type")
                .orderByAsc("stay_date");
        return inventoryMapper.selectList(wrapper).stream()
                .map(RoomInventoryCalendarResponse::fromEntity)
                .toList();
    }

    /**
     * 按房型数量锁定房态库存。
     *
     * <p>锁房按每一天扣减余量，并写入一条数量锁房流水。这里不绑定具体房号，
     * 避免计调在初步排房时被迫先选房间号。</p>
     */
    @Transactional
    public ControlledRoomLockRecordResponse lockInventory(
            RoomInventoryLockRequest request,
            Long tenantId,
            String operator
    ) {
        validateDateRange(request.checkInDate(), request.checkOutDate());
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BizException("锁房数量必须大于0");
        }
        SourceSnapshot source = requireSourceSnapshot(
                tenantId,
                request.sourceType(),
                request.sourceId(),
                request.roomTypeId(),
                request.roomType()
        );
        if ("self_owned".equals(source.sourceType())) {
            assertWithinValidPeriod(requireResource(tenantId, source.sourceId()), request.checkInDate(), request.checkOutDate().minusDays(1));
        }

        List<RoomInventoryEntity> inventories = new ArrayList<>();
        LocalDate cursor = request.checkInDate();
        while (cursor.isBefore(request.checkOutDate())) {
            RoomInventoryEntity inventory = findInventory(
                    tenantId,
                    source.sourceType(),
                    source.sourceId(),
                    source.roomTypeId(),
                    source.roomType(),
                    cursor
            );
            if (inventory == null) {
                throw new BizException(source.hotelName() + " " + source.roomType() + " " + cursor + " 未生成房态库存");
            }
            if (!"active".equals(inventory.getStatus())) {
                throw new BizException(source.hotelName() + " " + cursor + " 房态已停用，不能锁房");
            }
            if (number(inventory.getRemainingQuantity()) < request.quantity()) {
                throw new BizException(source.hotelName() + " " + source.roomType() + " " + cursor + " 余房不足");
            }
            inventories.add(inventory);
            cursor = cursor.plusDays(1);
        }

        ControlledRoomLockRecordEntity lock = new ControlledRoomLockRecordEntity();
        lock.setTenantId(tenantId);
        lock.setResourceId("self_owned".equals(source.sourceType()) ? source.sourceId() : null);
        lock.setSourceType(source.sourceType());
        lock.setSourceId(source.sourceId());
        lock.setRoomTypeId(source.roomTypeId());
        lock.setRoomType(source.roomType());
        lock.setQuantity(request.quantity());
        lock.setRoomId(null);
        lock.setCheckInDate(request.checkInDate());
        lock.setCheckOutDate(request.checkOutDate());
        lock.setTeamNo(clean(request.teamNo()));
        lock.setTeamName(clean(request.teamName()));
        lock.setRequiredStandard(clean(request.requiredStandard()));
        lock.setStatus(ControlledRoomLockStatus.LOCKED.getValue());
        lock.setCreatedBy(operator);
        lock.setRemark(clean(request.remark()));
        lock.setIsDeleted(false);
        lockRecordMapper.insert(lock);

        for (RoomInventoryEntity inventory : inventories) {
            RoomInventoryEntity update = new RoomInventoryEntity();
            update.setLockedQuantity(number(inventory.getLockedQuantity()) + request.quantity());
            update.setRemainingQuantity(number(inventory.getRemainingQuantity()) - request.quantity());
            int updated = inventoryMapper.update(update, inventoryBaseUpdate(tenantId)
                    .eq("id", inventory.getId())
                    .ge("remaining_quantity", request.quantity()));
            if (updated == 0) {
                throw new BizException("房态库存已变化，请刷新后重试");
            }
        }
        return ControlledRoomLockRecordResponse.fromEntity(
                lock,
                source.hotelName(),
                null,
                source.roomType(),
                source.starStandard()
        );
    }

    /**
     * 查询某一天房态库存的团队占用明细。
     */
    public List<RoomInventoryOccupancyResponse> inventoryOccupancy(
            Long tenantId,
            String sourceType,
            Long sourceId,
            Long roomTypeId,
            String roomType,
            LocalDate stayDate
    ) {
        if (stayDate == null) {
            throw new BizException("住宿日期不能为空");
        }
        QueryWrapper<ControlledRoomLockRecordEntity> wrapper = lockBaseQuery(tenantId)
                .eq(StringUtils.hasText(sourceType), "source_type", sourceType)
                .eq(sourceId != null, "source_id", sourceId)
                .eq(roomTypeId != null, "room_type_id", roomTypeId)
                .eq(StringUtils.hasText(roomType), "room_type", roomType)
                .le("check_in_date", stayDate)
                .gt("check_out_date", stayDate)
                .in("status", ControlledRoomLockStatus.LOCKED.getValue(), ControlledRoomLockStatus.OCCUPIED.getValue())
                .orderByDesc("id");
        return lockRecordMapper.selectList(wrapper).stream()
                .map(lock -> RoomInventoryOccupancyResponse.fromLock(lock, stayDate))
                .toList();
    }

    /**
     * 锁定自控房间。
     *
     * <p>锁房会逐日校验房态，只允许 available 状态转为 locked。有效期外、停用房源、维修房间、
     * 已锁定或已占用日期都会被拒绝。</p>
     */
    @Transactional
    public List<ControlledRoomLockRecordResponse> lockRooms(
            ControlledRoomLockRequest request,
            Long tenantId,
            String operator
    ) {
        validateDateRange(request.checkInDate(), request.checkOutDate());
        ControlledRoomResourceEntity resource = requireResource(tenantId, request.resourceId());
        if (!ControlledRoomResourceStatus.ACTIVE.getValue().equals(resource.getStatus())) {
            throw new BizException("自控房源未启用，不能锁房");
        }
        assertWithinValidPeriod(resource, request.checkInDate(), request.checkOutDate().minusDays(1));

        List<ControlledRoomLockRecordResponse> responses = new ArrayList<>();
        for (Long roomId : request.roomIds()) {
            ControlledRoomUnitEntity room = requireRoom(tenantId, request.resourceId(), roomId);
            if (!ControlledRoomUnitStatus.ACTIVE.getValue().equals(room.getStatus())) {
                throw new BizException("房间未启用或维修中，不能锁房");
            }
            List<ControlledRoomDayStatusEntity> dayStatuses = requireAvailableStatuses(
                    tenantId, resource, room, request.checkInDate(), request.checkOutDate(), operator);

            ControlledRoomLockRecordEntity lock = new ControlledRoomLockRecordEntity();
            lock.setTenantId(tenantId);
            lock.setResourceId(resource.getId());
            lock.setRoomId(room.getId());
            lock.setCheckInDate(request.checkInDate());
            lock.setCheckOutDate(request.checkOutDate());
            lock.setTeamNo(clean(request.teamNo()));
            lock.setTeamName(clean(request.teamName()));
            lock.setRequiredStandard(clean(request.requiredStandard()));
            lock.setStatus(ControlledRoomLockStatus.LOCKED.getValue());
            lock.setCreatedBy(operator);
            lock.setRemark(clean(request.remark()));
            lock.setIsDeleted(false);
            lockRecordMapper.insert(lock);

            for (ControlledRoomDayStatusEntity status : dayStatuses) {
                ControlledRoomDayStatusEntity update = new ControlledRoomDayStatusEntity();
                update.setStatus(ControlledRoomDayStatus.LOCKED.getValue());
                update.setLockRecordId(lock.getId());
                update.setTeamNo(lock.getTeamNo());
                update.setTeamName(lock.getTeamName());
                int updated = dayStatusMapper.update(update, dayStatusBaseUpdate(tenantId)
                        .eq("id", status.getId())
                        .eq("status", ControlledRoomDayStatus.AVAILABLE.getValue()));
                if (updated == 0) {
                    throw new BizException("房态已被占用，请刷新后重试");
                }
            }
            responses.add(lockResponse(lock, resource, room));
        }
        return responses;
    }

    /**
     * 释放锁房。
     */
    @Transactional
    public void releaseLock(Long lockId, Long tenantId, String operator) {
        ControlledRoomLockRecordEntity lock = lockRecordMapper.selectOne(lockBaseQuery(tenantId).eq("id", lockId));
        if (lock == null) {
            throw new BizException("锁房记录不存在或已删除");
        }
        if (!ControlledRoomLockStatus.LOCKED.getValue().equals(lock.getStatus())) {
            throw new BizException("只有已锁定记录可以释放");
        }

        int updated = lockRecordMapper.update(null, lockBaseUpdate(tenantId)
                .eq("id", lockId)
                .set("status", ControlledRoomLockStatus.RELEASED.getValue())
                .set("released_at", OffsetDateTime.now())
                .set("released_by", operator));
        if (updated == 0) {
            throw new BizException("锁房记录不存在或已删除");
        }

        dayStatusMapper.update(null, dayStatusBaseUpdate(tenantId)
                .eq("lock_record_id", lockId)
                .eq("status", ControlledRoomDayStatus.LOCKED.getValue())
                .set("status", ControlledRoomDayStatus.AVAILABLE.getValue())
                .set("lock_record_id", null)
                .set("team_no", null)
                .set("team_name", null));
        releaseInventoryQuantity(lock, tenantId);
    }

    /**
     * 查询锁房流水。
     */
    public PageResult<ControlledRoomLockRecordResponse> pageLocks(
            Long tenantId,
            Long resourceId,
            String teamNo,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<ControlledRoomLockRecordEntity> wrapper = lockBaseQuery(tenantId)
                .eq(resourceId != null, "resource_id", resourceId)
                .like(StringUtils.hasText(teamNo), "team_no", teamNo)
                .eq(StringUtils.hasText(status), "status", status)
                .orderByDesc("id");
        var result = lockRecordMapper.selectPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(page, pageSize), wrapper);
        return new PageResult<>(toLockResponses(tenantId, result.getRecords()), result.getTotal());
    }

    private List<ControlledRoomDayStatusEntity> requireAvailableStatuses(
            Long tenantId,
            ControlledRoomResourceEntity resource,
            ControlledRoomUnitEntity room,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String operator
    ) {
        List<ControlledRoomDayStatusEntity> statuses = new ArrayList<>();
        LocalDate cursor = checkInDate;
        while (cursor.isBefore(checkOutDate)) {
            ControlledRoomDayStatusEntity status = findDayStatus(tenantId, room.getId(), cursor);
            if (status == null) {
                status = createAvailableDayStatus(tenantId, resource, room, cursor, operator);
            }
            if (!ControlledRoomDayStatus.AVAILABLE.getValue().equals(status.getStatus())) {
                throw new BizException("房间 " + room.getRoomNo() + " 在 " + cursor + " 状态为 " + status.getStatus() + "，不可锁定");
            }
            statuses.add(status);
            cursor = cursor.plusDays(1);
        }
        return statuses;
    }

    private ControlledRoomDayStatusEntity createAvailableDayStatus(
            Long tenantId,
            ControlledRoomResourceEntity resource,
            ControlledRoomUnitEntity room,
            LocalDate stayDate,
            String operator
    ) {
        ControlledRoomDayStatusEntity entity = new ControlledRoomDayStatusEntity();
        entity.setTenantId(tenantId);
        entity.setResourceId(resource.getId());
        entity.setRoomId(room.getId());
        entity.setStayDate(stayDate);
        entity.setStatus(ControlledRoomDayStatus.AVAILABLE.getValue());
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        dayStatusMapper.insert(entity);
        return entity;
    }

    private ControlledRoomDayStatusEntity findDayStatus(Long tenantId, Long roomId, LocalDate stayDate) {
        return dayStatusMapper.selectOne(dayStatusBaseQuery(tenantId)
                .eq("room_id", roomId)
                .eq("stay_date", stayDate));
    }

    private ControlledRoomResourceEntity requireResource(Long tenantId, Long resourceId) {
        ControlledRoomResourceEntity resource = resourceMapper.selectOne(baseQuery(tenantId).eq("id", resourceId));
        if (resource == null) {
            throw new BizException(notFoundMessage());
        }
        return resource;
    }

    private ControlledRoomUnitEntity requireRoom(Long tenantId, Long resourceId, Long roomId) {
        ControlledRoomUnitEntity room = roomMapper.selectOne(roomBaseQuery(tenantId)
                .eq("id", roomId)
                .eq("resource_id", resourceId));
        if (room == null) {
            throw new BizException("房间不存在或已删除");
        }
        return room;
    }

    private ControlledRoomTypeEntity requireRoomType(Long tenantId, Long resourceId, Long roomTypeId) {
        ControlledRoomTypeEntity roomType = roomTypeMapper.selectOne(roomTypeBaseQuery(tenantId)
                .eq("id", roomTypeId)
                .eq("resource_id", resourceId));
        if (roomType == null) {
            throw new BizException("房型不存在或已删除");
        }
        if (!"active".equals(roomType.getStatus())) {
            throw new BizException("房型未启用，不能生成房态或锁房");
        }
        return roomType;
    }

    private PurchaseRelationEntity requirePurchaseRelation(Long tenantId, Long relationId) {
        PurchaseRelationEntity relation = purchaseRelationMapper.selectOne(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", relationId));
        if (relation == null) {
            throw new BizException("采购关系不存在或已删除");
        }
        if (!"hotel".equals(relation.getResourceType())) {
            throw new BizException("只有酒店类采购关系可以生成资源采购房源库存");
        }
        return relation;
    }

    private SourceSnapshot requireSourceSnapshot(
            Long tenantId,
            String sourceType,
            Long sourceId,
            Long roomTypeId,
            String roomType
    ) {
        String cleanedSourceType = cleanRequired(sourceType);
        if ("self_owned".equals(cleanedSourceType)) {
            ControlledRoomResourceEntity resource = requireResource(tenantId, sourceId);
            if (!ControlledRoomResourceStatus.ACTIVE.getValue().equals(resource.getStatus())) {
                throw new BizException("自营房源未启用");
            }
            ControlledRoomTypeEntity type = roomTypeId == null
                    ? requireRoomTypeByName(tenantId, sourceId, roomType)
                    : requireRoomType(tenantId, sourceId, roomTypeId);
            return new SourceSnapshot(
                    "self_owned",
                    resource.getId(),
                    type.getId(),
                    resource.getHotelName(),
                    null,
                    type.getRoomType(),
                    resource.getStarStandard()
            );
        }
        if ("purchased_resource".equals(cleanedSourceType)) {
            PurchaseRelationEntity relation = requirePurchaseRelation(tenantId, sourceId);
            return new SourceSnapshot(
                    "purchased_resource",
                    relation.getId(),
                    null,
                    relation.getResourceName(),
                    null,
                    cleanRequired(roomType),
                    null
            );
        }
        throw new BizException("房源来源类型不合法");
    }

    private ControlledRoomTypeEntity requireRoomTypeByName(Long tenantId, Long resourceId, String roomType) {
        ControlledRoomTypeEntity entity = roomTypeMapper.selectOne(roomTypeBaseQuery(tenantId)
                .eq("resource_id", resourceId)
                .eq("room_type", cleanRequired(roomType)));
        if (entity == null) {
            throw new BizException("房型不存在或已删除");
        }
        if (!"active".equals(entity.getStatus())) {
            throw new BizException("房型未启用，不能生成房态或锁房");
        }
        return entity;
    }

    private RoomInventoryEntity findInventory(
            Long tenantId,
            String sourceType,
            Long sourceId,
            Long roomTypeId,
            String roomType,
            LocalDate stayDate
    ) {
        QueryWrapper<RoomInventoryEntity> wrapper = inventoryBaseQuery(tenantId)
                .eq("source_type", sourceType)
                .eq("source_id", sourceId)
                .eq(roomTypeId != null, "room_type_id", roomTypeId)
                .eq("room_type", roomType)
                .eq("stay_date", stayDate);
        return inventoryMapper.selectOne(wrapper);
    }

    private void releaseInventoryQuantity(ControlledRoomLockRecordEntity lock, Long tenantId) {
        if (!StringUtils.hasText(lock.getSourceType()) || lock.getSourceId() == null || lock.getQuantity() == null) {
            return;
        }
        LocalDate cursor = lock.getCheckInDate();
        while (cursor.isBefore(lock.getCheckOutDate())) {
            RoomInventoryEntity inventory = findInventory(
                    tenantId,
                    lock.getSourceType(),
                    lock.getSourceId(),
                    lock.getRoomTypeId(),
                    lock.getRoomType(),
                    cursor
            );
            if (inventory != null) {
                RoomInventoryEntity update = new RoomInventoryEntity();
                update.setLockedQuantity(Math.max(0, number(inventory.getLockedQuantity()) - lock.getQuantity()));
                update.setRemainingQuantity(number(inventory.getRemainingQuantity()) + lock.getQuantity());
                inventoryMapper.update(update, inventoryBaseUpdate(tenantId).eq("id", inventory.getId()));
            }
            cursor = cursor.plusDays(1);
        }
    }

    private void assertWithinValidPeriod(ControlledRoomResourceEntity resource, LocalDate startDate, LocalDate endDate) {
        if (resource.getValidFrom() != null && startDate.isBefore(resource.getValidFrom())) {
            throw new BizException("入住日期不在房源价格有效期内");
        }
        if (resource.getValidTo() != null && endDate.isAfter(resource.getValidTo())) {
            throw new BizException("入住日期不在房源价格有效期内");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BizException("日期不能为空");
        }
        if (!endDate.isAfter(startDate)) {
            throw new BizException("结束日期必须晚于开始日期");
        }
    }

    private void assertResourceDuplicate(Long tenantId, ControlledRoomResourceSaveRequest request, Long excludeId) {
        Long count = resourceMapper.selectCount(baseQuery(tenantId)
                .eq("hotel_name", cleanRequired(request.hotelName()))
                .eq("valid_from", request.validFrom())
                .eq("valid_to", request.validTo())
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("同一酒店和有效期的自营房源已存在");
        }
    }

    private void assertRoomDuplicate(Long tenantId, Long resourceId, String roomNo, Long excludeId) {
        Long count = roomMapper.selectCount(roomBaseQuery(tenantId)
                .eq("resource_id", resourceId)
                .eq("room_no", cleanRequired(roomNo))
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("房号已存在");
        }
    }

    private void assertRoomTypeDuplicate(Long tenantId, Long resourceId, String roomType, Long excludeId) {
        Long count = roomTypeMapper.selectCount(roomTypeBaseQuery(tenantId)
                .eq("resource_id", resourceId)
                .eq("room_type", cleanRequired(roomType))
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("同一自营酒店下房型已存在");
        }
    }

    private void applyResourceFields(ControlledRoomResourceEntity entity, ControlledRoomResourceSaveRequest request) {
        entity.setHotelName(cleanRequired(request.hotelName()));
        entity.setProvince(clean(request.province()));
        entity.setCity(clean(request.city()));
        entity.setDistrict(clean(request.district()));
        entity.setArea(clean(request.area()));
        entity.setAddress(clean(request.address()));
        entity.setStarStandard(clean(request.starStandard()));
        entity.setRoomType(clean(request.roomType()));
        entity.setSourceName(clean(request.sourceName()));
        entity.setPurchasePrice(money(request.purchasePrice()));
        entity.setAgreementPrice(money(request.agreementPrice()));
        entity.setPriceUnit(StringUtils.hasText(request.priceUnit()) ? clean(request.priceUnit()) : "间夜");
        entity.setValidFrom(request.validFrom());
        entity.setValidTo(request.validTo());
        entity.setContactName(clean(request.contactName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setStatus(ControlledRoomResourceStatus.fromValueOrDefault(request.status()).getValue());
        entity.setRemark(clean(request.remark()));
    }

    private void applyRoomTypeFields(ControlledRoomTypeEntity entity, ControlledRoomTypeSaveRequest request) {
        entity.setResourceId(request.resourceId());
        entity.setRoomType(cleanRequired(request.roomType()));
        entity.setBedType(clean(request.bedType()));
        entity.setCapacity(number(request.capacity()));
        entity.setPurchasePrice(money(request.purchasePrice()));
        entity.setAgreementPrice(money(request.agreementPrice()));
        entity.setPriceUnit(StringUtils.hasText(request.priceUnit()) ? clean(request.priceUnit()) : "间夜");
        entity.setStatus(StringUtils.hasText(request.status()) ? clean(request.status()) : "active");
        entity.setRemark(clean(request.remark()));
    }

    private void applyRoomFields(
            ControlledRoomUnitEntity entity,
            ControlledRoomUnitSaveRequest request,
            ControlledRoomResourceEntity resource
    ) {
        entity.setResourceId(request.resourceId());
        entity.setBuildingName(clean(request.buildingName()));
        entity.setFloorNo(clean(request.floorNo()));
        entity.setRoomNo(cleanRequired(request.roomNo()));
        entity.setRoomType(clean(request.roomType()));
        entity.setBedType(clean(request.bedType()));
        entity.setCapacity(number(request.capacity()));
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : ControlledRoomUnitStatus.ACTIVE.getValue());
        entity.setRemark(clean(request.remark()));
    }

    private List<ControlledRoomUnitResponse> toRoomResponses(Long tenantId, List<ControlledRoomUnitEntity> rooms) {
        Map<Long, ControlledRoomResourceEntity> resources = loadResources(tenantId, rooms.stream()
                .map(ControlledRoomUnitEntity::getResourceId)
                .toList());
        return rooms.stream()
                .map(room -> roomResponse(room, resources.get(room.getResourceId())))
                .toList();
    }

    private ControlledRoomUnitResponse roomResponse(ControlledRoomUnitEntity room, ControlledRoomResourceEntity resource) {
        return ControlledRoomUnitResponse.fromEntity(
                room,
                resource == null ? null : resource.getHotelName(),
                resource == null ? null : resource.getStarStandard()
        );
    }

    private List<ControlledRoomTypeResponse> toRoomTypeResponses(Long tenantId, List<ControlledRoomTypeEntity> roomTypes) {
        Map<Long, ControlledRoomResourceEntity> resources = loadResources(tenantId, roomTypes.stream()
                .map(ControlledRoomTypeEntity::getResourceId)
                .toList());
        return roomTypes.stream()
                .map(type -> ControlledRoomTypeResponse.fromEntity(
                        type,
                        resources.get(type.getResourceId()) == null ? null : resources.get(type.getResourceId()).getHotelName()
                ))
                .toList();
    }

    private List<ControlledRoomDayStatusResponse> toDayStatusResponses(
            Long tenantId,
            List<ControlledRoomDayStatusEntity> statuses,
            String starStandard
    ) {
        Map<Long, ControlledRoomResourceEntity> resources = loadResources(tenantId, statuses.stream()
                .map(ControlledRoomDayStatusEntity::getResourceId)
                .toList());
        Map<Long, ControlledRoomUnitEntity> rooms = loadRooms(tenantId, statuses.stream()
                .map(ControlledRoomDayStatusEntity::getRoomId)
                .toList());
        return statuses.stream()
                .filter(status -> {
                    ControlledRoomResourceEntity resource = resources.get(status.getResourceId());
                    return !StringUtils.hasText(starStandard)
                            || (resource != null && starStandard.equals(resource.getStarStandard()));
                })
                .map(status -> {
                    ControlledRoomResourceEntity resource = resources.get(status.getResourceId());
                    ControlledRoomUnitEntity room = rooms.get(status.getRoomId());
                    return ControlledRoomDayStatusResponse.fromEntity(
                            status,
                            resource == null ? null : resource.getHotelName(),
                            room == null ? null : room.getRoomNo(),
                            room == null ? null : room.getRoomType(),
                            resource == null ? null : resource.getStarStandard()
                    );
                })
                .toList();
    }

    private List<ControlledRoomLockRecordResponse> toLockResponses(Long tenantId, List<ControlledRoomLockRecordEntity> locks) {
        Map<Long, ControlledRoomResourceEntity> resources = loadResources(tenantId, locks.stream()
                .map(ControlledRoomLockRecordEntity::getResourceId)
                .toList());
        Map<Long, ControlledRoomUnitEntity> rooms = loadRooms(tenantId, locks.stream()
                .map(ControlledRoomLockRecordEntity::getRoomId)
                .toList());
        return locks.stream()
                .map(lock -> lockResponse(lock, getNullable(resources, lock.getResourceId()), getNullable(rooms, lock.getRoomId())))
                .toList();
    }

    private ControlledRoomLockRecordResponse lockResponse(
            ControlledRoomLockRecordEntity lock,
            ControlledRoomResourceEntity resource,
            ControlledRoomUnitEntity room
    ) {
        return ControlledRoomLockRecordResponse.fromEntity(
                lock,
                resource == null ? null : resource.getHotelName(),
                room == null ? null : room.getRoomNo(),
                room == null ? lock.getRoomType() : room.getRoomType(),
                resource == null ? null : resource.getStarStandard()
        );
    }

    private Map<Long, ControlledRoomResourceEntity> loadResources(Long tenantId, List<Long> ids) {
        List<Long> distinctIds = ids.stream().filter(id -> id != null).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return resourceMapper.selectList(baseQuery(tenantId).in("id", distinctIds)).stream()
                .collect(Collectors.toMap(ControlledRoomResourceEntity::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, ControlledRoomUnitEntity> loadRooms(Long tenantId, List<Long> ids) {
        List<Long> distinctIds = ids.stream().filter(id -> id != null).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return roomMapper.selectList(roomBaseQuery(tenantId).in("id", distinctIds)).stream()
                .collect(Collectors.toMap(ControlledRoomUnitEntity::getId, Function.identity(), (left, right) -> left));
    }

    /**
     * 安全读取关联资料。按房型数量锁房时不会选择具体房号，roomId 允许为空；
     * 直接对不可变空 Map 调用 get(null) 会触发 NullPointerException，导致锁房记录列表 500。
     */
    private <T> T getNullable(Map<Long, T> values, Long id) {
        return id == null ? null : values.get(id);
    }

    private QueryWrapper<ControlledRoomUnitEntity> roomBaseQuery(Long tenantId) {
        return new QueryWrapper<ControlledRoomUnitEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ControlledRoomUnitEntity> roomBaseUpdate(Long tenantId) {
        return new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ControlledRoomUnitEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<ControlledRoomTypeEntity> roomTypeBaseQuery(Long tenantId) {
        return new QueryWrapper<ControlledRoomTypeEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ControlledRoomTypeEntity> roomTypeBaseUpdate(Long tenantId) {
        return new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ControlledRoomTypeEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<RoomInventoryEntity> inventoryBaseQuery(Long tenantId) {
        return new QueryWrapper<RoomInventoryEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<RoomInventoryEntity> inventoryBaseUpdate(Long tenantId) {
        return new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<RoomInventoryEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<ControlledRoomDayStatusEntity> dayStatusBaseQuery(Long tenantId) {
        return new QueryWrapper<ControlledRoomDayStatusEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ControlledRoomDayStatusEntity> dayStatusBaseUpdate(Long tenantId) {
        return new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ControlledRoomDayStatusEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<ControlledRoomLockRecordEntity> lockBaseQuery(Long tenantId) {
        return new QueryWrapper<ControlledRoomLockRecordEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ControlledRoomLockRecordEntity> lockBaseUpdate(Long tenantId) {
        return new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ControlledRoomLockRecordEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    @Override
    protected ControlledRoomResourceEntity newEntity() {
        return new ControlledRoomResourceEntity();
    }

    @Override
    protected ControlledRoomResourceResponse toResponse(ControlledRoomResourceEntity entity) {
        return ControlledRoomResourceResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "自控房源不存在或已删除";
    }

    /**
     * 房态库存来源快照。
     *
     * <p>生成库存和锁房时把酒店、供应商、房型等展示字段固化为快照，
     * 后续即使基础资料改名，历史排房记录仍能看懂。</p>
     */
    private record SourceSnapshot(
            String sourceType,
            Long sourceId,
            Long roomTypeId,
            String hotelName,
            String supplierName,
            String roomType,
            String starStandard
    ) {
    }
}

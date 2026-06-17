package com.mtravel.platform.dispatch.roomstatus.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
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
import com.mtravel.platform.dispatch.roomstatus.service.ControlledRoomStatusService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自控房源与房态库存接口。
 *
 * <p>Controller 只负责接收参数、解析租户和操作人。房源有效期、星钻标准匹配、锁房状态流转、
 * 软删除保护等业务规则统一放在 Service。</p>
 */
@Validated
@RestController
@RequestMapping("/dispatch/room-status")
public class ControlledRoomStatusController extends ControllerSupport {

    private final ControlledRoomStatusService service;

    public ControlledRoomStatusController(
            ControlledRoomStatusService service,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
    }

    /** 分页查询自控房源批次。 */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/resources/page")
    public ApiResponse<PageResult<ControlledRoomResourceResponse>> pageResources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String starStandard,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.pageResources(
                currentTenantId(), keyword, province, city, district, starStandard, roomType, status, page, pageSize));
    }

    /** 查询自控房源下拉列表。 */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/resources/all")
    public ApiResponse<List<ControlledRoomResourceResponse>> allResources(
            @RequestParam(defaultValue = "false") Boolean includeDisabled
    ) {
        return ApiResponse.ok(service.listResources(currentTenantId(), Boolean.TRUE.equals(includeDisabled)));
    }

    /** 查询自控房源详情。 */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/resources/detail")
    public ApiResponse<ControlledRoomResourceResponse> resourceDetail(@RequestParam Long id) {
        return ApiResponse.ok(service.detail(id, currentTenantId()));
    }

    /** 新增自控房源批次。 */
    @OperationLog(module = "计调操作", type = "新增")
    @PostMapping("/resources/create")
    public ApiResponse<ControlledRoomResourceResponse> createResource(
            @Valid @RequestBody ControlledRoomResourceSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.createResource(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改自控房源批次。 */
    @OperationLog(module = "计调操作", type = "修改")
    @PostMapping("/resources/update")
    public ApiResponse<ControlledRoomResourceResponse> updateResource(
            @RequestParam Long id,
            @Valid @RequestBody ControlledRoomResourceSaveRequest request
    ) {
        return ApiResponse.ok(service.updateResource(id, request, currentTenantId()));
    }

    /** 软删除自控房源批次。 */
    @OperationLog(module = "计调操作", type = "删除")
    @PostMapping("/resources/delete")
    public ApiResponse<Void> deleteResource(@RequestParam Long id, Authentication authentication) {
        service.delete(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 分页查询自控房间明细。 */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/rooms/page")
    public ApiResponse<PageResult<ControlledRoomUnitResponse>> pageRooms(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.pageRooms(currentTenantId(), resourceId, keyword, status, page, pageSize));
    }

    /** 新增自控房间。 */
    @OperationLog(module = "计调操作", type = "新增")
    @PostMapping("/rooms/create")
    public ApiResponse<ControlledRoomUnitResponse> createRoom(
            @Valid @RequestBody ControlledRoomUnitSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.createRoom(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改自控房间。 */
    @OperationLog(module = "计调操作", type = "修改")
    @PostMapping("/rooms/update")
    public ApiResponse<ControlledRoomUnitResponse> updateRoom(
            @RequestParam Long id,
            @Valid @RequestBody ControlledRoomUnitSaveRequest request
    ) {
        return ApiResponse.ok(service.updateRoom(id, request, currentTenantId()));
    }

    /** 软删除自控房间。 */
    @OperationLog(module = "计调操作", type = "删除")
    @PostMapping("/rooms/delete")
    public ApiResponse<Void> deleteRoom(@RequestParam Long id, Authentication authentication) {
        service.deleteRoom(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 分页查询自营房型。 */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/room-types/page")
    public ApiResponse<PageResult<ControlledRoomTypeResponse>> pageRoomTypes(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.pageRoomTypes(currentTenantId(), resourceId, keyword, status, page, pageSize));
    }

    /** 查询自营房型下拉列表。 */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/room-types/all")
    public ApiResponse<List<ControlledRoomTypeResponse>> allRoomTypes(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(defaultValue = "false") Boolean includeDisabled
    ) {
        return ApiResponse.ok(service.listRoomTypes(currentTenantId(), resourceId, Boolean.TRUE.equals(includeDisabled)));
    }

    /** 新增自营房型。 */
    @OperationLog(module = "计调操作", type = "新增")
    @PostMapping("/room-types/create")
    public ApiResponse<ControlledRoomTypeResponse> createRoomType(
            @Valid @RequestBody ControlledRoomTypeSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.createRoomType(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 修改自营房型。 */
    @OperationLog(module = "计调操作", type = "修改")
    @PostMapping("/room-types/update")
    public ApiResponse<ControlledRoomTypeResponse> updateRoomType(
            @RequestParam Long id,
            @Valid @RequestBody ControlledRoomTypeSaveRequest request
    ) {
        return ApiResponse.ok(service.updateRoomType(id, request, currentTenantId()));
    }

    /** 软删除自营房型。 */
    @OperationLog(module = "计调操作", type = "删除")
    @PostMapping("/room-types/delete")
    public ApiResponse<Void> deleteRoomType(@RequestParam Long id, Authentication authentication) {
        service.deleteRoomType(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 批量生成房态日历。 */
    @OperationLog(module = "计调操作", type = "新增")
    @PostMapping("/calendar/generate")
    public ApiResponse<Integer> generateCalendar(
            @Valid @RequestBody ControlledRoomGenerateStatusRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.generateDayStatuses(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 查询房态日历。 */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/calendar")
    public ApiResponse<List<ControlledRoomDayStatusResponse>> calendar(
            @RequestParam(required = false) Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String starStandard,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(service.calendar(currentTenantId(), resourceId, startDate, endDate, starStandard, status));
    }

    /** 按来源、房型和日期生成聚合库存。 */
    @OperationLog(module = "计调操作", type = "新增")
    @PostMapping("/inventories/generate")
    public ApiResponse<Integer> generateInventories(
            @Valid @RequestBody RoomInventoryGenerateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.generateInventories(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 查询聚合房态库存。 */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/inventories/calendar")
    public ApiResponse<List<RoomInventoryCalendarResponse>> inventoryCalendar(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(service.inventoryCalendar(
                currentTenantId(), sourceType, sourceId, roomTypeId, startDate, endDate, status));
    }

    /** 按房型数量锁房。 */
    @OperationLog(module = "计调操作", type = "修改")
    @PostMapping("/inventories/locks/create")
    public ApiResponse<ControlledRoomLockRecordResponse> createInventoryLock(
            @Valid @RequestBody RoomInventoryLockRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.lockInventory(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 查询房态库存占用明细。 */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/inventories/occupancy")
    public ApiResponse<List<RoomInventoryOccupancyResponse>> inventoryOccupancy(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) String roomType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stayDate
    ) {
        return ApiResponse.ok(service.inventoryOccupancy(
                currentTenantId(), sourceType, sourceId, roomTypeId, roomType, stayDate));
    }

    /** 锁定自控房间。 */
    @OperationLog(module = "计调操作", type = "修改")
    @PostMapping("/locks/create")
    public ApiResponse<List<ControlledRoomLockRecordResponse>> createLock(
            @Valid @RequestBody ControlledRoomLockRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.lockRooms(request, currentTenantId(), currentOperator(authentication)));
    }

    /** 释放锁房。 */
    @OperationLog(module = "计调操作", type = "修改")
    @PostMapping("/locks/release")
    public ApiResponse<Void> releaseLock(@RequestParam Long id, Authentication authentication) {
        service.releaseLock(id, currentTenantId(), currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 分页查询锁房流水。 */
    @OperationLog(module = "计调操作", type = "查询")
    @GetMapping("/locks/page")
    public ApiResponse<PageResult<ControlledRoomLockRecordResponse>> pageLocks(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String teamNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize
    ) {
        return ApiResponse.ok(service.pageLocks(currentTenantId(), resourceId, teamNo, status, page, pageSize));
    }
}

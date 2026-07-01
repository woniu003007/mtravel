package com.mtravel.platform.dispatch.vehiclequote.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteCalculateRequest;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteCalculateResponse;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteRuleResponse;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteRuleSaveRequest;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteRuleSnapshotResponse;
import com.mtravel.platform.dispatch.vehiclequote.entity.VehicleQuoteRuleEntity;
import com.mtravel.platform.dispatch.vehiclequote.mapper.VehicleQuoteRuleMapper;
import com.mtravel.platform.enterprise.expenseitem.entity.EnterpriseExpenseItemEntity;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 座位数报价规则服务。
 *
 * <p>该服务维护车队报价测算规则，并根据车辆座位数和路书公里输出参考价。测算结果只是产品模板和询价时的参考，
 * 不直接生成正式派车成本。</p>
 */
@Service
public class VehicleQuoteRuleService extends BusinessCrudService<VehicleQuoteRuleEntity, VehicleQuoteRuleResponse> {

    private static final String VEHICLE_RESOURCE_TYPE = "vehicle";
    private static final String ACTIVE_STATUS = "active";

    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    private final VehicleQuoteRuleMapper mapper;
    private final EnterpriseExpenseItemMapper expenseItemMapper;

    public VehicleQuoteRuleService(VehicleQuoteRuleMapper mapper, EnterpriseExpenseItemMapper expenseItemMapper) {
        super(mapper);
        this.mapper = mapper;
        this.expenseItemMapper = expenseItemMapper;
    }

    /**
     * 分页查询座位数报价规则。
     *
     * @param tenantId 当前租户 ID
     * @param keyword 座位数关键字
     * @param vehicleType 座位数筛选，字段名沿用 vehicleType 兼容接口
     * @param status 状态筛选
     * @param city 预留城市筛选，当前页面不启用
     * @param page 当前页码
     * @param pageSize 每页数量
     * @return 规则分页结果
     */
    public PageResult<VehicleQuoteRuleResponse> page(
            Long tenantId,
            String keyword,
            String vehicleType,
            String status,
            String city,
            long page,
            long pageSize
    ) {
        QueryWrapper<VehicleQuoteRuleEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(vehicleType), "vehicle_type", clean(vehicleType))
                .eq(StringUtils.hasText(status), "status", clean(status))
                .eq(StringUtils.hasText(city), "city", clean(city))
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("vehicle_type", clean(keyword))
                        .or()
                        .like("remark", clean(keyword)))
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /**
     * 查询启用报价规则，供前端测算和下拉选择。
     *
     * @param tenantId 当前租户 ID
     * @param vehicleType 座位数筛选，可为空
     * @return 启用规则列表
     */
    public List<VehicleQuoteRuleResponse> listActive(Long tenantId, String vehicleType) {
        return mapper.selectList(baseQuery(tenantId)
                        .eq("status", "active")
                        .eq(StringUtils.hasText(vehicleType), "vehicle_type", clean(vehicleType))
                        .orderByAsc("vehicle_type")
                        .orderByAsc("province")
                        .orderByAsc("city")
                        .orderByAsc("district"))
                .stream()
                .map(VehicleQuoteRuleResponse::fromEntity)
                .toList();
    }

    /**
     * 新增座位数报价规则。
     *
     * @param request 保存请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     * @return 新增后的规则
     */
    public VehicleQuoteRuleResponse create(VehicleQuoteRuleSaveRequest request, Long tenantId, String operator) {
        assertDuplicateRule(tenantId, request, null);
        VehicleQuoteRuleEntity entity = new VehicleQuoteRuleEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        syncVehicleExpenseItemForActiveRule(request, tenantId, operator);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改座位数报价规则。
     *
     * @param id 规则 ID
     * @param request 保存请求
     * @param tenantId 当前租户 ID
     * @return 修改后的规则
     */
    public VehicleQuoteRuleResponse update(Long id, VehicleQuoteRuleSaveRequest request, Long tenantId, String operator) {
        assertDuplicateRule(tenantId, request, id);
        VehicleQuoteRuleEntity entity = new VehicleQuoteRuleEntity();
        applyFields(entity, request);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        syncVehicleExpenseItemForActiveRule(request, tenantId, operator);
        return detail(id, tenantId);
    }

    /**
     * 根据座位数和距离测算用车参考价。
     *
     * <p>当前版本先不启用地区维度，只按座位数匹配报价规则；路书公里来自产品行程路书。</p>
     *
     * @param request 测算请求
     * @param tenantId 当前租户 ID
     * @return 测算结果和命中规则快照
     */
    public VehicleQuoteCalculateResponse calculate(VehicleQuoteCalculateRequest request, Long tenantId) {
        VehicleQuoteRuleEntity rule = findBestRule(request, tenantId);
        if (rule == null) {
            throw new BizException("未找到匹配的座位数报价规则");
        }
        BigDecimal distanceKilometers = metersToKilometers(request.distanceMeters());
        BigDecimal baseKilometers = money(rule.getBaseKilometers());
        BigDecimal extraKilometers = distanceKilometers.subtract(baseKilometers).max(BigDecimal.ZERO);
        BigDecimal rawAmount = money(rule.getBasePrice())
                .add(extraKilometers.multiply(money(rule.getExtraKilometerPrice())))
                .multiply(defaultFloatRate(rule.getFloatRate()));
        BigDecimal calculatedAmount = rawAmount.max(money(rule.getMinimumPrice())).setScale(2, RoundingMode.HALF_UP);
        return new VehicleQuoteCalculateResponse(
                rule.getVehicleType(),
                request.distanceMeters() == null ? 0 : request.distanceMeters(),
                distanceKilometers,
                calculatedAmount,
                VehicleQuoteRuleSnapshotResponse.fromEntity(rule)
        );
    }

    /** 按座位数查找启用报价规则；地区字段当前作为预留字段，不参与匹配。 */
    private VehicleQuoteRuleEntity findBestRule(VehicleQuoteCalculateRequest request, Long tenantId) {
        QueryWrapper<VehicleQuoteRuleEntity> wrapper = baseQuery(tenantId)
                .eq("status", "active")
                .eq("vehicle_type", cleanRequired(request.vehicleType()))
                .last("LIMIT 1");
        return mapper.selectOne(wrapper);
    }

    /** 同一租户、同一座位数当前只维护一条未删除规则，地区维度后续启用时再纳入唯一范围。 */
    private void assertDuplicateRule(Long tenantId, VehicleQuoteRuleSaveRequest request, Long excludeId) {
        QueryWrapper<VehicleQuoteRuleEntity> wrapper = baseQuery(tenantId)
                .eq("vehicle_type", cleanRequired(request.vehicleType()))
                .ne(excludeId != null, "id", excludeId);
        Long count = mapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("同座位数报价规则已存在");
        }
    }

    /** 将保存请求写入实体，并补齐默认价格和状态。 */
    private void applyFields(VehicleQuoteRuleEntity entity, VehicleQuoteRuleSaveRequest request) {
        entity.setVehicleType(cleanRequired(request.vehicleType()));
        // 当前报价规则先不启用地区，保留字段但统一写空，避免用户误以为同座位数可按地区维护多套价格。
        entity.setProvince(null);
        entity.setCity(null);
        entity.setDistrict(null);
        entity.setBasePrice(money(request.basePrice()));
        entity.setBaseKilometers(money(request.baseKilometers()));
        entity.setExtraKilometerPrice(money(request.extraKilometerPrice()));
        entity.setMinimumPrice(money(request.minimumPrice()));
        entity.setFloatRate(defaultFloatRate(request.floatRate()));
        entity.setStatus(StringUtils.hasText(request.status()) ? clean(request.status()) : "active");
        entity.setRemark(clean(request.remark()));
    }

    /**
     * 启用的座位数报价规则要同步生成同名车队费用项目，保证用车弹窗座位数和价格信息项目可对齐。
     */
    private void syncVehicleExpenseItemForActiveRule(VehicleQuoteRuleSaveRequest request, Long tenantId, String operator) {
        if (!isActiveRule(request)) {
            return;
        }
        String vehicleType = cleanRequired(request.vehicleType());
        EnterpriseExpenseItemEntity existing = expenseItemMapper.selectOne(new QueryWrapper<EnterpriseExpenseItemEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_type", VEHICLE_RESOURCE_TYPE)
                .eq("project_name", vehicleType)
                .last("LIMIT 1"));
        if (existing == null) {
            EnterpriseExpenseItemEntity entity = new EnterpriseExpenseItemEntity();
            entity.setTenantId(tenantId);
            entity.setResourceType(VEHICLE_RESOURCE_TYPE);
            entity.setProjectName(vehicleType);
            entity.setStatisticsEnabled(Boolean.TRUE);
            entity.setSortOrder(vehicleExpenseSortOrder(vehicleType));
            entity.setStatus(ACTIVE_STATUS);
            entity.setCreatedBy(StringUtils.hasText(operator) ? operator : "system");
            entity.setIsDeleted(false);
            entity.setRemark("由座位数报价规则自动补齐");
            expenseItemMapper.insert(entity);
            return;
        }
        if (!ACTIVE_STATUS.equals(existing.getStatus())) {
            EnterpriseExpenseItemEntity entity = new EnterpriseExpenseItemEntity();
            entity.setStatus(ACTIVE_STATUS);
            expenseItemMapper.update(entity, new UpdateWrapper<EnterpriseExpenseItemEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("id", existing.getId())
                    .eq("is_deleted", false));
        }
    }

    /** 报价规则状态为空时按启用处理，与保存默认值保持一致。 */
    private boolean isActiveRule(VehicleQuoteRuleSaveRequest request) {
        return !StringUtils.hasText(request.status()) || ACTIVE_STATUS.equals(clean(request.status()));
    }

    /** 费用项目排序按座位数字靠前排列；无法解析数字的自定义值放在末尾。 */
    private int vehicleExpenseSortOrder(String vehicleType) {
        String numericText = String.valueOf(vehicleType).replaceAll("\\D+", "");
        if (!StringUtils.hasText(numericText)) {
            return 999;
        }
        try {
            return Integer.parseInt(numericText);
        } catch (NumberFormatException ignored) {
            return 999;
        }
    }

    /** 把米转换成公里并保留两位，供前端回显和金额计算使用。 */
    private BigDecimal metersToKilometers(Integer distanceMeters) {
        int meters = distanceMeters == null ? 0 : Math.max(distanceMeters, 0);
        return new BigDecimal(meters).divide(ONE_THOUSAND, 2, RoundingMode.HALF_UP);
    }

    /** 空浮动系数按 1 处理。 */
    private BigDecimal defaultFloatRate(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : value;
    }

    @Override
    protected VehicleQuoteRuleEntity newEntity() {
        return new VehicleQuoteRuleEntity();
    }

    @Override
    protected VehicleQuoteRuleResponse toResponse(VehicleQuoteRuleEntity entity) {
        return VehicleQuoteRuleResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "座位数报价规则不存在或已删除";
    }
}

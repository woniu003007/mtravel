package com.mtravel.platform.customer.creditrule.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.enums.CustomerCategoryStatus;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.customer.creditrule.dto.CustomerCreditRuleResponse;
import com.mtravel.platform.customer.creditrule.dto.CustomerCreditRuleSaveRequest;
import com.mtravel.platform.customer.creditrule.entity.CustomerCreditRuleEntity;
import com.mtravel.platform.customer.creditrule.enums.CustomerCreditRuleStatus;
import com.mtravel.platform.customer.creditrule.mapper.CustomerCreditRuleMapper;
import com.mtravel.platform.enterprise.employee.entity.EnterpriseEmployeeEntity;
import com.mtravel.platform.enterprise.employee.enums.EnterpriseEmployeeStatus;
import com.mtravel.platform.enterprise.employee.mapper.EnterpriseEmployeeMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 客户授信规则服务。
 *
 * <p>该服务按客户等级维护默认授信规则，并负责租户隔离、软删除、客户等级和员工有效性校验。
 * 分页回显的客户等级、审批人和抄送人均通过批量查询补齐，避免规则列表产生 N+1 查询。</p>
 */
@Service
public class CustomerCreditRuleService extends BusinessCrudService<CustomerCreditRuleEntity, CustomerCreditRuleResponse> {

    private static final int EMPLOYEE_ID_TEXT_MAX_LENGTH = 1000;

    private final CustomerCreditRuleMapper mapper;
    private final CustomerCategoryMapper categoryMapper;
    private final EnterpriseEmployeeMapper employeeMapper;

    public CustomerCreditRuleService(
            CustomerCreditRuleMapper mapper,
            CustomerCategoryMapper categoryMapper,
            EnterpriseEmployeeMapper employeeMapper
    ) {
        super(mapper);
        this.mapper = mapper;
        this.categoryMapper = categoryMapper;
        this.employeeMapper = employeeMapper;
    }

    /**
     * 分页查询客户授信规则。
     *
     * <p>列表固定按授信额度升序、主键升序排列，便于等级额度从低到高展示。</p>
     *
     * @param tenantId 当前租户 ID
     * @param keyword 备注关键字，可为空
     * @param customerLevelId 客户等级筛选，可为空
     * @param status 状态筛选，可为空
     * @param page 当前页
     * @param pageSize 每页数量
     * @return 授信规则分页结果
     */
    public PageResult<CustomerCreditRuleResponse> page(
            Long tenantId,
            String keyword,
            Long customerLevelId,
            String status,
            long page,
            long pageSize
    ) {
        CustomerCreditRuleStatus parsedStatus = parseNullableStatus(status);
        QueryWrapper<CustomerCreditRuleEntity> wrapper = baseQuery(tenantId)
                .eq(customerLevelId != null, "customer_level_id", customerLevelId)
                .eq(parsedStatus != null, "status", parsedStatus == null ? null : parsedStatus.getValue())
                .like(StringUtils.hasText(keyword), "remark", clean(keyword))
                .orderByAsc("credit_limit")
                .orderByAsc("id");
        Page<CustomerCreditRuleEntity> result = mapper.selectPage(Page.of(page, pageSize), wrapper);
        return toPageResponse(tenantId, result);
    }

    /**
     * 查询单条授信规则详情。
     *
     * @param id 规则 ID
     * @param tenantId 当前租户 ID
     * @return 带客户等级和员工名称的规则
     */
    @Override
    public CustomerCreditRuleResponse detail(Long id, Long tenantId) {
        CustomerCreditRuleEntity entity = mapper.selectOne(baseQuery(tenantId).eq("id", id));
        if (entity == null) {
            throw new BizException(notFoundMessage());
        }
        return toResponse(entity, customerLevelNameMap(tenantId, List.of(entity)), employeeNameMap(tenantId, List.of(entity)));
    }

    /**
     * 新增客户授信规则。
     *
     * <p>同一租户、同一未删除客户等级只能保留一条规则；审批和抄送员工必须属于当前租户且处于启用状态。</p>
     *
     * @param request 保存请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     * @return 新增后的规则
     */
    public CustomerCreditRuleResponse create(CustomerCreditRuleSaveRequest request, Long tenantId, String operator) {
        CreditRuleFields fields = fields(request);
        assertCustomerLevelActive(tenantId, request.customerLevelId());
        List<Long> approverIds = normalizeEmployeeIds(request.approverEmployeeIds(), "审批员工");
        List<Long> ccIds = normalizeEmployeeIds(request.ccEmployeeIds(), "抄送员工");
        assertEmployeesActive(tenantId, distinctEmployeeIds(approverIds, ccIds));
        assertDuplicateRule(tenantId, request.customerLevelId(), null);

        CustomerCreditRuleEntity entity = new CustomerCreditRuleEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request.customerLevelId(), approverIds, ccIds, fields);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改客户授信规则。
     *
     * <p>使用 UpdateWrapper 明确覆盖每个可编辑字段，使空备注和空人员列表也能清除旧值。</p>
     *
     * @param id 规则 ID
     * @param request 保存请求
     * @param tenantId 当前租户 ID
     * @return 修改后的规则
     */
    public CustomerCreditRuleResponse update(Long id, CustomerCreditRuleSaveRequest request, Long tenantId) {
        CreditRuleFields fields = fields(request);
        assertCustomerLevelActive(tenantId, request.customerLevelId());
        List<Long> approverIds = normalizeEmployeeIds(request.approverEmployeeIds(), "审批员工");
        List<Long> ccIds = normalizeEmployeeIds(request.ccEmployeeIds(), "抄送员工");
        assertEmployeesActive(tenantId, distinctEmployeeIds(approverIds, ccIds));
        assertDuplicateRule(tenantId, request.customerLevelId(), id);

        int updated = mapper.update(null, baseUpdate(tenantId)
                .eq("id", id)
                .set("customer_level_id", request.customerLevelId())
                .set("credit_limit", fields.creditLimit())
                .set("account_period_days", fields.accountPeriodDays())
                .set("allow_over_limit", fields.allowOverLimit())
                .set("approver_employee_ids", serializeEmployeeIds(approverIds))
                .set("cc_employee_ids", serializeEmployeeIds(ccIds))
                .set("status", fields.status().getValue())
                .set("remark", fields.remark()));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /** 将经过校验的保存字段写入新增实体。 */
    private void applyFields(
            CustomerCreditRuleEntity entity,
            Long customerLevelId,
            List<Long> approverIds,
            List<Long> ccIds,
            CreditRuleFields fields
    ) {
        entity.setCustomerLevelId(customerLevelId);
        entity.setCreditLimit(fields.creditLimit());
        entity.setAccountPeriodDays(fields.accountPeriodDays());
        entity.setAllowOverLimit(fields.allowOverLimit());
        entity.setApproverEmployeeIds(serializeEmployeeIds(approverIds));
        entity.setCcEmployeeIds(serializeEmployeeIds(ccIds));
        entity.setStatus(fields.status().getValue());
        entity.setRemark(fields.remark());
    }

    /** 规范保存字段，并在 Service 入口补足非负金额和账期校验。 */
    private CreditRuleFields fields(CustomerCreditRuleSaveRequest request) {
        return new CreditRuleFields(
                nonNegativeMoney(request.creditLimit(), "授信额度不能小于0"),
                nonNegativeDays(request.paymentTermDays()),
                Boolean.TRUE.equals(request.allowOverLimit()),
                CustomerCreditRuleStatus.fromValueOrDefault(request.status()),
                clean(request.remark())
        );
    }

    /** 客户等级必须属于当前租户、未删除且处于启用状态，不能将规则挂到其它租户的等级。 */
    private void assertCustomerLevelActive(Long tenantId, Long customerLevelId) {
        CustomerCategoryEntity level = categoryMapper.selectOne(categoryBaseQuery(tenantId)
                .eq("id", customerLevelId));
        if (level == null) {
            throw new BizException("客户等级不存在或已删除");
        }
        if (!CustomerCategoryStatus.ACTIVE.getValue().equals(level.getStatus())) {
            throw new BizException("客户等级已停用");
        }
    }

    /** 同一租户、同一客户等级只能维护一条未删除授信规则，与数据库唯一索引保持一致。 */
    private void assertDuplicateRule(Long tenantId, Long customerLevelId, Long excludeId) {
        Long count = mapper.selectCount(baseQuery(tenantId)
                .eq("customer_level_id", customerLevelId)
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("该客户等级的授信规则已存在");
        }
    }

    /** 一次查询验证全部审批和抄送员工，避免按人员逐条查询。 */
    private void assertEmployeesActive(Long tenantId, Collection<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return;
        }
        Set<Long> activeIds = employeeMapper.selectList(new QueryWrapper<EnterpriseEmployeeEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("status", EnterpriseEmployeeStatus.ACTIVE.getValue())
                        .in("id", employeeIds))
                .stream()
                .map(EnterpriseEmployeeEntity::getId)
                .collect(Collectors.toSet());
        if (!activeIds.containsAll(employeeIds)) {
            throw new BizException("审批或抄送员工不存在、已删除或已停用");
        }
    }

    /** 将请求中的人员 ID 规范为去重且保持原顺序的列表。 */
    private List<Long> normalizeEmployeeIds(List<Long> employeeIds, String fieldName) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long employeeId : employeeIds) {
            if (employeeId == null || employeeId <= 0) {
                throw new BizException(fieldName + "不合法");
            }
            normalized.add(employeeId);
        }
        String serialized = serializeEmployeeIds(normalized);
        if (serialized.length() > EMPLOYEE_ID_TEXT_MAX_LENGTH) {
            throw new BizException(fieldName + "数量过多");
        }
        return List.copyOf(normalized);
    }

    /** 按数据库的 varchar 协议把有序员工 ID 列表转换为英文逗号分隔文本。 */
    private String serializeEmployeeIds(Collection<Long> employeeIds) {
        return employeeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /** 从数据库员工 ID 文本恢复 JSON DTO 所需的列表；异常旧值被忽略以保证历史规则可读取。 */
    private List<Long> parseEmployeeIds(String serialized) {
        if (!StringUtils.hasText(serialized)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : serialized.split(",")) {
            try {
                long id = Long.parseLong(part.trim());
                if (id > 0) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
                // 历史脏数据不应阻断规则列表；保存时会重新按合法 ID 格式规范化。
            }
        }
        return List.copyOf(ids);
    }

    /** 合并审批和抄送员工 ID，供单次有效性校验使用。 */
    private Collection<Long> distinctEmployeeIds(List<Long> approverIds, List<Long> ccIds) {
        return Stream.concat(approverIds.stream(), ccIds.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 为当前页规则一次性查询客户等级名称。 */
    private Map<Long, String> customerLevelNameMap(Long tenantId, List<CustomerCreditRuleEntity> rules) {
        List<Long> levelIds = rules.stream()
                .map(CustomerCreditRuleEntity::getCustomerLevelId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (levelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectList(categoryBaseQuery(tenantId).in("id", levelIds))
                .stream()
                .collect(Collectors.toMap(
                        CustomerCategoryEntity::getId,
                        CustomerCategoryEntity::getCategoryName,
                        (first, ignored) -> first
                ));
    }

    /** 为当前页规则一次性查询审批和抄送员工名称；停用员工仍保留名称用于历史回显。 */
    private Map<Long, String> employeeNameMap(Long tenantId, List<CustomerCreditRuleEntity> rules) {
        Set<Long> employeeIds = new LinkedHashSet<>();
        for (CustomerCreditRuleEntity rule : rules) {
            employeeIds.addAll(parseEmployeeIds(rule.getApproverEmployeeIds()));
            employeeIds.addAll(parseEmployeeIds(rule.getCcEmployeeIds()));
        }
        if (employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return employeeMapper.selectList(new QueryWrapper<EnterpriseEmployeeEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("id", employeeIds))
                .stream()
                .collect(Collectors.toMap(
                        EnterpriseEmployeeEntity::getId,
                        EnterpriseEmployeeEntity::getEmployeeName,
                        (first, ignored) -> first
                ));
    }

    /** 将分页查询结果转换为携带批量名称映射的 DTO。 */
    private PageResult<CustomerCreditRuleResponse> toPageResponse(Long tenantId, Page<CustomerCreditRuleEntity> result) {
        List<CustomerCreditRuleEntity> rules = result.getRecords();
        Map<Long, String> levelNames = customerLevelNameMap(tenantId, rules);
        Map<Long, String> employeeNames = employeeNameMap(tenantId, rules);
        List<CustomerCreditRuleResponse> items = rules.stream()
                .map(rule -> toResponse(rule, levelNames, employeeNames))
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /** 构造单条响应，并让人员名称列表保持与人员 ID 列表完全同序。 */
    private CustomerCreditRuleResponse toResponse(
            CustomerCreditRuleEntity entity,
            Map<Long, String> levelNames,
            Map<Long, String> employeeNames
    ) {
        List<Long> approverIds = parseEmployeeIds(entity.getApproverEmployeeIds());
        List<Long> ccIds = parseEmployeeIds(entity.getCcEmployeeIds());
        return CustomerCreditRuleResponse.fromEntity(
                entity,
                levelNames.get(entity.getCustomerLevelId()),
                approverIds,
                approverIds.stream().map(employeeNames::get).toList(),
                ccIds,
                ccIds.stream().map(employeeNames::get).toList()
        );
    }

    /** 客户分类查询必须始终带租户和软删除边界。 */
    private QueryWrapper<CustomerCategoryEntity> categoryBaseQuery(Long tenantId) {
        return new QueryWrapper<CustomerCategoryEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    /** 页面状态参数为空时不筛选，非空时由枚举校验。 */
    private CustomerCreditRuleStatus parseNullableStatus(String status) {
        return StringUtils.hasText(status) ? CustomerCreditRuleStatus.fromValueOrDefault(status) : null;
    }

    /** 统一处理空金额为零，同时拦截绕过 Controller 的负数请求。 */
    private BigDecimal nonNegativeMoney(BigDecimal value, String message) {
        BigDecimal amount = money(value);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(message);
        }
        return amount;
    }

    /** 空账期按零天处理，负数账期拒绝保存。 */
    private Integer nonNegativeDays(Integer value) {
        int days = number(value);
        if (days < 0) {
            throw new BizException("账期天数不能小于0");
        }
        return days;
    }

    @Override
    protected CustomerCreditRuleEntity newEntity() {
        return new CustomerCreditRuleEntity();
    }

    @Override
    protected CustomerCreditRuleResponse toResponse(CustomerCreditRuleEntity entity) {
        return toResponse(
                entity,
                customerLevelNameMap(entity.getTenantId(), List.of(entity)),
                employeeNameMap(entity.getTenantId(), List.of(entity))
        );
    }

    @Override
    protected String notFoundMessage() {
        return "客户授信规则不存在或已删除";
    }

    /** 保存字段的内部快照，避免新增和修改出现不一致的默认值。 */
    private record CreditRuleFields(
            BigDecimal creditLimit,
            Integer accountPeriodDays,
            Boolean allowOverLimit,
            CustomerCreditRuleStatus status,
            String remark
    ) {
    }
}

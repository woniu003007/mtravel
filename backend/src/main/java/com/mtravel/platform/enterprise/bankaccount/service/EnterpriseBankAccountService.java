package com.mtravel.platform.enterprise.bankaccount.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.bankaccount.dto.EnterpriseBankAccountResponse;
import com.mtravel.platform.enterprise.bankaccount.dto.EnterpriseBankAccountSaveRequest;
import com.mtravel.platform.enterprise.bankaccount.entity.EnterpriseBankAccountEntity;
import com.mtravel.platform.enterprise.bankaccount.enums.EnterpriseBankAccountStatus;
import com.mtravel.platform.enterprise.bankaccount.mapper.EnterpriseBankAccountMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 企业银行账号业务服务。
 *
 * <p>这里集中处理租户隔离、软删除、账号唯一性、打印开关和状态默认值。
 * 银行账号后续会被收付款、备用金、员工现金账权限引用，因此不能物理删除。</p>
 */
@Service
public class EnterpriseBankAccountService extends BusinessCrudService<EnterpriseBankAccountEntity, EnterpriseBankAccountResponse> {

    private final EnterpriseBankAccountMapper mapper;

    public EnterpriseBankAccountService(EnterpriseBankAccountMapper mapper) {
        super(mapper);
        this.mapper = mapper;
    }

    /**
     * 分页查询企业银行账号。
     *
     * <p>keyword 同时查开户行、户名、账号和其它说明。status 为空时查询全部未删除账号。</p>
     */
    public PageResult<EnterpriseBankAccountResponse> page(
            Long tenantId,
            String keyword,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<EnterpriseBankAccountEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(status), "status", status)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("bank_name", keyword)
                        .or()
                        .like("account_name", keyword)
                        .or()
                        .like("account_no", keyword)
                        .or()
                        .like("other_info", keyword))
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /**
     * 查询启用银行账号列表。
     *
     * <p>该接口用于收付款、备用金付款、员工现金账权限等下拉选择，只返回 active 账号。</p>
     */
    public List<EnterpriseBankAccountResponse> listActive(Long tenantId) {
        return mapper.selectList(baseQuery(tenantId)
                        .eq("status", EnterpriseBankAccountStatus.ACTIVE.getValue())
                        .orderByAsc("bank_name")
                        .orderByAsc("account_name"))
                .stream()
                .map(EnterpriseBankAccountResponse::fromEntity)
                .toList();
    }

    /**
     * 新增企业银行账号。
     *
     * <p>同一租户下未删除账号不允许重复，避免收付款选择时出现无法区分的相同账号。</p>
     */
    public EnterpriseBankAccountResponse create(
            EnterpriseBankAccountSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertAccountNoNotExists(tenantId, request.accountNo(), null);

        EnterpriseBankAccountEntity entity = new EnterpriseBankAccountEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改企业银行账号。
     *
     * <p>修改账号时要排除当前记录再查重，避免原账号不变时误判重复。</p>
     */
    public EnterpriseBankAccountResponse update(
            Long id,
            EnterpriseBankAccountSaveRequest request,
            Long tenantId
    ) {
        assertAccountNoNotExists(tenantId, request.accountNo(), id);

        EnterpriseBankAccountEntity entity = new EnterpriseBankAccountEntity();
        applyFields(entity, request);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /**
     * 修改打印开关。
     *
     * <p>打印展示属于独立开关。这里保留独立接口，避免只想调整打印显示时误改账号资料。</p>
     */
    public void updatePrintEnabled(Long id, Long tenantId, Boolean printEnabled) {
        EnterpriseBankAccountEntity entity = new EnterpriseBankAccountEntity();
        entity.setPrintEnabled(Boolean.TRUE.equals(printEnabled));
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
    }

    private void applyFields(EnterpriseBankAccountEntity entity, EnterpriseBankAccountSaveRequest request) {
        EnterpriseBankAccountStatus status = EnterpriseBankAccountStatus.fromValueOrDefault(request.status());
        entity.setBankName(cleanRequired(request.bankName()));
        entity.setAccountName(cleanRequired(request.accountName()));
        entity.setAccountNo(cleanRequired(request.accountNo()));
        entity.setPrintEnabled(request.printEnabled() == null ? Boolean.FALSE : request.printEnabled());
        entity.setOtherInfo(clean(request.otherInfo()));
        entity.setStatus(status.getValue());
        entity.setRemark(clean(request.remark()));
    }

    private void assertAccountNoNotExists(Long tenantId, String accountNo, Long excludeId) {
        assertValueNotExists(tenantId, "account_no", cleanRequired(accountNo), excludeId, "银行账号已存在");
    }

    @Override
    protected EnterpriseBankAccountEntity newEntity() {
        return new EnterpriseBankAccountEntity();
    }

    @Override
    protected EnterpriseBankAccountResponse toResponse(EnterpriseBankAccountEntity entity) {
        return EnterpriseBankAccountResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "银行账号不存在或已删除";
    }
}

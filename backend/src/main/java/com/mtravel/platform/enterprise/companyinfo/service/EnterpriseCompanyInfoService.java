package com.mtravel.platform.enterprise.companyinfo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.enterprise.companyinfo.dto.EnterpriseCompanyInfoResponse;
import com.mtravel.platform.enterprise.companyinfo.dto.EnterpriseCompanyInfoSaveRequest;
import com.mtravel.platform.enterprise.companyinfo.entity.EnterpriseCompanyInfoEntity;
import com.mtravel.platform.enterprise.companyinfo.enums.EnterpriseCompanyInfoStatus;
import com.mtravel.platform.enterprise.companyinfo.enums.EnterpriseCompanySignStatus;
import com.mtravel.platform.enterprise.companyinfo.mapper.EnterpriseCompanyInfoMapper;
import org.springframework.stereotype.Service;

/**
 * 企业公司信息业务服务。
 *
 * <p>本服务负责维护当前租户自己的公司主体资料。公司资料按租户只保留一份未删除记录，
 * 保存时采用先查后新增/更新的方式，不依赖数据库 upsert，便于后续补业务审计和变更留痕。</p>
 */
@Service
public class EnterpriseCompanyInfoService extends BusinessCrudService<EnterpriseCompanyInfoEntity, EnterpriseCompanyInfoResponse> {

    private final EnterpriseCompanyInfoMapper mapper;

    public EnterpriseCompanyInfoService(EnterpriseCompanyInfoMapper mapper) {
        super(mapper);
        this.mapper = mapper;
    }

    /**
     * 查询当前租户公司信息。
     *
     * <p>公司资料还没有维护时返回 null，前端可以继续显示空表单，合同甲方也允许手工录入。</p>
     */
    public EnterpriseCompanyInfoResponse current(Long tenantId) {
        EnterpriseCompanyInfoEntity entity = findCurrentEntity(tenantId);
        return EnterpriseCompanyInfoResponse.fromEntity(entity);
    }

    /**
     * 保存当前租户公司信息。
     *
     * <p>如果租户没有公司信息则新增；已有公司信息则更新。创建人和软删除字段只在新增时写入，
     * 更新时不覆盖创建留痕，也不误改软删除状态。</p>
     */
    public EnterpriseCompanyInfoResponse save(
            EnterpriseCompanyInfoSaveRequest request,
            Long tenantId,
            String operator
    ) {
        EnterpriseCompanyInfoEntity existing = findCurrentEntity(tenantId);
        if (existing == null) {
            EnterpriseCompanyInfoEntity entity = new EnterpriseCompanyInfoEntity();
            entity.setTenantId(tenantId);
            applyFields(entity, request);
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            mapper.insert(entity);
            return detail(entity.getId(), tenantId);
        }

        EnterpriseCompanyInfoEntity entity = new EnterpriseCompanyInfoEntity();
        applyFields(entity, request);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", existing.getId()));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(existing.getId(), tenantId);
    }

    /** 查询当前租户未删除的公司信息记录。 */
    private EnterpriseCompanyInfoEntity findCurrentEntity(Long tenantId) {
        return mapper.selectOne(baseQuery(tenantId).orderByAsc("id").last("LIMIT 1"));
    }

    /** 把表单字段落到实体，空字符串统一清理为 null，并集中校验状态枚举。 */
    private void applyFields(EnterpriseCompanyInfoEntity entity, EnterpriseCompanyInfoSaveRequest request) {
        EnterpriseCompanyInfoStatus status = EnterpriseCompanyInfoStatus.fromValueOrDefault(request.status());
        EnterpriseCompanySignStatus signStatus = EnterpriseCompanySignStatus.fromValueOrDefault(request.signStatus());
        entity.setCompanyName(cleanRequired(request.companyName()));
        entity.setProvince(clean(request.province()));
        entity.setCity(clean(request.city()));
        entity.setDistrict(clean(request.district()));
        entity.setContactName(clean(request.contactName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setFaxNumber(clean(request.faxNumber()));
        entity.setOfficeAddress(clean(request.officeAddress()));
        entity.setAlipayEnterpriseName(clean(request.alipayEnterpriseName()));
        entity.setAlipayAccount(clean(request.alipayAccount()));
        entity.setAlipayNickname(clean(request.alipayNickname()));
        entity.setSignStatus(signStatus.getValue());
        entity.setSignLink(clean(request.signLink()));
        entity.setStatus(status.getValue());
        entity.setRemark(clean(request.remark()));
    }

    @Override
    protected EnterpriseCompanyInfoEntity newEntity() {
        return new EnterpriseCompanyInfoEntity();
    }

    @Override
    protected EnterpriseCompanyInfoResponse toResponse(EnterpriseCompanyInfoEntity entity) {
        return EnterpriseCompanyInfoResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "公司信息不存在或已删除";
    }
}

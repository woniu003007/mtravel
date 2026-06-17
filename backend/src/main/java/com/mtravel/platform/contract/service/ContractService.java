package com.mtravel.platform.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.contract.dto.ContractResponse;
import com.mtravel.platform.contract.dto.ContractSaveRequest;
import com.mtravel.platform.contract.entity.ContractEntity;
import com.mtravel.platform.contract.enums.ContractType;
import com.mtravel.platform.contract.mapper.ContractMapper;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.purchase.common.SupplierLookupService;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 统一合同业务服务。
 *
 * <p>本服务统一处理客户合同和采购合同的分页、编号、主体校验、附件绑定和软删除，
 * 确保合同管理页的所有页签使用同一套业务规则。</p>
 */
@Service
public class ContractService extends BusinessCrudService<ContractEntity, ContractResponse> {

    private static final Set<ContractType> SUPPLIER_REQUIRED_TYPES = Set.of(
            ContractType.SCENIC,
            ContractType.HOTEL,
            ContractType.RESTAURANT,
            ContractType.VEHICLE,
            ContractType.TRAFFIC,
            ContractType.GROUND_AGENT,
            ContractType.GUIDE,
            ContractType.SHOPPING
    );

    private final ContractMapper mapper;
    private final CustomerUnitMapper customerMapper;
    private final SupplierLookupService supplierLookup;
    private final CommonAttachmentService attachmentService;
    private final Clock clock;

    @Autowired
    public ContractService(
            ContractMapper mapper,
            CustomerUnitMapper customerMapper,
            SupplierLookupService supplierLookup,
            CommonAttachmentService attachmentService
    ) {
        this(mapper, customerMapper, supplierLookup, attachmentService, Clock.systemDefaultZone());
    }

    ContractService(
            ContractMapper mapper,
            CustomerUnitMapper customerMapper,
            SupplierLookupService supplierLookup,
            CommonAttachmentService attachmentService,
            Clock clock
    ) {
        super(mapper);
        this.mapper = mapper;
        this.customerMapper = customerMapper;
        this.supplierLookup = supplierLookup;
        this.attachmentService = attachmentService;
        this.clock = clock;
    }

    /** 分页查询统一合同台账，支持按合同类型、关联主体、状态和关键字过滤。 */
    public PageResult<ContractResponse> page(
            Long tenantId,
            String keyword,
            String contractType,
            String status,
            Long customerId,
            Long supplierId,
            long page,
            long pageSize
    ) {
        QueryWrapper<ContractEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(contractType), "contract_type", contractType)
                .eq(StringUtils.hasText(status), "status", status)
                .eq(customerId != null, "customer_id", customerId)
                .eq(supplierId != null, "supplier_id", supplierId)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("contract_no", keyword)
                        .or()
                        .like("contract_name", keyword)
                        .or()
                        .like("counterparty_name", keyword))
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /** 按合同类型生成下一业务编号，客户合同与各采购类型使用不同的可读前缀。 */
    public String nextContractNo(Long tenantId, String contractTypeValue) {
        ContractType contractType = ContractType.fromValue(contractTypeValue);
        String prefix = contractType.numberPrefix(LocalDate.now(clock));
        List<Object> values = mapper.selectObjs(baseQuery(tenantId)
                .select("contract_no")
                .likeRight("contract_no", prefix));
        int maxSequence = values.stream()
                .map(Object::toString)
                .mapToInt(value -> parseContractSequence(value, prefix))
                .max()
                .orElse(0);
        return "%s%03d".formatted(prefix, maxSequence + 1);
    }

    /** 新增合同，校验关联主体后绑定预上传附件。 */
    public ContractResponse create(ContractSaveRequest request, Long tenantId, String operator) {
        ContractType type = validateBinding(request, tenantId);
        String contractNo = StringUtils.hasText(request.contractNo())
                ? request.contractNo().trim()
                : nextContractNo(tenantId, type.value());
        assertValueNotExists(tenantId, "contract_no", contractNo, null, "合同编号已存在");

        ContractEntity entity = new ContractEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request, type, contractNo, tenantId);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        attachmentService.bind(entity.getAttachmentId(), entity.getId(), tenantId);
        return detail(entity.getId(), tenantId);
    }

    /** 修改合同，只允许更新当前租户下未删除记录。 */
    public ContractResponse update(Long id, ContractSaveRequest request, Long tenantId) {
        if (!StringUtils.hasText(request.contractNo())) {
            throw new BizException("合同编号不能为空");
        }
        ContractType type = validateBinding(request, tenantId);
        String contractNo = request.contractNo().trim();
        assertValueNotExists(tenantId, "contract_no", contractNo, id, "合同编号已存在");

        ContractEntity entity = new ContractEntity();
        applyFields(entity, request, type, contractNo, tenantId);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        attachmentService.bind(entity.getAttachmentId(), id, tenantId);
        return detail(id, tenantId);
    }

    /**
     * 校验合同关联主体。
     *
     * <p>客户合同必须绑定客户单位；常规采购合同必须绑定供应商；财务费用、现收现退等特殊合同
     * 允许仅保留乙方快照。任何合同都不允许同时绑定客户和供应商。</p>
     */
    private ContractType validateBinding(ContractSaveRequest request, Long tenantId) {
        ContractType type = ContractType.fromValue(request.contractType());
        if (request.customerId() != null && request.supplierId() != null) {
            throw new BizException("合同不能同时绑定客户单位和供应商");
        }
        if (type == ContractType.CUSTOMER) {
            if (request.customerId() == null) {
                throw new BizException("分销商合同必须选择客户单位");
            }
            assertCustomer(tenantId, request.customerId());
            if (request.supplierId() != null) {
                throw new BizException("分销商合同不能绑定供应商");
            }
        } else {
            if (request.customerId() != null) {
                throw new BizException("采购合同不能绑定客户单位");
            }
            if (SUPPLIER_REQUIRED_TYPES.contains(type) && request.supplierId() == null) {
                throw new BizException("%s合同必须选择供应商".formatted(type.label()));
            }
            supplierLookup.assertSupplierIfPresent(tenantId, request.supplierId());
        }
        return type;
    }

    /** 将统一保存请求转换为合同实体，并补充关联主体的名称快照。 */
    private void applyFields(
            ContractEntity entity,
            ContractSaveRequest request,
            ContractType type,
            String contractNo,
            Long tenantId
    ) {
        String counterpartyName = counterpartyName(request, tenantId);
        entity.setContractType(type.value());
        entity.setCustomerId(request.customerId());
        entity.setSupplierId(request.supplierId());
        entity.setContractNo(cleanRequired(contractNo));
        entity.setContractName(StringUtils.hasText(request.contractName())
                ? request.contractName().trim()
                : type.label() + "合同");
        entity.setCounterpartyName(clean(counterpartyName));
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setSettlementTerms(clean(request.settlementTerms()));
        entity.setPurchasePriceSummary(clean(request.purchasePriceSummary()));
        entity.setLegalSubject(clean(request.legalSubject()));
        entity.setInvoiceSubject(clean(request.invoiceSubject()));
        entity.setSettlementSubject(clean(request.settlementSubject()));
        entity.setTemplateName(clean(request.templateName()));
        entity.setReminderDays(request.reminderDays() == null ? 30 : request.reminderDays());
        entity.setAttachmentId(request.attachmentId());
        entity.setContractFileUrl(clean(request.contractFileUrl()));
        entity.setPrintStatus(clean(request.printStatus()));
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        entity.setRemark(clean(request.remark()));
        entity.setPartyAName(clean(request.partyAName()));
        entity.setPartyAPhone(clean(request.partyAPhone()));
        entity.setPartyAFax(clean(request.partyAFax()));
        entity.setPartyAAddress(clean(request.partyAAddress()));
        entity.setPartyAContact(clean(request.partyAContact()));
        entity.setPartyBName(clean(StringUtils.hasText(request.partyBName()) ? request.partyBName() : counterpartyName));
        entity.setPartyBPhone(clean(request.partyBPhone()));
        entity.setPartyBFax(clean(request.partyBFax()));
        entity.setPartyBAddress(clean(request.partyBAddress()));
        entity.setPartyBContact(clean(request.partyBContact()));
        entity.setAgreementContent(clean(request.agreementContent()));
        entity.setOtherContent(clean(request.otherContent()));
    }

    /** 优先使用合同表单中的乙方名称，否则从客户或供应商主档带入名称快照。 */
    private String counterpartyName(ContractSaveRequest request, Long tenantId) {
        if (StringUtils.hasText(request.counterpartyName())) {
            return request.counterpartyName();
        }
        if (request.customerId() != null) {
            CustomerUnitEntity customer = findCustomer(tenantId, request.customerId());
            return customer == null ? null : customer.getCustomerName();
        }
        if (request.supplierId() != null) {
            SupplierEntity supplier = supplierLookup.supplier(tenantId, request.supplierId());
            return supplier == null ? null : supplier.getSupplierName();
        }
        return request.partyBName();
    }

    private void assertCustomer(Long tenantId, Long customerId) {
        if (findCustomer(tenantId, customerId) == null) {
            throw new BizException("客户单位不存在或已删除");
        }
    }

    private CustomerUnitEntity findCustomer(Long tenantId, Long customerId) {
        return customerMapper.selectOne(new QueryWrapper<CustomerUnitEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", customerId));
    }

    private int parseContractSequence(String contractNo, String prefix) {
        if (!contractNo.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(contractNo.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    protected ContractEntity newEntity() {
        return new ContractEntity();
    }

    @Override
    protected ContractResponse toResponse(ContractEntity entity) {
        return ContractResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "合同不存在或已删除";
    }
}

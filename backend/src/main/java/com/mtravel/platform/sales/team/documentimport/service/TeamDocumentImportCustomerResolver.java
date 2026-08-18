package com.mtravel.platform.sales.team.documentimport.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 团队 Word 代录的客户主档解析和校验服务。
 *
 * <p>文档识别出的客户名称只是候选文本，只有唯一命中当前租户启用客户主档后才可写入
 * customerId 和客户名称快照。草稿保存、旧草稿读取和正式应用均复用本服务，防止自由文本
 * 绕过前端选择框进入订单。</p>
 */
@Service
public class TeamDocumentImportCustomerResolver {
    private static final String ACTIVE_STATUS = "active";

    private final CustomerUnitMapper customerMapper;

    public TeamDocumentImportCustomerResolver(CustomerUnitMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    /**
     * 将模型识别的客户名称关联到唯一有效客户主档。
     *
     * <p>未命中或同名多条时不保留自由文本客户名称，但联系人和联系电话仍可由计调继续编辑。</p>
     */
    public TeamDocumentImportDraft resolveRecognizedCustomer(TeamDocumentImportDraft draft, Long tenantId) {
        if (draft == null || draft.order() == null) {
            return draft;
        }
        TeamDocumentImportDraft.OrderDraft order = draft.order();
        String recognizedName = clean(order.customerName());
        if (!StringUtils.hasText(recognizedName) || order.customerId() != null) {
            return draft;
        }
        List<CustomerUnitEntity> matches = findActiveCustomersByName(recognizedName, tenantId);
        if (matches.size() == 1) {
            CustomerUnitEntity customer = matches.getFirst();
            return replaceOrder(draft, copyOrder(order, customer.getId(), customer.getCustomerName()), draft.warnings());
        }
        String warning = matches.isEmpty()
                ? "识别到客户单位“%s”，但系统客户主档中不存在或已停用，请先新增客户或选择已有客户".formatted(recognizedName)
                : "识别到客户单位“%s”，但系统中存在多个有效同名客户，请手工选择客户单位".formatted(recognizedName);
        return replaceOrder(draft, copyOrder(order, null, null), appendWarning(draft.warnings(), warning));
    }

    /**
     * 规范化历史未应用草稿的客户选择。
     *
     * <p>旧任务可能只存了 customerName。读取时重新匹配并持久化，避免页面继续展示可被直接
     * 写入订单的自由文本；已携带客户 ID 的历史草稿则按当前有效主档回填正式名称。</p>
     */
    public TeamDocumentImportDraft normalizeForPreview(TeamDocumentImportDraft draft, Long tenantId) {
        if (draft == null || draft.order() == null) {
            return draft;
        }
        TeamDocumentImportDraft.OrderDraft order = draft.order();
        if (order.customerId() == null) {
            return resolveRecognizedCustomer(draft, tenantId);
        }
        CustomerUnitEntity customer = findActiveCustomerById(order.customerId(), tenantId);
        if (customer == null) {
            return replaceOrder(
                    draft,
                    copyOrder(order, null, null),
                    appendWarning(draft.warnings(), "草稿中的客户单位不存在、已停用或不属于当前租户，请重新选择")
            );
        }
        return replaceOrder(draft, copyOrder(order, customer.getId(), customer.getCustomerName()), draft.warnings());
    }

    /**
     * 在保存草稿或正式写入前校验客户选择。
     *
     * @throws BizException 当请求携带自由文本、跨租户客户、停用客户或名称快照不一致时抛出。
     */
    public TeamDocumentImportDraft validateForPersistence(TeamDocumentImportDraft draft, Long tenantId) {
        if (draft == null || draft.order() == null) {
            return draft;
        }
        TeamDocumentImportDraft.OrderDraft order = draft.order();
        Long customerId = order.customerId();
        String customerName = clean(order.customerName());
        if (customerId == null && !StringUtils.hasText(customerName)) {
            return draft;
        }
        if (customerId == null) {
            throw new BizException("客户单位必须从系统客户主档选择，不能直接填写名称");
        }
        if (!StringUtils.hasText(customerName)) {
            throw new BizException("已选择客户单位但缺少客户名称，请重新选择");
        }
        CustomerUnitEntity customer = findActiveCustomerById(customerId, tenantId);
        if (customer == null) {
            throw new BizException("客户单位不存在、已停用或不属于当前租户，请重新选择");
        }
        if (!customer.getCustomerName().equals(customerName)) {
            throw new BizException("客户单位名称与系统主档不一致，请重新选择");
        }
        return replaceOrder(draft, copyOrder(order, customer.getId(), customer.getCustomerName()), draft.warnings());
    }

    /**
     * 在正式生成订单前确认已选择有效客户主档。
     *
     * <p>草稿允许暂存尚未匹配的客户，方便计调先修正其它识别内容；但订单必须有系统客户，
     * 不能以空客户绕过“客户单位必须建档”的规则。</p>
     *
     * @throws BizException 当订单草稿未选择系统客户时抛出。
     */
    public TeamDocumentImportDraft requireCustomerForApplication(TeamDocumentImportDraft draft, Long tenantId) {
        TeamDocumentImportDraft validated = validateForPersistence(draft, tenantId);
        if (validated == null || validated.order() == null || validated.order().customerId() == null) {
            throw new BizException("请先从系统客户主档选择客户单位，再生成订单");
        }
        return validated;
    }

    private List<CustomerUnitEntity> findActiveCustomersByName(String customerName, Long tenantId) {
        return customerMapper.selectList(activeQuery(tenantId)
                .eq(CustomerUnitEntity::getCustomerName, customerName)
                .orderByAsc(CustomerUnitEntity::getId));
    }

    private CustomerUnitEntity findActiveCustomerById(Long customerId, Long tenantId) {
        return customerMapper.selectOne(activeQuery(tenantId)
                .eq(CustomerUnitEntity::getId, customerId)
                .last("limit 1"));
    }

    private LambdaQueryWrapper<CustomerUnitEntity> activeQuery(Long tenantId) {
        return new LambdaQueryWrapper<CustomerUnitEntity>()
                .eq(CustomerUnitEntity::getTenantId, tenantId)
                .eq(CustomerUnitEntity::getIsDeleted, false)
                .eq(CustomerUnitEntity::getStatus, ACTIVE_STATUS);
    }

    private TeamDocumentImportDraft replaceOrder(
            TeamDocumentImportDraft draft,
            TeamDocumentImportDraft.OrderDraft order,
            List<String> warnings
    ) {
        return new TeamDocumentImportDraft(
                draft.documentType(), draft.confidence(), draft.team(), order, draft.guests(), draft.itineraryDays(),
                draft.resources(), warnings, draft.evidence(), draft.productDescription()
        );
    }

    private TeamDocumentImportDraft.OrderDraft copyOrder(
            TeamDocumentImportDraft.OrderDraft source,
            Long customerId,
            String customerName
    ) {
        return new TeamDocumentImportDraft.OrderDraft(
                customerId,
                clean(customerName),
                source.contactName(),
                source.contactPhone(),
                source.pickupInfo(),
                source.dropoffInfo(),
                source.guideName(),
                source.guidePhone(),
                source.orderRemark(),
                source.priceLines()
        );
    }

    private List<String> appendWarning(List<String> warnings, String warning) {
        List<String> result = new ArrayList<>();
        if (warnings != null) {
            warnings.stream().filter(StringUtils::hasText).map(String::trim).forEach(result::add);
        }
        if (StringUtils.hasText(warning) && !result.contains(warning)) {
            result.add(warning);
        }
        return List.copyOf(result);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

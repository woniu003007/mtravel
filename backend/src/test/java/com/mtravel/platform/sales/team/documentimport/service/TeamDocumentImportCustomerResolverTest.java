package com.mtravel.platform.sales.team.documentimport.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 团队 Word 草稿客户主档关联测试，防止自由文本客户进入订单。 */
class TeamDocumentImportCustomerResolverTest {

    @Test
    void resolveRecognizedCustomerShouldUseTheUniqueActiveTenantCustomerAndKeepContactEditableFields() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        TeamDocumentImportCustomerResolver resolver = new TeamDocumentImportCustomerResolver(customerMapper);
        CustomerUnitEntity customer = customer(51L, "东晟假期");
        when(customerMapper.selectList(any())).thenReturn(List.of(customer));

        TeamDocumentImportDraft resolved = resolver.resolveRecognizedCustomer(draft(null, "东晟假期"), 1L);

        assertThat(resolved.order().customerId()).isEqualTo(51L);
        assertThat(resolved.order().customerName()).isEqualTo("东晟假期");
        assertThat(resolved.order().contactName()).isEqualTo("刘小川");
        assertThat(resolved.order().contactPhone()).isEqualTo("13840835417");
    }

    @Test
    void resolveRecognizedCustomerShouldClearAnUnmatchedLegacyFreeTextCustomerAndWarnForManualSelection() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        TeamDocumentImportCustomerResolver resolver = new TeamDocumentImportCustomerResolver(customerMapper);
        when(customerMapper.selectList(any())).thenReturn(List.of());

        TeamDocumentImportDraft resolved = resolver.normalizeForPreview(draft(null, "东晟假期"), 1L);

        assertThat(resolved.order().customerId()).isNull();
        assertThat(resolved.order().customerName()).isNull();
        assertThat(resolved.order().contactName()).isEqualTo("刘小川");
        assertThat(resolved.warnings()).anyMatch(item -> item.contains("东晟假期") && item.contains("客户主档"));
    }

    @Test
    void validateForPersistenceShouldRejectFreeTextCustomerNameWithoutCustomerId() {
        TeamDocumentImportCustomerResolver resolver = new TeamDocumentImportCustomerResolver(mock(CustomerUnitMapper.class));

        assertThatThrownBy(() -> resolver.validateForPersistence(draft(null, "东晟假期"), 1L))
                .isInstanceOf(BizException.class)
                .hasMessage("客户单位必须从系统客户主档选择，不能直接填写名称");
    }

    @Test
    void validateForPersistenceShouldRejectANameThatDoesNotMatchTheSelectedTenantCustomer() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        TeamDocumentImportCustomerResolver resolver = new TeamDocumentImportCustomerResolver(customerMapper);
        when(customerMapper.selectOne(any())).thenReturn(customer(51L, "东晟假期"));

        assertThatThrownBy(() -> resolver.validateForPersistence(draft(51L, "其它旅行社"), 1L))
                .isInstanceOf(BizException.class)
                .hasMessage("客户单位名称与系统主档不一致，请重新选择");
    }

    @Test
    void validateForPersistenceShouldRejectASelectedCustomerOutsideTheCurrentTenantOrDisabled() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        TeamDocumentImportCustomerResolver resolver = new TeamDocumentImportCustomerResolver(customerMapper);
        when(customerMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> resolver.validateForPersistence(draft(51L, "东晟假期"), 1L))
                .isInstanceOf(BizException.class)
                .hasMessage("客户单位不存在、已停用或不属于当前租户，请重新选择");
    }

    private CustomerUnitEntity customer(Long id, String name) {
        CustomerUnitEntity entity = new CustomerUnitEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setCustomerName(name);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private TeamDocumentImportDraft draft(Long customerId, String customerName) {
        return new TeamDocumentImportDraft(
                "ground_confirmation",
                0.9,
                new TeamDocumentImportDraft.TeamDraft("悦色江南", "2026-06-25", 6, 17, null, "domestic", null, null),
                new TeamDocumentImportDraft.OrderDraft(
                        customerId,
                        customerName,
                        "刘小川",
                        "13840835417",
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of()
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}

package com.mtravel.platform.customer.creditrule.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.enums.CustomerCategoryStatus;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.customer.creditrule.dto.CustomerCreditRuleResponse;
import com.mtravel.platform.customer.creditrule.dto.CustomerCreditRuleSaveRequest;
import com.mtravel.platform.customer.creditrule.entity.CustomerCreditRuleEntity;
import com.mtravel.platform.customer.creditrule.mapper.CustomerCreditRuleMapper;
import com.mtravel.platform.enterprise.employee.entity.EnterpriseEmployeeEntity;
import com.mtravel.platform.enterprise.employee.mapper.EnterpriseEmployeeMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户授信规则服务测试。
 *
 * <p>重点固定人员 ID 的有序序列化和分页名称批量查询，避免页面回显演变为按行查客户等级、员工的 N+1 查询。</p>
 */
class CustomerCreditRuleServiceTest {

    @Test
    void createShouldSerializeOrderedEmployeeIdsAndReturnNames() {
        CustomerCreditRuleMapper mapper = mock(CustomerCreditRuleMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        EnterpriseEmployeeMapper employeeMapper = mock(EnterpriseEmployeeMapper.class);
        CustomerCreditRuleService service = new CustomerCreditRuleService(mapper, categoryMapper, employeeMapper);
        CustomerCategoryEntity level = category(7L, "A级客户");
        CustomerCreditRuleEntity[] inserted = new CustomerCreditRuleEntity[1];
        ArgumentCaptor<CustomerCreditRuleEntity> captor = ArgumentCaptor.forClass(CustomerCreditRuleEntity.class);

        when(categoryMapper.selectOne(any(Wrapper.class))).thenReturn(level);
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(level));
        when(employeeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                employee(11L, "张三"), employee(12L, "李四"), employee(13L, "王五")
        ));
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            CustomerCreditRuleEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            inserted[0] = entity;
            return 1;
        }).when(mapper).insert(any(CustomerCreditRuleEntity.class));
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> inserted[0]);

        CustomerCreditRuleResponse response = service.create(new CustomerCreditRuleSaveRequest(
                7L,
                new BigDecimal("500000.00"),
                30,
                true,
                List.of(12L, 11L, 12L),
                List.of(13L),
                "active",
                "重点等级"
        ), 1L, "admin");

        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getCustomerLevelId()).isEqualTo(7L);
        assertThat(captor.getValue().getApproverEmployeeIds()).isEqualTo("12,11");
        assertThat(captor.getValue().getCcEmployeeIds()).isEqualTo("13");
        assertThat(captor.getValue().getAccountPeriodDays()).isEqualTo(30);
        assertThat(captor.getValue().getAllowOverLimit()).isTrue();
        assertThat(response.customerLevelName()).isEqualTo("A级客户");
        assertThat(response.approverEmployeeIds()).containsExactly(12L, 11L);
        assertThat(response.approverNames()).containsExactly("李四", "张三");
        assertThat(response.ccNames()).containsExactly("王五");
    }

    @Test
    void pageShouldLoadCustomerLevelsAndEmployeesInBulk() {
        CustomerCreditRuleMapper mapper = mock(CustomerCreditRuleMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        EnterpriseEmployeeMapper employeeMapper = mock(EnterpriseEmployeeMapper.class);
        CustomerCreditRuleService service = new CustomerCreditRuleService(mapper, categoryMapper, employeeMapper);
        Page<CustomerCreditRuleEntity> databasePage = new Page<>(1, 20);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        databasePage.setTotal(2L);
        databasePage.setRecords(List.of(
                rule(1L, 7L, "11,12", "13"),
                rule(2L, 8L, "12", "")
        ));

        when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(databasePage);
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                category(7L, "A级客户"), category(8L, "B级客户")
        ));
        when(employeeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                employee(11L, "张三"), employee(12L, "李四"), employee(13L, "王五")
        ));

        PageResult<CustomerCreditRuleResponse> result = service.page(1L, null, null, null, 1, 20);

        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.items()).extracting(CustomerCreditRuleResponse::customerLevelName)
                .containsExactly("A级客户", "B级客户");
        assertThat(result.items().getFirst().approverNames()).containsExactly("张三", "李四");
        assertThat(result.items().getFirst().ccNames()).containsExactly("王五");
        verify(mapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue()).isInstanceOf(QueryWrapper.class);
        assertThat(((QueryWrapper<?>) wrapperCaptor.getValue()).getSqlSegment())
                .contains("ORDER BY credit_limit ASC,id ASC");
        verify(categoryMapper, times(1)).selectList(any(Wrapper.class));
        verify(employeeMapper, times(1)).selectList(any(Wrapper.class));
    }

    private CustomerCategoryEntity category(Long id, String name) {
        CustomerCategoryEntity entity = new CustomerCategoryEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setCategoryName(name);
        entity.setStatus(CustomerCategoryStatus.ACTIVE.getValue());
        entity.setIsDeleted(false);
        return entity;
    }

    private EnterpriseEmployeeEntity employee(Long id, String name) {
        EnterpriseEmployeeEntity entity = new EnterpriseEmployeeEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setEmployeeName(name);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private CustomerCreditRuleEntity rule(Long id, Long levelId, String approvers, String ccEmployees) {
        CustomerCreditRuleEntity entity = new CustomerCreditRuleEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setCustomerLevelId(levelId);
        entity.setCreditLimit(new BigDecimal("100000"));
        entity.setAccountPeriodDays(30);
        entity.setAllowOverLimit(false);
        entity.setApproverEmployeeIds(approvers);
        entity.setCcEmployeeIds(ccEmployees);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }
}

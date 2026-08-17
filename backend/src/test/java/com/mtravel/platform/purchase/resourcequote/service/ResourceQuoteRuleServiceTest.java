package com.mtravel.platform.purchase.resourcequote.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.purchase.resourcequote.dto.ResourceQuoteRuleResponse;
import com.mtravel.platform.purchase.resourcequote.dto.ResourceQuoteRuleSaveRequest;
import com.mtravel.platform.purchase.resourcequote.entity.ResourceQuoteRuleEntity;
import com.mtravel.platform.purchase.resourcequote.mapper.ResourceQuoteRuleMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 普通资源报价规则服务测试。
 *
 * <p>重点验证客户等级由具体等级切换为默认规则时，UpdateWrapper 会生成 customer_level_id 的 NULL 更新，
 * 不受 MyBatis-Plus 实体字段空值策略影响。</p>
 */
class ResourceQuoteRuleServiceTest {

    @Test
    void updateShouldExplicitlyClearCustomerLevelId() {
        ResourceQuoteRuleMapper mapper = mock(ResourceQuoteRuleMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        ResourceQuoteRuleService service = new ResourceQuoteRuleService(mapper, categoryMapper);
        ResourceQuoteRuleEntity saved = rule(99L, null);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(saved);

        ResourceQuoteRuleResponse response = service.update(99L, new ResourceQuoteRuleSaveRequest(
                "hotel",
                null,
                new BigDecimal("20.00"),
                new BigDecimal("15.00"),
                new BigDecimal("100.00"),
                new BigDecimal("80.00"),
                "active",
                "默认酒店规则"
        ), 1L);

        verify(mapper).update(isNull(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue()).isInstanceOf(UpdateWrapper.class);
        @SuppressWarnings("unchecked")
        UpdateWrapper<ResourceQuoteRuleEntity> updateWrapper = (UpdateWrapper<ResourceQuoteRuleEntity>) wrapperCaptor.getValue();
        assertThat(updateWrapper.getSqlSet()).contains("customer_level_id");
        assertThat(updateWrapper.getParamNameValuePairs().values()).containsNull();
        assertThat(response.customerLevelId()).isNull();
        assertThat(response.suggestedRate()).isEqualByComparingTo("20.00");
    }

    @Test
    void rejectsMinimumMarkupAboveSuggestedMarkup() {
        ResourceQuoteRuleMapper mapper = mock(ResourceQuoteRuleMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        ResourceQuoteRuleService service = new ResourceQuoteRuleService(mapper, categoryMapper);

        assertThatThrownBy(() -> service.create(new ResourceQuoteRuleSaveRequest(
                "hotel",
                null,
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                new BigDecimal("80.00"),
                "active",
                ""
        ), 1L, "tester"))
                .isInstanceOf(BizException.class)
                .hasMessage("最低比例不能高于建议比例");

        verifyNoInteractions(mapper, categoryMapper);
    }

    private ResourceQuoteRuleEntity rule(Long id, Long customerLevelId) {
        ResourceQuoteRuleEntity entity = new ResourceQuoteRuleEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setResourceType("hotel");
        entity.setCustomerLevelId(customerLevelId);
        entity.setSuggestedMarkupRate(new BigDecimal("20.00"));
        entity.setMinimumMarkupRate(new BigDecimal("15.00"));
        entity.setSuggestedFixedMarkup(new BigDecimal("100.00"));
        entity.setMinimumFixedMarkup(new BigDecimal("80.00"));
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }
}

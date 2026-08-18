package com.mtravel.platform.configuration.quote.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.configuration.quote.dto.QuoteApprovalConfigRequest;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGroundAgentRuleSaveRequest;
import com.mtravel.platform.configuration.quote.entity.SalesQuoteApprovalMemberEntity;
import com.mtravel.platform.configuration.quote.entity.SalesQuoteGroundAgentRuleEntity;
import com.mtravel.platform.configuration.quote.entity.SalesQuoteResourceRuleEntity;
import com.mtravel.platform.configuration.quote.enums.SalesQuoteResourceQuoteMode;
import java.lang.reflect.Field;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteApprovalMemberMapper;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteGroundAgentRuleMapper;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteGuideLevelMapper;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteGuideRuleMapper;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteResourceRuleMapper;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideMapper;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalesQuoteConfigServiceTest {

    @Test
    void saveApprovalConfigShouldRequireApprover() {
        SalesQuoteApprovalMemberMapper approvalMapper = mock(SalesQuoteApprovalMemberMapper.class);
        SalesQuoteConfigService service = service(mock(SalesQuoteGroundAgentRuleMapper.class), approvalMapper);

        assertThatThrownBy(() -> service.saveApprovalConfig(
                new QuoteApprovalConfigRequest(List.of(), List.of()),
                1L,
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("报价低价审批人不能为空");

        verify(approvalMapper, never()).insert(any(SalesQuoteApprovalMemberEntity.class));
    }

    @Test
    void createGroundAgentRuleShouldRejectOverlapRange() {
        SalesQuoteGroundAgentRuleMapper groundMapper = mock(SalesQuoteGroundAgentRuleMapper.class);
        SalesQuoteConfigService service = service(groundMapper, mock(SalesQuoteApprovalMemberMapper.class));
        SalesQuoteGroundAgentRuleSaveRequest request = new SalesQuoteGroundAgentRuleSaveRequest(
                8,
                15,
                BigDecimal.valueOf(12_000),
                "active",
                "11-20人已存在时不能重叠"
        );

        when(groundMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.createGroundAgentRule(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("地接报价人数区间不能重叠");

        verify(groundMapper, never()).insert(any(SalesQuoteGroundAgentRuleEntity.class));
    }

    @Test
    void resourceCustomerCategoryFieldsShouldWriteNullWhenCleared() throws NoSuchFieldException {
        TableField categoryId = field("customerCategoryId").getAnnotation(TableField.class);
        TableField categoryName = field("customerCategoryName").getAnnotation(TableField.class);

        assertThat(categoryId.updateStrategy()).isEqualTo(FieldStrategy.ALWAYS);
        assertThat(categoryName.updateStrategy()).isEqualTo(FieldStrategy.ALWAYS);
    }

    @Test
    void approvalMemberShouldExcludeTheInheritedRemarkField() {
        TableName tableName = SalesQuoteApprovalMemberEntity.class.getAnnotation(TableName.class);

        assertThat(tableName.excludeProperty()).containsExactly("remark");
    }

    @Test
    void resourceQuoteModeShouldDefaultToBothAndRejectUnknownValues() {
        assertThat(SalesQuoteResourceQuoteMode.fromValueOrDefault(null)).isEqualTo("both");
        assertThatThrownBy(() -> SalesQuoteResourceQuoteMode.fromValueOrDefault("invalid"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("报价方式不合法");
    }

    private Field field(String name) throws NoSuchFieldException {
        return SalesQuoteResourceRuleEntity.class.getDeclaredField(name);
    }

    private SalesQuoteConfigService service(
            SalesQuoteGroundAgentRuleMapper groundMapper,
            SalesQuoteApprovalMemberMapper approvalMapper
    ) {
        return new SalesQuoteConfigService(
                mock(SalesQuoteResourceRuleMapper.class),
                mock(SalesQuoteGuideLevelMapper.class),
                mock(SalesQuoteGuideRuleMapper.class),
                groundMapper,
                approvalMapper,
                mock(CustomerCategoryMapper.class),
                mock(SystemUserMapper.class),
                mock(EnterpriseGuideMapper.class)
        );
    }
}

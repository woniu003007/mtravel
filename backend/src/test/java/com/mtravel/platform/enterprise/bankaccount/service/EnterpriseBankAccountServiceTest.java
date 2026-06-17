package com.mtravel.platform.enterprise.bankaccount.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.enterprise.bankaccount.dto.EnterpriseBankAccountSaveRequest;
import com.mtravel.platform.enterprise.bankaccount.entity.EnterpriseBankAccountEntity;
import com.mtravel.platform.enterprise.bankaccount.mapper.EnterpriseBankAccountMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class EnterpriseBankAccountServiceTest {

    @Test
    void createShouldRejectDuplicateBankAccountNo() {
        EnterpriseBankAccountMapper mapper = mock(EnterpriseBankAccountMapper.class);
        EnterpriseBankAccountService service = new EnterpriseBankAccountService(mapper);
        EnterpriseBankAccountSaveRequest request = request("中国农业银行湖墅支行", "浙江测试旅行社", "6228480012345678901");

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("银行账号已存在");

        verify(mapper, never()).insert(any(EnterpriseBankAccountEntity.class));
    }

    @Test
    void createShouldPersistPrintableFlagAndOtherInfo() {
        EnterpriseBankAccountMapper mapper = mock(EnterpriseBankAccountMapper.class);
        EnterpriseBankAccountService service = new EnterpriseBankAccountService(mapper);
        EnterpriseBankAccountSaveRequest request = request("支付宝", "张三疯", "travel@example.com");
        ArgumentCaptor<EnterpriseBankAccountEntity> captor = ArgumentCaptor.forClass(EnterpriseBankAccountEntity.class);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer((Answer<Integer>) invocation -> {
            EnterpriseBankAccountEntity entity = invocation.getArgument(0);
            entity.setId(12L);
            return 1;
        }).when(mapper).insert(any(EnterpriseBankAccountEntity.class));
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            verify(mapper).insert(captor.capture());
            return captor.getValue();
        });

        service.create(request, 1L, "admin");

        EnterpriseBankAccountEntity entity = captor.getValue();
        assertThat(entity.getBankName()).isEqualTo("支付宝");
        assertThat(entity.getAccountName()).isEqualTo("张三疯");
        assertThat(entity.getAccountNo()).isEqualTo("travel@example.com");
        assertThat(entity.getPrintEnabled()).isTrue();
        assertThat(entity.getOtherInfo()).isEqualTo("收款码可用于导游备用金付款");
        assertThat(entity.getStatus()).isEqualTo("active");
        assertThat(entity.getCreatedBy()).isEqualTo("admin");
    }

    @Test
    void updatePrintEnabledShouldOnlyChangePrintableFlag() {
        EnterpriseBankAccountMapper mapper = mock(EnterpriseBankAccountMapper.class);
        EnterpriseBankAccountService service = new EnterpriseBankAccountService(mapper);
        ArgumentCaptor<EnterpriseBankAccountEntity> captor = ArgumentCaptor.forClass(EnterpriseBankAccountEntity.class);

        when(mapper.update(any(EnterpriseBankAccountEntity.class), any(Wrapper.class))).thenReturn(1);

        service.updatePrintEnabled(9L, 1L, false);

        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getPrintEnabled()).isFalse();
        assertThat(captor.getValue().getBankName()).isNull();
    }

    @Test
    void deleteShouldSoftDeleteBankAccount() {
        EnterpriseBankAccountMapper mapper = mock(EnterpriseBankAccountMapper.class);
        EnterpriseBankAccountService service = new EnterpriseBankAccountService(mapper);
        ArgumentCaptor<EnterpriseBankAccountEntity> captor = ArgumentCaptor.forClass(EnterpriseBankAccountEntity.class);

        when(mapper.update(any(EnterpriseBankAccountEntity.class), any(Wrapper.class))).thenReturn(1);

        service.delete(9L, 1L, "admin");

        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    private EnterpriseBankAccountSaveRequest request(String bankName, String accountName, String accountNo) {
        return new EnterpriseBankAccountSaveRequest(
                bankName,
                accountName,
                accountNo,
                true,
                "收款码可用于导游备用金付款",
                "active",
                "企业常用收付款账户"
        );
    }
}

package com.mtravel.platform.contract.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.contract.dto.ContractSaveRequest;
import com.mtravel.platform.contract.entity.ContractEntity;
import com.mtravel.platform.contract.mapper.ContractMapper;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.purchase.common.SupplierLookupService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 统一合同服务测试。
 *
 * <p>测试覆盖合同编号生成、合同主体互斥和附件绑定，防止客户合同与采购合同合表后
 * 出现同一合同同时绑定客户和供应商的脏数据。</p>
 */
class ContractServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T03:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Test
    void nextContractNoShouldUseContractTypePrefix() {
        ContractMapper mapper = mock(ContractMapper.class);
        ContractService service = service(mapper);
        when(mapper.selectObjs(any(Wrapper.class))).thenReturn(List.of(
                "HT-JQ-20260610-002",
                "HT-JQ-20260610-009"
        ));

        assertThat(service.nextContractNo(1L, "scenic"))
                .isEqualTo("HT-JQ-20260610-010");
    }

    @Test
    void createShouldRejectCustomerAndSupplierBoundTogether() {
        ContractService service = service(mock(ContractMapper.class));

        assertThatThrownBy(() -> service.create(request(3L, 6L), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("合同不能同时绑定客户单位和供应商");
    }

    @Test
    void createShouldPersistUnifiedContractAndBindAttachment() {
        ContractMapper mapper = mock(ContractMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        SupplierLookupService supplierLookup = mock(SupplierLookupService.class);
        ContractService service = new ContractService(
                mapper,
                customerMapper,
                supplierLookup,
                attachmentService,
                FIXED_CLOCK
        );
        ArgumentCaptor<ContractEntity> captor = ArgumentCaptor.forClass(ContractEntity.class);
        when(mapper.selectObjs(any(Wrapper.class))).thenReturn(List.of());
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer((Answer<Integer>) invocation -> {
            ContractEntity entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        }).when(mapper).insert(any(ContractEntity.class));
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            verify(mapper).insert(captor.capture());
            return captor.getValue();
        });

        service.create(request(null, 6L), 1L, "admin");

        ContractEntity saved = captor.getValue();
        assertThat(saved.getContractType()).isEqualTo("scenic");
        assertThat(saved.getSupplierId()).isEqualTo(6L);
        assertThat(saved.getContractNo()).isEqualTo("HT-JQ-20260610-001");
        verify(attachmentService).bind(eq(99L), eq(88L), eq(1L));
    }

    private ContractService service(ContractMapper mapper) {
        return new ContractService(
                mapper,
                mock(CustomerUnitMapper.class),
                mock(SupplierLookupService.class),
                mock(CommonAttachmentService.class),
                FIXED_CLOCK
        );
    }

    private ContractSaveRequest request(Long customerId, Long supplierId) {
        return new ContractSaveRequest(
                "scenic",
                customerId,
                supplierId,
                null,
                "景区采购合同",
                "苏州园林票务中心",
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2027, 6, 9),
                "月结",
                "门票采购价按确认单执行",
                null,
                null,
                null,
                null,
                30,
                99L,
                null,
                null,
                "active",
                "测试合同",
                "常云科技",
                null,
                null,
                null,
                null,
                "苏州园林票务中心",
                null,
                null,
                null,
                null,
                "双方按确认单执行。",
                null
        );
    }
}

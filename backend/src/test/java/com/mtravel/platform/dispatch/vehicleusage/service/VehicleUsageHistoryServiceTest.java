package com.mtravel.platform.dispatch.vehicleusage.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.dispatch.vehicleusage.dto.VehicleUsageHistoryRecordRequest;
import com.mtravel.platform.dispatch.vehicleusage.entity.VehicleUsageHistoryEntity;
import com.mtravel.platform.dispatch.vehicleusage.mapper.VehicleUsageHistoryMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用车历史候选服务测试。
 *
 * <p>司机信息和车牌号由业务人员手动输入，系统只负责把常用内容沉淀成候选项并按使用次数排序，
 * 不能把它误做成固定司机档案或车辆档案。</p>
 */
class VehicleUsageHistoryServiceTest {

    @Test
    void recordUseShouldInsertNewCandidate() {
        VehicleUsageHistoryMapper mapper = mock(VehicleUsageHistoryMapper.class);
        VehicleUsageHistoryService service = new VehicleUsageHistoryService(mapper);
        ArgumentCaptor<VehicleUsageHistoryEntity> captor = ArgumentCaptor.forClass(VehicleUsageHistoryEntity.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);

        service.recordUse(new VehicleUsageHistoryRecordRequest("driver_info", "  宋小宝13499999999  "), 1L, "admin");

        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getHistoryType()).isEqualTo("driver_info");
        assertThat(captor.getValue().getContent()).isEqualTo("宋小宝13499999999");
        assertThat(captor.getValue().getNormalizedContent()).isEqualTo("宋小宝13499999999");
        assertThat(captor.getValue().getUsageCount()).isEqualTo(1);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getIsDeleted()).isFalse();
    }

    @Test
    void recordUseShouldIncrementExistingCandidate() {
        VehicleUsageHistoryMapper mapper = mock(VehicleUsageHistoryMapper.class);
        VehicleUsageHistoryService service = new VehicleUsageHistoryService(mapper);
        ArgumentCaptor<VehicleUsageHistoryEntity> captor = ArgumentCaptor.forClass(VehicleUsageHistoryEntity.class);
        VehicleUsageHistoryEntity existing = new VehicleUsageHistoryEntity();
        existing.setId(88L);
        existing.setTenantId(1L);
        existing.setHistoryType("vehicle_plate");
        existing.setContent("浙A66666");
        existing.setNormalizedContent("浙a66666");
        existing.setUsageCount(4);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        service.recordUse(new VehicleUsageHistoryRecordRequest("vehicle_plate", "浙A66666"), 1L, "admin");

        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(88L);
        assertThat(captor.getValue().getUsageCount()).isEqualTo(5);
    }

    @Test
    void recordUseShouldIgnoreBlankContent() {
        VehicleUsageHistoryMapper mapper = mock(VehicleUsageHistoryMapper.class);
        VehicleUsageHistoryService service = new VehicleUsageHistoryService(mapper);

        service.recordUse(new VehicleUsageHistoryRecordRequest("driver_info", "  "), 1L, "admin");

        verify(mapper, never()).insert(any(VehicleUsageHistoryEntity.class));
        verify(mapper, never()).updateById(any(VehicleUsageHistoryEntity.class));
    }

    @Test
    void suggestShouldReturnMappedCandidates() {
        VehicleUsageHistoryMapper mapper = mock(VehicleUsageHistoryMapper.class);
        VehicleUsageHistoryService service = new VehicleUsageHistoryService(mapper);
        VehicleUsageHistoryEntity frequent = entity("宋小宝13499999999", 12);
        VehicleUsageHistoryEntity recent = entity("曲师傅13900000000", 5);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(frequent, recent));

        var result = service.suggest("driver_info", null, 1L, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).content()).isEqualTo("宋小宝13499999999");
        assertThat(result.get(0).usageCount()).isEqualTo(12);
        assertThat(result.get(1).content()).isEqualTo("曲师傅13900000000");
    }

    @Test
    void invalidHistoryTypeShouldBeRejected() {
        VehicleUsageHistoryMapper mapper = mock(VehicleUsageHistoryMapper.class);
        VehicleUsageHistoryService service = new VehicleUsageHistoryService(mapper);

        assertThatThrownBy(() -> service.suggest("supplier", null, 1L, 10))
                .isInstanceOf(BizException.class)
                .hasMessage("用车历史类型不正确");
    }

    private VehicleUsageHistoryEntity entity(String content, int usageCount) {
        VehicleUsageHistoryEntity entity = new VehicleUsageHistoryEntity();
        entity.setContent(content);
        entity.setUsageCount(usageCount);
        return entity;
    }
}

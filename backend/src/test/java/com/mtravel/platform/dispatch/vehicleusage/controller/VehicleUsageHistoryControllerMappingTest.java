package com.mtravel.platform.dispatch.vehicleusage.controller;

import com.mtravel.platform.dispatch.vehicleusage.service.VehicleUsageHistoryService;
import com.mtravel.platform.tenant.TenantProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 用车历史候选接口映射测试。
 *
 * <p>前端产品团队安排和后续正式团队安排都依赖这组路径，避免后续重构时误改接口地址。</p>
 */
class VehicleUsageHistoryControllerMappingTest {

    @Test
    void shouldExposeStableVehicleUsageHistoryPaths() throws NoSuchMethodException {
        VehicleUsageHistoryController controller = new VehicleUsageHistoryController(
                mock(VehicleUsageHistoryService.class),
                new TenantProperties()
        );

        assertThat(controller).isNotNull();
        RequestMapping root = VehicleUsageHistoryController.class.getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/dispatch/vehicle-usage-histories");
        assertThat(VehicleUsageHistoryController.class.getMethod("suggest", String.class, String.class, Integer.class)
                .getAnnotation(GetMapping.class)
                .value()).isEmpty();
        assertThat(VehicleUsageHistoryController.class.getMethod(
                        "recordUse",
                        com.mtravel.platform.dispatch.vehicleusage.dto.VehicleUsageHistoryRecordRequest.class,
                        org.springframework.security.core.Authentication.class)
                .getAnnotation(PostMapping.class)
                .value()).containsExactly("/record-use");
    }
}

package com.mtravel.platform.dispatch.vehiclequote.controller;

import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteCalculateRequest;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteRuleSaveRequest;
import com.mtravel.platform.dispatch.vehiclequote.service.VehicleQuoteRuleService;
import com.mtravel.platform.tenant.TenantProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 座位数报价规则接口映射测试。
 *
 * <p>前端报价规则页和产品团队安排用车弹窗都会调用这些路径，测试固定接口地址，避免菜单接入后误改。</p>
 */
class VehicleQuoteRuleControllerMappingTest {

    @Test
    void shouldExposeStableVehicleQuoteRulePaths() throws NoSuchMethodException {
        VehicleQuoteRuleController controller = new VehicleQuoteRuleController(
                mock(VehicleQuoteRuleService.class),
                new TenantProperties()
        );

        assertThat(controller).isNotNull();
        RequestMapping root = VehicleQuoteRuleController.class.getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/dispatch/vehicle-quote-rules");
        assertThat(VehicleQuoteRuleController.class.getMethod("page", String.class, String.class, String.class, String.class, long.class, long.class)
                .getAnnotation(GetMapping.class)
                .value()).containsExactly("/page");
        assertThat(VehicleQuoteRuleController.class.getMethod("all", String.class)
                .getAnnotation(GetMapping.class)
                .value()).containsExactly("/all");
        assertThat(VehicleQuoteRuleController.class.getMethod("create", VehicleQuoteRuleSaveRequest.class, org.springframework.security.core.Authentication.class)
                .getAnnotation(PostMapping.class)
                .value()).containsExactly("/create");
        assertThat(VehicleQuoteRuleController.class.getMethod("update", Long.class, VehicleQuoteRuleSaveRequest.class, org.springframework.security.core.Authentication.class)
                .getAnnotation(PostMapping.class)
                .value()).containsExactly("/update");
        assertThat(VehicleQuoteRuleController.class.getMethod("delete", Long.class, org.springframework.security.core.Authentication.class)
                .getAnnotation(PostMapping.class)
                .value()).containsExactly("/delete");
        assertThat(VehicleQuoteRuleController.class.getMethod("calculate", VehicleQuoteCalculateRequest.class)
                .getAnnotation(PostMapping.class)
                .value()).containsExactly("/calculate");
    }
}

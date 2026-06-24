package com.mtravel.platform.sales.team.controller;

import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.sales.team.dto.SalesTeamListResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse;
import com.mtravel.platform.sales.team.service.SalesTeamScheduleService;
import com.mtravel.platform.tenant.TenantProperties;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 销售团队管理接口映射测试。
 *
 * <p>团队管理全局列表被前端菜单直接调用，测试固定入口路径和筛选参数传递，避免误改成产品团期路径。</p>
 */
class SalesTeamControllerMappingTest {

    @Test
    void shouldExposeGlobalTeamPageAndDelegateFilters() throws NoSuchMethodException {
        SalesTeamScheduleService service = mock(SalesTeamScheduleService.class);
        SalesTeamController controller = new SalesTeamController(service, new TenantProperties());
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 31);
        PageResult<SalesTeamListResponse> expected = new PageResult<>(List.of(), 0);
        when(service.globalPage(
                1L,
                "sanpin",
                "西湖",
                "张三",
                "杭州",
                "疗休养",
                startDate,
                endDate,
                3,
                "normal",
                2,
                50
        )).thenReturn(expected);

        var response = controller.page(
                "sanpin",
                "西湖",
                "张三",
                "杭州",
                "疗休养",
                startDate,
                endDate,
                3,
                "normal",
                2,
                50
        );

        assertThat(response.data()).isSameAs(expected);
        RequestMapping root = SalesTeamController.class.getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/sales/team");
        assertThat(SalesTeamController.class.getMethod(
                "page",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                LocalDate.class,
                LocalDate.class,
                Integer.class,
                String.class,
                long.class,
                long.class
        ).getAnnotation(GetMapping.class).value()).containsExactly("/page");
        verify(service).globalPage(
                1L,
                "sanpin",
                "西湖",
                "张三",
                "杭州",
                "疗休养",
                startDate,
                endDate,
                3,
                "normal",
                2,
                50
        );
    }

    @Test
    void shouldExposeTeamOperationDetailAndDelegateTeamId() throws NoSuchMethodException {
        SalesTeamScheduleService service = mock(SalesTeamScheduleService.class);
        SalesTeamController controller = new SalesTeamController(service, new TenantProperties());
        SalesTeamOperationResponse expected = new SalesTeamOperationResponse(
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(service.operationDetail(260561L, 1L)).thenReturn(expected);

        var response = controller.operationDetail(260561L);

        assertThat(response.data()).isSameAs(expected);
        assertThat(SalesTeamController.class.getMethod(
                "operationDetail",
                Long.class
        ).getAnnotation(GetMapping.class).value()).containsExactly("/{teamId}/operation");
        verify(service).operationDetail(260561L, 1L);
    }
}

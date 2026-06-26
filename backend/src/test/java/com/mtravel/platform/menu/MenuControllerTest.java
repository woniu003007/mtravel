package com.mtravel.platform.menu;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MenuControllerTest {

    @Test
    void menuShouldOnlyExposeDevelopedBusinessPages() {
        List<Map<String, Object>> menus = new MenuController().all().data();
        Set<String> leafPaths = leafPaths(menus);

        assertThat(leafPaths).containsExactlyInAnyOrder(
                "/workspace",
                "/customer/unit",
                "/customer/category",
                "/customer/credit",
                "/customer/contract",
                "/customer/product-auth",
                "/purchase/resource",
                "/purchase/supplier",
                "/purchase/relation",
                "/purchase/contract",
                "/sales/product",
                "/dispatch/room-status",
                "/dispatch/vehicle-quote",
                "/enterprise/company-info",
                "/enterprise/bank-account",
                "/enterprise/department",
                "/enterprise/role",
                "/enterprise/employee",
                "/enterprise/guide",
                "/enterprise/expense-item",
                "/enterprise/product-dictionary",
                "/system/config",
                "/system/risk-approval"
        );
        assertThat(menus.toString())
                .doesNotContain("/finance")
                .doesNotContain("/statistics")
                .doesNotContain("PrototypePage");
    }

    private static Set<String> leafPaths(List<Map<String, Object>> routes) {
        return routes.stream()
                .flatMap(MenuControllerTest::leafPath)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private static Stream<String> leafPath(Map<String, Object> route) {
        Object children = route.get("children");
        if (children instanceof List<?> list && !list.isEmpty()) {
            return ((List<Map<String, Object>>) children).stream()
                    .flatMap(MenuControllerTest::leafPath);
        }
        return route.containsKey("component") ? Stream.of((String) route.get("path")) : Stream.empty();
    }
}

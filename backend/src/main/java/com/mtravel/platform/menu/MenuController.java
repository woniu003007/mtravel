package com.mtravel.platform.menu;

import com.mtravel.platform.common.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuController {

    /**
     * 返回管理后台基础菜单。
     *
     * <p>当前阶段菜单仍采用内置列表，路径必须和前端真实路由保持一致，
     * 避免动态菜单模式下跳到不存在的页面。</p>
     */
    @GetMapping("/menu/all")
    public ApiResponse<List<Map<String, Object>>> all() {
        return ApiResponse.ok(List.of(
                Map.of(
                        "name", "Dashboard",
                        "path", "/dashboard",
                        "redirect", "/workspace",
                        "meta", Map.of("title", "page.dashboard.title", "order", -1),
                        "children", List.of(Map.of(
                                "name", "Workspace",
                                "path", "/workspace",
                                "component", "/dashboard/workspace/index",
                                "meta", Map.of("title", "page.dashboard.workspace", "affixTab", true)
                        ))
                ),
                Map.of(
                        "name", "Customer",
                        "path", "/customer",
                        "meta", Map.of("title", "客户管理", "icon", "lucide:users", "order", 10),
                        "children", List.of(
                                Map.of("name", "CustomerCategory", "path", "/customer/category", "component", "/customer/category/index",
                                        "meta", Map.of("title", "客户分类")),
                                Map.of("name", "CustomerUnit", "path", "/customer/unit", "component", "/customer/unit/index",
                                        "meta", Map.of("title", "客户单位")),
                                Map.of("name", "CustomerContract", "path", "/customer/contract", "component", "/customer/contract/index",
                                        "meta", Map.of("title", "合同管理"))
                        )
                )
        ));
    }
}

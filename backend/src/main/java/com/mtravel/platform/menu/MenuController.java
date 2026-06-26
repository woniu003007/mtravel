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
     * <p>当前阶段菜单仍采用内置列表，只暴露已经接入真实页面和接口的模块。
     * 前端路由模式或后端动态菜单模式切换时，都不能把 PrototypePage 原型占位页显示给业务用户。</p>
     */
    @GetMapping("/menu/all")
    public ApiResponse<List<Map<String, Object>>> all() {
        return ApiResponse.ok(List.of(
                Map.of(
                        "name", "Dashboard",
                        "path", "/dashboard",
                        "redirect", "/workspace",
                        "meta", Map.of("title", "业务工作台", "icon", "lucide:layout-dashboard", "order", -1),
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
                        "redirect", "/customer/unit",
                        "meta", Map.of("title", "客户管理", "icon", "lucide:contact", "order", 1),
                        "children", List.of(
                                Map.of("name", "CustomerUnit", "path", "/customer/unit", "component", "/customer/unit/index",
                                        "meta", Map.of("title", "客户单位", "icon", "lucide:building")),
                                Map.of("name", "CustomerCategory", "path", "/customer/category", "component", "/customer/category/index",
                                        "meta", Map.of("title", "客户分类", "icon", "lucide:tags")),
                                Map.of("name", "CustomerCredit", "path", "/customer/credit", "component", "/customer/credit/index",
                                        "meta", Map.of("title", "客户授信与实时应收", "icon", "lucide:shield-alert")),
                                Map.of("name", "CustomerContract", "path", "/customer/contract", "component", "/customer/contract/index",
                                        "meta", Map.of("title", "合同管理", "icon", "lucide:file-signature")),
                                Map.of("name", "CustomerProductAuth", "path", "/customer/product-auth", "component", "/customer/product-auth/index",
                                        "meta", Map.of("title", "产品授权", "icon", "lucide:key-round"))
                        )
                ),
                Map.of(
                        "name", "Purchase",
                        "path", "/purchase",
                        "redirect", "/purchase/resource",
                        "meta", Map.of("title", "采购管理", "icon", "lucide:shopping-bag", "order", 2),
                        "children", List.of(
                                Map.of("name", "Resource", "path", "/purchase/resource", "component", "/purchase/resource/index",
                                        "meta", Map.of("title", "资源总览", "icon", "lucide:database")),
                                Map.of("name", "Supplier", "path", "/purchase/supplier", "component", "/purchase/supplier/index",
                                        "meta", Map.of("title", "供应商管理", "icon", "lucide:truck")),
                                Map.of("name", "PurchaseRelation", "path", "/purchase/relation", "component", "/purchase/relation/index",
                                        "meta", Map.of("title", "采购关系管理", "icon", "lucide:link")),
                                Map.of("name", "PurchaseContract", "path", "/purchase/contract", "component", "/customer/contract/index",
                                        "meta", Map.of("title", "合同管理", "icon", "lucide:file-check"))
                        )
                ),
                Map.of(
                        "name", "Sales",
                        "path", "/sales",
                        "redirect", "/sales/product",
                        "meta", Map.of("title", "销售管理", "icon", "lucide:shopping-cart", "order", 3),
                        "children", List.of(
                                Map.of("name", "Product", "path", "/sales/product", "component", "/sales/product/index",
                                        "meta", Map.of("title", "产品管理", "icon", "lucide:package"))
                        )
                ),
                Map.of(
                        "name", "Dispatch",
                        "path", "/dispatch",
                        "redirect", "/dispatch/room-status",
                        "meta", Map.of("title", "计调操作", "icon", "lucide:clipboard-check", "order", 4),
                        "children", List.of(
                                Map.of("name", "RoomStatus", "path", "/dispatch/room-status", "component", "/dispatch/room-status/index",
                                        "meta", Map.of("title", "自控房源与房态库存", "icon", "lucide:bed")),
                                Map.of("name", "VehicleQuote", "path", "/dispatch/vehicle-quote", "component", "/dispatch/vehicle-quote/index",
                                        "meta", Map.of("title", "用车报价测算", "icon", "lucide:calculator"))
                        )
                ),
                Map.of(
                        "name", "Enterprise",
                        "path", "/enterprise",
                        "redirect", "/enterprise/company-info",
                        "meta", Map.of("title", "企业资料", "icon", "lucide:building-2", "order", 7),
                        "children", List.of(
                                Map.of("name", "CompanyInfo", "path", "/enterprise/company-info", "component", "/enterprise/company-info/index",
                                        "meta", Map.of("title", "公司信息", "icon", "lucide:building-2")),
                                Map.of("name", "BankAccount", "path", "/enterprise/bank-account", "component", "/enterprise/bank-account/index",
                                        "meta", Map.of("title", "银行账号", "icon", "lucide:landmark")),
                                Map.of("name", "Department", "path", "/enterprise/department", "component", "/enterprise/department/index",
                                        "meta", Map.of("title", "部门管理", "icon", "lucide:network")),
                                Map.of("name", "Role", "path", "/enterprise/role", "component", "/enterprise/role/index",
                                        "meta", Map.of("title", "角色权限", "icon", "lucide:shield")),
                                Map.of("name", "Employee", "path", "/enterprise/employee", "component", "/enterprise/employee/index",
                                        "meta", Map.of("title", "员工管理", "icon", "lucide:users")),
                                Map.of("name", "Guide", "path", "/enterprise/guide", "component", "/enterprise/guide/index",
                                        "meta", Map.of("title", "导游管理", "icon", "lucide:map-pin")),
                                Map.of("name", "ExpenseItem", "path", "/enterprise/expense-item", "component", "/enterprise/expense-item/index",
                                        "meta", Map.of("title", "费用项目", "icon", "lucide:receipt")),
                                Map.of("name", "ProductDictionary", "path", "/enterprise/product-dictionary", "component", "/enterprise/product-dictionary/index",
                                        "meta", Map.of("title", "产品字典", "icon", "lucide:list-checks"))
                        )
                ),
                Map.of(
                        "name", "System",
                        "path", "/system",
                        "redirect", "/system/config",
                        "meta", Map.of("title", "系统设置", "icon", "lucide:settings", "order", 9),
                        "children", List.of(
                                Map.of("name", "SystemConfig", "path", "/system/config", "component", "/system/config/index",
                                        "meta", Map.of("title", "系统配置", "icon", "lucide:sliders-horizontal")),
                                Map.of("name", "RiskApproval", "path", "/system/risk-approval", "component", "/system/risk-approval/index",
                                        "meta", Map.of("title", "总经理审批", "icon", "lucide:badge-check"))
                        )
                )
        ));
    }
}

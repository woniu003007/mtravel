package com.mtravel.platform.config;

import com.mtravel.platform.system.log.web.OperationLog;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import static java.util.Map.entry;

/**
 * OpenAPI 接口文档配置。
 *
 * <p>SpringDoc 会自动扫描 Controller 生成接口文档；这里统一设置系统标题、版本和
 * JWT Bearer 鉴权方案，方便 Swagger UI 直接调试受保护接口。</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String AGENT_SERVICE_TOKEN = "agentServiceToken";
    private static final String APPLICATION_JSON = "application/json";
    private static final String WILDCARD_MEDIA_TYPE = "*/*";
    private static final String BOOKING_AI_RECOGNIZE_PATH = "/sales/booking/ai-import/recognize";

    private static final Map<String, String> CONTROLLER_TAGS = Map.ofEntries(
            entry("AuthController", "认证登录"),
            entry("CommonAttachmentController", "公共能力-附件"),
            entry("ContractController", "合同管理-合同"),
            entry("CustomerCategoryController", "客户管理-客户分类"),
            entry("CustomerCreditAccountController", "客户管理-授信账户"),
            entry("CustomerProductAuthorizationController", "客户管理-产品授权"),
            entry("CustomerRiskApprovalController", "客户管理-风控审批"),
            entry("CustomerUnitController", "客户管理-客户单位"),
            entry("DispatchGuideScheduleController", "计调管理-导游排班"),
            entry("DispatchTeamArrangementController", "计调管理-团队安排"),
            entry("GuidePortalLeaveController", "导游端-请假"),
            entry("TeamGuideArrangementController", "计调管理-团队导游"),
            entry("VehicleQuoteRuleController", "计调管理-用车报价规则"),
            entry("VehicleUsageHistoryController", "计调管理-用车历史"),
            entry("EnterpriseBankAccountController", "企业资料-银行账户"),
            entry("EnterpriseCompanyInfoController", "企业资料-公司信息"),
            entry("EnterpriseDepartmentController", "企业资料-部门"),
            entry("EnterpriseEmployeeController", "企业资料-员工"),
            entry("EnterpriseExpenseItemController", "企业资料-费用项目"),
            entry("EnterpriseGuideController", "企业资料-导游"),
            entry("EnterpriseProductDictionaryController", "企业资料-产品字典"),
            entry("EnterpriseRoleController", "企业资料-角色权限"),
            entry("FinanceGuideImprestController", "财务管理-导游备用金"),
            entry("FinanceShoppingCommissionController", "财务管理-购物返佣"),
            entry("MenuController", "系统菜单"),
            entry("GroundAgentController", "采购管理-地接社资源"),
            entry("HotelResourceController", "采购管理-酒店资源"),
            entry("PurchaseRelationController", "采购管理-采购关系"),
            entry("PurchaseRelationTicketTemplateController", "采购管理-出票模板"),
            entry("PurchaseResourceController", "采购管理-资源"),
            entry("ScenicResourceController", "采购管理-景区资源"),
            entry("SupplierController", "采购管理-供应商"),
            entry("SupplierResourcePriceController", "采购管理-供应商资源价格"),
            entry("BookingAiImportController", "销售管理-订单AI导入"),
            entry("SalesBookingOrderController", "销售管理-订单"),
            entry("SalesProductController", "销售管理-产品"),
            entry("SalesTeamController", "销售管理-团队"),
            entry("SalesTeamGrossProfitController", "销售管理-团队毛利"),
            entry("SalesTeamScheduleController", "销售管理-团期"),
            entry("SystemConfigController", "系统设置-系统配置"),
            entry("OperationLogController", "系统设置-操作日志"),
            entry("UserInfoController", "用户信息")
            ,entry("AgentCustomerController", "Agent客服-客户上下文")
            ,entry("AgentProductController", "Agent客服-产品与团期")
            ,entry("AgentPolicyController", "Agent客服-业务政策")
            ,entry("AgentQuoteRequestController", "Agent客服-询价任务")
            ,entry("AgentHandoffController", "Agent客服-转人工")
    );

    private static final Map<String, String> TAG_DESCRIPTIONS = Map.ofEntries(
            entry("认证登录", "登录、登出、当前用户会话相关接口。"),
            entry("公共能力-附件", "业务附件上传、下载和关联查询接口。"),
            entry("合同管理-合同", "客户、供应商等业务合同档案维护接口。"),
            entry("客户管理-客户分类", "客户分类、状态和基础选项维护接口。"),
            entry("客户管理-授信账户", "客户授信额度、占用和账户状态维护接口。"),
            entry("客户管理-产品授权", "客户可售产品授权关系维护接口。"),
            entry("客户管理-风控审批", "客户风控申请、审批和查询接口。"),
            entry("客户管理-客户单位", "客户单位档案、联系人和结算信息维护接口。"),
            entry("计调管理-导游排班", "导游排班、占用和可用性查询接口。"),
            entry("计调管理-团队安排", "团队住宿、用车、门票、其他成本和审核安排接口。"),
            entry("导游端-请假", "导游端请假申请和查询接口。"),
            entry("计调管理-团队导游", "团队导游派遣和安排接口。"),
            entry("计调管理-用车报价规则", "车型、线路和用车报价规则维护接口。"),
            entry("计调管理-用车历史", "历史用车记录查询接口。"),
            entry("企业资料-银行账户", "企业银行账户资料维护接口。"),
            entry("企业资料-公司信息", "公司基础资料维护接口。"),
            entry("企业资料-部门", "组织部门维护接口。"),
            entry("企业资料-员工", "员工账号、角色和状态维护接口。"),
            entry("企业资料-费用项目", "成本、费用和结算项目字典维护接口。"),
            entry("企业资料-导游", "导游档案、企业码和状态维护接口。"),
            entry("企业资料-产品字典", "产品分类、线路字典和展示选项维护接口。"),
            entry("企业资料-角色权限", "角色、菜单权限和数据权限维护接口。"),
            entry("财务管理-导游备用金", "导游备用金申请、审批、付款和作废接口。"),
            entry("财务管理-购物返佣", "团队购物返佣录入、计算和作废接口。"),
            entry("系统菜单", "前端菜单和路由树接口。"),
            entry("采购管理-地接社资源", "地接社资源档案维护接口。"),
            entry("采购管理-酒店资源", "酒店资源档案维护接口。"),
            entry("采购管理-采购关系", "供应商与资源、产品之间的采购关系维护接口。"),
            entry("采购管理-出票模板", "采购关系下出票模板和票务字段维护接口。"),
            entry("采购管理-资源", "采购资源主档维护接口。"),
            entry("采购管理-景区资源", "景区资源档案维护接口。"),
            entry("采购管理-供应商", "供应商档案、联系人和合作状态维护接口。"),
            entry("采购管理-供应商资源价格", "维护采购关系下不同项目类型的门市价、同行价和团队价。"),
            entry("销售管理-订单AI导入", "订单文本识别、解析和导入预览接口。"),
            entry("销售管理-订单", "销售订单、游客、价格、占位和拼团相关接口。"),
            entry("销售管理-产品", "线路产品、行程、说明和排团相关接口。"),
            entry("销售管理-团队", "团队主档、团队操作、排团和订单归集接口。"),
            entry("销售管理-团队毛利", "团队收入、成本和毛利统计接口。"),
            entry("销售管理-团期", "产品团期和可售团期维护接口。"),
            entry("系统设置-系统配置", "系统参数、业务开关和基础配置接口。"),
            entry("系统设置-操作日志", "系统操作日志查询接口。"),
            entry("用户信息", "当前用户信息查询接口。"),
            entry("Agent客服-客户上下文", "查询客户服务状态、产品权限和默认负责人的 Agent 专用接口。"),
            entry("Agent客服-产品与团期", "查询客户已授权产品、对外行程、实时余位和客户适用价格。"),
            entry("Agent客服-业务政策", "查询已审核结构化政策，并返回是否必须人工复核。"),
            entry("Agent客服-询价任务", "幂等创建非标询价任务，并仅查询已审核的客户可见报价。"),
            entry("Agent客服-转人工", "幂等创建转人工待办，由地接系统路由负责人。")
    );

    private static final Map<String, String> PROPERTY_DESCRIPTIONS = Map.ofEntries(
            entry("id", "主键 ID"),
            entry("tenantId", "租户 ID"),
            entry("code", "业务编码或响应状态码"),
            entry("data", "接口返回数据"),
            entry("error", "错误详情"),
            entry("message", "响应消息"),
            entry("requestId", "跨系统日志追踪请求 ID"),
            entry("retryable", "调用方是否可以自动重试"),
            entry("details", "不包含服务器堆栈的字段校验详情"),
            entry("items", "分页数据列表"),
            entry("total", "总记录数"),
            entry("page", "页码，从 1 开始"),
            entry("pageSize", "每页条数，常规最大 200"),
            entry("keyword", "关键词，通常匹配名称、编号或联系人"),
            entry("status", "业务状态"),
            entry("type", "业务类型"),
            entry("name", "名称"),
            entry("title", "标题"),
            entry("remark", "备注"),
            entry("createdBy", "创建人"),
            entry("updatedBy", "最后修改人"),
            entry("deletedBy", "删除人"),
            entry("createdAt", "创建时间"),
            entry("updatedAt", "最后修改时间"),
            entry("deletedAt", "删除时间"),
            entry("relationId", "采购关系 ID"),
            entry("supplierId", "供应商 ID"),
            entry("supplierName", "供应商名称"),
            entry("resourceId", "资源 ID"),
            entry("resourceName", "资源名称"),
            entry("resourceType", "资源类型"),
            entry("resourceProjectId", "费用项目或资源项目 ID"),
            entry("projectName", "项目名称"),
            entry("marketPrice", "门市价"),
            entry("peerPrice", "同行价"),
            entry("teamPrice", "团队价"),
            entry("costPrice", "供应商自费项目成本价，统一按元/人"),
            entry("priceUnit", "计价单位代码，当前固定为 yuan_per_person"),
            entry("optionalItems", "供应商关系下的自费项目报价列表"),
            entry("priceDescription", "价格说明"),
            entry("customerId", "客户 ID"),
            entry("customerName", "客户名称"),
            entry("customerUnitId", "客户单位 ID"),
            entry("customerUnitName", "客户单位名称"),
            entry("teamId", "团队 ID"),
            entry("teamNo", "团队编号"),
            entry("teamName", "团队名称"),
            entry("teamType", "团队类型"),
            entry("productId", "产品 ID"),
            entry("scheduleId", "团期 ID"),
            entry("conversationId", "Agent 中间件服务端会话 ID"),
            entry("quoteType", "询价任务类型稳定编码"),
            entry("requirements", "按询价类型校验的结构化需求"),
            entry("sourceMessage", "触发询价的客户原始消息纯文本"),
            entry("quoteRequestId", "询价任务业务编号"),
            entry("handoffId", "转人工待办业务编号"),
            entry("reasonCode", "转人工原因稳定编码"),
            entry("priority", "待办优先级"),
            entry("summary", "供人工处理人阅读的问题摘要"),
            entry("sourceMessages", "需要随待办保存的来源聊天消息"),
            entry("messageId", "渠道会话内的消息 ID"),
            entry("senderName", "消息发送人展示名称，不作为身份凭证"),
            entry("sentAt", "消息发送时间，包含时区"),
            entry("content", "允许对外引用的纯文本内容"),
            entry("customerVisible", "人工审核后是否允许向客户展示"),
            entry("currency", "币种编码，第一期为 CNY"),
            entry("validUntil", "报价有效截止时间"),
            entry("approvedAt", "报价或政策审核通过时间"),
            entry("reviewLevel", "政策回答复核级别"),
            entry("effectiveReviewLevel", "多条有效政策聚合后的最终复核级别"),
            entry("mustHandoff", "是否必须创建转人工待办"),
            entry("answerable", "Agent 是否可以根据政策原文生成回复草稿"),
            entry("productName", "产品名称"),
            entry("orderId", "订单 ID"),
            entry("orderNo", "订单编号"),
            entry("orderStatus", "订单状态"),
            entry("startDate", "开始日期"),
            entry("endDate", "结束日期"),
            entry("departurePlace", "出发地"),
            entry("days", "行程天数"),
            entry("plannedPeople", "预控人数"),
            entry("receivedPeople", "实收人数"),
            entry("adultCount", "成人数"),
            entry("childCount", "儿童数"),
            entry("leaderCount", "领队人数"),
            entry("placeholderCount", "占位人数"),
            entry("amount", "金额"),
            entry("totalAmount", "总金额"),
            entry("receivableAmount", "应收金额"),
            entry("receivedAmount", "已收金额"),
            entry("balanceAmount", "余额"),
            entry("payableAmount", "应付金额"),
            entry("paidAmount", "已付金额"),
            entry("costAmount", "成本金额"),
            entry("grossProfit", "毛利"),
            entry("grossProfitRate", "毛利率"),
            entry("contactName", "联系人姓名"),
            entry("contactPhone", "联系人电话"),
            entry("phone", "联系电话"),
            entry("mobile", "手机号"),
            entry("province", "省份"),
            entry("city", "城市"),
            entry("district", "区县"),
            entry("address", "详细地址"),
            entry("description", "说明"),
            entry("enabled", "是否启用"),
            entry("sourceType", "识别来源类型"),
            entry("confidence", "识别置信度，0 到 1 之间"),
            entry("warnings", "识别警告或待人工核对事项"),
            entry("travelInfo", "出行交通信息草稿"),
            entry("guideInfo", "导游和接待要求草稿"),
            entry("priceInfo", "价格和费用草稿"),
            entry("additionalInfo", "附加说明草稿"),
            entry("guests", "游客名单草稿"),
            entry("moduleScores", "识别模块完整度评分"),
            entry("guestSummary", "游客名单识别摘要"),
            entry("evidence", "识别依据文本片段"),
            entry("joinDate", "参团或抵达日期"),
            entry("outboundOriginCity", "去程出发城市"),
            entry("outboundArrivalCity", "去程抵达城市"),
            entry("outboundStationName", "去程出发站点或机场"),
            entry("outboundTrafficNo", "去程车次、航班号或交通编号"),
            entry("outboundDepartureTime", "去程出发时间"),
            entry("outboundArrivalTime", "去程抵达时间"),
            entry("returnStationName", "返程站点或机场"),
            entry("returnDepartureCity", "返程出发城市"),
            entry("returnDestinationCity", "返程抵达城市"),
            entry("returnTrafficNo", "返程车次、航班号或交通编号"),
            entry("returnDepartureTime", "返程出发时间"),
            entry("returnArrivalTime", "返程抵达时间"),
            entry("guideName", "导游姓名"),
            entry("guidePhone", "导游电话"),
            entry("escortName", "全陪或领队姓名"),
            entry("receptionRequirement", "接待要求"),
            entry("adultPrice", "成人单价"),
            entry("childPrice", "儿童单价"),
            entry("seniorPrice", "老人单价"),
            entry("singleRoomDifference", "单人房差"),
            entry("priceLines", "价格明细文本"),
            entry("notes", "附加备注"),
            entry("roomingNote", "分房说明"),
            entry("leaderNote", "领队说明"),
            entry("indexNo", "游客序号"),
            entry("englishName", "英文名"),
            entry("certificateNo", "证件号码"),
            entry("gender", "性别"),
            entry("birthDate", "出生日期"),
            entry("age", "年龄"),
            entry("customerType", "游客类型"),
            entry("birthplace", "出生地"),
            entry("issueDate", "证件签发日期"),
            entry("expiryDate", "证件有效期"),
            entry("issuePlace", "证件签发地"),
            entry("roomGroup", "房间分组"),
            entry("roomingRemark", "分房备注"),
            entry("leader", "是否领队"),
            entry("suspectedLeader", "是否疑似领队"),
            entry("leaderSourceText", "领队识别来源文本"),
            entry("groupRemark", "团队备注"),
            entry("personalRemark", "个人备注"),
            entry("idCardValid", "身份证号码是否通过格式校验"),
            entry("travelScore", "交通信息完整度评分"),
            entry("guideScore", "导游信息完整度评分"),
            entry("customerScore", "客户信息完整度评分"),
            entry("priceScore", "价格信息完整度评分"),
            entry("additionalScore", "附加说明完整度评分"),
            entry("guestListScore", "游客名单完整度评分"),
            entry("guestCount", "识别到的游客人数"),
            entry("invalidIdCardCount", "身份证校验失败人数"),
            entry("missingRequiredCount", "缺少必填信息人数"),
            entry("suspectedMissingCount", "疑似漏识别人数")
    );

    private static final Map<String, String> PARAMETER_DESCRIPTIONS = Map.ofEntries(
            entry("id", "主键 ID"),
            entry("relationId", "采购关系 ID，不传则查询当前租户下全部关系价格"),
            entry("supplierId", "供应商 ID"),
            entry("customerId", "客户 ID"),
            entry("teamId", "团队 ID"),
            entry("orderId", "订单 ID"),
            entry("productId", "产品 ID"),
            entry("scheduleId", "团期 ID"),
            entry("quoteRequestId", "询价任务业务编号"),
            entry("topic", "结构化政策主题编码"),
            entry("onDate", "政策生效性判断日期"),
            entry("from", "团期查询开始日期"),
            entry("to", "团期查询结束日期"),
            entry("keyword", "关键词，通常匹配名称、编号或联系人"),
            entry("status", "业务状态"),
            entry("type", "业务类型"),
            entry("resourceType", "资源类型"),
            entry("city", "城市"),
            entry("page", "页码，从 1 开始"),
            entry("pageSize", "每页条数，常规最大 200")
    );

    /**
     * 定义 MTravel 后端 OpenAPI 元信息和全局 JWT 授权方案。
     *
     * @return OpenAPI 文档配置
     */
    @Bean
    public OpenAPI mtravelOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("旅游接待管理系统 API")
                        .version("0.1.0")
                        .description("MTravel 旅游地接业务数字化管理平台后端接口文档。")
                        .license(new License().name("Private")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("管理后台用户 JWT"))
                        .addSecuritySchemes(AGENT_SERVICE_TOKEN, new SecurityScheme()
                                .name(AGENT_SERVICE_TOKEN)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Service Token")
                                .description("Agent 中间件专用服务 Token，绑定租户和最小 Scope")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    /**
     * 根据 Controller 和操作日志生成中文接口分组、摘要和参数说明。
     *
     * @return SpringDoc 操作级定制器
     */
    @Bean
    public OperationCustomizer chineseOperationCustomizer() {
        return (operation, handlerMethod) -> {
            String tagName = tagName(handlerMethod);
            operation.setTags(List.of(tagName));
            if (handlerMethod.getBeanType().getPackageName().startsWith("com.mtravel.platform.agent.")) {
                operation.setSecurity(List.of(new SecurityRequirement().addList(AGENT_SERVICE_TOKEN)));
            }

            if (!hasText(operation.getSummary())) {
                operation.setSummary(summaryFor(tagName, handlerMethod));
            }
            if (!hasText(operation.getDescription())) {
                operation.setDescription(descriptionFor(tagName, operation.getSummary()));
            }
            applyParameterDescriptions(operation);
            applyRequestBodyDescription(operation, tagName);
            return operation;
        };
    }

    /**
     * 为未单独标注的常用 DTO 字段补充中文说明，并注册中文标签描述。
     *
     * @return SpringDoc 文档级定制器
     */
    @Bean
    public OpenApiCustomizer chineseSchemaDescriptionCustomizer() {
        return openApi -> {
            Map<String, Tag> tags = new LinkedHashMap<>();
            if (openApi.getTags() != null) {
                for (Tag tag : openApi.getTags()) {
                    tags.put(tag.getName(), tag);
                }
            }
            TAG_DESCRIPTIONS.forEach((name, description) ->
                    tags.putIfAbsent(name, new Tag().name(name).description(description)));
            openApi.setTags(new ArrayList<>(tags.values()));

            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().values().forEach(this::applySchemaDescriptions);
            }
            normalizeJsonResponseContent(openApi);
            applyCuratedResponseExamples(openApi);
        };
    }

    private String tagName(HandlerMethod handlerMethod) {
        String controllerName = handlerMethod.getBeanType().getSimpleName();
        OperationLog operationLog = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), OperationLog.class);
        return CONTROLLER_TAGS.getOrDefault(
                controllerName,
                operationLog == null ? "系统接口" : operationLog.module()
        );
    }

    private String summaryFor(String tagName, HandlerMethod handlerMethod) {
        OperationLog operationLog = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), OperationLog.class);
        String resourceName = resourceName(tagName);
        String methodName = handlerMethod.getMethod().getName();
        return switch (methodName) {
            case "page" -> "分页查询" + resourceName;
            case "list", "all", "options" -> "查询" + resourceName + "列表";
            case "detail" -> "查询" + resourceName + "详情";
            case "create" -> "新增" + resourceName;
            case "update" -> "修改" + resourceName;
            case "delete" -> "删除" + resourceName;
            case "disable" -> "停用" + resourceName;
            case "enable" -> "启用" + resourceName;
            default -> operationLog == null ? methodName : operationLog.type() + resourceName;
        };
    }

    private String descriptionFor(String tagName, String summary) {
        String tagDescription = TAG_DESCRIPTIONS.get(tagName);
        if (!hasText(tagDescription)) {
            return summary;
        }
        return tagDescription + "当前接口：" + summary + "。";
    }

    private String resourceName(String tagName) {
        int index = tagName.lastIndexOf('-');
        return index >= 0 && index + 1 < tagName.length() ? tagName.substring(index + 1) : tagName;
    }

    private void applyParameterDescriptions(Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters == null) {
            return;
        }
        for (Parameter parameter : parameters) {
            if (!hasText(parameter.getDescription())) {
                parameter.setDescription(PARAMETER_DESCRIPTIONS.getOrDefault(parameter.getName(), parameter.getName()));
            }
        }
    }

    private void applyRequestBodyDescription(Operation operation, String tagName) {
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody != null && !hasText(requestBody.getDescription())) {
            requestBody.setDescription(resourceName(tagName) + "请求体");
        }
    }

    private void applySchemaDescriptions(Schema<?> schema) {
        if (schema == null) {
            return;
        }
        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            properties.forEach((propertyName, propertySchema) -> {
                if (propertySchema != null && !hasText(propertySchema.getDescription())) {
                    propertySchema.setDescription(PROPERTY_DESCRIPTIONS.getOrDefault(propertyName, propertyName));
                }
                applySchemaDescriptions(propertySchema);
            });
        }
        if (schema.getItems() != null) {
            applySchemaDescriptions(schema.getItems());
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(this::applySchemaDescriptions);
        }
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(this::applySchemaDescriptions);
        }
        if (schema.getAnyOf() != null) {
            schema.getAnyOf().forEach(this::applySchemaDescriptions);
        }
    }

    private void normalizeJsonResponseContent(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
            if (operation.getResponses() == null) {
                return;
            }
            operation.getResponses().values().forEach(response -> {
                Content content = response.getContent();
                if (content == null || !content.containsKey(WILDCARD_MEDIA_TYPE) || content.containsKey(APPLICATION_JSON)) {
                    return;
                }
                MediaType wildcard = content.remove(WILDCARD_MEDIA_TYPE);
                content.addMediaType(APPLICATION_JSON, wildcard);
            });
        }));
    }

    private void applyCuratedResponseExamples(OpenAPI openApi) {
        if (openApi.getPaths() == null || !openApi.getPaths().containsKey(BOOKING_AI_RECOGNIZE_PATH)) {
            return;
        }
        Operation operation = openApi.getPaths().get(BOOKING_AI_RECOGNIZE_PATH).getPost();
        if (operation == null || operation.getResponses() == null || operation.getResponses().get("200") == null) {
            return;
        }
        Content content = operation.getResponses().get("200").getContent();
        if (content == null || content.get(APPLICATION_JSON) == null) {
            return;
        }
        content.get(APPLICATION_JSON).addExamples("recognizeSuccess", new Example()
                .summary("精简识别结果示例")
                .description("只展示前端联调最常看的识别结果字段，完整字段请看 Schema。")
                .value(bookingAiRecognizeResponseExample()));
    }

    private Map<String, Object> bookingAiRecognizeResponseExample() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sourceType", "text");
        data.put("confidence", 0.86);
        data.put("warnings", List.of("返程交通信息未识别完整，请人工核对。"));
        data.put("travelInfo", Map.of(
                "joinDate", "2026-07-20",
                "outboundArrivalCity", "南京",
                "outboundStationName", "南京南站",
                "outboundTrafficNo", "G7001",
                "outboundArrivalTime", "09:00"
        ));
        data.put("customerInfo", Map.of(
                "customerName", "南京某旅行社",
                "contactName", "王经理",
                "contactPhone", "13800000000"
        ));
        data.put("priceInfo", Map.of(
                "adultPrice", "1000",
                "singleRoomDifference", "300"
        ));
        data.put("guests", List.of(Map.of(
                "indexNo", 1,
                "name", "张三",
                "certificateNo", "320102199001011234",
                "customerType", "adult",
                "idCardValid", true
        )));
        data.put("guestSummary", Map.of(
                "guestCount", 2,
                "invalidIdCardCount", 0,
                "missingRequiredCount", 0,
                "suspectedMissingCount", 0
        ));

        Map<String, Object> example = new LinkedHashMap<>();
        example.put("code", 0);
        example.put("data", data);
        example.put("error", null);
        example.put("message", "ok");
        return example;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

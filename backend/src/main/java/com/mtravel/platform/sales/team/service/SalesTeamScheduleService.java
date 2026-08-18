package com.mtravel.platform.sales.team.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import com.mtravel.platform.dispatch.guide.enums.DispatchTeamGuideStatus;
import com.mtravel.platform.dispatch.guide.mapper.DispatchTeamGuideMapper;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementOrderAllocationEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementPriceLineEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementSectionStatusEntity;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementSettlementType;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementOrderAllocationMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementPriceLineMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementSectionStatusMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingTeamDraftResponse;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.booking.order.service.SalesBookingOrderService;
import com.mtravel.platform.sales.product.entity.SalesProductArrangementItemEntity;
import com.mtravel.platform.sales.product.entity.SalesProductArrangementPriceLineEntity;
import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import com.mtravel.platform.sales.product.entity.SalesProductRoadbookPointEntity;
import com.mtravel.platform.sales.product.dto.SalesProductItineraryDayResponse;
import com.mtravel.platform.sales.product.dto.SalesProductRoadbookPointResponse;
import com.mtravel.platform.sales.product.enums.SalesProductDomesticType;
import com.mtravel.platform.sales.product.enums.SalesProductStatus;
import com.mtravel.platform.sales.product.enums.SalesProductTripType;
import com.mtravel.platform.sales.product.mapper.SalesProductDescriptionMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductItineraryDayMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductRoadbookPointMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductArrangementItemMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductArrangementPriceLineMapper;
import com.mtravel.platform.sales.team.dto.SalesTeamBatchEditRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamBatchCreateRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamDirectEditResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamDirectCreateRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamDisplayNameFormatter;
import com.mtravel.platform.sales.team.dto.SalesTeamListResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamPriceResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamPriceSaveRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamSaveRequest;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamListSummaryEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamNoLogEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamPriceEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamStatusLogEntity;
import com.mtravel.platform.sales.team.enums.SalesTeamPriceStatus;
import com.mtravel.platform.sales.team.enums.SalesTeamStatus;
import com.mtravel.platform.sales.team.enums.SalesTeamStatusAction;
import com.mtravel.platform.sales.team.enums.SalesTeamType;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamListSummaryMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamNoLogMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamPriceMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamStatusLogMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 销售团期管理业务服务。
 *
 * <p>本服务负责产品下正式团队的生成、价格维护和状态流转。产品只是线路模板，批量生成团期后才会
 * 形成可收客的 sales_teams 数据；同一团队不同客户类型价格保存在 sales_team_prices。</p>
 */
@Service
public class SalesTeamScheduleService {

    private static final String DEFAULT_TEAM_NO_PREFIX = "CS-SP-BK";
    private static final int PRODUCT_NAME_MAX_LENGTH = 200;
    private static final String PRODUCT_SCOPE_TEAM_SNAPSHOT = "team_snapshot";
    private static final DateTimeFormatter TEAM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private static final Pattern TEAM_SUFFIX_PATTERN = Pattern.compile("([A-Z](?:\\d+)?)$");
    private static final String TEAM_PROFILE_MARKER = "[[TEAM_PROFILE_JSON]]";
    private static final Pattern TEAM_PROFILE_STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final String ARRANGE_STATUS_NONE = "none";
    private static final String ARRANGE_STATUS_PENDING = "pending";
    private static final String ARRANGE_STATUS_CONFIRMED = "confirmed";
    private static final String ARRANGEMENT_STATUS_ACTIVE = "active";

    private final SalesProductMapper productMapper;
    private final SalesProductDescriptionMapper descriptionMapper;
    private final SalesProductItineraryDayMapper itineraryDayMapper;
    private final SalesProductRoadbookPointMapper roadbookPointMapper;
    private final SalesProductArrangementItemMapper productArrangementMapper;
    private final SalesProductArrangementPriceLineMapper productArrangementPriceLineMapper;
    private final SalesTeamMapper teamMapper;
    private final SalesTeamListSummaryMapper teamListSummaryMapper;
    private final SalesTeamPriceMapper priceMapper;
    private final SalesTeamStatusLogMapper statusLogMapper;
    private final SalesTeamNoLogMapper noLogMapper;
    private final SalesBookingOrderService bookingOrderService;
    private final SalesBookingOrderMapper bookingOrderMapper;
    private final DispatchTeamArrangementMapper teamArrangementMapper;
    private final DispatchTeamArrangementPriceLineMapper teamArrangementPriceLineMapper;
    private final DispatchTeamArrangementOrderAllocationMapper teamArrangementAllocationMapper;
    private final DispatchTeamArrangementSectionStatusMapper teamArrangementSectionStatusMapper;
    private final DispatchTeamGuideMapper teamGuideMapper;
    private final SalesTeamListSummaryRefreshService teamListSummaryRefreshService;

    /**
     * 测试专用兼容构造器。
     *
     * <p>单元测试只关注团队和价格行为，团号日志 Mapper 可为空；Spring 运行时使用带日志 Mapper 的构造器。</p>
     */
    public SalesTeamScheduleService(
            SalesProductMapper productMapper,
            SalesTeamMapper teamMapper,
            SalesTeamPriceMapper priceMapper,
            SalesTeamStatusLogMapper statusLogMapper
    ) {
        this(productMapper, null, null, null, null, null, teamMapper, null, priceMapper, statusLogMapper, null, null, null, null, null, null, null, null, null);
    }

    /**
     * 测试专用兼容构造器，允许团队操作页测试注入产品说明 Mapper。
     */
    public SalesTeamScheduleService(
            SalesProductMapper productMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesTeamMapper teamMapper,
            SalesTeamPriceMapper priceMapper,
            SalesTeamStatusLogMapper statusLogMapper
    ) {
        this(productMapper, descriptionMapper, null, null, null, null, teamMapper, null, priceMapper, statusLogMapper, null, null, null, null, null, null, null, null, null);
    }

    /**
     * 测试专用兼容构造器，允许团队操作页测试注入产品说明和每日行程 Mapper。
     */
    public SalesTeamScheduleService(
            SalesProductMapper productMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductItineraryDayMapper itineraryDayMapper,
            SalesTeamMapper teamMapper,
            SalesTeamPriceMapper priceMapper,
            SalesTeamStatusLogMapper statusLogMapper
    ) {
        this(productMapper, descriptionMapper, itineraryDayMapper, null, null, null, teamMapper, null, priceMapper, statusLogMapper, null, null, null, null, null, null, null, null, null);
    }

    /**
     * 测试专用兼容构造器，允许直接建团测试注入路书地点 Mapper。
     */
    public SalesTeamScheduleService(
            SalesProductMapper productMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductItineraryDayMapper itineraryDayMapper,
            SalesProductRoadbookPointMapper roadbookPointMapper,
            SalesTeamMapper teamMapper,
            SalesTeamPriceMapper priceMapper,
            SalesTeamStatusLogMapper statusLogMapper
    ) {
        this(productMapper, descriptionMapper, itineraryDayMapper, roadbookPointMapper, null, null, teamMapper, null, priceMapper, statusLogMapper, null, null, null, null, null, null, null, null, null);
    }

    /**
     * 运行时构造器，注入团队主表、价格表、状态日志和团号日志访问对象。
     */
    public SalesTeamScheduleService(
            SalesProductMapper productMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductItineraryDayMapper itineraryDayMapper,
            SalesProductRoadbookPointMapper roadbookPointMapper,
            SalesProductArrangementItemMapper productArrangementMapper,
            SalesProductArrangementPriceLineMapper productArrangementPriceLineMapper,
            SalesTeamMapper teamMapper,
            SalesTeamPriceMapper priceMapper,
            SalesTeamStatusLogMapper statusLogMapper,
            SalesTeamNoLogMapper noLogMapper,
            SalesBookingOrderService bookingOrderService,
            DispatchTeamArrangementMapper teamArrangementMapper,
            DispatchTeamArrangementPriceLineMapper teamArrangementPriceLineMapper,
            DispatchTeamArrangementOrderAllocationMapper teamArrangementAllocationMapper
    ) {
        this(
                productMapper,
                descriptionMapper,
                itineraryDayMapper,
                roadbookPointMapper,
                productArrangementMapper,
                productArrangementPriceLineMapper,
                teamMapper,
                null,
                priceMapper,
                statusLogMapper,
                noLogMapper,
                bookingOrderService,
                null,
                teamArrangementMapper,
                teamArrangementPriceLineMapper,
                teamArrangementAllocationMapper,
                null,
                null,
                null
        );
    }

    /**
     * 运行时构造器，注入团队主表、订单主表、价格表、状态日志和团号日志访问对象。
     */
    public SalesTeamScheduleService(
            SalesProductMapper productMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductItineraryDayMapper itineraryDayMapper,
            SalesProductRoadbookPointMapper roadbookPointMapper,
            SalesProductArrangementItemMapper productArrangementMapper,
            SalesProductArrangementPriceLineMapper productArrangementPriceLineMapper,
            SalesTeamMapper teamMapper,
            SalesTeamPriceMapper priceMapper,
            SalesTeamStatusLogMapper statusLogMapper,
            SalesTeamNoLogMapper noLogMapper,
            SalesBookingOrderService bookingOrderService,
            SalesBookingOrderMapper bookingOrderMapper,
            DispatchTeamArrangementMapper teamArrangementMapper,
            DispatchTeamArrangementPriceLineMapper teamArrangementPriceLineMapper,
            DispatchTeamArrangementOrderAllocationMapper teamArrangementAllocationMapper
    ) {
        this(
                productMapper,
                descriptionMapper,
                itineraryDayMapper,
                roadbookPointMapper,
                productArrangementMapper,
                productArrangementPriceLineMapper,
                teamMapper,
                null,
                priceMapper,
                statusLogMapper,
                noLogMapper,
                bookingOrderService,
                bookingOrderMapper,
                teamArrangementMapper,
                teamArrangementPriceLineMapper,
                teamArrangementAllocationMapper,
                null,
                null,
                null
        );
    }

    /**
     * 运行时构造器，注入团队主表、价格表、状态日志、团号日志和团队执行安排访问对象。
     */
    public SalesTeamScheduleService(
            SalesProductMapper productMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductItineraryDayMapper itineraryDayMapper,
            SalesProductRoadbookPointMapper roadbookPointMapper,
            SalesProductArrangementItemMapper productArrangementMapper,
            SalesProductArrangementPriceLineMapper productArrangementPriceLineMapper,
            SalesTeamMapper teamMapper,
            SalesTeamListSummaryMapper teamListSummaryMapper,
            SalesTeamPriceMapper priceMapper,
            SalesTeamStatusLogMapper statusLogMapper,
            SalesTeamNoLogMapper noLogMapper,
            SalesBookingOrderService bookingOrderService,
            SalesBookingOrderMapper bookingOrderMapper,
            DispatchTeamArrangementMapper teamArrangementMapper,
            DispatchTeamArrangementPriceLineMapper teamArrangementPriceLineMapper,
            DispatchTeamArrangementOrderAllocationMapper teamArrangementAllocationMapper,
            DispatchTeamArrangementSectionStatusMapper teamArrangementSectionStatusMapper,
            DispatchTeamGuideMapper teamGuideMapper
    ) {
        this(
                productMapper,
                descriptionMapper,
                itineraryDayMapper,
                roadbookPointMapper,
                productArrangementMapper,
                productArrangementPriceLineMapper,
                teamMapper,
                teamListSummaryMapper,
                priceMapper,
                statusLogMapper,
                noLogMapper,
                bookingOrderService,
                bookingOrderMapper,
                teamArrangementMapper,
                teamArrangementPriceLineMapper,
                teamArrangementAllocationMapper,
                teamArrangementSectionStatusMapper,
                teamGuideMapper,
                null
        );
    }

    /**
     * 运行时构造器，注入团队主表、价格表、状态日志、团号日志和团队执行安排访问对象。
     */
    @Autowired
    public SalesTeamScheduleService(
            SalesProductMapper productMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductItineraryDayMapper itineraryDayMapper,
            SalesProductRoadbookPointMapper roadbookPointMapper,
            SalesProductArrangementItemMapper productArrangementMapper,
            SalesProductArrangementPriceLineMapper productArrangementPriceLineMapper,
            SalesTeamMapper teamMapper,
            SalesTeamListSummaryMapper teamListSummaryMapper,
            SalesTeamPriceMapper priceMapper,
            SalesTeamStatusLogMapper statusLogMapper,
            SalesTeamNoLogMapper noLogMapper,
            SalesBookingOrderService bookingOrderService,
            SalesBookingOrderMapper bookingOrderMapper,
            DispatchTeamArrangementMapper teamArrangementMapper,
            DispatchTeamArrangementPriceLineMapper teamArrangementPriceLineMapper,
            DispatchTeamArrangementOrderAllocationMapper teamArrangementAllocationMapper,
            DispatchTeamArrangementSectionStatusMapper teamArrangementSectionStatusMapper,
            DispatchTeamGuideMapper teamGuideMapper,
            SalesTeamListSummaryRefreshService teamListSummaryRefreshService
    ) {
        this.productMapper = productMapper;
        this.descriptionMapper = descriptionMapper;
        this.itineraryDayMapper = itineraryDayMapper;
        this.roadbookPointMapper = roadbookPointMapper;
        this.productArrangementMapper = productArrangementMapper;
        this.productArrangementPriceLineMapper = productArrangementPriceLineMapper;
        this.teamMapper = teamMapper;
        this.teamListSummaryMapper = teamListSummaryMapper;
        this.priceMapper = priceMapper;
        this.statusLogMapper = statusLogMapper;
        this.noLogMapper = noLogMapper;
        this.bookingOrderService = bookingOrderService;
        this.bookingOrderMapper = bookingOrderMapper;
        this.teamArrangementMapper = teamArrangementMapper;
        this.teamArrangementPriceLineMapper = teamArrangementPriceLineMapper;
        this.teamArrangementAllocationMapper = teamArrangementAllocationMapper;
        this.teamArrangementSectionStatusMapper = teamArrangementSectionStatusMapper;
        this.teamGuideMapper = teamGuideMapper;
        this.teamListSummaryRefreshService = teamListSummaryRefreshService;
    }

    /**
     * 分页查询产品下的团期。
     *
     * <p>先分页查询团队主表，再一次性批量查询当前页团队的价格行，避免按行 N+1 查询。</p>
     *
     * @param tenantId 当前租户 ID
     * @param productId 产品 ID
     * @param startDate 发团开始日期
     * @param endDate 发团结束日期
     * @param status 团队状态
     * @param keyword 团号或操作计调关键字
     * @param page 当前页
     * @param pageSize 每页数量
     * @return 团期分页结果
     */
    public PageResult<SalesTeamResponse> page(
            Long tenantId,
            Long productId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String keyword,
            long page,
            long pageSize
    ) {
        QueryWrapper<SalesTeamEntity> wrapper = baseTeamQuery(tenantId)
                .eq(productId != null, "product_id", productId)
                .ge(startDate != null, "departure_date", startDate)
                .le(endDate != null, "departure_date", endDate)
                .eq(StringUtils.hasText(status), "status", status)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("team_no", clean(keyword))
                        .or()
                        .like("operator_employee_name", clean(keyword)))
                .orderByAsc("departure_date")
                .orderByAsc("team_no");
        Page<SalesTeamEntity> result = teamMapper.selectPage(Page.of(page, pageSize), wrapper);
        if (result.getRecords().isEmpty()) {
            return new PageResult<>(List.of(), result.getTotal());
        }
        Map<Long, List<SalesTeamPriceResponse>> prices = loadPricesForTeams(tenantId, result.getRecords());
        List<SalesTeamResponse> items = result.getRecords().stream()
                .map(team -> SalesTeamResponse.fromEntity(team, prices.getOrDefault(team.getId(), List.of())))
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /**
     * 分页查询销售团队全局列表。
     *
     * <p>该列表对应老系统“销售管理 / 团队管理”，查询粒度是正式团队，而不是某个产品下的团期。
     * 当前页团队查出后只批量补充产品名称、业务类型、天数和出发地，不加载客户类型价格，避免列表页
     * 因价格明细产生 N+1 查询或响应体过大。</p>
     *
     * @param tenantId 当前租户 ID
     * @param teamType 团队类型，散拼、整团、散团、单项等
     * @param keyword 团号、团队名称或备注关键字
     * @param customerKeyword 客户单位关键字
     * @param operatorKeyword 操作计调关键字
     * @param departurePlace 出发地关键字
     * @param businessType 业务类型
     * @param startDate 出团开始日期
     * @param endDate 出团结束日期
     * @param travelDays 行程天数
     * @param teamStatus 团队状态或日期状态
     * @param page 当前页
     * @param pageSize 每页数量
     * @return 团队管理分页结果
     */
    public PageResult<SalesTeamListResponse> globalPage(
            Long tenantId,
            String teamType,
            String keyword,
            String customerKeyword,
            String operatorKeyword,
            String departurePlace,
            String businessType,
            LocalDate startDate,
            LocalDate endDate,
            Integer travelDays,
            String teamStatus,
            long page,
            long pageSize
    ) {
        return globalPage(
                tenantId,
                teamType,
                keyword,
                customerKeyword,
                operatorKeyword,
                departurePlace,
                businessType,
                startDate,
                endDate,
                travelDays,
                teamStatus,
                null,
                null,
                null,
                null,
                page,
                pageSize
        );
    }

    /**
     * 分页查询销售团队全局列表，包含高级筛选。
     *
     * @param guideKeyword 导游姓名或手机号关键字
     * @param departmentName 团队归属部门名称
     * @param orderStatus 订单状态；none 表示无有效订单
     * @param addDate 团队添加日期
     */
    public PageResult<SalesTeamListResponse> globalPage(
            Long tenantId,
            String teamType,
            String keyword,
            String customerKeyword,
            String operatorKeyword,
            String departurePlace,
            String businessType,
            LocalDate startDate,
            LocalDate endDate,
            Integer travelDays,
            String teamStatus,
            String guideKeyword,
            String departmentName,
            String orderStatus,
            LocalDate addDate,
            long page,
            long pageSize
    ) {
        if (teamListSummaryMapper != null) {
            return globalPageFromSummary(
                    tenantId,
                    teamType,
                    keyword,
                    customerKeyword,
                    operatorKeyword,
                    departurePlace,
                    businessType,
                    startDate,
                    endDate,
                    travelDays,
                    teamStatus,
                    guideKeyword,
                    departmentName,
                    orderStatus,
                    addDate,
                    page,
                    pageSize
            );
        }
        QueryWrapper<SalesTeamEntity> wrapper = baseTeamQuery(tenantId)
                .eq(StringUtils.hasText(teamType), "team_type", clean(teamType))
                .like(StringUtils.hasText(operatorKeyword), "operator_employee_name", clean(operatorKeyword))
                .like(StringUtils.hasText(departmentName), "department_name", clean(departmentName))
                .ge(startDate != null, "departure_date", startDate)
                .le(endDate != null, "departure_date", endDate);
        applyGlobalAddDateFilter(wrapper, addDate);
        applyGlobalStatusFilter(wrapper, teamStatus);
        applyGlobalGuideFilter(wrapper, tenantId, guideKeyword);
        applyGlobalOrderStatusFilter(wrapper, tenantId, orderStatus);
        applyGlobalProductFilters(wrapper, tenantId, keyword, departurePlace, businessType, travelDays);
        applyGlobalCustomerFilter(wrapper, tenantId, customerKeyword);
        wrapper.orderByDesc("created_at").orderByAsc("team_no");

        Page<SalesTeamEntity> result = teamMapper.selectPage(Page.of(page, Math.min(pageSize, 200)), wrapper);
        if (result.getRecords().isEmpty()) {
            return new PageResult<>(List.of(), result.getTotal());
        }
        Map<Long, SalesProductEntity> products = loadProductsForTeams(tenantId, result.getRecords());
        List<DispatchTeamGuideEntity> activeGuides = loadActiveGuidesForTeams(tenantId, result.getRecords());
        Map<Long, SalesTeamListResponse.ArrangePlans> plans = loadArrangePlansForTeams(tenantId, result.getRecords(), activeGuides);
        Map<Long, String> customerSummaries = loadCustomerSummariesForTeams(tenantId, result.getRecords());
        Map<Long, String> guideSummaries = buildGuideSummaries(activeGuides);
        List<SalesTeamListResponse> items = result.getRecords().stream()
                .map(team -> SalesTeamListResponse.fromEntity(
                        team,
                        products.get(team.getProductId()),
                        plans.get(team.getId()),
                        customerSummaries.get(team.getId()),
                        guideSummaries.get(team.getId())))
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /**
     * 从团队列表汇总表分页查询团队管理列表。
     *
     * <p>该路径是千万级数据量下的正式查询路径，只访问 sales_team_list_summaries，避免实时联查产品、订单、导游和安排表。</p>
     */
    private PageResult<SalesTeamListResponse> globalPageFromSummary(
            Long tenantId,
            String teamType,
            String keyword,
            String customerKeyword,
            String operatorKeyword,
            String departurePlace,
            String businessType,
            LocalDate startDate,
            LocalDate endDate,
            Integer travelDays,
            String teamStatus,
            String guideKeyword,
            String departmentName,
            String orderStatus,
            LocalDate addDate,
            long page,
            long pageSize
    ) {
        QueryWrapper<SalesTeamListSummaryEntity> wrapper = new QueryWrapper<SalesTeamListSummaryEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq(StringUtils.hasText(teamType), "team_type", clean(teamType))
                .eq(StringUtils.hasText(businessType), "business_type", clean(businessType))
                .eq(travelDays != null, "travel_days", travelDays)
                .ge(startDate != null, "departure_date", startDate)
                .le(endDate != null, "departure_date", endDate)
                .like(StringUtils.hasText(operatorKeyword), "operator_employee_name", clean(operatorKeyword))
                .eq(StringUtils.hasText(departmentName), "department_name", clean(departmentName));
        applySummaryStatusFilter(wrapper, teamStatus);
        if (addDate != null) {
            wrapper.apply("created_at::date = {0}", addDate);
        }
        if (StringUtils.hasText(keyword)) {
            String value = clean(keyword);
            wrapper.and(nested -> nested
                    .like("team_no", value)
                    .or()
                    .like("team_name", value)
                    .or()
                    .like("remark", value));
        }
        if (StringUtils.hasText(customerKeyword)) {
            String value = clean(customerKeyword);
            wrapper.and(nested -> nested
                    .like("customer_summary", value)
                    .or()
                    .like("salesperson_summary", value));
        }
        if (StringUtils.hasText(departurePlace)) {
            wrapper.like("departure_place", clean(departurePlace));
        }
        if (StringUtils.hasText(guideKeyword)) {
            wrapper.like("guide_summary", clean(guideKeyword));
        }
        if (StringUtils.hasText(orderStatus) && !"all".equals(orderStatus)) {
            if ("none".equals(orderStatus)) {
                wrapper.and(nested -> nested.isNull("order_status_summary").or().eq("order_status_summary", ""));
            } else {
                wrapper.like("order_status_summary", clean(orderStatus));
            }
        }
        wrapper.orderByDesc("created_at").orderByAsc("team_no");
        Page<SalesTeamListSummaryEntity> result = teamListSummaryMapper.selectPage(Page.of(page, Math.min(pageSize, 200)), wrapper);
        List<SalesTeamListResponse> items = result.getRecords().stream()
                .map(SalesTeamListResponse::fromSummary)
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /** 按团队列表页状态下拉语义过滤汇总表。 */
    private void applySummaryStatusFilter(QueryWrapper<SalesTeamListSummaryEntity> wrapper, String teamStatus) {
        if (!StringUtils.hasText(teamStatus) || "all".equals(teamStatus)) {
            return;
        }
        if ("not_departed".equals(teamStatus)) {
            wrapper.ne("status", SalesTeamStatus.CANCELLED.getValue())
                    .ge("departure_date", LocalDate.now());
            return;
        }
        if ("departed".equals(teamStatus)) {
            wrapper.ne("status", SalesTeamStatus.CANCELLED.getValue())
                    .lt("departure_date", LocalDate.now());
            return;
        }
        wrapper.eq("status", clean(teamStatus));
    }

    /**
     * 查询团队操作页只读详情。
     *
     * <p>团队操作页是正式团队执行入口，第一版只聚合团队主表、产品基础资料、产品说明和客户类型价格。
     * 订单、导游报账、打印、拼团转团等后续链路不在这里做假数据，前端按空列表或待接入按钮展示。</p>
     *
     * @param teamId 团队 ID
     * @param tenantId 当前租户 ID
     * @return 团队操作页详情
     */
    public SalesTeamOperationResponse operationDetail(Long teamId, Long tenantId) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        SalesProductEntity product = null;
        if (team.getProductId() != null) {
            product = productMapper.selectOne(baseProductQuery(tenantId).eq("id", team.getProductId()));
        }
        SalesProductDescriptionEntity description = null;
        if (descriptionMapper != null && team.getProductId() != null) {
            description = descriptionMapper.selectOne(baseDescriptionQuery(tenantId).eq("product_id", team.getProductId()));
        }
        List<SalesProductItineraryDayEntity> itineraryDays = loadProductItineraryDays(tenantId, team.getProductId());
        List<SalesTeamPriceResponse> prices = loadPricesForTeams(tenantId, List.of(team))
                .getOrDefault(team.getId(), List.of());
        List<SalesBookingOrderEntity> orders = bookingOrderService == null
                ? List.of()
                : bookingOrderService.listOrdersByTeam(teamId, tenantId);
        List<SalesTeamOperationResponse.OrderRow> orderRows = bookingOrderService == null
                ? List.of()
                : bookingOrderService.toOperationRows(orders);
        String guideSummary = buildGuideSummaries(loadActiveGuidesForTeams(tenantId, List.of(team))).get(team.getId());
        String leaderSummary = bookingOrderService == null ? null : bookingOrderService.operationLeaderSummary(tenantId, orders);
        return SalesTeamOperationResponse.from(
                team,
                product,
                description,
                prices,
                itineraryDays,
                orderRows,
                guideSummary,
                leaderSummary
        );
    }

    /**
     * 查询收客订单编辑页所需的轻量团队草稿。
     *
     * <p>订单编辑页只需要团队基础信息、产品说明、行程和团队价格，不能复用团队操作页完整详情，
     * 否则会额外加载订单列表、拼团关系和导游汇总，导致打开订单页面等待时间过长。</p>
     *
     * @param teamId 团队 ID
     * @param tenantId 当前租户 ID
     * @return 收客订单页面团队草稿
     */
    public SalesBookingTeamDraftResponse bookingTeamDraft(Long teamId, Long tenantId) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        SalesProductEntity product = null;
        if (team.getProductId() != null) {
            product = productMapper.selectOne(baseProductQuery(tenantId).eq("id", team.getProductId()));
        }
        SalesProductDescriptionEntity description = null;
        if (descriptionMapper != null && team.getProductId() != null) {
            description = descriptionMapper.selectOne(baseDescriptionQuery(tenantId).eq("product_id", team.getProductId()));
        }
        List<SalesProductItineraryDayEntity> itineraryDays = loadProductItineraryDays(tenantId, team.getProductId());
        List<SalesTeamPriceResponse> prices = loadPricesForTeams(tenantId, List.of(team))
                .getOrDefault(team.getId(), List.of());
        SalesTeamOperationResponse detail = SalesTeamOperationResponse.from(
                team,
                product,
                description,
                prices,
                itineraryDays,
                List.of(),
                null,
                null
        );
        return new SalesBookingTeamDraftResponse(
                detail.team(),
                detail.product(),
                detail.content(),
                detail.routeSummary(),
                detail.itineraryDays(),
                detail.prices()
        );
    }

    /**
     * 团队管理页直接创建散拼、整团或散团。
     *
     * <p>老系统这三个入口复用“产品团队”基础信息页面。新系统保持统一团队主表，同时创建一个
     * 最小销售产品快照作为团队名称、天数、所在地和后续行程/产品说明页的承载对象。</p>
     *
     * @param request 直接创建团队请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     * @return 新增团队信息
     */
    @Transactional
    public SalesTeamResponse directCreate(SalesTeamDirectCreateRequest request, Long tenantId, String operator) {
        SalesTeamType teamType = SalesTeamType.fromValue(request.teamType());
        if (teamType == SalesTeamType.SINGLE) {
            throw new BizException("单项业务请使用单项创建入口");
        }
        String teamNo = nextTeamNo(teamType, tenantId, request.departureDate(), new HashSet<>());
        SalesProductEntity product = buildDirectProductSnapshot(request, tenantId, operator, teamNo);
        productMapper.insert(product);
        saveDirectProductDescription(product.getId(), request, tenantId, operator);
        saveDirectProductItineraryDays(product.getId(), request, tenantId, operator);

        SalesTeamEntity team = buildDirectTeam(product, request, teamType, tenantId, operator, teamNo);
        teamMapper.insert(team);

        insertStatusLog(tenantId, team.getId(), null, team.getStatus(), SalesTeamStatusAction.CREATE, operator, "团队管理直接新增");
        insertNoLog(tenantId, product.getId(), request.departureDate(), teamNo, suffixOf(teamNo), operator);
        refreshTeamListSummary(team.getId(), tenantId);
        return SalesTeamResponse.fromEntity(team, List.of());
    }

    /**
     * 查询团队直接编辑页详情。
     *
     * <p>该详情对应老系统团队操作页“修改团队”跳回的 LineAdd.aspx 页面。返回团队主表字段、
     * 产品快照基础信息、产品说明、每日行程和路书点位，供前端复用直接建团页面回显。</p>
     *
     * @param teamId 团队 ID
     * @param tenantId 当前租户 ID
     * @return 团队编辑页详情
     */
    public SalesTeamDirectEditResponse directEditDetail(Long teamId, Long tenantId) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        SalesProductEntity product = requireDirectProductSnapshot(team.getProductId(), tenantId);
        SalesProductDescriptionEntity description = descriptionMapper == null
                ? null
                : descriptionMapper.selectOne(baseDescriptionQuery(tenantId).eq("product_id", product.getId()));
        List<SalesProductItineraryDayResponse> itineraryDays = loadDirectEditItineraryDays(tenantId, product.getId());
        return new SalesTeamDirectEditResponse(
                team.getId(),
                product.getId(),
                team.getTeamNo(),
                team.getTeamType(),
                displayTeamName(team, product),
                firstText(team.getBusinessType(), product.getBusinessType()),
                product.getDomesticInternational(),
                product.getProvince(),
                product.getCity(),
                product.getDistrict(),
                team.getDepartureDate(),
                product.getTripType(),
                product.getReceptionStandard(),
                product.getProductTheme(),
                product.getTravelDays(),
                team.getCloseDaysBefore(),
                team.getSingleRoomDifference(),
                team.getTotalSeats(),
                itineraryDays,
                description == null ? null : description.getBookingNotice(),
                description == null ? null : description.getProductDescription(),
                description == null ? null : description.getFeeIncluded(),
                description == null ? null : description.getFeeExcluded(),
                description == null ? null : description.getChildPolicy(),
                description == null ? null : description.getShoppingArrangement(),
                description == null ? null : description.getOptionalItems(),
                description == null ? null : description.getGiftItems(),
                description == null ? null : description.getAttentionItems(),
                description == null ? null : description.getWarmReminder(),
                team.getRemark()
        );
    }

    /**
     * 保存团队直接编辑页。
     *
     * <p>编辑团队不重新生成团号、不新建产品快照，也不覆盖团队安排成本明细；只更新团队主表、
     * 团队专属产品快照基础信息、产品说明、每日行程和路书点位。</p>
     *
     * @param teamId 团队 ID
     * @param request 编辑页提交内容
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     * @return 修改后的团队信息
     */
    @Transactional
    public SalesTeamResponse directUpdate(
            Long teamId,
            SalesTeamDirectCreateRequest request,
            Long tenantId,
            String operator
    ) {
        SalesTeamEntity current = requireTeam(teamId, tenantId);
        SalesTeamType teamType = SalesTeamType.fromValue(request.teamType());
        if (teamType == SalesTeamType.SINGLE) {
            throw new BizException("单项业务请使用单项创建入口");
        }
        SalesProductEntity product = requireDirectProductSnapshot(current.getProductId(), tenantId);
        Integer usedSeats = number(current.getUsedSeats());
        Integer totalSeats = number(request.totalSeats());
        if (totalSeats < usedSeats) {
            throw new BizException("预控人数不能小于已收客人数");
        }

        SalesProductEntity productUpdate = new SalesProductEntity();
        applyDirectProductFields(productUpdate, request, current.getTeamNo());
        int productUpdated = productMapper.update(productUpdate, baseProductUpdate(tenantId).eq("id", product.getId()));
        if (productUpdated == 0) {
            throw new BizException("产品快照不存在或已删除");
        }

        SalesTeamEntity teamUpdate = new SalesTeamEntity();
        teamUpdate.setTeamType(teamType.getValue());
        teamUpdate.setTeamName(clean(request.teamName()));
        teamUpdate.setBusinessType(clean(request.businessType()));
        teamUpdate.setDepartureDate(request.departureDate());
        teamUpdate.setTotalSeats(totalSeats);
        teamUpdate.setRemainingSeats(totalSeats - usedSeats);
        teamUpdate.setSingleRoomDifference(money(request.singleRoomDifference()));
        teamUpdate.setCloseDaysBefore(number(request.closeDaysBefore()));
        teamUpdate.setRemark(clean(request.remark()));
        int teamUpdated = teamMapper.update(teamUpdate, baseTeamUpdate(tenantId).eq("id", teamId));
        if (teamUpdated == 0) {
            throw new BizException("团队不存在或已删除");
        }

        softDeleteDirectProductContent(product.getId(), tenantId, operator);
        saveDirectProductDescription(product.getId(), request, tenantId, operator);
        saveDirectProductItineraryDays(product.getId(), request, tenantId, operator);
        SalesTeamEntity latest = requireTeam(teamId, tenantId);
        refreshTeamListSummary(teamId, tenantId);
        return SalesTeamResponse.fromEntity(latest, loadPricesForTeams(tenantId, List.of(latest)).getOrDefault(teamId, List.of()));
    }

    /**
     * 按日期范围批量生成团期。
     *
     * <p>命中星期条件的每一天都会创建一条散拼团队。创建时从产品带入截止收客天数、单房差和预控人数，
     * 并同步创建默认客户类型价格行。</p>
     *
     * @param productId 产品 ID
     * @param request 批量生成请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     * @return 新生成的团队列表
     */
    @Transactional
    public List<SalesTeamResponse> batchCreate(
            Long productId,
            SalesTeamBatchCreateRequest request,
            Long tenantId,
            String operator
    ) {
        SalesProductEntity product = requireActiveProduct(productId, tenantId);
        List<LocalDate> dates = collectDates(request);
        if (dates.isEmpty()) {
            throw new BizException("没有符合条件的发团日期");
        }
        List<SalesTeamResponse> created = new ArrayList<>();
        Set<String> generatedInThisBatch = new HashSet<>();
        List<SalesProductArrangementItemEntity> productArrangements = loadProductArrangementTemplates(productId, tenantId);
        Map<Long, List<SalesProductArrangementPriceLineEntity>> productPriceLines =
                loadProductArrangementPriceLines(tenantId, productArrangements);
        for (LocalDate departureDate : dates) {
            String teamNo = nextTeamNo(SalesTeamType.SANPIN, tenantId, departureDate, generatedInThisBatch);
            SalesTeamEntity team = buildTeam(product, request, tenantId, operator, departureDate, teamNo);
            teamMapper.insert(team);
            SalesTeamPriceEntity price = buildPrice(team, request, tenantId, operator);
            priceMapper.insert(price);
            insertStatusLog(tenantId, team.getId(), null, team.getStatus(), SalesTeamStatusAction.CREATE, operator, "批量生成团期");
            insertNoLog(tenantId, productId, departureDate, teamNo, suffixOf(teamNo), operator);
            copyProductArrangementsToTeam(team, productArrangements, productPriceLines, tenantId, operator);
            refreshTeamListSummary(team.getId(), tenantId);
            created.add(SalesTeamResponse.fromEntity(team, List.of(SalesTeamPriceResponse.fromEntity(price))));
        }
        return created;
    }

    /**
     * 保存团队主信息。
     *
     * <p>修改总位数时保留已占用位数，并重算余位；总位数不能小于已占用位数。</p>
     */
    @Transactional
    public SalesTeamResponse saveTeam(Long teamId, SalesTeamSaveRequest request, Long tenantId, String operator) {
        SalesTeamEntity current = requireTeam(teamId, tenantId);
        SalesTeamEntity update = new SalesTeamEntity();
        if (request.departureDate() != null) {
            update.setDepartureDate(request.departureDate());
        }
        if (StringUtils.hasText(request.teamType())) {
            update.setTeamType(SalesTeamType.fromValueOrDefault(request.teamType()).getValue());
        }
        Integer usedSeats = number(current.getUsedSeats());
        Integer totalSeats = request.totalSeats() == null ? current.getTotalSeats() : request.totalSeats();
        if (totalSeats != null && totalSeats < usedSeats) {
            throw new BizException("总位数不能小于已占用位数");
        }
        if (request.totalSeats() != null) {
            update.setTotalSeats(totalSeats);
            update.setRemainingSeats(totalSeats - usedSeats);
        }
        if (request.singleRoomDifference() != null) {
            update.setSingleRoomDifference(money(request.singleRoomDifference()));
        }
        UpdateWrapper<SalesTeamEntity> wrapper = baseTeamUpdate(tenantId).eq("id", teamId);
        applyNullableTeamProfileFields(wrapper, request);
        int updated = teamMapper.update(update, wrapper);
        if (updated == 0) {
            throw new BizException("团队不存在或已删除");
        }
        SalesTeamEntity latest = requireTeam(teamId, tenantId);
        refreshTeamListSummary(teamId, tenantId);
        return SalesTeamResponse.fromEntity(latest, loadPricesForTeams(tenantId, List.of(latest)).getOrDefault(teamId, List.of()));
    }

    /**
     * 保存团队客户类型价格。
     *
     * <p>同一团队同一客户类型重复保存时更新原价格行；新客户类型则新增价格行。</p>
     */
    @Transactional
    public SalesTeamPriceResponse savePrice(
            Long teamId,
            SalesTeamPriceSaveRequest request,
            Long tenantId,
            String operator
    ) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        SalesTeamPriceEntity current = findPrice(teamId, request.customerCategoryId(), request.customerCategoryName(), tenantId);
        if (current == null) {
            SalesTeamPriceEntity entity = new SalesTeamPriceEntity();
            applyPriceFields(entity, request);
            entity.setTenantId(tenantId);
            entity.setTeamId(team.getId());
            entity.setProductId(team.getProductId());
            entity.setStatus(SalesTeamPriceStatus.ACTIVE.getValue());
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            priceMapper.insert(entity);
            return SalesTeamPriceResponse.fromEntity(entity);
        }
        SalesTeamPriceEntity update = new SalesTeamPriceEntity();
        applyPriceFields(update, request);
        int updated = priceMapper.update(update, basePriceUpdate(tenantId).eq("id", current.getId()));
        if (updated == 0) {
            throw new BizException("团队价格不存在或已删除");
        }
        SalesTeamPriceEntity latest = priceMapper.selectOne(basePriceQuery(tenantId).eq("id", current.getId()));
        return SalesTeamPriceResponse.fromEntity(latest == null ? current : latest);
    }

    /**
     * 批量编辑团期和客户类型价格。
     *
     * <p>该方法复刻旧系统“添加/修改团期信息”弹窗规则：先选择团队，再选择客户类型。
     * 选中客户类型时只处理这些客户类型价格；未选客户类型时处理团队下已有全部价格，
     * 若团队没有价格且本次是修改价格，则补一条默认价格。</p>
     */
    @Transactional
    public void batchEdit(SalesTeamBatchEditRequest request, Long tenantId, String operator) {
        if (CollectionUtils.isEmpty(request.teamIds())) {
            throw new BizException("请选择团队");
        }
        boolean updateTeam = Boolean.TRUE.equals(request.updateTotalSeats())
                || Boolean.TRUE.equals(request.updateSingleRoomDifference());
        boolean updatePrice = hasPricePayload(request);
        boolean deletePrice = Boolean.TRUE.equals(request.deletePrice());
        if (!updateTeam && !updatePrice && !deletePrice) {
            throw new BizException("请选择需要修改的内容");
        }
        for (Long teamId : request.teamIds()) {
            SalesTeamEntity team = requireTeam(teamId, tenantId);
            if (updateTeam) {
                batchUpdateTeam(team, request, tenantId);
                refreshTeamListSummary(teamId, tenantId);
            }
            if (deletePrice) {
                deletePricesByBatchRule(team, request, tenantId, operator);
            } else if (updatePrice) {
                savePricesByBatchRule(team, request, tenantId, operator);
            }
        }
    }

    /**
     * 删除团队价格行。
     *
     * <p>删除价格只软删除当前价格行，不影响团队主记录。</p>
     */
    @Transactional
    public void deletePrice(Long priceId, Long tenantId, String operator) {
        SalesTeamPriceEntity update = new SalesTeamPriceEntity();
        update.setIsDeleted(true);
        update.setDeletedAt(OffsetDateTime.now());
        update.setDeletedBy(operator);
        int updated = priceMapper.update(update, basePriceUpdate(tenantId).eq("id", priceId));
        if (updated == 0) {
            throw new BizException("团队价格不存在或已删除");
        }
    }

    /**
     * 批量变更团队状态。
     *
     * <p>状态动作按老系统按钮语义限制：正常可停收或取消，停收可启用或取消，取消可恢复或删除。</p>
     */
    @Transactional
    public void changeStatus(List<Long> teamIds, String action, Long tenantId, String operator, String remark) {
        if (CollectionUtils.isEmpty(teamIds)) {
            throw new BizException("请选择团队");
        }
        for (Long teamId : teamIds) {
            switch (action) {
                case "stop" -> updateStatus(teamId, tenantId, operator, remark, SalesTeamStatus.NORMAL, SalesTeamStatus.STOPPED, SalesTeamStatusAction.STOP);
                case "start" -> updateStatus(teamId, tenantId, operator, remark, SalesTeamStatus.STOPPED, SalesTeamStatus.NORMAL, SalesTeamStatusAction.START);
                case "cancel" -> cancelTeam(teamId, tenantId, operator, remark);
                case "recover" -> updateStatus(teamId, tenantId, operator, remark, SalesTeamStatus.CANCELLED, SalesTeamStatus.NORMAL, SalesTeamStatusAction.RECOVER);
                case "delete" -> deleteTeam(teamId, tenantId, operator);
                default -> throw new BizException("状态动作不合法");
            }
        }
    }

    /**
     * 删除团队。
     *
     * <p>只有取消状态的团队可以软删除。该规则防止用户直接删除仍在收客或停收中的团队。</p>
     */
    @Transactional
    public void deleteTeam(Long teamId, Long tenantId, String operator) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        if (!SalesTeamStatus.CANCELLED.getValue().equals(team.getStatus())) {
            throw new BizException("只有取消状态的团队可以删除");
        }
        OffsetDateTime now = OffsetDateTime.now();
        SalesTeamEntity teamUpdate = new SalesTeamEntity();
        teamUpdate.setIsDeleted(true);
        teamUpdate.setDeletedAt(now);
        teamUpdate.setDeletedBy(operator);
        teamMapper.update(teamUpdate, baseTeamUpdate(tenantId).eq("id", teamId));

        SalesTeamPriceEntity priceUpdate = new SalesTeamPriceEntity();
        priceUpdate.setIsDeleted(true);
        priceUpdate.setDeletedAt(now);
        priceUpdate.setDeletedBy(operator);
        priceMapper.update(priceUpdate, basePriceUpdate(tenantId).eq("team_id", teamId));

        insertStatusLog(tenantId, teamId, team.getStatus(), team.getStatus(), SalesTeamStatusAction.DELETE, operator, "删除取消状态团队");
        refreshTeamListSummary(teamId, tenantId);
    }

    private QueryWrapper<SalesTeamEntity> baseTeamQuery(Long tenantId) {
        return new QueryWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesTeamEntity> baseTeamUpdate(Long tenantId) {
        return new UpdateWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    /**
     * 显式写入团队属性字段，允许页面提交空字符串清空团队快照。
     *
     * <p>实体更新会忽略 null；这里用 wrapper.set 保证“用户清空”和“字段未参与本次保存”
     * 可以区分。前端团期行内保存只传总位/房差时不会清空这些属性。</p>
     */
    private void applyNullableTeamProfileFields(UpdateWrapper<SalesTeamEntity> wrapper, SalesTeamSaveRequest request) {
        if (request.businessType() != null) {
            wrapper.set("business_type", clean(request.businessType()));
        }
        if (request.departmentId() != null || request.departmentName() != null) {
            wrapper.set("department_id", request.departmentId());
            wrapper.set("department_name", clean(request.departmentName()));
        }
        if (request.operatorEmployeeId() != null || request.operatorEmployeeName() != null) {
            wrapper.set("operator_employee_id", request.operatorEmployeeId());
            wrapper.set("operator_employee_name", clean(request.operatorEmployeeName()));
        }
        if (request.escortEmployeeId() != null || request.escortEmployeeName() != null) {
            wrapper.set("escort_employee_id", request.escortEmployeeId());
            wrapper.set("escort_employee_name", clean(request.escortEmployeeName()));
        }
        if (request.remark() != null) {
            wrapper.set("remark", clean(request.remark()));
        }
        if (request.perCapitaPitAmount() != null) {
            wrapper.set("per_capita_pit_amount", money(request.perCapitaPitAmount()));
        }
        if (request.optionalMarkupRate() != null) {
            wrapper.set("optional_markup_rate", money(request.optionalMarkupRate()));
        }
        if (request.perCapitaShoppingAmount() != null) {
            wrapper.set("per_capita_shopping_amount", money(request.perCapitaShoppingAmount()));
        }
    }

    private QueryWrapper<SalesTeamPriceEntity> basePriceQuery(Long tenantId) {
        return new QueryWrapper<SalesTeamPriceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesProductEntity> baseProductQuery(Long tenantId) {
        return new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    /** 批量读取产品团队安排模板，供从产品排期生成团队时复制为团队安排参考。 */
    private List<SalesProductArrangementItemEntity> loadProductArrangementTemplates(Long productId, Long tenantId) {
        if (productArrangementMapper == null) {
            return List.of();
        }
        List<SalesProductArrangementItemEntity> items = productArrangementMapper.selectList(
                new QueryWrapper<SalesProductArrangementItemEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("product_id", productId)
                        .eq("is_deleted", false)
                        .orderByAsc("arrangement_type")
                        .orderByAsc("id")
        );
        return items == null ? List.of() : items;
    }

    /** 批量读取产品安排价格明细，按安排项 ID 分组，避免生成多个团期时形成 N+1 查询。 */
    private Map<Long, List<SalesProductArrangementPriceLineEntity>> loadProductArrangementPriceLines(
            Long tenantId,
            List<SalesProductArrangementItemEntity> arrangements
    ) {
        if (productArrangementPriceLineMapper == null || CollectionUtils.isEmpty(arrangements)) {
            return Map.of();
        }
        List<Long> arrangementIds = arrangements.stream()
                .map(SalesProductArrangementItemEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (arrangementIds.isEmpty()) {
            return Map.of();
        }
        List<SalesProductArrangementPriceLineEntity> lines = productArrangementPriceLineMapper.selectList(
                new QueryWrapper<SalesProductArrangementPriceLineEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("arrangement_item_id", arrangementIds)
                        .orderByAsc("arrangement_item_id")
                        .orderByAsc("sort_order")
                        .orderByAsc("id")
        );
        if (CollectionUtils.isEmpty(lines)) {
            return Map.of();
        }
        return lines.stream().collect(Collectors.groupingBy(
                SalesProductArrangementPriceLineEntity::getArrangementItemId,
                Collectors.toList()
        ));
    }

    /**
     * 将产品团队安排模板复制为正式团队安排参考。
     *
     * <p>产品阶段没有正式订单和游客人数，复制时只带资源、供应商、项目和参考单价。
     * 数量、人数、成本、现结和挂账都置零，后续由计调在团队安排页确认具体执行数据后再进入成本和审核链路。</p>
     */
    private void copyProductArrangementsToTeam(
            SalesTeamEntity team,
            List<SalesProductArrangementItemEntity> productArrangements,
            Map<Long, List<SalesProductArrangementPriceLineEntity>> productPriceLines,
            Long tenantId,
            String operator
    ) {
        if (teamArrangementMapper == null || teamArrangementPriceLineMapper == null
                || teamArrangementAllocationMapper == null || CollectionUtils.isEmpty(productArrangements)) {
            return;
        }
        for (SalesProductArrangementItemEntity template : productArrangements) {
            DispatchTeamArrangementEntity arrangement = buildTeamArrangementSnapshot(team, template, tenantId, operator);
            teamArrangementMapper.insert(arrangement);
            List<SalesProductArrangementPriceLineEntity> lines =
                    productPriceLines.getOrDefault(template.getId(), List.of());
            if (CollectionUtils.isEmpty(lines)) {
                insertDefaultTeamArrangementPriceLine(arrangement, template, tenantId, operator);
            } else {
                for (SalesProductArrangementPriceLineEntity line : lines) {
                    insertTeamArrangementPriceLine(arrangement, line, tenantId, operator);
                }
            }
            insertTeamPublicAllocation(arrangement, tenantId, operator);
        }
    }

    /** 构建正式团队安排参考主记录，避免产品模板人数提前进入正式团队成本。 */
    private DispatchTeamArrangementEntity buildTeamArrangementSnapshot(
            SalesTeamEntity team,
            SalesProductArrangementItemEntity template,
            Long tenantId,
            String operator
    ) {
        DispatchTeamArrangementEntity entity = new DispatchTeamArrangementEntity();
        entity.setTenantId(tenantId);
        entity.setTeamId(team.getId());
        entity.setTeamNo(team.getTeamNo());
        entity.setTeamType(team.getTeamType());
        entity.setBusinessType(team.getBusinessType());
        entity.setDepartmentId(team.getDepartmentId());
        entity.setDepartmentName(team.getDepartmentName());
        entity.setOperatorEmployeeId(team.getOperatorEmployeeId());
        entity.setOperatorEmployeeName(team.getOperatorEmployeeName());
        entity.setArrangementType(clean(template.getArrangementType()));
        entity.setItemName(firstText(template.getItemName(), template.getResourceName(), template.getSupplierName(), "团队安排"));
        entity.setArrangementContent(clean(template.getArrangementContent()));
        entity.setAllocationMode("group_order_average");
        entity.setScheduleStartDay(clean(template.getScheduleStartDay()));
        entity.setScheduleEndDay(clean(template.getScheduleEndDay()));
        entity.setBusinessDate(team.getDepartureDate());
        entity.setDeparturePlace(clean(template.getDeparturePlace()));
        entity.setArrivalPlace(clean(template.getArrivalPlace()));
        entity.setDaysCount(number(template.getDaysCount()));
        entity.setResourceName(clean(template.getResourceName()));
        entity.setSupplierId(template.getSupplierId());
        entity.setSupplierName(clean(template.getSupplierName()));
        entity.setTrafficType(clean(template.getTrafficType()));
        entity.setVehicleType(clean(template.getVehicleType()));
        entity.setDriverName(clean(template.getDriverName()));
        entity.setVehiclePlate(clean(template.getVehiclePlate()));
        entity.setResponsibleEmployeeId(template.getResponsibleEmployeeId());
        entity.setResponsibleEmployeeName(clean(template.getResponsibleEmployeeName()));
        entity.setSettlementType(DispatchArrangementSettlementType.fromValueOrDefault(template.getSettlementType()).getValue());
        entity.setMealType(clean(template.getMealType()));
        entity.setFundIncluded(clean(template.getFundIncluded()));
        entity.setConfirmed(false);
        entity.setConfirmationNo(clean(template.getConfirmationNo()));
        entity.setGuideId(template.getGuideId());
        entity.setGuideName(clean(template.getGuideName()));
        entity.setTotalAmount(zeroMoney());
        entity.setCashAmount(zeroMoney());
        entity.setCreditAmount(zeroMoney());
        entity.setPrepaidAmount(zeroMoney());
        entity.setSaleAmount(zeroMoney());
        entity.setCostAmount(zeroMoney());
        entity.setGuideCommissionAmount(zeroMoney());
        entity.setCompanyRebateAmount(zeroMoney());
        entity.setHeadFeeAmount(zeroMoney());
        entity.setConsumptionAmount(zeroMoney());
        entity.setPeopleCount(zeroMoney());
        entity.setNoGuideReport(Boolean.FALSE);
        entity.setGuideInvolved(Boolean.TRUE);
        entity.setCostStage("arrangement");
        entity.setGuideReportStatus("pending");
        entity.setOperatorAuditStatus("pending");
        entity.setFinanceAuditStatus("pending");
        entity.setStatus("active");
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        return entity;
    }

    /** 复制产品安排价格明细到正式团队安排价格明细。 */
    private void insertTeamArrangementPriceLine(
            DispatchTeamArrangementEntity arrangement,
            SalesProductArrangementPriceLineEntity line,
            Long tenantId,
            String operator
    ) {
        DispatchTeamArrangementPriceLineEntity entity = new DispatchTeamArrangementPriceLineEntity();
        entity.setTenantId(tenantId);
        entity.setArrangementId(arrangement.getId());
        entity.setTeamId(arrangement.getTeamId());
        entity.setProjectId(line.getProjectId());
        entity.setProjectName(firstText(line.getProjectName(), arrangement.getItemName()));
        entity.setUnitPrice(referenceUnitPrice(line));
        entity.setQuantity(zeroMoney());
        entity.setAmount(zeroMoney());
        entity.setSalePrice(money(line.getSalePrice()));
        entity.setCostPrice(money(line.getCostPrice()));
        entity.setCashAmount(zeroMoney());
        entity.setCreditAmount(zeroMoney());
        entity.setGuideCommissionAmount(zeroMoney());
        entity.setGuideCommissionRate(money(line.getGuideCommissionRate()));
        entity.setCompanyRebateAmount(zeroMoney());
        entity.setCompanyRebateRate(money(line.getCompanyRebateRate()));
        entity.setHeadFeeAmount(zeroMoney());
        entity.setConsumptionAmount(zeroMoney());
        entity.setSortOrder(line.getSortOrder() == null ? 1 : line.getSortOrder());
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        teamArrangementPriceLineMapper.insert(entity);
    }

    /** 产品模板没有价格明细时生成一条默认参考明细，计调后续人工填写真实数量和金额。 */
    private void insertDefaultTeamArrangementPriceLine(
            DispatchTeamArrangementEntity arrangement,
            SalesProductArrangementItemEntity template,
            Long tenantId,
            String operator
    ) {
        DispatchTeamArrangementPriceLineEntity entity = new DispatchTeamArrangementPriceLineEntity();
        entity.setTenantId(tenantId);
        entity.setArrangementId(arrangement.getId());
        entity.setTeamId(arrangement.getTeamId());
        entity.setProjectName(arrangement.getItemName());
        entity.setUnitPrice(referenceUnitPrice(template));
        entity.setQuantity(zeroMoney());
        entity.setAmount(zeroMoney());
        entity.setCashAmount(zeroMoney());
        entity.setCreditAmount(zeroMoney());
        entity.setSortOrder(1);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        teamArrangementPriceLineMapper.insert(entity);
    }

    /** 产品排期创建的初始安排默认是团队公共成本，不关联正式订单。 */
    private void insertTeamPublicAllocation(
            DispatchTeamArrangementEntity arrangement,
            Long tenantId,
            String operator
    ) {
        DispatchTeamArrangementOrderAllocationEntity allocation = new DispatchTeamArrangementOrderAllocationEntity();
        allocation.setTenantId(tenantId);
        allocation.setArrangementId(arrangement.getId());
        allocation.setTeamId(arrangement.getTeamId());
        allocation.setTeamNo(arrangement.getTeamNo());
        allocation.setAllocationScope("team");
        allocation.setGuestCount(0);
        allocation.setAllocationMode("group_order_average");
        allocation.setOriginalAmount(arrangement.getTotalAmount());
        allocation.setAllocationAmount(arrangement.getTotalAmount());
        allocation.setSortOrder(1);
        allocation.setCreatedBy(operator);
        allocation.setIsDeleted(false);
        teamArrangementAllocationMapper.insert(allocation);
    }

    private BigDecimal referenceUnitPrice(SalesProductArrangementItemEntity template) {
        BigDecimal unitPrice = money(template.getUnitPrice());
        if (unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            return unitPrice;
        }
        return divideAmountByQuantity(template.getTotalAmount(), template.getQuantity());
    }

    private BigDecimal referenceUnitPrice(SalesProductArrangementPriceLineEntity line) {
        BigDecimal unitPrice = money(line.getUnitPrice());
        if (unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            return unitPrice;
        }
        return divideAmountByQuantity(line.getAmount(), line.getQuantity());
    }

    private BigDecimal divideAmountByQuantity(BigDecimal amount, BigDecimal quantity) {
        BigDecimal count = money(quantity);
        if (count.compareTo(BigDecimal.ZERO) <= 0) {
            return zeroMoney();
        }
        return money(amount).divide(count, 2, RoundingMode.HALF_UP);
    }

    private UpdateWrapper<SalesProductEntity> baseProductUpdate(Long tenantId) {
        return new UpdateWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesProductDescriptionEntity> baseDescriptionQuery(Long tenantId) {
        return new QueryWrapper<SalesProductDescriptionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesProductItineraryDayEntity> baseItineraryDayQuery(Long tenantId) {
        return new QueryWrapper<SalesProductItineraryDayEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesProductRoadbookPointEntity> baseRoadbookPointQuery(Long tenantId) {
        return new QueryWrapper<SalesProductRoadbookPointEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private List<SalesProductItineraryDayEntity> loadProductItineraryDays(Long tenantId, Long productId) {
        if (itineraryDayMapper == null || productId == null) {
            return List.of();
        }
        return itineraryDayMapper.selectList(baseItineraryDayQuery(tenantId)
                .eq("product_id", productId)
                .orderByAsc("day_no"));
    }

    private List<SalesProductItineraryDayResponse> loadDirectEditItineraryDays(Long tenantId, Long productId) {
        if (itineraryDayMapper == null || productId == null) {
            return List.of();
        }
        Map<Integer, List<SalesProductRoadbookPointResponse>> roadbookByDay = loadDirectEditRoadbookPoints(tenantId, productId);
        return itineraryDayMapper.selectList(baseItineraryDayQuery(tenantId)
                        .eq("product_id", productId)
                        .orderByAsc("day_no")
                        .orderByAsc("id"))
                .stream()
                .map(item -> SalesProductItineraryDayResponse.fromEntity(
                        item,
                        roadbookByDay.getOrDefault(item.getDayNo(), List.of())
                ))
                .toList();
    }

    private Map<Integer, List<SalesProductRoadbookPointResponse>> loadDirectEditRoadbookPoints(Long tenantId, Long productId) {
        if (roadbookPointMapper == null || productId == null) {
            return Map.of();
        }
        return roadbookPointMapper.selectList(baseRoadbookPointQuery(tenantId)
                        .eq("product_id", productId)
                        .orderByAsc("day_no")
                        .orderByAsc("point_order")
                        .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(
                        SalesProductRoadbookPointEntity::getDayNo,
                        Collectors.mapping(SalesProductRoadbookPointResponse::fromEntity, Collectors.toList())
                ));
    }

    private void applyGlobalStatusFilter(QueryWrapper<SalesTeamEntity> wrapper, String teamStatus) {
        if (!StringUtils.hasText(teamStatus)) {
            return;
        }
        String status = clean(teamStatus);
        LocalDate today = LocalDate.now();
        switch (status) {
            case "not_departed" -> wrapper.ge("departure_date", today);
            case "departed" -> wrapper.lt("departure_date", today);
            case "normal", "stopped", "cancelled" -> wrapper.eq("status", status);
            default -> {
                // 未识别的页面选项不拼接条件，避免因为前端临时枚举导致团队列表为空。
            }
        }
    }

    private void applyGlobalProductFilters(
            QueryWrapper<SalesTeamEntity> wrapper,
            Long tenantId,
            String keyword,
            String departurePlace,
            String businessType,
            Integer travelDays
    ) {
        boolean hasProductFilter = StringUtils.hasText(keyword)
                || StringUtils.hasText(departurePlace)
                || StringUtils.hasText(businessType)
                || travelDays != null;
        if (!hasProductFilter) {
            return;
        }
        QueryWrapper<SalesProductEntity> productWrapper = baseProductQuery(tenantId)
                .select("id")
                .eq(StringUtils.hasText(businessType), "business_type", clean(businessType))
                .eq(travelDays != null, "travel_days", travelDays);
        if (StringUtils.hasText(keyword)) {
            String value = clean(keyword);
            productWrapper.and(nested -> nested
                    .like("product_name", value)
                    .or()
                    .like("remark", value));
        }
        if (StringUtils.hasText(departurePlace)) {
            String value = clean(departurePlace);
            productWrapper.and(nested -> nested
                    .like("province", value)
                    .or()
                    .like("city", value)
                    .or()
                    .like("district", value));
        }
        List<Long> productIds = productMapper.selectList(productWrapper)
                .stream()
                .map(SalesProductEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (StringUtils.hasText(keyword)) {
            String value = clean(keyword);
            wrapper.and(nested -> {
                nested.like("team_no", value)
                        .or()
                        .like("remark", value);
                if (!productIds.isEmpty()) {
                    nested.or().in("product_id", productIds);
                }
            });
        } else if (productIds.isEmpty()) {
            wrapper.apply("1 = 0");
        } else {
            wrapper.in("product_id", productIds);
        }
    }

    private void applyGlobalCustomerFilter(QueryWrapper<SalesTeamEntity> wrapper, Long tenantId, String customerKeyword) {
        if (!StringUtils.hasText(customerKeyword)) {
            return;
        }
        String value = clean(customerKeyword).replace("'", "''");
        wrapper.exists("""
                SELECT 1
                FROM sales_orders so
                WHERE so.tenant_id = {0}
                  AND so.team_id = sales_teams.id
                  AND so.is_deleted = false
                  AND so.status <> 'cancelled'
                  AND COALESCE(so.order_role, 'normal') IN ('normal', 'merge_child', 'merge_source')
                  AND (so.customer_name ILIKE {1} OR so.salesperson_employee_name ILIKE {1})
                """, tenantId, "%" + value + "%");
    }

    private void applyGlobalGuideFilter(QueryWrapper<SalesTeamEntity> wrapper, Long tenantId, String guideKeyword) {
        if (!StringUtils.hasText(guideKeyword)) {
            return;
        }
        String value = clean(guideKeyword).replace("'", "''");
        wrapper.exists("""
                SELECT 1
                FROM dispatch_team_guides dtg
                WHERE dtg.tenant_id = {0}
                  AND dtg.team_id = sales_teams.id
                  AND dtg.is_deleted = false
                  AND dtg.status = 'active'
                  AND (dtg.guide_name ILIKE {1} OR dtg.guide_mobile ILIKE {1})
                """, tenantId, "%" + value + "%");
    }

    private void applyGlobalOrderStatusFilter(QueryWrapper<SalesTeamEntity> wrapper, Long tenantId, String orderStatus) {
        if (!StringUtils.hasText(orderStatus)) {
            return;
        }
        String status = clean(orderStatus);
        if ("none".equals(status)) {
            wrapper.notExists("""
                    SELECT 1
                    FROM sales_orders so
                    WHERE so.tenant_id = {0}
                      AND so.team_id = sales_teams.id
                      AND so.is_deleted = false
                      AND COALESCE(so.order_role, 'normal') IN ('normal', 'merge_child', 'merge_source')
                    """, tenantId);
            return;
        }
        wrapper.exists("""
                SELECT 1
                FROM sales_orders so
                WHERE so.tenant_id = {0}
                  AND so.team_id = sales_teams.id
                  AND so.is_deleted = false
                  AND COALESCE(so.order_role, 'normal') IN ('normal', 'merge_child', 'merge_source')
                  AND so.status = {1}
                """, tenantId, status);
    }

    private void applyGlobalAddDateFilter(QueryWrapper<SalesTeamEntity> wrapper, LocalDate addDate) {
        if (addDate == null) {
            return;
        }
        wrapper.ge("created_at", addDate.atStartOfDay())
                .lt("created_at", addDate.plusDays(1).atStartOfDay());
    }

    private UpdateWrapper<SalesTeamPriceEntity> basePriceUpdate(Long tenantId) {
        return new UpdateWrapper<SalesTeamPriceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private SalesProductEntity requireActiveProduct(Long productId, Long tenantId) {
        SalesProductEntity product = productMapper.selectOne(new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", productId));
        if (product == null) {
            throw new BizException("产品不存在或已删除");
        }
        if (!SalesProductStatus.ACTIVE.getValue().equals(product.getStatus())) {
            throw new BizException("停用产品不能生成团期");
        }
        return product;
    }

    private SalesProductEntity requireDirectProductSnapshot(Long productId, Long tenantId) {
        if (productId == null) {
            throw new BizException("团队产品快照不存在");
        }
        SalesProductEntity product = productMapper.selectOne(baseProductQuery(tenantId).eq("id", productId));
        if (product == null) {
            throw new BizException("产品快照不存在或已删除");
        }
        return product;
    }

    private SalesTeamEntity requireTeam(Long teamId, Long tenantId) {
        SalesTeamEntity team = teamMapper.selectOne(baseTeamQuery(tenantId).eq("id", teamId));
        if (team == null) {
            throw new BizException("团队不存在或已删除");
        }
        return team;
    }

    private List<LocalDate> collectDates(SalesTeamBatchCreateRequest request) {
        if (request.dates() != null && !request.dates().isEmpty()) {
            return request.dates().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BizException("结束日期不能早于开始日期");
        }
        Set<Integer> weekdays = new HashSet<>(Objects.requireNonNullElse(request.weekdays(), List.of()));
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = request.startDate();
        while (!current.isAfter(request.endDate())) {
            int weekday = current.getDayOfWeek().getValue();
            if (weekdays.isEmpty() || weekdays.contains(weekday)) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }
        return dates;
    }

    private SalesTeamEntity buildTeam(
            SalesProductEntity product,
            SalesTeamBatchCreateRequest request,
            Long tenantId,
            String operator,
            LocalDate departureDate,
            String teamNo
    ) {
        Integer totalSeats = request.totalSeats() == null ? number(product.getPlannedCapacity()) : request.totalSeats();
        TeamProfileSnapshot profile = parseTeamProfile(product.getRemark());
        SalesTeamEntity entity = new SalesTeamEntity();
        entity.setTenantId(tenantId);
        entity.setProductId(product.getId());
        entity.setTeamNo(teamNo);
        entity.setTeamName(SalesTeamDisplayNameFormatter.productDisplayName(product.getProductName(), teamNo));
        entity.setTeamType(SalesTeamType.SANPIN.getValue());
        entity.setBusinessType(firstText(profile.businessType(), product.getBusinessType()));
        entity.setDepartureDate(departureDate);
        entity.setDepartmentName(clean(profile.departmentName()));
        entity.setOperatorEmployeeId(request.operatorEmployeeId());
        entity.setOperatorEmployeeName(firstText(request.operatorEmployeeName(), profile.operatorName()));
        entity.setEscortEmployeeName(clean(profile.escortName()));
        entity.setRemark(clean(profile.internalRemark()));
        if (profile.perCapitaPitAmount() != null) {
            entity.setPerCapitaPitAmount(money(profile.perCapitaPitAmount()));
        }
        if (profile.optionalMarkupRate() != null) {
            entity.setOptionalMarkupRate(money(profile.optionalMarkupRate()));
        }
        if (profile.perCapitaShoppingAmount() != null) {
            entity.setPerCapitaShoppingAmount(money(profile.perCapitaShoppingAmount()));
        }
        entity.setStatus(SalesTeamStatus.NORMAL.getValue());
        entity.setTotalSeats(totalSeats);
        entity.setUsedSeats(0);
        entity.setRemainingSeats(totalSeats);
        entity.setSingleRoomDifference(request.singleRoomDifference() == null
                ? money(product.getSingleRoomDifference())
                : money(request.singleRoomDifference()));
        entity.setCloseDaysBefore(number(product.getCloseDaysBefore()));
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        return entity;
    }

    private SalesProductEntity buildDirectProductSnapshot(
            SalesTeamDirectCreateRequest request,
            Long tenantId,
            String operator,
            String teamNo
    ) {
        SalesProductEntity entity = new SalesProductEntity();
        entity.setTenantId(tenantId);
        applyDirectProductFields(entity, request, teamNo);
        entity.setProductScope(PRODUCT_SCOPE_TEAM_SNAPSHOT);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        return entity;
    }

    /** 将直接建团或改团页面的产品快照字段写入实体。 */
    private void applyDirectProductFields(
            SalesProductEntity entity,
            SalesTeamDirectCreateRequest request,
            String teamNo
    ) {
        entity.setProductName(directProductSnapshotName(request.teamName(), teamNo));
        entity.setBusinessType(clean(request.businessType()));
        entity.setDomesticInternational(SalesProductDomesticType.fromValueOrDefault(request.domesticInternational()).getValue());
        entity.setProvince(clean(request.province()));
        entity.setCity(clean(request.city()));
        entity.setDistrict(clean(request.district()));
        entity.setTripType(SalesProductTripType.fromValueOrDefault(request.tripType()).getValue());
        entity.setReceptionStandard(clean(request.receptionStandard()));
        entity.setProductTheme(clean(request.productTheme()));
        entity.setTravelDays(request.travelDays() == null ? 1 : request.travelDays());
        entity.setCloseDaysBefore(number(request.closeDaysBefore()));
        entity.setSingleRoomDifference(money(request.singleRoomDifference()));
        entity.setPlannedCapacity(number(request.totalSeats()));
        entity.setStatus(SalesProductStatus.ACTIVE.getValue());
        entity.setRemark(clean(request.remark()));
    }

    /**
     * 直接建团创建的是团队专属产品快照，不进入产品模板唯一性校验，名称保持用户录入的团队名称。
     */
    private String directProductSnapshotName(String teamName, String teamNo) {
        String baseName = clean(teamName);
        if (!StringUtils.hasText(baseName) || baseName.length() <= PRODUCT_NAME_MAX_LENGTH) {
            return baseName;
        }
        return baseName.substring(0, PRODUCT_NAME_MAX_LENGTH);
    }

    /**
     * 编辑页展示名称时移除直接建团快照为唯一性追加的团号后缀，避免用户保存后重复追加团号。
     */
    private String directProductDisplayName(String productName, String teamNo) {
        String cleanName = clean(productName);
        String cleanTeamNo = clean(teamNo);
        if (!StringUtils.hasText(cleanName) || !StringUtils.hasText(cleanTeamNo)) {
            return cleanName;
        }
        String suffix = "-" + cleanTeamNo;
        return cleanName.endsWith(suffix) ? cleanName.substring(0, cleanName.length() - suffix.length()) : cleanName;
    }

    private SalesTeamEntity buildDirectTeam(
            SalesProductEntity product,
            SalesTeamDirectCreateRequest request,
            SalesTeamType teamType,
            Long tenantId,
            String operator,
            String teamNo
    ) {
        Integer totalSeats = number(request.totalSeats());
        SalesTeamEntity entity = new SalesTeamEntity();
        entity.setTenantId(tenantId);
        entity.setProductId(product.getId());
        entity.setTeamNo(teamNo);
        entity.setTeamName(clean(request.teamName()));
        entity.setTeamType(teamType.getValue());
        entity.setBusinessType(clean(request.businessType()));
        entity.setDepartureDate(request.departureDate());
        entity.setStatus(SalesTeamStatus.NORMAL.getValue());
        entity.setTotalSeats(totalSeats);
        entity.setUsedSeats(0);
        entity.setRemainingSeats(totalSeats);
        entity.setSingleRoomDifference(money(request.singleRoomDifference()));
        entity.setCloseDaysBefore(number(request.closeDaysBefore()));
        entity.setCreatedBy(operator);
        entity.setRemark(clean(request.remark()));
        entity.setIsDeleted(false);
        return entity;
    }

    /**
     * 保存团队直接创建页中的产品说明页签。
     *
     * <p>散拼、整团、散团入口在老系统中复用产品团队编辑页，因此这些说明必须落到产品快照子表，
     * 供后续团队操作页、行程单和收客说明读取。</p>
     */
    private void saveDirectProductDescription(
            Long productId,
            SalesTeamDirectCreateRequest request,
            Long tenantId,
            String operator
    ) {
        if (descriptionMapper == null || productId == null) {
            return;
        }
        SalesProductDescriptionEntity entity = new SalesProductDescriptionEntity();
        entity.setTenantId(tenantId);
        entity.setProductId(productId);
        entity.setBookingNotice(clean(request.bookingNotice()));
        entity.setProductDescription(clean(request.productDescription()));
        entity.setFeeIncluded(clean(request.feeIncluded()));
        entity.setFeeExcluded(clean(request.feeExcluded()));
        entity.setChildPolicy(clean(request.childPolicy()));
        entity.setShoppingArrangement(clean(request.shoppingArrangement()));
        entity.setOptionalItems(clean(request.optionalItems()));
        entity.setGiftItems(clean(request.giftItems()));
        entity.setAttentionItems(clean(request.attentionItems()));
        entity.setWarmReminder(clean(request.warmReminder()));
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        descriptionMapper.insert(entity);
    }

    /**
     * 保存团队直接创建页中的每日行程。
     *
     * <p>散拼、整团、散团入口和产品页复用同一套行程录入口径，路书摘要和地图点位都落到产品快照子表，
     * 这样后续团队操作页、行程单和计调安排读取的是同一份行程资料。</p>
     */
    private void saveDirectProductItineraryDays(
            Long productId,
            SalesTeamDirectCreateRequest request,
            Long tenantId,
            String operator
    ) {
        if (itineraryDayMapper == null || productId == null || request.itineraryDays() == null) {
            return;
        }
        int index = 1;
        for (var item : request.itineraryDays()) {
            SalesProductItineraryDayEntity entity = new SalesProductItineraryDayEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(productId);
            entity.setDayNo(item.dayNo() == null ? index : item.dayNo());
            entity.setDayTitle(clean(item.dayTitle()));
            entity.setItineraryContent(clean(item.itineraryContent()));
            entity.setAccommodationNote(clean(item.accommodationNote()));
            entity.setRelatedHotel(clean(item.relatedHotel()));
            entity.setSeasonalSurcharge(money(item.seasonalSurcharge()));
            entity.setBreakfastIncluded(Boolean.TRUE.equals(item.breakfastIncluded()));
            entity.setLunchIncluded(Boolean.TRUE.equals(item.lunchIncluded()));
            entity.setDinnerIncluded(Boolean.TRUE.equals(item.dinnerIncluded()));
            entity.setRoadbookPlace(clean(item.roadbookPlace()));
            entity.setRoadbookSummary(clean(item.roadbookSummary()));
            entity.setRoadbookTotalDistanceMeters(number(item.roadbookTotalDistanceMeters()));
            entity.setRoadbookTotalDurationSeconds(number(item.roadbookTotalDurationSeconds()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            itineraryDayMapper.insert(entity);
            saveDirectRoadbookPoints(productId, entity.getDayNo(), item.roadbookPoints(), tenantId, operator);
            index += 1;
        }
    }

    /**
     * 保存直接建团行程中的路书地图地点。
     *
     * <p>路书地点必须跟随产品快照保存，否则用户在新增团队页维护的地图路线保存后会只剩摘要，后续再编辑无法还原点位。</p>
     */
    private void saveDirectRoadbookPoints(
            Long productId,
            Integer dayNo,
            List<com.mtravel.platform.sales.product.dto.SalesProductRoadbookPointRequest> roadbookPoints,
            Long tenantId,
            String operator
    ) {
        if (roadbookPointMapper == null || productId == null || dayNo == null || roadbookPoints == null) {
            return;
        }
        int index = 1;
        for (var item : roadbookPoints) {
            if (!StringUtils.hasText(item.placeName())) {
                continue;
            }
            SalesProductRoadbookPointEntity entity = new SalesProductRoadbookPointEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(productId);
            entity.setDayNo(dayNo);
            entity.setPointOrder(item.pointOrder() == null ? index : item.pointOrder());
            entity.setPlaceName(clean(item.placeName()));
            entity.setAddress(clean(item.address()));
            entity.setLongitude(clean(item.longitude()));
            entity.setLatitude(clean(item.latitude()));
            entity.setPointType(StringUtils.hasText(item.pointType()) ? item.pointType() : "waypoint");
            entity.setStayMinutes(number(item.stayMinutes()));
            entity.setDistanceToNextMeters(number(item.distanceToNextMeters()));
            entity.setDurationToNextSeconds(number(item.durationToNextSeconds()));
            entity.setRemark(clean(item.remark()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            roadbookPointMapper.insert(entity);
            index += 1;
        }
    }

    /**
     * 编辑团队时只替换产品快照的说明、行程和路书。
     *
     * <p>团队安排成本和供应商明细属于后续执行数据，不应因为用户修改基本信息或行程说明被清空。</p>
     */
    private void softDeleteDirectProductContent(Long productId, Long tenantId, String operator) {
        OffsetDateTime now = OffsetDateTime.now();
        if (descriptionMapper != null) {
            SalesProductDescriptionEntity description = new SalesProductDescriptionEntity();
            description.setIsDeleted(true);
            description.setDeletedAt(now);
            description.setDeletedBy(operator);
            descriptionMapper.update(description, new UpdateWrapper<SalesProductDescriptionEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("product_id", productId)
                    .eq("is_deleted", false));
        }
        if (itineraryDayMapper != null) {
            SalesProductItineraryDayEntity itinerary = new SalesProductItineraryDayEntity();
            itinerary.setIsDeleted(true);
            itinerary.setDeletedAt(now);
            itinerary.setDeletedBy(operator);
            itineraryDayMapper.update(itinerary, new UpdateWrapper<SalesProductItineraryDayEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("product_id", productId)
                    .eq("is_deleted", false));
        }
        if (roadbookPointMapper != null) {
            SalesProductRoadbookPointEntity roadbook = new SalesProductRoadbookPointEntity();
            roadbook.setIsDeleted(true);
            roadbook.setDeletedAt(now);
            roadbook.setDeletedBy(operator);
            roadbookPointMapper.update(roadbook, new UpdateWrapper<SalesProductRoadbookPointEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("product_id", productId)
                    .eq("is_deleted", false));
        }
    }

    /**
     * 从产品团队安排备注扩展区解析默认团队属性。
     *
     * <p>当前产品团队安排页把团队默认属性暂存在 remark 的 JSON 标记后。这里仅做兼容读取，
     * 解析失败时返回空快照，避免历史备注格式影响团期生成。</p>
     */
    private TeamProfileSnapshot parseTeamProfile(String rawRemark) {
        String text = Objects.toString(rawRemark, "");
        int markerIndex = text.indexOf(TEAM_PROFILE_MARKER);
        if (markerIndex < 0) {
            return TeamProfileSnapshot.empty();
        }
        String internalRemark = clean(text.substring(0, markerIndex));
        String json = text.substring(markerIndex + TEAM_PROFILE_MARKER.length());
        return new TeamProfileSnapshot(
                jsonStringField(json, "businessType"),
                jsonStringField(json, "departmentName"),
                jsonStringField(json, "operatorName"),
                jsonStringField(json, "escortName"),
                internalRemark,
                jsonDecimalField(json, "perCapitaPitAmount"),
                jsonDecimalField(json, "optionalMarkupRate"),
                jsonDecimalField(json, "perCapitaShoppingAmount")
        );
    }

    private String jsonStringField(String json, String fieldName) {
        Matcher matcher = Pattern.compile(TEAM_PROFILE_STRING_FIELD_PATTERN.pattern().formatted(Pattern.quote(fieldName))).matcher(json);
        return matcher.find() ? clean(matcher.group(1)) : null;
    }

    private BigDecimal jsonDecimalField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(?:\"([^\"]*)\"|(-?\\d+(?:\\.\\d+)?))");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        String rawValue = firstText(matcher.group(1), matcher.group(2));
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return new BigDecimal(rawValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String cleaned = clean(value);
            if (StringUtils.hasText(cleaned)) {
                return cleaned;
            }
        }
        return null;
    }

    /**
     * 产品团队安排默认属性快照。正式团队生成时复制这些值，后续团队可单独修改。
     */
    private record TeamProfileSnapshot(
            String businessType,
            String departmentName,
            String operatorName,
            String escortName,
            String internalRemark,
            BigDecimal perCapitaPitAmount,
            BigDecimal optionalMarkupRate,
            BigDecimal perCapitaShoppingAmount
    ) {
        static TeamProfileSnapshot empty() {
            return new TeamProfileSnapshot(null, null, null, null, null, null, null, null);
        }
    }

    private SalesTeamPriceEntity buildPrice(
            SalesTeamEntity team,
            SalesTeamBatchCreateRequest request,
            Long tenantId,
            String operator
    ) {
        SalesTeamPriceEntity price = new SalesTeamPriceEntity();
        price.setTenantId(tenantId);
        price.setTeamId(team.getId());
        price.setProductId(team.getProductId());
        price.setCustomerCategoryId(request.customerCategoryId());
        price.setCustomerCategoryName(defaultCategoryName(request.customerCategoryName()));
        price.setAdultPrice(money(request.adultPrice()));
        price.setChildPrice(money(request.childPrice()));
        price.setChildNoBedPrice(money(request.childNoBedPrice()));
        price.setSeniorPrice(money(request.seniorPrice()));
        price.setExtraFee(money(request.extraFee()));
        price.setStatus(SalesTeamPriceStatus.ACTIVE.getValue());
        price.setCreatedBy(operator);
        price.setIsDeleted(false);
        return price;
    }

    private void batchUpdateTeam(SalesTeamEntity current, SalesTeamBatchEditRequest request, Long tenantId) {
        SalesTeamEntity update = new SalesTeamEntity();
        Integer usedSeats = number(current.getUsedSeats());
        if (Boolean.TRUE.equals(request.updateTotalSeats())) {
            Integer totalSeats = number(request.totalSeats());
            if (totalSeats < usedSeats) {
                throw new BizException("总位数不能小于已占用位数");
            }
            update.setTotalSeats(totalSeats);
            update.setRemainingSeats(totalSeats - usedSeats);
        }
        if (Boolean.TRUE.equals(request.updateSingleRoomDifference())) {
            update.setSingleRoomDifference(money(request.singleRoomDifference()));
        }
        int updated = teamMapper.update(update, baseTeamUpdate(tenantId).eq("id", current.getId()));
        if (updated == 0) {
            throw new BizException("团队不存在或已删除");
        }
    }

    private boolean hasPricePayload(SalesTeamBatchEditRequest request) {
        return request.adultPrice() != null
                || request.childPrice() != null
                || request.childNoBedPrice() != null
                || request.seniorPrice() != null
                || request.extraFee() != null;
    }

    private void savePricesByBatchRule(
            SalesTeamEntity team,
            SalesTeamBatchEditRequest request,
            Long tenantId,
            String operator
    ) {
        List<SalesTeamBatchEditRequest.CustomerCategoryItem> categories =
                Objects.requireNonNullElse(request.customerCategories(), List.of());
        if (!categories.isEmpty()) {
            for (SalesTeamBatchEditRequest.CustomerCategoryItem category : categories) {
                saveBatchPriceForCategory(team, request, category, tenantId, operator);
            }
            return;
        }
        List<SalesTeamPriceEntity> existingPrices = priceMapper.selectList(basePriceQuery(tenantId).eq("team_id", team.getId()));
        if (existingPrices.isEmpty()) {
            saveBatchPriceForCategory(team, request, new SalesTeamBatchEditRequest.CustomerCategoryItem(null, "默认"), tenantId, operator);
            return;
        }
        for (SalesTeamPriceEntity existingPrice : existingPrices) {
            SalesTeamPriceEntity update = new SalesTeamPriceEntity();
            applyBatchPriceFields(update, request);
            priceMapper.update(update, basePriceUpdate(tenantId).eq("id", existingPrice.getId()));
        }
    }

    private void saveBatchPriceForCategory(
            SalesTeamEntity team,
            SalesTeamBatchEditRequest request,
            SalesTeamBatchEditRequest.CustomerCategoryItem category,
            Long tenantId,
            String operator
    ) {
        Long categoryId = normalizeCategoryId(category.id());
        String categoryName = categoryId == null ? defaultCategoryName(category.name()) : clean(category.name());
        SalesTeamPriceEntity current = findPrice(team.getId(), categoryId, categoryName, tenantId);
        if (current == null) {
            SalesTeamPriceEntity entity = new SalesTeamPriceEntity();
            applyBatchPriceFields(entity, request);
            entity.setTenantId(tenantId);
            entity.setTeamId(team.getId());
            entity.setProductId(team.getProductId());
            entity.setCustomerCategoryId(categoryId);
            entity.setCustomerCategoryName(defaultCategoryName(categoryName));
            entity.setStatus(SalesTeamPriceStatus.ACTIVE.getValue());
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            priceMapper.insert(entity);
            return;
        }
        SalesTeamPriceEntity update = new SalesTeamPriceEntity();
        applyBatchPriceFields(update, request);
        priceMapper.update(update, basePriceUpdate(tenantId).eq("id", current.getId()));
    }

    private void deletePricesByBatchRule(
            SalesTeamEntity team,
            SalesTeamBatchEditRequest request,
            Long tenantId,
            String operator
    ) {
        SalesTeamPriceEntity update = new SalesTeamPriceEntity();
        update.setIsDeleted(true);
        update.setDeletedAt(OffsetDateTime.now());
        update.setDeletedBy(operator);
        UpdateWrapper<SalesTeamPriceEntity> wrapper = basePriceUpdate(tenantId).eq("team_id", team.getId());
        List<SalesTeamBatchEditRequest.CustomerCategoryItem> categories =
                Objects.requireNonNullElse(request.customerCategories(), List.of());
        if (!categories.isEmpty()) {
            List<Long> categoryIds = categories.stream()
                    .map(SalesTeamBatchEditRequest.CustomerCategoryItem::id)
                    .map(this::normalizeCategoryId)
                    .filter(Objects::nonNull)
                    .toList();
            boolean includeDefault = categories.stream()
                    .map(SalesTeamBatchEditRequest.CustomerCategoryItem::id)
                    .map(this::normalizeCategoryId)
                    .anyMatch(Objects::isNull);
            wrapper.and(nested -> {
                if (!categoryIds.isEmpty()) {
                    nested.in("customer_category_id", categoryIds);
                    if (includeDefault) {
                        nested.or().isNull("customer_category_id");
                    }
                } else {
                    nested.isNull("customer_category_id");
                }
            });
        }
        priceMapper.update(update, wrapper);
    }

    private void applyBatchPriceFields(SalesTeamPriceEntity entity, SalesTeamBatchEditRequest request) {
        entity.setAdultPrice(money(request.adultPrice()));
        entity.setChildPrice(money(request.childPrice()));
        entity.setChildNoBedPrice(money(request.childNoBedPrice()));
        entity.setSeniorPrice(money(request.seniorPrice()));
        entity.setExtraFee(money(request.extraFee()));
    }

    private void applyPriceFields(SalesTeamPriceEntity entity, SalesTeamPriceSaveRequest request) {
        entity.setCustomerCategoryId(request.customerCategoryId());
        entity.setCustomerCategoryName(defaultCategoryName(request.customerCategoryName()));
        entity.setAdultPrice(money(request.adultPrice()));
        entity.setChildPrice(money(request.childPrice()));
        entity.setChildNoBedPrice(money(request.childNoBedPrice()));
        entity.setSeniorPrice(money(request.seniorPrice()));
        entity.setExtraFee(money(request.extraFee()));
    }

    private SalesTeamPriceEntity findPrice(Long teamId, Long categoryId, String categoryName, Long tenantId) {
        QueryWrapper<SalesTeamPriceEntity> wrapper = basePriceQuery(tenantId).eq("team_id", teamId);
        if (categoryId != null) {
            wrapper.eq("customer_category_id", categoryId);
        } else {
            wrapper.isNull("customer_category_id").eq("customer_category_name", defaultCategoryName(categoryName));
        }
        return priceMapper.selectOne(wrapper);
    }

    private Map<Long, List<SalesTeamPriceResponse>> loadPricesForTeams(Long tenantId, List<SalesTeamEntity> teams) {
        List<Long> teamIds = teams.stream()
                .map(SalesTeamEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return priceMapper.selectList(basePriceQuery(tenantId).in("team_id", teamIds).orderByAsc("id"))
                .stream()
                .map(SalesTeamPriceResponse::fromEntity)
                .collect(Collectors.groupingBy(SalesTeamPriceResponse::teamId));
    }

    private Map<Long, SalesProductEntity> loadProductsForTeams(Long tenantId, List<SalesTeamEntity> teams) {
        List<Long> productIds = teams.stream()
                .map(SalesTeamEntity::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productMapper.selectList(baseProductQuery(tenantId).in("id", productIds))
                .stream()
                .collect(Collectors.toMap(SalesProductEntity::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, String> loadCustomerSummariesForTeams(Long tenantId, List<SalesTeamEntity> teams) {
        if (bookingOrderMapper == null) {
            return Map.of();
        }
        List<Long> teamIds = teamIds(teams);
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return bookingOrderMapper.selectList(new QueryWrapper<SalesBookingOrderEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("team_id", teamIds)
                        .ne("status", "cancelled")
                        .and(wrapper -> wrapper
                                .in("order_role", List.of("normal", "merge_child", "merge_source"))
                                .or()
                                .isNull("order_role"))
                        .orderByAsc("team_id")
                        .orderByAsc("id"))
                .stream()
                .filter(this::visibleCustomerOrder)
                .filter(order -> StringUtils.hasText(order.getCustomerName()))
                .collect(Collectors.groupingBy(
                        SalesBookingOrderEntity::getTeamId,
                        Collectors.collectingAndThen(
                                Collectors.mapping(order -> clean(order.getCustomerName()), Collectors.toCollection(java.util.LinkedHashSet::new)),
                                names -> names.isEmpty() ? null : String.join("、", names)
                        )
                ));
    }

    private boolean visibleCustomerOrder(SalesBookingOrderEntity order) {
        if (order == null || "cancelled".equals(order.getStatus())) {
            return false;
        }
        String role = StringUtils.hasText(order.getOrderRole()) ? order.getOrderRole() : "normal";
        return "normal".equals(role) || "merge_child".equals(role) || "merge_source".equals(role);
    }

    private List<DispatchTeamGuideEntity> loadActiveGuidesForTeams(Long tenantId, List<SalesTeamEntity> teams) {
        if (teamGuideMapper == null) {
            return List.of();
        }
        List<Long> teamIds = teamIds(teams);
        if (teamIds.isEmpty()) {
            return List.of();
        }
        return teamGuideMapper.selectList(new QueryWrapper<DispatchTeamGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .in("team_id", teamIds)
                .eq("status", DispatchTeamGuideStatus.ACTIVE.getValue())
                .orderByAsc("team_id")
                .orderByAsc("id"));
    }

    private Map<Long, String> buildGuideSummaries(List<DispatchTeamGuideEntity> guides) {
        if (CollectionUtils.isEmpty(guides)) {
            return Map.of();
        }
        return guides.stream()
                .filter(guide -> StringUtils.hasText(guide.getGuideName()))
                .collect(Collectors.groupingBy(
                        DispatchTeamGuideEntity::getTeamId,
                        Collectors.collectingAndThen(
                                Collectors.mapping(this::guideSummaryText, Collectors.toCollection(java.util.LinkedHashSet::new)),
                                names -> names.isEmpty() ? null : String.join("、", names)
                        )
                ));
    }

    private String guideSummaryText(DispatchTeamGuideEntity guide) {
        String name = clean(guide.getGuideName());
        if (!StringUtils.hasText(guide.getGuideMobile())) {
            return name;
        }
        return name + "[Tel:" + clean(guide.getGuideMobile()) + "]";
    }

    /**
     * 刷新单个团队的列表汇总行。
     *
     * <p>列表页只查汇总表；团队、订单、导游或安排变化后调用该方法，将相关来源表提前聚合为一行。</p>
     */
    public void refreshTeamListSummary(Long teamId, Long tenantId) {
        if (teamListSummaryRefreshService != null) {
            teamListSummaryRefreshService.refresh(teamId, tenantId);
            return;
        }
        if (teamListSummaryMapper == null || teamId == null || tenantId == null) {
            return;
        }
        SalesTeamEntity team = teamMapper.selectOne(baseTeamQuery(tenantId).eq("id", teamId));
        if (team == null) {
            SalesTeamListSummaryEntity deleted = new SalesTeamListSummaryEntity();
            deleted.setIsDeleted(true);
            teamListSummaryMapper.update(deleted, new UpdateWrapper<SalesTeamListSummaryEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("team_id", teamId));
            return;
        }
        SalesProductEntity product = team.getProductId() == null
                ? null
                : productMapper.selectOne(baseProductQuery(tenantId).eq("id", team.getProductId()));
        List<SalesBookingOrderEntity> orders = bookingOrderMapper == null
                ? List.of()
                : bookingOrderMapper.selectList(new QueryWrapper<SalesBookingOrderEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("team_id", teamId)
                        .orderByAsc("id"));
        List<SalesBookingOrderEntity> visibleOrders = orders.stream().filter(this::visibleCustomerOrder).toList();
        List<DispatchTeamGuideEntity> guides = loadActiveGuidesForTeams(tenantId, List.of(team));
        SalesTeamListResponse.ArrangePlans plans = loadArrangePlansForTeams(tenantId, List.of(team), guides)
                .getOrDefault(teamId, SalesTeamListResponse.ArrangePlans.empty());

        SalesTeamListSummaryEntity summary = new SalesTeamListSummaryEntity();
        summary.setTenantId(tenantId);
        summary.setTeamId(teamId);
        summary.setTeamNo(team.getTeamNo());
        summary.setTeamName(displayTeamName(team, product));
        summary.setTeamType(team.getTeamType());
        summary.setStatus(team.getStatus());
        summary.setDepartureDate(team.getDepartureDate());
        int days = product != null && product.getTravelDays() != null && product.getTravelDays() > 0 ? product.getTravelDays() : 1;
        summary.setTravelDays(days);
        summary.setEndDate(team.getDepartureDate() == null ? null : team.getDepartureDate().plusDays(days - 1L));
        summary.setDeparturePlace(product == null ? null : joinPlaceText(product.getProvince(), product.getCity(), product.getDistrict()));
        summary.setBusinessType(firstText(team.getBusinessType(), product == null ? null : product.getBusinessType()));
        summary.setDepartmentName(clean(team.getDepartmentName()));
        summary.setOperatorEmployeeName(clean(team.getOperatorEmployeeName()));
        summary.setCustomerSummary(joinDistinct(visibleOrders.stream().map(SalesBookingOrderEntity::getCustomerName).toList()));
        summary.setSalespersonSummary(joinDistinct(visibleOrders.stream().map(SalesBookingOrderEntity::getSalespersonEmployeeName).toList()));
        summary.setOrderStatusSummary(joinDistinct(visibleOrders.stream().map(SalesBookingOrderEntity::getStatus).toList()));
        summary.setGuideSummary(buildGuideSummaries(guides).get(teamId));
        summary.setTotalSeats(number(team.getTotalSeats()));
        summary.setUsedSeats(number(team.getUsedSeats()));
        summary.setRemainingSeats(number(team.getRemainingSeats()));
        summary.setGuidePlan(plans.guidePlan());
        summary.setTrafficPlan(plans.trafficPlan());
        summary.setHotelPlan(plans.hotelPlan());
        summary.setVehiclePlan(plans.vehiclePlan());
        summary.setScenicPlan(plans.scenicPlan());
        summary.setMealPlan(plans.mealPlan());
        summary.setOtherPlan(plans.otherPlan());
        summary.setOptionalPlan(plans.optionalPlan());
        summary.setShoppingPlan(plans.shoppingPlan());
        summary.setGroundAgentPlan(plans.groundAgentPlan());
        summary.setCreatedBy(team.getCreatedBy());
        summary.setCreatedAt(team.getCreatedAt());
        summary.setRemark(team.getRemark());
        summary.setIsDeleted(false);

        int updated = teamListSummaryMapper.update(summary, new UpdateWrapper<SalesTeamListSummaryEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId));
        if (updated == 0) {
            teamListSummaryMapper.insert(summary);
        }
    }

    private String displayTeamName(SalesTeamEntity team, SalesProductEntity product) {
        if (StringUtils.hasText(team.getTeamName())) {
            return clean(team.getTeamName());
        }
        return product == null ? null : SalesTeamDisplayNameFormatter.productDisplayName(product.getProductName(), team.getTeamNo());
    }

    private String joinDistinct(List<String> values) {
        java.util.LinkedHashSet<String> cleaned = values.stream()
                .map(this::clean)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return cleaned.isEmpty() ? null : String.join("、", cleaned);
    }

    private String joinPlaceText(String province, String city, String district) {
        return Stream.of(province, city, district)
                .map(this::clean)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining());
    }

    private List<Long> teamIds(List<SalesTeamEntity> teams) {
        return teams.stream()
                .map(SalesTeamEntity::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 批量读取团队管理列表上的安排状态。
     *
     * <p>列表只需要判断各模块是否已安排、是否确认，因此按当前页团队 ID 聚合正式团队安排和导游排班，
     * 不加载价格明细、成本分摊和导游报账流水，避免团队管理页出现 N+1 查询或响应体过大。</p>
     */
    private Map<Long, SalesTeamListResponse.ArrangePlans> loadArrangePlansForTeams(
            Long tenantId,
            List<SalesTeamEntity> teams,
            List<DispatchTeamGuideEntity> activeGuides
    ) {
        List<Long> teamIds = teamIds(teams);
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, MutableArrangePlans> mutablePlans = teamIds.stream()
                .collect(Collectors.toMap(id -> id, id -> new MutableArrangePlans()));
        applyTeamArrangementPlans(tenantId, teamIds, mutablePlans);
        applyTeamArrangementSectionStatusPlans(tenantId, teamIds, mutablePlans);
        applyGuidePlans(activeGuides, mutablePlans);
        return mutablePlans.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toResponse()));
    }

    /** 批量聚合大交通、住宿、用车、景区等正式团队安排状态。 */
    private void applyTeamArrangementPlans(
            Long tenantId,
            List<Long> teamIds,
            Map<Long, MutableArrangePlans> plans
    ) {
        if (teamArrangementMapper == null) {
            return;
        }
        List<DispatchTeamArrangementEntity> arrangements = teamArrangementMapper.selectList(
                new QueryWrapper<DispatchTeamArrangementEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("team_id", teamIds)
                        .eq("status", ARRANGEMENT_STATUS_ACTIVE)
        );
        if (CollectionUtils.isEmpty(arrangements)) {
            return;
        }
        for (DispatchTeamArrangementEntity arrangement : arrangements) {
            if (!ARRANGEMENT_STATUS_ACTIVE.equals(arrangement.getStatus())) {
                continue;
            }
            MutableArrangePlans plan = plans.get(arrangement.getTeamId());
            if (plan == null) {
                continue;
            }
            plan.merge(arrangement.getArrangementType(), ARRANGE_STATUS_PENDING);
        }
    }

    /** 批量聚合正式团队安排分类流程状态，覆盖“有安排”推导出的待完成状态。 */
    private void applyTeamArrangementSectionStatusPlans(
            Long tenantId,
            List<Long> teamIds,
            Map<Long, MutableArrangePlans> plans
    ) {
        if (teamArrangementSectionStatusMapper == null) {
            return;
        }
        List<DispatchTeamArrangementSectionStatusEntity> sectionStatuses = teamArrangementSectionStatusMapper.selectList(
                new QueryWrapper<DispatchTeamArrangementSectionStatusEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("team_id", teamIds)
        );
        if (CollectionUtils.isEmpty(sectionStatuses)) {
            return;
        }
        for (DispatchTeamArrangementSectionStatusEntity sectionStatus : sectionStatuses) {
            MutableArrangePlans plan = plans.get(sectionStatus.getTeamId());
            if (plan == null) {
                continue;
            }
            String listStatus = switch (sectionStatus.getStatus()) {
                case "done" -> ARRANGE_STATUS_CONFIRMED;
                case "none" -> ARRANGE_STATUS_NONE;
                default -> ARRANGE_STATUS_PENDING;
            };
            plan.set(sectionStatus.getArrangementType(), listStatus);
        }
    }

    /** 批量聚合导游安排状态。 */
    private void applyGuidePlans(List<DispatchTeamGuideEntity> guides, Map<Long, MutableArrangePlans> plans) {
        if (CollectionUtils.isEmpty(guides)) {
            return;
        }
        for (DispatchTeamGuideEntity guide : guides) {
            if (!DispatchTeamGuideStatus.ACTIVE.getValue().equals(guide.getStatus())) {
                continue;
            }
            MutableArrangePlans plan = plans.get(guide.getTeamId());
            if (plan == null) {
                continue;
            }
            String status = Boolean.TRUE.equals(guide.getIsTentative())
                    ? ARRANGE_STATUS_PENDING
                    : ARRANGE_STATUS_CONFIRMED;
            plan.mergeGuide(status);
        }
    }

    /** 团队列表安排状态的可变聚合对象，用于处理同一团队同一模块多条安排记录的优先级。 */
    private static class MutableArrangePlans {
        private String guidePlan = ARRANGE_STATUS_NONE;
        private String trafficPlan = ARRANGE_STATUS_NONE;
        private String hotelPlan = ARRANGE_STATUS_NONE;
        private String vehiclePlan = ARRANGE_STATUS_NONE;
        private String scenicPlan = ARRANGE_STATUS_NONE;
        private String mealPlan = ARRANGE_STATUS_NONE;
        private String otherPlan = ARRANGE_STATUS_NONE;
        private String optionalPlan = ARRANGE_STATUS_NONE;
        private String shoppingPlan = ARRANGE_STATUS_NONE;
        private String groundAgentPlan = ARRANGE_STATUS_NONE;

        void mergeGuide(String status) {
            guidePlan = higherStatus(guidePlan, status);
        }

        void merge(String arrangementType, String status) {
            switch (arrangementType) {
                case "traffic" -> trafficPlan = higherStatus(trafficPlan, status);
                case "hotel" -> hotelPlan = higherStatus(hotelPlan, status);
                case "vehicle" -> vehiclePlan = higherStatus(vehiclePlan, status);
                case "scenic" -> scenicPlan = higherStatus(scenicPlan, status);
                case "meal" -> mealPlan = higherStatus(mealPlan, status);
                case "other" -> otherPlan = higherStatus(otherPlan, status);
                case "optional" -> optionalPlan = higherStatus(optionalPlan, status);
                case "shopping" -> shoppingPlan = higherStatus(shoppingPlan, status);
                case "ground_agent" -> groundAgentPlan = higherStatus(groundAgentPlan, status);
                default -> {
                    // 列表只展示老系统团队管理中的固定资源安排列，其它类型不参与图标聚合。
                }
            }
        }

        void set(String arrangementType, String status) {
            switch (arrangementType) {
                case "traffic" -> trafficPlan = status;
                case "hotel" -> hotelPlan = status;
                case "vehicle" -> vehiclePlan = status;
                case "scenic" -> scenicPlan = status;
                case "meal" -> mealPlan = status;
                case "other" -> otherPlan = status;
                case "optional" -> optionalPlan = status;
                case "shopping" -> shoppingPlan = status;
                case "ground_agent" -> groundAgentPlan = status;
                default -> {
                    // 列表只展示固定资源安排列，其它类型不参与图标聚合。
                }
            }
        }

        SalesTeamListResponse.ArrangePlans toResponse() {
            return new SalesTeamListResponse.ArrangePlans(
                    guidePlan,
                    trafficPlan,
                    hotelPlan,
                    vehiclePlan,
                    scenicPlan,
                    mealPlan,
                    otherPlan,
                    optionalPlan,
                    shoppingPlan,
                    groundAgentPlan
            );
        }

        private static String higherStatus(String current, String next) {
            return statusRank(next) > statusRank(current) ? next : current;
        }

        private static int statusRank(String status) {
            if (ARRANGE_STATUS_CONFIRMED.equals(status)) {
                return 2;
            }
            if (ARRANGE_STATUS_PENDING.equals(status)) {
                return 1;
            }
            return 0;
        }
    }

    private void updateStatus(
            Long teamId,
            Long tenantId,
            String operator,
            String remark,
            SalesTeamStatus requiredStatus,
            SalesTeamStatus targetStatus,
            SalesTeamStatusAction action
    ) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        if (!requiredStatus.getValue().equals(team.getStatus())) {
            throw new BizException("团队当前状态不能执行该操作");
        }
        SalesTeamEntity update = new SalesTeamEntity();
        update.setStatus(targetStatus.getValue());
        int updated = teamMapper.update(update, baseTeamUpdate(tenantId).eq("id", teamId).eq("status", requiredStatus.getValue()));
        if (updated == 0) {
            throw new BizException("团队状态已变化，请刷新后重试");
        }
        insertStatusLog(tenantId, teamId, requiredStatus.getValue(), targetStatus.getValue(), action, operator, remark);
        refreshTeamListSummary(teamId, tenantId);
    }

    private void cancelTeam(Long teamId, Long tenantId, String operator, String remark) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        SalesTeamStatus current = SalesTeamStatus.fromValue(team.getStatus());
        if (current == SalesTeamStatus.CANCELLED) {
            throw new BizException("团队已经取消");
        }
        SalesTeamEntity update = new SalesTeamEntity();
        update.setStatus(SalesTeamStatus.CANCELLED.getValue());
        teamMapper.update(update, baseTeamUpdate(tenantId).eq("id", teamId));
        insertStatusLog(tenantId, teamId, current.getValue(), SalesTeamStatus.CANCELLED.getValue(), SalesTeamStatusAction.CANCEL, operator, remark);
        refreshTeamListSummary(teamId, tenantId);
    }

    private String nextTeamNo(
            SalesTeamType teamType,
            Long tenantId,
            LocalDate departureDate,
            Set<String> generatedInThisBatch
    ) {
        String base = teamNoBase(teamType, departureDate);
        teamMapper.lockTeamNoGeneration(tenantId, base);
        List<String> existingNos = teamMapper.selectList(baseTeamQuery(tenantId)
                        .eq("departure_date", departureDate)
                        .likeRight("team_no", base))
                .stream()
                .filter(item -> departureDate.equals(item.getDepartureDate()))
                .map(SalesTeamEntity::getTeamNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        existingNos.addAll(generatedInThisBatch.stream().filter(no -> no.startsWith(base)).toList());
        int nextIndex = existingNos.stream()
                .map(this::suffixOf)
                .mapToInt(this::suffixIndex)
                .max()
                .orElse(-1) + 1;
        String teamNo = base + suffixByIndex(nextIndex);
        generatedInThisBatch.add(teamNo);
        return teamNo;
    }

    private String teamNoBase(SalesTeamType teamType, LocalDate departureDate) {
        String prefix = teamType == SalesTeamType.SANPIN ? DEFAULT_TEAM_NO_PREFIX : "CS-BK";
        return prefix + "-" + departureDate.format(TEAM_DATE_FORMATTER);
    }

    private String suffixOf(String teamNo) {
        Matcher matcher = TEAM_SUFFIX_PATTERN.matcher(teamNo == null ? "" : teamNo);
        return matcher.find() ? matcher.group(1) : "A";
    }

    private int suffixIndex(String suffix) {
        if (!StringUtils.hasText(suffix)) {
            return 0;
        }
        char first = suffix.charAt(0);
        if (suffix.length() == 1) {
            return Math.max(0, first - 'A');
        }
        String numericPart = suffix.substring(1);
        try {
            return 26 + Integer.parseInt(numericPart);
        } catch (NumberFormatException ignored) {
            return Math.max(0, first - 'A');
        }
    }

    private String suffixByIndex(int index) {
        if (index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return "Z" + (index - 26);
    }

    private void insertStatusLog(
            Long tenantId,
            Long teamId,
            String fromStatus,
            String toStatus,
            SalesTeamStatusAction action,
            String operator,
            String remark
    ) {
        if (teamId == null) {
            return;
        }
        SalesTeamStatusLogEntity log = new SalesTeamStatusLogEntity();
        log.setTenantId(tenantId);
        log.setTeamId(teamId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setActionType(action.getValue());
        log.setOperator(operator);
        log.setActionTime(OffsetDateTime.now());
        log.setRemark(remark);
        statusLogMapper.insert(log);
    }

    private void insertNoLog(
            Long tenantId,
            Long productId,
            LocalDate departureDate,
            String teamNo,
            String suffixCode,
            String operator
    ) {
        if (noLogMapper == null) {
            return;
        }
        SalesTeamNoLogEntity log = new SalesTeamNoLogEntity();
        log.setTenantId(tenantId);
        log.setProductId(productId);
        log.setDepartureDate(departureDate);
        log.setTeamNo(teamNo);
        log.setSuffixCode(suffixCode);
        log.setOperator(operator);
        log.setCreatedAt(OffsetDateTime.now());
        noLogMapper.insert(log);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultCategoryName(String value) {
        return StringUtils.hasText(value) ? value.trim() : "默认";
    }

    private Long normalizeCategoryId(Long categoryId) {
        return categoryId == null || categoryId == 0 ? null : categoryId;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(2);
    }

    private Integer number(Integer value) {
        return value == null ? 0 : value;
    }
}

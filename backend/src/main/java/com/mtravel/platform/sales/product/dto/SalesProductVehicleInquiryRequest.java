package com.mtravel.platform.sales.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 销售产品用车询价记录保存请求。
 *
 * <p>用于记录微信群、企业微信或电话询价后的车队报价。选定报价可以回填产品团队安排价格信息。</p>
 */
public record SalesProductVehicleInquiryRequest(
        @Min(value = 1, message = "排序号必须从1开始") Integer sortOrder,
        @Pattern(regexp = "wechat_group|enterprise_wechat|phone|other", message = "询价方式不合法") String inquiryMethod,
        @Size(max = 100) String inquiryPerson,
        OffsetDateTime inquiryTime,
        @Size(max = 160) String groupName,
        Long supplierId,
        @Size(max = 200) String supplierName,
        @DecimalMin(value = "0", message = "报价金额不能小于0") BigDecimal quotedAmount,
        Boolean includesToll,
        Boolean includesParking,
        Boolean includesDriverMeal,
        Boolean includesDriverLodging,
        @Min(value = 0, message = "可用车辆数不能小于0") Integer availableVehicleCount,
        @Size(max = 100) String replyPerson,
        OffsetDateTime replyTime,
        Long attachmentId,
        @Size(max = 500) String attachmentUrl,
        Boolean selected,
        String remark
) {}

package com.mtravel.platform.sales.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 销售产品每日路书地点实体。
 *
 * <p>一条记录表示某个产品某一天路线中的一个地图地点，保存顺序、经纬度以及到下一站的距离和预计车程。</p>
 */
@TableName("sales_product_roadbook_points")
public class SalesProductRoadbookPointEntity extends TenantSoftDeleteEntity {

    /** 所属产品 ID。 */
    @TableField("product_id")
    private Long productId;

    /** 行程第几天，从 1 开始。 */
    @TableField("day_no")
    private Integer dayNo;

    /** 当天地点顺序，从 1 开始。 */
    @TableField("point_order")
    private Integer pointOrder;

    /** 地点名称。 */
    @TableField("place_name")
    private String placeName;

    /** 地点详细地址。 */
    @TableField("address")
    private String address;

    /** 地点经度。 */
    @TableField("longitude")
    private String longitude;

    /** 地点纬度。 */
    @TableField("latitude")
    private String latitude;

    /** 地点类型：departure、waypoint、scenic、meal、shopping、hotel、arrival。 */
    @TableField("point_type")
    private String pointType;

    /** 计划停留时长，单位分钟。 */
    @TableField("stay_minutes")
    private Integer stayMinutes;

    /** 到下一站距离，单位米。 */
    @TableField("distance_to_next_meters")
    private Integer distanceToNextMeters;

    /** 到下一站预计车程，单位秒。 */
    @TableField("duration_to_next_seconds")
    private Integer durationToNextSeconds;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getDayNo() {
        return dayNo;
    }

    public void setDayNo(Integer dayNo) {
        this.dayNo = dayNo;
    }

    public Integer getPointOrder() {
        return pointOrder;
    }

    public void setPointOrder(Integer pointOrder) {
        this.pointOrder = pointOrder;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public Integer getStayMinutes() {
        return stayMinutes;
    }

    public void setStayMinutes(Integer stayMinutes) {
        this.stayMinutes = stayMinutes;
    }

    public Integer getDistanceToNextMeters() {
        return distanceToNextMeters;
    }

    public void setDistanceToNextMeters(Integer distanceToNextMeters) {
        this.distanceToNextMeters = distanceToNextMeters;
    }

    public Integer getDurationToNextSeconds() {
        return durationToNextSeconds;
    }

    public void setDurationToNextSeconds(Integer durationToNextSeconds) {
        this.durationToNextSeconds = durationToNextSeconds;
    }
}

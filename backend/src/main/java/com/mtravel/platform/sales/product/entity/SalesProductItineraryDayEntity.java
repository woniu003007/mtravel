package com.mtravel.platform.sales.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 销售产品每日行程实体，对应 sales_product_itinerary_days 表。
 *
 * <p>行程按天保存，后续生成行程单、确认件或同步团期内容时会复用这些模板信息。</p>
 */
@TableName("sales_product_itinerary_days")
public class SalesProductItineraryDayEntity extends TenantSoftDeleteEntity {

    /** 所属产品 ID。 */
    @TableField("product_id")
    private Long productId;

    /** 行程第几天，从 1 开始。 */
    @TableField("day_no")
    private Integer dayNo;

    /** 当日行程标题。 */
    @TableField("day_title")
    private String dayTitle;

    /** 当日行程内容。 */
    @TableField("itinerary_content")
    private String itineraryContent;

    /** 住宿说明。 */
    @TableField("accommodation_note")
    private String accommodationNote;

    /** 关联酒店名称或说明。 */
    @TableField("related_hotel")
    private String relatedHotel;

    /** 旺季附加费。 */
    @TableField("seasonal_surcharge")
    private BigDecimal seasonalSurcharge;

    /** 是否含早餐。 */
    @TableField("breakfast_included")
    private Boolean breakfastIncluded;

    /** 是否含中餐。 */
    @TableField("lunch_included")
    private Boolean lunchIncluded;

    /** 是否含晚餐。 */
    @TableField("dinner_included")
    private Boolean dinnerIncluded;

    /** 路书地点或关键途经地点。 */
    @TableField("roadbook_place")
    private String roadbookPlace;

    /** 当天路书路线摘要。 */
    @TableField("roadbook_summary")
    private String roadbookSummary;

    /** 当天路书总距离，单位米。 */
    @TableField("roadbook_total_distance_meters")
    private Integer roadbookTotalDistanceMeters;

    /** 当天路书预计总车程，单位秒。 */
    @TableField("roadbook_total_duration_seconds")
    private Integer roadbookTotalDurationSeconds;

    /** 产品 Word 图片展示方式：跟随景区、当天末尾或不展示。 */
    @TableField("word_image_mode")
    private String wordImageMode;

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

    public String getDayTitle() {
        return dayTitle;
    }

    public void setDayTitle(String dayTitle) {
        this.dayTitle = dayTitle;
    }

    public String getItineraryContent() {
        return itineraryContent;
    }

    public void setItineraryContent(String itineraryContent) {
        this.itineraryContent = itineraryContent;
    }

    public String getAccommodationNote() {
        return accommodationNote;
    }

    public void setAccommodationNote(String accommodationNote) {
        this.accommodationNote = accommodationNote;
    }

    public String getRelatedHotel() {
        return relatedHotel;
    }

    public void setRelatedHotel(String relatedHotel) {
        this.relatedHotel = relatedHotel;
    }

    public BigDecimal getSeasonalSurcharge() {
        return seasonalSurcharge;
    }

    public void setSeasonalSurcharge(BigDecimal seasonalSurcharge) {
        this.seasonalSurcharge = seasonalSurcharge;
    }

    public Boolean getBreakfastIncluded() {
        return breakfastIncluded;
    }

    public void setBreakfastIncluded(Boolean breakfastIncluded) {
        this.breakfastIncluded = breakfastIncluded;
    }

    public Boolean getLunchIncluded() {
        return lunchIncluded;
    }

    public void setLunchIncluded(Boolean lunchIncluded) {
        this.lunchIncluded = lunchIncluded;
    }

    public Boolean getDinnerIncluded() {
        return dinnerIncluded;
    }

    public void setDinnerIncluded(Boolean dinnerIncluded) {
        this.dinnerIncluded = dinnerIncluded;
    }

    public String getRoadbookPlace() {
        return roadbookPlace;
    }

    public void setRoadbookPlace(String roadbookPlace) {
        this.roadbookPlace = roadbookPlace;
    }

    public String getRoadbookSummary() {
        return roadbookSummary;
    }

    public void setRoadbookSummary(String roadbookSummary) {
        this.roadbookSummary = roadbookSummary;
    }

    public Integer getRoadbookTotalDistanceMeters() {
        return roadbookTotalDistanceMeters;
    }

    public void setRoadbookTotalDistanceMeters(Integer roadbookTotalDistanceMeters) {
        this.roadbookTotalDistanceMeters = roadbookTotalDistanceMeters;
    }

    public Integer getRoadbookTotalDurationSeconds() {
        return roadbookTotalDurationSeconds;
    }

    public void setRoadbookTotalDurationSeconds(Integer roadbookTotalDurationSeconds) {
        this.roadbookTotalDurationSeconds = roadbookTotalDurationSeconds;
    }

    public String getWordImageMode() {
        return wordImageMode;
    }

    public void setWordImageMode(String wordImageMode) {
        this.wordImageMode = wordImageMode;
    }
}

package com.mtravel.platform.sales.booking.aiimport.service;

import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportRequest;
import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 确认单 AI 辅助录入真实样例验收测试。
 *
 * <p>这些测试使用用户提供的真实确认单资料作为金标准，重点保护游客名单不漏行。模型能力接入前，
 * Word/Excel 也必须通过本地提取和规则解析达到可验收的识别效果。</p>
 */
class BookingAiImportGoldenSampleTest {

    private final LocalBookingImportParser parser = new LocalBookingImportParser(new IdCardValidator());
    private final BookingImportAttachmentTextExtractor extractor = new BookingImportAttachmentTextExtractor();

    @Test
    void docxGoldenSampleShouldKeepAllVisibleGuestsAndCoreTravelFields() throws IOException {
        String text = extract("确认单/06.25-悦色江南（地接确认）杭州百缘.docx", "docx");

        var response = parser.parse(text, "docx");

        assertThat(response.travelInfo().outboundTrafficNo()).isEqualTo("CZ6533");
        assertThat(response.travelInfo().returnTrafficNo()).isEqualTo("CZ6536");
        assertThat(response.travelInfo().outboundStationName()).isEqualTo("上海");
        assertThat(response.travelInfo().returnStationName()).isEqualTo("上海");
        assertThat(response.customerInfo().customerName()).contains("杭州百缘");
        assertThat(response.priceInfo().priceLines())
                .anySatisfy(line -> assertThat(line).contains("房", "9280元"))
                .anySatisfy(line -> assertThat(line).contains("车", "7000元"))
                .anySatisfy(line -> assertThat(line).contains("门", "6740元"))
                .anySatisfy(line -> assertThat(line).contains("餐", "2960元"))
                .anySatisfy(line -> assertThat(line).contains("导游", "2400元"))
                .anySatisfy(line -> assertThat(line).contains("总计", "28380元"));
        assertThat(response.priceInfo().priceLines()).doesNotContain("儿童 1.2元");
        assertThat(response.priceInfo().totalAmount()).isEqualTo("28380");
        assertThat(response.guests()).hasSize(17);
        assertThat(response.guests()).extracting("name")
                .containsExactly(
                        "张百全", "张磊", "曹月焕", "刘松", "曹睿格", "范洪娥", "曹睿洢", "段楠", "韩怡如",
                        "田明明", "贾玉英", "张涵晨", "王恩德", "孙正忠", "孙豪泽", "丛钲文", "丛世岩"
                );
        assertThat(response.guests()).extracting("certificateNo")
                .contains("210204198206214832", "210281201012158614", "210281198006278615");
        assertThat(response.guestSummary().guestCount()).isEqualTo(17);
        assertThat(response.guestSummary().missingRequiredCount()).isZero();
        assertThat(response.moduleScores().guestListScore()).isGreaterThanOrEqualTo(0.99);
    }

    @Test
    void docxGoldenSampleShouldSplitRoomGroupsByMergedContactBlock() throws IOException {
        String text = extract("确认单/06.25-悦色江南（地接确认）杭州百缘.docx", "docx");

        var response = parser.parse(text, "docx");

        assertSameRoomGroup(response, "张百全", "张磊", "曹月焕");
        assertSameRoomGroup(response, "刘松", "曹睿格");
        assertDifferentRoomGroup(response, "张百全", "刘松");
        assertSameRoomGroup(response, "段楠", "韩怡如");
        assertSameRoomGroup(response, "田明明", "贾玉英");
        assertDifferentRoomGroup(response, "段楠", "田明明");
    }

    @Test
    void xlsGuestListGoldenSampleShouldKeepLeaderAndNineGuests() throws IOException {
        String text = extract("确认单/MZ-HD-20260606-CZ杭州【零购江南】客人名单表.xls", "xls");

        var response = parser.parse(text, "xls");

        assertThat(response.travelInfo().outboundTrafficNo()).isEqualTo("CZ5817");
        assertThat(response.travelInfo().returnTrafficNo()).isEqualTo("CZ5868");
        assertThat(response.guests()).hasSize(10);
        assertThat(response.guests()).extracting("name")
                .containsExactly("刘思慧", "刘鹏飞", "徐紫冉", "王萍", "马梅华", "于立红", "郁桂霞", "李洪军", "王瑛", "丁伟");
        assertThat(response.guests().get(0).leader()).isTrue();
        assertThat(response.guests().get(0).roomGroup()).matches("\\d+房");
        assertThat(response.guests().get(0).roomingRemark()).contains("拼住单女");
        assertThat(response.guests().stream()
                .filter(item -> "王瑛".equals(item.name()) || "丁伟".equals(item.name()))
                .map(BookingAiImportResponse.GuestInfo::roomGroup)
                .toList())
                .hasSize(2)
                .allSatisfy(roomGroup -> assertThat(roomGroup).matches("\\d+房"))
                .containsOnly(response.guests().stream()
                        .filter(item -> "王瑛".equals(item.name()))
                        .findFirst()
                        .orElseThrow()
                        .roomGroup());
        assertThat(response.guests().stream()
                .filter(item -> "王瑛".equals(item.name()) || "丁伟".equals(item.name()))
                .map(BookingAiImportResponse.GuestInfo::roomingRemark)
                .toList())
                .containsExactly("1大床（必须保证大床）", "1大床（必须保证大床）");
        assertThat(response.guests()).extracting("certificateNo")
                .contains("210181200106174025", "210502196312071833");
        assertThat(response.guestSummary().guestCount()).isEqualTo(10);
        assertThat(response.moduleScores().guestListScore()).isGreaterThanOrEqualTo(0.99);
    }

    @Test
    void requestShouldSupportMultipleAttachmentsForMergedRecognition() {
        BookingAiImportRequest request = new BookingAiImportRequest(null, null, java.util.List.of(1L, 2L, 3L), null);

        assertThat(request.attachmentIds()).containsExactly(1L, 2L, 3L);
    }

    @Test
    void pngOcrTextShouldKeepRecognizedGuestRows() {
        String ocrText = """
                6、客人电话或者其他信息不明，至少提前一天与我社联系，否则当天临时出现的任何状况自行承担。
                7、接团前一天导游分别和客人联系，核对交通信息（航班信息/单地接等），否则产生的后果自行承担。
                C1-PVG-260508-6D 江南壹号
                序号 姓名 身份证 电话 分房 航班
                1 刘艳 210319196309164824 18841243188 1标间
                2 王广富 210381196305270855
                3 赵云莉 210311196602270043 13050014369 1标间
                4 杨海璐 210181200101283724 15524459586 大床 新婚 5.08 沈阳-上海CZ650 3 (08:00-10:40)
                5 王星 130825199901142016
                6 李楠 210311197402010065 13841290528 1标间 5.13上海-沈阳CZ6510 (07:55-10:25)
                7 黄绍永 210311197007080039
                8 周洪伟 230182196811030861 15840187310 1标间
                9 李保平 230182196410100830 15840187320
                10 马家清 211202195010030032 15641017887 1标间，南航主管华东的领导的父母一定一定要照顾好 5.08 沈阳-上海CZ650 3 (08:00-10:40)
                11 何丽 211202195112020062
                南航会员：请导游协助团队客人注册南航会员 流程如下******
                1.根据流程给游客注册南航会员
                """;

        var response = parser.parse(ocrText, "png");

        assertThat(response.guests()).hasSize(11);
        assertThat(response.guests()).extracting("name")
                .containsExactly("刘艳", "王广富", "赵云莉", "杨海璐", "王星", "李楠", "黄绍永", "周洪伟", "李保平", "马家清", "何丽");
        assertThat(response.travelInfo().outboundTrafficNo()).isEqualTo("CZ6503");
        assertThat(response.travelInfo().returnTrafficNo()).isEqualTo("CZ6510");
    }

    private String extract(String relativePath, String sourceType) throws IOException {
        Path path = Path.of("..", relativePath).normalize();
        try (InputStream input = Files.newInputStream(path)) {
            return extractor.extract(input, sourceType);
        }
    }

    private void assertSameRoomGroup(BookingAiImportResponse response, String firstName, String... otherNames) {
        String roomGroup = roomGroupOf(response, firstName);
        assertThat(roomGroup).isNotBlank();
        for (String otherName : otherNames) {
            assertThat(roomGroupOf(response, otherName)).isEqualTo(roomGroup);
        }
    }

    private void assertDifferentRoomGroup(BookingAiImportResponse response, String firstName, String secondName) {
        assertThat(roomGroupOf(response, firstName)).isNotEqualTo(roomGroupOf(response, secondName));
    }

    private String roomGroupOf(BookingAiImportResponse response, String name) {
        return response.guests().stream()
                .filter(guest -> name.equals(guest.name()))
                .findFirst()
                .orElseThrow()
                .roomGroup();
    }
}

package com.mtravel.platform.sales.booking.aiimport.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 确认单 AI 辅助录入服务测试。
 *
 * <p>首版必须在没有模型 Key 的情况下仍能解析文本/Word 中明显结构，并给出可编辑草稿。</p>
 */
class BookingAiImportServiceTest {

    @Test
    void recognizeShouldExtractTravelInfoGuestsRoomingAndLeaderFromPlainText() {
        BookingAiImportService service = new BookingAiImportService(
                new LocalBookingImportParser(new IdCardValidator()),
                new BailianAiModelClient(null, "qwen-plus", "qwen-vl-ocr-latest")
        );
        String text = """
                航班时间：
                2026年6月25日 大连-上海CZ6533（0910-1120）
                2026年6月30日 上海-大连CZ6536（1920-2115）
                导游：王导 13800000000
                客户：杭州百缘 叶菊莲 13521124678
                报价：成人 2999 元/人，儿童 1999 元/人，单房差 580 元
                附加说明：张三、李四住一间，王五为领队。
                序号 姓名 年龄 出生日期 身份证号 电话 分房 备注
                1 张三 44 1982-06-21 210204198206214832 13521124678 1房 领队
                2 李四 15 2010-10-28 21020420101028741X 13521124678 1房
                """;

        var response = service.recognize(new BookingAiImportRequest(text, null, null), 1L, "admin");

        assertThat(response.travelInfo().outboundTrafficNo()).isEqualTo("CZ6533");
        assertThat(response.travelInfo().returnTrafficNo()).isEqualTo("CZ6536");
        assertThat(response.guideInfo().guideName()).isEqualTo("王导");
        assertThat(response.customerInfo().contactName()).isEqualTo("叶菊莲");
        assertThat(response.priceInfo().adultPrice()).isEqualTo("2999");
        assertThat(response.additionalInfo().notes()).contains("张三、李四住一间");
        assertThat(response.guests()).hasSize(2);
        assertThat(response.guests().get(0).leader()).isTrue();
        assertThat(response.guests().get(0).roomGroup()).isEqualTo("1房");
        assertThat(response.guests().get(0).warnings()).isEmpty();
    }

    @Test
    void recognizeShouldMarkInvalidGuestIdCard() {
        BookingAiImportService service = new BookingAiImportService(
                new LocalBookingImportParser(new IdCardValidator()),
                new BailianAiModelClient(null, "qwen-plus", "qwen-vl-ocr-latest")
        );

        var response = service.recognize(new BookingAiImportRequest(
                "1 张三 44 1982-06-21 210204198206214831 13521124678 1房", null, null
        ), 1L, "admin");

        assertThat(response.guests()).hasSize(1);
        assertThat(response.guests().get(0).warnings()).contains("身份证校验位不正确");
    }

    @Test
    void recognizeShouldExtractWechatTextGuestsWithoutSequenceNumber() {
        BookingAiImportService service = new BookingAiImportService(
                new LocalBookingImportParser(new IdCardValidator()),
                new BailianAiModelClient(null, "qwen-plus", "qwen-vl-ocr-latest")
        );
        String text = """
                越游越幸福 一行四人
                27号济南-南京南G75 (08.15-10.21)
                1号  南京南-济南西G804(16.34-19.13)

                杜玉珍372430196606060044 15006519663
                温明华372430196503050011
                温天琪370125201407310164
                温昊泽370125201705230138
                """;

        var response = service.recognize(new BookingAiImportRequest(text, null, null), 1L, "admin");

        assertThat(response.travelInfo().outboundTrafficNo()).isEqualTo("G75");
        assertThat(response.travelInfo().returnTrafficNo()).isEqualTo("G804");
        assertThat(response.guests()).hasSize(4);
        assertThat(response.guests()).extracting("name")
                .containsExactly("杜玉珍", "温明华", "温天琪", "温昊泽");
        assertThat(response.guests()).extracting("certificateNo")
                .containsExactly("372430196606060044", "372430196503050011", "370125201407310164", "370125201705230138");
        assertThat(response.guests().get(0).phone()).isEqualTo("15006519663");
        assertThat(response.guests()).allSatisfy(guest -> assertThat(guest.idCardValid()).isTrue());
        assertThat(response.guestSummary().guestCount()).isEqualTo(4);
        assertThat(response.guestSummary().suspectedMissingCount()).isZero();
    }

    @Test
    void recognizeShouldExtractBracketedGuestsFromDocConfirmationText() {
        BookingAiImportService service = new BookingAiImportService(
                new LocalBookingImportParser(new IdCardValidator()),
                new BailianAiModelClient(null, "qwen-plus", "qwen-vl-ocr-latest")
        );
        String text = """
                产品名称： 7.5臻享迪士尼5日2+1
                团　　号： BY-SP-D3-260705A 旅游天数： 5 天
                费用说明：
                成人：1170 * 2 = 2340 [7.5臻享迪士尼5日2+1 2大，全程1间，乌镇外 ]
                其它：55 * 2 = 110 [2西湖游船]
                合计金额：4858.00元
                游客名单： 合计接待人数：成人：2人 /
                客人名单明细：
                杨文杰[370102198701203723/15805312289]、李圣瑶[370102201207010023]
                """;

        var response = service.recognize(new BookingAiImportRequest(text, null, "doc"), 1L, "admin");

        assertThat(response.priceInfo().adultPrice()).isEqualTo("1170");
        assertThat(response.priceInfo().totalAmount()).isEqualTo("4858.00");
        assertThat(response.guests()).hasSize(2);
        assertThat(response.guests()).extracting("name").containsExactly("杨文杰", "李圣瑶");
        assertThat(response.guests()).extracting("certificateNo")
                .containsExactly("370102198701203723", "370102201207010023");
        assertThat(response.guests().get(0).phone()).isEqualTo("15805312289");
        assertThat(response.guestSummary().suspectedMissingCount()).isZero();
    }

    @Test
    void recognizeShouldRejectEmptyInput() {
        BookingAiImportService service = new BookingAiImportService(
                new LocalBookingImportParser(new IdCardValidator()),
                new BailianAiModelClient(null, "qwen-plus", "qwen-vl-ocr-latest")
        );

        assertThatThrownBy(() -> service.recognize(new BookingAiImportRequest(" ", null, null), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("请上传确认单或粘贴需要识别的内容");
    }

    @Test
    void recognizeShouldReadTextFromUploadedAttachmentWhenTextMissing() {
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        when(attachmentService.openStream(9L, 1L)).thenReturn(new ByteArrayInputStream("""
                航班时间：
                2026年6月25日 大连-上海CZ6533（0910-1120）
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        BookingAiImportService service = new BookingAiImportService(
                new LocalBookingImportParser(new IdCardValidator()),
                new BailianAiModelClient(null, "qwen-plus", "qwen-vl-ocr-latest"),
                attachmentService
        );

        var response = service.recognize(new BookingAiImportRequest(null, 9L, "txt"), 1L, "admin");

        assertThat(response.travelInfo().outboundTrafficNo()).isEqualTo("CZ6533");
    }

    @Test
    void recognizeShouldReadImageAttachmentThroughVisionModel() {
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        CommonAttachmentEntity entity = new CommonAttachmentEntity();
        entity.setFileExt("png");
        entity.setOriginalFilename("确认单截图.png");
        when(attachmentService.getEntity(19L, 1L)).thenReturn(entity);
        when(attachmentService.openStream(19L, 1L)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        AtomicReference<Long> visionTenantId = new AtomicReference<>();
        AiModelClient visionClient = new AiModelClient() {
            @Override
            public Optional<String> recognize(Long tenantId, String sourceText) {
                return Optional.empty();
            }

            @Override
            public Optional<String> recognizeImageOrDocument(Long tenantId, String sourceType, byte[] content) {
                visionTenantId.set(tenantId);
                return Optional.of("""
                        航班时间：
                        2026年6月25日 大连-上海CZ6533（0910-1120）
                        序号 姓名 年龄 出生日期 身份证号 电话 分房 备注
                        1 张三 44 1982-06-21 210204198206214832 13521124678 1房 领队
                        """);
            }
        };
        BookingAiImportService service = new BookingAiImportService(
                new LocalBookingImportParser(new IdCardValidator()),
                visionClient,
                attachmentService,
                new BookingImportAttachmentTextExtractor(visionClient)
        );

        var response = service.recognize(new BookingAiImportRequest(null, 19L, "png"), 1L, "admin");

        assertThat(response.travelInfo().outboundTrafficNo()).isEqualTo("CZ6533");
        assertThat(response.guests()).hasSize(1);
        assertThat(response.guests().get(0).name()).isEqualTo("张三");
        assertThat(visionTenantId).hasValue(1L);
    }

    @Test
    void attachmentExtractorShouldSupportCommonOfficeAndImageSourceTypes() {
        BookingImportAttachmentTextExtractor extractor = new BookingImportAttachmentTextExtractor();

        assertThatThrownBy(() -> extractor.extract(new ByteArrayInputStream(new byte[]{1, 2, 3}), "doc"))
                .isInstanceOf(BizException.class)
                .hasMessage("确认单文件读取失败，请检查文件是否加密或损坏");
        assertThatThrownBy(() -> extractor.extract(new ByteArrayInputStream(new byte[]{1, 2, 3}), "zip"))
                .isInstanceOf(BizException.class)
                .hasMessage("暂不支持该文件类型，请上传 Word、Excel、PDF、图片或粘贴文本");
    }

    @Test
    void attachmentExtractorShouldReadTextPdfBeforeVisionFallback() throws IOException {
        BookingImportAttachmentTextExtractor extractor = new BookingImportAttachmentTextExtractor();

        String text = extractor.extract(new ByteArrayInputStream(textPdfBytes("CZ6533")), "pdf", 1L);

        assertThat(text).contains("CZ6533");
    }

    @Test
    void attachmentExtractorShouldFallbackBlankPdfAndWebpBmpToVisionModel() throws IOException {
        AtomicReference<String> sourceType = new AtomicReference<>();
        AiModelClient visionClient = new AiModelClient() {
            @Override
            public Optional<String> recognize(Long tenantId, String sourceText) {
                return Optional.empty();
            }

            @Override
            public Optional<String> recognizeImageOrDocument(Long tenantId, String type, byte[] content) {
                sourceType.set(type);
                return Optional.of("航班时间：2026年6月25日 大连-上海CZ6533");
            }
        };
        BookingImportAttachmentTextExtractor extractor = new BookingImportAttachmentTextExtractor(visionClient);

        assertThat(extractor.extract(new ByteArrayInputStream(blankPdfBytes()), "pdf", 1L)).contains("CZ6533");
        assertThat(sourceType).hasValue("pdf");
        assertThat(extractor.extract(new ByteArrayInputStream(new byte[]{1, 2, 3}), "webp", 1L)).contains("CZ6533");
        assertThat(sourceType).hasValue("webp");
        assertThat(extractor.extract(new ByteArrayInputStream(new byte[]{1, 2, 3}), "bmp", 1L)).contains("CZ6533");
        assertThat(sourceType).hasValue("bmp");
    }

    private byte[] textPdfBytes(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(80, 720);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] blankPdfBytes() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }
}

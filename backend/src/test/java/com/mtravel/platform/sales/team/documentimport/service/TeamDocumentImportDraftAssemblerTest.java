package com.mtravel.platform.sales.team.documentimport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.sales.booking.aiimport.service.IdCardValidator;
import com.mtravel.platform.sales.booking.aiimport.service.LocalBookingImportParser;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 团队 Word 草稿组装测试，保证百炼不可用时仍可回退到本地可校验解析。 */
class TeamDocumentImportDraftAssemblerTest {

    @Test
    void assembleShouldMergeStructuredItineraryAndKeepLocalGuestDataWhenAiIsUnavailable() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()),
                new ObjectMapper()
        );
        String source = """
                2026年6月25日 大连-上海 CZ6533（09:10-11:20）
                客户：杭州百缘旅行社 叶菊莲 13521124678
                成人：2999 元/人
                1 张三 210204198206214832 13800000000 1房 领队
                """;

        var draft = assembler.assemble(source, """
                {
                  "documentType":"ground_confirmation",
                  "teamName":"悦色江南六日游",
                  "departureDate":"2026-06-25",
                  "travelDays":2,
                  "itineraryDays":[
                    {"dayNo":1,"dayTitle":"抵达上海","content":"接机入住","accommodation":"上海","breakfast":false,"lunch":false,"dinner":true},
                    {"dayNo":2,"dayTitle":"游览杭州","content":"西湖游览","accommodation":"杭州","breakfast":true,"lunch":true,"dinner":true}
                  ],
                  "resources":[{"dayNo":2,"resourceType":"scenic","resourceName":"西湖","city":"杭州"}]
                }
                """, "docx");

        assertThat(draft.documentType()).isEqualTo("ground_confirmation");
        assertThat(draft.team().teamName()).isEqualTo("悦色江南六日游");
        assertThat(draft.team().departureDate()).isEqualTo("2026-06-25");
        assertThat(draft.itineraryDays()).hasSize(2);
        assertThat(draft.resources()).singleElement().satisfies(resource -> {
            assertThat(resource.arrangementType()).isEqualTo("scenic");
            assertThat(resource.requiresConfirmation()).isTrue();
        });
        assertThat(draft.order().customerName()).isEqualTo("杭州百缘旅行社");
        assertThat(draft.guests()).singleElement().satisfies(guest -> {
            assertThat(guest.guestName()).isEqualTo("张三");
            assertThat(guest.certificateNo()).isEqualTo("210204198206214832");
            assertThat(guest.phone()).isEqualTo("13800000000");
            assertThat(guest.leaderFlag()).isTrue();
        });
    }

    @Test
    void assembleShouldFallbackToDocumentHeadersWhenAiReturnsNoUsableJson() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()),
                new ObjectMapper()
        );

        var draft = assembler.assemble("""
                2026年8月10日
                D1 上海抵达
                抵达后入住酒店
                D2 杭州西湖
                游览西湖，返回上海
                """, "not-json", "doc");

        assertThat(draft.documentType()).isEqualTo("mixed");
        assertThat(draft.itineraryDays()).extracting(item -> item.dayNo()).containsExactly(1, 2);
        assertThat(draft.resources()).isEmpty();
        assertThat(draft.warnings()).contains("未识别到可确认的发团日期，请计调手工填写");
    }

    @Test
    void assembleShouldExtractOnlyExplicitItineraryResourcesWhenAiIsUnavailable() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()),
                new ObjectMapper()
        );

        var draft = assembler.assemble("""
                华东地接确认件
                D1
                交通：大连→上海→苏州
                ‏【拙政园】（游览约120分钟）园林介绍正文中远观其它建筑。
                ‏【漫步七里山塘】（游览90分钟）
                温馨提示：【备用景区】预约不到时再调整
                远观【雷峰塔】
                D2
                交通：苏州→乌镇→杭州
                ‏【乌镇西栅】（游览2小时）
                自费游览【某演出】
                早餐后游览【新场古镇】（游览2小时）
                这是一段正文，其中提到【正文中的地点】，但不是独立资源安排。
                【汉服体验】穿越到汉唐禅境
                【接待标准】
                参考酒店：【上海鹿安花园酒店】或同级
                【费用说明中的名称】不应继续提取
                """, null, "docx");

        assertThat(draft.resources())
                .extracting(TeamDocumentImportDraft.ResourceDraft::sourceName)
                .containsExactly("拙政园", "漫步七里山塘", "乌镇西栅", "新场古镇");
        assertThat(draft.resources())
                .allSatisfy(resource -> {
                    assertThat(resource.itemKey()).startsWith("local-resource:");
                    assertThat(resource.arrangementType()).isEqualTo("scenic");
                    assertThat(resource.requiresConfirmation()).isTrue();
                    assertThat(resource.selectedResourceId()).isNull();
                });
        assertThat(draft.warnings()).contains("AI未返回资源，已从每日行程的明确景点标记生成待确认候选");
    }

    @Test
    void assembleShouldNotDuplicateLocalResourcesWhenAiAlreadyReturnedResources() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()),
                new ObjectMapper()
        );

        var draft = assembler.assemble("""
                D1
                【拙政园】（游览约120分钟）
                """, """
                {
                  "itineraryDays":[{"dayNo":1,"content":"【拙政园】（游览约120分钟）"}],
                  "resources":[{"dayNo":1,"resourceType":"scenic","resourceName":"拙政园","city":"苏州"}]
                }
                """, "docx");

        assertThat(draft.resources()).singleElement().satisfies(resource -> {
            assertThat(resource.itemKey()).isEqualTo("resource:1");
            assertThat(resource.sourceName()).isEqualTo("拙政园");
            assertThat(resource.city()).isEqualTo("苏州");
        });
        assertThat(draft.warnings()).doesNotContain("AI未返回资源，已从每日行程的明确景点标记生成待确认候选");
    }

    @Test
    void assembleShouldKeepAiResourceTimeAliasesAndUseOnlyReliableOriginalItineraryFallback() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()), new ObjectMapper()
        );

        var draft = assembler.assemble("""
                D1 杭州
                08:30 游览西湖
                10:15 游览灵隐寺
                15:00 游览雷峰塔
                09:00 和 10:00 游览断桥
                D2 上海
                14:00 游览外滩
                """, """
                {
                  "resources":[
                    {"dayNo":1,"resourceType":"scenic","resourceName":"西湖","time":"08:20"},
                    {"dayNo":1,"resourceType":"scenic","resourceName":"灵隐寺","visitTime":"10:15"},
                    {"dayNo":2,"resourceType":"scenic","resourceName":"外滩","startTime":"14:00"},
                    {"dayNo":1,"resourceType":"scenic","resourceName":"雷峰塔"},
                    {"dayNo":1,"resourceType":"scenic","resourceName":"断桥"}
                  ]
                }
                """, "docx");

        assertThat(draft.resources()).extracting(TeamDocumentImportDraft.ResourceDraft::time)
                .containsExactly("08:20", "10:15", "14:00", "15:00", null);
    }

    @Test
    void assembleShouldKeepExplicitLocalResourceTimeAndLeaveAmbiguousLineTimeBlank() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()), new ObjectMapper()
        );

        var draft = assembler.assemble("""
                D1 苏州
                【拙政园】（08:30 入园）
                【狮子林】（08:30 集合，10:00 入园）
                """, null, "docx");

        assertThat(draft.resources()).extracting(TeamDocumentImportDraft.ResourceDraft::sourceName)
                .containsExactly("拙政园", "狮子林");
        assertThat(draft.resources()).extracting(TeamDocumentImportDraft.ResourceDraft::time)
                .containsExactly("08:30", null);
    }

    @Test
    void assembleShouldDefaultGuestTypeWhenLocalParserDoesNotRecognizeOne() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()),
                new ObjectMapper()
        );

        var draft = assembler.assemble("""
                2026年8月10日
                1 李四 210204198206214832 13800000000
                """, null, "docx");

        assertThat(draft.guests()).singleElement().satisfies(guest -> {
            assertThat(guest.guestName()).isEqualTo("李四");
            assertThat(guest.guestType()).isEqualTo("adult");
        });
    }

    @Test
    void assembleShouldMapProductDescriptionAliasesAndUseWordSectionsForMissingFields() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()), new ObjectMapper()
        );

        var draft = assembler.assemble("""
                产品说明：华东六日游
                费用包含：交通、住宿
                报价不含：个人消费
                儿童安排：儿童不占床
                购物项目：全程 0 购物
                自费说明：全程 0 自费
                赠送项目：每人每天一瓶水
                特别说明：请按时集合
                温馨
                提示
                请携带身份证原件
                """, """
                {
                  "productDetails": {
                    "productIntro":"AI 提炼的产品说明",
                    "includedFees":"AI 识别的费用包含",
                    "specialInstructions":"AI 识别的特别说明",
                    "warmTips":"AI 识别的温馨提示"
                  }
                }
                """, "docx");

        assertThat(draft.productDescription()).isNotNull().satisfies(description -> {
            assertThat(description.content()).isEqualTo("AI 提炼的产品说明");
            assertThat(description.feeIncluded()).isEqualTo("AI 识别的费用包含");
            assertThat(description.feeExcluded()).isEqualTo("个人消费");
            assertThat(description.childPolicy()).isEqualTo("儿童不占床");
            assertThat(description.shoppingArrangement()).isEqualTo("全程 0 购物");
            assertThat(description.optionalItems()).isEqualTo("全程 0 自费");
            assertThat(description.giftItems()).isEqualTo("每人每天一瓶水");
            assertThat(description.attentionItems()).isEqualTo("AI 识别的特别说明");
            assertThat(description.warmReminder()).isEqualTo("AI 识别的温馨提示");
        });
    }

    @Test
    void assembleShouldExtractSplitProductDescriptionHeadersFromWordWithoutAi() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()), new ObjectMapper()
        );

        var draft = assembler.assemble("""
                儿童
                1.2 米以下儿童不占床，超高费用自理。
                报价不含
                个人消费和自由活动期间的餐费。
                特别
                说明
                景区预约失败时调整同级景区。
                温馨
                提示
                出行请携带身份证原件。
                """, null, "docx");

        assertThat(draft.productDescription()).isNotNull().satisfies(description -> {
            assertThat(description.childPolicy()).isEqualTo("1.2 米以下儿童不占床，超高费用自理。");
            assertThat(description.feeExcluded()).isEqualTo("个人消费和自由活动期间的餐费。");
            assertThat(description.attentionItems()).isEqualTo("景区预约失败时调整同级景区。");
            assertThat(description.warmReminder()).isEqualTo("出行请携带身份证原件。");
        });
    }

    @Test
    void assembleShouldKeepNestedResourcesAndChineseResourceTypes() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()), new ObjectMapper()
        );

        String aiJson = """
                {
                  "itineraryDays":[{"dayNo":1,"resources":[
                    {"name":"西湖景区","type":"景区","location":"杭州"},
                    {"resourceName":"杭州酒店","resourceType":"hotel"}
                  ]}]}
                """;
        try {
            assertThat(new ObjectMapper().readTree(aiJson).path("itineraryDays").get(0).path("resources")).hasSize(2);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
        var draft = assembler.assemble("2026年8月10日\nD1 杭州行程", aiJson, "docx");

        assertThat(draft.resources()).hasSize(2);
        assertThat(draft.resources()).extracting(TeamDocumentImportDraft.ResourceDraft::arrangementType)
                .containsExactly("scenic", "hotel");
        assertThat(draft.resources().getFirst().dayNo()).isEqualTo(1);
        assertThat(draft.resources().getFirst().sourceName()).isEqualTo("西湖景区");
    }

    @Test
    void assembleShouldExcludePartyCompanyMistakenlyClassifiedAsGroundAgent() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()), new ObjectMapper()
        );

        var draft = assembler.assemble("""
                        D1 杭州
                        ATTN：杭州百缘 叶菊莲
                        游览西湖
                        """, """
                        {
                          "parties":[
                            {
                              "direction":"receiver",
                              "businessRole":"our_company",
                              "companyName":"杭州百缘国际旅行社有限公司",
                              "contactName":"叶菊莲"
                            }
                          ],
                          "resources":[
                            {"dayNo":1,"resourceType":"ground_agent","resourceName":"杭州百缘","city":"杭州"},
                            {"dayNo":1,"resourceType":"scenic","resourceName":"西湖","city":"杭州"}
                          ]
                        }
                        """, "docx");

        assertThat(draft.resources()).singleElement().satisfies(resource -> {
            assertThat(resource.arrangementType()).isEqualTo("scenic");
            assertThat(resource.sourceName()).isEqualTo("西湖");
        });
    }

    @Test
    void assembleShouldUseAiPartiesAndCurrentCompanyToPickExternalCustomer() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()),
                new ObjectMapper()
        );

        var draft = assembler.assemble("""
                        华东地接确认件
                        FROM：东晟假期 刘小川 13800000001
                        ATTN：杭州百缘 叶菊莲 13800000002
                        2026年6月25日 大连-上海 CZ6533（09:10-11:20）
                        """,
                """
                        {
                          "teamName":"华东地接确认件",
                          "parties":[
                            {
                              "direction":"receiver",
                              "businessRole":"our_company",
                              "companyName":"杭州百缘",
                              "contactName":"叶菊莲",
                              "phoneToken":"[PHONE_2]",
                              "evidence":"ATTN：杭州百缘 叶菊莲",
                              "confidence":0.98
                            },
                            {
                              "direction":"sender",
                              "businessRole":"entrusting_party",
                              "companyName":"东晟假期",
                              "contactName":"刘小川",
                              "phoneToken":"[PHONE_1]",
                              "evidence":"FROM：东晟假期 刘小川",
                              "confidence":0.98
                            }
                          ],
                          "departureDate":"2026-06-25"
                        }
                        """,
                "docx",
                "06.25-悦色江南（地接确认）杭州百缘.docx",
                "杭州百缘国际旅行社有限公司",
                Map.of("[PHONE_1]", "13800000001", "[PHONE_2]", "13800000002"));

        assertThat(draft.team().teamName()).isEqualTo("悦色江南");
        assertThat(draft.order().customerName()).isEqualTo("东晟假期");
        assertThat(draft.order().contactName()).isEqualTo("刘小川");
        assertThat(draft.order().contactPhone()).isEqualTo("13800000001");
        assertThat(draft.warnings()).contains("团队名称已结合文件名语义推断，请人工确认");
    }

    @Test
    void assembleShouldLeaveCustomerBlankWhenAiPartiesAreAmbiguous() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()),
                new ObjectMapper()
        );

        var draft = assembler.assemble("""
                        华东地接确认件
                        ATTN：杭州百缘 叶菊莲 13800000002
                        联系人：张某 13800000003
                        """,
                """
                        {
                          "teamName":"华东地接确认件",
                          "parties":[
                            {
                              "direction":"other",
                              "businessRole":"unknown",
                              "companyName":"杭州百缘",
                              "contactName":"叶菊莲",
                              "phoneToken":"[PHONE_2]",
                              "confidence":0.62
                            },
                            {
                              "direction":"other",
                              "businessRole":"unknown",
                              "companyName":"某旅行社",
                              "contactName":"张某",
                              "phoneToken":"[PHONE_3]",
                              "confidence":0.61
                            }
                          ]
                        }
                        """,
                "docx",
                "华东地接确认件.docx",
                "杭州百缘国际旅行社有限公司",
                Map.of("[PHONE_2]", "13800000002", "[PHONE_3]", "13800000003"));

        assertThat(draft.order().customerName()).isNull();
        assertThat(draft.order().contactName()).isNull();
        assertThat(draft.order().contactPhone()).isNull();
        assertThat(draft.warnings()).contains("客户单位未能自动判断，请人工确认委托方后填写");
    }

    @Test
    void assembleShouldNotFallbackToGenericBodyTitleOrUnmappedModelPhone() {
        TeamDocumentImportDraftAssembler assembler = new TeamDocumentImportDraftAssembler(
                new LocalBookingImportParser(new IdCardValidator()),
                new ObjectMapper()
        );

        var draft = assembler.assemble("""
                        华东地接确认件
                        FROM：东晟假期 刘小川 13800000001
                        """,
                """
                        {
                          "teamName":"确认单",
                          "parties":[
                            {
                              "direction":"sender",
                              "businessRole":"entrusting_party",
                              "companyName":"东晟假期",
                              "contactName":"刘小川",
                              "phoneToken":"[PHONE_9]",
                              "confidence":0.98
                            }
                          ]
                        }
                        """,
                "docx",
                null,
                null,
                Map.of("[PHONE_1]", "13800000001"));

        assertThat(draft.team().teamName()).isEqualTo("Word导入团队");
        assertThat(draft.order().customerName()).isEqualTo("东晟假期");
        assertThat(draft.order().contactName()).isEqualTo("刘小川");
        assertThat(draft.order().contactPhone()).isNull();
        assertThat(draft.warnings()).contains("团队名称未能自动确认，请人工填写");
    }
}

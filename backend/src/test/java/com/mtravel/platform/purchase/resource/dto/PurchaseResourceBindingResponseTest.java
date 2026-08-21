package com.mtravel.platform.purchase.resource.dto;

import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseResourceBindingResponseTest {

    @Test
    void bindingShouldReturnOptionalItemsForProductWordSelection() {
        PurchaseRelationEntity relation = new PurchaseRelationEntity();
        relation.setId(58L);
        relation.setSupplierId(70L);
        relation.setPriceMode("unified");
        relation.setUnifiedPrice(new BigDecimal("200.00"));

        PurchaseResourceBindingResponse response = PurchaseResourceBindingResponse.fromEntity(
                relation,
                "测试供应商",
                List.of(),
                List.of(new ResourceSupplierOptionalItemResponse(
                        101L, "苏州游船", new BigDecimal("120.00"), "yuan_per_person", "含船票", "active"
                ))
        );

        assertThat(response.optionalItems()).hasSize(1);
        assertThat(response.optionalItems().get(0).projectName()).isEqualTo("苏州游船");
        assertThat(response.optionalItems().get(0).costPrice()).isEqualByComparingTo("120.00");
    }

    @Test
    void unifiedPriceShouldBeReadFromRelationWithoutProjectPriceLines() {
        PurchaseRelationEntity relation = new PurchaseRelationEntity();
        relation.setId(58L);
        relation.setSupplierId(70L);
        relation.setPriceMode("unified");
        relation.setUnifiedPrice(new BigDecimal("200.00"));
        relation.setPriceRemark("30人以内参考价");
        relation.setStatus("active");

        PurchaseResourceBindingResponse response = PurchaseResourceBindingResponse.fromEntity(
                relation, "测试供应商", List.of()
        );

        assertThat(response.priceMode()).isEqualTo("unified");
        assertThat(response.unifiedPrice()).isEqualByComparingTo("200.00");
        assertThat(response.priceRemark()).isEqualTo("30人以内参考价");
        assertThat(response.priceLines()).isEmpty();
    }
}

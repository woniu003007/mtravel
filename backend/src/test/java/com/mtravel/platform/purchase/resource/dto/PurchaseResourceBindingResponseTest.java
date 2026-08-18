package com.mtravel.platform.purchase.resource.dto;

import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseResourceBindingResponseTest {

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

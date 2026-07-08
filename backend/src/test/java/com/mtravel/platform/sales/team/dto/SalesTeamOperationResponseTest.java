package com.mtravel.platform.sales.team.dto;

import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 团队操作页返回对象测试。
 */
class SalesTeamOperationResponseTest {

    @Test
    void shouldExposeShoppingReconciliationAsSingleTeamOperationAction() {
        SalesTeamEntity team = new SalesTeamEntity();
        team.setId(25L);
        team.setProductId(15L);
        team.setTeamNo("CS-DJ-BK-260625A");
        team.setTeamType("sanpin");
        team.setStatus("normal");
        team.setDepartureDate(LocalDate.of(2026, 7, 7));

        SalesProductEntity product = new SalesProductEntity();
        product.setId(15L);
        product.setProductName("华东五日测试团");
        product.setTravelDays(5);

        SalesTeamOperationResponse response = SalesTeamOperationResponse.from(
                team,
                product,
                null,
                List.of()
        );

        assertThat(response.actions())
                .anySatisfy(action -> {
                    assertThat(action.code()).isEqualTo("shoppingReconciliation");
                    assertThat(action.label()).isEqualTo("购物核对/补佣");
                    assertThat(action.group()).isEqualTo("business");
                    assertThat(action.enabled()).isTrue();
                });
    }
}

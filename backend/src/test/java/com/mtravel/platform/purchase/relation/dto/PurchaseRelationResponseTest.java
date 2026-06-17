package com.mtravel.platform.purchase.relation.dto;

import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseRelationResponseTest {

    @Test
    void shouldExposeOldSystemLocationAndSupplierContactFields() {
        PurchaseRelationEntity relation = new PurchaseRelationEntity();
        relation.setId(1L);
        relation.setResourceName("苏州园林");
        relation.setSupplierId(2L);

        PurchaseResourceEntity resource = new PurchaseResourceEntity();
        resource.setProvince("江苏省");
        resource.setCity("苏州市");
        resource.setDistrict("姑苏区");

        SupplierEntity supplier = new SupplierEntity();
        supplier.setSupplierName("苏州园林票务中心");
        supplier.setContactName("张经理");
        supplier.setContactPhone("13800000000");

        PurchaseRelationResponse response = PurchaseRelationResponse.fromEntities(relation, resource, supplier);

        assertThat(response.location()).isEqualTo("江苏省 / 苏州市 / 姑苏区");
        assertThat(response.supplierName()).isEqualTo("苏州园林票务中心");
        assertThat(response.contactName()).isEqualTo("张经理");
        assertThat(response.contactPhone()).isEqualTo("13800000000");
    }
}

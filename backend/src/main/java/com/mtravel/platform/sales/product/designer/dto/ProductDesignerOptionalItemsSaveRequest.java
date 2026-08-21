package com.mtravel.platform.sales.product.designer.dto;
import jakarta.validation.constraints.NotNull;
import java.util.List;
/** 单独保存某个产品日资源的自费项目选择，空列表代表清空。 */
public record ProductDesignerOptionalItemsSaveRequest(@NotNull Long productId, @NotNull Long dayResourceId, List<ProductDesignerSelectedOptionalItemRequest> selectedOptionalItems) {}

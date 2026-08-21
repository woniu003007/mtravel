package com.mtravel.platform.sales.product.designer.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDocumentVersionResponse;
import com.mtravel.platform.sales.product.designer.service.SalesProductDesignerDocumentService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 产品设计工作台对外 Word 和成人报价单接口。 */
@RestController
@RequestMapping("/sales/product/designer/documents")
public class SalesProductDesignerDocumentController extends ControllerSupport {

    private final SalesProductDesignerDocumentService service;

    public SalesProductDesignerDocumentController(
            SalesProductDesignerDocumentService service,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
    }

    /** 查询产品已生成的文档版本。 */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping
    public ApiResponse<List<ProductDesignerDocumentVersionResponse>> list(@RequestParam Long productId) {
        return ApiResponse.ok(service.list(currentTenantId(), productId));
    }

    /** 生成产品介绍 Word。 */
    @OperationLog(module = "销售管理", type = "新增")
    @PostMapping("/product-word")
    public ApiResponse<ProductDesignerDocumentVersionResponse> productWord(
            @RequestParam Long productId,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.productWord(currentTenantId(), productId, currentOperator(authentication)));
    }

    /** 生成成人报价单 Word。 */
    @OperationLog(module = "销售管理", type = "新增")
    @PostMapping("/adult-quote")
    public ApiResponse<ProductDesignerDocumentVersionResponse> adultQuote(
            @RequestParam Long productId,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.adultQuote(currentTenantId(), productId, currentOperator(authentication)));
    }

    /** 下载当前租户下的产品文档版本。 */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/{versionId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long versionId) {
        return service.download(currentTenantId(), versionId);
    }

    /** 预览同一文档版本转换出的 PDF，不重新生成文档。 */
    @OperationLog(module = "销售管理", type = "查询")
    @GetMapping("/{versionId}/preview")
    public ResponseEntity<InputStreamResource> preview(@PathVariable Long versionId) {
        return service.preview(currentTenantId(), versionId);
    }
}

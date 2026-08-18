package com.mtravel.platform.purchase.resource.document.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.common.knowledge.dto.KnowledgeDocumentResponse;
import com.mtravel.platform.purchase.resource.document.service.PurchaseResourceDocumentService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 采购资源资料接口。
 */
@Validated
@RestController
@RequestMapping("/purchase/resource/{resourceId}/documents")
public class PurchaseResourceDocumentController extends ControllerSupport {

    private final PurchaseResourceDocumentService service;

    public PurchaseResourceDocumentController(
            PurchaseResourceDocumentService service,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.service = service;
    }

    /** 查询资源资料列表。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping
    public ApiResponse<List<KnowledgeDocumentResponse>> list(@PathVariable Long resourceId) {
        return ApiResponse.ok(service.list(currentTenantId(), resourceId));
    }

    /** 上传一个或多个资源资料文件。 */
    @OperationLog(module = "采购管理", type = "新增")
    @PostMapping("/upload")
    public ApiResponse<List<KnowledgeDocumentResponse>> upload(
            @PathVariable Long resourceId,
            @RequestPart("files") MultipartFile[] files,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.upload(
                currentTenantId(),
                resourceId,
                java.util.List.of(files),
                currentOperator(authentication)
        ));
    }

    /** 下载资源资料原文件。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long resourceId,
            @PathVariable Long documentId
    ) {
        return service.download(currentTenantId(), resourceId, documentId);
    }

    /** 删除资源资料。 */
    @OperationLog(module = "采购管理", type = "删除")
    @PostMapping("/{documentId}/delete")
    public ApiResponse<Void> delete(
            @PathVariable Long resourceId,
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        service.delete(currentTenantId(), resourceId, documentId, currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 重新处理资源资料。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/{documentId}/retry")
    public ApiResponse<KnowledgeDocumentResponse> retry(
            @PathVariable Long resourceId,
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.retry(currentTenantId(), resourceId, documentId, currentOperator(authentication)));
    }

    /** 发布资源资料。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/{documentId}/publish")
    public ApiResponse<KnowledgeDocumentResponse> publish(
            @PathVariable Long resourceId,
            @PathVariable Long documentId
    ) {
        return ApiResponse.ok(service.publish(currentTenantId(), resourceId, documentId));
    }

    /** 停用资源资料。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/{documentId}/disable")
    public ApiResponse<KnowledgeDocumentResponse> disable(
            @PathVariable Long resourceId,
            @PathVariable Long documentId
    ) {
        return ApiResponse.ok(service.disable(currentTenantId(), resourceId, documentId));
    }
}

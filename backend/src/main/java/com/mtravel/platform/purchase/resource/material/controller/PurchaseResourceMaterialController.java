package com.mtravel.platform.purchase.resource.material.controller;

import com.mtravel.platform.common.ApiResponse;
import com.mtravel.platform.common.ControllerSupport;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceImageResponse;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceImageUpdateRequest;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceIntroductionResponse;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceIntroductionSaveRequest;
import com.mtravel.platform.purchase.resource.material.service.PurchaseResourceImageService;
import com.mtravel.platform.purchase.resource.material.service.PurchaseResourceIntroductionService;
import com.mtravel.platform.system.log.web.OperationLog;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 采购资源介绍和图片素材接口。 */
@Validated
@RestController
@RequestMapping("/purchase/resource/{resourceId}/materials")
public class PurchaseResourceMaterialController extends ControllerSupport {

    private final PurchaseResourceIntroductionService introductionService;
    private final PurchaseResourceImageService imageService;

    public PurchaseResourceMaterialController(
            PurchaseResourceIntroductionService introductionService,
            PurchaseResourceImageService imageService,
            TenantProperties tenantProperties
    ) {
        super(tenantProperties);
        this.introductionService = introductionService;
        this.imageService = imageService;
    }

    /** 查询当前资源的介绍素材版本。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/introductions")
    public ApiResponse<List<PurchaseResourceIntroductionResponse>> introductions(@PathVariable Long resourceId) {
        return ApiResponse.ok(introductionService.list(currentTenantId(), resourceId));
    }

    /** 新增介绍草稿。 */
    @OperationLog(module = "采购管理", type = "新增")
    @PostMapping("/introductions")
    public ApiResponse<PurchaseResourceIntroductionResponse> createIntroduction(
            @PathVariable Long resourceId,
            @Valid @RequestBody PurchaseResourceIntroductionSaveRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(introductionService.create(
                currentTenantId(), resourceId, request, currentOperator(authentication)
        ));
    }

    /** 保存介绍草稿。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/introductions/{introductionId}")
    public ApiResponse<PurchaseResourceIntroductionResponse> updateIntroduction(
            @PathVariable Long resourceId,
            @PathVariable Long introductionId,
            @Valid @RequestBody PurchaseResourceIntroductionSaveRequest request
    ) {
        return ApiResponse.ok(introductionService.update(currentTenantId(), resourceId, introductionId, request));
    }

    /** 发布介绍并异步写入向量索引。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/introductions/{introductionId}/publish")
    public ApiResponse<PurchaseResourceIntroductionResponse> publishIntroduction(
            @PathVariable Long resourceId,
            @PathVariable Long introductionId
    ) {
        return ApiResponse.ok(introductionService.publish(currentTenantId(), resourceId, introductionId));
    }

    /** 重试已发布介绍的向量化。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/introductions/{introductionId}/retry")
    public ApiResponse<PurchaseResourceIntroductionResponse> retryIntroduction(
            @PathVariable Long resourceId,
            @PathVariable Long introductionId
    ) {
        return ApiResponse.ok(introductionService.retry(currentTenantId(), resourceId, introductionId));
    }

    /** 删除介绍和对应向量切片。 */
    @OperationLog(module = "采购管理", type = "删除")
    @PostMapping("/introductions/{introductionId}/delete")
    public ApiResponse<Void> deleteIntroduction(
            @PathVariable Long resourceId,
            @PathVariable Long introductionId,
            Authentication authentication
    ) {
        introductionService.delete(currentTenantId(), resourceId, introductionId, currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 查询当前资源的图片素材。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/images")
    public ApiResponse<List<PurchaseResourceImageResponse>> images(@PathVariable Long resourceId) {
        return ApiResponse.ok(imageService.list(currentTenantId(), resourceId));
    }

    /** 上传图片素材。 */
    @OperationLog(module = "采购管理", type = "新增")
    @PostMapping("/images/upload")
    public ApiResponse<List<PurchaseResourceImageResponse>> uploadImages(
            @PathVariable Long resourceId,
            @RequestPart("files") MultipartFile[] files,
            Authentication authentication
    ) {
        return ApiResponse.ok(imageService.upload(
                currentTenantId(), resourceId, List.of(files), currentOperator(authentication)
        ));
    }

    /** 更新图片标签和排序。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/images/{imageId}")
    public ApiResponse<PurchaseResourceImageResponse> updateImage(
            @PathVariable Long resourceId,
            @PathVariable Long imageId,
            @Valid @RequestBody PurchaseResourceImageUpdateRequest request
    ) {
        return ApiResponse.ok(imageService.update(currentTenantId(), resourceId, imageId, request));
    }

    /** 设置资源封面图片。 */
    @OperationLog(module = "采购管理", type = "修改")
    @PostMapping("/images/{imageId}/cover")
    public ApiResponse<PurchaseResourceImageResponse> setImageCover(
            @PathVariable Long resourceId,
            @PathVariable Long imageId
    ) {
        return ApiResponse.ok(imageService.setCover(currentTenantId(), resourceId, imageId));
    }

    /** 删除图片素材和原始图片文件。 */
    @OperationLog(module = "采购管理", type = "删除")
    @PostMapping("/images/{imageId}/delete")
    public ApiResponse<Void> deleteImage(
            @PathVariable Long resourceId,
            @PathVariable Long imageId,
            Authentication authentication
    ) {
        imageService.delete(currentTenantId(), resourceId, imageId, currentOperator(authentication));
        return ApiResponse.ok();
    }

    /** 下载或预览原始图片。 */
    @OperationLog(module = "采购管理", type = "查询")
    @GetMapping("/images/{imageId}/download")
    public ResponseEntity<InputStreamResource> downloadImage(
            @PathVariable Long resourceId,
            @PathVariable Long imageId
    ) {
        return imageService.download(currentTenantId(), resourceId, imageId);
    }
}

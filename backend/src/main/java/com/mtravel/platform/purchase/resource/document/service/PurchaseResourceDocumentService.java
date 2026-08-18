package com.mtravel.platform.purchase.resource.document.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.dto.AttachmentResponse;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.common.knowledge.dto.KnowledgeDocumentResponse;
import com.mtravel.platform.common.knowledge.entity.KnowledgeDocumentEntity;
import com.mtravel.platform.common.knowledge.entity.KnowledgeProcessingTaskEntity;
import com.mtravel.platform.common.knowledge.mapper.KnowledgeDocumentChunkMapper;
import com.mtravel.platform.common.knowledge.mapper.KnowledgeDocumentMapper;
import com.mtravel.platform.common.knowledge.mapper.KnowledgeProcessingTaskMapper;
import com.mtravel.platform.common.knowledge.service.KnowledgeDocumentProcessor;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import java.io.InputStream;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 采购资源资料服务。
 *
 * <p>景区、酒店、餐厅、购物、用车、大交通、地接和其它资源共用该资料链路。资料归属统一资源主档，
 * 不归属供应商。原始文件落公共附件表，知识库处理状态和向量数据单独维护。</p>
 */
@Service
public class PurchaseResourceDocumentService {

    private static final String SOURCE_TYPE = "purchase_resource";
    private static final String LEGACY_SOURCE_TYPE = "purchase_resource_scenic";
    private static final List<String> SOURCE_TYPES = List.of(SOURCE_TYPE, LEGACY_SOURCE_TYPE);
    private static final String BUSINESS_MODULE = "采购管理";
    private static final String BUSINESS_TYPE = "资源资料";
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "doc", "docx", "xls", "xlsx", "txt", "jpg", "jpeg", "png", "webp"
    );

    private final PurchaseResourceMapper resourceMapper;
    private final CommonAttachmentService attachmentService;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeDocumentChunkMapper chunkMapper;
    private final KnowledgeProcessingTaskMapper taskMapper;
    private final KnowledgeDocumentProcessor processor;

    public PurchaseResourceDocumentService(
            PurchaseResourceMapper resourceMapper,
            CommonAttachmentService attachmentService,
            KnowledgeDocumentMapper documentMapper,
            KnowledgeDocumentChunkMapper chunkMapper,
            KnowledgeProcessingTaskMapper taskMapper,
            KnowledgeDocumentProcessor processor
    ) {
        this.resourceMapper = resourceMapper;
        this.attachmentService = attachmentService;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.taskMapper = taskMapper;
        this.processor = processor;
    }

    /** 查询某个采购资源下的全部资料。 */
    public List<KnowledgeDocumentResponse> list(Long tenantId, Long resourceId) {
        validateResource(resourceId, tenantId);
        return documentMapper.selectList(new QueryWrapper<KnowledgeDocumentEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("source_type", SOURCE_TYPES)
                        .eq("source_id", resourceId)
                        .orderByDesc("id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 上传多个采购资源资料文件。 */
    @Transactional
    public List<KnowledgeDocumentResponse> upload(
            Long tenantId,
            Long resourceId,
            List<MultipartFile> files,
            String operator
    ) {
        PurchaseResourceEntity resource = validateResource(resourceId, tenantId);
        if (files == null || files.isEmpty()) {
            throw new BizException("请先选择要上传的资源资料文件");
        }
        List<KnowledgeDocumentEntity> documents = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalFilename = cleanFilename(file.getOriginalFilename());
            String fileExt = fileExtension(originalFilename);
            if (!ALLOWED_EXTENSIONS.contains(fileExt)) {
                throw new BizException("资源资料只支持 PDF、Word、Excel、文本和常规图片格式");
            }
            String sha256 = hashOf(file);
            AttachmentResponse uploaded = attachmentService.upload(
                    file,
                    BUSINESS_MODULE,
                    BUSINESS_TYPE,
                    resourceId,
                    tenantId,
                    operator
            );
            CommonAttachmentEntity attachment = attachmentService.getEntity(uploaded.id(), tenantId);
            KnowledgeDocumentEntity document = createDocument(
                    tenantId,
                    resource,
                    attachment,
                    originalFilename,
                    fileExt,
                    operator,
                    sha256
            );
            documents.add(document);
        }
        registerAfterCommitProcessing(tenantId, documents);
        return documents.stream().map(this::toResponse).toList();
    }

    /** 下载某个资源资料原文件。 */
    public ResponseEntity<InputStreamResource> download(Long tenantId, Long resourceId, Long documentId) {
        KnowledgeDocumentEntity document = loadDocument(tenantId, resourceId, documentId);
        CommonAttachmentEntity attachment = attachmentService.getEntity(document.getAttachmentId(), tenantId);
        InputStream inputStream = attachmentService.openStream(attachment.getId(), tenantId);
        String contentType = StringUtils.hasText(attachment.getContentType())
                ? attachment.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String filename = StringUtils.hasText(attachment.getOriginalFilename())
                ? attachment.getOriginalFilename()
                : document.getOriginalFilename();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename == null ? "document.bin" : filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(inputStream));
    }

    /** 删除资源资料，物理删除切片并在提交后删除原文件。 */
    @Transactional
    public void delete(Long tenantId, Long resourceId, Long documentId, String operator) {
        KnowledgeDocumentEntity document = loadDocument(tenantId, resourceId, documentId);
        Integer currentVersion = document.getIndexVersion();
        int nextVersion = nextVersion(currentVersion);
        document.setProcessingStatus("deleted");
        document.setReviewStatus("disabled");
        document.setIndexStatus("deleted");
        document.setStatus("disabled");
        document.setIndexVersion(nextVersion);
        document.setErrorMessage("资料已删除");
        document.setDeletedAt(OffsetDateTime.now());
        document.setDeletedBy(operator);
        document.setIsDeleted(true);
        documentMapper.update(document, new UpdateWrapper<KnowledgeDocumentEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", documentId)
                .eq("source_id", resourceId));
        KnowledgeProcessingTaskEntity task = new KnowledgeProcessingTaskEntity();
        task.setTaskStatus("cancelled");
        taskMapper.update(task, new UpdateWrapper<KnowledgeProcessingTaskEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("document_id", documentId)
                .eq("index_version", currentVersion)
                .in("task_status", List.of("pending", "running")));
        chunkMapper.deleteByDocument(tenantId, documentId);
        CommonAttachmentEntity attachment = attachmentService.softDelete(document.getAttachmentId(), tenantId, operator);
        attachmentService.deletePhysicalFileAfterCommit(attachment);
    }

    /** 重新处理某份资源资料。 */
    @Transactional
    public KnowledgeDocumentResponse retry(Long tenantId, Long resourceId, Long documentId, String operator) {
        KnowledgeDocumentEntity document = loadDocument(tenantId, resourceId, documentId);
        Integer currentVersion = document.getIndexVersion();
        int nextVersion = nextVersion(document.getIndexVersion());
        document.setIndexVersion(nextVersion);
        document.setProcessingStatus("pending");
        document.setIndexStatus("pending");
        document.setReviewStatus("draft");
        document.setStatus("active");
        document.setErrorMessage(null);
        documentMapper.update(document, new UpdateWrapper<KnowledgeDocumentEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", documentId)
                .eq("source_id", resourceId));
        KnowledgeProcessingTaskEntity task = new KnowledgeProcessingTaskEntity();
        task.setTaskStatus("cancelled");
        taskMapper.update(task, new UpdateWrapper<KnowledgeProcessingTaskEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("document_id", documentId)
                .eq("index_version", currentVersion)
                .in("task_status", List.of("pending", "running")));
        chunkMapper.deleteByDocument(tenantId, documentId);
        createPendingTask(document, operator);
        registerAfterCommitProcessing(tenantId, List.of(document));
        return toResponse(document);
    }

    /** 发布资源资料。 */
    @Transactional
    public KnowledgeDocumentResponse publish(Long tenantId, Long resourceId, Long documentId) {
        KnowledgeDocumentEntity document = loadDocument(tenantId, resourceId, documentId);
        document.setReviewStatus("published");
        document.setPublishedAt(OffsetDateTime.now());
        documentMapper.update(document, new UpdateWrapper<KnowledgeDocumentEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", documentId)
                .eq("source_id", resourceId));
        return toResponse(document);
    }

    /** 停用资源资料。 */
    @Transactional
    public KnowledgeDocumentResponse disable(Long tenantId, Long resourceId, Long documentId) {
        KnowledgeDocumentEntity document = loadDocument(tenantId, resourceId, documentId);
        document.setStatus("disabled");
        document.setReviewStatus("disabled");
        documentMapper.update(document, new UpdateWrapper<KnowledgeDocumentEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", documentId)
                .eq("source_id", resourceId));
        return toResponse(document);
    }

    private KnowledgeDocumentEntity createDocument(
            Long tenantId,
            PurchaseResourceEntity resource,
            CommonAttachmentEntity attachment,
            String originalFilename,
            String fileExt,
            String operator,
            String sha256
    ) {
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setTenantId(tenantId);
        entity.setSourceType(SOURCE_TYPE);
        entity.setSourceId(resource.getId());
        entity.setAttachmentId(attachment.getId());
        entity.setOriginalFilename(originalFilename);
        entity.setFileExt(fileExt);
        entity.setFileSize(attachment.getFileSize());
        entity.setFileSha256(sha256);
        entity.setProcessingStatus("pending");
        entity.setReviewStatus("draft");
        entity.setIndexStatus("pending");
        entity.setIndexVersion(1);
        entity.setUsageProductManual(true);
        entity.setUsageQa(false);
        entity.setStatus("active");
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        documentMapper.insert(entity);
        createPendingTask(entity, operator);
        return entity;
    }

    private void createPendingTask(KnowledgeDocumentEntity document, String operator) {
        KnowledgeProcessingTaskEntity extractTask = new KnowledgeProcessingTaskEntity();
        extractTask.setTenantId(document.getTenantId());
        extractTask.setDocumentId(document.getId());
        extractTask.setTaskType("extract");
        extractTask.setTaskStatus("pending");
        extractTask.setIndexVersion(document.getIndexVersion());
        extractTask.setRetryCount(0);
        extractTask.setCreatedBy(operator);
        extractTask.setIsDeleted(false);
        taskMapper.insert(extractTask);

        KnowledgeProcessingTaskEntity indexTask = new KnowledgeProcessingTaskEntity();
        indexTask.setTenantId(document.getTenantId());
        indexTask.setDocumentId(document.getId());
        indexTask.setTaskType("index");
        indexTask.setTaskStatus("pending");
        indexTask.setIndexVersion(document.getIndexVersion());
        indexTask.setRetryCount(0);
        indexTask.setCreatedBy(operator);
        indexTask.setIsDeleted(false);
        taskMapper.insert(indexTask);
    }

    private void registerAfterCommitProcessing(Long tenantId, List<KnowledgeDocumentEntity> documents) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            documents.forEach(document -> processor.processAsync(document.getId(), tenantId, document.getIndexVersion()));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                documents.forEach(document -> processor.processAsync(document.getId(), tenantId, document.getIndexVersion()));
            }
        });
    }

    private PurchaseResourceEntity validateResource(Long resourceId, Long tenantId) {
        PurchaseResourceEntity resource = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", resourceId));
        if (resource == null) {
            throw new BizException("资源不存在或已删除");
        }
        return resource;
    }

    private KnowledgeDocumentEntity loadDocument(Long tenantId, Long resourceId, Long documentId) {
        KnowledgeDocumentEntity document = documentMapper.selectOne(new QueryWrapper<KnowledgeDocumentEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .in("source_type", SOURCE_TYPES)
                .eq("source_id", resourceId)
                .eq("id", documentId)
                .last("limit 1"));
        if (document == null) {
            throw new BizException("资源资料不存在或已删除");
        }
        return document;
    }

    private KnowledgeDocumentResponse toResponse(KnowledgeDocumentEntity entity) {
        KnowledgeDocumentResponse response = KnowledgeDocumentResponse.fromEntity(entity);
        String downloadUrl = "/purchase/resource/%d/documents/%d/download".formatted(entity.getSourceId(), entity.getId());
        return new KnowledgeDocumentResponse(
                response.id(),
                response.sourceType(),
                response.sourceId(),
                response.attachmentId(),
                downloadUrl,
                response.originalFilename(),
                response.fileExt(),
                response.fileSize(),
                response.fileSha256(),
                response.processingStatus(),
                response.reviewStatus(),
                response.indexStatus(),
                response.indexVersion(),
                response.usageProductManual(),
                response.usageQa(),
                response.errorMessage(),
                response.processedAt(),
                response.publishedAt(),
                response.status(),
                response.createdBy(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private String cleanFilename(String filename) {
        String value = StringUtils.hasText(filename) ? filename.trim() : "upload.bin";
        return java.nio.file.Path.of(value).getFileName().toString();
    }

    private String fileExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase();
    }

    private String hashOf(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = file.getInputStream()) {
                inputStream.transferTo(new java.io.OutputStream() {
                    @Override
                    public void write(int b) {
                        digest.update((byte) b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) {
                        digest.update(b, off, len);
                    }
                });
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception ex) {
            return null;
        }
    }

    private int nextVersion(Integer currentVersion) {
        return currentVersion == null ? 1 : currentVersion + 1;
    }
}

package com.mtravel.platform.sales.booking.aiimport.service;

import java.util.Optional;

/**
 * AI 模型客户端抽象。
 *
 * <p>业务服务只依赖本接口，不直接依赖百炼或其它厂商 SDK。后续切换豆包、百度或私有模型时，
 * 保持识别服务和前端协议不变。</p>
 */
public interface AiModelClient {

    /**
     * 调用模型将原始文本整理成结构化 JSON。
     *
     * <p>首版允许在无模型配置时返回空，服务会使用本地规则识别兜底。</p>
     *
     * @param tenantId 当前租户 ID，用于读取租户级模型配置
     * @param sourceText 从确认单、Excel、图片 OCR 或用户粘贴中得到的文本
     * @return 模型返回的 JSON 文本；无配置或调用失败时为空
     */
    Optional<String> recognize(Long tenantId, String sourceText);

    /**
     * 调用视觉或 OCR 模型，把图片、扫描件或 PDF 页面转成文本。
     *
     * <p>无模型配置时返回空，由上层返回明确提示；实现类不得记录文件内容、身份证号和密钥。</p>
     *
     * @param tenantId 当前租户 ID
     * @param sourceType 文件类型，例如 png、jpg、pdf
     * @param content 文件二进制
     * @return OCR 文本；无配置或调用失败时为空
     */
    default Optional<String> recognizeImageOrDocument(Long tenantId, String sourceType, byte[] content) {
        return Optional.empty();
    }
}

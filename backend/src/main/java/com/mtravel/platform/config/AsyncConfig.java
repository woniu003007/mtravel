package com.mtravel.platform.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 后台异步任务配置。
 *
 * <p>知识文档抽取、OCR 和向量化不能阻塞普通上传接口，因此使用独立小线程池处理。
 * 线程数保持保守，避免内部业务系统低并发场景下抢占数据库和第三方接口资源。</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** 知识库处理任务执行器。 */
    @Bean("knowledgeTaskExecutor")
    public Executor knowledgeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("knowledge-");
        executor.initialize();
        return executor;
    }

    /** 团队 Word 智能代录识别线程池，避免百炼调用阻塞普通团队保存请求。 */
    @Bean("teamDocumentImportTaskExecutor")
    public Executor teamDocumentImportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(30);
        executor.setThreadNamePrefix("team-import-");
        executor.initialize();
        return executor;
    }
}

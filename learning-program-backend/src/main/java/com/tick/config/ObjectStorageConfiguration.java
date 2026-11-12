package com.tick.config;

import com.pig4cloud.plugin.oss.OssProperties;
import com.pig4cloud.plugin.oss.service.OssTemplate;
import com.tick.storage.OssResourceStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对象存储配置。
 * <p>
 * 总开关是 {@code oss.enable}（环境变量 {@code OSS_ENABLE}）：false 时学习资源走本地磁盘目录，
 * true 时上传 / 下载都改走 S3 兼容的对象存储。这里自己声明 {@link OssTemplate}，
 * 一是为了在启动阶段就校验连接参数（缺少 endpoint / 密钥时给出明确中文报错，
 * 而不是等到第一次上传才炸），二是允许 endpoint 不写协议前缀。
 * <p>
 * oss-spring-boot-starter 自带的 OssTemplate 带 {@code @ConditionalOnMissingBean}，
 * 检测到这里的 Bean 后会自动让位。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OssProperties.class)
public class ObjectStorageConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ObjectStorageConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "oss", name = "enable", havingValue = "true")
    public OssTemplate ossTemplate(OssProperties properties) {
        validate(properties);
        log.info("学习资源存储：对象存储已启用，endpoint={}, bucket={}, pathStyleAccess={}",
                properties.getEndpoint(), properties.getBucketName(), properties.getPathStyleAccess());
        return new OssTemplate(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "oss", name = "enable", havingValue = "true")
    public OssResourceStorage ossResourceStorage(OssTemplate ossTemplate, OssProperties properties) {
        return new OssResourceStorage(ossTemplate, properties.getBucketName());
    }

    /**
     * 校验并修补连接参数。endpoint 允许只写 host:port，缺少协议时补 http://。
     */
    private void validate(OssProperties properties) {
        String endpoint = properties.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("已启用对象存储（oss.enable=true），但未配置 oss.endpoint");
        }
        endpoint = endpoint.trim();
        if (!endpoint.contains("://")) {
            endpoint = "http://" + endpoint;
        }
        properties.setEndpoint(endpoint);

        if (properties.getAccessKey() == null || properties.getAccessKey().isBlank()) {
            throw new IllegalStateException("已启用对象存储（oss.enable=true），但未配置 oss.access-key");
        }
        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
            throw new IllegalStateException("已启用对象存储（oss.enable=true），但未配置 oss.secret-key");
        }
        if (properties.getBucketName() == null || properties.getBucketName().isBlank()) {
            throw new IllegalStateException("已启用对象存储（oss.enable=true），但未配置 oss.bucket-name");
        }
        properties.setBucketName(properties.getBucketName().trim());
    }
}

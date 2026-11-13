package com.tick.storage;

import com.pig4cloud.plugin.oss.service.OssTemplate;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

/**
 * 对象存储：通过 S3 兼容协议写入 {@code oss.bucket-name} 指定的桶。
 * 只有 {@code oss.enable=true} 时才会由 {@code ObjectStorageConfiguration} 注册成 Bean。
 * <p>
 * 对象键统一加 {@link #OBJECT_PREFIX} 前缀，桶里可以同时放别的东西而不会和资源文件打架。
 */
@Slf4j
public class OssResourceStorage implements ResourceStorage {
    private static final String OBJECT_PREFIX = "resources/";

    private final OssTemplate ossTemplate;
    private final String bucketName;

    public OssResourceStorage(OssTemplate ossTemplate, String bucketName) {
        this.ossTemplate = ossTemplate;
        this.bucketName = bucketName;
        ensureBucket();
    }

    @Override
    public StorageType type() {
        return StorageType.OSS;
    }

    /**
     * 资源文件所在的桶名，供管理端展示当前存储位置。
     */
    String bucketName() {
        return bucketName;
    }

    @Override
    public void store(String storedName, InputStream stream, long size, String contentType) {
        ossTemplate.putObject(bucketName, objectKey(storedName), stream, size, contentType);
    }

    @Override
    public InputStream open(String storedName) {
        try {
            return ossTemplate.getObject(bucketName, objectKey(storedName));
        } catch (S3Exception exception) {
            // 对象不存在按“文件丢失”处理，交给上层转成 404；其他错误（鉴权、网络）继续往上抛
            if (exception.statusCode() == 404) {
                return null;
            }
            throw exception;
        }
    }

    @Override
    public void delete(String storedName) {
        ossTemplate.removeObject(bucketName, objectKey(storedName));
    }

    /**
     * 启动时确认桶存在，不存在就建一个（MinIO 常见做法）。
     * 建桶失败只记日志，不阻断启动：权限受限的环境通常由运维预先建好桶。
     */
    private void ensureBucket() {
        try {
            if (ossTemplate.headBucket(bucketName)) {
                return;
            }
            ossTemplate.createBucket(bucketName);
            log.info("对象存储桶不存在，已自动创建：{}", bucketName);
        } catch (RuntimeException exception) {
            log.warn("对象存储桶检查 / 创建失败，bucket={}，上传可能失败，请确认桶已存在且有写入权限",
                    bucketName, exception);
        }
    }

    private String objectKey(String storedName) {
        return OBJECT_PREFIX + storedName;
    }
}

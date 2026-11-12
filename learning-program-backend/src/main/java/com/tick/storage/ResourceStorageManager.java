package com.tick.storage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * 存储实现的调度者：负责回答“新文件存哪里”和“这个文件该从哪里读”。
 * <p>
 * 新上传的文件走 {@link #defaultType()}：对象存储启用时是 OSS，否则是本地磁盘。
 * 读取 / 删除按资源行里记录的存储位置走，所以开启对象存储之后，
 * 之前落在磁盘上的老资源依然能正常下载。
 */
@Component
public class ResourceStorageManager {
    private final Map<StorageType, ResourceStorage> storages = new EnumMap<>(StorageType.class);
    private final OssResourceStorage ossStorage;

    public ResourceStorageManager(LocalResourceStorage localStorage,
                                  ObjectProvider<OssResourceStorage> ossStorageProvider) {
        // OSS 实现是条件注册的（oss.enable=true 才有），用 ObjectProvider 避免强制依赖
        this.ossStorage = ossStorageProvider.getIfAvailable();
        storages.put(StorageType.LOCAL, localStorage);
        if (ossStorage != null) {
            storages.put(StorageType.OSS, ossStorage);
        }
    }

    /**
     * 是否启用了对象存储。
     */
    public boolean objectStorageEnabled() {
        return ossStorage != null;
    }

    /**
     * 新文件默认写入的位置。
     */
    public StorageType defaultType() {
        return objectStorageEnabled() ? StorageType.OSS : StorageType.LOCAL;
    }

    /**
     * 按存储标记取实现。标记为 OSS 但对象存储已被关掉时退回本地磁盘，
     * 让下载走到明确的“文件已丢失”分支，而不是 500。
     */
    public ResourceStorage forType(StorageType type) {
        ResourceStorage storage = storages.get(type);
        return storage != null ? storage : storages.get(StorageType.LOCAL);
    }

    /**
     * 对象存储的桶名；未启用时返回 {@code null}。
     */
    public String ossBucket() {
        return ossStorage == null ? null : ossStorage.bucketName();
    }
}

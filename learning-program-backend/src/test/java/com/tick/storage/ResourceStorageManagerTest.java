package com.tick.storage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 存储调度：开关决定新文件的去向，读取按资源记录的存储位置走，关闭对象存储时安全回退。
 */
class ResourceStorageManagerTest {

    private final LocalResourceStorage localStorage = mock(LocalResourceStorage.class);
    private final OssResourceStorage ossStorage = mock(OssResourceStorage.class);
    private final ObjectProvider<OssResourceStorage> ossProvider = mock(ObjectProvider.class);

    @SuppressWarnings("unchecked")
    private ResourceStorageManager manager(boolean ossAvailable) {
        when(ossProvider.getIfAvailable()).thenReturn(ossAvailable ? ossStorage : null);
        if (ossAvailable) {
            when(ossStorage.bucketName()).thenReturn("learning-resources");
        }
        return new ResourceStorageManager(localStorage, ossProvider);
    }

    @Test
    void defaultsToLocalDiskWithoutObjectStorage() {
        ResourceStorageManager manager = manager(false);

        assertFalse(manager.objectStorageEnabled());
        assertEquals(StorageType.LOCAL, manager.defaultType());
        assertSame(localStorage, manager.forType(StorageType.LOCAL));
        // 对象存储被关掉后，标记为 oss 的老资源退到本地实现，最终给出“文件丢失”而不是 500
        assertSame(localStorage, manager.forType(StorageType.OSS));
        assertNull(manager.ossBucket());
    }

    @Test
    void prefersObjectStorageWhenEnabled() {
        ResourceStorageManager manager = manager(true);

        assertTrue(manager.objectStorageEnabled());
        assertEquals(StorageType.OSS, manager.defaultType());
        assertSame(ossStorage, manager.forType(StorageType.OSS));
        // 开启对象存储后，之前落在磁盘上的资源仍然读本地
        assertSame(localStorage, manager.forType(StorageType.LOCAL));
        assertEquals("learning-resources", manager.ossBucket());
    }

    @Test
    void unknownStorageCodeFallsBackToLocal() {
        assertEquals(StorageType.LOCAL, StorageType.from(null));
        assertEquals(StorageType.LOCAL, StorageType.from(""));
        assertEquals(StorageType.LOCAL, StorageType.from("ftp"));
        assertEquals(StorageType.OSS, StorageType.from("oss"));
        assertEquals(StorageType.OSS, StorageType.from(" OSS "));
    }
}

package com.tick.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地磁盘存储：写入、读取、删除，以及拒绝带路径成分的托管文件名。
 */
class LocalResourceStorageTest {

    private final Path root;
    private final LocalResourceStorage storage;

    LocalResourceStorageTest(@TempDir Path root) {
        this.root = root;
        this.storage = new LocalResourceStorage(root.toString());
    }

    @Test
    void storeOpenDeleteRoundTrip() throws Exception {
        byte[] body = "学习资料".getBytes(StandardCharsets.UTF_8);
        storage.store("abc123.pdf", new ByteArrayInputStream(body), body.length, "application/pdf");

        assertTrue(Files.exists(root.resolve("abc123.pdf")));
        try (InputStream stream = storage.open("abc123.pdf")) {
            assertNotNull(stream);
            assertEquals("学习资料", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }

        storage.delete("abc123.pdf");
        assertFalse(Files.exists(root.resolve("abc123.pdf")));
        assertNull(storage.open("abc123.pdf"), "删除后应当读不到");
        // 重复删除不应报错
        storage.delete("abc123.pdf");
    }

    @Test
    void rejectsStoredNameWithPathComponents() {
        // 托管文件名由服务端生成，出现目录层级说明数据被篡改过，必须拒绝
        assertThrows(IllegalArgumentException.class, () -> storage.open("../escape.txt"));
        assertThrows(IllegalArgumentException.class, () -> storage.delete("sub/dir.txt"));
    }
}

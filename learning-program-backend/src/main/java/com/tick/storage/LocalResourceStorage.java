package com.tick.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储：文件落在 {@code learning.resources.upload-dir} 目录下，
 * 这也是未启用对象存储时的默认位置。
 */
@Component
public class LocalResourceStorage implements ResourceStorage {
    private final Path root;

    public LocalResourceStorage(
            @Value("${learning.resources.upload-dir:./uploads/resources}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public StorageType type() {
        return StorageType.LOCAL;
    }

    @Override
    public void store(String storedName, InputStream stream, long size, String contentType) throws IOException {
        Files.createDirectories(root);
        Files.copy(stream, resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public InputStream open(String storedName) throws IOException {
        Path path = resolve(storedName);
        return Files.isRegularFile(path) ? Files.newInputStream(path) : null;
    }

    @Override
    public void delete(String storedName) throws IOException {
        Files.deleteIfExists(resolve(storedName));
    }

    /**
     * 解析托管文件路径。stored_name 只应是纯文件名，一旦带出目录层级就拒绝，
     * 避免历史脏数据把读写引到上传目录之外。
     */
    private Path resolve(String storedName) {
        Path path = root.resolve(storedName).normalize();
        if (!root.equals(path.getParent())) {
            throw new IllegalArgumentException("非法的资源文件名");
        }
        return path;
    }
}

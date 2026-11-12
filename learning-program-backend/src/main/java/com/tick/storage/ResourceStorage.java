package com.tick.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 资源文件存储的具体实现。同一套接口两种落地方式：本地磁盘与对象存储。
 * <p>
 * 所有方法都按“托管文件名”（stored_name）寻址；该名字由服务端生成（UUID + 校验过的小写扩展名），
 * 不含路径成分，因此实现里只需要拼上自己的前缀即可。
 */
public interface ResourceStorage {

    StorageType type();

    /**
     * 写入文件。实现需自行创建目录 / 桶等前置条件。
     */
    void store(String storedName, InputStream stream, long size, String contentType) throws IOException;

    /**
     * 打开文件流，调用方负责关闭；文件不存在时返回 {@code null}。
     */
    InputStream open(String storedName) throws IOException;

    /**
     * 删除文件；文件不存在时静默忽略。
     */
    void delete(String storedName) throws IOException;
}

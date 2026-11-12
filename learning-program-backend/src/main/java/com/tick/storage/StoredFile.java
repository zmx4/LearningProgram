package com.tick.storage;

import java.io.InputStream;

/**
 * 一笔待下载的资源文件：内容流 + 长度（未知时为 -1，交给容器按 chunked 输出）。
 * 由调用方负责关闭 {@code stream}。
 */
public record StoredFile(InputStream stream, long size) {
}

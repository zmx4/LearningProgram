package com.tick.storage;

/**
 * 学习资源文件的实际存放位置。
 * <p>
 * 数据库里以短字符串保存（见 db_learning_resource.storage），这样切换存储方式后，
 * 历史文件仍按当初落盘的位置读取，不需要做数据迁移。
 */
public enum StorageType {
    /**
     * 后端服务器本地目录（learning.resources.upload-dir）。
     */
    LOCAL("local"),
    /**
     * S3 兼容的对象存储（MinIO / 阿里云 OSS / 腾讯云 COS 等）。
     */
    OSS("oss");

    private final String code;

    StorageType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * 解析库里的存储标记。空值与无法识别的值一律按本地磁盘处理，
     * 兼容加“存储位置”字段之前写入的历史数据。
     */
    public static StorageType from(String code) {
        if (code == null || code.isBlank()) {
            return LOCAL;
        }
        for (StorageType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return LOCAL;
    }
}

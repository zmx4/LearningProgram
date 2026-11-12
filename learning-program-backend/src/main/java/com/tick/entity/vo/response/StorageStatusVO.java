package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习资源当前的存储方式，供管理端在资源页展示。
 * <p>
 * type 为生效的默认存储位置（local / oss）；bucket 仅在启用对象存储时有值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageStatusVO {
    private String type;
    private Boolean objectStorageEnabled;
    private String bucket;
}

package com.tick.service;

import com.tick.entity.dto.Account;
import com.tick.storage.StoredFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户头像服务。头像只存放在对象存储（oss.enable=true）的 avatars/ 前缀下：
 * 未启用对象存储时上传被拒绝、读取返回空，界面上也不展示头像入口。
 */
public interface AvatarService {

    /**
     * 是否启用了对象存储，即头像功能是否可用。
     */
    boolean enabled();

    /**
     * 上传并替换当前头像，成功后清理旧头像文件。
     *
     * @return 更新后的账号；账号不存在时返回 {@code null}
     * @throws IllegalArgumentException 功能未启用或文件校验失败
     */
    Account upload(Integer accountId, MultipartFile file);

    /**
     * 移除当前头像。对象存储被关掉时只清数据库记录，不阻塞移除操作。
     *
     * @return 更新后的账号；账号不存在时返回 {@code null}
     */
    Account remove(Integer accountId);

    /**
     * 打开头像文件流，调用方负责关闭；未启用对象存储、未设置头像或文件丢失时返回 {@code null}。
     */
    StoredFile open(Account account);
}
package com.tick.service;

import com.tick.entity.dto.LearningResource;
import com.tick.entity.vo.response.ResourceVO;
import com.tick.entity.vo.response.StorageStatusVO;
import com.tick.storage.StoredFile;
import com.tick.storage.StorageType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学习资源服务：上传、展示范围控制与文件读取。
 * <p>
 * 文件本体可以落在本地磁盘或对象存储，由 oss.enable 决定新文件存哪里；
 * 每条资源记录自己实际存放的位置，切换开关后历史文件依然可读。
 */
public interface LearningResourceService {
    /**
     * 学员端资源列表，仅包含 visible 为真的资源。
     */
    List<ResourceVO> listVisible();

    /**
     * 管理端资源列表，包含隐藏资源。
     */
    List<ResourceVO> listAll();

    /**
     * 上传资源文件（单个上限 50MB，扩展名仅允许字母数字），写入当前默认存储位置并保存元信息。
     */
    ResourceVO upload(MultipartFile file, String title, String description, boolean visible, Integer accountId);

    /**
     * 设置资源是否在学员端展示。
     */
    void setVisible(Integer id, boolean visible);

    /**
     * 删除资源元信息，并从它所在的存储位置清理文件。
     */
    void delete(Integer id);

    /**
     * 按 id 取资源行，供下载接口判断可见性后取文件。
     */
    LearningResource getResource(Integer id);

    /**
     * 按资源记录的位置打开文件流；文件已丢失时返回 {@code null}。
     * 调用方负责关闭返回的流。
     */
    StoredFile openFile(LearningResource resource);

    /**
     * 当前生效的存储方式，供管理端展示。
     */
    StorageStatusVO storageStatus();

    /**
     * 记录一次资源访问（下载），供小组任务中 resource 类型任务项判定完成情况。
     */
    void recordAccess(Integer accountId, Integer resourceId);

    /**
     * 新上传文件的存放位置：启用对象存储时为 OSS，否则为本地磁盘。
     */
    StorageType defaultStorageType();

    /**
     * 对象存储当前是否启用（对应 oss.enable）。
     */
    boolean objectStorageEnabled();
}

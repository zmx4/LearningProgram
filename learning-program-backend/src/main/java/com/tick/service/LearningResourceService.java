package com.tick.service;

import com.tick.entity.dto.LearningResource;
import com.tick.entity.vo.response.ResourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * 学习资源服务：上传、展示范围控制与文件读取。
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
     * 上传资源文件（单个上限 50MB，扩展名仅允许字母数字）并保存元信息。
     */
    ResourceVO upload(MultipartFile file, String title, String description, boolean visible, Integer accountId);

    /**
     * 设置资源是否在学员端展示。
     */
    void setVisible(Integer id, boolean visible);

    /**
     * 删除资源元信息并清理磁盘文件。
     */
    void delete(Integer id);

    /**
     * 按 id 取资源行，供下载接口判断可见性后取文件。
     */
    LearningResource getResource(Integer id);

    /**
     * 托管文件在磁盘上的路径。stored_name 由服务端生成（UUID + 校验过的扩展名），不含路径成分。
     */
    Path resolveStoredPath(String storedName);
}

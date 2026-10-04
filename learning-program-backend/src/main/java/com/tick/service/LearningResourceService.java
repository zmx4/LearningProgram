package com.tick.service;

import com.tick.entity.dto.LearningResource;
import com.tick.entity.vo.response.ResourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface LearningResourceService {
    List<ResourceVO> listVisible();

    List<ResourceVO> listAll();

    ResourceVO upload(MultipartFile file, String title, String description, boolean visible, Integer accountId);

    void setVisible(Integer id, boolean visible);

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

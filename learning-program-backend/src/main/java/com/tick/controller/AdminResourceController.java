package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.ResourceVisibleVO;
import com.tick.entity.vo.response.ResourceVO;
import com.tick.service.LearningResourceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学习资源的管理接口。挂在 /api/admin 下，由 SecurityConfiguration 限定为 admin 角色。
 */
@RestController
@RequestMapping("/api/admin/resources")
public class AdminResourceController {
    private final LearningResourceService resourceService;

    public AdminResourceController(LearningResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public RestBean<List<ResourceVO>> listAll() {
        return RestBean.success(resourceService.listAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestBean<ResourceVO> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam("title") String title,
                                       @RequestParam(value = "description", required = false) String description,
                                       @RequestParam(value = "visible", defaultValue = "true") boolean visible,
                                       HttpServletRequest request) {
        Integer accountId = (Integer) request.getAttribute("id");
        try {
            return RestBean.success(resourceService.upload(file, title, description, visible, accountId));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        } catch (IllegalStateException exception) {
            return RestBean.failure(500, exception.getMessage());
        }
    }

    @PutMapping("/{id}")
    public RestBean<Void> setVisible(@PathVariable Integer id, @RequestBody ResourceVisibleVO vo) {
        if (vo == null || vo.getVisible() == null) {
            return RestBean.failure(400, "visible 不能为空");
        }
        try {
            resourceService.setVisible(id, vo.getVisible());
            return RestBean.success();
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public RestBean<Void> delete(@PathVariable Integer id) {
        try {
            resourceService.delete(id);
            return RestBean.success();
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }
}

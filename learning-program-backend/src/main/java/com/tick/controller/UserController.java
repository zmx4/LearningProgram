package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.response.UserProfileVO;
import com.tick.service.AccountService;
import com.tick.service.AvatarService;
import com.tick.storage.StoredFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 用户主页接口：按 id 查询用户的公开基本信息（用户名、简介、头像）。
 * 头像读取是公开接口（img 标签无法携带 JWT），见 SecurityConfiguration 的放行规则。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final AccountService accountService;
    private final AvatarService avatarService;

    public UserController(AccountService accountService, AvatarService avatarService) {
        this.accountService = accountService;
        this.avatarService = avatarService;
    }

    @GetMapping("/{id}")
    public RestBean<UserProfileVO> getUser(@PathVariable Integer id) {
        Account account = accountService.getById(id);
        if (account == null) {
            return RestBean.failure(404, "用户不存在");
        }
        return RestBean.success(UserProfileVO.from(account, avatarService.enabled()));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable Integer id) {
        Account account = accountService.getById(id);
        if (account == null) {
            return notFound("用户不存在");
        }
        StoredFile file = avatarService.open(account);
        if (file == null) {
            return notFound("头像不存在");
        }
        // 托管文件名的扩展名由服务端按图片白名单生成，可安全地反推 Content-Type
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(avatarMediaType(account.getAvatar()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().build().toString())
                // 禁止浏览器嗅探类型，避免伪装内容被当成 HTML 执行
                .header("X-Content-Type-Options", "nosniff")
                // URL 带 ?v=托管文件名 做版本标识，内容变化即 URL 变化，可放心缓存
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
        return builder.body(new InputStreamResource(file.stream()));
    }

    private MediaType avatarMediaType(String storedName) {
        String name = storedName == null ? "" : storedName.toLowerCase();
        if (name.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (name.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (name.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    private ResponseEntity<Resource> notFound(String message) {
        byte[] body = RestBean.failure(404, message).asJsonString()
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ByteArrayResource(body));
    }
}
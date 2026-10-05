package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.DiscussionCommentCreateVO;
import com.tick.entity.vo.request.DiscussionPostCreateVO;
import com.tick.entity.vo.response.DiscussionCommentVO;
import com.tick.entity.vo.response.DiscussionPostPageVO;
import com.tick.entity.vo.response.DiscussionPostVO;
import com.tick.service.DiscussionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 讨论区接口：任何登录用户都可以发表文章和评论。
 * 删除仅限作者本人，管理员可以删除任意文章与评论。
 */
@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {
    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping
    public RestBean<DiscussionPostPageVO> list(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return RestBean.success(discussionService.listPosts(page, size));
    }

    @GetMapping("/{id}")
    public RestBean<DiscussionPostVO> detail(@PathVariable Integer id) {
        DiscussionPostVO post = discussionService.getPost(id);
        return post == null ? RestBean.failure(404, "文章不存在") : RestBean.success(post);
    }

    @PostMapping
    public RestBean<DiscussionPostVO> create(@RequestBody DiscussionPostCreateVO vo, HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        try {
            return RestBean.success(discussionService.createPost(accountId, vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public RestBean<Void> delete(@PathVariable Integer id, HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        if (discussionService.getPost(id) == null) {
            return RestBean.failure(404, "文章不存在");
        }
        if (!discussionService.deletePost(accountId, id)) {
            return RestBean.forbidden("只能删除自己发表的文章");
        }
        return RestBean.success();
    }

    @GetMapping("/{id}/comments")
    public RestBean<List<DiscussionCommentVO>> comments(@PathVariable Integer id) {
        List<DiscussionCommentVO> comments = discussionService.listComments(id);
        return comments == null ? RestBean.failure(404, "文章不存在") : RestBean.success(comments);
    }

    @PostMapping("/{id}/comments")
    public RestBean<DiscussionCommentVO> comment(@PathVariable Integer id,
                                                 @RequestBody DiscussionCommentCreateVO vo,
                                                 HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        try {
            DiscussionCommentVO comment = discussionService.createComment(accountId, id, vo);
            return comment == null ? RestBean.failure(404, "文章不存在") : RestBean.success(comment);
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public RestBean<Void> deleteComment(@PathVariable Integer postId,
                                        @PathVariable Integer commentId,
                                        HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        Boolean deleted = discussionService.deleteComment(accountId, commentId);
        if (deleted == null) {
            return RestBean.failure(404, "评论不存在");
        }
        if (!deleted) {
            return RestBean.forbidden("只能删除自己发表的评论");
        }
        return RestBean.success();
    }

    private Integer accountId(HttpServletRequest request) {
        return (Integer) request.getAttribute("id");
    }
}

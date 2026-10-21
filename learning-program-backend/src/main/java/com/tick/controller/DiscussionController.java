package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.DiscussionCommentCreateVO;
import com.tick.entity.vo.request.DiscussionPostCreateVO;
import com.tick.entity.vo.response.DiscussionCommentVO;
import com.tick.entity.vo.response.DiscussionPostPageVO;
import com.tick.entity.vo.response.DiscussionPostVO;
import com.tick.service.DiscussionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
    public RestBean<DiscussionPostVO> create(@RequestBody DiscussionPostCreateVO vo,
                                             @RequestAttribute("id") Integer accountId) {
        return RestBean.success(discussionService.createPost(accountId, vo));
    }

    @DeleteMapping("/{id}")
    public RestBean<Void> delete(@PathVariable Integer id,
                                 @RequestAttribute("id") Integer accountId) {
        discussionService.deletePost(accountId, id);
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
                                                 @RequestAttribute("id") Integer accountId) {
        DiscussionCommentVO comment = discussionService.createComment(accountId, id, vo);
        return comment == null ? RestBean.failure(404, "文章不存在") : RestBean.success(comment);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public RestBean<Void> deleteComment(@PathVariable Integer postId,
                                        @PathVariable Integer commentId,
                                        @RequestAttribute("id") Integer accountId) {
        discussionService.deleteComment(accountId, commentId);
        return RestBean.success();
    }
}

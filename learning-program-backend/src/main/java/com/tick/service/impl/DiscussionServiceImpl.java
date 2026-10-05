package com.tick.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Account;
import com.tick.entity.dto.DiscussionComment;
import com.tick.entity.dto.DiscussionPost;
import com.tick.entity.dto.Notification;
import com.tick.entity.vo.request.DiscussionCommentCreateVO;
import com.tick.entity.vo.request.DiscussionPostCreateVO;
import com.tick.entity.vo.response.DiscussionCommentVO;
import com.tick.entity.vo.response.DiscussionPostPageVO;
import com.tick.entity.vo.response.DiscussionPostSummaryVO;
import com.tick.entity.vo.response.DiscussionPostVO;
import com.tick.mapper.DiscussionCommentMapper;
import com.tick.mapper.DiscussionPostMapper;
import com.tick.service.AccountService;
import com.tick.service.DiscussionService;
import com.tick.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 讨论区服务实现。文章与评论都以 account_id 记录作者，展示时批量换取用户名，避免逐条查库。
 */
@Service
public class DiscussionServiceImpl extends ServiceImpl<DiscussionPostMapper, DiscussionPost>
        implements DiscussionService {
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_TITLE_LENGTH = 150;
    static final int MAX_POST_CONTENT_LENGTH = 5000;
    static final int MAX_COMMENT_LENGTH = 1000;
    /** 列表摘要长度 */
    static final int SUMMARY_LENGTH = 120;
    /** db_notification.content 为 varchar(1000)，留出余量 */
    static final int NOTIFICATION_CONTENT_LIMIT = 900;
    static final String NOTIFICATION_TITLE = "你的文章有新评论";
    static final String NOTIFICATION_TYPE = "discussion";

    private final DiscussionCommentMapper discussionCommentMapper;
    private final AccountService accountService;
    private final NotificationService notificationService;

    public DiscussionServiceImpl(DiscussionCommentMapper discussionCommentMapper,
                                 AccountService accountService,
                                 NotificationService notificationService) {
        this.discussionCommentMapper = discussionCommentMapper;
        this.accountService = accountService;
        this.notificationService = notificationService;
    }

    @Override
    public DiscussionPostPageVO listPosts(int page, int size) {
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 1);

        long total = query().count();
        long offset = (long) (safePage - 1) * safeSize;
        List<DiscussionPost> posts = offset >= total
                ? List.of()
                : baseMapper.selectPageByCreatedDesc(offset, safeSize);

        Map<Integer, String> names = authorNames(posts.stream().map(DiscussionPost::getAccountId).toList());
        List<DiscussionPostSummaryVO> items = posts.stream()
                .map(post -> new DiscussionPostSummaryVO(
                        post.getId(),
                        post.getTitle(),
                        summarize(post.getContent()),
                        post.getAccountId(),
                        names.getOrDefault(post.getAccountId(), ""),
                        post.getCommentCount() == null ? 0 : post.getCommentCount(),
                        post.getCreatedAt()))
                .toList();
        return new DiscussionPostPageVO(items, total, safePage, safeSize);
    }

    @Override
    public DiscussionPostVO getPost(Integer postId) {
        DiscussionPost post = postId == null ? null : getById(postId);
        return post == null ? null : toPostVO(post);
    }

    @Override
    public DiscussionPostVO createPost(Integer accountId, DiscussionPostCreateVO vo) {
        if (vo == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String title = vo.getTitle() == null ? "" : vo.getTitle().trim();
        String content = vo.getContent() == null ? "" : vo.getContent().trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("标题不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }
        if (content.isEmpty()) {
            throw new IllegalArgumentException("正文不能为空");
        }
        if (content.length() > MAX_POST_CONTENT_LENGTH) {
            throw new IllegalArgumentException("正文不能超过 " + MAX_POST_CONTENT_LENGTH + " 个字符");
        }

        DiscussionPost post = new DiscussionPost(null, accountId, title, content, 0, LocalDateTime.now());
        save(post);
        return toPostVO(post);
    }

    @Override
    @Transactional
    public boolean deletePost(Integer accountId, Integer postId) {
        DiscussionPost post = postId == null ? null : getById(postId);
        if (post == null || !mayDelete(accountId, post.getAccountId())) {
            return false;
        }
        // 评论由 db_discussion_comment 的外键 ON DELETE CASCADE 一并删除
        return removeById(post.getId());
    }

    @Override
    public List<DiscussionCommentVO> listComments(Integer postId) {
        if (postId == null || getById(postId) == null) {
            return null;
        }
        List<DiscussionComment> comments = discussionCommentMapper.selectList(
                Wrappers.<DiscussionComment>query()
                        .eq("post_id", postId)
                        .orderByAsc("created_at")
                        .orderByAsc("id"));
        Map<Integer, String> names = authorNames(comments.stream().map(DiscussionComment::getAccountId).toList());
        return comments.stream()
                .map(comment -> new DiscussionCommentVO(
                        comment.getId(),
                        comment.getPostId(),
                        comment.getAccountId(),
                        names.getOrDefault(comment.getAccountId(), ""),
                        comment.getContent(),
                        comment.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public DiscussionCommentVO createComment(Integer accountId, Integer postId, DiscussionCommentCreateVO vo) {
        DiscussionPost post = postId == null ? null : getById(postId);
        if (post == null) {
            return null;
        }
        if (vo == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String content = vo.getContent() == null ? "" : vo.getContent().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        if (content.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("评论不能超过 " + MAX_COMMENT_LENGTH + " 个字符");
        }

        DiscussionComment comment = new DiscussionComment(null, post.getId(), accountId, content, LocalDateTime.now());
        discussionCommentMapper.insert(comment);
        update().eq("id", post.getId()).setSql("comment_count = comment_count + 1").update();
        notifyPostAuthor(accountId, post, content);

        Map<Integer, String> names = authorNames(List.of(accountId));
        return new DiscussionCommentVO(comment.getId(), comment.getPostId(), comment.getAccountId(),
                names.getOrDefault(accountId, ""), comment.getContent(), comment.getCreatedAt());
    }

    @Override
    @Transactional
    public Boolean deleteComment(Integer accountId, Integer commentId) {
        DiscussionComment comment = commentId == null ? null : discussionCommentMapper.selectById(commentId);
        if (comment == null) {
            return null;
        }
        if (!mayDelete(accountId, comment.getAccountId())) {
            return false;
        }
        discussionCommentMapper.deleteById(comment.getId());
        update().eq("id", comment.getPostId())
                .setSql("comment_count = GREATEST(comment_count - 1, 0)")
                .update();
        return true;
    }

    /** 评论者不是作者本人时，给文章作者发一条站内通知。 */
    private void notifyPostAuthor(Integer commenterId, DiscussionPost post, String commentContent) {
        if (commenterId == null || commenterId.equals(post.getAccountId())) {
            return;
        }
        String commenterName = authorNames(List.of(commenterId)).getOrDefault(commenterId, "某位同学");
        Notification notification = new Notification(
                null,
                post.getAccountId(),
                NOTIFICATION_TITLE,
                commentNotification(commenterName, post.getTitle(), commentContent),
                "/discussions/" + post.getId(),
                NOTIFICATION_TYPE,
                false,
                new Date());
        notificationService.save(notification);
    }

    /** 通知正文，整体截断到 db_notification.content 的长度上限以内。 */
    static String commentNotification(String commenterName, String postTitle, String comment) {
        String text = commenterName + " 评论了你的文章《" + postTitle + "》：" + comment;
        if (text.length() <= NOTIFICATION_CONTENT_LIMIT) {
            return text;
        }
        return text.substring(0, NOTIFICATION_CONTENT_LIMIT - 1) + "…";
    }

    /** 列表摘要：把换行折成空格并截断。 */
    static String summarize(String content) {
        if (content == null) {
            return "";
        }
        String flattened = content.replaceAll("\\s+", " ").trim();
        return flattened.length() <= SUMMARY_LENGTH ? flattened : flattened.substring(0, SUMMARY_LENGTH) + "…";
    }

    private DiscussionPostVO toPostVO(DiscussionPost post) {
        Map<Integer, String> names = authorNames(List.of(post.getAccountId()));
        return new DiscussionPostVO(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAccountId(),
                names.getOrDefault(post.getAccountId(), ""),
                post.getCommentCount() == null ? 0 : post.getCommentCount(),
                post.getCreatedAt());
    }

    /** 作者本人或管理员可以删除。 */
    private boolean mayDelete(Integer accountId, Integer ownerId) {
        if (accountId == null) {
            return false;
        }
        if (accountId.equals(ownerId)) {
            return true;
        }
        Account account = accountService.getById(accountId);
        return account != null && "admin".equals(account.getRole());
    }

    private Map<Integer, String> authorNames(Collection<Integer> accountIds) {
        Set<Integer> ids = accountIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> names = new HashMap<>();
        for (Account account : accountService.listByIds(ids)) {
            names.put(account.getId(), account.getUsername());
        }
        return names;
    }
}

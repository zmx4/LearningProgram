package com.tick.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tick.entity.dto.DiscussionPost;
import com.tick.entity.vo.request.DiscussionCommentCreateVO;
import com.tick.entity.vo.request.DiscussionPostCreateVO;
import com.tick.entity.vo.response.DiscussionCommentVO;
import com.tick.entity.vo.response.DiscussionPostPageVO;
import com.tick.entity.vo.response.DiscussionPostVO;

import java.util.List;

/**
 * 讨论区：文章与文章下的评论。发表评论时会顺带通知文章作者。
 */
public interface DiscussionService extends IService<DiscussionPost> {
    /**
     * 分页查询文章，按发表时间倒序。page 从 1 开始。
     */
    DiscussionPostPageVO listPosts(int page, int size);

    /**
     * 文章详情，不存在返回 null。
     */
    DiscussionPostVO getPost(Integer postId);

    /**
     * 发表文章。
     */
    DiscussionPostVO createPost(Integer accountId, DiscussionPostCreateVO vo);

    /**
     * 删除文章（连同其评论）。仅作者本人或管理员可以删除，否则返回 false。
     */
    boolean deletePost(Integer accountId, Integer postId);

    /**
     * 某篇文章的评论，按发表时间正序。文章不存在返回 null。
     */
    List<DiscussionCommentVO> listComments(Integer postId);

    /**
     * 发表评论，并给文章作者发一条站内通知（自己评论自己的文章不通知）。
     */
    DiscussionCommentVO createComment(Integer accountId, Integer postId, DiscussionCommentCreateVO vo);

    /**
     * 删除评论。
     *
     * @return {@code null} 表示评论不存在，{@code false} 表示无权删除，{@code true} 表示删除成功
     */
    Boolean deleteComment(Integer accountId, Integer commentId);
}

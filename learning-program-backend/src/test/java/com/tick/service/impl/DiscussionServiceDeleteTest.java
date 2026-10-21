package com.tick.service.impl;

import com.tick.entity.dto.Account;
import com.tick.entity.dto.DiscussionPost;
import com.tick.exception.NotFoundException;
import com.tick.mapper.DiscussionCommentMapper;
import com.tick.service.AccountService;
import com.tick.service.NotificationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 删除文章用异常表达「不存在 / 无权限」，控制器不再需要先查一次再删。
 */
class DiscussionServiceDeleteTest {
    private final AccountService accountService = mock(AccountService.class);

    private DiscussionServiceImpl service() {
        return spy(new DiscussionServiceImpl(
                mock(DiscussionCommentMapper.class),
                accountService,
                mock(NotificationService.class)));
    }

    private DiscussionPost post(int id, int authorId) {
        DiscussionPost post = new DiscussionPost();
        post.setId(id);
        post.setAccountId(authorId);
        return post;
    }

    @Test
    void missingPostBecomesNotFound() {
        DiscussionServiceImpl service = service();
        doReturn(null).when(service).getById(7);

        assertThrows(NotFoundException.class, () -> service.deletePost(1, 7));
    }

    @Test
    void nonAuthorWithoutAdminRoleGetsForbidden() {
        DiscussionServiceImpl service = service();
        doReturn(post(7, 2)).when(service).getById(7);
        when(accountService.getById(1))
                .thenReturn(new Account(1, "u", "p", "e", null, null, "user", null));

        assertThrows(SecurityException.class, () -> service.deletePost(1, 7));
    }

    @Test
    void authorCanDeleteOwnPost() {
        DiscussionServiceImpl service = service();
        doReturn(post(7, 1)).when(service).getById(7);
        doReturn(true).when(service).removeById(7);

        service.deletePost(1, 7);

        verify(service).removeById(7);
    }
}

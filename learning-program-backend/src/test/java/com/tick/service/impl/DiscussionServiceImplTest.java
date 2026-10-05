package com.tick.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscussionServiceImplTest {

    @Test
    void notificationMentionsCommenterPostAndComment() {
        String text = DiscussionServiceImpl.commentNotification("tick", "如何学习 Java", "建议先看官方教程");

        assertEquals("tick 评论了你的文章《如何学习 Java》：建议先看官方教程", text);
    }

    @Test
    void notificationIsTruncatedToColumnLimit() {
        String longComment = "评".repeat(DiscussionServiceImpl.MAX_COMMENT_LENGTH);

        String text = DiscussionServiceImpl.commentNotification("tick", "标题", longComment);

        assertTrue(text.length() <= DiscussionServiceImpl.NOTIFICATION_CONTENT_LIMIT,
                "通知正文必须能放进 varchar(1000)，实际长度 " + text.length());
        assertTrue(text.endsWith("…"), "截断后应以省略号结尾");
    }

    @Test
    void shortNotificationIsNotTruncated() {
        String text = DiscussionServiceImpl.commentNotification("a", "b", "c");

        assertEquals("a 评论了你的文章《b》：c", text);
        assertTrue(!text.endsWith("…"));
    }

    @Test
    void summarizeFlattensWhitespace() {
        assertEquals("第一行 第二行", DiscussionServiceImpl.summarize("第一行\n\n  第二行  "));
    }

    @Test
    void summarizeTruncatesLongContent() {
        String summary = DiscussionServiceImpl.summarize("字".repeat(400));

        assertTrue(summary.length() <= DiscussionServiceImpl.SUMMARY_LENGTH + 1);
        assertTrue(summary.endsWith("…"));
    }

    @Test
    void summarizeHandlesNull() {
        assertEquals("", DiscussionServiceImpl.summarize(null));
    }
}

package com.tick.service.impl.group;

import com.tick.entity.dto.GroupTaskItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupTaskItemJudgeTest {
    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 11, 1, 9, 0);

    private GroupTaskItem item(String type, Integer refId) {
        GroupTaskItem item = new GroupTaskItem();
        item.setId(1);
        item.setItemType(type);
        item.setRefId(refId);
        return item;
    }

    private GroupTaskCompletionContext context(Map<String, LocalDateTime> manual,
                                               Map<Integer, Long> chapters,
                                               Map<String, GroupTaskCompletionContext.CourseStudy> studies,
                                               Map<String, LocalDateTime> accesses,
                                               Map<Integer, LocalDateTime> visits) {
        return new GroupTaskCompletionContext(Map.of(), manual, chapters, studies, accesses, visits,
                Map.of(), Map.of(), Map.of());
    }

    @Test
    void textItemNeedsManualRecord() {
        TextGroupTaskItemJudge judge = new TextGroupTaskItemJudge();

        assertFalse(judge.judge(context(Map.of(), Map.of(), Map.of(), Map.of(), Map.of()),
                item(GroupTaskItem.TYPE_TEXT, null), 2, PUBLISHED_AT).completed());
        assertTrue(judge.judge(context(Map.of("1_2", PUBLISHED_AT), Map.of(), Map.of(), Map.of(), Map.of()),
                item(GroupTaskItem.TYPE_TEXT, null), 2, PUBLISHED_AT).completed());
    }

    @Test
    void courseItemNeedsEveryChapter() {
        CourseGroupTaskItemJudge judge = new CourseGroupTaskItemJudge();
        GroupTaskItem course = item(GroupTaskItem.TYPE_COURSE, 7);

        assertFalse(judge.judge(context(Map.of(), Map.of(7, 3L), Map.of(), Map.of(), Map.of()),
                course, 2, PUBLISHED_AT).completed());
        assertFalse(judge.judge(context(Map.of(), Map.of(7, 3L),
                        Map.of("2_7", new GroupTaskCompletionContext.CourseStudy(2, PUBLISHED_AT)),
                        Map.of(), Map.of()),
                course, 2, PUBLISHED_AT).completed());
        assertFalse(judge.judge(context(Map.of(), Map.of(), Map.of(), Map.of(), Map.of()),
                course, 2, PUBLISHED_AT).completed());
        assertTrue(judge.judge(context(Map.of(), Map.of(7, 3L),
                        Map.of("2_7", new GroupTaskCompletionContext.CourseStudy(3, PUBLISHED_AT)),
                        Map.of(), Map.of()),
                course, 2, PUBLISHED_AT).completed());
    }

    @Test
    void resourceItemOnlyCountsAccessAfterPublish() {
        ResourceGroupTaskItemJudge judge = new ResourceGroupTaskItemJudge();
        GroupTaskItem resource = item(GroupTaskItem.TYPE_RESOURCE, 9);

        assertFalse(judge.judge(context(Map.of(), Map.of(), Map.of(),
                        Map.of("2_9", PUBLISHED_AT.minusDays(1)), Map.of()),
                resource, 2, PUBLISHED_AT).completed());
        assertTrue(judge.judge(context(Map.of(), Map.of(), Map.of(),
                        Map.of("2_9", PUBLISHED_AT), Map.of()),
                resource, 2, PUBLISHED_AT).completed());
    }

    @Test
    void memberVisitOnlyCountsVisitAfterPublish() {
        MemberVisitGroupTaskItemJudge judge = new MemberVisitGroupTaskItemJudge();
        GroupTaskItem visit = item(GroupTaskItem.TYPE_MEMBER_VISIT, null);

        assertFalse(judge.judge(context(Map.of(), Map.of(), Map.of(), Map.of(),
                        Map.of(2, PUBLISHED_AT.minusMinutes(1))),
                visit, 2, PUBLISHED_AT).completed());
        assertTrue(judge.judge(context(Map.of(), Map.of(), Map.of(), Map.of(),
                        Map.of(2, PUBLISHED_AT.plusMinutes(1))),
                visit, 2, PUBLISHED_AT).completed());
    }

    @Test
    void wordTestOnlyCountsPassingScoreAfterPublish() {
        WordTestGroupTaskItemJudge judge = new WordTestGroupTaskItemJudge();
        GroupTaskItem word = item(GroupTaskItem.TYPE_WORD_TEST, null);
        word.setSource("cet4");
        word.setPassScore(80);
        GroupTaskCompletionContext context = new GroupTaskCompletionContext(Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of("2_cet4", List.of(
                        new GroupTaskCompletionContext.TestScore(90, PUBLISHED_AT.minusMinutes(1)),
                        new GroupTaskCompletionContext.TestScore(60, PUBLISHED_AT.plusMinutes(1)),
                        new GroupTaskCompletionContext.TestScore(85, PUBLISHED_AT.plusMinutes(2)))), Map.of());

        GroupTaskItemState state = judge.judge(context, word, 2, PUBLISHED_AT);

        assertTrue(state.completed());
        assertTrue(state.completedAt().isEqual(PUBLISHED_AT.plusMinutes(2)));
    }

    @Test
    void questionSetTestNeedsPassingScoreForSelectedSet() {
        QuestionSetGroupTaskItemJudge judge = new QuestionSetGroupTaskItemJudge();
        GroupTaskItem questionSet = item(GroupTaskItem.TYPE_QUESTION_SET, 9);
        questionSet.setPassScore(60);
        GroupTaskCompletionContext context = new GroupTaskCompletionContext(Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of("2_9", List.of(new GroupTaskCompletionContext.TestScore(70, PUBLISHED_AT))));

        assertTrue(judge.judge(context, questionSet, 2, PUBLISHED_AT).completed());
        assertFalse(judge.judge(context, item(GroupTaskItem.TYPE_QUESTION_SET, 10), 2, PUBLISHED_AT).completed());
    }
}
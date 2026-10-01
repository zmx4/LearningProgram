package com.tick.entity.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestQuestionContentTest {

    private static TestQuestionContent choice(String stem, List<String> keys, String... answer) {
        TestQuestionContent content = new TestQuestionContent();
        content.setStem(stem);
        List<QuestionOption> options = new ArrayList<>();
        for (String key : keys) {
            options.add(new QuestionOption(key, "选项 " + key));
        }
        content.setOptions(options);
        content.setAnswer(Arrays.asList(answer));
        return content;
    }

    private static TestQuestionContent blank(String stem, String... answer) {
        TestQuestionContent content = new TestQuestionContent();
        content.setStem(stem);
        content.setAnswer(Arrays.asList(answer));
        return content;
    }

    @Test
    void singleChoiceAcceptsExactlyOneAnswer() {
        TestQuestionContent content = choice("题干", List.of("A", "B", "C"), "B");

        content.normalizeAndValidate(TestQuestionKind.SINGLE);

        assertEquals(List.of("B"), content.getAnswer());
        assertEquals(3, content.getOptions().size());
    }

    @Test
    void singleChoiceRejectsTwoAnswers() {
        TestQuestionContent content = choice("题干", List.of("A", "B", "C"), "A", "B");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> content.normalizeAndValidate(TestQuestionKind.SINGLE));

        assertTrue(error.getMessage().contains("单选题只能有一个答案"));
    }

    @Test
    void multipleChoiceRequiresAtLeastTwoAnswers() {
        TestQuestionContent content = choice("题干", List.of("A", "B", "C"), "A");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> content.normalizeAndValidate(TestQuestionKind.MULTIPLE));

        assertTrue(error.getMessage().contains("多选题至少需要两个答案"));
    }

    @Test
    void multipleChoiceAcceptsSeveralAnswers() {
        TestQuestionContent content = choice("题干", List.of("A", "B", "C", "D"), "A", "C", "D");

        content.normalizeAndValidate(TestQuestionKind.MULTIPLE);

        assertEquals(List.of("A", "C", "D"), content.getAnswer());
    }

    @Test
    void choiceRejectsAnswerOutsideOptions() {
        TestQuestionContent content = choice("题干", List.of("A", "B"), "C");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> content.normalizeAndValidate(TestQuestionKind.SINGLE));

        assertTrue(error.getMessage().contains("答案必须是选项标识之一"));
    }

    @Test
    void choiceRejectsDuplicateAnswers() {
        TestQuestionContent content = choice("题干", List.of("A", "B", "C"), "A", "A");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> content.normalizeAndValidate(TestQuestionKind.MULTIPLE));

        assertTrue(error.getMessage().contains("答案不能重复"));
    }

    @Test
    void choiceRejectsFewerThanTwoOptions() {
        TestQuestionContent content = choice("题干", List.of("A"), "A");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> content.normalizeAndValidate(TestQuestionKind.SINGLE));

        assertTrue(error.getMessage().contains("至少需要 2 个选项"));
    }

    @Test
    void choiceRejectsDuplicateOptionKeys() {
        TestQuestionContent content = choice("题干", List.of("A", "A"), "A");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> content.normalizeAndValidate(TestQuestionKind.SINGLE));

        assertTrue(error.getMessage().contains("选项标识不能重复"));
    }

    @Test
    void blankAcceptsOneAnswerPerBlank() {
        TestQuestionContent content = blank("____ 和 ____", "final", "StringBuilder");

        content.normalizeAndValidate(TestQuestionKind.BLANK);

        assertEquals(List.of("final", "StringBuilder"), content.getAnswer());
        assertTrue(content.getOptions().isEmpty());
    }

    @Test
    void blankRejectsOptions() {
        TestQuestionContent content = choice("____", List.of("A", "B"), "A");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> content.normalizeAndValidate(TestQuestionKind.BLANK));

        assertTrue(error.getMessage().contains("填空题不能包含选项"));
    }

    @Test
    void blankRejectsEmptyAnswer() {
        TestQuestionContent content = blank("____");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> content.normalizeAndValidate(TestQuestionKind.BLANK));

        assertTrue(error.getMessage().contains("填空题答案不能为空"));
    }

    @Test
    void blankRejectsBlankItemInsideAnswer() {
        TestQuestionContent content = blank("____ 和 ____", "final", "   ");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> content.normalizeAndValidate(TestQuestionKind.BLANK));

        assertTrue(error.getMessage().contains("填空题答案不能为空"));
    }

    @Test
    void emptyStemIsRejected() {
        TestQuestionContent content = choice("   ", List.of("A", "B"), "A");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> content.normalizeAndValidate(TestQuestionKind.SINGLE));

        assertTrue(error.getMessage().contains("题干不能为空"));
    }

    @Test
    void valuesAreTrimmedAndBlankAnalysisBecomesNull() {
        TestQuestionContent content = choice("  题干  ", List.of("A", "B"), " B ");
        content.setAnalysis("   ");

        content.normalizeAndValidate(TestQuestionKind.SINGLE);

        assertEquals("题干", content.getStem());
        assertEquals(List.of("B"), content.getAnswer());
        assertEquals("选项 A", content.getOptions().getFirst().getText());
        assertNull(content.getAnalysis());
    }

    @Test
    void fromCodeAcceptsKnownKindsAndRejectsOthers() {
        assertEquals(TestQuestionKind.SINGLE, TestQuestionKind.fromCode("single"));
        assertEquals(TestQuestionKind.MULTIPLE, TestQuestionKind.fromCode(" MULTIPLE "));
        assertEquals(TestQuestionKind.BLANK, TestQuestionKind.fromCode("blank"));

        assertThrows(IllegalArgumentException.class, () -> TestQuestionKind.fromCode("judge"));
        assertThrows(IllegalArgumentException.class, () -> TestQuestionKind.fromCode(null));
    }
}

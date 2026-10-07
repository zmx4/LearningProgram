package com.tick.service.impl;

import com.tick.entity.vo.response.LeaderboardEntryVO;
import com.tick.entity.vo.response.LeaderboardVO;
import com.tick.mapper.LeaderboardMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LeaderboardServiceImplTest {

    private static final int ME = 42;

    private final LeaderboardMapper mapper = mock(LeaderboardMapper.class);
    private final LeaderboardServiceImpl service = new LeaderboardServiceImpl(mapper);

    private static LeaderboardEntryVO row(int rank, int accountId, String username, long value, int rankedUsers) {
        return new LeaderboardEntryVO(rank, accountId, username, value, rankedUsers);
    }

    @Test
    void listsAvailableMetricsForTheFrontend() {
        List<Map<String, String>> metrics = service.listMetrics();

        assertEquals(3, metrics.size());
        assertEquals("points", metrics.get(0).get("code"));
        assertEquals("积分", metrics.get(0).get("label"));
        assertEquals("study", metrics.get(1).get("code"));
        assertEquals("check-in", metrics.get(2).get("code"));
        assertTrue(metrics.stream().allMatch(metric -> metric.containsKey("unit")));
    }

    @Test
    void splitsTopEntriesFromMyOwnRank() {
        // 接口一次返回前 2 名 + 当前用户（第 5 名）
        when(mapper.selectPointsRanking(ME, 2)).thenReturn(List.of(
                row(1, 7, "alice", 120, 9),
                row(2, 8, "bob", 90, 9),
                row(5, ME, "me", 10, 9)));

        LeaderboardVO board = service.getLeaderboard(ME, "points", 2);

        assertEquals("points", board.getMetric());
        assertEquals("积分", board.getMetricLabel());
        assertEquals(2, board.getEntries().size());
        assertEquals(List.of(1, 2), board.getEntries().stream().map(LeaderboardEntryVO::getRank).toList());
        assertEquals(9, board.getRankedUsers());
        assertEquals(5, board.getMe().getRank());
        assertEquals("me", board.getMe().getUsername());
    }

    @Test
    void meIsNullWhenTheUserHasNoRecordInThisMetric() {
        when(mapper.selectStudyRanking(ME, 20)).thenReturn(List.of(row(1, 7, "alice", 3600, 1)));

        LeaderboardVO board = service.getLeaderboard(ME, "study", 20);

        assertNull(board.getMe());
        assertEquals(1, board.getEntries().size());
        assertEquals("秒", board.getUnit());
    }

    @Test
    void emptyLeaderboardIsReturnedAsEmptyListNotAnError() {
        when(mapper.selectCheckInRanking(ME, 20)).thenReturn(List.of());

        LeaderboardVO board = service.getLeaderboard(ME, "check-in", 20);

        assertTrue(board.getEntries().isEmpty());
        assertNull(board.getMe());
        assertEquals(0, board.getRankedUsers());
    }

    @Test
    void limitDefaultsToTwentyAndIsCappedAtOneHundred() {
        when(mapper.selectPointsRanking(ME, 20)).thenReturn(List.of());
        when(mapper.selectPointsRanking(ME, 100)).thenReturn(List.of());
        when(mapper.selectPointsRanking(ME, 1)).thenReturn(List.of());

        service.getLeaderboard(ME, "points", null);
        verify(mapper).selectPointsRanking(ME, 20);

        service.getLeaderboard(ME, "points", 5000);
        verify(mapper).selectPointsRanking(ME, 100);

        service.getLeaderboard(ME, "points", 0);
        verify(mapper, org.mockito.Mockito.times(2)).selectPointsRanking(ME, 20);

        service.getLeaderboard(ME, "points", 1);
        verify(mapper).selectPointsRanking(ME, 1);
    }

    @Test
    void metricCodeIsCaseInsensitiveAndTrimmed() {
        when(mapper.selectStudyRanking(ME, 20)).thenReturn(List.of());

        service.getLeaderboard(ME, "  STUDY ", null);

        verify(mapper).selectStudyRanking(ME, 20);
    }

    @Test
    void unknownMetricIsRejectedWithAReadableMessage() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.getLeaderboard(ME, "height", 20));

        assertTrue(error.getMessage().contains("points"), error.getMessage());
        verifyNoInteractions(mapper);
    }

    @Test
    void nullMetricIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.getLeaderboard(ME, null, 20));
    }
}

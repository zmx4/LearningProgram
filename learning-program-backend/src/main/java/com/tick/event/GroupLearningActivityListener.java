package com.tick.event;

import com.tick.service.impl.GroupTaskSettlementService;
import com.tick.service.impl.StudyGroupMemberQuery;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 小组学习事件监听：只在原写事务提交后触发结算，查询接口不会产生奖励副作用。
 */
@Component
public class GroupLearningActivityListener {
    private final GroupTaskSettlementService settlementService;
    private final StudyGroupMemberQuery memberQuery;

    public GroupLearningActivityListener(GroupTaskSettlementService settlementService,
                                         StudyGroupMemberQuery memberQuery) {
        this.settlementService = settlementService;
        this.memberQuery = memberQuery;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLearningActivity(GroupLearningActivityEvent event) {
        settlementService.settleForAccount(event.accountId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskPublished(GroupTaskPublishedEvent event) {
        memberQuery.membersOf(event.groupId()).forEach(member ->
                settlementService.settleForAccount(member.getAccountId()));
    }
}

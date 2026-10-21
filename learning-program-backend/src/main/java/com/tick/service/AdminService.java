package com.tick.service;

import com.tick.entity.vo.request.AdminBatchCreateVO;
import com.tick.entity.vo.request.AdminNotificationVO;
import com.tick.entity.vo.response.BatchCreateResultVO;

/**
 * 管理员批量操作：批量建号与定向通知发送。
 */
public interface AdminService {
    /**
     * 批量创建账号，允许部分成功，逐条返回失败原因。
     */
    BatchCreateResultVO createAccounts(AdminBatchCreateVO vo);

    /**
     * 按范围给用户发送站内通知，返回实际发送数量。
     */
    int sendNotification(AdminNotificationVO vo);
}

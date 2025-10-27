package groupbuy.market.plus.trigger.job.notify;

import groupbuy.market.plus.trigger.job.AbstractNotifyJob;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TeamSuccessNotifyJob extends AbstractNotifyJob {

    /**
     * 定时任务 - 执行拼团完成回调通知
     */
    @Scheduled(cron = "0/10 * * * * ?")
    public void exec() {
        execNotifyJob(GroupBuyConstants.TeamSuccessNotifyJobLock,
                "拼团完成回调通知",
                () -> taskService.execTeamSuccessNotifyJob());
    }

}

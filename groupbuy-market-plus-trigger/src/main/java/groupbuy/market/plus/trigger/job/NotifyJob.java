package groupbuy.market.plus.trigger.job;

import groupbuy.market.plus.domain.trade.service.settle.SettleOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Service
public class NotifyJob {

    @Resource
    private SettleOrderService settleOrderService;

    /**
     * 定时任务 - 执行回调通知拼团完成
     */
    @Scheduled(cron = "0/10 * * * * ?")
    public void exec() {
        try {
            log.info("定时任务 - 执行回调通知开始");
            Map<String, Integer> result = settleOrderService.execNotifyJob();
            log.info("定时任务 - 执行回调通知拼团完成 result:{}", result);
        } catch (Exception e) {
            log.error("定时任务 - 执行回调通知拼团完成 失败", e);
        }
    }

}

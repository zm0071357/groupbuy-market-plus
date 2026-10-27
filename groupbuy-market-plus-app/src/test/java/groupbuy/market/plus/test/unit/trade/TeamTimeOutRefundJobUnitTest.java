package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.trigger.job.TeamTimeOutRefundJob;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 拼团超时退单定时任务单元测试 - 校验执行频率为每15分钟一次
 */
public class TeamTimeOutRefundJobUnitTest {

    @Test
    public void test_cron_every15Minutes() throws Exception {
        String cron = TeamTimeOutRefundJob.class.getMethod("exec").getAnnotation(Scheduled.class).cron();
        CronExpression cronExpression = CronExpression.parse(cron);

        LocalDateTime time = LocalDateTime.of(2026, 10, 27, 10, 0, 0);
        LocalDateTime next1 = cronExpression.next(time);
        LocalDateTime next2 = cronExpression.next(next1);
        LocalDateTime next3 = cronExpression.next(next2);

        Assert.assertEquals(LocalDateTime.of(2026, 10, 27, 10, 15, 0), next1);
        Assert.assertEquals(Duration.ofMinutes(15), Duration.between(next1, next2));
        Assert.assertEquals(Duration.ofMinutes(15), Duration.between(next2, next3));
    }

}

package groupbuy.market.plus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 回调线程池配置 - 成团、退单、新团长回调等慢 IO 任务专用，与试算、退单查询线程池隔离
 */
@Data
@ConfigurationProperties(prefix = "thread.pool.notify.config", ignoreInvalidFields = true)
public class NotifyThreadPoolConfigProperties {

    /** 核心线程数 */
    private Integer corePoolSize = 10;
    /** 最大线程数 */
    private Integer maxPoolSize = 20;
    /** 最大等待时间（秒） */
    private Long keepAliveTime = 10L;
    /** 最大队列数 */
    private Integer blockQueueSize = 1000;

}

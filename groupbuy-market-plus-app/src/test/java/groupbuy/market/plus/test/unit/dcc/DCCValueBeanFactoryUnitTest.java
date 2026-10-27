package groupbuy.market.plus.test.unit.dcc;

import groupbuy.market.plus.config.DCCValueBeanFactory;
import groupbuy.market.plus.infrastructure.dcc.DCCServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 动态配置中心单元测试 - 配置值包含逗号时不被截断
 */
@RunWith(MockitoJUnitRunner.class)
public class DCCValueBeanFactoryUnitTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RTopic topic;

    @Mock
    private RBucket<Object> bucket;

    private DCCServiceImpl dccService;

    private MessageListener<String> listener;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        when(redissonClient.getBucket(anyString())).thenReturn(bucket);
        when(bucket.isExists()).thenReturn(false);
        when(redissonClient.getTopic(anyString())).thenReturn(topic);

        DCCValueBeanFactory factory = new DCCValueBeanFactory(redissonClient);
        // 注入默认配置：downgradeSwitch=0、cutRange=100、scBlack=s02,c02
        dccService = new DCCServiceImpl();
        factory.postProcessAfterInitialization(dccService, "dccServiceImpl");
        // 注册订阅者并拿到监听器
        factory.dccTopicSubscriber(redissonClient);
        ArgumentCaptor<MessageListener> captor = ArgumentCaptor.forClass(MessageListener.class);
        verify(topic).addListener(eq(String.class), captor.capture());
        listener = captor.getValue();
    }

    @Test
    public void test_valueWithComma_notTruncated() {
        Assert.assertTrue(dccService.isBlack("s02", "c99"));
        Assert.assertTrue(dccService.isBlack("s99", "c02"));

        // 推送新黑名单 s03,c03（DCCController 发布格式为 key,value）
        listener.onMessage("groupbuy_market_plus_dcc", "scBlack,s03,c03");

        Assert.assertTrue("渠道 c03 应在新黑名单中", dccService.isBlack("s99", "c03"));
        Assert.assertTrue("来源 s03 应在新黑名单中", dccService.isBlack("s03", "c99"));
        Assert.assertFalse("旧黑名单应被替换", dccService.isBlack("s02", "c02"));
    }

    @Test
    public void test_simpleValue_updated() {
        Assert.assertFalse(dccService.isDowngradeSwitch());

        listener.onMessage("groupbuy_market_plus_dcc", "downgradeSwitch,1");

        Assert.assertTrue(dccService.isDowngradeSwitch());
    }

    @Test
    public void test_illegalMessage_ignored() {
        listener.onMessage("groupbuy_market_plus_dcc", "downgradeSwitch");

        Assert.assertFalse(dccService.isDowngradeSwitch());
    }

}

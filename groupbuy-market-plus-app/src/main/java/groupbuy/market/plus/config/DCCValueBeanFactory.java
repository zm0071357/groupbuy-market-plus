package groupbuy.market.plus.config;

import groupbuy.market.plus.types.annotations.DCCValue;
import groupbuy.market.plus.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态配置中心
 */
@Slf4j
@Configuration
public class DCCValueBeanFactory implements BeanPostProcessor {
    private static final String TOPIC = "groupbuy_market_plus_dcc";
    private static final String PATH = TOPIC + Constants.UNDERSCORE;
    private final RedissonClient redissonClient;
    private final Map<String, Object> dccObjectMap = new HashMap<>();

    public DCCValueBeanFactory(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 发布者
     */
    @Bean("dccTopicPublisher")
    public RTopic dccTopicPublisher(RedissonClient redissonClient) {
        return redissonClient.getTopic(TOPIC);
    }

    /**
     * 订阅者
     */
    @Bean("dccTopicSubscriber")
    public RTopic dccTopicSubscriber(RedissonClient redissonClient) {
        RTopic topic = redissonClient.getTopic(TOPIC);
        topic.addListener(String.class, (charSequence, s) -> {
            try {
                String[] split = s.split(Constants.SPLIT);
                String key = split[0];      // 配置键名，如 "downgradeSwitch"
                String value = split[1];    // 配置值，如 "1"

                // 更新 Redis
                String redisKey = PATH + key;
                RBucket<Object> bucket = redissonClient.getBucket(redisKey);
                if (bucket.isExists()) {
                    bucket.set(value);
                }

                // 更新内存字段
                Object objBean = dccObjectMap.get(key);  // 用配置键名查找
                if (objBean != null) {
                    Class<?> objBeanClass = AopUtils.isAopProxy(objBean) ?
                            AopUtils.getTargetClass(objBean) : objBean.getClass();
                    Field field = objBeanClass.getDeclaredField(key);
                    field.setAccessible(true);
                    field.set(objBean, value);
                    field.setAccessible(false);
                    log.info("DCC 节点监听，动态设置值 {} {}", key, value);
                }
            } catch (Exception e) {
                log.error("DCC 配置更新失败", e);
            }
        });
        return topic;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetBeanClass = bean.getClass();
        Object targetBeanObject = bean;

        if (AopUtils.isAopProxy(bean)) {
            targetBeanClass = AopUtils.getTargetClass(bean);
            targetBeanObject = AopProxyUtils.getSingletonTarget(bean);
        }

        Field[] fields = targetBeanClass.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(DCCValue.class)) continue;

            DCCValue dccValue = field.getAnnotation(DCCValue.class);
            String annotationValue = dccValue.value();
            log.info("读取到@DCCValue默认配置：{}", annotationValue);
            if (StringUtils.isBlank(annotationValue)) {
                throw new RuntimeException(field.getName() + " @DCCValue 配置值不能为空");
            }

            // 使用冒号分割（与注解格式一致）
            String[] split = annotationValue.split(Constants.ENUMERATION_COMMA);
            String configKey = split[0];  // 配置键名，如 "downgradeSwitch"
            String defaultValue = split.length == 2 ? split[1] : null;
            String redisKey = PATH + configKey;

            if (StringUtils.isBlank(defaultValue)) {
                throw new RuntimeException("DCC 配置错误：" + configKey + " 必须配置默认值");
            }

            try {
                RBucket<String> bucket = redissonClient.getBucket(redisKey);
                String actualValue = defaultValue;
                if (bucket.isExists()) {
                    actualValue = bucket.get();
                    log.info("读取到Redis配置：{}", configKey.concat(Constants.ENUMERATION_COMMA).concat(actualValue));
                } else {
                    bucket.set(defaultValue);
                    log.info("默认配置写入Redis：{}", configKey.concat(Constants.ENUMERATION_COMMA).concat(defaultValue));
                }
                field.setAccessible(true);
                field.set(targetBeanObject, actualValue);
                field.setAccessible(false);
                // 存储配置键名到 Bean 的映射
                dccObjectMap.put(configKey, targetBeanObject);
            } catch (Exception e) {
                throw new RuntimeException("DCC 配置初始化失败: " + configKey, e);
            }
        }
        return bean;
    }
}

package groupbuy.market.plus.types.design.framework.tree;

import lombok.Getter;
import lombok.Setter;

/**
 * 策略路由抽象类
 */
public abstract class AbstractStrategyRouter<T, D, R> implements StrategyMapper<T, D, R>, StrategyHandler<T, D, R> {

    /**
     * 默认值
     */
    @Getter
    @Setter
    protected StrategyHandler<T, D, R> defaultStrategyHandler = StrategyHandler.DEFAULT;

    /**
     * 获取并处理
     * @param requestParameter 入参
     * @param dynamicContext 动态上下文
     * @return
     * @throws Exception
     */
    public R router(T requestParameter, D dynamicContext) throws Exception {
        // 获取下一个节点的策略处理器
        StrategyHandler<T, D, R> strategyHandler = get(requestParameter, dynamicContext);
        // 处理器不为空 - 执行业务处理
        if (strategyHandler != null) {
            return strategyHandler.apply(requestParameter, dynamicContext);
        }
        // 处理器为空 - 默认处理器进行处理，返回 null
        return defaultStrategyHandler.apply(requestParameter, dynamicContext);
    }

}

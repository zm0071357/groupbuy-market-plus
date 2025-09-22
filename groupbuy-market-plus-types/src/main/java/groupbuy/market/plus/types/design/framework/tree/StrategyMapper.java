package groupbuy.market.plus.types.design.framework.tree;

/**
 * 策略映射器
 */
public interface StrategyMapper<T, D, R> {

    /**
     * 获取节点
     * @param requestParameter 入参
     * @param dynamicContext 动态上下文
     * @return
     */
    StrategyHandler<T, D, R> get(T requestParameter, D dynamicContext);
}

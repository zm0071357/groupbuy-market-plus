package groupbuy.market.plus.types.design.framework.tree.multithread;

import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import groupbuy.market.plus.types.design.framework.tree.StrategyMapper;
import lombok.Getter;
import lombok.Setter;

/**
 * 策略路由抽象类 - 多线程数据加载
 */
public abstract class AbstractMultiThreadStrategyRouter<T, D, R> implements StrategyMapper<T, D, R>, StrategyHandler<T, D, R> {

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

    /**
     * 异步加载数据后处理节点
     * @param requestParameter 入参
     * @param dynamicContext 动态上下文
     * @return
     * @throws Exception
     */
    @Override
    public R apply(T requestParameter, D dynamicContext) throws Exception {
        multiThread(requestParameter, dynamicContext);
        return doApply(requestParameter, dynamicContext);
    }

    /**
     * 异步加载数据
     * @param requestParameter
     * @param dynamicContext
     * @throws Exception
     */
    protected abstract void multiThread(T requestParameter, D dynamicContext) throws Exception;

    /**
     * 处理节点
     * @param requestParameter
     * @param dynamicContext
     * @return
     * @throws Exception
     */
    protected abstract R doApply(T requestParameter, D dynamicContext) throws Exception;
}

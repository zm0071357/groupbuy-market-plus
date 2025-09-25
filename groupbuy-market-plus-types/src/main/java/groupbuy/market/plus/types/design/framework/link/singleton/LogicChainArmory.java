package groupbuy.market.plus.types.design.framework.link.singleton;

/**
 * 定义责任链的装配方法，解耦链的构建逻辑
 * @param <T>
 * @param <D>
 * @param <R>
 */
public interface LogicChainArmory<T, D, R> {

    /**
     * 获取下一节点
     * @return
     */
    LogicLink<T, D, R> next();

    /**
     * 添加节点
     * @param next
     * @return
     */
    LogicLink<T, D, R> appendNext(LogicLink<T, D, R> next);
}

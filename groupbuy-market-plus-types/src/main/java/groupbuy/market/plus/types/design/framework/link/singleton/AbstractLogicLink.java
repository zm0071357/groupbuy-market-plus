package groupbuy.market.plus.types.design.framework.link.singleton;

/**
 * 抽象类 - 封装添加节点和执行 next 下一个节点的方法
 * @param <T>
 * @param <D>
 * @param <R>
 */
public abstract class AbstractLogicLink<T, D, R> implements LogicLink<T, D, R> {

    private LogicLink<T, D, R> next;

    @Override
    public LogicLink<T, D, R> next() {
        return next;
    }

    @Override
    public LogicLink<T, D, R> appendNext(LogicLink<T, D, R> next) {
        this.next = next;
        return next;
    }

    protected R next(T requestParameter, D dynamicContext) throws Exception {
        return next.apply(requestParameter, dynamicContext);
    }

}

package groupbuy.market.plus.types.design.framework.link.multition.handler;


public interface LogicHandler<T, D, R> {

    default R next(T requestParameter, D dynamicContext) {
        return null;
    }

    R apply(T requestParameter, D dynamicContext) throws Exception;

}
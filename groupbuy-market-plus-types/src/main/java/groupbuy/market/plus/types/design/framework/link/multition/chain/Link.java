package groupbuy.market.plus.types.design.framework.link.multition.chain;

public interface Link<E> {

    boolean add(E e);

    boolean addFirst(E e);

    boolean addLast(E e);

    boolean remove(Object o);

    E get(int index);

    void printLinkList();

}


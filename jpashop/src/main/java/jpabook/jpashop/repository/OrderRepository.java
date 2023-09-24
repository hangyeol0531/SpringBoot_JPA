package jpabook.jpashop.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

import static jpabook.jpashop.domain.QMember.member;
import static jpabook.jpashop.domain.QOrder.order;

@Repository
public class OrderRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public OrderRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }


    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    public List<Order> findAllOld(OrderSearch orderSearch) {
        return em.createQuery("select o from Order o join o.member m" +
                " where o.status = :status " +
                " and m.name like :name", Order.class)
            .setParameter("status", orderSearch.getOrderStatus())
            .setParameter("name", orderSearch.getMemberName())
            .setMaxResults(1000)
            .getResultList();
    }

    public List<Order> findAllByString(OrderSearch orderSearch) {

        String jpql = "select o from Order o join o.member m";
        boolean isFirstCondition = true;

        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }

        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
            .setMaxResults(1000);

        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }

        return query.getResultList();
    }


    public List<Order> findAll(OrderSearch orderSearch) {
        return queryFactory.select(order)
            .from(order)
            .join(order.member, member)
            .where(
                statusEq(orderSearch.getOrderStatus()),
                nameLike(orderSearch.getMemberName())
            )
            .limit(1000)
            .fetch();
    }

    private BooleanExpression nameLike(String memberName) {
        if (!StringUtils.hasText(memberName)) {
            return null;
        }
        return member.name.like(memberName);
    }

    private BooleanExpression statusEq(OrderStatus status) {
        // null return where에서 적용안함
        if (status == null) {
            return null;
        }
        return order.status.eq(status);
    }

    /**
     * DTO 조회 형식보다 성능이 안좋음, 재사용성이 높음
     */
    public List<Order> findAllWithMemberDelivery() {
        return em.createQuery(
            "select o from Order o" +
                " join fetch o.member m" +
                " join fetch o.delivery d", Order.class
        ).getResultList();
    }

    /**
     * 1 : n 에서 fetchjoin + paging 처리하면 메모리에서 처리하기에 사용하면 안된다.
     */
    public List<Order> findAllWithItem() {
        return em.createQuery(
                "select distinct o from Order o" +
                    " join fetch o.member m" +
                    " join fetch o.delivery d" +
                    " join fetch o.orderItems oi" +
                    " join fetch oi.item i", Order.class)
            .getResultList();
    }

    /**
     * oneToOne 같은 경우는 네트워크를 보통 fetch로 해결한다. (네트워크를 자주타기 때문에)
     * toOne 관계는 fetch join으로 쿼리를 줄이고
     * 나머지는 hibernate.default_batch_fetch_size로 최적화하면 된다.
     */
    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery(
                "select o from Order o" +
                    " join fetch o.member m" +
                    " join fetch o.delivery d", Order.class
            ).setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
    }
}

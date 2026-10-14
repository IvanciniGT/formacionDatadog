package com.course.shop.repository;

import com.course.shop.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Used on purpose by /api/report to produce an N+1 query pattern (DBM demo)
    long countByProductId(Long productId);
    // join fetch: the product is LAZY and the view is built outside the transaction
    @Query("select o from Order o join fetch o.product order by o.createdAt desc, o.id desc")
    List<Order> findLatestWithProduct(Pageable page);

    @Query("select o.product.name, count(o) from Order o group by o.product.name")
    List<Object[]> countByProduct();
}

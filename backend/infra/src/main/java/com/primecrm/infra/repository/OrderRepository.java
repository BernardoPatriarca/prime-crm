package com.primecrm.infra.repository;

import com.primecrm.infra.entity.order.Order;
import com.primecrm.infra.entity.order.OrderStatus;
import com.primecrm.infra.repository.projection.AmountAggregate;
import com.primecrm.infra.repository.projection.LabeledAmountAggregate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select count(o) as itemCount, coalesce(sum(o.totalAmount), 0) as totalAmount
            from Order o
            where o.deletedAt is null and o.orderDate >= :from and o.orderDate < :to
            """)
    AmountAggregate summarizeOrderedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select count(o) as itemCount, coalesce(sum(o.totalAmount), 0) as totalAmount
            from Order o
            where o.deletedAt is null and o.status = :status and o.orderDate >= :from and o.orderDate < :to
            """)
    AmountAggregate summarizeByStatusOrderedBetween(@Param("status") OrderStatus status,
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select function('to_char', o.orderDate, 'YYYY-MM') as label,
                   count(o) as itemCount, coalesce(sum(o.totalAmount), 0) as totalAmount
            from Order o
            where o.deletedAt is null and o.orderDate >= :from
            group by function('to_char', o.orderDate, 'YYYY-MM')
            """)
    List<LabeledAmountAggregate> summarizeOrderedByMonth(@Param("from") LocalDate from);
}

package com.primecrm.infra.repository;

import com.primecrm.infra.entity.order.OrderItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrder_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(UUID orderId);

    Optional<OrderItem> findByIdAndOrder_IdAndDeletedAtIsNull(UUID id, UUID orderId);

    long countByOrder_IdAndDeletedAtIsNull(UUID orderId);
}

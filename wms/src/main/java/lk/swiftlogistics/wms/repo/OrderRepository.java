package lk.swiftlogistics.wms.repo;

import lk.swiftlogistics.wms.domain.Order;
import lk.swiftlogistics.wms.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByClientRef(String clientRef);
    
    List<Order> findByStatus(OrderStatus status);
    
    List<Order> findByDeliveryDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE o.deliveryDate >= :startDate AND o.deliveryDate <= :endDate AND o.status = :status")
    List<Order> findByDeliveryDateBetweenAndStatus(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate, 
        @Param("status") OrderStatus status
    );
    
    List<Order> findByAssignedDriverId(Long driverId);
    
    List<Order> findByAssignedDriverIsNull();
}
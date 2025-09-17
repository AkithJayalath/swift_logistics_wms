package lk.swiftlogistics.wms.service;

import lk.swiftlogistics.wms.domain.Driver;
import lk.swiftlogistics.wms.domain.Order;
import lk.swiftlogistics.wms.domain.OrderStatus;
import lk.swiftlogistics.wms.repo.DriverRepository;
import lk.swiftlogistics.wms.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderByClientRef(String clientRef) {
        return orderRepository.findByClientRef(clientRef);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public List<Order> getOrdersByDeliveryDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findByDeliveryDateBetween(startDate, endDate);
    }

    public List<Order> getOrdersByDeliveryDateRangeAndStatus(LocalDateTime startDate, LocalDateTime endDate, OrderStatus status) {
        return orderRepository.findByDeliveryDateBetweenAndStatus(startDate, endDate, status);
    }

    public List<Order> getUnassignedOrders() {
        return orderRepository.findByAssignedDriverIsNull();
    }

    public List<Order> getOrdersByDriver(Long driverId) {
        return orderRepository.findByAssignedDriverId(driverId);
    }

    public Order assignDriverToOrder(Long orderId, Long driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        if (order.getStatus() != OrderStatus.RECEIVED) {
            throw new RuntimeException("Order must be in RECEIVED status to assign a driver");
        }

        order.setAssignedDriver(driver);
        order.setStatus(OrderStatus.READY_TO_DISPATCH);
        
        log.info("Driver {} assigned to order {}", driver.getName(), order.getClientRef());
        
        return orderRepository.save(order);
    }

    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Validate status transitions
        if (!isValidStatusTransition(order.getStatus(), newStatus)) {
            throw new RuntimeException("Invalid status transition from " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);
        log.info("Order {} status updated to {}", order.getClientRef(), newStatus);
        
        return orderRepository.save(order);
    }

    public Order updateOrderDeliveryDate(Long orderId, LocalDateTime newDeliveryDate) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setDeliveryDate(newDeliveryDate);
        log.info("Order {} delivery date updated to {}", order.getClientRef(), newDeliveryDate);
        
        return orderRepository.save(order);
    }

    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        return switch (currentStatus) {
            case RECEIVED -> newStatus == OrderStatus.READY_TO_DISPATCH;
            case READY_TO_DISPATCH -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED -> false; // Cannot change from delivered
        };
    }

    // Driver management methods
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public List<Driver> getAvailableDrivers() {
        return driverRepository.findByAvailable(true);
    }

    public Driver createDriver(Driver driver) {
        log.info("Creating new driver: {}", driver.getName());
        return driverRepository.save(driver);
    }

    public Driver updateDriverAvailability(Long driverId, boolean available) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));
        
        driver.setAvailable(available);
        log.info("Driver {} availability updated to {}", driver.getName(), available);
        
        return driverRepository.save(driver);
    }

    public Optional<Driver> getDriverById(Long id) {
        return driverRepository.findById(id);
    }
}
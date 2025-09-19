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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

        if (!driver.isAvailable()) {
            throw new RuntimeException("Driver is not available for assignment");
        }

        if (order.getStatus() != OrderStatus.RECEIVED) {
            throw new RuntimeException("Order must be in RECEIVED status to assign a driver");
        }

        order.setAssignedDriver(driver);
        order.setStatus(OrderStatus.READY_TO_DISPATCH);
        
        // Set driver as unavailable when assigned to an order
        driver.setAvailable(false);
        driverRepository.save(driver);
        
        log.info("Driver {} assigned to order {} and marked as unavailable", driver.getName(), order.getClientRef());
        
        return orderRepository.save(order);
    }

    public List<Order> bulkAssignDriver(List<Long> orderIds, Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        if (!driver.isAvailable()) {
            throw new RuntimeException("Driver is not available for assignment");
        }

        List<Order> orders = orderRepository.findAllById(orderIds);
        List<Order> updatedOrders = new ArrayList<>();

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.RECEIVED && order.getAssignedDriver() == null) {
                order.setAssignedDriver(driver);
                order.setStatus(OrderStatus.READY_TO_DISPATCH);
                updatedOrders.add(order);
            }
        }

        if (updatedOrders.isEmpty()) {
            throw new RuntimeException("No eligible orders found for assignment");
        }

        // Set driver as unavailable when assigned to orders
        driver.setAvailable(false);
        driverRepository.save(driver);
        
        orderRepository.saveAll(updatedOrders);
        log.info("Driver {} assigned to {} orders and marked as unavailable", driver.getName(), updatedOrders.size());
        
        return updatedOrders;
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

    public Map<String, Object> getOrderStatistics() {
        List<Order> allOrders = orderRepository.findAll();
        
        long totalOrders = allOrders.size();
        long receivedOrders = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.RECEIVED).count();
        long readyToDispatchOrders = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.READY_TO_DISPATCH).count();
        long deliveredOrders = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        long unassignedOrders = allOrders.stream().filter(o -> o.getAssignedDriver() == null).count();
        long highPriorityOrders = allOrders.stream().filter(o -> "High".equals(o.getPriority())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders);
        stats.put("receivedOrders", receivedOrders);
        stats.put("readyToDispatchOrders", readyToDispatchOrders);
        stats.put("deliveredOrders", deliveredOrders);
        stats.put("unassignedOrders", unassignedOrders);
        stats.put("highPriorityOrders", highPriorityOrders);
        
        return stats;
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

    public Driver updateDriver(Driver driver) {
        Driver existingDriver = driverRepository.findById(driver.getId())
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driver.getId()));
        
        existingDriver.setName(driver.getName());
        existingDriver.setLicenseNumber(driver.getLicenseNumber());
        existingDriver.setPhoneNumber(driver.getPhoneNumber());
        existingDriver.setEmail(driver.getEmail());
        if (driver.isAvailable() != existingDriver.isAvailable()) {
            existingDriver.setAvailable(driver.isAvailable());
        }
        
        log.info("Driver {} updated", existingDriver.getName());
        return driverRepository.save(existingDriver);
    }

    public Driver updateDriverAvailability(Long driverId, boolean available) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));
        
        driver.setAvailable(available);
        log.info("Driver {} availability updated to {}", driver.getName(), available);
        
        return driverRepository.save(driver);
    }

    public void deleteDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));
        
        // Check if driver has active orders
        List<Order> activeOrders = orderRepository.findByAssignedDriverIdAndStatusIn(driverId, 
                List.of(OrderStatus.RECEIVED, OrderStatus.READY_TO_DISPATCH));
        
        if (!activeOrders.isEmpty()) {
            throw new RuntimeException("Cannot delete driver with active orders");
        }
        
        driverRepository.deleteById(driverId);
        log.info("Driver {} deleted", driver.getName());
    }

    public Map<String, Object> getDriverStatistics() {
        List<Driver> allDrivers = driverRepository.findAll();
        
        long totalDrivers = allDrivers.size();
        long availableDrivers = allDrivers.stream().filter(Driver::isAvailable).count();
        long busyDrivers = totalDrivers - availableDrivers;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDrivers", totalDrivers);
        stats.put("availableDrivers", availableDrivers);
        stats.put("busyDrivers", busyDrivers);
        
        return stats;
    }

    public Map<String, Object> getDriverPerformance(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));
        
        List<Order> driverOrders = orderRepository.findByAssignedDriverId(driverId);
        
        long totalDeliveries = driverOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        long pendingDeliveries = driverOrders.stream().filter(o -> o.getStatus() == OrderStatus.READY_TO_DISPATCH).count();
        
        Map<String, Object> performance = new HashMap<>();
        performance.put("driverName", driver.getName());
        performance.put("totalDeliveries", totalDeliveries);
        performance.put("pendingDeliveries", pendingDeliveries);
        performance.put("isAvailable", driver.isAvailable());
        
        return performance;
    }

    public Optional<Driver> getDriverById(Long id) {
        return driverRepository.findById(id);
    }

    /**
     * Check if driver has any active (non-delivered) orders
     * Used to determine if driver should be marked as available
     */
    public boolean hasActiveOrders(Long driverId) {
        List<Order> activeOrders = orderRepository.findByAssignedDriverIdAndStatusIn(driverId, 
                List.of(OrderStatus.RECEIVED, OrderStatus.READY_TO_DISPATCH));
        return !activeOrders.isEmpty();
    }

    /**
     * Automatically update driver availability based on their order status
     * Driver becomes available when all assigned orders are delivered
     */
    public Driver updateDriverAvailabilityBasedOnOrders(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));
        
        boolean hasActiveOrders = hasActiveOrders(driverId);
        
        if (!hasActiveOrders && !driver.isAvailable()) {
            driver.setAvailable(true);
            driverRepository.save(driver);
            log.info("Driver {} marked as available - no active orders remaining", driver.getName());
        }
        
        return driver;
    }
}
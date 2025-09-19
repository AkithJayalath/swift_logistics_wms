package lk.swiftlogistics.wms.web;

import lk.swiftlogistics.wms.domain.Order;
import lk.swiftlogistics.wms.domain.OrderStatus;
import lk.swiftlogistics.wms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver-app")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:8081", "http://127.0.0.1:3000", "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:8081"})
@Slf4j
public class DriverAppController {

    private final OrderService orderService;

    /**
     * Get all drivers with their availability status
     * Used for driver management and status monitoring
     */
    @GetMapping("/drivers")
    public ResponseEntity<?> getAllDrivers() {
        try {
            List<Map<String, Object>> driverList = orderService.getAllDrivers().stream()
                .map(driver -> {
                    Map<String, Object> driverMap = new HashMap<>();
                    driverMap.put("driverId", driver.getId());
                    driverMap.put("name", driver.getName());
                    driverMap.put("licenseNumber", driver.getLicenseNumber());
                    driverMap.put("phoneNumber", driver.getPhoneNumber());
                    driverMap.put("email", driver.getEmail());
                    driverMap.put("available", driver.isAvailable());
                    driverMap.put("activeOrders", orderService.hasActiveOrders(driver.getId()) ? 
                        orderService.getOrdersByDriver(driver.getId()).stream()
                            .filter(order -> order.getStatus() != OrderStatus.DELIVERED)
                            .count() : 0);
                    return driverMap;
                })
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "drivers", driverList,
                "totalDrivers", driverList.size(),
                "availableDrivers", driverList.stream().filter(d -> (Boolean) d.get("available")).count()
            ));
            
        } catch (Exception e) {
            log.error("Error retrieving drivers: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Failed to retrieve drivers: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all orders assigned to a specific driver
     * Driver App will call this to get their assignments
     */
    @GetMapping("/driver/{driverId}/orders")
    public ResponseEntity<?> getDriverOrders(@PathVariable Long driverId) {
        try {
            List<Order> driverOrders = orderService.getOrdersByDriver(driverId);
            
            // Transform to driver app friendly format
            List<Map<String, Object>> driverAppOrders = driverOrders.stream()
                .map(order -> {
                    Map<String, Object> orderMap = new HashMap<>();
                    orderMap.put("orderId", order.getId());
                    orderMap.put("trackingNumber", order.getClientRef());
                    orderMap.put("customerName", order.getCustomerName());
                    orderMap.put("deliveryAddress", order.getDeliveryAddress());
                    orderMap.put("deliveryDate", order.getDeliveryDate().toString());
                    orderMap.put("status", order.getStatus().toString());
                    orderMap.put("priority", order.getPriority() != null ? order.getPriority() : "Medium");
                    orderMap.put("packageInfo", order.getPackageInfo() != null ? order.getPackageInfo() : "");
                    orderMap.put("weight", order.getWeight() != null ? order.getWeight() : 0.0);
                    orderMap.put("warehouseLocation", order.getWarehouseLocation() != null ? order.getWarehouseLocation() : "A-12-3");
                    orderMap.put("createdAt", order.getCreatedAt().toString());
                    orderMap.put("assignedAt", order.getUpdatedAt().toString());
                    return orderMap;
                })
                .toList();
            
            log.info("Retrieved {} orders for driver {}", driverAppOrders.size(), driverId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "driverId", driverId,
                "orders", driverAppOrders,
                "totalOrders", driverAppOrders.size()
            ));
            
        } catch (Exception e) {
            log.error("Error retrieving orders for driver {}: {}", driverId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Failed to retrieve driver orders: " + e.getMessage()
            ));
        }
    }

    /**
     * Notify driver app when a new order is assigned
     * WMS will call this internally when driver is assigned
     */
    @PostMapping("/driver/{driverId}/notify-assignment")
    public ResponseEntity<?> notifyDriverAssignment(
            @PathVariable Long driverId,
            @RequestBody Map<String, Object> assignmentData) {
        try {
            Long orderId = Long.valueOf(assignmentData.get("orderId").toString());
            
            // Get the order details
            Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            // Prepare notification data for driver app
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_ASSIGNMENT");
            notification.put("orderId", order.getId());
            notification.put("trackingNumber", order.getClientRef());
            notification.put("customerName", order.getCustomerName());
            notification.put("deliveryAddress", order.getDeliveryAddress());
            notification.put("deliveryDate", order.getDeliveryDate().toString());
            notification.put("priority", order.getPriority() != null ? order.getPriority() : "Medium");
            notification.put("packageInfo", order.getPackageInfo() != null ? order.getPackageInfo() : "");
            notification.put("weight", order.getWeight() != null ? order.getWeight() : 0.0);
            notification.put("warehouseLocation", order.getWarehouseLocation() != null ? order.getWarehouseLocation() : "A-12-3");
            notification.put("assignedAt", LocalDateTime.now().toString());
            notification.put("message", "New delivery assignment received");
            
            log.info("Assignment notification sent to driver {} for order {}", driverId, orderId);
            
            // In a real implementation, you would:
            // 1. Send push notification to driver app
            // 2. Store notification in driver notification table
            // 3. Use WebSocket or SSE for real-time updates
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Assignment notification sent to driver",
                "notification", notification
            ));
            
        } catch (Exception e) {
            log.error("Error notifying driver {} of assignment: {}", driverId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Failed to notify driver: " + e.getMessage()
            ));
        }
    }

    /**
     * Driver app confirms delivery completion
     * Updates order status to DELIVERED
     */
    @PostMapping("/driver/{driverId}/confirm-delivery")
    public ResponseEntity<?> confirmDelivery(
            @PathVariable Long driverId,
            @RequestBody Map<String, Object> deliveryData) {
        try {
            Long orderId = Long.valueOf(deliveryData.get("orderId").toString());
            String deliveryNotes = deliveryData.getOrDefault("deliveryNotes", "").toString();
            String deliveryProof = deliveryData.getOrDefault("deliveryProof", "").toString(); // Could be photo URL, signature, etc.
            
            // Verify that the order is assigned to this driver
            Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (order.getAssignedDriver() == null || !order.getAssignedDriver().getId().equals(driverId)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Order is not assigned to this driver"
                ));
            }
            
            // Update order status to DELIVERED
            Order updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);
            
            // Check if driver has any remaining active orders and update availability accordingly
            orderService.updateDriverAvailabilityBasedOnOrders(driverId);
            
            // Log delivery completion with additional data
            log.info("Delivery confirmed by driver {} for order {}. Driver availability updated based on remaining orders. Notes: {}", 
                    driverId, orderId, deliveryNotes);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Delivery confirmed successfully",
                "orderId", orderId,
                "trackingNumber", updatedOrder.getClientRef(),
                "status", updatedOrder.getStatus().toString(),
                "deliveredAt", updatedOrder.getUpdatedAt().toString(),
                "deliveryNotes", deliveryNotes,
                "deliveryProof", deliveryProof
            ));
            
        } catch (Exception e) {
            log.error("Error confirming delivery by driver {}: {}", driverId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Failed to confirm delivery: " + e.getMessage()
            ));
        }
    }
}
package lk.swiftlogistics.wms.web;

import lk.swiftlogistics.wms.domain.Order;
import lk.swiftlogistics.wms.domain.OrderStatus;
import lk.swiftlogistics.wms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:3000", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/client-ref/{clientRef}")
    public ResponseEntity<Order> getOrderByClientRef(@PathVariable String clientRef) {
        return orderService.getOrderByClientRef(clientRef)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @GetMapping("/ready-for-dispatch")
    public ResponseEntity<List<Order>> getReadyForDispatchOrders() {
        return ResponseEntity.ok(orderService.getOrdersByStatus(OrderStatus.RECEIVED));
    }

    @GetMapping("/unassigned")
    public ResponseEntity<List<Order>> getUnassignedOrders() {
        return ResponseEntity.ok(orderService.getUnassignedOrders());
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<Order>> getOrdersByDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(orderService.getOrdersByDriver(driverId));
    }

    @GetMapping("/delivery-date")
    public ResponseEntity<List<Order>> getOrdersByDeliveryDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) OrderStatus status) {
        
        if (status != null) {
            return ResponseEntity.ok(orderService.getOrdersByDeliveryDateRangeAndStatus(startDate, endDate, status));
        } else {
            return ResponseEntity.ok(orderService.getOrdersByDeliveryDateRange(startDate, endDate));
        }
    }

    @PutMapping("/{id}/assign-driver")
    public ResponseEntity<?> assignDriver(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {
        try {
            Long driverId = request.get("driverId");
            if (driverId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "driverId is required"));
            }
            Order updatedOrder = orderService.assignDriverToOrder(id, driverId);
            
            // Log the assignment for driver app notification
            log.info("Driver {} assigned to order {}. Driver app can be notified via /api/driver-app/driver/{}/notify-assignment", 
                    driverId, id, driverId);
            
            // Note: In a real implementation, you could automatically call the driver app notification here
            // or use an event-driven approach with message queues
            
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            log.error("Error assigning driver to order {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String statusStr = request.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
            }
            OrderStatus newStatus = OrderStatus.valueOf(statusStr.toUpperCase());
            Order updatedOrder = orderService.updateOrderStatus(id, newStatus);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + request.get("status")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/delivery-date")
    public ResponseEntity<?> updateDeliveryDate(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String dateStr = request.get("deliveryDate");
            if (dateStr == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "deliveryDate is required"));
            }
            LocalDateTime newDeliveryDate = LocalDateTime.parse(dateStr);
            Order updatedOrder = orderService.updateOrderDeliveryDate(id, newDeliveryDate);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format. Use ISO format: 2023-12-25T10:00:00"));
        }
    }

    // Bulk operations for frontend convenience
    @PutMapping("/bulk/assign-driver")
    public ResponseEntity<?> bulkAssignDriver(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> orderIds = (List<Long>) request.get("orderIds");
            Long driverId = ((Number) request.get("driverId")).longValue();
            
            if (orderIds == null || orderIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "orderIds are required"));
            }
            if (driverId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "driverId is required"));
            }
            
            List<Order> updatedOrders = orderService.bulkAssignDriver(orderIds, driverId);
            return ResponseEntity.ok(Map.of("success", true, "orders", updatedOrders));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get order statistics for dashboard
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOrderStats() {
        Map<String, Object> stats = orderService.getOrderStatistics();
        return ResponseEntity.ok(stats);
    }
}
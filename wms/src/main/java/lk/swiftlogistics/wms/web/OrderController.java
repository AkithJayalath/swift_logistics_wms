package lk.swiftlogistics.wms.web;

import lk.swiftlogistics.wms.domain.Order;
import lk.swiftlogistics.wms.domain.OrderStatus;
import lk.swiftlogistics.wms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
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
                return ResponseEntity.badRequest().body("driverId is required");
            }
            Order updatedOrder = orderService.assignDriverToOrder(id, driverId);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String statusStr = request.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest().body("status is required");
            }
            OrderStatus newStatus = OrderStatus.valueOf(statusStr.toUpperCase());
            Order updatedOrder = orderService.updateOrderStatus(id, newStatus);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status: " + request.get("status"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/delivery-date")
    public ResponseEntity<?> updateDeliveryDate(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String dateStr = request.get("deliveryDate");
            if (dateStr == null) {
                return ResponseEntity.badRequest().body("deliveryDate is required");
            }
            LocalDateTime newDeliveryDate = LocalDateTime.parse(dateStr);
            Order updatedOrder = orderService.updateOrderDeliveryDate(id, newDeliveryDate);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date format. Use ISO format: 2023-12-25T10:00:00");
        }
    }
}
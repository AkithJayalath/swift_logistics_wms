package lk.swiftlogistics.wms.web;

import lk.swiftlogistics.wms.domain.Driver;
import lk.swiftlogistics.wms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:3000", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class DriverController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<Driver>> getAllDrivers() {
        return ResponseEntity.ok(orderService.getAllDrivers());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Driver>> getAvailableDrivers() {
        return ResponseEntity.ok(orderService.getAvailableDrivers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Driver> getDriverById(@PathVariable Long id) {
        return orderService.getDriverById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createDriver(@RequestBody Driver driver) {
        try {
            // Basic validation
            if (driver.getName() == null || driver.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Driver name is required"));
            }
            if (driver.getLicenseNumber() == null || driver.getLicenseNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "License number is required"));
            }
            if (driver.getEmail() == null || driver.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            if (driver.getPhoneNumber() == null || driver.getPhoneNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
            }
            
            Driver createdDriver = orderService.createDriver(driver);
            return ResponseEntity.ok(createdDriver);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create driver: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDriver(@PathVariable Long id, @RequestBody Driver driver) {
        try {
            driver.setId(id);
            Driver updatedDriver = orderService.updateDriver(driver);
            return ResponseEntity.ok(updatedDriver);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<?> updateDriverAvailability(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean available = request.get("available");
            if (available == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "available field is required"));
            }
            Driver updatedDriver = orderService.updateDriverAvailability(id, available);
            return ResponseEntity.ok(updatedDriver);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDriver(@PathVariable Long id) {
        try {
            orderService.deleteDriver(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Driver deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get driver statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDriverStats() {
        Map<String, Object> stats = orderService.getDriverStatistics();
        return ResponseEntity.ok(stats);
    }

    // Get driver performance data
    @GetMapping("/{id}/performance")
    public ResponseEntity<Map<String, Object>> getDriverPerformance(@PathVariable Long id) {
        try {
            Map<String, Object> performance = orderService.getDriverPerformance(id);
            return ResponseEntity.ok(performance);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
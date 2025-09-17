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
                return ResponseEntity.badRequest().body("Driver name is required");
            }
            if (driver.getLicenseNumber() == null || driver.getLicenseNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("License number is required");
            }
            if (driver.getEmail() == null || driver.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }
            if (driver.getPhoneNumber() == null || driver.getPhoneNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Phone number is required");
            }
            
            Driver createdDriver = orderService.createDriver(driver);
            return ResponseEntity.ok(createdDriver);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create driver: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<?> updateDriverAvailability(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean available = request.get("available");
            if (available == null) {
                return ResponseEntity.badRequest().body("available field is required");
            }
            Driver updatedDriver = orderService.updateDriverAvailability(id, available);
            return ResponseEntity.ok(updatedDriver);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
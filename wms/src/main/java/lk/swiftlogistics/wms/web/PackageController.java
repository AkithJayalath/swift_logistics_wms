package lk.swiftlogistics.wms.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import lombok.RequiredArgsConstructor;
import lk.swiftlogistics.wms.repo.PackageRepository;
import lk.swiftlogistics.wms.messaging.EventPublisher;
import lk.swiftlogistics.wms.domain.Package;
import lk.swiftlogistics.wms.domain.PackageStatus;
import java.util.Map;
import java.time.Instant;

@RestController
@RequestMapping("/api/wms/packages")
@RequiredArgsConstructor
public class PackageController {
    private final PackageRepository repo;
    private final EventPublisher publisher;

    @PostMapping("/receive")
    public Package receive(@RequestBody Map<String, String> body) {
        Package pkg = new Package();
        pkg.setClientRef(body.get("clientRef"));
        pkg.setStatus(PackageStatus.RECEIVED);
        pkg.setLastUpdated(Instant.now());
        repo.save(pkg);

        publisher.publishEvent("{\"type\":\"PKG_RECEIVED\",\"ref\":\"" + pkg.getClientRef() + "\"}");
        return pkg;
    }

    @GetMapping("/{ref}")
    public Package find(@PathVariable String ref) {
        return repo.findByClientRef(ref).orElseThrow();
    }
}

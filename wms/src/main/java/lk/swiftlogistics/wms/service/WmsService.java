package lk.swiftlogistics.wms.service;

import lk.swiftlogistics.wms.domain.Package;
import lk.swiftlogistics.wms.domain.PackageStatus;
import lk.swiftlogistics.wms.repo.PackageRepository;
import lk.swiftlogistics.wms.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WmsService {

    private final PackageRepository repo;
    private final EventPublisher publisher;

    public Package handlePackageReceived(String clientRef, String zone) {
        // idempotency check
        Package pkg = repo.findByClientRef(clientRef).orElseGet(Package::new);

        pkg.setClientRef(clientRef);
        pkg.setStatus(PackageStatus.RECEIVED);
        pkg.setCurrentZone(zone);
        pkg.setLastUpdated(Instant.now());
        repo.save(pkg);

        // publish event
        String eventJson = String.format(
            "{\"type\":\"PKG_RECEIVED\",\"ref\":\"%s\",\"zone\":\"%s\"}",
            clientRef, zone
        );
        
        WarehouseEvent event = new WarehouseEvent();
    event.setEventType("PKG_RECEIVED");
    event.setPayload(eventJson);
    event.setStatus(WarehouseEvent.Status.NEW);
    eventRepo.save(event);

        return pkg;
    }

    public Package findPackage(String clientRef) {
    return repo.findByClientRef(clientRef).orElse(null);
}

public Package handleRouteAssignment(String clientRef, String routeId, String vehicleId) {
    var pkgOpt = repo.findByClientRef(clientRef);

    if (pkgOpt.isEmpty()) {
        System.out.println("⚠️ Package not found for ref " + clientRef);
        return null;
    }

    Package pkg = pkgOpt.get();
    pkg.setStatus(PackageStatus.LOADED);
    pkg.setCurrentZone("VEHICLE:" + vehicleId);
    pkg.setLastUpdated(Instant.now());
    repo.save(pkg);

    // outbox event
    String eventJson = String.format(
        "{\"type\":\"PKG_LOADED\",\"ref\":\"%s\",\"routeId\":\"%s\",\"vehicleId\":\"%s\"}",
        clientRef, routeId, vehicleId
    );
    WarehouseEvent e = new WarehouseEvent();
    e.setEventType("PKG_LOADED");
    e.setPayload(eventJson);
    e.setStatus(WarehouseEvent.Status.NEW);
    eventRepo.save(e);

    return pkg;
}


}

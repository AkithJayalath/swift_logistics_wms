package lk.swiftlogistics.wms.messaging;

import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import lombok.RequiredArgsConstructor;
import lk.swiftlogistics.wms.repo.PackageRepository;
import lk.swiftlogistics.wms.domain.Package;
import lk.swiftlogistics.wms.domain.PackageStatus;
import lk.swiftlogistics.wms.config.RabbitConfig;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderListener {
    private final PackageRepository repo;

    @RabbitListener(queues = RabbitConfig.ORDERS_QUEUE)
    public void handleOrder(String payload) {
        // parse JSON (use Jackson)
        Package pkg = new Package();
        pkg.setClientRef(UUID.randomUUID().toString());
        pkg.setStatus(PackageStatus.RECEIVED);
        repo.save(pkg);
        System.out.println("Order received -> " + pkg.getClientRef());
    }
}

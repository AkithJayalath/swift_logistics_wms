package lk.swiftlogistics.wms.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.swiftlogistics.wms.config.RabbitConfig;
import lk.swiftlogistics.wms.domain.Package;
import lk.swiftlogistics.wms.domain.PackageStatus;
import lk.swiftlogistics.wms.repo.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderListener {
    private final PackageRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    @RabbitListener(queues = RabbitConfig.ORDERS_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleOrder(String payload) throws Exception {
        try {
            JsonNode json = mapper.readTree(payload);

            Package pkg = new Package();
            pkg.setClientRef(json.has("clientRef")
                    ? json.get("clientRef").asText()
                    : UUID.randomUUID().toString());
            pkg.setStatus(PackageStatus.RECEIVED);
            pkg.setCurrentZone(json.has("zone") ? json.get("zone").asText() : "INBOUND");
            pkg.setLastUpdated(Instant.now());

            repo.save(pkg);
            System.out.println("✅ Order received -> " + pkg.getClientRef());
        } catch (Exception e) {
            // Let exception bubble up → Spring AMQP retry/DLQ will handle it
            System.err.println("❌ Failed to process order: " + e.getMessage());
            throw e;
        }
    }
}

package lk.swiftlogistics.wms.messaging;

import lk.swiftlogistics.wms.domain.WarehouseEvent;
import lk.swiftlogistics.wms.repo.WarehouseEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final WarehouseEventRepository repo;
    private final RabbitTemplate rabbit;

    @Scheduled(fixedDelay = 2000) // every 2s
    public void publishOutbox() {
        List<WarehouseEvent> events = repo.findTop50ByStatusOrderByCreatedAtAsc(WarehouseEvent.Status.NEW);

        for (WarehouseEvent e : events) {
            try {
                rabbit.convertAndSend("wms.events", e.getPayload());
                e.setStatus(WarehouseEvent.Status.SENT);
            } catch (Exception ex) {
                e.setStatus(WarehouseEvent.Status.FAILED);
            }
            repo.save(e);
        }
    }
}

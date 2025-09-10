package lk.swiftlogistics.wms.messaging;

import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import lombok.RequiredArgsConstructor;
import lk.swiftlogistics.wms.config.RabbitConfig;

@Service
@RequiredArgsConstructor
public class EventPublisher {
    private final RabbitTemplate rabbit;

    public void publishEvent(String json) {
        rabbit.convertAndSend(RabbitConfig.EVENTS_QUEUE, json);
    }
}

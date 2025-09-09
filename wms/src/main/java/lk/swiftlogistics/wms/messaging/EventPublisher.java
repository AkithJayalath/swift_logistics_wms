@Service
@RequiredArgsConstructor
public class EventPublisher {
    private final RabbitTemplate rabbit;

    public void publishEvent(String json) {
        rabbit.convertAndSend(RabbitConfig.EVENTS_QUEUE, json);
    }
}

package lk.swiftlogistics.wms.web;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Controller
public class EventStreamController {

    // Hot publisher (keeps live stream of events)
    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    // Subscribe to RabbitMQ events and push to SSE sink
    @RabbitListener(queues = "wms.events")
    public void handleEvent(String payload) {
        sink.tryEmitNext(payload);
    }

    // SSE endpoint
    @GetMapping(value = "/api/wms/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents() {
        return sink.asFlux()
                .map(e -> ServerSentEvent.builder(e).build())
                .mergeWith(Flux.interval(Duration.ofSeconds(10))
                        .map(t -> ServerSentEvent.builder("ping").comment("keepalive").build()));
    }
}

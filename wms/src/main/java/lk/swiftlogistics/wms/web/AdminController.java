package lk.swiftlogistics.wms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Peek messages from a DLQ (non-blocking receive). Returns up to 'count' messages.
     * Example: GET /api/admin/dlq?queue=orders.inbound.dlq&count=10
     */
    @GetMapping("/dlq")
    public ResponseEntity<List<String>> peekDlq(
            @RequestParam String queue,
            @RequestParam(defaultValue = "10") int count) {

        List<String> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Message msg = rabbitTemplate.receive(queue, 100L); // timeout millis
            if (msg == null) break;
            messages.add(new String(msg.getBody()));
            // re-publish to same DLQ so we don't remove it here (we will remove with retry endpoint)
            rabbitTemplate.send(queue, msg);
        }
        return ResponseEntity.ok(messages);
    }

    /**
     * Retry one message from DLQ: pop one message from DLQ and send back to original queue.
     * Example: POST /api/admin/retry?dlq=orders.inbound.dlq&target=orders.inbound
     */
    @PostMapping("/retry")
    public ResponseEntity<String> retryOne(
            @RequestParam String dlq,
            @RequestParam String target
    ) {
        Message msg = rabbitTemplate.receive(dlq);
        if (msg == null) return ResponseEntity.badRequest().body("No messages in DLQ");

        // re-publish to target queue (or to exchange)
        // preserve headers if needed
        MessageProperties props = msg.getMessageProperties();
        rabbitTemplate.send("", target, msg);

        return ResponseEntity.ok("Requeued one message to " + target);
    }
}

package lk.swiftlogistics.wms.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.swiftlogistics.wms.config.RabbitConfig;
import lk.swiftlogistics.wms.domain.Order;
import lk.swiftlogistics.wms.domain.OrderStatus;
import lk.swiftlogistics.wms.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderListener {
    
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = RabbitConfig.ORDERS_QUEUE)
    public void handleOrder(String orderMessage) {
        try {
            log.info("Received order message: {}", orderMessage);
            
            JsonNode orderJson = objectMapper.readTree(orderMessage);
            
            Order order = new Order();
            order.setClientRef(orderJson.has("clientRef") 
                ? orderJson.get("clientRef").asText() 
                : "ORD-" + UUID.randomUUID().toString().substring(0, 8));
            
            order.setCustomerName(orderJson.get("customerName").asText());
            order.setDeliveryAddress(orderJson.get("deliveryAddress").asText());
            
            // Parse delivery date with multiple format support
            String deliveryDateStr = orderJson.get("deliveryDate").asText();
            LocalDateTime deliveryDate;
            try {
                // Try ISO format first
                deliveryDate = LocalDateTime.parse(deliveryDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e1) {
                try {
                    // Try ISO date format (add default time)
                    deliveryDate = LocalDateTime.parse(deliveryDateStr + "T09:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e2) {
                    // Default to tomorrow 9 AM if parsing fails
                    log.warn("Could not parse delivery date '{}', using default", deliveryDateStr);
                    deliveryDate = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
                }
            }
            order.setDeliveryDate(deliveryDate);
            
            order.setStatus(OrderStatus.RECEIVED);
            
            orderRepository.save(order);
            
            log.info("Order {} processed and saved successfully", order.getClientRef());
            
        } catch (Exception e) {
            log.error("Failed to process order message: {}", orderMessage, e);
            throw new RuntimeException("Order processing failed", e);
        }
    }
}

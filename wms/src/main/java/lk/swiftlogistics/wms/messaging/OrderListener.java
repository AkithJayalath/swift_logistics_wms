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
import java.time.format.DateTimeParseException;

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
            
            // Handle tracking number from CMS
            String trackingNumber = orderJson.has("trackingNumber") 
                ? orderJson.get("trackingNumber").asText() 
                : orderJson.has("orderId") 
                    ? orderJson.get("orderId").asText()
                    : "ORD-" + System.currentTimeMillis();
            
            order.setClientRef(trackingNumber);
            
            // Extract customer name from CMS format
            String customerName = "Unknown Customer";
            if (orderJson.has("pickup") && orderJson.get("pickup").has("contact")) {
                customerName = orderJson.get("pickup").get("contact").asText();
            } else if (orderJson.has("source") && orderJson.get("source").has("contact")) {
                customerName = orderJson.get("source").get("contact").asText();
            }
            order.setCustomerName(customerName);
            
            // Extract delivery address from CMS format
            String deliveryAddress = "No address provided";
            if (orderJson.has("source") && orderJson.get("source").has("address")) {
                deliveryAddress = orderJson.get("source").get("address").asText();
            } else if (orderJson.has("pickup") && orderJson.get("pickup").has("address")) {
                deliveryAddress = orderJson.get("pickup").get("address").asText();
            }
            order.setDeliveryAddress(deliveryAddress);
            
            // Extract package weight and description
            String packageInfo = "";
            if (orderJson.has("package")) {
                JsonNode packageNode = orderJson.get("package");
                if (packageNode.has("weight") && packageNode.has("description")) {
                    packageInfo = packageNode.get("weight").asText() + "kg " + 
                                 packageNode.get("description").asText();
                } else if (packageNode.has("packageInfo")) {
                    packageInfo = packageNode.get("packageInfo").asText();
                }
            }
            order.setPackageInfo(packageInfo);
            
            // Parse delivery date with multiple format support
            LocalDateTime deliveryDate = LocalDateTime.now().plusDays(1);
            
            if (orderJson.has("service") && orderJson.get("service").has("estimatedDelivery")) {
                String deliveryDateStr = orderJson.get("service").get("estimatedDelivery").asText();
                deliveryDate = parseDeliveryDate(deliveryDateStr);
            } else if (orderJson.has("estimatedDelivery")) {
                String deliveryDateStr = orderJson.get("estimatedDelivery").asText();
                deliveryDate = parseDeliveryDate(deliveryDateStr);
            }
            
            order.setDeliveryDate(deliveryDate);
            order.setStatus(OrderStatus.RECEIVED);
            
            // Extract priority from urgency field
            String priority = "Medium";
            if (orderJson.has("service") && orderJson.get("service").has("urgency")) {
                String urgency = orderJson.get("service").get("urgency").asText();
                priority = mapUrgencyToPriority(urgency);
            } else if (orderJson.has("urgency")) {
                String urgency = orderJson.get("urgency").asText();
                priority = mapUrgencyToPriority(urgency);
            }
            order.setPriority(priority);
            
            orderRepository.save(order);
            
            log.info("Order {} processed and saved successfully with priority {}", 
                    order.getClientRef(), order.getPriority());
            
        } catch (Exception e) {
            log.error("Failed to process order message: {}", orderMessage, e);
            throw new RuntimeException("Order processing failed", e);
        }
    }
    
    private LocalDateTime parseDeliveryDate(String deliveryDateStr) {
        try {
            // Try ISO format first
            return LocalDateTime.parse(deliveryDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                // Try ISO date format (add default time)
                return LocalDateTime.parse(deliveryDateStr + "T09:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e2) {
                try {
                    // Try ISO instant format and convert
                    return LocalDateTime.parse(deliveryDateStr.replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (DateTimeParseException e3) {
                    // Default to tomorrow 9 AM if parsing fails
                    log.warn("Could not parse delivery date '{}', using default", deliveryDateStr);
                    return LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
                }
            }
        }
    }
    
    private String mapUrgencyToPriority(String urgency) {
        switch (urgency.toLowerCase()) {
            case "urgent":
                return "High";
            case "high":
                return "High";
            case "normal":
            case "medium":
                return "Medium";
            case "low":
                return "Low";
            default:
                return "Medium";
        }
    }
}

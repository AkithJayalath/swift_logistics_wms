package lk.swiftlogistics.wms.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.swiftlogistics.wms.service.WmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteAssignmentListener {

    private final WmsService wmsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "route.assignments")
    public void handleRouteAssignment(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String clientRef = node.get("clientRef").asText();
            String routeId = node.get("routeId").asText();
            String vehicleId = node.get("vehicleId").asText();

            var pkg = wmsService.handleRouteAssignment(clientRef, routeId, vehicleId);
            if (pkg != null) {
                System.out.println("✅ Package " + clientRef + " assigned to route " + routeId);
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to process route assignment: " + e.getMessage());
        }
    }
}

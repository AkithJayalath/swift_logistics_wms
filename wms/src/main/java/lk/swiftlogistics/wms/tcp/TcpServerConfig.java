package lk.swiftlogistics.wms.tcp;

import lk.swiftlogistics.wms.service.WmsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.TcpInboundGateway;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.messaging.MessageChannel;

import java.nio.charset.StandardCharsets;

@Configuration
public class TcpServerConfig {

    private final WmsService wmsService;

    public TcpServerConfig(WmsService wmsService) {
        this.wmsService = wmsService;
    }

    @Bean
    public TcpNetServerConnectionFactory serverFactory() {
        return new TcpNetServerConnectionFactory(7071);
    }

    @Bean
    public TcpInboundGateway inboundGateway(TcpNetServerConnectionFactory serverFactory) {
        TcpInboundGateway gateway = new TcpInboundGateway();
        gateway.setConnectionFactory(serverFactory);
        gateway.setRequestChannel(tcpInboundChannel());
        return gateway;
    }

    @Bean
    public MessageChannel tcpInboundChannel() {
        return new DirectChannel();
    }

    @ServiceActivator(inputChannel = "tcpInboundChannel")
    public byte[] handleTcp(byte[] bytes) {
        String raw = new String(bytes, StandardCharsets.UTF_8).trim();
        try {
            TcpMessage msg = TcpMessageParser.parse(raw);
            System.out.println("✅ Parsed TCP: " + msg);

            if ("PKG_RECEIVED".equalsIgnoreCase(msg.getType())) {
                wmsService.handlePackageReceived(
                    msg.getPkgRef(),
                    msg.getData().replaceAll("[{}\"]", "") // quick parse
                );
            }

            return ("ACK|" + msg.getPkgRef() + "\n").getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ("NACK|" + e.getMessage() + "\n").getBytes(StandardCharsets.UTF_8);
        }
    }

    @ServiceActivator(inputChannel = "tcpInboundChannel")
public byte[] handleTcp(byte[] bytes) {
    String raw = new String(bytes, StandardCharsets.UTF_8).trim();
    try {
        TcpMessage msg = TcpMessageParser.parse(raw);
        System.out.println("✅ Parsed TCP: " + msg);

        switch (msg.getType().toUpperCase()) {
            case "PKG_RECEIVED" -> {
                wmsService.handlePackageReceived(
                        msg.getPkgRef(),
                        msg.getData().replaceAll("[{}\"]", "") // crude parse
                );
                return ("ACK|" + msg.getPkgRef() + "\n").getBytes(StandardCharsets.UTF_8);
            }

            case "REQ_STATUS" -> {
                var pkg = wmsService.findPackage(msg.getPkgRef());
                String response = String.format(
                        "STATUS|%s|%s\n",
                        msg.getPkgRef(),
                        pkg != null ? pkg.getStatus().name() : "NOT_FOUND"
                );
                return response.getBytes(StandardCharsets.UTF_8);
            }

            default -> {
                return ("NACK|UnknownType\n").getBytes(StandardCharsets.UTF_8);
            }
        }
    } catch (Exception e) {
        return ("NACK|" + e.getMessage() + "\n").getBytes(StandardCharsets.UTF_8);
    }
}

}

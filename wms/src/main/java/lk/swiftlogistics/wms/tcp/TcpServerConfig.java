@Configuration
public class TcpServerConfig {
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
    public String handleTcp(byte[] bytes) {
        String msg = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("Received TCP: " + msg);
        return "ACK\n";
    }
}
